package com.moblin.android.platform.video

import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import com.moblin.android.platform.core.PipelineThread
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object GlRenderer {
    enum class ScalingMode {
        fit,
        fill,
        stretch,
        trim,
    }

    private const val TAG = "MoblinPipeline"

    private const val VERTEX_SHADER = """
attribute vec4 aPosition;
attribute vec4 aTexCoord;
uniform mat4 uTexMatrix;
varying vec2 vTexCoord;
void main() {
    gl_Position = aPosition;
    vTexCoord = (uTexMatrix * aTexCoord).xy;
}
"""

    private const val FRAGMENT_SHADER_2D = """
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif
varying vec2 vTexCoord;
uniform sampler2D sTexture;
void main() {
    gl_FragColor = texture2D(sTexture, vTexCoord);
}
"""

    private const val FRAGMENT_SHADER_OES = """#extension GL_OES_EGL_image_external : require
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif
varying vec2 vTexCoord;
uniform samplerExternalOES sTexture;
void main() {
    gl_FragColor = texture2D(sTexture, vTexCoord);
}
"""

    private class Program(val id: Int) {
        val position = GLES20.glGetAttribLocation(id, "aPosition")
        val texCoord = GLES20.glGetAttribLocation(id, "aTexCoord")
        val texMatrix = GLES20.glGetUniformLocation(id, "uTexMatrix")
        val sampler = GLES20.glGetUniformLocation(id, "sTexture")
    }

    private val corners = floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f, 1f, 1f)
    private val positions: FloatBuffer = makeFloatBuffer(floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f))
    private val texCoords: FloatBuffer = makeFloatBuffer(FloatArray(8))
    private val texCoordValues = FloatArray(8)
    private val identity = FloatArray(16).also { Matrix.setIdentityM(it, 0) }
    private var program2d: Program? = null
    private var programOes: Program? = null
    private var programsFailed = false
    private var lastErrorLogMs = 0L

    fun createOesTexture(): Int {
        if (!PipelineThread.isCurrent()) {
            return PipelineThread.runSync { createOesTexture() }
        }
        if (!EglCore.isReady) {
            Log.e(TAG, "createOesTexture: EGL is not ready")
            return 0
        }
        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, ids[0])
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
        return ids[0]
    }

    fun deleteTexture(texture: Int) {
        if (texture == 0) {
            return
        }
        if (!PipelineThread.isCurrent()) {
            PipelineThread.post { deleteTexture(texture) }
            return
        }
        if (!EglCore.isReady) {
            return
        }
        GLES20.glDeleteTextures(1, intArrayOf(texture), 0)
    }

    fun drawOes(oesTexture: Int, stMatrix: FloatArray, target: CVPixelBuffer, rotationDegreesCw: Int, mirror: Boolean) {
        if (!EglCore.isReady || !target.checkReadable("camera draw target")) {
            return
        }
        val previousFramebuffer = currentFramebuffer()
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, target.framebuffer)
        drawTexture(
            texture = oesTexture,
            oes = true,
            texMatrix = stMatrix,
            sourceWidth = target.width,
            sourceHeight = target.height,
            targetWidth = target.width,
            targetHeight = target.height,
            mode = ScalingMode.stretch,
            rotationDegreesCw = rotationDegreesCw,
            mirror = mirror,
            flipVertical = false,
        )
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, previousFramebuffer)
    }

    fun draw(
        source: CVPixelBuffer,
        targetWidth: Int,
        targetHeight: Int,
        mode: ScalingMode,
        rotationDegreesCw: Int = 0,
        mirror: Boolean = false,
        clearBlack: Boolean = true,
    ) {
        if (!EglCore.isReady) {
            return
        }
        val isReadable = source.checkReadable("draw")
        if (clearBlack || !isReadable) {
            GLES20.glViewport(0, 0, targetWidth, targetHeight)
            clear(0f, 0f, 0f, 1f)
        }
        if (!isReadable) {
            return
        }
        drawTexture(
            texture = source.texture,
            oes = false,
            texMatrix = null,
            sourceWidth = source.width,
            sourceHeight = source.height,
            targetWidth = targetWidth,
            targetHeight = targetHeight,
            mode = mode,
            rotationDegreesCw = rotationDegreesCw,
            mirror = mirror,
            flipVertical = false,
        )
    }

    fun bind(target: CVPixelBuffer?) {
        if (!EglCore.isReady) {
            return
        }
        if (target == null) {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        } else {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, target.framebuffer)
            GLES20.glViewport(0, 0, target.width, target.height)
        }
    }

    fun clear(r: Float, g: Float, b: Float, a: Float) {
        if (!EglCore.isReady) {
            return
        }
        GLES20.glClearColor(r, g, b, a)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
    }

    internal fun currentFramebuffer(): Int {
        val values = IntArray(1)
        GLES20.glGetIntegerv(GLES20.GL_FRAMEBUFFER_BINDING, values, 0)
        return values[0]
    }

    internal fun drawTexture(
        texture: Int,
        oes: Boolean,
        texMatrix: FloatArray?,
        sourceWidth: Int,
        sourceHeight: Int,
        targetWidth: Int,
        targetHeight: Int,
        mode: ScalingMode,
        rotationDegreesCw: Int,
        mirror: Boolean,
        flipVertical: Boolean,
    ) {
        if (sourceWidth <= 0 || sourceHeight <= 0 || targetWidth <= 0 || targetHeight <= 0 || texture == 0) {
            return
        }
        val program = program(oes) ?: return
        val quarter = (((rotationDegreesCw % 360) + 360) % 360 + 45) / 90 % 4
        val rotatedWidth = if (quarter % 2 == 1) sourceHeight else sourceWidth
        val rotatedHeight = if (quarter % 2 == 1) sourceWidth else sourceHeight
        var viewportX = 0
        var viewportY = 0
        var viewportWidth = targetWidth
        var viewportHeight = targetHeight
        var cropX = 1f
        var cropY = 1f
        when (mode) {
            ScalingMode.fit -> {
                val scale = min(
                    targetWidth.toFloat() / rotatedWidth,
                    targetHeight.toFloat() / rotatedHeight
                )
                viewportWidth = max(1, (rotatedWidth * scale).roundToInt())
                viewportHeight = max(1, (rotatedHeight * scale).roundToInt())
                viewportX = (targetWidth - viewportWidth) / 2
                viewportY = (targetHeight - viewportHeight) / 2
            }
            ScalingMode.fill, ScalingMode.trim -> {
                val scale = max(
                    targetWidth.toFloat() / rotatedWidth,
                    targetHeight.toFloat() / rotatedHeight
                )
                cropX = targetWidth / (rotatedWidth * scale)
                cropY = targetHeight / (rotatedHeight * scale)
            }
            ScalingMode.stretch -> {}
        }
        for (index in 0 until 4) {
            var x = corners[index * 2]
            var y = corners[index * 2 + 1]
            if (flipVertical) {
                y = 1f - y
            }
            if (mirror) {
                x = 1f - x
            }
            x = 0.5f + (x - 0.5f) * cropX
            y = 0.5f + (y - 0.5f) * cropY
            val sourceX: Float
            val sourceY: Float
            when (quarter) {
                1 -> {
                    sourceX = 1f - y
                    sourceY = x
                }
                2 -> {
                    sourceX = 1f - x
                    sourceY = 1f - y
                }
                3 -> {
                    sourceX = y
                    sourceY = 1f - x
                }
                else -> {
                    sourceX = x
                    sourceY = y
                }
            }
            texCoordValues[index * 2] = sourceX
            texCoordValues[index * 2 + 1] = sourceY
        }
        texCoords.position(0)
        texCoords.put(texCoordValues)
        texCoords.position(0)
        positions.position(0)
        GLES20.glViewport(viewportX, viewportY, viewportWidth, viewportHeight)
        GLES20.glUseProgram(program.id)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        val textureTarget = if (oes) GLES11Ext.GL_TEXTURE_EXTERNAL_OES else GLES20.GL_TEXTURE_2D
        GLES20.glBindTexture(textureTarget, texture)
        GLES20.glUniform1i(program.sampler, 0)
        GLES20.glUniformMatrix4fv(program.texMatrix, 1, false, texMatrix ?: identity, 0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glEnableVertexAttribArray(program.position)
        GLES20.glVertexAttribPointer(program.position, 2, GLES20.GL_FLOAT, false, 8, positions)
        GLES20.glEnableVertexAttribArray(program.texCoord)
        GLES20.glVertexAttribPointer(program.texCoord, 2, GLES20.GL_FLOAT, false, 8, texCoords)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(program.position)
        GLES20.glDisableVertexAttribArray(program.texCoord)
        GLES20.glBindTexture(textureTarget, 0)
        GLES20.glUseProgram(0)
        checkError("drawTexture")
    }

    internal fun checkError(operation: String) {
        val error = GLES20.glGetError()
        if (error == GLES20.GL_NO_ERROR) {
            return
        }
        val nowMs = System.currentTimeMillis()
        if (nowMs - lastErrorLogMs > 5000) {
            lastErrorLogMs = nowMs
            Log.e(TAG, "$operation: GL error 0x${Integer.toHexString(error)}")
        }
    }

    private fun program(oes: Boolean): Program? {
        if (programsFailed) {
            return null
        }
        val existing = if (oes) programOes else program2d
        if (existing != null) {
            return existing
        }
        val id = buildProgram(VERTEX_SHADER, if (oes) FRAGMENT_SHADER_OES else FRAGMENT_SHADER_2D)
        if (id == 0) {
            programsFailed = true
            return null
        }
        val program = Program(id)
        if (oes) {
            programOes = program
        } else {
            program2d = program
        }
        return program
    }

    private fun buildProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        if (vertexShader == 0) {
            return 0
        }
        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        if (fragmentShader == 0) {
            GLES20.glDeleteShader(vertexShader)
            return 0
        }
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
        GLES20.glDeleteShader(vertexShader)
        GLES20.glDeleteShader(fragmentShader)
        val status = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0)
        if (status[0] != GLES20.GL_TRUE) {
            Log.e(TAG, "Program link failed: ${GLES20.glGetProgramInfoLog(program)}")
            GLES20.glDeleteProgram(program)
            return 0
        }
        return program
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] != GLES20.GL_TRUE) {
            Log.e(TAG, "Shader compile failed: ${GLES20.glGetShaderInfoLog(shader)}")
            GLES20.glDeleteShader(shader)
            return 0
        }
        return shader
    }

    private fun makeFloatBuffer(values: FloatArray): FloatBuffer {
        val buffer = ByteBuffer.allocateDirect(values.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        buffer.put(values)
        buffer.position(0)
        return buffer
    }
}

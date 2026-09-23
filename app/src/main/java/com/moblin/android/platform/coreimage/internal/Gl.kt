package com.moblin.android.platform.coreimage.internal

import android.opengl.GLES20
import android.opengl.GLES30
import android.util.Log
import com.moblin.android.platform.core.PipelineStats
import com.moblin.android.platform.video.EglCore
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

internal const val TAG = "MoblinEffects"

internal object EffectsLog {
    private val loggedMessages = HashSet<String>()

    fun once(key: String, message: String, error: Throwable? = null) {
        val isNew = synchronized(loggedMessages) {
            if (loggedMessages.size > 2000) {
                loggedMessages.clear()
            }
            loggedMessages.add(key)
        }
        if (!isNew) {
            return
        }
        try {
            if (error != null) {
                Log.w(TAG, message, error)
            } else {
                Log.i(TAG, message)
            }
        } catch (_: Throwable) {
        }
    }

    fun notImplemented(member: String) {
        once("notImplemented:$member", "$member not implemented yet")
    }

    fun info(message: String) {
        try {
            Log.i(TAG, message)
        } catch (_: Throwable) {
        }
    }
}

internal enum class TextureFormat(val bytesPerPixel: Int) {
    rgba8(4),
    rgba16f(8),
}

internal class GlProgram(val id: Int) {
    private val locations = HashMap<String, Int>()
    val position: Int = GLES20.glGetAttribLocation(id, "aPos")

    fun location(name: String): Int {
        return locations.getOrPut(name) { GLES20.glGetUniformLocation(id, name) }
    }
}

internal object Gl {
    private const val MAX_PROGRAMS = 96
    private var capabilitiesChecked = false
    private var quadBuffer = 0
    private val quadData: FloatBuffer = ByteBuffer.allocateDirect(8 * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f, 1f, 1f))
            position(0)
        }
    private val programs = object : LinkedHashMap<String, GlProgram?>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, GlProgram?>?): Boolean {
            if (size <= MAX_PROGRAMS) {
                return false
            }
            val program = eldest?.value
            if (program != null) {
                GLES20.glDeleteProgram(program.id)
            }
            return true
        }
    }

    var es3 = false
        private set
    var linearSupported = false
        private set
    var maxTextureSize = 4096
        private set
    var maxTextureUnits = 8
        private set
    var versionText = ""
        private set

    fun ensureCapabilities() {
        if (capabilitiesChecked || !EglCore.isReady) {
            return
        }
        capabilitiesChecked = true
        es3 = EglCore.glMajorVersion >= 3
        val values = IntArray(1)
        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, values, 0)
        if (values[0] > 0) {
            maxTextureSize = values[0]
        }
        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_IMAGE_UNITS, values, 0)
        if (values[0] > 0) {
            maxTextureUnits = values[0]
        }
        versionText = GLES20.glGetString(GLES20.GL_VERSION) ?: ""
        linearSupported = es3 && probeHalfFloatRendering()
        EffectsLog.info(
            "renderer ready (ES ${if (es3) "3.x" else "2.0"}, linear light ${if (linearSupported) "yes" else "no"})"
        )
        if (!linearSupported) {
            EffectsLog.info("linear light unavailable")
        }
        PipelineStats.gauge("fxLinear", if (linearSupported) 1 else 0)
    }

    private fun probeHalfFloatRendering(): Boolean {
        val extensions = GLES20.glGetString(GLES20.GL_EXTENSIONS) ?: ""
        val versionAllows = Regex("OpenGL ES (\\d+)\\.(\\d+)").find(versionText)?.let {
            val major = it.groupValues[1].toInt()
            val minor = it.groupValues[2].toInt()
            major > 3 || (major == 3 && minor >= 2)
        } ?: false
        if (!versionAllows &&
            !extensions.contains("GL_EXT_color_buffer_half_float") &&
            !extensions.contains("GL_EXT_color_buffer_float")
        ) {
            return false
        }
        val previousFramebuffer = currentFramebuffer()
        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        val texture = ids[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D,
            0,
            GLES30.GL_RGBA16F,
            4,
            4,
            0,
            GLES20.GL_RGBA,
            GLES30.GL_HALF_FLOAT,
            null
        )
        GLES20.glGenFramebuffers(1, ids, 0)
        val framebuffer = ids[0]
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebuffer)
        GLES20.glFramebufferTexture2D(
            GLES20.GL_FRAMEBUFFER,
            GLES20.GL_COLOR_ATTACHMENT0,
            GLES20.GL_TEXTURE_2D,
            texture,
            0
        )
        val status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, previousFramebuffer)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glDeleteFramebuffers(1, intArrayOf(framebuffer), 0)
        GLES20.glDeleteTextures(1, intArrayOf(texture), 0)
        while (GLES20.glGetError() != GLES20.GL_NO_ERROR) {
            continue
        }
        return status == GLES20.GL_FRAMEBUFFER_COMPLETE
    }

    fun currentFramebuffer(): Int {
        val values = IntArray(1)
        GLES20.glGetIntegerv(GLES20.GL_FRAMEBUFFER_BINDING, values, 0)
        return values[0]
    }

    fun vertexShader(): String {
        return if (es3) {
            "#version 300 es\nin vec2 aPos;\nuniform vec4 uRect;\n" +
                "void main() {\n    gl_Position = vec4(mix(uRect.xy, uRect.zw, aPos), 0.0, 1.0);\n}\n"
        } else {
            "attribute vec2 aPos;\nuniform vec4 uRect;\n" +
                "void main() {\n    gl_Position = vec4(mix(uRect.xy, uRect.zw, aPos), 0.0, 1.0);\n}\n"
        }
    }

    fun program(fragmentSource: String): GlProgram? {
        if (programs.containsKey(fragmentSource)) {
            return programs[fragmentSource]
        }
        val program = buildProgram(vertexShader(), fragmentSource)
        programs[fragmentSource] = program
        return program
    }

    fun drawQuad(program: GlProgram, x0: Float, y0: Float, x1: Float, y1: Float) {
        if (quadBuffer == 0) {
            val ids = IntArray(1)
            GLES20.glGenBuffers(1, ids, 0)
            quadBuffer = ids[0]
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, quadBuffer)
            quadData.position(0)
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 8 * 4, quadData, GLES20.GL_STATIC_DRAW)
        }
        GLES20.glUniform4f(program.location("uRect"), x0, y0, x1, y1)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, quadBuffer)
        GLES20.glEnableVertexAttribArray(program.position)
        GLES20.glVertexAttribPointer(program.position, 2, GLES20.GL_FLOAT, false, 8, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(program.position)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
    }

    private fun buildProgram(vertexSource: String, fragmentSource: String): GlProgram? {
        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource) ?: return null
        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        if (fragmentShader == null) {
            GLES20.glDeleteShader(vertexShader)
            return null
        }
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glBindAttribLocation(program, 0, "aPos")
        GLES20.glLinkProgram(program)
        GLES20.glDeleteShader(vertexShader)
        GLES20.glDeleteShader(fragmentShader)
        val status = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0)
        if (status[0] != GLES20.GL_TRUE) {
            EffectsLog.once(
                "link:${fragmentSource.hashCode()}",
                "Program link failed: ${GLES20.glGetProgramInfoLog(program)}"
            )
            GLES20.glDeleteProgram(program)
            return null
        }
        return GlProgram(program)
    }

    private fun compileShader(type: Int, source: String): Int? {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] != GLES20.GL_TRUE) {
            EffectsLog.once(
                "compile:${source.hashCode()}",
                "Shader compile failed: ${GLES20.glGetShaderInfoLog(shader)}\n$source"
            )
            GLES20.glDeleteShader(shader)
            return null
        }
        return shader
    }
}

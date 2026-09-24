package com.moblin.android.platform.live2d

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLES30
import android.util.Log
import com.moblin.android.platform.core.PipelineStats
import com.moblin.android.platform.core.PipelineThread
import com.moblin.android.platform.video.CVPixelBuffer
import java.lang.ref.PhantomReference
import java.lang.ref.ReferenceQueue
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

private const val TAG = "MoblinEffects"

private const val VERTEX_SHADER = """#version 300 es
layout(location = 0) in vec2 position;
layout(location = 1) in vec2 textureCoordinate;
uniform vec2 scale;
uniform vec2 offset;
out vec2 vTextureCoordinate;
void main() {
    gl_Position = vec4(position * scale + offset, 0.0, 1.0);
    vTextureCoordinate = textureCoordinate;
}
"""

private const val FRAGMENT_SHADER = """#version 300 es
precision highp float;
in vec2 vTextureCoordinate;
uniform sampler2D colorTexture;
uniform sampler2D maskTexture;
uniform vec3 multiplyColor;
uniform float opacity;
uniform vec3 screenColor;
uniform int useMask;
uniform int invertMask;
uniform vec2 maskSize;
out vec4 fragColor;
void main() {
    vec4 color = texture(colorTexture, vTextureCoordinate);
    color.rgb *= color.a;
    color.rgb *= multiplyColor;
    color.rgb = color.a - (color.a - color.rgb) * (1.0 - screenColor);
    float maskValue = 1.0;
    if (useMask != 0) {
        maskValue = texture(maskTexture, gl_FragCoord.xy / maskSize).r;
        if (invertMask != 0) {
            maskValue = 1.0 - maskValue;
        }
    }
    fragColor = clamp(color, 0.0, 1.0) * opacity * maskValue;
}
"""

private const val MASK_FRAGMENT_SHADER = """#version 300 es
precision highp float;
in vec2 vTextureCoordinate;
uniform sampler2D colorTexture;
out vec4 fragColor;
void main() {
    fragColor = vec4(texture(colorTexture, vTextureCoordinate).a);
}
"""

internal class Live2DGlResources {
    var textures = IntArray(0)
    var buffers = IntArray(0)
    var vertexArray = 0
    var programs = IntArray(0)
    var framebuffer = 0
    var clipTextures = IntArray(0)

    fun delete() {
        if (textures.isNotEmpty()) {
            GLES20.glDeleteTextures(textures.size, textures, 0)
        }
        val clips = clipTextures.filter { it != 0 }.toIntArray()
        if (clips.isNotEmpty()) {
            GLES20.glDeleteTextures(clips.size, clips, 0)
        }
        if (buffers.isNotEmpty()) {
            GLES20.glDeleteBuffers(buffers.size, buffers, 0)
        }
        if (vertexArray != 0) {
            GLES30.glDeleteVertexArrays(1, intArrayOf(vertexArray), 0)
        }
        for (program in programs) {
            if (program != 0) {
                GLES20.glDeleteProgram(program)
            }
        }
        if (framebuffer != 0) {
            GLES20.glDeleteFramebuffers(1, intArrayOf(framebuffer), 0)
        }
        textures = IntArray(0)
        clipTextures = IntArray(0)
        buffers = IntArray(0)
        vertexArray = 0
        programs = IntArray(0)
        framebuffer = 0
    }
}

internal object Live2DGlReaper {
    private class Reference(
        owner: Live2DRenderer,
        queue: ReferenceQueue<Live2DRenderer>,
        val resources: Live2DGlResources,
    ) : PhantomReference<Live2DRenderer>(owner, queue)

    private val queue = ReferenceQueue<Live2DRenderer>()
    private val references = HashSet<Reference>()

    @Volatile
    private var tickRegistered = false

    fun register(owner: Live2DRenderer, resources: Live2DGlResources) {
        synchronized(references) {
            references.add(Reference(owner, queue, resources))
            if (!tickRegistered) {
                tickRegistered = true
                PipelineStats.addPipelineTick { poll() }
            }
        }
    }

    fun poll() {
        if (!PipelineThread.isCurrent()) {
            return
        }
        while (true) {
            val reference = queue.poll() as? Reference ?: break
            synchronized(references) {
                references.remove(reference)
            }
            try {
                reference.resources.delete()
            } catch (error: Throwable) {
                Log.w(TAG, "v-tuber: Failed to delete Live2D GL resources: $error")
            }
        }
    }
}

private class Live2DPrograms(
    val normal: Int,
    val mask: Int,
    val scale: Int,
    val offset: Int,
    val colorTexture: Int,
    val maskTexture: Int,
    val multiplyColor: Int,
    val opacity: Int,
    val screenColor: Int,
    val useMask: Int,
    val invertMask: Int,
    val maskSize: Int,
    val maskScale: Int,
    val maskOffset: Int,
    val maskColorTexture: Int,
)

class Live2DRenderer private constructor(
    model: AyagamiModel,
    private var bitmaps: List<Bitmap>?,
    private val artMeshes: List<AyagamiArtMeshInfo>,
) {
    private val resources = Live2DGlResources()
    private val clipSetOf: IntArray
    private val clipSets: List<IntArray>
    private val clipTextureWidth: IntArray
    private val clipTextureHeight: IntArray
    private val scaleX: Float
    private val scaleY: Float
    private val offsetX: Float
    private val offsetY: Float
    private val indices: ShortArray = model.indices
    private val texcoords: FloatArray = model.texcoords
    private var programs: Live2DPrograms? = null
    private var failed = false
    private var vertexData: FloatBuffer? = null

    init {
        val indexes = HashMap<List<Int>, Int>()
        val sets = ArrayList<IntArray>()
        clipSetOf = IntArray(artMeshes.size) { uid ->
            val clips = artMeshes[uid].clips
            if (clips.isEmpty()) {
                -1
            } else {
                indexes.getOrPut(clips) {
                    sets.add(clips.toIntArray())
                    sets.size - 1
                }
            }
        }
        clipSets = sets
        clipTextureWidth = IntArray(sets.size)
        clipTextureHeight = IntArray(sets.size)
        val canvas = model.canvas
        scaleX = 2 * canvas.scale / canvas.dimensions.x
        scaleY = 2 * canvas.scale / canvas.dimensions.y * -1f
        offsetX = 2 * canvas.center.x / canvas.dimensions.x - 1
        offsetY = (2 * canvas.center.y / canvas.dimensions.y - 1) * -1f
        Live2DGlReaper.register(this, resources)
    }

    fun render(model: AyagamiModel, into: CVPixelBuffer) {
        if (!PipelineThread.isCurrent()) {
            try {
                PipelineThread.runSync { render(model, into) }
            } catch (error: Throwable) {
                Log.w(TAG, "v-tuber: Live2D render failed: $error")
            }
            return
        }
        Live2DGlReaper.poll()
        if (!into.checkReadable("Live2DRenderer.render")) {
            return
        }
        val saved = SavedGlState.save()
        var rendered = false
        try {
            if (!failed && setUp()) {
                renderInternal(model, into)
                rendered = true
            }
        } catch (error: Throwable) {
            Live2DLog.once("render:${error.message}", "v-tuber: Live2D render failed: $error")
        }
        try {
            if (!rendered) {
                clear(into)
            }
        } catch (error: Throwable) {
            Live2DLog.once("clear:${error.message}", "v-tuber: Live2D clear failed: $error")
        } finally {
            saved.restore()
        }
    }

    private fun clear(into: CVPixelBuffer) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, into.framebuffer)
        GLES20.glViewport(0, 0, into.width, into.height)
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST)
        GLES20.glColorMask(true, true, true, true)
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
    }

    private fun setUp(): Boolean {
        if (programs != null) {
            return true
        }
        val decoded = bitmaps ?: return false
        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
        val maskFragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, MASK_FRAGMENT_SHADER)
        val normal = if (vertexShader != 0 && fragmentShader != 0) linkProgram(vertexShader, fragmentShader) else 0
        val mask = if (vertexShader != 0 && maskFragmentShader != 0) linkProgram(vertexShader, maskFragmentShader) else 0
        for (shader in intArrayOf(vertexShader, fragmentShader, maskFragmentShader)) {
            if (shader != 0) {
                GLES20.glDeleteShader(shader)
            }
        }
        resources.programs = intArrayOf(normal, mask)
        if (normal == 0 || mask == 0) {
            Log.i(TAG, "v-tuber: Failed to create pipeline")
            failed = true
            return false
        }
        val textureIds = IntArray(decoded.size)
        if (textureIds.isNotEmpty()) {
            GLES20.glGenTextures(textureIds.size, textureIds, 0)
        }
        resources.textures = textureIds
        for ((index, bitmap) in decoded.withIndex()) {
            uploadTexture(textureIds[index], bitmap)
            bitmap.recycle()
        }
        bitmaps = null
        val bufferIds = IntArray(3)
        GLES20.glGenBuffers(3, bufferIds, 0)
        resources.buffers = bufferIds
        val indexBuffer = ByteBuffer.allocateDirect(maxOf(indices.size, 1) * 2).order(ByteOrder.nativeOrder())
            .asShortBuffer()
        indexBuffer.put(indices)
        indexBuffer.position(0)
        val vertexArrays = IntArray(1)
        GLES30.glGenVertexArrays(1, vertexArrays, 0)
        resources.vertexArray = vertexArrays[0]
        GLES30.glBindVertexArray(resources.vertexArray)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, bufferIds[0])
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, indices.size * 2, indexBuffer, GLES20.GL_STATIC_DRAW)
        GLES30.glBindVertexArray(0)
        val texcoordBuffer = ByteBuffer.allocateDirect(maxOf(texcoords.size, 1) * 4).order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        texcoordBuffer.put(texcoords)
        texcoordBuffer.position(0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferIds[1])
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, texcoords.size * 4, texcoordBuffer, GLES20.GL_STATIC_DRAW)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferIds[2])
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, texcoords.size * 4, null, GLES20.GL_DYNAMIC_DRAW)
        vertexData = ByteBuffer.allocateDirect(maxOf(texcoords.size, 1) * 4).order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        val framebuffers = IntArray(1)
        GLES20.glGenFramebuffers(1, framebuffers, 0)
        resources.framebuffer = framebuffers[0]
        resources.clipTextures = IntArray(clipSets.size)
        programs = Live2DPrograms(
            normal = normal,
            mask = mask,
            scale = GLES20.glGetUniformLocation(normal, "scale"),
            offset = GLES20.glGetUniformLocation(normal, "offset"),
            colorTexture = GLES20.glGetUniformLocation(normal, "colorTexture"),
            maskTexture = GLES20.glGetUniformLocation(normal, "maskTexture"),
            multiplyColor = GLES20.glGetUniformLocation(normal, "multiplyColor"),
            opacity = GLES20.glGetUniformLocation(normal, "opacity"),
            screenColor = GLES20.glGetUniformLocation(normal, "screenColor"),
            useMask = GLES20.glGetUniformLocation(normal, "useMask"),
            invertMask = GLES20.glGetUniformLocation(normal, "invertMask"),
            maskSize = GLES20.glGetUniformLocation(normal, "maskSize"),
            maskScale = GLES20.glGetUniformLocation(mask, "scale"),
            maskOffset = GLES20.glGetUniformLocation(mask, "offset"),
            maskColorTexture = GLES20.glGetUniformLocation(mask, "colorTexture"),
        )
        return true
    }

    private fun uploadTexture(texture: Int, bitmap: Bitmap) {
        val buffer = ByteBuffer.allocateDirect(bitmap.width * bitmap.height * 4).order(ByteOrder.nativeOrder())
        bitmap.copyPixelsToBuffer(buffer)
        buffer.position(0)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D,
            0,
            GLES30.GL_RGBA8,
            bitmap.width,
            bitmap.height,
            0,
            GLES20.GL_RGBA,
            GLES20.GL_UNSIGNED_BYTE,
            buffer
        )
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    private fun renderInternal(model: AyagamiModel, into: CVPixelBuffer) {
        val programs = programs ?: return
        val vertexData = vertexData ?: return
        val width = into.width
        val height = into.height
        val states = arrayOfNulls<AyagamiArtMeshState>(artMeshes.size)
        for (uid in artMeshes.indices) {
            val state = model.artMeshState(uid)
            if (state != null && state.visible && state.vertices.isNotEmpty()) {
                val offset = artMeshes[uid].texcoordOffset * 2
                if (offset >= 0 && offset + state.vertices.size <= vertexData.capacity()) {
                    vertexData.position(offset)
                    vertexData.put(state.vertices)
                }
            }
            states[uid] = state
        }
        vertexData.position(0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, resources.buffers[2])
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, vertexData.capacity() * 4, vertexData)
        val drawOrder = model.drawOrderArray().filter { uid ->
            val state = states.getOrNull(uid) ?: return@filter false
            state.visible && state.opacity > 0f && state.vertices.isNotEmpty()
        }
        val usedClipSets = sortedSetOf<Int>()
        for (uid in drawOrder) {
            val clipSet = clipSetOf[uid]
            if (clipSet != -1) {
                usedClipSets.add(clipSet)
            }
        }
        GLES30.glBindVertexArray(resources.vertexArray)
        GLES20.glEnableVertexAttribArray(0)
        GLES20.glEnableVertexAttribArray(1)
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST)
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDisable(GLES20.GL_STENCIL_TEST)
        GLES20.glColorMask(true, true, true, true)
        GLES20.glFrontFace(GLES20.GL_CCW)
        GLES20.glCullFace(GLES20.GL_FRONT)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendEquation(GLES20.GL_FUNC_ADD)
        for (index in usedClipSets) {
            renderClipSet(index, states, programs, width, height)
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, into.framebuffer)
        GLES20.glViewport(0, 0, width, height)
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(programs.normal)
        GLES20.glUniform2f(programs.scale, scaleX, scaleY)
        GLES20.glUniform2f(programs.offset, offsetX, offsetY)
        GLES20.glUniform1i(programs.colorTexture, 0)
        GLES20.glUniform1i(programs.maskTexture, 1)
        GLES20.glUniform2f(programs.maskSize, width.toFloat(), height.toFloat())
        for (uid in drawOrder) {
            val artMesh = artMeshes[uid]
            val state = states[uid] ?: continue
            when (artMesh.blendMode) {
                AyagamiBlendMode.normal -> GLES20.glBlendFuncSeparate(
                    GLES20.GL_ONE,
                    GLES20.GL_ONE_MINUS_SRC_ALPHA,
                    GLES20.GL_ONE,
                    GLES20.GL_ONE_MINUS_SRC_ALPHA
                )
                AyagamiBlendMode.add -> GLES20.glBlendFuncSeparate(
                    GLES20.GL_ONE,
                    GLES20.GL_ONE,
                    GLES20.GL_ZERO,
                    GLES20.GL_ONE
                )
                AyagamiBlendMode.multiply -> GLES20.glBlendFuncSeparate(
                    GLES20.GL_DST_COLOR,
                    GLES20.GL_ONE_MINUS_SRC_ALPHA,
                    GLES20.GL_ZERO,
                    GLES20.GL_ONE
                )
            }
            GLES20.glUniform3f(
                programs.multiplyColor,
                state.multiplyColor.x,
                state.multiplyColor.y,
                state.multiplyColor.z
            )
            GLES20.glUniform1f(programs.opacity, state.opacity)
            GLES20.glUniform3f(programs.screenColor, state.screenColor.x, state.screenColor.y, state.screenColor.z)
            GLES20.glUniform1i(programs.invertMask, if (artMesh.invertMask) 1 else 0)
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, resources.textures.getOrElse(artMesh.textureIndex) { 0 })
            val clipSet = clipSetOf[uid]
            if (clipSet != -1) {
                GLES20.glUniform1i(programs.useMask, 1)
                GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, resources.clipTextures[clipSet])
            } else {
                GLES20.glUniform1i(programs.useMask, 0)
            }
            draw(artMesh)
        }
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    private fun renderClipSet(
        index: Int,
        states: Array<AyagamiArtMeshState?>,
        programs: Live2DPrograms,
        width: Int,
        height: Int,
    ) {
        var texture = resources.clipTextures[index]
        if (texture != 0 && (clipTextureWidth[index] != width || clipTextureHeight[index] != height)) {
            GLES20.glDeleteTextures(1, intArrayOf(texture), 0)
            texture = 0
            resources.clipTextures[index] = 0
        }
        if (texture == 0) {
            val ids = IntArray(1)
            GLES20.glGenTextures(1, ids, 0)
            texture = ids[0]
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D,
                0,
                GLES30.GL_R8,
                width,
                height,
                0,
                GLES30.GL_RED,
                GLES20.GL_UNSIGNED_BYTE,
                null
            )
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
            resources.clipTextures[index] = texture
            clipTextureWidth[index] = width
            clipTextureHeight[index] = height
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, resources.framebuffer)
        GLES20.glFramebufferTexture2D(
            GLES20.GL_FRAMEBUFFER,
            GLES20.GL_COLOR_ATTACHMENT0,
            GLES20.GL_TEXTURE_2D,
            texture,
            0
        )
        GLES20.glViewport(0, 0, width, height)
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(programs.mask)
        GLES20.glUniform2f(programs.maskScale, scaleX, scaleY)
        GLES20.glUniform2f(programs.maskOffset, offsetX, offsetY)
        GLES20.glUniform1i(programs.maskColorTexture, 0)
        GLES20.glBlendFuncSeparate(
            GLES20.GL_ONE,
            GLES20.GL_ONE_MINUS_SRC_COLOR,
            GLES20.GL_ONE,
            GLES20.GL_ONE_MINUS_SRC_ALPHA
        )
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        for (uid in clipSets[index]) {
            val state = states.getOrNull(uid) ?: continue
            if (!state.visible || state.vertices.isEmpty()) {
                continue
            }
            val artMesh = artMeshes[uid]
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, resources.textures.getOrElse(artMesh.textureIndex) { 0 })
            draw(artMesh)
        }
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, 0, 0)
    }

    private fun draw(artMesh: AyagamiArtMeshInfo) {
        val count = artMesh.indexRange.last - artMesh.indexRange.first + 1
        if (count <= 0) {
            return
        }
        if (artMesh.culling) {
            GLES20.glEnable(GLES20.GL_CULL_FACE)
        } else {
            GLES20.glDisable(GLES20.GL_CULL_FACE)
        }
        val offset = artMesh.texcoordOffset * 8
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, resources.buffers[2])
        GLES20.glVertexAttribPointer(0, 2, GLES20.GL_FLOAT, false, 8, offset)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, resources.buffers[1])
        GLES20.glVertexAttribPointer(1, 2, GLES20.GL_FLOAT, false, 8, offset)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, count, GLES20.GL_UNSIGNED_SHORT, artMesh.indexRange.first * 2)
    }

    companion object {
        operator fun invoke(model: AyagamiModel): Live2DRenderer? {
            val bitmaps = ArrayList<Bitmap>()
            for (texturePath in model.texturePaths) {
                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                    inPremultiplied = false
                    inScaled = false
                }
                val bitmap = try {
                    BitmapFactory.decodeFile(texturePath.path, options)
                } catch (error: Throwable) {
                    Log.i(TAG, "v-tuber: Failed to load texture ${texturePath.name}: $error")
                    null
                }
                if (bitmap == null) {
                    Log.i(TAG, "v-tuber: Failed to load texture ${texturePath.name}")
                    for (decoded in bitmaps) {
                        decoded.recycle()
                    }
                    return null
                }
                bitmaps.add(bitmap)
            }
            val artMeshes = ArrayList<AyagamiArtMeshInfo>()
            for (uid in 0 until model.artMeshCount) {
                artMeshes.add(model.artMeshInfo(uid) ?: return null)
            }
            return Live2DRenderer(model, bitmaps, artMeshes)
        }
    }
}

private fun compileShader(type: Int, source: String): Int {
    val shader = GLES20.glCreateShader(type)
    if (shader == 0) {
        return 0
    }
    GLES20.glShaderSource(shader, source)
    GLES20.glCompileShader(shader)
    val status = IntArray(1)
    GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
    if (status[0] == 0) {
        Log.w(TAG, "v-tuber: Live2D shader error: ${GLES20.glGetShaderInfoLog(shader)}")
        GLES20.glDeleteShader(shader)
        return 0
    }
    return shader
}

private fun linkProgram(vertexShader: Int, fragmentShader: Int): Int {
    val program = GLES20.glCreateProgram()
    if (program == 0) {
        return 0
    }
    GLES20.glAttachShader(program, vertexShader)
    GLES20.glAttachShader(program, fragmentShader)
    GLES20.glLinkProgram(program)
    val status = IntArray(1)
    GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0)
    if (status[0] == 0) {
        Log.w(TAG, "v-tuber: Live2D program error: ${GLES20.glGetProgramInfoLog(program)}")
        GLES20.glDeleteProgram(program)
        return 0
    }
    return program
}

private class SavedGlState(
    private val framebuffer: Int,
    private val viewport: IntArray,
    private val program: Int,
    private val vertexArray: Int,
    private val arrayBuffer: Int,
    private val activeTexture: Int,
    private val blend: Boolean,
    private val cullFace: Boolean,
    private val frontFace: Int,
    private val cullFaceMode: Int,
    private val blendFunctions: IntArray,
    private val clearColor: FloatArray,
) {
    fun restore() {
        GLES30.glBindVertexArray(vertexArray)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, arrayBuffer)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebuffer)
        GLES20.glViewport(viewport[0], viewport[1], viewport[2], viewport[3])
        GLES20.glUseProgram(program)
        GLES20.glActiveTexture(activeTexture)
        if (blend) {
            GLES20.glEnable(GLES20.GL_BLEND)
        } else {
            GLES20.glDisable(GLES20.GL_BLEND)
        }
        if (cullFace) {
            GLES20.glEnable(GLES20.GL_CULL_FACE)
        } else {
            GLES20.glDisable(GLES20.GL_CULL_FACE)
        }
        GLES20.glFrontFace(frontFace)
        GLES20.glCullFace(cullFaceMode)
        GLES20.glBlendFuncSeparate(blendFunctions[0], blendFunctions[1], blendFunctions[2], blendFunctions[3])
        GLES20.glClearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3])
    }

    companion object {
        fun save(): SavedGlState {
            val value = IntArray(1)
            fun get(name: Int): Int {
                GLES20.glGetIntegerv(name, value, 0)
                return value[0]
            }
            val viewport = IntArray(4)
            GLES20.glGetIntegerv(GLES20.GL_VIEWPORT, viewport, 0)
            val clearColor = FloatArray(4)
            GLES20.glGetFloatv(GLES20.GL_COLOR_CLEAR_VALUE, clearColor, 0)
            return SavedGlState(
                framebuffer = get(GLES20.GL_FRAMEBUFFER_BINDING),
                viewport = viewport,
                program = get(GLES20.GL_CURRENT_PROGRAM),
                vertexArray = get(GLES30.GL_VERTEX_ARRAY_BINDING),
                arrayBuffer = get(GLES20.GL_ARRAY_BUFFER_BINDING),
                activeTexture = get(GLES20.GL_ACTIVE_TEXTURE),
                blend = GLES20.glIsEnabled(GLES20.GL_BLEND),
                cullFace = GLES20.glIsEnabled(GLES20.GL_CULL_FACE),
                frontFace = get(GLES20.GL_FRONT_FACE),
                cullFaceMode = get(GLES20.GL_CULL_FACE_MODE),
                blendFunctions = intArrayOf(
                    get(GLES20.GL_BLEND_SRC_RGB),
                    get(GLES20.GL_BLEND_DST_RGB),
                    get(GLES20.GL_BLEND_SRC_ALPHA),
                    get(GLES20.GL_BLEND_DST_ALPHA),
                ),
                clearColor = clearColor,
            )
        }
    }
}

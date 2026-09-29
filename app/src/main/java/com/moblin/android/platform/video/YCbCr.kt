package com.moblin.android.platform.video

import android.graphics.Color
import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.util.Log
import android.view.Surface
import com.moblin.android.BuildConfig
import com.moblin.android.platform.coreimage.internal.Gl
import com.moblin.android.platform.coreimage.internal.Glsl
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.roundToInt

private const val TAG = "MoblinPipeline"

internal class YCbCrDescriptor(
    val forwardRows: DoubleArray,
    val inverseRows: DoubleArray,
    val offset: DoubleArray,
) {
    val inverseMat3: FloatArray = FloatArray(9) { index -> inverseRows[(index % 3) * 3 + index / 3].toFloat() }
    val lumaRow: FloatArray = floatArrayOf(forwardRows[0].toFloat(), forwardRows[1].toFloat(), forwardRows[2].toFloat())
    val cbRow: FloatArray = floatArrayOf(forwardRows[3].toFloat(), forwardRows[4].toFloat(), forwardRows[5].toFloat())
    val crRow: FloatArray = floatArrayOf(forwardRows[6].toFloat(), forwardRows[7].toFloat(), forwardRows[8].toFloat())
    val offsetVector: FloatArray = floatArrayOf(offset[0].toFloat(), offset[1].toFloat(), offset[2].toFloat())

    fun encode(red: Float, green: Float, blue: Float): FloatArray {
        val rgb = doubleArrayOf(
            red.toDouble().coerceIn(0.0, 1.0),
            green.toDouble().coerceIn(0.0, 1.0),
            blue.toDouble().coerceIn(0.0, 1.0),
        )
        return FloatArray(3) { row ->
            (forwardRows[row * 3] * rgb[0] + forwardRows[row * 3 + 1] * rgb[1] + forwardRows[row * 3 + 2] * rgb[2] +
                offset[row]).toFloat()
        }
    }

    fun decode(y: Double, cb: Double, cr: Double): DoubleArray {
        val ycc = doubleArrayOf(y - offset[0], cb - offset[1], cr - offset[2])
        return DoubleArray(3) { row ->
            (inverseRows[row * 3] * ycc[0] + inverseRows[row * 3 + 1] * ycc[1] + inverseRows[row * 3 + 2] * ycc[2])
                .coerceIn(0.0, 1.0)
        }
    }
}

internal object YCbCr {
    private const val KR = 0.2126
    private const val KB = 0.0722

    val full: YCbCrDescriptor = make(KR, KB, lumaScale = 1.0, chromaScale = 1.0, lumaOffset = 0.0)

    val video: YCbCrDescriptor = make(KR, KB, lumaScale = 219.0 / 255, chromaScale = 224.0 / 255, lumaOffset = 16.0 / 255)

    private val descriptors = HashMap<Pair<String, Boolean>, YCbCrDescriptor>()

    fun descriptor(matrix: String?, videoRange: Boolean): YCbCrDescriptor {
        if (matrix == null || matrix == kCVImageBufferYCbCrMatrix_ITU_R_709_2) {
            return if (videoRange) video else full
        }
        return synchronized(descriptors) {
            descriptors.getOrPut(Pair(matrix, videoRange)) {
                val (kr, kb) = YCbCrCoding.coefficients(matrix)
                if (videoRange) {
                    make(kr, kb, lumaScale = 219.0 / 255, chromaScale = 224.0 / 255, lumaOffset = 16.0 / 255)
                } else {
                    make(kr, kb, lumaScale = 1.0, chromaScale = 1.0, lumaOffset = 0.0)
                }
            }
        }
    }

    private fun make(kr: Double, kb: Double, lumaScale: Double, chromaScale: Double, lumaOffset: Double): YCbCrDescriptor {
        val kg = 1 - kr - kb
        val cbRange = 2 - 2 * kb
        val crRange = 2 - 2 * kr
        val forward = doubleArrayOf(
            kr * lumaScale,
            kg * lumaScale,
            kb * lumaScale,
            -kr / cbRange * chromaScale,
            -kg / cbRange * chromaScale,
            (1 - kb) / cbRange * chromaScale,
            (1 - kr) / crRange * chromaScale,
            -kg / crRange * chromaScale,
            -kb / crRange * chromaScale,
        )
        val inverse = doubleArrayOf(
            1 / lumaScale,
            0.0,
            crRange / chromaScale,
            1 / lumaScale,
            -kb * cbRange / kg / chromaScale,
            -kr * crRange / kg / chromaScale,
            1 / lumaScale,
            cbRange / chromaScale,
            0.0,
        )
        return YCbCrDescriptor(forward, inverse, doubleArrayOf(lumaOffset, 128.0 / 255, 128.0 / 255))
    }
}

internal object YCbCrGeometry {
    fun chromaExtent(width: Int, height: Int): FloatArray {
        return floatArrayOf(
            2f * PixelBufferLayout.ycbcr420Full.chromaWidth(width) / width,
            2f * PixelBufferLayout.ycbcr420Full.chromaHeight(height) / height,
        )
    }

    fun chromaScale(width: Int, height: Int): FloatArray {
        return floatArrayOf(
            width / (2f * PixelBufferLayout.ycbcr420Full.chromaWidth(width)),
            height / (2f * PixelBufferLayout.ycbcr420Full.chromaHeight(height)),
        )
    }

    fun lumaClampActive(size: Int): Boolean {
        val lastChromaCentre = (2.0 * PixelBufferLayout.ycbcr420Full.chromaWidth(size) - 1) / size
        return lastChromaCentre > 1 - 0.5 / size
    }
}

internal object YCbCrStorage {
    private const val PROBE_TOLERANCE = 2

    @Volatile
    private var enabled = false

    @Volatile
    private var disabledReason: String? = null

    @Volatile
    private var probed = false

    @Volatile
    internal var probeFailureReason: String? = null
        private set

    @Volatile
    internal var override: Boolean? = null
        set(value) {
            field = value
            disabledReason = null
        }

    val isEnabled: Boolean
        get() = disabledReason == null && (override ?: enabled)

    fun layoutFor(requestedTag: Int): PixelBufferLayout {
        if (!isEnabled) {
            return PixelBufferLayout.rgba8
        }
        return when (requestedTag) {
            kCVPixelFormatType_32BGRA, kCVPixelFormatType_32RGBA -> PixelBufferLayout.rgba8
            kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange,
            kCVPixelFormatType_420YpCbCr10BiPlanarVideoRange,
            -> PixelBufferLayout.ycbcr420Video
            else -> PixelBufferLayout.ycbcr420Full
        }
    }

    fun tagFor(layout: PixelBufferLayout, requestedTag: Int): Int {
        return when (layout) {
            PixelBufferLayout.rgba8 -> requestedTag
            PixelBufferLayout.ycbcr420Full -> kCVPixelFormatType_420YpCbCr8BiPlanarFullRange
            PixelBufferLayout.ycbcr420Video -> kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange
        }
    }

    fun descriptorFor(layout: PixelBufferLayout): YCbCrDescriptor {
        return if (layout == PixelBufferLayout.ycbcr420Video) YCbCr.video else YCbCr.full
    }

    fun descriptorFor(buffer: CVPixelBuffer): YCbCrDescriptor {
        val matrix = buffer.attachments[kCVImageBufferYCbCrMatrixKey] as? String
        return YCbCr.descriptor(matrix, buffer.layout == PixelBufferLayout.ycbcr420Video)
    }

    fun disable(reason: String) {
        if (disabledReason != null) {
            return
        }
        disabledReason = reason
        Log.w(TAG, "YCbCr storage: off ($reason)")
    }

    fun probe() {
        if (probed) {
            return
        }
        probed = true
        val failure = try {
            probeFailure()
        } catch (error: Throwable) {
            "probe failed: $error"
        }
        while (GLES20.glGetError() != GLES20.GL_NO_ERROR) {
            continue
        }
        enabled = failure == null
        probeFailureReason = failure
        if (failure == null) {
            Log.i(TAG, "YCbCr storage: on")
        } else {
            Log.i(TAG, "YCbCr storage: off ($failure)")
        }
    }

    private fun probeFailure(): String? {
        if (!BuildConfig.YCBCR_INGEST) {
            return "turned off in this build"
        }
        if (!EglCore.isReady || EglCore.glMajorVersion < 3) {
            return "needs OpenGL ES 3, have ${EglCore.glMajorVersion}"
        }
        GlRenderer.planarProgramFailure()?.let {
            return it
        }
        val previousFramebuffer = GlRenderer.currentFramebuffer()
        val viewport = IntArray(4)
        GLES20.glGetIntegerv(GLES20.GL_VIEWPORT, viewport, 0)
        try {
            return probeStoredColours() ?: probeDecodedColour()
        } finally {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, previousFramebuffer)
            GLES20.glViewport(viewport[0], viewport[1], viewport[2], viewport[3])
        }
    }

    private fun probeStoredColours(): String? {
        for (layout in listOf(PixelBufferLayout.ycbcr420Full, PixelBufferLayout.ycbcr420Video)) {
            val backing = PixelBufferGl.allocatePlanar(3, 3, layout) ?: return "3x3 ${layout.name} planes incomplete"
            val scratch = PixelBufferGl.allocate(3, 3)
            try {
                if (scratch == null) {
                    return "3x3 scratch incomplete"
                }
                PixelBufferGl.clearPlanes(backing, 128f / 255, 64f / 255, 191f / 255)
                val read = drawAndRead(backing, scratch)
                val expected = descriptorFor(layout).decode(128.0 / 255, 64.0 / 255, 191.0 / 255).map {
                    (it * 255).roundToInt()
                }.toIntArray()
                if (!matches(read, expected)) {
                    return "stage 1 ${layout.name} read ${read.joinToString(",")}, " +
                        "expected ${expected.joinToString(",")}"
                }
                val effects = drawEffectsAndRead(backing, scratch) ?: return "the effects ycc leaf does not compile"
                if (!matches(effects, expected)) {
                    return "stage 1 effects ${layout.name} read ${effects.joinToString(",")}, " +
                        "expected ${expected.joinToString(",")}"
                }
            } finally {
                PixelBufferGl.delete(backing)
                scratch?.let { PixelBufferGl.delete(it) }
            }
        }
        return null
    }

    private fun probeDecodedColour(): String? {
        val expected = intArrayOf(200, 120, 60)
        val texture = GlRenderer.createOesTexture()
        if (texture == 0) {
            return "stage 2 has no external texture"
        }
        val surfaceTexture = SurfaceTexture(texture)
        surfaceTexture.setDefaultBufferSize(4, 4)
        val surface = Surface(surfaceTexture)
        try {
            val canvas = surface.lockCanvas(null)
            canvas.drawColor(Color.rgb(expected[0], expected[1], expected[2]))
            surface.unlockCanvasAndPost(canvas)
            var attempts = 0
            surfaceTexture.updateTexImage()
            while (surfaceTexture.timestamp == 0L && attempts < 25) {
                attempts += 1
                Thread.sleep(2)
                surfaceTexture.updateTexImage()
            }
            if (surfaceTexture.timestamp == 0L) {
                return "stage 2 frame never arrived"
            }
            val matrix = FloatArray(16)
            surfaceTexture.getTransformMatrix(matrix)
            for (layout in listOf(PixelBufferLayout.ycbcr420Full, PixelBufferLayout.ycbcr420Video)) {
                val backing = PixelBufferGl.allocatePlanar(4, 4, layout)
                    ?: return "4x4 ${layout.name} planes incomplete"
                val scratch = PixelBufferGl.allocate(4, 4)
                try {
                    if (scratch == null) {
                        return "4x4 scratch incomplete"
                    }
                    GlRenderer.drawOes(texture, matrix, CVPixelBuffer(backing, null, tagFor(layout, 0)), 0, false)
                    val read = drawAndRead(backing, scratch)
                    if (!matches(read, expected)) {
                        return "stage 2 ${layout.name} read ${read.joinToString(",")}, " +
                            "expected ${expected.joinToString(",")}"
                    }
                } finally {
                    PixelBufferGl.delete(backing)
                    scratch?.let { PixelBufferGl.delete(it) }
                }
            }
            return null
        } finally {
            surface.release()
            surfaceTexture.release()
            GlRenderer.deleteTexture(texture)
        }
    }

    private fun drawAndRead(backing: PixelBufferBacking, scratch: PixelBufferBacking): IntArray {
        val source = CVPixelBuffer(backing, null, tagFor(backing.layout, 0))
        GlRenderer.bind(CVPixelBuffer(scratch, null, kCVPixelFormatType_32BGRA))
        GlRenderer.drawBuffer(
            source = source,
            targetWidth = scratch.width,
            targetHeight = scratch.height,
            mode = GlRenderer.ScalingMode.stretch,
            rotationDegreesCw = 0,
            mirror = false,
            flipVertical = false,
        )
        return readCentre()
    }

    private fun drawEffectsAndRead(backing: PixelBufferBacking, scratch: PixelBufferBacking): IntArray? {
        Gl.ensureCapabilities()
        if (!Gl.es3) {
            return null
        }
        val program = Gl.program(Glsl.yccProbeSource()) ?: return null
        val layout = backing.layout
        val descriptor = descriptorFor(layout)
        val source = CVPixelBuffer(backing, null, tagFor(layout, 0))
        val width = backing.width.toFloat()
        val height = backing.height.toFloat()
        GlRenderer.bind(CVPixelBuffer(scratch, null, kCVPixelFormatType_32BGRA))
        GlRenderer.clear(0f, 0f, 0f, 0f)
        GLES20.glUseProgram(program.id)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, source.chromaTexture)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, source.lumaTexture)
        GLES20.glUniform1i(program.location("sLuma"), 0)
        GLES20.glUniform1i(program.location("sChroma"), 1)
        GLES20.glUniform4f(program.location("uSize"), width, height, width, height)
        GLES20.glUniform2f(
            program.location("uChromaSize"),
            2f * layout.chromaWidth(backing.width),
            2f * layout.chromaHeight(backing.height),
        )
        GLES20.glUniformMatrix3fv(program.location("uMatrix"), 1, false, descriptor.inverseMat3, 0)
        val offset = descriptor.offsetVector
        GLES20.glUniform3f(program.location("uOffset"), offset[0], offset[1], offset[2])
        Gl.drawQuad(program, -1f, -1f, 1f, 1f)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glUseProgram(0)
        return readCentre()
    }

    private fun readCentre(): IntArray {
        val pixels = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder())
        GLES20.glReadPixels(1, 1, 1, 1, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixels)
        return IntArray(4) { index -> pixels.get(index).toInt() and 0xFF }
    }

    private fun matches(read: IntArray, expected: IntArray): Boolean {
        for (index in 0 until 3) {
            if (abs(read[index] - expected[index]) > PROBE_TOLERANCE) {
                return false
            }
        }
        return read[3] == 255
    }
}

package com.moblin.android.platform.video

import android.graphics.Bitmap
import android.media.MediaFormat
import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.GLUtils
import android.os.SystemClock
import android.util.Log
import android.util.Size
import com.moblin.android.BuildConfig
import com.moblin.android.media.MediaSample
import com.moblin.android.platform.core.PipelineStats
import com.moblin.android.platform.core.PipelineThread
import java.io.ByteArrayOutputStream
import java.lang.ref.PhantomReference
import java.lang.ref.Reference
import java.lang.ref.ReferenceQueue
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max
import kotlin.math.roundToInt

private const val TAG = "MoblinPipeline"

const val kCVReturnSuccess = 0
const val kCVPixelFormatType_32BGRA = 0x42475241
const val kCVPixelFormatType_32RGBA = 0x52474241
const val kCVPixelFormatType_420YpCbCr8BiPlanarFullRange = 0x34323066
const val kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange = 0x34323076
const val kCVPixelFormatType_420YpCbCr10BiPlanarFullRange = 0x78663230
const val kCVPixelFormatType_420YpCbCr10BiPlanarVideoRange = 0x78343230
const val kCVPixelBufferWidthKey = "Width"
const val kCVPixelBufferHeightKey = "Height"
const val kCVPixelBufferPixelFormatTypeKey = "PixelFormatType"
const val kCVPixelBufferIOSurfacePropertiesKey = "IOSurfaceProperties"
const val kCVPixelBufferMetalCompatibilityKey = "MetalCompatibility"

internal enum class PixelBufferLayout(val planeCount: Int, val label: String) {
    rgba8(1, "rgba"),
    ycbcr420Full(2, "ycc"),
    ycbcr420Video(2, "ycc"),
    ;

    val isPlanar: Boolean
        get() = planeCount > 1

    fun chromaWidth(width: Int): Int {
        return (width + 1) / 2
    }

    fun chromaHeight(height: Int): Int {
        return (height + 1) / 2
    }

    fun bytes(width: Int, height: Int): Long {
        if (!isPlanar) {
            return 4L * width * height
        }
        return width.toLong() * height + 2L * chromaWidth(width) * chromaHeight(height)
    }
}

internal class PixelBufferBacking(
    val texture: Int,
    val framebuffer: Int,
    val width: Int,
    val height: Int,
    val layout: PixelBufferLayout = PixelBufferLayout.rgba8,
    val chromaTexture: Int = 0,
    val chromaFramebuffer: Int = 0,
) {
    @Volatile
    var generation = 0

    val bytes: Long
        get() = layout.bytes(width, height)
}

internal class PixelBufferPoolState(
    val width: Int,
    val height: Int,
    val maximumBufferCount: Int,
    var name: String,
    val layout: PixelBufferLayout = PixelBufferLayout.rgba8,
) {
    val free = ArrayDeque<PixelBufferBacking>()
    var allocated = 0
    var allocatedBytes = 0L
    var leased = 0
    var unleased = 0
    var released = false
    var lastExhaustedLogMs = 0L
    var trimIdle = false
    var peakInUse = 0
}

internal object PixelBufferPlanar {
    private const val MAXIMUM_LOGGED_SITES = 64
    private val loggedSites = ConcurrentHashMap.newKeySet<String>()
    val misses = AtomicLong()
    val badTargets = AtomicLong()

    fun miss(buffer: CVPixelBuffer, member: String) {
        misses.incrementAndGet()
        PipelineStats.increment("pbPlanarMiss")
        log(buffer, "Planar pixel buffer read through .$member")
    }

    fun badTarget(buffer: CVPixelBuffer, site: String) {
        badTargets.incrementAndGet()
        PipelineStats.increment("pbBadTarget")
        log(buffer, "Planar pixel buffer refused as $site")
    }

    private fun log(buffer: CVPixelBuffer, message: String) {
        if (loggedSites.size >= MAXIMUM_LOGGED_SITES) {
            return
        }
        val trace = Throwable(message)
        val caller = trace.stackTrace.firstOrNull { element ->
            element.className != PixelBufferPlanar::class.java.name &&
                element.className != CVPixelBuffer::class.java.name
        }
        val key = "$message ${caller?.className}.${caller?.methodName}:${caller?.lineNumber}"
        if (!loggedSites.add(key)) {
            return
        }
        Log.w(TAG, "$message: $buffer at $caller", trace)
    }
}

class CVPixelBuffer internal constructor(
    internal val backing: PixelBufferBacking,
    internal val poolState: PixelBufferPoolState?,
    val pixelFormatType: Int,
    leased: Boolean = false,
) {
    internal val generation: Int = backing.generation

    internal val leaseCount = AtomicInteger(if (leased) 1 else UNLEASED)

    internal var reference: Any? = null

    internal val attachments = ConcurrentHashMap<String, Any>()

    val isValid: Boolean
        get() = backing.generation == generation

    val width: Int
        get() = backing.width

    val height: Int
        get() = backing.height

    val size: Size = Size(backing.width, backing.height)

    internal val layout: PixelBufferLayout
        get() = backing.layout

    internal val planeCount: Int
        get() = backing.layout.planeCount

    val texture: Int
        get() {
            if (backing.layout.isPlanar) {
                PixelBufferPlanar.miss(this, "texture")
                return 0
            }
            return backing.texture
        }

    val framebuffer: Int
        get() {
            if (backing.layout.isPlanar) {
                PixelBufferPlanar.miss(this, "framebuffer")
                return 0
            }
            return backing.framebuffer
        }

    internal val lumaTexture: Int
        get() = backing.texture

    internal val lumaFramebuffer: Int
        get() = backing.framebuffer

    internal val chromaTexture: Int
        get() = backing.chromaTexture

    internal val chromaFramebuffer: Int
        get() = backing.chromaFramebuffer

    fun isPortrait(): Boolean {
        return height > width
    }

    fun checkReadable(site: String): Boolean {
        if (isValid) {
            return true
        }
        PixelBufferStale.report(this, site)
        return false
    }

    fun checkRenderable(site: String): Boolean {
        if (!backing.layout.isPlanar) {
            return true
        }
        PixelBufferPlanar.badTarget(this, site)
        return false
    }

    fun readableTexture(site: String): Int {
        return if (checkReadable(site)) texture else 0
    }

    fun toBitmap(maxLongSide: Int = 0): Bitmap? {
        val longSide = max(width, height)
        val outputSize = if (maxLongSide in 1 until longSide) {
            scaledSize(maxLongSide)
        } else {
            size
        }
        return readBitmap(outputSize)
    }

    fun jpegData(longSide: Int, compressionQuality: Float): ByteArray? {
        val outputSize = if (longSide > 0) scaledSize(longSide) else size
        val bitmap = readBitmap(outputSize) ?: return null
        return try {
            val output = ByteArrayOutputStream()
            val quality = (compressionQuality.coerceIn(0f, 1f) * 100).roundToInt()
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)) {
                null
            } else {
                output.toByteArray()
            }
        } catch (error: Throwable) {
            Log.w(TAG, "JPEG compression failed: $error")
            null
        } finally {
            bitmap.recycle()
        }
    }

    override fun toString(): String {
        val pool = poolState?.name ?: "unpooled"
        return "CVPixelBuffer(${width}x$height, ${layout.label}, texture=${backing.texture}, $pool)"
    }

    private fun scaledSize(longSide: Int): Size {
        val scale = longSide.toDouble() / max(width, height)
        return Size(max(1, (width * scale).roundToInt()), max(1, (height * scale).roundToInt()))
    }

    private fun readBitmap(outputSize: Size): Bitmap? {
        return try {
            PipelineThread.runSync(timeoutMs = 3000) {
                PixelBufferGl.readBitmap(this, outputSize.width, outputSize.height)
            }
        } catch (error: Throwable) {
            Log.w(TAG, "Pixel buffer readback failed: $error")
            null
        }
    }
}

typealias CVImageBuffer = CVPixelBuffer

enum class CVAttachmentMode {
    shouldNotPropagate,
    shouldPropagate,
}

fun CVBufferSetAttachment(buffer: CVPixelBuffer, key: String, value: Any, attachmentMode: CVAttachmentMode) {
    buffer.attachments[key] = value
}

fun CVBufferSetAttachments(buffer: CVPixelBuffer, theAttachments: Map<String, Any>, attachmentMode: CVAttachmentMode) {
    buffer.attachments.putAll(theAttachments)
}

fun CVBufferCopyAttachment(buffer: CVPixelBuffer, key: String): Any? {
    return buffer.attachments[key]
}

class CVPixelBufferPool internal constructor(
    val width: Int,
    val height: Int,
    val pixelFormatType: Int,
    val maximumBufferCount: Int,
    internal val layout: PixelBufferLayout,
) {
    constructor(
        width: Int,
        height: Int,
        pixelFormatType: Int,
        maximumBufferCount: Int = 16,
    ) : this(width, height, pixelFormatType, maximumBufferCount, PixelBufferLayout.rgba8)

    internal val state = PixelBufferPoolState(width, height, maximumBufferCount, "pool", layout)

    var name: String
        get() = state.name
        set(value) {
            state.name = value
        }

    init {
        PixelBufferReaper.trackPool(this, state)
    }

    fun createPixelBuffer(): CVPixelBuffer? {
        if (!PipelineThread.isCurrent()) {
            return try {
                PipelineThread.runSync { PixelBufferReaper.obtain(state, pixelFormatType, leased = false) }
            } catch (error: Throwable) {
                Log.w(TAG, "createPixelBuffer failed: $error")
                null
            }
        }
        return PixelBufferReaper.obtain(state, pixelFormatType, leased = true)
    }

    fun invalidate() {
        PixelBufferReaper.invalidate(state)
    }

    companion object {
        fun matching(existing: Any?, sampleBuffer: MediaSample): CVPixelBufferPool? {
            val imageBuffer = sampleBuffer.imageBuffer ?: return null
            val pool = existing as? CVPixelBufferPool
            if (pool != null && pool.width == imageBuffer.width && pool.height == imageBuffer.height) {
                return pool
            }
            pool?.invalidate()
            return CVPixelBufferPool(
                width = imageBuffer.width,
                height = imageBuffer.height,
                pixelFormatType = imageBuffer.pixelFormatType,
                maximumBufferCount = 64,
            ).also { it.name = "buffered" }
        }
    }
}

fun CVPixelBufferPoolCreate(attributes: Map<String, Any>): CVPixelBufferPool? {
    val width = (attributes[kCVPixelBufferWidthKey] as? Number)?.toInt() ?: return null
    val height = (attributes[kCVPixelBufferHeightKey] as? Number)?.toInt() ?: return null
    if (width <= 0 || height <= 0) {
        return null
    }
    val pixelFormatType = (attributes[kCVPixelBufferPixelFormatTypeKey] as? Number)?.toInt()
        ?: kCVPixelFormatType_32BGRA
    return CVPixelBufferPool(width = width, height = height, pixelFormatType = pixelFormatType)
}

fun CVPixelBufferPoolCreatePixelBuffer(pool: CVPixelBufferPool): CVPixelBuffer? {
    return pool.createPixelBuffer()
}

fun CVPixelBufferGetWidth(pixelBuffer: CVPixelBuffer): Int {
    return pixelBuffer.width
}

fun CVPixelBufferGetHeight(pixelBuffer: CVPixelBuffer): Int {
    return pixelBuffer.height
}

fun CVPixelBufferGetPixelFormatType(pixelBuffer: CVPixelBuffer): Int {
    return pixelBuffer.pixelFormatType
}

fun CVPixelBufferGetPlaneCount(pixelBuffer: CVPixelBuffer): Int {
    return if (pixelBuffer.layout.isPlanar) pixelBuffer.planeCount else 0
}

fun CVPixelBufferIsPlanar(pixelBuffer: CVPixelBuffer): Boolean {
    return pixelBuffer.layout.isPlanar
}

fun VTPixelTransferSessionTransferImage(from: CVPixelBuffer, to: CVPixelBuffer) {
    if (!PipelineThread.isCurrent()) {
        try {
            PipelineThread.runSync { VTPixelTransferSessionTransferImage(from, to) }
        } catch (error: Throwable) {
            Log.w(TAG, "VTPixelTransferSessionTransferImage failed: $error")
        }
        return
    }
    PixelBufferGl.transfer(from, to)
}

fun makeBlackPixelBuffer(width: Int, height: Int): CVPixelBuffer? {
    if (width <= 0 || height <= 0) {
        return null
    }
    if (!PipelineThread.isCurrent()) {
        return try {
            PipelineThread.runSync { makeBlackPixelBuffer(width, height) }
        } catch (error: Throwable) {
            Log.w(TAG, "makeBlackPixelBuffer failed: $error")
            null
        }
    }
    val backing = PixelBufferGl.allocate(width, height) ?: return null
    PixelBufferGl.clear(backing, 0f, 0f, 0f, 1f)
    return PixelBufferReaper.wrapUnpooled(backing, kCVPixelFormatType_32BGRA)
}

fun makeCopy(sampleBuffer: MediaSample, into: CVPixelBuffer): MediaSample? {
    val source = sampleBuffer.imageBuffer ?: return null
    VTPixelTransferSessionTransferImage(source, into)
    return MediaSample(
        data = ByteArray(0),
        presentationTimeUs = sampleBuffer.presentationTimeUs,
        isKeyFrame = sampleBuffer.isKeyFrame,
        format = sampleBuffer.format,
        imageBuffer = into,
        durationUs = sampleBuffer.durationUs,
        decodeTimeStampUs = sampleBuffer.decodeTimeStampUs,
    )
}

private val videoFormatDescriptions = HashMap<Long, MediaFormat>()

fun CMVideoFormatDescriptionCreateForImageBuffer(imageBuffer: CVPixelBuffer): MediaFormat {
    val key = (imageBuffer.width.toLong() shl 32) or (imageBuffer.height.toLong() and 0xFFFF_FFFFL)
    return synchronized(videoFormatDescriptions) {
        videoFormatDescriptions.getOrPut(key) {
            MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_RAW, imageBuffer.width, imageBuffer.height)
        }
    }
}

fun CMVideoFormatDescriptionMatchesImageBuffer(description: MediaFormat, imageBuffer: CVPixelBuffer): Boolean {
    return description.getInteger(MediaFormat.KEY_WIDTH) == imageBuffer.width &&
        description.getInteger(MediaFormat.KEY_HEIGHT) == imageBuffer.height
}

internal object PixelBufferGl {
    private class Plane(val texture: Int, val framebuffer: Int, val status: Int)

    fun allocate(
        width: Int,
        height: Int,
        layout: PixelBufferLayout = PixelBufferLayout.rgba8,
    ): PixelBufferBacking? {
        if (!EglCore.isReady) {
            return null
        }
        if (layout.isPlanar && YCbCrStorage.isEnabled) {
            val planar = allocatePlanar(width, height, layout)
            if (planar != null) {
                return planar
            }
        }
        if (layout.isPlanar) {
            PipelineStats.increment("pbFallback")
        }
        val previousFramebuffer = GlRenderer.currentFramebuffer()
        val plane = createPlane(width, height, GLES20.GL_RGBA, GLES20.GL_RGBA)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, previousFramebuffer)
        if (plane.status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            Log.e(TAG, "Framebuffer ${width}x$height incomplete: 0x${Integer.toHexString(plane.status)}")
            deletePlane(plane.texture, plane.framebuffer)
            return null
        }
        return PixelBufferBacking(plane.texture, plane.framebuffer, width, height)
    }

    fun allocatePlanar(width: Int, height: Int, layout: PixelBufferLayout): PixelBufferBacking? {
        if (!EglCore.isReady || !layout.isPlanar) {
            return null
        }
        val previousFramebuffer = GlRenderer.currentFramebuffer()
        val luma = createPlane(width, height, GLES30.GL_R8, GLES30.GL_RED)
        val chroma = createPlane(layout.chromaWidth(width), layout.chromaHeight(height), GLES30.GL_RG8, GLES30.GL_RG)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, previousFramebuffer)
        if (luma.status != GLES20.GL_FRAMEBUFFER_COMPLETE || chroma.status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            deletePlane(luma.texture, luma.framebuffer)
            deletePlane(chroma.texture, chroma.framebuffer)
            YCbCrStorage.disable(
                "planar framebuffer ${width}x$height incomplete: luma 0x${Integer.toHexString(luma.status)}, " +
                    "chroma 0x${Integer.toHexString(chroma.status)}"
            )
            return null
        }
        return PixelBufferBacking(
            luma.texture,
            luma.framebuffer,
            width,
            height,
            layout,
            chroma.texture,
            chroma.framebuffer,
        )
    }

    private fun createPlane(width: Int, height: Int, internalFormat: Int, format: Int): Plane {
        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        val texture = ids[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D,
            0,
            internalFormat,
            width,
            height,
            0,
            format,
            GLES20.GL_UNSIGNED_BYTE,
            null
        )
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
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
        return Plane(texture, framebuffer, GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER))
    }

    private fun deletePlane(texture: Int, framebuffer: Int) {
        GLES20.glDeleteFramebuffers(1, intArrayOf(framebuffer), 0)
        GLES20.glDeleteTextures(1, intArrayOf(texture), 0)
    }

    fun delete(backing: PixelBufferBacking) {
        if (!EglCore.isReady) {
            return
        }
        deletePlane(backing.texture, backing.framebuffer)
        if (backing.layout.isPlanar) {
            deletePlane(backing.chromaTexture, backing.chromaFramebuffer)
        }
    }

    fun clear(backing: PixelBufferBacking, r: Float, g: Float, b: Float, a: Float) {
        if (!EglCore.isReady) {
            return
        }
        if (backing.layout.isPlanar) {
            val ycc = YCbCrStorage.descriptorFor(backing.layout).encode(r, g, b)
            clearPlanes(backing, ycc[0], ycc[1], ycc[2])
            return
        }
        val previousFramebuffer = GlRenderer.currentFramebuffer()
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, backing.framebuffer)
        GLES20.glViewport(0, 0, backing.width, backing.height)
        GlRenderer.clear(r, g, b, a)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, previousFramebuffer)
    }

    fun clearPlanes(backing: PixelBufferBacking, y: Float, cb: Float, cr: Float) {
        if (!EglCore.isReady || !backing.layout.isPlanar) {
            return
        }
        val previousFramebuffer = GlRenderer.currentFramebuffer()
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, backing.framebuffer)
        GLES20.glViewport(0, 0, backing.width, backing.height)
        GlRenderer.clear(y, 0f, 0f, 1f)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, backing.chromaFramebuffer)
        GLES20.glViewport(
            0,
            0,
            backing.layout.chromaWidth(backing.width),
            backing.layout.chromaHeight(backing.height)
        )
        GlRenderer.clear(cb, cr, 0f, 1f)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, previousFramebuffer)
    }

    fun transfer(from: CVPixelBuffer, to: CVPixelBuffer) {
        if (!EglCore.isReady || from.backing === to.backing || !to.checkReadable("pixel transfer target")) {
            return
        }
        if (!to.checkRenderable("pixel transfer target")) {
            return
        }
        if (!from.checkReadable("pixel transfer")) {
            clear(to.backing, 0f, 0f, 0f, 1f)
            return
        }
        val previousFramebuffer = GlRenderer.currentFramebuffer()
        if (from.layout.isPlanar) {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, to.framebuffer)
            GlRenderer.drawBuffer(
                source = from,
                targetWidth = to.width,
                targetHeight = to.height,
                mode = GlRenderer.ScalingMode.stretch,
                rotationDegreesCw = 0,
                mirror = false,
                flipVertical = false,
            )
        } else if (EglCore.glMajorVersion >= 3) {
            GLES30.glBindFramebuffer(GLES30.GL_READ_FRAMEBUFFER, from.framebuffer)
            GLES30.glBindFramebuffer(GLES30.GL_DRAW_FRAMEBUFFER, to.framebuffer)
            val filter = if (from.width == to.width && from.height == to.height) {
                GLES30.GL_NEAREST
            } else {
                GLES30.GL_LINEAR
            }
            GLES30.glBlitFramebuffer(
                0,
                0,
                from.width,
                from.height,
                0,
                0,
                to.width,
                to.height,
                GLES30.GL_COLOR_BUFFER_BIT,
                filter
            )
        } else {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, to.framebuffer)
            GlRenderer.drawTexture(
                texture = from.texture,
                oes = false,
                texMatrix = null,
                sourceWidth = from.width,
                sourceHeight = from.height,
                targetWidth = to.width,
                targetHeight = to.height,
                mode = GlRenderer.ScalingMode.stretch,
                rotationDegreesCw = 0,
                mirror = false,
                flipVertical = false,
            )
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, previousFramebuffer)
        GlRenderer.checkError("transfer")
    }

    fun upload(bitmap: Bitmap, to: CVPixelBuffer) {
        if (!EglCore.isReady || bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) {
            return
        }
        if (!to.checkReadable("bitmap upload target") || !to.checkRenderable("bitmap upload target")) {
            return
        }
        val previousFramebuffer = GlRenderer.currentFramebuffer()
        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        val texture = ids[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        try {
            val uploadable = if (bitmap.config == Bitmap.Config.HARDWARE) {
                bitmap.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                bitmap
            }
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, uploadable, 0)
            if (uploadable !== bitmap) {
                uploadable.recycle()
            }
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, to.framebuffer)
            GlRenderer.drawTexture(
                texture = texture,
                oes = false,
                texMatrix = null,
                sourceWidth = bitmap.width,
                sourceHeight = bitmap.height,
                targetWidth = to.width,
                targetHeight = to.height,
                mode = GlRenderer.ScalingMode.stretch,
                rotationDegreesCw = 0,
                mirror = false,
                flipVertical = true,
            )
        } catch (error: Throwable) {
            Log.w(TAG, "Bitmap upload failed: $error")
        } finally {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, previousFramebuffer)
            GLES20.glDeleteTextures(1, intArrayOf(texture), 0)
        }
    }

    fun readBitmap(source: CVPixelBuffer, width: Int, height: Int): Bitmap? {
        if (!EglCore.isReady || width <= 0 || height <= 0) {
            return null
        }
        if (!source.checkReadable("readback")) {
            return null
        }
        val scratch = allocate(width, height) ?: return null
        val previousFramebuffer = GlRenderer.currentFramebuffer()
        try {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, scratch.framebuffer)
            GlRenderer.drawBuffer(
                source = source,
                targetWidth = width,
                targetHeight = height,
                mode = GlRenderer.ScalingMode.stretch,
                rotationDegreesCw = 0,
                mirror = false,
                flipVertical = true,
            )
            val pixels = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder())
            GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixels)
            GlRenderer.checkError("readBitmap")
            pixels.rewind()
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.copyPixelsFromBuffer(pixels)
            return bitmap
        } finally {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, previousFramebuffer)
            delete(scratch)
        }
    }
}

internal object PixelBufferReaper {
    private const val FORCED_GC_INTERVAL_MS = 100L
    private const val TRIM_INTERVAL_MS = 5000L
    private const val TRIM_SPARE_BUFFERS = 2

    private class BufferReference(
        buffer: CVPixelBuffer,
        queue: ReferenceQueue<Any>,
        val backing: PixelBufferBacking,
        val poolState: PixelBufferPoolState?,
        val leased: Boolean,
    ) : PhantomReference<Any>(buffer, queue) {
        var recycled = false
    }

    private class PoolReference(
        pool: CVPixelBufferPool,
        queue: ReferenceQueue<Any>,
        val state: PixelBufferPoolState,
    ) : PhantomReference<Any>(pool, queue)

    private val queue = ReferenceQueue<Any>()
    private val references = HashSet<Reference<*>>()
    private val pools = LinkedHashSet<PixelBufferPoolState>()
    private var unpooledCount = 0
    private var lastGcRequestMs = 0L
    private var lastNotReadyLogMs = 0L
    private var lastTrimMs = 0L
    internal var trimEnabled = BuildConfig.POOL_TRIM
    val leakedLeases = AtomicLong()
    private val gcExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "MoblinPixelBufferGc").apply {
            isDaemon = true
            priority = Thread.MIN_PRIORITY
        }
    }

    init {
        val names = listOf(
            "poolExhausted",
            "gcRequested",
            "leaseReleased",
            "staleBuffer",
            "pbPlanarMiss",
            "pbBadTarget",
            "pbFallback",
        )
        for (name in names) {
            PipelineStats.increment(name, 0)
        }
        PipelineStats.addReporter { report() }
        PipelineStats.addPipelineTick { poll() }
        PipelineStats.addPipelineTick { trimIdle(SystemClock.uptimeMillis()) }
    }

    fun trackPool(pool: CVPixelBufferPool, state: PixelBufferPoolState) {
        synchronized(this) {
            references.add(PoolReference(pool, queue, state))
            pools.add(state)
        }
    }

    fun wrapUnpooled(backing: PixelBufferBacking, pixelFormatType: Int): CVPixelBuffer {
        val buffer = CVPixelBuffer(backing, null, pixelFormatType)
        val reference = BufferReference(buffer, queue, backing, null, leased = false)
        buffer.reference = reference
        synchronized(this) {
            references.add(reference)
            unpooledCount += 1
        }
        return buffer
    }

    fun obtain(state: PixelBufferPoolState, pixelFormatType: Int, leased: Boolean): CVPixelBuffer? {
        poll()
        if (!EglCore.isReady) {
            val nowMs = SystemClock.uptimeMillis()
            if (nowMs - lastNotReadyLogMs > 1000) {
                lastNotReadyLogMs = nowMs
                Log.w(TAG, "createPixelBuffer: EGL is not ready")
            }
            return null
        }
        val backing = takeBacking(state) ?: return null
        val buffer = CVPixelBuffer(backing, state, pixelFormatType, leased)
        val reference = BufferReference(buffer, queue, backing, state, leased)
        buffer.reference = reference
        synchronized(this) {
            references.add(reference)
            if (leased) {
                state.leased += 1
            } else {
                state.unleased += 1
            }
            state.peakInUse = max(state.peakInUse, state.allocated - state.free.size)
        }
        if (leased) {
            PixelBufferTurn.defer(buffer)
        }
        return buffer
    }

    fun recycle(buffer: CVPixelBuffer) {
        var toDelete: PixelBufferBacking? = null
        synchronized(this) {
            val reference = buffer.reference as? BufferReference
            if (reference != null) {
                if (reference.recycled) {
                    return
                }
                reference.recycled = true
                reference.clear()
                references.remove(reference)
            }
            val backing = buffer.backing
            backing.generation += 1
            val state = buffer.poolState
            if (state == null) {
                unpooledCount -= 1
                toDelete = backing
            } else {
                state.leased -= 1
                if (state.released) {
                    state.allocated -= 1
                    state.allocatedBytes -= backing.bytes
                    toDelete = backing
                    if (state.allocated <= 0) {
                        pools.remove(state)
                    }
                } else {
                    state.free.addLast(backing)
                }
            }
        }
        PipelineStats.increment("leaseReleased")
        toDelete?.let { delete(listOf(it)) }
    }

    fun invalidate(state: PixelBufferPoolState) {
        val toDelete = synchronized(this) {
            if (state.released) {
                return
            }
            state.released = true
            val free = state.free.toList()
            state.allocated -= free.size
            state.allocatedBytes -= free.sumOf { it.bytes }
            state.free.clear()
            if (state.allocated <= 0) {
                pools.remove(state)
            }
            free
        }
        delete(toDelete)
    }

    private fun takeBacking(state: PixelBufferPoolState): PixelBufferBacking? {
        val reused = synchronized(this) { state.free.removeFirstOrNull() }
        if (reused != null) {
            return reused
        }
        val canAllocate = synchronized(this) { state.allocated < state.maximumBufferCount }
        if (canAllocate) {
            val allocated = PixelBufferGl.allocate(state.width, state.height, state.layout) ?: return null
            synchronized(this) {
                state.allocated += 1
                state.allocatedBytes += allocated.bytes
            }
            return allocated
        }
        val (leased, unleased) = synchronized(this) { Pair(state.leased, state.unleased) }
        val pendingReleases = PixelBufferTurn.pendingCount(state)
        var gcRequested = false
        if (pendingReleases == 0 && unleased > 0) {
            gcRequested = requestGc(minimumIntervalMs = FORCED_GC_INTERVAL_MS)
            awaitReclaimed(timeoutMs = 3)
        } else if (pendingReleases == 0 && leased > 0) {
            gcRequested = requestGc(minimumIntervalMs = 1000)
        }
        val reclaimed = synchronized(this) { state.free.removeFirstOrNull() }
        if (reclaimed != null) {
            return reclaimed
        }
        val nowMs = SystemClock.uptimeMillis()
        if (nowMs - state.lastExhaustedLogMs > 1000) {
            state.lastExhaustedLogMs = nowMs
            Log.w(
                TAG,
                "pool exhausted ${state.width}x${state.height} (${state.name}): $leased leased, $unleased unleased, " +
                    "$pendingReleases releases pending this turn, gc ${if (gcRequested) "requested" else "not requested"}",
            )
        }
        PipelineStats.increment("poolExhausted")
        return null
    }

    fun poll() {
        if (!PipelineThread.isCurrent()) {
            return
        }
        while (true) {
            val reference = queue.poll() ?: break
            handle(reference)
        }
    }

    private fun awaitReclaimed(timeoutMs: Long) {
        if (!PipelineThread.isCurrent()) {
            return
        }
        val reference = try {
            queue.remove(timeoutMs)
        } catch (error: InterruptedException) {
            null
        }
        if (reference != null) {
            handle(reference)
        }
        poll()
    }

    private fun handle(reference: Reference<*>) {
        var toDelete: List<PixelBufferBacking> = emptyList()
        var leaked = false
        synchronized(this) {
            references.remove(reference)
            when (reference) {
                is BufferReference -> {
                    if (reference.recycled) {
                        return
                    }
                    reference.recycled = true
                    reference.backing.generation += 1
                    leaked = reference.leased
                    val state = reference.poolState
                    if (state == null) {
                        unpooledCount -= 1
                        toDelete = listOf(reference.backing)
                    } else {
                        if (reference.leased) {
                            state.leased -= 1
                        } else {
                            state.unleased -= 1
                        }
                        if (state.released) {
                            state.allocated -= 1
                            state.allocatedBytes -= reference.backing.bytes
                            toDelete = listOf(reference.backing)
                            if (state.allocated <= 0) {
                                pools.remove(state)
                            }
                        } else {
                            state.free.addLast(reference.backing)
                        }
                    }
                }
                is PoolReference -> {
                    val state = reference.state
                    state.released = true
                    toDelete = state.free.toList()
                    state.allocated -= state.free.size
                    state.allocatedBytes -= toDelete.sumOf { it.bytes }
                    state.free.clear()
                    pools.remove(state)
                }
            }
        }
        if (leaked) {
            leakedLeases.incrementAndGet()
            PipelineStats.increment("leaseLeaked")
        }
        delete(toDelete)
    }

    private fun delete(backings: List<PixelBufferBacking>) {
        if (backings.isEmpty()) {
            return
        }
        if (PipelineThread.isCurrent()) {
            for (backing in backings) {
                PixelBufferGl.delete(backing)
            }
        } else {
            PipelineThread.post {
                for (backing in backings) {
                    PixelBufferGl.delete(backing)
                }
            }
        }
    }

    private fun requestGc(minimumIntervalMs: Long): Boolean {
        val nowMs = SystemClock.uptimeMillis()
        synchronized(this) {
            if (nowMs - lastGcRequestMs < minimumIntervalMs) {
                return false
            }
            lastGcRequestMs = nowMs
        }
        PipelineStats.increment("gcRequested")
        gcExecutor.execute {
            Runtime.getRuntime().gc()
            PipelineThread.post { poll() }
        }
        return true
    }

    internal fun trimIdle(nowMs: Long) {
        if (!trimEnabled || !PipelineThread.isCurrent()) {
            return
        }
        val toDelete = ArrayList<PixelBufferBacking>()
        synchronized(this) {
            if (nowMs - lastTrimMs < TRIM_INTERVAL_MS) {
                return
            }
            lastTrimMs = nowMs
            for (state in pools) {
                val inUse = state.allocated - state.free.size
                if (!state.trimIdle || state.released) {
                    state.peakInUse = inUse
                    continue
                }
                val keep = max(state.peakInUse, inUse) + TRIM_SPARE_BUFFERS
                var count = 0
                var bytes = 0L
                while (state.allocated > keep) {
                    val backing = state.free.removeLastOrNull() ?: break
                    state.allocated -= 1
                    state.allocatedBytes -= backing.bytes
                    bytes += backing.bytes
                    toDelete.add(backing)
                    count += 1
                }
                state.peakInUse = inUse
                if (count > 0) {
                    val megabytes = megabytes(bytes)
                    PipelineStats.increment("pbTrim", count.toLong())
                    Log.i(TAG, "pbTrim ${state.name} ${state.width}x${state.height}: $count buffers, ${megabytes}MB")
                }
            }
        }
        delete(toDelete)
    }

    private fun megabytes(bytes: Long): Long {
        return (bytes + 500_000) / 1_000_000
    }

    internal fun report(): String? {
        synchronized(this) {
            var totalBytes = 0L
            val parts = pools.filter { it.allocated > 0 }.map {
                val bytes = it.allocatedBytes
                totalBytes += bytes
                buildString {
                    append("${it.name} ${it.width}x${it.height} ${it.layout.label} ")
                    append("${it.allocated - it.free.size}/${it.allocated}/${it.maximumBufferCount} ")
                    append("${megabytes(bytes)}MB")
                    if (it.unleased > 0) {
                        append(" unleased ${it.unleased}")
                    }
                    if (it.released) {
                        append(" released")
                    }
                }
            }.toMutableList()
            PipelineStats.gauge("pbMB", megabytes(totalBytes))
            if (unpooledCount > 0) {
                parts.add("unpooled $unpooledCount")
            }
            if (parts.isEmpty()) {
                return null
            }
            return "pools " + parts.joinToString(", ")
        }
    }
}

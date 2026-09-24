package com.moblin.android.platform.video

import android.graphics.Bitmap
import android.media.MediaFormat
import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.GLUtils
import android.os.SystemClock
import android.util.Log
import android.util.Size
import com.moblin.android.media.MediaSample
import com.moblin.android.platform.core.PipelineStats
import com.moblin.android.platform.core.PipelineThread
import java.io.ByteArrayOutputStream
import java.lang.ref.PhantomReference
import java.lang.ref.Reference
import java.lang.ref.ReferenceQueue
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.roundToInt

private const val TAG = "MoblinPipeline"

const val kCVReturnSuccess = 0
const val kCVPixelFormatType_32BGRA = 0x42475241
const val kCVPixelFormatType_32RGBA = 0x52474241
const val kCVPixelFormatType_420YpCbCr8BiPlanarFullRange = 0x34323066
const val kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange = 0x34323076
const val kCVPixelBufferWidthKey = "Width"
const val kCVPixelBufferHeightKey = "Height"
const val kCVPixelBufferPixelFormatTypeKey = "PixelFormatType"
const val kCVPixelBufferIOSurfacePropertiesKey = "IOSurfaceProperties"
const val kCVPixelBufferMetalCompatibilityKey = "MetalCompatibility"

internal class PixelBufferBacking(val texture: Int, val framebuffer: Int, val width: Int, val height: Int) {
    @Volatile
    var generation = 0
}

internal class PixelBufferPoolState(val width: Int, val height: Int, val maximumBufferCount: Int, var name: String) {
    val free = ArrayDeque<PixelBufferBacking>()
    var allocated = 0
    var leased = 0
    var unleased = 0
    var released = false
    var lastExhaustedLogMs = 0L
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

    val isValid: Boolean
        get() = backing.generation == generation

    val width: Int
        get() = backing.width

    val height: Int
        get() = backing.height

    val size: Size = Size(backing.width, backing.height)

    val texture: Int
        get() = backing.texture

    val framebuffer: Int
        get() = backing.framebuffer

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
        return "CVPixelBuffer(${width}x$height, texture=$texture, ${poolState?.name ?: "unpooled"})"
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

class CVPixelBufferPool(
    val width: Int,
    val height: Int,
    val pixelFormatType: Int,
    val maximumBufferCount: Int = 16,
) {
    internal val state = PixelBufferPoolState(width, height, maximumBufferCount, "pool")

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

internal object PixelBufferGl {
    fun allocate(width: Int, height: Int): PixelBufferBacking? {
        if (!EglCore.isReady) {
            return null
        }
        val previousFramebuffer = GlRenderer.currentFramebuffer()
        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        val texture = ids[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D,
            0,
            GLES20.GL_RGBA,
            width,
            height,
            0,
            GLES20.GL_RGBA,
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
        val status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, previousFramebuffer)
        if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            Log.e(TAG, "Framebuffer ${width}x$height incomplete: 0x${Integer.toHexString(status)}")
            GLES20.glDeleteFramebuffers(1, intArrayOf(framebuffer), 0)
            GLES20.glDeleteTextures(1, intArrayOf(texture), 0)
            return null
        }
        return PixelBufferBacking(texture, framebuffer, width, height)
    }

    fun delete(backing: PixelBufferBacking) {
        if (!EglCore.isReady) {
            return
        }
        GLES20.glDeleteFramebuffers(1, intArrayOf(backing.framebuffer), 0)
        GLES20.glDeleteTextures(1, intArrayOf(backing.texture), 0)
    }

    fun clear(backing: PixelBufferBacking, r: Float, g: Float, b: Float, a: Float) {
        if (!EglCore.isReady) {
            return
        }
        val previousFramebuffer = GlRenderer.currentFramebuffer()
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, backing.framebuffer)
        GLES20.glViewport(0, 0, backing.width, backing.height)
        GlRenderer.clear(r, g, b, a)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, previousFramebuffer)
    }

    fun transfer(from: CVPixelBuffer, to: CVPixelBuffer) {
        if (!EglCore.isReady || from.framebuffer == to.framebuffer || !to.checkReadable("pixel transfer target")) {
            return
        }
        if (!from.checkReadable("pixel transfer")) {
            clear(to.backing, 0f, 0f, 0f, 1f)
            return
        }
        val previousFramebuffer = GlRenderer.currentFramebuffer()
        if (EglCore.glMajorVersion >= 3) {
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
        if (!to.checkReadable("bitmap upload target")) {
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
            GlRenderer.drawTexture(
                texture = source.texture,
                oes = false,
                texMatrix = null,
                sourceWidth = source.width,
                sourceHeight = source.height,
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
    private val gcExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "MoblinPixelBufferGc").apply {
            isDaemon = true
            priority = Thread.MIN_PRIORITY
        }
    }

    init {
        for (name in listOf("poolExhausted", "gcRequested", "leaseReleased", "staleBuffer")) {
            PipelineStats.increment(name, 0)
        }
        PipelineStats.addReporter { report() }
        PipelineStats.addPipelineTick { poll() }
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
                    toDelete = backing
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
            state.free.clear()
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
            val allocated = PixelBufferGl.allocate(state.width, state.height) ?: return null
            synchronized(this) {
                state.allocated += 1
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
                            toDelete = listOf(reference.backing)
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
                    state.free.clear()
                    pools.remove(state)
                }
            }
        }
        if (leaked) {
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

    private fun report(): String? {
        synchronized(this) {
            val parts = pools.filter { it.allocated > 0 }.map {
                buildString {
                    append("${it.name} ${it.width}x${it.height} ${it.allocated - it.free.size}/${it.allocated}/")
                    append("${it.maximumBufferCount}")
                    if (it.unleased > 0) {
                        append(" unleased ${it.unleased}")
                    }
                    if (it.released) {
                        append(" released")
                    }
                }
            }.toMutableList()
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

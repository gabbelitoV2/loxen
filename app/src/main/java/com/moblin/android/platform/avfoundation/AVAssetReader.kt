package com.moblin.android.platform.avfoundation

import android.graphics.SurfaceTexture
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.os.SystemClock
import android.util.Log
import android.view.Surface
import com.moblin.android.media.MediaSample
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.core.PipelineStats
import com.moblin.android.platform.core.PipelineThread
import com.moblin.android.platform.video.CMVideoFormatDescriptionCreateForImageBuffer
import com.moblin.android.platform.video.CVPixelBuffer
import com.moblin.android.platform.video.CVPixelBufferPool
import com.moblin.android.platform.video.GlRenderer
import com.moblin.android.platform.video.PixelBufferLeases
import com.moblin.android.platform.video.PixelBufferReaper
import com.moblin.android.platform.video.kCVPixelBufferPixelFormatTypeKey
import com.moblin.android.platform.video.kCVPixelFormatType_32BGRA
import com.moblin.android.platform.video.releaseLease
import java.lang.ref.PhantomReference
import java.lang.ref.Reference
import java.lang.ref.ReferenceQueue
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private const val TAG = "MoblinReader"
private const val AUDIO_CHUNK_FRAMES = 1024
private const val CODEC_TIMEOUT_US = 10_000L
private const val STALL_TIMEOUT_MS = 5_000L

const val kAudioFormatLinearPCM = MediaFormat.MIMETYPE_AUDIO_RAW
const val AVLinearPCMBitDepthKey = "AVLinearPCMBitDepthKey"
const val AVLinearPCMIsFloatKey = "AVLinearPCMIsFloatKey"
const val AVLinearPCMIsBigEndianKey = "AVLinearPCMIsBigEndianKey"
const val AVLinearPCMIsNonInterleaved = "AVLinearPCMIsNonInterleaved"

internal object ReaderLog {
    private val loggedMessages = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    fun once(key: String, message: String) {
        if (loggedMessages.size > 1000) {
            loggedMessages.clear()
        }
        if (!loggedMessages.add(key)) {
            return
        }
        info(message)
    }

    fun info(message: String) {
        try {
            Log.i(TAG, message)
        } catch (_: Throwable) {
        }
    }

    fun notImplemented(member: String) {
        once("notImplemented:$member", "$member not implemented yet")
    }
}

private val readerThreadNumber = AtomicInteger(0)

private val readerExecutor: ExecutorService by lazy {
    Executors.newCachedThreadPool { runnable ->
        Thread(runnable, "MoblinReader-${readerThreadNumber.incrementAndGet()}").apply {
            isDaemon = true
        }
    }
}

private fun saturatingAdd(a: Long, b: Long): Long {
    val sum = a + b
    return if (((a xor sum) and (b xor sum)) < 0) {
        if (a < 0) Long.MIN_VALUE else Long.MAX_VALUE
    } else {
        sum
    }
}

class CMTimeRange(val start: Long, duration: Long? = null, end: Long? = null) {
    val duration: Long = duration ?: if (end != null) end - start else Long.MAX_VALUE

    val end: Long
        get() = saturatingAdd(start, duration)

    fun containsTime(time: Long): Boolean {
        return time >= start && time < end
    }

    override fun equals(other: Any?): Boolean {
        return other is CMTimeRange && other.start == start && other.duration == duration
    }

    override fun hashCode(): Int {
        return start.hashCode() * 31 + duration.hashCode()
    }

    override fun toString(): String {
        return "CMTimeRange(start=${start}us, duration=${duration}us)"
    }

    companion object {
        val zero = CMTimeRange(start = 0L, duration = 0L)
        val all = CMTimeRange(start = 0L)
    }
}

class AVAssetTrack internal constructor(
    val mediaType: AVMediaType,
    val naturalSize: CGSize,
    val nominalFrameRate: Float,
    val trackID: Int,
    internal val trackIndex: Int,
    internal val format: MediaFormat,
)

class AVAsset(url: String) {
    val path: String = url

    constructor(url: java.io.File) : this(url.path)

    constructor(url: java.net.URI) : this(if (url.scheme == null || url.scheme == "file") url.path else url.toString())

    @Volatile
    private var cachedDurationUs: Long? = null

    val durationUs: Long
        get() {
            cachedDurationUs?.let { return it }
            val value = readDurationUs()
            cachedDurationUs = value
            return value
        }

    fun duration(): Double {
        return durationUs / 1_000_000.0
    }

    fun loadTracks(withMediaType: AVMediaType, completionHandler: (List<AVAssetTrack>?, Throwable?) -> Unit) {
        readerExecutor.execute {
            val result = try {
                Pair(readTracks(withMediaType), null)
            } catch (error: Exception) {
                ReaderLog.once("loadTracks:${error.message}", "AVAsset.loadTracks failed for $path: $error")
                Pair(null, AVError(AVError.unknown, error.message ?: "Cannot read $path"))
            }
            completionHandler(result.first, result.second)
        }
    }

    private fun readTracks(mediaType: AVMediaType): List<AVAssetTrack> {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(path)
            val prefix = when (mediaType) {
                AVMediaType.video -> "video/"
                AVMediaType.audio -> "audio/"
            }
            val tracks = mutableListOf<AVAssetTrack>()
            for (index in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(index)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (!mime.startsWith(prefix)) {
                    continue
                }
                val width = readFormatInteger(format, MediaFormat.KEY_WIDTH) ?: 0
                val height = readFormatInteger(format, MediaFormat.KEY_HEIGHT) ?: 0
                val frameRate = readNumber(format, MediaFormat.KEY_FRAME_RATE) ?: 0f
                tracks.add(
                    AVAssetTrack(
                        mediaType = mediaType,
                        naturalSize = CGSize(width = width, height = height),
                        nominalFrameRate = frameRate,
                        trackID = index + 1,
                        trackIndex = index,
                        format = format,
                    ),
                )
            }
            return tracks
        } finally {
            runCatching { extractor.release() }
        }
    }

    private fun readDurationUs(): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            val milliseconds = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            (milliseconds ?: 0L) * 1000L
        } catch (error: Exception) {
            ReaderLog.once("duration:${error.message}", "AVAsset.duration failed for $path: $error")
            0L
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun readNumber(format: MediaFormat, key: String): Float? {
        if (!format.containsKey(key)) {
            return null
        }
        return runCatching { format.getInteger(key).toFloat() }.getOrNull()
            ?: runCatching { format.getFloat(key) }.getOrNull()
    }
}

abstract class AVAssetReaderOutput {
    var alwaysCopiesSampleData: Boolean = true
    internal var reader: AVAssetReader? = null

    abstract fun copyNextSampleBuffer(): MediaSample?

    internal abstract fun prepare(path: String, timeRange: CMTimeRange): Boolean

    internal abstract fun cancel()

    internal abstract val isFinished: Boolean
}

class AVAssetReaderTrackOutput(val track: AVAssetTrack, val outputSettings: Map<String, Any>?) : AVAssetReaderOutput() {
    var leasesSampleBuffers: Boolean = false

    private val holder = DecoderHolder()
    private val lock = ReentrantLock()

    @Volatile
    private var cancelled = false

    @Volatile
    private var finished = false

    val mediaType: AVMediaType
        get() = track.mediaType

    override val isFinished: Boolean
        get() = finished

    override fun prepare(path: String, timeRange: CMTimeRange): Boolean {
        lock.lock()
        try {
            if (cancelled) {
                return false
            }
            val settings = outputSettings
            val decoder = when {
                settings == null -> PassthroughTrackReader(path, track, timeRange)
                track.mediaType == AVMediaType.video -> VideoTrackDecoder(
                    path = path,
                    track = track,
                    timeRange = timeRange,
                    pixelFormatType = (settings[kCVPixelBufferPixelFormatTypeKey] as? Number)?.toInt()
                        ?: kCVPixelFormatType_32BGRA,
                    leased = leasesSampleBuffers,
                )
                else -> AudioTrackDecoder(path, track, timeRange, AudioOutputSettings.from(settings))
            }
            holder.decoder = decoder
            holder.reference = ReaderReaper.register(this, holder)
            if (!decoder.start()) {
                holder.release()
                finished = true
                return false
            }
            return true
        } finally {
            lock.unlock()
        }
    }

    override fun copyNextSampleBuffer(): MediaSample? {
        if (PipelineThread.isCurrent()) {
            ReaderLog.once(
                "copyNextSampleBufferOnPipeline",
                "AVAssetReaderTrackOutput.copyNextSampleBuffer must not run on the pipeline thread",
            )
            return null
        }
        val reader = reader ?: return null
        if (reader.status != AVAssetReader.Status.reading || finished) {
            return null
        }
        lock.lock()
        try {
            val decoder = holder.decoder ?: return null
            if (cancelled) {
                holder.release()
                return null
            }
            val sample = try {
                decoder.next()
            } catch (error: Throwable) {
                decoder.failure = error
                ReaderLog.once(
                    "next:${track.mediaType}:${error.javaClass.name}",
                    "AVAssetReader: ${track.mediaType} decoding failed: $error",
                )
                null
            }
            if (cancelled) {
                releaseLease(sample)
                holder.release()
                return null
            }
            if (sample == null) {
                finished = true
                val failure = decoder.failure
                holder.release()
                reader.outputFinished(failure)
            }
            return sample
        } finally {
            lock.unlock()
            if (cancelled) {
                releaseIfIdle()
            }
        }
    }

    override fun cancel() {
        cancelled = true
        holder.decoder?.cancelled = true
        releaseIfIdle()
    }

    private fun releaseIfIdle() {
        if (lock.tryLock()) {
            try {
                holder.release()
            } finally {
                lock.unlock()
            }
        }
    }
}

class AVAssetReader(val asset: AVAsset) {
    enum class Status {
        unknown,
        reading,
        completed,
        failed,
        cancelled,
    }

    private val lock = Any()
    private val outputList = mutableListOf<AVAssetReaderOutput>()
    private var starting = false

    var timeRange: CMTimeRange = CMTimeRange.all

    @Volatile
    var status: Status = Status.unknown
        private set

    @Volatile
    var error: Throwable? = null
        private set

    val outputs: List<AVAssetReaderOutput>
        get() = synchronized(lock) { outputList.toList() }

    fun canAdd(output: AVAssetReaderOutput): Boolean {
        return synchronized(lock) {
            status == Status.unknown && !starting && output.reader == null && !outputList.contains(output)
        }
    }

    fun add(output: AVAssetReaderOutput) {
        synchronized(lock) {
            if (status != Status.unknown || starting || output.reader != null || outputList.contains(output)) {
                ReaderLog.once("addRejected", "AVAssetReader.add: output rejected")
                return
            }
            output.reader = this
            outputList.add(output)
        }
    }

    fun startReading(): Boolean {
        val outputs = synchronized(lock) {
            if (status != Status.unknown || starting) {
                return status == Status.reading
            }
            starting = true
            outputList.toList()
        }
        val range = timeRange
        var failedOutput = false
        for (output in outputs) {
            if (!output.prepare(asset.path, range)) {
                failedOutput = true
                break
            }
        }
        synchronized(lock) {
            starting = false
            if (status == Status.cancelled) {
                for (output in outputs) {
                    output.cancel()
                }
                return false
            }
            if (failedOutput) {
                status = Status.failed
                error = AVError(AVError.unknown, "Cannot decode ${asset.path}")
                ReaderLog.info("AVAssetReader: cannot start reading ${asset.path}")
                for (output in outputs) {
                    output.cancel()
                }
                return false
            }
            status = Status.reading
            return true
        }
    }

    fun cancelReading() {
        val outputs = synchronized(lock) {
            if (status == Status.unknown || status == Status.reading) {
                status = Status.cancelled
            }
            if (starting) {
                return
            }
            outputList.toList()
        }
        for (output in outputs) {
            output.cancel()
        }
    }

    internal fun outputFinished(failure: Throwable?) {
        val outputs = synchronized(lock) {
            if (status != Status.reading) {
                return
            }
            if (failure == null) {
                if (outputList.all { it.isFinished }) {
                    status = Status.completed
                }
                return
            }
            status = Status.failed
            error = failure
            outputList.toList()
        }
        for (output in outputs) {
            output.cancel()
        }
    }
}

internal class DecoderHolder {
    @Volatile
    var decoder: TrackDecoder? = null

    @Volatile
    var reference: Reference<*>? = null

    fun release() {
        val current = synchronized(this) {
            val value = decoder
            decoder = null
            value
        } ?: return
        current.release()
        reference?.let { ReaderReaper.unregister(it) }
        reference = null
    }
}

private object ReaderReaper {
    private val queue = ReferenceQueue<Any>()
    private val references = Collections.newSetFromMap(ConcurrentHashMap<Reference<*>, Boolean>())

    private class OutputReference(output: Any, val holder: DecoderHolder) : PhantomReference<Any>(output, queue)

    init {
        Thread({ reap() }, "MoblinReaderReaper").apply {
            isDaemon = true
            start()
        }
    }

    fun register(output: Any, holder: DecoderHolder): Reference<*> {
        val reference = OutputReference(output, holder)
        references.add(reference)
        return reference
    }

    fun unregister(reference: Reference<*>) {
        references.remove(reference)
    }

    private fun reap() {
        while (true) {
            try {
                val reference = queue.remove() as? OutputReference ?: continue
                references.remove(reference)
                reference.holder.release()
                ReaderLog.once("reaped", "AVAssetReader: released the decoder of a collected output")
            } catch (_: InterruptedException) {
            } catch (error: Throwable) {
                ReaderLog.once("reap:${error.javaClass.name}", "AVAssetReader: reaping failed: $error")
            }
        }
    }
}

internal abstract class TrackDecoder {
    @Volatile
    var cancelled = false

    @Volatile
    var failure: Throwable? = null

    abstract fun start(): Boolean

    abstract fun next(): MediaSample?

    abstract fun release()
}

private fun openExtractor(path: String, track: AVAssetTrack, timeRange: CMTimeRange): MediaExtractor? {
    val extractor = MediaExtractor()
    return try {
        extractor.setDataSource(path)
        extractor.selectTrack(track.trackIndex)
        if (timeRange.start > 0) {
            extractor.seekTo(timeRange.start, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        }
        extractor
    } catch (error: Exception) {
        ReaderLog.info("AVAssetReader: cannot open $path: $error")
        runCatching { extractor.release() }
        null
    }
}

private fun createDecoder(format: MediaFormat): MediaCodec? {
    val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
    val name = runCatching {
        MediaCodecList(MediaCodecList.REGULAR_CODECS).findDecoderForFormat(format)
    }.getOrNull()
    return try {
        if (name != null) {
            MediaCodec.createByCodecName(name)
        } else {
            MediaCodec.createDecoderByType(mime)
        }
    } catch (error: Exception) {
        ReaderLog.info("AVAssetReader: no decoder for $mime: $error")
        null
    }
}

private fun feedInput(codec: MediaCodec, extractor: MediaExtractor, stopTimeUs: Long): Boolean {
    repeat(8) {
        val index = codec.dequeueInputBuffer(0)
        if (index < 0) {
            return false
        }
        val buffer = codec.getInputBuffer(index)
        val size = if (buffer != null) extractor.readSampleData(buffer, 0) else -1
        val sampleTime = extractor.sampleTime
        if (size < 0 || sampleTime >= stopTimeUs) {
            codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            return true
        }
        codec.queueInputBuffer(index, 0, size, sampleTime, 0)
        extractor.advance()
    }
    return false
}

private fun readFormatInteger(format: MediaFormat, key: String): Int? {
    return runCatching {
        if (format.containsKey(key)) format.getInteger(key) else null
    }.getOrNull()
}

private class VideoTrackDecoder(
    private val path: String,
    private val track: AVAssetTrack,
    private val timeRange: CMTimeRange,
    private val pixelFormatType: Int,
    private val leased: Boolean,
) : TrackDecoder() {
    private var extractor: MediaExtractor? = null
    private var codec: MediaCodec? = null
    private var texture = 0
    private var surfaceTexture: SurfaceTexture? = null
    private var surface: Surface? = null
    private val frameAvailable = Semaphore(0)
    private val transform = FloatArray(16)
    private var pool: CVPixelBufferPool? = null
    private var width = track.naturalSize.width.roundToInt()
    private var height = track.naturalSize.height.roundToInt()
    private var inputDone = false
    private var outputDone = false
    private val info = MediaCodec.BufferInfo()
    private var released = false

    override fun start(): Boolean {
        val extractor = openExtractor(path, track, timeRange) ?: return false
        this.extractor = extractor
        val format = extractor.getTrackFormat(track.trackIndex)
        if (format.containsKey(MediaFormat.KEY_ROTATION)) {
            format.setInteger(MediaFormat.KEY_ROTATION, 0)
        }
        readFormatInteger(format, MediaFormat.KEY_WIDTH)?.let { width = it }
        readFormatInteger(format, MediaFormat.KEY_HEIGHT)?.let { height = it }
        if (!createSurface()) {
            return false
        }
        val codec = createDecoder(format) ?: return false
        this.codec = codec
        return try {
            codec.configure(format, surface, null, 0)
            codec.start()
            true
        } catch (error: Exception) {
            ReaderLog.info("AVAssetReader: cannot start the video decoder for $path: $error")
            false
        }
    }

    override fun next(): MediaSample? {
        val codec = codec ?: return null
        val extractor = extractor ?: return null
        val stopTimeUs = saturatingAdd(timeRange.end, 1_000_000L)
        var lastProgressMs = SystemClock.uptimeMillis()
        while (!outputDone && !cancelled) {
            if (!inputDone) {
                inputDone = feedInput(codec, extractor, stopTimeUs)
            }
            val index = codec.dequeueOutputBuffer(info, CODEC_TIMEOUT_US)
            if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (SystemClock.uptimeMillis() - lastProgressMs > STALL_TIMEOUT_MS) {
                    ReaderLog.info("AVAssetReader: the video decoder stalled for $path")
                    outputDone = true
                }
                continue
            }
            lastProgressMs = SystemClock.uptimeMillis()
            if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                updateSize(codec.outputFormat)
                continue
            }
            if (index < 0) {
                continue
            }
            if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                outputDone = true
            }
            val presentationTimeUs = info.presentationTimeUs
            if (info.size == 0 || presentationTimeUs < timeRange.start) {
                codec.releaseOutputBuffer(index, false)
                continue
            }
            if (presentationTimeUs >= timeRange.end) {
                codec.releaseOutputBuffer(index, false)
                outputDone = true
                continue
            }
            frameAvailable.drainPermits()
            codec.releaseOutputBuffer(index, true)
            val sample = copyFrame(presentationTimeUs)
            if (sample == null) {
                outputDone = true
            }
            return sample
        }
        return null
    }

    private fun updateSize(format: MediaFormat) {
        val formatWidth = readFormatInteger(format, MediaFormat.KEY_WIDTH) ?: width
        val formatHeight = readFormatInteger(format, MediaFormat.KEY_HEIGHT) ?: height
        val cropLeft = readFormatInteger(format, "crop-left")
        val cropRight = readFormatInteger(format, "crop-right")
        val cropTop = readFormatInteger(format, "crop-top")
        val cropBottom = readFormatInteger(format, "crop-bottom")
        width = if (cropLeft != null && cropRight != null) cropRight - cropLeft + 1 else formatWidth
        height = if (cropTop != null && cropBottom != null) cropBottom - cropTop + 1 else formatHeight
    }

    private fun createSurface(): Boolean {
        val available = frameAvailable
        return try {
            PipelineThread.runSync(timeoutMs = 3000) {
                val name = GlRenderer.createOesTexture()
                if (name == 0) {
                    false
                } else {
                    val newSurfaceTexture = SurfaceTexture(name)
                    newSurfaceTexture.setOnFrameAvailableListener({ available.release() }, PipelineThread.handler)
                    texture = name
                    surfaceTexture = newSurfaceTexture
                    surface = Surface(newSurfaceTexture)
                    true
                }
            }
        } catch (error: Throwable) {
            ReaderLog.info("AVAssetReader: cannot create the decoder surface: $error")
            false
        }
    }

    private fun copyFrame(presentationTimeUs: Long): MediaSample? {
        if (!frameAvailable.tryAcquire(2_500, TimeUnit.MILLISECONDS)) {
            ReaderLog.info("AVAssetReader: a decoded frame never reached the surface for $path")
            return null
        }
        if (width <= 0 || height <= 0) {
            return null
        }
        val pool = pool(width, height)
        val deadlineMs = SystemClock.uptimeMillis() + 2_000
        var waited = false
        var lastAttemptMs = 0L
        while (!cancelled) {
            val nowMs = SystemClock.uptimeMillis()
            val buffer = if (hasFreeBuffer(pool) || (!leased && nowMs - lastAttemptMs >= 250)) {
                lastAttemptMs = nowMs
                try {
                    PipelineThread.runSync(timeoutMs = 3000) { drawFrame(pool) }
                } catch (error: Throwable) {
                    ReaderLog.once("draw:${error.javaClass.name}", "AVAssetReader: frame copy failed: $error")
                    null
                }
            } else {
                null
            }
            if (buffer != null) {
                PipelineStats.increment("readerFrames")
                if (waited) {
                    PipelineStats.increment("readerPoolWaits")
                }
                return MediaSample(
                    data = emptyData,
                    presentationTimeUs = presentationTimeUs,
                    isKeyFrame = true,
                    format = CMVideoFormatDescriptionCreateForImageBuffer(buffer),
                    imageBuffer = buffer,
                )
            }
            if (SystemClock.uptimeMillis() >= deadlineMs) {
                ReaderLog.info("AVAssetReader: no free ${width}x$height buffer within 2 s for $path, ending the track")
                return null
            }
            waited = true
            try {
                Thread.sleep(4)
            } catch (_: InterruptedException) {
                return null
            }
        }
        return null
    }

    private fun hasFreeBuffer(pool: CVPixelBufferPool): Boolean {
        val state = pool.state
        return synchronized(PixelBufferReaper) {
            state.free.isNotEmpty() || state.allocated < state.maximumBufferCount
        }
    }

    private fun drawFrame(pool: CVPixelBufferPool): CVPixelBuffer? {
        val surfaceTexture = surfaceTexture ?: return null
        val buffer = PixelBufferReaper.obtain(pool.state, pool.pixelFormatType, leased) ?: return null
        if (leased) {
            PixelBufferLeases.retain(buffer, "AVAssetReader")
        }
        try {
            surfaceTexture.updateTexImage()
            surfaceTexture.getTransformMatrix(transform)
            GlRenderer.drawOes(texture, transform, buffer, 0, false)
        } catch (error: Throwable) {
            if (leased) {
                PixelBufferLeases.release(buffer, "AVAssetReader")
            }
            throw error
        }
        return buffer
    }

    private fun pool(width: Int, height: Int): CVPixelBufferPool {
        val existing = pool
        if (existing != null && existing.width == width && existing.height == height) {
            return existing
        }
        existing?.invalidate()
        val newPool = CVPixelBufferPool(width, height, pixelFormatType, 32)
        newPool.name = "reader"
        pool = newPool
        return newPool
    }

    override fun release() {
        synchronized(this) {
            if (released) {
                return
            }
            released = true
        }
        codec?.let { codec ->
            runCatching { codec.stop() }
            runCatching { codec.release() }
        }
        codec = null
        extractor?.let { runCatching { it.release() } }
        extractor = null
        pool?.invalidate()
        val oldSurfaceTexture = surfaceTexture
        val oldSurface = surface
        val oldTexture = texture
        surfaceTexture = null
        surface = null
        texture = 0
        if (oldSurfaceTexture == null && oldSurface == null && oldTexture == 0) {
            return
        }
        PipelineThread.post {
            runCatching { oldSurfaceTexture?.setOnFrameAvailableListener(null) }
            runCatching { oldSurface?.release() }
            runCatching { oldSurfaceTexture?.release() }
            GlRenderer.deleteTexture(oldTexture)
        }
    }

    companion object {
        private val emptyData = ByteArray(0)
    }
}

internal class AudioOutputSettings(
    val sampleRate: Int?,
    val channels: Int?,
    val isFloat: Boolean,
    val isBigEndian: Boolean,
) {
    companion object {
        fun from(settings: Map<String, Any>): AudioOutputSettings {
            val bitDepth = (settings[AVLinearPCMBitDepthKey] as? Number)?.toInt()
            val isFloat = settings[AVLinearPCMIsFloatKey] == true
            val expectedBitDepth = if (isFloat) 32 else 16
            if (bitDepth != null && bitDepth != expectedBitDepth) {
                ReaderLog.once(
                    "pcmFormat:$bitDepth:$isFloat",
                    "AVAssetReader: $bitDepth-bit PCM is not supported, using $expectedBitDepth-bit",
                )
            }
            if (settings[AVLinearPCMIsNonInterleaved] == true) {
                ReaderLog.once("pcmNonInterleaved", "AVAssetReader: non-interleaved PCM is not supported")
            }
            return AudioOutputSettings(
                sampleRate = (settings[AVSampleRateKey] as? Number)?.toDouble()?.roundToInt()?.takeIf { it > 0 },
                channels = (settings[AVNumberOfChannelsKey] as? Number)?.toInt()?.takeIf { it > 0 },
                isFloat = isFloat,
                isBigEndian = settings[AVLinearPCMIsBigEndianKey] == true,
            )
        }
    }
}

private class AudioTrackDecoder(
    private val path: String,
    private val track: AVAssetTrack,
    private val timeRange: CMTimeRange,
    private val settings: AudioOutputSettings,
) : TrackDecoder() {
    private var extractor: MediaExtractor? = null
    private var codec: MediaCodec? = null
    private var converter: PcmConverter? = null
    private val info = MediaCodec.BufferInfo()
    private var inputDone = false
    private var outputDone = false
    private var flushed = false
    private var basePresentationTimeUs: Long? = null
    private var emittedFrames = 0L
    private var outputFormat: MediaFormat? = null
    private var decodedRate = 0
    private var decodedChannels = 0
    private var decodedEncoding = AudioFormat.ENCODING_PCM_16BIT
    private var lastProgressMs = 0L
    private var released = false

    override fun start(): Boolean {
        val extractor = openExtractor(path, track, timeRange) ?: return false
        this.extractor = extractor
        val format = extractor.getTrackFormat(track.trackIndex)
        readFormatInteger(format, MediaFormat.KEY_SAMPLE_RATE)?.let { decodedRate = it }
        readFormatInteger(format, MediaFormat.KEY_CHANNEL_COUNT)?.let { decodedChannels = it }
        val codec = createDecoder(format) ?: return false
        this.codec = codec
        return try {
            codec.configure(format, null, null, 0)
            codec.start()
            lastProgressMs = SystemClock.uptimeMillis()
            true
        } catch (error: Exception) {
            ReaderLog.info("AVAssetReader: cannot start the audio decoder for $path: $error")
            false
        }
    }

    override fun next(): MediaSample? {
        while (true) {
            converter?.takeChunk(AUDIO_CHUNK_FRAMES)?.let {
                return makeSample(it)
            }
            if (outputDone) {
                if (!flushed) {
                    flushed = true
                    converter?.flush()
                    continue
                }
                return converter?.takeRemaining()?.let { makeSample(it) }
            }
            if (cancelled) {
                return null
            }
            decodeStep()
        }
    }

    private fun decodeStep() {
        val codec = codec
        val extractor = extractor
        if (codec == null || extractor == null) {
            outputDone = true
            return
        }
        if (!inputDone) {
            inputDone = feedInput(codec, extractor, saturatingAdd(timeRange.end, 100_000L))
        }
        val index = codec.dequeueOutputBuffer(info, CODEC_TIMEOUT_US)
        if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
            if (SystemClock.uptimeMillis() - lastProgressMs > STALL_TIMEOUT_MS) {
                ReaderLog.info("AVAssetReader: the audio decoder stalled for $path")
                outputDone = true
            }
            return
        }
        lastProgressMs = SystemClock.uptimeMillis()
        if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            updateDecodedFormat(codec.outputFormat)
            return
        }
        if (index < 0) {
            return
        }
        try {
            if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                outputDone = true
            }
            if (info.size > 0 && (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                val buffer = codec.getOutputBuffer(index)
                if (buffer != null) {
                    appendPcm(buffer, info.offset, info.size, info.presentationTimeUs)
                }
            }
        } finally {
            codec.releaseOutputBuffer(index, false)
        }
    }

    private fun updateDecodedFormat(format: MediaFormat) {
        readFormatInteger(format, MediaFormat.KEY_SAMPLE_RATE)?.let { decodedRate = it }
        readFormatInteger(format, MediaFormat.KEY_CHANNEL_COUNT)?.let { decodedChannels = it }
        decodedEncoding = readFormatInteger(format, MediaFormat.KEY_PCM_ENCODING) ?: AudioFormat.ENCODING_PCM_16BIT
    }

    private fun appendPcm(buffer: ByteBuffer, offset: Int, size: Int, presentationTimeUs: Long) {
        if (decodedRate <= 0 || decodedChannels <= 0) {
            return
        }
        val data = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        data.limit(offset + size)
        data.position(offset)
        val samples = decodePcm(data, decodedEncoding)
        val frames = samples.size / decodedChannels
        if (frames <= 0) {
            return
        }
        var firstFrame = 0
        var lastFrame = frames
        if (presentationTimeUs < timeRange.start) {
            val skip = ((timeRange.start - presentationTimeUs) * decodedRate + 999_999) / 1_000_000
            firstFrame = min(frames.toLong(), skip).toInt()
        }
        val endUs = timeRange.end
        if (endUs != Long.MAX_VALUE) {
            val keep = (endUs - presentationTimeUs) * decodedRate / 1_000_000
            if (keep < frames) {
                lastFrame = max(0L, keep).toInt()
                outputDone = true
            }
        }
        if (lastFrame <= firstFrame) {
            return
        }
        if (basePresentationTimeUs == null) {
            basePresentationTimeUs = presentationTimeUs + firstFrame * 1_000_000L / decodedRate
        }
        converterFor(decodedRate, decodedChannels).append(samples, firstFrame * decodedChannels, lastFrame - firstFrame)
    }

    private fun converterFor(rate: Int, channels: Int): PcmConverter {
        val existing = converter
        if (existing != null) {
            existing.setInputFormat(rate, channels)
            return existing
        }
        val outputRate = settings.sampleRate ?: rate
        val outputChannels = settings.channels ?: channels
        outputFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_RAW, outputRate, outputChannels).apply {
            setInteger(
                MediaFormat.KEY_PCM_ENCODING,
                if (settings.isFloat) AudioFormat.ENCODING_PCM_FLOAT else AudioFormat.ENCODING_PCM_16BIT,
            )
        }
        return PcmConverter(rate, channels, outputRate, outputChannels).also { converter = it }
    }

    private fun makeSample(samples: FloatArray): MediaSample? {
        val converter = converter ?: return null
        val frames = samples.size / converter.outputChannels
        if (frames <= 0) {
            return null
        }
        val presentationTimeUs = (basePresentationTimeUs ?: 0L) + emittedFrames * 1_000_000L / converter.outputRate
        emittedFrames += frames
        val data = if (settings.isFloat) {
            encodePcmFloat(samples, settings.isBigEndian)
        } else {
            encodePcm16(samples, settings.isBigEndian)
        }
        return MediaSample(
            data = data,
            presentationTimeUs = presentationTimeUs,
            isKeyFrame = true,
            format = outputFormat,
            durationUs = frames * 1_000_000L / converter.outputRate,
        )
    }

    override fun release() {
        synchronized(this) {
            if (released) {
                return
            }
            released = true
        }
        codec?.let { codec ->
            runCatching { codec.stop() }
            runCatching { codec.release() }
        }
        codec = null
        extractor?.let { runCatching { it.release() } }
        extractor = null
    }
}

private class PassthroughTrackReader(
    private val path: String,
    private val track: AVAssetTrack,
    private val timeRange: CMTimeRange,
) : TrackDecoder() {
    private var extractor: MediaExtractor? = null
    private var buffer: ByteBuffer = ByteBuffer.allocate(0)
    private var released = false

    override fun start(): Boolean {
        val extractor = openExtractor(path, track, timeRange) ?: return false
        this.extractor = extractor
        val maximumSize = readFormatInteger(track.format, MediaFormat.KEY_MAX_INPUT_SIZE) ?: (1 shl 20)
        buffer = ByteBuffer.allocate(max(maximumSize, 4096))
        return true
    }

    override fun next(): MediaSample? {
        val extractor = extractor ?: return null
        if (cancelled) {
            return null
        }
        buffer.clear()
        val size = extractor.readSampleData(buffer, 0)
        val sampleTime = extractor.sampleTime
        if (size < 0 || sampleTime >= timeRange.end) {
            return null
        }
        val flags = extractor.sampleFlags
        extractor.advance()
        val data = ByteArray(size)
        buffer.position(0)
        buffer.get(data, 0, size)
        return MediaSample(
            data = data,
            presentationTimeUs = sampleTime,
            isKeyFrame = (flags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0,
            format = track.format,
        )
    }

    override fun release() {
        synchronized(this) {
            if (released) {
                return
            }
            released = true
        }
        extractor?.let { runCatching { it.release() } }
        extractor = null
    }
}

internal fun decodePcm(data: ByteBuffer, encoding: Int): FloatArray {
    return when (encoding) {
        AudioFormat.ENCODING_PCM_FLOAT -> {
            val floats = data.asFloatBuffer()
            FloatArray(floats.remaining()).also { floats.get(it) }
        }
        AudioFormat.ENCODING_PCM_8BIT -> {
            FloatArray(data.remaining()) { ((data.get().toInt() and 0xFF) - 128) / 128f }
        }
        else -> {
            val shorts = data.asShortBuffer()
            FloatArray(shorts.remaining()) { shorts.get() / 32768f }
        }
    }
}

internal fun encodePcm16(samples: FloatArray, bigEndian: Boolean): ByteArray {
    val data = ByteArray(samples.size * 2)
    for (index in samples.indices) {
        val value = (samples[index] * 32768f).roundToInt().coerceIn(-32768, 32767)
        val low = (value and 0xFF).toByte()
        val high = ((value shr 8) and 0xFF).toByte()
        if (bigEndian) {
            data[2 * index] = high
            data[2 * index + 1] = low
        } else {
            data[2 * index] = low
            data[2 * index + 1] = high
        }
    }
    return data
}

internal fun encodePcmFloat(samples: FloatArray, bigEndian: Boolean): ByteArray {
    val buffer = ByteBuffer.allocate(samples.size * 4)
    buffer.order(if (bigEndian) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN)
    buffer.asFloatBuffer().put(samples)
    return buffer.array()
}

internal fun mixChannels(input: FloatArray, offset: Int, frames: Int, inputChannels: Int, outputChannels: Int): FloatArray {
    val output = FloatArray(frames * outputChannels)
    if (inputChannels == outputChannels) {
        System.arraycopy(input, offset, output, 0, frames * outputChannels)
        return output
    }
    val leftSurround = if (inputChannels >= 6) 4 else 3
    val rightSurround = if (inputChannels >= 6) 5 else 4
    for (frame in 0 until frames) {
        val source = offset + frame * inputChannels
        val target = frame * outputChannels
        when {
            outputChannels == 1 -> {
                var sum = 0f
                for (channel in 0 until inputChannels) {
                    sum += input[source + channel]
                }
                output[target] = sum / inputChannels
            }
            inputChannels == 1 -> {
                val value = input[source]
                for (channel in 0 until outputChannels) {
                    output[target + channel] = value
                }
            }
            outputChannels == 2 && inputChannels >= 5 -> {
                val center = input[source + 2] * 0.7071f
                val left = input[source] + center + input[source + leftSurround] * 0.7071f
                val right = input[source + 1] + center + input[source + rightSurround] * 0.7071f
                output[target] = left / 2.4142f
                output[target + 1] = right / 2.4142f
            }
            else -> {
                for (channel in 0 until outputChannels) {
                    output[target + channel] = if (channel < inputChannels) input[source + channel] else 0f
                }
            }
        }
    }
    return output
}

internal class PcmResampler(
    private val inputRate: Int,
    private val outputRate: Int,
    private val channels: Int,
) {
    private val cutoff = min(1.0, outputRate.toDouble() / inputRate) * 0.95
    private val halfTaps = kotlin.math.ceil(ZERO_CROSSINGS / min(1.0, outputRate.toDouble() / inputRate)).toInt()
    private val taps = 2 * halfTaps
    private val table = buildTable()
    private var buffer = FloatArray(4096 * channels)
    private var bufferFrames = 0
    private var bufferStartFrame = -halfTaps.toLong()
    private var inputFrames = 0L
    private var outputIndex = 0L
    private var output = FloatArray(4096 * channels)
    private var outputFrames = 0

    init {
        appendZeros(halfTaps)
    }

    fun process(input: FloatArray, offset: Int, frames: Int): FloatArray {
        ensureBufferCapacity(bufferFrames + frames)
        System.arraycopy(input, offset, buffer, bufferFrames * channels, frames * channels)
        bufferFrames += frames
        inputFrames += frames
        return produce(flushing = false)
    }

    fun flush(): FloatArray {
        appendZeros(halfTaps)
        return produce(flushing = true)
    }

    private fun appendZeros(frames: Int) {
        ensureBufferCapacity(bufferFrames + frames)
        java.util.Arrays.fill(buffer, bufferFrames * channels, (bufferFrames + frames) * channels, 0f)
        bufferFrames += frames
    }

    private fun ensureBufferCapacity(frames: Int) {
        if (frames * channels > buffer.size) {
            buffer = buffer.copyOf(max(frames * channels, buffer.size * 2))
        }
    }

    private fun produce(flushing: Boolean): FloatArray {
        outputFrames = 0
        val availableEnd = bufferStartFrame + bufferFrames
        while (true) {
            val position = outputIndex * inputRate
            val center = Math.floorDiv(position, outputRate.toLong())
            if (flushing && position >= inputFrames * outputRate) {
                break
            }
            if (center + halfTaps >= availableEnd) {
                break
            }
            val phasePosition = (position - center * outputRate).toDouble() / outputRate * PHASES
            val phase = min(phasePosition.toInt(), PHASES - 1)
            val weight = (phasePosition - phase).toFloat()
            val first = (center - halfTaps + 1 - bufferStartFrame).toInt()
            ensureOutputCapacity(outputFrames + 1)
            val rowA = phase * taps
            val rowB = rowA + taps
            for (channel in 0 until channels) {
                var sum = 0f
                var index = first * channels + channel
                for (tap in 0 until taps) {
                    val coefficient = table[rowA + tap] + (table[rowB + tap] - table[rowA + tap]) * weight
                    sum += coefficient * buffer[index]
                    index += channels
                }
                output[outputFrames * channels + channel] = sum
            }
            outputFrames += 1
            outputIndex += 1
        }
        compact()
        return output.copyOf(outputFrames * channels)
    }

    private fun ensureOutputCapacity(frames: Int) {
        if (frames * channels > output.size) {
            output = output.copyOf(max(frames * channels, output.size * 2))
        }
    }

    private fun compact() {
        val nextCenter = Math.floorDiv(outputIndex * inputRate, outputRate.toLong())
        val drop = min((nextCenter - halfTaps + 1 - bufferStartFrame).toInt(), bufferFrames)
        if (drop <= 0) {
            return
        }
        System.arraycopy(buffer, drop * channels, buffer, 0, (bufferFrames - drop) * channels)
        bufferFrames -= drop
        bufferStartFrame += drop
    }

    private fun buildTable(): FloatArray {
        val table = FloatArray((PHASES + 1) * taps)
        val row = DoubleArray(taps)
        for (phase in 0..PHASES) {
            val fraction = phase.toDouble() / PHASES
            var sum = 0.0
            for (tap in 0 until taps) {
                val distance = (tap - halfTaps + 1) - fraction
                val x = distance / halfTaps
                val window = if (x <= -1.0 || x >= 1.0) {
                    0.0
                } else {
                    0.42 + 0.5 * cos(PI * x) + 0.08 * cos(2 * PI * x)
                }
                val argument = cutoff * distance
                val sinc = if (argument == 0.0) 1.0 else sin(PI * argument) / (PI * argument)
                row[tap] = cutoff * sinc * window
                sum += row[tap]
            }
            for (tap in 0 until taps) {
                table[phase * taps + tap] = (row[tap] / sum).toFloat()
            }
        }
        return table
    }

    companion object {
        private const val ZERO_CROSSINGS = 16.0
        private const val PHASES = 256
    }
}

internal class PcmConverter(
    inputRate: Int,
    inputChannels: Int,
    val outputRate: Int,
    val outputChannels: Int,
) {
    private var inputRate = inputRate
    private var inputChannels = inputChannels
    private var resampler = makeResampler()
    private var fifo = FloatArray(8192 * outputChannels)
    private var fifoFrames = 0

    val bufferedFrames: Int
        get() = fifoFrames

    fun setInputFormat(rate: Int, channels: Int) {
        if (rate == inputRate && channels == inputChannels) {
            return
        }
        resampler?.let { push(it.flush()) }
        inputRate = rate
        inputChannels = channels
        resampler = makeResampler()
    }

    fun append(samples: FloatArray, offset: Int, frames: Int) {
        if (frames <= 0) {
            return
        }
        val mixed = mixChannels(samples, offset, frames, inputChannels, outputChannels)
        val resampler = resampler
        if (resampler == null) {
            push(mixed)
        } else {
            push(resampler.process(mixed, 0, frames))
        }
    }

    fun flush() {
        resampler?.let { push(it.flush()) }
        resampler = makeResampler()
    }

    fun takeChunk(frames: Int): FloatArray? {
        if (fifoFrames < frames) {
            return null
        }
        return take(frames)
    }

    fun takeRemaining(): FloatArray? {
        if (fifoFrames == 0) {
            return null
        }
        return take(fifoFrames)
    }

    private fun take(frames: Int): FloatArray {
        val count = frames * outputChannels
        val chunk = fifo.copyOf(count)
        System.arraycopy(fifo, count, fifo, 0, (fifoFrames - frames) * outputChannels)
        fifoFrames -= frames
        return chunk
    }

    private fun push(samples: FloatArray) {
        val frames = samples.size / outputChannels
        if (frames == 0) {
            return
        }
        val needed = (fifoFrames + frames) * outputChannels
        if (needed > fifo.size) {
            fifo = fifo.copyOf(max(needed, fifo.size * 2))
        }
        System.arraycopy(samples, 0, fifo, fifoFrames * outputChannels, frames * outputChannels)
        fifoFrames += frames
    }

    private fun makeResampler(): PcmResampler? {
        if (inputRate == outputRate) {
            return null
        }
        return PcmResampler(inputRate, outputRate, outputChannels)
    }
}

package com.moblin.android.platform.avfoundation

import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.util.Log
import com.moblin.android.media.MediaSample
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.core.PipelineThread
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

private const val TAG = "MoblinReader"

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
                val width = readInteger(format, MediaFormat.KEY_WIDTH) ?: 0
                val height = readInteger(format, MediaFormat.KEY_HEIGHT) ?: 0
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

    private fun readInteger(format: MediaFormat, key: String): Int? {
        if (!format.containsKey(key)) {
            return null
        }
        return runCatching { format.getInteger(key) }.getOrNull()
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
}

class AVAssetReaderTrackOutput(val track: AVAssetTrack, val outputSettings: Map<String, Any>?) : AVAssetReaderOutput() {
    val mediaType: AVMediaType
        get() = track.mediaType

    override fun copyNextSampleBuffer(): MediaSample? {
        if (PipelineThread.isCurrent()) {
            ReaderLog.once(
                "copyNextSampleBufferOnPipeline",
                "AVAssetReaderTrackOutput.copyNextSampleBuffer must not run on the pipeline thread",
            )
            return null
        }
        val reader = reader ?: return null
        if (reader.status != AVAssetReader.Status.reading) {
            return null
        }
        ReaderLog.notImplemented("AVAssetReaderTrackOutput.copyNextSampleBuffer")
        reader.markCompleted()
        return null
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
            status == Status.unknown && output.reader == null && !outputList.contains(output)
        }
    }

    fun add(output: AVAssetReaderOutput) {
        synchronized(lock) {
            if (status != Status.unknown || output.reader != null || outputList.contains(output)) {
                ReaderLog.once("addRejected", "AVAssetReader.add: output rejected")
                return
            }
            output.reader = this
            outputList.add(output)
        }
    }

    fun startReading(): Boolean {
        synchronized(lock) {
            if (status != Status.unknown) {
                return status == Status.reading
            }
            ReaderLog.notImplemented("AVAssetReader.startReading")
            status = Status.failed
            error = AVError(AVError.unknown, "AVAssetReader.startReading not implemented yet")
            return false
        }
    }

    fun cancelReading() {
        synchronized(lock) {
            if (status == Status.unknown || status == Status.reading) {
                status = Status.cancelled
            }
        }
    }

    internal fun markCompleted() {
        synchronized(lock) {
            if (status == Status.reading) {
                status = Status.completed
            }
        }
    }
}

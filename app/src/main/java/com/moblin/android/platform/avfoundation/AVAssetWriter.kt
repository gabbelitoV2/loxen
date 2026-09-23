package com.moblin.android.platform.avfoundation

import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import com.moblin.android.media.MediaSample
import com.moblin.android.platform.audio.audioChannelCount
import com.moblin.android.platform.audio.audioSampleRate
import com.moblin.android.platform.audio.makePcmFormat
import com.moblin.android.platform.capture.CaptureFrameRate
import com.moblin.android.platform.core.PipelineStats
import com.moblin.android.platform.core.PipelineThread
import com.moblin.android.platform.mp4.FragmentedMp4Muxer
import com.moblin.android.platform.mp4.FragmentedMp4MuxerListener
import com.moblin.android.platform.mp4.Mp4AudioTrackConfig
import com.moblin.android.platform.mp4.Mp4TrackReport
import com.moblin.android.platform.mp4.Mp4VideoTrackConfig
import com.moblin.android.platform.mp4.makeAacAudioSpecificConfig
import com.moblin.android.platform.video.CVPixelBuffer
import com.moblin.android.platform.video.PixelBufferLeases
import com.moblin.android.platform.video.releaseLease
import com.moblin.android.platform.videotoolbox.CMFormatDescriptionGetExtension
import com.moblin.android.platform.videotoolbox.VTCompressionSession
import com.moblin.android.platform.videotoolbox.VTCompressionSessionCreate
import com.moblin.android.platform.videotoolbox.VTCompressionSessionEncodeFrame
import com.moblin.android.platform.videotoolbox.VTCompressionSessionInvalidate
import com.moblin.android.platform.videotoolbox.VTCompressionSessionPrepareToEncodeFrames
import com.moblin.android.platform.videotoolbox.VTSessionSetProperties
import com.moblin.android.platform.videotoolbox.kCMFormatDescriptionExtension_SampleDescriptionExtensionAtoms
import com.moblin.android.platform.videotoolbox.kVTEncodeInfo_FrameDropped
import com.moblin.android.platform.videotoolbox.kVTInvalidSessionErr
import com.moblin.android.platform.videotoolbox.kVTProfileLevel_H264_High_AutoLevel
import com.moblin.android.platform.videotoolbox.kVTProfileLevel_HEVC_Main_AutoLevel
import com.moblin.android.platform.videotoolbox.noErr
import java.util.Collections
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private const val TAG = "MoblinRecorder"
private const val aacFramesPerPacket = 1024
private const val finishEncoderDrainMs = 300L

private val loggedMessages: MutableSet<String> = Collections.synchronizedSet(HashSet())

private val encoderSetupExecutor = Executors.newCachedThreadPool { runnable ->
    Thread(runnable, "MoblinRecorderEncoderSetup").apply { isDaemon = true }
}

private fun logOnce(message: String) {
    if (loggedMessages.add(message)) {
        Log.i(TAG, message)
    }
}

object UTType {
    const val mpeg4Movie = "public.mpeg-4"
}

object AVFileTypeProfile {
    const val mpeg4AppleHLS = "MPEG4AppleHLS"
}

object AVVideoCodecType {
    const val h264 = MediaFormat.MIMETYPE_VIDEO_AVC
    const val hevc = MediaFormat.MIMETYPE_VIDEO_HEVC
}

const val AVVideoCodecKey = "mime"
const val AVVideoWidthKey = "width"
const val AVVideoHeightKey = "height"
const val AVVideoCompressionPropertiesKey = "compressionProperties"
const val AVVideoAverageBitRateKey = "bitrate"
const val AVVideoMaxKeyFrameIntervalDurationKey = "i-frame-interval"
const val AVFormatIDKey = "mime"
const val AVSampleRateKey = "sample-rate"
const val AVNumberOfChannelsKey = "channel-count"
const val AVEncoderBitRateKey = "bitrate"

enum class AVAssetSegmentType {
    initialization,
    separable,
}

class AVAssetSegmentTrackReport(
    val trackID: Int,
    val mediaType: AVMediaType,
    val earliestPresentationTimeStamp: Long,
    val duration: Long,
)

class AVAssetSegmentReport(val trackReports: List<AVAssetSegmentTrackReport>)

interface AVAssetWriterDelegate {
    fun assetWriter(
        writer: AVAssetWriter,
        didOutputSegmentData: ByteArray,
        segmentType: AVAssetSegmentType,
        segmentReport: AVAssetSegmentReport?,
    )
}

class AVAssetWriter(val contentType: String) {
    enum class Status {
        unknown,
        writing,
        completed,
        failed,
        cancelled,
    }

    var shouldOptimizeForNetworkUse = false
    var outputFileTypeProfile: String? = null
    var preferredOutputSegmentInterval: Long = 0
    var initialSegmentStartTime: Long = 0

    @Volatile
    var delegate: AVAssetWriterDelegate? = null

    private val lock = Any()
    private val inputList = mutableListOf<AVAssetWriterInput>()

    @Volatile
    private var statusValue = Status.unknown

    @Volatile
    private var errorValue: Throwable? = null

    @Volatile
    private var executor: ScheduledExecutorService? = null
    private var muxer: FragmentedMp4Muxer? = null
    private var finishing = false
    private var latestVideoFormat: MediaFormat? = null
    private var sessionStartTime = 0L

    val inputs: List<AVAssetWriterInput>
        get() = synchronized(lock) { inputList.toList() }

    val status: Status
        get() = statusValue

    val error: Throwable?
        get() = errorValue

    private val muxerListener = object : FragmentedMp4MuxerListener {
        override fun fragmentedMp4MuxerInitializationSegment(data: ByteArray) {
            outputSegment(data, AVAssetSegmentType.initialization, null)
        }

        override fun fragmentedMp4MuxerMediaSegment(data: ByteArray, reports: List<Mp4TrackReport>) {
            PipelineStats.increment("recSeg")
            val segmentReport = AVAssetSegmentReport(
                reports.map {
                    AVAssetSegmentTrackReport(
                        trackID = it.trackId,
                        mediaType = if (it.isVideo) AVMediaType.video else AVMediaType.audio,
                        earliestPresentationTimeStamp = it.earliestPresentationTimeUs,
                        duration = it.durationUs,
                    )
                },
            )
            outputSegment(data, AVAssetSegmentType.separable, segmentReport)
        }
    }

    fun canAdd(input: AVAssetWriterInput): Boolean {
        synchronized(lock) {
            return canAddLocked(input)
        }
    }

    fun add(input: AVAssetWriterInput) {
        synchronized(lock) {
            if (!canAddLocked(input)) {
                Log.i(TAG, "Cannot add a ${input.mediaType} input to a writer with status $statusValue")
                return
            }
            input.attach(this, if (input.mediaType == AVMediaType.video) 1 else 2)
            inputList.add(input)
        }
    }

    fun startWriting(): Boolean {
        synchronized(lock) {
            if (statusValue != Status.unknown) {
                Log.i(TAG, "Cannot start writing with status $statusValue")
                return false
            }
            if (inputList.isEmpty()) {
                statusValue = Status.failed
                errorValue = AVError(AVError.unknown, "No inputs")
                return false
            }
            val interval = if (preferredOutputSegmentInterval > 0) preferredOutputSegmentInterval else 2_000_000L
            val executor = Executors.newSingleThreadScheduledExecutor { runnable ->
                Thread(runnable, "MoblinRecorderMuxer").apply { isDaemon = true }
            }
            val muxer = FragmentedMp4Muxer(interval, muxerListener)
            this.executor = executor
            this.muxer = muxer
            val audioTrackConfig = inputList.firstOrNull { it.mediaType == AVMediaType.audio }?.makeAudioTrackConfig()
            statusValue = Status.writing
            Log.i(
                TAG,
                "Start writing $contentType ($outputFileTypeProfile) with ${inputList.size} inputs and " +
                    "${interval / 1000} ms segments",
            )
            if (audioTrackConfig != null) {
                execute(executor) {
                    muxer.setAudioTrack(audioTrackConfig)
                }
            }
            return true
        }
    }

    fun startSession(atSourceTime: Long) {
        sessionStartTime = atSourceTime
        Log.i(TAG, "Session starts at $sessionStartTime us")
    }

    fun finishWriting(completionHandler: () -> Unit) {
        val writerExecutor = synchronized(lock) {
            val executor = executor
            if (statusValue != Status.writing || finishing || executor == null) {
                null
            } else {
                finishing = true
                executor
            }
        }
        if (writerExecutor == null) {
            Log.i(TAG, "Finish writing with status $statusValue, nothing to flush")
            runInBackground(completionHandler)
            return
        }
        for (input in inputs) {
            input.markAsFinished()
        }
        PipelineThread.post {
            try {
                writerExecutor.schedule(
                    Runnable { finalizeWriting(writerExecutor, completionHandler) },
                    finishEncoderDrainMs,
                    TimeUnit.MILLISECONDS,
                )
            } catch (error: RejectedExecutionException) {
                runInBackground {
                    finalizeWriting(null, completionHandler)
                }
            }
        }
    }

    fun cancelWriting() {
        val executor = synchronized(lock) {
            if (statusValue == Status.completed || statusValue == Status.cancelled) {
                return
            }
            statusValue = Status.cancelled
            this.executor
        }
        for (input in inputs) {
            input.markAsFinished()
        }
        if (executor != null) {
            execute(executor) {
                releaseInputs()
                executor.shutdown()
            }
        } else {
            runInBackground {
                releaseInputs()
            }
        }
    }

    fun inputReceiver(input: AVAssetWriterInput): AVAssetWriterInput.SampleBufferReceiver {
        if (input.writer == null) {
            add(input)
        }
        return AVAssetWriterInput.SampleBufferReceiver(input)
    }

    internal fun appendEncodedVideo(sampleBuffer: MediaSample) {
        val executor = executor ?: return
        execute(executor) {
            val muxer = muxer ?: return@execute
            val format = sampleBuffer.format
            if (format != null && format !== latestVideoFormat) {
                latestVideoFormat = format
                val config = makeVideoTrackConfig(format)
                if (config == null) {
                    logOnce("No decoder configuration record in the encoder output format $format")
                } else {
                    muxer.setVideoTrack(config)
                }
            }
            muxer.appendVideo(
                sampleBuffer.data,
                sampleBuffer.presentationTimeUs,
                sampleBuffer.decodeTimeStampUs,
                sampleBuffer.isKeyFrame,
            )
        }
    }

    internal fun appendEncodedAudio(data: ByteArray, presentationTimeUs: Long) {
        val executor = executor ?: return
        execute(executor) {
            muxer?.appendAudio(data, presentationTimeUs)
        }
    }

    internal fun fail(message: String) {
        val (isFinishing, executorToShutDown) = synchronized(lock) {
            if (statusValue != Status.writing && statusValue != Status.unknown) {
                return
            }
            statusValue = Status.failed
            errorValue = AVError(AVError.unknown, message)
            Pair(finishing, executor)
        }
        Log.i(TAG, "Writer failed: $message")
        for (input in inputs) {
            input.markAsFinished()
        }
        if (isFinishing) {
            return
        }
        if (executorToShutDown != null) {
            execute(executorToShutDown) {
                releaseInputs()
                executorToShutDown.shutdown()
            }
        } else {
            runInBackground {
                releaseInputs()
            }
        }
    }

    private fun canAddLocked(input: AVAssetWriterInput): Boolean {
        return statusValue == Status.unknown &&
            input.writer == null &&
            inputList.none { it.mediaType == input.mediaType }
    }

    private fun finalizeWriting(executor: ScheduledExecutorService?, completionHandler: () -> Unit) {
        val muxer = muxer
        try {
            muxer?.finish()
        } catch (error: Throwable) {
            Log.i(TAG, "Failed to flush the last segment: $error")
        }
        releaseInputs()
        synchronized(lock) {
            if (statusValue == Status.writing) {
                statusValue = Status.completed
            }
        }
        Log.i(TAG, "Finished writing ${muxer?.numberOfSegments ?: 0} segments with status $statusValue")
        executor?.shutdown()
        try {
            completionHandler()
        } catch (error: Throwable) {
            Log.i(TAG, "Finish writing completion handler failed: $error")
        }
    }

    private fun releaseInputs() {
        for (input in inputs) {
            try {
                input.release()
            } catch (error: Throwable) {
                Log.i(TAG, "Failed to release ${input.mediaType} input: $error")
            }
        }
    }

    private fun outputSegment(data: ByteArray, segmentType: AVAssetSegmentType, segmentReport: AVAssetSegmentReport?) {
        try {
            delegate?.assetWriter(this, data, segmentType, segmentReport)
        } catch (error: Throwable) {
            Log.i(TAG, "Segment delegate failed: $error")
        }
    }

    private fun makeVideoTrackConfig(format: MediaFormat): Mp4VideoTrackConfig? {
        val mimeType = try {
            format.getString(MediaFormat.KEY_MIME)
        } catch (error: Exception) {
            null
        } ?: return null
        val width = readInteger(format, MediaFormat.KEY_WIDTH) ?: return null
        val height = readInteger(format, MediaFormat.KEY_HEIGHT) ?: return null
        val atoms = CMFormatDescriptionGetExtension(format, kCMFormatDescriptionExtension_SampleDescriptionExtensionAtoms)
            as? Map<*, *> ?: return null
        val key = if (mimeType == MediaFormat.MIMETYPE_VIDEO_HEVC) "hvcC" else "avcC"
        val record = atoms[key] as? ByteArray ?: return null
        return Mp4VideoTrackConfig(
            trackId = 1,
            mimeType = mimeType,
            width = width,
            height = height,
            decoderConfigurationRecord = record,
        )
    }

    private fun readInteger(format: MediaFormat, key: String): Int? {
        return try {
            if (format.containsKey(key)) format.getInteger(key) else null
        } catch (error: Exception) {
            null
        }
    }

    private fun execute(executor: ScheduledExecutorService, block: () -> Unit) {
        try {
            executor.execute {
                try {
                    block()
                } catch (error: Throwable) {
                    Log.i(TAG, "Writer task failed: $error")
                }
            }
        } catch (error: RejectedExecutionException) {
            logOnce("Writer is finished, dropping late samples")
        }
    }

    private fun runInBackground(block: () -> Unit) {
        Thread({
            try {
                block()
            } catch (error: Throwable) {
                Log.i(TAG, "Writer background task failed: $error")
            }
        }, "MoblinRecorderFinish").start()
    }
}

class AVAssetWriterInput(
    val mediaType: AVMediaType,
    val outputSettings: Map<String, Any>?,
    val sourceFormatHint: MediaFormat?,
) {
    class SampleBufferReceiver internal constructor(private val input: AVAssetWriterInput) {
        fun appendImmediately(sampleBuffer: MediaSample): Boolean {
            if (!input.isReadyForMoreMediaData) {
                return false
            }
            if (!input.append(sampleBuffer)) {
                throw AVError(AVError.unknown, input.writer?.error?.message ?: "Append failed")
            }
            return true
        }
    }

    var expectsMediaDataInRealTime = false

    @Volatile
    internal var writer: AVAssetWriter? = null
        private set

    internal var trackID = 0
        private set

    @Volatile
    private var finished = false

    private val videoEncoder = if (mediaType == AVMediaType.video) AssetWriterVideoEncoder(outputSettings) else null

    private val audioEncoder = if (mediaType == AVMediaType.audio) {
        AssetWriterAudioEncoder(outputSettings, sourceFormatHint)
    } else {
        null
    }

    val isReadyForMoreMediaData: Boolean
        get() {
            val writer = writer ?: return false
            if (writer.status == AVAssetWriter.Status.failed) {
                return true
            }
            if (finished || writer.status != AVAssetWriter.Status.writing) {
                return false
            }
            return videoEncoder?.isReadyForMoreMediaData ?: true
        }

    fun append(sampleBuffer: MediaSample): Boolean {
        val writer = writer ?: return false
        if (finished || writer.status != AVAssetWriter.Status.writing) {
            return false
        }
        return try {
            when {
                videoEncoder != null -> videoEncoder.append(writer, sampleBuffer)
                audioEncoder != null -> audioEncoder.append(writer, sampleBuffer)
                else -> false
            }
        } catch (error: Exception) {
            writer.fail("${mediaType.name} append failed: $error")
            false
        }
    }

    fun markAsFinished() {
        finished = true
    }

    internal fun attach(writer: AVAssetWriter, trackID: Int) {
        this.writer = writer
        this.trackID = trackID
        videoEncoder?.prepare(writer)
        audioEncoder?.prepare()
    }

    internal fun makeAudioTrackConfig(): Mp4AudioTrackConfig? {
        return audioEncoder?.makeTrackConfig(trackID)
    }

    internal fun release() {
        finished = true
        videoEncoder?.release()
        audioEncoder?.release()
    }
}

internal class AssetWriterVideoEncoder(outputSettings: Map<String, Any>?) {
    private val lock = Any()
    private val mimeType = when ((outputSettings?.get(AVVideoCodecKey) as? String)?.lowercase()) {
        MediaFormat.MIMETYPE_VIDEO_HEVC, "hevc", "hvc1", "h265" -> MediaFormat.MIMETYPE_VIDEO_HEVC
        else -> MediaFormat.MIMETYPE_VIDEO_AVC
    }
    private val requestedWidth = (outputSettings?.get(AVVideoWidthKey) as? Number)?.toInt() ?: 0
    private val requestedHeight = (outputSettings?.get(AVVideoHeightKey) as? Number)?.toInt() ?: 0
    private val compressionProperties = outputSettings?.get(AVVideoCompressionPropertiesKey) as? Map<*, *>
    private val requestedBitrate = (compressionProperties?.get(AVVideoAverageBitRateKey) as? Number)?.toInt()
    private val requestedKeyFrameInterval =
        (compressionProperties?.get(AVVideoMaxKeyFrameIntervalDurationKey) as? Number)?.toDouble()
    private var session: VTCompressionSession? = null
    private var isCreatingSession = false
    private var hasAppendedFirstFrame = false
    private var numberOfFramesBeforeSession = 0
    private val numberOfEncodeErrors = AtomicInteger(0)

    @Volatile
    private var sessionRequestTimeNs = 0L

    @Volatile
    private var released = false

    val isReadyForMoreMediaData: Boolean
        get() {
            val (session, isCreatingSession) = synchronized(lock) { Pair(session, isCreatingSession) }
            if (session == null) {
                return !isCreatingSession
            }
            return session.isReadyForMoreFrames
        }

    fun prepare(writer: AVAssetWriter) {
        if (requestedWidth > 0 && requestedHeight > 0) {
            startCreatingSession(writer, requestedWidth, requestedHeight)
        }
    }

    fun append(writer: AVAssetWriter, sampleBuffer: MediaSample): Boolean {
        val imageBuffer = sampleBuffer.imageBuffer
        if (imageBuffer == null) {
            logOnce("Video input needs image buffers")
            return false
        }
        if (released) {
            return false
        }
        val hasSession = synchronized(lock) { session != null }
        if (!hasSession) {
            startCreatingSession(
                writer,
                if (requestedWidth > 0) requestedWidth else imageBuffer.width,
                if (requestedHeight > 0) requestedHeight else imageBuffer.height,
            )
            numberOfFramesBeforeSession += 1
            return true
        }
        if (!hasAppendedFirstFrame) {
            hasAppendedFirstFrame = true
            Log.i(
                TAG,
                "First video frame appended ${elapsedMs(sessionRequestTimeNs)} ms after the video encoder was " +
                    "requested, $numberOfFramesBeforeSession frames arrived before the encoder was ready",
            )
        }
        encode(writer, sampleBuffer)
        return true
    }

    fun release() {
        val session = synchronized(lock) {
            released = true
            val current = this.session
            this.session = null
            current
        }
        if (session != null) {
            VTCompressionSessionInvalidate(session)
        }
    }

    private fun startCreatingSession(writer: AVAssetWriter, width: Int, height: Int) {
        synchronized(lock) {
            if (isCreatingSession || session != null || released) {
                return
            }
            isCreatingSession = true
        }
        sessionRequestTimeNs = System.nanoTime()
        val frameRate = CaptureFrameRate.value.roundToInt().coerceIn(5, 120).toDouble()
        Log.i(TAG, "Creating the video encoder for ${width}x$height at ${frameRate.roundToInt()} fps")
        encoderSetupExecutor.execute {
            val created = try {
                createSession(writer, width and 1.inv(), height and 1.inv(), frameRate)
            } catch (error: Throwable) {
                writer.fail("Video encoder setup failed: $error")
                false
            }
            synchronized(lock) {
                isCreatingSession = false
            }
            Log.i(
                TAG,
                "Video encoder ${if (created) "ready" else "not created"} after ${elapsedMs(sessionRequestTimeNs)} ms",
            )
        }
    }

    private fun createSession(writer: AVAssetWriter, width: Int, height: Int, frameRate: Double): Boolean {
        val failures = mutableListOf<String>()
        for (codecType in listOf(mimeType, MediaFormat.MIMETYPE_VIDEO_AVC).distinct()) {
            if (released) {
                return false
            }
            if (failures.isNotEmpty()) {
                Log.i(TAG, "${failures.last()}, falling back to $codecType")
            }
            val session = makeSession(codecType, width, height, frameRate, failures) ?: continue
            val accepted = synchronized(lock) {
                if (released) {
                    false
                } else {
                    this.session = session
                    true
                }
            }
            if (!accepted) {
                VTCompressionSessionInvalidate(session)
                return false
            }
            return true
        }
        writer.fail(failures.joinToString("; "))
        return false
    }

    private fun makeSession(
        codecType: String,
        width: Int,
        height: Int,
        frameRate: Double,
        failures: MutableList<String>,
    ): VTCompressionSession? {
        val (status, session) = VTCompressionSessionCreate(
            width = width,
            height = height,
            codecType = codecType,
            imageBufferAttributes = null,
        )
        if (status != noErr || session == null) {
            failures.add("Failed to create a $codecType encoder for ${width}x$height (status $status)")
            return null
        }
        val isHevc = codecType == MediaFormat.MIMETYPE_VIDEO_HEVC
        val bitrate = requestedBitrate ?: (width.toDouble() * height * frameRate * (if (isHevc) 0.1 else 0.15)).toInt()
        val properties = mapOf<String, Any>(
            "RealTime" to true,
            "ExpectedFrameRate" to frameRate,
            "AllowFrameReordering" to false,
            "MaxKeyFrameIntervalDuration" to (requestedKeyFrameInterval ?: 2.0),
            "ProfileLevel" to if (isHevc) kVTProfileLevel_HEVC_Main_AutoLevel else kVTProfileLevel_H264_High_AutoLevel,
            "AverageBitRate" to bitrate,
        )
        VTSessionSetProperties(session, properties)
        val prepareStatus = VTCompressionSessionPrepareToEncodeFrames(session)
        if (prepareStatus != noErr) {
            VTCompressionSessionInvalidate(session)
            failures.add("Failed to prepare the $codecType encoder for ${width}x$height (status $prepareStatus)")
            return null
        }
        Log.i(
            TAG,
            "Video encoder $codecType ${width}x$height ${frameRate.roundToInt()} fps $bitrate bps " +
                "key frame interval ${requestedKeyFrameInterval ?: 2.0} s",
        )
        return session
    }

    private fun encode(writer: AVAssetWriter, sampleBuffer: MediaSample) {
        val imageBuffer = sampleBuffer.imageBuffer ?: return
        val presentationTimeStamp = sampleBuffer.presentationTimeUs
        val duration = sampleBuffer.durationUs
        if (!PixelBufferLeases.retain(imageBuffer, "recorder encode")) {
            return
        }
        PipelineThread.post {
            try {
                encodeOnPipeline(writer, imageBuffer, presentationTimeStamp, duration)
            } finally {
                releaseLease(imageBuffer)
            }
        }
    }

    private fun encodeOnPipeline(
        writer: AVAssetWriter,
        imageBuffer: CVPixelBuffer,
        presentationTimeStamp: Long,
        duration: Long,
    ) {
        val session = synchronized(lock) { session } ?: return
        val status = VTCompressionSessionEncodeFrame(
            session,
            imageBuffer,
            presentationTimeStamp,
            duration,
            null,
        ) { outputStatus, infoFlags, outputSampleBuffer ->
            handleOutput(writer, outputStatus, infoFlags, outputSampleBuffer)
        }
        if (status == kVTInvalidSessionErr) {
            if (!released) {
                writer.fail("Video encoder session is invalid")
            }
        } else if (status != noErr) {
            val count = numberOfEncodeErrors.incrementAndGet()
            if (count <= 5 || count % 100 == 0) {
                Log.i(TAG, "Video encode failed with status $status ($count so far)")
            }
        }
    }

    private fun handleOutput(writer: AVAssetWriter, status: Int, infoFlags: Int, sampleBuffer: MediaSample?) {
        if (status != noErr) {
            if (!released) {
                writer.fail("Video encoder failed with status $status")
            }
            return
        }
        if ((infoFlags and kVTEncodeInfo_FrameDropped) != 0 || sampleBuffer == null) {
            return
        }
        writer.appendEncodedVideo(sampleBuffer)
    }

    private fun elapsedMs(startNs: Long): Long {
        return (System.nanoTime() - startNs) / 1_000_000
    }
}

internal class AssetWriterAudioEncoder(outputSettings: Map<String, Any>?, sourceFormatHint: MediaFormat?) {
    private val lock = Any()
    private val sampleRate: Int
    private val channelCount: Int
    private val bitrate: Int
    private var converter: AVAudioConverter? = null
    private var converterInputChannels = 0
    private var nextConverterAttemptNs = 0L
    private var pending = ByteArray(0)
    private var pendingFrames = 0
    private var pendingChannels = 0
    private var pendingStartTimeUs = 0.0
    private val chunkPresentationTimeStamps = ArrayDeque<Long>()
    private var released = false
    private val sourceChannels = sourceFormatHint?.audioChannelCount() ?: 0

    init {
        val sourceSampleRate = sourceFormatHint?.audioSampleRate() ?: 0
        val requestedSampleRate = (outputSettings?.get(AVSampleRateKey) as? Number)?.toInt() ?: 0
        val requestedChannels = (outputSettings?.get(AVNumberOfChannelsKey) as? Number)?.toInt() ?: 0
        sampleRate = when {
            sourceSampleRate > 0 -> sourceSampleRate
            requestedSampleRate > 0 -> requestedSampleRate
            else -> 48000
        }
        if (requestedSampleRate > 0 && requestedSampleRate != sampleRate) {
            Log.i(TAG, "AAC is encoded at the source sample rate $sampleRate Hz instead of $requestedSampleRate Hz")
        }
        channelCount = (if (requestedChannels > 0) requestedChannels else sourceChannels).coerceIn(1, 2)
        bitrate = (outputSettings?.get(AVEncoderBitRateKey) as? Number)?.toInt()?.takeIf { it > 0 }
            ?: (64_000 * channelCount)
    }

    fun prepare() {
        if (sourceChannels <= 0) {
            return
        }
        encoderSetupExecutor.execute {
            synchronized(lock) {
                if (!released && converter == null) {
                    getConverter(sourceChannels)
                }
            }
        }
    }

    fun makeTrackConfig(trackID: Int): Mp4AudioTrackConfig {
        return Mp4AudioTrackConfig(
            trackId = trackID,
            sampleRate = sampleRate,
            channelCount = channelCount,
            bitrate = bitrate,
            audioSpecificConfig = makeAacAudioSpecificConfig(sampleRate, channelCount),
        )
    }

    fun append(writer: AVAssetWriter, sampleBuffer: MediaSample): Boolean {
        synchronized(lock) {
            if (released) {
                return false
            }
            val format = sampleBuffer.format ?: return false
            val channels = format.audioChannelCount()
            if (channels <= 0) {
                return false
            }
            if (format.audioSampleRate() != sampleRate) {
                logOnce("Dropping ${format.audioSampleRate()} Hz audio, the AAC encoder runs at $sampleRate Hz")
                return true
            }
            val converter = getConverter(channels) ?: return true
            val frameSize = channels * 2
            val frames = sampleBuffer.data.size / frameSize
            if (frames <= 0) {
                return true
            }
            val presentationTimeStamp = sampleBuffer.presentationTimeUs
            if (pendingFrames > 0) {
                val expectedPresentationTimeStamp = pendingStartTimeUs + pendingFrames * 1_000_000.0 / sampleRate
                if (abs(presentationTimeStamp - expectedPresentationTimeStamp) > 100_000) {
                    pendingFrames = 0
                }
            }
            if (pendingFrames == 0) {
                pendingStartTimeUs = presentationTimeStamp.toDouble()
            }
            val requiredSize = (pendingFrames + frames) * frameSize
            if (pending.size < requiredSize) {
                pending = pending.copyOf(maxOf(requiredSize, aacFramesPerPacket * frameSize * 4))
            }
            System.arraycopy(sampleBuffer.data, 0, pending, pendingFrames * frameSize, frames * frameSize)
            pendingFrames += frames
            val chunkSize = aacFramesPerPacket * frameSize
            var consumedFrames = 0
            while (pendingFrames - consumedFrames >= aacFramesPerPacket) {
                val offset = consumedFrames * frameSize
                val chunk = pending.copyOfRange(offset, offset + chunkSize)
                val chunkPresentationTimeStamp = pendingStartTimeUs.roundToLong()
                encodeChunk(writer, converter, chunk, chunkPresentationTimeStamp)
                consumedFrames += aacFramesPerPacket
                pendingStartTimeUs += aacFramesPerPacket * 1_000_000.0 / sampleRate
            }
            if (consumedFrames > 0) {
                val remainingFrames = pendingFrames - consumedFrames
                System.arraycopy(pending, consumedFrames * frameSize, pending, 0, remainingFrames * frameSize)
                pendingFrames = remainingFrames
            }
            return true
        }
    }

    fun release() {
        synchronized(lock) {
            released = true
            converter?.release()
            converter = null
            chunkPresentationTimeStamps.clear()
            pendingFrames = 0
        }
    }

    private fun getConverter(inputChannels: Int): AVAudioConverter? {
        val current = converter
        if (current != null && converterInputChannels == inputChannels) {
            return current
        }
        if (current != null) {
            Log.i(TAG, "Audio input channels changed from $converterInputChannels to $inputChannels")
            current.release()
            converter = null
        }
        if (pendingChannels != inputChannels) {
            pendingFrames = 0
            pendingChannels = inputChannels
        }
        chunkPresentationTimeStamps.clear()
        val now = System.nanoTime()
        if (now < nextConverterAttemptNs) {
            return null
        }
        val outputFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount)
        outputFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        outputFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
        val newConverter = AVAudioConverter.create(from = makePcmFormat(sampleRate, inputChannels), to = outputFormat)
        if (newConverter == null) {
            Log.i(TAG, "No AAC encoder for $sampleRate Hz $channelCount channels, retrying in 5 s")
            nextConverterAttemptNs = now + 5_000_000_000L
            return null
        }
        newConverter.bitRate = bitrate
        converter = newConverter
        converterInputChannels = inputChannels
        Log.i(TAG, "Audio encoder AAC $sampleRate Hz $channelCount channels $bitrate bps")
        return newConverter
    }

    private fun encodeChunk(
        writer: AVAssetWriter,
        converter: AVAudioConverter,
        chunk: ByteArray,
        presentationTimeStamp: Long,
    ) {
        val outputFormat = converter.outputFormat
        val buffer = AVAudioCompressedBuffer(
            format = outputFormat,
            packetCapacity = 1,
            maximumPacketSize = 1536 * channelCount,
        )
        var consumed = false
        val error = converter.convert(to = buffer) {
            consumed = true
            chunk
        }
        if (consumed) {
            chunkPresentationTimeStamps.addLast(presentationTimeStamp)
            while (chunkPresentationTimeStamps.size > 64) {
                chunkPresentationTimeStamps.removeFirst()
            }
        }
        if (error == null) {
            val packetPresentationTimeStamp = chunkPresentationTimeStamps.removeFirstOrNull() ?: presentationTimeStamp
            writer.appendEncodedAudio(buffer.data, packetPresentationTimeStamp)
        } else if (error.startsWith("encoder failed") || error == "encoder not available") {
            logOnce("AAC encoder problem: $error")
            chunkPresentationTimeStamps.clear()
        }
    }
}

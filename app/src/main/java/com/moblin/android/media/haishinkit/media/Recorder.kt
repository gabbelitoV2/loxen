package com.moblin.android.media.haishinkit.media

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import com.moblin.android.common.various.formatThreeDecimals
import com.moblin.android.media.MediaSample
import com.moblin.android.media.haishinkit.util.isZero
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch

data class RecorderDataSegment(
    val data: ByteArray,
    val startTime: Double,
    val duration: Double,
)

interface RecorderDelegate {
    fun recorderInitSegment(data: ByteArray)

    fun recorderDataSegment(segment: RecorderDataSegment)

    fun recorderFinished()
}

private const val TAG = "Recorder"

private const val maximumAudioPresentationTimeStampOffset = 0.01

private val queue = CoroutineScope(
    Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "com.eerimoq.recorder") }
        .asCoroutineDispatcher(),
)

private val fileWriterQueue = CoroutineScope(
    Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "com.eerimoq.recorder-file-writer") }
        .asCoroutineDispatcher(),
)

sealed class SegmentType {
    object Initialization : SegmentType()

    object Separable : SegmentType()

    object Media : SegmentType()
}

class SegmentTrackReport(
    val earliestPresentationTimeStamp: Long,
    val duration: Long,
)

class SegmentReport(
    val trackReports: List<SegmentTrackReport>,
)

private sealed class RecorderError(message: String) : Exception(message) {
    object AppendFailed : RecorderError("appendFailed")
}

private interface WriterInput {
    fun append(sampleBuffer: MediaSample): Boolean
}

private class LegacyWriterInput(private val writer: MediaMuxer, private val trackIndex: Int) :
    WriterInput
{
    override fun append(sampleBuffer: MediaSample): Boolean {
        val byteBuffer = ByteBuffer.wrap(sampleBuffer.data)
        val bufferInfo = MediaCodec.BufferInfo()
        bufferInfo.set(
            0,
            sampleBuffer.data.size,
            sampleBuffer.presentationTimeUs,
            if (sampleBuffer.isKeyFrame) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0,
        )
        try {
            writer.writeSampleData(trackIndex, byteBuffer, bufferInfo)
        } catch (e: Exception) {
            throw RecorderError.AppendFailed
        }
        return true
    }
}

private class ReceiverWriterInput(writer: MediaMuxer, input: MediaFormat) : WriterInput {
    override fun append(sampleBuffer: MediaSample): Boolean {
        TODO("AVAssetWriterInput.SampleBufferReceiver has no Android counterpart")
    }
}

private fun MediaFormat.getIntOrNull(key: String): Int? {
    return runCatching { getInteger(key) }.getOrNull()
}

private fun numSamples(sampleBuffer: MediaSample): Int {
    val format = sampleBuffer.format ?: return 0
    val channels = format.getIntOrNull(MediaFormat.KEY_CHANNEL_COUNT) ?: return 0
    if (channels <= 0) {
        return 0
    }
    return sampleBuffer.data.size / (channels * 2)
}

private fun MediaSample.replacePresentationTimeStamp(presentationTimeUs: Long): MediaSample? {
    return MediaSample(
        data = data,
        presentationTimeUs = presentationTimeUs,
        isKeyFrame = isKeyFrame,
        format = format,
    )
}

class Recorder {
    private var replay = false
    private var audioOutputSettings: Map<String, Any> = emptyMap()
    private var videoOutputSettings: Map<String, Any> = emptyMap()
    private var fileHandle: RandomAccessFile? = null
    private var initSegment: ByteArray? = null
    private var outputChannelsMap: Map<Int, Int> = mapOf(0 to 0, 1 to 1)
    private var writer: MediaMuxer? = null
    private var audioWriterInput: WriterInput? = null
    private var videoWriterInput: WriterInput? = null
    private var audioConverter: MediaCodec? = null
    private var audioOutputFormat: MediaFormat? = null
    private var basePresentationTimeStamp: Long? = null
    private var nextAudioPresentationTimeStamp: Long? = null
    @Volatile private var isRecording: Boolean = false
    var delegate: RecorderDelegate? = null
    private var numberOfTracks = 0

    fun setAudioChannelsMap(map: Map<Int, Int>) {
        queue.launch {
            this@Recorder.outputChannelsMap = map
        }
    }

    fun startRunning(
        url: String?,
        replay: Boolean,
        audioOutputSettings: Map<String, Any>,
        videoOutputSettings: Map<String, Any>,
    ) {
        queue.launch {
            startRunningInternal(url, replay, audioOutputSettings, videoOutputSettings)
        }
        processorPipelineQueue.launch {
            this@Recorder.isRecording = true
        }
    }

    fun stopRunning() {
        queue.launch {
            stopRunningInternal()
        }
        processorPipelineQueue.launch {
            this@Recorder.isRecording = false
        }
    }

    fun setUrl(url: String?) {
        fileWriterQueue.launch {
            if (url != null) {
                runCatching { File(url).writeBytes(ByteArray(0)) }
                fileHandle = RandomAccessFile(File(url), "rw")
                initSegment?.let { fileHandle?.write(it) }
            } else {
                fileHandle?.close()
                fileHandle = null
            }
        }
    }

    fun setReplayBuffering(enabled: Boolean) {
        fileWriterQueue.launch {
            replay = enabled
            initSegment?.let { delegate?.recorderInitSegment(it) }
        }
    }

    fun appendAudio(sampleBuffer: MediaSample, presentationTimeStamp: Long) {
        if (!isRecording) {
            return
        }
        queue.launch {
            appendAudioInternal(sampleBuffer, presentationTimeStamp)
        }
    }

    fun appendVideo(sampleBuffer: MediaSample) {
        if (!isRecording) {
            return
        }
        queue.launch {
            appendVideoInternal(sampleBuffer)
        }
    }

    private fun appendAudioInternal(sampleBuffer: MediaSample, presentationTimeStamp: Long) {
        val writer = writer ?: return
        val convertedSampleBuffer = convertAudio(sampleBuffer, presentationTimeStamp) ?: return
        val input = getAudioWriterInput(convertedSampleBuffer) ?: return
        val duration = makeAudioDuration(numSamples(convertedSampleBuffer)) ?: return
        if (!isReadyForStartWriting(writer)) {
            return
        }
        val base = basePresentationTimeStamp ?: return
        val adjustedPresentationTimeStamp = presentationTimeStamp - base
        if (adjustedPresentationTimeStamp < 0) {
            return
        }
        if (nextAudioPresentationTimeStamp == null) {
            nextAudioPresentationTimeStamp = 0
        }
        val next = nextAudioPresentationTimeStamp ?: 0
        val offset = (adjustedPresentationTimeStamp - next) / 1_000_000.0
        if (offset < -maximumAudioPresentationTimeStampOffset) {
            return
        }
        if (offset > maximumAudioPresentationTimeStampOffset && offset < 10) {
            appendAudioSilence(writer, input, next, adjustedPresentationTimeStamp)
        }
        val adjustedSampleBuffer =
            convertedSampleBuffer.replacePresentationTimeStamp(adjustedPresentationTimeStamp)
                ?: return
        if (!appendAudioSampleBuffer(writer, input, adjustedSampleBuffer)) {
            return
        }
        nextAudioPresentationTimeStamp = adjustedPresentationTimeStamp + duration
    }

    private fun makeAudioDuration(numberOfFrames: Int): Long? {
        val outputFormat = audioConverter?.outputFormat ?: return null
        val sampleRate = outputFormat.getIntOrNull(MediaFormat.KEY_SAMPLE_RATE) ?: return null
        if (sampleRate <= 0) {
            return null
        }
        return numberOfFrames.toLong() * 1_000_000L / sampleRate
    }

    private fun appendAudioSilence(writer: MediaMuxer, input: WriterInput, from: Long, to: Long) {
        val outputFormat = audioConverter?.outputFormat ?: return
        val sampleRate = outputFormat.getIntOrNull(MediaFormat.KEY_SAMPLE_RATE) ?: return
        val numberOfFrames = ((to - from) / 1_000_000.0 * sampleRate).roundToInt()
        if (numberOfFrames <= 0) {
            return
        }
        val channels = outputFormat.getIntOrNull(MediaFormat.KEY_CHANNEL_COUNT) ?: 1
        val data = ByteArray(numberOfFrames * channels * 2)
        val sampleBuffer = MediaSample(
            data = data,
            presentationTimeUs = from,
            isKeyFrame = true,
            format = outputFormat,
        )
        Log.i(
            TAG,
            "recorder: audio: Inserting ${formatThreeDecimals((to - from) / 1_000_000.0)} seconds " +
                "of silence at ${formatThreeDecimals(from / 1_000_000.0)}",
        )
        appendAudioSampleBuffer(writer, input, sampleBuffer)
    }

    private fun appendAudioSampleBuffer(
        writer: MediaMuxer,
        input: WriterInput,
        sampleBuffer: MediaSample,
    ): Boolean {
        return try {
            input.append(sampleBuffer)
        } catch (e: Exception) {
            Log.i(
                TAG,
                "recorder: audio: Append failed with ${e.localizedMessage ?: ""}",
            )
            stopRunningInternal()
            false
        }
    }

    private fun appendVideoInternal(sampleBuffer: MediaSample) {
        val writer = writer ?: return
        val input = getVideoWriterInput(sampleBuffer) ?: return
        if (!isReadyForStartWriting(writer)) {
            return
        }
        if (basePresentationTimeStamp == null) {
            basePresentationTimeStamp = sampleBuffer.presentationTimeUs
        }
        val base = basePresentationTimeStamp ?: return
        val adjustedSampleBuffer =
            sampleBuffer.replacePresentationTimeStamp(sampleBuffer.presentationTimeUs - base)
                ?: return
        try {
            input.append(adjustedSampleBuffer)
        } catch (e: Exception) {
            Log.i(
                TAG,
                "recorder: video: Append failed with ${e.localizedMessage ?: ""}",
            )
            stopRunningInternal()
        }
    }

    private fun convertAudio(sampleBuffer: MediaSample, presentationTimeStamp: Long): MediaSample? {
        return tryConvertAudio(sampleBuffer, presentationTimeStamp, false)
            ?: tryConvertAudio(sampleBuffer, presentationTimeStamp, true)
    }

    private fun tryConvertAudio(
        sampleBuffer: MediaSample,
        presentationTimeStamp: Long,
        makeConverter: Boolean,
    ): MediaSample? {
        if (makeConverter) {
            makeAudioConverter(sampleBuffer.format)
        }
        if (audioConverter == null) {
            return null
        }
        TODO("AVAudioConverter has no Android counterpart; resample to the recorder format with MediaCodec")
    }

    private fun createAudioWriterInput(sampleBuffer: MediaSample): WriterInput? {
        val sourceFormatHint = sampleBuffer.format
        val outputSettings = mutableMapOf<String, Any>()
        if (sourceFormatHint != null && sourceFormatHint.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
            for ((key, value) in audioOutputSettings) {
                when (key) {
                    MediaFormat.KEY_SAMPLE_RATE ->
                        outputSettings[key] =
                            if (isZero(value)) {
                                sourceFormatHint.getIntOrNull(MediaFormat.KEY_SAMPLE_RATE) ?: 0
                            } else {
                                value
                            }
                    MediaFormat.KEY_CHANNEL_COUNT ->
                        outputSettings[key] =
                            if (isZero(value)) {
                                minOf(
                                    sourceFormatHint
                                        .getIntOrNull(MediaFormat.KEY_CHANNEL_COUNT) ?: 1,
                                    2,
                                )
                            } else {
                                value
                            }
                    else -> outputSettings[key] = value
                }
            }
        }
        return makeWriterInput(MediaFormat.MIMETYPE_AUDIO_AAC, outputSettings, sampleBuffer)
    }

    private fun getAudioWriterInput(sampleBuffer: MediaSample): WriterInput? {
        if (audioWriterInput == null) {
            audioWriterInput = createAudioWriterInput(sampleBuffer)
        }
        return audioWriterInput
    }

    private fun createVideoWriterInput(sampleBuffer: MediaSample): WriterInput? {
        val format = sampleBuffer.format ?: return null
        val outputSettings = mutableMapOf<String, Any>()
        for ((key, value) in videoOutputSettings) {
            when (key) {
                MediaFormat.KEY_HEIGHT ->
                    outputSettings[key] =
                        if (isZero(value)) {
                            format.getIntOrNull(MediaFormat.KEY_HEIGHT) ?: 0
                        } else {
                            value
                        }
                MediaFormat.KEY_WIDTH ->
                    outputSettings[key] =
                        if (isZero(value)) {
                            format.getIntOrNull(MediaFormat.KEY_WIDTH) ?: 0
                        } else {
                            value
                        }
                else -> outputSettings[key] = value
            }
        }
        return makeWriterInput(MediaFormat.MIMETYPE_VIDEO_AVC, outputSettings, sampleBuffer)
    }

    private fun getVideoWriterInput(sampleBuffer: MediaSample): WriterInput? {
        if (videoWriterInput == null) {
            videoWriterInput = createVideoWriterInput(sampleBuffer)
        }
        return videoWriterInput
    }

    private fun makeWriterInput(
        mediaType: String,
        outputSettings: Map<String, Any>,
        sampleBuffer: MediaSample,
    ): WriterInput? {
        val writer = writer ?: return null
        if (sampleBuffer.format != null) {
            Log.d(TAG, "recorder: Make writer: Output: $outputSettings, Input: ${sampleBuffer.format}")
        }
        val format = MediaFormat()
        format.setString(MediaFormat.KEY_MIME, mediaType)
        for ((key, value) in outputSettings) {
            when (value) {
                is Int -> format.setInteger(key, value)
                is Long -> format.setLong(key, value)
                is Float -> format.setFloat(key, value)
                is String -> format.setString(key, value)
            }
        }
        val trackIndex = runCatching { writer.addTrack(format) }.getOrNull() ?: return null
        numberOfTracks += 1
        val writerInput: WriterInput = LegacyWriterInput(writer, trackIndex)
        if (numberOfTracks == 2) {
            runCatching { writer.start() }
        }
        return writerInput
    }

    private fun makeAudioConverter(formatDescription: MediaFormat?) {
        TODO("AVAudioConverter and AVAudioFormat have no Android counterpart")
    }

    private fun makeChannelLayout(numberOfChannels: Long): Int? {
        TODO("AVAudioChannelLayout has no Android counterpart; MediaFormat.KEY_CHANNEL_MASK is used instead")
    }

    private fun makeAudioFormat(basicDescription: MediaFormat): MediaFormat? {
        TODO("AVAudioFormat(streamDescription:) has no Android counterpart")
    }

    private fun startRunningInternal(
        url: String?,
        replay: Boolean,
        audioOutputSettings: Map<String, Any>,
        videoOutputSettings: Map<String, Any>,
    ) {
        fileWriterQueue.launch {
            this@Recorder.replay = replay
        }
        this.audioOutputSettings = audioOutputSettings
        this.videoOutputSettings = videoOutputSettings
        if (writer != null) {
            Log.i(TAG, "recorder: Will not start recording as it is already running or missing URL")
            return
        }
        reset()
        writer = TODO(
            "AVAssetWriter(contentType: .mpeg4Movie) with shouldOptimizeForNetworkUse, " +
                "outputFileTypeProfile = .mpeg4AppleHLS, preferredOutputSegmentInterval, " +
                "initialSegmentStartTime and its segment delegate has no Android counterpart; " +
                "MediaMuxer cannot emit an initialization segment plus media segments",
        )
        setUrl(url)
    }

    private fun stopRunningInternal() {
        val writer = writer
        if (writer == null) {
            Log.i(TAG, "recorder: Will not stop recording as it is not running")
            return
        }
        val finished = runCatching { writer.stop() }.isSuccess
        if (finished) {
            fileWriterQueue.launch {
                delegate?.recorderFinished()
            }
        } else {
            Log.i(TAG, "recorder: Failed to finish writing")
        }
        reset()
    }

    private fun reset() {
        runCatching { writer?.release() }
        writer = null
        audioWriterInput = null
        videoWriterInput = null
        audioConverter = null
        audioOutputFormat = null
        basePresentationTimeStamp = null
        nextAudioPresentationTimeStamp = null
        numberOfTracks = 0
        fileWriterQueue.launch {
            fileHandle?.close()
            fileHandle = null
            initSegment = null
        }
    }

    private fun isReadyForStartWriting(writer: MediaMuxer): Boolean {
        return numberOfTracks == 2
    }

    fun assetWriter(
        writer: MediaMuxer,
        segmentData: ByteArray,
        segmentType: SegmentType,
        segmentReport: SegmentReport?,
    ) {
        fileWriterQueue.launch {
            fileHandle?.write(segmentData)
            if (segmentType == SegmentType.Initialization) {
                initSegment = segmentData
            }
            if (replay) {
                when (segmentType) {
                    SegmentType.Initialization -> delegate?.recorderInitSegment(segmentData)
                    SegmentType.Separable -> {
                        val report = segmentReport?.trackReports?.firstOrNull()
                        if (report != null) {
                            delegate?.recorderDataSegment(
                                RecorderDataSegment(
                                    data = segmentData,
                                    startTime = report.earliestPresentationTimeStamp /
                                        1_000_000.0,
                                    duration = report.duration / 1_000_000.0,
                                ),
                            )
                        }
                    }
                    else -> Unit
                }
            }
        }
    }
}

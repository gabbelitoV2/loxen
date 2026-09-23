package com.moblin.android.media.haishinkit.media

import android.media.AudioFormat
import android.media.MediaFormat
import android.util.Log
import com.moblin.android.common.various.formatThreeDecimals
import com.moblin.android.media.MediaSample
import com.moblin.android.media.haishinkit.media.audio.makeChannelMap
import com.moblin.android.media.haishinkit.util.isZero
import com.moblin.android.media.kCMTimeInvalidUs
import com.moblin.android.platform.audio.audioChannelCount
import com.moblin.android.platform.audio.audioSampleRate
import com.moblin.android.platform.audio.isRawPcmAudio
import com.moblin.android.platform.audio.isSameAudioFormat
import com.moblin.android.platform.audio.makePcmFormat
import com.moblin.android.platform.audio.pcmEncoding
import com.moblin.android.platform.avfoundation.AVAssetSegmentReport
import com.moblin.android.platform.avfoundation.AVAssetSegmentType
import com.moblin.android.platform.avfoundation.AVAssetWriter
import com.moblin.android.platform.avfoundation.AVAssetWriterDelegate
import com.moblin.android.platform.avfoundation.AVAssetWriterInput
import com.moblin.android.platform.avfoundation.AVAudioConverter
import com.moblin.android.platform.avfoundation.AVAudioPCMBuffer
import com.moblin.android.platform.avfoundation.AVFileTypeProfile
import com.moblin.android.platform.avfoundation.AVMediaType
import com.moblin.android.platform.avfoundation.AVNumberOfChannelsKey
import com.moblin.android.platform.avfoundation.AVSampleRateKey
import com.moblin.android.platform.avfoundation.AVVideoHeightKey
import com.moblin.android.platform.avfoundation.AVVideoWidthKey
import com.moblin.android.platform.avfoundation.UTType
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URI
import java.util.concurrent.Executors
import kotlin.math.min
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

private sealed class RecorderError(message: String) : Exception(message) {
    object AppendFailed : RecorderError("appendFailed")
}

private interface WriterInput {
    fun append(sampleBuffer: MediaSample): Boolean
}

private class LegacyWriterInput(writer: AVAssetWriter, private val input: AVAssetWriterInput) : WriterInput {
    init {
        writer.add(input)
    }

    override fun append(sampleBuffer: MediaSample): Boolean {
        if (!input.isReadyForMoreMediaData) {
            return false
        }
        if (!input.append(sampleBuffer)) {
            throw RecorderError.AppendFailed
        }
        return true
    }
}

private class ReceiverWriterInput(writer: AVAssetWriter, input: AVAssetWriterInput) : WriterInput {
    private val receiver: AVAssetWriterInput.SampleBufferReceiver = writer.inputReceiver(input)

    override fun append(sampleBuffer: MediaSample): Boolean {
        return receiver.appendImmediately(sampleBuffer)
    }
}

class Recorder : AVAssetWriterDelegate {
    private var replay = false
    private var audioOutputSettings: Map<String, Any> = emptyMap()
    private var videoOutputSettings: Map<String, Any> = emptyMap()
    private var fileHandle: FileOutputStream? = null
    private var initSegment: ByteArray? = null
    private var outputChannelsMap: Map<Int, Int> = mapOf(0 to 0, 1 to 1)
    private var writer: AVAssetWriter? = null
    private var audioWriterInput: WriterInput? = null
    private var videoWriterInput: WriterInput? = null
    private var audioConverter: AVAudioConverter? = null
    private var audioOutputFormat: MediaFormat? = null
    private var basePresentationTimeStamp: Long = kCMTimeInvalidUs
    private var nextAudioPresentationTimeStamp: Long = kCMTimeInvalidUs

    @Volatile
    private var isRecording: Boolean = false
    var delegate: RecorderDelegate? = null

    fun setAudioChannelsMap(map: Map<Int, Int>) {
        queue.launch {
            outputChannelsMap = map
        }
    }

    fun startRunning(
        url: String?,
        replay: Boolean,
        audioOutputSettings: Map<String, Any>,
        videoOutputSettings: Map<String, Any>,
    ) {
        queue.launch {
            startRunningInternal(
                url = url,
                replay = replay,
                audioOutputSettings = audioOutputSettings,
                videoOutputSettings = videoOutputSettings,
            )
        }
        processorPipelineQueue.launch {
            isRecording = true
        }
    }

    fun stopRunning() {
        queue.launch {
            stopRunningInternal()
        }
        processorPipelineQueue.launch {
            isRecording = false
        }
    }

    fun setUrl(url: String?) {
        fileWriterQueue.launch {
            if (url != null) {
                val file = if (url.startsWith("file:")) {
                    runCatching { File(URI(url)) }.getOrElse { File(url) }
                } else {
                    File(url)
                }
                runCatching { file.writeBytes(ByteArray(0)) }
                runCatching { fileHandle?.close() }
                fileHandle = runCatching { FileOutputStream(file) }.getOrNull()
                val initSegment = initSegment
                if (initSegment != null) {
                    try {
                        fileHandle?.write(initSegment)
                    } catch (error: IOException) {
                        Log.i(TAG, "recorder: Failed to write init segment: $error")
                    }
                }
            } else {
                runCatching { fileHandle?.close() }
                fileHandle = null
            }
        }
    }

    fun setReplayBuffering(enabled: Boolean) {
        fileWriterQueue.launch {
            replay = enabled
            val initSegment = initSegment
            if (initSegment != null) {
                delegate?.recorderInitSegment(data = initSegment)
            }
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
        val input = getAudioWriterInput(sampleBuffer = convertedSampleBuffer) ?: return
        val duration = makeAudioDuration(numberOfFrames = convertedSampleBuffer.numSamples) ?: return
        if (!isReadyForStartWriting(writer = writer) || basePresentationTimeStamp == kCMTimeInvalidUs) {
            return
        }
        val relativePresentationTimeStamp = presentationTimeStamp - basePresentationTimeStamp
        if (relativePresentationTimeStamp < 0) {
            return
        }
        if (nextAudioPresentationTimeStamp == kCMTimeInvalidUs) {
            nextAudioPresentationTimeStamp = 0
        }
        val offset = (relativePresentationTimeStamp - nextAudioPresentationTimeStamp) / 1_000_000.0
        if (offset < -maximumAudioPresentationTimeStampOffset) {
            return
        }
        if (offset > maximumAudioPresentationTimeStampOffset && offset < 10) {
            appendAudioSilence(writer, input, nextAudioPresentationTimeStamp, relativePresentationTimeStamp)
        }
        val relativeSampleBuffer = convertedSampleBuffer.replacePresentationTimeStamp(relativePresentationTimeStamp)
        if (!appendAudioSampleBuffer(writer, input, relativeSampleBuffer)) {
            return
        }
        nextAudioPresentationTimeStamp = relativePresentationTimeStamp + duration
    }

    private fun makeAudioDuration(numberOfFrames: Int): Long? {
        val outputFormat = audioConverter?.outputFormat ?: return null
        val sampleRate = outputFormat.audioSampleRate()
        if (sampleRate <= 0) {
            return null
        }
        return numberOfFrames.toLong() * 1_000_000L / sampleRate
    }

    private fun appendAudioSilence(
        writer: AVAssetWriter,
        input: WriterInput,
        from: Long,
        to: Long,
    ) {
        val outputFormat = audioConverter?.outputFormat ?: return
        val numberOfFrames = ((to - from) / 1_000_000.0 * outputFormat.audioSampleRate()).roundToInt()
        if (numberOfFrames <= 0) {
            return
        }
        val buffer = AVAudioPCMBuffer(pcmFormat = outputFormat, frameCapacity = numberOfFrames) ?: return
        buffer.data.fill(0)
        val sampleBuffer = buffer.replacePresentationTimeStamp(from)
        Log.i(
            TAG,
            "recorder: audio: Inserting ${formatThreeDecimals((to - from) / 1_000_000.0)} seconds of silence at " +
                formatThreeDecimals(from / 1_000_000.0),
        )
        appendAudioSampleBuffer(writer, input, sampleBuffer)
    }

    private fun appendAudioSampleBuffer(
        writer: AVAssetWriter,
        input: WriterInput,
        sampleBuffer: MediaSample,
    ): Boolean {
        return try {
            input.append(sampleBuffer)
        } catch (error: Exception) {
            Log.i(
                TAG,
                "recorder: audio: Append failed with ${writer.error?.localizedMessage ?: ""} " +
                    "(status: ${writer.status})",
            )
            stopRunningInternal()
            false
        }
    }

    private fun appendVideoInternal(sampleBuffer: MediaSample) {
        val writer = writer ?: return
        val input = getVideoWriterInput(sampleBuffer = sampleBuffer) ?: return
        if (!isReadyForStartWriting(writer = writer)) {
            return
        }
        if (basePresentationTimeStamp == kCMTimeInvalidUs) {
            basePresentationTimeStamp = sampleBuffer.presentationTimeUs
        }
        val relativeSampleBuffer = sampleBuffer
            .replacePresentationTimeStamp(sampleBuffer.presentationTimeUs - basePresentationTimeStamp)
        try {
            input.append(relativeSampleBuffer)
        } catch (error: Exception) {
            Log.i(
                TAG,
                "recorder: video: Append failed with ${writer.error?.localizedMessage ?: ""} " +
                    "(status: ${writer.status})",
            )
            stopRunningInternal()
        }
    }

    private fun convertAudio(sampleBuffer: MediaSample, presentationTimeStamp: Long): MediaSample? {
        return tryConvertAudio(sampleBuffer, presentationTimeStamp, makeConverter = false)
            ?: tryConvertAudio(sampleBuffer, presentationTimeStamp, makeConverter = true)
    }

    private fun tryConvertAudio(
        sampleBuffer: MediaSample,
        presentationTimeStamp: Long,
        makeConverter: Boolean,
    ): MediaSample? {
        if (makeConverter) {
            makeAudioConverter(sampleBuffer.format)
        }
        val converter = audioConverter ?: return null
        val outputBuffer = AVAudioPCMBuffer(
            pcmFormat = converter.outputFormat,
            frameCapacity = sampleBuffer.numSamples,
        ) ?: return null
        val inputBuffer = if (sampleBuffer.format.isSameAudioFormat(converter.inputFormat)) {
            sampleBuffer
        } else {
            null
        }
        if (inputBuffer == null) {
            Log.i(TAG, "recorder: Failed to create input buffer")
            return null
        }
        val error = converter.convert(to = outputBuffer, from = inputBuffer)
        if (error != null) {
            Log.i(TAG, "recorder: audio: Convert failed with $error")
            return null
        }
        return outputBuffer.replacePresentationTimeStamp(presentationTimeStamp)
    }

    private fun createAudioWriterInput(sampleBuffer: MediaSample): WriterInput? {
        val sourceFormatHint = sampleBuffer.format
        val outputSettings = mutableMapOf<String, Any>()
        if (sourceFormatHint != null && sourceFormatHint.audioSampleRate() > 0) {
            for ((key, value) in audioOutputSettings) {
                when (key) {
                    AVSampleRateKey ->
                        outputSettings[key] = if (isZero(value)) sourceFormatHint.audioSampleRate() else value
                    AVNumberOfChannelsKey ->
                        outputSettings[key] = if (isZero(value)) {
                            min(sourceFormatHint.audioChannelCount(), 2)
                        } else {
                            value
                        }
                    else -> outputSettings[key] = value
                }
            }
        }
        return makeWriterInput(AVMediaType.audio, outputSettings, sampleBuffer)
    }

    private fun getAudioWriterInput(sampleBuffer: MediaSample): WriterInput? {
        if (audioWriterInput == null) {
            audioWriterInput = createAudioWriterInput(sampleBuffer = sampleBuffer)
        }
        return audioWriterInput
    }

    private fun createVideoWriterInput(sampleBuffer: MediaSample): WriterInput? {
        val pixelBuffer = sampleBuffer.imageBuffer ?: return null
        val outputSettings = mutableMapOf<String, Any>()
        for ((key, value) in videoOutputSettings) {
            when (key) {
                AVVideoHeightKey -> outputSettings[key] = if (isZero(value)) pixelBuffer.height else value
                AVVideoWidthKey -> outputSettings[key] = if (isZero(value)) pixelBuffer.width else value
                else -> outputSettings[key] = value
            }
        }
        return makeWriterInput(AVMediaType.video, outputSettings, sampleBuffer)
    }

    private fun getVideoWriterInput(sampleBuffer: MediaSample): WriterInput? {
        if (videoWriterInput == null) {
            videoWriterInput = createVideoWriterInput(sampleBuffer = sampleBuffer)
        }
        return videoWriterInput
    }

    private fun makeWriterInput(
        mediaType: AVMediaType,
        outputSettings: Map<String, Any>,
        sampleBuffer: MediaSample,
    ): WriterInput? {
        val writer = writer ?: return null
        val audioStreamBasicDescription = sampleBuffer.format?.takeIf { it.isRawPcmAudio() }
        if (audioStreamBasicDescription != null) {
            Log.d(TAG, "recorder: Make writer: Output: $outputSettings, Input: $audioStreamBasicDescription")
        }
        val input = AVAssetWriterInput(
            mediaType = mediaType,
            outputSettings = outputSettings,
            sourceFormatHint = sampleBuffer.format,
        )
        input.expectsMediaDataInRealTime = true
        val writerInput: WriterInput = LegacyWriterInput(writer, input)
        if (writer.inputs.size == 2) {
            writer.startWriting()
            writer.startSession(atSourceTime = 0L)
        }
        return writerInput
    }

    private fun makeAudioConverter(formatDescription: MediaFormat?) {
        val streamBasicDescription = formatDescription?.takeIf { it.audioSampleRate() > 0 } ?: return
        Log.d(TAG, "recorder: Creating converter from $streamBasicDescription")
        val inputFormat = makeAudioFormat(streamBasicDescription) ?: return
        if (audioOutputFormat == null) {
            audioOutputFormat = makePcmFormat(
                sampleRate = inputFormat.audioSampleRate(),
                channels = min(inputFormat.audioChannelCount(), 2),
            )
        }
        val audioOutputFormat = audioOutputFormat ?: return
        Log.d(TAG, "recorder: Input: $inputFormat, output: $audioOutputFormat")
        audioConverter?.release()
        audioConverter = AVAudioConverter.create(from = inputFormat, to = audioOutputFormat)
        audioConverter?.channelMap = makeChannelMap(
            numberOfInputChannels = inputFormat.audioChannelCount(),
            numberOfOutputChannels = audioOutputFormat.audioChannelCount(),
            outputToInputChannelsMap = outputChannelsMap,
        )
    }

    private fun makeChannelLayout(numberOfChannels: Int): Int? {
        if (numberOfChannels <= 2) {
            return null
        }
        return (1 shl numberOfChannels) - 1
    }

    private fun makeAudioFormat(basicDescription: MediaFormat): MediaFormat? {
        if (!basicDescription.isRawPcmAudio() || basicDescription.pcmEncoding() != AudioFormat.ENCODING_PCM_16BIT) {
            return null
        }
        val sampleRate = basicDescription.audioSampleRate()
        val numberOfChannels = basicDescription.audioChannelCount()
        if (sampleRate <= 0 || numberOfChannels <= 0) {
            return null
        }
        val format = makePcmFormat(sampleRate = sampleRate, channels = numberOfChannels)
        val layout = makeChannelLayout(numberOfChannels)
        if (layout != null) {
            format.setInteger("channel-index-mask", layout)
        }
        return format
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
        writer = AVAssetWriter(contentType = UTType.mpeg4Movie)
        writer?.shouldOptimizeForNetworkUse = true
        writer?.outputFileTypeProfile = AVFileTypeProfile.mpeg4AppleHLS
        writer?.preferredOutputSegmentInterval = 2_000_000
        writer?.delegate = this
        writer?.initialSegmentStartTime = 0
        setUrl(url = url)
    }

    private fun stopRunningInternal() {
        val writer = writer
        if (writer == null) {
            Log.i(TAG, "recorder: Will not stop recording as it is not running")
            return
        }
        if (writer.status != AVAssetWriter.Status.writing) {
            Log.i(TAG, "recorder: Failed to finish writing ${writer.error?.localizedMessage ?: ""}")
            reset()
            return
        }
        writer.finishWriting {
            delegate?.recorderFinished()
        }
        reset()
    }

    private fun reset() {
        writer = null
        audioWriterInput = null
        videoWriterInput = null
        audioConverter?.release()
        audioConverter = null
        audioOutputFormat = null
        basePresentationTimeStamp = kCMTimeInvalidUs
        nextAudioPresentationTimeStamp = kCMTimeInvalidUs
        fileWriterQueue.launch {
            runCatching { fileHandle?.close() }
            fileHandle = null
            initSegment = null
        }
    }

    private fun isReadyForStartWriting(writer: AVAssetWriter): Boolean {
        return writer.inputs.size == 2
    }

    override fun assetWriter(
        writer: AVAssetWriter,
        didOutputSegmentData: ByteArray,
        segmentType: AVAssetSegmentType,
        segmentReport: AVAssetSegmentReport?,
    ) {
        fileWriterQueue.launch {
            try {
                fileHandle?.write(didOutputSegmentData)
            } catch (error: IOException) {
                Log.i(TAG, "recorder: Failed to write segment: $error")
            }
            if (segmentType == AVAssetSegmentType.initialization) {
                initSegment = didOutputSegmentData
            }
            if (replay) {
                when (segmentType) {
                    AVAssetSegmentType.initialization -> delegate?.recorderInitSegment(data = didOutputSegmentData)
                    AVAssetSegmentType.separable -> {
                        val report = segmentReport?.trackReports?.firstOrNull()
                        if (report != null) {
                            delegate?.recorderDataSegment(
                                segment = RecorderDataSegment(
                                    data = didOutputSegmentData,
                                    startTime = report.earliestPresentationTimeStamp / 1_000_000.0,
                                    duration = report.duration / 1_000_000.0,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

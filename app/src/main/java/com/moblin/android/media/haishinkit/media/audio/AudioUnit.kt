package com.moblin.android.media.haishinkit.media.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaFormat
import android.util.Log
import com.moblin.android.common.various.*
import com.moblin.android.media.MediaSample
import com.moblin.android.media.haishinkit.codec.audio.AudioEncoder
import com.moblin.android.media.haishinkit.codec.audio.AudioEncoderDelegate
import com.moblin.android.media.haishinkit.codec.audio.AudioEncoderSettings
import com.moblin.android.media.haishinkit.media.Processor
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.platform.audio.isSameAudioFormat
import com.moblin.android.platform.avfoundation.AVCaptureAudioDataOutput
import com.moblin.android.platform.avfoundation.AVCaptureAudioDataOutputSampleBufferDelegate
import com.moblin.android.platform.avfoundation.AVCaptureConnection
import com.moblin.android.platform.avfoundation.AVCaptureDevice
import com.moblin.android.platform.avfoundation.AVCaptureDeviceInput
import com.moblin.android.platform.avfoundation.AVCaptureOutput
import com.moblin.android.platform.avfoundation.AVCaptureSession
import java.util.UUID
import kotlin.coroutines.ContinuationInterceptor
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch

private const val TAG = "AudioUnit"

private val processorPipelineDispatcher: CoroutineDispatcher
    get() = processorPipelineQueue.coroutineContext[ContinuationInterceptor] as CoroutineDispatcher

private class TalkbackPlayer {
    private var engine: AudioTrack? = null

    var isRunning = false
        private set

    fun start(format: MediaFormat) {
        val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channelMask = when (format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)) {
            1 -> AudioFormat.CHANNEL_OUT_MONO
            2 -> AudioFormat.CHANNEL_OUT_STEREO
            else -> {
                Log.i(TAG, "audio-unit: Failed to start talkback player engine: unsupported channel count")
                return
            }
        }
        val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelMask, AudioFormat.ENCODING_PCM_16BIT)
        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelMask)
                        .build()
                )
                .setBufferSizeInBytes(max(minBufferSize, 0) * 4)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            track.play()
            engine = track
            isRunning = true
        } catch (error: Exception) {
            Log.i(TAG, "audio-unit: Failed to start talkback player engine: $error")
        }
    }

    fun stop() {
        try {
            engine?.stop()
        } catch (error: Exception) {
            Log.d(TAG, "audio-unit: Failed to stop talkback player engine: $error")
        }
        engine?.release()
        engine = null
        isRunning = false
    }

    fun appendSampleBuffer(sampleBuffer: MediaSample) {
        val pcmBuffer = makePcmBuffer(sampleBuffer) ?: return
        engine?.write(pcmBuffer, 0, pcmBuffer.size, AudioTrack.WRITE_NON_BLOCKING)
    }

    private fun makePcmBuffer(sampleBuffer: MediaSample): ByteArray? {
        if (sampleBuffer.format == null) {
            return null
        }
        val frameCount = sampleBuffer.numSamples
        if (frameCount <= 0) {
            return null
        }
        return sampleBuffer.data
    }
}

data class AudioUnitAttachParams(
    val device: AVCaptureDevice?,
    val builtinDelay: Double,
    val bufferedAudio: UUID?
)

fun makeChannelMap(
    numberOfInputChannels: Int,
    numberOfOutputChannels: Int,
    outputToInputChannelsMap: Map<Int, Int>
): List<Int> {
    val channelMap = MutableList(numberOfOutputChannels) { -1 }
    for (inputIndex in 0 until minOf(numberOfInputChannels, numberOfOutputChannels)) {
        channelMap[inputIndex] = inputIndex
    }
    for (outputIndex in 0 until numberOfOutputChannels) {
        val inputIndex = outputToInputChannelsMap[outputIndex]
        if (inputIndex != null && inputIndex < numberOfInputChannels) {
            channelMap[outputIndex] = inputIndex
        }
    }
    return channelMap
}

fun calcAudioLevelPeakFloat32(samples: FloatArray, count: Int): Float {
    var peak = 0.0f
    for (index in 0 until count) {
        peak = max(peak, abs(samples[index]))
    }
    return peak
}

fun calcAudioLevelPeakInt16(samples: ShortArray, count: Int): Float {
    var peak = 0
    for (index in 0 until count) {
        peak = max(peak, abs(samples[index].toInt()))
    }
    return peak.toFloat() / (Short.MAX_VALUE.toFloat() + 1)
}

private class AudioMeasurement {
    private var currentPeak: Float = 0.0f
    private var windowStart: Double = Double.NaN
    private val windowDuration = 0.05
    private val windowInterval = 0.2

    fun input(sampleBuffer: MediaSample): Float? {
        val now = sampleBuffer.presentationTimeUs / 1_000_000.0
        if (windowStart.isNaN()) {
            windowStart = now
        }
        if (now < windowStart) {
            return null
        }
        sampleBuffer.foreachAudioSample(
            float32 = { samples, count ->
                currentPeak = max(currentPeak, calcAudioLevelPeakFloat32(samples, count))
            },
            int16 = { samples, count ->
                currentPeak = max(currentPeak, calcAudioLevelPeakInt16(samples, count))
            }
        )
        if (now < windowStart + windowDuration) {
            return null
        }
        windowStart = windowStart + windowInterval
        val audioLevel = peak()
        currentPeak = 0.0f
        return audioLevel
    }

    fun reset() {
        currentPeak = 0.0f
        windowStart = Double.NaN
    }

    private fun peak(): Float {
        if (currentPeak <= 0) {
            return defaultAudioLevel
        }
        return 20 * log10(currentPeak)
    }
}

class AudioUnit : BufferedAudioSampleBufferDelegate, AVCaptureAudioDataOutputSampleBufferDelegate {
    val encoder = AudioEncoder(lockQueue = processorPipelineDispatcher)
    var previewEncoder: AudioEncoder? = null
    private var input: AVCaptureDeviceInput? = null
    private var output: AVCaptureAudioDataOutput? = null

    @Volatile
    var muted = false

    @Volatile
    var gain: Float = 1.0f
    private var delay = 0.0
    var processor: Processor? = null
    private var selectedBufferedAudioId: UUID? = null
    private var bufferedAudios: MutableMap<UUID, BufferedAudio> = mutableMapOf()
    val session = AVCaptureSession()
    private var speechToTextEnabled = false
    private var bufferedBuiltinAudio: BufferedAudio? = null
    private var talkbackCameraId: UUID? = null
    private var talkbackPlayer: TalkbackPlayer? = null
    private var latestSampleBufferAppendTime: Long = 0L
    private var numberOfDiscardedSampleBuffers = 0
    private var measurement = AudioMeasurement()

    private var inputSourceFormat: MediaFormat? = null
        set(value) {
            if (value.isSameAudioFormat(field)) {
                return
            }
            field = value
            encoder.setInputSourceFormat(value)
            previewEncoder?.setInputSourceFormat(value)
        }

    fun startRunning() {
        session.startRunning()
    }

    fun stopRunning() {
        session.stopRunning()
    }

    fun attach(params: AudioUnitAttachParams) {
        processorPipelineQueue.launch {
            selectedBufferedAudioId = params.bufferedAudio
            bufferedBuiltinAudio = BufferedAudio(
                cameraId = UUID.randomUUID(),
                name = "builtin",
                latency = params.builtinDelay,
                manualOutput = true,
                driftTracker = null
            )
        }
        val device = params.device
        if (device != null) {
            attachDevice(device)
        }
        measurement.reset()
    }

    fun startEncoding(delegate: AudioEncoderDelegate) {
        encoder.delegate = delegate
        encoder.startRunning()
    }

    fun stopEncoding() {
        encoder.stopRunning()
        processorPipelineQueue.launch {
            inputSourceFormat = null
        }
    }

    fun startPreviewEncoding(delegate: AudioEncoderDelegate, settings: AudioEncoderSettings) {
        val encoder = AudioEncoder(lockQueue = processorPipelineDispatcher)
        encoder.setSettings(settings)
        encoder.delegate = delegate
        encoder.startRunning()
        processorPipelineQueue.launch {
            val inputSourceFormat = inputSourceFormat
            if (inputSourceFormat != null) {
                encoder.setInputSourceFormat(inputSourceFormat)
            }
            previewEncoder = encoder
        }
    }

    fun stopPreviewEncoding() {
        processorPipelineQueue.launch {
            previewEncoder?.stopRunning()
            previewEncoder = null
        }
    }

    fun setDelay(delay: Double) {
        processorPipelineQueue.launch {
            this@AudioUnit.delay = delay
        }
    }

    fun setSpeechToText(enabled: Boolean) {
        processorPipelineQueue.launch {
            speechToTextEnabled = enabled
        }
    }

    fun setTalkback(cameraId: UUID?) {
        processorPipelineQueue.launch {
            setTalkbackInternal(cameraId)
        }
    }

    fun addBufferedAudio(cameraId: UUID, name: String, latency: Double) {
        processorPipelineQueue.launch {
            addBufferedAudioInternal(cameraId, name, latency)
        }
    }

    fun removeBufferedAudio(cameraId: UUID) {
        processorPipelineQueue.launch {
            removeBufferedAudioInternal(cameraId)
        }
    }

    fun appendBufferedAudioSampleBuffer(cameraId: UUID, sampleBuffer: MediaSample) {
        processorPipelineQueue.launch {
            appendBufferedAudioSampleBufferInternal(cameraId, sampleBuffer)
        }
    }

    private fun attachDevice(device: AVCaptureDevice) {
        session.beginConfiguration()
        try {
            val input = input
            if (input != null && session.inputs.contains(input)) {
                session.removeInput(input)
            }
            val output = output
            if (output != null && session.outputs.contains(output)) {
                session.removeOutput(output)
            }
            val newInput = AVCaptureDeviceInput(device)
            this.input = newInput
            if (session.canAddInput(newInput)) {
                session.addInput(newInput)
            }
            val newOutput = AVCaptureAudioDataOutput()
            this.output = newOutput
            newOutput.setSampleBufferDelegate(this, processorPipelineQueue)
            if (session.canAddOutput(newOutput)) {
                session.addOutput(newOutput)
            }
            session.automaticallyConfiguresApplicationAudioSession = false
        } finally {
            session.commitConfiguration()
        }
    }

    private fun setTalkbackInternal(cameraId: UUID?) {
        talkbackCameraId = cameraId
        talkbackPlayer?.stop()
        talkbackPlayer = null
        if (talkbackCameraId != null) {
            talkbackPlayer = TalkbackPlayer()
        }
    }

    private fun addBufferedAudioInternal(cameraId: UUID, name: String, latency: Double) {
        val bufferedAudio = BufferedAudio(
            cameraId = cameraId,
            name = name,
            latency = latency,
            manualOutput = false,
            driftTracker = processor?.driftTracker(cameraId, name)
        )
        bufferedAudio.delegate = this
        bufferedAudios[cameraId] = bufferedAudio
    }

    private fun removeBufferedAudioInternal(cameraId: UUID) {
        bufferedAudios.remove(cameraId)?.stopOutput()
        processor?.removeDriftTracker(cameraId)
    }

    private fun appendBufferedAudioSampleBufferInternal(cameraId: UUID, sampleBuffer: MediaSample) {
        bufferedAudios[cameraId]?.appendSampleBuffer(sampleBuffer)
    }

    private fun appendNewSampleBuffer(
        processor: Processor,
        sampleBuffer: MediaSample,
        presentationTimeStamp: Long
    ) {
        val sampleBuffer = sampleBuffer.muted(muted)?.withGain(gain) ?: return
        val presentationTimeStamp = presentationTimeStamp + (delay * 1_000_000.0).toLong()
        if (presentationTimeStamp <= latestSampleBufferAppendTime) {
            numberOfDiscardedSampleBuffers += 1
            return
        }
        if (numberOfDiscardedSampleBuffers > 0) {
            Log.i(
                TAG,
                "audio-unit: Discarded $numberOfDiscardedSampleBuffers old buffers before " +
                    "${presentationTimeStamp / 1_000_000.0}"
            )
            numberOfDiscardedSampleBuffers = 0
        }
        latestSampleBufferAppendTime = presentationTimeStamp
        val audioLevel = measurement.input(sampleBuffer)
        if (audioLevel != null) {
            val numberOfAudioChannels = sampleBuffer.format?.numberOfAudioChannels() ?: 0
            updateAudioLevel(
                sampleBuffer = sampleBuffer,
                audioLevel = audioLevel,
                numberOfAudioChannels = numberOfAudioChannels
            )
        }
        if (speechToTextEnabled) {
            processor.delegate.streamAudio(sampleBuffer)
        }
        inputSourceFormat = sampleBuffer.format
        encoder.appendSampleBuffer(sampleBuffer, presentationTimeStamp)
        processor.recorder.appendAudio(sampleBuffer, presentationTimeStamp)
        previewEncoder?.appendSampleBuffer(sampleBuffer, presentationTimeStamp)
    }

    private fun appendBufferedBuiltinAudio(
        sampleBuffer: MediaSample,
        presentationTimeStamp: Long
    ): BufferedAudio? {
        val bufferedBuiltinAudio = bufferedBuiltinAudio ?: return null
        if (bufferedBuiltinAudio.latency <= 0) {
            return null
        }
        val sampleBufferCopy = if (bufferedBuiltinAudio.numberOfBuffers() > 4) {
            sampleBuffer.deepCopyAudioSampleBuffer() ?: sampleBuffer
        } else {
            sampleBuffer
        }
        val presentationTimeStamp = presentationTimeStamp + (bufferedBuiltinAudio.latency * 1_000_000.0).toLong()
        val replacedSampleBuffer = sampleBufferCopy.replacePresentationTimeStamp(presentationTimeStamp)
        bufferedBuiltinAudio.appendSampleBuffer(replacedSampleBuffer)
        return bufferedBuiltinAudio
    }

    private fun updateAudioLevel(
        sampleBuffer: MediaSample,
        audioLevel: Float,
        numberOfAudioChannels: Int
    ) {
        val format = sampleBuffer.format
        val sampleRate = if (format != null && format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
            format.getInteger(MediaFormat.KEY_SAMPLE_RATE).toDouble()
        } else {
            0.0
        }
        processor?.delegate?.streamAudioLevel(
            audioLevel = audioLevel,
            numberOfAudioChannels = numberOfAudioChannels,
            sampleRate = sampleRate
        )
    }

    private fun appendTalkback(sampleBuffer: MediaSample) {
        val talkbackPlayer = talkbackPlayer ?: return
        if (!talkbackPlayer.isRunning) {
            val format = audioFormat(sampleBuffer) ?: return
            talkbackPlayer.start(format)
        }
        talkbackPlayer.appendSampleBuffer(sampleBuffer)
    }

    override fun captureOutput(output: AVCaptureOutput, didOutput: MediaSample, from: AVCaptureConnection?) {
        val processor = processor ?: return
        val presentationTimeStamp = syncTimeToHost(processor, didOutput)
        var sampleBuffer = didOutput
        val bufferedAudio = appendBufferedBuiltinAudio(sampleBuffer, presentationTimeStamp)
        if (bufferedAudio != null) {
            sampleBuffer = bufferedAudio.getSampleBuffer(presentationTimeStamp / 1_000_000.0) ?: sampleBuffer
        }
        if (selectedBufferedAudioId != null) {
            return
        }
        appendNewSampleBuffer(processor, sampleBuffer, presentationTimeStamp)
    }

    override fun didOutputBufferedSampleBuffer(cameraId: UUID, sampleBuffer: MediaSample) {
        if (cameraId == talkbackCameraId) {
            appendTalkback(sampleBuffer)
        }
        val processor = processor
        if (selectedBufferedAudioId != cameraId || processor == null) {
            return
        }
        appendNewSampleBuffer(processor, sampleBuffer, sampleBuffer.presentationTimeUs)
    }
}

fun audioFormat(sampleBuffer: MediaSample): MediaFormat? {
    val format = sampleBuffer.format ?: return null
    if (!format.containsKey(MediaFormat.KEY_SAMPLE_RATE) || !format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
        return null
    }
    return format
}

private fun syncTimeToHost(processor: Processor, sampleBuffer: MediaSample): Long {
    return processor.audio.session.synchronizationClock?.convertTime(sampleBuffer.presentationTimeUs)
        ?: sampleBuffer.presentationTimeUs
}

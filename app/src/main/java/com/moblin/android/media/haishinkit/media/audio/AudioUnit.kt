package com.moblin.android.media.haishinkit.media.audio

import android.hardware.camera2.CameraDevice
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaFormat
import android.util.Log
import com.moblin.android.common.various.defaultAudioLevel
import com.moblin.android.media.MediaSample
import com.moblin.android.media.haishinkit.codec.audio.AudioEncoder
import com.moblin.android.media.haishinkit.codec.audio.AudioEncoderDelegate
import com.moblin.android.media.haishinkit.codec.audio.AudioEncoderSettings
import com.moblin.android.media.haishinkit.media.Processor
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import kotlin.coroutines.ContinuationInterceptor
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch

private const val TAG = "AudioUnit"

private val processorPipelineDispatcher: CoroutineDispatcher
    get() = processorPipelineQueue.coroutineContext[ContinuationInterceptor] as CoroutineDispatcher

private class TalkbackPlayer {
    private var engine: AudioTrack? = null

    var isRunning = false
        private set

    fun start(format: AudioFormat) {
        if (engine != null) {
            return
        }
        val minBufferSize = AudioTrack.getMinBufferSize(
            format.sampleRate,
            format.channelMask,
            format.encoding
        )
        if (minBufferSize <= 0) {
            Log.i("TalkbackPlayer", "audio-unit: Failed to start talkback player engine: invalid buffer size")
            return
        }
        val track: AudioTrack
        try {
            track = AudioTrack.Builder()
                .setAudioFormat(format)
                .setBufferSizeInBytes(minBufferSize * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (error: Exception) {
            Log.i("TalkbackPlayer", "audio-unit: Failed to start talkback player engine: $error")
            return
        }
        track.play()
        engine = track
        isRunning = true
    }

    fun stop() {
        engine?.stop()
        engine?.release()
        engine = null
    }

    fun appendSampleBuffer(sampleBuffer: MediaSample) {
        val track = engine ?: return
        val pcmBuffer = makePcmBuffer(sampleBuffer) ?: return
        track.write(pcmBuffer, 0, pcmBuffer.size)
    }

    private fun makePcmBuffer(sampleBuffer: MediaSample): ShortArray? {
        val data = sampleBuffer.data
        if (data.size < 2) {
            return null
        }
        return pcm16Samples(data)
    }
}

data class AudioUnitAttachParams(
    val device: CameraDevice?,
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
        val magnitude = abs(samples[index].toInt())
        if (magnitude > peak) {
            peak = magnitude
        }
    }
    return peak.toFloat() / (Short.MAX_VALUE.toFloat() + 1)
}

private class AudioMeasurement {
    private var currentPeak: Float = 0.0f
    private var windowStart: Double = Double.NaN
    private val windowDuration = 0.05
    private val windowInterval = 0.2

    fun input(sampleBuffer: MediaSample): Float? {
        val now = sampleBuffer.presentationTimeUs.toDouble() / 1_000_000.0
        if (windowStart.isNaN()) {
            windowStart = now
        }
        if (now < windowStart) {
            return null
        }
        val samples = pcm16Samples(sampleBuffer.data)
        if (samples.isNotEmpty()) {
            currentPeak = max(currentPeak, calcAudioLevelPeakInt16(samples, samples.size))
        }
        if (now < windowStart + windowDuration) {
            return null
        }
        windowStart += windowInterval
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
        return 20.0f * log10(currentPeak)
    }
}

class AudioUnit : BufferedAudioSampleBufferDelegate {
    val encoder = AudioEncoder(processorPipelineDispatcher)
    var previewEncoder: AudioEncoder? = null
    private var input: AudioRecord? = null
    private var output: Any? = null
    var muted = false
    var gain: Float = 1.0f
    private var delay = 0.0
    var processor: Processor? = null
    private var selectedBufferedAudioId: UUID? = null
    private var bufferedAudios: MutableMap<UUID, BufferedAudio> = mutableMapOf()
    var session: AudioRecord? = null
    private var speechToTextEnabled = false
    private var bufferedBuiltinAudio: BufferedAudio? = null
    private var talkbackCameraId: UUID? = null
    private var talkbackPlayer: TalkbackPlayer? = null
    private var latestSampleBufferAppendTime: Long = 0L
    private var numberOfDiscardedSampleBuffers = 0
    private var measurement = AudioMeasurement()

    private var inputSourceFormat: MediaFormat? = null
        set(value) {
            if (field == value) {
                return
            }
            field = value
            encoder.setInputSourceFormat(value)
            previewEncoder?.setInputSourceFormat(value)
        }

    fun startRunning() {
        session?.startRecording()
    }

    fun stopRunning() {
        session?.stop()
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
        params.device?.let {
            attachDevice(it)
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
        val encoder = AudioEncoder(processorPipelineDispatcher)
        encoder.setSettings(settings)
        encoder.delegate = delegate
        encoder.startRunning()
        processorPipelineQueue.launch {
            inputSourceFormat?.let { encoder.setInputSourceFormat(it) }
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

    private fun attachDevice(device: CameraDevice) {
        session?.stop()
        session?.release()
        session = null
        input = null
        output = null
        Unit
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
        presentationTimeUs: Long
    ) {
        val sampleBuffer = sampleBuffer.muted(muted)?.withGain(gain) ?: return
        val presentationTimeUs = presentationTimeUs + (delay * 1_000_000.0).toLong()
        if (presentationTimeUs <= latestSampleBufferAppendTime) {
            numberOfDiscardedSampleBuffers += 1
            return
        }
        if (numberOfDiscardedSampleBuffers > 0) {
            Log.i(
                TAG,
                "audio-unit: Discarded $numberOfDiscardedSampleBuffers old buffers before " +
                    (presentationTimeUs / 1_000_000.0)
            )
            numberOfDiscardedSampleBuffers = 0
        }
        latestSampleBufferAppendTime = presentationTimeUs
        val audioLevel = measurement.input(sampleBuffer)
        if (audioLevel != null) {
            val numberOfAudioChannels = sampleBuffer.numberOfAudioChannels()
            updateAudioLevel(
                sampleBuffer = sampleBuffer,
                audioLevel = audioLevel,
                numberOfAudioChannels = numberOfAudioChannels
            )
        }
        if (speechToTextEnabled) {
            processor.delegate?.streamAudio(sampleBuffer)
        }
        inputSourceFormat = sampleBuffer.format
        encoder.appendSampleBuffer(sampleBuffer, presentationTimeUs)
        processor.recorder.appendAudio(sampleBuffer, presentationTimeUs)
        previewEncoder?.appendSampleBuffer(sampleBuffer, presentationTimeUs)
    }

    private fun appendBufferedBuiltinAudio(
        sampleBuffer: MediaSample,
        presentationTimeUs: Long
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
        val presentationTimeUs = presentationTimeUs + (bufferedBuiltinAudio.latency * 1_000_000.0).toLong()
        val replacedSampleBuffer = sampleBufferCopy.replacePresentationTimeStamp(presentationTimeUs) ?: return null
        bufferedBuiltinAudio.appendSampleBuffer(replacedSampleBuffer)
        return bufferedBuiltinAudio
    }

    private fun updateAudioLevel(
        sampleBuffer: MediaSample,
        audioLevel: Float,
        numberOfAudioChannels: Int
    ) {
        val sampleRate = sampleBuffer.sampleRate()
        processor?.delegate?.streamAudioLevel(
            audioLevel = audioLevel,
            numberOfAudioChannels = numberOfAudioChannels,
            sampleRate = sampleRate.toDouble()
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

    fun captureOutput(output: Any?, didOutput: MediaSample, from: Any?) {
        val processor = this.processor ?: return
        val presentationTimeUs = syncTimeToHost(processor, didOutput)
        var sampleBuffer = didOutput
        val bufferedAudio = appendBufferedBuiltinAudio(sampleBuffer, presentationTimeUs)
        if (bufferedAudio != null) {
            sampleBuffer = bufferedAudio.getSampleBuffer(presentationTimeUs / 1_000_000.0) ?: sampleBuffer
        }
        if (selectedBufferedAudioId != null) {
            return
        }
        appendNewSampleBuffer(processor, sampleBuffer, presentationTimeUs)
    }

    override fun didOutputBufferedSampleBuffer(cameraId: UUID, sampleBuffer: MediaSample) {
        if (cameraId == talkbackCameraId) {
            appendTalkback(sampleBuffer)
        }
        val processor = this.processor
        if (selectedBufferedAudioId != cameraId || processor == null) {
            return
        }
        appendNewSampleBuffer(processor, sampleBuffer, sampleBuffer.presentationTimeUs)
    }
}

fun audioFormat(sampleBuffer: MediaSample): AudioFormat? {
    val mediaFormat = sampleBuffer.format ?: return null
    if (!mediaFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
        return null
    }
    if (!mediaFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
        return null
    }
    val sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
    val channels = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
    val channelMask = when (channels) {
        1 -> AudioFormat.CHANNEL_OUT_MONO
        2 -> AudioFormat.CHANNEL_OUT_STEREO
        else -> return null
    }
    return AudioFormat.Builder()
        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
        .setSampleRate(sampleRate)
        .setChannelMask(channelMask)
        .build()
}

private fun syncTimeToHost(processor: Processor, sampleBuffer: MediaSample): Long {
    return 0L
}

private fun pcm16Samples(data: ByteArray): ShortArray {
    val count = data.size / 2
    val samples = ShortArray(count)
    ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(samples)
    return samples
}

private fun MediaSample.muted(muted: Boolean): MediaSample? {
    if (!muted) {
        return this
    }
    if (data.size < 2) {
        return this
    }
    return MediaSample(
        data = ByteArray(data.size),
        presentationTimeUs = presentationTimeUs,
        isKeyFrame = isKeyFrame,
        format = format
    )
}

private fun MediaSample.withGain(gain: Float): MediaSample? {
    if (gain == 1.0f) {
        return this
    }
    val samples = pcm16Samples(data)
    if (samples.isEmpty()) {
        return this
    }
    val buffer = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
    for (sample in samples) {
        val value = (sample.toFloat() * gain).roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
        buffer.putShort(value.toShort())
    }
    return MediaSample(
        data = buffer.array(),
        presentationTimeUs = presentationTimeUs,
        isKeyFrame = isKeyFrame,
        format = format
    )
}

private fun MediaSample.deepCopyAudioSampleBuffer(): MediaSample? {
    return MediaSample(
        data = data.copyOf(),
        presentationTimeUs = presentationTimeUs,
        isKeyFrame = isKeyFrame,
        format = format
    )
}

private fun MediaSample.replacePresentationTimeStamp(presentationTimeUs: Long): MediaSample? {
    return MediaSample(
        data = data,
        presentationTimeUs = presentationTimeUs,
        isKeyFrame = isKeyFrame,
        format = format
    )
}

private fun MediaSample.numberOfAudioChannels(): Int {
    val mediaFormat = format ?: return 0
    if (!mediaFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
        return 0
    }
    return mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
}

private fun MediaSample.sampleRate(): Int {
    val mediaFormat = format ?: return 0
    if (!mediaFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
        return 0
    }
    return mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
}

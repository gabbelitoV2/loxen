package com.moblin.android.media.haishinkit.codec.audio

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import com.moblin.android.media.MediaSample
import com.moblin.android.media.haishinkit.media.audio.makeChannelMap
import com.moblin.android.media.haishinkit.util.Atomic
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val TAG = "AudioEncoder"

private val converterBitrates = ConcurrentHashMap<MediaCodec, Int>()

interface AudioEncoderDelegate {
    fun audioEncoderOutputFormat(format: MediaFormat)

    fun audioEncoderOutputBuffer(buffer: MediaSample, presentationTimeStamp: Long)
}

class AudioEncoder(private val lockQueue: CoroutineDispatcher) {
    var delegate: AudioEncoderDelegate? = null
    private var isRunning = false
    private val lockQueueScope = CoroutineScope(lockQueue)
    private var ringBuffer: AudioEncoderRingBuffer? = null
    private var audioConverter: MediaCodec? = null
    private var settings = AudioEncoderSettings()
    private val bitrate: Atomic<Int> = Atomic(128_000)
    private val sampleRate: Atomic<Double?> = Atomic(null)
    private var inSourceFormat: MediaFormat? = null

    fun startRunning() {
        lockQueueScope.launch {
            startRunningInternal()
        }
    }

    fun stopRunning() {
        lockQueueScope.launch {
            stopRunningInternal()
        }
    }

    fun appendSampleBuffer(sampleBuffer: MediaSample, presentationTimeStamp: Long) {
        val audioConverter = audioConverter ?: return
        val ringBuffer = ringBuffer ?: return
        if (!isRunning) {
            return
        }
        ringBuffer.setWorkingSampleBuffer(sampleBuffer.data, presentationTimeStamp)
        while (true) {
            val (outputBuffer, outputPresentationTimeStamp) = ringBuffer.createOutputBuffer() ?: break
            convertBuffer(audioConverter, outputBuffer, outputPresentationTimeStamp)
        }
    }

    fun setSettings(settings: AudioEncoderSettings) {
        lockQueueScope.launch {
            this@AudioEncoder.settings = settings
            audioConverter?.setBitrate(settings.bitrate)
            bitrate.value = settings.bitrate
        }
    }

    fun setInputSourceFormat(newInSourceFormat: MediaFormat?) {
        if (newInSourceFormat == null || newInSourceFormat == inSourceFormat) {
            return
        }
        ringBuffer = AudioEncoderRingBuffer(newInSourceFormat, numSamplesPerBuffer = samplesPerBuffer())
        audioConverter = makeAudioConverter(newInSourceFormat)
        val newSampleRate = newInSourceFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE).toDouble()
        sampleRate.value = newSampleRate
    }

    fun getBitrate(): Int {
        return bitrate.value
    }

    fun getSampleRate(): Double? {
        return sampleRate.value
    }

    companion object {
        fun makeAudioFormat(basicDescription: MediaFormat): MediaFormat? {
            val sampleRate = basicDescription.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val numberOfChannels = basicDescription.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            val mime = basicDescription.getString(MediaFormat.KEY_MIME) ?: MediaFormat.MIMETYPE_AUDIO_RAW
            val format = MediaFormat.createAudioFormat(mime, sampleRate, numberOfChannels)
            makeChannelLayout(numberOfChannels)?.let {
                format.setInteger(MediaFormat.KEY_CHANNEL_MASK, it)
            }
            return format
        }

        private fun makeChannelLayout(numberOfChannels: Int): Int? {
            if (numberOfChannels <= 2) {
                return null
            }
            return (1 shl numberOfChannels) - 1
        }
    }

    private fun samplesPerBuffer(): Int {
        return when (settings.format) {
            AudioEncoderSettingsFormat.aac -> 1024
            AudioEncoderSettingsFormat.opus -> 960
        }
    }

    private fun startRunningInternal() {
        audioConverter?.let {
            val format = runCatching { it.outputFormat }.getOrNull()
            if (format != null) {
                delegate?.audioEncoderOutputFormat(format)
            }
        }
        isRunning = true
    }

    private fun stopRunningInternal() {
        inSourceFormat = null
        audioConverter?.let {
            runCatching { it.stop() }
            runCatching { it.release() }
            converterBitrates.remove(it)
        }
        audioConverter = null
        ringBuffer = null
        isRunning = false
    }

    private fun convertBuffer(
        audioConverter: MediaCodec,
        inputBuffer: ByteArray,
        presentationTimeStamp: Long
    ) {
        var error: String? = null
        val inputBufferIndex = audioConverter.dequeueInputBuffer(0L)
        if (inputBufferIndex < 0) {
            error = "no input buffer available"
        } else {
            val codecInputBuffer = audioConverter.getInputBuffer(inputBufferIndex)
            if (codecInputBuffer == null) {
                error = "no input buffer available"
            } else {
                codecInputBuffer.clear()
                val size = minOf(inputBuffer.size, codecInputBuffer.remaining())
                codecInputBuffer.put(inputBuffer, 0, size)
                audioConverter.queueInputBuffer(inputBufferIndex, 0, size, presentationTimeStamp, 0)
            }
        }
        val bufferInfo = MediaCodec.BufferInfo()
        val outputBufferIndex = audioConverter.dequeueOutputBuffer(bufferInfo, 0L)
        if (outputBufferIndex >= 0) {
            val codecOutputBuffer = audioConverter.getOutputBuffer(outputBufferIndex)
            val data = ByteArray(bufferInfo.size)
            if (codecOutputBuffer != null && bufferInfo.size > 0) {
                codecOutputBuffer.position(bufferInfo.offset)
                codecOutputBuffer.get(data, 0, minOf(bufferInfo.size, codecOutputBuffer.remaining()))
            }
            audioConverter.releaseOutputBuffer(outputBufferIndex, false)
            delegate?.audioEncoderOutputBuffer(
                MediaSample(
                    data = data,
                    presentationTimeUs = bufferInfo.presentationTimeUs,
                    isKeyFrame = bufferInfo.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME != 0,
                    format = runCatching { audioConverter.outputFormat }.getOrNull()
                ),
                presentationTimeStamp
            )
        } else if (outputBufferIndex != MediaCodec.INFO_TRY_AGAIN_LATER) {
            error = "output buffer $outputBufferIndex"
        }
        if (error != null) {
            Log.i(TAG, "audio-encoder: Failed to convert $error")
        }
    }

    private fun makeAudioConverter(inSourceFormat: MediaFormat): MediaCodec? {
        val inputFormat = makeAudioFormat(inSourceFormat)
        val outputFormat = settings.format.makeAudioFormat(inSourceFormat)
        if (inputFormat == null || outputFormat == null) {
            return null
        }
        Log.d(TAG, "audio-encoder: inputFormat: $inputFormat")
        Log.d(TAG, "audio-encoder: outputFormat: $outputFormat")
        val mime = outputFormat.getString(MediaFormat.KEY_MIME)
        val converter = if (mime == null) {
            null
        } else {
            runCatching { MediaCodec.createEncoderByType(mime) }.getOrNull()
        }
        if (converter == null) {
            Log.i(TAG, "audio-encoder: Failed to create from $inputFormat to $outputFormat")
            return null
        }
        val channelsMap = makeChannelMap(
            numberOfInputChannels = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT),
            numberOfOutputChannels = outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT),
            outputToInputChannelsMap = settings.channelsMap
        )
        if (channelsMap.isNotEmpty()) {
            var channelMask = 0
            for (channel in channelsMap) {
                channelMask = channelMask or (1 shl channel)
            }
            outputFormat.setInteger(MediaFormat.KEY_CHANNEL_MASK, channelMask)
        }
        outputFormat.setInteger(MediaFormat.KEY_BIT_RATE, settings.bitrate)
        val configured = runCatching {
            converter.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            converter.start()
            true
        }.getOrDefault(false)
        if (!configured) {
            runCatching { converter.release() }
            Log.i(TAG, "audio-encoder: Failed to create from $inputFormat to $outputFormat")
            return null
        }
        converterBitrates[converter] = settings.bitrate
        converter.setBitrate(settings.bitrate)
        delegate?.audioEncoderOutputFormat(outputFormat)
        return converter
    }
}

private val MediaCodec.bitRate: Int
    get() = converterBitrates[this] ?: 0

private val MediaCodec.applicableEncodeBitRates: IntRange?
    get() {
        val mime = runCatching { outputFormat.getString(MediaFormat.KEY_MIME) }.getOrNull() ?: return null
        val capabilities = runCatching { codecInfo.getCapabilitiesForType(mime) }.getOrNull() ?: return null
        val audioCapabilities = capabilities.audioCapabilities ?: return null
        val bitrateRange = audioCapabilities.bitrateRange ?: return null
        return bitrateRange.lower..bitrateRange.upper
    }

fun MediaCodec.setBitrate(bitrate: Int) {
    if (bitrate == bitRate) {
        return
    }
    val bitrates = applicableEncodeBitRates ?: return
    val minBitrate = bitrates.first
    val maxBitrate = bitrates.last
    val clamped = bitrate.coerceIn(minBitrate, maxBitrate)
    converterBitrates[this] = clamped
    Log.d(TAG, "audio-encoder: $clamped, maximum: $maxBitrate")
}

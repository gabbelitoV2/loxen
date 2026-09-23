package com.moblin.android.media.haishinkit.codec.audio

import android.media.MediaFormat
import android.util.Log
import com.moblin.android.media.MediaSample
import com.moblin.android.media.haishinkit.media.audio.makeChannelMap
import com.moblin.android.media.haishinkit.util.Atomic
import com.moblin.android.platform.audio.isSameAudioFormat
import com.moblin.android.platform.avfoundation.AVAudioConverter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val TAG = "AudioEncoder"

interface AudioEncoderDelegate {
    fun audioEncoderOutputFormat(format: MediaFormat)

    fun audioEncoderOutputBuffer(buffer: MediaSample, presentationTimeStamp: Long)
}

class AudioEncoder(private val lockQueue: CoroutineDispatcher) {
    var delegate: AudioEncoderDelegate? = null
    private var isRunning = false
    private val lockQueueScope = CoroutineScope(lockQueue)
    private var ringBuffer: AudioEncoderRingBuffer? = null
    private var audioConverter: AVAudioConverter? = null
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
        val audioConverter = audioConverter
        val ringBuffer = ringBuffer
        if (!isRunning || audioConverter == null || ringBuffer == null) {
            return
        }
        ringBuffer.setWorkingSampleBuffer(sampleBuffer.data, presentationTimeStamp)
        while (true) {
            val (outputBuffer, outputPresentationTimeStamp) = ringBuffer.createOutputBuffer() ?: break
            convertBuffer(audioConverter, outputBuffer, outputPresentationTimeStamp)
        }
    }

    fun setSettings(settings: AudioEncoderSettings) {
        val newSettings = settings.copy()
        lockQueueScope.launch {
            this@AudioEncoder.settings = newSettings
            audioConverter?.setBitrate(newSettings.bitrate)
            bitrate.mutate { it.value = newSettings.bitrate }
        }
    }

    fun setInputSourceFormat(newInSourceFormat: MediaFormat?) {
        if (newInSourceFormat == null || newInSourceFormat.isSameAudioFormat(inSourceFormat)) {
            return
        }
        ringBuffer = AudioEncoderRingBuffer(newInSourceFormat, numSamplesPerBuffer = samplesPerBuffer())
        audioConverter?.release()
        audioConverter = makeAudioConverter(newInSourceFormat)
        val newSampleRate = newInSourceFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE).toDouble()
        sampleRate.mutate { it.value = newSampleRate }
    }

    fun getBitrate(): Int {
        return bitrate.value
    }

    fun getSampleRate(): Double? {
        return sampleRate.value
    }

    companion object {
        fun makeAudioFormat(basicDescription: MediaFormat): MediaFormat? {
            if (!basicDescription.containsKey(MediaFormat.KEY_SAMPLE_RATE) ||
                !basicDescription.containsKey(MediaFormat.KEY_CHANNEL_COUNT)
            ) {
                return null
            }
            val sampleRate = basicDescription.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val numberOfChannels = basicDescription.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            val mime = basicDescription.getString(MediaFormat.KEY_MIME) ?: MediaFormat.MIMETYPE_AUDIO_RAW
            val format = MediaFormat.createAudioFormat(mime, sampleRate, numberOfChannels)
            if (basicDescription.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                format.setInteger(
                    MediaFormat.KEY_PCM_ENCODING,
                    basicDescription.getInteger(MediaFormat.KEY_PCM_ENCODING),
                )
            }
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
            AudioEncoderSettings.Format.aac -> 1024
            AudioEncoderSettings.Format.opus -> 960
        }
    }

    private fun startRunningInternal() {
        audioConverter?.let {
            delegate?.audioEncoderOutputFormat(it.outputFormat)
        }
        isRunning = true
    }

    private fun stopRunningInternal() {
        inSourceFormat = null
        audioConverter?.release()
        audioConverter = null
        ringBuffer = null
        isRunning = false
    }

    private fun convertBuffer(
        audioConverter: AVAudioConverter,
        inputBuffer: PcmBuffer,
        presentationTimeStamp: Long,
    ) {
        val outputBuffer = settings.format.makeAudioBuffer(audioConverter.outputFormat)
        val error = audioConverter.convert(to = outputBuffer) {
            inputBuffer.data
        }
        if (error != null) {
            Log.i(TAG, "audio-encoder: Failed to convert $error")
        } else {
            delegate?.audioEncoderOutputBuffer(outputBuffer, presentationTimeStamp)
        }
    }

    private fun makeAudioConverter(inSourceFormat: MediaFormat): AVAudioConverter? {
        val inputFormat = makeAudioFormat(inSourceFormat) ?: return null
        val outputFormat = settings.format.makeAudioFormat(inSourceFormat) ?: return null
        Log.d(TAG, "audio-encoder: inputFormat: $inputFormat")
        Log.d(TAG, "audio-encoder: outputFormat: $outputFormat")
        val converter = AVAudioConverter.create(from = inputFormat, to = outputFormat)
        if (converter == null) {
            Log.i(TAG, "audio-encoder: Failed to create from $inputFormat to $outputFormat")
            return null
        }
        converter.channelMap = makeChannelMap(
            numberOfInputChannels = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT),
            numberOfOutputChannels = outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT),
            outputToInputChannelsMap = settings.channelsMap,
        )
        converter.setBitrate(settings.bitrate)
        delegate?.audioEncoderOutputFormat(outputFormat)
        return converter
    }
}

fun AVAudioConverter.setBitrate(bitrate: Int) {
    if (bitrate == bitRate) {
        return
    }
    val bitrates = applicableEncodeBitRates ?: return
    val minBitrate = bitrates.minOrNull() ?: bitrate
    val maxBitrate = bitrates.maxOrNull() ?: bitrate
    bitRate = bitrate.coerceIn(minBitrate, maxBitrate)
    Log.d(TAG, "audio-encoder: $bitRate, maximum: $maxBitrate")
}

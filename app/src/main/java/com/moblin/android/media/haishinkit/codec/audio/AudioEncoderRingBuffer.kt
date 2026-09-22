package com.moblin.android.media.haishinkit.codec.audio

import android.media.AudioFormat
import android.media.MediaFormat

enum class PcmCommonFormat {
    INT16,
    INT32,
    FLOAT32,
    OTHER,
}

class PcmAudioFormat(
    val sampleRate: Int,
    val channels: Int,
    val isInterleaved: Boolean,
    val commonFormat: PcmCommonFormat,
) {
    val bytesPerSample: Int
        get() = when (commonFormat) {
            PcmCommonFormat.INT16 -> 2
            PcmCommonFormat.INT32 -> 4
            PcmCommonFormat.FLOAT32 -> 4
            PcmCommonFormat.OTHER -> 0
        }

    val bytesPerFrame: Int
        get() = bytesPerSample * channels
}

class PcmBuffer(
    var data: ByteArray,
    var frameLength: Int,
)

class AudioEncoderRingBuffer private constructor(
    private val numSamplesPerBuffer: Int,
    private var format: PcmAudioFormat,
    private var outputBuffer: PcmBuffer,
    private var workingBuffer: PcmBuffer,
) {
    private var latestPresentationTimeStamp: Long = INVALID_PRESENTATION_TIME_STAMP
    private var workingIndex = 0
    private var outputIndex = 0
    private var workingBufferPresentationTimeStamp: Long = 0L

    fun setWorkingSampleBuffer(audioBufferList: ByteArray, presentationTimeStamp: Long) {
        workingBufferPresentationTimeStamp = presentationTimeStamp
        workingIndex = 0
        val bytesPerFrame = format.bytesPerFrame
        if (bytesPerFrame > 0) {
            workingBuffer = PcmBuffer(audioBufferList, audioBufferList.size / bytesPerFrame)
        }
    }

    fun createOutputBuffer(): Pair<PcmBuffer, Long>? {
        if (latestPresentationTimeStamp == INVALID_PRESENTATION_TIME_STAMP) {
            val offsetTimeStamp = workingIndex * 1_000_000L / format.sampleRate
            latestPresentationTimeStamp = workingBufferPresentationTimeStamp + offsetTimeStamp
        }
        val numSamples = minOf(
            numSamplesPerBuffer - outputIndex,
            workingBuffer.frameLength - workingIndex,
        )
        val bytesPerSample = format.bytesPerSample
        if (format.isInterleaved) {
            val channelCount = format.channels
            val sourceOffset = workingIndex * channelCount * bytesPerSample
            val destinationOffset = outputIndex * channelCount * bytesPerSample
            when (format.commonFormat) {
                PcmCommonFormat.INT16 -> System.arraycopy(
                    workingBuffer.data,
                    sourceOffset,
                    outputBuffer.data,
                    destinationOffset,
                    numSamples * 2 * channelCount,
                )
                PcmCommonFormat.INT32 -> System.arraycopy(
                    workingBuffer.data,
                    sourceOffset,
                    outputBuffer.data,
                    destinationOffset,
                    numSamples * 4 * channelCount,
                )
                PcmCommonFormat.FLOAT32 -> System.arraycopy(
                    workingBuffer.data,
                    sourceOffset,
                    outputBuffer.data,
                    destinationOffset,
                    numSamples * 4 * channelCount,
                )
                PcmCommonFormat.OTHER -> {}
            }
        } else {
            for (i in 0 until format.channels) {
                val sourceOffset = (i * workingBuffer.frameLength + workingIndex) * bytesPerSample
                val destinationOffset = (i * outputBuffer.frameLength + outputIndex) * bytesPerSample
                when (format.commonFormat) {
                    PcmCommonFormat.INT16 -> System.arraycopy(
                        workingBuffer.data,
                        sourceOffset,
                        outputBuffer.data,
                        destinationOffset,
                        numSamples * 2,
                    )
                    PcmCommonFormat.INT32 -> System.arraycopy(
                        workingBuffer.data,
                        sourceOffset,
                        outputBuffer.data,
                        destinationOffset,
                        numSamples * 4,
                    )
                    PcmCommonFormat.FLOAT32 -> System.arraycopy(
                        workingBuffer.data,
                        sourceOffset,
                        outputBuffer.data,
                        destinationOffset,
                        numSamples * 4,
                    )
                    PcmCommonFormat.OTHER -> {}
                }
            }
        }
        workingIndex += numSamples
        outputIndex += numSamples
        if (numSamplesPerBuffer != outputIndex) {
            return null
        }
        val result = outputBuffer to latestPresentationTimeStamp
        latestPresentationTimeStamp = INVALID_PRESENTATION_TIME_STAMP
        outputIndex = 0
        return result
    }

    companion object {
        private const val INVALID_PRESENTATION_TIME_STAMP = Long.MIN_VALUE

        operator fun invoke(
            inputBasicDescription: MediaFormat,
            numSamplesPerBuffer: Int,
        ): AudioEncoderRingBuffer? {
            val pcmEncoding = if (inputBasicDescription.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                inputBasicDescription.getInteger(MediaFormat.KEY_PCM_ENCODING)
            } else {
                AudioFormat.ENCODING_PCM_16BIT
            }
            if (pcmEncoding != AudioFormat.ENCODING_PCM_16BIT) {
                return null
            }
            val format = AudioEncoder.makeAudioFormat(inputBasicDescription) ?: return null
            val bytesPerFrame = format.bytesPerFrame
            if (bytesPerFrame <= 0 || numSamplesPerBuffer <= 0) {
                return null
            }
            val outputBuffer = PcmBuffer(ByteArray(numSamplesPerBuffer * bytesPerFrame), numSamplesPerBuffer)
            val workingBuffer = PcmBuffer(ByteArray(numSamplesPerBuffer * bytesPerFrame), 0)
            return AudioEncoderRingBuffer(numSamplesPerBuffer, format, outputBuffer, workingBuffer)
        }
    }
}

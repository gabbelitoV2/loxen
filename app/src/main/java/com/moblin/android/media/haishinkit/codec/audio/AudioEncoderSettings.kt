package com.moblin.android.media.haishinkit.codec.audio

import android.media.MediaCodecInfo
import android.media.MediaFormat
import com.moblin.android.platform.audio.hasAudioEncoder
import com.moblin.android.platform.audio.isOpusSampleRateSupported
import com.moblin.android.platform.avfoundation.AVAudioCompressedBuffer

data class AudioEncoderSettings(
    var bitrate: Int = 64 * 1000,
    var channelsMap: MutableMap<Int, Int> = mutableMapOf(0 to 0, 1 to 1),
    var format: Format = Format.aac,
) {
    companion object {
        private const val maximumNumberOfChannels = 2
    }

    enum class Format {
        aac,
        opus;

        fun makeAudioBuffer(format: MediaFormat): AVAudioCompressedBuffer {
            return AVAudioCompressedBuffer(
                format = format,
                packetCapacity = 1,
                maximumPacketSize = 1024 * format.getInteger(MediaFormat.KEY_CHANNEL_COUNT),
            )
        }

        fun makeAudioFormat(inSourceFormat: MediaFormat): MediaFormat? {
            val channels = minOf(
                inSourceFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT),
                maximumNumberOfChannels,
            )
            val sampleRate = inSourceFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val streamDescription = when (this) {
                aac -> {
                    val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channels)
                    format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                    format
                }
                opus -> {
                    if (!isOpusSampleRateSupported(sampleRate)) {
                        return null
                    }
                    MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_OPUS, sampleRate, channels)
                }
            }
            if (!hasAudioEncoder(streamDescription)) {
                return null
            }
            return streamDescription
        }
    }
}

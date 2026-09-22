package com.moblin.android.media.haishinkit.codec.audio

import android.media.MediaFormat

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

        fun makeAudioBuffer(format: MediaFormat): ByteArray {
            val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            return ByteArray(1024 * channels)
        }

        fun makeAudioFormat(inSourceFormat: MediaFormat): MediaFormat? {
            val channels = minOf(
                inSourceFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT),
                maximumNumberOfChannels,
            )
            val sampleRate = inSourceFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val mimeType = when (this) {
                aac -> MediaFormat.MIMETYPE_AUDIO_AAC
                opus -> MediaFormat.MIMETYPE_AUDIO_OPUS
            }
            return MediaFormat.createAudioFormat(mimeType, sampleRate, channels)
        }
    }
}

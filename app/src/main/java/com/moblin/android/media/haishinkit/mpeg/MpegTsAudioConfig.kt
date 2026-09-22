package com.moblin.android.media.haishinkit.mpeg

import android.media.MediaFormat

data class MpegTsAudioConfig(
    val type: AudioObjectType,
    val frequency: SamplingFrequency,
    val channel: ChannelConfiguration,
    val frameLengthFlag: Boolean = false,
) {
    constructor(formatDescription: MediaFormat) : this(
        type = when (formatDescription.getString(MediaFormat.KEY_MIME)) {
            MediaFormat.MIMETYPE_AUDIO_OPUS -> AudioObjectType.opus
            else -> AudioObjectType.fromRawValue(
                (if (formatDescription.containsKey(MediaFormat.KEY_AAC_PROFILE)) {
                    formatDescription.getInteger(MediaFormat.KEY_AAC_PROFILE)
                } else {
                    AudioObjectType.aacLc.rawValue.toInt()
                }).toUByte(),
            ) ?: AudioObjectType.unknown
        },
        frequency = SamplingFrequency.fromSampleRate(
            formatDescription.getInteger(MediaFormat.KEY_SAMPLE_RATE).toDouble(),
        ),
        channel = ChannelConfiguration.fromRawValue(
            formatDescription.getInteger(MediaFormat.KEY_CHANNEL_COUNT).toUByte(),
        ) ?: ChannelConfiguration.definedInAOTSpecificConfig,
    )

    enum class AudioObjectType(val rawValue: UByte) {
        unknown(0u),
        aacMain(1u),
        aacLc(2u),
        aacSsr(3u),
        aacLtp(4u),
        aacSbr(5u),
        aacScalable(6u),
        twinqVQ(7u),
        celp(8u),
        hvxc(9u),
        opus(10u),
        ;

        companion object {
            fun fromRawValue(rawValue: UByte): AudioObjectType? =
                AudioObjectType.entries.firstOrNull { it.rawValue == rawValue }

            fun fromObjectID(objectID: Int): AudioObjectType = when (objectID) {
                1 -> aacMain
                2 -> aacLc
                3 -> aacSsr
                4 -> aacLtp
                5 -> aacSbr
                6 -> aacScalable
                7 -> twinqVQ
                8 -> celp
                9 -> hvxc
                else -> unknown
            }
        }
    }

    enum class SamplingFrequency(val rawValue: UByte) {
        hz96000(0u),
        hz88200(1u),
        hz64000(2u),
        hz48000(3u),
        hz44100(4u),
        hz32000(5u),
        hz24000(6u),
        hz22050(7u),
        hz16000(8u),
        hz12000(9u),
        hz11025(10u),
        hz8000(11u),
        hz7350(12u),
        ;

        val sampleRate: Double
            get() = when (this) {
                hz96000 -> 96000.0
                hz88200 -> 88200.0
                hz64000 -> 64000.0
                hz48000 -> 48000.0
                hz44100 -> 44100.0
                hz32000 -> 32000.0
                hz24000 -> 24000.0
                hz22050 -> 22050.0
                hz16000 -> 16000.0
                hz12000 -> 12000.0
                hz11025 -> 11025.0
                hz8000 -> 8000.0
                hz7350 -> 7350.0
            }

        companion object {
            fun fromRawValue(rawValue: UByte): SamplingFrequency? =
                SamplingFrequency.entries.firstOrNull { it.rawValue == rawValue }

            fun fromSampleRate(sampleRate: Double): SamplingFrequency = when (sampleRate.toInt()) {
                96000 -> hz96000
                88200 -> hz88200
                64000 -> hz64000
                48000 -> hz48000
                44100 -> hz44100
                32000 -> hz32000
                24000 -> hz24000
                22050 -> hz22050
                16000 -> hz16000
                12000 -> hz12000
                11025 -> hz11025
                8000 -> hz8000
                7350 -> hz7350
                else -> hz44100
            }
        }
    }

    enum class ChannelConfiguration(val rawValue: UByte) {
        definedInAOTSpecificConfig(0u),
        frontCenter(1u),
        frontLeftAndFrontRight(2u),
        frontCenterAndFrontLeftAndFrontRight(3u),
        frontCenterAndFrontLeftAndFrontRightAndBackCenter(4u),
        frontCenterAndFrontLeftAndFrontRightAndBackLeftAndBackRight(5u),
        frontCenterAndFrontLeftAndFrontRightAndBackLeftAndBackRightLFE(6u),
        frontCenterAndFrontLeftAndFrontRightAndSideLeftAndSideRightAndBackLeftAndBackRightLFE(7u),
        ;

        companion object {
            fun fromRawValue(rawValue: UByte): ChannelConfiguration? =
                ChannelConfiguration.entries.firstOrNull { it.rawValue == rawValue }
        }
    }

    fun encode(): ByteArray {
        val data = ByteArray(2)
        data[0] = ((type.rawValue.toInt() shl 3) or (frequency.rawValue.toInt() shr 1)).toByte()
        data[1] = (((frequency.rawValue.toInt() and 0x1) shl 7) or
            ((channel.rawValue.toInt() and 0xF) shl 3)).toByte()
        return data
    }

    fun audioStreamBasicDescription(): MediaFormat {
        val mime = if (type == AudioObjectType.opus) {
            MediaFormat.MIMETYPE_AUDIO_OPUS
        } else {
            MediaFormat.MIMETYPE_AUDIO_AAC
        }
        val format = MediaFormat.createAudioFormat(
            mime,
            frequency.sampleRate.toInt(),
            channel.rawValue.toInt(),
        )
        if (mime == MediaFormat.MIMETYPE_AUDIO_AAC) {
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, type.rawValue.toInt())
        }
        return format
    }

    companion object {
        fun fromData(data: ByteArray): MpegTsAudioConfig? {
            if (data.size < 2) {
                return null
            }
            val byte0 = data[0].toInt() and 0xFF
            val byte1 = data[1].toInt() and 0xFF
            val type = AudioObjectType.fromRawValue((byte0 shr 3).toUByte()) ?: return null
            val frequency = SamplingFrequency.fromRawValue(
                (((byte0 and 0b0000_0111) shl 1) or (byte1 shr 7)).toUByte(),
            ) ?: return null
            val channel = ChannelConfiguration.fromRawValue(
                ((byte1 and 0b0111_1000) shr 3).toUByte(),
            ) ?: return null
            return MpegTsAudioConfig(type, frequency, channel)
        }
    }
}

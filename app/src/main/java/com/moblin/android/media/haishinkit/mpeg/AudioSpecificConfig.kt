package com.moblin.android.media.haishinkit.mpeg

object AudioSpecificConfig {
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
        hxvc(9u);

        companion object {
            fun fromRawValue(value: UByte): AudioObjectType? {
                return entries.firstOrNull { it.rawValue == value }
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
        hz7350(12u);

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
            fun fromRawValue(value: UByte): SamplingFrequency? {
                return entries.firstOrNull { it.rawValue == value }
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
        frontCenterAndFrontLeftAndFrontRightAndSideLeftAndSideRightAndBackLeftAndBackRightLFE(7u);

        companion object {
            fun fromRawValue(value: UByte): ChannelConfiguration? {
                return entries.firstOrNull { it.rawValue == value }
            }
        }
    }
}

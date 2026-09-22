package com.moblin.android.media.haishinkit.flv

enum class FlvAacPacketType(val rawValue: UByte) {
    seq(0u),
    raw(1u);

    companion object {
        fun fromRawValue(value: UByte): FlvAacPacketType? = entries.firstOrNull { it.rawValue == value }
    }
}

enum class FlvOpusPacketType(val rawValue: UByte) {
    sequenceStart(0u),
    codedFrames(1u);

    companion object {
        fun fromRawValue(value: UByte): FlvOpusPacketType? = entries.firstOrNull { it.rawValue == value }
    }
}

enum class FlvAvcPacketType(val rawValue: UByte) {
    seq(0u),
    nal(1u);

    companion object {
        fun fromRawValue(value: UByte): FlvAvcPacketType? = entries.firstOrNull { it.rawValue == value }
    }
}

enum class FlvFrameType(val rawValue: UByte) {
    key(1u),
    inter(2u);

    companion object {
        fun fromRawValue(value: UByte): FlvFrameType? = entries.firstOrNull { it.rawValue == value }
    }
}

enum class FlvSoundRate(val rawValue: UByte) {
    kHz5_5(0u),
    kHz11(1u),
    kHz22(2u),
    kHz44(3u);

    companion object {
        fun fromRawValue(value: UByte): FlvSoundRate? = entries.firstOrNull { it.rawValue == value }
    }
}

enum class FlvSoundSize(val rawValue: UByte) {
    snd8bit(0u),
    snd16bit(1u);

    companion object {
        fun fromRawValue(value: UByte): FlvSoundSize? = entries.firstOrNull { it.rawValue == value }
    }
}

enum class FlvSoundType(val rawValue: UByte) {
    mono(0u),
    stereo(1u);

    companion object {
        fun fromRawValue(value: UByte): FlvSoundType? = entries.firstOrNull { it.rawValue == value }
    }
}

enum class FlvTagType(val rawValue: UByte) {
    audio(8u),
    video(9u),
    data(18u);

    val streamId: UShort
        get() = when (this) {
            audio, video -> rawValue.toUShort()
            data -> 0u
        }

    val headerSize: Int
        get() = when (this) {
            audio -> 2
            video -> 5
            data -> 0
        }

    companion object {
        fun fromRawValue(value: UByte): FlvTagType? = entries.firstOrNull { it.rawValue == value }
    }
}

enum class FlvVideoCodec(val rawValue: UByte) {
    avc(7u),
    hevc(12u);

    override fun toString(): String {
        return when (this) {
            avc -> "H.264/AVC"
            hevc -> "H.265/HEVC"
        }
    }

    companion object {
        fun fromRawValue(value: UByte): FlvVideoCodec? = entries.firstOrNull { it.rawValue == value }
    }
}

enum class FlvVideoFourCC(val rawValue: UInt) {
    avc1(0x61766331u),
    hevc(0x68766331u);

    companion object {
        fun fromRawValue(value: UInt): FlvVideoFourCC? = entries.firstOrNull { it.rawValue == value }
    }
}

enum class FlvVideoPacketType(val rawValue: UByte) {
    sequenceStart(0u),
    codedFrames(1u),
    sequenceEnd(2u),
    codedFramesX(3u),
    multiTrack(6u);

    companion object {
        fun fromRawValue(value: UByte): FlvVideoPacketType? = entries.firstOrNull { it.rawValue == value }
    }
}

enum class FlvAudioCodec(val rawValue: UByte) {
    pcm(0u),
    adpcm(1u),
    mp3(2u),
    pcmle(3u),
    exHeader(9u),
    aac(10u),
    speex(11u),
    mp3_8k(14u),
    device(15u),
    unknown(0xFFu);

    val headerSize: Int
        get() = when (this) {
            aac -> 2
            else -> 1
        }

    companion object {
        fun fromRawValue(value: UByte): FlvAudioCodec? = entries.firstOrNull { it.rawValue == value }
    }
}

package com.moblin.android.media.haishinkit.mpeg

import android.media.MediaFormat

data class AdtsHeader(
    var sync: UByte = AdtsHeader.sync,
    var protectionAbsent: Boolean = false,
    var profile: UByte = 0u,
    var sampleFrequencyIndex: UByte = 0u,
    var channelConfiguration: UByte = 0u,
    var originalOrCopy: Boolean = false,
    var home: Boolean = false,
    var copyrightIdBit: Boolean = false,
    var copyrightIdStart: Boolean = false,
    var aacFrameLength: UShort = 0u,
) {
    companion object {
        val size: Int = 7
        val sync: UByte = 0xFFu

        operator fun invoke(data: ByteArray, offset: Int = 0): AdtsHeader? {
            if (offset + size > data.size) {
                return null
            }
            val syncValue = data[offset].toUByte()
            val protectionAbsent = (data[offset + 1].toInt() and 0b0000_0001) == 1
            val profile = ((data[offset + 2].toInt() and 0xFF) shr 6 and 0b11).toUByte()
            val sampleFrequencyIndex = (((data[offset + 2].toInt() and 0xFF) shr 2) and 0b0000_1111).toUByte()
            val channelConfiguration = (
                ((data[offset + 2].toInt() and 0b1) shl 2) or
                    ((data[offset + 3].toInt() and 0xFF) shr 6)
                ).toUByte()
            val originalOrCopy = (data[offset + 3].toInt() and 0b0010_0000) == 0b0010_0000
            val home = (data[offset + 3].toInt() and 0b0001_0000) == 0b0001_0000
            val copyrightIdBit = (data[offset + 3].toInt() and 0b0000_1000) == 0b0000_1000
            val copyrightIdStart = (data[offset + 3].toInt() and 0b0000_0100) == 0b0000_0100
            val aacFrameLength = (
                ((data[offset + 3].toInt() and 0b0000_0011) shl 11) or
                    ((data[offset + 4].toInt() and 0xFF) shl 3) or
                    ((data[offset + 5].toInt() and 0xFF) shr 5)
                ).toUShort()
            if (aacFrameLength.toInt() < size) {
                return null
            }
            return AdtsHeader(
                sync = syncValue,
                protectionAbsent = protectionAbsent,
                profile = profile,
                sampleFrequencyIndex = sampleFrequencyIndex,
                channelConfiguration = channelConfiguration,
                originalOrCopy = originalOrCopy,
                home = home,
                copyrightIdBit = copyrightIdBit,
                copyrightIdStart = copyrightIdStart,
                aacFrameLength = aacFrameLength,
            )
        }

        fun encode(type: UByte, frequency: UByte, channels: UByte, length: Int): ByteArray {
            val size = AdtsHeader.size
            val fullSize = size + length
            val adts = ByteArray(size)
            adts[0] = AdtsHeader.sync.toByte()
            adts[1] = 0xF9.toByte()
            adts[2] = ((type.toInt() - 1) shl 6 or (frequency.toInt() shl 2) or (channels.toInt() shr 2)).toByte()
            adts[3] = (((channels.toInt() and 3) shl 6) or (fullSize shr 11)).toByte()
            adts[4] = ((fullSize and 0x7FF) shr 3).toByte()
            adts[5] = (((fullSize and 7) shl 5) + 0x1F).toByte()
            adts[6] = 0xFC.toByte()
            return adts
        }
    }

    fun isSameFormatDescription(other: AdtsHeader?): Boolean {
        val o = other ?: return false
        return profile == o.profile &&
            sampleFrequencyIndex == o.sampleFrequencyIndex &&
            channelConfiguration == o.channelConfiguration
    }

    fun makeFormatDescription(): MediaFormat? {
        val type = AudioSpecificConfig.AudioObjectType.fromRawValue((profile.toInt() + 1).toUByte()) ?: return null
        val frequency = AudioSpecificConfig.SamplingFrequency.fromRawValue(sampleFrequencyIndex) ?: return null
        val channel = AudioSpecificConfig.ChannelConfiguration.fromRawValue(channelConfiguration) ?: return null
        val format = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_AAC,
            frequency.sampleRate.toInt(),
            channel.rawValue.toInt(),
        )
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, type.rawValue.toInt())
        return format
    }
}

class ADTSReader(private var data: ByteArray) : Iterable<Int> {
    override fun iterator(): ADTSReaderIterator = ADTSReaderIterator(data)
}

class ADTSReaderIterator(private val data: ByteArray) : Iterator<Int> {
    private var cursor: Int = 0

    override fun hasNext(): Boolean {
        if (cursor >= data.size) {
            return false
        }
        return AdtsHeader(data, cursor) != null
    }

    override fun next(): Int {
        val header = if (cursor < data.size) AdtsHeader(data, cursor) else null
        if (header == null) {
            throw NoSuchElementException()
        }
        cursor += header.aacFrameLength.toInt()
        return header.aacFrameLength.toInt() - AdtsHeader.size
    }
}

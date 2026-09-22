package com.moblin.android.media.haishinkit.mpeg

import com.moblin.android.media.haishinkit.util.ByteReader
import com.moblin.android.media.haishinkit.util.ByteWriter

enum class ElementaryStreamType(val rawValue: UByte) {
    unspecific(0x00u),
    mpeg1Video(0x01u),
    mpeg2Video(0x02u),
    mpeg1Audio(0x03u),
    mpeg2Audio(0x04u),
    mpeg2TabledData(0x05u),
    mpeg2PacketizedData(0x06u),
    adtsAac(0x0Fu),
    h263(0x10u),
    h264(0x1Bu),
    h265(0x24u),
    ;

    companion object {
        fun fromRawValue(rawValue: UByte): ElementaryStreamType? {
            return entries.firstOrNull { it.rawValue == rawValue }
        }
    }
}

enum class ElementaryStreamDescriptiorTag(val rawValue: UByte) {
    registration(0x05u),
    extension(0x7Fu),
    ;

    companion object {
        fun fromRawValue(rawValue: UByte): ElementaryStreamDescriptiorTag? {
            return entries.firstOrNull { it.rawValue == rawValue }
        }
    }
}

object ElementaryStreamDescriptiorRegistration {
    val opus: ByteArray = "Opus".toByteArray(Charsets.UTF_8)
}

data class ElementaryStreamSpecificData(
    var streamType: ElementaryStreamType = ElementaryStreamType.unspecific,
    var elementaryPacketId: UShort = 0u,
) {
    private var esDescriptors: ByteArray = ByteArray(0)

    constructor(reader: ByteReader) : this() {
        streamType = ElementaryStreamType.fromRawValue(reader.readUInt8()) ?: ElementaryStreamType.unspecific
        elementaryPacketId = (reader.readUInt16().toInt() and 0x0FFF).toUShort()
        val esInfoLength = reader.readUInt16().toInt() and 0x01FF
        esDescriptors = reader.readBytes(esInfoLength)
    }

    fun appendDescriptor(tag: ElementaryStreamDescriptiorTag, data: ByteArray) {
        esDescriptors += tag.rawValue.toByte()
        esDescriptors += data.size.toByte()
        esDescriptors += data
    }

    fun getDescriptor(tag: ElementaryStreamDescriptiorTag): ByteArray? {
        val reader = ByteReader(esDescriptors)
        try {
            while (true) {
                val rawTag = reader.readUInt8()
                val length = reader.readUInt8().toInt()
                if (ElementaryStreamDescriptiorTag.fromRawValue(rawTag) == tag) {
                    return reader.readBytes(length)
                } else {
                    reader.skipBytes(length)
                }
            }
        } catch (e: Exception) {
            return null
        }
    }

    fun encode(): ByteArray {
        val writer = ByteWriter()
        writer.writeUInt8(streamType.rawValue)
        writer.writeUInt16((elementaryPacketId.toInt() or 0xE000).toUShort())
        writer.writeUInt16((esDescriptors.size or 0xF000).toUShort())
        writer.writeBytes(esDescriptors)
        return writer.data
    }
}

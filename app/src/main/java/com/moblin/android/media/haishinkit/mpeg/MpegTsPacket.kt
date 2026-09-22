package com.moblin.android.media.haishinkit.mpeg

import com.moblin.android.media.haishinkit.util.ByteReader

class MpegTsPacket(
    var id: UShort,
) {
    var payloadUnitStartIndicator = false
    var continuityCounter: UByte = 0u
    var adaptationField: MpegTsAdaptationField? = null
    var payload: ByteArray = ByteArray(0)

    constructor(reader: ByteReader) : this(0u) {
        val startPosition = reader.position
        if (reader.readUInt8() != syncByte) {
            throw IllegalStateException("Invalid sync byte")
        }
        var byte = reader.readUInt8()
        payloadUnitStartIndicator = (byte.toInt() and 0x40) == 0x40
        id = ((byte.toInt() and 0x1F) shl 8).toUShort()
        id = id or reader.readUInt8().toUShort()
        byte = reader.readUInt8()
        continuityCounter = (byte.toInt() and 0xF).toUByte()
        val hasAdaptationField = (byte.toInt() and 0x20) == 0x20
        if (hasAdaptationField) {
            val length = reader.readUInt8()
            if (length > 0u) {
                adaptationField = MpegTsAdaptationField(reader, length)
            }
        }
        val hasPayload = (byte.toInt() and 0x10) == 0x10
        if (hasPayload) {
            payload = reader.readBytes(size - (reader.position - startPosition))
        }
    }

    fun maximumPayloadSize(): Int {
        return size - fixedHeaderSize - (adaptationField?.calcLength() ?: 0)
    }

    fun setAdaptionFieldStuffing(size: Int) {
        adaptationField?.setStuffing(size)
    }

    fun encodeFixedHeaderInto(pointer: ByteArray, offset: Int = 0) {
        pointer[offset + 0] = syncByte.toByte()
        pointer[offset + 1] = ((if (payloadUnitStartIndicator) 0x40 else 0) or ((id.toInt() shr 8) and 0xFF)).toByte()
        pointer[offset + 2] = (id.toInt() and 0xFF).toByte()
        pointer[offset + 3] = ((if (adaptationField != null) 0x20 else 0) or 0x10 or continuityCounter.toInt()).toByte()
    }

    fun encode(): ByteArray {
        var data = ByteArray(fixedHeaderSize)
        encodeFixedHeaderInto(data)
        adaptationField?.let {
            data = data + it.encode()
        }
        return data + payload
    }

    companion object {
        const val size: Int = 188
        const val fixedHeaderSize: Int = 4
        val syncByte: UByte = 0x47u
        val programAssociationTableId: UShort = 0u
    }
}

object TSTimestamp {
    const val resolution: Double = 90 * 1000.0
    const val dataSize: Int = 5

    fun encode(b: Long, m: UByte): ByteArray {
        val value = b and 0x1_FFFF_FFFFL
        val encoded = ByteArray(dataSize)
        encoded[0] = (((value shr 29) and 0xFFL).toInt() or 0x01 or m.toInt()).toByte()
        encoded[1] = ((value shr 22) and 0xFFL).toByte()
        encoded[2] = (((value shr 14) and 0xFFL).toInt() or 0x01).toByte()
        encoded[3] = ((value shr 7) and 0xFFL).toByte()
        encoded[4] = (((value shl 1) and 0xFFL).toInt() or 0x01).toByte()
        return encoded
    }

    fun decode(data: ByteArray, offset: Int = 0): Long? {
        if (data.size < offset + dataSize) {
            return null
        }
        var result: Long = 0
        result = result or ((data[offset + 0].toLong() and 0x0E) shl 29)
        result = result or ((data[offset + 1].toLong() and 0xFF) shl 22)
        result = result or ((data[offset + 2].toLong() and 0xFE) shl 14)
        result = result or ((data[offset + 3].toLong() and 0xFF) shl 7)
        result = result or ((data[offset + 4].toLong() and 0xFE) shr 1)
        return result
    }
}

object TSProgramClockReference {
    fun encode(b: ULong, e: UShort): ByteArray {
        val encoded = ByteArray(6)
        encoded[0] = ((b shr 25) and 0xFFuL).toByte()
        encoded[1] = ((b shr 17) and 0xFFuL).toByte()
        encoded[2] = ((b shr 9) and 0xFFuL).toByte()
        encoded[3] = ((b shr 1) and 0xFFuL).toByte()
        encoded[4] = 0xFF.toByte()
        if ((b and 1uL) == 1uL) {
            encoded[4] = (encoded[4].toInt() or 0x80).toByte()
        } else {
            encoded[4] = (encoded[4].toInt() and 0x7F).toByte()
        }
        val reserved = ((encoded[4].toInt() and 0x01).toUInt() shr 8).toInt()
        if (reserved == 1) {
            encoded[4] = (encoded[4].toInt() or 0x01).toByte()
        } else {
            encoded[4] = (encoded[4].toInt() and 0xFE).toByte()
        }
        encoded[5] = ((e.toULong()) and 0xFFuL).toByte()
        return encoded
    }
}

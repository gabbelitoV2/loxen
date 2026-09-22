package com.moblin.android.integrations.dji

import com.moblin.android.media.haishinkit.util.ByteReader
import com.moblin.android.media.haishinkit.util.ByteWriter

private const val firstByte = 0x55
private const val version = 0x04

private fun djiCrc8(data: ByteArray): Int {
    var crc = 0xEE
    for (byte in data) {
        crc = crc xor (byte.toInt() and 0xFF)
        repeat(8) {
            crc = if (crc and 1 != 0) {
                (crc ushr 1) xor 0x8C
            } else {
                crc ushr 1
            }
        }
    }
    return crc and 0xFF
}

private fun djiCrc16(data: ByteArray): Int {
    var crc = 0x496C
    for (byte in data) {
        crc = crc xor (byte.toInt() and 0xFF)
        repeat(8) {
            crc = if (crc and 1 != 0) {
                (crc ushr 1) xor 0x8408
            } else {
                crc ushr 1
            }
        }
    }
    return crc and 0xFFFF
}

fun djiPackString(value: String): ByteArray {
    val data = value.toByteArray(Charsets.UTF_8)
    return byteArrayOf((data.size and 0xFF).toByte()) + data
}

fun djiPackUrl(url: String): ByteArray {
    val data = url.toByteArray(Charsets.UTF_8)
    return byteArrayOf((data.size and 0xFF).toByte(), 0) + data
}

class DjiMessage {
    var target: Int
    var id: Int
    var type: Int
    var payload: ByteArray

    constructor(target: Int, id: Int, type: Int, payload: ByteArray) {
        this.target = target
        this.id = id
        this.type = type
        this.payload = payload
    }

    constructor(data: ByteArray) {
        val reader = ByteReader(data)
        if (reader.readUInt8().toInt() != firstByte) {
            throw IllegalArgumentException("Bad first byte")
        }
        val length = reader.readUInt8().toInt()
        if (data.size != length) {
            throw IllegalArgumentException("Bad length")
        }
        if (reader.readUInt8().toInt() != version) {
            throw IllegalArgumentException("Bad version")
        }
        val headerCrc = reader.readUInt8().toInt()
        val calculatedHeaderCrc = djiCrc8(data.copyOfRange(0, 3))
        if (headerCrc != calculatedHeaderCrc) {
            throw IllegalArgumentException(
                "Calculated CRC $calculatedHeaderCrc does not match received CRC $headerCrc"
            )
        }
        target = reader.readUInt16Le().toInt()
        id = reader.readUInt16Le().toInt()
        type = reader.readUInt24Le().toInt()
        payload = reader.readBytes(reader.bytesAvailable - 2)
        val crc = reader.readUInt16Le().toInt()
        val dataWithoutCrc = data.copyOfRange(0, data.size - 2)
        val calculatedCrc = djiCrc16(dataWithoutCrc)
        if (crc != calculatedCrc) {
            throw IllegalArgumentException(
                "Calculated CRC $calculatedCrc does not match received CRC $crc"
            )
        }
    }

    fun encode(): ByteArray {
        val writer = ByteWriter()
        writer.writeUInt8(firstByte.toUByte())
        writer.writeUInt8(((13 + payload.size) and 0xFF).toUByte())
        writer.writeUInt8(version.toUByte())
        writer.writeUInt8(djiCrc8(writer.data).toUByte())
        writer.writeUInt16Le(target.toUShort())
        writer.writeUInt16Le(id.toUShort())
        writer.writeUInt24Le(type.toUInt())
        writer.writeBytes(payload)
        val crc = djiCrc16(writer.data)
        writer.writeUInt16Le(crc.toUShort())
        return writer.data
    }

    fun format(): String {
        val hex = payload.joinToString("") {
            (it.toInt() and 0xFF).toString(16).padStart(2, '0')
        }
        return "target: $target, id: $id, type: $type $hex"
    }
}

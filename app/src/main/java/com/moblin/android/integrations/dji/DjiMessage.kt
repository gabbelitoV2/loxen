package com.moblin.android.integrations.dji

import com.moblin.android.media.haishinkit.util.ByteReader
import com.moblin.android.media.haishinkit.util.ByteWriter
import com.moblin.android.platform.crcswift.CrcSwift

private val firstByte: UByte = 0x55u
private val version: UByte = 0x04u

class DjiMessageError(message: String) : Exception(message) {
    override fun toString(): String = message ?: ""
}

private fun djiCrc8(data: ByteArray): UByte {
    return CrcSwift.computeCrc8(
        data,
        initialCrc = 0xEEu,
        polynom = 0x31u,
        xor = 0x00u,
        refIn = true,
        refOut = true,
    )
}

private fun djiCrc16(data: ByteArray): UShort {
    return CrcSwift.computeCrc16(
        data,
        initialCrc = 0x496Cu,
        polynom = 0x1021u,
        xor = 0x0000u,
        refIn = true,
        refOut = true,
    )
}

private fun ByteArray.hexString(): String {
    return joinToString("") { "%02x".format(it.toInt() and 0xFF) }
}

fun djiPackString(value: String): ByteArray {
    val data = value.toByteArray()
    return byteArrayOf((data.size and 0xFF).toByte()) + data
}

fun djiPackUrl(url: String): ByteArray {
    val data = url.toByteArray()
    return byteArrayOf((data.size and 0xFF).toByte(), 0) + data
}

open class DjiMessage(
    open var target: UShort,
    open var id: UShort,
    open var type: UInt,
    open var payload: ByteArray,
) {
    constructor(data: ByteArray) : this(0u, 0u, 0u, ByteArray(0)) {
        val reader = ByteReader(data)
        if (reader.readUInt8() != firstByte) {
            throw DjiMessageError("Bad first byte")
        }
        val length = reader.readUInt8()
        if (data.size != length.toInt()) {
            throw DjiMessageError("Bad length")
        }
        if (reader.readUInt8() != version) {
            throw DjiMessageError("Bad version")
        }
        val hedaerCrc = reader.readUInt8()
        val calculatedHeaderCrc = djiCrc8(data.copyOfRange(0, 3))
        if (hedaerCrc != calculatedHeaderCrc) {
            throw DjiMessageError(
                "Calculated CRC $calculatedHeaderCrc does not match received CRC $hedaerCrc",
            )
        }
        target = reader.readUInt16Le()
        id = reader.readUInt16Le()
        type = reader.readUInt24Le()
        payload = reader.readBytes(reader.bytesAvailable - 2)
        val crc = reader.readUInt16Le()
        val payloadData = data.copyOfRange(0, data.size - 2)
        val calculatedCrc = djiCrc16(payloadData)
        if (crc != calculatedCrc) {
            throw DjiMessageError("Calculated CRC $calculatedCrc does not match received CRC $crc")
        }
    }

    open fun encode(): ByteArray {
        val writer = ByteWriter()
        writer.writeUInt8(firstByte)
        writer.writeUInt8(((13 + payload.size) and 0xFF).toUByte())
        writer.writeUInt8(version)
        writer.writeUInt8(djiCrc8(writer.data))
        writer.writeUInt16Le(target)
        writer.writeUInt16Le(id)
        writer.writeUInt24Le(type)
        writer.writeBytes(payload)
        val crc = djiCrc16(writer.data)
        writer.writeUInt16Le(crc)
        return writer.data
    }

    open fun format(): String {
        return "target: $target, id: $id, type: $type ${payload.hexString()}"
    }
}

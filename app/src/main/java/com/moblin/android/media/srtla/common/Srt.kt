package com.moblin.android.media.srtla.common

val srtControlPacketTypeBit: UShort = 0x8000u

const val srtControlTypeSize: Int = 2

enum class SrtPacketType(val rawValue: UShort) {
    handshake(0x0000u),
    keepAlive(0x0001u),
    ack(0x0002u),
    nak(0x0003u),
    congestionWarning(0x0004u),
    shutdown(0x0005u),
    ackAck(0x0006u),
    dropReq(0x0007u),
    peerError(0x0008u);

    companion object {
        fun fromRawValue(rawValue: UShort): SrtPacketType? =
            SrtPacketType.entries.firstOrNull { it.rawValue == rawValue }
    }
}

fun isSrtDataPacket(packet: ByteArray): Boolean {
    return (packet[0].toInt() and 0x80) == 0
}

fun getSrtControlPacketType(packet: ByteArray): UShort {
    return readUInt16Be(packet) and 0x7FFFu
}

fun getSrtSequenceNumber(packet: ByteArray): UInt {
    return readUInt32Be(packet)
}

fun isSrtSnAcked(sn: UInt, ackSn: UInt): Boolean {
    return if (sn < ackSn) {
        ackSn - sn < 100_000_000u
    } else {
        sn - ackSn > 100_000_000u
    }
}

fun isSrtSnRange(sn: UInt): Boolean {
    return (sn and 0x8000_0000u) == 0x8000_0000u
}

fun processSrtNak(packet: ByteArray, onNak: (UInt) -> Unit) {
    var offset = 16
    while (offset <= packet.size - 4) {
        val nakSn = readUInt32Be(packet, offset)
        offset += 4
        if (isSrtSnRange(nakSn)) {
            if (offset > packet.size - 4) {
                return
            }
            val upToNakSn = readUInt32Be(packet, offset)
            offset += 4
            val fromNakSn = nakSn and 0x7FFF_FFFFu
            if (upToNakSn - fromNakSn >= 100_000u) {
                continue
            }
            for (sn in fromNakSn..upToNakSn) {
                onNak(sn)
            }
        } else {
            onNak(nakSn)
        }
    }
}

private fun readUInt16Be(packet: ByteArray, offset: Int = 0): UShort {
    val value = ((packet[offset].toInt() and 0xFF) shl 8) or
        (packet[offset + 1].toInt() and 0xFF)
    return value.toUShort()
}

private fun readUInt32Be(packet: ByteArray, offset: Int = 0): UInt {
    val value = ((packet[offset].toLong() and 0xFF) shl 24) or
        ((packet[offset + 1].toLong() and 0xFF) shl 16) or
        ((packet[offset + 2].toLong() and 0xFF) shl 8) or
        (packet[offset + 3].toLong() and 0xFF)
    return value.toUInt()
}

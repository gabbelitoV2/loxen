package com.moblin.android.media.srtla.common

enum class SrtlaPacketType(val rawValue: UShort) {
    keepalive(0x1000u.toUShort()),
    ack(0x1100u.toUShort()),
    reg1(0x1200u.toUShort()),
    reg2(0x1201u.toUShort()),
    reg3(0x1202u.toUShort()),
    regErr(0x1210u.toUShort()),
    regNgp(0x1211u.toUShort()),
    regNak(0x1212u.toUShort());

    companion object {
        fun fromRawValue(rawValue: UShort): SrtlaPacketType? =
            entries.firstOrNull { it.rawValue == rawValue }
    }
}

fun createSrtlaPacket(type: SrtlaPacketType, length: Int): ByteArray {
    val packet = ByteArray(length)
    val value = (type.rawValue.toInt() or srtControlPacketTypeBit.toInt()) and 0xFFFF
    packet[0] = ((value shr 8) and 0xFF).toByte()
    packet[1] = (value and 0xFF).toByte()
    return packet
}

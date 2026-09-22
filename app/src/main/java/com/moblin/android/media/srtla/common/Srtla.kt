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
    packet.setUInt16Be(type.rawValue or srtControlPacketTypeBit)
    return packet
}

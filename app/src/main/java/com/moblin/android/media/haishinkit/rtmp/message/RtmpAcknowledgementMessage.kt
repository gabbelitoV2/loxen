package com.moblin.android.media.haishinkit.rtmp.message

class RtmpAcknowledgementMessage : RtmpMessage(RtmpMessageType.ack) {
    var sequence: UInt
        get() = if (encoded.size >= 4) encoded.toUInt32BigEndian() else 0u
        set(value) {
            encoded = value.toBigEndianBytes()
        }

    init {
        encoded = 0u.toBigEndianBytes()
    }
}

private fun UInt.toBigEndianBytes(): ByteArray = byteArrayOf(
    (this shr 24).toByte(),
    (this shr 16).toByte(),
    (this shr 8).toByte(),
    this.toByte(),
)

private fun ByteArray.toUInt32BigEndian(): UInt {
    return ((this[0].toInt() and 0xFF).toUInt() shl 24) or
        ((this[1].toInt() and 0xFF).toUInt() shl 16) or
        ((this[2].toInt() and 0xFF).toUInt() shl 8) or
        (this[3].toInt() and 0xFF).toUInt()
}

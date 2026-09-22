package com.moblin.android.media.haishinkit.rtmp.message

class RtmpWindowAcknowledgementSizeMessage : RtmpMessage {
    var size: UInt = 0u
        set(value) {
            field = value
            encoded = value.toBigEndianBytes()
        }

    constructor() : super(RtmpMessageType.windowAck)

    constructor(size: UInt) : super(RtmpMessageType.windowAck) {
        this.size = size
    }
}

private fun UInt.toBigEndianBytes(): ByteArray =
    byteArrayOf(
        (this shr 24).toByte(),
        (this shr 16).toByte(),
        (this shr 8).toByte(),
        this.toByte()
    )

private fun ByteArray.toUInt32BigEndian(): UInt {
    var result = 0u
    for (i in 0 until 4) {
        result = (result shl 8) or (if (i < this.size) (this[i].toUInt() and 0xFFu) else 0u)
    }
    return result
}

package com.moblin.android.media.haishinkit.rtmp.message

class RtmpWindowAcknowledgementSizeMessage : RtmpMessage {
    var size: UInt = 0u

    constructor() : super(RtmpMessageType.windowAck)

    constructor(size: UInt) : super(RtmpMessageType.windowAck) {
        this.size = size
    }

    override var encoded: ByteArray
        get() {
            if (super.encoded.isNotEmpty()) {
                return super.encoded
            }
            super.encoded = size.toBigEndianBytes()
            return super.encoded
        }
        set(value) {
            if (super.encoded.contentEquals(value)) {
                return
            }
            size = value.toUInt32BigEndian()
            super.encoded = value
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

package com.moblin.android.media.haishinkit.rtmp.message

class RtmpSetChunkSizeMessage : RtmpMessage {
    var size: UInt = 0u
        set(value) {
            field = value
            encoded = uint32ToBigEndianBytes(value)
        }

    constructor() : super(RtmpMessageType.chunkSize) {
        encoded = uint32ToBigEndianBytes(size)
    }

    constructor(size: UInt) : super(RtmpMessageType.chunkSize) {
        this.size = size
    }
}

private fun uint32ToBigEndianBytes(value: UInt): ByteArray {
    return byteArrayOf(
        ((value shr 24) and 0xFFu).toByte(),
        ((value shr 16) and 0xFFu).toByte(),
        ((value shr 8) and 0xFFu).toByte(),
        (value and 0xFFu).toByte()
    )
}

private fun readUInt32BigEndian(data: ByteArray): UInt {
    var value = 0u
    val count = if (data.size < 4) data.size else 4
    for (i in 0 until count) {
        value = (value shl 8) or data[i].toUByte().toUInt()
    }
    return value
}

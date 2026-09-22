package com.moblin.android.media.haishinkit.rtmp.message

class RtmpAbortMessge : RtmpMessage {
    var chunkStreamId: UInt = 0u

    constructor() : super(RtmpMessageType.abort)

    var encodedData: ByteArray
        get() {
            if (super.encoded.isNotEmpty()) {
                return super.encoded
            }
            super.encoded = byteArrayOf(
                ((chunkStreamId shr 24) and 0xFFu).toByte(),
                ((chunkStreamId shr 16) and 0xFFu).toByte(),
                ((chunkStreamId shr 8) and 0xFFu).toByte(),
                (chunkStreamId and 0xFFu).toByte(),
            )
            return super.encoded
        }
        set(value) {
            if (super.encoded.contentEquals(value)) {
                return
            }
            val b0 = value[0].toInt() and 0xFF
            val b1 = value[1].toInt() and 0xFF
            val b2 = value[2].toInt() and 0xFF
            val b3 = value[3].toInt() and 0xFF
            chunkStreamId = ((b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3).toUInt()
            super.encoded = value
        }
}

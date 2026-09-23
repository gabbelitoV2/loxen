package com.moblin.android.media.haishinkit.rtmp.message

class RtmpAbortMessge : RtmpMessage {
    var chunkStreamId: UInt = 0u

    constructor() : super(RtmpMessageType.abort)

    override var encoded: ByteArray
        get() {
            if (super.encoded.isNotEmpty()) {
                return super.encoded
            }
            super.encoded = uint32ToBigEndianBytes(chunkStreamId)
            return super.encoded
        }
        set(newValue) {
            if (super.encoded.contentEquals(newValue)) {
                return
            }
            chunkStreamId = readUInt32BigEndian(newValue)
            super.encoded = newValue
        }
}

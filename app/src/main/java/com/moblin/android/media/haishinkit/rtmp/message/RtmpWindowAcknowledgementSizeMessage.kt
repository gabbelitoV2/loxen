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
            super.encoded = uint32ToBigEndianBytes(size)
            return super.encoded
        }
        set(newValue) {
            if (super.encoded.contentEquals(newValue)) {
                return
            }
            size = readUInt32BigEndian(newValue)
            super.encoded = newValue
        }
}

package com.moblin.android.media.haishinkit.rtmp.message

class RtmpAcknowledgementMessage : RtmpMessage {
    var sequence: UInt = 0u

    constructor() : super(RtmpMessageType.ack)

    override var encoded: ByteArray
        get() {
            if (super.encoded.isNotEmpty()) {
                return super.encoded
            }
            super.encoded = uint32ToBigEndianBytes(sequence)
            return super.encoded
        }
        set(newValue) {
            if (super.encoded.contentEquals(newValue)) {
                return
            }
            sequence = readUInt32BigEndian(newValue)
            super.encoded = newValue
        }
}

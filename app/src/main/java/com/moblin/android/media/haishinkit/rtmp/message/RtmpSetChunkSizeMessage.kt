package com.moblin.android.media.haishinkit.rtmp.message

class RtmpSetChunkSizeMessage : RtmpMessage {
    var size: UInt = 0u

    constructor() : super(RtmpMessageType.chunkSize)

    constructor(size: UInt) : super(RtmpMessageType.chunkSize) {
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

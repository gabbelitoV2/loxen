package com.moblin.android.media.haishinkit.rtmp.message

import java.nio.ByteBuffer

class RtmpSetPeerBandwidthMessage : RtmpMessage {

    enum class Limit(val rawValue: UByte) {
        HARD(0x00u),
        SOFT(0x01u),
        DYNAMIC(0x02u),
        UNKNOWN(0xFFu);

        companion object {
            fun fromRawValue(value: UByte): Limit =
                Limit.entries.firstOrNull { it.rawValue == value } ?: Limit.UNKNOWN
        }
    }

    var size: UInt = 0u
    var limit: Limit = Limit.HARD

    constructor() : super(RtmpMessageType.BANDWIDTH)

    constructor(size: UInt, limit: Limit) : super(RtmpMessageType.BANDWIDTH) {
        this.size = size
        this.limit = limit
    }

    override var encoded: ByteArray
        get() {
            if (super.encoded.isNotEmpty()) {
                return super.encoded
            }
            val payload = ByteBuffer.allocate(5)
            payload.putInt(size.toInt())
            payload.put(limit.rawValue.toByte())
            super.encoded = payload.array()
            return super.encoded
        }
        set(value) {
            if (super.encoded.contentEquals(value)) {
                return
            }
            if (value.size >= 5) {
                size = ByteBuffer.wrap(value, 0, 4).int.toUInt()
                limit = Limit.fromRawValue(value[4].toUByte())
            }
            super.encoded = value
        }
}

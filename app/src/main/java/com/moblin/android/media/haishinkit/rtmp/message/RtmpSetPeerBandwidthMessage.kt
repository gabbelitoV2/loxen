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

    constructor() : super(RtmpMessageType.entries.first { it.rawValue.toInt() == 0x06 }) {
        encoded = createEncoded()
    }

    constructor(size: UInt, limit: Limit) : super(RtmpMessageType.entries.first { it.rawValue.toInt() == 0x06 }) {
        this.size = size
        this.limit = limit
        encoded = createEncoded()
    }

    private fun createEncoded(): ByteArray {
        val payload = ByteBuffer.allocate(5)
        payload.putInt(size.toInt())
        payload.put(limit.rawValue.toByte())
        return payload.array()
    }
}

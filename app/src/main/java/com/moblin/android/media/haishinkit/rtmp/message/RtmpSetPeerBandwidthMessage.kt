package com.moblin.android.media.haishinkit.rtmp.message

class RtmpSetPeerBandwidthMessage : RtmpMessage {
    enum class Limit(val rawValue: UByte) {
        hard(0x00u),
        soft(0x01u),
        `dynamic`(0x02u),
        unknown(0xFFu),
        ;

        companion object {
            val HARD: Limit
                get() = Limit.hard

            val SOFT: Limit
                get() = Limit.soft

            val DYNAMIC: Limit
                get() = Limit.`dynamic`

            val UNKNOWN: Limit
                get() = Limit.unknown

            fun fromRawValue(rawValue: UByte): Limit {
                return entries.firstOrNull { it.rawValue == rawValue } ?: Limit.unknown
            }
        }
    }

    var size: UInt = 0u
    var limit: Limit = Limit.hard

    constructor() : super(RtmpMessageType.bandwidth)

    constructor(size: UInt, limit: Limit) : super(RtmpMessageType.bandwidth) {
        this.size = size
        this.limit = limit
    }

    override var encoded: ByteArray
        get() {
            if (super.encoded.isNotEmpty()) {
                return super.encoded
            }
            var payload = ByteArray(0)
            payload += uint32ToBigEndianBytes(size)
            payload += limit.rawValue.toByte()
            super.encoded = payload
            return super.encoded
        }
        set(newValue) {
            if (super.encoded.contentEquals(newValue)) {
                return
            }
            if (newValue.size >= 5) {
                size = readUInt32BigEndian(newValue.copyOfRange(0, 4))
                limit = Limit.fromRawValue(newValue[4].toUByte())
            }
            super.encoded = newValue
        }
}

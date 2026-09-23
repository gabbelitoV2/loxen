package com.moblin.android.media.haishinkit.rtmp.message

class RtmpUserControlMessage : RtmpMessage {
    enum class Event(val rawValue: UByte) {
        streamBegin(0x00u),
        streamEof(0x01u),
        streamDry(0x02u),
        setBuffer(0x03u),
        recorded(0x04u),
        ping(0x06u),
        pong(0x07u),
        bufferEmpty(0x1Fu),
        bufferFull(0x20u),
        unknown(0xFFu),
        ;

        val bytes: ByteArray
            get() = byteArrayOf(0x00, rawValue.toByte())

        companion object {
            fun fromRawValue(rawValue: UByte): Event? {
                return entries.firstOrNull { it.rawValue == rawValue }
            }
        }
    }

    var event: Event = Event.unknown
    var value: Int = 0

    constructor() : super(RtmpMessageType.user)

    constructor(event: Event, value: Int) : super(RtmpMessageType.user) {
        this.event = event
        this.value = value
    }

    override var encoded: ByteArray
        get() {
            if (super.encoded.isNotEmpty()) {
                return super.encoded
            }
            super.encoded = ByteArray(0)
            super.encoded = super.encoded + event.bytes
            super.encoded = super.encoded + int32ToBigEndianBytes(value)
            return super.encoded
        }
        set(newValue) {
            if (super.encoded.contentEquals(newValue)) {
                return
            }
            if (length == newValue.size && newValue.size >= 2) {
                val event = Event.fromRawValue(newValue[1].toUByte())
                if (event != null) {
                    this.event = event
                }
                value = readInt32BigEndian(newValue.copyOfRange(2, newValue.size))
            }
            super.encoded = newValue
        }
}

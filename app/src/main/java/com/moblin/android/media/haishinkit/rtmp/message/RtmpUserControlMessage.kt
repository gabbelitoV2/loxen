package com.moblin.android.media.haishinkit.rtmp.message

class RtmpUserControlMessage : RtmpMessage(RtmpMessageType.user) {
    enum class Event(val rawValue: UByte) {
        streamBegin(0x00.toUByte()),
        streamEof(0x01.toUByte()),
        streamDry(0x02.toUByte()),
        setBuffer(0x03.toUByte()),
        recorded(0x04.toUByte()),
        ping(0x06.toUByte()),
        pong(0x07.toUByte()),
        bufferEmpty(0x1F.toUByte()),
        bufferFull(0x20.toUByte()),
        unknown(0xFF.toUByte());

        val bytes: ByteArray
            get() = byteArrayOf(0x00.toByte(), rawValue.toByte())

        companion object {
            fun fromRawValue(rawValue: UByte): Event? {
                for (event in entries) {
                    if (event.rawValue == rawValue) {
                        return event
                    }
                }
                return null
            }
        }
    }

    var event: Event = Event.unknown
    var value: Int = 0

    constructor(event: Event, value: Int) : this() {
        this.event = event
        this.value = value
    }

    override var encoded: ByteArray
        get() {
            if (super.encoded.isNotEmpty()) {
                return super.encoded
            }
            val data = event.bytes + byteArrayOf(
                (value ushr 24).toByte(),
                (value ushr 16).toByte(),
                (value ushr 8).toByte(),
                value.toByte()
            )
            super.encoded = data
            return data
        }
        set(newValue) {
            if (super.encoded.contentEquals(newValue)) {
                return
            }
            if (super.encoded.size == newValue.size && newValue.size >= 2) {
                Event.fromRawValue(newValue[1].toUByte())?.let { event = it }
                var parsed = 0
                var index = 2
                val end = minOf(newValue.size, 6)
                while (index < end) {
                    parsed = (parsed shl 8) or (newValue[index].toInt() and 0xFF)
                    index++
                }
                value = parsed
            }
            super.encoded = newValue
        }
}

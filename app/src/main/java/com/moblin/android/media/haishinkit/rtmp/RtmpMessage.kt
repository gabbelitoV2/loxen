package com.moblin.android.media.haishinkit.rtmp

import com.moblin.android.platform.log.Log
import com.moblin.android.media.haishinkit.rtmp.amf.Amf0Decoder
import com.moblin.android.media.haishinkit.rtmp.amf.Amf0Encoder
import com.moblin.android.media.haishinkit.rtmp.amf.AsObject
import com.moblin.android.media.haishinkit.rtmp.amf.AsValue

private const val TAG = "RtmpMessage"

private fun rtmpBigEndianBytes(value: UInt): ByteArray = byteArrayOf(
    ((value shr 24) and 0xFFu).toByte(),
    ((value shr 16) and 0xFFu).toByte(),
    ((value shr 8) and 0xFFu).toByte(),
    (value and 0xFFu).toByte(),
)

private fun rtmpBigEndianUInt32(data: ByteArray, offset: Int = 0): UInt {
    var value = 0u
    for (i in 0 until 4) {
        val index = offset + i
        val byte = if (index < data.size) data[index].toInt() and 0xFF else 0
        value = (value shl 8) or byte.toUInt()
    }
    return value
}

private fun rtmpBigEndianInt32(data: ByteArray, offset: Int = 0): Int = rtmpBigEndianUInt32(data, offset).toInt()

private fun rtmpHexString(data: ByteArray): String {
    val builder = StringBuilder(data.size * 2)
    for (element in data) {
        val value = element.toInt() and 0xFF
        builder.append("0123456789abcdef"[value shr 4])
        builder.append("0123456789abcdef"[value and 0x0F])
    }
    return builder.toString()
}

enum class RtmpMessageType(val rawValue: UByte) {
    chunkSize(0x01u.toUByte()),
    abort(0x02u.toUByte()),
    ack(0x03u.toUByte()),
    user(0x04u.toUByte()),
    windowAck(0x05u.toUByte()),
    bandwidth(0x06u.toUByte()),
    audio(0x08u.toUByte()),
    video(0x09u.toUByte()),
    amf3Data(0x0Fu.toUByte()),
    amf3Command(0x11u.toUByte()),
    amf0Data(0x12u.toUByte()),
    amf0Command(0x14u.toUByte()),
    aggregate(0x16u.toUByte());

    companion object {
        fun fromRawValue(rawValue: UByte): RtmpMessageType? = entries.firstOrNull { it.rawValue == rawValue }
    }
}

open class RtmpMessage(val type: RtmpMessageType) {
    var length: Int = 0
    var streamId: UInt = 0u
    var timestamp: UInt = 0u
    open var encoded: ByteArray = ByteArray(0)

    companion object {
        fun create(type: RtmpMessageType): RtmpMessage = when (type) {
            RtmpMessageType.chunkSize -> RtmpSetChunkSizeMessage()
            RtmpMessageType.abort -> RtmpAbortMessage()
            RtmpMessageType.ack -> RtmpAcknowledgementMessage()
            RtmpMessageType.user -> RtmpUserControlMessage()
            RtmpMessageType.windowAck -> RtmpWindowAcknowledgementSizeMessage()
            RtmpMessageType.bandwidth -> RtmpSetPeerBandwidthMessage()
            RtmpMessageType.audio -> RtmpAudioMessage()
            RtmpMessageType.video -> RtmpVideoMessage()
            RtmpMessageType.amf3Data -> RtmpDataMessage(RtmpMessageType.amf3Data)
            RtmpMessageType.amf3Command -> RtmpCommandMessage(RtmpMessageType.amf3Command)
            RtmpMessageType.amf0Data -> RtmpDataMessage(RtmpMessageType.amf0Data)
            RtmpMessageType.amf0Command -> RtmpCommandMessage(RtmpMessageType.amf0Command)
            RtmpMessageType.aggregate -> RtmpAggregateMessage()
        }
    }
}

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
            super.encoded = rtmpBigEndianBytes(size)
            return super.encoded
        }
        set(newValue) {
            if (super.encoded.contentEquals(newValue)) {
                return
            }
            size = rtmpBigEndianUInt32(newValue)
            super.encoded = newValue
        }
}

class RtmpAbortMessage : RtmpMessage {
    var chunkStreamId: UInt = 0u

    constructor() : super(RtmpMessageType.abort)

    override var encoded: ByteArray
        get() {
            if (super.encoded.isNotEmpty()) {
                return super.encoded
            }
            super.encoded = rtmpBigEndianBytes(chunkStreamId)
            return super.encoded
        }
        set(newValue) {
            if (super.encoded.contentEquals(newValue)) {
                return
            }
            chunkStreamId = rtmpBigEndianUInt32(newValue)
            super.encoded = newValue
        }
}

class RtmpAcknowledgementMessage : RtmpMessage {
    var sequence: UInt = 0u

    constructor() : super(RtmpMessageType.ack)

    override var encoded: ByteArray
        get() {
            if (super.encoded.isNotEmpty()) {
                return super.encoded
            }
            super.encoded = rtmpBigEndianBytes(sequence)
            return super.encoded
        }
        set(newValue) {
            if (super.encoded.contentEquals(newValue)) {
                return
            }
            sequence = rtmpBigEndianUInt32(newValue)
            super.encoded = newValue
        }
}

class RtmpUserControlMessage : RtmpMessage {
    enum class Event(val rawValue: UByte) {
        streamBegin(0x00u.toUByte()),
        streamEof(0x01u.toUByte()),
        streamDry(0x02u.toUByte()),
        setBuffer(0x03u.toUByte()),
        recorded(0x04u.toUByte()),
        ping(0x06u.toUByte()),
        pong(0x07u.toUByte()),
        bufferEmpty(0x1Fu.toUByte()),
        bufferFull(0x20u.toUByte()),
        unknown(0xFFu.toUByte());

        val bytes: ByteArray
            get() = byteArrayOf(0x00, rawValue.toByte())

        companion object {
            fun fromRawValue(rawValue: UByte): Event? = entries.firstOrNull { it.rawValue == rawValue }
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
            super.encoded = event.bytes + rtmpBigEndianBytes(value.toUInt())
            return super.encoded
        }
        set(newValue) {
            if (super.encoded.contentEquals(newValue)) {
                return
            }
            if (length == newValue.size && newValue.size >= 2) {
                Event.fromRawValue(newValue[1].toUByte())?.let { event = it }
                value = rtmpBigEndianInt32(newValue, 2)
            }
            super.encoded = newValue
        }
}

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
            super.encoded = rtmpBigEndianBytes(size)
            return super.encoded
        }
        set(newValue) {
            if (super.encoded.contentEquals(newValue)) {
                return
            }
            size = rtmpBigEndianUInt32(newValue)
            super.encoded = newValue
        }
}

class RtmpSetPeerBandwidthMessage : RtmpMessage {
    enum class Limit(val rawValue: UByte) {
        hard(0x00u.toUByte()),
        soft(0x01u.toUByte()),
        dynamic(0x02u.toUByte()),
        unknown(0xFFu.toUByte());

        companion object {
            fun fromRawValue(rawValue: UByte): Limit? = entries.firstOrNull { it.rawValue == rawValue }
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
            super.encoded = rtmpBigEndianBytes(size) + byteArrayOf(limit.rawValue.toByte())
            return super.encoded
        }
        set(newValue) {
            if (super.encoded.contentEquals(newValue)) {
                return
            }
            if (newValue.size >= 5) {
                size = rtmpBigEndianUInt32(newValue, 0)
                limit = Limit.fromRawValue(newValue[4].toUByte()) ?: Limit.unknown
            }
            super.encoded = newValue
        }
}

class RtmpAudioMessage : RtmpMessage {
    constructor() : super(RtmpMessageType.audio)

    constructor(streamId: UInt, timestamp: UInt, payload: ByteArray) : super(RtmpMessageType.audio) {
        this.streamId = streamId
        this.timestamp = timestamp
        encoded = payload
    }
}

class RtmpVideoMessage : RtmpMessage {
    constructor() : super(RtmpMessageType.video)

    constructor(streamId: UInt, timestamp: UInt, payload: ByteArray) : super(RtmpMessageType.video) {
        this.streamId = streamId
        this.timestamp = timestamp
        encoded = payload
    }
}

class RtmpDataMessage : RtmpMessage {
    var handlerName: String = ""
    var arguments: MutableList<AsValue> = mutableListOf()

    constructor(dataType: RtmpMessageType) : super(dataType)

    constructor(
        streamId: UInt,
        dataType: RtmpMessageType,
        timestamp: UInt,
        handlerName: String,
        arguments: List<AsValue> = emptyList(),
    ) : super(dataType) {
        this.handlerName = handlerName
        this.arguments = arguments.toMutableList()
        this.timestamp = timestamp
        this.streamId = streamId
    }

    override var encoded: ByteArray
        get() {
            if (super.encoded.isNotEmpty()) {
                return super.encoded
            }
            val serializer = Amf0Encoder()
            if (type == RtmpMessageType.amf3Data) {
                serializer.writeUInt8(0x00u.toUByte())
            }
            serializer.encode(AsValue.String(handlerName))
            for (argument in arguments) {
                serializer.encode(argument)
            }
            super.encoded = serializer.data.copyOf(serializer.length)
            return super.encoded
        }
        set(newValue) {
            if (super.encoded.contentEquals(newValue)) {
                return
            }
            if (length == newValue.size) {
                val decoder = Amf0Decoder(newValue)
                if (type == RtmpMessageType.amf3Data) {
                    decoder.position = 1
                }
                try {
                    handlerName = decoder.decodeString()
                    while (decoder.bytesAvailable > 0) {
                        arguments.add(decoder.decode())
                    }
                } catch (error: Exception) {
                    Log.i(TAG, "rtmp-data-message: $decoder")
                }
            }
            super.encoded = newValue
        }
}

enum class RtmpCommandName(val rawValue: String) {
    connect("connect"),
    close("close"),
    result("_result"),
    error("_error"),
    publish("publish"),
    createStream("createStream"),
    releaseStream("releaseStream"),
    fcPublish("FCPublish"),
    fcUnpublish("FCUnpublish"),
    deleteStream("deleteStream"),
    closeStream("closeStream"),
    onStatus("onStatus"),
    onFcPublish("onFCPublish"),
    unknown("unknown");

    companion object {
        fun fromRawValue(rawValue: String): RtmpCommandName? = entries.firstOrNull { it.rawValue == rawValue }
    }
}

class RtmpCommandMessage : RtmpMessage {
    var commandName: RtmpCommandName = RtmpCommandName.unknown
        private set
    var transactionId: Int = 0
        private set
    var commandObject: AsObject? = null
        private set
    var arguments: MutableList<AsValue> = mutableListOf()
        private set

    constructor(commandType: RtmpMessageType) : super(commandType)

    constructor(
        streamId: UInt,
        transactionId: Int,
        commandType: RtmpMessageType,
        commandName: RtmpCommandName,
        commandObject: AsObject?,
        arguments: List<AsValue>,
    ) : super(commandType) {
        this.transactionId = transactionId
        this.commandName = commandName
        this.commandObject = commandObject
        this.arguments = arguments.toMutableList()
        this.streamId = streamId
    }

    override var encoded: ByteArray
        get() {
            if (super.encoded.isNotEmpty()) {
                return super.encoded
            }
            val serializer = Amf0Encoder()
            if (type == RtmpMessageType.amf3Command) {
                serializer.writeUInt8(0x00u.toUByte())
            }
            serializer.encode(AsValue.String(commandName.rawValue))
            serializer.encode(AsValue.Number(transactionId.toDouble()))
            val commandObject = commandObject
            if (commandObject != null) {
                serializer.encode(AsValue.Object(commandObject))
            } else {
                serializer.writeUInt8(0x05u.toUByte())
            }
            for (argument in arguments) {
                serializer.encode(argument)
            }
            super.encoded = serializer.data.copyOf(serializer.length)
            return super.encoded
        }
        set(newValue) {
            if (length == newValue.size) {
                val decoder = Amf0Decoder(newValue)
                try {
                    if (type == RtmpMessageType.amf3Command) {
                        decoder.position = 1
                    }
                    commandName = RtmpCommandName.fromRawValue(decoder.decodeString()) ?: RtmpCommandName.unknown
                    transactionId = decoder.decodeInt()
                    commandObject = decoder.decodeObject()
                    arguments.clear()
                    while (decoder.bytesAvailable > 0) {
                        arguments.add(decoder.decode())
                    }
                } catch (error: Exception) {
                    Log.i(TAG, "rtmp: Command message error: $error: ${rtmpHexString(newValue)}")
                    commandName = RtmpCommandName.unknown
                    commandObject = null
                    arguments.clear()
                }
            }
            super.encoded = newValue
        }
}

class RtmpAggregateMessage : RtmpMessage {
    constructor() : super(RtmpMessageType.aggregate)
}

package com.moblin.android.media.haishinkit.rtmp.message

import android.util.Log
import com.moblin.android.media.haishinkit.rtmp.amf.Amf0Decoder
import com.moblin.android.media.haishinkit.rtmp.amf.Amf0Encoder
import com.moblin.android.media.haishinkit.rtmp.amf.AsObject
import com.moblin.android.media.haishinkit.rtmp.amf.AsValue

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
        arguments: List<AsValue>
    ) : super(commandType) {
        this.transactionId = transactionId
        this.commandName = commandName
        this.commandObject = commandObject
        this.arguments = arguments.toMutableList()
        this.streamId = streamId
        encode()
    }

    fun encode(): ByteArray {
        if (encoded.isNotEmpty()) {
            return encoded
        }
        val serializer = Amf0Encoder()
        if (type == RtmpMessageType.amf3Command) {
            serializer.writeBytes(byteArrayOf(0))
        }
        serializer.encode(AsValue.String(commandName.rawValue))
        serializer.encode(AsValue.Number(transactionId.toDouble()))
        val obj = commandObject
        if (obj != null) {
            serializer.encode(AsValue.Object(obj))
        } else {
            serializer.encode(AsValue.Null)
        }
        for (argument in arguments) {
            serializer.encode(argument)
        }
        encoded = serializer.data
        return encoded
    }

    fun decode(value: ByteArray) {
        if (length == value.size) {
            val decoder = Amf0Decoder()
            decoder.data = value
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
            } catch (e: Exception) {
                Log.i(TAG, "rtmp: Command message error: $e: ${value.hexString()}")
                commandName = RtmpCommandName.unknown
                commandObject = null
                arguments.clear()
            }
        }
        encoded = value
    }
}

private const val TAG: String = "RtmpCommandMessage"

private fun ByteArray.hexString(): String = joinToString("") { "%02x".format(it) }

package com.moblin.android.media.haishinkit.rtmp.message

import android.util.Log
import com.moblin.android.common.various.hexString
import com.moblin.android.media.haishinkit.rtmp.amf.Amf0Decoder
import com.moblin.android.media.haishinkit.rtmp.amf.Amf0Encoder
import com.moblin.android.media.haishinkit.rtmp.amf.AsObject
import com.moblin.android.media.haishinkit.rtmp.amf.AsValue

private const val TAG = "RtmpCommandMessage"

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
    unknown("unknown"),
    ;

    companion object {
        fun fromRawValue(rawValue: String): RtmpCommandName? {
            return entries.firstOrNull { it.rawValue == rawValue }
        }
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
                serializer.writeUInt8(0u)
            }
            serializer.encode(AsValue.String(commandName.rawValue))
            serializer.encode(AsValue.Number(transactionId.toDouble()))
            val commandObject = commandObject
            if (commandObject != null) {
                serializer.encode(AsValue.Object(commandObject))
            } else {
                serializer.encode(AsValue.Null)
            }
            for (argument in arguments) {
                serializer.encode(argument)
            }
            super.encoded = serializer.data
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
                    Log.i(TAG, "rtmp: Command message error: $error: ${newValue.hexString()}")
                    commandName = RtmpCommandName.unknown
                    commandObject = null
                    arguments.clear()
                }
            }
            super.encoded = newValue
        }
}

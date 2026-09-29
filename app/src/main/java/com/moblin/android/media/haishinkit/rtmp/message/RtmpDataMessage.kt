package com.moblin.android.media.haishinkit.rtmp.message

import com.moblin.android.platform.log.Log
import com.moblin.android.media.haishinkit.rtmp.amf.Amf0Decoder
import com.moblin.android.media.haishinkit.rtmp.amf.Amf0Encoder
import com.moblin.android.media.haishinkit.rtmp.amf.AsValue

private const val TAG = "RtmpDataMessage"

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
                serializer.writeUInt8(0u)
            }
            serializer.encode(AsValue.String(handlerName))
            for (argument in arguments) {
                serializer.encode(argument)
            }
            super.encoded = serializer.data
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

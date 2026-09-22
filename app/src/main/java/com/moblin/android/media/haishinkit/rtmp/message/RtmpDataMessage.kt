package com.moblin.android.media.haishinkit.rtmp.message

import android.util.Log
import com.moblin.android.media.haishinkit.rtmp.amf.Amf0Decoder
import com.moblin.android.media.haishinkit.rtmp.amf.Amf0Encoder
import com.moblin.android.media.haishinkit.rtmp.amf.AsValue

class RtmpDataMessage : RtmpMessage {
    var handlerName: String = ""
    var arguments: MutableList<AsValue> = mutableListOf()

    constructor(dataType: RtmpMessageType) : super(dataType)

    constructor(
        streamId: UInt,
        dataType: RtmpMessageType,
        timestamp: UInt,
        handlerName: String,
        arguments: List<AsValue> = emptyList()
    ) : super(dataType) {
        this.handlerName = handlerName
        this.arguments = arguments.toMutableList()
        this.timestamp = timestamp
        this.streamId = streamId
    }

    var encodedData: ByteArray
        get() {
            if (encoded.isNotEmpty()) {
                return encoded
            }
            val serializer = Amf0Encoder()
            if (type == RtmpMessageType.amf3Data) {
                serializer.writeUInt8(0.toUByte())
            }
            serializer.encode(AsValue.String(handlerName))
            for (argument in arguments) {
                serializer.encode(argument)
            }
            encoded = serializer.data
            return encoded
        }
        set(value) {
            if (encoded.contentEquals(value)) {
                return
            }
            if (length == value.size) {
                val decoder = Amf0Decoder(value)
                if (type == RtmpMessageType.amf3Data) {
                    decoder.position = 1
                }
                try {
                    handlerName = decoder.decodeString()
                    while (decoder.bytesAvailable > 0) {
                        arguments.add(decoder.decode())
                    }
                } catch (e: Exception) {
                    Log.i(TAG, "rtmp-data-message: $decoder")
                }
            }
            encoded = value
        }

    companion object {
        private const val TAG = "RtmpDataMessage"
    }
}

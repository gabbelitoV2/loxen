package com.moblin.android.media.haishinkit.rtmp.message

class RtmpVideoMessage : RtmpMessage {
    constructor() : super(RtmpMessageType.video)

    constructor(streamId: UInt, timestamp: UInt, payload: ByteArray) : super(RtmpMessageType.video) {
        this.streamId = streamId
        this.timestamp = timestamp
        encoded = payload
    }
}

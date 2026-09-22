package com.moblin.android.media.haishinkit.rtmp.message

class RtmpAudioMessage : RtmpMessage {
    constructor() : super(RtmpMessageType.audio)

    constructor(streamId: UInt, timestamp: UInt, payload: ByteArray) : super(RtmpMessageType.audio) {
        this.streamId = streamId
        this.timestamp = timestamp
        encoded = payload
    }
}

package com.moblin.android.media.haishinkit.rtmp.message

enum class RtmpMessageType(val rawValue: UByte) {
    chunkSize(0x01u),
    abort(0x02u),
    ack(0x03u),
    user(0x04u),
    windowAck(0x05u),
    bandwidth(0x06u),
    audio(0x08u),
    video(0x09u),
    amf3Data(0x0Fu),
    amf3Command(0x11u),
    amf0Data(0x12u),
    amf0Command(0x14u),
    aggregate(0x16u);

    companion object {
        fun fromRawValue(rawValue: UByte): RtmpMessageType? {
            return entries.firstOrNull { it.rawValue == rawValue }
        }
    }
}

open class RtmpMessage(val type: RtmpMessageType) {
    var length: Int = 0
    var streamId: UInt = 0u
    var timestamp: UInt = 0u
    var encoded: ByteArray = ByteArray(0)

    companion object {
        fun create(type: RtmpMessageType): RtmpMessage {
            return when (type) {
                RtmpMessageType.chunkSize -> RtmpSetChunkSizeMessage()
                RtmpMessageType.abort -> RtmpAbortMessge()
                RtmpMessageType.ack -> RtmpAcknowledgementMessage()
                RtmpMessageType.user -> RtmpUserControlMessage()
                RtmpMessageType.windowAck -> RtmpWindowAcknowledgementSizeMessage()
                RtmpMessageType.bandwidth -> RtmpSetPeerBandwidthMessage()
                RtmpMessageType.audio -> RtmpAudioMessage()
                RtmpMessageType.video -> RtmpVideoMessage()
                RtmpMessageType.amf3Data -> RtmpDataMessage(dataType = RtmpMessageType.amf3Data)
                RtmpMessageType.amf3Command -> RtmpCommandMessage(commandType = RtmpMessageType.amf3Command)
                RtmpMessageType.amf0Data -> RtmpDataMessage(dataType = RtmpMessageType.amf0Data)
                RtmpMessageType.amf0Command -> RtmpCommandMessage(commandType = RtmpMessageType.amf0Command)
                RtmpMessageType.aggregate -> RtmpAggregateMessage()
            }
        }
    }
}

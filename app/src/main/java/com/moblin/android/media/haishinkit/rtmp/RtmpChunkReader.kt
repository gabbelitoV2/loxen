package com.moblin.android.media.haishinkit.rtmp

import com.moblin.android.media.haishinkit.rtmp.message.RtmpMessage
import com.moblin.android.media.haishinkit.rtmp.message.RtmpMessageType
import com.moblin.android.media.haishinkit.util.ByteReader

private val extendedTimestampMarker: UInt = 0xFFFFFFu

private data class RtmpBasicHeader(
    val type: RtmpChunkType,
    val chunkStreamId: UShort
)

private data class RtmpChunkStream(
    var timestamp: UInt = 0u,
    var timestampDelta: UInt = 0u,
    var messageType: RtmpMessageType? = null,
    var messageLength: Int = 0,
    var messageStreamId: UInt = 0u,
    var hasExtendedTimestamp: Boolean = false,
    var payload: ByteArray = ByteArray(0)
)

private fun ByteReader.canRead(count: Int): Boolean = bytesAvailable >= count

class RtmpChunkReader {
    var maximumChunkSize: Int = RtmpChunk.defaultSize

    private val chunkStreams = mutableMapOf<UShort, RtmpChunkStream>()

    fun clear() {
        maximumChunkSize = RtmpChunk.defaultSize
        chunkStreams.clear()
    }

    fun read(data: ByteArray, onMessage: (RtmpMessage) -> Unit): ByteArray {
        var offset = 0
        while (true) {
            val size = readChunk(data, offset, onMessage) ?: break
            offset += size
        }
        if (offset <= 0) {
            return data
        }
        return data.copyOfRange(offset, data.size)
    }

    private fun readChunk(data: ByteArray, offset: Int, onMessage: (RtmpMessage) -> Unit): Int? {
        val reader = ByteReader(data)
        reader.position = offset
        val basicHeader = readBasicHeader(reader) ?: return null
        val chunkStream = chunkStreams[basicHeader.chunkStreamId] ?: RtmpChunkStream()
        if (!readMessageHeader(reader, basicHeader.type, chunkStream)) {
            return null
        }
        val payloadSize = minOf(chunkStream.messageLength - chunkStream.payload.size, maximumChunkSize)
        if (!reader.canRead(payloadSize)) {
            return null
        }
        chunkStream.payload += reader.readBytes(payloadSize)
        if (chunkStream.payload.size == chunkStream.messageLength) {
            val message = makeMessage(chunkStream)
            if (message != null) {
                onMessage(message)
            }
            chunkStream.payload = ByteArray(0)
        }
        chunkStreams[basicHeader.chunkStreamId] = chunkStream
        return reader.position - offset
    }

    private fun readBasicHeader(reader: ByteReader): RtmpBasicHeader? {
        if (!reader.canRead(1)) {
            return null
        }
        val firstByte = reader.readUInt8()
        val type = RtmpChunkType.fromRawValue(((firstByte.toInt() and 0xFF) shr 6).toUByte()) ?: return null
        return when (val chunkStreamId = firstByte.toInt() and 0b0011_1111) {
            0 -> {
                if (!reader.canRead(1)) {
                    return null
                }
                RtmpBasicHeader(type, (reader.readUInt8().toUShort() + 64u.toUShort()).toUShort())
            }
            1 -> {
                if (!reader.canRead(2)) {
                    return null
                }
                RtmpBasicHeader(type, (reader.readUInt16Le() + 64u.toUShort()).toUShort())
            }
            else -> RtmpBasicHeader(type, chunkStreamId.toUShort())
        }
    }

    private fun readMessageHeader(
        reader: ByteReader,
        type: RtmpChunkType,
        chunkStream: RtmpChunkStream
    ): Boolean {
        if (!reader.canRead(type.messageHeaderSize())) {
            return false
        }
        var timestamp = chunkStream.timestampDelta
        when (type) {
            RtmpChunkType.zero -> {
                timestamp = reader.readUInt24()
                chunkStream.messageLength = reader.readUInt24().toInt()
                chunkStream.messageType = RtmpMessageType.fromRawValue(reader.readUInt8())
                chunkStream.messageStreamId = reader.readUInt32Le()
            }
            RtmpChunkType.one -> {
                timestamp = reader.readUInt24()
                chunkStream.messageLength = reader.readUInt24().toInt()
                chunkStream.messageType = RtmpMessageType.fromRawValue(reader.readUInt8())
            }
            RtmpChunkType.two -> {
                timestamp = reader.readUInt24()
            }
            RtmpChunkType.three -> {
            }
        }
        if (type != RtmpChunkType.three) {
            chunkStream.hasExtendedTimestamp = timestamp == extendedTimestampMarker
        }
        if (chunkStream.hasExtendedTimestamp) {
            if (!reader.canRead(4)) {
                return false
            }
            timestamp = reader.readUInt32()
        }
        val isFirstChunkOfMessage = type != RtmpChunkType.three || chunkStream.payload.isEmpty()
        if (isFirstChunkOfMessage) {
            chunkStream.payload = ByteArray(0)
            if (type == RtmpChunkType.zero) {
                chunkStream.timestamp = timestamp
            } else {
                chunkStream.timestamp += timestamp
            }
        }
        chunkStream.timestampDelta = if (type == RtmpChunkType.zero) 0u else timestamp
        return true
    }

    private fun makeMessage(chunkStream: RtmpChunkStream): RtmpMessage? {
        val messageType = chunkStream.messageType ?: return null
        val message = RtmpMessage.create(messageType)
        message.timestamp = chunkStream.timestamp
        message.length = chunkStream.messageLength
        message.streamId = chunkStream.messageStreamId
        message.encoded = chunkStream.payload
        return message
    }
}

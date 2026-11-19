package com.moblin.android.media.haishinkit.rtmp

import com.moblin.android.media.haishinkit.rtmp.RtmpMessage
import com.moblin.android.media.haishinkit.util.ByteReader
import com.moblin.android.media.haishinkit.util.ByteWriter

private val extendedTimestampMarker: UInt = 0xFFFFFFu

enum class RtmpChunkType(val rawValue: UByte) {
    zero(0.toUByte()),
    one(1.toUByte()),
    two(2.toUByte()),
    three(3.toUByte());

    fun messageHeaderSize(): Int = when (this) {
        RtmpChunkType.zero -> 11
        RtmpChunkType.one -> 7
        RtmpChunkType.two -> 3
        RtmpChunkType.three -> 0
    }

    fun toBasicHeader(chunkStreamId: UShort): ByteArray {
        val raw = rawValue.toInt()
        val id = chunkStreamId.toInt()
        if (id <= 63) {
            return byteArrayOf((raw shl 6 or id).toByte())
        }
        if (id <= 319) {
            return byteArrayOf((raw shl 6).toByte(), (id - 64).toByte())
        }
        val value = id - 64
        return byteArrayOf(
            (raw shl 6 or 1).toByte(),
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte()
        )
    }

    companion object {
        fun fromRawValue(rawValue: UByte): RtmpChunkType? =
            entries.firstOrNull { it.rawValue == rawValue }
    }
}

private fun basicAndMessageHeadersSize(
    chunkStreamId: UShort,
    type: RtmpChunkType,
    hasExtendedTimestamp: Boolean
): Int =
    basicHeaderSize(chunkStreamId) + type.messageHeaderSize() +
        (if (hasExtendedTimestamp) 4 else 0)

private fun basicHeaderSize(chunkStreamId: UShort): Int {
    val id = chunkStreamId.toInt()
    if (id <= 63) {
        return 1
    }
    if (id <= 319) {
        return 2
    }
    return 3
}

class RtmpChunk(
    private val type: RtmpChunkType,
    private val chunkStreamId: UShort,
    val message: RtmpMessage
) {
    enum class ChunkStreamId(val rawValue: UShort) {
        control(0x02u.toUShort()),
        command(0x03u.toUShort()),
        `data`(0x04u.toUShort());

        companion object {
            fun fromRawValue(rawValue: UShort): ChunkStreamId? =
                entries.firstOrNull { it.rawValue == rawValue }
        }
    }

    companion object {
        const val defaultSize = 128
    }

    constructor(message: RtmpMessage) : this(
        type = RtmpChunkType.zero,
        chunkStreamId = ChunkStreamId.command.rawValue,
        message = message
    )

    private val hasExtendedTimestamp: Boolean
        get() = message.timestamp >= extendedTimestampMarker

    fun encode(): ByteArray {
        val writer = ByteWriter()
        writer.writeBytes(type.toBasicHeader(chunkStreamId))
        if (hasExtendedTimestamp) {
            writer.writeUInt24(extendedTimestampMarker)
        } else {
            writer.writeUInt24(message.timestamp)
        }
        writer.writeUInt24(message.encoded.size.toUInt())
        writer.writeUInt8(message.type.rawValue)
        if (type == RtmpChunkType.zero) {
            writer.writeUInt32Le(message.streamId)
        }
        if (hasExtendedTimestamp) {
            writer.writeUInt32(message.timestamp)
        }
        return writer.data + message.encoded
    }

    fun split(maximumSize: Int): List<ByteArray> {
        val data = encode()
        message.length = data.size
        if (maximumSize >= message.encoded.size) {
            return listOf(data)
        }
        val startIndex = maximumSize + basicAndMessageHeadersSize(
            chunkStreamId = chunkStreamId,
            type = type,
            hasExtendedTimestamp = hasExtendedTimestamp
        )
        var header = RtmpChunkType.three.toBasicHeader(chunkStreamId)
        if (hasExtendedTimestamp) {
            val ts = message.timestamp
            header += byteArrayOf(
                ((ts shr 24) and 0xFFu).toByte(),
                ((ts shr 16) and 0xFFu).toByte(),
                ((ts shr 8) and 0xFFu).toByte(),
                (ts and 0xFFu).toByte()
            )
        }
        val chunks = mutableListOf(data.copyOfRange(0, startIndex))
        var index = startIndex
        while (index < data.size) {
            val endIndex = index +
                if (index + maximumSize < data.size) maximumSize else data.size - index
            chunks.add(header + data.copyOfRange(index, endIndex))
            index += maximumSize
        }
        return chunks
    }
}

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
    var maximumChunkSize = RtmpChunk.defaultSize
    private var chunkStreams: MutableMap<UShort, RtmpChunkStream> = mutableMapOf()

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
        val chunkStream = (chunkStreams[basicHeader.chunkStreamId] ?: RtmpChunkStream()).copy()
        if (!readMessageHeader(reader, basicHeader.type, chunkStream)) {
            return null
        }
        val payloadSize = minOf(chunkStream.messageLength - chunkStream.payload.size, maximumChunkSize)
        if (!reader.canRead(payloadSize)) {
            return null
        }
        chunkStream.payload += reader.readBytes(payloadSize)
        if (chunkStream.payload.size == chunkStream.messageLength) {
            makeMessage(chunkStream)?.let { onMessage(it) }
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
        val type = RtmpChunkType.fromRawValue((firstByte.toInt() shr 6).toUByte()) ?: return null
        return when (val chunkStreamId = firstByte.toInt() and 0b0011_1111) {
            0 -> {
                if (!reader.canRead(1)) {
                    return null
                }
                RtmpBasicHeader(type, (reader.readUInt8().toInt() + 64).toUShort())
            }
            1 -> {
                if (!reader.canRead(2)) {
                    return null
                }
                RtmpBasicHeader(type, (reader.readUInt16Le().toInt() + 64).toUShort())
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

package com.moblin.android.media.haishinkit.rtmp

import com.moblin.android.media.haishinkit.rtmp.message.RtmpMessage
import com.moblin.android.media.haishinkit.util.ByteWriter

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
        data(0x04u.toUShort());

        companion object {
            fun fromRawValue(rawValue: UShort): ChunkStreamId? =
                entries.firstOrNull { it.rawValue == rawValue }
        }
    }

    companion object {
        const val defaultSize = 128
        private val extendedTimestampMarker: UInt = 0xFFFFFFu
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

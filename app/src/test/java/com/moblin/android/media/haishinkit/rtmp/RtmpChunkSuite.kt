package com.moblin.android.media.haishinkit.rtmp

import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RtmpChunkSuite {
    data class BasicHeaderCase(
        val type: RtmpChunkType,
        val chunkStreamId: UShort,
        val expected: ByteArray,
    )

    @Test
    fun basicHeader() {
        val cases = listOf(
            BasicHeaderCase(RtmpChunkType.zero, 2.toUShort(), bytes(0x02)),
            BasicHeaderCase(RtmpChunkType.one, 3.toUShort(), bytes(0x43)),
            BasicHeaderCase(RtmpChunkType.two, 4.toUShort(), bytes(0x84)),
            BasicHeaderCase(RtmpChunkType.three, 63.toUShort(), bytes(0xFF)),
            BasicHeaderCase(RtmpChunkType.zero, 64.toUShort(), bytes(0x00, 0x00)),
            BasicHeaderCase(RtmpChunkType.three, 319.toUShort(), bytes(0xC0, 0xFF)),
            BasicHeaderCase(RtmpChunkType.zero, 320.toUShort(), bytes(0x01, 0x00, 0x01)),
            BasicHeaderCase(RtmpChunkType.one, 400.toUShort(), bytes(0x41, 0x50, 0x01)),
        )
        for (testCase in cases) {
            assertContentEquals(testCase.expected, testCase.type.toBasicHeader(testCase.chunkStreamId))
        }
    }

    @Test
    fun encodeTypeZero() {
        val message = RtmpSetChunkSizeMessage(4096u)
        message.streamId = 1u
        message.timestamp = 0x010203u
        val chunk = RtmpChunk(type = RtmpChunkType.zero, chunkStreamId = 3.toUShort(), message = message)
        assertContentEquals(
            bytes(
                0x03, 0x01, 0x02, 0x03, 0x00, 0x00, 0x04, 0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10, 0x00,
            ),
            chunk.encode(),
        )
    }

    @Test
    fun encodeTypeOne() {
        val chunk = RtmpChunk(
            type = RtmpChunkType.one,
            chunkStreamId = 8.toUShort(),
            message = RtmpAudioMessage(1u, 20u, bytes(1, 2, 3)),
        )
        assertContentEquals(bytes(0x48, 0x00, 0x00, 0x14, 0x00, 0x00, 0x03, 0x08, 1, 2, 3), chunk.encode())
    }

    @Test
    fun encodeTypeZeroWithExtendedTimestamp() {
        val chunk = RtmpChunk(
            type = RtmpChunkType.zero,
            chunkStreamId = 4.toUShort(),
            message = RtmpAudioMessage(1u, 0x0100_0000u, bytes(7)),
        )
        assertContentEquals(
            bytes(
                0x04, 0xFF, 0xFF, 0xFF, 0x00, 0x00, 0x01, 0x08, 0x01, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 7,
            ),
            chunk.encode(),
        )
    }

    @Test
    fun encodeTypeZeroWithTimestampAtExtendedMarker() {
        val chunk = RtmpChunk(
            type = RtmpChunkType.zero,
            chunkStreamId = 4.toUShort(),
            message = RtmpAudioMessage(1u, 0xFFFFFFu, bytes(7)),
        )
        assertContentEquals(
            bytes(
                0x04, 0xFF, 0xFF, 0xFF, 0x00, 0x00, 0x01, 0x08, 0x01, 0x00, 0x00, 0x00, 0x00, 0xFF, 0xFF, 0xFF, 7,
            ),
            chunk.encode(),
        )
    }

    @Test
    fun defaultChunkIsTypeZeroOnCommandChunkStream() {
        val chunk = RtmpChunk(message = RtmpSetChunkSizeMessage(128u))
        assertContentEquals(bytes(0x03), chunk.encode().copyOfRange(0, 1))
    }

    @Test
    fun splitSmallerThanMaximumSize() {
        val chunk = RtmpChunk(
            type = RtmpChunkType.zero,
            chunkStreamId = 8.toUShort(),
            message = RtmpAudioMessage(1u, 5u, makePayload(128)),
        )
        val chunks = chunk.split(maximumSize = 128)
        val expected = listOf(chunk.encode())
        assertChunks(expected, chunks)
    }

    @Test
    fun splitOneByteLargerThanMaximumSize() {
        val payload = makePayload(129)
        val chunk = RtmpChunk(
            type = RtmpChunkType.zero,
            chunkStreamId = 8.toUShort(),
            message = RtmpAudioMessage(1u, 5u, payload),
        )
        val chunks = chunk.split(maximumSize = 128)
        val expected = listOf(
            chunk.encode().copyOfRange(0, 12 + 128),
            bytes(0xC8) + payload.copyOfRange(payload.size - 1, payload.size),
        )
        assertChunks(expected, chunks)
    }

    @Test
    fun splitIntoThreeChunks() {
        val payload = makePayload(300)
        val chunk = RtmpChunk(
            type = RtmpChunkType.zero,
            chunkStreamId = 8.toUShort(),
            message = RtmpAudioMessage(1u, 5u, payload),
        )
        val chunks = chunk.split(maximumSize = 128)
        val expected = listOf(
            chunk.encode().copyOfRange(0, 12 + 128),
            bytes(0xC8) + payload.copyOfRange(128, 256),
            bytes(0xC8) + payload.copyOfRange(256, 300),
        )
        assertChunks(expected, chunks)
    }

    @Test
    fun splitSetsMessageLengthToEncodedChunkSize() {
        val message = RtmpAudioMessage(1u, 5u, makePayload(300))
        RtmpChunk(type = RtmpChunkType.zero, chunkStreamId = 8.toUShort(), message = message)
            .split(maximumSize = 128)
        assertEquals(12 + 300, message.length)
    }

    @Test
    fun splitTypeOneSetsMessageLengthToEncodedChunkSize() {
        val message = RtmpAudioMessage(1u, 5u, makePayload(10))
        RtmpChunk(type = RtmpChunkType.one, chunkStreamId = 8.toUShort(), message = message)
            .split(maximumSize = 128)
        assertEquals(8 + 10, message.length)
    }

    @Test
    fun splitWithExtendedTimestamp() {
        val payload = makePayload(200)
        val chunk = RtmpChunk(
            type = RtmpChunkType.zero,
            chunkStreamId = 8.toUShort(),
            message = RtmpAudioMessage(1u, 0x0100_0000u, payload),
        )
        val chunks = chunk.split(maximumSize = 128)
        val expected = listOf(
            chunk.encode().copyOfRange(0, 16 + 128),
            bytes(0xC8, 0x01, 0x00, 0x00, 0x00) + payload.copyOfRange(128, 200),
        )
        assertChunks(expected, chunks)
    }

    @Test
    fun splitWithTwoByteBasicHeader() {
        val payload = makePayload(200)
        val chunk = RtmpChunk(
            type = RtmpChunkType.zero,
            chunkStreamId = 100.toUShort(),
            message = RtmpAudioMessage(1u, 5u, payload),
        )
        val chunks = chunk.split(maximumSize = 128)
        val expected = listOf(
            chunk.encode().copyOfRange(0, 13 + 128),
            bytes(0xC0, 36) + payload.copyOfRange(128, 200),
        )
        assertChunks(expected, chunks)
    }

    @Test
    fun splitWithLargerMaximumSize() {
        val payload = makePayload(10000)
        val chunk = RtmpChunk(
            type = RtmpChunkType.zero,
            chunkStreamId = 9.toUShort(),
            message = RtmpVideoMessage(1u, 5u, payload),
        )
        val chunks = chunk.split(maximumSize = 8192)
        val expected = listOf(
            chunk.encode().copyOfRange(0, 12 + 8192),
            bytes(0xC9) + payload.copyOfRange(8192, 10000),
        )
        assertChunks(expected, chunks)
    }

    @Test
    fun splitChunksAreReadBack() {
        val payload = makePayload(1000)
        val chunk = RtmpChunk(
            type = RtmpChunkType.zero,
            chunkStreamId = 9.toUShort(),
            message = RtmpVideoMessage(1u, 33u, payload),
        )
        val reader = RtmpChunkReader()
        reader.maximumChunkSize = 300
        val messages = mutableListOf<RtmpMessage>()
        val remaining = reader.read(data = concat(chunk.split(maximumSize = 300))) {
            messages.add(it)
        }
        assertTrue(remaining.isEmpty())
        val message = messages.first()
        assertEquals(1, messages.size)
        assertEquals(RtmpMessageType.video, message.type)
        assertEquals(33u, message.timestamp)
        assertContentEquals(payload, message.encoded)
    }

    @Test
    fun readerClearRestoresDefaultChunkSize() {
        val reader = RtmpChunkReader()
        reader.maximumChunkSize = 4096
        reader.clear()
        assertEquals(RtmpChunk.defaultSize, reader.maximumChunkSize)
    }

    @Test
    fun readerClearDropsPartialMessage() {
        val reader = RtmpChunkReader()
        val payload = makePayload(200)
        val data = concat(
            RtmpChunk(
                type = RtmpChunkType.zero,
                chunkStreamId = 8.toUShort(),
                message = RtmpAudioMessage(1u, 5u, payload),
            ).split(maximumSize = 128),
        )
        val messages = mutableListOf<RtmpMessage>()
        assertTrue(reader.read(data = data.copyOfRange(0, 12 + 128)) { messages.add(it) }.isEmpty())
        reader.clear()
        val ack = RtmpAcknowledgementMessage()
        ack.sequence = 7u
        val ackData = RtmpChunk(type = RtmpChunkType.zero, chunkStreamId = 2.toUShort(), message = ack).encode()
        assertTrue(reader.read(data = ackData) { messages.add(it) }.isEmpty())
        assertEquals(listOf(7u), messages.mapNotNull { (it as? RtmpAcknowledgementMessage)?.sequence })
    }
}

private fun bytes(vararg values: Int): ByteArray = ByteArray(values.size) { values[it].toByte() }

private fun makePayload(count: Int): ByteArray = ByteArray(count) { (it % 251).toByte() }

private fun concat(chunks: List<ByteArray>): ByteArray {
    val size = chunks.sumOf { it.size }
    val result = ByteArray(size)
    var offset = 0
    for (chunk in chunks) {
        chunk.copyInto(result, offset)
        offset += chunk.size
    }
    return result
}

private fun assertChunks(expected: List<ByteArray>, actual: List<ByteArray>) {
    assertEquals(expected.size, actual.size)
    for (index in expected.indices) {
        assertContentEquals(expected[index], actual[index])
    }
}

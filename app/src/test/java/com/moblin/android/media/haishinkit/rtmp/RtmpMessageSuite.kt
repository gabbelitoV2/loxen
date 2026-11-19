package com.moblin.android.media.haishinkit.rtmp

import com.moblin.android.media.haishinkit.rtmp.amf.Amf0Encoder
import com.moblin.android.media.haishinkit.rtmp.amf.AsValue
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RtmpMessageSuite {
    @Test
    fun setChunkSizeEncode() {
        assertContentEquals(byteArrayOf(0x00, 0x00, 0x10, 0x00), RtmpSetChunkSizeMessage(4096u).encoded)
    }

    @Test
    fun setChunkSizeDecode() {
        val message = assertNotNull(transfer(RtmpSetChunkSizeMessage(8192u)) as? RtmpSetChunkSizeMessage)
        assertEquals(8192u, message.size)
    }

    @Test
    fun windowAcknowledgementSizeEncode() {
        assertContentEquals(
            byteArrayOf(0x00, 0x26, 0x25, 0xA0.toByte()),
            RtmpWindowAcknowledgementSizeMessage(2_500_000u).encoded,
        )
    }

    @Test
    fun windowAcknowledgementSizeDecode() {
        val message = assertNotNull(
            transfer(RtmpWindowAcknowledgementSizeMessage(100_000u)) as? RtmpWindowAcknowledgementSizeMessage,
        )
        assertEquals(100_000u, message.size)
    }

    @Test
    fun acknowledgementEncode() {
        val message = RtmpAcknowledgementMessage()
        message.sequence = 0x0102_0304u
        assertContentEquals(byteArrayOf(0x01, 0x02, 0x03, 0x04), message.encoded)
    }

    @Test
    fun acknowledgementDecode() {
        val sent = RtmpAcknowledgementMessage()
        sent.sequence = 0xFFFF_FFFEu
        val message = assertNotNull(transfer(sent) as? RtmpAcknowledgementMessage)
        assertEquals(0xFFFF_FFFEu, message.sequence)
    }

    @Test
    fun abortEncode() {
        val message = RtmpAbortMessage()
        message.chunkStreamId = 5u
        assertContentEquals(byteArrayOf(0x00, 0x00, 0x00, 0x05), message.encoded)
    }

    @Test
    fun abortDecode() {
        val sent = RtmpAbortMessage()
        sent.chunkStreamId = 9u
        val message = assertNotNull(transfer(sent) as? RtmpAbortMessage)
        assertEquals(9u, message.chunkStreamId)
    }

    @Test
    fun setPeerBandwidthEncode() {
        assertContentEquals(
            byteArrayOf(0x00, 0x00, 0x03, 0xE8.toByte(), 0x02),
            RtmpSetPeerBandwidthMessage(
                size = 1000u,
                limit = RtmpSetPeerBandwidthMessage.Limit.`dynamic`,
            ).encoded,
        )
    }

    @Test
    fun setPeerBandwidthDecode() {
        val message = assertNotNull(
            transfer(
                RtmpSetPeerBandwidthMessage(
                    size = 59_768_832u,
                    limit = RtmpSetPeerBandwidthMessage.Limit.soft,
                ),
            ) as? RtmpSetPeerBandwidthMessage,
        )
        assertEquals(59_768_832u, message.size)
        assertEquals(RtmpSetPeerBandwidthMessage.Limit.soft, message.limit)
    }

    @Test
    fun setPeerBandwidthDecodeUnknownLimit() {
        val message = assertNotNull(
            transfer(
                type = RtmpMessageType.bandwidth,
                payload = byteArrayOf(0x00, 0x00, 0x03, 0xE8.toByte(), 0x09),
            ) as? RtmpSetPeerBandwidthMessage,
        )
        assertEquals(1000u, message.size)
        assertEquals(RtmpSetPeerBandwidthMessage.Limit.unknown, message.limit)
    }

    @Test
    fun userControlEncode() {
        assertContentEquals(
            byteArrayOf(0x00, 0x07, 0x00, 0x00, 0x04, 0xD2.toByte()),
            RtmpUserControlMessage(
                event = RtmpUserControlMessage.Event.pong,
                value = 1234,
            ).encoded,
        )
    }

    @Test
    fun userControlDecode() {
        val message = assertNotNull(
            transfer(
                RtmpUserControlMessage(
                    event = RtmpUserControlMessage.Event.ping,
                    value = -2,
                ),
            ) as? RtmpUserControlMessage,
        )
        assertEquals(RtmpUserControlMessage.Event.ping, message.event)
        assertEquals(-2, message.value)
    }

    @Test
    fun userControlDecodeUnknownEvent() {
        val message = assertNotNull(
            transfer(
                type = RtmpMessageType.user,
                payload = byteArrayOf(0x00, 0x42, 0x00, 0x00, 0x00, 0x01),
            ) as? RtmpUserControlMessage,
        )
        assertEquals(RtmpUserControlMessage.Event.unknown, message.event)
        assertEquals(1, message.value)
    }

    @Test
    fun audioDecode() {
        val message = assertNotNull(
            transfer(
                RtmpAudioMessage(
                    streamId = 1u,
                    timestamp = 2u,
                    payload = byteArrayOf(3, 4),
                ),
            ),
        )
        assertTrue(message is RtmpAudioMessage)
        assertEquals(1u, message.streamId)
        assertEquals(2u, message.timestamp)
        assertContentEquals(byteArrayOf(3, 4), message.encoded)
    }

    @Test
    fun videoDecode() {
        val message = assertNotNull(
            transfer(
                RtmpVideoMessage(
                    streamId = 1u,
                    timestamp = 2u,
                    payload = byteArrayOf(3, 4),
                ),
            ),
        )
        assertTrue(message is RtmpVideoMessage)
        assertContentEquals(byteArrayOf(3, 4), message.encoded)
    }

    @Test
    fun aggregateDecode() {
        val message = assertNotNull(
            transfer(type = RtmpMessageType.aggregate, payload = byteArrayOf(1, 2, 3)),
        )
        assertTrue(message is RtmpAggregateMessage)
        assertContentEquals(byteArrayOf(1, 2, 3), message.encoded)
    }

    @Test
    fun commandEncode() {
        val message = RtmpCommandMessage(
            streamId = 0u,
            transactionId = 2,
            commandType = RtmpMessageType.amf0Command,
            commandName = RtmpCommandName.createStream,
            commandObject = null,
            arguments = emptyList(),
        )
        val encoder = Amf0Encoder()
        encoder.encode(AsValue.String("createStream"))
        encoder.encode(AsValue.Number(2.0))
        encoder.encode(AsValue.Null)
        assertContentEquals(encoder.data, message.encoded)
    }

    @Test
    fun commandDecode() {
        val message = assertNotNull(
            transfer(
                RtmpCommandMessage(
                    streamId = 1u,
                    transactionId = 4,
                    commandType = RtmpMessageType.amf0Command,
                    commandName = RtmpCommandName.onStatus,
                    commandObject = mapOf<String, AsValue>("a" to AsValue.Number(1.0)),
                    arguments = listOf(
                        AsValue.String("x"),
                        AsValue.Null,
                        AsValue.Object(mapOf<String, AsValue>("code" to AsValue.String("NetStream.Publish.Start"))),
                    ),
                ),
            ) as? RtmpCommandMessage,
        )
        assertEquals(RtmpMessageType.amf0Command, message.type)
        assertEquals(1u, message.streamId)
        assertEquals(RtmpCommandName.onStatus, message.commandName)
        assertEquals(4, message.transactionId)
        assertEquals(mapOf<String, AsValue>("a" to AsValue.Number(1.0)), message.commandObject)
        assertEquals(
            listOf<AsValue>(
                AsValue.String("x"),
                AsValue.Null,
                AsValue.Object(mapOf<String, AsValue>("code" to AsValue.String("NetStream.Publish.Start"))),
            ),
            message.arguments,
        )
    }

    @Test
    fun amf3CommandEncodeStartsWithZero() {
        val message = RtmpCommandMessage(
            streamId = 0u,
            transactionId = 1,
            commandType = RtmpMessageType.amf3Command,
            commandName = RtmpCommandName.connect,
            commandObject = null,
            arguments = emptyList(),
        )
        assertEquals(0.toByte(), message.encoded.first())
    }

    @Test
    fun amf3CommandDecode() {
        val message = assertNotNull(
            transfer(
                RtmpCommandMessage(
                    streamId = 0u,
                    transactionId = 3,
                    commandType = RtmpMessageType.amf3Command,
                    commandName = RtmpCommandName.result,
                    commandObject = null,
                    arguments = listOf<AsValue>(AsValue.Number(5.0)),
                ),
            ) as? RtmpCommandMessage,
        )
        assertEquals(RtmpMessageType.amf3Command, message.type)
        assertEquals(RtmpCommandName.result, message.commandName)
        assertEquals(3, message.transactionId)
        assertEquals(emptyMap<String, AsValue>(), message.commandObject)
        assertEquals(listOf<AsValue>(AsValue.Number(5.0)), message.arguments)
    }

    @Test
    fun commandDecodeUnknownName() {
        val encoder = Amf0Encoder()
        encoder.encode(AsValue.String("foo"))
        encoder.encode(AsValue.Number(7.0))
        encoder.encode(AsValue.Null)
        val message = assertNotNull(
            transfer(type = RtmpMessageType.amf0Command, payload = encoder.data) as? RtmpCommandMessage,
        )
        assertEquals(RtmpCommandName.unknown, message.commandName)
        assertEquals(7, message.transactionId)
    }

    @Test
    fun commandDecodeMalformed() {
        val message = assertNotNull(
            transfer(
                type = RtmpMessageType.amf0Command,
                payload = byteArrayOf(0x02, 0x00),
            ) as? RtmpCommandMessage,
        )
        assertEquals(RtmpCommandName.unknown, message.commandName)
        assertNull(message.commandObject)
        assertTrue(message.arguments.isEmpty())
    }

    @Test
    fun dataEncode() {
        val message = RtmpDataMessage(
            streamId = 1u,
            dataType = RtmpMessageType.amf0Data,
            timestamp = 0u,
            handlerName = "@setDataFrame",
            arguments = listOf<AsValue>(AsValue.String("onMetaData")),
        )
        val encoder = Amf0Encoder()
        encoder.encode(AsValue.String("@setDataFrame"))
        encoder.encode(AsValue.String("onMetaData"))
        assertContentEquals(encoder.data, message.encoded)
    }

    @Test
    fun dataDecode() {
        val message = assertNotNull(
            transfer(
                RtmpDataMessage(
                    streamId = 1u,
                    dataType = RtmpMessageType.amf0Data,
                    timestamp = 9u,
                    handlerName = "@setDataFrame",
                    arguments = listOf<AsValue>(
                        AsValue.String("onMetaData"),
                        AsValue.Object(mapOf<String, AsValue>("width" to AsValue.Number(1280.0))),
                    ),
                ),
            ) as? RtmpDataMessage,
        )
        assertEquals(RtmpMessageType.amf0Data, message.type)
        assertEquals(9u, message.timestamp)
        assertEquals("@setDataFrame", message.handlerName)
        assertEquals(
            listOf<AsValue>(
                AsValue.String("onMetaData"),
                AsValue.Object(mapOf<String, AsValue>("width" to AsValue.Number(1280.0))),
            ),
            message.arguments,
        )
    }

    @Test
    fun amf3DataDecode() {
        val message = assertNotNull(
            transfer(
                RtmpDataMessage(
                    streamId = 1u,
                    dataType = RtmpMessageType.amf3Data,
                    timestamp = 0u,
                    handlerName = "onFoo",
                    arguments = listOf<AsValue>(AsValue.Bool(true)),
                ),
            ) as? RtmpDataMessage,
        )
        assertEquals(RtmpMessageType.amf3Data, message.type)
        assertEquals("onFoo", message.handlerName)
        assertEquals(listOf<AsValue>(AsValue.Bool(true)), message.arguments)
    }
}

private fun transfer(message: RtmpMessage): RtmpMessage? {
    val chunks = RtmpChunk(RtmpChunkType.zero, 3.toUShort(), message).split(maximumSize = 128)
    return read(data = joinChunks(chunks))
}

private fun transfer(type: RtmpMessageType, payload: ByteArray): RtmpMessage? {
    val message = RtmpMessage(type)
    message.encoded = payload
    return transfer(message)
}

private fun read(data: ByteArray): RtmpMessage? {
    val messages = mutableListOf<RtmpMessage>()
    val remaining = RtmpChunkReader().read(data = data) { messages.add(it) }
    assertTrue(remaining.isEmpty())
    assertEquals(1, messages.size)
    return messages.firstOrNull()
}

private fun joinChunks(chunks: List<ByteArray>): ByteArray {
    val data = ByteArray(chunks.sumOf { it.size })
    var offset = 0
    for (chunk in chunks) {
        chunk.copyInto(data, destinationOffset = offset)
        offset += chunk.size
    }
    return data
}

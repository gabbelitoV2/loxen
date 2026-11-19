package com.moblin.android.media.haishinkit.rtmp

import android.util.Log
import com.moblin.android.MessageQueue
import com.moblin.android.media.haishinkit.media.Processor
import com.moblin.android.media.haishinkit.mpeg.avc.MpegTsVideoConfigAvc
import com.moblin.android.media.haishinkit.rtmp.RtmpAcknowledgementMessage
import com.moblin.android.media.haishinkit.rtmp.RtmpAudioMessage
import com.moblin.android.media.haishinkit.rtmp.RtmpCommandMessage
import com.moblin.android.media.haishinkit.rtmp.RtmpCommandName
import com.moblin.android.media.haishinkit.rtmp.RtmpDataMessage
import com.moblin.android.media.haishinkit.rtmp.RtmpMessage
import com.moblin.android.media.haishinkit.rtmp.RtmpMessageType
import com.moblin.android.media.haishinkit.rtmp.RtmpSetChunkSizeMessage
import com.moblin.android.media.haishinkit.rtmp.RtmpSetPeerBandwidthMessage
import com.moblin.android.media.haishinkit.rtmp.RtmpUserControlMessage
import com.moblin.android.media.haishinkit.rtmp.RtmpWindowAcknowledgementSizeMessage
import com.moblin.android.platform.core.ContinuousClock
import com.moblin.android.various.settings.SettingsStreamColorRange
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import com.moblin.android.media.haishinkit.rtmp.amf.AsValue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.time.Duration.Companion.seconds

private val rtmpQueue: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1)

private class RtmpServerMock {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val lock = Any()
    private val listener: ServerSocket
    private var client: Socket? = null
    private var requestedInputCount: Int? = null
    private var inputData = ByteArray(0)
    private val input = MessageQueue<ByteArray>()
    private val localPort = MessageQueue<UShort>()

    init {
        val serverSocket = ServerSocket(0)
        serverSocket.reuseAddress = true
        listener = serverSocket
        localPort.put(serverSocket.localPort.toUShort())
        scope.launch {
            try {
                val connection = serverSocket.accept()
                handlenNewConnectionHandler(connection)
            } catch (e: IOException) {
                Log.e("RtmpServerMock", "accept failed", e)
            }
        }
    }

    suspend fun getLocalPort(): UShort {
        return localPort.get()
    }

    suspend fun receive(count: Int): ByteArray {
        synchronized(lock) {
            requestedInputCount = count
            tryGetData()?.let { return it }
        }
        return input.get()
    }

    fun send(chunk: RtmpChunk, maximumSize: Int = 128) {
        for (part in chunk.split(maximumSize = maximumSize)) {
            send(data = part)
        }
    }

    fun closeClient() {
        val socket = client ?: return
        try {
            socket.shutdownOutput()
        } catch (e: IOException) {
            Log.e("RtmpServerMock", "close failed", e)
        }
    }

    fun send(data: ByteArray) {
        val socket = client ?: return
        try {
            socket.getOutputStream().write(data)
            socket.getOutputStream().flush()
        } catch (e: IOException) {
            Log.e("RtmpServerMock", "send failed", e)
        }
    }

    suspend fun handlenNewConnectionHandler(connection: Socket) {
        client = connection
        handleData(ByteArray(0))
        receiveLoop(connection)
    }

    private fun handleData(data: ByteArray?) {
        if (data == null) {
            return
        }
        synchronized(lock) {
            inputData += data
            tryGetData()?.let { input.put(it) }
        }
    }

    private suspend fun receiveLoop(connection: Socket) {
        val stream = connection.getInputStream()
        val buffer = ByteArray(4096)
        while (true) {
            val count = try {
                stream.read(buffer)
            } catch (e: IOException) {
                -1
            }
            if (count < 0) {
                break
            }
            handleData(buffer.copyOfRange(0, count))
        }
    }

    private fun tryGetData(): ByteArray? {
        val requested = requestedInputCount ?: return null
        if (inputData.size < requested) {
            return null
        }
        val data = inputData.copyOfRange(0, requested)
        inputData = inputData.copyOfRange(requested, inputData.size)
        requestedInputCount = null
        return data
    }
}

@RunWith(RobolectricTestRunner::class)
class RtmpStreamSuite {
    @Test
    fun basic() {
        runBlocking {
            val streamKey = "5"
            val modelMock = RtmpModelMock()
            val processor = Processor(delegate = modelMock, colorRange = SettingsStreamColorRange.full)
            val server = RtmpServerMock()
            val rtmpStream = RtmpStream(
                name = "test",
                processor = processor,
                delegate = modelMock,
                queue = rtmpQueue
            )
            rtmpStream.setUrl("rtmp://127.0.0.1:${server.getLocalPort()}/live/$streamKey")
            rtmpStream.connect()
            val c0c1 = receiveC0C1(server)
            assertEquals(RtmpHandshake.protocolVersion.toLong(), c0c1[0].toLong())
            sendS0S1(server)
            receiveC2(server)
            sendS2(server)
            expectConnectCommandMessage(server)
            sendWindowAcknowledgementSize(server, 3.toUShort(), 256u)
            sendSetPeerBandwidth(server, 3.toUShort(), 1000u)
            expectWindowAcknowledgementSize(server)
            server.send(
                chunk = RtmpChunk(
                    type = RtmpChunkType.zero,
                    chunkStreamId = 3.toUShort(),
                    message = RtmpCommandMessage(
                        streamId = 0u,
                        transactionId = 2,
                        commandType = RtmpMessageType.amf0Command,
                        commandName = RtmpCommandName.result,
                        commandObject = null,
                        arguments = listOf(
                            AsValue.Object(
                                mapOf(
                                    "level" to AsValue.String("status"),
                                    "code" to AsValue.String("NetConnection.Connect.Success"),
                                    "description" to AsValue.String("Connection succeeded.")
                                )
                            )
                        )
                    )
                )
            )
            val setChunkSize = receiveSetChunkSize(server)
            assertEquals(8192L, setChunkSize.size.toLong())
            var message = receiveCommandMessage(server, 42)
            assertEquals(RtmpCommandName.releaseStream, message.commandName)
            assertEquals(1, message.arguments.size)
            assertEquals(AsValue.String(streamKey), message.arguments[0])
            message = receiveCommandMessage(server, 38)
            assertEquals(RtmpCommandName.fcPublish, message.commandName)
            assertEquals(1, message.arguments.size)
            assertEquals(AsValue.String(streamKey), message.arguments[0])
            message = receiveCommandMessage(server, 37)
            assertEquals(RtmpCommandName.createStream, message.commandName)
            assertEquals(0, message.arguments.size)
            server.send(
                chunk = RtmpChunk(
                    type = RtmpChunkType.zero,
                    chunkStreamId = 3.toUShort(),
                    message = RtmpCommandMessage(
                        streamId = 0u,
                        transactionId = message.transactionId,
                        commandType = RtmpMessageType.amf0Command,
                        commandName = RtmpCommandName.result,
                        commandObject = null,
                        arguments = listOf(AsValue.Number(1.0))
                    )
                )
            )
            message = receiveCommandMessage(server, 43)
            assertEquals(RtmpCommandName.publish, message.commandName)
            assertEquals(2, message.arguments.size)
            assertEquals(AsValue.String(streamKey), message.arguments[0])
            assertEquals(AsValue.String("live"), message.arguments[1])
            server.send(
                chunk = RtmpChunk(
                    type = RtmpChunkType.zero,
                    chunkStreamId = 3.toUShort(),
                    message = RtmpCommandMessage(
                        streamId = 0u,
                        transactionId = message.transactionId,
                        commandType = RtmpMessageType.amf0Command,
                        commandName = RtmpCommandName.onStatus,
                        commandObject = null,
                        arguments = listOf(
                            AsValue.Object(
                                mapOf(
                                    "level" to AsValue.String("status"),
                                    "code" to AsValue.String("NetStream.Publish.Start"),
                                    "description" to AsValue.String("Start publishing.")
                                )
                            )
                        )
                    )
                )
            )
            modelMock.waitForConnected()
            rtmpStream.disconnect()
            server.receive(count = 192)
            message = receiveCommandMessage(server, 40)
            assertEquals(RtmpCommandName.fcUnpublish, message.commandName)
            assertEquals(1, message.arguments.size)
            assertEquals(AsValue.String(streamKey), message.arguments[0])
            message = receiveCommandMessage(server, 46)
            assertEquals(RtmpCommandName.deleteStream, message.commandName)
            assertEquals(1, message.arguments.size)
            assertEquals(AsValue.Number(1.0), message.arguments[0])
            message = receiveCommandMessage(server, 45)
            assertEquals(RtmpCommandName.closeStream, message.commandName)
            assertEquals(1, message.arguments.size)
            assertEquals(AsValue.Number(1.0), message.arguments[0])
        }
    }

    @Test
    fun youTube() {
        runBlocking {
            val streamKey = "5"
            val modelMock = RtmpModelMock()
            val processor = Processor(delegate = modelMock, colorRange = SettingsStreamColorRange.full)
            val server = RtmpServerMock()
            val rtmpStream = RtmpStream(
                name = "test",
                processor = processor,
                delegate = modelMock,
                queue = rtmpQueue
            )
            rtmpStream.setUrl("rtmp://127.0.0.1:${server.getLocalPort()}/live/$streamKey")
            rtmpStream.connect()
            val c0c1 = receiveC0C1(server)
            assertEquals(RtmpHandshake.protocolVersion.toLong(), c0c1[0].toLong())
            sendS0S1(server)
            receiveC2(server)
            sendS2(server)
            expectConnectCommandMessage(server)
            sendWindowAcknowledgementSize(server, 2.toUShort(), 2_500_000u)
            sendSetPeerBandwidth(server, 2.toUShort(), 59_768_832u)
            expectWindowAcknowledgementSize(server)
            server.send(
                chunk = RtmpChunk(
                    type = RtmpChunkType.zero,
                    chunkStreamId = 3.toUShort(),
                    message = RtmpCommandMessage(
                        streamId = 0u,
                        transactionId = 2,
                        commandType = RtmpMessageType.amf0Command,
                        commandName = RtmpCommandName.result,
                        commandObject = mutableMapOf(
                            "fmsVer" to AsValue.String("FMS/3,5,3,824"),
                            "capabilities" to AsValue.Number(127.0),
                            "mode" to AsValue.Number(1.0)
                        ),
                        arguments = listOf(
                            AsValue.Object(
                                mapOf(
                                    "level" to AsValue.String("status"),
                                    "code" to AsValue.String("NetConnection.Connect.Success"),
                                    "description" to AsValue.String("Connection succeeded."),
                                    "objectEncoding" to AsValue.Number(0.0)
                                )
                            )
                        )
                    )
                )
            )
            val setChunkSize = receiveSetChunkSize(server)
            assertEquals(8192L, setChunkSize.size.toLong())
            var message = receiveCommandMessage(server, 42)
            assertEquals(RtmpCommandName.releaseStream, message.commandName)
            assertEquals(1, message.arguments.size)
            assertEquals(AsValue.String(streamKey), message.arguments[0])
            message = receiveCommandMessage(server, 38)
            assertEquals(RtmpCommandName.fcPublish, message.commandName)
            assertEquals(1, message.arguments.size)
            assertEquals(AsValue.String(streamKey), message.arguments[0])
            message = receiveCommandMessage(server, 37)
            assertEquals(RtmpCommandName.createStream, message.commandName)
            assertEquals(0, message.arguments.size)
            server.send(
                chunk = RtmpChunk(
                    type = RtmpChunkType.zero,
                    chunkStreamId = 3.toUShort(),
                    message = RtmpCommandMessage(
                        streamId = 0u,
                        transactionId = message.transactionId,
                        commandType = RtmpMessageType.amf0Command,
                        commandName = RtmpCommandName.result,
                        commandObject = null,
                        arguments = listOf(AsValue.Number(1.0))
                    )
                )
            )
            message = receiveCommandMessage(server, 43)
            assertEquals(RtmpCommandName.publish, message.commandName)
            assertEquals(2, message.arguments.size)
            assertEquals(AsValue.String(streamKey), message.arguments[0])
            assertEquals(AsValue.String("live"), message.arguments[1])
            server.send(
                chunk = RtmpChunk(
                    type = RtmpChunkType.zero,
                    chunkStreamId = 3.toUShort(),
                    message = RtmpCommandMessage(
                        streamId = 0u,
                        transactionId = message.transactionId,
                        commandType = RtmpMessageType.amf0Command,
                        commandName = RtmpCommandName.onStatus,
                        commandObject = null,
                        arguments = listOf(
                            AsValue.Object(
                                mapOf(
                                    "level" to AsValue.String("status"),
                                    "code" to AsValue.String("NetStream.Publish.Start"),
                                    "description" to AsValue.String("Start publishing.")
                                )
                            )
                        )
                    )
                )
            )
            modelMock.waitForConnected()
            rtmpStream.disconnect()
            server.receive(count = 192)
            message = receiveCommandMessage(server, 40)
            assertEquals(RtmpCommandName.fcUnpublish, message.commandName)
            assertEquals(1, message.arguments.size)
            assertEquals(AsValue.String(streamKey), message.arguments[0])
            message = receiveCommandMessage(server, 46)
            assertEquals(RtmpCommandName.deleteStream, message.commandName)
            assertEquals(1, message.arguments.size)
            assertEquals(AsValue.Number(1.0), message.arguments[0])
            message = receiveCommandMessage(server, 45)
            assertEquals(RtmpCommandName.closeStream, message.commandName)
            assertEquals(1, message.arguments.size)
            assertEquals(AsValue.Number(1.0), message.arguments[0])
        }
    }

    @Test
    fun acknowledgementChunkSplitOverTwoReads() {
        val modelMock = RtmpModelMock()
        val processor = Processor(delegate = modelMock, colorRange = SettingsStreamColorRange.full)
        val rtmpStream = RtmpStream(
            name = "test",
            processor = processor,
            delegate = modelMock,
            queue = rtmpQueue
        )
        rtmpStream.info.onWritten(sequence = 14000)
        assertEquals(10, rtmpStream.info.stats.value.packetsInFlight.toInt())
        val typeZeroChunk = byteArrayOf(0x02, 0, 0, 0, 0, 0, 4, 0x03, 0, 0, 0, 0) + 1u.toBigEndianBytes()
        assertTrue(rtmpStream.socketDataReceived(rtmpStream.socket, data = typeZeroChunk).isEmpty())
        assertEquals(10, rtmpStream.info.stats.value.packetsInFlight.toInt())
        val typeThreeChunk = byteArrayOf(0xC2.toByte()) + 14001u.toBigEndianBytes()
        val buffered = rtmpStream.socketDataReceived(rtmpStream.socket, data = typeThreeChunk.copyOfRange(0, 3))
        assertContentEquals(typeThreeChunk.copyOfRange(0, 3), buffered)
        assertTrue(
            rtmpStream.socketDataReceived(
                rtmpStream.socket,
                data = buffered + typeThreeChunk.copyOfRange(typeThreeChunk.size - 2, typeThreeChunk.size)
            ).isEmpty()
        )
        assertEquals(0, rtmpStream.info.stats.value.packetsInFlight.toInt())
    }

    @Test
    fun typeZeroChunk() {
        val data = byteArrayOf(0x04, 0x01, 0x02, 0x03, 0, 0, 5, 0x08, 9, 0, 0, 0) + byteArrayOf(1, 2, 3, 4, 5)
        val message = assertNotNull(readMessage(data))
        assertEquals(RtmpMessageType.audio, message.type)
        assertEquals(0x010203L, message.timestamp.toLong())
        assertEquals(5L, message.length.toLong())
        assertEquals(9L, message.streamId.toLong())
        assertContentEquals(byteArrayOf(1, 2, 3, 4, 5), message.encoded)
    }

    @Test
    fun typeZeroChunkWithExtendedTimestamp() {
        val header = byteArrayOf(0x04, 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0, 0, 1, 0x08, 9, 0, 0, 0)
        val message = assertNotNull(readMessage(header + byteArrayOf(1, 2, 3, 4) + byteArrayOf(7)))
        assertEquals(0x0102_0304L, message.timestamp.toLong())
        assertContentEquals(byteArrayOf(7), message.encoded)
    }

    @Test
    fun typeOneChunk() {
        val typeZero = byteArrayOf(0x05, 0, 0, 100, 0, 0, 4, 0x03, 7, 0, 0, 0) + 1u.toBigEndianBytes()
        val typeOne = byteArrayOf(0x45, 0, 0, 20, 0, 0, 8, 0x08) + byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        val messages = readMessages(typeZero + typeOne)
        assertEquals(2, messages.size)
        val message = messages.last()
        assertEquals(RtmpMessageType.audio, message.type)
        assertEquals(120L, message.timestamp.toLong())
        assertEquals(8L, message.length.toLong())
        assertEquals(7L, message.streamId.toLong())
        assertContentEquals(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8), message.encoded)
    }

    @Test
    fun typeTwoChunk() {
        val typeZero = byteArrayOf(0x06, 0, 0, 50, 0, 0, 8, 0x08, 3, 0, 0, 0) + byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        val typeTwo = byteArrayOf(0x86.toByte(), 0, 0, 7) + byteArrayOf(9, 10, 11, 12, 13, 14, 15, 16)
        val messages = readMessages(typeZero + typeTwo)
        assertEquals(2, messages.size)
        val message = messages.last()
        assertEquals(RtmpMessageType.audio, message.type)
        assertEquals(57L, message.timestamp.toLong())
        assertEquals(8L, message.length.toLong())
        assertEquals(3L, message.streamId.toLong())
        assertContentEquals(byteArrayOf(9, 10, 11, 12, 13, 14, 15, 16), message.encoded)
    }

    @Test
    fun typeThreeChunkStartsNewMessage() {
        var data = byteArrayOf(0x02, 0, 0, 10, 0, 0, 4, 0x03, 0, 0, 0, 0) + 1u.toBigEndianBytes()
        data += byteArrayOf(0x82.toByte(), 0, 0, 5) + 2u.toBigEndianBytes()
        data += byteArrayOf(0xC2.toByte()) + 3u.toBigEndianBytes()
        val messages = readMessages(data).mapNotNull { it as? RtmpAcknowledgementMessage }
        assertEquals(listOf(1L, 2L, 3L), messages.map { it.sequence.toLong() })
        assertEquals(listOf(10L, 15L, 20L), messages.map { it.timestamp.toLong() })
    }

    @Test
    fun twoByteBasicHeader() {
        assertContentEquals(byteArrayOf(0x00, 0x88.toByte()), RtmpChunkType.zero.toBasicHeader(200.toUShort()))
        var data = byteArrayOf(0x02, 0, 0, 10, 0, 0, 4, 0x03, 0, 0, 0, 0) + 1u.toBigEndianBytes()
        data += byteArrayOf(0x00, 0x02, 0, 0, 20, 0, 0, 8, 0x08, 0, 0, 0, 0) + byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        data += byteArrayOf(0xC2.toByte()) + 2u.toBigEndianBytes()
        data += byteArrayOf(0xC0.toByte(), 0x02) + byteArrayOf(9, 10, 11, 12, 13, 14, 15, 16)
        val messages = readMessages(data)
        assertEquals(
            listOf(RtmpMessageType.ack, RtmpMessageType.audio, RtmpMessageType.ack, RtmpMessageType.audio),
            messages.map { it.type }
        )
        assertEquals(listOf(4L, 8L, 4L, 8L), messages.map { it.length.toLong() })
        assertContentEquals(byteArrayOf(9, 10, 11, 12, 13, 14, 15, 16), messages.last().encoded)
    }

    @Test
    fun threeByteBasicHeader() {
        assertContentEquals(byteArrayOf(0x01, 0x50, 0x01), RtmpChunkType.zero.toBasicHeader(400.toUShort()))
        var data = byteArrayOf(0x01, 0x02, 0x00, 0, 0, 10, 0, 0, 4, 0x03, 0, 0, 0, 0)
        data += 1u.toBigEndianBytes()
        data += byteArrayOf(0xC0.toByte(), 0x02) + 2u.toBigEndianBytes()
        val messages = readMessages(data).mapNotNull { it as? RtmpAcknowledgementMessage }
        assertEquals(listOf(1L, 2L), messages.map { it.sequence.toLong() })
    }

    @Test
    fun typeThreeChunkContinuesMessage() {
        val payload = ByteArray(300) { (it % 251).toByte() }
        var data = byteArrayOf(0x08, 0, 0, 0, 0, 1, 0x2C, 0x08, 1, 0, 0, 0) + payload.copyOfRange(0, 128)
        data += byteArrayOf(0xC8.toByte()) + payload.copyOfRange(128, 256)
        data += byteArrayOf(0xC8.toByte()) + payload.copyOfRange(256, 300)
        val message = assertNotNull(readMessage(data))
        assertEquals(RtmpMessageType.audio, message.type)
        assertEquals(1L, message.streamId.toLong())
        assertEquals(300L, message.length.toLong())
        assertContentEquals(payload, message.encoded)
    }

    @Test
    fun typeThreeChunkContinuesMessageWithExtendedTimestamp() {
        val payload = ByteArray(300) { (it % 251).toByte() }
        var data = byteArrayOf(0x08, 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0, 1, 0x2C, 0x08, 1, 0, 0, 0)
        data += 0x0100_0000u.toBigEndianBytes() + payload.copyOfRange(0, 128)
        data += byteArrayOf(0xC8.toByte()) + 0x0100_0000u.toBigEndianBytes() + payload.copyOfRange(128, 256)
        data += byteArrayOf(0xC8.toByte()) + 0x0100_0000u.toBigEndianBytes() + payload.copyOfRange(256, 300)
        val message = assertNotNull(readMessage(data))
        assertEquals(RtmpMessageType.audio, message.type)
        assertEquals(0x0100_0000L, message.timestamp.toLong())
        assertEquals(300L, message.length.toLong())
        assertContentEquals(payload, message.encoded)
    }

    @Test
    fun typeThreeChunkStartsNewMessageWithExtendedTimestamp() {
        var data = byteArrayOf(0x02, 0, 0, 10, 0, 0, 4, 0x03, 0, 0, 0, 0) + 1u.toBigEndianBytes()
        data += byteArrayOf(0x82.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()) + 0x0100_0000u.toBigEndianBytes()
        data += 2u.toBigEndianBytes()
        data += byteArrayOf(0xC2.toByte()) + 0x0100_0000u.toBigEndianBytes() + 3u.toBigEndianBytes()
        val messages = readMessages(data).mapNotNull { it as? RtmpAcknowledgementMessage }
        assertEquals(listOf(1L, 2L, 3L), messages.map { it.sequence.toLong() })
        assertEquals(listOf(10L, 0x0100_000AL, 0x0200_000AL), messages.map { it.timestamp.toLong() })
    }

    @Test
    fun splitAndReadMessageWithExtendedTimestamp() {
        val payload = ByteArray(300) { (it % 251).toByte() }
        val chunk = RtmpChunk(
            type = RtmpChunkType.zero,
            chunkStreamId = 8.toUShort(),
            message = RtmpAudioMessage(
                streamId = 1u,
                timestamp = 0xFFFFFFu,
                payload = payload
            )
        )
        val message = assertNotNull(readMessage(joinBytes(chunk.split(maximumSize = 128))))
        assertEquals(RtmpMessageType.audio, message.type)
        assertEquals(0xFFFFFFL, message.timestamp.toLong())
        assertEquals(1L, message.streamId.toLong())
        assertContentEquals(payload, message.encoded)
    }

    @Test
    fun chunksDeliveredOneByteAtATime() {
        val reader = RtmpChunkReader()
        var data = byteArrayOf(0x02, 0, 0, 10, 0, 0, 4, 0x03, 0, 0, 0, 0) + 1u.toBigEndianBytes()
        data += byteArrayOf(0x00, 0x02, 0, 0, 10, 0, 0, 4, 0x03, 0, 0, 0, 0) + 2u.toBigEndianBytes()
        data += byteArrayOf(0x01, 0x02, 0x01, 0, 0, 10, 0, 0, 4, 0x03, 0, 0, 0, 0) + 3u.toBigEndianBytes()
        val messages = mutableListOf<RtmpMessage>()
        var buffer = ByteArray(0)
        for (byte in data) {
            buffer = buffer + byteArrayOf(byte)
            buffer = reader.read(data = buffer) { messages.add(it) }
        }
        assertTrue(buffer.isEmpty())
        assertEquals(
            listOf(1L, 2L, 3L),
            messages.mapNotNull { (it as? RtmpAcknowledgementMessage)?.sequence?.toLong() }
        )
    }

    @Test
    fun pingIsAnsweredWithPong() {
        runBlocking {
            val stream = connectAndPublish()
            stream.server.send(
                chunk = RtmpChunk(
                    type = RtmpChunkType.zero,
                    chunkStreamId = 2.toUShort(),
                    message = RtmpUserControlMessage(RtmpUserControlMessage.Event.ping, 1234)
                )
            )
            val (message, raw) = stream.reader.next(stream.server)
            val pong = assertNotNull(message as? RtmpUserControlMessage)
            assertEquals(RtmpUserControlMessage.Event.pong, pong.event)
            assertEquals(1234, pong.value)
            assertEquals(0x02L, raw[0].toLong())
        }
    }

    @Test
    fun serverChunkSizeIsUsedWhenReading() {
        runBlocking {
            val stream = connectAndPublish()
            stream.server.send(
                chunk = RtmpChunk(
                    type = RtmpChunkType.zero,
                    chunkStreamId = 2.toUShort(),
                    message = RtmpSetChunkSizeMessage(4096u)
                )
            )
            sendCommand(
                server = stream.server,
                transactionId = 0,
                commandName = RtmpCommandName.onStatus,
                arguments = listOf(
                    makeStatus(
                        code = "NetConnection.Connect.Failed",
                        description = "x".repeat(300)
                    )
                ),
                maximumSize = 4096,
            )
            assertEquals(RtmpDisconnectReason.connectionLost, stream.modelMock.waitForDisconnected())
        }
    }

    @Test
    fun closeCommandUnpublishes() {
        runBlocking {
            val stream = connectAndPublish()
            sendCommand(
                server = stream.server,
                transactionId = 0,
                commandName = RtmpCommandName.close,
                arguments = emptyList()
            )
            expectUnpublish(stream)
        }
    }

    @Test
    fun connectClosedStatusUnpublishes() {
        runBlocking {
            val stream = connectAndPublish()
            sendCommand(
                server = stream.server,
                transactionId = 0,
                commandName = RtmpCommandName.onStatus,
                arguments = listOf(makeStatus(code = "NetConnection.Connect.Closed"))
            )
            assertEquals(RtmpDisconnectReason.connectionLost, stream.modelMock.waitForDisconnected())
            expectUnpublish(stream)
        }
    }

    @Test
    fun aacAudio() {
        runBlocking {
            val stream = connectAndPublish()
            val format = makeAacFormat()
            stream.rtmpStream.audioEncoderOutputFormat(format)
            var pair = stream.reader.next(stream.server)
            var message = pair.first
            var raw = pair.second
            assertEquals(RtmpMessageType.audio, message.type)
            assertEquals(1L, message.streamId.toLong())
            assertEquals(0L, message.timestamp.toLong())
            assertContentEquals(byteArrayOf(0xAF.toByte(), 0x00, 0x11, 0x90.toByte()), message.encoded)
            assertContentEquals(byteArrayOf(0x08), raw.copyOfRange(0, 1))
            val timestamps = mutableListOf<Long>()
            for ((index, pts) in listOf(10240, 10261, 10282, 10303, 10324, 10345).withIndex()) {
                stream.rtmpStream.audioEncoderOutputBuffer(
                    makeAudioBuffer(format = format, payload = byteArrayOf(index.toByte(), 1, 2)),
                    mediaTime(pts.toLong())
                )
                pair = stream.reader.next(stream.server)
                message = pair.first
                raw = pair.second
                assertEquals(RtmpMessageType.audio, message.type)
                assertContentEquals(
                    byteArrayOf(0xAF.toByte(), 0x01, index.toByte(), 1, 2),
                    message.encoded
                )
                assertContentEquals(byteArrayOf(0x48), raw.copyOfRange(0, 1))
                timestamps.add(message.timestamp.toLong())
            }
            assertEquals(listOf(0L, 20L, 41L, 61L, 82L, 102L), timestamps)
        }
    }

    @Test
    fun audioBuffersBeforeBaseOrOutOfOrderAreDropped() {
        runBlocking {
            val stream = connectAndPublish()
            val format = makeAacFormat()
            for ((index, pts) in listOf(10240, 10200, 10261, 10250, 10282).withIndex()) {
                stream.rtmpStream.audioEncoderOutputBuffer(
                    makeAudioBuffer(format = format, payload = byteArrayOf(index.toByte())),
                    mediaTime(pts.toLong())
                )
            }
            val timestamps = mutableListOf<Long>()
            repeat(3) {
                val message = stream.reader.nextMessage(stream.server)
                assertEquals(RtmpMessageType.audio, message.type)
                timestamps.add(message.timestamp.toLong())
            }
            assertEquals(listOf(0L, 20L, 41L), timestamps)
        }
    }

    @Test
    fun h264Video() {
        runBlocking {
            val stream = connectAndPublish()
            val formatDescription = makeH264FormatDescription()
            val avcC = assertNotNull(MpegTsVideoConfigAvc.getAvcC(formatDescription))
            val encoder = assertNotNull(stream.processor.video.encoder)
            stream.rtmpStream.videoEncoderOutputFormat(encoder, formatDescription)
            var pair = stream.reader.next(stream.server)
            var message = pair.first
            var raw = pair.second
            assertEquals(RtmpMessageType.video, message.type)
            assertEquals(1L, message.streamId.toLong())
            assertEquals(0L, message.timestamp.toLong())
            assertContentEquals(byteArrayOf(0x17, 0x00, 0x00, 0x00, 0x00) + avcC, message.encoded)
            assertContentEquals(byteArrayOf(0x09), raw.copyOfRange(0, 1))
            val timestamps = mutableListOf<Long>()
            for ((index, dts) in listOf(10240, 10272, 10304, 10336, 10368).withIndex()) {
                val isSync = index == 0
                stream.rtmpStream.videoEncoderOutputSampleBuffer(
                    encoder,
                    makeVideoSampleBuffer(
                        formatDescription = formatDescription,
                        decodeTimeStamp = mediaTime(dts.toLong()),
                        presentationTimeStamp = mediaTime((dts + 64).toLong()),
                        isSync = isSync,
                        payload = byteArrayOf(
                            0, 0, 0, 2,
                            (if (isSync) 0x65 else 0x41).toByte(),
                            index.toByte()
                        )
                    ),
                    0L
                )
                pair = stream.reader.next(stream.server)
                message = pair.first
                raw = pair.second
                assertEquals(RtmpMessageType.video, message.type)
                assertContentEquals(
                    byteArrayOf(
                        (if (isSync) 0x17 else 0x27).toByte(), 0x01, 0x00, 0x00, 62,
                        0, 0, 0, 2, (if (isSync) 0x65 else 0x41).toByte(), index.toByte()
                    ),
                    message.encoded
                )
                assertContentEquals(byteArrayOf(0x49), raw.copyOfRange(0, 1))
                timestamps.add(message.timestamp.toLong())
            }
            assertEquals(listOf(0L, 31L, 62L, 93L, 125L), timestamps)
        }
    }

    @Test
    fun audioAndVideoShareBaseTimestamp() {
        runBlocking {
            val stream = connectAndPublish()
            val formatDescription = makeH264FormatDescription()
            stream.rtmpStream.videoEncoderOutputSampleBuffer(
                assertNotNull(stream.processor.video.encoder),
                makeVideoSampleBuffer(
                    formatDescription = formatDescription,
                    decodeTimeStamp = mediaTime(10240),
                    presentationTimeStamp = mediaTime(10240),
                    isSync = true,
                    payload = byteArrayOf(0, 0, 0, 1, 0x65)
                ),
                0L
            )
            assertEquals(RtmpMessageType.video, stream.reader.nextMessage(stream.server).type)
            val format = makeAacFormat()
            stream.rtmpStream.audioEncoderOutputBuffer(
                makeAudioBuffer(format = format, payload = byteArrayOf(1)),
                mediaTime(10200)
            )
            stream.rtmpStream.audioEncoderOutputBuffer(
                makeAudioBuffer(format = format, payload = byteArrayOf(2)),
                mediaTime(10261)
            )
            stream.rtmpStream.audioEncoderOutputBuffer(
                makeAudioBuffer(format = format, payload = byteArrayOf(3)),
                mediaTime(10282)
            )
            val first = stream.reader.nextMessage(stream.server)
            assertEquals(RtmpMessageType.audio, first.type)
            assertEquals(0L, first.timestamp.toLong())
            assertEquals(2L, first.encoded[first.encoded.size - 1].toLong())
            val second = stream.reader.nextMessage(stream.server)
            assertEquals(20L, second.timestamp.toLong())
            assertEquals(3L, second.encoded[second.encoded.size - 1].toLong())
        }
    }

    @Test
    fun serverClosesConnectionAfterPublish() {
        runBlocking {
            val modelMock = RtmpModelMock()
            val processor = Processor(delegate = modelMock, colorRange = SettingsStreamColorRange.full)
            val server = RtmpServerMock()
            val rtmpStream = RtmpStream(name = "test", processor = processor, delegate = modelMock, queue = rtmpQueue)
            val reader = ClientMessageReader()
            rtmpStream.setUrl("rtmp://127.0.0.1:${server.getLocalPort()}/app/bad")
            rtmpStream.connect()
            receiveC0C1(server)
            sendS0S1(server)
            receiveC2(server)
            sendS2(server)
            assertEquals(RtmpCommandName.connect, reader.nextCommand(server).commandName)
            sendCommand(
                server = server,
                transactionId = 1,
                commandName = RtmpCommandName.result,
                arguments = listOf(makeStatus(code = "NetConnection.Connect.Success"))
            )
            assertTrue(reader.nextMessage(server) is RtmpSetChunkSizeMessage)
            assertEquals(RtmpCommandName.releaseStream, reader.nextCommand(server).commandName)
            assertEquals(RtmpCommandName.fcPublish, reader.nextCommand(server).commandName)
            val createStream = reader.nextCommand(server)
            sendCommand(
                server = server,
                transactionId = createStream.transactionId,
                commandName = RtmpCommandName.result,
                arguments = listOf(AsValue.Number(1.0))
            )
            assertEquals(RtmpCommandName.publish, reader.nextCommand(server).commandName)
            val start = ContinuousClock.now
            server.closeClient()
            assertEquals(RtmpDisconnectReason.streamRejected, modelMock.waitForDisconnected())
            assertTrue(start.duration(to = ContinuousClock.now) < 5.seconds)
        }
    }

    @Test
    fun serverReportsPublishErrorAndClosesConnection() {
        runBlocking {
            val modelMock = RtmpModelMock()
            val processor = Processor(delegate = modelMock, colorRange = SettingsStreamColorRange.full)
            val server = RtmpServerMock()
            val rtmpStream = RtmpStream(name = "test", processor = processor, delegate = modelMock, queue = rtmpQueue)
            val reader = ClientMessageReader()
            rtmpStream.setUrl("rtmp://127.0.0.1:${server.getLocalPort()}/app/bad")
            rtmpStream.connect()
            receiveC0C1(server)
            sendS0S1(server)
            receiveC2(server)
            sendS2(server)
            assertEquals(RtmpCommandName.connect, reader.nextCommand(server).commandName)
            sendCommand(
                server = server,
                transactionId = 1,
                commandName = RtmpCommandName.result,
                arguments = listOf(makeStatus(code = "NetConnection.Connect.Success"))
            )
            assertTrue(reader.nextMessage(server) is RtmpSetChunkSizeMessage)
            assertEquals(RtmpCommandName.releaseStream, reader.nextCommand(server).commandName)
            assertEquals(RtmpCommandName.fcPublish, reader.nextCommand(server).commandName)
            val createStream = reader.nextCommand(server)
            sendCommand(
                server = server,
                transactionId = createStream.transactionId,
                commandName = RtmpCommandName.result,
                arguments = listOf(AsValue.Number(1.0))
            )
            assertEquals(RtmpCommandName.publish, reader.nextCommand(server).commandName)
            sendCommand(
                server = server,
                transactionId = 0,
                commandName = RtmpCommandName.onStatus,
                arguments = listOf(
                    makeStatus(
                        code = "NetStream.Publish.BadName",
                        description = "Bad stream key.",
                        level = "error"
                    )
                )
            )
            server.closeClient()
            assertEquals(
                RtmpDisconnectReason.serverError("NetStream.Publish.BadName", "Bad stream key."),
                modelMock.waitForDisconnected()
            )
        }
    }

    @Test
    fun failedCallIsNotReportedAsDisconnectReason() {
        runBlocking {
            val modelMock = RtmpModelMock()
            val processor = Processor(delegate = modelMock, colorRange = SettingsStreamColorRange.full)
            val server = RtmpServerMock()
            val rtmpStream = RtmpStream(name = "test", processor = processor, delegate = modelMock, queue = rtmpQueue)
            val reader = ClientMessageReader()
            rtmpStream.setUrl("rtmp://127.0.0.1:${server.getLocalPort()}/app/bad")
            rtmpStream.connect()
            receiveC0C1(server)
            sendS0S1(server)
            receiveC2(server)
            sendS2(server)
            assertEquals(RtmpCommandName.connect, reader.nextCommand(server).commandName)
            sendCommand(
                server = server,
                transactionId = 1,
                commandName = RtmpCommandName.result,
                arguments = listOf(makeStatus(code = "NetConnection.Connect.Success"))
            )
            assertTrue(reader.nextMessage(server) is RtmpSetChunkSizeMessage)
            val releaseStream = reader.nextCommand(server)
            assertEquals(RtmpCommandName.releaseStream, releaseStream.commandName)
            sendCommand(
                server = server,
                transactionId = releaseStream.transactionId,
                commandName = RtmpCommandName.error,
                arguments = listOf(
                    makeStatus(
                        code = "NetConnection.Call.Failed",
                        description = "Method not found (releaseStream).",
                        level = "error"
                    )
                )
            )
            assertEquals(RtmpCommandName.fcPublish, reader.nextCommand(server).commandName)
            val createStream = reader.nextCommand(server)
            sendCommand(
                server = server,
                transactionId = createStream.transactionId,
                commandName = RtmpCommandName.result,
                arguments = listOf(AsValue.Number(1.0))
            )
            assertEquals(RtmpCommandName.publish, reader.nextCommand(server).commandName)
            server.closeClient()
            assertEquals(RtmpDisconnectReason.streamRejected, modelMock.waitForDisconnected())
        }
    }

    @Test
    fun unreachableServerIsReportedWithoutWaitingForConnectTimeout() {
        runBlocking {
            for (url in listOf(
                "rtmp://127.0.0.1:1/app/key",
                "rtmp://moblin-no-such-host.invalid/app/key"
            )) {
                val modelMock = RtmpModelMock()
                val processor = Processor(delegate = modelMock, colorRange = SettingsStreamColorRange.full)
                val rtmpStream = RtmpStream(
                    name = "test",
                    processor = processor,
                    delegate = modelMock,
                    queue = rtmpQueue
                )
                rtmpStream.setUrl(url)
                val start = ContinuousClock.now
                rtmpStream.connect()
                assertEquals(RtmpDisconnectReason.connectFailed, modelMock.waitForDisconnected())
                assertTrue(start.duration(to = ContinuousClock.now) < 10.seconds)
            }
        }
    }
}

private suspend fun receiveC0C1(server: RtmpServerMock): ByteArray {
    return server.receive(count = RtmpHandshake.sigSize + 1)
}

private suspend fun sendS0S1(server: RtmpServerMock) {
    server.send(data = ByteArray(RtmpHandshake.sigSize + 1))
}

private suspend fun receiveC2(server: RtmpServerMock): ByteArray {
    return server.receive(count = RtmpHandshake.sigSize)
}

private suspend fun sendS2(server: RtmpServerMock) {
    server.send(data = ByteArray(RtmpHandshake.sigSize))
}

private suspend fun sendWindowAcknowledgementSize(server: RtmpServerMock, chunkStreamId: UShort, size: UInt) {
    server.send(
        chunk = RtmpChunk(
            type = RtmpChunkType.zero,
            chunkStreamId = chunkStreamId,
            message = RtmpWindowAcknowledgementSizeMessage(size)
        )
    )
}

private suspend fun sendSetPeerBandwidth(server: RtmpServerMock, chunkStreamId: UShort, size: UInt) {
    server.send(
        chunk = RtmpChunk(
            type = RtmpChunkType.zero,
            chunkStreamId = chunkStreamId,
            message = RtmpSetPeerBandwidthMessage(size = size, limit = RtmpSetPeerBandwidthMessage.Limit.dynamic)
        )
    )
}

private suspend fun expectConnectCommandMessage(server: RtmpServerMock) {
    val data = server.receive(count = 273)
    assertEquals(0x03L, data[0].toLong())
    val message = assertNotNull(readMessage(data) as? RtmpCommandMessage)
    assertEquals(RtmpCommandName.connect, message.commandName)
    assertEquals(1L, message.transactionId.toLong())
    val connectMessage = message.commandObject ?: error("error")
    assertEquals(AsValue.String("live"), connectMessage["app"])
    assertEquals(AsValue.String("FMLE/3.0 (compatible; FMSc/1.0)"), connectMessage["flashVer"])
    assertEquals(AsValue.Null, connectMessage["swfUrl"])
    val tcUrl = (connectMessage["tcUrl"] as? AsValue.String)?.value ?: error("error")
    assertNotNull(Regex("rtmp://127\\.0\\.0\\.1:\\d+/live").matchEntire(tcUrl))
    assertEquals(AsValue.Bool(false), connectMessage["fpad"])
    assertEquals(AsValue.Number(239.0), connectMessage["capabilities"])
    assertEquals(AsValue.Number(0x0400.toDouble()), connectMessage["audioCodecs"])
    assertEquals(AsValue.Number(0x0080.toDouble()), connectMessage["videoCodecs"])
    assertEquals(AsValue.Number(1.0), connectMessage["videoFunction"])
    assertEquals(AsValue.Null, connectMessage["pageUrl"])
    assertEquals(AsValue.Number(0.0), connectMessage["objectEncoding"])
}

private suspend fun expectWindowAcknowledgementSize(server: RtmpServerMock) {
    val data = server.receive(count = 16)
    assertEquals(0x02L, data[0].toLong())
    val message = assertNotNull(readMessage(data) as? RtmpWindowAcknowledgementSizeMessage)
    assertEquals(100_000L, message.size.toLong())
}

private suspend fun receiveCommandMessage(server: RtmpServerMock, size: Int): RtmpCommandMessage {
    val data = server.receive(count = size)
    return assertNotNull(readMessage(data) as? RtmpCommandMessage)
}

private suspend fun receiveSetChunkSize(server: RtmpServerMock): RtmpSetChunkSizeMessage {
    val data = server.receive(count = 16)
    return assertNotNull(readMessage(data) as? RtmpSetChunkSizeMessage)
}

private fun readMessages(data: ByteArray): List<RtmpMessage> {
    val messages = mutableListOf<RtmpMessage>()
    val remaining = RtmpChunkReader().read(data = data) { messages.add(it) }
    assertTrue(remaining.isEmpty())
    return messages
}

private fun readMessage(data: ByteArray): RtmpMessage? {
    val messages = readMessages(data)
    assertEquals(1, messages.size)
    return messages.firstOrNull()
}

private class ClientMessageReader {
    private val reader = RtmpChunkReader()
    private var buffer = ByteArray(0)
    private var raw = ByteArray(0)
    private val messages = mutableListOf<Pair<RtmpMessage, ByteArray>>()

    suspend fun next(server: RtmpServerMock): Pair<RtmpMessage, ByteArray> {
        while (messages.isEmpty()) {
            val data = server.receive(count = 1)
            raw += data
            buffer += data
            buffer = reader.read(data = buffer) { message ->
                messages.add(Pair(message, raw))
                raw = ByteArray(0)
            }
        }
        val pair = messages.removeAt(0)
        val message = pair.first
        if (message is RtmpSetChunkSizeMessage) {
            reader.maximumChunkSize = message.size.toInt()
        }
        return pair
    }

    suspend fun nextMessage(server: RtmpServerMock): RtmpMessage {
        return next(server).first
    }

    suspend fun nextCommand(server: RtmpServerMock): RtmpCommandMessage {
        return assertNotNull(nextMessage(server) as? RtmpCommandMessage)
    }
}

private data class PublishingStream(
    val modelMock: RtmpModelMock,
    val processor: Processor,
    val server: RtmpServerMock,
    val rtmpStream: RtmpStream,
    val reader: ClientMessageReader,
)

private fun makeStatus(code: String, description: String = "", level: String = "status"): AsValue {
    return AsValue.Object(
        mapOf(
            "level" to AsValue.String(level),
            "code" to AsValue.String(code),
            "description" to AsValue.String(description)
        )
    )
}

private suspend fun sendCommand(
    server: RtmpServerMock,
    transactionId: Int,
    commandName: RtmpCommandName,
    arguments: List<AsValue>,
    maximumSize: Int = 128,
) {
    server.send(
        chunk = RtmpChunk(
            type = RtmpChunkType.zero,
            chunkStreamId = 3.toUShort(),
            message = RtmpCommandMessage(
                streamId = 0u,
                transactionId = transactionId,
                commandType = RtmpMessageType.amf0Command,
                commandName = commandName,
                commandObject = null,
                arguments = arguments
            )
        ),
        maximumSize = maximumSize
    )
}

private suspend fun connectAndPublish(): PublishingStream {
    val modelMock = RtmpModelMock()
    val processor = Processor(delegate = modelMock, colorRange = SettingsStreamColorRange.full)
    val server = RtmpServerMock()
    val rtmpStream = RtmpStream(name = "test", processor = processor, delegate = modelMock, queue = rtmpQueue)
    val reader = ClientMessageReader()
    rtmpStream.setUrl("rtmp://127.0.0.1:${server.getLocalPort()}/live/5")
    rtmpStream.connect()
    receiveC0C1(server)
    sendS0S1(server)
    receiveC2(server)
    sendS2(server)
    assertEquals(RtmpCommandName.connect, reader.nextCommand(server).commandName)
    sendWindowAcknowledgementSize(server, 2.toUShort(), 2_500_000u)
    assertTrue(reader.nextMessage(server) is RtmpWindowAcknowledgementSizeMessage)
    sendCommand(
        server = server,
        transactionId = 1,
        commandName = RtmpCommandName.result,
        arguments = listOf(makeStatus(code = "NetConnection.Connect.Success"))
    )
    assertTrue(reader.nextMessage(server) is RtmpSetChunkSizeMessage)
    assertEquals(RtmpCommandName.releaseStream, reader.nextCommand(server).commandName)
    assertEquals(RtmpCommandName.fcPublish, reader.nextCommand(server).commandName)
    val createStream = reader.nextCommand(server)
    assertEquals(RtmpCommandName.createStream, createStream.commandName)
    sendCommand(
        server = server,
        transactionId = createStream.transactionId,
        commandName = RtmpCommandName.result,
        arguments = listOf(AsValue.Number(1.0))
    )
    val publish = reader.nextCommand(server)
    assertEquals(RtmpCommandName.publish, publish.commandName)
    assertEquals(1L, publish.streamId.toLong())
    sendCommand(
        server = server,
        transactionId = 0,
        commandName = RtmpCommandName.onStatus,
        arguments = listOf(makeStatus(code = "NetStream.Publish.Start"))
    )
    modelMock.waitForConnected()
    val setDataFrame = assertNotNull(reader.nextMessage(server) as? RtmpDataMessage)
    assertEquals("@setDataFrame", setDataFrame.handlerName)
    assertEquals(1L, setDataFrame.streamId.toLong())
    return PublishingStream(
        modelMock = modelMock,
        processor = processor,
        server = server,
        rtmpStream = rtmpStream,
        reader = reader,
    )
}

private suspend fun expectUnpublish(stream: PublishingStream) {
    assertEquals(RtmpCommandName.fcUnpublish, stream.reader.nextCommand(stream.server).commandName)
    val deleteStream = stream.reader.nextCommand(stream.server)
    assertEquals(RtmpCommandName.deleteStream, deleteStream.commandName)
    assertEquals(listOf<AsValue>(AsValue.Number(1.0)), deleteStream.arguments)
    val closeStream = stream.reader.nextCommand(stream.server)
    assertEquals(RtmpCommandName.closeStream, closeStream.commandName)
    assertEquals(listOf<AsValue>(AsValue.Number(1.0)), closeStream.arguments)
}

private fun UInt.toBigEndianBytes(): ByteArray = byteArrayOf(
    (this shr 24).toByte(),
    (this shr 16).toByte(),
    (this shr 8).toByte(),
    toByte()
)

private fun joinBytes(chunks: List<ByteArray>): ByteArray {
    val result = ByteArray(chunks.sumOf { it.size })
    var offset = 0
    for (chunk in chunks) {
        chunk.copyInto(result, offset)
        offset += chunk.size
    }
    return result
}

package com.moblin.android.media.haishinkit.rtmp

import android.util.Log
import android.util.Size
import com.moblin.android.MessageQueue
import com.moblin.android.media.haishinkit.media.MediaSample
import com.moblin.android.media.haishinkit.media.Processor
import com.moblin.android.media.haishinkit.media.ProcessorDelegate
import com.moblin.android.media.haishinkit.media.RecorderDataSegment
import com.moblin.android.media.haishinkit.rtmp.message.RtmpAcknowledgementMessage
import com.moblin.android.media.haishinkit.rtmp.message.RtmpAudioMessage
import com.moblin.android.media.haishinkit.rtmp.message.RtmpCommandMessage
import com.moblin.android.media.haishinkit.rtmp.message.RtmpCommandName
import com.moblin.android.media.haishinkit.rtmp.message.RtmpMessage
import com.moblin.android.media.haishinkit.rtmp.message.RtmpMessageType
import com.moblin.android.media.haishinkit.rtmp.message.RtmpSetChunkSizeMessage
import com.moblin.android.media.haishinkit.rtmp.message.RtmpSetPeerBandwidthMessage
import com.moblin.android.media.haishinkit.rtmp.message.RtmpWindowAcknowledgementSizeMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private val rtmpQueue: CoroutineDispatcher = Dispatchers.IO

private class ModelMock : RtmpStreamDelegate, ProcessorDelegate {
    private val status = MessageQueue<String>()
    private val connected = MessageQueue<Unit>()

    suspend fun waitForStatus(): String {
        return status.get()
    }

    suspend fun waitForConnected() {
        connected.get()
    }

    override fun rtmpStreamStatus(stream: RtmpStream, code: String) {
        Log.i("ModelMock", "rtmp-test: Status $code")
        status.put(code)
    }

    override fun rtmpStreamConnected(stream: RtmpStream) {
        Log.i("ModelMock", "rtmp-test: Connected")
        connected.put(Unit)
    }

    override fun streamAudioLevel(audioLevel: Float, numberOfAudioChannels: Int, sampleRate: Double) {
    }

    override fun streamLowFpsImage(lowFpsImage: ByteArray?, frameNumber: ULong) {
    }

    override fun streamVideoAttachCameraError() {
    }

    override fun streamVideoCaptureSessionError(message: String) {
    }

    override fun streamVideoBufferedVideoReady(cameraId: UUID) {
    }

    override fun streamVideoBufferedVideoRemoved(cameraId: UUID) {
    }

    override fun streamVideoFps(fps: Int) {
    }

    override fun streamVideoEncoderResolution(resolution: Size) {
    }

    override fun streamRecorderInitSegment(data: ByteArray) {
    }

    override fun streamRecorderDataSegment(segment: RecorderDataSegment) {
    }

    override fun streamRecorderFinished() {
    }

    override fun streamAudio(sampleBuffer: MediaSample) {
    }

    override fun streamNoTorch() {
    }

    override fun streamSetZoomX(x: Float) {
    }

    override fun streamSetExposureBias(bias: Float) {
    }

    override fun streamSelectedFps(auto: Boolean) {
    }
}

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

    fun send(chunk: RtmpChunk) {
        for (part in chunk.split(maximumSize = 128)) {
            send(data = part)
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

class RtmpStreamSuite {
    @Test
    fun basic() {
        runBlocking {
            val streamKey = "5"
            val modelMock = ModelMock()
            val processor = Processor(delegate = modelMock)
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
            sendWindowAcknowledgementSize(server, 3, 256)
            sendSetPeerBandwidth(server, 3, 1000)
            expectWindowAcknowledgementSize(server)
            server.send(
                chunk = RtmpChunk(
                    type = RtmpChunkType.zero,
                    chunkStreamId = 3,
                    message = RtmpCommandMessage(
                        streamId = 0,
                        transactionId = 2,
                        commandType = RtmpCommandMessage.CommandType.amf0Command,
                        commandName = RtmpCommandName.result,
                        commandObject = null,
                        arguments = listOf(
                            Amf0Value.Object(
                                mapOf(
                                    "level" to Amf0Value.String("status"),
                                    "code" to Amf0Value.String("NetConnection.Connect.Success"),
                                    "description" to Amf0Value.String("Connection succeeded.")
                                )
                            )
                        )
                    )
                )
            )
            val setChunkSize = receiveSetChunkSize(server)
            assertEquals(8192L, setChunkSize.size.toLong())
            assertEquals("NetConnection.Connect.Success", modelMock.waitForStatus())
            var message = receiveCommandMessage(server, 42)
            assertEquals(RtmpCommandName.releaseStream, message.commandName)
            assertEquals(1, message.arguments.size)
            assertEquals(Amf0Value.String(streamKey), message.arguments[0])
            message = receiveCommandMessage(server, 38)
            assertEquals(RtmpCommandName.fcPublish, message.commandName)
            assertEquals(1, message.arguments.size)
            assertEquals(Amf0Value.String(streamKey), message.arguments[0])
            message = receiveCommandMessage(server, 37)
            assertEquals(RtmpCommandName.createStream, message.commandName)
            assertEquals(0, message.arguments.size)
            server.send(
                chunk = RtmpChunk(
                    type = RtmpChunkType.zero,
                    chunkStreamId = 3,
                    message = RtmpCommandMessage(
                        streamId = 0,
                        transactionId = message.transactionId,
                        commandType = RtmpCommandMessage.CommandType.amf0Command,
                        commandName = RtmpCommandName.result,
                        commandObject = null,
                        arguments = listOf(Amf0Value.Number(1.0))
                    )
                )
            )
            message = receiveCommandMessage(server, 43)
            assertEquals(RtmpCommandName.publish, message.commandName)
            assertEquals(2, message.arguments.size)
            assertEquals(Amf0Value.String(streamKey), message.arguments[0])
            assertEquals(Amf0Value.String("live"), message.arguments[1])
            server.send(
                chunk = RtmpChunk(
                    type = RtmpChunkType.zero,
                    chunkStreamId = 3,
                    message = RtmpCommandMessage(
                        streamId = 0,
                        transactionId = message.transactionId,
                        commandType = RtmpCommandMessage.CommandType.amf0Command,
                        commandName = RtmpCommandName.onStatus,
                        commandObject = null,
                        arguments = listOf(
                            Amf0Value.Object(
                                mapOf(
                                    "level" to Amf0Value.String("status"),
                                    "code" to Amf0Value.String("NetStream.Publish.Start"),
                                    "description" to Amf0Value.String("Start publishing.")
                                )
                            )
                        )
                    )
                )
            )
            assertEquals("NetStream.Publish.Start", modelMock.waitForStatus())
            modelMock.waitForConnected()
            rtmpStream.disconnect()
            server.receive(count = 192)
            message = receiveCommandMessage(server, 40)
            assertEquals(RtmpCommandName.fcUnpublish, message.commandName)
            assertEquals(1, message.arguments.size)
            assertEquals(Amf0Value.String(streamKey), message.arguments[0])
            message = receiveCommandMessage(server, 46)
            assertEquals(RtmpCommandName.deleteStream, message.commandName)
            assertEquals(1, message.arguments.size)
            assertEquals(Amf0Value.Number(1.0), message.arguments[0])
            message = receiveCommandMessage(server, 45)
            assertEquals(RtmpCommandName.closeStream, message.commandName)
            assertEquals(1, message.arguments.size)
            assertEquals(Amf0Value.Number(1.0), message.arguments[0])
        }
    }

    @Test
    fun youTube() {
        runBlocking {
            val streamKey = "5"
            val modelMock = ModelMock()
            val processor = Processor(delegate = modelMock)
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
            sendWindowAcknowledgementSize(server, 2, 2_500_000)
            sendSetPeerBandwidth(server, 2, 59_768_832)
            expectWindowAcknowledgementSize(server)
            server.send(
                chunk = RtmpChunk(
                    type = RtmpChunkType.zero,
                    chunkStreamId = 3,
                    message = RtmpCommandMessage(
                        streamId = 0,
                        transactionId = 2,
                        commandType = RtmpCommandMessage.CommandType.amf0Command,
                        commandName = RtmpCommandName.result,
                        commandObject = mapOf(
                            "fmsVer" to Amf0Value.String("FMS/3,5,3,824"),
                            "capabilities" to Amf0Value.Number(127.0),
                            "mode" to Amf0Value.Number(1.0)
                        ),
                        arguments = listOf(
                            Amf0Value.Object(
                                mapOf(
                                    "level" to Amf0Value.String("status"),
                                    "code" to Amf0Value.String("NetConnection.Connect.Success"),
                                    "description" to Amf0Value.String("Connection succeeded."),
                                    "objectEncoding" to Amf0Value.Number(0.0)
                                )
                            )
                        )
                    )
                )
            )
            val setChunkSize = receiveSetChunkSize(server)
            assertEquals(8192L, setChunkSize.size.toLong())
            assertEquals("NetConnection.Connect.Success", modelMock.waitForStatus())
            var message = receiveCommandMessage(server, 42)
            assertEquals(RtmpCommandName.releaseStream, message.commandName)
            assertEquals(1, message.arguments.size)
            assertEquals(Amf0Value.String(streamKey), message.arguments[0])
            message = receiveCommandMessage(server, 38)
            assertEquals(RtmpCommandName.fcPublish, message.commandName)
            assertEquals(1, message.arguments.size)
            assertEquals(Amf0Value.String(streamKey), message.arguments[0])
            message = receiveCommandMessage(server, 37)
            assertEquals(RtmpCommandName.createStream, message.commandName)
            assertEquals(0, message.arguments.size)
            server.send(
                chunk = RtmpChunk(
                    type = RtmpChunkType.zero,
                    chunkStreamId = 3,
                    message = RtmpCommandMessage(
                        streamId = 0,
                        transactionId = message.transactionId,
                        commandType = RtmpCommandMessage.CommandType.amf0Command,
                        commandName = RtmpCommandName.result,
                        commandObject = null,
                        arguments = listOf(Amf0Value.Number(1.0))
                    )
                )
            )
            message = receiveCommandMessage(server, 43)
            assertEquals(RtmpCommandName.publish, message.commandName)
            assertEquals(2, message.arguments.size)
            assertEquals(Amf0Value.String(streamKey), message.arguments[0])
            assertEquals(Amf0Value.String("live"), message.arguments[1])
            server.send(
                chunk = RtmpChunk(
                    type = RtmpChunkType.zero,
                    chunkStreamId = 3,
                    message = RtmpCommandMessage(
                        streamId = 0,
                        transactionId = message.transactionId,
                        commandType = RtmpCommandMessage.CommandType.amf0Command,
                        commandName = RtmpCommandName.onStatus,
                        commandObject = null,
                        arguments = listOf(
                            Amf0Value.Object(
                                mapOf(
                                    "level" to Amf0Value.String("status"),
                                    "code" to Amf0Value.String("NetStream.Publish.Start"),
                                    "description" to Amf0Value.String("Start publishing.")
                                )
                            )
                        )
                    )
                )
            )
            assertEquals("NetStream.Publish.Start", modelMock.waitForStatus())
            modelMock.waitForConnected()
            rtmpStream.disconnect()
            server.receive(count = 192)
            message = receiveCommandMessage(server, 40)
            assertEquals(RtmpCommandName.fcUnpublish, message.commandName)
            assertEquals(1, message.arguments.size)
            assertEquals(Amf0Value.String(streamKey), message.arguments[0])
            message = receiveCommandMessage(server, 46)
            assertEquals(RtmpCommandName.deleteStream, message.commandName)
            assertEquals(1, message.arguments.size)
            assertEquals(Amf0Value.Number(1.0), message.arguments[0])
            message = receiveCommandMessage(server, 45)
            assertEquals(RtmpCommandName.closeStream, message.commandName)
            assertEquals(1, message.arguments.size)
            assertEquals(Amf0Value.Number(1.0), message.arguments[0])
        }
    }

    @Test
    fun acknowledgementChunkSplitOverTwoReads() {
        val modelMock = ModelMock()
        val processor = Processor(delegate = modelMock)
        val rtmpStream = RtmpStream(
            name = "test",
            processor = processor,
            delegate = modelMock,
            queue = rtmpQueue
        )
        val connection = RtmpConnection(name = "test", queue = rtmpQueue)
        connection.stream = rtmpStream
        rtmpStream.info.onWritten(sequence = 14000)
        assertEquals(10, rtmpStream.info.stats.value.packetsInFlight)
        val typeZeroChunk = byteArrayOf(0x02, 0, 0, 0, 0, 0, 4, 0x03, 0, 0, 0, 0) + 1u.toBigEndianBytes()
        assertTrue(connection.socketDataReceived(data = typeZeroChunk).isEmpty())
        assertEquals(10, rtmpStream.info.stats.value.packetsInFlight)
        val typeThreeChunk = byteArrayOf(0xC2.toByte()) + 14001u.toBigEndianBytes()
        val buffered = connection.socketDataReceived(data = typeThreeChunk.copyOfRange(0, 3))
        assertContentEquals(typeThreeChunk.copyOfRange(0, 3), buffered)
        assertTrue(
            connection.socketDataReceived(
                data = buffered + typeThreeChunk.copyOfRange(typeThreeChunk.size - 2, typeThreeChunk.size)
            ).isEmpty()
        )
        assertEquals(0, rtmpStream.info.stats.value.packetsInFlight)
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
        assertContentEquals(byteArrayOf(0x00, 0x88.toByte()), RtmpChunkType.zero.toBasicHeader(200))
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
        assertContentEquals(byteArrayOf(0x01, 0x50, 0x01), RtmpChunkType.zero.toBasicHeader(400))
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
            chunkStreamId = 8,
            message = RtmpAudioMessage(
                streamId = 1,
                timestamp = 0xFFFFFF,
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
    assertEquals(Amf0Value.String("live"), connectMessage["app"])
    assertEquals(Amf0Value.String("FMLE/3.0 (compatible; FMSc/1.0)"), connectMessage["flashVer"])
    assertEquals(Amf0Value.Null, connectMessage["swfUrl"])
    val tcUrl = (connectMessage["tcUrl"] as? Amf0Value.String)?.value ?: error("error")
    assertNotNull(Regex("rtmp://127\\.0\\.0\\.1:\\d+/live").matchEntire(tcUrl))
    assertEquals(Amf0Value.Bool(false), connectMessage["fpad"])
    assertEquals(Amf0Value.Number(239.0), connectMessage["capabilities"])
    assertEquals(Amf0Value.Number(0x0400.toDouble()), connectMessage["audioCodecs"])
    assertEquals(Amf0Value.Number(0x0080.toDouble()), connectMessage["videoCodecs"])
    assertEquals(Amf0Value.Number(1.0), connectMessage["videoFunction"])
    assertEquals(Amf0Value.Null, connectMessage["pageUrl"])
    assertEquals(Amf0Value.Number(0.0), connectMessage["objectEncoding"])
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

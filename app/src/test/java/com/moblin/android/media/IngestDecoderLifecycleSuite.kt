package com.moblin.android.media

import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.HandlerThread
import android.os.Looper
import com.moblin.android.media.haishinkit.codec.video.VideoDecoder
import com.moblin.android.media.haishinkit.media.video.VideoUnit
import com.moblin.android.media.haishinkit.mpeg.ElementaryStreamSpecificData
import com.moblin.android.media.haishinkit.mpeg.ElementaryStreamType
import com.moblin.android.media.haishinkit.mpeg.MpegTsPacketizedElementaryStream
import com.moblin.android.media.haishinkit.mpeg.MpegTsProgramAssociation
import com.moblin.android.media.haishinkit.mpeg.MpegTsProgramMapping
import com.moblin.android.media.haishinkit.mpeg.MpegTsReader
import com.moblin.android.media.haishinkit.mpeg.MpegTsReaderDelegate
import com.moblin.android.media.ristserver.RistServer
import com.moblin.android.media.ristserver.RistServerDelegate
import com.moblin.android.platform.core.PipelineThread
import com.moblin.android.platform.video.CVPixelBufferPool
import com.moblin.android.platform.video.kCVPixelFormatType_32BGRA
import com.moblin.android.platform.video.releaseLease
import com.moblin.android.platform.video.retainLease
import com.moblin.android.platform.videotoolbox.DecompressionSessions
import com.moblin.android.platform.videotoolbox.VTDecompressionSession
import com.moblin.android.various.settings.SettingsRistServerStream
import com.moblin.android.media.rtspclient.RtspClient
import com.moblin.android.media.rtspclient.RtspClientDelegate
import com.moblin.android.various.settings.SettingsRtspTransport
import com.moblin.android.various.settings.SettingsStreamColorRange
import java.io.IOException
import java.io.InputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import java.nio.ByteBuffer
import java.util.Base64
import java.util.Collections
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.MediaCodecInfoBuilder
import org.robolectric.shadows.ShadowMediaCodec
import org.robolectric.shadows.ShadowMediaCodecList

private const val decoderName = "c2.test.avc.decoder"
private const val videoPacketId: UShort = 256u
private const val programMappingTablePacketId: UShort = 4095u
private const val virtualDestinationPort = 2000

private val sps = byteArrayOf(0x67, 0x42, 0xC0.toByte(), 0x1F, 0xDA.toByte(), 0x01, 0x40, 0x16, 0xE8.toByte(), 0x40)
private val pps = byteArrayOf(0x68, 0xCE.toByte(), 0x3C, 0x80.toByte())
private val idrSlice = byteArrayOf(0x65, 0x88.toByte(), 0x84.toByte(), 0x00, 0x33, 0x44)
private val nonIdrSlice = byteArrayOf(0x41, 0x9A.toByte(), 0x02, 0x04, 0x05)

private fun annexB(vararg nalUnits: ByteArray): ByteArray {
    var data = ByteArray(0)
    for (nalUnit in nalUnits) {
        data += byteArrayOf(0, 0, 0, 1) + nalUnit
    }
    return data
}

private fun programPackets(): ByteArray {
    val programAssociation = MpegTsProgramAssociation()
    programAssociation.programs[1u] = programMappingTablePacketId
    val programMapping = MpegTsProgramMapping()
    programMapping.programClockReferencePacketId = videoPacketId
    programMapping.elementaryStreamSpecificDatas.add(
        ElementaryStreamSpecificData(streamType = ElementaryStreamType.h264, elementaryPacketId = videoPacketId),
    )
    return programAssociation.packet(0u).encode() + programMapping.packet(programMappingTablePacketId).encode()
}

private fun videoPackets(frameNumber: Int): ByteArray {
    val keyFrame = frameNumber % 30 == 0
    val data = if (keyFrame) annexB(sps, pps, idrSlice) else annexB(nonIdrSlice)
    val presentationTimeStamp = 1_000_000L + frameNumber * 33_333L
    val packetizedElementaryStream = MpegTsPacketizedElementaryStream(
        streamId = 224u,
        presentationTimeStamp = presentationTimeStamp,
        decodeTimeStamp = presentationTimeStamp,
        data = data,
    )
    var packets = ByteArray(0)
    for (packet in packetizedElementaryStream.arrayOfPackets(videoPacketId, keyFrame, null)) {
        packets += packet.encode()
    }
    return packets
}

private fun transportStream(firstFrameNumber: Int, numberOfFrames: Int): List<ByteArray> {
    return listOf(programPackets()) + (firstFrameNumber until firstFrameNumber + numberOfFrames).map {
        videoPackets(it)
    }
}

private fun waitUntil(what: String, timeoutMs: Long = 10_000, condition: () -> Boolean) {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (!condition()) {
        if (System.currentTimeMillis() > deadline) {
            throw AssertionError("Timed out waiting until $what")
        }
        shadowOf(Looper.getMainLooper()).idle()
        Thread.sleep(5)
    }
}

private fun renderFrame(session: VTDecompressionSession, consumer: Consumer) {
    val heldBefore = consumer.heldFrames.size
    waitUntil("a decoded frame reaches the consumer") {
        PipelineThread.runSync { session.onFrameAvailable() }
        consumer.heldFrames.size > heldBefore
    }
}

private fun awaitNewDecoder(
    before: Set<VTDecompressionSession>,
    consumer: Consumer,
    timeoutMs: Long = 10_000,
): DecoderObservation {
    var session: VTDecompressionSession? = null
    waitUntil("a decoder session is running", timeoutMs) {
        session = DecompressionSessions.running().firstOrNull { it !in before }
        session != null
    }
    val observation = DecoderObservation(session!!)
    renderFrame(observation.session, consumer)
    observation.pool = PipelineThread.runSync { observation.session.outputPool }
    assertNotNull(observation.pool)
    observation.assertRunning()
    return observation
}

private class DecoderObservation(val session: VTDecompressionSession) {
    val thread: HandlerThread = assertNotNull(session.handlerThread)
    var pool: CVPixelBufferPool? = null

    fun assertRunning() {
        assertFalse(session.isInvalidated)
        assertTrue(thread.isAlive)
        assertTrue(DecompressionSessions.running().contains(session))
    }

    fun assertReleased(consumer: Consumer) {
        waitUntil("the decoder is invalidated") { session.isInvalidated }
        thread.join(5_000)
        assertFalse(thread.isAlive, "${thread.name} did not quit")
        assertFalse(DecompressionSessions.running().contains(session))
        val pool = pool ?: return
        waitUntil("the decoder pool only holds the frames its consumer keeps") {
            PipelineThread.runSync {
                pool.state.released && pool.state.free.isEmpty() && pool.state.allocated == consumer.heldFrom(pool)
            }
        }
    }

    fun assertDrained() {
        val pool = assertNotNull(pool)
        waitUntil("the decoder pool is empty") { PipelineThread.runSync { pool.state.allocated == 0 } }
    }
}

private class Consumer {
    val heldFrames: MutableList<MediaSample> = Collections.synchronizedList(mutableListOf())

    fun receive(sampleBuffer: MediaSample) {
        if (sampleBuffer.imageBuffer != null) {
            heldFrames.add(retainLease(sampleBuffer))
        }
    }

    fun heldFrom(pool: CVPixelBufferPool): Int {
        return synchronized(heldFrames) { heldFrames.count { it.imageBuffer?.poolState === pool.state } }
    }

    fun releaseAll() {
        val frames = synchronized(heldFrames) { heldFrames.toList().also { heldFrames.clear() } }
        for (frame in frames) {
            releaseLease(frame)
        }
    }
}

private class Ingest : RistServerDelegate {
    val consumer = Consumer()
    val connected: MutableList<Int> = Collections.synchronizedList(mutableListOf())
    val disconnected: MutableList<Int> = Collections.synchronizedList(mutableListOf())
    val stream = SettingsRistServerStream().also { it.virtualDestinationPort = virtualDestinationPort }
    val server = RistServer(5556, listOf(stream), false, SettingsStreamColorRange.full, this)

    override fun ristServerOnConnected(port: Int) {
        connected.add(port)
    }

    override fun ristServerOnDisconnected(port: Int, reason: String) {
        disconnected.add(port)
    }

    override fun ristServerOnVideoBuffer(cameraId: UUID, sampleBuffer: MediaSample) {
        consumer.receive(sampleBuffer)
    }

    override fun ristServerOnAudioBuffer(cameraId: UUID, sampleBuffer: MediaSample) {}

    fun connect() {
        val numberOfConnects = connected.size
        server.ristReceiverContextConnected(virtualDestinationPort)
        waitUntil("the client is connected") { connected.size > numberOfConnects }
    }

    fun disconnect() {
        val numberOfDisconnects = disconnected.size
        server.ristReceiverContextDisconnected(virtualDestinationPort)
        waitUntil("the client is disconnected") { disconnected.size > numberOfDisconnects }
    }

    fun stream(firstFrameNumber: Int, numberOfFrames: Int): DecoderObservation {
        val before = DecompressionSessions.running().toSet()
        server.ristReceiverContextReceivedData(
            virtualDestinationPort,
            transportStream(firstFrameNumber, numberOfFrames),
        )
        return awaitNewDecoder(before, consumer)
    }
}

private class FakeRtspServer : AutoCloseable {
    private val serverSocket = ServerSocket(0, 5, InetAddress.getLoopbackAddress())
    private val sockets: MutableList<Socket> = Collections.synchronizedList(mutableListOf())
    val url = URI("rtsp://127.0.0.1:${serverSocket.localPort}/stream")

    init {
        Thread { accept() }.apply { isDaemon = true }.start()
    }

    private fun accept() {
        while (true) {
            val socket = try {
                serverSocket.accept()
            } catch (error: IOException) {
                return
            }
            sockets.add(socket)
            Thread { serve(socket) }.apply { isDaemon = true }.start()
        }
    }

    private fun serve(socket: Socket) {
        try {
            val input = socket.getInputStream()
            val output = socket.getOutputStream()
            var sequenceNumber = 0
            while (true) {
                val request = readRequest(input) ?: return
                val method = request.substringBefore(" ")
                val cSeq = Regex("CSeq: (\\d+)").find(request)?.groupValues?.get(1) ?: return
                var response = "RTSP/1.0 200 OK\r\nCSeq: $cSeq\r\n"
                var content = ""
                when (method) {
                    "DESCRIBE" -> {
                        val encoder = Base64.getEncoder()
                        content = "v=0\r\nm=video 0 RTP/AVP 96\r\na=rtpmap:96 H264/90000\r\n" +
                            "a=fmtp:96 packetization-mode=1;sprop-parameter-sets=" +
                            "${encoder.encodeToString(sps)},${encoder.encodeToString(pps)}\r\n" +
                            "a=control:trackID=0\r\n"
                        response += "Content-Type: application/sdp\r\nContent-Length: ${content.length}\r\n"
                    }
                    "SETUP" -> response += "Session: 12345678\r\nTransport: RTP/AVP/TCP;unicast;interleaved=0-1\r\n"
                }
                output.write((response + "\r\n" + content).toByteArray())
                if (method == "PLAY") {
                    for (frameNumber in 0 until 5) {
                        output.write(interleavedRtpPacket(sequenceNumber, frameNumber * 3000L, idrSlice))
                        sequenceNumber += 1
                    }
                }
                output.flush()
            }
        } catch (error: IOException) {
            return
        }
    }

    private fun readRequest(input: InputStream): String? {
        val request = StringBuilder()
        while (!request.endsWith("\r\n\r\n")) {
            val value = input.read()
            if (value < 0) {
                return null
            }
            request.append(value.toChar())
        }
        return request.toString()
    }

    private fun interleavedRtpPacket(sequenceNumber: Int, timestamp: Long, nalUnit: ByteArray): ByteArray {
        val packet = byteArrayOf(
            0x80.toByte(),
            96,
            (sequenceNumber shr 8).toByte(),
            sequenceNumber.toByte(),
            (timestamp shr 24).toByte(),
            (timestamp shr 16).toByte(),
            (timestamp shr 8).toByte(),
            timestamp.toByte(),
            0x11,
            0x22,
            0x33,
            0x44,
        ) + nalUnit
        return byteArrayOf(0x24, 0, (packet.size shr 8).toByte(), packet.size.toByte()) + packet
    }

    override fun close() {
        serverSocket.close()
        synchronized(sockets) {
            for (socket in sockets) {
                socket.close()
            }
        }
    }
}

private class RtspIngest : RtspClientDelegate {
    val consumer = Consumer()
    val server = FakeRtspServer()
    val client = RtspClient(UUID.randomUUID(), server.url, 0.5, SettingsRtspTransport.rtpRtspTcp, false, SettingsStreamColorRange.full, this)

    override fun rtspClientErrorToast(title: String) {}

    override fun rtspClientConnected(cameraId: UUID) {}

    override fun rtspClientDisconnected(cameraId: UUID) {}

    override fun rtspClientOnVideoBuffer(cameraId: UUID, sampleBuffer: MediaSample) {
        consumer.receive(sampleBuffer)
    }

    fun start(): DecoderObservation {
        val before = DecompressionSessions.running().toSet()
        client.start()
        return awaitNewDecoder(before, consumer, timeoutMs = 30_000)
    }
}

private class ReaderOutput : MpegTsReaderDelegate {
    val consumer = Consumer()

    override fun mpegTsReaderAudioBuffer(sampleBuffer: MediaSample) {}

    override fun mpegTsReaderVideoBuffer(sampleBuffer: MediaSample) {
        consumer.receive(sampleBuffer)
    }
}

@RunWith(RobolectricTestRunner::class)
class IngestDecoderLifecycleSuite {
    @Before
    fun registerDecoder() {
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 1920, 1080)
        ShadowMediaCodecList.addCodec(
            MediaCodecInfoBuilder.newBuilder()
                .setName(decoderName)
                .setIsEncoder(false)
                .setCapabilities(
                    MediaCodecInfoBuilder.CodecCapabilitiesBuilder.newBuilder()
                        .setMediaFormat(format)
                        .setIsEncoder(false)
                        .setColorFormats(intArrayOf(MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface))
                        .build(),
                )
                .build(),
        )
        val config = ShadowMediaCodec.CodecConfig(2 * 1024 * 1024, 1024) { input, output ->
            val size = minOf(input.remaining(), output.remaining())
            output.put(ByteArray(size))
        }
        ShadowMediaCodec.addDecoder(MediaFormat.MIMETYPE_VIDEO_AVC, config)
        ShadowMediaCodec.addDecoder(decoderName, config)
    }

    @Test
    fun ristDisconnectReleasesTheDecoderSessionThreadAndPool() {
        val ingest = Ingest()
        ingest.connect()
        val decoder = ingest.stream(firstFrameNumber = 0, numberOfFrames = 4)
        assertTrue(ingest.consumer.heldFrom(decoder.pool!!) > 0)
        ingest.disconnect()
        decoder.assertReleased(ingest.consumer)
        ingest.consumer.releaseAll()
        decoder.assertDrained()
        ingest.server.stop()
    }

    @Test
    fun ristReconnectReleasesThePreviousDecoder() {
        val ingest = Ingest()
        ingest.connect()
        val first = ingest.stream(firstFrameNumber = 0, numberOfFrames = 4)
        ingest.connect()
        first.assertReleased(ingest.consumer)
        val second = ingest.stream(firstFrameNumber = 30, numberOfFrames = 4)
        assertNotSame(first.session, second.session)
        ingest.disconnect()
        second.assertReleased(ingest.consumer)
        ingest.connect()
        val third = ingest.stream(firstFrameNumber = 60, numberOfFrames = 4)
        ingest.disconnect()
        third.assertReleased(ingest.consumer)
        ingest.consumer.releaseAll()
        first.assertDrained()
        second.assertDrained()
        third.assertDrained()
        ingest.server.stop()
    }

    @Test
    fun ristServerStopReleasesTheDecoder() {
        val ingest = Ingest()
        ingest.connect()
        val decoder = ingest.stream(firstFrameNumber = 0, numberOfFrames = 4)
        ingest.server.stop()
        decoder.assertReleased(ingest.consumer)
        assertEquals(listOf(virtualDestinationPort), ingest.disconnected.toList())
        ingest.consumer.releaseAll()
        decoder.assertDrained()
    }

    @Test
    fun rtspStopAndReconnectReleaseTheDecoder() {
        val ingest = RtspIngest()
        try {
            val first = ingest.start()
            val second = ingest.start()
            assertNotSame(first.session, second.session)
            first.assertReleased(ingest.consumer)
            ingest.client.stop()
            second.assertReleased(ingest.consumer)
            ingest.consumer.releaseAll()
            first.assertDrained()
            second.assertDrained()
        } finally {
            ingest.client.stop()
            ingest.server.close()
        }
    }

    @Test
    fun readerStopReleasesTheDecoder() {
        val reader = MpegTsReader(
            name = "srt-server",
            decoderQueue = Dispatchers.IO,
            timecodesEnabled = false,
            softwareDecoding = false,
            colorRange = SettingsStreamColorRange.full,
            targetLatency = 0.5,
        )
        val output = ReaderOutput()
        reader.delegate = output
        val before = DecompressionSessions.running().toSet()
        for (packet in transportStream(firstFrameNumber = 0, numberOfFrames = 3)) {
            reader.handlePacketFromClient(packet)
        }
        val decoder = awaitNewDecoder(before, output.consumer)
        reader.stop()
        decoder.assertReleased(output.consumer)
        output.consumer.releaseAll()
        decoder.assertDrained()
        reader.stop()
        assertTrue(DecompressionSessions.running().none { it !in before })
    }

    @Test
    fun stoppedReaderDropsDecodedFramesStillQueuedForIt() {
        val queue = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        val reader = MpegTsReader(
            name = "srt-server",
            decoderQueue = queue,
            timecodesEnabled = false,
            softwareDecoding = false,
            colorRange = SettingsStreamColorRange.full,
            targetLatency = 0.5,
        )
        val output = ReaderOutput()
        reader.delegate = output
        val before = DecompressionSessions.running().toSet()
        for (packet in transportStream(firstFrameNumber = 0, numberOfFrames = 8)) {
            reader.handlePacketFromClient(packet)
        }
        val decoder = awaitNewDecoder(before, output.consumer)
        val pool = assertNotNull(decoder.pool)
        val delivered = output.consumer.heldFrames.size
        val gate = CountDownLatch(1)
        CoroutineScope(queue).launch { gate.await(10, TimeUnit.SECONDS) }
        waitUntil("a decoded frame waits on the blocked decoder queue") {
            PipelineThread.runSync {
                decoder.session.onFrameAvailable()
                pool.state.allocated - pool.state.free.size > output.consumer.heldFrom(pool)
            }
        }
        reader.stop()
        gate.countDown()
        runBlocking(queue) {}
        assertEquals(delivered, output.consumer.heldFrames.size)
        decoder.assertReleased(output.consumer)
        output.consumer.releaseAll()
        decoder.assertDrained()
        queue.close()
    }

    @Test
    fun stoppedDecoderDoesNotStartANewSession() {
        val decoder = VideoDecoder("rtsp-client", CoroutineScope(Dispatchers.IO), false, SettingsStreamColorRange.full)
        val before = DecompressionSessions.running().toSet()
        val formatDescription = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 0, 0)
        formatDescription.setByteBuffer("csd-0", ByteBuffer.wrap(byteArrayOf(0, 0, 0, 1) + sps))
        formatDescription.setByteBuffer("csd-1", ByteBuffer.wrap(byteArrayOf(0, 0, 0, 1) + pps))
        val keyFrame = MediaSample(annexB(sps, pps, idrSlice), 1_000_000, true, formatDescription)
        decoder.startRunning(formatDescription)
        decoder.decodeSampleBuffer(keyFrame)
        val observation = DecoderObservation(DecompressionSessions.running().single { it !in before })
        val feeder = Thread {
            repeat(200) {
                decoder.decodeSampleBuffer(keyFrame)
            }
        }
        feeder.start()
        decoder.stopRunning()
        feeder.join(10_000)
        observation.assertReleased(Consumer())
        decoder.decodeSampleBuffer(keyFrame)
        assertTrue(DecompressionSessions.running().none { it !in before })
    }

    @Test
    fun replacingABufferedVideoReturnsItsFramesToThePool() {
        val videoUnit = VideoUnit(SettingsStreamColorRange.full)
        val cameraId = UUID.randomUUID()
        val pool = CVPixelBufferPool(64, 36, kCVPixelFormatType_32BGRA, maximumBufferCount = 16)
        try {
            videoUnit.addBufferedVideo(cameraId, "rist-server", 2.0)
            val buffers = PipelineThread.runSync { (0 until 10).map { assertNotNull(retainLease(pool.createPixelBuffer())) } }
            for ((index, buffer) in buffers.withIndex()) {
                val presentationTimeStamp = 1_000_000_000_000L + index * 33_333L
                val sampleBuffer = MediaSample(ByteArray(0), presentationTimeStamp, false, null, buffer, 33_333)
                videoUnit.appendBufferedVideoSampleBuffer(cameraId, sampleBuffer)
            }
            for (buffer in buffers) {
                releaseLease(buffer)
            }
            PipelineThread.runSync {}
            PipelineThread.runSync {
                assertEquals(10, pool.state.allocated)
                assertEquals(0, pool.state.free.size)
            }
            videoUnit.addBufferedVideo(cameraId, "rist-server", 2.0)
            PipelineThread.runSync {}
            PipelineThread.runSync {
                assertEquals(10, pool.state.free.size)
                assertEquals(0, pool.state.leased)
            }
            for (buffer in buffers) {
                assertEquals(0, buffer.leaseCount.get())
            }
            videoUnit.removeBufferedVideo(cameraId)
            PipelineThread.runSync {}
        } finally {
            videoUnit.dispose()
            pool.invalidate()
        }
    }
}

package com.moblin.android.media.rtspclient

import java.io.OutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val rtspClientThreadName = "com.moblin.android.rtsp"

private class RecordingDelegate : RtspTransportDelegate {
    val events: MutableList<String> = Collections.synchronizedList(mutableListOf())
    val threads: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())

    private fun record(event: String) {
        threads.add(Thread.currentThread().name.substringBefore(" @coroutine"))
        events.add(event)
    }

    override fun rtspTransportConnected() = record("connected")

    override fun rtspTransportDisconnected() = record("disconnected")

    override fun rtspTransportReceivedRtspMessage(header: ByteArray, content: ByteArray?) {
        val cSeq = Regex("CSeq: (\\d+)").find(header.toString(Charsets.UTF_8))?.groupValues?.get(1)
        record("rtsp $cSeq ${content?.toString(Charsets.UTF_8)}")
    }

    override fun rtspTransportReceivedRtpPacket(packet: ByteArray) = record("rtp ${packet.toString(Charsets.UTF_8)}")

    override fun rtspTransportReceivedRtcpPacket(packet: ByteArray) = record("rtcp ${packet.toString(Charsets.UTF_8)}")
}

private fun waitUntil(what: String, condition: () -> Boolean) {
    val deadline = System.currentTimeMillis() + 10_000
    while (!condition()) {
        if (System.currentTimeMillis() > deadline) {
            throw AssertionError("Timed out waiting until $what")
        }
        Thread.sleep(5)
    }
}

private fun threadsConnectingFrom(transportClass: Class<*>): List<String> {
    return Thread.getAllStackTraces().filter { (_, trace) ->
        trace.any { it.className.startsWith(transportClass.name) } &&
            trace.any { it.className.startsWith("java.net.") && it.methodName == "connect" }
    }.keys.map { it.name }
}

private fun onRtspClientQueue(block: () -> Unit) {
    runBlocking(rtspClientQueue) {
        block()
    }
}

private fun interleaved(channel: Int, payload: String): ByteArray {
    val data = payload.toByteArray()
    return byteArrayOf(0x24, channel.toByte(), (data.size shr 8).toByte(), data.size.toByte()) + data
}

private fun responses(output: OutputStream, first: Int, count: Int) {
    for (cSeq in first until first + count) {
        output.write("RTSP/1.0 200 OK\r\nCSeq: $cSeq\r\n\r\n".toByteArray())
    }
}

@RunWith(RobolectricTestRunner::class)
class RtspTransportQueueSuite {
    private val server = ServerSocket(0, 1, InetAddress.getLoopbackAddress())
    private val closeables = mutableListOf<AutoCloseable>(server)

    @After
    fun tearDown() {
        for (closeable in closeables) {
            closeable.close()
        }
    }

    private fun accept(): Socket {
        return server.accept().also { closeables.add(it) }
    }

    @Test
    fun tcpTransportDeliversEverythingOnTheRtspClientQueueInOrder() {
        val transport = RtspTransportRtpRtspTcp()
        transport.handleSetupTransportResponse("RTP/AVP/TCP;unicast;interleaved=0-1")
        val delegate = RecordingDelegate()
        transport.delegate = delegate
        onRtspClientQueue { transport.start("127.0.0.1", server.localPort) }
        val output = accept().getOutputStream()
        responses(output, first = 1, count = 50)
        output.write("RTSP/1.0 200 OK\r\nCSeq: 51\r\nContent-Length: 3\r\n\r\nsdp".toByteArray())
        output.write(interleaved(0, "video"))
        output.write(interleaved(1, "report"))
        output.flush()
        waitUntil("everything is delivered") { delegate.events.size == 54 }
        val expected = listOf("connected") + (1..50).map { "rtsp $it null" } +
            listOf("rtsp 51 sdp", "rtp video", "rtcp report")
        assertEquals(expected, delegate.events.toList())
        assertEquals(setOf(rtspClientThreadName), delegate.threads.toSet())
        onRtspClientQueue { transport.stop() }
    }

    @Test
    fun tcpTransportDeliversNothingAfterItIsStopped() {
        val transport = RtspTransportRtpRtspTcp()
        transport.handleSetupTransportResponse("RTP/AVP/TCP;unicast;interleaved=0-1")
        val delegate = RecordingDelegate()
        transport.delegate = delegate
        onRtspClientQueue { transport.start("127.0.0.1", server.localPort) }
        val output = accept().getOutputStream()
        waitUntil("connected") { delegate.events.contains("connected") }
        val gate = CountDownLatch(1)
        CoroutineScope(rtspClientQueue).launch { gate.await(5, TimeUnit.SECONDS) }
        CoroutineScope(rtspClientQueue).launch { transport.stop() }
        output.write(interleaved(0, "late"))
        responses(output, first = 1, count = 1)
        output.flush()
        Thread.sleep(300)
        gate.countDown()
        onRtspClientQueue {}
        assertEquals(listOf("connected"), delegate.events.toList())
    }

    @Test
    fun stopAbortsAConnectInProgress() {
        for (transport in listOf(RtspTransportRtpRtspTcp(), RtspTransportRtpUdp())) {
            val delegate = RecordingDelegate()
            transport.delegate = delegate
            onRtspClientQueue { transport.start("10.255.255.1", 554) }
            Thread.sleep(300)
            onRtspClientQueue { transport.stop() }
            waitUntil("no thread is still connecting for ${transport.javaClass.simpleName}") {
                threadsConnectingFrom(transport.javaClass).isEmpty()
            }
            onRtspClientQueue {}
            assertEquals(emptyList(), delegate.events.toList())
        }
    }

    @Test
    fun udpTransportDeliversEverythingOnTheRtspClientQueue() {
        val transport = RtspTransportRtpUdp()
        val delegate = RecordingDelegate()
        transport.delegate = delegate
        onRtspClientQueue { transport.start("127.0.0.1", server.localPort) }
        val output = accept().getOutputStream()
        waitUntil("connected") { delegate.events.contains("connected") }
        val ports = Regex("client_port=(\\d+)-(\\d+)").find(transport.setupTransportHeader())!!.groupValues
        responses(output, first = 1, count = 20)
        output.write("RTSP/1.0 200 OK\r\nCSeq: 21\r\nContent-Length: 3\r\n\r\nsdp".toByteArray())
        output.flush()
        waitUntil("the RTSP messages are delivered") { delegate.events.size == 22 }
        val sender = DatagramSocket().also { closeables.add(it) }
        val loopback = InetAddress.getLoopbackAddress()
        sender.send(DatagramPacket("video".toByteArray(), 5, InetSocketAddress(loopback, ports[1].toInt())))
        waitUntil("the RTP packet is delivered") { delegate.events.size == 23 }
        sender.send(DatagramPacket("report".toByteArray(), 6, InetSocketAddress(loopback, ports[2].toInt())))
        waitUntil("the RTCP packet is delivered") { delegate.events.size == 24 }
        val expected = listOf("connected") + (1..20).map { "rtsp $it null" } +
            listOf("rtsp 21 sdp", "rtp video", "rtcp report")
        assertEquals(expected, delegate.events.toList())
        assertEquals(setOf(rtspClientThreadName), delegate.threads.toSet())
        onRtspClientQueue { transport.stop() }
        sender.send(DatagramPacket("late".toByteArray(), 4, InetSocketAddress(loopback, ports[1].toInt())))
        Thread.sleep(100)
        onRtspClientQueue {}
        assertFalse(delegate.events.contains("rtp late"))
    }
}

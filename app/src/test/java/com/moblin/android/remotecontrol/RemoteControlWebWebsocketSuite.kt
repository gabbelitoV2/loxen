package com.moblin.android.remotecontrol

import android.os.Looper
import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.Proxy
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.Collections
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class RemoteControlWebWebsocketSuite {
    private val calls = Collections.synchronizedList(mutableListOf<String>())
    private val client = Executors.newSingleThreadExecutor()
    private lateinit var web: RemoteControlWeb
    private var socket: Socket? = null
    private var longestMainBlock = 0L
    private val handshake = (
        "GET / HTTP/1.1\r\nHost: phone\r\nUpgrade: websocket\r\nConnection: Upgrade\r\n" +
            "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\nSec-WebSocket-Version: 13\r\n\r\n"
        ).toByteArray()

    @Before
    fun setUp() {
        val delegate = Proxy.newProxyInstance(
            RemoteControlWebDelegate::class.java.classLoader,
            arrayOf(RemoteControlWebDelegate::class.java),
        ) { _, method, args ->
            calls.add(method.name + (args?.joinToString(prefix = "(", postfix = ")") ?: ""))
            when (method.returnType) {
                java.lang.Boolean.TYPE -> false
                Integer.TYPE -> 0
                java.lang.Float.TYPE -> 0f
                else -> null
            }
        } as RemoteControlWebDelegate
        web = RemoteControlWeb(delegate)
    }

    @After
    fun tearDown() {
        runCatching { socket?.close() }
        web.stop()
        shadowOf(Looper.getMainLooper()).idle()
        client.shutdownNow()
    }

    private fun <T> await(future: Future<T>): T {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (System.nanoTime() < deadline) {
            val start = System.nanoTime()
            shadowOf(Looper.getMainLooper()).idle()
            longestMainBlock = maxOf(longestMainBlock, System.nanoTime() - start)
            try {
                return future.get(5, TimeUnit.MILLISECONDS)
            } catch (_: TimeoutException) {
            }
        }
        fail("timed out")
    }

    private fun <T> onClient(block: () -> T): T = await(client.submit(Callable { block() }))

    private fun awaitCall(name: String) {
        await(client.submit(Callable { while (calls.none { it.startsWith(name) }) Thread.sleep(5) }))
    }

    private fun connect(port: Int): Socket {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (true) {
            try {
                return Socket(InetAddress.getLoopbackAddress(), port).also { it.soTimeout = 10_000 }
            } catch (error: java.io.IOException) {
                if (System.nanoTime() > deadline) {
                    throw error
                }
                Thread.sleep(10)
            }
        }
    }

    private fun readHead(input: InputStream): String {
        val head = StringBuilder()
        while (!head.endsWith("\r\n\r\n")) {
            val byte = input.read()
            if (byte < 0) {
                break
            }
            head.append(byte.toChar())
        }
        return head.toString()
    }

    private fun writeFrame(output: OutputStream, opcode: Int, payload: ByteArray) {
        val mask = byteArrayOf(1, 2, 3, 4)
        val frame = java.io.ByteArrayOutputStream()
        frame.write(0x80 or opcode)
        if (payload.size < 126) {
            frame.write(0x80 or payload.size)
        } else {
            frame.write(0x80 or 126)
            frame.write(payload.size shr 8)
            frame.write(payload.size and 0xFF)
        }
        frame.write(mask)
        frame.write(ByteArray(payload.size) { (payload[it].toInt() xor mask[it % 4].toInt()).toByte() })
        output.write(frame.toByteArray())
        output.flush()
    }

    private fun readFrame(input: InputStream): Pair<Int, String> {
        val opcode = input.read() and 0x0F
        var length = input.read() and 0x7F
        if (length == 126) {
            length = (input.read() shl 8) or input.read()
        } else if (length == 127) {
            length = 0
            repeat(8) {
                length = (length shl 8) or input.read()
            }
        }
        val payload = ByteArray(length)
        var offset = 0
        while (offset < length) {
            offset += input.read(payload, offset, length - offset)
        }
        return opcode to payload.toString(Charsets.UTF_8)
    }

    @Test
    fun theWebRemoteControlTalksOverItsWebsocketWithoutBlockingTheMainThread() {
        val port = ServerSocket(0).use { it.localPort }
        web.start(port - 1)
        shadowOf(Looper.getMainLooper()).idle()
        val head = onClient {
            val socket = connect(port).also { socket = it }
            socket.getOutputStream().write(handshake, 0, 20)
            Thread.sleep(1_000)
            socket.getOutputStream().write(handshake, 20, handshake.size - 20)
            readHead(socket.getInputStream())
        }
        assertTrue(longestMainBlock < TimeUnit.MILLISECONDS.toNanos(500), "main blocked ${longestMainBlock / 1_000_000} ms")
        assertTrue(head.startsWith("HTTP/1.1 101 Switching Protocols\r\n"), head)
        assertTrue("Sec-WebSocket-Accept: s3pPLMBiTxaQ9kYGzzhZRbK+xOo=\r\n" in head, head)
        awaitCall("remoteControlWebConnected")
        val connection = assertNotNull(socket)
        val request = assertNotNull(
            RemoteControlMessageToStreamer.Request(id = 5, data = RemoteControlRequest.SetMute(on = true)).toJson(),
        )
        val response = onClient {
            writeFrame(connection.getOutputStream(), 0x1, request.toByteArray())
            readFrame(connection.getInputStream())
        }
        assertEquals(
            0x1 to RemoteControlMessageToAssistant.Response(id = 5, result = RemoteControlResult.Ok, data = null).toJson(),
            response,
        )
        assertTrue("remoteControlWebSetMute(true)" in calls)
        web.log(entry = "12:00:00.000 hello")
        val log = onClient { readFrame(connection.getInputStream()) }
        assertEquals(
            0x1 to RemoteControlMessageToAssistant.Event(data = RemoteControlEvent.Log(entry = "12:00:00.000 hello")).toJson(),
            log,
        )
        val pong = onClient {
            writeFrame(connection.getOutputStream(), 0x9, "ping".toByteArray())
            readFrame(connection.getInputStream())
        }
        assertEquals(0xA to "ping", pong)
        onClient { connection.close() }
        awaitCall("remoteControlWebDisconnected")
    }

    @Test
    fun aBrowserWhoseHandshakeEndsAfterStopIsClosedAndNeverConnected() {
        val port = ServerSocket(0).use { it.localPort }
        web.start(port - 1)
        shadowOf(Looper.getMainLooper()).idle()
        onClient {
            val socket = connect(port).also { socket = it }
            socket.getOutputStream().write(handshake, 0, 20)
            Thread.sleep(500)
        }
        web.stop()
        val reply = onClient {
            val socket = assertNotNull(socket)
            socket.getOutputStream().write(handshake, 20, handshake.size - 20)
            socket.getInputStream().read()
        }
        assertEquals(-1, reply)
        assertTrue(calls.none { it.startsWith("remoteControlWebConnected") }, calls.toString())
    }
}

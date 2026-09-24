package com.moblin.android.various.network

import android.os.Looper
import com.moblin.android.AppDelegate
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.assertEquals
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class WebSocketClientSuite {
    private val server = MockWebServer()
    private val serverSockets = CopyOnWriteArrayList<WebSocket>()
    private val clients = mutableListOf<WebSocketClient>()

    private class Delegate : WebSocketClientDelegate {
        var connected = 0
        var disconnected = 0
        val messages = mutableListOf<String>()

        override fun webSocketClientConnected(webSocket: WebSocketClient) {
            connected += 1
        }

        override fun webSocketClientDisconnected(webSocket: WebSocketClient) {
            disconnected += 1
        }

        override fun webSocketClientReceiveMessage(webSocket: WebSocketClient, string: String) {
            messages.add(string)
        }
    }

    @Before
    fun setUp() {
        repeat(5) {
            server.enqueue(
                MockResponse().withWebSocketUpgrade(
                    object : WebSocketListener() {
                        override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                            serverSockets.add(webSocket)
                        }

                        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                            webSocket.close(code, null)
                        }
                    },
                ),
            )
        }
        server.start()
    }

    @After
    fun tearDown() {
        clients.forEach { it.stop() }
        pump(millis = 500)
        server.shutdown()
    }

    private fun url(): String {
        return server.url("/").toString().replaceFirst("http", "ws")
    }

    private fun pump(millis: Long = 3000, until: () -> Boolean = { false }) {
        val end = System.currentTimeMillis() + millis
        while (System.currentTimeMillis() < end) {
            shadowOf(Looper.getMainLooper()).idle()
            if (until()) {
                return
            }
            Thread.sleep(10)
        }
    }

    private fun advance(seconds: Long) {
        repeat(seconds.toInt()) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
            pump(millis = 50)
        }
    }

    private fun connect(delegate: Delegate): WebSocketClient {
        val client = WebSocketClient(context = AppDelegate.context, url = url(), loopback = true)
        clients.add(client)
        client.delegate = delegate
        client.start()
        pump { delegate.connected == 1 }
        assertEquals(1, delegate.connected)
        return client
    }

    @Test
    fun stoppedClientDoesNotReconnect() {
        val delegate = Delegate()
        val client = connect(delegate)
        client.stop()
        pump(millis = 1000)
        advance(30)
        assertEquals(1, server.requestCount)
        assertEquals(0, delegate.disconnected)
    }

    @Test
    fun restartDoesNotLoop() {
        val delegate = Delegate()
        val client = connect(delegate)
        client.start()
        pump { delegate.connected == 2 }
        advance(30)
        assertEquals(2, server.requestCount)
        assertEquals(2, delegate.connected)
        assertEquals(0, delegate.disconnected)
    }

    @Test
    fun staysConnectedAcrossPings() {
        val delegate = Delegate()
        connect(delegate)
        advance(45)
        assertEquals(1, server.requestCount)
        assertEquals(0, delegate.disconnected)
    }

    @Test
    fun receivesMessagesAndReconnectsWhenServerCloses() {
        val delegate = Delegate()
        connect(delegate)
        pump { serverSockets.size == 1 }
        serverSockets[0].send("hello")
        pump { delegate.messages.isNotEmpty() }
        assertEquals(listOf("hello"), delegate.messages)
        serverSockets[0].close(1000, null)
        pump { delegate.disconnected == 1 }
        assertEquals(1, delegate.disconnected)
        advance(1)
        pump { delegate.connected == 2 }
        assertEquals(2, delegate.connected)
        assertEquals(2, server.requestCount)
    }
}

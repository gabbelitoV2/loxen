package com.moblin.android.various.network

import android.content.Context
import android.util.Log
import com.moblin.android.various.MainTimer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okio.ByteString
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

private const val shortestDelayMs = 500
private const val longestDelayMs = 10000
private const val tag = "WebSocketClient"

interface WebSocketClientDelegate {
    fun webSocketClientConnected(webSocket: WebSocketClient)
    fun webSocketClientDisconnected(webSocket: WebSocketClient)
    fun webSocketClientReceiveMessage(webSocket: WebSocketClient, string: String)
}

class WebSocketClient(
    context: Context,
    url: String,
    loopback: Boolean = false,
    cellular: Boolean = true,
    protocols: List<String>? = null
) {
    private var webSocket: WebSocket? = null
    private var connectTimer = MainTimer()
    private var networkInterfaceTypeSelector: NetworkInterfaceTypeSelector
    private var pingTimer = MainTimer()
    private var pongReceived = true
    private var delegateReference: WeakReference<WebSocketClientDelegate>? = null

    var delegate: WebSocketClientDelegate?
        get() = delegateReference?.get()
        set(value) {
            delegateReference = value?.let { WeakReference(it) }
        }

    private val url: String
    private val loopback: Boolean
    private var connected = false
    private var connectDelayMs = shortestDelayMs
    private val protocols: List<String>?
    private val mainScope = CoroutineScope(Dispatchers.Main)

    init {
        this.url = url
        this.loopback = loopback
        this.protocols = protocols
        networkInterfaceTypeSelector = NetworkInterfaceTypeSelector(context, Dispatchers.Main, cellular)
    }

    fun start() {
        startInternal()
    }

    fun stop() {
        stopInternal()
    }

    fun isConnected(): Boolean {
        return connected
    }

    fun send(string: String) {
        webSocket?.send(string)
    }

    private fun startInternal() {
        stopInternal()
        val interfaceTypeAvailable = networkInterfaceTypeSelector.getNextType() != null
        if (interfaceTypeAvailable || loopback) {
            webSocket = newWebSocket()
            Log.d(tag, "websocket: Connecting to $url")
            startPingTimer()
        } else {
            connectDelayMs = shortestDelayMs
            startConnectTimer()
        }
    }

    private fun newWebSocket(): WebSocket {
        val requestBuilder = Request.Builder().url(url)
        protocols?.let { subprotocols ->
            requestBuilder.header("Sec-WebSocket-Protocol", subprotocols.joinToString(", "))
        }
        val client = OkHttpClient.Builder()
            .pingInterval(10, TimeUnit.SECONDS)
            .build()
        return client.newWebSocket(requestBuilder.build(), listener)
    }

    private fun stopInternal() {
        connected = false
        webSocket?.close(1000, null)
        webSocket = null
        stopConnectTimer()
        stopPingTimer()
    }

    private fun startConnectTimer() {
        connected = false
        connectTimer.startSingleShot(connectDelayMs / 1000.0) {
            startInternal()
        }
        connectDelayMs *= 2
        if (connectDelayMs > longestDelayMs) {
            connectDelayMs = longestDelayMs
        }
    }

    private fun stopConnectTimer() {
        connectTimer.stop()
    }

    private fun startPingTimer() {
        pongReceived = true
        pingTimer.startPeriodic(10.0, 0.0) {
            if (pongReceived) {
                pongReceived = false
                TODO("NWWebSocket.ping() has no OkHttp counterpart, OkHttpClient.pingInterval pings instead")
            } else {
                startInternal()
                delegate?.webSocketClientDisconnected(this)
            }
        }
    }

    private fun stopPingTimer() {
        pingTimer.stop()
    }

    fun webSocketDidConnect() {
        Log.d(tag, "websocket: Connected")
        connectDelayMs = shortestDelayMs
        stopConnectTimer()
        connected = true
        delegate?.webSocketClientConnected(this)
    }

    fun webSocketDidDisconnect(closeCode: Int, reason: String?) {
        Log.d(tag, "websocket: Disconnected")
        stopInternal()
        startConnectTimer()
        delegate?.webSocketClientDisconnected(this)
    }

    fun webSocketViabilityDidChange(isViable: Boolean) {
        TODO("NWConnection viability has no Android counterpart in OkHttp")
    }

    fun webSocketDidAttemptBetterPathMigration() {
        TODO("NWConnection better path migration has no Android counterpart in OkHttp")
    }

    fun webSocketDidReceiveError(error: Throwable) {
        Log.d(tag, "websocket: Error ${error.message}")
        val wasConnected = connected
        stopInternal()
        startConnectTimer()
        if (wasConnected) {
            delegate?.webSocketClientDisconnected(this)
        }
    }

    fun webSocketDidReceivePong() {
        pongReceived = true
    }

    fun webSocketDidReceiveMessage(string: String) {
        delegate?.webSocketClientReceiveMessage(this, string = string)
    }

    fun webSocketDidReceiveMessage(data: ByteArray) {
    }

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            mainScope.launch {
                webSocketDidConnect()
            }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            mainScope.launch {
                webSocketDidReceiveMessage(text)
            }
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            mainScope.launch {
                webSocketDidReceiveMessage(bytes.toByteArray())
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(code, reason)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            mainScope.launch {
                webSocketDidDisconnect(code, reason)
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            mainScope.launch {
                webSocketDidReceiveError(t)
            }
        }
    }
}

package com.moblin.android.various.network

import android.content.Context
import com.moblin.android.platform.log.Log
import com.moblin.android.platform.network.NWError
import com.moblin.android.platform.network.NWInterface
import com.moblin.android.platform.network.NWProtocolWebSocket
import com.moblin.android.platform.network.NWWebSocket
import com.moblin.android.platform.network.WebSocketConnection
import com.moblin.android.platform.network.WebSocketConnectionDelegate
import com.moblin.android.various.MainTimer
import java.lang.ref.WeakReference
import kotlinx.coroutines.Dispatchers

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
    private val url: String,
    private val loopback: Boolean = false,
    cellular: Boolean = true,
    private val protocols: List<String>? = null,
) : WebSocketConnectionDelegate {
    private var webSocket = NWWebSocket(url = url, requiredInterfaceType = NWInterface.InterfaceType.cellular)
    private var connectTimer = MainTimer()
    private var networkInterfaceTypeSelector = NetworkInterfaceTypeSelector(context, Dispatchers.Main, cellular)
    private var pingTimer = MainTimer()
    private var pongReceived = true
    private var delegateReference: WeakReference<WebSocketClientDelegate>? = null
    private var connected = false
    private var connectDelayMs = shortestDelayMs

    var delegate: WebSocketClientDelegate?
        get() = delegateReference?.get()
        set(value) {
            delegateReference = value?.let { WeakReference(it) }
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
        webSocket.send(string = string)
    }

    private fun startInternal() {
        stopInternal()
        var interfaceType = networkInterfaceTypeSelector.getNextType()?.toNWInterfaceType()
        if (interfaceType != null) {
            if (loopback) {
                interfaceType = NWInterface.InterfaceType.loopback
            }
            val options = NWProtocolWebSocket.Options()
            options.autoReplyPing = true
            if (protocols != null) {
                options.setSubprotocols(protocols)
            }
            webSocket = NWWebSocket(url = url, requiredInterfaceType = interfaceType, options = options)
            Log.d(tag, "websocket: Connecting to $url over $interfaceType")
            webSocket.delegate = this
            webSocket.connect()
            startPingTimer()
        } else {
            connectDelayMs = shortestDelayMs
            startConnectTimer()
        }
    }

    private fun stopInternal() {
        connected = false
        webSocket.disconnect()
        webSocket = NWWebSocket(url = url, requiredInterfaceType = NWInterface.InterfaceType.cellular)
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
                webSocket.ping()
            } else {
                startInternal()
                delegate?.webSocketClientDisconnected(this)
            }
        }
    }

    private fun stopPingTimer() {
        pingTimer.stop()
    }

    override fun webSocketDidConnect(connection: WebSocketConnection) {
        Log.d(tag, "websocket: Connected")
        connectDelayMs = shortestDelayMs
        stopConnectTimer()
        connected = true
        delegate?.webSocketClientConnected(this)
    }

    override fun webSocketDidDisconnect(
        connection: WebSocketConnection,
        closeCode: NWProtocolWebSocket.CloseCode,
        reason: ByteArray?,
    ) {
        Log.d(tag, "websocket: Disconnected")
        stopInternal()
        startConnectTimer()
        delegate?.webSocketClientDisconnected(this)
    }

    override fun webSocketViabilityDidChange(connection: WebSocketConnection, isViable: Boolean) {
        Log.d(tag, "websocket: Viability changed to $isViable")
        if (isViable) {
            return
        }
        stopInternal()
        startConnectTimer()
        delegate?.webSocketClientDisconnected(this)
    }

    override fun webSocketDidAttemptBetterPathMigration(result: Result<WebSocketConnection>) {
        Log.d(tag, "websocket: Better path migration")
    }

    override fun webSocketDidReceiveError(connection: WebSocketConnection, error: NWError) {
        Log.d(tag, "websocket: Error ${error.message}")
        val connected = connected
        stopInternal()
        startConnectTimer()
        if (connected) {
            delegate?.webSocketClientDisconnected(this)
        }
    }

    override fun webSocketDidReceivePong(connection: WebSocketConnection) {
        pongReceived = true
    }

    override fun webSocketDidReceiveMessage(connection: WebSocketConnection, string: String) {
        delegate?.webSocketClientReceiveMessage(this, string = string)
    }

    override fun webSocketDidReceiveMessage(connection: WebSocketConnection, data: ByteArray) {}
}

private fun InterfaceType.toNWInterfaceType(): NWInterface.InterfaceType {
    return when (this) {
        InterfaceType.cellular -> NWInterface.InterfaceType.cellular
        InterfaceType.wifi -> NWInterface.InterfaceType.wifi
        InterfaceType.wiredEthernet -> NWInterface.InterfaceType.wiredEthernet
        InterfaceType.other -> NWInterface.InterfaceType.other
    }
}

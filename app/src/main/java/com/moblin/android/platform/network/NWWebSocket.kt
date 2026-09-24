package com.moblin.android.platform.network

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.moblin.android.AppDelegate
import java.lang.ref.WeakReference
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString

class NWProtocolWebSocket {
    class Options {
        var autoReplyPing = false
        internal var subprotocols: List<String> = emptyList()
            private set

        fun setSubprotocols(subprotocols: List<String>) {
            this.subprotocols = subprotocols
        }
    }

    sealed class CloseCode {
        enum class Defined(val rawValue: Int) {
            normalClosure(1000),
            goingAway(1001),
            protocolError(1002),
            unsupportedData(1003),
            noStatusReceived(1005),
            abnormalClosure(1006),
            invalidFramePayloadData(1007),
            policyViolation(1008),
            messageTooBig(1009),
            mandatoryExtension(1010),
            internalServerError(1011),
            tlsHandshake(1015),
        }

        data class protocolCode(val code: Defined) : CloseCode()

        data class applicationCode(val code: Int) : CloseCode()

        data class privateCode(val code: Int) : CloseCode()

        val rawValue: Int
            get() = when (this) {
                is protocolCode -> code.rawValue
                is applicationCode -> code
                is privateCode -> code
            }

        companion object {
            fun fromRawValue(rawValue: Int): CloseCode {
                Defined.entries.firstOrNull { it.rawValue == rawValue }?.let {
                    return protocolCode(it)
                }
                return if (rawValue in 3000..3999) applicationCode(rawValue) else privateCode(rawValue)
            }
        }
    }
}

interface WebSocketConnection {
    var delegate: WebSocketConnectionDelegate?

    fun connect()

    fun send(string: String)

    fun send(data: ByteArray)

    fun ping()

    fun disconnect(
        closeCode: NWProtocolWebSocket.CloseCode =
            NWProtocolWebSocket.CloseCode.protocolCode(NWProtocolWebSocket.CloseCode.Defined.normalClosure),
    )
}

interface WebSocketConnectionDelegate {
    fun webSocketDidConnect(connection: WebSocketConnection)

    fun webSocketDidDisconnect(
        connection: WebSocketConnection,
        closeCode: NWProtocolWebSocket.CloseCode,
        reason: ByteArray?,
    )

    fun webSocketViabilityDidChange(connection: WebSocketConnection, isViable: Boolean)

    fun webSocketDidAttemptBetterPathMigration(result: Result<WebSocketConnection>)

    fun webSocketDidReceiveError(connection: WebSocketConnection, error: NWError)

    fun webSocketDidReceivePong(connection: WebSocketConnection)

    fun webSocketDidReceiveMessage(connection: WebSocketConnection, string: String)

    fun webSocketDidReceiveMessage(connection: WebSocketConnection, data: ByteArray)
}

class NWWebSocket(
    val url: String,
    private val requiredInterfaceType: NWInterface.InterfaceType,
    private val options: NWProtocolWebSocket.Options = NWProtocolWebSocket.Options(),
) : WebSocketConnection {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var delegateReference: WeakReference<WebSocketConnectionDelegate>? = null
    private var socket: WebSocket? = null
    private var session: Any? = null
    private var open = false
    private var pendingPing = false
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override var delegate: WebSocketConnectionDelegate?
        get() = delegateReference?.get()
        set(value) {
            delegateReference = value?.let { WeakReference(it) }
        }

    override fun connect() {
        if (session != null) {
            return
        }
        val current = Any()
        session = current
        val request = try {
            Request.Builder().url(url).apply {
                if (options.subprotocols.isNotEmpty()) {
                    header("Sec-WebSocket-Protocol", options.subprotocols.joinToString(", "))
                }
            }.build()
        } catch (error: IllegalArgumentException) {
            post(current) { delegate?.webSocketDidReceiveError(this, NWError("Invalid URL $url")) }
            return
        }
        val builder = baseClient.newBuilder().pingInterval(10, TimeUnit.SECONDS)
        when (requiredInterfaceType) {
            NWInterface.InterfaceType.loopback, NWInterface.InterfaceType.other -> {}
            else -> {
                val network = findNetwork(requiredInterfaceType)
                if (network == null) {
                    post(current) {
                        delegate?.webSocketDidReceiveError(this, NWError("No $requiredInterfaceType network"))
                    }
                    return
                }
                builder.socketFactory(network.socketFactory)
                builder.dns(
                    object : Dns {
                        override fun lookup(hostname: String): List<InetAddress> {
                            return network.getAllByName(hostname).toList()
                        }
                    },
                )
                watchNetwork(current, network)
            }
        }
        socket = builder.build().newWebSocket(request, Listener(current))
    }

    override fun send(string: String) {
        socket?.send(string)
    }

    override fun send(data: ByteArray) {
        socket?.send(data.toByteString())
    }

    override fun ping() {
        val current = session ?: return
        if (open) {
            post(current) { delegate?.webSocketDidReceivePong(this) }
        } else {
            pendingPing = true
        }
    }

    override fun disconnect(closeCode: NWProtocolWebSocket.CloseCode) {
        session = null
        open = false
        pendingPing = false
        unwatchNetwork()
        socket?.close(closeCode.rawValue, null)
        socket = null
    }

    private fun post(current: Any, block: () -> Unit) {
        scope.launch {
            if (session === current) {
                block()
            }
        }
    }

    private fun watchNetwork(current: Any, network: Network) {
        val connectivityManager = AppDelegate.context.getSystemService(ConnectivityManager::class.java) ?: return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onLost(lost: Network) {
                if (lost == network) {
                    post(current) {
                        open = false
                        delegate?.webSocketViabilityDidChange(this@NWWebSocket, false)
                    }
                }
            }
        }
        try {
            connectivityManager.registerNetworkCallback(NetworkRequest.Builder().build(), callback)
            networkCallback = callback
        } catch (error: RuntimeException) {
            networkCallback = null
        }
    }

    private fun unwatchNetwork() {
        val callback = networkCallback ?: return
        networkCallback = null
        try {
            AppDelegate.context.getSystemService(ConnectivityManager::class.java)?.unregisterNetworkCallback(callback)
        } catch (error: RuntimeException) {
        }
    }

    private inner class Listener(private val current: Any) : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            post(current) {
                open = true
                delegate?.webSocketDidConnect(this@NWWebSocket)
                if (pendingPing) {
                    pendingPing = false
                    ping()
                }
            }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            post(current) { delegate?.webSocketDidReceiveMessage(this@NWWebSocket, text) }
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            val data = bytes.toByteArray()
            post(current) { delegate?.webSocketDidReceiveMessage(this@NWWebSocket, data) }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(code, null)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            post(current) {
                open = false
                delegate?.webSocketDidDisconnect(
                    this@NWWebSocket,
                    NWProtocolWebSocket.CloseCode.fromRawValue(code),
                    reason.toByteArray().takeIf { it.isNotEmpty() },
                )
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            post(current) {
                open = false
                delegate?.webSocketDidReceiveError(this@NWWebSocket, NWError(t.message ?: t.toString()))
            }
        }
    }

    private companion object {
        val baseClient: OkHttpClient by lazy { OkHttpClient() }

        @Suppress("DEPRECATION")
        fun findNetwork(type: NWInterface.InterfaceType): Network? {
            val transport = when (type) {
                NWInterface.InterfaceType.wifi -> NetworkCapabilities.TRANSPORT_WIFI
                NWInterface.InterfaceType.cellular -> NetworkCapabilities.TRANSPORT_CELLULAR
                NWInterface.InterfaceType.wiredEthernet -> NetworkCapabilities.TRANSPORT_ETHERNET
                else -> return null
            }
            val connectivityManager = try {
                AppDelegate.context.getSystemService(ConnectivityManager::class.java)
            } catch (error: Throwable) {
                null
            } ?: return null
            return connectivityManager.allNetworks.firstOrNull { network ->
                val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return@firstOrNull false
                capabilities.hasTransport(transport) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            }
        }
    }
}

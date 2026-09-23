package com.moblin.android.platform.network

import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketAddress
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.coroutines.ContinuationInterceptor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NWListener(using: NWParameters, on: NWEndpoint.Port? = null) {
    sealed class State {
        data object setup : State()

        class waiting(val error: NWError) : State() {
            override fun toString(): String {
                return "waiting($error)"
            }
        }

        data object ready : State()

        class failed(val error: NWError) : State() {
            override fun toString(): String {
                return "failed($error)"
            }
        }

        data object cancelled : State()
    }

    val parameters: NWParameters = using
    private val requestedPort: NWEndpoint.Port? = on
    private val lock = Any()
    private val id = nextId.incrementAndGet()
    private var started = false
    private var hasFailed = false
    private val datagramConnections = HashMap<SocketAddress, NWConnection>()

    @Volatile
    private var scope: CoroutineScope? = null

    @Volatile
    private var cancelled = false

    @Volatile
    private var currentState: State = State.setup

    @Volatile
    private var boundPort: NWEndpoint.Port? = null

    @Volatile
    private var datagramSocket: DatagramSocket? = null

    @Volatile
    private var serverSocket: ServerSocket? = null

    @Volatile
    var stateUpdateHandler: ((State) -> Unit)? = null

    @Volatile
    var newConnectionHandler: ((NWConnection) -> Unit)? = null

    val port: NWEndpoint.Port?
        get() = boundPort

    val state: State
        get() = currentState

    private val description: String
        get() = if (parameters.isDatagram) "udp-listener#$id" else "tcp-listener#$id"

    fun start(queue: CoroutineDispatcher) {
        startInternal(CoroutineScope(queue + SupervisorJob()))
    }

    fun start(queue: CoroutineScope) {
        val dispatcher = queue.coroutineContext[ContinuationInterceptor] as? CoroutineDispatcher ?: Dispatchers.Default
        startInternal(CoroutineScope(dispatcher + SupervisorJob()))
    }

    fun cancel() {
        synchronized(lock) {
            if (cancelled) {
                return
            }
            cancelled = true
            currentState = State.cancelled
        }
        closeSockets()
        val queueScope = scope ?: return
        queueScope.launch {
            runHandler {
                stateUpdateHandler?.invoke(State.cancelled)
            }
        }
    }

    private fun startInternal(queueScope: CoroutineScope) {
        synchronized(lock) {
            if (started || cancelled) {
                return
            }
            started = true
            scope = queueScope
        }
        if (parameters.isDatagram) {
            startThread("udp-listener") { runUdp() }
        } else {
            startThread("tcp-listener") { runTcp() }
        }
    }

    private fun startThread(kind: String, block: () -> Unit) {
        thread(name = "nw-$kind-$id", isDaemon = true) {
            try {
                block()
            } catch (error: Throwable) {
                Log.e(TAG, "$description: Thread failed", error)
            }
        }
    }

    private fun bindAddress(): InetSocketAddress {
        val requiredLocalEndpoint = parameters.requiredLocalEndpoint
        val portValue = requestedPort?.value ?: requiredLocalEndpoint?.port?.value ?: 0
        return when {
            requiredLocalEndpoint != null -> InetSocketAddress(requiredLocalEndpoint.host.value, portValue)
            parameters.acceptLocalOnly -> InetSocketAddress(ipv4Loopback, portValue)
            else -> InetSocketAddress(portValue)
        }
    }

    private fun runUdp() {
        val socket = try {
            DatagramSocket(null as SocketAddress?).apply {
                if (parameters.allowLocalEndpointReuse) {
                    reuseAddress = true
                }
                bind(bindAddress())
            }
        } catch (error: Throwable) {
            if (!cancelled) {
                fail(makeError(error))
            }
            return
        }
        datagramSocket = socket
        if (cancelled) {
            socket.close()
            return
        }
        boundPort = NWEndpoint.Port(socket.localPort)
        Log.i(TAG, "$description: Listening on ${socket.localSocketAddress}")
        setState(State.ready)
        val buffer = ByteArray(MAXIMUM_DATAGRAM_SIZE)
        val packet = DatagramPacket(buffer, buffer.size)
        while (!cancelled) {
            try {
                packet.length = buffer.size
                socket.receive(packet)
            } catch (error: Throwable) {
                if (cancelled) {
                    return
                }
                fail(makeError(error))
                return
            }
            val address = packet.address ?: continue
            if (parameters.acceptLocalOnly && !address.isLoopbackAddress) {
                continue
            }
            val peer = packet.socketAddress
            val data = buffer.copyOf(packet.length)
            val existing = datagramConnections[peer]
            val connection = if (existing != null && !existing.isCancelled) {
                existing
            } else {
                val endpoint = NWEndpoint.hostPort(
                    host = NWEndpoint.Host(address.hostAddress ?: ""),
                    port = NWEndpoint.Port(packet.port),
                )
                val newConnection = NWConnection(endpoint, parameters, socket, peer)
                datagramConnections[peer] = newConnection
                Log.i(TAG, "$description: New connection from $endpoint")
                deliverNewConnection(newConnection)
                newConnection
            }
            connection.deliverDatagram(data)
        }
    }

    private fun runTcp() {
        if (parameters.tlsOptions != null) {
            fail(NWError("TLS listeners are not supported"))
            return
        }
        val server = try {
            ServerSocket().apply {
                if (parameters.allowLocalEndpointReuse) {
                    reuseAddress = true
                }
                bind(bindAddress(), TCP_BACKLOG)
            }
        } catch (error: Throwable) {
            if (!cancelled) {
                fail(makeError(error))
            }
            return
        }
        serverSocket = server
        if (cancelled) {
            closeQuietly(server)
            return
        }
        boundPort = NWEndpoint.Port(server.localPort)
        Log.i(TAG, "$description: Listening on ${server.localSocketAddress}")
        setState(State.ready)
        while (!cancelled) {
            val socket = try {
                server.accept()
            } catch (error: Throwable) {
                if (cancelled) {
                    return
                }
                fail(makeError(error))
                return
            }
            val address = socket.inetAddress
            if (address == null || (parameters.acceptLocalOnly && !address.isLoopbackAddress)) {
                closeQuietly(socket)
                continue
            }
            val tcpOptions = parameters.tcpOptions
            try {
                socket.tcpNoDelay = tcpOptions.noDelay
                socket.keepAlive = tcpOptions.enableKeepalive
            } catch (error: Throwable) {
                Log.i(TAG, "$description: Socket options failed: $error")
            }
            val endpoint = NWEndpoint.hostPort(
                host = NWEndpoint.Host(address.hostAddress ?: ""),
                port = NWEndpoint.Port(socket.port),
            )
            deliverNewConnection(NWConnection(endpoint, parameters, socket))
        }
    }

    private fun deliverNewConnection(connection: NWConnection) {
        post {
            val handler = newConnectionHandler
            if (handler == null) {
                connection.cancel()
            } else {
                handler(connection)
            }
        }
    }

    private fun setState(newState: State) {
        synchronized(lock) {
            val current = currentState
            if (cancelled || current is State.failed || current == newState) {
                return
            }
            currentState = newState
        }
        post {
            stateUpdateHandler?.invoke(newState)
        }
    }

    private fun fail(error: NWError) {
        synchronized(lock) {
            if (hasFailed || cancelled) {
                return
            }
            hasFailed = true
        }
        Log.i(TAG, "$description: Failed: $error")
        setState(State.failed(error))
        closeSockets()
    }

    private fun closeSockets() {
        datagramSocket?.close()
        val server = serverSocket
        if (server != null) {
            closeQuietly(server)
        }
    }

    private fun post(block: () -> Unit) {
        val queueScope = scope ?: return
        queueScope.launch {
            if (!cancelled) {
                runHandler(block)
            }
        }
    }

    private fun runHandler(block: () -> Unit) {
        try {
            block()
        } catch (error: Throwable) {
            Log.e(TAG, "$description: Handler failed", error)
        }
    }

    private fun closeQuietly(socket: ServerSocket) {
        runCatching {
            socket.close()
        }
    }

    private fun closeQuietly(socket: Socket) {
        runCatching {
            socket.close()
        }
    }

    private fun makeError(error: Throwable): NWError {
        if (error is NWError) {
            return error
        }
        val message = error.message
        return NWError(
            if (message.isNullOrEmpty()) {
                error.javaClass.simpleName
            } else {
                "${error.javaClass.simpleName}: $message"
            },
        )
    }

    private companion object {
        private const val TAG = "MoblinNet"
        private const val MAXIMUM_DATAGRAM_SIZE = 65_535
        private const val TCP_BACKLOG = 50
        private val nextId = AtomicInteger(0)
        private val ipv4Loopback: InetAddress = InetAddress.getByAddress(byteArrayOf(127, 0, 0, 1))
    }
}

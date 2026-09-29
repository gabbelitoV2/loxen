package com.moblin.android.various.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.moblin.android.platform.log.Log
import com.moblin.android.AppDelegate
import com.moblin.android.various.SimpleTimer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.ConnectException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.UnknownHostException

private const val TAG = "HttpProxyServer"

private val queue = CoroutineScope(Dispatchers.IO)

class HttpConnectRequestParser {
    data class Result(
        val destinationHost: String,
        val destinationPort: Int,
        val version: String,
        val bodyOffset: Int,
    )

    var data: ByteArray = ByteArray(0)

    fun append(data: ByteArray) {
        this.data += data
    }

    private fun getLine(data: ByteArray, offset: Int): Pair<String, Int>? {
        val remaining = data.copyOfRange(offset, data.size)
        var rIndex = -1
        for (i in 0 until remaining.size - 1) {
            if (remaining[i] == '\r'.code.toByte() && remaining[i + 1] == '\n'.code.toByte()) {
                rIndex = i
                break
            }
        }
        if (rIndex == -1) {
            return null
        }
        val line = String(remaining, 0, rIndex, Charsets.UTF_8)
        return Pair(line, offset + rIndex + 2)
    }

    fun parse(): Pair<Boolean, Result?> {
        var offset = 0
        val startLineResult = getLine(data, offset) ?: return Pair(false, null)
        val startLine = startLineResult.first
        offset = startLineResult.second
        val parts = startLine.split(" ")
        if (parts.size != 3 || parts[0] != "CONNECT") {
            return Pair(true, null)
        }
        val version = parts[2]
        if (!version.startsWith("HTTP/1.")) {
            return Pair(true, null)
        }
        val hostPort = parts[1].split(":", limit = 2)
        if (hostPort.size != 2) {
            return Pair(true, null)
        }
        val port = hostPort[1].toIntOrNull() ?: return Pair(true, null)
        val host = hostPort[0]
        while (true) {
            val lineResult = getLine(data, offset) ?: return Pair(false, null)
            offset = lineResult.second
            if (lineResult.first.isEmpty()) {
                return Pair(true, Result(host, port, version, offset))
            }
        }
    }
}

private class Connection(
    private val client: Socket,
    private val networkInterfaceTypeSelector: NetworkInterfaceTypeSelector,
    private val onStopped: (Connection) -> Unit,
) {
    private var destination: Socket? = null
    private var parser = HttpConnectRequestParser()
    private var tunneling = false
    private var body: ByteArray? = null
    private var stopping = false
    private val stopSoonTimer = SimpleTimer(Dispatchers.IO)
    private var clientJob: Job? = null
    private var destinationJob: Job? = null

    fun start() {
        clientJob = queue.launch {
            receiveFromClient()
        }
    }

    fun cancel() {
        stopping = true
        stopSoonTimer.stop()
        runCatching { client.close() }
        destination?.let { runCatching { it.close() } }
        destination = null
        clientJob?.cancel()
        destinationJob?.cancel()
    }

    private fun stop() {
        if (stopping) {
            return
        }
        stopping = true
        stopSoonTimer.startSingleShot(10.0) {
            cancel()
            onStopped(this@Connection)
        }
    }

    private suspend fun receiveFromClient() {
        val input = try {
            client.getInputStream()
        } catch (e: Exception) {
            stop()
            return
        }
        val buffer = ByteArray(65536)
        while (!stopping) {
            val size = try {
                input.read(buffer)
            } catch (e: Exception) {
                stop()
                return
            }
            if (size < 0) {
                closeWrite(destination)
                stop()
                return
            }
            if (size == 0) {
                continue
            }
            val data = buffer.copyOf(size)
            if (tunneling) {
                handleDataTunneling(data)
            } else {
                handleDataConnecting(data)
            }
        }
    }

    private fun handleDataConnecting(data: ByteArray) {
        parser.append(data)
        val parsed = parser.parse()
        if (!parsed.first) {
            return
        }
        val result = parsed.second
        if (result == null) {
            sendResponseAndStop("HTTP/1.1 400 Bad Request\r\n\r\n")
            return
        }
        if (result.bodyOffset < parser.data.size) {
            body = parser.data.copyOfRange(result.bodyOffset, parser.data.size)
        }
        connectToDestination(result.destinationHost, result.destinationPort, result.version)
    }

    private fun handleDataTunneling(data: ByteArray) {
        val destination = destination ?: return
        try {
            destination.getOutputStream().write(data)
            destination.getOutputStream().flush()
        } catch (e: Exception) {
            stop()
        }
    }

    private fun connectToDestination(host: String, port: Int, version: String) {
        val interfaceType = networkInterfaceTypeSelector.getType()
        val socket = Socket()
        try {
            if (interfaceType != null) {
                bindToInterfaceType(socket, interfaceType)
            }
            socket.connect(InetSocketAddress(host, port))
        } catch (e: Exception) {
            handleDestinationNotConnected(socket, version, e, interfaceType)
            return
        }
        destination = socket
        tunneling = true
        sendResponse("$version 200 Connection Established\r\n\r\n")
        body?.let {
            try {
                socket.getOutputStream().write(it)
                socket.getOutputStream().flush()
            } catch (e: Exception) {
            }
            body = null
        }
        destinationJob = queue.launch {
            receiveFromDestination()
        }
    }

    @Suppress("DEPRECATION")
    private fun bindToInterfaceType(socket: Socket, interfaceType: InterfaceType) {
        val transport = when (interfaceType) {
            InterfaceType.cellular -> NetworkCapabilities.TRANSPORT_CELLULAR
            InterfaceType.wifi -> NetworkCapabilities.TRANSPORT_WIFI
            InterfaceType.wiredEthernet -> NetworkCapabilities.TRANSPORT_ETHERNET
            InterfaceType.other -> return
        }
        val connectivityManager = AppDelegate.context.getSystemService(ConnectivityManager::class.java) ?: return
        val network: Network = connectivityManager.allNetworks.firstOrNull { network ->
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return@firstOrNull false
            capabilities.hasTransport(transport) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } ?: throw IOException("No $interfaceType network")
        network.bindSocket(socket)
    }

    private fun handleDestinationNotConnected(
        connection: Socket,
        version: String,
        error: Exception,
        interfaceType: InterfaceType?,
    ) {
        runCatching { connection.close() }
        destination = null
        sendResponseAndStop("$version 502 Bad Gateway\r\n\r\n")
        if (interfaceType != null && !isDestinationError(error)) {
            networkInterfaceTypeSelector.markBad(interfaceType)
        }
    }

    private fun isDestinationError(error: Exception): Boolean {
        return error is ConnectException || error is SocketException || error is UnknownHostException
    }

    private suspend fun receiveFromDestination() {
        val socket = destination ?: return
        val input = try {
            socket.getInputStream()
        } catch (e: Exception) {
            stop()
            return
        }
        val buffer = ByteArray(65536)
        while (!stopping) {
            val size = try {
                input.read(buffer)
            } catch (e: Exception) {
                stop()
                return
            }
            if (size < 0) {
                closeWrite(client)
                stop()
                return
            }
            if (size == 0) {
                continue
            }
            val data = buffer.copyOf(size)
            try {
                client.getOutputStream().write(data)
                client.getOutputStream().flush()
            } catch (e: Exception) {
                stop()
                return
            }
        }
    }

    private fun closeWrite(connection: Socket?) {
        connection?.let { runCatching { it.shutdownOutput() } }
    }

    private fun sendResponse(response: String) {
        try {
            client.getOutputStream().write(response.toByteArray())
            client.getOutputStream().flush()
        } catch (e: Exception) {
        }
    }

    private fun sendResponseAndStop(response: String) {
        try {
            client.getOutputStream().write(response.toByteArray())
            client.getOutputStream().flush()
        } catch (e: Exception) {
        }
        closeWrite(client)
        stop()
    }
}

interface HttpProxyServerDelegate {
    fun httpProxyServerPortReady(port: Int)
}

class HttpProxyServer(private val context: Context) {
    private var listener: ServerSocket? = null
    private val retryTimer = SimpleTimer(Dispatchers.IO)
    private var started = false
    private var port: Int = 0
    private var localNetwork = false
    private var connections: MutableList<Connection> = mutableListOf()
    private val networkInterfaceTypeSelector = NetworkInterfaceTypeSelector(context, Dispatchers.IO)
    var delegate: HttpProxyServerDelegate? = null

    fun start(port: Int, localNetwork: Boolean) {
        Log.i(TAG, "http-proxy: Start")
        queue.launch {
            startInternal(port, localNetwork)
        }
    }

    fun stop() {
        Log.i(TAG, "http-proxy: Stop")
        queue.launch {
            stopInternal()
        }
    }

    private fun startInternal(port: Int, localNetwork: Boolean) {
        this.port = port
        this.localNetwork = localNetwork
        started = true
        setupListener()
    }

    private fun stopInternal() {
        started = false
        retryTimer.stop()
        listener?.let { runCatching { it.close() } }
        listener = null
        for (connection in connections) {
            connection.cancel()
        }
        connections.clear()
    }

    private fun setupListener() {
        val newListener = try {
            if (localNetwork) {
                ServerSocket(port)
            } else {
                ServerSocket(port, 50, InetAddress.getByName("127.0.0.1"))
            }
        } catch (e: Exception) {
            handleStateUpdate(e)
            return
        }
        listener = newListener
        handleStateUpdate(null)
        queue.launch {
            acceptConnections(newListener)
        }
    }

    private fun handleStateUpdate(error: Exception?) {
        if (error == null) {
            Log.i(TAG, "http-proxy: Listening on port $port")
            delegate?.httpProxyServerPortReady(port)
        } else {
            Log.i(TAG, "http-proxy: Listener failed with $error")
            retryTimer.startSingleShot(1.0) {
                if (started) {
                    setupListener()
                }
            }
        }
    }

    private suspend fun acceptConnections(serverSocket: ServerSocket) {
        while (started && !serverSocket.isClosed) {
            val client = try {
                serverSocket.accept()
            } catch (e: Exception) {
                return
            }
            handleNewConnection(client)
        }
    }

    private fun handleNewConnection(clientConnection: Socket) {
        val connection = Connection(clientConnection, networkInterfaceTypeSelector) { c ->
            removeConnection(c)
        }
        connections.add(connection)
        connection.start()
    }

    private fun removeConnection(connection: Connection) {
        connections.removeAll { it === connection }
    }
}

package com.moblin.android.moblink

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.moblin.android.platform.log.Log
import com.moblin.android.remotecontrol.remoteControlApiVersion
import com.moblin.android.remotecontrol.remoteControlHashPassword
import com.moblin.android.various.MainTimer
import com.moblin.android.various.storages.SimpleStringStorage
import com.moblin.android.various.utils.randomString
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "MoblinkStreamer"

interface MoblinkStreamerDelegate {
    fun moblinkStreamerTunnelAdded(host: String, port: Int, relayId: UUID, relayName: String)
    fun moblinkStreamerTunnelRemoved(host: String, port: Int)
}

private class RequestResponse(
    val onSuccess: (MoblinkResponse?) -> Unit,
    val onError: (String) -> Unit,
)

private enum class WebSocketOpcode(val value: Int) {
    TEXT(0x1),
    BINARY(0x2),
    CLOSE(0x8),
    PING(0x9),
    PONG(0xA),
}

private class WebSocketFrame(val opcode: WebSocketOpcode, val data: ByteArray)

private class WebSocketConnection(private val socket: Socket) {
    private val input = socket.getInputStream().buffered()
    private val output = socket.getOutputStream().buffered()
    private val sendLock = Any()

    val host: String = socket.inetAddress?.hostAddress ?: ""
    val port: Int = socket.port

    fun cancel() {
        runCatching { socket.close() }
    }

    fun sendWebSocket(data: ByteArray?, opcode: WebSocketOpcode) {
        val payload = data ?: ByteArray(0)
        synchronized(sendLock) {
            try {
                val header = ByteArray(10)
                var headerLength = 2
                header[0] = (0x80 or opcode.value).toByte()
                when {
                    payload.size < 126 -> {
                        header[1] = payload.size.toByte()
                    }
                    payload.size < 65536 -> {
                        header[1] = 126.toByte()
                        header[2] = ((payload.size shr 8) and 0xFF).toByte()
                        header[3] = (payload.size and 0xFF).toByte()
                        headerLength = 4
                    }
                    else -> {
                        header[1] = 127.toByte()
                        for (index in 0 until 8) {
                            header[2 + index] = ((payload.size.toLong() shr (56 - 8 * index)) and 0xFF).toByte()
                        }
                        headerLength = 10
                    }
                }
                output.write(header, 0, headerLength)
                if (payload.isNotEmpty()) {
                    output.write(payload)
                }
                output.flush()
            } catch (e: IOException) {
                Log.i(TAG, "moblink-streamer: Failed to send with error $e")
            }
        }
    }

    fun readMessage(): WebSocketFrame? {
        try {
            val first = readExactly(2) ?: return null
            val opcodeValue = first[0].toInt() and 0x0F
            val masked = (first[1].toInt() and 0x80) != 0
            var length = (first[1].toInt() and 0x7F).toLong()
            if (length == 126L) {
                val extended = readExactly(2) ?: return null
                length = (((extended[0].toInt() and 0xFF) shl 8) or (extended[1].toInt() and 0xFF)).toLong()
            } else if (length == 127L) {
                val extended = readExactly(8) ?: return null
                var value = 0L
                for (byte in extended) {
                    value = (value shl 8) or (byte.toLong() and 0xFF)
                }
                length = value
            }
            if (length > Int.MAX_VALUE) {
                return null
            }
            val maskKey = if (masked) readExactly(4) ?: return null else null
            val payload = if (length > 0) readExactly(length.toInt()) ?: return null else ByteArray(0)
            if (maskKey != null) {
                for (index in payload.indices) {
                    payload[index] = (payload[index].toInt() xor maskKey[index % 4].toInt()).toByte()
                }
            }
            val opcode = WebSocketOpcode.entries.firstOrNull { it.value == opcodeValue } ?: WebSocketOpcode.CLOSE
            return WebSocketFrame(opcode, payload)
        } catch (e: IOException) {
            Log.i(TAG, "moblink-streamer: Failed to read with error $e")
            return null
        }
    }

    private fun readExactly(count: Int): ByteArray? {
        val buffer = ByteArray(count)
        var offset = 0
        while (offset < count) {
            val read = input.read(buffer, offset, count - offset)
            if (read < 0) {
                return null
            }
            offset += read
        }
        return buffer
    }
}

private class MoblinkServerRelay(
    val webSocket: WebSocketConnection,
    private val password: String,
    private val streamer: MoblinkStreamer?,
) {
    private var nextId = 0
    private var identified = false
    private var challenge = ""
    private var salt = ""
    private val requests = mutableMapOf<Int, RequestResponse>()
    private var address: String? = null
    private var port: Int? = null
    private var tunnelEndpointHost: String? = null
    private var tunnelEndpointPort: Int? = null
    var relayId: UUID = UUID.randomUUID()
    var name = ""
    var batteryPercentage: Int? = null
    var thermalState: MoblinkThermalState? = null
    private var pingTimer = MainTimer()
    var pongReceived = true

    fun start() {
        challenge = randomString()
        salt = randomString()
        send(
            MoblinkMessageToRelay.Hello(
                apiVersion = remoteControlApiVersion,
                authentication = MoblinkAuthentication(challenge = challenge, salt = salt),
            )
        )
        identified = false
        startPingTimer()
    }

    fun stop() {
        stopPingTimer()
        webSocket.cancel()
        reportTunnelRemoved()
        requests.clear()
    }

    fun handleStringMessage(message: String) {
        try {
            val parsed = MoblinkMessageToStreamer.fromJson(message)
            when (parsed) {
                is MoblinkMessageToStreamer.Identify -> handleIdentify(
                    parsed.id,
                    parsed.name,
                    parsed.authentication,
                )
                is MoblinkMessageToStreamer.Response -> handleResponse(
                    parsed.id,
                    parsed.result,
                    parsed.data,
                )
                else -> {}
            }
        } catch (e: Exception) {
            Log.i(TAG, "moblink-streamer: $name: Failed to process message with error $e")
            webSocket.cancel()
        }
    }

    fun startTunnel(address: String, port: Int) {
        this.address = address
        this.port = port
        startTunnelInternal()
    }

    fun stopTunnel() {
        reportTunnelRemoved()
    }

    fun restartTunnel() {
        startTunnelInternal()
    }

    fun updateStatus() {
        performRequest(
            data = MoblinkRequest.Status,
            onSuccess = { response ->
                if (response is MoblinkResponse.Status) {
                    batteryPercentage = response.batteryPercentage
                    thermalState = response.thermalState
                }
            },
            onError = { error ->
                Log.i(TAG, "moblink-streamer: $name: Status failed with $error")
            },
        )
    }

    private fun startPingTimer() {
        pongReceived = true
        pingTimer.startPeriodic(interval = 10.0, initial = 0.0) {
            if (pongReceived) {
                pongReceived = false
                webSocket.sendWebSocket(null, WebSocketOpcode.PING)
            } else {
                Log.i(TAG, "moblink-streamer: $name: Ping timeout")
                webSocket.cancel()
            }
        }
    }

    private fun stopPingTimer() {
        pingTimer.stop()
    }

    private fun startTunnelInternal() {
        val address = address
        val port = port
        if (!identified || address == null || port == null) {
            return
        }
        reportTunnelRemoved()
        executeStartTunnel(address, port) { id, relayName, tunnelPort ->
            val host = webSocket.host
            if (host.isEmpty()) {
                Log.i(TAG, "moblink-streamer: $relayName: Missing relay host")
            } else {
                tunnelEndpointHost = host
                tunnelEndpointPort = tunnelPort
                streamer?.delegate?.moblinkStreamerTunnelAdded(host, tunnelPort, id, relayName)
            }
        }
    }

    private fun reportTunnelRemoved() {
        val host = tunnelEndpointHost
        val port = tunnelEndpointPort
        if (host != null && port != null) {
            streamer?.delegate?.moblinkStreamerTunnelRemoved(host, port)
        }
        tunnelEndpointHost = null
        tunnelEndpointPort = null
    }

    private fun executeStartTunnel(
        address: String,
        port: Int,
        onSuccess: (UUID, String, Int) -> Unit,
    ) {
        Log.i(TAG, "moblink-streamer: $name: Starting tunnel to destination $address:$port")
        performRequest(
            data = MoblinkRequest.StartTunnel(address = address, port = port.toUShort()),
            onSuccess = { response ->
                if (response is MoblinkResponse.StartTunnel) {
                    onSuccess(relayId, name, response.port.toInt())
                }
            },
            onError = { error ->
                Log.i(TAG, "moblink-streamer: $name: Start tunnel failed with $error")
            },
        )
    }

    private fun handleIdentify(relayId: UUID, name: String, authentication: String) {
        if (authentication == remoteControlHashPassword(challenge, salt, password)) {
            streamer?.removeRelay(relayId)
            this.relayId = relayId
            this.name = name.substringBefore("\n").trim().take(30)
            identified = true
            send(MoblinkMessageToRelay.Identified(result = MoblinkResult.ok))
            startTunnelInternal()
            updateStatus()
        } else {
            send(MoblinkMessageToRelay.Identified(result = MoblinkResult.wrongPassword))
            throw IllegalStateException("Relay sent wrong password")
        }
    }

    private fun handleResponse(id: Int, result: MoblinkResult, data: MoblinkResponse?) {
        if (!identified) {
            throw IllegalStateException("Relay not identified")
        }
        val request = requests.remove(id)
        if (request == null) {
            Log.i(TAG, "moblink-streamer: $name: Unexpected id in response")
            return
        }
        when (result) {
            MoblinkResult.ok -> request.onSuccess(data)
            MoblinkResult.wrongPassword -> request.onError("Wrong password")
            MoblinkResult.notIdentified -> Log.i(TAG, "moblink-streamer: $name: Not identified")
            MoblinkResult.alreadyIdentified -> Log.i(TAG, "moblink-streamer: $name: Already identified")
            MoblinkResult.unknownRequest -> Log.i(TAG, "moblink-streamer: $name: Unknown request")
        }
    }

    private fun performRequest(
        data: MoblinkRequest,
        onSuccess: (MoblinkResponse?) -> Unit,
        onError: (String) -> Unit,
    ) {
        val id = getNextId()
        requests[id] = RequestResponse(onSuccess = onSuccess, onError = onError)
        send(MoblinkMessageToRelay.Request(id = id, data = data))
    }

    private fun getNextId(): Int {
        nextId += 1
        return nextId
    }

    private fun send(message: MoblinkMessageToRelay) {
        val text = message.toJson() ?: return
        webSocket.sendWebSocket(text.toByteArray(Charsets.UTF_8), WebSocketOpcode.TEXT)
    }
}

private val idStorage = SimpleStringStorage(key = "moblinkServerId")

class MoblinkStreamer(
    private val context: Context,
    private val port: Int,
    private val password: String,
    private val name: String,
) {
    private var server: ServerSocket? = null
    var connectionErrorMessage = ""
    private var retryStartTimer = MainTimer()
    internal var delegate: MoblinkStreamerDelegate? = null
    private val relays = mutableListOf<MoblinkServerRelay>()
    private var destinationAddress: String? = null
    private var destinationPort: Int? = null
    private val mainScope = CoroutineScope(Dispatchers.Main)
    private val serverScope = CoroutineScope(Dispatchers.IO)
    private var serverJob: Job? = null
    private var nsdManager: NsdManager? = null
    private var registrationListener: NsdManager.RegistrationListener? = null

    init {
        if (idStorage.get().isEmpty()) {
            idStorage.set(UUID.randomUUID().toString())
        }
    }

    fun start(delegate: MoblinkStreamerDelegate) {
        stop()
        Log.d(TAG, "moblink-streamer: start")
        this.delegate = delegate
        startInternal()
    }

    fun stop() {
        Log.d(TAG, "moblink-streamer: stop")
        serverJob?.cancel()
        serverJob = null
        runCatching { server?.close() }
        server = null
        unregisterService()
        stopRetryStartTimer()
        for (relay in relays.toList()) {
            relay.stop()
        }
        relays.clear()
        delegate = null
    }

    fun restartTunnel(relayId: UUID) {
        val relay = relays.firstOrNull { it.relayId == relayId } ?: return
        relay.restartTunnel()
    }

    fun startTunnels(address: String, port: Int) {
        destinationAddress = address
        destinationPort = port
        for (relay in relays.toList()) {
            relay.startTunnel(address, port)
        }
    }

    fun stopTunnels() {
        destinationAddress = null
        destinationPort = null
        for (relay in relays.toList()) {
            relay.stopTunnel()
        }
    }

    fun getStatuses(): List<Triple<String, Int?, MoblinkThermalState?>> {
        return relays
            .sortedBy { it.name }
            .map { Triple(it.name, it.batteryPercentage, it.thermalState) }
    }

    fun updateStatus() {
        for (relay in relays.toList()) {
            relay.updateStatus()
        }
    }

    private fun startInternal() {
        try {
            val listener = ServerSocket()
            listener.reuseAddress = true
            listener.bind(InetSocketAddress(port))
            server = listener
            registerService()
            serverJob = serverScope.launch {
                while (isActive) {
                    val socket = try {
                        listener.accept()
                    } catch (e: IOException) {
                        Log.d(TAG, "moblink-streamer: Failed to accept with error $e")
                        null
                    } ?: break
                    try {
                        val connection = performWebSocketHandshake(socket)
                        if (connection != null) {
                            mainScope.launch {
                                handleNewConnection(connection)
                            }
                        }
                    } catch (e: IOException) {
                        Log.d(TAG, "moblink-streamer: Failed to upgrade connection with error $e")
                        runCatching { socket.close() }
                    }
                }
            }
            stopRetryStartTimer()
        } catch (e: Exception) {
            Log.d(TAG, "moblink-streamer: Failed to start server with error $e")
            connectionErrorMessage = e.message ?: ""
            startRetryStartTimer()
        }
    }

    private fun startRetryStartTimer() {
        retryStartTimer.startSingleShot(timeout = 5.0) {
            startInternal()
        }
    }

    private fun stopRetryStartTimer() {
        retryStartTimer.stop()
    }

    private fun handleNewConnection(connection: WebSocketConnection) {
        Log.d(TAG, "moblink-streamer: Relay connected")
        receivePacket(connection)
        val relay = MoblinkServerRelay(webSocket = connection, password = password, streamer = this)
        relay.start()
        relays.add(relay)
        val address = destinationAddress
        val port = destinationPort
        if (address != null && port != null) {
            relay.startTunnel(address, port)
        }
    }

    fun removeRelay(relayId: UUID) {
        val relay = relays.firstOrNull { it.relayId == relayId }
        if (relay != null) {
            Log.d(TAG, "moblink-streamer: Replacing relay ${relay.name} (id: $relayId)")
            relay.stop()
        }
        relays.removeAll { it.relayId == relayId }
    }

    private fun handlePong(webSocket: WebSocketConnection) {
        relays.firstOrNull { it.webSocket === webSocket }?.pongReceived = true
    }

    private fun handleDisconnected(webSocket: WebSocketConnection) {
        Log.d(TAG, "moblink-streamer: Relay disconnected")
        relays.firstOrNull { it.webSocket === webSocket }?.stop()
        relays.removeAll { it.webSocket === webSocket }
    }

    private fun receivePacket(webSocket: WebSocketConnection) {
        mainScope.launch {
            while (true) {
                val frame = withContext(Dispatchers.IO) { webSocket.readMessage() } ?: break
                when (frame.opcode) {
                    WebSocketOpcode.TEXT -> {
                        if (frame.data.isNotEmpty()) {
                            handleMessage(webSocket, frame.data)
                        } else {
                            break
                        }
                    }
                    WebSocketOpcode.PING -> {
                        webSocket.sendWebSocket(frame.data, WebSocketOpcode.PONG)
                    }
                    WebSocketOpcode.PONG -> {
                        handlePong(webSocket)
                    }
                    else -> break
                }
            }
            handleDisconnected(webSocket)
        }
    }

    private fun handleMessage(webSocket: WebSocketConnection, packet: ByteArray) {
        val text = String(packet, Charsets.UTF_8)
        val relay = relays.firstOrNull { it.webSocket === webSocket } ?: return
        relay.handleStringMessage(text)
    }

    private fun registerService() {
        val manager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager ?: return
        nsdManager = manager
        val serviceInfo = NsdServiceInfo()
        serviceInfo.serviceName = idStorage.get()
        serviceInfo.serviceType = moblinkBonjourType
        serviceInfo.port = port
        serviceInfo.setAttribute("name", name)
        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(registeredInfo: NsdServiceInfo) {}

            override fun onRegistrationFailed(failedInfo: NsdServiceInfo, errorCode: Int) {}

            override fun onServiceUnregistered(unregisteredInfo: NsdServiceInfo) {}

            override fun onUnregistrationFailed(failedInfo: NsdServiceInfo, errorCode: Int) {}
        }
        registrationListener = listener
        manager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, listener)
    }

    private fun unregisterService() {
        val manager = nsdManager
        val listener = registrationListener
        if (manager != null && listener != null) {
            runCatching { manager.unregisterService(listener) }
        }
        registrationListener = null
        nsdManager = null
    }

    private fun performWebSocketHandshake(socket: Socket): WebSocketConnection? {
        val input = socket.getInputStream()
        val head = readHttpHead(input) ?: return null
        val key = head.lineSequence()
            .firstOrNull { it.lowercase().startsWith("sec-websocket-key:") }
            ?.substringAfter(":")
            ?.trim()
            ?: return null
        val digest = MessageDigest.getInstance("SHA-1")
            .digest((key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").toByteArray(Charsets.US_ASCII))
        val accept = Base64.getEncoder().encodeToString(digest)
        val response = "HTTP/1.1 101 Switching Protocols\r\n" +
            "Upgrade: websocket\r\n" +
            "Connection: Upgrade\r\n" +
            "Sec-WebSocket-Accept: " + accept + "\r\n\r\n"
        val output = socket.getOutputStream()
        output.write(response.toByteArray(Charsets.US_ASCII))
        output.flush()
        return WebSocketConnection(socket)
    }

    private fun readHttpHead(input: InputStream): String? {
        val bytes = ByteArrayOutputStream()
        var state = 0
        while (true) {
            val byte = input.read()
            if (byte < 0) {
                return null
            }
            bytes.write(byte)
            if (state == 3 && byte == '\n'.code) {
                return bytes.toString(Charsets.ISO_8859_1)
            }
            state = when {
                byte == '\r'.code -> 1
                state == 1 && byte == '\n'.code -> 2
                state == 2 && byte == '\r'.code -> 3
                else -> 0
            }
            if (bytes.size() > 16384) {
                return null
            }
        }
    }
}

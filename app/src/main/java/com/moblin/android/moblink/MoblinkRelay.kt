package com.moblin.android.moblink

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.moblin.android.remotecontrol.remoteControlHashPassword
import com.moblin.android.various.MainTimer
import com.moblin.android.various.network.WebSocketClient
import com.moblin.android.various.network.WebSocketClientDelegate
import com.moblin.android.various.storages.SimpleStringStorage
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TAG = "MoblinkRelay"

private val moblinkRelayQueue = CoroutineScope(Dispatchers.IO + SupervisorJob())
private val mainScope = CoroutineScope(Dispatchers.Main)
private val relayIdStorage = SimpleStringStorage("srtlaRelayId")
private var relayId: String = ""

fun moblinkRelayLoadRelayId() {
    relayId = relayIdStorage.get()
    if (relayId.isEmpty()) {
        moblinkRelayResetId()
    }
}

fun getMoblinkRelayId(): String {
    return relayId
}

fun moblinkRelayResetId() {
    relayId = UUID.randomUUID().toString()
    relayIdStorage.set(relayId)
}

enum class MoblinkRelayState(val rawValue: String) {
    WaitingForStreamers("Waiting for streamers"),
    NoInterface("No interface"),
    Connecting("Connecting"),
    Connected("Connected"),
    WrongPassword("Wrong password"),
    UnknownError("Unknown error"),
    ;

    companion object {
        fun fromRawValue(value: String): MoblinkRelayState? {
            return entries.firstOrNull { it.rawValue == value }
        }
    }
}

private enum class RelayState(val rawValue: String) {
    None("None"),
    Connecting("Connecting"),
    Connected("Connected"),
    WaitingForCellular("Waiting for cellular"),
    WrongPassword("Wrong password"),
    UnknownError("Unknown error"),
    ;

    companion object {
        fun fromRawValue(value: String): RelayState? {
            return entries.firstOrNull { it.rawValue == value }
        }
    }
}

interface MoblinkRelayDelegate {
    fun moblinkRelayNewState(state: MoblinkRelayState)
    fun moblinkRelayGetStatus(): Pair<Int?, MoblinkThermalState?>
}

class MoblinkRelayInterface(
    val network: Network,
    val name: String,
    val index: Int,
    val type: Type,
) {
    enum class Type {
        cellular,
        wiredEthernet,
    }
}

private data class Endpoint(val host: String, val port: Int)

private class MoblinkRelayConnection(
    private var id: String,
    private val name: String,
    private var streamerUrl: String,
    private var password: String,
    private var delegate: MoblinkRelayDelegate?,
    var destinationInterface: MoblinkRelayInterface,
    private var relay: MoblinkRelayServer?,
) : WebSocketClientDelegate {
    private var webSocket: WebSocketClient = WebSocketClient(MoblinkRelayServer.applicationContext, streamerUrl)
    private var startTunnelId: Int? = null
    private var destination: Endpoint? = null
    private var streamerListener: DatagramSocket? = null
    private var streamerConnection: DatagramSocket? = null
    private var streamerAddress: InetSocketAddress? = null
    private var destinationConnection: DatagramSocket? = null
    var state: RelayState = RelayState.None
    private var started = false
    private val reconnectTimer = MainTimer()
    var isMain = false

    fun start() {
        if (started) {
            return
        }
        Log.i(TAG, "moblink-relay: $name: Start")
        started = true
        startInternal()
    }

    fun stop() {
        if (!started) {
            return
        }
        Log.i(TAG, "moblink-relay: $name: Stop")
        stopInternal()
        started = false
    }

    private fun startInternal() {
        if (!started) {
            return
        }
        stopInternal()
        updateState(RelayState.Connecting)
        webSocket = WebSocketClient(MoblinkRelayServer.applicationContext, streamerUrl, false)
        webSocket.delegate = this
        webSocket.start()
    }

    private fun stopInternal() {
        reconnectTimer.stop()
        updateState(RelayState.None)
        webSocket.delegate = null
        webSocket.stop()
        stopTunnel()
    }

    private fun reconnect(reason: String) {
        Log.i(TAG, "moblink-relay: $name: Reconnecting soon with reason $reason")
        stopInternal()
        reconnectTimer.startSingleShot(5.0) {
            startInternal()
        }
    }

    private fun updateState(state: RelayState) {
        if (state == this.state) {
            return
        }
        Log.i(TAG, "moblink-relay: $name: State change ${this.state.rawValue} -> ${state.rawValue}")
        this.state = state
        relay?.relayStateChanged()
    }

    private fun send(message: MoblinkMessageToStreamer) {
        try {
            val json = message.toJson()
            webSocket.send(json)
        } catch (e: Exception) {
            Log.i(TAG, "moblink-relay: $name: Encode failed")
        }
    }

    private fun handleMessage(message: String) {
        try {
            when (val decoded = MoblinkMessageToRelay.fromJson(message)) {
                is MoblinkMessageToRelay.Hello -> {
                    handleHello(decoded.apiVersion, decoded.authentication)
                }
                is MoblinkMessageToRelay.Identified -> {
                    if (!handleIdentified(decoded.result)) {
                        Log.i(TAG, "moblink-relay: $name: Failed to identify")
                        return
                    }
                    updateState(RelayState.Connected)
                }
                is MoblinkMessageToRelay.Request -> {
                    handleRequest(decoded.id, decoded.data)
                }
                else -> {}
            }
        } catch (e: Exception) {
            Log.i(TAG, "moblink-relay: $name: Decode failed")
        }
    }

    private fun handleHello(apiVersion: String, authentication: MoblinkAuthentication) {
        val hash = remoteControlHashPassword(authentication.challenge, authentication.salt, password)
        val uuid = runCatching { UUID.fromString(id) }.getOrNull() ?: return
        send(MoblinkMessageToStreamer.Identify(uuid, name, hash))
    }

    private fun handleIdentified(result: MoblinkResult): Boolean {
        when (result) {
            MoblinkResult.ok -> return true
            MoblinkResult.wrongPassword -> {
                reconnect(reason = "Wrong password")
                updateState(RelayState.WrongPassword)
            }
            else -> {
                reconnect(reason = "Unknown error")
                updateState(RelayState.UnknownError)
            }
        }
        return false
    }

    private fun handleRequest(id: Int, data: MoblinkRequest) {
        when (data) {
            is MoblinkRequest.StartTunnel -> {
                handleStartTunnel(id, data.address, data.port.toInt())
            }
            is MoblinkRequest.Status -> {
                handleStatus(id)
            }
            else -> {}
        }
    }

    private fun handleStartTunnel(id: Int, address: String, port: Int) {
        stopTunnel()
        Log.i(TAG, "moblink-relay: $name: Start tunnel to $address:$port")
        destination = Endpoint(address, port)
        val listener = try {
            DatagramSocket(0)
        } catch (e: Exception) {
            Log.i(TAG, "moblink-relay: $name: Failed to create streamer listener with error $e")
            reconnect(reason = "Failed to create listener")
            return
        }
        streamerListener = listener
        updateState(RelayState.WaitingForCellular)
        send(
            MoblinkMessageToStreamer.Response(
                id,
                MoblinkResult.ok,
                MoblinkResponse.StartTunnel(listener.localPort.toUShort()),
            )
        )
        receiveStreamerPacket()
        val dest = destination
        if (dest == null) {
            reconnect(reason = "Failed to parse host and port")
            return
        }
        val socket = DatagramSocket()
        destinationConnection = socket
        try {
            socket.connect(InetSocketAddress(dest.host, dest.port))
        } catch (e: Exception) {
            reconnect(reason = "Destination connection failed")
            return
        }
        Log.d(TAG, "moblink-relay: $name: Destination state change to Ready")
        receiveDestinationPacket()
        updateState(RelayState.Connected)
        startTunnelId = id
    }

    private fun handleStatus(id: Int) {
        var batteryPercentage: Int? = null
        var thermalState: MoblinkThermalState? = null
        if (isMain) {
            val status = delegate?.moblinkRelayGetStatus()
            if (status != null) {
                batteryPercentage = status.first
                thermalState = status.second
            }
        }
        send(
            MoblinkMessageToStreamer.Response(
                id,
                MoblinkResult.ok,
                MoblinkResponse.Status(batteryPercentage, thermalState),
            )
        )
    }

    private fun stopTunnel() {
        runCatching { streamerListener?.close() }
        streamerListener = null
        streamerConnection = null
        streamerAddress = null
        runCatching { destinationConnection?.close() }
        destinationConnection = null
        startTunnelId = null
    }

    private fun handleNewListenerConnection(connection: DatagramSocket, address: InetSocketAddress) {
        streamerConnection = connection
        streamerAddress = address
    }

    private fun receiveStreamerPacket() {
        val socket = streamerListener ?: return
        moblinkRelayQueue.launch {
            val buffer = ByteArray(65536)
            while (isActive) {
                val packet = DatagramPacket(buffer, buffer.size)
                try {
                    socket.receive(packet)
                } catch (e: Exception) {
                    if (socket !== streamerListener) {
                        return@launch
                    }
                    Log.i(TAG, "moblink-relay: $name: Streamer receive error")
                    mainScope.launch {
                        reconnect(reason = "Streamer receive error")
                    }
                    return@launch
                }
                if (socket !== streamerListener) {
                    return@launch
                }
                if (packet.length > 0) {
                    val address = packet.socketAddress
                    if (address is InetSocketAddress) {
                        handleNewListenerConnection(socket, address)
                    }
                    handlePacketFromStreamer(packet.data.copyOf(packet.length))
                } else {
                    Log.i(TAG, "moblink-relay: $name: Streamer receive error")
                    mainScope.launch {
                        reconnect(reason = "Streamer receive error")
                    }
                    return@launch
                }
            }
        }
    }

    private fun handlePacketFromStreamer(packet: ByteArray) {
        val socket = destinationConnection ?: return
        runCatching {
            socket.send(DatagramPacket(packet, packet.size))
        }
    }

    private fun receiveDestinationPacket() {
        val socket = destinationConnection ?: return
        moblinkRelayQueue.launch {
            val buffer = ByteArray(65536)
            while (isActive) {
                val packet = DatagramPacket(buffer, buffer.size)
                try {
                    socket.receive(packet)
                } catch (e: Exception) {
                    if (socket !== destinationConnection) {
                        return@launch
                    }
                    Log.i(TAG, "moblink-relay: $name: Destination receive error $e")
                    mainScope.launch {
                        reconnect(reason = "Destination receive error")
                    }
                    return@launch
                }
                if (socket !== destinationConnection) {
                    return@launch
                }
                if (packet.length > 0) {
                    handlePacketFromDestination(packet.data.copyOf(packet.length))
                }
            }
        }
    }

    private fun handlePacketFromDestination(packet: ByteArray) {
        val socket = streamerConnection ?: return
        val address = streamerAddress ?: return
        runCatching {
            socket.send(DatagramPacket(packet, packet.size, address))
        }
    }

    override fun webSocketClientConnected(client: WebSocketClient) {}

    override fun webSocketClientDisconnected(client: WebSocketClient) {
        mainScope.launch {
            updateState(RelayState.Connecting)
            stopTunnel()
        }
    }

    override fun webSocketClientReceiveMessage(client: WebSocketClient, string: String) {
        mainScope.launch {
            handleMessage(string)
        }
    }
}

class MoblinkRelayServer(
    private val name: String,
    val streamerUrl: String,
    private val password: String,
    delegate: MoblinkRelayDelegate,
) {
    private var delegate: MoblinkRelayDelegate? = delegate
    private var relays: MutableList<MoblinkRelayConnection> = mutableListOf()
    private var started = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            mainScope.launch {
                handleNetworkPathUpdate()
            }
        }

        override fun onLost(network: Network) {
            mainScope.launch {
                handleNetworkPathUpdate()
            }
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            mainScope.launch {
                handleNetworkPathUpdate()
            }
        }
    }

    fun start() {
        started = true
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
            .build()
        runCatching {
            connectivityManager.registerNetworkCallback(request, networkCallback)
        }
        relayStateChanged()
    }

    fun stop() {
        started = false
        runCatching {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
        for (relay in relays) {
            relay.stop()
        }
        relays.clear()
    }

    fun relayStateChanged() {
        var state = MoblinkRelayState.NoInterface
        for (relay in relays) {
            when (relay.state) {
                RelayState.None -> {}
                RelayState.Connecting -> state = MoblinkRelayState.Connecting
                RelayState.Connected -> {
                    if (state == MoblinkRelayState.NoInterface) {
                        state = MoblinkRelayState.Connected
                    }
                }
                RelayState.WaitingForCellular -> {}
                RelayState.WrongPassword -> state = MoblinkRelayState.WrongPassword
                RelayState.UnknownError -> state = MoblinkRelayState.UnknownError
            }
        }
        delegate?.moblinkRelayNewState(state)
    }

    private fun makeRelayId(networkInterface: MoblinkRelayInterface): String {
        return if (networkInterface.type == MoblinkRelayInterface.Type.cellular) {
            relayId
        } else {
            val bytes = (relayId + networkInterface.name).toByteArray()
            UUID.nameUUIDFromBytes(bytes).toString()
        }
    }

    private fun makeRelayName(networkInterface: MoblinkRelayInterface): String {
        return if (networkInterface.type == MoblinkRelayInterface.Type.cellular) {
            name
        } else {
            "$name-${networkInterface.index}"
        }
    }

    private fun handleNetworkPathUpdate() {
        if (!started) {
            return
        }
        val newRelays = mutableListOf<MoblinkRelayConnection>()
        for (networkInterface in availableInterfaces()) {
            val existing = relays.firstOrNull { it.destinationInterface == networkInterface }
            if (existing != null) {
                newRelays.add(existing)
            } else {
                val relay = MoblinkRelayConnection(
                    id = makeRelayId(networkInterface),
                    name = makeRelayName(networkInterface),
                    streamerUrl = streamerUrl,
                    password = password,
                    delegate = delegate,
                    destinationInterface = networkInterface,
                    relay = this,
                )
                relay.start()
                newRelays.add(relay)
            }
        }
        for (relay in relays) {
            if (newRelays.none { it.destinationInterface == relay.destinationInterface }) {
                relay.stop()
            }
        }
        var mainRelay: MoblinkRelayConnection? = newRelays.firstOrNull()
        for (relay in newRelays) {
            relay.isMain = false
            if (relay.destinationInterface.type == MoblinkRelayInterface.Type.cellular) {
                mainRelay = relay
                break
            }
        }
        mainRelay?.isMain = true
        relays = newRelays
        relayStateChanged()
    }

    private fun availableInterfaces(): List<MoblinkRelayInterface> {
        val result = mutableListOf<MoblinkRelayInterface>()
        val networks = runCatching { connectivityManager.allNetworks }.getOrNull() ?: return result
        for (network in networks) {
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: continue
            val isCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
            val isWiredEthernet = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            if (!isCellular && !isWiredEthernet) {
                continue
            }
            val type = if (isCellular) {
                MoblinkRelayInterface.Type.cellular
            } else {
                MoblinkRelayInterface.Type.wiredEthernet
            }
            val interfaceName = connectivityManager.getLinkProperties(network)?.interfaceName
                ?: network.toString()
            result.add(
                MoblinkRelayInterface(
                    network = network,
                    name = interfaceName,
                    index = network.networkHandle.toInt(),
                    type = type,
                )
            )
        }
        return result
    }

    private val connectivityManager: ConnectivityManager
        get() = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    companion object {
        lateinit var applicationContext: Context
    }
}

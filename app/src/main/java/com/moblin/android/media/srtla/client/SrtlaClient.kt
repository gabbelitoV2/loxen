package com.moblin.android.media.srtla.client

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.moblin.android.media.srtla.common.isSrtDataPacket
import com.moblin.android.various.BondingConnection
import com.moblin.android.various.SimpleTimer
import com.moblin.android.various.network.DnsLookupFamily
import com.moblin.android.various.network.performDnsLookup
import com.moblin.android.various.settings.SettingsDnsLookupStrategy
import com.moblin.android.various.settings.SettingsNetworkInterfaceName
import com.moblin.android.various.settings.SettingsStreamSrtConnectionPriorities
import com.moblin.android.various.settings.SettingsStreamSrtConnectionPriority
import com.moblin.android.various.settings.SettingsStreamSrtImplementation
import java.net.URI
import java.util.UUID
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

interface SrtlaDelegate {
    fun srtlaReady(port: Int)

    fun srtlaError(message: String)

    fun srtlaReceivedPacket(packet: ByteArray)

    fun moblinkStreamerDestinationAddress(address: String, port: Int)

    fun moblinkStreamerRestartTunnel(relayId: UUID)
}

private enum class State {
    idle,
    waitForRemoteSocketConnected,
    waitForProbe,
    waitForGroupId,
    waitForRegistered,
    waitForLocalSocketListening,
    running,
}

class SrtlaNetworkInterfaces {
    var names: MutableMap<String, String> = mutableMapOf()
}

val srtlaClientQueue: CoroutineDispatcher =
    Executors.newSingleThreadExecutor().asCoroutineDispatcher()

private val srtlaClientScope = CoroutineScope(srtlaClientQueue)

private const val tag = "SrtlaClient"

var srtlaConnectivityManager: ConnectivityManager? = null

class SrtlaClient(
    delegate: SrtlaDelegate,
    passThrough: Boolean,
    mpegtsPacketsPerPacket: Int,
    packetPadding: Boolean,
    networkInterfaceNames: List<SettingsNetworkInterfaceName>,
    connectionPriorities: SettingsStreamSrtConnectionPriorities,
    srtImplementation: SettingsStreamSrtImplementation,
) : RemoteConnectionDelegate {
    private var remoteConnections: MutableList<RemoteConnection> = mutableListOf()
    private var localListener: LocalListener? = null
    private val delegate: SrtlaDelegate?
    private val passThrough: Boolean
    private var connectTimer: SimpleTimer = SimpleTimer(srtlaClientQueue)
    private var state: State = State.idle
        set(value) {
            Log.d(tag, "srtla: State $field -> $value")
            field = value
        }
    private var networkPathMonitor: ConnectivityManager.NetworkCallback? = null
    private var networkPathInterfaces: MutableMap<Network, Int> = mutableMapOf()
    private val mpegtsPacketsPerPacket: Int
    private val packetPadding: Boolean
    private var host: String = ""
    private var port: Int = 0
    private var groupId: ByteArray? = null
    private var totalByteCount: Long = 0
    private var networkInterfaces: SrtlaNetworkInterfaces
    private var connectionPriorities: MutableList<SettingsStreamSrtConnectionPriority>
    private var latestFlushDataPacketsTime: Long = System.nanoTime()
    private val srtImplementation: SettingsStreamSrtImplementation

    init {
        this.delegate = delegate
        this.passThrough = passThrough
        this.mpegtsPacketsPerPacket = mpegtsPacketsPerPacket
        this.packetPadding = packetPadding
        networkInterfaces = SrtlaNetworkInterfaces()
        this.connectionPriorities = mutableListOf()
        this.srtImplementation = srtImplementation
        setNetworkInterfaceNames(networkInterfaceNames)
        updateConnectionPriorities(connectionPriorities)
        Log.d(tag, "srtla: SRT instead of SRTLA: $passThrough")
        if (passThrough) {
            remoteConnections.add(
                RemoteConnection(
                    type = null,
                    mpegtsPacketsPerPacket = mpegtsPacketsPerPacket,
                    packetPadding = packetPadding,
                    networkInterface = null,
                    networkInterfaces = networkInterfaces,
                    priority = 1.0f,
                )
            )
        }
    }

    fun start(uri: String, timeout: Double, dnsLookupStrategy: SettingsDnsLookupStrategy) {
        srtlaClientScope.launch {
            val url = runCatching { URI(uri) }.getOrNull()
            val parsedHost = url?.host
            val parsedPort = url?.port
            if (url == null || parsedHost == null || parsedPort == null || parsedPort < 0) {
                Log.i(tag, "srtla: Malformed URL")
                return@launch
            }
            var host = parsedHost
            val port = parsedPort
            if (!isIpAddress(host)) {
                Log.i(tag, "dns: Lookup strategy $dnsLookupStrategy")
                host = when (dnsLookupStrategy) {
                    SettingsDnsLookupStrategy.ipv4 ->
                        performDnsLookup(host = host, family = DnsLookupFamily.ipv4) ?: host
                    SettingsDnsLookupStrategy.ipv6 ->
                        performDnsLookup(host = host, family = DnsLookupFamily.ipv6) ?: host
                    SettingsDnsLookupStrategy.ipv4AndIpv6 ->
                        performDnsLookup(host = host, family = DnsLookupFamily.unspec) ?: host
                    SettingsDnsLookupStrategy.system -> host
                }
            }
            if (!passThrough) {
                val manager = srtlaConnectivityManager
                    ?: TODO("no ConnectivityManager available, set srtlaConnectivityManager from the Activity layer")
                val callback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        srtlaClientScope.launch {
                            handleNetworkPathUpdate(network)
                        }
                    }

                    override fun onLost(network: Network) {
                        srtlaClientScope.launch {
                            networkPathInterfaces.remove(network)
                            handleNetworkPathUpdate(network)
                        }
                    }

                    override fun onCapabilitiesChanged(
                        network: Network,
                        networkCapabilities: NetworkCapabilities,
                    ) {
                        srtlaClientScope.launch {
                            val type = when {
                                networkCapabilities.hasTransport(
                                    NetworkCapabilities.TRANSPORT_CELLULAR
                                ) -> NetworkCapabilities.TRANSPORT_CELLULAR
                                networkCapabilities.hasTransport(
                                    NetworkCapabilities.TRANSPORT_WIFI
                                ) -> NetworkCapabilities.TRANSPORT_WIFI
                                networkCapabilities.hasTransport(
                                    NetworkCapabilities.TRANSPORT_ETHERNET
                                ) -> NetworkCapabilities.TRANSPORT_ETHERNET
                                else -> null
                            } ?: return@launch
                            networkPathInterfaces[network] = type
                            handleNetworkPathUpdate(network)
                        }
                    }
                }
                networkPathMonitor = callback
                val request = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()
                manager.registerNetworkCallback(request, callback)
            }
            totalByteCount = 0
            this@SrtlaClient.host = host
            this@SrtlaClient.port = port
            Log.i(tag, "srtla: Using destination address $host and port $port")
            for (connection in remoteConnections) {
                startRemote(connection, host, port)
            }
            Log.d(tag, "srtla: Setting connect timer to $timeout seconds")
            connectTimer.startSingleShot(timeout) {
                Log.d(tag, "srtla: Connect timer expired after $timeout seconds")
                onDisconnected("connect timer expired")
            }
            state = State.waitForRemoteSocketConnected
            delegate?.moblinkStreamerDestinationAddress(host, port.coerceIn(0, 65535))
        }
    }

    fun stop() {
        srtlaClientScope.launch {
            for (connection in remoteConnections) {
                stopRemote(connection)
            }
            remoteConnections = mutableListOf()
            stopListener()
            cancelConnectTimer()
            state = State.idle
            val callback = networkPathMonitor
            if (callback != null) {
                srtlaConnectivityManager?.let { manager ->
                    runCatching { manager.unregisterNetworkCallback(callback) }
                }
                networkPathMonitor = null
            }
            networkPathInterfaces.clear()
        }
    }

    fun addMoblink(host: String, port: Int, id: UUID, name: String) {
        srtlaClientScope.launch {
            if (state == State.idle) {
                return@launch
            }
            val remoteConnection = RemoteConnection(
                type = null,
                mpegtsPacketsPerPacket = mpegtsPacketsPerPacket,
                packetPadding = packetPadding,
                networkInterface = null,
                networkInterfaces = networkInterfaces,
                priority = getRelayConnectionPriority(id),
                relayId = id,
                relayName = name,
            )
            startRemote(remoteConnection, host, port)
            groupId?.let { remoteConnection.register(it) }
            remoteConnections.add(remoteConnection)
        }
    }

    fun removeMoblink(host: String, port: Int) {
        srtlaClientScope.launch {
            if (state == State.idle) {
                return@launch
            }
            val remoteConnection = remoteConnections.firstOrNull {
                it.destinationHost == host && it.destinationPort == port
            } ?: return@launch
            stopRemote(remoteConnection)
            remoteConnections.removeAll { it === remoteConnection }
        }
    }

    fun setNetworkInterfaceNames(networkInterfaceNames: List<SettingsNetworkInterfaceName>) {
        srtlaClientScope.launch {
            networkInterfaces.names.clear()
            for (networkInterface in networkInterfaceNames) {
                networkInterfaces.names[networkInterface.interfaceName] = networkInterface.name
            }
        }
    }

    fun setConnectionPriorities(connectionPriorities: SettingsStreamSrtConnectionPriorities) {
        srtlaClientScope.launch {
            updateConnectionPriorities(connectionPriorities)
            for (connection in remoteConnections) {
                val relayId = connection.relayId
                if (relayId != null) {
                    connection.setPriority(getRelayConnectionPriority(relayId))
                } else {
                    val name = interfaceName(connection.type, connection.networkInterface)
                    connection.setPriority(getConnectionPriority(name))
                }
            }
        }
    }

    fun connectionStatistics(): List<BondingConnection> {
        val connections = mutableListOf<BondingConnection>()
        runBlocking(srtlaClientQueue) {
            for (connection in remoteConnections) {
                if (!connection.isEnabled()) {
                    continue
                }
                val byteCount = connection.getDataSentDelta() ?: continue
                connections.add(
                    BondingConnection(
                        connection.typeString,
                        byteCount,
                        connection.rtt,
                    )
                )
            }
        }
        return connections
    }

    fun logStatistics() {
        srtlaClientScope.launch {
            for (connection in remoteConnections) {
                connection.logStatistics()
            }
        }
    }

    fun getTotalByteCount(): Long {
        return runBlocking(srtlaClientQueue) {
            totalByteCount
        }
    }

    fun handleLocalPacket(packet: ByteArray) {
        val connection = selectRemoteConnection() ?: return
        connection.sendSrtPacket(packet)
        if (isSrtDataPacket(packet)) {
            val now = System.nanoTime()
            if (now - latestFlushDataPacketsTime > 15_000_000L) {
                latestFlushDataPacketsTime = now
                for (remoteConnection in remoteConnections) {
                    remoteConnection.flushDataPackets()
                }
            }
        }
        totalByteCount += packet.size.toLong()
    }

    private fun updateConnectionPriorities(
        connectionPriorities: SettingsStreamSrtConnectionPriorities,
    ) {
        this.connectionPriorities = mutableListOf()
        if (!connectionPriorities.enabled) {
            return
        }
        val lowestPriority = connectionPriorities.priorities
            .filter { it.enabled }
            .minByOrNull { it.priority }
            ?: return
        for (connectionPriority in connectionPriorities.priorities) {
            val priority = connectionPriority.clone()
            priority.priority = priority.priority - lowestPriority.priority + 1
            this.connectionPriorities.add(priority)
        }
    }

    private fun getConnectionPriority(name: String): Float {
        val priority = connectionPriorities.firstOrNull { it.name == name } ?: return 1f
        return if (priority.enabled) {
            priority.priority.toFloat()
        } else {
            0f
        }
    }

    private fun getRelayConnectionPriority(relayId: UUID): Float {
        val priority = connectionPriorities.firstOrNull { it.relayId == relayId } ?: return 1f
        return if (priority.enabled) {
            priority.priority.toFloat()
        } else {
            0f
        }
    }

    private fun handleNetworkPathUpdate(network: Network) {
        val newRemoteConnections = mutableListOf<RemoteConnection>()
        for (connection in remoteConnections) {
            val connectionNetwork = connection.networkInterface
            if (connectionNetwork != null) {
                if (networkPathInterfaces.containsKey(connectionNetwork)) {
                    newRemoteConnections.add(connection)
                } else {
                    stopRemote(connection)
                }
            } else {
                newRemoteConnections.add(connection)
            }
        }
        val interfaceTypes = listOf(
            NetworkCapabilities.TRANSPORT_CELLULAR,
            NetworkCapabilities.TRANSPORT_WIFI,
            NetworkCapabilities.TRANSPORT_ETHERNET,
        )
        for ((availableNetwork, type) in networkPathInterfaces) {
            if (type !in interfaceTypes) {
                continue
            }
            if (newRemoteConnections.any { it.networkInterface == availableNetwork }) {
                continue
            }
            val name = interfaceName(type, availableNetwork)
            val newConnection = RemoteConnection(
                type = type,
                mpegtsPacketsPerPacket = mpegtsPacketsPerPacket,
                packetPadding = packetPadding,
                networkInterface = availableNetwork,
                networkInterfaces = networkInterfaces,
                priority = getConnectionPriority(name),
            )
            newRemoteConnections.add(newConnection)
            startRemote(newConnection, host, port)
            groupId?.let { newConnection.register(it) }
        }
        remoteConnections = newRemoteConnections
            .sortedWith(
                compareBy {
                    when (it.type) {
                        NetworkCapabilities.TRANSPORT_CELLULAR -> 0
                        NetworkCapabilities.TRANSPORT_WIFI -> 1
                        else -> 2
                    }
                }
            )
            .toMutableList()
    }

    private fun startRemote(connection: RemoteConnection, host: String, port: Int) {
        connection.delegate = this
        connection.start(host, port)
    }

    private fun stopRemote(connection: RemoteConnection) {
        connection.stop("Stopping stream")
        connection.delegate = null
    }

    private fun startListener() {
        when (srtImplementation) {
            SettingsStreamSrtImplementation.moblin -> startListenerMoblin()
            SettingsStreamSrtImplementation.official -> startListenerOfficial()
        }
    }

    private fun startListenerMoblin() {
        state = State.running
        delegate?.srtlaReady(0)
        cancelConnectTimer()
    }

    private fun startListenerOfficial() {
        if (localListener != null) {
            return
        }
        val listener = LocalListener()
        localListener = listener
        listener.onReady = { port -> handleLocalReady(port) }
        listener.onError = { message -> handleLocalError(message) }
        listener.start()
        state = State.waitForLocalSocketListening
    }

    private fun stopListener() {
        localListener?.stop()
        localListener?.onReady = null
        localListener?.onError = null
        localListener = null
    }

    private fun handleLocalReady(port: Int) {
        if (state != State.waitForLocalSocketListening) {
            return
        }
        state = State.running
        delegate?.srtlaReady(port)
        cancelConnectTimer()
    }

    private fun cancelConnectTimer() {
        connectTimer.stop()
    }

    private fun handleLocalError(message: String) {
        onDisconnected(message)
    }

    private fun onDisconnected(message: String) {
        if (state == State.idle) {
            return
        }
        stop()
        delegate?.srtlaError(message)
        state = State.idle
    }

    private fun selectRemoteConnection(): RemoteConnection? {
        var selectedConnection: RemoteConnection? = null
        var selectedScore = -1
        for (connection in remoteConnections) {
            val score = connection.score()
            if (score > selectedScore) {
                selectedConnection = connection
                selectedScore = score
            }
        }
        return selectedConnection
    }

    override fun remoteConnectionOnSocketConnected(connection: RemoteConnection) {
        if (state != State.waitForRemoteSocketConnected && state != State.waitForProbe) {
            return
        }
        if (passThrough) {
            startListener()
        } else {
            connection.probe()
            state = State.waitForProbe
        }
    }

    override fun remoteConnectionOnRegNgp(connection: RemoteConnection) {
        if (state != State.waitForProbe) {
            return
        }
        connection.sendSrtlaReg1()
        state = State.waitForGroupId
    }

    override fun remoteConnectionOnReg2(groupId: ByteArray) {
        if (state != State.waitForGroupId) {
            return
        }
        this.groupId = groupId
        for (connection in remoteConnections) {
            connection.register(groupId)
        }
        state = State.waitForRegistered
    }

    override fun remoteConnectionOnRegistered() {
        if (state != State.waitForRegistered) {
            return
        }
        startListener()
    }

    override fun remoteConnectionPacketHandler(packet: ByteArray) {
        when (srtImplementation) {
            SettingsStreamSrtImplementation.moblin -> delegate?.srtlaReceivedPacket(packet)
            SettingsStreamSrtImplementation.official -> localListener?.sendPacket(packet)
        }
        totalByteCount += packet.size.toLong()
    }

    override fun remoteConnectionOnSrtAck(sn: UInt) {
        for (connection in remoteConnections) {
            connection.handleSrtAckSn(sn)
        }
    }

    override fun remoteConnectionOnSrtNak(sn: UInt) {
        for (connection in remoteConnections) {
            connection.handleSrtNakSn(sn)
        }
    }

    override fun remoteConnectionOnSrtlaAck(sn: UInt) {
        for (connection in remoteConnections) {
            connection.handleSrtlaAckSn(sn)
        }
    }

    override fun remoteConnectionOnMoblinkReconnect(connection: RemoteConnection) {
        val relayId = connection.relayId ?: return
        delegate?.moblinkStreamerRestartTunnel(relayId)
    }
}

private fun interfaceName(type: Int?, network: Network?): String =
    when (type) {
        NetworkCapabilities.TRANSPORT_CELLULAR -> "Cellular"
        NetworkCapabilities.TRANSPORT_WIFI -> "WiFi"
        else -> network?.let {
            srtlaConnectivityManager?.getLinkProperties(it)?.interfaceName ?: ""
        } ?: ""
    }

private fun isIpAddress(host: String): Boolean {
    if (host.contains(':')) {
        return host.matches(Regex("^[0-9a-fA-F:.]+$"))
    }
    return host.matches(Regex("^\\d{1,3}(\\.\\d{1,3}){3}$"))
}

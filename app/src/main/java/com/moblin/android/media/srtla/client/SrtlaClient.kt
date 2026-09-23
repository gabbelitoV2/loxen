package com.moblin.android.media.srtla.client

import android.util.Log
import com.moblin.android.media.srtla.common.isSrtDataPacket
import com.moblin.android.platform.network.NWEndpoint
import com.moblin.android.platform.network.NWInterface
import com.moblin.android.platform.network.NWPath
import com.moblin.android.platform.network.NWPathMonitor
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
import kotlinx.coroutines.withContext

private const val TAG = "SrtlaClient"

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

val srtlaClientQueue: CoroutineDispatcher = Executors.newSingleThreadExecutor { runnable ->
    Thread(runnable, "com.eerimoq.srtla-client")
}.asCoroutineDispatcher()

private val srtlaClientDnsQueue: CoroutineDispatcher = Executors.newCachedThreadPool { runnable ->
    Thread(runnable, "com.eerimoq.srtla-client-dns").apply {
        isDaemon = true
    }
}.asCoroutineDispatcher()

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
    private val delegate: SrtlaDelegate? = delegate
    private val passThrough: Boolean = passThrough
    private val connectTimer = SimpleTimer(queue = srtlaClientQueue)
    private var state: State = State.idle
        set(value) {
            val oldValue = field
            field = value
            Log.d(TAG, "srtla: State $oldValue -> $value")
        }

    private val networkPathMonitor = NWPathMonitor()
    private val mpegtsPacketsPerPacket: Int = mpegtsPacketsPerPacket
    private val packetPadding: Boolean = packetPadding
    private var host: String = ""
    private var port: Int = 0
    private var groupId: ByteArray? = null

    private var totalByteCount: Long = 0
    private var networkInterfaces: SrtlaNetworkInterfaces = SrtlaNetworkInterfaces()
    private var connectionPriorities: MutableList<SettingsStreamSrtConnectionPriority> = mutableListOf()
    private var latestFlushDataPacketsTime = System.nanoTime()
    private val srtImplementation: SettingsStreamSrtImplementation = srtImplementation
    private var numberOfStops = 0

    init {
        setNetworkInterfaceNames(networkInterfaceNames = networkInterfaceNames)
        updateConnectionPriorities(connectionPriorities = connectionPriorities)
        Log.d(TAG, "srtla: SRT instead of SRTLA: $passThrough")
        if (passThrough) {
            remoteConnections.add(
                RemoteConnection(
                    type = null,
                    mpegtsPacketsPerPacket = mpegtsPacketsPerPacket,
                    packetPadding = packetPadding,
                    `interface` = null,
                    networkInterfaces = networkInterfaces,
                    priority = 1.0f,
                ),
            )
        }
    }

    fun start(uri: String, timeout: Double, dnsLookupStrategy: SettingsDnsLookupStrategy) {
        CoroutineScope(srtlaClientQueue).launch {
            val url = try {
                URI(uri)
            } catch (error: Exception) {
                null
            }
            val urlHost = url?.host?.removePrefix("[")?.removeSuffix("]")
            val port = url?.port ?: -1
            if (urlHost == null || urlHost.isEmpty() || port < 0) {
                Log.i(TAG, "srtla: Malformed URL")
                return@launch
            }
            var host: String = urlHost
            if (!isIpAddress(host)) {
                Log.i(TAG, "dns: Lookup strategy $dnsLookupStrategy")
                val family = when (dnsLookupStrategy) {
                    SettingsDnsLookupStrategy.ipv4 -> DnsLookupFamily.ipv4
                    SettingsDnsLookupStrategy.ipv6 -> DnsLookupFamily.ipv6
                    SettingsDnsLookupStrategy.ipv4AndIpv6 -> DnsLookupFamily.unspec
                    SettingsDnsLookupStrategy.system -> null
                }
                if (family != null) {
                    val numberOfStopsBeforeLookup = numberOfStops
                    val lookupHost: String = host
                    host = withContext(srtlaClientDnsQueue) {
                        performDnsLookup(host = lookupHost, family = family)
                    } ?: host
                    if (numberOfStops != numberOfStopsBeforeLookup) {
                        return@launch
                    }
                }
            }
            if (!passThrough) {
                networkPathMonitor.pathUpdateHandler = this@SrtlaClient::handleNetworkPathUpdate
                networkPathMonitor.start(queue = srtlaClientQueue)
            }
            totalByteCount = 0
            this@SrtlaClient.host = host
            this@SrtlaClient.port = port
            Log.i(TAG, "srtla: Using destination address $host and port $port")
            for (connection in remoteConnections) {
                startRemote(
                    connection = connection,
                    host = NWEndpoint.Host(host),
                    port = NWEndpoint.Port(port.coerceIn(0, 65535)),
                )
            }
            Log.d(TAG, "srtla: Setting connect timer to $timeout seconds")
            connectTimer.startSingleShot(timeout = timeout) {
                Log.d(TAG, "srtla: Connect timer expired after $timeout seconds")
                onDisconnected(message = "connect timer expired")
            }
            state = State.waitForRemoteSocketConnected
            delegate?.moblinkStreamerDestinationAddress(address = host, port = port.coerceIn(0, 65535))
        }
    }

    fun stop() {
        CoroutineScope(srtlaClientQueue).launch {
            numberOfStops += 1
            for (connection in remoteConnections) {
                stopRemote(connection = connection)
            }
            remoteConnections = mutableListOf()
            stopListener()
            cancelConnectTimer()
            state = State.idle
            networkPathMonitor.cancel()
        }
    }

    fun addMoblink(host: String, port: Int, id: UUID, name: String) {
        CoroutineScope(srtlaClientQueue).launch {
            if (state == State.idle) {
                return@launch
            }
            val remoteConnection = RemoteConnection(
                type = NWInterface.InterfaceType.other,
                mpegtsPacketsPerPacket = mpegtsPacketsPerPacket,
                packetPadding = packetPadding,
                `interface` = null,
                networkInterfaces = networkInterfaces,
                priority = getRelayConnectionPriority(relayId = id),
                relayId = id,
                relayName = name,
            )
            startRemote(
                connection = remoteConnection,
                host = NWEndpoint.Host(host),
                port = NWEndpoint.Port(port.coerceIn(0, 65535)),
            )
            val groupId = groupId
            if (groupId != null) {
                remoteConnection.register(groupId = groupId)
            }
            remoteConnections.add(remoteConnection)
        }
    }

    fun removeMoblink(host: String, port: Int) {
        CoroutineScope(srtlaClientQueue).launch {
            if (state == State.idle) {
                return@launch
            }
            val remoteConnection = remoteConnections.firstOrNull {
                it.destinationHost == NWEndpoint.Host(host) && it.destinationPort == NWEndpoint.Port(port)
            } ?: return@launch
            stopRemote(connection = remoteConnection)
            remoteConnections.removeAll { it === remoteConnection }
        }
    }

    fun setNetworkInterfaceNames(networkInterfaceNames: List<SettingsNetworkInterfaceName>) {
        CoroutineScope(srtlaClientQueue).launch {
            networkInterfaces.names.clear()
            for (networkInterface in networkInterfaceNames) {
                networkInterfaces.names[networkInterface.interfaceName] = networkInterface.name
            }
        }
    }

    fun setConnectionPriorities(connectionPriorities: SettingsStreamSrtConnectionPriorities) {
        CoroutineScope(srtlaClientQueue).launch {
            updateConnectionPriorities(connectionPriorities = connectionPriorities)
            for (connection in remoteConnections) {
                val relayId = connection.relayId
                if (relayId != null) {
                    connection.setPriority(priority = getRelayConnectionPriority(relayId = relayId))
                } else {
                    val name = interfaceName(type = connection.type, `interface` = connection.`interface`)
                    connection.setPriority(priority = getConnectionPriority(name = name))
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
                        name = connection.typeString,
                        usage = byteCount,
                        rtt = connection.rtt,
                    ),
                )
            }
        }
        return connections
    }

    fun logStatistics() {
        CoroutineScope(srtlaClientQueue).launch {
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
        connection.sendSrtPacket(packet = packet)
        if (isSrtDataPacket(packet = packet)) {
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

    private fun updateConnectionPriorities(connectionPriorities: SettingsStreamSrtConnectionPriorities) {
        this.connectionPriorities = mutableListOf()
        if (!connectionPriorities.enabled) {
            return
        }
        val lowestPriority = connectionPriorities.priorities
            .filter { priority -> priority.enabled }
            .minByOrNull { priority -> priority.priority }
            ?: return
        for (connectionPriority in connectionPriorities.priorities) {
            val priority = connectionPriority.clone()
            priority.priority -= lowestPriority.priority
            priority.priority += 1
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

    private fun handleNetworkPathUpdate(path: NWPath) {
        val newRemoteConnections = mutableListOf<RemoteConnection>()
        for (connection in remoteConnections) {
            val connectionInterface = connection.`interface`
            if (connectionInterface != null) {
                if (path.uniqueAvailableInterfaces().contains(connectionInterface)) {
                    newRemoteConnections.add(connection)
                } else {
                    stopRemote(connection = connection)
                }
            } else {
                newRemoteConnections.add(connection)
            }
        }
        val interfaceTypes = listOf(
            NWInterface.InterfaceType.cellular,
            NWInterface.InterfaceType.wifi,
            NWInterface.InterfaceType.wiredEthernet,
        )
        for (availableInterface in path.uniqueAvailableInterfaces()) {
            if (!interfaceTypes.contains(availableInterface.type)) {
                continue
            }
            if (newRemoteConnections.any { it.`interface` == availableInterface }) {
                continue
            }
            val name = interfaceName(type = availableInterface.type, `interface` = availableInterface)
            newRemoteConnections.add(
                RemoteConnection(
                    type = availableInterface.type,
                    mpegtsPacketsPerPacket = mpegtsPacketsPerPacket,
                    packetPadding = packetPadding,
                    `interface` = availableInterface,
                    networkInterfaces = networkInterfaces,
                    priority = getConnectionPriority(name = name),
                ),
            )
            startRemote(
                connection = newRemoteConnections.last(),
                host = NWEndpoint.Host(host),
                port = NWEndpoint.Port(port.coerceIn(0, 65535)),
            )
            val groupId = groupId
            if (groupId != null) {
                newRemoteConnections.last().register(groupId = groupId)
            }
        }
        remoteConnections = newRemoteConnections.sortedBy { connection ->
            when (connection.type) {
                NWInterface.InterfaceType.cellular -> 0
                NWInterface.InterfaceType.wifi -> 1
                else -> 2
            }
        }.toMutableList()
    }

    private fun startRemote(connection: RemoteConnection, host: NWEndpoint.Host, port: NWEndpoint.Port) {
        connection.delegate = this
        connection.start(host = host, port = port)
    }

    private fun stopRemote(connection: RemoteConnection) {
        connection.stop(reason = "Stopping stream")
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
        delegate?.srtlaReady(port = 0)
        cancelConnectTimer()
    }

    private fun startListenerOfficial() {
        if (localListener != null) {
            return
        }
        localListener = LocalListener()
        localListener!!.onReady = ::handleLocalReady
        localListener!!.onError = ::handleLocalError
        localListener!!.start()
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
        delegate?.srtlaReady(port = port)
        cancelConnectTimer()
    }

    private fun cancelConnectTimer() {
        connectTimer.stop()
    }

    private fun handleLocalError(message: String) {
        onDisconnected(message = message)
    }

    private fun onDisconnected(message: String) {
        if (state == State.idle) {
            return
        }
        stop()
        delegate?.srtlaError(message = message)
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
            connection.register(groupId = groupId)
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
            SettingsStreamSrtImplementation.moblin -> delegate?.srtlaReceivedPacket(packet = packet)
            SettingsStreamSrtImplementation.official -> localListener?.sendPacket(packet = packet)
        }
        totalByteCount += packet.size.toLong()
    }

    override fun remoteConnectionOnSrtAck(sn: UInt) {
        for (connection in remoteConnections) {
            connection.handleSrtAckSn(sn = sn)
        }
    }

    override fun remoteConnectionOnSrtNak(sn: UInt) {
        for (connection in remoteConnections) {
            connection.handleSrtNakSn(sn = sn)
        }
    }

    override fun remoteConnectionOnSrtlaAck(sn: UInt) {
        for (connection in remoteConnections) {
            connection.handleSrtlaAckSn(sn = sn)
        }
    }

    override fun remoteConnectionOnMoblinkReconnect(connection: RemoteConnection) {
        val relayId = connection.relayId ?: return
        delegate?.moblinkStreamerRestartTunnel(relayId = relayId)
    }
}

private fun interfaceName(type: NWInterface.InterfaceType?, `interface`: NWInterface?): String {
    return when (type) {
        NWInterface.InterfaceType.cellular -> "Cellular"
        NWInterface.InterfaceType.wifi -> "WiFi"
        else -> `interface`?.name ?: ""
    }
}

private fun isIpAddress(host: String): Boolean {
    if (host.contains(':')) {
        return host.matches(Regex("^[0-9a-fA-F:.]+$"))
    }
    return host.matches(Regex("^\\d{1,3}(\\.\\d{1,3}){3}$"))
}

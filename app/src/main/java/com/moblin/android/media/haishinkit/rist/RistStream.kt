package com.moblin.android.media.haishinkit.rist

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import com.moblin.android.common.various.formatBytesPerSecond
import com.moblin.android.media.adaptivebitrate.AdaptiveBitrateDelegate
import com.moblin.android.media.adaptivebitrate.AdaptiveBitrateRistExperiment
import com.moblin.android.media.adaptivebitrate.StreamStats
import com.moblin.android.media.haishinkit.media.Processor
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.mpeg.MpegTsWriter
import com.moblin.android.media.haishinkit.mpeg.MpegTsWriterDelegate
import com.moblin.android.media.haishinkit.util.Atomic
import com.moblin.android.various.BondingConnection
import com.moblin.android.various.SimpleTimer
import java.net.URI
import java.util.UUID
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private val ristDispatcher = Executors.newSingleThreadExecutor { runnable ->
    Thread(runnable, "com.moblin.android.rist")
}.asCoroutineDispatcher()

private val ristScope = CoroutineScope(ristDispatcher)

private val weigthTargetBitrate: Int = 10_000_000

private const val tag = "RistStream"

private const val ristRemotePeerTag = "RistRemotePeer"

var ristApplicationContext: Context? = null

private val ristConnectivityManager: ConnectivityManager?
    get() = ristApplicationContext?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

private enum class RistPeerState {
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
}

private data class NetworkInterfaceInfo(val name: String, val type: Int?)

data class RistEndpoint(val host: String, val port: Int)

private class RistRemotePeer(
    val interfaceName: String,
    val interfaceType: Int?,
    val relayEndpoint: RistEndpoint?,
    val peer: RistPeer,
    val stream: RistStream,
) : AdaptiveBitrateDelegate {
    var stats: RistSenderStats? = null
    var adaptiveWeight: AdaptiveBitrateRistExperiment? = null
    private var state: RistPeerState = RistPeerState.CONNECTING
    private var connectingTimer = SimpleTimer(ristDispatcher)

    init {
        adaptiveWeight = AdaptiveBitrateRistExperiment(
            targetBitrate = weigthTargetBitrate,
            delegate = this,
        )
        connectingTimer.startSingleShot(timeout = 5.0) {
            Log.i(ristRemotePeerTag, "rist: Failed to connect to server")
            state = RistPeerState.DISCONNECTED
            stream.checkDisconnected()
        }
    }

    fun close() {
        stopConnectingTimer()
    }

    fun bondingConnectionName(): String {
        return when (interfaceType) {
            NetworkCapabilities.TRANSPORT_CELLULAR -> "Cellular"
            NetworkCapabilities.TRANSPORT_WIFI -> "WiFi"
            else -> interfaceName
        }
    }

    fun setConnected() {
        state = RistPeerState.CONNECTED
        stopConnectingTimer()
    }

    fun setDisconnected() {
        state = RistPeerState.DISCONNECTED
    }

    fun isConnected(): Boolean {
        return state == RistPeerState.CONNECTED
    }

    fun isDisconnected(): Boolean {
        return state == RistPeerState.DISCONNECTED
    }

    private fun stopConnectingTimer() {
        connectingTimer.stop()
    }

    override fun adaptiveBitrateSetVideoStreamBitrate(bitrate: Int) {}
}

private enum class RistStreamState {
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
}

interface RistStreamDelegate {
    fun ristStreamOnConnected()
    fun ristStreamOnDisconnected()
    fun ristStreamRelayDestinationAddress(address: String, port: Int)
}

class RistStream(
    private val processor: Processor,
    timecodesEnabled: Boolean,
    delegate: RistStreamDelegate,
) : MpegTsWriterDelegate, RistSenderContextDelegate {
    private var context: RistSenderContext? = null
    private var peers: MutableList<RistRemotePeer> = mutableListOf()
    private val writer: MpegTsWriter = MpegTsWriter(timecodesEnabled, newSrt = false)
    private var networkPathMonitor: ConnectivityManager.NetworkCallback? = null
    private var bonding: Boolean = false
    private var url: String = ""
    private var state: RistStreamState = RistStreamState.CONNECTING
    private val ristDelegate: RistStreamDelegate = delegate
    private val totalByteCount = Atomic<Long>(0L)

    init {
        writer.delegate = this
    }

    fun start(url: String, bonding: Boolean) {
        ristScope.launch {
            startInternal(url, bonding)
        }
    }

    fun stop() {
        ristScope.launch {
            stopInternal()
        }
    }

    fun addMoblink(endpoint: RistEndpoint, id: UUID, name: String) {
        ristScope.launch {
            addMoblinkInternal(endpoint, id, name)
        }
    }

    fun removeMoblink(endpoint: RistEndpoint) {
        ristScope.launch {
            removeMoblinkInternal(endpoint)
        }
    }

    fun getSpeed(): ULong {
        var totalBandwidth: ULong = 0u
        runBlocking(ristDispatcher) {
            for (peer in peers) {
                val stats = peer.stats
                if (stats != null) {
                    totalBandwidth += stats.bandwidth + stats.retryBandwidth
                }
            }
        }
        return totalBandwidth
    }

    fun getTotalByteCount(): Long {
        return totalByteCount.value
    }

    fun connectionStatistics(): List<BondingConnection> {
        val connections = mutableListOf<BondingConnection>()
        runBlocking(ristDispatcher) {
            for (peer in peers) {
                val connection = BondingConnection(
                    name = peer.bondingConnectionName(),
                    usage = 0L,
                    rtt = null,
                )
                val stats = peer.stats
                if (stats != null) {
                    connection.usage = (stats.bandwidth + stats.retryBandwidth).toLong()
                    connection.rtt = stats.rtt.toInt()
                }
                connections.add(connection)
            }
        }
        return connections
    }

    fun getStats(): List<RistSenderStats> {
        return runBlocking(ristDispatcher) {
            peers.filter { it.stats != null }.map { it.stats!! }
        }
    }

    fun updateConnectionsWeights() {
        ristScope.launch {
            updateConnectionsWeightsInternal()
        }
    }

    fun checkConnected() {
        if (state != RistStreamState.CONNECTING) {
            return
        }
        for (peer in peers) {
            if (peer.isConnected()) {
                state = RistStreamState.CONNECTED
                ristDelegate.ristStreamOnConnected()
                break
            }
        }
    }

    fun checkDisconnected() {
        for (peer in peers) {
            if (!peer.isDisconnected()) {
                return
            }
        }
        Log.i(tag, "rist: All peers disconnected")
        state = RistStreamState.DISCONNECTED
        ristDelegate.ristStreamOnDisconnected()
    }

    private fun startInternal(url: String, bonding: Boolean) {
        state = RistStreamState.CONNECTING
        this.url = url
        this.bonding = bonding
        val context = RistSenderContext()
        if (context == null) {
            Log.i(tag, "rist: Failed to create context")
            return
        }
        context.delegate = this
        this.context = context
        if (bonding) {
            startNetworkPathMonitor()
        } else {
            addPeer(url, "", null)
        }
        if (!context.start()) {
            Log.i(tag, "rist: Failed to start")
            return
        }
        processorPipelineQueue.launch {
            Unit
            writer.startRunning()
        }
        val uri = runCatching { URI(url) }.getOrNull() ?: return
        val host = uri.host ?: return
        if (uri.port == -1) {
            return
        }
        ristDelegate.ristStreamRelayDestinationAddress(host, uri.port)
    }

    private fun stopInternal() {
        state = RistStreamState.DISCONNECTED
        stopNetworkPathMonitor()
        processorPipelineQueue.launch {
            writer.stopRunning()
            Unit
        }
        peers.forEach { it.close() }
        peers.clear()
        context?.delegate = null
        context?.stop()
        context = null
    }

    private fun addMoblinkInternal(endpoint: RistEndpoint, moblinkId: UUID, name: String) {
        if (!bonding) {
            return
        }
        addPeer(
            url = makeRistMoblinkBondingUrl(url, endpoint),
            interfaceName = name,
            interfaceType = null,
            relayEndpoint = endpoint,
        )
    }

    private fun removeMoblinkInternal(endpoint: RistEndpoint) {
        if (!bonding) {
            return
        }
        peers.removeAll { it.relayEndpoint == endpoint }
    }

    private fun updateConnectionsWeightsInternal() {
        for (peer in peers) {
            val stats = peer.stats
            val adaptiveWeight = peer.adaptiveWeight
            if (stats == null || adaptiveWeight == null) {
                continue
            }
            adaptiveWeight.update(
                StreamStats(
                    rttMs = stats.rtt.toDouble(),
                    packetsInFlight = 10.0,
                    transportBitrate = null,
                    latency = null,
                    mbpsSendRate = null,
                    relaxed = null,
                ),
            )
            val weight = maxOf(adaptiveWeight.getCurrentBitrate() / (weigthTargetBitrate / 25), 1)
            Log.d(tag, "rist: peer ${stats.peerId}: weight $weight")
            peer.peer.setWeight(weight.toUInt())
        }
    }

    private fun handleNetworkPathUpdate(interfaces: List<NetworkInterfaceInfo>) {
        if (!bonding) {
            return
        }
        val removedInterfaceNames = mutableListOf<String>()
        for (peer in peers) {
            if (peer.relayEndpoint != null) {
                continue
            }
            if (interfaces.any { it.name == peer.interfaceName }) {
                continue
            }
            removedInterfaceNames.add(peer.interfaceName)
        }
        for (interfaceName in removedInterfaceNames) {
            Log.i(tag, "rist: Removing peer for interface $interfaceName")
            peers.removeAll { it.interfaceName == interfaceName }
        }
        for (networkInterface in interfaces) {
            if (peers.any { it.interfaceName == networkInterface.name }) {
                continue
            }
            addPeer(
                url = makeRistBondingUrl(url, networkInterface.name),
                interfaceName = networkInterface.name,
                interfaceType = networkInterface.type,
            )
        }
    }

    private fun handleStatsInternal(stats: RistStats) {
        Log.d(
            tag,
            "rist: peer ${stats.sender.peerId}, rtt ${stats.sender.rtt}, " +
                "sent ${stats.sender.sentPackets}, received ${stats.sender.receivedPackets}, " +
                "retransmitted ${stats.sender.retransmittedPackets}, quality ${stats.sender.quality}, " +
                "bandwidth ${formatBytesPerSecond(stats.sender.bandwidth.toLong())}, " +
                "retry bandwidth ${formatBytesPerSecond(stats.sender.retryBandwidth.toLong())}",
        )
        getPeerById(stats.sender.peerId)?.stats = stats.sender
    }

    private fun addPeer(
        url: String?,
        interfaceName: String,
        interfaceType: Int?,
        relayEndpoint: RistEndpoint? = null,
    ) {
        val peer = url?.let { context?.addPeer(it) }
        if (peer == null) {
            Log.i(tag, "rist: Failed to add peer")
            return
        }
        peers.add(
            RistRemotePeer(
                interfaceName = interfaceName,
                interfaceType = interfaceType,
                relayEndpoint = relayEndpoint,
                peer = peer,
                stream = this,
            ),
        )
    }

    private fun getPeerById(peerId: UInt): RistRemotePeer? {
        return peers.firstOrNull { it.peer.getId() == peerId }
    }

    private fun send(data: ByteArray) {
        totalByteCount.mutate { it.value + data.size.toLong() }
        context?.send(data)
    }

    private fun send(dataPointer: ByteArray, count: Int) {
        totalByteCount.mutate { it.value + count.toLong() }
        context?.send(dataPointer, count)
    }

    private fun startNetworkPathMonitor() {
        val connectivityManager = ristConnectivityManager ?: return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                handleNetworkPathUpdateOnQueue()
            }

            override fun onLost(network: Network) {
                handleNetworkPathUpdateOnQueue()
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                handleNetworkPathUpdateOnQueue()
            }
        }
        networkPathMonitor = callback
        runCatching { connectivityManager.registerDefaultNetworkCallback(callback) }
    }

    private fun stopNetworkPathMonitor() {
        val callback = networkPathMonitor
        networkPathMonitor = null
        if (callback != null) {
            runCatching { ristConnectivityManager?.unregisterNetworkCallback(callback) }
        }
    }

    private fun handleNetworkPathUpdateOnQueue() {
        val interfaces = currentNetworkInterfaces()
        ristScope.launch {
            handleNetworkPathUpdate(interfaces)
        }
    }

    override fun writer(writer: MpegTsWriter, doOutput: ByteArray, containsAudio: Boolean) {
        send(doOutput)
    }

    override fun writer(writer: MpegTsWriter, doOutputPointer: ByteArray, count: Int) {
        send(doOutputPointer, count)
    }

    override fun ristSenderContextStats(context: RistSenderContext, stats: RistStats) {
        ristScope.launch {
            handleStatsInternal(stats)
        }
    }

    override fun ristSenderContextPeerConnected(context: RistSenderContext, peerId: UInt) {
        ristScope.launch {
            handlePeerConnectedInternal(peerId)
        }
    }

    override fun ristSenderContextPeerDisconnected(context: RistSenderContext, peerId: UInt) {
        ristScope.launch {
            handlePeerDisconnectedInternal(peerId)
        }
    }

    private fun handlePeerConnectedInternal(peerId: UInt) {
        Log.i(tag, "rist: Peer $peerId connected")
        getPeerById(peerId)?.setConnected()
        checkConnected()
    }

    private fun handlePeerDisconnectedInternal(peerId: UInt) {
        Log.i(tag, "rist: Peer $peerId disconnected")
        getPeerById(peerId)?.setDisconnected()
        checkDisconnected()
    }
}

fun makeRistBondingUrl(url: String, interfaceName: String? = null): String? {
    val uri = Uri.parse(url)
    if (uri.scheme == null) {
        return null
    }
    val builder = uri.buildUpon()
    if (interfaceName != null) {
        builder.appendQueryParameter("miface", interfaceName)
    }
    builder.appendQueryParameter("weight", "1")
    return builder.build().toString()
}

fun makeRistMoblinkBondingUrl(url: String, endpoint: RistEndpoint): String? {
    val uri = Uri.parse(url)
    if (uri.scheme == null) {
        return null
    }
    val builder = uri.buildUpon()
    builder.authority("${endpoint.host}:${endpoint.port}")
    return makeRistBondingUrl(builder.build().toString())
}

private fun currentNetworkInterfaces(): List<NetworkInterfaceInfo> {
    val connectivityManager = ristConnectivityManager ?: return emptyList()
    val interfaces = mutableListOf<NetworkInterfaceInfo>()
    for (network in connectivityManager.allNetworks) {
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: continue
        val name = connectivityManager.getLinkProperties(network)?.interfaceName ?: continue
        val type = when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ->
                NetworkCapabilities.TRANSPORT_CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ->
                NetworkCapabilities.TRANSPORT_WIFI
            else -> null
        }
        interfaces.add(NetworkInterfaceInfo(name, type))
    }
    return interfaces
}

interface RistSenderContextDelegate {
    fun ristSenderContextStats(context: RistSenderContext, stats: RistStats)
    fun ristSenderContextPeerConnected(context: RistSenderContext, peerId: UInt)
    fun ristSenderContextPeerDisconnected(context: RistSenderContext, peerId: UInt)
}

class RistPeer internal constructor(private val handle: Long) {
    fun getId(): UInt {
        return RistNative.senderPeerId(handle).toUInt()
    }

    fun setWeight(weight: UInt) {
        RistNative.setSenderPeerWeight(handle, weight.toInt())
    }
}

class RistSenderContext private constructor(private val handle: Long) {
    var delegate: RistSenderContextDelegate? = null

    fun start(): Boolean {
        return RistNative.startSenderContext(handle)
    }

    fun stop() {
        RistNative.stopSenderContext(handle)
    }

    fun addPeer(url: String): RistPeer? {
        val peerHandle = RistNative.addSenderPeer(handle, url)
        if (peerHandle == 0L) {
            return null
        }
        return RistPeer(peerHandle)
    }

    fun send(data: ByteArray): Int {
        return RistNative.senderSend(handle, data, data.size)
    }

    fun send(dataPointer: ByteArray, count: Int): Int {
        return RistNative.senderSend(handle, dataPointer, count)
    }

    companion object {
        operator fun invoke(): RistSenderContext? {
            val handle = RistNative.createSenderContext()
            if (handle == 0L) {
                return null
            }
            return RistSenderContext(handle)
        }
    }
}

class RistSenderStats(
    val peerId: UInt,
    val rtt: UInt,
    val sentPackets: UInt,
    val receivedPackets: UInt,
    val retransmittedPackets: UInt,
    val quality: UInt,
    val bandwidth: ULong,
    val retryBandwidth: ULong,
)

class RistStats(val sender: RistSenderStats)

private object RistNative {
    init {
        System.loadLibrary("rist")
    }

    external fun createSenderContext(): Long

    external fun startSenderContext(context: Long): Boolean

    external fun stopSenderContext(context: Long)

    external fun addSenderPeer(context: Long, url: String): Long

    external fun senderPeerId(peer: Long): Int

    external fun setSenderPeerWeight(peer: Long, weight: Int)

    external fun senderSend(context: Long, data: ByteArray, count: Int): Int
}

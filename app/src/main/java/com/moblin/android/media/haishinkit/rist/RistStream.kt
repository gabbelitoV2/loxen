package com.moblin.android.media.haishinkit.rist

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
import com.moblin.android.platform.network.NWEndpoint
import com.moblin.android.platform.network.NWInterface
import com.moblin.android.platform.network.NWPath
import com.moblin.android.platform.network.NWPathMonitor
import com.moblin.android.platform.rist.RistPeer
import com.moblin.android.platform.rist.RistSenderContext
import com.moblin.android.platform.rist.RistSenderContextDelegate
import com.moblin.android.platform.rist.RistSenderStats
import com.moblin.android.platform.rist.RistStats
import com.moblin.android.various.BondingConnection
import com.moblin.android.various.SimpleTimer
import java.util.UUID
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private const val TAG = "RistStream"

private val ristQueue: CoroutineDispatcher = Executors.newSingleThreadExecutor { runnable ->
    Thread(runnable, "com.eerimoq.Moblin.rist")
}.asCoroutineDispatcher()

private const val weigthTargetBitrate: Int = 10_000_000

private enum class RistPeerState {
    connecting,
    connected,
    disconnected,
}

private class RistRemotePeer(
    val interfaceName: String,
    val interfaceType: NWInterface.InterfaceType?,
    val relayEndpoint: NWEndpoint?,
    val peer: RistPeer,
    val stream: RistStream?,
) : AdaptiveBitrateDelegate {
    var stats: RistSenderStats? = null
    var adaptiveWeight: AdaptiveBitrateRistExperiment? = null
    private var state: RistPeerState = RistPeerState.connecting
    private var connectingTimer = SimpleTimer(queue = ristQueue)

    init {
        adaptiveWeight = null
        adaptiveWeight = AdaptiveBitrateRistExperiment(targetBitrate = weigthTargetBitrate, delegate = this)
        connectingTimer.startSingleShot(timeout = 5.0) {
            Log.i(TAG, "rist: Failed to connect to server")
            state = RistPeerState.disconnected
            stream?.checkDisconnected()
        }
    }

    fun close() {
        stopConnectingTimer()
        peer.close()
    }

    fun bondingConnectionName(): String {
        return when (interfaceType) {
            NWInterface.InterfaceType.cellular -> "Cellular"
            NWInterface.InterfaceType.wifi -> "WiFi"
            else -> interfaceName
        }
    }

    fun setConnected() {
        state = RistPeerState.connected
        stopConnectingTimer()
    }

    fun setDisconnected() {
        state = RistPeerState.disconnected
    }

    fun isConnected(): Boolean {
        return state == RistPeerState.connected
    }

    fun isDisconnected(): Boolean {
        return state == RistPeerState.disconnected
    }

    private fun stopConnectingTimer() {
        connectingTimer.stop()
    }

    override fun adaptiveBitrateSetVideoStreamBitrate(bitrate: Int) {}
}

private enum class RistStreamState {
    connecting,
    connected,
    disconnected,
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
    private val writer: MpegTsWriter = MpegTsWriter(timecodesEnabled = timecodesEnabled, newSrt = false)
    private var networkPathMonitor: NWPathMonitor? = null
    private var bonding: Boolean = false
    private var url: String = ""
    private var state: RistStreamState = RistStreamState.connecting
    private val ristDelegate: RistStreamDelegate? = delegate
    private var totalByteCount = Atomic(0L)

    init {
        writer.delegate = this
    }

    fun start(url: String, bonding: Boolean) {
        CoroutineScope(ristQueue).launch {
            startInternal(url = url, bonding = bonding)
        }
    }

    fun stop() {
        CoroutineScope(ristQueue).launch {
            stopInternal()
        }
    }

    fun addMoblink(endpoint: NWEndpoint, id: UUID, name: String) {
        CoroutineScope(ristQueue).launch {
            addMoblinkInternal(endpoint = endpoint, moblinkId = id, name = name)
        }
    }

    fun removeMoblink(endpoint: NWEndpoint) {
        CoroutineScope(ristQueue).launch {
            removeMoblinkInternal(endpoint = endpoint)
        }
    }

    fun getSpeed(): ULong {
        var totalBandwidth: ULong = 0u
        runBlocking(ristQueue) {
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
        runBlocking(ristQueue) {
            for (peer in peers) {
                val connection = BondingConnection(name = peer.bondingConnectionName(), usage = 0L, rtt = null)
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
        return runBlocking(ristQueue) {
            peers.filter { it.stats != null }.map { it.stats!! }
        }
    }

    fun updateConnectionsWeights() {
        CoroutineScope(ristQueue).launch {
            updateConnectionsWeightsInternal()
        }
    }

    fun checkConnected() {
        if (state != RistStreamState.connecting) {
            return
        }
        for (peer in peers) {
            if (!peer.isConnected()) {
                continue
            }
            state = RistStreamState.connected
            ristDelegate?.ristStreamOnConnected()
            break
        }
    }

    fun checkDisconnected() {
        for (peer in peers) {
            if (!peer.isDisconnected()) {
                return
            }
        }
        Log.i(TAG, "rist: All peers disconnected")
        state = RistStreamState.disconnected
        ristDelegate?.ristStreamOnDisconnected()
    }

    private fun startInternal(url: String, bonding: Boolean) {
        state = RistStreamState.connecting
        this.url = url
        this.bonding = bonding
        val context = RistSenderContext()
        if (context == null) {
            Log.i(TAG, "rist: Failed to create context")
            return
        }
        context.delegate = this
        this.context = context
        if (bonding) {
            networkPathMonitor = NWPathMonitor()
            networkPathMonitor?.pathUpdateHandler = ::handleNetworkPathUpdate
            networkPathMonitor?.start(queue = ristQueue)
        } else {
            addPeer(url = url, interfaceName = "", interfaceType = null)
        }
        if (!context.start()) {
            Log.i(TAG, "rist: Failed to start")
            return
        }
        processorPipelineQueue.launch {
            processor.startEncoding(writer)
            writer.startRunning()
        }
        val parsedUrl = Uri.parse(url)
        val host = parsedUrl.host ?: return
        val port = parsedUrl.port
        if (port == -1) {
            return
        }
        ristDelegate?.ristStreamRelayDestinationAddress(address = host, port = port.coerceIn(0, 0xFFFF))
    }

    private fun stopInternal() {
        state = RistStreamState.disconnected
        networkPathMonitor?.cancel()
        networkPathMonitor = null
        processorPipelineQueue.launch {
            writer.stopRunning()
            processor.stopEncoding(writer)
        }
        removePeers { true }
        context?.delegate = null
        context?.stop()
        context = null
    }

    private fun addMoblinkInternal(endpoint: NWEndpoint, moblinkId: UUID, name: String) {
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

    private fun removeMoblinkInternal(endpoint: NWEndpoint) {
        if (!bonding) {
            return
        }
        removePeers { it.relayEndpoint == endpoint }
    }

    private fun updateConnectionsWeightsInternal() {
        for (peer in peers) {
            val stats = peer.stats ?: continue
            val adaptiveWeight = peer.adaptiveWeight ?: continue
            adaptiveWeight.update(
                stats = StreamStats(
                    rttMs = stats.rtt.toDouble(),
                    packetsInFlight = 10.0,
                    transportBitrate = null,
                    latency = null,
                    mbpsSendRate = null,
                    relaxed = null,
                ),
            )
            val weight = maxOf(adaptiveWeight.getCurrentBitrate() / (weigthTargetBitrate / 25), 1)
            Log.d(TAG, "rist: peer ${stats.peerId}: weight $weight")
            peer.peer.setWeight(weight = weight.toUInt())
        }
    }

    private fun handleNetworkPathUpdate(path: NWPath) {
        if (!bonding) {
            return
        }
        val interfaces = path.uniqueAvailableInterfaces()
        val removedInterfaceNames = mutableListOf<String>()
        for (peer in peers) {
            if (peer.relayEndpoint != null) {
                continue
            }
            if (interfaces.map { it.name }.contains(peer.interfaceName)) {
                continue
            }
            removedInterfaceNames.add(peer.interfaceName)
        }
        for (interfaceName in removedInterfaceNames) {
            Log.i(TAG, "rist: Removing peer for interface $interfaceName")
            removePeers { it.interfaceName == interfaceName }
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
            TAG,
            "rist: peer ${stats.sender.peerId}, rtt ${stats.sender.rtt}, " +
                "sent ${stats.sender.sentPackets}, received ${stats.sender.receivedPackets}, " +
                "retransmitted ${stats.sender.retransmittedPackets}, quality ${stats.sender.quality}, " +
                "bandwidth ${formatBytesPerSecond(speed = stats.sender.bandwidth.toLong())}, " +
                "retry bandwidth ${formatBytesPerSecond(speed = stats.sender.retryBandwidth.toLong())}",
        )
        getPeerById(peerId = stats.sender.peerId)?.stats = stats.sender
    }

    private fun addPeer(
        url: String?,
        interfaceName: String,
        interfaceType: NWInterface.InterfaceType?,
        relayEndpoint: NWEndpoint? = null,
    ) {
        val peer = url?.let { context?.addPeer(url = it) }
        if (peer == null) {
            Log.i(TAG, "rist: Failed to add peer")
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

    private fun removePeers(predicate: (RistRemotePeer) -> Boolean) {
        val removedPeers = peers.filter(predicate)
        peers.removeAll(predicate)
        for (peer in removedPeers) {
            peer.close()
        }
    }

    private fun getPeerById(peerId: UInt): RistRemotePeer? {
        return peers.firstOrNull { it.peer.getId() == peerId }
    }

    private fun send(data: ByteArray) {
        totalByteCount.mutate { it.value += data.size.toLong() }
        context?.send(data = data)
    }

    private fun send(dataPointer: ByteArray, count: Int) {
        totalByteCount.mutate { it.value += count.toLong() }
        context?.send(dataPointer = dataPointer, count = count)
    }

    override fun writer(writer: MpegTsWriter, doOutput: ByteArray, containsAudio: Boolean) {
        send(data = doOutput)
    }

    override fun writer(writer: MpegTsWriter, doOutputPointer: ByteArray, count: Int) {
        send(dataPointer = doOutputPointer, count = count)
    }

    override fun ristSenderContextStats(context: RistSenderContext, stats: RistStats) {
        CoroutineScope(ristQueue).launch {
            handleStatsInternal(stats = stats)
        }
    }

    override fun ristSenderContextPeerConnected(context: RistSenderContext, peerId: UInt) {
        CoroutineScope(ristQueue).launch {
            handlePeerConnectedInternal(peerId = peerId)
        }
    }

    override fun ristSenderContextPeerDisconnected(context: RistSenderContext, peerId: UInt) {
        CoroutineScope(ristQueue).launch {
            handlePeerDisconnectedInternal(peerId = peerId)
        }
    }

    private fun handlePeerConnectedInternal(peerId: UInt) {
        Log.i(TAG, "rist: Peer $peerId connected")
        getPeerById(peerId = peerId)?.setConnected()
        checkConnected()
    }

    private fun handlePeerDisconnectedInternal(peerId: UInt) {
        Log.i(TAG, "rist: Peer $peerId disconnected")
        getPeerById(peerId = peerId)?.setDisconnected()
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

fun makeRistMoblinkBondingUrl(url: String, endpoint: NWEndpoint): String? {
    val uri = Uri.parse(url)
    if (uri.scheme == null) {
        return null
    }
    val userInfo = uri.encodedUserInfo?.let { "$it@" } ?: ""
    val moblinkUrl = uri.buildUpon().encodedAuthority("$userInfo${endpoint.host}:${endpoint.port}").build()
    return makeRistBondingUrl(moblinkUrl.toString())
}

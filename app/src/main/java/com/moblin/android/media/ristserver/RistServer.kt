package com.moblin.android.media.ristserver

import android.util.Log
import com.moblin.android.media.MediaSample
import com.moblin.android.media.haishinkit.util.Atomic
import com.moblin.android.media.haishinkit.util.BitrateStats
import com.moblin.android.media.haishinkit.util.BitrateStatsInstant
import com.moblin.android.platform.rist.RistReceiverContext
import com.moblin.android.platform.rist.RistReceiverContextDelegate
import com.moblin.android.various.settings.SettingsRistServerStream
import java.util.UUID
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch

interface RistServerDelegate {
    fun ristServerOnConnected(port: Int)

    fun ristServerOnDisconnected(port: Int, reason: String)

    fun ristServerOnVideoBuffer(cameraId: UUID, sampleBuffer: MediaSample)

    fun ristServerOnAudioBuffer(cameraId: UUID, sampleBuffer: MediaSample)
}

val ristServerQueue = CoroutineScope(
    Executors.newSingleThreadExecutor().asCoroutineDispatcher() + SupervisorJob(),
)

class RistServer(
    private var port: Int,
    private val streams: List<SettingsRistServerStream>,
    private val softwareDecoding: Boolean,
    val delegate: RistServerDelegate,
) : RistReceiverContextDelegate {
    private var context: RistReceiverContext? = null
    private val clientsByVirtualDestinationPort = mutableMapOf<Int, RistServerClient>()
    private val bitrateStats = Atomic(BitrateStats())
    private var numberOfClients = Atomic(0)

    fun start() {
        ristServerQueue.launch {
            startInternal()
        }
    }

    fun stop() {
        ristServerQueue.launch {
            stopInternal()
        }
    }

    fun updateStats(): BitrateStatsInstant = bitrateStats.mutate { it.value.update() }

    fun getNumberOfClients(): Int = numberOfClients.value

    private fun startInternal() {
        Log.i("RistServer", "rist-server: Starting")
        context = RistReceiverContext(inputUrl = "rist://@0.0.0.0:$port?rtt-min=100")
        context?.delegate = this
        context?.start()
    }

    private fun stopInternal() {
        Log.i("RistServer", "rist-server: Stopping")
        context?.stop()
        context = null
        for (virtualDestinationPort in clientsByVirtualDestinationPort.keys.toList()) {
            delegate.ristServerOnDisconnected(virtualDestinationPort, "")
        }
        clientsByVirtualDestinationPort.onEach { it.value.stop() }.clear()
        clientsChanged()
    }

    private fun peerConnected(virtualDestinationPort: Int) {
        Log.i(
            "RistServer",
            "rist-server: Connected virtual destination port $virtualDestinationPort",
        )
        val stream = streams.firstOrNull { it.virtualDestinationPort == virtualDestinationPort }
        if (stream == null) {
            Log.i(
                "RistServer",
                "rist-server: Ignoring unknown virtual destination port $virtualDestinationPort",
            )
            return
        }
        val client = RistServerClient(
            stream.id,
            stream.latencySeconds(),
            softwareDecoding,
        )
        client.server = this
        clientsByVirtualDestinationPort.put(virtualDestinationPort, client)?.stop()
        clientsChanged()
        delegate.ristServerOnConnected(virtualDestinationPort)
    }

    private fun peerDisconnected(virtualDestinationPort: Int) {
        Log.i(
            "RistServer",
            "rist-server: Disconnected virtual destination port $virtualDestinationPort",
        )
        if (clientsByVirtualDestinationPort.remove(virtualDestinationPort)?.also { it.stop() } != null) {
            clientsChanged()
            delegate.ristServerOnDisconnected(virtualDestinationPort, "")
        }
    }

    private fun clientsChanged() {
        val count = clientsByVirtualDestinationPort.size
        numberOfClients.mutate { it.value = count }
    }

    private fun peerReceivedData(virtualDestinationPort: Int, packets: List<ByteArray>) {
        val client = clientsByVirtualDestinationPort[virtualDestinationPort] ?: return
        for (packet in packets) {
            bitrateStats.mutate { it.value.add(bytesTransferred = packet.size) }
            client.handlePacketFromClient(packet)
        }
    }

    override fun ristReceiverContextConnected(virtualDestinationPort: Int) {
        ristServerQueue.launch {
            peerConnected(virtualDestinationPort)
        }
    }

    override fun ristReceiverContextDisconnected(virtualDestinationPort: Int) {
        ristServerQueue.launch {
            peerDisconnected(virtualDestinationPort)
        }
    }

    override fun ristReceiverContextReceivedData(
        virtualDestinationPort: Int,
        packets: List<ByteArray>,
    ) {
        ristServerQueue.launch {
            peerReceivedData(virtualDestinationPort, packets)
        }
    }
}

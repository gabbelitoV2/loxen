package com.moblin.android.media.rtmpserver

import android.util.Log
import com.moblin.android.media.MediaSample
import com.moblin.android.media.haishinkit.util.Atomic
import com.moblin.android.media.haishinkit.util.BitrateStats
import com.moblin.android.media.haishinkit.util.BitrateStatsInstant
import com.moblin.android.various.SimpleTimer
import com.moblin.android.various.network.DefaultTcpPorts
import com.moblin.android.various.settings.SettingsRtmpServer
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

val rtmpServerDispatchQueue: CoroutineDispatcher = Dispatchers.IO

val rtmpServerApp = "/live"

interface RtmpServerDelegate {
    fun rtmpServerOnPublishStart(streamKey: String)

    fun rtmpServerOnPublishStop(streamKey: String, reason: String)

    fun rtmpServerOnVideoBuffer(cameraId: UUID, sampleBuffer: MediaSample)

    fun rtmpServerOnAudioBuffer(cameraId: UUID, sampleBuffer: MediaSample)
}

class RtmpServer(
    settings: SettingsRtmpServer,
    softwareDecoding: Boolean,
    delegate: RtmpServerDelegate,
) {
    private var listener: ServerSocket? = null
    private var clients: MutableList<RtmpServerClient> = mutableListOf()
    val delegate: RtmpServerDelegate
    var settings: SettingsRtmpServer
    private val softwareDecoding: Boolean
    private var periodicTimer: SimpleTimer = SimpleTimer(rtmpServerDispatchQueue)
    val bitrateStats: Atomic<BitrateStats> = Atomic(BitrateStats())
    private var numberOfClients: Atomic<Int> = Atomic(0)
    private var connectedStreamKeys: Atomic<List<String>> = Atomic(emptyList())
    private var listenerFailed: Boolean = false
    private val scope: CoroutineScope = CoroutineScope(rtmpServerDispatchQueue + SupervisorJob())

    init {
        this.settings = settings
        this.softwareDecoding = softwareDecoding
        this.delegate = delegate
    }

    fun start() {
        scope.launch {
            setupPeriodicTimer()
            setupListener()
        }
    }

    fun stop() {
        scope.launch {
            for (client in clients) {
                client.stop(reason = "Server stop")
            }
            clients.clear()
            clientsChanged()
            listener?.close()
            listener = null
            periodicTimer.stop()
        }
    }

    fun isStreamConnected(streamKey: String): Boolean {
        return connectedStreamKeys.value.contains(streamKey)
    }

    fun updateStats(): BitrateStatsInstant {
        return bitrateStats.value.update()
    }

    fun getNumberOfClients(): Int {
        return numberOfClients.value
    }

    private fun setupListener() {
        try {
            val port = if (settings.port in 1..65535) settings.port else DefaultTcpPorts.rtmpServer
            val serverSocket = ServerSocket()
            serverSocket.reuseAddress = true
            serverSocket.bind(InetSocketAddress(port))
            listener = serverSocket
            listenerFailed = false
            handleListenerStateChange()
            scope.launch {
                while (true) {
                    val connection = try {
                        serverSocket.accept()
                    } catch (e: Exception) {
                        listenerFailed = true
                        break
                    }
                    handleNewListenerConnection(connection)
                }
            }
        } catch (e: Exception) {
            Log.i(TAG, "rtmp-server: Failed to create listener with error $e")
            listenerFailed = true
        }
    }

    private fun setupPeriodicTimer() {
        periodicTimer.startPeriodic(interval = 3.0) {
            cleanupClients()
            if (listenerFailed) {
                setupListener()
            }
        }
    }

    private fun cleanupClients() {
        val clientsToRemove = mutableListOf<RtmpServerClient>()
        for (client in clients) {
            if (Duration.between(client.latestReceiveTime, Instant.now()) > Duration.ofSeconds(10)) {
                clientsToRemove.add(client)
            }
        }
        for (client in clientsToRemove) {
            handleClientDisconnected(client = client, reason = "Receive timeout")
        }
    }

    private fun handleListenerStateChange() {
        Log.i(TAG, "rtmp-server: State change to ready")
        val port = listener?.localPort
        if (port != null) {
            Log.i(TAG, "rtmp-server: Listening on port $port")
        }
    }

    private fun handleNewListenerConnection(connection: Socket) {
        Log.i(TAG, "rtmp-server: Client TCP connected")
        val client = RtmpServerClient(this, connection, softwareDecoding)
        client.start()
        clients.add(client)
        clientsChanged()
    }

    fun handleClientConnected(client: RtmpServerClient) {
        val newClients = mutableListOf<RtmpServerClient>()
        for (aClient in clients) {
            if (aClient !== client && aClient.streamKey == client.streamKey) {
                val reason = "Same stream key"
                delegate.rtmpServerOnPublishStop(streamKey = client.streamKey, reason = reason)
                aClient.stop(reason = reason)
            } else {
                newClients.add(aClient)
            }
        }
        clients = newClients
        clientsChanged()
        delegate.rtmpServerOnPublishStart(streamKey = client.streamKey)
        logNumberOfClients()
    }

    fun handleClientDisconnected(client: RtmpServerClient, reason: String) {
        client.stop(reason = reason)
        clients.removeAll { it === client }
        clientsChanged()
        logNumberOfClients()
        if (client.streamKey.isNotEmpty()) {
            delegate.rtmpServerOnPublishStop(streamKey = client.streamKey, reason = reason)
        }
    }

    private fun clientsChanged() {
        val count = clients.size
        numberOfClients.value = count
        val streamKeys = clients.map { it.streamKey }.filter { it.isNotEmpty() }
        connectedStreamKeys.value = streamKeys
    }

    private fun logNumberOfClients() {
        Log.i(TAG, "rtmp-server: Number of clients: ${clients.size}")
    }

    companion object {
        private const val TAG = "RtmpServer"
    }
}

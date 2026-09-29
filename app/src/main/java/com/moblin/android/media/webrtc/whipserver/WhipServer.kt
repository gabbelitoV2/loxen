package com.moblin.android.media.webrtc.whipserver

import android.util.Log
import com.moblin.android.media.MediaSample
import com.moblin.android.media.haishinkit.util.Atomic
import com.moblin.android.media.haishinkit.util.BitrateStats
import com.moblin.android.media.haishinkit.util.BitrateStatsInstant
import com.moblin.android.media.webrtc.defaultStunServer
import com.moblin.android.various.network.HttpServer
import com.moblin.android.various.network.HttpServerRequest
import com.moblin.android.various.network.HttpServerResponse
import com.moblin.android.various.network.HttpServerRoute
import com.moblin.android.various.network.HttpServerStatus
import com.moblin.android.various.settings.SettingsHttpHeader
import com.moblin.android.various.settings.SettingsStreamColorRange
import com.moblin.android.various.settings.SettingsWhipServer
import com.moblin.android.various.settings.SettingsWhipServerStream
import java.util.UUID
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch

private const val TAG = "WhipServer"

val whipServerDispatchQueue: CoroutineDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

private val whipServerMainScope = CoroutineScope(Dispatchers.Main)

interface WhipServerDelegate {
    fun whipServerOnPublishStart(streamId: UUID)

    fun whipServerOnPublishStop(streamId: UUID, reason: String)

    fun whipServerOnVideoBuffer(streamId: UUID, sampleBuffer: MediaSample)

    fun whipServerOnAudioBuffer(streamId: UUID, sampleBuffer: MediaSample)
}

class WhipServer(
    var settings: SettingsWhipServer,
    private val softwareDecoding: Boolean,
    private val colorRange: SettingsStreamColorRange,
    private val delegate: WhipServerDelegate,
) : WhipServerClientDelegate {
    private var server: HttpServer? = null
    private val clients: MutableMap<UUID, WhipServerClient> = mutableMapOf()
    private val bitrateStats: Atomic<BitrateStats> = Atomic(BitrateStats())
    private var numberOfClients: Atomic<Int> = Atomic(0)
    private var connectedStreamIds: Atomic<List<UUID>> = Atomic(emptyList())
    private val scope = CoroutineScope(whipServerDispatchQueue)

    fun start() {
        scope.launch {
            startInternal()
        }
    }

    fun stop() {
        scope.launch {
            stopInternal()
        }
    }

    fun getNumberOfClients(): Int {
        return numberOfClients.value
    }

    fun updateStats(): BitrateStatsInstant {
        return bitrateStats.mutate { it.value.update() }
    }

    fun isStreamConnected(streamId: UUID): Boolean {
        return connectedStreamIds.value.contains(streamId)
    }

    fun startClient(
        streamKey: String,
        sdpOffer: String,
        onCompleted: (String?) -> Unit,
    ) {
        scope.launch {
            startClientInternal(streamKey = streamKey, sdpOffer = sdpOffer, onCompleted = onCompleted)
        }
    }

    private fun startInternal() {
        val routes = listOf(
            HttpServerRoute(path = "/whip/stream/", prefixMatch = true, handler = ::handleWhipStream),
            HttpServerRoute(path = "/whip/session/", prefixMatch = true, handler = ::handleWhipSession),
        )
        server = HttpServer(queue = whipServerDispatchQueue, routes = routes)
        server?.start(port = settings.port.toInt())
        Log.i(TAG, "whip-server: Listening on port ${settings.port}")
    }

    private fun stopInternal() {
        for (client in clients.values) {
            client.stop()
        }
        clients.clear()
        clientsChanged()
        server?.stop()
        server = null
        Log.i(TAG, "whip-server: Stopped")
    }

    private fun startClientInternal(
        streamKey: String,
        sdpOffer: String,
        onCompleted: (String?) -> Unit,
    ) {
        val stream = settings.streams.firstOrNull { it.streamKey == streamKey }
        if (stream == null) {
            whipServerMainScope.launch {
                onCompleted(null)
            }
            return
        }
        setupClient(stream = stream, sdpOffer = sdpOffer) { sdpAnswer ->
            if (sdpAnswer == null) {
                whipServerMainScope.launch {
                    onCompleted(null)
                }
                clients.remove(stream.id)
                clientsChanged()
                return@setupClient
            }
            whipServerMainScope.launch {
                onCompleted(sdpAnswer)
            }
        }
    }

    private fun handleWhipStream(request: HttpServerRequest, response: HttpServerResponse) {
        if (request.method != "POST") {
            response.send(status = HttpServerStatus.methodNotAllowed)
            return
        }
        val streamKey = request.path.split("/").lastOrNull()
        val stream = streamKey?.let { key -> settings.streams.firstOrNull { it.streamKey == key } }
        if (streamKey == null || stream == null) {
            response.send(status = HttpServerStatus.badRequest)
            return
        }
        val sdpOffer = request.body.toString(Charsets.UTF_8)
        setupClient(stream = stream, sdpOffer = sdpOffer) { sdpAnswer ->
            if (sdpAnswer == null) {
                response.send(status = HttpServerStatus.notFound)
                clients.remove(stream.id)
                clientsChanged()
                return@setupClient
            }
            response.send(
                data = sdpAnswer.toByteArray(Charsets.UTF_8),
                status = HttpServerStatus.created,
                contentType = "application/sdp",
                headers = listOf(
                    SettingsHttpHeader(name = "Location", value = "/whip/session/${stream.id}"),
                ),
            )
        }
    }

    private fun setupClient(
        stream: SettingsWhipServerStream,
        sdpOffer: String,
        completion: (String?) -> Unit,
    ) {
        val client = WhipServerClient(
            streamId = stream.id,
            latency = stream.latencySeconds(),
            syncTimestamps = stream.syncTimestamps,
            softwareDecoding = softwareDecoding,
            colorRange = colorRange,
            iceServers = listOf(defaultStunServer),
            delegate = this,
        )
        val streamId = client.streamId
        clients[streamId] = client
        clientsChanged()
        client.handleOffer(sdpOffer = sdpOffer, completion = completion)
    }

    private fun clientsChanged() {
        val count = clients.size
        numberOfClients.mutate { it.value = count }
        val streamIds = clients.keys.toList()
        connectedStreamIds.mutate { it.value = streamIds }
    }

    private fun handleWhipSession(request: HttpServerRequest, response: HttpServerResponse) {
        if (request.method != "DELETE") {
            response.send(status = HttpServerStatus.methodNotAllowed)
            return
        }
        val lastComponent = request.path.split("/").lastOrNull()
        val streamId = lastComponent?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        if (streamId == null) {
            response.send(status = HttpServerStatus.badRequest)
            return
        }
        val client = clients.remove(streamId)
        if (client != null) {
            clientsChanged()
            client.stop()
            delegate.whipServerOnPublishStop(streamId = streamId, reason = "Client disconnect")
        }
        response.send(status = HttpServerStatus.ok)
    }

    override fun whipServerClientOnConnected(streamId: UUID) {
        delegate.whipServerOnPublishStart(streamId = streamId)
    }

    override fun whipServerClientOnDisconnected(streamId: UUID, reason: String) {
        clients.remove(streamId)
        clientsChanged()
        delegate.whipServerOnPublishStop(streamId = streamId, reason = reason)
    }

    override fun whipServerClientOnVideoBuffer(streamId: UUID, sampleBuffer: MediaSample) {
        delegate.whipServerOnVideoBuffer(streamId = streamId, sampleBuffer = sampleBuffer)
    }

    override fun whipServerClientOnAudioBuffer(streamId: UUID, sampleBuffer: MediaSample) {
        delegate.whipServerOnAudioBuffer(streamId = streamId, sampleBuffer = sampleBuffer)
    }

    override fun whipServerClientOnDataReceived(streamId: UUID, count: Int) {
        bitrateStats.mutate { it.value.add(bytesTransferred = count) }
    }
}

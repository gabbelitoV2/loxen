package com.moblin.android.various.model

import com.moblin.android.localized
import com.moblin.android.media.MediaSample
import com.moblin.android.media.rtspclient.RtspClient
import com.moblin.android.media.rtspclient.RtspClientDelegate
import com.moblin.android.various.settings.SettingsRtspClientStream
import java.net.URI
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun Model.updateRtspVideoSources() {
    videoSources.rtsp.value = database.rtspClient.streams.map { stream ->
        Camera(id = stream.id.toString(), name = stream.camera())
    }
}

fun Model.getRtspStream(id: UUID): SettingsRtspClientStream? {
    return database.rtspClient.streams.firstOrNull { stream ->
        stream.id == id
    }
}

fun Model.getRtspStream(idString: String): SettingsRtspClientStream? {
    return database.rtspClient.streams.firstOrNull { stream ->
        idString == stream.id.toString()
    }
}

fun Model.reloadRtspClient() {
    stopRtspClient()
    val delegate = ModelRtspClientDelegate(this)
    for (stream in database.rtspClient.streams) {
        if (!stream.enabled) {
            continue
        }
        val url = runCatching { URI(stream.url) }.getOrNull() ?: continue
        val client = RtspClient(
            cameraId = stream.id,
            url = url,
            latency = stream.latencySeconds(),
            transport = stream.transport,
            softwareDecoding = database.ingestsSoftwareVideoDecoding,
            colorRange = this.stream.value.colorRange,
            delegate = delegate
        )
        client.start()
        ingests.rtsp.add(client)
    }
}

fun Model.stopRtspClient() {
    for (client in ingests.rtsp) {
        client.stop()
    }
    ingests.rtsp = mutableListOf()
}

fun Model.rtspClientConnectedInternal(cameraId: UUID) {
    val stream = getRtspStream(cameraId) ?: return
    val camera = stream.camera()
    makeToast(title = localized("$camera connected"))
    media.addBufferedVideo(cameraId = cameraId, name = camera, latency = stream.latencySeconds())
}

fun Model.rtspClientDisconnectedInternal(cameraId: UUID) {
    val stream = getRtspStream(cameraId) ?: return
    makeToast(title = localized("${stream.camera()} disconnected"))
    media.removeBufferedVideo(cameraId = cameraId)
}

class ModelRtspClientDelegate(private val model: Model) : RtspClientDelegate {
    private val mainScope = CoroutineScope(Dispatchers.Main)

    override fun rtspClientErrorToast(title: String) {
        model.makeErrorToastMain(title)
    }

    override fun rtspClientConnected(cameraId: UUID) {
        mainScope.launch {
            model.rtspClientConnectedInternal(cameraId)
        }
    }

    override fun rtspClientDisconnected(cameraId: UUID) {
        mainScope.launch {
            model.rtspClientDisconnectedInternal(cameraId)
        }
    }

    override fun rtspClientOnVideoBuffer(cameraId: UUID, sampleBuffer: MediaSample) {
        model.media.appendBufferedVideoSampleBuffer(cameraId = cameraId, sampleBuffer = sampleBuffer)
    }
}

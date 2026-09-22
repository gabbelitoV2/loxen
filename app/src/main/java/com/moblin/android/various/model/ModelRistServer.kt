package com.moblin.android.various.model

import com.moblin.android.localized
import com.moblin.android.media.MediaSample
import com.moblin.android.media.ristserver.RistServer
import com.moblin.android.media.ristserver.RistServerDelegate
import com.moblin.android.various.settings.SettingsRistServerStream
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val mainScope = CoroutineScope(Dispatchers.Main)

private var ristServerDelegate: ModelRistServerDelegate? = null

fun Model.stopRistServer() {
    ingests.rist?.stop()
    ingests.rist = null
}

fun Model.reloadRistServer() {
    stopRistServer()
    if (database.ristServer.enabled) {
        val delegate = ModelRistServerDelegate(this)
        ristServerDelegate = delegate
        ingests.rist = RistServer(
            port = database.ristServer.port,
            streams = database.ristServer.streams.map { it.clone() },
            softwareDecoding = database.ingestsSoftwareVideoDecoding,
            delegate = delegate
        )
        ingests.rist?.start()
    }
}

fun Model.ristServerEnabled(): Boolean {
    return database.ristServer.enabled
}

fun Model.ristCameras(): List<Camera> {
    return database.ristServer.streams.map { Camera(id = it.id.toString(), name = it.camera()) }
}

fun Model.getRistStream(id: UUID): SettingsRistServerStream? {
    return database.ristServer.streams.firstOrNull { it.id == id }
}

fun Model.getRistStream(idString: String): SettingsRistServerStream? {
    return database.ristServer.streams.firstOrNull { it.id.toString() == idString }
}

fun Model.getRistStream(virtualDestinationPort: Int): SettingsRistServerStream? {
    return database.ristServer.streams.firstOrNull { it.virtualDestinationPort == virtualDestinationPort }
}

fun Model.isRistStreamConnected(port: Int): Boolean {
    return database.ristServer.streams.firstOrNull { it.virtualDestinationPort == port }?.connected == true
}

class ModelRistServerDelegate(private val model: Model) : RistServerDelegate {
    override fun ristServerOnConnected(port: Int) {
        model.ristServerOnConnected(port)
    }

    override fun ristServerOnDisconnected(port: Int, reason: String) {
        model.ristServerOnDisconnected(port, reason)
    }

    override fun ristServerOnAudioBuffer(cameraId: UUID, sampleBuffer: MediaSample) {
        model.ristServerOnAudioBuffer(cameraId, sampleBuffer)
    }

    override fun ristServerOnVideoBuffer(cameraId: UUID, sampleBuffer: MediaSample) {
        model.ristServerOnVideoBuffer(cameraId, sampleBuffer)
    }
}

fun Model.ristServerOnConnected(port: Int) {
    mainScope.launch {
        this@ristServerOnConnected.ristServerOnConnectedInternal(virtualDestinationPort = port)
    }
}

fun Model.ristServerOnDisconnected(port: Int, reason: String) {
    mainScope.launch {
        this@ristServerOnDisconnected.ristServerOnDisconnectedInternal(virtualDestinationPort = port, reason = reason)
    }
}

fun Model.ristServerOnAudioBuffer(cameraId: UUID, sampleBuffer: MediaSample) {
    media.appendBufferedAudioSampleBuffer(cameraId = cameraId, sampleBuffer = sampleBuffer)
}

fun Model.ristServerOnVideoBuffer(cameraId: UUID, sampleBuffer: MediaSample) {
    media.appendBufferedVideoSampleBuffer(cameraId = cameraId, sampleBuffer = sampleBuffer)
}

private fun Model.ristServerOnConnectedInternal(virtualDestinationPort: Int) {
    val stream = getRistStream(virtualDestinationPort = virtualDestinationPort) ?: return
    val camera = stream.camera()
    makeToast(title = localized("$camera connected"))
    val latency = stream.latencySeconds()
    media.addBufferedVideo(cameraId = stream.id, name = camera, latency = latency)
    media.addBufferedAudio(cameraId = stream.id, name = camera, latency = latency)
}

private fun Model.ristServerOnDisconnectedInternal(virtualDestinationPort: Int, reason: String) {
    val stream = getRistStream(virtualDestinationPort = virtualDestinationPort) ?: return
    makeToast(title = localized("${stream.camera()} disconnected"))
    media.removeBufferedVideo(cameraId = stream.id)
    media.removeBufferedAudio(cameraId = stream.id)
}

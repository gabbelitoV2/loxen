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
            colorRange = stream.value.colorRange,
            delegate = delegate
        )
        ingests.rist?.start()
    }
}

fun Model.ristServerEnabled(): Boolean {
    return database.ristServer.enabled
}

fun Model.updateRistVideoSources() {
    videoSources.rist.value = database.ristServer.streams
        .map { Camera(id = it.id.toString(), name = it.camera()) }
}

fun Model.updateRistVideoSourcesAndMics() {
    updateRistVideoSources()
    updateRistMics()
}

fun Model.getRistStream(id: UUID): SettingsRistServerStream? {
    return database.ristServer.streams.firstOrNull { it.id == id }
}

fun Model.getRistStream(idString: String): SettingsRistServerStream? {
    return database.ristServer.streams.firstOrNull { it.id.toString() == idString }
}

fun Model.isRistStreamConnected(port: Int): Boolean {
    return database.ristServer.streams.firstOrNull { it.virtualDestinationPort == port }?.connected == true
}

class ModelRistServerDelegate(private val model: Model) : RistServerDelegate {
    override fun ristServerOnConnected(cameraId: UUID, name: String, latency: Double) {
        model.ristServerOnConnected(cameraId, name, latency)
    }

    override fun ristServerOnDisconnected(cameraId: UUID, name: String) {
        model.ristServerOnDisconnected(cameraId, name)
    }

    override fun ristServerOnAudioBuffer(cameraId: UUID, sampleBuffer: MediaSample) {
        model.ristServerOnAudioBuffer(cameraId, sampleBuffer)
    }

    override fun ristServerOnVideoBuffer(cameraId: UUID, sampleBuffer: MediaSample) {
        model.ristServerOnVideoBuffer(cameraId, sampleBuffer)
    }
}

fun Model.ristServerOnConnected(cameraId: UUID, name: String, latency: Double) {
    mainScope.launch {
        this@ristServerOnConnected.makeToast(title = localized("$name connected"))
        this@ristServerOnConnected.media.addBufferedVideo(cameraId = cameraId, name = name, latency = latency)
        this@ristServerOnConnected.media.addBufferedAudio(cameraId = cameraId, name = name, latency = latency)
    }
}

fun Model.ristServerOnDisconnected(cameraId: UUID, name: String) {
    mainScope.launch {
        this@ristServerOnDisconnected.makeToast(title = localized("$name disconnected"))
        this@ristServerOnDisconnected.media.removeBufferedVideo(cameraId = cameraId)
        this@ristServerOnDisconnected.media.removeBufferedAudio(cameraId = cameraId)
    }
}

fun Model.ristServerOnAudioBuffer(cameraId: UUID, sampleBuffer: MediaSample) {
    media.appendBufferedAudioSampleBuffer(cameraId = cameraId, sampleBuffer = sampleBuffer)
}

fun Model.ristServerOnVideoBuffer(cameraId: UUID, sampleBuffer: MediaSample) {
    media.appendBufferedVideoSampleBuffer(cameraId = cameraId, sampleBuffer = sampleBuffer)
}

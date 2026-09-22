package com.moblin.android.various.model

import com.moblin.android.localized
import com.moblin.android.media.MediaSample
import com.moblin.android.media.srtla.server.SrtlaServer
import com.moblin.android.media.srtla.server.SrtlaServerDelegate
import com.moblin.android.media.srtla.server.srtServerClientLatency
import com.moblin.android.various.settings.SettingsSrtlaServerStream
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun Model.stopSrtlaServer() {
    ingests.srtla?.stop()
    ingests.srtla = null
}

fun Model.reloadSrtlaServer() {
    stopSrtlaServer()
    if (database.srtlaServer.enabled) {
        ingests.srtla = SrtlaServer(
            settings = database.srtlaServer,
            delegate = ModelSrtlaServerDelegate(this),
            timecodesEnabled = isTimecodesEnabled(),
            softwareDecoding = database.ingestsSoftwareVideoDecoding
        )
        ingests.srtla?.start()
    }
}

fun Model.srtlaServerEnabled(): Boolean {
    return database.srtlaServer.enabled
}

fun Model.srtlaCameras(): List<Camera> {
    return database.srtlaServer.streams.map { Camera(id = it.id.toString(), name = it.camera()) }
}

fun Model.getSrtlaStream(id: UUID): SettingsSrtlaServerStream? {
    return database.srtlaServer.streams.firstOrNull { it.id == id }
}

fun Model.getSrtlaStream(
    streamId: String? = null,
    idString: String? = null,
): SettingsSrtlaServerStream? {
    if (streamId != null) {
        database.srtlaServer.streams.firstOrNull { it.streamId == streamId }?.let {
            return it
        }
    }
    if (idString != null) {
        database.srtlaServer.streams.firstOrNull { it.id.toString() == idString }?.let {
            return it
        }
    }
    return null
}

fun Model.isSrtlaStreamConnected(streamId: String): Boolean {
    return ingests.srtla?.isStreamConnected(streamId = streamId) ?: false
}

class ModelSrtlaServerDelegate(private val model: Model) : SrtlaServerDelegate {
    private val mainScope = CoroutineScope(Dispatchers.Main)

    override fun srtlaServerOnClientStart(cameraId: UUID, name: String) {
        mainScope.launch {
            srtlaServerOnClientStartInternal(cameraId = cameraId, name = name)
        }
    }

    override fun srtlaServerOnClientStop(cameraId: UUID, name: String) {
        mainScope.launch {
            srtlaServerOnClientStopInternal(cameraId = cameraId, name = name)
        }
    }

    private fun srtlaServerOnClientStartInternal(cameraId: UUID, name: String) {
        model.makeToast(title = localized("$name connected"))
        model.media.addBufferedVideo(cameraId = cameraId, name = name, latency = srtServerClientLatency)
        model.media.addBufferedAudio(cameraId = cameraId, name = name, latency = srtServerClientLatency)
    }

    private fun srtlaServerOnClientStopInternal(cameraId: UUID, name: String) {
        model.makeToast(title = localized("$name disconnected"))
        model.media.removeBufferedVideo(cameraId = cameraId)
        model.media.removeBufferedAudio(cameraId = cameraId)
    }

    override fun srtlaServerOnAudioBuffer(cameraId: UUID, sampleBuffer: MediaSample) {
        model.media.appendBufferedAudioSampleBuffer(cameraId = cameraId, sampleBuffer = sampleBuffer)
    }

    override fun srtlaServerOnVideoBuffer(cameraId: UUID, sampleBuffer: MediaSample) {
        model.media.appendBufferedVideoSampleBuffer(cameraId = cameraId, sampleBuffer = sampleBuffer)
    }
}

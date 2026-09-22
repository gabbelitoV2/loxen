package com.moblin.android.various.model

import com.moblin.android.localized
import com.moblin.android.media.MediaSample
import com.moblin.android.media.srtclient.SrtClient
import com.moblin.android.media.srtclient.srtClientLatency
import com.moblin.android.various.settings.SettingsSrtClientStream
import com.moblin.android.various.settings.camera
import java.net.URI
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val mainScope = CoroutineScope(Dispatchers.Main)

fun Model.srtClientCameras(): List<Camera> {
    return database.srtClient.streams.map { stream ->
        Camera(id = stream.id.toString(), name = stream.camera())
    }
}

fun Model.getSrtClientStream(id: UUID): SettingsSrtClientStream? {
    return database.srtClient.streams.firstOrNull { stream ->
        stream.id == id
    }
}

fun Model.getSrtClientStream(idString: String): SettingsSrtClientStream? {
    return database.srtClient.streams.firstOrNull { stream ->
        stream.id.toString() == idString
    }
}

fun Model.isSrtClientStreamConnected(id: UUID): Boolean {
    return true
}

fun Model.reloadSrtClient() {
    stopSrtClient()
    for (stream in database.srtClient.streams) {
        if (!stream.enabled) {
            continue
        }
        val url = runCatching { URI(stream.url) }.getOrNull() ?: continue
        val client = SrtClient(
            cameraId = stream.id,
            url = url,
            softwareDecoding = database.ingestsSoftwareVideoDecoding,
            delegate = this,
        )
        client.start()
        ingests.srt.add(client)
    }
}

fun Model.stopSrtClient() {
    for (client in ingests.srt) {
        client.stop()
    }
    ingests.srt.clear()
}

private fun Model.srtClientConnectedInternal(cameraId: UUID) {
    val stream = getSrtClientStream(id = cameraId) ?: return
    val camera = stream.camera()
    makeToast(title = localized("$camera connected"))
    media.addBufferedVideo(cameraId = cameraId, name = camera, latency = srtClientLatency)
    media.addBufferedAudio(cameraId = cameraId, name = camera, latency = srtClientLatency)
}

private fun Model.srtClientDisconnectedInternal(cameraId: UUID) {
    val stream = getSrtClientStream(id = cameraId) ?: return
    makeToast(title = localized("${stream.camera()} disconnected"))
    media.removeBufferedVideo(cameraId = cameraId)
    media.removeBufferedAudio(cameraId = cameraId)
}

fun Model.srtClientConnected(cameraId: UUID) {
    mainScope.launch {
        srtClientConnectedInternal(cameraId)
    }
}

fun Model.srtClientDisconnected(cameraId: UUID) {
    mainScope.launch {
        srtClientDisconnectedInternal(cameraId)
    }
}

fun Model.srtClientOnVideoBuffer(cameraId: UUID, sampleBuffer: MediaSample) {
    media.appendBufferedVideoSampleBuffer(cameraId = cameraId, sampleBuffer = sampleBuffer)
}

fun Model.srtClientOnAudioBuffer(cameraId: UUID, sampleBuffer: MediaSample) {
    media.appendBufferedAudioSampleBuffer(cameraId = cameraId, sampleBuffer = sampleBuffer)
}

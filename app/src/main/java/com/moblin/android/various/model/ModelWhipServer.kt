package com.moblin.android.various.model

import com.moblin.android.localized
import com.moblin.android.media.MediaSample
import com.moblin.android.media.webrtc.whipserver.WhipServer
import com.moblin.android.media.webrtc.whipserver.WhipServerDelegate
import com.moblin.android.various.settings.SettingsWhipServerStream
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val mainScope = CoroutineScope(Dispatchers.Main)

fun Model.whipServerEnabled(): Boolean {
    return database.whipServer.enabled
}

fun Model.whipCameras(): List<Camera> {
    return database.whipServer.streams.map { stream ->
        Camera(id = stream.id.toString(), name = stream.camera())
    }
}

fun Model.getWhipStream(id: UUID): SettingsWhipServerStream? {
    return database.whipServer.streams.firstOrNull { stream ->
        stream.id == id
    }
}

fun Model.getWhipStream(idString: CharSequence): SettingsWhipServerStream? {
    return database.whipServer.streams.firstOrNull { stream ->
        idString.toString() == stream.id.toString()
    }
}

fun Model.getWhipStream(streamKey: String): SettingsWhipServerStream? {
    return database.whipServer.streams.firstOrNull { stream ->
        stream.streamKey == streamKey
    }
}

fun Model.isWhipStreamConnected(streamId: UUID): Boolean {
    return ingests.whip?.isStreamConnected(streamId) ?: false
}

fun Model.stopAllWhipStreams() {
    for (stream in database.whipServer.streams) {
        stopWhipServerStream(stream, showToast = false)
    }
}

private fun Model.stopWhipServerStream(
    stream: SettingsWhipServerStream,
    showToast: Boolean,
    reason: String? = null
) {
    if (showToast) {
        makeToast(title = localized("${stream.camera()} disconnected"), subTitle = reason)
    }
    media.removeBufferedVideo(cameraId = stream.id)
    media.removeBufferedAudio(cameraId = stream.id)
}

private fun Model.handleWhipServerPublishStart(streamId: UUID) {
    val stream = getWhipStream(id = streamId) ?: return
    val camera = stream.camera()
    makeToast(title = localized("$camera connected"))
    val latency = stream.latencySeconds()
    media.addBufferedVideo(cameraId = stream.id, name = camera, latency = latency)
    media.addBufferedAudio(cameraId = stream.id, name = camera, latency = latency)
}

private fun Model.handleWhipServerPublishStop(streamId: UUID, reason: String) {
    val stream = getWhipStream(id = streamId) ?: return
    stopWhipServerStream(stream, showToast = true, reason = reason)
}

fun Model.stopWhipServer() {
    ingests.whip?.stop()
    ingests.whip = null
    stopAllWhipStreams()
}

fun Model.reloadWhipServer() {
    stopWhipServer()
    if (database.whipServer.enabled) {
        ingests.whip = WhipServer(
            settings = database.whipServer.clone(),
            softwareDecoding = database.ingestsSoftwareVideoDecoding,
            delegate = ModelWhipServerDelegate(this)
        )
        ingests.whip?.start()
    }
}

class ModelWhipServerDelegate(private val model: Model) : WhipServerDelegate {
    override fun whipServerOnPublishStart(streamId: UUID) {
        mainScope.launch {
            model.handleWhipServerPublishStart(streamId)
        }
    }

    override fun whipServerOnPublishStop(streamId: UUID, reason: String) {
        mainScope.launch {
            model.handleWhipServerPublishStop(streamId, reason)
        }
    }

    override fun whipServerOnVideoBuffer(streamId: UUID, sampleBuffer: MediaSample) {
        model.media.appendBufferedVideoSampleBuffer(cameraId = streamId, sampleBuffer = sampleBuffer)
    }

    override fun whipServerOnAudioBuffer(streamId: UUID, sampleBuffer: MediaSample) {
        model.media.appendBufferedAudioSampleBuffer(cameraId = streamId, sampleBuffer = sampleBuffer)
    }
}

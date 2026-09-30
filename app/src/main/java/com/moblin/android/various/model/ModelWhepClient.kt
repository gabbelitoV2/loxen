package com.moblin.android.various.model

import com.moblin.android.localized
import com.moblin.android.media.MediaSample
import com.moblin.android.media.webrtc.whepclient.WhepClient
import com.moblin.android.media.webrtc.whepclient.WhepClientDelegate
import com.moblin.android.various.settings.SettingsWhepClientStream
import java.net.URI
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val whepMainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

fun Model.updateWhepVideoSources() {
    videoSources.whep.value = database.whepClient.streams.map { stream ->
        Camera(id = stream.id.toString(), name = stream.camera())
    }
}

fun Model.updateWhepVideoSourcesAndMics() {
    updateWhepVideoSources()
    updateWhepMics()
}

fun Model.getWhepStream(id: UUID): SettingsWhepClientStream? {
    return database.whepClient.streams.firstOrNull { stream ->
        stream.id == id
    }
}

fun Model.getWhepStream(idString: String): SettingsWhepClientStream? {
    return database.whepClient.streams.firstOrNull { stream ->
        idString == stream.id.toString()
    }
}

fun Model.reloadWhepClient() {
    stopWhepClient()
    for (stream in database.whepClient.streams) {
        if (!stream.enabled) {
            continue
        }
        val client = WhepClient(
            streamId = stream.id,
            url = stream.url,
            latency = stream.latencySeconds(),
            syncTimestamps = stream.syncTimestamps,
            softwareDecoding = database.ingestsSoftwareVideoDecoding,
            colorRange = this.stream.value.colorRange,
            delegate = ModelWhepClientDelegate(this)
        )
        client.start()
        ingests.whep.add(client)
    }
}

fun Model.stopWhepClient() {
    for (client in ingests.whep) {
        client.stop()
    }
    ingests.whep.clear()
    for (stream in database.whepClient.streams) {
        media.removeBufferedVideo(cameraId = stream.id)
        media.removeBufferedAudio(cameraId = stream.id)
    }
}

fun Model.whepClientOnPublishStart(streamId: UUID) {
    whepMainScope.launch {
        val stream = this@whepClientOnPublishStart.getWhepStream(streamId) ?: return@launch
        val camera = stream.camera()
        makeToast(title = localized("$camera connected"))
        val latency = stream.latencySeconds()
        media.addBufferedVideo(cameraId = stream.id, name = camera, latency = latency)
        media.addBufferedAudio(cameraId = stream.id, name = camera, latency = latency)
    }
}

fun Model.whepClientOnPublishStop(streamId: UUID, reason: String) {
    whepMainScope.launch {
        val stream = this@whepClientOnPublishStop.getWhepStream(streamId) ?: return@launch
        makeToast(
            title = localized("${stream.camera()} disconnected"),
            subTitle = reason
        )
        media.removeBufferedVideo(cameraId = stream.id)
        media.removeBufferedAudio(cameraId = stream.id)
    }
}

fun Model.whepClientOnVideoBuffer(streamId: UUID, sampleBuffer: MediaSample) {
    media.appendBufferedVideoSampleBuffer(cameraId = streamId, sampleBuffer = sampleBuffer)
}

fun Model.whepClientOnAudioBuffer(streamId: UUID, sampleBuffer: MediaSample) {
    media.appendBufferedAudioSampleBuffer(cameraId = streamId, sampleBuffer = sampleBuffer)
}

class ModelWhepClientDelegate(private val model: Model) : WhepClientDelegate {
    override fun whepClientOnPublishStart(streamId: UUID) {
        model.whepClientOnPublishStart(streamId)
    }

    override fun whepClientOnPublishStop(streamId: UUID, reason: String) {
        model.whepClientOnPublishStop(streamId, reason)
    }

    override fun whepClientOnVideoBuffer(streamId: UUID, sampleBuffer: MediaSample) {
        model.whepClientOnVideoBuffer(streamId, sampleBuffer)
    }

    override fun whepClientOnAudioBuffer(streamId: UUID, sampleBuffer: MediaSample) {
        model.whepClientOnAudioBuffer(streamId, sampleBuffer)
    }
}

package com.moblin.android.various.model

import com.moblin.android.localized
import com.moblin.android.media.MediaSample
import com.moblin.android.media.rtmpserver.RtmpServer
import com.moblin.android.media.rtmpserver.RtmpServerDelegate
import com.moblin.android.various.settings.SettingsDjiDeviceUrlType
import com.moblin.android.various.settings.SettingsGoProUrlType
import com.moblin.android.various.settings.SettingsRtmpServerStream
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val mainScope = CoroutineScope(Dispatchers.Main)

fun Model.rtmpCameras(): List<Camera> {
    return database.rtmpServer.streams.map { Camera(id = it.id.toString(), name = it.camera()) }
}

fun Model.getRtmpStream(id: UUID): SettingsRtmpServerStream? {
    return database.rtmpServer.streams.firstOrNull { it.id == id }
}

@JvmName("getRtmpStreamByIdString")
fun Model.getRtmpStream(idString: String): SettingsRtmpServerStream? {
    return database.rtmpServer.streams.firstOrNull { it.id.toString() == idString }
}

@JvmName("getRtmpStreamByStreamKey")
fun Model.getRtmpStream(streamKey: String): SettingsRtmpServerStream? {
    return database.rtmpServer.streams.firstOrNull { it.streamKey == streamKey }
}

fun Model.stopAllRtmpStreams() {
    for (stream in database.rtmpServer.streams) {
        stopRtmpServerStream(stream = stream, showToast = false)
    }
}

fun Model.isRtmpStreamConnected(streamKey: String): Boolean {
    return ingests.rtmp?.isStreamConnected(streamKey = streamKey) ?: false
}

private fun Model.handleRtmpServerPublishStart(streamKey: String) {
    val stream = getRtmpStream(streamKey = streamKey) ?: return
    val camera = stream.camera()
    makeToast(title = localized("$camera connected"))
    val latency = stream.latencySeconds()
    media.addBufferedVideo(cameraId = stream.id, name = camera, latency = latency)
    media.addBufferedAudio(cameraId = stream.id, name = camera, latency = latency)
    markDjiIsStreamingIfNeeded(rtmpServerStreamId = stream.id)
    markGoProIsStreamingIfNeeded(rtmpServerStreamId = stream.id)
}

private fun Model.handleRtmpServerPublishStop(streamKey: String, reason: String) {
    val stream = getRtmpStream(streamKey = streamKey) ?: return
    stopRtmpServerStream(stream = stream, showToast = true, reason = reason)
    switchMicIfNeededAfterNetworkCameraChange()
}

private fun Model.stopRtmpServerStream(
    stream: SettingsRtmpServerStream,
    showToast: Boolean,
    reason: String? = null,
) {
    if (showToast) {
        makeToast(title = localized("${stream.camera()} disconnected"), subTitle = reason)
    }
    media.removeBufferedVideo(cameraId = stream.id)
    media.removeBufferedAudio(cameraId = stream.id)
    for (device in database.djiDevices.devices) {
        if (device.rtmpUrlType != SettingsDjiDeviceUrlType.server ||
            device.serverRtmpStreamId != stream.id
        ) {
            continue
        }
        restartDjiLiveStreamIfNeededAfterDelay(device = device)
    }
    for (device in database.goPro.devices) {
        if (device.rtmpUrlType != SettingsGoProUrlType.server ||
            device.serverRtmpStreamId != stream.id
        ) {
            continue
        }
        restartGoProLiveStreamIfNeededAfterDelay(device = device)
    }
}

fun Model.stopRtmpServer() {
    ingests.rtmp?.stop()
    ingests.rtmp = null
    stopAllRtmpStreams()
}

fun Model.reloadRtmpServer() {
    stopRtmpServer()
    if (database.rtmpServer.enabled) {
        ingests.rtmp = RtmpServer(
            settings = database.rtmpServer.clone(),
            softwareDecoding = database.ingestsSoftwareVideoDecoding,
            delegate = this,
        )
        ingests.rtmp?.start()
    }
}

fun Model.rtmpServerEnabled(): Boolean {
    return database.rtmpServer.enabled
}

fun Model.rtmpServerOnPublishStart(streamKey: String) {
    mainScope.launch {
        handleRtmpServerPublishStart(streamKey = streamKey)
    }
}

fun Model.rtmpServerOnPublishStop(streamKey: String, reason: String) {
    mainScope.launch {
        handleRtmpServerPublishStop(streamKey = streamKey, reason = reason)
    }
}

fun Model.rtmpServerOnVideoBuffer(cameraId: UUID, sampleBuffer: MediaSample) {
    media.appendBufferedVideoSampleBuffer(cameraId = cameraId, sampleBuffer = sampleBuffer)
}

fun Model.rtmpServerOnAudioBuffer(cameraId: UUID, sampleBuffer: MediaSample) {
    media.appendBufferedAudioSampleBuffer(cameraId = cameraId, sampleBuffer = sampleBuffer)
}

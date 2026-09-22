package com.moblin.android.various.model

import com.moblin.android.localized
import com.moblin.android.media.MediaSample
import com.moblin.android.various.settings.SettingsScene
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val mainScope = CoroutineScope(Dispatchers.Main)

enum class SampleBufferType {
    video,
    audioApp,
    audioMic,
}

interface SampleBufferReceiverDelegate {
    fun senderConnected()

    fun senderDisconnected()

    fun handleSampleBuffer(type: SampleBufferType, sampleBuffer: MediaSample)
}

fun Model.isScreenCaptureCamera(cameraId: CameraId): Boolean {
    return cameraId == screenCaptureCameraId.toString()
}

fun Model.isNoneCamera(cameraId: CameraId): Boolean {
    return cameraId == noneCameraId.toString()
}

fun Model.sceneNeedsMacScreenCapture(scene: SettingsScene): Boolean {
    return false
}

private fun Model.handleScreenCaptureStarted(latency: Double) {
    makeToast(title = localized("Screen capture started"))
    media.addBufferedVideo(
        cameraId = screenCaptureCameraId,
        name = screenCaptureCameraName,
        latency = latency,
    )
}

private fun Model.handleScreenCaptureStopped() {
    makeToast(title = localized("Screen capture stopped"))
    media.removeBufferedVideo(cameraId = screenCaptureCameraId)
}

fun Model.senderConnected() {
    mainScope.launch {
        handleScreenCaptureStarted(screenRecordingLatency)
    }
}

fun Model.senderDisconnected() {
    mainScope.launch {
        handleScreenCaptureStopped()
    }
}

fun Model.handleSampleBuffer(type: SampleBufferType, sampleBuffer: MediaSample) {
    when (type) {
        SampleBufferType.video -> media.appendBufferedVideoSampleBuffer(
            cameraId = screenCaptureCameraId,
            sampleBuffer = sampleBuffer,
        )
        else -> Unit
    }
}

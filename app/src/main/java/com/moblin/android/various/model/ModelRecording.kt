package com.moblin.android.various.model

import com.moblin.android.common.various.formatBytes
import com.moblin.android.common.various.noValue
import com.moblin.android.localized
import com.moblin.android.remotecontrol.RemoteControlAssistantStreamerState
import com.moblin.android.various.settings.MacroEvent
import com.moblin.android.various.settings.SettingsMacrosEvent
import com.moblin.android.various.settings.SettingsQuickButtonType
import java.io.File
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow

class RecordingProvider {
    private val _length = MutableStateFlow(noValue)

    var length: String
        get() = _length.value
        set(value) {
            _length.value = value
        }
}

fun Model.startRecording() {
    if (isChatPhone()) {
        return
    }
    setIsRecording(value = true)
    if (!resumeRecording()) {
        if (stream.value.recording.isDefaultRecordingPath()) {
            makeErrorToast(title = localized("Failed to start recording"))
        } else {
            makeErrorToast(
                title = localized("Failed to start recording"),
                subTitle = localized("Is the disk connected?")
            )
        }
        setIsRecording(value = false)
        return
    }
    macrosEventOccurred(MacroEvent(event = SettingsMacrosEvent.START_RECORDING))
}

fun Model.stopRecording(toastTitle: String? = null, toastSubTitle: String? = null) {
    if (!isRecording.value) {
        return
    }
    macrosEventOccurred(MacroEvent(event = SettingsMacrosEvent.STOP_RECORDING))
    setIsRecording(value = false)
    toastTitle?.let { title ->
        makeToast(title = title, subTitle = toastSubTitle)
    }
    media.setRecordUrl(url = null)
    suspendRecording()
}

fun Model.resumeRecording(): Boolean {
    currentRecording = recordingsStorage.createRecording(recording = stream.value.recording.clone())
    if (currentRecording == null) {
        return false
    }
    media.setRecordUrl(url = currentRecording?.url()?.toString())
    startRecorderIfNeeded()
    return true
}

fun Model.suspendRecording() {
    stopRecorderIfNeeded()
    updateRecordingLength(now = Instant.now())
    currentRecording = null
}

fun Model.startRecorderIfNeeded() {
    if (isRecorderRecording) {
        return
    }
    if (!(isRecording.value || stream.value.replay.enabled)) {
        return
    }
    isRecorderRecording = true
    val bitrate = stream.value.recording.videoBitrate.toInt()
    val keyFrameInterval = stream.value.recording.maxKeyFrameInterval.toInt()
    val audioBitrate = stream.value.recording.audioBitrate.toInt()
    media.startRecording(
        url = if (isRecording.value) currentRecording?.url()?.toString() else null,
        replay = stream.value.replay.enabled,
        videoCodec = stream.value.recording.videoCodec,
        videoBitrate = if (bitrate != 0) bitrate else null,
        keyFrameInterval = if (keyFrameInterval != 0) keyFrameInterval else null,
        audioBitrate = if (audioBitrate != 0) audioBitrate else null
    )
}

fun Model.stopRecorderIfNeeded(forceStop: Boolean = false) {
    if (!isRecorderRecording) {
        return
    }
    if (forceStop || (!isRecording.value && !stream.value.replay.enabled)) {
        media.stopRecording()
        isRecorderRecording = false
    }
}

fun Model.updateRecordingLength(now: Instant) {
    val current = currentRecording
    if (current != null) {
        val elapsed = TODO("uptimeFormatter.string(from:) has no direct Android equivalent")
        val url = current.url()
        val size = if (url != null) url.length().formatBytes() else "-"
        recording.length = "$elapsed ($size)"
        if (isWatchLocal()) {
            sendRecordingLengthToWatch(recordingLength = recording.length)
        }
    } else if (recording.length != noValue) {
        recording.length = noValue
        if (isWatchLocal()) {
            sendRecordingLengthToWatch(recordingLength = recording.length)
        }
    }
}

fun Model.toggleRecording() {
    if (isRecording.value) {
        stopRecording()
    } else {
        startRecording()
    }
}

fun Model.setIsRecording(value: Boolean) {
    isRecording.value = value
    updateLiveActivity()
    updateMacStatusItem()
    setQuickButton(type = SettingsQuickButtonType.RECORD, isOn = value)
    updatePictureInPicture()
    if (isWatchLocal()) {
        sendIsRecordingToWatch(isRecording = isRecording)
    }
    remoteControlStateChanged(state = RemoteControlAssistantStreamerState(recording = isRecording))
}

fun Model.setCleanRecordings() {
    media.setCleanRecordings(enabled = stream.value.recording.cleanRecordings)
}

fun Model.isShowingStatusRecording(): Boolean {
    return isRecording.value
}

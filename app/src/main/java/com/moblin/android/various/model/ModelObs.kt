package com.moblin.android.various.model

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.moblin.android.common.various.formatOneDecimal
import com.moblin.android.common.various.noValue
import com.moblin.android.localized
import com.moblin.android.obs.ObsAudioInputVolume
import com.moblin.android.obs.ObsOutputState
import com.moblin.android.obs.ObsWebSocket
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow

private const val TAG = "Model"

data class ObsSceneInput(
    var id: UUID = UUID.randomUUID(),
    var name: String,
    var muted: Boolean? = null,
)

data class ObsSceneMediaSource(
    var id: UUID = UUID.randomUUID(),
    var name: String,
    var input: String,
)

class QuickButtonObs {
    var sourceFetchScreenshot = false
    var sourceScreenshotIsFetching = false
    var recording = false
    var audioVolumeLatest: String = ""
    val streamingState = MutableStateFlow(ObsOutputState.STOPPED)
    val recordingState = MutableStateFlow(ObsOutputState.STOPPED)
    val sceneInputs = MutableStateFlow<List<ObsSceneInput>>(emptyList())
    val sceneMediaSources = MutableStateFlow<List<ObsSceneMediaSource>>(emptyList())
    val audioVolume = MutableStateFlow(noValue)
    val currentScenePicker = MutableStateFlow("")
    val currentScene = MutableStateFlow("")
    val scenes = MutableStateFlow<List<String>>(emptyList())
    val screenshot = MutableStateFlow<Bitmap?>(null)
    val streaming = MutableStateFlow(false)
    val fixOngoing = MutableStateFlow(false)
    val audioDelay = MutableStateFlow(0)

    fun startObsSourceScreenshot() {
        screenshot.value = null
        sourceFetchScreenshot = true
        sourceScreenshotIsFetching = false
    }

    fun stopObsSourceScreenshot() {
        sourceFetchScreenshot = false
    }
}

fun Model.updateObsSourceScreenshot() {
    if (!obsQuickButton.sourceFetchScreenshot) {
        return
    }
    if (obsQuickButton.sourceScreenshotIsFetching) {
        return
    }
    if (obsQuickButton.currentScene.value.isEmpty()) {
        return
    }
    obsWebSocket?.getSourceScreenshot(
        name = obsQuickButton.currentScene.value,
        onSuccess = { data ->
            obsQuickButton.screenshot.value = BitmapFactory.decodeByteArray(data, 0, data.size)
            obsQuickButton.sourceScreenshotIsFetching = false
        },
        onError = { message ->
            Log.d(TAG, "Failed to update screenshot with error $message")
            obsQuickButton.screenshot.value = null
            obsQuickButton.sourceScreenshotIsFetching = false
        },
    )
}

fun Model.setObsAudioDelay(offset: Int) {
    if (stream.obsSourceName.isEmpty()) {
        return
    }
    obsWebSocket?.setInputAudioSyncOffset(
        name = stream.obsSourceName,
        offsetInMs = offset,
        onSuccess = {
            updateObsAudioDelay()
        },
        onError = { _ ->
        },
    )
}

fun Model.updateObsAudioDelay() {
    if (stream.obsSourceName.isEmpty()) {
        return
    }
    obsWebSocket?.getInputAudioSyncOffset(
        name = stream.obsSourceName,
        onSuccess = { offset ->
            obsQuickButton.audioDelay.value = offset
        },
        onError = { _ ->
        },
    )
}

fun Model.isObsConnected(): Boolean {
    return obsWebSocket?.isConnected() ?: false
}

fun Model.obsConnectionErrorMessage(): String {
    return obsWebSocket?.connectionErrorMessage ?: ""
}

fun Model.listObsScenes(updateAudioInputs: Boolean = false) {
    obsWebSocket?.getSceneList(
        onSuccess = { list ->
            obsQuickButton.currentScenePicker.value = list.current
            obsQuickButton.currentScene.value = list.current
            obsQuickButton.scenes.value = list.scenes
            if (updateAudioInputs) {
                updateObsAudioInputs(sceneName = list.current)
            }
            updateObsMediaInputs(sceneName = list.current)
            updateStatusObsText()
        },
        onError = { _ ->
        },
    )
}

fun Model.updateObsMediaInputs(sceneName: String) {
    obsWebSocket?.getSceneItemList(
        sceneName = sceneName,
        onSuccess = { sceneItems ->
            val names = sceneItems
                .filter { it.inputKind == "ffmpeg_source" }
                .map { it.sourceName }
            if (names.isEmpty()) {
                return@getSceneItemList
            }
            obsWebSocket?.getMediaSourcesSettingsBatch(
                inputNames = names,
                onSuccess = { settings ->
                    val sources = mutableListOf<ObsSceneMediaSource>()
                    for ((index, name) in names.withIndex()) {
                        val setting = settings[index] ?: continue
                        if (setting.isLocalFile) {
                            continue
                        }
                        sources.add(ObsSceneMediaSource(name = name, input = setting.input))
                    }
                    obsQuickButton.sceneMediaSources.value = sources
                },
                onError = { _ ->
                    obsQuickButton.sceneMediaSources.value = emptyList()
                },
            )
        },
        onError = { _ ->
            obsQuickButton.sceneMediaSources.value = emptyList()
        },
    )
}

fun Model.setObsMediaSourceSettings(name: String, input: String) {
    obsWebSocket?.setMediaSourceSettings(name = name, input = input)
}

fun Model.updateObsAudioInputs(sceneName: String) {
    obsWebSocket?.getInputList(
        onSuccess = { inputs ->
            obsWebSocket?.getSpecialInputs(
                onSuccess = { specialInputs ->
                    obsWebSocket?.getSceneItemList(
                        sceneName = sceneName,
                        onSuccess = { sceneItems ->
                            if (sceneItems.isEmpty()) {
                                obsQuickButton.sceneInputs.value = emptyList()
                                return@getSceneItemList
                            }
                            val obsSceneInputs = mutableListOf<ObsSceneInput>()
                            for (input in inputs) {
                                if (specialInputs.mics().contains(input)) {
                                    obsSceneInputs.add(ObsSceneInput(name = input))
                                } else if (sceneItems.any { it.sourceName == input }) {
                                    val sceneItem = sceneItems.firstOrNull { it.sourceName == input }
                                    if (sceneItem?.sceneItemEnabled == true) {
                                        obsSceneInputs.add(ObsSceneInput(name = input))
                                    }
                                }
                            }
                            obsWebSocket?.getInputMuteBatch(
                                inputNames = obsSceneInputs.map { it.name },
                                onSuccess = { muteds ->
                                    if (muteds.size != obsSceneInputs.size) {
                                        obsQuickButton.sceneInputs.value = emptyList()
                                        return@getInputMuteBatch
                                    }
                                    for ((i, muted) in muteds.withIndex()) {
                                        obsSceneInputs[i].muted = muted
                                    }
                                    obsQuickButton.sceneInputs.value = obsSceneInputs
                                },
                                onError = { _ ->
                                    obsQuickButton.sceneInputs.value = emptyList()
                                },
                            )
                        },
                        onError = { _ ->
                            obsQuickButton.sceneInputs.value = emptyList()
                        },
                    )
                },
                onError = { _ ->
                    obsQuickButton.sceneInputs.value = emptyList()
                },
            )
        },
        onError = { _ ->
            obsQuickButton.sceneInputs.value = emptyList()
        },
    )
}

fun Model.setObsScene(name: String) {
    updateObsMediaInputs(sceneName = name)
    obsWebSocket?.setCurrentProgramScene(
        name = name,
        onSuccess = {
            obsQuickButton.currentScene.value = name
            updateObsAudioInputs(sceneName = name)
        },
        onError = { message ->
            makeErrorToast(
                title = localized("Failed to set OBS scene to $name"),
                subTitle = message,
            )
        },
    )
}

fun Model.updateObsStatus() {
    if (!isObsConnected()) {
        obsQuickButton.audioVolumeLatest = noValue
        return
    }
    obsWebSocket?.getStreamStatus(
        onSuccess = { state ->
            obsWebsocketStreamStatusChanged(active = state.active, state = state.state)
        },
        onError = { _ ->
            obsWebsocketStreamStatusChanged(active = false, state = null)
        },
    )
    obsWebSocket?.getRecordStatus(
        onSuccess = { status ->
            obsWebsocketRecordStatusChanged(active = status.active, state = null)
        },
        onError = { _ ->
            obsWebsocketRecordStatusChanged(active = false, state = null)
        },
    )
    listObsScenes()
}

private fun Model.isStreamLikelyBroken(now: Instant): Boolean {
    try {
        if (streamState == StreamState.DISCONNECTED) {
            return true
        }
        if (media.srtDroppedPacketsTotal > previousSrtDroppedPacketsTotal) {
            streamBecameBrokenTime = now
            return true
        }
        val becameBrokenTime = streamBecameBrokenTime
        if (becameBrokenTime != null) {
            if (Duration.between(becameBrokenTime, now) < Duration.ofSeconds(15)) {
                return true
            } else if (obsQuickButton.currentScene.value != stream.obsBrbScene) {
                return true
            }
        }
        if (stream.obsBrbSceneVideoSourceBroken) {
            val scene = getSelectedScene()
            if (scene != null) {
                when (scene.videoSource.cameraPosition) {
                    CameraPosition.SRTLA -> {
                        val srtlaStream = getSrtlaStream(id = scene.videoSource.srtlaCameraId)
                        if (srtlaStream != null) {
                            if (ingests.srtla?.isStreamConnected(streamId = srtlaStream.streamId) ==
                                false
                            ) {
                                streamBecameBrokenTime = now
                                return true
                            }
                        }
                    }
                    CameraPosition.SRT_CLIENT -> {
                        if (!activeBufferedVideoIds.contains(scene.videoSource.srtClientCameraId)) {
                            streamBecameBrokenTime = now
                            return true
                        }
                    }
                    CameraPosition.RTMP -> {
                        val rtmpStream = getRtmpStream(id = scene.videoSource.rtmpCameraId)
                        if (rtmpStream != null) {
                            if (ingests.rtmp?.isStreamConnected(streamKey = rtmpStream.streamKey) ==
                                false
                            ) {
                                streamBecameBrokenTime = now
                                return true
                            }
                        }
                    }
                    else -> {
                    }
                }
            }
        }
        streamBecameBrokenTime = null
        return false
    } finally {
        previousSrtDroppedPacketsTotal = media.srtDroppedPacketsTotal
    }
}

fun Model.updateObsSceneSwitcher(now: Instant) {
    if (!isLive) {
        return
    }
    if (stream.obsMainScene.isEmpty()) {
        return
    }
    if (stream.obsBrbScene.isEmpty()) {
        return
    }
    if (obsQuickButton.currentScene.value.isEmpty()) {
        return
    }
    if (!isObsConnected()) {
        return
    }
    if (stream.streamingDirectlyToObs) {
        updateObsSceneSwitcherStreamingDirectlyToObs(now = now)
    } else {
        updateObsSceneSwitcherStreamingViaRelay(now = now)
    }
}

private fun Model.updateObsSceneSwitcherStreamingDirectlyToObs(now: Instant) {
    val startTime = streamStartTime ?: return
    val uptime = Duration.between(startTime, now)
    if (uptime < Duration.ofSeconds(10)) {
        if (uptime <= Duration.ofSeconds(5)) {
            return
        }
        if (isStreamLikelyBroken(now = now)) {
            switchToBrbSceneIfNeeded()
        } else {
            switchToMainSceneIfNeeded()
        }
    } else {
        if (isStreamLikelyBroken(now = now)) {
            switchToBrbSceneIfNeeded()
        } else {
            if (obsQuickButton.currentScene.value == stream.obsBrbScene) {
                makeStreamReconnectFixAudioToast()
                stopNetStream()
                streamState = StreamState.DISCONNECTED
                reconnectTimer.startSingleShot(timeout = 5) {
                    Log.i(TAG, "stream: Reconnecting because of OBS scene switcher")
                    startNetStream()
                }
            }
        }
    }
}

private fun Model.updateObsSceneSwitcherStreamingViaRelay(now: Instant) {
    if (isStreamLikelyBroken(now = now)) {
        switchToBrbSceneIfNeeded()
    } else {
        switchToMainSceneIfNeeded()
    }
}

private fun Model.switchToMainSceneIfNeeded() {
    if (obsQuickButton.currentScene.value != stream.obsBrbScene) {
        return
    }
    makeStreamLikelyWorkingToast(scene = stream.obsMainScene)
    setObsScene(name = stream.obsMainScene)
}

private fun Model.switchToBrbSceneIfNeeded() {
    if (obsQuickButton.currentScene.value != stream.obsMainScene) {
        return
    }
    makeStreamLikelyBrokenToast(scene = stream.obsBrbScene)
    setObsScene(name = stream.obsBrbScene)
}

private fun Model.makeStreamLikelyBrokenToast(scene: String) {
    makeErrorToast(
        title = localized("😠 Stream likely broken 😠"),
        subTitle = localized("Trying to switch OBS scene to $scene"),
    )
}

private fun Model.makeStreamReconnectFixAudioToast() {
    makeToast(
        title = localized("😠 Stream audio likely broken 😠"),
        subTitle = localized("Restarting stream to OBS"),
    )
}

private fun Model.makeStreamLikelyWorkingToast(scene: String) {
    makeToast(
        title = localized("🥳 Stream likely working 🥳"),
        subTitle = localized("Trying to switch OBS scene to $scene"),
    )
}

fun Model.reloadObsWebSocket() {
    obsWebSocket?.stop()
    obsWebSocket = null
    if (!isObsRemoteControlConfigured()) {
        updateStatusObsText()
        return
    }
    if (stream.obsWebSocketUrl.isEmpty()) {
        updateStatusObsText()
        return
    }
    obsWebSocket = ObsWebSocket(
        url = stream.obsWebSocketUrl,
        password = stream.obsWebSocketPassword,
        delegate = this,
    )
    obsWebSocket!!.start()
    updateStatusObsText()
}

fun Model.obsWebSocketEnabledUpdated() {
    reloadObsWebSocket()
}

fun Model.obsWebSocketUrlUpdated() {
    reloadObsWebSocket()
}

fun Model.obsWebSocketPasswordUpdated() {
    reloadObsWebSocket()
}

fun Model.obsStartStream() {
    obsWebSocket?.startStream(
        onSuccess = {
        },
        onError = { message ->
            makeErrorToast(
                title = localized("Failed to start OBS stream"),
                subTitle = message,
            )
        },
    )
}

fun Model.obsStopStream() {
    obsWebSocket?.stopStream(
        onSuccess = {
        },
        onError = { message ->
            makeErrorToast(
                title = localized("Failed to stop OBS stream"),
                subTitle = message,
            )
        },
    )
}

fun Model.obsStartRecording() {
    obsWebSocket?.startRecord(
        onSuccess = {
        },
        onError = { message ->
            makeErrorToast(
                title = localized("Failed to start OBS recording"),
                subTitle = message,
            )
        },
    )
}

fun Model.obsStopRecording() {
    obsWebSocket?.stopRecord(
        onSuccess = {
        },
        onError = { message ->
            makeErrorToast(
                title = localized("Failed to stop OBS recording"),
                subTitle = message,
            )
        },
    )
}

fun Model.obsFixStream() {
    val webSocket = obsWebSocket ?: return
    obsQuickButton.fixOngoing.value = true
    webSocket.setInputSettings(
        inputName = stream.obsSourceName,
        onSuccess = {
            obsQuickButton.fixOngoing.value = false
        },
        onError = { message ->
            obsQuickButton.fixOngoing.value = false
            makeErrorToast(
                title = localized("Failed to fix OBS input"),
                subTitle = message,
            )
        },
    )
}

fun Model.obsMuteAudio(inputName: String, muted: Boolean) {
    val webSocket = obsWebSocket ?: return
    webSocket.setInputMute(
        inputName = inputName,
        muted = muted,
        onSuccess = {
        },
        onError = { _ ->
        },
    )
}

fun Model.startObsAudioVolume() {
    obsQuickButton.audioVolumeLatest = noValue
    obsWebSocket?.startAudioVolume()
}

fun Model.stopObsAudioVolume() {
    obsWebSocket?.stopAudioVolume()
}

fun Model.updateObsAudioVolume() {
    if (obsQuickButton.audioVolumeLatest != obsQuickButton.audioVolume.value) {
        obsQuickButton.audioVolume.value = obsQuickButton.audioVolumeLatest
    }
}

fun Model.isShowingStatusObs(): Boolean {
    return database.show.obsStatus && isObsRemoteControlConfigured()
}

private fun Model.statusObsText(): String {
    return if (!isObsRemoteControlConfigured()) {
        localized("Not configured")
    } else if (isObsConnected()) {
        if (obsQuickButton.streaming.value && obsQuickButton.recording) {
            "${obsQuickButton.currentScene.value} (Streaming, Recording)"
        } else if (obsQuickButton.streaming.value) {
            "${obsQuickButton.currentScene.value} (Streaming)"
        } else if (obsQuickButton.recording) {
            "${obsQuickButton.currentScene.value} (Recording)"
        } else {
            obsQuickButton.currentScene.value
        }
    } else {
        obsConnectionErrorMessage()
    }
}

fun Model.updateStatusObsText() {
    statusTopLeft.statusObsText = statusObsText()
}

fun Model.isObsRemoteControlConfigured(): Boolean {
    return stream.obsWebSocketEnabled && stream.obsWebSocketUrl != ""
}

fun Model.obsWebsocketConnected() {
    updateObsStatus()
    updateStatusObsText()
}

fun Model.obsWebsocketSceneChanged(sceneName: String) {
    obsQuickButton.currentScenePicker.value = sceneName
    obsQuickButton.currentScene.value = sceneName
    updateObsAudioInputs(sceneName = sceneName)
    updateObsMediaInputs(sceneName = sceneName)
    updateStatusObsText()
}

fun Model.obsWebsocketInputMuteStateChangedEvent(inputName: String, muted: Boolean) {
    obsQuickButton.sceneInputs.value = obsQuickButton.sceneInputs.value.map { input ->
        if (input.name == inputName) {
            input.muted = muted
        }
        input
    }
    updateStatusObsText()
}

fun Model.obsWebsocketStreamStatusChanged(active: Boolean, state: ObsOutputState?) {
    obsQuickButton.streaming.value = active
    if (state != null) {
        obsQuickButton.streamingState.value = state
    } else if (active) {
        obsQuickButton.streamingState.value = ObsOutputState.STARTED
    } else {
        obsQuickButton.streamingState.value = ObsOutputState.STOPPED
    }
    updateStatusObsText()
}

fun Model.obsWebsocketRecordStatusChanged(active: Boolean, state: ObsOutputState?) {
    obsQuickButton.recording = active
    if (state != null) {
        obsQuickButton.recordingState.value = state
    } else if (active) {
        obsQuickButton.recordingState.value = ObsOutputState.STARTED
    } else {
        obsQuickButton.recordingState.value = ObsOutputState.STOPPED
    }
    updateStatusObsText()
}

fun Model.obsWebsocketAudioVolume(volumes: List<ObsAudioInputVolume>) {
    val volume = volumes.firstOrNull { it.name == stream.obsSourceName }
    if (volume == null) {
        obsQuickButton.audioVolumeLatest =
            localized("Source ${stream.obsSourceName} not found")
        return
    }
    val values = mutableListOf<String>()
    for (v in volume.volumes) {
        if (v.isInfinite()) {
            values.add(localized("Muted"))
        } else {
            values.add(localized("${formatOneDecimal(v)} dB"))
        }
    }
    obsQuickButton.audioVolumeLatest = values.joinToString(", ")
}

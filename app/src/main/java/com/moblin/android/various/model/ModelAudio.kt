package com.moblin.android.various.model

import com.moblin.android.platform.log.Log
import com.moblin.android.common.various.defaultAudioLevel
import com.moblin.android.localized
import com.moblin.android.media.haishinkit.media.processorControlQueue
import com.moblin.android.platform.avfoundation.AVAudioSession
import com.moblin.android.platform.avfoundation.AVAudioSessionDataSourceDescription
import com.moblin.android.platform.avfoundation.AVAudioSessionPortDescription
import com.moblin.android.platform.core.Notification
import com.moblin.android.remotecontrol.RemoteControlAssistantStreamerState
import com.moblin.android.various.KeepSpeakerAlivePlayer
import com.moblin.android.various.SimpleTimer
import com.moblin.android.various.settings.SettingsMic
import com.moblin.android.various.settings.SettingsMicsMic
import com.moblin.android.various.utils.isMac
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.math.abs
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.ContinuationInterceptor

private const val TAG = "Model"

private val mainScope = CoroutineScope(Dispatchers.Main)

private val shared = KeepSpeakerAlivePlayer()

class AudioLevel {
    val level = MutableStateFlow(defaultAudioLevel)

    fun setLevel(level: Float) {
        this.level.value = level
    }
}

class AudioProvider {
    val level = AudioLevel()
    val muted = MutableStateFlow(false)
    val numberOfChannels = MutableStateFlow(0)
    val sampleRate = MutableStateFlow(0.0)

    fun setMuted(muted: Boolean) {
        this.muted.value = muted
    }

    fun setNumberOfChannels(numberOfChannels: Int) {
        this.numberOfChannels.value = numberOfChannels
    }

    fun setSampleRate(sampleRate: Double) {
        this.sampleRate.value = sampleRate
    }
}

class Mic {
    val current = MutableStateFlow(noMic)
    val inputGain = MutableStateFlow(1.0f)
    val inputGainSettable = MutableStateFlow(false)
    val inputGainTimer = SimpleTimer(queue = processorControlQueue.coroutineContext[ContinuationInterceptor] as CoroutineDispatcher)
    var requested: SettingsMicsMic? = null
    val isSwitchTimerRunning = MutableStateFlow(false)

    fun setCurrent(current: SettingsMicsMic) {
        this.current.value = current
    }

    fun setInputGain(inputGain: Float) {
        this.inputGain.value = inputGain
    }

    fun setInputGainSettable(inputGainSettable: Boolean) {
        this.inputGainSettable.value = inputGainSettable
    }

    fun setIsSwitchTimerRunning(isSwitchTimerRunning: Boolean) {
        this.isSwitchTimerRunning.value = isSwitchTimerRunning
    }
}

fun Model.setupInputGainObserver() {
    inputGainObservation = AVAudioSession.sharedInstance().observeInputGain { session ->
        mainScope.launch {
            mic.setInputGain(session.inputGain)
        }
    }
}

fun Model.setupAudio() {
    updateMics()
    if (database.mics.defaultMic.isEmpty()) {
        database.mics.defaultMic = database.mics.mics.value
            .firstOrNull { it.builtInOrientation == database.mic }
            ?.id ?: ""
    }
    val connectedMic = getConnectedMicById(id = database.mics.defaultMic)
    if (connectedMic != null) {
        defaultMic = connectedMic
    } else {
        defaultMic = getHighestPriorityConnectedMic() ?: noMic
    }
    val scene = getSelectedScene()
    if (scene != null && scene.overrideMic) {
        val sceneMic = getConnectedMicById(id = scene.micId)
        if (sceneMic != null) {
            selectMic(mic = sceneMic)
            return
        }
    }
    selectMic(mic = defaultMic)
}

fun Model.setupAudioAfterSettingsImport() {
    mic.setCurrent(noMic)
    setupAudio()
}

fun Model.reloadAudioSession() {
    teardownAudioSession()
    setupAudioSession()
    if (isChatPhone()) {
        return
    }
    media.attachDefaultAudioDevice(builtinDelay = database.debug.builtinAudioAndVideoDelay.value)
}

fun Model.setInputGainIfSupported(inputGain: Float) {
    mic.inputGainTimer.startSingleShot(timeout = 0.5) {
        val session = AVAudioSession.sharedInstance()
        if (!session.isInputGainSettable || inputGain == session.inputGain) {
            return@startSingleShot
        }
        runCatching { session.setInputGain(inputGain) }
    }
}

fun Model.setupAudioSession() {
    val bluetoothOutputOnly = database.debug.bluetoothOutputOnly.value
    val chatPhone = isChatPhone()
    processorControlQueue.launch {
        val session = AVAudioSession.sharedInstance()
        try {
            val bluetoothOption = if (bluetoothOutputOnly) {
                AVAudioSession.CategoryOptions.allowBluetoothA2DP
            } else {
                AVAudioSession.CategoryOptions.allowBluetoothHFP or
                    AVAudioSession.CategoryOptions.bluetoothHighQualityRecording
            }
            if (chatPhone) {
                session.setCategory(
                    AVAudioSession.Category.playback,
                    options = AVAudioSession.CategoryOptions.mixWithOthers,
                )
            } else {
                session.setCategory(
                    AVAudioSession.Category.playAndRecord,
                    options = AVAudioSession.CategoryOptions.mixWithOthers or
                        bluetoothOption or
                        AVAudioSession.CategoryOptions.defaultToSpeaker,
                )
            }
            session.setPreferredSampleRate(48000.0)
            session.setPrefersNoInterruptionsFromSystemAlerts(true)
            session.setActive(true)
            Log.i(TAG, "audio: Preferred sample rate: ${session.preferredSampleRate}")
        } catch (error: Exception) {
            mainScope.launch {
                makeErrorToast(
                    title = "Audio session setup failed",
                    subTitle = error.localizedMessage,
                )
            }
        }
        mainScope.launch {
            setAllowHapticsAndSystemSoundsDuringRecording()
        }
    }
}

fun Model.teardownAudioSession() {
    processorControlQueue.launch {
        try {
            AVAudioSession.sharedInstance().setActive(false)
        } catch (error: Exception) {
            Log.i(TAG, "Failed to stop audio session with error: $error")
        }
    }
}

fun Model.switchMicIfNeededAfterSceneSwitch() {
    updateMics()
    if (database.mics.autoSwitch.value) {
        val scene = getSelectedScene()
        if (scene != null && scene.overrideMic) {
            val sceneMic = getConnectedMicById(id = scene.micId)
            if (sceneMic != null) {
                selectMic(mic = sceneMic)
                return
            }
        }
        if (defaultMic.connected.value) {
            selectMic(mic = defaultMic)
        } else {
            val highestPrioMic = getHighestPriorityConnectedMic()
            if (highestPrioMic != null) {
                selectMic(mic = highestPrioMic)
            }
        }
    }
}

fun Model.switchMicIfNeededAfterNetworkCameraChange() {
    if (database.mics.autoSwitch.value) {
        updateMics()
        val scene = getSelectedScene()
        if (scene != null && scene.overrideMic) {
            val sceneMic = getConnectedMicById(id = scene.micId)
            if (sceneMic != null) {
                selectMic(mic = sceneMic)
                val highestPrioMic = getHighestPriorityConnectedMic()
                if (highestPrioMic != null) {
                    defaultMic = highestPrioMic
                }
                return
            }
        }
        val highestPrioMic = getHighestPriorityConnectedMic()
        if (highestPrioMic != null) {
            selectMic(mic = highestPrioMic)
            defaultMic = highestPrioMic
        }
    }
}

fun Model.markMicAsConnected(id: String) {
    getMicById(id = id)?.let { it._connected.value = true }
}

fun Model.markMicAsDisconnected(id: String) {
    getMicById(id = id)?.let { it._connected.value = false }
}

fun Model.updateMics() {
    updateMics(audioSession = listAudioSessionMics())
}

private fun Model.updateMics(audioSession: List<SettingsMicsMic>) {
    updateMediaPlayerMics()
    updateRistMics()
    updateSrtlaMics()
    updateSrtClientMics()
    updateRtmpMics()
    updateWhipMics()
    updateWhepMics()
    syncMics(found = audioSession, isKind = { it.isAudioSession() }, removeMissing = false)
}

fun Model.updateRtmpMics() {
    syncMics(found = listRtmpMics(), isKind = { it.isRtmp() }, removeMissing = true)
}

fun Model.updateSrtlaMics() {
    syncMics(found = listSrtlaMics(), isKind = { it.isSrtla() }, removeMissing = true)
}

fun Model.updateSrtClientMics() {
    syncMics(found = listSrtClientMics(), isKind = { it.isSrtClient() }, removeMissing = true)
}

fun Model.updateRistMics() {
    syncMics(found = listRistMics(), isKind = { it.isRist() }, removeMissing = true)
}

fun Model.updateWhipMics() {
    syncMics(found = listWhipMics(), isKind = { it.isWhip() }, removeMissing = true)
}

fun Model.updateWhepMics() {
    syncMics(found = listWhepMics(), isKind = { it.isWhep() }, removeMissing = true)
}

fun Model.updateMediaPlayerMics() {
    syncMics(found = listMediaPlayerMics(), isKind = { it.isMediaPlayer() }, removeMissing = true)
}

fun Model.updateAudioSessionMicsAsync(onCompleted: (() -> Unit)? = null) {
    processorControlQueue.launch {
        val audioSessionMics = listAudioSessionMics()
        mainScope.launch {
            syncMics(found = audioSessionMics, isKind = { it.isAudioSession() }, removeMissing = false)
            onCompleted?.invoke()
        }
    }
}

private fun Model.syncMics(
    found: List<SettingsMicsMic>,
    isKind: (SettingsMicsMic) -> Boolean,
    removeMissing: Boolean,
) {
    val mics = database.mics.mics.value.toMutableList()
    if (removeMissing) {
        mics.removeAll { isKind(it) && !found.contains(it) }
    }
    for (mic in mics) {
        if (!isKind(mic)) {
            continue
        }
        val foundMic = found.firstOrNull { it == mic }
        if (foundMic != null) {
            mic.name = foundMic.name
            mic._connected.value = foundMic.connected.value
        } else {
            mic._connected.value = false
        }
    }
    for (mic in found) {
        if (!mics.contains(mic)) {
            mics.add(0, mic)
        }
    }
    database.mics._mics.value = mics
}

fun Model.getMicById(id: String): SettingsMicsMic? {
    return database.mics.mics.value.firstOrNull { it.id == id }
}

fun Model.manualSelectMicById(id: String) {
    val availableMic = getAvailableMicById(id = id) ?: return
    selectMic(mic = availableMic)
    defaultMic = availableMic
}

fun Model.updateMicDelay() {
    media.setAudioDelay(delay = getMicById(id = mic.current.value.id)?.delay?.value ?: 0.0)
}

fun Model.selectMicDefault(mic: SettingsMicsMic) {
    media.attachBufferedAudio(cameraId = null)
    val preferStereoMic = database.audio.preferStereoMic.value
    processorControlQueue.launch {
        val session = AVAudioSession.sharedInstance()
        val inputPort = session.availableInputs?.firstOrNull { it.uid == mic.inputUid } ?: return@launch
        runCatching { session.setPreferredInput(inputPort) }
        val dataSourceId = mic.dataSourceId ?: return@launch
        val dataSource = inputPort.dataSources?.firstOrNull { it.dataSourceID == dataSourceId } ?: return@launch
        runCatching { setBuiltInMicAudioMode(dataSource = dataSource, preferStereoMic = preferStereoMic) }
        runCatching { session.setInputDataSource(dataSource) }
    }
    media.attachDefaultAudioDevice(builtinDelay = database.debug.builtinAudioAndVideoDelay.value)
    remoteControlStateChanged(state = RemoteControlAssistantStreamerState(mic = mic.id))
}

fun Model.keepSpeakerAlive(now: Instant) {
    KeepSpeakerAlivePlayer.shared.playIfNeeded(now = now)
}

fun Model.updateAudioLevel() {
    val newAudioLevel = media.getAudioLevel()
    val newNumberOfAudioChannels = media.getNumberOfAudioChannels()
    val newSampleRate = media.getAudioSampleRate()
    if (newNumberOfAudioChannels != audio.numberOfChannels.value) {
        audio.setNumberOfChannels(newNumberOfAudioChannels)
    }
    if (newSampleRate != audio.sampleRate.value) {
        audio.setSampleRate(newSampleRate)
    }
    if (newAudioLevel == audio.level.level.value) {
        return
    }
    if (abs(audio.level.level.value - newAudioLevel) > 7) {
        audio.level.setLevel(newAudioLevel)
        if (isWatchLocal()) {
            sendAudioLevelToWatch(audioLevel = audio.level.level.value)
        }
    }
}

fun Model.setTalkbackMic(id: String) {
    database.talkback.micId.value = id
    updateTalkback()
}

fun Model.updateTalkback() {
    if (database.talkback.enabled.value) {
        val mic = getMicById(id = database.talkback.micId.value)
        if (mic != null) {
            startTalkback(mic = mic)
        } else {
            stopTalkback()
        }
    } else {
        stopTalkback()
    }
}

fun Model.handleSystemVolumeDidChange(notification: Notification) {
    val userInfo = notification.userInfo
    val volume = userInfo["Volume"] as? Float ?: return
    val reason = userInfo["Reason"] as? String ?: return
    val sequenceNumber = userInfo["SequenceNumber"] as? Int ?: return
    mainScope.launch {
        handleSystemVolumeDidChange(volume = volume, reason = reason, sequenceNumber = sequenceNumber)
    }
}

fun Model.handleAudioRouteChange(notification: Notification) {
    mainScope.launch {
        handleAudioRouteChange()
    }
}

private fun Model.handleAudioRouteChange() {
    updateIsBluetoothAudioOutput()
    stopTextToSpeechIfOutputNotAllowed()
    if (isMac()) {
        updateAudioSessionMicsAsync()
        return
    }
    switchMicIfNeededAfterRouteChange()
    val session = AVAudioSession.sharedInstance()
    mic.setInputGainSettable(session.isInputGainSettable)
    mic.setInputGain(session.inputGain)
}

fun Model.updateIsBluetoothAudioOutput() {
    isBluetoothAudioOutput = AVAudioSession.sharedInstance().currentRoute.outputs.any {
        listOf(
            AVAudioSession.Port.bluetoothA2DP,
            AVAudioSession.Port.bluetoothHFP,
            AVAudioSession.Port.bluetoothLE
        ).contains(it.portType)
    }
}

private fun Model.handleSystemVolumeDidChange(volume: Float, reason: String, sequenceNumber: Int) {
    if (sequenceNumber == latestVolumeChangeSequenceNumber) {
        return
    }
    latestVolumeChangeSequenceNumber = sequenceNumber
    if (reason == "ExplicitVolumeChange" && database.selfieStick.enabled.value && isAppActive) {
        if (initialVolume == null) {
            initialVolume = volume
        }
        val initialVolumeValue = initialVolume ?: return
        if (volume != initialVolumeValue) {
            setSystemVolume(volume = initialVolumeValue)
            executeSelfieStickAction()
        } else if (isVolumeMinOrMax(volume = volume)
            && latestSetVolumeTime.duration(to = com.moblin.android.platform.core.ContinuousClock.now).toDouble(kotlin.time.DurationUnit.SECONDS) > 1.0
        ) {
            executeSelfieStickAction()
        }
    } else {
        initialVolume = volume
    }
}

private fun Model.executeSelfieStickAction() {
    handleControllerFunction(
        buttonId = "s:button",
        function = database.selfieStick.function.value,
        functionData = database.selfieStick.functionData.value,
        pressed = false
    )
}

private fun Model.isVolumeMinOrMax(volume: Float): Boolean {
    return volume == 0f || volume == 1f
}

private fun Model.setSystemVolume(volume: Float) {
    val volumeSlider = volumeView.subviews.firstOrNull { it is com.moblin.android.platform.uikit.UISlider } as? com.moblin.android.platform.uikit.UISlider ?: return; mainScope.launch { delay(100); latestSetVolumeTime = com.moblin.android.platform.core.ContinuousClock.now; volumeSlider.value = volume }
}

private fun Model.switchMicIfNeededAfterRouteChange() {
    updateAudioSessionMicsAsync {
        if (database.mics.autoSwitch.value) {
            autoSwitchMicIfNeededAfterRouteChange()
        } else {
            manualSwitchMicIfNeededAfterRouteChange()
        }
    }
}

private fun Model.getActiveAudioSessionMic(): SettingsMicsMic? {
    val inputPort = AVAudioSession.sharedInstance().currentRoute.inputs.firstOrNull() ?: return null
    return makeAudioSessionMic(inputPort = inputPort, dataSource = inputPort.preferredDataSource)
}

private fun Model.autoSwitchMicIfNeededAfterRouteChange() {
    val scene = getSelectedScene()
    if (scene != null && scene.overrideMic) {
        if (mic.current.value.isAudioSession()) {
            val activeMic = getActiveAudioSessionMic()
            if (activeMic != null && activeMic != mic.current.value) {
                if (getMicPriority(mic = activeMic) > getMicPriority(mic = defaultMic)) {
                    defaultMic = activeMic
                }
                selectMicDefault(mic = mic.current.value)
            }
        } else {
            val activeMic = getActiveAudioSessionMic()
            if (activeMic != null && getMicPriority(mic = activeMic) > getMicPriority(mic = defaultMic)) {
                defaultMic = activeMic
            }
        }
    } else {
        val activeMic = getActiveAudioSessionMic()
        if (activeMic != null && getMicPriority(mic = activeMic) > getMicPriority(mic = mic.current.value)) {
            selectMic(mic = activeMic)
            defaultMic = activeMic
        } else if (getActiveAudioSessionMic() == mic.current.value) {
        } else if (mic.current.value.connected.value && mic.current.value.isAudioSession()) {
            selectMicDefault(mic = mic.current.value)
        } else {
            val highestPrioMic = getHighestPriorityConnectedMic()
            if (highestPrioMic != null) {
                selectMic(mic = highestPrioMic)
                defaultMic = highestPrioMic
            }
        }
    }
}

private fun Model.manualSwitchMicIfNeededAfterRouteChange() {
    if (mic.current.value.isAudioSession() && getActiveAudioSessionMic() != mic.current.value) {
        selectMicDefault(mic = mic.current.value)
    }
}

private fun Model.getMicPriority(mic: SettingsMicsMic): Int {
    val priority = database.mics.mics.value.indexOfFirst { it.id == mic.id }
    return if (priority != -1) -priority else Int.MIN_VALUE
}

private fun Model.getHighestPriorityConnectedMic(): SettingsMicsMic? {
    return database.mics.mics.value.firstOrNull { it.connected.value }
}

private fun Model.makeMicChangeToast(name: String) {
    makeToast(title = localized("Switched mic to '$name'"))
}

private fun Model.listRtmpMics(): List<SettingsMicsMic> {
    return database.rtmpServer.streams.map {
        SettingsMicsMic(
            name = it.camera(),
            inputUid = it.id.toString(),
            connected = activeBufferedVideoIds.contains(it.id),
        )
    }
}

private fun Model.listSrtlaMics(): List<SettingsMicsMic> {
    return database.srtlaServer.streams.map {
        SettingsMicsMic(
            name = it.camera(),
            inputUid = it.id.toString(),
            connected = activeBufferedVideoIds.contains(it.id),
        )
    }
}

private fun Model.listSrtClientMics(): List<SettingsMicsMic> {
    return database.srtClient.streams.map {
        SettingsMicsMic(
            name = it.camera(),
            inputUid = it.id.toString(),
            connected = activeBufferedVideoIds.contains(it.id),
        )
    }
}

private fun Model.listRistMics(): List<SettingsMicsMic> {
    return database.ristServer.streams.map {
        SettingsMicsMic(
            name = it.camera(),
            inputUid = it.id.toString(),
            connected = activeBufferedVideoIds.contains(it.id),
        )
    }
}

private fun Model.listWhipMics(): List<SettingsMicsMic> {
    return database.whipServer.streams.map {
        SettingsMicsMic(
            name = it.camera(),
            inputUid = it.id.toString(),
            connected = activeBufferedVideoIds.contains(it.id),
        )
    }
}

private fun Model.listWhepMics(): List<SettingsMicsMic> {
    return database.whepClient.streams.map {
        SettingsMicsMic(
            name = it.camera(),
            inputUid = it.id.toString(),
            connected = activeBufferedVideoIds.contains(it.id),
        )
    }
}

private fun Model.listMediaPlayerMics(): List<SettingsMicsMic> {
    return database.mediaPlayers.players.map {
        SettingsMicsMic(name = it.camera(), inputUid = it.id.toString(), connected = true)
    }
}

private fun Model.getConnectedMicById(id: String): SettingsMicsMic? {
    val mic = getMicById(id = id)
    if (mic == null || !mic.connected.value) {
        return null
    }
    return mic
}

private fun Model.getAvailableMicById(id: String): SettingsMicsMic? {
    val mic = getMicById(id = id)
    if (mic == null) {
        Log.i(TAG, "Mic with id $id not found")
        makeErrorToast(
            title = localized("Mic not found"),
            subTitle = localized("Mic id $id")
        )
        return null
    }
    return mic
}

private fun Model.selectMic(mic: SettingsMicsMic) {
    if (isChatPhone()) {
        return
    }
    this.mic.requested = mic
    trySwitchMic()
}

private fun Model.trySwitchMic() {
    if (mic.isSwitchTimerRunning.value) {
        return
    }
    val requestedMic = mic.requested ?: return
    mic.requested = null
    if (requestedMic == mic.current.value) {
        return
    }
    if (mic.current.value != noMic) {
        makeMicChangeToast(name = requestedMic.name)
    }
    if (isRtmpMic(mic = requestedMic)) {
        attachBufferedAudio(cameraId = getRtmpMicCameraId(mic = requestedMic), micId = requestedMic.id)
    } else if (isSrtlaMic(mic = requestedMic)) {
        attachBufferedAudio(cameraId = getSrtlaMicCameraId(mic = requestedMic), micId = requestedMic.id)
    } else if (isSrtClientMic(mic = requestedMic)) {
        attachBufferedAudio(cameraId = getSrtClientCameraId(mic = requestedMic), micId = requestedMic.id)
    } else if (isRistMic(mic = requestedMic)) {
        attachBufferedAudio(cameraId = getRistMicCameraId(mic = requestedMic), micId = requestedMic.id)
    } else if (isWhipMic(mic = requestedMic)) {
        attachBufferedAudio(cameraId = getWhipMicCameraId(mic = requestedMic), micId = requestedMic.id)
    } else if (isWhepMic(mic = requestedMic)) {
        attachBufferedAudio(cameraId = getWhepMicCameraId(mic = requestedMic), micId = requestedMic.id)
    } else if (isMediaPlayerMic(mic = requestedMic)) {
        attachBufferedAudio(cameraId = getMediaPlayerMicCameraId(mic = requestedMic), micId = requestedMic.id)
    } else {
        selectMicDefault(mic = requestedMic)
    }
    mic.setCurrent(requestedMic)
    updateMicDelay()
    mic.setIsSwitchTimerRunning(true)
    mainScope.launch {
        delay(1000)
        mic.setIsSwitchTimerRunning(false)
        trySwitchMic()
    }
}

private fun Model.isRtmpMic(mic: SettingsMicsMic): Boolean {
    val id = runCatching { UUID.fromString(mic.inputUid) }.getOrNull() ?: return false
    return getRtmpStream(id = id) != null
}

private fun Model.isSrtlaMic(mic: SettingsMicsMic): Boolean {
    val id = runCatching { UUID.fromString(mic.inputUid) }.getOrNull() ?: return false
    return getSrtlaStream(id = id) != null
}

private fun Model.isSrtClientMic(mic: SettingsMicsMic): Boolean {
    val id = runCatching { UUID.fromString(mic.inputUid) }.getOrNull() ?: return false
    return getSrtClientStream(id = id) != null
}

private fun Model.isRistMic(mic: SettingsMicsMic): Boolean {
    val id = runCatching { UUID.fromString(mic.inputUid) }.getOrNull() ?: return false
    return getRistStream(id = id) != null
}

private fun Model.isWhipMic(mic: SettingsMicsMic): Boolean {
    val id = runCatching { UUID.fromString(mic.inputUid) }.getOrNull() ?: return false
    return getWhipStream(id = id) != null
}

private fun Model.isWhepMic(mic: SettingsMicsMic): Boolean {
    val id = runCatching { UUID.fromString(mic.inputUid) }.getOrNull() ?: return false
    return getWhepStream(id = id) != null
}

private fun Model.isMediaPlayerMic(mic: SettingsMicsMic): Boolean {
    val id = runCatching { UUID.fromString(mic.inputUid) }.getOrNull() ?: return false
    return getMediaPlayer(id = id) != null
}

private fun Model.getRtmpMicCameraId(mic: SettingsMicsMic): UUID? {
    val id = runCatching { UUID.fromString(mic.inputUid) }.getOrNull() ?: return null
    return getRtmpStream(id = id)?.id
}

private fun Model.getSrtlaMicCameraId(mic: SettingsMicsMic): UUID? {
    return getSrtlaStream(idString = mic.inputUid)?.id
}

private fun Model.getSrtClientCameraId(mic: SettingsMicsMic): UUID? {
    return getSrtClientStream(idString = mic.inputUid)?.id
}

private fun Model.getRistMicCameraId(mic: SettingsMicsMic): UUID? {
    return getRistStream(idString = mic.inputUid)?.id
}

private fun Model.getWhipMicCameraId(mic: SettingsMicsMic): UUID? {
    return getWhipStream(idString = mic.inputUid)?.id
}

private fun Model.getWhepMicCameraId(mic: SettingsMicsMic): UUID? {
    return getWhepStream(idString = mic.inputUid)?.id
}

private fun Model.getMediaPlayerMicCameraId(mic: SettingsMicsMic): UUID? {
    return getMediaPlayer(idString = mic.inputUid)?.id
}

private fun Model.attachBufferedAudio(cameraId: UUID?, micId: String) {
    if (cameraId == null) {
        Log.i(TAG, "Cannot attach unknown mic $micId")
        return
    }
    media.attachBufferedAudio(cameraId = cameraId)
    remoteControlStateChanged(state = RemoteControlAssistantStreamerState(mic = micId))
}

private fun Model.startTalkback(mic: SettingsMicsMic) {
    if (isRtmpMic(mic = mic)) {
        media.setTalkback(cameraId = getRtmpMicCameraId(mic = mic))
    } else if (isSrtlaMic(mic = mic)) {
        media.setTalkback(cameraId = getSrtlaMicCameraId(mic = mic))
    } else if (isSrtClientMic(mic = mic)) {
        media.setTalkback(cameraId = getSrtClientCameraId(mic = mic))
    } else if (isRistMic(mic = mic)) {
        media.setTalkback(cameraId = getRistMicCameraId(mic = mic))
    } else if (isWhipMic(mic = mic)) {
        media.setTalkback(cameraId = getWhipMicCameraId(mic = mic))
    } else if (isWhepMic(mic = mic)) {
        media.setTalkback(cameraId = getWhepMicCameraId(mic = mic))
    } else if (isMediaPlayerMic(mic = mic)) {
        media.setTalkback(cameraId = getMediaPlayerMicCameraId(mic = mic))
    } else {
        media.setTalkback(cameraId = null)
    }
}

private fun Model.stopTalkback() {
    media.setTalkback(cameraId = null)
}

private fun setBuiltInMicAudioMode(
    dataSource: AVAudioSessionDataSourceDescription,
    preferStereoMic: Boolean
) {
    if (preferStereoMic) {
        if (dataSource.supportedPolarPatterns?.contains(AVAudioSession.PolarPattern.stereo) == true) {
            dataSource.setPreferredPolarPattern(AVAudioSession.PolarPattern.stereo)
        } else {
            dataSource.setPreferredPolarPattern(null)
        }
    } else {
        dataSource.setPreferredPolarPattern(null)
    }
}

private fun listAudioSessionMics(): List<SettingsMicsMic> {
    val mics = mutableListOf<SettingsMicsMic>()
    for (inputPort in AVAudioSession.sharedInstance().availableInputs ?: emptyList()) {
        val dataSources = inputPort.dataSources
        if (dataSources != null && dataSources.isNotEmpty()) {
            val builtInMics = mutableListOf<SettingsMicsMic>()
            for (dataSource in dataSources) {
                val mic = makeAudioSessionMic(inputPort = inputPort, dataSource = dataSource)
                when (mic.builtInOrientation) {
                    SettingsMic.bottom, SettingsMic.top -> builtInMics.add(mic)
                    else -> builtInMics.add(0, mic)
                }
            }
            mics.addAll(builtInMics)
        } else {
            mics.add(makeAudioSessionMic(inputPort = inputPort, dataSource = null))
        }
    }
    return mics
}

private fun makeAudioSessionMic(
    inputPort: AVAudioSessionPortDescription,
    dataSource: AVAudioSessionDataSourceDescription?,
): SettingsMicsMic {
    val mic = SettingsMicsMic()
    mic.inputUid = inputPort.uid
    mic._connected.value = true
    if (dataSource != null) {
        if (inputPort.portType == AVAudioSession.Port.builtInMic) {
            mic.name = dataSource.dataSourceName
            mic.builtInOrientation = getBuiltInMicOrientation(orientation = dataSource.orientation)
        } else {
            mic.name = "${inputPort.portName}: ${dataSource.dataSourceName}"
        }
        mic.dataSourceId = dataSource.dataSourceID
    } else {
        mic.name = inputPort.portName
    }
    return mic
}

private fun getBuiltInMicOrientation(orientation: String?): SettingsMic? {
    if (orientation == null) {
        return null
    }
    return when (orientation) {
        AVAudioSession.Orientation.bottom -> SettingsMic.bottom
        AVAudioSession.Orientation.front -> SettingsMic.front
        AVAudioSession.Orientation.back -> SettingsMic.back
        AVAudioSession.Orientation.top -> SettingsMic.top
        else -> null
    }
}

package com.moblin.android.various.model

import android.util.Log
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
    updateMicsList()
    if (database.mics.defaultMic.isEmpty()) {
        database.mics.defaultMic = database.mics.mics.value
            .firstOrNull { it.builtInOrientation == database.mic }
            ?.id ?: ""
    }
    val mic = getMicById(id = database.mics.defaultMic)
    if (mic != null && mic.connected.value) {
        defaultMic = mic
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
    updateMicsList()
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
        updateMicsList()
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
    database.mics.mics.value.firstOrNull { it.id == id }?.let { it._connected.value = true }
}

fun Model.markMicAsDisconnected(id: String) {
    database.mics.mics.value.firstOrNull { it.id == id }?.let { it._connected.value = false }
}

fun Model.updateMicsList() {
    updateMicsListDatabase(foundMics = listMics())
}

fun Model.updateMicsListAsync(onCompleted: (() -> Unit)? = null) {
    listMicsAsync { mics ->
        updateMicsListDatabase(foundMics = mics)
        onCompleted?.invoke()
    }
}

private fun Model.updateMicsListDatabase(foundMics: List<SettingsMicsMic>) {
    val databaseMics = mutableListOf<SettingsMicsMic>()
    for (mic in database.mics.mics.value) {
        if ((mic.isRtmp()
                || mic.isSrtla()
                || mic.isSrtClient()
                || mic.isRist()
                || mic.isRtsp()
                || mic.isWhip()
                || mic.isWhep()
                || mic.isMediaPlayer()) && !foundMics.contains(mic)
        ) {
            continue
        }
        if (mic.isExternal()) {
            mic._connected.value = foundMics.contains(mic)
            databaseMics.add(mic)
        } else {
            val foundMic = foundMics.firstOrNull { it == mic }
            if (foundMic != null) {
                mic._connected.value = foundMic.connected.value
                databaseMics.add(mic)
            } else {
                databaseMics.add(mic)
            }
        }
        foundMics.firstOrNull { it == mic }?.let { mic.name = it.name }
    }
    for (mic in foundMics) {
        if (!databaseMics.contains(mic)) {
            databaseMics.add(0, mic)
        }
    }
    database.mics._mics.value = databaseMics
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
    shared.playIfNeeded(now = now)
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
    if (isMac()) {
        return
    }
    switchMicIfNeededAfterRouteChange()
    val session = AVAudioSession.sharedInstance()
    mic.setInputGainSettable(session.isInputGainSettable)
    mic.setInputGain(session.inputGain)
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
            && Duration.between(latestSetVolumeTime, Instant.now()).seconds > 1
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
    Unit
}

private fun Model.switchMicIfNeededAfterRouteChange() {
    updateMicsListAsync {
        if (database.mics.autoSwitch.value) {
            autoSwitchMicIfNeededAfterRouteChange()
        } else {
            manualSwitchMicIfNeededAfterRouteChange()
        }
    }
}

private fun Model.getActiveAudioSessionMic(): SettingsMicsMic? {
    val inputPort = AVAudioSession.sharedInstance().currentRoute.inputs.firstOrNull() ?: return null
    val newMic: SettingsMicsMic
    val dataSource = inputPort.preferredDataSource
    if (dataSource != null) {
        val name: String
        var builtInMicOrientation: SettingsMic? = null
        if (inputPort.portType == AVAudioSession.Port.builtInMic) {
            name = dataSource.dataSourceName
            builtInMicOrientation = getBuiltInMicOrientation(orientation = dataSource.orientation)
        } else {
            name = "${inputPort.portName}: ${dataSource.dataSourceName}"
        }
        newMic = SettingsMicsMic()
        newMic.name = name
        newMic.inputUid = inputPort.uid
        newMic.dataSourceId = dataSource.dataSourceID
        newMic.builtInOrientation = builtInMicOrientation
    } else {
        newMic = SettingsMicsMic()
        newMic.name = inputPort.portName
        newMic.inputUid = inputPort.uid
    }
    return newMic
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

private fun Model.listMics(): List<SettingsMicsMic> {
    val mics = mutableListOf<SettingsMicsMic>()
    listMediaPlayerMics(mics)
    listRistMics(mics)
    listSrtlaMics(mics)
    listSrtClientMics(mics)
    listRtmpMics(mics)
    listWhipMics(mics)
    listWhepMics(mics)
    listAudioSessionMics(mics)
    return mics
}

private fun Model.listMicsAsync(onCompleted: (List<SettingsMicsMic>) -> Unit) {
    val mics = mutableListOf<SettingsMicsMic>()
    listMediaPlayerMics(mics)
    listRistMics(mics)
    listSrtlaMics(mics)
    listSrtClientMics(mics)
    listRtmpMics(mics)
    listWhipMics(mics)
    listWhepMics(mics)
    processorControlQueue.launch {
        val audioSessionMics = mics.toMutableList()
        listAudioSessionMics(audioSessionMics)
        mainScope.launch {
            onCompleted(audioSessionMics)
        }
    }
}

private fun Model.listRtmpMics(mics: MutableList<SettingsMicsMic>) {
    for (stream in database.rtmpServer.streams) {
        val mic = SettingsMicsMic()
        mic.name = stream.camera()
        mic.inputUid = stream.id.toString()
        mic._connected.value = isRtmpStreamConnected(streamKey = stream.streamKey)
        mics.add(mic)
    }
}

private fun Model.listSrtlaMics(mics: MutableList<SettingsMicsMic>) {
    for (stream in database.srtlaServer.streams) {
        val mic = SettingsMicsMic()
        mic.name = stream.camera()
        mic.inputUid = stream.id.toString()
        mic._connected.value = isSrtlaStreamConnected(streamId = stream.streamId)
        mics.add(mic)
    }
}

private fun Model.listSrtClientMics(mics: MutableList<SettingsMicsMic>) {
    for (stream in database.srtClient.streams) {
        val mic = SettingsMicsMic()
        mic.name = stream.camera()
        mic.inputUid = stream.id.toString()
        mic._connected.value = isSrtClientStreamConnected(id = stream.id)
        mics.add(mic)
    }
}

private fun Model.listRistMics(mics: MutableList<SettingsMicsMic>) {
    for (stream in database.ristServer.streams) {
        val mic = SettingsMicsMic()
        mic.name = stream.camera()
        mic.inputUid = stream.id.toString()
        mic._connected.value = isRistStreamConnected(port = stream.virtualDestinationPort.toInt())
        mics.add(mic)
    }
}

private fun Model.listWhipMics(mics: MutableList<SettingsMicsMic>) {
    for (stream in database.whipServer.streams) {
        val mic = SettingsMicsMic()
        mic.name = stream.camera()
        mic.inputUid = stream.id.toString()
        mic._connected.value = isWhipStreamConnected(streamId = stream.id)
        mics.add(mic)
    }
}

private fun Model.listWhepMics(mics: MutableList<SettingsMicsMic>) {
    for (stream in database.whepClient.streams) {
        val mic = SettingsMicsMic()
        mic.name = stream.camera()
        mic.inputUid = stream.id.toString()
        mic._connected.value = isWhepStreamConnected(streamId = stream.id)
        mics.add(mic)
    }
}

private fun Model.listMediaPlayerMics(mics: MutableList<SettingsMicsMic>) {
    for (mediaPlayer in database.mediaPlayers.players) {
        val mic = SettingsMicsMic()
        mic.name = mediaPlayer.camera()
        mic.inputUid = mediaPlayer.id.toString()
        mic._connected.value = true
        mics.add(mic)
    }
}

private fun Model.getConnectedMicById(id: String): SettingsMicsMic? {
    val mic = database.mics.mics.value.firstOrNull { it.id == id }
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

private fun listAudioSessionMics(mics: MutableList<SettingsMicsMic>) {
    for (inputPort in AVAudioSession.sharedInstance().availableInputs ?: emptyList()) {
        val dataSources = inputPort.dataSources
        if (dataSources != null && dataSources.isNotEmpty()) {
            addAudioSessionBuiltinMics(mics, inputPort, dataSources)
        } else {
            addAudioSessionExternalMics(mics, inputPort)
        }
    }
}

private fun addAudioSessionBuiltinMics(
    mics: MutableList<SettingsMicsMic>,
    inputPort: AVAudioSessionPortDescription,
    dataSources: List<AVAudioSessionDataSourceDescription>
) {
    val builtInMics = mutableListOf<SettingsMicsMic>()
    for (dataSource in dataSources) {
        val name: String
        var builtInOrientation: SettingsMic? = null
        if (inputPort.portType == AVAudioSession.Port.builtInMic) {
            name = dataSource.dataSourceName
            builtInOrientation = getBuiltInMicOrientation(orientation = dataSource.orientation)
        } else {
            name = "${inputPort.portName}: ${dataSource.dataSourceName}"
        }
        val mic = SettingsMicsMic()
        mic.name = name
        mic.inputUid = inputPort.uid
        mic.dataSourceId = dataSource.dataSourceID
        mic.builtInOrientation = builtInOrientation
        mic._connected.value = true
        when (mic.builtInOrientation) {
            SettingsMic.bottom, SettingsMic.top -> builtInMics.add(mic)
            else -> builtInMics.add(0, mic)
        }
    }
    mics.addAll(builtInMics)
}

private fun addAudioSessionExternalMics(
    mics: MutableList<SettingsMicsMic>,
    inputPort: AVAudioSessionPortDescription
) {
    val mic = SettingsMicsMic()
    mic.name = inputPort.portName
    mic.inputUid = inputPort.uid
    mic._connected.value = true
    mics.add(mic)
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

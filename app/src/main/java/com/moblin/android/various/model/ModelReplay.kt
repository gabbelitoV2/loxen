package com.moblin.android.various.model

import android.graphics.Bitmap
import com.moblin.android.localized
import com.moblin.android.various.ReplayBuffer
import com.moblin.android.various.ReplayBufferFile
import com.moblin.android.various.ReplayDelegate
import com.moblin.android.various.ReplayFrameExtractor
import com.moblin.android.various.settings.SettingsReplay
import com.moblin.android.various.settings.SettingsReplaySpeed
import com.moblin.android.various.storages.ReplaySettings
import com.moblin.android.videoeffects.replay.ReplayEffect
import com.moblin.android.videoeffects.replay.ReplayEffectDelegate
import com.moblin.android.videoeffects.replay.ReplayEffectTransitionMode
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private val mainScope = CoroutineScope(Dispatchers.Main)

class ReplayProvider {
    val selectedId = MutableStateFlow<UUID?>(null)
    val isSaving = MutableStateFlow(false)
    val previewImage = MutableStateFlow<Bitmap?>(null)
    val isPlaying = MutableStateFlow(false)
    val startFromEnd = MutableStateFlow(10.0)
    val speed = MutableStateFlow<SettingsReplaySpeed?>(SettingsReplaySpeed.one)
    val instantReplayCountdown = MutableStateFlow(0)
    val timeLeft = MutableStateFlow(0)
}

private class ReplayDelegateAdapter(private val model: Model) : ReplayDelegate {
    override fun replayOutputFrame(
        image: Bitmap,
        offset: Double,
        video: ReplayBufferFile,
        completion: (suspend () -> Unit)?,
    ) {
        model.replayOutputFrame(
            image = image,
            offset = offset,
            video = video,
            completion = unitCompletion(completion),
        )
    }
}

private class ReplayEffectDelegateAdapter(private val model: Model) : ReplayEffectDelegate {
    override fun replayEffectStatus(timeLeft: Int) {
        model.replayEffectStatus(timeLeft = timeLeft)
    }

    override fun replayEffectCompleted() {
        model.replayEffectCompleted()
    }

    override fun replayEffectError(message: String) {
        model.replayEffectError(message = message)
    }
}

private fun suspendCompletion(completion: (() -> Unit)?): (suspend () -> Unit)? {
    if (completion == null) {
        return null
    }
    val result: suspend () -> Unit = { completion() }
    return result
}

private fun unitCompletion(completion: (suspend () -> Unit)?): (() -> Unit)? {
    if (completion == null) {
        return null
    }
    val result: () -> Unit = { mainScope.launch { completion() } }
    return result
}

fun Model.saveReplay(
    start: Double? = null,
    delay: Int? = null,
    completion: ((ReplaySettings) -> Unit)? = null,
): Boolean {
    if (replay.isSaving.value) {
        return false
    }
    replay.isSaving.value = true
    val delaySeconds = delay ?: stream.value.replay.postTriggerDelay
    mainScope.launch {
        kotlinx.coroutines.delay(delaySeconds * 1000L)
        replayBuffer.createFile { file ->
            mainScope.launch {
                replay.isSaving.value = false
                val createdFile = file ?: return@launch
                val replaySettings = replaysStorage.createReplay()
                replaySettings.start = start ?: database.replay.start
                replaySettings.stop = database.replay.stop
                replaySettings.duration = createdFile.duration
                runCatching {
                    File(createdFile.url).copyTo(replaySettings.url(), overwrite = true)
                }
                replaysStorage.append(replay = replaySettings)
                completion?.invoke(replaySettings)
            }
        }
    }
    return true
}

fun Model.loadReplay(video: ReplaySettings, completion: (() -> Unit)? = null) {
    replaySettings = video
    replay.startFromEnd.value = video.startFromEnd()
    replay.selectedId.value = video.id
    replayFrameExtractor = ReplayFrameExtractor(
        video = ReplayBufferFile(url = video.url().path, duration = video.duration, remove = false),
        offset = video.thumbnailOffset(),
        delegate = ReplayDelegateAdapter(this),
        completion = suspendCompletion(completion),
    )
}

fun Model.deleteSelectedReplay() {
    val replayId = replay.selectedId.value ?: return
    replaysStorage.delete(id = replayId)
}

fun Model.replaySpeedChanged() {
    database.replay.speed = replay.speed.value ?: SettingsReplaySpeed.one
}

fun Model.instantReplay(start: Double? = null, delay: Int? = null) {
    if (replay.instantReplayCountdown.value != 0) {
        return
    }
    val delaySeconds = delay ?: stream.value.replay.postTriggerDelay
    val savingStarted = saveReplay(start = start, delay = delaySeconds) { video ->
        loadReplay(video = video) {
            replay.isPlaying.value = true
            if (!replayPlay()) {
                replay.isPlaying.value = false
            }
        }
    }
    if (savingStarted) {
        replay.instantReplayCountdown.value = delaySeconds + 1
        instantReplayCountdownTick()
    }
}

private fun Model.instantReplayCountdownTick() {
    if (replay.instantReplayCountdown.value == 0) {
        return
    }
    replay.instantReplayCountdown.value = replay.instantReplayCountdown.value - 1
    mainScope.launch {
        delay(1000L)
        instantReplayCountdownTick()
    }
}

fun Model.makeReplayIsNotEnabledToast() {
    makeToast(
        title = localized("Replay is not enabled"),
        subTitle = localized("Tap here to enable it."),
    ) {
        Unit
        streamReplayEnabledUpdated()
        makeToast(title = localized("Replay enabled"))
    }
}

fun Model.makeReplayShouldBeDisabledToastIfNeeded() {
    val replayEnabled: Boolean = stream.value.replay.enabled
    if (!replayEnabled) {
        return
    }
    val enterForegroundCountAtLatestUsage: Int =
        0
    val unusedCount = enterForegroundCount - enterForegroundCountAtLatestUsage
    if (unusedCount < 20 || unusedCount % 3 != 0) {
        return
    }
    makeToast(
        title = localized("Replay is enabled but seems unused"),
        subTitle = localized("Tap here to disable it."),
    ) {
        Unit
        streamReplayEnabledUpdated()
        makeToast(title = localized("Replay disabled"))
    }
}

fun Model.setReplayPosition(start: Double) {
    val replaySettings = replaySettings ?: return
    replaySettings.start = start
    replaySettings.stop = SettingsReplay.stop
    database.replay.start = start
    replayFrameExtractor?.seek(offset = replaySettings.thumbnailOffset())
}

fun Model.replayPlay(): Boolean {
    replayCancel()
    val replayVideo = replayVideo ?: return false
    val replaySettings = replaySettings ?: return false
    TODO()
    val transitionMode: ReplayEffectTransitionMode =
        TODO()
    replayEffect = ReplayEffect(
        video = replayVideo,
        start = replaySettings.startFromVideoStart(),
        stop = replaySettings.stopFromVideoStart(),
        speed = database.replay.speed.toNumber(),
        size = stream.value.dimensions(),
        layout = stream.value.replay.layout,
        transitionMode = transitionMode,
        delegate = ReplayEffectDelegateAdapter(this),
    )
    media.registerEffectBack(replayEffect!!)
    return true
}

fun Model.replayCancel() {
    replayEffect?.cancel()
    replayEffect = null
}

fun Model.streamReplayEnabledUpdated() {
    replayBuffer = ReplayBuffer()
    val replayEnabled: Boolean = stream.value.replay.enabled
    media.setReplayBuffering(enabled = replayEnabled)
    if (replayEnabled) {
        startRecorderIfNeeded()
    } else {
        stopRecorderIfNeeded()
    }
    Unit
}

internal fun Model.replayOutputFrame(
    image: Bitmap,
    offset: Double,
    video: ReplayBufferFile,
    completion: (() -> Unit)?,
) {
    mainScope.launch {
        replay.previewImage.value = image
        replayVideo = video
        completion?.invoke()
    }
}

fun Model.replayEffectStatus(timeLeft: Int) {
    mainScope.launch {
        replay.timeLeft.value = timeLeft
    }
}

fun Model.replayEffectCompleted() {
    mainScope.launch {
        replay.isPlaying.value = false
    }
}

fun Model.replayEffectError(message: String) {
    makeErrorToastMain(title = message)
}

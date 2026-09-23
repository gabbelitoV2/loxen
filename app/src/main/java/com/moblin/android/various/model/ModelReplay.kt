package com.moblin.android.various.model

import android.graphics.Bitmap
import com.moblin.android.localized
import com.moblin.android.various.ReplayBuffer
import com.moblin.android.various.ReplayBufferFile
import com.moblin.android.various.ReplayDelegate
import com.moblin.android.various.ReplayFrameExtractor
import com.moblin.android.various.settings.SettingsReplay
import com.moblin.android.various.settings.SettingsReplaySpeed
import com.moblin.android.various.settings.SettingsStreamReplay
import com.moblin.android.various.storages.ReplaySettings
import com.moblin.android.videoeffects.replay.ReplayEffect
import com.moblin.android.videoeffects.replay.ReplayEffectDelegate
import com.moblin.android.videoeffects.replay.ReplayEffectTransitionMode
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

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

fun Model.saveReplay(
    start: Double? = null,
    delay: Int? = null,
    completion: ((ReplaySettings) -> Unit)? = null,
): Boolean {
    if (replay.isSaving.value) {
        return false
    }
    replay.isSaving.value = true
    val effectiveDelay = delay ?: stream.value.replay.postTriggerDelay
    CoroutineScope(Dispatchers.Main.immediate).launch {
        kotlinx.coroutines.delay(effectiveDelay * 1000L)
        replayBuffer.createFile { file ->
            CoroutineScope(Dispatchers.Main.immediate).launch {
                replay.isSaving.value = false
                if (file == null) {
                    return@launch
                }
                val replaySettings = replaysStorage.createReplay()
                replaySettings.start = start ?: database.replay.start
                replaySettings.stop = database.replay.stop
                replaySettings.duration = file.duration
                runCatching { File(file.url).copyTo(replaySettings.url()) }
                replaysStorage.append(replay = replaySettings)
                completion?.invoke(replaySettings)
            }
        }
    }
    return true
}

fun Model.loadReplay(video: ReplaySettings, completion: (suspend () -> Unit)? = null) {
    replaySettings = video
    replay.startFromEnd.value = video.startFromEnd()
    replay.selectedId.value = video.id
    replayFrameExtractor = ReplayFrameExtractor(
        video = ReplayBufferFile(url = video.url().path, duration = video.duration, remove = false),
        offset = video.thumbnailOffset(),
        delegate = ModelReplayDelegate(this),
        completion = completion,
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
    val effectiveDelay = delay ?: stream.value.replay.postTriggerDelay
    val savingStarted = saveReplay(start = start, delay = effectiveDelay) { video ->
        loadReplay(video = video) {
            replay.isPlaying.value = true
            if (!replayPlay()) {
                replay.isPlaying.value = false
            }
        }
    }
    if (savingStarted) {
        replay.instantReplayCountdown.value = effectiveDelay + 1
        instantReplayCountdownTick()
    }
}

private fun Model.instantReplayCountdownTick() {
    if (replay.instantReplayCountdown.value == 0) {
        return
    }
    replay.instantReplayCountdown.value -= 1
    CoroutineScope(Dispatchers.Main.immediate).launch {
        kotlinx.coroutines.delay(1000)
        instantReplayCountdownTick()
    }
}

fun Model.makeReplayIsNotEnabledToast() {
    makeToast(
        title = localized("Replay is not enabled"),
        subTitle = localized("Tap here to enable it."),
    ) {
        stream.value.replay.enabled = true
        streamReplayEnabledUpdated()
        makeToast(title = localized("Replay enabled"))
    }
}

fun Model.makeReplayShouldBeDisabledToastIfNeeded() {
    if (!stream.value.replay.enabled) {
        return
    }
    val enterForegroundCountAtLatestUsage = stream.value.replay.enterForegroundCountAtLatestUsage ?: return
    val unusedCount = enterForegroundCount - enterForegroundCountAtLatestUsage
    if (unusedCount < 20 || unusedCount % 3 != 0) {
        return
    }
    makeToast(
        title = localized("Replay is enabled but seems unused"),
        subTitle = localized("Tap here to disable it."),
    ) {
        stream.value.replay.enabled = false
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
    val replay = stream.value.replay
    replay.enterForegroundCountAtLatestUsage = enterForegroundCount
    val transitionMode: ReplayEffectTransitionMode = when (replay.transitionType.rawValue) {
        "none" -> ReplayEffectTransitionMode.None
        "fade" -> ReplayEffectTransitionMode.Fade
        "stingers" -> {
            val inPath = replayTransitionsStorage.makePath(
                filename = replay.inStinger.makeFilename() ?: "",
            )
            val outPath = replayTransitionsStorage.makePath(
                filename = replay.outStinger.makeFilename() ?: "",
            )
            ReplayEffectTransitionMode.Stingers(
                inPath = inPath.path,
                inTransitionPoint = replay.inStinger.transitionPoint,
                outPath = outPath.path,
                outTransitionPoint = replay.outStinger.transitionPoint,
            )
        }
        else -> ReplayEffectTransitionMode.None
    }
    replayEffect = ReplayEffect(
        video = replayVideo,
        start = replaySettings.startFromVideoStart(),
        stop = replaySettings.stopFromVideoStart(),
        speed = database.replay.speed.toNumber(),
        size = stream.value.dimensions(),
        layout = replay.layout,
        transitionMode = transitionMode,
        delegate = ModelReplayEffectDelegate(this),
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
    media.setReplayBuffering(enabled = stream.value.replay.enabled)
    if (stream.value.replay.enabled) {
        startRecorderIfNeeded()
    } else {
        stopRecorderIfNeeded()
    }
    stream.value.replay.enterForegroundCountAtLatestUsage = enterForegroundCount
}

internal fun Model.replayOutputFrame(
    image: Bitmap,
    offset: Double,
    video: ReplayBufferFile,
    completion: (suspend () -> Unit)?,
) {
    CoroutineScope(Dispatchers.Main.immediate).launch {
        replay.previewImage.value = image
        replayVideo = video
        completion?.invoke()
    }
}

fun Model.replayEffectStatus(timeLeft: Int) {
    CoroutineScope(Dispatchers.Main.immediate).launch {
        replay.timeLeft.value = timeLeft
    }
}

fun Model.replayEffectCompleted() {
    CoroutineScope(Dispatchers.Main.immediate).launch {
        replay.isPlaying.value = false
    }
}

fun Model.replayEffectError(message: String) {
    makeErrorToastMain(title = message)
}

private class ModelReplayDelegate(private val model: Model) : ReplayDelegate {
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
            completion = completion,
        )
    }
}

private class ModelReplayEffectDelegate(private val model: Model) : ReplayEffectDelegate {
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

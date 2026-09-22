package com.moblin.android.various.model

import com.moblin.android.media.MediaSample
import com.moblin.android.various.MediaPlayer
import com.moblin.android.various.MediaPlayerDelegate
import com.moblin.android.various.mediaPlayerLatency
import com.moblin.android.various.settings.SettingsCameraId
import com.moblin.android.various.settings.SettingsMediaPlayer
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private val mainScope = CoroutineScope(Dispatchers.Main)

class MediaPlayerPlayer {
    val playing = MutableStateFlow(false)

    val position = MutableStateFlow(0f)

    val time = MutableStateFlow("0:00")

    val fileName = MutableStateFlow("Media name")

    val seeking = MutableStateFlow(false)
}

fun Model.initMediaPlayers() {
    for (settings in database.mediaPlayers.players) {
        addMediaPlayer(settings = settings)
    }
    removeUnusedMediaPlayerFiles()
}

private fun Model.removeUnusedMediaPlayerFiles() {
    for (mediaId in mediaStorage.ids()) {
        var found = false
        for (player in database.mediaPlayers.players) {
            if (player.playlist.any { it.id == mediaId }) {
                found = true
            }
        }
        if (!found) {
            mediaStorage.remove(id = mediaId)
        }
    }
}

fun Model.addMediaPlayer(settings: SettingsMediaPlayer) {
    val mediaPlayer = MediaPlayer(settings = settings, mediaStorage = mediaStorage)
    mediaPlayer.delegate = MediaPlayerDelegateImpl(model = this)
    mediaPlayers[settings.id] = mediaPlayer
    updateMicsListAsync()
}

fun Model.deleteMediaPlayer(playerId: UUID) {
    mediaPlayers.remove(playerId)
    updateMicsListAsync()
}

fun Model.updateMediaPlayerSettings(playerId: UUID, settings: SettingsMediaPlayer) {
    mediaPlayers[playerId]?.updateSettings(settings = settings)
}

fun Model.mediaPlayerTogglePlaying() {
    val mediaPlayer = getCurrentMediaPlayer() ?: return
    if (mediaPlayerPlayer.playing.value) {
        mediaPlayer.pause()
    } else {
        mediaPlayer.play()
    }
    mediaPlayerPlayer.playing.value = !mediaPlayerPlayer.playing.value
}

fun Model.mediaPlayerNext() {
    getCurrentMediaPlayer()?.next()
}

fun Model.mediaPlayerPrevious() {
    getCurrentMediaPlayer()?.previous()
}

fun Model.mediaPlayerSeek(position: Double) {
    getCurrentMediaPlayer()?.seek(position = position)
}

fun Model.mediaPlayerSetSeeking(on: Boolean) {
    getCurrentMediaPlayer()?.setSeeking(on = on)
}

fun Model.getCurrentMediaPlayer(): MediaPlayer? {
    val scene = getSelectedScene() ?: return null
    if (scene.videoSource.cameraPosition !is SettingsCameraId.MediaPlayer) {
        return null
    }
    val mediaPlayerSettings = getMediaPlayer(id = scene.videoSource.mediaPlayerCameraId) ?: return null
    return mediaPlayers[mediaPlayerSettings.id]
}

fun Model.deactivateAllMediaPlayers() {
    for (mediaPlayer in mediaPlayers.values) {
        mediaPlayer.deactivate()
    }
}

fun Model.playerCameras(): List<Camera> =
    database.mediaPlayers.players.map {
        Camera(id = it.id.toString(), name = it.camera())
    }

fun Model.getMediaPlayer(idString: String): SettingsMediaPlayer? =
    database.mediaPlayers.players.firstOrNull {
        idString == it.id.toString()
    }

fun Model.getMediaPlayer(id: UUID): SettingsMediaPlayer? =
    database.mediaPlayers.players.firstOrNull {
        it.id == id
    }

fun Model.mediaPlayerFileLoaded(playerId: UUID, name: String) {
    val latency = mediaPlayerLatency
    media.addBufferedVideo(cameraId = playerId, name = "Media player: $name", latency = latency)
    media.addBufferedAudio(cameraId = playerId, name = "Media player: $name", latency = latency)
}

fun Model.mediaPlayerFileUnloaded(playerId: UUID) {
    media.removeBufferedVideo(cameraId = playerId)
    media.removeBufferedAudio(cameraId = playerId)
}

fun Model.mediaPlayerStateUpdate(
    playerId: UUID,
    name: String,
    playing: Boolean,
    position: Double,
    time: String
) {
    mainScope.launch {
        mediaPlayerPlayer.playing.value = playing
        mediaPlayerPlayer.fileName.value = name
        if (!mediaPlayerPlayer.seeking.value) {
            mediaPlayerPlayer.position.value = position.toFloat()
        }
        mediaPlayerPlayer.time.value = time
    }
}

fun Model.mediaPlayerVideoBuffer(playerId: UUID, sampleBuffer: MediaSample) {
    media.appendBufferedVideoSampleBuffer(cameraId = playerId, sampleBuffer = sampleBuffer)
}

fun Model.mediaPlayerAudioBuffer(playerId: UUID, sampleBuffer: MediaSample) {
    media.appendBufferedAudioSampleBuffer(cameraId = playerId, sampleBuffer = sampleBuffer)
}

private class MediaPlayerDelegateImpl(private val model: Model) : MediaPlayerDelegate {
    override fun mediaPlayerFileLoaded(playerId: UUID, name: String) {
        model.mediaPlayerFileLoaded(playerId = playerId, name = name)
    }

    override fun mediaPlayerFileUnloaded(playerId: UUID) {
        model.mediaPlayerFileUnloaded(playerId = playerId)
    }

    override fun mediaPlayerStateUpdate(
        playerId: UUID,
        name: String,
        playing: Boolean,
        position: Double,
        time: String,
    ) {
        model.mediaPlayerStateUpdate(
            playerId = playerId,
            name = name,
            playing = playing,
            position = position,
            time = time,
        )
    }

    override fun mediaPlayerVideoBuffer(playerId: UUID, sampleBuffer: MediaSample) {
        model.mediaPlayerVideoBuffer(playerId = playerId, sampleBuffer = sampleBuffer)
    }

    override fun mediaPlayerAudioBuffer(playerId: UUID, sampleBuffer: MediaSample) {
        model.mediaPlayerAudioBuffer(playerId = playerId, sampleBuffer = sampleBuffer)
    }
}

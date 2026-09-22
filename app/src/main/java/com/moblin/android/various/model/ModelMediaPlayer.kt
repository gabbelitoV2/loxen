package com.moblin.android.various.model

import com.moblin.android.media.MediaSample
import com.moblin.android.various.MediaPlayer
import com.moblin.android.various.mediaPlayerLatency
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
    internal val _playing = MutableStateFlow(false)
    val playing: StateFlow<Boolean> = _playing.asStateFlow()

    internal val _position = MutableStateFlow(0f)
    val position: StateFlow<Float> = _position.asStateFlow()

    internal val _time = MutableStateFlow("0:00")
    val time: StateFlow<String> = _time.asStateFlow()

    internal val _fileName = MutableStateFlow("Media name")
    val fileName: StateFlow<String> = _fileName.asStateFlow()

    internal val _seeking = MutableStateFlow(false)
    val seeking: StateFlow<Boolean> = _seeking.asStateFlow()
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
    mediaPlayer.delegate = this
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
    mediaPlayerPlayer._playing.value = !mediaPlayerPlayer.playing.value
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
    if (scene.videoSource.cameraPosition != CameraPosition.mediaPlayer) {
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
        mediaPlayerPlayer._playing.value = playing
        mediaPlayerPlayer._fileName.value = name
        if (!mediaPlayerPlayer.seeking.value) {
            mediaPlayerPlayer._position.value = position.toFloat()
        }
        mediaPlayerPlayer._time.value = time
    }
}

fun Model.mediaPlayerVideoBuffer(playerId: UUID, sampleBuffer: MediaSample) {
    media.appendBufferedVideoSampleBuffer(cameraId = playerId, sampleBuffer = sampleBuffer)
}

fun Model.mediaPlayerAudioBuffer(playerId: UUID, sampleBuffer: MediaSample) {
    media.appendBufferedAudioSampleBuffer(cameraId = playerId, sampleBuffer = sampleBuffer)
}

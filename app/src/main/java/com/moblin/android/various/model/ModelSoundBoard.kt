package com.moblin.android.various.model

import com.moblin.android.platform.Bundle
import com.moblin.android.various.AudioPlayer
import java.util.UUID

fun Model.makeAlertSoundUrl(soundId: UUID): String? {
    val bundledSound = database.alertsMediaGallery.bundledSounds.firstOrNull { it.id == soundId }
    return if (bundledSound != null) {
        Bundle.url("Alerts.bundle/${bundledSound.name}", "mp3")
    } else {
        alertMediaStorage.makePath(id = soundId)?.path
    }
}

fun Model.playSoundBoardSound(soundId: UUID) {
    soundBoardPlayer?.stop()
    soundBoardPlayer = null
    val url = makeAlertSoundUrl(soundId) ?: return
    soundBoardPlayer = runCatching { AudioPlayer(contentsOf = url) }.getOrNull()
    soundBoardPlayer?.play()
}

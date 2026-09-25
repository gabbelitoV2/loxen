package com.moblin.android.various.model

import android.media.MediaPlayer
import com.moblin.android.various.settings.SettingsQuickButtonType

fun Model.isChatPhone(): Boolean {
    return database.appMode == com.moblin.android.various.settings.SettingsAppMode.chatPhone
}

fun Model.isQuickButtonAllowed(type: SettingsQuickButtonType): Boolean {
    if (!isChatPhone()) {
        return true
    }
    return when (type) {
        SettingsQuickButtonType.torch -> false
        SettingsQuickButtonType.gimbalTracking -> false
        SettingsQuickButtonType.workout -> false
        SettingsQuickButtonType.portrait -> false
        SettingsQuickButtonType.mute -> false
        SettingsQuickButtonType.mic -> false
        SettingsQuickButtonType.blackScreen -> false
        SettingsQuickButtonType.bitrate -> false
        SettingsQuickButtonType.record -> false
        SettingsQuickButtonType.image -> false
        SettingsQuickButtonType.movie -> false
        SettingsQuickButtonType.grayScale -> false
        SettingsQuickButtonType.sepia -> false
        SettingsQuickButtonType.triple -> false
        SettingsQuickButtonType.twin -> false
        SettingsQuickButtonType.pixellate -> false
        SettingsQuickButtonType.stream -> false
        SettingsQuickButtonType.grid -> false
        SettingsQuickButtonType.cameraLevel -> false
        SettingsQuickButtonType.localOverlays -> false
        SettingsQuickButtonType.draw -> false
        SettingsQuickButtonType.cameraPreview -> false
        SettingsQuickButtonType.fourThree -> false
        SettingsQuickButtonType.crt -> false
        SettingsQuickButtonType.poll -> false
        SettingsQuickButtonType.snapshot -> false
        SettingsQuickButtonType.widgets -> false
        SettingsQuickButtonType.luts -> false
        SettingsQuickButtonType.replay -> false
        SettingsQuickButtonType.connectionPriorities -> false
        SettingsQuickButtonType.instantReplay -> false
        SettingsQuickButtonType.pinch -> false
        SettingsQuickButtonType.whirlpool -> false
        SettingsQuickButtonType.autoSceneSwitcher -> false
        SettingsQuickButtonType.blurFaces -> false
        SettingsQuickButtonType.blurText -> false
        SettingsQuickButtonType.privacy -> false
        SettingsQuickButtonType.moblinInMouth -> false
        SettingsQuickButtonType.glasses -> false
        SettingsQuickButtonType.sparkle -> false
        SettingsQuickButtonType.beauty -> false
        SettingsQuickButtonType.cameraMan -> false
        SettingsQuickButtonType.videoPreview -> false
        SettingsQuickButtonType.previewStream -> false
        SettingsQuickButtonType.photoShoot -> false
        else -> true
    }
}

fun Model.appModeChanged() {
    show.chatPhone.value = isChatPhone()
    mic.current.value = noMic
    updateQuickButtonPairs()
    updateScreenAutoOff()
    reloadAudioSession()
    reloadStream()
    resetSelectedScene(changeScene = false)
    updateOrientation()
    updateOrientationLock()
    setupAudio()
}

fun Model.startChatPhoneBackgroundAudio() {
    if (!isChatPhone()) {
        return
    }
    Unit
}

fun Model.stopChatPhoneBackgroundAudio() {
    com.moblin.android.platform.host.StreamingService.stopBackground()
    chatPhoneBackgroundAudioPlayer?.stop()
    chatPhoneBackgroundAudioPlayer = null
}

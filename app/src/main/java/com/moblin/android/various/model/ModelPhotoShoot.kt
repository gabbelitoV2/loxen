package com.moblin.android.various.model

import com.moblin.android.platform.core.ContinuousClock
import com.moblin.android.various.settings.SettingsQuickButtonType

fun Model.togglePhotoShoot() {
    photoShootEnabled.value = !photoShootEnabled.value
    setQuickButton(type = SettingsQuickButtonType.photoShoot, isOn = photoShootEnabled.value)
    stop()
    if (database.alwaysAttachPhotoShoot) {
        activatePhotoShoot()
    } else {
        attachCamera()
    }
}

fun Model.setPhotoShootInterval(interval: Int) {
    database.photoShootInterval = interval
    if (photoShoot.active) {
        start()
    }
}

fun Model.activatePhotoShoot() {
    if (isChatPhone() || !photoShootEnabled.value) {
        return
    }
    photoShoot.active = true
    start()
}

fun Model.handlePhotoTaken() {
    photoShoot.photoTaken.value = true
    photoShoot.flashTimer.startSingleShot(timeout = 0.15) {
        photoShoot.photoTaken.value = false
    }
    if (photoShoot.active) {
        start()
    }
}

private fun Model.start() {
    val interval = database.photoShootInterval.toDouble()
    photoShoot.nextPhotoTime.value = ContinuousClock.now.advanced(bySeconds = interval)
    photoShoot.timer.startSingleShot(timeout = interval) {
        media.takePhoto(flash = database.photoShootFlash)
    }
}

private fun Model.stop() {
    photoShoot.timer.stop()
    photoShoot.flashTimer.stop()
    photoShoot.photoTaken.value = false
    photoShoot.nextPhotoTime.value = null
    photoShoot.active = false
}

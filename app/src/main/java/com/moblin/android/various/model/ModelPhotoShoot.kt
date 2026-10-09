package com.moblin.android.various.model

fun Model.startPhotoShoot() {
    if (isChatPhone()) {
        return
    }
    photoShootTimer.startPeriodic(interval = database.photoShootInterval.toDouble()) {
        media.takePhoto(flash = database.photoShootFlash)
    }
}

fun Model.stopPhotoShoot() {
    photoShootTimer.stop()
    photoShootFlashTimer.stop()
    photoShoot.photoTaken.value = false
}

fun Model.setPhotoShootInterval(interval: Int) {
    database.photoShootInterval = interval
    if (photoShootEnabled.value) {
        startPhotoShoot()
    }
}

fun Model.handlePhotoTaken() {
    photoShoot.photoTaken.value = true
    photoShootFlashTimer.startSingleShot(timeout = 0.15) {
        photoShoot.photoTaken.value = false
    }
}

fun Model.togglePhotoShoot() {
    if (!database.alwaysAttachPhotoShoot) {
        attachCamera()
    }
}

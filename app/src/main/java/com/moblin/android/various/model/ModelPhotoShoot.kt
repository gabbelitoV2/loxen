package com.moblin.android.various.model

fun Model.startPhotoShoot() {
    if (isChatPhone()) {
        return
    }
    photoShootTimer.startPeriodic(interval = 1.0) {
        media.takePhoto()
    }
}

fun Model.stopPhotoShoot() {
    photoShootTimer.stop()
}

fun Model.togglePhotoShoot() {
    if (!database.alwaysAttachPhotoShoot) {
        attachCamera()
    }
}

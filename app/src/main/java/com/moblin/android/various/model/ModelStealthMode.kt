package com.moblin.android.various.model

import android.graphics.BitmapFactory
import java.io.File

val stealthModeImagePath: File
    get() = TODO("no Android counterpart for URL.documentsDirectory: needs File(context.filesDir, \"stealthModeImage.img\")")

fun Model.setStealthMode(on: Boolean) {
    showStealthMode.value = on
    remoteControlStateChanged(state = TODO("RemoteControlState is not available"))
}

fun Model.toggleStealthMode() {
    setStealthMode(on = !showStealthMode.value)
}

fun Model.saveStealthModeImage(data: ByteArray) {
    runCatching { stealthModeImagePath.writeBytes(data) }
}

fun Model.loadStealthModeImage() {
    val data = runCatching { stealthModeImagePath.readBytes() }.getOrNull()
    if (data == null) {
        stealthMode.image.value = null
        return
    }
    stealthMode.image.value = BitmapFactory.decodeByteArray(data, 0, data.size)
}

fun Model.deleteStealthModeImage() {
    stealthModeImagePath.delete()
}

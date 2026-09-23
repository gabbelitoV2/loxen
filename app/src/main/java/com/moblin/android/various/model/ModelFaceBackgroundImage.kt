package com.moblin.android.various.model

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import com.moblin.android.AppDelegate

val faceBackgroundImagePath: File
    get() = File(AppDelegate.context.filesDir, "faceBackgroundImage.img")

fun Model.saveFaceBackgroundImage(data: ByteArray) {
    runCatching {
        faceBackgroundImagePath.writeBytes(data)
    }
}

fun Model.loadFaceBackgroundImage() {
    faceBackgroundImage = readFaceBackgroundImage()
    updateFaceFilterSettings()
}

private fun Model.readFaceBackgroundImage(): com.moblin.android.platform.coreimage.CIImage? {
    val data = runCatching { faceBackgroundImagePath.readBytes() }.getOrNull() ?: return null
    return BitmapFactory.decodeByteArray(data, 0, data.size)?.let { com.moblin.android.platform.coreimage.CIImage(cgImage = it) }
}

package com.moblin.android.various.model

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

val faceBackgroundImagePath: File
    get() = TODO("URL.documentsDirectory has no Android equivalent; resolve against context.filesDir")

fun Model.saveFaceBackgroundImage(data: ByteArray) {
    runCatching {
        faceBackgroundImagePath.writeBytes(data)
    }
}

fun Model.loadFaceBackgroundImage() {
    faceBackgroundImage = readFaceBackgroundImage()
    updateFaceFilterSettings()
}

private fun Model.readFaceBackgroundImage(): Bitmap? {
    val data = runCatching { faceBackgroundImagePath.readBytes() }.getOrNull() ?: return null
    return BitmapFactory.decodeByteArray(data, 0, data.size)
}

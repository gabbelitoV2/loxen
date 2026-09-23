package com.moblin.android.various.model

import com.moblin.android.AppDelegate
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.uikit.UIImage
import com.moblin.android.platform.uikit.cgImage
import java.io.File

val faceBackgroundImagePath: File = File(AppDelegate.context.filesDir, "faceBackgroundImage.img")

fun Model.saveFaceBackgroundImage(data: ByteArray) {
    runCatching { faceBackgroundImagePath.writeBytes(data) }
}

fun Model.loadFaceBackgroundImage() {
    faceBackgroundImage = readFaceBackgroundImage()
    updateFaceFilterSettings()
}

private fun Model.readFaceBackgroundImage(): CIImage? {
    val data = runCatching { faceBackgroundImagePath.readBytes() }.getOrNull() ?: return null
    val cgImage = UIImage(data = data)?.cgImage ?: return null
    return CIImage(cgImage = cgImage)
}

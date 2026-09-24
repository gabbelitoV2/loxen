package com.moblin.android.various.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import java.io.File
import java.io.FileOutputStream

val appContext: Context get() = com.moblin.android.AppDelegate.context

val controlBarBackgroundImagePath: File
    get() = File(com.moblin.android.platform.Documents.directory, "controlBarBackgroundImage.img")

fun Model.saveControlBarBackgroundImage(data: ByteArray): Bitmap? {
    val original = BitmapFactory.decodeByteArray(data, 0, data.size) ?: return null
    val image = downscaleControlBarBackgroundImage(original) ?: original
    writeControlBarBackgroundImage(image)
    val quickButtons = database.quickButtonsGeneral
    quickButtons.backgroundImageCropX = 0.0
    quickButtons.backgroundImageCropY = 0.0
    quickButtons.backgroundImageCropWidth = 1.0
    quickButtons.backgroundImageCropHeight = 1.0
    updateControlBarBackgroundImage(image)
    return image
}

fun Model.readControlBarBackgroundImage(): Bitmap? {
    val data = runCatching { controlBarBackgroundImagePath.readBytes() }.getOrNull() ?: return null
    return BitmapFactory.decodeByteArray(data, 0, data.size)
}

private fun Model.writeControlBarBackgroundImage(image: Bitmap) {
    runCatching {
        FileOutputStream(controlBarBackgroundImagePath).use { output ->
            image.compress(Bitmap.CompressFormat.JPEG, 90, output)
        }
    }
}

private fun Model.downscaleControlBarBackgroundImage(image: Bitmap): Bitmap? {
    val scale = 2048f / maxOf(image.width, image.height)
    if (scale >= 1f) {
        return null
    }
    val width = (image.width * scale).toInt()
    val height = (image.height * scale).toInt()
    return Bitmap.createScaledBitmap(image, width, height, true)
}

fun Model.loadControlBarBackgroundImage() {
    updateControlBarBackgroundImage(readControlBarBackgroundImage())
    updateControlBarBackgroundImageOpacity()
}

fun Model.updateControlBarBackgroundImageOpacity() {
    controlBar.backgroundImageOpacity.value = database.quickButtonsGeneral.backgroundImageOpacity.value
}

fun Model.updateControlBarBackgroundImage(image: Bitmap?) {
    if (image == null) {
        controlBar.backgroundImage.value = null
        return
    }
    val quickButtons = database.quickButtonsGeneral
    val x = image.width * quickButtons.backgroundImageCropX
    val y = image.height * quickButtons.backgroundImageCropY
    val width = image.width * quickButtons.backgroundImageCropWidth
    val height = image.height * quickButtons.backgroundImageCropHeight
    if (!(width > 0f && height > 0f)) {
        controlBar.backgroundImage.value = image
        return
    }
    val scale = minOf(1.0, 1024.0 / maxOf(width, height))
    val outputWidth = (width * scale).toInt()
    val outputHeight = (height * scale).toInt()
    if (outputWidth <= 0 || outputHeight <= 0) {
        controlBar.backgroundImage.value = null
        return
    }
    val output = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val matrix = Matrix()
    matrix.postScale(scale.toFloat(), scale.toFloat())
    matrix.postTranslate((-x * scale).toFloat(), (-y * scale).toFloat())
    canvas.drawBitmap(image, matrix, Paint(Paint.FILTER_BITMAP_FLAG))
    controlBar.backgroundImage.value = output
}

fun Model.deleteControlBarBackgroundImage() {
    runCatching { controlBarBackgroundImagePath.delete() }
    controlBar.backgroundImage.value = null
}

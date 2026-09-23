package com.moblin.android.platform.uikit

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorSpace
import android.os.Build
import com.moblin.android.platform.Bundle
import com.moblin.android.platform.coregraphics.CGImageSourceFormat
import com.moblin.android.platform.coregraphics.CGImageSourceFormats
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.offscreen.logOverlayOnce
import java.io.File

private val pngSignature = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)

private fun pngSourceFormat(data: ByteArray): CGImageSourceFormat? {
    if (data.size < 26) {
        return null
    }
    for (index in pngSignature.indices) {
        if (data[index] != pngSignature[index]) {
            return null
        }
    }
    if (data[12] != 'I'.code.toByte() || data[13] != 'H'.code.toByte() ||
        data[14] != 'D'.code.toByte() || data[15] != 'R'.code.toByte()
    ) {
        return null
    }
    val bitDepth = data[24].toInt() and 0xFF
    val components = when (data[25].toInt() and 0xFF) {
        0 -> 1
        2 -> 3
        3 -> 1
        4 -> 2
        6 -> 4
        else -> return null
    }
    if (bitDepth !in setOf(1, 2, 4, 8, 16)) {
        return null
    }
    return CGImageSourceFormat(bitsPerComponent = bitDepth, componentsPerPixel = components)
}

private fun decodeOptions(format: CGImageSourceFormat?, sixteenBit: Boolean): BitmapFactory.Options =
    BitmapFactory.Options().apply {
        inScaled = false
        inMutable = false
        inPremultiplied = true
        inPreferredConfig = if (sixteenBit) Bitmap.Config.RGBA_F16 else Bitmap.Config.ARGB_8888
        if (sixteenBit || format == null) {
            inPreferredColorSpace = ColorSpace.get(ColorSpace.Named.SRGB)
        }
    }

private fun isLinear(bitmap: Bitmap): Boolean {
    val colorSpace = bitmap.colorSpace ?: return false
    return colorSpace == ColorSpace.get(ColorSpace.Named.LINEAR_EXTENDED_SRGB) ||
        colorSpace == ColorSpace.get(ColorSpace.Named.LINEAR_SRGB)
}

private fun decodeImage(data: ByteArray): Bitmap? {
    if (data.isEmpty()) {
        return null
    }
    val format = pngSourceFormat(data)
    val sixteenBit = format?.bitsPerComponent == 16 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    var bitmap: Bitmap? = null
    if (sixteenBit) {
        bitmap = runCatching { BitmapFactory.decodeByteArray(data, 0, data.size, decodeOptions(format, true)) }
            .getOrNull()
            ?.takeIf { it.config == Bitmap.Config.RGBA_F16 && !isLinear(it) }
    }
    if (bitmap == null) {
        bitmap = BitmapFactory.decodeByteArray(data, 0, data.size, decodeOptions(format, false)) ?: return null
    }
    if (bitmap.config != Bitmap.Config.ARGB_8888 && bitmap.config != Bitmap.Config.RGBA_F16) {
        bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return null
    }
    if (format != null) {
        CGImageSourceFormats.set(bitmap, format)
    }
    return bitmap
}

fun UIImage(data: ByteArray): Bitmap? = try {
    decodeImage(data)
} catch (error: Throwable) {
    logOverlayOnce("UIImage(data:) failed: $error")
    null
}

fun UIImage(contentsOfFile: String): Bitmap? = try {
    val file = File(contentsOfFile)
    val data = if (file.isFile) {
        file.readBytes()
    } else if (!file.isAbsolute) {
        Bundle.readBytes(contentsOfFile)
    } else {
        null
    }
    data?.let { decodeImage(it) }
} catch (error: Throwable) {
    logOverlayOnce("UIImage(contentsOfFile:) failed: $error")
    null
}

val Bitmap.cgImage: Bitmap
    get() = this

val Bitmap.size: CGSize
    get() = CGSize(width = width.toDouble(), height = height.toDouble())

val Bitmap.scale: Double
    get() = 1.0

package com.moblin.android.platform.coreimage

import android.graphics.Bitmap
import com.google.zxing.WriterException
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder
import com.moblin.android.platform.coreimage.internal.EffectsLog

internal const val qrCodeQuietZone = 1

internal class QrCodeModules(val size: Int, private val dark: BooleanArray) {
    fun isDark(x: Int, y: Int): Boolean = dark[y * size + x]
}

internal fun qrCodeErrorCorrectionLevel(correctionLevel: String): ErrorCorrectionLevel? {
    return when (correctionLevel) {
        "L" -> ErrorCorrectionLevel.L
        "M" -> ErrorCorrectionLevel.M
        "Q" -> ErrorCorrectionLevel.Q
        "H" -> ErrorCorrectionLevel.H
        else -> null
    }
}

internal fun qrCodeModules(message: ByteArray, correctionLevel: String): QrCodeModules? {
    val level = qrCodeErrorCorrectionLevel(correctionLevel)
    if (level == null) {
        EffectsLog.once(
            "CIQRCodeGenerator.level:$correctionLevel",
            "CIQRCodeGenerator: invalid correction level $correctionLevel"
        )
        return null
    }
    val code = try {
        Encoder.encode(String(message, Charsets.ISO_8859_1), level)
    } catch (error: WriterException) {
        EffectsLog.once("CIQRCodeGenerator.encode", "CIQRCodeGenerator: cannot encode ${message.size} bytes: $error")
        return null
    }
    val matrix = code.matrix
    val size = matrix.width + 2 * qrCodeQuietZone
    val dark = BooleanArray(size * size)
    for (y in 0 until matrix.height) {
        for (x in 0 until matrix.width) {
            if (matrix.get(x, y).toInt() == 1) {
                dark[(y + qrCodeQuietZone) * size + x + qrCodeQuietZone] = true
            }
        }
    }
    return QrCodeModules(size, dark)
}

class CIQRCodeGenerator : CIFilter() {
    var message: ByteArray = ByteArray(0)
    var correctionLevel: String = "M"

    override val name: String
        get() = "CIQRCodeGenerator"

    override val outputImage: CIImage?
        get() {
            val modules = qrCodeModules(message, correctionLevel) ?: return null
            val size = modules.size
            val pixels = IntArray(size * size)
            for (y in 0 until size) {
                for (x in 0 until size) {
                    pixels[y * size + x] = if (modules.isDark(x, y)) BLACK else WHITE
                }
            }
            val bitmap = Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
            return CIImage(cgImage = bitmap).samplingNearest()
        }

    override fun setValue(value: Any?, forKey: String) {
        when (forKey) {
            "inputMessage" -> message = value as? ByteArray ?: message
            "inputCorrectionLevel" -> correctionLevel = value as? String ?: correctionLevel
            else -> super.setValue(value, forKey)
        }
    }

    override fun value(forKey: String): Any? {
        return when (forKey) {
            "inputMessage" -> message
            "inputCorrectionLevel" -> correctionLevel
            else -> super.value(forKey)
        }
    }

    override fun setDefaults() {
        message = ByteArray(0)
        correctionLevel = "M"
    }

    private companion object {
        const val BLACK = 0xFF000000.toInt()
        const val WHITE = 0xFFFFFFFF.toInt()
    }
}

package com.moblin.android.platform.coreimage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorSpace
import android.graphics.ImageDecoder
import android.media.ExifInterface
import android.os.Build
import androidx.annotation.RequiresApi
import com.moblin.android.platform.coregraphics.CGAffineTransform
import com.moblin.android.platform.coregraphics.CGImagePropertyOrientation
import com.moblin.android.platform.coreimage.internal.EffectsLog
import com.moblin.android.platform.coreimage.internal.Gl
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.roundToInt

internal object CIImageDecoder {
    fun decode(data: ByteArray, options: Map<CIImageOption, Any>?): CIImage? {
        if (data.isEmpty()) {
            return null
        }
        val applyOrientation = options?.get(CIImageOption.applyOrientationProperty) == true
        return try {
            if (applyOrientation && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                decodeWithImageDecoder(data)
            } else {
                val image = decodeWithBitmapFactory(data) ?: return null
                if (applyOrientation) {
                    orientedByExif(image, data)
                } else {
                    image
                }
            }
        } catch (error: Throwable) {
            EffectsLog.once("CIImageDecoder:${error.javaClass.name}", "CIImage decode failed: $error")
            null
        }
    }

    fun decode(contentsOf: String, options: Map<CIImageOption, Any>?): CIImage? {
        val data = try {
            File(contentsOf).readBytes()
        } catch (error: Exception) {
            EffectsLog.once("CIImageDecoder.read:${error.javaClass.name}", "CIImage read failed: $error")
            return null
        }
        return decode(data, options)
    }

    private fun decodeWithBitmapFactory(data: ByteArray): CIImage? {
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(data, 0, data.size, bounds)
        val width = bounds.outWidth
        val height = bounds.outHeight
        if (width <= 0 || height <= 0) {
            return null
        }
        val options = BitmapFactory.Options().apply {
            inScaled = false
            inPremultiplied = true
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inPreferredColorSpace = ColorSpace.get(ColorSpace.Named.SRGB)
            inSampleSize = sampleSize(width, height)
        }
        val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size, options) ?: return null
        return logicalImage(plainBitmap(bitmap) ?: return null, width, height)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun decodeWithImageDecoder(data: ByteArray): CIImage? {
        var width = 0
        var height = 0
        val source = ImageDecoder.createSource(ByteBuffer.wrap(data))
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.isMutableRequired = false
            decoder.setTargetColorSpace(ColorSpace.get(ColorSpace.Named.SRGB))
            width = info.size.width
            height = info.size.height
            val scale = textureScale(width, height)
            if (scale < 1.0) {
                decoder.setTargetSize(max(1, (width * scale).roundToInt()), max(1, (height * scale).roundToInt()))
            }
        }
        return logicalImage(plainBitmap(bitmap) ?: return null, width, height)
    }

    private fun orientedByExif(image: CIImage, data: ByteArray): CIImage {
        val exif = ExifInterface(ByteArrayInputStream(data))
        val value = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val orientation = CGImagePropertyOrientation(value) ?: return image
        if (orientation == CGImagePropertyOrientation.up) {
            return image
        }
        return image.oriented(orientation)
    }

    private fun plainBitmap(bitmap: Bitmap): Bitmap? {
        if (bitmap.config == Bitmap.Config.ARGB_8888 && bitmap.isPremultiplied) {
            return bitmap
        }
        val copy = bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return null
        bitmap.recycle()
        return copy
    }

    private fun logicalImage(bitmap: Bitmap, width: Int, height: Int): CIImage {
        val image = CIImage(cgImage = bitmap)
        if (bitmap.width == width && bitmap.height == height) {
            return image
        }
        val sameOrientation = (bitmap.width >= bitmap.height) == (width >= height)
        val logicalWidth = if (sameOrientation) width else height
        val logicalHeight = if (sameOrientation) height else width
        if (bitmap.width == logicalWidth && bitmap.height == logicalHeight) {
            return image
        }
        return image.transformed(
            by = CGAffineTransform(
                scaleX = logicalWidth.toDouble() / bitmap.width,
                y = logicalHeight.toDouble() / bitmap.height
            )
        )
    }

    private fun textureScale(width: Int, height: Int): Double {
        val maxSize = Gl.maxTextureSize
        val largest = max(width, height)
        if (largest <= maxSize) {
            return 1.0
        }
        return maxSize.toDouble() / largest
    }

    private fun sampleSize(width: Int, height: Int): Int {
        val maxSize = Gl.maxTextureSize
        var sampleSize = 1
        while (max(width, height) / sampleSize > maxSize) {
            sampleSize *= 2
        }
        return sampleSize
    }
}

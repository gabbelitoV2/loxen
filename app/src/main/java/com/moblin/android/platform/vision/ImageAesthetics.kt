package com.moblin.android.platform.vision

import android.graphics.Bitmap
import com.moblin.android.media.MediaSample
import com.moblin.android.platform.video.CVPixelBuffer
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val analysisLongSide = 512
private const val sharpnessHalfVariance = 150.0
private const val middleGray = 118.0
private const val darkClipLevel = 8
private const val brightClipLevel = 247

class ImageAestheticsScoresObservation internal constructor(
    val overallScore: Float,
    val isUtility: Boolean,
)

class CalculateImageAestheticsScoresRequest {
    suspend fun perform(on: MediaSample): ImageAestheticsScoresObservation {
        val imageBuffer = on.imageBuffer ?: throw VNError("The sample buffer has no image buffer")
        return perform(on = imageBuffer)
    }

    suspend fun perform(on: CVPixelBuffer): ImageAestheticsScoresObservation = withContext(Dispatchers.Default) {
        if (!on.checkReadable("CalculateImageAestheticsScoresRequest")) {
            throw VNError("The pixel buffer is no longer valid")
        }
        val bitmap = on.toBitmap(maxLongSide = analysisLongSide) ?: throw VNError("Pixel buffer readback failed")
        try {
            observation(bitmap)
        } finally {
            bitmap.recycle()
        }
    }

    suspend fun perform(on: Bitmap): ImageAestheticsScoresObservation = withContext(Dispatchers.Default) {
        val longSide = max(on.width, on.height)
        if (longSide <= analysisLongSide) {
            observation(on)
        } else {
            val scale = analysisLongSide.toDouble() / longSide
            val width = max(1, (on.width * scale).roundToInt())
            val height = max(1, (on.height * scale).roundToInt())
            val scaled = Bitmap.createScaledBitmap(on, width, height, true)
            try {
                observation(scaled)
            } finally {
                if (scaled !== on) {
                    scaled.recycle()
                }
            }
        }
    }

    private fun observation(bitmap: Bitmap): ImageAestheticsScoresObservation {
        val width = bitmap.width
        val height = bitmap.height
        if (width < 3 || height < 3) {
            throw VNError("The image is too small")
        }
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val luma = IntArray(pixels.size) { index ->
            val pixel = pixels[index]
            val red = (pixel shr 16) and 0xFF
            val green = (pixel shr 8) and 0xFF
            val blue = pixel and 0xFF
            (77 * red + 150 * green + 29 * blue) shr 8
        }
        return ImageAestheticsScoresObservation(
            overallScore = imageAestheticsOverallScore(luma, width, height),
            isUtility = false,
        )
    }
}

internal fun imageSharpnessScore(luma: IntArray, width: Int, height: Int): Double {
    var sum = 0.0
    var sumOfSquares = 0.0
    var count = 0
    for (y in 1 until height - 1) {
        val row = y * width
        for (x in 1 until width - 1) {
            val index = row + x
            val laplacian = 4 * luma[index] - luma[index - 1] - luma[index + 1] - luma[index - width] -
                luma[index + width]
            sum += laplacian
            sumOfSquares += laplacian.toDouble() * laplacian
            count += 1
        }
    }
    if (count == 0) {
        return 0.0
    }
    val mean = sum / count
    val variance = max(0.0, sumOfSquares / count - mean * mean)
    return variance / (variance + sharpnessHalfVariance)
}

internal fun imageExposureScore(luma: IntArray): Double {
    if (luma.isEmpty()) {
        return 0.0
    }
    var sum = 0L
    var clipped = 0
    for (value in luma) {
        sum += value
        if (value <= darkClipLevel || value >= brightClipLevel) {
            clipped += 1
        }
    }
    val mean = sum.toDouble() / luma.size
    val brightness = (1.0 - abs(mean - middleGray) / middleGray).coerceIn(0.0, 1.0)
    val clipping = (1.0 - 2.5 * clipped / luma.size).coerceIn(0.0, 1.0)
    return 0.5 * brightness + 0.5 * clipping
}

internal fun imageAestheticsOverallScore(luma: IntArray, width: Int, height: Int): Float {
    val quality = 0.6 * imageSharpnessScore(luma, width, height) + 0.4 * imageExposureScore(luma)
    return (2.0 * quality - 1.0).coerceIn(-1.0, 1.0).toFloat()
}

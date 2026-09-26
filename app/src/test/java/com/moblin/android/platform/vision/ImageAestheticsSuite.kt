package com.moblin.android.platform.vision

import android.graphics.Bitmap
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ImageAestheticsSuite {
    private val width = 320
    private val height = 180

    private fun checkerboard(dark: Int, bright: Int): IntArray {
        return IntArray(width * height) { index ->
            val x = index % width
            val y = index / width
            if ((x / 8 + y / 8) % 2 == 0) dark else bright
        }
    }

    private fun boxBlur(luma: IntArray, radius: Int): IntArray {
        return IntArray(luma.size) { index ->
            val x = index % width
            val y = index / width
            var sum = 0
            var count = 0
            for (dy in -radius..radius) {
                for (dx in -radius..radius) {
                    val sx = x + dx
                    val sy = y + dy
                    if (sx in 0 until width && sy in 0 until height) {
                        sum += luma[sy * width + sx]
                        count += 1
                    }
                }
            }
            sum / count
        }
    }

    private fun bitmap(luma: IntArray, scale: Int = 1): Bitmap {
        val bitmap = Bitmap.createBitmap(width * scale, height * scale, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(bitmap.width * bitmap.height) { index ->
            val x = index % bitmap.width / scale
            val y = index / bitmap.width / scale
            val value = luma[y * width + x]
            (0xFF shl 24) or (value shl 16) or (value shl 8) or value
        }
        bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return bitmap
    }

    private fun score(bitmap: Bitmap): Float {
        return runBlocking {
            CalculateImageAestheticsScoresRequest().perform(on = bitmap).overallScore
        }
    }

    @Test
    fun sharpFrameBeatsBlurredFrameByMoreThanTheSnapshotMargin() {
        val sharp = checkerboard(dark = 60, bright = 180)
        val sharpScore = score(bitmap(sharp))
        val blurredScore = score(bitmap(boxBlur(sharp, radius = 4)))
        assertTrue(sharpScore > blurredScore + 0.2f, "sharp $sharpScore, blurred $blurredScore")
    }

    @Test
    fun wellExposedFrameBeatsOverAndUnderExposedFrames() {
        val wellExposed = score(bitmap(checkerboard(dark = 60, bright = 180)))
        val overExposed = score(bitmap(checkerboard(dark = 200, bright = 255)))
        val underExposed = score(bitmap(checkerboard(dark = 0, bright = 50)))
        assertTrue(wellExposed > overExposed + 0.2f, "well $wellExposed, over $overExposed")
        assertTrue(wellExposed > underExposed + 0.2f, "well $wellExposed, under $underExposed")
    }

    @Test
    fun scoresStayInVisionRange() {
        val white = IntArray(width * height) { 255 }
        val black = IntArray(width * height) { 0 }
        val noise = java.util.Random(1).let { random -> IntArray(width * height) { random.nextInt(256) } }
        for (luma in listOf(white, black, noise, checkerboard(dark = 60, bright = 180))) {
            val value = score(bitmap(luma))
            assertTrue(value in -1f..1f, "score $value")
        }
        assertEquals(-1f, score(bitmap(white)))
    }

    @Test
    fun largeFramesAreDownscaledBeforeScoring() {
        val luma = checkerboard(dark = 60, bright = 180)
        val small = score(bitmap(luma))
        val large = score(bitmap(luma, scale = 4))
        assertTrue(large > 0.5f, "large $large")
        assertTrue(small > 0.5f, "small $small")
    }

    @Test
    fun observationIsNotUtility() {
        val observation = runBlocking {
            CalculateImageAestheticsScoresRequest().perform(on = bitmap(checkerboard(dark = 60, bright = 180)))
        }
        assertFalse(observation.isUtility)
    }
}

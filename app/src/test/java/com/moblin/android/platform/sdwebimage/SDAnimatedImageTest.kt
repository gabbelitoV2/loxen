package com.moblin.android.platform.sdwebimage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import java.io.ByteArrayOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

private class GifLayout(val width: Int, val height: Int, val delays: List<Int>)

private fun parseGifLayout(data: ByteArray): GifLayout {
    fun u16(offset: Int): Int = (data[offset].toInt() and 0xFF) or ((data[offset + 1].toInt() and 0xFF) shl 8)
    fun skipSubBlocks(start: Int): Int {
        var offset = start
        while (true) {
            val size = data[offset].toInt() and 0xFF
            offset += 1
            if (size == 0) {
                return offset
            }
            offset += size
        }
    }
    val width = u16(6)
    val height = u16(8)
    val packed = data[10].toInt() and 0xFF
    var offset = 13
    if (packed and 0x80 != 0) {
        offset += 3 * (1 shl ((packed and 7) + 1))
    }
    val delays = mutableListOf<Int>()
    var pendingDelay = 0
    while (offset < data.size) {
        when (data[offset].toInt() and 0xFF) {
            0x21 -> {
                val label = data[offset + 1].toInt() and 0xFF
                if (label == 0xF9) {
                    pendingDelay = u16(offset + 4)
                    offset = skipSubBlocks(offset + 2)
                } else {
                    offset = skipSubBlocks(offset + 2)
                }
            }
            0x2C -> {
                val localPacked = data[offset + 9].toInt() and 0xFF
                offset += 10
                if (localPacked and 0x80 != 0) {
                    offset += 3 * (1 shl ((localPacked and 7) + 1))
                }
                offset = skipSubBlocks(offset + 1)
                delays.add(pendingDelay)
                pendingDelay = 0
            }
            else -> break
        }
    }
    return GifLayout(width, height, delays)
}

private fun readResource(name: String): ByteArray {
    val stream = assertNotNull(SDAnimatedImageTest::class.java.classLoader?.getResourceAsStream(name), name)
    return stream.use { it.readBytes() }
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SDAnimatedImageTest {
    @Test
    fun bundledGifsDecodeEveryFrameWithSdWebImageDurations() {
        for (name in listOf("-100", "Angry", "Moblin pixels", "Salty")) {
            val data = readResource("Alerts.bundle/$name.gif")
            val layout = parseGifLayout(data)
            val image = assertNotNull(SDAnimatedImage(data = data), name)
            assertEquals(layout.delays.size, image.animatedImageFrameCount, name)
            for (index in layout.delays.indices) {
                val expected = if (layout.delays[index] / 100.0 < 0.011) 0.1 else layout.delays[index] / 100.0
                assertEquals(expected, image.animatedImageDuration(at = index), 1e-9, "$name frame $index")
                val frame = assertNotNull(image.animatedImageFrame(at = index), "$name frame $index")
                assertEquals(layout.width, frame.width, name)
                assertEquals(layout.height, frame.height, name)
                assertEquals(Bitmap.Config.ARGB_8888, frame.config, name)
            }
            assertNull(image.animatedImageFrame(at = layout.delays.size))
        }
    }

    @Test
    fun firstGifFrameMatchesThePlatformDecoder() {
        val data = readResource("Alerts.bundle/Angry.gif")
        val image = assertNotNull(SDAnimatedImage(data = data))
        val frame = assertNotNull(image.animatedImageFrame(at = 0))
        val reference = assertNotNull(BitmapFactory.decodeByteArray(data, 0, data.size))
        var different = 0
        for (y in 0 until frame.height) {
            for (x in 0 until frame.width) {
                val a = frame.getPixel(x, y)
                val b = reference.getPixel(x, y)
                if (Color.alpha(a) != Color.alpha(b) || (Color.alpha(a) != 0 && a != b)) {
                    different += 1
                }
            }
        }
        assertTrue(different <= frame.width * frame.height / 1000, "$different pixels differ")
    }

    @Test
    fun framesAreDistinctBitmaps() {
        val image = assertNotNull(SDAnimatedImage(data = readResource("Alerts.bundle/Moblin pixels.gif")))
        assertTrue(image.animatedImageFrameCount > 1)
        val first = assertNotNull(image.animatedImageFrame(at = 0))
        val second = assertNotNull(image.animatedImageFrame(at = 1))
        assertTrue(first !== second)
        assertTrue(!first.isRecycled && !second.isRecycled)
    }

    @Test
    fun pngIsOneFrameOfOneTenthSecond() {
        val bitmap = Bitmap.createBitmap(7, 5, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.RED)
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        val image = assertNotNull(SDAnimatedImage(data = output.toByteArray()))
        assertEquals(1, image.animatedImageFrameCount)
        assertEquals(0.1, image.animatedImageDuration(at = 0), 1e-9)
        val frame = assertNotNull(image.animatedImageFrame(at = 0))
        assertEquals(7, frame.width)
        assertEquals(5, frame.height)
        assertEquals(Color.RED, frame.getPixel(3, 2))
    }

    @Test
    fun otherFormatsAreNotAnimatedImages() {
        val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
        assertNull(SDAnimatedImage(data = output.toByteArray()))
        assertNull(SDAnimatedImage(data = ByteArray(0)))
        assertNull(SDAnimatedImage(data = "GIF89a".toByteArray()))
    }

    @Test
    fun loopCountFollowsSdWebImage() {
        assertEquals(1, sdLoopCount(-1))
        assertEquals(0, sdLoopCount(0))
        assertEquals(3, sdLoopCount(3))
        assertEquals(0.1, sdFrameDuration(0), 1e-9)
        assertEquals(0.1, sdFrameDuration(10), 1e-9)
        assertEquals(0.02, sdFrameDuration(20), 1e-9)
    }
}

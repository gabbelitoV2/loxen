package com.moblin.android.platform.video

import com.moblin.android.platform.core.PipelineThread
import kotlin.math.abs
import kotlin.math.floor
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

class YCbCrSuite {
    @After
    fun tearDown() {
        YCbCrStorage.override = null
    }

    private fun quantize(value: Double): Double {
        return floor(value.coerceIn(0.0, 1.0) * 255 + 0.5) / 255
    }

    private fun roundTripErrors(descriptor: YCbCrDescriptor): Pair<Int, Double> {
        val forward = descriptor.forwardRows
        val inverse = descriptor.inverseRows
        val offset = descriptor.offset
        var maximum = 0
        var total = 0L
        for (red in 0 until 256) {
            val r = red / 255.0
            for (green in 0 until 256) {
                val g = green / 255.0
                for (blue in 0 until 256) {
                    val b = blue / 255.0
                    val y = quantize(forward[0] * r + forward[1] * g + forward[2] * b + offset[0]) - offset[0]
                    val cb = quantize(forward[3] * r + forward[4] * g + forward[5] * b + offset[1]) - offset[1]
                    val cr = quantize(forward[6] * r + forward[7] * g + forward[8] * b + offset[2]) - offset[2]
                    val outRed = quantize(inverse[0] * y + inverse[1] * cb + inverse[2] * cr) * 255
                    val outGreen = quantize(inverse[3] * y + inverse[4] * cb + inverse[5] * cr) * 255
                    val outBlue = quantize(inverse[6] * y + inverse[7] * cb + inverse[8] * cr) * 255
                    val errors = intArrayOf(
                        abs(Math.round(outRed).toInt() - red),
                        abs(Math.round(outGreen).toInt() - green),
                        abs(Math.round(outBlue).toInt() - blue),
                    )
                    for (error in errors) {
                        maximum = maxOf(maximum, error)
                        total += error
                    }
                }
            }
        }
        return Pair(maximum, total.toDouble() / (3L * 256 * 256 * 256))
    }

    @Test
    fun forwardTimesInverseIsTheIdentity() {
        for (descriptor in listOf(YCbCr.full, YCbCr.video)) {
            for (row in 0 until 3) {
                for (column in 0 until 3) {
                    var sum = 0.0
                    for (k in 0 until 3) {
                        sum += descriptor.forwardRows[row * 3 + k] * descriptor.inverseRows[k * 3 + column]
                    }
                    assertEquals(if (row == column) 1.0 else 0.0, sum, 1e-6)
                }
            }
            for (index in 0 until 9) {
                val row = index % 3
                val column = index / 3
                assertEquals(descriptor.inverseRows[row * 3 + column].toFloat(), descriptor.inverseMat3[index])
            }
        }
    }

    @Test
    fun fullRangeMatchesTheIosDefaultCoefficients() {
        val inverse = YCbCr.full.inverseRows
        assertEquals(1.5748, inverse[2], 1e-9)
        assertEquals(-0.187324, inverse[4], 1e-6)
        assertEquals(-0.468124, inverse[5], 1e-6)
        assertEquals(1.8556, inverse[7], 1e-9)
        assertEquals(128.0 / 255, YCbCr.full.offset[1], 1e-12)
        assertEquals(0.0, YCbCr.full.offset[0], 1e-12)
        assertEquals(16.0 / 255, YCbCr.video.offset[0], 1e-12)
        assertEquals(219.0 / 255 * 0.2126, YCbCr.video.forwardRows[0], 1e-12)
        assertEquals(224.0 / 255 * 0.5, YCbCr.video.forwardRows[5], 1e-12)
    }

    @Test
    fun fullRangeRoundTripOfEveryRgb8ColourIsWithinOneLsb() {
        val (maximum, mean) = roundTripErrors(YCbCr.full)
        assertEquals(1, maximum)
        assertTrue(mean < 0.34, "mean $mean")
    }

    @Test
    fun videoRangeRoundTripOfEveryRgb8ColourIsWithinTwoLsb() {
        val (maximum, mean) = roundTripErrors(YCbCr.video)
        assertEquals(2, maximum)
        assertTrue(mean < 0.41, "mean $mean")
    }

    @Test
    fun everyInGamutVideoRangeCodeRoundTripsBitExactly() {
        val descriptor = YCbCr.video
        val inverse = FloatArray(9) { descriptor.inverseRows[it].toFloat() }
        val forward = FloatArray(9) { descriptor.forwardRows[it].toFloat() }
        val offset = descriptor.offsetVector
        var inGamut = 0
        for (y in 16..235) {
            for (cb in 16..240) {
                for (cr in 16..240) {
                    val code = floatArrayOf(y / 255f - offset[0], cb / 255f - offset[1], cr / 255f - offset[2])
                    val rgb = FloatArray(3) { row ->
                        inverse[row * 3] * code[0] + inverse[row * 3 + 1] * code[1] + inverse[row * 3 + 2] * code[2]
                    }
                    if (rgb.any { it < 0f || it > 1f }) {
                        continue
                    }
                    inGamut += 1
                    val expected = intArrayOf(y, cb, cr)
                    for (row in 0 until 3) {
                        val value = forward[row * 3] * rgb[0] + forward[row * 3 + 1] * rgb[1] +
                            forward[row * 3 + 2] * rgb[2] + offset[row]
                        assertEquals(expected[row], floor(value * 255 + 0.5f).toInt(), "code $y,$cb,$cr row $row")
                    }
                }
            }
        }
        assertTrue(inGamut > 2_000_000, "in gamut $inGamut")
    }

    @Test
    fun storageFollowsTheRequestedTag() {
        YCbCrStorage.override = true
        assertEquals(PixelBufferLayout.rgba8, YCbCrStorage.layoutFor(kCVPixelFormatType_32BGRA))
        assertEquals(PixelBufferLayout.rgba8, YCbCrStorage.layoutFor(kCVPixelFormatType_32RGBA))
        assertEquals(PixelBufferLayout.ycbcr420Full, YCbCrStorage.layoutFor(0x23))
        assertEquals(PixelBufferLayout.ycbcr420Full, YCbCrStorage.layoutFor(kCVPixelFormatType_420YpCbCr8BiPlanarFullRange))
        assertEquals(PixelBufferLayout.ycbcr420Video, YCbCrStorage.layoutFor(kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange))
        assertEquals(0x23, YCbCrStorage.tagFor(PixelBufferLayout.rgba8, 0x23))
        assertEquals(
            kCVPixelFormatType_420YpCbCr8BiPlanarFullRange,
            YCbCrStorage.tagFor(PixelBufferLayout.ycbcr420Full, 0x23),
        )
        assertEquals(
            kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange,
            YCbCrStorage.tagFor(PixelBufferLayout.ycbcr420Video, kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange),
        )
        assertEquals(YCbCr.full, YCbCrStorage.descriptorFor(PixelBufferLayout.ycbcr420Full))
        assertEquals(YCbCr.video, YCbCrStorage.descriptorFor(PixelBufferLayout.ycbcr420Video))
        YCbCrStorage.disable("test")
        assertFalse(YCbCrStorage.isEnabled)
        assertEquals(PixelBufferLayout.rgba8, YCbCrStorage.layoutFor(0x23))
        YCbCrStorage.override = false
        assertEquals(PixelBufferLayout.rgba8, YCbCrStorage.layoutFor(0x23))
        assertEquals(PixelBufferLayout.rgba8, YCbCrStorage.layoutFor(kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange))
    }

    @Test
    fun chromaGeometryCoversOddSizes() {
        assertEquals(listOf(1f, 1f), YCbCrGeometry.chromaExtent(1920, 1080).toList())
        assertEquals(listOf(1f, 1f), YCbCrGeometry.chromaScale(1920, 1080).toList())
        assertEquals(listOf(1920f / 1919, 1080f / 1079), YCbCrGeometry.chromaExtent(1919, 1079).toList())
        assertEquals(listOf(1919f / 1920, 1079f / 1080), YCbCrGeometry.chromaScale(1919, 1079).toList())
        assertEquals(960, PixelBufferLayout.ycbcr420Full.chromaWidth(1919))
        assertEquals(540, PixelBufferLayout.ycbcr420Full.chromaHeight(1079))
        assertFalse(YCbCrGeometry.lumaClampActive(1920))
        assertFalse(YCbCrGeometry.lumaClampActive(1080))
        assertTrue(YCbCrGeometry.lumaClampActive(1919))
        assertTrue(YCbCrGeometry.lumaClampActive(1079))
        assertFalse(YCbCrGeometry.lumaClampActive(2))
        assertTrue(YCbCrGeometry.lumaClampActive(3))
    }
}

@RunWith(RobolectricTestRunner::class)
class YCbCrProbeSuite {
    @Test
    fun probeFailsClosedWhenReadbackCannotBeVerified() {
        val ready = PipelineThread.runSync(timeoutMs = 5000) { EglCore.isReady }
        assertTrue(ready)
        assertTrue(EglCore.glMajorVersion >= 3)
        YCbCrStorage.override = null
        assertFalse(YCbCrStorage.isEnabled)
        val failure = assertNotNull(YCbCrStorage.probeFailureReason)
        assertTrue(failure.startsWith("stage 1"), failure)
        assertEquals(PixelBufferLayout.rgba8, YCbCrStorage.layoutFor(0x23))
    }
}

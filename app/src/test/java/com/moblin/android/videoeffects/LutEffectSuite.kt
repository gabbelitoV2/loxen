package com.moblin.android.videoeffects

import android.graphics.BitmapFactory
import com.moblin.android.isEqual
import com.moblin.android.localized
import com.moblin.android.platform.swiftcube.LutEntry
import com.moblin.android.platform.swiftcube.SC3DLut
import com.moblin.android.platform.swiftcube.SwiftCubeError
import com.moblin.android.readMainFile
import com.moblin.android.readTestFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private fun makeLut(dimension: Int, function: (SIMD3) -> SIMD3): List<SIMD3> {
    val lut = MutableList(dimension * dimension * dimension) { SIMD3(0f, 0f, 0f) }
    for (blue in 0 until dimension) {
        for (green in 0 until dimension) {
            for (red in 0 until dimension) {
                val denominator = (dimension - 1).toFloat()
                val input = SIMD3(
                    red.toFloat() / denominator,
                    green.toFloat() / denominator,
                    blue.toFloat() / denominator
                )
                lut[blue * dimension * dimension + green * dimension + red] = function(input)
            }
        }
    }
    return lut
}

private fun makeCubeFile(dimension: Int, function: (SIMD3) -> SIMD3): ByteArray {
    val lines = mutableListOf("TITLE \"Test\"", "LUT_3D_SIZE $dimension")
    for (e in makeLut(dimension, function)) {
        lines.add("${e.x} ${e.y} ${e.z}")
    }
    return lines.joinToString("\n").toByteArray(Charsets.UTF_8)
}

private fun isEqual(actual: SIMD3, expected: SIMD3, epsilon: Float = 1e-6f): Boolean {
    val dx = actual.x - expected.x
    val dy = actual.y - expected.y
    val dz = actual.z - expected.z
    return maxOf(abs(dx), abs(dy), abs(dz)) < epsilon
}

private fun identity(input: SIMD3): SIMD3 {
    return input
}

private fun swapRedAndBlue(input: SIMD3): SIMD3 {
    return SIMD3(input.z, input.y, input.x)
}

private fun linear(input: SIMD3): SIMD3 {
    return SIMD3(
        0.25f + 0.5f * input.x,
        1f - input.y,
        0.1f * input.x + 0.2f * input.y + 0.3f * input.z
    )
}

private fun entry(entries: List<LutEntry>, size: Int, red: Int, green: Int, blue: Int): SIMD3 {
    val e = entries[(blue * size + green) * size + red]
    return SIMD3(e.red, e.green, e.blue)
}

private fun entry(cubeData: ByteArray, dimension: Int, red: Int, green: Int, blue: Int): SIMD3 {
    val index = 4 * ((blue * dimension + green) * dimension + red)
    val cube = ByteBuffer.wrap(cubeData).order(ByteOrder.nativeOrder()).asFloatBuffer()
    return SIMD3(cube.get(index), cube.get(index + 1), cube.get(index + 2))
}

@RunWith(RobolectricTestRunner::class)
class LutEffectSuite {
    @Test
    fun interpolate3dAtGridPoints() {
        val dimension = 5
        val lut = makeLut(dimension, ::swapRedAndBlue)
        for (blue in 0 until dimension) {
            for (green in 0 until dimension) {
                for (red in 0 until dimension) {
                    val denominator = (dimension - 1).toFloat()
                    val point = SIMD3(
                        blue.toFloat() / denominator,
                        green.toFloat() / denominator,
                        red.toFloat() / denominator
                    )
                    val value = interpolate3d(point, lut, dimension)
                    assertTrue(
                        isEqual(
                            value,
                            lut[blue * dimension * dimension + green * dimension + red]
                        )
                    )
                }
            }
        }
    }

    @Test
    fun interpolate3dBetweenGridPoints() {
        val dimension = 3
        val lut = makeLut(dimension, ::linear)
        val points = listOf(
            SIMD3(0.1f, 0.6f, 0.9f),
            SIMD3(0.5f, 0.5f, 0.5f),
            SIMD3(0.75f, 0f, 1f),
            SIMD3(0.99f, 0.01f, 0.4f)
        )
        for (point in points) {
            val value = interpolate3d(point, lut, dimension)
            assertTrue(isEqual(value, linear(SIMD3(point.z, point.y, point.x))))
        }
    }

    @Test
    fun interpolate3dClampsOutOfRangeInput() {
        val lut = makeLut(4, ::identity)
        assertTrue(
            isEqual(
                interpolate3d(SIMD3(-1f, -0.5f, -10f), lut, 4),
                SIMD3(0f, 0f, 0f)
            )
        )
        assertTrue(
            isEqual(
                interpolate3d(SIMD3(1.5f, 2f, 100f), lut, 4),
                SIMD3(1f, 1f, 1f)
            )
        )
    }

    @Test
    fun convertIdentityLutTo64() {
        val lut64 = convertLutTo64(makeLut(65, ::identity), 65)
        assertEquals(64 * 64 * 64, lut64.size)
        for ((actual, expected) in lut64.zip(makeLut(64, ::identity))) {
            assertTrue(isEqual(actual, expected))
        }
    }

    @Test
    fun convertLutTo64KeepsChannelOrder() {
        val lut64 = convertLutTo64(makeLut(96, ::linear), 96)
        for ((actual, expected) in lut64.zip(makeLut(64, ::linear))) {
            assertTrue(isEqual(actual, expected))
        }
    }

    @Test
    fun convertCubeFile() {
        val lut = lutEffectConvertCube(makeCubeFile(3, ::swapRedAndBlue))
        assertEquals(3, lut.size)
        assertEquals(27, lut.entries.size)
        assertTrue(isEqual(entry(lut.entries, lut.size, 0, 0, 0), SIMD3(0f, 0f, 0f)))
        assertTrue(isEqual(entry(lut.entries, lut.size, 2, 0, 0), SIMD3(0f, 0f, 1f)))
        assertTrue(isEqual(entry(lut.entries, lut.size, 0, 1, 0), SIMD3(0f, 0.5f, 0f)))
        assertTrue(isEqual(entry(lut.entries, lut.size, 0, 0, 2), SIMD3(1f, 0f, 0f)))
        assertTrue(isEqual(entry(lut.entries, lut.size, 2, 2, 2), SIMD3(1f, 1f, 1f)))
        val cubeData = makeCubeData(lut.entries)
        assertEquals(27 * 4 * 4, cubeData.size)
        assertTrue(isEqual(entry(cubeData, 3, 2, 0, 0), SIMD3(0f, 0f, 1f)))
        val cube = ByteBuffer.wrap(cubeData).order(ByteOrder.nativeOrder()).asFloatBuffer()
        var index = 3
        while (index < cube.limit()) {
            assertEquals(1f, cube.get(index))
            index += 4
        }
    }

    @Test
    fun convertBigCubeFileTo64() {
        val lut = lutEffectConvertCube(makeCubeFile(65, ::linear))
        assertEquals(64, lut.size)
        assertEquals(64 * 64 * 64, lut.entries.size)
        assertTrue(
            isEqual(
                entry(lut.entries, lut.size, 0, 0, 0),
                linear(SIMD3(0f, 0f, 0f))
            )
        )
        assertTrue(
            isEqual(
                entry(lut.entries, lut.size, 63, 0, 0),
                linear(SIMD3(1f, 0f, 0f))
            )
        )
        assertTrue(
            isEqual(
                entry(lut.entries, lut.size, 21, 42, 63),
                linear(SIMD3(21f / 63f, 42f / 63f, 1f))
            )
        )
    }

    @Test
    fun convertCubeFileErrors() {
        assertFailsWith<SwiftCubeError> {
            lutEffectConvertCube("LUT_1D_SIZE 2\n0 0 0\n1 1 1\n".toByteArray())
        }
        assertFailsWith<SwiftCubeError> {
            lutEffectConvertCube("LUT_3D_SIZE 2\n0 0 0\n".toByteArray())
        }
        assertFailsWith<SwiftCubeError> {
            lutEffectConvertCube(byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 0x00))
        }
    }

    @Test
    fun parseCubeFileFormats() {
        val text = "# Created by test\r\n" +
            "TITLE \"My LUT\"\r\n" +
            "\r\n" +
            "LUT_3D_SIZE 2\r\n" +
            "\t0 0 0\r\n" +
            "1.5e-1\t-0.25 +.5  \r\n" +
            "0.125E+1 100e-3 1E0\r\n" +
            "0.000001 1. 12345678901234567890\n" +
            "7e-4 8.50 9\n" +
            "1 2 3\n" +
            "4 5 6\n" +
            "7 8 9"
        val lut = SC3DLut(text.toByteArray(Charsets.UTF_8))
        assertEquals("My LUT", lut.title)
        assertEquals(2, lut.size)
        assertEquals(8, lut.entries.size)
        assertTrue(isEqual(entry(lut.entries, lut.size, 0, 0, 0), SIMD3(0f, 0f, 0f)))
        assertTrue(
            isEqual(
                entry(lut.entries, lut.size, 1, 0, 0),
                SIMD3(0.15f, -0.25f, 0.5f)
            )
        )
        assertTrue(
            isEqual(
                entry(lut.entries, lut.size, 0, 1, 0),
                SIMD3(1.25f, 0.1f, 1f)
            )
        )
        val big = entry(lut.entries, lut.size, 1, 1, 0)
        assertTrue(isEqual(big.x, 0.000001f, 1e-12f))
        assertEquals(1f, big.y)
        assertTrue(isEqual(big.z, 1.2345678901234568e19f, 1e12f))
        assertTrue(
            isEqual(
                entry(lut.entries, lut.size, 0, 0, 1),
                SIMD3(0.0007f, 8.5f, 9f)
            )
        )
    }

    @Test
    fun parseCubeFileSyntaxErrors() {
        val lines = listOf("1 2", "1 2 3 4", "1 2 x", "1e 2 3", "1.2.3 4 5", "- 2 3", "1 2 3;")
        for (line in lines) {
            assertFailsWith<SwiftCubeError> {
                SC3DLut("LUT_3D_SIZE 1\n$line\n".toByteArray())
            }
        }
        assertFailsWith<SwiftCubeError> {
            SC3DLut("LUT_3D_SIZE 100\n".toByteArray())
        }
        assertFailsWith<SwiftCubeError> {
            SC3DLut("LUT_3D_SIZE 1\nDOMAIN_MIN 0 0 0\n1 1 1\n".toByteArray())
        }
        assertFailsWith<SwiftCubeError> {
            SC3DLut("LUT_3D_SIZE 1\nFOO\n1 1 1\n".toByteArray())
        }
        assertFailsWith<SwiftCubeError> {
            SC3DLut("1 1 1\n".toByteArray())
        }
    }

    @Test
    fun lutImageRoundTrip() {
        val dimension = 8
        val original = lutEffectConvertCube(makeCubeFile(dimension, ::linear))
        val cubeData = makeCubeData(original.entries)
        val cgImage = assertNotNull(makeLutCgImage(dimension, cubeData))
        assertEquals(dimension * dimension, cgImage.width)
        assertEquals(dimension, cgImage.height)
        val (convertedDimension, convertedData) = lutEffectConvertLut(cgImage)
        assertEquals(dimension.toFloat(), convertedDimension)
        for (blue in 0 until dimension) {
            for (green in 0 until dimension) {
                for (red in 0 until dimension) {
                    val actual = entry(convertedData, dimension, red, green, blue)
                    val expected = entry(original.entries, original.size, red, green, blue)
                    assertTrue(isEqual(actual, expected, 0.6f / 255f))
                }
            }
        }
    }

    @Test
    fun convertBigPngLutTo64() {
        val dimension = 65
        val original = SC3DLut(makeCubeFile(dimension, ::linear))
        val cgImage = assertNotNull(
            makeLutCgImage(dimension, makeCubeData(original.entries))
        )
        val (convertedDimension, convertedData) = lutEffectConvertLut(cgImage)
        assertEquals(64f, convertedDimension)
        assertEquals(64 * 64 * 64 * 4 * 4, convertedData.size)
        val points = listOf(
            Triple(0, 0, 0),
            Triple(63, 0, 0),
            Triple(0, 63, 0),
            Triple(0, 0, 63),
            Triple(21, 42, 63),
            Triple(63, 63, 63)
        )
        for ((red, green, blue) in points) {
            val actual = entry(convertedData, 64, red, green, blue)
            val expected = linear(
                SIMD3(
                    red.toFloat() / 63f,
                    green.toFloat() / 63f,
                    blue.toFloat() / 63f
                )
            )
            assertTrue(isEqual(actual, expected, 0.6f / 255f))
        }
    }

    @Test
    fun renderBigPngLutWithMetalPetal() {
        Unit
    }

    @Test
    fun appleLogToRec709() {
        val bytes = readMainFile(name = "LUTs.bundle/Apple Log To Rec 709", suffix = "png")
        val image = assertNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
        val (dimension, data) = lutEffectConvertLut(image)
        assertEquals(64f, dimension)
        assertEquals(4_194_304, data.size)
    }

    @Test
    fun dither64() {
        val bytes = readTestFile(name = "dither64", suffix = "png")
        val image = assertNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
        val exception = assertFailsWith<Exception> { lutEffectConvertLut(image, pngComponentsPerPixel(bytes)) }
        assertEquals(localized("LUT image is not 3 or 4 components per pixel"), exception.message)
    }
}

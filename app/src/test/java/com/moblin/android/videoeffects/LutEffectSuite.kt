package com.moblin.android.videoeffects

import com.moblin.android.isEqual
import com.moblin.android.localized
import com.moblin.android.platform.simd.SIMD3
import com.moblin.android.platform.swiftcube.SC3DLut
import com.moblin.android.platform.swiftcube.SwiftCubeError
import com.moblin.android.platform.uikit.UIImage
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
    val lut = MutableList(dimension * dimension * dimension) { SIMD3.zero }
    for (blue in 0 until dimension) {
        for (green in 0 until dimension) {
            for (red in 0 until dimension) {
                val input = SIMD3(red.toFloat(), green.toFloat(), blue.toFloat()) / (dimension - 1).toFloat()
                lut[blue * dimension * dimension + green * dimension + red] = function(input)
            }
        }
    }
    return lut
}

private fun makeCubeFile(dimension: Int, function: (SIMD3) -> SIMD3): ByteArray {
    val lines = mutableListOf("TITLE \"Test\"", "LUT_3D_SIZE $dimension")
    for (entry in makeLut(dimension, function)) {
        lines.add("${entry.x} ${entry.y} ${entry.z}")
    }
    return lines.joinToString("\n").toByteArray(Charsets.UTF_8)
}

private fun isEqual(actual: SIMD3, expected: SIMD3, epsilon: Float = 1e-6f): Boolean {
    val difference = actual - expected
    return maxOf(abs(difference.x), abs(difference.y), abs(difference.z)) < epsilon
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
        0.1f * input.x + 0.2f * input.y + 0.3f * input.z,
    )
}

private fun entry(lut: SC3DLut, red: Int, green: Int, blue: Int): SIMD3 {
    val entry = lut.entries[(blue * lut.size + green) * lut.size + red]
    return SIMD3(entry.red, entry.green, entry.blue)
}

private fun entry(cubeData: ByteArray, dimension: Int, red: Int, green: Int, blue: Int): SIMD3 {
    val index = 4 * ((blue * dimension + green) * dimension + red)
    val cube = ByteBuffer.wrap(cubeData).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
    return SIMD3(cube[index], cube[index + 1], cube[index + 2])
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
                    val point = SIMD3(blue.toFloat(), green.toFloat(), red.toFloat()) / (dimension - 1).toFloat()
                    val value = interpolate3d(point, lut, dimension)
                    assertTrue(isEqual(value, lut[blue * dimension * dimension + green * dimension + red]))
                }
            }
        }
    }

    @Test
    fun interpolate3dBetweenGridPoints() {
        val dimension = 3
        val lut = makeLut(dimension, ::linear)
        for (point in listOf(
            SIMD3(0.1f, 0.6f, 0.9f),
            SIMD3(0.5f, 0.5f, 0.5f),
            SIMD3(0.75f, 0f, 1f),
            SIMD3(0.99f, 0.01f, 0.4f),
        )) {
            val value = interpolate3d(point, lut, dimension)
            assertTrue(isEqual(value, linear(SIMD3(point.z, point.y, point.x))))
        }
    }

    @Test
    fun interpolate3dClampsOutOfRangeInput() {
        val lut = makeLut(4, ::identity)
        assertTrue(isEqual(interpolate3d(SIMD3(-1f, -0.5f, -10f), lut, 4), SIMD3(0f, 0f, 0f)))
        assertTrue(isEqual(interpolate3d(SIMD3(1.5f, 2f, 100f), lut, 4), SIMD3(1f, 1f, 1f)))
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
        assertTrue(isEqual(entry(lut, 0, 0, 0), SIMD3(0f, 0f, 0f)))
        assertTrue(isEqual(entry(lut, 2, 0, 0), SIMD3(0f, 0f, 1f)))
        assertTrue(isEqual(entry(lut, 0, 1, 0), SIMD3(0f, 0.5f, 0f)))
        assertTrue(isEqual(entry(lut, 0, 0, 2), SIMD3(1f, 0f, 0f)))
        assertTrue(isEqual(entry(lut, 2, 2, 2), SIMD3(1f, 1f, 1f)))
        val cubeData = makeCubeData(lut.entries)
        assertEquals(27 * 4 * 4, cubeData.size)
        assertTrue(isEqual(entry(cubeData, dimension = 3, red = 2, green = 0, blue = 0), SIMD3(0f, 0f, 1f)))
        val cube = ByteBuffer.wrap(cubeData).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
        for (index in 3 until cube.capacity() step 4) {
            assertTrue(cube[index] == 1f)
        }
    }

    @Test
    fun convertBigCubeFileTo64() {
        val lut = lutEffectConvertCube(makeCubeFile(65, ::linear))
        assertEquals(64, lut.size)
        assertEquals(64 * 64 * 64, lut.entries.size)
        assertTrue(isEqual(entry(lut, 0, 0, 0), linear(SIMD3(0f, 0f, 0f))))
        assertTrue(isEqual(entry(lut, 63, 0, 0), linear(SIMD3(1f, 0f, 0f))))
        assertTrue(isEqual(entry(lut, 21, 42, 63), linear(SIMD3(21f / 63f, 42f / 63f, 1f))))
    }

    @Test
    fun convertCubeFileErrors() {
        assertFailsWith<SwiftCubeError> {
            lutEffectConvertCube("LUT_1D_SIZE 2\n0 0 0\n1 1 1\n".toByteArray(Charsets.UTF_8))
        }
        assertFailsWith<SwiftCubeError> {
            lutEffectConvertCube("LUT_3D_SIZE 2\n0 0 0\n".toByteArray(Charsets.UTF_8))
        }
        assertFailsWith<SwiftCubeError> {
            lutEffectConvertCube(byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 0x00))
        }
    }

    @Test
    fun parseCubeFileFormats() {
        val lut = SC3DLut(
            fileData = (
                "# Created by test\r\n" +
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
                ).toByteArray(Charsets.UTF_8),
        )
        assertEquals("My LUT", lut.title)
        assertEquals(2, lut.size)
        assertEquals(8, lut.entries.size)
        assertTrue(isEqual(entry(lut, 0, 0, 0), SIMD3(0f, 0f, 0f)))
        assertTrue(isEqual(entry(lut, 1, 0, 0), SIMD3(0.15f, -0.25f, 0.5f)))
        assertTrue(isEqual(entry(lut, 0, 1, 0), SIMD3(1.25f, 0.1f, 1f)))
        val big = entry(lut, 1, 1, 0)
        assertTrue(isEqual(big.x, 0.000001f, 1e-12f))
        assertTrue(big.y == 1f)
        assertTrue(isEqual(big.z, 12_345_678_901_234_567_890f, 1e12f))
        assertTrue(isEqual(entry(lut, 0, 0, 1), SIMD3(0.0007f, 8.5f, 9f)))
    }

    @Test
    fun parseCubeFileSyntaxErrors() {
        for (line in listOf("1 2", "1 2 3 4", "1 2 x", "1e 2 3", "1.2.3 4 5", "- 2 3", "1 2 3;")) {
            assertFailsWith<SwiftCubeError> {
                SC3DLut(fileData = "LUT_3D_SIZE 1\n$line\n".toByteArray(Charsets.UTF_8))
            }
        }
        assertFailsWith<SwiftCubeError> {
            SC3DLut(fileData = "LUT_3D_SIZE 100\n".toByteArray(Charsets.UTF_8))
        }
        assertFailsWith<SwiftCubeError> {
            SC3DLut(fileData = "LUT_3D_SIZE 1\nDOMAIN_MIN 0 0 0\n1 1 1\n".toByteArray(Charsets.UTF_8))
        }
        assertFailsWith<SwiftCubeError> {
            SC3DLut(fileData = "LUT_3D_SIZE 1\nFOO\n1 1 1\n".toByteArray(Charsets.UTF_8))
        }
        assertFailsWith<SwiftCubeError> {
            SC3DLut(fileData = "1 1 1\n".toByteArray(Charsets.UTF_8))
        }
    }

    @Test
    fun lutImageRoundTrip() {
        val dimension = 8
        val original = lutEffectConvertCube(makeCubeFile(dimension, ::linear))
        val cubeData = makeCubeData(original.entries)
        val cgImage = assertNotNull(makeLutCgImage(dimension = dimension, cubeData = cubeData))
        assertEquals(dimension * dimension, cgImage.width)
        assertEquals(dimension, cgImage.height)
        val (convertedDimension, convertedData) = lutEffectConvertLut(cgImage)
        assertEquals(dimension.toFloat(), convertedDimension)
        for (blue in 0 until dimension) {
            for (green in 0 until dimension) {
                for (red in 0 until dimension) {
                    val actual = entry(
                        convertedData,
                        dimension = dimension,
                        red = red,
                        green = green,
                        blue = blue,
                    )
                    val expected = entry(original, red = red, green = green, blue = blue)
                    assertTrue(isEqual(actual, expected, 0.6f / 255f))
                }
            }
        }
    }

    @Test
    fun convertBigPngLutTo64() {
        val dimension = 65
        val original = SC3DLut(fileData = makeCubeFile(dimension, ::linear))
        val cgImage = assertNotNull(
            makeLutCgImage(dimension = dimension, cubeData = makeCubeData(original.entries)),
        )
        val (convertedDimension, convertedData) = lutEffectConvertLut(cgImage)
        assertEquals(64f, convertedDimension)
        assertEquals(64 * 64 * 64 * 4 * 4, convertedData.size)
        for ((red, green, blue) in listOf(
            Triple(0, 0, 0),
            Triple(63, 0, 0),
            Triple(0, 63, 0),
            Triple(0, 0, 63),
            Triple(21, 42, 63),
            Triple(63, 63, 63),
        )) {
            val actual = entry(convertedData, dimension = 64, red = red, green = green, blue = blue)
            val expected = linear(SIMD3(red.toFloat(), green.toFloat(), blue.toFloat()) / 63f)
            assertTrue(isEqual(actual, expected, 0.6f / 255f))
        }
    }

    @Test
    fun appleLogToRec709() {
        val image = assertNotNull(
            UIImage(
                data = readMainFile(
                    name = "LUTs.bundle/Apple Log To Rec 709",
                    suffix = "png",
                ),
            ),
        )
        val (dimension, data) = lutEffectConvertLut(image)
        assertEquals(64f, dimension)
        assertEquals(4_194_304, data.size)
    }

    @Test
    fun dither64() {
        val image = assertNotNull(UIImage(data = readTestFile(name = "dither64", suffix = "png")))
        val error = assertFailsWith<Exception> {
            lutEffectConvertLut(image)
        }
        assertEquals(localized("LUT image is not 3 or 4 components per pixel"), error.message)
    }
}

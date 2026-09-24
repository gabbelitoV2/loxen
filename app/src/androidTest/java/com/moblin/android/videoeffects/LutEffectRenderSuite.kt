package com.moblin.android.videoeffects

import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.moblin.android.platform.coregraphics.CGColorSpaceCreateDeviceRGB
import com.moblin.android.platform.coregraphics.CGContext
import com.moblin.android.platform.coregraphics.CGImageAlphaInfo
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.metalpetal.MTIColor
import com.moblin.android.platform.metalpetal.MTIColorLookupFilter
import com.moblin.android.platform.metalpetal.MTIContext
import com.moblin.android.platform.metalpetal.MTIImage
import com.moblin.android.platform.metalpetal.MTKTextureLoader
import com.moblin.android.platform.metalpetal.MTLCreateSystemDefaultDevice
import com.moblin.android.platform.simd.SIMD3
import com.moblin.android.platform.video.CVPixelBufferPool
import com.moblin.android.platform.video.kCVPixelFormatType_32BGRA
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

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

private fun swapRedAndBlue(input: SIMD3): SIMD3 {
    return SIMD3(input.z, input.y, input.x)
}

@RunWith(AndroidJUnit4::class)
class LutEffectRenderSuite {
    @Test
    fun renderCubeLutWithMetalPetal() {
        val (dimension, cubeData) = lutEffectConvertCube(makeCubeFile(8, ::swapRedAndBlue))
        val lutCgImage = makeLutCgImage(dimension = dimension.toInt(), cubeData = cubeData)
        assertNotNull(lutCgImage)
        val filter = MTIColorLookupFilter()
        filter.inputColorLookupTable = MTIImage(
            cgImage = lutCgImage!!,
            options = mapOf(MTKTextureLoader.Option.SRGB to false),
            isOpaque = true,
        )
        filter.inputImage = MTIImage(
            color = MTIColor(red = 0.25f, green = 0.5f, blue = 0.75f, alpha = 1f),
            sRGB = false,
            size = CGSize(width = 4, height = 4),
        )
        val outputImage = filter.outputImage
        assertNotNull(outputImage)
        val pixelBuffer = CVPixelBufferPool(4, 4, kCVPixelFormatType_32BGRA).createPixelBuffer()
        assertNotNull(pixelBuffer)
        val device = MTLCreateSystemDefaultDevice()
        assertNotNull(device)
        MTIContext(device = device!!).render(outputImage!!, to = pixelBuffer!!)
        val bitmap = pixelBuffer.toBitmap()
        assertNotNull(bitmap)
        val pixel = bitmap!!.getPixel(0, 0)
        assertTrue(abs(Color.blue(pixel) - 64) <= 2)
        assertTrue(abs(Color.green(pixel) - 128) <= 2)
        assertTrue(abs(Color.red(pixel) - 191) <= 2)
    }

    @Test
    fun renderBigPngLutWithMetalPetal() {
        val size = 2744
        val context = CGContext(
            data = null,
            width = size,
            height = size,
            bitsPerComponent = 8,
            bytesPerRow = size * 4,
            space = CGColorSpaceCreateDeviceRGB(),
            bitmapInfo = CGImageAlphaInfo.premultipliedLast.rawValue,
        )
        assertNotNull(context)
        val pngLut = context!!.makeImage()
        assertNotNull(pngLut)
        val (dimension, cubeData) = lutEffectConvertLut(pngLut!!)
        assertEquals(64f, dimension)
        val lutCgImage = makeLutCgImage(dimension = dimension.toInt(), cubeData = cubeData)
        assertNotNull(lutCgImage)
        val filter = MTIColorLookupFilter()
        filter.inputColorLookupTable = MTIImage(
            cgImage = lutCgImage!!,
            options = mapOf(MTKTextureLoader.Option.SRGB to false),
            isOpaque = true,
        )
        filter.inputImage = MTIImage(
            color = MTIColor(red = 0.5f, green = 0.5f, blue = 0.5f, alpha = 1f),
            sRGB = false,
            size = CGSize(width = 64, height = 64),
        )
        val outputImage = filter.outputImage
        assertNotNull(outputImage)
        val pixelBuffer = CVPixelBufferPool(64, 64, kCVPixelFormatType_32BGRA).createPixelBuffer()
        assertNotNull(pixelBuffer)
        val device = MTLCreateSystemDefaultDevice()
        assertNotNull(device)
        MTIContext(device = device!!).render(outputImage!!, to = pixelBuffer!!)
    }
}

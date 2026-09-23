package com.moblin.android.videoeffects

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
import com.moblin.android.platform.video.CVPixelBufferPool
import com.moblin.android.platform.video.kCVPixelFormatType_32BGRA
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LutEffectRenderSuite {
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

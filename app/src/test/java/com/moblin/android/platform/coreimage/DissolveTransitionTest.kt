package com.moblin.android.platform.coreimage

import com.moblin.android.platform.coregraphics.CGRect
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class DissolveTransitionTest {
    @Test
    fun extentIsTheUnionOfBothImages() {
        val filter = CIFilter.dissolveTransition()
        filter.inputImage = CIImage.black.cropped(to = CGRect(0.0, 0.0, 100.0, 50.0))
        filter.targetImage = CIImage.white.cropped(to = CGRect(50.0, 10.0, 100.0, 100.0))
        filter.time = 0.25f
        val output = assertNotNull(filter.outputImage)
        assertEquals(CGRect(0.0, 0.0, 150.0, 110.0), output.extent)
    }

    @Test
    fun missingImagesGiveNoOutput() {
        val filter = CIFilter.dissolveTransition()
        filter.inputImage = CIImage.black.cropped(to = CGRect(0.0, 0.0, 10.0, 10.0))
        assertNull(filter.outputImage)
        filter.inputImage = null
        filter.targetImage = CIImage.black.cropped(to = CGRect(0.0, 0.0, 10.0, 10.0))
        assertNull(filter.outputImage)
    }

    @Test
    fun keyValueCodingMatchesTheProperties() {
        val filter = CIFilter.dissolveTransition()
        val input = CIImage.black.cropped(to = CGRect(0.0, 0.0, 10.0, 10.0))
        filter.setValue(input, forKey = kCIInputImageKey)
        filter.setValue(0.75, forKey = kCIInputTimeKey)
        assertTrue(filter.inputImage === input)
        assertEquals(0.75f, filter.time)
        assertEquals("CIDissolveTransition", filter.name)
    }

    @Test
    fun everyCallMakesANewImage() {
        val filter = CIFilter.dissolveTransition()
        filter.inputImage = CIImage.black.cropped(to = CGRect(0.0, 0.0, 10.0, 10.0))
        filter.targetImage = CIImage.white.cropped(to = CGRect(0.0, 0.0, 10.0, 10.0))
        assertTrue(filter.outputImage !== filter.outputImage)
        assertTrue(filter.outputImage !== filter.inputImage)
    }
}

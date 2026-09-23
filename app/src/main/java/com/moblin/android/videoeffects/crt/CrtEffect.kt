package com.moblin.android.videoeffects.crt

import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.platform.coregraphics.CGAffineTransform
import com.moblin.android.platform.coregraphics.CGPoint
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.coreimage.CIColor
import com.moblin.android.platform.coreimage.CIFilter
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.metalpetal.MTICrtFilter
import com.moblin.android.platform.metalpetal.MTIImage
import kotlin.math.PI

class CrtEffect : VideoEffect() {
    private val barrelFilter = CrtBarrelDistortionFilter()
    private val colorControls = CIFilter.colorControls()
    private val vignette = CIFilter.vignette()
    private val crtFilter = MTICrtFilter()

    override fun execute(image: CIImage, info: VideoEffectInfo): CIImage {
        val extent = image.extent
        val cropRect = CGRect(
            x = extent.width / 8,
            y = 0.0,
            width = 3 * extent.width / 4,
            height = extent.height,
        )
        var image = image.cropped(to = cropRect)
        image = applyScanlines(image, cropRect)
        image = applyBarrelDistortion(image, extent.width)
        image = applyColors(image)
        return image.composited(over = CIImage.black.cropped(to = extent))
    }

    override fun executeMetalPetal(image: MTIImage, info: VideoEffectInfo): MTIImage {
        crtFilter.inputImage = image
        crtFilter.barrelStrength = 0.5f
        return crtFilter.outputImage ?: image
    }

    private fun applyBarrelDistortion(image: CIImage, width: Double): CIImage {
        barrelFilter.inputImage = image
        barrelFilter.width = width
        return (barrelFilter.outputImage as? CIImage) ?: image
    }

    private fun applyColors(image: CIImage): CIImage {
        colorControls.inputImage = image
        colorControls.saturation = 0.7f
        colorControls.contrast = 1.05f
        colorControls.brightness = -0.02f
        vignette.inputImage = colorControls.outputImage ?: image
        vignette.intensity = 1.5f
        vignette.radius = 1.5f
        return vignette.outputImage ?: image
    }

    private fun applyScanlines(image: CIImage, cropRect: CGRect): CIImage {
        val scanlineWidth = maxOf(1.0f, (cropRect.height / 240.0).toFloat())
        val stripes = CIFilter.stripesGenerator()
        stripes.color0 = CIColor(red = 0.0, green = 0.0, blue = 0.0, alpha = 0.25)
        stripes.color1 = CIColor.clear
        stripes.width = scanlineWidth
        stripes.sharpness = 0.3f
        stripes.center = CGPoint.zero
        return stripes.outputImage
            ?.transformed(by = CGAffineTransform(rotationAngle = PI / 2))
            ?.cropped(to = cropRect)
            ?.composited(over = image) ?: image
    }
}

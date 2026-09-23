package com.moblin.android.videoeffects

import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.platform.coregraphics.CGAffineTransform
import com.moblin.android.platform.coregraphics.CGPoint
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.coreimage.CIFilter
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.metalpetal.MTIMultilayerCompositingFilter
import com.moblin.android.platform.metalpetal.MTIImage
import com.moblin.android.platform.metalpetal.MTILayer

class TripleEffect : VideoEffect() {
    private val centerFilter = CIFilter.sourceOverCompositing()
    private val rightFilter = CIFilter.sourceOverCompositing()

    override fun execute(image: CIImage, info: VideoEffectInfo): CIImage {
        val size = image.extent.size
        val width = size.width / 3
        val height = size.height
        val centerImage = image.cropped(to = CGRect(
            x = width,
            y = 0.0,
            width = width,
            height = height
        ))
        val leftImage = centerImage.transformed(by = CGAffineTransform(translationX = -width, y = 0.0))
        val rightImage = centerImage.transformed(by = CGAffineTransform(translationX = width, y = 0.0))
        centerFilter.inputImage = centerImage
        centerFilter.backgroundImage = leftImage
        rightFilter.inputImage = rightImage
        rightFilter.backgroundImage = centerFilter.outputImage
        return rightFilter.outputImage ?: image
    }

    override fun executeMetalPetal(image: MTIImage, info: VideoEffectInfo): MTIImage {
        val size = image.extent.size
        val width = size.width / 3
        val height = size.height
        val centerRegion = CGRect(x = width, y = 0.0, width = width, height = height)
        val filter = MTIMultilayerCompositingFilter()
        filter.inputBackgroundImage = image
        filter.layers = (0 until 3).map { index ->
            MTILayer(content = image,
                contentRegion = centerRegion,
                position = CGPoint(x = (index + 0.5) * width, y = height / 2),
                size = CGSize(width = width, height = height))
        }
        return filter.outputImage ?: image
    }
}

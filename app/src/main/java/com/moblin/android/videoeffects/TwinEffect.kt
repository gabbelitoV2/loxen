package com.moblin.android.videoeffects

import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.platform.coregraphics.CGAffineTransform
import com.moblin.android.platform.coregraphics.CGPoint
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.coreimage.CIFilter
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.metalpetal.MTILayer
import com.moblin.android.platform.metalpetal.MTIImage
import com.moblin.android.platform.metalpetal.MTIMultilayerCompositingFilter

class TwinEffect : VideoEffect() {
    private val filter = CIFilter.sourceOverCompositing()

    override fun execute(image: CIImage, info: VideoEffectInfo): CIImage {
        val size = image.extent.size
        val width = size.width / 2
        val height = size.height
        val centerImage = image.cropped(
            to = CGRect(
                x = width / 2,
                y = 0.0,
                width = width,
                height = height,
            ),
        )
        val leftImage = centerImage.transformed(
            by = CGAffineTransform(translationX = -width / 2, y = 0.0),
        )
        val rightImage = centerImage
            .transformed(by = CGAffineTransform(scaleX = -1.0, y = 1.0))
            .transformed(by = CGAffineTransform(translationX = 5 * width / 2, y = 0.0))
        filter.inputImage = rightImage
        filter.backgroundImage = leftImage
        return filter.outputImage ?: image
    }

    override fun executeMetalPetal(image: MTIImage, info: VideoEffectInfo): MTIImage {
        val size = image.extent.size
        val width = size.width / 2
        val height = size.height
        val centerRegion = CGRect(x = width / 2, y = 0.0, width = width, height = height)
        val layerSize = CGSize(width = width, height = height)
        val filter = MTIMultilayerCompositingFilter()
        filter.inputBackgroundImage = image
        filter.layers = listOf(
            MTILayer(
                content = image,
                contentRegion = centerRegion,
                position = CGPoint(x = width / 2, y = height / 2),
                size = layerSize,
            ),
            MTILayer(
                content = image,
                contentRegion = centerRegion,
                contentFlipOptions = MTILayer.FlipOptions.flipHorizontally,
                position = CGPoint(x = 3 * width / 2, y = height / 2),
                size = layerSize,
            ),
        )
        return filter.outputImage ?: image
    }
}

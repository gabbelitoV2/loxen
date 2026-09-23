package com.moblin.android.videoeffects

import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.platform.coregraphics.CGPoint
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.metalpetal.MTIImage
import com.moblin.android.platform.metalpetal.MTILayer
import com.moblin.android.platform.metalpetal.MTIMultilayerCompositingFilter

class FourThreeEffect : VideoEffect() {
    override fun execute(image: CIImage, info: VideoEffectInfo): CIImage {
        return image
            .cropped(to = CGRect(x = image.extent.width / 8,
                                 y = 0.0,
                                 width = 3 * image.extent.width / 4,
                                 height = image.extent.height))
            .composited(over = CIImage.black.cropped(to = image.extent))
    }

    override fun executeMetalPetal(image: MTIImage, info: VideoEffectInfo): MTIImage {
        val size = image.extent.size
        val barSize = CGSize(width = size.width / 8, height = size.height)
        val filter = MTIMultilayerCompositingFilter()
        filter.inputBackgroundImage = image
        filter.layers = listOf(
            MTILayer(content = MTIImage.black,
                     position = CGPoint(x = barSize.width / 2, y = size.height / 2),
                     size = barSize),
            MTILayer(content = MTIImage.black,
                     position = CGPoint(x = size.width - barSize.width / 2, y = size.height / 2),
                     size = barSize),
        )
        return filter.outputImage ?: image
    }
}

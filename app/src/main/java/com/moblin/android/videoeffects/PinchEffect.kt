package com.moblin.android.videoeffects

import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.platform.coregraphics.CGPoint
import com.moblin.android.platform.coreimage.CIFilter
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.metalpetal.MTIImage
import com.moblin.android.platform.metalpetal.MTIPinchDistortionFilter
import com.moblin.android.platform.simd.SIMD2
import kotlinx.coroutines.launch
import kotlin.math.min

class PinchEffect(scale: Float) : VideoEffect() {
    private val filterMetalPetal = MTIPinchDistortionFilter()
    private var scale: Float = scale

    fun setSettings(scale: Float) {
        processorPipelineQueue.launch {
            this@PinchEffect.scale = scale
        }
    }

    override fun execute(image: CIImage, info: VideoEffectInfo): CIImage {
        val filter = CIFilter.pinchDistortion()
        filter.inputImage = image
        filter.radius = (min(image.extent.width, image.extent.height) / 2).toFloat()
        filter.scale = scale
        filter.center = CGPoint(x = image.extent.width / 2, y = image.extent.height / 2)
        return filter.outputImage?.cropped(to = image.extent) ?: image
    }

    override fun executeMetalPetal(image: MTIImage, info: VideoEffectInfo): MTIImage {
        filterMetalPetal.inputImage = image
        filterMetalPetal.radius = (min(image.extent.width, image.extent.height) / 2).toFloat()
        filterMetalPetal.scale = scale
        filterMetalPetal.center = SIMD2(
            (image.extent.width / 2).toFloat(),
            (image.extent.height / 2).toFloat(),
        )
        return filterMetalPetal.outputImage ?: image
    }
}

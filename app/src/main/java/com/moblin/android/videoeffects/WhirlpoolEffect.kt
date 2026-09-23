package com.moblin.android.videoeffects

import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.platform.coregraphics.CGPoint
import com.moblin.android.platform.coreimage.CIFilter
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.metalpetal.MTIImage
import com.moblin.android.platform.metalpetal.MTITwirlDistortionFilter
import com.moblin.android.platform.simd.SIMD2
import kotlinx.coroutines.launch
import kotlin.math.min

class WhirlpoolEffect(angle: Float) : VideoEffect() {
    private val filterMetalPetal = MTITwirlDistortionFilter()
    private var angle: Float = angle

    fun setSettings(angle: Float) {
        processorPipelineQueue.launch {
            this@WhirlpoolEffect.angle = angle
        }
    }

    override fun execute(image: CIImage, info: VideoEffectInfo): CIImage {
        val filter = CIFilter.twirlDistortion()
        filter.inputImage = image
        filter.angle = angle
        filter.radius = (min(image.extent.width, image.extent.height) / 1.9).toFloat()
        filter.center = CGPoint(x = image.extent.width / 2, y = image.extent.height / 2)
        return filter.outputImage?.cropped(to = image.extent) ?: image
    }

    override fun executeMetalPetal(image: MTIImage, info: VideoEffectInfo): MTIImage {
        filterMetalPetal.inputImage = image
        filterMetalPetal.angle = angle
        filterMetalPetal.radius = (min(image.extent.width, image.extent.height) / 1.9).toFloat()
        filterMetalPetal.center = SIMD2((image.extent.width / 2).toFloat(), (image.extent.height / 2).toFloat())
        return filterMetalPetal.outputImage ?: image
    }
}

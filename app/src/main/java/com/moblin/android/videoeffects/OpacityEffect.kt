package com.moblin.android.videoeffects

import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.platform.coreimage.CIFilter
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.coreimage.CIVector
import com.moblin.android.platform.metalpetal.MTIImage
import com.moblin.android.platform.metalpetal.MTIOpacityFilter
import kotlinx.coroutines.launch

class OpacityEffect : VideoEffect() {
    private var opacity: Double = 1.0
    private val filterMetalPetal = MTIOpacityFilter()

    fun setOpacity(opacity: Double) {
        processorPipelineQueue.launch {
            this@OpacityEffect.opacity = opacity
        }
    }

    override fun execute(image: CIImage, info: VideoEffectInfo): CIImage {
        val filter = CIFilter.colorMatrix()
        filter.aVector = CIVector(x = 0.0, y = 0.0, z = 0.0, w = opacity)
        filter.inputImage = image
        return filter.outputImage ?: image
    }

    override fun executeMetalPetal(image: MTIImage, info: VideoEffectInfo): MTIImage {
        filterMetalPetal.inputImage = image
        filterMetalPetal.opacity = opacity.toFloat()
        return filterMetalPetal.outputImage ?: image
    }
}

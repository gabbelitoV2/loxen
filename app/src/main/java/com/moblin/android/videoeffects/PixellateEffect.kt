package com.moblin.android.videoeffects

import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.platform.coregraphics.CGPoint
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.coreimage.CIFilter
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.metalpetal.MTIImage
import com.moblin.android.platform.metalpetal.MTIPixellateFilter
import com.moblin.android.platform.simd.SIMD2
import kotlinx.coroutines.launch

fun pixellateCalcScale(size: CGSize, strength: Float): Float {
    val maximum = size.maximum().toFloat()
    val sizeInPixels = 20f * (maximum / 1920f) * (1f + 5f * strength)
    return maximum / (maximum / sizeInPixels).toInt()
}

class PixellateEffect(private var strength: Float) : VideoEffect() {
    private val filter = CIFilter.pixellate()
    private val filterMetalPetal = MTIPixellateFilter()

    fun setSettings(strength: Float) {
        processorPipelineQueue.launch {
            this@PixellateEffect.strength = strength
        }
    }

    override fun execute(image: CIImage, info: VideoEffectInfo): CIImage {
        filter.inputImage = image
        filter.center = CGPoint.zero
        filter.scale = pixellateCalcScale(size = image.extent.size, strength = strength)
        return filter.outputImage?.cropped(to = image.extent) ?: image
    }

    override fun executeMetalPetal(image: MTIImage, info: VideoEffectInfo): MTIImage {
        val scale = pixellateCalcScale(size = image.extent.size, strength = strength)
        filterMetalPetal.inputImage = image
        filterMetalPetal.scale = SIMD2(scale, scale)
        return filterMetalPetal.outputImage ?: image
    }
}

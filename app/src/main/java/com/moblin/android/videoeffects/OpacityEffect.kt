package com.moblin.android.videoeffects

import android.media.Image
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import kotlinx.coroutines.launch

private class MTIOpacityFilter {
    var inputImage: Image? = null
    var opacity: Float = 1.0f
    val outputImage: Image?
        get() = null
}

class OpacityEffect : VideoEffect() {
    @Volatile
    private var opacity: Double = 1.0
    private val filterMetalPetal = MTIOpacityFilter()

    fun setOpacity(opacity: Double) {
        processorPipelineQueue.launch {
            this@OpacityEffect.opacity = opacity
        }
    }

    override fun execute(image: Image, info: VideoEffectInfo): Image =
        TODO()
    override fun executeMetalPetal(image: Image, info: VideoEffectInfo): Image {
        filterMetalPetal.inputImage = image
        filterMetalPetal.opacity = opacity.toFloat()
        return filterMetalPetal.outputImage ?: image
    }
}

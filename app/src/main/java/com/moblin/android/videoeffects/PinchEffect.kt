package com.moblin.android.videoeffects

import android.media.Image
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import kotlinx.coroutines.launch

class PinchEffect(scale: Float) : VideoEffect() {
    private var scale: Float = scale

    fun setSettings(scale: Float) {
        processorPipelineQueue.launch {
            this@PinchEffect.scale = scale
        }
    }

    override fun execute(image: Image, info: VideoEffectInfo): Image {
        TODO()
    }

    override fun executeMetalPetal(image: Image, info: VideoEffectInfo): Image {
        TODO()
    }
}

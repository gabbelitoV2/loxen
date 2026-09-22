package com.moblin.android.videoeffects

import android.media.Image
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import kotlinx.coroutines.launch

class WhirlpoolEffect(private var angle: Float) : VideoEffect() {
    fun setSettings(angle: Float) {
        processorPipelineQueue.launch {
            this@WhirlpoolEffect.angle = angle
        }
    }

    override fun execute(image: Image, info: VideoEffectInfo): Image {
        TODO("OpenGL ES port")
    }

    override fun executeMetalPetal(image: Image, info: VideoEffectInfo): Image {
        TODO("OpenGL ES port")
    }
}

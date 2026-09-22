package com.moblin.android.videoeffects

import android.media.Image
import android.util.SizeF
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import kotlinx.coroutines.launch

fun pixellateCalcScale(size: SizeF, strength: Float): Float {
    val maximum = maxOf(size.width, size.height)
    val sizeInPixels = 20 * (maximum / 1920) * (1 + 5 * strength)
    return maximum / (maximum / sizeInPixels).toInt().toFloat()
}

class PixellateEffect(private var strength: Float) : VideoEffect() {
    fun setSettings(strength: Float) {
        processorPipelineQueue.launch {
            this@PixellateEffect.strength = strength
        }
    }

    override fun execute(image: Image, info: VideoEffectInfo): Image {
        TODO()
    }

    override fun executeMetalPetal(image: Image, info: VideoEffectInfo): Image {
        TODO()
    }
}

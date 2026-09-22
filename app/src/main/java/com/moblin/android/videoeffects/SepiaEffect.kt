package com.moblin.android.videoeffects

import android.media.Image
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo

private val colorMatrix = floatArrayOf(
    0.393f, 0.769f, 0.189f, 0.0f,
    0.349f, 0.686f, 0.168f, 0.0f,
    0.272f, 0.534f, 0.131f, 0.0f,
    0.0f, 0.0f, 0.0f, 1.0f
)

class SepiaEffect : VideoEffect() {
    private val intensity = 0.9f

    override fun execute(image: Image, info: VideoEffectInfo): Image {
        TODO()
    }

    override fun executeMetalPetal(image: Image, info: VideoEffectInfo): Image {
        TODO()
    }
}

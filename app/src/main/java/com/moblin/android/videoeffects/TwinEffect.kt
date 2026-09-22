package com.moblin.android.videoeffects

import android.media.Image
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo

class TwinEffect : VideoEffect() {
    private val filter: Any by lazy { TODO("OpenGL ES port") }

    override fun execute(image: Image, info: VideoEffectInfo): Image {
        TODO()
    }

    override fun executeMetalPetal(image: Image, info: VideoEffectInfo): Image {
        TODO()
    }
}

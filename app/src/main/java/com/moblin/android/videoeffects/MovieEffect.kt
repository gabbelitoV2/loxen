package com.moblin.android.videoeffects

import com.moblin.android.platform.video.CVPixelBuffer as Image
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo

class MovieEffect : VideoEffect() {
    override fun execute(image: Image, info: VideoEffectInfo): Image =
        TODO()
    override fun executeMetalPetal(image: Image, info: VideoEffectInfo): Image =
        TODO()
}

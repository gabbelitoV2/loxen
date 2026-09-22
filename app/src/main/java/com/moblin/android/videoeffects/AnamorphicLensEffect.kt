package com.moblin.android.videoeffects

import android.media.Image
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.various.settings.SettingsVideoEffectAnamorphicLens
import kotlinx.coroutines.launch

class AnamorphicLensEffect(settings: SettingsVideoEffectAnamorphicLens) : VideoEffect() {
    private var settings: SettingsVideoEffectAnamorphicLens = settings

    fun setSettings(settings: SettingsVideoEffectAnamorphicLens) {
        processorPipelineQueue.launch {
            this@AnamorphicLensEffect.settings = settings
        }
    }

    override fun executeEarly(image: Image, videoEffectInfo: VideoEffectInfo): Image {
        TODO("OpenGL ES port")
    }
}

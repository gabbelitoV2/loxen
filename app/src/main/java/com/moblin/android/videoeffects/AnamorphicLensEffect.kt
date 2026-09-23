package com.moblin.android.videoeffects

import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.platform.coregraphics.CGPoint
import com.moblin.android.platform.coreimage.CIFilter
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.various.settings.SettingsVideoEffectAnamorphicLens
import kotlinx.coroutines.launch

class AnamorphicLensEffect(private var settings: SettingsVideoEffectAnamorphicLens) : VideoEffect() {
    fun setSettings(settings: SettingsVideoEffectAnamorphicLens) {
        processorPipelineQueue.launch {
            this@AnamorphicLensEffect.settings = settings
        }
    }

    override fun executeEarly(image: CIImage, info: VideoEffectInfo): CIImage {
        val filter = CIFilter.stretchCrop()
        filter.inputImage = image
        filter.centerStretchAmount = 1f
        filter.size = CGPoint(x = image.extent.width * settings.scale, y = image.extent.height)
        return filter.outputImage ?: image
    }
}

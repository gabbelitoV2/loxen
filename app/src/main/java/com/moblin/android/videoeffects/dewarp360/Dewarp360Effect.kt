package com.moblin.android.videoeffects.dewarp360

import com.moblin.android.platform.video.CVPixelBuffer as Image
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import kotlin.math.PI
import kotlinx.coroutines.launch

sealed class Dewarp360EffectSettings {
    data class Direct(
        val pan: Float = 0f,
        val tilt: Float = 0f,
        val fieldOfView: Float = (PI / 2).toFloat()
    ) : Dewarp360EffectSettings()

    data class Animate(
        val speed: Float = 1f,
        val pan: Float = 0f,
        val tilt: Float = 0f,
        val fieldOfView: Float = (PI / 2).toFloat()
    ) : Dewarp360EffectSettings()
}

class Dewarp360Effect : VideoEffect() {
    private val filter = Dewarp360Filter()
    private var settings: Dewarp360EffectSettings = Dewarp360EffectSettings.Direct()
    private var currentPan: Float = 0f
    private var currentTilt: Float = 0f
    private var currentFieldOfView: Float = (PI / 2).toFloat()

    fun setSettings(settings: Dewarp360EffectSettings) {
        processorPipelineQueue.launch {
            applySettings(settings)
        }
    }

    override fun executeEarly(image: Image, info: VideoEffectInfo): Image {
        updateParameters()
        filter.inputImage = image
        filter.outputSize = SizeF(
            info.videoUnit.canvasSize.width.toFloat(),
            info.videoUnit.canvasSize.height.toFloat()
        )
        filter.pan = currentPan
        filter.tilt = currentTilt
        filter.fieldOfView = currentFieldOfView
        return filter.outputImage ?: image
    }

    private fun applySettings(settings: Dewarp360EffectSettings) {
        this.settings = settings
        when (settings) {
            is Dewarp360EffectSettings.Direct -> {
                currentPan = settings.pan
                currentTilt = settings.tilt
                currentFieldOfView = settings.fieldOfView
            }
            is Dewarp360EffectSettings.Animate -> Unit
        }
    }

    private fun updateParameters() {
        val settings = this.settings
        when (settings) {
            is Dewarp360EffectSettings.Direct -> Unit
            is Dewarp360EffectSettings.Animate -> {
                currentPan = settings.pan
                currentTilt = settings.tilt
                currentFieldOfView = settings.fieldOfView
            }
        }
    }
}

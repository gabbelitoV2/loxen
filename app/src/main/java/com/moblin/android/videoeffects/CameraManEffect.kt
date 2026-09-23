package com.moblin.android.videoeffects

import android.graphics.RectF
import com.moblin.android.platform.video.CVPixelBuffer as Image
import android.util.SizeF
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.launch

class CameraManEffect(moveVertically: Boolean, speed: Double, alwaysMove: Boolean) : VideoEffect() {
    private var startTime: Double? = null
    private val minScale: Double = 0.92
    private val xSpeed: Double = 0.27
    private val ySpeed: Double = 0.36
    private val zoomSpeed: Double = 0.33
    private var moveVertically: Boolean = moveVertically
    private var speed: Double = speed
    private var alwaysMove: Boolean = alwaysMove
    private var previousIsRising: Boolean = false
    private var previousScale: Double = 0.0
    private var isStill: Boolean = true

    fun setSettings(moveVertically: Boolean, speed: Double, alwaysMove: Boolean) {
        processorPipelineQueue.launch {
            this@CameraManEffect.moveVertically = moveVertically
            this@CameraManEffect.speed = speed
            this@CameraManEffect.alwaysMove = alwaysMove
        }
    }

    private fun calcCropRegion(size: SizeF, presentationTimeStamp: Double): RectF? {
        if (startTime == null) {
            startTime = presentationTimeStamp
        }
        val elapsed = presentationTimeStamp - startTime!!
        val scale = minScale + (1 - minScale) * (0.5 + 0.5 * cos(elapsed * zoomSpeed * speed))
        val isRising = scale - previousScale > 0
        if (previousIsRising && !isRising) {
            isStill = !isStill
        }
        previousScale = scale
        previousIsRising = isRising
        if (isStill && !alwaysMove) {
            return null
        }
        val cropWidth = size.width.toDouble() * scale
        val cropHeight = size.height.toDouble() * scale
        val maxOffsetX = size.width.toDouble() - cropWidth
        val maxOffsetY = size.height.toDouble() - cropHeight
        val cropX = maxOffsetX * (0.5 + 0.5 * sin(elapsed * xSpeed * speed))
        val cropY = maxOffsetY * (0.5 + if (moveVertically) 0.5 * cos(elapsed * ySpeed * speed) else 0.0)
        return RectF(
            cropX.toFloat(),
            cropY.toFloat(),
            (cropX + cropWidth).toFloat(),
            (cropY + cropHeight).toFloat(),
        )
    }

    override fun execute(image: Image, info: VideoEffectInfo): Image {
        TODO()
    }

    override fun executeMetalPetal(image: Image, info: VideoEffectInfo): Image {
        TODO()
    }
}

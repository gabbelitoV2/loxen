package com.moblin.android.videoeffects

import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.platform.coregraphics.CGAffineTransform
import com.moblin.android.platform.coregraphics.CGPoint
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.metalpetal.MTILayer
import com.moblin.android.platform.metalpetal.MTIMultilayerCompositingFilter
import com.moblin.android.platform.metalpetal.MTIImage
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.launch

class CameraManEffect(
    private var moveVertically: Boolean,
    private var speed: Double,
    private var alwaysMove: Boolean,
) : VideoEffect() {
    private var startTime: Double? = null
    private val minScale: Double = 0.92
    private val xSpeed: Double = 0.27
    private val ySpeed: Double = 0.36
    private val zoomSpeed: Double = 0.33
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

    private fun calcCropRegion(size: CGSize, presentationTimeStamp: Double): CGRect? {
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
        val cropWidth = size.width * scale
        val cropHeight = size.height * scale
        val maxOffsetX = size.width - cropWidth
        val maxOffsetY = size.height - cropHeight
        val cropX = maxOffsetX * (0.5 + 0.5 * sin(elapsed * xSpeed * speed))
        val cropY = maxOffsetY * (0.5 + (if (moveVertically) 0.5 * cos(elapsed * ySpeed * speed) else 0.0))
        return CGRect(x = cropX, y = cropY, width = cropWidth, height = cropHeight)
    }

    override fun execute(image: CIImage, info: VideoEffectInfo): CIImage {
        val cropRect = calcCropRegion(image.extent.size, info.presentationTimeStamp / 1_000_000.0) ?: return image
        val scaleX = image.extent.width / cropRect.width
        val scaleY = image.extent.height / cropRect.height
        return image
            .cropped(to = cropRect)
            .transformed(by = CGAffineTransform(translationX = -cropRect.minX, y = -cropRect.minY))
            .transformed(
                by = CGAffineTransform(scaleX = scaleX, y = scaleY),
                highQualityDownsample = highQualityDownsampling,
            )
    }

    override fun executeMetalPetal(image: MTIImage, info: VideoEffectInfo): MTIImage {
        val size = image.extent.size
        val cropRect = calcCropRegion(size, info.presentationTimeStamp / 1_000_000.0) ?: return image
        val contentRegion = CGRect(
            x = cropRect.minX,
            y = size.height - cropRect.maxY,
            width = cropRect.width,
            height = cropRect.height,
        )
        val filter = MTIMultilayerCompositingFilter()
        filter.inputBackgroundImage = image
        filter.layers = listOf(
            MTILayer(
                content = image,
                contentRegion = contentRegion,
                position = CGPoint(x = size.width / 2, y = size.height / 2),
                size = size,
            ),
        )
        return filter.outputImage ?: image
    }
}

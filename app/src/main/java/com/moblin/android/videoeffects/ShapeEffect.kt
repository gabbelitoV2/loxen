package com.moblin.android.videoeffects

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import kotlinx.coroutines.launch

data class MTIColor(
    val red: Float,
    val green: Float,
    val blue: Float,
    val alpha: Float = 1.0f,
) {
    companion object {
        val black: MTIColor = MTIColor(0.0f, 0.0f, 0.0f, 1.0f)
    }
}

object MTILayer {
    enum class FlipOptions {
        flipHorizontally,
        flipVertically,
    }
}

private fun shapeBorderWidthPixels(borderWidth: Double, size: Size): Double {
    return 0.025 * borderWidth * minOf(size.width, size.height).toDouble()
}

private fun shapeCornerRadiusPixels(cornerRadius: Float, size: Size): Float {
    return minOf(size.width, size.height) / 2 * cornerRadius
}

data class ShapeEffectSettings(
    var cornerRadius: Float = 0.0f,
    var borderWidth: Double = 1.0,
    var borderColor: MTIColor = MTIColor.black,
    var cropEnabled: Boolean = false,
    var cropX: Double = 0.25,
    var cropY: Double = 0.0,
    var cropWidth: Double = 0.5,
    var cropHeight: Double = 1.0,
) {
    fun borderWidthAndScale(image: Rect): Triple<Double, Double, Double> {
        val size = Size(image.right - image.left, image.bottom - image.top)
        val borderWidth = shapeBorderWidthPixels(this.borderWidth, size)
        val scaleX = (size.width + 2 * borderWidth) / size.width
        val scaleY = (size.height + 2 * borderWidth) / size.height
        return Triple(borderWidth, scaleX, scaleY)
    }
}

data class MetalPetalWidgetShape(
    var contentRegion: Rect,
    var cornerRadius: Float = 0.0f,
    var borderWidth: Double = 0.0,
    var borderColor: MTIColor = MTIColor.black,
    var rotation: Double = 0.0,
) {
    fun borderWidthPixels(size: Size): Double {
        return shapeBorderWidthPixels(borderWidth, size)
    }

    fun cornerRadius(size: Size): Float {
        return shapeCornerRadiusPixels(cornerRadius, size)
    }

    fun rotated(size: Size): Size {
        return if (isQuarterTurn()) {
            Size(size.height, size.width)
        } else {
            size
        }
    }

    fun rotationRadians(): Float {
        return (rotation * Math.PI / 180).toFloat()
    }

    fun mirrorFlipOptions(): MTILayer.FlipOptions {
        return if (isQuarterTurn()) {
            MTILayer.FlipOptions.flipVertically
        } else {
            MTILayer.FlipOptions.flipHorizontally
        }
    }

    private fun isQuarterTurn(): Boolean {
        return rotation == 90.0 || rotation == 270.0
    }
}

private class MaskImage {
    var extent: Rect? = null
    var cornerRadius: Float? = null
    var image: EffectImage? = null

    fun get(extent: Rect, settings: ShapeEffectSettings): EffectImage? {
        if (extent != this.extent) {
            return null
        }
        if (settings.cornerRadius != cornerRadius) {
            return null
        }
        return image
    }

    fun set(extent: Rect, settings: ShapeEffectSettings, image: EffectImage?) {
        this.extent = extent
        cornerRadius = settings.cornerRadius
        this.image = image
    }
}

class ShapeEffect : VideoEffect() {
    private var settings: ShapeEffectSettings = ShapeEffectSettings()
    private var cachedMask = MaskImage()
    private var cachedBorderMask = MaskImage()

    fun setSettings(settings: ShapeEffectSettings) {
        processorPipelineQueue.launch {
            this@ShapeEffect.settings = settings
        }
    }

    private fun makeMaskImage(extent: Rect,
                              settings: ShapeEffectSettings,
                              cache: MaskImage): EffectImage?
    {
        cache.get(extent, settings)?.let { image ->
            return image
        }
        TODO("OpenGL ES port")
    }

    private fun makeSharpCornersImage(image: EffectImage, settings: ShapeEffectSettings): EffectImage {
        if (settings.borderWidth == 0.0) {
            return image
        }
        TODO("OpenGL ES port")
    }

    private fun makeRoundedCornersImage(image: EffectImage, settings: ShapeEffectSettings): EffectImage {
        TODO("OpenGL ES port")
    }

    private fun crop(image: EffectImage): EffectImage {
        TODO("OpenGL ES port")
    }

    override fun executeEarly(image: EffectImage, info: VideoEffectInfo): EffectImage {
        return if (settings.cropEnabled) {
            crop(image)
        } else {
            image
        }
    }

    override fun execute(image: EffectImage, info: VideoEffectInfo): EffectImage {
        return if (settings.cornerRadius == 0.0f) {
            makeSharpCornersImage(image, settings)
        } else {
            makeRoundedCornersImage(image, settings)
        }
    }

    override fun modifyMetalPetalWidgetShape(shape: MetalPetalWidgetShape) {
        if (settings.cropEnabled) {
            val region = shape.contentRegion
            val regionWidth = region.right - region.left
            val regionHeight = region.bottom - region.top
            val x = region.left + (settings.cropX * regionWidth).toFloat()
            val y = region.top + (settings.cropY * regionHeight).toFloat()
            val width = (settings.cropWidth * regionWidth).toFloat()
            val height = (settings.cropHeight * regionHeight).toFloat()
            shape.contentRegion = Rect(x, y, x + width, y + height)
        }
        shape.cornerRadius = settings.cornerRadius
        shape.borderWidth = settings.borderWidth
        shape.borderColor = MTIColor(
            red = settings.borderColor.red,
            green = settings.borderColor.green,
            blue = settings.borderColor.blue,
            alpha = settings.borderColor.alpha,
        )
    }
}

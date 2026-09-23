package com.moblin.android.videoeffects

import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.coreimage.CIColor
import com.moblin.android.platform.coreimage.CIFilter
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.metalpetal.MTIColor
import com.moblin.android.platform.metalpetal.MTICornerRadius
import com.moblin.android.platform.metalpetal.MTILayer
import kotlinx.coroutines.launch
import kotlin.math.PI

private fun shapeBorderWidthPixels(borderWidth: Double, size: CGSize): Double {
    return 0.025 * borderWidth * minOf(size.height, size.width)
}

private fun shapeCornerRadiusPixels(cornerRadius: Float, size: CGSize): Float {
    return minOf(size.height, size.width).toFloat() / 2 * cornerRadius
}

private fun shapeSameExtent(a: CGRect, b: CGRect): Boolean {
    return a.origin.x == b.origin.x &&
        a.origin.y == b.origin.y &&
        a.size.width == b.size.width &&
        a.size.height == b.size.height
}

data class ShapeEffectSettings(
    val cornerRadius: Float = 0f,
    val borderWidth: Double = 1.0,
    val borderColor: CIColor = CIColor.black,
    val cropEnabled: Boolean = false,
    val cropX: Double = 0.25,
    val cropY: Double = 0.0,
    val cropWidth: Double = 0.5,
    val cropHeight: Double = 1.0,
) {
    fun borderWidthAndScale(image: CGRect): Triple<Double, Double, Double> {
        val borderWidth = shapeBorderWidthPixels(this.borderWidth, image.size)
        val scaleX = (image.width + 2 * borderWidth) / image.width
        val scaleY = (image.height + 2 * borderWidth) / image.height
        return Triple(borderWidth, scaleX, scaleY)
    }
}

data class MetalPetalWidgetShape(
    var contentRegion: CGRect,
    var cornerRadius: Float = 0f,
    var borderWidth: Double = 0.0,
    var borderColor: MTIColor = MTIColor.black,
    var rotation: Double = 0.0,
) {
    fun borderWidthPixels(size: CGSize): Double {
        return shapeBorderWidthPixels(borderWidth, size)
    }

    fun cornerRadius(size: CGSize): MTICornerRadius {
        return MTICornerRadius(shapeCornerRadiusPixels(cornerRadius, size))
    }

    fun rotated(size: CGSize): CGSize {
        return if (isQuarterTurn()) {
            CGSize(width = size.height, height = size.width)
        } else {
            size
        }
    }

    fun rotationRadians(): Float {
        return (rotation * PI / 180.0).toFloat()
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

private data class MaskImage(
    var extent: CGRect? = null,
    var cornerRadius: Float? = null,
    var image: CIImage? = null,
) {
    fun get(extent: CGRect, settings: ShapeEffectSettings): CIImage? {
        val cachedExtent = this.extent
        if (cachedExtent == null || !shapeSameExtent(cachedExtent, extent)) {
            return null
        }
        if (cornerRadius != settings.cornerRadius) {
            return null
        }
        return image
    }

    fun set(extent: CGRect, settings: ShapeEffectSettings, image: CIImage?) {
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

    private fun makeMaskImage(
        extent: CGRect,
        settings: ShapeEffectSettings,
        cache: MaskImage,
    ): CIImage? {
        cache.get(extent = extent, settings = settings)?.let { return it }
        val roundedRectangleGenerator = CIFilter.roundedRectangleGenerator()
        roundedRectangleGenerator.color = CIColor.green
        val maskExtent = extent.copy(
            x = extent.origin.x + 1,
            y = extent.origin.y + 1,
            width = extent.size.width - 2,
            height = extent.size.height - 2,
        )
        roundedRectangleGenerator.extent = maskExtent
        var radiusPixels = minOf(extent.height, extent.width).toFloat()
        radiusPixels /= 2
        radiusPixels *= settings.cornerRadius
        roundedRectangleGenerator.radius = radiusPixels
        cache.set(extent = extent, settings = settings, image = roundedRectangleGenerator.outputImage)
        return cache.get(extent = extent, settings = settings)
    }

    private fun makeSharpCornersImage(image: CIImage, settings: ShapeEffectSettings): CIImage {
        if (settings.borderWidth == 0.0) {
            return image
        } else {
            val (borderWidth, scaleX, scaleY) = settings.borderWidthAndScale(image.extent)
            val borderImage = CIImage(color = settings.borderColor)
                .cropped(to = image.extent)
                .scaled(x = scaleX, y = scaleY)
                .translated(x = -borderWidth, y = -borderWidth)
            return image.composited(over = borderImage)
        }
    }

    private fun makeRoundedCornersImage(image: CIImage, settings: ShapeEffectSettings): CIImage {
        if (settings.borderWidth == 0.0) {
            val roundedCornersBlender = CIFilter.blendWithMask()
            roundedCornersBlender.inputImage = image
            roundedCornersBlender.maskImage = makeMaskImage(image.extent, settings, cachedMask)
            return roundedCornersBlender.outputImage ?: image
        } else {
            val (borderWidth, scaleX, scaleY) = settings.borderWidthAndScale(image.extent)
            val borderImage = CIImage(color = settings.borderColor)
                .cropped(to = image.extent)
                .scaled(x = scaleX, y = scaleY)
                .translated(x = -borderWidth, y = -borderWidth)
            val roundedCornersBlender = CIFilter.blendWithMask()
            roundedCornersBlender.inputImage = borderImage
            roundedCornersBlender.maskImage =
                makeMaskImage(borderImage.extent, settings, cachedBorderMask)
            val roundedBorderImage = roundedCornersBlender.outputImage ?: return image
            roundedCornersBlender.inputImage = image
            roundedCornersBlender.maskImage = makeMaskImage(image.extent, settings, cachedMask)
            val widgetImage = roundedCornersBlender.outputImage ?: return image
            return widgetImage.composited(over = roundedBorderImage)
        }
    }

    private fun crop(image: CIImage): CIImage {
        val cropX = toPixels(100 * settings.cropX, image.extent.width)
        val cropY = toPixels(100 * settings.cropY, image.extent.height)
        val cropWidth = toPixels(100 * settings.cropWidth, image.extent.width)
        val cropHeight = toPixels(100 * settings.cropHeight, image.extent.height)
        return image
            .cropped(
                to = CGRect(
                    x = cropX,
                    y = image.extent.height - cropY - cropHeight,
                    width = cropWidth,
                    height = cropHeight,
                )
            )
            .translated(x = -cropX, y = -(image.extent.height - cropY - cropHeight))
    }

    override fun executeEarly(image: CIImage, info: VideoEffectInfo): CIImage {
        return if (settings.cropEnabled) {
            crop(image)
        } else {
            image
        }
    }

    override fun execute(image: CIImage, info: VideoEffectInfo): CIImage {
        return if (settings.cornerRadius == 0f) {
            makeSharpCornersImage(image, settings)
        } else {
            makeRoundedCornersImage(image, settings)
        }
    }

    override fun modifyMetalPetalWidgetShape(shape: MetalPetalWidgetShape) {
        if (settings.cropEnabled) {
            val region = shape.contentRegion
            shape.contentRegion = CGRect(
                x = region.minX + settings.cropX * region.width,
                y = region.minY + settings.cropY * region.height,
                width = settings.cropWidth * region.width,
                height = settings.cropHeight * region.height,
            )
        }
        shape.cornerRadius = settings.cornerRadius
        shape.borderWidth = settings.borderWidth
        shape.borderColor = MTIColor(
            red = settings.borderColor.red.toFloat(),
            green = settings.borderColor.green.toFloat(),
            blue = settings.borderColor.blue.toFloat(),
            alpha = settings.borderColor.alpha.toFloat(),
        )
    }
}

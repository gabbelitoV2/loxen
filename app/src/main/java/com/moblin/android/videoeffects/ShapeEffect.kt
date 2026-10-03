package com.moblin.android.videoeffects

import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.platform.coregraphics.CGPoint
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.coreimage.CIColor
import com.moblin.android.platform.coreimage.CIFilter
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.metalpetal.MTILayer
import com.moblin.android.various.settings.SettingsWidgetLayout
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

    fun cropRegion(region: CGRect): CGRect {
        return CGRect(
            x = region.minX + cropX * region.width,
            y = region.minY + cropY * region.height,
            width = cropWidth * region.width,
            height = cropHeight * region.height,
        )
    }
}

data class WidgetShapePlacement(
    val scale: Double,
    val size: CGSize,
    val borderWidth: Double,
    val borderSize: CGSize,
    val center: CGPoint,
)

data class WidgetShape(
    var contentRegion: CGRect,
    var cornerRadius: Float = 0f,
    var borderWidth: Double = 0.0,
    var borderColor: CIColor = CIColor.black,
    var rotation: Double = 0.0,
) {
    fun apply(settings: ShapeEffectSettings) {
        if (settings.cropEnabled) {
            contentRegion = settings.cropRegion(contentRegion)
        }
        cornerRadius = settings.cornerRadius
        borderWidth = settings.borderWidth
        borderColor = settings.borderColor
    }

    fun placement(layout: SettingsWidgetLayout, streamSize: CGSize, resize: Boolean = true): WidgetShapePlacement {
        val scale =
            if (resize) {
                layoutScale(layout = layout, size = rotated(contentRegion.size), streamSize = streamSize)
            } else {
                1.0
            }
        val size = CGSize(width = contentRegion.width * scale, height = contentRegion.height * scale)
        val borderWidth = shapeBorderWidthPixels(this.borderWidth, size)
        val borderSize = CGSize(width = size.width + 2 * borderWidth, height = size.height + 2 * borderWidth)
        return WidgetShapePlacement(
            scale = scale,
            size = size,
            borderWidth = borderWidth,
            borderSize = borderSize,
            center = layoutCenter(layout = layout, size = rotated(borderSize), streamSize = streamSize),
        )
    }

    fun cornerRadiusPixels(size: CGSize): Float {
        return shapeCornerRadiusPixels(cornerRadius, size)
    }

    fun rotated(size: CGSize): CGSize {
        return if (isQuarterTurn()) {
            CGSize(width = size.height, height = size.width)
        } else {
            size
        }
    }

    fun rotationRadians(): Double {
        return rotation * PI / 180.0
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
        roundedRectangleGenerator.radius = shapeCornerRadiusPixels(settings.cornerRadius, extent.size)
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
        val region = settings.cropRegion(CGRect(origin = CGPoint.zero, size = image.extent.size))
        val cropY = image.extent.height - region.maxY
        return image
            .cropped(to = CGRect(x = region.minX, y = cropY, width = region.width, height = region.height))
            .translated(x = -region.minX, y = -cropY)
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

    override fun modifyWidgetShape(shape: WidgetShape) {
        shape.apply(settings)
    }
}

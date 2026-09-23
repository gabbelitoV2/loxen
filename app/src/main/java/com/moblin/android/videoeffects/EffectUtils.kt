package com.moblin.android.videoeffects

import android.graphics.Bitmap
import com.moblin.android.platform.coregraphics.CGAffineTransform
import com.moblin.android.platform.coregraphics.CGPoint
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.metalpetal.MTIAlphaType
import com.moblin.android.platform.metalpetal.MTIImage
import com.moblin.android.platform.metalpetal.MTILayer
import com.moblin.android.platform.metalpetal.MTIMultilayerCompositingFilter
import com.moblin.android.platform.video.CVPixelBuffer
import com.moblin.android.various.settings.SettingsWidgetLayout
import com.moblin.android.platform.coregraphics.CGRect

@Volatile
var highQualityDownsampling = false

fun toPixels(percentage: Double, total: Double): Double = (percentage * total) / 100

interface EffectImage {
    fun getCiImage(): CIImage
    fun getMetalPetalImage(): MTIImage
}

class EffectImageCgImage(image: Bitmap) : EffectImage {
    private val source: Bitmap = image
    private var ciImage: CIImage? = null
    private var metalPetalImage: MTIImage? = null

    override fun getCiImage(): CIImage {
        val cached = ciImage
        if (cached != null) {
            return cached
        }
        val created = CIImage(cgImage = source)
        ciImage = created
        return created
    }

    override fun getMetalPetalImage(): MTIImage {
        val cached = metalPetalImage
        if (cached != null) {
            return cached
        }
        val created = MTIImage(cgImage = source)
        metalPetalImage = created
        return created
    }
}

class EffectImageCiImage(image: CIImage, private val isOpaque: Boolean) : EffectImage {
    private val source: CIImage = image
    private var metalPetalImage: MTIImage? = null

    override fun getCiImage(): CIImage = source

    override fun getMetalPetalImage(): MTIImage {
        val cached = metalPetalImage
        if (cached != null) {
            return cached
        }
        val created = MTIImage(ciImage = source, isOpaque = isOpaque)
        metalPetalImage = created
        return created
    }
}

class EffectImagePixelBuffer(pixelBuffer: CVPixelBuffer) : EffectImage {
    private val source: CVPixelBuffer = pixelBuffer
    private var ciImage: CIImage? = null
    private var metalPetalImage: MTIImage? = null

    override fun getCiImage(): CIImage {
        val cached = ciImage
        if (cached != null) {
            return cached
        }
        val created = CIImage(cvPixelBuffer = source)
        ciImage = created
        return created
    }

    override fun getMetalPetalImage(): MTIImage {
        val cached = metalPetalImage
        if (cached != null) {
            return cached
        }
        val created = MTIImage(cvPixelBuffer = source, alphaType = MTIAlphaType.premultiplied)
        metalPetalImage = created
        return created
    }
}

fun CIImage.toEffectImage(isOpaque: Boolean): EffectImageCiImage = EffectImageCiImage(image = this, isOpaque = isOpaque)

fun Bitmap.toEffectImage(): EffectImageCgImage = EffectImageCgImage(image = this)

fun layoutPosition(layout: SettingsWidgetLayout, size: CGSize, streamSize: CGSize): CGPoint {
    var x: Double
    var y: Double
    if (layout.alignment.isHorizontalCenter()) {
        x = (streamSize.width - size.width) / 2
    } else if (layout.alignment.isLeft()) {
        x = toPixels(layout.x, streamSize.width)
    } else {
        x = streamSize.width - toPixels(layout.x, streamSize.width) - size.width
    }
    if (layout.alignment.isVerticalCenter()) {
        y = (streamSize.height - size.height) / 2
    } else if (layout.alignment.isTop()) {
        y = toPixels(layout.y, streamSize.height)
    } else {
        y = streamSize.height - toPixels(layout.y, streamSize.height) - size.height
    }
    return CGPoint(x = x, y = y)
}

fun metalPetalLayerPosition(layout: SettingsWidgetLayout, size: CGSize, streamSize: CGSize): CGPoint {
    val position = layoutPosition(layout = layout, size = size, streamSize = streamSize)
    return CGPoint(x = position.x + size.width / 2, y = position.y + size.height / 2)
}

fun MTIImage.moveComposited(layout: SettingsWidgetLayout, backgroundImage: MTIImage, contentRegion: CGRect? = null): MTIImage {
    val region = contentRegion ?: extent
    return composited(layout = layout,
        size = region.size,
        mirror = false,
        backgroundImage = backgroundImage,
        shape = MetalPetalWidgetShape(contentRegion = region))
}

fun MTIImage.positionComposited(position: CGPoint, backgroundImage: MTIImage, size: CGSize? = null): MTIImage {
    val filter = MTIMultilayerCompositingFilter()
    filter.inputBackgroundImage = backgroundImage
    filter.layers = listOf(
        MTILayer(content = this, position = position, size = size),
    )
    return filter.outputImage ?: backgroundImage
}

fun MTIImage.resizeMirrorMoveComposited(layout: SettingsWidgetLayout, mirror: Boolean, backgroundImage: MTIImage, shape: MetalPetalWidgetShape): MTIImage {
    val backgroundImageSize = backgroundImage.extent.size
    val rotatedSize = shape.rotated(size = shape.contentRegion.size)
    val scaleX = toPixels(layout.size, backgroundImageSize.width) / rotatedSize.width
    val scaleY = toPixels(layout.size, backgroundImageSize.height) / rotatedSize.height
    val scale = minOf(scaleX, scaleY)
    val size = CGSize(width = shape.contentRegion.width * scale,
        height = shape.contentRegion.height * scale)
    return composited(layout = layout, size = size, mirror = mirror, backgroundImage = backgroundImage, shape = shape)
}

private fun MTIImage.composited(layout: SettingsWidgetLayout, size: CGSize, mirror: Boolean, backgroundImage: MTIImage, shape: MetalPetalWidgetShape): MTIImage {
    val borderWidth = shape.borderWidthPixels(size)
    val borderSize = CGSize(width = size.width + 2 * borderWidth,
        height = size.height + 2 * borderWidth)
    val position = metalPetalLayerPosition(layout = layout,
        size = shape.rotated(size = borderSize),
        streamSize = backgroundImage.extent.size)
    val rotation = shape.rotationRadians()
    val layers = mutableListOf<MTILayer>()
    if (borderWidth > 0) {
        layers.add(MTILayer(content = MTIImage.white,
            position = position,
            size = borderSize,
            rotation = rotation,
            cornerRadius = shape.cornerRadius(borderSize),
            tintColor = shape.borderColor))
    }
    layers.add(MTILayer(content = this,
        contentRegion = shape.contentRegion,
        contentFlipOptions = if (mirror) shape.mirrorFlipOptions() else MTILayer.FlipOptions.donotFlip,
        position = position,
        size = size,
        rotation = rotation,
        cornerRadius = shape.cornerRadius(size)))
    val filter = MTIMultilayerCompositingFilter()
    filter.inputBackgroundImage = backgroundImage
    filter.layers = layers
    return filter.outputImage ?: backgroundImage
}

fun CIImage.resizeMirror(layout: SettingsWidgetLayout, streamSize: CGSize, mirror: Boolean, resize: Boolean = true): CIImage {
    if (!resize) {
        return this
    }
    var scaleX = toPixels(layout.size, streamSize.width) / extent.size.width
    var scaleY = toPixels(layout.size, streamSize.height) / extent.size.height
    val scale = minOf(scaleX, scaleY)
    if (mirror) {
        scaleX = -scale
    } else {
        scaleX = scale
    }
    scaleY = scale
    val scaledImage = scaled(x = scaleX, y = scaleY)
    if (mirror) {
        return scaledImage.translated(x = scaledImage.extent.width, y = 0.0)
    } else {
        return scaledImage
    }
}

fun CIImage.move(layout: SettingsWidgetLayout, streamSize: CGSize): CIImage {
    var x: Double
    var y: Double
    if (layout.alignment.isHorizontalCenter()) {
        x = (streamSize.width - extent.width) / 2 - extent.minX
    } else if (layout.alignment.isLeft()) {
        x = toPixels(layout.x, streamSize.width) - extent.minX
    } else {
        x = streamSize.width - toPixels(layout.x, streamSize.width) - extent.width - extent.minX
        if (x != 0.0) {
            x += 1
        }
    }
    if (layout.alignment.isVerticalCenter()) {
        y = (streamSize.height - extent.height) / 2 - extent.minY
    } else if (layout.alignment.isTop()) {
        y = streamSize.height - toPixels(layout.y, streamSize.height) - extent.height - extent.minY
        if (y != 0.0) {
            y += 1
        }
    } else {
        y = toPixels(layout.y, streamSize.height) - extent.minY
    }
    return translated(x = x, y = y)
}

fun CIImage.translated(x: Double, y: Double): CIImage = transformed(by = CGAffineTransform(translationX = x, y = y))

fun CIImage.scaled(x: Double, y: Double): CIImage = transformed(by = CGAffineTransform(scaleX = x, y = y), highQualityDownsample = highQualityDownsampling)

fun CIImage.scaledTo(size: CGSize): CIImage {
    val scaleX = size.width / extent.width
    val scaleY = size.height / extent.height
    val scale = minOf(scaleX, scaleY)
    return scaled(x = scale, y = scale)
}

fun CIImage.scaledToFill(size: CGSize): CIImage {
    val scaleX = size.width / extent.width
    val scaleY = size.height / extent.height
    val scale = maxOf(scaleX, scaleY)
    return scaled(x = scale, y = scale)
}

fun CIImage.centered(size: CGSize): CIImage {
    val targetCenterX = size.width / 2
    val targetCenterY = size.height / 2
    val currentCenterX = extent.width / 2
    val currentCenterY = extent.height / 2
    val x = targetCenterX - currentCenterX
    val y = targetCenterY - currentCenterY
    return translated(x = x, y = y)
}

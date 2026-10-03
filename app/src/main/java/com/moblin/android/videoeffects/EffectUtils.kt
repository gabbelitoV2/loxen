package com.moblin.android.videoeffects

import android.graphics.Bitmap
import com.moblin.android.platform.coregraphics.CGAffineTransform
import com.moblin.android.platform.coregraphics.CGPoint
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.metalpetal.MTIAlphaType
import com.moblin.android.platform.metalpetal.MTIColor
import com.moblin.android.platform.metalpetal.MTICornerRadius
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

fun layoutScale(layout: SettingsWidgetLayout, size: CGSize, streamSize: CGSize): Double =
    minOf(toPixels(layout.size, streamSize.width) / size.width,
        toPixels(layout.size, streamSize.height) / size.height)

private fun layoutOffset(offset: Double,
                         size: Double,
                         streamSize: Double,
                         isCenter: Boolean,
                         isNear: Boolean): Double {
    return if (isCenter) {
        (streamSize - size) / 2
    } else if (isNear) {
        toPixels(offset, streamSize)
    } else {
        streamSize - toPixels(offset, streamSize) - size
    }
}

fun layoutPosition(layout: SettingsWidgetLayout, size: CGSize, streamSize: CGSize): CGPoint {
    val alignment = layout.alignment
    return CGPoint(x = layoutOffset(layout.x,
            size.width,
            streamSize.width,
            alignment.isHorizontalCenter(),
            alignment.isLeft()),
        y = layoutOffset(layout.y,
            size.height,
            streamSize.height,
            alignment.isVerticalCenter(),
            alignment.isTop()))
}

fun layoutCenter(layout: SettingsWidgetLayout, size: CGSize, streamSize: CGSize): CGPoint {
    val position = layoutPosition(layout = layout, size = size, streamSize = streamSize)
    return CGPoint(x = position.x + size.width / 2, y = position.y + size.height / 2)
}

fun MTIImage.moveComposited(layout: SettingsWidgetLayout, backgroundImage: MTIImage, contentRegion: CGRect? = null): MTIImage {
    return composited(layout = layout,
        mirror = false,
        backgroundImage = backgroundImage,
        shape = WidgetShape(contentRegion = contentRegion ?: extent),
        resize = false)
}

fun MTIImage.positionComposited(position: CGPoint, backgroundImage: MTIImage, size: CGSize? = null): MTIImage {
    val filter = MTIMultilayerCompositingFilter()
    filter.inputBackgroundImage = backgroundImage
    filter.layers = listOf(
        MTILayer(content = this, position = position, size = size),
    )
    return filter.outputImage ?: backgroundImage
}

fun MTIImage.resizeMirrorMoveComposited(layout: SettingsWidgetLayout, mirror: Boolean, backgroundImage: MTIImage, shape: WidgetShape): MTIImage {
    return composited(layout = layout, mirror = mirror, backgroundImage = backgroundImage, shape = shape, resize = true)
}

private fun MTIImage.composited(layout: SettingsWidgetLayout,
                               mirror: Boolean,
                               backgroundImage: MTIImage,
                               shape: WidgetShape,
                               resize: Boolean): MTIImage {
    val placement = shape.placement(layout, backgroundImage.extent.size, resize)
    val rotation = shape.rotationRadians().toFloat()
    val layers = mutableListOf<MTILayer>()
    if (placement.borderWidth > 0) {
        layers.add(MTILayer(content = MTIImage.white,
            position = placement.center,
            size = placement.borderSize,
            rotation = rotation,
            cornerRadius = MTICornerRadius(shape.cornerRadiusPixels(placement.borderSize)),
            tintColor = MTIColor(red = shape.borderColor.red.toFloat(),
                green = shape.borderColor.green.toFloat(),
                blue = shape.borderColor.blue.toFloat(),
                alpha = shape.borderColor.alpha.toFloat())))
    }
    layers.add(MTILayer(content = this,
        contentRegion = shape.contentRegion,
        contentFlipOptions = if (mirror) shape.mirrorFlipOptions() else MTILayer.FlipOptions.donotFlip,
        position = placement.center,
        size = placement.size,
        rotation = rotation,
        cornerRadius = MTICornerRadius(shape.cornerRadiusPixels(placement.size))))
    val filter = MTIMultilayerCompositingFilter()
    filter.inputBackgroundImage = backgroundImage
    filter.layers = layers
    return filter.outputImage ?: backgroundImage
}

fun CIImage.resizeMirror(layout: SettingsWidgetLayout, streamSize: CGSize, mirror: Boolean, resize: Boolean = true): CIImage {
    if (!resize) {
        return this
    }
    val scale = layoutScale(layout = layout, size = extent.size, streamSize = streamSize)
    val scaledImage = scaled(x = if (mirror) -scale else scale, y = scale)
    if (mirror) {
        return scaledImage.translated(x = scaledImage.extent.width, y = 0.0)
    } else {
        return scaledImage
    }
}

fun CIImage.move(layout: SettingsWidgetLayout, streamSize: CGSize): CIImage {
    val alignment = layout.alignment
    var x = layoutOffset(layout.x,
        extent.width,
        streamSize.width,
        alignment.isHorizontalCenter(),
        alignment.isLeft()) - extent.minX
    var y = layoutOffset(layout.y,
        extent.height,
        streamSize.height,
        alignment.isVerticalCenter(),
        alignment.mirrorPositionVertically()) - extent.minY
    if (alignment.mirrorPositionHorizontally() && x != 0.0) {
        x += 1
    }
    if (alignment.isTop() && y != 0.0) {
        y += 1
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

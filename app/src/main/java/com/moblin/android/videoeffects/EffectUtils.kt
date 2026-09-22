package com.moblin.android.videoeffects

import android.graphics.Bitmap
import android.media.Image
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.moblin.android.various.settings.SettingsWidgetLayout
import kotlin.math.max
import kotlin.math.min

var highQualityDownsampling = false

fun toPixels(percentage: Double, total: Double): Double {
    return (percentage * total) / 100
}

interface EffectImage {
    fun getCiImage(): CIImage

    fun getMetalPetalImage(): MTIImage
}

class EffectImageCgImage(image: Bitmap) : EffectImage {
    private val source: Bitmap = image
    private var ciImage: CIImage? = null
    private var metalPetalImage: MTIImage? = null

    override fun getCiImage(): CIImage {
        ciImage?.let { return it }
        val created: CIImage = TODO("OpenGL ES port: android.graphics.Bitmap to CIImage")
        ciImage = created
        return created
    }

    override fun getMetalPetalImage(): MTIImage {
        metalPetalImage?.let { return it }
        val created: MTIImage = TODO("OpenGL ES port: android.graphics.Bitmap to MTIImage")
        metalPetalImage = created
        return created
    }
}

class EffectImageCiImage(image: CIImage, private val isOpaque: Boolean) : EffectImage {
    private val source: CIImage = image
    private var metalPetalImage: MTIImage? = null

    override fun getCiImage(): CIImage {
        return source
    }

    override fun getMetalPetalImage(): MTIImage {
        metalPetalImage?.let { return it }
        val created: MTIImage = TODO("OpenGL ES port: CIImage to MTIImage (isOpaque=$isOpaque)")
        metalPetalImage = created
        return created
    }
}

class EffectImagePixelBuffer(pixelBuffer: Image) : EffectImage {
    private val source: Image = pixelBuffer
    private var ciImage: CIImage? = null
    private var metalPetalImage: MTIImage? = null

    override fun getCiImage(): CIImage {
        ciImage?.let { return it }
        val created: CIImage = TODO("OpenGL ES port: android.media.Image to CIImage")
        ciImage = created
        return created
    }

    override fun getMetalPetalImage(): MTIImage {
        metalPetalImage?.let { return it }
        val created: MTIImage = TODO("OpenGL ES port: android.media.Image to MTIImage")
        metalPetalImage = created
        return created
    }
}

fun CIImage.toEffectImage(isOpaque: Boolean): EffectImageCiImage {
    return EffectImageCiImage(image = this, isOpaque = isOpaque)
}

fun Bitmap.toEffectImage(): EffectImageCgImage {
    return EffectImageCgImage(image = this)
}

fun layoutPosition(layout: SettingsWidgetLayout, size: Size, streamSize: Size): Offset {
    var x: Double
    var y: Double
    if (layout.alignment.isHorizontalCenter()) {
        x = (streamSize.width - size.width) / 2.0
    } else if (layout.alignment.isLeft()) {
        x = toPixels(layout.x, streamSize.width.toDouble())
    } else {
        x = streamSize.width - toPixels(layout.x, streamSize.width.toDouble()) - size.width
    }
    if (layout.alignment.isVerticalCenter()) {
        y = (streamSize.height - size.height) / 2.0
    } else if (layout.alignment.isTop()) {
        y = toPixels(layout.y, streamSize.height.toDouble())
    } else {
        y = streamSize.height - toPixels(layout.y, streamSize.height.toDouble()) - size.height
    }
    return Offset(x.toFloat(), y.toFloat())
}

fun metalPetalLayerPosition(layout: SettingsWidgetLayout, size: Size, streamSize: Size): Offset {
    val position = layoutPosition(layout, size, streamSize)
    return Offset(position.x + size.width / 2F, position.y + size.height / 2F)
}

fun MTIImage.moveComposited(layout: SettingsWidgetLayout,
                            backgroundImage: MTIImage,
                            contentRegion: Rect? = null): MTIImage {
    val region = contentRegion ?: extent
    return composited(layout,
                      region.size,
                      false,
                      backgroundImage,
                      MetalPetalWidgetShape(contentRegion = region))
}

fun MTIImage.positionComposited(position: Offset,
                                backgroundImage: MTIImage,
                                size: Size? = null): MTIImage {
    TODO()
}

fun MTIImage.resizeMirrorMoveComposited(layout: SettingsWidgetLayout,
                                        mirror: Boolean,
                                        backgroundImage: MTIImage,
                                        shape: MetalPetalWidgetShape): MTIImage {
    val backgroundImageSize = backgroundImage.extent.size
    val rotatedSize = shape.rotated(shape.contentRegion.size)
    val scaleX = toPixels(layout.size, backgroundImageSize.width.toDouble()) / rotatedSize.width
    val scaleY = toPixels(layout.size, backgroundImageSize.height.toDouble()) / rotatedSize.height
    val scale = min(scaleX, scaleY)
    val size = Size((shape.contentRegion.width * scale).toFloat(),
                    (shape.contentRegion.height * scale).toFloat())
    return composited(layout, size, mirror, backgroundImage, shape)
}

private fun MTIImage.composited(layout: SettingsWidgetLayout,
                               size: Size,
                               mirror: Boolean,
                               backgroundImage: MTIImage,
                               shape: MetalPetalWidgetShape): MTIImage {
    val borderWidth = shape.borderWidthPixels(size)
    val borderSize = Size((size.width + 2 * borderWidth).toFloat(),
                          (size.height + 2 * borderWidth).toFloat())
    val position = metalPetalLayerPosition(layout,
                                           shape.rotated(borderSize),
                                           backgroundImage.extent.size)
    val rotation = shape.rotationRadians()
    TODO()
}

fun CIImage.resizeMirror(layout: SettingsWidgetLayout,
                         streamSize: Size,
                         mirror: Boolean,
                         resize: Boolean = true): CIImage {
    if (!resize) {
        return this
    }
    var scaleX = toPixels(layout.size, streamSize.width.toDouble()) / extent.size.width
    var scaleY = toPixels(layout.size, streamSize.height.toDouble()) / extent.size.height
    val scale = min(scaleX, scaleY)
    if (mirror) {
        scaleX = -scale
    } else {
        scaleX = scale
    }
    scaleY = scale
    val scaledImage = scaled(scaleX, scaleY)
    return if (mirror) {
        scaledImage.translated(scaledImage.extent.width.toDouble(), 0.0)
    } else {
        scaledImage
    }
}

fun CIImage.move(layout: SettingsWidgetLayout, streamSize: Size): CIImage {
    var x: Double
    var y: Double
    if (layout.alignment.isHorizontalCenter()) {
        x = (streamSize.width - extent.width) / 2.0 - extent.left
    } else if (layout.alignment.isLeft()) {
        x = toPixels(layout.x, streamSize.width.toDouble()) - extent.left
    } else {
        x = streamSize.width - toPixels(layout.x, streamSize.width.toDouble()) - extent.width -
            extent.left
        if (x != 0.0) {
            x += 1.0
        }
    }
    if (layout.alignment.isVerticalCenter()) {
        y = (streamSize.height - extent.height) / 2.0 - extent.top
    } else if (layout.alignment.isTop()) {
        y = streamSize.height - toPixels(layout.y, streamSize.height.toDouble()) - extent.height -
            extent.top
        if (y != 0.0) {
            y += 1.0
        }
    } else {
        y = toPixels(layout.y, streamSize.height.toDouble()) - extent.top
    }
    return translated(x, y)
}

fun CIImage.translated(x: Double, y: Double): CIImage {
    TODO()
}

fun CIImage.scaled(x: Double, y: Double): CIImage {
    TODO()
}

fun CIImage.scaledTo(size: Size): CIImage {
    val scaleX = size.width / extent.width
    val scaleY = size.height / extent.height
    val scale = min(scaleX, scaleY)
    return scaled(scale.toDouble(), scale.toDouble())
}

fun CIImage.scaledToFill(size: Size): CIImage {
    val scaleX = size.width / extent.width
    val scaleY = size.height / extent.height
    val scale = max(scaleX, scaleY)
    return scaled(scale.toDouble(), scale.toDouble())
}

fun CIImage.centered(size: Size): CIImage {
    val targetCenterX = size.width / 2
    val targetCenterY = size.height / 2
    val currentCenterX = extent.width / 2
    val currentCenterY = extent.height / 2
    val x = targetCenterX - currentCenterX
    val y = targetCenterY - currentCenterY
    return translated(x.toDouble(), y.toDouble())
}

class CIImage(val extent: Rect)

class MTIImage(val extent: Rect)

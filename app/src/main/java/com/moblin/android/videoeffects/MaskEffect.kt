package com.moblin.android.videoeffects

import android.graphics.Bitmap
import com.moblin.android.common.various.RgbColor
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.platform.coregraphics.CGAffineTransform
import com.moblin.android.platform.coregraphics.CGColor
import com.moblin.android.platform.coregraphics.CGColorSpaceCreateDeviceGray
import com.moblin.android.platform.coregraphics.CGColorSpaceCreateDeviceRGB
import com.moblin.android.platform.coregraphics.CGContext
import com.moblin.android.platform.coregraphics.CGImageAlphaInfo
import com.moblin.android.platform.coregraphics.CGMutablePath
import com.moblin.android.platform.coregraphics.CGPoint
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.coreimage.CIColor
import com.moblin.android.platform.coreimage.CIFilter
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.metalpetal.MTIBlendWithMaskFilter
import com.moblin.android.platform.metalpetal.MTIColor
import com.moblin.android.platform.metalpetal.MTIColorComponent
import com.moblin.android.platform.metalpetal.MTIImage
import com.moblin.android.platform.metalpetal.MTIMask
import com.moblin.android.platform.metalpetal.MTIMaskMode
import com.moblin.android.platform.metalpetal.MTKTextureLoader
import com.moblin.android.various.settings.SettingsMaskBackgroundType
import kotlin.math.ceil
import kotlinx.coroutines.launch

data class MaskEffectPoint(val x: Double, val y: Double)

data class MaskEffectSettings(
    val points: List<MaskEffectPoint>,
    val inverted: Boolean,
    val tension: Double,
    val backgroundType: SettingsMaskBackgroundType,
    val backgroundColor: RgbColor,
    val backgroundColor2: RgbColor,
)

private val checkerboardSquareCount: Float = 20.0f

private fun CGRect.isSameRectAs(other: CGRect): Boolean =
    origin.x == other.origin.x &&
        origin.y == other.origin.y &&
        size.width == other.size.width &&
        size.height == other.size.height

fun makeCatmullRomPath(points: List<CGPoint>, tension: Double): CGMutablePath {
    val numberOfPoints = points.size
    val path = CGMutablePath()
    path.move(to = points[0])
    for (i in 0 until numberOfPoints) {
        val point0 = points[(i - 1 + numberOfPoints) % numberOfPoints]
        val point1 = points[i]
        val point2 = points[(i + 1) % numberOfPoints]
        val point3 = points[(i + 2) % numberOfPoints]
        val cpoint1 = CGPoint(
            x = point1.x + (point2.x - point0.x) * tension,
            y = point1.y + (point2.y - point0.y) * tension,
        )
        val cpoint2 = CGPoint(
            x = point2.x - (point3.x - point1.x) * tension,
            y = point2.y - (point3.y - point1.y) * tension,
        )
        path.addCurve(to = point2, control1 = cpoint1, control2 = cpoint2)
    }
    path.closeSubpath()
    return path
}

private fun makeCiColor(color: RgbColor): CIColor =
    CIColor(
        red = color.red.toDouble() / 255.0,
        green = color.green.toDouble() / 255.0,
        blue = color.blue.toDouble() / 255.0,
    )

private fun makeCgColor(color: RgbColor): CGColor =
    CGColor(
        red = color.red.toDouble() / 255.0,
        green = color.green.toDouble() / 255.0,
        blue = color.blue.toDouble() / 255.0,
        alpha = 1.0,
    )

private fun makeMtiColor(color: RgbColor): MTIColor =
    MTIColor(
        red = color.red.toFloat() / 255f,
        green = color.green.toFloat() / 255f,
        blue = color.blue.toFloat() / 255f,
        alpha = 1f,
    )

private fun makeCheckerboardImage(extent: CGRect, settings: MaskEffectSettings): Bitmap? {
    val width = extent.width.toInt()
    val height = extent.height.toInt()
    if (width <= 0 || height <= 0) {
        return null
    }
    val context = CGContext(
        data = null,
        width = width,
        height = height,
        bitsPerComponent = 8,
        bytesPerRow = 0,
        space = CGColorSpaceCreateDeviceRGB(),
        bitmapInfo = CGImageAlphaInfo.premultipliedLast.rawValue,
    ) ?: return null
    val squareSide = minOf(width, height).toDouble() / checkerboardSquareCount.toDouble()
    context.setFillColor(makeCgColor(settings.backgroundColor))
    context.fill(CGRect(x = 0, y = 0, width = width, height = height))
    context.setFillColor(makeCgColor(settings.backgroundColor2))
    val columns = ceil(width.toDouble() / squareSide).toInt() + 2
    val rows = ceil(height.toDouble() / squareSide).toInt() + 2
    for (row in 0 until rows) {
        for (column in 0 until columns) {
            if ((row + column) % 2 == 1) {
                val x = width.toDouble() / 2 + (column - columns / 2) * squareSide
                val y = height.toDouble() / 2 + (row - rows / 2) * squareSide
                context.fill(CGRect(x = x, y = y, width = squareSide, height = squareSide))
            }
        }
    }
    return context.makeImage()
}

class MaskEffect : VideoEffect() {
    private var settings: MaskEffectSettings? = null
    private var cachedSettings: MaskEffectSettings? = null
    private var cachedExtent: CGRect = CGRect.zero
    private var cachedMaskImage: CIImage? = null
    private var cachedBackgroundImage: CIImage? = null
    private val filterMetalPetal = MTIBlendWithMaskFilter()
    private var cachedMetalPetalSettings: MaskEffectSettings? = null
    private var cachedMetalPetalExtent: CGRect = CGRect.zero
    private var cachedMetalPetalMask: MTIMask? = null
    private var cachedMetalPetalBackgroundImage: MTIImage? = null

    fun setSettings(settings: MaskEffectSettings) {
        processorPipelineQueue.launch {
            this@MaskEffect.settings = settings
            this@MaskEffect.cachedSettings = null
            this@MaskEffect.cachedMaskImage = null
            this@MaskEffect.cachedBackgroundImage = null
            this@MaskEffect.cachedMetalPetalSettings = null
            this@MaskEffect.cachedMetalPetalMask = null
            this@MaskEffect.cachedMetalPetalBackgroundImage = null
        }
    }

    private fun makeMaskImage(extent: CGRect, settings: MaskEffectSettings): CIImage? {
        val cgImage = makeMaskCgImage(extent, settings) ?: return null
        return CIImage(cgImage = cgImage)
            .transformed(by = CGAffineTransform(translationX = extent.minX, y = extent.minY))
    }

    private fun makeMaskCgImage(extent: CGRect, settings: MaskEffectSettings): Bitmap? {
        if (settings.points.size < 3) {
            return null
        }
        val width = extent.width.toInt()
        val height = extent.height.toInt()
        if (width <= 0 || height <= 0) {
            return null
        }
        val context = CGContext(
            data = null,
            width = width,
            height = height,
            bitsPerComponent = 8,
            bytesPerRow = width,
            space = CGColorSpaceCreateDeviceGray(),
            bitmapInfo = CGImageAlphaInfo.none.rawValue,
        ) ?: return null
        val backgroundGray: Double = if (settings.inverted) 1.0 else 0.0
        val polygonGray: Double = if (settings.inverted) 0.0 else 1.0
        context.setFillColor(gray = backgroundGray, alpha = 1.0)
        context.fill(CGRect(x = 0, y = 0, width = width, height = height))
        context.setFillColor(gray = polygonGray, alpha = 1.0)
        val screenPoints = settings.points.map {
            CGPoint(x = it.x * width, y = (1.0 - it.y) * height)
        }
        val path = makeCatmullRomPath(screenPoints, tension = settings.tension)
        context.addPath(path)
        context.fillPath()
        return context.makeImage()
    }

    private fun makeBackgroundImage(extent: CGRect, settings: MaskEffectSettings): CIImage =
        when (settings.backgroundType) {
            SettingsMaskBackgroundType.transparent -> CIImage.empty()
            SettingsMaskBackgroundType.solid ->
                CIImage(color = makeCiColor(settings.backgroundColor)).cropped(to = extent)
            SettingsMaskBackgroundType.checkerboard -> {
                val filter = CIFilter.checkerboardGenerator()
                filter.color0 = makeCiColor(settings.backgroundColor)
                filter.color1 = makeCiColor(settings.backgroundColor2)
                filter.width = minOf(extent.width, extent.height).toFloat() / checkerboardSquareCount
                filter.sharpness = 1.0f
                filter.center = CGPoint(x = extent.midX, y = extent.midY)
                filter.outputImage?.cropped(to = extent) ?: CIImage.empty()
            }
        }

    private fun makeMetalPetalBackgroundImage(extent: CGRect, settings: MaskEffectSettings): MTIImage {
        when (settings.backgroundType) {
            SettingsMaskBackgroundType.transparent ->
                return MTIImage(color = MTIColor.clear, sRGB = false, size = extent.size)
            SettingsMaskBackgroundType.solid ->
                return MTIImage(
                    color = makeMtiColor(settings.backgroundColor),
                    sRGB = false,
                    size = extent.size,
                )
            SettingsMaskBackgroundType.checkerboard -> {
                val cgImage = makeCheckerboardImage(extent, settings)
                    ?: return MTIImage(color = MTIColor.clear, sRGB = false, size = extent.size)
                return MTIImage(
                    cgImage = cgImage,
                    options = mapOf(MTKTextureLoader.Option.SRGB to false),
                    isOpaque = true,
                )
            }
        }
    }

    override fun executeMetalPetal(image: MTIImage, info: VideoEffectInfo): MTIImage {
        val settings = this.settings ?: return image
        val extent = image.extent
        if (cachedMetalPetalMask == null || cachedMetalPetalSettings != settings ||
            !cachedMetalPetalExtent.isSameRectAs(extent)
        ) {
            val maskCgImage = makeMaskCgImage(extent, settings) ?: return image
            cachedMetalPetalMask = MTIMask(
                content = MTIImage(
                    cgImage = maskCgImage,
                    options = mapOf(MTKTextureLoader.Option.SRGB to false),
                    isOpaque = true,
                ),
                component = MTIColorComponent.red,
                mode = MTIMaskMode.normal,
            )
            cachedMetalPetalBackgroundImage = makeMetalPetalBackgroundImage(extent, settings)
            cachedMetalPetalSettings = settings
            cachedMetalPetalExtent = extent
        }
        filterMetalPetal.inputImage = image
        filterMetalPetal.inputMask = cachedMetalPetalMask
        filterMetalPetal.inputBackgroundImage = cachedMetalPetalBackgroundImage
        return filterMetalPetal.outputImage ?: image
    }

    override fun execute(image: CIImage, info: VideoEffectInfo): CIImage {
        val settings = this.settings ?: return image
        val extent = image.extent
        val maskImage: CIImage
        val backgroundImage: CIImage
        val cachedMaskImage = this.cachedMaskImage
        val cachedBackgroundImage = this.cachedBackgroundImage
        if (cachedMaskImage != null && cachedBackgroundImage != null &&
            cachedSettings == settings && cachedExtent.isSameRectAs(extent)
        ) {
            maskImage = cachedMaskImage
            backgroundImage = cachedBackgroundImage
        } else {
            val newMaskImage = makeMaskImage(extent, settings) ?: return image
            this.cachedMaskImage = newMaskImage
            this.cachedSettings = settings
            this.cachedExtent = extent
            maskImage = newMaskImage
            backgroundImage = makeBackgroundImage(extent, settings)
            this.cachedBackgroundImage = backgroundImage
        }
        val blender = CIFilter.blendWithMask()
        blender.inputImage = image
        blender.maskImage = maskImage
        blender.backgroundImage = backgroundImage
        return blender.outputImage ?: image
    }
}

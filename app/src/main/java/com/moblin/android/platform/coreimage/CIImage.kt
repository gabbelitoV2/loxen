package com.moblin.android.platform.coreimage

import android.graphics.Bitmap
import com.moblin.android.platform.coregraphics.CGAffineTransform
import com.moblin.android.platform.coregraphics.CGImagePropertyOrientation
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.coreimage.internal.AlphaOneOp
import com.moblin.android.platform.coreimage.internal.BitmapNode
import com.moblin.android.platform.coreimage.internal.BlurNode
import com.moblin.android.platform.coreimage.internal.ClampNode
import com.moblin.android.platform.coreimage.internal.ColorOpNode
import com.moblin.android.platform.coreimage.internal.CompositeNode
import com.moblin.android.platform.coreimage.internal.ConstantNode
import com.moblin.android.platform.coreimage.internal.CropNode
import com.moblin.android.platform.coreimage.internal.EffectsLog
import com.moblin.android.platform.coreimage.internal.EmptyNode
import com.moblin.android.platform.coreimage.internal.ImageNode
import com.moblin.android.platform.coreimage.internal.PixelBufferNode
import com.moblin.android.platform.coreimage.internal.PremultiplyOp
import com.moblin.android.platform.coreimage.internal.SamplingNode
import com.moblin.android.platform.coreimage.internal.SourceAlpha
import com.moblin.android.platform.coreimage.internal.SourceEncoding
import com.moblin.android.platform.coreimage.internal.TransformNode
import com.moblin.android.platform.coreimage.internal.UnpremultiplyOp
import com.moblin.android.platform.coreimage.internal.orientedNode
import com.moblin.android.platform.video.CVPixelBuffer
import java.io.File

enum class CIImageOption {
    applyOrientationProperty,
    colorSpace,
    properties,
}

class CIImage internal constructor(internal val node: ImageNode) {
    constructor(cvPixelBuffer: CVPixelBuffer) : this(
        PixelBufferNode(cvPixelBuffer, SourceAlpha.asIs, SourceEncoding.srgb)
    )

    constructor(cvPixelBuffer: CVPixelBuffer, options: Map<CIImageOption, Any>?) : this(
        PixelBufferNode(cvPixelBuffer, SourceAlpha.asIs, encodingFor(options))
    )

    constructor(cgImage: Bitmap) : this(BitmapNode(cgImage, SourceAlpha.asIs, SourceEncoding.srgb))

    constructor(cgImage: Bitmap, options: Map<CIImageOption, Any>?) : this(
        BitmapNode(cgImage, SourceAlpha.asIs, encodingFor(options))
    )

    constructor(color: CIColor) : this(
        ConstantNode(
            color.red,
            color.green,
            color.blue,
            color.alpha,
            colorEncoding(color),
            CGRect.infinite
        )
    )

    val extent: CGRect
        get() = node.extent

    val properties: Map<String, Any>
        get() = emptyMap()

    fun cropped(to: CGRect): CIImage {
        return CIImage(CropNode(node, to))
    }

    fun composited(over: CIImage): CIImage {
        return CIImage(CompositeNode(node, over.node))
    }

    fun transformed(by: CGAffineTransform): CIImage {
        return CIImage(TransformNode(node, by, false))
    }

    fun transformed(by: CGAffineTransform, highQualityDownsample: Boolean): CIImage {
        return CIImage(TransformNode(node, by, highQualityDownsample))
    }

    fun oriented(orientation: CGImagePropertyOrientation): CIImage {
        return CIImage(orientedNode(node, orientation))
    }

    fun oriented(forExifOrientation: Int): CIImage {
        val orientation = CGImagePropertyOrientation(forExifOrientation) ?: CGImagePropertyOrientation.up
        return oriented(orientation)
    }

    fun applyingGaussianBlur(sigma: Double): CIImage {
        return CIImage(BlurNode(node, sigma, false, blurExtent(extent, sigma)))
    }

    fun applyingFilter(filterName: String, parameters: Map<String, Any> = emptyMap()): CIImage {
        val filter = CIFilter(name = filterName)
        if (filter == null) {
            EffectsLog.once("applyingFilter:$filterName", "CIImage.applyingFilter: unknown filter $filterName")
            return empty()
        }
        filter.setValue(this, forKey = kCIInputImageKey)
        for ((key, value) in parameters) {
            filter.setValue(value, forKey = key)
        }
        return filter.outputImage ?: empty()
    }

    fun clampedToExtent(): CIImage {
        return CIImage(ClampNode(node))
    }

    fun samplingNearest(): CIImage {
        return CIImage(SamplingNode(node, true))
    }

    fun samplingLinear(): CIImage {
        val current = node
        if (current is SamplingNode) {
            return CIImage(SamplingNode(current.input, false))
        }
        return CIImage(SamplingNode(current, false))
    }

    fun premultiplyingAlpha(): CIImage {
        return CIImage(ColorOpNode(node, PremultiplyOp()))
    }

    fun unpremultiplyingAlpha(): CIImage {
        return CIImage(ColorOpNode(node, UnpremultiplyOp()))
    }

    fun settingAlphaOne(inExtent: CGRect): CIImage {
        return CIImage(ColorOpNode(node, AlphaOneOp(inExtent), inExtent))
    }

    override fun toString(): String {
        return "CIImage(extent=$extent)"
    }

    companion object {
        operator fun invoke(data: ByteArray, options: Map<CIImageOption, Any>? = null): CIImage? {
            return CIImageDecoder.decode(data, options)
        }

        operator fun invoke(image: Bitmap): CIImage? {
            if (image.isRecycled) {
                return null
            }
            return CIImage(cgImage = image)
        }

        operator fun invoke(contentsOf: String, options: Map<CIImageOption, Any>? = null): CIImage? {
            return CIImageDecoder.decode(contentsOf, options)
        }

        operator fun invoke(contentsOf: File, options: Map<CIImageOption, Any>? = null): CIImage? {
            return invoke(contentsOf = contentsOf.path, options = options)
        }

        val black: CIImage
            get() = CIImage(color = CIColor.black)

        val white: CIImage
            get() = CIImage(color = CIColor.white)

        val clear: CIImage
            get() = CIImage(color = CIColor.clear)

        fun empty(): CIImage {
            return CIImage(EmptyNode())
        }
    }
}

internal fun blurExtent(extent: CGRect, sigma: Double): CGRect {
    if (extent.isNull || extent.isInfinite || sigma <= 0) {
        return extent
    }
    return extent.insetBy(-3 * sigma, -3 * sigma)
}

private fun colorEncoding(color: CIColor): SourceEncoding {
    return when (color.colorSpace?.name) {
        com.moblin.android.platform.coregraphics.CGColorSpace.extendedLinearSRGB,
        com.moblin.android.platform.coregraphics.CGColorSpace.linearSRGB,
        -> SourceEncoding.working
        else -> SourceEncoding.srgb
    }
}

private fun encodingFor(options: Map<CIImageOption, Any>?): SourceEncoding {
    val colorSpace = options?.get(CIImageOption.colorSpace) ?: return SourceEncoding.srgb
    return if (colorSpace is com.moblin.android.platform.coregraphics.CGColorSpace) {
        SourceEncoding.srgb
    } else {
        SourceEncoding.working
    }
}

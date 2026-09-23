package com.moblin.android.platform.metalpetal

import android.graphics.Bitmap
import com.moblin.android.platform.coregraphics.CGImagePropertyOrientation
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.coreimage.internal.BitmapNode
import com.moblin.android.platform.coreimage.internal.CiBoundaryNode
import com.moblin.android.platform.coreimage.internal.ConstantNode
import com.moblin.android.platform.coreimage.internal.ImageNode
import com.moblin.android.platform.coreimage.internal.PixelBufferNode
import com.moblin.android.platform.coreimage.internal.SourceAlpha
import com.moblin.android.platform.coreimage.internal.SourceEncoding
import com.moblin.android.platform.coreimage.internal.orientedNode
import com.moblin.android.platform.video.CVPixelBuffer
import kotlin.math.max
import kotlin.math.roundToInt

class MTLDevice internal constructor() {
    val name: String
        get() = "OpenGL ES"
}

private val systemDefaultDevice = MTLDevice()

fun MTLCreateSystemDefaultDevice(): MTLDevice? {
    return systemDefaultDevice
}

enum class MTLPixelFormat {
    unspecified,
    bgra8Unorm,
    bgra8Unorm_srgb,
    rgba8Unorm,
    rgba16Float,
    r8Unorm,
}

object MTKTextureLoader {
    enum class Option {
        SRGB,
        allocateMipmaps,
        generateMipmaps,
    }
}

enum class MTIAlphaType {
    unknown,
    nonPremultiplied,
    premultiplied,
    alphaIsOne,
}

class MTIColor(val red: Float, val green: Float, val blue: Float, val alpha: Float) {
    constructor(red: Double, green: Double, blue: Double, alpha: Double) : this(
        red.toFloat(),
        green.toFloat(),
        blue.toFloat(),
        alpha.toFloat()
    )

    fun copy(
        red: Float = this.red,
        green: Float = this.green,
        blue: Float = this.blue,
        alpha: Float = this.alpha,
    ): MTIColor {
        return MTIColor(red, green, blue, alpha)
    }

    fun toFloat4(): com.moblin.android.platform.simd.SIMD4 {
        return com.moblin.android.platform.simd.SIMD4(red, green, blue, alpha)
    }

    override fun equals(other: Any?): Boolean {
        return other is MTIColor && red == other.red && green == other.green && blue == other.blue &&
            alpha == other.alpha
    }

    override fun hashCode(): Int {
        return listOf(red, green, blue, alpha).hashCode()
    }

    override fun toString(): String {
        return "MTIColor(red=$red, green=$green, blue=$blue, alpha=$alpha)"
    }

    companion object {
        val white = MTIColor(1f, 1f, 1f, 1f)
        val black = MTIColor(0f, 0f, 0f, 1f)
        val clear = MTIColor(0f, 0f, 0f, 0f)
    }
}

class MTIImage internal constructor(
    internal val node: ImageNode,
    val alphaType: MTIAlphaType,
    internal val width: Int,
    internal val height: Int,
) {
    internal constructor(node: ImageNode, alphaType: MTIAlphaType) : this(
        node,
        alphaType,
        dimension(node.extent.width),
        dimension(node.extent.height)
    )

    constructor(cvPixelBuffer: CVPixelBuffer, alphaType: MTIAlphaType) : this(
        PixelBufferNode(cvPixelBuffer, sourceAlpha(alphaType), SourceEncoding.raw),
        alphaType,
        cvPixelBuffer.width,
        cvPixelBuffer.height
    )

    constructor(
        cgImage: Bitmap,
        options: Map<MTKTextureLoader.Option, Any> = mapOf(MTKTextureLoader.Option.SRGB to false),
        isOpaque: Boolean = false,
    ) : this(
        BitmapNode(
            cgImage,
            if (isOpaque) SourceAlpha.opaque else SourceAlpha.asIs,
            if (options[MTKTextureLoader.Option.SRGB] == true) SourceEncoding.srgbAlways else SourceEncoding.raw
        ),
        if (isOpaque) MTIAlphaType.alphaIsOne else MTIAlphaType.premultiplied,
        cgImage.width,
        cgImage.height
    )

    constructor(ciImage: CIImage, isOpaque: Boolean = false) : this(
        CiBoundaryNode(
            ciImage.node,
            ciImage.extent,
            isOpaque,
            dimension(ciImage.extent.width),
            dimension(ciImage.extent.height)
        ),
        if (isOpaque) MTIAlphaType.alphaIsOne else MTIAlphaType.premultiplied,
        dimension(ciImage.extent.width),
        dimension(ciImage.extent.height)
    )

    constructor(color: MTIColor, sRGB: Boolean, size: CGSize) : this(
        ConstantNode(
            color.red.toDouble(),
            color.green.toDouble(),
            color.blue.toDouble(),
            color.alpha.toDouble(),
            if (sRGB) SourceEncoding.srgbAlways else SourceEncoding.raw,
            CGRect(0.0, 0.0, dimension(size.width).toDouble(), dimension(size.height).toDouble())
        ),
        MTIAlphaType.nonPremultiplied,
        dimension(size.width),
        dimension(size.height)
    )

    val extent: CGRect
        get() = CGRect(0.0, 0.0, width.toDouble(), height.toDouble())

    val size: CGSize
        get() = CGSize(width.toDouble(), height.toDouble())

    fun oriented(orientation: CGImagePropertyOrientation): MTIImage {
        val swapped = orientation.rawValue >= 5
        return MTIImage(
            orientedNode(node, orientation),
            alphaType,
            if (swapped) height else width,
            if (swapped) width else height
        )
    }

    fun premultiplyingAlpha(): MTIImage {
        return MTIImage(node, MTIAlphaType.premultiplied, width, height)
    }

    fun unpremultiplyingAlpha(): MTIImage {
        return MTIImage(node, MTIAlphaType.nonPremultiplied, width, height)
    }

    fun withAlphaType(alphaType: MTIAlphaType): MTIImage {
        return MTIImage(node, alphaType, width, height)
    }

    override fun toString(): String {
        return "MTIImage(${width}x$height, $alphaType)"
    }

    companion object {
        val white = MTIImage(MTIColor.white, false, CGSize(1.0, 1.0))
        val black = MTIImage(MTIColor.black, false, CGSize(1.0, 1.0))
        val transparent = MTIImage(MTIColor.clear, false, CGSize(1.0, 1.0))
    }
}

internal fun dimension(value: Double): Int {
    if (value.isNaN() || value.isInfinite() || value <= 0) {
        return 0
    }
    return max(0, value.roundToInt())
}

private fun sourceAlpha(alphaType: MTIAlphaType): SourceAlpha {
    return when (alphaType) {
        MTIAlphaType.alphaIsOne -> SourceAlpha.opaque
        MTIAlphaType.nonPremultiplied -> SourceAlpha.premultiply
        else -> SourceAlpha.asIs
    }
}

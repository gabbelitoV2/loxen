package com.moblin.android.platform.metalpetal

import com.moblin.android.platform.coregraphics.CGAffineTransform
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.coreimage.internal.BlurNode
import com.moblin.android.platform.coreimage.internal.ColorMatrixOp
import com.moblin.android.platform.coreimage.internal.ColorOpNode
import com.moblin.android.platform.coreimage.internal.CropNode
import com.moblin.android.platform.coreimage.internal.LayerBlend
import com.moblin.android.platform.coreimage.internal.LayerSpec
import com.moblin.android.platform.coreimage.internal.LayersNode
import com.moblin.android.platform.coreimage.internal.MaskSpec
import com.moblin.android.platform.coreimage.internal.TransformNode
import com.moblin.android.platform.coreimage.internal.UnpremultiplyAlphaOneOp
import com.moblin.android.platform.simd.SIMD4
import com.moblin.android.platform.simd.simd_float4x4
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToLong

abstract class MTIFilter {
    abstract val outputImage: MTIImage?
    var outputPixelFormat: MTLPixelFormat = MTLPixelFormat.unspecified
}

open class MTIUnaryImageRenderingFilter : MTIFilter() {
    var inputImage: MTIImage? = null

    override val outputImage: MTIImage?
        get() = null
}

class MTIColorMatrix(var matrix: simd_float4x4, var bias: SIMD4) {
    fun copy(): MTIColorMatrix {
        return MTIColorMatrix(matrix, bias)
    }

    val isIdentity: Boolean
        get() = matrix == simd_float4x4.identity && bias == SIMD4.zero

    override fun equals(other: Any?): Boolean {
        return other is MTIColorMatrix && matrix == other.matrix && bias == other.bias
    }

    override fun hashCode(): Int {
        return 31 * matrix.hashCode() + bias.hashCode()
    }

    companion object {
        val identity: MTIColorMatrix
            get() = MTIColorMatrix(simd_float4x4.identity, SIMD4.zero)

        fun opacity(opacity: Float): MTIColorMatrix {
            return MTIColorMatrix(
                simd_float4x4(
                    SIMD4(1f, 0f, 0f, 0f),
                    SIMD4(0f, 1f, 0f, 0f),
                    SIMD4(0f, 0f, 1f, 0f),
                    SIMD4(0f, 0f, 0f, opacity)
                ),
                SIMD4.zero
            )
        }
    }
}

open class MTIColorMatrixFilter : MTIUnaryImageRenderingFilter() {
    var colorMatrix: MTIColorMatrix = MTIColorMatrix.identity

    override val outputImage: MTIImage?
        get() {
            val image = inputImage ?: return null
            val matrix = colorMatrix
            if (matrix.isIdentity) {
                return MTIImage(image.node, MTIAlphaType.nonPremultiplied, image.width, image.height)
            }
            val op = ColorMatrixOp(
                columnArray(matrix.matrix[0]),
                columnArray(matrix.matrix[1]),
                columnArray(matrix.matrix[2]),
                columnArray(matrix.matrix[3]),
                columnArray(matrix.bias)
            )
            return MTIImage(ColorOpNode(image.node, op), MTIAlphaType.nonPremultiplied, image.width, image.height)
        }

    private fun columnArray(vector: SIMD4): FloatArray {
        return floatArrayOf(vector.x, vector.y, vector.z, vector.w)
    }
}

class MTIOpacityFilter : MTIColorMatrixFilter() {
    var opacity: Float = 1f
        set(value) {
            field = value
            colorMatrix = MTIColorMatrix.opacity(value)
        }
}

enum class MTICropRegionUnit {
    pixel,
    percentage,
}

class MTICropRegion(var bounds: CGRect, var unit: MTICropRegionUnit) {
    companion object {
        fun pixel(rect: CGRect): MTICropRegion {
            return MTICropRegion(rect, MTICropRegionUnit.pixel)
        }

        fun fractional(rect: CGRect): MTICropRegion {
            return MTICropRegion(rect, MTICropRegionUnit.percentage)
        }
    }
}

enum class MTICropFilterRoundingMode {
    plain,
    ceiling,
    floor,
    pixelPerfect,
}

class MTICropFilter : MTIFilter() {
    var inputImage: MTIImage? = null
    var cropRegion: MTICropRegion = MTICropRegion(CGRect(0.0, 0.0, 1.0, 1.0), MTICropRegionUnit.percentage)
    var scale: Float = 1f
    var roundingMode: MTICropFilterRoundingMode = MTICropFilterRoundingMode.plain

    override val outputImage: MTIImage?
        get() {
            val image = inputImage ?: return null
            val bounds = cropRegion.bounds
            if (bounds.size.width <= 0 || bounds.size.height <= 0) {
                return null
            }
            val imageWidth = image.width.toDouble()
            val imageHeight = image.height.toDouble()
            val cropRect = when (cropRegion.unit) {
                MTICropRegionUnit.pixel -> bounds
                MTICropRegionUnit.percentage -> CGRect(
                    bounds.origin.x * imageWidth,
                    bounds.origin.y * imageHeight,
                    bounds.size.width * imageWidth,
                    bounds.size.height * imageHeight
                )
            }
            val outputWidth = round(cropRect.size.width * scale.toDouble())
            val outputHeight = round(cropRect.size.height * scale.toDouble())
            if (outputWidth <= 0 || outputHeight <= 0) {
                return null
            }
            if (outputWidth == image.width && outputHeight == image.height &&
                cropRect.origin.x == 0.0 && cropRect.origin.y == 0.0
            ) {
                return image
            }
            val treeX = cropRect.origin.x
            val treeY = imageHeight - cropRect.origin.y - cropRect.size.height
            val scaleX = outputWidth / cropRect.size.width
            val scaleY = outputHeight / cropRect.size.height
            val transform = CGAffineTransform(scaleX, 0.0, 0.0, scaleY, -treeX * scaleX, -treeY * scaleY)
            val node = CropNode(
                TransformNode(image.node, transform, false),
                CGRect(0.0, 0.0, outputWidth.toDouble(), outputHeight.toDouble())
            )
            return MTIImage(node, image.alphaType, outputWidth, outputHeight)
        }

    private fun round(value: Double): Int {
        val rounded = when (roundingMode) {
            MTICropFilterRoundingMode.plain, MTICropFilterRoundingMode.pixelPerfect -> Math.round(value).toDouble()
            MTICropFilterRoundingMode.ceiling -> ceil(value)
            MTICropFilterRoundingMode.floor -> floor(value)
        }
        return rounded.roundToLong().toInt()
    }
}

class MTIMPSGaussianBlurFilter : MTIFilter() {
    var inputImage: MTIImage? = null
    var radius: Float = 0f

    override val outputImage: MTIImage?
        get() {
            val image = inputImage ?: return null
            val sigma = ceil(radius.toDouble())
            if (sigma <= 0) {
                return image
            }
            val extent = CGRect(0.0, 0.0, image.width.toDouble(), image.height.toDouble())
            return MTIImage(BlurNode(image.node, sigma, true, extent), image.alphaType, image.width, image.height)
        }
}

class MTIMultilayerCompositingFilter : MTIFilter() {
    var inputBackgroundImage: MTIImage? = null
    var layers: List<MTILayer> = emptyList()
    var rasterSampleCount: Int = 1
    var outputAlphaType: MTIAlphaType = MTIAlphaType.nonPremultiplied

    override val outputImage: MTIImage?
        get() {
            val background = inputBackgroundImage ?: return null
            if (layers.isEmpty()) {
                return background
            }
            val canvasWidth = background.width.toDouble()
            val canvasHeight = background.height.toDouble()
            val backgroundSize = background.size
            val specs = layers.map { layerSpec(it, backgroundSize.width, backgroundSize.height) }
            val backgroundNode = CropNode(background.node, CGRect(0.0, 0.0, canvasWidth, canvasHeight))
            val node = LayersNode(backgroundNode, specs, canvasWidth, canvasHeight)
            if (outputAlphaType == MTIAlphaType.alphaIsOne) {
                return MTIImage(ColorOpNode(node, UnpremultiplyAlphaOneOp()), outputAlphaType, background.width, background.height)
            }
            return MTIImage(node, outputAlphaType, background.width, background.height)
        }

    private fun layerSpec(layer: MTILayer, canvasWidth: Double, canvasHeight: Double): LayerSpec {
        val backgroundSize = com.moblin.android.platform.coregraphics.CGSize(canvasWidth, canvasHeight)
        val size = layer.sizeInPixel(forBackgroundSize = backgroundSize)
        val position = layer.positionInPixel(forBackgroundSize = backgroundSize)
        val region = layer.contentRegion.standardized
        val corner = layer.cornerRadius
        return LayerSpec(
            content = layer.content.node,
            contentWidth = layer.content.width.toDouble(),
            contentHeight = layer.content.height.toDouble(),
            regionMinX = region.minX,
            regionMinY = region.minY,
            regionMaxX = region.maxX,
            regionMaxY = region.maxY,
            flipHorizontally = layer.contentFlipOptions.contains(MTILayer.FlipOptions.flipHorizontally),
            flipVertically = layer.contentFlipOptions.contains(MTILayer.FlipOptions.flipVertically),
            mask = layer.mask?.let { maskSpec(it) },
            compositingMask = layer.compositingMask?.let { maskSpec(it) },
            centerX = position.x,
            centerY = canvasHeight - position.y,
            width = size.width,
            height = size.height,
            rotation = layer.rotation.toDouble(),
            opacity = layer.opacity.toDouble(),
            cornerRadius = if (corner.isZero) {
                floatArrayOf(0f, 0f, 0f, 0f)
            } else {
                floatArrayOf(corner.topLeft, corner.topRight, corner.bottomRight, corner.bottomLeft)
            },
            continuousCorners = layer.cornerCurve == MTICornerCurve.continuous,
            tint = floatArrayOf(layer.tintColor.red, layer.tintColor.green, layer.tintColor.blue, layer.tintColor.alpha),
            blend = if (layer.blendMode == MTIBlendMode.normal) LayerBlend.normal else LayerBlend.other
        )
    }

    private fun maskSpec(mask: MTIMask): MaskSpec {
        return MaskSpec(
            content = mask.content.node,
            width = mask.content.width.toDouble(),
            height = mask.content.height.toDouble(),
            component = mask.component.rawValue,
            oneMinus = mask.mode == MTIMaskMode.oneMinusMaskValue
        )
    }
}

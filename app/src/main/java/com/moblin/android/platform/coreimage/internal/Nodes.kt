package com.moblin.android.platform.coreimage.internal

import android.graphics.Bitmap
import com.moblin.android.platform.coregraphics.CGAffineTransform
import com.moblin.android.platform.coregraphics.CGImagePropertyOrientation
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.video.CVPixelBuffer
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

internal enum class SourceEncoding {
    srgb,
    raw,
    srgbAlways,
    working,
}

internal enum class SourceAlpha {
    asIs,
    premultiply,
    opaque,
}

internal abstract class ImageNode {
    abstract val extent: CGRect

    open val inputs: List<ImageNode>
        get() = emptyList()

    open val isFusable: Boolean
        get() = true

    open val isStatic: Boolean
        get() = inputs.all { it.isStatic }
}

internal class PixelBufferNode(
    val buffer: CVPixelBuffer,
    val alpha: SourceAlpha,
    val encoding: SourceEncoding,
) : ImageNode() {
    override val extent = CGRect(0.0, 0.0, buffer.width.toDouble(), buffer.height.toDouble())

    override val isStatic: Boolean
        get() = false
}

internal class BitmapNode(
    val bitmap: Bitmap,
    val alpha: SourceAlpha,
    val encoding: SourceEncoding,
) : ImageNode() {
    override val extent = CGRect(0.0, 0.0, bitmap.width.toDouble(), bitmap.height.toDouble())
}

internal class ConstantNode(
    val red: Double,
    val green: Double,
    val blue: Double,
    val alpha: Double,
    val encoding: SourceEncoding,
    override val extent: CGRect,
) : ImageNode()

internal class EmptyNode : ImageNode() {
    override val extent: CGRect = CGRect.nullRect
}

internal class TransformNode(
    val input: ImageNode,
    val transform: CGAffineTransform,
    val highQuality: Boolean,
) : ImageNode() {
    override val extent: CGRect = input.extent.applying(transform)
    override val inputs: List<ImageNode>
        get() = listOf(input)

    private var pyramid: PyramidNode? = null
    private var pyramidComputed = false

    fun pyramidInput(): PyramidNode? {
        if (!pyramidComputed) {
            pyramidComputed = true
            pyramid = makePyramid()
        }
        return pyramid
    }

    private fun makePyramid(): PyramidNode? {
        if (!highQuality) {
            return null
        }
        val scaleX = sqrt(transform.a * transform.a + transform.b * transform.b)
        val scaleY = sqrt(transform.c * transform.c + transform.d * transform.d)
        val scale = min(scaleX, scaleY)
        if (scale <= 0.0 || scale >= 0.5 || scale.isNaN()) {
            return null
        }
        val level = ceil(ln(0.5 / scale) / ln(2.0) - 1e-9).toInt().coerceIn(1, 12)
        return PyramidNode(input, level)
    }
}

internal class CropNode(val input: ImageNode, val rect: CGRect) : ImageNode() {
    override val extent: CGRect = input.extent.intersection(rect)
    override val inputs: List<ImageNode>
        get() = listOf(input)
}

internal class ClampNode(val input: ImageNode) : ImageNode() {
    override val extent: CGRect = if (input.extent.isNull) CGRect.nullRect else CGRect.infinite
    override val inputs: List<ImageNode>
        get() = listOf(input)
}

internal class SamplingNode(val input: ImageNode, val nearest: Boolean) : ImageNode() {
    override val extent: CGRect = input.extent
    override val inputs: List<ImageNode>
        get() = listOf(input)
}

internal class ColorOpNode(val input: ImageNode, val op: ColorOp, extent: CGRect? = null) : ImageNode() {
    override val extent: CGRect = extent ?: input.extent
    override val inputs: List<ImageNode>
        get() = listOf(input)
}

internal class WarpNode(val input: ImageNode, val warp: WarpOp, override val extent: CGRect) : ImageNode() {
    override val inputs: List<ImageNode>
        get() = listOf(input)
}

internal class GeneratorNode(val generator: GeneratorOp, override val extent: CGRect) : ImageNode()

internal class CombineNode(
    override val inputs: List<ImageNode>,
    val combiner: CombineOp,
    override val extent: CGRect,
) : ImageNode()

internal class CompositeNode(val foreground: ImageNode, val background: ImageNode) : ImageNode() {
    override val extent: CGRect = foreground.extent.union(background.extent)
    override val inputs: List<ImageNode>
        get() = listOf(foreground, background)
}

internal class BlurNode(
    val input: ImageNode,
    val sigma: Double,
    val clampEdges: Boolean,
    override val extent: CGRect,
) : ImageNode() {
    override val inputs: List<ImageNode>
        get() = listOf(input)
    override val isFusable: Boolean
        get() = false
}

internal class PyramidNode(val input: ImageNode, val level: Int) : ImageNode() {
    override val extent: CGRect = input.extent
    override val inputs: List<ImageNode>
        get() = listOf(input)
    override val isFusable: Boolean
        get() = false
}

internal enum class LayerBlend {
    normal,
    other,
}

internal class MaskSpec(
    val content: ImageNode,
    val width: Double,
    val height: Double,
    val component: Int,
    val oneMinus: Boolean,
)

internal class LayerSpec(
    val content: ImageNode,
    val contentWidth: Double,
    val contentHeight: Double,
    val regionMinX: Double,
    val regionMinY: Double,
    val regionMaxX: Double,
    val regionMaxY: Double,
    val flipHorizontally: Boolean,
    val flipVertically: Boolean,
    val mask: MaskSpec?,
    val compositingMask: MaskSpec?,
    val centerX: Double,
    val centerY: Double,
    val width: Double,
    val height: Double,
    val rotation: Double,
    val opacity: Double,
    val cornerRadius: FloatArray,
    val continuousCorners: Boolean,
    val tint: FloatArray,
    val blend: LayerBlend,
)

internal class LayersNode(
    val background: ImageNode,
    val layers: List<LayerSpec>,
    val canvasWidth: Double,
    val canvasHeight: Double,
) : ImageNode() {
    override val extent: CGRect = CGRect(0.0, 0.0, canvasWidth, canvasHeight)
    override val inputs: List<ImageNode>
        get() = listOf(background) + layers.map { it.content } +
            layers.mapNotNull { it.mask?.content } + layers.mapNotNull { it.compositingMask?.content }
    override val isFusable: Boolean
        get() = false
}

internal class CiBoundaryNode(
    val ciNode: ImageNode,
    val bounds: CGRect,
    val opaque: Boolean,
    val width: Int,
    val height: Int,
) : ImageNode() {
    override val extent: CGRect = CGRect(0.0, 0.0, width.toDouble(), height.toDouble())
    override val inputs: List<ImageNode>
        get() = listOf(ciNode)
    override val isFusable: Boolean
        get() = false

    var cached: Intermediate? = null
    var cachedMode: RenderMode? = null
    var slot: TextureSlot? = null
}

internal abstract class CustomNode : ImageNode() {
    override val isFusable: Boolean
        get() = false

    abstract fun render(context: RenderContext, target: Intermediate)
}

internal abstract class ColorOp {
    abstract fun emit(builder: ShaderBuilder, name: String)

    open fun roi(region: CGRect): CGRect {
        return region
    }
}

internal abstract class WarpOp {
    abstract fun emit(builder: ShaderBuilder, name: String)

    open fun roi(region: CGRect, inputExtent: CGRect): CGRect? {
        return null
    }
}

internal abstract class GeneratorOp {
    abstract fun emit(builder: ShaderBuilder, name: String)
}

internal abstract class CombineOp {
    abstract fun emit(builder: ShaderBuilder, name: String, inputs: List<String>)

    open fun roi(index: Int, region: CGRect): CGRect {
        return region
    }
}

internal class ColorMatrixOp(
    val red: FloatArray,
    val green: FloatArray,
    val blue: FloatArray,
    val alpha: FloatArray,
    val bias: FloatArray,
) : ColorOp() {
    override fun emit(builder: ShaderBuilder, name: String) {
        val matrix = builder.uniformMat4(
            floatArrayOf(
                red[0], green[0], blue[0], alpha[0],
                red[1], green[1], blue[1], alpha[1],
                red[2], green[2], blue[2], alpha[2],
                red[3], green[3], blue[3], alpha[3],
            )
        )
        val offset = builder.uniform4f(bias[0], bias[1], bias[2], bias[3])
        builder.function(
            "vec4 $name(vec4 c, vec2 p) {\n" +
                "    vec4 s = mbUnpremultiply(c);\n" +
                "    vec4 r = $matrix * s + $offset;\n" +
                "    r.a = clamp(r.a, 0.0, 1.0);\n" +
                "    return vec4(r.rgb * r.a, r.a);\n" +
                "}\n"
        )
    }
}

internal class PremultiplyOp : ColorOp() {
    override fun emit(builder: ShaderBuilder, name: String) {
        builder.function("vec4 $name(vec4 c, vec2 p) {\n    return vec4(c.rgb * c.a, c.a);\n}\n")
    }
}

internal class UnpremultiplyOp : ColorOp() {
    override fun emit(builder: ShaderBuilder, name: String) {
        builder.function(
            "vec4 $name(vec4 c, vec2 p) {\n    return c.a > 0.0 ? vec4(c.rgb / c.a, c.a) : vec4(0.0);\n}\n"
        )
    }
}

internal class AlphaOneOp(val rect: CGRect?) : ColorOp() {
    override fun emit(builder: ShaderBuilder, name: String) {
        if (rect == null || rect.isInfinite) {
            builder.function("vec4 $name(vec4 c, vec2 p) {\n    return vec4(c.rgb, 1.0);\n}\n")
            return
        }
        if (rect.isNull) {
            builder.function("vec4 $name(vec4 c, vec2 p) {\n    return vec4(0.0);\n}\n")
            return
        }
        val bounds = builder.uniformRect(rect)
        builder.function(
            "vec4 $name(vec4 c, vec2 p) {\n" +
                "    if (!mbInside(p, $bounds)) {\n        return vec4(0.0);\n    }\n" +
                "    return vec4(c.rgb, 1.0);\n" +
                "}\n"
        )
    }
}

internal class UnpremultiplyAlphaOneOp : ColorOp() {
    override fun emit(builder: ShaderBuilder, name: String) {
        builder.function(
            "vec4 $name(vec4 c, vec2 p) {\n" +
                "    return c.a > 0.0 ? vec4(c.rgb / c.a, 1.0) : vec4(0.0, 0.0, 0.0, 1.0);\n" +
                "}\n"
        )
    }
}

internal class RoundedRectangleGenerator(
    val rect: CGRect,
    val radius: Double,
    val red: Double,
    val green: Double,
    val blue: Double,
    val alpha: Double,
) : GeneratorOp() {
    override fun emit(builder: ShaderBuilder, name: String) {
        if (rect.isNull || rect.isEmpty) {
            builder.function("vec4 $name(vec2 p) {\n    return vec4(0.0);\n}\n")
            return
        }
        val color = builder.workingColor(red, green, blue, alpha, SourceEncoding.srgb)
        val center = builder.uniform2f(rect.midX.toFloat(), rect.midY.toFloat())
        val halfSize = builder.uniform2f((rect.width / 2).toFloat(), (rect.height / 2).toFloat())
        val clampedRadius = max(0.0, min(radius, min(rect.width, rect.height) / 2))
        val r = builder.uniform1f(clampedRadius.toFloat())
        builder.function(
            "vec4 $name(vec2 p) {\n" +
                "    vec2 q = abs(p - $center) - ($halfSize - vec2($r));\n" +
                "    float d = length(max(q, vec2(0.0))) + min(max(q.x, q.y), 0.0) - $r;\n" +
                "    float coverage = clamp(0.5 - d, 0.0, 1.0);\n" +
                "    return $color * coverage;\n" +
                "}\n"
        )
    }
}

internal class RadialGradientGenerator(
    val centerX: Double,
    val centerY: Double,
    val radius0: Double,
    val radius1: Double,
    val color0: DoubleArray,
    val color1: DoubleArray,
) : GeneratorOp() {
    override fun emit(builder: ShaderBuilder, name: String) {
        val c0 = builder.workingColor(color0[0], color0[1], color0[2], color0[3], SourceEncoding.srgb)
        val c1 = builder.workingColor(color1[0], color1[1], color1[2], color1[3], SourceEncoding.srgb)
        val center = builder.uniform2f(centerX.toFloat(), centerY.toFloat())
        val radii = builder.uniform2f(radius0.toFloat(), radius1.toFloat())
        builder.function(
            "vec4 $name(vec2 p) {\n" +
                "    float d = distance(p, $center);\n" +
                "    float span = $radii.y - $radii.x;\n" +
                "    float t = abs(span) < 1e-6 ? step($radii.x, d) : clamp((d - $radii.x) / span, 0.0, 1.0);\n" +
                "    return mix($c0, $c1, t);\n" +
                "}\n"
        )
    }
}

internal class SourceOverCombine : CombineOp() {
    override fun emit(builder: ShaderBuilder, name: String, inputs: List<String>) {
        builder.function(
            "vec4 $name(vec2 p) {\n" +
                "    vec4 a = ${inputs[0]}(p);\n" +
                "    vec4 b = ${inputs[1]}(p);\n" +
                "    return a + b * (1.0 - a.a);\n" +
                "}\n"
        )
    }
}

internal class BlendWithMaskCombine : CombineOp() {
    override fun emit(builder: ShaderBuilder, name: String, inputs: List<String>) {
        builder.function(
            "vec4 $name(vec2 p) {\n" +
                "    vec4 foreground = ${inputs[0]}(p);\n" +
                "    vec4 background = ${inputs[1]}(p);\n" +
                "    float m = clamp(${inputs[2]}(p).g, 0.0, 1.0);\n" +
                "    return mix(background, foreground, m);\n" +
                "}\n"
        )
    }
}

internal class MixCombine(val amount: Double) : CombineOp() {
    override fun emit(builder: ShaderBuilder, name: String, inputs: List<String>) {
        val t = builder.uniform1f(amount.toFloat())
        builder.function(
            "vec4 $name(vec2 p) {\n    return mix(${inputs[0]}(p), ${inputs[1]}(p), $t);\n}\n"
        )
    }
}

internal fun orientationTransform(orientation: CGImagePropertyOrientation, extent: CGRect): CGAffineTransform {
    val w = extent.width
    val h = extent.height
    val base = when (orientation) {
        CGImagePropertyOrientation.up -> CGAffineTransform(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)
        CGImagePropertyOrientation.upMirrored -> CGAffineTransform(-1.0, 0.0, 0.0, 1.0, w, 0.0)
        CGImagePropertyOrientation.down -> CGAffineTransform(-1.0, 0.0, 0.0, -1.0, w, h)
        CGImagePropertyOrientation.downMirrored -> CGAffineTransform(1.0, 0.0, 0.0, -1.0, 0.0, h)
        CGImagePropertyOrientation.leftMirrored -> CGAffineTransform(0.0, -1.0, -1.0, 0.0, h, w)
        CGImagePropertyOrientation.right -> CGAffineTransform(0.0, -1.0, 1.0, 0.0, 0.0, w)
        CGImagePropertyOrientation.rightMirrored -> CGAffineTransform(0.0, 1.0, 1.0, 0.0, 0.0, 0.0)
        CGImagePropertyOrientation.left -> CGAffineTransform(0.0, 1.0, -1.0, 0.0, h, 0.0)
    }
    if (extent.minX == 0.0 && extent.minY == 0.0) {
        return base
    }
    return CGAffineTransform(1.0, 0.0, 0.0, 1.0, -extent.minX, -extent.minY).concatenating(base)
}

internal fun orientedExtent(orientation: CGImagePropertyOrientation, extent: CGRect): CGRect {
    if (extent.isNull) {
        return CGRect.nullRect
    }
    if (extent.isInfinite) {
        return CGRect.infinite
    }
    return when (orientation) {
        CGImagePropertyOrientation.up,
        CGImagePropertyOrientation.upMirrored,
        CGImagePropertyOrientation.down,
        CGImagePropertyOrientation.downMirrored,
        -> CGRect(0.0, 0.0, extent.width, extent.height)
        else -> CGRect(0.0, 0.0, extent.height, extent.width)
    }
}

internal fun orientedNode(input: ImageNode, orientation: CGImagePropertyOrientation): ImageNode {
    val extent = input.extent
    if (extent.isNull || extent.isInfinite) {
        return TransformNode(input, CGAffineTransform.identity, false)
    }
    val transformed = TransformNode(input, orientationTransform(orientation, extent), false)
    return CropNode(transformed, orientedExtent(orientation, extent))
}

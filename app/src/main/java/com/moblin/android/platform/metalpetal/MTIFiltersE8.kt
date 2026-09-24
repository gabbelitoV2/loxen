package com.moblin.android.platform.metalpetal

import android.graphics.Bitmap
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.coreimage.CubeTextureSource
import com.moblin.android.platform.coreimage.PinchWarp
import com.moblin.android.platform.coreimage.TwirlWarp
import com.moblin.android.platform.coreimage.circleRoi
import com.moblin.android.platform.coreimage.internal.BitmapNode
import com.moblin.android.platform.coreimage.internal.ColorOp
import com.moblin.android.platform.coreimage.internal.ColorOpNode
import com.moblin.android.platform.coreimage.internal.CombineNode
import com.moblin.android.platform.coreimage.internal.CombineOp
import com.moblin.android.platform.coreimage.internal.EffectsLog
import com.moblin.android.platform.coreimage.internal.ShaderBuilder
import com.moblin.android.platform.coreimage.internal.WarpNode
import com.moblin.android.platform.coreimage.internal.WarpOp
import com.moblin.android.platform.simd.SIMD2
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val METALPETAL_BLEND_FUNCTIONS = """
vec4 mpNormalBlend(vec4 cb, vec4 cs) {
    vec4 dst = vec4(cb.rgb * cb.a, cb.a);
    vec4 src = vec4(cs.rgb * cs.a, cs.a);
    vec4 r = src + dst * (1.0 - src.a);
    return vec4(r.rgb / max(r.a, 0.00001), r.a);
}
vec4 mpBlendBaseAlpha(vec4 cb, vec4 cs, vec3 b) {
    vec4 cr = vec4((1.0 - cb.a) * cs.rgb + cb.a * clamp(b, 0.0, 1.0), cs.a);
    return mpNormalBlend(cb, cr);
}
float mpOverlayChannel(float b, float s) {
    return b < 0.5 ? (2.0 * s * b) : (1.0 - 2.0 * (1.0 - b) * (1.0 - s));
}
vec4 mpMultiplyBlend(vec4 cb, vec4 cs) {
    return mpBlendBaseAlpha(cb, cs, clamp(cb.rgb * cs.rgb, 0.0, 1.0));
}
vec4 mpScreenBlend(vec4 cb, vec4 cs) {
    return mpBlendBaseAlpha(cb, cs, vec3(1.0) - (vec3(1.0) - cs.rgb) * (vec3(1.0) - cb.rgb));
}
vec4 mpOverlayBlend(vec4 cb, vec4 cs) {
    vec3 b = vec3(mpOverlayChannel(cb.r, cs.r), mpOverlayChannel(cb.g, cs.g), mpOverlayChannel(cb.b, cs.b));
    return mpBlendBaseAlpha(cb, cs, b);
}
"""

private fun fullExtent(width: Int, height: Int): CGRect {
    return CGRect(0.0, 0.0, width.toDouble(), height.toDouble())
}

private fun scaledRegion(region: CGRect, scaleX: Double, scaleY: Double): CGRect {
    if (region.isNull || region.isInfinite) {
        return region
    }
    return CGRect(region.minX * scaleX, region.minY * scaleY, region.width * scaleX, region.height * scaleY)
        .insetBy(-1.0, -1.0)
}

private fun sizeScale(builder: ShaderBuilder, scaleX: Double, scaleY: Double): String {
    return builder.uniform2f(scaleX.toFloat(), scaleY.toFloat())
}

private class MetalPetalPixellateWarp(
    private val scaleX: Float,
    private val scaleY: Float,
    private val height: Int,
) : WarpOp() {
    override fun emit(builder: ShaderBuilder, name: String) {
        val scale = builder.uniform2f(scaleX, scaleY)
        val imageHeight = builder.uniform1f(height.toFloat())
        builder.function(
            "vec2 $name(vec2 p) {\n" +
                "    vec2 q = vec2(p.x, $imageHeight - p.y);\n" +
                "    vec2 s = q - mod(q, $scale) + $scale * 0.5;\n" +
                "    return vec2(s.x, $imageHeight - s.y);\n" +
                "}\n"
        )
    }

    override fun roi(region: CGRect, inputExtent: CGRect): CGRect? {
        return region.insetBy(-scaleX.toDouble() - 1, -scaleY.toDouble() - 1)
    }
}

private class BulgeWarp(
    private val centerX: Double,
    private val centerY: Double,
    private val radius: Double,
    private val scale: Double,
) : WarpOp() {
    override fun emit(builder: ShaderBuilder, name: String) {
        val center = builder.uniform2f(centerX.toFloat(), centerY.toFloat())
        val parameters = builder.uniform2f(radius.toFloat(), scale.toFloat())
        builder.function(
            "vec2 $name(vec2 p) {\n" +
                "    float d = distance(p, $center);\n" +
                "    if (d < $parameters.x) {\n" +
                "        float percent = 1.0 - (($parameters.x - d) / $parameters.x) * $parameters.y;\n" +
                "        percent = percent * percent;\n" +
                "        return $center + (p - $center) * percent;\n" +
                "    }\n" +
                "    return p;\n" +
                "}\n"
        )
    }

    override fun roi(region: CGRect, inputExtent: CGRect): CGRect? {
        if (scale < 0 || scale > 2 || radius < 0) {
            return null
        }
        return circleRoi(region, centerX, centerY, radius)
    }
}

private class CrtCombine(
    private val width: Int,
    private val height: Int,
    private val barrelStrength: Float,
) : CombineOp() {
    override fun emit(builder: ShaderBuilder, name: String, inputs: List<String>) {
        val size = builder.uniform3f(width.toFloat(), height.toFloat(), barrelStrength)
        val source = inputs[0]
        builder.function(
            "vec4 $name(vec2 p) {\n" +
                "    vec2 uv = vec2(p.x / $size.x, ($size.y - p.y) / $size.y);\n" +
                "    float cropUvX = (uv.x - 0.125) / 0.75;\n" +
                "    if (cropUvX < 0.0 || cropUvX > 1.0) {\n" +
                "        return vec4(0.0, 0.0, 0.0, 1.0);\n" +
                "    }\n" +
                "    vec2 coord = vec2(cropUvX, uv.y) - vec2(0.5);\n" +
                "    float r2 = dot(coord, coord);\n" +
                "    vec2 distorted = coord * (1.0 + $size.z * r2) + vec2(0.5);\n" +
                "    if (distorted.x < 0.0 || distorted.x > 1.0 || distorted.y < 0.0 || distorted.y > 1.0) {\n" +
                "        return vec4(0.0, 0.0, 0.0, 1.0);\n" +
                "    }\n" +
                "    vec2 sampleUv = vec2(distorted.x * 0.75 + 0.125, distorted.y);\n" +
                "    vec4 color = mbUnpremultiply($source(vec2(sampleUv.x * $size.x, $size.y - sampleUv.y * $size.y)));\n" +
                "    float scanlineWidth = max(1.0, $size.y / 240.0);\n" +
                "    float scanlinePhase = mod(distorted.y * $size.y, scanlineWidth * 2.0);\n" +
                "    if (scanlinePhase < scanlineWidth) {\n" +
                "        color.rgb *= 0.75;\n" +
                "    }\n" +
                "    float luminance = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));\n" +
                "    color.rgb = mix(vec3(luminance), color.rgb, 0.7);\n" +
                "    color.rgb = (color.rgb - 0.5) * 1.05 + 0.5;\n" +
                "    color.rgb += -0.02;\n" +
                "    float vignetteDistance = length(vec2(cropUvX, uv.y) - vec2(0.5)) * 2.0;\n" +
                "    color.rgb *= 1.0 - smoothstep(0.5, 1.5, vignetteDistance);\n" +
                "    color.rgb = clamp(color.rgb, 0.0, 1.0);\n" +
                "    return vec4(color.rgb * color.a, color.a);\n" +
                "}\n"
        )
    }

    override fun roi(index: Int, region: CGRect): CGRect {
        return fullExtent(width, height)
    }
}

private class ChromaKeyCombine(
    private val color: MTIColor,
    private val thresholdSensitivity: Float,
    private val smoothing: Float,
    private val backgroundScaleX: Double,
    private val backgroundScaleY: Double,
) : CombineOp() {
    override fun emit(builder: ShaderBuilder, name: String, inputs: List<String>) {
        val key = builder.uniform3f(color.red, color.green, color.blue)
        val parameters = builder.uniform2f(thresholdSensitivity, smoothing)
        val scale = sizeScale(builder, backgroundScaleX, backgroundScaleY)
        builder.function(
            "vec4 $name(vec2 p) {\n" +
                "    vec4 t = mbUnpremultiply(${inputs[0]}(p));\n" +
                "    vec4 t2 = mbUnpremultiply(${inputs[1]}(p * $scale));\n" +
                "    float maskY = 0.2989 * $key.r + 0.5866 * $key.g + 0.1145 * $key.b;\n" +
                "    float maskCr = 0.7132 * ($key.r - maskY);\n" +
                "    float maskCb = 0.5647 * ($key.b - maskY);\n" +
                "    float y = 0.2989 * t.r + 0.5866 * t.g + 0.1145 * t.b;\n" +
                "    float cr = 0.7132 * (t.r - y);\n" +
                "    float cb = 0.5647 * (t.b - y);\n" +
                "    float blendValue = 1.0 - smoothstep($parameters.x, $parameters.x + $parameters.y,\n" +
                "        distance(vec2(cr, cb), vec2(maskCr, maskCb)));\n" +
                "    vec4 r = mix(t, t2, blendValue);\n" +
                "    return vec4(r.rgb * r.a, r.a);\n" +
                "}\n"
        )
    }

    override fun roi(index: Int, region: CGRect): CGRect {
        return if (index == 1) scaledRegion(region, backgroundScaleX, backgroundScaleY) else region
    }
}

private class MetalPetalBlendCombine(
    private val mode: MTIBlendMode,
    private val intensity: Float,
    private val opaqueOutput: Boolean,
    private val sourceScaleX: Double,
    private val sourceScaleY: Double,
) : CombineOp() {
    override fun emit(builder: ShaderBuilder, name: String, inputs: List<String>) {
        builder.includeOnce("mpBlend", METALPETAL_BLEND_FUNCTIONS)
        val amount = builder.uniform1f(intensity)
        val scale = sizeScale(builder, sourceScaleX, sourceScaleY)
        val function = when (mode) {
            MTIBlendMode.multiply -> "mpMultiplyBlend"
            MTIBlendMode.screen -> "mpScreenBlend"
            MTIBlendMode.overlay -> "mpOverlayBlend"
            else -> "mpNormalBlend"
        }
        val output = if (opaqueOutput) "vec4(o.rgb, 1.0)" else "vec4(o.rgb * o.a, o.a)"
        builder.function(
            "vec4 $name(vec2 p) {\n" +
                "    vec4 cb = mbUnpremultiply(${inputs[0]}(p));\n" +
                "    vec4 cf = mbUnpremultiply(${inputs[1]}(p * $scale));\n" +
                "    vec4 o = mix(cb, $function(cb, cf), $amount);\n" +
                "    o.a = clamp(o.a, 0.0, 1.0);\n" +
                "    return $output;\n" +
                "}\n"
        )
    }

    override fun roi(index: Int, region: CGRect): CGRect {
        return if (index == 1) scaledRegion(region, sourceScaleX, sourceScaleY) else region
    }
}

private class MetalPetalBlendWithMaskCombine(
    private val component: Int,
    private val oneMinus: Boolean,
    private val imageScaleX: Double,
    private val imageScaleY: Double,
    private val maskScaleX: Double,
    private val maskScaleY: Double,
) : CombineOp() {
    override fun emit(builder: ShaderBuilder, name: String, inputs: List<String>) {
        val imageScale = sizeScale(builder, imageScaleX, imageScaleY)
        val maskScale = sizeScale(builder, maskScaleX, maskScaleY)
        val maskValue = if (oneMinus) "(1.0 - m[$component])" else "m[$component]"
        builder.function(
            "vec4 $name(vec2 p) {\n" +
                "    vec4 overlay = mbUnpremultiply(${inputs[0]}(p * $imageScale));\n" +
                "    vec4 m = mbUnpremultiply(${inputs[1]}(p * $maskScale));\n" +
                "    vec4 base = mbUnpremultiply(${inputs[2]}(p));\n" +
                "    overlay.a = clamp(overlay.a * $maskValue, 0.0, 1.0);\n" +
                "    vec4 src = vec4(overlay.rgb * overlay.a, overlay.a);\n" +
                "    return src + vec4(base.rgb * base.a, base.a) * (1.0 - src.a);\n" +
                "}\n"
        )
    }

    override fun roi(index: Int, region: CGRect): CGRect {
        return when (index) {
            0 -> scaledRegion(region, imageScaleX, imageScaleY)
            1 -> scaledRegion(region, maskScaleX, maskScaleY)
            else -> region
        }
    }
}

private class MetalPetalLookupOp(
    private val source: CubeTextureSource,
    private val intensity: Float,
) : ColorOp() {
    override fun emit(builder: ShaderBuilder, name: String) {
        val texture = source.texture()
        if (texture == 0) {
            builder.function("vec4 $name(vec4 c, vec2 p) {\n    return c;\n}\n")
            return
        }
        val sampler = builder.sampler3D(texture)
        val dimension = source.dimension.toFloat()
        val parameters = builder.uniform3f(dimension - 1f, dimension, intensity)
        builder.function(
            "vec4 $name(vec4 c, vec2 p) {\n" +
                "    vec4 s = mbUnpremultiply(c);\n" +
                "    vec4 q = texture($sampler, (clamp(s.rgb, 0.0, 1.0) * $parameters.x + 0.5) / $parameters.y);\n" +
                "    vec3 rgb = mix(s.rgb, q.rgb, $parameters.z);\n" +
                "    return vec4(rgb * s.a, s.a);\n" +
                "}\n"
        )
    }
}

private fun lookupTableBuffer(bitmap: Bitmap, type: MTIColorLookupTableType, dimension: Int): Buffer? {
    if (bitmap.isRecycled) {
        return null
    }
    val readable = if (bitmap.config == Bitmap.Config.HARDWARE) {
        bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return null
    } else {
        bitmap
    }
    val width = readable.width
    val height = readable.height
    val pixels = IntArray(width * height)
    readable.getPixels(pixels, 0, width, 0, 0, width, height)
    if (readable !== bitmap) {
        readable.recycle()
    }
    val rows = sqrt(dimension.toDouble()).roundToInt()
    val output = ByteBuffer.allocateDirect(dimension * dimension * dimension * 4).order(ByteOrder.nativeOrder())
    for (blue in 0 until dimension) {
        for (green in 0 until dimension) {
            for (red in 0 until dimension) {
                val x: Int
                val y: Int
                when (type) {
                    MTIColorLookupTableType.type2DHorizontalStrip -> {
                        x = blue * dimension + red
                        y = green
                    }
                    MTIColorLookupTableType.type2DVerticalStrip -> {
                        x = red
                        y = blue * dimension + green
                    }
                    else -> {
                        x = (blue % rows) * dimension + red
                        y = (blue / rows) * dimension + green
                    }
                }
                val color = if (x < width && y < height) pixels[y * width + x] else 0
                output.put(((color shr 16) and 0xFF).toByte())
                output.put(((color shr 8) and 0xFF).toByte())
                output.put((color and 0xFF).toByte())
                output.put(((color ushr 24) and 0xFF).toByte())
            }
        }
    }
    output.position(0)
    return output
}

class MTIPixellateFilter : MTIUnaryImageRenderingFilter() {
    var scale: SIMD2 = SIMD2(16f, 16f)

    override val outputImage: MTIImage?
        get() {
            val image = inputImage ?: return null
            if (!(scale.x > 0f && scale.y > 0f)) {
                return image
            }
            val warp = MetalPetalPixellateWarp(scale.x, scale.y, image.height)
            val extent = fullExtent(image.width, image.height)
            return MTIImage(WarpNode(image.node, warp, extent), image.alphaType, image.width, image.height)
        }
}

class MTIPinchDistortionFilter : MTIUnaryImageRenderingFilter() {
    var center: SIMD2 = SIMD2(0f, 0f)
    var radius: Float = 0f
    var scale: Float = 0f

    override val outputImage: MTIImage?
        get() {
            val image = inputImage ?: return null
            val warp = PinchWarp(
                center.x.toDouble(),
                image.height - center.y.toDouble(),
                radius.toDouble(),
                scale.toDouble()
            )
            val extent = fullExtent(image.width, image.height)
            return MTIImage(WarpNode(image.node, warp, extent), image.alphaType, image.width, image.height)
        }
}

class MTITwirlDistortionFilter : MTIUnaryImageRenderingFilter() {
    var center: SIMD2 = SIMD2(0f, 0f)
    var radius: Float = 0f
    var angle: Float = 0f

    override val outputImage: MTIImage?
        get() {
            val image = inputImage ?: return null
            val warp = TwirlWarp(
                center.x.toDouble(),
                image.height - center.y.toDouble(),
                radius.toDouble(),
                angle.toDouble()
            )
            val extent = fullExtent(image.width, image.height)
            return MTIImage(WarpNode(image.node, warp, extent), image.alphaType, image.width, image.height)
        }
}

class MTIBulgeDistortionFilter : MTIUnaryImageRenderingFilter() {
    var center: SIMD2 = SIMD2(0f, 0f)
    var radius: Float = 0f
    var scale: Float = 0f

    override val outputImage: MTIImage?
        get() {
            val image = inputImage ?: return null
            val warp = BulgeWarp(
                center.x.toDouble(),
                image.height - center.y.toDouble(),
                radius.toDouble(),
                scale.toDouble()
            )
            val extent = fullExtent(image.width, image.height)
            return MTIImage(WarpNode(image.node, warp, extent), image.alphaType, image.width, image.height)
        }
}

class MTICrtFilter : MTIUnaryImageRenderingFilter() {
    var barrelStrength: Float = 0.1f

    override val outputImage: MTIImage?
        get() {
            val image = inputImage ?: return null
            if (image.width <= 0 || image.height <= 0) {
                return null
            }
            val combine = CrtCombine(image.width, image.height, barrelStrength)
            val node = CombineNode(listOf(image.node), combine, fullExtent(image.width, image.height))
            return MTIImage(node, MTIAlphaType.nonPremultiplied, image.width, image.height)
        }
}

class MTIChromaKeyBlendFilter : MTIFilter() {
    var inputImage: MTIImage? = null
    var inputBackgroundImage: MTIImage? = null
    var thresholdSensitivity: Float = 0.4f
    var smoothing: Float = 0.1f
    var color: MTIColor = MTIColor(0f, 1f, 0f, 1f)

    override val outputImage: MTIImage?
        get() {
            val image = inputImage ?: return null
            val background = inputBackgroundImage ?: return null
            if (image.width <= 0 || image.height <= 0) {
                return null
            }
            val combine = ChromaKeyCombine(
                color,
                thresholdSensitivity,
                smoothing,
                background.width.toDouble() / image.width,
                background.height.toDouble() / image.height
            )
            val node = CombineNode(
                listOf(image.node, background.node),
                combine,
                fullExtent(image.width, image.height)
            )
            return MTIImage(node, MTIAlphaType.nonPremultiplied, image.width, image.height)
        }
}

enum class MTIColorLookupTableType {
    typeUnknown,
    type2DSquare,
    type2DHorizontalStrip,
    type2DVerticalStrip,
    type3D,
}

class MTIColorLookupTableInfo(val type: MTIColorLookupTableType, val dimension: Int) {
    internal constructor(width: Int, height: Int) : this(tableType(width, height), tableDimension(width, height))

    private companion object {
        fun squareDimension(width: Int, height: Int): Int? {
            if (width != height) {
                return null
            }
            val pixels = width.toDouble() * height.toDouble()
            val dimension = pixels.pow(1.0 / 3.0).roundToInt()
            return if (dimension.toDouble() * dimension * dimension == pixels) dimension else null
        }

        fun tableType(width: Int, height: Int): MTIColorLookupTableType {
            if (width == height) {
                return if (squareDimension(width, height) != null) {
                    MTIColorLookupTableType.type2DSquare
                } else {
                    MTIColorLookupTableType.typeUnknown
                }
            }
            return when {
                height.toLong() * height == width.toLong() -> MTIColorLookupTableType.type2DHorizontalStrip
                width.toLong() * width == height.toLong() -> MTIColorLookupTableType.type2DVerticalStrip
                else -> MTIColorLookupTableType.typeUnknown
            }
        }

        fun tableDimension(width: Int, height: Int): Int {
            return when (tableType(width, height)) {
                MTIColorLookupTableType.type2DSquare -> squareDimension(width, height) ?: 0
                MTIColorLookupTableType.type2DHorizontalStrip -> height
                MTIColorLookupTableType.type2DVerticalStrip -> width
                else -> 0
            }
        }
    }
}

class MTIColorLookupFilter : MTIFilter() {
    var inputImage: MTIImage? = null
    var inputColorLookupTable: MTIImage? = null
        set(value) {
            field = value
            inputColorLookupTableInfo = value?.let { MTIColorLookupTableInfo(it.width, it.height) }
        }
    var inputColorLookupTableInfo: MTIColorLookupTableInfo? = null
        private set
    var intensity: Float = 1f

    private var cachedTable: MTIImage? = null
    private var cachedSource: CubeTextureSource? = null

    override val outputImage: MTIImage?
        get() {
            val image = inputImage ?: return null
            val table = inputColorLookupTable ?: return null
            val info = inputColorLookupTableInfo ?: return null
            if (info.type == MTIColorLookupTableType.typeUnknown || info.dimension == 0) {
                return null
            }
            val source = lookupSource(table, info) ?: return null
            val node = ColorOpNode(image.node, MetalPetalLookupOp(source, intensity))
            return MTIImage(node, MTIAlphaType.nonPremultiplied, image.width, image.height)
        }

    private fun lookupSource(table: MTIImage, info: MTIColorLookupTableInfo): CubeTextureSource? {
        val cached = cachedSource
        if (cached != null && cachedTable === table) {
            return cached
        }
        val bitmap = (table.node as? BitmapNode)?.bitmap
        if (bitmap == null) {
            EffectsLog.once(
                "lookup:source",
                "MTIColorLookupFilter: only lookup tables made from a CGImage are supported"
            )
            return null
        }
        val type = info.type
        val dimension = info.dimension
        val source = CubeTextureSource(dimension, false) { lookupTableBuffer(bitmap, type, dimension) }
        cachedTable = table
        cachedSource = source
        return source
    }
}

class MTIBlendFilter(val blendMode: MTIBlendMode) : MTIFilter() {
    var inputImage: MTIImage? = null
    var inputBackgroundImage: MTIImage? = null
    var intensity: Float = 1f
    var outputAlphaType: MTIAlphaType = MTIAlphaType.nonPremultiplied

    override val outputImage: MTIImage?
        get() {
            val background = inputBackgroundImage ?: return null
            val image = inputImage ?: return null
            if (background.width <= 0 || background.height <= 0) {
                return null
            }
            if (blendMode != MTIBlendMode.normal && blendMode != MTIBlendMode.multiply &&
                blendMode != MTIBlendMode.screen && blendMode != MTIBlendMode.overlay
            ) {
                EffectsLog.once("blend:$blendMode", "MTIBlendFilter: blend mode $blendMode is drawn as normal")
            }
            val combine = MetalPetalBlendCombine(
                blendMode,
                intensity,
                outputAlphaType == MTIAlphaType.alphaIsOne,
                image.width.toDouble() / background.width,
                image.height.toDouble() / background.height
            )
            val node = CombineNode(
                listOf(background.node, image.node),
                combine,
                fullExtent(background.width, background.height)
            )
            return MTIImage(node, outputAlphaType, background.width, background.height)
        }
}

class MTIBlendWithMaskFilter : MTIFilter() {
    var inputImage: MTIImage? = null
    var inputBackgroundImage: MTIImage? = null
    var inputMask: MTIMask? = null

    override val outputImage: MTIImage?
        get() {
            val image = inputImage ?: return null
            val mask = inputMask ?: return null
            val background = inputBackgroundImage ?: return null
            if (background.width <= 0 || background.height <= 0) {
                return null
            }
            val combine = MetalPetalBlendWithMaskCombine(
                mask.component.rawValue,
                mask.mode == MTIMaskMode.oneMinusMaskValue,
                image.width.toDouble() / background.width,
                image.height.toDouble() / background.height,
                mask.content.width.toDouble() / background.width,
                mask.content.height.toDouble() / background.height
            )
            val node = CombineNode(
                listOf(image.node, mask.content.node, background.node),
                combine,
                fullExtent(background.width, background.height)
            )
            return MTIImage(node, MTIAlphaType.nonPremultiplied, background.width, background.height)
        }
}

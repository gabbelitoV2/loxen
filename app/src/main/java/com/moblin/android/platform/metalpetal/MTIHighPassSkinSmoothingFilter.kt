package com.moblin.android.platform.metalpetal

import android.graphics.Bitmap
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.coreimage.internal.BitmapNode
import com.moblin.android.platform.coreimage.internal.BlurNode
import com.moblin.android.platform.coreimage.internal.CombineNode
import com.moblin.android.platform.coreimage.internal.CombineOp
import com.moblin.android.platform.coreimage.internal.EffectsLog
import com.moblin.android.platform.coreimage.internal.ShaderBuilder
import com.moblin.android.platform.coreimage.internal.SourceAlpha
import com.moblin.android.platform.coreimage.internal.SourceEncoding
import com.moblin.android.platform.simd.SIMD2

private const val SKIN_SMOOTHING_FUNCTIONS = """
vec3 mpSkinSmoothingHardLight(vec3 m) {
    vec3 low = 2.0 * m * m;
    vec3 high = vec3(1.0) - 2.0 * (vec3(1.0) - m) * (vec3(1.0) - m);
    return clamp(mix(high, low, vec3(lessThan(m, vec3(0.5)))), 0.0, 1.0);
}
"""

private val TONE_CURVE_EXTENT = CGRect(0.0, 0.0, 256.0, 1.0)

private object GreenBlueChannelOverlayCombine : CombineOp() {
    override fun emit(builder: ShaderBuilder, name: String, inputs: List<String>) {
        builder.function(
            "vec4 $name(vec2 p) {\n" +
                "    vec4 c = mbUnpremultiply(${inputs[0]}(p));\n" +
                "    float g = c.g * 0.5;\n" +
                "    float b = c.b * 0.5;\n" +
                "    float o = g < 0.5 ? 2.0 * b * g : 1.0 - 2.0 * (1.0 - g) * (1.0 - b);\n" +
                "    o = clamp(o, 0.0, 1.0);\n" +
                "    return vec4(o, o, o, 1.0);\n" +
                "}\n"
        )
    }
}

private class MaskProcessAndCompositeCombine(private val amount: Float) : CombineOp() {
    override fun emit(builder: ShaderBuilder, name: String, inputs: List<String>) {
        builder.includeOnce("mpSkinSmoothing", SKIN_SMOOTHING_FUNCTIONS)
        val strength = builder.uniform1f(amount)
        val source = inputs[0]
        val overlay = inputs[1]
        val blurred = inputs[2]
        val toneCurve = inputs[3]
        builder.function(
            "vec4 $name(vec2 p) {\n" +
                "    vec4 textureColor = mbUnpremultiply($source(p));\n" +
                "    vec3 index = clamp(textureColor.rgb, 0.0, 1.0) * 255.0 + 0.5;\n" +
                "    float r = $toneCurve(vec2(index.r, 0.5)).r;\n" +
                "    float g = $toneCurve(vec2(index.g, 0.5)).g;\n" +
                "    float b = $toneCurve(vec2(index.b, 0.5)).b;\n" +
                "    vec4 toneCurvedColor = mix(textureColor, vec4(r, g, b, textureColor.a), $strength);\n" +
                "    vec3 mask = clamp($overlay(p).rgb - mbUnpremultiply($blurred(p)).rgb + vec3(0.5), 0.0, 1.0);\n" +
                "    mask = mpSkinSmoothingHardLight(mask);\n" +
                "    mask = mpSkinSmoothingHardLight(mask);\n" +
                "    mask = mpSkinSmoothingHardLight(mask);\n" +
                "    float maskValue = clamp((mask.r - 75.0 / 255.0) * (255.0 / (164.0 - 75.0)), 0.0, 1.0);\n" +
                "    vec4 o = mix(toneCurvedColor, textureColor, maskValue);\n" +
                "    o.a = clamp(o.a, 0.0, 1.0);\n" +
                "    return vec4(o.rgb * o.a, o.a);\n" +
                "}\n"
        )
    }

    override fun roi(index: Int, region: CGRect): CGRect {
        return if (index == 3) TONE_CURVE_EXTENT else region
    }
}

class MTIHighPassSkinSmoothingFilter : MTIFilter() {
    var inputImage: MTIImage? = null
    var amount: Float = 0.65f
    var radius: Float = 8f

    var toneCurveControlPoints: List<SIMD2> = defaultToneCurveControlPoints
        set(value) {
            field = if (value.size >= 2) value else defaultToneCurveControlPoints
        }

    override val outputImage: MTIImage?
        get() {
            val image = inputImage ?: return null
            if (image.width <= 0 || image.height <= 0) {
                return null
            }
            val toneCurve = toneCurveBitmap(toneCurveControlPoints) ?: return null
            val extent = CGRect(0.0, 0.0, image.width.toDouble(), image.height.toDouble())
            val overlay = BlurNode(
                CombineNode(listOf(image.node), GreenBlueChannelOverlayCombine, extent),
                0.0,
                true,
                extent
            )
            val blurFilter = MTIMPSGaussianBlurFilter()
            blurFilter.radius = radius
            blurFilter.inputImage = MTIImage(overlay, MTIAlphaType.nonPremultiplied, image.width, image.height)
            val blurred = blurFilter.outputImage ?: return null
            val toneCurveNode = BitmapNode(toneCurve, SourceAlpha.opaque, SourceEncoding.raw)
            val node = CombineNode(
                listOf(image.node, overlay, blurred.node, toneCurveNode),
                MaskProcessAndCompositeCombine(amount),
                extent
            )
            return MTIImage(node, MTIAlphaType.nonPremultiplied, image.width, image.height)
        }

    companion object {
        internal val defaultToneCurveControlPoints: List<SIMD2> = listOf(
            SIMD2(0f, 0f),
            SIMD2(120f / 255f, 146f / 255f),
            SIMD2(1f, 1f)
        )

        private val toneCurveLock = Any()
        private var cachedToneCurvePoints: List<SIMD2>? = null
        private var cachedToneCurve: Bitmap? = null

        fun isSupported(on: MTLDevice): Boolean {
            return true
        }

        internal fun toneCurveLookupTable(controlPoints: List<SIMD2>): IntArray {
            val redCurve = FloatArray(256)
            val greenCurve = FloatArray(256)
            val blueCurve = FloatArray(256)
            val rgbCurve = preparedSplineCurve(controlPoints)
            val table = IntArray(256 * 3)
            for (index in 0 until 256) {
                val blue = (index.toFloat() + blueCurve[index]).coerceIn(0f, 255f).toInt()
                val green = (index.toFloat() + greenCurve[index]).coerceIn(0f, 255f).toInt()
                val red = (index.toFloat() + redCurve[index]).coerceIn(0f, 255f).toInt()
                table[index * 3] = (red.toFloat() + rgbCurve[red]).coerceIn(0f, 255f).toInt()
                table[index * 3 + 1] = (green.toFloat() + rgbCurve[green]).coerceIn(0f, 255f).toInt()
                table[index * 3 + 2] = (blue.toFloat() + rgbCurve[blue]).coerceIn(0f, 255f).toInt()
            }
            return table
        }

        private fun preparedSplineCurve(controlPoints: List<SIMD2>): FloatArray {
            val curve = FloatArray(256)
            if (controlPoints.size <= 1) {
                return curve
            }
            val points = controlPoints
                .sortedBy { it.x }
                .map { Pair(it.x.toDouble() * 255, it.y.toDouble() * 255) }
            val n = points.size
            val matrix = Array(n) { DoubleArray(3) }
            val result = DoubleArray(n)
            matrix[0][1] = 1.0
            for (i in 1 until n - 1) {
                val p1 = points[i - 1]
                val p2 = points[i]
                val p3 = points[i + 1]
                matrix[i][0] = (p2.first - p1.first) / 6
                matrix[i][1] = (p3.first - p1.first) / 3
                matrix[i][2] = (p3.first - p2.first) / 6
                result[i] = (p3.second - p2.second) / (p3.first - p2.first) -
                    (p2.second - p1.second) / (p2.first - p1.first)
            }
            matrix[n - 1][1] = 1.0
            for (i in 1 until n) {
                val k = matrix[i][0] / matrix[i - 1][1]
                matrix[i][1] -= k * matrix[i - 1][2]
                matrix[i][0] = 0.0
                result[i] -= k * result[i - 1]
            }
            for (i in n - 2 downTo 0) {
                val k = matrix[i][2] / matrix[i + 1][1]
                matrix[i][1] -= k * matrix[i + 1][0]
                matrix[i][2] = 0.0
                result[i] -= k * result[i + 1]
            }
            val secondDerivative = DoubleArray(n) { result[it] / matrix[it][1] }
            fun setCurvePoint(x: Double, y: Double) {
                val index = x.toInt()
                if (index in 0..255) {
                    curve[index] = (y - x).toFloat()
                }
            }
            for (i in 0 until n - 1) {
                val current = points[i]
                val next = points[i + 1]
                var x = current.first.toInt()
                while (x < next.first.toInt()) {
                    val t = (x.toDouble() - current.first) / (next.first - current.first)
                    val a = 1 - t
                    val b = t
                    val h = next.first - current.first
                    var y = a * current.second + b * next.second + (h * h / 6) *
                        ((a * a * a - a) * secondDerivative[i] + (b * b * b - b) * secondDerivative[i + 1])
                    if (y > 255.0) {
                        y = 255.0
                    } else if (y < 0.0) {
                        y = 0.0
                    }
                    setCurvePoint(x.toDouble(), y)
                    x += 1
                }
            }
            setCurvePoint(points[n - 1].first, points[n - 1].second)
            if (points[0].first > 0) {
                var i = points[0].first.toInt()
                while (i >= 0) {
                    setCurvePoint(i.toDouble(), 0.0)
                    i -= 1
                }
            }
            if (points[n - 1].first < 255) {
                var i = points[n - 1].first.toInt() + 1
                while (i <= 255) {
                    setCurvePoint(i.toDouble(), 255.0)
                    i += 1
                }
            }
            return curve
        }

        private fun toneCurveBitmap(controlPoints: List<SIMD2>): Bitmap? {
            synchronized(toneCurveLock) {
                val cached = cachedToneCurve
                if (cached != null && !cached.isRecycled && cachedToneCurvePoints == controlPoints) {
                    return cached
                }
                return try {
                    val table = toneCurveLookupTable(controlPoints)
                    val colors = IntArray(256) { index ->
                        (0xFF shl 24) or
                            (table[index * 3] shl 16) or
                            (table[index * 3 + 1] shl 8) or
                            table[index * 3 + 2]
                    }
                    val bitmap = Bitmap.createBitmap(colors, 256, 1, Bitmap.Config.ARGB_8888)
                    cachedToneCurvePoints = controlPoints.toList()
                    cachedToneCurve = bitmap
                    bitmap
                } catch (error: Throwable) {
                    EffectsLog.once("skinSmoothing:toneCurve", "Skin smoothing tone curve failed: $error")
                    null
                }
            }
        }
    }
}

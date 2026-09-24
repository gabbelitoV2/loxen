package com.moblin.android.platform.coreimage

import android.opengl.GLES20
import android.opengl.GLES30
import com.moblin.android.platform.core.PipelineStats
import com.moblin.android.platform.core.PipelineThread
import com.moblin.android.platform.coregraphics.CGAffineTransform
import com.moblin.android.platform.coregraphics.CGColorSpace
import com.moblin.android.platform.coregraphics.CGPoint
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.coreimage.internal.ClampNode
import com.moblin.android.platform.coreimage.internal.ColorMatrixOp
import com.moblin.android.platform.coreimage.internal.ColorOp
import com.moblin.android.platform.coreimage.internal.ColorOpNode
import com.moblin.android.platform.coreimage.internal.CropNode
import com.moblin.android.platform.coreimage.internal.EffectsLog
import com.moblin.android.platform.coreimage.internal.GeneratorNode
import com.moblin.android.platform.coreimage.internal.GeneratorOp
import com.moblin.android.platform.coreimage.internal.Gl
import com.moblin.android.platform.coreimage.internal.RenderMode
import com.moblin.android.platform.coreimage.internal.ShaderBuilder
import com.moblin.android.platform.coreimage.internal.SourceEncoding
import com.moblin.android.platform.coreimage.internal.TransformNode
import com.moblin.android.platform.coreimage.internal.WarpNode
import com.moblin.android.platform.coreimage.internal.WarpOp
import java.lang.ref.PhantomReference
import java.lang.ref.ReferenceQueue
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.pow

private val defaultCenter = CGPoint(150.0, 150.0)

private const val LUMINANCE = "vec3(0.2126, 0.7152, 0.0722)"

private val identityCube2: ByteArray = run {
    val buffer = ByteBuffer.allocate(8 * 16).order(ByteOrder.LITTLE_ENDIAN)
    for (blue in 0..1) {
        for (green in 0..1) {
            for (red in 0..1) {
                buffer.putFloat(red.toFloat())
                buffer.putFloat(green.toFloat())
                buffer.putFloat(blue.toFloat())
                buffer.putFloat(1f)
            }
        }
    }
    buffer.array()
}

private fun anyToCIVector(value: Any?, fallback: CIVector): CIVector {
    return when (value) {
        is CIVector -> value
        is CGPoint -> CIVector(cgPoint = value)
        else -> fallback
    }
}

private fun vectorFloats(vector: CIVector): FloatArray {
    return FloatArray(4) { vector.value(at = it).toFloat() }
}

internal fun circleRoi(region: CGRect, centerX: Double, centerY: Double, radius: Double): CGRect {
    val circle = CGRect(centerX - radius, centerY - radius, 2 * radius, 2 * radius)
    return region.union(circle).insetBy(-2.0, -2.0)
}

internal class PinchWarp(
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
                "    vec2 o = p - $center;\n" +
                "    float d = length(o);\n" +
                "    if (d > 0.0) {\n" +
                "        float distorted = d + (sqrt(d * max($parameters.x, 0.0)) - d) * $parameters.y;\n" +
                "        return $center + o * (distorted / d);\n" +
                "    }\n" +
                "    return p;\n" +
                "}\n"
        )
    }

    override fun roi(region: CGRect, inputExtent: CGRect): CGRect? {
        if (scale < 0 || scale > 1 || radius < 0) {
            return null
        }
        val shrink = 1 - scale
        val inner = CGRect(
            centerX + (region.minX - centerX) * shrink,
            centerY + (region.minY - centerY) * shrink,
            region.width * shrink,
            region.height * shrink
        )
        return circleRoi(region.union(inner), centerX, centerY, radius)
    }
}

internal class TwirlWarp(
    private val centerX: Double,
    private val centerY: Double,
    private val radius: Double,
    private val angle: Double,
) : WarpOp() {
    override fun emit(builder: ShaderBuilder, name: String) {
        val center = builder.uniform2f(centerX.toFloat(), centerY.toFloat())
        val parameters = builder.uniform2f(radius.toFloat(), angle.toFloat())
        builder.function(
            "vec2 $name(vec2 p) {\n" +
                "    if ($parameters.x <= 0.0) {\n" +
                "        return p;\n" +
                "    }\n" +
                "    vec2 o = p - $center;\n" +
                "    float falloff = 1.0 - smoothstep(0.0, 1.0, length(o) / $parameters.x);\n" +
                "    float rotation = -$parameters.y * falloff;\n" +
                "    float s = sin(rotation);\n" +
                "    float c = cos(rotation);\n" +
                "    return $center + vec2(o.x * c - o.y * s, o.x * s + o.y * c);\n" +
                "}\n"
        )
    }

    override fun roi(region: CGRect, inputExtent: CGRect): CGRect? {
        return circleRoi(region, centerX, centerY, radius)
    }
}

private class PixellateWarp(
    private val centerX: Double,
    private val centerY: Double,
    private val scale: Double,
) : WarpOp() {
    override fun emit(builder: ShaderBuilder, name: String) {
        val center = builder.uniform2f(centerX.toFloat(), centerY.toFloat())
        val size = builder.uniform1f(scale.toFloat())
        builder.function(
            "vec2 $name(vec2 p) {\n" +
                "    return $center + (floor((p - $center) / $size) + 0.5) * $size;\n" +
                "}\n"
        )
    }

    override fun roi(region: CGRect, inputExtent: CGRect): CGRect? {
        return region.insetBy(-scale - 1, -scale - 1)
    }
}

private class MonochromeOp(
    private val red: Double,
    private val green: Double,
    private val blue: Double,
    private val intensity: Double,
) : ColorOp() {
    override fun emit(builder: ShaderBuilder, name: String) {
        val color = builder.workingColorValues(red, green, blue, 1.0, SourceEncoding.srgb)
        val tint = builder.uniform3f(color[0], color[1], color[2])
        val amount = builder.uniform1f(intensity.toFloat())
        builder.function(
            "vec4 $name(vec4 c, vec2 p) {\n" +
                "    vec4 s = mbUnpremultiply(c);\n" +
                "    float l = dot(s.rgb, $LUMINANCE);\n" +
                "    vec3 rgb = mix(s.rgb, l * $tint, $amount);\n" +
                "    return vec4(rgb * s.a, s.a);\n" +
                "}\n"
        )
    }
}

private class SepiaOp(private val intensity: Double) : ColorOp() {
    override fun emit(builder: ShaderBuilder, name: String) {
        val amount = builder.uniform1f(intensity.toFloat())
        builder.function(
            "vec4 $name(vec4 c, vec2 p) {\n" +
                "    vec4 s = mbUnpremultiply(c);\n" +
                "    vec3 sepia = vec3(\n" +
                "        dot(s.rgb, vec3(0.393, 0.769, 0.189)),\n" +
                "        dot(s.rgb, vec3(0.349, 0.686, 0.168)),\n" +
                "        dot(s.rgb, vec3(0.272, 0.534, 0.131)));\n" +
                "    vec3 rgb = mix(s.rgb, sepia, $amount);\n" +
                "    return vec4(rgb * s.a, s.a);\n" +
                "}\n"
        )
    }
}

private class ColorControlsOp(
    private val saturation: Double,
    private val brightness: Double,
    private val contrast: Double,
) : ColorOp() {
    override fun emit(builder: ShaderBuilder, name: String) {
        val parameters = builder.uniform3f(saturation.toFloat(), brightness.toFloat(), contrast.toFloat())
        builder.function(
            "vec4 $name(vec4 c, vec2 p) {\n" +
                "    vec4 s = mbUnpremultiply(c);\n" +
                "    float l = dot(s.rgb, $LUMINANCE);\n" +
                "    vec3 rgb = mix(vec3(l), s.rgb, $parameters.x);\n" +
                "    rgb = (rgb - 0.5) * $parameters.z + 0.5;\n" +
                "    rgb += $parameters.y;\n" +
                "    return vec4(rgb * s.a, s.a);\n" +
                "}\n"
        )
    }
}

private class VignetteOp(
    private val centerX: Double,
    private val centerY: Double,
    private val halfDiagonal: Double,
    private val intensity: Double,
    private val radius: Double,
) : ColorOp() {
    override fun emit(builder: ShaderBuilder, name: String) {
        val center = builder.uniform2f(centerX.toFloat(), centerY.toFloat())
        val parameters = builder.uniform3f(
            halfDiagonal.toFloat(),
            intensity.toFloat(),
            (1.0 - radius * 0.5).toFloat()
        )
        builder.function(
            "vec4 $name(vec4 c, vec2 p) {\n" +
                "    float d = distance(p, $center) / $parameters.x;\n" +
                "    float f = max(1.0 - $parameters.y * smoothstep(0.0, 1.0, d - $parameters.z), 0.0);\n" +
                "    return vec4(c.rgb * f, c.a);\n" +
                "}\n"
        )
    }
}

private const val STRIPE_FUNCTIONS = """
float mbStripeMix(float x, float width, float ramp) {
    float u = mod(x, 2.0 * width) / width;
    if (ramp <= 0.0) {
        return step(1.0, u);
    }
    float d = abs(u - 1.5);
    d = min(d, 2.0 - d);
    float h = (0.5 - d) * width;
    return clamp(h / ramp + 0.5, 0.0, 1.0);
}
"""

private class StripesGenerator(
    private val centerX: Double,
    private val width: Double,
    private val sharpness: Double,
    private val color0: CIColor,
    private val color1: CIColor,
) : GeneratorOp() {
    override fun emit(builder: ShaderBuilder, name: String) {
        builder.includeOnce("mbStripeMix", STRIPE_FUNCTIONS)
        val c0 = builder.workingColor(color0.red, color0.green, color0.blue, color0.alpha, SourceEncoding.srgb)
        val c1 = builder.workingColor(color1.red, color1.green, color1.blue, color1.alpha, SourceEncoding.srgb)
        val parameters = builder.uniform3f(
            centerX.toFloat(),
            width.toFloat(),
            ((1.0 - sharpness.coerceIn(0.0, 1.0)) * width).toFloat()
        )
        builder.function(
            "vec4 $name(vec2 p) {\n" +
                "    float m = mbStripeMix(p.x - $parameters.x, $parameters.y, $parameters.z);\n" +
                "    return mix($c0, $c1, m);\n" +
                "}\n"
        )
    }
}

private class CheckerboardGenerator(
    private val centerX: Double,
    private val centerY: Double,
    private val width: Double,
    private val sharpness: Double,
    private val color0: CIColor,
    private val color1: CIColor,
) : GeneratorOp() {
    override fun emit(builder: ShaderBuilder, name: String) {
        builder.includeOnce("mbStripeMix", STRIPE_FUNCTIONS)
        val c0 = builder.workingColor(color0.red, color0.green, color0.blue, color0.alpha, SourceEncoding.srgb)
        val c1 = builder.workingColor(color1.red, color1.green, color1.blue, color1.alpha, SourceEncoding.srgb)
        val center = builder.uniform2f(centerX.toFloat(), centerY.toFloat())
        val parameters = builder.uniform2f(
            width.toFloat(),
            ((1.0 - sharpness.coerceIn(0.0, 1.0)) * width).toFloat()
        )
        builder.function(
            "vec4 $name(vec2 p) {\n" +
                "    vec2 q = p - $center;\n" +
                "    float mx = mbStripeMix(q.x, $parameters.x, $parameters.y);\n" +
                "    float my = mbStripeMix(q.y, $parameters.x, $parameters.y);\n" +
                "    return mix($c0, $c1, mx + my - 2.0 * mx * my);\n" +
                "}\n"
        )
    }
}

internal class CubeTextureSource(
    val dimension: Int,
    private val halfFloat: Boolean,
    private val fill: () -> Buffer?,
) {
    private var textureId = 0
    private var failed = false

    fun texture(): Int {
        CubeTextures.poll()
        if (textureId != 0 || failed) {
            return textureId
        }
        if (!Gl.es3) {
            failed = true
            EffectsLog.once("cube:es2", "Color cubes need OpenGL ES 3; drawn without the lookup")
            return 0
        }
        val data = try {
            fill()
        } catch (error: Throwable) {
            EffectsLog.once("cube:fill:${error.message}", "Color cube data failed: $error", error)
            null
        }
        if (data == null) {
            failed = true
            return 0
        }
        textureId = CubeTextures.upload(this, dimension, halfFloat, data)
        if (textureId == 0) {
            failed = true
        }
        return textureId
    }
}

internal object CubeTextures {
    private class CubeReference(
        owner: CubeTextureSource,
        queue: ReferenceQueue<CubeTextureSource>,
        val id: Int,
    ) : PhantomReference<CubeTextureSource>(owner, queue)

    private val queue = ReferenceQueue<CubeTextureSource>()
    private val references = HashSet<CubeReference>()
    private var tickRegistered = false

    fun upload(owner: CubeTextureSource, dimension: Int, halfFloat: Boolean, data: Buffer): Int {
        ensureTick()
        var pending = 0
        while (pending < 16) {
            val earlier = GLES20.glGetError()
            if (earlier == GLES20.GL_NO_ERROR) {
                break
            }
            EffectsLog.once(
                "glError:$earlier",
                "GL error 0x${Integer.toHexString(earlier)} during effects rendering"
            )
            pending += 1
        }
        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        if (ids[0] == 0) {
            return 0
        }
        GLES20.glBindTexture(GLES30.GL_TEXTURE_3D, ids[0])
        GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1)
        data.position(0)
        if (halfFloat) {
            GLES30.glTexImage3D(
                GLES30.GL_TEXTURE_3D,
                0,
                GLES30.GL_RGBA16F,
                dimension,
                dimension,
                dimension,
                0,
                GLES20.GL_RGBA,
                GLES20.GL_FLOAT,
                data
            )
        } else {
            GLES30.glTexImage3D(
                GLES30.GL_TEXTURE_3D,
                0,
                GLES30.GL_RGBA8,
                dimension,
                dimension,
                dimension,
                0,
                GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE,
                data
            )
        }
        GLES20.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES30.GL_TEXTURE_3D, GLES30.GL_TEXTURE_WRAP_R, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glBindTexture(GLES30.GL_TEXTURE_3D, 0)
        val error = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            EffectsLog.once(
                "cube:upload:$error",
                "Color cube upload of ${dimension}x${dimension}x$dimension failed with GL error 0x" +
                    Integer.toHexString(error)
            )
            GLES20.glDeleteTextures(1, ids, 0)
            return 0
        }
        PipelineStats.increment("fxUploads")
        EffectsLog.info(
            "Color cube ${dimension}x${dimension}x$dimension ${if (halfFloat) "RGBA16F" else "RGBA8"} uploaded"
        )
        synchronized(references) {
            references.add(CubeReference(owner, queue, ids[0]))
        }
        return ids[0]
    }

    fun poll() {
        if (!PipelineThread.isCurrent()) {
            return
        }
        while (true) {
            val reference = queue.poll() as? CubeReference ?: break
            synchronized(references) {
                references.remove(reference)
            }
            GLES20.glDeleteTextures(1, intArrayOf(reference.id), 0)
        }
    }

    private fun ensureTick() {
        if (tickRegistered) {
            return
        }
        tickRegistered = true
        PipelineStats.addPipelineTick { poll() }
    }
}

private class ColorCubeOp(
    private val source: CubeTextureSource,
    private val encodeForLookup: Boolean,
) : ColorOp() {
    override fun emit(builder: ShaderBuilder, name: String) {
        val texture = source.texture()
        if (texture == 0) {
            builder.function("vec4 $name(vec4 c, vec2 p) {\n    return c;\n}\n")
            return
        }
        val convert = encodeForLookup && builder.mode == RenderMode.linear
        val sampler = builder.sampler3D(texture)
        val dimension = source.dimension.toFloat()
        val size = builder.uniform2f(dimension - 1f, dimension)
        val body = StringBuilder()
        body.append("vec4 $name(vec4 c, vec2 p) {\n")
        body.append("    vec4 s = mbUnpremultiply(c);\n")
        body.append("    vec3 rgb = s.rgb;\n")
        if (convert) {
            body.append("    rgb = mbLinearToSrgb(rgb);\n")
        }
        body.append("    rgb = clamp(rgb, 0.0, 1.0);\n")
        body.append("    vec4 q = texture($sampler, (rgb * $size.x + 0.5) / $size.y);\n")
        body.append("    vec3 result = q.a > 0.0 ? q.rgb / q.a : vec3(0.0);\n")
        if (convert) {
            body.append("    result = mbSrgbToLinear(result);\n")
        }
        body.append("    float a = clamp(q.a, 0.0, 1.0) * s.a;\n")
        body.append("    return vec4(result * a, a);\n")
        body.append("}\n")
        builder.function(body.toString())
    }
}

private fun isLinearColorSpace(colorSpace: CGColorSpace): Boolean {
    return colorSpace.name == CGColorSpace.linearSRGB || colorSpace.name == CGColorSpace.extendedLinearSRGB
}

private fun cubeFloatBuffer(data: ByteArray, dimension: Int): Buffer {
    val count = dimension * dimension * dimension
    val input = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
    val output = ByteBuffer.allocateDirect(count * 16).order(ByteOrder.nativeOrder()).asFloatBuffer()
    for (index in 0 until count) {
        val alpha = input.get(4 * index + 3)
        var red = input.get(4 * index)
        var green = input.get(4 * index + 1)
        var blue = input.get(4 * index + 2)
        if (alpha < 1f) {
            val limit = maxOf(alpha, 0f)
            red = min(red, limit)
            green = min(green, limit)
            blue = min(blue, limit)
        }
        output.put(red)
        output.put(green)
        output.put(blue)
        output.put(alpha)
    }
    output.position(0)
    return output
}

class CIColorMonochrome : CIFilter() {
    var inputImage: CIImage? = null
    var color: CIColor = CIColor(0.6, 0.45, 0.3, 1.0)
    var intensity: Float = 1f

    override val name: String
        get() = "CIColorMonochrome"

    override val outputImage: CIImage?
        get() {
            val image = inputImage ?: return null
            val op = MonochromeOp(color.red, color.green, color.blue, intensity.toDouble())
            return CIImage(ColorOpNode(image.node, op))
        }

    override fun setValue(value: Any?, forKey: String) {
        when (forKey) {
            kCIInputImageKey -> inputImage = value as? CIImage
            kCIInputColorKey -> color = value as? CIColor ?: color
            kCIInputIntensityKey -> intensity = anyToFloat(value, intensity)
            else -> super.setValue(value, forKey)
        }
    }

    override fun value(forKey: String): Any? {
        return when (forKey) {
            kCIInputImageKey -> inputImage
            kCIInputColorKey -> color
            kCIInputIntensityKey -> intensity
            else -> super.value(forKey)
        }
    }

    override fun setDefaults() {
        color = CIColor(0.6, 0.45, 0.3, 1.0)
        intensity = 1f
    }
}

class CIColorMatrix : CIFilter() {
    var inputImage: CIImage? = null
    var rVector: CIVector = CIVector(1.0, 0.0, 0.0, 0.0)
    var gVector: CIVector = CIVector(0.0, 1.0, 0.0, 0.0)
    var bVector: CIVector = CIVector(0.0, 0.0, 1.0, 0.0)
    var aVector: CIVector = CIVector(0.0, 0.0, 0.0, 1.0)
    var biasVector: CIVector = CIVector(0.0, 0.0, 0.0, 0.0)

    override val name: String
        get() = "CIColorMatrix"

    override val outputImage: CIImage?
        get() {
            val image = inputImage ?: return null
            val op = ColorMatrixOp(
                vectorFloats(rVector),
                vectorFloats(gVector),
                vectorFloats(bVector),
                vectorFloats(aVector),
                vectorFloats(biasVector)
            )
            val extent = if (biasVector.value(at = 3) != 0.0) CGRect.infinite else null
            return CIImage(ColorOpNode(image.node, op, extent))
        }

    override fun setValue(value: Any?, forKey: String) {
        when (forKey) {
            kCIInputImageKey -> inputImage = value as? CIImage
            "inputRVector" -> rVector = anyToCIVector(value, rVector)
            "inputGVector" -> gVector = anyToCIVector(value, gVector)
            "inputBVector" -> bVector = anyToCIVector(value, bVector)
            "inputAVector" -> aVector = anyToCIVector(value, aVector)
            "inputBiasVector" -> biasVector = anyToCIVector(value, biasVector)
            else -> super.setValue(value, forKey)
        }
    }

    override fun value(forKey: String): Any? {
        return when (forKey) {
            kCIInputImageKey -> inputImage
            "inputRVector" -> rVector
            "inputGVector" -> gVector
            "inputBVector" -> bVector
            "inputAVector" -> aVector
            "inputBiasVector" -> biasVector
            else -> super.value(forKey)
        }
    }

    override fun setDefaults() {
        rVector = CIVector(1.0, 0.0, 0.0, 0.0)
        gVector = CIVector(0.0, 1.0, 0.0, 0.0)
        bVector = CIVector(0.0, 0.0, 1.0, 0.0)
        aVector = CIVector(0.0, 0.0, 0.0, 1.0)
        biasVector = CIVector(0.0, 0.0, 0.0, 0.0)
    }
}

class CISepiaTone : CIFilter() {
    var inputImage: CIImage? = null
    var intensity: Float = 1f

    override val name: String
        get() = "CISepiaTone"

    override val outputImage: CIImage?
        get() {
            val image = inputImage ?: return null
            return CIImage(ColorOpNode(image.node, SepiaOp(intensity.toDouble())))
        }

    override fun setValue(value: Any?, forKey: String) {
        when (forKey) {
            kCIInputImageKey -> inputImage = value as? CIImage
            kCIInputIntensityKey -> intensity = anyToFloat(value, intensity)
            else -> super.setValue(value, forKey)
        }
    }

    override fun value(forKey: String): Any? {
        return when (forKey) {
            kCIInputImageKey -> inputImage
            kCIInputIntensityKey -> intensity
            else -> super.value(forKey)
        }
    }

    override fun setDefaults() {
        intensity = 1f
    }
}

class CIColorControls : CIFilter() {
    var inputImage: CIImage? = null
    var saturation: Float = 1f
    var brightness: Float = 0f
    var contrast: Float = 1f

    override val name: String
        get() = "CIColorControls"

    override val outputImage: CIImage?
        get() {
            val image = inputImage ?: return null
            val op = ColorControlsOp(saturation.toDouble(), brightness.toDouble(), contrast.toDouble())
            return CIImage(ColorOpNode(image.node, op))
        }

    override fun setValue(value: Any?, forKey: String) {
        when (forKey) {
            kCIInputImageKey -> inputImage = value as? CIImage
            kCIInputSaturationKey -> saturation = anyToFloat(value, saturation)
            kCIInputBrightnessKey -> brightness = anyToFloat(value, brightness)
            kCIInputContrastKey -> contrast = anyToFloat(value, contrast)
            else -> super.setValue(value, forKey)
        }
    }

    override fun value(forKey: String): Any? {
        return when (forKey) {
            kCIInputImageKey -> inputImage
            kCIInputSaturationKey -> saturation
            kCIInputBrightnessKey -> brightness
            kCIInputContrastKey -> contrast
            else -> super.value(forKey)
        }
    }

    override fun setDefaults() {
        saturation = 1f
        brightness = 0f
        contrast = 1f
    }
}

class CIVignette : CIFilter() {
    var inputImage: CIImage? = null
    var intensity: Float = 0f
    var radius: Float = 1f

    override val name: String
        get() = "CIVignette"

    override val outputImage: CIImage?
        get() {
            val image = inputImage ?: return null
            val extent = image.extent
            if (extent.isNull || extent.isInfinite || extent.isEmpty) {
                return CIImage(image.node)
            }
            val op = VignetteOp(
                extent.midX,
                extent.midY,
                hypot(extent.width, extent.height) / 2,
                intensity.toDouble(),
                radius.toDouble()
            )
            return CIImage(ColorOpNode(image.node, op))
        }

    override fun setValue(value: Any?, forKey: String) {
        when (forKey) {
            kCIInputImageKey -> inputImage = value as? CIImage
            kCIInputIntensityKey -> intensity = anyToFloat(value, intensity)
            kCIInputRadiusKey -> radius = anyToFloat(value, radius)
            else -> super.setValue(value, forKey)
        }
    }

    override fun value(forKey: String): Any? {
        return when (forKey) {
            kCIInputImageKey -> inputImage
            kCIInputIntensityKey -> intensity
            kCIInputRadiusKey -> radius
            else -> super.value(forKey)
        }
    }

    override fun setDefaults() {
        intensity = 0f
        radius = 1f
    }
}

class CIPixellate : CIFilter() {
    var inputImage: CIImage? = null
    var center: CGPoint = defaultCenter
    var scale: Float = 8f

    override val name: String
        get() = "CIPixellate"

    override val outputImage: CIImage?
        get() {
            val image = inputImage ?: return null
            if (scale <= 0f || scale.isNaN()) {
                return CIImage(image.node)
            }
            val warp = PixellateWarp(center.x, center.y, scale.toDouble())
            return CIImage(WarpNode(image.node, warp, image.extent))
        }

    override fun setValue(value: Any?, forKey: String) {
        when (forKey) {
            kCIInputImageKey -> inputImage = value as? CIImage
            kCIInputCenterKey -> center = anyToCGPoint(value, center)
            kCIInputScaleKey -> scale = anyToFloat(value, scale)
            else -> super.setValue(value, forKey)
        }
    }

    override fun value(forKey: String): Any? {
        return when (forKey) {
            kCIInputImageKey -> inputImage
            kCIInputCenterKey -> CIVector(cgPoint = center)
            kCIInputScaleKey -> scale
            else -> super.value(forKey)
        }
    }

    override fun setDefaults() {
        center = defaultCenter
        scale = 8f
    }
}

class CIPinchDistortion : CIFilter() {
    var inputImage: CIImage? = null
    var center: CGPoint = defaultCenter
    var radius: Float = 300f
    var scale: Float = 0.5f

    override val name: String
        get() = "CIPinchDistortion"

    override val outputImage: CIImage?
        get() {
            val image = inputImage ?: return null
            val warp = PinchWarp(center.x, center.y, radius.toDouble(), scale.toDouble())
            return CIImage(WarpNode(image.node, warp, image.extent))
        }

    override fun setValue(value: Any?, forKey: String) {
        when (forKey) {
            kCIInputImageKey -> inputImage = value as? CIImage
            kCIInputCenterKey -> center = anyToCGPoint(value, center)
            kCIInputRadiusKey -> radius = anyToFloat(value, radius)
            kCIInputScaleKey -> scale = anyToFloat(value, scale)
            else -> super.setValue(value, forKey)
        }
    }

    override fun value(forKey: String): Any? {
        return when (forKey) {
            kCIInputImageKey -> inputImage
            kCIInputCenterKey -> CIVector(cgPoint = center)
            kCIInputRadiusKey -> radius
            kCIInputScaleKey -> scale
            else -> super.value(forKey)
        }
    }

    override fun setDefaults() {
        center = defaultCenter
        radius = 300f
        scale = 0.5f
    }
}

class CITwirlDistortion : CIFilter() {
    var inputImage: CIImage? = null
    var center: CGPoint = defaultCenter
    var radius: Float = 300f
    var angle: Float = Math.PI.toFloat()

    override val name: String
        get() = "CITwirlDistortion"

    override val outputImage: CIImage?
        get() {
            val image = inputImage ?: return null
            val warp = TwirlWarp(center.x, center.y, radius.toDouble(), angle.toDouble())
            return CIImage(WarpNode(image.node, warp, image.extent))
        }

    override fun setValue(value: Any?, forKey: String) {
        when (forKey) {
            kCIInputImageKey -> inputImage = value as? CIImage
            kCIInputCenterKey -> center = anyToCGPoint(value, center)
            kCIInputRadiusKey -> radius = anyToFloat(value, radius)
            kCIInputAngleKey -> angle = anyToFloat(value, angle)
            else -> super.setValue(value, forKey)
        }
    }

    override fun value(forKey: String): Any? {
        return when (forKey) {
            kCIInputImageKey -> inputImage
            kCIInputCenterKey -> CIVector(cgPoint = center)
            kCIInputRadiusKey -> radius
            kCIInputAngleKey -> angle
            else -> super.value(forKey)
        }
    }

    override fun setDefaults() {
        center = defaultCenter
        radius = 300f
        angle = Math.PI.toFloat()
    }
}

class CIStripesGenerator : CIFilter() {
    var center: CGPoint = defaultCenter
    var color0: CIColor = CIColor.white
    var color1: CIColor = CIColor.black
    var width: Float = 80f
    var sharpness: Float = 1f

    override val name: String
        get() = "CIStripesGenerator"

    override val outputImage: CIImage?
        get() {
            if (width <= 0f || width.isNaN()) {
                return null
            }
            val generator = StripesGenerator(center.x, width.toDouble(), sharpness.toDouble(), color0, color1)
            return CIImage(GeneratorNode(generator, CGRect.infinite))
        }

    override fun setValue(value: Any?, forKey: String) {
        when (forKey) {
            kCIInputCenterKey -> center = anyToCGPoint(value, center)
            "inputColor0" -> color0 = value as? CIColor ?: color0
            "inputColor1" -> color1 = value as? CIColor ?: color1
            kCIInputWidthKey -> width = anyToFloat(value, width)
            kCIInputSharpnessKey -> sharpness = anyToFloat(value, sharpness)
            else -> super.setValue(value, forKey)
        }
    }

    override fun value(forKey: String): Any? {
        return when (forKey) {
            kCIInputCenterKey -> CIVector(cgPoint = center)
            "inputColor0" -> color0
            "inputColor1" -> color1
            kCIInputWidthKey -> width
            kCIInputSharpnessKey -> sharpness
            else -> super.value(forKey)
        }
    }

    override fun setDefaults() {
        center = defaultCenter
        color0 = CIColor.white
        color1 = CIColor.black
        width = 80f
        sharpness = 1f
    }
}

class CICheckerboardGenerator : CIFilter() {
    var center: CGPoint = defaultCenter
    var color0: CIColor = CIColor.white
    var color1: CIColor = CIColor.black
    var width: Float = 80f
    var sharpness: Float = 1f

    override val name: String
        get() = "CICheckerboardGenerator"

    override val outputImage: CIImage?
        get() {
            if (width <= 0f || width.isNaN()) {
                return null
            }
            val generator = CheckerboardGenerator(
                center.x,
                center.y,
                width.toDouble(),
                sharpness.toDouble(),
                color0,
                color1
            )
            return CIImage(GeneratorNode(generator, CGRect.infinite))
        }

    override fun setValue(value: Any?, forKey: String) {
        when (forKey) {
            kCIInputCenterKey -> center = anyToCGPoint(value, center)
            "inputColor0" -> color0 = value as? CIColor ?: color0
            "inputColor1" -> color1 = value as? CIColor ?: color1
            kCIInputWidthKey -> width = anyToFloat(value, width)
            kCIInputSharpnessKey -> sharpness = anyToFloat(value, sharpness)
            else -> super.setValue(value, forKey)
        }
    }

    override fun value(forKey: String): Any? {
        return when (forKey) {
            kCIInputCenterKey -> CIVector(cgPoint = center)
            "inputColor0" -> color0
            "inputColor1" -> color1
            kCIInputWidthKey -> width
            kCIInputSharpnessKey -> sharpness
            else -> super.value(forKey)
        }
    }

    override fun setDefaults() {
        center = defaultCenter
        color0 = CIColor.white
        color1 = CIColor.black
        width = 80f
        sharpness = 1f
    }
}

class CIStretchCrop : CIFilter() {
    var inputImage: CIImage? = null
    var size: CGPoint = CGPoint(1280.0, 720.0)
    var cropAmount: Float = 0.25f
    var centerStretchAmount: Float = 0.25f

    override val name: String
        get() = "CIStretchCrop"

    override val outputImage: CIImage?
        get() {
            val image = inputImage ?: return null
            val extent = image.extent
            val outputWidth = size.x
            val outputHeight = size.y
            if (extent.isNull || extent.isInfinite || extent.isEmpty || outputWidth <= 0 || outputHeight <= 0) {
                return null
            }
            if (centerStretchAmount != 1f) {
                EffectsLog.once(
                    "stretchCrop:centerStretch",
                    "CIStretchCrop: centerStretchAmount $centerStretchAmount is drawn as a uniform stretch"
                )
            }
            val ratio = (outputWidth / extent.width) / (outputHeight / extent.height)
            val amount = cropAmount.toDouble().coerceIn(0.0, 1.0)
            val cropRect = when {
                ratio > 1 -> {
                    val cropHeight = extent.height / ratio.pow(amount)
                    CGRect(extent.minX, extent.midY - cropHeight / 2, extent.width, cropHeight)
                }
                ratio < 1 -> {
                    val cropWidth = extent.width * ratio.pow(amount)
                    CGRect(extent.midX - cropWidth / 2, extent.minY, cropWidth, extent.height)
                }
                else -> extent
            }
            val scaleX = outputWidth / cropRect.width
            val scaleY = outputHeight / cropRect.height
            val transform = CGAffineTransform(
                scaleX,
                0.0,
                0.0,
                scaleY,
                -cropRect.minX * scaleX,
                -cropRect.minY * scaleY
            )
            val cropped = if (cropRect == extent) image.node else CropNode(image.node, cropRect)
            val stretched = TransformNode(ClampNode(cropped), transform, false)
            return CIImage(CropNode(stretched, CGRect(0.0, 0.0, outputWidth, outputHeight)))
        }

    override fun setValue(value: Any?, forKey: String) {
        when (forKey) {
            kCIInputImageKey -> inputImage = value as? CIImage
            "inputSize" -> size = anyToCGPoint(value, size)
            "inputCropAmount" -> cropAmount = anyToFloat(value, cropAmount)
            "inputCenterStretchAmount" -> centerStretchAmount = anyToFloat(value, centerStretchAmount)
            else -> super.setValue(value, forKey)
        }
    }

    override fun value(forKey: String): Any? {
        return when (forKey) {
            kCIInputImageKey -> inputImage
            "inputSize" -> CIVector(cgPoint = size)
            "inputCropAmount" -> cropAmount
            "inputCenterStretchAmount" -> centerStretchAmount
            else -> super.value(forKey)
        }
    }

    override fun setDefaults() {
        size = CGPoint(1280.0, 720.0)
        cropAmount = 0.25f
        centerStretchAmount = 0.25f
    }
}

class CIColorCubeWithColorSpace : CIFilter() {
    var inputImage: CIImage? = null
    var cubeDimension: Float = 2f
    var cubeData: ByteArray = identityCube2.copyOf()
    var colorSpace: CGColorSpace? = null
    var extrapolate: Boolean = false

    private var cachedSource: CubeTextureSource? = null
    private var cachedData: ByteArray? = null

    override val name: String
        get() = "CIColorCubeWithColorSpace"

    override val outputImage: CIImage?
        get() {
            val image = inputImage ?: return null
            val source = cubeSource() ?: return null
            val space = colorSpace
            val encodeForLookup = space != null && !isLinearColorSpace(space)
            return CIImage(ColorOpNode(image.node, ColorCubeOp(source, encodeForLookup)))
        }

    private fun cubeSource(): CubeTextureSource? {
        val dimension = cubeDimension.toInt()
        val data = cubeData
        if (dimension < 2 || dimension > 128 || cubeDimension != dimension.toFloat()) {
            EffectsLog.once("cube:dimension:$cubeDimension", "CIColorCubeWithColorSpace: bad cube dimension $cubeDimension")
            return null
        }
        if (data.size != dimension * dimension * dimension * 16) {
            EffectsLog.once(
                "cube:size:${data.size}:$dimension",
                "CIColorCubeWithColorSpace: cube data of ${data.size} bytes does not match dimension $dimension"
            )
            return null
        }
        val cached = cachedSource
        if (cached != null && cachedData === data && cached.dimension == dimension) {
            return cached
        }
        val source = CubeTextureSource(dimension, true) { cubeFloatBuffer(data, dimension) }
        cachedSource = source
        cachedData = data
        return source
    }

    override fun setValue(value: Any?, forKey: String) {
        when (forKey) {
            kCIInputImageKey -> inputImage = value as? CIImage
            "inputCubeDimension" -> cubeDimension = anyToFloat(value, cubeDimension)
            "inputCubeData" -> cubeData = value as? ByteArray ?: cubeData
            "inputColorSpace" -> colorSpace = value as? CGColorSpace
            "inputExtrapolate" -> extrapolate = value as? Boolean ?: extrapolate
            else -> super.setValue(value, forKey)
        }
    }

    override fun value(forKey: String): Any? {
        return when (forKey) {
            kCIInputImageKey -> inputImage
            "inputCubeDimension" -> cubeDimension
            "inputCubeData" -> cubeData
            "inputColorSpace" -> colorSpace
            "inputExtrapolate" -> extrapolate
            else -> super.value(forKey)
        }
    }

    override fun setDefaults() {
        cubeDimension = 2f
        cubeData = identityCube2.copyOf()
        colorSpace = null
        extrapolate = false
    }
}

package com.moblin.android.platform.coreimage.internal

import android.opengl.GLES20
import android.opengl.GLES30
import com.moblin.android.platform.core.PipelineStats
import com.moblin.android.platform.coregraphics.CGAffineTransform
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.video.CVPixelBuffer
import com.moblin.android.platform.video.YCbCrStorage
import java.util.IdentityHashMap
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

internal enum class RenderMode {
    linear,
    encoded,
    metalPetal,
    ;

    val workingFormat: TextureFormat
        get() = if (this == linear) TextureFormat.rgba16f else TextureFormat.rgba8
}

internal object Glsl {
    private const val LIBRARY = """
float mbSrgbToLinear1(float c) {
    float a = abs(c);
    float l = a <= 0.04045 ? a / 12.92 : pow((a + 0.055) / 1.055, 2.4);
    return c < 0.0 ? -l : l;
}
vec3 mbSrgbToLinear(vec3 c) {
    return vec3(mbSrgbToLinear1(c.r), mbSrgbToLinear1(c.g), mbSrgbToLinear1(c.b));
}
float mbLinearToSrgb1(float c) {
    float a = abs(c);
    float e = a <= 0.0031308 ? a * 12.92 : 1.055 * pow(a, 1.0 / 2.4) - 0.055;
    return c < 0.0 ? -e : e;
}
vec3 mbLinearToSrgb(vec3 c) {
    return vec3(mbLinearToSrgb1(c.r), mbLinearToSrgb1(c.g), mbLinearToSrgb1(c.b));
}
vec4 mbUnpremultiply(vec4 c) {
    return c.a > 0.0 ? vec4(c.rgb / c.a, c.a) : vec4(0.0);
}
vec4 mbPremultiply(vec4 c) {
    return vec4(c.rgb * c.a, c.a);
}
vec4 mbDecode(vec4 c) {
    if (c.a <= 0.0) {
        return vec4(0.0);
    }
    return vec4(mbSrgbToLinear(c.rgb / c.a) * c.a, c.a);
}
vec4 mbEncode(vec4 c) {
    if (c.a <= 0.0) {
        return vec4(0.0);
    }
    return vec4(mbLinearToSrgb(clamp(c.rgb / c.a, 0.0, 1.0)) * c.a, c.a);
}
bool mbInside(vec2 p, vec4 r) {
    return p.x >= r.x && p.y >= r.y && p.x < r.z && p.y < r.w;
}
vec4 mbFetchZero(sampler2D t, ivec2 i, vec4 size) {
    if (i.x < 0 || i.y < 0 || i.x >= int(size.x) || i.y >= int(size.y)) {
        return vec4(0.0);
    }
    return FETCH(t, i, size.zw);
}
vec4 mbSampleZero(sampler2D t, vec2 q, vec4 size) {
    if (q.x >= 1.0 && q.y >= 1.0 && q.x <= size.x - 1.0 && q.y <= size.y - 1.0) {
        return TEX(t, q / size.zw);
    }
    if (q.x <= -0.5 || q.y <= -0.5 || q.x >= size.x + 0.5 || q.y >= size.y + 0.5) {
        return vec4(0.0);
    }
    vec2 g = q - 0.5;
    vec2 f0 = floor(g);
    vec2 f = g - f0;
    ivec2 i = ivec2(f0);
    vec4 a = mbFetchZero(t, i, size);
    vec4 b = mbFetchZero(t, i + ivec2(1, 0), size);
    vec4 c = mbFetchZero(t, i + ivec2(0, 1), size);
    vec4 d = mbFetchZero(t, i + ivec2(1, 1), size);
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}
vec4 mbSampleNearestZero(sampler2D t, vec2 q, vec4 size) {
    return mbFetchZero(t, ivec2(floor(q)), size);
}
vec4 mbSampleClamp(sampler2D t, vec2 q, vec4 size) {
    return TEX(t, clamp(q, vec2(0.5), size.xy - vec2(0.5)) / size.zw);
}
"""

    const val YCC_LIBRARY = """
vec4 mbYcc(float y, vec2 c, mat3 m, vec3 o) {
    return vec4(clamp(m * (vec3(y, c) - o), 0.0, 1.0), 1.0);
}
vec4 mbYccFetchZero(sampler2D l, sampler2D k, ivec2 i, vec4 size, vec2 csize, mat3 m, vec3 o) {
    if (i.x < 0 || i.y < 0 || i.x >= int(size.x) || i.y >= int(size.y)) {
        return vec4(0.0);
    }
    return mbYcc(FETCH(l, i, size.zw).r, TEX(k, (vec2(i) + 0.5) / csize).rg, m, o);
}
vec4 mbYccSampleZero(sampler2D l, sampler2D k, vec2 q, vec4 size, vec2 csize, mat3 m, vec3 o) {
    if (q.x >= 1.0 && q.y >= 1.0 && q.x <= size.x - 1.0 && q.y <= size.y - 1.0) {
        return mbYcc(TEX(l, q / size.zw).r, TEX(k, q / csize).rg, m, o);
    }
    if (q.x <= -0.5 || q.y <= -0.5 || q.x >= size.x + 0.5 || q.y >= size.y + 0.5) {
        return vec4(0.0);
    }
    vec2 g = q - 0.5;
    vec2 f0 = floor(g);
    vec2 f = g - f0;
    ivec2 i = ivec2(f0);
    vec4 a = mbYccFetchZero(l, k, i, size, csize, m, o);
    vec4 b = mbYccFetchZero(l, k, i + ivec2(1, 0), size, csize, m, o);
    vec4 c = mbYccFetchZero(l, k, i + ivec2(0, 1), size, csize, m, o);
    vec4 d = mbYccFetchZero(l, k, i + ivec2(1, 1), size, csize, m, o);
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}
vec4 mbYccSampleClamp(sampler2D l, sampler2D k, vec2 q, vec4 size, vec2 csize, mat3 m, vec3 o) {
    vec2 s = clamp(q, vec2(0.5), size.xy - vec2(0.5));
    return mbYcc(TEX(l, s / size.zw).r, TEX(k, s / csize).rg, m, o);
}
"""

    fun prelude(): String {
        val header = if (Gl.es3) {
            "#version 300 es\nprecision highp float;\nprecision highp int;\nprecision highp sampler2D;\n" +
                "#define TEX(t, uv) texture(t, uv)\n" +
                "#define FETCH(t, i, size) texelFetch(t, i, 0)\n" +
                "out vec4 fragColor;\n"
        } else {
            "#ifdef GL_FRAGMENT_PRECISION_HIGH\nprecision highp float;\n#else\nprecision mediump float;\n#endif\n" +
                "#define TEX(t, uv) texture2D(t, uv)\n" +
                "#define FETCH(t, i, size) texture2D(t, (vec2(i) + 0.5) / (size))\n" +
                "#define fragColor gl_FragColor\n"
        }
        return header + LIBRARY
    }

    fun yccProbeSource(): String {
        return prelude() +
            "uniform sampler2D sLuma;\nuniform sampler2D sChroma;\nuniform vec4 uSize;\nuniform vec2 uChromaSize;\n" +
            "uniform mat3 uMatrix;\nuniform vec3 uOffset;\n" + YCC_LIBRARY +
            "void main() {\n" +
            "    fragColor = mbYccSampleZero(sLuma, sChroma, gl_FragCoord.xy, uSize, uChromaSize, uMatrix, uOffset);\n" +
            "}\n"
    }
}

internal class TextureBinding(val target: Int, val texture: Int)

internal fun workingColor(
    mode: RenderMode,
    red: Double,
    green: Double,
    blue: Double,
    alpha: Double,
    encoding: SourceEncoding,
): FloatArray {
    val decode = when (encoding) {
        SourceEncoding.srgb -> mode == RenderMode.linear
        SourceEncoding.srgbAlways -> true
        else -> false
    }
    val a = min(max(alpha, 0.0), 1.0)
    val r = if (decode) ShaderBuilder.srgbToLinear(red) else red
    val g = if (decode) ShaderBuilder.srgbToLinear(green) else green
    val b = if (decode) ShaderBuilder.srgbToLinear(blue) else blue
    return floatArrayOf((r * a).toFloat(), (g * a).toFloat(), (b * a).toFloat(), a.toFloat())
}

internal class ShaderBuilder(val context: RenderContext) {
    val mode: RenderMode
        get() = context.mode
    private val declarations = StringBuilder()
    private val functions = StringBuilder()
    private val included = HashSet<String>()
    private var nextId = 0
    val setters = ArrayList<(GlProgram) -> Unit>()
    val textures = ArrayList<TextureBinding>()
    private val memo = IdentityHashMap<ImageNode, String>()

    fun newName(prefix: String): String {
        return "$prefix${nextId++}"
    }

    fun function(code: String) {
        functions.append(code)
    }

    fun includeOnce(key: String, code: String) {
        if (included.add(key)) {
            functions.append(code)
        }
    }

    fun source(main: String): String {
        return Glsl.prelude() + declarations.toString() + functions.toString() + main
    }

    fun uniform1f(value: Float): String {
        val name = newName("u")
        declarations.append("uniform float $name;\n")
        setters.add { GLES20.glUniform1f(it.location(name), value) }
        return name
    }

    fun uniform2f(x: Float, y: Float): String {
        val name = newName("u")
        declarations.append("uniform vec2 $name;\n")
        setters.add { GLES20.glUniform2f(it.location(name), x, y) }
        return name
    }

    fun uniform3f(x: Float, y: Float, z: Float): String {
        val name = newName("u")
        declarations.append("uniform vec3 $name;\n")
        setters.add { GLES20.glUniform3f(it.location(name), x, y, z) }
        return name
    }

    fun uniform4f(x: Float, y: Float, z: Float, w: Float): String {
        val name = newName("u")
        declarations.append("uniform vec4 $name;\n")
        setters.add { GLES20.glUniform4f(it.location(name), x, y, z, w) }
        return name
    }

    fun uniformFloatArray(values: FloatArray): String {
        val name = newName("u")
        declarations.append("uniform float $name[${max(values.size, 1)}];\n")
        val copy = if (values.isEmpty()) floatArrayOf(0f) else values.copyOf()
        setters.add { GLES20.glUniform1fv(it.location(name), copy.size, copy, 0) }
        return name
    }

    fun uniformMat3(columnMajor: FloatArray): String {
        val name = newName("u")
        declarations.append("uniform mat3 $name;\n")
        setters.add { GLES20.glUniformMatrix3fv(it.location(name), 1, false, columnMajor, 0) }
        return name
    }

    fun uniformMat4(columnMajor: FloatArray): String {
        val name = newName("u")
        declarations.append("uniform mat4 $name;\n")
        setters.add { GLES20.glUniformMatrix4fv(it.location(name), 1, false, columnMajor, 0) }
        return name
    }

    fun uniformRect(rect: CGRect): String {
        return uniform4f(
            clampCoordinate(rect.minX),
            clampCoordinate(rect.minY),
            clampCoordinate(rect.maxX),
            clampCoordinate(rect.maxY)
        )
    }

    fun uniformAffine(transform: CGAffineTransform): String {
        return uniformMat3(
            floatArrayOf(
                transform.a.toFloat(),
                transform.b.toFloat(),
                0f,
                transform.c.toFloat(),
                transform.d.toFloat(),
                0f,
                transform.tx.toFloat(),
                transform.ty.toFloat(),
                1f
            )
        )
    }

    fun sampler2D(texture: Int): String {
        val name = newName("s")
        val unit = textures.size
        declarations.append("uniform sampler2D $name;\n")
        textures.add(TextureBinding(GLES20.GL_TEXTURE_2D, texture))
        setters.add { GLES20.glUniform1i(it.location(name), unit) }
        return name
    }

    fun sampler3D(texture: Int): String {
        val name = newName("s")
        val unit = textures.size
        declarations.append("uniform highp sampler3D $name;\n")
        textures.add(TextureBinding(GLES30.GL_TEXTURE_3D, texture))
        setters.add { GLES20.glUniform1i(it.location(name), unit) }
        return name
    }

    fun workingColor(red: Double, green: Double, blue: Double, alpha: Double, encoding: SourceEncoding): String {
        val color = workingColorValues(red, green, blue, alpha, encoding)
        return uniform4f(color[0], color[1], color[2], color[3])
    }

    fun workingColorValues(
        red: Double,
        green: Double,
        blue: Double,
        alpha: Double,
        encoding: SourceEncoding,
    ): FloatArray {
        return workingColor(mode, red, green, blue, alpha, encoding)
    }

    fun textureLeaf(
        texture: Int,
        mapX: Double,
        mapY: Double,
        offsetX: Double,
        offsetY: Double,
        validWidth: Int,
        validHeight: Int,
        allocWidth: Int,
        allocHeight: Int,
        alpha: SourceAlpha,
        encoding: SourceEncoding,
        clampEdges: Boolean = false,
    ): String {
        val sampler = sampler2D(texture)
        val map = uniform4f(mapX.toFloat(), mapY.toFloat(), offsetX.toFloat(), offsetY.toFloat())
        val size = uniform4f(
            validWidth.toFloat(),
            validHeight.toFloat(),
            allocWidth.toFloat(),
            allocHeight.toFloat()
        )
        val name = newName("f")
        val sample = if (clampEdges) "mbSampleClamp" else "mbSampleZero"
        val body = StringBuilder()
        body.append("vec4 $name(vec2 p) {\n")
        body.append("    vec4 c = $sample($sampler, p * $map.xy + $map.zw, $size);\n")
        appendSourceTail(body, alpha, encoding)
        functions.append(body)
        return name
    }

    fun yccLeaf(buffer: CVPixelBuffer, alpha: SourceAlpha, encoding: SourceEncoding): String {
        includeOnce("ycc", Glsl.YCC_LIBRARY)
        val layout = buffer.layout
        val width = buffer.width
        val height = buffer.height
        val luma = sampler2D(buffer.lumaTexture)
        val chroma = sampler2D(buffer.chromaTexture)
        val size = uniform4f(width.toFloat(), height.toFloat(), width.toFloat(), height.toFloat())
        val chromaSize = uniform2f(2f * layout.chromaWidth(width), 2f * layout.chromaHeight(height))
        val descriptor = YCbCrStorage.descriptorFor(buffer)
        val matrix = uniformMat3(descriptor.inverseMat3)
        val offset = descriptor.offsetVector
        val offsetName = uniform3f(offset[0], offset[1], offset[2])
        val name = newName("f")
        val body = StringBuilder()
        body.append("vec4 $name(vec2 p) {\n")
        body.append("    vec4 c = mbYccSampleZero($luma, $chroma, p, $size, $chromaSize, $matrix, $offsetName);\n")
        appendSourceTail(body, alpha, encoding)
        functions.append(body)
        return name
    }

    private fun appendSourceTail(body: StringBuilder, alpha: SourceAlpha, encoding: SourceEncoding) {
        when (alpha) {
            SourceAlpha.premultiply -> body.append("    c = vec4(c.rgb * c.a, c.a);\n")
            SourceAlpha.opaque -> body.append("    c.a = 1.0;\n")
            SourceAlpha.asIs -> {}
        }
        val decode = when (encoding) {
            SourceEncoding.srgb -> mode == RenderMode.linear
            SourceEncoding.srgbAlways -> true
            else -> false
        }
        if (decode) {
            body.append("    c = mbDecode(c);\n")
        }
        body.append("    return c;\n}\n")
    }

    fun intermediateLeaf(intermediate: Intermediate, clampEdges: Boolean = false): String {
        return textureLeaf(
            texture = intermediate.texture.id,
            mapX = intermediate.scaleX,
            mapY = intermediate.scaleY,
            offsetX = -intermediate.originX * intermediate.scaleX,
            offsetY = -intermediate.originY * intermediate.scaleY,
            validWidth = intermediate.validWidth,
            validHeight = intermediate.validHeight,
            allocWidth = intermediate.texture.width,
            allocHeight = intermediate.texture.height,
            alpha = SourceAlpha.asIs,
            encoding = SourceEncoding.working,
            clampEdges = clampEdges
        )
    }

    fun emit(node: ImageNode): String {
        memo[node]?.let {
            return it
        }
        val name = emitNode(node)
        memo[node] = name
        return name
    }

    private fun transparent(): String {
        val name = newName("f")
        functions.append("vec4 $name(vec2 p) {\n    return vec4(0.0);\n}\n")
        return name
    }

    private fun emitNode(node: ImageNode): String {
        if (!node.isFusable || context.isForcedMaterialization(node)) {
            val intermediate = context.materialized(node) ?: return transparent()
            val clamp = node is BlurNode && node.clampEdges
            return intermediateLeaf(intermediate, clamp)
        }
        return when (node) {
            is PixelBufferNode -> emitPixelBuffer(node)
            is BitmapNode -> emitBitmap(node)
            is ConstantNode -> {
                val color = workingColor(node.red, node.green, node.blue, node.alpha, node.encoding)
                val name = newName("f")
                functions.append("vec4 $name(vec2 p) {\n    return $color;\n}\n")
                name
            }
            is EmptyNode -> transparent()
            is TransformNode -> emitTransform(node)
            is CropNode -> {
                if (node.extent.isNull) {
                    return transparent()
                }
                val input = emit(node.input)
                val name = newName("f")
                if (node.rect.isInfinite) {
                    functions.append("vec4 $name(vec2 p) {\n    return $input(p);\n}\n")
                } else {
                    val rect = uniformRect(node.rect)
                    functions.append(
                        "vec4 $name(vec2 p) {\n" +
                            "    if (!mbInside(p, $rect)) {\n        return vec4(0.0);\n    }\n" +
                            "    return $input(p);\n}\n"
                    )
                }
                name
            }
            is ClampNode -> {
                val extent = node.input.extent
                if (extent.isNull) {
                    return transparent()
                }
                val input = emit(node.input)
                val name = newName("f")
                if (extent.isInfinite) {
                    functions.append("vec4 $name(vec2 p) {\n    return $input(p);\n}\n")
                } else {
                    val low = uniform2f((extent.minX + 0.5).toFloat(), (extent.minY + 0.5).toFloat())
                    val high = uniform2f(
                        max(extent.minX + 0.5, extent.maxX - 0.5).toFloat(),
                        max(extent.minY + 0.5, extent.maxY - 0.5).toFloat()
                    )
                    functions.append("vec4 $name(vec2 p) {\n    return $input(clamp(p, $low, $high));\n}\n")
                }
                name
            }
            is SamplingNode -> {
                val input = emit(node.input)
                val name = newName("f")
                if (node.nearest) {
                    functions.append("vec4 $name(vec2 p) {\n    return $input(floor(p) + 0.5);\n}\n")
                } else {
                    functions.append("vec4 $name(vec2 p) {\n    return $input(p);\n}\n")
                }
                name
            }
            is ColorOpNode -> {
                if (node.extent.isNull) {
                    return transparent()
                }
                val input = emit(node.input)
                val op = newName("op")
                node.op.emit(this, op)
                val name = newName("f")
                functions.append("vec4 $name(vec2 p) {\n")
                if (!node.extent.isInfinite) {
                    val rect = uniformRect(node.extent)
                    functions.append("    if (!mbInside(p, $rect)) {\n        return vec4(0.0);\n    }\n")
                }
                functions.append("    return $op($input(p), p);\n}\n")
                name
            }
            is WarpNode -> {
                if (node.extent.isNull) {
                    return transparent()
                }
                val input = emit(node.input)
                val warp = newName("w")
                node.warp.emit(this, warp)
                val name = newName("f")
                if (node.extent.isInfinite) {
                    functions.append("vec4 $name(vec2 p) {\n    return $input($warp(p));\n}\n")
                } else {
                    val rect = uniformRect(node.extent)
                    functions.append(
                        "vec4 $name(vec2 p) {\n" +
                            "    if (!mbInside(p, $rect)) {\n        return vec4(0.0);\n    }\n" +
                            "    return $input($warp(p));\n}\n"
                    )
                }
                name
            }
            is GeneratorNode -> {
                if (node.extent.isNull) {
                    return transparent()
                }
                val generator = newName("g")
                node.generator.emit(this, generator)
                val name = newName("f")
                if (node.extent.isInfinite) {
                    functions.append("vec4 $name(vec2 p) {\n    return $generator(p);\n}\n")
                } else {
                    val rect = uniformRect(node.extent)
                    functions.append(
                        "vec4 $name(vec2 p) {\n" +
                            "    if (!mbInside(p, $rect)) {\n        return vec4(0.0);\n    }\n" +
                            "    return $generator(p);\n}\n"
                    )
                }
                name
            }
            is CombineNode -> {
                if (node.extent.isNull) {
                    return transparent()
                }
                val inputNames = node.inputs.map { emit(it) }
                val combined = newName("c")
                node.combiner.emit(this, combined, inputNames)
                if (node.extent.isInfinite) {
                    return combined
                }
                val rect = uniformRect(node.extent)
                val name = newName("f")
                functions.append(
                    "vec4 $name(vec2 p) {\n" +
                        "    if (!mbInside(p, $rect)) {\n        return vec4(0.0);\n    }\n" +
                        "    return $combined(p);\n}\n"
                )
                name
            }
            is CompositeNode -> {
                val foreground = emit(node.foreground)
                val background = emit(node.background)
                val name = newName("f")
                functions.append(
                    "vec4 $name(vec2 p) {\n" +
                        "    vec4 a = $foreground(p);\n" +
                        "    vec4 b = $background(p);\n" +
                        "    return a + b * (1.0 - a.a);\n}\n"
                )
                name
            }
            else -> {
                EffectsLog.once("emit:${node.javaClass.name}", "Cannot draw ${node.javaClass.simpleName}")
                transparent()
            }
        }
    }

    private fun emitPixelBuffer(node: PixelBufferNode): String {
        val buffer = node.buffer
        if (!buffer.checkReadable("CIImage source")) {
            PipelineStats.increment("fxStale")
            return transparent()
        }
        if (buffer.layout.isPlanar) {
            return yccLeaf(buffer, node.alpha, node.encoding)
        }
        return textureLeaf(
            texture = buffer.texture,
            mapX = 1.0,
            mapY = 1.0,
            offsetX = 0.0,
            offsetY = 0.0,
            validWidth = buffer.width,
            validHeight = buffer.height,
            allocWidth = buffer.width,
            allocHeight = buffer.height,
            alpha = node.alpha,
            encoding = node.encoding
        )
    }

    private fun emitBitmap(node: BitmapNode): String {
        val entry = BitmapTextures.texture(node.bitmap) ?: return transparent()
        val width = node.extent.width
        val height = node.extent.height
        val textureWidth = entry.texture.width.toDouble()
        val textureHeight = entry.texture.height.toDouble()
        return textureLeaf(
            texture = entry.texture.id,
            mapX = textureWidth / width,
            mapY = -textureHeight / height,
            offsetX = 0.0,
            offsetY = textureHeight,
            validWidth = entry.texture.width,
            validHeight = entry.texture.height,
            allocWidth = entry.texture.width,
            allocHeight = entry.texture.height,
            alpha = node.alpha,
            encoding = node.encoding
        )
    }

    private fun emitTransform(node: TransformNode): String {
        if (node.extent.isNull) {
            return transparent()
        }
        val transform = node.transform
        val determinant = transform.a * transform.d - transform.b * transform.c
        if (determinant == 0.0 || determinant.isNaN()) {
            return transparent()
        }
        val pyramid = node.pyramidInput()
        val input = if (pyramid != null) {
            val intermediate = context.materialized(pyramid) ?: return transparent()
            intermediateLeaf(intermediate)
        } else {
            emit(node.input)
        }
        if (transform.isIdentity) {
            return input
        }
        val inverse = uniformAffine(transform.inverted())
        val name = newName("f")
        functions.append("vec4 $name(vec2 p) {\n    return $input(($inverse * vec3(p, 1.0)).xy);\n}\n")
        return name
    }

    companion object {
        fun clampCoordinate(value: Double): Float {
            return when {
                value.isNaN() -> 0f
                value > 1e30 -> 1e30f
                value < -1e30 -> -1e30f
                else -> value.toFloat()
            }
        }

        fun srgbToLinear(value: Double): Double {
            val magnitude = kotlin.math.abs(value)
            val linear = if (magnitude <= 0.04045) magnitude / 12.92 else ((magnitude + 0.055) / 1.055).pow(2.4)
            return if (value < 0) -linear else linear
        }
    }
}

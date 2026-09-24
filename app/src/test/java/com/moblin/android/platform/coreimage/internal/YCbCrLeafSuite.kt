package com.moblin.android.platform.coreimage.internal

import android.opengl.GLES20
import com.moblin.android.platform.coregraphics.CGAffineTransform
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.video.CVPixelBuffer
import com.moblin.android.platform.video.PixelBufferBacking
import com.moblin.android.platform.video.PixelBufferLayout
import com.moblin.android.platform.video.PixelBufferPlanar
import com.moblin.android.platform.video.kCVPixelFormatType_32BGRA
import com.moblin.android.platform.video.kCVPixelFormatType_420YpCbCr8BiPlanarFullRange
import java.util.Collections
import java.util.IdentityHashMap
import kotlin.math.min
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

private const val PRELUDE_GOLDEN = """#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif
#define TEX(t, uv) texture2D(t, uv)
#define FETCH(t, i, size) texture2D(t, (vec2(i) + 0.5) / (size))
#define fragColor gl_FragColor

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

private const val GRAPH1_GOLDEN = """uniform sampler2D s0;
uniform vec4 u1;
uniform vec4 u2;
vec4 f3(vec2 p) {
    vec4 c = mbSampleZero(s0, p * u1.xy + u1.zw, u2);
    c = mbDecode(c);
    return c;
}
uniform vec2 uOrigin;
uniform vec2 uScale;
void main() {
    vec2 p = uOrigin + gl_FragCoord.xy / uScale;
    fragColor = f3(p);
}
"""

private const val GRAPH2_GOLDEN = """uniform sampler2D s0;
uniform vec4 u1;
uniform vec4 u2;
uniform mat3 u4;
vec4 f3(vec2 p) {
    vec4 c = mbSampleZero(s0, p * u1.xy + u1.zw, u2);
    return c;
}
vec4 f5(vec2 p) {
    return f3((u4 * vec3(p, 1.0)).xy);
}
uniform vec2 uOrigin;
uniform vec2 uScale;
void main() {
    vec2 p = uOrigin + gl_FragCoord.xy / uScale;
    fragColor = f5(p);
}
"""

private const val GRAPH3_GOLDEN = """uniform sampler2D s0;
uniform vec4 u1;
uniform vec4 u2;
uniform vec4 u5;
uniform sampler2D s6;
uniform vec4 u7;
uniform vec4 u8;
vec4 f3(vec2 p) {
    vec4 c = mbSampleZero(s0, p * u1.xy + u1.zw, u2);
    c = vec4(c.rgb * c.a, c.a);
    return c;
}
vec4 f4(vec2 p) {
    if (!mbInside(p, u5)) {
        return vec4(0.0);
    }
    return f3(p);
}
vec4 f9(vec2 p) {
    vec4 c = mbSampleZero(s6, p * u7.xy + u7.zw, u8);
    return c;
}
uniform vec2 uOrigin;
uniform vec2 uScale;
void main() {
    vec2 p = uOrigin + gl_FragCoord.xy / uScale;
    fragColor = f4(p);
}
// f9
"""

class YCbCrLeafSuite {
    private var nextName = 100

    private fun rgbaBuffer(width: Int = 1920, height: Int = 1080): CVPixelBuffer {
        return CVPixelBuffer(PixelBufferBacking(nextName++, nextName++, width, height), null, 0x23)
    }

    private fun planarBuffer(width: Int = 1920, height: Int = 1080): CVPixelBuffer {
        val backing = PixelBufferBacking(
            nextName++,
            nextName++,
            width,
            height,
            PixelBufferLayout.ycbcr420Full,
            nextName++,
            nextName++,
        )
        return CVPixelBuffer(backing, null, kCVPixelFormatType_420YpCbCr8BiPlanarFullRange)
    }

    private fun chainMain(function: String): String {
        return "uniform vec2 uOrigin;\nuniform vec2 uScale;\n" +
            "void main() {\n    vec2 p = uOrigin + gl_FragCoord.xy / uScale;\n    fragColor = $function(p);\n}\n"
    }

    private fun count(context: RenderContext, node: ImageNode): Int {
        return Renderer.countTextures(context, node, Collections.newSetFromMap(IdentityHashMap()))
    }

    private fun graphSources(camera: CVPixelBuffer, overlay: CVPixelBuffer): List<String> {
        val linear = ShaderBuilder(RenderContext(RenderMode.linear))
        val first = linear.source(chainMain(linear.emit(PixelBufferNode(camera, SourceAlpha.asIs, SourceEncoding.srgb))))
        val raw = ShaderBuilder(RenderContext(RenderMode.metalPetal))
        val node = TransformNode(
            PixelBufferNode(camera, SourceAlpha.asIs, SourceEncoding.raw),
            CGAffineTransform(0.5, 0.0, 0.0, 0.5, 10.0, 20.0),
            false,
        )
        val second = raw.source(chainMain(raw.emit(node)))
        val layer = ShaderBuilder(RenderContext(RenderMode.metalPetal))
        val content = layer.emit(
            CropNode(
                PixelBufferNode(camera, SourceAlpha.premultiply, SourceEncoding.raw),
                CGRect(0.0, 0.0, 960.0, 540.0),
            )
        )
        val mask = layer.emit(PixelBufferNode(overlay, SourceAlpha.asIs, SourceEncoding.raw))
        val third = layer.source(chainMain(content) + "// $mask\n")
        return listOf(first, second, third)
    }

    @Test
    fun cameraStyleGraphsKeepTheirShaderSourcesByteForByte() {
        val camera = CVPixelBuffer(PixelBufferBacking(3, 4, 1920, 1080), null, 0x23)
        val overlay = CVPixelBuffer(PixelBufferBacking(5, 6, 64, 32), null, kCVPixelFormatType_32BGRA)
        val sources = graphSources(camera, overlay)
        assertEquals(PRELUDE_GOLDEN + GRAPH1_GOLDEN, sources[0])
        assertEquals(PRELUDE_GOLDEN + GRAPH2_GOLDEN, sources[1])
        assertEquals(PRELUDE_GOLDEN + GRAPH3_GOLDEN, sources[2])
    }

    @Test
    fun planarSourceBindsBothPlanesAndConvertsInTheLeaf() {
        val buffer = planarBuffer()
        val builder = ShaderBuilder(RenderContext(RenderMode.linear))
        val function = builder.emit(PixelBufferNode(buffer, SourceAlpha.asIs, SourceEncoding.srgb))
        assertEquals(2, builder.textures.size)
        assertEquals(listOf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_2D), builder.textures.map { it.target })
        assertEquals(listOf(buffer.lumaTexture, buffer.chromaTexture), builder.textures.map { it.texture })
        val source = builder.source(chainMain(function))
        assertTrue(source.contains("uniform mat3 "))
        assertTrue(source.contains("    vec4 c = mbYccSampleZero(s0, s1, p, u2, u3, u4, u5);\n    c = mbDecode(c);\n"))
        assertFalse(source.contains("mbSampleZero(s0"))
        assertFalse(source.contains("#version"))
    }

    @Test
    fun planarSourcesIncludeTheConversionLibraryOnce() {
        val builder = ShaderBuilder(RenderContext(RenderMode.metalPetal))
        val first = builder.emit(PixelBufferNode(planarBuffer(), SourceAlpha.asIs, SourceEncoding.raw))
        val second = builder.emit(PixelBufferNode(planarBuffer(), SourceAlpha.premultiply, SourceEncoding.raw))
        val source = builder.source("void main() {\n    fragColor = $first(vec2(0.0)) + $second(vec2(0.0));\n}\n")
        assertEquals(4, builder.textures.size)
        assertEquals(1, Regex("vec4 mbYccSampleZero\\(").findAll(source).count())
        assertEquals(1, Regex("vec4 mbYccSampleClamp\\(").findAll(source).count())
        assertEquals(1, Regex("vec4 mbYccFetchZero\\(").findAll(source).count())
        val rgba = ShaderBuilder(RenderContext(RenderMode.metalPetal))
        val function = rgba.emit(PixelBufferNode(rgbaBuffer(), SourceAlpha.asIs, SourceEncoding.raw))
        assertFalse(rgba.source(chainMain(function)).contains("mbYcc"))
    }

    @Test
    fun planarSourcesCountTwoTexturesEach() {
        val context = RenderContext(RenderMode.metalPetal)
        val planar = PixelBufferNode(planarBuffer(), SourceAlpha.asIs, SourceEncoding.raw)
        val rgba = PixelBufferNode(rgbaBuffer(), SourceAlpha.asIs, SourceEncoding.raw)
        assertEquals(2, count(context, planar))
        assertEquals(1, count(context, rgba))
        assertEquals(3, count(context, CompositeNode(planar, rgba)))
        assertEquals(2, count(context, CompositeNode(planar, planar)))
    }

    @Test
    fun chainOfFourPlanarSourcesIsSplitToFitTheTextureUnits() {
        val context = RenderContext(RenderMode.metalPetal)
        var root: ImageNode = PixelBufferNode(planarBuffer(), SourceAlpha.asIs, SourceEncoding.raw)
        repeat(3) { index ->
            val source = TransformNode(
                PixelBufferNode(planarBuffer(), SourceAlpha.asIs, SourceEncoding.raw),
                CGAffineTransform(0.25, 0.0, 0.0, 0.25, 100.0 * index, 0.0),
                false,
            )
            root = CompositeNode(source, root)
        }
        val limit = min(Gl.maxTextureUnits, 16) - 1
        assertEquals(8, count(context, root))
        Renderer.enforceTextureBudget(context, root, limit)
        assertTrue(count(context, root) <= limit)
    }

    @Test
    fun layerWithSevenPlanarSourcesAndAMaskIsSplitBelowTheLayerBudget() {
        val context = RenderContext(RenderMode.metalPetal)
        var content: ImageNode = PixelBufferNode(planarBuffer(), SourceAlpha.asIs, SourceEncoding.raw)
        repeat(6) { index ->
            val source = TransformNode(
                PixelBufferNode(planarBuffer(), SourceAlpha.asIs, SourceEncoding.raw),
                CGAffineTransform(0.25, 0.0, 0.0, 0.25, 100.0 * index, 0.0),
                false,
            )
            content = CompositeNode(source, content)
        }
        val mask = PixelBufferNode(rgbaBuffer(64, 32), SourceAlpha.asIs, SourceEncoding.raw)
        val limit = min(Gl.maxTextureUnits, 16) - 3
        assertEquals(14, count(context, content))
        Renderer.enforceTextureBudget(context, content, limit)
        val forced = ArrayList<ImageNode>()
        val pending = arrayListOf(content)
        val seen: MutableSet<ImageNode> = Collections.newSetFromMap(IdentityHashMap())
        while (pending.isNotEmpty()) {
            val node = pending.removeAt(pending.size - 1)
            if (!seen.add(node)) {
                continue
            }
            if (context.isForcedMaterialization(node)) {
                forced.add(node)
            }
            pending.addAll(node.inputs)
        }
        assertTrue(forced.isNotEmpty())
        assertTrue(count(context, content) <= limit)
        assertTrue(count(context, content) + count(context, mask) <= min(Gl.maxTextureUnits, 16) - 2)
        for (node in forced) {
            val inner = RenderContext(RenderMode.metalPetal)
            val innerLimit = min(Gl.maxTextureUnits, 16) - 1
            Renderer.enforceTextureBudget(inner, node, innerLimit)
            assertTrue(count(inner, node) <= innerLimit)
        }
    }

    @Test
    fun probeSourceRunsTheProductionLeafLibrary() {
        val source = Glsl.yccProbeSource()
        assertTrue(source.startsWith(Glsl.prelude()))
        assertTrue(source.contains(Glsl.YCC_LIBRARY))
        assertTrue(
            source.endsWith(
                "void main() {\n" +
                    "    fragColor = mbYccSampleZero(sLuma, sChroma, gl_FragCoord.xy, uSize, uChromaSize, uMatrix, uOffset);\n" +
                    "}\n"
            )
        )
        for (name in listOf("sLuma", "sChroma", "uSize", "uChromaSize", "uMatrix", "uOffset")) {
            assertEquals(1, Regex("uniform \\w+ $name;").findAll(source).count(), name)
        }
    }

    @Test
    fun renderingIntoAPlanarBufferIsRefused() {
        val target = planarBuffer()
        val source = PixelBufferNode(rgbaBuffer(), SourceAlpha.asIs, SourceEncoding.srgb)
        val before = PixelBufferPlanar.badTargets.get()
        val misses = PixelBufferPlanar.misses.get()
        Renderer.renderCoreImageToBuffer(source, CGRect(0.0, 0.0, 1920.0, 1080.0), target)
        Renderer.renderMetalPetalToBuffer(source, 1920, 1080, target)
        assertEquals(before + 2, PixelBufferPlanar.badTargets.get())
        assertEquals(misses, PixelBufferPlanar.misses.get())
    }
}

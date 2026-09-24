package com.moblin.android.platform.video

import android.opengl.Matrix
import com.moblin.android.platform.core.PipelineThread
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val CAMERA_2D_GOLDEN = """
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif
varying vec2 vTexCoord;
uniform sampler2D sTexture;
void main() {
    gl_FragColor = texture2D(sTexture, vTexCoord);
}
"""

private const val CAMERA_OES_GOLDEN = """#extension GL_OES_EGL_image_external : require
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif
varying vec2 vTexCoord;
uniform samplerExternalOES sTexture;
void main() {
    gl_FragColor = texture2D(sTexture, vTexCoord);
}
"""

private const val CAMERA_VERTEX_GOLDEN = """
attribute vec4 aPosition;
attribute vec4 aTexCoord;
uniform mat4 uTexMatrix;
varying vec2 vTexCoord;
void main() {
    gl_Position = aPosition;
    vTexCoord = (uTexMatrix * aTexCoord).xy;
}
"""

@RunWith(RobolectricTestRunner::class)
class GlRendererYccSuite {
    private val pools = mutableListOf<CVPixelBufferPool>()
    private val identity = FloatArray(16).also { Matrix.setIdentityM(it, 0) }

    @Before
    fun setUp() {
        YCbCrStorage.override = true
    }

    @After
    fun tearDown() {
        YCbCrStorage.override = null
        PipelineThread.runSync(timeoutMs = 5000) { GlRenderer.resetProgramFailures() }
        for (pool in pools) {
            pool.invalidate()
        }
    }

    private fun <T> onPipeline(block: () -> T): T {
        val result = PipelineThread.runSync(timeoutMs = 5000, block = block)
        PipelineThread.runSync(timeoutMs = 5000) {}
        return result
    }

    private fun heldBuffer(layout: PixelBufferLayout, width: Int = 64, height: Int = 32): CVPixelBuffer {
        val pool = CVPixelBufferPool(width, height, YCbCrStorage.tagFor(layout, 0x23), 4, layout)
        pools.add(pool)
        return assertNotNull(onPipeline { retainLease(pool.createPixelBuffer()) })
    }

    @Test
    fun cameraShadersAreUnchanged() {
        assertEquals(CAMERA_2D_GOLDEN, GlRenderer.fragmentSource(GlRenderer.ProgramKind.rgba2d))
        assertEquals(CAMERA_OES_GOLDEN, GlRenderer.fragmentSource(GlRenderer.ProgramKind.oes))
        assertEquals(CAMERA_VERTEX_GOLDEN, GlRenderer.vertexSource(GlRenderer.ProgramKind.rgba2d))
        assertEquals(CAMERA_VERTEX_GOLDEN, GlRenderer.vertexSource(GlRenderer.ProgramKind.oes))
    }

    @Test
    fun planarShadersAreEssl100() {
        val kinds = listOf(
            GlRenderer.ProgramKind.oesToLuma,
            GlRenderer.ProgramKind.oesToChroma,
            GlRenderer.ProgramKind.rgbaToLuma,
            GlRenderer.ProgramKind.rgbaToChroma,
            GlRenderer.ProgramKind.yccToRgb,
        )
        for (kind in kinds) {
            assertFalse(GlRenderer.fragmentSource(kind).contains("#version"), kind.name)
            assertFalse(GlRenderer.vertexSource(kind).contains("#version"), kind.name)
            assertTrue(GlRenderer.fragmentSource(kind).contains("gl_FragColor"), kind.name)
        }
        for (kind in listOf(GlRenderer.ProgramKind.oesToLuma, GlRenderer.ProgramKind.oesToChroma)) {
            assertTrue(
                GlRenderer.fragmentSource(kind).startsWith("#extension GL_OES_EGL_image_external : require\n"),
                kind.name,
            )
            assertTrue(GlRenderer.fragmentSource(kind).contains("samplerExternalOES sTexture"), kind.name)
        }
        for (kind in listOf(GlRenderer.ProgramKind.rgbaToLuma, GlRenderer.ProgramKind.rgbaToChroma)) {
            val source = GlRenderer.fragmentSource(kind)
            assertFalse(source.contains("#extension"), kind.name)
            assertFalse(source.contains("samplerExternalOES"), kind.name)
            assertTrue(source.contains("uniform sampler2D sTexture;"), kind.name)
        }
        assertEquals(
            GlRenderer.fragmentSource(GlRenderer.ProgramKind.oesToChroma)
                .removePrefix("#extension GL_OES_EGL_image_external : require\n")
                .replace("samplerExternalOES", "sampler2D"),
            GlRenderer.fragmentSource(GlRenderer.ProgramKind.rgbaToChroma),
        )
        assertEquals(
            GlRenderer.vertexSource(GlRenderer.ProgramKind.oesToChroma),
            GlRenderer.vertexSource(GlRenderer.ProgramKind.rgbaToChroma),
        )
        val chroma = GlRenderer.fragmentSource(GlRenderer.ProgramKind.oesToChroma)
        assertTrue(chroma.contains("min(vCoord, vec2(1.0) - 0.5 / uLumaSize)"))
        assertTrue(chroma.contains("uTexMatrix * vec4(t, 0.0, 1.0)"))
        assertTrue(GlRenderer.vertexSource(GlRenderer.ProgramKind.oesToChroma).contains("vCoord = aTexCoord.xy * uExtent;"))
        val ycc = GlRenderer.fragmentSource(GlRenderer.ProgramKind.yccToRgb)
        assertFalse(ycc.contains("#extension"))
        assertTrue(ycc.contains("texture2D(sChroma, vTexCoord * uChromaScale).rg"))
        assertTrue(ycc.contains("vec4(clamp(uMatrix * (vec3(y, c) - uOffset), 0.0, 1.0), 1.0)"))
    }

    @Test
    fun failedPlanarProgramLeavesCameraProgramsUsable() {
        val usable = onPipeline {
            GlRenderer.markProgramFailed(GlRenderer.ProgramKind.yccToRgb)
            listOf(
                GlRenderer.isProgramUsable(GlRenderer.ProgramKind.rgba2d),
                GlRenderer.isProgramUsable(GlRenderer.ProgramKind.oes),
                GlRenderer.isProgramUsable(GlRenderer.ProgramKind.oesToLuma),
                GlRenderer.isProgramUsable(GlRenderer.ProgramKind.yccToRgb),
            )
        }
        assertEquals(listOf(true, true, true, false), usable)
        val failure = onPipeline { GlRenderer.planarProgramFailure() }
        assertEquals("the yccToRgb program does not compile", failure)
    }

    @Test
    fun rotatedOrMirroredDrawIntoPlanarTargetIsRefused() {
        val target = heldBuffer(PixelBufferLayout.ycbcr420Full)
        assertTrue(target.layout.isPlanar)
        val badTargets = PixelBufferPlanar.badTargets.get()
        val misses = PixelBufferPlanar.misses.get()
        onPipeline { GlRenderer.drawOes(1, identity, target, 90, false) }
        assertEquals(badTargets + 1, PixelBufferPlanar.badTargets.get())
        onPipeline { GlRenderer.drawOes(1, identity, target, 0, true) }
        assertEquals(badTargets + 2, PixelBufferPlanar.badTargets.get())
        onPipeline { GlRenderer.drawOes(1, identity, target, 360, false) }
        onPipeline { GlRenderer.drawOes(1, identity, target, 0, false) }
        assertEquals(badTargets + 2, PixelBufferPlanar.badTargets.get())
        assertEquals(misses, PixelBufferPlanar.misses.get())
        releaseLease(target)
    }

    @Test
    fun cameraDrawIntoRgbaTargetStillRotates() {
        val target = heldBuffer(PixelBufferLayout.rgba8)
        val badTargets = PixelBufferPlanar.badTargets.get()
        onPipeline { GlRenderer.drawOes(1, identity, target, 90, true) }
        assertEquals(badTargets, PixelBufferPlanar.badTargets.get())
        releaseLease(target)
    }

    @Test
    fun planarSourcesDrawWithoutSinglePlaneAccess() {
        val source = heldBuffer(PixelBufferLayout.ycbcr420Video, 1919, 1079)
        assertTrue(source.layout.isPlanar)
        val misses = PixelBufferPlanar.misses.get()
        onPipeline {
            GlRenderer.bind(null)
            GlRenderer.draw(source, 1280, 720, GlRenderer.ScalingMode.fill, 90, true)
            GlRenderer.drawBuffer(source, 640, 360, GlRenderer.ScalingMode.stretch, 0, false, true)
        }
        assertEquals(misses, PixelBufferPlanar.misses.get())
        releaseLease(source)
    }
}

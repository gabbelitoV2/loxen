package com.moblin.android.platform.video

import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class PixelBufferConsumerAuditTest {
    private val audited = setOf(
        "platform/video/GlRenderer.kt",
        "platform/video/CoreVideo.kt",
        "platform/vision/VisionEngine.kt",
        "platform/coreimage/internal/Shader.kt",
        "platform/coreimage/internal/Renderer.kt",
        "platform/live2d/Live2DRenderer.kt",
        "platform/coreimage/internal/Textures.kt",
        "platform/metalpetal/MTIFiltersE8.kt",
        "platform/coreimage/FiltersColor.kt",
    )

    private val planarProducers = setOf(
        "platform/videotoolbox/VTDecompressionSession.kt",
        "platform/avfoundation/AVAssetReader.kt",
    )

    private fun sourceRoot(): File {
        return listOf(
            File("src/main/java/com/moblin/android"),
            File("app/src/main/java/com/moblin/android"),
        ).first { it.isDirectory }
    }

    private fun filesMatching(pattern: Regex): Set<String> {
        val root = sourceRoot()
        return root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { pattern.containsMatchIn(it.readText()) }
            .map { it.relativeTo(root).invariantSeparatorsPath }
            .toSortedSet()
    }

    @Test
    fun everyTextureOrFramebufferReaderHasBeenAuditedForPlanarBuffers() {
        val found = filesMatching(Regex("""\.(texture|framebuffer)\b|readableTexture\b"""))
        assertEquals(emptySet(), found - audited, "audit these files for planar pixel buffers")
        for (path in audited) {
            assertTrue(File(sourceRoot(), path).isFile, path)
        }
    }

    @Test
    fun onlyTheDecoderAndTheReaderPickPlanarStorage() {
        assertEquals(planarProducers, filesMatching(Regex("""YCbCrStorage\.layoutFor\(""")))
    }
}

package com.moblin.android.platform.coreimage.internal

import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.video.CVPixelBuffer
import com.moblin.android.platform.video.CVPixelBufferPool
import com.moblin.android.platform.video.PixelBufferBacking
import com.moblin.android.platform.video.kCVPixelFormatType_32BGRA
import com.moblin.android.platform.video.swapPool
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Test

class EffectsTextureResidencyTest {
    @Before
    fun releaseLeftoverSlots() {
        TextureReaper.evictAll()
    }

    private fun buffer(width: Int = 64, height: Int = 32): CVPixelBuffer {
        return CVPixelBuffer(PixelBufferBacking(7, 9, width, height), null, kCVPixelFormatType_32BGRA)
    }

    private fun registeredSlot(side: Int = 8): TextureSlot {
        val slot = TextureSlot()
        slot.add(PooledTexture(0, side, side, TextureFormat.rgba8))
        TextureReaper.register(slot)
        return slot
    }

    private fun layer(content: ImageNode, mask: ImageNode?): LayerSpec {
        return LayerSpec(
            content = content,
            contentWidth = 64.0,
            contentHeight = 32.0,
            regionMinX = 0.0,
            regionMinY = 0.0,
            regionMaxX = 64.0,
            regionMaxY = 32.0,
            flipHorizontally = false,
            flipVertically = false,
            mask = mask?.let { MaskSpec(it, 64.0, 32.0, 0, false) },
            compositingMask = null,
            centerX = 32.0,
            centerY = 16.0,
            width = 64.0,
            height = 32.0,
            rotation = 0.0,
            opacity = 1.0,
            cornerRadius = floatArrayOf(0f, 0f, 0f, 0f),
            continuousCorners = false,
            tint = floatArrayOf(0f, 0f, 0f, 0f),
            blend = LayerBlend.normal,
        )
    }

    @Test
    fun slotUsedEveryFrameStaysResident() {
        var nowMs = 1_000_000L
        TextureReaper.beginRender(nowMs)
        val slot = registeredSlot()
        repeat(30) {
            nowMs += 33
            TextureReaper.beginRender(nowMs)
            TextureReaper.touch(slot)
        }
        assertFalse(slot.released)
    }

    @Test
    fun slotUnusedForThreeRendersAndHundredMillisecondsIsReleased() {
        var nowMs = 2_000_000L
        TextureReaper.beginRender(nowMs)
        val slot = registeredSlot()
        val residentBefore = TextureReaper.residentByteCount
        repeat(3) {
            nowMs += 33
            TextureReaper.beginRender(nowMs)
            assertFalse(slot.released)
        }
        nowMs += 33
        TextureReaper.beginRender(nowMs)
        assertTrue(slot.released)
        assertEquals(residentBefore - 8 * 8 * 4, TextureReaper.residentByteCount)
    }

    @Test
    fun manyRendersWithinOneFrameKeepSlot() {
        var nowMs = 3_000_000L
        TextureReaper.beginRender(nowMs)
        val slot = registeredSlot()
        repeat(10) {
            nowMs += 5
            TextureReaper.beginRender(nowMs)
        }
        assertFalse(slot.released)
        nowMs += 60
        TextureReaper.beginRender(nowMs)
        assertTrue(slot.released)
    }

    @Test
    fun idleSlotIsReleasedWithoutRenders() {
        val nowMs = 4_000_000L
        TextureReaper.beginRender(nowMs)
        val slot = registeredSlot()
        TextureReaper.evictIdle(nowMs + 1999)
        assertFalse(slot.released)
        TextureReaper.evictIdle(nowMs + 2000)
        assertTrue(slot.released)
    }

    @Test
    fun releasedSlotRunsCallbackOnce() {
        TextureReaper.beginRender(5_000_000L)
        val slot = registeredSlot()
        var calls = 0
        slot.onRelease = { calls += 1 }
        TextureReaper.evictAll()
        slot.releaseAll()
        assertTrue(slot.released)
        assertEquals(1, calls)
        assertEquals(0L, slot.bytes)
    }

    @Test
    fun freeTexturePoolIsBounded() {
        TexturePool.trimAll()
        repeat(20) {
            TexturePool.release(PooledTexture(0, 2048, 2048, TextureFormat.rgba8), nowMs = 6_000_000L + it)
        }
        assertTrue(TexturePool.freeByteCount <= 192L * 1024 * 1024)
        assertEquals(TexturePool.freeCount.toLong() * 2048 * 2048 * 4, TexturePool.freeByteCount)
        val reused = TexturePool.obtain(2048, 2048, TextureFormat.rgba8, true)
        assertEquals(2048, reused?.width)
        TexturePool.trimAll()
        assertEquals(0L, TexturePool.freeByteCount)
        assertEquals(0, TexturePool.freeCount)
    }

    @Test
    fun texturesReleasedByOneRenderAreKeptOverTheCap() {
        TexturePool.trimAll()
        repeat(20) {
            TexturePool.release(PooledTexture(0, 2048, 2048, TextureFormat.rgba8), nowMs = 7_000_000L)
        }
        assertEquals(20L * 2048 * 2048 * 4, TexturePool.freeByteCount)
        TexturePool.release(PooledTexture(0, 64, 64, TextureFormat.rgba8), nowMs = 7_000_033L)
        assertTrue(TexturePool.freeByteCount <= 192L * 1024 * 1024)
        assertEquals(64L * 64 * 4, TexturePool.freeByteCount % (2048L * 2048 * 4))
        TexturePool.trimAll()
        assertEquals(0L, TexturePool.freeByteCount)
    }

    @Test
    fun sourcesAreFoundOnceThroughEveryNodeKind() {
        val camera = buffer()
        val overlay = buffer()
        val mask = buffer()
        val cameraNode = PixelBufferNode(camera, SourceAlpha.asIs, SourceEncoding.srgb)
        val boundary = CiBoundaryNode(
            PixelBufferNode(overlay, SourceAlpha.asIs, SourceEncoding.srgb),
            CGRect(0.0, 0.0, 64.0, 32.0),
            false,
            64,
            32
        )
        val layers = LayersNode(
            TransformNode(cameraNode, com.moblin.android.platform.coregraphics.CGAffineTransform.identity, false),
            listOf(layer(boundary, PixelBufferNode(mask, SourceAlpha.asIs, SourceEncoding.raw))),
            64.0,
            32.0
        )
        val root = CompositeNode(CropNode(layers, CGRect(0.0, 0.0, 64.0, 32.0)), cameraNode)
        val sources = pixelBufferSources(root)
        assertEquals(3, sources.size)
        assertTrue(sources.any { it === camera })
        assertTrue(sources.any { it === overlay })
        assertTrue(sources.any { it === mask })
    }

    @Test
    fun staleSourceIsDrawnTransparent() {
        val stale = buffer()
        stale.backing.generation += 1
        val builder = ShaderBuilder(RenderContext(RenderMode.encoded))
        builder.emit(PixelBufferNode(stale, SourceAlpha.asIs, SourceEncoding.srgb))
        assertTrue(builder.textures.isEmpty())
        val valid = buffer()
        builder.emit(PixelBufferNode(valid, SourceAlpha.asIs, SourceEncoding.srgb))
        assertEquals(1, builder.textures.size)
        assertEquals(7, builder.textures[0].texture)
    }

    @Test
    fun staleSourceIsNotLeased() {
        val stale = buffer()
        stale.backing.generation += 1
        val leases = RenderLeases.acquire(PixelBufferNode(stale, SourceAlpha.asIs, SourceEncoding.srgb), null, "test")
        assertEquals(0, leases.count)
        leases.release()
    }

    @Test
    fun swapPoolInvalidatesOnlyTheReplacedPool() {
        val first = CVPixelBufferPool(16, 16, kCVPixelFormatType_32BGRA)
        val second = CVPixelBufferPool(16, 16, kCVPixelFormatType_32BGRA)
        assertSame(second, swapPool(first, second))
        assertTrue(first.state.released)
        assertFalse(second.state.released)
        assertSame(second, swapPool(second, second))
        assertFalse(second.state.released)
        assertNull(swapPool(second, null))
        assertTrue(second.state.released)
        assertNull(swapPool("not a pool", null))
    }
}

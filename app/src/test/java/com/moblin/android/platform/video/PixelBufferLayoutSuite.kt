package com.moblin.android.platform.video

import android.graphics.Bitmap
import com.moblin.android.media.MediaSample
import com.moblin.android.platform.core.PipelineThread
import com.moblin.android.platform.vision.visionIdentity
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PixelBufferLayoutSuite {
    private val pools = mutableListOf<CVPixelBufferPool>()

    @Before
    fun setUp() {
        YCbCrStorage.override = true
    }

    @After
    fun tearDown() {
        for (pool in pools) {
            pool.invalidate()
        }
        YCbCrStorage.override = null
    }

    private fun <T> onPipeline(block: () -> T): T {
        val result = PipelineThread.runSync(timeoutMs = 5000, block = block)
        PipelineThread.runSync(timeoutMs = 5000) {}
        return result
    }

    private fun makePool(
        layout: PixelBufferLayout,
        width: Int = 64,
        height: Int = 32,
        maximumBufferCount: Int = 4,
        name: String = "layout-test",
    ): CVPixelBufferPool {
        val pool = CVPixelBufferPool(
            width,
            height,
            YCbCrStorage.tagFor(layout, 0x23),
            maximumBufferCount,
            layout,
        )
        pool.name = name
        pools.add(pool)
        return pool
    }

    private fun heldBuffer(pool: CVPixelBufferPool): CVPixelBuffer {
        return assertNotNull(onPipeline { retainLease(pool.createPixelBuffer()) })
    }

    private fun fakePlanar(width: Int = 1920, height: Int = 1080): CVPixelBuffer {
        val backing = PixelBufferBacking(11, 12, width, height, PixelBufferLayout.ycbcr420Full, 13, 14)
        return CVPixelBuffer(backing, null, kCVPixelFormatType_420YpCbCr8BiPlanarFullRange)
    }

    @Test
    fun planarBuffersHoldAsManyBytesAsNv12() {
        assertEquals(3_110_400L, PixelBufferLayout.ycbcr420Full.bytes(1920, 1080))
        assertEquals(3_110_400L, PixelBufferLayout.ycbcr420Video.bytes(1920, 1080))
        assertEquals(3_116_403L, PixelBufferLayout.ycbcr420Full.bytes(1921, 1081))
        assertEquals(961, PixelBufferLayout.ycbcr420Full.chromaWidth(1921))
        assertEquals(541, PixelBufferLayout.ycbcr420Full.chromaHeight(1081))
        assertEquals(8_294_400L, PixelBufferLayout.rgba8.bytes(1920, 1080))
        assertEquals(1, PixelBufferLayout.rgba8.planeCount)
        assertEquals(2, PixelBufferLayout.ycbcr420Full.planeCount)
        assertEquals(2, PixelBufferLayout.ycbcr420Video.planeCount)
    }

    @Test
    fun positionalBackingsStayRgba() {
        val backing = PixelBufferBacking(7, 9, 64, 32)
        assertEquals(PixelBufferLayout.rgba8, backing.layout)
        assertEquals(0, backing.chromaTexture)
        assertEquals(0, backing.chromaFramebuffer)
        val buffer = CVPixelBuffer(backing, null, kCVPixelFormatType_32BGRA)
        assertEquals(7, buffer.texture)
        assertEquals(9, buffer.framebuffer)
        assertEquals(0, CVPixelBufferGetPlaneCount(buffer))
        assertFalse(CVPixelBufferIsPlanar(buffer))
        assertTrue(buffer.checkRenderable("test target"))
    }

    @Test
    fun planarBuffersRefuseSinglePlaneAccess() {
        val buffer = fakePlanar()
        val misses = PixelBufferPlanar.misses.get()
        assertEquals(0, buffer.texture)
        assertEquals(0, buffer.framebuffer)
        assertEquals(0, buffer.readableTexture("test"))
        assertEquals(misses + 3, PixelBufferPlanar.misses.get())
        assertEquals(11, buffer.lumaTexture)
        assertEquals(12, buffer.lumaFramebuffer)
        assertEquals(13, buffer.chromaTexture)
        assertEquals(14, buffer.chromaFramebuffer)
        assertEquals(2, CVPixelBufferGetPlaneCount(buffer))
        assertTrue(CVPixelBufferIsPlanar(buffer))
        assertTrue(buffer.toString().contains("ycc"))
        assertEquals(misses + 3, PixelBufferPlanar.misses.get())
    }

    @Test
    fun planarBuffersAreNotRenderTargets() {
        val buffer = fakePlanar()
        val badTargets = PixelBufferPlanar.badTargets.get()
        assertFalse(buffer.checkRenderable("test target"))
        assertEquals(badTargets + 1, PixelBufferPlanar.badTargets.get())
    }

    private fun lifecycle(layout: PixelBufferLayout): List<Any> {
        val pool = makePool(layout)
        val observations = mutableListOf<Any>()
        val buffer = heldBuffer(pool)
        observations.add(buffer.layout == layout)
        observations.add(buffer.leaseCount.get())
        observations.add(buffer.isValid)
        val generation = buffer.backing.generation
        retainLease(buffer)
        observations.add(buffer.leaseCount.get())
        releaseLease(buffer)
        observations.add(buffer.leaseCount.get())
        observations.add(buffer.isValid)
        releaseLease(buffer)
        observations.add(buffer.isValid)
        observations.add(buffer.backing.generation - generation)
        observations.add(onPipeline { synchronized(PixelBufferReaper) { pool.state.leased } })
        observations.add(onPipeline { synchronized(PixelBufferReaper) { pool.state.free.size } })
        observations.add(onPipeline { synchronized(PixelBufferReaper) { pool.state.allocated } })
        val second = assertNotNull(onPipeline { pool.createPixelBuffer() })
        observations.add(second.isValid)
        observations.add(second.backing === buffer.backing)
        observations.add(buffer.checkReadable("stale handle"))
        return observations
    }

    @Test
    fun planarLeasesBehaveExactlyLikeRgbaLeases() {
        val rgba = lifecycle(PixelBufferLayout.rgba8)
        val planar = lifecycle(PixelBufferLayout.ycbcr420Full)
        assertEquals(listOf<Any>(true, 1, true, 2, 1, true, false, 1, 0, 1, 1, false, true, false), rgba)
        assertEquals(rgba, planar)
    }

    @Test
    fun reportShowsTheLayoutAndMegabytesPerPool() {
        val pool = makePool(PixelBufferLayout.ycbcr420Full, 1920, 1080, 256, "ycc-report")
        val held = (0 until 89).map { heldBuffer(pool) }
        val rgba = makePool(PixelBufferLayout.rgba8, 1920, 1080, 16, "rgba-report")
        val rgbaHeld = heldBuffer(rgba)
        val report = assertNotNull(onPipeline { PixelBufferReaper.report() })
        assertTrue(report.contains("ycc-report 1920x1080 ycc 89/89/256 277MB"), report)
        assertTrue(report.contains("rgba-report 1920x1080 rgba 1/1/16 8MB"), report)
        for (buffer in held) {
            releaseLease(buffer)
        }
        releaseLease(rgbaHeld)
    }

    @Test
    fun transferFromAPlanarSourceDrawsInsteadOfBlitting() {
        val source = heldBuffer(makePool(PixelBufferLayout.ycbcr420Full))
        val target = heldBuffer(makePool(PixelBufferLayout.rgba8))
        assertTrue(source.layout.isPlanar)
        val misses = PixelBufferPlanar.misses.get()
        val badTargets = PixelBufferPlanar.badTargets.get()
        VTPixelTransferSessionTransferImage(source, target)
        assertEquals(misses, PixelBufferPlanar.misses.get())
        assertEquals(badTargets, PixelBufferPlanar.badTargets.get())
        releaseLease(source)
        releaseLease(target)
    }

    @Test
    fun transferIntoAPlanarTargetIsRefused() {
        val source = heldBuffer(makePool(PixelBufferLayout.rgba8))
        val target = heldBuffer(makePool(PixelBufferLayout.ycbcr420Full))
        val misses = PixelBufferPlanar.misses.get()
        val badTargets = PixelBufferPlanar.badTargets.get()
        VTPixelTransferSessionTransferImage(source, target)
        assertEquals(badTargets + 1, PixelBufferPlanar.badTargets.get())
        assertEquals(misses, PixelBufferPlanar.misses.get())
        releaseLease(source)
        releaseLease(target)
    }

    @Test
    fun transferBetweenHandlesOfOneBackingIsSkipped() {
        val source = heldBuffer(makePool(PixelBufferLayout.ycbcr420Full))
        val alias = CVPixelBuffer(source.backing, source.poolState, source.pixelFormatType)
        val badTargets = PixelBufferPlanar.badTargets.get()
        VTPixelTransferSessionTransferImage(source, alias)
        assertEquals(badTargets, PixelBufferPlanar.badTargets.get())
        releaseLease(source)
    }

    @Test
    fun readbackUploadAndClearHandlePlanarBuffers() {
        val buffer = heldBuffer(makePool(PixelBufferLayout.ycbcr420Full))
        val misses = PixelBufferPlanar.misses.get()
        val badTargets = PixelBufferPlanar.badTargets.get()
        val bitmap = assertNotNull(buffer.toBitmap())
        assertEquals(64, bitmap.width)
        assertEquals(32, bitmap.height)
        onPipeline {
            PixelBufferGl.clear(buffer.backing, 0f, 0f, 0f, 1f)
            GlRenderer.draw(buffer, 128, 72, GlRenderer.ScalingMode.fit)
        }
        assertEquals(misses, PixelBufferPlanar.misses.get())
        onPipeline { PixelBufferGl.upload(Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888), buffer) }
        assertEquals(badTargets + 1, PixelBufferPlanar.badTargets.get())
        releaseLease(buffer)
    }

    @Test
    fun planarBuffersKeepTheirPoolAsVisionIdentity() {
        val pool = makePool(PixelBufferLayout.ycbcr420Full)
        val first = heldBuffer(pool)
        val second = heldBuffer(pool)
        assertSame(pool.state, visionIdentity(first))
        assertSame(visionIdentity(first), visionIdentity(second))
        val unpooled = fakePlanar()
        assertSame(unpooled.backing, visionIdentity(unpooled))
        releaseLease(first)
        releaseLease(second)
    }

    @Test
    fun planarPoolsFallBackOneBufferAtATimeOnceStorageIsTurnedOff() {
        val pool = makePool(PixelBufferLayout.ycbcr420Full)
        val planar = heldBuffer(pool)
        assertEquals(PixelBufferLayout.ycbcr420Full, planar.layout)
        YCbCrStorage.disable("test")
        val fallback = heldBuffer(pool)
        assertEquals(PixelBufferLayout.rgba8, fallback.layout)
        assertEquals(PixelBufferLayout.ycbcr420Full, pool.layout)
        assertEquals(kCVPixelFormatType_420YpCbCr8BiPlanarFullRange, fallback.pixelFormatType)
        assertTrue(fallback.checkRenderable("fallback target"))
        assertEquals(PixelBufferLayout.rgba8, YCbCrStorage.layoutFor(0x23))
        val bytes = onPipeline { synchronized(PixelBufferReaper) { pool.state.allocatedBytes } }
        assertEquals(64L * 32 + 2L * 32 * 16 + 4L * 64 * 32, bytes)
        releaseLease(planar)
        releaseLease(fallback)
        onPipeline { pool.invalidate() }
        assertEquals(0L, onPipeline { synchronized(PixelBufferReaper) { pool.state.allocatedBytes } })
    }

    @Test
    fun onlyDecoderAndReaderStorageFollowsTheTag() {
        YCbCrStorage.override = true
        assertEquals(PixelBufferLayout.rgba8, CVPixelBufferPool(64, 32, 0x23, 12).layout)
        val effects = assertNotNull(
            CVPixelBufferPoolCreate(
                mapOf(
                    kCVPixelBufferWidthKey to 64,
                    kCVPixelBufferHeightKey to 32,
                    kCVPixelBufferPixelFormatTypeKey to 0x23,
                )
            )
        )
        pools.add(effects)
        assertEquals(PixelBufferLayout.rgba8, effects.layout)
        val decoded = heldBuffer(makePool(PixelBufferLayout.ycbcr420Full))
        val sample = MediaSample(ByteArray(0), 0, true, null, decoded, 33_333)
        val buffered = assertNotNull(CVPixelBufferPool.matching(null, sample))
        pools.add(buffered)
        assertEquals(PixelBufferLayout.rgba8, buffered.layout)
        assertEquals("buffered", buffered.name)
        assertEquals(kCVPixelFormatType_420YpCbCr8BiPlanarFullRange, buffered.pixelFormatType)
        val copy = heldBuffer(buffered)
        assertEquals(PixelBufferLayout.rgba8, copy.layout)
        val black = assertNotNull(makeBlackPixelBuffer(64, 32))
        assertEquals(PixelBufferLayout.rgba8, black.layout)
        releaseLease(decoded)
        releaseLease(copy)
    }
}

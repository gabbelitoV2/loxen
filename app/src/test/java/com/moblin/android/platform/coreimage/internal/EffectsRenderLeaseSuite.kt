package com.moblin.android.platform.coreimage.internal

import com.moblin.android.platform.coregraphics.CGAffineTransform
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.video.CVPixelBuffer
import com.moblin.android.platform.video.PixelBufferBacking
import com.moblin.android.platform.video.kCVPixelFormatType_32BGRA
import com.moblin.android.platform.video.kCVPixelFormatType_420YpCbCr8BiPlanarFullRange
import com.moblin.android.platform.video.releaseLease
import com.moblin.android.platform.video.swapLease
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EffectsRenderLeaseSuite {
    private fun leasedBuffer(pixelFormatType: Int = kCVPixelFormatType_32BGRA): CVPixelBuffer {
        return CVPixelBuffer(PixelBufferBacking(3, 4, 1920, 1080), null, pixelFormatType, leased = true)
    }

    private fun source(buffer: CVPixelBuffer): ImageNode {
        return PixelBufferNode(buffer, SourceAlpha.asIs, SourceEncoding.srgb)
    }

    @Test
    fun renderLeasesSourcesAndTargetUntilReleased() {
        val camera = leasedBuffer(kCVPixelFormatType_420YpCbCr8BiPlanarFullRange)
        val overlay = leasedBuffer()
        val output = leasedBuffer(kCVPixelFormatType_420YpCbCr8BiPlanarFullRange)
        val cameraNode = source(camera)
        val root = CompositeNode(
            TransformNode(source(overlay), CGAffineTransform(1.0, 0.0, 0.0, 1.0, 10.0, 20.0), false),
            CropNode(cameraNode, CGRect(0.0, 0.0, 1920.0, 1080.0))
        )
        val leases = RenderLeases.acquire(CompositeNode(root, cameraNode), output, "CIContext.render")
        assertEquals(3, leases.count)
        assertEquals(2, camera.leaseCount.get())
        assertEquals(2, overlay.leaseCount.get())
        assertEquals(2, output.leaseCount.get())
        leases.release()
        assertEquals(0, leases.count)
        assertEquals(1, camera.leaseCount.get())
        assertEquals(1, overlay.leaseCount.get())
        assertEquals(1, output.leaseCount.get())
        assertTrue(camera.isValid)
    }

    @Test
    fun nestedRendersKeepTheirOwnLeases() {
        val camera = leasedBuffer()
        val outer = RenderLeases.acquire(source(camera), null, "MTIContext.render")
        val inner = RenderLeases.acquire(source(camera), null, "CIContext.createCGImage")
        assertEquals(3, camera.leaseCount.get())
        inner.release()
        assertEquals(2, camera.leaseCount.get())
        outer.release()
        assertEquals(1, camera.leaseCount.get())
    }

    @Test
    fun recycledSourceIsSkipped() {
        val camera = leasedBuffer()
        camera.backing.generation += 1
        assertFalse(camera.isValid)
        val leases = RenderLeases.acquire(source(camera), null, "CIContext.render")
        assertEquals(0, leases.count)
        assertEquals(1, camera.leaseCount.get())
        leases.release()
        assertEquals(1, camera.leaseCount.get())
    }

    @Test
    fun heldImageKeepsItsBufferUntilReplaced() {
        class Held(val buffer: CVPixelBuffer)
        val first = leasedBuffer()
        val second = leasedBuffer()
        var held: Held? = null
        held = swapLease(held, Held(first)) { it.buffer }
        releaseLease(first)
        assertTrue(first.isValid)
        val leases = RenderLeases.acquire(source(held!!.buffer), null, "CIContext.render")
        assertEquals(1, leases.count)
        leases.release()
        assertTrue(first.isValid)
        held = swapLease(held, Held(second)) { it.buffer }
        releaseLease(second)
        assertFalse(first.isValid)
        assertTrue(second.isValid)
        assertEquals(1, second.leaseCount.get())
        swapLease(held, null) { it.buffer }
        assertFalse(second.isValid)
    }

    @Test
    fun lastReleaseAfterRenderRecyclesBuffer() {
        val overlay = leasedBuffer()
        val leases = RenderLeases.acquire(source(overlay), null, "CIContext.render")
        releaseLease(overlay)
        assertTrue(overlay.isValid)
        leases.release()
        assertFalse(overlay.isValid)
    }
}

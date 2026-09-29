package com.moblin.android.platform.video

import android.os.Looper
import com.moblin.android.AppDelegate
import com.moblin.android.media.MediaSample
import com.moblin.android.media.haishinkit.media.video.BufferedVideo
import com.moblin.android.media.haishinkit.media.video.PreviewView
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.media.haishinkit.media.video.VideoEffectsProcessor
import com.moblin.android.media.haishinkit.media.video.VideoUnit
import com.moblin.android.platform.avfoundation.AVCaptureDevice
import com.moblin.android.platform.core.PipelineThread
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.various.settings.SettingsStreamColorRange
import java.util.UUID
import kotlin.math.max
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLog

private class OverlayEffect : VideoEffect() {
    override fun execute(image: CIImage, info: VideoEffectInfo): CIImage {
        return CIImage.black.cropped(to = CGRect(x = 0.0, y = 0.0, width = 64.0, height = 64.0)).composited(over = image)
    }
}

private class Frames(private val pool: CVPixelBufferPool) {
    var presentationTimeUs = 10_000_000L
    var drops = 0
    val sent = mutableListOf<MediaSample>()

    fun next(): MediaSample? {
        val buffer = pool.createPixelBuffer()
        presentationTimeUs += 33_333
        if (buffer == null) {
            drops += 1
            return null
        }
        val sample = MediaSample(
            ByteArray(0),
            presentationTimeUs,
            true,
            CMVideoFormatDescriptionCreateForImageBuffer(buffer),
            buffer,
        )
        sent.add(sample)
        return sample
    }
}

@RunWith(RobolectricTestRunner::class)
class PreviewLeaseSuite {
    private val pools = mutableListOf<CVPixelBufferPool>()
    private val videoUnits = mutableListOf<VideoUnit>()
    private val device = AVCaptureDevice.makeAudioDevice("preview-lease-camera", "Camera", null, null)

    @After
    fun tearDown() {
        for (videoUnit in videoUnits) {
            videoUnit.dispose()
        }
        for (pool in pools) {
            pool.invalidate()
        }
    }

    private fun <T> onPipeline(block: () -> T): T {
        val result = PipelineThread.runSync(timeoutMs = 5000, block = block)
        PipelineThread.runSync(timeoutMs = 5000) {}
        return result
    }

    private fun runMainThread() {
        shadowOf(Looper.getMainLooper()).idle()
        onPipeline {}
        shadowOf(Looper.getMainLooper()).idle()
        onPipeline {}
    }

    private fun inUse(pool: CVPixelBufferPool): Int {
        return onPipeline { synchronized(PixelBufferReaper) { pool.state.allocated - pool.state.free.size } }
    }

    private fun allocated(pool: CVPixelBufferPool): Int {
        return onPipeline { synchronized(PixelBufferReaper) { pool.state.allocated } }
    }

    private fun <T> privateField(owner: Any, name: String): T {
        val field = owner.javaClass.getDeclaredField(name)
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return field.get(owner) as T
    }

    private fun shownBuffer(view: PreviewView): CVPixelBuffer? {
        return onPipeline { privateField<CVPixelBuffer?>(view.layer, "lastBuffer") }
    }

    private fun makePool(name: String, maximumBufferCount: Int): CVPixelBufferPool {
        val pool = CVPixelBufferPool(1920, 1080, kCVPixelFormatType_32BGRA, maximumBufferCount)
        pool.name = name
        pools.add(pool)
        return pool
    }

    private fun makeVideoUnit(view: PreviewView): VideoUnit {
        val videoUnit = VideoUnit(SettingsStreamColorRange.full)
        videoUnits.add(videoUnit)
        videoUnit.drawable = view
        return videoUnit
    }

    private fun sceneCameraId(videoUnit: VideoUnit): UUID {
        return onPipeline { privateField<UUID>(videoUnit, "sceneVideoSourceId") }
    }

    private fun effectsPool(videoUnit: VideoUnit): CVPixelBufferPool? {
        val effectsProcessor = privateField<VideoEffectsProcessor>(videoUnit, "effectsProcessor")
        return onPipeline { privateField<CVPixelBufferPool?>(effectsProcessor, "pool") }
    }

    private fun assertNoStaleBuffers() {
        val messages = ShadowLog.getLogsForTag("MoblinPipeline").map { it.msg }
        assertTrue(messages.none { it.startsWith("Stale pixel buffer") || it.contains("too many times") }, "$messages")
    }

    @Test
    fun stalledMainThreadKeepsOnlyTheNewestFrameLeased() {
        val pool = makePool("effects", 16)
        val view = PreviewView(AppDelegate.context)
        val frames = Frames(pool)
        repeat(60) {
            onPipeline {
                frames.next()?.let { view.enqueue(it, isFirstAfterAttach = false) }
            }
        }
        assertEquals(0, frames.drops)
        assertEquals(2, allocated(pool))
        assertEquals(1, inUse(pool))
        runMainThread()
        assertSame(frames.sent.last().imageBuffer, shownBuffer(view))
        assertEquals(1, inUse(pool))
        assertEquals(1, frames.sent.last().imageBuffer!!.leaseCount.get())
        assertNoStaleBuffers()
    }

    @Test
    fun framesKeepTheirOrderAroundFirstFrameAfterAttach() {
        val pool = makePool("effects", 16)
        val view = PreviewView(AppDelegate.context)
        val frames = Frames(pool)
        onPipeline { view.enqueue(frames.next()!!, isFirstAfterAttach = false) }
        runMainThread()
        assertSame(frames.sent[0].imageBuffer, shownBuffer(view))
        onPipeline { view.enqueue(frames.next()!!, isFirstAfterAttach = false) }
        onPipeline { view.enqueue(frames.next()!!, isFirstAfterAttach = true) }
        runMainThread()
        assertNull(shownBuffer(view))
        assertEquals(0, inUse(pool))
        onPipeline { view.enqueue(frames.next()!!, isFirstAfterAttach = true) }
        onPipeline { view.enqueue(frames.next()!!, isFirstAfterAttach = false) }
        runMainThread()
        assertSame(frames.sent[4].imageBuffer, shownBuffer(view))
        assertEquals(1, inUse(pool))
        onPipeline { view.enqueue(frames.next()!!, isFirstAfterAttach = false) }
        onPipeline { view.enqueue(frames.sent[5].replacePresentationTimeStamp(20_000_000), isFirstAfterAttach = false) }
        runMainThread()
        assertSame(frames.sent[5].imageBuffer, shownBuffer(view))
        assertEquals(1, inUse(pool))
        assertEquals(1, frames.sent[5].imageBuffer!!.leaseCount.get())
        assertNoStaleBuffers()
    }

    @Test
    fun sameSampleEnqueuedAgainAfterANewerOneIsShownLast() {
        val pool = makePool("camera", 12)
        val view = PreviewView(AppDelegate.context)
        val frames = Frames(pool)
        val latest = onPipeline { retainLease(frames.next()!!) }
        onPipeline { view.enqueue(latest, isFirstAfterAttach = false) }
        onPipeline { view.enqueue(frames.next()!!, isFirstAfterAttach = false) }
        onPipeline { view.enqueue(latest, isFirstAfterAttach = false) }
        assertEquals(1, inUse(pool))
        runMainThread()
        assertSame(latest.imageBuffer, shownBuffer(view))
        assertEquals(2, latest.imageBuffer!!.leaseCount.get())
        onPipeline { releaseLease(latest) }
        assertEquals(1, inUse(pool))
        assertNoStaleBuffers()
    }

    @Test
    fun olderFrameStillHeldElsewhereDoesNotReplaceTheNewestOne() {
        val pool = makePool("camera", 12)
        val view = PreviewView(AppDelegate.context)
        val frames = Frames(pool)
        val held = onPipeline { retainLease(frames.next()!!).also { view.enqueue(it, isFirstAfterAttach = false) } }
        onPipeline { view.enqueue(frames.next()!!, isFirstAfterAttach = false) }
        assertEquals(2, inUse(pool))
        runMainThread()
        assertSame(frames.sent[1].imageBuffer, shownBuffer(view))
        assertTrue(held.imageBuffer!!.isValid)
        assertEquals(1, held.imageBuffer!!.leaseCount.get())
        onPipeline { releaseLease(held) }
        assertEquals(1, inUse(pool))
        assertNoStaleBuffers()
    }

    @Test
    fun cameraFramesToAStalledPreviewDoNotExhaustTheCameraPool() {
        val view = PreviewView(AppDelegate.context)
        val videoUnit = makeVideoUnit(view)
        val cameraId = sceneCameraId(videoUnit)
        val cameraPool = makePool("camera", 12)
        val frames = Frames(cameraPool)
        repeat(60) {
            onPipeline {
                frames.next()?.let { videoUnit.videoCaptureSessionDidOutput(device, cameraId, it) }
            }
        }
        assertEquals(0, frames.drops)
        assertEquals(1, inUse(cameraPool))
        runMainThread()
        assertSame(frames.sent.last().imageBuffer, shownBuffer(view))
        assertEquals(1, inUse(cameraPool))
        assertNoStaleBuffers()
    }

    @Test
    fun effectFramesToAStalledPreviewDoNotExhaustTheEffectsOrCameraPool() {
        val view = PreviewView(AppDelegate.context)
        val videoUnit = makeVideoUnit(view)
        videoUnit.registerEffect(OverlayEffect())
        val cameraId = sceneCameraId(videoUnit)
        val cameraPool = makePool("camera", 12)
        val frames = Frames(cameraPool)
        repeat(60) {
            onPipeline {
                frames.next()?.let { videoUnit.videoCaptureSessionDidOutput(device, cameraId, it) }
            }
        }
        val effectsPool = assertNotNull(effectsPool(videoUnit))
        assertEquals("effects", effectsPool.name)
        assertEquals(0, frames.drops)
        assertEquals(2, allocated(effectsPool))
        assertEquals(1, inUse(effectsPool))
        assertEquals(1, inUse(cameraPool))
        runMainThread()
        val shown = assertNotNull(shownBuffer(view))
        assertSame(effectsPool.state, shown.poolState)
        assertTrue(shown.isValid)
        assertEquals(1, inUse(effectsPool))
        assertEquals(1, inUse(cameraPool))
        assertFalse(frames.sent.dropLast(1).any { it.imageBuffer!!.isValid })
        assertNoStaleBuffers()
    }

    @Test
    fun defaultBuiltinDelayKeepsThreeCameraFramesWhileThePreviewIsStalled() {
        val view = PreviewView(AppDelegate.context)
        val videoUnit = makeVideoUnit(view)
        val cameraId = sceneCameraId(videoUnit)
        onPipeline {
            val bufferedVideo = BufferedVideo(
                cameraId = cameraId,
                name = "Camera",
                update = false,
                latency = 0.07,
                processor = null,
                driftTracker = null,
            )
            privateField<MutableMap<UUID, BufferedVideo>>(videoUnit, "bufferedVideos")[cameraId] = bufferedVideo
            privateField<MutableMap<Any, BufferedVideo>>(videoUnit, "bufferedVideoBuiltins")[device] = bufferedVideo
        }
        val cameraPool = makePool("camera", 12)
        val frames = Frames(cameraPool)
        var maximumInUse = 0
        repeat(90) {
            onPipeline {
                frames.next()?.let { videoUnit.videoCaptureSessionDidOutput(device, cameraId, it) }
            }
            maximumInUse = max(maximumInUse, inUse(cameraPool))
        }
        assertEquals(0, frames.drops)
        assertEquals(3, maximumInUse)
        runMainThread()
        assertSame(frames.sent[frames.sent.size - 3].imageBuffer, shownBuffer(view))
        assertEquals(3, inUse(cameraPool))
        assertNoStaleBuffers()
    }
}

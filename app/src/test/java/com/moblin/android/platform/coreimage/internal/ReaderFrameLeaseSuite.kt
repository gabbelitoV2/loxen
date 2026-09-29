package com.moblin.android.platform.coreimage.internal

import android.graphics.Bitmap
import android.media.MediaFormat
import android.util.Size
import com.moblin.android.platform.avfoundation.AVAsset
import com.moblin.android.platform.avfoundation.AVAssetReader
import com.moblin.android.platform.avfoundation.AVAssetReaderTrackOutput
import com.moblin.android.platform.avfoundation.AVAssetTrack
import com.moblin.android.platform.avfoundation.AVMediaType
import com.moblin.android.platform.avfoundation.makeReaderPool
import com.moblin.android.platform.core.PipelineThread
import com.moblin.android.platform.coregraphics.CGPoint
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.coreimage.drainImageLeases
import com.moblin.android.platform.coreimage.releaseImageLeases
import com.moblin.android.platform.coreimage.retainImageLeases
import com.moblin.android.platform.coreimage.swapImageLease
import com.moblin.android.platform.video.CVPixelBuffer
import com.moblin.android.platform.video.CVPixelBufferPool
import com.moblin.android.platform.video.PixelBufferBacking
import com.moblin.android.platform.video.PixelBufferLayout
import com.moblin.android.platform.video.PixelBufferLeases
import com.moblin.android.platform.video.PixelBufferPoolState
import com.moblin.android.platform.video.PixelBufferReaper
import com.moblin.android.platform.video.UNLEASED
import com.moblin.android.platform.video.YCbCrStorage
import com.moblin.android.platform.video.kCVPixelFormatType_32BGRA
import com.moblin.android.platform.video.kCVPixelFormatType_420YpCbCr8BiPlanarFullRange
import com.moblin.android.various.ReplayBufferFile
import com.moblin.android.various.settings.SettingsStreamColorRange
import com.moblin.android.various.settings.SettingsWidgetLayout
import com.moblin.android.videoeffects.EffectImageCiImage
import com.moblin.android.videoeffects.alerts.AlertsEffectVideoReader
import com.moblin.android.videoeffects.centered
import com.moblin.android.videoeffects.replay.ReplayEffect
import com.moblin.android.videoeffects.replay.ReplayEffectDelegate
import com.moblin.android.videoeffects.replay.ReplayEffectReplayReader
import com.moblin.android.videoeffects.replay.ReplayEffectStingerReader
import com.moblin.android.videoeffects.replay.ReplayEffectTransitionMode
import com.moblin.android.videoeffects.replay.ReplayImage
import com.moblin.android.videoeffects.replay.replayEffectQueue
import com.moblin.android.videoeffects.scaledTo
import com.moblin.android.videoeffects.toEffectImage
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReaderFrameLeaseSuite {
    private val size = CGSize(1920.0, 1080.0)
    private val pool = PixelBufferPoolState(1920, 1080, 32, "reader")
    private var nextName = 1000
    private val overlay = CIImage(cgImage = Bitmap.createBitmap(64, 32, Bitmap.Config.ARGB_8888))

    private val delegate = object : ReplayEffectDelegate {
        override fun replayEffectStatus(timeLeft: Int) {}

        override fun replayEffectCompleted() {}

        override fun replayEffectError(message: String) {}
    }

    private fun readerFrame(): CVPixelBuffer {
        val backing = synchronized(PixelBufferReaper) {
            val backing = pool.free.removeFirstOrNull()
                ?: PixelBufferBacking(nextName++, nextName++, 1920, 1080).also { pool.allocated += 1 }
            pool.leased += 1
            backing
        }
        return CVPixelBuffer(backing, pool, kCVPixelFormatType_32BGRA, leased = true)
    }

    @After
    fun tearDown() {
        YCbCrStorage.override = null
    }

    private fun decodedReaderFrame(pool: CVPixelBufferPool): CVPixelBuffer {
        return onPipeline {
            val buffer = PixelBufferReaper.obtain(pool.state, pool.pixelFormatType, leased = true)!!
            PixelBufferLeases.retain(buffer, "AVAssetReader")
            buffer
        }
    }

    private fun readerLeaseTrace(pool: CVPixelBufferPool): List<Any> {
        val trace = mutableListOf<Any>()
        val frame = decodedReaderFrame(pool)
        trace.add(frame.leaseCount.get())
        val shown = replayImage(frame, 0.0).image!!.getCiImage()
        retainImageLeases(shown)
        trace.add(frame.leaseCount.get())
        releaseImageLeases(shown)
        trace.add(frame.leaseCount.get())
        trace.add(frame.isValid)
        releaseImageLeases(shown)
        trace.add(frame.isValid)
        val queued = List(3) { decodedReaderFrame(pool) }
        val images = ArrayDeque(queued.mapIndexed { index, buffer -> replayImage(buffer, index / 30.0) })
        var held: EffectImageCiImage? = null
        held = swapImageLease(held, images.first().image) { it.getCiImage() }
        drainImageLeases(images) { it.image?.getCiImage() }
        trace.add(queued.map { it.isValid })
        held = swapImageLease(held, null) { it.getCiImage() }
        trace.add(held == null)
        trace.add(queued.map { it.isValid })
        trace.add(synchronized(PixelBufferReaper) { pool.state.leased })
        trace.add(synchronized(PixelBufferReaper) { pool.state.allocated == pool.state.free.size })
        pool.invalidate()
        return trace
    }

    @Test
    fun globalTagReaderFramesArePlanarAndEffectReaderFramesStayRgba() {
        YCbCrStorage.override = true
        val replay = makeReaderPool(1920, 1080, SettingsStreamColorRange.full.pixelFormatType())
        val alerts = makeReaderPool(1920, 1080, kCVPixelFormatType_32BGRA)
        assertEquals(PixelBufferLayout.ycbcr420Full, replay.layout)
        assertEquals(kCVPixelFormatType_420YpCbCr8BiPlanarFullRange, replay.pixelFormatType)
        assertEquals(PixelBufferLayout.rgba8, alerts.layout)
        assertEquals(kCVPixelFormatType_32BGRA, alerts.pixelFormatType)
        assertEquals("reader", replay.name)
        assertEquals(32, replay.maximumBufferCount)
        assertTrue(decodedReaderFrame(replay).layout.isPlanar)
        val planar = readerLeaseTrace(makeReaderPool(1920, 1080, 0x23))
        val rgba = readerLeaseTrace(makeReaderPool(1920, 1080, kCVPixelFormatType_32BGRA))
        assertEquals(listOf<Any>(1, 2, 1, true, false, listOf(true, false, false), true, listOf(false, false, false), 0, true), rgba)
        assertEquals(rgba, planar)
        replay.invalidate()
        alerts.invalidate()
    }

    private fun replayImage(buffer: CVPixelBuffer, offset: Double): ReplayImage {
        val image = CIImage(cvPixelBuffer = buffer)
            .scaledTo(size = size)
            .centered(size = size)
            .composited(over = CIImage.black.cropped(to = CGRect(origin = CGPoint.zero, size = size)))
        return ReplayImage(image = overlay.composited(over = image).toEffectImage(isOpaque = true), offset = offset, isLast = false)
    }

    private fun stingerImage(buffer: CVPixelBuffer, offset: Double): ReplayImage {
        val image = CIImage(cvPixelBuffer = buffer)
            .scaledTo(size = size)
            .centered(size = size)
            .composited(over = CIImage.clear.cropped(to = CGRect(origin = CGPoint.zero, size = size)))
        return ReplayImage(image = image.toEffectImage(isOpaque = false), offset = offset, isLast = false)
    }

    private fun <T> onPipeline(block: () -> T): T {
        val result = PipelineThread.runSync(timeoutMs = 5000, block = block)
        PipelineThread.runSync(timeoutMs = 5000) {}
        return result
    }

    private fun <T> createdOffMain(block: () -> T): T {
        val executor = Executors.newSingleThreadExecutor()
        try {
            return executor.submit(Callable(block)).get()
        } finally {
            executor.shutdown()
        }
    }

    private fun <T> privateField(owner: Any, name: String): T {
        val field = owner.javaClass.getDeclaredField(name)
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return field.get(owner) as T
    }

    private fun flushReplayEffectQueue() {
        runBlocking { replayEffectQueue.launch {}.join() }
        onPipeline {}
    }

    private fun assertAllFramesReturned() {
        synchronized(PixelBufferReaper) {
            assertEquals(0, pool.leased)
            assertEquals(pool.allocated, pool.free.size)
        }
    }

    private fun replayEffect(transitionMode: ReplayEffectTransitionMode): ReplayEffect {
        return createdOffMain {
            ReplayEffect(
                video = ReplayBufferFile("/nonexistent/replay.mp4", 10.0, false),
                start = 0.0,
                stop = 10.0,
                speed = 1.0,
                size = Size(1920, 1080),
                layout = SettingsWidgetLayout(),
                transitionMode = transitionMode,
                pixelFormatType = SettingsStreamColorRange.full.pixelFormatType(),
                delegate = delegate,
            )
        }
    }

    @Test
    fun imageLeasesFollowThePixelBuffersTheImageReads() {
        val frame = readerFrame()
        val camera = CVPixelBuffer(PixelBufferBacking(1, 2, 1920, 1080), null, kCVPixelFormatType_32BGRA)
        val image = replayImage(frame, 0.0).image!!.getCiImage().composited(over = CIImage(cvPixelBuffer = camera))
        retainImageLeases(image)
        assertEquals(2, frame.leaseCount.get())
        assertEquals(UNLEASED, camera.leaseCount.get())
        releaseImageLeases(image)
        assertEquals(1, frame.leaseCount.get())
        assertTrue(frame.isValid)
        releaseImageLeases(image)
        assertFalse(frame.isValid)
        assertTrue(camera.isValid)
        releaseImageLeases(overlay)
        releaseImageLeases(null)
        assertAllFramesReturned()
    }

    @Test
    fun shownImageKeepsItsFrameAfterTheQueueIsDrained() {
        val first = readerFrame()
        val second = readerFrame()
        val queue = ArrayDeque(listOf(replayImage(first, 0.0), replayImage(second, 1 / 30.0)))
        queue.addLast(ReplayImage(image = null, offset = null, isLast = true))
        var shown: EffectImageCiImage? = null
        shown = swapImageLease(shown, queue.first().image) { it.getCiImage() }
        shown = swapImageLease(shown, shown) { it.getCiImage() }
        assertEquals(2, first.leaseCount.get())
        drainImageLeases(queue) { it.image?.getCiImage() }
        assertTrue(queue.isEmpty())
        assertTrue(first.isValid)
        assertFalse(second.isValid)
        shown = swapImageLease(shown, null) { it.getCiImage() }
        assertEquals(null, shown)
        assertFalse(first.isValid)
        assertAllFramesReturned()
    }

    @Test
    fun replayReaderReturnsPassedFramesAtTheEndOfTheTurn() {
        val reader = createdOffMain {
            ReplayEffectReplayReader(
                video = ReplayBufferFile("/nonexistent/replay.mp4", 10.0, false),
                start = 0.0,
                duration = 10.0,
                size = Size(1920, 1080),
                pixelFormatType = SettingsStreamColorRange.full.pixelFormatType(),
            )
        }
        val images: ArrayDeque<ReplayImage> = privateField(reader, "images")
        val frames = List(4) { readerFrame() }
        val queued = frames.mapIndexed { index, frame -> replayImage(frame, index / 30.0) }
        onPipeline { images.addAll(queued) }
        val validInTurn = onPipeline {
            val image = reader.getImage(offset = 0.05)
            assertSame(queued[2].image, image.image)
            frames[0].isValid && frames[1].isValid
        }
        assertTrue(validInTurn)
        assertFalse(frames[0].isValid)
        assertFalse(frames[1].isValid)
        assertTrue(frames[2].isValid)
        assertEquals(1, frames[2].leaseCount.get())
        assertEquals(2, synchronized(PixelBufferReaper) { pool.free.size })
        onPipeline { reader.close() }
        flushReplayEffectQueue()
        assertTrue(images.isEmpty())
        assertAllFramesReturned()
    }

    @Test
    fun replayEffectKeepsTheShownFrameUntilReplacedOrRemoved() {
        val effect = replayEffect(ReplayEffectTransitionMode.None)
        val reader: ReplayEffectReplayReader = privateField(effect, "reader")
        val images: ArrayDeque<ReplayImage> = privateField(reader, "images")
        val update = ReplayEffect::class.java.getDeclaredMethod("update", Double::class.javaPrimitiveType)
        update.isAccessible = true
        val frames = List(3) { readerFrame() }
        onPipeline { frames.forEachIndexed { index, frame -> images.addLast(replayImage(frame, index / 30.0)) } }
        onPipeline { update.invoke(effect, 100.0) }
        assertFalse(frames[0].isValid)
        assertEquals(2, frames[1].leaseCount.get())
        onPipeline { update.invoke(effect, 100.05) }
        assertFalse(frames[1].isValid)
        assertEquals(2, frames[2].leaseCount.get())
        onPipeline { update.invoke(effect, 100.1) }
        assertTrue(images.isEmpty())
        assertTrue(frames[2].isValid)
        assertEquals(1, frames[2].leaseCount.get())
        val leftOver = List(2) { readerFrame() }
        onPipeline { leftOver.forEachIndexed { index, frame -> images.addLast(replayImage(frame, 5.0 + index / 30.0)) } }
        onPipeline { effect.removed() }
        flushReplayEffectQueue()
        assertFalse(frames[2].isValid)
        assertFalse(leftOver[0].isValid)
        assertFalse(leftOver[1].isValid)
        assertTrue(images.isEmpty())
        assertAllFramesReturned()
    }

    @Test
    fun removedReplayEffectReturnsQueuedStingerFrames() {
        val effect = replayEffect(
            ReplayEffectTransitionMode.Stingers(
                inPath = "/nonexistent/in.mp4",
                inTransitionPoint = 0.5,
                outPath = "/nonexistent/out.mp4",
                outTransitionPoint = 0.5,
            ),
        )
        val stingersIn: ReplayEffectStingerReader = privateField(effect, "stingersInReader")
        val stingersOut: ReplayEffectStingerReader = privateField(effect, "stingersOutReader")
        val inImages: ArrayDeque<ReplayImage> = privateField(stingersIn, "images")
        val outImages: ArrayDeque<ReplayImage> = privateField(stingersOut, "images")
        val inFrames = List(3) { readerFrame() }
        val outFrames = List(11) { readerFrame() }
        onPipeline {
            inFrames.forEachIndexed { index, frame -> inImages.addLast(stingerImage(frame, index / 30.0)) }
            outFrames.forEachIndexed { index, frame -> outImages.addLast(stingerImage(frame, index / 30.0)) }
        }
        val image = onPipeline { stingersIn.getImage(offset = 0.05) }
        assertTrue(image?.image != null)
        assertFalse(inFrames[0].isValid)
        assertFalse(inFrames[1].isValid)
        assertTrue(inFrames[2].isValid)
        assertTrue(outFrames.all { it.isValid })
        onPipeline { effect.removed() }
        flushReplayEffectQueue()
        assertTrue(inImages.isEmpty())
        assertTrue(outImages.isEmpty())
        assertAllFramesReturned()
    }

    private fun closeCancelsTheReaderWhileAFillIsRunning(owner: Any, close: () -> Unit) {
        val assetReader = AVAssetReader(asset = AVAsset(url = "/nonexistent/replay.mp4"))
        val readerField = owner.javaClass.getDeclaredField("reader")
        readerField.isAccessible = true
        readerField.set(owner, assetReader)
        val images: ArrayDeque<ReplayImage> = privateField(owner, "images")
        val frames = List(3) { readerFrame() }
        onPipeline { frames.forEachIndexed { index, frame -> images.addLast(replayImage(frame, index / 30.0)) } }
        val fillStarted = CountDownLatch(1)
        val fillEnded = CountDownLatch(1)
        val fillMayEnd = CountDownLatch(1)
        replayEffectQueue.launch {
            fillStarted.countDown()
            val deadlineNs = System.nanoTime() + TimeUnit.SECONDS.toNanos(2)
            while (assetReader.status != AVAssetReader.Status.cancelled && System.nanoTime() < deadlineNs) {
                Thread.sleep(4)
            }
            fillEnded.countDown()
            fillMayEnd.await(5, TimeUnit.SECONDS)
        }
        assertTrue(fillStarted.await(5, TimeUnit.SECONDS))
        val closedNs = System.nanoTime()
        onPipeline { close() }
        assertTrue(fillEnded.await(5, TimeUnit.SECONDS))
        assertTrue(System.nanoTime() - closedNs < TimeUnit.MILLISECONDS.toNanos(1500))
        assertEquals(AVAssetReader.Status.cancelled, assetReader.status)
        assertTrue(frames.all { it.isValid })
        fillMayEnd.countDown()
        flushReplayEffectQueue()
        assertTrue(images.isEmpty())
        assertAllFramesReturned()
    }

    @Test
    fun closeCancelsTheReaderWithoutWaitingForARunningFill() {
        val replay = createdOffMain {
            ReplayEffectReplayReader(
                video = ReplayBufferFile("/nonexistent/replay.mp4", 10.0, false),
                start = 0.0,
                duration = 10.0,
                size = Size(1920, 1080),
                pixelFormatType = SettingsStreamColorRange.full.pixelFormatType(),
            )
        }
        closeCancelsTheReaderWhileAFillIsRunning(replay) { replay.close() }
        val stinger = createdOffMain { ReplayEffectStingerReader(path = "/nonexistent/out.mp4", size = Size(1920, 1080)) }
        flushReplayEffectQueue()
        closeCancelsTheReaderWhileAFillIsRunning(stinger) { stinger.close() }
    }

    @Test
    fun effectReadersAskForLeasedFrames() {
        val track = AVAssetTrack(
            mediaType = AVMediaType.video,
            naturalSize = size,
            nominalFrameRate = 30f,
            trackID = 1,
            trackIndex = 0,
            format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 1920, 1080),
        )
        val alerts = AlertsEffectVideoReader(path = "/nonexistent/alert.mp4")
        val stinger = createdOffMain { ReplayEffectStingerReader(path = "/nonexistent/in.mp4", size = Size(1920, 1080)) }
        val replay = createdOffMain {
            ReplayEffectReplayReader(
                video = ReplayBufferFile("/nonexistent/replay.mp4", 10.0, false),
                start = 0.0,
                duration = 10.0,
                size = Size(1920, 1080),
                pixelFormatType = SettingsStreamColorRange.full.pixelFormatType(),
            )
        }
        val load = { owner: Any, argument: Any ->
            val method = owner.javaClass.declaredMethods.single { it.name == "loadVideoTrackCompletion" }
            method.isAccessible = true
            method.invoke(owner, argument, null)
        }
        load(alerts, track)
        load(stinger, track)
        load(replay, listOf(track))
        for (owner in listOf(alerts, stinger, replay)) {
            val output: AVAssetReaderTrackOutput = privateField(owner, "trackOutput")
            assertTrue(output.leasesSampleBuffers, owner.javaClass.simpleName)
        }
    }

    @Test
    fun alertsReaderReusesItsFramesWithoutGrowingThePool() {
        val reader = AlertsEffectVideoReader(path = "/nonexistent/alert.mp4")
        val images: ArrayDeque<Any> = privateField(reader, "images")
        val videoImage = Class.forName("com.moblin.android.videoeffects.alerts.VideoImage").declaredConstructors.single()
        videoImage.isAccessible = true
        val totalFrames = 150
        var produced = 0
        fun fill() {
            onPipeline {
                repeat(11) {
                    if (produced < totalFrames) {
                        val image = CIImage(cvPixelBuffer = readerFrame()).toEffectImage(isOpaque = true)
                        images.addLast(videoImage.newInstance(image, produced / 30.0))
                        produced += 1
                    }
                }
            }
        }
        fill()
        var maximumAllocated = 0
        for (index in 0 until totalFrames + 10) {
            onPipeline { reader.getImage(presentationTimeStamp = 10.0 + index / 30.0) }
            if (onPipeline { images.size } < 10) {
                fill()
            }
            maximumAllocated = max(maximumAllocated, synchronized(PixelBufferReaper) { pool.allocated })
        }
        assertEquals(totalFrames, produced)
        assertTrue(maximumAllocated <= 21, "allocated $maximumAllocated")
        assertTrue(onPipeline { images.isEmpty() })
        assertAllFramesReturned()
    }
}

package com.moblin.android.platform.video

import android.media.MediaFormat
import com.moblin.android.media.MediaSample
import com.moblin.android.media.haishinkit.media.video.BufferedVideo
import com.moblin.android.platform.core.PipelineThread
import com.moblin.android.platform.videotoolbox.VTDecompressionOutputHandler
import com.moblin.android.platform.videotoolbox.VTDecompressionSession
import com.moblin.android.platform.videotoolbox.noErr
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private var nowMs = 1_000_000_000_000L

@RunWith(RobolectricTestRunner::class)
class PixelBufferTrimSuite {
    private val windowMs = 5000L
    private val pools = mutableListOf<CVPixelBufferPool>()

    @Before
    fun setUp() {
        nowMs += 1_000_000_000L
        PipelineThread.runSync(timeoutMs = 5000) { PixelBufferReaper.trimIdle(nowMs) }
    }

    @After
    fun tearDown() {
        for (pool in pools) {
            pool.invalidate()
        }
    }

    private fun <T> onPipeline(block: () -> T): T {
        val result = PipelineThread.runSync(timeoutMs = 5000, block = block)
        PipelineThread.runSync(timeoutMs = 5000) {}
        return result
    }

    private fun nextWindow() {
        nowMs += windowMs
        onPipeline { PixelBufferReaper.trimIdle(nowMs) }
    }

    private fun counts(state: PixelBufferPoolState): Triple<Int, Int, Int> {
        return onPipeline {
            synchronized(PixelBufferReaper) {
                Triple(state.allocated, state.allocated - state.free.size, state.leased)
            }
        }
    }

    private class Ingest(private val fps: Int) {
        val session = VTDecompressionSession(
            MediaFormat.MIMETYPE_VIDEO_HEVC,
            "c2.test.hevc.decoder",
            MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, 1920, 1080),
            0x23,
        ).also {
            it.updateOutputSize(MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, 1920, 1080))
        }
        val frameDurationUs = 1_000_000L / fps
        val handles = mutableListOf<CVPixelBuffer>()
        private var latency = 2.0
        var bufferedVideo = makeBufferedVideo(latency)
        private var decoded: MediaSample? = null
        private var baseUs = 0L
        private var frameNumber = 0

        private val outputHandler: VTDecompressionOutputHandler =
            { status, _, imageBuffer, presentationTimeStamp, duration ->
                if (imageBuffer != null && status == noErr) {
                    handles.add(imageBuffer)
                    decoded = retainLease(MediaSample(ByteArray(0), presentationTimeStamp, false, null, imageBuffer, duration))
                }
            }

        val state: PixelBufferPoolState
            get() = session.outputPixelBufferPool(1920, 1080).state

        fun makeBufferedVideo(latency: Double): BufferedVideo {
            return BufferedVideo(
                cameraId = UUID.randomUUID(),
                name = "rist-server",
                update = true,
                latency = latency,
                processor = null,
                driftTracker = null,
            )
        }

        fun changeLatency(newLatency: Double) {
            PipelineThread.runSync { bufferedVideo.close() }
            baseUs += frameNumber * frameDurationUs
            frameNumber = 0
            latency = newLatency
            bufferedVideo = makeBufferedVideo(newLatency)
        }

        fun run(seconds: Int) {
            repeat(seconds * fps) {
                val arrivalUs = baseUs + frameNumber * frameDurationUs
                val presentationTimeUs = arrivalUs + (latency * 1_000_000).toLong()
                frameNumber += 1
                PipelineThread.runSync {
                    session.outputFrame(presentationTimeUs, frameDurationUs, outputHandler)
                }
                val sample = decoded ?: return@repeat
                decoded = null
                PipelineThread.runSync {
                    try {
                        bufferedVideo.appendSampleBuffer(sample)
                    } finally {
                        releaseLease(sample)
                    }
                }
                PipelineThread.runSync { bufferedVideo.updateSampleBuffer(arrivalUs / 1_000_000.0) }
            }
        }

        fun close() {
            PipelineThread.runSync { bufferedVideo.close() }
            session.invalidateSession()
        }
    }

    @Test
    fun idleDecoderBuffersAreFreedAfterTheLatencyDrops() {
        val ingest = Ingest(fps = 30)
        assertTrue(ingest.state.trimIdle)
        ingest.run(seconds = 20)
        val (allocatedAtTwoSeconds, inUseAtTwoSeconds, _) = counts(ingest.state)
        assertEquals(62, allocatedAtTwoSeconds)
        assertEquals(61, inUseAtTwoSeconds)
        nextWindow()
        assertEquals(62, counts(ingest.state).first)
        ingest.changeLatency(0.5)
        ingest.run(seconds = 10)
        nextWindow()
        assertEquals(62, counts(ingest.state).first)
        ingest.run(seconds = 5)
        val peak = onPipeline { synchronized(PixelBufferReaper) { ingest.state.peakInUse } }
        val (_, inUseBefore, leasedBefore) = counts(ingest.state)
        assertTrue(peak in inUseBefore..inUseBefore + 2, "peak $peak, in use $inUseBefore")
        assertTrue(peak < 30, "peak $peak")
        val readableBefore = ingest.handles.filter { it.isValid }.toSet()
        nextWindow()
        val (allocated, inUse, leased) = counts(ingest.state)
        assertEquals(peak + 2, allocated)
        val bytes = onPipeline { synchronized(PixelBufferReaper) { ingest.state.allocatedBytes } }
        assertEquals(allocated * ingest.state.layout.bytes(1920, 1080), bytes)
        assertEquals(inUseBefore, inUse)
        assertEquals(leasedBefore, leased)
        assertEquals(readableBefore, ingest.handles.filter { it.isValid }.toSet())
        val trimmed = ingest.handles.filter { handle ->
            synchronized(PixelBufferReaper) { handle.backing !in ingest.state.free } && !handle.isValid &&
                readableBefore.none { it.backing === handle.backing }
        }
        assertTrue(trimmed.isNotEmpty())
        for (handle in trimmed.take(3)) {
            assertFalse(handle.checkReadable("trimmed buffer"))
        }
        ingest.close()
    }

    @Test
    fun cameraEffectsAndBufferedPoolsAreNeverTrimmed() {
        val camera = CVPixelBufferPool(64, 32, 0x23, 12).also { it.name = "camera" }
        val effects = CVPixelBufferPoolCreate(
            mapOf(kCVPixelBufferWidthKey to 64, kCVPixelBufferHeightKey to 32, kCVPixelBufferPixelFormatTypeKey to 0x23)
        )!!
        val decoded = onPipeline { retainLease(camera.createPixelBuffer())!! }
        val buffered = CVPixelBufferPool.matching(null, MediaSample(ByteArray(0), 0, true, null, decoded, 33_333))!!
        releaseLease(decoded)
        pools.addAll(listOf(camera, effects, buffered))
        for (pool in listOf(camera, effects, buffered)) {
            assertFalse(pool.state.trimIdle)
            val held = (0 until 8).map { onPipeline { retainLease(pool.createPixelBuffer())!! } }
            for (buffer in held) {
                releaseLease(buffer)
            }
        }
        nextWindow()
        nextWindow()
        nextWindow()
        assertEquals(8, counts(camera.state).first)
        assertEquals(8, counts(effects.state).first)
        assertEquals(8, counts(buffered.state).first)
    }

    @Test
    fun readerPoolsAreTrimmedToo() {
        val reader = com.moblin.android.platform.avfoundation.makeReaderPool(64, 32, 0x23)
        pools.add(reader)
        assertTrue(reader.state.trimIdle)
        val held = (0 until 10).map { onPipeline { retainLease(reader.createPixelBuffer())!! } }
        for (buffer in held.drop(1)) {
            releaseLease(buffer)
        }
        nextWindow()
        assertEquals(10, counts(reader.state).first)
        nextWindow()
        assertEquals(Triple(3, 1, 1), counts(reader.state))
        releaseLease(held[0])
    }
}

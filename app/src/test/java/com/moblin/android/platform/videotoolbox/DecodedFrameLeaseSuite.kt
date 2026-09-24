package com.moblin.android.platform.videotoolbox

import android.media.MediaFormat
import com.moblin.android.media.MediaSample
import com.moblin.android.media.haishinkit.media.video.BufferedVideo
import com.moblin.android.platform.core.PipelineThread
import com.moblin.android.platform.video.CVPixelBuffer
import com.moblin.android.platform.video.PixelBufferLayout
import com.moblin.android.platform.video.YCbCrStorage
import com.moblin.android.platform.video.kCVPixelFormatType_32BGRA
import com.moblin.android.platform.video.kCVPixelFormatType_420YpCbCr8BiPlanarFullRange
import com.moblin.android.platform.video.releaseLease
import com.moblin.android.platform.video.retainLease
import java.util.UUID
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

private data class Output(
    val status: Int,
    val infoFlags: Int,
    val imageBuffer: CVPixelBuffer?,
    val presentationTimeStamp: Long,
    val duration: Long,
)

private class Outputs {
    val outputs = mutableListOf<Output>()

    val handler: VTDecompressionOutputHandler = { status, infoFlags, imageBuffer, presentationTimeStamp, duration ->
        outputs.add(Output(status, infoFlags, imageBuffer, presentationTimeStamp, duration))
    }
}

private const val globalPixelFormatType = 0x23

private fun makeSession(pixelFormatType: Int = kCVPixelFormatType_32BGRA): VTDecompressionSession {
    val session = VTDecompressionSession(
        MediaFormat.MIMETYPE_VIDEO_HEVC,
        "c2.test.hevc.decoder",
        MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, 1920, 1080),
        pixelFormatType,
    )
    session.updateOutputSize(MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, 1920, 1080))
    return session
}

private class Ingest(
    fps: Int,
    private val latencySeconds: Double,
    pixelFormatType: Int = kCVPixelFormatType_32BGRA,
) {
    val session = makeSession(pixelFormatType)
    val bufferedVideo = BufferedVideo(
        cameraId = UUID.randomUUID(),
        name = "rist-server",
        update = true,
        latency = latencySeconds,
        processor = null,
        driftTracker = null,
    )
    val frameDurationUs = 1_000_000L / fps
    val drops = mutableListOf<Output>()
    private var decoded: MediaSample? = null

    private val videoDecoderOutputHandler: VTDecompressionOutputHandler =
        { status, infoFlags, imageBuffer, presentationTimeStamp, duration ->
            if (imageBuffer == null || status != noErr) {
                drops.add(Output(status, infoFlags, imageBuffer, presentationTimeStamp, duration))
            } else {
                val sampleBuffer = MediaSample(ByteArray(0), presentationTimeStamp, false, null, imageBuffer, duration)
                decoded = retainLease(sampleBuffer)
            }
        }

    val allocated: Int
        get() = PipelineThread.runSync { session.outputPixelBufferPool(1920, 1080).state.allocated }

    val inUse: Int
        get() = PipelineThread.runSync {
            val state = session.outputPixelBufferPool(1920, 1080).state
            state.allocated - state.free.size
        }

    fun decode(arrivalUs: Long, basePresentationTimeUs: Long, frameNumber: Int) {
        val presentationTimeUs = basePresentationTimeUs + frameNumber * frameDurationUs
        PipelineThread.runSync {
            session.outputFrame(presentationTimeUs, frameDurationUs, videoDecoderOutputHandler)
        }
        val sampleBuffer = decoded ?: return
        decoded = null
        retainLease(sampleBuffer)
        releaseLease(sampleBuffer)
        PipelineThread.runSync {
            try {
                bufferedVideo.appendSampleBuffer(sampleBuffer)
            } finally {
                releaseLease(sampleBuffer)
            }
        }
        PipelineThread.runSync {
            bufferedVideo.updateSampleBuffer(arrivalUs / 1_000_000.0)
            val current = bufferedVideo.getLatestSampleBuffer()?.imageBuffer
            assertTrue(current == null || current.isValid)
        }
    }

    fun run(seconds: Int, firstFrameLateUs: Long) {
        val numberOfFrames = seconds * (1_000_000L / frameDurationUs).toInt()
        val basePresentationTimeUs = firstFrameLateUs + (latencySeconds * 1_000_000).toLong()
        decode(firstFrameLateUs, basePresentationTimeUs, 0)
        for (frameNumber in 1 until numberOfFrames) {
            decode(frameNumber * frameDurationUs, basePresentationTimeUs, frameNumber)
        }
    }

    fun close() {
        PipelineThread.runSync { bufferedVideo.close() }
        session.invalidateSession()
    }
}

@RunWith(RobolectricTestRunner::class)
class DecodedFrameLeaseSuite {
    @After
    fun tearDown() {
        YCbCrStorage.override = null
    }

    private fun lengthPrefixed(nalUnit: ByteArray): ByteArray {
        val size = nalUnit.size
        return byteArrayOf((size shr 24).toByte(), (size shr 16).toByte(), (size shr 8).toByte(), size.toByte()) +
            nalUnit
    }

    private fun hevcSample(presentationTimeUs: Long, keyFrame: Boolean): MediaSample {
        val nalUnit = if (keyFrame) byteArrayOf(0x26, 0x01, 0x11) else byteArrayOf(0x02, 0x01, 0x11)
        return MediaSample(lengthPrefixed(nalUnit), presentationTimeUs, keyFrame, null, null, 33_333)
    }

    @Test
    fun decodedFramesGoBackToThePoolAsBufferedVideoConsumesThem() {
        val ingest = Ingest(fps = 30, latencySeconds = 2.0)
        ingest.run(seconds = 20, firstFrameLateUs = 0)
        assertTrue(ingest.drops.isEmpty())
        assertEquals(62, ingest.allocated)
        assertEquals(61, ingest.inUse)
        val current = PipelineThread.runSync { ingest.bufferedVideo.getLatestSampleBuffer()?.imageBuffer }
        assertEquals(1, current?.leaseCount?.get())
        PipelineThread.runSync { ingest.bufferedVideo.close() }
        assertEquals(0, ingest.inUse)
        ingest.session.invalidateSession()
    }

    @Test
    fun twoSecondsOfSixtyFpsDecodeWithoutDrops() {
        val ingest = Ingest(fps = 60, latencySeconds = 2.0)
        ingest.run(seconds = 20, firstFrameLateUs = 150_000)
        assertTrue(ingest.drops.isEmpty())
        assertTrue(ingest.allocated > 64)
        assertTrue(ingest.allocated < maximumNumberOfOutputBuffers)
        ingest.close()
    }

    @Test
    fun lateFirstFrameAtThirtyFpsNeedsMoreThanSixtyFourBuffers() {
        val ingest = Ingest(fps = 30, latencySeconds = 2.0)
        ingest.run(seconds = 20, firstFrameLateUs = 150_000)
        assertTrue(ingest.drops.isEmpty())
        assertTrue(ingest.allocated > 64)
        ingest.close()
    }

    @Test
    fun decoderPoolHoldsEverythingBufferedVideoKeeps() {
        val ingest = Ingest(fps = 30, latencySeconds = 2.0)
        for (frameNumber in 0 until 300) {
            ingest.decode(0, 60_000_000, frameNumber)
        }
        assertTrue(ingest.drops.isEmpty())
        assertEquals(200, PipelineThread.runSync { ingest.bufferedVideo.numberOfBuffers() })
        assertEquals(201, ingest.inUse)
        assertEquals(202, ingest.allocated)
        val pool = ingest.session.outputPixelBufferPool(1920, 1080)
        assertEquals(maximumNumberOfOutputBuffers, pool.maximumBufferCount)
        assertEquals("decoder", pool.name)
        ingest.close()
    }

    @Test
    fun planarDecodedFramesGoBackToThePoolAsBufferedVideoConsumesThem() {
        YCbCrStorage.override = true
        val ingest = Ingest(fps = 30, latencySeconds = 2.0, pixelFormatType = globalPixelFormatType)
        ingest.run(seconds = 20, firstFrameLateUs = 0)
        assertTrue(ingest.drops.isEmpty())
        assertEquals(62, ingest.allocated)
        assertEquals(61, ingest.inUse)
        val pool = ingest.session.outputPixelBufferPool(1920, 1080)
        assertEquals(PixelBufferLayout.ycbcr420Full, pool.layout)
        assertEquals(kCVPixelFormatType_420YpCbCr8BiPlanarFullRange, pool.pixelFormatType)
        val current = PipelineThread.runSync { ingest.bufferedVideo.getLatestSampleBuffer()?.imageBuffer }
        assertEquals(1, current?.leaseCount?.get())
        assertEquals(PixelBufferLayout.ycbcr420Full, current?.layout)
        assertEquals(kCVPixelFormatType_420YpCbCr8BiPlanarFullRange, current?.pixelFormatType)
        PipelineThread.runSync { ingest.bufferedVideo.close() }
        assertEquals(0, ingest.inUse)
        ingest.session.invalidateSession()
    }

    @Test
    fun planarDecoderPoolHoldsEverythingBufferedVideoKeeps() {
        YCbCrStorage.override = true
        val ingest = Ingest(fps = 30, latencySeconds = 2.0, pixelFormatType = globalPixelFormatType)
        for (frameNumber in 0 until 300) {
            ingest.decode(0, 60_000_000, frameNumber)
        }
        assertTrue(ingest.drops.isEmpty())
        assertEquals(200, PipelineThread.runSync { ingest.bufferedVideo.numberOfBuffers() })
        assertEquals(201, ingest.inUse)
        assertEquals(202, ingest.allocated)
        val pool = ingest.session.outputPixelBufferPool(1920, 1080)
        assertEquals(maximumNumberOfOutputBuffers, pool.maximumBufferCount)
        assertEquals("decoder", pool.name)
        assertEquals(PixelBufferLayout.ycbcr420Full, pool.layout)
        assertEquals(kCVPixelFormatType_420YpCbCr8BiPlanarFullRange, pool.pixelFormatType)
        ingest.close()
    }

    @Test
    fun decoderStorageFollowsTheRequestedTag() {
        YCbCrStorage.override = true
        val bgra = makeSession(kCVPixelFormatType_32BGRA)
        val bgraPool = PipelineThread.runSync { bgra.outputPixelBufferPool(1920, 1080) }
        assertEquals(PixelBufferLayout.rgba8, bgraPool.layout)
        assertEquals(kCVPixelFormatType_32BGRA, bgraPool.pixelFormatType)
        bgra.invalidateSession()
        YCbCrStorage.override = false
        val off = makeSession(globalPixelFormatType)
        val offPool = PipelineThread.runSync { off.outputPixelBufferPool(1920, 1080) }
        assertEquals(PixelBufferLayout.rgba8, offPool.layout)
        assertEquals(globalPixelFormatType, offPool.pixelFormatType)
        off.invalidateSession()
    }

    @Test
    fun frameWithoutFreeOutputPixelBufferIsReportedAsDropped() {
        val session = makeSession()
        val held = PipelineThread.runSync {
            val pool = session.outputPixelBufferPool(1920, 1080)
            (0 until maximumNumberOfOutputBuffers).map { retainLease(pool.createPixelBuffer()) }
        }
        assertTrue(held.all { it != null })
        val outputs = Outputs()
        PipelineThread.runSync { session.outputFrame(66_666, 33_333, outputs.handler) }
        assertEquals(listOf(Output(noErr, VTDecodeInfoFlags._FrameDropped, null, 66_666, 33_333)), outputs.outputs)
        for (buffer in held) {
            releaseLease(buffer)
        }
        outputs.outputs.clear()
        PipelineThread.runSync { session.outputFrame(100_000, 33_333, outputs.handler) }
        assertEquals(1, outputs.outputs.size)
        assertEquals(noErr, outputs.outputs[0].status)
        assertEquals(0, outputs.outputs[0].infoFlags)
        val imageBuffer = assertNotNull(outputs.outputs[0].imageBuffer)
        assertFalse(imageBuffer.isValid)
        val state = session.outputPixelBufferPool(1920, 1080).state
        assertEquals(state.allocated, PipelineThread.runSync { state.free.size })
        session.invalidateSession()
    }

    @Test
    fun framesDroppedWhenTheInputIsFullAreEachReportedOnce() {
        val session = makeSession()
        val outputs = Outputs()
        assertEquals(noErr, session.decodeFrame(hevcSample(0, keyFrame = true), outputs.handler))
        for (frameNumber in 1..120) {
            session.decodeFrame(hevcSample(frameNumber * 33_333L, keyFrame = false), outputs.handler)
        }
        assertEquals(121, outputs.outputs.size)
        for ((frameNumber, output) in outputs.outputs.withIndex()) {
            assertEquals(Output(noErr, VTDecodeInfoFlags._FrameDropped, null, frameNumber * 33_333L, 33_333), output)
        }
        outputs.outputs.clear()
        session.decodeFrame(hevcSample(121 * 33_333L, keyFrame = false), outputs.handler)
        assertEquals(
            listOf(Output(kVTVideoDecoderBadDataErr, VTDecodeInfoFlags._FrameDropped, null, 121 * 33_333L, 33_333)),
            outputs.outputs,
        )
        outputs.outputs.clear()
        session.decodeFrame(hevcSample(122 * 33_333L, keyFrame = true), outputs.handler)
        assertTrue(outputs.outputs.isEmpty())
        session.invalidateSession()
        assertEquals(kVTInvalidSessionErr, session.decodeFrame(hevcSample(123 * 33_333L, true), outputs.handler))
        assertTrue(outputs.outputs.isEmpty())
    }

    @Test
    fun outputsTheCodecSkippedAreReportedAsDropped() {
        val handlers = DecodeOutputHandlers()
        val first = Outputs()
        val second = Outputs()
        val third = Outputs()
        val fourth = Outputs()
        handlers.add(0, 33_333, first.handler)
        handlers.add(33_333, 33_333, second.handler)
        handlers.add(66_666, 33_334, third.handler)
        handlers.add(100_000, 33_333, fourth.handler)
        val skipped = mutableListOf<Pair<Long, Long>>()
        val (duration, handler) = handlers.take(66_666) { presentationTimeStamp, length, outputHandler ->
            skipped.add(Pair(presentationTimeStamp, length))
            outputHandler(noErr, VTDecodeInfoFlags._FrameDropped, null, presentationTimeStamp, length)
        }
        assertEquals(listOf(Pair(0L, 33_333L), Pair(33_333L, 33_333L)), skipped)
        assertEquals(33_334, duration)
        assertSame(third.handler, handler)
        assertEquals(1, first.outputs.size)
        assertEquals(1, second.outputs.size)
        val (unknownDuration, unknownHandler) = handlers.take(80_000) { presentationTimeStamp, length, _ ->
            skipped.add(Pair(presentationTimeStamp, length))
        }
        assertEquals(-1, unknownDuration)
        assertSame(fourth.handler, unknownHandler)
        assertEquals(2, skipped.size)
        val drained = mutableListOf<Long>()
        handlers.drain { presentationTimeStamp, _, _ -> drained.add(presentationTimeStamp) }
        assertEquals(listOf(100_000L), drained)
        handlers.clear()
        val (_, clearedHandler) = handlers.take(133_333) { _, _, _ -> }
        assertNull(clearedHandler)
    }
}

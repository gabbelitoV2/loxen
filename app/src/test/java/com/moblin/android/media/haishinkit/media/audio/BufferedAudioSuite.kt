package com.moblin.android.media.haishinkit.media.audio

import android.media.MediaFormat
import com.moblin.android.media.MediaSample
import com.moblin.android.media.haishinkit.media.DriftTracker
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertSame
import org.junit.Test

class BufferedAudioSuite {
    @Test
    fun processNormal() {
        val bufferedAudio = createBufferedAudio()
        val sampleBuffer1 = createSampleBuffer(presentationTimeStamp = 1.000)
        val sampleBuffer2 = createSampleBuffer(presentationTimeStamp = 1.021)
        val sampleBuffer3 = createSampleBuffer(presentationTimeStamp = 1.042)
        bufferedAudio.appendSampleBuffer(sampleBuffer1)
        bufferedAudio.appendSampleBuffer(sampleBuffer2)
        bufferedAudio.appendSampleBuffer(sampleBuffer3)
        assertEquals(3, bufferedAudio.numberOfBuffers())
        assertSame(sampleBuffer1, bufferedAudio.getSampleBuffer(1.000))
        assertSame(sampleBuffer2, bufferedAudio.getSampleBuffer(1.021))
        assertSame(sampleBuffer3, bufferedAudio.getSampleBuffer(1.042))
        assertEquals(0, bufferedAudio.numberOfBuffers())
        assertSame(sampleBuffer3, bufferedAudio.getSampleBuffer(1.063))
        assertEquals(0, bufferedAudio.numberOfBuffers())
    }

    @Test
    fun processGap() {
        val bufferedAudio = createBufferedAudio()
        val sampleBuffers = listOf(
            createSampleBuffer(presentationTimeStamp = 1.000),
            createSampleBuffer(presentationTimeStamp = 1.021),
            createSampleBuffer(presentationTimeStamp = 1.210),
            createSampleBuffer(presentationTimeStamp = 1.231),
            createSampleBuffer(presentationTimeStamp = 1.252),
            createSampleBuffer(presentationTimeStamp = 1.273),
            createSampleBuffer(presentationTimeStamp = 1.294),
        )
        appendSampleBuffers(bufferedAudio, sampleBuffers)
        assertEquals(7, bufferedAudio.numberOfBuffers())
        var timestamp = 1.000
        timestamp = expectSequence(bufferedAudio, sampleBuffers, timestamp, 0 until 2)
        repeat(10) {
            assertSame(sampleBuffers[2], bufferedAudio.getSampleBuffer(timestamp))
            timestamp += 0.021
        }
        timestamp = expectSequence(bufferedAudio, sampleBuffers, timestamp, 3 until 7)
        assertEquals(0, bufferedAudio.numberOfBuffers())
    }

    @Test
    fun processDriftChange() {
        val driftTracker = DriftTracker(name = "")
        val bufferedAudio = createBufferedAudio(driftTracker = driftTracker)
        val sampleBuffers = listOf(
            createSampleBuffer(presentationTimeStamp = 1.000),
            createSampleBuffer(presentationTimeStamp = 1.021),
            createSampleBuffer(presentationTimeStamp = 1.042),
            createSampleBuffer(presentationTimeStamp = 1.063),
            createSampleBuffer(presentationTimeStamp = 1.084),
            createSampleBuffer(presentationTimeStamp = 1.105),
            createSampleBuffer(presentationTimeStamp = 1.126),
            createSampleBuffer(presentationTimeStamp = 1.147),
            createSampleBuffer(presentationTimeStamp = 1.168),
            createSampleBuffer(presentationTimeStamp = 1.189),
            createSampleBuffer(presentationTimeStamp = 1.210),
            createSampleBuffer(presentationTimeStamp = 1.231),
            createSampleBuffer(presentationTimeStamp = 1.252),
            createSampleBuffer(presentationTimeStamp = 1.273),
            createSampleBuffer(presentationTimeStamp = 1.294),
        )
        appendSampleBuffers(bufferedAudio, sampleBuffers)
        assertEquals(15, bufferedAudio.numberOfBuffers())
        var timestamp = 1.000
        timestamp = expectSequence(bufferedAudio, sampleBuffers, timestamp, 0 until 2)
        driftTracker.setDrift(drift = 0.1)
        assertSame(sampleBuffers[2], bufferedAudio.getSampleBuffer(timestamp))
        timestamp += 0.021
        assertSame(sampleBuffers[2], bufferedAudio.getSampleBuffer(timestamp))
        timestamp += 0.021
        assertSame(sampleBuffers[2], bufferedAudio.getSampleBuffer(timestamp))
        timestamp += 0.021
        assertSame(sampleBuffers[2], bufferedAudio.getSampleBuffer(timestamp))
        timestamp += 0.021
        assertSame(sampleBuffers[2], bufferedAudio.getSampleBuffer(timestamp))
        timestamp += 0.021
        assertSame(sampleBuffers[2], bufferedAudio.getSampleBuffer(timestamp))
        timestamp += 0.021
        assertSame(sampleBuffers[3], bufferedAudio.getSampleBuffer(timestamp))
        timestamp += 0.021
        assertSame(sampleBuffers[4], bufferedAudio.getSampleBuffer(timestamp))
        timestamp += 0.021
        assertSame(sampleBuffers[5], bufferedAudio.getSampleBuffer(timestamp))
        timestamp += 0.021
        assertSame(sampleBuffers[6], bufferedAudio.getSampleBuffer(timestamp))
        timestamp += 0.021
        driftTracker.setDrift(drift = 0.0)
        assertSame(sampleBuffers[7], bufferedAudio.getSampleBuffer(timestamp))
        timestamp += 0.021
        assertSame(sampleBuffers[12], bufferedAudio.getSampleBuffer(timestamp))
        timestamp += 0.021
        assertSame(sampleBuffers[13], bufferedAudio.getSampleBuffer(timestamp))
        timestamp += 0.021
        assertSame(sampleBuffers[14], bufferedAudio.getSampleBuffer(timestamp))
        timestamp += 0.021
        assertSame(sampleBuffers[14], bufferedAudio.getSampleBuffer(timestamp))
        timestamp += 0.021
        assertSame(sampleBuffers[14], bufferedAudio.getSampleBuffer(timestamp))
        timestamp += 0.021
        assertEquals(0, bufferedAudio.numberOfBuffers())
    }

    @Test
    fun oa6BadAudioTimestamps() {
        val bufferedAudio = createBufferedAudio()
        val sampleBuffers = createOa6SampleBuffers()
        appendSampleBuffers(bufferedAudio, sampleBuffers)
        assertEquals(26, bufferedAudio.numberOfBuffers())
        var timestamp = 565.064
        timestamp = expectSequence(bufferedAudio, sampleBuffers, timestamp, 0 until 26)
        assertEquals(0, bufferedAudio.numberOfBuffers())
    }

    @Test
    fun oa6BadAudioTimestampsOffsetOutputTime() {
        val bufferedAudio = createBufferedAudio()
        val sampleBuffers = createOa6SampleBuffers()
        appendSampleBuffers(bufferedAudio, sampleBuffers)
        assertEquals(26, bufferedAudio.numberOfBuffers())
        var timestamp = 565.064 - 0.01
        assertSame(sampleBuffers[0], bufferedAudio.getSampleBuffer(timestamp))
        timestamp += 0.021
        timestamp = expectSequence(bufferedAudio, sampleBuffers, timestamp, 0 until 26)
        assertEquals(0, bufferedAudio.numberOfBuffers())
    }

    @Test
    fun oa6BadAudioTimestampsOffsetOutputTime2() {
        val bufferedAudio = createBufferedAudio()
        val sampleBuffers = createOa6SampleBuffers()
        appendSampleBuffers(bufferedAudio, sampleBuffers)
        assertEquals(26, bufferedAudio.numberOfBuffers())
        var timestamp = 565.064 + 0.01
        timestamp = expectSequence(bufferedAudio, sampleBuffers, timestamp, 0 until 26)
        assertEquals(0, bufferedAudio.numberOfBuffers())
    }

    @Test
    fun oa6BadAudioTimestampsBigOffsetOutputTime() {
        val bufferedAudio = createBufferedAudio()
        val sampleBuffers = createOa6SampleBuffers()
        appendSampleBuffers(bufferedAudio, sampleBuffers)
        assertEquals(26, bufferedAudio.numberOfBuffers())
        var timestamp = 565.064 - 0.2
        repeat(10) {
            assertSame(sampleBuffers[0], bufferedAudio.getSampleBuffer(timestamp))
            timestamp += 0.021
        }
        timestamp = expectSequence(bufferedAudio, sampleBuffers, timestamp, 0 until 26)
        assertEquals(0, bufferedAudio.numberOfBuffers())
    }
}

private fun createBufferedAudio(driftTracker: DriftTracker = DriftTracker(name = "")): BufferedAudio {
    return BufferedAudio(
        cameraId = UUID.randomUUID(),
        name = "",
        latency = 0.1,
        manualOutput = true,
        driftTracker = driftTracker,
    )
}

private fun createSampleBuffer(presentationTimeStamp: Double): MediaSample {
    val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_RAW, 48000, 1)
    return MediaSample(
        data = ByteArray(1024 * 2),
        presentationTimeUs = (presentationTimeStamp * 1_000_000.0).toLong(),
        isKeyFrame = true,
        format = format,
    )
}

private fun createOa6SampleBuffers(): List<MediaSample> {
    return listOf(
        createSampleBuffer(presentationTimeStamp = 565.064),
        createSampleBuffer(presentationTimeStamp = 565.097),
        createSampleBuffer(presentationTimeStamp = 565.097),
        createSampleBuffer(presentationTimeStamp = 565.131),
        createSampleBuffer(presentationTimeStamp = 565.164),
        createSampleBuffer(presentationTimeStamp = 565.164),
        createSampleBuffer(presentationTimeStamp = 565.197),
        createSampleBuffer(presentationTimeStamp = 565.197),
        createSampleBuffer(presentationTimeStamp = 565.231),
        createSampleBuffer(presentationTimeStamp = 565.231),
        createSampleBuffer(presentationTimeStamp = 565.264),
        createSampleBuffer(presentationTimeStamp = 565.298),
        createSampleBuffer(presentationTimeStamp = 565.331),
        createSampleBuffer(presentationTimeStamp = 565.331),
        createSampleBuffer(presentationTimeStamp = 565.364),
        createSampleBuffer(presentationTimeStamp = 565.398),
        createSampleBuffer(presentationTimeStamp = 565.398),
        createSampleBuffer(presentationTimeStamp = 565.431),
        createSampleBuffer(presentationTimeStamp = 565.464),
        createSampleBuffer(presentationTimeStamp = 565.464),
        createSampleBuffer(presentationTimeStamp = 565.498),
        createSampleBuffer(presentationTimeStamp = 565.531),
        createSampleBuffer(presentationTimeStamp = 565.531),
        createSampleBuffer(presentationTimeStamp = 565.531),
        createSampleBuffer(presentationTimeStamp = 565.565),
        createSampleBuffer(presentationTimeStamp = 565.598),
    )
}

private fun appendSampleBuffers(
    bufferedAudio: BufferedAudio,
    sampleBuffers: List<MediaSample>,
) {
    for (sampleBuffer in sampleBuffers) {
        bufferedAudio.appendSampleBuffer(sampleBuffer)
    }
}

private fun expectSequence(
    bufferedAudio: BufferedAudio,
    sampleBuffers: List<MediaSample>,
    timestamp: Double,
    range: IntRange,
): Double {
    var currentTimestamp = timestamp
    for (i in range) {
        assertSame(
            sampleBuffers[i],
            bufferedAudio.getSampleBuffer(currentTimestamp),
            "Failed at index $i, timestamp $currentTimestamp.",
        )
        currentTimestamp += 0.021
    }
    return currentTimestamp
}

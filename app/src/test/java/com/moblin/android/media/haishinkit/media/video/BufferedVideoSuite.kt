package com.moblin.android.media.haishinkit.media.video

import com.moblin.android.media.MediaSample
import com.moblin.android.media.haishinkit.media.DriftTracker
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.Test

class BufferedVideoSuite {
    @Test
    fun appendOutOfOrderEarlyFrame() {
        val bufferedVideo = createBufferedVideo()
        val sampleBuffer1 = createVideoSampleBuffer(2.0)
        val sampleBuffer2 = createVideoSampleBuffer(3.0)
        val sampleBuffer3 = createVideoSampleBuffer(1.0)
        bufferedVideo.appendSampleBuffer(sampleBuffer1)
        bufferedVideo.appendSampleBuffer(sampleBuffer2)
        bufferedVideo.appendSampleBuffer(sampleBuffer3)
        assertEquals(3, bufferedVideo.numberOfBuffers())
        bufferedVideo.updateSampleBuffer(1.5)
        assertEquals(2, bufferedVideo.numberOfBuffers())
    }

    @Test
    fun appendFramesInReverseOrder() {
        val bufferedVideo = createBufferedVideo()
        val sampleBuffer1 = createVideoSampleBuffer(3.0)
        val sampleBuffer2 = createVideoSampleBuffer(2.0)
        val sampleBuffer3 = createVideoSampleBuffer(1.0)
        bufferedVideo.appendSampleBuffer(sampleBuffer1)
        bufferedVideo.appendSampleBuffer(sampleBuffer2)
        bufferedVideo.appendSampleBuffer(sampleBuffer3)
        assertEquals(3, bufferedVideo.numberOfBuffers())
        bufferedVideo.updateSampleBuffer(2.5)
        assertEquals(1, bufferedVideo.numberOfBuffers())
    }
}

private fun createBufferedVideo(): BufferedVideo {
    return BufferedVideo(
        cameraId = UUID.randomUUID(),
        name = "",
        update = true,
        latency = 0.1,
        processor = null,
        driftTracker = DriftTracker(name = "")
    )
}

private fun createVideoSampleBuffer(presentationTimeStamp: Double): MediaSample {
    return MediaSample(
        data = ByteArray(4),
        presentationTimeUs = (presentationTimeStamp * 1_000_000.0).toLong(),
        isKeyFrame = false,
        format = null
    )
}

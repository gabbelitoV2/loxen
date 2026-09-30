package com.moblin.android.platform.avfoundation

import android.media.MediaFormat
import com.moblin.android.media.MediaSample
import com.moblin.android.media.haishinkit.media.Recorder
import com.moblin.android.media.haishinkit.media.RecorderDataSegment
import com.moblin.android.media.haishinkit.media.RecorderDelegate
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.kCMTimeInvalidUs
import com.moblin.android.platform.audio.makePcmFormat
import com.moblin.android.platform.video.CMSampleBufferCreateForImageBuffer
import com.moblin.android.platform.video.CMSampleTimingInfo
import com.moblin.android.platform.video.CMVideoFormatDescriptionCreateForImageBuffer
import com.moblin.android.platform.video.CVPixelBufferCreate
import com.moblin.android.platform.video.kCVPixelBufferIOSurfacePropertiesKey
import com.moblin.android.platform.video.kCVPixelFormatType_32BGRA
import com.moblin.android.runMainTest
import java.io.File
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val sampleRate = 48000
private const val framesPerBuffer = 1024
private const val frameDurationUs = 1_000_000L / 30
private const val recordingSeconds = 5.0
private const val startUs = 1_000_000_000L

@RunWith(RobolectricTestRunner::class)
class AVAssetWriterLastSegmentSuite : RecorderDelegate {
    private val finished = CountDownLatch(1)
    private val url = File(System.getProperty("java.io.tmpdir"), "last-segment-${UUID.randomUUID()}.mp4").absolutePath

    @After
    fun tearDown() {
        File(url).delete()
    }

    override fun recorderInitSegment(data: ByteArray) {}

    override fun recorderDataSegment(segment: RecorderDataSegment) {}

    override fun recorderFinished() {
        finished.countDown()
    }

    @Test
    fun theSegmentAfterTheLastFullSegmentIsInTheFile() = runMainTest {
        val recorder = Recorder()
        recorder.delegate = this@AVAssetWriterLastSegmentSuite
        recorder.startRunning(
            url = url,
            replay = false,
            audioOutputSettings = mapOf<String, Any>(
                AVFormatIDKey to MediaFormat.MIMETYPE_AUDIO_AAC,
                AVSampleRateKey to sampleRate,
                AVNumberOfChannelsKey to 0,
            ),
            videoOutputSettings = mapOf<String, Any>(
                AVVideoCodecKey to AVVideoCodecType.h264,
                AVVideoWidthKey to 0,
                AVVideoHeightKey to 0,
            ),
        )
        runBlocking { withContext(processorPipelineQueue.coroutineContext) {} }
        var audioFrame = 0L
        var videoUs = 0L
        while (videoUs < recordingSeconds * 1_000_000) {
            if (audioFrame * 1_000_000 / sampleRate <= videoUs) {
                recorder.appendAudio(silence(), startUs + audioFrame * 1_000_000 / sampleRate)
                audioFrame += framesPerBuffer
            } else {
                recorder.appendVideo(frame(startUs + videoUs))
                videoUs += frameDurationUs
                Thread.sleep(15)
            }
        }
        recorder.stopRunning()
        recorder.setUrl(null)
        assertTrue(finished.await(10, TimeUnit.SECONDS))
        assertTrue(waitForStableFile())
        val track = AVURLAsset(url = url).loadTracks(withMediaType = AVMediaType.video).first()
        val seconds = track.timeRange.duration / 1_000_000.0
        assertTrue(seconds > recordingSeconds - 0.5, "Recorded $seconds s of $recordingSeconds s")
    }

    private fun waitForStableFile(): Boolean {
        var previous = -1L
        repeat(100) {
            Thread.sleep(50)
            val length = File(url).length()
            if (length > 0 && length == previous) {
                return true
            }
            previous = length
        }
        return false
    }
}

private fun silence(): MediaSample {
    val buffer = AVAudioPCMBuffer(pcmFormat = makePcmFormat(sampleRate = sampleRate, channels = 1),
                                  frameCapacity = framesPerBuffer)!!
    buffer.frameLength = framesPerBuffer
    return buffer.replacePresentationTimeStamp(0)
}

private fun frame(presentationTimeStampUs: Long): MediaSample {
    val pixelBuffer = CVPixelBufferCreate(
        null,
        320,
        180,
        kCVPixelFormatType_32BGRA,
        mapOf<String, Any>(kCVPixelBufferIOSurfacePropertiesKey to emptyMap<String, Any>()),
    )!!
    return CMSampleBufferCreateForImageBuffer(
        null,
        pixelBuffer,
        true,
        null,
        null,
        CMVideoFormatDescriptionCreateForImageBuffer(pixelBuffer),
        CMSampleTimingInfo(
            duration = frameDurationUs,
            presentationTimeStamp = presentationTimeStampUs,
            decodeTimeStamp = kCMTimeInvalidUs,
        ),
    )!!
}

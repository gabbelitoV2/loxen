package com.moblin.android.media.haishinkit.media

import android.media.MediaFormat
import com.moblin.android.isEqual
import com.moblin.android.media.MediaSample
import com.moblin.android.media.kCMTimeInvalidUs
import com.moblin.android.platform.audio.makePcmFormat
import com.moblin.android.platform.avfoundation.AVAssetReader
import com.moblin.android.platform.avfoundation.AVAssetReaderTrackOutput
import com.moblin.android.platform.avfoundation.AVAudioPCMBuffer
import com.moblin.android.platform.avfoundation.AVFormatIDKey
import com.moblin.android.platform.avfoundation.AVLinearPCMBitDepthKey
import com.moblin.android.platform.avfoundation.AVLinearPCMIsBigEndianKey
import com.moblin.android.platform.avfoundation.AVLinearPCMIsFloatKey
import com.moblin.android.platform.avfoundation.AVLinearPCMIsNonInterleaved
import com.moblin.android.platform.avfoundation.AVMediaType
import com.moblin.android.platform.avfoundation.AVNumberOfChannelsKey
import com.moblin.android.platform.avfoundation.AVSampleRateKey
import com.moblin.android.platform.avfoundation.AVURLAsset
import com.moblin.android.platform.avfoundation.AVVideoCodecKey
import com.moblin.android.platform.avfoundation.AVVideoCodecType
import com.moblin.android.platform.avfoundation.AVVideoHeightKey
import com.moblin.android.platform.avfoundation.AVVideoWidthKey
import com.moblin.android.platform.avfoundation.frameLength
import com.moblin.android.platform.avfoundation.int16ChannelData
import com.moblin.android.platform.avfoundation.kAudioFormatLinearPCM
import com.moblin.android.platform.video.CMSampleBufferCreateForImageBuffer
import com.moblin.android.platform.video.CMSampleTimingInfo
import com.moblin.android.platform.video.CMVideoFormatDescriptionCreateForImageBuffer
import com.moblin.android.platform.video.CVPixelBufferCreate
import com.moblin.android.platform.video.kCVPixelBufferIOSurfacePropertiesKey
import com.moblin.android.platform.video.kCVPixelFormatType_32BGRA
import com.moblin.android.platform.video.kCVPixelFormatType_420YpCbCr8BiPlanarFullRange
import com.moblin.android.platform.video.kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange
import com.moblin.android.platform.videotoolbox.CMFormatDescriptionGetExtension
import com.moblin.android.platform.videotoolbox.kCMFormatDescriptionExtension_FullRangeVideo
import com.moblin.android.runMainTest
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private val audioSampleRate = 48000.0
private val audioFramesPerBuffer = 1024
private val videoFrameDuration = 1_000_000L / 30
private val toneAmplitude = 0.3
private val recordingLength = 3.0
private val startTime = 1000.0

private val recorderSuiteTempFiles = mutableListOf<String>()

private fun cmTime(seconds: Double): Long = (seconds * 1_000_000.0).roundToLong()

private class RecorderTester(
    private val toneTime: Double,
    private val pixelFormat: Int = kCVPixelFormatType_32BGRA,
) : RecorderDelegate {
    private val finished = CountDownLatch(1)
    val url: String = File(
        System.getProperty("java.io.tmpdir"),
        "recorder-suite-${UUID.randomUUID()}.mp4",
    ).absolutePath

    init {
        recorderSuiteTempFiles.add(url)
    }

    fun record(audioDelay: (Double) -> Double): Boolean {
        val recorder = Recorder()
        recorder.delegate = this
        recorder.startRunning(
            url = url,
            replay = false,
            audioOutputSettings = mapOf<String, Any>(
                AVFormatIDKey to MediaFormat.MIMETYPE_AUDIO_AAC,
                AVSampleRateKey to 48000,
                AVNumberOfChannelsKey to 0,
            ),
            videoOutputSettings = mapOf<String, Any>(
                AVVideoCodecKey to AVVideoCodecType.h264,
                AVVideoWidthKey to 0,
                AVVideoHeightKey to 0,
            ),
        )
        runBlocking { withContext(processorPipelineQueue.coroutineContext) {} }
        appendSampleBuffers(recorder, audioDelay)
        recorder.stopRunning()
        if (!finished.await(10, TimeUnit.SECONDS)) {
            return false
        }
        return waitForFileWritten()
    }

    override fun recorderInitSegment(data: ByteArray) {}

    override fun recorderDataSegment(segment: RecorderDataSegment) {}

    override fun recorderFinished() {
        finished.countDown()
    }

    private fun appendSampleBuffers(recorder: Recorder, audioDelay: (Double) -> Double) {
        var audioTime = 0.0
        var videoTime = 0.0
        var audioFrame = 0
        while (videoTime < recordingLength) {
            if (audioTime <= videoTime) {
                recorder.appendAudio(
                    createAudioSampleBuffer(audioFrame, toneTime),
                    cmTime(startTime + audioTime + audioDelay(audioTime)),
                )
                audioFrame += audioFramesPerBuffer
                audioTime = audioFrame.toDouble() / audioSampleRate
            } else {
                recorder.appendVideo(
                    createVideoSampleBuffer(
                        cmTime(startTime + videoTime),
                        pixelFormat,
                    ),
                )
                videoTime += videoFrameDuration / 1_000_000.0
                Thread.sleep(15)
            }
        }
    }

    private fun waitForFileWritten(): Boolean {
        var latestFileSize = -1L
        for (i in 0 until 100) {
            Thread.sleep(50)
            val file = File(url)
            if (!file.exists()) {
                continue
            }
            val fileSize = file.length()
            if (fileSize > 0 && fileSize == latestFileSize) {
                return true
            }
            latestFileSize = fileSize
        }
        return false
    }
}

private fun createAudioSampleBuffer(firstFrame: Int, toneTime: Double): MediaSample {
    val format = makePcmFormat(sampleRate = audioSampleRate.toInt(), channels = 1)
    val buffer = AVAudioPCMBuffer(pcmFormat = format, frameCapacity = audioFramesPerBuffer)!!
    buffer.frameLength = audioFramesPerBuffer
    val samples = buffer.int16ChannelData[0]
    for (index in 0 until audioFramesPerBuffer) {
        val frame = firstFrame + index
        if (frame.toDouble() / audioSampleRate >= toneTime) {
            samples.put(
                index,
                (toneAmplitude * sin(2 * PI * 1000 * frame.toDouble() / audioSampleRate) * 32767).toInt()
                    .toShort(),
            )
        } else {
            samples.put(index, 0)
        }
    }
    return buffer.replacePresentationTimeStamp(cmTime(0.0))
}

private fun createVideoSampleBuffer(presentationTimeStamp: Long, pixelFormat: Int): MediaSample {
    val pixelBuffer = CVPixelBufferCreate(
        null,
        320,
        180,
        pixelFormat,
        mapOf<String, Any>(kCVPixelBufferIOSurfacePropertiesKey to emptyMap<String, Any>()),
    )!!
    val formatDescription = CMVideoFormatDescriptionCreateForImageBuffer(pixelBuffer)
    val timingInfo = CMSampleTimingInfo(
        duration = videoFrameDuration,
        presentationTimeStamp = presentationTimeStamp,
        decodeTimeStamp = kCMTimeInvalidUs,
    )
    return CMSampleBufferCreateForImageBuffer(
        null,
        pixelBuffer,
        true,
        null,
        null,
        formatDescription,
        timingInfo,
    )!!
}

private suspend fun loadTrackDuration(url: String, mediaType: AVMediaType): Double? {
    val asset = AVURLAsset(url = url)
    val track = asset.loadTracks(withMediaType = mediaType).firstOrNull() ?: return null
    return track.timeRange.duration / 1_000_000.0
}

private suspend fun loadToneTime(url: String): Double? {
    val asset = AVURLAsset(url = url)
    val track = asset.loadTracks(withMediaType = AVMediaType.audio).firstOrNull() ?: return null
    val reader = AVAssetReader(asset)
    val output = AVAssetReaderTrackOutput(
        track,
        mapOf<String, Any>(
            AVFormatIDKey to kAudioFormatLinearPCM,
            AVLinearPCMBitDepthKey to 16,
            AVLinearPCMIsFloatKey to false,
            AVLinearPCMIsBigEndianKey to false,
            AVLinearPCMIsNonInterleaved to false,
            AVSampleRateKey to audioSampleRate,
            AVNumberOfChannelsKey to 1,
        ),
    )
    reader.add(output = output)
    reader.startReading()
    while (true) {
        val sampleBuffer = output.copyNextSampleBuffer() ?: break
        var toneFrame: Int? = null
        val samples = ByteBuffer.wrap(sampleBuffer.data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        for (index in 0 until sampleBuffer.numSamples) {
            if (abs(samples.get(index).toInt()) > 3000) {
                toneFrame = index
                break
            }
        }
        val foundToneFrame = toneFrame
        if (foundToneFrame != null) {
            return sampleBuffer.presentationTimeUs / 1_000_000.0 + foundToneFrame.toDouble() / audioSampleRate
        }
    }
    return null
}

private suspend fun loadVideoFullRange(url: String): Boolean? {
    val asset = AVURLAsset(url = url)
    val track = asset.loadTracks(withMediaType = AVMediaType.video).firstOrNull() ?: return null
    val formatDescription = track.formatDescriptions.firstOrNull() ?: return null
    return (CMFormatDescriptionGetExtension(
        formatDescription,
        kCMFormatDescriptionExtension_FullRangeVideo,
    ) as? Boolean) ?: false
}

@RunWith(RobolectricTestRunner::class)
class RecorderSuite {
    @After
    fun tearDown() {
        for (path in recorderSuiteTempFiles) {
            File(path).delete()
        }
        recorderSuiteTempFiles.clear()
    }

    @Test
    fun recordsAudioAndVideo() = runMainTest {
        val tester = RecorderTester(toneTime = 1.0)
        assertTrue(tester.record { 0.0 })
        val videoDuration = loadTrackDuration(tester.url, AVMediaType.video)
        val audioDuration = loadTrackDuration(tester.url, AVMediaType.audio)
        assertTrue((videoDuration ?: 0.0) > 1.5)
        assertTrue((audioDuration ?: 0.0) > 1.5)
        val toneTime = assertNotNull(loadToneTime(tester.url))
        assertTrue(isEqual(toneTime, 1.0, 0.06), "Tone at $toneTime")
    }

    @Test
    fun positiveAudioDelayDelaysAudio() = runMainTest {
        val tester = RecorderTester(toneTime = 1.0)
        assertTrue(tester.record { 0.5 })
        val toneTime = assertNotNull(loadToneTime(tester.url))
        assertTrue(isEqual(toneTime, 1.5, 0.06), "Tone at $toneTime")
    }

    @Test
    fun audioGoingBackInTimeIsDropped() = runMainTest {
        val tester = RecorderTester(toneTime = 2.0)
        assertTrue(tester.record { if (it < 1.0) 0.0 else -0.5 })
        val toneTime = assertNotNull(loadToneTime(tester.url))
        assertTrue(isEqual(toneTime, 1.5, 0.06), "Tone at $toneTime")
    }

    @Test
    fun negativeAudioDelayAdvancesAudio() = runMainTest {
        val tester = RecorderTester(toneTime = 1.0)
        assertTrue(tester.record { -0.5 })
        val toneTime = assertNotNull(loadToneTime(tester.url))
        assertTrue(isEqual(toneTime, 0.5, 0.06), "Tone at $toneTime")
    }

    @Test
    fun recordsColorRange() = runMainTest {
        for (pixelFormat in listOf(
            kCVPixelFormatType_420YpCbCr8BiPlanarFullRange,
            kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange,
        )) {
            val tester = RecorderTester(toneTime = 1.0, pixelFormat = pixelFormat)
            assertTrue(tester.record { 0.0 })
            val fullRange = loadVideoFullRange(tester.url)
            assertEquals(pixelFormat == kCVPixelFormatType_420YpCbCr8BiPlanarFullRange, fullRange)
        }
    }
}

package com.moblin.android.media.haishinkit.media

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import com.moblin.android.isEqual
import com.moblin.android.media.MediaSample
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val audioSampleRate = 48000.0
private const val audioFramesPerBuffer = 1024
private val videoFrameDuration = 1_000_000L / 30
private const val toneAmplitude = 0.3
private const val recordingLength = 3.0
private const val startTime = 1000.0

private class RecorderTester(private val toneTime: Double) : RecorderDelegate, AutoCloseable {
    private val finished = CountDownLatch(1)
    val url: String = File(
        System.getProperty("java.io.tmpdir"),
        "recorder-suite-${UUID.randomUUID()}.mp4",
    ).absolutePath

    override fun close() {
        File(url).delete()
    }

    fun record(audioDelay: (Double) -> Double): Boolean {
        val recorder = Recorder()
        recorder.delegate = this
        recorder.startRunning(
            url = url,
            replay = false,
            audioOutputSettings = mapOf(
                MediaFormat.KEY_MIME to MediaFormat.MIMETYPE_AUDIO_AAC,
                MediaFormat.KEY_SAMPLE_RATE to 48000,
                MediaFormat.KEY_CHANNEL_COUNT to 0,
            ),
            videoOutputSettings = mapOf(
                MediaFormat.KEY_MIME to MediaFormat.MIMETYPE_VIDEO_AVC,
                MediaFormat.KEY_WIDTH to 0,
                MediaFormat.KEY_HEIGHT to 0,
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

    override fun recorderInitSegment(data: ByteArray) {
    }

    override fun recorderDataSegment(segment: RecorderDataSegment) {
    }

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
                    ((startTime + audioTime + audioDelay(audioTime)) * 1_000_000).toLong(),
                )
                audioFrame += audioFramesPerBuffer
                audioTime = audioFrame.toDouble() / audioSampleRate
            } else {
                recorder.appendVideo(
                    createVideoSampleBuffer(((startTime + videoTime) * 1_000_000).toLong()),
                )
                videoTime += videoFrameDuration / 1_000_000.0
                Thread.sleep(15)
            }
        }
    }

    private fun waitForFileWritten(): Boolean {
        var latestFileSize = -1L
        repeat(100) {
            Thread.sleep(50)
            val fileSize = File(url).length()
            if (fileSize > 0 && fileSize == latestFileSize) {
                return true
            }
            latestFileSize = fileSize
        }
        return false
    }
}

private fun createAudioSampleBuffer(firstFrame: Int, toneTime: Double): MediaSample {
    val samples = ShortArray(audioFramesPerBuffer)
    for (index in 0 until audioFramesPerBuffer) {
        val frame = firstFrame + index
        if (frame.toDouble() / audioSampleRate >= toneTime) {
            samples[index] =
                (toneAmplitude * sin(2 * PI * 1000 * frame / audioSampleRate) * 32767).toInt().toShort()
        } else {
            samples[index] = 0
        }
    }
    val data = ByteArray(audioFramesPerBuffer * 2)
    ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(samples)
    val format = MediaFormat.createAudioFormat("audio/raw", audioSampleRate.toInt(), 1).apply {
        setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
    }
    return MediaSample(data = data, presentationTimeUs = 0L, isKeyFrame = true, format = format)
}

private fun createVideoSampleBuffer(presentationTimeUs: Long): MediaSample {
    val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 320, 180).apply {
        setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
    }
    val data = ByteArray(320 * 180 * 3 / 2)
    return MediaSample(data = data, presentationTimeUs = presentationTimeUs, isKeyFrame = true, format = format)
}

private suspend fun loadTrackDuration(url: String, mediaType: String): Double? = withContext(Dispatchers.IO) {
    val extractor = MediaExtractor()
    try {
        extractor.setDataSource(url)
        for (index in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(index)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime != null && mime.startsWith(mediaType)) {
                if (!format.containsKey(MediaFormat.KEY_DURATION)) {
                    return@withContext null
                }
                return@withContext format.getLong(MediaFormat.KEY_DURATION) / 1_000_000.0
            }
        }
        null
    } catch (exception: Exception) {
        null
    } finally {
        extractor.release()
    }
}

private suspend fun loadToneTime(url: String): Double? = withContext(Dispatchers.IO) {
    val extractor = MediaExtractor()
    try {
        extractor.setDataSource(url)
        var trackIndex = -1
        for (index in 0 until extractor.trackCount) {
            val mime = extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)
            if (mime != null && mime.startsWith("audio/")) {
                trackIndex = index
                break
            }
        }
        if (trackIndex == -1) {
            return@withContext null
        }
        extractor.selectTrack(trackIndex)
        val format = extractor.getTrackFormat(trackIndex)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: return@withContext null
        val decoder = MediaCodec.createDecoderByType(mime)
        try {
            decoder.configure(format, null, null, 0)
            decoder.start()
            val bufferInfo = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            while (!outputDone) {
                if (!inputDone) {
                    val inputIndex = decoder.dequeueInputBuffer(10_000)
                    if (inputIndex >= 0) {
                        val inputBuffer = decoder.getInputBuffer(inputIndex)
                        if (inputBuffer == null) {
                            inputDone = true
                        } else {
                            val size = extractor.readSampleData(inputBuffer, 0)
                            if (size < 0) {
                                decoder.queueInputBuffer(
                                    inputIndex,
                                    0,
                                    0,
                                    0,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                                )
                                inputDone = true
                            } else {
                                decoder.queueInputBuffer(inputIndex, 0, size, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }
                }
                val outputIndex = decoder.dequeueOutputBuffer(bufferInfo, 10_000)
                if (outputIndex >= 0) {
                    val outputBuffer = decoder.getOutputBuffer(outputIndex)
                    if (outputBuffer != null && bufferInfo.size > 0) {
                        val shortBuffer = outputBuffer.order(ByteOrder.nativeOrder()).asShortBuffer()
                        for (index in 0 until bufferInfo.size / 2) {
                            if (abs(shortBuffer.get(index).toInt()) > 3000) {
                                return@withContext bufferInfo.presentationTimeUs / 1_000_000.0 +
                                    index.toDouble() / audioSampleRate
                            }
                        }
                    }
                    decoder.releaseOutputBuffer(outputIndex, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        outputDone = true
                    }
                }
            }
        } finally {
            runCatching { decoder.stop() }
            decoder.release()
        }
    } finally {
        extractor.release()
    }
    null
}

@RunWith(RobolectricTestRunner::class)
class RecorderSuite {
    @Test
    fun recordsAudioAndVideo() {
        runBlocking {
            val tester = RecorderTester(toneTime = 1.0)
            assertTrue(tester.record(audioDelay = { _ -> 0.0 }))
            val videoDuration = loadTrackDuration(tester.url, "video/")
            val audioDuration = loadTrackDuration(tester.url, "audio/")
            assertTrue((videoDuration ?: 0.0) > 1.5)
            assertTrue((audioDuration ?: 0.0) > 1.5)
            val toneTime = requireNotNull(loadToneTime(tester.url))
            assertTrue(isEqual(toneTime, 1.0, epsilon = 0.06), "Tone at $toneTime")
        }
    }

    @Test
    fun positiveAudioDelayDelaysAudio() {
        runBlocking {
            val tester = RecorderTester(toneTime = 1.0)
            assertTrue(tester.record(audioDelay = { _ -> 0.5 }))
            val toneTime = requireNotNull(loadToneTime(tester.url))
            assertTrue(isEqual(toneTime, 1.5, epsilon = 0.06), "Tone at $toneTime")
        }
    }

    @Test
    fun audioGoingBackInTimeIsDropped() {
        runBlocking {
            val tester = RecorderTester(toneTime = 2.0)
            assertTrue(tester.record(audioDelay = { delay -> if (delay < 1.0) 0.0 else -0.5 }))
            val toneTime = requireNotNull(loadToneTime(tester.url))
            assertTrue(isEqual(toneTime, 1.5, epsilon = 0.06), "Tone at $toneTime")
        }
    }

    @Test
    fun negativeAudioDelayAdvancesAudio() {
        runBlocking {
            val tester = RecorderTester(toneTime = 1.0)
            assertTrue(tester.record(audioDelay = { _ -> -0.5 }))
            val toneTime = requireNotNull(loadToneTime(tester.url))
            assertTrue(isEqual(toneTime, 0.5, epsilon = 0.06), "Tone at $toneTime")
        }
    }
}

package com.moblin.android.platform.speech

import android.media.AudioFormat
import android.media.MediaFormat
import com.moblin.android.media.MediaSample
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SpeechAudioSuite {
    @Test
    fun stereo48kHzIsDownmixedAndResampledTo16kHzMono() {
        val converter = SpeechAudioConverter()
        var total = 0
        repeat(3) {
            val bytes = converter.convert(pcm16Sample(48000, 2, 1024) { _, channel -> if (channel == 0) 1000 else 3000 })!!
            val samples = shorts(bytes)
            assertTrue(samples.all { it.toInt() == 2000 }, samples.toList().toString())
            total += samples.size
        }
        assertEquals(1024, total)
    }

    @Test
    fun resamplingKeepsTheDurationOverManyBuffers() {
        for (sampleRate in listOf(44100, 48000, 32000, 24000, 22050, 16000, 8000)) {
            val converter = SpeechAudioConverter()
            var total = 0
            repeat(100) {
                total += converter.convert(pcm16Sample(sampleRate, 1, 441) { _, _ -> 0 })!!.size / 2
            }
            val expected = 100 * 441 * 16000.0 / sampleRate
            assertTrue(abs(total - expected) <= 2, "$sampleRate Hz gave $total samples, expected $expected")
        }
    }

    @Test
    fun a440HzToneKeepsItsFrequencyAndLevel() {
        val converter = SpeechAudioConverter()
        val frames = 48000
        val bytes = converter.convert(
            pcm16Sample(48000, 1, frames) { frame, _ -> (sin(2 * PI * 440 * frame / 48000.0) * 16000).toInt() },
        )!!
        val samples = shorts(bytes)
        assertEquals(16000, samples.size)
        var crossings = 0
        for (index in 1 until samples.size) {
            if (samples[index - 1] < 0 && samples[index] >= 0) {
                crossings += 1
            }
        }
        assertTrue(crossings in 438..442, "crossings $crossings")
        val peak = samples.maxOf { abs(it.toInt()) }
        assertTrue(peak in 15000..16100, "peak $peak")
    }

    @Test
    fun floatSamplesAreConvertedAndClipped() {
        val converter = SpeechAudioConverter()
        val bytes = converter.convert(pcmFloatSample(16000, 1, 4) { frame, _ -> listOf(0.5f, -0.5f, 2f, -2f)[frame] })!!
        assertContentEquals(shortArrayOf(16384, -16384, 32767, -32768), shorts(bytes))
    }

    @Test
    fun otherPcmEncodingsAreConverted() {
        val eightBit = sample(16000, 1, AudioFormat.ENCODING_PCM_8BIT, byteArrayOf(0xC0.toByte(), 0x40, 0x80.toByte()))
        assertContentEquals(shortArrayOf(16384, -16384, 0), shorts(SpeechAudioConverter().convert(eightBit)!!))
        val twentyFourBit = sample(
            16000,
            1,
            AudioFormat.ENCODING_PCM_24BIT_PACKED,
            byteArrayOf(0x00, 0x00, 0x40, 0x00, 0x00, 0xC0.toByte()),
        )
        assertContentEquals(shortArrayOf(16384, -16384), shorts(SpeechAudioConverter().convert(twentyFourBit)!!))
        val thirtyTwoBit = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putInt(1 shl 30).putInt(-(1 shl 30)).array()
        val sample32 = sample(16000, 1, AudioFormat.ENCODING_PCM_32BIT, thirtyTwoBit)
        assertContentEquals(shortArrayOf(16384, -16384), shorts(SpeechAudioConverter().convert(sample32)!!))
    }

    @Test
    fun upsamplingInterpolates() {
        val converter = SpeechAudioConverter()
        val bytes = converter.convert(pcm16Sample(8000, 1, 3) { frame, _ -> listOf(0, 1000, 2000)[frame] })!!
        assertContentEquals(shortArrayOf(0, 500, 1000, 1500), shorts(bytes))
    }

    @Test
    fun formatChangeStartsOver() {
        val converter = SpeechAudioConverter()
        converter.convert(pcm16Sample(48000, 2, 1) { _, _ -> 1000 })
        val bytes = converter.convert(pcm16Sample(16000, 1, 2) { _, _ -> 3000 })!!
        assertContentEquals(shortArrayOf(3000, 3000), shorts(bytes))
    }

    @Test
    fun compressedAndUnknownAudioIsIgnored() {
        val aac = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 48000, 2)
        val converter = SpeechAudioConverter()
        assertNull(converter.convert(MediaSample(ByteArray(100), 0, true, aac)))
        assertNull(converter.convert(MediaSample(ByteArray(100), 0, true, null)))
        assertNull(converter.convert(sample(48000, 1, AudioFormat.ENCODING_AC3, ByteArray(100))))
    }

    @Test
    fun pipeWritesAudioAppendedBeforeAndAfterAttachInOrder() {
        val pipe = SpeechAudioPipe(maximumBufferedBytes = 1000)
        val output = ByteArrayOutputStream()
        val finished = CountDownLatch(1)
        pipe.write(byteArrayOf(1, 2))
        pipe.attach(output) { finished.countDown() }
        pipe.write(byteArrayOf(3))
        pipe.write(byteArrayOf(4, 5))
        pipe.end()
        pipe.write(byteArrayOf(6))
        assertTrue(finished.await(5, TimeUnit.SECONDS))
        assertContentEquals(byteArrayOf(1, 2, 3, 4, 5), output.toByteArray())
        assertTrue(pipe.isFinished)
        assertEquals(5, pipe.writtenBytes)
    }

    @Test
    fun endBeforeAttachFlushesAndClosesOnAttach() {
        val pipe = SpeechAudioPipe(maximumBufferedBytes = 1000)
        val closed = CountDownLatch(1)
        val output = object : ByteArrayOutputStream() {
            override fun close() {
                closed.countDown()
            }
        }
        pipe.write(byteArrayOf(7, 8, 9))
        pipe.end()
        pipe.attach(output) {}
        assertTrue(closed.await(5, TimeUnit.SECONDS))
        assertContentEquals(byteArrayOf(7, 8, 9), output.toByteArray())
    }

    @Test
    fun writesNeverBlockAndDropTheOldestAudioWhileTheRecognizerIsNotReading() {
        val pipe = SpeechAudioPipe(maximumBufferedBytes = 10)
        val output = BlockingOutputStream()
        val finished = CountDownLatch(1)
        pipe.attach(output) { finished.countDown() }
        pipe.write(byteArrayOf(0))
        waitUntil { output.blockedWrites.get() == 1 }
        val start = System.nanoTime()
        for (value in 1..20) {
            pipe.write(byteArrayOf(value.toByte(), value.toByte()))
        }
        assertTrue(System.nanoTime() - start < 1_000_000_000L)
        assertEquals(30, pipe.droppedBytes)
        output.release.countDown()
        pipe.end()
        assertTrue(finished.await(5, TimeUnit.SECONDS))
        assertContentEquals(
            byteArrayOf(0, 16, 16, 17, 17, 18, 18, 19, 19, 20, 20),
            output.written.toByteArray(),
        )
    }

    @Test
    fun closeDiscardsQueuedAudioAndUnblocksTheWriter() {
        val pipe = SpeechAudioPipe(maximumBufferedBytes = 1000)
        val output = BlockingOutputStream()
        val finished = CountDownLatch(1)
        pipe.attach(output) { finished.countDown() }
        pipe.write(byteArrayOf(1))
        waitUntil { output.blockedWrites.get() == 1 }
        pipe.write(byteArrayOf(2))
        pipe.close()
        assertTrue(finished.await(5, TimeUnit.SECONDS))
        assertTrue(output.closed)
        assertContentEquals(byteArrayOf(), output.written.toByteArray())
        pipe.write(byteArrayOf(3))
        assertTrue(pipe.isEnded)
    }

    @Test
    fun aSecondOutputIsClosedInsteadOfLeaked() {
        val pipe = SpeechAudioPipe(maximumBufferedBytes = 1000)
        val first = ByteArrayOutputStream()
        pipe.attach(first) {}
        var secondClosed = false
        val second = object : ByteArrayOutputStream() {
            override fun close() {
                secondClosed = true
            }
        }
        pipe.attach(second) {}
        assertTrue(secondClosed)
        pipe.close()
    }

    @Test
    fun readerGoneStopsTheWriter() {
        val pipe = SpeechAudioPipe(maximumBufferedBytes = 1000)
        val finished = CountDownLatch(1)
        val output = object : OutputStream() {
            override fun write(b: Int) {
                throw IOException("EPIPE")
            }
        }
        pipe.attach(output) { finished.countDown() }
        pipe.write(byteArrayOf(1))
        assertTrue(finished.await(5, TimeUnit.SECONDS))
        assertTrue(pipe.isEnded)
    }

    private fun sample(sampleRate: Int, channels: Int, encoding: Int, data: ByteArray): MediaSample {
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_RAW, sampleRate, channels)
        format.setInteger(MediaFormat.KEY_PCM_ENCODING, encoding)
        return MediaSample(data = data, presentationTimeUs = 0, isKeyFrame = true, format = format)
    }

    private class BlockingOutputStream : OutputStream() {
        val release = CountDownLatch(1)
        val blockedWrites = AtomicInteger(0)
        val written = ByteArrayOutputStream()

        @Volatile
        var closed = false

        override fun write(b: Int) {
            write(byteArrayOf(b.toByte()), 0, 1)
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            if (blockedWrites.incrementAndGet() == 1) {
                while (!release.await(10, TimeUnit.MILLISECONDS)) {
                    if (closed) {
                        throw IOException("closed")
                    }
                }
            }
            synchronized(written) {
                written.write(b, off, len)
            }
        }

        override fun close() {
            closed = true
        }
    }
}

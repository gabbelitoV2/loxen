package com.moblin.android.platform.avfoundation

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class PcmConverterTest {
    private fun sine(frequency: Double, rate: Int, frames: Int, amplitude: Double = 0.5): FloatArray {
        return FloatArray(frames) { (amplitude * sin(2 * PI * frequency * it / rate)).toFloat() }
    }

    private fun convertAll(converter: PcmConverter, input: FloatArray, inputChannels: Int, blockFrames: Int): FloatArray {
        val output = ArrayList<Float>()
        var frame = 0
        val frames = input.size / inputChannels
        while (frame < frames) {
            val count = minOf(blockFrames, frames - frame)
            converter.append(input, frame * inputChannels, count)
            frame += count
            while (true) {
                val chunk = converter.takeChunk(1024) ?: break
                output.addAll(chunk.toList())
            }
        }
        converter.flush()
        while (true) {
            val chunk = converter.takeChunk(1024) ?: break
            output.addAll(chunk.toList())
        }
        converter.takeRemaining()?.let { output.addAll(it.toList()) }
        return output.toFloatArray()
    }

    @Test
    fun sameRatePassesSamplesThroughInChunks() {
        val converter = PcmConverter(48000, 2, 48000, 2)
        val input = FloatArray(2000 * 2) { it / 8000f }
        converter.append(input, 0, 2000)
        val chunk = assertNotNull(converter.takeChunk(1024))
        assertContentEquals(input.copyOf(1024 * 2), chunk)
        assertNull(converter.takeChunk(1024))
        converter.flush()
        val remaining = assertNotNull(converter.takeRemaining())
        assertContentEquals(input.copyOfRange(1024 * 2, 2000 * 2), remaining)
        assertNull(converter.takeRemaining())
    }

    @Test
    fun resampledLengthMatchesTheDuration() {
        val converter = PcmConverter(44100, 1, 48000, 1)
        val output = convertAll(converter, FloatArray(44100), 1, 997)
        assertEquals(48000, output.size)
    }

    @Test
    fun downsampledLengthMatchesTheDuration() {
        val converter = PcmConverter(48000, 1, 16000, 1)
        val output = convertAll(converter, FloatArray(48001), 1, 1024)
        assertEquals(16001, output.size)
    }

    @Test
    fun constantSignalKeepsItsLevel() {
        val converter = PcmConverter(44100, 1, 48000, 1)
        val output = convertAll(converter, FloatArray(44100) { 0.5f }, 1, 1000)
        for (index in 32 until output.size - 32) {
            assertTrue(abs(output[index] - 0.5f) < 1e-4f, "sample $index is ${output[index]}")
        }
    }

    @Test
    fun upsampledSineMatchesTheIdealSine() {
        val converter = PcmConverter(44100, 1, 48000, 1)
        val output = convertAll(converter, sine(1000.0, 44100, 44100), 1, 512)
        var maximumError = 0.0
        for (index in 64 until output.size - 64) {
            val expected = 0.5 * sin(2 * PI * 1000.0 * index / 48000)
            maximumError = max(maximumError, abs(output[index] - expected))
        }
        assertTrue(maximumError < 2e-3, "maximum error $maximumError")
    }

    @Test
    fun downsampledSineMatchesTheIdealSine() {
        val converter = PcmConverter(48000, 1, 16000, 1)
        val output = convertAll(converter, sine(1000.0, 48000, 48000), 1, 1024)
        var maximumError = 0.0
        for (index in 32 until output.size - 32) {
            val expected = 0.5 * sin(2 * PI * 1000.0 * index / 16000)
            maximumError = max(maximumError, abs(output[index] - expected))
        }
        assertTrue(maximumError < 2e-3, "maximum error $maximumError")
    }

    @Test
    fun downsamplingRemovesFrequenciesAboveTheNewNyquistFrequency() {
        val converter = PcmConverter(48000, 1, 16000, 1)
        val output = convertAll(converter, sine(12000.0, 48000, 48000), 1, 1024)
        var peak = 0f
        for (index in 64 until output.size - 64) {
            peak = max(peak, abs(output[index]))
        }
        assertTrue(peak < 0.01f, "aliased peak $peak")
    }

    @Test
    fun blockSizeDoesNotChangeTheOutput() {
        val input = sine(440.0, 44100, 20000)
        val whole = convertAll(PcmConverter(44100, 1, 48000, 1), input, 1, 20000)
        val pieces = convertAll(PcmConverter(44100, 1, 48000, 1), input, 1, 37)
        assertContentEquals(whole, pieces)
    }

    @Test
    fun stereoIsMixedDownToMonoByAveraging() {
        val mixed = mixChannels(floatArrayOf(0.2f, 0.4f, -1f, 1f), 0, 2, 2, 1)
        assertEquals(0.3f, mixed[0], 1e-6f)
        assertEquals(0f, mixed[1], 1e-6f)
    }

    @Test
    fun monoIsCopiedToBothStereoChannels() {
        val mixed = mixChannels(floatArrayOf(0.25f, -0.5f), 0, 2, 1, 2)
        assertContentEquals(floatArrayOf(0.25f, 0.25f, -0.5f, -0.5f), mixed)
    }

    @Test
    fun mixingHonoursTheInputOffset() {
        val mixed = mixChannels(floatArrayOf(9f, 9f, 0.5f, 0.5f), 2, 1, 2, 1)
        assertContentEquals(floatArrayOf(0.5f), mixed)
    }

    @Test
    fun sixteenBitSamplesRoundTrip() {
        val values = shortArrayOf(0, 1, -1, 32767, -32768, 12345, -23456)
        val samples = FloatArray(values.size) { values[it] / 32768f }
        val data = encodePcm16(samples, bigEndian = false)
        for (index in values.indices) {
            val value = ((data[2 * index + 1].toInt() shl 8) or (data[2 * index].toInt() and 0xFF)).toShort()
            assertEquals(values[index], value)
        }
    }

    @Test
    fun sixteenBitSamplesAreClipped() {
        val data = encodePcm16(floatArrayOf(1.5f, -1.5f), bigEndian = true)
        assertContentEquals(byteArrayOf(0x7F, 0xFF.toByte(), 0x80.toByte(), 0x00), data)
    }

    @Test
    fun formatChangeFlushesTheOldResampler() {
        val converter = PcmConverter(44100, 1, 48000, 1)
        converter.append(FloatArray(4410) { 0.5f }, 0, 4410)
        converter.setInputFormat(48000, 1)
        converter.append(FloatArray(4800) { 0.5f }, 0, 4800)
        converter.flush()
        var frames = 0
        while (true) {
            val chunk = converter.takeChunk(1024) ?: break
            frames += chunk.size
        }
        frames += converter.takeRemaining()?.size ?: 0
        assertEquals(4800 + 4800, frames)
    }

    @Test
    fun timeRangeEndSaturates() {
        assertEquals(Long.MAX_VALUE, CMTimeRange.all.end)
        assertEquals(7_000_000L, CMTimeRange(start = 5_000_000L, duration = 2_000_000L).end)
        assertEquals(2_000_000L, CMTimeRange(start = 5_000_000L, end = 7_000_000L).duration)
    }
}

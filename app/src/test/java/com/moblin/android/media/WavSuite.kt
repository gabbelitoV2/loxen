package com.moblin.android.media

import com.moblin.android.media.haishinkit.util.ByteWriter
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WavSuite {
    @Test
    fun mono() {
        val samples: List<List<Short>> = listOf(
            listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9).map { it.toShort() },
        )
        val wav = createWav(sampleRate = 48000, samples = samples)
        assertContentEquals(createMonoWav(samples[0]), wav)
    }

    @Test
    fun stereo() {
        val samplesRight: List<Short> = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9).map { it.toShort() }
        val samplesLeft: List<Short> = listOf(0, -1, -2, -3, -4, -5, -6, -7, -8, -9).map { it.toShort() }
        val wav = createWav(sampleRate = 48000, samples = listOf(samplesRight, samplesLeft))
        assertContentEquals(createStereoWav(samplesRight, samplesLeft), wav)
    }
}

private fun createMonoWav(samples: List<Short>): ByteArray {
    val dataSize = (samples.size * 2).toUInt()
    val writer = ByteWriter()
    writer.writeUTF8Bytes("RIFF")
    writer.writeUInt32Le((44 - 8).toUInt() + dataSize)
    writer.writeUTF8Bytes("WAVE")
    writer.writeUTF8Bytes("fmt ")
    writer.writeUInt32Le(16u)
    writer.writeUInt16Le(1.toUShort())
    writer.writeUInt16Le(1.toUShort())
    writer.writeUInt32Le(48000u)
    writer.writeUInt32Le(0x02EE00u)
    writer.writeUInt16Le(4u.toUShort())
    writer.writeUInt16Le(16u.toUShort())
    writer.writeUTF8Bytes("data")
    writer.writeUInt32Le(dataSize)
    for (sample in samples) {
        writer.writeUInt16Le(sample.toUShort())
    }
    return writer.data
}

private fun createStereoWav(samplesRight: List<Short>, samplesLeft: List<Short>): ByteArray {
    assertEquals(samplesLeft.size, samplesRight.size)
    val dataSize = (samplesRight.size * 2 * 2).toUInt()
    val writer = ByteWriter()
    writer.writeUTF8Bytes("RIFF")
    writer.writeUInt32Le((44 - 8).toUInt() + dataSize)
    writer.writeUTF8Bytes("WAVE")
    writer.writeUTF8Bytes("fmt ")
    writer.writeUInt32Le(16u)
    writer.writeUInt16Le(1.toUShort())
    writer.writeUInt16Le(2.toUShort())
    writer.writeUInt32Le(48000u)
    writer.writeUInt32Le(0x02EE00u)
    writer.writeUInt16Le(4u.toUShort())
    writer.writeUInt16Le(16u.toUShort())
    writer.writeUTF8Bytes("data")
    writer.writeUInt32Le(dataSize)
    for ((sampleRight, sampleLeft) in samplesRight.zip(samplesLeft)) {
        writer.writeUInt16Le(sampleRight.toUShort())
        writer.writeUInt16Le(sampleLeft.toUShort())
    }
    return writer.data
}

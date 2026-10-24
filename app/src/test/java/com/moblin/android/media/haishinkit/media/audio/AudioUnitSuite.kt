package com.moblin.android.media.haishinkit.media.audio

import com.moblin.android.platform.audio.makePcmFormat
import com.moblin.android.platform.avfoundation.AVAudioConverter
import com.moblin.android.platform.avfoundation.AVAudioPCMBuffer
import com.moblin.android.platform.avfoundation.frameLength
import com.moblin.android.platform.avfoundation.int16ChannelData
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AudioUnitSuite {
    @Test
    fun calcAudioLevelPeakFloat32() {
        val samples = floatArrayOf(0.1f, -0.75f, 0.5f, -0.2f)
        val peak = com.moblin.android.media.haishinkit.media.audio.calcAudioLevelPeakFloat32(
            samples,
            samples.size,
        )
        assertEquals(0.75f, peak, 0.0001f)
    }

    @Test
    fun calcAudioLevelPeakFloat32Empty() {
        val samples = floatArrayOf(1.0f)
        val peak = com.moblin.android.media.haishinkit.media.audio.calcAudioLevelPeakFloat32(samples, 0)
        assertEquals(0.0f, peak, 0.0001f)
    }

    @Test
    fun calcAudioLevelPeakInt16() {
        val samples = shortArrayOf(100, -3000, 2000, -50)
        val peak = com.moblin.android.media.haishinkit.media.audio.calcAudioLevelPeakInt16(
            samples,
            samples.size,
        )
        assertEquals(0.091552734f, peak, 0.0001f)
    }

    @Test
    fun calcAudioLevelPeakInt16Min() {
        val samples = shortArrayOf(Short.MIN_VALUE, Short.MAX_VALUE)
        val peak = com.moblin.android.media.haishinkit.media.audio.calcAudioLevelPeakInt16(
            samples,
            samples.size,
        )
        assertEquals(1.0f, peak, 0.0001f)
    }

    @Test
    fun calcAudioLevelPeakInt16Empty() {
        val samples = shortArrayOf(1000)
        val peak = com.moblin.android.media.haishinkit.media.audio.calcAudioLevelPeakInt16(samples, 0)
        assertEquals(0.0f, peak, 0.0001f)
    }

    @Test
    fun makeChannelMapMonoToStereo() {
        val channelMap = makeChannelMap(
            numberOfInputChannels = 1,
            numberOfOutputChannels = 2,
            outputToInputChannelsMap = mapOf(0 to 0, 1 to 1),
        )
        assertEquals(listOf(0, 0), channelMap)
    }

    @Test
    fun makeChannelMapStereoToMono() {
        val channelMap = makeChannelMap(
            numberOfInputChannels = 2,
            numberOfOutputChannels = 1,
            outputToInputChannelsMap = mapOf(0 to 0, 1 to 1),
        )
        assertEquals(listOf(0), channelMap)
    }

    @Test
    fun makeChannelMapStereoToStereo() {
        val channelMap = makeChannelMap(
            numberOfInputChannels = 2,
            numberOfOutputChannels = 2,
            outputToInputChannelsMap = mapOf(0 to 1, 1 to 0),
        )
        assertEquals(listOf(1, 0), channelMap)
    }

    @Test
    fun makeChannelMapFourToStereo() {
        val channelMap = makeChannelMap(
            numberOfInputChannels = 4,
            numberOfOutputChannels = 2,
            outputToInputChannelsMap = mapOf(0 to 2, 1 to 3),
        )
        assertEquals(listOf(2, 3), channelMap)
    }

    @Test
    fun convertMonoToStereo() {
        val inputFormat = makePcmFormat(sampleRate = 48000, channels = 1)
        val outputFormat = makePcmFormat(sampleRate = 48000, channels = 2)
        val converter = requireNotNull(AVAudioConverter.create(from = inputFormat, to = outputFormat))
        converter.channelMap = makeChannelMap(
            numberOfInputChannels = 1,
            numberOfOutputChannels = 2,
            outputToInputChannelsMap = mapOf(0 to 0, 1 to 1),
        )
        val inputBuffer = requireNotNull(AVAudioPCMBuffer(pcmFormat = inputFormat, frameCapacity = 4))
        inputBuffer.frameLength = 4
        for ((index, sample) in listOf<Short>(100, -200, 300, -400).withIndex()) {
            inputBuffer.int16ChannelData[0].put(index, sample)
        }
        val outputBuffer = requireNotNull(AVAudioPCMBuffer(pcmFormat = outputFormat, frameCapacity = 4))
        val error = converter.convert(to = outputBuffer, from = inputBuffer)
        assertNull(error)
        val channel = outputBuffer.int16ChannelData[0]
        val samples = ShortArray(8) { channel.get(it) }
        assertContentEquals(shortArrayOf(100, 100, -200, -200, 300, 300, -400, -400), samples)
    }
}

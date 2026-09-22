package com.moblin.android.media.haishinkit.media.audio

import kotlin.test.assertEquals
import org.junit.Test

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
}

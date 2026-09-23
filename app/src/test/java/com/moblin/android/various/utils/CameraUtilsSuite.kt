package com.moblin.android.various.utils

import org.junit.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private fun cmTime(value: Int, timescale: Int): Long = value * 1_000_000L / timescale

private val invalidExposure = Long.MIN_VALUE

@RunWith(RobolectricTestRunner::class)
class CameraUtilsSuite {
    private val exposures = listOf(1000, 500, 250, 125, 60).map { cmTime(value = 1, timescale = it) }

    @Test
    fun factorEndsAreFastestAndSlowestExposures() {
        assertEquals(cmTime(value = 1, timescale = 1000), factorToExposure(exposures = exposures, factor = 0f))
        assertEquals(cmTime(value = 1, timescale = 60), factorToExposure(exposures = exposures, factor = 1f))
        assertEquals(cmTime(value = 1, timescale = 250), factorToExposure(exposures = exposures, factor = 0.5f))
    }

    @Test
    fun factorOutsideRangeIsClamped() {
        assertEquals(cmTime(value = 1, timescale = 1000), factorToExposure(exposures = exposures, factor = -1f))
        assertEquals(cmTime(value = 1, timescale = 60), factorToExposure(exposures = exposures, factor = 2f))
    }

    @Test
    fun factorSnapsToNearestExposure() {
        assertEquals(cmTime(value = 1, timescale = 500), factorToExposure(exposures = exposures, factor = 0.3f))
        assertEquals(cmTime(value = 1, timescale = 250), factorToExposure(exposures = exposures, factor = 0.6f))
    }

    @Test
    fun everyExposureRoundTripsThroughItsFactor() {
        for (exposure in exposures) {
            val factor = factorFromExposure(exposures = exposures, exposure = exposure)
            assertEquals(exposure, factorToExposure(exposures = exposures, factor = factor))
        }
    }

    @Test
    fun exposureFromDeviceIsNearestInLogSpace() {
        val exposure = factorFromExposure(exposures = exposures, exposure = cmTime(value = 1, timescale = 130))
        assertEquals(cmTime(value = 1, timescale = 125), factorToExposure(exposures = exposures, factor = exposure))
        val slow = factorFromExposure(exposures = exposures, exposure = cmTime(value = 1, timescale = 4))
        assertEquals(cmTime(value = 1, timescale = 60), factorToExposure(exposures = exposures, factor = slow))
        val fast = factorFromExposure(exposures = exposures, exposure = cmTime(value = 1, timescale = 8000))
        assertEquals(cmTime(value = 1, timescale = 1000), factorToExposure(exposures = exposures, factor = fast))
    }

    @Test
    fun invalidExposureIsFastest() {
        assertEquals(0f, factorFromExposure(exposures = exposures, exposure = 0L))
        assertEquals(0f, factorFromExposure(exposures = exposures, exposure = invalidExposure))
    }

    @Test
    fun stepIsOnePerExposure() {
        assertEquals(0.25f, exposureFactorStep(exposures = exposures))
        assertEquals(1f, exposureFactorStep(exposures = listOf(cmTime(value = 1, timescale = 60))))
        assertEquals(1f, exposureFactorStep(exposures = emptyList<Long>()))
    }

    @Test
    fun singleExposureIsAlwaysUsed() {
        val exposures = listOf(cmTime(value = 1, timescale = 30))
        assertEquals(cmTime(value = 1, timescale = 30), factorToExposure(exposures = exposures, factor = 0f))
        assertEquals(cmTime(value = 1, timescale = 30), factorToExposure(exposures = exposures, factor = 1f))
        assertEquals(0f, factorFromExposure(exposures = exposures, exposure = cmTime(value = 1, timescale = 30)))
    }

    @Test
    fun exposureIsFormattedAsFractionOfSecond() {
        assertEquals("1/125", formatExposure(exposure = cmTime(value = 1, timescale = 125)))
        assertEquals("1/8000", formatExposure(exposure = cmTime(value = 1, timescale = 8000)))
        assertEquals("1/50", formatExposure(exposure = 20_000L))
        assertEquals("1/71", formatExposure(exposure = 1_000_000L / 71))
        assertEquals("", formatExposure(exposure = 0L))
        assertEquals("", formatExposure(exposure = invalidExposure))
    }
}

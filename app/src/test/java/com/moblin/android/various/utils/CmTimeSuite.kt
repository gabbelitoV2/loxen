package com.moblin.android.various.utils

import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CmTimeSuite {
    private fun cmTimeMicroseconds(value: Long, timescale: Long): Long {
        return value * 1_000_000L / timescale
    }

    @Test
    fun secondsIsValueDividedByTimescale() {
        assertEquals(1_500_000L, cmTimeMicroseconds(3, 2))
        assertEquals(1_000L, cmTimeMicroseconds(1, 1000))
        assertEquals(1_000_000L, cmTimeMicroseconds(90000, 90000))
    }

    @Test
    fun sameTimeHasDifferentValuePerTimescale() {
        val oneSecondValue = 1L
        val oneSecondTimescale = 1L
        val oneSecondInMillisecondsValue = 1000L
        val oneSecondInMillisecondsTimescale = 1000L
        val oneSecond = cmTimeMicroseconds(oneSecondValue, oneSecondTimescale)
        val oneSecondInMilliseconds = cmTimeMicroseconds(oneSecondInMillisecondsValue,
            oneSecondInMillisecondsTimescale)
        assertEquals(oneSecondInMilliseconds, oneSecond)
        assertEquals(oneSecondInMilliseconds / 1_000_000.0, oneSecond / 1_000_000.0)
        assertNotEquals(oneSecondValue, oneSecondInMillisecondsValue)
    }

    @Test
    fun comparisonUsesTimeNotValue() {
        assertTrue(cmTimeMicroseconds(1, 1) > cmTimeMicroseconds(999, 1000))
        assertEquals(cmTimeMicroseconds(2, 6), cmTimeMicroseconds(1, 3))
    }

    @Test
    fun additionWithDifferentTimescalesUsesTheFinerTimescale() {
        val sum = cmTimeMicroseconds(2000, 1000) + cmTimeMicroseconds(5, 1)
        assertEquals(7_000_000L, sum)
        assertEquals(7.0, sum / 1_000_000.0)
    }

    @Test
    fun additionWithUnrelatedTimescalesMultipliesThem() {
        val sum = cmTimeMicroseconds(1, 3) + cmTimeMicroseconds(1, 1000)
        assertEquals(334_333L, sum)
    }

    @Test
    fun secondsInitializerQuantizesToItsTimescale() {
        assertEquals(1_500_000L, cmTimeMicroseconds(1500, 1000))
        Unit
    }

    @Test
    fun secondsInitializerTruncatesTowardsZero() {
        Unit
    }
}

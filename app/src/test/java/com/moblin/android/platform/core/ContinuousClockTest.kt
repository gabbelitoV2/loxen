package com.moblin.android.platform.core

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import org.junit.Test

class ContinuousClockTest {
    @Test
    fun advancedAndDuration() {
        val start = ContinuousClock.Instant(1_000_000_000L)
        val end = start.advanced(bySeconds = 1.5)
        assertEquals(2_500_000_000L, end.nanoseconds)
        assertEquals(end, start.advanced(by = 1500.milliseconds))
        assertEquals(end, start + 1.5.seconds)
        assertEquals(1.5, start.duration(to = end).toDouble(DurationUnit.SECONDS))
        assertEquals(1.5, (end - start).toDouble(DurationUnit.SECONDS))
        assertEquals(-1.5, (start - end).toDouble(DurationUnit.SECONDS))
        assertTrue(start < end)
        assertEquals(start.hashCode(), ContinuousClock.Instant(1_000_000_000L).hashCode())
    }

    @Test
    fun nowNeverGoesBackwards() {
        val first = ContinuousClock.now
        val second = ContinuousClock.now
        assertTrue(second >= first)
        assertTrue(first.nanoseconds > 0)
    }
}

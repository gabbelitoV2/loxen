package com.moblin.android.integrations.workoutdevice

import com.moblin.android.platform.core.ContinuousClock
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WorkoutDeviceCrankCadenceSuite {
    @Test
    fun reportsNothingWithoutCrankData() {
        val crankCadence = WorkoutDeviceCrankCadence()
        val now = ContinuousClock.now
        assertNull(crankCadence.update(revolutions = null, time = null, now = now))
        assertNull(
            crankCadence.update(
                revolutions = null,
                time = null,
                now = now.advanced(bySeconds = 10.0),
            ),
        )
    }

    @Test
    fun firstCrankMeasurementReportsNothing() {
        val crankCadence = WorkoutDeviceCrankCadence()
        val now = ContinuousClock.now.advanced(bySeconds = 10.0)
        assertNull(crankCadence.update(revolutions = 10.toUShort(), time = 1024.toUShort(), now = now))
    }

    @Test
    fun reportsZeroWhenCrankStops() {
        val crankCadence = WorkoutDeviceCrankCadence()
        var now = ContinuousClock.now
        crankCadence.update(revolutions = 10.toUShort(), time = 1024.toUShort(), now = now)
        now = now.advanced(bySeconds = 1.0)
        assertEquals(60, crankCadence.update(revolutions = 11.toUShort(), time = 2048.toUShort(), now = now))
        repeat(3) {
            now = now.advanced(bySeconds = 4.0)
            crankCadence.update(revolutions = 11.toUShort(), time = 2048.toUShort(), now = now)
        }
        assertEquals(0, crankCadence.update(revolutions = 11.toUShort(), time = 2048.toUShort(), now = now))
    }

    @Test
    fun forgetsCadenceOnReset() {
        val crankCadence = WorkoutDeviceCrankCadence()
        val now = ContinuousClock.now
        crankCadence.update(revolutions = 10.toUShort(), time = 1024.toUShort(), now = now)
        assertEquals(
            60,
            crankCadence.update(
                revolutions = 11.toUShort(),
                time = 2048.toUShort(),
                now = now.advanced(bySeconds = 1.0),
            ),
        )
        crankCadence.reset()
        assertNull(
            crankCadence.update(
                revolutions = 12.toUShort(),
                time = 3072.toUShort(),
                now = now.advanced(bySeconds = 2.0),
            ),
        )
    }
}

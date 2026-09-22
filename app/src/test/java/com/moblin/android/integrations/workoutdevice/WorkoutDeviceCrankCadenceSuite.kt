package com.moblin.android.integrations.workoutdevice

import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class WorkoutDeviceCrankCadenceSuite {
    @Test
    fun reportsNothingWithoutCrankData() {
        val crankCadence = WorkoutDeviceCrankCadence()
        val now = Instant.now()
        assertNull(crankCadence.update(null, null, now))
        assertNull(crankCadence.update(null, null, now.plusSeconds(10)))
    }

    @Test
    fun firstCrankMeasurementReportsNothing() {
        val crankCadence = WorkoutDeviceCrankCadence()
        val now = Instant.now().plusSeconds(10)
        assertNull(crankCadence.update(10.toUShort(), 1024.toUShort(), now))
    }

    @Test
    fun reportsZeroWhenCrankStops() {
        val crankCadence = WorkoutDeviceCrankCadence()
        var now = Instant.now()
        crankCadence.update(10.toUShort(), 1024.toUShort(), now)
        now = now.plusSeconds(1)
        assertEquals(60, crankCadence.update(11.toUShort(), 2048.toUShort(), now))
        repeat(3) {
            now = now.plusSeconds(4)
            crankCadence.update(11.toUShort(), 2048.toUShort(), now)
        }
        assertEquals(0, crankCadence.update(11.toUShort(), 2048.toUShort(), now))
    }

    @Test
    fun forgetsCadenceOnReset() {
        val crankCadence = WorkoutDeviceCrankCadence()
        val now = Instant.now()
        crankCadence.update(10.toUShort(), 1024.toUShort(), now)
        assertEquals(60, crankCadence.update(11.toUShort(), 2048.toUShort(), now.plusSeconds(1)))
        crankCadence.reset()
        assertNull(crankCadence.update(12.toUShort(), 3072.toUShort(), now.plusSeconds(2)))
    }
}

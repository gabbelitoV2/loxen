package com.moblin.android.integrations.workoutdevice

import android.os.Looper
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class WorkoutDeviceRunningSuite {
    @Test
    fun speedAndCadenceAreDecodedAndDistanceStartsAtZero() {
        val running = WorkoutDeviceRunning()
        val metrics = running.handleMeasurement(value = byteArrayOf(0x00, 0x80.toByte(), 0x02, 90))
        assertEquals(2.5, metrics.speed)
        assertEquals(90, metrics.cadence)
        assertEquals(0.0, metrics.distance)
    }

    @Test
    fun theStrideLengthIsSkippedBeforeTheTotalDistance() {
        val running = WorkoutDeviceRunning()
        val metrics = running.handleMeasurement(
            value = byteArrayOf(0x03, 0x00, 0x04, 170.toByte(), 0x7B, 0x00, 0x10, 0x27, 0x00, 0x00),
        )
        assertEquals(4.0, metrics.speed)
        assertEquals(170, metrics.cadence)
        assertEquals(1000.0, metrics.distance)
    }

    @Test
    fun theFallbackDistanceGrowsWithSpeedUntilTheDeviceReportsItsOwn() {
        val running = WorkoutDeviceRunning()
        running.handleMeasurement(value = byteArrayOf(0x00, 0x00, 0x0A, 80))
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))
        val fallback = running.handleMeasurement(value = byteArrayOf(0x00, 0x00, 0x0A, 80))
        assertEquals(10.0, fallback.speed)
        assertEquals(20.0, fallback.distance!!, 0.000001)
        val device = running.handleMeasurement(value = byteArrayOf(0x02, 0x00, 0x0A, 80, 0x64, 0x00, 0x00, 0x00))
        assertEquals(10.0, device.distance)
        assertNull(running.handleMeasurement(value = byteArrayOf(0x00, 0x00, 0x0A, 80)).distance)
        running.reset()
        assertEquals(0.0, running.handleMeasurement(value = byteArrayOf(0x00, 0x00, 0x0A, 80)).distance)
    }

    @Test
    fun aTruncatedMeasurementThrows() {
        val running = WorkoutDeviceRunning()
        assertFailsWith<Exception> {
            running.handleMeasurement(value = byteArrayOf(0x02, 0x00, 0x0A, 80, 0x64))
        }
    }
}

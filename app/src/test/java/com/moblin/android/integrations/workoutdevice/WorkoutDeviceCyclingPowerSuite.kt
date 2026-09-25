package com.moblin.android.integrations.workoutdevice

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private fun powerMeasurement(power: Short): ByteArray {
    val data = ByteArray(4)
    data[0] = 0x00
    data[1] = 0x00
    data[2] = (power.toInt() and 0xFF).toByte()
    data[3] = ((power.toInt() shr 8) and 0xFF).toByte()
    return data
}

private fun powerAndCrankMeasurement(power: Short, revolutions: Int, eventTime: Int): ByteArray {
    val data = ByteArray(8)
    data[0] = 0x20
    data[1] = 0x00
    data[2] = (power.toInt() and 0xFF).toByte()
    data[3] = ((power.toInt() shr 8) and 0xFF).toByte()
    data[4] = (revolutions and 0xFF).toByte()
    data[5] = ((revolutions shr 8) and 0xFF).toByte()
    data[6] = (eventTime and 0xFF).toByte()
    data[7] = ((eventTime shr 8) and 0xFF).toByte()
    return data
}

@RunWith(RobolectricTestRunner::class)
class WorkoutDeviceCyclingPowerSuite {
    @Test
    fun powerOnlyDeviceReportsNoCadence() {
        val device = WorkoutDeviceCyclingPower()
        device.handleMeasurement(powerMeasurement(150.toShort()))
        val (power, cadence) = device.handleMeasurement(powerMeasurement(150.toShort()))
        assertEquals(100, power)
        assertNull(cadence)
    }

    @Test
    fun calculatesCadenceFromCrankRevolutions() {
        val device = WorkoutDeviceCyclingPower()
        device.handleMeasurement(
            powerAndCrankMeasurement(power = 200.toShort(), revolutions = 10, eventTime = 1024)
        )
        val (_, cadence) = device.handleMeasurement(
            powerAndCrankMeasurement(power = 200.toShort(), revolutions = 13, eventTime = 1024 + 2048)
        )
        assertEquals(90, cadence)
    }

    @Test
    fun resetForgetsCadence() {
        val device = WorkoutDeviceCyclingPower()
        device.handleMeasurement(
            powerAndCrankMeasurement(power = 200.toShort(), revolutions = 10, eventTime = 1024)
        )
        val (_, cadenceBeforeReset) = device.handleMeasurement(
            powerAndCrankMeasurement(power = 200.toShort(), revolutions = 13, eventTime = 1024 + 2048)
        )
        assertEquals(90, cadenceBeforeReset)
        device.reset()
        val (_, cadence) = device.handleMeasurement(powerMeasurement(200.toShort()))
        assertNull(cadence)
    }
}

package com.moblin.android.integrations.workoutdevice

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private fun powerMeasurement(power: Short): ByteArray {
    val bytes = ByteArray(4)
    val buffer = ByteBuffer.wrap(bytes)
    buffer.order(ByteOrder.LITTLE_ENDIAN)
    buffer.position(2)
    buffer.putShort(power)
    return bytes
}

private fun powerAndCrankMeasurement(power: Short, revolutions: UShort, eventTime: UShort): ByteArray {
    val bytes = ByteArray(8)
    bytes[0] = 0x20
    val buffer = ByteBuffer.wrap(bytes)
    buffer.order(ByteOrder.LITTLE_ENDIAN)
    buffer.position(2)
    buffer.putShort(power)
    buffer.putShort(revolutions.toShort())
    buffer.putShort(eventTime.toShort())
    return bytes
}

@RunWith(RobolectricTestRunner::class)
class WorkoutDeviceCyclingPowerSuite {
    @Test
    fun powerOnlyDeviceReportsNoCadence() {
        val device = WorkoutDeviceCyclingPower()
        device.handleMeasurement(value = powerMeasurement(150.toShort()))
        val (power, cadence) = device.handleMeasurement(value = powerMeasurement(150.toShort()))
        assertEquals(100, power)
        assertNull(cadence)
    }

    @Test
    fun calculatesCadenceFromCrankRevolutions() {
        val device = WorkoutDeviceCyclingPower()
        device.handleMeasurement(
            value = powerAndCrankMeasurement(200.toShort(), 10.toUShort(), 1024.toUShort()),
        )
        val (_, cadence) = device.handleMeasurement(
            value = powerAndCrankMeasurement(200.toShort(), 13.toUShort(), (1024 + 2048).toUShort()),
        )
        assertEquals(90, cadence)
    }

    @Test
    fun resetForgetsCadence() {
        val device = WorkoutDeviceCyclingPower()
        device.handleMeasurement(
            value = powerAndCrankMeasurement(200.toShort(), 10.toUShort(), 1024.toUShort()),
        )
        val (_, cadenceBeforeReset) = device.handleMeasurement(
            value = powerAndCrankMeasurement(200.toShort(), 13.toUShort(), (1024 + 2048).toUShort()),
        )
        assertEquals(90, cadenceBeforeReset)
        device.reset()
        val (_, cadence) = device.handleMeasurement(value = powerMeasurement(200.toShort()))
        assertNull(cadence)
    }
}

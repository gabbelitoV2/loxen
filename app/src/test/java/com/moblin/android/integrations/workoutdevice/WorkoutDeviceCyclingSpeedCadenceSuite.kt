package com.moblin.android.integrations.workoutdevice

import com.moblin.android.isEqual
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private fun crankMeasurement(revolutions: Int, eventTime: Int): ByteArray {
    val data = ByteArray(5)
    data[0] = 0x02
    data[1] = (revolutions and 0xFF).toByte()
    data[2] = ((revolutions shr 8) and 0xFF).toByte()
    data[3] = (eventTime and 0xFF).toByte()
    data[4] = ((eventTime shr 8) and 0xFF).toByte()
    return data
}

private fun wheelMeasurement(revolutions: UInt, eventTime: Int): ByteArray {
    val data = ByteArray(7)
    val revolutionCount = revolutions.toLong()
    data[0] = 0x01
    data[1] = (revolutionCount and 0xFF).toByte()
    data[2] = ((revolutionCount shr 8) and 0xFF).toByte()
    data[3] = ((revolutionCount shr 16) and 0xFF).toByte()
    data[4] = ((revolutionCount shr 24) and 0xFF).toByte()
    data[5] = (eventTime and 0xFF).toByte()
    data[6] = ((eventTime shr 8) and 0xFF).toByte()
    return data
}

private fun wheelAndCrankMeasurement(wheelRevolutions: UInt,
                                    wheelEventTime: Int,
                                    crankRevolutions: Int,
                                    crankEventTime: Int): ByteArray {
    val data = ByteArray(11)
    val wheelRevolutionCount = wheelRevolutions.toLong()
    data[0] = 0x03
    data[1] = (wheelRevolutionCount and 0xFF).toByte()
    data[2] = ((wheelRevolutionCount shr 8) and 0xFF).toByte()
    data[3] = ((wheelRevolutionCount shr 16) and 0xFF).toByte()
    data[4] = ((wheelRevolutionCount shr 24) and 0xFF).toByte()
    data[5] = (wheelEventTime and 0xFF).toByte()
    data[6] = ((wheelEventTime shr 8) and 0xFF).toByte()
    data[7] = (crankRevolutions and 0xFF).toByte()
    data[8] = ((crankRevolutions shr 8) and 0xFF).toByte()
    data[9] = (crankEventTime and 0xFF).toByte()
    data[10] = ((crankEventTime shr 8) and 0xFF).toByte()
    return data
}

@RunWith(RobolectricTestRunner::class)
class WorkoutDeviceCyclingSpeedCadenceSuite {
    @Test
    fun firstMeasurementOnlySeedsState() {
        val device = WorkoutDeviceCyclingSpeedCadence(wheelCircumference = 2105)
        val (speed, cadence) = device.handleMeasurement(
            value = crankMeasurement(revolutions = 10, eventTime = 1024)
        )
        assertNull(cadence)
        assertNull(speed)
    }

    @Test
    fun calculatesCadenceFromCrankRevolutions() {
        val device = WorkoutDeviceCyclingSpeedCadence(wheelCircumference = 2105)
        device.handleMeasurement(value = crankMeasurement(revolutions = 10, eventTime = 1024))
        val (_, cadence) = device.handleMeasurement(
            value = crankMeasurement(revolutions = 13, eventTime = 1024 + 2048)
        )
        assertEquals(90, cadence)
    }

    @Test
    fun handlesCrankRevolutionsWrapAround() {
        val device = WorkoutDeviceCyclingSpeedCadence(wheelCircumference = 2105)
        device.handleMeasurement(value = crankMeasurement(revolutions = 65535, eventTime = 65000))
        val (_, cadence) = device.handleMeasurement(
            value = crankMeasurement(revolutions = 0, eventTime = (65000 + 1024) and 0xFFFF)
        )
        assertEquals(60, cadence)
    }

    @Test
    fun ignoresMeasurementWithoutTimeProgress() {
        val device = WorkoutDeviceCyclingSpeedCadence(wheelCircumference = 2105)
        device.handleMeasurement(value = crankMeasurement(revolutions = 10, eventTime = 1024))
        device.handleMeasurement(value = crankMeasurement(revolutions = 11, eventTime = 1024 + 1024))
        val (_, cadence) = device.handleMeasurement(
            value = crankMeasurement(revolutions = 11, eventTime = 1024 + 1024)
        )
        assertEquals(60, cadence)
    }

    @Test
    fun cadenceOnlySensorReportsNoSpeed() {
        val device = WorkoutDeviceCyclingSpeedCadence(wheelCircumference = 2105)
        device.handleMeasurement(value = crankMeasurement(revolutions = 10, eventTime = 1024))
        val (speed, cadence) = device.handleMeasurement(
            value = crankMeasurement(revolutions = 11, eventTime = 1024 + 1024)
        )
        assertEquals(60, cadence)
        assertNull(speed)
    }

    @Test
    fun speedOnlySensorReportsNoCadence() {
        val device = WorkoutDeviceCyclingSpeedCadence(wheelCircumference = 2105)
        device.setWheelCircumference(millimeters = 2000)
        device.handleMeasurement(value = wheelMeasurement(revolutions = 100u, eventTime = 1024))
        val (speed, cadence) = device.handleMeasurement(
            value = wheelMeasurement(revolutions = 105u, eventTime = 1024 + 1024)
        )
        assertNull(cadence)
        assertTrue(isEqual(requireNotNull(speed), 10.0, 0.001))
    }

    @Test
    fun calculatesSpeedAndCadenceFromCombinedMeasurement() {
        val device = WorkoutDeviceCyclingSpeedCadence(wheelCircumference = 2105)
        device.setWheelCircumference(millimeters = 2000)
        device.handleMeasurement(
            value = wheelAndCrankMeasurement(
                wheelRevolutions = 100u,
                wheelEventTime = 1024,
                crankRevolutions = 10,
                crankEventTime = 1024
            )
        )
        val (speed, cadence) = device.handleMeasurement(
            value = wheelAndCrankMeasurement(
                wheelRevolutions = 105u,
                wheelEventTime = 1024 + 1024,
                crankRevolutions = 11,
                crankEventTime = 1024 + 1024
            )
        )
        assertEquals(60, cadence)
        assertTrue(isEqual(requireNotNull(speed), 10.0, 0.001))
    }

    @Test
    fun handlesWheelRevolutionsWrapAround() {
        val device = WorkoutDeviceCyclingSpeedCadence(wheelCircumference = 2105)
        device.setWheelCircumference(millimeters = 2000)
        device.handleMeasurement(
            value = wheelMeasurement(revolutions = UInt.MAX_VALUE, eventTime = 1024)
        )
        val (speed, _) = device.handleMeasurement(
            value = wheelMeasurement(revolutions = 4u, eventTime = 1024 + 1024)
        )
        assertTrue(isEqual(requireNotNull(speed), 10.0, 0.001))
    }

    @Test
    fun rejectsTruncatedMeasurement() {
        val device = WorkoutDeviceCyclingSpeedCadence(wheelCircumference = 2105)
        assertFailsWith<Exception> {
            device.handleMeasurement(value = byteArrayOf(0x02, 0x01))
        }
    }
}

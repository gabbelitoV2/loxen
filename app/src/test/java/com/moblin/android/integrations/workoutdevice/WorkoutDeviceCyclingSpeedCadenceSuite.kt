package com.moblin.android.integrations.workoutdevice

import com.moblin.android.isEqual
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

private fun crankMeasurement(revolutions: UShort, eventTime: UShort): ByteArray {
    val buffer = ByteBuffer.allocate(5).order(ByteOrder.LITTLE_ENDIAN)
    buffer.put(0x02.toByte())
    buffer.putShort(revolutions.toShort())
    buffer.putShort(eventTime.toShort())
    return buffer.array()
}

private fun wheelMeasurement(revolutions: UInt, eventTime: UShort): ByteArray {
    val buffer = ByteBuffer.allocate(7).order(ByteOrder.LITTLE_ENDIAN)
    buffer.put(0x01.toByte())
    buffer.putInt(revolutions.toInt())
    buffer.putShort(eventTime.toShort())
    return buffer.array()
}

private fun wheelAndCrankMeasurement(wheelRevolutions: UInt,
                                     wheelEventTime: UShort,
                                     crankRevolutions: UShort,
                                     crankEventTime: UShort): ByteArray {
    val buffer = ByteBuffer.allocate(11).order(ByteOrder.LITTLE_ENDIAN)
    buffer.put(0x03.toByte())
    buffer.putInt(wheelRevolutions.toInt())
    buffer.putShort(wheelEventTime.toShort())
    buffer.putShort(crankRevolutions.toShort())
    buffer.putShort(crankEventTime.toShort())
    return buffer.array()
}

class WorkoutDeviceCyclingSpeedCadenceSuite {
    @Test
    fun firstMeasurementOnlySeedsState() {
        val device = WorkoutDeviceCyclingSpeedCadence(wheelCircumference = 2105)
        val measurement = device.handleMeasurement(
            value = crankMeasurement(revolutions = 10u, eventTime = 1024u)
        )
        assertNull(measurement.second)
        assertNull(measurement.first)
    }

    @Test
    fun calculatesCadenceFromCrankRevolutions() {
        val device = WorkoutDeviceCyclingSpeedCadence(wheelCircumference = 2105)
        device.handleMeasurement(value = crankMeasurement(revolutions = 10u, eventTime = 1024u))
        val measurement = device.handleMeasurement(
            value = crankMeasurement(revolutions = 13u, eventTime = 3072u)
        )
        assertEquals(90, measurement.second)
    }

    @Test
    fun handlesCrankRevolutionsWrapAround() {
        val device = WorkoutDeviceCyclingSpeedCadence(wheelCircumference = 2105)
        device.handleMeasurement(value = crankMeasurement(revolutions = 65535u, eventTime = 65000u))
        val measurement = device.handleMeasurement(
            value = crankMeasurement(revolutions = 0u, eventTime = (65000u + 1024u).toUShort())
        )
        assertEquals(60, measurement.second)
    }

    @Test
    fun ignoresMeasurementWithoutTimeProgress() {
        val device = WorkoutDeviceCyclingSpeedCadence(wheelCircumference = 2105)
        device.handleMeasurement(value = crankMeasurement(revolutions = 10u, eventTime = 1024u))
        device.handleMeasurement(value = crankMeasurement(revolutions = 11u, eventTime = 2048u))
        val measurement = device.handleMeasurement(
            value = crankMeasurement(revolutions = 11u, eventTime = 2048u)
        )
        assertEquals(60, measurement.second)
    }

    @Test
    fun cadenceOnlySensorReportsNoSpeed() {
        val device = WorkoutDeviceCyclingSpeedCadence(wheelCircumference = 2105)
        device.handleMeasurement(value = crankMeasurement(revolutions = 10u, eventTime = 1024u))
        val measurement = device.handleMeasurement(
            value = crankMeasurement(revolutions = 11u, eventTime = 2048u)
        )
        assertEquals(60, measurement.second)
        assertNull(measurement.first)
    }

    @Test
    fun speedOnlySensorReportsNoCadence() {
        val device = WorkoutDeviceCyclingSpeedCadence(wheelCircumference = 2105)
        device.setWheelCircumference(millimeters = 2000)
        device.handleMeasurement(value = wheelMeasurement(revolutions = 100u, eventTime = 1024u))
        val measurement = device.handleMeasurement(
            value = wheelMeasurement(revolutions = 105u, eventTime = 2048u)
        )
        assertNull(measurement.second)
        assertTrue(isEqual(requireNotNull(measurement.first), 10.0, 0.001))
    }

    @Test
    fun calculatesSpeedAndCadenceFromCombinedMeasurement() {
        val device = WorkoutDeviceCyclingSpeedCadence(wheelCircumference = 2105)
        device.setWheelCircumference(millimeters = 2000)
        device.handleMeasurement(value = wheelAndCrankMeasurement(
            wheelRevolutions = 100u,
            wheelEventTime = 1024u,
            crankRevolutions = 10u,
            crankEventTime = 1024u
        ))
        val measurement = device.handleMeasurement(value = wheelAndCrankMeasurement(
            wheelRevolutions = 105u,
            wheelEventTime = 2048u,
            crankRevolutions = 11u,
            crankEventTime = 2048u
        ))
        assertEquals(60, measurement.second)
        assertTrue(isEqual(requireNotNull(measurement.first), 10.0, 0.001))
    }

    @Test
    fun handlesWheelRevolutionsWrapAround() {
        val device = WorkoutDeviceCyclingSpeedCadence(wheelCircumference = 2105)
        device.setWheelCircumference(millimeters = 2000)
        device.handleMeasurement(
            value = wheelMeasurement(revolutions = UInt.MAX_VALUE, eventTime = 1024u)
        )
        val measurement = device.handleMeasurement(
            value = wheelMeasurement(revolutions = 4u, eventTime = 2048u)
        )
        assertTrue(isEqual(requireNotNull(measurement.first), 10.0, 0.001))
    }

    @Test
    fun rejectsTruncatedMeasurement() {
        val device = WorkoutDeviceCyclingSpeedCadence(wheelCircumference = 2105)
        assertFailsWith<Exception> {
            device.handleMeasurement(value = byteArrayOf(0x02, 0x01))
        }
    }
}

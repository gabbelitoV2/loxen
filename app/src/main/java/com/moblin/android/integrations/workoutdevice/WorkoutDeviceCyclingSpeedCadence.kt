package com.moblin.android.integrations.workoutdevice

import android.bluetooth.BluetoothGattCharacteristic
import com.moblin.android.media.haishinkit.util.ByteReader
import java.util.UUID
import kotlin.math.min

val workoutDeviceCyclingSpeedCadenceServiceId: UUID =
    UUID.fromString("00001816-0000-1000-8000-00805F9B34FB")

val workoutDeviceCyclingSpeedCadenceMeasurementCharacteristicId: UUID =
    UUID.fromString("00002A5B-0000-1000-8000-00805F9B34FB")

private const val measurementWheelRevolutionDataFlagIndex = 0
private const val measurementCrankRevolutionDataFlagIndex = 1

private fun UByte.isBitSet(index: Int): Boolean = ((this.toInt() shr index) and 1) == 1

private class CyclingSpeedCadenceMeasurement {
    var cumulativeWheelRevolutions: UInt? = null
    var lastWheelEventTime: UShort? = null
    var cumulativeCrankRevolutions: UShort? = null
    var lastCrankEventTime: UShort? = null

    constructor(value: ByteArray) {
        val reader = ByteReader(value)
        val flags = reader.readUInt8()
        if (flags.isBitSet(measurementWheelRevolutionDataFlagIndex)) {
            cumulativeWheelRevolutions = reader.readUInt32Le()
            lastWheelEventTime = reader.readUInt16Le()
        }
        if (flags.isBitSet(measurementCrankRevolutionDataFlagIndex)) {
            cumulativeCrankRevolutions = reader.readUInt16Le()
            lastCrankEventTime = reader.readUInt16Le()
        }
    }
}

class WorkoutDeviceCyclingSpeedCadence(wheelCircumference: Int) {
    private var measurementCharacteristic: BluetoothGattCharacteristic? = null
    private var previousWheelRevolutions: UInt? = null
    private var previousWheelRevolutionsTime: UShort? = null
    private val crankCadence = WorkoutDeviceCrankCadence()
    private val averageSpeed = WorkoutDeviceAverageCalculator()
    private var latestAverageSpeedUpdateTime = System.nanoTime()
    private var reportsWheelRevolutions = false
    private var wheelCircumferenceMeters: Double = wheelCircumference.toDouble() / 1000.0

    fun reset() {
        measurementCharacteristic = null
        resetMeasurements()
        reportsWheelRevolutions = false
    }

    fun resetMeasurements() {
        previousWheelRevolutions = null
        previousWheelRevolutionsTime = null
        crankCadence.reset()
        averageSpeed.reset()
    }

    fun setMeasurementCharacteristic(characteristic: BluetoothGattCharacteristic) {
        measurementCharacteristic = characteristic
    }

    fun isAnyCharacteristicDiscovered(): Boolean {
        return measurementCharacteristic != null
    }

    fun setWheelCircumference(millimeters: Int) {
        wheelCircumferenceMeters = millimeters.toDouble() / 1000.0
    }

    fun handleMeasurement(value: ByteArray): Pair<Double?, Int?> {
        val measurement = CyclingSpeedCadenceMeasurement(value)
        val now = System.nanoTime()
        val cadence = crankCadence.update(revolutions = measurement.cumulativeCrankRevolutions,
                                          time = measurement.lastCrankEventTime,
                                          now = now)
        updateSpeed(measurement = measurement, now = now)
        return Pair(if (reportsWheelRevolutions) averageSpeed.averageIgnoreZeros() else null,
                    cadence)
    }

    private fun updateSpeed(measurement: CyclingSpeedCadenceMeasurement, now: Long) {
        var speed = -1.0
        val revolutions = measurement.cumulativeWheelRevolutions
        val time = measurement.lastWheelEventTime
        if (revolutions != null && time != null) {
            reportsWheelRevolutions = true
            val previousRevolutions = previousWheelRevolutions
            val previousRevolutionsTime = previousWheelRevolutionsTime
            if (previousRevolutions != null && previousRevolutionsTime != null) {
                var deltaRevolutions = revolutions.toLong() - previousRevolutions.toLong()
                if (deltaRevolutions < 0) {
                    deltaRevolutions += 4_294_967_296L
                }
                deltaRevolutions = min(deltaRevolutions, 1000L)
                var deltaTime = time.toLong() - previousRevolutionsTime.toLong()
                if (deltaTime < 0) {
                    deltaTime += 65536
                }
                val deltaTimeSeconds = deltaTime.toDouble() / 1024.0
                if (deltaTimeSeconds > 0) {
                    speed = deltaRevolutions.toDouble() * wheelCircumferenceMeters / deltaTimeSeconds
                    speed = min(speed, 100.0)
                }
            }
            previousWheelRevolutions = revolutions
            previousWheelRevolutionsTime = time
        }
        if (speed != -1.0) {
            averageSpeed.update(value = speed)
            latestAverageSpeedUpdateTime = now
        } else if (now - latestAverageSpeedUpdateTime > 3_000_000_000L) {
            averageSpeed.update(value = 0.0)
        }
    }
}

package com.moblin.android.integrations.workoutdevice

import com.moblin.android.common.various.isBitSet
import com.moblin.android.media.haishinkit.util.ByteReader
import com.moblin.android.platform.core.ContinuousClock
import com.moblin.android.platform.corebluetooth.CBCharacteristic
import com.moblin.android.platform.corebluetooth.CBUUID
import kotlin.time.Duration.Companion.seconds

val workoutDeviceCyclingSpeedCadenceServiceId = CBUUID(string = "1816")
val workoutDeviceCyclingSpeedCadenceMeasurementCharacteristicId = CBUUID(string = "2A5B")

private const val measurementWheelRevolutionDataFlagIndex = 0
private const val measurementCrankRevolutionDataFlagIndex = 1

private data class CyclingSpeedCadenceMeasurement(
    val cumulativeWheelRevolutions: UInt? = null,
    val lastWheelEventTime: UShort? = null,
    val cumulativeCrankRevolutions: UShort? = null,
    val lastCrankEventTime: UShort? = null,
) {
    companion object {
        fun parse(value: ByteArray): CyclingSpeedCadenceMeasurement {
            val reader = ByteReader(data = value)
            val flags = reader.readUInt8()
            var cumulativeWheelRevolutions: UInt? = null
            var lastWheelEventTime: UShort? = null
            if (flags.isBitSet(index = measurementWheelRevolutionDataFlagIndex)) {
                cumulativeWheelRevolutions = reader.readUInt32Le()
                lastWheelEventTime = reader.readUInt16Le()
            }
            var cumulativeCrankRevolutions: UShort? = null
            var lastCrankEventTime: UShort? = null
            if (flags.isBitSet(index = measurementCrankRevolutionDataFlagIndex)) {
                cumulativeCrankRevolutions = reader.readUInt16Le()
                lastCrankEventTime = reader.readUInt16Le()
            }
            return CyclingSpeedCadenceMeasurement(
                cumulativeWheelRevolutions = cumulativeWheelRevolutions,
                lastWheelEventTime = lastWheelEventTime,
                cumulativeCrankRevolutions = cumulativeCrankRevolutions,
                lastCrankEventTime = lastCrankEventTime,
            )
        }
    }
}

open class WorkoutDeviceCyclingSpeedCadence(wheelCircumference: Int) {
    private var measurementCharacteristic: CBCharacteristic? = null
    private var previousWheelRevolutions: UInt? = null
    private var previousWheelRevolutionsTime: UShort? = null
    private val crankCadence = WorkoutDeviceCrankCadence()
    private val averageSpeed = WorkoutDeviceAverageCalculator()
    private var latestAverageSpeedUpdateTime = ContinuousClock.now
    private var reportsWheelRevolutions = false
    private var wheelCircumferenceMeters: Double = wheelCircumference.toDouble() / 1000

    open fun reset() {
        measurementCharacteristic = null
        resetMeasurements()
        reportsWheelRevolutions = false
    }

    open fun resetMeasurements() {
        previousWheelRevolutions = null
        previousWheelRevolutionsTime = null
        crankCadence.reset()
        averageSpeed.reset()
    }

    open fun setMeasurementCharacteristic(characteristic: CBCharacteristic) {
        measurementCharacteristic = characteristic
    }

    open fun isAnyCharacteristicDiscovered(): Boolean {
        return measurementCharacteristic != null
    }

    open fun setWheelCircumference(millimeters: Int) {
        wheelCircumferenceMeters = millimeters.toDouble() / 1000
    }

    open fun handleMeasurement(value: ByteArray): Pair<Double?, Int?> {
        val measurement = CyclingSpeedCadenceMeasurement.parse(value)
        val now = ContinuousClock.now
        val cadence = crankCadence.update(revolutions = measurement.cumulativeCrankRevolutions,
                                          time = measurement.lastCrankEventTime,
                                          now = now)
        updateSpeed(measurement = measurement, now = now)
        return Pair(if (reportsWheelRevolutions) averageSpeed.averageIgnoreZeros() else null,
                    cadence)
    }

    private fun updateSpeed(measurement: CyclingSpeedCadenceMeasurement, now: ContinuousClock.Instant) {
        var speed = -1.0
        val revolutions = measurement.cumulativeWheelRevolutions
        val time = measurement.lastWheelEventTime
        if (revolutions != null && time != null) {
            reportsWheelRevolutions = true
            val previousWheelRevolutions = this.previousWheelRevolutions
            val previousWheelRevolutionsTime = this.previousWheelRevolutionsTime
            if (previousWheelRevolutions != null && previousWheelRevolutionsTime != null) {
                var deltaRevolutions = revolutions.toLong() - previousWheelRevolutions.toLong()
                if (deltaRevolutions < 0) {
                    deltaRevolutions += 4_294_967_296L
                }
                deltaRevolutions = minOf(deltaRevolutions, 1000L)
                var deltaTime = time.toInt() - previousWheelRevolutionsTime.toInt()
                if (deltaTime < 0) {
                    deltaTime += 65536
                }
                val deltaTimeSeconds = deltaTime.toDouble() / 1024
                if (deltaTimeSeconds > 0) {
                    speed = deltaRevolutions.toDouble() * wheelCircumferenceMeters / deltaTimeSeconds
                    speed = minOf(speed, 100.0)
                }
            }
            this.previousWheelRevolutions = revolutions
            this.previousWheelRevolutionsTime = time
        }
        if (speed != -1.0) {
            averageSpeed.update(value = speed)
            latestAverageSpeedUpdateTime = now
        } else if (latestAverageSpeedUpdateTime.duration(to = now) > 3.seconds) {
            averageSpeed.update(value = 0.0)
        }
    }
}

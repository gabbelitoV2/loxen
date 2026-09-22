package com.moblin.android.integrations.workoutdevice

import android.bluetooth.BluetoothGattCharacteristic
import com.moblin.android.media.haishinkit.util.ByteReader
import java.time.Instant
import java.util.UUID

val workoutDeviceCyclingPowerServiceId: UUID =
    UUID.fromString("00001818-0000-1000-8000-00805F9B34FB")
val workoutDeviceCyclingPowerMeasurementCharacteristicId: UUID =
    UUID.fromString("00002A63-0000-1000-8000-00805F9B34FB")
val workoutDeviceCyclingPowerVectorCharacteristicId: UUID =
    UUID.fromString("00002A64-0000-1000-8000-00805F9B34FB")

private val measurementPedalPowerBalanceFlagIndex = 0
private val measurementAccumulatedTorqueFlagIndex = 2
private val measurementWheelRevolutionDataFlagIndex = 4
private val measurementCrankRevolutionDataFlagIndex = 5
private val measurementExtremeForceFlagIndex = 6
private val measurementExtremeTorqueFlagIndex = 7
private val measurementExtremeAnglesFlagIndex = 8
private val measurementTopDeadSpotAngleFlagIndex = 9
private val measurementBottomDeadSpotAngleFlagIndex = 10
private val measurementAccumulatedEnergyFlagIndex = 11

private class PowerMeasurement(value: ByteArray) {
    var instantaneousPower: UShort = 0u.toUShort()
    var pedalPowerBalance: UByte? = null
    var accumulatedTorque: UShort? = null
    var cumulativeWheelRevolutions: UInt? = null
    var lastWheelEventTime: UShort? = null
    var cumulativeCrankRevolutions: UShort? = null
    var lastCrankEventTime: UShort? = null
    var maximumForceMagnitude: UShort? = null
    var minimumForceMagnitude: UShort? = null
    var maximumTorqueMagnitude: UShort? = null
    var minimumTorqueMagnitude: UShort? = null
    var extremeAngles: UShort? = null
    var topDeadSpotAngle: UShort? = null
    var bottomDeadSpotAngle: UShort? = null
    var accumulatedEnergy: UShort? = null

    init {
        val reader = ByteReader(value)
        val flags = reader.readUInt16Le()
        instantaneousPower = reader.readUInt16Le()
        if ((flags.toInt() and (1 shl measurementPedalPowerBalanceFlagIndex)) != 0) {
            pedalPowerBalance = reader.readUInt8()
        }
        if ((flags.toInt() and (1 shl measurementAccumulatedTorqueFlagIndex)) != 0) {
            accumulatedTorque = reader.readUInt16Le()
        }
        if ((flags.toInt() and (1 shl measurementWheelRevolutionDataFlagIndex)) != 0) {
            cumulativeWheelRevolutions = reader.readUInt32Le()
            lastWheelEventTime = reader.readUInt16Le()
        }
        if ((flags.toInt() and (1 shl measurementCrankRevolutionDataFlagIndex)) != 0) {
            cumulativeCrankRevolutions = reader.readUInt16Le()
            lastCrankEventTime = reader.readUInt16Le()
        }
        if ((flags.toInt() and (1 shl measurementExtremeForceFlagIndex)) != 0) {
            maximumForceMagnitude = reader.readUInt16Le()
            minimumForceMagnitude = reader.readUInt16Le()
        }
        if ((flags.toInt() and (1 shl measurementExtremeTorqueFlagIndex)) != 0) {
            maximumTorqueMagnitude = reader.readUInt16Le()
            minimumTorqueMagnitude = reader.readUInt16Le()
        }
        if ((flags.toInt() and (1 shl measurementExtremeAnglesFlagIndex)) != 0) {
            reader.readBytes(3)
        }
        if ((flags.toInt() and (1 shl measurementTopDeadSpotAngleFlagIndex)) != 0) {
            topDeadSpotAngle = reader.readUInt16Le()
        }
        if ((flags.toInt() and (1 shl measurementBottomDeadSpotAngleFlagIndex)) != 0) {
            bottomDeadSpotAngle = reader.readUInt16Le()
        }
        if ((flags.toInt() and (1 shl measurementAccumulatedEnergyFlagIndex)) != 0) {
            accumulatedEnergy = reader.readUInt16Le()
        }
    }
}

private val vectorCrankRevolutionDataFlagIndex = 0
private val vectorFirstCrankMeasurementAngleFlagIndex = 1
private val vectorInstantaneousForceArrayFlagIndex = 2
private val vectorInstantaneousTorqueArrayFlagIndex = 3
private val vectorInstantaneousMeasurementDirectionMask: UByte = 0b110000u
private val vectorInstantaneousMeasurementDirectionIndex = 4

private enum class WorkoutDeviceCyclingPowerInstantaneousMeasurementDirection(val rawValue: UByte) {
    unknown(0u.toUByte()),
    tangentialComponent(1u.toUByte()),
    radialComponent(2u.toUByte()),
    lateralComponent(3u.toUByte());

    companion object {
        fun fromRawValue(value: UByte): WorkoutDeviceCyclingPowerInstantaneousMeasurementDirection? {
            return entries.firstOrNull { it.rawValue == value }
        }
    }
}

private class PowerVector(value: ByteArray) {
    var cumulativeCrankRevolutions: UShort? = null
    var lastCrankEventTime: UShort? = null
    var firstCrankMeasurementAngle: UShort? = null
    var instantaneousForceMagnitudes: MutableList<UShort>? = null
    var instantaneousTorqueMagnitudes: MutableList<UShort>? = null
    var instantaneousMeasurementDirection: WorkoutDeviceCyclingPowerInstantaneousMeasurementDirection? = null

    init {
        val reader = ByteReader(value)
        val flags = reader.readUInt8()
        if ((flags.toInt() and (1 shl vectorCrankRevolutionDataFlagIndex)) != 0) {
            cumulativeCrankRevolutions = reader.readUInt16Le()
            lastCrankEventTime = reader.readUInt16Le()
        }
        if ((flags.toInt() and (1 shl vectorFirstCrankMeasurementAngleFlagIndex)) != 0) {
            firstCrankMeasurementAngle = reader.readUInt16Le()
        }
        while (reader.bytesAvailable >= 2) {
            val element = reader.readUInt16Le()
            if ((flags.toInt() and (1 shl vectorInstantaneousForceArrayFlagIndex)) != 0) {
                if (instantaneousForceMagnitudes == null) {
                    instantaneousForceMagnitudes = mutableListOf()
                }
                instantaneousForceMagnitudes!!.add(element)
            } else if ((flags.toInt() and (1 shl vectorInstantaneousTorqueArrayFlagIndex)) != 0) {
                if (instantaneousTorqueMagnitudes == null) {
                    instantaneousTorqueMagnitudes = mutableListOf()
                }
                instantaneousTorqueMagnitudes!!.add(element)
            }
        }
        val direction = ((flags.toInt() and vectorInstantaneousMeasurementDirectionMask.toInt()) shr
            vectorInstantaneousMeasurementDirectionIndex).toUByte()
        instantaneousMeasurementDirection =
            WorkoutDeviceCyclingPowerInstantaneousMeasurementDirection.fromRawValue(direction)
    }
}

class WorkoutDeviceCyclingPower {
    private var measurementCharacteristic: BluetoothGattCharacteristic? = null
    private val averagePower = WorkoutDeviceAverageCalculator()
    private val crankCadence = WorkoutDeviceCrankCadence()

    fun reset() {
        measurementCharacteristic = null
        resetMeasurements()
    }

    fun resetMeasurements() {
        crankCadence.reset()
    }

    fun setMeasurementCharacteristic(characteristic: BluetoothGattCharacteristic) {
        measurementCharacteristic = characteristic
    }

    fun isAnyCharacteristicDiscovered(): Boolean {
        return measurementCharacteristic != null
    }

    fun handleMeasurement(value: ByteArray): Pair<Int, Int?> {
        val measurement = PowerMeasurement(value)
        averagePower.update(measurement.instantaneousPower.toDouble())
        val cadence = crankCadence.update(measurement.cumulativeCrankRevolutions,
            measurement.lastCrankEventTime,
            Instant.now())
        return Pair(averagePower.average().toInt(), cadence)
    }

    fun handlePowerVector(value: ByteArray) {
        PowerVector(value)
    }
}

package com.moblin.android.integrations.workoutdevice

import com.moblin.android.common.various.isBitSet
import com.moblin.android.media.haishinkit.util.ByteReader
import com.moblin.android.platform.core.ContinuousClock
import com.moblin.android.platform.corebluetooth.CBCharacteristic
import com.moblin.android.platform.corebluetooth.CBUUID

val workoutDeviceCyclingPowerServiceId = CBUUID(string = "1818")
val workoutDeviceCyclingPowerMeasurementCharacteristicId = CBUUID(string = "2A63")
val workoutDeviceCyclingPowerVectorCharacteristicId = CBUUID(string = "2A64")

private const val measurementPedalPowerBalanceFlagIndex = 0
private const val measurementAccumulatedTorqueFlagIndex = 2
private const val measurementWheelRevolutionDataFlagIndex = 4
private const val measurementCrankRevolutionDataFlagIndex = 5
private const val measurementExtremeForceFlagIndex = 6
private const val measurementExtremeTorqueFlagIndex = 7
private const val measurementExtremeAnglesFlagIndex = 8
private const val measurementTopDeadSpotAngleFlagIndex = 9
private const val measurementBottomDeadSpotAngleFlagIndex = 10
private const val measurementAccumulatedEnergyFlagIndex = 11

private class PowerMeasurement(value: ByteArray) {
    var instantaneousPower: UShort = 0u
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
        val reader = ByteReader(data = value)
        val flags = reader.readUInt16Le()
        instantaneousPower = reader.readUInt16Le()
        if (flags.isBitSet(index = measurementPedalPowerBalanceFlagIndex)) {
            pedalPowerBalance = reader.readUInt8()
        }
        if (flags.isBitSet(index = measurementAccumulatedTorqueFlagIndex)) {
            accumulatedTorque = reader.readUInt16Le()
        }
        if (flags.isBitSet(index = measurementWheelRevolutionDataFlagIndex)) {
            cumulativeWheelRevolutions = reader.readUInt32Le()
            lastWheelEventTime = reader.readUInt16Le()
        }
        if (flags.isBitSet(index = measurementCrankRevolutionDataFlagIndex)) {
            cumulativeCrankRevolutions = reader.readUInt16Le()
            lastCrankEventTime = reader.readUInt16Le()
        }
        if (flags.isBitSet(index = measurementExtremeForceFlagIndex)) {
            maximumForceMagnitude = reader.readUInt16Le()
            minimumForceMagnitude = reader.readUInt16Le()
        }
        if (flags.isBitSet(index = measurementExtremeTorqueFlagIndex)) {
            maximumTorqueMagnitude = reader.readUInt16Le()
            minimumTorqueMagnitude = reader.readUInt16Le()
        }
        if (flags.isBitSet(index = measurementExtremeAnglesFlagIndex)) {
            reader.readBytes(3)
        }
        if (flags.isBitSet(index = measurementTopDeadSpotAngleFlagIndex)) {
            topDeadSpotAngle = reader.readUInt16Le()
        }
        if (flags.isBitSet(index = measurementBottomDeadSpotAngleFlagIndex)) {
            bottomDeadSpotAngle = reader.readUInt16Le()
        }
        if (flags.isBitSet(index = measurementAccumulatedEnergyFlagIndex)) {
            accumulatedEnergy = reader.readUInt16Le()
        }
    }
}

private const val vectorCrankRevolutionDataFlagIndex = 0
private const val vectorFirstCrankMeasurementAngleFlagIndex = 1
private const val vectorInstantaneousForceArrayFlagIndex = 2
private const val vectorInstantaneousTorqueArrayFlagIndex = 3
private val vectorInstantaneousMeasurementDirectionMask: UByte = 0b110000u.toUByte()
private const val vectorInstantaneousMeasurementDirectionIndex = 4

private enum class WorkoutDeviceCyclingPowerInstantaneousMeasurementDirection(val rawValue: UByte) {
    unknown(0u),
    tangentialComponent(1u),
    radialComponent(2u),
    lateralComponent(3u);

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
        val reader = ByteReader(data = value)
        val flags = reader.readUInt8()
        if (flags.isBitSet(index = vectorCrankRevolutionDataFlagIndex)) {
            cumulativeCrankRevolutions = reader.readUInt16Le()
            lastCrankEventTime = reader.readUInt16Le()
        }
        if (flags.isBitSet(index = vectorFirstCrankMeasurementAngleFlagIndex)) {
            firstCrankMeasurementAngle = reader.readUInt16Le()
        }
        while (reader.bytesAvailable >= 2) {
            val value = reader.readUInt16Le()
            if (flags.isBitSet(index = vectorInstantaneousForceArrayFlagIndex)) {
                if (instantaneousForceMagnitudes == null) {
                    instantaneousForceMagnitudes = mutableListOf()
                }
                instantaneousForceMagnitudes!!.add(value)
            } else if (flags.isBitSet(index = vectorInstantaneousTorqueArrayFlagIndex)) {
                if (instantaneousTorqueMagnitudes == null) {
                    instantaneousTorqueMagnitudes = mutableListOf()
                }
                instantaneousTorqueMagnitudes!!.add(value)
            }
        }
        val value = ((flags and vectorInstantaneousMeasurementDirectionMask).toInt() shr
            vectorInstantaneousMeasurementDirectionIndex).toUByte()
        instantaneousMeasurementDirection =
            WorkoutDeviceCyclingPowerInstantaneousMeasurementDirection.fromRawValue(value)
    }
}

class WorkoutDeviceCyclingPower {
    private var measurementCharacteristic: CBCharacteristic? = null
    private val averagePower = WorkoutDeviceAverageCalculator()
    private val crankCadence = WorkoutDeviceCrankCadence()

    fun reset() {
        measurementCharacteristic = null
        resetMeasurements()
    }

    fun resetMeasurements() {
        crankCadence.reset()
    }

    fun setMeasurementCharacteristic(characteristic: CBCharacteristic) {
        measurementCharacteristic = characteristic
    }

    fun isAnyCharacteristicDiscovered(): Boolean {
        return measurementCharacteristic != null
    }

    fun handleMeasurement(value: ByteArray): Pair<Int, Int?> {
        val measurement = PowerMeasurement(value)
        averagePower.update(value = measurement.instantaneousPower.toDouble())
        val cadence = crankCadence.update(revolutions = measurement.cumulativeCrankRevolutions,
            time = measurement.lastCrankEventTime,
            now = ContinuousClock.now)
        return Pair(averagePower.average().toInt(), cadence)
    }

    fun handlePowerVector(value: ByteArray) {
        PowerVector(value)
    }
}

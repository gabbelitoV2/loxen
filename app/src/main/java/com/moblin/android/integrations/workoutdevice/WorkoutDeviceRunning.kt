package com.moblin.android.integrations.workoutdevice

import com.moblin.android.common.various.isBitSet
import com.moblin.android.media.haishinkit.util.ByteReader
import com.moblin.android.platform.core.ContinuousClock
import com.moblin.android.platform.corebluetooth.CBCharacteristic
import com.moblin.android.platform.corebluetooth.CBUUID
import kotlinx.serialization.Serializable
import kotlin.time.DurationUnit

val workoutDeviceRunningServiceId = CBUUID(string = "1814")
val workoutDeviceRunningMeasurementCharacteristicId = CBUUID(string = "2A53")

private const val rscStrideLengthFlagIndex = 0
private const val rscTotalDistanceFlagIndex = 1

@Serializable
data class WorkoutDeviceRunningMetrics(
    var speed: Double? = null,
    var cadence: Int? = null,
    var distance: Double? = null,
)

private data class RscMeasurement private constructor(
    val speedMetersPerSecond: Double,
    val cadence: Int,
    val totalDistanceMeters: Double?,
) {
    companion object {
        fun create(value: ByteArray): RscMeasurement {
            val reader = ByteReader(value)
            val flags = reader.readUInt8()
            val speedRaw = reader.readUInt16Le()
            val speedMetersPerSecond = speedRaw.toDouble() / 256.0
            val cadence = reader.readUInt8().toInt()
            if (flags.isBitSet(index = rscStrideLengthFlagIndex)) {
                reader.readUInt16Le()
            }
            var totalDistanceMeters: Double? = null
            if (flags.isBitSet(index = rscTotalDistanceFlagIndex)) {
                val totalDistanceRaw = reader.readUInt32Le()
                totalDistanceMeters = totalDistanceRaw.toDouble() / 10.0
            }
            return RscMeasurement(speedMetersPerSecond, cadence, totalDistanceMeters)
        }
    }
}

open class WorkoutDeviceRunning {
    private var measurementCharacteristic: CBCharacteristic? = null
    private var lastRscUpdateTime: ContinuousClock.Instant? = null
    private var distanceMetersFallback = 0.0
    private var usingDeviceDistance = false

    open fun reset() {
        measurementCharacteristic = null
        resetMeasurements()
        distanceMetersFallback = 0.0
        usingDeviceDistance = false
    }

    open fun resetMeasurements() {
        lastRscUpdateTime = null
    }

    open fun setMeasurementCharacteristic(characteristic: CBCharacteristic) {
        measurementCharacteristic = characteristic
    }

    open fun isAnyCharacteristicDiscovered(): Boolean {
        return measurementCharacteristic != null
    }

    open fun handleMeasurement(value: ByteArray): WorkoutDeviceRunningMetrics {
        val measurement = RscMeasurement.create(value)
        var distanceMeters: Double?
        val totalDistanceMeters = measurement.totalDistanceMeters
        if (totalDistanceMeters != null) {
            usingDeviceDistance = true
            distanceMeters = totalDistanceMeters
        } else if (!usingDeviceDistance) {
            val now = ContinuousClock.now
            val lastRscUpdateTime = lastRscUpdateTime
            if (lastRscUpdateTime != null) {
                val deltaSeconds = lastRscUpdateTime.duration(to = now).toDouble(DurationUnit.SECONDS)
                if (deltaSeconds > 0) {
                    distanceMetersFallback += measurement.speedMetersPerSecond * deltaSeconds
                }
            }
            distanceMeters = distanceMetersFallback
            this.lastRscUpdateTime = now
        } else {
            distanceMeters = null
        }
        return WorkoutDeviceRunningMetrics(speed = measurement.speedMetersPerSecond,
                                           cadence = measurement.cadence,
                                           distance = distanceMeters)
    }
}

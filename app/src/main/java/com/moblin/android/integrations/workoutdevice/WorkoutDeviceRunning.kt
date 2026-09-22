package com.moblin.android.integrations.workoutdevice

import android.bluetooth.BluetoothGattCharacteristic
import android.os.SystemClock
import com.moblin.android.media.haishinkit.util.ByteReader
import java.io.IOException
import java.util.UUID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

val workoutDeviceRunningServiceId: UUID = UUID.fromString("00001814-0000-1000-8000-00805f9b34fb")
val workoutDeviceRunningMeasurementCharacteristicId: UUID = UUID.fromString("00002a53-0000-1000-8000-00805f9b34fb")

private const val rscStrideLengthFlagIndex = 0
private const val rscTotalDistanceFlagIndex = 1

private fun UByte.isBitSet(index: Int): Boolean = ((this.toInt() shr index) and 0x01) == 0x01

@Serializable
data class WorkoutDeviceRunningMetrics(
    @SerialName("speed") var speed: Double? = null,
    @SerialName("cadence") var cadence: Int? = null,
    @SerialName("distance") var distance: Double? = null,
)

private class RscMeasurement @Throws(IOException::class) constructor(value: ByteArray) {
    val speedMetersPerSecond: Double
    val cadence: Int
    var totalDistanceMeters: Double? = null

    init {
        val reader = ByteReader(value)
        val flags = reader.readUInt8()
        val speedRaw = reader.readUInt16Le()
        speedMetersPerSecond = speedRaw.toDouble() / 256.0
        cadence = reader.readUInt8().toInt()
        if (flags.isBitSet(rscStrideLengthFlagIndex)) {
            reader.readUInt16Le()
        }
        if (flags.isBitSet(rscTotalDistanceFlagIndex)) {
            val totalDistanceRaw = reader.readUInt32Le()
            totalDistanceMeters = totalDistanceRaw.toDouble() / 10.0
        }
    }
}

class WorkoutDeviceRunning {
    private var measurementCharacteristic: BluetoothGattCharacteristic? = null
    private var lastRscUpdateTime: Long? = null
    private var distanceMetersFallback: Double = 0.0
    private var usingDeviceDistance: Boolean = false

    fun reset() {
        measurementCharacteristic = null
        resetMeasurements()
        distanceMetersFallback = 0.0
        usingDeviceDistance = false
    }

    fun resetMeasurements() {
        lastRscUpdateTime = null
    }

    fun setMeasurementCharacteristic(characteristic: BluetoothGattCharacteristic) {
        measurementCharacteristic = characteristic
    }

    fun isAnyCharacteristicDiscovered(): Boolean = measurementCharacteristic != null

    @Throws(IOException::class)
    fun handleMeasurement(value: ByteArray): WorkoutDeviceRunningMetrics {
        val measurement = RscMeasurement(value)
        var distanceMeters: Double? = null
        val totalDistanceMeters = measurement.totalDistanceMeters
        if (totalDistanceMeters != null) {
            usingDeviceDistance = true
            distanceMeters = totalDistanceMeters
        } else if (!usingDeviceDistance) {
            val now = SystemClock.elapsedRealtimeNanos()
            val last = lastRscUpdateTime
            if (last != null) {
                val deltaSeconds = (now - last) / 1_000_000_000.0
                if (deltaSeconds > 0.0) {
                    distanceMetersFallback += measurement.speedMetersPerSecond * deltaSeconds
                }
            }
            distanceMeters = distanceMetersFallback
            lastRscUpdateTime = now
        }
        return WorkoutDeviceRunningMetrics(
            speed = measurement.speedMetersPerSecond,
            cadence = measurement.cadence,
            distance = distanceMeters,
        )
    }
}

package com.moblin.android.integrations.workoutdevice

import android.bluetooth.BluetoothGattCharacteristic
import com.moblin.android.media.haishinkit.util.ByteReader
import java.util.UUID

val workoutDeviceHeartRateServiceId: UUID = UUID.fromString("0000180D-0000-1000-8000-00805F9B34FB")
val workoutDeviceHeartRateMeasurementCharacteristicId: UUID = UUID.fromString("00002A37-0000-1000-8000-00805F9B34FB")
private const val measurementHeartRateValueFormatIndex = 0

private class HeartRateMeasurement(value: ByteArray) {
    var heartRate: UShort = 0u

    init {
        val reader = ByteReader(data = value)
        val flags = reader.readUInt8()
        if ((flags.toInt() and (1 shl measurementHeartRateValueFormatIndex)) != 0) {
            heartRate = reader.readUInt16Le()
        } else {
            heartRate = reader.readUInt8().toUShort()
        }
    }
}

class WorkoutDeviceHeartRate {
    private var measurementCharacteristic: BluetoothGattCharacteristic? = null

    fun reset() {
        measurementCharacteristic = null
    }

    fun setMeasurementCharacteristic(characteristic: BluetoothGattCharacteristic) {
        measurementCharacteristic = characteristic
    }

    fun isAnyCharacteristicDiscovered(): Boolean = measurementCharacteristic != null

    fun handleMeasurement(value: ByteArray): Int {
        val measurement = HeartRateMeasurement(value)
        return measurement.heartRate.toInt()
    }
}

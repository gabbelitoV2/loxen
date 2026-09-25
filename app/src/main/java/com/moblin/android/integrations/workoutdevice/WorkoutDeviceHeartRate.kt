package com.moblin.android.integrations.workoutdevice

import com.moblin.android.common.various.isBitSet
import com.moblin.android.media.haishinkit.util.ByteReader
import com.moblin.android.platform.corebluetooth.CBCharacteristic
import com.moblin.android.platform.corebluetooth.CBUUID

val workoutDeviceHeartRateServiceId = CBUUID(string = "180D")
val workoutDeviceHeartRateMeasurementCharacteristicId = CBUUID(string = "2A37")
private const val measurementHeartRateValueFormatIndex = 0

private class HeartRateMeasurement {
    var heartRate: UShort = 0u

    constructor(value: ByteArray) {
        val reader = ByteReader(value)
        val flags = reader.readUInt8()
        if (flags.isBitSet(index = measurementHeartRateValueFormatIndex)) {
            heartRate = reader.readUInt16Le()
        } else {
            heartRate = reader.readUInt8().toUShort()
        }
    }
}

open class WorkoutDeviceHeartRate {
    private var measurementCharacteristic: CBCharacteristic? = null

    open fun reset() {
        measurementCharacteristic = null
    }

    open fun setMeasurementCharacteristic(characteristic: CBCharacteristic) {
        measurementCharacteristic = characteristic
    }

    open fun isAnyCharacteristicDiscovered(): Boolean {
        return measurementCharacteristic != null
    }

    open fun handleMeasurement(value: ByteArray): Int {
        val measurement = HeartRateMeasurement(value)
        return measurement.heartRate.toInt()
    }
}

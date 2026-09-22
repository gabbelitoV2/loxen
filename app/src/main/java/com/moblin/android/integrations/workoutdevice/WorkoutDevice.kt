package com.moblin.android.integrations.workoutdevice

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.util.Log
import com.moblin.android.various.BluetoothScanner
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val tag = "WorkoutDevice"

private val dispatchQueue = CoroutineScope(Dispatchers.IO)

val workoutDeviceScanner = BluetoothScanner(
    serviceIds = listOf(
        workoutDeviceHeartRateServiceId,
        workoutDeviceCyclingPowerServiceId,
        workoutDeviceCyclingSpeedCadenceServiceId,
        workoutDeviceRunningServiceId,
    )
)

interface WorkoutDeviceDelegate {
    fun workoutDeviceState(device: WorkoutDevice, state: WorkoutDeviceState)

    fun workoutDeviceHeartRate(device: WorkoutDevice, heartRate: Int)

    fun workoutDeviceCyclingPower(device: WorkoutDevice, power: Int, cadence: Int?)

    fun workoutDeviceCyclingSpeedCadence(device: WorkoutDevice, speed: Double?, cadence: Int?)

    fun workoutDeviceRunningMetrics(device: WorkoutDevice, metrics: WorkoutDeviceRunningMetrics)
}

enum class WorkoutDeviceState {
    disconnected,
    discovering,
    connecting,
    connected,
}

@SuppressLint("MissingPermission")
class WorkoutDevice(wheelCircumference: Int) : BluetoothGattCallback() {
    private var state = WorkoutDeviceState.disconnected
    private var centralManager: BluetoothAdapter? = null
    private var peripheral: BluetoothGatt? = null
    private val heartRate = WorkoutDeviceHeartRate()
    private val cyclingPower = WorkoutDeviceCyclingPower()
    private val cyclingSpeedCadence = WorkoutDeviceCyclingSpeedCadence(wheelCircumference)
    private val running = WorkoutDeviceRunning()
    private var deviceId: UUID? = null
    var delegate: WorkoutDeviceDelegate? = null

    fun start(deviceId: UUID?) {
        dispatchQueue.launch {
            startInternal(deviceId)
        }
    }

    fun setWheelCircumference(millimeters: Int) {
        dispatchQueue.launch {
            cyclingSpeedCadence.setWheelCircumference(millimeters)
        }
    }

    fun stop() {
        dispatchQueue.launch {
            stopInternal()
        }
    }

    fun getState(): WorkoutDeviceState = state

    private fun startInternal(deviceId: UUID?) {
        this.deviceId = deviceId
        reset()
        reconnect()
    }

    private fun stopInternal() {
        reset()
    }

    private fun reset() {
        centralManager = null
        peripheral?.close()
        peripheral = null
        heartRate.reset()
        cyclingPower.reset()
        cyclingSpeedCadence.reset()
        running.reset()
        setState(WorkoutDeviceState.disconnected)
    }

    private fun resetMeasurements() {
        cyclingPower.resetMeasurements()
        cyclingSpeedCadence.resetMeasurements()
        running.resetMeasurements()
    }

    @Suppress("DEPRECATION")
    private fun reconnect() {
        peripheral = null
        resetMeasurements()
        setState(WorkoutDeviceState.discovering)
        centralManager = BluetoothAdapter.getDefaultAdapter()
        centralManager?.let { centralManagerDidUpdateState(it) }
    }

    private fun setState(state: WorkoutDeviceState) {
        if (state == this.state) {
            return
        }
        Log.d(tag, "workout-device: State change ${this.state} -> $state")
        this.state = state
        delegate?.workoutDeviceState(this, state)
    }

    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            centralManagerDidFailToConnect()
            return
        }
        when (newState) {
            BluetoothProfile.STATE_CONNECTED -> centralManagerDidConnect(gatt)
            BluetoothProfile.STATE_DISCONNECTED -> centralManagerDidDisconnectPeripheral()
            else -> {}
        }
    }

    private fun centralManagerDidUpdateState(central: BluetoothAdapter) {
        if (central.isEnabled) {
            connect(central)
        }
    }

    private fun connect(central: BluetoothAdapter) {
        val deviceId = this.deviceId
        if (deviceId == null) {
            Log.i(tag, "workout-device: Device not found")
            return
        }
        val device = runCatching { central.getRemoteDevice(deviceId.toString()) }.getOrNull()
        if (device == null) {
            Log.i(tag, "workout-device: Device not found")
            return
        }
        peripheral = device.connectGatt(
            TODO("BluetoothDevice.connectGatt needs an Android Context supplied by the Activity layer"),
            false,
            this,
        )
        setState(WorkoutDeviceState.connecting)
    }

    private fun centralManagerDidFailToConnect() {
    }

    private fun centralManagerDidConnect(peripheral: BluetoothGatt) {
        peripheral.discoverServices()
    }

    private fun centralManagerDidDisconnectPeripheral() {
        reconnect()
    }

    private fun isAnyCharacteristicDiscovered(): Boolean {
        if (heartRate.isAnyCharacteristicDiscovered()) {
            return true
        }
        if (cyclingPower.isAnyCharacteristicDiscovered()) {
            return true
        }
        if (cyclingSpeedCadence.isAnyCharacteristicDiscovered()) {
            return true
        }
        if (running.isAnyCharacteristicDiscovered()) {
            return true
        }
        return false
    }

    private fun handleHeartRateMeasurement(value: ByteArray) {
        val heartRate = heartRate.handleMeasurement(value)
        delegate?.workoutDeviceHeartRate(this, heartRate)
    }

    private fun handleCyclingPowerMeasurement(value: ByteArray) {
        val (power, cadence) = cyclingPower.handleMeasurement(value)
        delegate?.workoutDeviceCyclingPower(this, power, cadence)
    }

    private fun handleCyclingSpeedCadenceMeasurement(value: ByteArray) {
        val (speed, cadence) = cyclingSpeedCadence.handleMeasurement(value)
        delegate?.workoutDeviceCyclingSpeedCadence(this, speed, cadence)
    }

    private fun handleCyclingPowerVector(value: ByteArray) {
        cyclingPower.handlePowerVector(value)
    }

    private fun handleRunningMeasurement(value: ByteArray) {
        val metrics = running.handleMeasurement(value)
        delegate?.workoutDeviceRunningMetrics(this, metrics)
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            return
        }
        peripheralDidDiscoverServices(gatt)
    }

    private fun peripheralDidDiscoverServices(peripheral: BluetoothGatt) {
        val services = peripheral.services ?: return
        for (service in services) {
            when (service.uuid) {
                workoutDeviceHeartRateServiceId,
                workoutDeviceCyclingPowerServiceId,
                workoutDeviceCyclingSpeedCadenceServiceId,
                workoutDeviceRunningServiceId -> peripheralDidDiscoverCharacteristicsFor(service)

                else -> {}
            }
        }
    }

    private fun peripheralDidDiscoverCharacteristicsFor(service: BluetoothGattService) {
        for (characteristic in service.characteristics ?: emptyList()) {
            when (characteristic.uuid) {
                workoutDeviceHeartRateMeasurementCharacteristicId -> {
                    heartRate.setMeasurementCharacteristic(characteristic)
                    peripheral?.setCharacteristicNotification(characteristic, true)
                }

                workoutDeviceCyclingPowerMeasurementCharacteristicId -> {
                    cyclingPower.setMeasurementCharacteristic(characteristic)
                    peripheral?.setCharacteristicNotification(characteristic, true)
                }

                workoutDeviceCyclingSpeedCadenceMeasurementCharacteristicId -> {
                    cyclingSpeedCadence.setMeasurementCharacteristic(characteristic)
                    peripheral?.setCharacteristicNotification(characteristic, true)
                }

                workoutDeviceRunningMeasurementCharacteristicId -> {
                    running.setMeasurementCharacteristic(characteristic)
                    peripheral?.setCharacteristicNotification(characteristic, true)
                }

                else -> {}
            }
        }
        if (isAnyCharacteristicDiscovered()) {
            setState(WorkoutDeviceState.connected)
        }
    }

    @Suppress("DEPRECATION")
    override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        peripheralDidUpdateValueFor(characteristic, characteristic.value)
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
    ) {
        peripheralDidUpdateValueFor(characteristic, value)
    }

    private fun peripheralDidUpdateValueFor(
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray?,
    ) {
        if (value == null) {
            return
        }
        try {
            when (characteristic.uuid) {
                workoutDeviceHeartRateMeasurementCharacteristicId -> handleHeartRateMeasurement(value)

                workoutDeviceCyclingPowerMeasurementCharacteristicId -> handleCyclingPowerMeasurement(value)

                workoutDeviceCyclingPowerVectorCharacteristicId -> handleCyclingPowerVector(value)

                workoutDeviceCyclingSpeedCadenceMeasurementCharacteristicId -> {
                    handleCyclingSpeedCadenceMeasurement(value)
                }

                workoutDeviceRunningMeasurementCharacteristicId -> handleRunningMeasurement(value)

                else -> {}
            }
        } catch (error: Exception) {
            Log.i(
                tag,
                "workout-device: Characteristic ${characteristic.uuid}, " +
                    "value ${value.joinToString("") { "%02x".format(it) }}: Error $error",
            )
        }
    }
}

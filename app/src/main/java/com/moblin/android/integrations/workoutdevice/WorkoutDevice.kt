package com.moblin.android.integrations.workoutdevice

import android.util.Log
import com.moblin.android.common.various.hexString
import com.moblin.android.platform.corebluetooth.CBCentralManager
import com.moblin.android.platform.corebluetooth.CBCentralManagerDelegate
import com.moblin.android.platform.corebluetooth.CBCharacteristic
import com.moblin.android.platform.corebluetooth.CBManagerState
import com.moblin.android.platform.corebluetooth.CBPeripheral
import com.moblin.android.platform.corebluetooth.CBPeripheralDelegate
import com.moblin.android.platform.corebluetooth.CBService
import com.moblin.android.various.BluetoothScanner
import java.util.UUID
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch

private const val logTag = "WorkoutDevice"

private val dispatchQueue = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())

val workoutDeviceScanner = BluetoothScanner(serviceIds = listOf(
    workoutDeviceHeartRateServiceId,
    workoutDeviceCyclingPowerServiceId,
    workoutDeviceCyclingSpeedCadenceServiceId,
    workoutDeviceRunningServiceId,
))

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

open class WorkoutDevice(wheelCircumference: Int) : CBCentralManagerDelegate, CBPeripheralDelegate {
    private var state: WorkoutDeviceState = WorkoutDeviceState.disconnected
    private var centralManager: CBCentralManager? by CBCentralManager.holder()
    private var peripheral: CBPeripheral? = null
    private val heartRate = WorkoutDeviceHeartRate()
    private val cyclingPower = WorkoutDeviceCyclingPower()
    private val cyclingSpeedCadence =
        WorkoutDeviceCyclingSpeedCadence(wheelCircumference = wheelCircumference)
    private val running = WorkoutDeviceRunning()
    private var deviceId: UUID? = null
    open var delegate: WorkoutDeviceDelegate? = null

    open fun start(deviceId: UUID?) {
        dispatchQueue.launch {
            startInternal(deviceId = deviceId)
        }
    }

    open fun setWheelCircumference(millimeters: Int) {
        dispatchQueue.launch {
            cyclingSpeedCadence.setWheelCircumference(millimeters = millimeters)
        }
    }

    open fun stop() {
        dispatchQueue.launch {
            stopInternal()
        }
    }

    open fun getState(): WorkoutDeviceState {
        return state
    }

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
        peripheral = null
        heartRate.reset()
        cyclingPower.reset()
        cyclingSpeedCadence.reset()
        running.reset()
        setState(state = WorkoutDeviceState.disconnected)
    }

    private fun resetMeasurements() {
        cyclingPower.resetMeasurements()
        cyclingSpeedCadence.resetMeasurements()
        running.resetMeasurements()
    }

    private fun reconnect() {
        peripheral = null
        resetMeasurements()
        setState(state = WorkoutDeviceState.discovering)
        centralManager = CBCentralManager(delegate = this, queue = dispatchQueue)
    }

    private fun setState(state: WorkoutDeviceState) {
        if (state == this.state) {
            return
        }
        Log.d(logTag, "workout-device: State change ${this.state} -> $state")
        this.state = state
        delegate?.workoutDeviceState(this, state = state)
    }

    override fun centralManagerDidUpdateState(central: CBCentralManager) {
        when (central.state) {
            CBManagerState.poweredOn -> connect(central)
            else -> {}
        }
    }

    private fun connect(central: CBCentralManager) {
        val deviceId = this.deviceId
        val peripheral = deviceId?.let {
            central.retrievePeripherals(withIdentifiers = listOf(it)).firstOrNull()
        }
        if (peripheral == null) {
            Log.i(logTag, "workout-device: Device not found")
            return
        }
        this.peripheral = peripheral
        peripheral.delegate = this
        central.connect(peripheral)
        setState(state = WorkoutDeviceState.connecting)
    }

    override fun centralManagerDidFailToConnect(
        central: CBCentralManager,
        peripheral: CBPeripheral,
        error: Throwable?,
    ) {
    }

    override fun centralManagerDidConnect(central: CBCentralManager, peripheral: CBPeripheral) {
        peripheral.discoverServices(null)
    }

    override fun centralManagerDidDisconnectPeripheral(
        central: CBCentralManager,
        peripheral: CBPeripheral,
        error: Throwable?,
    ) {
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
        delegate?.workoutDeviceHeartRate(this, heartRate = heartRate.handleMeasurement(value = value))
    }

    private fun handleCyclingPowerMeasurement(value: ByteArray) {
        val (power, cadence) = cyclingPower.handleMeasurement(value = value)
        delegate?.workoutDeviceCyclingPower(this, power = power, cadence = cadence)
    }

    private fun handleCyclingSpeedCadenceMeasurement(value: ByteArray) {
        val (speed, cadence) = cyclingSpeedCadence.handleMeasurement(value = value)
        delegate?.workoutDeviceCyclingSpeedCadence(this, speed = speed, cadence = cadence)
    }

    private fun handleCyclingPowerVector(value: ByteArray) {
        cyclingPower.handlePowerVector(value = value)
    }

    private fun handleRunningMeasurement(value: ByteArray) {
        val metrics = running.handleMeasurement(value = value)
        delegate?.workoutDeviceRunningMetrics(this, metrics = metrics)
    }

    override fun peripheralDidDiscoverServices(peripheral: CBPeripheral, error: Throwable?) {
        val services = peripheral.services ?: return
        val heartRateService = services.firstOrNull { it.uuid == workoutDeviceHeartRateServiceId }
        if (heartRateService != null) {
            peripheral.discoverCharacteristics(null, `for` = heartRateService)
        }
        val cyclingPowerService =
            services.firstOrNull { it.uuid == workoutDeviceCyclingPowerServiceId }
        if (cyclingPowerService != null) {
            peripheral.discoverCharacteristics(null, `for` = cyclingPowerService)
        }
        val cyclingSpeedCadenceService =
            services.firstOrNull { it.uuid == workoutDeviceCyclingSpeedCadenceServiceId }
        if (cyclingSpeedCadenceService != null) {
            peripheral.discoverCharacteristics(null, `for` = cyclingSpeedCadenceService)
        }
        val runningService = services.firstOrNull { it.uuid == workoutDeviceRunningServiceId }
        if (runningService != null) {
            peripheral.discoverCharacteristics(null, `for` = runningService)
        }
    }

    override fun peripheralDidDiscoverCharacteristicsFor(
        peripheralArg: CBPeripheral,
        service: CBService,
        error: Throwable?,
    ) {
        for (characteristic in service.characteristics.orEmpty()) {
            when (characteristic.uuid) {
                workoutDeviceHeartRateMeasurementCharacteristicId -> {
                    heartRate.setMeasurementCharacteristic(characteristic)
                    peripheral?.setNotifyValue(true, `for` = characteristic)
                }
                workoutDeviceCyclingPowerMeasurementCharacteristicId -> {
                    cyclingPower.setMeasurementCharacteristic(characteristic)
                    peripheral?.setNotifyValue(true, `for` = characteristic)
                }
                workoutDeviceCyclingSpeedCadenceMeasurementCharacteristicId -> {
                    cyclingSpeedCadence.setMeasurementCharacteristic(characteristic)
                    peripheral?.setNotifyValue(true, `for` = characteristic)
                }
                workoutDeviceRunningMeasurementCharacteristicId -> {
                    running.setMeasurementCharacteristic(characteristic)
                    peripheral?.setNotifyValue(true, `for` = characteristic)
                }
                else -> {}
            }
        }
        if (isAnyCharacteristicDiscovered()) {
            setState(state = WorkoutDeviceState.connected)
        }
    }

    override fun peripheralDidUpdateValueFor(
        peripheral: CBPeripheral,
        characteristic: CBCharacteristic,
        error: Throwable?,
    ) {
        val value = characteristic.value ?: return
        try {
            when (characteristic.uuid) {
                workoutDeviceHeartRateMeasurementCharacteristicId ->
                    handleHeartRateMeasurement(value = value)
                workoutDeviceCyclingPowerMeasurementCharacteristicId ->
                    handleCyclingPowerMeasurement(value = value)
                workoutDeviceCyclingPowerVectorCharacteristicId ->
                    handleCyclingPowerVector(value = value)
                workoutDeviceCyclingSpeedCadenceMeasurementCharacteristicId ->
                    handleCyclingSpeedCadenceMeasurement(value = value)
                workoutDeviceRunningMeasurementCharacteristicId ->
                    handleRunningMeasurement(value = value)
                else -> {}
            }
        } catch (error: Throwable) {
            Log.i(
                logTag,
                "workout-device: Characteristic ${characteristic.uuid.uuidString}, " +
                    "value ${value.hexString()}: Error $error",
            )
        }
    }
}

package com.moblin.android.integrations.blacksharkcooler

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.util.Log
import com.moblin.android.common.various.RgbColor
import com.moblin.android.various.BluetoothScanner
import com.moblin.android.various.SimpleTimer
import com.moblin.android.various.storages.ThermalState
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.moblin.android.AppDelegate

private const val TAG = "BlackSharkCoolerDevice"

private val blackSharkCoolerDeviceDispatchQueue =
    CoroutineScope(SupervisorJob() + Dispatchers.IO)

private val clientCharacteristicConfigUuid: UUID =
    UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

interface BlackSharkCoolerDeviceDelegate {
    fun blackSharkCoolerDeviceState(device: BlackSharkCoolerDevice, state: BlackSharkCoolerDeviceState)
    fun blackSharkCoolerDeviceStatus(device: BlackSharkCoolerDevice, status: BlackSharkLib.CoolingState)
}

enum class BlackSharkCoolerDeviceState {
    DISCONNECTED,
    DISCOVERING,
    CONNECTING,
    CONNECTED,
}

private val blackSharkCoolerServiceId: UUID by lazy { BlackSharkLib.getServiceUUID() }

val blackSharkCoolerScanner = BluetoothScanner(
    context = AppDelegate.context,
    serviceIds = emptyList(),
)

class BlackSharkCoolerDevice(private val context: Context) {
    private var state: BlackSharkCoolerDeviceState = BlackSharkCoolerDeviceState.DISCONNECTED
    private var centralManager: BluetoothAdapter? = null
    private var peripheral: BluetoothDevice? = null
    private var deviceId: UUID? = null
    private var readCharacteristic: BluetoothGattCharacteristic? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var latestTransmissionTime: Long = System.currentTimeMillis()
    private var model: BlackSharkLib.Model? = null
    private var coolingStatsTimer = SimpleTimer(Dispatchers.IO)
    private var coolingPower: Int? = null
    private var fanSpeed: Int? = null
    var delegate: BlackSharkCoolerDeviceDelegate? = null
    private var bluetoothGatt: BluetoothGatt? = null

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        didConnect(gatt.device)
                    } else {
                        didFailToConnect(gatt.device, null)
                    }
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    didDisconnectPeripheral(gatt.device, null)
                }

                else -> {}
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            didDiscoverServices(gatt, null)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            didUpdateValueFor(characteristic, null)
        }
    }

    fun start(deviceId: UUID?) {
        blackSharkCoolerDeviceDispatchQueue.launch {
            startInternal(deviceId)
        }
    }

    fun stop() {
        blackSharkCoolerDeviceDispatchQueue.launch {
            stopInternal()
        }
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
        bluetoothGatt?.close()
        bluetoothGatt = null
        centralManager = null
        peripheral = null
        readCharacteristic = null
        writeCharacteristic = null
        coolingStatsTimer.stop()
        setState(BlackSharkCoolerDeviceState.DISCONNECTED)
    }

    private fun reconnect() {
        peripheral = null
        setState(BlackSharkCoolerDeviceState.DISCOVERING)
        val central = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
        centralManager = central
        if (central != null) {
            centralManagerDidUpdateState(central)
        }
    }

    private fun setState(state: BlackSharkCoolerDeviceState) {
        if (state == this.state) {
            return
        }
        Log.d(TAG, "black-shark-cooler-device: State change ${this.state} -> $state")
        this.state = state
        delegate?.blackSharkCoolerDeviceState(this, state)
    }

    fun centralManagerDidUpdateState(central: BluetoothAdapter) {
        when (central.state) {
            BluetoothAdapter.STATE_ON -> connect(central)
            else -> {}
        }
    }

    private fun connect(central: BluetoothAdapter) {
        val deviceId = this.deviceId
        if (deviceId == null) {
            Log.i(TAG, "black-shark-cooler-device: Device not found")
            return
        }
        val peripheral = try {
            central.getRemoteDevice(deviceId.toString())
        } catch (e: IllegalArgumentException) {
            Log.i(TAG, "black-shark-cooler-device: Device not found")
            return
        }
        this.peripheral = peripheral
        bluetoothGatt = peripheral.connectGatt(context, false, gattCallback)
        setState(BlackSharkCoolerDeviceState.CONNECTING)
    }

    fun didFailToConnect(peripheral: BluetoothDevice?, error: Throwable?) {
    }

    fun didConnect(peripheral: BluetoothDevice) {
        model = BlackSharkLib.detectModel(peripheral.name)
        bluetoothGatt?.discoverServices()
    }

    fun didDisconnectPeripheral(peripheral: BluetoothDevice?, error: Throwable?) {
        reconnect()
    }

    fun didDiscoverServices(gatt: BluetoothGatt, error: Throwable?) {
        val service = gatt.services.firstOrNull { it.uuid == blackSharkCoolerServiceId }
        if (service != null) {
            didDiscoverCharacteristicsFor(service, null)
        }
    }

    fun didDiscoverCharacteristicsFor(service: BluetoothGattService, error: Throwable?) {
        for (characteristic in service.characteristics) {
            Log.d(TAG, "black-shark-cooler-device: Characteristic found: ${characteristic.uuid}")
            when (characteristic.uuid) {
                BlackSharkLib.getReadCharacteristicsUUID() -> {
                    readCharacteristic = characteristic
                    setNotifyValue(characteristic, true)
                }

                BlackSharkLib.getWriteCharacteristicsUUID() -> {
                    writeCharacteristic = characteristic
                    pollForCoolingStats()
                    coolingStatsTimer.startPeriodic(2.0) {
                        pollForCoolingStats()
                    }
                }

                else -> {}
            }
        }
        if (readCharacteristic != null) {
            setState(BlackSharkCoolerDeviceState.CONNECTED)
        }
    }

    private fun updatedPercentageScale(current: Int?, target: Int): Int {
        if (current == null) {
            return target
        }
        return when {
            current > target -> maxOf(current - 5, target)
            current < target -> minOf(current + 5, target)
            else -> target
        }
    }

    private fun pollForCoolingStats() {
        val writeCharacteristic = writeCharacteristic ?: return
        val model = model ?: return
        if (bluetoothGatt == null) {
            return
        }
        writeValue(
            writeCharacteristic,
            BlackSharkLib.getCoolingMetadataCommand(model),
        )
    }

    fun adjustCoolerProfilePro4(thermalState: ThermalState) {
        val writeCharacteristic = writeCharacteristic ?: return
        if (bluetoothGatt == null) {
            return
        }
        val model = model ?: return
        val (coolingPowerTarget, fanSpeedTarget) = when (thermalState) {
            ThermalState.NOMINAL -> 0 to 10
            ThermalState.FAIR -> 20 to 20
            ThermalState.SERIOUS -> 80 to 50
            ThermalState.CRITICAL -> 100 to 100
            else -> {
                Log.i(TAG, "black-shark-cooler-device: Thermal state is unknown value ( $thermalState )")
                100 to 100
            }
        }
        val coolingPower = updatedPercentageScale(this.coolingPower, coolingPowerTarget)
        Log.d(TAG, "black-shark-cooler-device (Pro 4): Adjusting cooling power to $coolingPower%")
        writeValue(
            writeCharacteristic,
            BlackSharkLib.getSetCoolingPowerCommand(coolingPower, model)!!,
        )
        val fanSpeed = updatedPercentageScale(this.fanSpeed, fanSpeedTarget)
        Log.d(TAG, "black-shark-cooler-device (Pro 4): Adjusting fan speed to $fanSpeed%")
        writeValue(
            writeCharacteristic,
            BlackSharkLib.getSetFanSpeedCommand(fanSpeed, model)!!,
        )
    }

    fun setCustomModePro5(intensity: Int) {
        val writeCharacteristic = writeCharacteristic ?: return
        if (bluetoothGatt == null) {
            return
        }
        if (intensity == 0) {
            Log.d(TAG, "black-shark-cooler-device (Pro 5): Adjusting cooling power to OFF")
            writeValue(
                writeCharacteristic,
                BlackSharkLib.getSetCoolingEnabledCommand(false, BlackSharkLib.Model.PRO5)!!,
            )
            return
        }
        val coolingPower = this.coolingPower
        if (coolingPower != null && coolingPower == 2) {
            Log.d(TAG, "black-shark-cooler-device (Pro 5): Enabling custom mode for cooler.")
            writeValue(
                writeCharacteristic,
                BlackSharkLib.getSetCoolingEnabledCommand(true, BlackSharkLib.Model.PRO5)!!,
            )
        }
        Log.d(TAG, "black-shark-cooler-device (Pro 5): Adjusting cooling power to $intensity")
        writeValue(
            writeCharacteristic,
            BlackSharkLib.getSetCustomModeCommand(intensity, BlackSharkLib.Model.PRO5)!!,
        )
    }

    fun adjustCoolerProfilePro5(thermalState: ThermalState) {
        when (thermalState) {
            ThermalState.NOMINAL -> setCustomModePro5(0)
            ThermalState.FAIR -> setCustomModePro5(1)
            ThermalState.SERIOUS -> setCustomModePro5(2)
            ThermalState.CRITICAL -> setCustomModePro5(5)
            else -> setCustomModePro5(5)
        }
    }

    fun adjustCoolerProfile() {
        val model = model ?: return
        val thermalState = currentThermalState()
        when (model) {
            BlackSharkLib.Model.PRO4 -> adjustCoolerProfilePro4(thermalState)
            BlackSharkLib.Model.PRO5 -> adjustCoolerProfilePro5(thermalState)
        }
    }

    fun setLedColor(color: RgbColor, brightness: Int) {
        val model = model ?: return
        val now = System.currentTimeMillis()
        if (now - latestTransmissionTime < 80) {
            return
        }
        latestTransmissionTime = now
        val setColorCommand = BlackSharkLib.getSetLEDColorCommand(
            color.red,
            color.green,
            color.blue,
            brightness,
            model,
        ) ?: return
        val writeCharacteristic = writeCharacteristic ?: return
        writeValue(writeCharacteristic, setColorCommand)
    }

    fun turnLedOff() {
        val writeCharacteristic = writeCharacteristic ?: return
        val model = model ?: return
        if (bluetoothGatt == null) {
            return
        }
        writeValue(
            writeCharacteristic,
            BlackSharkLib.getTurnOffLEDCommand(model),
        )
    }

    fun didUpdateValueFor(characteristic: BluetoothGattCharacteristic, error: Throwable?) {
        val value = characteristic.value ?: return
        val model = model ?: return
        when (characteristic.uuid) {
            BlackSharkLib.getReadCharacteristicsUUID() -> {
                val message = BlackSharkLib.parseMessages(value)
                if (message is BlackSharkLib.CoolingState) {
                    delegate?.blackSharkCoolerDeviceStatus(this, message)
                    when (model) {
                        BlackSharkLib.Model.PRO4 -> Log.d(
                            TAG,
                            "black-shark-cooler-device (Pro 4): CoolerTemp: ${message.phoneTemperature}, " +
                                "Heatsink: ${message.heatsinkTemperature}",
                        )

                        BlackSharkLib.Model.PRO5 -> Log.d(
                            TAG,
                            "black-shark-cooler-device (Pro 5): CoolerTemp: ${message.phoneTemperature}, " +
                                "Heatsink: ${message.heatsinkTemperature}, " +
                                "Fan: ${message.fanRPM?.toString() ?: "n/a"}, " +
                                "Power: ${message.powerLevel?.toString() ?: "n/a"}",
                        )
                    }
                    adjustCoolerProfile()
                } else if (message is BlackSharkLib.UnknownMessage) {
                    Log.d(
                        TAG,
                        "black-shark-cooler-device: Got unknown message " +
                            message.rawData.joinToString("") { "%02x".format(it) },
                    )
                }
            }

            else -> {}
        }
    }

    private fun setNotifyValue(characteristic: BluetoothGattCharacteristic, enabled: Boolean) {
        val gatt = bluetoothGatt ?: return
        gatt.setCharacteristicNotification(characteristic, enabled)
        val descriptor = characteristic.getDescriptor(clientCharacteristicConfigUuid) ?: return
        descriptor.value = if (enabled) {
            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        } else {
            BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
        }
        gatt.writeDescriptor(descriptor)
    }

    private fun writeValue(characteristic: BluetoothGattCharacteristic, value: ByteArray): Boolean {
        val gatt = bluetoothGatt ?: return false
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        characteristic.value = value
        return gatt.writeCharacteristic(characteristic)
    }

    private fun currentThermalState(): ThermalState {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return ThermalState.NOMINAL
        }
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            ?: return ThermalState.NOMINAL
        return when (powerManager.currentThermalStatus) {
            PowerManager.THERMAL_STATUS_NONE -> ThermalState.NOMINAL
            PowerManager.THERMAL_STATUS_LIGHT -> ThermalState.FAIR
            PowerManager.THERMAL_STATUS_MODERATE -> ThermalState.SERIOUS
            PowerManager.THERMAL_STATUS_SEVERE -> ThermalState.SERIOUS
            PowerManager.THERMAL_STATUS_CRITICAL -> ThermalState.CRITICAL
            else -> ThermalState.NOMINAL
        }
    }
}

object BlackSharkLib {
    enum class Model {
        PRO4,
        PRO5,
    }

    data class CoolingState(
        val phoneTemperature: Int,
        val heatsinkTemperature: Int,
        val fanRPM: Int? = null,
        val powerLevel: Int? = null,
    )

    class UnknownMessage(val rawData: ByteArray)

    fun getServiceUUID(): UUID = TODO("no Android counterpart for BlackSharkLib")

    fun getReadCharacteristicsUUID(): UUID = TODO("no Android counterpart for BlackSharkLib")

    fun getWriteCharacteristicsUUID(): UUID = TODO("no Android counterpart for BlackSharkLib")

    fun detectModel(advertisedName: String?): Model? = null

    fun getCoolingMetadataCommand(model: Model): ByteArray =
        ByteArray(0)
    fun getSetCoolingPowerCommand(power: Int, model: Model): ByteArray? =
        null
    fun getSetFanSpeedCommand(fanSpeed: Int, model: Model): ByteArray? =
        null
    fun getSetCoolingEnabledCommand(enabled: Boolean, model: Model): ByteArray? =
        null
    fun getSetCustomModeCommand(intensity: Int, model: Model): ByteArray? =
        null
    fun getSetLEDColorCommand(
        red: Int,
        green: Int,
        blue: Int,
        brightness: Int,
        model: Model,
    ): ByteArray? = TODO("no Android counterpart for BlackSharkLib")

    fun getTurnOffLEDCommand(model: Model): ByteArray =
        ByteArray(0)
    fun parseMessages(value: ByteArray): Any? = null
}

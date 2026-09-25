package com.moblin.android.integrations.blacksharkcooler

import android.util.Log
import com.moblin.android.common.various.RgbColor
import com.moblin.android.common.various.hexString
import com.moblin.android.platform.blacksharklib.BlackSharkLib
import com.moblin.android.platform.core.ContinuousClock
import com.moblin.android.platform.core.ProcessInfo
import com.moblin.android.platform.corebluetooth.CBCharacteristic
import com.moblin.android.platform.corebluetooth.CBCharacteristicWriteType
import com.moblin.android.platform.corebluetooth.CBCentralManager
import com.moblin.android.platform.corebluetooth.CBCentralManagerDelegate
import com.moblin.android.platform.corebluetooth.CBManagerState
import com.moblin.android.platform.corebluetooth.CBPeripheral
import com.moblin.android.platform.corebluetooth.CBPeripheralDelegate
import com.moblin.android.platform.corebluetooth.CBService
import com.moblin.android.platform.corebluetooth.CBUUID
import com.moblin.android.various.BluetoothScanner
import com.moblin.android.various.SimpleTimer
import java.util.UUID
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch

private val blackSharkCoolerDeviceDispatchQueue: CoroutineDispatcher =
    Executors.newSingleThreadExecutor().asCoroutineDispatcher()

interface BlackSharkCoolerDeviceDelegate {
    fun blackSharkCoolerDeviceState(device: BlackSharkCoolerDevice, state: BlackSharkCoolerDeviceState)

    fun blackSharkCoolerDeviceStatus(device: BlackSharkCoolerDevice, status: BlackSharkLib.CoolingState)
}

enum class BlackSharkCoolerDeviceState {
    disconnected,
    discovering,
    connecting,
    connected,
}

private val blackSharkCoolerServiceId = CBUUID(string = BlackSharkLib.getServiceUUID().toString())

val blackSharkCoolerScanner = BluetoothScanner(serviceIds = emptyList())

open class BlackSharkCoolerDevice : CBCentralManagerDelegate, CBPeripheralDelegate {
    private var state: BlackSharkCoolerDeviceState = BlackSharkCoolerDeviceState.disconnected
    private var centralManager: CBCentralManager? by CBCentralManager.holder()
    private var peripheral: CBPeripheral? = null
    private var deviceId: UUID? = null
    private var readCharacteristic: CBCharacteristic? = null
    private var writeCharacteristic: CBCharacteristic? = null
    private var latestTransmissionTime = ContinuousClock.now
    private var model: BlackSharkLib.Model? = null
    private var coolingStatsTimer = SimpleTimer(queue = blackSharkCoolerDeviceDispatchQueue)
    private var coolingPower: Int? = null
    private var fanSpeed: Int? = null
    open var delegate: BlackSharkCoolerDeviceDelegate? = null

    open fun start(deviceId: UUID?) {
        CoroutineScope(blackSharkCoolerDeviceDispatchQueue).launch {
            startInternal(deviceId)
        }
    }

    open fun stop() {
        CoroutineScope(blackSharkCoolerDeviceDispatchQueue).launch {
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
        centralManager = null
        peripheral = null
        readCharacteristic = null
        writeCharacteristic = null
        coolingStatsTimer.stop()
        setState(state = BlackSharkCoolerDeviceState.disconnected)
    }

    private fun reconnect() {
        peripheral = null
        setState(state = BlackSharkCoolerDeviceState.discovering)
        centralManager = CBCentralManager(
            delegate = this,
            queue = CoroutineScope(blackSharkCoolerDeviceDispatchQueue),
        )
    }

    private fun setState(state: BlackSharkCoolerDeviceState) {
        if (state == this.state) {
            return
        }
        Log.d("BlackSharkCoolerDevice", "black-shark-cooler-device: State change ${this.state} -> $state")
        this.state = state
        delegate?.blackSharkCoolerDeviceState(this, state = state)
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
            Log.i("BlackSharkCoolerDevice", "black-shark-cooler-device: Device not found")
            return
        }
        this.peripheral = peripheral
        peripheral.delegate = this
        central.connect(peripheral = peripheral, options = null)
        setState(state = BlackSharkCoolerDeviceState.connecting)
    }

    override fun centralManagerDidFailToConnect(
        central: CBCentralManager,
        peripheral: CBPeripheral,
        error: Throwable?,
    ) {
    }

    override fun centralManagerDidConnect(central: CBCentralManager, peripheral: CBPeripheral) {
        model = BlackSharkLib.detectModel(advertisedName = peripheral.name)
        peripheral.discoverServices(null)
    }

    override fun centralManagerDidDisconnectPeripheral(
        central: CBCentralManager,
        peripheral: CBPeripheral,
        error: Throwable?,
    ) {
        reconnect()
    }

    override fun peripheralDidDiscoverServices(peripheral: CBPeripheral, error: Throwable?) {
        val service = peripheral.services?.firstOrNull { it.uuid == blackSharkCoolerServiceId }
        if (service != null) {
            peripheral.discoverCharacteristics(null, `for` = service)
        }
    }

    override fun peripheralDidDiscoverCharacteristicsFor(
        peripheral: CBPeripheral,
        service: CBService,
        error: Throwable?,
    ) {
        for (characteristic in service.characteristics.orEmpty()) {
            Log.d(
                "BlackSharkCoolerDevice",
                "black-shark-cooler-device: Characteristic found: ${characteristic.uuid.uuidString}",
            )
            when (characteristic.uuid) {
                CBUUID(data = BlackSharkLib.getReadCharacteristicsUUID()) -> {
                    readCharacteristic = characteristic
                    this.peripheral?.setNotifyValue(true, `for` = characteristic)
                }
                CBUUID(data = BlackSharkLib.getWriteCharacteristicsUUID()) -> {
                    writeCharacteristic = characteristic
                    pollForCoolingStats()
                    coolingStatsTimer.startPeriodic(interval = 2.0) {
                        pollForCoolingStats()
                    }
                }
                else -> {}
            }
        }
        if (readCharacteristic != null) {
            setState(state = BlackSharkCoolerDeviceState.connected)
        }
    }

    private fun updatedPercentageScale(current: Int?, target: Int): Int {
        if (current == null) {
            return target
        }
        return if (current > target) {
            maxOf(current - 5, target)
        } else if (current < target) {
            minOf(current + 5, target)
        } else {
            target
        }
    }

    private fun pollForCoolingStats() {
        val peripheral = this.peripheral ?: return
        val writeCharacteristic = this.writeCharacteristic ?: return
        val model = this.model ?: return
        peripheral.writeValue(
            BlackSharkLib.getCoolingMetadataCommand(model = model),
            `for` = writeCharacteristic,
            type = CBCharacteristicWriteType.withoutResponse,
        )
    }

    open fun adjustCoolerProfilePro4(thermalState: ProcessInfo.ThermalState) {
        val peripheral = this.peripheral ?: return
        val writeCharacteristic = this.writeCharacteristic ?: return
        val model = this.model ?: return
        val coolingPowerTarget: Int
        val fanSpeedTarget: Int
        when (thermalState) {
            ProcessInfo.ThermalState.nominal -> {
                coolingPowerTarget = 0
                fanSpeedTarget = 10
            }
            ProcessInfo.ThermalState.fair -> {
                coolingPowerTarget = 20
                fanSpeedTarget = 20
            }
            ProcessInfo.ThermalState.serious -> {
                coolingPowerTarget = 80
                fanSpeedTarget = 50
            }
            ProcessInfo.ThermalState.critical -> {
                coolingPowerTarget = 100
                fanSpeedTarget = 100
            }
            else -> {
                coolingPowerTarget = 100
                fanSpeedTarget = 100
                Log.i(
                    "BlackSharkCoolerDevice",
                    "black-shark-cooler-device: Thermal state is unknown value ( $thermalState )",
                )
            }
        }
        val coolingPower = updatedPercentageScale(this.coolingPower, target = coolingPowerTarget)
        Log.d(
            "BlackSharkCoolerDevice",
            "black-shark-cooler-device (Pro 4): Adjusting cooling power to $coolingPower%",
        )
        peripheral.writeValue(
            BlackSharkLib.getSetCoolingPowerCommand(coolingPower, model = model)!!,
            `for` = writeCharacteristic,
            type = CBCharacteristicWriteType.withoutResponse,
        )
        val fanSpeed = updatedPercentageScale(this.fanSpeed, target = fanSpeedTarget)
        Log.d(
            "BlackSharkCoolerDevice",
            "black-shark-cooler-device (Pro 4): Adjusting fan speed to $fanSpeed%",
        )
        peripheral.writeValue(
            BlackSharkLib.getSetFanSpeedCommand(fanSpeed, model = model)!!,
            `for` = writeCharacteristic,
            type = CBCharacteristicWriteType.withoutResponse,
        )
    }

    open fun setCustomModePro5(intensity: Int) {
        val peripheral = this.peripheral ?: return
        val writeCharacteristic = this.writeCharacteristic ?: return
        if (intensity == 0) {
            Log.d(
                "BlackSharkCoolerDevice",
                "black-shark-cooler-device (Pro 5): Adjusting cooling power to OFF",
            )
            peripheral.writeValue(
                BlackSharkLib.getSetCoolingEnabledCommand(false, model = BlackSharkLib.Model.pro5)!!,
                `for` = writeCharacteristic,
                type = CBCharacteristicWriteType.withoutResponse,
            )
            return
        }
        val coolingPower = this.coolingPower
        if (coolingPower != null && coolingPower == 2) {
            Log.d(
                "BlackSharkCoolerDevice",
                "black-shark-cooler-device (Pro 5): Enabling custom mode for cooler.",
            )
            peripheral.writeValue(
                BlackSharkLib.getSetCoolingEnabledCommand(true, model = BlackSharkLib.Model.pro5)!!,
                `for` = writeCharacteristic,
                type = CBCharacteristicWriteType.withoutResponse,
            )
        }
        Log.d(
            "BlackSharkCoolerDevice",
            "black-shark-cooler-device (Pro 5): Adjusting cooling power to $intensity",
        )
        peripheral.writeValue(
            BlackSharkLib.getSetCustomModeCommand(intensity = intensity, model = BlackSharkLib.Model.pro5)!!,
            `for` = writeCharacteristic,
            type = CBCharacteristicWriteType.withoutResponse,
        )
    }

    open fun adjustCoolerProfilePro5(thermalState: ProcessInfo.ThermalState) {
        when (thermalState) {
            ProcessInfo.ThermalState.nominal -> setCustomModePro5(0)
            ProcessInfo.ThermalState.fair -> setCustomModePro5(1)
            ProcessInfo.ThermalState.serious -> setCustomModePro5(2)
            ProcessInfo.ThermalState.critical -> setCustomModePro5(5)
            else -> setCustomModePro5(5)
        }
    }

    open fun adjustCoolerProfile() {
        val model = this.model ?: return
        val thermalState = ProcessInfo.processInfo.thermalState
        when (model) {
            BlackSharkLib.Model.pro4 -> adjustCoolerProfilePro4(thermalState)
            BlackSharkLib.Model.pro5 -> adjustCoolerProfilePro5(thermalState)
        }
    }

    open fun setLedColor(color: RgbColor, brightness: Int) {
        val model = this.model ?: return
        val now = ContinuousClock.now
        if (latestTransmissionTime.duration(to = now) < 80.milliseconds) {
            return
        }
        latestTransmissionTime = now
        val setColorCommand = BlackSharkLib.getSetLEDColorCommand(
            color.red,
            color.green,
            color.blue,
            brightness = brightness,
            model = model,
        ) ?: return
        val peripheral = this.peripheral ?: return
        val writeCharacteristic = this.writeCharacteristic ?: return
        peripheral.writeValue(
            setColorCommand,
            `for` = writeCharacteristic,
            type = CBCharacteristicWriteType.withoutResponse,
        )
    }

    open fun turnLedOff() {
        val peripheral = this.peripheral ?: return
        val writeCharacteristic = this.writeCharacteristic ?: return
        val model = this.model ?: return
        peripheral.writeValue(
            BlackSharkLib.getTurnOffLEDCommand(model = model),
            `for` = writeCharacteristic,
            type = CBCharacteristicWriteType.withoutResponse,
        )
    }

    override fun peripheralDidUpdateValueFor(
        peripheral: CBPeripheral,
        characteristic: CBCharacteristic,
        error: Throwable?,
    ) {
        val value = characteristic.value ?: return
        val model = this.model ?: return
        when (characteristic.uuid) {
            CBUUID(data = BlackSharkLib.getReadCharacteristicsUUID()) -> {
                val message = BlackSharkLib.parseMessages(value)
                if (message is BlackSharkLib.CoolingState) {
                    delegate?.blackSharkCoolerDeviceStatus(this, status = message)
                    when (model) {
                        BlackSharkLib.Model.pro4 -> Log.d(
                            "BlackSharkCoolerDevice",
                            "black-shark-cooler-device (Pro 4): CoolerTemp: ${message.phoneTemperature}, " +
                                "Heatsink: ${message.heatsinkTemperature}",
                        )
                        BlackSharkLib.Model.pro5 -> Log.d(
                            "BlackSharkCoolerDevice",
                            "black-shark-cooler-device (Pro 5): CoolerTemp: ${message.phoneTemperature}, " +
                                "Heatsink: ${message.heatsinkTemperature}, " +
                                "Fan: ${message.fanRPM?.toString() ?: "n/a"}, " +
                                "Power: ${message.powerLevel?.toString() ?: "n/a"}",
                        )
                    }
                    adjustCoolerProfile()
                } else if (message is BlackSharkLib.UnknownMessage) {
                    Log.d(
                        "BlackSharkCoolerDevice",
                        "black-shark-cooler-device: Got unknown message ${message.rawData.hexString()}",
                    )
                }
            }
            else -> {}
        }
    }
}

package com.moblin.android.various.model

import com.moblin.android.integrations.blacksharkcooler.BlackSharkCoolerDevice
import com.moblin.android.integrations.blacksharkcooler.BlackSharkCoolerDeviceState
import com.moblin.android.various.settings.SettingsBlackSharkCoolerDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val mainScope = CoroutineScope(Dispatchers.Main)

fun Model.enableBlackSharkDevice(device: SettingsBlackSharkCoolerDevice) {
    val peripheralId = device.bluetoothPeripheralId ?: return
    if (!blackSharkCoolerDevices.containsKey(peripheralId)) {
        val blackSharkCoolerDevice = BlackSharkCoolerDevice()
        blackSharkCoolerDevice.delegate = this
        blackSharkCoolerDevices[peripheralId] = blackSharkCoolerDevice
    }
    blackSharkCoolerDevices[peripheralId]?.start(deviceId = peripheralId)
}

fun Model.disableBlackSharkCoolerDevice(device: SettingsBlackSharkCoolerDevice) {
    blackSharkCoolerDevices[device.id]?.stop()
}

fun Model.blackSharkCoolerDeviceState(device: BlackSharkCoolerDevice, state: BlackSharkCoolerDeviceState) {
    mainScope.launch {
        statusTopRight.blackSharkCoolerDeviceState = state
    }
}

fun Model.blackSharkCoolerDeviceStatus(
    device: BlackSharkCoolerDevice,
    phoneTemperature: Double,
    heatsinkTemperature: Double,
) {
    mainScope.launch {
        statusTopRight.blackSharkCoolerPhoneTemp = phoneTemperature
        statusTopRight.blackSharkCoolerExhaustTemp = heatsinkTemperature
    }
}

fun Model.autoStartBlackSharkCoolerDevices() {
    for (device in database.blackSharkCoolerDevices.devices.filter { it.enabled }) {
        enableBlackSharkDevice(device)
    }
}

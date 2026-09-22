package com.moblin.android.various.model

import com.moblin.android.integrations.blacksharkcooler.BlackSharkCoolerDevice
import com.moblin.android.integrations.blacksharkcooler.BlackSharkCoolerDeviceDelegate
import com.moblin.android.integrations.blacksharkcooler.BlackSharkCoolerDeviceState
import com.moblin.android.integrations.blacksharkcooler.BlackSharkLib
import com.moblin.android.various.settings.SettingsBlackSharkCoolerDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private class BlackSharkCoolerDeviceDelegateImpl(private val model: Model) : BlackSharkCoolerDeviceDelegate {
    override fun blackSharkCoolerDeviceState(device: BlackSharkCoolerDevice, state: BlackSharkCoolerDeviceState) {
        model.blackSharkCoolerDeviceState(device, state)
    }
    override fun blackSharkCoolerDeviceStatus(device: BlackSharkCoolerDevice, status: BlackSharkLib.CoolingState) {
        model.blackSharkCoolerDeviceStatus(device, status.phoneTemperature.toDouble(), status.heatsinkTemperature.toDouble())
    }
}

private val mainScope = CoroutineScope(Dispatchers.Main)

fun Model.enableBlackSharkDevice(device: SettingsBlackSharkCoolerDevice) {
    val peripheralId = device.bluetoothPeripheralId ?: return
    if (!blackSharkCoolerDevices.containsKey(peripheralId)) {
        val blackSharkCoolerDevice = BlackSharkCoolerDevice(context = TODO("no Android context available"))
        blackSharkCoolerDevice.delegate = BlackSharkCoolerDeviceDelegateImpl(this)
        blackSharkCoolerDevices[peripheralId] = blackSharkCoolerDevice
    }
    blackSharkCoolerDevices[peripheralId]?.start(deviceId = peripheralId)
}

fun Model.disableBlackSharkCoolerDevice(device: SettingsBlackSharkCoolerDevice) {
    blackSharkCoolerDevices[device.id]?.stop()
}

fun Model.blackSharkCoolerDeviceState(device: BlackSharkCoolerDevice, state: BlackSharkCoolerDeviceState) {
    mainScope.launch {
        statusTopRight.blackSharkCoolerDeviceState.value = state
    }
}

fun Model.blackSharkCoolerDeviceStatus(
    device: BlackSharkCoolerDevice,
    phoneTemperature: Double,
    heatsinkTemperature: Double,
) {
    mainScope.launch {
        statusTopRight.blackSharkCoolerPhoneTemp.value = phoneTemperature.toInt()
        statusTopRight.blackSharkCoolerExhaustTemp.value = heatsinkTemperature.toInt()
    }
}

fun Model.autoStartBlackSharkCoolerDevices() {
    for (device in database.blackSharkCoolerDevices.devices.filter { it.enabled }) {
        enableBlackSharkDevice(device)
    }
}

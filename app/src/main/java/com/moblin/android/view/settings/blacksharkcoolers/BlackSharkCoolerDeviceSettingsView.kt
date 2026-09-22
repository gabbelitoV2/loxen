package com.moblin.android.view.settings.blacksharkcoolers

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.moblin.android.integrations.blacksharkcooler.BlackSharkCoolerDeviceState
import com.moblin.android.integrations.blacksharkcooler.blackSharkCoolerScanner
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.StatusTopRight
import com.moblin.android.various.settings.SettingsBlackSharkCoolerDevice
import com.moblin.android.various.settings.SettingsBlackSharkCoolerDevices
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.NameEditView
import java.util.UUID
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

private fun formatBlackSharkCoolerDeviceState(state: BlackSharkCoolerDeviceState?): String {
    return when (state) {
        null, BlackSharkCoolerDeviceState.DISCONNECTED -> localized("Disconnected")
        BlackSharkCoolerDeviceState.DISCOVERING -> localized("Discovering")
        BlackSharkCoolerDeviceState.CONNECTING -> localized("Connecting")
        BlackSharkCoolerDeviceState.CONNECTED -> localized("Connected")
        else -> localized("Unknown")
    }
}

@Composable
private fun state(status: StatusTopRight): String {
    return formatBlackSharkCoolerDeviceState(status.blackSharkCoolerDeviceState.collectAsState().value)
}

private fun canEnable(bluetoothPeripheralId: UUID?): Boolean {
    return bluetoothPeripheralId != null
}

private fun onDeviceChange(value: String, device: SettingsBlackSharkCoolerDevice) {
    val deviceId = runCatching { UUID.fromString(value) }.getOrNull() ?: return
    val peripheral = blackSharkCoolerScanner.discoveredPeripherals.value
        .filterIsInstance<BlackSharkCoolerPeripheral>()
        .firstOrNull { it.identifier == value } ?: return
    device.bluetoothPeripheralName = peripheral.name
    device.bluetoothPeripheralId = deviceId
}

private fun changeColor(model: Model, device: SettingsBlackSharkCoolerDevice) {
    val blackSharkCoolerDevice = model.blackSharkCoolerDevices
        .entries
        .firstOrNull { it.key == device.bluetoothPeripheralId }?.value
    if (blackSharkCoolerDevice == null) {
        Log.i("BlackSharkCoolerDeviceSettingsView", "Could not find phone cooler")
        return
    }
    blackSharkCoolerDevice.setLedColor(
        color = device.rgbLightColor,
        brightness = device.rgbLightBrightness.toInt(),
    )
}

private fun toggleLight(model: Model, device: SettingsBlackSharkCoolerDevice) {
    val blackSharkCoolerDevice = model.blackSharkCoolerDevices
        .entries
        .firstOrNull { it.key == device.bluetoothPeripheralId }?.value
    if (blackSharkCoolerDevice == null) {
        Log.i("BlackSharkCoolerDeviceSettingsView", "Could not find phone cooler")
        return
    }
    if (device.rgbLightEnabled) {
        blackSharkCoolerDevice.setLedColor(
            color = device.rgbLightColor,
            brightness = device.rgbLightBrightness.toInt(),
        )
    } else {
        blackSharkCoolerDevice.turnLedOff()
    }
}

@Composable
fun BlackSharkCoolerDeviceSettingsView(
    model: Model = LocalModel.current,
    blackSharkCoolerDevices: SettingsBlackSharkCoolerDevices,
    device: SettingsBlackSharkCoolerDevice,
    status: StatusTopRight,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Text(
        text = device.name,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onNavigate("Cooler")
            },
    )
}

@Composable
fun BlackSharkCoolerDeviceSettingsViewContent(
    model: Model = LocalModel.current,
    blackSharkCoolerDevices: SettingsBlackSharkCoolerDevices,
    device: SettingsBlackSharkCoolerDevice,
    status: StatusTopRight,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val name = device.name
    val bluetoothPeripheralName = device.bluetoothPeripheralName
    val bluetoothPeripheralId = device.bluetoothPeripheralId
    val enabled = device.enabled
    val rgbLightEnabled = device.rgbLightEnabled
    val rgbLightBrightness = device.rgbLightBrightness
    val phoneTemp = status.blackSharkCoolerPhoneTemp.collectAsState().value
    val exhaustTemp = status.blackSharkCoolerExhaustTemp.collectAsState().value

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            NameEditView(
                name = name,
                existingNames = blackSharkCoolerDevices.devices,
                onNameChange = {
                    device.name = it
                },
            )
        }
        item {
            Text(localized("Device"))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !enabled) {
                        onNavigate("BlackSharkCoolerDeviceScannerSettingsView")
                    },
            ) {
                GrayTextView(
                    text = bluetoothPeripheralName ?: localized("Select device"),
                )
            }
            if (phoneTemp != null && exhaustTemp != null) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("Phone: $phoneTemp °C")
                    Spacer(Modifier.weight(1f))
                    Text("Exhaust: $exhaustTemp °C")
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(localized("Enabled"), modifier = Modifier.weight(1f))
                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        device.enabled = it
                    },
                    enabled = canEnable(bluetoothPeripheralId),
                )
            }
            LaunchedEffect(enabled) {
                if (enabled) {
                    Unit
                } else {
                    Unit
                }
            }
        }
        if (enabled) {
            item {
                HCenter {
                    Text(state(status))
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(localized("Enabled"), modifier = Modifier.weight(1f))
                Switch(
                    checked = rgbLightEnabled,
                    onCheckedChange = {
                        device.rgbLightEnabled = it
                    },
                )
            }
            LaunchedEffect(rgbLightEnabled) {
                toggleLight(model, device)
            }
            if (rgbLightEnabled) {
                Unit
            }
            if (rgbLightEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(localized("Brightness"))
                    Slider(
                        value = rgbLightBrightness.toFloat(),
                        onValueChange = {
                            device.rgbLightBrightness = it.toDouble()
                        },
                        valueRange = 0f..100f,
                        modifier = Modifier.weight(1f),
                    )
                }
                LaunchedEffect(rgbLightBrightness) {
                    changeColor(model, device)
                }
            }
            Text(localized("RGB light"))
        }
    }
}

package com.moblin.android.view.settings.blacksharkcoolers

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.common.various.color
import com.moblin.android.integrations.blacksharkcooler.BlackSharkCoolerDeviceState
import com.moblin.android.integrations.blacksharkcooler.blackSharkCoolerScanner
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.FormSlider
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.StatusTopRight
import com.moblin.android.various.settings.SettingsBlackSharkCoolerDevice
import com.moblin.android.various.settings.SettingsBlackSharkCoolerDevices
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.NameEditView
import java.util.UUID
import com.moblin.android.various.model.disableBlackSharkCoolerDevice
import com.moblin.android.various.model.enableBlackSharkDevice
import com.moblin.android.view.settings.blacksharkcoolers.BlackSharkCoolerPeripheral

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
    val deviceState by status.blackSharkCoolerDeviceState.collectAsState()
    return formatBlackSharkCoolerDeviceState(deviceState)
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
    NavigationLink(
        destination = {
            BlackSharkCoolerDeviceSettingsViewContent(
                model = model,
                blackSharkCoolerDevices = blackSharkCoolerDevices,
                device = device,
                status = status,
                onNavigate = onNavigate,
            )
        },
    ) {
        Text(device.name)
    }
}

@Composable
fun BlackSharkCoolerDeviceSettingsViewContent(
    model: Model = LocalModel.current,
    blackSharkCoolerDevices: SettingsBlackSharkCoolerDevices,
    device: SettingsBlackSharkCoolerDevice,
    status: StatusTopRight,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val phoneTemp by status.blackSharkCoolerPhoneTemp.collectAsState()
    val exhaustTemp by status.blackSharkCoolerExhaustTemp.collectAsState()
    val scanner = remember { BlackSharkCoolerScanner() }

    Form(title = localized("Cooler")) {
        Section {
            NameEditView(
                name = device.name,
                existingNames = blackSharkCoolerDevices.devices,
                onNameChange = {
                    device.name = it
                },
            )
        }
        Section(
            header = localized("Device"),
            footerContent = {
                if (phoneTemp != null && exhaustTemp != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Phone: $phoneTemp °C")
                        Spacer(Modifier.weight(1f))
                        Text("Exhaust: $exhaustTemp °C")
                    }
                }
            },
        ) {
            NavigationLink(
                destination = {
                    BlackSharkCoolerDeviceScannerSettingsView(
                        scanner = scanner,
                        onChange = { onDeviceChange(it, device) },
                        selectedId = device.bluetoothPeripheralId?.toString()
                            ?: localized("Select device"),
                        onDismiss = {},
                    )
                },
                enabled = !device.enabled,
            ) {
                GrayTextView(
                    text = device.bluetoothPeripheralName ?: localized("Select device"),
                )
            }
        }
        Section {
            Toggle(
                title = localized("Enabled"),
                isOn = device.enabled,
                enabled = canEnable(device.bluetoothPeripheralId),
                onChange = { newValue ->
                    device.enabled = newValue
                    if (newValue) {
                        model.enableBlackSharkDevice(device)
                    } else {
                        model.disableBlackSharkCoolerDevice(device)
                    }
                },
            )
        }
        if (device.enabled) {
            Section {
                HCenter {
                    Text(state(status))
                }
            }
        }
        Section(header = localized("RGB light")) {
            Toggle(
                title = localized("Enabled"),
                isOn = device.rgbLightEnabled,
                onChange = { newValue ->
                    device.rgbLightEnabled = newValue
                    toggleLight(model, device)
                },
            )
            if (device.rgbLightEnabled) {
                FormRow {
                    Text(localized("Color"))
                    Spacer(Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .size(29.dp)
                            .clip(CircleShape)
                            .background(device.rgbLightColor.color()),
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(localized("Brightness"))
                    FormSlider(
                        value = device.rgbLightBrightness.toFloat(),
                        onValueChange = {
                            device.rgbLightBrightness = it.toDouble()
                            changeColor(model, device)
                        },
                        valueRange = 0f..100f,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

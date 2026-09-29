package com.moblin.android.view.settings.blacksharkcoolers

import com.moblin.android.platform.log.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.common.various.toStandardRgb
import com.moblin.android.integrations.blacksharkcooler.BlackSharkCoolerDeviceState
import com.moblin.android.integrations.blacksharkcooler.blackSharkCoolerScanner
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.ColorPicker
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.FormSlider
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.NameEditView
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.StatusTopRight
import com.moblin.android.various.model.disableBlackSharkCoolerDevice
import com.moblin.android.various.model.enableBlackSharkDevice
import com.moblin.android.various.settings.SettingsBlackSharkCoolerDevice
import com.moblin.android.various.settings.SettingsBlackSharkCoolerDevices
import java.util.UUID

private fun formatBlackSharkCoolerDeviceState(state: BlackSharkCoolerDeviceState?): String {
    if (state == null || state == BlackSharkCoolerDeviceState.disconnected) {
        return localized("Disconnected")
    } else if (state == BlackSharkCoolerDeviceState.discovering) {
        return localized("Discovering")
    } else if (state == BlackSharkCoolerDeviceState.connecting) {
        return localized("Connecting")
    } else if (state == BlackSharkCoolerDeviceState.connected) {
        return localized("Connected")
    } else {
        return localized("Unknown")
    }
}

@Composable
fun BlackSharkCoolerDeviceSettingsView(
    model: Model = LocalModel.current,
    blackSharkCoolerDevices: SettingsBlackSharkCoolerDevices,
    device: SettingsBlackSharkCoolerDevice,
    status: StatusTopRight,
) {
    val scanner = blackSharkCoolerScanner
    val blackSharkCoolerDeviceState by status.blackSharkCoolerDeviceState.collectAsState()
    val blackSharkCoolerPhoneTemp by status.blackSharkCoolerPhoneTemp.collectAsState()
    val blackSharkCoolerExhaustTemp by status.blackSharkCoolerExhaustTemp.collectAsState()

    fun state(): String {
        return formatBlackSharkCoolerDeviceState(blackSharkCoolerDeviceState)
    }

    fun canEnable(): Boolean {
        return device.bluetoothPeripheralId != null
    }

    fun onDeviceChange(value: String) {
        val deviceId = runCatching { UUID.fromString(value) }.getOrNull() ?: return
        val peripheral = scanner.discoveredPeripherals.value.firstOrNull { it.identifier == deviceId }
            ?: return
        device.bluetoothPeripheralName = peripheral.name
        device.bluetoothPeripheralId = deviceId
    }

    fun changeColor() {
        val blackSharkCoolerDevice = model.blackSharkCoolerDevices.entries
            .firstOrNull { it.key == device.bluetoothPeripheralId }?.value
            ?: run {
                Log.i("BlackSharkCoolerDeviceSettingsView", "Could not find phone cooler")
                return
            }
        blackSharkCoolerDevice.setLedColor(
            color = device.rgbLightColor,
            brightness = device.rgbLightBrightness.toInt(),
        )
    }

    fun toggleLight() {
        val blackSharkCoolerDevice = model.blackSharkCoolerDevices.entries
            .firstOrNull { it.key == device.bluetoothPeripheralId }?.value
            ?: run {
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

    NavigationLink(
        destination = {
            Form(title = "Cooler") {
                Section {
                    NameEditView(
                        name = device.name,
                        onNameChange = { device.name = it },
                        existingNames = blackSharkCoolerDevices.devices,
                    )
                }
                Section(
                    header = "Device",
                    footerContent = {
                        val phoneTemp = blackSharkCoolerPhoneTemp
                        val exhaustTemp = blackSharkCoolerExhaustTemp
                        if (phoneTemp != null && exhaustTemp != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(localized("Phone: $phoneTemp °C"))
                                Spacer(Modifier.weight(1f))
                                Text(localized("Exhaust: $exhaustTemp °C"))
                            }
                        }
                    },
                ) {
                    NavigationLink(
                        destination = {
                            BlackSharkCoolerDeviceScannerSettingsView(
                                onChange = { onDeviceChange(it) },
                                selectedId = device.bluetoothPeripheralId?.toString()
                                    ?: localized("Select device"),
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
                        title = "Enabled",
                        isOn = device.enabled,
                        enabled = canEnable(),
                        onChange = { enabled ->
                            device.enabled = enabled
                            if (device.enabled) {
                                model.enableBlackSharkDevice(device = device)
                            } else {
                                model.disableBlackSharkCoolerDevice(device = device)
                            }
                        },
                    )
                }
                if (device.enabled) {
                    Section {
                        HCenter {
                            Text(state())
                        }
                    }
                }
                Section(header = "RGB light") {
                    Toggle(
                        title = "Enabled",
                        isOn = device.rgbLightEnabled,
                        onChange = { enabled ->
                            device.rgbLightEnabled = enabled
                            toggleLight()
                        },
                    )
                    if (device.rgbLightEnabled) {
                        ColorPicker(
                            title = "Color",
                            selection = device.rgbLightColorColor,
                            onSelectionChange = { color ->
                                device.rgbLightColorColor = color
                                val rgbColor = color.toStandardRgb() ?: return@ColorPicker
                                device.rgbLightColor = rgbColor
                                changeColor()
                            },
                            supportsOpacity = false,
                        )
                        FormRow {
                            Text(localized("Brightness"))
                            FormSlider(
                                value = device.rgbLightBrightness.toFloat(),
                                onValueChange = { value ->
                                    device.rgbLightBrightness = value.toDouble()
                                    changeColor()
                                },
                                modifier = Modifier.weight(1f),
                                valueRange = 0f..100f,
                            )
                        }
                    }
                }
            }
        },
    ) {
        Text(device.name)
    }
}

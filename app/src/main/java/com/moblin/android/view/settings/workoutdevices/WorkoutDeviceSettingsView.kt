package com.moblin.android.view.settings.workoutdevices

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.input.KeyboardType
import com.moblin.android.LocalModel
import com.moblin.android.integrations.workoutdevice.WorkoutDeviceState
import com.moblin.android.integrations.workoutdevice.workoutDeviceScanner
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.StatusTopRight
import com.moblin.android.various.model.disableWorkoutDevice
import com.moblin.android.various.model.enableWorkoutDevice
import com.moblin.android.various.model.isWorkoutDeviceEnabled
import com.moblin.android.various.model.setCurrentWorkoutDevice
import com.moblin.android.various.model.setWorkoutDeviceWheelCircumference
import com.moblin.android.various.settings.SettingsWorkoutDevice
import com.moblin.android.various.settings.SettingsWorkoutDevices
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.NameEditView
import com.moblin.android.view.utils.TextEditNavigationView
import java.util.UUID

private fun formatWorkoutDeviceState(state: WorkoutDeviceState?): String {
    return if (state == null || state == WorkoutDeviceState.disconnected) {
        localized("Disconnected")
    } else if (state == WorkoutDeviceState.discovering) {
        localized("Discovering")
    } else if (state == WorkoutDeviceState.connecting) {
        localized("Connecting")
    } else if (state == WorkoutDeviceState.connected) {
        localized("Connected")
    } else {
        localized("Unknown")
    }
}

@Composable
fun WorkoutDeviceSettingsView(
    model: Model = LocalModel.current,
    workoutDevices: SettingsWorkoutDevices,
    device: SettingsWorkoutDevice,
    status: StatusTopRight,
) {
    val scanner = workoutDeviceScanner
    val workoutDeviceState by status.workoutDeviceState.collectAsState()

    fun state(): String {
        return formatWorkoutDeviceState(workoutDeviceState)
    }

    fun canEnable(): Boolean {
        return device.bluetoothPeripheralId != null
    }

    fun isValidWheelCircumference(value: String): String? {
        val millimeters = value.toLongOrNull() ?: return localized("Not a number")
        if (millimeters < 500) {
            return localized("Too small")
        }
        if (millimeters > 3000) {
            return localized("Too big")
        }
        return null
    }

    fun submitWheelCircumference(value: String) {
        val millimeters = value.toLongOrNull() ?: return
        device.wheelCircumference = millimeters.toInt()
        model.setWorkoutDeviceWheelCircumference(device = device)
    }

    fun onDeviceChange(value: String) {
        val deviceId = runCatching { UUID.fromString(value) }.getOrNull() ?: return
        val peripheral = scanner.discoveredPeripherals.value.firstOrNull { it.identifier == deviceId } ?: return
        device.bluetoothPeripheralName = peripheral.name
        device.bluetoothPeripheralId = deviceId
    }

    NavigationLink(
        destination = {
            Form(title = localized("Workout device")) {
                DisposableEffect(Unit) {
                    model.setCurrentWorkoutDevice(device = device)
                    onDispose {}
                }
                Section(
                    footer = localized("Add {heartRate:${device.name}} to a text widget to show heart rate on stream."),
                ) {
                    NameEditView(
                        name = device.name,
                        onNameChange = { device.name = it },
                        existingNames = workoutDevices.devices,
                    )
                }
                Section(header = localized("Device")) {
                    NavigationLink(
                        destination = {
                            WorkoutDeviceScannerSettingsView(
                                onChange = { onDeviceChange(it) },
                                selectedId = device.bluetoothPeripheralId?.toString() ?: localized("Select device"),
                            )
                        },
                        enabled = !model.isWorkoutDeviceEnabled(device = device),
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
                        onChange = { newValue ->
                            device.enabled = newValue
                            if (device.enabled) {
                                model.enableWorkoutDevice(device = device)
                            } else {
                                model.disableWorkoutDevice(device = device)
                            }
                        },
                    )
                }
                Section(
                    footer = localized("Used to calculate speed from wheel revolutions."),
                ) {
                    TextEditNavigationView(
                        title = localized("Wheel circumference"),
                        value = device.wheelCircumference.toString(),
                        onChange = { value -> isValidWheelCircumference(value) },
                        onSubmit = { value -> submitWheelCircumference(value) },
                        keyboardType = KeyboardType.Number,
                        valueFormat = { "$it mm" },
                    )
                }
                if (device.enabled) {
                    Section {
                        HCenter {
                            Text(state())
                        }
                    }
                }
            }
        },
    ) {
        Text(device.name)
    }
}

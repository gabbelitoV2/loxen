package com.moblin.android.view.settings.workoutdevices

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.integrations.workoutdevice.WorkoutDeviceState
import com.moblin.android.integrations.workoutdevice.workoutDeviceScanner
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.StatusTopRight
import com.moblin.android.various.settings.SettingsWorkoutDevice
import com.moblin.android.various.settings.SettingsWorkoutDevices
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.NameEditView
import com.moblin.android.view.utils.TextEditNavigationView
import java.util.UUID
import com.moblin.android.various.model.disableWorkoutDevice
import com.moblin.android.various.model.enableWorkoutDevice
import com.moblin.android.various.model.isWorkoutDeviceEnabled
import com.moblin.android.various.model.setCurrentWorkoutDevice
import com.moblin.android.various.model.setWorkoutDeviceWheelCircumference

private fun formatWorkoutDeviceState(state: WorkoutDeviceState?): String {
    return when (state) {
        null, WorkoutDeviceState.disconnected -> localized("Disconnected")
        WorkoutDeviceState.discovering -> localized("Discovering")
        WorkoutDeviceState.connecting -> localized("Connecting")
        WorkoutDeviceState.connected -> localized("Connected")
        else -> localized("Unknown")
    }
}

@Composable
fun WorkoutDeviceSettingsView(
    model: Model = LocalModel.current,
    workoutDevices: SettingsWorkoutDevices,
    device: SettingsWorkoutDevice,
    status: StatusTopRight,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
    modifier: Modifier = Modifier,
) {
    NavigationLink(
        destination = {
            WorkoutDeviceSettingsViewContent(
                model = model,
                workoutDevices = workoutDevices,
                device = device,
                status = status,
            )
        },
    ) {
        Text(text = device.name)
    }
}

@Composable
fun WorkoutDeviceSettingsViewContent(
    model: Model = LocalModel.current,
    workoutDevices: SettingsWorkoutDevices,
    device: SettingsWorkoutDevice,
    status: StatusTopRight,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
    modifier: Modifier = Modifier,
) {
    val workoutDeviceState by status.workoutDeviceState.collectAsState()
    var enabled by remember(device) { mutableStateOf(device.enabled) }

    fun state(): String {
        return formatWorkoutDeviceState(workoutDeviceState)
    }

    fun canEnable(): Boolean {
        return device.bluetoothPeripheralId != null
    }

    fun isValidWheelCircumference(value: String): String? {
        val millimeters = value.toIntOrNull() ?: return localized("Not a number")
        if (millimeters < 500) {
            return localized("Too small")
        }
        if (millimeters > 3000) {
            return localized("Too big")
        }
        return null
    }

    fun submitWheelCircumference(value: String) {
        val millimeters = value.toIntOrNull() ?: return
        device.wheelCircumference = millimeters
        model.setWorkoutDeviceWheelCircumference(device = device)
    }

    fun onDeviceChange(value: String) {
        val deviceId = runCatching { UUID.fromString(value) }.getOrNull() ?: return
        val peripheral = workoutDeviceScanner.discoveredPeripherals.value
            .firstOrNull { it.address == value } ?: return
        device.bluetoothPeripheralName = peripheral.name
        device.bluetoothPeripheralId = deviceId
    }

    LaunchedEffect(Unit) {
        model.setCurrentWorkoutDevice(device = device)
    }

    Form(
        title = localized("Workout device"),
        modifier = modifier,
    ) {
        Section(
            footer = localized("Add {heartRate:${device.name}} to a text widget to show heart rate on stream."),
        ) {
            NameEditView(
                name = device.name,
                existingNames = workoutDevices.devices,
                onNameChange = { device.name = it },
            )
        }
        Section(
            header = localized("Device"),
        ) {
            NavigationLink(
                destination = {
                    WorkoutDeviceScannerSettingsView(
                        onChange = { onDeviceChange(it) },
                        selectedId = device.bluetoothPeripheralId?.toString()
                            ?: localized("Select device"),
                        onSelectedIdChange = { onDeviceChange(it) },
                        onDismiss = { },
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
                title = localized("Enabled"),
                isOn = enabled,
                enabled = canEnable(),
                onChange = { newValue ->
                    enabled = newValue
                    device.enabled = newValue
                    if (newValue) {
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
                onChange = { isValidWheelCircumference(it) },
                onSubmit = { submitWheelCircumference(it) },
                keyboardType = KeyboardType.Number,
                valueFormat = { "$it mm" },
            )
        }
        if (enabled) {
            Section {
                HCenter {
                    Text(text = state())
                }
            }
        }
    }
}

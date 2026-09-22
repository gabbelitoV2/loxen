package com.moblin.android.view.settings.workoutdevices

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.moblin.android.integrations.workoutdevice.WorkoutDeviceState
import com.moblin.android.integrations.workoutdevice.workoutDeviceScanner
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.StatusTopRight
import com.moblin.android.various.settings.SettingsWorkoutDevice
import com.moblin.android.various.settings.SettingsWorkoutDevices
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.NameEditView
import com.moblin.android.view.utils.TextEditNavigationView
import java.util.UUID
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

private fun formatWorkoutDeviceState(state: WorkoutDeviceState?): String {
    return when {
        state == null || state == WorkoutDeviceState.disconnected -> localized("Disconnected")
        state == WorkoutDeviceState.discovering -> localized("Discovering")
        state == WorkoutDeviceState.connecting -> localized("Connecting")
        state == WorkoutDeviceState.connected -> localized("Connected")
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
    val name = device.name
    Text(
        text = name,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onNavigate("WorkoutDeviceSettingsView") },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDeviceSettingsViewContent(
    model: Model = LocalModel.current,
    workoutDevices: SettingsWorkoutDevices,
    device: SettingsWorkoutDevice,
    status: StatusTopRight,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
    modifier: Modifier = Modifier,
) {
    val deviceName = device.name
    val deviceEnabled = device.enabled
    val bluetoothPeripheralId = device.bluetoothPeripheralId
    val bluetoothPeripheralName = device.bluetoothPeripheralName
    val wheelCircumference = device.wheelCircumference
    val workoutDeviceState = status.workoutDeviceState.collectAsState().value
    val existingNames = workoutDevices.devices

    fun state(): String {
        return formatWorkoutDeviceState(workoutDeviceState)
    }

    fun canEnable(): Boolean {
        return bluetoothPeripheralId != null
    }

    fun isWorkoutDeviceEnabled(): Boolean {
        return TODO("model.isWorkoutDeviceEnabled(device)")
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
        TODO("model.setWorkoutDeviceWheelCircumference(device)")
    }

    fun onDeviceChange(value: String) {
        val deviceId = runCatching { UUID.fromString(value) }.getOrNull() ?: return
        val peripheral = workoutDeviceScanner.discoveredPeripherals.value
            .firstOrNull { it.id.toString() == value } ?: return
        device.bluetoothPeripheralName = peripheral.name
        device.bluetoothPeripheralId = deviceId
    }

    LaunchedEffect(Unit) {
        TODO("model.setCurrentWorkoutDevice(device)")
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text(localized("Workout device")) })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            NameEditView(
                name = deviceName,
                existingNames = existingNames,
                onNameChange = { device.name = it },
            )
            Text(text = "Add {heartRate:$deviceName} to a text widget to show heart rate on stream.")

            Text(
                text = localized("Device"),
                style = MaterialTheme.typography.titleSmall,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isWorkoutDeviceEnabled()) {
                        onNavigate("WorkoutDeviceScannerSettingsView")
                    },
            ) {
                GrayTextView(text = bluetoothPeripheralName ?: localized("Select device"))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = localized("Enabled"),
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = deviceEnabled,
                    onCheckedChange = { enabled ->
                        device.enabled = enabled
                        if (enabled) {
                            TODO("model.enableWorkoutDevice(device)")
                        } else {
                            TODO("model.disableWorkoutDevice(device)")
                        }
                    },
                    enabled = canEnable(),
                )
            }

            TextEditNavigationView(
                title = localized("Wheel circumference"),
                value = wheelCircumference.toString(),
                onChange = { isValidWheelCircumference(it) },
                onSubmit = { submitWheelCircumference(it) },
                keyboardType = KeyboardType.Number,
                valueFormat = { "$it mm" },
            )
            Text(text = localized("Used to calculate speed from wheel revolutions."))

            if (deviceEnabled) {
                HCenter {
                    Text(text = state())
                }
            }
        }
    }
}

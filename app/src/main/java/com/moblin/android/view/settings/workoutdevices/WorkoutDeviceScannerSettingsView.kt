package com.moblin.android.view.settings.workoutdevices

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.moblin.android.LocalModel
import com.moblin.android.integrations.workoutdevice.workoutDeviceScanner
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Button
import com.moblin.android.platform.swiftui.ForEach
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.rememberDismiss
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.bluetoothNotAllowedMessage
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.InlinePickerItem

@Composable
fun WorkoutDeviceScannerSettingsView(
    onChange: (String) -> Unit,
    selectedId: String,
    model: Model = LocalModel.current,
) {
    val scanner = workoutDeviceScanner
    val dismiss = rememberDismiss()
    val bluetoothAllowed by model.bluetoothAllowed.collectAsState()
    val discoveredPeripherals by scanner.discoveredPeripherals.collectAsState()
    DisposableEffect(Unit) {
        scanner.startScanningForDevices()
        onDispose {
            scanner.stopScanningForDevices()
        }
    }
    Form(title = "Device") {
        Section {
            if (!bluetoothAllowed) {
                Text(bluetoothNotAllowedMessage)
            } else if (discoveredPeripherals.isEmpty()) {
                HCenter {
                    CircularProgressIndicator()
                }
            } else {
                val items = discoveredPeripherals.map { peripheral ->
                    InlinePickerItem(
                        id = peripheral.identifier.toString(),
                        text = peripheral.name ?: localized("Unknown"),
                    )
                }
                ForEach(items, id = { it.id }) { item ->
                    Button(action = {
                        onChange(item.id)
                        dismiss()
                    }) {
                        Text(item.text)
                    }
                }
            }
        }
    }
}

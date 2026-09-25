package com.moblin.android.view.settings.blacksharkcoolers

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import com.moblin.android.LocalModel
import com.moblin.android.integrations.blacksharkcooler.blackSharkCoolerScanner
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Button
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.rememberDismiss
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.bluetoothNotAllowedMessage
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.InlinePickerItem

@Composable
fun BlackSharkCoolerDeviceScannerSettingsView(
    model: Model = LocalModel.current,
    onChange: (String) -> Unit,
    selectedId: String,
) {
    val scanner = blackSharkCoolerScanner
    val dismiss = rememberDismiss()
    val bluetoothAllowed by model.bluetoothAllowed.collectAsState()
    val discoveredPeripherals by scanner.discoveredPeripherals.collectAsState()
    val options = discoveredPeripherals
        .filter { it.name?.contains("black shark", ignoreCase = true) == true }
        .map { peripheral ->
            InlinePickerItem(
                id = peripheral.identifier.toString(),
                text = peripheral.name ?: localized("Unknown"),
            )
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
                options.forEach { item ->
                    key(item.id) {
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
    DisposableEffect(Unit) {
        scanner.startScanningForDevices()
        onDispose {
            scanner.stopScanningForDevices()
        }
    }
}

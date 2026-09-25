package com.moblin.android.view.settings.tesla

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.integrations.tesla.TeslaVehicleScanner
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Button
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.platform.swiftui.rememberDismiss
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.bluetoothNotAllowedMessage
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.InlinePickerItem

@Composable
fun TeslaVehicleScannerSettingsView(
    model: Model = LocalModel.current,
    onChange: (String) -> Unit,
) {
    val scanner = TeslaVehicleScanner.shared
    val dismiss = rememberDismiss()
    val bluetoothAllowed by model.bluetoothAllowed.collectAsState()
    val discoveredPeripherals by scanner.discoveredPeripherals.collectAsState()
    DisposableEffect(Unit) {
        scanner.startScanningForDevices()
        onDispose {
            scanner.stopScanningForDevices()
        }
    }
    Form(title = "Vehicle") {
        Section(footer = "Make sure the Moblin device is relatively near the vehicle.") {
            if (!bluetoothAllowed) {
                Text(bluetoothNotAllowedMessage)
            } else if (discoveredPeripherals.isEmpty()) {
                HCenter {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = formPalette().gray,
                        strokeWidth = 2.dp,
                    )
                }
            } else {
                discoveredPeripherals.map { peripheral ->
                    InlinePickerItem(
                        id = peripheral.identifier.toString(),
                        text = peripheral.name ?: localized("Unknown"),
                    )
                }.forEach { item ->
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
}

package com.moblin.android.view.settings.tesla

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalContext
import com.moblin.android.LocalModel
import com.moblin.android.integrations.tesla.TeslaVehicleScanner
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.bluetoothNotAllowedMessage
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.InlinePickerItem
import com.moblin.android.platform.corebluetooth.identifier

@Composable
fun TeslaVehicleScannerSettingsView(
    model: Model = LocalModel.current,
    onChange: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val bluetoothAllowed by model.bluetoothAllowed.collectAsState()
    val scanner = TeslaVehicleScanner.shared
    val peripherals by scanner.discoveredPeripherals.collectAsState()
    val context = LocalContext.current

    DisposableEffect(Unit) {
        scanner.startScanningForDevices(context)
        onDispose {
            scanner.stopScanningForDevices()
        }
    }

    Form(title = localized("Vehicle")) {
        Section(
            footer = localized("Make sure the Moblin device is relatively near the vehicle."),
        ) {
            when {
                !bluetoothAllowed -> {
                    Text(bluetoothNotAllowedMessage)
                }
                peripherals.isEmpty() -> {
                    HCenter {
                        CircularProgressIndicator()
                    }
                }
                else -> {
                    peripherals.forEach { peripheral ->
                        val item = InlinePickerItem(
                            id = peripheral.identifier.toString(),
                            text = peripheral.name ?: localized("Unknown"),
                        )
                        key(item.id) {
                            FormRow(
                                onClick = {
                                    onChange(item.id)
                                    onDismiss()
                                },
                            ) {
                                Text(item.text)
                            }
                        }
                    }
                }
            }
        }
    }
}

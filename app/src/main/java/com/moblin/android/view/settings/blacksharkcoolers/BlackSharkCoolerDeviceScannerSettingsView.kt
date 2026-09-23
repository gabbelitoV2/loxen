package com.moblin.android.view.settings.blacksharkcoolers

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import com.moblin.android.LocalModel
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.bluetoothNotAllowedMessage
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.InlinePickerItem
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun BlackSharkCoolerDeviceScannerSettingsView(
    model: Model = LocalModel.current,
    scanner: BlackSharkCoolerScanner,
    onChange: (String) -> Unit,
    selectedId: String,
    onDismiss: () -> Unit,
) {
    val bluetoothAllowed by model.bluetoothAllowed.collectAsState()
    val discoveredPeripherals by scanner.discoveredPeripherals.collectAsState()

    DisposableEffect(Unit) {
        scanner.startScanningForDevices()
        onDispose {
            scanner.stopScanningForDevices()
        }
    }

    Form(title = localized("Device")) {
        Section {
            if (!bluetoothAllowed) {
                Text(bluetoothNotAllowedMessage)
            } else if (discoveredPeripherals.isEmpty()) {
                HCenter {
                    CircularProgressIndicator(color = formPalette().gray)
                }
            } else {
                val items = discoveredPeripherals
                    .filter { it.name?.contains("black shark", ignoreCase = true) == true }
                    .map { peripheral ->
                        InlinePickerItem(
                            id = peripheral.identifier,
                            text = peripheral.name ?: localized("Unknown"),
                        )
                    }
                for (item in items) {
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

class BlackSharkCoolerScanner {
    val discoveredPeripherals = MutableStateFlow<List<BlackSharkCoolerPeripheral>>(emptyList())

    fun startScanningForDevices() {
    }

    fun stopScanningForDevices() {
    }
}

data class BlackSharkCoolerPeripheral(
    val identifier: String,
    val name: String?,
)

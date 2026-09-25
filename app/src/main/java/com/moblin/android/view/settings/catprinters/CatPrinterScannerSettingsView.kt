package com.moblin.android.view.settings.catprinters

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
import com.moblin.android.integrations.catprinter.catPrinterScanner
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Button
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.formBodyStyle
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.platform.swiftui.rememberDismiss
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.bluetoothNotAllowedMessage
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.InlinePickerItem

@Composable
fun CatPrinterScannerSettingsView(
    model: Model = LocalModel.current,
    onChange: (String) -> Unit,
    selectedId: String,
) {
    val dismiss = rememberDismiss()
    val scanner = catPrinterScanner
    val discoveredPeripherals by scanner.discoveredPeripherals.collectAsState()
    val bluetoothAllowed by model.bluetoothAllowed.collectAsState()
    DisposableEffect(Unit) {
        scanner.startScanningForDevices()
        onDispose {
            scanner.stopScanningForDevices()
        }
    }
    Form(title = "Device") {
        Section {
            if (!bluetoothAllowed) {
                Text(bluetoothNotAllowedMessage, style = formBodyStyle)
            } else if (discoveredPeripherals.isEmpty()) {
                HCenter {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = formPalette().gray,
                        strokeWidth = 2.dp,
                    )
                }
            } else {
                val items = discoveredPeripherals.map { peripheral ->
                    InlinePickerItem(
                        id = peripheral.identifier.toString(),
                        text = peripheral.name ?: localized("Unknown"),
                    )
                }
                items.forEach { item ->
                    key(item.id) {
                        Button(
                            action = {
                                onChange(item.id)
                                dismiss()
                            },
                        ) {
                            Text(item.text, style = formBodyStyle)
                        }
                    }
                }
            }
        }
    }
}

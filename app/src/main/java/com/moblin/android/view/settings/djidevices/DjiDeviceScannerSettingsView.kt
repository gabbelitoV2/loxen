package com.moblin.android.view.settings.djidevices

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.moblin.android.integrations.dji.djidevice.DjiDeviceScanner
import com.moblin.android.localized
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.InlinePickerItem
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.bluetoothNotAllowedMessage
import com.moblin.android.LocalModel
import com.moblin.android.platform.swiftui.*

@Composable
fun DjiDeviceScannerSettingsView(
    model: Model = LocalModel.current,
    djiScanner: DjiDeviceScanner = DjiDeviceScanner.shared,
    onChange: (String) -> Unit,
    selectedId: String,
    onDismiss: () -> Unit,
) {
    val bluetoothAllowed by model.bluetoothAllowed.collectAsState()
    val discoveredDevices by djiScanner.discoveredDevices.collectAsState()
    val pickerItems = discoveredDevices.map { discoveredDevice ->
        InlinePickerItem(
            id = discoveredDevice.peripheral.identifier.toString(),
            text = discoveredDevice.peripheral.name ?: localized("Unknown"),
        )
    }
    Form(title = localized("Device")) {
        Section(
            footer = localized(
                "Make sure your DJI device is powered on and that no other apps are " +
                    "connected to it via Bluetooth. Make sure the Moblin device is " +
                    "relatively near the DJI device. If you still dont see your DJI " +
                    "device, turn your DJI device off and then on again.",
            ),
        ) {
            if (!bluetoothAllowed) {
                Text(bluetoothNotAllowedMessage)
            } else if (discoveredDevices.isEmpty()) {
                HCenter {
                    CircularProgressIndicator()
                }
            } else {
                Column {
                    pickerItems.forEach { item ->
                        FormButton(title = item.text) {
                            onChange(item.id)
                            onDismiss()
                        }
                    }
                }
            }
        }
    }
    DisposableEffect(Unit) {
        djiScanner.startScanningForDevices()
        onDispose {
            djiScanner.stopScanningForDevices()
        }
    }
}

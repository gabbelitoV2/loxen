package com.moblin.android.view.settings.djidevices

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.moblin.android.integrations.dji.djidevice.DjiDeviceScanner
import com.moblin.android.localized
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.InlinePickerItem
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.bluetoothNotAllowedMessage
import com.moblin.android.LocalModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DjiDeviceScannerSettingsView(
    model: Model = LocalModel.current,
    djiScanner: DjiDeviceScanner = DjiDeviceScanner,
    onChange: (String) -> Unit,
    selectedId: String,
    onDismiss: () -> Unit,
) {
    val bluetoothAllowed by model.bluetoothAllowed.collectAsState()
    val discoveredDevices by djiScanner.discoveredDevices.collectAsState()
    val pickerItems = discoveredDevices.map { discoveredDevice ->
        InlinePickerItem(
            id = discoveredDevice.peripheral.identifier.uuidString,
            text = discoveredDevice.peripheral.name ?: localized("Unknown"),
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Device") })
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            if (!bluetoothAllowed) {
                item {
                    Text(bluetoothNotAllowedMessage)
                }
            } else if (discoveredDevices.isEmpty()) {
                item {
                    HCenter {
                        CircularProgressIndicator()
                    }
                }
            } else {
                items(pickerItems) { item ->
                    TextButton(onClick = {
                        onChange(item.id)
                        onDismiss()
                    }) {
                        Text(item.text)
                    }
                }
            }
            item {
                Text(
                    "Make sure your DJI device is powered on and that no other apps are " +
                        "connected to it via Bluetooth. Make sure the Moblin device is " +
                        "relatively near the DJI device. If you still dont see your DJI " +
                        "device, turn your DJI device off and then on again.",
                )
            }
        }
    }
    LaunchedEffect(Unit) {
        djiScanner.startScanningForDevices()
    }
    DisposableEffect(Unit) {
        onDispose {
            djiScanner.stopScanningForDevices()
        }
    }
}

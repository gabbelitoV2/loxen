package com.moblin.android.view.settings.blacksharkcoolers

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.bluetoothNotAllowedMessage
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.InlinePickerItem
import com.moblin.android.LocalModel

@OptIn(ExperimentalMaterial3Api::class)
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

    LaunchedEffect(Unit) {
        scanner.startScanningForDevices()
    }

    DisposableEffect(Unit) {
        onDispose {
            scanner.stopScanningForDevices()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(localized("Device")) })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            if (!bluetoothAllowed) {
                Text(bluetoothNotAllowedMessage)
            } else if (discoveredPeripherals.isEmpty()) {
                HCenter {
                    CircularProgressIndicator()
                }
            } else {
                val pickerItems = discoveredPeripherals
                    .filter { it.name?.contains("black shark", ignoreCase = true) == true }
                    .map { peripheral ->
                        InlinePickerItem(
                            id = peripheral.identifier.toString(),
                            text = peripheral.name ?: localized("Unknown"),
                        )
                    }
                LazyColumn {
                    items(pickerItems) { item ->
                        Button(
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

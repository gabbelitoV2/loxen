package com.moblin.android.view.settings.catprinters

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
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.bluetoothNotAllowedMessage
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.InlinePickerItem
import com.moblin.android.LocalModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatPrinterScannerSettingsView(
    model: Model = LocalModel.current,
    scanner: CatPrinterScanner,
    selectedId: String,
    onChange: (String) -> Unit,
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
            } else if (discoveredPeripherals.isEmpty()) {
                item {
                    HCenter {
                        CircularProgressIndicator()
                    }
                }
            } else {
                items(
                    items = discoveredPeripherals.map { peripheral ->
                        InlinePickerItem(
                            id = peripheral.identifier.uuidString,
                            text = peripheral.name ?: localized("Unknown"),
                        )
                    },
                ) { item ->
                    TextButton(
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

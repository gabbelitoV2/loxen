package com.moblin.android.view.settings.workoutdevices

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.bluetoothNotAllowedMessage
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.InlinePickerItem
import com.moblin.android.workout.WorkoutDeviceScanner
import com.moblin.android.workout.workoutDeviceScanner
import com.moblin.android.LocalModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDeviceScannerSettingsView(
    model: Model = LocalModel.current,
    scanner: WorkoutDeviceScanner = workoutDeviceScanner,
    onChange: (String) -> Unit,
    selectedId: String,
    onSelectedIdChange: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val bluetoothAllowed = model.bluetoothAllowed.collectAsState().value
    val discoveredPeripherals = scanner.discoveredPeripherals.collectAsState().value

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
            TopAppBar(
                title = {
                    Text(text = "Device")
                },
                navigationIcon = {
                    IconButton(onClick = { onDismiss() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!bluetoothAllowed) {
                item {
                    Text(text = bluetoothNotAllowedMessage)
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
                            id = peripheral.identifier.toString(),
                            text = peripheral.name ?: localized("Unknown")
                        )
                    },
                    key = { item -> item.id }
                ) { item ->
                    TextButton(onClick = {
                        onChange(item.id)
                        onDismiss()
                    }) {
                        Text(text = item.text)
                    }
                }
            }
        }
    }
}

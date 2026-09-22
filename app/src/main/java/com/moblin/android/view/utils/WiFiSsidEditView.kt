package com.moblin.android.view.utils

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private class CurrentWiFiNetwork {
    val ssid = MutableStateFlow<String?>(null)

    val locationDenied = MutableStateFlow(false)

    fun locationManagerDidChangeAuthorization() {
        TODO("no Android counterpart for CoreLocation")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WiFiSsidEditView(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val currentNetwork = remember { CurrentWiFiNetwork() }
    var changed by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }

    val ssid by currentNetwork.ssid.collectAsState()
    val locationDenied by currentNetwork.locationDenied.collectAsState()

    val latestValue by rememberUpdatedState(value)
    val latestOnValueChange by rememberUpdatedState(onValueChange)
    val latestOnSubmit by rememberUpdatedState(onSubmit)

    fun submit(text: String) {
        submitted = true
        val trimmed = text.trim()
        latestOnValueChange(trimmed)
        latestOnSubmit(trimmed)
    }

    LaunchedEffect(ssid) {
        val currentSsid = ssid
        if (latestValue.isEmpty() && currentSsid != null) {
            latestOnValueChange(currentSsid)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (changed && !submitted) {
                submit(latestValue)
            }
        }
    }

    val currentSsid = ssid

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("SSID") })
        },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
        ) {
            item {
                OutlinedTextField(
                    value = value,
                    onValueChange = {
                        onValueChange(it)
                        changed = true
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            submit(value)
                            onDismiss()
                        },
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (currentSsid != null) {
                item {
                    Text(
                        text = "Current network",
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                item {
                    TextButton(
                        onClick = {
                            submit(currentSsid)
                            onDismiss()
                        },
                    ) {
                        Row {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = null,
                            )
                            Text(
                                text = currentSsid,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                            Spacer(Modifier.weight(1f))
                            if (currentSsid == value) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                )
                            }
                        }
                    }
                }
                item {
                    Text(
                        text = "The WiFi network this device is currently connected to.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            } else if (locationDenied) {
                item {
                    Text(
                        text = "⚠️ Allow Moblin to access your location in iOS Settings to see the current WiFi network.",
                    )
                }
            }
        }
    }
}

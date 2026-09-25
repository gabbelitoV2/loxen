package com.moblin.android.view.utils

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.formBodyStyle
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.utils.fetchCurrentWiFiSsid
import kotlinx.coroutines.flow.MutableStateFlow

private class CurrentWiFiNetwork : com.moblin.android.platform.corelocation.CLLocationManagerDelegate {
    val ssid = MutableStateFlow<String?>(null)

    val locationDenied = MutableStateFlow(false)
    private val locationManager = com.moblin.android.platform.corelocation.CLLocationManager().also { it.delegate = this }

    override fun locationManagerDidChangeAuthorization(manager: com.moblin.android.platform.corelocation.CLLocationManager) {
        when (locationManager.authorizationStatus) { com.moblin.android.platform.corelocation.CLAuthorizationStatus.notDetermined -> locationManager.requestWhenInUseAuthorization(); com.moblin.android.platform.corelocation.CLAuthorizationStatus.denied, com.moblin.android.platform.corelocation.CLAuthorizationStatus.restricted -> locationDenied.value = true; else -> fetchCurrentWiFiSsid { newSsid ->
            ssid.value = newSsid }
        }
    }
}

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

    LaunchedEffect(Unit) {
        Unit
    }

    LaunchedEffect(ssid) {
        val currentSsid = ssid
        if (latestValue.isEmpty() && currentSsid != null) {
            changed = true
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

    val palette = formPalette()
    val currentSsid = ssid

    Form(title = "SSID") {
        Section {
            FormRow {
                BasicTextField(
                    value = value,
                    onValueChange = {
                        onValueChange(it)
                        changed = true
                    },
                    modifier = Modifier.weight(1f),
                    textStyle = formBodyStyle.copy(color = palette.label),
                    singleLine = true,
                    cursorBrush = SolidColor(palette.accent),
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
                )
            }
        }
        if (currentSsid != null) {
            Section(
                header = "Current network",
                footer = "The WiFi network this device is currently connected to.",
            ) {
                FormRow(
                    onClick = {
                        submit(currentSsid)
                        onDismiss()
                    },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SystemImage("wifi", fontSize = 17.sp, tint = palette.accent)
                        Text(text = currentSsid, color = palette.accent)
                        Spacer(Modifier.weight(1f))
                        if (currentSsid == value) {
                            SystemImage("checkmark", fontSize = 17.sp, tint = palette.accent)
                        }
                    }
                }
            }
        } else if (locationDenied) {
            Section {
                Text(
                    "⚠️ Allow Moblin to access your location in iOS Settings to see the current WiFi network.",
                )
            }
        }
    }
}

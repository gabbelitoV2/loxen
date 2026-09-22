package com.moblin.android.view.settings.streams.stream.whip

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsHttpHeader
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.various.settings.SettingsStreamWhip
import com.moblin.android.various.settings.SettingsStreamWhipHttpTransport
import com.moblin.android.view.utils.RemoteControlAssistantShortcutView
import com.moblin.android.view.utils.ShortcutSectionView
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.LocalModel

private fun getBearerToken(headers: List<SettingsHttpHeader>): String {
    val authorization = headers.firstOrNull { it.name == "Authorization" } ?: return ""
    val match = Regex("^Bearer (.*)$").find(authorization.value) ?: return ""
    return match.groupValues[1]
}

private fun setBearerToken(
    model: Model,
    stream: SettingsStream,
    whip: SettingsStreamWhip,
    token: String,
) {
    val value = "Bearer $token"
    val index = whip.headers.indexOfFirst { it.name == "Authorization" }
    if (index != -1) {
        whip.headers[index].value = value
    } else {
        whip.headers.add(SettingsHttpHeader(name = "Authorization", value = value))
    }
    Unit
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamWhipSettingsView(model: Model = LocalModel.current, stream: SettingsStream, whip: SettingsStreamWhip) {
    val headers = whip.headers
    val httpTransport = whip.httpTransport
    val isLive by model.isLive.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    val disabled = stream.enabled && isLive
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("WHIP") })
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            item {
                TextEditNavigationView(
                    title = localized("Bearer token"),
                    value = getBearerToken(headers),
                    onSubmit = { token -> setBearerToken(model, stream, whip, token) },
                    sensitive = true,
                )
            }
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { isExpanded -> if (!disabled) expanded = isExpanded },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        OutlinedTextField(
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                            readOnly = true,
                            value = httpTransport.toString(),
                            onValueChange = {},
                            label = { Text(localized("HTTP transport")) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            enabled = !disabled,
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                        ) {
                            SettingsStreamWhipHttpTransport.entries.forEach { transport ->
                                DropdownMenuItem(
                                    text = { Text(transport.toString()) },
                                    onClick = {
                                        whip.httpTransport = transport
                                        expanded = false
                                        Unit
                                    },
                                )
                            }
                        }
                    }
                }
            }
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        localized(
                            "Select ${SettingsStreamWhipHttpTransport.standard.toString()} to use standard WHIP.",
                        ),
                    )
                    Text("")
                    Text(
                        localized(
                            "Select ${SettingsStreamWhipHttpTransport.remoteControl.toString()} to exchange " +
                                "connection establishment information via the remote control. Configure this " +
                                "device as remote control assistant, and the device you are streaming to as " +
                                "remote control streamer.",
                        ),
                    )
                }
            }
            if (httpTransport == SettingsStreamWhipHttpTransport.remoteControl) {
                item {
                    ShortcutSectionView {
                        RemoteControlAssistantShortcutView(model = model)
                    }
                }
            }
        }
    }
}

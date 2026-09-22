package com.moblin.android.view.settings.moblink

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.moblin.android.localized
import com.moblin.android.common.various.isValidPort
import com.moblin.android.common.various.isValidWebSocketUrl
import com.moblin.android.moblink.MoblinkScannerStreamer
import com.moblin.android.moblink.getMoblinkRelayId
import com.moblin.android.moblink.moblinkRelayResetId
import com.moblin.android.various.model.Moblink
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.StatusOther
import com.moblin.android.various.model.reloadMoblinkRelay
import com.moblin.android.various.model.reloadMoblinkStreamer
import com.moblin.android.various.network.DefaultTcpPorts
import com.moblin.android.various.settings.SettingsMoblinkRelay
import com.moblin.android.various.settings.SettingsMoblinkStreamer
import com.moblin.android.view.utils.CopyToClipboardButtonView
import com.moblin.android.view.utils.NameEditView
import com.moblin.android.view.utils.TextButtonView
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.view.utils.TextItemLocalizedView
import com.moblin.android.view.utils.UrlsView
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

private fun isAllowedPassword(password: String): Boolean {
    return password.isNotEmpty()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PasswordView(
    model: Model = LocalModel.current,
    initialValue: String,
    onSubmit: (String) -> Unit,
) {
    val isLive by model.isLive.collectAsState()
    var value by remember { mutableStateOf(initialValue) }
    var changed by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    fun submit() {
        value = value.trim()
        if (isAllowedPassword(password = value)) {
            submitted = true
            onSubmit(value)
        }
    }

    fun createMessage(): String? {
        return if (isAllowedPassword(password = value)) {
            null
        } else {
            "Not long and random enough"
        }
    }

    LaunchedEffect(value) {
        changed = true
        message = createMessage()
    }

    DisposableEffect(Unit) {
        onDispose {
            if (changed && !submitted) {
                submit()
            }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Password") }) }) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        modifier = Modifier.weight(1f),
                        enabled = !isLive,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                    )
                    CopyToClipboardButtonView(text = value)
                }
            }
            item {
                message?.let {
                    Text(
                        text = it,
                        color = Color.Red,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            item {
                TextButtonView(
                    title = "Reset to default",
                    action = {
                        value = "1234"
                        submit()
                    },
                )
            }
        }
    }
}

@Composable
private fun RelayStreamerServerView(
    server: MoblinkScannerStreamer,
    streamerUrl: String,
    onStreamerUrlChange: (String) -> Unit,
    submitUrl: (String) -> Unit,
) {
    Column {
        Text(server.name, style = MaterialTheme.typography.titleSmall)
        server.urls.forEach { url ->
            TextButtonView(
                title = url,
                action = {
                    onStreamerUrlChange(url)
                    submitUrl(url)
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RelayStreamerUrlView(
    model: Model = LocalModel.current,
    moblink: Moblink,
    initialStreamerUrl: String,
    onDismiss: () -> Unit,
) {
    val discoveredStreamers by moblink.scannerDiscoveredStreamers.collectAsState()
    var streamerUrl by remember { mutableStateOf(initialStreamerUrl) }

    fun submitUrl(value: String) {
        if (isValidWebSocketUrl(value = value) != null) {
            streamerUrl = model.database.moblink.relay.url.value
            return
        }
        model.database.moblink.relay.url.value = value
        model.reloadMoblinkRelay()
        onDismiss()
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Streamer URL") }) }) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            item {
                OutlinedTextField(
                    value = streamerUrl,
                    onValueChange = { streamerUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = {
                        Text("ws://32.143.32.12:${DefaultTcpPorts.remoteControlAssistant}")
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { submitUrl(streamerUrl) }),
                )
            }
            if (discoveredStreamers.isEmpty()) {
                item {
                    Text("No streamers discovered yet on your local network.")
                }
            } else {
                items(discoveredStreamers) { server ->
                    RelayStreamerServerView(
                        server = server,
                        streamerUrl = streamerUrl,
                        onStreamerUrlChange = { streamerUrl = it },
                        submitUrl = { submitUrl(it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RelayView(
    model: Model = LocalModel.current,
    relay: SettingsMoblinkRelay,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val isLive by model.isLive.collectAsState()
    val enabled by relay.enabled.collectAsState()
    val name by relay.name.collectAsState()
    val manual by relay.manual.collectAsState()
    val url by relay.url.collectAsState()
    var relayId by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        relayId = getMoblinkRelayId()
    }
    LaunchedEffect(enabled) {
        model.reloadMoblinkRelay()
    }
    LaunchedEffect(name) {
        model.reloadMoblinkRelay()
    }
    LaunchedEffect(manual) {
        model.reloadMoblinkRelay()
    }

    Column {
        Text("Relay", style = MaterialTheme.typography.titleSmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Enabled")
            Spacer(Modifier.weight(1f))
            Switch(
                checked = enabled,
                onCheckedChange = { relay.enabled.value = it },
            )
        }
        NameEditView(
            name = name,
            onNameChange = { relay.name.value = it },
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Manual")
            Spacer(Modifier.weight(1f))
            Switch(
                checked = manual,
                onCheckedChange = { relay.manual.value = it },
                enabled = !isLive,
            )
        }
        if (manual) {
            Box(modifier = Modifier.clickable { onNavigate("relayStreamerUrl") }) {
                TextItemLocalizedView(name = "Streamer URL", value = url)
            }
        }
        TextButtonView(
            title = "Reset id",
            action = {
                moblinkRelayResetId()
                model.reloadMoblinkRelay()
                relayId = getMoblinkRelayId()
            },
        )
        Column(horizontalAlignment = Alignment.Start) {
            Text(
                "Enable this on the device you want to use as the extra bonding connection. " +
                    "The device must have cellular data enabled.",
            )
            Text("")
            Text(
                text = "ID: $relayId",
                maxLines = 1,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun StreamerView(
    model: Model = LocalModel.current,
    streamer: SettingsMoblinkStreamer,
) {
    val isLive by model.isLive.collectAsState()
    val enabled by streamer.enabled.collectAsState()
    val port by streamer.port.collectAsState()

    fun submitPort(value: String) {
        val parsedPort = value.trim().toIntOrNull()?.takeIf { it in 0..65535 }
        if (parsedPort == null) {
            model.makePortErrorToast(port = value)
            return
        }
        streamer.port.value = parsedPort
        model.reloadMoblinkStreamer()
    }

    LaunchedEffect(enabled) {
        model.reloadMoblinkStreamer()
    }

    Column {
        Text("Streamer", style = MaterialTheme.typography.titleSmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Enabled")
            Spacer(Modifier.weight(1f))
            Switch(
                checked = enabled,
                onCheckedChange = { streamer.enabled.value = it },
                enabled = !isLive,
            )
        }
        TextEditNavigationView(
            title = localized("Server port"),
            value = port.toString(),
            onChange = ::isValidPort,
            onSubmit = { submitPort(it) },
            keyboardType = KeyboardType.Number,
            placeholder = DefaultTcpPorts.moblinkStreamer.toString(),
        )
        Text(
            "Enable this on your streaming device. Configure relay devices to connect to this device.",
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoblinkSettingsView(
    model: Model = LocalModel.current,
    status: StatusOther,
    streamer: SettingsMoblinkStreamer,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val password = model.database.moblink.password
    val streamerEnabled by streamer.enabled.collectAsState()
    val streamerPort by streamer.port.collectAsState()

    fun submitPassword(value: String) {
        model.database.moblink.password = value.trim()
        model.reloadMoblinkRelay()
        model.reloadMoblinkStreamer()
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Moblink") }) }) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            item {
                Text(
                    "Use phones as additional SRTLA and RIST bonding connections. " +
                        "Install Moblink on Android phones to use them.",
                )
            }
            item {
                Box(modifier = Modifier.clickable { onNavigate("password") }) {
                    TextItemLocalizedView(
                        name = "Password",
                        value = password,
                        sensitive = true,
                    )
                }
            }
            item {
                Text(
                    "Used by both relay and streamer devices. Copy the streamer's password to " +
                        "the relay device.",
                )
            }
            item {
                RelayView(
                    model = model,
                    relay = model.database.moblink.relay,
                    onNavigate = onNavigate,
                )
            }
            item {
                StreamerView(model = model, streamer = streamer)
            }
            if (streamerEnabled) {
                item {
                    UrlsView(
                        status = status,
                        formatUrl = { "ws://$it:$streamerPort" },
                    )
                }
                item {
                    Text(
                        "Enter one of the URL:s as \"Streamer URL\" in the relay device to " +
                            "use it as an additional bonding connection.",
                    )
                }
            }
        }
    }
}

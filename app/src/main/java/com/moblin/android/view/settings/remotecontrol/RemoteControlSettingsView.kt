package com.moblin.android.view.settings.remotecontrol

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.moblin.android.common.various.isValidPort
import com.moblin.android.common.various.isValidWebSocketUrl
import com.moblin.android.common.various.urlImage
import com.moblin.android.localized
import com.moblin.android.various.model.InterfaceType
import com.moblin.android.various.model.IpType
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.StatusOther
import com.moblin.android.various.model.fallbackStream
import com.moblin.android.various.network.DefaultTcpPorts
import com.moblin.android.various.network.makeMdnsHostname
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsRemoteControl
import com.moblin.android.various.settings.SettingsRemoteControlAssistant
import com.moblin.android.various.settings.SettingsRemoteControlServerRelay
import com.moblin.android.various.settings.SettingsRemoteControlStreamer
import com.moblin.android.various.settings.SettingsRemoteControlStreamerUrl
import com.moblin.android.various.settings.SettingsRemoteControlWeb
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.various.utils.makeOffsets
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.utils.ContextMenuDeleteButtonView
import com.moblin.android.view.utils.CopyToClipboardButtonView
import com.moblin.android.view.utils.DraggableItemTextView
import com.moblin.android.view.utils.ExternalUrlButtonView
import com.moblin.android.view.utils.NameEditView
import com.moblin.android.view.utils.ShortcutSectionView
import com.moblin.android.view.utils.SliderView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.view.utils.TextButtonView
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.view.utils.TextItemLocalizedView
import com.moblin.android.view.utils.UrlCopyView
import com.moblin.android.view.utils.UrlsIpv4View
import com.moblin.android.view.utils.UrlsIpv6View

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PasswordView(
    value: String,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var valueState by remember { mutableStateOf(value) }
    var changed by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }

    fun submit() {
        val trimmed = valueState.trim()
        valueState = trimmed
        submitted = true
        onSubmit(trimmed)
        onDismiss()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Password") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = valueState,
                    onValueChange = {
                        valueState = it
                        changed = true
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                )
                CopyToClipboardButtonView(text = valueState)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (changed && !submitted) {
                submit()
            }
        }
    }
}

@Composable
private fun AssistantUrlSettingsView(
    model: Model,
    streamer: SettingsRemoteControlStreamer,
    url: SettingsRemoteControlStreamerUrl,
    onDelete: () -> Unit,
) {
    Button(onClick = {
        streamer.name = url.name
        streamer.url = url.url
        model.reloadRemoteControlStreamer()
        model.reloadConnections()
    }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(url.name, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.weight(1f))
            Text(url.url, color = MaterialTheme.colorScheme.onSurface)
            if (streamer.url == url.url) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Blue)
            }
        }
    }
    ContextMenuDeleteButtonView {
        onDelete()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UrlSettingsInnerView(
    model: Model,
    database: Database,
    streamer: SettingsRemoteControlStreamer,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Assistant") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            NameEditView(
                name = streamer.name,
                onChange = { name -> submitStreamerName(streamer = streamer, name = name) },
            )
            TextEditNavigationView(
                title = localized("URL"),
                value = streamer.url,
                onChange = ::isValidWebSocketUrl,
                onSubmit = { value ->
                    submitStreamerUrl(model = model, streamer = streamer, value = value)
                },
                footers = listOf(
                    localized("Enter assistant's address and port. For example ws://132.23.43.43:2345."),
                ),
                placeholder = "ws://32.143.32.12:${DefaultTcpPorts.remoteControlAssistant}",
            )
            streamer.savedUrls.forEach { url ->
                AssistantUrlSettingsView(
                    model = model,
                    streamer = streamer,
                    url = url,
                    onDelete = { streamer.savedUrls.remove(url) },
                )
            }
            Text("Saved URLs", style = MaterialTheme.typography.titleSmall)
            SwipeLeftToDeleteHelpView(kind = localized("a URL"))
        }
    }
}

@Composable
private fun RemoteControlSettingsStreamerView(
    model: Model,
    streamer: SettingsRemoteControlStreamer,
    onNavigate: (String) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Enabled")
        Spacer(Modifier.weight(1f))
        Switch(
            checked = streamer.enabled,
            onCheckedChange = { enabled ->
                streamer.enabled = enabled
                model.reloadRemoteControlStreamer()
                model.reloadConnections()
            },
        )
    }
    Box(modifier = Modifier.clickable { onNavigate("Assistant") }) {
        TextItemLocalizedView(
            name = "Assistant",
            value = if (streamer.name.isEmpty()) streamer.url else streamer.name,
        )
    }
    Text(
        "Enable to allow an assistant to monitor and control this device from a different device."
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Reliable chat and events")
        Spacer(Modifier.weight(1f))
        Switch(
            checked = streamer.reliableChatAndEvents,
            onCheckedChange = { enabled ->
                streamer.reliableChatAndEvents = enabled
                model.reloadRemoteControlStreamer()
                model.reloadConnections()
            },
        )
    }
    Column {
        Text(
            "Receive chat and events from the assistant instead of directly from the streaming platform."
        )
        Text("")
        Text("Only works for Twitch.")
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Preview FPS")
        SliderView(
            value = streamer.previewFps,
            minimum = 0f,
            maximum = 5f,
            step = 1f,
            onSubmit = { value ->
                submitStreamerPreviewFps(model = model, streamer = streamer, value = value)
            },
            width = 20f,
            format = ::formatStreamerPreviewFps,
        )
    }
}

@Composable
private fun RemoteControlUrlsView(
    relay: SettingsRemoteControlServerRelay,
    port: UShort,
    status: StatusOther,
    onNavigate: (String) -> Unit,
) {
    Text("URLs", modifier = Modifier.clickable { onNavigate("URLs") })
    Text(
        "Enter one of the URLs as \"Assistant URL\" in the streamer device to connect to this device."
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemoteControlUrlsForm(
    relay: SettingsRemoteControlServerRelay,
    port: UShort,
    status: StatusOther,
) {
    fun formatUrl(ip: String): String = "ws://$ip:$port"

    Scaffold(
        topBar = { TopAppBar(title = { Text("URLs") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            if (relay.enabled) {
                Text("Relay", style = MaterialTheme.typography.titleSmall)
                UrlCopyView("${relay.baseUrl}/streamer/${relay.bridgeId}", image = "globe")
            }
            UrlsIpv4View(status = status, formatUrl = { ip -> formatUrl(ip) })
            UrlsIpv6View(status = status, formatUrl = { ip -> formatUrl(ip) })
        }
    }
}

@Composable
private fun StreamerView(
    model: Model,
    remoteControlSettings: SettingsRemoteControl,
    streamer: SettingsRemoteControlAssistant,
    streamerRelay: SettingsRemoteControlServerRelay,
    onNavigate: (String) -> Unit,
) {
    DraggableItemTextView(
        name = streamer.name,
        modifier = Modifier.clickable { onNavigate("Streamer") },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StreamerForm(
    model: Model,
    remoteControlSettings: SettingsRemoteControl,
    streamer: SettingsRemoteControlAssistant,
    streamerRelay: SettingsRemoteControlServerRelay,
    onNavigate: (String) -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Streamer") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            NameEditView(
                name = streamer.name,
                existingNames = remoteControlSettings.streamers,
                onChange = { name -> streamer.name = name },
            )
            Text("Assistant", style = MaterialTheme.typography.titleSmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Enabled")
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = streamer.enabled,
                    onCheckedChange = { enabled ->
                        streamer.enabled = enabled
                        reloadIfEnabled(
                            model = model,
                            remoteControlSettings = remoteControlSettings,
                            streamer = streamer,
                            streamerRelay = streamerRelay,
                        )
                    },
                )
            }
            TextEditNavigationView(
                title = localized("Server port"),
                value = streamer.port.toString(),
                onChange = ::isValidPort,
                onSubmit = { value ->
                    submitAssistantPort(
                        model = model,
                        remoteControlSettings = remoteControlSettings,
                        streamer = streamer,
                        streamerRelay = streamerRelay,
                        value = value,
                    )
                },
                keyboardType = KeyboardType.Number,
                placeholder = DefaultTcpPorts.remoteControlAssistant.toString(),
            )
            Text("Relay", style = MaterialTheme.typography.titleSmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Enabled")
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = streamerRelay.enabled,
                    onCheckedChange = { enabled ->
                        streamerRelay.enabled = enabled
                        reloadIfEnabled(
                            model = model,
                            remoteControlSettings = remoteControlSettings,
                            streamer = streamer,
                            streamerRelay = streamerRelay,
                        )
                    },
                )
            }
            TextEditNavigationView(
                title = localized("Base URL"),
                value = streamerRelay.baseUrl,
                onChange = ::isValidWebSocketUrl,
                onSubmit = { value ->
                    submitAssistantRelayUrl(
                        model = model,
                        remoteControlSettings = remoteControlSettings,
                        streamer = streamer,
                        streamerRelay = streamerRelay,
                        value = value,
                    )
                },
            )
            TextEditNavigationView(
                title = localized("Bridge id"),
                value = streamerRelay.bridgeId,
                onChange = ::changeAssistantRelayBridgeId,
                onSubmit = { value ->
                    submitAssistantRelayBridgeId(
                        model = model,
                        remoteControlSettings = remoteControlSettings,
                        streamer = streamer,
                        streamerRelay = streamerRelay,
                        value = value,
                    )
                },
                sensitive = true,
            )
            Text("Use a relay server when the assistant is behind CGNAT or similar.")
            if (streamer.enabled) {
                RemoteControlUrlsForm(
                    relay = streamerRelay,
                    port = streamer.port,
                    status = model.statusOther,
                )
            }
        }
    }
}

@Composable
private fun StreamerItemView(
    streamer: SettingsRemoteControlAssistant,
    onClick: () -> Unit,
) {
    DropdownMenuItem(text = { Text(streamer.name) }, onClick = onClick)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteControlStreamersView(
    model: Model,
    remoteControlSettings: SettingsRemoteControl,
    onNavigate: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedStreamer = remoteControlSettings.selectedStreamer
    val selectedName = remoteControlSettings.streamers
        .firstOrNull { it.id == selectedStreamer }?.name ?: "-- None --"

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Current streamer") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("-- None --") },
                onClick = {
                    remoteControlSettings.selectedStreamer = null
                    onStreamerChanged(model = model, remoteControlSettings = remoteControlSettings)
                    expanded = false
                },
            )
            remoteControlSettings.streamers.forEach { streamer ->
                StreamerItemView(
                    streamer = streamer,
                    onClick = {
                        remoteControlSettings.selectedStreamer = streamer.id
                        onStreamerChanged(
                            model = model,
                            remoteControlSettings = remoteControlSettings,
                        )
                        expanded = false
                    },
                )
            }
        }
    }
    Text(
        "Select a streamer. Once the streamer has connected to this device, this device can monitor and control it."
    )
    remoteControlSettings.streamers.forEach { streamer ->
        Box {
            StreamerView(
                model = model,
                remoteControlSettings = remoteControlSettings,
                streamer = streamer,
                streamerRelay = streamer.relay,
                onNavigate = onNavigate,
            )
            ContextMenuDeleteButtonView {
                val offsets = makeOffsets(remoteControlSettings.streamers, streamer.id)
                if (offsets != null) {
                    deleteStreamer(
                        model = model,
                        remoteControlSettings = remoteControlSettings,
                        offsets = offsets,
                    )
                }
            }
        }
    }
    TextButtonView("Create") {
        val streamer = SettingsRemoteControlAssistant()
        streamer.name = makeUniqueName(
            name = SettingsRemoteControlAssistant.baseName,
            existingNames = remoteControlSettings.streamers,
        )
        streamer.enabled = true
        streamer.port = DefaultTcpPorts.remoteControlAssistant.toUShort()
        remoteControlSettings.streamers.add(streamer)
    }
    SwipeLeftToDeleteHelpView(kind = localized("a streamer"))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssistantForm(
    model: Model,
    remoteControlSettings: SettingsRemoteControl,
    onNavigate: (String) -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Assistant") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            RemoteControlStreamersView(
                model = model,
                remoteControlSettings = remoteControlSettings,
                onNavigate = onNavigate,
            )
            Text("Websites", style = MaterialTheme.typography.titleSmall)
            ExternalUrlButtonView(
                url = "https://moblin.mys-lang.org/moblin-remote-control-relay/assistant.html",
            ) {
                Text("Moblin Remote Control Assistant")
            }
            ExternalUrlButtonView(url = "https://moblinremote.com/") {
                Text("Moblin Remote Control")
            }
            Text("Alternatively, use a website as assistant.")
        }
    }
}

private fun formatUrl(ip: String, port: UShort): String {
    return if (port == 80u.toUShort()) {
        "http://$ip"
    } else {
        "http://$ip:$port"
    }
}

@Composable
private fun WebUrlsView(
    web: SettingsRemoteControlWeb,
    status: StatusOther,
    onNavigate: (String) -> Unit,
) {
    Text("URLs", modifier = Modifier.clickable { onNavigate("URLs") })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WebUrlsForm(
    web: SettingsRemoteControlWeb,
    status: StatusOther,
) {
    fun format(ip: String): String = formatUrl(ip = ip, port = web.port)

    Scaffold(
        topBar = { TopAppBar(title = { Text("URLs") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            Text("mDNS", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = web.deviceName,
                onValueChange = { web.deviceName = it },
                label = { Text("My device name") },
            )
            if (web.deviceName.isNotEmpty()) {
                UrlCopyView(
                    format(makeMdnsHostname(deviceName = web.deviceName)),
                    image = "network",
                )
            }
            Text("Copy your device name from iOS settings.")
            UrlsIpv4View(status = status, formatUrl = { ip -> format(ip) })
            UrlsIpv6View(status = status, formatUrl = { ip -> format(ip) })
        }
    }
}

@Composable
fun RemoteControlWebDefaultUrlView(
    web: SettingsRemoteControlWeb,
    status: StatusOther,
    path: String,
) {
    fun format(ip: String): String = formatUrl(ip = ip, port = web.port)

    if (web.deviceName.isNotEmpty()) {
        UrlCopyView(
            format(makeMdnsHostname(deviceName = web.deviceName)) + path,
            image = "network",
        )
    } else {
        val ipStatus = status.ipStatuses.firstOrNull {
            it.ipType == IpType.ipv4 &&
                (it.interfaceType == InterfaceType.wifi || it.interfaceType == InterfaceType.wiredEthernet)
        }
        if (ipStatus != null) {
            UrlCopyView(
                format(ipStatus.ipType.formatAddress(ipStatus.ip)) + path,
                image = urlImage(ipStatus.interfaceType),
            )
        }
    }
}

@Composable
fun RemoteControlSettingsWebView(
    model: Model,
    web: SettingsRemoteControlWeb,
    onNavigate: (String) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Enabled")
        Spacer(Modifier.weight(1f))
        Switch(
            checked = web.enabled,
            onCheckedChange = { enabled ->
                web.enabled = enabled
                model.reloadRemoteControlWeb()
            },
        )
    }
    Text(
        "Enable to monitor and control this device from another device using a web browser. There is no authentication, nor encryption, so be careful."
    )
    TextEditNavigationView(
        title = localized("Server port"),
        value = web.port.toString(),
        onChange = ::isValidPort,
        onSubmit = { value -> submitPort(model = model, web = web, value = value) },
        keyboardType = KeyboardType.Number,
        placeholder = "80",
        enabled = !web.enabled,
    )
    if (web.enabled) {
        WebUrlsView(web = web, status = model.statusOther, onNavigate = onNavigate)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteControlSettingsView(
    model: Model,
    database: Database,
    stream: SettingsStream,
    onStreamChange: (SettingsStream) -> Unit,
    onNavigate: (String) -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Remote control") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            Text("Control and monitor Moblin from another device.")
            Text("General", style = MaterialTheme.typography.titleSmall)
            Box(modifier = Modifier.clickable { onNavigate("Password") }) {
                TextItemLocalizedView(
                    name = "Password",
                    value = database.remoteControl.password,
                    sensitive = true,
                )
            }
            Text("Used by both streamer and assistant.")
            Text("Streamer", modifier = Modifier.clickable { onNavigate("Streamer") })
            Text("Assistant", modifier = Modifier.clickable { onNavigate("Assistant") })
            Text("Web", modifier = Modifier.clickable { onNavigate("Web") })
            if (stream !== fallbackStream) {
                ShortcutSectionView {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onNavigate("OBS remote control") },
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Text("OBS remote control", modifier = Modifier.weight(1f))
                        Switch(
                            checked = stream.obsWebSocketEnabled,
                            onCheckedChange = { enabled ->
                                stream.obsWebSocketEnabled = enabled
                                onStreamChange(stream)
                                model.obsWebSocketEnabledUpdated()
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun submitStreamerName(streamer: SettingsRemoteControlStreamer, name: String) {
    streamer.name = name
    streamer.savedUrls.firstOrNull { it.url == streamer.url }?.name = name
}

private fun submitStreamerUrl(
    model: Model,
    streamer: SettingsRemoteControlStreamer,
    value: String,
) {
    if (isValidWebSocketUrl(value) != null) {
        return
    }
    streamer.url = value
    model.reloadRemoteControlStreamer()
    model.reloadConnections()
    if (streamer.savedUrls.any { it.url == value }) {
        return
    }
    val url = SettingsRemoteControlStreamerUrl()
    url.name = streamer.name
    url.url = value
    streamer.savedUrls.add(url)
}

private fun submitStreamerPreviewFps(
    model: Model,
    streamer: SettingsRemoteControlStreamer,
    value: Float,
) {
    streamer.previewFps = value
    model.setLowFpsImage()
}

private fun formatStreamerPreviewFps(value: Float): String {
    return value.toInt().toString()
}

private fun reloadIfEnabled(
    model: Model,
    remoteControlSettings: SettingsRemoteControl,
    streamer: SettingsRemoteControlAssistant,
    streamerRelay: SettingsRemoteControlServerRelay,
) {
    if (streamer.id != remoteControlSettings.selectedStreamer) {
        return
    }
    val assistant = model.database.remoteControl.assistant
    assistant.enabled = streamer.enabled
    assistant.port = streamer.port
    assistant.relay.enabled = streamerRelay.enabled
    assistant.relay.baseUrl = streamerRelay.baseUrl
    assistant.relay.bridgeId = streamerRelay.bridgeId
    model.reloadRemoteControlRelay()
    model.reloadRemoteControlAssistant()
}

private fun submitAssistantPort(
    model: Model,
    remoteControlSettings: SettingsRemoteControl,
    streamer: SettingsRemoteControlAssistant,
    streamerRelay: SettingsRemoteControlServerRelay,
    value: String,
) {
    val port = value.toUShortOrNull() ?: return
    streamer.port = port
    reloadIfEnabled(
        model = model,
        remoteControlSettings = remoteControlSettings,
        streamer = streamer,
        streamerRelay = streamerRelay,
    )
}

private fun submitAssistantRelayUrl(
    model: Model,
    remoteControlSettings: SettingsRemoteControl,
    streamer: SettingsRemoteControlAssistant,
    streamerRelay: SettingsRemoteControlServerRelay,
    value: String,
) {
    streamerRelay.baseUrl = value
    reloadIfEnabled(
        model = model,
        remoteControlSettings = remoteControlSettings,
        streamer = streamer,
        streamerRelay = streamerRelay,
    )
}

private fun changeAssistantRelayBridgeId(value: String): String? {
    if (value.isEmpty()) {
        return localized("Empty")
    }
    return null
}

private fun submitAssistantRelayBridgeId(
    model: Model,
    remoteControlSettings: SettingsRemoteControl,
    streamer: SettingsRemoteControlAssistant,
    streamerRelay: SettingsRemoteControlServerRelay,
    value: String,
) {
    streamerRelay.bridgeId = value
    reloadIfEnabled(
        model = model,
        remoteControlSettings = remoteControlSettings,
        streamer = streamer,
        streamerRelay = streamerRelay,
    )
}

private fun onStreamerChanged(model: Model, remoteControlSettings: SettingsRemoteControl) {
    val assistant = model.database.remoteControl.assistant
    val streamer = remoteControlSettings.streamers
        .firstOrNull { it.id == remoteControlSettings.selectedStreamer }
    if (streamer != null) {
        assistant.enabled = streamer.enabled
        assistant.port = streamer.port
        assistant.relay.enabled = streamer.relay.enabled
        assistant.relay.baseUrl = streamer.relay.baseUrl
        assistant.relay.bridgeId = streamer.relay.bridgeId
    } else {
        assistant.enabled = false
        assistant.relay.enabled = false
    }
    model.reloadRemoteControlRelay()
    model.reloadRemoteControlAssistant()
}

private fun deleteStreamer(
    model: Model,
    remoteControlSettings: SettingsRemoteControl,
    offsets: Int,
) {
    if (offsets in remoteControlSettings.streamers.indices) {
        remoteControlSettings.streamers.removeAt(offsets)
    }
    val selectedStreamer = remoteControlSettings.selectedStreamer ?: return
    if (remoteControlSettings.streamers.any { it.id == selectedStreamer }) {
        return
    }
    remoteControlSettings.selectedStreamer = null
    onStreamerChanged(model = model, remoteControlSettings = remoteControlSettings)
}

private fun <T> moveItems(items: MutableList<T>, fromIndex: Int, toIndex: Int) {
    if (fromIndex == toIndex) {
        return
    }
    if (fromIndex !in items.indices) {
        return
    }
    val item = items.removeAt(fromIndex)
    val index = if (toIndex > fromIndex) toIndex - 1 else toIndex
    items.add(index.coerceIn(0, items.size), item)
}

private fun submitPort(model: Model, web: SettingsRemoteControlWeb, value: String) {
    val port = value.toUShortOrNull() ?: return
    if (port >= UShort.MAX_VALUE) {
        return
    }
    web.port = port
    model.reloadRemoteControlWeb()
}

private fun submitPassword(model: Model, database: Database, value: String) {
    database.remoteControl.password = value.trim()
    model.reloadRemoteControlStreamer()
    model.reloadRemoteControlAssistant()
}

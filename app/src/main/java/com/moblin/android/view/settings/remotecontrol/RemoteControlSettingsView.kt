package com.moblin.android.view.settings.remotecontrol

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.common.various.isValidPort
import com.moblin.android.common.various.isValidWebSocketUrl
import com.moblin.android.localized
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.ContextMenu
import com.moblin.android.platform.swiftui.ForEach
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.IndexSet
import com.moblin.android.platform.swiftui.Label
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.formBodyStyle
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.platform.swiftui.moving
import com.moblin.android.platform.swiftui.rememberDismiss
import com.moblin.android.platform.swiftui.removing
import com.moblin.android.view.settings.streams.stream.obsremotecontrol.StreamObsRemoteControlSettingsView
import com.moblin.android.view.utils.ContextMenuDeleteButton
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
import com.moblin.android.various.utils.isMac
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.various.model.obsWebSocketEnabledUpdated
import com.moblin.android.various.model.reloadRemoteControlAssistant
import com.moblin.android.various.model.reloadRemoteControlRelay
import com.moblin.android.various.model.reloadRemoteControlStreamer
import com.moblin.android.various.model.reloadRemoteControlWeb

@Composable
private fun PasswordView(
    value: String,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var valueState by remember { mutableStateOf(value) }
    var changed by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }
    val palette = formPalette()

    fun submit() {
        val trimmed = valueState.trim()
        valueState = trimmed
        submitted = true
        onSubmit(trimmed)
        onDismiss()
    }

    Form(title = localized("Password")) {
        Section {
            FormRow {
                BasicTextField(
                    value = valueState,
                    onValueChange = {
                        valueState = it
                        changed = true
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = formBodyStyle.copy(color = palette.label),
                    cursorBrush = SolidColor(palette.accent),
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
    model: Model = LocalModel.current,
    streamer: SettingsRemoteControlStreamer,
    url: SettingsRemoteControlStreamerUrl,
) {
    ContextMenu(
        menu = {
            if (isMac()) {
                ContextMenuDeleteButtonView {
                    streamer.savedUrls = streamer.savedUrls.filterNot { it === url }
                }
            }
        },
    ) {
        FormRow(
            onClick = {
                streamer.name = url.name
                streamer.url = url.url
                model.reloadRemoteControlStreamer()
                model.reloadConnections()
            },
        ) {
            Text(url.name)
            Spacer(Modifier.weight(1f))
            Text(url.url)
            if (streamer.url == url.url) {
                SystemImage(name = "checkmark", fontSize = 17.sp, tint = formPalette().accent)
            }
        }
    }
}

@Composable
private fun UrlSettingsInnerView(
    model: Model = LocalModel.current,
    database: Database,
    streamer: SettingsRemoteControlStreamer,
) {
    Form(title = localized("Assistant")) {
        Section {
            NameEditView(
                name = streamer.name,
                onNameChange = { name -> submitStreamerName(streamer = streamer, name = name) },
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
        }
        Section(
            header = localized("Saved URLs"),
            footerContent = { SwipeLeftToDeleteHelpView(kind = localized("a URL")) },
        ) {
            ForEach(
                streamer.savedUrls,
                id = { it.id },
                onDelete = { offsets ->
                    streamer.savedUrls = streamer.savedUrls.removing(atOffsets = offsets)
                },
            ) { url ->
                AssistantUrlSettingsView(
                    model = model,
                    streamer = streamer,
                    url = url,
                )
            }
        }
    }
}

@Composable
private fun RemoteControlSettingsStreamerView(
    model: Model = LocalModel.current,
    streamer: SettingsRemoteControlStreamer,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Section(
        footerContent = {
            Text(
                localized(
                    "Enable to allow an assistant to monitor and control this device from a different device.",
                ),
            )
        },
    ) {
        Toggle(
            title = localized("Enabled"),
            isOn = streamer.enabled,
            onChange = { enabled ->
                streamer.enabled = enabled
                model.reloadRemoteControlStreamer()
                model.reloadConnections()
            },
        )
        NavigationLink(
            destination = {
                UrlSettingsInnerView(
                    model = model,
                    database = model.database,
                    streamer = streamer,
                )
            },
        ) {
            TextItemLocalizedView(
                name = "Assistant",
                value = if (streamer.name.isEmpty()) streamer.url else streamer.name,
            )
        }
    }
    Section(
        footerContent = {
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    localized(
                        "Receive chat and events from the assistant instead of directly from the streaming platform.",
                    ),
                )
                Text("")
                Text(localized("Only works for Twitch."))
            }
        },
    ) {
        Toggle(
            title = localized("Reliable chat and events"),
            isOn = streamer.reliableChatAndEvents,
            onChange = { enabled ->
                streamer.reliableChatAndEvents = enabled
                model.reloadRemoteControlStreamer()
                model.reloadConnections()
            },
        )
    }
    Section {
        FormRow {
            Text(localized("Preview FPS"))
            SliderView(
                value = streamer.previewFps,
                minimum = 0f,
                maximum = 5f,
                step = 1f,
                onChange = {},
                onSubmit = { value ->
                    submitStreamerPreviewFps(model = model, streamer = streamer, value = value)
                },
                width = 20f,
                format = ::formatStreamerPreviewFps,
            )
        }
    }
}

@Composable
private fun RemoteControlUrlsView(
    relay: SettingsRemoteControlServerRelay,
    port: Int,
    status: StatusOther,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Section(
        footerContent = {
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    localized(
                        "Enter one of the URLs as \"Assistant URL\" in the streamer device to connect to this device.",
                    ),
                )
            }
        },
    ) {
        NavigationLink(
            destination = {
                RemoteControlUrlsForm(relay = relay, port = port, status = status)
            },
        ) {
            Text(localized("URLs"))
        }
    }
}

@Composable
private fun RemoteControlUrlsForm(
    relay: SettingsRemoteControlServerRelay,
    port: Int,
    status: StatusOther,
) {
    fun formatUrl(ip: String): String = "ws://$ip:$port"

    Form(title = localized("URLs")) {
        if (relay.enabled) {
            Section(header = localized("Relay")) {
                UrlCopyView("${relay.baseUrl}/streamer/${relay.bridgeId}", image = "globe")
            }
        }
        UrlsIpv4View(status = status, formatUrl = { ip -> formatUrl(ip) })
        UrlsIpv6View(status = status, formatUrl = { ip -> formatUrl(ip) })
    }
}

@Composable
private fun StreamerView(
    model: Model = LocalModel.current,
    remoteControlSettings: SettingsRemoteControl,
    streamer: SettingsRemoteControlAssistant,
    streamerRelay: SettingsRemoteControlServerRelay,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    NavigationLink(
        destination = {
            StreamerForm(
                model = model,
                remoteControlSettings = remoteControlSettings,
                streamer = streamer,
                streamerRelay = streamerRelay,
                onNavigate = onNavigate,
            )
        },
    ) {
        DraggableItemTextView(name = streamer.name)
    }
}

@Composable
private fun StreamerForm(
    model: Model = LocalModel.current,
    remoteControlSettings: SettingsRemoteControl,
    streamer: SettingsRemoteControlAssistant,
    streamerRelay: SettingsRemoteControlServerRelay,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Form(title = localized("Streamer")) {
        Section {
            NameEditView(
                name = streamer.name,
                existingNames = remoteControlSettings.streamers,
                onNameChange = { name -> streamer.name = name },
            )
        }
        Section(header = localized("Assistant")) {
            Toggle(
                title = localized("Enabled"),
                isOn = streamer.enabled,
                onChange = { enabled ->
                    streamer.enabled = enabled
                    reloadIfEnabled(
                        model = model,
                        remoteControlSettings = remoteControlSettings,
                        streamer = streamer,
                        streamerRelay = streamerRelay,
                    )
                },
            )
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
        }
        Section(
            header = localized("Relay"),
            footer = localized("Use a relay server when the assistant is behind CGNAT or similar."),
        ) {
            Toggle(
                title = localized("Enabled"),
                isOn = streamerRelay.enabled,
                onChange = { enabled ->
                    streamerRelay.enabled = enabled
                    reloadIfEnabled(
                        model = model,
                        remoteControlSettings = remoteControlSettings,
                        streamer = streamer,
                        streamerRelay = streamerRelay,
                    )
                },
            )
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
        }
        if (streamer.enabled) {
            RemoteControlUrlsView(
                relay = streamerRelay,
                port = streamer.port,
                status = model.statusOther,
                onNavigate = onNavigate,
            )
        }
    }
}

@Composable
private fun StreamerItemView(
    streamer: SettingsRemoteControlAssistant,
    onClick: () -> Unit,
) {
    FormRow(onClick = onClick) {
        Text(streamer.name)
    }
}

@Composable
fun RemoteControlStreamersView(
    model: Model = LocalModel.current,
    remoteControlSettings: SettingsRemoteControl,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Section(
        footerContent = {
            Text(
                localized(
                    "Select a streamer. Once the streamer has connected to this device, this device can monitor and control it.",
                ),
            )
        },
    ) {
        Picker(
            title = localized("Current streamer"),
            selection = remoteControlSettings.selectedStreamer,
            options = listOf(null) + remoteControlSettings.streamers.map { it.id },
            text = { id ->
                remoteControlSettings.streamers.firstOrNull { it.id == id }?.name
                    ?: "-- None --"
            },
            onChange = { id ->
                remoteControlSettings.selectedStreamer = id
                onStreamerChanged(model = model, remoteControlSettings = remoteControlSettings)
            },
        )
    }
    Section(
        footerContent = { SwipeLeftToDeleteHelpView(kind = localized("a streamer")) },
    ) {
        ForEach(
            remoteControlSettings.streamers,
            id = { it.id },
            onDelete = { offsets ->
                deleteStreamer(
                    model = model,
                    remoteControlSettings = remoteControlSettings,
                    offsets = offsets,
                )
            },
            onMove = { froms, to ->
                remoteControlSettings.streamers =
                    remoteControlSettings.streamers.moving(fromOffsets = froms, toOffset = to)
            },
        ) { streamer ->
            ContextMenuDeleteButton(action = {
                val offset = remoteControlSettings.streamers
                    .indexOfFirst { it.id == streamer.id }
                if (offset != -1) {
                    deleteStreamer(
                        model = model,
                        remoteControlSettings = remoteControlSettings,
                        offsets = setOf(offset),
                    )
                }
            }) {
                StreamerView(
                    model = model,
                    remoteControlSettings = remoteControlSettings,
                    streamer = streamer,
                    streamerRelay = streamer.relay,
                    onNavigate = onNavigate,
                )
            }
        }
        TextButtonView(localized("Create")) {
            val streamer = SettingsRemoteControlAssistant()
            streamer.name = makeUniqueName(
                name = SettingsRemoteControlAssistant.baseName,
                existingNames = remoteControlSettings.streamers,
            )
            streamer.enabled = true
            streamer.port = DefaultTcpPorts.remoteControlAssistant
            remoteControlSettings.streamers = remoteControlSettings.streamers + streamer
        }
    }
}

@Composable
private fun AssistantForm(
    model: Model = LocalModel.current,
    remoteControlSettings: SettingsRemoteControl,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Form(title = localized("Assistant")) {
        RemoteControlStreamersView(
            model = model,
            remoteControlSettings = remoteControlSettings,
            onNavigate = onNavigate,
        )
        Section(
            header = localized("Websites"),
            footer = localized("Alternatively, use a website as assistant."),
        ) {
            ExternalUrlButtonView(
                url = "https://moblin.mys-lang.org/moblin-remote-control-relay/assistant.html",
            ) {
                Text("Moblin Remote Control Assistant")
            }
            ExternalUrlButtonView(url = "https://moblinremote.com/") {
                Text("Moblin Remote Control")
            }
        }
    }
}

private fun formatUrl(ip: String, port: Int): String {
    return if (port == 80) {
        "http://$ip"
    } else {
        "http://$ip:$port"
    }
}

@Composable
private fun WebUrlsView(
    web: SettingsRemoteControlWeb,
    status: StatusOther,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    NavigationLink(
        destination = {
            WebUrlsForm(web = web, status = status)
        },
    ) {
        Text(localized("URLs"))
    }
}

@Composable
private fun WebUrlsForm(
    web: SettingsRemoteControlWeb,
    status: StatusOther,
) {
    fun format(ip: String): String = formatUrl(ip = ip, port = web.port)
    val palette = formPalette()

    Form(title = localized("URLs")) {
        Section(
            header = localized("mDNS"),
            footer = localized("Copy your device name from iOS settings."),
        ) {
            FormRow {
                Box(modifier = Modifier.weight(1f)) {
                    BasicTextField(
                        value = web.deviceName,
                        onValueChange = { web.deviceName = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = formBodyStyle.copy(color = palette.label),
                        cursorBrush = SolidColor(palette.accent),
                    )
                    if (web.deviceName.isEmpty()) {
                        Text(localized("My device name"), color = palette.secondaryLabel)
                    }
                }
            }
            if (web.deviceName.isNotEmpty()) {
                UrlCopyView(
                    format(makeMdnsHostname(deviceName = web.deviceName)),
                    image = "network",
                )
            }
        }
        UrlsIpv4View(status = status, formatUrl = { ip -> format(ip) })
        UrlsIpv6View(status = status, formatUrl = { ip -> format(ip) })
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
    }
}

@Composable
fun RemoteControlSettingsWebView(
    model: Model = LocalModel.current,
    web: SettingsRemoteControlWeb,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Section(
        footerContent = {
            Text(
                localized(
                    "Enable to monitor and control this device from another device using a web browser. There is no authentication, nor encryption, so be careful.",
                ),
            )
        },
    ) {
        Toggle(
            title = localized("Enabled"),
            isOn = web.enabled,
            onChange = { enabled ->
                web.enabled = enabled
                model.reloadRemoteControlWeb()
            },
        )
    }
    Section {
        TextEditNavigationView(
            title = localized("Server port"),
            value = web.port.toString(),
            onChange = ::isValidPort,
            onSubmit = { value -> submitPort(model = model, web = web, value = value) },
            keyboardType = KeyboardType.Number,
            placeholder = "80",
        )
    }
    if (web.enabled) {
        WebUrlsView(web = web, status = model.statusOther, onNavigate = onNavigate)
    }
}

@Composable
fun RemoteControlSettingsView(
    model: Model = LocalModel.current,
    database: Database,
    stream: SettingsStream,
    onStreamChange: (SettingsStream) -> Unit,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Form(title = localized("Remote control")) {
        Section {
            Text(localized("Control and monitor Moblin from another device."))
        }
        Section(
            header = localized("General"),
            footer = localized("Used by both streamer and assistant."),
        ) {
            NavigationLink(
                destination = {
                    PasswordView(
                        value = database.remoteControl.password,
                        onSubmit = { value ->
                            submitPassword(model = model, database = database, value = value)
                        },
                        onDismiss = rememberDismiss(),
                    )
                },
            ) {
                TextItemLocalizedView(
                    name = "Password",
                    value = database.remoteControl.password,
                    sensitive = true,
                )
            }
        }
        Section {
            NavigationLink(
                destination = {
                    Form(title = localized("Streamer")) {
                        RemoteControlSettingsStreamerView(
                            model = model,
                            streamer = database.remoteControl.streamer,
                        )
                    }
                },
            ) {
                Text(localized("Streamer"))
            }
            NavigationLink(
                destination = {
                    AssistantForm(
                        model = model,
                        remoteControlSettings = database.remoteControl,
                        onNavigate = onNavigate,
                    )
                },
            ) {
                Text(localized("Assistant"))
            }
        }
        Section {
            NavigationLink(
                destination = {
                    Form(title = localized("Web")) {
                        RemoteControlSettingsWebView(
                            model = model,
                            web = database.remoteControl.web,
                            onNavigate = onNavigate,
                        )
                    }
                },
            ) {
                Text(localized("Web"))
            }
        }
        if (stream !== fallbackStream) {
            ShortcutSectionView {
                NavigationLink(
                    destination = {
                        StreamObsRemoteControlSettingsView(stream = stream)
                    },
                ) {
                    Toggle(
                        isOn = stream.obsWebSocketEnabled,
                        onChange = { enabled ->
                            stream.obsWebSocketEnabled = enabled
                            onStreamChange(stream)
                            model.obsWebSocketEnabledUpdated()
                        },
                    ) {
                        Label(
                            localized("OBS remote control"),
                            systemImage = "dot.radiowaves.left.and.right",
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
    streamer.savedUrls = streamer.savedUrls + url
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
    val port = value.toIntOrNull() ?: return
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
    offsets: IndexSet,
) {
    remoteControlSettings.streamers = remoteControlSettings.streamers.removing(atOffsets = offsets)
    val selectedStreamer = remoteControlSettings.selectedStreamer ?: return
    if (remoteControlSettings.streamers.any { it.id == selectedStreamer }) {
        return
    }
    remoteControlSettings.selectedStreamer = null
    onStreamerChanged(model = model, remoteControlSettings = remoteControlSettings)
}

private fun submitPort(model: Model, web: SettingsRemoteControlWeb, value: String) {
    val port = value.toIntOrNull() ?: return
    if (port >= 65535) {
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

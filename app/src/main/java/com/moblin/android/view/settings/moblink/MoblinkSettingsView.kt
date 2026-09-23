package com.moblin.android.view.settings.moblink

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.common.various.isValidPort
import com.moblin.android.common.various.isValidWebSocketUrl
import com.moblin.android.localized
import com.moblin.android.moblink.MoblinkScannerStreamer
import com.moblin.android.moblink.getMoblinkRelayId
import com.moblin.android.moblink.moblinkRelayResetId
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormButton
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.formBodyStyle
import com.moblin.android.platform.swiftui.formFootnoteStyle
import com.moblin.android.platform.swiftui.formPalette
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
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.view.utils.TextItemLocalizedView
import com.moblin.android.view.utils.UrlsView

private fun isAllowedPassword(password: String): Boolean {
    return password.isNotEmpty()
}

@Composable
private fun PasswordView(
    model: Model = LocalModel.current,
    initialValue: String,
    onSubmit: (String) -> Unit,
) {
    val palette = formPalette()
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
            localized("Not long and random enough")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (changed && !submitted) {
                submit()
            }
        }
    }

    Form(title = "Password") {
        Section(footerContent = {
            message?.let {
                Text(it, color = palette.red, fontWeight = FontWeight.Bold)
            }
        }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = value,
                    onValueChange = {
                        value = it
                        changed = true
                        message = createMessage()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isLive,
                    singleLine = true,
                    textStyle = formBodyStyle.copy(color = palette.label),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    cursorBrush = SolidColor(palette.accent),
                )
                CopyToClipboardButtonView(text = value)
            }
        }
        Section {
            FormButton(
                title = "Reset to default",
                enabled = !isLive,
                action = {
                    value = "1234"
                    submit()
                },
            )
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
    Section(headerContent = { Text(server.name) }) {
        server.urls.forEach { url ->
            FormButton(
                title = url,
                action = {
                    onStreamerUrlChange(url)
                    submitUrl(url)
                },
            )
        }
    }
}

@Composable
private fun RelayStreamerUrlView(
    model: Model = LocalModel.current,
    moblink: Moblink,
    initialStreamerUrl: String,
    onDismiss: () -> Unit,
) {
    val palette = formPalette()
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

    Form(title = "Streamer URL") {
        Section {
            BasicTextField(
                value = streamerUrl,
                onValueChange = { streamerUrl = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = formBodyStyle.copy(color = palette.label),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submitUrl(streamerUrl) }),
                cursorBrush = SolidColor(palette.accent),
                decorationBox = { innerTextField ->
                    Box {
                        if (streamerUrl.isEmpty()) {
                            Text(
                                "ws://32.143.32.12:${DefaultTcpPorts.remoteControlAssistant}",
                                style = formBodyStyle,
                                color = palette.tertiaryLabel,
                            )
                        }
                        innerTextField()
                    }
                },
            )
        }
        if (discoveredStreamers.isEmpty()) {
            Section {
                Text(localized("No streamers discovered yet on your local network."))
            }
        } else {
            discoveredStreamers.forEach { server ->
                key(server) {
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
    var idFontSize by remember { mutableStateOf(13.sp) }

    LaunchedEffect(Unit) {
        relayId = getMoblinkRelayId()
    }

    Section(
        header = "Relay",
        footerContent = {
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    localized(
                        "Enable this on the device you want to use as the extra bonding " +
                            "connection. The device must have cellular data enabled.",
                    ),
                )
                Text("")
                Text(
                    text = "ID: $relayId",
                    maxLines = 1,
                    style = formFootnoteStyle.copy(fontSize = idFontSize),
                    onTextLayout = { layout ->
                        if (layout.hasVisualOverflow && idFontSize > 6.5.sp) {
                            idFontSize *= 0.9f
                        }
                    },
                )
            }
        },
    ) {
        Toggle(
            title = "Enabled",
            isOn = enabled,
            onChange = {
                relay.enabled.value = it
                model.reloadMoblinkRelay()
            },
        )
        NameEditView(
            name = name,
            onNameChange = {
                relay.name.value = it
                model.reloadMoblinkRelay()
            },
        )
        Toggle(
            title = "Manual",
            isOn = manual,
            enabled = !isLive,
            onChange = {
                relay.manual.value = it
                model.reloadMoblinkRelay()
            },
        )
        if (manual) {
            NavigationLink(
                destination = {
                    RelayStreamerUrlView(
                        model = model,
                        moblink = model.moblink,
                        initialStreamerUrl = relay.url.value,
                        onDismiss = {},
                    )
                },
            ) {
                TextItemLocalizedView(name = "Streamer URL", value = url)
            }
        }
        FormButton(
            title = "Reset id",
            action = {
                moblinkRelayResetId()
                model.reloadMoblinkRelay()
                relayId = getMoblinkRelayId()
            },
        )
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

    Section(
        header = "Streamer",
        footer = localized(
            "Enable this on your streaming device. Configure relay devices to connect to this device.",
        ),
    ) {
        Toggle(
            title = "Enabled",
            isOn = enabled,
            enabled = !isLive,
            onChange = {
                streamer.enabled.value = it
                model.reloadMoblinkStreamer()
            },
        )
        TextEditNavigationView(
            title = localized("Server port"),
            value = port.toString(),
            onChange = ::isValidPort,
            onSubmit = { submitPort(it) },
            keyboardType = KeyboardType.Number,
            placeholder = DefaultTcpPorts.moblinkStreamer.toString(),
        )
    }
}

@Composable
fun MoblinkSettingsView(
    model: Model = LocalModel.current,
    status: StatusOther,
    streamer: SettingsMoblinkStreamer,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val streamerEnabled by streamer.enabled.collectAsState()
    val streamerPort by streamer.port.collectAsState()

    fun submitPassword(value: String) {
        model.database.moblink.password = value.trim()
        model.reloadMoblinkRelay()
        model.reloadMoblinkStreamer()
    }

    Form(title = "Moblink") {
        Section {
            Text(
                localized(
                    "Use phones as additional SRTLA and RIST bonding connections. Install " +
                        "Moblink on Android phones to use them.",
                ),
            )
        }
        Section(
            footer = localized(
                "Used by both relay and streamer devices. Copy the streamer's password to " +
                    "the relay device.",
            ),
        ) {
            NavigationLink(
                destination = {
                    PasswordView(
                        model = model,
                        initialValue = model.database.moblink.password,
                        onSubmit = { submitPassword(it) },
                    )
                },
            ) {
                TextItemLocalizedView(
                    name = "Password",
                    value = model.database.moblink.password,
                    sensitive = true,
                )
            }
        }
        RelayView(
            model = model,
            relay = model.database.moblink.relay,
            onNavigate = onNavigate,
        )
        StreamerView(model = model, streamer = streamer)
        if (streamerEnabled) {
            Section(
                footer = localized(
                    "Enter one of the URL:s as \"Streamer URL\" in the relay device to use " +
                        "it as an additional bonding connection.",
                ),
            ) {
                UrlsView(
                    status = status,
                    formatUrl = { "ws://$it:$streamerPort" },
                )
            }
        }
    }
}

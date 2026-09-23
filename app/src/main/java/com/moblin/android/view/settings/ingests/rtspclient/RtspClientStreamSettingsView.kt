package com.moblin.android.view.settings.ingests.rtspclient

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.common.various.cleanUrl
import com.moblin.android.common.various.isValidIngestLatency
import com.moblin.android.common.various.isValidUrl
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Sheet
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsRtspClient
import com.moblin.android.various.settings.SettingsRtspClientStream
import com.moblin.android.various.settings.SettingsRtspTransport
import com.moblin.android.view.utils.CloseToolbar
import com.moblin.android.view.utils.FormFieldError
import com.moblin.android.view.utils.MultiLineTextFieldView
import com.moblin.android.view.utils.NameEditView
import com.moblin.android.view.utils.TextButtonView
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.view.utils.TextItemLocalizedView
import com.moblin.android.view.utils.UrlCopyView
import com.moblin.android.various.model.reloadRtspClient

@Composable
fun UrlSettingsView(
    disabled: Boolean,
    url: String,
    onChangeUrl: (String) -> Unit,
    value: String,
    placeholder: String,
    allowedSchemes: List<String>?,
    examples: List<Pair<String, String>>,
    onSubmitted: () -> Unit,
    onDismiss: () -> Unit
) {
    var valueState by remember { mutableStateOf(value) }
    var changed by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var presentingHelp by remember { mutableStateOf(false) }

    fun submitUrl() {
        if (submitted) {
            return
        }
        valueState = cleanUrl(valueState)
        if (isValidUrl(valueState, allowedSchemes) != null) {
            onDismiss()
            return
        }
        submitted = true
        onChangeUrl(valueState)
        onSubmitted()
        onDismiss()
    }

    DisposableEffect(Unit) {
        onDispose {
            if (changed && !submitted) {
                submitUrl()
            }
        }
    }

    Form(title = "URL") {
        Section(footerContent = {
            error?.let {
                FormFieldError(error = it)
            }
        }) {
            MultiLineTextFieldView(
                value = valueState,
                onValueChange = { newValue ->
                    valueState = newValue
                    error = isValidUrl(newValue, allowedSchemes)
                    changed = true
                    if (newValue.contains("\n")) {
                        valueState = newValue.replace("\n", "")
                        submitUrl()
                    }
                },
                placeholder = placeholder
            )
        }
        Section {
            TextButtonView(
                title = localized("Examples"),
                action = { presentingHelp = true }
            )
        }
    }

    if (presentingHelp) {
        Sheet(onDismissRequest = { presentingHelp = false }) {
            Form(
                title = "Examples",
                toolbar = {
                    CloseToolbar(
                        presenting = presentingHelp,
                        onPresentingChange = { presentingHelp = it }
                    )
                }
            ) {
                examples.forEach { example ->
                    key(example.second) {
                        Section(header = example.first) {
                            UrlCopyView(url = example.second)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RtspClientStreamSettingsView(
    model: Model = LocalModel.current,
    rtspClient: SettingsRtspClient,
    stream: SettingsRtspClientStream,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    NavigationLink(
        destination = {
            RtspClientStreamSettingsViewDestination(
                model = model,
                rtspClient = rtspClient,
                stream = stream
            )
        }
    ) {
        Toggle(
            isOn = stream.enabled,
            onChange = { enabled ->
                stream.enabled = enabled
                model.reloadRtspClient()
            }
        ) {
            Row {
                Text(stream.name)
            }
        }
    }
}

@Composable
fun RtspClientStreamSettingsViewDestination(
    model: Model = LocalModel.current,
    rtspClient: SettingsRtspClient,
    stream: SettingsRtspClientStream,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    Form(title = "Stream") {
        Section {
            NameEditView(
                name = stream.name,
                onNameChange = { stream.name = it },
                existingNames = rtspClient.streams
            )
        }
        Section {
            NavigationLink(
                destination = {
                    UrlSettingsView(
                        disabled = false,
                        url = stream.url,
                        onChangeUrl = { stream.url = it },
                        value = stream.url,
                        placeholder = "rtsp://192.168.1.83/stream1",
                        allowedSchemes = listOf("rtsp"),
                        examples = listOf(
                            "TP-Link" to "rtsp://username:password@192.168.1.83/stream1"
                        ),
                        onSubmitted = { model.reloadRtspClient() },
                        onDismiss = {}
                    )
                }
            ) {
                TextItemLocalizedView(name = "URL", value = stream.url, sensitive = true)
            }
        }
        Section {
            Picker(
                title = "Transport",
                selection = stream.transport,
                options = SettingsRtspTransport.entries,
                text = { it.toString() },
                onChange = { transport ->
                    stream.transport = transport
                    model.reloadRtspClient()
                }
            )
        }
        Section(footerContent = {
            Text(localized("The higher, the lower risk of stuttering."))
        }) {
            TextEditNavigationView(
                title = localized("Latency"),
                value = stream.latency.toString(),
                onChange = { isValidIngestLatency(it) },
                onSubmit = { text ->
                    val latency = text.toIntOrNull()
                    if (latency != null) {
                        stream.latency = latency
                        model.reloadRtspClient()
                    }
                },
                footers = listOf(
                    localized("5 or more milliseconds. 2000 ms by default.")
                ),
                keyboardType = KeyboardType.Number,
                valueFormat = { "$it ms" }
            )
        }
    }
}

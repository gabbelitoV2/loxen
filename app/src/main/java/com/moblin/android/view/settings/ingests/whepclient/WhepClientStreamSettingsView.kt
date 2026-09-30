package com.moblin.android.view.settings.ingests.whepclient

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.KeyboardType
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.common.various.isValidIngestLatency
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.rememberDismiss
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsWhepClient
import com.moblin.android.various.settings.SettingsWhepClientStream
import com.moblin.android.view.settings.ingests.rtspclient.UrlSettingsView
import com.moblin.android.view.utils.NameEditView
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.view.utils.TextItemLocalizedView
import com.moblin.android.various.model.reloadWhepClient
import com.moblin.android.various.model.updateWhepVideoSourcesAndMics

@Composable
fun WhepClientStreamSettingsView(
    model: Model = LocalModel.current,
    whepClient: SettingsWhepClient,
    stream: SettingsWhepClientStream,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    NavigationLink(
        destination = {
            WhepClientStreamSettingsViewInner(
                model = model,
                whepClient = whepClient,
                stream = stream,
                onNavigate = onNavigate,
            )
        },
    ) {
        Toggle(
            isOn = stream.enabled,
            onChange = { enabled ->
                stream.enabled = enabled
                model.reloadWhepClient()
            },
        ) {
            Text(stream.name)
        }
    }
}

@Composable
fun WhepClientStreamSettingsViewInner(
    model: Model = LocalModel.current,
    whepClient: SettingsWhepClient,
    stream: SettingsWhepClientStream,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Form(title = "Stream") {
        Section {
            NameEditView(
                name = stream.name,
                onNameChange = {
                    stream.name = it
                    model.updateWhepVideoSourcesAndMics()
                },
                existingNames = whepClient.streams,
            )
        }
        Section {
            NavigationLink(
                destination = {
                    UrlSettingsView(
                        disabled = stream.enabled,
                        url = stream.url,
                        onChangeUrl = { stream.url = it },
                        value = stream.url,
                        placeholder = "http://foo.com/whep",
                        allowedSchemes = listOf("http", "https"),
                        examples = emptyList(),
                        onSubmitted = { model.reloadWhepClient() },
                        onDismiss = rememberDismiss(),
                    )
                },
            ) {
                TextItemLocalizedView(
                    name = "URL",
                    value = stream.url,
                    sensitive = true,
                )
            }
        }
        Section(footer = "The higher, the lower risk of stuttering.") {
            TextEditNavigationView(
                title = localized("Latency"),
                value = stream.latency.toString(),
                onChange = { isValidIngestLatency(it) },
                onSubmit = { value ->
                    value.toIntOrNull()?.let { latency ->
                        stream.latency = latency
                        model.reloadWhepClient()
                    }
                },
                footers = listOf(
                    localized("5 or more milliseconds. 100 ms by default."),
                ),
                keyboardType = KeyboardType.Number,
                valueFormat = { "$it ms" },
            )
        }
        Section {
            Toggle(
                title = "Sync timestamps",
                isOn = stream.syncTimestamps,
                enabled = !stream.enabled,
                onChange = {
                    stream.syncTimestamps = it
                    model.reloadWhepClient()
                },
            )
        }
    }
}

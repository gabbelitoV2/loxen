package com.moblin.android.view.settings.ingests.srtclient

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.platform.swiftui.*
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsSrtClient
import com.moblin.android.various.settings.SettingsSrtClientStream
import com.moblin.android.view.settings.ingests.rtspclient.UrlSettingsView
import com.moblin.android.view.utils.NameEditView
import com.moblin.android.view.utils.TextItemLocalizedView
import com.moblin.android.various.model.reloadSrtClient

@Composable
fun SrtClientStreamSettingsView(
    model: Model = LocalModel.current,
    srtClient: SettingsSrtClient,
    stream: SettingsSrtClientStream,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
    onNameChange: (String) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onUrlChange: (String) -> Unit,
) {
    NavigationLink(
        destination = {
            SrtClientStreamSettingsForm(
                model = model,
                srtClient = srtClient,
                stream = stream,
                onNavigate = onNavigate,
                onNameChange = onNameChange,
                onUrlChange = onUrlChange,
            )
        },
    ) {
        Toggle(
            isOn = stream.enabled,
            onChange = { value ->
                onEnabledChange(value)
                model.reloadSrtClient()
            },
        ) {
            Text(stream.name)
        }
    }
}

@Composable
fun SrtClientStreamSettingsForm(
    model: Model = LocalModel.current,
    srtClient: SettingsSrtClient,
    stream: SettingsSrtClientStream,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
    onNameChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
) {
    Form(title = "Stream") {
        Section {
            NameEditView(
                name = stream.name,
                existingNames = srtClient.streams,
                onNameChange = onNameChange,
            )
        }
        Section {
            NavigationLink(
                destination = {
                    SrtClientStreamUrlSettingsView(
                        model = model,
                        stream = stream,
                        onUrlChange = onUrlChange,
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
    }
}

@Composable
fun SrtClientStreamUrlSettingsView(
    model: Model = LocalModel.current,
    stream: SettingsSrtClientStream,
    onUrlChange: (String) -> Unit,
) {
    UrlSettingsView(
        disabled = false,
        url = stream.url,
        onChangeUrl = onUrlChange,
        value = stream.url,
        placeholder = "srt://192.168.1.100:4000",
        allowedSchemes = listOf("srt"),
        examples = listOf(
            "BELABOX cloud" to "srt://eu.srt.belabox.net:4001?streamid=P3Kd229fslEWF3SGRQAsd",
        ),
        onSubmitted = { model.reloadSrtClient() },
        onDismiss = rememberDismiss(),
    )
}

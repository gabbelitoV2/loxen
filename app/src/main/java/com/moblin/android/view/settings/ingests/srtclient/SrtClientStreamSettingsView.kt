package com.moblin.android.view.settings.ingests.srtclient

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsSrtClient
import com.moblin.android.various.settings.SettingsSrtClientStream
import com.moblin.android.view.settings.ingests.rtspclient.UrlSettingsView
import com.moblin.android.view.utils.NameEditView
import com.moblin.android.view.utils.TextItemLocalizedView

@Composable
fun SrtClientStreamSettingsView(
    model: Model,
    srtClient: SettingsSrtClient,
    stream: SettingsSrtClientStream,
    onNavigate: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onUrlChange: (String) -> Unit,
) {
    val name by stream.name.collectAsState()
    val enabled by stream.enabled.collectAsState()
    var previousEnabled by remember { mutableStateOf(enabled) }
    LaunchedEffect(enabled) {
        if (previousEnabled != enabled) {
            previousEnabled = enabled
            model.reloadSrtClient()
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("srtClientStreamSettings") }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
        )
        Switch(
            checked = enabled,
            onCheckedChange = { value -> onEnabledChange(value) },
        )
    }
}

@Composable
fun SrtClientStreamSettingsForm(
    model: Model,
    srtClient: SettingsSrtClient,
    stream: SettingsSrtClientStream,
    onNavigate: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
) {
    val name by stream.name.collectAsState()
    val url by stream.url.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        NameEditView(
            name = name,
            existingNames = srtClient.streams,
            onNameChange = onNameChange,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate("url") }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextItemLocalizedView(
                name = "URL",
                value = url,
                sensitive = true,
            )
        }
    }
}

@Composable
fun SrtClientStreamUrlSettingsView(
    model: Model,
    stream: SettingsSrtClientStream,
    onUrlChange: (String) -> Unit,
) {
    val url by stream.url.collectAsState()
    UrlSettingsView(
        disabled = false,
        url = url,
        value = url,
        placeholder = "srt://192.168.1.100:4000",
        allowedSchemes = listOf("srt"),
        examples = listOf(
            "BELABOX cloud" to "srt://eu.srt.belabox.net:4001?streamid=P3Kd229fslEWF3SGRQAsd",
        ),
        onUrlChange = onUrlChange,
        onSubmitted = { model.reloadSrtClient() },
    )
}

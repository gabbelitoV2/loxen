package com.moblin.android.view.settings.streams.stream.obsremotecontrol

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.moblin.android.common.various.cleanUrl
import com.moblin.android.common.various.isValidWebSocketUrl
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.LocalModel

private fun submitWebSocketUrl(model: Model, stream: SettingsStream, value: String) {
    val url = cleanUrl(value)
    val message = isValidWebSocketUrl(url)
    if (message != null) {
        model.makeErrorToast(message)
        return
    }
    stream.obsWebSocketUrl = url
    if (stream.enabled) {
        Unit
    }
}

private fun submitWebSocketPassword(model: Model, stream: SettingsStream, value: String) {
    stream.obsWebSocketPassword = value
    if (stream.enabled) {
        Unit
    }
}

private fun submitSourceName(stream: SettingsStream, value: String) {
    stream.obsSourceName = value
}

private fun submitBrbScene(stream: SettingsStream, value: String) {
    stream.obsBrbScene = value
}

private fun submitMainScene(stream: SettingsStream, value: String) {
    stream.obsMainScene = value
}

@Composable
fun StreamObsRemoteControlSettingsInnerView(model: Model = LocalModel.current, stream: SettingsStream) {
    val showAllSettings = model.database.showAllSettings
    Column {
        Text("WebSocket")
        TextEditNavigationView(
            title = localized("URL"),
            value = stream.obsWebSocketUrl,
            onChange = ::isValidWebSocketUrl,
            onSubmit = { submitWebSocketUrl(model, stream, it) },
            footers = listOf(localized("For example ws://232.32.45.332:4567."))
        )
        TextEditNavigationView(
            title = localized("Password"),
            value = stream.obsWebSocketPassword,
            onSubmit = { submitWebSocketPassword(model, stream, it) },
            sensitive = true
        )
        TextEditNavigationView(
            title = localized("Main scene"),
            value = stream.obsMainScene,
            onSubmit = { submitMainScene(stream, it) },
            capitalize = true
        )
        TextEditNavigationView(
            title = localized("BRB scene"),
            value = stream.obsBrbScene,
            onSubmit = { submitBrbScene(stream, it) },
            capitalize = true
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Streaming directly to OBS",
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = stream.streamingDirectlyToObs,
                onCheckedChange = { value -> stream.streamingDirectlyToObs = value }
            )
        }
        Text(
            "Moblin will periodically try to switch to the BRB scene if the stream is " +
                "likely broken, and back to the main scene once everything seems to work again."
        )
        if (showAllSettings) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "BRB scene when video source is broken",
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = stream.obsBrbSceneVideoSourceBroken,
                    onCheckedChange = { value -> stream.obsBrbSceneVideoSourceBroken = value },
                    enabled = stream.obsBrbScene.isNotEmpty()
                )
            }
            Text(
                "Moblin will switch to the BRB scene configured above when the current scene's " +
                    "SRT(LA) or RTMP video source is disconnected. Typically enable when using Moblin " +
                    "as SRT(LA) server at home, streaming to OBS on the same computer."
            )
        }
        TextEditNavigationView(
            title = localized("Source name"),
            value = stream.obsSourceName,
            onSubmit = { submitSourceName(stream, it) },
            capitalize = true
        )
        Text("The name of the Source in OBS that receives the stream from Moblin.")
        if (showAllSettings) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Auto start streaming when going live",
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = stream.obsAutoStartStream,
                    onCheckedChange = { value -> stream.obsAutoStartStream = value }
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Auto stop streaming when ending stream",
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = stream.obsAutoStopStream,
                    onCheckedChange = { value -> stream.obsAutoStopStream = value }
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Auto start recording when going live",
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = stream.obsAutoStartRecording,
                    onCheckedChange = { value -> stream.obsAutoStartRecording = value }
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Auto stop recording when ending stream",
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = stream.obsAutoStopRecording,
                    onCheckedChange = { value -> stream.obsAutoStopRecording = value }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamObsRemoteControlSettingsView(model: Model = LocalModel.current, stream: SettingsStream) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(localized("OBS remote control")) })
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            item {
                StreamObsRemoteControlSettingsInnerView(model = model, stream = stream)
            }
        }
    }
}

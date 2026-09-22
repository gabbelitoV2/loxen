package com.moblin.android.view.settings.ingests.whipserver

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.moblin.android.common.various.isValidIngestLatency
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.StatusOther
import com.moblin.android.various.settings.SettingsWhipServer
import com.moblin.android.various.settings.SettingsWhipServerStream
import com.moblin.android.view.settings.ingests.rtmpserver.IngestStreamItemView
import com.moblin.android.view.utils.NameEditView
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.view.utils.UrlsView
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

private const val WhipServerStreamDestination = "whipServerStream"

@Composable
fun WhipServerStreamSettingsView(
    model: Model = LocalModel.current,
    status: StatusOther,
    whipServer: SettingsWhipServer,
    stream: SettingsWhipServerStream,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    Box(
        modifier = Modifier.clickable {
            onNavigate(WhipServerStreamDestination)
        }
    ) {
        IngestStreamItemView(
            name = stream.name,
            connected = model.isWhipStreamConnected(streamId = stream.id)
        )
    }
}

private fun changeStreamKey(model: Model, value: String): String? {
    if (model.getWhipStream(streamKey = value.trim()) == null) {
        return null
    }
    return localized("Already in use")
}

private fun submitStreamKey(model: Model, stream: SettingsWhipServerStream, value: String) {
    val streamKey = value.trim()
    if (model.getWhipStream(streamKey = streamKey) != null) {
        return
    }
    stream.streamKey = streamKey
}

private fun submitLatency(stream: SettingsWhipServerStream, value: String) {
    val latency = value.toIntOrNull() ?: return
    stream.latency = latency
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhipServerStreamSettingsDetail(
    model: Model = LocalModel.current,
    status: StatusOther,
    whipServer: SettingsWhipServer,
    stream: SettingsWhipServerStream
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(localized("Stream"))
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            item {
                NameEditView(
                    name = stream.name,
                    existingNames = whipServer.streams,
                    onNameChange = { stream.name = it },
                    enabled = !whipServer.enabled
                )
            }
            item {
                TextEditNavigationView(
                    title = localized("Stream key"),
                    value = stream.streamKey,
                    onChange = { changeStreamKey(model, it) },
                    onSubmit = { submitStreamKey(model, stream, it) },
                    enabled = !whipServer.enabled
                )
            }
            item {
                Text(
                    text = localized("The stream name is shown in the list of cameras in scene settings."),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            item {
                TextEditNavigationView(
                    title = localized("Latency"),
                    value = stream.latency.toString(),
                    onChange = { isValidIngestLatency(it) },
                    onSubmit = { submitLatency(stream, it) },
                    footers = listOf(localized("5 or more milliseconds. 100 ms by default.")),
                    keyboardType = KeyboardType.Number,
                    valueFormat = { "$it ms" },
                    enabled = !whipServer.enabled
                )
            }
            item {
                Text(
                    text = localized("The higher, the lower risk of stuttering."),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(localized("Sync timestamps"))
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = stream.syncTimestamps,
                        onCheckedChange = { stream.syncTimestamps = it },
                        enabled = !whipServer.enabled
                    )
                }
            }
            item {
                Text(
                    text = localized("Publish URLs"),
                    style = MaterialTheme.typography.titleSmall
                )
            }
            item {
                UrlsView(
                    status = status,
                    formatUrl = { "whip://$it:${whipServer.port}/whip/stream/${stream.streamKey}" }
                )
            }
            item {
                Text(
                    text = localized(
                        "Enter one of the URLs into the WHIP publisher device to send video " +
                            "to this stream. Usually enter the WiFi or Personal Hotspot URL."
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

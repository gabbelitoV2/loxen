package com.moblin.android.view.settings.ingests.srtlaserver

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.StatusOther
import com.moblin.android.various.settings.SettingsSrtlaServer
import com.moblin.android.various.settings.SettingsSrtlaServerStream
import com.moblin.android.view.settings.ingests.rtmpserver.IngestStreamItemView
import com.moblin.android.view.utils.NameEditView
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.view.utils.UrlsView

private fun changeStreamId(value: String, model: Model): String? {
    val streamId = value.trim()
    if (!Regex("[a-zA-Z0-9]*").matches(streamId)) {
        return localized("Bad character")
    }
    if (model.getSrtlaStream(streamId = streamId) != null) {
        return localized("Already in use")
    }
    return null
}

private fun submitStreamId(value: String, model: Model, stream: SettingsSrtlaServerStream) {
    val streamId = value.trim()
    if (!Regex("[a-zA-Z0-9]*").matches(streamId)) {
        return
    }
    if (model.getSrtlaStream(streamId = streamId) != null) {
        return
    }
    stream.streamId = streamId
}

private fun formatUrl(
    proto: String,
    ip: String,
    port: Int,
    stream: SettingsSrtlaServerStream,
): String {
    var url = "$proto://$ip:$port"
    if (stream.streamId.isNotEmpty()) {
        url += "?streamid=${stream.streamId}"
    }
    return url
}

@Composable
fun SrtlaServerStreamSettingsView(
    model: Model,
    status: StatusOther,
    srtlaServer: SettingsSrtlaServer,
    stream: SettingsSrtlaServerStream,
    onNavigate: (String) -> Unit,
) {
    Box(modifier = Modifier.clickable { onNavigate("Stream") }) {
        IngestStreamItemView(
            name = stream.name,
            connected = model.isSrtlaStreamConnected(streamId = stream.streamId),
        )
    }
}

@Composable
fun SrtlaServerStreamSettingsDetailView(
    model: Model,
    status: StatusOther,
    srtlaServer: SettingsSrtlaServer,
    stream: SettingsSrtlaServerStream,
) {
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                NameEditView(
                    name = stream.name,
                    onChange = { stream.name = it },
                    existingNames = srtlaServer.streams,
                )
                TextEditNavigationView(
                    title = localized("Stream id"),
                    value = stream.streamId,
                    onChange = { changeStreamId(value = it, model = model) },
                    onSubmit = { submitStreamId(value = it, model = model, stream = stream) },
                    footers = listOf(localized("May only contain lower case letters.")),
                )
                Text(
                    text = localized(
                        "The stream name is shown in the list of cameras in scene settings."
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        item {
            Text(
                text = localized("Publish URLs"),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        item {
            UrlsView(
                status = status,
                title = localized("SRT URLs"),
                formatUrl = {
                    formatUrl(proto = "srt", ip = it, port = srtlaServer.srtPort, stream = stream)
                },
            )
        }
        item {
            UrlsView(
                status = status,
                title = localized("SRTLA URLs"),
                formatUrl = {
                    formatUrl(
                        proto = "srtla",
                        ip = it,
                        port = srtlaServer.srtlaPort,
                        stream = stream,
                    )
                },
            )
        }
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = localized(
                        "Enter one of the URLs into the SRT(LA) publisher device to send video " +
                            "to this stream. Usually enter the WiFi or Personal Hotspot URL."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

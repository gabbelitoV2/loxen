package com.moblin.android.view.settings.ingests.rtmpserver

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.isValidIngestLatency
import com.moblin.android.localized
import com.moblin.android.media.rtmpserver.rtmpServerApp
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.StatusOther
import com.moblin.android.various.settings.SettingsRtmpServer
import com.moblin.android.various.settings.SettingsRtmpServerStream
import com.moblin.android.view.utils.NameEditView
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.view.utils.UrlsView
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

@Composable
fun IngestStreamItemView(
    name: String,
    connected: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (connected) {
            Icon(imageVector = Icons.Default.Check, contentDescription = null)
        } else {
            Icon(imageVector = Icons.Default.Close, contentDescription = null)
        }
        Text(name)
        Spacer(Modifier.weight(1f))
    }
}

@Composable
fun RtmpServerStreamSettingsView(
    model: Model = LocalModel.current,
    status: StatusOther,
    rtmpServer: SettingsRtmpServer,
    stream: SettingsRtmpServerStream,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val name = stream.name
    val streamKey = stream.streamKey
    IngestStreamItemView(
        name = name,
        connected = model.ingests.rtmp?.isStreamConnected(streamKey = streamKey) ?: false,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("RtmpServerStreamSettingsForm") }
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

private fun changeStreamKey(rtmpServer: SettingsRtmpServer, value: String): String? {
    if (rtmpServer.streams.none { it.streamKey == value.trim() }) {
        return null
    }
    return localized("Already in use")
}

private fun submitStreamKey(
    rtmpServer: SettingsRtmpServer,
    stream: SettingsRtmpServerStream,
    value: String,
) {
    val streamKey = value.trim()
    if (rtmpServer.streams.any { it.streamKey == streamKey }) {
        return
    }
    stream.streamKey = streamKey
}

private fun submitLatency(stream: SettingsRtmpServerStream, value: String) {
    val latency = value.toIntOrNull() ?: return
    stream.latency = latency
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RtmpServerStreamSettingsForm(
    model: Model = LocalModel.current,
    status: StatusOther,
    rtmpServer: SettingsRtmpServer,
    stream: SettingsRtmpServerStream,
) {
    val streams = rtmpServer.streams
    val port = rtmpServer.port
    val name = stream.name
    val streamKey = stream.streamKey
    val latency = stream.latency
    val rtmpServerEnabled = rtmpServer.enabled

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(localized("Stream")) })
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    NameEditView(
                        name = name,
                        onNameChange = { stream.name = it },
                        existingNames = streams,
                    )
                    TextEditNavigationView(
                        title = localized("Stream key"),
                        value = streamKey,
                        onChange = { changeStreamKey(rtmpServer, it) },
                        onSubmit = { submitStreamKey(rtmpServer, stream, it) },
                    )
                    Text(localized("The stream name is shown in the list of cameras in scene settings."))
                }
            }
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    TextEditNavigationView(
                        title = localized("Latency"),
                        value = latency.toString(),
                        onChange = { isValidIngestLatency(it) },
                        onSubmit = { submitLatency(stream, it) },
                        footers = listOf(localized("5 or more milliseconds. 2000 ms by default.")),
                        keyboardType = KeyboardType.Number,
                        valueFormat = { "$it ms" },
                    )
                    Text(localized("The higher, the lower risk of stuttering."))
                }
            }
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = localized("Publish URLs"),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    UrlsView(
                        status = status,
                        formatUrl = { "rtmp://$it:$port$rtmpServerApp/$streamKey" },
                    )
                    Text(
                        localized(
                            "Enter one of the URLs into the RTMP publisher device to send video " +
                                "to this stream. Usually enter the WiFi or Personal Hotspot URL.",
                        ),
                    )
                }
            }
        }
    }
}

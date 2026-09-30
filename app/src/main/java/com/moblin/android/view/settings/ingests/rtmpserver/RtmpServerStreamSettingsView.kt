package com.moblin.android.view.settings.ingests.rtmpserver

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.common.various.isValidIngestLatency
import com.moblin.android.localized
import com.moblin.android.media.rtmpserver.rtmpServerApp
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.StatusOther
import com.moblin.android.various.model.updateRtmpVideoSourcesAndMics
import com.moblin.android.various.settings.SettingsRtmpServer
import com.moblin.android.various.settings.SettingsRtmpServerStream
import com.moblin.android.view.utils.NameEditView
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.view.utils.UrlsView

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
            SystemImage(name = "cable.connector", fontSize = 17.sp)
        } else {
            SystemImage(name = "cable.connector.slash", fontSize = 17.sp)
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
    val connected = model.ingests.rtmp?.isStreamConnected(streamKey = streamKey) ?: false
    NavigationLink(
        destination = {
            RtmpServerStreamSettingsForm(
                model = model,
                status = status,
                rtmpServer = rtmpServer,
                stream = stream,
            )
        },
    ) {
        IngestStreamItemView(
            name = name,
            connected = connected,
        )
    }
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

@Composable
fun RtmpServerStreamSettingsForm(
    model: Model = LocalModel.current,
    status: StatusOther,
    rtmpServer: SettingsRtmpServer,
    stream: SettingsRtmpServerStream,
) {
    val streams = rtmpServer.streams
    val port = rtmpServer.port
    val streamKey = stream.streamKey
    val latency = stream.latency

    Form(title = localized("Stream")) {
        Section(
            footer = localized("The stream name is shown in the list of cameras in scene settings."),
        ) {
            NameEditView(
                name = stream.name,
                onNameChange = {
                    val changed = stream.name != it
                    stream.name = it
                    if (changed) {
                        model.updateRtmpVideoSourcesAndMics()
                    }
                },
                existingNames = streams,
            )
            TextEditNavigationView(
                title = localized("Stream key"),
                value = streamKey,
                onChange = { changeStreamKey(rtmpServer, it) },
                onSubmit = { submitStreamKey(rtmpServer, stream, it) },
            )
        }
        Section(
            footer = localized("The higher, the lower risk of stuttering."),
        ) {
            TextEditNavigationView(
                title = localized("Latency"),
                value = latency.toString(),
                onChange = { isValidIngestLatency(it) },
                onSubmit = { submitLatency(stream, it) },
                footers = listOf(localized("5 or more milliseconds. 2000 ms by default.")),
                keyboardType = KeyboardType.Number,
                valueFormat = { "$it ms" },
            )
        }
        Section(
            header = localized("Publish URLs"),
            footer = localized(
                "Enter one of the URLs into the RTMP publisher device to send video " +
                    "to this stream. Usually enter the WiFi or Personal Hotspot URL.",
            ),
        ) {
            UrlsView(
                status = status,
                formatUrl = { "rtmp://$it:$port$rtmpServerApp/$streamKey" },
            )
        }
    }
}

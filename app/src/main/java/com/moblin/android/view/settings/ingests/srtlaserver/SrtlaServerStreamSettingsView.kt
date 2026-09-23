package com.moblin.android.view.settings.ingests.srtlaserver

import androidx.compose.runtime.Composable
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.StatusOther
import com.moblin.android.various.settings.SettingsSrtlaServer
import com.moblin.android.various.settings.SettingsSrtlaServerStream
import com.moblin.android.view.settings.ingests.rtmpserver.IngestStreamItemView
import com.moblin.android.view.utils.NameEditView
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.view.utils.UrlsView
import com.moblin.android.various.model.isSrtlaStreamConnected

private fun changeStreamId(value: String, srtlaServer: SettingsSrtlaServer): String? {
    val streamId = value.trim()
    if (!Regex("[a-zA-Z0-9]*").matches(streamId)) {
        return localized("Bad character")
    }
    if (srtlaServer.streams.any { it.streamId == streamId }) {
        return localized("Already in use")
    }
    return null
}

private fun submitStreamId(
    value: String,
    srtlaServer: SettingsSrtlaServer,
    stream: SettingsSrtlaServerStream,
) {
    val streamId = value.trim()
    if (!Regex("[a-zA-Z0-9]*").matches(streamId)) {
        return
    }
    if (srtlaServer.streams.any { it.streamId == streamId }) {
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
    model: Model = LocalModel.current,
    status: StatusOther,
    srtlaServer: SettingsSrtlaServer,
    stream: SettingsSrtlaServerStream,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    NavigationLink(
        destination = {
            SrtlaServerStreamSettingsDetailView(
                model = model,
                status = status,
                srtlaServer = srtlaServer,
                stream = stream,
            )
        },
    ) {
        IngestStreamItemView(
            name = stream.name,
            connected = model.isSrtlaStreamConnected(streamId = stream.streamId),
        )
    }
}

@Composable
fun SrtlaServerStreamSettingsDetailView(
    model: Model = LocalModel.current,
    status: StatusOther,
    srtlaServer: SettingsSrtlaServer,
    stream: SettingsSrtlaServerStream,
) {
    Form(title = localized("Stream")) {
        Section(
            footer = localized(
                "The stream name is shown in the list of cameras in scene settings."
            ),
        ) {
            NameEditView(
                name = stream.name,
                onNameChange = { stream.name = it },
                existingNames = srtlaServer.streams,
            )
            TextEditNavigationView(
                title = localized("Stream id"),
                value = stream.streamId,
                onChange = { changeStreamId(value = it, srtlaServer = srtlaServer) },
                onSubmit = {
                    submitStreamId(value = it, srtlaServer = srtlaServer, stream = stream)
                },
                footers = listOf(localized("May only contain lower case letters.")),
            )
        }
        Section(
            header = localized("Publish URLs"),
            footer = localized(
                "Enter one of the URLs into the SRT(LA) publisher device to send video " +
                    "to this stream. Usually enter the WiFi or Personal Hotspot URL."
            ),
        ) {
            UrlsView(
                status = status,
                title = localized("SRT URLs"),
                formatUrl = {
                    formatUrl(proto = "srt", ip = it, port = srtlaServer.srtPort, stream = stream)
                },
            )
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
    }
}

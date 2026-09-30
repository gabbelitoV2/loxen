package com.moblin.android.view.settings.ingests.whipserver

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
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.StatusOther
import com.moblin.android.various.settings.SettingsWhipServer
import com.moblin.android.various.settings.SettingsWhipServerStream
import com.moblin.android.view.settings.ingests.rtmpserver.IngestStreamItemView
import com.moblin.android.view.utils.NameEditView
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.view.utils.UrlsView
import com.moblin.android.various.model.isWhipStreamConnected
import com.moblin.android.various.model.updateWhipVideoSourcesAndMics

private const val WhipServerStreamDestination = "whipServerStream"

@Composable
fun WhipServerStreamSettingsView(
    model: Model = LocalModel.current,
    status: StatusOther,
    whipServer: SettingsWhipServer,
    stream: SettingsWhipServerStream,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    NavigationLink(
        destination = {
            WhipServerStreamSettingsDetail(
                model = model,
                status = status,
                whipServer = whipServer,
                stream = stream
            )
        }
    ) {
        IngestStreamItemView(
            name = stream.name,
            connected = model.isWhipStreamConnected(streamId = stream.id)
        )
    }
}

private fun changeStreamKey(whipServer: SettingsWhipServer, value: String): String? {
    if (whipServer.streams.none { it.streamKey == value.trim() }) {
        return null
    }
    return localized("Already in use")
}

private fun submitStreamKey(
    whipServer: SettingsWhipServer,
    stream: SettingsWhipServerStream,
    value: String
) {
    val streamKey = value.trim()
    if (whipServer.streams.any { it.streamKey == streamKey }) {
        return
    }
    stream.streamKey = streamKey
}

private fun submitLatency(stream: SettingsWhipServerStream, value: String) {
    val latency = value.toIntOrNull() ?: return
    stream.latency = latency
}

@Composable
fun WhipServerStreamSettingsDetail(
    model: Model = LocalModel.current,
    status: StatusOther,
    whipServer: SettingsWhipServer,
    stream: SettingsWhipServerStream
) {
    Form(title = localized("Stream")) {
        Section(
            footer = localized(
                "The stream name is shown in the list of cameras in scene settings."
            )
        ) {
            NameEditView(
                name = stream.name,
                existingNames = whipServer.streams,
                onNameChange = {
                    val changed = stream.name != it
                    stream.name = it
                    if (changed) {
                        model.updateWhipVideoSourcesAndMics()
                    }
                }
            )
            TextEditNavigationView(
                title = localized("Stream key"),
                value = stream.streamKey,
                onChange = { changeStreamKey(whipServer, it) },
                onSubmit = { submitStreamKey(whipServer, stream, it) }
            )
        }
        Section(
            footer = localized("The higher, the lower risk of stuttering.")
        ) {
            TextEditNavigationView(
                title = localized("Latency"),
                value = stream.latency.toString(),
                onChange = { isValidIngestLatency(it) },
                onSubmit = { submitLatency(stream, it) },
                footers = listOf(localized("5 or more milliseconds. 100 ms by default.")),
                keyboardType = KeyboardType.Number,
                valueFormat = { "$it ms" }
            )
        }
        Section {
            Toggle(
                title = localized("Sync timestamps"),
                isOn = stream.syncTimestamps,
                enabled = !whipServer.enabled,
                onChange = { stream.syncTimestamps = it }
            )
        }
        Section(
            header = localized("Publish URLs"),
            footer = localized(
                "Enter one of the URLs into the WHIP publisher device to send video " +
                    "to this stream. Usually enter the WiFi or Personal Hotspot URL."
            )
        ) {
            UrlsView(
                status = status,
                formatUrl = { "whip://$it:${whipServer.port}/whip/stream/${stream.streamKey}" }
            )
        }
    }
}

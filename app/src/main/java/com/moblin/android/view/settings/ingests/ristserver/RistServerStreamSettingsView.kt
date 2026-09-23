package com.moblin.android.view.settings.ingests.ristserver

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.KeyboardType
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.common.various.isValidIngestLatency
import com.moblin.android.common.various.isValidPort
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.StatusOther
import com.moblin.android.various.settings.SettingsRistServer
import com.moblin.android.various.settings.SettingsRistServerStream
import com.moblin.android.view.settings.ingests.rtmpserver.IngestStreamItemView
import com.moblin.android.view.utils.NameEditView
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.view.utils.UrlsView
import com.moblin.android.various.model.isRistStreamConnected
import com.moblin.android.various.model.reloadRistServer
import com.moblin.android.various.model.ristServerEnabled

@Composable
fun RistServerStreamSettingsView(
    model: Model = LocalModel.current,
    status: StatusOther,
    ristServer: SettingsRistServer,
    stream: SettingsRistServerStream,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    fun submitPort(value: String) {
        val port = value.trim().toIntOrNull()
        if (port == null || port > 65535) {
            return
        }
        stream.virtualDestinationPort = port
        model.reloadRistServer()
    }

    fun submitLatency(value: String) {
        val latency = value.toIntOrNull()
        if (latency == null) {
            return
        }
        stream.latency = latency
    }

    NavigationLink(
        destination = {
            Form(title = localized("Stream")) {
                Section(
                    footer = localized(
                        "The stream name is shown in the list of cameras in scene settings.",
                    ),
                ) {
                    NameEditView(
                        name = stream.name,
                        existingNames = ristServer.streams,
                        onNameChange = { stream.name = it },
                    )
                }
                Section(
                    footer = localized("The virtual destination port for this stream."),
                ) {
                    TextEditNavigationView(
                        title = localized("Virtual port"),
                        value = stream.virtualDestinationPort.toString(),
                        onChange = { isValidPort(it) },
                        onSubmit = { submitPort(it) },
                        footers = emptyList(),
                        onNavigate = onNavigate,
                        keyboardType = KeyboardType.Number,
                    )
                }
                Section(
                    footer = localized("The higher, the lower risk of stuttering."),
                ) {
                    TextEditNavigationView(
                        title = localized("Latency"),
                        value = stream.latency.toString(),
                        onChange = { isValidIngestLatency(it) },
                        onSubmit = { submitLatency(it) },
                        footers = listOf(
                            localized("5 or more milliseconds. 2000 ms by default."),
                        ),
                        onNavigate = onNavigate,
                        keyboardType = KeyboardType.Number,
                        valueFormat = { value -> "$value ms" },
                    )
                }
                Section(
                    header = localized("Publish URLs"),
                    footer = localized(
                        "Enter one of the URLs into the RIST publisher device to send video to " +
                            "this stream. Usually enter the WiFi or Personal Hotspot URL.",
                    ),
                ) {
                    UrlsView(
                        status = status,
                        showIPv6 = false,
                        formatUrl = { host ->
                            "rist://$host:${ristServer.port}" +
                                "?virt-dst-port=${stream.virtualDestinationPort}"
                        },
                    )
                }
            }
        },
    ) {
        IngestStreamItemView(
            name = stream.name,
            connected = model.isRistStreamConnected(stream.virtualDestinationPort),
        )
    }
}

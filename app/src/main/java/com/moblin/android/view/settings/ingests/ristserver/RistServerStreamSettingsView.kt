package com.moblin.android.view.settings.ingests.ristserver

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.moblin.android.common.various.isValidIngestLatency
import com.moblin.android.common.various.isValidPort
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.StatusOther
import com.moblin.android.various.settings.SettingsRistServer
import com.moblin.android.various.settings.SettingsRistServerStream
import com.moblin.android.view.utils.NameEditView
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.view.utils.UrlsView
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RistServerStreamSettingsView(
    model: Model = LocalModel.current,
    status: StatusOther,
    ristServer: SettingsRistServer,
    stream: SettingsRistServerStream,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    fun submitPort(value: String) {
        val parsedPort = value.trim().toIntOrNull()
        if (parsedPort == null || parsedPort !in 0..65535) {
            return
        }
        stream.virtualDestinationPort = parsedPort
        Unit
    }

    fun submitLatency(value: String) {
        val parsedLatency = value.toIntOrNull()
        if (parsedLatency == null) {
            return
        }
        stream.latency = parsedLatency
    }

    val streamName = stream.name
    val virtualDestinationPort = stream.virtualDestinationPort
    val streamLatency = stream.latency
    val ristServerEnabled = ristServer.enabled
    val ristServerPort = ristServer.port

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(localized("Stream")) })
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
        ) {
            item {
                NameEditView(
                    name = streamName,
                    existingNames = ristServer.streams,
                    onNameChange = { stream.name = it },
                )
                Text(
                    text = localized(
                        "The stream name is shown in the list of cameras in scene settings.",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            item {
                TextEditNavigationView(
                    title = localized("Virtual port"),
                    value = virtualDestinationPort.toString(),
                    onChange = { isValidPort(it) },
                    onSubmit = { submitPort(it) },
                    footers = emptyList(),
                    onNavigate = onNavigate,
                    keyboardType = KeyboardType.Number,
                )
                Text(
                    text = localized("The virtual destination port for this stream."),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            item {
                TextEditNavigationView(
                    title = localized("Latency"),
                    value = streamLatency.toString(),
                    onChange = { isValidIngestLatency(it) },
                    onSubmit = { submitLatency(it) },
                    footers = listOf(
                        localized("5 or more milliseconds. 2000 ms by default."),
                    ),
                    onNavigate = onNavigate,
                    keyboardType = KeyboardType.Number,
                    valueFormat = { value -> "$value ms" },
                )
                Text(
                    text = localized("The higher, the lower risk of stuttering."),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            item {
                Text(
                    text = localized("Publish URLs"),
                    style = MaterialTheme.typography.titleMedium,
                )
                UrlsView(
                    status = status,
                    showIPv6 = false,
                    formatUrl = { host ->
                        "rist://$host:$ristServerPort?virt-dst-port=$virtualDestinationPort"
                    },
                )
                Column {
                    Text(
                        text = localized(
                            "Enter one of the URLs into the RIST publisher device to send " +
                                "video to this stream. Usually enter the WiFi or Personal " +
                                "Hotspot URL.",
                        ),
                    )
                }
            }
        }
    }
}

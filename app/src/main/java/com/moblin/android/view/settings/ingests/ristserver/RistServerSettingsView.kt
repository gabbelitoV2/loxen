package com.moblin.android.view.settings.ingests.ristserver

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.isValidPort
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsRistServer
import com.moblin.android.various.settings.SettingsRistServerStream
import com.moblin.android.various.utils.makeOffsets
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.InfoBannerView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

private const val ristServerSettingsDestination = "RIST server"

@Composable
fun RistServerSettingsView(
    model: Model = LocalModel.current,
    ristServer: SettingsRistServer,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate(ristServerSettingsDestination) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("RIST server")
        Spacer(Modifier.weight(1f))
        GrayTextView(text = status(ristServer))
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RistServerSettingsDetailView(
    model: Model = LocalModel.current,
    ristServer: SettingsRistServer,
) {
    val statusOther by model.statusOther.collectAsState()
    val enabled = ristServer.enabled
    val port = ristServer.port
    val streams = ristServer.streams

    LaunchedEffect(enabled) {
        model.reloadRistServer()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("RIST server") })
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text("The RIST server allows Moblin to receive video streams over the network.")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Enabled")
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = enabled,
                    onCheckedChange = { ristServer.enabled = it },
                )
            }
            if (enabled) {
                InfoBannerView(text = "Disable the RIST server to change its settings.")
            }
            Box(modifier = Modifier.alpha(if (enabled) 0.5f else 1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !enabled) { },
                ) {
                    TextEditNavigationView(
                        title = localized("Port"),
                        value = port.toString(),
                        onChange = { value -> isValidPort(value) },
                        onSubmit = { value -> submitPort(model, ristServer, value) },
                        keyboardType = KeyboardType.Number,
                    )
                }
            }
            Text("The UDP port the RIST server listens for RIST publishers on.")
            Text("Streams", style = MaterialTheme.typography.titleMedium)
            for (stream in streams) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {},
                            onLongClick = {
                                if (!enabled) {
                                    makeOffsets(streams, stream.id)?.let { offsets ->
                                        deleteStream(model, ristServer, offsets)
                                    }
                                }
                            },
                        ),
                ) {
                    RistServerStreamSettingsView(
                        status = statusOther,
                        ristServer = ristServer,
                        stream = stream,
                    )
                }
            }
            Box(modifier = Modifier.alpha(if (model.ristServerEnabled()) 0.5f else 1f)) {
                CreateButtonView(
                    action = {
                        val stream = SettingsRistServerStream()
                        stream.name = makeUniqueName(
                            SettingsRistServerStream.baseName,
                            streams,
                        )
                        stream.virtualDestinationPort = ristServer.makeUniqueVirtualDestinationPort()
                        streams.add(stream)
                        model.updateMicsListAsync()
                    },
                )
            }
            Text("Each stream can receive video from one RIST publisher.")
            Text("")
            SwipeLeftToDeleteHelpView(kind = localized("a stream"))
        }
    }
}

private fun submitPort(model: Model, ristServer: SettingsRistServer, value: String) {
    val port = value.toIntOrNull() ?: return
    if (port !in 0..65535) {
        return
    }
    ristServer.port = port
    model.reloadRistServer()
}

private fun status(ristServer: SettingsRistServer): String {
    return if (ristServer.enabled) {
        ristServer.streams.size.toString()
    } else {
        "0"
    }
}

private fun deleteStream(model: Model, ristServer: SettingsRistServer, indexes: Set<Int>) {
    for (index in indexes.sortedDescending()) {
        if (index in ristServer.streams.indices) {
            ristServer.streams.removeAt(index)
        }
    }
    model.reloadRistServer()
    model.updateMicsListAsync()
}

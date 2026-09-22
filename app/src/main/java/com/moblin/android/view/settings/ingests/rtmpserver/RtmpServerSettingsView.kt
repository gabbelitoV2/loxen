package com.moblin.android.view.settings.ingests.rtmpserver

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.moblin.android.common.various.isValidPort
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsRtmpServer
import com.moblin.android.various.settings.SettingsRtmpServerStream
import com.moblin.android.various.utils.makeOffsets
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.various.utils.randomHumanString
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.InfoBannerView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

@Composable
fun RtmpServerSettingsView(
    model: Model = LocalModel.current,
    rtmpServer: SettingsRtmpServer,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("RtmpServerSettings") },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("RTMP server")
        Spacer(Modifier.weight(1f))
        GrayTextView(text = status(rtmpServer))
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RtmpServerSettingsForm(
    model: Model = LocalModel.current,
    rtmpServer: SettingsRtmpServer,
) {
    val enabled by rtmpServer.enabled.collectAsState()
    val statusOther by model.statusOther.collectAsState()
    val streams = rtmpServer.streams
    LaunchedEffect(enabled) {
        model.reloadRtmpServer()
    }
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("RTMP server") })
        },
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues)) {
            item {
                Text(
                    "The RTMP server allows Moblin to receive video streams over the network. " +
                        "This allows the use of some drones and other cameras as sources.",
                )
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Enabled")
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = enabled,
                        onCheckedChange = { rtmpServer.enabled.value = it },
                    )
                }
            }
            if (enabled) {
                item {
                    InfoBannerView(text = "Disable the RTMP server to change its settings.")
                }
            }
            item {
                TextEditNavigationView(
                    title = "Port",
                    value = rtmpServer.port.toString(),
                    onChange = { isValidPort(it) },
                    onSubmit = { submitPort(model, rtmpServer, it) },
                    keyboardType = KeyboardType.Number,
                    enabled = !enabled,
                )
            }
            item {
                Text("The TCP port the RTMP server listens for RTMP publishers on.")
            }
            item {
                Text("Streams")
            }
            items(streams, key = { it.id }) { stream ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {},
                            onLongClick = {
                                if (!enabled) {
                                    makeOffsets(streams, stream.id)?.let { offsets ->
                                        deleteStream(model, rtmpServer, offsets)
                                    }
                                }
                            },
                        ),
                ) {
                    RtmpServerStreamSettingsView(
                        status = statusOther,
                        rtmpServer = rtmpServer,
                        stream = stream,
                    )
                }
            }
            item {
                CreateButtonView(enabled = !model.rtmpServerEnabled()) {
                    val stream = SettingsRtmpServerStream()
                    stream.name = makeUniqueName(SettingsRtmpServerStream.baseName, streams)
                    while (true) {
                        stream.streamKey = randomHumanString()
                        if (model.getRtmpStream(stream.streamKey) == null) {
                            break
                        }
                    }
                    streams.add(stream)
                    model.updateMicsListAsync()
                }
            }
            item {
                Column(horizontalAlignment = Alignment.Start) {
                    Text("Each stream can receive video from one RTMP publisher, typically a drone.")
                    Text("")
                    SwipeLeftToDeleteHelpView(kind = localized("a stream"))
                }
            }
        }
    }
}

private fun submitPort(model: Model, rtmpServer: SettingsRtmpServer, value: String) {
    val port = value.toIntOrNull() ?: return
    if (port < 0 || port > 65535) {
        return
    }
    rtmpServer.port = port
    model.reloadRtmpServer()
}

private fun status(rtmpServer: SettingsRtmpServer): String {
    return if (rtmpServer.enabled.value) {
        rtmpServer.streams.size.toString()
    } else {
        "0"
    }
}

private fun deleteStream(model: Model, rtmpServer: SettingsRtmpServer, indexes: List<Int>) {
    for (index in indexes.sortedDescending()) {
        if (index in rtmpServer.streams.indices) {
            rtmpServer.streams.removeAt(index)
        }
    }
    model.reloadRtmpServer()
    model.updateMicsListAsync()
}

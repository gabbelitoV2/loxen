package com.moblin.android.view.settings.ingests.srtlaserver

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.moblin.android.common.various.isValidPort
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsSrtlaServer
import com.moblin.android.various.settings.SettingsSrtlaServerStream
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

private fun submitSrtPort(srtlaServer: SettingsSrtlaServer, model: Model, value: String) {
    val port = value.toIntOrNull() ?: return
    if (port !in 0..65535) {
        return
    }
    srtlaServer.srtPort = port
    model.reloadSrtlaServer()
}

private fun submitSrtlaPort(srtlaServer: SettingsSrtlaServer, model: Model, value: String) {
    val port = value.toIntOrNull() ?: return
    if (port !in 0..65535) {
        return
    }
    srtlaServer.srtlaPort = port
    model.reloadSrtlaServer()
}

private fun status(srtlaServer: SettingsSrtlaServer): String {
    return if (srtlaServer.enabled) {
        srtlaServer.streams.count().toString()
    } else {
        "0"
    }
}

private fun deleteStream(srtlaServer: SettingsSrtlaServer, model: Model, indexes: List<Int>) {
    indexes.sortedDescending().forEach { index ->
        if (index in srtlaServer.streams.indices) {
            srtlaServer.streams.removeAt(index)
        }
    }
    model.reloadSrtlaServer()
    model.updateMicsListAsync()
}

@Composable
fun SrtlaServerSettingsView(
    model: Model = LocalModel.current,
    srtlaServer: SettingsSrtlaServer,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("SrtlaServerSettingsForm") },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("SRT(LA) server")
        Spacer(Modifier.weight(1f))
        GrayTextView(text = status(srtlaServer))
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SrtlaServerSettingsForm(
    model: Model = LocalModel.current,
    srtlaServer: SettingsSrtlaServer,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("SRT(LA) server") })
        },
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Enabled")
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = srtlaServer.enabled,
                        onCheckedChange = { enabled ->
                            srtlaServer.enabled = enabled
                            model.reloadSrtlaServer()
                        },
                    )
                }
            }
            if (srtlaServer.enabled) {
                item {
                    InfoBannerView(text = "Disable the SRT(LA) server to change its settings.")
                }
            }
            item {
                TextEditNavigationView(
                    title = localized("SRT port"),
                    value = srtlaServer.srtPort.toString(),
                    onChange = { value -> isValidPort(value) },
                    onSubmit = { value -> submitSrtPort(srtlaServer, model, value) },
                    keyboardType = KeyboardType.Number,
                    enabled = !srtlaServer.enabled,
                )
            }
            item {
                Text("The UDP port the SRT(LA) server listens for SRT publishers on.")
            }
            item {
                TextEditNavigationView(
                    title = localized("SRTLA port"),
                    value = srtlaServer.srtlaPort.toString(),
                    onChange = { value -> isValidPort(value) },
                    onSubmit = { value -> submitSrtlaPort(srtlaServer, model, value) },
                    keyboardType = KeyboardType.Number,
                    enabled = !srtlaServer.enabled,
                )
            }
            item {
                Column(horizontalAlignment = Alignment.Start) {
                    Text("The UDP port the SRT(LA) server listens for SRTLA publishers on.")
                    Text("")
                    Text("The UDP port ${srtlaServer.srtlaSrtPort()} will also be used.")
                }
            }
            item {
                Text("Streams", style = MaterialTheme.typography.titleSmall)
            }
            items(items = srtlaServer.streams, key = { it.id }) { stream ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .combinedClickable(
                                onClick = {},
                                onLongClick = {
                                    if (!model.srtlaServerEnabled()) {
                                        TODO("contextMenuDeleteButton has no Android counterpart")
                                    }
                                },
                            ),
                    ) {
                        SrtlaServerStreamSettingsView(
                            status = model.statusOther,
                            srtlaServer = srtlaServer,
                            stream = stream,
                        )
                    }
                    if (!model.srtlaServerEnabled()) {
                        IconButton(
                            onClick = {
                                val offsets = makeOffsets(srtlaServer.streams, stream.id)
                                if (offsets != null) {
                                    deleteStream(srtlaServer, model, offsets)
                                }
                            },
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                        }
                    }
                }
            }
            item {
                CreateButtonView(enabled = !srtlaServer.enabled) {
                    val stream = SettingsSrtlaServerStream()
                    stream.name = makeUniqueName(
                        name = SettingsSrtlaServerStream.baseName,
                        existingNames = srtlaServer.streams,
                    )
                    while (true) {
                        stream.streamId = randomHumanString()
                        if (model.getSrtlaStream(streamId = stream.streamId) == null) {
                            break
                        }
                    }
                    srtlaServer.streams.add(stream)
                    model.updateMicsListAsync()
                }
            }
            item {
                Column(horizontalAlignment = Alignment.Start) {
                    Text("Each stream can receive video from one SRT(LA) publisher.")
                    Text("")
                    SwipeLeftToDeleteHelpView(kind = localized("a stream"))
                }
            }
        }
    }
}

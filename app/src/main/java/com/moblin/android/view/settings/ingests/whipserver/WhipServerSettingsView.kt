package com.moblin.android.view.settings.ingests.whipserver

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.isValidPort
import com.moblin.android.localized
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.InfoBannerView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsWhipServer
import com.moblin.android.various.settings.SettingsWhipServerStream
import com.moblin.android.various.utils.makeOffsets
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.various.utils.randomHumanString
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WhipServerSettingsView(
    model: Model = LocalModel.current,
    whipServer: SettingsWhipServer,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val enabled = whipServer.enabled
    val port = whipServer.port
    val streams = whipServer.streams
    val statusOther = model.statusOther

    LaunchedEffect(enabled) {
        Unit
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("WHIP server") })
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate("WhipServerSettingsView") }
                .padding(16.dp),
        ) {
            Text("WHIP server")
            Spacer(Modifier.weight(1f))
            GrayTextView(text = status(whipServer))
        }
        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                Text("The WHIP server allows Moblin to receive video streams over the network.")
            }
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("Enabled")
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = enabled,
                        onCheckedChange = { whipServer.enabled = it },
                    )
                }
            }
            if (enabled) {
                item {
                    InfoBannerView(text = "Disable the WHIP server to change its settings.")
                }
            }
            item {
                TextEditNavigationView(
                    title = localized("Port"),
                    value = port.toString(),
                    onChange = { isValidPort(it) },
                    onSubmit = { submitPort(model, whipServer, it) },
                    keyboardType = KeyboardType.Number,
                    onNavigate = onNavigate,
                )
            }
            item {
                Text("The TCP port the WHIP server listens for WHIP streams on.")
            }
            item {
                Text("Streams")
            }
            itemsIndexed(streams, key = { _, stream -> stream.id }) { index, stream ->
                var menuExpanded by remember { mutableStateOf(false) }
                val row: @Composable () -> Unit = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {},
                                onLongClick = {
                                    if (!enabled) {
                                        menuExpanded = true
                                    }
                                },
                            ),
                    ) {
                        WhipServerStreamSettingsView(
                            status = statusOther,
                            whipServer = whipServer,
                            stream = stream,
                        )
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                enabled = !enabled,
                                onClick = {
                                    menuExpanded = false
                                    val offsets = streamIndex(streams, stream.id)
                                    if (offsets != null) {
                                        deleteStream(model, whipServer, offsets)
                                    }
                                },
                            )
                        }
                    }
                }
                if (!enabled) {
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                deleteStream(model, whipServer, index)
                                true
                            } else {
                                false
                            }
                        },
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {},
                    ) {
                        row()
                    }
                } else {
                    row()
                }
            }
            item {
                CreateButtonView {
                    val stream = SettingsWhipServerStream()
                    stream.name = makeUniqueName(
                        name = SettingsWhipServerStream.baseName,
                        existingNames = whipServer.streams,
                    )
                    while (true) {
                        stream.streamKey = randomHumanString()
                        if (TODO("Model.getWhipStream(streamKey:) is not available") == null) {
                            break
                        }
                    }
                    whipServer.streams.add(stream)
                    Unit
                }
            }
            item {
                SwipeLeftToDeleteHelpView(kind = localized("a stream"))
            }
        }
    }
}

private fun streamIndex(streams: List<SettingsWhipServerStream>, id: java.util.UUID): Int? {
    val index = streams.indexOfFirst { it.id == id }
    return if (index == -1) null else index
}

private fun status(whipServer: SettingsWhipServer): String {
    return if (whipServer.enabled) {
        whipServer.streams.size.toString()
    } else {
        "0"
    }
}

private fun submitPort(model: Model, whipServer: SettingsWhipServer, value: String) {
    val port = value.toIntOrNull() ?: return
    if (port < 0 || port > 65535) {
        return
    }
    whipServer.port = port
    Unit
}

private fun deleteStream(model: Model, whipServer: SettingsWhipServer, index: Int) {
    if (index in whipServer.streams.indices) {
        whipServer.streams.removeAt(index)
    }
    Unit
    Unit
}

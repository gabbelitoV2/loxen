package com.moblin.android.view.settings.ingests.rtspclient

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsRtspClient
import com.moblin.android.various.settings.SettingsRtspClientStream
import com.moblin.android.various.utils.makeOffsets
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

private fun status(numberOfEnabledStreams: Int): String {
    return numberOfEnabledStreams.toString()
}

private fun deleteStream(model: Model, rtspClient: SettingsRtspClient, indexes: Set<Int>) {
    rtspClient.streams.value = rtspClient.streams.value.filterIndexed { index, _ ->
        !indexes.contains(index)
    }
    model.reloadRtspClient()
}

@Composable
fun RtspClientSettingsView(
    model: Model = LocalModel.current,
    rtspClient: SettingsRtspClient,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val streams by rtspClient.streams.collectAsState()
    val numberOfEnabledStreams = streams.count { it.enabled }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("rtspClientSettings") }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("RTSP client")
        Spacer(Modifier.weight(1f))
        GrayTextView(text = status(numberOfEnabledStreams))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RtspClientSettingsViewDestination(
    model: Model = LocalModel.current,
    rtspClient: SettingsRtspClient,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val streams by rtspClient.streams.collectAsState()
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("RTSP client") },
            navigationIcon = {
                IconButton(onClick = { onNavigate("back") }) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
                }
            },
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Text(
                    text = "Streams",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
                )
            }
            items(items = streams, key = { it.id }) { stream ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        RtspClientStreamSettingsView(rtspClient = rtspClient, stream = stream)
                    }
                    var menuExpanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                menuExpanded = false
                                makeOffsets(streams, stream.id)?.let { offsets ->
                                    deleteStream(model, rtspClient, offsets)
                                }
                            },
                        )
                    }
                }
            }
            item {
                CreateButtonView {
                    val stream = SettingsRtspClientStream()
                    stream.name = makeUniqueName(SettingsRtspClientStream.baseName, streams)
                    rtspClient.streams.value = streams + stream
                }
            }
            item {
                SwipeLeftToDeleteHelpView(kind = localized("a stream"))
            }
        }
    }
}

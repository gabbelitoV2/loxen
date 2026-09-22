package com.moblin.android.view.settings.streams.stream.multistreaming

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.various.settings.SettingsStreamMultiStreaming
import com.moblin.android.various.settings.SettingsStreamMultiStreamingDestination
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.NameEditView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.view.utils.TextItemLocalizedView
import kotlinx.coroutines.launch

@Composable
fun DestinationView(
    model: Model,
    stream: SettingsStream,
    destination: SettingsStreamMultiStreamingDestination,
    onNavigate: (String) -> Unit,
) {
    val isLive by model.isLive.collectAsState()
    val isRecording by model.isRecording.collectAsState()
    val locked = stream.enabled && (isLive || isRecording)
    val scope = rememberCoroutineScope()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = destination.name,
            modifier = Modifier
                .weight(1f)
                .clickable { onNavigate("Destination") },
        )
        Switch(
            checked = destination.enabled,
            onCheckedChange = {
                destination.enabled = it
                scope.launch {
                    model.reloadStreamIfEnabled(stream)
                }
            },
            enabled = !locked,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DestinationSettingsView(
    model: Model,
    stream: SettingsStream,
    destination: SettingsStreamMultiStreamingDestination,
    onNavigate: (String) -> Unit,
) {
    val isLive by model.isLive.collectAsState()
    val isRecording by model.isRecording.collectAsState()
    val locked = stream.enabled && (isLive || isRecording)
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Destination") })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            NameEditView(
                name = destination.name,
                existingNames = stream.multiStreaming.destinations,
                onChange = { destination.name = it },
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !locked) { onNavigate("StreamMultiStreamingUrl") },
            ) {
                TextItemLocalizedView(name = "URL", value = destination.url, sensitive = true)
            }
        }
    }
}

private fun numberOfEnabledDestinations(multiStreaming: SettingsStreamMultiStreaming): String {
    val count = multiStreaming.destinations.count { it.enabled }
    return count.toString()
}

@Composable
fun StreamMultiStreamingSettingsView(
    multiStreaming: SettingsStreamMultiStreaming,
    onNavigate: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("Multi streaming") }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Multi streaming")
        Spacer(Modifier.weight(1f))
        GrayTextView(text = numberOfEnabledDestinations(multiStreaming))
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MultiStreamingSettingsView(
    model: Model,
    stream: SettingsStream,
    multiStreaming: SettingsStreamMultiStreaming,
    onNavigate: (String) -> Unit,
) {
    val isLive by model.isLive.collectAsState()
    val isRecording by model.isRecording.collectAsState()
    val locked = stream.enabled && (isLive || isRecording)
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Multi streaming") })
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Stream to additional destinations directly from this device.")
                    Text("")
                    Text("⚠️ This will increase network bandwidth usage, system load and device heat.")
                }
            }
            items(items = multiStreaming.destinations, key = { it.id.toString() }) { destination ->
                var deleteMenuVisible by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier.combinedClickable(
                        enabled = !locked,
                        onClick = {},
                        onLongClick = { deleteMenuVisible = true },
                    ),
                ) {
                    DestinationView(
                        model = model,
                        stream = stream,
                        destination = destination,
                        onNavigate = onNavigate,
                    )
                    DropdownMenu(
                        expanded = deleteMenuVisible,
                        onDismissRequest = { deleteMenuVisible = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(localized("Delete")) },
                            onClick = {
                                deleteMenuVisible = false
                                multiStreaming.destinations.removeAll { item -> item.id == destination.id }
                            },
                        )
                    }
                }
            }
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    CreateButtonView(
                        enabled = !locked,
                        onClick = {
                            val destination = SettingsStreamMultiStreamingDestination()
                            destination.name = makeUniqueName(
                                name = SettingsStreamMultiStreamingDestination.baseName,
                                existingNames = multiStreaming.destinations,
                            )
                            multiStreaming.destinations.add(destination)
                        },
                    )
                    SwipeLeftToDeleteHelpView(kind = localized("a destination"))
                }
            }
        }
    }
}

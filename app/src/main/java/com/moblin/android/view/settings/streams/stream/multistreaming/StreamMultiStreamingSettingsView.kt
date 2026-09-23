package com.moblin.android.view.settings.streams.stream.multistreaming

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.various.settings.SettingsStreamMultiStreaming
import com.moblin.android.various.settings.SettingsStreamMultiStreamingDestination
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.settings.streams.stream.url.StreamMultiStreamingUrlView
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.NameEditView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.view.utils.TextItemLocalizedView
import kotlinx.coroutines.launch
import com.moblin.android.various.model.reloadStreamIfEnabled

@Composable
fun DestinationView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
    destination: SettingsStreamMultiStreamingDestination,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val isLive by model.isLive.collectAsState()
    val isRecording by model.isRecording.collectAsState()
    val locked = stream.enabled && (isLive || isRecording)
    val scope = rememberCoroutineScope()
    NavigationLink(
        destination = {
            DestinationSettingsView(
                model = model,
                stream = stream,
                destination = destination,
                onNavigate = onNavigate,
            )
        },
    ) {
        Toggle(
            title = destination.name,
            isOn = destination.enabled,
            enabled = !locked,
        ) { enabled ->
            destination.enabled = enabled
            scope.launch {
                model.reloadStreamIfEnabled(stream)
            }
        }
    }
}

@Composable
fun DestinationSettingsView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
    destination: SettingsStreamMultiStreamingDestination,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val isLive by model.isLive.collectAsState()
    val isRecording by model.isRecording.collectAsState()
    val locked = stream.enabled && (isLive || isRecording)
    Form(title = "Destination") {
        Section {
            NameEditView(
                name = destination.name,
                existingNames = stream.multiStreaming.destinations,
                onNameChange = { destination.name = it },
            )
        }
        Section {
            NavigationLink(
                destination = {
                    StreamMultiStreamingUrlView(stream = stream, destination = destination)
                },
                enabled = !locked,
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
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    FormRow(onClick = { onNavigate("Multi streaming") }) {
        Text("Multi streaming")
        Spacer(Modifier.weight(1f))
        GrayTextView(text = numberOfEnabledDestinations(multiStreaming))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MultiStreamingSettingsView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
    multiStreaming: SettingsStreamMultiStreaming,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val isLive by model.isLive.collectAsState()
    val isRecording by model.isRecording.collectAsState()
    val locked = stream.enabled && (isLive || isRecording)
    Form(title = "Multi streaming") {
        Section {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(localized("Stream to additional destinations directly from this device."))
                Text("")
                Text(localized("⚠️ This will increase network bandwidth usage, system load and device heat."))
            }
        }
        Section(
            footerContent = {
                SwipeLeftToDeleteHelpView(kind = localized("a destination"))
            },
        ) {
            multiStreaming.destinations.forEach { destination ->
                key(destination.id) {
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
                                    multiStreaming.destinations.removeAll { item ->
                                        item.id == destination.id
                                    }
                                },
                            )
                        }
                    }
                }
            }
            CreateButtonView(
                action = {
                    val destination = SettingsStreamMultiStreamingDestination()
                    destination.name = makeUniqueName(
                        name = SettingsStreamMultiStreamingDestination.baseName,
                        existingNames = multiStreaming.destinations,
                    )
                    multiStreaming.destinations.add(destination)
                },
            )
        }
    }
}

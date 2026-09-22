package com.moblin.android.view.settings.streams

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.moblin.android.localized
import com.moblin.android.various.model.CreateStreamWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.various.utils.isMac
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.streams.stream.StreamWizardSettingsView
import com.moblin.android.view.utils.ContextMenuDeleteButtonView
import com.moblin.android.view.utils.ContextMenuDuplicateButtonView
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.DraggableItemPrefixView
import com.moblin.android.view.utils.SwipeLeftToDeleteButtonView
import com.moblin.android.view.utils.SwipeLeftToDuplicateButtonView
import com.moblin.android.view.utils.SwipeLeftToDuplicateOrDeleteHelpView
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StreamItemView(
    model: Model = LocalModel.current,
    database: Database,
    stream: SettingsStream,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val isLive by model.isLive.collectAsState()
    val isRecording by model.isRecording.collectAsState()
    var contextMenuExpanded by remember { mutableStateOf(false) }

    val duplicate: () -> Unit = {
        val clone = stream.clone()
        clone.name = makeUniqueName(stream.name, database.streams)
        database.streams.add(clone)
    }

    val delete: () -> Unit = {
        database.streams.removeAll { it === stream }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { onNavigate("StreamSettingsView") },
                        onLongClick = { contextMenuExpanded = true },
                    ),
            ) {
                DraggableItemPrefixView()
                Text(stream.name)
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = stream.enabled,
                    onCheckedChange = { _ ->
                        model.setCurrentStream(stream)
                        model.reloadStreamIfEnabled(stream)
                    },
                    enabled = !(stream.enabled || isLive || isRecording),
                )
            }
            DropdownMenu(
                expanded = contextMenuExpanded,
                onDismissRequest = { contextMenuExpanded = false },
            ) {
                if (isMac()) {
                    ContextMenuDuplicateButtonView {
                        contextMenuExpanded = false
                        duplicate()
                    }
                    if (!stream.enabled) {
                        ContextMenuDeleteButtonView {
                            contextMenuExpanded = false
                            delete()
                        }
                    }
                }
            }
        }
        Row {
            if (!stream.enabled) {
                SwipeLeftToDeleteButtonView {
                    delete()
                }
            }
            SwipeLeftToDuplicateButtonView {
                duplicate()
            }
        }
    }
}

private fun moveStreams(streams: MutableList<SettingsStream>, froms: List<Int>, to: Int) {
    val sortedFroms = froms.sorted()
    val moved = sortedFroms.map { streams[it] }
    for (index in sortedFroms.sortedDescending()) {
        streams.removeAt(index)
    }
    val insertIndex = (to - sortedFroms.count { it < to }).coerceIn(0, streams.size)
    streams.addAll(insertIndex, moved)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamsSettingsView(
    model: Model = LocalModel.current,
    createStreamWizard: CreateStreamWizard,
    database: Database,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val presenting by createStreamWizard.presenting.collectAsState()
    val isLive by model.isLive.collectAsState()
    val isRecording by model.isRecording.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Streams") })
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(database.streams) { stream ->
                StreamItemView(
                    model = model,
                    database = database,
                    stream = stream,
                    onNavigate = onNavigate,
                )
            }
            item {
                CreateButtonView(enabled = !(isLive || isRecording)) {
                    model.resetWizard()
                    createStreamWizard.presenting.value = true
                }
            }
            item {
                SwipeLeftToDuplicateOrDeleteHelpView(kind = localized("a stream"))
            }
        }
    }

    if (presenting) {
        ModalBottomSheet(
            onDismissRequest = { createStreamWizard.presenting.value = false },
        ) {
            StreamWizardSettingsView(
                model = model,
                createStreamWizard = createStreamWizard,
            )
        }
    }
}

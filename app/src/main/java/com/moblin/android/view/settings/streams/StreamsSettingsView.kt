package com.moblin.android.view.settings.streams

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Sheet
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.various.model.CreateStreamWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.reloadStreamIfEnabled
import com.moblin.android.various.model.setCurrentStream
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.streams.stream.StreamSettingsView
import com.moblin.android.view.settings.streams.stream.StreamWizardSettingsView
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.DraggableItemPrefixView
import com.moblin.android.view.utils.SwipeLeftToDeleteButtonView
import com.moblin.android.view.utils.SwipeLeftToDuplicateButtonView
import com.moblin.android.view.utils.SwipeLeftToDuplicateOrDeleteHelpView
import com.moblin.android.various.model.resetWizard

@Composable
private fun StreamItemView(
    model: Model = LocalModel.current,
    database: Database,
    stream: SettingsStream,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val isLive by model.isLive.collectAsState()
    val isRecording by model.isRecording.collectAsState()

    val duplicate: () -> Unit = {
        val clone = stream.clone()
        clone.name = makeUniqueName(stream.name, database.streams)
        database.streams.add(clone)
    }

    val delete: () -> Unit = {
        database.streams.removeAll { it === stream }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        NavigationLink(
            destination = {
                StreamSettingsView(database = database, stream = stream)
            },
        ) {
            DraggableItemPrefixView()
            Toggle(
                title = stream.name,
                isOn = stream.enabled,
                enabled = !(stream.enabled || isLive || isRecording),
                onChange = { _ ->
                    model.setCurrentStream(stream)
                    model.reloadStreamIfEnabled(stream)
                },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!stream.enabled) {
                SwipeLeftToDeleteButtonView(action = {
                    delete()
                })
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

@Composable
fun StreamsSettingsView(
    model: Model = LocalModel.current,
    createStreamWizard: CreateStreamWizard,
    database: Database,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val isLive by model.isLive.collectAsState()
    val isRecording by model.isRecording.collectAsState()

    Form(title = "Streams") {
        Section(
            footerContent = {
                SwipeLeftToDuplicateOrDeleteHelpView(kind = localized("a stream"))
            },
        ) {
            database.streams.forEach { stream ->
                key(stream.id) {
                    StreamItemView(
                        model = model,
                        database = database,
                        stream = stream,
                        onNavigate = onNavigate,
                    )
                }
            }
            CreateButtonView {
                if (!isLive && !isRecording) {
                    model.resetWizard()
                    createStreamWizard.presenting = true
                }
            }
        }
    }

    if (createStreamWizard.presenting) {
        Sheet(onDismissRequest = { createStreamWizard.presenting = false }) {
            StreamWizardSettingsView(
                model = model,
                createStreamWizard = createStreamWizard,
            )
        }
    }
}

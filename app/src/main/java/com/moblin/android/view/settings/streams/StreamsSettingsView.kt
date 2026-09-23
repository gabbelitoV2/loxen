package com.moblin.android.view.settings.streams

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.ContextMenu
import com.moblin.android.platform.swiftui.ForEach
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.HorizontalEdge
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Sheet
import com.moblin.android.platform.swiftui.SwipeActions
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.move
import com.moblin.android.various.model.CreateStreamWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.reloadStreamIfEnabled
import com.moblin.android.various.model.resetWizard
import com.moblin.android.various.model.setCurrentStream
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.various.utils.isMac
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.streams.stream.StreamSettingsView
import com.moblin.android.view.settings.streams.stream.StreamWizardSettingsView
import com.moblin.android.view.utils.ContextMenuDeleteButtonView
import com.moblin.android.view.utils.ContextMenuDuplicateButtonView
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.DraggableItemPrefixView
import com.moblin.android.view.utils.SwipeLeftToDeleteButtonView
import com.moblin.android.view.utils.SwipeLeftToDuplicateButtonView
import com.moblin.android.view.utils.SwipeLeftToDuplicateOrDeleteHelpView

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

    ContextMenu(
        menu = {
            if (isMac()) {
                ContextMenuDuplicateButtonView {
                    duplicate()
                }
                if (!stream.enabled) {
                    ContextMenuDeleteButtonView {
                        delete()
                    }
                }
            }
        },
    ) {
        SwipeActions(
            edge = HorizontalEdge.trailing,
            allowsFullSwipe = false,
            actions = {
                if (!stream.enabled) {
                    SwipeLeftToDeleteButtonView {
                        delete()
                    }
                }
                SwipeLeftToDuplicateButtonView {
                    duplicate()
                }
            },
        ) {
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
        }
    }
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
            ForEach(
                database.streams,
                id = { it.id },
                onMove = { froms, to ->
                    database.streams.move(fromOffsets = froms, toOffset = to)
                },
            ) { stream ->
                StreamItemView(
                    model = model,
                    database = database,
                    stream = stream,
                    onNavigate = onNavigate,
                )
            }
            CreateButtonView {
                if (!isLive && !isRecording) {
                    model.resetWizard()
                    createStreamWizard.presenting = true
                }
            }
        }
    }

    Sheet(
        isPresented = createStreamWizard.presenting,
        onDismissRequest = { createStreamWizard.presenting = false },
    ) {
        StreamWizardSettingsView(
            model = model,
            createStreamWizard = createStreamWizard,
        )
    }
}

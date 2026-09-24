package com.moblin.android.view.controlbar.quickbutton

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalModel
import com.moblin.android.localized
import com.moblin.android.obs.ObsOutputState
import com.moblin.android.obs.obsMaximumAudioDelay
import com.moblin.android.obs.obsMinimumAudioDelay
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.ConfirmationDialog
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.Label
import com.moblin.android.platform.swiftui.LocalTint
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.PickerStyle
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.ObsSceneMediaSource
import com.moblin.android.various.model.QuickButtonObs
import com.moblin.android.various.model.fallbackStream
import com.moblin.android.various.model.isObsConnected
import com.moblin.android.various.model.isObsRemoteControlConfigured
import com.moblin.android.various.model.listObsScenes
import com.moblin.android.various.model.obsFixStream
import com.moblin.android.various.model.obsMuteAudio
import com.moblin.android.various.model.obsStartRecording
import com.moblin.android.various.model.obsStartStream
import com.moblin.android.various.model.obsStopRecording
import com.moblin.android.various.model.obsStopStream
import com.moblin.android.various.model.obsWebSocketEnabledUpdated
import com.moblin.android.various.model.setObsAudioDelay
import com.moblin.android.various.model.setObsMediaSourceSettings
import com.moblin.android.various.model.setObsScene
import com.moblin.android.various.model.startObsAudioVolume
import com.moblin.android.various.model.stopObsAudioVolume
import com.moblin.android.various.model.updateObsAudioDelay
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.view.settings.streams.stream.obsremotecontrol.StreamObsRemoteControlSettingsInnerView
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.ShortcutSectionView
import com.moblin.android.view.utils.TextButtonView
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.view.utils.ValueEditView

@Composable
private fun ObsStartStopButtonView(
    state: ObsOutputState,
    startAction: () -> Unit,
    stopAction: () -> Unit,
    startText: String,
    stopText: String,
) {
    var presentingStartConfirm by remember { mutableStateOf(false) }
    var presentingStopConfirm by remember { mutableStateOf(false) }
    val palette = formPalette()
    when (state) {
        ObsOutputState.stopped -> {
            Section {
                TextButtonView(title = startText, action = { presentingStartConfirm = true })
            }
            ConfirmationDialog(
                title = "",
                isPresented = presentingStartConfirm,
                onDismissRequest = { presentingStartConfirm = false },
            ) {
                Button(startText) {
                    startAction()
                }
            }
        }
        ObsOutputState.starting -> {
            Section {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.gray)
                        .padding(vertical = 11.dp),
                ) {
                    HCenter {
                        Text(localized("Starting..."), color = Color.White)
                    }
                }
            }
        }
        ObsOutputState.started -> {
            Section {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.accent)
                        .padding(vertical = 11.dp),
                ) {
                    CompositionLocalProvider(LocalTint provides Color.White) {
                        TextButtonView(title = stopText, action = { presentingStopConfirm = true })
                    }
                }
            }
            ConfirmationDialog(
                title = "",
                isPresented = presentingStopConfirm,
                onDismissRequest = { presentingStopConfirm = false },
            ) {
                Button(stopText) {
                    stopAction()
                }
            }
        }
        ObsOutputState.stopping -> {
            Section {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.gray)
                        .padding(vertical = 11.dp),
                ) {
                    HCenter {
                        Text(localized("Stopping..."), color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun ObsStartStopStreamingView(
    model: Model = LocalModel.current,
    obsQuickButton: QuickButtonObs,
) {
    val streamingState by obsQuickButton.streamingState.collectAsState<ObsOutputState>()
    ObsStartStopButtonView(
        state = streamingState,
        startAction = {
            model.obsStartStream()
        },
        stopAction = {
            model.obsStopStream()
        },
        startText = localized("Start streaming"),
        stopText = localized("Stop streaming"),
    )
}

@Composable
private fun ObsStartStopRecordingView(
    model: Model = LocalModel.current,
    obsQuickButton: QuickButtonObs,
) {
    val recordingState by obsQuickButton.recordingState.collectAsState<ObsOutputState>()
    ObsStartStopButtonView(
        state = recordingState,
        startAction = {
            model.obsStartRecording()
        },
        stopAction = {
            model.obsStopRecording()
        },
        startText = localized("Start recording"),
        stopText = localized("Stop recording"),
    )
}

@Composable
private fun ObsSettingsView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
) {
    Form(title = localized("OBS remote control")) {
        Toggle(
            title = localized("Enabled"),
            isOn = stream.obsWebSocketEnabled,
            onChange = { enabled ->
                stream.obsWebSocketEnabled = enabled
                if (stream.enabled) {
                    model.obsWebSocketEnabledUpdated()
                }
            },
        )
        StreamObsRemoteControlSettingsInnerView(stream = stream)
    }
}

@Composable
private fun ObsSnapshotView(
    obsQuickButton: QuickButtonObs,
) {
    Section(header = localized("Current scene snapshot")) {
        val screenshot = obsQuickButton.screenshot.collectAsState().value
        if (screenshot != null) {
            Image(
                bitmap = screenshot.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black),
                contentScale = ContentScale.Fit,
            )
        } else {
            Text(localized("No snapshot received yet."))
        }
    }
}

@Composable
private fun ObsScenesView(
    model: Model = LocalModel.current,
    obsQuickButton: QuickButtonObs,
) {
    Section(header = localized("Scenes")) {
        val scenes by obsQuickButton.scenes.collectAsState()
        val currentScenePicker by obsQuickButton.currentScenePicker.collectAsState()
        val currentScene by obsQuickButton.currentScene.collectAsState()
        Picker(
            title = "",
            selection = currentScenePicker,
            options = scenes,
            pickerStyle = PickerStyle.inline,
            onChange = { scene ->
                obsQuickButton.currentScenePicker.value = scene
                if (currentScene != scene) {
                    model.setObsScene(name = scene)
                }
            },
        )
    }
}

@Composable
private fun ObsSceneMediaSourceView(
    model: Model = LocalModel.current,
    source: ObsSceneMediaSource,
) {
    NavigationLink(destination = {
        Form(title = "${source.name} settings") {
            Section {
                TextEditNavigationView(
                    title = localized("Input"),
                    value = source.input,
                    onSubmit = {
                        model.setObsMediaSourceSettings(name = source.name, input = it.trim())
                    },
                    placeholder = "srt://1.2.3.4:4000",
                )
            }
        }
    }) {
        Text(source.name)
    }
}

@Composable
private fun ObsSceneMediaSourcesView(
    model: Model = LocalModel.current,
    obsQuickButton: QuickButtonObs,
) {
    Section(header = localized("Scene media sources")) {
        val sceneMediaSources by obsQuickButton.sceneMediaSources.collectAsState()
        sceneMediaSources.forEach { source ->
            ObsSceneMediaSourceView(model = model, source = source)
        }
    }
}

@Composable
private fun ObsSceneAudioInputsView(
    model: Model = LocalModel.current,
    obsQuickButton: QuickButtonObs,
) {
    val palette = formPalette()
    Section(header = localized("Scene audio inputs")) {
        val sceneInputs by obsQuickButton.sceneInputs.collectAsState()
        sceneInputs.forEach { input ->
            val muted = input.muted
            if (muted != null) {
                FormRow {
                    Text(input.name)
                    Spacer(modifier = Modifier.weight(1f))
                    SystemImage(
                        name = if (muted) "microphone.slash" else "microphone",
                        fontSize = 17.sp,
                        tint = if (muted) palette.red else Color.Unspecified,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            model.obsMuteAudio(inputName = input.name, muted = !muted)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ObsFixSourceView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
    obsQuickButton: QuickButtonObs,
) {
    val palette = formPalette()
    val obsSourceName = stream.obsSourceName
    val fixOngoing by obsQuickButton.fixOngoing.collectAsState()
    val footer = "Restarts the $obsSourceName source to hopefully fix audio and video issues."
    if (!fixOngoing) {
        Section(footer = footer) {
            TextButtonView(title = "Fix $obsSourceName source", action = {
                model.obsFixStream()
            })
        }
    } else {
        Section(footer = footer) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.gray)
                    .padding(vertical = 11.dp),
            ) {
                HCenter {
                    Text(localized("Fixing..."), color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ObsAudioSyncView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
    obsQuickButton: QuickButtonObs,
) {
    val obsSourceName = stream.obsSourceName
    val audioDelay by obsQuickButton.audioDelay.collectAsState()
    val submitAudioDelay: (String) -> String = { value ->
        val offsetDouble = (value.toDoubleOrNull() ?: 0.0)
            .coerceIn(
                obsMinimumAudioDelay.toDouble(),
                obsMaximumAudioDelay.toDouble(),
            )
        val offset = offsetDouble.toInt()
        model.setObsAudioDelay(offset = offset)
        offset.toString()
    }
    Section(header = "$obsSourceName source audio sync") {
        ValueEditView(
            title = localized("Delay"),
            number = audioDelay.toFloat(),
            value = audioDelay.toString(),
            minimum = obsMinimumAudioDelay.toFloat(),
            maximum = minOf(obsMaximumAudioDelay, 9999).toFloat(),
            onSubmit = submitAudioDelay,
            increment = 10f,
            unit = "ms",
        )
    }
}

@Composable
private fun ObsAudioLevelsView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
    obsQuickButton: QuickButtonObs,
) {
    val isLive by model.isLive.collectAsState()
    val obsSourceName = stream.obsSourceName
    val audioVolume by obsQuickButton.audioVolume.collectAsState()
    Section(header = "$obsSourceName source audio levels") {
        if (isLive) {
            if (audioVolume.isNotEmpty()) {
                Text(audioVolume)
            } else {
                Text(localized("No audio levels received yet."))
            }
        } else {
            Text(localized("Go live to see audio levels."))
        }
    }
}

@Composable
private fun ObsConnectedView(
    stream: SettingsStream,
    obsQuickButton: QuickButtonObs,
) {
    val obsSourceName = stream.obsSourceName
    val streamName = stream.name
    ObsStartStopStreamingView(obsQuickButton = obsQuickButton)
    ObsStartStopRecordingView(obsQuickButton = obsQuickButton)
    ObsSnapshotView(obsQuickButton = obsQuickButton)
    ObsScenesView(obsQuickButton = obsQuickButton)
    ObsSceneAudioInputsView(obsQuickButton = obsQuickButton)
    ObsSceneMediaSourcesView(obsQuickButton = obsQuickButton)
    if (obsSourceName.isNotEmpty()) {
        ObsFixSourceView(stream = stream, obsQuickButton = obsQuickButton)
        ObsAudioSyncView(stream = stream, obsQuickButton = obsQuickButton)
        ObsAudioLevelsView(stream = stream, obsQuickButton = obsQuickButton)
    } else {
        Section {
            Text(
                "Configure source name in Settings → Streams → $streamName → OBS remote control " +
                    "for Fix button and more.",
            )
        }
    }
}

@Composable
fun QuickButtonObsView(
    stream: SettingsStream,
    obsQuickButton: QuickButtonObs,
    model: Model = LocalModel.current,
) {
    DisposableEffect(Unit) {
        model.listObsScenes(updateAudioInputs = true)
        obsQuickButton.startObsSourceScreenshot()
        model.startObsAudioVolume()
        model.updateObsAudioDelay()
        onDispose {
            obsQuickButton.stopObsSourceScreenshot()
            model.stopObsAudioVolume()
        }
    }
    Form(title = localized("OBS remote control")) {
        if (!model.isObsRemoteControlConfigured()) {
        } else if (!model.isObsConnected()) {
            Section {
                Text(localized("Unable to connect the OBS server. Retrying every 5 seconds."))
            }
        } else {
            ObsConnectedView(stream = stream, obsQuickButton = obsQuickButton)
        }
        if (stream !== fallbackStream) {
            ShortcutSectionView {
                NavigationLink(destination = {
                    ObsSettingsView(model = model, stream = stream)
                }) {
                    Label(
                        localized("OBS remote control"),
                        systemImage = "dot.radiowaves.left.and.right",
                    )
                }
            }
        }
    }
}

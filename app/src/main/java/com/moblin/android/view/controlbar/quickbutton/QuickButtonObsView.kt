package com.moblin.android.view.controlbar.quickbutton

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.moblin.android.localized
import com.moblin.android.obs.ObsOutputState
import com.moblin.android.obs.obsMaximumAudioDelay
import com.moblin.android.obs.obsMinimumAudioDelay
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
import com.moblin.android.LocalModel

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
    when (state) {
        ObsOutputState.stopped -> {
            Column(modifier = Modifier.fillMaxWidth()) {
                TextButtonView(title = startText, action = { presentingStartConfirm = true })
                if (presentingStartConfirm) {
                    AlertDialog(
                        onDismissRequest = { presentingStartConfirm = false },
                        confirmButton = {
                            TextButton(onClick = {
                                presentingStartConfirm = false
                                startAction()
                            }) {
                                Text(startText)
                            }
                        },
                    )
                }
            }
        }
        ObsOutputState.starting -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Gray)
                    .padding(8.dp),
            ) {
                HCenter {
                    Text(text = localized("Starting..."), color = Color.White)
                }
            }
        }
        ObsOutputState.started -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Blue)
                    .padding(8.dp),
            ) {
                TextButtonView(title = stopText, action = { presentingStopConfirm = true })
                if (presentingStopConfirm) {
                    AlertDialog(
                        onDismissRequest = { presentingStopConfirm = false },
                        confirmButton = {
                            TextButton(onClick = {
                                presentingStopConfirm = false
                                stopAction()
                            }) {
                                Text(stopText)
                            }
                        },
                    )
                }
            }
        }
        ObsOutputState.stopping -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Gray)
                    .padding(8.dp),
            ) {
                HCenter {
                    Text(text = localized("Stopping..."), color = Color.White)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ObsSettingsView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
) {
    val obsWebSocketEnabled = stream.obsWebSocketEnabled
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        TopAppBar(title = { Text(localized("OBS remote control")) })
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(localized("Enabled"))
            Switch(
                checked = obsWebSocketEnabled,
                onCheckedChange = { enabled ->
                    stream.obsWebSocketEnabled = enabled
                    if (stream.enabled) {
                        model.obsWebSocketEnabledUpdated()
                    }
                },
            )
        }
        StreamObsRemoteControlSettingsInnerView(stream = stream)
    }
}

@Composable
private fun ObsSnapshotView(
    obsQuickButton: QuickButtonObs,
) {
    val screenshot = obsQuickButton.screenshot.collectAsState().value
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
    ) {
        Text(localized("Current scene snapshot"))
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
    val scenes by obsQuickButton.scenes.collectAsState()
    val currentScenePicker by obsQuickButton.currentScenePicker.collectAsState()
    val currentScene by obsQuickButton.currentScene.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
    ) {
        Text(localized("Scenes"))
        scenes.forEach { scene ->
            val selectScene = {
                if (currentScene != currentScenePicker) {
                    model.setObsScene(name = currentScenePicker)
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        obsQuickButton.currentScenePicker.value = scene
                        selectScene()
                    }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = currentScenePicker == scene,
                    onClick = null,
                )
                Text(scene)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ObsSceneMediaSourceView(
    model: Model = LocalModel.current,
    source: ObsSceneMediaSource,
) {
    var showSettings by remember { mutableStateOf(false) }
    TextButtonView(title = source.name, action = { showSettings = true })
    if (showSettings) {
        Dialog(onDismissRequest = { showSettings = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
            ) {
                TopAppBar(title = { Text("${source.name} settings") })
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
    }
}

@Composable
private fun ObsSceneMediaSourcesView(
    model: Model = LocalModel.current,
    obsQuickButton: QuickButtonObs,
) {
    val sceneMediaSources by obsQuickButton.sceneMediaSources.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
    ) {
        Text(localized("Scene media sources"))
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
    val sceneInputs by obsQuickButton.sceneInputs.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
    ) {
        Text(localized("Scene audio inputs"))
        sceneInputs.forEach { input ->
            val muted = input.muted
            if (muted != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(input.name)
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = {
                        model.obsMuteAudio(inputName = input.name, muted = !muted)
                    }) {
                        if (muted) {
                            Icon(
                                imageVector = Icons.Filled.MicOff,
                                contentDescription = null,
                                tint = Color.Red,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Mic,
                                contentDescription = null,
                            )
                        }
                    }
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
    val obsSourceName = stream.obsSourceName
    val fixOngoing by obsQuickButton.fixOngoing.collectAsState()
    if (!fixOngoing) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
        ) {
            TextButtonView(title = "Fix $obsSourceName source", action = {
                model.obsFixStream()
            })
            Text(
                "Restarts the $obsSourceName source to hopefully fix audio and video issues.",
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Gray)
                .padding(8.dp),
        ) {
            HCenter {
                Text(text = localized("Fixing..."), color = Color.White)
            }
            Text(
                text = "Restarts the $obsSourceName source to hopefully fix audio and video issues.",
                color = Color.White,
            )
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
    ) {
        Text("$obsSourceName source audio sync")
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
    ) {
        Text("$obsSourceName source audio levels")
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
        Text(
            "Configure source name in Settings → Streams → $streamName → OBS remote control " +
                "for Fix button and more.",
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickButtonObsView(
    stream: SettingsStream,
    obsQuickButton: QuickButtonObs,
    model: Model = LocalModel.current,
) {
    var showObsSettings by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        model.listObsScenes(updateAudioInputs = true)
        obsQuickButton.startObsSourceScreenshot()
        model.startObsAudioVolume()
        model.updateObsAudioDelay()
    }
    DisposableEffect(Unit) {
        onDispose {
            obsQuickButton.stopObsSourceScreenshot()
            model.stopObsAudioVolume()
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        TopAppBar(title = { Text(localized("OBS remote control")) })
        if (!model.isObsRemoteControlConfigured()) {
        } else if (!model.isObsConnected()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
            ) {
                Text(localized("Unable to connect the OBS server. Retrying every 5 seconds."))
            }
        } else {
            ObsConnectedView(stream = stream, obsQuickButton = obsQuickButton)
        }
        if (stream !== fallbackStream) {
            ShortcutSectionView {
                TextButtonView(
                    title = localized("OBS remote control"),
                    action = { showObsSettings = true },
                )
            }
        }
    }
    if (showObsSettings) {
        Dialog(onDismissRequest = { showObsSettings = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
            ) {
                ObsSettingsView(stream = stream)
            }
        }
    }
}

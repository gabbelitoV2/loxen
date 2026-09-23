package com.moblin.android.view.settings.streams.stream.recording

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.common.various.bitrateFromMbps
import com.moblin.android.common.various.bitrateToMbps
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormButton
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Sheet
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.various.settings.SettingsStreamCodec
import com.moblin.android.various.settings.SettingsStreamRecording
import com.moblin.android.various.settings.SettingsStreamResolution
import com.moblin.android.various.utils.makeRecordingPath
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.TextEditView
import com.moblin.android.view.utils.TextItemLocalizedView
import com.moblin.android.various.model.reloadStreamIfEnabled
import com.moblin.android.various.model.setCleanRecordings

@Composable
private fun PickerView(model: Model = LocalModel.current) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            model.onDocumentPickerUrl?.invoke(uri.toString())
        }
    }
    LaunchedEffect(Unit) {
        launcher.launch(null)
    }
}

private fun getRecordingPath(recordingPath: ByteArray): String {
    return makeRecordingPath(recordingPath = recordingPath) ?: localized("Disk not connected?")
}

private fun onUrl(url: String, recording: SettingsStreamRecording) {
    recording.recordingPath = url.toByteArray()
}

@Composable
private fun RecordingPathView(
    recording: SettingsStreamRecording,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val recordingPath = recording.recordingPath
    NavigationLink(
        destination = { RecordingPathFormView(recording = recording) },
    ) {
        Text(localized("Recording path"))
        Spacer(Modifier.weight(1f))
        if (recordingPath != null) {
            GrayTextView(text = getRecordingPath(recordingPath = recordingPath))
        }
    }
}

@Composable
fun RecordingPathFormView(recording: SettingsStreamRecording, model: Model = LocalModel.current) {
    var showPicker by remember { mutableStateOf(false) }
    val recordingPath = recording.recordingPath
    Form(title = "Recording path") {
        Section(header = "Folder") {
            FormRow(
                onClick = {
                    showPicker = true
                    model.onDocumentPickerUrl = { url ->
                        onUrl(url = url, recording = recording)
                    }
                },
            ) {
                HCenter {
                    if (recordingPath != null) {
                        Text(
                            getRecordingPath(recordingPath = recordingPath),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    } else {
                        Text(localized("Select"))
                    }
                }
            }
        }
        Section {
            FormButton(title = "Reset", destructive = true, centered = true) {
                recording.recordingPath = null
            }
        }
    }
    if (showPicker) {
        Sheet(onDismissRequest = { showPicker = false }) {
            PickerView(model = model)
        }
    }
}

@Composable
private fun ResolutionSettingsView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
    recording: SettingsStreamRecording,
    enabled: Boolean = true,
) {
    Picker(
        title = "Resolution",
        selection = recording.resolution,
        options = SettingsStreamResolution.entries,
        enabled = enabled,
        text = { it.shortString() },
        onChange = { value ->
            recording.resolution = value
            if (recording.overrideStream) {
                model.reloadStreamIfEnabled(stream)
            }
        },
    )
}

private fun submitVideoBitrateChange(recording: SettingsStreamRecording, value: String) {
    val bitrate = value.toFloatOrNull() ?: return
    recording.videoBitrate = bitrateFromMbps(bitrate = bitrate.coerceIn(0f, 50f)).toInt()
}

private fun submitMaxKeyFrameInterval(recording: SettingsStreamRecording, value: String) {
    val interval = value.toIntOrNull() ?: return
    if (interval < 0 || interval > 10) {
        return
    }
    recording.maxKeyFrameInterval = interval
}

@Composable
fun StreamRecordingSettingsView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
    recording: SettingsStreamRecording,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val isLive by model.isLive.collectAsState()
    val isRecording by model.isRecording.collectAsState()
    val streamEnabled = stream.enabled
    val overrideEnabled = !(streamEnabled && (isLive || isRecording))
    val recordingEnabled = !(streamEnabled && isRecording)

    Form(title = "Recording") {
        Section(
            footerContent = {
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        localized(
                            "Resolution and FPS are same as for live stream if not overridden."
                        )
                    )
                    Text(" ")
                    Text(
                        localized(
                            "The overall energy consumption will be higher and the live stream " +
                                "image quality will be worse when the override is enabled, " +
                                "regardless of if you are recording or not."
                        )
                    )
                }
            },
        ) {
            Toggle(
                title = "Override",
                isOn = recording.overrideStream,
                enabled = overrideEnabled,
            ) { value ->
                recording.overrideStream = value
                model.reloadStreamIfEnabled(stream)
            }
            ResolutionSettingsView(
                model = model,
                stream = stream,
                recording = recording,
                enabled = overrideEnabled,
            )
        }
        Section {
            Picker(
                title = "Video codec",
                selection = recording.videoCodec,
                options = SettingsStreamCodec.entries,
                enabled = recordingEnabled,
                text = { it.rawValue },
                onChange = { recording.videoCodec = it },
            )
            NavigationLink(
                destination = {
                    StreamRecordingVideoBitrateView(recording = recording)
                },
                enabled = recordingEnabled,
            ) {
                TextItemLocalizedView(
                    name = "Video bitrate",
                    value = recording.videoBitrateString(),
                )
            }
            NavigationLink(
                destination = {
                    StreamRecordingKeyFrameIntervalView(recording = recording)
                },
                enabled = recordingEnabled,
            ) {
                TextItemLocalizedView(
                    name = "Key frame interval",
                    value = recording.maxKeyFrameIntervalString(),
                )
            }
            NavigationLink(
                destination = {
                    StreamRecordingAudioBitrateView(stream = stream, recording = recording)
                },
                enabled = recordingEnabled,
            ) {
                TextItemLocalizedView(
                    name = "Audio bitrate",
                    value = recording.audioBitrateString(),
                )
            }
        }
        RecordingPathView(recording = recording, onNavigate = onNavigate)
        Section(footer = "Do not show widgets in recordings.") {
            Toggle(
                title = "Clean recordings",
                isOn = recording.cleanRecordings,
            ) { value ->
                recording.cleanRecordings = value
                model.setCleanRecordings()
            }
        }
        Section {
            Toggle(
                title = "Auto start recording when going live",
                isOn = recording.autoStartRecording,
            ) { value ->
                recording.autoStartRecording = value
            }
            Toggle(
                title = "Auto stop recording when ending stream",
                isOn = recording.autoStopRecording,
            ) { value ->
                recording.autoStopRecording = value
            }
        }
    }
}

@Composable
fun StreamRecordingVideoBitrateView(recording: SettingsStreamRecording) {
    val videoBitrate = recording.videoBitrate
    TextEditView(
        title = localized("Video bitrate"),
        value = bitrateToMbps(bitrate = videoBitrate.toUInt()).toString(),
        footers = listOf(localized("Up to 50 Mbps. Set to 0 for automatic.")),
        keyboardType = KeyboardType.Number,
        onSubmit = { value ->
            submitVideoBitrateChange(recording = recording, value = value)
        },
    )
}

@Composable
fun StreamRecordingKeyFrameIntervalView(recording: SettingsStreamRecording) {
    val maxKeyFrameInterval = recording.maxKeyFrameInterval
    TextEditView(
        title = localized("Key frame interval"),
        value = maxKeyFrameInterval.toString(),
        footers = listOf(
            localized("Maximum key frame interval in seconds. Set to 0 for automatic."),
        ),
        keyboardType = KeyboardType.Number,
        onSubmit = { value ->
            submitMaxKeyFrameInterval(recording = recording, value = value)
        },
    )
}

@Composable
fun StreamRecordingAudioBitrateView(stream: SettingsStream, recording: SettingsStreamRecording) {
    val audioBitrate = recording.audioBitrate
    StreamRecordingAudioSettingsView(
        stream = stream,
        initialBitrate = (audioBitrate / 1000).toFloat(),
    )
}

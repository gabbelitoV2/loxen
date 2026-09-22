package com.moblin.android.view.settings.streams.stream.recording

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import com.moblin.android.common.various.bitrateFromMbps
import com.moblin.android.common.various.bitrateToMbps
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.various.settings.SettingsStreamCodec
import com.moblin.android.various.settings.SettingsStreamRecording
import com.moblin.android.various.settings.SettingsStreamResolution
import com.moblin.android.various.utils.makeRecordingPath
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.TextButtonView
import com.moblin.android.view.utils.TextEditView
import com.moblin.android.view.utils.TextItemLocalizedView
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

@Composable
private fun PickerView(model: Model = LocalModel.current) {
    Unit
}

private fun getRecordingPath(recordingPath: ByteArray): String {
    return makeRecordingPath(recordingPath = recordingPath) ?: localized("Disk not connected?")
}

private fun onUrl(url: String, recording: SettingsStreamRecording) {
    Unit
}

@Composable
private fun RecordingPathView(
    recording: SettingsStreamRecording,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val recordingPath = recording.recordingPath
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("recordingPath") }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(localized("Recording path"))
        Spacer(Modifier.weight(1f))
        if (recordingPath != null) {
            GrayTextView(text = getRecordingPath(recordingPath = recordingPath))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingPathFormView(recording: SettingsStreamRecording, model: Model = LocalModel.current) {
    var showPicker by remember { mutableStateOf(false) }
    val recordingPath = recording.recordingPath
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            localized("Folder"),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(16.dp),
        )
        Button(
            onClick = {
                showPicker = true
                model.onDocumentPickerUrl = { url -> onUrl(url = url, recording = recording) }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
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
        TextButtonView(title = "Reset") {
            recording.recordingPath = null
        }
    }
    if (showPicker) {
        ModalBottomSheet(onDismissRequest = { showPicker = false }) {
            PickerView(model = model)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResolutionSettingsView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
    recording: SettingsStreamRecording,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    val resolution = recording.resolution
    LaunchedEffect(resolution) {
        if (recording.overrideStream) {
            Unit
        }
    }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = resolution.shortString(),
            onValueChange = {},
            readOnly = true,
            label = { Text(localized("Resolution")) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            enabled = enabled,
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SettingsStreamResolution.entries.forEach { value ->
                DropdownMenuItem(
                    text = { Text(value.shortString()) },
                    onClick = {
                        recording.resolution = value
                        expanded = false
                    },
                )
            }
        }
    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamRecordingSettingsView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
    recording: SettingsStreamRecording,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val streamEnabled = stream.enabled
    val isLive = model.isLive.collectAsState().value
    val isRecording = model.isRecording.collectAsState().value
    val overrideStream = recording.overrideStream
    val videoCodec = recording.videoCodec
    val cleanRecordings = recording.cleanRecordings
    val autoStartRecording = recording.autoStartRecording
    val autoStopRecording = recording.autoStopRecording
    var videoCodecExpanded by remember { mutableStateOf(false) }
    val overrideEnabled = !(streamEnabled && (isLive || isRecording))
    val recordingEnabled = !(streamEnabled && isRecording)

    LaunchedEffect(overrideStream) {
        if (overrideStream) {
            Unit
        }
    }
    LaunchedEffect(cleanRecordings) {
        Unit
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(localized("Override"))
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = overrideStream,
                        onCheckedChange = { recording.overrideStream = it },
                        enabled = overrideEnabled,
                    )
                }
                ResolutionSettingsView(
                    model = model,
                    stream = stream,
                    recording = recording,
                    enabled = overrideEnabled,
                )
            }
        }
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(
                    localized("Resolution and FPS are same as for live stream if not overridden."),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text("", style = MaterialTheme.typography.bodySmall)
                Text(
                    localized(
                        "The overall energy consumption will be higher and the live stream image quality " +
                            "will be worse when the override is enabled, regardless of if you are " +
                            "recording or not."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        item {
            ExposedDropdownMenuBox(
                expanded = videoCodecExpanded,
                onExpandedChange = { videoCodecExpanded = it },
            ) {
                OutlinedTextField(
                    value = videoCodec.rawValue,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(localized("Video codec")) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = videoCodecExpanded)
                    },
                    enabled = recordingEnabled,
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                        .padding(16.dp),
                )
                ExposedDropdownMenu(
                    expanded = videoCodecExpanded,
                    onDismissRequest = { videoCodecExpanded = false },
                ) {
                    SettingsStreamCodec.entries.forEach { codec ->
                        DropdownMenuItem(
                            text = { Text(codec.rawValue) },
                            onClick = {
                                recording.videoCodec = codec
                                videoCodecExpanded = false
                            },
                        )
                    }
                }
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = recordingEnabled) { onNavigate("videoBitrate") }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextItemLocalizedView(
                    name = "Video bitrate",
                    value = recording.videoBitrateString(),
                )
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = recordingEnabled) { onNavigate("keyFrameInterval") }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextItemLocalizedView(
                    name = "Key frame interval",
                    value = recording.maxKeyFrameIntervalString(),
                )
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = recordingEnabled) { onNavigate("audioBitrate") }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextItemLocalizedView(
                    name = "Audio bitrate",
                    value = recording.audioBitrateString(),
                )
            }
        }
        item {
            RecordingPathView(recording = recording, onNavigate = onNavigate)
        }
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(localized("Clean recordings"))
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = cleanRecordings,
                        onCheckedChange = { recording.cleanRecordings = it },
                    )
                }
                Text(
                    localized("Do not show widgets in recordings."),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(localized("Auto start recording when going live"))
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = autoStartRecording,
                        onCheckedChange = { recording.autoStartRecording = it },
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(localized("Auto stop recording when ending stream"))
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = autoStopRecording,
                        onCheckedChange = { recording.autoStopRecording = it },
                    )
                }
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

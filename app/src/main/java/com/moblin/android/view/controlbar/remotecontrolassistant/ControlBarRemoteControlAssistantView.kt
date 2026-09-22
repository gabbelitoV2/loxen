package com.moblin.android.view.controlbar.remotecontrolassistant

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.backgroundColor
import com.moblin.android.common.various.clippingThresholdDb
import com.moblin.android.common.various.formatAudioLevelChannels
import com.moblin.android.common.various.formatBytesPerSecond
import com.moblin.android.common.various.redThresholdDb
import com.moblin.android.common.various.smallFont
import com.moblin.android.common.various.yellowThresholdDb
import com.moblin.android.common.various.zeroThresholdDb
import com.moblin.android.localized
import com.moblin.android.remotecontrol.RemoteControlFilter
import com.moblin.android.remotecontrol.RemoteControlMacro
import com.moblin.android.remotecontrol.RemoteControlSettingsSrt
import com.moblin.android.remotecontrol.RemoteControlSettingsSrtConnectionPriority
import com.moblin.android.remotecontrol.RemoteControlStatusGeneral
import com.moblin.android.remotecontrol.RemoteControlStatusItem
import com.moblin.android.view.CloseButtonView
import com.moblin.android.view.TextButtonView
import com.moblin.android.view.settings.debug.DebugLogSettingsView
import com.moblin.android.view.settings.remotecontrol.RemoteControlStreamersView
import com.moblin.android.view.settings.streams.stream.srt.clampConnectionPriority
import com.moblin.android.view.settings.streams.stream.srt.maximumSrtConnectionPriority
import com.moblin.android.view.settings.streams.stream.srt.minimumSrtConnectionPriority
import com.moblin.android.view.utils.HCenter
import com.moblin.android.various.model.LogEntry
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.Orientation
import com.moblin.android.various.model.QuickButtonType
import com.moblin.android.various.model.RemoteControl
import com.moblin.android.various.model.RemoteControlAssistantPreviewUser
import com.moblin.android.various.settings.SettingsRemoteControl
import java.util.UUID
import kotlin.math.min
import kotlin.math.roundToInt
import com.moblin.android.LocalModel

@Composable
private fun StatusItemView(icon: String, status: RemoteControlStatusItem?) {
    if (status != null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = statusIcon(icon),
                contentDescription = null,
                tint = if (status.ok) LocalContentColor.current else Color.Red,
                modifier = Modifier.width(20.dp),
            )
            Text(text = status.message, fontSize = smallFont)
        }
    }
}

@Composable
private fun RemoteControlSrtConnectionPriorityView(
    model: Model = LocalModel.current,
    priority: RemoteControlSettingsSrtConnectionPriority,
    enabled: Boolean,
    prio: Float,
) {
    var enabledState by remember { mutableStateOf(enabled) }
    var prioState by remember { mutableFloatStateOf(prio) }

    fun makeName(): String {
        val name = model.database.networkInterfaceNames
            .firstOrNull { interface -> interface.interfaceName == priority.name }
            ?.name
        return if (name != null && name.isNotEmpty()) {
            name
        } else {
            priority.name
        }
    }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(makeName())
            Spacer(Modifier.weight(1f))
            Switch(
                checked = enabledState,
                onCheckedChange = { value ->
                    val updated = priority.copy(enabled = value)
                    enabledState = value
                    model.remoteControlAssistantSetSrtConnectionPriority(priority = updated)
                },
            )
        }
        Slider(
            value = prioState,
            onValueChange = { prioState = it },
            valueRange = minimumSrtConnectionPriority.toFloat()..maximumSrtConnectionPriority.toFloat(),
            steps = (maximumSrtConnectionPriority - minimumSrtConnectionPriority - 1).coerceAtLeast(0),
            onValueChangeFinished = {
                val updated = priority.copy(
                    priority = clampConnectionPriority(value = prioState.roundToInt()),
                )
                model.remoteControlAssistantSetSrtConnectionPriority(priority = updated)
            },
        )
    }
}

@Composable
private fun RemoteControlSrtConnectionPrioritiesView(
    model: Model = LocalModel.current,
    srt: RemoteControlSettingsSrt,
    enabled: Boolean,
) {
    var enabledState by remember { mutableStateOf(enabled) }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Enabled")
            Spacer(Modifier.weight(1f))
            Switch(
                checked = enabledState,
                onCheckedChange = { value ->
                    enabledState = value
                    model.remoteControlAssistantSetSrtConnectionPriorityEnabled(enabled = value)
                },
            )
        }
        srt.connectionPriorities.forEach { priority ->
            RemoteControlSrtConnectionPriorityView(
                model = model,
                priority = priority,
                enabled = priority.enabled,
                prio = priority.priority.toFloat(),
            )
        }
    }
}

@Composable
private fun RemoteControlAudioLevelView(level: Float, channels: Int?) {
    val barsPerDb = 0.3f
    val maxBars = "||||||||||||||||||||"

    fun bars(count: Float): String {
        val barCount = count.roundToInt().coerceIn(0, maxBars.length)
        return maxBars.take(barCount)
    }

    fun isClipping(): Boolean {
        return level > clippingThresholdDb
    }

    fun clippingText(): String {
        val db = -zeroThresholdDb
        return bars(count = db * barsPerDb)
    }

    fun redText(): String {
        if (level <= redThresholdDb) {
            return ""
        }
        val db = level - redThresholdDb
        return bars(count = db * barsPerDb)
    }

    fun yellowText(): String {
        if (level <= yellowThresholdDb) {
            return ""
        }
        val db = min(level - yellowThresholdDb, redThresholdDb - yellowThresholdDb)
        return bars(count = db * barsPerDb)
    }

    fun greenText(): String {
        if (level <= zeroThresholdDb) {
            return ""
        }
        val db = min(level - zeroThresholdDb, yellowThresholdDb - zeroThresholdDb)
        return bars(count = db * barsPerDb)
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.width(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
            if (level.isNaN()) {
                if (channels == null) {
                    Text("Muted", fontSize = smallFont)
                } else {
                    Text("Muted,", fontSize = smallFont)
                }
            } else {
                Row(modifier = Modifier.padding(bottom = 3.dp)) {
                    if (isClipping()) {
                        Text(
                            clippingText(),
                            color = Color.Red,
                            fontSize = smallFont,
                            fontWeight = FontWeight.Bold,
                        )
                    } else {
                        Text(
                            redText(),
                            color = Color.Red,
                            fontSize = smallFont,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            yellowText(),
                            color = Color.Yellow,
                            fontSize = smallFont,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            greenText(),
                            color = Color.Green,
                            fontSize = smallFont,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            if (channels != null) {
                Text(formatAudioLevelChannels(channels = channels), fontSize = smallFont)
            }
        }
    }
}

private fun batteryStatus(status: RemoteControlStatusGeneral): RemoteControlStatusItem? {
    val charging = status.batteryCharging ?: return null
    val level = status.batteryLevel ?: return null
    var message = "$level%"
    if (charging) {
        message += ", Charging"
    } else {
        message += ", Not charging"
    }
    return RemoteControlStatusItem(message = message)
}

private fun flameStatus(status: RemoteControlStatusGeneral): RemoteControlStatusItem? {
    val flame = status.flame ?: return null
    return RemoteControlStatusItem(message = flame.rawValue)
}

private fun ssidStatus(status: RemoteControlStatusGeneral): RemoteControlStatusItem? {
    val wiFiSsid = status.wiFiSsid ?: return null
    return RemoteControlStatusItem(message = wiFiSsid)
}

@Composable
private fun ControlBarRemoteControlAssistantStatusView(
    model: Model = LocalModel.current,
    remoteControl: RemoteControl,
    title: String = "",
) {
    val presentingPreview = remoteControl.presentingPreview.collectAsState().value
    val preview = remoteControl.preview.collectAsState().value
    val general = remoteControl.general.collectAsState().value
    val topLeft = remoteControl.topLeft.collectAsState().value
    val topRight = remoteControl.topRight.collectAsState().value

    Column {
        if (title.isNotEmpty()) {
            Text(title, style = MaterialTheme.typography.titleSmall)
        }
        if (presentingPreview) {
            if (preview != null) {
                Image(
                    bitmap = preview.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 3.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    remoteControl.presentingPreviewFullScreen = true
                                },
                                onTap = {
                                    model.remoteControlAssistantStopPreview(
                                        user = RemoteControlAssistantPreviewUser.Panel,
                                    )
                                    remoteControl.presentingPreview = false
                                },
                            )
                        },
                    contentScale = ContentScale.Fit,
                )
            } else {
                Text("No preview received yet.")
            }
        } else {
            TextButtonView("Show") {
                model.remoteControlAssistantStartPreview(
                    user = RemoteControlAssistantPreviewUser.Panel,
                )
                remoteControl.presentingPreview = true
            }
        }
        if (presentingPreview) {
            Text("Tap the preview to hide it. Double tap to toggle full screen.")
        }
        Text("General", style = MaterialTheme.typography.titleSmall)
        if (general != null) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                StatusItemView(
                    icon = "battery.0",
                    status = batteryStatus(status = general),
                )
                StatusItemView(icon = "flame", status = flameStatus(status = general))
                StatusItemView(icon = "wifi", status = ssidStatus(status = general))
            }
        } else {
            Text("No status received yet.")
        }
        Text("Top left", style = MaterialTheme.typography.titleSmall)
        if (topLeft != null) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                StatusItemView(
                    icon = "dot.radiowaves.left.and.right",
                    status = topLeft.stream,
                )
                StatusItemView(icon = "camera", status = topLeft.camera)
                StatusItemView(icon = "music.mic", status = topLeft.mic)
                StatusItemView(icon = "magnifyingglass", status = topLeft.zoom)
                StatusItemView(icon = "xserve", status = topLeft.obs)
                StatusItemView(icon = "megaphone", status = topLeft.events)
                StatusItemView(icon = "message", status = topLeft.chat)
                StatusItemView(icon = "eye", status = topLeft.viewers)
            }
        } else {
            Text("No status received yet.")
        }
        Text("Top right", style = MaterialTheme.typography.titleSmall)
        if (topRight != null) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                val audioInfo = topRight.audioInfo
                if (audioInfo != null) {
                    RemoteControlAudioLevelView(
                        level = audioInfo.audioLevel.toFloat(),
                        channels = audioInfo.numberOfAudioChannels,
                    )
                } else {
                    StatusItemView(icon = "waveform", status = topRight.audioLevel)
                }
                StatusItemView(icon = "cpu", status = topRight.systemMonitor)
                StatusItemView(icon = "server.rack", status = topRight.rtmpServer)
                StatusItemView(icon = "app.connected.to.app.below.fill", status = topRight.moblink)
                StatusItemView(icon = "appletvremote.gen1", status = topRight.remoteControl)
                StatusItemView(icon = "appletvremote.gen1", status = topRight.djiDevices)
                StatusItemView(icon = "gamecontroller", status = topRight.gameController)
                StatusItemView(icon = "speedometer", status = topRight.bitrate)
                StatusItemView(icon = "deskclock", status = topRight.uptime)
                StatusItemView(icon = "location", status = topRight.location)
                StatusItemView(icon = "phone.connection", status = topRight.srtla)
                StatusItemView(icon = "phone.connection", status = topRight.srtlaRtts)
                StatusItemView(icon = "record.circle", status = topRight.recording)
                StatusItemView(icon = "play", status = topRight.replay)
                StatusItemView(icon = "globe", status = topRight.browserWidgets)
            }
        } else {
            Text("No status received yet.")
        }
    }
}

@Composable
private fun LiveView(model: Model = LocalModel.current, remoteControl: RemoteControl) {
    val streaming = remoteControl.streaming.collectAsState().value
    var checked by remember(streaming) { mutableStateOf(streaming) }
    var presentingConfirm by remember { mutableStateOf(false) }
    var pendingStreaming by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Live")
        Spacer(Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = { value ->
                if (value != model.remoteControlAssistantStreamerState.streaming) {
                    pendingStreaming = value
                    presentingConfirm = true
                }
            },
        )
    }
    if (presentingConfirm) {
        AlertDialog(
            onDismissRequest = { presentingConfirm = false },
            confirmButton = {
                TextButton(onClick = {
                    model.remoteControlAssistantSetLive(on = pendingStreaming)
                    remoteControl.streaming = pendingStreaming
                    checked = pendingStreaming
                    presentingConfirm = false
                }) {
                    Text(if (pendingStreaming) "Go Live" else "End")
                }
            },
        )
    }
}

@Composable
private fun RecordingView(model: Model = LocalModel.current, remoteControl: RemoteControl) {
    val recording = remoteControl.recording.collectAsState().value
    var checked by remember(recording) { mutableStateOf(recording) }
    var presentingConfirm by remember { mutableStateOf(false) }
    var pendingRecording by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Recording")
        Spacer(Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = { value ->
                if (value != model.remoteControlAssistantStreamerState.recording) {
                    pendingRecording = value
                    presentingConfirm = true
                }
            },
        )
    }
    if (presentingConfirm) {
        AlertDialog(
            onDismissRequest = { presentingConfirm = false },
            confirmButton = {
                TextButton(onClick = {
                    model.remoteControlAssistantSetRecord(on = pendingRecording)
                    remoteControl.recording = pendingRecording
                    checked = pendingRecording
                    presentingConfirm = false
                }) {
                    Text(if (pendingRecording) "Start recording" else "Stop recording")
                }
            },
        )
    }
}

@Composable
private fun MutedView(model: Model = LocalModel.current, remoteControl: RemoteControl) {
    val mutedFlow = remoteControl.muted.collectAsState().value
    var muted by remember(mutedFlow) { mutableStateOf(mutedFlow) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Muted")
        Spacer(Modifier.weight(1f))
        Switch(checked = muted, onCheckedChange = { muted = it })
    }
    LaunchedEffect(muted) {
        if (muted != model.remoteControlAssistantStreamerState.muted) {
            model.remoteControlAssistantSetMute(on = muted)
        }
    }
}

@Composable
private fun PreviewStreamView(model: Model = LocalModel.current, remoteControl: RemoteControl) {
    val previewStreamFlow = remoteControl.previewStream.collectAsState().value
    var previewStream by remember(previewStreamFlow) { mutableStateOf(previewStreamFlow) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Preview stream")
        Spacer(Modifier.weight(1f))
        Switch(checked = previewStream, onCheckedChange = { previewStream = it })
    }
    LaunchedEffect(previewStream) {
        if (previewStream != model.remoteControlAssistantStreamerState.previewStream) {
            model.remoteControlAssistantSetPreviewStream(on = previewStream)
        }
    }
}

@Composable
private fun StealthModeControlView(model: Model = LocalModel.current, remoteControl: RemoteControl) {
    val stealthModeFlow = remoteControl.stealthMode.collectAsState().value
    var stealthMode by remember(stealthModeFlow) { mutableStateOf(stealthModeFlow) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Stealth mode")
        Spacer(Modifier.weight(1f))
        Switch(checked = stealthMode, onCheckedChange = { stealthMode = it })
    }
    LaunchedEffect(stealthMode) {
        if (stealthMode != model.remoteControlAssistantStreamerState.stealthMode) {
            model.remoteControlAssistantSetStealthMode(on = stealthMode)
        }
    }
}

@Composable
private fun ZoomView(model: Model = LocalModel.current, remoteControl: RemoteControl) {
    val zoomFlow = remoteControl.zoom.collectAsState().value
    val zoomPreset = remoteControl.zoomPreset.collectAsState().value
    val zoomPresets = remoteControl.zoomPresets.collectAsState().value
    var zoom by remember(zoomFlow) { mutableStateOf(zoomFlow) }

    fun submitZoom(value: String) {
        val x = value.toFloatOrNull()
        if (x == null || !x.isFinite()) {
            val currentZoom = model.remoteControlAssistantStreamerState.zoom
            if (currentZoom != null) {
                zoom = currentZoom.toString()
            }
            return
        }
        model.remoteControlAssistantSetZoom(x = x)
    }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Zoom")
            Spacer(Modifier.weight(1f))
            OutlinedTextField(
                value = zoom,
                onValueChange = { zoom = it },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                keyboardActions = KeyboardActions(onDone = {
                    val currentZoom = model.remoteControlAssistantStreamerState.zoom
                    if (currentZoom != null && zoom != currentZoom.toString()) {
                        submitZoom(value = zoom)
                    }
                }),
                modifier = Modifier.width(120.dp),
            )
        }
        RemoteControlPicker(
            label = "",
            options = zoomPresets.map { it.name to it.id },
            selected = zoomPreset,
            onSelect = { id ->
                if (id != null && id != model.remoteControlAssistantStreamerState.zoomPreset) {
                    model.remoteControlAssistantSetZoomPreset(id = id)
                }
            },
        )
    }
}

@Composable
private fun ScenePickerView(model: Model = LocalModel.current, remoteControl: RemoteControl) {
    val scene = remoteControl.scene.collectAsState().value
    val settings = remoteControl.settings.collectAsState().value
    val scenes = settings?.scenes ?: emptyList()

    RemoteControlPicker(
        label = "Scene",
        options = scenes.map { it.name to it.id },
        selected = scene,
        onSelect = { id ->
            if (id != null && id != model.remoteControlAssistantStreamerState.scene) {
                model.remoteControlAssistantSetScene(id = id)
            }
        },
    )
}

@Composable
private fun AutoSceneSwitcherPickerView(model: Model = LocalModel.current, remoteControl: RemoteControl) {
    val autoSceneSwitcher = remoteControl.autoSceneSwitcher.collectAsState().value
    val settings = remoteControl.settings.collectAsState().value
    val autoSceneSwitchers = settings?.autoSceneSwitchers ?: emptyList()
    val options = listOf<Pair<String, UUID?>>("-- None --" to null) +
        autoSceneSwitchers.map { it.name to it.id }

    RemoteControlPicker(
        label = "Auto scene switcher",
        options = options,
        selected = autoSceneSwitcher,
        onSelect = { id ->
            if (id != model.remoteControlAssistantStreamerState.autoSceneSwitcher?.id) {
                model.remoteControlAssistantSetAutoSceneSwitcher(id = id)
            }
        },
    )
}

@Composable
private fun MicView(model: Model = LocalModel.current, remoteControl: RemoteControl) {
    val mic = remoteControl.mic.collectAsState().value
    val settings = remoteControl.settings.collectAsState().value
    val mics = settings?.mics ?: emptyList()

    RemoteControlPicker(
        label = "Mic",
        options = mics.map { it.name to it.id },
        selected = mic,
        onSelect = { id ->
            if (id != null && id != model.remoteControlAssistantStreamerState.mic) {
                model.remoteControlAssistantSetMic(id = id)
            }
        },
    )
}

@Composable
private fun BitrateView(model: Model = LocalModel.current, remoteControl: RemoteControl) {
    val bitrate = remoteControl.bitrate.collectAsState().value
    val settings = remoteControl.settings.collectAsState().value
    val bitratePresets = settings?.bitratePresets ?: emptyList()

    RemoteControlPicker(
        label = "Bitrate",
        options = bitratePresets.map { preset ->
            val name = if (preset.bitrate > 0) {
                formatBytesPerSecond(speed = preset.bitrate.toLong())
            } else {
                "Unknown"
            }
            name to preset.id
        },
        selected = bitrate,
        onSelect = { id ->
            if (id != null && id != model.remoteControlAssistantStreamerState.bitrate) {
                model.remoteControlAssistantSetBitratePreset(id = id)
            }
        },
    )
}

@Composable
private fun GimbalPresetView(model: Model = LocalModel.current, remoteControl: RemoteControl) {
    val gimbalPresets = remoteControl.gimbalPresets.collectAsState().value

    Column {
        Text("Gimbal presets", style = MaterialTheme.typography.titleSmall)
        if (gimbalPresets.isNotEmpty()) {
            gimbalPresets.forEach { preset ->
                TextButtonView(title = preset.name) {
                    model.remoteControlAssistantMoveToGimbalPreset(id = preset.id)
                }
            }
        } else {
            HCenter {
                Text("No gimbal presets configured in streamer")
            }
        }
    }
}

@Composable
private fun MacroView(model: Model = LocalModel.current, macro: RemoteControlMacro) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(macro.name)
        Spacer(Modifier.weight(1f))
        if (macro.running) {
            TextButton(onClick = {
                model.remoteControlAssistantStopMacro(id = macro.id)
            }) {
                Text("Cancel", color = Color.Red)
            }
        } else {
            TextButton(onClick = {
                model.remoteControlAssistantStartMacro(id = macro.id)
            }) {
                Text("Run")
            }
        }
    }
}

@Composable
private fun MacrosView(model: Model = LocalModel.current, remoteControl: RemoteControl) {
    val macros = remoteControl.macros.collectAsState().value

    Column {
        Text("Macros", style = MaterialTheme.typography.titleSmall)
        if (macros.isNotEmpty()) {
            macros.forEach { macro ->
                MacroView(model = model, macro = macro)
            }
        } else {
            HCenter {
                Text("No macros configured in streamer")
            }
        }
    }
}

@Composable
private fun SrtConnectionPrioritiesView(model: Model = LocalModel.current, remoteControl: RemoteControl) {
    val settings = remoteControl.settings.collectAsState().value

    if (settings != null) {
        RemoteControlSrtConnectionPrioritiesView(
            model = model,
            srt = settings.srt,
            enabled = settings.srt.connectionPrioritiesEnabled,
        )
    }
}

@Composable
private fun FilterToggleView(model: Model = LocalModel.current, filter: RemoteControlFilter, value: Boolean) {
    var valueState by remember(value) { mutableStateOf(value) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(filter.toString())
        Spacer(Modifier.weight(1f))
        Switch(checked = valueState, onCheckedChange = { valueState = it })
    }
    LaunchedEffect(valueState) {
        if (valueState != model.remoteControlAssistantStreamerState.filters?.get(filter)) {
            model.remoteControlAssistantSetFilter(filter = filter, on = valueState)
        }
    }
}

@Composable
private fun FiltersView(model: Model = LocalModel.current, remoteControl: RemoteControl) {
    val pixellate = remoteControl.pixellate.collectAsState().value
    val movie = remoteControl.movie.collectAsState().value
    val grayScale = remoteControl.grayScale.collectAsState().value
    val sepia = remoteControl.sepia.collectAsState().value
    val triple = remoteControl.triple.collectAsState().value
    val twin = remoteControl.twin.collectAsState().value
    val fourThree = remoteControl.fourThree.collectAsState().value
    val pinch = remoteControl.pinch.collectAsState().value
    val whirlpool = remoteControl.whirlpool.collectAsState().value
    val poll = remoteControl.poll.collectAsState().value
    val blurFaces = remoteControl.blurFaces.collectAsState().value
    val privacy = remoteControl.privacy.collectAsState().value
    val beauty = remoteControl.beauty.collectAsState().value
    val moblinInMouth = remoteControl.moblinInMouth.collectAsState().value
    val cameraMan = remoteControl.cameraMan.collectAsState().value

    Column {
        Text("Filters", style = MaterialTheme.typography.titleSmall)
        FilterToggleView(model = model, filter = RemoteControlFilter.Pixellate, value = pixellate)
        FilterToggleView(model = model, filter = RemoteControlFilter.Movie, value = movie)
        FilterToggleView(model = model, filter = RemoteControlFilter.GrayScale, value = grayScale)
        FilterToggleView(model = model, filter = RemoteControlFilter.Sepia, value = sepia)
        FilterToggleView(model = model, filter = RemoteControlFilter.Triple, value = triple)
        FilterToggleView(model = model, filter = RemoteControlFilter.Twin, value = twin)
        FilterToggleView(model = model, filter = RemoteControlFilter.FourThree, value = fourThree)
        FilterToggleView(model = model, filter = RemoteControlFilter.Pinch, value = pinch)
        FilterToggleView(model = model, filter = RemoteControlFilter.Whirlpool, value = whirlpool)
        FilterToggleView(model = model, filter = RemoteControlFilter.Poll, value = poll)
        FilterToggleView(model = model, filter = RemoteControlFilter.BlurFaces, value = blurFaces)
        FilterToggleView(model = model, filter = RemoteControlFilter.Privacy, value = privacy)
        FilterToggleView(model = model, filter = RemoteControlFilter.Beauty, value = beauty)
        FilterToggleView(
            model = model,
            filter = RemoteControlFilter.MoblinInMouth,
            value = moblinInMouth,
        )
        FilterToggleView(model = model, filter = RemoteControlFilter.CameraMan, value = cameraMan)
    }
}

@Composable
private fun SendMessageView(model: Model = LocalModel.current) {
    var text by remember { mutableStateOf("") }

    fun send() {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return
        }
        model.remoteControlAssistantSendMessage(text = trimmed)
        text = ""
    }

    Column {
        Text("Send message", style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Message") },
            keyboardActions = KeyboardActions(onDone = { send() }),
            modifier = Modifier.fillMaxWidth(),
        )
        TextButtonView("Send") {
            send()
        }
        Text("Shown in the streamers activity feed.")
    }
}

@Composable
private fun DebugLoggingView(model: Model = LocalModel.current, remoteControl: RemoteControl) {
    val debugLoggingFlow = remoteControl.debugLogging.collectAsState().value
    var debugLogging by remember(debugLoggingFlow) { mutableStateOf(debugLoggingFlow) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Debug logging")
        Spacer(Modifier.weight(1f))
        Switch(checked = debugLogging, onCheckedChange = { debugLogging = it })
    }
    LaunchedEffect(debugLogging) {
        if (debugLogging != model.remoteControlAssistantStreamerState.debugLogging) {
            model.remoteControlAssistantSetDebugLogging(on = debugLogging)
        }
    }
}

@Composable
private fun ControlBarRemoteControlAssistantControlView(
    model: Model = LocalModel.current,
    remoteControl: RemoteControl,
    title: String = "",
) {
    val settings = remoteControl.settings.collectAsState().value
    var presentingLog by remember { mutableStateOf(false) }
    var log by remember { mutableStateOf<List<LogEntry>>(emptyList()) }

    fun reloadLog() {
        log = model.remoteControlAssistantLog
    }

    Column {
        if (title.isNotEmpty()) {
            Text(title, style = MaterialTheme.typography.titleSmall)
        }
        if (settings != null) {
            LiveView(model = model, remoteControl = remoteControl)
            RecordingView(model = model, remoteControl = remoteControl)
            MutedView(model = model, remoteControl = remoteControl)
            StealthModeControlView(model = model, remoteControl = remoteControl)
            PreviewStreamView(model = model, remoteControl = remoteControl)
            ZoomView(model = model, remoteControl = remoteControl)
            ScenePickerView(model = model, remoteControl = remoteControl)
            AutoSceneSwitcherPickerView(model = model, remoteControl = remoteControl)
            MicView(model = model, remoteControl = remoteControl)
            BitrateView(model = model, remoteControl = remoteControl)
            SrtConnectionPrioritiesView(model = model, remoteControl = remoteControl)
            GimbalPresetView(model = model, remoteControl = remoteControl)
            MacrosView(model = model, remoteControl = remoteControl)
            FiltersView(model = model, remoteControl = remoteControl)
            SendMessageView(model = model)
            DebugLoggingView(model = model, remoteControl = remoteControl)
        } else {
            HCenter {
                CircularProgressIndicator()
            }
        }
        TextButtonView("Reload browser widgets") {
            model.remoteControlAssistantReloadBrowserWidgets()
        }
        TextButtonView("Refresh status") {
            model.updateRemoteControlAssistantStatus()
        }
        TextButtonView("Log") {
            presentingLog = true
        }
        if (presentingLog) {
            Surface(modifier = Modifier.fillMaxSize()) {
                DebugLogSettingsView(
                    model = model,
                    debug = model.database.debug,
                    log = log,
                    presentingLog = presentingLog,
                    onPresentingLogChange = { presentingLog = it },
                    reloadLog = { reloadLog() },
                    clearLog = { model.clearRemoteControlAssistantLog() },
                )
                LaunchedEffect(Unit) {
                    reloadLog()
                }
            }
        }
    }
}

@Composable
private fun StreamerSelectionButtonView(remoteControl: RemoteControl) {
    TextButton(onClick = {
        remoteControl.presentingStreamers = true
    }) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier
                .padding(7.dp)
                .size(30.dp)
                .border(1.dp, Color.Gray, CircleShape),
        )
    }
}

@Composable
private fun ButtonsView(model: Model = LocalModel.current) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Row {
                StreamerSelectionButtonView(remoteControl = model.remoteControl)
                CloseButtonView(onClick = {
                    model.showingRemoteControl = false
                    model.setQuickButton(
                        type = QuickButtonType.Remote,
                        isOn = model.showingRemoteControl,
                    )
                })
            }
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun StreamerNotConfiguredView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Text("No streamer selected.")
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun WaitingForStreamerView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Text("Waiting for the remote control streamer to connect...")
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun ControlBarRemoteControlAssistantInnerView(
    model: Model = LocalModel.current,
    remoteControlSettings: SettingsRemoteControl,
    remoteControl: RemoteControl,
    orientation: Orientation,
) {
    var didDetachCamera by remember { mutableStateOf(false) }
    val presentingPreview = remoteControl.presentingPreview.collectAsState().value
    val presentingPreviewFullScreen = remoteControl.presentingPreviewFullScreen.collectAsState().value
    val presentingStreamers = remoteControl.presentingStreamers.collectAsState().value
    val preview = remoteControl.preview.collectAsState().value
    val isPortrait = orientation.isPortrait.collectAsState().value

    Box(modifier = Modifier.fillMaxSize()) {
        if (presentingPreviewFullScreen) {
            if (!model.isRemoteControlAssistantConfigured()) {
                StreamerNotConfiguredView()
            } else if (model.isRemoteControlAssistantConnected()) {
                if (preview != null) {
                    Image(
                        bitmap = preview.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        remoteControl.presentingPreviewFullScreen = false
                                    },
                                )
                            },
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Text("No preview received yet.")
                }
            } else {
                WaitingForStreamerView()
            }
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                if (!model.isRemoteControlAssistantConfigured()) {
                    StreamerNotConfiguredView()
                } else if (!model.isRemoteControlAssistantConnected()) {
                    WaitingForStreamerView()
                } else if (isPortrait) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        ControlBarRemoteControlAssistantStatusView(
                            model = model,
                            remoteControl = remoteControl,
                            title = "Preview",
                        )
                        ControlBarRemoteControlAssistantControlView(
                            model = model,
                            remoteControl = remoteControl,
                            title = "Control",
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        ControlBarRemoteControlAssistantStatusView(
                            model = model,
                            remoteControl = remoteControl,
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        ControlBarRemoteControlAssistantControlView(
                            model = model,
                            remoteControl = remoteControl,
                        )
                    }
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        model.updateRemoteControlAssistantStatus()
        didDetachCamera = !(model.isLive || model.isRecording)
        if (didDetachCamera) {
            model.detachCamera()
        }
        model.updateScreenAutoOff()
        if (presentingPreview) {
            model.remoteControlAssistantStartPreview(
                user = RemoteControlAssistantPreviewUser.Panel,
            )
        }
        model.remoteControlAssistantStartStatus()
    }
    DisposableEffect(Unit) {
        onDispose {
            if (didDetachCamera) {
                model.attachCamera()
            }
            model.updateScreenAutoOff()
            model.remoteControlAssistantStopPreview(
                user = RemoteControlAssistantPreviewUser.Panel,
            )
            model.remoteControlAssistantStopStatus()
        }
    }
    if (presentingStreamers) {
        ModalBottomSheet(onDismissRequest = {
            remoteControl.presentingStreamers = false
        }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text("Streamers", style = MaterialTheme.typography.titleSmall)
                RemoteControlStreamersView(
                    model = model,
                    remoteControlSettings = model.database.remoteControl,
                )
            }
        }
    }
}

@Composable
fun ControlBarRemoteControlAssistantView(
    model: Model = LocalModel.current,
    remoteControlSettings: SettingsRemoteControl,
) {
    fun title(): String {
        val streamerName = remoteControlSettings.getSelectedStreamerName()
        return if (streamerName != null) {
            localized("Remote control assistant") + " ($streamerName)"
        } else {
            localized("Remote control assistant")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
    ) {
        ControlBarRemoteControlAssistantInnerView(
            model = model,
            remoteControlSettings = model.database.remoteControl,
            remoteControl = model.remoteControl,
            orientation = model.orientation,
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title(),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(5.dp),
            )
            Spacer(Modifier.weight(1f))
        }
        ButtonsView(model = model)
    }
}

private fun statusIcon(name: String): ImageVector = when (name) {
    "battery.0" -> Icons.Default.Settings
    "flame" -> Icons.Default.Warning
    "wifi" -> Icons.Default.Notifications
    "dot.radiowaves.left.and.right" -> Icons.Default.Share
    "camera" -> Icons.Default.Face
    "music.mic" -> Icons.Default.Call
    "magnifyingglass" -> Icons.Default.Search
    "xserve" -> Icons.Default.Home
    "megaphone" -> Icons.Default.Notifications
    "message" -> Icons.Default.MailOutline
    "eye" -> Icons.Default.Face
    "waveform" -> Icons.Default.Star
    "cpu" -> Icons.Default.Build
    "server.rack" -> Icons.Default.Settings
    "app.connected.to.app.below.fill" -> Icons.Default.Share
    "appletvremote.gen1" -> Icons.Default.Settings
    "gamecontroller" -> Icons.Default.PlayArrow
    "speedometer" -> Icons.Default.Settings
    "deskclock" -> Icons.Default.DateRange
    "location" -> Icons.Default.LocationOn
    "phone.connection" -> Icons.Default.Call
    "record.circle" -> Icons.Default.CheckCircle
    "play" -> Icons.Default.PlayArrow
    "globe" -> Icons.Default.Place
    else -> Icons.Default.Info
}

@Composable
private fun <T> RemoteControlPicker(
    label: String,
    options: List<Pair<String, T?>>,
    selected: T?,
    onSelect: (T?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = options.firstOrNull { it.second == selected }?.first ?: ""

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.first) },
                    onClick = {
                        expanded = false
                        onSelect(option.second)
                    },
                )
            }
        }
    }
}

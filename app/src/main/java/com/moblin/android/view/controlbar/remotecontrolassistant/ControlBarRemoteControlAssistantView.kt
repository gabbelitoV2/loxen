package com.moblin.android.view.controlbar.remotecontrolassistant

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalModel
import com.moblin.android.common.various.clippingThresholdDb
import com.moblin.android.common.various.formatAudioLevelChannels
import com.moblin.android.common.various.formatBytesPerSecond
import com.moblin.android.common.various.redThresholdDb
import com.moblin.android.common.various.smallFont
import com.moblin.android.common.various.yellowThresholdDb
import com.moblin.android.common.various.zeroThresholdDb
import com.moblin.android.localized
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.ConfirmationDialog
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.FormSlider
import com.moblin.android.platform.swiftui.FullScreenCover
import com.moblin.android.platform.swiftui.IosSwitch
import com.moblin.android.platform.swiftui.LocalTint
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Sheet
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.formBodyStyle
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.remotecontrol.RemoteControlFilter
import com.moblin.android.remotecontrol.RemoteControlMacro
import com.moblin.android.remotecontrol.RemoteControlSettingsSrt
import com.moblin.android.remotecontrol.RemoteControlSettingsSrtConnectionPriority
import com.moblin.android.remotecontrol.RemoteControlStatusGeneral
import com.moblin.android.remotecontrol.RemoteControlStatusItem
import com.moblin.android.various.model.LogEntry
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.Orientation
import com.moblin.android.various.model.RemoteControl
import com.moblin.android.various.model.RemoteControlAssistantPreviewUser
import com.moblin.android.various.model.clearRemoteControlAssistantLog
import com.moblin.android.various.model.isRemoteControlAssistantConfigured
import com.moblin.android.various.model.isRemoteControlAssistantConnected
import com.moblin.android.various.model.remoteControlAssistantMoveToGimbalPreset
import com.moblin.android.various.model.remoteControlAssistantReloadBrowserWidgets
import com.moblin.android.various.model.remoteControlAssistantSendMessage
import com.moblin.android.various.model.remoteControlAssistantSetAutoSceneSwitcher
import com.moblin.android.various.model.remoteControlAssistantSetBitratePreset
import com.moblin.android.various.model.remoteControlAssistantSetDebugLogging
import com.moblin.android.various.model.remoteControlAssistantSetFilter
import com.moblin.android.various.model.remoteControlAssistantSetLive
import com.moblin.android.various.model.remoteControlAssistantSetMic
import com.moblin.android.various.model.remoteControlAssistantSetMute
import com.moblin.android.various.model.remoteControlAssistantSetPreviewStream
import com.moblin.android.various.model.remoteControlAssistantSetRecord
import com.moblin.android.various.model.remoteControlAssistantSetScene
import com.moblin.android.various.model.remoteControlAssistantSetSrtConnectionPriority
import com.moblin.android.various.model.remoteControlAssistantSetSrtConnectionPriorityEnabled
import com.moblin.android.various.model.remoteControlAssistantSetStealthMode
import com.moblin.android.various.model.remoteControlAssistantSetZoom
import com.moblin.android.various.model.remoteControlAssistantSetZoomPreset
import com.moblin.android.various.model.remoteControlAssistantStartMacro
import com.moblin.android.various.model.remoteControlAssistantStartPreview
import com.moblin.android.various.model.remoteControlAssistantStartStatus
import com.moblin.android.various.model.remoteControlAssistantStopMacro
import com.moblin.android.various.model.remoteControlAssistantStopPreview
import com.moblin.android.various.model.remoteControlAssistantStopStatus
import com.moblin.android.various.model.updateRemoteControlAssistantStatus
import com.moblin.android.various.settings.SettingsQuickButtonType
import com.moblin.android.various.settings.SettingsRemoteControl
import com.moblin.android.view.CloseButtonView
import com.moblin.android.view.settings.debug.DebugLogSettingsView
import com.moblin.android.view.settings.remotecontrol.RemoteControlStreamersView
import com.moblin.android.view.settings.streams.stream.srt.clampConnectionPriority
import com.moblin.android.view.settings.streams.stream.srt.maximumSrtConnectionPriority
import com.moblin.android.view.settings.streams.stream.srt.minimumSrtConnectionPriority
import com.moblin.android.view.utils.BorderlessButtonView
import com.moblin.android.view.utils.CloseToolbar
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.TextButtonView
import java.util.UUID
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
private fun StatusItemView(icon: String, status: RemoteControlStatusItem?) {
    if (status != null) {
        val palette = formPalette()
        CompositionLocalProvider(LocalTextStyle provides smallFont) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.width(20.dp), contentAlignment = Alignment.Center) {
                    SystemImage(
                        name = icon,
                        fontSize = smallFont.fontSize,
                        tint = if (status.ok) palette.label else palette.red,
                    )
                }
                Text(text = status.message)
            }
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
            .firstOrNull { iface -> iface.interfaceName == priority.name }
            ?.name
        return if (name != null && name.isNotEmpty()) {
            name
        } else {
            priority.name
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = makeName(), modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(8.dp))
            IosSwitch(
                checked = enabledState,
                onCheckedChange = { value ->
                    enabledState = value
                    model.remoteControlAssistantSetSrtConnectionPriority(priority = priority.copy(enabled = value))
                },
            )
        }
        FormSlider(
            value = prioState,
            onValueChange = { prioState = it.roundToInt().toFloat() },
            modifier = Modifier.fillMaxWidth(),
            valueRange = minimumSrtConnectionPriority.toFloat()..maximumSrtConnectionPriority.toFloat(),
            onValueChangeFinished = {
                model.remoteControlAssistantSetSrtConnectionPriority(
                    priority = priority.copy(priority = clampConnectionPriority(value = prioState.toInt())),
                )
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

    Form(title = "SRT connection priorities") {
        Section {
            Toggle(title = "Enabled", isOn = enabledState) { value ->
                enabledState = value
                model.remoteControlAssistantSetSrtConnectionPriorityEnabled(enabled = value)
            }
        }
        Section {
            srt.connectionPriorities.forEach { priority ->
                key(priority.id) {
                    RemoteControlSrtConnectionPriorityView(
                        model = model,
                        priority = priority,
                        enabled = priority.enabled,
                        prio = priority.priority.toFloat(),
                    )
                }
            }
        }
    }
}

@Composable
private fun RemoteControlAudioLevelView(level: Float, channels: Int?) {
    val palette = formPalette()
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

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(20.dp), contentAlignment = Alignment.Center) {
            SystemImage(name = "waveform", fontSize = 17.sp, tint = palette.label)
        }
        CompositionLocalProvider(LocalTextStyle provides smallFont) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (level.isNaN()) {
                    if (channels == null) {
                        Text(localized("Muted"))
                    } else {
                        Text(localized("Muted,"))
                    }
                } else {
                    Row(modifier = Modifier.padding(bottom = 3.dp)) {
                        if (isClipping()) {
                            Text(clippingText(), color = palette.red, fontWeight = FontWeight.Bold)
                        } else {
                            Text(redText(), color = palette.red, fontWeight = FontWeight.Bold)
                            Text(yellowText(), color = Color(0xFFFFCC00), fontWeight = FontWeight.Bold)
                            Text(greenText(), color = palette.green, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (channels != null) {
                    Text(formatAudioLevelChannels(channels = channels))
                }
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
private fun StatusItemsView(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        content()
    }
}

@Composable
private fun ControlBarRemoteControlAssistantStatusView(
    model: Model = LocalModel.current,
    remoteControl: RemoteControl,
    title: String = "",
) {
    val presentingPreview by remoteControl.presentingPreview.collectAsState()
    val preview by remoteControl.preview.collectAsState()
    val general by remoteControl.general.collectAsState()
    val topLeft by remoteControl.topLeft.collectAsState()
    val topRight by remoteControl.topRight.collectAsState()

    Section(
        header = title,
        footer = if (presentingPreview) "Tap the preview to hide it. Double tap to toggle full screen." else null,
    ) {
        if (presentingPreview) {
            val image = preview
            if (image != null) {
                Image(
                    bitmap = image.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 3.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    remoteControl.presentingPreviewFullScreen.value = true
                                },
                                onTap = {
                                    model.remoteControlAssistantStopPreview(user = RemoteControlAssistantPreviewUser.panel)
                                    remoteControl.presentingPreview.value = false
                                },
                            )
                        },
                    contentScale = ContentScale.Fit,
                )
            } else {
                Text(localized("No preview received yet."))
            }
        } else {
            TextButtonView("Show") {
                model.remoteControlAssistantStartPreview(user = RemoteControlAssistantPreviewUser.panel)
                remoteControl.presentingPreview.value = true
            }
        }
    }
    Section(header = "General") {
        val status = general
        if (status != null) {
            StatusItemsView {
                StatusItemView(icon = "battery.0", status = batteryStatus(status = status))
                StatusItemView(icon = "flame", status = flameStatus(status = status))
                StatusItemView(icon = "wifi", status = ssidStatus(status = status))
            }
        } else {
            Text(localized("No status received yet."))
        }
    }
    Section(header = "Top left") {
        val status = topLeft
        if (status != null) {
            StatusItemsView {
                StatusItemView(icon = "dot.radiowaves.left.and.right", status = status.stream)
                StatusItemView(icon = "camera", status = status.camera)
                StatusItemView(icon = "music.mic", status = status.mic)
                StatusItemView(icon = "magnifyingglass", status = status.zoom)
                StatusItemView(icon = "xserve", status = status.obs)
                StatusItemView(icon = "megaphone", status = status.events)
                StatusItemView(icon = "message", status = status.chat)
                StatusItemView(icon = "eye", status = status.viewers)
            }
        } else {
            Text(localized("No status received yet."))
        }
    }
    Section(header = "Top right") {
        val status = topRight
        if (status != null) {
            StatusItemsView {
                val audioInfo = status.audioInfo
                if (audioInfo != null) {
                    RemoteControlAudioLevelView(
                        level = audioInfo.audioLevel.toFloat(),
                        channels = audioInfo.numberOfAudioChannels,
                    )
                } else {
                    StatusItemView(icon = "waveform", status = status.audioLevel)
                }
                StatusItemView(icon = "cpu", status = status.systemMonitor)
                StatusItemView(icon = "server.rack", status = status.rtmpServer)
                StatusItemView(icon = "app.connected.to.app.below.fill", status = status.moblink)
                StatusItemView(icon = "appletvremote.gen1", status = status.remoteControl)
                StatusItemView(icon = "appletvremote.gen1", status = status.djiDevices)
                StatusItemView(icon = "gamecontroller", status = status.gameController)
                StatusItemView(icon = "speedometer", status = status.bitrate)
                StatusItemView(icon = "deskclock", status = status.uptime)
                StatusItemView(icon = "location", status = status.location)
                StatusItemView(icon = "phone.connection", status = status.srtla)
                StatusItemView(icon = "phone.connection", status = status.srtlaRtts)
                StatusItemView(icon = "record.circle", status = status.recording)
                StatusItemView(icon = "play", status = status.replay)
                StatusItemView(icon = "globe", status = status.browserWidgets)
            }
        } else {
            Text(localized("No status received yet."))
        }
    }
}

@Composable
private fun LiveView(model: Model = LocalModel.current, remoteControl: RemoteControl) {
    val streaming by remoteControl.streaming.collectAsState()
    var presentingConfirm by remember { mutableStateOf(false) }
    var pendingStreaming by remember { mutableStateOf(false) }

    Toggle(title = "Live", isOn = streaming) { value ->
        if (value != model.remoteControlAssistantStreamerState.streaming) {
            pendingStreaming = value
            presentingConfirm = true
        }
    }
    ConfirmationDialog(
        title = "",
        isPresented = presentingConfirm,
        onDismissRequest = { presentingConfirm = false },
    ) {
        Button(if (pendingStreaming) "Go Live" else "End") {
            model.remoteControlAssistantSetLive(on = pendingStreaming)
            remoteControl.streaming.value = pendingStreaming
        }
    }
}

@Composable
private fun RecordingView(model: Model = LocalModel.current, remoteControl: RemoteControl) {
    val recording by remoteControl.recording.collectAsState()
    var presentingConfirm by remember { mutableStateOf(false) }
    var pendingRecording by remember { mutableStateOf(false) }

    Toggle(title = "Recording", isOn = recording) { value ->
        if (value != model.remoteControlAssistantStreamerState.recording) {
            pendingRecording = value
            presentingConfirm = true
        }
    }
    ConfirmationDialog(
        title = "",
        isPresented = presentingConfirm,
        onDismissRequest = { presentingConfirm = false },
    ) {
        Button(if (pendingRecording) "Start recording" else "Stop recording") {
            model.remoteControlAssistantSetRecord(on = pendingRecording)
            remoteControl.recording.value = pendingRecording
        }
    }
}

@Composable
private fun MutedView(model: Model = LocalModel.current, remoteControl: RemoteControl) {
    val muted by remoteControl.muted.collectAsState()

    Toggle(title = "Muted", isOn = muted) { value ->
        remoteControl.muted.value = value
        if (value != model.remoteControlAssistantStreamerState.muted) {
            model.remoteControlAssistantSetMute(on = value)
        }
    }
}

@Composable
private fun PreviewStreamView(model: Model = LocalModel.current, remoteControl: RemoteControl) {
    val previewStream by remoteControl.previewStream.collectAsState()

    Toggle(title = "Preview stream", isOn = previewStream) { value ->
        remoteControl.previewStream.value = value
        if (value != model.remoteControlAssistantStreamerState.previewStream) {
            model.remoteControlAssistantSetPreviewStream(on = value)
        }
    }
}

@Composable
private fun StealthModeControlView(model: Model = LocalModel.current, remoteControl: RemoteControl) {
    val stealthMode by remoteControl.stealthMode.collectAsState()

    Toggle(title = "Stealth mode", isOn = stealthMode) { value ->
        remoteControl.stealthMode.value = value
        if (value != model.remoteControlAssistantStreamerState.stealthMode) {
            model.remoteControlAssistantSetStealthMode(on = value)
        }
    }
}

@Composable
private fun <T> SegmentedPicker(selection: T, options: List<T>, text: (T) -> String, onChange: (T) -> Unit) {
    val palette = formPalette()
    val dark = isSystemInDarkTheme()
    val trackColor = if (dark) Color(0x3D767680) else Color(0x1F767680)
    val thumbColor = if (dark) Color(0xFF636366) else Color.White
    val thumbShape = RoundedCornerShape(7.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(trackColor)
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEachIndexed { index, option ->
            val selected = option == selection
            if (index > 0) {
                val hidden = selected || options[index - 1] == selection
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(12.dp)
                        .background(if (hidden) Color.Transparent else palette.separator),
                )
            }
            val interactionSource = remember { MutableInteractionSource() }
            val pressed by interactionSource.collectIsPressedAsState()
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(
                        if (selected) {
                            Modifier
                                .shadow(elevation = 2.dp, shape = thumbShape)
                                .background(thumbColor, thumbShape)
                        } else {
                            Modifier
                        },
                    )
                    .clickable(interactionSource = interactionSource, indication = null) {
                        if (!selected) {
                            onChange(option)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = text(option),
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    ),
                    color = palette.label,
                    maxLines = 1,
                    modifier = Modifier
                        .alpha(if (pressed && !selected) 0.3f else 1f)
                        .padding(horizontal = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun ZoomView(model: Model = LocalModel.current, remoteControl: RemoteControl) {
    val palette = formPalette()
    val zoom by remoteControl.zoom.collectAsState()
    val zoomPreset by remoteControl.zoomPreset.collectAsState()
    val zoomPresets by remoteControl.zoomPresets.collectAsState()

    fun submitZoom(value: String) {
        val x = value.toFloatOrNull()
        if (x == null || !x.isFinite()) {
            val currentZoom = model.remoteControlAssistantStreamerState.zoom
            if (currentZoom != null) {
                remoteControl.zoom.value = currentZoom.toString()
            }
            return
        }
        model.remoteControlAssistantSetZoom(x = x)
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(localized("Zoom"))
        Spacer(modifier = Modifier.width(8.dp))
        BasicTextField(
            value = zoom,
            onValueChange = { remoteControl.zoom.value = it },
            modifier = Modifier.weight(1f),
            textStyle = formBodyStyle.copy(color = palette.label, textAlign = TextAlign.End),
            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    val currentZoom = model.remoteControlAssistantStreamerState.zoom
                    if (currentZoom != null && remoteControl.zoom.value != currentZoom.toString()) {
                        submitZoom(value = remoteControl.zoom.value)
                    }
                },
            ),
            singleLine = true,
            cursorBrush = SolidColor(palette.accent),
        )
    }
    FormRow {
        SegmentedPicker(
            selection = zoomPreset,
            options = zoomPresets.map { it.id },
            text = { id -> zoomPresets.firstOrNull { it.id == id }?.name ?: "" },
        ) { id ->
            remoteControl.zoomPreset.value = id
            if (id != model.remoteControlAssistantStreamerState.zoomPreset) {
                model.remoteControlAssistantSetZoomPreset(id = id)
            }
        }
    }
}

@Composable
private fun ScenePickerView(model: Model = LocalModel.current, remoteControl: RemoteControl) {
    val scene by remoteControl.scene.collectAsState()
    val settings by remoteControl.settings.collectAsState()
    val scenes = settings?.scenes ?: emptyList()

    Picker(
        title = "Scene",
        selection = scene,
        options = scenes.map { it.id },
        text = { id -> scenes.firstOrNull { it.id == id }?.name ?: "" },
    ) { id ->
        remoteControl.scene.value = id
        if (id != model.remoteControlAssistantStreamerState.scene) {
            model.remoteControlAssistantSetScene(id = id)
        }
    }
}

@Composable
private fun AutoSceneSwitcherPickerView(model: Model = LocalModel.current, remoteControl: RemoteControl) {
    val autoSceneSwitcher by remoteControl.autoSceneSwitcher.collectAsState()
    val settings by remoteControl.settings.collectAsState()
    val autoSceneSwitchers = settings?.autoSceneSwitchers ?: emptyList()

    Picker(
        title = "Auto scene switcher",
        selection = autoSceneSwitcher,
        options = listOf<UUID?>(null) + autoSceneSwitchers.map { it.id },
        text = { id ->
            if (id == null) {
                localized("-- None --")
            } else {
                autoSceneSwitchers.firstOrNull { it.id == id }?.name ?: ""
            }
        },
    ) { id ->
        remoteControl.autoSceneSwitcher.value = id
        if (id != model.remoteControlAssistantStreamerState.autoSceneSwitcher?.id) {
            model.remoteControlAssistantSetAutoSceneSwitcher(id = id)
        }
    }
}

@Composable
private fun MicView(model: Model = LocalModel.current, remoteControl: RemoteControl) {
    val mic by remoteControl.mic.collectAsState()
    val settings by remoteControl.settings.collectAsState()
    val mics = settings?.mics ?: emptyList()

    Picker(
        title = "Mic",
        selection = mic,
        options = mics.map { it.id },
        text = { id -> mics.firstOrNull { it.id == id }?.name ?: "" },
    ) { id ->
        remoteControl.mic.value = id
        if (id != model.remoteControlAssistantStreamerState.mic) {
            model.remoteControlAssistantSetMic(id = id)
        }
    }
}

@Composable
private fun BitrateView(model: Model = LocalModel.current, remoteControl: RemoteControl) {
    val bitrate by remoteControl.bitrate.collectAsState()
    val settings by remoteControl.settings.collectAsState()
    val bitratePresets = settings?.bitratePresets ?: emptyList()

    Picker(
        title = "Bitrate",
        selection = bitrate,
        options = bitratePresets.map { it.id },
        text = { id ->
            val preset = bitratePresets.firstOrNull { it.id == id }
            if (preset == null) {
                ""
            } else if (preset.bitrate.toLong() > 0) {
                formatBytesPerSecond(speed = preset.bitrate.toLong())
            } else {
                "Unknown"
            }
        },
    ) { id ->
        remoteControl.bitrate.value = id
        if (id != model.remoteControlAssistantStreamerState.bitrate) {
            model.remoteControlAssistantSetBitratePreset(id = id)
        }
    }
}

@Composable
private fun GimbalPresetView(model: Model = LocalModel.current, remoteControl: RemoteControl) {
    NavigationLink("Gimbal presets") {
        val gimbalPresets by remoteControl.gimbalPresets.collectAsState()
        Form(title = "Gimbal presets") {
            Section {
                if (gimbalPresets.isNotEmpty()) {
                    gimbalPresets.forEach { preset ->
                        key(preset.id) {
                            TextButtonView(title = preset.name) {
                                model.remoteControlAssistantMoveToGimbalPreset(id = preset.id)
                            }
                        }
                    }
                } else {
                    HCenter {
                        Text(localized("No gimbal presets configured in streamer"))
                    }
                }
            }
        }
    }
}

@Composable
private fun MacroView(model: Model = LocalModel.current, macro: RemoteControlMacro) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = macro.name, modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(8.dp))
        if (macro.running) {
            CompositionLocalProvider(LocalTint provides formPalette().red) {
                BorderlessButtonView(text = "Cancel") {
                    model.remoteControlAssistantStopMacro(id = macro.id)
                }
            }
        } else {
            BorderlessButtonView(text = "Run") {
                model.remoteControlAssistantStartMacro(id = macro.id)
            }
        }
    }
}

@Composable
private fun MacrosView(model: Model = LocalModel.current, remoteControl: RemoteControl) {
    NavigationLink("Macros") {
        val macros by remoteControl.macros.collectAsState()
        Form(title = "Macros") {
            Section {
                if (macros.isNotEmpty()) {
                    macros.forEach { macro ->
                        key(macro.id) {
                            MacroView(model = model, macro = macro)
                        }
                    }
                } else {
                    HCenter {
                        Text(localized("No macros configured in streamer"))
                    }
                }
            }
        }
    }
}

@Composable
private fun SrtConnectionPrioritiesView(model: Model = LocalModel.current, remoteControl: RemoteControl) {
    val settings by remoteControl.settings.collectAsState()
    val currentSettings = settings

    if (currentSettings != null) {
        NavigationLink("SRT connection priorities") {
            RemoteControlSrtConnectionPrioritiesView(
                model = model,
                srt = currentSettings.srt,
                enabled = currentSettings.srt.connectionPrioritiesEnabled,
            )
        }
    }
}

@Composable
private fun FilterToggleView(
    model: Model = LocalModel.current,
    filter: RemoteControlFilter,
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
) {
    Toggle(title = filter.toString(), isOn = value) { newValue ->
        onValueChange(newValue)
        if (newValue != model.remoteControlAssistantStreamerState.filters?.get(filter)) {
            model.remoteControlAssistantSetFilter(filter = filter, on = newValue)
        }
    }
}

@Composable
private fun FiltersView(model: Model = LocalModel.current, remoteControl: RemoteControl) {
    NavigationLink("Filters") {
        val pixellate by remoteControl.pixellate.collectAsState()
        val movie by remoteControl.movie.collectAsState()
        val grayScale by remoteControl.grayScale.collectAsState()
        val sepia by remoteControl.sepia.collectAsState()
        val triple by remoteControl.triple.collectAsState()
        val twin by remoteControl.twin.collectAsState()
        val fourThree by remoteControl.fourThree.collectAsState()
        val pinch by remoteControl.pinch.collectAsState()
        val whirlpool by remoteControl.whirlpool.collectAsState()
        val poll by remoteControl.poll.collectAsState()
        val blurFaces by remoteControl.blurFaces.collectAsState()
        val privacy by remoteControl.privacy.collectAsState()
        val beauty by remoteControl.beauty.collectAsState()
        val moblinInMouth by remoteControl.moblinInMouth.collectAsState()
        val cameraMan by remoteControl.cameraMan.collectAsState()
        Form(title = "Filters") {
            Section {
                FilterToggleView(model = model, filter = RemoteControlFilter.Pixellate, value = pixellate) {
                    remoteControl.pixellate.value = it
                }
                FilterToggleView(model = model, filter = RemoteControlFilter.Movie, value = movie) {
                    remoteControl.movie.value = it
                }
                FilterToggleView(model = model, filter = RemoteControlFilter.GrayScale, value = grayScale) {
                    remoteControl.grayScale.value = it
                }
                FilterToggleView(model = model, filter = RemoteControlFilter.Sepia, value = sepia) {
                    remoteControl.sepia.value = it
                }
                FilterToggleView(model = model, filter = RemoteControlFilter.Triple, value = triple) {
                    remoteControl.triple.value = it
                }
                FilterToggleView(model = model, filter = RemoteControlFilter.Twin, value = twin) {
                    remoteControl.twin.value = it
                }
                FilterToggleView(model = model, filter = RemoteControlFilter.FourThree, value = fourThree) {
                    remoteControl.fourThree.value = it
                }
                FilterToggleView(model = model, filter = RemoteControlFilter.Pinch, value = pinch) {
                    remoteControl.pinch.value = it
                }
                FilterToggleView(model = model, filter = RemoteControlFilter.Whirlpool, value = whirlpool) {
                    remoteControl.whirlpool.value = it
                }
                FilterToggleView(model = model, filter = RemoteControlFilter.Poll, value = poll) {
                    remoteControl.poll.value = it
                }
                FilterToggleView(model = model, filter = RemoteControlFilter.BlurFaces, value = blurFaces) {
                    remoteControl.blurFaces.value = it
                }
                FilterToggleView(model = model, filter = RemoteControlFilter.Privacy, value = privacy) {
                    remoteControl.privacy.value = it
                }
                FilterToggleView(model = model, filter = RemoteControlFilter.Beauty, value = beauty) {
                    remoteControl.beauty.value = it
                }
                FilterToggleView(
                    model = model,
                    filter = RemoteControlFilter.MoblinInMouth,
                    value = moblinInMouth,
                ) {
                    remoteControl.moblinInMouth.value = it
                }
                FilterToggleView(model = model, filter = RemoteControlFilter.CameraMan, value = cameraMan) {
                    remoteControl.cameraMan.value = it
                }
            }
        }
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

    NavigationLink("Send message") {
        val palette = formPalette()
        Form(title = "Send message") {
            Section(footer = "Shown in the streamers activity feed.") {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = formBodyStyle.copy(color = palette.label),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { send() }),
                    singleLine = true,
                    cursorBrush = SolidColor(palette.accent),
                    decorationBox = { innerTextField ->
                        Box {
                            if (text.isEmpty()) {
                                Text(
                                    text = localized("Message"),
                                    style = formBodyStyle,
                                    color = palette.tertiaryLabel,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
                TextButtonView("Send") {
                    send()
                }
            }
        }
    }
}

@Composable
private fun DebugLoggingView(model: Model = LocalModel.current, remoteControl: RemoteControl) {
    val debugLogging by remoteControl.debugLogging.collectAsState()

    Toggle(title = "Debug logging", isOn = debugLogging) { value ->
        remoteControl.debugLogging.value = value
        if (value != model.remoteControlAssistantStreamerState.debugLogging) {
            model.remoteControlAssistantSetDebugLogging(on = value)
        }
    }
}

@Composable
private fun ControlBarRemoteControlAssistantControlView(
    model: Model = LocalModel.current,
    remoteControl: RemoteControl,
    title: String = "",
) {
    val settings by remoteControl.settings.collectAsState()
    var presentingLog by remember { mutableStateOf(false) }
    var log by remember { mutableStateOf<List<LogEntry>>(emptyList()) }

    fun reloadLog() {
        log = model.remoteControlAssistantLog.toList()
    }

    Section(header = title) {
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
    }
    Section {
        TextButtonView("Reload browser widgets") {
            model.remoteControlAssistantReloadBrowserWidgets()
        }
        TextButtonView("Refresh status") {
            model.updateRemoteControlAssistantStatus()
        }
        TextButtonView("Log") {
            presentingLog = true
        }
        FullScreenCover(isPresented = presentingLog, onDismissRequest = { presentingLog = false }) {
            DebugLogSettingsView(
                model = model,
                debug = model.database.debug,
                log = log,
                presentingLog = presentingLog,
                onPresentingLogChange = { presentingLog = it },
                reloadLog = { reloadLog() },
                clearLog = { model.clearRemoteControlAssistantLog() },
                onLogChange = { log = it },
            )
            LaunchedEffect(Unit) {
                reloadLog()
            }
        }
    }
}

@Composable
private fun StreamerSelectionButtonView(remoteControl: RemoteControl) {
    val palette = formPalette()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .alpha(if (pressed) 0.2f else 1f)
            .clickable(interactionSource = interactionSource, indication = null) {
                remoteControl.presentingStreamers.value = true
            }
            .padding(7.dp)
            .size(30.dp)
            .border(1.dp, palette.gray, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        SystemImage(name = "person", fontSize = 17.sp, tint = palette.gray)
    }
}

@Composable
private fun ButtonsView(model: Model = LocalModel.current) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Row {
                StreamerSelectionButtonView(remoteControl = model.remoteControl)
                CloseButtonView(onClose = {
                    model.showingRemoteControl.value = false
                    model.setQuickButton(type = SettingsQuickButtonType.remote, isOn = model.showingRemoteControl.value)
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
        Text(localized("No streamer selected."), color = formPalette().label)
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
        Text(localized("Waiting for the remote control streamer to connect..."), color = formPalette().label)
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
    val presentingPreviewFullScreen by remoteControl.presentingPreviewFullScreen.collectAsState()
    val presentingStreamers by remoteControl.presentingStreamers.collectAsState()
    val preview by remoteControl.preview.collectAsState()
    val isPortrait by orientation.isPortrait.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        if (presentingPreviewFullScreen) {
            if (!model.isRemoteControlAssistantConfigured()) {
                StreamerNotConfiguredView()
            } else if (model.isRemoteControlAssistantConnected()) {
                val image = preview
                if (image != null) {
                    Image(
                        bitmap = image.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        remoteControl.presentingPreviewFullScreen.value = false
                                    },
                                )
                            },
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Text(localized("No preview received yet."), color = formPalette().label)
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
                    Form(title = " ") {
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
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    ) {
                        Form(title = "Status") {
                            ControlBarRemoteControlAssistantStatusView(
                                model = model,
                                remoteControl = remoteControl,
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    ) {
                        Form(title = "Control") {
                            ControlBarRemoteControlAssistantControlView(
                                model = model,
                                remoteControl = remoteControl,
                            )
                        }
                    }
                }
            }
        }
    }
    DisposableEffect(Unit) {
        model.updateRemoteControlAssistantStatus()
        val didDetachCamera = !(model.isLive.value || model.isRecording.value)
        if (didDetachCamera) {
            model.detachCamera()
        }
        model.updateScreenAutoOff()
        if (remoteControl.presentingPreview.value) {
            model.remoteControlAssistantStartPreview(user = RemoteControlAssistantPreviewUser.panel)
        }
        model.remoteControlAssistantStartStatus()
        onDispose {
            if (didDetachCamera) {
                model.attachCamera()
            }
            model.updateScreenAutoOff()
            model.remoteControlAssistantStopPreview(user = RemoteControlAssistantPreviewUser.panel)
            model.remoteControlAssistantStopStatus()
        }
    }
    Sheet(
        isPresented = presentingStreamers,
        onDismissRequest = { remoteControl.presentingStreamers.value = false },
    ) {
        Form(
            title = "Streamers",
            toolbar = {
                CloseToolbar(
                    presenting = presentingStreamers,
                    onPresentingChange = { remoteControl.presentingStreamers.value = it },
                )
            },
        ) {
            RemoteControlStreamersView(
                model = model,
                remoteControlSettings = model.database.remoteControl,
            )
        }
    }
}

@Composable
fun ControlBarRemoteControlAssistantView(
    model: Model = LocalModel.current,
    remoteControlSettings: SettingsRemoteControl,
) {
    val palette = formPalette()

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
            .background(palette.groupedBackground),
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
                style = TextStyle(fontSize = 20.sp, lineHeight = 25.sp),
                color = palette.label,
                modifier = Modifier.padding(5.dp),
            )
            Spacer(Modifier.weight(1f))
        }
        ButtonsView(model = model)
    }
}

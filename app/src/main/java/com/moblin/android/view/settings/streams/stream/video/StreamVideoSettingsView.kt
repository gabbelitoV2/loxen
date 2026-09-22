package com.moblin.android.view.settings.streams.stream.video

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.formatBytesPerSecond
import com.moblin.android.localized
import com.moblin.android.media.haishinkit.codec.video.VideoEncoderSettings
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.various.settings.SettingsStreamCodec
import com.moblin.android.various.settings.SettingsStreamH264Profile
import com.moblin.android.various.settings.SettingsStreamProtocol
import com.moblin.android.various.settings.SettingsStreamRateControl
import com.moblin.android.various.settings.SettingsStreamResolution
import com.moblin.android.various.settings.fpss
import com.moblin.android.view.utils.TextItemLocalizedView
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

@Composable
private fun SettingsSection(
    headerText: String? = null,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (headerText != null) {
            Text(
                headerText,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        content()
        if (footer != null) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                footer()
            }
        }
    }
    HorizontalDivider()
}

@Composable
private fun SettingsFooterText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SettingsToggle(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}

@Composable
private fun NavigationRow(
    title: String,
    enabled: Boolean = true,
    onNavigate: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onNavigate() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, modifier = Modifier.weight(1f))
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SettingsPicker(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onValueChange: (T) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            if (enabled) {
                expanded = it
            }
        },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = label(selected),
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(title) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(label(option)) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ResolutionSettingsView(model: Model = LocalModel.current, stream: SettingsStream) {
    val isLive by model.isLive.collectAsState()
    val isRecording by model.isRecording.collectAsState()
    SettingsSection {
        SettingsPicker(
            title = localized("Resolution"),
            options = SettingsStreamResolution.entries,
            selected = stream.resolution,
            label = { it.shortString() },
            onValueChange = {
                stream.resolution = it
                Unit
            },
            enabled = !(stream.enabled && (isLive || isRecording)),
        )
    }
}

@Composable
private fun FpsSettingsView(model: Model = LocalModel.current, stream: SettingsStream) {
    val isLive by model.isLive.collectAsState()
    val isRecording by model.isRecording.collectAsState()
    SettingsSection(
        footer = {
            SettingsFooterText(
                localized("Lower FPS generally gives brighter image in low light conditions."),
            )
        },
    ) {
        SettingsPicker(
            title = localized("FPS"),
            options = fpss,
            selected = stream.fps,
            label = { it.toString() },
            onValueChange = {
                stream.fps = it
                Unit
            },
            enabled = !(stream.enabled && (isLive || isRecording)),
        )
    }
}

@Composable
private fun LowLightBoostSettingsView(model: Model = LocalModel.current, stream: SettingsStream) {
    SettingsSection(
        footer = {
            SettingsFooterText(
                localized(
                    "Enable low light boost to make builtin cameras automatically " +
                        "lower the selected FPS for brighter image when dark (if supported).",
                ),
            )
        },
    ) {
        SettingsToggle(
            title = localized("Low light boost (LLB)"),
            checked = stream.lowLightBoost,
            onCheckedChange = {
                stream.lowLightBoost = it
                Unit
            },
        )
    }
}

@Composable
private fun CodecSettingsView(model: Model = LocalModel.current, stream: SettingsStream) {
    val isLive by model.isLive.collectAsState()
    SettingsSection(
        footer = {
            SettingsFooterText(
                localized(
                    "H.265/HEVC generally requires less bandwidth for same image quality. RTMP " +
                        "generally only supports H.264/AVC.",
                ),
            )
        },
    ) {
        SettingsPicker(
            title = localized("Codec"),
            options = SettingsStreamCodec.entries,
            selected = stream.codec,
            label = { it.rawValue },
            onValueChange = {
                stream.codec = it
                Unit
            },
            enabled = !(stream.enabled && isLive),
        )
        if (stream.codec == SettingsStreamCodec.h264avc) {
            SettingsPicker(
                title = localized("Profile"),
                options = SettingsStreamH264Profile.entries,
                selected = stream.h264Profile,
                label = { it.rawValue },
                onValueChange = {
                    stream.h264Profile = it
                    Unit
                },
                enabled = !(stream.enabled && isLive),
            )
        }
    }
}

@Composable
private fun RateControlView(model: Model = LocalModel.current, stream: SettingsStream) {
    val isLive by model.isLive.collectAsState()
    SettingsSection {
        SettingsPicker(
            title = localized("Rate control"),
            options = SettingsStreamRateControl.cases(),
            selected = stream.rateControl,
            label = { it.toString() },
            onValueChange = {
                stream.rateControl = it
                Unit
            },
            enabled = !(stream.enabled && isLive),
        )
    }
}

@Composable
private fun BitrateSettingsView(
    model: Model = LocalModel.current,
    database: Database,
    stream: SettingsStream,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val bitratePresets = database.bitratePresets
    SettingsSection(
        footer = {
            SettingsFooterText(
                localized("About 5-8 Mbps is usually enough for decent image quality."),
            )
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Speed,
                contentDescription = null,
                modifier = Modifier.padding(start = 16.dp),
            )
            SettingsPicker(
                title = localized("Bitrate"),
                options = bitratePresets.map { it.bitrate },
                selected = stream.bitrate,
                label = { formatBytesPerSecond(it.toLong()) },
                onValueChange = {
                    stream.bitrate = it
                    if (stream.enabled) {
                        Unit
                    }
                },
                modifier = Modifier.weight(1f),
            )
        }
        NavigationRow(
            title = localized("Bitrate presets"),
            onNavigate = { onNavigate("Bitrate presets") },
        )
    }
}

private fun submitMaxKeyFrameInterval(value: String, stream: SettingsStream, model: Model) {
    val interval = value.toIntOrNull() ?: return
    if (interval < 0 || interval > 10) {
        return
    }
    stream.maxKeyFrameInterval = interval
    Unit
}

@Composable
private fun KeyFrameIntervalSettingsView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val isLive by model.isLive.collectAsState()
    SettingsSection {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !(stream.enabled && isLive)) {
                    onNavigate("Key frame interval")
                }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextItemLocalizedView(
                name = "Key frame interval",
                value = stream.maxKeyFrameIntervalString(),
            )
        }
    }
}

@Composable
private fun BFramesSettingsView(model: Model = LocalModel.current, stream: SettingsStream) {
    val isLive by model.isLive.collectAsState()
    SettingsToggle(
        title = localized("B-frames"),
        checked = stream.bFrames,
        enabled = !(stream.enabled && isLive),
        onCheckedChange = {
            stream.bFrames = it
            Unit
        },
    )
}

private fun encoderSettings(stream: SettingsStream): VideoEncoderSettings {
    val settings = VideoEncoderSettings()
    settings.updateAdtaptiveResolutionThresholds(stream.adaptiveEncoderResolutionThreashold)
    return settings
}

private fun formatBitrate(value: Long): String {
    return formatBytesPerSecond(value)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdaptiveResolutionThresholdSettingsView(model: Model = LocalModel.current, stream: SettingsStream) {
    val isLive by model.isLive.collectAsState()
    val settings = encoderSettings(stream)
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(localized("Adaptive resolution")) })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            SettingsSection(
                headerText = localized("Thresholds"),
                footer = {
                    Column {
                        SettingsFooterText(
                            "< ${formatBitrate(settings.adaptiveResolution160Threshold.toLong())} \u2192 160p",
                        )
                        SettingsFooterText(
                            "< ${formatBitrate(settings.adaptiveResolution360Threshold.toLong())} \u2192 360p",
                        )
                        SettingsFooterText(
                            "< ${formatBitrate(settings.adaptiveResolution480Threshold.toLong())} \u2192 480p",
                        )
                        SettingsFooterText(
                            "< ${formatBitrate(settings.adaptiveResolution720Threshold.toLong())} \u2192 720p",
                        )
                        SettingsFooterText(
                            "< ${formatBitrate(settings.adaptiveResolution1080Threshold.toLong())} \u2192 1080p",
                        )
                    }
                },
            ) {
                Slider(
                    value = stream.adaptiveEncoderResolutionThreashold.toFloat(),
                    onValueChange = {
                        stream.adaptiveEncoderResolutionThreashold = it.toDouble()
                    },
                    valueRange = 1f..3f,
                    steps = 19,
                    onValueChangeFinished = {
                        Unit
                    },
                    enabled = !(stream.enabled && isLive),
                )
            }
        }
    }
}

@Composable
private fun AdaptiveResolutionSettingsView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val isLive by model.isLive.collectAsState()
    SettingsSection(
        footer = {
            Column {
                SettingsFooterText(
                    localized(
                        "Automatically lower resolution when the available bandwidth is " +
                            "low. Generally gives better image quality at low (< 750 Kbps) bitrates.",
                    ),
                )
                SettingsFooterText("")
                SettingsFooterText(
                    localized(
                        "Warning: OBS typically requires hardware decoding not to crash when enabled.",
                    ),
                )
            }
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate("Adaptive resolution") }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(localized("Adaptive resolution"), modifier = Modifier.weight(1f))
            Switch(
                checked = stream.adaptiveEncoderResolution,
                onCheckedChange = {
                    stream.adaptiveEncoderResolution = it
                    Unit
                },
                enabled = !(stream.enabled && isLive),
            )
        }
    }
}

private fun areTimecodesDisabled(stream: SettingsStream, isLive: Boolean): Boolean {
    val protocol = stream.getProtocol()
    if (protocol != SettingsStreamProtocol.srt && protocol != SettingsStreamProtocol.rist) {
        return true
    }
    if (stream.codec != SettingsStreamCodec.h265hevc) {
        return true
    }
    return stream.enabled && isLive
}

@Composable
private fun StreamTimecodesSettingsView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val isLive by model.isLive.collectAsState()
    val disabled = areTimecodesDisabled(stream, isLive)
    SettingsSection(
        footer = {
            SettingsFooterText(
                localized(
                    "Synchronize multiple streams on your server using timecodes. " +
                        "Timecodes are in UTC and requires H.265/HEVC codec and SRT(LA) or " +
                        "RIST.",
                ),
            )
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !disabled) { onNavigate("Timecodes") }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(localized("Timecodes"), modifier = Modifier.weight(1f))
            Switch(
                checked = stream.timecodesEnabled,
                onCheckedChange = {
                    stream.timecodesEnabled = it
                    if (stream.enabled) {
                        model.reloadNtpClient()
                        model.reloadIngests()
                    }
                },
                enabled = !disabled,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamVideoSettingsView(
    database: Database,
    stream: SettingsStream,
    model: Model = LocalModel.current,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val showAllSettings = database.showAllSettings
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(localized("Video")) })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            ResolutionSettingsView(model = model, stream = stream)
            FpsSettingsView(model = model, stream = stream)
            LowLightBoostSettingsView(model = model, stream = stream)
            CodecSettingsView(model = model, stream = stream)
            if (showAllSettings) {
                RateControlView(model = model, stream = stream)
                BitrateSettingsView(
                    model = model,
                    database = database,
                    stream = stream,
                    onNavigate = onNavigate,
                )
                KeyFrameIntervalSettingsView(
                    model = model,
                    stream = stream,
                    onNavigate = onNavigate,
                )
                BFramesSettingsView(model = model, stream = stream)
                AdaptiveResolutionSettingsView(
                    model = model,
                    stream = stream,
                    onNavigate = onNavigate,
                )
                StreamTimecodesSettingsView(
                    model = model,
                    stream = stream,
                    onNavigate = onNavigate,
                )
            }
        }
    }
}

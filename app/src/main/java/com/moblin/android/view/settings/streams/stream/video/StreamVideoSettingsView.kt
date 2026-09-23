package com.moblin.android.view.settings.streams.stream.video

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.common.various.formatBytesPerSecond
import com.moblin.android.localized
import com.moblin.android.media.haishinkit.codec.video.VideoEncoderSettings
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.FormSlider
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.formFootnoteStyle
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.various.settings.SettingsStreamCodec
import com.moblin.android.various.settings.SettingsStreamH264Profile
import com.moblin.android.various.settings.SettingsStreamProtocol
import com.moblin.android.various.settings.SettingsStreamRateControl
import com.moblin.android.various.settings.SettingsStreamResolution
import com.moblin.android.various.settings.fpss
import com.moblin.android.view.settings.bitratepresets.BitratePresetsSettingsView
import com.moblin.android.view.utils.TextItemLocalizedView
import com.moblin.android.various.model.reloadStreamIfEnabled
import com.moblin.android.various.model.setStreamBitrate
import com.moblin.android.various.model.setStreamFps

@Composable
private fun ResolutionSettingsView(model: Model = LocalModel.current, stream: SettingsStream) {
    val isLive by model.isLive.collectAsState()
    val isRecording by model.isRecording.collectAsState()
    Section {
        Picker(
            title = localized("Resolution"),
            selection = stream.resolution,
            options = SettingsStreamResolution.entries,
            enabled = !(stream.enabled && (isLive || isRecording)),
            text = { it.shortString() },
        ) {
            stream.resolution = it
            model.reloadStreamIfEnabled(stream)
        }
    }
}

@Composable
private fun FpsSettingsView(model: Model = LocalModel.current, stream: SettingsStream) {
    val isLive by model.isLive.collectAsState()
    val isRecording by model.isRecording.collectAsState()
    Section(
        footer = localized("Lower FPS generally gives brighter image in low light conditions."),
    ) {
        Picker(
            title = localized("FPS"),
            selection = stream.fps,
            options = fpss,
            enabled = !(stream.enabled && (isLive || isRecording)),
            text = { it.toString() },
        ) {
            stream.fps = it
            model.reloadStreamIfEnabled(stream)
        }
    }
}

@Composable
private fun LowLightBoostSettingsView(model: Model = LocalModel.current, stream: SettingsStream) {
    Section(
        footer = localized(
            "Enable low light boost to make builtin cameras automatically lower the " +
                "selected FPS for brighter image when dark (if supported).",
        ),
    ) {
        Toggle(
            title = localized("Low light boost (LLB)"),
            isOn = stream.lowLightBoost,
        ) {
            stream.lowLightBoost = it
            model.setStreamFps()
        }
    }
}

@Composable
private fun CodecSettingsView(model: Model = LocalModel.current, stream: SettingsStream) {
    val isLive by model.isLive.collectAsState()
    Section(
        footer = localized(
            "H.265/HEVC generally requires less bandwidth for same image quality. RTMP " +
                "generally only supports H.264/AVC.",
        ),
    ) {
        Picker(
            title = localized("Codec"),
            selection = stream.codec,
            options = SettingsStreamCodec.entries,
            enabled = !(stream.enabled && isLive),
            text = { it.rawValue },
        ) {
            stream.codec = it
            model.reloadStreamIfEnabled(stream)
        }
        if (stream.codec == SettingsStreamCodec.h264avc) {
            Picker(
                title = localized("Profile"),
                selection = stream.h264Profile,
                options = SettingsStreamH264Profile.entries,
                enabled = !(stream.enabled && isLive),
                text = { it.rawValue },
            ) {
                stream.h264Profile = it
                model.reloadStreamIfEnabled(stream)
            }
        }
    }
}

@Composable
private fun RateControlView(model: Model = LocalModel.current, stream: SettingsStream) {
    val isLive by model.isLive.collectAsState()
    Section {
        Picker(
            title = localized("Rate control"),
            selection = stream.rateControl,
            options = SettingsStreamRateControl.entries,
            enabled = !(stream.enabled && isLive),
            text = { it.toString() },
        ) {
            stream.rateControl = it
            model.reloadStreamIfEnabled(stream)
        }
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
    Section(
        footer = localized("About 5-8 Mbps is usually enough for decent image quality."),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SystemImage(
                "speedometer",
                fontSize = 17.sp,
                modifier = Modifier.padding(start = 16.dp),
            )
            Picker(
                title = localized("Bitrate"),
                selection = stream.bitrate,
                options = bitratePresets.map { it.bitrate },
                text = { formatBytesPerSecond(it.toLong()) },
            ) {
                stream.bitrate = it
                if (stream.enabled) {
                    model.setStreamBitrate(stream)
                }
            }
        }
        NavigationLink(title = localized("Bitrate presets")) {
            BitratePresetsSettingsView(database = database)
        }
    }
}

private fun submitMaxKeyFrameInterval(value: String, stream: SettingsStream, model: Model) {
    val interval = value.toIntOrNull() ?: return
    if (interval < 0 || interval > 10) {
        return
    }
    stream.maxKeyFrameInterval = interval
    model.reloadStreamIfEnabled(stream)
}

@Composable
private fun KeyFrameIntervalSettingsView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val isLive by model.isLive.collectAsState()
    Section {
        FormRow(
            onClick = { onNavigate("Key frame interval") },
            enabled = !(stream.enabled && isLive),
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
    Toggle(
        title = localized("B-frames"),
        isOn = stream.bFrames,
        enabled = !(stream.enabled && isLive),
    ) {
        stream.bFrames = it
        model.reloadStreamIfEnabled(stream)
    }
}

private fun encoderSettings(stream: SettingsStream): VideoEncoderSettings {
    val settings = VideoEncoderSettings()
    settings.updateAdtaptiveResolutionThresholds(stream.adaptiveEncoderResolutionThreashold)
    return settings
}

private fun formatBitrate(value: Long): String {
    return formatBytesPerSecond(value)
}

@Composable
private fun AdaptiveResolutionThresholdSettingsView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
) {
    val isLive by model.isLive.collectAsState()
    val palette = formPalette()
    Form(title = localized("Adaptive resolution")) {
        Section(
            header = localized("Thresholds"),
            footerContent = {
                val settings = encoderSettings(stream)
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "< ${formatBitrate(settings.adaptiveResolution160Threshold.toLong())} → 160p",
                        style = formFootnoteStyle,
                        color = palette.secondaryLabel,
                    )
                    Text(
                        "< ${formatBitrate(settings.adaptiveResolution360Threshold.toLong())} → 360p",
                        style = formFootnoteStyle,
                        color = palette.secondaryLabel,
                    )
                    Text(
                        "< ${formatBitrate(settings.adaptiveResolution480Threshold.toLong())} → 480p",
                        style = formFootnoteStyle,
                        color = palette.secondaryLabel,
                    )
                    Text(
                        "< ${formatBitrate(settings.adaptiveResolution720Threshold.toLong())} → 720p",
                        style = formFootnoteStyle,
                        color = palette.secondaryLabel,
                    )
                    Text(
                        "< ${formatBitrate(settings.adaptiveResolution1080Threshold.toLong())} → 1080p",
                        style = formFootnoteStyle,
                        color = palette.secondaryLabel,
                    )
                }
            },
        ) {
            FormSlider(
                value = stream.adaptiveEncoderResolutionThreashold.toFloat(),
                onValueChange = {
                    stream.adaptiveEncoderResolutionThreashold = it.toDouble()
                },
                valueRange = 1f..3f,
                enabled = !(stream.enabled && isLive),
                onValueChangeFinished = {
                    model.reloadStreamIfEnabled(stream)
                },
            )
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
    val palette = formPalette()
    Section(
        footerContent = {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    localized(
                        "Automatically lower resolution when the available bandwidth is " +
                            "low. Generally gives better image quality at low (< 750 Kbps) bitrates.",
                    ),
                    style = formFootnoteStyle,
                    color = palette.secondaryLabel,
                )
                Text(
                    "",
                    style = formFootnoteStyle,
                    color = palette.secondaryLabel,
                )
                Text(
                    localized(
                        "Warning: OBS typically requires hardware decoding not to crash when enabled.",
                    ),
                    style = formFootnoteStyle,
                    color = palette.secondaryLabel,
                )
            }
        },
    ) {
        NavigationLink(
            destination = {
                AdaptiveResolutionThresholdSettingsView(model = model, stream = stream)
            },
        ) {
            Toggle(
                title = localized("Adaptive resolution"),
                isOn = stream.adaptiveEncoderResolution,
                enabled = !(stream.enabled && isLive),
            ) {
                stream.adaptiveEncoderResolution = it
                model.reloadStreamIfEnabled(stream)
            }
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
    Section(
        footer = localized(
            "Synchronize multiple streams on your server using timecodes. " +
                "Timecodes are in UTC and requires H.265/HEVC codec and SRT(LA) or RIST.",
        ),
    ) {
        FormRow(
            onClick = { onNavigate("Timecodes") },
            enabled = !disabled,
        ) {
            Toggle(
                title = localized("Timecodes"),
                isOn = stream.timecodesEnabled,
                enabled = !disabled,
            ) {
                stream.timecodesEnabled = it
                if (stream.enabled) {
                    model.reloadNtpClient()
                    model.reloadIngests()
                }
            }
        }
    }
}

@Composable
fun StreamVideoSettingsView(
    database: Database,
    stream: SettingsStream,
    model: Model = LocalModel.current,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Form(title = localized("Video")) {
        ResolutionSettingsView(model = model, stream = stream)
        FpsSettingsView(model = model, stream = stream)
        LowLightBoostSettingsView(model = model, stream = stream)
        CodecSettingsView(model = model, stream = stream)
        if (database.showAllSettings) {
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

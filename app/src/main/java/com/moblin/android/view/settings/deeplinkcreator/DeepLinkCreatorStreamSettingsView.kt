package com.moblin.android.view.settings.deeplinkcreator

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.common.various.cleanUrl
import com.moblin.android.common.various.formatBytesPerSecond
import com.moblin.android.common.various.formatShortDuration
import com.moblin.android.common.various.isValidWebSocketUrl
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.FormSlider
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.DeepLinkCreator
import com.moblin.android.various.settings.DeepLinkCreatorStream
import com.moblin.android.various.settings.DeepLinkCreatorStreamAudio
import com.moblin.android.various.settings.DeepLinkCreatorStreamKick
import com.moblin.android.various.settings.DeepLinkCreatorStreamObs
import com.moblin.android.various.settings.DeepLinkCreatorStreamSrt
import com.moblin.android.various.settings.DeepLinkCreatorStreamTwitch
import com.moblin.android.various.settings.DeepLinkCreatorStreamVideo
import com.moblin.android.various.settings.SettingsDnsLookupStrategy
import com.moblin.android.various.settings.SettingsStreamCodec
import com.moblin.android.various.settings.SettingsStreamResolution
import com.moblin.android.various.settings.fpss
import com.moblin.android.view.settings.streams.stream.KickLogoAndNameView
import com.moblin.android.view.settings.streams.stream.TwitchLogoAndNameView
import com.moblin.android.view.utils.DraggableItemTextView
import com.moblin.android.view.utils.NameEditView
import com.moblin.android.view.utils.TextEditNavigationView
import kotlin.math.ceil

private fun submitMaxKeyFrameInterval(video: DeepLinkCreatorStreamVideo, value: String) {
    val interval = value.toIntOrNull() ?: return
    if (interval < 0 || interval > 10) {
        return
    }
    video.maxKeyFrameInterval = interval
}

@Composable
private fun DeepLinkCreatorStreamVideoView(
    model: Model = LocalModel.current,
    video: DeepLinkCreatorStreamVideo,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val bitratePresets = model.database.bitratePresets
    Form(title = localized("Video")) {
        Section {
            Picker(
                title = localized("Resolution"),
                selection = video.resolution,
                options = SettingsStreamResolution.entries,
                text = { it.shortString() },
                onChange = { video.resolution = it },
            )
            Picker(
                title = localized("FPS"),
                selection = video.fps,
                options = fpss,
                onChange = { video.fps = it },
            )
            Picker(
                title = localized("Codec"),
                selection = video.codec,
                options = SettingsStreamCodec.entries,
                text = { it.rawValue },
                onChange = { video.codec = it },
            )
            Picker(
                title = localized("Bitrate"),
                selection = video.bitrate,
                options = bitratePresets.map { it.bitrate },
                text = { formatBytesPerSecond(speed = it.toLong()) },
                onChange = { video.bitrate = it },
            )
            TextEditNavigationView(
                title = localized("Key frame interval"),
                value = video.maxKeyFrameInterval.toString(),
                onSubmit = { submitMaxKeyFrameInterval(video, it) },
                footers = listOf(
                    localized(
                        "Maximum key frame interval in seconds. Set to 0 for automatic.",
                    ),
                ),
                valueFormat = { formatShortDuration(seconds = it.toIntOrNull() ?: 0) },
            )
            Toggle(
                title = localized("B-frames"),
                isOn = video.bFrames,
                onChange = { video.bFrames = it },
            )
        }
    }
}

private fun calcBitrate(bitrateFloat: Float): Int =
    ceil(bitrateFloat * 1000.0).toInt()

@Composable
private fun DeepLinkCreatorStreamAudioView(audio: DeepLinkCreatorStreamAudio) {
    var bitrateFloat by remember { mutableStateOf(audio.bitrateFloat) }
    Form(title = localized("Audio")) {
        Section {
            FormRow {
                FormSlider(
                    value = bitrateFloat,
                    onValueChange = {
                        bitrateFloat = it
                    },
                    modifier = Modifier.weight(1f),
                    valueRange = 32f..320f,
                    onValueChangeFinished = {
                        audio.bitrate = calcBitrate(bitrateFloat)
                    },
                )
                Box(
                    modifier = Modifier.width(90.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = formatBytesPerSecond(speed = calcBitrate(bitrateFloat).toLong()),
                    )
                }
            }
        }
    }
}

private fun changeLatency(value: String): String? {
    if (value.toIntOrNull() != null) {
        return null
    }
    return localized("Not a number")
}

private fun submitLatency(srt: DeepLinkCreatorStreamSrt, value: String) {
    val latency = value.toIntOrNull() ?: return
    if (latency < 0) {
        return
    }
    srt.latency = latency
}

@Composable
private fun DeepLinkCreatorStreamSrtView(srt: DeepLinkCreatorStreamSrt) {
    Form(title = localized("SRT(LA)")) {
        Section {
            TextEditNavigationView(
                title = localized("Latency"),
                value = srt.latency.toString(),
                onChange = { changeLatency(it) },
                onSubmit = { submitLatency(srt, it) },
                valueFormat = { "$it ms" },
            )
            Toggle(
                title = localized("Adaptive bitrate"),
                isOn = srt.adaptiveBitrateEnabled,
                onChange = { srt.adaptiveBitrateEnabled = it },
            )
            Picker(
                title = localized("DNS lookup strategy"),
                selection = srt.dnsLookupStrategy,
                options = SettingsDnsLookupStrategy.entries,
                text = { it.rawValue },
                onChange = { srt.dnsLookupStrategy = it },
            )
        }
    }
}

private fun changeWebSocketUrl(value: String): String? =
    isValidWebSocketUrl(value = cleanUrl(value = value))

private fun submitWebSocketUrl(
    model: Model,
    obs: DeepLinkCreatorStreamObs,
    value: String,
) {
    val url = cleanUrl(value = value)
    val message = isValidWebSocketUrl(value = url)
    if (message != null) {
        model.makeErrorToast(title = message)
        return
    }
    obs.webSocketUrl = url
}

@Composable
private fun DeepLinkCreatorStreamObsView(
    model: Model = LocalModel.current,
    obs: DeepLinkCreatorStreamObs,
) {
    Form(title = localized("OBS remote control")) {
        Section(
            header = localized("WebSocket"),
            footer = localized(
                "Source name is the name of the Source in OBS that receives the stream from Moblin.",
            ),
        ) {
            TextEditNavigationView(
                title = localized("URL"),
                value = obs.webSocketUrl,
                onChange = { changeWebSocketUrl(it) },
                onSubmit = { submitWebSocketUrl(model, obs, it) },
                footers = listOf(localized("For example ws://232.32.45.332:4567.")),
            )
            TextEditNavigationView(
                title = localized("Password"),
                value = obs.webSocketPassword,
                onSubmit = { obs.webSocketPassword = it },
                sensitive = true,
            )
        }
    }
}

@Composable
private fun DeepLinkCreatorStreamTwitchView(twitch: DeepLinkCreatorStreamTwitch) {
    Form(title = localized("Twitch")) {
        Section {
            TextEditNavigationView(
                title = localized("Channel name"),
                value = twitch.channelName,
                onSubmit = { twitch.channelName = it },
                capitalize = true,
            )
            TextEditNavigationView(
                title = localized("Channel id"),
                value = twitch.channelId,
                onSubmit = { twitch.channelId = it },
            )
        }
    }
}

@Composable
private fun DeepLinkCreatorStreamKickView(kick: DeepLinkCreatorStreamKick) {
    Form(title = localized("Kick")) {
        Section {
            TextEditNavigationView(
                title = localized("Channel name"),
                value = kick.channelName,
                onSubmit = { kick.channelName = it },
                capitalize = true,
            )
        }
    }
}

@Composable
fun DeepLinkCreatorStreamSettingsView(
    deepLinkCreator: DeepLinkCreator,
    stream: DeepLinkCreatorStream,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val uri = remember(stream.url) {
        runCatching { java.net.URI(stream.url) }.getOrNull()
    }
    val scheme = uri?.scheme?.lowercase()
    val isSrt = scheme == "srt" || scheme == "srtla"
    NavigationLink(
        destination = {
            Form(title = localized("Stream")) {
                Section(header = localized("Media")) {
                    NameEditView(
                        name = stream.name,
                        existingNames = deepLinkCreator.streams,
                        onNameChange = { stream.name = it },
                    )
                    TextEditNavigationView(
                        title = localized("URL"),
                        value = stream.url,
                        onSubmit = { stream.url = it },
                    )
                    NavigationLink(localized("Video")) {
                        DeepLinkCreatorStreamVideoView(video = stream.video)
                    }
                    NavigationLink(localized("Audio")) {
                        DeepLinkCreatorStreamAudioView(audio = stream.audio)
                    }
                    if (isSrt) {
                        NavigationLink(localized("SRT(LA)")) {
                            DeepLinkCreatorStreamSrtView(srt = stream.srt)
                        }
                    }
                }
                Section(header = localized("Chat and viewers")) {
                    NavigationLink(
                        destination = {
                            DeepLinkCreatorStreamTwitchView(twitch = stream.twitch)
                        },
                    ) {
                        TwitchLogoAndNameView()
                    }
                    NavigationLink(
                        destination = {
                            DeepLinkCreatorStreamKickView(kick = stream.kick)
                        },
                    ) {
                        KickLogoAndNameView()
                    }
                }
                Section {
                    NavigationLink(localized("OBS remote control")) {
                        DeepLinkCreatorStreamObsView(obs = stream.obs)
                    }
                }
                Section {
                    Toggle(
                        title = localized("Selected"),
                        isOn = stream.selected,
                        onChange = { stream.selected = it },
                    )
                }
            }
        },
    ) {
        DraggableItemTextView(name = stream.name)
    }
}

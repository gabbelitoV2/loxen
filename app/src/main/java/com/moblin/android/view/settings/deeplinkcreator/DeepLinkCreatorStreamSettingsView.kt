package com.moblin.android.view.settings.deeplinkcreator

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.cleanUrl
import com.moblin.android.common.various.formatBytesPerSecond
import com.moblin.android.common.various.formatShortDuration
import com.moblin.android.common.various.isValidWebSocketUrl
import com.moblin.android.localized
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
import com.moblin.android.view.utils.TextItemView
import kotlin.math.ceil
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

private fun submitMaxKeyFrameInterval(video: DeepLinkCreatorStreamVideo, value: String) {
    val interval = value.toIntOrNull() ?: return
    if (interval < 0 || interval > 10) {
        return
    }
    video.maxKeyFrameInterval = interval
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeepLinkCreatorStreamVideoView(
    model: Model = LocalModel.current,
    video: DeepLinkCreatorStreamVideo,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val bitratePresets = model.database.bitratePresets
    val resolution = video.resolution
    val fps = video.fps
    val codec = video.codec
    val bitrate = video.bitrate
    val maxKeyFrameInterval = video.maxKeyFrameInterval
    val bFrames = video.bFrames

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Video") })
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                ) {
                    OutlinedTextField(
                        value = resolution.shortString(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Resolution") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        SettingsStreamResolution.entries.forEach { value ->
                            DropdownMenuItem(
                                text = { Text(value.shortString()) },
                                onClick = {
                                    video.resolution = value
                                    expanded = false
                                },
                            )
                        }
                    }
                }
            }
            item {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                ) {
                    OutlinedTextField(
                        value = fps.toString(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("FPS") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        fpss.forEach { value ->
                            DropdownMenuItem(
                                text = { Text(value.toString()) },
                                onClick = {
                                    video.fps = value
                                    expanded = false
                                },
                            )
                        }
                    }
                }
            }
            item {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                ) {
                    OutlinedTextField(
                        value = codec.rawValue,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Codec") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        SettingsStreamCodec.entries.forEach { value ->
                            DropdownMenuItem(
                                text = { Text(value.rawValue) },
                                onClick = {
                                    video.codec = value
                                    expanded = false
                                },
                            )
                        }
                    }
                }
            }
            item {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                ) {
                    OutlinedTextField(
                        value = formatBytesPerSecond(speed = bitrate.toLong()),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Bitrate") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        bitratePresets.forEach { preset ->
                            DropdownMenuItem(
                                text = {
                                    Text(formatBytesPerSecond(speed = preset.bitrate.toLong()))
                                },
                                onClick = {
                                    video.bitrate = preset.bitrate
                                    expanded = false
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
                        .clickable { onNavigate("keyFrameInterval") },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextItemView(
                        name = "Key frame interval",
                        value = formatShortDuration(seconds = maxKeyFrameInterval.toInt()),
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "B-frames",
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = bFrames,
                        onCheckedChange = { video.bFrames = it },
                    )
                }
            }
        }
    }
}

private fun calcBitrate(audio: DeepLinkCreatorStreamAudio): Int =
    ceil(audio.bitrateFloat * 1000.0).toInt()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeepLinkCreatorStreamAudioView(audio: DeepLinkCreatorStreamAudio) {
    var bitrateFloat by remember { mutableStateOf(audio.bitrateFloat) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Audio") })
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Slider(
                        value = bitrateFloat,
                        onValueChange = { bitrateFloat = it },
                        valueRange = 32f..320f,
                        steps = 8,
                        onValueChangeFinished = {
                            audio.bitrate = ceil(bitrateFloat * 1000.0).toInt()
                        },
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = formatBytesPerSecond(speed = ceil(bitrateFloat * 1000.0).toInt().toLong()),
                        modifier = Modifier.width(90.dp),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeepLinkCreatorStreamSrtView(srt: DeepLinkCreatorStreamSrt) {
    val latency = srt.latency
    val adaptiveBitrateEnabled = srt.adaptiveBitrateEnabled
    val dnsLookupStrategy = srt.dnsLookupStrategy

    LaunchedEffect(dnsLookupStrategy) {
        srt.dnsLookupStrategy = dnsLookupStrategy
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("SRT(LA)") })
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item {
                TextEditNavigationView(
                    title = localized("Latency"),
                    value = latency.toString(),
                    onChange = { changeLatency(it) },
                    onSubmit = { submitLatency(srt, it) },
                    valueFormat = { "$it ms" },
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Adaptive bitrate",
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = adaptiveBitrateEnabled,
                        onCheckedChange = { srt.adaptiveBitrateEnabled = it },
                    )
                }
            }
            item {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                ) {
                    OutlinedTextField(
                        value = dnsLookupStrategy.rawValue,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("DNS lookup strategy") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        SettingsDnsLookupStrategy.entries.forEach { strategy ->
                            DropdownMenuItem(
                                text = { Text(strategy.rawValue) },
                                onClick = {
                                    srt.dnsLookupStrategy = strategy
                                    expanded = false
                                },
                            )
                        }
                    }
                }
            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeepLinkCreatorStreamObsView(
    model: Model = LocalModel.current,
    obs: DeepLinkCreatorStreamObs,
) {
    val webSocketUrl = obs.webSocketUrl
    val webSocketPassword = obs.webSocketPassword

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("OBS remote control") })
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item {
                Text(
                    text = "WebSocket",
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            item {
                TextEditNavigationView(
                    title = localized("URL"),
                    value = webSocketUrl,
                    onChange = { changeWebSocketUrl(it) },
                    onSubmit = { submitWebSocketUrl(model, obs, it) },
                    footers = listOf(localized("For example ws://232.32.45.332:4567.")),
                )
            }
            item {
                TextEditNavigationView(
                    title = localized("Password"),
                    value = webSocketPassword,
                    onSubmit = { obs.webSocketPassword = it },
                    sensitive = true,
                )
            }
            item {
                Text(
                    text = "Source name is the name of the Source in OBS that receives the stream from Moblin.",
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeepLinkCreatorStreamTwitchView(twitch: DeepLinkCreatorStreamTwitch) {
    val channelName = twitch.channelName
    val channelId = twitch.channelId

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Twitch") })
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item {
                TextEditNavigationView(
                    title = localized("Channel name"),
                    value = channelName,
                    onSubmit = { twitch.channelName = it },
                    capitalize = true,
                )
            }
            item {
                TextEditNavigationView(
                    title = localized("Channel id"),
                    value = channelId,
                    onSubmit = { twitch.channelId = it },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeepLinkCreatorStreamKickView(kick: DeepLinkCreatorStreamKick) {
    val channelName = kick.channelName

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Kick") })
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item {
                TextEditNavigationView(
                    title = localized("Channel name"),
                    value = channelName,
                    onSubmit = { kick.channelName = it },
                    capitalize = true,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeepLinkCreatorStreamSettingsView(
    deepLinkCreator: DeepLinkCreator,
    stream: DeepLinkCreatorStream,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val streams = deepLinkCreator.streams
    val name = stream.name
    val url = stream.url
    val selected = stream.selected

    val uri = remember(url) {
        runCatching { java.net.URI(url) }.getOrNull()
    }
    val isSrt = uri != null && (uri.scheme == "srt" || uri.scheme == "srtla")

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Stream") })
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("stream") },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DraggableItemTextView(name = name)
                }
            }
            item {
                Text(
                    text = "Media",
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            item {
                NameEditView(
                    name = name,
                    existingNames = streams,
                    onNameChange = { stream.name = it },
                )
            }
            item {
                TextEditNavigationView(
                    title = localized("URL"),
                    value = url,
                    onSubmit = { stream.url = it },
                )
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("video") },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Video")
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("audio") },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Audio")
                }
            }
            if (isSrt) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate("srt") },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("SRT(LA)")
                    }
                }
            }
            item {
                Text(
                    text = "Chat and viewers",
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("twitch") },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TwitchLogoAndNameView()
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("kick") },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    KickLogoAndNameView()
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("obs") },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("OBS remote control")
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Selected",
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = selected,
                        onCheckedChange = { stream.selected = it },
                    )
                }
            }
        }
    }
}

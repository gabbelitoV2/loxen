package com.moblin.android.view.settings.streams.stream

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.format
import com.moblin.android.common.various.formatShortDuration
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.various.settings.SettingsStreamProtocol
import com.moblin.android.various.utils.isMac
import com.moblin.android.view.settings.streams.stream.multistreaming.StreamMultiStreamingSettingsView
import com.moblin.android.view.utils.IconAndTextSettingView
import com.moblin.android.view.utils.NameEditView
import com.moblin.android.view.utils.TextItemLocalizedView
import kotlin.time.Duration
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

@Composable
private fun PlatformLogoAndNameView(
    logo: String,
    name: String,
    channel: String = "",
    scale: Double = 1.0,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val context = LocalContext.current
        val logoId = context.resources.getIdentifier(logo, "drawable", context.packageName)
        Image(
            painter = painterResource(id = logoId),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(width = (30 * scale).dp, height = (25 * scale).dp),
        )
        if (channel.isEmpty()) {
            Text(name)
        } else {
            Text("$name ($channel)")
        }
    }
}

@Composable
fun TwitchLogoAndNameView(channel: String = "") {
    PlatformLogoAndNameView(logo = "TwitchLogo", name = localized("Twitch"), channel = channel)
}

@Composable
fun KickLogoAndNameView(channel: String = "") {
    PlatformLogoAndNameView(logo = "KickLogo", name = localized("Kick"), channel = channel)
}

@Composable
fun YouTubeLogoAndNameView(handle: String = "") {
    PlatformLogoAndNameView(logo = "YouTubeLogo", name = localized("YouTube"), channel = handle)
}

@Composable
fun OpenStreamingPlatformLogoAndNameView() {
    PlatformLogoAndNameView(
        logo = "OpenStreamingPlatform",
        name = localized("Open Streaming Platform"),
    )
}

@Composable
fun SoopLogoAndNameView(channel: String = "") {
    PlatformLogoAndNameView(logo = "SoopLogo", name = localized("SOOP"), channel = channel)
}

@Composable
fun ObsLogoAndNameView() {
    PlatformLogoAndNameView(logo = "ObsLogo", name = localized("OBS"))
}

@Composable
fun DiscordLogoAndNameView() {
    PlatformLogoAndNameView(logo = "DiscordLogo", name = localized("Discord"))
}

@Composable
fun TtsMonsterLogoAndNameView() {
    PlatformLogoAndNameView(logo = "TtsMonster", name = localized("TTS.Monster"))
}

@Composable
fun MobcamLogoAndNameView() {
    PlatformLogoAndNameView(logo = "MobcamLogo", name = localized("Mobcam"), scale = 1.25)
}

@Composable
fun GithubLogoAndNameView() {
    val darkTheme = isSystemInDarkTheme()
    PlatformLogoAndNameView(
        logo = if (darkTheme) "GithubWhiteLogo" else "GithubLogo",
        name = localized("Github"),
    )
}

@Composable
fun GrayTextView(text: String) {
    Text(text = text, color = Color.Gray, maxLines = 1)
}

@Composable
fun TokenExpiresInView(expiresIn: Duration?) {
    if (expiresIn != null) {
        Text("Expires in ${expiresIn.format()}.")
    }
}

@Composable
fun StreamPlatformsSettingsView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val twitchChannelName = stream.twitchChannelName
    val kickChannelName = stream.kickChannelName
    val youTubeHandle = stream.youTubeHandle
    val soopChannelName = stream.soopChannelName

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate("StreamTwitchSettings") },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TwitchLogoAndNameView()
            Spacer(Modifier.weight(1f))
            GrayTextView(text = twitchChannelName)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate("StreamKickSettings") },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KickLogoAndNameView()
            Spacer(Modifier.weight(1f))
            GrayTextView(text = kickChannelName)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate("StreamYouTubeSettings") },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            YouTubeLogoAndNameView()
            Spacer(Modifier.weight(1f))
            GrayTextView(text = youTubeHandle)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate("StreamSoopSettings") },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SoopLogoAndNameView()
            Spacer(Modifier.weight(1f))
            GrayTextView(text = soopChannelName)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate("StreamOpenStreamingPlatformSettings") },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OpenStreamingPlatformLogoAndNameView()
        }
    }
}

@Composable
fun BackgroundStreamingFooterView() {
    Text(localized("Live stream and record when the app is in background mode."))
}

@Composable
fun AutoGoLiveFooterView() {
    Text(localized("Automatically go live when the app enters foreground."))
}

@Composable
private fun BackgroundStreamingView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val backgroundStreaming = stream.backgroundStreaming
    val enabled = stream.enabled

    LaunchedEffect(backgroundStreaming) {
        if (enabled) {
            TODO("updatePictureInPicture")
        }
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate("BackgroundStreaming") },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(localized("Background streaming"), modifier = Modifier.weight(1f))
            Switch(
                checked = backgroundStreaming,
                onCheckedChange = { stream.backgroundStreaming = it },
            )
        }
        BackgroundStreamingFooterView()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamSettingsView(
    model: Model = LocalModel.current,
    database: Database,
    stream: SettingsStream,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val showAllSettings = database.showAllSettings
    val streams = database.streams
    val streamName = stream.name
    val streamUrl = stream.url
    val enabled = stream.enabled
    val portrait = stream.portrait
    val obsWebSocketEnabled = stream.obsWebSocketEnabled
    val realtimeIrlEnabled = stream.realtimeIrlEnabled
    val estimatedViewerDelay = stream.estimatedViewerDelay
    val autoGoLive = stream.autoGoLive
    val isLive by model.isLive.collectAsState()
    val isRecording by model.isRecording.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text(localized("Stream")) }) },
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding)) {
            item {
                NameEditView(
                    name = streamName,
                    onNameChange = { stream.name = it },
                    existingNames = streams,
                )
            }
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(text = localized("Destination"), style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate("StreamUrlSettings") },
                    ) {
                        TextItemLocalizedView(name = "URL", value = streamUrl, sensitive = true)
                    }
                    if (showAllSettings) {
                        when (stream.getProtocol()) {
                            SettingsStreamProtocol.srt -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onNavigate("StreamSrtSettings") },
                                ) {
                                    Text(localized("SRT(LA)"))
                                }
                            }
                            SettingsStreamProtocol.rtmp -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onNavigate("StreamRtmpSettings") },
                                ) {
                                    Text(localized("RTMP"))
                                }
                                StreamMultiStreamingSettingsView(
                                    multiStreaming = stream.multiStreaming,
                                    onNavigate = onNavigate,
                                )
                            }
                            SettingsStreamProtocol.rist -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onNavigate("StreamRistSettings") },
                                ) {
                                    Text(localized("RIST"))
                                }
                            }
                            SettingsStreamProtocol.whip -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onNavigate("StreamWhipSettings") },
                                ) {
                                    Text(localized("WHIP"))
                                }
                            }
                            SettingsStreamProtocol.mobcam -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onNavigate("StreamMobcamSettings") },
                                ) {
                                    Text(localized("Mobcam"))
                                }
                            }
                            else -> Unit
                        }
                    }
                }
            }
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(text = localized("Media"), style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate("StreamVideoSettings") },
                    ) {
                        Text(localized("Video"))
                    }
                    if (showAllSettings) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigate("StreamAudioSettings") },
                        ) {
                            Text(localized("Audio"))
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigate("StreamRecordingSettings") },
                        ) {
                            IconAndTextSettingView(image = "record.circle", text = "Recording")
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate("StreamReplaySettings") },
                    ) {
                        IconAndTextSettingView(image = "play", text = "Replay")
                    }
                    if (showAllSettings) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigate("StreamSnapshotSettings") },
                        ) {
                            IconAndTextSettingView(image = "camera.aperture", text = "Snapshot")
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigate("StreamPreviewStreamSettings") },
                        ) {
                            IconAndTextSettingView(image = "video.circle", text = "Preview stream")
                        }
                    }
                    if (!isMac()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(localized("Portrait"), modifier = Modifier.weight(1f))
                            Switch(
                                checked = portrait,
                                onCheckedChange = { stream.portrait = it },
                                enabled = !(enabled && (isLive || isRecording)),
                            )
                        }
                        LaunchedEffect(portrait) {
                            if (enabled) {
                                TODO("setCurrentStream")
                                TODO("reloadStream")
                                TODO("resetSelectedScene")
                                model.updateOrientation()
                                model.updateOrientationLock()
                            }
                        }
                    }
                }
            }
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = localized("Streaming platforms"),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    StreamPlatformsSettingsView(
                        model = model,
                        stream = stream,
                        onNavigate = onNavigate,
                    )
                }
            }
            if (!isMac()) {
                item {
                    BackgroundStreamingView(
                        model = model,
                        stream = stream,
                        onNavigate = onNavigate,
                    )
                }
            }
            if (stream.getProtocol() == SettingsStreamProtocol.mobcam) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(localized("Auto go live"), modifier = Modifier.weight(1f))
                            Switch(
                                checked = autoGoLive,
                                onCheckedChange = { stream.autoGoLive = it },
                            )
                        }
                        AutoGoLiveFooterView()
                    }
                }
            }
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate("StreamObsRemoteControlSettings") },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(localized("OBS remote control"), modifier = Modifier.weight(1f))
                        Switch(
                            checked = obsWebSocketEnabled,
                            onCheckedChange = { stream.obsWebSocketEnabled = it },
                        )
                    }
                    LaunchedEffect(obsWebSocketEnabled) {
                        if (enabled) {
                            TODO("obsWebSocketEnabledUpdated")
                        }
                    }
                    if (showAllSettings) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigate("GoLiveNotificationSettings") },
                        ) {
                            Text(localized("Go live notification"))
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigate("StreamRealtimeIrlSettings") },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(localized("RealtimeIRL"), modifier = Modifier.weight(1f))
                            Switch(
                                checked = realtimeIrlEnabled,
                                onCheckedChange = { value ->
                                    stream.realtimeIrlEnabled = value
                                    if (enabled) {
                                        TODO("reloadLocation")
                                    }
                                },
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate("StreamEmotesSettings") },
                    ) {
                        Text(localized("Emotes"))
                    }
                }
            }
            if (showAllSettings) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigate("EstimatedViewerDelay") },
                        ) {
                            TextItemLocalizedView(
                                name = "Estimated viewer delay",
                                value = formatShortDuration(estimatedViewerDelay.toInt()),
                            )
                        }
                        Text(
                            localized(
                                "Estimated viewer delay, for example used to make it easier to " +
                                    "take snapshots using the chat bot. It does not delay the stream."
                            )
                        )
                    }
                }
            }
        }
    }
}

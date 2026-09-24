package com.moblin.android.view.settings.streams.stream

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.common.various.format
import com.moblin.android.common.various.formatOneDecimal
import com.moblin.android.common.various.formatShortDuration
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.IosSwitch
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.various.settings.SettingsStreamProtocol
import com.moblin.android.various.utils.isMac
import com.moblin.android.view.settings.streams.stream.audio.StreamAudioSettingsView
import com.moblin.android.view.settings.streams.stream.chat.StreamEmotesSettingsView
import com.moblin.android.view.settings.streams.stream.golivenotification.GoLiveNotificationSettingsView
import com.moblin.android.view.settings.streams.stream.kick.StreamKickSettingsView
import com.moblin.android.view.settings.streams.stream.mobcam.StreamMobcamSettingsView
import com.moblin.android.view.settings.streams.stream.multistreaming.StreamMultiStreamingSettingsView
import com.moblin.android.view.settings.streams.stream.obsremotecontrol.StreamObsRemoteControlSettingsView
import com.moblin.android.view.settings.streams.stream.openstreamingplatform.StreamOpenStreamingPlatformSettingsView
import com.moblin.android.view.settings.streams.stream.previewstream.StreamPreviewStreamSettingsView
import com.moblin.android.view.settings.streams.stream.realtimeirl.StreamRealtimeIrlSettingsView
import com.moblin.android.view.settings.streams.stream.recording.StreamRecordingSettingsView
import com.moblin.android.view.settings.streams.stream.replay.StreamReplaySettingsView
import com.moblin.android.view.settings.streams.stream.rist.StreamRistSettingsView
import com.moblin.android.view.settings.streams.stream.rtmp.StreamRtmpSettingsView
import com.moblin.android.view.settings.streams.stream.snapshot.StreamSnapshotSettingsView
import com.moblin.android.view.settings.streams.stream.soop.StreamSoopSettingsView
import com.moblin.android.view.settings.streams.stream.srt.StreamSrtSettingsView
import com.moblin.android.view.settings.streams.stream.twitch.StreamTwitchSettingsView
import com.moblin.android.view.settings.streams.stream.url.StreamUrlSettingsView
import com.moblin.android.view.settings.streams.stream.video.StreamVideoSettingsView
import com.moblin.android.view.settings.streams.stream.whip.StreamWhipSettingsView
import com.moblin.android.view.settings.streams.stream.youtube.StreamYouTubeSettingsView
import com.moblin.android.view.utils.IconAndTextSettingView
import com.moblin.android.view.utils.NameEditView
import com.moblin.android.view.utils.TextEditView
import com.moblin.android.view.utils.TextItemLocalizedView
import kotlin.time.Duration
import com.moblin.android.various.model.obsWebSocketEnabledUpdated
import com.moblin.android.various.model.reloadLocation
import com.moblin.android.various.model.reloadStream
import com.moblin.android.various.model.resetSelectedScene
import com.moblin.android.various.model.setCurrentStream
import com.moblin.android.various.model.updatePictureInPicture
import com.moblin.android.platform.swiftui.AssetImage

@Composable
private fun PlatformLogoAndNameView(
    logo: String,
    name: String,
    channel: String = "",
    scale: Double = 1.0,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AssetImage(
            name = logo,
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
    Text(
        text = text,
        color = formPalette().gray,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun TokenExpiresInView(expiresIn: Duration?) {
    if (expiresIn != null) {
        Text(localized("Expires in") + " " + expiresIn.format() + ".")
    }
}

@Composable
fun StreamPlatformsSettingsView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
) {
    NavigationLink(destination = {
        StreamTwitchSettingsView(stream = stream, loggedInInitial = stream.twitchLoggedIn)
    }) {
        TwitchLogoAndNameView()
        Spacer(Modifier.weight(1f))
        GrayTextView(text = stream.twitchChannelName)
    }
    NavigationLink(destination = {
        StreamKickSettingsView(stream = stream)
    }) {
        KickLogoAndNameView()
        Spacer(Modifier.weight(1f))
        GrayTextView(text = stream.kickChannelName)
    }
    NavigationLink(destination = {
        StreamYouTubeSettingsView(debug = model.database.debug, stream = stream)
    }) {
        YouTubeLogoAndNameView()
        Spacer(Modifier.weight(1f))
        GrayTextView(text = stream.youTubeHandle)
    }
    NavigationLink(destination = {
        StreamSoopSettingsView(stream = stream)
    }) {
        SoopLogoAndNameView()
        Spacer(Modifier.weight(1f))
        GrayTextView(text = stream.soopChannelName)
    }
    NavigationLink(destination = {
        StreamOpenStreamingPlatformSettingsView(stream = stream)
    }) {
        OpenStreamingPlatformLogoAndNameView()
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
) {
    Section(footerContent = { BackgroundStreamingFooterView() }) {
        NavigationLink(destination = {
            Form(title = localized("Background streaming")) {
                Section(
                    footer = localized(
                        "Make built-in and USB cameras not freeze in background mode."
                    )
                ) {
                    Toggle(
                        title = localized("Show PiP"),
                        isOn = stream.backgroundStreamingPiP,
                        onChange = { value ->
                            stream.backgroundStreamingPiP = value
                            if (stream.enabled) {
                                model.updatePictureInPicture()
                            }
                        },
                    )
                }
            }
        }) {
            Text(localized("Background streaming"), modifier = Modifier.weight(1f))
            IosSwitch(
                checked = stream.backgroundStreaming,
                onCheckedChange = { value ->
                    stream.backgroundStreaming = value
                    if (stream.enabled) {
                        model.updatePictureInPicture()
                    }
                },
            )
        }
    }
}

@Composable
fun StreamSettingsView(
    model: Model = LocalModel.current,
    database: Database,
    stream: SettingsStream,
) {
    val isLive by model.isLive.collectAsState()
    val isRecording by model.isRecording.collectAsState()

    Form(title = localized("Stream")) {
        Section {
            NameEditView(
                name = stream.name,
                onNameChange = { stream.name = it },
                existingNames = database.streams,
            )
        }
        Section(header = localized("Destination")) {
            NavigationLink(destination = {
                StreamUrlSettingsView(stream = stream)
            }) {
                TextItemLocalizedView(name = "URL", value = stream.url, sensitive = true)
            }
            if (database.showAllSettings) {
                when (stream.getProtocol()) {
                    SettingsStreamProtocol.srt -> NavigationLink(destination = {
                        StreamSrtSettingsView(stream = stream, srt = stream.srt)
                    }) {
                        Text(localized("SRT(LA)"))
                    }
                    SettingsStreamProtocol.rtmp -> {
                        NavigationLink(destination = {
                            StreamRtmpSettingsView(stream = stream)
                        }) {
                            Text(localized("RTMP"))
                        }
                        StreamMultiStreamingSettingsView(
                            stream = stream,
                            multiStreaming = stream.multiStreaming,
                        )
                    }
                    SettingsStreamProtocol.rist -> NavigationLink(destination = {
                        StreamRistSettingsView(stream = stream)
                    }) {
                        Text(localized("RIST"))
                    }
                    SettingsStreamProtocol.whip -> NavigationLink(destination = {
                        StreamWhipSettingsView(
                            model = model,
                            stream = stream,
                            whip = stream.whip,
                        )
                    }) {
                        Text(localized("WHIP"))
                    }
                    SettingsStreamProtocol.mobcam -> NavigationLink(destination = {
                        StreamMobcamSettingsView(stream = stream)
                    }) {
                        Text(localized("Mobcam"))
                    }
                    else -> Unit
                }
            }
        }
        Section(header = localized("Media")) {
            NavigationLink(destination = {
                StreamVideoSettingsView(database = database, stream = stream)
            }) {
                Text(localized("Video"))
            }
            if (database.showAllSettings) {
                NavigationLink(destination = {
                    StreamAudioSettingsView(
                        stream = stream,
                    )
                }) {
                    Text(localized("Audio"))
                }
                NavigationLink(destination = {
                    StreamRecordingSettingsView(stream = stream, recording = stream.recording)
                }) {
                    IconAndTextSettingView(image = "record.circle", text = "Recording")
                }
            }
            NavigationLink(destination = {
                StreamReplaySettingsView(
                    database = database,
                    stream = stream,
                    replay = stream.replay,
                )
            }) {
                IconAndTextSettingView(image = "play", text = "Replay")
            }
            if (database.showAllSettings) {
                NavigationLink(destination = {
                    StreamSnapshotSettingsView(stream = stream, recording = stream.recording)
                }) {
                    IconAndTextSettingView(image = "camera.aperture", text = "Snapshot")
                }
                NavigationLink(destination = {
                    StreamPreviewStreamSettingsView(previewStream = stream.previewStream)
                }) {
                    IconAndTextSettingView(image = "video.circle", text = "Preview stream")
                }
            }
            if (!isMac()) {
                Toggle(
                    isOn = stream.portrait,
                    onChange = { value ->
                        stream.portrait = value
                        if (stream.enabled) {
                            model.setCurrentStream(stream = stream)
                            model.reloadStream()
                            model.resetSelectedScene(changeScene = false)
                            model.updateOrientation()
                            model.updateOrientationLock()
                        }
                    },
                    enabled = !(stream.enabled && (isLive || isRecording)),
                ) {
                    Text(localized("Portrait"))
                }
            }
        }
        Section(header = localized("Streaming platforms")) {
            StreamPlatformsSettingsView(model = model, stream = stream)
        }
        if (!isMac()) {
            BackgroundStreamingView(model = model, stream = stream)
        }
        if (stream.getProtocol() == SettingsStreamProtocol.mobcam) {
            Section(footerContent = { AutoGoLiveFooterView() }) {
                Toggle(
                    title = localized("Auto go live"),
                    isOn = stream.autoGoLive,
                    onChange = { stream.autoGoLive = it },
                )
            }
        }
        Section {
            NavigationLink(destination = {
                StreamObsRemoteControlSettingsView(stream = stream)
            }) {
                Text(localized("OBS remote control"), modifier = Modifier.weight(1f))
                IosSwitch(
                    checked = stream.obsWebSocketEnabled,
                    onCheckedChange = { value ->
                        stream.obsWebSocketEnabled = value
                        if (stream.enabled) {
                            model.obsWebSocketEnabledUpdated()
                        }
                    },
                )
            }
            if (database.showAllSettings) {
                NavigationLink(destination = {
                    GoLiveNotificationSettingsView(stream = stream)
                }) {
                    Text(localized("Go live notification"))
                }
                NavigationLink(destination = {
                    StreamRealtimeIrlSettingsView(stream = stream)
                }) {
                    Text(localized("RealtimeIRL"), modifier = Modifier.weight(1f))
                    IosSwitch(
                        checked = stream.realtimeIrlEnabled,
                        onCheckedChange = { value ->
                            stream.realtimeIrlEnabled = value
                            if (stream.enabled) {
                                model.reloadLocation()
                            }
                        },
                    )
                }
            }
            NavigationLink(destination = {
                StreamEmotesSettingsView(stream = stream)
            }) {
                Text(localized("Emotes"))
            }
        }
        if (database.showAllSettings) {
            Section(
                footer = localized(
                    "Estimated viewer delay, for example used to make it easier to take " +
                        "snapshots using the chat bot. It does not delay the stream."
                )
            ) {
                NavigationLink(destination = {
                    TextEditView(
                        title = localized("Estimated viewer delay"),
                        value = formatOneDecimal(stream.estimatedViewerDelay),
                        onSubmit = { value ->
                            val latency = value.toFloatOrNull()
                            if (latency != null && latency >= 0.0f && latency <= 15.0f) {
                                stream.estimatedViewerDelay = latency
                            }
                        },
                    )
                }) {
                    TextItemLocalizedView(
                        name = "Estimated viewer delay",
                        value = formatShortDuration(stream.estimatedViewerDelay.toInt()),
                    )
                }
            }
        }
    }
}

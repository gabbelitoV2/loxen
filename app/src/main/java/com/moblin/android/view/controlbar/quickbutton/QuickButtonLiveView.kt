package com.moblin.android.view.controlbar.quickbutton

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormButton
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.Label
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.fallbackStream
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsDebug
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.view.settings.streams.stream.DiscordLogoAndNameView
import com.moblin.android.view.settings.streams.stream.KickLogoAndNameView
import com.moblin.android.view.settings.streams.stream.SoopLogoAndNameView
import com.moblin.android.view.settings.streams.stream.TwitchLogoAndNameView
import com.moblin.android.view.settings.streams.stream.YouTubeLogoAndNameView
import com.moblin.android.view.settings.streams.stream.golivenotification.GoLiveNotificationDiscordTextSettingsView
import com.moblin.android.view.settings.streams.stream.golivenotification.GoLiveNotificationSettingsView
import com.moblin.android.view.settings.streams.stream.kick.KickStreamLiveSettingsView
import com.moblin.android.view.settings.streams.stream.kick.loadKickStreamInfo
import com.moblin.android.view.settings.streams.stream.twitch.TwitchStreamLiveSettingsView
import com.moblin.android.view.settings.streams.stream.twitch.loadTwitchStreamInfo
import com.moblin.android.view.settings.streams.stream.youtube.StreamYouTubeScheduleStreamView
import com.moblin.android.view.utils.ShortcutSectionView
import com.moblin.android.view.utils.StreamingPlatformsShortcutView
import com.moblin.android.various.model.sendGoLiveNotification

@Composable
private fun TwitchView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
    title: String?,
    onTitleChange: (String?) -> Unit,
    category: String?,
    onCategoryChange: (String?) -> Unit,
) {
    Section(headerContent = { TwitchLogoAndNameView(channel = stream.twitchChannelName) }) {
        if (stream.twitchLoggedIn) {
            TwitchStreamLiveSettingsView(
                model = model,
                stream = stream,
                title = title,
                category = category,
            )
        }
    }
}

@Composable
private fun KickView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
    title: String?,
    onTitleChange: (String?) -> Unit,
    category: String?,
    onCategoryChange: (String?) -> Unit,
) {
    Section(headerContent = { KickLogoAndNameView(channel = stream.kickChannelName) }) {
        if (stream.kickLoggedIn) {
            KickStreamLiveSettingsView(
                model = model,
                stream = stream,
                title = title,
                onTitleChange = onTitleChange,
                category = category,
                onCategoryChange = onCategoryChange,
            )
        }
    }
}

@Composable
private fun YouTubeView(
    model: Model = LocalModel.current,
    debug: SettingsDebug,
    stream: SettingsStream,
) {
    Section(headerContent = { YouTubeLogoAndNameView(handle = stream.youTubeHandle) }) {
        StreamYouTubeScheduleStreamView(model = model, stream = stream)
    }
}

@Composable
private fun SoopView(
    stream: SettingsStream,
) {
    Section(headerContent = { SoopLogoAndNameView(channel = stream.soopChannelName) }) {
    }
}

@Composable
private fun GoLiveNotificationView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    var sending by remember { mutableStateOf(false) }
    Section(header = "Go live notification") {
        if (stream.goLiveNotificationDiscordWebhookUrl.isNotEmpty()) {
            NavigationLink(
                destination = {
                    Form(title = "Discord") {
                        GoLiveNotificationDiscordTextSettingsView(stream = stream)
                    }
                },
                label = {
                    DiscordLogoAndNameView()
                },
            )
        }
        FormButton(
            title = "Send",
            centered = true,
            enabled = !sending && isGoLiveNotificationConfigured(
                stream.goLiveNotificationDiscordWebhookUrl,
                stream.goLiveNotificationMoblinWebsite,
            ),
            action = {
                sending = true
                model.sendGoLiveNotification {
                    sending = false
                }
            },
        )
    }
}

@Composable
private fun ShortcutView(
    model: Model = LocalModel.current,
    database: Database,
    stream: SettingsStream,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    ShortcutSectionView {
        StreamingPlatformsShortcutView(model = model, stream = stream)
        if (database.showAllSettings) {
            NavigationLink(
                destination = {
                    GoLiveNotificationSettingsView(stream = stream)
                },
                label = {
                    Label("Go live notification", systemImage = "dot.radiowaves.left.and.right")
                },
            )
        }
    }
}

private fun anyStreamingPlatformConfigured(
    twitchChannelName: String,
    kickChannelName: String,
    youTubeHandle: String,
    soopChannelName: String,
): Boolean {
    if (twitchChannelName.isNotEmpty()) {
        return true
    }
    if (kickChannelName.isNotEmpty()) {
        return true
    }
    if (youTubeHandle.isNotEmpty()) {
        return true
    }
    if (soopChannelName.isNotEmpty()) {
        return true
    }
    return false
}

private fun isGoLiveNotificationConfigured(
    goLiveNotificationDiscordWebhookUrl: String,
    goLiveNotificationMoblinWebsite: Boolean,
): Boolean {
    return goLiveNotificationDiscordWebhookUrl.isNotEmpty() || goLiveNotificationMoblinWebsite
}

@Composable
fun QuickButtonLiveView(
    model: Model = LocalModel.current,
    database: Database,
    stream: SettingsStream,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    var twitchTitle by remember { mutableStateOf<String?>(null) }
    var twitchCategory by remember { mutableStateOf<String?>(null) }
    var kickTitle by remember { mutableStateOf<String?>(null) }
    var kickCategory by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        loadTwitchStreamInfo(model = model, stream = stream, loggedIn = stream.twitchLoggedIn) { title, category ->
            twitchTitle = title
            twitchCategory = category
        }
        loadKickStreamInfo(model = model, stream = stream, loggedIn = stream.kickLoggedIn) { title, category ->
            kickTitle = title
            kickCategory = category
        }
    }

    Form(title = "Stream") {
        if (stream !== fallbackStream) {
            if (!anyStreamingPlatformConfigured(
                    stream.twitchChannelName,
                    stream.kickChannelName,
                    stream.youTubeHandle,
                    stream.soopChannelName,
                )
            ) {
                FormRow {
                    Text("No 'Streaming platform' is configured.")
                }
            }
            if (stream.twitchChannelName.isNotEmpty()) {
                TwitchView(
                    model = model,
                    stream = stream,
                    title = twitchTitle,
                    onTitleChange = { twitchTitle = it },
                    category = twitchCategory,
                    onCategoryChange = { twitchCategory = it },
                )
            }
            if (stream.kickChannelName.isNotEmpty()) {
                KickView(
                    model = model,
                    stream = stream,
                    title = kickTitle,
                    onTitleChange = { kickTitle = it },
                    category = kickCategory,
                    onCategoryChange = { kickCategory = it },
                )
            }
            if (stream.youTubeHandle.isNotEmpty()) {
                YouTubeView(model = model, debug = database.debug, stream = stream)
            }
            if (stream.soopChannelName.isNotEmpty()) {
                SoopView(stream = stream)
            }
            if (database.showAllSettings && isGoLiveNotificationConfigured(
                    stream.goLiveNotificationDiscordWebhookUrl,
                    stream.goLiveNotificationMoblinWebsite,
                )
            ) {
                GoLiveNotificationView(
                    model = model,
                    stream = stream,
                    onNavigate = onNavigate,
                )
            }
            ShortcutView(
                model = model,
                database = database,
                stream = stream,
                onNavigate = onNavigate,
            )
        }
    }
}

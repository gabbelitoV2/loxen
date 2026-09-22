package com.moblin.android.view.controlbar.quickbutton

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import com.moblin.android.view.settings.streams.stream.kick.KickStreamLiveSettingsView
import com.moblin.android.view.settings.streams.stream.kick.loadKickStreamInfo
import com.moblin.android.view.settings.streams.stream.twitch.TwitchStreamLiveSettingsView
import com.moblin.android.view.settings.streams.stream.twitch.loadTwitchStreamInfo
import com.moblin.android.view.settings.streams.stream.youtube.StreamYouTubeScheduleStreamView
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.ShortcutSectionView
import com.moblin.android.view.utils.StreamingPlatformsShortcutView
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

@Composable
private fun TwitchView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
    title: String?,
    onTitleChange: (String?) -> Unit,
    category: String?,
    onCategoryChange: (String?) -> Unit,
) {
    val twitchChannelName by stream.twitchChannelName.collectAsState()
    val twitchLoggedIn by stream.twitchLoggedIn.collectAsState()
    Column {
        TwitchLogoAndNameView(channel = twitchChannelName)
        if (twitchLoggedIn) {
            TwitchStreamLiveSettingsView(
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
private fun KickView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
    title: String?,
    onTitleChange: (String?) -> Unit,
    category: String?,
    onCategoryChange: (String?) -> Unit,
) {
    val kickChannelName by stream.kickChannelName.collectAsState()
    val kickLoggedIn by stream.kickLoggedIn.collectAsState()
    Column {
        KickLogoAndNameView(channel = kickChannelName)
        if (kickLoggedIn) {
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
    val youTubeHandle by stream.youTubeHandle.collectAsState()
    Column {
        YouTubeLogoAndNameView(handle = youTubeHandle)
        StreamYouTubeScheduleStreamView(model = model, stream = stream)
    }
}

@Composable
private fun SoopView(
    stream: SettingsStream,
) {
    val soopChannelName by stream.soopChannelName.collectAsState()
    Column {
        SoopLogoAndNameView(channel = soopChannelName)
    }
}

@Composable
private fun GoLiveNotificationView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val goLiveNotificationDiscordWebhookUrl by stream.goLiveNotificationDiscordWebhookUrl.collectAsState()
    var sending by remember { mutableStateOf(false) }
    Column {
        Text("Go live notification")
        if (goLiveNotificationDiscordWebhookUrl.isNotEmpty()) {
            Row(
                modifier = Modifier.clickable {
                    onNavigate("GoLiveNotificationDiscordTextSettingsView")
                },
            ) {
                DiscordLogoAndNameView()
            }
        }
        Button(
            onClick = {
                sending = true
                model.sendGoLiveNotification {
                    sending = false
                }
            },
            enabled = !sending && model.isGoLiveNotificationConfigured(),
        ) {
            HCenter {
                if (sending) {
                    CircularProgressIndicator()
                } else {
                    Text("Send")
                }
            }
        }
    }
}

@Composable
private fun ShortcutView(
    model: Model = LocalModel.current,
    database: Database,
    stream: SettingsStream,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val showAllSettings by database.showAllSettings.collectAsState()
    ShortcutSectionView {
        StreamingPlatformsShortcutView(model = model, stream = stream)
        if (showAllSettings) {
            Row(
                modifier = Modifier.clickable {
                    onNavigate("GoLiveNotificationSettingsView")
                },
            ) {
                Icon(Icons.Default.Notifications, contentDescription = null)
                Text("Go live notification")
            }
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

@OptIn(ExperimentalMaterial3Api::class)
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

    val twitchChannelName by stream.twitchChannelName.collectAsState()
    val kickChannelName by stream.kickChannelName.collectAsState()
    val youTubeHandle by stream.youTubeHandle.collectAsState()
    val soopChannelName by stream.soopChannelName.collectAsState()
    val twitchLoggedIn by stream.twitchLoggedIn.collectAsState()
    val kickLoggedIn by stream.kickLoggedIn.collectAsState()
    val goLiveNotificationDiscordWebhookUrl by stream.goLiveNotificationDiscordWebhookUrl.collectAsState()
    val goLiveNotificationMoblinWebsite by stream.goLiveNotificationMoblinWebsite.collectAsState()
    val showAllSettings by database.showAllSettings.collectAsState()
    val debug by database.debug.collectAsState()

    LaunchedEffect(Unit) {
        loadTwitchStreamInfo(model = model, stream = stream, loggedIn = twitchLoggedIn) { title, category ->
            twitchTitle = title
            twitchCategory = category
        }
        loadKickStreamInfo(model = model, stream = stream, loggedIn = kickLoggedIn) { title, category ->
            kickTitle = title
            kickCategory = category
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Stream") })
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            if (stream !== fallbackStream) {
                if (!anyStreamingPlatformConfigured(
                        twitchChannelName,
                        kickChannelName,
                        youTubeHandle,
                        soopChannelName,
                    )
                ) {
                    item {
                        Text("No 'Streaming platform' is configured.")
                    }
                }
                if (twitchChannelName.isNotEmpty()) {
                    item {
                        TwitchView(
                            model = model,
                            stream = stream,
                            title = twitchTitle,
                            onTitleChange = { twitchTitle = it },
                            category = twitchCategory,
                            onCategoryChange = { twitchCategory = it },
                        )
                    }
                }
                if (kickChannelName.isNotEmpty()) {
                    item {
                        KickView(
                            model = model,
                            stream = stream,
                            title = kickTitle,
                            onTitleChange = { kickTitle = it },
                            category = kickCategory,
                            onCategoryChange = { kickCategory = it },
                        )
                    }
                }
                if (youTubeHandle.isNotEmpty()) {
                    item {
                        YouTubeView(model = model, debug = debug, stream = stream)
                    }
                }
                if (soopChannelName.isNotEmpty()) {
                    item {
                        SoopView(stream = stream)
                    }
                }
                if (showAllSettings && isGoLiveNotificationConfigured(
                        goLiveNotificationDiscordWebhookUrl,
                        goLiveNotificationMoblinWebsite,
                    )
                ) {
                    item {
                        GoLiveNotificationView(
                            model = model,
                            stream = stream,
                            onNavigate = onNavigate,
                        )
                    }
                }
                item {
                    ShortcutView(
                        model = model,
                        database = database,
                        stream = stream,
                        onNavigate = onNavigate,
                    )
                }
            }
        }
    }
}

package com.moblin.android.view.settings.streams.stream.snapshot

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
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
import com.moblin.android.common.various.cleanUrl
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.various.settings.SettingsStreamRecording
import com.moblin.android.view.settings.streams.stream.DiscordLogoAndNameView
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.LocalModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamSnapshotSettingsView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
    recording: SettingsStreamRecording,
) {
    fun submitSnapshotWebhookUrl(value: String) {
        val url = cleanUrl(value)
        stream.discordSnapshotWebhook = url
    }

    fun submitSnapshotChatBotWebhookUrl(value: String) {
        val url = cleanUrl(value)
        stream.discordChatBotSnapshotWebhook = url
    }

    val cleanSnapshots = recording.cleanSnapshots
    val discordSnapshotWebhook = stream.discordSnapshotWebhook
    val discordChatBotSnapshotWebhook = stream.discordChatBotSnapshotWebhook
    val discordSnapshotWebhookOnlyWhenLive = stream.discordSnapshotWebhookOnlyWhenLive

    var initialized by remember { mutableStateOf(false) }
    LaunchedEffect(cleanSnapshots) {
        if (initialized) {
            TODO("model.setCleanSnapshots()")
        } else {
            initialized = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(localized("Snapshot")) })
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            item {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Clean snapshots")
                        Spacer(Modifier.weight(1f))
                        Switch(
                            checked = cleanSnapshots,
                            onCheckedChange = { recording.cleanSnapshots = it },
                        )
                    }
                    Text("Do not show widgets in snapshots.")
                }
            }
            item {
                Column {
                    DiscordLogoAndNameView()
                    TextEditNavigationView(
                        title = localized("Webhook URL"),
                        value = discordSnapshotWebhook,
                        onSubmit = { submitSnapshotWebhookUrl(it) },
                        placeholder = "https://discord.com/api/webhooks/foobar",
                    )
                    TextEditNavigationView(
                        title = localized("Chat bot webhook URL"),
                        value = discordChatBotSnapshotWebhook,
                        onSubmit = { submitSnapshotChatBotWebhookUrl(it) },
                        placeholder = "https://discord.com/api/webhooks/foobar",
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Only when live")
                        Spacer(Modifier.weight(1f))
                        Switch(
                            checked = discordSnapshotWebhookOnlyWhenLive,
                            onCheckedChange = {
                                stream.discordSnapshotWebhookOnlyWhenLive = it
                            },
                        )
                    }
                }
            }
            item {
                Column {
                    Text(
                        "Automatically upload quick button snapshots and chat bot snapshots " +
                            "to channels in your Discord server.",
                    )
                    Text("")
                    Text(
                        "Create a webhook in your Discord server's settings and paste it's URL above.",
                    )
                }
            }
        }
    }
}

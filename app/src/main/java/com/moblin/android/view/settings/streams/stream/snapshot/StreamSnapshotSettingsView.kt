package com.moblin.android.view.settings.streams.stream.snapshot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.common.various.cleanUrl
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.various.settings.SettingsStreamRecording
import com.moblin.android.view.settings.streams.stream.DiscordLogoAndNameView
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.various.model.setCleanSnapshots

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

    Form(title = localized("Snapshot")) {
        Section(footer = localized("Do not show widgets in snapshots.")) {
            Toggle(localized("Clean snapshots"), isOn = recording.cleanSnapshots) { value ->
                recording.cleanSnapshots = value
                model.setCleanSnapshots()
            }
        }
        Section(
            headerContent = {
                DiscordLogoAndNameView()
            },
            footerContent = {
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        localized(
                            "Automatically upload quick button snapshots and chat bot snapshots " +
                                "to channels in your Discord server.",
                        ),
                    )
                    Text("")
                    Text(
                        localized(
                            "Create a webhook in your Discord server's settings and paste it's " +
                                "URL above.",
                        ),
                    )
                }
            },
        ) {
            TextEditNavigationView(
                title = localized("Webhook URL"),
                value = stream.discordSnapshotWebhook,
                onSubmit = { submitSnapshotWebhookUrl(it) },
                placeholder = "https://discord.com/api/webhooks/foobar",
            )
            TextEditNavigationView(
                title = localized("Chat bot webhook URL"),
                value = stream.discordChatBotSnapshotWebhook,
                onSubmit = { submitSnapshotChatBotWebhookUrl(it) },
                placeholder = "https://discord.com/api/webhooks/foobar",
            )
            Toggle(
                localized("Only when live"),
                isOn = stream.discordSnapshotWebhookOnlyWhenLive,
            ) { value ->
                stream.discordSnapshotWebhookOnlyWhenLive = value
            }
        }
    }
}

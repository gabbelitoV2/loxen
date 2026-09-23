package com.moblin.android.view.settings.streams.stream.golivenotification

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import com.moblin.android.LocalOnNavigate
import com.moblin.android.common.various.cleanUrl
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.various.utils.isMac
import com.moblin.android.view.settings.streams.stream.DiscordLogoAndNameView
import com.moblin.android.view.utils.MultiLineTextFieldDoneButtonView
import com.moblin.android.view.utils.MultiLineTextFieldView
import com.moblin.android.view.utils.TextEditNavigationView

@Composable
fun GoLiveNotificationDiscordTextSettingsView(stream: SettingsStream) {
    var editingText by remember { mutableStateOf(false) }
    Section(
        header = "Message",
        footerContent = {
            Column(horizontalAlignment = Alignment.Start) {
                MultiLineTextFieldDoneButtonView(
                    editingText = editingText,
                    onEditingTextChange = { editingText = it },
                )
                Text(
                    "Markdown works. Add emojis as <:myEmojiName:8912739817498174>. " +
                        "Send \\\\:myEmojiName: in Discord to get it."
                )
                Text("")
                Text("A snapshot will also be uploaded.")
            }
        },
    ) {
        MultiLineTextFieldView(
            value = stream.goLiveNotificationDiscordMessage,
            onValueChange = { stream.goLiveNotificationDiscordMessage = it },
            placeholder = "My text",
            modifier = Modifier.onFocusChanged { editingText = it.isFocused },
        )
    }
}

@Composable
private fun GoLiveNotificationDiscordSettingsView(stream: SettingsStream) {
    Form(title = "Discord") {
        GoLiveNotificationDiscordTextSettingsView(stream = stream)
        Section {
            TextEditNavigationView(
                title = localized("Webhook URL"),
                value = stream.goLiveNotificationDiscordWebhookUrl,
                onSubmit = {
                    stream.goLiveNotificationDiscordWebhookUrl = cleanUrl(it)
                },
                placeholder = "https://discord.com/api/webhooks/foobar",
            )
        }
    }
}

@Composable
fun GoLiveNotificationSettingsView(
    stream: SettingsStream,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Form(title = "Go live notification") {
        NavigationLink(
            destination = {
                GoLiveNotificationDiscordSettingsView(stream = stream)
            },
        ) {
            DiscordLogoAndNameView()
        }
        if (!isMac()) {
            Section {
                Toggle(
                    title = "[Moblin website](https://moblin.app/#streamers)",
                    isOn = stream.goLiveNotificationMoblinWebsite,
                    onChange = { stream.goLiveNotificationMoblinWebsite = it },
                )
            }
        }
    }
}

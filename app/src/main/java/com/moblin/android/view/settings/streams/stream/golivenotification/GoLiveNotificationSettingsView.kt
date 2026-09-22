package com.moblin.android.view.settings.streams.stream.golivenotification

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.moblin.android.common.various.cleanUrl
import com.moblin.android.various.utils.isMac
import com.moblin.android.localized
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.view.settings.streams.stream.DiscordLogoAndNameView
import com.moblin.android.view.utils.MultiLineTextFieldDoneButtonView
import com.moblin.android.view.utils.MultiLineTextFieldView
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.LocalOnNavigate

@Composable
fun GoLiveNotificationDiscordTextSettingsView(stream: SettingsStream) {
    val message = stream.goLiveNotificationDiscordMessage
    var editingText by remember { mutableStateOf(false) }
    Column {
        Text("Message")
        MultiLineTextFieldView(
            value = message,
            onValueChange = { stream.goLiveNotificationDiscordMessage = it },
            placeholder = "My text",
            modifier = Modifier.onFocusChanged { editingText = it.isFocused },
        )
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoLiveNotificationDiscordSettingsView(stream: SettingsStream) {
    val webhookUrl = stream.goLiveNotificationDiscordWebhookUrl
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Discord") })
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            item {
                GoLiveNotificationDiscordTextSettingsView(stream = stream)
            }
            item {
                TextEditNavigationView(
                    title = localized("Webhook URL"),
                    value = webhookUrl,
                    onSubmit = {
                        stream.goLiveNotificationDiscordWebhookUrl = cleanUrl(it)
                    },
                    placeholder = "https://discord.com/api/webhooks/foobar",
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoLiveNotificationSettingsView(
    stream: SettingsStream,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Go live notification") })
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("GoLiveNotificationDiscordSettingsView") },
                ) {
                    DiscordLogoAndNameView()
                }
            }
            if (!isMac()) {
                item {
                    val moblinWebsite = stream.goLiveNotificationMoblinWebsite
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("[Moblin website](https://moblin.app/#streamers)")
                        Switch(
                            checked = moblinWebsite,
                            onCheckedChange = { stream.goLiveNotificationMoblinWebsite = it },
                        )
                    }
                }
            }
        }
    }
}

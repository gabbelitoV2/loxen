package com.moblin.android.view.settings.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.fallbackStream
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsChat
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.view.utils.ShortcutSectionView
import com.moblin.android.view.utils.StreamingPlatformsShortcutView
import com.moblin.android.view.utils.TextItemLocalizedView

private fun submitMaximumAge(chat: SettingsChat, value: String) {
    val maximumAge = value.toIntOrNull() ?: return
    if (maximumAge <= 0) {
        return
    }
    chat.maximumAge.value = maximumAge
}

@Composable
private fun ChatSettingsGeneralView(
    model: Model,
    database: Database,
    chat: SettingsChat,
    onNavigate: (String) -> Unit,
) {
    val botEnabled by chat.botEnabled.collectAsState()
    val textToSpeechEnabled by chat.textToSpeechEnabled.collectAsState()
    val showAllSettings by database.showAllSettings.collectAsState()
    val maximumAge by chat.maximumAge.collectAsState()
    val maximumAgeEnabled by chat.maximumAgeEnabled.collectAsState()
    val showDeletedMessages by chat.showDeletedMessages.collectAsState()

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate("ChatBotSettingsView") },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(localized("Bot"))
            Spacer(Modifier.weight(1f))
            Switch(
                checked = botEnabled,
                onCheckedChange = { value ->
                    chat.botEnabled.value = value
                    model.chatBotCustomCommandsTextChanged()
                },
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate("ChatTextToSpeechSettingsView") },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(localized("Text to speech"))
            Spacer(Modifier.weight(1f))
            Switch(
                checked = textToSpeechEnabled,
                onCheckedChange = { value ->
                    chat.textToSpeechEnabled.value = value
                    if (!value) {
                        model.chatTextToSpeech.reset(running = true)
                    }
                },
            )
        }
        if (showAllSettings) {
            ChatFiltersSettingsView(chat = chat)
            ChatNicknamesSettingsView(model = model, nicknames = chat.nicknames)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate("Maximum age") },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextItemLocalizedView(
                    name = localized("Maximum age"),
                    value = maximumAge.toString(),
                )
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = maximumAgeEnabled,
                    onCheckedChange = { value -> chat.maximumAgeEnabled.value = value },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(localized("Show deleted messages"))
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = showDeletedMessages,
                    onCheckedChange = { value -> chat.showDeletedMessages.value = value },
                )
            }
            LaunchedEffect(showDeletedMessages) {
                model.reloadChatMessages()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSettingsView(
    model: Model,
    database: Database,
    chat: SettingsChat,
    stream: SettingsStream,
    onNavigate: (String) -> Unit,
) {
    val enabled by chat.enabled.collectAsState()
    val activityFeed by chat.activityFeed.collectAsState()
    val showAllSettings by database.showAllSettings.collectAsState()
    val background by chat.background.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(localized("Chat")) })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(localized("Enabled"))
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = enabled,
                    onCheckedChange = { value -> chat.enabled.value = value },
                )
            }
            LaunchedEffect(enabled) {
                model.reloadChats()
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(localized("Activity feed"))
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = activityFeed,
                    onCheckedChange = { value -> chat.activityFeed.value = value },
                )
            }
            ChatSettingsAppearanceView(model = model, database = database, chat = chat)
            ChatSettingsLayoutView(model = model, database = database, chat = chat)
            ChatSettingsGeneralView(
                model = model,
                database = database,
                chat = chat,
                onNavigate = onNavigate,
            )
            if (showAllSettings) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(localized("Background chat"))
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = background,
                        onCheckedChange = { value -> chat.background.value = value },
                    )
                }
                Text(
                    localized(
                        "Enable to keep chat alive when the app is in background mode, " +
                            "even if not streaming."
                    )
                )
            }
            if (stream !== fallbackStream) {
                ShortcutSectionView {
                    StreamingPlatformsShortcutView(model = model, stream = stream)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate("StreamEmotesSettingsView") },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Text(localized("Emotes"))
                    }
                }
            }
        }
    }
}

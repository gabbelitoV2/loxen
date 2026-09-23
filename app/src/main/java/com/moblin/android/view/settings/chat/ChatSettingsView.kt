package com.moblin.android.view.settings.chat

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Label
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.binding
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.fallbackStream
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsChat
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.view.settings.streams.stream.chat.StreamEmotesSettingsView
import com.moblin.android.view.utils.ShortcutSectionView
import com.moblin.android.view.utils.StreamingPlatformsShortcutView
import com.moblin.android.view.utils.TextEditView
import com.moblin.android.view.utils.TextItemLocalizedView
import com.moblin.android.various.model.chatBotCustomCommandsTextChanged
import com.moblin.android.various.model.reloadChatMessages
import com.moblin.android.various.model.reloadChats

private fun submitMaximumAge(chat: SettingsChat, value: String) {
    val maximumAge = value.toIntOrNull() ?: return
    if (maximumAge <= 0) {
        return
    }
    chat.maximumAge = maximumAge
}

@Composable
private fun ChatSettingsGeneralView(
    model: Model = LocalModel.current,
    database: Database,
    chat: SettingsChat,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    NavigationLink(destination = { ChatBotSettingsView(model = model) }) {
        Toggle(
            isOn = chat.botEnabled,
            onChange = { value ->
                chat.botEnabled = value
                model.chatBotCustomCommandsTextChanged()
            },
        ) {
            Text(localized("Bot"))
        }
    }
    NavigationLink(
        destination = {
            ChatTextToSpeechSettingsView(chat = chat, ttsMonster = chat.ttsMonster)
        },
    ) {
        Toggle(
            isOn = chat.textToSpeechEnabled,
            onChange = { value ->
                chat.textToSpeechEnabled = value
                if (!value) {
                    model.chatTextToSpeech.reset(running = true)
                }
            },
        ) {
            Text(localized("Text to speech"))
        }
    }
    if (database.showAllSettings) {
        ChatFiltersSettingsView(chat = chat)
        ChatNicknamesSettingsView(model = model, nicknames = chat.nicknames)
        NavigationLink(
            destination = {
                TextEditView(
                    title = localized("Maximum age"),
                    value = chat.maximumAge.toString(),
                    footers = listOf(localized("Maximum message age in seconds.")),
                    onSubmit = { value ->
                        submitMaximumAge(chat, value)
                    },
                )
            },
        ) {
            Toggle(
                isOn = chat.maximumAgeEnabled,
                onChange = { value -> chat.maximumAgeEnabled = value },
            ) {
                TextItemLocalizedView(
                    name = localized("Maximum age"),
                    value = chat.maximumAge.toString(),
                )
            }
        }
        Toggle(localized("Show deleted messages"), isOn = chat.showDeletedMessages) { value ->
            chat.showDeletedMessages = value
            model.reloadChatMessages()
        }
    }
}

@Composable
fun ChatSettingsView(
    model: Model = LocalModel.current,
    database: Database,
    chat: SettingsChat,
    stream: SettingsStream,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Form(title = localized("Chat")) {
        Section {
            Toggle(localized("Enabled"), isOn = chat.enabled) { value ->
                chat.enabled = value
                model.reloadChats()
            }
        }
        Section {
            Toggle(
                title = localized("Activity feed"),
                isOn = binding({ chat.activityFeed }) { chat.activityFeed = it },
            )
            ChatSettingsAppearanceView(model = model, database = database, chat = chat)
            ChatSettingsLayoutView(model = model, database = database, chat = chat)
            ChatSettingsGeneralView(
                model = model,
                database = database,
                chat = chat,
                onNavigate = onNavigate,
            )
        }
        if (database.showAllSettings) {
            Section(
                footer = localized(
                    "Enable to keep chat alive when the app is in background mode, " +
                        "even if not streaming."
                ),
            ) {
                Toggle(
                    title = localized("Background chat"),
                    isOn = binding({ chat.background }) { chat.background = it },
                )
            }
        }
        if (stream !== fallbackStream) {
            ShortcutSectionView {
                StreamingPlatformsShortcutView(model = model, stream = stream)
                NavigationLink(destination = { StreamEmotesSettingsView(stream = stream) }) {
                    Label(localized("Emotes"), systemImage = "dot.radiowaves.left.and.right")
                }
            }
        }
    }
}

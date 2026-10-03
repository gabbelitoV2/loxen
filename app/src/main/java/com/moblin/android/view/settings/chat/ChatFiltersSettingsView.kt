package com.moblin.android.view.settings.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.ForEach
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.move
import com.moblin.android.platform.swiftui.remove
import com.moblin.android.various.settings.SettingsChat
import com.moblin.android.various.settings.SettingsChatFilter
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.AddButtonView
import com.moblin.android.view.utils.ContextMenuDeleteButton
import com.moblin.android.view.utils.DraggableItemPrefixView
import com.moblin.android.view.utils.SwipeLeftToRemoveHelpView
import com.moblin.android.view.utils.TextEditView
import com.moblin.android.view.utils.TextItemLocalizedView

@Composable
private fun ChatFilterFilterSettingsView(
    filter: SettingsChatFilter,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val user = filter.user
    val messageStart = filter.messageStart

    Section(
        header = localized("Condition"),
        footer = localized(
            "The condition is true when both \"Username\" and \"Message starts with\" matches the received chat message."
        ),
    ) {
        NavigationLink(destination = { ChatFilterUsernameEditView(filter = filter) }) {
            TextItemLocalizedView(name = "Username", value = filter.username())
        }
        NavigationLink(destination = { ChatFilterMessageStartEditView(filter = filter) }) {
            TextItemLocalizedView(name = "Message starts with", value = filter.message())
        }
    }
    if (user.isEmpty() && messageStart.isEmpty()) {
        Section {
            Text(
                localized("⚠️ This filter matches all messages, check if this is the desired behaviour."),
            )
        }
    }
}

@Composable
private fun ChatFilterActionsSettingsView(
    filter: SettingsChatFilter,
) {
    Section(
        header = localized("Actions"),
        footer = localized("The actions to perform when the condition is true."),
    ) {
        Toggle(
            title = localized("Show in chat"),
            isOn = filter.showInChat,
            onChange = { filter.showInChat = it },
        )
        Toggle(
            title = localized("Show in activity feed"),
            isOn = filter.showInActivityFeed,
            onChange = { filter.showInActivityFeed = it },
        )
        Toggle(
            title = localized("Text to speech"),
            isOn = filter.textToSpeech,
            onChange = { filter.textToSpeech = it },
        )
        Toggle(
            title = localized("Chat bot"),
            isOn = filter.chatBot,
            onChange = { filter.chatBot = it },
        )
        Toggle(
            title = localized("Poll"),
            isOn = filter.poll,
            onChange = { filter.poll = it },
        )
        Toggle(
            title = localized("Print"),
            isOn = filter.print,
            onChange = { filter.print = it },
        )
    }
}

@Composable
private fun ChatFilterSettingsView(
    filter: SettingsChatFilter,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    NavigationLink(
        destination = { ChatFilterFormView(filter = filter, onNavigate = onNavigate) },
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DraggableItemPrefixView()
            TextItemLocalizedView(name = "Username", value = filter.username())
        }
    }
}

@Composable
fun ChatFiltersSettingsView(
    chat: SettingsChat,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    NavigationLink(
        destination = { ChatFiltersFormView(chat = chat, onNavigate = onNavigate) },
    ) {
        Text(localized("Filters"))
        Spacer(modifier = Modifier.weight(1f))
        GrayTextView(text = chat.filters.size.toString())
    }
}

@Composable
private fun ChatFilterFormView(
    filter: SettingsChatFilter,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Form(title = localized("Filter")) {
        Section {
            Toggle(
                title = localized("Enabled"),
                isOn = filter.enabled,
                onChange = { filter.enabled = it },
            )
        }
        ChatFilterFilterSettingsView(filter = filter, onNavigate = onNavigate)
        ChatFilterActionsSettingsView(filter = filter)
    }
}

@Composable
private fun ChatFiltersFormView(
    chat: SettingsChat,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Form(title = localized("Filters")) {
        Section(
            footerContent = {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(localized("The first filter that matches is used."))
                    Text("")
                    SwipeLeftToRemoveHelpView(kind = localized("a filter"))
                }
            },
        ) {
            ForEach(
                chat.filters,
                id = { it.id },
                onDelete = { offsets ->
                    chat.filters.remove(atOffsets = offsets)
                },
                onMove = { froms, to ->
                    chat.filters.move(fromOffsets = froms, toOffset = to)
                },
            ) { filter ->
                ContextMenuDeleteButton(
                    action = {
                        chat.filters.removeAll { it.id == filter.id }
                    },
                ) {
                    ChatFilterSettingsView(filter = filter, onNavigate = onNavigate)
                }
            }
            AddButtonView {
                chat.filters.add(SettingsChatFilter())
            }
        }
    }
}

@Composable
private fun ChatFilterUsernameEditView(
    filter: SettingsChatFilter,
) {
    val user = filter.user

    TextEditView(
        title = localized("Username"),
        value = user,
        onSubmit = {
            filter.user = it
        },
    )
}

@Composable
private fun ChatFilterMessageStartEditView(
    filter: SettingsChatFilter,
) {
    val messageStart = filter.messageStart

    TextEditView(
        title = localized("Message starts with"),
        value = messageStart,
        onSubmit = { newValue ->
            if (newValue.isEmpty()) {
                filter.messageStartWords = mutableListOf()
            } else {
                filter.messageStartWords = newValue.split(" ").toMutableList()
            }
            filter.messageStart = filter.messageStartWords.joinToString(" ")
        },
    )
}

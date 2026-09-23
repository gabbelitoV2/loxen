package com.moblin.android.view.settings.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.various.settings.SettingsChat
import com.moblin.android.various.settings.SettingsChatFilter
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.AddButtonView
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
            title = localized("Show on screen"),
            isOn = filter.showOnScreen,
            onChange = { filter.showOnScreen = it },
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
        DraggableItemPrefixView()
        TextItemLocalizedView(name = "Username", value = filter.username())
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
    val filters = chat.filters

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
            for (filter in filters) {
                key(filter.id) {
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

private fun deleteFilters(
    chat: SettingsChat,
    offsets: List<Int>,
) {
    val current = chat.filters.toMutableList()
    offsets.sortedDescending().forEach { current.removeAt(it) }
    chat.filters.clear()
    chat.filters.addAll(current)
}

private fun moveFilters(
    chat: SettingsChat,
    froms: List<Int>,
    to: Int,
) {
    val current = chat.filters.toMutableList()
    val moving = froms.sorted().mapNotNull { current.getOrNull(it) }
    froms.sortedDescending().forEach { index ->
        if (index in current.indices) {
            current.removeAt(index)
        }
    }
    val destination = (to - froms.count { it < to }).coerceIn(0, current.size)
    current.addAll(destination, moving)
    chat.filters.clear()
    chat.filters.addAll(current)
}

package com.moblin.android.view.settings.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.moblin.android.localized
import com.moblin.android.various.settings.SettingsChat
import com.moblin.android.various.settings.SettingsChatFilter
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.AddButtonView
import com.moblin.android.view.utils.DraggableItemPrefixView
import com.moblin.android.view.utils.SwipeLeftToRemoveHelpView
import com.moblin.android.view.utils.TextEditView
import com.moblin.android.view.utils.TextItemLocalizedView
import com.moblin.android.LocalOnNavigate

@Composable
private fun ChatFilterFilterSettingsView(
    filter: SettingsChatFilter,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val user by filter.user.collectAsState()
    val messageStart by filter.messageStart.collectAsState()

    Text(
        text = localized("Condition"),
        style = MaterialTheme.typography.titleSmall,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("Username") },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextItemLocalizedView(name = "Username", value = filter.username())
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("Message starts with") },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextItemLocalizedView(name = "Message starts with", value = filter.message())
    }
    Text(
        text = localized(
            "The condition is true when both \"Username\" and \"Message starts with\" matches the received chat message."
        ),
        style = MaterialTheme.typography.bodySmall,
    )
    if (user.isEmpty() && messageStart.isEmpty()) {
        Text(
            text = localized("⚠️ This filter matches all messages, check if this is the desired behaviour."),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ChatFilterActionsSettingsView(
    filter: SettingsChatFilter,
) {
    val showOnScreen by filter.showOnScreen.collectAsState()
    val textToSpeech by filter.textToSpeech.collectAsState()
    val chatBot by filter.chatBot.collectAsState()
    val poll by filter.poll.collectAsState()
    val printEnabled by filter.print.collectAsState()

    Text(
        text = localized("Actions"),
        style = MaterialTheme.typography.titleSmall,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = localized("Show on screen"), modifier = Modifier.weight(1f))
        Switch(
            checked = showOnScreen,
            onCheckedChange = { filter.showOnScreen.value = it },
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = localized("Text to speech"), modifier = Modifier.weight(1f))
        Switch(
            checked = textToSpeech,
            onCheckedChange = { filter.textToSpeech.value = it },
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = localized("Chat bot"), modifier = Modifier.weight(1f))
        Switch(
            checked = chatBot,
            onCheckedChange = { filter.chatBot.value = it },
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = localized("Poll"), modifier = Modifier.weight(1f))
        Switch(
            checked = poll,
            onCheckedChange = { filter.poll.value = it },
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = localized("Print"), modifier = Modifier.weight(1f))
        Switch(
            checked = printEnabled,
            onCheckedChange = { filter.print.value = it },
        )
    }
    Text(
        text = localized("The actions to perform when the condition is true."),
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun ChatFilterSettingsView(
    filter: SettingsChatFilter,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("Filter") },
        verticalAlignment = Alignment.CenterVertically,
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
    val filters by chat.filters.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("Filters") },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = localized("Filters"))
        Spacer(modifier = Modifier.weight(1f))
        GrayTextView(text = filters.size.toString())
    }
}

@Composable
private fun ChatFilterFormView(
    filter: SettingsChatFilter,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val enabled by filter.enabled.collectAsState()

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = localized("Enabled"), modifier = Modifier.weight(1f))
            Switch(
                checked = enabled,
                onCheckedChange = { filter.enabled.value = it },
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
    val filters by chat.filters.collectAsState()

    Column {
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(filters, key = { it.id }) { filter ->
                ChatFilterSettingsView(filter = filter, onNavigate = onNavigate)
                TODO("contextMenuDeleteButton has no Compose counterpart")
            }
            item {
                AddButtonView {
                    chat.filters.value = chat.filters.value + SettingsChatFilter()
                }
            }
        }
        Column(horizontalAlignment = Alignment.Start) {
            Text(text = localized("The first filter that matches is used."))
            Text(text = "")
            SwipeLeftToRemoveHelpView(kind = localized("a filter"))
        }
    }
}

@Composable
private fun ChatFilterUsernameEditView(
    filter: SettingsChatFilter,
) {
    val user by filter.user.collectAsState()

    TextEditView(
        title = localized("Username"),
        value = user,
        onSubmit = {
            filter.user.value = it
        },
    )
}

@Composable
private fun ChatFilterMessageStartEditView(
    filter: SettingsChatFilter,
) {
    val messageStart by filter.messageStart.collectAsState()

    TextEditView(
        title = localized("Message starts with"),
        value = messageStart,
        onSubmit = { newValue ->
            if (newValue.isEmpty()) {
                filter.messageStartWords.value = emptyList()
            } else {
                filter.messageStartWords.value = newValue.split(" ")
            }
            filter.messageStart.value = filter.messageStartWords.value.joinToString(" ")
        },
    )
}

private fun deleteFilters(
    chat: SettingsChat,
    offsets: List<Int>,
) {
    val current = chat.filters.value.toMutableList()
    offsets.sortedDescending().forEach { current.removeAt(it) }
    chat.filters.value = current
}

private fun moveFilters(
    chat: SettingsChat,
    froms: List<Int>,
    to: Int,
) {
    TODO("SwiftUI List.onMove has no Compose counterpart")
}

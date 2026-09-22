package com.moblin.android.view.settings.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsChat
import com.moblin.android.various.settings.SettingsChatDisplayStyle
import com.moblin.android.view.utils.RgbColorPickerView
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSettingsAppearanceView(
    model: Model = LocalModel.current,
    database: Database,
    chat: SettingsChat,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val showAllSettings = database.showAllSettings.collectAsState().value
    val fontSize = chat.fontSize.collectAsState().value
    val bigGifScale = chat.bigGifScale.collectAsState().value
    val displayStyle = chat.displayStyle.collectAsState().value
    val timestampColorEnabled = chat.timestampColorEnabled.collectAsState().value
    val boldUsername = chat.boldUsername.collectAsState().value
    val boldMessage = chat.boldMessage.collectAsState().value
    val badges = chat.badges.collectAsState().value
    val animatedEmotes = chat.animatedEmotes.collectAsState().value
    val sharedChatIcons = chat.sharedChatIcons.collectAsState().value
    val compactEvents = chat.compactEvents.collectAsState().value
    val sameUsernameColor = chat.sameUsernameColor.collectAsState().value
    val backgroundColorEnabled = chat.backgroundColorEnabled.collectAsState().value
    val shadowColorEnabled = chat.shadowColorEnabled.collectAsState().value
    val meInUsernameColor = chat.meInUsernameColor.collectAsState().value
    var displayStyleExpanded by remember { mutableStateOf(false) }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            TextButton(onClick = { onNavigate("Appearance") }) {
                Text(localized("Appearance"))
            }
        }
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Text(localized("Font size"))
                Slider(
                    value = fontSize.toFloat(),
                    onValueChange = { chat.fontSize.value = it.toDouble() },
                    valueRange = 10f..30f,
                    steps = 19,
                    onValueChangeFinished = { model.reloadChatMessages() },
                    modifier = Modifier.weight(1f),
                )
                Text(fontSize.toInt().toString(), modifier = Modifier.width(25.dp))
            }
            LaunchedEffect(fontSize) {
                model.reloadChatMessages()
            }
        }
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Text(localized("Big GIF scale"))
                Slider(
                    value = bigGifScale.toFloat(),
                    onValueChange = { chat.bigGifScale.value = it.toDouble() },
                    valueRange = 1f..10f,
                    steps = 8,
                    onValueChangeFinished = { model.reloadChatMessages() },
                    modifier = Modifier.weight(1f),
                )
                Text(bigGifScale.toInt().toString(), modifier = Modifier.width(25.dp))
            }
            LaunchedEffect(bigGifScale) {
                model.reloadChatMessages()
            }
        }
        if (showAllSettings) {
            item {
                ExposedDropdownMenuBox(
                    expanded = displayStyleExpanded,
                    onExpandedChange = { displayStyleExpanded = it },
                ) {
                    OutlinedTextField(
                        value = displayStyle.toString(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(localized("Display style")) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = displayStyleExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    )
                    ExposedDropdownMenu(
                        expanded = displayStyleExpanded,
                        onDismissRequest = { displayStyleExpanded = false },
                    ) {
                        SettingsChatDisplayStyle.entries.forEach { style ->
                            DropdownMenuItem(
                                text = { Text(style.toString()) },
                                onClick = {
                                    chat.displayStyle.value = style
                                    displayStyleExpanded = false
                                },
                            )
                        }
                    }
                }
            }
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    Text(localized("Timestamp"), modifier = Modifier.weight(1f))
                    Switch(
                        checked = timestampColorEnabled,
                        onCheckedChange = { chat.timestampColorEnabled.value = it },
                    )
                }
                LaunchedEffect(timestampColorEnabled) {
                    model.reloadChatMessages()
                }
            }
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    Text(localized("Bold name"), modifier = Modifier.weight(1f))
                    Switch(
                        checked = boldUsername,
                        onCheckedChange = { chat.boldUsername.value = it },
                    )
                }
                LaunchedEffect(boldUsername) {
                    model.reloadChatMessages()
                }
            }
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    Text(localized("Bold message"), modifier = Modifier.weight(1f))
                    Switch(
                        checked = boldMessage,
                        onCheckedChange = { chat.boldMessage.value = it },
                    )
                }
                LaunchedEffect(boldMessage) {
                    model.reloadChatMessages()
                }
            }
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    Text(localized("Badges"), modifier = Modifier.weight(1f))
                    Switch(
                        checked = badges,
                        onCheckedChange = { chat.badges.value = it },
                    )
                }
                LaunchedEffect(badges) {
                    model.reloadChatMessages()
                }
            }
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    Text(localized("Animated emotes"), modifier = Modifier.weight(1f))
                    Switch(
                        checked = animatedEmotes,
                        onCheckedChange = { chat.animatedEmotes.value = it },
                    )
                }
                LaunchedEffect(animatedEmotes) {
                    model.reloadChatMessages()
                }
            }
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    Text(localized("Shared chat icons"), modifier = Modifier.weight(1f))
                    Switch(
                        checked = sharedChatIcons,
                        onCheckedChange = { chat.sharedChatIcons.value = it },
                    )
                }
                LaunchedEffect(sharedChatIcons) {
                    model.reloadChatMessages()
                }
            }
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    Text(localized("Compact events"), modifier = Modifier.weight(1f))
                    Switch(
                        checked = compactEvents,
                        onCheckedChange = { chat.compactEvents.value = it },
                    )
                }
                LaunchedEffect(compactEvents) {
                    model.reloadChatMessages()
                }
            }
        }
        item {
            Text(
                localized("Colors"),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        }
        item {
            if (showAllSettings) {
                RgbColorPickerView(title = "Timestamp", color = chat.timestampColorColor) {
                    chat.timestampColor.value = it
                    model.reloadChatMessages()
                }
                RgbColorPickerView(title = "Name", color = chat.usernameColorColor) {
                    chat.usernameColor.value = it
                    model.reloadChatMessages()
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    Text(localized("Same color for all names"), modifier = Modifier.weight(1f))
                    Switch(
                        checked = sameUsernameColor,
                        onCheckedChange = { chat.sameUsernameColor.value = it },
                    )
                }
                RgbColorPickerView(title = "Message", color = chat.messageColorColor) {
                    chat.messageColor.value = it
                    model.reloadChatMessages()
                }
            }
        }
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                RgbColorPickerView(title = "Background", color = chat.backgroundColorColor) {
                    chat.backgroundColor.value = it
                    model.reloadChatMessages()
                }
                Switch(
                    checked = backgroundColorEnabled,
                    onCheckedChange = { chat.backgroundColorEnabled.value = it },
                )
            }
            LaunchedEffect(backgroundColorEnabled) {
                model.reloadChatMessages()
            }
        }
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                RgbColorPickerView(title = "Border", color = chat.shadowColorColor) {
                    chat.shadowColor.value = it
                    model.reloadChatMessages()
                }
                Switch(
                    checked = shadowColorEnabled,
                    onCheckedChange = { chat.shadowColorEnabled.value = it },
                )
            }
            LaunchedEffect(shadowColorEnabled) {
                model.reloadChatMessages()
            }
        }
        if (showAllSettings) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    Text(localized("Me in name color"), modifier = Modifier.weight(1f))
                    Switch(
                        checked = meInUsernameColor,
                        onCheckedChange = { chat.meInUsernameColor.value = it },
                    )
                }
            }
        }
    }
}

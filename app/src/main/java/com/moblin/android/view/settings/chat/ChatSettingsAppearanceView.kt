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
    val showAllSettings = database.showAllSettings
    val fontSize = chat.fontSize
    val bigGifScale = chat.bigGifScale
    val displayStyle = chat.displayStyle
    val timestampColorEnabled = chat.timestampColorEnabled
    val boldUsername = chat.boldUsername
    val boldMessage = chat.boldMessage
    val badges = chat.badges
    val animatedEmotes = chat.animatedEmotes
    val sharedChatIcons = chat.sharedChatIcons
    val compactEvents = chat.compactEvents
    val sameUsernameColor = chat.sameUsernameColor
    val backgroundColorEnabled = chat.backgroundColorEnabled
    val shadowColorEnabled = chat.shadowColorEnabled
    val meInUsernameColor = chat.meInUsernameColor
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
                    onValueChange = { chat.fontSize = it.toDouble() },
                    valueRange = 10f..30f,
                    steps = 19,
                    onValueChangeFinished = { TODO("reloadChatMessages") },
                    modifier = Modifier.weight(1f),
                )
                Text(fontSize.toInt().toString(), modifier = Modifier.width(25.dp))
            }
            LaunchedEffect(fontSize) {
                TODO("reloadChatMessages")
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
                    onValueChange = { chat.bigGifScale = it.toDouble() },
                    valueRange = 1f..10f,
                    steps = 8,
                    onValueChangeFinished = { TODO("reloadChatMessages") },
                    modifier = Modifier.weight(1f),
                )
                Text(bigGifScale.toInt().toString(), modifier = Modifier.width(25.dp))
            }
            LaunchedEffect(bigGifScale) {
                TODO("reloadChatMessages")
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
                                    chat.displayStyle = style
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
                        onCheckedChange = { chat.timestampColorEnabled = it },
                    )
                }
                LaunchedEffect(timestampColorEnabled) {
                    TODO("reloadChatMessages")
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
                        onCheckedChange = { chat.boldUsername = it },
                    )
                }
                LaunchedEffect(boldUsername) {
                    TODO("reloadChatMessages")
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
                        onCheckedChange = { chat.boldMessage = it },
                    )
                }
                LaunchedEffect(boldMessage) {
                    TODO("reloadChatMessages")
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
                        onCheckedChange = { chat.badges = it },
                    )
                }
                LaunchedEffect(badges) {
                    TODO("reloadChatMessages")
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
                        onCheckedChange = { chat.animatedEmotes = it },
                    )
                }
                LaunchedEffect(animatedEmotes) {
                    TODO("reloadChatMessages")
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
                        onCheckedChange = { chat.sharedChatIcons = it },
                    )
                }
                LaunchedEffect(sharedChatIcons) {
                    TODO("reloadChatMessages")
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
                        onCheckedChange = { chat.compactEvents = it },
                    )
                }
                LaunchedEffect(compactEvents) {
                    TODO("reloadChatMessages")
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
                RgbColorPickerView(
                    title = "Timestamp",
                    color = chat.timestampColorColor,
                    onColorChanged = {},
                ) {
                    chat.timestampColor = it
                    TODO("reloadChatMessages")
                }
                RgbColorPickerView(
                    title = "Name",
                    color = chat.usernameColorColor,
                    onColorChanged = {},
                ) {
                    chat.usernameColor = it
                    TODO("reloadChatMessages")
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
                        onCheckedChange = { chat.sameUsernameColor = it },
                    )
                }
                RgbColorPickerView(
                    title = "Message",
                    color = chat.messageColorColor,
                    onColorChanged = {},
                ) {
                    chat.messageColor = it
                    TODO("reloadChatMessages")
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
                RgbColorPickerView(
                    title = "Background",
                    color = chat.backgroundColorColor,
                    onColorChanged = {},
                ) {
                    chat.backgroundColor = it
                    TODO("reloadChatMessages")
                }
                Switch(
                    checked = backgroundColorEnabled,
                    onCheckedChange = { chat.backgroundColorEnabled = it },
                )
            }
            LaunchedEffect(backgroundColorEnabled) {
                TODO("reloadChatMessages")
            }
        }
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                RgbColorPickerView(
                    title = "Border",
                    color = chat.shadowColorColor,
                    onColorChanged = {},
                ) {
                    chat.shadowColor = it
                    TODO("reloadChatMessages")
                }
                Switch(
                    checked = shadowColorEnabled,
                    onCheckedChange = { chat.shadowColorEnabled = it },
                )
            }
            LaunchedEffect(shadowColorEnabled) {
                TODO("reloadChatMessages")
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
                        onCheckedChange = { chat.meInUsernameColor = it },
                    )
                }
            }
        }
    }
}

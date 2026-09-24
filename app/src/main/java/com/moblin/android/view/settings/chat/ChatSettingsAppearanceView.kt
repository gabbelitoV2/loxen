package com.moblin.android.view.settings.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.FormSlider
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.binding
import com.moblin.android.various.model.*
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsChat
import com.moblin.android.various.settings.SettingsChatDisplayStyle
import com.moblin.android.view.utils.FontSettingsView
import com.moblin.android.view.utils.RgbColorPickerView

@Composable
fun ChatSettingsAppearanceView(
    model: Model = LocalModel.current,
    database: Database,
    chat: SettingsChat,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    NavigationLink(
        destination = {
            Form(title = "Appearance") {
                Section(header = "Font") {
                    FormRow {
                        Text(localized("Size"))
                        FormSlider(
                            value = chat.fontSize,
                            onValueChange = {
                                chat.fontSize = it
                                model.reloadChatMessages()
                            },
                            modifier = Modifier.weight(1f),
                            valueRange = 10f..30f,
                            onValueChangeFinished = { model.reloadChatMessages() },
                        )
                        Box(
                            modifier = Modifier.width(25.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(chat.fontSize.toInt().toString())
                        }
                    }
                    if (database.showAllSettings) {
                        FontSettingsView(
                            font = binding(get = { chat.font }, set = { chat.font = it }),
                        ) {
                            model.reloadChatMessages()
                        }
                        Toggle("Bold name", isOn = chat.boldUsername) {
                            chat.boldUsername = it
                            model.reloadChatMessages()
                        }
                        Toggle("Bold message", isOn = chat.boldMessage) {
                            chat.boldMessage = it
                            model.reloadChatMessages()
                        }
                    }
                }
                if (database.showAllSettings) {
                    Section(header = "General") {
                        FormRow {
                            Text(localized("Big GIF scale"))
                            FormSlider(
                                value = chat.bigGifScale,
                                onValueChange = {
                                    chat.bigGifScale = it
                                    model.reloadChatMessages()
                                },
                                modifier = Modifier.weight(1f),
                                valueRange = 1f..10f,
                                onValueChangeFinished = { model.reloadChatMessages() },
                            )
                            Box(
                                modifier = Modifier.width(25.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(chat.bigGifScale.toInt().toString())
                            }
                        }
                        Picker(
                            title = "Display style",
                            selection = chat.displayStyle,
                            options = SettingsChatDisplayStyle.entries,
                            text = { it.toString() },
                            onChange = { chat.displayStyle = it },
                        )
                        Toggle("Timestamp", isOn = chat.timestampColorEnabled) {
                            chat.timestampColorEnabled = it
                            model.reloadChatMessages()
                        }
                        Toggle("Badges", isOn = chat.badges) {
                            chat.badges = it
                            model.reloadChatMessages()
                        }
                        Toggle("Animated emotes", isOn = chat.animatedEmotes) {
                            chat.animatedEmotes = it
                            model.reloadChatMessages()
                        }
                        Toggle("Shared chat icons", isOn = chat.sharedChatIcons) {
                            chat.sharedChatIcons = it
                            model.reloadChatMessages()
                        }
                        Toggle("Compact events", isOn = chat.compactEvents) {
                            chat.compactEvents = it
                            model.reloadChatMessages()
                        }
                    }
                }
                Section(header = "Colors") {
                    if (database.showAllSettings) {
                        RgbColorPickerView(
                            title = "Timestamp",
                            color = chat.timestampColorColor,
                            onColorChanged = {},
                        ) {
                            chat.timestampColor = it
                            model.reloadChatMessages()
                        }
                        RgbColorPickerView(
                            title = "Name",
                            color = chat.usernameColorColor,
                            onColorChanged = {},
                        ) {
                            chat.usernameColor = it
                            model.reloadChatMessages()
                        }
                        Toggle("Same color for all names", isOn = chat.sameUsernameColor) {
                            chat.sameUsernameColor = it
                        }
                        RgbColorPickerView(
                            title = "Message",
                            color = chat.messageColorColor,
                            onColorChanged = {},
                        ) {
                            chat.messageColor = it
                            model.reloadChatMessages()
                        }
                    }
                    Toggle(
                        isOn = chat.backgroundColorEnabled,
                        onChange = {
                            chat.backgroundColorEnabled = it
                            model.reloadChatMessages()
                        },
                    ) {
                        RgbColorPickerView(
                            title = "Background",
                            color = chat.backgroundColorColor,
                            onColorChanged = {},
                        ) {
                            chat.backgroundColor = it
                            model.reloadChatMessages()
                        }
                    }
                    Toggle(
                        isOn = chat.shadowColorEnabled,
                        onChange = {
                            chat.shadowColorEnabled = it
                            model.reloadChatMessages()
                        },
                    ) {
                        RgbColorPickerView(
                            title = "Border",
                            color = chat.shadowColorColor,
                            onColorChanged = {},
                        ) {
                            chat.shadowColor = it
                            model.reloadChatMessages()
                        }
                    }
                    if (database.showAllSettings) {
                        Toggle("Me in name color", isOn = chat.meInUsernameColor) {
                            chat.meInUsernameColor = it
                        }
                    }
                }
            }
        },
    ) {
        Text(localized("Appearance"))
    }
}

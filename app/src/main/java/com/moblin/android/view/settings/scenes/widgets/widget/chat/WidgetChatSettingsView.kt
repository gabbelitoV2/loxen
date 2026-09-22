package com.moblin.android.view.settings.scenes.widgets.widget.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.RgbColor
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsChatDisplayStyle
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetChat
import com.moblin.android.view.settings.chat.sliderValuePercentageWidth
import com.moblin.android.view.utils.RgbColorPickerView
import com.moblin.android.LocalModel

@Composable
fun WidgetChatSettingsView(
    model: Model = LocalModel.current,
    database: Database,
    widget: SettingsWidget,
    chat: SettingsWidgetChat,
) {
    fun setEffectSettings() {
        model.getChatEffect(id = widget.id)?.setSettings(settings = chat)
    }

    val showAllSettings = database.showAllSettings

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Font size")
            Slider(
                value = chat.fontSize.toFloat(),
                onValueChange = { chat.fontSize = it.toDouble() },
                valueRange = 10f..50f,
                steps = 39,
                onValueChangeFinished = { setEffectSettings() },
            )
            Text(
                chat.fontSize.toInt().toString(),
                modifier = Modifier.width(25.dp),
            )
        }
        LaunchedEffect(chat.fontSize) {
            setEffectSettings()
        }
        var messagesExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = messagesExpanded,
            onExpandedChange = { messagesExpanded = it },
        ) {
            OutlinedTextField(
                value = chat.maximumNumberOfMessages.toString(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Messages") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = messagesExpanded)
                },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
            )
            ExposedDropdownMenu(
                expanded = messagesExpanded,
                onDismissRequest = { messagesExpanded = false },
            ) {
                listOf(1, 2, 3, 4, 5).forEach { value ->
                    DropdownMenuItem(
                        text = { Text(value.toString()) },
                        onClick = {
                            chat.maximumNumberOfMessages = value
                            messagesExpanded = false
                        },
                    )
                }
            }
        }
        LaunchedEffect(chat.maximumNumberOfMessages) {
            setEffectSettings()
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Height")
            Slider(
                value = chat.height.toFloat(),
                onValueChange = { chat.height = it.toDouble() },
                valueRange = 0.1f..1f,
                steps = 89,
                onValueChangeFinished = { setEffectSettings() },
            )
            Text(
                "${(100 * chat.height).toInt()}%",
                modifier = Modifier.width(sliderValuePercentageWidth),
            )
        }
        LaunchedEffect(chat.height) {
            setEffectSettings()
        }
        if (showAllSettings) {
            var displayStyleExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = displayStyleExpanded,
                onExpandedChange = { displayStyleExpanded = it },
            ) {
                OutlinedTextField(
                    value = chat.displayStyle.toString(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Display style") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = displayStyleExpanded)
                    },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = displayStyleExpanded,
                    onDismissRequest = { displayStyleExpanded = false },
                ) {
                    SettingsChatDisplayStyle.entries.forEach { displayStyle ->
                        DropdownMenuItem(
                            text = { Text(displayStyle.toString()) },
                            onClick = {
                                chat.displayStyle = displayStyle
                                displayStyleExpanded = false
                            },
                        )
                    }
                }
            }
            LaunchedEffect(chat.displayStyle) {
                setEffectSettings()
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Bold name")
                Switch(
                    checked = chat.boldUsername,
                    onCheckedChange = { chat.boldUsername = it },
                )
            }
            LaunchedEffect(chat.boldUsername) {
                setEffectSettings()
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Bold message")
                Switch(
                    checked = chat.boldMessage,
                    onCheckedChange = { chat.boldMessage = it },
                )
            }
            LaunchedEffect(chat.boldMessage) {
                setEffectSettings()
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Badges")
                Switch(
                    checked = chat.badges,
                    onCheckedChange = { chat.badges = it },
                )
            }
            LaunchedEffect(chat.badges) {
                setEffectSettings()
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Shared chat icons")
                Switch(
                    checked = chat.sharedChatIcons,
                    onCheckedChange = { chat.sharedChatIcons = it },
                )
            }
            LaunchedEffect(chat.sharedChatIcons) {
                setEffectSettings()
            }
        }

        Text("Colors")
        if (showAllSettings) {
            RgbColorPickerView(
                title = "Name",
                color = chat.usernameColorColor,
            ) { newColor: RgbColor ->
                chat.usernameColor = newColor
                setEffectSettings()
            }
            RgbColorPickerView(
                title = "Message",
                color = chat.messageColorColor,
            ) { newColor: RgbColor ->
                chat.messageColor = newColor
                setEffectSettings()
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            RgbColorPickerView(
                title = "Background",
                color = chat.backgroundColorColor,
            ) { newColor: RgbColor ->
                chat.backgroundColor = newColor
                setEffectSettings()
            }
            Switch(
                checked = chat.backgroundColorEnabled,
                onCheckedChange = { chat.backgroundColorEnabled = it },
            )
        }
        LaunchedEffect(chat.backgroundColorEnabled) {
            setEffectSettings()
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            RgbColorPickerView(
                title = "Border",
                color = chat.shadowColorColor,
            ) { newColor: RgbColor ->
                chat.shadowColor = newColor
                setEffectSettings()
            }
            Switch(
                checked = chat.shadowColorEnabled,
                onCheckedChange = { chat.shadowColorEnabled = it },
            )
        }
        LaunchedEffect(chat.shadowColorEnabled) {
            setEffectSettings()
        }
    }
}

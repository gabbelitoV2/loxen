package com.moblin.android.view.settings.scenes.widgets.widget.chat

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalModel
import com.moblin.android.common.various.RgbColor
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.FormSlider
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.binding
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsChatDisplayStyle
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetChat
import com.moblin.android.view.settings.chat.sliderValuePercentageWidth
import com.moblin.android.view.utils.FontSettingsView
import com.moblin.android.view.utils.RgbColorPickerView
import com.moblin.android.various.model.getChatEffect

@Composable
fun WidgetChatSettingsView(
    model: Model = LocalModel.current,
    database: Database,
    widget: SettingsWidget,
    chat: SettingsWidgetChat,
) {
    fun setEffectSettings() {
        model.getChatEffect(widget.id)?.setSettings(chat)
    }

    val showAllSettings = database.showAllSettings

    Section(header = "Font") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(localized("Size"), fontSize = 17.sp)
            FormSlider(
                value = chat.fontSize.toFloat(),
                onValueChange = {
                    chat.fontSize = it
                    setEffectSettings()
                },
                modifier = Modifier.weight(1f),
                valueRange = 10f..50f,
                onValueChangeFinished = { setEffectSettings() },
            )
            Text(
                chat.fontSize.toInt().toString(),
                modifier = Modifier.width(25.dp),
                fontSize = 17.sp,
            )
        }
        if (showAllSettings) {
            val font = binding(get = { chat.font }, set = { chat.font = it })
            FontSettingsView(
                font = font,
                onChange = { setEffectSettings() },
            )
            Toggle(
                title = "Bold name",
                isOn = chat.boldUsername,
                onChange = {
                    chat.boldUsername = it
                    setEffectSettings()
                },
            )
            Toggle(
                title = "Bold message",
                isOn = chat.boldMessage,
                onChange = {
                    chat.boldMessage = it
                    setEffectSettings()
                },
            )
        }
    }
    Section(header = "General") {
        Picker(
            title = "Messages",
            selection = chat.maximumNumberOfMessages,
            options = listOf(1, 2, 3, 4, 5),
            text = { it.toString() },
            onChange = {
                chat.maximumNumberOfMessages = it
                setEffectSettings()
            },
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(localized("Height"), fontSize = 17.sp)
            FormSlider(
                value = chat.height.toFloat(),
                onValueChange = {
                    chat.height = it
                    setEffectSettings()
                },
                modifier = Modifier.weight(1f),
                valueRange = 0.1f..1f,
            )
            Text(
                "${(100 * chat.height).toInt()}%",
                modifier = Modifier.width(sliderValuePercentageWidth.dp),
                fontSize = 17.sp,
            )
        }
        if (showAllSettings) {
            Picker(
                title = "Display style",
                selection = chat.displayStyle,
                options = SettingsChatDisplayStyle.entries,
                text = { it.toString() },
                onChange = {
                    chat.displayStyle = it
                    setEffectSettings()
                },
            )
            Toggle(
                title = "Badges",
                isOn = chat.badges,
                onChange = {
                    chat.badges = it
                    setEffectSettings()
                },
            )
            Toggle(
                title = "Shared chat icons",
                isOn = chat.sharedChatIcons,
                onChange = {
                    chat.sharedChatIcons = it
                    setEffectSettings()
                },
            )
        }
    }
    Section(header = "Colors") {
        if (showAllSettings) {
            RgbColorPickerView(
                title = "Name",
                color = chat.usernameColorColor,
                onColorChanged = { chat.usernameColorColor = it },
            ) { newColor: RgbColor ->
                chat.usernameColor = newColor
                setEffectSettings()
            }
            RgbColorPickerView(
                title = "Message",
                color = chat.messageColorColor,
                onColorChanged = { chat.messageColorColor = it },
            ) { newColor: RgbColor ->
                chat.messageColor = newColor
                setEffectSettings()
            }
        }
        Toggle(
            isOn = chat.backgroundColorEnabled,
            onChange = {
                chat.backgroundColorEnabled = it
                setEffectSettings()
            },
        ) {
            RgbColorPickerView(
                title = "Background",
                color = chat.backgroundColorColor,
                onColorChanged = { chat.backgroundColorColor = it },
            ) { newColor: RgbColor ->
                chat.backgroundColor = newColor
                setEffectSettings()
            }
        }
        Toggle(
            isOn = chat.shadowColorEnabled,
            onChange = {
                chat.shadowColorEnabled = it
                setEffectSettings()
            },
        ) {
            RgbColorPickerView(
                title = "Border",
                color = chat.shadowColorColor,
                onColorChanged = { chat.shadowColorColor = it },
            ) { newColor: RgbColor ->
                chat.shadowColor = newColor
                setEffectSettings()
            }
        }
    }
}

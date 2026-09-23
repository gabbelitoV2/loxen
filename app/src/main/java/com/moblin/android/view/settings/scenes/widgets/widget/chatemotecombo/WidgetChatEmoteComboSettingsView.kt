package com.moblin.android.view.settings.scenes.widgets.widget.chatemotecombo

import androidx.compose.runtime.Composable
import com.moblin.android.LocalModel
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetChatEmoteCombo
import com.moblin.android.localized
import com.moblin.android.various.model.getChatEmoteComboEffect

@Composable
fun WidgetChatEmoteComboSettingsView(
    model: Model = LocalModel.current,
    widget: SettingsWidget,
    chatEmoteCombo: SettingsWidgetChatEmoteCombo,
) {
    Section {
        Picker(
            title = localized("Minimum combo"),
            selection = chatEmoteCombo.minimumCombo,
            options = listOf(2, 3, 4, 5, 6, 7, 8, 9, 10),
            onChange = {
                chatEmoteCombo.minimumCombo = it
                setEffectSettings(model, widget, chatEmoteCombo)
            },
        )
        Picker(
            title = localized("Timeout"),
            selection = chatEmoteCombo.resetAfter,
            options = listOf(3, 4, 5, 6, 7, 8, 9, 10),
            text = { "${it}s" },
            onChange = {
                chatEmoteCombo.resetAfter = it
                setEffectSettings(model, widget, chatEmoteCombo)
            },
        )
    }
}

private fun setEffectSettings(
    model: Model,
    widget: SettingsWidget,
    chatEmoteCombo: SettingsWidgetChatEmoteCombo,
) {
    model.getChatEmoteComboEffect(widget.id)?.setSettings(settings = chatEmoteCombo)
}

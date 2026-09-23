package com.moblin.android.view.settings.keyboard

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.moblin.android.LocalModel
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.ForEach
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.removing
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsKeyboard
import com.moblin.android.various.settings.SettingsKeyboardKey
import com.moblin.android.view.utils.ContextMenuDeleteButton
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView

@Composable
fun KeyboardSettingsView(
    model: Model = LocalModel.current,
    keyboard: SettingsKeyboard,
) {
    Form(title = "Keyboard") {
        Section {
            Text(localized("Use a keyboard to zoom, set scene, and more."))
        }
        Section(footerContent = { SwipeLeftToDeleteHelpView(kind = localized("a key")) }) {
            ForEach(
                keyboard.keys,
                id = { it.id },
                onDelete = { indexSet ->
                    keyboard.keys = keyboard.keys.removing(atOffsets = indexSet)
                },
            ) { keyValue ->
                ContextMenuDeleteButton(action = {
                    keyboard.keys = keyboard.keys.filterNot { it.id == keyValue.id }
                }) {
                    KeyboardKeySettingsView(model = model, key = keyValue)
                }
            }
            CreateButtonView {
                keyboard.keys = keyboard.keys + SettingsKeyboardKey()
            }
        }
    }
}

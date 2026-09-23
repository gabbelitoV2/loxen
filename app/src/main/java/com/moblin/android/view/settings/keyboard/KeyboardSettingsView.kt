package com.moblin.android.view.settings.keyboard

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.moblin.android.LocalModel
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsKeyboard
import com.moblin.android.various.settings.SettingsKeyboardKey
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
            keyboard.keys.forEach { keyValue ->
                key(keyValue.id) {
                    var menuExpanded by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(keyValue.id) {
                                detectTapGestures(onLongPress = { menuExpanded = true })
                            },
                    ) {
                        KeyboardKeySettingsView(model = model, key = keyValue)
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(localized("Delete")) },
                                onClick = {
                                    menuExpanded = false
                                    keyboard.keys =
                                        keyboard.keys.filterNot { it.id == keyValue.id }
                                },
                            )
                        }
                    }
                }
            }
            CreateButtonView {
                keyboard.keys = keyboard.keys + SettingsKeyboardKey()
            }
        }
    }
}

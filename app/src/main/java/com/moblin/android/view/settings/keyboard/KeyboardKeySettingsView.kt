package com.moblin.android.view.settings.keyboard

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.LocalNavigator
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsControllerFunction
import com.moblin.android.various.settings.SettingsKeyboardKey
import com.moblin.android.view.settings.gamecontrollers.ControllerButtonView

@Composable
private fun SelectedKeyView(key: SettingsKeyboardKey) {
    val keyValue = key.key
    if (keyValue.isEmpty()) {
        Text(text = "No key set", color = formPalette().gray)
    } else {
        Text(text = keyValue)
    }
}

@Composable
private fun KeyPickerView(key: SettingsKeyboardKey, onDismiss: () -> Unit) {
    val focusRequester = remember { FocusRequester() }
    var editingText by remember { mutableStateOf(false) }

    LaunchedEffect(editingText) {
        if (editingText) {
            key.key = ""
        }
    }
    LaunchedEffect(Unit) {
        if (key.key.isEmpty()) {
            focusRequester.requestFocus()
        }
    }
    OutlinedTextField(
        value = key.key,
        onValueChange = { newValue ->
            key.key = newValue
            if (newValue.isNotEmpty()) {
                onDismiss()
            }
        },
        placeholder = { Text(text = "No key set") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
        ),
        keyboardActions = KeyboardActions(onDone = { onDismiss() }),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged { editingText = it.isFocused },
    )
}

private fun functions(): List<SettingsControllerFunction> {
    return SettingsControllerFunction.entries.filter {
        it != SettingsControllerFunction.ZOOM_IN && it != SettingsControllerFunction.ZOOM_OUT
    }
}

@Composable
fun KeyboardKeySettingsView(
    model: Model = LocalModel.current,
    key: SettingsKeyboardKey,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    NavigationLink(destination = {
        KeyboardKeySettingsForm(model = model, key = key, onNavigate = onNavigate)
    }) {
        SelectedKeyView(key = key)
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = key.function.toString(
                sceneName = null,
                widgetName = null,
            ),
            color = key.function.color(),
        )
    }
}

@Composable
fun KeyboardKeySettingsForm(
    model: Model = LocalModel.current,
    key: SettingsKeyboardKey,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Form(title = "Keyboard key") {
        Section {
            NavigationLink(destination = {
                val navigator = LocalNavigator.current
                KeyboardKeyPickerView(key = key, onDismiss = { navigator?.pop() })
            }) {
                Text(text = localized("Key"))
                Spacer(modifier = Modifier.weight(1f))
                CompositionLocalProvider(LocalContentColor provides formPalette().gray) {
                    SelectedKeyView(key = key)
                }
            }
        }
        Section {
            ControllerButtonView(
                model = model,
                functions = functions(),
                function = key.function,
                onFunctionChange = { key.function = it },
                functionData = key.functionData,
                onFunctionDataChange = { key.functionData = it },
            )
        }
    }
}

@Composable
fun KeyboardKeyPickerView(key: SettingsKeyboardKey, onDismiss: () -> Unit) {
    Form(title = "Key") {
        Section {
            KeyPickerView(key = key, onDismiss = onDismiss)
        }
    }
}

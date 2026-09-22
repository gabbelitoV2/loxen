package com.moblin.android.view.settings.keyboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsControllerFunction
import com.moblin.android.various.settings.SettingsKeyboardKey
import com.moblin.android.view.settings.gamecontrollers.ControllerButtonView
import java.util.UUID
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

@Composable
private fun SelectedKeyView(key: SettingsKeyboardKey) {
    val keyValue by key.key.collectAsState()
    if (keyValue.isEmpty()) {
        Text(text = "No key set", color = Color.Gray)
    } else {
        Text(text = keyValue)
    }
}

@Composable
private fun KeyPickerView(key: SettingsKeyboardKey, onDismiss: () -> Unit) {
    val keyValue by key.key.collectAsState()
    val focusRequester = remember { FocusRequester() }
    var editingText by remember { mutableStateOf(false) }

    LaunchedEffect(keyValue) {
        if (keyValue.isEmpty()) {
            return@LaunchedEffect
        }
        onDismiss()
    }
    LaunchedEffect(editingText) {
        if (!editingText) {
            return@LaunchedEffect
        }
        key.key.value = ""
    }
    LaunchedEffect(Unit) {
        if (key.key.value.isEmpty()) {
            editingText = true
            focusRequester.requestFocus()
        }
    }
    OutlinedTextField(
        value = keyValue,
        onValueChange = { key.key.value = it },
        label = { Text(text = "No key set") },
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
        it != SettingsControllerFunction.zoomIn && it != SettingsControllerFunction.zoomOut
    }
}

@Composable
fun KeyboardKeySettingsView(
    model: Model = LocalModel.current,
    key: SettingsKeyboardKey,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val function by key.function.collectAsState()
    val functionData by key.functionData.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("keyboardKey") }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SelectedKeyView(key = key)
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = function.toString(
                sceneName = model.getSceneName(
                    id = functionData.sceneId ?: UUID.randomUUID(),
                ),
                widgetName = model.getWidgetName(
                    id = functionData.widgetId ?: UUID.randomUUID(),
                ),
            ),
            color = function.color(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyboardKeySettingsForm(
    model: Model = LocalModel.current,
    key: SettingsKeyboardKey,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = "Keyboard key") })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate("key") }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "Key")
                Spacer(modifier = Modifier.weight(1f))
                SelectedKeyView(key = key)
            }
            ControllerButtonView(
                model = model,
                functions = functions(),
                function = key.function.value,
                onFunctionChange = { key.function.value = it },
                functionData = key.functionData.value,
                onFunctionDataChange = { key.functionData.value = it },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyboardKeyPickerView(key: SettingsKeyboardKey, onDismiss: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = "Key") })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            KeyPickerView(key = key, onDismiss = onDismiss)
        }
    }
}

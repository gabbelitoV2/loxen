package com.moblin.android.view.settings.scenes.widgets.widget.chatemotecombo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetChatEmoteCombo
import com.moblin.android.LocalModel

@Composable
fun WidgetChatEmoteComboSettingsView(
    model: Model = LocalModel.current,
    widget: SettingsWidget,
    chatEmoteCombo: SettingsWidgetChatEmoteCombo,
) {
    val minimumCombo by chatEmoteCombo.minimumCombo.collectAsState()
    val resetAfter by chatEmoteCombo.resetAfter.collectAsState()
    LaunchedEffect(minimumCombo) {
        setEffectSettings(model, widget, chatEmoteCombo)
    }
    LaunchedEffect(resetAfter) {
        setEffectSettings(model, widget, chatEmoteCombo)
    }
    Column {
        ComboPicker(
            label = "Minimum combo",
            options = listOf(2, 3, 4, 5, 6, 7, 8, 9, 10),
            selected = minimumCombo,
            optionLabel = { it.toString() },
            onSelected = { chatEmoteCombo.minimumCombo.value = it },
        )
        ComboPicker(
            label = "Timeout",
            options = listOf(3, 4, 5, 6, 7, 8, 9, 10),
            selected = resetAfter,
            optionLabel = { "${it}s" },
            onSelected = { chatEmoteCombo.resetAfter.value = it },
        )
    }
}

private fun setEffectSettings(
    model: Model,
    widget: SettingsWidget,
    chatEmoteCombo: SettingsWidgetChatEmoteCombo,
) {
    model.getChatEmoteComboEffect(widget.id)?.setSettings(chatEmoteCombo)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComboPicker(
    label: String,
    options: List<Int>,
    selected: Int,
    optionLabel: (Int) -> String,
    onSelected: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = optionLabel(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        expanded = false
                        onSelected(option)
                    },
                )
            }
        }
    }
}

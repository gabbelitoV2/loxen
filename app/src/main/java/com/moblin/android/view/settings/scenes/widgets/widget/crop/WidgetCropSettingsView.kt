package com.moblin.android.view.settings.scenes.widgets.widget.crop

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetType
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.LocalModel

private fun changeXY(value: String): String? {
    val x = value.toIntOrNull() ?: return localized("Not a number")
    if (x < 0) {
        return localized("Too small")
    }
    return null
}

private fun changeWidthHeight(value: String): String? {
    val x = value.toIntOrNull() ?: return localized("Not a number")
    if (x <= 0) {
        return localized("Too small")
    }
    return null
}

private fun submitX(model: Model, widget: SettingsWidget, value: String) {
    val x = value.toIntOrNull() ?: return
    widget.crop.x = x
    TODO("model.resetSelectedScene(changeScene = false)")
}

private fun submitY(model: Model, widget: SettingsWidget, value: String) {
    val y = value.toIntOrNull() ?: return
    widget.crop.y = y
    TODO("model.resetSelectedScene(changeScene = false)")
}

private fun submitWidth(model: Model, widget: SettingsWidget, value: String) {
    val width = value.toIntOrNull() ?: return
    widget.crop.width = width
    TODO("model.resetSelectedScene(changeScene = false)")
}

private fun submitHeight(model: Model, widget: SettingsWidget, value: String) {
    val height = value.toIntOrNull() ?: return
    if (height <= 0) {
        return
    }
    widget.crop.height = height
    TODO("model.resetSelectedScene(changeScene = false)")
}

private fun sourceWidgetExists(model: Model, widget: SettingsWidget): Boolean {
    return model.database.widgets.any { it.id == widget.crop.sourceWidgetId }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetCropSettingsView(model: Model = LocalModel.current, widget: SettingsWidget) {
    val database = model.database
    var expanded by remember { mutableStateOf(false) }
    val sourceWidgetId = if (sourceWidgetExists(model, widget)) {
        widget.crop.sourceWidgetId
    } else {
        null
    }
    val sourceWidgetName = database.widgets.firstOrNull { it.id == sourceWidgetId }?.name ?: ""
    Column {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = sourceWidgetName,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                label = { Text(localized("Source widget")) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                if (!sourceWidgetExists(model, widget)) {
                    DropdownMenuItem(
                        text = { Text("") },
                        onClick = {
                            TODO("widget.crop.sourceWidgetId = null")
                        }
                    )
                }
                database.widgets
                    .filter { it.type == SettingsWidgetType.browser }
                    .forEach { browserWidget ->
                        DropdownMenuItem(
                            text = { Text(browserWidget.name) },
                            onClick = {
                                widget.crop.sourceWidgetId = browserWidget.id
                                TODO("model.resetSelectedScene(changeScene = false)")
                                expanded = false
                            }
                        )
                    }
            }
        }
        TextEditNavigationView(
            title = localized("X"),
            value = widget.crop.x.toString(),
            onChange = ::changeXY,
            onSubmit = { submitX(model, widget, it) },
            keyboardType = KeyboardType.Number
        )
        TextEditNavigationView(
            title = localized("Y"),
            value = widget.crop.y.toString(),
            onChange = ::changeXY,
            onSubmit = { submitY(model, widget, it) },
            keyboardType = KeyboardType.Number
        )
        TextEditNavigationView(
            title = localized("Width"),
            value = widget.crop.width.toString(),
            onChange = ::changeWidthHeight,
            onSubmit = { submitWidth(model, widget, it) },
            keyboardType = KeyboardType.Number
        )
        TextEditNavigationView(
            title = localized("Height"),
            value = widget.crop.height.toString(),
            onChange = ::changeWidthHeight,
            onSubmit = { submitHeight(model, widget, it) },
            keyboardType = KeyboardType.Number
        )
    }
}

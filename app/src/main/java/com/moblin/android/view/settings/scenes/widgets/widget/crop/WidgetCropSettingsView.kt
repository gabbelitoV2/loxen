package com.moblin.android.view.settings.scenes.widgets.widget.crop

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.KeyboardType
import com.moblin.android.LocalModel
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetType
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.various.model.resetSelectedScene

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
    model.resetSelectedScene(changeScene = false)
}

private fun submitY(model: Model, widget: SettingsWidget, value: String) {
    val y = value.toIntOrNull() ?: return
    widget.crop.y = y
    model.resetSelectedScene(changeScene = false)
}

private fun submitWidth(model: Model, widget: SettingsWidget, value: String) {
    val width = value.toIntOrNull() ?: return
    widget.crop.width = width
    model.resetSelectedScene(changeScene = false)
}

private fun submitHeight(model: Model, widget: SettingsWidget, value: String) {
    val height = value.toIntOrNull() ?: return
    if (height <= 0) {
        return
    }
    widget.crop.height = height
    model.resetSelectedScene(changeScene = false)
}

private fun sourceWidgetExists(model: Model, widget: SettingsWidget): Boolean {
    return model.database.widgets.any { it.id == widget.crop.sourceWidgetId }
}

@Composable
fun WidgetCropSettingsView(model: Model = LocalModel.current, widget: SettingsWidget) {
    val database = model.database
    val browserWidgets = database.widgets.filter { it.type == SettingsWidgetType.browser }
    val exists = sourceWidgetExists(model, widget)
    val selectedId = if (exists) {
        widget.crop.sourceWidgetId
    } else {
        null
    }
    val options = if (exists) {
        browserWidgets.map { it.id }
    } else {
        listOf(null) + browserWidgets.map { it.id }
    }
    Section {
        Picker(
            title = "Source widget",
            selection = selectedId,
            options = options,
            text = { id -> browserWidgets.firstOrNull { it.id == id }?.name ?: "" },
            onChange = { value ->
                if (value != null) {
                    widget.crop.sourceWidgetId = value
                    model.resetSelectedScene(changeScene = false)
                }
            }
        )
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

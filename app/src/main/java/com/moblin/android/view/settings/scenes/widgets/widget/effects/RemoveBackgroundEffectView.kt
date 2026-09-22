package com.moblin.android.view.settings.scenes.widgets.widget.effects

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsVideoEffect
import com.moblin.android.various.settings.SettingsVideoEffectRemoveBackground
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.view.utils.RgbColorPickerView
import com.moblin.android.LocalModel

@Composable
fun RemoveBackgroundEffectView(
    model: Model = LocalModel.current,
    widget: SettingsWidget,
    effect: SettingsVideoEffect,
    removeBackground: SettingsVideoEffectRemoveBackground,
) {
    val fromColor = removeBackground.fromColor
    val toColor = removeBackground.toColor

    fun updateWidget() {
        TODO("model.getWidgetRemoveBackgroundEffect(widget, effect)?.setColorRange(from = removeBackground.from, to = removeBackground.to)")
    }

    Column {
        Text(
            text = "Color range",
            style = MaterialTheme.typography.titleSmall,
        )
        RgbColorPickerView(
            title = "From",
            color = fromColor,
            onColorChanged = { color ->
                removeBackground.fromColor = color
            },
            onChange = { color ->
                removeBackground.from = color
                updateWidget()
            },
        )
        RgbColorPickerView(
            title = "To",
            color = toColor,
            onColorChanged = { color ->
                removeBackground.toColor = color
            },
            onChange = { color ->
                removeBackground.to = color
                updateWidget()
            },
        )
    }
}

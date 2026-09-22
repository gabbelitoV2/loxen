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
    val fromColor by removeBackground.fromColor.collectAsState()
    val toColor by removeBackground.toColor.collectAsState()

    fun updateWidget() {
        model.getWidgetRemoveBackgroundEffect(widget, effect)?.setColorRange(
            from = removeBackground.from.value,
            to = removeBackground.to.value,
        )
    }

    Column {
        Text(
            text = "Color range",
            style = MaterialTheme.typography.titleSmall,
        )
        RgbColorPickerView(
            title = "From",
            color = fromColor,
            onChange = { color ->
                removeBackground.fromColor.value = color
                removeBackground.from.value = color
                updateWidget()
            },
        )
        RgbColorPickerView(
            title = "To",
            color = toColor,
            onChange = { color ->
                removeBackground.toColor.value = color
                removeBackground.to.value = color
                updateWidget()
            },
        )
    }
}

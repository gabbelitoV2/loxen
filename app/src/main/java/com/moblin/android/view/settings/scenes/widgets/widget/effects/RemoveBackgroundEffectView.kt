package com.moblin.android.view.settings.scenes.widgets.widget.effects

import androidx.compose.runtime.Composable
import com.moblin.android.LocalModel
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsVideoEffect
import com.moblin.android.various.settings.SettingsVideoEffectRemoveBackground
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.view.utils.RgbColorPickerView
import com.moblin.android.various.model.getWidgetRemoveBackgroundEffect

@Composable
fun RemoveBackgroundEffectView(
    model: Model = LocalModel.current,
    widget: SettingsWidget,
    effect: SettingsVideoEffect,
    removeBackground: SettingsVideoEffectRemoveBackground,
) {
    fun updateWidget() {
        model.getWidgetRemoveBackgroundEffect(widget, effect)?.setColorRange(
            removeBackground.from,
            removeBackground.to,
        )
    }

    Section(header = "Color range") {
        RgbColorPickerView(
            title = "From",
            color = removeBackground.fromColor,
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
            color = removeBackground.toColor,
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

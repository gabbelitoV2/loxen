package com.moblin.android.view.settings.scenes.widgets.widget.effects

import androidx.compose.runtime.Composable
import com.moblin.android.LocalModel
import com.moblin.android.platform.swiftui.FormSlider
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsVideoEffect
import com.moblin.android.various.settings.SettingsVideoEffectOpacity
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.model.getWidgetOpacityEffect

@Composable
fun OpacityEffectView(
    model: Model = LocalModel.current,
    widget: SettingsWidget,
    effect: SettingsVideoEffect,
    opacity: SettingsVideoEffectOpacity
) {
    fun updateWidget() {
        model.getWidgetOpacityEffect(widget, effect)?.setOpacity(opacity.opacity)
    }

    Section {
        FormSlider(
            value = opacity.opacity.toFloat(),
            onValueChange = { value ->
                opacity.opacity = value.toDouble()
                updateWidget()
            },
            valueRange = 0f..1f
        )
    }
}

package com.moblin.android.view.settings.scenes.widgets.widget.effects

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsVideoEffect
import com.moblin.android.various.settings.SettingsVideoEffectOpacity
import com.moblin.android.various.settings.SettingsWidget

@Composable
fun OpacityEffectView(
    model: Model,
    widget: SettingsWidget,
    effect: SettingsVideoEffect,
    opacity: SettingsVideoEffectOpacity
) {
    val opacityValue by opacity.opacity.collectAsState()

    fun updateWidget() {
        model.getWidgetOpacityEffect(widget, effect)?.setOpacity(opacity = opacity.opacity.value)
    }

    LaunchedEffect(opacityValue) {
        updateWidget()
    }

    Column {
        Slider(
            value = opacityValue,
            onValueChange = { value ->
                opacity.setOpacity(value)
            },
            valueRange = 0f..1f
        )
    }
}

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
import com.moblin.android.LocalModel

@Composable
fun OpacityEffectView(
    model: Model = LocalModel.current,
    widget: SettingsWidget,
    effect: SettingsVideoEffect,
    opacity: SettingsVideoEffectOpacity
) {
    val opacityValue = opacity.opacity

    fun updateWidget() {
        effect.opacity.opacity = opacityValue
    }

    LaunchedEffect(opacityValue) {
        updateWidget()
    }

    Column {
        Slider(
            value = opacityValue.toFloat(),
            onValueChange = { value ->
                opacity.opacity = value.toDouble()
            },
            valueRange = 0f..1f
        )
    }
}

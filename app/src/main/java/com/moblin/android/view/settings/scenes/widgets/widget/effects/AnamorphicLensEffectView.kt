package com.moblin.android.view.settings.scenes.widgets.widget.effects

import androidx.compose.runtime.Composable
import com.moblin.android.LocalModel
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsVideoEffect
import com.moblin.android.various.settings.SettingsVideoEffectAnamorphicLens
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.various.model.getWidgetAnamorphicLensEffect

@Composable
fun AnamorphicLensEffectView(
    model: Model = LocalModel.current,
    widget: SettingsWidget,
    effect: SettingsVideoEffect,
    anamorphicLens: SettingsVideoEffectAnamorphicLens
) {
    Section {
        TextEditNavigationView(
            title = localized("Desqueeze factor"),
            value = anamorphicLens.scale.toString(),
            onChange = ::changeScale,
            onSubmit = { value ->
                submitScale(
                    value = value,
                    model = model,
                    widget = widget,
                    effect = effect,
                    anamorphicLens = anamorphicLens
                )
            },
            valueFormat = { "${it}x" }
        )
    }
}

private fun updateWidget(
    model: Model,
    widget: SettingsWidget,
    effect: SettingsVideoEffect,
    anamorphicLens: SettingsVideoEffectAnamorphicLens
) {
    model.getWidgetAnamorphicLensEffect(widget, effect)?.setSettings(anamorphicLens.clone())
}

private fun changeScale(value: String): String? {
    val scale = value.toDoubleOrNull() ?: return localized("Not a number")
    if (scale <= 0) {
        return localized("Too small")
    }
    if (scale > 10) {
        return localized("Too big")
    }
    return null
}

private fun submitScale(
    value: String,
    model: Model,
    widget: SettingsWidget,
    effect: SettingsVideoEffect,
    anamorphicLens: SettingsVideoEffectAnamorphicLens
) {
    val scale = value.toDoubleOrNull() ?: return
    anamorphicLens.scale = scale
    updateWidget(
        model = model,
        widget = widget,
        effect = effect,
        anamorphicLens = anamorphicLens
    )
}

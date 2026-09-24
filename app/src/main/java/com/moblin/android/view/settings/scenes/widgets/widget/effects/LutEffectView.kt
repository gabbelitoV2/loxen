package com.moblin.android.view.settings.scenes.widgets.widget.effects

import androidx.compose.runtime.Composable
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.platform.swiftui.Label
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.PickerStyle
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsColor
import com.moblin.android.various.settings.SettingsVideoEffect
import com.moblin.android.various.settings.SettingsVideoEffectLut
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.view.settings.camera.CameraSettingsLutsView
import com.moblin.android.view.utils.ShortcutSectionView
import java.util.UUID
import com.moblin.android.various.model.getLogLutById
import com.moblin.android.various.model.getWidgetLutEffect

private fun updateWidget(
    model: Model,
    widget: SettingsWidget,
    effect: SettingsVideoEffect,
    lut: SettingsVideoEffectLut
) {
    val logLut = model.getLogLutById(lut.lut)
    model.getWidgetLutEffect(widget, effect)?.setLut(
        lut = logLut,
        imageStorage = model.imageStorage
    ) { title, subTitle ->
        model.makeErrorToast(title = title, subTitle = subTitle)
    }
}

@Composable
fun LutEffectView(
    model: Model = LocalModel.current,
    color: SettingsColor,
    widget: SettingsWidget,
    effect: SettingsVideoEffect,
    lut: SettingsVideoEffectLut,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    val luts = color.allLuts()
    val options: List<UUID?> = listOf(null) + luts.map { it.id }
    Section(header = "LUT") {
        Picker(
            title = "",
            selection = lut.lut,
            options = options,
            text = { id ->
                if (id == null) {
                    "-- None --"
                } else {
                    luts.firstOrNull { it.id == id }?.name ?: "-- None --"
                }
            },
            pickerStyle = PickerStyle.inline,
            onChange = { newValue ->
                lut.lut = newValue
                updateWidget(
                    model = model,
                    widget = widget,
                    effect = effect,
                    lut = lut
                )
            }
        )
    }
    ShortcutSectionView {
        NavigationLink(
            destination = {
                CameraSettingsLutsView(color = color)
            }
        ) {
            Label("LUTs", systemImage = "camera")
        }
    }
}

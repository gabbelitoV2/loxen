package com.moblin.android.view.settings.scenes.widgets.widget.alerts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.FormSlider
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsFontDesign
import com.moblin.android.various.settings.SettingsFontWeight
import com.moblin.android.various.settings.SettingsWidgetAlertPositionType
import com.moblin.android.various.settings.SettingsWidgetAlertsAlert
import com.moblin.android.view.utils.RgbColorPickerView

@Composable
fun AlertFontView(
    model: Model = LocalModel.current,
    alert: SettingsWidgetAlertsAlert,
    fontSize: Float,
    fontDesign: SettingsFontDesign,
    fontWeight: SettingsFontWeight,
) {
    var fontSizeState by remember { mutableStateOf(fontSize) }
    var fontDesignState by remember { mutableStateOf(fontDesign) }
    var fontWeightState by remember { mutableStateOf(fontWeight) }

    if (alert.positionType == SettingsWidgetAlertPositionType.scene) {
        Section(header = "Font") {
            FormRow {
                Text(text = "Size")
                FormSlider(
                    value = fontSizeState,
                    onValueChange = { value ->
                        fontSizeState = value
                        alert.fontSize = value.toInt()
                        model.updateAlertsSettings()
                    },
                    modifier = Modifier.weight(1f),
                    valueRange = 10f..80f,
                )
                Box(
                    modifier = Modifier.width(35.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = fontSizeState.toInt().toString())
                }
            }
            Picker(
                title = "Design",
                selection = fontDesignState,
                options = SettingsFontDesign.entries,
                onChange = { value ->
                    fontDesignState = value
                    alert.fontDesign = value
                    model.updateAlertsSettings()
                },
            )
            Picker(
                title = "Weight",
                selection = fontWeightState,
                options = SettingsFontWeight.entries,
                onChange = { value ->
                    fontWeightState = value
                    alert.fontWeight = value
                    model.updateAlertsSettings()
                },
            )
        }
    }
}

@Composable
fun AlertColorsView(
    model: Model = LocalModel.current,
    alert: SettingsWidgetAlertsAlert,
    textColor: Color,
    accentColor: Color,
) {
    var textColorState by remember { mutableStateOf(textColor) }
    var accentColorState by remember { mutableStateOf(accentColor) }

    if (alert.positionType == SettingsWidgetAlertPositionType.scene) {
        Section(header = "Colors") {
            RgbColorPickerView(
                title = "Text",
                color = textColorState,
                onColorChanged = { color ->
                    textColorState = color
                },
                onChange = { rgbColor ->
                    alert.textColor = rgbColor
                    model.updateAlertsSettings()
                },
            )
            RgbColorPickerView(
                title = "Accent",
                color = accentColorState,
                onColorChanged = { color ->
                    accentColorState = color
                },
                onChange = { rgbColor ->
                    alert.accentColor = rgbColor
                    model.updateAlertsSettings()
                },
            )
        }
    }
}

package com.moblin.android.view.settings.scenes.widgets.widget.alerts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsFontDesign
import com.moblin.android.various.settings.SettingsFontWeight
import com.moblin.android.various.settings.SettingsWidgetAlertPositionType
import com.moblin.android.various.settings.SettingsWidgetAlertsAlert
import com.moblin.android.view.utils.RgbColorPickerView
import com.moblin.android.LocalModel

@OptIn(ExperimentalMaterial3Api::class)
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
    var designMenuExpanded by remember { mutableStateOf(false) }
    var weightMenuExpanded by remember { mutableStateOf(false) }

    if (alert.positionType == SettingsWidgetAlertPositionType.scene) {
        LaunchedEffect(fontSizeState) {
            alert.fontSize = fontSizeState.toInt()
            model.updateAlertsSettings()
        }
        LaunchedEffect(fontDesignState) {
            alert.fontDesign = fontDesignState
            model.updateAlertsSettings()
        }
        LaunchedEffect(fontWeightState) {
            alert.fontWeight = fontWeightState
            model.updateAlertsSettings()
        }
        Column {
            Text(
                text = "Font",
                style = MaterialTheme.typography.titleMedium,
            )
            Row {
                Text(text = "Size")
                Slider(
                    value = fontSizeState,
                    onValueChange = { fontSizeState = it },
                    valueRange = 10f..80f,
                    steps = 13,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = fontSizeState.toInt().toString(),
                    modifier = Modifier.width(35.dp),
                )
            }
            ExposedDropdownMenuBox(
                expanded = designMenuExpanded,
                onExpandedChange = { designMenuExpanded = it },
            ) {
                OutlinedTextField(
                    value = fontDesignState.toString(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(text = "Design") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = designMenuExpanded)
                    },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                )
                ExposedDropdownMenu(
                    expanded = designMenuExpanded,
                    onDismissRequest = { designMenuExpanded = false },
                ) {
                    SettingsFontDesign.entries.forEach { design ->
                        DropdownMenuItem(
                            text = { Text(text = design.toString()) },
                            onClick = {
                                fontDesignState = design
                                designMenuExpanded = false
                            },
                        )
                    }
                }
            }
            ExposedDropdownMenuBox(
                expanded = weightMenuExpanded,
                onExpandedChange = { weightMenuExpanded = it },
            ) {
                OutlinedTextField(
                    value = fontWeightState.toString(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(text = "Weight") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = weightMenuExpanded)
                    },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                )
                ExposedDropdownMenu(
                    expanded = weightMenuExpanded,
                    onDismissRequest = { weightMenuExpanded = false },
                ) {
                    SettingsFontWeight.entries.forEach { weight ->
                        DropdownMenuItem(
                            text = { Text(text = weight.toString()) },
                            onClick = {
                                fontWeightState = weight
                                weightMenuExpanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
        Column {
            Text(
                text = "Colors",
                style = MaterialTheme.typography.titleMedium,
            )
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

package com.moblin.android.view.settings.scenes.widgets.widget.effects

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsColor
import com.moblin.android.various.settings.SettingsVideoEffect
import com.moblin.android.various.settings.SettingsVideoEffectLut
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.view.utils.ShortcutSectionView
import java.util.UUID

private fun updateWidget(
    model: Model,
    widget: SettingsWidget,
    effect: SettingsVideoEffect,
    lut: SettingsVideoEffectLut,
    lutId: UUID?
) {
    val logLut = model.getLogLutById(id = lutId)
    model.getWidgetLutEffect(widget, effect)?.setLut(
        lut = logLut,
        imageStorage = model.imageStorage
    ) { title, subTitle ->
        model.makeErrorToast(title = title, subTitle = subTitle)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LutEffectView(
    model: Model,
    color: SettingsColor,
    widget: SettingsWidget,
    effect: SettingsVideoEffect,
    lut: SettingsVideoEffectLut,
    onNavigate: (String) -> Unit
) {
    var selectedLutId by remember { mutableStateOf(lut.lut) }
    var initialized by remember { mutableStateOf(false) }
    LaunchedEffect(selectedLutId) {
        if (initialized) {
            updateWidget(
                model = model,
                widget = widget,
                effect = effect,
                lut = lut,
                lutId = selectedLutId
            )
        } else {
            initialized = true
        }
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("LUT", style = MaterialTheme.typography.titleSmall)
        var expanded by remember { mutableStateOf(false) }
        val allLuts = color.allLuts()
        val selectedName = allLuts.firstOrNull { it.id == selectedLutId }?.name ?: "-- None --"
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selectedName,
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("-- None --") },
                    onClick = {
                        selectedLutId = null
                        lut.lut = null
                        expanded = false
                    }
                )
                allLuts.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item.name) },
                        onClick = {
                            selectedLutId = item.id
                            lut.lut = item.id
                            expanded = false
                        }
                    )
                }
            }
        }
        ShortcutSectionView {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate("CameraSettingsLutsView") }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("LUTs")
            }
        }
    }
}

package com.moblin.android.view.settings.scenes.widgets.widget.snapshot

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetSnapshot
import com.moblin.android.view.settings.scenes.widgets.widget.effects.WidgetEffectsView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetSnapshotSettingsView(
    model: Model,
    widget: SettingsWidget,
    snapshot: SettingsWidgetSnapshot,
) {
    val showtime by snapshot.showtime.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = "${showtime}s",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Showtime") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    for (item in listOf(3, 5, 10, 15, 30, 60, 120)) {
                        DropdownMenuItem(
                            text = { Text("${item}s") },
                            onClick = {
                                TODO("set SettingsWidgetSnapshot.showtime")
                                expanded = false
                            },
                        )
                    }
                }
            }
        }
        LaunchedEffect(showtime) {
            setEffectSettings(model, widget, showtime)
        }
        WidgetEffectsView(model = model, widget = widget)
    }
}

private fun setEffectSettings(model: Model, widget: SettingsWidget, showtime: Int) {
    model.getSnapshotEffect(id = widget.id)?.setSettings(showtime = showtime)
}

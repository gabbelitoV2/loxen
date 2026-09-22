package com.moblin.android.view.settings.scenes.widgets.widget.scoreboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.formatShortDuration
import com.moblin.android.localized
import com.moblin.android.various.settings.SettingsWidgetGenericScoreboardClockDirection
import com.moblin.android.various.settings.SettingsWidgetModularScoreboard
import com.moblin.android.various.settings.SettingsWidgetModularScoreboardTeam
import com.moblin.android.various.settings.SettingsWidgetScoreboardClock
import com.moblin.android.various.settings.SettingsWidgetScoreboardLayout
import com.moblin.android.view.utils.RgbColorPickerView
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.LocalOnNavigate

@Composable
private fun TeamView(
    side: String,
    team: SettingsWidgetModularScoreboardTeam,
    updated: () -> Unit,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate(side) },
    ) {
        Text(side)
        Spacer(modifier = Modifier.weight(1f))
        Text(team.name, color = Color.Gray)
    }
}

@Composable
fun TeamViewDetail(
    side: String,
    team: SettingsWidgetModularScoreboardTeam,
    updated: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TextEditNavigationView(
            title = localized("Name"),
            value = team.name,
            onChange = {
                team.name = it
                updated()
                null
            },
            onSubmit = {},
        )
        RgbColorPickerView(
            title = "Text",
            color = team.textColorColor,
            onColorChanged = {},
            onChange = {
                team.textColor = it
                updated()
            },
        )
        RgbColorPickerView(
            title = "Background",
            color = team.backgroundColorColor,
            onColorChanged = {},
            onChange = {
                team.backgroundColor = it
                updated()
            },
        )
    }
}

private fun isValidClockMaximum(value: String): String? {
    val maximum = value.toIntOrNull() ?: return localized("Not a number")
    if (maximum <= 0) {
        return localized("Too small")
    }
    if (maximum > 180) {
        return localized("Too big")
    }
    return null
}

private fun submitClockMaximum(clock: SettingsWidgetScoreboardClock, value: String) {
    val maximum = value.toIntOrNull() ?: return
    clock.maximum = maximum
}

private fun formatMaximum(value: String): String {
    val maximum = value.toIntOrNull() ?: return ""
    return formatShortDuration(60 * maximum)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetScoreboardModularSettingsView(
    modular: SettingsWidgetModularScoreboard,
    clock: SettingsWidgetScoreboardClock,
    updated: () -> Unit,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Teams")
        TeamView(
            side = localized("Home"),
            team = modular.home,
            updated = updated,
            onNavigate = onNavigate,
        )
        TeamView(
            side = localized("Away"),
            team = modular.away,
            updated = updated,
            onNavigate = onNavigate,
        )

        Text("Clock")
        TextEditNavigationView(
            title = localized("Maximum"),
            value = clock.maximum.toString(),
            onChange = { isValidClockMaximum(it) },
            onSubmit = {
                submitClockMaximum(clock, it)
                clock.reset()
                updated()
            },
            valueFormat = { formatMaximum(it) },
        )
        var directionExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = directionExpanded,
            onExpandedChange = { directionExpanded = it },
        ) {
            OutlinedTextField(
                value = clock.direction.toString(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Direction") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = directionExpanded)
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
            )
            ExposedDropdownMenu(
                expanded = directionExpanded,
                onDismissRequest = { directionExpanded = false },
            ) {
                SettingsWidgetGenericScoreboardClockDirection.entries.forEach { direction ->
                    DropdownMenuItem(
                        text = { Text(direction.toString()) },
                        onClick = {
                            clock.direction = direction
                            clock.reset()
                            updated()
                            directionExpanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun WidgetScoreboardModularGeneralSettingsView(
    modular: SettingsWidgetModularScoreboard,
    updated: () -> Unit,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Text(
        text = "Layout",
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("Layout") },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetScoreboardModularLayoutDetailView(
    modular: SettingsWidgetModularScoreboard,
    updated: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        var typeExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = typeExpanded,
            onExpandedChange = { typeExpanded = it },
        ) {
            OutlinedTextField(
                value = modular.layout.toString(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Type") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded)
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
            )
            ExposedDropdownMenu(
                expanded = typeExpanded,
                onDismissRequest = { typeExpanded = false },
            ) {
                SettingsWidgetScoreboardLayout.entries.forEach { layout ->
                    DropdownMenuItem(
                        text = { Text(layout.toString()) },
                        onClick = {
                            modular.layout = layout
                            updated()
                            typeExpanded = false
                        },
                    )
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            Text("Width")
            Slider(
                value = modular.width.toFloat(),
                onValueChange = {
                    modular.width = it
                    updated()
                },
                valueRange = 100f..1000f,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = modular.width.toInt().toString(),
                modifier = Modifier.width(35.dp),
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Text("Height")
            Slider(
                value = modular.rowHeight.toFloat(),
                onValueChange = {
                    modular.rowHeight = it
                    updated()
                },
                valueRange = 10f..150f,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = modular.rowHeight.toInt().toString(),
                modifier = Modifier.width(35.dp),
            )
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            Text("Title")
            Switch(
                checked = modular.showTitle,
                onCheckedChange = {
                    modular.showTitle = it
                    updated()
                },
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Text("More stats")
            Switch(
                checked = modular.showMoreStats,
                onCheckedChange = {
                    modular.showMoreStats = it
                    updated()
                },
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Text("Info box")
            Switch(
                checked = modular.showGlobalStatsBlock,
                onCheckedChange = {
                    modular.showGlobalStatsBlock = it
                    updated()
                },
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Text("Clock")
            Switch(
                checked = modular.showClock,
                onCheckedChange = {
                    modular.showClock = it
                    updated()
                },
                enabled = modular.showGlobalStatsBlock,
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Text("Bold")
            Switch(
                checked = modular.isBold,
                onCheckedChange = {
                    modular.isBold = it
                    updated()
                },
            )
        }
    }
}

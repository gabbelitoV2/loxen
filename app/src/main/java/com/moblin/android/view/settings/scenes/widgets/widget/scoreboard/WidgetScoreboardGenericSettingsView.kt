package com.moblin.android.view.settings.scenes.widgets.widget.scoreboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.moblin.android.common.various.formatShortDuration
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetGenericScoreboard
import com.moblin.android.various.settings.SettingsWidgetGenericScoreboardClockDirection
import com.moblin.android.various.settings.SettingsWidgetScoreboard
import com.moblin.android.various.settings.SettingsWidgetScoreboardClock
import com.moblin.android.view.settings.scenes.widgets.widget.text.TimeButtonView
import com.moblin.android.view.settings.scenes.widgets.widget.text.TimeComponentPickerView
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.LocalModel

@Composable
private fun TimePickerView(
    model: Model = LocalModel.current,
    widget: SettingsWidget,
    clock: SettingsWidgetScoreboardClock,
    presenting: Boolean,
    onPresentingChange: (Boolean) -> Unit
) {
    var minutes by remember { mutableStateOf(0) }
    var seconds by remember { mutableStateOf(0) }

    Column(modifier = Modifier.padding(16.dp)) {
        Row(modifier = Modifier.padding(16.dp)) {
            TimeComponentPickerView(
                title = "Minutes",
                range = 0 until 120,
                time = minutes,
                onChange = { minutes = it }
            )
            TimeComponentPickerView(
                title = "Seconds",
                range = 0 until 60,
                time = seconds,
                onChange = { seconds = it }
            )
        }
        Row(modifier = Modifier.padding(16.dp)) {
            TimeButtonView(text = "Set") {
                TODO("handleUpdateGenericScoreboard")
                onPresentingChange(false)
            }
            TimeButtonView(text = "Cancel") {
                onPresentingChange(false)
            }
        }
    }
    LaunchedEffect(Unit) {
        minutes = clock.minutes
        seconds = clock.seconds
    }
}

@Composable
private fun ScoreboardSetClockButtonView(
    model: Model = LocalModel.current,
    widget: SettingsWidget,
    clock: SettingsWidgetScoreboardClock
) {
    var presenting by remember { mutableStateOf(false) }
    IconButton(onClick = { presenting = true }) {
        Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = null,
            modifier = Modifier.size(28.dp)
        )
    }
    if (presenting) {
        Dialog(onDismissRequest = { presenting = false }) {
            TimePickerView(
                model = model,
                widget = widget,
                clock = clock,
                presenting = presenting,
                onPresentingChange = { presenting = it }
            )
        }
    }
}

@Composable
private fun ScoreboardStartStopClockButtonView(clock: SettingsWidgetScoreboardClock) {
    IconButton(onClick = { clock.isStopped = !clock.isStopped }) {
        Icon(
            imageVector = if (clock.isStopped) Icons.Default.PlayArrow else Icons.Default.Stop,
            contentDescription = null,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
fun ScoreboardUndoButtonView(action: () -> Unit) {
    IconButton(onClick = action) {
        Icon(
            imageVector = Icons.Default.Undo,
            contentDescription = null,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
fun ScoreboardIncrementButtonView(action: () -> Unit) {
    IconButton(onClick = action) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
fun ScoreboardResetScoreButtonView(action: () -> Unit) {
    var presentingResetConfirimation by remember { mutableStateOf(false) }
    IconButton(onClick = { presentingResetConfirimation = true }) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = null,
            tint = Color.Red,
            modifier = Modifier.size(28.dp)
        )
    }
    if (presentingResetConfirimation) {
        AlertDialog(
            onDismissRequest = { presentingResetConfirimation = false },
            title = { Text("") },
            confirmButton = {
                TextButton(onClick = {
                    presentingResetConfirimation = false
                    action()
                }) {
                    Text(localized("Reset score"))
                }
            },
            dismissButton = {
                TextButton(onClick = { presentingResetConfirimation = false }) {
                    Text(localized("Cancel"))
                }
            }
        )
    }
}

@Composable
fun WidgetScoreboardGenericQuickButtonControlsView(model: Model = LocalModel.current, widget: SettingsWidget) {
    Row(
        modifier = Modifier.height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
            ScoreboardStartStopClockButtonView(clock = widget.scoreboard.generic.clock)
            ScoreboardSetClockButtonView(
                model = model,
                widget = widget,
                clock = widget.scoreboard.generic.clock
            )
        }
        VerticalDivider()
        Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
            ScoreboardUndoButtonView {
                TODO("handleUpdateGenericScoreboard")
            }
            ScoreboardResetScoreButtonView {
                TODO("handleUpdateGenericScoreboard")
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
            ScoreboardIncrementButtonView {
                TODO("handleUpdateGenericScoreboard")
            }
            ScoreboardIncrementButtonView {
                TODO("handleUpdateGenericScoreboard")
            }
        }
    }
}

@Composable
fun WidgetScoreboardGenericGeneralSettingsView(
    widget: SettingsWidget,
    scoreboard: SettingsWidgetScoreboard,
    generic: SettingsWidgetGenericScoreboard,
    updated: () -> Unit
) {
    TextEditNavigationView(
        title = localized("Title"),
        value = generic.title,
        onChange = { title -> generic.title = title; null },
        onSubmit = { title -> generic.title = title }
    )
    LaunchedEffect(generic.title) {
        updated()
    }
    ScoreboardColorsView(scoreboard = scoreboard, updated = updated)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetScoreboardGenericSettingsView(
    generic: SettingsWidgetGenericScoreboard,
    clock: SettingsWidgetScoreboardClock,
    updated: () -> Unit
) {
    fun isValidClockMaximum(value: String): String? {
        val maximum = value.toIntOrNull() ?: return localized("Not a number")
        if (maximum <= 0) {
            return localized("Too small")
        }
        if (maximum > 180) {
            return localized("Too big")
        }
        return null
    }

    fun submitClockMaximum(value: String) {
        val maximum = value.toIntOrNull() ?: return
        clock.maximum = maximum
    }

    fun formatMaximum(value: String): String {
        val maximum = value.toIntOrNull() ?: return ""
        return formatShortDuration(60 * maximum)
    }

    Column {
        Text("Teams", style = MaterialTheme.typography.titleSmall)
        TextEditNavigationView(
            title = localized("Home"),
            value = generic.home,
            onChange = { home -> generic.home = home; null },
            onSubmit = { home -> generic.home = home }
        )
        LaunchedEffect(generic.home) {
            updated()
        }
        TextEditNavigationView(
            title = localized("Away"),
            value = generic.away,
            onChange = { away -> generic.away = away; null },
            onSubmit = { away -> generic.away = away }
        )
        LaunchedEffect(generic.away) {
            updated()
        }
        Text("Clock", style = MaterialTheme.typography.titleSmall)
        TextEditNavigationView(
            title = localized("Maximum"),
            value = clock.maximum.toString(),
            onChange = { value -> isValidClockMaximum(value) },
            onSubmit = { value -> submitClockMaximum(value) },
            valueFormat = { value -> formatMaximum(value) }
        )
        LaunchedEffect(clock.maximum) {
            clock.reset()
            updated()
        }
        var directionExpanded by remember { mutableStateOf(false) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(localized("Direction"))
            ExposedDropdownMenuBox(
                expanded = directionExpanded,
                onExpandedChange = { directionExpanded = it }
            ) {
                OutlinedTextField(
                    value = clock.direction.toString(),
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.menuAnchor(),
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = directionExpanded)
                    }
                )
                ExposedDropdownMenu(
                    expanded = directionExpanded,
                    onDismissRequest = { directionExpanded = false }
                ) {
                    SettingsWidgetGenericScoreboardClockDirection.entries.forEach { direction ->
                        DropdownMenuItem(
                            text = { Text(direction.toString()) },
                            onClick = {
                                clock.direction = direction
                                directionExpanded = false
                            }
                        )
                    }
                }
            }
        }
        LaunchedEffect(clock.direction) {
            clock.reset()
            updated()
        }
    }
}

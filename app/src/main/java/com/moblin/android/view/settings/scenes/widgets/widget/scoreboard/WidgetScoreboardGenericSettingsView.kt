package com.moblin.android.view.settings.scenes.widgets.widget.scoreboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalModel
import com.moblin.android.common.various.formatShortDuration
import com.moblin.android.localized
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Sheet
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetGenericScoreboard
import com.moblin.android.various.settings.SettingsWidgetGenericScoreboardClockDirection
import com.moblin.android.various.settings.SettingsWidgetScoreboard
import com.moblin.android.various.settings.SettingsWidgetScoreboardClock
import com.moblin.android.view.settings.scenes.widgets.widget.text.TimeButtonView
import com.moblin.android.view.settings.scenes.widgets.widget.text.TimeComponentPickerView
import com.moblin.android.view.utils.TextEditNavigationView

@Composable
private fun OnChange(value: Any?, onChange: () -> Unit) {
    var previous by remember { mutableStateOf(value) }
    LaunchedEffect(value) {
        if (previous != value) {
            previous = value
            onChange()
        }
    }
}

@Composable
private fun ScoreboardIconButton(
    name: String,
    tint: Color? = null,
    action: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    SystemImage(
        name = name,
        fontSize = 28.sp,
        modifier = Modifier
            .alpha(if (pressed) 0.2f else 1f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = action
            ),
        tint = tint ?: Color.Unspecified
    )
}

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

    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimeButtonView(text = "Set") {
                Unit
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

    ScoreboardIconButton(name = "clock") {
        presenting = true
    }
    if (presenting) {
        Sheet(onDismissRequest = { presenting = false }) {
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
    ScoreboardIconButton(name = if (clock.isStopped) "play" else "stop") {
        clock.isStopped = !clock.isStopped
    }
}

@Composable
fun ScoreboardUndoButtonView(action: () -> Unit) {
    ScoreboardIconButton(name = "arrow.uturn.backward", action = action)
}

@Composable
fun ScoreboardIncrementButtonView(action: () -> Unit) {
    ScoreboardIconButton(name = "plus", action = action)
}

@Composable
fun ScoreboardResetScoreButtonView(action: () -> Unit) {
    var presentingResetConfirimation by remember { mutableStateOf(false) }

    ScoreboardIconButton(name = "trash", tint = formPalette().red) {
        presentingResetConfirimation = true
    }
    if (presentingResetConfirimation) {
        AlertDialog(
            onDismissRequest = { presentingResetConfirimation = false },
            confirmButton = {
                TextButton(onClick = {
                    presentingResetConfirimation = false
                    action()
                }) {
                    Text(localized("Reset score"), color = formPalette().red)
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
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(formPalette().separator)
        )
        Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
            ScoreboardUndoButtonView {
                Unit
            }
            ScoreboardResetScoreButtonView {
                Unit
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
            ScoreboardIncrementButtonView {
                Unit
            }
            ScoreboardIncrementButtonView {
                Unit
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
    OnChange(generic.title) {
        updated()
    }
    ScoreboardColorsView(scoreboard = scoreboard, updated = updated)
}

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
        return formatShortDuration(seconds = 60 * maximum)
    }

    Section(header = localized("Teams")) {
        TextEditNavigationView(
            title = localized("Home"),
            value = generic.home,
            onChange = { home -> generic.home = home; null },
            onSubmit = { home -> generic.home = home }
        )
        OnChange(generic.home) {
            updated()
        }
        TextEditNavigationView(
            title = localized("Away"),
            value = generic.away,
            onChange = { away -> generic.away = away; null },
            onSubmit = { away -> generic.away = away }
        )
        OnChange(generic.away) {
            updated()
        }
    }
    Section(header = localized("Clock")) {
        TextEditNavigationView(
            title = localized("Maximum"),
            value = clock.maximum.toString(),
            onChange = { value -> isValidClockMaximum(value) },
            onSubmit = { value -> submitClockMaximum(value) },
            valueFormat = { value -> formatMaximum(value) }
        )
        OnChange(clock.maximum) {
            clock.reset()
            updated()
        }
        Picker(
            title = localized("Direction"),
            selection = clock.direction,
            options = SettingsWidgetGenericScoreboardClockDirection.entries,
            onChange = { clock.direction = it }
        )
        OnChange(clock.direction) {
            clock.reset()
            updated()
        }
    }
}

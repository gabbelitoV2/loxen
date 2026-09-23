package com.moblin.android.view.settings.scenes.widgets.widget.scoreboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalOnNavigate
import com.moblin.android.common.various.formatShortDuration
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormSlider
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.settings.SettingsWidgetGenericScoreboardClockDirection
import com.moblin.android.various.settings.SettingsWidgetModularScoreboard
import com.moblin.android.various.settings.SettingsWidgetModularScoreboardTeam
import com.moblin.android.various.settings.SettingsWidgetScoreboardClock
import com.moblin.android.various.settings.SettingsWidgetScoreboardLayout
import com.moblin.android.view.utils.RgbColorPickerView
import com.moblin.android.view.utils.TextEditNavigationView

@Composable
private fun TeamView(
    side: String,
    team: SettingsWidgetModularScoreboardTeam,
    updated: () -> Unit,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    NavigationLink(
        destination = {
            TeamViewDetail(side = side, team = team, updated = updated)
        },
    ) {
        Text(side)
        Spacer(modifier = Modifier.weight(1f))
        Text(team.name, color = formPalette().gray)
    }
}

@Composable
fun TeamViewDetail(
    side: String,
    team: SettingsWidgetModularScoreboardTeam,
    updated: () -> Unit,
) {
    Form(title = side) {
        Section {
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
                title = localized("Text"),
                color = team.textColorColor,
                onColorChanged = {},
                onChange = {
                    team.textColor = it
                    updated()
                },
            )
            RgbColorPickerView(
                title = localized("Background"),
                color = team.backgroundColorColor,
                onColorChanged = {},
                onChange = {
                    team.backgroundColor = it
                    updated()
                },
            )
        }
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

@Composable
fun WidgetScoreboardModularSettingsView(
    modular: SettingsWidgetModularScoreboard,
    clock: SettingsWidgetScoreboardClock,
    updated: () -> Unit,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Section(header = localized("Teams")) {
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
    }
    Section(header = localized("Clock")) {
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
        Picker(
            title = localized("Direction"),
            selection = clock.direction,
            options = SettingsWidgetGenericScoreboardClockDirection.entries,
            onChange = {
                clock.direction = it
                clock.reset()
                updated()
            },
        )
    }
}

@Composable
fun WidgetScoreboardModularGeneralSettingsView(
    modular: SettingsWidgetModularScoreboard,
    updated: () -> Unit,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    NavigationLink(
        title = localized("Layout"),
    ) {
        WidgetScoreboardModularLayoutDetailView(modular = modular, updated = updated)
    }
}

@Composable
fun WidgetScoreboardModularLayoutDetailView(
    modular: SettingsWidgetModularScoreboard,
    updated: () -> Unit,
) {
    Form(title = localized("Layout")) {
        Section {
            Picker(
                title = localized("Type"),
                selection = modular.layout,
                options = SettingsWidgetScoreboardLayout.entries,
                onChange = {
                    modular.layout = it
                    updated()
                },
            )
        }
        Section {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(localized("Width"))
                FormSlider(
                    value = modular.width.toFloat(),
                    onValueChange = {
                        modular.width = it
                        updated()
                    },
                    modifier = Modifier.weight(1f),
                    valueRange = 100f..1000f,
                )
                Box(
                    modifier = Modifier.width(35.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(modular.width.toInt().toString())
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(localized("Height"))
                FormSlider(
                    value = modular.rowHeight.toFloat(),
                    onValueChange = {
                        modular.rowHeight = it
                        updated()
                    },
                    modifier = Modifier.weight(1f),
                    valueRange = 10f..150f,
                )
                Box(
                    modifier = Modifier.width(35.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(modular.rowHeight.toInt().toString())
                }
            }
        }
        Section {
            Toggle(
                title = localized("Title"),
                isOn = modular.showTitle,
                onChange = {
                    modular.showTitle = it
                    updated()
                },
            )
            Toggle(
                title = localized("More stats"),
                isOn = modular.showMoreStats,
                onChange = {
                    modular.showMoreStats = it
                    updated()
                },
            )
            Toggle(
                title = localized("Info box"),
                isOn = modular.showGlobalStatsBlock,
                onChange = {
                    modular.showGlobalStatsBlock = it
                    updated()
                },
            )
            Toggle(
                title = localized("Clock"),
                isOn = modular.showClock,
                enabled = modular.showGlobalStatsBlock,
                onChange = {
                    modular.showClock = it
                    updated()
                },
            )
            Toggle(
                title = localized("Bold"),
                isOn = modular.isBold,
                onChange = {
                    modular.isBold = it
                    updated()
                },
            )
        }
    }
}

package com.moblin.android.view.settings.scenes.widgets.widget.scoreboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
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
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsRemoteControlWeb
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetScoreboard
import com.moblin.android.various.settings.SettingsWidgetScoreboardSport
import com.moblin.android.view.settings.remotecontrol.RemoteControlWebDefaultUrlView
import com.moblin.android.view.utils.RemoteControlWebShortcutView
import com.moblin.android.view.utils.RgbColorPickerView
import com.moblin.android.view.utils.TextButtonView

@Composable
fun WidgetScoreboardQuickButtonControlsView(
    model: Model,
    widget: SettingsWidget,
    scoreboard: SettingsWidgetScoreboard,
) {
    val sport by scoreboard.sport.collectAsState()
    when (sport) {
        SettingsWidgetScoreboardSport.GENERIC -> WidgetScoreboardGenericQuickButtonControlsView(
            model = model,
            widget = widget,
        )
        SettingsWidgetScoreboardSport.PADEL -> WidgetScoreboardPadelQuickButtonControlsView(
            model = model,
            widget = widget,
        )
        else -> Unit
    }
}

@Composable
fun ScoreboardColorsView(
    scoreboard: SettingsWidgetScoreboard,
    updated: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("Colors") }
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(text = localized("Colors"))
    }
}

@Composable
fun ScoreboardColorsFormView(
    scoreboard: SettingsWidgetScoreboard,
    updated: () -> Unit,
) {
    val textColorColor by scoreboard.textColorColor.collectAsState()
    val primaryBackgroundColorColor by scoreboard.primaryBackgroundColorColor.collectAsState()
    val secondaryBackgroundColorColor by scoreboard.secondaryBackgroundColorColor.collectAsState()
    Column(modifier = Modifier.fillMaxWidth()) {
        RgbColorPickerView(
            title = localized("Text"),
            color = textColorColor,
        ) { color ->
            scoreboard.textColor.value = color
            updated()
        }
        RgbColorPickerView(
            title = localized("Primary background"),
            color = primaryBackgroundColorColor,
        ) { color ->
            scoreboard.primaryBackgroundColor.value = color
            updated()
        }
        RgbColorPickerView(
            title = localized("Secondary background"),
            color = secondaryBackgroundColorColor,
        ) { color ->
            scoreboard.secondaryBackgroundColor.value = color
            updated()
        }
        TextButtonView(localized("Reset")) {
            scoreboard.resetColors()
            updated()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetScoreboardSettingsView(
    model: Model,
    widget: SettingsWidget,
    scoreboard: SettingsWidgetScoreboard,
    web: SettingsRemoteControlWeb,
) {
    val sport by scoreboard.sport.collectAsState()
    val modular by scoreboard.modular.collectAsState()
    val generic by scoreboard.generic.collectAsState()
    val padel by scoreboard.padel.collectAsState()
    val golf by scoreboard.golf.collectAsState()
    val webEnabled by web.enabled.collectAsState()
    var sportExpanded by remember { mutableStateOf(false) }

    val updated: () -> Unit = {
        when (sport) {
            SettingsWidgetScoreboardSport.GENERIC -> model.sendUpdateGenericScoreboardToWatch(
                id = widget.id,
                generic = generic,
            )
            SettingsWidgetScoreboardSport.PADEL -> model.sendUpdatePadelScoreboardToWatch(
                id = widget.id,
                padel = padel,
            )
            else -> Unit
        }
        model.remoteControlScoreboardUpdate(scoreboard = scoreboard)
        model.getScoreboardEffect(id = widget.id)?.update(
            scoreboard = scoreboard,
            config = model.getModularScoreboardConfig(scoreboard = scoreboard),
            players = model.database.scoreboardPlayers,
        )
    }

    LaunchedEffect(sport) {
        modular.config.value = null
        updated()
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = localized("Sport"),
                modifier = Modifier.weight(1f),
            )
            ExposedDropdownMenuBox(
                expanded = sportExpanded,
                onExpandedChange = { sportExpanded = it },
            ) {
                OutlinedTextField(
                    value = sport.toString(),
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = sportExpanded)
                    },
                    modifier = Modifier.menuAnchor(),
                )
                ExposedDropdownMenu(
                    expanded = sportExpanded,
                    onDismissRequest = { sportExpanded = false },
                ) {
                    SettingsWidgetScoreboardSport.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.toString()) },
                            onClick = {
                                scoreboard.sport.value = option
                                sportExpanded = false
                            },
                        )
                    }
                }
            }
        }
        when (sport) {
            SettingsWidgetScoreboardSport.PADEL -> WidgetScoreboardPadelGeneralSettingsView(
                widget = widget,
                scoreboard = scoreboard,
                padel = padel,
                updated = updated,
            )
            SettingsWidgetScoreboardSport.GENERIC -> WidgetScoreboardGenericGeneralSettingsView(
                widget = widget,
                scoreboard = scoreboard,
                generic = generic,
                updated = updated,
            )
            SettingsWidgetScoreboardSport.GOLF -> WidgetScoreboardGolfGeneralSettingsView(
                scoreboard = scoreboard,
                golf = golf,
                updated = updated,
            )
            SettingsWidgetScoreboardSport.GOLF_FULL_SCORECARD -> WidgetScoreboardGolfFullScorecardGeneralSettingsView(
                scoreboard = scoreboard,
                golf = golf,
                updated = updated,
            )
            else -> WidgetScoreboardModularGeneralSettingsView(
                modular = modular,
                updated = updated,
            )
        }
        Text(
            text = localized("Remote control"),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        when (sport) {
            SettingsWidgetScoreboardSport.PADEL, SettingsWidgetScoreboardSport.GENERIC -> Text(
                text = localized("Use your Apple Watch to update the scoreboard."),
            )
            SettingsWidgetScoreboardSport.GOLF -> {
                Text(
                    text = localized(
                        "Use the web based remote control on another device to update the scoreboard.",
                    ),
                )
                if (webEnabled) {
                    RemoteControlWebDefaultUrlView(
                        web = web,
                        status = model.statusOther,
                        path = "/golf.html",
                    )
                }
                RemoteControlWebShortcutView(model = model)
                if (!webEnabled) {
                    Text(text = localized("⚠️ The web based remote control is not enabled."))
                }
            }
            SettingsWidgetScoreboardSport.GOLF_FULL_SCORECARD -> Text(
                text = localized("Use a golf scoreboard widget to control this widget."),
            )
            else -> {
                Text(
                    text = localized(
                        "Use the web based remote control on another device to update the scoreboard.",
                    ),
                )
                if (webEnabled) {
                    RemoteControlWebDefaultUrlView(
                        web = web,
                        status = model.statusOther,
                        path = "/remote.html",
                    )
                }
                RemoteControlWebShortcutView(model = model)
                if (!webEnabled) {
                    Text(text = localized("⚠️ The web based remote control is not enabled."))
                }
            }
        }
        when (sport) {
            SettingsWidgetScoreboardSport.PADEL -> WidgetScoreboardPadelSettingsView(
                model = model,
                padel = padel,
                updated = updated,
            )
            SettingsWidgetScoreboardSport.GENERIC -> WidgetScoreboardGenericSettingsView(
                generic = generic,
                clock = generic.clock.value,
                updated = updated,
            )
            SettingsWidgetScoreboardSport.GOLF -> WidgetScoreboardGolfSettingsView(
                golf = golf,
                updated = updated,
            )
            SettingsWidgetScoreboardSport.GOLF_FULL_SCORECARD -> Unit
            else -> WidgetScoreboardModularSettingsView(
                modular = modular,
                clock = modular.clock.value,
                updated = updated,
            )
        }
    }
}

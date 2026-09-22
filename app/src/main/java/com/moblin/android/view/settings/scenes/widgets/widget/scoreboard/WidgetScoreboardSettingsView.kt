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
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

@Composable
fun WidgetScoreboardQuickButtonControlsView(
    model: Model = LocalModel.current,
    widget: SettingsWidget,
    scoreboard: SettingsWidgetScoreboard,
) {
    val sport = scoreboard.sport
    when (sport) {
        SettingsWidgetScoreboardSport.generic -> WidgetScoreboardGenericQuickButtonControlsView(
            model = model,
            widget = widget,
        )
        SettingsWidgetScoreboardSport.padel -> WidgetScoreboardPadelQuickButtonControlsView(
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
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
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
    val textColorColor = scoreboard.textColorColor
    val primaryBackgroundColorColor = scoreboard.primaryBackgroundColorColor
    val secondaryBackgroundColorColor = scoreboard.secondaryBackgroundColorColor
    Column(modifier = Modifier.fillMaxWidth()) {
        RgbColorPickerView(
            title = localized("Text"),
            color = textColorColor,
            onColorChanged = {},
        ) { color ->
            scoreboard.textColor = color
            updated()
        }
        RgbColorPickerView(
            title = localized("Primary background"),
            color = primaryBackgroundColorColor,
            onColorChanged = {},
        ) { color ->
            scoreboard.primaryBackgroundColor = color
            updated()
        }
        RgbColorPickerView(
            title = localized("Secondary background"),
            color = secondaryBackgroundColorColor,
            onColorChanged = {},
        ) { color ->
            scoreboard.secondaryBackgroundColor = color
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
    model: Model = LocalModel.current,
    widget: SettingsWidget,
    scoreboard: SettingsWidgetScoreboard,
    web: SettingsRemoteControlWeb,
) {
    val sport = scoreboard.sport
    val modular = scoreboard.modular
    val generic = scoreboard.generic
    val padel = scoreboard.padel
    val golf = scoreboard.golf
    val webEnabled = web.enabled
    var sportExpanded by remember { mutableStateOf(false) }

    val updated: () -> Unit = {
        when (sport) {
            SettingsWidgetScoreboardSport.generic -> TODO("sendUpdateGenericScoreboardToWatch")
            SettingsWidgetScoreboardSport.padel -> TODO("sendUpdatePadelScoreboardToWatch")
            else -> Unit
        }
        Unit
    }

    LaunchedEffect(sport) {
        modular.config = null
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
                                scoreboard.sport = option
                                sportExpanded = false
                            },
                        )
                    }
                }
            }
        }
        when (sport) {
            SettingsWidgetScoreboardSport.padel -> WidgetScoreboardPadelGeneralSettingsView(
                widget = widget,
                scoreboard = scoreboard,
                padel = padel,
                updated = updated,
            )
            SettingsWidgetScoreboardSport.generic -> WidgetScoreboardGenericGeneralSettingsView(
                widget = widget,
                scoreboard = scoreboard,
                generic = generic,
                updated = updated,
            )
            SettingsWidgetScoreboardSport.golf -> WidgetScoreboardGolfGeneralSettingsView(
                scoreboard = scoreboard,
                golf = golf,
                updated = updated,
            )
            SettingsWidgetScoreboardSport.golfFullScorecard -> WidgetScoreboardGolfFullScorecardGeneralSettingsView(
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
            SettingsWidgetScoreboardSport.padel, SettingsWidgetScoreboardSport.generic -> Text(
                text = localized("Use your Apple Watch to update the scoreboard."),
            )
            SettingsWidgetScoreboardSport.golf -> {
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
            SettingsWidgetScoreboardSport.golfFullScorecard -> Text(
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
            SettingsWidgetScoreboardSport.padel -> WidgetScoreboardPadelSettingsView(
                model = model,
                padel = padel,
                updated = updated,
            )
            SettingsWidgetScoreboardSport.generic -> WidgetScoreboardGenericSettingsView(
                generic = generic,
                clock = generic.clock,
                updated = updated,
            )
            SettingsWidgetScoreboardSport.golf -> WidgetScoreboardGolfSettingsView(
                golf = golf,
                updated = updated,
            )
            SettingsWidgetScoreboardSport.golfFullScorecard -> Unit
            else -> WidgetScoreboardModularSettingsView(
                modular = modular,
                clock = modular.clock,
                updated = updated,
            )
        }
    }
}

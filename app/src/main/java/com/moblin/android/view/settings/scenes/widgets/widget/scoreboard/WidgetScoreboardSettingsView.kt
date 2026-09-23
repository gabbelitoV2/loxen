package com.moblin.android.view.settings.scenes.widgets.widget.scoreboard

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsRemoteControlWeb
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetScoreboard
import com.moblin.android.various.settings.SettingsWidgetScoreboardSport
import com.moblin.android.view.settings.remotecontrol.RemoteControlWebDefaultUrlView
import com.moblin.android.view.utils.RemoteControlWebShortcutView
import com.moblin.android.view.utils.RgbColorPickerView
import com.moblin.android.view.utils.TextButtonView
import com.moblin.android.various.model.getModularScoreboardConfig
import com.moblin.android.various.model.getScoreboardEffect
import com.moblin.android.various.model.remoteControlScoreboardUpdate
import com.moblin.android.various.model.sendUpdateGenericScoreboardToWatch
import com.moblin.android.various.model.sendUpdatePadelScoreboardToWatch

@Composable
fun WidgetScoreboardQuickButtonControlsView(
    model: Model = LocalModel.current,
    widget: SettingsWidget,
    scoreboard: SettingsWidgetScoreboard,
) {
    when (scoreboard.sport) {
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
    NavigationLink(
        destination = {
            ScoreboardColorsFormView(scoreboard = scoreboard, updated = updated)
        },
    ) {
        Text(localized("Colors"))
    }
}

@Composable
fun ScoreboardColorsFormView(
    scoreboard: SettingsWidgetScoreboard,
    updated: () -> Unit,
) {
    Form(title = localized("Colors")) {
        Section {
            RgbColorPickerView(
                title = localized("Text"),
                color = scoreboard.textColorColor,
                onColorChanged = { color ->
                    scoreboard.textColorColor = color
                },
            ) { color ->
                scoreboard.textColor = color
                updated()
            }
            RgbColorPickerView(
                title = localized("Primary background"),
                color = scoreboard.primaryBackgroundColorColor,
                onColorChanged = { color ->
                    scoreboard.primaryBackgroundColorColor = color
                },
            ) { color ->
                scoreboard.primaryBackgroundColor = color
                updated()
            }
            RgbColorPickerView(
                title = localized("Secondary background"),
                color = scoreboard.secondaryBackgroundColorColor,
                onColorChanged = { color ->
                    scoreboard.secondaryBackgroundColorColor = color
                },
            ) { color ->
                scoreboard.secondaryBackgroundColor = color
                updated()
            }
        }
        Section {
            TextButtonView(title = localized("Reset")) {
                scoreboard.resetColors()
                updated()
            }
        }
    }
}

@Composable
fun WidgetScoreboardSettingsView(
    model: Model = LocalModel.current,
    widget: SettingsWidget,
    scoreboard: SettingsWidgetScoreboard,
    web: SettingsRemoteControlWeb,
) {
    val updated: () -> Unit = {
        when (scoreboard.sport) {
            SettingsWidgetScoreboardSport.generic -> model.sendUpdateGenericScoreboardToWatch(
                widget.id,
                scoreboard.generic,
            )
            SettingsWidgetScoreboardSport.padel -> model.sendUpdatePadelScoreboardToWatch(
                widget.id,
                scoreboard.padel,
            )
            else -> Unit
        }
        model.remoteControlScoreboardUpdate(scoreboard)
        model.getScoreboardEffect(widget.id)?.update(
            scoreboard,
            model.getModularScoreboardConfig(scoreboard),
            model.database.scoreboardPlayers,
        )
    }

    Section {
        Picker(
            title = localized("Sport"),
            selection = scoreboard.sport,
            options = SettingsWidgetScoreboardSport.entries,
            text = { it.toString() },
            onChange = { sport ->
                scoreboard.sport = sport
                scoreboard.modular.config = null
                updated()
            },
        )
        when (scoreboard.sport) {
            SettingsWidgetScoreboardSport.padel -> WidgetScoreboardPadelGeneralSettingsView(
                widget = widget,
                scoreboard = scoreboard,
                padel = scoreboard.padel,
                updated = updated,
            )
            SettingsWidgetScoreboardSport.generic -> WidgetScoreboardGenericGeneralSettingsView(
                widget = widget,
                scoreboard = scoreboard,
                generic = scoreboard.generic,
                updated = updated,
            )
            SettingsWidgetScoreboardSport.golf -> WidgetScoreboardGolfGeneralSettingsView(
                scoreboard = scoreboard,
                golf = scoreboard.golf,
                updated = updated,
            )
            SettingsWidgetScoreboardSport.golfFullScorecard ->
                WidgetScoreboardGolfFullScorecardGeneralSettingsView(
                    scoreboard = scoreboard,
                    golf = scoreboard.golf,
                    updated = updated,
                )
            else -> WidgetScoreboardModularGeneralSettingsView(
                modular = scoreboard.modular,
                updated = updated,
            )
        }
    }
    Section(header = localized("Remote control")) {
        when (scoreboard.sport) {
            SettingsWidgetScoreboardSport.padel, SettingsWidgetScoreboardSport.generic ->
                Text(localized("Use your Apple Watch to update the scoreboard."))
            SettingsWidgetScoreboardSport.golf -> {
                Text(
                    localized(
                        "Use the web based remote control on another device to update the scoreboard.",
                    ),
                )
                if (web.enabled) {
                    RemoteControlWebDefaultUrlView(
                        web = web,
                        status = model.statusOther,
                        path = "/golf.html",
                    )
                }
                RemoteControlWebShortcutView(model = model)
                if (!web.enabled) {
                    Text(localized("⚠️ The web based remote control is not enabled."))
                }
            }
            SettingsWidgetScoreboardSport.golfFullScorecard ->
                Text(localized("Use a golf scoreboard widget to control this widget."))
            else -> {
                Text(
                    localized(
                        "Use the web based remote control on another device to update the scoreboard.",
                    ),
                )
                if (web.enabled) {
                    RemoteControlWebDefaultUrlView(
                        web = web,
                        status = model.statusOther,
                        path = "/remote.html",
                    )
                }
                RemoteControlWebShortcutView(model = model)
                if (!web.enabled) {
                    Text(localized("⚠️ The web based remote control is not enabled."))
                }
            }
        }
    }
    when (scoreboard.sport) {
        SettingsWidgetScoreboardSport.padel -> WidgetScoreboardPadelSettingsView(
            model = model,
            padel = scoreboard.padel,
            updated = updated,
        )
        SettingsWidgetScoreboardSport.generic -> WidgetScoreboardGenericSettingsView(
            generic = scoreboard.generic,
            clock = scoreboard.generic.clock,
            updated = updated,
        )
        SettingsWidgetScoreboardSport.golf -> WidgetScoreboardGolfSettingsView(
            golf = scoreboard.golf,
            updated = updated,
        )
        SettingsWidgetScoreboardSport.golfFullScorecard -> Unit
        else -> WidgetScoreboardModularSettingsView(
            modular = scoreboard.modular,
            clock = scoreboard.modular.clock,
            updated = updated,
        )
    }
}

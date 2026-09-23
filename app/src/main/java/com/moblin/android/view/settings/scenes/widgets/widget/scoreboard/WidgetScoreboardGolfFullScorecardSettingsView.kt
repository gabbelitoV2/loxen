package com.moblin.android.view.settings.scenes.widgets.widget.scoreboard

import androidx.compose.runtime.Composable
import com.moblin.android.platform.swiftui.*
import com.moblin.android.various.settings.SettingsWidgetGolfScoreboard
import com.moblin.android.various.settings.SettingsWidgetScoreboard

@Composable
fun WidgetScoreboardGolfFullScorecardGeneralSettingsView(
    scoreboard: SettingsWidgetScoreboard,
    golf: SettingsWidgetGolfScoreboard,
    updated: () -> Unit,
) {
    ScoreboardColorsView(scoreboard = scoreboard, updated = updated)
    Toggle("Show pars", isOn = golf.showPars) { newValue ->
        golf.showPars = newValue
        updated()
    }
}

package com.moblin.android.view.settings.scenes.widgets.widget.scoreboard

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.moblin.android.localized
import com.moblin.android.various.settings.SettingsWidgetGolfScoreboard
import com.moblin.android.various.settings.SettingsWidgetScoreboard

@Composable
fun WidgetScoreboardGolfFullScorecardGeneralSettingsView(
    scoreboard: SettingsWidgetScoreboard,
    golf: SettingsWidgetGolfScoreboard,
    updated: () -> Unit,
) {
    val showPars = golf.showPars
    var firstRun by remember { mutableStateOf(true) }
    ScoreboardColorsView(scoreboard = scoreboard, updated = updated)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = localized("Show pars"),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = showPars,
            onCheckedChange = { newValue -> golf.showPars = newValue },
        )
    }
    LaunchedEffect(showPars) {
        if (firstRun) {
            firstRun = false
        } else {
            updated()
        }
    }
}

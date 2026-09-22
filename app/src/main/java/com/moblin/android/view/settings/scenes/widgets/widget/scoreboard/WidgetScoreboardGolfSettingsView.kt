package com.moblin.android.view.settings.scenes.widgets.widget.scoreboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.moblin.android.localized
import com.moblin.android.various.settings.SettingsWidgetGolfScoreboard
import com.moblin.android.various.settings.SettingsWidgetGolfScoreboardPlayer
import com.moblin.android.various.settings.SettingsWidgetScoreboard
import com.moblin.android.various.utils.makeOffsets
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.view.utils.TextEditNavigationView

@Composable
fun WidgetScoreboardGolfGeneralSettingsView(
    scoreboard: SettingsWidgetScoreboard,
    golf: SettingsWidgetGolfScoreboard,
    updated: () -> Unit
) {
    ScoreboardColorsView(scoreboard = scoreboard, updated = updated)
}

@Composable
private fun GolfPlayerView(player: SettingsWidgetGolfScoreboardPlayer, updated: () -> Unit) {
    val name by player.name.collectAsState()
    TextEditNavigationView(title = localized("Name"), value = name) {
        player.name.value = it
        updated()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetScoreboardGolfSettingsView(
    golf: SettingsWidgetGolfScoreboard,
    updated: () -> Unit,
    onNavigate: (String) -> Unit = {}
) {
    Column {
        Text("Round", style = MaterialTheme.typography.titleSmall)
        val title by golf.title.collectAsState()
        TextEditNavigationView(title = localized("Title"), value = title) {
            golf.title.value = it
            updated()
        }
        val numberOfHoles by golf.numberOfHoles.collectAsState()
        var holesExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = holesExpanded,
            onExpandedChange = { holesExpanded = it }
        ) {
            OutlinedTextField(
                value = numberOfHoles.toString(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Holes") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = holesExpanded)
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = holesExpanded,
                onDismissRequest = { holesExpanded = false }
            ) {
                listOf(9, 18).forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.toString()) },
                        onClick = {
                            golf.numberOfHoles.value = option
                            holesExpanded = false
                        }
                    )
                }
            }
        }
        LaunchedEffect(numberOfHoles) {
            golf.currentHole.value = 0
            updated()
        }
        val pars by golf.pars.collectAsState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate("WidgetScoreboardGolfParsView") }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Par")
            Spacer(Modifier.weight(1f))
            Text(
                pars.take(numberOfHoles).sum().toString(),
                color = Color.Gray
            )
        }
        Text("Players", style = MaterialTheme.typography.titleSmall)
        val players by golf.players.collectAsState()
        players.forEach { player ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.weight(1f)) {
                    GolfPlayerView(player = player, updated = updated)
                }
                IconButton(
                    onClick = {
                        val offsets = makeOffsets(players, player.id)
                        if (offsets != null) {
                            golf.players.value = players.filterIndexed { index, _ ->
                                index !in offsets
                            }
                            updated()
                        }
                    }
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                }
            }
        }
        if (players.size < 4) {
            CreateButtonView {
                val n = players.size + 1
                golf.players.value = players + SettingsWidgetGolfScoreboardPlayer(name = "Player $n")
                updated()
            }
        }
        SwipeLeftToDeleteHelpView(kind = localized("a player"))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetScoreboardGolfParsView(
    golf: SettingsWidgetGolfScoreboard,
    updated: () -> Unit
) {
    Column {
        val pars by golf.pars.collectAsState()
        val numberOfHoles by golf.numberOfHoles.collectAsState()
        for (i in 0 until numberOfHoles) {
            var parExpanded by remember(i) { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = parExpanded,
                onExpandedChange = { parExpanded = it }
            ) {
                OutlinedTextField(
                    value = (pars.getOrNull(i) ?: 0).toString(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Hole ${i + 1}") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = parExpanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = parExpanded,
                    onDismissRequest = { parExpanded = false }
                ) {
                    listOf(1, 2, 3, 4, 5, 6, 7, 8, 9).forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.toString()) },
                            onClick = {
                                val updatedPars = golf.pars.value.toMutableList()
                                if (i < updatedPars.size) {
                                    updatedPars[i] = option
                                    golf.pars.value = updatedPars
                                }
                                parExpanded = false
                                updated()
                            }
                        )
                    }
                }
            }
            LaunchedEffect(pars) {
                updated()
            }
        }
    }
}

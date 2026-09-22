package com.moblin.android.view.settings.scenes.widgets.widget.scoreboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetPadelScoreboard
import com.moblin.android.various.settings.SettingsWidgetPadelScoreboardGameType
import com.moblin.android.various.settings.SettingsWidgetScoreboard
import com.moblin.android.various.settings.SettingsWidgetScoreboardPlayer
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.InlinePickerItem
import com.moblin.android.view.utils.InlinePickerView
import com.moblin.android.view.utils.NameEditView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import java.util.UUID

@Composable
private fun PlayersPlayerView(
    model: Model,
    database: Database,
    player: SettingsWidgetScoreboardPlayer
) {
    NameEditView(
        name = player.name,
        existingNames = database.scoreboardPlayers,
        onChange = { player.name = it }
    )
    LaunchedEffect(player.name) {
        model.sceneUpdated()
        model.sendScoreboardPlayersToWatch()
    }
}

private fun deletePlayer(
    model: Model,
    database: Database,
    offsets: List<Int>,
    updated: () -> Unit
) {
    offsets.sortedDescending().forEach { index ->
        if (index in database.scoreboardPlayers.indices) {
            database.scoreboardPlayers.removeAt(index)
        }
    }
    updated()
    model.sendScoreboardPlayersToWatch()
}

@Composable
private fun PlayersView(
    model: Model,
    database: Database,
    updated: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Players", style = MaterialTheme.typography.titleMedium)
        database.scoreboardPlayers.forEachIndexed { index, player ->
            PlayersPlayerView(model = model, database = database, player = player)
            if (index < database.scoreboardPlayers.size) {
                TODO("no Android counterpart for contextMenuDeleteButton")
            }
        }
        TODO("no Android counterpart for list reordering")
        CreateButtonView(onClick = {
            val player = SettingsWidgetScoreboardPlayer()
            player.name = makeUniqueName(
                name = SettingsWidgetScoreboardPlayer.baseName,
                existingNames = database.scoreboardPlayers
            )
            database.scoreboardPlayers.add(player)
            model.sendScoreboardPlayersToWatch()
        })
        SwipeLeftToDeleteHelpView(kind = localized("a player"))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerView(
    model: Model,
    playerId: UUID,
    onPlayerIdChange: (UUID) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showPicker = true },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = model.findScoreboardPlayer(playerId))
        Spacer(modifier = Modifier.weight(1f))
        Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = null)
    }
    if (showPicker) {
        ModalBottomSheet(onDismissRequest = { showPicker = false }) {
            InlinePickerView(
                title = "Name",
                onChange = {
                    onPlayerIdChange(
                        runCatching { UUID.fromString(it) }.getOrNull() ?: UUID.randomUUID()
                    )
                    showPicker = false
                },
                items = model.database.scoreboardPlayers.map {
                    InlinePickerItem(id = it.id.toString(), text = it.name)
                },
                selectedId = playerId.toString()
            )
        }
    }
}

@Composable
fun WidgetScoreboardPadelQuickButtonControlsView(
    model: Model,
    widget: SettingsWidget
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Spacer(modifier = Modifier.weight(1f))
            ScoreboardUndoButtonView(onClick = {
                model.handleUpdatePadelScoreboard(
                    action = TODO("PadelScoreboardAction(undo) type not in the port glossary")
                )
            })
            ScoreboardIncrementButtonView(onClick = {
                model.handleUpdatePadelScoreboard(
                    action = TODO("PadelScoreboardAction(incrementHome) type not in the port glossary")
                )
            })
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Spacer(modifier = Modifier.weight(1f))
            ScoreboardResetScoreButtonView(onClick = {
                model.handleUpdatePadelScoreboard(
                    action = TODO("PadelScoreboardAction(reset) type not in the port glossary")
                )
            })
            ScoreboardIncrementButtonView(onClick = {
                model.handleUpdatePadelScoreboard(
                    action = TODO("PadelScoreboardAction(incrementAway) type not in the port glossary")
                )
            })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetScoreboardPadelGeneralSettingsView(
    widget: SettingsWidget,
    scoreboard: SettingsWidgetScoreboard,
    padel: SettingsWidgetPadelScoreboard,
    updated: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "Game type")
        Spacer(modifier = Modifier.weight(1f))
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = padel.type.toString(),
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                SettingsWidgetPadelScoreboardGameType.values().forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.toString()) },
                        onClick = {
                            padel.type = type
                            expanded = false
                        }
                    )
                }
            }
        }
    }
    LaunchedEffect(padel.type) {
        updated()
    }
    ScoreboardColorsView(scoreboard = scoreboard, updated = updated)
}

@Composable
fun WidgetScoreboardPadelSettingsView(
    model: Model,
    padel: SettingsWidgetPadelScoreboard,
    updated: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Home", style = MaterialTheme.typography.titleMedium)
        PlayerView(
            model = model,
            playerId = padel.homePlayer1,
            onPlayerIdChange = { padel.homePlayer1 = it }
        )
        LaunchedEffect(padel.homePlayer1) {
            updated()
        }
        if (padel.type == SettingsWidgetPadelScoreboardGameType.doubles) {
            PlayerView(
                model = model,
                playerId = padel.homePlayer2,
                onPlayerIdChange = { padel.homePlayer2 = it }
            )
            LaunchedEffect(padel.homePlayer2) {
                updated()
            }
        }
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Away", style = MaterialTheme.typography.titleMedium)
        PlayerView(
            model = model,
            playerId = padel.awayPlayer1,
            onPlayerIdChange = { padel.awayPlayer1 = it }
        )
        LaunchedEffect(padel.awayPlayer1) {
            updated()
        }
        if (padel.type == SettingsWidgetPadelScoreboardGameType.doubles) {
            PlayerView(
                model = model,
                playerId = padel.awayPlayer2,
                onPlayerIdChange = { padel.awayPlayer2 = it }
            )
            LaunchedEffect(padel.awayPlayer2) {
                updated()
            }
        }
    }
    PlayersView(model = model, database = model.database, updated = updated)
}

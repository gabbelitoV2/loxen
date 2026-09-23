package com.moblin.android.view.settings.scenes.widgets.widget.scoreboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.ForEach
import com.moblin.android.platform.swiftui.IndexSet
import com.moblin.android.platform.swiftui.LocalNavigator
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.binding
import com.moblin.android.platform.swiftui.move
import com.moblin.android.platform.swiftui.remove
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.sceneUpdated
import com.moblin.android.various.model.sendScoreboardPlayersToWatch
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetPadelScoreboard
import com.moblin.android.various.settings.SettingsWidgetPadelScoreboardGameType
import com.moblin.android.various.settings.SettingsWidgetScoreboard
import com.moblin.android.various.settings.SettingsWidgetScoreboardPlayer
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.utils.ContextMenuDeleteButton
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.InlinePickerItem
import com.moblin.android.view.utils.InlinePickerView
import com.moblin.android.view.utils.NameEditView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import java.util.UUID
import com.moblin.android.LocalModel

@Composable
private fun PlayersPlayerView(
    model: Model = LocalModel.current,
    database: Database,
    player: SettingsWidgetScoreboardPlayer
) {
    NameEditView(
        name = player.name,
        existingNames = database.scoreboardPlayers,
        onNameChange = {
            if (player.name != it) {
                player.name = it
                model.sceneUpdated()
                model.sendScoreboardPlayersToWatch()
            }
        }
    )
}

private fun deletePlayer(
    model: Model,
    database: Database,
    offsets: IndexSet,
    updated: () -> Unit
) {
    database.scoreboardPlayers.remove(atOffsets = offsets)
    updated()
    model.sendScoreboardPlayersToWatch()
}

@Composable
private fun PlayersView(
    model: Model = LocalModel.current,
    database: Database,
    updated: () -> Unit
) {
    Section(
        header = "Players",
        footerContent = { SwipeLeftToDeleteHelpView(kind = localized("a player")) }
    ) {
        ForEach(
            database.scoreboardPlayers,
            id = { it.id },
            onDelete = { deletePlayer(model, database, it, updated) },
            onMove = { froms, to ->
                database.scoreboardPlayers.move(fromOffsets = froms, toOffset = to)
                updated()
                model.sendScoreboardPlayersToWatch()
            }
        ) { player ->
            ContextMenuDeleteButton(
                action = {
                    val index = database.scoreboardPlayers.indexOfFirst { it.id == player.id }
                    if (index >= 0) {
                        deletePlayer(model, database, setOf(index), updated)
                    }
                }
            ) {
                PlayersPlayerView(model = model, database = database, player = player)
            }
        }
        CreateButtonView(action = {
            val player = SettingsWidgetScoreboardPlayer()
            player.name = makeUniqueName(
                name = SettingsWidgetScoreboardPlayer.baseName,
                existingNames = database.scoreboardPlayers
            )
            database.scoreboardPlayers.add(player)
            model.sendScoreboardPlayersToWatch()
        })
    }
}

@Composable
private fun PlayerView(
    model: Model = LocalModel.current,
    playerId: UUID,
    onPlayerIdChange: (UUID) -> Unit
) {
    NavigationLink(
        destination = {
            val navigator = LocalNavigator.current
            InlinePickerView(
                title = "Name",
                onChange = {
                    onPlayerIdChange(runCatching { UUID.fromString(it) }.getOrNull() ?: UUID.randomUUID())
                },
                items = model.database.scoreboardPlayers.map {
                    InlinePickerItem(id = it.id.toString(), text = it.name)
                },
                initialSelectedId = playerId.toString(),
                onDismiss = { navigator?.pop() }
            )
        }
    ) {
        Text(text = model.findScoreboardPlayer(playerId))
    }
}

@Composable
fun WidgetScoreboardPadelQuickButtonControlsView(
    model: Model = LocalModel.current,
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
            ScoreboardUndoButtonView(action = {
                Unit
            })
            ScoreboardIncrementButtonView(action = {
                Unit
            })
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Spacer(modifier = Modifier.weight(1f))
            ScoreboardResetScoreButtonView(action = {
                Unit
            })
            ScoreboardIncrementButtonView(action = {
                Unit
            })
        }
    }
}

@Composable
fun WidgetScoreboardPadelGeneralSettingsView(
    widget: SettingsWidget,
    scoreboard: SettingsWidgetScoreboard,
    padel: SettingsWidgetPadelScoreboard,
    updated: () -> Unit
) {
    val type = binding(get = { padel.type }, set = { padel.type = it })
    Picker(
        title = "Game type",
        selection = type.value,
        options = SettingsWidgetPadelScoreboardGameType.entries,
        text = { it.toString() }
    ) {
        type.value = it
        updated()
    }
    ScoreboardColorsView(scoreboard = scoreboard, updated = updated)
}

@Composable
fun WidgetScoreboardPadelSettingsView(
    model: Model = LocalModel.current,
    padel: SettingsWidgetPadelScoreboard,
    updated: () -> Unit
) {
    Section(header = "Home") {
        PlayerView(
            model = model,
            playerId = padel.homePlayer1,
            onPlayerIdChange = {
                if (padel.homePlayer1 != it) {
                    padel.homePlayer1 = it
                    updated()
                }
            }
        )
        if (padel.type == SettingsWidgetPadelScoreboardGameType.doubles) {
            PlayerView(
                model = model,
                playerId = padel.homePlayer2,
                onPlayerIdChange = {
                    if (padel.homePlayer2 != it) {
                        padel.homePlayer2 = it
                        updated()
                    }
                }
            )
        }
    }
    Section(header = "Away") {
        PlayerView(
            model = model,
            playerId = padel.awayPlayer1,
            onPlayerIdChange = {
                if (padel.awayPlayer1 != it) {
                    padel.awayPlayer1 = it
                    updated()
                }
            }
        )
        if (padel.type == SettingsWidgetPadelScoreboardGameType.doubles) {
            PlayerView(
                model = model,
                playerId = padel.awayPlayer2,
                onPlayerIdChange = {
                    if (padel.awayPlayer2 != it) {
                        padel.awayPlayer2 = it
                        updated()
                    }
                }
            )
        }
    }
    PlayersView(model = model, database = model.database, updated = updated)
}

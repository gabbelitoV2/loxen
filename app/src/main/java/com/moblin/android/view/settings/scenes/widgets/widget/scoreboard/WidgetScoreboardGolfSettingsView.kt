package com.moblin.android.view.settings.scenes.widgets.widget.scoreboard

import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.ForEach
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.platform.swiftui.removing
import com.moblin.android.various.settings.SettingsWidgetGolfScoreboard
import com.moblin.android.various.settings.SettingsWidgetGolfScoreboardPlayer
import com.moblin.android.various.settings.SettingsWidgetScoreboard
import com.moblin.android.view.utils.ContextMenuDeleteButton
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
    TextEditNavigationView(title = localized("Name"), value = player.name, onSubmit = {
        player.name = it
        updated()
    })
}

@Composable
fun WidgetScoreboardGolfSettingsView(
    golf: SettingsWidgetGolfScoreboard,
    updated: () -> Unit,
    onNavigate: (String) -> Unit = {}
) {
    val palette = formPalette()
    Section(header = localized("Round")) {
        TextEditNavigationView(title = localized("Title"), value = golf.title, onSubmit = {
            golf.title = it
            updated()
        })
        Picker(
            title = localized("Holes"),
            selection = golf.numberOfHoles,
            options = listOf(9, 18),
            onChange = { value ->
                golf.numberOfHoles = value
                golf.currentHole = 0
                updated()
            }
        )
        NavigationLink(destination = {
            WidgetScoreboardGolfParsView(golf = golf, updated = updated)
        }) {
            Text(localized("Par"))
            Spacer(Modifier.weight(1f))
            Text(
                golf.pars.take(golf.numberOfHoles).sum().toString(),
                color = palette.gray
            )
        }
    }
    Section(
        header = localized("Players"),
        footerContent = { SwipeLeftToDeleteHelpView(kind = localized("a player")) }
    ) {
        ForEach(
            golf.players,
            id = { it.id },
            onDelete = { offsets ->
                golf.players = golf.players.removing(atOffsets = offsets)
                updated()
            },
        ) { player ->
            ContextMenuDeleteButton(
                action = {
                    val index = golf.players.indexOfFirst { it.id == player.id }
                    if (index != -1) {
                        golf.players = golf.players.removing(atOffsets = setOf(index))
                        updated()
                    }
                },
            ) {
                GolfPlayerView(player = player, updated = updated)
            }
        }
        if (golf.players.size < 4) {
            CreateButtonView {
                val n = golf.players.size + 1
                golf.players = golf.players + SettingsWidgetGolfScoreboardPlayer(name = "Player $n")
                updated()
            }
        }
    }
}

@Composable
fun WidgetScoreboardGolfParsView(
    golf: SettingsWidgetGolfScoreboard,
    updated: () -> Unit
) {
    Form(title = localized("Pars")) {
        for (i in 0 until golf.numberOfHoles) {
            Picker(
                title = "Hole ${i + 1}",
                selection = golf.pars.getOrNull(i) ?: 0,
                options = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9),
                onChange = { value ->
                    val pars = golf.pars.toMutableList()
                    if (i < pars.size) {
                        pars[i] = value
                        golf.pars = pars
                    }
                    updated()
                }
            )
        }
    }
}

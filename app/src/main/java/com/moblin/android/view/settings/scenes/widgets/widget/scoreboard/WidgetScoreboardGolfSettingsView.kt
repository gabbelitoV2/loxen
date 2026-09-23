package com.moblin.android.view.settings.scenes.widgets.widget.scoreboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.localized
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.settings.SettingsWidgetGolfScoreboard
import com.moblin.android.various.settings.SettingsWidgetGolfScoreboardPlayer
import com.moblin.android.various.settings.SettingsWidgetScoreboard
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
        golf.players.forEach { player ->
            key(player.id) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.weight(1f)) {
                        GolfPlayerView(player = player, updated = updated)
                    }
                    val interactionSource = remember { MutableInteractionSource() }
                    val pressed by interactionSource.collectIsPressedAsState()
                    Box(
                        modifier = Modifier
                            .alpha(if (pressed) 0.2f else 1f)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                val index = golf.players.indexOfFirst { it.id == player.id }
                                if (index != -1) {
                                    golf.players = golf.players.filterIndexed { i, _ ->
                                        i != index
                                    }
                                    updated()
                                }
                            }
                            .padding(horizontal = 8.dp)
                    ) {
                        SystemImage(name = "trash", fontSize = 20.sp, tint = palette.red)
                    }
                }
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

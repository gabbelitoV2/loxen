package com.moblin.android.videoeffects.scoreboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.common.various.isSetWin
import com.moblin.android.various.settings.SettingsWidgetPadelScoreboard
import com.moblin.android.various.settings.SettingsWidgetPadelScoreboardGameType
import com.moblin.android.various.settings.SettingsWidgetScoreboardPlayer
import java.util.UUID

private data class PadelScoreboardScore(
    val id: UUID = UUID.randomUUID(),
    val home: Int,
    val away: Int,
) {
    fun isHomeWin(): Boolean {
        return isSetWin(home, away)
    }

    fun isAwayWin(): Boolean {
        return isSetWin(away, home)
    }
}

private data class PadelScoreboardPlayer(
    val id: UUID = UUID.randomUUID(),
    val name: String,
)

private data class PadelScoreboardTeam(
    val players: List<PadelScoreboardPlayer>,
)

private data class PadelScoreboard(
    val home: PadelScoreboardTeam,
    val away: PadelScoreboardTeam,
    val score: List<PadelScoreboardScore>,
)

private fun createPadelPlayer(players: List<SettingsWidgetScoreboardPlayer>, id: UUID): PadelScoreboardPlayer {
    return PadelScoreboardPlayer(name = findScoreboardPlayer(players, id))
}

private fun findScoreboardPlayer(players: List<SettingsWidgetScoreboardPlayer>, id: UUID): String {
    return players.firstOrNull { it.id == id }?.name ?: "🇸🇪 Moblin"
}

private fun padelScoreboardSettingsToEffect(
    scoreboard: SettingsWidgetPadelScoreboard,
    players: List<SettingsWidgetScoreboardPlayer>,
): PadelScoreboard {
    val homePlayers = mutableListOf(createPadelPlayer(players, scoreboard.homePlayer1))
    val awayPlayers = mutableListOf(createPadelPlayer(players, scoreboard.awayPlayer1))
    if (scoreboard.type == SettingsWidgetPadelScoreboardGameType.doubles) {
        homePlayers.add(createPadelPlayer(players, scoreboard.homePlayer2))
        awayPlayers.add(createPadelPlayer(players, scoreboard.awayPlayer2))
    }
    val home = PadelScoreboardTeam(players = homePlayers)
    val away = PadelScoreboardTeam(players = awayPlayers)
    val score = scoreboard.score.map { PadelScoreboardScore(home = it.home, away = it.away) }
    return PadelScoreboard(home = home, away = away, score = score)
}

private fun scoreFontSize(padel: SettingsWidgetPadelScoreboard): Double {
    return when (padel.type) {
        SettingsWidgetPadelScoreboardGameType.doubles -> scoreboardScoreBigFontSize
        SettingsWidgetPadelScoreboardGameType.singles -> scoreboardScoreFontSize
    }
}

@Composable
fun ScoreboardEffectPadelView(
    textColor: Color,
    primaryBackgroundColor: Color,
    secondaryBackgroundColor: Color,
    padel: SettingsWidgetPadelScoreboard,
    players: List<SettingsWidgetScoreboardPlayer>,
    scale: Double,
) {
    val scoreboard = padelScoreboardSettingsToEffect(padel, players)
    Column(
        modifier = Modifier.clip(RoundedCornerShape((5 * scale).dp)),
    ) {
        CompositionLocalProvider(LocalContentColor provides textColor) {
            Column {
                Row(
                    modifier = Modifier
                        .background(primaryBackgroundColor)
                        .padding(start = (3 * scale).dp, top = (3 * scale).dp, end = (18 * scale).dp),
                    horizontalArrangement = Arrangement.spacedBy((18 * scale).dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CompositionLocalProvider(
                        LocalTextStyle provides TextStyle(fontSize = (25 * scale).sp),
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Spacer(Modifier.weight(1f))
                                scoreboard.home.players.forEach { player ->
                                    Text(player.name.uppercase())
                                }
                                Spacer(Modifier.weight(1f))
                            }
                            Column(horizontalAlignment = Alignment.Start) {
                                Spacer(Modifier.weight(1f))
                                scoreboard.away.players.forEach { player ->
                                    Text(player.name.uppercase())
                                }
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                    scoreboard.score.forEach { score ->
                        CompositionLocalProvider(
                            LocalTextStyle provides TextStyle(fontSize = (scoreFontSize(padel) * scale).sp),
                        ) {
                            Column(
                                modifier = Modifier.width((28 * scale).dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                TeamScoreView(score = score.home)
                                TeamScoreView(score = score.away)
                            }
                        }
                    }
                }
                PoweredByMoblinView(backgroundColor = secondaryBackgroundColor, scale = scale)
            }
        }
    }
}

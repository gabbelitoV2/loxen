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
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.isSetWin
import com.moblin.android.platform.swiftui.SwiftUIFonts
import com.moblin.android.various.settings.SettingsWidgetPadelScoreboard
import com.moblin.android.various.settings.SettingsWidgetPadelScoreboardGameType
import com.moblin.android.various.settings.SettingsWidgetScoreboardPlayer
import java.util.UUID

private data class PadelScoreboardScore(
    val id: UUID = UUID.randomUUID(),
    val home: Int,
    val away: Int,
) {
    fun isHomeWin(): Boolean = isSetWin(first = home, second = away)

    fun isAwayWin(): Boolean = isSetWin(first = away, second = home)
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
    return PadelScoreboardPlayer(name = findScoreboardPlayer(players = players, id = id))
}

private fun findScoreboardPlayer(players: List<SettingsWidgetScoreboardPlayer>, id: UUID): String {
    return players.firstOrNull { it.id == id }?.name ?: "🇸🇪 Moblin"
}

private fun padelScoreboardSettingsToEffect(
    scoreboard: SettingsWidgetPadelScoreboard,
    players: List<SettingsWidgetScoreboardPlayer>,
): PadelScoreboard {
    val homePlayers = mutableListOf(createPadelPlayer(players = players, id = scoreboard.homePlayer1))
    val awayPlayers = mutableListOf(createPadelPlayer(players = players, id = scoreboard.awayPlayer1))
    if (scoreboard.type == SettingsWidgetPadelScoreboardGameType.doubles) {
        homePlayers.add(createPadelPlayer(players = players, id = scoreboard.homePlayer2))
        awayPlayers.add(createPadelPlayer(players = players, id = scoreboard.awayPlayer2))
    }
    val home = PadelScoreboardTeam(players = homePlayers)
    val away = PadelScoreboardTeam(players = awayPlayers)
    val score = scoreboard.score.map { PadelScoreboardScore(home = it.home, away = it.away) }
    return PadelScoreboard(home = home, away = away, score = score)
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
    fun scoreFontSize(): Double = when (padel.type) {
        SettingsWidgetPadelScoreboardGameType.doubles -> scoreboardScoreBigFontSize
        SettingsWidgetPadelScoreboardGameType.singles -> scoreboardScoreFontSize
    }

    val scoreboard = padelScoreboardSettingsToEffect(padel, players)
    CompositionLocalProvider(LocalContentColor provides textColor) {
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.clip(RoundedCornerShape((5 * scale).dp)),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((18 * scale).dp),
                modifier = Modifier
                    .background(primaryBackgroundColor)
                    .padding(
                        start = (3 * scale).dp,
                        end = (18 * scale).dp,
                        top = (3 * scale).dp,
                    ),
            ) {
                CompositionLocalProvider(LocalTextStyle provides SwiftUIFonts.system(25 * scale)) {
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Spacer(Modifier.weight(1f))
                            scoreboard.home.players.forEach { player ->
                                key(player.id) {
                                    Text(player.name.uppercase())
                                }
                            }
                            Spacer(Modifier.weight(1f))
                        }
                        Column(
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Spacer(Modifier.weight(1f))
                            scoreboard.away.players.forEach { player ->
                                key(player.id) {
                                    Text(player.name.uppercase())
                                }
                            }
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
                scoreboard.score.forEach { score ->
                    key(score.id) {
                        CompositionLocalProvider(
                            LocalTextStyle provides SwiftUIFonts.system(scoreFontSize() * scale),
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.width((28 * scale).dp),
                            ) {
                                CompositionLocalProvider(
                                    LocalTextStyle provides SwiftUIFonts.system(
                                        scoreFontSize() * scale,
                                        weight = if (score.isHomeWin()) {
                                            FontWeight.Bold
                                        } else {
                                            FontWeight.Normal
                                        },
                                    ),
                                ) {
                                    TeamScoreView(score = score.home)
                                }
                                CompositionLocalProvider(
                                    LocalTextStyle provides SwiftUIFonts.system(
                                        scoreFontSize() * scale,
                                        weight = if (score.isAwayWin()) {
                                            FontWeight.Bold
                                        } else {
                                            FontWeight.Normal
                                        },
                                    ),
                                ) {
                                    TeamScoreView(score = score.away)
                                }
                            }
                        }
                    }
                }
            }
            PoweredByMoblinView(backgroundColor = secondaryBackgroundColor, scale = scale)
        }
    }
}

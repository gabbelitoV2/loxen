package com.moblin.android.videoeffects.scoreboard

import androidx.compose.ui.graphics.Color
import com.moblin.android.common.various.isSetWin
import com.moblin.android.platform.swiftui.layout.EdgeSet
import com.moblin.android.platform.swiftui.layout.Font
import com.moblin.android.platform.swiftui.layout.HorizontalAlignment
import com.moblin.android.platform.swiftui.layout.VerticalAlignment
import com.moblin.android.platform.swiftui.layout.View
import com.moblin.android.platform.swiftui.layout.ViewBuilder
import com.moblin.android.platform.swiftui.layout.background
import com.moblin.android.platform.swiftui.layout.bold
import com.moblin.android.platform.swiftui.layout.clipShape
import com.moblin.android.platform.swiftui.layout.font
import com.moblin.android.platform.swiftui.layout.foregroundStyle
import com.moblin.android.platform.swiftui.layout.frame
import com.moblin.android.platform.swiftui.layout.padding
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

fun ViewBuilder.ScoreboardEffectPadelView(
    textColor: Color,
    primaryBackgroundColor: Color,
    secondaryBackgroundColor: Color,
    padel: SettingsWidgetPadelScoreboard,
    players: List<SettingsWidgetScoreboardPlayer>,
    scale: Double,
): View {
    fun scoreFontSize(): Double = when (padel.type) {
        SettingsWidgetPadelScoreboardGameType.doubles -> scoreboardScoreBigFontSize
        SettingsWidgetPadelScoreboardGameType.singles -> scoreboardScoreFontSize
    }

    val scoreboard = padelScoreboardSettingsToEffect(padel, players)
    return VStack(alignment = HorizontalAlignment.leading, spacing = 0.0) {
        HStack(alignment = VerticalAlignment.center, spacing = 18 * scale) {
            VStack(alignment = HorizontalAlignment.leading) {
                VStack(alignment = HorizontalAlignment.leading) {
                    Spacer(minLength = 0.0)
                    for (player in scoreboard.home.players) {
                        Text(player.name.uppercase())
                    }
                    Spacer(minLength = 0.0)
                }
                VStack(alignment = HorizontalAlignment.leading) {
                    Spacer(minLength = 0.0)
                    for (player in scoreboard.away.players) {
                        Text(player.name.uppercase())
                    }
                    Spacer(minLength = 0.0)
                }
            }
                .font(Font.system(size = 25 * scale))
            for (score in scoreboard.score) {
                VStack {
                    TeamScoreView(score = score.home)
                        .bold(score.isHomeWin())
                    TeamScoreView(score = score.away)
                        .bold(score.isAwayWin())
                }
                    .frame(width = 28 * scale)
                    .font(Font.system(size = scoreFontSize() * scale))
            }
        }
            .padding(EdgeSet.leading, 3 * scale)
            .padding(EdgeSet.trailing, 18 * scale)
            .padding(EdgeSet.top, 3 * scale)
            .background(primaryBackgroundColor)
        PoweredByMoblinView(backgroundColor = secondaryBackgroundColor, scale = scale)
    }
        .clipShape(RoundedRectangle(cornerRadius = 5 * scale))
        .foregroundStyle(textColor)
}

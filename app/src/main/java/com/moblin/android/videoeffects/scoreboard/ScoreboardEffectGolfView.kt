package com.moblin.android.videoeffects.scoreboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.common.various.RgbColor
import com.moblin.android.localized
import com.moblin.android.various.settings.SettingsWidgetGolfScoreboard
import com.moblin.android.various.settings.SettingsWidgetGolfScoreboardPlayer

private fun RgbColor.color(): Color =
    Color(red.toFloat() / 255f, green.toFloat() / 255f, blue.toFloat() / 255f)

@Composable
private fun PlayerNameView(
    player: SettingsWidgetGolfScoreboardPlayer,
    playerColor: Boolean,
    scale: Double,
) {
    Row(
        modifier = Modifier
            .height((35 * scale).dp)
            .padding(start = (8 * scale).dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((6 * scale).dp),
    ) {
        if (playerColor) {
            Box(
                modifier = Modifier
                    .size((18 * scale).dp)
                    .clip(RoundedCornerShape((3 * scale).dp))
                    .background(player.color.color()),
            )
        }
        Text(
            text = player.name.uppercase(),
            fontSize = (25 * scale).sp,
        )
    }
}

private fun format(player: SettingsWidgetGolfScoreboardPlayer, numberOfHoles: Int): String {
    val thru = player.holesPlayed(numberOfHoles)
    return when {
        thru == 0 -> ""
        thru < numberOfHoles -> localized("THRU $thru")
        else -> localized("F")
    }
}

@Composable
private fun ThruView(
    player: SettingsWidgetGolfScoreboardPlayer,
    numberOfHoles: Int,
    textColor: Color,
    scale: Double,
) {
    Text(
        text = format(player, numberOfHoles),
        modifier = Modifier
            .padding(end = (6 * scale).dp)
            .width((70 * scale).dp)
            .height((35 * scale).dp),
        color = textColor.copy(alpha = 0.6f),
        fontSize = (15 * scale).sp,
        fontFamily = FontFamily.Monospace,
    )
}

@Composable
private fun ScoreView(
    player: SettingsWidgetGolfScoreboardPlayer,
    pars: List<Int>,
    numberOfHoles: Int,
    textColor: Color,
    scale: Double,
) {
    val total = player.totalRelativeToPar(pars, numberOfHoles)
    Text(
        text = formatScore(total),
        modifier = Modifier
            .padding(end = (8 * scale).dp)
            .height((35 * scale).dp),
        color = when {
            total < 0 -> Color.Green
            total > 0 -> Color.Red
            else -> textColor
        },
        fontSize = (scoreboardScoreFontSize * scale).sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
    )
}

@Composable
fun ScoreboardEffectGolfView(
    textColor: Color,
    primaryBackgroundColor: Color,
    secondaryBackgroundColor: Color,
    golf: SettingsWidgetGolfScoreboard,
    scale: Double,
) {
    val title = golf.title
    val currentHole = golf.currentHole
    val numberOfHoles = golf.numberOfHoles
    val pars = golf.pars
    val players = golf.players
    val playerColors = golf.playerColors

    val holeIndex = minOf(currentHole, numberOfHoles - 1)
    val par = if (holeIndex in pars.indices) pars[holeIndex] else 4

    CompositionLocalProvider(LocalContentColor provides textColor) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape((5 * scale).dp)),
            horizontalAlignment = Alignment.Start,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(secondaryBackgroundColor)
                    .padding(horizontal = (8 * scale).dp, vertical = (4 * scale).dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((8 * scale).dp),
            ) {
                Text(
                    text = title,
                    fontSize = (20 * scale).sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "HOLE ${holeIndex + 1}  PAR $par",
                    fontSize = (18 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(primaryBackgroundColor),
            ) {
                Column(
                    horizontalAlignment = Alignment.Start,
                ) {
                    players.forEach { player ->
                        PlayerNameView(
                            player = player,
                            playerColor = playerColors,
                            scale = scale,
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Column(
                    horizontalAlignment = Alignment.Start,
                ) {
                    players.forEach { player ->
                        ThruView(
                            player = player,
                            numberOfHoles = numberOfHoles,
                            textColor = textColor,
                            scale = scale,
                        )
                    }
                }
                Column(
                    horizontalAlignment = Alignment.End,
                ) {
                    players.forEach { player ->
                        ScoreView(
                            player = player,
                            pars = pars,
                            numberOfHoles = numberOfHoles,
                            textColor = textColor,
                            scale = scale,
                        )
                    }
                }
            }
            PoweredByMoblinView(
                backgroundColor = secondaryBackgroundColor,
                scale = scale,
            )
        }
    }
}

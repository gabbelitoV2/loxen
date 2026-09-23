package com.moblin.android.videoeffects.scoreboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.color
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.SwiftUIFonts
import com.moblin.android.platform.swiftui.monospacedDigit
import com.moblin.android.various.settings.SettingsWidgetGolfScoreboard
import com.moblin.android.various.settings.SettingsWidgetGolfScoreboardPlayer
import androidx.compose.foundation.layout.wrapContentSize

@Composable
private fun PlayerNameView(
    player: SettingsWidgetGolfScoreboardPlayer,
    playerColor: Boolean,
    scale: Double,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((6 * scale).dp),
        modifier = Modifier
            .padding(start = (8 * scale).dp)
            .height((35 * scale).dp),
    ) {
        if (playerColor) {
            Box(
                modifier = Modifier
                    .size((18 * scale).dp)
                    .clip(RoundedCornerShape((3 * scale).dp))
                    .background(player.color.color()),
            ) {
            }
        }
        Text(
            player.name.uppercase(),
            style = SwiftUIFonts.system(25 * scale),
        )
    }
}

private fun format(player: SettingsWidgetGolfScoreboardPlayer, numberOfHoles: Int): String {
    val thru = player.holesPlayed(numHoles = numberOfHoles)
    return if (thru == 0) {
        ""
    } else if (thru < numberOfHoles) {
        localized("THRU ${thru}")
    } else {
        localized("F")
    }
}

@Composable
private fun GolfShrinkText(
    text: String,
    fontSize: Double,
    minimumScaleFactor: Double,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    weight: FontWeight = FontWeight.Normal,
) {
    val minimumFontSize = fontSize * minimumScaleFactor
    var currentFontSize by remember(text, fontSize) { mutableStateOf(fontSize) }
    Text(
        text,
        modifier = modifier,
        color = color,
        style = SwiftUIFonts.system(currentFontSize, weight).monospacedDigit(),
        onTextLayout = { layout ->
            if (layout.hasVisualOverflow && currentFontSize > minimumFontSize) {
                currentFontSize = maxOf(minimumFontSize, currentFontSize * 0.95)
            }
        },
    )
}

@Composable
private fun ThruView(
    player: SettingsWidgetGolfScoreboardPlayer,
    numberOfHoles: Int,
    textColor: Color,
    scale: Double,
) {
    GolfShrinkText(
        text = format(player, numberOfHoles),
        fontSize = 15 * scale,
        minimumScaleFactor = 0.5,
        modifier = Modifier
            .width((70 * scale).dp)
            .height((35 * scale).dp)
            .padding(end = (6 * scale).dp).wrapContentSize(),
        color = textColor.copy(alpha = 0.6f),
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
    val total = player.totalRelativeToPar(pars = pars, numberOfHoles = numberOfHoles)
    GolfShrinkText(
        text = formatScore(total),
        fontSize = scoreboardScoreFontSize * scale,
        minimumScaleFactor = 0.5,
        modifier = Modifier
            .height((35 * scale).dp)
            .padding(end = (8 * scale).dp).wrapContentSize(),
        color = if (total < 0) Color(0xFF34C759) else if (total > 0) Color(0xFFFF3B30) else textColor,
        weight = FontWeight.Bold,
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
    val holeIndex = minOf(golf.currentHole, golf.numberOfHoles - 1)
    val par = if (holeIndex in golf.pars.indices) golf.pars[holeIndex] else 4
    CompositionLocalProvider(LocalContentColor provides textColor) {
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.clip(RoundedCornerShape((5 * scale).dp)),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((8 * scale).dp),
                modifier = Modifier
                    .background(secondaryBackgroundColor)
                    .padding(vertical = (4 * scale).dp)
                    .padding(horizontal = (8 * scale).dp),
            ) {
                Text(
                    golf.title,
                    style = SwiftUIFonts.system(20 * scale, FontWeight.Bold),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    localized("HOLE ${holeIndex + 1}  PAR $par"),
                    style = SwiftUIFonts.system(18 * scale, FontWeight.Bold).monospacedDigit(),
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.background(primaryBackgroundColor),
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    golf.players.forEach { player ->
                        key(player.id) {
                            PlayerNameView(
                                player = player,
                                playerColor = golf.playerColors,
                                scale = scale,
                            )
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.Start) {
                    golf.players.forEach { player ->
                        key(player.id) {
                            ThruView(
                                player = player,
                                numberOfHoles = golf.numberOfHoles,
                                textColor = textColor,
                                scale = scale,
                            )
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    golf.players.forEach { player ->
                        key(player.id) {
                            ScoreView(
                                player = player,
                                pars = golf.pars,
                                numberOfHoles = golf.numberOfHoles,
                                textColor = textColor,
                                scale = scale,
                            )
                        }
                    }
                }
            }
            PoweredByMoblinView(backgroundColor = secondaryBackgroundColor, scale = scale)
        }
    }
}

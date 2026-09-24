package com.moblin.android.videoeffects.scoreboard

import androidx.compose.ui.graphics.Color
import com.moblin.android.common.various.color
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.layout.EdgeSet
import com.moblin.android.platform.swiftui.layout.Font
import com.moblin.android.platform.swiftui.layout.HorizontalAlignment
import com.moblin.android.platform.swiftui.layout.View
import com.moblin.android.platform.swiftui.layout.ViewBuilder
import com.moblin.android.platform.swiftui.layout.background
import com.moblin.android.platform.swiftui.layout.bold
import com.moblin.android.platform.swiftui.layout.clipShape
import com.moblin.android.platform.swiftui.layout.font
import com.moblin.android.platform.swiftui.layout.foregroundStyle
import com.moblin.android.platform.swiftui.layout.frame
import com.moblin.android.platform.swiftui.layout.minimumScaleFactor
import com.moblin.android.platform.swiftui.layout.monospacedDigit
import com.moblin.android.platform.swiftui.layout.padding
import com.moblin.android.various.settings.SettingsWidgetGolfScoreboard
import com.moblin.android.various.settings.SettingsWidgetGolfScoreboardPlayer

private fun ViewBuilder.PlayerNameView(
    player: SettingsWidgetGolfScoreboardPlayer,
    playerColor: Boolean,
    scale: Double,
): View = HStack(spacing = 6 * scale) {
    if (playerColor) {
        RoundedRectangle(cornerRadius = 3 * scale)
            .fill(player.color.color())
            .frame(width = 18 * scale, height = 18 * scale)
    }
    Text(player.name.uppercase())
        .font(Font.system(size = 25 * scale))
}
    .frame(height = 35 * scale)
    .padding(EdgeSet.leading, 8 * scale)

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

private fun ViewBuilder.ThruView(
    player: SettingsWidgetGolfScoreboardPlayer,
    numberOfHoles: Int,
    textColor: Color,
    scale: Double,
): View = Text(format(player, numberOfHoles))
    .monospacedDigit()
    .minimumScaleFactor(0.5)
    .font(Font.system(size = 15 * scale))
    .foregroundStyle(textColor.copy(alpha = textColor.alpha * 0.6f))
    .padding(EdgeSet.trailing, 6 * scale)
    .frame(width = 70 * scale, height = 35 * scale)

private fun ViewBuilder.ScoreView(
    player: SettingsWidgetGolfScoreboardPlayer,
    pars: List<Int>,
    numberOfHoles: Int,
    textColor: Color,
    scale: Double,
): View {
    val total = player.totalRelativeToPar(pars = pars, numberOfHoles = numberOfHoles)
    return Text(formatScore(total))
        .monospacedDigit()
        .minimumScaleFactor(0.5)
        .font(Font.system(size = scoreboardScoreFontSize * scale))
        .bold()
        .foregroundStyle(if (total < 0) Color(0xFF34C759) else if (total > 0) Color(0xFFFF3B30) else textColor)
        .padding(EdgeSet.trailing, 8 * scale)
        .frame(height = 35 * scale)
}

fun ViewBuilder.ScoreboardEffectGolfView(
    textColor: Color,
    primaryBackgroundColor: Color,
    secondaryBackgroundColor: Color,
    golf: SettingsWidgetGolfScoreboard,
    scale: Double,
): View {
    val holeIndex = minOf(golf.currentHole, golf.numberOfHoles - 1)
    val par = if (holeIndex in golf.pars.indices) golf.pars[holeIndex] else 4
    return VStack(alignment = HorizontalAlignment.leading, spacing = 0.0) {
        HStack(spacing = 8 * scale) {
            Text(golf.title)
                .font(Font.system(size = 20 * scale))
            Spacer()
            Text(localized("HOLE ${holeIndex + 1}  PAR $par"))
                .monospacedDigit()
                .font(Font.system(size = 18 * scale))
        }
            .bold()
            .padding(EdgeSet.horizontal, 8 * scale)
            .padding(EdgeSet.vertical, 4 * scale)
            .background(secondaryBackgroundColor)
        HStack {
            VStack(alignment = HorizontalAlignment.leading, spacing = 0.0) {
                for (player in golf.players) {
                    PlayerNameView(player = player, playerColor = golf.playerColors, scale = scale)
                }
            }
            Spacer()
            VStack(alignment = HorizontalAlignment.leading, spacing = 0.0) {
                for (player in golf.players) {
                    ThruView(
                        player = player,
                        numberOfHoles = golf.numberOfHoles,
                        textColor = textColor,
                        scale = scale,
                    )
                }
            }
            VStack(alignment = HorizontalAlignment.trailing, spacing = 0.0) {
                for (player in golf.players) {
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
            .background(primaryBackgroundColor)
        PoweredByMoblinView(backgroundColor = secondaryBackgroundColor, scale = scale)
    }
        .clipShape(RoundedRectangle(cornerRadius = 5 * scale))
        .foregroundStyle(textColor)
}

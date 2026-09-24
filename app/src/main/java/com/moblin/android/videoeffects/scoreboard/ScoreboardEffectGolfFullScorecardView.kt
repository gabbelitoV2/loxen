package com.moblin.android.videoeffects.scoreboard

import androidx.compose.ui.graphics.Color
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
import com.moblin.android.platform.swiftui.layout.lineLimit
import com.moblin.android.platform.swiftui.layout.minimumScaleFactor
import com.moblin.android.platform.swiftui.layout.overlay
import com.moblin.android.platform.swiftui.layout.padding
import com.moblin.android.various.settings.SettingsWidgetGolfScoreboard

private val nameCellWidth = 150.0
private val numberCellWidth = 28.0
private val totalCellWidth = 100.0
private val scorecardFontSize = 17.0
private val leftAlignPadding = 8.0

private fun scoreCellColor(strokes: Int, par: Int): Color {
    if (strokes < 0) {
        return Color.Transparent
    }
    val diff = strokes - par
    return when {
        diff <= -2 -> Color(red = 0x31 / 255.0f, green = 0x5C / 255.0f, blue = 0x95 / 255.0f)
        diff == -1 -> Color(red = 0x1D / 255.0f, green = 0x79 / 255.0f, blue = 0x42 / 255.0f)
        diff == 0 -> Color(red = 0x66 / 255.0f, green = 0x66 / 255.0f, blue = 0x66 / 255.0f)
        diff == 1 -> Color(red = 0xA2 / 255.0f, green = 0x10 / 255.0f, blue = 0x10 / 255.0f)
        diff == 2 -> Color(red = 0x6B / 255.0f, green = 0x1C / 255.0f, blue = 0xA9 / 255.0f)
        else -> Color(red = 0x68 / 255.0f, green = 0x39 / 255.0f, blue = 0x15 / 255.0f)
    }
}

private fun ViewBuilder.HeaderCellView(
    text: String,
    width: Double,
    leftAlign: Boolean = false,
    scale: Double,
): View = HStack {
    Text(text)
        .font(Font.system(size = (scorecardFontSize - 1) * scale))
        .lineLimit(1)
        .minimumScaleFactor(0.5)
        .padding(EdgeSet.leading, if (leftAlign) leftAlignPadding * scale else 0.0)
    if (leftAlign) {
        Spacer()
    }
}
    .frame(width = width, height = 20 * scale)
    .overlay(
        Rectangle()
            .stroke(Color(0xFF8E8E93).copy(alpha = 0.4f), lineWidth = 0.5),
    )

private fun ViewBuilder.CellView(
    text: String,
    width: Double,
    background: Color = Color.Transparent,
    bold: Boolean = false,
    leftAlign: Boolean = false,
    scale: Double,
): View = HStack {
    Text(text)
        .font(Font.system(size = scorecardFontSize * scale))
        .bold(bold)
        .lineLimit(1)
        .padding(EdgeSet.leading, if (leftAlign) leftAlignPadding * scale else 0.0)
    if (leftAlign) {
        Spacer()
    }
}
    .frame(width = width, height = 22 * scale)
    .background(background)
    .overlay(
        Rectangle()
            .stroke(Color(0xFF8E8E93).copy(alpha = 0.4f), lineWidth = 0.5),
    )

fun ViewBuilder.ScoreboardEffectGolfFullScorecardView(
    textColor: Color,
    primaryBackgroundColor: Color,
    secondaryBackgroundColor: Color,
    golf: SettingsWidgetGolfScoreboard,
    scale: Double,
): View = VStack(alignment = HorizontalAlignment.leading, spacing = 0.0) {
    HStack(spacing = 0.0) {
        HeaderCellView(text = "", width = nameCellWidth * scale, scale = scale)
        for (holeIndex in 0 until golf.numberOfHoles) {
            HeaderCellView(text = "${holeIndex + 1}", width = numberCellWidth * scale, scale = scale)
        }
        HeaderCellView(text = "", width = totalCellWidth * scale, scale = scale)
    }
        .background(secondaryBackgroundColor)
    if (golf.showPars) {
        HStack(spacing = 0.0) {
            CellView(
                text = localized("PAR"),
                width = nameCellWidth * scale,
                leftAlign = true,
                scale = scale,
            )
            for (holeIndex in 0 until golf.numberOfHoles) {
                val par = if (holeIndex < golf.pars.size) golf.pars[holeIndex] else 4
                CellView(
                    text = "$par",
                    width = numberCellWidth * scale,
                    scale = scale,
                )
            }
            val totalPar = golf.pars.take(golf.numberOfHoles).sum()
            CellView(
                text = "$totalPar",
                width = totalCellWidth * scale,
                bold = true,
                scale = scale,
            )
        }
            .background(secondaryBackgroundColor)
    }
    for (player in golf.players) {
        HStack(spacing = 0.0) {
            CellView(
                text = player.name.uppercase(),
                width = nameCellWidth * scale,
                leftAlign = true,
                scale = scale,
            )
            for (holeIndex in 0 until golf.numberOfHoles) {
                val score = if (holeIndex < player.scores.size) player.scores[holeIndex] else -1
                val par = if (holeIndex < golf.pars.size) golf.pars[holeIndex] else 4
                CellView(
                    text = if (score >= 0) "$score" else "",
                    width = numberCellWidth * scale,
                    background = scoreCellColor(strokes = score, par = par),
                    scale = scale,
                )
            }
            val strokes = player.totalStrokes(numberOfHoles = golf.numberOfHoles)
            val relative = player.totalRelativeToPar(
                pars = golf.pars,
                numberOfHoles = golf.numberOfHoles,
            )
            CellView(
                text = "$strokes (${formatScore(relative)})",
                width = totalCellWidth * scale,
                bold = true,
                scale = scale,
            )
        }
            .background(primaryBackgroundColor)
    }
}
    .clipShape(RoundedRectangle(cornerRadius = 5 * scale))
    .foregroundStyle(textColor)

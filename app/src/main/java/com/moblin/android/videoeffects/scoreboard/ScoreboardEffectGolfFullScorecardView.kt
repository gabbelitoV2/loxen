package com.moblin.android.videoeffects.scoreboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.SwiftUIFonts
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

@Composable
private fun ScorecardShrinkText(
    text: String,
    fontSize: Float,
    color: Color,
    weight: FontWeight,
    minimumScaleFactor: Float,
    modifier: Modifier = Modifier,
) {
    var scaleFactor by remember(text, fontSize) { mutableStateOf(1f) }
    Text(
        text = text,
        color = color,
        style = SwiftUIFonts.system(size = fontSize * scaleFactor, weight = weight),
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        onTextLayout = { result ->
            if (result.hasVisualOverflow && scaleFactor > minimumScaleFactor) {
                scaleFactor = maxOf(minimumScaleFactor, scaleFactor - 0.05f)
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun HeaderCellView(
    text: String,
    width: Double,
    leftAlign: Boolean = false,
    scale: Double,
    color: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        modifier = Modifier
            .border(width = 0.5.dp, color = Color(0xFF8E8E93).copy(alpha = 0.4f))
            .size(width = width.dp, height = (20 * scale).dp)
            .padding(start = (if (leftAlign) leftAlignPadding * scale else 0.0).dp),
    ) {
        ScorecardShrinkText(
            text = text,
            fontSize = ((scorecardFontSize - 1) * scale).toFloat(),
            color = color,
            weight = FontWeight.Normal,
            minimumScaleFactor = 0.5f,
        )
        if (leftAlign) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun CellView(
    text: String,
    width: Double,
    background: Color = Color.Transparent,
    bold: Boolean = false,
    leftAlign: Boolean = false,
    scale: Double,
    color: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        modifier = Modifier
            .border(width = 0.5.dp, color = Color(0xFF8E8E93).copy(alpha = 0.4f))
            .background(background)
            .size(width = width.dp, height = (22 * scale).dp)
            .padding(start = (if (leftAlign) leftAlignPadding * scale else 0.0).dp),
    ) {
        ScorecardShrinkText(
            text = text,
            fontSize = (scorecardFontSize * scale).toFloat(),
            color = color,
            weight = if (bold) FontWeight.Bold else FontWeight.Normal,
            minimumScaleFactor = 1.0f,
        )
        if (leftAlign) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun ScoreboardEffectGolfFullScorecardView(
    textColor: Color,
    primaryBackgroundColor: Color,
    secondaryBackgroundColor: Color,
    golf: SettingsWidgetGolfScoreboard,
    scale: Double,
) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier.clip(RoundedCornerShape((5 * scale).dp)),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.background(secondaryBackgroundColor),
        ) {
            HeaderCellView(
                text = "",
                width = nameCellWidth * scale,
                leftAlign = false,
                scale = scale,
                color = textColor,
            )
            for (holeIndex in 0 until golf.numberOfHoles) {
                HeaderCellView(
                    text = "${holeIndex + 1}",
                    width = numberCellWidth * scale,
                    leftAlign = false,
                    scale = scale,
                    color = textColor,
                )
            }
            HeaderCellView(
                text = "",
                width = totalCellWidth * scale,
                leftAlign = false,
                scale = scale,
                color = textColor,
            )
        }
        if (golf.showPars) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.background(secondaryBackgroundColor),
            ) {
                CellView(
                    text = localized("PAR"),
                    width = nameCellWidth * scale,
                    leftAlign = true,
                    scale = scale,
                    color = textColor,
                )
                for (holeIndex in 0 until golf.numberOfHoles) {
                    val par = if (holeIndex < golf.pars.size) golf.pars[holeIndex] else 4
                    CellView(
                        text = "$par",
                        width = numberCellWidth * scale,
                        scale = scale,
                        color = textColor,
                    )
                }
                val totalPar = golf.pars.take(golf.numberOfHoles).sum()
                CellView(
                    text = "$totalPar",
                    width = totalCellWidth * scale,
                    bold = true,
                    scale = scale,
                    color = textColor,
                )
            }
        }
        Column(modifier = Modifier.background(primaryBackgroundColor)) {
            golf.players.forEach { player ->
                key(player.id) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CellView(
                            text = player.name.uppercase(),
                            width = nameCellWidth * scale,
                            leftAlign = true,
                            scale = scale,
                            color = textColor,
                        )
                        for (holeIndex in 0 until golf.numberOfHoles) {
                            val score = if (holeIndex < player.scores.size) player.scores[holeIndex] else -1
                            val par = if (holeIndex < golf.pars.size) golf.pars[holeIndex] else 4
                            CellView(
                                text = if (score >= 0) "$score" else "",
                                width = numberCellWidth * scale,
                                background = scoreCellColor(strokes = score, par = par),
                                scale = scale,
                                color = textColor,
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
                            color = textColor,
                        )
                    }
                }
            }
        }
    }
}

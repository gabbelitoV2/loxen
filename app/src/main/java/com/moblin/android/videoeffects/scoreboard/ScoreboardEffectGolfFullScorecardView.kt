package com.moblin.android.videoeffects.scoreboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
private fun HeaderCellView(
    text: String,
    width: Double,
    leftAlign: Boolean = false,
    scale: Double,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .width(width.dp)
            .height((20 * scale).dp)
            .border(width = 0.5.dp, color = Color.Gray.copy(alpha = 0.4f)),
    ) {
        Text(
            text = text,
            color = LocalContentColor.current,
            fontSize = ((scorecardFontSize - 1) * scale).sp,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(
                start = if (leftAlign) (leftAlignPadding * scale).dp else 0.dp,
            ),
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
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .width(width.dp)
            .height((22 * scale).dp)
            .background(background)
            .border(width = 0.5.dp, color = Color.Gray.copy(alpha = 0.4f)),
    ) {
        Text(
            text = text,
            color = LocalContentColor.current,
            fontSize = (scorecardFontSize * scale).sp,
            fontWeight = if (bold) FontWeight.Bold else null,
            maxLines = 1,
            modifier = Modifier.padding(
                start = if (leftAlign) (leftAlignPadding * scale).dp else 0.dp,
            ),
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
    val numberOfHoles by golf.numberOfHoles.collectAsState()
    val showPars by golf.showPars.collectAsState()
    val pars by golf.pars.collectAsState()
    val players by golf.players.collectAsState()

    CompositionLocalProvider(LocalContentColor provides textColor) {
        Column(
            modifier = Modifier.clip(RoundedCornerShape((5 * scale).dp)),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.background(secondaryBackgroundColor),
            ) {
                HeaderCellView(text = "", width = nameCellWidth * scale, scale = scale)
                for (holeIndex in 0 until numberOfHoles) {
                    HeaderCellView(
                        text = "${holeIndex + 1}",
                        width = numberCellWidth * scale,
                        scale = scale,
                    )
                }
                HeaderCellView(text = "", width = totalCellWidth * scale, scale = scale)
            }
            if (showPars) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.background(secondaryBackgroundColor),
                ) {
                    CellView(
                        text = "PAR",
                        width = nameCellWidth * scale,
                        leftAlign = true,
                        scale = scale,
                    )
                    for (holeIndex in 0 until numberOfHoles) {
                        val par = if (holeIndex < pars.size) pars[holeIndex] else 4
                        CellView(
                            text = "$par",
                            width = numberCellWidth * scale,
                            scale = scale,
                        )
                    }
                    val totalPar = pars.take(numberOfHoles).sum()
                    CellView(
                        text = "$totalPar",
                        width = totalCellWidth * scale,
                        bold = true,
                        scale = scale,
                    )
                }
            }
            for (player in players) {
                val playerName by player.name.collectAsState()
                val playerScores by player.scores.collectAsState()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.background(primaryBackgroundColor),
                ) {
                    CellView(
                        text = playerName.uppercase(),
                        width = nameCellWidth * scale,
                        leftAlign = true,
                        scale = scale,
                    )
                    for (holeIndex in 0 until numberOfHoles) {
                        val score = if (holeIndex < playerScores.size) playerScores[holeIndex] else -1
                        val par = if (holeIndex < pars.size) pars[holeIndex] else 4
                        CellView(
                            text = if (score >= 0) "$score" else "",
                            width = numberCellWidth * scale,
                            background = scoreCellColor(strokes = score, par = par),
                            scale = scale,
                        )
                    }
                    val strokes = player.totalStrokes(numberOfHoles = numberOfHoles)
                    val relative = player.totalRelativeToPar(
                        pars = pars,
                        numberOfHoles = numberOfHoles,
                    )
                    CellView(
                        text = "$strokes (${formatScore(relative)})",
                        width = totalCellWidth * scale,
                        bold = true,
                        scale = scale,
                    )
                }
            }
        }
    }
}

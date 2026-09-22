package com.moblin.android.videoeffects.scoreboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.remotecontrol.RemoteControlScoreboardMatchConfig
import com.moblin.android.remotecontrol.RemoteControlScoreboardTeam
import com.moblin.android.view.utils.HCenter
import com.moblin.android.various.settings.SettingsWidgetModularScoreboard
import com.moblin.android.various.settings.SettingsWidgetModularScoreboardTeam
import com.moblin.android.various.settings.SettingsWidgetScoreboardLayout

private fun getHistoricScore(team: RemoteControlScoreboardTeam, indexPlusOne: Int): String? {
    return when (indexPlusOne) {
        1 -> team.secondaryScore1
        2 -> team.secondaryScore2
        3 -> team.secondaryScore3
        4 -> team.secondaryScore4
        5 -> team.secondaryScore5
        else -> null
    }
}

private fun calculateMaxHistory(config: RemoteControlScoreboardMatchConfig): Int {
    var maxHistory = 0
    for (indexPlusOne in 1..5) {
        val homeHas = getHistoricScore(team = config.team1, indexPlusOne = indexPlusOne) ?: ""
        val awayHas = getHistoricScore(team = config.team2, indexPlusOne = indexPlusOne) ?: ""
        if (homeHas.isNotEmpty() || awayHas.isNotEmpty()) {
            maxHistory = indexPlusOne
        }
    }
    return maxHistory
}

private fun fontSize(modular: SettingsWidgetModularScoreboard, scale: Double): Double {
    return modular.fontSize() * scale
}

@Composable
private fun stackedHistory(
    modular: SettingsWidgetModularScoreboard,
    config: RemoteControlScoreboardMatchConfig,
    scale: Double,
) {
    title(modular = modular, config = config, scale = scale)
    Row(verticalAlignment = Alignment.Top) {
        val maxHistory = calculateMaxHistory(config = config)
        val histWidth = fontSize(modular = modular, scale = scale) * 1.5
        Column(
            modifier = Modifier.width((modular.width * scale + maxHistory * histWidth).dp),
        ) {
            stackedHistoryTeam(
                modular = modular,
                config = config,
                scale = scale,
                team = config.team1,
                otherTeam = config.team2,
                modularTeam = modular.home,
                histCount = maxHistory,
                histWidth = histWidth,
            )
            stackedHistoryTeam(
                modular = modular,
                config = config,
                scale = scale,
                team = config.team2,
                otherTeam = config.team1,
                modularTeam = modular.away,
                histCount = maxHistory,
                histWidth = histWidth,
            )
        }
        infoBox(modular = modular, config = config, scale = scale)
    }
}

@Composable
private fun stackedHistoryTeam(
    modular: SettingsWidgetModularScoreboard,
    config: RemoteControlScoreboardMatchConfig,
    scale: Double,
    team: RemoteControlScoreboardTeam,
    otherTeam: RemoteControlScoreboardTeam,
    modularTeam: SettingsWidgetModularScoreboardTeam,
    histCount: Int,
    histWidth: Double,
) {
    Column {
        val currentPeriod = config.global.period.toIntOrNull() ?: 1
        val height = modular.rowHeight * scale
        Row(
            modifier = Modifier
                .height(height.dp)
                .background(modularTeam.backgroundColorColor),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            teamName(
                modular = modular,
                scale = scale,
                team = modularTeam,
                color = modularTeam.textColorColor,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = (8 * scale).dp, end = (2 * scale).dp),
            )
            possession(show = team.possession, modular = modular, scale = scale)
            if (histCount > 0) {
                for (indexPlusOne in 1..histCount) {
                    val value = getHistoricScore(team = team, indexPlusOne = indexPlusOne) ?: ""
                    val otherValue = getHistoricScore(team = otherTeam, indexPlusOne = indexPlusOne) ?: ""
                    val valueInt = value.toIntOrNull() ?: -1
                    val otherValueInt = otherValue.toIntOrNull() ?: -1
                    val weight: FontWeight =
                        if (indexPlusOne < currentPeriod && valueInt > otherValueInt && valueInt >= 0) {
                            if (modular.isBold) FontWeight.Black else FontWeight.Bold
                        } else {
                            if (modular.isBold) FontWeight.Bold else FontWeight.Normal
                        }
                    when {
                        value.isNotEmpty() -> stat(
                            modular = modular,
                            scale = scale,
                            value = value,
                            fontSize = fontSize(modular = modular, scale = scale) * 0.9,
                            width = histWidth,
                            gray = true,
                            color = modularTeam.textColorColor,
                            weight = weight,
                        )

                        otherValue.isNotEmpty() -> stat(
                            modular = modular,
                            scale = scale,
                            value = "0",
                            fontSize = fontSize(modular = modular, scale = scale) * 0.9,
                            width = histWidth,
                            gray = true,
                            color = modularTeam.textColorColor,
                            weight = if (modular.isBold) FontWeight.Bold else FontWeight.Normal,
                        )

                        else -> Spacer(modifier = Modifier.width(histWidth.dp))
                    }
                }
            }
            primaryScore(
                modular = modular,
                scale = scale,
                team = team,
                color = modularTeam.textColorColor,
            )
        }
        moreStats(
            modular = modular,
            scale = scale,
            team = team,
            height = height * 0.6,
            backgroundColor = modularTeam.backgroundColorColor,
            color = modularTeam.textColorColor,
        )
    }
}

@Composable
private fun stacked(
    modular: SettingsWidgetModularScoreboard,
    config: RemoteControlScoreboardMatchConfig,
    scale: Double,
) {
    title(modular = modular, config = config, scale = scale)
    Row(verticalAlignment = Alignment.Top) {
        Column(
            modifier = Modifier.width((modular.width * scale).dp),
        ) {
            stackedTeam(
                modular = modular,
                config = config,
                scale = scale,
                team = config.team1,
                modularTeam = modular.home,
            )
            stackedTeam(
                modular = modular,
                config = config,
                scale = scale,
                team = config.team2,
                modularTeam = modular.away,
            )
        }
        infoBox(modular = modular, config = config, scale = scale)
    }
}

@Composable
private fun sideBySide(
    modular: SettingsWidgetModularScoreboard,
    config: RemoteControlScoreboardMatchConfig,
    scale: Double,
) {
    title(modular = modular, config = config, scale = scale)
    Row {
        Box(modifier = Modifier.width((modular.width * scale).dp)) {
            sideBySideTeam(
                modular = modular,
                config = config,
                scale = scale,
                team = config.team1,
                modularTeam = modular.home,
                mirrored = false,
            )
        }
        val height = modular.rowHeight * scale
        val teamRowFullHeight = height + (if (modular.showMoreStats) height * 0.6 else 0.0)
        if (modular.showGlobalStatsBlock) {
            Column(
                modifier = Modifier
                    .width((fontSize(modular = modular, scale = scale) * 3.5).dp)
                    .height(teamRowFullHeight.dp)
                    .background(Color.Black),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val periodFull = config.periodFull()
                if (periodFull.isNotEmpty()) {
                    Text(
                        text = periodFull,
                        color = Color.White,
                        fontSize = (fontSize(modular = modular, scale = scale) * 0.6).sp,
                        fontWeight = if (modular.isBold) FontWeight.Bold else FontWeight.Normal,
                    )
                }
                if (modular.showClock) {
                    Text(
                        text = config.global.timer,
                        color = Color.White,
                        fontSize = (fontSize(modular = modular, scale = scale) * 0.9).sp,
                        fontWeight = if (modular.isBold) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .height(teamRowFullHeight.dp)
                    .background(Color.Black),
            ) {
                Text(
                    text = "-",
                    color = Color.White,
                    fontSize = fontSize(modular = modular, scale = scale).sp,
                    fontWeight = if (modular.isBold) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
        Box(modifier = Modifier.width((modular.width * scale).dp)) {
            sideBySideTeam(
                modular = modular,
                config = config,
                scale = scale,
                team = config.team2,
                modularTeam = modular.away,
                mirrored = true,
            )
        }
    }
}

@Composable
private fun stackedTeam(
    modular: SettingsWidgetModularScoreboard,
    config: RemoteControlScoreboardMatchConfig,
    scale: Double,
    team: RemoteControlScoreboardTeam,
    modularTeam: SettingsWidgetModularScoreboardTeam,
) {
    Column {
        val height = modular.rowHeight * scale
        val width = fontSize(modular = modular, scale = scale) * 1.55
        Row(
            modifier = Modifier
                .height(height.dp)
                .background(modularTeam.backgroundColorColor),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (modular.layout == SettingsWidgetScoreboardLayout.stacked) {
                stat(
                    modular = modular,
                    scale = scale,
                    value = team.secondaryScore,
                    label = team.secondaryScoreLabel,
                    fontSize = fontSize(modular = modular, scale = scale),
                    width = width,
                    gray = true,
                    color = modularTeam.textColorColor,
                )
            }
            teamName(
                modular = modular,
                scale = scale,
                team = modularTeam,
                color = modularTeam.textColorColor,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = (8 * scale).dp),
            )
            possession(show = team.possession, modular = modular, scale = scale)
            if (modular.layout == SettingsWidgetScoreboardLayout.stackedInline) {
                stat(
                    modular = modular,
                    scale = scale,
                    value = team.secondaryScore,
                    label = team.secondaryScoreLabel,
                    fontSize = fontSize(modular = modular, scale = scale),
                    width = width,
                    gray = true,
                    color = modularTeam.textColorColor,
                )
            }
            primaryScore(
                modular = modular,
                scale = scale,
                team = team,
                color = modularTeam.textColorColor,
            )
        }
        moreStats(
            modular = modular,
            scale = scale,
            team = team,
            height = height * 0.6,
            backgroundColor = modularTeam.backgroundColorColor,
            color = modularTeam.textColorColor,
        )
    }
}

@Composable
private fun sideBySideTeam(
    modular: SettingsWidgetModularScoreboard,
    config: RemoteControlScoreboardMatchConfig,
    scale: Double,
    team: RemoteControlScoreboardTeam,
    modularTeam: SettingsWidgetModularScoreboardTeam,
    mirrored: Boolean,
) {
    val height = modular.rowHeight * scale
    val width = fontSize(modular = modular, scale = scale) * 1.55
    Column {
        Row(
            modifier = Modifier
                .height(height.dp)
                .background(modularTeam.backgroundColorColor),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!mirrored) {
                possession(show = team.possession, modular = modular, scale = scale)
                teamName(
                    modular = modular,
                    scale = scale,
                    team = modularTeam,
                    color = modularTeam.textColorColor,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = (4 * scale).dp),
                )
                stat(
                    modular = modular,
                    scale = scale,
                    value = team.secondaryScore,
                    label = team.secondaryScoreLabel,
                    fontSize = fontSize(modular = modular, scale = scale),
                    width = width,
                    gray = true,
                    color = modularTeam.textColorColor,
                )
                primaryScore(
                    modular = modular,
                    scale = scale,
                    team = team,
                    color = modularTeam.textColorColor,
                )
            } else {
                primaryScore(
                    modular = modular,
                    scale = scale,
                    team = team,
                    color = modularTeam.textColorColor,
                )
                stat(
                    modular = modular,
                    scale = scale,
                    value = team.secondaryScore,
                    label = team.secondaryScoreLabel,
                    fontSize = fontSize(modular = modular, scale = scale),
                    width = width,
                    gray = true,
                    color = modularTeam.textColorColor,
                )
                teamName(
                    modular = modular,
                    scale = scale,
                    team = modularTeam,
                    color = modularTeam.textColorColor,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = (4 * scale).dp),
                )
                possession(show = team.possession, modular = modular, scale = scale)
            }
        }
        moreStats(
            modular = modular,
            scale = scale,
            team = team,
            height = height * 0.6,
            backgroundColor = modularTeam.backgroundColorColor,
            color = modularTeam.textColorColor,
            alignRight = !mirrored,
        )
    }
}

@Composable
private fun stat(
    modular: SettingsWidgetModularScoreboard,
    scale: Double,
    value: String,
    label: String? = null,
    fontSize: Double,
    width: Double,
    gray: Boolean,
    color: Color,
    weight: FontWeight? = null,
) {
    if (value.isNotEmpty()) {
        val resolvedWeight = weight ?: (if (modular.isBold) FontWeight.Black else FontWeight.Bold)
        Box(modifier = Modifier.width(width.dp)) {
            if (gray) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.25f)),
                )
            }
            if (label != null && label.isNotEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy((-2.0 * scale).dp),
                ) {
                    Text(
                        text = label,
                        color = color,
                        fontSize = (fontSize * 0.25).sp,
                        fontWeight = if (modular.isBold) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.offset(y = (fontSize * 0.04).dp),
                    )
                    Text(
                        text = value,
                        color = color,
                        fontSize = (fontSize * 0.75).sp,
                        fontWeight = resolvedWeight,
                    )
                }
            } else {
                Text(
                    text = value,
                    color = color,
                    fontSize = fontSize.sp,
                    fontWeight = resolvedWeight,
                )
            }
        }
    }
}

@Composable
private fun primaryScore(
    modular: SettingsWidgetModularScoreboard,
    scale: Double,
    team: RemoteControlScoreboardTeam,
    color: Color,
) {
    stat(
        modular = modular,
        scale = scale,
        value = team.primaryScore,
        fontSize = fontSize(modular = modular, scale = scale),
        width = fontSize(modular = modular, scale = scale) * 1.55,
        gray = false,
        color = color,
    )
}

@Composable
private fun teamName(
    modular: SettingsWidgetModularScoreboard,
    scale: Double,
    team: SettingsWidgetModularScoreboardTeam,
    color: Color,
    textAlign: TextAlign = TextAlign.Start,
    modifier: Modifier = Modifier,
) {
    Text(
        text = team.name,
        color = color,
        fontSize = fontSize(modular = modular, scale = scale).sp,
        fontWeight = if (modular.isBold) FontWeight.Bold else FontWeight.Normal,
        maxLines = 1,
        textAlign = textAlign,
        modifier = modifier,
    )
}

@Composable
private fun possession(
    show: Boolean,
    modular: SettingsWidgetModularScoreboard,
    scale: Double,
) {
    if (show) {
        Image(
            painter = painterResource(id = TODO("asset VolleyballIndicator")),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.padding((fontSize(modular = modular, scale = scale) * 0.1).dp),
        )
    }
}

@Composable
private fun moreStats(
    modular: SettingsWidgetModularScoreboard,
    scale: Double,
    team: RemoteControlScoreboardTeam,
    height: Double,
    backgroundColor: Color,
    color: Color,
    alignRight: Boolean = false,
) {
    if (modular.showMoreStats) {
        val stats = listOf(
            Triple(0, team.stat1Label, team.stat1),
            Triple(1, team.stat2Label, team.stat2),
            Triple(2, team.stat3Label, team.stat3),
            Triple(3, team.stat4Label, team.stat4),
        ).filter { !it.third.isEmpty() && !it.third.startsWith("NO ") }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height.dp)
                .background(backgroundColor),
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.25f)),
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = (6 * scale).dp),
                horizontalArrangement = Arrangement.spacedBy((8 * scale).dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (alignRight) {
                    Spacer(modifier = Modifier.weight(1f))
                }
                for ((_, label, value) in stats) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (label.isNotEmpty()) {
                            Text(
                                text = label + ":",
                                color = color,
                                fontSize = fontSize(modular = modular, scale = scale).sp,
                                fontWeight = if (modular.isBold) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.alpha(0.8f),
                            )
                        }
                        Text(
                            text = value,
                            color = color,
                            fontSize = fontSize(modular = modular, scale = scale).sp,
                            fontWeight = if (modular.isBold) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
                if (!alignRight) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun infoBox(
    modular: SettingsWidgetModularScoreboard,
    config: RemoteControlScoreboardMatchConfig,
    scale: Double,
) {
    if (modular.showGlobalStatsBlock) {
        val stats = config.infoBoxStats(showClock = modular.showClock)
        if (stats.isNotEmpty()) {
            val rowHeight = modular.rowHeight * scale
            val fullHeight = rowHeight + (if (modular.showMoreStats) rowHeight * 0.6 else 0.0)
            val height = fullHeight * 2
            Column(
                modifier = Modifier
                    .height(height.dp)
                    .background(Color.Black),
            ) {
                for (index in stats.indices) {
                    HCenter {
                        Text(
                            text = stats[index],
                            color = Color.White,
                            fontSize = fontSize(modular = modular, scale = scale).sp,
                            fontWeight = if (modular.isBold) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun title(
    modular: SettingsWidgetModularScoreboard,
    config: RemoteControlScoreboardMatchConfig,
    scale: Double,
) {
    if (modular.showTitle) {
        Box(modifier = Modifier.background(Color.Black)) {
            HCenter {
                Text(
                    text = config.global.title,
                    color = Color.White,
                    fontSize = (fontSize(modular = modular, scale = scale) * 0.7).sp,
                    fontWeight = if (modular.isBold) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(vertical = (1 * scale).dp),
                )
            }
        }
    }
}

@Composable
fun ScoreboardEffectModularView(
    modular: SettingsWidgetModularScoreboard,
    config: RemoteControlScoreboardMatchConfig,
    scale: Double,
) {
    Column {
        when (modular.layout) {
            SettingsWidgetScoreboardLayout.sideBySide ->
                sideBySide(modular = modular, config = config, scale = scale)

            SettingsWidgetScoreboardLayout.stackHistory ->
                stackedHistory(modular = modular, config = config, scale = scale)

            else -> stacked(modular = modular, config = config, scale = scale)
        }
    }
}

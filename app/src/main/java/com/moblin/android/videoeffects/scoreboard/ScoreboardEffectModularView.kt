package com.moblin.android.videoeffects.scoreboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.platform.swiftui.AssetImage
import com.moblin.android.platform.swiftui.SwiftUIFonts
import com.moblin.android.platform.swiftui.monospacedDigit
import com.moblin.android.remotecontrol.RemoteControlScoreboardMatchConfig
import com.moblin.android.remotecontrol.RemoteControlScoreboardTeam
import com.moblin.android.various.settings.SettingsWidgetModularScoreboard
import com.moblin.android.various.settings.SettingsWidgetModularScoreboardTeam
import com.moblin.android.various.settings.SettingsWidgetScoreboardLayout
import com.moblin.android.view.utils.HCenter

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
    return modular.fontSize().toDouble() * scale
}

@Composable
fun ScoreboardEffectModularView(
    modular: SettingsWidgetModularScoreboard,
    config: RemoteControlScoreboardMatchConfig,
    scale: Double,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        when (modular.layout) {
            SettingsWidgetScoreboardLayout.sideBySide -> SideBySide(
                modular = modular,
                config = config,
                scale = scale,
            )

            SettingsWidgetScoreboardLayout.stackHistory -> StackedHistory(
                modular = modular,
                config = config,
                scale = scale,
            )

            else -> Stacked(
                modular = modular,
                config = config,
                scale = scale,
            )
        }
    }
}

@Composable
private fun StackedHistory(
    modular: SettingsWidgetModularScoreboard,
    config: RemoteControlScoreboardMatchConfig,
    scale: Double,
) {
    Title(modular = modular, config = config, scale = scale)
    val maxHistory = calculateMaxHistory(config = config)
    val histWidth = fontSize(modular = modular, scale = scale) * 1.5
    Row(verticalAlignment = Alignment.Top) {
        Column(
            modifier = Modifier.width((modular.width.toDouble() * scale + maxHistory * histWidth).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StackedHistoryTeam(
                team = config.team1,
                otherTeam = config.team2,
                modularTeam = modular.home,
                histCount = maxHistory,
                histWidth = histWidth,
                config = config,
                modular = modular,
                scale = scale,
            )
            StackedHistoryTeam(
                team = config.team2,
                otherTeam = config.team1,
                modularTeam = modular.away,
                histCount = maxHistory,
                histWidth = histWidth,
                config = config,
                modular = modular,
                scale = scale,
            )
        }
        InfoBox(modular = modular, config = config, scale = scale)
    }
}

@Composable
private fun StackedHistoryTeam(
    team: RemoteControlScoreboardTeam,
    otherTeam: RemoteControlScoreboardTeam,
    modularTeam: SettingsWidgetModularScoreboardTeam,
    histCount: Int,
    histWidth: Double,
    config: RemoteControlScoreboardMatchConfig,
    modular: SettingsWidgetModularScoreboard,
    scale: Double,
) {
    val currentPeriod = config.global.period.toIntOrNull() ?: 1
    val height = modular.rowHeight.toDouble() * scale
    CompositionLocalProvider(LocalContentColor provides modularTeam.textColorColor) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier
                    .background(modularTeam.backgroundColorColor)
                    .height(height.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TeamName(
                    team = modularTeam,
                    modular = modular,
                    scale = scale,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = (8 * scale).dp, end = (2 * scale).dp),
                )
                Possession(show = team.possession, modular = modular, scale = scale)
                if (histCount > 0) {
                    for (indexPlusOne in 1..histCount) {
                        key(indexPlusOne) {
                            val value = getHistoricScore(team = team, indexPlusOne = indexPlusOne) ?: ""
                            val otherValue =
                                getHistoricScore(team = otherTeam, indexPlusOne = indexPlusOne) ?: ""
                            val valueInt = value.toIntOrNull() ?: -1
                            val otherValueInt = otherValue.toIntOrNull() ?: -1
                            val weight =
                                if (indexPlusOne < currentPeriod &&
                                    valueInt > otherValueInt &&
                                    valueInt >= 0
                                ) {
                                    if (modular.isBold) FontWeight.Black else FontWeight.Bold
                                } else {
                                    if (modular.isBold) FontWeight.Bold else FontWeight.Normal
                                }
                            if (value.isNotEmpty()) {
                                Stat(
                                    value = value,
                                    fontSize = fontSize(modular = modular, scale = scale) * 0.9,
                                    width = histWidth,
                                    gray = true,
                                    weight = weight,
                                    modular = modular,
                                    scale = scale,
                                )
                            } else if (otherValue.isNotEmpty()) {
                                Stat(
                                    value = "0",
                                    fontSize = fontSize(modular = modular, scale = scale) * 0.9,
                                    width = histWidth,
                                    gray = true,
                                    weight = if (modular.isBold) FontWeight.Bold else FontWeight.Normal,
                                    modular = modular,
                                    scale = scale,
                                )
                            } else {
                                Spacer(modifier = Modifier.width(histWidth.dp))
                            }
                        }
                    }
                }
                PrimaryScore(team = team, modular = modular, scale = scale)
            }
            MoreStats(
                team = team,
                height = height * 0.6,
                backgroundColor = modularTeam.backgroundColorColor,
                modular = modular,
                scale = scale,
            )
        }
    }
}

@Composable
private fun Stacked(
    modular: SettingsWidgetModularScoreboard,
    config: RemoteControlScoreboardMatchConfig,
    scale: Double,
) {
    Title(modular = modular, config = config, scale = scale)
    Row(verticalAlignment = Alignment.Top) {
        Column(
            modifier = Modifier.width((modular.width.toDouble() * scale).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StackedTeam(team = config.team1, modularTeam = modular.home, modular = modular, scale = scale)
            StackedTeam(team = config.team2, modularTeam = modular.away, modular = modular, scale = scale)
        }
        InfoBox(modular = modular, config = config, scale = scale)
    }
}

@Composable
private fun SideBySide(
    modular: SettingsWidgetModularScoreboard,
    config: RemoteControlScoreboardMatchConfig,
    scale: Double,
) {
    Title(modular = modular, config = config, scale = scale)
    Row(verticalAlignment = Alignment.CenterVertically) {
        SideBySideTeam(
            team = config.team1,
            modularTeam = modular.home,
            mirrored = false,
            modular = modular,
            scale = scale,
            modifier = Modifier.width((modular.width.toDouble() * scale).dp),
        )
        val height = modular.rowHeight.toDouble() * scale
        val teamRowFullHeight = height + (if (modular.showMoreStats) height * 0.6 else 0.0)
        val textWeight = if (modular.isBold) FontWeight.Bold else FontWeight.Normal
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            if (modular.showGlobalStatsBlock) {
                Column(
                    modifier = Modifier
                        .background(Color.Black)
                        .height(teamRowFullHeight.dp)
                        .width((fontSize(modular = modular, scale = scale) * 3.5).dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val periodFull = config.periodFull()
                    if (periodFull.isNotEmpty()) {
                        MinimumScaleText(
                            text = periodFull,
                            style = SwiftUIFonts.system(
                                size = (fontSize(modular = modular, scale = scale) * 0.6).toFloat(),
                                weight = textWeight,
                            ),
                            minScaleFactor = 0.1f,
                        )
                    }
                    if (modular.showClock) {
                        MinimumScaleText(
                            text = config.global.timer,
                            style = SwiftUIFonts.system(
                                size = (fontSize(modular = modular, scale = scale) * 0.9).toFloat(),
                                weight = textWeight,
                            ).monospacedDigit(),
                            minScaleFactor = 0.1f,
                        )
                    }
                }
            } else {
                Text(
                    text = "-",
                    modifier = Modifier
                        .background(Color.Black)
                        .height(teamRowFullHeight.dp),
                    style = SwiftUIFonts.system(
                        size = fontSize(modular = modular, scale = scale).toFloat(),
                        weight = textWeight,
                    ),
                )
            }
        }
        SideBySideTeam(
            team = config.team2,
            modularTeam = modular.away,
            mirrored = true,
            modular = modular,
            scale = scale,
            modifier = Modifier.width((modular.width.toDouble() * scale).dp),
        )
    }
}

@Composable
private fun StackedTeam(
    team: RemoteControlScoreboardTeam,
    modularTeam: SettingsWidgetModularScoreboardTeam,
    modular: SettingsWidgetModularScoreboard,
    scale: Double,
) {
    val height = modular.rowHeight.toDouble() * scale
    val width = fontSize(modular = modular, scale = scale) * 1.55
    CompositionLocalProvider(LocalContentColor provides modularTeam.textColorColor) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier
                    .background(modularTeam.backgroundColorColor)
                    .height(height.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (modular.layout == SettingsWidgetScoreboardLayout.stacked) {
                    Stat(
                        value = team.secondaryScore,
                        label = team.secondaryScoreLabel,
                        fontSize = fontSize(modular = modular, scale = scale),
                        width = width,
                        gray = true,
                        modular = modular,
                        scale = scale,
                    )
                }
                TeamName(
                    team = modularTeam,
                    modular = modular,
                    scale = scale,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = (8 * scale).dp),
                )
                Possession(show = team.possession, modular = modular, scale = scale)
                if (modular.layout == SettingsWidgetScoreboardLayout.stackedInline) {
                    Stat(
                        value = team.secondaryScore,
                        label = team.secondaryScoreLabel,
                        fontSize = fontSize(modular = modular, scale = scale),
                        width = width,
                        gray = true,
                        modular = modular,
                        scale = scale,
                    )
                }
                PrimaryScore(team = team, modular = modular, scale = scale)
            }
            MoreStats(
                team = team,
                height = height * 0.6,
                backgroundColor = modularTeam.backgroundColorColor,
                modular = modular,
                scale = scale,
            )
        }
    }
}

@Composable
private fun SideBySideTeam(
    team: RemoteControlScoreboardTeam,
    modularTeam: SettingsWidgetModularScoreboardTeam,
    mirrored: Boolean,
    modular: SettingsWidgetModularScoreboard,
    scale: Double,
    modifier: Modifier = Modifier,
) {
    val height = modular.rowHeight.toDouble() * scale
    val width = fontSize(modular = modular, scale = scale) * 1.55
    CompositionLocalProvider(LocalContentColor provides modularTeam.textColorColor) {
        Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier
                    .background(modularTeam.backgroundColorColor)
                    .height(height.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!mirrored) {
                    Possession(show = team.possession, modular = modular, scale = scale)
                    TeamName(
                        team = modularTeam,
                        modular = modular,
                        scale = scale,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = (4 * scale).dp),
                    )
                    Stat(
                        value = team.secondaryScore,
                        label = team.secondaryScoreLabel,
                        fontSize = fontSize(modular = modular, scale = scale),
                        width = width,
                        gray = true,
                        modular = modular,
                        scale = scale,
                    )
                    PrimaryScore(team = team, modular = modular, scale = scale)
                } else {
                    PrimaryScore(team = team, modular = modular, scale = scale)
                    Stat(
                        value = team.secondaryScore,
                        label = team.secondaryScoreLabel,
                        fontSize = fontSize(modular = modular, scale = scale),
                        width = width,
                        gray = true,
                        modular = modular,
                        scale = scale,
                    )
                    TeamName(
                        team = modularTeam,
                        modular = modular,
                        scale = scale,
                        textAlign = TextAlign.Start,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = (4 * scale).dp),
                    )
                    Possession(show = team.possession, modular = modular, scale = scale)
                }
            }
            MoreStats(
                team = team,
                height = height * 0.6,
                backgroundColor = modularTeam.backgroundColorColor,
                alignRight = !mirrored,
                modular = modular,
                scale = scale,
            )
        }
    }
}

@Composable
private fun Stat(
    value: String,
    label: String? = null,
    fontSize: Double,
    width: Double,
    gray: Boolean,
    weight: FontWeight? = null,
    modular: SettingsWidgetModularScoreboard,
    scale: Double,
) {
    if (value.isEmpty()) {
        return
    }
    val resolvedWeight = weight ?: if (modular.isBold) FontWeight.Black else FontWeight.Bold
    Box(
        modifier = Modifier
            .width(width.dp)
            .fillMaxHeight(),
        contentAlignment = Alignment.Center,
    ) {
        if (gray) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.25f)),
            )
        }
        if (label != null && label.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy((-2 * scale).dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = label,
                    modifier = Modifier.offset(x = 0.dp, y = (fontSize * 0.04).dp),
                    style = SwiftUIFonts.system(
                        size = (fontSize * 0.25).toFloat(),
                        weight = if (modular.isBold) FontWeight.Bold else FontWeight.Normal,
                    ),
                )
                Text(
                    text = value,
                    style = SwiftUIFonts.system(
                        size = (fontSize * 0.75).toFloat(),
                        weight = resolvedWeight,
                    ),
                )
            }
        } else {
            Text(
                text = value,
                style = SwiftUIFonts.system(
                    size = fontSize.toFloat(),
                    weight = resolvedWeight,
                ),
            )
        }
    }
}

@Composable
private fun PrimaryScore(
    team: RemoteControlScoreboardTeam,
    modular: SettingsWidgetModularScoreboard,
    scale: Double,
) {
    Stat(
        value = team.primaryScore,
        fontSize = fontSize(modular = modular, scale = scale),
        width = fontSize(modular = modular, scale = scale) * 1.55,
        gray = false,
        modular = modular,
        scale = scale,
    )
}

@Composable
private fun TeamName(
    team: SettingsWidgetModularScoreboardTeam,
    modular: SettingsWidgetModularScoreboard,
    scale: Double,
    textAlign: TextAlign? = null,
    modifier: Modifier = Modifier,
) {
    MinimumScaleText(
        text = team.name,
        style = SwiftUIFonts.system(
            size = fontSize(modular = modular, scale = scale).toFloat(),
            weight = if (modular.isBold) FontWeight.Bold else FontWeight.Normal,
        ),
        minScaleFactor = 0.1f,
        modifier = modifier,
        textAlign = textAlign,
    )
}

@Composable
private fun Possession(
    show: Boolean,
    modular: SettingsWidgetModularScoreboard,
    scale: Double,
) {
    if (show) {
        AssetImage(
            name = "VolleyballIndicator",
            modifier = Modifier.padding((fontSize(modular = modular, scale = scale) * 0.1).dp),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun MoreStats(
    team: RemoteControlScoreboardTeam,
    height: Double,
    backgroundColor: Color,
    alignRight: Boolean = false,
    modular: SettingsWidgetModularScoreboard,
    scale: Double,
) {
    if (!modular.showMoreStats) {
        return
    }
    val stats = listOf(
        Triple(0, team.stat1Label, team.stat1),
        Triple(1, team.stat2Label, team.stat2),
        Triple(2, team.stat3Label, team.stat3),
        Triple(3, team.stat4Label, team.stat4),
    ).filter { !it.third.isEmpty() && !it.third.startsWith("NO ") }
    val textSize = fontSize(modular = modular, scale = scale)
    val style = SwiftUIFonts.system(
        size = textSize.toFloat(),
        weight = if (modular.isBold) FontWeight.Bold else FontWeight.Normal,
    )
    Box(
        modifier = Modifier
            .background(backgroundColor)
            .background(Color.Black.copy(alpha = 0.25f))
            .height(height.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = (6 * scale).dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy((8 * scale).dp),
        ) {
            if (alignRight) {
                Spacer(modifier = Modifier.weight(1f))
            }
            stats.forEach { (id, label, value) ->
                key(id) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        if (label.isNotEmpty()) {
                            MinimumScaleText(
                                text = label + ":",
                                style = style,
                                minScaleFactor = 0.3f,
                                modifier = Modifier.alpha(0.8f),
                            )
                        }
                        MinimumScaleText(
                            text = value,
                            style = style.monospacedDigit(),
                            minScaleFactor = 0.3f,
                        )
                    }
                }
            }
            if (!alignRight) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun InfoBox(
    modular: SettingsWidgetModularScoreboard,
    config: RemoteControlScoreboardMatchConfig,
    scale: Double,
) {
    if (!modular.showGlobalStatsBlock) {
        return
    }
    val stats = config.infoBoxStats(showClock = modular.showClock)
    if (stats.isEmpty()) {
        return
    }
    val rowHeight = modular.rowHeight.toDouble() * scale
    val fullHeight = rowHeight + (if (modular.showMoreStats) rowHeight * 0.6 else 0.0)
    val height = fullHeight * 2
    val style = SwiftUIFonts.system(
        size = fontSize(modular = modular, scale = scale).toFloat(),
        weight = if (modular.isBold) FontWeight.Bold else FontWeight.Normal,
    ).monospacedDigit()
    CompositionLocalProvider(LocalContentColor provides Color.White) {
        Column(
            modifier = Modifier
                .background(Color.Black)
                .height(height.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            stats.forEachIndexed { index, value ->
                key(index) {
                    HCenter {
                        MinimumScaleText(
                            text = value,
                            style = style,
                            minScaleFactor = 0.1f,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Title(
    modular: SettingsWidgetModularScoreboard,
    config: RemoteControlScoreboardMatchConfig,
    scale: Double,
) {
    if (!modular.showTitle) {
        return
    }
    CompositionLocalProvider(LocalContentColor provides Color.White) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = config.global.title,
                modifier = Modifier.padding(vertical = (1 * scale).dp),
                style = SwiftUIFonts.system(
                    size = (fontSize(modular = modular, scale = scale) * 0.7).toFloat(),
                    weight = if (modular.isBold) FontWeight.Bold else FontWeight.Normal,
                ),
            )
        }
    }
}

@Composable
private fun MinimumScaleText(
    text: String,
    style: TextStyle,
    minScaleFactor: Float,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    maxLines: Int = 1,
) {
    var factor by remember { mutableStateOf(1f) }
    val baseSize = style.fontSize.value
    Text(
        text = text,
        modifier = modifier,
        style = style.copy(fontSize = (baseSize * factor).sp),
        maxLines = maxLines,
        textAlign = textAlign,
        onTextLayout = { result ->
            if (result.hasVisualOverflow && factor > minScaleFactor) {
                factor = (factor - 0.05f).coerceAtLeast(minScaleFactor)
            }
        },
    )
}

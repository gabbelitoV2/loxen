package com.moblin.android.videoeffects.scoreboard

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.moblin.android.platform.swiftui.layout.Alignment
import com.moblin.android.platform.swiftui.layout.EdgeSet
import com.moblin.android.platform.swiftui.layout.Font
import com.moblin.android.platform.swiftui.layout.VerticalAlignment
import com.moblin.android.platform.swiftui.layout.View
import com.moblin.android.platform.swiftui.layout.ViewBuilder
import com.moblin.android.platform.swiftui.layout.background
import com.moblin.android.platform.swiftui.layout.bold
import com.moblin.android.platform.swiftui.layout.font
import com.moblin.android.platform.swiftui.layout.foregroundStyle
import com.moblin.android.platform.swiftui.layout.frame
import com.moblin.android.platform.swiftui.layout.lineLimit
import com.moblin.android.platform.swiftui.layout.minimumScaleFactor
import com.moblin.android.platform.swiftui.layout.monospacedDigit
import com.moblin.android.platform.swiftui.layout.offset
import com.moblin.android.platform.swiftui.layout.opacity
import com.moblin.android.platform.swiftui.layout.padding
import com.moblin.android.platform.swiftui.layout.resizable
import com.moblin.android.platform.swiftui.layout.scaledToFit
import com.moblin.android.remotecontrol.RemoteControlScoreboardMatchConfig
import com.moblin.android.remotecontrol.RemoteControlScoreboardTeam
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

private fun ViewBuilder.HCenter(content: ViewBuilder.() -> Unit): View = HStack {
    Spacer()
    content()
    Spacer()
}

private class ModularScoreboardContent(
    val modular: SettingsWidgetModularScoreboard,
    val config: RemoteControlScoreboardMatchConfig,
    val scale: Double,
) {
    private fun calculateMaxHistory(): Int {
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

    private fun fontSize(): Double = modular.fontSize() * scale

    private fun ViewBuilder.stackedHistory() {
        title()
        HStack(alignment = VerticalAlignment.top, spacing = 0.0) {
            val maxHistory = calculateMaxHistory()
            val histWidth = fontSize() * 1.5
            VStack(spacing = 0.0) {
                stackedHistoryTeam(
                    team = config.team1,
                    otherTeam = config.team2,
                    modularTeam = modular.home,
                    histCount = maxHistory,
                    histWidth = histWidth,
                )
                stackedHistoryTeam(
                    team = config.team2,
                    otherTeam = config.team1,
                    modularTeam = modular.away,
                    histCount = maxHistory,
                    histWidth = histWidth,
                )
            }
                .frame(width = modular.width.toDouble() * scale + maxHistory * histWidth)
            infoBox()
        }
    }

    private fun ViewBuilder.stackedHistoryTeam(
        team: RemoteControlScoreboardTeam,
        otherTeam: RemoteControlScoreboardTeam,
        modularTeam: SettingsWidgetModularScoreboardTeam,
        histCount: Int,
        histWidth: Double,
    ): View = VStack(spacing = 0.0) {
        val currentPeriod = config.global.period.toIntOrNull() ?: 1
        val height = modular.rowHeight.toDouble() * scale
        HStack(spacing = 0.0) {
            teamName(team = modularTeam)
                .padding(EdgeSet.leading, 8 * scale)
                .padding(EdgeSet.trailing, 2 * scale)
                .frame(maxWidth = Double.POSITIVE_INFINITY, alignment = Alignment.leading)
            possession(show = team.possession)
            if (histCount > 0) {
                for (indexPlusOne in 1..histCount) {
                    val value = getHistoricScore(team = team, indexPlusOne = indexPlusOne) ?: ""
                    val otherValue = getHistoricScore(team = otherTeam, indexPlusOne = indexPlusOne) ?: ""
                    val valueInt = value.toIntOrNull() ?: -1
                    val otherValueInt = otherValue.toIntOrNull() ?: -1
                    val weight = if (indexPlusOne < currentPeriod && valueInt > otherValueInt && valueInt >= 0) {
                        if (modular.isBold) FontWeight.Black else FontWeight.Bold
                    } else {
                        if (modular.isBold) FontWeight.Bold else FontWeight.Normal
                    }
                    if (value.isNotEmpty()) {
                        stat(
                            value = value,
                            fontSize = fontSize() * 0.9,
                            width = histWidth,
                            gray = true,
                            weight = weight,
                        )
                    } else if (otherValue.isNotEmpty()) {
                        stat(
                            value = "0",
                            fontSize = fontSize() * 0.9,
                            width = histWidth,
                            gray = true,
                            weight = if (modular.isBold) FontWeight.Bold else FontWeight.Normal,
                        )
                    } else {
                        ColorView(Color.Transparent)
                            .frame(width = histWidth)
                    }
                }
            }
            primaryScore(team = team)
        }
            .frame(height = height)
            .background(modularTeam.backgroundColorColor)
        moreStats(
            team = team,
            height = height * 0.6,
            backgroundColor = modularTeam.backgroundColorColor,
        )
    }
        .foregroundStyle(modularTeam.textColorColor)

    private fun ViewBuilder.stacked() {
        title()
        HStack(alignment = VerticalAlignment.top, spacing = 0.0) {
            VStack(spacing = 0.0) {
                stackedTeam(team = config.team1, modularTeam = modular.home)
                stackedTeam(team = config.team2, modularTeam = modular.away)
            }
                .frame(width = modular.width.toDouble() * scale)
            infoBox()
        }
    }

    private fun ViewBuilder.sideBySide() {
        title()
        HStack(spacing = 0.0) {
            sideBySideTeam(
                team = config.team1,
                modularTeam = modular.home,
                mirrored = false,
            )
                .frame(width = modular.width.toDouble() * scale)
            val height = modular.rowHeight.toDouble() * scale
            val teamRowFullHeight = height + (if (modular.showMoreStats) height * 0.6 else 0.0)
            val globalStats = if (modular.showGlobalStatsBlock) {
                VStack(spacing = 0.0) {
                    val periodFull = config.periodFull()
                    if (periodFull.isNotEmpty()) {
                        Text(periodFull)
                            .font(Font.system(size = fontSize() * 0.6))
                            .minimumScaleFactor(0.1)
                    }
                    if (modular.showClock) {
                        Text(config.global.timer)
                            .font(Font.system(size = fontSize() * 0.9))
                            .monospacedDigit()
                            .minimumScaleFactor(0.1)
                    }
                }
                    .frame(width = fontSize() * 3.5, height = teamRowFullHeight)
            } else {
                Text("-")
                    .font(Font.system(size = fontSize()))
                    .frame(height = teamRowFullHeight)
            }
            globalStats
                .bold(modular.isBold)
                .background(Color.Black)
                .foregroundStyle(Color.White)
            sideBySideTeam(
                team = config.team2,
                modularTeam = modular.away,
                mirrored = true,
            )
                .frame(width = modular.width.toDouble() * scale)
        }
    }

    private fun ViewBuilder.stackedTeam(
        team: RemoteControlScoreboardTeam,
        modularTeam: SettingsWidgetModularScoreboardTeam,
    ): View = VStack(spacing = 0.0) {
        val height = modular.rowHeight.toDouble() * scale
        val width = fontSize() * 1.55
        HStack(spacing = 0.0) {
            if (modular.layout == SettingsWidgetScoreboardLayout.stacked) {
                stat(
                    value = team.secondaryScore,
                    label = team.secondaryScoreLabel,
                    fontSize = fontSize(),
                    width = width,
                    gray = true,
                )
            }
            teamName(team = modularTeam)
                .padding(EdgeSet.leading, 8 * scale)
                .frame(maxWidth = Double.POSITIVE_INFINITY, alignment = Alignment.leading)
            possession(show = team.possession)
            if (modular.layout == SettingsWidgetScoreboardLayout.stackedInline) {
                stat(
                    value = team.secondaryScore,
                    label = team.secondaryScoreLabel,
                    fontSize = fontSize(),
                    width = width,
                    gray = true,
                )
            }
            primaryScore(team = team)
        }
            .frame(height = height)
            .background(modularTeam.backgroundColorColor)
        moreStats(
            team = team,
            height = height * 0.6,
            backgroundColor = modularTeam.backgroundColorColor,
        )
    }
        .foregroundStyle(modularTeam.textColorColor)

    private fun ViewBuilder.sideBySideTeam(
        team: RemoteControlScoreboardTeam,
        modularTeam: SettingsWidgetModularScoreboardTeam,
        mirrored: Boolean,
    ): View {
        val height = modular.rowHeight.toDouble() * scale
        val width = fontSize() * 1.55
        return VStack(spacing = 0.0) {
            HStack(spacing = 0.0) {
                if (!mirrored) {
                    possession(show = team.possession)
                    teamName(team = modularTeam)
                        .padding(EdgeSet.horizontal, 4 * scale)
                        .frame(maxWidth = Double.POSITIVE_INFINITY, alignment = Alignment.trailing)
                    stat(
                        value = team.secondaryScore,
                        label = team.secondaryScoreLabel,
                        fontSize = fontSize(),
                        width = width,
                        gray = true,
                    )
                    primaryScore(team = team)
                } else {
                    primaryScore(team = team)
                    stat(
                        value = team.secondaryScore,
                        label = team.secondaryScoreLabel,
                        fontSize = fontSize(),
                        width = width,
                        gray = true,
                    )
                    teamName(team = modularTeam)
                        .padding(EdgeSet.horizontal, 4 * scale)
                        .frame(maxWidth = Double.POSITIVE_INFINITY, alignment = Alignment.leading)
                    possession(show = team.possession)
                }
            }
                .frame(height = height)
                .background(modularTeam.backgroundColorColor)
            moreStats(
                team = team,
                height = height * 0.6,
                backgroundColor = modularTeam.backgroundColorColor,
                alignRight = !mirrored,
            )
        }
            .foregroundStyle(modularTeam.textColorColor)
    }

    private fun ViewBuilder.stat(
        value: String,
        label: String? = null,
        fontSize: Double,
        width: Double,
        gray: Boolean,
        weight: FontWeight? = null,
    ) {
        if (value.isNotEmpty()) {
            val weight = weight ?: if (modular.isBold) FontWeight.Black else FontWeight.Bold
            ZStack {
                if (gray) {
                    ColorView(Color.Black.copy(alpha = 0.25f))
                }
                if (label != null && label.isNotEmpty()) {
                    VStack(spacing = -2 * scale) {
                        Text(label)
                            .font(Font.system(size = fontSize * 0.25))
                            .bold(modular.isBold)
                            .offset(x = 0.0, y = fontSize * 0.04)
                        Text(value)
                            .font(Font.system(size = fontSize * 0.75, weight = weight))
                    }
                } else {
                    Text(value)
                        .font(Font.system(size = fontSize, weight = weight))
                }
            }
                .frame(width = width)
        }
    }

    private fun ViewBuilder.primaryScore(team: RemoteControlScoreboardTeam) {
        stat(
            value = team.primaryScore,
            fontSize = fontSize(),
            width = fontSize() * 1.55,
            gray = false,
        )
    }

    private fun ViewBuilder.teamName(team: SettingsWidgetModularScoreboardTeam): View = Text(team.name)
        .font(Font.system(size = fontSize()))
        .bold(modular.isBold)
        .lineLimit(1)
        .minimumScaleFactor(0.1)

    private fun ViewBuilder.possession(show: Boolean) {
        if (show) {
            Image("VolleyballIndicator")
                .resizable()
                .scaledToFit()
                .padding(fontSize() * 0.1)
        }
    }

    private fun ViewBuilder.moreStats(
        team: RemoteControlScoreboardTeam,
        height: Double,
        backgroundColor: Color,
        alignRight: Boolean = false,
    ) {
        if (modular.showMoreStats) {
            val stats = listOf(
                Triple(0, team.stat1Label, team.stat1),
                Triple(1, team.stat2Label, team.stat2),
                Triple(2, team.stat3Label, team.stat3),
                Triple(3, team.stat4Label, team.stat4),
            )
                .filter { (_, _, value) -> value.isNotEmpty() && !value.startsWith("NO ") }
            ZStack {
                ColorView(Color.Black.copy(alpha = 0.25f))
                HStack(spacing = 8 * scale) {
                    if (alignRight) {
                        Spacer()
                    }
                    for ((_, label, value) in stats) {
                        HStack(spacing = 2.0) {
                            if (label.isNotEmpty()) {
                                Text("$label:")
                                    .opacity(0.8)
                            }
                            Text(value)
                                .monospacedDigit()
                        }
                            .font(Font.system(size = fontSize()))
                            .bold(modular.isBold)
                            .minimumScaleFactor(0.3)
                    }
                    if (!alignRight) {
                        Spacer()
                    }
                }
                    .padding(EdgeSet.horizontal, 6 * scale)
            }
                .frame(height = height)
                .background(backgroundColor)
        }
    }

    private fun ViewBuilder.infoBox() {
        if (modular.showGlobalStatsBlock) {
            val stats = config.infoBoxStats(showClock = modular.showClock)
            if (stats.isNotEmpty()) {
                val rowHeight = modular.rowHeight.toDouble() * scale
                val fullHeight = rowHeight + (if (modular.showMoreStats) rowHeight * 0.6 else 0.0)
                val height = fullHeight * 2
                VStack(spacing = 0.0) {
                    for (index in stats.indices) {
                        HCenter {
                            Text(stats[index])
                                .font(Font.system(size = fontSize()))
                                .bold(modular.isBold)
                                .monospacedDigit()
                                .minimumScaleFactor(0.1)
                                .lineLimit(1)
                        }
                    }
                }
                    .frame(height = height)
                    .background(Color.Black)
                    .foregroundStyle(Color.White)
            }
        }
    }

    private fun ViewBuilder.title() {
        if (modular.showTitle) {
            HCenter {
                Text(config.global.title)
                    .font(Font.system(size = fontSize() * 0.7))
                    .bold(modular.isBold)
                    .padding(EdgeSet.vertical, 1 * scale)
            }
                .background(Color.Black)
                .foregroundStyle(Color.White)
        }
    }

    fun ViewBuilder.body(): View = VStack(spacing = 0.0) {
        when (modular.layout) {
            SettingsWidgetScoreboardLayout.sideBySide -> sideBySide()
            SettingsWidgetScoreboardLayout.stackHistory -> stackedHistory()
            else -> stacked()
        }
    }
}

fun ViewBuilder.ScoreboardEffectModularView(
    modular: SettingsWidgetModularScoreboard,
    config: RemoteControlScoreboardMatchConfig,
    scale: Double,
): View = with(ModularScoreboardContent(modular = modular, config = config, scale = scale)) {
    body()
}

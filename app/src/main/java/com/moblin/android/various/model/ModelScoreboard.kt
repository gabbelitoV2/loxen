package com.moblin.android.various.model

import com.moblin.android.common.various.format
import com.moblin.android.remotecontrol.RemoteControlGolfPlayer
import com.moblin.android.remotecontrol.RemoteControlGolfScoreboard
import com.moblin.android.remotecontrol.RemoteControlScoreboardControl
import com.moblin.android.remotecontrol.RemoteControlScoreboardGlobalStats
import com.moblin.android.remotecontrol.RemoteControlScoreboardMatchConfig
import com.moblin.android.remotecontrol.RemoteControlScoreboardTeam
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetGenericScoreboard
import com.moblin.android.various.settings.SettingsWidgetGenericScoreboardClockDirection
import com.moblin.android.various.settings.SettingsWidgetGolfScoreboard
import com.moblin.android.various.settings.SettingsWidgetGolfScoreboardPlayer
import com.moblin.android.various.settings.SettingsWidgetPadelScoreboard
import com.moblin.android.various.settings.SettingsWidgetScoreboard
import com.moblin.android.various.settings.SettingsWidgetScoreboardLayout
import com.moblin.android.various.settings.SettingsWidgetScoreboardScore
import com.moblin.android.various.settings.SettingsWidgetScoreboardScoreIncrement
import com.moblin.android.various.settings.SettingsWidgetScoreboardSport
import com.moblin.android.various.settings.SettingsWidgetType
import com.moblin.android.various.utils.clockAsMinutesAndSeconds

private val homeBackgroundColor = "#1e40af"
private val awayBackgroundColor = "#dc2626"

private val basketballConfig: RemoteControlScoreboardMatchConfig = RemoteControlScoreboardMatchConfig(
    sportId = "basketball",
    layout = "sideBySide",
    team1 = RemoteControlScoreboardTeam(
        name = "Home",
        bgColor = homeBackgroundColor,
        possession = true,
        stat1 = "5",
        stat1Label = "TO",
        stat2 = "0",
        stat2Label = "FOUL",
        stat3 = "NO BONUS"
    ),
    team2 = RemoteControlScoreboardTeam(
        name = "Away",
        bgColor = awayBackgroundColor,
        possession = false,
        stat1 = "5",
        stat1Label = "TO",
        stat2 = "0",
        stat2Label = "FOUL",
        stat3 = "NO BONUS"
    ),
    global = RemoteControlScoreboardGlobalStats(
        title = "Varsity Basketball",
        timer = "10:00",
        timerDirection = "down",
        period = "1",
        periodLabel = "QTR",
        primaryScoreResetOnPeriod = false,
        changePossessionOnScore = false
    ),
    controls = mapOf(
        "primaryScore" to RemoteControlScoreboardControl(
            type = "counter",
            label = "Pt",
            periodReset = false
        ),
        "stat1" to RemoteControlScoreboardControl(
            type = "select",
            label = "TO",
            options = listOf("0", "1", "2", "3", "4", "5"),
            periodReset = true
        ),
        "stat2" to RemoteControlScoreboardControl(
            type = "cycle",
            label = "FOUL",
            options = listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10"),
            periodReset = true
        ),
        "stat3" to RemoteControlScoreboardControl(
            type = "cycle",
            label = "",
            options = listOf("NO BONUS", "BONUS", "DOUBLE"),
            periodReset = true
        ),
        "possession" to RemoteControlScoreboardControl(
            type = "toggleTeam",
            label = "POSS",
            periodReset = false
        ),
    )
)

private val genericConfig: RemoteControlScoreboardMatchConfig = RemoteControlScoreboardMatchConfig(
    sportId = "generic",
    layout = "stacked",
    team1 = RemoteControlScoreboardTeam(
        name = "Home",
        bgColor = homeBackgroundColor,
        possession = false
    ),
    team2 = RemoteControlScoreboardTeam(
        name = "Away",
        bgColor = awayBackgroundColor,
        possession = false
    ),
    global = RemoteControlScoreboardGlobalStats(
        title = "",
        timer = "",
        timerDirection = "down",
        period = "",
        periodLabel = "",
        primaryScoreResetOnPeriod = false,
        changePossessionOnScore = false,
        showTitle = false,
        showStats = false
    ),
    controls = mapOf(
        "primaryScore" to RemoteControlScoreboardControl(
            type = "counter",
            label = "Pt",
            periodReset = false
        ),
    )
)

private val genericSetsConfig: RemoteControlScoreboardMatchConfig = RemoteControlScoreboardMatchConfig(
    sportId = "generic sets",
    layout = "stacked",
    team1 = RemoteControlScoreboardTeam(
        name = "Home",
        bgColor = homeBackgroundColor,
        possession = false,
        secondaryScore = "0"
    ),
    team2 = RemoteControlScoreboardTeam(
        name = "Away",
        bgColor = awayBackgroundColor,
        possession = false,
        secondaryScore = "0"
    ),
    global = RemoteControlScoreboardGlobalStats(
        title = "GENERIC MATCH",
        timer = "0:00",
        timerDirection = "up",
        period = "1",
        periodLabel = "SET",
        primaryScoreResetOnPeriod = true,
        changePossessionOnScore = false,
        showTitle = false,
        showStats = false,
        showMoreStats = false,
        showClock = true
    ),
    controls = mapOf(
        "primaryScore" to RemoteControlScoreboardControl(
            type = "counter",
            label = "Pt",
            periodReset = true
        ),
        "secondaryScore" to RemoteControlScoreboardControl(
            type = "counter",
            label = "Set",
            periodReset = false
        ),
        "possession" to RemoteControlScoreboardControl(
            type = "toggleTeam",
            label = "POSS",
            periodReset = false
        ),
    )
)

private val hockeyConfig: RemoteControlScoreboardMatchConfig = RemoteControlScoreboardMatchConfig(
    sportId = "hockey",
    layout = "sideBySide",
    team1 = RemoteControlScoreboardTeam(
        name = "Home",
        bgColor = homeBackgroundColor,
        possession = false,
        secondaryScore = "0",
        secondaryScoreLabel = "SOG",
        stat1 = "NO PP",
        stat2 = "NO EN",
        stat3 = "NO DP"
    ),
    team2 = RemoteControlScoreboardTeam(
        name = "Away",
        bgColor = awayBackgroundColor,
        textColor = "#ffffff",
        possession = false,
        secondaryScore = "0",
        secondaryScoreLabel = "SOG",
        stat1 = "NO PP",
        stat2 = "NO EN",
        stat3 = "NO DP"
    ),
    global = RemoteControlScoreboardGlobalStats(
        title = "Varsity Hockey",
        timer = "15:00",
        timerDirection = "down",
        period = "1",
        periodLabel = "PER",
        primaryScoreResetOnPeriod = false,
        changePossessionOnScore = false
    ),
    controls = mapOf(
        "primaryScore" to RemoteControlScoreboardControl(
            type = "counter",
            label = "Goal",
            periodReset = false
        ),
        "secondaryScore" to RemoteControlScoreboardControl(
            type = "counter",
            label = "SOG",
            periodReset = false
        ),
        "stat1" to RemoteControlScoreboardControl(
            type = "cycle",
            label = "",
            options = listOf("NO PP", "PP"),
            periodReset = true
        ),
        "stat2" to RemoteControlScoreboardControl(
            type = "cycle",
            label = "",
            options = listOf("NO EN", "EN"),
            periodReset = true
        ),
        "stat3" to RemoteControlScoreboardControl(
            type = "cycle",
            label = "",
            options = listOf("NO DP", "DP"),
            periodReset = true
        ),
    )
)

private val footballConfig: RemoteControlScoreboardMatchConfig = RemoteControlScoreboardMatchConfig(
    sportId = "football",
    layout = "stacked",
    team1 = RemoteControlScoreboardTeam(
        name = "Home",
        bgColor = homeBackgroundColor,
        possession = false
    ),
    team2 = RemoteControlScoreboardTeam(
        name = "Away",
        bgColor = awayBackgroundColor,
        textColor = "#ffffff",
        possession = false
    ),
    global = RemoteControlScoreboardGlobalStats(
        title = "VARSITY FOOTBALL",
        timer = "30:00",
        timerDirection = "down",
        period = "1",
        periodLabel = "HALF",
        primaryScoreResetOnPeriod = false,
        changePossessionOnScore = false
    ),
    controls = mapOf(
        "primaryScore" to RemoteControlScoreboardControl(
            type = "counter",
            label = "Goal",
            periodReset = false
        ),
    )
)

private val tennisConfig: RemoteControlScoreboardMatchConfig = RemoteControlScoreboardMatchConfig(
    sportId = "tennis",
    layout = "stackHistory",
    team1 = RemoteControlScoreboardTeam(
        name = "Home",
        bgColor = homeBackgroundColor,
        possession = true
    ),
    team2 = RemoteControlScoreboardTeam(
        name = "Away",
        bgColor = awayBackgroundColor,
        textColor = "#ffffff",
        possession = false
    ),
    global = RemoteControlScoreboardGlobalStats(
        title = "TENNIS",
        timer = "0:00",
        timerDirection = "up",
        period = "1",
        periodLabel = "SET",
        primaryScoreResetOnPeriod = true,
        changePossessionOnScore = false,
        scoringMode = "tennis",
        showStats = false
    ),
    controls = mapOf(
        "primaryScore" to RemoteControlScoreboardControl(
            type = "counter",
            label = "Pt",
            periodReset = false
        ),
        "currentSetScore" to RemoteControlScoreboardControl(
            type = "counter",
            label = "Game",
            periodReset = true
        ),
        "possession" to RemoteControlScoreboardControl(
            type = "toggleTeam",
            label = "SERVE",
            periodReset = false
        ),
    )
)

private val volleyballConfig: RemoteControlScoreboardMatchConfig = RemoteControlScoreboardMatchConfig(
    sportId = "volleyball",
    layout = "stacked",
    team1 = RemoteControlScoreboardTeam(
        name = "Home",
        bgColor = homeBackgroundColor,
        possession = true,
        secondaryScore = "0",
        stat1 = "0",
        stat1Label = "TO"
    ),
    team2 = RemoteControlScoreboardTeam(
        name = "Away",
        bgColor = awayBackgroundColor,
        possession = false,
        secondaryScore = "0",
        stat1 = "0",
        stat1Label = "TO"
    ),
    global = RemoteControlScoreboardGlobalStats(
        title = "Varsity Volleyball",
        timer = "0:00",
        timerDirection = "up",
        period = "1",
        periodLabel = "SET",
        primaryScoreResetOnPeriod = true,
        changePossessionOnScore = true
    ),
    controls = mapOf(
        "primaryScore" to RemoteControlScoreboardControl(
            type = "counter",
            label = "Pt",
            periodReset = true
        ),
        "secondaryScore" to RemoteControlScoreboardControl(
            type = "counter",
            label = "Set",
            periodReset = false
        ),
        "stat1" to RemoteControlScoreboardControl(
            type = "select",
            label = "TO",
            options = listOf("0", "1", "2", "3"),
            periodReset = true
        ),
        "possession" to RemoteControlScoreboardControl(
            type = "toggleTeam",
            label = "SERVE",
            periodReset = false
        ),
    )
)

private val configs: Map<String, RemoteControlScoreboardMatchConfig> = mapOf(
    "basketball" to basketballConfig,
    "generic" to genericConfig,
    "generic sets" to genericSetsConfig,
    "hockey" to hockeyConfig,
    "football" to footballConfig,
    "tennis" to tennisConfig,
    "volleyball" to volleyballConfig,
)

fun Model.handleUpdatePadelScoreboard(action: Any) {
    Unit
}

fun Model.handleUpdateGenericScoreboard(action: Any) {
    Unit
}

fun Model.getEnabledScoreboardWidgetsInSelectedScene(): List<SettingsWidget> {
    val scene = getSelectedScene()
    return if (scene != null) {
        getSceneWidgets(scene = scene, onlyEnabled = true)
            .filter { it.widget.type == SettingsWidgetType.scoreboard }
            .map { it.widget }
    } else {
        emptyList()
    }
}

fun Model.updateScoreboardEffects() {
    for (widget in getEnabledScoreboardWidgetsInSelectedScene()) {
        val effect = getScoreboardEffect(id = widget.id) ?: continue
        val scoreboard = widget.scoreboard
        when (scoreboard.sport) {
            SettingsWidgetScoreboardSport.padel -> {}
            SettingsWidgetScoreboardSport.golf -> {}
            SettingsWidgetScoreboardSport.golfFullScorecard -> {}
            SettingsWidgetScoreboardSport.generic -> {
                if (scoreboard.generic.clock.isStopped) {
                    continue
                }
                scoreboard.generic.clock.tick()
                effect.update(
                    scoreboard = scoreboard,
                    config = getModularScoreboardConfig(scoreboard = scoreboard),
                    players = database.scoreboardPlayers
                )
                sendUpdateGenericScoreboardToWatch(id = widget.id, generic = scoreboard.generic)
            }
            else -> {
                if (scoreboard.modular.clock.isStopped) {
                    continue
                }
                widget.scoreboard.modular.clock.tick()
                effect.update(
                    scoreboard = scoreboard,
                    config = getModularScoreboardConfig(scoreboard = scoreboard),
                    players = database.scoreboardPlayers
                )
                remoteControlScoreboardUpdate(scoreboard = scoreboard)
            }
        }
    }
}

fun Model.getModularScoreboardConfig(scoreboard: SettingsWidgetScoreboard?): RemoteControlScoreboardMatchConfig {
    val sportId = when (scoreboard?.sport) {
        SettingsWidgetScoreboardSport.basketball -> "basketball"
        SettingsWidgetScoreboardSport.generic2 -> "generic"
        SettingsWidgetScoreboardSport.genericSets -> "generic sets"
        SettingsWidgetScoreboardSport.hockey -> "hockey"
        SettingsWidgetScoreboardSport.football -> "football"
        SettingsWidgetScoreboardSport.tennis -> "tennis"
        SettingsWidgetScoreboardSport.volleyball -> "volleyball"
        else -> "generic"
    }
    var config: RemoteControlScoreboardMatchConfig
    val current = scoreboard?.modular?.config
    if (current != null && current.sportId == sportId) {
        config = current
    } else {
        val loaded = configs[sportId]
        if (loaded != null) {
            config = loaded
            if (scoreboard != null) {
                scoreboard.modular.config = loaded
            }
        } else {
            return RemoteControlScoreboardMatchConfig(
                sportId = "error",
                layout = "stacked",
                team1 = RemoteControlScoreboardTeam(
                    name = "FILE MISSING",
                    bgColor = "#000000",
                    possession = false,
                    primaryScore = "0",
                    secondaryScore = "0"
                ),
                team2 = RemoteControlScoreboardTeam(
                    name = "ERROR",
                    bgColor = "#000000",
                    textColor = "#ffffff",
                    possession = false,
                    primaryScore = "0",
                    secondaryScore = "0"
                ),
                global = RemoteControlScoreboardGlobalStats(
                    title = "ERROR",
                    timer = "0:00",
                    timerDirection = "up",
                    period = "1",
                    periodLabel = "SET",
                    primaryScoreResetOnPeriod = false,
                    changePossessionOnScore = false
                ),
                controls = emptyMap()
            )
        }
    }
    val modular = scoreboard?.modular
    if (modular != null) {
        when (modular.layout) {
            SettingsWidgetScoreboardLayout.sideBySide -> config.layout = "sideBySide"
            SettingsWidgetScoreboardLayout.stackHistory -> config.layout = "stackHistory"
            SettingsWidgetScoreboardLayout.stackedInline -> config.layout = "stackedInline"
            else -> config.layout = "stacked"
        }
        config.global.showTitle = modular.showTitle
        config.global.showStats = modular.showGlobalStatsBlock
        config.global.showMoreStats = modular.showMoreStats
        config.global.showClock = modular.showClock
        config.global.title = modular.title
        config.global.timer = modular.clock.format()
        when (modular.clock.direction) {
            SettingsWidgetGenericScoreboardClockDirection.up -> config.global.timerDirection = "up"
            SettingsWidgetGenericScoreboardClockDirection.down -> config.global.timerDirection = "down"
        }
        config.global.duration = modular.clock.maximum
        if (modular.period.isNotEmpty()) {
            config.global.period = modular.period
        }
        config.global.infoBoxText = modular.infoBoxText
        config.team1.name = modular.home.name
        config.team2.name = modular.away.name
        config.team1.textColor = modular.home.textColor.toHex()
        config.team2.textColor = modular.away.textColor.toHex()
        config.team1.bgColor = modular.home.backgroundColor.toHex()
        config.team2.bgColor = modular.away.backgroundColor.toHex()
        if (config.global.scoringMode != "tennis") {
            config.team1.primaryScore = modular.score.home.toString()
            config.team2.primaryScore = modular.score.away.toString()
        }
    }
    return config
}

fun Model.getScoreboardSports(): List<String> {
    val sports = configs.keys
    val topPriority = listOf("generic", "generic sets")
    val rest = sports.filter { !topPriority.contains(it) }.sorted()
    val finalSports = topPriority.filter { sports.contains(it) } + rest
    return if (finalSports.isEmpty()) listOf("volleyball", "basketball") else finalSports
}

private fun Model.updateScoreboardEffect(widget: SettingsWidget) {
    getScoreboardEffect(id = widget.id)
        ?.update(
            scoreboard = widget.scoreboard,
            config = getModularScoreboardConfig(scoreboard = widget.scoreboard),
            players = database.scoreboardPlayers
        )
}

private fun Model.updateGolfScoreboardEffect(widget: SettingsWidget) {
    getScoreboardEffect(id = widget.id)
        ?.update(
            scoreboard = widget.scoreboard,
            config = getModularScoreboardConfig(scoreboard = widget.scoreboard),
            players = database.scoreboardPlayers
        )
}

private fun Model.updateAllGolfScoreboardEffects(golf: SettingsWidgetGolfScoreboard) {
    for (widget in getEnabledScoreboardWidgetsInSelectedScene()) {
        val sport = widget.scoreboard.sport
        if (sport != SettingsWidgetScoreboardSport.golf && sport != SettingsWidgetScoreboardSport.golfFullScorecard) {
            continue
        }
        widget.scoreboard.golf = golf
        updateGolfScoreboardEffect(widget = widget)
    }
}

fun Model.getGolfScoreboardForRemoteControl(): RemoteControlGolfScoreboard {
    val golf = getEnabledScoreboardWidgetsInSelectedScene()
        .firstOrNull { it.scoreboard.sport == SettingsWidgetScoreboardSport.golf }
        ?.scoreboard
        ?.golf
        ?: SettingsWidgetGolfScoreboard()
    val players = golf.players.map {
        RemoteControlGolfPlayer(name = it.name, scores = it.scores, color = it.color)
    }
    return RemoteControlGolfScoreboard(
        title = golf.title,
        numberOfHoles = golf.numberOfHoles,
        pars = golf.pars,
        currentHole = golf.currentHole,
        players = players,
        playerColors = golf.playerColors
    )
}

fun Model.handleExternalGolfScoreboardUpdate(remoteScorecard: RemoteControlGolfScoreboard) {
    val widget = getEnabledScoreboardWidgetsInSelectedScene()
        .firstOrNull { it.scoreboard.sport == SettingsWidgetScoreboardSport.golf }
        ?: return
    val golf = widget.scoreboard.golf
    golf.title = remoteScorecard.title
    golf.numberOfHoles = remoteScorecard.numberOfHoles
    golf.currentHole = remoteScorecard.currentHole
    golf.updatePars(remoteScorecard.pars)
    golf.playerColors = remoteScorecard.playerColors
    for ((index, remotePlayer) in remoteScorecard.players.withIndex()) {
        if (index < golf.players.size) {
            golf.players[index].name = remotePlayer.name
            golf.players[index].scores = remotePlayer.scores
            golf.players[index].color = remotePlayer.color
        } else {
            val player = SettingsWidgetGolfScoreboardPlayer(name = remotePlayer.name)
            player.scores = remotePlayer.scores
            player.color = remotePlayer.color
            golf.players = golf.players + player
        }
    }
    while (golf.players.size > remoteScorecard.players.size) {
        golf.players = golf.players.dropLast(1)
    }
    updateAllGolfScoreboardEffects(golf = golf)
    remoteControlWeb?.sendGolfScoreboardUpdate(data = remoteScorecard)
}

fun Model.handleScoreboardToggleClock() {
    val widget = getEnabledScoreboardWidgetsInSelectedScene().firstOrNull() ?: return
    widget.scoreboard.modular.clock.isStopped = !widget.scoreboard.modular.clock.isStopped
    updateScoreboardEffect(widget = widget)
    remoteControlScoreboardUpdate(scoreboard = widget.scoreboard)
}

fun Model.handleScoreboardSetDuration(minutes: Int) {
    val widget = getEnabledScoreboardWidgetsInSelectedScene().firstOrNull() ?: return
    val clock = widget.scoreboard.modular.clock
    clock.maximum = minutes
    clock.reset()
    clock.isStopped = true
    updateScoreboardEffect(widget = widget)
    remoteControlScoreboardUpdate(scoreboard = widget.scoreboard)
}

fun Model.handleScoreboardSetClockManual(time: String) {
    val widget = getEnabledScoreboardWidgetsInSelectedScene().firstOrNull() ?: return
    val (minutes, seconds) = clockAsMinutesAndSeconds(clock = time)
    val clock = widget.scoreboard.modular.clock
    clock.minutes = minutes
    clock.seconds = seconds
    clock.isStopped = true
    updateScoreboardEffect(widget = widget)
    remoteControlScoreboardUpdate(scoreboard = widget.scoreboard)
}

fun Model.handleExternalScoreboardUpdate(config: RemoteControlScoreboardMatchConfig) {
    val widget = getEnabledScoreboardWidgetsInSelectedScene().firstOrNull() ?: return
    val scoreboard = widget.scoreboard
    val modular = scoreboard.modular
    modular.config = config
    scoreboard.setModularSport(sportId = config.sportId)
    modular.setLayout(name = config.layout)
    config.global.showTitle?.let { modular.showTitle = it }
    config.global.showStats?.let { modular.showGlobalStatsBlock = it }
    config.global.showMoreStats?.let { modular.showMoreStats = it }
    config.global.showClock?.let { modular.showClock = it }
    modular.home.name = config.team1.name
    modular.away.name = config.team2.name
    modular.title = config.global.title
    modular.period = config.global.period
    modular.infoBoxText = config.global.infoBoxText
    config.team1.primaryScore.toIntOrNull()?.let { modular.score.home = it }
    config.team2.primaryScore.toIntOrNull()?.let { modular.score.away = it }
    modular.home.setHexColors(config.team1.textColor, config.team1.bgColor)
    modular.away.setHexColors(config.team2.textColor, config.team2.bgColor)
    val (minutes, seconds) = config.global.minutesAndSeconds()
    modular.clock.minutes = minutes
    modular.clock.seconds = seconds
    modular.clock.direction = if (config.global.timerDirection == "down") {
        SettingsWidgetGenericScoreboardClockDirection.down
    } else {
        SettingsWidgetGenericScoreboardClockDirection.up
    }
    updateScoreboardEffect(widget = widget)
    remoteControlScoreboardUpdate(scoreboard = scoreboard)
}

fun Model.handleSportSwitch(sportId: String) {
    val widget = getEnabledScoreboardWidgetsInSelectedScene().firstOrNull() ?: return
    val scoreboard = widget.scoreboard
    scoreboard.setModularSport(sportId = sportId)
    val config = configs[sportId]
    if (config != null) {
        val modular = scoreboard.modular
        modular.config = config
        modular.setLayout(name = config.layout)
        modular.score.home = config.team1.primaryScore.toIntOrNull() ?: 0
        modular.score.away = config.team2.primaryScore.toIntOrNull() ?: 0
        modular.period = config.global.period
        val (minutes, seconds) = config.global.minutesAndSeconds()
        modular.clock.minutes = minutes
        modular.clock.seconds = seconds
        modular.clock.maximum = minutes + (if (seconds > 0) 1 else 0)
        modular.clock.direction = if (config.global.timerDirection == "down") {
            SettingsWidgetGenericScoreboardClockDirection.down
        } else {
            SettingsWidgetGenericScoreboardClockDirection.up
        }
        modular.clock.isStopped = true
        modular.home.setHexColors(config.team1.textColor, config.team1.bgColor)
        modular.away.setHexColors(config.team2.textColor, config.team2.bgColor)
        updateScoreboardEffect(widget = widget)
        remoteControlScoreboardUpdate(scoreboard = scoreboard)
    }
}

private fun Model.handleUpdatePadelScoreboardReset(scoreboard: SettingsWidgetPadelScoreboard) {
    scoreboard.score = mutableListOf(SettingsWidgetScoreboardScore())
    scoreboard.scoreChanges = mutableListOf()
}

private fun Model.handleUpdatePadelScoreboardUndo(scoreboard: SettingsWidgetPadelScoreboard) {
    val team = scoreboard.scoreChanges.lastOrNull() ?: return
    scoreboard.scoreChanges = scoreboard.scoreChanges.dropLast(1)
    val score = scoreboard.score.lastOrNull() ?: return
    if (score.home == 0 && score.away == 0 && scoreboard.score.size > 1) {
        scoreboard.score = scoreboard.score.dropLast(1)
    }
    val index = scoreboard.score.size - 1
    when (team) {
        SettingsWidgetScoreboardScoreIncrement.home -> if (scoreboard.score[index].home > 0) {
            scoreboard.score[index].home -= 1
        }
        SettingsWidgetScoreboardScoreIncrement.away -> if (scoreboard.score[index].away > 0) {
            scoreboard.score[index].away -= 1
        }
    }
}

private fun Model.handleUpdatePadelScoreboardIncrementHome(scoreboard: SettingsWidgetPadelScoreboard) {
    if (!isCurrentSetCompleted(scoreboard = scoreboard)) {
        if (isMatchCompleted(scoreboard = scoreboard)) {
            return
        }
        scoreboard.score[scoreboard.score.size - 1].home += 1
        scoreboard.scoreChanges = scoreboard.scoreChanges + SettingsWidgetScoreboardScoreIncrement.home
    } else {
        padelScoreboardUpdateSetCompleted(scoreboard = scoreboard)
    }
}

private fun Model.handleUpdatePadelScoreboardIncrementAway(scoreboard: SettingsWidgetPadelScoreboard) {
    if (!isCurrentSetCompleted(scoreboard = scoreboard)) {
        if (isMatchCompleted(scoreboard = scoreboard)) {
            return
        }
        scoreboard.score[scoreboard.score.size - 1].away += 1
        scoreboard.scoreChanges = scoreboard.scoreChanges + SettingsWidgetScoreboardScoreIncrement.away
    } else {
        padelScoreboardUpdateSetCompleted(scoreboard = scoreboard)
    }
}

private fun Model.handleUpdatePadelScoreboardChangePlayers(scoreboard: SettingsWidgetPadelScoreboard,
                                                           players: Any)
{
    Unit
}

private fun Model.handleUpdateGenericScoreboardReset(scoreboard: SettingsWidgetGenericScoreboard) {
    scoreboard.score.home = 0
    scoreboard.score.away = 0
    scoreboard.scoreChanges = mutableListOf()
}

private fun Model.handleUpdateGenericScoreboardUndo(scoreboard: SettingsWidgetGenericScoreboard) {
    val team = scoreboard.scoreChanges.lastOrNull() ?: return
    scoreboard.scoreChanges = scoreboard.scoreChanges.dropLast(1)
    when (team) {
        SettingsWidgetScoreboardScoreIncrement.home -> if (scoreboard.score.home > 0) {
            scoreboard.score.home -= 1
        }
        SettingsWidgetScoreboardScoreIncrement.away -> if (scoreboard.score.away > 0) {
            scoreboard.score.away -= 1
        }
    }
}

private fun Model.handleUpdateGenericScoreboardIncrementHome(scoreboard: SettingsWidgetGenericScoreboard) {
    scoreboard.score.home += 1
    scoreboard.scoreChanges = scoreboard.scoreChanges + SettingsWidgetScoreboardScoreIncrement.home
}

private fun Model.handleUpdateGenericScoreboardIncrementAway(scoreboard: SettingsWidgetGenericScoreboard) {
    scoreboard.score.away += 1
    scoreboard.scoreChanges = scoreboard.scoreChanges + SettingsWidgetScoreboardScoreIncrement.away
}

private fun Model.handleUpdateGenericScoreboardSetTitle(scoreboard: SettingsWidgetGenericScoreboard,
                                                        title: String)
{
    scoreboard.title = title
}

private fun Model.handleUpdateGenericScoreboardSetClock(scoreboard: SettingsWidgetGenericScoreboard,
                                                        minutes: Int,
                                                        seconds: Int)
{
    scoreboard.clock.minutes = minutes.coerceIn(0, scoreboard.clock.maximum)
    if (scoreboard.clock.minutes == scoreboard.clock.maximum) {
        scoreboard.clock.seconds = 0
    } else {
        scoreboard.clock.seconds = seconds.coerceIn(0, 59)
    }
}

private fun Model.handleUpdateGenericScoreboardSetClockState(scoreboard: SettingsWidgetGenericScoreboard,
                                                             stopped: Boolean)
{
    scoreboard.clock.isStopped = stopped
}

private fun Model.padelScoreboardUpdateSetCompleted(scoreboard: SettingsWidgetPadelScoreboard) {
    val score = scoreboard.score.lastOrNull() ?: return
    if (!isSetCompleted(score = score)) {
        return
    }
    if (isMatchCompleted(scoreboard = scoreboard)) {
        return
    }
    scoreboard.score = scoreboard.score + SettingsWidgetScoreboardScore()
}

private fun Model.isCurrentSetCompleted(scoreboard: SettingsWidgetPadelScoreboard): Boolean {
    val score = scoreboard.score.lastOrNull() ?: return false
    return isSetCompleted(score = score)
}

private fun Model.isSetCompleted(score: SettingsWidgetScoreboardScore): Boolean {
    val maxScore = maxOf(score.home, score.away)
    val minScore = minOf(score.home, score.away)
    if (maxScore == 6 && minScore <= 4) {
        return true
    }
    if (maxScore == 7) {
        return true
    }
    return false
}

private fun Model.isMatchCompleted(scoreboard: SettingsWidgetPadelScoreboard): Boolean {
    if (scoreboard.score.size < 5) {
        return false
    }
    val score = scoreboard.score.lastOrNull() ?: return false
    return isSetCompleted(score = score)
}

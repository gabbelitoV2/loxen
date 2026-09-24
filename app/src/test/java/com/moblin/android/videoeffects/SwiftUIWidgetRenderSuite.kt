package com.moblin.android.videoeffects

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.moblin.android.AppDelegate
import com.moblin.android.MainLooperDrainRule
import com.moblin.android.platform.coregraphics.CGSize
import androidx.compose.ui.text.font.createFontFamilyResolver
import com.moblin.android.platform.swiftui.layout.AndroidTextMeasurer
import com.moblin.android.platform.swiftui.layout.ResolvedFont
import com.moblin.android.platform.swiftui.layout.ViewBuilder
import com.moblin.android.platform.swiftui.layout.renderSwiftUIView
import com.moblin.android.remotecontrol.RemoteControlScoreboardGlobalStats
import com.moblin.android.remotecontrol.RemoteControlScoreboardMatchConfig
import com.moblin.android.remotecontrol.RemoteControlScoreboardTeam
import com.moblin.android.various.settings.SettingsBingoCardSquare
import com.moblin.android.various.settings.SettingsSceneWidget
import com.moblin.android.various.settings.SettingsWidgetBingoCard
import com.moblin.android.various.settings.SettingsWidgetGenericScoreboard
import com.moblin.android.various.settings.SettingsWidgetGolfScoreboard
import com.moblin.android.various.settings.SettingsWidgetLayout
import com.moblin.android.various.settings.SettingsWidgetModularScoreboard
import com.moblin.android.various.settings.SettingsWidgetPadelScoreboard
import com.moblin.android.various.settings.SettingsWidgetPomodoroTimer
import com.moblin.android.various.settings.SettingsWidgetScoreboardLayout
import com.moblin.android.various.settings.SettingsWidgetScoreboardPlayer
import com.moblin.android.videoeffects.scoreboard.ScoreboardEffectGenericView
import com.moblin.android.videoeffects.scoreboard.ScoreboardEffectGolfFullScorecardView
import com.moblin.android.videoeffects.scoreboard.ScoreboardEffectGolfView
import com.moblin.android.videoeffects.scoreboard.ScoreboardEffectModularView
import com.moblin.android.videoeffects.scoreboard.ScoreboardEffectPadelView
import com.moblin.android.view.utils.FontDesign
import java.io.File
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

private val primary = Color(0xFF223344)
private val secondary = Color(0xFF556677)

private fun render(name: String, content: ViewBuilder.() -> Unit): Bitmap {
    val bitmap = renderSwiftUIView(AppDelegate.context, 1f, content)
    assertNotNull(bitmap, name)
    val directory = File("build/swiftui-renders")
    directory.mkdirs()
    File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    return bitmap
}

private fun assertColor(expected: Color, actual: Int, message: String) {
    val expectedArgb = expected.toArgb()
    val components = listOf(24, 16, 8, 0)
    for (shift in components) {
        val a = (expectedArgb shr shift) and 0xFF
        val b = (actual shr shift) and 0xFF
        assertTrue(abs(a - b) <= 2, "$message: expected ${Integer.toHexString(expectedArgb)}, got ${Integer.toHexString(actual)}")
    }
}

private fun modularConfig(): RemoteControlScoreboardMatchConfig = RemoteControlScoreboardMatchConfig(
    sportId = "volleyball",
    layout = "stacked",
    team1 = RemoteControlScoreboardTeam(
        name = "HOME",
        bgColor = "#0b10ac",
        possession = true,
        primaryScore = "12",
        secondaryScore = "1",
        secondaryScoreLabel = "SETS",
        secondaryScore1 = "25",
        stat1 = "3",
        stat1Label = "TO",
    ),
    team2 = RemoteControlScoreboardTeam(
        name = "AWAY TEAM WITH A LONG NAME",
        bgColor = "#dc2626",
        possession = false,
        primaryScore = "8",
        secondaryScore = "0",
        secondaryScoreLabel = "SETS",
        secondaryScore1 = "20",
        stat1 = "1",
        stat1Label = "TO",
    ),
    global = RemoteControlScoreboardGlobalStats(
        title = "FINAL",
        timer = "12:34",
        timerDirection = "down",
        period = "2",
        periodLabel = "SET",
        primaryScoreResetOnPeriod = false,
        changePossessionOnScore = false,
    ),
    controls = emptyMap(),
)

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SwiftUIWidgetRenderSuite {
    @get:Rule
    val mainLooperDrain = MainLooperDrainRule()

    @Test
    fun genericScoreboardBarsSpanTheWholeWidth() {
        val generic = SettingsWidgetGenericScoreboard(home = "Home", away = "Away", title = "Final")
        generic.score.home = 2
        generic.score.away = 11
        val bitmap = render("generic") {
            ScoreboardEffectGenericView(
                textColor = Color.White,
                primaryBackgroundColor = primary,
                secondaryBackgroundColor = secondary,
                generic = generic,
                scale = 1.35,
            )
        }
        assertTrue(bitmap.width > bitmap.height)
        assertEquals(0, bitmap.getPixel(0, 0) ushr 24)
        assertColor(secondary, bitmap.getPixel(bitmap.width - 8, bitmap.height - 3), "powered by bar")
        assertColor(secondary, bitmap.getPixel(bitmap.width - 8, 3), "title bar")
        assertColor(primary, bitmap.getPixel(bitmap.width - 3, bitmap.height / 2), "score area")
    }

    @Test
    fun padelScoreboard() {
        val playerOne = SettingsWidgetScoreboardPlayer(name = "Anna")
        val playerTwo = SettingsWidgetScoreboardPlayer(name = "Bo")
        val padel = SettingsWidgetPadelScoreboard(homePlayer1 = playerOne.id, awayPlayer1 = playerTwo.id)
        render("padel") {
            ScoreboardEffectPadelView(
                textColor = Color.White,
                primaryBackgroundColor = primary,
                secondaryBackgroundColor = secondary,
                padel = padel,
                players = listOf(playerOne, playerTwo),
                scale = 1.35,
            )
        }
    }

    @Test
    fun golfScoreboards() {
        val golf = SettingsWidgetGolfScoreboard(currentHole = 3)
        render("golf") {
            ScoreboardEffectGolfView(
                textColor = Color.White,
                primaryBackgroundColor = primary,
                secondaryBackgroundColor = secondary,
                golf = golf,
                scale = 1.35,
            )
        }
        val scorecard = render("golf-full-scorecard") {
            ScoreboardEffectGolfFullScorecardView(
                textColor = Color.White,
                primaryBackgroundColor = primary,
                secondaryBackgroundColor = secondary,
                golf = golf,
                scale = 1.35,
            )
        }
        val expectedWidth = (150.0 + 18 * 28.0 + 100.0) * 1.35
        assertTrue(abs(scorecard.width - expectedWidth) <= 1.0, "scorecard width ${scorecard.width}")
    }

    @Test
    fun modularScoreboards() {
        for (layout in SettingsWidgetScoreboardLayout.entries) {
            for (options in 0 until 2) {
                val modular = SettingsWidgetModularScoreboard(
                    layout = layout,
                    showTitle = options == 1,
                    showMoreStats = options == 1,
                    showGlobalStatsBlock = options == 1,
                )
                val bitmap = render("modular-$layout-$options") {
                    ScoreboardEffectModularView(modular = modular, config = modularConfig(), scale = 1.35)
                }
                assertTrue(bitmap.width >= (350 * 1.35).toInt(), "modular $layout width ${bitmap.width}")
            }
        }
    }

    @Test
    fun bingoCard() {
        val settings = SettingsWidgetBingoCard(
            squares = (1..9).map { SettingsBingoCardSquare(text = "Square number $it", checked = it % 4 == 0) },
        )
        val sceneWidget = SettingsSceneWidget(layout = SettingsWidgetLayout(size = 30.0))
        val bitmap = render("bingo") {
            BingoView(settings = settings, sceneWidget = sceneWidget, canvasSize = CGSize(1920.0, 1080.0))
        }
        assertEquals(324 + 2, bitmap.width)
        assertEquals(324 + 2, bitmap.height)
    }

    @Test
    fun pomodoroTimer() {
        val settings = SettingsWidgetPomodoroTimer()
        settings.secondsRemaining = 12 * 60 + 5
        val sceneWidget = SettingsSceneWidget(layout = SettingsWidgetLayout(size = 20.0))
        render("pomodoro") {
            PomodoroTimerView(settings = settings, sceneWidget = sceneWidget, canvasSize = CGSize(1920.0, 1080.0))
        }
    }

    @Test
    fun truncatedTextReportsTheWidthOfTheVisibleText() {
        val measurer = AndroidTextMeasurer(1f, createFontFamilyResolver(AppDelegate.context))
        val font = ResolvedFont(size = 17.0, weight = 400, design = FontDesign.Default, monospacedDigit = false)
        val text = "A VERY LONG PLAYER NAME"
        val full = measurer.measure(text, font, 1.0, null, 1, true)
        val truncated = measurer.measure(text, font, 1.0, 101.5, 1, true)
        assertTrue(truncated.truncated, "not truncated")
        assertTrue(truncated.width > 0.0 && truncated.width <= 101.0, "truncated width ${truncated.width}")
        assertTrue(truncated.width < full.width, "full ${full.width}, truncated ${truncated.width}")
        val empty = renderSwiftUIView(AppDelegate.context, 1f) {
            VStack(spacing = 0.0) {
                Text("")
                Text("x")
            }
        }
        assertNotNull(empty)
        assertEquals(1 + 21, empty.height)
    }

    @Test
    fun pollAndWheel() {
        val state = PollState(CGSize(1920.0, 1080.0))
        state.text = "Yes 60%, No 40%"
        render("poll") {
            PollView(state = state)
        }
        val options = listOf(
            WheelOfLuckEffectOption(id = 0, weight = 1, text = "One", textAngle = 45.0 - 90),
            WheelOfLuckEffectOption(id = 1, weight = 2, text = "Two is longer", textAngle = 90.0 + 90 - 90),
            WheelOfLuckEffectOption(id = 2, weight = 1, text = "Three", textAngle = 315.0 - 90),
        )
        val wheel = render("wheel") {
            WheelView(size = 400.0, options = options)
        }
        assertEquals(400, wheel.width)
        render("arrow") {
            ArrowView(size = 400.0)
        }
    }
}

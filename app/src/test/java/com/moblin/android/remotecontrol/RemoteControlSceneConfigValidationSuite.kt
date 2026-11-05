package com.moblin.android.remotecontrol

import com.moblin.android.common.various.RgbColor
import com.moblin.android.platform.codable.codableJson
import com.moblin.android.platform.core.uuidString
import com.moblin.android.various.model.CameraId
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsAlignment
import com.moblin.android.various.settings.SettingsAlertsMediaGalleryItem
import com.moblin.android.various.settings.SettingsCameraId
import com.moblin.android.various.settings.SettingsColorLut
import com.moblin.android.various.settings.SettingsColorLutType
import com.moblin.android.various.settings.SettingsMicsMic
import com.moblin.android.various.settings.SettingsControllerFunction
import com.moblin.android.various.settings.SettingsScene
import com.moblin.android.various.settings.SettingsSceneWidget
import com.moblin.android.various.settings.SettingsVideoEffect
import com.moblin.android.various.settings.SettingsVideoEffectMaskEffectPoint
import com.moblin.android.various.settings.SettingsVideoEffectType
import com.moblin.android.various.settings.SettingsVideoSource
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetAlertsChatBotCommand
import com.moblin.android.various.settings.SettingsWidgetCrop
import com.moblin.android.various.settings.SettingsWidgetGolfScoreboardPlayer
import com.moblin.android.various.settings.SettingsWidgetScoreboardPlayer
import com.moblin.android.various.settings.SettingsWidgetScoreboardSport
import com.moblin.android.various.settings.SettingsWidgetSlideshowSlide
import com.moblin.android.various.settings.SettingsWidgetTextTimer
import com.moblin.android.various.settings.SettingsWidgetType
import com.moblin.android.various.settings.SettingsWidgetWheelOfLuckOption
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

private inline fun <reified T> roundTrip(value: T): T =
    codableJson.decodeFromString(codableJson.encodeToString(value))

private inline fun <reified T> modified(value: T, edit: (T) -> Unit = {}): T {
    val newValue = roundTrip(value)
    edit(newValue)
    return newValue
}

private fun cameraId(videoSource: SettingsVideoSource): CameraId = videoSource.toCameraId().toString()

private fun emptyWidget(type: SettingsWidgetType): SettingsWidget {
    val widget = SettingsWidget(name = "New")
    widget.type = type
    return widget
}

private fun emptyScene(scene: SettingsScene): SettingsScene {
    val newScene = SettingsScene(name = scene.name)
    newScene.id = scene.id
    return newScene
}

private fun replacing(scenes: List<SettingsScene>, scene: SettingsScene): List<SettingsScene> =
    scenes.map { if (it.id == scene.id) scene else it }

private fun sceneWidget(
    widgetId: UUID,
    x: Double = 0.0,
    y: Double = 0.0,
    size: Double = 10.0,
): SettingsSceneWidget {
    val sceneWidget = SettingsSceneWidget(widgetId = widgetId)
    sceneWidget.layout = sceneWidget.layout.copy(x = x, y = y, size = size)
    return sceneWidget
}

private fun timer(delta: Int): SettingsWidgetTextTimer {
    val timer = SettingsWidgetTextTimer()
    timer.delta = delta
    return timer
}

private fun effect(type: SettingsVideoEffectType): SettingsVideoEffect {
    val effect = SettingsVideoEffect()
    effect.type = type
    return effect
}

private fun mask(points: List<Pair<Double, Double>>): SettingsVideoEffect {
    val mask = effect(SettingsVideoEffectType.mask)
    mask.mask.points = points.map { (x, y) -> SettingsVideoEffectMaskEffectPoint(x = x, y = y) }
    return mask
}

@RunWith(RobolectricTestRunner::class)
class RemoteControlSceneConfigValidationSuite {
    class Fixture {
        val mainScene = SettingsScene(name = "Main")
        val targetScene = SettingsScene(name = "Target")
        val mainText = SettingsWidget(name = "Main text")
        val targetText = SettingsWidget(name = "Target text")
        val browser = SettingsWidget(name = "Browser")
        val targetInMain = SettingsWidget(name = "Target in main")
        val mainInTarget = SettingsWidget(name = "Main in target")
        val database = Database()
        val rtmpCameraId = UUID.randomUUID()
        var cameras = RemoteControlCameras(cameraId = { videoSource -> cameraId(videoSource) })

        init {
            mainText.type = SettingsWidgetType.text
            mainText.text.formatString = "Main"
            targetText.type = SettingsWidgetType.text
            targetText.text.formatString = "Target"
            browser.type = SettingsWidgetType.browser
            browser.browser.url = "https://example.com"
            targetInMain.type = SettingsWidgetType.scene
            targetInMain.scene.sceneId = targetScene.id
            mainInTarget.type = SettingsWidgetType.scene
            mainInTarget.scene.sceneId = mainScene.id
            mainScene.widgets = mutableListOf(
                SettingsSceneWidget(widgetId = mainText.id),
                sceneWidget(targetInMain.id),
            )
            targetScene.widgets = mutableListOf(SettingsSceneWidget(widgetId = targetText.id))
            database.scenes = mutableListOf(mainScene, targetScene)
            database.widgets = mutableListOf(mainText, targetText, browser, targetInMain, mainInTarget)
            val rtmp = mainScene.videoSource.updatingCameraId(SettingsCameraId.Rtmp(rtmpCameraId))
            cameras.ids = setOf(cameraId(mainScene.videoSource), cameraId(rtmp))
        }

        fun scene(name: String): SettingsScene = database.scenes.first { it.name == name }

        fun makeWidget(type: SettingsWidgetType): SettingsWidget {
            val widget = emptyWidget(type)
            when (type) {
                SettingsWidgetType.text -> {
                    widget.text.formatString = "{timer}"
                    widget.text.timers = mutableListOf(SettingsWidgetTextTimer())
                }
                SettingsWidgetType.image -> {
                    widget.effects = mutableListOf(effect(SettingsVideoEffectType.opacity))
                }
                SettingsWidgetType.browser -> {
                    widget.browser.url = "https://example.com"
                }
                SettingsWidgetType.slideshow -> {
                    val slide = SettingsWidgetSlideshowSlide()
                    slide.widgetId = mainText.id
                    widget.slideshow.slides = mutableListOf(slide)
                }
                SettingsWidgetType.wheelOfLuck -> {
                    widget.wheelOfLuck.options = mutableListOf(SettingsWidgetWheelOfLuckOption())
                }
                SettingsWidgetType.scene -> {
                    widget.scene.sceneId = targetScene.id
                }
                SettingsWidgetType.crop -> {
                    widget.crop.sourceWidgetId = browser.id
                }
                else -> {}
            }
            return widget
        }
    }

    class WidgetCase(
        val testDescription: String,
        val type: SettingsWidgetType,
        val editableTextTimers: Int = 0,
        val edit: (Fixture, SettingsWidget) -> Unit = { _, _ -> },
    )

    class SceneCase(
        val testDescription: String,
        val scene: String,
        val edit: (Fixture, SettingsScene) -> Unit,
    )

    class WidgetScenesCase(
        val testDescription: String,
        val type: SettingsWidgetType,
        val scenes: List<String>,
        val valid: Boolean,
    )

    @Test
    fun createAndEchoEveryWidgetType() {
        for (type in SettingsWidgetType.entries) {
            val fixture = Fixture()
            val widget = fixture.makeWidget(type)
            assertTrue(
                isValidRemoteControlWidgetUpdate(
                    widget = emptyWidget(type),
                    new = modified(widget),
                    creating = true,
                    editableTextTimers = 0,
                    database = fixture.database,
                    cameras = fixture.cameras,
                ),
            )
            assertTrue(
                isValidRemoteControlWidgetUpdate(
                    widget = widget,
                    new = modified(widget),
                    creating = false,
                    editableTextTimers = 0,
                    database = fixture.database,
                    cameras = fixture.cameras,
                ),
            )
        }
    }

    @Test
    fun widgetTypeCannotChange() {
        val fixture = Fixture()
        assertFalse(
            isValidRemoteControlWidgetUpdate(
                widget = fixture.mainText,
                new = modified(fixture.mainText) { it.type = SettingsWidgetType.image },
                creating = false,
                editableTextTimers = 0,
                database = fixture.database,
                cameras = fixture.cameras,
            ),
        )
    }

    @Test
    fun createIsRejected() {
        val testCases = listOf(
            WidgetCase("Empty text", SettingsWidgetType.text) { _, widget ->
                widget.text.formatString = ""
            },
            WidgetCase("Empty browser URL", SettingsWidgetType.browser) { _, widget ->
                widget.browser.url = ""
            },
            WidgetCase("Blank browser URL", SettingsWidgetType.browser) { _, widget ->
                widget.browser.url = " "
            },
            WidgetCase("Invalid browser URL", SettingsWidgetType.browser) { _, widget ->
                widget.browser.url = "no url"
            },
            WidgetCase("No slides", SettingsWidgetType.slideshow) { _, widget ->
                widget.slideshow.slides = mutableListOf()
            },
            WidgetCase("No wheel of luck options", SettingsWidgetType.wheelOfLuck) { _, widget ->
                widget.wheelOfLuck.options = mutableListOf()
            },
            WidgetCase("Text timer delta", SettingsWidgetType.text) { _, widget ->
                widget.text.formatString = "{timer}"
                widget.text.timers = mutableListOf(timer(15))
            },
            WidgetCase("Crop of text widget", SettingsWidgetType.crop) { fixture, widget ->
                widget.crop.sourceWidgetId = fixture.mainText.id
            },
            WidgetCase("Slide of browser widget", SettingsWidgetType.slideshow) { fixture, widget ->
                widget.slideshow.slides[0].widgetId = fixture.browser.id
            },
            WidgetCase("Slide of unknown widget", SettingsWidgetType.slideshow) { _, widget ->
                widget.slideshow.slides[0].widgetId = UUID.randomUUID()
            },
        )
        for (testCase in testCases) {
            val fixture = Fixture()
            assertFalse(
                isValidRemoteControlWidgetUpdate(
                    widget = emptyWidget(testCase.type),
                    new = modified(fixture.makeWidget(testCase.type)) { testCase.edit(fixture, it) },
                    creating = true,
                    editableTextTimers = testCase.editableTextTimers,
                    database = fixture.database,
                    cameras = fixture.cameras,
                ),
            )
        }
    }

    @Test
    fun createIsAccepted() {
        val testCases = listOf(
            WidgetCase("Default padel players", SettingsWidgetType.scoreboard),
            WidgetCase("Crop of unknown widget", SettingsWidgetType.crop) { _, widget ->
                widget.crop.sourceWidgetId = UUID.randomUUID()
            },
            WidgetCase("Unknown scene", SettingsWidgetType.scene) { _, widget ->
                widget.scene.sceneId = UUID.randomUUID()
            },
            WidgetCase("Text timer with default delta", SettingsWidgetType.text) { _, widget ->
                widget.text.formatString = "{timer}"
                widget.text.timers = mutableListOf(SettingsWidgetTextTimer())
            },
        )
        for (testCase in testCases) {
            val fixture = Fixture()
            assertTrue(
                isValidRemoteControlWidgetUpdate(
                    widget = emptyWidget(testCase.type),
                    new = modified(fixture.makeWidget(testCase.type)) { testCase.edit(fixture, it) },
                    creating = true,
                    editableTextTimers = testCase.editableTextTimers,
                    database = fixture.database,
                    cameras = fixture.cameras,
                ),
                testCase.testDescription,
            )
        }
    }

    @Test
    fun updateIsRejected() {
        val testCases = listOf(
            WidgetCase("Crop of text widget", SettingsWidgetType.crop) { fixture, widget ->
                widget.crop.sourceWidgetId = fixture.mainText.id
            },
            WidgetCase("Crop of unknown widget", SettingsWidgetType.crop) { _, widget ->
                widget.crop.sourceWidgetId = UUID.randomUUID()
            },
            WidgetCase("Unknown scene", SettingsWidgetType.scene) { _, widget ->
                widget.scene.sceneId = UUID.randomUUID()
            },
            WidgetCase("Unknown padel player", SettingsWidgetType.scoreboard) { _, widget ->
                widget.scoreboard.padel.homePlayer1 = UUID.randomUUID()
            },
            WidgetCase("Text font size", SettingsWidgetType.text) { _, widget ->
                widget.text.fontSize = 5
            },
            WidgetCase("Text delay", SettingsWidgetType.text) { _, widget ->
                widget.text.delay = 11.0
            },
            WidgetCase("Text width", SettingsWidgetType.text) { _, widget ->
                widget.text.width = 0
            },
            WidgetCase("Text corner radius", SettingsWidgetType.text) { _, widget ->
                widget.text.cornerRadius = -1
            },
            WidgetCase("Browser URL", SettingsWidgetType.browser) { _, widget ->
                widget.browser.url = "no url"
            },
            WidgetCase("Browser width", SettingsWidgetType.browser) { _, widget ->
                widget.browser.width = 0
            },
            WidgetCase("Browser height", SettingsWidgetType.browser) { _, widget ->
                widget.browser.height = 4000
            },
            WidgetCase("Browser FPS", SettingsWidgetType.browser) { _, widget ->
                widget.browser.baseFps = 16f
            },
            WidgetCase("Crop x", SettingsWidgetType.crop) { _, widget ->
                widget.crop.x = -1
            },
            WidgetCase("Crop width", SettingsWidgetType.crop) { _, widget ->
                widget.crop.width = 0
            },
            WidgetCase("Snapshot showtime", SettingsWidgetType.snapshot) { _, widget ->
                widget.snapshot.showtime = 7
            },
            WidgetCase("Chat height", SettingsWidgetType.chat) { _, widget ->
                widget.chat.height = 2f
            },
            WidgetCase("Chat font size", SettingsWidgetType.chat) { _, widget ->
                widget.chat.fontSize = 100f
            },
            WidgetCase("Chat messages", SettingsWidgetType.chat) { _, widget ->
                widget.chat.maximumNumberOfMessages = 6
            },
            WidgetCase("Map size", SettingsWidgetType.map) { _, widget ->
                widget.map.size = 100.0
            },
            WidgetCase("Map delay", SettingsWidgetType.map) { _, widget ->
                widget.map.delay = 11.0
            },
            WidgetCase("Emote combo minimum", SettingsWidgetType.chatEmoteCombo) { _, widget ->
                widget.chatEmoteCombo.minimumCombo = 1
            },
            WidgetCase("Emote combo reset", SettingsWidgetType.chatEmoteCombo) { _, widget ->
                widget.chatEmoteCombo.resetAfter = 11
            },
            WidgetCase("Pomodoro sound", SettingsWidgetType.pomodoroTimer) { _, widget ->
                widget.pomodoroTimer.focusToBreakSoundId = UUID.randomUUID()
            },
            WidgetCase("Pomodoro focus", SettingsWidgetType.pomodoroTimer) { _, widget ->
                widget.pomodoroTimer.focusDuration = 4
            },
            WidgetCase("Pomodoro break", SettingsWidgetType.pomodoroTimer) { _, widget ->
                widget.pomodoroTimer.breakDuration = 4
            },
            WidgetCase("Pomodoro width", SettingsWidgetType.pomodoroTimer) { _, widget ->
                widget.pomodoroTimer.width = 6.0
            },
            WidgetCase("Golf holes", SettingsWidgetType.scoreboard) { _, widget ->
                widget.scoreboard.golf.numberOfHoles = 10
            },
            WidgetCase("Golf par", SettingsWidgetType.scoreboard) { _, widget ->
                widget.scoreboard.golf.updatePars(listOf(10))
            },
            WidgetCase("Golf players", SettingsWidgetType.scoreboard) { _, widget ->
                widget.scoreboard.golf.players =
                    MutableList(5) { SettingsWidgetGolfScoreboardPlayer() }
            },
            WidgetCase("Generic clock", SettingsWidgetType.scoreboard) { _, widget ->
                widget.scoreboard.generic.clock.maximum = 0
            },
            WidgetCase("Modular width", SettingsWidgetType.scoreboard) { _, widget ->
                widget.scoreboard.modular.width = 50f
            },
            WidgetCase("Modular row height", SettingsWidgetType.scoreboard) { _, widget ->
                widget.scoreboard.modular.rowHeight = 200f
            },
            WidgetCase("Video source rotation", SettingsWidgetType.videoSource) { _, widget ->
                widget.videoSource.rotation = 45.0
            },
            WidgetCase("Video source face zoom", SettingsWidgetType.videoSource) { _, widget ->
                widget.videoSource.trackFaceZoom = 2.0
            },
            WidgetCase("Video source camera", SettingsWidgetType.videoSource) { _, widget ->
                widget.videoSource.videoSource = widget.videoSource.videoSource
                    .updatingCameraId(SettingsCameraId.Rtmp(UUID.randomUUID()))
            },
            WidgetCase("No wheel of luck options", SettingsWidgetType.wheelOfLuck) { _, widget ->
                widget.wheelOfLuck.options = mutableListOf()
            },
            WidgetCase("Wheel of luck weight", SettingsWidgetType.wheelOfLuck) { _, widget ->
                widget.wheelOfLuck.options[0].weight = 11
            },
            WidgetCase("Wheel of luck function", SettingsWidgetType.wheelOfLuck) { _, widget ->
                widget.wheelOfLuck.options[0].function = SettingsControllerFunction.GIMBAL_UP
            },
            WidgetCase("VTuber field of view", SettingsWidgetType.vTuber) { _, widget ->
                widget.vTuber.cameraFieldOfView = 5.0
            },
            WidgetCase("VTuber camera y", SettingsWidgetType.vTuber) { _, widget ->
                widget.vTuber.cameraPositionY = 3.0
            },
            WidgetCase("VTuber arms angle", SettingsWidgetType.vTuber) { _, widget ->
                widget.vTuber.armsAngle = 10.0
            },
            WidgetCase("VTuber mouth", SettingsWidgetType.vTuber) { _, widget ->
                widget.vTuber.sensitivity = widget.vTuber.sensitivity.copy(mouth = 4.0)
            },
            WidgetCase("PNGTuber eyes", SettingsWidgetType.pngTuber) { _, widget ->
                widget.pngTuber.sensitivity = widget.pngTuber.sensitivity.copy(eyes = 6.0)
            },
            WidgetCase("Slide time", SettingsWidgetType.slideshow) { _, widget ->
                widget.slideshow.slides[0].time = 7
            },
            WidgetCase("Alert font size", SettingsWidgetType.alerts) { _, widget ->
                widget.alerts.twitch.follows.fontSize = 5
            },
            WidgetCase("Alert image loops", SettingsWidgetType.alerts) { _, widget ->
                widget.alerts.twitch.follows.imageLoopCount = 0
            },
            WidgetCase("Alert speech delay", SettingsWidgetType.alerts) { _, widget ->
                widget.alerts.twitch.follows.textToSpeechDelay = 6.0
            },
            WidgetCase("Alert image", SettingsWidgetType.alerts) { _, widget ->
                widget.alerts.twitch.follows.imageId = UUID.randomUUID()
            },
            WidgetCase("Alert sound", SettingsWidgetType.alerts) { _, widget ->
                widget.alerts.twitch.follows.soundId = UUID.randomUUID()
            },
            WidgetCase("AI without configuration", SettingsWidgetType.alerts) { _, widget ->
                widget.alerts.aiEnabled = true
            },
            WidgetCase("AI base URL", SettingsWidgetType.alerts) { _, widget ->
                widget.alerts.ai.baseUrl = "no url"
            },
            WidgetCase("Timer delta when not editable", SettingsWidgetType.text) { _, widget ->
                widget.text.formatString = "{timer}"
                widget.text.timers = mutableListOf(timer(15))
            },
            WidgetCase(
                "Delta of timer that is not editable",
                SettingsWidgetType.text,
                editableTextTimers = 1,
            ) { _, widget ->
                widget.text.formatString = "{timer}"
                widget.text.timers = mutableListOf(SettingsWidgetTextTimer(), timer(15))
            },
            WidgetCase(
                "Invalid timer delta",
                SettingsWidgetType.text,
                editableTextTimers = 1,
            ) { _, widget ->
                widget.text.formatString = "{timer}"
                widget.text.timers = mutableListOf(timer(7))
            },
            WidgetCase("Existing timer delta when not editable", SettingsWidgetType.text) { _, widget ->
                widget.text.timers[0].delta = 60
            },
            WidgetCase(
                "Invalid existing timer delta",
                SettingsWidgetType.text,
                editableTextTimers = 1,
            ) { _, widget ->
                widget.text.timers[0].delta = 7
            },
            WidgetCase("Opacity", SettingsWidgetType.image) { _, widget ->
                widget.effects[0].opacity.opacity = 2.0
            },
            WidgetCase("Effect type", SettingsWidgetType.image) { _, widget ->
                widget.effects[0].type = SettingsVideoEffectType.sepia
            },
            WidgetCase("Duplicate effect id", SettingsWidgetType.image) { _, widget ->
                widget.effects = widget.effects + roundTrip(widget.effects[0])
            },
            WidgetCase("Mask with two points", SettingsWidgetType.image) { _, widget ->
                widget.effects = widget.effects + mask(listOf(0.0 to 0.0, 50.0 to 50.0))
            },
            WidgetCase("Unknown LUT", SettingsWidgetType.image) { _, widget ->
                val lut = effect(SettingsVideoEffectType.lut)
                lut.lut.lut = UUID.randomUUID()
                widget.effects = widget.effects + lut
            },
            WidgetCase("Shape corner radius", SettingsWidgetType.image) { _, widget ->
                val shape = effect(SettingsVideoEffectType.shape)
                shape.shape.cornerRadius = 2f
                widget.effects = widget.effects + shape
            },
        )
        for (testCase in testCases) {
            val fixture = Fixture()
            val widget = fixture.makeWidget(testCase.type)
            assertFalse(
                isValidRemoteControlWidgetUpdate(
                    widget = widget,
                    new = modified(widget) { testCase.edit(fixture, it) },
                    creating = false,
                    editableTextTimers = testCase.editableTextTimers,
                    database = fixture.database,
                    cameras = fixture.cameras,
                ),
            )
        }
    }

    @Test
    fun updateIsAccepted() {
        val testCases = listOf(
            WidgetCase("Crop of other browser widget", SettingsWidgetType.crop) { fixture, widget ->
                val browser = fixture.makeWidget(SettingsWidgetType.browser)
                fixture.database.widgets.add(browser)
                widget.crop.sourceWidgetId = browser.id
            },
            WidgetCase("Other scene", SettingsWidgetType.scene) { fixture, widget ->
                widget.scene.sceneId = fixture.mainScene.id
            },
            WidgetCase("Known padel player", SettingsWidgetType.scoreboard) { fixture, widget ->
                val player = SettingsWidgetScoreboardPlayer()
                fixture.database.scoreboardPlayers = mutableListOf(player)
                widget.scoreboard.padel.homePlayer1 = player.id
            },
            WidgetCase("Text font size", SettingsWidgetType.text) { _, widget ->
                widget.text.fontSize = 50
            },
            WidgetCase("Text format string", SettingsWidgetType.text) { _, widget ->
                widget.text.formatString = "{time}"
            },
            WidgetCase("Text background color", SettingsWidgetType.text) { _, widget ->
                widget.text.backgroundColor = RgbColor(red = 1, green = 2, blue = 3)
            },
            WidgetCase("Browser URL", SettingsWidgetType.browser) { _, widget ->
                widget.browser.url = "https://example.org/"
            },
            WidgetCase("Snapshot showtime", SettingsWidgetType.snapshot) { _, widget ->
                widget.snapshot.showtime = 10
            },
            WidgetCase("QR code message", SettingsWidgetType.qrCode) { _, widget ->
                widget.qrCode.message = "Remote"
            },
            WidgetCase("Golf holes", SettingsWidgetType.scoreboard) { _, widget ->
                widget.scoreboard.golf.numberOfHoles = 9
            },
            WidgetCase("Golf players", SettingsWidgetType.scoreboard) { _, widget ->
                widget.scoreboard.golf.players =
                    MutableList(3) { SettingsWidgetGolfScoreboardPlayer() }
            },
            WidgetCase("Scoreboard sport", SettingsWidgetType.scoreboard) { _, widget ->
                widget.scoreboard.sport = SettingsWidgetScoreboardSport.padel
            },
            WidgetCase("Wheel of luck weight", SettingsWidgetType.wheelOfLuck) { _, widget ->
                widget.wheelOfLuck.options[0].weight = 20
            },
            WidgetCase("Wheel of luck option", SettingsWidgetType.wheelOfLuck) { _, widget ->
                widget.wheelOfLuck.options = widget.wheelOfLuck.options + SettingsWidgetWheelOfLuckOption()
            },
            WidgetCase("Video source rotation", SettingsWidgetType.videoSource) { _, widget ->
                widget.videoSource.rotation = 90.0
            },
            WidgetCase("Pomodoro sound", SettingsWidgetType.pomodoroTimer) { fixture, widget ->
                val sound = SettingsAlertsMediaGalleryItem(name = "Sound")
                fixture.database.alertsMediaGallery.customSounds = mutableListOf(sound)
                widget.pomodoroTimer.focusToBreakSoundId = sound.id
            },
            WidgetCase("Alert image and sound", SettingsWidgetType.alerts) { fixture, widget ->
                val image = SettingsAlertsMediaGalleryItem(name = "Image")
                val sound = SettingsAlertsMediaGalleryItem(name = "Sound")
                fixture.database.alertsMediaGallery.customImages = mutableListOf(image)
                fixture.database.alertsMediaGallery.customSounds = mutableListOf(sound)
                widget.alerts.twitch.follows.imageId = image.id
                widget.alerts.twitch.follows.soundId = sound.id
            },
            WidgetCase("Configured AI", SettingsWidgetType.alerts) { _, widget ->
                widget.alerts.ai.apiKey = "key"
                widget.alerts.aiEnabled = true
            },
            WidgetCase("Chat bot command", SettingsWidgetType.alerts) { _, widget ->
                widget.alerts.chatBot.commands = widget.alerts.chatBot.commands + SettingsWidgetAlertsChatBotCommand()
            },
            WidgetCase(
                "Timer delta when editable",
                SettingsWidgetType.text,
                editableTextTimers = 1,
            ) { _, widget ->
                widget.text.formatString = "{timer}"
                widget.text.timers = mutableListOf(timer(15))
            },
            WidgetCase(
                "Existing timer delta when editable",
                SettingsWidgetType.text,
                editableTextTimers = 1,
            ) { _, widget ->
                widget.text.timers[0].delta = 60
            },
            WidgetCase("Opacity", SettingsWidgetType.image) { _, widget ->
                widget.effects[0].opacity.opacity = 0.25
            },
            WidgetCase("Insert effect", SettingsWidgetType.image) { _, widget ->
                widget.effects = listOf(effect(SettingsVideoEffectType.grayScale)) + widget.effects
            },
            WidgetCase("Remove effects", SettingsWidgetType.image) { _, widget ->
                widget.effects = mutableListOf()
            },
            WidgetCase("Mask with three points", SettingsWidgetType.image) { _, widget ->
                widget.effects = widget.effects + mask(listOf(0.0 to 0.0, 50.0 to 50.0, 50.0 to 0.0))
            },
            WidgetCase("Known LUT", SettingsWidgetType.image) { fixture, widget ->
                val colorLut = SettingsColorLut(type = SettingsColorLutType.diskCube, name = "Lut")
                fixture.database.color.diskLutsCube = listOf(colorLut)
                val lut = effect(SettingsVideoEffectType.lut)
                lut.lut.lut = colorLut.id
                widget.effects = widget.effects + lut
            },
            WidgetCase("Ignored effects of text widget", SettingsWidgetType.text) { _, widget ->
                val opacity = effect(SettingsVideoEffectType.opacity)
                widget.effects = mutableListOf(opacity, opacity)
            },
        )
        for (testCase in testCases) {
            val fixture = Fixture()
            val widget = fixture.makeWidget(testCase.type)
            assertTrue(
                isValidRemoteControlWidgetUpdate(
                    widget = widget,
                    new = modified(widget) { testCase.edit(fixture, it) },
                    creating = false,
                    editableTextTimers = testCase.editableTextTimers,
                    database = fixture.database,
                    cameras = fixture.cameras,
                ),
            )
        }
    }

    @Test
    fun danglingReferenceIsKeptOnEcho() {
        val testCases = listOf(
            WidgetCase("Crop of unknown widget", SettingsWidgetType.crop) { _, widget ->
                widget.crop.sourceWidgetId = UUID.randomUUID()
            },
            WidgetCase("Slide of unknown widget", SettingsWidgetType.slideshow) { _, widget ->
                widget.slideshow.slides[0].widgetId = UUID.randomUUID()
            },
            WidgetCase("Unknown scene", SettingsWidgetType.scene) { _, widget ->
                widget.scene.sceneId = UUID.randomUUID()
            },
        )
        for (testCase in testCases) {
            val fixture = Fixture()
            val widget = fixture.makeWidget(testCase.type)
            testCase.edit(fixture, widget)
            assertTrue(
                isValidRemoteControlWidgetUpdate(
                    widget = widget,
                    new = modified(widget),
                    creating = false,
                    editableTextTimers = testCase.editableTextTimers,
                    database = fixture.database,
                    cameras = fixture.cameras,
                ),
            )
        }
    }

    @Test
    fun widgetName() {
        val testCases = listOf(
            "Renamed" to true,
            "Main text" to true,
            "Target text" to false,
            "" to false,
        )
        for ((name, isValid) in testCases) {
            val fixture = Fixture()
            val widgets = fixture.database.widgets
            assertEquals(
                isValid,
                isValidRemoteControlName(
                    modified(fixture.mainText) { it.name = name },
                    widgets,
                    widgets,
                ),
            )
        }
    }

    @Test
    fun newWidgetCannotTakeExistingName() {
        val fixture = Fixture()
        val widgets = fixture.database.widgets
        val new = modified(fixture.makeWidget(SettingsWidgetType.text)) { it.name = "Main text" }
        assertFalse(isValidRemoteControlName(new, widgets + listOf(new), widgets))
    }

    @Test
    fun sceneEcho() {
        for (name in listOf("Main", "Target")) {
            val fixture = Fixture()
            val scene = fixture.scene(name)
            val echo = modified(scene)
            assertTrue(
                isValidRemoteControlScene(
                    scene = echo,
                    scenes = replacing(fixture.database.scenes, echo),
                    database = fixture.database,
                ),
            )
            assertTrue(
                isValidRemoteControlSceneUpdate(
                    scene = scene,
                    new = echo,
                    database = fixture.database,
                    cameras = fixture.cameras,
                ),
            )
        }
    }

    @Test
    fun sceneClone() {
        val fixture = Fixture()
        val clone = modified(fixture.mainScene) {
            it.id = UUID.randomUUID()
            it.name = "Clone"
        }
        assertTrue(
            isValidRemoteControlScene(
                scene = clone,
                scenes = fixture.database.scenes + listOf(clone),
                database = fixture.database,
            ),
        )
        assertTrue(
            isValidRemoteControlSceneUpdate(
                scene = emptyScene(clone),
                new = clone,
                database = fixture.database,
                cameras = fixture.cameras,
            ),
        )
    }

    @Test
    fun newSceneCannotTakeExistingName() {
        val fixture = Fixture()
        val new = SettingsScene(name = "Target")
        assertFalse(
            isValidRemoteControlScene(
                scene = new,
                scenes = fixture.database.scenes + listOf(new),
                database = fixture.database,
            ),
        )
    }

    @Test
    fun sceneName() {
        val testCases = listOf(
            "Renamed" to true,
            "Target" to false,
            "" to false,
        )
        for ((name, isValid) in testCases) {
            val fixture = Fixture()
            val renamed = modified(fixture.mainScene) { it.name = name }
            assertEquals(
                isValid,
                isValidRemoteControlScene(
                    scene = renamed,
                    scenes = replacing(fixture.database.scenes, renamed),
                    database = fixture.database,
                ),
            )
        }
    }

    @Test
    fun sceneWidgetOutsideCanvasIsRejected() {
        val testCases = listOf(
            Triple(101.0, 0.0, 10.0),
            Triple(-1.0, 0.0, 10.0),
            Triple(0.0, 101.0, 10.0),
            Triple(0.0, -1.0, 10.0),
            Triple(0.0, 0.0, 0.0),
            Triple(0.0, 0.0, 101.0),
        )
        for ((x, y, size) in testCases) {
            val fixture = Fixture()
            assertFalse(
                isValidRemoteControlSceneUpdate(
                    scene = fixture.mainScene,
                    new = modified(fixture.mainScene) {
                        it.widgets[0].layout = it.widgets[0].layout.copy(x = x, y = y, size = size)
                    },
                    database = fixture.database,
                    cameras = fixture.cameras,
                ),
            )
            assertFalse(
                isValidRemoteControlSceneUpdate(
                    scene = fixture.targetScene,
                    new = modified(fixture.targetScene) {
                        it.widgets.add(
                            sceneWidget(fixture.mainText.id, x = x, y = y, size = size),
                        )
                    },
                    database = fixture.database,
                    cameras = fixture.cameras,
                ),
            )
        }
    }

    @Test
    fun sceneWidgetAtCanvasEdgeIsAccepted() {
        val fixture = Fixture()
        assertTrue(
            isValidRemoteControlSceneUpdate(
                scene = fixture.mainScene,
                new = modified(fixture.mainScene) {
                    it.widgets[0].layout = it.widgets[0].layout.copy(
                        x = 100.0,
                        y = 100.0,
                        size = 1.0,
                        alignment = SettingsAlignment.bottomRight,
                    )
                },
                database = fixture.database,
                cameras = fixture.cameras,
            ),
        )
    }

    @Test
    fun sceneUpdateIsRejected() {
        val testCases = listOf(
            SceneCase("Change widget of scene widget", "Main") { fixture, scene ->
                scene.widgets[0].widgetId = fixture.targetText.id
            },
            SceneCase("Add unknown widget", "Main") { _, scene ->
                scene.widgets.add(sceneWidget(UUID.randomUUID()))
            },
            SceneCase("Rotation", "Target") { _, scene ->
                scene.videoSourceRotation = 45.0
            },
            SceneCase("Too high quick switch group", "Target") { _, scene ->
                scene.quickSwitchGroup = 5
            },
            SceneCase("Too low quick switch group", "Target") { _, scene ->
                scene.quickSwitchGroup = 0
            },
            SceneCase("Unknown mic", "Target") { _, scene ->
                scene.micId = "Unknown mic"
            },
            SceneCase("Unknown camera", "Target") { _, scene ->
                scene.videoSource = scene.videoSource
                    .updatingCameraId(SettingsCameraId.Rtmp(UUID.randomUUID()))
            },
        )
        for (testCase in testCases) {
            val fixture = Fixture()
            val scene = fixture.scene(testCase.scene)
            assertFalse(
                isValidRemoteControlSceneUpdate(
                    scene = scene,
                    new = modified(scene) { testCase.edit(fixture, it) },
                    database = fixture.database,
                    cameras = fixture.cameras,
                ),
            )
        }
    }

    @Test
    fun sceneUpdateIsAccepted() {
        val testCases = listOf(
            SceneCase("Add widget", "Main") { fixture, scene ->
                scene.widgets.add(sceneWidget(fixture.browser.id))
            },
            SceneCase("Remove widgets", "Main") { _, scene ->
                scene.widgets.clear()
            },
            SceneCase("Rotation", "Target") { _, scene ->
                scene.videoSourceRotation = 90.0
            },
            SceneCase("Quick switch group", "Target") { _, scene ->
                scene.quickSwitchGroup = 2
            },
            SceneCase("Known mic", "Target") { fixture, scene ->
                val mic = SettingsMicsMic()
                mic.inputUid = "Mic"
                fixture.database.mics._mics.value = listOf(mic)
                scene.micId = mic.id
            },
            SceneCase("Mirror, fill frame, stabilization and color", "Target") { _, scene ->
                scene.mirror = true
                scene.fillFrame = true
                scene.overrideVideoStabilizationMode = true
                scene.backgroundColor = RgbColor(red = 10, green = 20, blue = 30)
            },
            SceneCase("Known camera", "Target") { fixture, scene ->
                scene.videoSource = scene.videoSource
                    .updatingCameraId(SettingsCameraId.Rtmp(fixture.rtmpCameraId))
            },
        )
        for (testCase in testCases) {
            val fixture = Fixture()
            val scene = fixture.scene(testCase.scene)
            assertTrue(
                isValidRemoteControlSceneUpdate(
                    scene = scene,
                    new = modified(scene) { testCase.edit(fixture, it) },
                    database = fixture.database,
                    cameras = fixture.cameras,
                ),
            )
        }
    }

    @Test
    fun newSceneWithUnknownWidgetIsRejected() {
        val fixture = Fixture()
        val new = SettingsScene(name = "Unknown widget")
        new.widgets = mutableListOf(sceneWidget(UUID.randomUUID()))
        assertFalse(
            isValidRemoteControlSceneUpdate(
                scene = emptyScene(new),
                new = modified(new),
                database = fixture.database,
                cameras = fixture.cameras,
            ),
        )
    }

    @Test
    fun duplicateWidgetsAreRejected() {
        val testCases = listOf(
            SceneCase("Add same widget again", "Main") { fixture, scene ->
                scene.widgets.add(sceneWidget(fixture.mainText.id))
            },
            SceneCase("Duplicate scene widget id", "Main") { fixture, scene ->
                val duplicate = roundTrip(scene.widgets[0])
                duplicate.widgetId = fixture.targetText.id
                scene.widgets.add(duplicate)
            },
            SceneCase("Add widget shown by nested scene", "Main") { fixture, scene ->
                scene.widgets.add(sceneWidget(fixture.targetText.id))
            },
            SceneCase("Add widget of scene shown in other scene", "Target") { fixture, scene ->
                scene.widgets.add(sceneWidget(fixture.mainText.id))
            },
        )
        for (testCase in testCases) {
            val fixture = Fixture()
            val new = modified(fixture.scene(testCase.scene)) { testCase.edit(fixture, it) }
            assertFalse(
                isValidRemoteControlScene(
                    scene = new,
                    scenes = replacing(fixture.database.scenes, new),
                    database = fixture.database,
                ),
            )
        }
    }

    @Test
    fun sceneWithoutDuplicateWidgetsIsAccepted() {
        val testCases = listOf(
            SceneCase("Add other widget", "Main") { fixture, scene ->
                scene.widgets.add(sceneWidget(fixture.browser.id))
            },
            SceneCase("Remove widgets", "Main") { _, scene ->
                scene.widgets.clear()
            },
            SceneCase("Show scene that shows this scene", "Target") { fixture, scene ->
                scene.widgets.add(sceneWidget(fixture.mainInTarget.id))
            },
        )
        for (testCase in testCases) {
            val fixture = Fixture()
            val new = modified(fixture.scene(testCase.scene)) { testCase.edit(fixture, it) }
            assertTrue(
                isValidRemoteControlScene(
                    scene = new,
                    scenes = replacing(fixture.database.scenes, new),
                    database = fixture.database,
                ),
            )
        }
    }

    @Test
    fun newSceneWithSameWidgetTwiceIsRejected() {
        val fixture = Fixture()
        val new = SettingsScene(name = "Twice")
        new.widgets = mutableListOf(
            sceneWidget(fixture.mainText.id),
            sceneWidget(fixture.mainText.id),
        )
        assertFalse(
            isValidRemoteControlScene(
                scene = new,
                scenes = fixture.database.scenes + listOf(new),
                database = fixture.database,
            ),
        )
    }

    @Test
    fun sceneWidgetCanShowSceneItIsIn() {
        val fixture = Fixture()
        val targetInMain = fixture.targetInMain
        assertTrue(
            isValidRemoteControlWidgetUpdate(
                widget = targetInMain,
                new = modified(targetInMain) {
                    it.scene.sceneId = fixture.mainScene.id
                },
                creating = false,
                editableTextTimers = 0,
                database = fixture.database,
                cameras = fixture.cameras,
            ),
        )
    }

    @Test
    fun centeringSceneWidgetUnlocksPosition() {
        val sceneWidget = SettingsSceneWidget(widgetId = UUID.randomUUID())
        sceneWidget.layout = sceneWidget.layout.copy(positioningLock = true)
        val kept = remoteControlMergeSceneWidgets(
            listOf(sceneWidget),
            listOf(roundTrip(sceneWidget)),
        )
        assertSame(sceneWidget, kept.first())
        assertTrue(sceneWidget.layout.positioningLock)
        remoteControlMergeSceneWidgets(
            listOf(sceneWidget),
            listOf(
                modified(sceneWidget) {
                    it.layout = it.layout.copy(alignment = SettingsAlignment.center)
                },
            ),
        )
        assertFalse(sceneWidget.layout.positioningLock)
        val new = SettingsSceneWidget(widgetId = UUID.randomUUID())
        new.layout = new.layout.copy(
            positioningLock = true,
            alignment = SettingsAlignment.topCenter,
        )
        val merged = remoteControlMergeSceneWidgets(listOf(sceneWidget), listOf(new))
        assertSame(new, merged.first())
        assertFalse(new.layout.positioningLock)
    }

    @Test
    fun partialCropDecodesDefaults() {
        val sourceWidgetId = UUID.randomUUID()
        val json = """{"sourceWidgetId": "${sourceWidgetId.uuidString}", "x": 5}"""
        val crop = codableJson.decodeFromString<SettingsWidgetCrop>(json)
        assertEquals(sourceWidgetId, crop.sourceWidgetId)
        assertEquals(5, crop.x)
        assertEquals(0, crop.y)
        assertEquals(200, crop.width)
        assertEquals(200, crop.height)
    }

    @Test
    fun createWidgetInScenes() {
        val testCases = listOf(
            WidgetScenesCase("No scenes", SettingsWidgetType.text, listOf(), true),
            WidgetScenesCase("One scene", SettingsWidgetType.text, listOf("Main"), true),
            WidgetScenesCase("Nested scene only", SettingsWidgetType.browser, listOf("Target"), true),
            WidgetScenesCase(
                "Scene and its nested scene",
                SettingsWidgetType.text,
                listOf("Main", "Target"),
                false,
            ),
            WidgetScenesCase("Same scene twice", SettingsWidgetType.text, listOf("Target", "Target"), false),
            WidgetScenesCase(
                "Scene widget into scene already nesting it",
                SettingsWidgetType.scene,
                listOf("Main"),
                true,
            ),
            WidgetScenesCase(
                "Scene widget into its own scene",
                SettingsWidgetType.scene,
                listOf("Target"),
                true,
            ),
        )
        for (widgetScenesCase in testCases) {
            val fixture = Fixture()
            assertEquals(
                widgetScenesCase.valid,
                isValidRemoteControlWidgetScenes(
                    widget = fixture.makeWidget(widgetScenesCase.type),
                    sceneIds = widgetScenesCase.scenes.map { fixture.scene(it).id },
                    database = fixture.database,
                ),
            )
        }
    }

    @Test
    fun createWidgetInUnknownScene() {
        val fixture = Fixture()
        assertFalse(
            isValidRemoteControlWidgetScenes(
                widget = fixture.makeWidget(SettingsWidgetType.text),
                sceneIds = listOf(UUID.randomUUID()),
                database = fixture.database,
            ),
        )
    }
}

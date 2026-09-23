package com.moblin.android.various.settings

import androidx.compose.runtime.snapshots.Snapshot
import com.moblin.android.common.various.RgbColor
import com.moblin.android.platform.codable.codableJson
import com.moblin.android.platform.codable.decode
import com.moblin.android.various.utils.defaultBackCameraPosition
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private val uuidPattern = Regex("\"[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}\"")

private fun normalized(json: String): String = uuidPattern.replace(json, "\"UUID\"")

private class Case<T>(
    val name: String,
    val serializer: KSerializer<T>,
    val make: () -> T,
    val synthesized: Boolean = false,
    val fromEmpty: () -> T = make,
) {
    fun encode(value: T): String = codableJson.encodeToString(serializer, value)

    fun decode(json: String): T = codableJson.decodeFromString(serializer, json)

    fun checkRoundTrip() {
        val first = encode(make())
        val second = encode(decode(first))
        assertEquals(first, second, "$name round trip")
        assertEquals(first, encode(decode(second)), "$name second round trip")
    }

    fun checkEmptyObject() {
        if (synthesized) {
            assertFailsWith<Exception>("$name must reject {} like Swift synthesized Codable") { decode("{}") }
        } else {
            assertEquals(normalized(encode(fromEmpty())), normalized(encode(decode("{}"))), "$name from {}")
        }
    }

    fun encodedKeys(): List<String> = codableJson.parseToJsonElement(encode(make())).jsonObject.keys.toList()

    fun checkSwiftStyleImport() {
        val kotlinJson = codableJson.parseToJsonElement(encode(make()))
        val decoded = codableJson.decodeFromJsonElement(serializer, swiftStyle(kotlinJson))
        assertEquals(kotlinJson, codableJson.parseToJsonElement(encode(decoded)), "$name from Swift style JSON")
    }
}

private fun swiftStyle(element: JsonElement): JsonElement = when (element) {
    is JsonObject -> JsonObject(
        element.filterNot { (key, value) -> key == "opacity" && value is JsonNull }.mapValues { swiftStyle(it.value) },
    )
    is JsonArray -> JsonArray(element.map { swiftStyle(it) })
    is JsonPrimitive -> if (!element.isString && element.content.endsWith(".0")) {
        JsonPrimitive(element.content.dropLast(2).toLong())
    } else {
        element
    }
}

private val swiftEncodedKeys: Map<String, List<String>> = mapOf(
    "SettingsVideoEffectRemoveBackground" to listOf("from", "to"),
    "SettingsVideoEffectShape" to listOf(
        "cornerRadius", "borderWidth", "borderColor", "cropEnabled", "cropX", "cropY", "cropWidth", "cropHeight",
    ),
    "SettingsVideoEffectDewarp360" to listOf("pan", "tilt", "zoom"),
    "SettingsVideoEffectAnamorphicLens" to listOf("scale"),
    "SettingsVideoEffectLut" to listOf("lut"),
    "SettingsVideoEffectOpacity" to listOf("opacity"),
    "SettingsVideoEffectMaskEffectPoint" to listOf("x", "y"),
    "SettingsVideoEffectMask" to listOf(
        "points", "inverted", "tension", "backgroundType", "backgroundColor", "backgroundColor2",
    ),
    "SettingsVideoEffect" to listOf(
        "id", "enabled", "type", "removeBackground", "shape", "dewarp360", "anamorphicLens", "lut", "opacity", "mask",
    ),
    "SettingsWidgetTextTimer" to listOf("id", "delta", "endTime"),
    "SettingsWidgetTextStopwatch" to listOf("id", "totalElapsed", "running"),
    "SettingsWidgetTextSubtitles" to listOf(),
    "SettingsWidgetTextCheckbox" to listOf("id", "checked"),
    "SettingsWidgetTextRating" to listOf("id", "rating"),
    "SettingsWidgetTextLapTimes" to listOf("id", "lapTimes"),
    "SettingsWidgetText" to listOf(
        "formatString", "backgroundColor", "clearBackgroundColor", "foregroundColor", "clearForegroundColor",
        "fontSize", "fontFamily", "fontStyle", "fontDesign", "fontWeight", "fontMonospacedDigits", "alignment",
        "horizontalAlignment", "verticalAlignment", "delay", "timers", "stopwatches", "needsWeather",
        "needsGeography", "needsSubtitles", "subtitles", "checkboxes", "ratings", "lapTimes", "needsGForce",
        "widthEnabled", "width", "cornerRadius",
    ),
    "SettingsWidgetCrop" to listOf("sourceWidgetId", "x", "y", "width", "height"),
    "SettingsWidgetBrowser" to listOf(
        "url", "width", "height", "mode", "fps", "styleSheet", "moblinAccess", "speechToText", "localOnly",
    ),
    "SettingsWidgetMap" to listOf("northUp", "delay", "scale"),
    "SettingsWidgetScene" to listOf("sceneId"),
    "SettingsWidgetQrCode" to listOf("message"),
    "SettingsWidgetAlertFacePosition" to listOf("x", "y", "width", "height"),
    "SettingsWidgetAlertsAlert" to listOf(
        "id", "enabled", "mediaType", "imageId", "imageLoopCount", "soundId", "videoName", "textColor",
        "accentColor", "fontSize", "fontDesign", "fontWeight", "textToSpeechEnabled", "textToSpeechDelay",
        "textToSpeechLanguageVoices", "positionType", "facePosition",
    ),
    "SettingsWidgetAlertsCheerBitsAlert" to listOf("id", "bits", "comparisonOperator", "alert"),
    "SettingsWidgetAlertsKickGiftsAlert" to listOf("id", "amount", "comparisonOperator", "alert"),
    "SettingsWidgetAlertsTwitch" to listOf("follows", "subscriptions", "raids", "cheers", "cheerBits"),
    "SettingsWidgetAlertsKick" to listOf("subscriptions", "giftedSubscriptions", "hosts", "rewards", "kickGifts"),
    "SettingsWidgetAlertsChatBotCommand" to listOf("id", "name", "alert", "imageType"),
    "SettingsWidgetAlertsChatBot" to listOf("commands"),
    "SettingsWidgetAlertsSpeechToTextString" to listOf("id", "string", "alert"),
    "SettingsWidgetAlertsSpeechToText" to listOf("strings"),
    "SettingsTtsMonster" to listOf("apiToken"),
    "SettingsWidgetAlerts" to listOf(
        "twitch", "kick", "chatBot", "speechToText", "needsSubtitles", "ai", "aiEnabled", "ttsMonster",
    ),
    "SettingsSensitivity" to listOf("mouth", "eyes"),
    "SettingsWidgetVTuber" to listOf(
        "id", "type", "cameraPosition", "backCameraId", "frontCameraId", "rtmpCameraId", "srtlaCameraId",
        "srtClientCameraId", "ristCameraId", "rtspCameraId", "whipCameraId", "whepCameraId", "mediaPlayerCameraId",
        "externalCameraId", "externalCameraName", "cameraPositionY", "cameraFieldOfView", "modelName", "mirror",
        "sensitivity", "armsAngle",
    ),
    "SettingsWidgetPngTuber" to listOf(
        "id", "cameraPosition", "backCameraId", "frontCameraId", "rtmpCameraId", "srtlaCameraId",
        "srtClientCameraId", "ristCameraId", "rtspCameraId", "whipCameraId", "whepCameraId", "mediaPlayerCameraId",
        "externalCameraId", "externalCameraName", "modelName", "mirror", "sensitivity",
    ),
    "SettingsWidgetSnapshot" to listOf("id", "showtime"),
    "SettingsWidgetChat" to listOf(
        "id", "fontSize", "usernameColor", "messageColor", "backgroundColor", "backgroundColorEnabled",
        "shadowColor", "shadowColorEnabled", "boldUsername", "boldMessage", "badges", "displayStyle",
        "sharedChatIcons", "height", "maximumNumberOfMessages",
    ),
    "SettingsWidgetSlideshowSlide" to listOf("id", "widgetId", "time"),
    "SettingsWidgetSlideshow" to listOf("id", "slides"),
    "SettingsWidgetWheelOfLuckOption" to listOf("id", "text", "weight"),
    "SettingsWidgetWheelOfLuck" to listOf("advanced", "options"),
    "SettingsBingoCardSquare" to listOf("id", "text", "checked"),
    "SettingsWidgetBingoCard" to listOf("backgroundColor", "foregroundColor", "squares"),
    "SettingsWidgetPomodoroTimer" to listOf(
        "focusDuration", "breakDuration", "width", "focusName", "breakName", "focusIcon", "breakIcon",
        "backgroundColor", "foregroundColor", "focusColor", "breakColor", "focusToBreakSoundId",
        "breakToFocusSoundId", "focusToBreakChatMessage", "breakToFocusChatMessage",
    ),
    "SettingsWidgetChatEmoteCombo" to listOf("minimumCombo", "resetAfter"),
    "SettingsWidget" to listOf(
        "name", "id", "type", "text", "browser", "crop", "map", "scene", "qrCode", "alerts", "videoSource",
        "scoreboard", "vTuber", "pngTuber", "snapshot", "chat", "chatEmoteCombo", "slideshow", "wheelOfLuck",
        "bingoCard", "pomodoroTimer", "enabled", "effects",
    ),
    "SettingsSceneWidget" to listOf(
        "widgetId", "id", "x", "y", "width", "height", "size", "alignment", "positioningLock", "migrated",
        "migrated2",
    ),
    "SettingsWidgetVideoSource" to listOf(
        "cornerRadius", "cameraPosition", "backCameraId", "frontCameraId", "rtmpCameraId", "srtlaCameraId",
        "srtClientCameraId", "ristCameraId", "rtspCameraId", "whipCameraId", "whepCameraId", "mediaPlayerCameraId",
        "externalCameraId", "externalCameraName", "cropEnabled", "cropX", "cropY", "cropWidth", "cropHeight",
        "rotation", "trackFaceEnabled", "trackFaceZoom", "mirror", "borderWidth", "borderColor",
    ),
    "SettingsWidgetScoreboardPlayer" to listOf("id", "name"),
    "SettingsWidgetScoreboardScore" to listOf("home", "away"),
    "SettingsWidgetPadelScoreboard" to listOf(
        "type", "homePlayer1", "homePlayer2", "awayPlayer1", "awayPlayer2", "score",
    ),
    "SettingsWidgetGolfScoreboardPlayer" to listOf("id", "name", "scores", "color"),
    "SettingsWidgetGolfScoreboard" to listOf(
        "eventName", "numberOfHoles", "currentHole", "pars", "players", "playerColors", "showPars",
    ),
    "SettingsWidgetGenericScoreboard" to listOf("home", "away", "title", "period", "clock"),
    "SettingsWidgetModularScoreboardTeam" to listOf("name", "textColor", "backgroundColor"),
    "SettingsWidgetScoreboardClock" to listOf("maximum", "direction"),
    "SettingsWidgetModularScoreboard" to listOf(
        "home", "away", "title", "period", "infoBoxText", "clock", "layout", "width", "rowHeight", "isBold",
        "showTitle", "showMoreStats", "showGlobalStatsBlock", "showClock",
    ),
    "SettingsWidgetScoreboard" to listOf(
        "type", "textColor", "primaryBackgroundColor", "secondaryBackgroundColor", "padel", "golf", "generic",
        "modular",
    ),
    "SettingsScene" to listOf(
        "name", "id", "enabled", "cameraPosition", "backCameraId", "frontCameraId", "rtmpCameraId",
        "srtlaCameraId", "srtClientCameraId", "ristCameraId", "rtspCameraId", "whipCameraId", "whepCameraId",
        "mediaPlayerCameraId", "externalCameraId", "externalCameraName", "widgets", "videoSourceRotation",
        "videoStabilizationMode", "overrideVideoStabilizationMode", "fillFrame", "overrideMic", "micId",
        "quickSwitchGroup", "mirror", "backgroundColor",
    ),
    "SettingsAutoSceneSwitcherScene" to listOf("id", "sceneId", "time"),
    "SettingsAutoSceneSwitcher" to listOf("id", "name", "shuffle", "scenes"),
    "SettingsAutoSceneSwitchers" to listOf("switcherId", "switchers"),
)

private class RawValueEnum<T>(
    val name: String,
    val serializer: KSerializer<T>,
    val entries: List<T>,
    val rawValues: List<String>,
    val unknownDecodesTo: T? = null,
) {
    fun check() {
        assertEquals(rawValues.size, entries.size, "$name case count")
        for ((entry, rawValue) in entries.zip(rawValues)) {
            assertEquals("\"$rawValue\"", codableJson.encodeToString(serializer, entry), "$name encode $entry")
            assertEquals(entry, codableJson.decodeFromString(serializer, "\"$rawValue\""), "$name decode $rawValue")
        }
        val fallback = entries.last()
        val unknown = codableJson.parseToJsonElement("""{"value":"NotARawValue","number":5}""").jsonObject
        assertEquals(unknownDecodesTo ?: fallback, unknown.decode("value", serializer, fallback), "$name unknown")
        assertEquals(fallback, unknown.decode("number", serializer, fallback), "$name number")
    }
}

private val swiftRawValueEnums: List<RawValueEnum<*>> = listOf(
    RawValueEnum(
        "SettingsVideoEffectType",
        SettingsVideoEffectType.serializer(),
        SettingsVideoEffectType.entries,
        listOf(
            "shape", "grayScale", "sepia", "whirlpool", "pinch", "removeBackground", "dewarp360", "anamorphicLens",
            "lut", "opacity", "mask",
        ),
    ),
    RawValueEnum(
        "SettingsMaskBackgroundType",
        SettingsMaskBackgroundType.serializer(),
        SettingsMaskBackgroundType.entries,
        listOf("Transparent", "Solid", "Checkerboard"),
    ),
    RawValueEnum(
        "SettingsFontDesign",
        SettingsFontDesign.serializer(),
        SettingsFontDesign.entries,
        listOf("Default", "Serif", "Rounded", "Monospaced"),
    ),
    RawValueEnum(
        "SettingsFontWeight",
        SettingsFontWeight.serializer(),
        SettingsFontWeight.entries,
        listOf("Regular", "Light", "Bold"),
    ),
    RawValueEnum(
        "SettingsHorizontalAlignment",
        SettingsHorizontalAlignment.serializer(),
        SettingsHorizontalAlignment.entries,
        listOf("Leading", "Trailing", "Center"),
    ),
    RawValueEnum(
        "SettingsVerticalAlignment",
        SettingsVerticalAlignment.serializer(),
        SettingsVerticalAlignment.entries,
        listOf("Top", "Bottom"),
    ),
    RawValueEnum(
        "SettingsAlignment",
        SettingsAlignment.serializer(),
        SettingsAlignment.entries,
        listOf(
            "TopLeft", "TopRight", "BottomLeft", "BottomRight", "TopCenter", "BottomCenter", "LeftCenter",
            "RightCenter", "Center",
        ),
    ),
    RawValueEnum(
        "SettingsWidgetBrowserMode",
        SettingsWidgetBrowserMode.serializer(),
        SettingsWidgetBrowserMode.entries,
        listOf("periodicAudioAndVideo", "audioAndVideoOnly", "audioOnly"),
    ),
    RawValueEnum(
        "SettingsWidgetAlertPositionType",
        SettingsWidgetAlertPositionType.serializer(),
        SettingsWidgetAlertPositionType.entries,
        listOf("Scene", "Face"),
    ),
    RawValueEnum(
        "SettingsWidgetAlertsAlertMediaType",
        SettingsWidgetAlertsAlertMediaType.serializer(),
        SettingsWidgetAlertsAlertMediaType.entries,
        listOf("gifAndSound", "video"),
    ),
    RawValueEnum(
        "SettingsWidgetAlertsCheerBitsAlertOperator",
        SettingsWidgetAlertsCheerBitsAlertOperator.serializer(),
        SettingsWidgetAlertsCheerBitsAlertOperator.entries,
        listOf("=", ">="),
        unknownDecodesTo = SettingsWidgetAlertsCheerBitsAlertOperator.equal,
    ),
    RawValueEnum(
        "SettingsWidgetAlertsChatBotCommandImageType",
        SettingsWidgetAlertsChatBotCommandImageType.serializer(),
        SettingsWidgetAlertsChatBotCommandImageType.entries,
        listOf("File"),
    ),
    RawValueEnum(
        "SettingsSceneSwitchTransition",
        SettingsSceneSwitchTransition.serializer(),
        SettingsSceneSwitchTransition.entries,
        listOf("Blur", "Freeze", "Blur & zoom"),
    ),
    RawValueEnum(
        "SettingsWidgetVTuberType",
        SettingsWidgetVTuberType.serializer(),
        SettingsWidgetVTuberType.entries,
        listOf("VRM", "Live2D"),
    ),
    RawValueEnum("PomodoroPhase", PomodoroPhase.serializer(), PomodoroPhase.entries, listOf("Focus", "Break")),
    RawValueEnum(
        "PomodoroFocusIcon",
        PomodoroFocusIcon.serializer(),
        PomodoroFocusIcon.entries,
        listOf(
            "sun.max", "bolt.circle", "graduationcap", "text.book.closed", "arrowtriangle.right.circle",
            "brain.head.profile", "pencil", "flame", "timer", "target",
        ),
    ),
    RawValueEnum(
        "PomodoroBreakIcon",
        PomodoroBreakIcon.serializer(),
        PomodoroBreakIcon.entries,
        listOf("cup.and.saucer", "figure.dance", "figure.walk", "music.note", "leaf", "gamecontroller", "fork.knife"),
    ),
    RawValueEnum(
        "SettingsSceneCameraPosition",
        SettingsSceneCameraPosition.serializer(),
        SettingsSceneCameraPosition.entries,
        listOf(
            "Back", "Front", "RTMP", "External", "SRT(LA)", "SRT client", "RIST", "RTSP", "WHIP", "WHEP",
            "Media player", "Screen capture", "Back triple", "Back dual", "Back wide dual", "None",
        ),
        unknownDecodesTo = SettingsSceneCameraPosition.back,
    ),
    RawValueEnum(
        "SettingsWidgetScoreboardSport",
        SettingsWidgetScoreboardSport.serializer(),
        SettingsWidgetScoreboardSport.entries,
        listOf(
            "generic", "padel", "golf", "golfFullScorecard", "basketball", "generic2", "genericSets", "hockey",
            "football", "tennis", "volleyball",
        ),
    ),
    RawValueEnum(
        "SettingsWidgetPadelScoreboardGameType",
        SettingsWidgetPadelScoreboardGameType.serializer(),
        SettingsWidgetPadelScoreboardGameType.entries,
        listOf("Double", "Single"),
    ),
    RawValueEnum(
        "SettingsWidgetType",
        SettingsWidgetType.serializer(),
        SettingsWidgetType.entries,
        listOf(
            "Text", "Browser", "Video source", "Image", "Alerts", "Map", "Snapshot", "Chat", "Chat emote combo",
            "Scene", "Slideshow", "VTuber", "PNGTuber", "QR code", "Scoreboard", "Wheel of luck", "Bingo card",
            "Crop", "Pomodoro timer",
        ),
    ),
    RawValueEnum(
        "SettingsGraphicsImplementation",
        SettingsGraphicsImplementation.serializer(),
        SettingsGraphicsImplementation.entries,
        listOf("coreImage", "metalPetal"),
    ),
)

private fun uuid(text: String): UUID = UUID.fromString(text)

private const val id1 = "E621E1F8-C36C-495A-93FC-0C247A3E6E5F"
private const val id2 = "11111111-2222-3333-4444-555555555555"
private const val id3 = "AAAAAAAA-BBBB-CCCC-DDDD-EEEEEEEEEEEE"

@RunWith(RobolectricTestRunner::class)
class SceneCodableSuite {
    private val cases: List<Case<*>> = listOf(
        Case("SettingsVideoEffectRemoveBackground", SettingsVideoEffectRemoveBackground.serializer(), {
            SettingsVideoEffectRemoveBackground()
        }),
        Case("SettingsVideoEffectShape", SettingsVideoEffectShape.serializer(), { SettingsVideoEffectShape() }),
        Case("SettingsVideoEffectDewarp360", SettingsVideoEffectDewarp360.serializer(), {
            SettingsVideoEffectDewarp360()
        }),
        Case("SettingsVideoEffectAnamorphicLens", SettingsVideoEffectAnamorphicLens.serializer(), {
            SettingsVideoEffectAnamorphicLens()
        }),
        Case(
            "SettingsVideoEffectLut",
            SettingsVideoEffectLut.serializer(),
            { SettingsVideoEffectLut() },
            fromEmpty = { SettingsVideoEffectLut(lut = UUID.randomUUID()) },
        ),
        Case("SettingsVideoEffectOpacity", SettingsVideoEffectOpacity.serializer(), { SettingsVideoEffectOpacity() }),
        Case("SettingsVideoEffectMaskEffectPoint", SettingsVideoEffectMaskEffectPoint.serializer(), {
            SettingsVideoEffectMaskEffectPoint()
        }),
        Case("SettingsVideoEffectMask", SettingsVideoEffectMask.serializer(), { SettingsVideoEffectMask() }),
        Case("SettingsVideoEffect", SettingsVideoEffect.serializer(), { SettingsVideoEffect() }),
        Case("SettingsWidgetTextTimer", SettingsWidgetTextTimer.serializer(), { SettingsWidgetTextTimer() }),
        Case("SettingsWidgetTextStopwatch", SettingsWidgetTextStopwatch.serializer(), {
            SettingsWidgetTextStopwatch()
        }),
        Case("SettingsWidgetTextSubtitles", SettingsWidgetTextSubtitles.serializer(), {
            SettingsWidgetTextSubtitles()
        }),
        Case("SettingsWidgetTextCheckbox", SettingsWidgetTextCheckbox.serializer(), {
            SettingsWidgetTextCheckbox()
        }, synthesized = true),
        Case("SettingsWidgetTextRating", SettingsWidgetTextRating.serializer(), {
            SettingsWidgetTextRating()
        }, synthesized = true),
        Case("SettingsWidgetTextLapTimes", SettingsWidgetTextLapTimes.serializer(), {
            SettingsWidgetTextLapTimes()
        }, synthesized = true),
        Case("SettingsWidgetText", SettingsWidgetText.serializer(), { SettingsWidgetText() }),
        Case("SettingsWidgetCrop", SettingsWidgetCrop.serializer(), { SettingsWidgetCrop() }, synthesized = true),
        Case("SettingsWidgetBrowser", SettingsWidgetBrowser.serializer(), { SettingsWidgetBrowser() }),
        Case("SettingsWidgetMap", SettingsWidgetMap.serializer(), { SettingsWidgetMap() }),
        Case("SettingsWidgetScene", SettingsWidgetScene.serializer(), { SettingsWidgetScene() }, synthesized = true),
        Case("SettingsWidgetQrCode", SettingsWidgetQrCode.serializer(), { SettingsWidgetQrCode() }, synthesized = true),
        Case("SettingsWidgetAlertFacePosition", SettingsWidgetAlertFacePosition.serializer(), {
            SettingsWidgetAlertFacePosition()
        }, synthesized = true),
        Case("SettingsWidgetAlertsAlert", SettingsWidgetAlertsAlert.serializer(), { SettingsWidgetAlertsAlert() }),
        Case("SettingsWidgetAlertsCheerBitsAlert", SettingsWidgetAlertsCheerBitsAlert.serializer(), {
            SettingsWidgetAlertsCheerBitsAlert()
        }, synthesized = true),
        Case("SettingsWidgetAlertsKickGiftsAlert", SettingsWidgetAlertsKickGiftsAlert.serializer(), {
            SettingsWidgetAlertsKickGiftsAlert()
        }, synthesized = true),
        Case("SettingsWidgetAlertsTwitch", SettingsWidgetAlertsTwitch.serializer(), { SettingsWidgetAlertsTwitch() }),
        Case("SettingsWidgetAlertsKick", SettingsWidgetAlertsKick.serializer(), { SettingsWidgetAlertsKick() }),
        Case("SettingsWidgetAlertsChatBotCommand", SettingsWidgetAlertsChatBotCommand.serializer(), {
            SettingsWidgetAlertsChatBotCommand()
        }),
        Case("SettingsWidgetAlertsChatBot", SettingsWidgetAlertsChatBot.serializer(), {
            SettingsWidgetAlertsChatBot()
        }),
        Case("SettingsWidgetAlertsSpeechToTextString", SettingsWidgetAlertsSpeechToTextString.serializer(), {
            SettingsWidgetAlertsSpeechToTextString()
        }, synthesized = true),
        Case("SettingsWidgetAlertsSpeechToText", SettingsWidgetAlertsSpeechToText.serializer(), {
            SettingsWidgetAlertsSpeechToText()
        }),
        Case("SettingsTtsMonster", SettingsTtsMonster.serializer(), { SettingsTtsMonster() }),
        Case("SettingsWidgetAlerts", SettingsWidgetAlerts.serializer(), { SettingsWidgetAlerts() }),
        Case("SettingsSensitivity", SettingsSensitivity.serializer(), { SettingsSensitivity() }, synthesized = true),
        Case("SettingsWidgetVTuber", SettingsWidgetVTuber.serializer(), { SettingsWidgetVTuber() }),
        Case("SettingsWidgetPngTuber", SettingsWidgetPngTuber.serializer(), { SettingsWidgetPngTuber() }),
        Case("SettingsWidgetSnapshot", SettingsWidgetSnapshot.serializer(), { SettingsWidgetSnapshot() }),
        Case(
            "SettingsWidgetChat",
            SettingsWidgetChat.serializer(),
            { SettingsWidgetChat() },
            fromEmpty = { SettingsWidgetChat(displayStyle = SettingsChatDisplayStyle.internationalName) },
        ),
        Case(
            "SettingsWidgetSlideshowSlide",
            SettingsWidgetSlideshowSlide.serializer(),
            { SettingsWidgetSlideshowSlide(widgetId = UUID.randomUUID()) },
            fromEmpty = { SettingsWidgetSlideshowSlide(widgetId = UUID.randomUUID(), time = 0) },
        ),
        Case("SettingsWidgetSlideshow", SettingsWidgetSlideshow.serializer(), { SettingsWidgetSlideshow() }),
        Case("SettingsWidgetWheelOfLuckOption", SettingsWidgetWheelOfLuckOption.serializer(), {
            SettingsWidgetWheelOfLuckOption()
        }),
        Case("SettingsWidgetWheelOfLuck", SettingsWidgetWheelOfLuck.serializer(), { SettingsWidgetWheelOfLuck() }),
        Case("SettingsBingoCardSquare", SettingsBingoCardSquare.serializer(), {
            SettingsBingoCardSquare(text = "", checked = false)
        }, synthesized = true),
        Case("SettingsWidgetBingoCard", SettingsWidgetBingoCard.serializer(), { SettingsWidgetBingoCard() }),
        Case("SettingsWidgetPomodoroTimer", SettingsWidgetPomodoroTimer.serializer(), {
            SettingsWidgetPomodoroTimer()
        }),
        Case("SettingsWidgetChatEmoteCombo", SettingsWidgetChatEmoteCombo.serializer(), {
            SettingsWidgetChatEmoteCombo()
        }),
        Case("SettingsWidget", SettingsWidget.serializer(), { SettingsWidget() }),
        Case(
            "SettingsSceneWidget",
            SettingsSceneWidget.serializer(),
            { SettingsSceneWidget() },
            fromEmpty = { SettingsSceneWidget(migrated = false, migrated2 = false) },
        ),
        Case("SettingsWidgetVideoSource", SettingsWidgetVideoSource.serializer(), { SettingsWidgetVideoSource() }),
        Case("SettingsWidgetScoreboardPlayer", SettingsWidgetScoreboardPlayer.serializer(), {
            SettingsWidgetScoreboardPlayer()
        }),
        Case("SettingsWidgetScoreboardScore", SettingsWidgetScoreboardScore.serializer(), {
            SettingsWidgetScoreboardScore()
        }, synthesized = true),
        Case("SettingsWidgetPadelScoreboard", SettingsWidgetPadelScoreboard.serializer(), {
            SettingsWidgetPadelScoreboard()
        }),
        Case("SettingsWidgetGolfScoreboardPlayer", SettingsWidgetGolfScoreboardPlayer.serializer(), {
            SettingsWidgetGolfScoreboardPlayer()
        }),
        Case("SettingsWidgetGolfScoreboard", SettingsWidgetGolfScoreboard.serializer(), {
            SettingsWidgetGolfScoreboard()
        }),
        Case("SettingsWidgetGenericScoreboard", SettingsWidgetGenericScoreboard.serializer(), {
            SettingsWidgetGenericScoreboard()
        }),
        Case("SettingsWidgetModularScoreboardTeam", SettingsWidgetModularScoreboardTeam.serializer(), {
            SettingsWidgetModularScoreboardTeam()
        }),
        Case("SettingsWidgetScoreboardClock", SettingsWidgetScoreboardClock.serializer(), {
            SettingsWidgetScoreboardClock()
        }),
        Case("SettingsWidgetModularScoreboard", SettingsWidgetModularScoreboard.serializer(), {
            SettingsWidgetModularScoreboard()
        }),
        Case("SettingsWidgetScoreboard", SettingsWidgetScoreboard.serializer(), { SettingsWidgetScoreboard() }),
        Case(
            "SettingsScene",
            SettingsScene.serializer(),
            { SettingsScene() },
            fromEmpty = {
                val scene = SettingsScene()
                scene.videoSource.cameraPosition = defaultBackCameraPosition
                scene
            },
        ),
        Case("SettingsAutoSceneSwitcherScene", SettingsAutoSceneSwitcherScene.serializer(), {
            SettingsAutoSceneSwitcherScene()
        }),
        Case("SettingsAutoSceneSwitcher", SettingsAutoSceneSwitcher.serializer(), { SettingsAutoSceneSwitcher() }),
        Case("SettingsAutoSceneSwitchers", SettingsAutoSceneSwitchers.serializer(), {
            SettingsAutoSceneSwitchers()
        }),
    )

    private fun checkAll(check: (Case<*>) -> Unit) {
        val failures = mutableListOf<String>()
        for (case in cases) {
            try {
                check(case)
            } catch (error: Throwable) {
                failures.add("${case.name}: ${error.message}")
            }
        }
        if (failures.isNotEmpty()) {
            fail(failures.joinToString("\n"))
        }
    }

    private fun parse(json: String): JsonObject = codableJson.parseToJsonElement(json).jsonObject

    @Test
    fun everyClassIsCovered() {
        assertEquals(63, cases.size)
        assertEquals(cases.size, cases.map { it.name }.toSet().size)
        assertEquals(swiftEncodedKeys.keys, cases.map { it.name }.toSet())
        assertEquals(22, swiftRawValueEnums.size)
    }

    @Test
    fun encodedKeysMatchSwiftEncodeOrder() {
        checkAll { assertEquals(swiftEncodedKeys[it.name], it.encodedKeys(), "${it.name} keys") }
    }

    @Test
    fun swiftStyleJsonImports() {
        checkAll { it.checkSwiftStyleImport() }
    }

    @Test
    fun rawValueEnumsMatchSwift() {
        val failures = mutableListOf<String>()
        for (rawValueEnum in swiftRawValueEnums) {
            try {
                rawValueEnum.check()
            } catch (error: Throwable) {
                failures.add("${rawValueEnum.name}: ${error.message}")
            }
        }
        if (failures.isNotEmpty()) {
            fail(failures.joinToString("\n"))
        }
    }

    private fun <T> assertSwiftJson(serializer: KSerializer<T>, value: T, swiftJson: String) {
        val kotlinJson = swiftStyle(codableJson.parseToJsonElement(codableJson.encodeToString(serializer, value)))
        assertEquals(codableJson.parseToJsonElement(swiftJson), kotlinJson)
        val decoded = codableJson.decodeFromString(serializer, swiftJson)
        assertEquals(kotlinJson, swiftStyle(codableJson.parseToJsonElement(codableJson.encodeToString(serializer, decoded))))
    }

    @Test
    fun defaultsEncodeLikeSwift() {
        assertSwiftJson(
            SettingsSceneWidget.serializer(),
            SettingsSceneWidget(widgetId = uuid(id1), id = uuid(id2)),
            """{"widgetId":"$id1","id":"$id2","x":0,"y":0,"width":100,"height":100,"size":100,
               "alignment":"TopLeft","positioningLock":false,"migrated":true,"migrated2":true}""",
        )
        assertSwiftJson(
            SettingsWidgetBrowser.serializer(),
            SettingsWidgetBrowser(),
            """{"url":"","width":500,"height":500,"mode":"periodicAudioAndVideo","fps":5,"styleSheet":"",
               "moblinAccess":false,"speechToText":false,"localOnly":false}""",
        )
        assertSwiftJson(
            SettingsVideoEffectShape.serializer(),
            SettingsVideoEffectShape(),
            """{"cornerRadius":0.1,"borderWidth":0,"borderColor":{"red":0,"green":0,"blue":0},"cropEnabled":false,
               "cropX":0.25,"cropY":0,"cropWidth":0.5,"cropHeight":1}""",
        )
        val name = SettingsWidgetModularScoreboard.baseName
        val title = SettingsWidgetModularScoreboard.baseTitle
        assertSwiftJson(
            SettingsWidgetModularScoreboard.serializer(),
            SettingsWidgetModularScoreboard(),
            """{"home":{"name":"$name","textColor":{"red":255,"green":255,"blue":255},
                        "backgroundColor":{"red":11,"green":16,"blue":172}},
               "away":{"name":"$name","textColor":{"red":255,"green":255,"blue":255},
                        "backgroundColor":{"red":220,"green":38,"blue":38}},
               "title":"$title","period":"1","infoBoxText":"","clock":{"maximum":45,"direction":{"up":{}}},
               "layout":{"stacked":{}},"width":350,"rowHeight":45,"isBold":true,"showTitle":false,
               "showMoreStats":false,"showGlobalStatsBlock":false,"showClock":true}""",
        )
        assertSwiftJson(
            SettingsWidgetPomodoroTimer.serializer(),
            SettingsWidgetPomodoroTimer(),
            """{"focusDuration":30,"breakDuration":5,"width":1.6,"focusName":"Focus","breakName":"Break",
               "focusIcon":"sun.max","breakIcon":"cup.and.saucer",
               "backgroundColor":{"red":0,"green":0,"blue":0,"opacity":0.75},
               "foregroundColor":{"red":255,"green":255,"blue":255},"focusColor":{"red":122,"green":181,"blue":255},
               "breakColor":{"red":103,"green":208,"blue":69},"focusToBreakSoundId":null,"breakToFocusSoundId":null,
               "focusToBreakChatMessage":"","breakToFocusChatMessage":""}""",
        )
        assertSwiftJson(
            SettingsAutoSceneSwitcherScene.serializer(),
            SettingsAutoSceneSwitcherScene(id = uuid(id1)),
            """{"id":"$id1","sceneId":null,"time":15}""",
        )
        assertSwiftJson(
            SettingsWidgetGolfScoreboardPlayer.serializer(),
            SettingsWidgetGolfScoreboardPlayer(name = "P", id = uuid(id1)),
            """{"id":"$id1","name":"P","scores":[-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1],
               "color":{"red":255,"green":255,"blue":255}}""",
        )
        assertSwiftJson(
            SettingsWidgetAlertsCheerBitsAlert.serializer(),
            SettingsWidgetAlertsCheerBitsAlert(
                id = uuid(id1),
                alert = SettingsWidgetAlertsAlert(id = uuid(id2), imageId = uuid(id3), soundId = uuid(id1)),
            ),
            """{"id":"$id1","bits":1,"comparisonOperator":">=",
               "alert":{"id":"$id2","enabled":true,"mediaType":"gifAndSound","imageId":"$id3","imageLoopCount":1,
                        "soundId":"$id1","videoName":"","textColor":{"red":255,"green":255,"blue":255},
                        "accentColor":{"red":253,"green":251,"blue":103},"fontSize":45,"fontDesign":"Monospaced",
                        "fontWeight":"Bold","textToSpeechEnabled":true,"textToSpeechDelay":1.5,
                        "textToSpeechLanguageVoices":{},"positionType":"Scene",
                        "facePosition":{"x":0.25,"y":0.25,"width":0.5,"height":0.5}}}""",
        )
    }

    @Test
    fun cloneCopiesSwiftValueTypes() {
        val scene = SettingsScene()
        scene.videoSource.cameraPosition = SettingsSceneCameraPosition.front
        scene.widgets = mutableListOf(SettingsSceneWidget(widgetId = uuid(id1)))
        val clone = scene.clone()
        clone.videoSource.cameraPosition = SettingsSceneCameraPosition.rtmp
        clone.widgets[0].layout.x = 50.0
        assertEquals(SettingsSceneCameraPosition.front, scene.videoSource.cameraPosition)
        assertEquals(0.0, scene.widgets[0].layout.x)
        assertEquals(uuid(id1), clone.widgets[0].widgetId)
    }

    private fun observedReads(read: () -> Any?): Set<Any> {
        val reads = mutableSetOf<Any>()
        Snapshot.observe(readObserver = { reads.add(it) }) { read() }
        return reads
    }

    private fun appliedWrites(write: () -> Unit): Set<Any> {
        val writes = mutableSetOf<Any>()
        val handle = Snapshot.registerApplyObserver { changed, _ -> writes.addAll(changed) }
        try {
            Snapshot.sendApplyNotifications()
            writes.clear()
            write()
            Snapshot.sendApplyNotifications()
        } finally {
            handle.dispose()
        }
        return writes
    }

    @Test
    fun publishedPropertiesAreObserved() {
        val scene = SettingsScene()
        val widget = SettingsWidget()
        val text = SettingsWidgetText()
        val browser = SettingsWidgetBrowser()
        val alert = SettingsWidgetAlertsAlert()
        val pomodoro = SettingsWidgetPomodoroTimer()
        val modular = SettingsWidgetModularScoreboard()
        val switcher = SettingsAutoSceneSwitcher()
        val effect = SettingsVideoEffect()
        val sceneWidget = SettingsSceneWidget()
        val videoSource = SettingsWidgetVideoSource()
        val reads: Map<String, () -> Any?> = mapOf(
            "SettingsScene.name" to { scene.name },
            "SettingsScene.enabled" to { scene.enabled },
            "SettingsScene.widgets" to { scene.widgets.size },
            "SettingsWidget.enabled" to { widget.enabled },
            "SettingsWidget.effects" to { widget.effects },
            "SettingsWidgetText.formatString" to { text.formatString },
            "SettingsWidgetText.timers" to { text.timers },
            "SettingsWidgetBrowser.url" to { browser.url },
            "SettingsWidgetAlertsAlert.textToSpeechLanguageVoices" to { alert.textToSpeechLanguageVoices },
            "SettingsWidgetPomodoroTimer.secondsRemaining" to { pomodoro.secondsRemaining },
            "SettingsWidgetModularScoreboard.config" to { modular.config },
            "SettingsAutoSceneSwitcher.scenes" to { switcher.scenes },
            "SettingsVideoEffect.enabled" to { effect.enabled },
            "SettingsSceneWidget.layout" to { sceneWidget.layout },
            "SettingsWidgetVideoSource.videoSource" to { videoSource.videoSource },
        )
        for ((name, read) in reads) {
            assertTrue(observedReads(read).isNotEmpty(), "$name read is not observed")
        }
        val widgetsReads = observedReads { scene.widgets.size }
        val widgetsWrites = appliedWrites { scene.widgets.add(SettingsSceneWidget(widgetId = uuid(id1))) }
        assertTrue(widgetsReads.any { it in widgetsWrites }, "in-place widgets mutation is not observed")
        assertEquals(listOf(uuid(id1)), scene.widgets.map { it.widgetId })
        val enabledReads = observedReads { scene.enabled }
        assertTrue(enabledReads.any { it in appliedWrites { scene.enabled = false } }, "enabled write is not observed")
        val timersReads = observedReads { text.timers }
        val timersWrites = appliedWrites { text.timers = text.timers + SettingsWidgetTextTimer() }
        assertTrue(timersReads.any { it in timersWrites }, "timers reassignment is not observed")
        val videoSourceReads = observedReads { videoSource.videoSource.cameraPosition }
        val videoSourceWrites = appliedWrites { videoSource.updateCameraId(SettingsCameraId.ScreenCapture) }
        assertTrue(videoSourceReads.any { it in videoSourceWrites }, "camera change is not observed")
        assertEquals(SettingsSceneCameraPosition.screenCapture, videoSource.videoSource.cameraPosition)
    }

    @Test
    fun defaultInstancesRoundTrip() {
        checkAll { it.checkRoundTrip() }
    }

    @Test
    fun emptyObjectDecodesLikeSwift() {
        checkAll { it.checkEmptyObject() }
    }

    @Test
    fun widgetTypeRawValuesAreUnchanged() {
        val expected = listOf(
            "Text", "Browser", "Video source", "Image", "Alerts", "Map", "Snapshot", "Chat",
            "Chat emote combo", "Scene", "Slideshow", "VTuber", "PNGTuber", "QR code", "Scoreboard",
            "Wheel of luck", "Bingo card", "Crop", "Pomodoro timer",
        )
        assertEquals(expected, SettingsWidgetType.entries.map { it.rawValue })
        for (type in SettingsWidgetType.entries) {
            assertEquals("\"${type.rawValue}\"", codableJson.encodeToString(SettingsWidgetType.serializer(), type))
            assertEquals(type, codableJson.decodeFromString(SettingsWidgetType.serializer(), "\"${type.rawValue}\""))
        }
    }

    @Test
    fun swiftSpecificJsonFormats() {
        assertEquals(
            "{\"stacked\":{}}",
            codableJson.encodeToString(SettingsWidgetScoreboardLayout.serializer(), SettingsWidgetScoreboardLayout.stacked),
        )
        assertEquals(
            "{\"down\":{}}",
            codableJson.encodeToString(
                SettingsWidgetGenericScoreboardClockDirection.serializer(),
                SettingsWidgetGenericScoreboardClockDirection.down,
            ),
        )
        assertEquals(
            "\"SRT(LA)\"",
            codableJson.encodeToString(SettingsSceneCameraPosition.serializer(), SettingsSceneCameraPosition.srtla),
        )
        assertEquals(
            "\">=\"",
            codableJson.encodeToString(
                SettingsWidgetAlertsCheerBitsAlertOperator.serializer(),
                SettingsWidgetAlertsCheerBitsAlertOperator.greaterEqual,
            ),
        )
        assertEquals("{}", codableJson.encodeToString(SettingsWidgetTextSubtitles.serializer(), SettingsWidgetTextSubtitles()))
        val lapTimes = SettingsWidgetTextLapTimes(id = uuid(id1), lapTimes = listOf(1.5))
        assertEquals(
            "{\"id\":\"$id1\",\"lapTimes\":[1.5]}",
            codableJson.encodeToString(SettingsWidgetTextLapTimes.serializer(), lapTimes),
        )
        assertEquals("{\"lut\":null}", codableJson.encodeToString(SettingsVideoEffectLut.serializer(), SettingsVideoEffectLut()))
        val text = parse(codableJson.encodeToString(SettingsWidgetText.serializer(), SettingsWidgetText()))
        assertEquals("null", text["fontFamily"].toString())
        val map = parse(codableJson.encodeToString(SettingsWidgetMap.serializer(), SettingsWidgetMap(size = 42.0)))
        assertEquals("42.0", map["scale"].toString())
        assertNull(map["size"])
        val golf = parse(codableJson.encodeToString(SettingsWidgetGolfScoreboard.serializer(), SettingsWidgetGolfScoreboard()))
        assertEquals("\"⛳ Masters 2026\"", golf["eventName"].toString())
        val scoreboard = parse(codableJson.encodeToString(SettingsWidgetScoreboard.serializer(), SettingsWidgetScoreboard()))
        assertEquals("\"generic\"", scoreboard["type"].toString())
        val browser = parse(codableJson.encodeToString(SettingsWidgetBrowser.serializer(), SettingsWidgetBrowser()))
        assertEquals("5.0", browser["fps"].toString())
        val sceneWidget = SettingsSceneWidget(widgetId = uuid(id1), id = uuid(id2), width2 = 12.0)
        assertEquals(
            listOf(
                "widgetId", "id", "x", "y", "width", "height", "size", "alignment", "positioningLock",
                "migrated", "migrated2",
            ),
            parse(codableJson.encodeToString(SettingsSceneWidget.serializer(), sceneWidget)).keys.toList(),
        )
        val scene = parse(codableJson.encodeToString(SettingsScene.serializer(), SettingsScene(id = uuid(id1))))
        assertEquals("\"$id1\"", scene["id"].toString())
        assertEquals("\"None\"", scene["cameraPosition"].toString())
        assertEquals("null", scene["quickSwitchGroup"].toString())
        assertNull(scene["videoSource"])
        val alerts = parse(codableJson.encodeToString(SettingsWidgetAlerts.serializer(), SettingsWidgetAlerts()))
        assertNull(alerts["quickButton"])
        val chat = parse(codableJson.encodeToString(SettingsWidgetChat.serializer(), SettingsWidgetChat()))
        assertNull(chat["nicknames"])
        assertEquals("\"internationalNameAndUsername\"", chat["displayStyle"].toString())
    }

    @Test
    fun sceneSample() {
        val json = """
            {"name":"Main","id":"$id1","enabled":false,"cameraPosition":"RTMP","rtmpCameraId":"$id2",
             "externalCameraId":"ext","externalCameraName":"USB camera",
             "widgets":[{"widgetId":"$id3","id":"$id2","x":10.5,"y":20,"width":30,"height":40,
                         "alignment":"BottomRight","positioningLock":true,"migrated":true},
                        {"widgetId":"$id1","size":12.5}],
             "videoSourceRotation":90,"videoStabilizationMode":"Cinematic","overrideVideoStabilizationMode":true,
             "fillFrame":true,"overrideMic":true,"micId":"mic","quickSwitchGroup":2,"mirror":true,
             "backgroundColor":{"red":1,"green":2,"blue":3}}
        """
        val scene = codableJson.decodeFromString(SettingsScene.serializer(), json)
        assertEquals("Main", scene.name)
        assertEquals(uuid(id1), scene.id)
        assertFalse(scene.enabled)
        assertEquals(SettingsSceneCameraPosition.rtmp, scene.videoSource.cameraPosition)
        assertEquals(uuid(id2), scene.videoSource.rtmpCameraId)
        assertEquals("ext", scene.videoSource.externalCameraId)
        assertEquals("USB camera", scene.videoSource.externalCameraName)
        assertEquals(SettingsCameraId.Rtmp(uuid(id2)), scene.toCameraId())
        assertEquals(2, scene.widgets.size)
        val first = scene.widgets[0]
        assertEquals(uuid(id3), first.widgetId)
        assertEquals(uuid(id2), first.id)
        assertEquals(10.5, first.layout.x)
        assertEquals("10.5", first.layout.xString)
        assertEquals(20.0, first.layout.y)
        assertEquals(30.0, first.width2)
        assertEquals(40.0, first.height2)
        assertEquals(30.0, first.layout.size)
        assertEquals("30.0", first.layout.sizeString)
        assertEquals(SettingsAlignment.bottomRight, first.layout.alignment)
        assertTrue(first.layout.positioningLock)
        assertTrue(first.migrated)
        assertFalse(first.migrated2)
        assertEquals(12.5, scene.widgets[1].layout.size)
        assertEquals(90.0, scene.videoSourceRotation)
        assertEquals(SettingsVideoStabilizationMode.cinematic, scene.videoStabilizationMode)
        assertTrue(scene.overrideVideoStabilizationMode)
        assertTrue(scene.fillFrame)
        assertTrue(scene.overrideMic)
        assertEquals("mic", scene.micId)
        assertEquals(2, scene.quickSwitchGroup)
        assertTrue(scene.mirror)
        assertEquals(RgbColor(red = 1, green = 2, blue = 3), scene.backgroundColor)
    }

    @Test
    fun scoreboardWidgetSample() {
        val json = """
            {"name":"Score","id":"$id1","type":"Scoreboard","enabled":false,
             "scoreboard":{"type":"padel","textColor":{"red":9,"green":8,"blue":7},
               "padel":{"type":"Single","homePlayer1":"$id2","score":[{"home":3,"away":2},{"home":1,"away":0}]},
               "golf":{"eventName":"Open","pars":[3,3],"players":[{"id":"$id3","name":"Tiger","scores":[1,2]}]},
               "generic":{"home":"H","clock":{"maximum":10,"direction":{"down":{}}}},
               "modular":{"layout":{"sideBySide":{}},"clock":{"maximum":20,"direction":{"down":{}}},
                          "home":{"name":"A","textColor":{"red":1,"green":1,"blue":1},
                                  "backgroundColor":{"red":2,"green":2,"blue":2}},"width":400}},
             "effects":[{"id":"$id2","type":"mask","enabled":false,
                         "mask":{"points":[{"x":1,"y":2}],"backgroundType":"Checkerboard","inverted":true}},
                        {"type":"dewarp360","dewarp360":{"pan":10,"zoom":2}}]}
        """
        val widget = codableJson.decodeFromString(SettingsWidget.serializer(), json)
        assertEquals("Score", widget.name)
        assertEquals(uuid(id1), widget.id)
        assertEquals(SettingsWidgetType.scoreboard, widget.type)
        assertFalse(widget.enabled)
        val scoreboard = widget.scoreboard
        assertEquals(SettingsWidgetScoreboardSport.padel, scoreboard.sport)
        assertEquals(RgbColor(red = 9, green = 8, blue = 7), scoreboard.textColor)
        assertEquals(SettingsWidgetPadelScoreboardGameType.singles, scoreboard.padel.type)
        assertEquals(uuid(id2), scoreboard.padel.homePlayer1)
        assertEquals(listOf(3 to 2, 1 to 0), scoreboard.padel.score.map { it.home to it.away })
        assertEquals("Open", scoreboard.golf.title)
        assertEquals(listOf(3, 3, 3, 4, 5, 4, 3, 4, 4, 4, 4, 3, 5, 4, 4, 3, 4, 5), scoreboard.golf.pars)
        assertEquals(1, scoreboard.golf.players.size)
        assertEquals("Tiger", scoreboard.golf.players[0].name)
        assertEquals(listOf(1, 2), scoreboard.golf.players[0].scores)
        assertEquals(RgbColor.white, scoreboard.golf.players[0].color)
        assertEquals("H", scoreboard.generic.home)
        assertEquals(SettingsWidgetGenericScoreboard.baseName, scoreboard.generic.away)
        assertEquals(SettingsWidgetGenericScoreboardClockDirection.down, scoreboard.generic.clock.direction)
        assertEquals(10, scoreboard.generic.clock.minutes)
        assertEquals(SettingsWidgetScoreboardLayout.sideBySide, scoreboard.modular.layout)
        assertEquals(20, scoreboard.modular.clock.maximum)
        assertEquals(20, scoreboard.modular.clock.minutes)
        assertEquals("A", scoreboard.modular.home.name)
        assertEquals(RgbColor(red = 2, green = 2, blue = 2), scoreboard.modular.home.backgroundColor)
        assertEquals(SettingsWidgetModularScoreboard.baseName, scoreboard.modular.away.name)
        assertEquals(400f, scoreboard.modular.width)
        assertEquals(2, widget.effects.size)
        val mask = widget.effects[0]
        assertEquals(uuid(id2), mask.id)
        assertEquals(SettingsVideoEffectType.mask, mask.type)
        assertFalse(mask.enabled)
        assertEquals(listOf(SettingsVideoEffectMaskEffectPoint(x = 1.0, y = 2.0)), mask.mask.points)
        assertEquals(SettingsMaskBackgroundType.checkerboard, mask.mask.backgroundType)
        assertTrue(mask.mask.inverted)
        val dewarp = widget.effects[1].dewarp360
        assertEquals(10f, dewarp.pan)
        assertEquals(2f, dewarp.zoom)
        assertNotEquals(90f, dewarp.inverseFieldOfView)
        val encoded = parse(codableJson.encodeToString(SettingsWidget.serializer(), widget))
        assertEquals(
            "{\"sideBySide\":{}}",
            encoded["scoreboard"]!!.jsonObject["modular"]!!.jsonObject["layout"].toString(),
        )
    }

    @Test
    fun alertSampleWithLegacyVoices() {
        val json = """
            {"id":"$id1","enabled":false,"mediaType":"video","imageId":"$id2","imageLoopCount":3,
             "videoName":"clip.mp4","fontSize":60,"fontDesign":"Serif","fontWeight":"Light",
             "textToSpeechEnabled":false,"textToSpeechDelay":2.5,
             "textToSpeechLanguageVoices":{"en":"voice-en","sv":"voice-sv"},
             "positionType":"Face","facePosition":{"x":0.1,"y":0.2,"width":0.3,"height":0.4}}
        """
        val alert = codableJson.decodeFromString(SettingsWidgetAlertsAlert.serializer(), json)
        assertEquals(uuid(id1), alert.id)
        assertFalse(alert.enabled)
        assertEquals(SettingsWidgetAlertsAlertMediaType.video, alert.mediaType)
        assertEquals(uuid(id2), alert.imageId)
        assertEquals(3, alert.imageLoopCount)
        assertEquals("clip.mp4", alert.videoName)
        assertEquals(60, alert.fontSize)
        assertEquals(SettingsFontDesign.serif, alert.fontDesign)
        assertEquals(SettingsFontWeight.light, alert.fontWeight)
        assertFalse(alert.textToSpeechEnabled)
        assertEquals(2.5, alert.textToSpeechDelay)
        assertEquals(setOf("en", "sv"), alert.textToSpeechLanguageVoices.keys)
        assertEquals("voice-en", alert.textToSpeechLanguageVoices["en"]!!.apple.voice)
        assertEquals("voice-sv", alert.textToSpeechLanguageVoices["sv"]!!.apple.voice)
        assertEquals(SettingsWidgetAlertPositionType.face, alert.positionType)
        assertEquals(0.3, alert.facePosition.width)
        val reencoded = codableJson.decodeFromString(
            SettingsWidgetAlertsAlert.serializer(),
            codableJson.encodeToString(SettingsWidgetAlertsAlert.serializer(), alert),
        )
        assertEquals("voice-sv", reencoded.textToSpeechLanguageVoices["sv"]!!.apple.voice)
        val partialFace = codableJson.decodeFromString(
            SettingsWidgetAlertsAlert.serializer(),
            """{"facePosition":{"x":0.9}}""",
        )
        assertEquals(0.25, partialFace.facePosition.x)
    }

    @Test
    fun alertsSample() {
        val json = """
            {"twitch":{"cheerBits":[{"id":"$id1","bits":100,"comparisonOperator":"<","alert":{"enabled":false}},
                                    {"id":"$id2","bits":5,"comparisonOperator":">=","alert":{}}],
                       "follows":{"enabled":false}},
             "kick":{"kickGifts":[{"amount":5,"comparisonOperator":"=","alert":{}}]},
             "chatBot":{"commands":[{"name":"hello","imageType":"File"}]},
             "speechToText":{"strings":[{"id":"$id3","string":"wow","alert":{"videoName":"a.mov"}}]},
             "needsSubtitles":true,"aiEnabled":true,"ttsMonster":{"apiToken":"secret"}}
        """
        val alerts = codableJson.decodeFromString(SettingsWidgetAlerts.serializer(), json)
        assertEquals(2, alerts.twitch.cheerBits.size)
        assertEquals(uuid(id1), alerts.twitch.cheerBits[0].id)
        assertEquals(100, alerts.twitch.cheerBits[0].bits)
        assertEquals(SettingsWidgetAlertsCheerBitsAlertOperator.equal, alerts.twitch.cheerBits[0].comparisonOperator)
        assertFalse(alerts.twitch.cheerBits[0].alert.enabled)
        assertEquals(SettingsWidgetAlertsCheerBitsAlertOperator.greaterEqual, alerts.twitch.cheerBits[1].comparisonOperator)
        assertFalse(alerts.twitch.follows.enabled)
        assertEquals(1, alerts.kick.kickGifts.size)
        assertEquals(1, alerts.kick.kickGifts[0].amount)
        assertEquals("hello", alerts.chatBot.commands[0].name)
        assertEquals("wow", alerts.speechToText.strings[0].string)
        assertEquals("a.mov", alerts.speechToText.strings[0].alert.videoName)
        assertTrue(alerts.needsSubtitles)
        assertTrue(alerts.aiEnabled)
        assertEquals("secret", alerts.ttsMonster.apiToken)
        assertEquals("You are rude and gives insulting answers. Answer in a few sentences.", alerts.ai.personality)
    }

    @Test
    fun textWidgetSample() {
        val json = """
            {"formatString":"{time}","fontFamily":"Arial","fontSize":44,"fontDesign":"Rounded",
             "horizontalAlignment":"Trailing","verticalAlignment":"Bottom","backgroundColor":{"red":5,"green":6,"blue":7,"opacity":0.5},
             "timers":[{"delta":10}],"stopwatches":[{"id":"$id1","totalElapsed":12.5,"running":true}],
             "subtitles":[{"identifier":"en"},{}],"checkboxes":[{"id":"$id2","checked":true}],
             "ratings":[{"id":"$id3","rating":4}],"lapTimes":[{"id":"$id1","currentLapStartTime":3.5,"lapTimes":[1.5,2.5]}],
             "widthEnabled":true,"width":150,"cornerRadius":3}
        """
        val text = codableJson.decodeFromString(SettingsWidgetText.serializer(), json)
        assertEquals("{time}", text.formatString)
        assertEquals("Arial", text.fontFamily)
        assertEquals(44, text.fontSize)
        assertEquals(44f, text.fontSizeFloat)
        assertEquals(SettingsFontDesign.rounded, text.fontDesign)
        assertEquals(SettingsHorizontalAlignment.trailing, text.horizontalAlignment)
        assertEquals(SettingsVerticalAlignment.bottom, text.verticalAlignment)
        assertEquals(RgbColor(red = 5, green = 6, blue = 7, opacity = 0.5), text.backgroundColor)
        assertEquals(10, text.timers[0].delta)
        assertEquals(uuid(id1), text.stopwatches[0].id)
        assertEquals(12.5, text.stopwatches[0].totalElapsed)
        assertTrue(text.stopwatches[0].running)
        assertEquals(listOf("en", null), text.subtitles.map { it.identifier })
        assertTrue(text.checkboxes[0].checked)
        assertEquals(4, text.ratings[0].rating)
        assertEquals(3.5, text.lapTimes[0].currentLapStartTime)
        assertEquals(listOf(1.5, 2.5), text.lapTimes[0].lapTimes)
        assertTrue(text.widthEnabled)
        assertEquals(150, text.width)
        assertEquals(3, text.cornerRadius)
        val strict = codableJson.decodeFromString(
            SettingsWidgetText.serializer(),
            """{"checkboxes":[{"id":"$id2","checked":true},{"checked":false}],"ratings":[{"id":"$id3","rating":"four"}]}""",
        )
        assertTrue(strict.checkboxes.isEmpty())
        assertTrue(strict.ratings.isEmpty())
    }

    @Test
    fun browserModeMigration() {
        val legacy = codableJson.decodeFromString(
            SettingsWidgetBrowser.serializer(),
            """{"url":"https://moblin","audioOnly":true,"fps":10,"width":800}""",
        )
        assertEquals("https://moblin", legacy.url)
        assertEquals(SettingsWidgetBrowserMode.audioAndVideoOnly, legacy.mode)
        assertEquals(10f, legacy.baseFps)
        assertEquals(800, legacy.width)
        val current = codableJson.decodeFromString(
            SettingsWidgetBrowser.serializer(),
            """{"mode":"audioOnly","audioOnly":false}""",
        )
        assertEquals(SettingsWidgetBrowserMode.audioOnly, current.mode)
        val invalid = codableJson.decodeFromString(
            SettingsWidgetBrowser.serializer(),
            """{"mode":"bad","audioOnly":true}""",
        )
        assertEquals(SettingsWidgetBrowserMode.audioAndVideoOnly, invalid.mode)
        val nullMode = codableJson.decodeFromString(SettingsWidgetBrowser.serializer(), """{"mode":null}""")
        assertEquals(SettingsWidgetBrowserMode.periodicAudioAndVideo, nullMode.mode)
    }

    @Test
    fun autoSceneSwitchersSample() {
        val json = """
            {"switcherId":"$id1","switchers":[{"id":"$id2","name":"Switcher","shuffle":true,
              "scenes":[{"id":"$id3","sceneId":"$id1","time":30},{"sceneId":null}]}]}
        """
        val switchers = codableJson.decodeFromString(SettingsAutoSceneSwitchers.serializer(), json)
        assertEquals(uuid(id1), switchers.switcherId)
        val switcher = switchers.switchers.single()
        assertEquals(uuid(id2), switcher.id)
        assertEquals("Switcher", switcher.name)
        assertTrue(switcher.shuffle)
        assertEquals(uuid(id3), switcher.scenes[0].id)
        assertEquals(uuid(id1), switcher.scenes[0].sceneId)
        assertEquals(30, switcher.scenes[0].time)
        assertNull(switcher.scenes[1].sceneId)
        assertEquals(15, switcher.scenes[1].time)
        assertNull(codableJson.decodeFromString(SettingsAutoSceneSwitchers.serializer(), """{"switcherId":5}""").switcherId)
        assertEquals(
            "{\"switcherId\":null,\"switchers\":[]}",
            codableJson.encodeToString(SettingsAutoSceneSwitchers.serializer(), SettingsAutoSceneSwitchers()),
        )
    }

    @Test
    fun videoSourceWidgetMigratesToShapeEffect() {
        val json = """
            {"type":"Video source","videoSource":{"cornerRadius":5,"borderWidth":2,
              "borderColor":{"red":10,"green":20,"blue":30},"cropEnabled":true,"cropX":0.1,
              "cameraPosition":"SRT(LA)","srtlaCameraId":"$id2","rotation":90}}
        """
        val widget = codableJson.decodeFromString(SettingsWidget.serializer(), json)
        assertEquals(SettingsWidgetType.videoSource, widget.type)
        assertEquals(1, widget.effects.size)
        val shape = widget.effects[0].shape
        assertEquals(SettingsVideoEffectType.shape, widget.effects[0].type)
        assertEquals(5f, shape.cornerRadius)
        assertEquals(2.0, shape.borderWidth)
        assertEquals(RgbColor(red = 10, green = 20, blue = 30), shape.borderColor)
        assertTrue(shape.cropEnabled)
        assertEquals(0.1, shape.cropX)
        assertEquals(0f, widget.videoSource.cornerRadius)
        assertEquals(0.0, widget.videoSource.borderWidth)
        assertFalse(widget.videoSource.cropEnabled)
        assertEquals(SettingsSceneCameraPosition.srtla, widget.videoSource.videoSource.cameraPosition)
        assertEquals(uuid(id2), widget.videoSource.videoSource.srtlaCameraId)
        assertEquals(90.0, widget.videoSource.rotation)
        val unchanged = codableJson.decodeFromString(
            SettingsWidget.serializer(),
            """{"type":"Video source","videoSource":{"cornerRadius":5},"effects":[{"type":"shape"}]}""",
        )
        assertEquals(1, unchanged.effects.size)
        assertEquals(5f, unchanged.videoSource.cornerRadius)
    }

    @Test
    fun pomodoroBingoSlideshowAndTunerSamples() {
        val pomodoro = codableJson.decodeFromString(
            SettingsWidgetPomodoroTimer.serializer(),
            """{"focusDuration":25,"focusIcon":"brain.head.profile","breakIcon":"leaf","focusToBreakSoundId":"$id1",
               "breakName":"Rest","focusColor":{"red":1,"green":2,"blue":3}}""",
        )
        assertEquals(25, pomodoro.focusDuration)
        assertEquals(1500, pomodoro.secondsRemaining)
        assertEquals(PomodoroFocusIcon.brain, pomodoro.focusIcon)
        assertEquals(PomodoroBreakIcon.leaf, pomodoro.breakIcon)
        assertEquals(uuid(id1), pomodoro.focusToBreakSoundId)
        assertNull(pomodoro.breakToFocusSoundId)
        assertEquals("Rest", pomodoro.breakName)
        assertEquals(RgbColor(red = 1, green = 2, blue = 3), pomodoro.focusColor)
        val bingo = codableJson.decodeFromString(
            SettingsWidgetBingoCard.serializer(),
            """{"squares":[{"id":"$id1","text":"a","checked":true},{"id":"$id2","text":"b","checked":false}]}""",
        )
        assertEquals("a\nb", bingo.squaresText)
        assertEquals(uuid(id1), bingo.squares[0].id)
        assertTrue(bingo.squares[0].checked)
        val bingoMissingId = codableJson.decodeFromString(
            SettingsWidgetBingoCard.serializer(),
            """{"squares":[{"text":"a","checked":true}]}""",
        )
        assertTrue(bingoMissingId.squares.isEmpty())
        val slideshow = codableJson.decodeFromString(
            SettingsWidgetSlideshow.serializer(),
            """{"id":"$id1","slides":[{"id":"$id2","widgetId":"$id3","time":7},{"widgetId":null}]}""",
        )
        assertEquals(uuid(id3), slideshow.slides[0].widgetId)
        assertEquals(7, slideshow.slides[0].time)
        assertNotNull(slideshow.slides[1].widgetId)
        assertEquals(0, slideshow.slides[1].time)
        val wheel = codableJson.decodeFromString(
            SettingsWidgetWheelOfLuck.serializer(),
            """{"advanced":true,"options":[{"text":"a","weight":2},{"text":"b","weight":3}]}""",
        )
        assertTrue(wheel.advanced)
        assertEquals("a\nb", wheel.text)
        assertEquals(5, wheel.totalWeight)
        val vTuber = codableJson.decodeFromString(
            SettingsWidgetVTuber.serializer(),
            """{"type":"Live2D","cameraPosition":"Front","modelName":"m","sensitivity":{"mouth":2,"eyes":3},"armsAngle":10}""",
        )
        assertEquals(SettingsWidgetVTuberType.live2D, vTuber.type)
        assertEquals(SettingsSceneCameraPosition.front, vTuber.videoSource.cameraPosition)
        assertEquals("m", vTuber.modelName)
        assertEquals(SettingsSensitivity(mouth = 2.0, eyes = 3.0), vTuber.sensitivity)
        assertEquals(10.0, vTuber.armsAngle)
        val pngTuber = codableJson.decodeFromString(
            SettingsWidgetPngTuber.serializer(),
            """{"sensitivity":{"mouth":2},"mirror":true}""",
        )
        assertEquals(SettingsSensitivity(), pngTuber.sensitivity)
        assertTrue(pngTuber.mirror)
    }

    @Test
    fun wrongTypesFallBackToDefaults() {
        val text = codableJson.decodeFromString(
            SettingsWidgetText.serializer(),
            """{"fontSize":"big","formatString":5,"timers":{},"fontFamily":7,"fontDesign":"Comic"}""",
        )
        assertEquals(30, text.fontSize)
        assertEquals("{shortTime}", text.formatString)
        assertTrue(text.timers.isEmpty())
        assertNull(text.fontFamily)
        assertEquals(SettingsFontDesign.`default`, text.fontDesign)
        val widget = codableJson.decodeFromString(
            SettingsWidget.serializer(),
            """{"type":"Unknown","enabled":"yes","crop":{"sourceWidgetId":"$id1","x":1,"y":2,"width":3},
               "qrCode":{"message":5},"scene":{"sceneId":"$id2"},"effects":[5]}""",
        )
        assertEquals(SettingsWidgetType.text, widget.type)
        assertTrue(widget.enabled)
        assertEquals(0, widget.crop.x)
        assertEquals(200, widget.crop.height)
        assertEquals("", widget.qrCode.message)
        assertEquals(uuid(id2), widget.scene.sceneId)
        assertTrue(widget.effects.isEmpty())
        val unknownPosition = codableJson.decodeFromString(SettingsScene.serializer(), """{"cameraPosition":"Unknown"}""")
        assertEquals(SettingsSceneCameraPosition.back, unknownPosition.videoSource.cameraPosition)
        val numberPosition = codableJson.decodeFromString(SettingsScene.serializer(), """{"cameraPosition":5}""")
        assertEquals(defaultBackCameraPosition, numberPosition.videoSource.cameraPosition)
        val tripleWithoutCamera = codableJson.decodeFromString(
            SettingsWidgetVideoSource.serializer(),
            """{"cameraPosition":"Back triple"}""",
        )
        assertEquals(SettingsSceneCameraPosition.none, tripleWithoutCamera.videoSource.cameraPosition)
        val badWidgets = codableJson.decodeFromString(SettingsScene.serializer(), """{"widgets":5,"quickSwitchGroup":"x"}""")
        assertTrue(badWidgets.widgets.isEmpty())
        assertNull(badWidgets.quickSwitchGroup)
        val layoutString = codableJson.decodeFromString(
            SettingsWidgetModularScoreboard.serializer(),
            """{"layout":"sideBySide"}""",
        )
        assertEquals(SettingsWidgetScoreboardLayout.stacked, layoutString.layout)
        val layoutTwoCases = codableJson.decodeFromString(
            SettingsWidgetModularScoreboard.serializer(),
            """{"layout":{"sideBySide":{},"stackHistory":{}}}""",
        )
        assertEquals(SettingsWidgetScoreboardLayout.stacked, layoutTwoCases.layout)
        val layoutExtraKey = codableJson.decodeFromString(
            SettingsWidgetModularScoreboard.serializer(),
            """{"layout":{"stackHistory":{},"other":{}}}""",
        )
        assertEquals(SettingsWidgetScoreboardLayout.stackHistory, layoutExtraKey.layout)
        val clock = codableJson.decodeFromString(
            SettingsWidgetScoreboardClock.serializer(),
            """{"direction":{"down":1},"maximum":"x"}""",
        )
        assertEquals(SettingsWidgetGenericScoreboardClockDirection.up, clock.direction)
        assertEquals(45, clock.maximum)
        val sceneWidget = codableJson.decodeFromString(
            SettingsSceneWidget.serializer(),
            """{"size":"big","width":50,"height":60,"alignment":"Middle"}""",
        )
        assertEquals(50.0, sceneWidget.layout.size)
        assertEquals(SettingsAlignment.topLeft, sceneWidget.layout.alignment)
        val lutInvalid = codableJson.decodeFromString(SettingsVideoEffectLut.serializer(), """{"lut":"not-a-uuid"}""")
        assertNotNull(lutInvalid.lut)
        val lutNull = codableJson.decodeFromString(SettingsVideoEffectLut.serializer(), """{"lut":null}""")
        assertNull(lutNull.lut)
        val lutValue = codableJson.decodeFromString(SettingsVideoEffectLut.serializer(), """{"lut":"$id1"}""")
        assertEquals(uuid(id1), lutValue.lut)
        val voices = codableJson.decodeFromString(
            SettingsWidgetAlertsAlert.serializer(),
            """{"textToSpeechLanguageVoices":5,"id":"bad"}""",
        )
        assertTrue(voices.textToSpeechLanguageVoices.isEmpty())
        val chat = codableJson.decodeFromString(SettingsWidgetChat.serializer(), """{"displayStyle":"bad","fontSize":"x"}""")
        assertEquals(SettingsChatDisplayStyle.internationalName, chat.displayStyle)
        assertEquals(19f, chat.fontSize)
        val twitch = codableJson.decodeFromString(
            SettingsWidgetAlertsTwitch.serializer(),
            """{"cheerBits":[{"id":"$id1","bits":7,"comparisonOperator":5,"alert":{}}]}""",
        )
        assertEquals(1, twitch.cheerBits.size)
        assertEquals(1, twitch.cheerBits[0].bits)
        assertEquals(SettingsWidgetAlertsCheerBitsAlertOperator.greaterEqual, twitch.cheerBits[0].comparisonOperator)
        assertFailsWith<Exception> {
            codableJson.decodeFromString(
                SettingsWidgetAlertsCheerBitsAlert.serializer(),
                """{"id":"$id1","bits":1,"comparisonOperator":5,"alert":{}}""",
            )
        }
        val pomodoro = codableJson.decodeFromString(
            SettingsWidgetPomodoroTimer.serializer(),
            """{"focusIcon":"star","focusToBreakSoundId":"bad","focusDuration":1.5}""",
        )
        assertEquals(PomodoroFocusIcon.sun, pomodoro.focusIcon)
        assertNull(pomodoro.focusToBreakSoundId)
        assertEquals(30, pomodoro.focusDuration)
    }
}

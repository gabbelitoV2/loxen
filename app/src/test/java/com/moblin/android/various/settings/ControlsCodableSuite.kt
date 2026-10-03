package com.moblin.android.various.settings

import androidx.compose.runtime.snapshots.Snapshot
import com.moblin.android.common.various.RgbColor
import com.moblin.android.platform.codable.codableJson
import com.moblin.android.platform.swiftui.move
import com.moblin.android.various.network.DefaultTcpPorts
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private val randomKeys = setOf("id", "bridgeId", "password", "selectedStreamer")

private fun normalized(element: JsonElement): JsonElement = when (element) {
    is JsonObject -> JsonObject(
        element.mapValues { (key, value) ->
            if (key in randomKeys && value is JsonPrimitive && value.isString) {
                JsonPrimitive("*")
            } else {
                normalized(value)
            }
        },
    )
    is JsonArray -> JsonArray(element.map { normalized(it) })
    else -> element
}

private fun normalized(json: String): JsonElement = normalized(codableJson.parseToJsonElement(json))

private fun <T> toJson(serializer: KSerializer<T>, value: T): String = codableJson.encodeToString(serializer, value)

private fun <T> fromJson(serializer: KSerializer<T>, json: String): T = codableJson.decodeFromString(serializer, json)

private fun <T> assertRoundTrip(serializer: KSerializer<T>, value: T): String {
    val json = toJson(serializer, value)
    assertEquals(json, toJson(serializer, fromJson(serializer, json)))
    return json
}

private fun <T> assertEmptyDecodesTo(serializer: KSerializer<T>, expected: T) {
    assertEquals(
        normalized(toJson(serializer, expected)),
        normalized(toJson(serializer, fromJson(serializer, "{}"))),
    )
}

private fun upper(id: UUID): String = id.toString().uppercase()

private fun assertObserved(name: String, read: () -> Any?, write: () -> Unit) {
    val reads = mutableSetOf<Any>()
    Snapshot.observe(readObserver = { reads.add(it) }) { read() }
    assertTrue(reads.isNotEmpty(), "$name read was not observed")
    val changes = mutableSetOf<Any>()
    val handle = Snapshot.registerApplyObserver { changed, _ -> changes.addAll(changed) }
    try {
        write()
        Snapshot.sendApplyNotifications()
    } finally {
        handle.dispose()
    }
    assertTrue(changes.any { it in reads }, "$name change was not observed")
}

private const val ID_1 = "E621E1F8-C36C-495A-93FC-0C247A3E6E5F"
private const val ID_2 = "6f1c9b8e-2a4d-4e3f-9b1a-7c5d3e2f1a0b"
private const val ID_3 = "0A1B2C3D-4E5F-4061-8273-94A5B6C7D8E9"
private const val ID_4 = "11111111-2222-4333-8444-555555555555"
private const val ID_5 = "AAAAAAAA-BBBB-4CCC-8DDD-EEEEEEEEEEEE"

@RunWith(RobolectricTestRunner::class)
class ControlsCodableSuite {
    @Test
    fun macrosDefaultsRoundTrip() {
        assertRoundTrip(SettingsMacrosAction.serializer(), SettingsMacrosAction())
        assertRoundTrip(SettingsMacrosMacro.serializer(), SettingsMacrosMacro())
        assertRoundTrip(SettingsMacros.serializer(), SettingsMacros())
        val action = SettingsMacrosAction()
        action.function = SettingsMacrosActionFunction.ENABLE_DISABLE_SCENES
        action.sceneIds = setOf(UUID.fromString(ID_1), UUID.fromString(ID_2))
        action.filters = setOf(SettingsQuickButtonType.movie, SettingsQuickButtonType.fourThree)
        action.reaction = SettingsReaction.SPARKLE
        val macro = SettingsMacrosMacro()
        macro.actions = listOf(action, SettingsMacrosAction())
        macro.repeatMode = SettingsMacrosMacroRepeatMode.FOREVER
        val macros = SettingsMacros()
        macros.macros = listOf(macro)
        assertRoundTrip(SettingsMacros.serializer(), macros)
    }

    @Test
    fun quickButtonsDefaultsRoundTrip() {
        assertRoundTrip(
            SettingsQuickButton.serializer(),
            SettingsQuickButton(type = SettingsQuickButtonType.torch, imageOn = "flashlight.on.fill"),
        )
        assertRoundTrip(SettingsQuickButtons.serializer(), SettingsQuickButtons())
    }

    @Test
    fun streamDeckDefaultsRoundTrip() {
        assertRoundTrip(SettingsStreamDeckKey.serializer(), SettingsStreamDeckKey())
        assertRoundTrip(SettingsStreamDeckLayout.serializer(), SettingsStreamDeckLayout())
        assertRoundTrip(SettingsStreamDecks.serializer(), SettingsStreamDecks())
        val streamDecks = SettingsStreamDecks(listOf(SettingsStreamDeckLayout()), UUID.fromString(ID_1))
        assertRoundTrip(SettingsStreamDecks.serializer(), streamDecks)
    }

    @Test
    fun keyboardDefaultsRoundTrip() {
        assertRoundTrip(SettingsKeyboardKey.serializer(), SettingsKeyboardKey())
        assertRoundTrip(SettingsKeyboard.serializer(), SettingsKeyboard())
        val keyboard = SettingsKeyboard()
        keyboard.keys = listOf(SettingsKeyboardKey(), SettingsKeyboardKey())
        assertRoundTrip(SettingsKeyboard.serializer(), keyboard)
    }

    @Test
    fun gameControllerDefaultsRoundTrip() {
        assertRoundTrip(SettingsGameControllerButton.serializer(), SettingsGameControllerButton())
        val json = assertRoundTrip(SettingsGameController.serializer(), SettingsGameController())
        assertEquals(20, fromJson(SettingsGameController.serializer(), json).buttons.value.size)
    }

    @Test
    fun remoteControlDefaultsRoundTrip() {
        assertRoundTrip(SettingsRemoteControlAssistant.serializer(), SettingsRemoteControlAssistant())
        assertRoundTrip(SettingsRemoteControlStreamerUrl.serializer(), SettingsRemoteControlStreamerUrl())
        assertRoundTrip(SettingsRemoteControlStreamer.serializer(), SettingsRemoteControlStreamer())
        assertRoundTrip(SettingsRemoteControlServerRelay.serializer(), SettingsRemoteControlServerRelay())
        assertRoundTrip(SettingsRemoteControlWeb.serializer(), SettingsRemoteControlWeb())
        assertRoundTrip(SettingsRemoteControl.serializer(), SettingsRemoteControl())
    }

    @Test
    fun deepLinkCreatorDefaultsRoundTrip() {
        assertRoundTrip(DeepLinkCreatorStreamVideo.serializer(), DeepLinkCreatorStreamVideo())
        assertRoundTrip(DeepLinkCreatorStreamAudio.serializer(), DeepLinkCreatorStreamAudio())
        assertRoundTrip(DeepLinkCreatorStreamSrt.serializer(), DeepLinkCreatorStreamSrt())
        assertRoundTrip(DeepLinkCreatorStreamObs.serializer(), DeepLinkCreatorStreamObs())
        assertRoundTrip(DeepLinkCreatorStreamTwitch.serializer(), DeepLinkCreatorStreamTwitch())
        assertRoundTrip(DeepLinkCreatorStreamKick.serializer(), DeepLinkCreatorStreamKick())
        assertRoundTrip(DeepLinkCreatorStream.serializer(), DeepLinkCreatorStream())
        assertRoundTrip(DeepLinkCreatorQuickButton.serializer(), DeepLinkCreatorQuickButton())
        assertRoundTrip(DeepLinkCreatorQuickButtons.serializer(), DeepLinkCreatorQuickButtons())
        assertRoundTrip(DeepLinkCreatorWebBrowser.serializer(), DeepLinkCreatorWebBrowser())
        assertRoundTrip(DeepLinkCreator.serializer(), DeepLinkCreator())
        val deepLinkCreator = DeepLinkCreator()
        deepLinkCreator.streams.add(DeepLinkCreatorStream())
        deepLinkCreator.quickButtons.buttons.add(DeepLinkCreatorQuickButton())
        assertRoundTrip(DeepLinkCreator.serializer(), deepLinkCreator)
    }

    @Test
    fun emptyObjectDecodesToDefaults() {
        assertEmptyDecodesTo(SettingsMacrosAction.serializer(), SettingsMacrosAction())
        assertEmptyDecodesTo(SettingsMacrosMacro.serializer(), SettingsMacrosMacro())
        assertEmptyDecodesTo(SettingsMacros.serializer(), SettingsMacros())
        val quickButton = SettingsQuickButton(type = SettingsQuickButtonType.unknown, imageOn = "")
        quickButton.name = ""
        assertEmptyDecodesTo(SettingsQuickButton.serializer(), quickButton)
        assertEmptyDecodesTo(SettingsQuickButtons.serializer(), SettingsQuickButtons())
        val streamDeckKey = SettingsStreamDeckKey()
        streamDeckKey.color = RgbColor.white
        assertEmptyDecodesTo(SettingsStreamDeckKey.serializer(), streamDeckKey)
        assertEmptyDecodesTo(SettingsStreamDeckLayout.serializer(), SettingsStreamDeckLayout())
        assertEmptyDecodesTo(SettingsStreamDecks.serializer(), SettingsStreamDecks())
        assertEmptyDecodesTo(SettingsKeyboardKey.serializer(), SettingsKeyboardKey())
        assertEmptyDecodesTo(SettingsKeyboard.serializer(), SettingsKeyboard())
        assertEmptyDecodesTo(SettingsGameControllerButton.serializer(), SettingsGameControllerButton())
        val gameController = SettingsGameController()
        gameController.buttons.value = emptyList()
        assertEmptyDecodesTo(SettingsGameController.serializer(), gameController)
        assertEmptyDecodesTo(SettingsRemoteControlAssistant.serializer(), SettingsRemoteControlAssistant())
        assertEmptyDecodesTo(SettingsRemoteControlStreamerUrl.serializer(), SettingsRemoteControlStreamerUrl())
        assertEmptyDecodesTo(SettingsRemoteControlStreamer.serializer(), SettingsRemoteControlStreamer())
        assertEmptyDecodesTo(
            SettingsRemoteControlServerRelay.serializer(),
            SettingsRemoteControlServerRelay(enabled = false),
        )
        assertEmptyDecodesTo(SettingsRemoteControlWeb.serializer(), SettingsRemoteControlWeb())
        val remoteControl = SettingsRemoteControl()
        val migratedStreamer = SettingsRemoteControlAssistant(name = "Streamer")
        remoteControl.streamers = listOf(migratedStreamer)
        remoteControl.selectedStreamer = migratedStreamer.id
        assertEmptyDecodesTo(SettingsRemoteControl.serializer(), remoteControl)
        assertEmptyDecodesTo(DeepLinkCreatorStreamVideo.serializer(), DeepLinkCreatorStreamVideo())
        assertEmptyDecodesTo(DeepLinkCreatorStreamAudio.serializer(), DeepLinkCreatorStreamAudio())
        assertEmptyDecodesTo(DeepLinkCreatorStreamSrt.serializer(), DeepLinkCreatorStreamSrt())
        assertEmptyDecodesTo(DeepLinkCreatorStreamObs.serializer(), DeepLinkCreatorStreamObs())
        assertEmptyDecodesTo(DeepLinkCreatorStreamTwitch.serializer(), DeepLinkCreatorStreamTwitch())
        assertEmptyDecodesTo(DeepLinkCreatorStreamKick.serializer(), DeepLinkCreatorStreamKick())
        assertEmptyDecodesTo(DeepLinkCreatorStream.serializer(), DeepLinkCreatorStream())
        assertEmptyDecodesTo(DeepLinkCreatorQuickButton.serializer(), DeepLinkCreatorQuickButton())
        assertEmptyDecodesTo(DeepLinkCreatorQuickButtons.serializer(), DeepLinkCreatorQuickButtons())
        assertEmptyDecodesTo(DeepLinkCreatorWebBrowser.serializer(), DeepLinkCreatorWebBrowser())
        assertEmptyDecodesTo(DeepLinkCreator.serializer(), DeepLinkCreator())
    }

    @Test
    fun emptyStreamDeckLayoutIsPaddedTo36Keys() {
        val layout = fromJson(SettingsStreamDeckLayout.serializer(), "{}")
        assertEquals(36, layout.keys.value.size)
        assertEquals(SettingsStreamDeckLayout.baseName, layout.name)
        assertEquals(SettingsStreamDeckModel.classic, layout.model.value)
    }

    @Test
    fun actionEncodesLikeSwift() {
        val action = SettingsMacrosAction()
        assertEquals(
            """{"id":"${upper(action.id)}","function":null,"sceneId":null,"sceneIds":[],""" +
                """"autoSceneSwitcherId":null,"zoomX":1.0,"gimbalPresetId":null,"chatMessage":"",""" +
                """"notificationMessage":"","delay":3.0,"macroId":null,"djiDevices":[],"filters":[],"record":true,"mute":true,""" +
                """"torch":true,"reaction":{"fireworks":{}},"ifValue":"","ifComparison":"=",""" +
                """"ifOtherValue":"","ifRunCount":1,"event":"Twitch follow","eventMinimumAmount":0,""" +
                """"eventText":"","eventSceneId":null}""",
            toJson(SettingsMacrosAction.serializer(), action),
        )
        val macro = SettingsMacrosMacro()
        assertEquals(
            """{"id":"${upper(macro.id)}","name":"My macro","actions":[],"repeatMode":"off",""" +
                """"repeatCount":5,"closePanelOnRun":false,"runAtAppStart":false}""",
            toJson(SettingsMacrosMacro.serializer(), macro),
        )
    }

    @Test
    fun controllerKeysEncodeLikeSwift() {
        val button = SettingsGameControllerButton()
        button.name = "a.circle"
        button.text = "A"
        button.function.value = SettingsControllerFunction.GIMBAL_PRESET
        button.functionData.value = SettingsControllerFunctionData(
            gimbalPresetId = UUID.fromString(ID_2),
            gimbalMotion = SettingsGimbalMotion.WAKEUP,
        )
        assertEquals(
            """{"id":"${upper(button.id)}","name":"a.circle","text":"A","function":"Gimbal preset",""" +
                """"sceneId":null,"widgetId":null,"gimbalPresetId":"${ID_2.uppercase()}",""" +
                """"gimbalMotion":{"wakeup":{}},"macroId":null,"streamDeckLayoutId":null}""",
            toJson(SettingsGameControllerButton.serializer(), button),
        )
        val key = SettingsKeyboardKey()
        key.key = "k"
        assertEquals(
            """{"id":"${upper(key.id)}","key":"k","function":"Unused","sceneId":null,"widgetId":null,""" +
                """"gimbalPresetId":null,"gimbalMotion":{"kapow":{}},"macroId":null,"streamDeckLayoutId":null}""",
            toJson(SettingsKeyboardKey.serializer(), key),
        )
        val controller = SettingsGameController()
        controller.buttons.value = emptyList()
        controller.leftThumbStickFunction.value = SettingsControllerThumbStickFunction.GIMBAL_PAN_TILT
        assertEquals(
            """{"id":"${upper(controller.id)}","buttons":[],"leftThumbStickFunction":"Gimbal pan and tilt",""" +
                """"rightThumbStickFunction":"Unused"}""",
            toJson(SettingsGameController.serializer(), controller),
        )
        assertEquals(
            """{"layouts":[],"selectedId":null}""",
            toJson(SettingsStreamDecks.serializer(), SettingsStreamDecks()),
        )
    }

    @Test
    fun remoteControlAndDeepLinkEncodeLikeSwift() {
        val relay = SettingsRemoteControlServerRelay(bridgeId = "bridge")
        assertEquals(
            """{"enabled":true,"baseUrl":"wss://moblin.mys-lang.org/moblin-remote-control-relay",""" +
                """"bridgeId":"bridge"}""",
            toJson(SettingsRemoteControlServerRelay.serializer(), relay),
        )
        assertEquals(
            """{"enabled":false,"port":80,"deviceName":""}""",
            toJson(SettingsRemoteControlWeb.serializer(), SettingsRemoteControlWeb()),
        )
        val remoteControl = SettingsRemoteControl()
        val json = codableJson.parseToJsonElement(toJson(SettingsRemoteControl.serializer(), remoteControl))
        assertEquals(
            listOf("client", "server", "web", "password", "streamers", "selectedStreamer", "hasMigratedAssistant"),
            (json as JsonObject).keys.toList(),
        )
        assertEquals(
            """{"resolution":"1920x1080","fps":30,"bitrate":5000000,"codec":"H.265/HEVC","bFrames":false,""" +
                """"maxKeyFrameInterval":2}""",
            toJson(DeepLinkCreatorStreamVideo.serializer(), DeepLinkCreatorStreamVideo()),
        )
        assertEquals(
            """{"latency":3000,"adaptiveBitrateEnabled":true,"dnsLookupStrategy":"System"}""",
            toJson(DeepLinkCreatorStreamSrt.serializer(), DeepLinkCreatorStreamSrt()),
        )
        assertEquals(
            """{"streams":[],"quickButtonsEnabled":false,"quickButtons":{"twoColumns":true,"showName":true,""" +
                """"enableScroll":true,"buttons":[]},"webBrowserEnabled":false,"webBrowser":{"home":""}}""",
            toJson(DeepLinkCreator.serializer(), DeepLinkCreator()),
        )
    }

    @Test
    fun enumsUseSwiftRepresentation() {
        assertEquals("{\"hearts\":{}}", toJson(SettingsReaction.serializer(), SettingsReaction.HEARTS))
        assertEquals("{\"no\":{}}", toJson(SettingsGimbalMotion.serializer(), SettingsGimbalMotion.NO))
        assertEquals(
            "\"Enable/disable scenes\"",
            toJson(SettingsMacrosActionFunction.serializer(), SettingsMacrosActionFunction.ENABLE_DISABLE_SCENES),
        )
        assertEquals("\"Scene switched\"", toJson(SettingsMacrosEvent.serializer(), SettingsMacrosEvent.SWITCH_SCENE))
        assertEquals(
            "\"!=\"",
            toJson(SettingsMacrosActionIfComparison.serializer(), SettingsMacrosActionIfComparison.NOT_EQUAL),
        )
        assertEquals(
            "\"count\"",
            toJson(SettingsMacrosMacroRepeatMode.serializer(), SettingsMacrosMacroRepeatMode.COUNT),
        )
        assertEquals("\"4:3\"", toJson(SettingsQuickButtonType.serializer(), SettingsQuickButtonType.fourThree))
        assertEquals(
            "\"Stream deck layout\"",
            toJson(SettingsControllerFunction.serializer(), SettingsControllerFunction.STREAM_DECK_LAYOUT),
        )
        assertEquals(
            "\"Gimbal pan and tilt\"",
            toJson(
                SettingsControllerThumbStickFunction.serializer(),
                SettingsControllerThumbStickFunction.GIMBAL_PAN_TILT,
            ),
        )
        assertEquals("\"xl\"", toJson(SettingsStreamDeckModel.serializer(), SettingsStreamDeckModel.xl))
        assertEquals(SettingsGimbalMotion.YES, fromJson(SettingsGimbalMotion.serializer(), "{\"yes\":{},\"x\":1}"))
        assertEquals(SettingsReaction.RAIN, fromJson(SettingsReaction.serializer(), "{\"rain\":{\"_0\":1}}"))
        assertEquals(
            SettingsQuickButtonType.chat,
            fromJson(SettingsQuickButtonType.serializer(), "\"Pause chat\""),
        )
        assertEquals(
            SettingsQuickButtonType.unknown,
            fromJson(SettingsQuickButtonType.serializer(), "\"Teleport\""),
        )
    }

    @Test
    fun swiftMacrosSampleDecodes() {
        val json = """
            {"macros":[{"id":"$ID_1","name":"Raid","actions":[
              {"id":"$ID_2","function":"Enable\/disable scenes","sceneId":"$ID_3",
               "sceneIds":["$ID_4","$ID_5"],"autoSceneSwitcherId":"$ID_1","zoomX":2.5,
               "gimbalPresetId":"$ID_2","chatMessage":"hi \/ there","delay":1.5,"macroId":"$ID_3",
               "djiDevices":["$ID_4"],"filters":["Movie","Pause chat","Bogus"],"record":false,"mute":false,
               "torch":false,"reaction":{"hearts":{}},"ifValue":"{speed}","ifComparison":">=",
               "ifOtherValue":"30","ifRunCount":3,"event":"Kick kicks","eventMinimumAmount":42,
               "eventText":"boom","eventSceneId":"$ID_5"},
              {"function":"Delay","delay":10}],
             "repeatMode":"count","repeatCount":7,"closePanelOnRun":true,"runAtAppStart":true}]}
        """.trimIndent()
        val macros = fromJson(SettingsMacros.serializer(), json)
        assertEquals(1, macros.macros.size)
        val macro = macros.macros[0]
        assertEquals(UUID.fromString(ID_1), macro.id)
        assertEquals("Raid", macro.name)
        assertEquals(SettingsMacrosMacroRepeatMode.COUNT, macro.repeatMode)
        assertEquals(7, macro.repeatCount)
        assertTrue(macro.closePanelOnRun)
        assertTrue(macro.runAtAppStart)
        assertEquals(2, macro.actions.size)
        val action = macro.actions[0]
        assertEquals(UUID.fromString(ID_2), action.id)
        assertEquals(SettingsMacrosActionFunction.ENABLE_DISABLE_SCENES, action.function)
        assertEquals(UUID.fromString(ID_3), action.sceneId)
        assertEquals(setOf(UUID.fromString(ID_4), UUID.fromString(ID_5)), action.sceneIds)
        assertEquals(UUID.fromString(ID_1), action.autoSceneSwitcherId)
        assertEquals(2.5f, action.zoomX)
        assertEquals(UUID.fromString(ID_2), action.gimbalPresetId)
        assertEquals("hi / there", action.chatMessage)
        assertEquals(1.5, action.delay)
        assertEquals(UUID.fromString(ID_3), action.macroId)
        assertEquals(setOf(UUID.fromString(ID_4)), action.djiDevices)
        assertEquals(
            setOf(SettingsQuickButtonType.movie, SettingsQuickButtonType.chat, SettingsQuickButtonType.unknown),
            action.filters,
        )
        assertFalse(action.record)
        assertFalse(action.mute)
        assertFalse(action.torch)
        assertEquals(SettingsReaction.HEARTS, action.reaction)
        assertEquals("{speed}", action.ifValue)
        assertEquals(SettingsMacrosActionIfComparison.GREATER_EQUAL, action.ifComparison)
        assertEquals("30", action.ifOtherValue)
        assertEquals(3, action.ifRunCount)
        assertEquals(SettingsMacrosEvent.KICK_KICKS, action.event)
        assertEquals(42, action.eventMinimumAmount)
        assertEquals("boom", action.eventText)
        assertEquals(UUID.fromString(ID_5), action.eventSceneId)
        assertEquals(SettingsMacrosActionFunction.DELAY, macro.actions[1].function)
        assertEquals(10.0, macro.actions[1].delay)
        assertEquals(SettingsReaction.FIREWORKS, macro.actions[1].reaction)
        val encoded = toJson(SettingsMacros.serializer(), macros)
        assertTrue(encoded.contains("\"sceneIds\":[\"${ID_4.uppercase()}\",\"$ID_5\"]"))
        assertTrue(encoded.contains("\"reaction\":{\"hearts\":{}}"))
        assertTrue(encoded.contains("\"id\":\"${ID_2.uppercase()}\""))
        assertEquals(encoded, toJson(SettingsMacros.serializer(), fromJson(SettingsMacros.serializer(), encoded)))
    }

    @Test
    fun swiftStreamDecksSampleDecodes() {
        val json = """
            {"layouts":[{"id":"$ID_1","name":"Deck","model":"xl","keys":[
              {"id":"$ID_2","text":"Go","color":{"red":255,"green":0,"blue":0},"function":"Macro",
               "sceneId":null,"widgetId":"$ID_3","gimbalPresetId":null,"gimbalMotion":{"wakeup":{}},
               "macroId":"$ID_4","streamDeckLayoutId":"$ID_5"}]}],
             "selectedId":"$ID_1"}
        """.trimIndent()
        val streamDecks = fromJson(SettingsStreamDecks.serializer(), json)
        assertEquals(UUID.fromString(ID_1), streamDecks.selectedId.value)
        assertEquals(1, streamDecks.layouts.value.size)
        val layout = streamDecks.layouts.value[0]
        assertEquals(UUID.fromString(ID_1), layout.id)
        assertEquals("Deck", layout.name)
        assertEquals(SettingsStreamDeckModel.xl, layout.model.value)
        assertEquals(36, layout.keys.value.size)
        val key = layout.keys.value[0]
        assertEquals(UUID.fromString(ID_2), key.id)
        assertEquals("Go", key.text.value)
        assertEquals(RgbColor(red = 255, green = 0, blue = 0), key.color)
        assertEquals(SettingsControllerFunction.MACRO, key.function.value)
        assertNull(key.functionData.value.sceneId)
        assertEquals(UUID.fromString(ID_3), key.functionData.value.widgetId)
        assertEquals(SettingsGimbalMotion.WAKEUP, key.functionData.value.gimbalMotion)
        assertEquals(UUID.fromString(ID_4), key.functionData.value.macroId)
        assertEquals(UUID.fromString(ID_5), key.functionData.value.streamDeckLayoutId)
        assertEquals(SettingsStreamDeckKey.defaultColor, layout.keys.value[1].color)
        assertEquals(SettingsControllerFunction.UNUSED, layout.keys.value[35].function.value)
    }

    @Test
    fun swiftGameControllerAndKeyboardSamplesDecode() {
        val controllerJson = """
            {"id":"$ID_1","buttons":[
              {"id":"$ID_2","name":"a.circle","text":"A","function":"Gimbal preset","sceneId":null,
               "widgetId":null,"gimbalPresetId":"$ID_3","gimbalMotion":{"no":{}},"macroId":null,
               "streamDeckLayoutId":null}],
             "leftThumbStickFunction":"Gimbal pan and tilt","rightThumbStickFunction":"Sideways"}
        """.trimIndent()
        val controller = fromJson(SettingsGameController.serializer(), controllerJson)
        assertEquals(UUID.fromString(ID_1), controller.id)
        assertEquals(1, controller.buttons.value.size)
        val button = controller.buttons.value[0]
        assertEquals(UUID.fromString(ID_2), button.id)
        assertEquals("a.circle", button.name)
        assertEquals("A", button.text)
        assertEquals(SettingsControllerFunction.GIMBAL_PRESET, button.function.value)
        assertEquals(UUID.fromString(ID_3), button.functionData.value.gimbalPresetId)
        assertEquals(SettingsGimbalMotion.NO, button.functionData.value.gimbalMotion)
        assertEquals(SettingsControllerThumbStickFunction.GIMBAL_PAN_TILT, controller.leftThumbStickFunction.value)
        assertEquals(SettingsControllerThumbStickFunction.UNUSED, controller.rightThumbStickFunction.value)
        val keyboardJson = """
            {"keys":[{"id":"$ID_4","key":"x","function":"Scene","sceneId":"$ID_5",
              "gimbalMotion":{"yes":{}}}]}
        """.trimIndent()
        val keyboard = fromJson(SettingsKeyboard.serializer(), keyboardJson)
        assertEquals(1, keyboard.keys.size)
        val key = keyboard.keys[0]
        assertEquals(UUID.fromString(ID_4), key.id)
        assertEquals("x", key.key)
        assertEquals(SettingsControllerFunction.SCENE, key.function)
        assertEquals(UUID.fromString(ID_5), key.functionData.sceneId)
        assertEquals(SettingsGimbalMotion.YES, key.functionData.gimbalMotion)
        assertNull(key.functionData.macroId)
    }

    @Test
    fun swiftRemoteControlSampleDecodes() {
        val json = """
            {"client":{"id":"$ID_1","name":"Phone","enabled":true,"port":2345,
               "relay":{"enabled":true,"baseUrl":"wss:\/\/relay.example\/x","bridgeId":"abc"}},
             "server":{"enabled":true,"name":"Assistant","url":"ws://1.2.3.4:2345","previewFps":2.5,
               "reliableChatAndEvents":true,"savedUrls":[{"id":"$ID_2","name":"Home","url":"ws://home"}]},
             "web":{"enabled":true,"port":8080,"deviceName":"Pixel"},
             "password":"secret",
             "streamers":[{"id":"$ID_3","name":"Cam","enabled":true,"port":3000,
               "relay":{"enabled":false,"baseUrl":"wss://b","bridgeId":"def"}}],
             "selectedStreamer":"$ID_3","hasMigratedAssistant":true}
        """.trimIndent()
        val remoteControl = fromJson(SettingsRemoteControl.serializer(), json)
        assertEquals(UUID.fromString(ID_1), remoteControl.assistant.id)
        assertEquals("Phone", remoteControl.assistant.name)
        assertTrue(remoteControl.assistant.enabled)
        assertEquals(2345, remoteControl.assistant.port)
        assertEquals("wss://relay.example/x", remoteControl.assistant.relay.baseUrl)
        assertEquals("abc", remoteControl.assistant.relay.bridgeId)
        assertTrue(remoteControl.streamer.enabled)
        assertEquals("Assistant", remoteControl.streamer.name)
        assertEquals(2.5f, remoteControl.streamer.previewFps)
        assertTrue(remoteControl.streamer.reliableChatAndEvents)
        assertEquals(1, remoteControl.streamer.savedUrls.size)
        assertEquals(UUID.fromString(ID_2), remoteControl.streamer.savedUrls[0].id)
        assertEquals("ws://home", remoteControl.streamer.savedUrls[0].url)
        assertTrue(remoteControl.web.enabled)
        assertEquals(8080, remoteControl.web.port)
        assertEquals("Pixel", remoteControl.web.deviceName)
        assertEquals("secret", remoteControl.password)
        assertEquals(1, remoteControl.streamers.size)
        assertEquals("Cam", remoteControl.streamers[0].name)
        assertFalse(remoteControl.streamers[0].relay.enabled)
        assertEquals(UUID.fromString(ID_3), remoteControl.selectedStreamer)
        assertEquals("Cam", remoteControl.getSelectedStreamerName())
        assertTrue(remoteControl.hasMigratedAssistant)
    }

    @Test
    fun remoteControlAssistantIsMigratedToStreamers() {
        val json = """
            {"client":{"enabled":true,"port":1234,"relay":{"enabled":true,"baseUrl":"wss://r","bridgeId":"b1"}},
             "streamers":[{"id":"$ID_2","name":"Old"}],"hasMigratedAssistant":false}
        """.trimIndent()
        val remoteControl = fromJson(SettingsRemoteControl.serializer(), json)
        assertTrue(remoteControl.hasMigratedAssistant)
        assertEquals(2, remoteControl.streamers.size)
        val streamer = remoteControl.streamers[1]
        assertEquals("Streamer", streamer.name)
        assertTrue(streamer.enabled)
        assertEquals(1234, streamer.port)
        assertTrue(streamer.relay.enabled)
        assertEquals("wss://r", streamer.relay.baseUrl)
        assertEquals("b1", streamer.relay.bridgeId)
        assertEquals(streamer.id, remoteControl.selectedStreamer)
        val empty = fromJson(SettingsRemoteControl.serializer(), "{}")
        assertEquals(1, empty.streamers.size)
        assertEquals(empty.streamers[0].id, empty.selectedStreamer)
        assertTrue(empty.streamers[0].relay.enabled)
        assertFalse(fromJson(SettingsRemoteControlServerRelay.serializer(), "{}").enabled)
    }

    @Test
    fun swiftDeepLinkCreatorSampleDecodes() {
        val json = """
            {"streams":[{"id":"$ID_1","name":"IRL","url":"srt://host:5000","selected":true,
               "video":{"resolution":"1280x720","fps":60,"bitrate":3000000,"codec":"H.264\/AVC","bFrames":true,
                 "maxKeyFrameInterval":4},
               "audio":{"bitrate":96000},
               "srt":{"latency":2000,"adaptiveBitrateEnabled":false,"dnsLookupStrategy":"IPv6"},
               "obs":{"webSocketUrl":"ws://obs:4455","webSocketPassword":"pw"},
               "twitch":{"channelName":"chan","channelId":"123"},
               "kick":{"channelName":"kchan"}}],
             "quickButtonsEnabled":true,
             "quickButtons":{"twoColumns":false,"showName":false,"enableScroll":false,
               "buttons":[{"id":"$ID_2","type":"Pause chat","enabled":true,"page":2}]},
             "webBrowserEnabled":true,"webBrowser":{"home":"https://moblin"}}
        """.trimIndent()
        val deepLinkCreator = fromJson(DeepLinkCreator.serializer(), json)
        assertEquals(1, deepLinkCreator.streams.size)
        val stream = deepLinkCreator.streams[0]
        assertEquals(UUID.fromString(ID_1), stream.id)
        assertEquals("IRL", stream.name)
        assertEquals("srt://host:5000", stream.url)
        assertTrue(stream.selected)
        assertEquals(SettingsStreamResolution.r1280x720, stream.video.resolution)
        assertEquals(60, stream.video.fps)
        assertEquals(3_000_000, stream.video.bitrate)
        assertEquals(SettingsStreamCodec.h264avc, stream.video.codec)
        assertTrue(stream.video.bFrames)
        assertEquals(4, stream.video.maxKeyFrameInterval)
        assertEquals(96_000, stream.audio.bitrate)
        assertEquals(96f, stream.audio.bitrateFloat)
        assertEquals(2000, stream.srt.latency)
        assertFalse(stream.srt.adaptiveBitrateEnabled)
        assertEquals(SettingsDnsLookupStrategy.ipv6, stream.srt.dnsLookupStrategy)
        assertEquals("ws://obs:4455", stream.obs.webSocketUrl)
        assertEquals("pw", stream.obs.webSocketPassword)
        assertEquals("chan", stream.twitch.channelName)
        assertEquals("123", stream.twitch.channelId)
        assertEquals("kchan", stream.kick.channelName)
        assertTrue(deepLinkCreator.quickButtonsEnabled)
        assertFalse(deepLinkCreator.quickButtons.twoColumns)
        assertFalse(deepLinkCreator.quickButtons.showName)
        assertFalse(deepLinkCreator.quickButtons.enableScroll)
        assertEquals(1, deepLinkCreator.quickButtons.buttons.size)
        val button = deepLinkCreator.quickButtons.buttons[0]
        assertEquals(UUID.fromString(ID_2), button.id)
        assertEquals(SettingsQuickButtonType.chat, button.type)
        assertTrue(button.enabled)
        assertEquals(2, button.page)
        assertTrue(deepLinkCreator.webBrowserEnabled)
        assertEquals("https://moblin", deepLinkCreator.webBrowser.home)
        deepLinkCreator.quickButtons.buttons.add(DeepLinkCreatorQuickButton())
        assertEquals(2, deepLinkCreator.quickButtons.buttons.size)
    }

    @Test
    fun swiftQuickButtonSampleDecodes() {
        val json = """
            {"name":"Torch","id":"$ID_1","type":"Torch","imageType":"System name",
             "systemImageNameOn":"flashlight.on.fill","systemImageNameOff":"flashlight.off.fill","isOn":true,
             "enabled":false,"backgroundColor":{"red":10,"green":20,"blue":30},"page":3}
        """.trimIndent()
        val button = fromJson(SettingsQuickButton.serializer(), json)
        assertEquals("Torch", button.name)
        assertEquals(UUID.fromString(ID_1), button.id)
        assertEquals(SettingsQuickButtonType.torch, button.type)
        assertEquals("flashlight.on.fill", button.imageOn)
        assertEquals("flashlight.off.fill", button.imageOff)
        assertTrue(button.isOn.value)
        assertFalse(button.enabled.value)
        assertEquals(RgbColor(red = 10, green = 20, blue = 30), button.backgroundColor)
        assertEquals(10f / 255f, button.color.value.red, 0.01f)
        assertEquals(3, button.page.value)
        val encoded = codableJson.parseToJsonElement(toJson(SettingsQuickButton.serializer(), button)) as JsonObject
        assertEquals(
            listOf(
                "name",
                "id",
                "type",
                "systemImageNameOn",
                "systemImageNameOff",
                "isOn",
                "enabled",
                "backgroundColor",
                "page",
            ),
            encoded.keys.toList(),
        )
        val quickButtons = fromJson(
            SettingsQuickButtons.serializer(),
            """{"twoColumns":false,"bigButtons":true,"showName":false,"enableScroll":false,""" +
                """"blackScreenShowChat":true,"blackScreenShowStatus":true,"backgroundImageCropX":0.25,""" +
                """"backgroundImageCropY":0.5,"backgroundImageCropWidth":0.5,"backgroundImageCropHeight":0.25,""" +
                """"backgroundImageOpacity":0.75}""",
        )
        assertFalse(quickButtons.twoColumns.value)
        assertTrue(quickButtons.bigButtons.value)
        assertFalse(quickButtons.showName.value)
        assertFalse(quickButtons.enableScroll.value)
        assertTrue(quickButtons.stealthModeShowChat.value)
        assertTrue(quickButtons.stealthModeShowStatus.value)
        assertEquals(0.25, quickButtons.backgroundImageCropX)
        assertEquals(0.5, quickButtons.backgroundImageCropY)
        assertEquals(0.5, quickButtons.backgroundImageCropWidth)
        assertEquals(0.25, quickButtons.backgroundImageCropHeight)
        assertEquals(0.75, quickButtons.backgroundImageOpacity.value)
        assertTrue(toJson(SettingsQuickButtons.serializer(), quickButtons).contains("\"blackScreenShowChat\":true"))
    }

    @Test
    fun wrongTypedMacroFieldsFallBackToDefaults() {
        val json = """
            {"id":5,"function":7,"sceneId":"nope","sceneIds":"x","autoSceneSwitcherId":[],"zoomX":"big",
             "chatMessage":3,"delay":"slow","djiDevices":["bad"],"filters":[1],"record":5,"mute":"no",
             "torch":{},"reaction":"hearts","ifValue":false,"ifComparison":"~","ifRunCount":1.5,
             "event":"Nope","eventMinimumAmount":"many","eventText":[],"eventSceneId":42}
        """.trimIndent()
        val action = fromJson(SettingsMacrosAction.serializer(), json)
        assertNull(action.function)
        assertNull(action.sceneId)
        assertEquals(emptySet(), action.sceneIds)
        assertNull(action.autoSceneSwitcherId)
        assertEquals(1f, action.zoomX)
        assertEquals("", action.chatMessage)
        assertEquals(3.0, action.delay)
        assertEquals(emptySet(), action.djiDevices)
        assertEquals(emptySet(), action.filters)
        assertTrue(action.record)
        assertTrue(action.mute)
        assertTrue(action.torch)
        assertEquals(SettingsReaction.FIREWORKS, action.reaction)
        assertEquals("", action.ifValue)
        assertEquals(SettingsMacrosActionIfComparison.EQUAL, action.ifComparison)
        assertEquals(1, action.ifRunCount)
        assertEquals(SettingsMacrosEvent.TWITCH_FOLLOW, action.event)
        assertEquals(0, action.eventMinimumAmount)
        assertEquals("", action.eventText)
        assertNull(action.eventSceneId)
        val macro = fromJson(
            SettingsMacrosMacro.serializer(),
            """{"id":"$ID_1","name":1,"actions":[1],"repeatMode":"sometimes","repeatCount":"lots",""" +
                """"closePanelOnRun":"on","runAtAppStart":0}""",
        )
        assertEquals(UUID.fromString(ID_1), macro.id)
        assertEquals(SettingsMacrosMacro.baseName, macro.name)
        assertEquals(emptyList(), macro.actions)
        assertEquals(SettingsMacrosMacroRepeatMode.OFF, macro.repeatMode)
        assertEquals(5, macro.repeatCount)
        assertFalse(macro.closePanelOnRun)
        assertFalse(macro.runAtAppStart)
        assertEquals(emptyList(), fromJson(SettingsMacros.serializer(), """{"macros":{}}""").macros)
    }

    @Test
    fun wrongTypedControllerFieldsFallBackToDefaults() {
        val button = fromJson(
            SettingsGameControllerButton.serializer(),
            """{"id":"$ID_1","name":2,"text":null,"function":"Fly","sceneId":"x","gimbalMotion":"yes"}""",
        )
        assertEquals(UUID.fromString(ID_1), button.id)
        assertEquals("", button.name)
        assertEquals("", button.text)
        assertEquals(SettingsControllerFunction.UNUSED, button.function.value)
        assertNull(button.functionData.value.sceneId)
        assertEquals(SettingsGimbalMotion.KAPOW, button.functionData.value.gimbalMotion)
        val twoCases = fromJson(SettingsKeyboardKey.serializer(), """{"gimbalMotion":{"yes":{},"no":{}}}""")
        assertEquals(SettingsGimbalMotion.KAPOW, twoCases.functionData.gimbalMotion)
        val nullCase = fromJson(SettingsKeyboardKey.serializer(), """{"gimbalMotion":{"yes":null}}""")
        assertEquals(SettingsGimbalMotion.KAPOW, nullCase.functionData.gimbalMotion)
        val controller = fromJson(
            SettingsGameController.serializer(),
            """{"buttons":"x","leftThumbStickFunction":5,"rightThumbStickFunction":"Sideways"}""",
        )
        assertEquals(emptyList(), controller.buttons.value)
        assertEquals(SettingsControllerThumbStickFunction.UNUSED, controller.leftThumbStickFunction.value)
        assertEquals(SettingsControllerThumbStickFunction.UNUSED, controller.rightThumbStickFunction.value)
        assertEquals(emptyList(), fromJson(SettingsKeyboard.serializer(), """{"keys":[5]}""").keys)
        val key = fromJson(SettingsStreamDeckKey.serializer(), """{"text":1,"color":{"red":"x"},"function":1}""")
        assertEquals("", key.text.value)
        assertEquals(RgbColor.white, key.color)
        assertEquals(SettingsControllerFunction.UNUSED, key.function.value)
        val layout = fromJson(SettingsStreamDeckLayout.serializer(), """{"name":[],"model":"huge","keys":{}}""")
        assertEquals(SettingsStreamDeckLayout.baseName, layout.name)
        assertEquals(SettingsStreamDeckModel.classic, layout.model.value)
        assertEquals(36, layout.keys.value.size)
        val streamDecks = fromJson(SettingsStreamDecks.serializer(), """{"layouts":5,"selectedId":"bad"}""")
        assertEquals(emptyList(), streamDecks.layouts.value)
        assertNull(streamDecks.selectedId.value)
    }

    @Test
    fun wrongTypedQuickButtonFieldsFallBackToDefaults() {
        val button = fromJson(
            SettingsQuickButton.serializer(),
            """{"name":1,"type":7,"isOn":"yes","enabled":0,"backgroundColor":"red","page":"two"}""",
        )
        assertEquals("", button.name)
        assertEquals(SettingsQuickButtonType.unknown, button.type)
        assertFalse(button.isOn.value)
        assertTrue(button.enabled.value)
        assertEquals(defaultQuickButtonColor, button.backgroundColor)
        assertEquals(1, button.page.value)
        val quickButtons = fromJson(
            SettingsQuickButtons.serializer(),
            """{"twoColumns":1,"blackScreenShowChat":"on","backgroundImageCropWidth":"x","backgroundImageOpacity":[]}""",
        )
        assertTrue(quickButtons.twoColumns.value)
        assertFalse(quickButtons.stealthModeShowChat.value)
        assertEquals(1.0, quickButtons.backgroundImageCropWidth)
        assertEquals(1.0, quickButtons.backgroundImageOpacity.value)
    }

    @Test
    fun wrongTypedRemoteControlFieldsFallBackToDefaults() {
        val assistant = fromJson(
            SettingsRemoteControlAssistant.serializer(),
            """{"id":"x","name":5,"enabled":"yes","port":70000,"relay":5}""",
        )
        assertEquals(SettingsRemoteControlAssistant.baseName, assistant.name)
        assertFalse(assistant.enabled)
        assertEquals(0, assistant.port)
        assertTrue(assistant.relay.enabled)
        assertEquals(0, fromJson(SettingsRemoteControlAssistant.serializer(), """{"port":-1}""").port)
        assertEquals(65535, fromJson(SettingsRemoteControlAssistant.serializer(), """{"port":65535}""").port)
        val web = fromJson(SettingsRemoteControlWeb.serializer(), """{"enabled":1,"port":"80a","deviceName":{}}""")
        assertFalse(web.enabled)
        assertEquals(80, web.port)
        assertEquals("", web.deviceName)
        val streamer = fromJson(
            SettingsRemoteControlStreamer.serializer(),
            """{"previewFps":"fast","savedUrls":[1],"url":5}""",
        )
        assertEquals(1.0f, streamer.previewFps)
        assertEquals(emptyList(), streamer.savedUrls)
        assertEquals("", streamer.url)
        val remoteControl = fromJson(
            SettingsRemoteControl.serializer(),
            """{"client":[],"password":1,"streamers":"x","selectedStreamer":"bad","hasMigratedAssistant":true}""",
        )
        assertEquals(emptyList(), remoteControl.streamers)
        assertNull(remoteControl.selectedStreamer)
        assertNotEquals("1", remoteControl.password)
        assertTrue(remoteControl.password.isNotEmpty())
        val relay = fromJson(SettingsRemoteControlServerRelay.serializer(), """{"bridgeId":1}""")
        assertTrue(relay.bridgeId.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")))
    }

    @Test
    fun wrongTypedDeepLinkCreatorFieldsFallBackToDefaults() {
        val video = fromJson(
            DeepLinkCreatorStreamVideo.serializer(),
            """{"resolution":"123x456","fps":"x","bitrate":-5,"codec":5,"bFrames":"no","maxKeyFrameInterval":2.5}""",
        )
        assertEquals(SettingsStream.defaultResolution, video.resolution)
        assertEquals(SettingsStream.defaultFps, video.fps)
        assertEquals(5_000_000, video.bitrate)
        assertEquals(SettingsStreamCodec.h265hevc, video.codec)
        assertFalse(video.bFrames)
        assertEquals(2, video.maxKeyFrameInterval)
        val srt = fromJson(DeepLinkCreatorStreamSrt.serializer(), """{"latency":"x","dnsLookupStrategy":"IPv5"}""")
        assertEquals(defaultSrtLatency, srt.latency)
        assertEquals(SettingsDnsLookupStrategy.system, srt.dnsLookupStrategy)
        assertEquals(128_000, fromJson(DeepLinkCreatorStreamAudio.serializer(), """{"bitrate":"x"}""").bitrate)
        val stream = fromJson(
            DeepLinkCreatorStream.serializer(),
            """{"name":null,"url":1,"selected":"x","video":[],"kick":{"channelName":2}}""",
        )
        assertEquals(DeepLinkCreatorStream.baseName, stream.name)
        assertEquals(defaultStreamUrl, stream.url)
        assertFalse(stream.selected)
        assertEquals(5_000_000, stream.video.bitrate)
        assertEquals("", stream.kick.channelName)
        val button = fromJson(DeepLinkCreatorQuickButton.serializer(), """{"type":[],"enabled":"x","page":"y"}""")
        assertEquals(SettingsQuickButtonType.unknown, button.type)
        assertFalse(button.enabled)
        assertEquals(1, button.page)
        val deepLinkCreator = fromJson(
            DeepLinkCreator.serializer(),
            """{"streams":5,"quickButtonsEnabled":"x","quickButtons":[],"webBrowser":"home"}""",
        )
        assertEquals(emptyList(), deepLinkCreator.streams)
        assertFalse(deepLinkCreator.quickButtonsEnabled)
        assertTrue(deepLinkCreator.quickButtons.twoColumns)
        assertEquals("", deepLinkCreator.webBrowser.home)
    }

    @Test
    fun everyClassEncodesSwiftKeysInOrder() {
        fun <T> keysOf(serializer: KSerializer<T>, value: T): List<String> =
            (codableJson.parseToJsonElement(toJson(serializer, value)) as JsonObject).keys.toList()
        assertEquals(listOf("macros"), keysOf(SettingsMacros.serializer(), SettingsMacros()))
        assertEquals(
            listOf(
                "twoColumns",
                "bigButtons",
                "showName",
                "enableScroll",
                "blackScreenShowChat",
                "blackScreenShowStatus",
                "backgroundImageCropX",
                "backgroundImageCropY",
                "backgroundImageCropWidth",
                "backgroundImageCropHeight",
                "backgroundImageOpacity",
            ),
            keysOf(SettingsQuickButtons.serializer(), SettingsQuickButtons()),
        )
        assertEquals(
            listOf(
                "id",
                "text",
                "color",
                "function",
                "sceneId",
                "widgetId",
                "gimbalPresetId",
                "gimbalMotion",
                "macroId",
                "streamDeckLayoutId",
            ),
            keysOf(SettingsStreamDeckKey.serializer(), SettingsStreamDeckKey()),
        )
        assertEquals(
            listOf("id", "name", "model", "keys"),
            keysOf(SettingsStreamDeckLayout.serializer(), SettingsStreamDeckLayout()),
        )
        assertEquals(listOf("keys"), keysOf(SettingsKeyboard.serializer(), SettingsKeyboard()))
        assertEquals(
            listOf("id", "name", "enabled", "port", "relay"),
            keysOf(SettingsRemoteControlAssistant.serializer(), SettingsRemoteControlAssistant()),
        )
        assertEquals(
            listOf("id", "name", "url"),
            keysOf(SettingsRemoteControlStreamerUrl.serializer(), SettingsRemoteControlStreamerUrl()),
        )
        assertEquals(
            listOf("enabled", "name", "url", "previewFps", "reliableChatAndEvents", "savedUrls"),
            keysOf(SettingsRemoteControlStreamer.serializer(), SettingsRemoteControlStreamer()),
        )
        assertEquals(listOf("bitrate"), keysOf(DeepLinkCreatorStreamAudio.serializer(), DeepLinkCreatorStreamAudio()))
        assertEquals(
            listOf("webSocketUrl", "webSocketPassword"),
            keysOf(DeepLinkCreatorStreamObs.serializer(), DeepLinkCreatorStreamObs()),
        )
        assertEquals(
            listOf("channelName", "channelId"),
            keysOf(DeepLinkCreatorStreamTwitch.serializer(), DeepLinkCreatorStreamTwitch()),
        )
        assertEquals(listOf("channelName"), keysOf(DeepLinkCreatorStreamKick.serializer(), DeepLinkCreatorStreamKick()))
        assertEquals(
            listOf("id", "name", "url", "selected", "video", "audio", "srt", "obs", "twitch", "kick"),
            keysOf(DeepLinkCreatorStream.serializer(), DeepLinkCreatorStream()),
        )
        assertEquals(
            listOf("id", "type", "enabled", "page"),
            keysOf(DeepLinkCreatorQuickButton.serializer(), DeepLinkCreatorQuickButton()),
        )
        val key = SettingsStreamDeckKey(
            UUID.fromString(ID_1),
            "Go",
            RgbColor(red = 1, green = 2, blue = 3),
            SettingsControllerFunction.WIDGET,
            SettingsControllerFunctionData(widgetId = UUID.fromString(ID_2), gimbalMotion = SettingsGimbalMotion.YES),
        )
        val keyJson = codableJson.parseToJsonElement(toJson(SettingsStreamDeckKey.serializer(), key)) as JsonObject
        assertEquals(JsonPrimitive(ID_1), keyJson["id"])
        assertEquals(JsonPrimitive("Widget"), keyJson["function"])
        assertEquals(JsonPrimitive(ID_2.uppercase()), keyJson["widgetId"])
        assertEquals(codableJson.parseToJsonElement("{\"yes\":{}}"), keyJson["gimbalMotion"])
        val color = keyJson["color"] as JsonObject
        assertEquals(
            listOf(1, 2, 3),
            listOf("red", "green", "blue").map { (color[it] as JsonPrimitive).content.toInt() },
        )
    }

    @Test
    fun swiftEnumDecodingEdgeCases() {
        fun reactionOf(json: String): SettingsReaction =
            fromJson(SettingsMacrosAction.serializer(), """{"reaction":$json}""").reaction
        assertEquals(SettingsReaction.FIREWORKS, reactionOf("{}"))
        assertEquals(SettingsReaction.FIREWORKS, reactionOf("""{"bogus":{}}"""))
        assertEquals(SettingsReaction.FIREWORKS, reactionOf("""{"rain":{},"lasers":{}}"""))
        assertEquals(SettingsReaction.FIREWORKS, reactionOf("""{"rain":[]}"""))
        assertEquals(SettingsReaction.FIREWORKS, reactionOf("null"))
        assertEquals(SettingsReaction.RAIN, reactionOf("""{"bogus":{},"rain":{}}"""))
        for (reaction in SettingsReaction.entries) {
            assertEquals(reaction, reactionOf("""{"${reaction.rawValue}":{}}"""))
        }
        for (motion in SettingsGimbalMotion.entries) {
            assertEquals(
                motion,
                fromJson(SettingsKeyboardKey.serializer(), """{"gimbalMotion":{"${motion.rawValue}":{}}}""")
                    .functionData.gimbalMotion,
            )
        }
        for (function in SettingsMacrosActionFunction.entries) {
            assertEquals(function, fromJson(SettingsMacrosActionFunction.serializer(), "\"${function.rawValue}\""))
        }
        for (event in SettingsMacrosEvent.entries) {
            assertEquals(event, fromJson(SettingsMacrosEvent.serializer(), "\"${event.rawValue}\""))
        }
        for (function in SettingsControllerFunction.entries) {
            assertEquals(function, fromJson(SettingsControllerFunction.serializer(), "\"${function.rawValue}\""))
        }
        for (type in SettingsQuickButtonType.entries) {
            assertEquals(type, fromJson(SettingsQuickButtonType.serializer(), "\"${type.rawValue}\""))
        }
        val action = fromJson(SettingsMacrosAction.serializer(), """{"function":null,"filters":["Torch",null]}""")
        assertNull(action.function)
        assertEquals(emptySet(), action.filters)
        val video = fromJson(DeepLinkCreatorStreamVideo.serializer(), """{"codec":"VP9"}""")
        assertEquals(SettingsStreamCodec.h264avc, video.codec)
        val web = fromJson(SettingsRemoteControlWeb.serializer(), """{"port":70000}""")
        assertEquals(DefaultTcpPorts.remoteControlWeb, web.port)
        val negativePort = fromJson(SettingsRemoteControlWeb.serializer(), """{"port":-1}""")
        assertEquals(DefaultTcpPorts.remoteControlWeb, negativePort.port)
        assertEquals(65535, fromJson(SettingsRemoteControlWeb.serializer(), """{"port":65535}""").port)
        assertEquals(
            4000,
            fromJson(DeepLinkCreatorStreamVideo.serializer(), """{"bitrate":4000}""").bitrate,
        )
    }

    @Test
    fun listsOfSettingsUseTheirSerializers() {
        val macros = listOf(SettingsMacrosMacro(), SettingsMacrosMacro())
        val json = toJson(ListSerializer(SettingsMacrosMacro.serializer()), macros)
        assertEquals(
            macros.map { it.id },
            fromJson(ListSerializer(SettingsMacrosMacro.serializer()), json).map { it.id },
        )
    }

    @Test
    fun publishedPropertiesAreObserved() {
        val macros = SettingsMacros()
        val macro = SettingsMacrosMacro()
        val action = SettingsMacrosAction()
        val keyboard = SettingsKeyboard()
        val keyboardKey = SettingsKeyboardKey()
        val layout = SettingsStreamDeckLayout()
        val remoteControl = SettingsRemoteControl()
        val stream = DeepLinkCreatorStream()
        val quickButtons = DeepLinkCreatorQuickButtons()
        val deepLinkCreator = DeepLinkCreator()
        assertObserved("SettingsMacros.macros", { macros.macros }) {
            macros.macros = macros.macros + SettingsMacrosMacro()
        }
        assertObserved("SettingsMacrosMacro.name", { macro.name }) { macro.name = "Renamed" }
        assertObserved("SettingsMacrosMacro.running", { macro.running }) { macro.running = true }
        assertObserved("SettingsMacrosMacro.actions", { macro.actions }) {
            macro.actions = macro.actions + SettingsMacrosAction()
        }
        assertObserved("SettingsMacrosAction.function", { action.function }) {
            action.function = SettingsMacrosActionFunction.DELAY
        }
        assertObserved("SettingsMacrosAction.filters", { action.filters }) {
            action.filters = setOf(SettingsQuickButtonType.movie)
        }
        assertObserved("SettingsKeyboard.keys", { keyboard.keys }) {
            keyboard.keys = keyboard.keys + SettingsKeyboardKey()
        }
        assertObserved("SettingsKeyboardKey.functionData", { keyboardKey.functionData }) {
            keyboardKey.functionData = keyboardKey.functionData.copy(macroId = UUID.fromString(ID_1))
        }
        assertObserved("SettingsStreamDeckLayout.name", { layout.name }) { layout.name = "Deck" }
        assertObserved("SettingsRemoteControlAssistant.enabled", { remoteControl.assistant.enabled }) {
            remoteControl.assistant.enabled = true
        }
        assertObserved("SettingsRemoteControlServerRelay.baseUrl", { remoteControl.assistant.relay.baseUrl }) {
            remoteControl.assistant.relay.baseUrl = "wss://relay"
        }
        assertObserved("SettingsRemoteControlStreamer.savedUrls", { remoteControl.streamer.savedUrls }) {
            remoteControl.streamer.savedUrls = remoteControl.streamer.savedUrls + SettingsRemoteControlStreamerUrl()
        }
        assertObserved("SettingsRemoteControlWeb.port", { remoteControl.web.port }) {
            remoteControl.web.port = 8080
        }
        assertObserved("SettingsRemoteControl.selectedStreamer", { remoteControl.selectedStreamer }) {
            remoteControl.selectedStreamer = UUID.fromString(ID_2)
        }
        assertObserved("DeepLinkCreatorStream.video", { stream.video }) {
            stream.video = DeepLinkCreatorStreamVideo()
        }
        assertObserved("DeepLinkCreatorStreamVideo.bitrate", { stream.video.bitrate }) {
            stream.video.bitrate = 1_000_000
        }
        assertObserved("DeepLinkCreatorStreamAudio.bitrateFloat", { stream.audio.bitrateFloat }) {
            stream.audio.bitrate = 64_000
        }
        assertObserved("DeepLinkCreatorQuickButtons.twoColumns", { quickButtons.twoColumns }) {
            quickButtons.twoColumns = false
        }
        assertObserved("DeepLinkCreator.webBrowserEnabled", { deepLinkCreator.webBrowserEnabled }) {
            deepLinkCreator.webBrowserEnabled = true
        }
        val savedUrl = SettingsRemoteControlStreamerUrl(name = "Home", url = "ws://home")
        assertEquals("Home", savedUrl.name)
        assertEquals("ws://home", savedUrl.url)
        assertObserved("SettingsRemoteControlStreamerUrl.url", { savedUrl.url }) { savedUrl.url = "ws://away" }
        val streamer = SettingsRemoteControlStreamer(previewFps = 5f, savedUrls = listOf(savedUrl))
        assertEquals(5f, streamer.previewFps)
        assertEquals(listOf(savedUrl), streamer.savedUrls)
        assertObserved("SettingsRemoteControlStreamer.previewFps", { streamer.previewFps }) {
            streamer.previewFps = 2f
        }
        assertObserved("DeepLinkCreatorStreamSrt.dnsLookupStrategy", { stream.srt.dnsLookupStrategy }) {
            stream.srt.dnsLookupStrategy = SettingsDnsLookupStrategy.ipv4
        }
        assertObserved("DeepLinkCreatorStreamObs.webSocketUrl", { stream.obs.webSocketUrl }) {
            stream.obs.webSocketUrl = "ws://obs"
        }
        assertObserved("DeepLinkCreatorStreamTwitch.channelId", { stream.twitch.channelId }) {
            stream.twitch.channelId = "123"
        }
        assertObserved("DeepLinkCreatorStreamKick.channelName", { stream.kick.channelName }) {
            stream.kick.channelName = "kick"
        }
        val button = DeepLinkCreatorQuickButton()
        assertObserved("DeepLinkCreatorQuickButton.enabled", { button.enabled }) { button.enabled = true }
        assertObserved("DeepLinkCreatorWebBrowser.home", { deepLinkCreator.webBrowser.home }) {
            deepLinkCreator.webBrowser.home = "https://home"
        }
    }

    @Test
    fun inPlaceListMutationIsObserved() {
        val deepLinkCreator = DeepLinkCreator()
        val first = DeepLinkCreatorStream()
        val second = DeepLinkCreatorStream()
        assertObserved("DeepLinkCreator.streams add", { deepLinkCreator.streams.size }) {
            deepLinkCreator.streams.add(first)
        }
        assertObserved("DeepLinkCreator.streams append", { deepLinkCreator.streams.toList() }) {
            deepLinkCreator.streams.add(second)
        }
        assertObserved("DeepLinkCreator.streams move", { deepLinkCreator.streams.toList() }) {
            deepLinkCreator.streams.move(fromOffsets = listOf(1), toOffset = 0)
        }
        assertEquals(listOf(second, first), deepLinkCreator.streams.toList())
        assertObserved("DeepLinkCreator.streams remove", { deepLinkCreator.streams.toList() }) {
            deepLinkCreator.streams.removeAll { it === second }
        }
        assertEquals(listOf(first), deepLinkCreator.streams.toList())
        val quickButtons = DeepLinkCreatorQuickButtons()
        assertObserved("DeepLinkCreatorQuickButtons.buttons add", { quickButtons.buttons.toList() }) {
            quickButtons.buttons.add(DeepLinkCreatorQuickButton())
        }
        val decoded = fromJson(DeepLinkCreator.serializer(), toJson(DeepLinkCreator.serializer(), deepLinkCreator))
        assertObserved("decoded DeepLinkCreator.streams add", { decoded.streams.size }) {
            decoded.streams.add(DeepLinkCreatorStream())
        }
        assertEquals(2, decoded.streams.size)
    }
}

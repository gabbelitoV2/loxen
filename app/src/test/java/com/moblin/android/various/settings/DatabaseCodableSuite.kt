package com.moblin.android.various.settings

import com.moblin.android.platform.codable.codableJson
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val databaseSampleId = "E621E1F8-C36C-495A-93FC-0C247A3E6E5F"
private const val databaseOtherId = "0A1B2C3D-4E5F-4A6B-8C7D-9E0F1A2B3C4D"

private val swiftTopLevelKeys = listOf(
    "streams", "scenes", "widgets", "show", "zoom", "tapToFocus", "bitratePresets", "iconImage",
    "videoStabilizationMode", "chat", "mic", "mics", "debug", "quickButtons", "globalButtons", "rtmpServer",
    "networkInterfaceNames", "lowBitrateWarning", "vibrate", "gameControllers", "remoteControl",
    "startStopRecordingConfirmations", "color", "mirrorFrontCameraOnStream", "streamButtonColor", "location",
    "watch", "audio", "macros", "webBrowser", "deepLinkCreator", "srtlaServer", "mediaPlayers", "showAllSettings",
    "portrait", "djiDevices", "alertsMediaGallery", "catPrinters", "verboseStatuses", "scoreboardPlayers",
    "keyboard", "tesla", "srtlaRelay", "pixellateStrength", "moblink", "sceneSwitchTransition",
    "forceSceneSwitchTransition", "alwaysAttachCameraPreview", "alwaysAttachPhotoShoot", "photoShootFlash",
    "photoShootInterval", "cameraControlsEnabled",
    "externalDisplayContent", "cyclingPowerDevices", "cyclingPowerDevicesMigrated", "heartRateDevices",
    "phoneCoolerDevices", "remoteSceneId", "sceneNumericInput", "goPro", "replay", "portraitVideoOffsetFromTop",
    "autoSceneSwitchers", "fixedHorizon", "whirlpoolAngle", "pinchScale", "selfieStick", "bigButtons",
    "verticalButtons", "bigAudioLevelMeter", "ristServer", "disconnectProtection", "rtspClient", "srtClient",
    "whipServer", "whepClient", "navigation", "wiFiAware", "face", "beauty", "talkBack", "gimbal",
    "savedWifiNetworks", "streamDecks", "graphicsImplementation",
    "graphicsHighQualityDownsampling", "ingestsSoftwareVideoDecoding", "torchLevel", "appMode", "httpProxy",
)

private fun parseObject(json: String): JsonObject = codableJson.parseToJsonElement(json).jsonObject

private fun decodeDatabase(json: String): Database = Database.decode(parseObject(json))

private fun createDefaultDatabase(): Database {
    val method = Class.forName("com.moblin.android.various.settings.SettingsKt").getDeclaredMethod("createDefault")
    method.isAccessible = true
    return method.invoke(null) as Database
}

private fun withoutIsOn(buttons: JsonElement?): JsonArray {
    return JsonArray(buttons!!.jsonArray.map { JsonObject(it.jsonObject - "isOn") })
}

private fun JsonObject.without(path: List<String>): JsonObject {
    val key = path[0]
    if (path.size == 1) {
        return JsonObject(this - key)
    }
    val child = this[key] as? JsonObject ?: return this
    return JsonObject(this + (key to child.without(path.drop(1))))
}

private val swiftDecodeMigrationFlags = listOf(
    listOf("debug", "debugLoggingMigrated"),
    listOf("debug", "builtinAudioAndVideoDelay70msMigrated"),
    listOf("cyclingPowerDevicesMigrated"),
)

private val anyUuidPattern = Regex(
    "\"[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\"",
)

private fun maskAllUuids(json: JsonObject): String = anyUuidPattern.replace(json.toString(), "\"UUID\"")

private fun JsonObject.withoutMigrationFlags(): JsonObject {
    var result = this
    for (path in swiftDecodeMigrationFlags) {
        result = result.without(path)
    }
    return result
}

private val emptyDecodeDifferencePaths = listOf(
    listOf("cyclingPowerDevicesMigrated"),
    listOf("remoteControl", "password"),
    listOf("srtlaRelay", "client", "name"),
    listOf("moblink", "client", "name"),
)

private fun JsonObject.withoutEmptyDecodeDifferences(): JsonObject {
    var result = this
    for (path in emptyDecodeDifferencePaths) {
        result = result.without(path)
    }
    return result
}

private fun JsonObject.withoutSwiftDecodeChanges(): JsonObject {
    var result = this.without(listOf("goPro")).without(listOf("globalButtons"))
    for (path in swiftDecodeMigrationFlags) {
        result = result.without(path)
    }
    return result
}

private fun JsonObject.valueAt(path: List<String>): JsonElement? {
    var element: JsonElement? = this
    for (key in path) {
        element = (element as? JsonObject)?.get(key)
    }
    return element
}

@RunWith(RobolectricTestRunner::class)
class DatabaseCodableSuite {
    @Test
    fun defaultDatabaseEncodesSwiftTopLevelKeys() {
        val json = createDefaultDatabase().toJsonString()
        assertEquals(swiftTopLevelKeys, parseObject(json).keys.toList())
    }

    @Test
    fun defaultDatabaseRoundTrip() {
        val encoded = createDefaultDatabase().toJsonString()
        val first = Database.fromString(encoded).toJsonString()
        val second = Database.fromString(first).toJsonString()
        assertEquals(first, second)
        val before = parseObject(encoded)
        val after = parseObject(first)
        assertEquals(before.withoutSwiftDecodeChanges(), after.withoutSwiftDecodeChanges())
        assertEquals(withoutIsOn(before["globalButtons"]), withoutIsOn(after["globalButtons"]))
        for (path in swiftDecodeMigrationFlags) {
            assertEquals(JsonPrimitive(true), after.valueAt(path), path.joinToString("."))
        }
        assertEquals(1, after.valueAt(listOf("goPro", "launchLiveStream"))?.jsonArray?.size)
    }

    @Test
    fun emptyObjectDecodes() {
        val first = Database.fromString("{}").toJsonString()
        assertEquals(swiftTopLevelKeys, parseObject(first).keys.toList())
        val second = Database.fromString(first).toJsonString()
        assertEquals(parseObject(first).withoutSwiftDecodeChanges(), parseObject(second).withoutSwiftDecodeChanges())
        assertEquals(second, Database.fromString(second).toJsonString())
    }

    @Test
    fun settingsStoreAndLoad() {
        val settings = Settings()
        settings.reset()
        val loaded = Settings()
        loaded.load()
        assertEquals(settings.database.scenes.map { it.id }, loaded.database.scenes.map { it.id })
        assertEquals(
            settings.database.bitratePresets.map { it.id },
            loaded.database.bitratePresets.map { it.id },
        )
        loaded.database.tapToFocus = true
        loaded.database.talkback.micId.value = "talkback mic"
        loaded.store()
        val reloaded = Settings()
        reloaded.load()
        assertTrue(reloaded.database.tapToFocus)
        assertEquals("talkback mic", reloaded.database.talkback.micId.value)
        assertEquals(loaded.database.toJsonString(), reloaded.database.toJsonString())
    }

    @Test
    fun plainDatabaseRoundTrip() {
        val encoded = parseObject(Database().toJsonString())
        val decoded = parseObject(Database.decode(encoded).toJsonString())
        assertEquals(swiftTopLevelKeys, decoded.keys.toList())
        assertEquals(encoded.withoutMigrationFlags(), decoded.withoutMigrationFlags())
        for (path in swiftDecodeMigrationFlags) {
            assertEquals(JsonPrimitive(false), encoded.valueAt(path), path.joinToString("."))
            assertEquals(JsonPrimitive(true), decoded.valueAt(path), path.joinToString("."))
        }
        assertEquals(decoded.toString(), parseObject(Database.decode(decoded).toJsonString()).toString())
    }

    @Test
    fun plainEmptyObjectDecodesToDefault() {
        val expected = parseObject(Database().toJsonString())
        val decoded = parseObject(Database.decode(parseObject("{}")).toJsonString())
        assertEquals(swiftTopLevelKeys, decoded.keys.toList())
        assertEquals(
            maskAllUuids(expected.withoutEmptyDecodeDifferences()),
            maskAllUuids(decoded.withoutEmptyDecodeDifferences()),
        )
        assertEquals(JsonPrimitive(true), decoded["cyclingPowerDevicesMigrated"])
        assertEquals(JsonPrimitive(false), decoded.valueAt(listOf("debug", "debugLoggingMigrated")))
    }

    @Test
    fun swiftFormattedJsonDecodes() {
        val database = decodeDatabase(
            """
            {"replay":{"start":20,"stop":30,"speed":"0.5x"},
             "webBrowser":{"home":"https:\/\/moblin.org","bookmarks":[{"url":"https:\/\/a.com"}]},
             "streamButtonColor":{"red":1,"green":2,"blue":3},
             "zoom":{"speed":5,"backgroundColor":{"red":4,"green":5,"blue":6,"opacity":1}},
             "portraitVideoOffsetFromTop":0,
             "torchLevel":1,
             "debug":{"builtinAudioAndVideoDelay":0,"cameraManSpeed":2}}
            """,
        )
        assertEquals(20.0, database.replay.start)
        assertEquals(30.0, database.replay.stop)
        assertEquals(SettingsReplaySpeed.oneHalf, database.replay.speed)
        assertEquals("https://moblin.org", database.webBrowser.home)
        assertEquals("https://a.com", database.webBrowser.bookmarks.single().url)
        assertNull(database.streamButtonColor.opacity)
        assertEquals(5.0f, database.zoom.speed)
        assertEquals(1.0, database.zoom.backgroundColor.opacity)
        assertEquals(0.0, database.portraitVideoOffsetFromTop)
        assertEquals(1.0f, database.torchLevel)
        assertEquals(0.07, database.debug.builtinAudioAndVideoDelay.value)
        assertEquals(2.0, database.debug.cameraManSpeed.value)
    }

    @Test
    fun moblinkDefaultsToSrtlaRelay() {
        val database = decodeDatabase("""{"srtlaRelay":{}}""")
        assertSame(database.srtlaRelay, database.moblink)
        val separate = decodeDatabase("""{"srtlaRelay":{},"moblink":{}}""")
        assertTrue(separate.srtlaRelay !== separate.moblink)
    }

    @Test
    fun debugValuesAreDefaultsForMovedSettings() {
        val database = decodeDatabase("""{"debug":{"highQualityDownsampling":true,"httpProxy3":true}}""")
        assertTrue(database.graphicsHighQualityDownsampling)
        assertTrue(database.httpProxy.enabled.value)
        val explicit = decodeDatabase(
            """{"debug":{"highQualityDownsampling":true,"httpProxy3":true},
               "graphicsHighQualityDownsampling":false,"httpProxy":{}}""",
        )
        assertFalse(explicit.graphicsHighQualityDownsampling)
        assertFalse(explicit.httpProxy.enabled.value)
        val wrongType = decodeDatabase("""{"debug":{"httpProxy3":true},"httpProxy":"on"}""")
        assertTrue(wrongType.httpProxy.enabled.value)
    }

    @Test
    fun cyclingPowerDevicesMigrateToWorkoutDevices() {
        val database = decodeDatabase(
            """
            {"cyclingPowerDevices":{"devices":[
                {"id":"$databaseSampleId","name":"Bike","enabled":true,"bluetoothPeripheralName":"BT",
                 "bluetoothPeripheralId":"$databaseOtherId"}]}}
            """,
        )
        assertTrue(database.cyclingPowerDevicesMigrated)
        val device = database.workoutDevices.devices.single()
        assertEquals(UUID.fromString(databaseSampleId), device.id)
        assertEquals("Bike", device.name)
        assertTrue(device.enabled)
        assertEquals("BT", device.bluetoothPeripheralName)
        assertEquals(UUID.fromString(databaseOtherId), device.bluetoothPeripheralId)
        val migrated = decodeDatabase(
            """
            {"cyclingPowerDevicesMigrated":true,
             "cyclingPowerDevices":{"devices":[{"id":"$databaseSampleId","bluetoothPeripheralId":"$databaseOtherId"}]}}
            """,
        )
        assertTrue(migrated.workoutDevices.devices.isEmpty())
    }

    @Test
    fun sample() {
        val database = decodeDatabase(
            """
            {"tapToFocus":true,
             "bitratePresets":[{"id":"$databaseSampleId","bitrate":3000000}],
             "videoStabilizationMode":"Cinematic",
             "mic":"Top",
             "networkInterfaceNames":[{"id":"$databaseOtherId","interfaceName":"en0","name":"WiFi"}],
             "streamButtonColor":{"red":1,"green":2,"blue":3},
             "watch":{"chat":{"fontSize":20},"viaRemoteControl":true},
             "remoteSceneId":"$databaseSampleId",
             "externalDisplayContent":"Clean stream",
             "whirlpoolAngle":1.0,
             "talkBack":{"enabled":true,"micId":"m"},
             "savedWifiNetworks":[{"ssid":"Home","password":"p"}],
             "appMode":"chatPhone",
             "wiFiAware":{"enabled":true,"role":{"receiver":{}}}}
            """,
        )
        assertTrue(database.tapToFocus)
        assertEquals(3_000_000, database.bitratePresets.single().bitrate)
        assertEquals(SettingsVideoStabilizationMode.cinematic, database.videoStabilizationMode)
        assertEquals(SettingsMic.top, database.mic)
        assertEquals("en0", database.networkInterfaceNames.single().interfaceName)
        assertEquals(1, database.streamButtonColor.red)
        assertEquals(UUID.fromString(databaseSampleId), database.remoteSceneId)
        assertEquals(SettingsExternalDisplayContent.cleanStream, database.externalDisplayContent)
        assertEquals(1.0f, database.whirlpoolAngle)
        assertTrue(database.talkback.enabled.value)
        assertEquals("m", database.talkback.micId.value)
        assertEquals("Home", database.getSavedWiFiNetwork("Home")?.ssid)
        assertEquals(SettingsAppMode.chatPhone, database.appMode)
        assertEquals(SettingsWiFiAwareRole.receiver, database.wiFiAware.role)
        assertEquals(
            parseObject(
                """{"chat":{"fontSize":20.0,"timestampEnabled":true,"notificationOnMessage":false,
                   "notificationRate":30,"badges":true},
                   "show":{"thermalState":true,"audioLevel":true,"speed":true},"viaRemoteControl":true}""",
            ),
            database.watch,
        )
    }

    @Test
    fun wrongTypesFallBack() {
        val database = decodeDatabase(
            """
            {"tapToFocus":"yes","mic":"Side","videoStabilizationMode":"Wobbly","remoteSceneId":5,"watch":[],
             "networkInterfaceNames":[{"interfaceName":"en0","name":"WiFi"}],"gameControllers":7,
             "whirlpoolAngle":"x","show":[]}
            """,
        )
        assertFalse(database.tapToFocus)
        assertEquals(getDefaultMic(), database.mic)
        assertEquals(SettingsVideoStabilizationMode.off, database.videoStabilizationMode)
        assertNull(database.remoteSceneId)
        assertEquals(false, (database.watch["viaRemoteControl"] as JsonPrimitive).content.toBoolean())
        assertTrue(database.networkInterfaceNames.isEmpty())
        assertEquals(1, database.gameControllers.size)
        assertEquals((Math.PI / 2).toFloat(), database.whirlpoolAngle)
        assertTrue(database.show.chat)
        assertNotEquals(0, database.watch.size)
    }
}

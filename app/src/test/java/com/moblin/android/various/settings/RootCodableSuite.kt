package com.moblin.android.various.settings

import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.graphics.Color
import com.moblin.android.common.various.RgbColor
import com.moblin.android.platform.codable.codableJson
import com.moblin.android.platform.codable.decode
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.serializer
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private val uuidPattern = Regex("\"[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}\"")
private const val sampleId = "E621E1F8-C36C-495A-93FC-0C247A3E6E5F"
private const val otherId = "0A1B2C3D-4E5F-4A6B-8C7D-9E0F1A2B3C4D"

private fun maskUuids(json: String): String = uuidPattern.replace(json, "\"UUID\"")

private fun <T> encodeToString(serializer: KSerializer<T>, value: T): String {
    return codableJson.encodeToString(serializer, value)
}

private fun <T> decodeFromString(serializer: KSerializer<T>, json: String): T {
    return codableJson.decodeFromString(serializer, json)
}

private fun <T> assertRoundTrip(serializer: KSerializer<T>, value: T) {
    val json = encodeToString(serializer, value)
    assertEquals(json, encodeToString(serializer, decodeFromString(serializer, json)))
}

private fun <T> assertEmptyDecodesTo(serializer: KSerializer<T>, expected: T) {
    assertEquals(
        maskUuids(encodeToString(serializer, expected)),
        maskUuids(encodeToString(serializer, decodeFromString(serializer, "{}"))),
    )
}

private fun <T> assertKeys(serializer: KSerializer<T>, value: T, keys: List<String>) {
    assertEquals(keys, codableJson.parseToJsonElement(encodeToString(serializer, value)).jsonObject.keys.toList())
}

private fun <T> assertDefaults(serializer: KSerializer<T>, default: T, keys: List<String>) {
    assertKeys(serializer, default, keys)
    assertRoundTrip(serializer, default)
    assertEmptyDecodesTo(serializer, default)
}

private inline fun <reified T> assertRawValue(value: T, rawValue: String) {
    val serializer = codableJson.serializersModule.serializer<T>()
    assertEquals("\"$rawValue\"", codableJson.encodeToString(serializer, value))
    assertEquals(value, codableJson.decodeFromString(serializer, "\"$rawValue\""))
}

private inline fun <reified T> assertUnknownRawValueFallsBack(default: T) {
    val container = codableJson.parseToJsonElement("{\"value\":\"No such raw value\"}").jsonObject
    assertEquals(default, container.decode("value", default))
}

private fun observedReads(read: () -> Any?): Set<Any> {
    val reads = mutableSetOf<Any>()
    Snapshot.observe(readObserver = { reads.add(it) }) {
        read()
    }
    return reads
}

private fun observedWrites(write: () -> Unit): Set<Any> {
    val writes = mutableSetOf<Any>()
    val snapshot = Snapshot.takeMutableSnapshot(writeObserver = { writes.add(it) })
    try {
        snapshot.enter(write)
        snapshot.apply().check()
    } finally {
        snapshot.dispose()
    }
    return writes
}

private fun assertPublished(read: () -> Any?, write: () -> Unit) {
    val reads = observedReads(read)
    assertTrue(reads.isNotEmpty())
    assertTrue(observedWrites(write).any { it in reads })
}

@RunWith(RobolectricTestRunner::class)
class RootCodableSuite {
    @Test
    fun httpProxy() {
        assertEquals(
            "{\"enabled\":false,\"localNetwork\":false,\"port\":8450}",
            encodeToString(SettingsHttpProxy.serializer(), SettingsHttpProxy()),
        )
        assertDefaults(SettingsHttpProxy.serializer(), SettingsHttpProxy(), listOf("enabled", "localNetwork", "port"))
        val httpProxy = decodeFromString(
            SettingsHttpProxy.serializer(),
            """{"enabled":true,"localNetwork":true,"port":1234}""",
        )
        assertTrue(httpProxy.enabled.value)
        assertTrue(httpProxy.localNetwork.value)
        assertEquals(1234, httpProxy.port.value)
        val fallback = decodeFromString(
            SettingsHttpProxy.serializer(),
            """{"enabled":"yes","localNetwork":1,"port":"x"}""",
        )
        assertFalse(fallback.enabled.value)
        assertFalse(fallback.localNetwork.value)
        assertEquals(8450, fallback.port.value)
        assertEquals(8450, decodeFromString(SettingsHttpProxy.serializer(), """{"port":70000}""").port.value)
        assertEquals(8450, decodeFromString(SettingsHttpProxy.serializer(), """{"port":-1}""").port.value)
    }

    @Test
    fun navigation() {
        assertEquals(
            "{\"followUser\":false,\"followHeading\":false}",
            encodeToString(SettingsNavigation.serializer(), SettingsNavigation()),
        )
        assertDefaults(SettingsNavigation.serializer(), SettingsNavigation(), listOf("followUser", "followHeading"))
        val navigation = decodeFromString(
            SettingsNavigation.serializer(),
            """{"followUser":true,"followHeading":true}""",
        )
        assertTrue(navigation.followUser.value)
        assertTrue(navigation.followHeading.value)
        val fallback = decodeFromString(SettingsNavigation.serializer(), """{"followUser":"x","followHeading":true}""")
        assertFalse(fallback.followUser.value)
        assertTrue(fallback.followHeading.value)
    }

    @Test
    fun talkback() {
        assertEquals(
            "{\"enabled\":false,\"micId\":\"\"}",
            encodeToString(SettingsTalkback.serializer(), SettingsTalkback()),
        )
        assertDefaults(SettingsTalkback.serializer(), SettingsTalkback(), listOf("enabled", "micId"))
        val talkback = decodeFromString(SettingsTalkback.serializer(), """{"enabled":true,"micId":"mic 1"}""")
        assertTrue(talkback.enabled.value)
        assertEquals("mic 1", talkback.micId.value)
        val fallback = decodeFromString(SettingsTalkback.serializer(), """{"enabled":true,"micId":5}""")
        assertTrue(fallback.enabled.value)
        assertEquals("", fallback.micId.value)
    }

    @Test
    fun debugKeysAndMigrations() {
        val keys = listOf(
            "logLevel", "logFilter", "debugLogging", "debugLoggingMigrated", "srtOverlay",
            "cameraSwitchRemoveBlackish", "bluetoothOutputOnly", "maximumLogLines",
            "beautyFilterSettings", "nativeLowLightBoost", "blurSceneSwitch",
            "preferStereoMic", "twitchRewards", "tesla", "dnsLookupStrategy", "dataRateLimitFactor",
            "bitrateDropFix", "relaxedBitrate", "externalDisplayChat", "videoSourceWidgetTrackFace", "replay",
            "recordSegmentLength", "builtinAudioAndVideoDelay", "builtinAudioAndVideoDelay70msMigrated",
            "cameraManMoveVertically", "cameraManSpeed", "cameraManAlwaysMove", "enhancedMoblinSrt",
            "videoBitrateChangeEnabled", "highQualityDownsampling", "httpProxy3", "packetPadding",
            "externalCameraVideoRange",
        )
        assertKeys(SettingsDebug.serializer(), SettingsDebug(), keys)
        val migrated = SettingsDebug()
        migrated.debugLoggingMigrated = true
        migrated.builtinAudioAndVideoDelay70msMigrated = true
        assertRoundTrip(SettingsDebug.serializer(), migrated)
        assertEmptyDecodesTo(SettingsDebug.serializer(), migrated)
        val decodedDefault = decodeFromString(
            SettingsDebug.serializer(),
            encodeToString(SettingsDebug.serializer(), SettingsDebug()),
        )
        assertEquals(
            encodeToString(SettingsDebug.serializer(), migrated),
            encodeToString(SettingsDebug.serializer(), decodedDefault),
        )
        assertTrue(decodeFromString(SettingsDebug.serializer(), """{"logLevel":"Debug"}""").debugLogging.value)
        assertFalse(
            decodeFromString(
                SettingsDebug.serializer(),
                """{"logLevel":"Debug","debugLogging":false,"debugLoggingMigrated":true}""",
            ).debugLogging.value,
        )
        assertEquals(
            0.07,
            decodeFromString(SettingsDebug.serializer(), """{"builtinAudioAndVideoDelay":0}""")
                .builtinAudioAndVideoDelay.value,
        )
        assertEquals(
            0.0,
            decodeFromString(
                SettingsDebug.serializer(),
                """{"builtinAudioAndVideoDelay":0,"builtinAudioAndVideoDelay70msMigrated":true}""",
            ).builtinAudioAndVideoDelay.value,
        )
    }

    @Test
    fun debugSample() {
        val debug = decodeFromString(
            SettingsDebug.serializer(),
            """
            {
                "logLevel": "Info",
                "logFilter": "srt",
                "srtOverlay": true,
                "cameraSwitchRemoveBlackish": 0.5,
                "bluetoothOutputOnly": false,
                "maximumLogLines": 1000,
                "beautyFilterSettings": {"privacyMode": "pixellate", "blurStrength": 0.25},
                "nativeLowLightBoost": true,
                "preferStereoMic": true,
                "tesla": {"vin": "VIN", "bluetoothPeripheralId": "$sampleId"},
                "dnsLookupStrategy": "IPv4 and IPv6",
                "dataRateLimitFactor": 1.5,
                "recordSegmentLength": 10.0,
                "builtinAudioAndVideoDelay": 0.2,
                "cameraManSpeed": 2.5,
                "videoBitrateChangeEnabled": true,
                "highQualityDownsampling": true,
                "httpProxy3": true,
                "packetPadding": true
            }
            """,
        )
        assertEquals(SettingsLogLevel.info, debug.logLevel)
        assertEquals("srt", debug.logFilter.value)
        assertFalse(debug.debugLogging.value)
        assertTrue(debug.debugLoggingMigrated)
        assertTrue(debug.debugOverlay.value)
        assertEquals(0.5f, debug.cameraSwitchRemoveBlackish.value)
        assertFalse(debug.bluetoothOutputOnly.value)
        assertEquals(1000, debug.maximumLogLines)
        assertEquals(SettingsFacePrivacyMode.pixellate, debug.faceToBeRemoved.privacyMode)
        assertEquals(0.25f, debug.faceToBeRemoved.blurStrength)
        assertTrue(debug.nativeLowLightBoost.value)
        assertTrue(debug.preferStereoMicToBeRemoved)
        assertEquals("VIN", debug.tesla.vin)
        assertEquals(UUID.fromString(sampleId), debug.tesla.bluetoothPeripheralId)
        assertEquals(SettingsDnsLookupStrategy.ipv4AndIpv6, debug.dnsLookupStrategy)
        assertEquals(1.5f, debug.dataRateLimitFactor.value)
        assertEquals(10.0, debug.recordSegmentLength)
        assertEquals(0.2, debug.builtinAudioAndVideoDelay.value)
        assertEquals(2.5, debug.cameraManSpeed.value)
        assertTrue(debug.videoBitrateChange.value)
        assertTrue(debug.highQualityDownsamplingToBeRemoved)
        assertTrue(debug.httpProxyToBeRemoved)
        assertTrue(debug.packetPadding.value)
        val json = encodeToString(SettingsDebug.serializer(), debug)
        assertTrue(json.contains("\"srtOverlay\":true"))
        assertTrue(json.contains("\"dnsLookupStrategy\":\"IPv4 and IPv6\""))
        assertTrue(json.contains("\"bluetoothPeripheralId\":\"$sampleId\""))
        assertRoundTrip(SettingsDebug.serializer(), debug)
    }

    @Test
    fun debugWrongTypes() {
        val debug = decodeFromString(
            SettingsDebug.serializer(),
            """
            {"logLevel":"Verbose","maximumLogLines":"x","tesla":5,"dnsLookupStrategy":"DNS",
             "beautyFilterSettings":[],"cameraManSpeed":"fast","srtOverlay":1}
            """,
        )
        assertEquals(SettingsLogLevel.error, debug.logLevel)
        assertEquals(500, debug.maximumLogLines)
        assertEquals("", debug.tesla.vin)
        assertEquals(SettingsDnsLookupStrategy.system, debug.dnsLookupStrategy)
        assertEquals(SettingsFacePrivacyMode.blur, debug.faceToBeRemoved.privacyMode)
        assertEquals(1.0, debug.cameraManSpeed.value)
        assertFalse(debug.debugOverlay.value)
    }

    @Test
    fun mic() {
        assertRawValue(SettingsMic.bottom, "Bottom")
        assertRawValue(SettingsMic.front, "Front")
        assertRawValue(SettingsMic.back, "Back")
        assertRawValue(SettingsMic.top, "Top")
        assertEquals(getDefaultMic(), decodeFromString(SettingsMic.serializer(), "\"Side\""))
    }

    @Test
    fun micsMic() {
        assertEquals(
            "{\"name\":\"\",\"inputUid\":\"\",\"dataSourceID\":null,\"builtInOrientation\":null,\"delay\":0.0}",
            encodeToString(SettingsMicsMic.serializer(), SettingsMicsMic()),
        )
        assertDefaults(
            SettingsMicsMic.serializer(),
            SettingsMicsMic(),
            listOf("name", "inputUid", "dataSourceID", "builtInOrientation", "delay"),
        )
        val mic = decodeFromString(
            SettingsMicsMic.serializer(),
            """{"name":"USB","inputUid":"uid","dataSourceID":3,"builtInOrientation":"Top","delay":0.5}""",
        )
        assertEquals("USB", mic.name)
        assertEquals("uid", mic.inputUid)
        assertEquals(3, mic.dataSourceId)
        assertEquals(SettingsMic.top, mic.builtInOrientation)
        assertEquals(0.5, mic.delay.value)
        assertRoundTrip(SettingsMicsMic.serializer(), mic)
        val fallback = decodeFromString(
            SettingsMicsMic.serializer(),
            """{"name":1,"dataSourceID":"x","builtInOrientation":5,"delay":"y"}""",
        )
        assertEquals("", fallback.name)
        assertNull(fallback.dataSourceId)
        assertNull(fallback.builtInOrientation)
        assertEquals(0.0, fallback.delay.value)
        assertEquals(
            getDefaultMic(),
            decodeFromString(SettingsMicsMic.serializer(), """{"builtInOrientation":"Side"}""").builtInOrientation,
        )
    }

    @Test
    fun mics() {
        assertDefaults(SettingsMics.serializer(), SettingsMics(), listOf("all", "autoSwitch", "defaultMic"))
        val mics = decodeFromString(
            SettingsMics.serializer(),
            """{"all":[{"name":"A","inputUid":"a"},{"name":"B","inputUid":"b","dataSourceID":2}],
               "autoSwitch":false,"defaultMic":"a 0"}""",
        )
        assertEquals(listOf("A", "B"), mics.mics.value.map { it.name })
        assertEquals(2, mics.mics.value[1].dataSourceId)
        assertFalse(mics.autoSwitch.value)
        assertEquals("a 0", mics.defaultMic)
        assertRoundTrip(SettingsMics.serializer(), mics)
        val fallback = decodeFromString(SettingsMics.serializer(), """{"all":5,"autoSwitch":"no"}""")
        assertTrue(fallback.mics.value.isEmpty())
        assertTrue(fallback.autoSwitch.value)
    }

    @Test
    fun audioOutputToInputChannelsMapIsSynthesized() {
        val map = SettingsAudioOutputToInputChannelsMap()
        assertEquals(
            "{\"channel1\":0,\"channel2\":1}",
            encodeToString(SettingsAudioOutputToInputChannelsMap.serializer(), map),
        )
        assertRoundTrip(SettingsAudioOutputToInputChannelsMap.serializer(), map)
        val decoded = decodeFromString(
            SettingsAudioOutputToInputChannelsMap.serializer(),
            """{"channel1":3,"channel2":-1}""",
        )
        assertEquals(3, decoded.channel1)
        assertEquals(-1, decoded.channel2)
        assertFailsWith<Exception> {
            decodeFromString(SettingsAudioOutputToInputChannelsMap.serializer(), "{}")
        }
        assertFailsWith<Exception> {
            decodeFromString(SettingsAudioOutputToInputChannelsMap.serializer(), """{"channel1":3}""")
        }
        assertFailsWith<Exception> {
            decodeFromString(SettingsAudioOutputToInputChannelsMap.serializer(), """{"channel1":3,"channel2":"x"}""")
        }
    }

    @Test
    fun audio() {
        assertEquals(
            "{\"audioOutputToInputChannelsMap\":{\"channel1\":0,\"channel2\":1},\"gainDb\":0.0,\"preferStereoMic\":false}",
            encodeToString(SettingsAudio.serializer(), SettingsAudio()),
        )
        assertDefaults(
            SettingsAudio.serializer(),
            SettingsAudio(),
            listOf("audioOutputToInputChannelsMap", "gainDb", "preferStereoMic"),
        )
        val audio = decodeFromString(
            SettingsAudio.serializer(),
            """{"audioOutputToInputChannelsMap":{"channel1":2,"channel2":3},"gainDb":-6.5,"preferStereoMic":true}""",
        )
        assertEquals(2, audio.outputToInputChannelsMap.channel1)
        assertEquals(3, audio.outputToInputChannelsMap.channel2)
        assertEquals(-6.5f, audio.gainDb.value)
        assertTrue(audio.preferStereoMic.value)
        val fallback = decodeFromString(
            SettingsAudio.serializer(),
            """{"audioOutputToInputChannelsMap":{"channel1":2},"gainDb":"loud","preferStereoMic":true}""",
        )
        assertEquals(0, fallback.outputToInputChannelsMap.channel1)
        assertEquals(1, fallback.outputToInputChannelsMap.channel2)
        assertEquals(0.0f, fallback.gainDb.value)
        assertTrue(fallback.preferStereoMic.value)
    }

    @Test
    fun enumsUseSwiftRawValues() {
        assertRawValue(SettingsLogLevel.error, "Error")
        assertRawValue(SettingsLogLevel.info, "Info")
        assertRawValue(SettingsLogLevel.debug, "Debug")
        assertRawValue(SettingsColorLutType.bundled, "bundled")
        assertRawValue(SettingsColorLutType.disk, "disk")
        assertRawValue(SettingsColorLutType.diskCube, "diskCube")
        assertRawValue(SettingsColorSpace.srgb, "Standard RGB")
        assertRawValue(SettingsColorSpace.p3D65, "P3 D65")
        assertRawValue(SettingsColorSpace.hlgBt2020, "HLG BT2020")
        assertRawValue(SettingsColorSpace.appleLog, "Apple Log")
        assertRawValue(SettingsVideoStabilizationMode.off, "Off")
        assertRawValue(SettingsVideoStabilizationMode.standard, "Standard")
        assertRawValue(SettingsVideoStabilizationMode.cinematic, "Cinematic")
        assertRawValue(SettingsVideoStabilizationMode.cinematicExtendedEnhanced, "Cinematic extended enhanced")
        assertRawValue(SettingsDnsLookupStrategy.system, "System")
        assertRawValue(SettingsDnsLookupStrategy.ipv4, "IPv4")
        assertRawValue(SettingsDnsLookupStrategy.ipv6, "IPv6")
        assertRawValue(SettingsDnsLookupStrategy.ipv4AndIpv6, "IPv4 and IPv6")
        assertRawValue(SettingsReplaySpeed.oneHalf, "0.5x")
        assertRawValue(SettingsReplaySpeed.one, "1x")
        assertRawValue(SettingsExternalDisplayContent.stream, "Stream")
        assertRawValue(SettingsExternalDisplayContent.cleanStream, "Clean stream")
        assertRawValue(SettingsExternalDisplayContent.chat, "Chat")
        assertRawValue(SettingsExternalDisplayContent.mirror, "Mirror")
        assertRawValue(SettingsAppMode.streaming, "streaming")
        assertRawValue(SettingsAppMode.chatPhone, "chatPhone")
        assertRawValue(SettingsFacePrivacyMode.blur, "blur")
        assertRawValue(SettingsFacePrivacyMode.pixellate, "pixellate")
        assertRawValue(SettingsFacePrivacyMode.backgroundImage, "backgroundImage")
        assertRawValue(SettingsFacePrivacyMode.icon, "icon")
        for (entry in SettingsVideoStabilizationMode.entries) {
            assertRawValue(entry, entry.rawValue)
        }
        assertUnknownRawValueFallsBack(SettingsLogLevel.error)
        assertUnknownRawValueFallsBack(SettingsColorLutType.bundled)
        assertUnknownRawValueFallsBack(SettingsColorSpace.srgb)
        assertUnknownRawValueFallsBack(SettingsVideoStabilizationMode.off)
        assertUnknownRawValueFallsBack(SettingsDnsLookupStrategy.system)
        assertUnknownRawValueFallsBack(SettingsReplaySpeed.one)
        assertUnknownRawValueFallsBack(SettingsExternalDisplayContent.stream)
        assertUnknownRawValueFallsBack(SettingsAppMode.streaming)
        assertUnknownRawValueFallsBack(SettingsFacePrivacyMode.blur)
    }

    @Test
    fun wiFiAwareRoleIsSynthesizedEnum() {
        assertEquals("{\"sender\":{}}", encodeToString(SettingsWiFiAwareRole.serializer(), SettingsWiFiAwareRole.sender))
        assertEquals(
            "{\"receiver\":{}}",
            encodeToString(SettingsWiFiAwareRole.serializer(), SettingsWiFiAwareRole.receiver),
        )
        assertEquals(
            SettingsWiFiAwareRole.receiver,
            decodeFromString(SettingsWiFiAwareRole.serializer(), """{"receiver":{}}"""),
        )
        assertEquals(
            SettingsWiFiAwareRole.sender,
            decodeFromString(SettingsWiFiAwareRole.serializer(), """{"sender":{},"other":1}"""),
        )
        assertFailsWith<Exception> { decodeFromString(SettingsWiFiAwareRole.serializer(), "{}") }
        assertFailsWith<Exception> { decodeFromString(SettingsWiFiAwareRole.serializer(), "\"receiver\"") }
        assertFailsWith<Exception> { decodeFromString(SettingsWiFiAwareRole.serializer(), """{"receiver":1}""") }
        assertFailsWith<Exception> {
            decodeFromString(SettingsWiFiAwareRole.serializer(), """{"sender":{},"receiver":{}}""")
        }
    }

    @Test
    fun wiFiAware() {
        assertEquals(
            "{\"enabled\":false,\"role\":{\"sender\":{}}}",
            encodeToString(SettingsWiFiAware.serializer(), SettingsWiFiAware()),
        )
        assertDefaults(SettingsWiFiAware.serializer(), SettingsWiFiAware(), listOf("enabled", "role"))
        val wiFiAware = decodeFromString(SettingsWiFiAware.serializer(), """{"enabled":true,"role":{"receiver":{}}}""")
        assertTrue(wiFiAware.enabled)
        assertEquals(SettingsWiFiAwareRole.receiver, wiFiAware.role)
        val fallback = decodeFromString(SettingsWiFiAware.serializer(), """{"enabled":true,"role":"receiver"}""")
        assertEquals(SettingsWiFiAwareRole.sender, fallback.role)
    }

    @Test
    fun colorLut() {
        assertDefaults(SettingsColorLut.serializer(), SettingsColorLut(), listOf("id", "type", "name", "enabled"))
        val lut = decodeFromString(
            SettingsColorLut.serializer(),
            """{"id":"$sampleId","type":"diskCube","name":"Film","enabled":true}""",
        )
        assertEquals(UUID.fromString(sampleId), lut.id)
        assertEquals(SettingsColorLutType.diskCube, lut.type)
        assertEquals("Film", lut.name)
        assertTrue(lut.enabled)
        assertEquals(
            "{\"id\":\"$sampleId\",\"type\":\"diskCube\",\"name\":\"Film\",\"enabled\":true}",
            encodeToString(SettingsColorLut.serializer(), lut),
        )
        val lowercase = decodeFromString(SettingsColorLut.serializer(), """{"id":"${sampleId.lowercase()}"}""")
        assertEquals(UUID.fromString(sampleId), lowercase.id)
        val fallback = decodeFromString(SettingsColorLut.serializer(), """{"id":"nope","type":"png","enabled":"x"}""")
        assertNotEquals(UUID.fromString(sampleId), fallback.id)
        assertEquals(SettingsColorLutType.bundled, fallback.type)
        assertFalse(fallback.enabled)
    }

    @Test
    fun color() {
        val color = SettingsColor()
        assertKeys(
            SettingsColor.serializer(),
            color,
            listOf("space", "lutEnabled", "lut", "bundledLuts", "diskLuts", "diskLutsPng", "diskLutsCube"),
        )
        assertEquals(2, color.bundledLuts.size)
        assertRoundTrip(SettingsColor.serializer(), color)
        assertEmptyDecodesTo(SettingsColor.serializer(), SettingsColor(bundledLuts = emptyList()))
        val decoded = decodeFromString(
            SettingsColor.serializer(),
            """
            {"space":"Apple Log","lutEnabled":false,"lut":"$sampleId",
             "bundledLuts":[{"id":"$otherId","type":"bundled","name":"Moblin Meme","enabled":true}],
             "diskLuts":[{"name":"Old","type":"disk"}],
             "diskLutsCube":[{"name":"Cube","type":"diskCube"}]}
            """,
        )
        assertEquals(SettingsColorSpace.appleLog, decoded.space)
        assertFalse(decoded.lutEnabled)
        assertEquals(UUID.fromString(sampleId), decoded.lut)
        assertEquals(UUID.fromString(otherId), decoded.bundledLuts[0].id)
        assertTrue(decoded.bundledLuts[0].enabled)
        assertEquals(SettingsColorLutType.disk, decoded.diskLuts[0].type)
        assertTrue(decoded.diskLutsPng.isEmpty())
        assertEquals("Cube", decoded.diskLutsCube[0].name)
        val fallback = decodeFromString(SettingsColor.serializer(), """{"space":"Rec 2020","bundledLuts":{}}""")
        assertEquals(SettingsColorSpace.srgb, fallback.space)
        assertTrue(fallback.bundledLuts.isEmpty())
    }

    @Test
    fun show() {
        val keys = listOf(
            "chat", "viewers", "uptime", "stream", "speed", "audioLevel", "zoom", "zoomPresets", "microphone",
            "audioBar", "cameras", "obsStatus", "rtmpSpeed", "gameController", "location", "remoteControl",
            "browserWidgets", "bonding", "events", "djiDevices", "bondingRtts", "moblink", "catPrinter",
            "heartRateDevice", "cpu",
        )
        assertDefaults(SettingsShow.serializer(), SettingsShow(), keys)
        val show = decodeFromString(
            SettingsShow.serializer(),
            """{"chat":false,"rtmpSpeed":false,"heartRateDevice":false,"cpu":true,"stream":true}""",
        )
        assertFalse(show.chat)
        assertFalse(show.ingests)
        assertFalse(show.workoutDevice)
        assertTrue(show.systemMonitor)
        assertTrue(show.stream)
        val fallback = decodeFromString(SettingsShow.serializer(), """{"chat":"no","cpu":0}""")
        assertTrue(fallback.chat)
        assertFalse(fallback.systemMonitor)
    }

    @Test
    fun zoom() {
        assertDefaults(SettingsZoomPreset.serializer(), SettingsZoomPreset(), listOf("id", "name", "x"))
        assertDefaults(SettingsZoomSwitchTo.serializer(), SettingsZoomSwitchTo(), listOf("level", "x", "enabled"))
        assertDefaults(
            SettingsZoom.serializer(),
            SettingsZoom(),
            listOf("back", "front", "switchToBack", "switchToFront", "speed", "backgroundColor"),
        )
        val zoom = decodeFromString(
            SettingsZoom.serializer(),
            """
            {"back":[{"id":"$sampleId","name":"0.5x","x":0.5},{"name":"2x","x":2}],
             "front":[],
             "switchToBack":{"level":2.0,"x":3.0,"enabled":true},
             "speed":7.5,
             "backgroundColor":{"red":1,"green":2,"blue":3}}
            """,
        )
        assertEquals(2, zoom.back.size)
        assertEquals(UUID.fromString(sampleId), zoom.back[0].id)
        assertEquals("0.5x", zoom.back[0].name)
        assertEquals(0.5f, zoom.back[0].x)
        assertEquals(2.0f, zoom.back[1].x)
        assertTrue(zoom.front.isEmpty())
        assertEquals(2.0f, zoom.switchToBack.level)
        assertEquals(3.0f, zoom.switchToBack.x)
        assertTrue(zoom.switchToBack.enabled)
        assertFalse(zoom.switchToFront.enabled)
        assertEquals(7.5f, zoom.speed)
        assertEquals(RgbColor(red = 1, green = 2, blue = 3), zoom.backgroundColor)
        assertEquals(1.0f, zoom.backgroundColorColor.alpha)
        zoom.back.add(SettingsZoomPreset())
        val fallback = decodeFromString(
            SettingsZoom.serializer(),
            """{"back":[{"x":"big"}],"switchToFront":[],"speed":"fast","backgroundColor":{"red":1}}""",
        )
        assertEquals(1.0f, fallback.back[0].x)
        assertEquals(1.0f, fallback.switchToFront.level)
        assertEquals(5.0f, fallback.speed)
        assertEquals(defaultSegmentedPickerSelectedColor, fallback.backgroundColor)
        assertEquals(0.6f, fallback.backgroundColorColor.alpha, 0.01f)
    }

    @Test
    fun bitratePreset() {
        assertDefaults(SettingsBitratePreset.serializer(), SettingsBitratePreset(), listOf("id", "bitrate"))
        val preset = decodeFromString(SettingsBitratePreset.serializer(), """{"id":"$sampleId","bitrate":12000000}""")
        assertEquals(UUID.fromString(sampleId), preset.id)
        assertEquals(12_000_000, preset.bitrate)
        assertEquals(5_000_000, decodeFromString(SettingsBitratePreset.serializer(), """{"bitrate":-1}""").bitrate)
        assertEquals(5_000_000, decodeFromString(SettingsBitratePreset.serializer(), """{"bitrate":"x"}""").bitrate)
    }

    @Test
    fun tesla() {
        assertEquals(
            "{\"vin\":\"\",\"privateKey\":\"\",\"enabled\":true,\"bluetoothPeripheralName\":null," +
                "\"bluetoothPeripheralId\":null}",
            encodeToString(SettingsTesla.serializer(), SettingsTesla()),
        )
        assertDefaults(
            SettingsTesla.serializer(),
            SettingsTesla(),
            listOf("vin", "privateKey", "enabled", "bluetoothPeripheralName", "bluetoothPeripheralId"),
        )
        val tesla = decodeFromString(
            SettingsTesla.serializer(),
            """{"vin":"V","privateKey":"K","enabled":false,"bluetoothPeripheralName":"Car",
               "bluetoothPeripheralId":"$sampleId"}""",
        )
        assertEquals("V", tesla.vin)
        assertEquals("K", tesla.privateKey)
        assertFalse(tesla.enabled)
        assertEquals("Car", tesla.bluetoothPeripheralName)
        assertEquals(UUID.fromString(sampleId), tesla.bluetoothPeripheralId)
        assertRoundTrip(SettingsTesla.serializer(), tesla)
        val fallback = decodeFromString(
            SettingsTesla.serializer(),
            """{"enabled":"x","bluetoothPeripheralName":7,"bluetoothPeripheralId":"not a uuid"}""",
        )
        assertTrue(fallback.enabled)
        assertNull(fallback.bluetoothPeripheralName)
        assertNull(fallback.bluetoothPeripheralId)
    }

    @Test
    fun mediaPlayers() {
        assertDefaults(SettingsMediaPlayerFile.serializer(), SettingsMediaPlayerFile(), listOf("id", "name"))
        assertDefaults(
            SettingsMediaPlayer.serializer(),
            SettingsMediaPlayer(),
            listOf("id", "name", "playerId", "autoSelectMic", "playlist"),
        )
        assertDefaults(SettingsMediaPlayers.serializer(), SettingsMediaPlayers(), listOf("players"))
        val players = decodeFromString(
            SettingsMediaPlayers.serializer(),
            """
            {"players":[{"id":"$sampleId","name":"Player","playerId":"p","autoSelectMic":false,
                         "playlist":[{"id":"$otherId","name":"Clip"},{}]}]}
            """,
        )
        val player = players.players[0]
        assertEquals(UUID.fromString(sampleId), player.id)
        assertEquals("Player", player.name)
        assertEquals("p", player.playerId)
        assertFalse(player.autoSelectMic)
        assertEquals(UUID.fromString(otherId), player.playlist[0].id)
        assertEquals("Clip", player.playlist[0].name)
        assertEquals("My video", player.playlist[1].name)
        player.playlist.add(SettingsMediaPlayerFile())
        val fallback = decodeFromString(
            SettingsMediaPlayers.serializer(),
            """{"players":[{"name":3,"playlist":"none"}]}""",
        )
        assertEquals(SettingsMediaPlayer.baseName, fallback.players[0].name)
        assertTrue(fallback.players[0].playlist.isEmpty())
        assertTrue(decodeFromString(SettingsMediaPlayers.serializer(), """{"players":5}""").players.isEmpty())
    }

    @Test
    fun replay() {
        assertEquals(
            "{\"start\":20.0,\"stop\":30.0,\"speed\":\"1x\"}",
            encodeToString(SettingsReplay.serializer(), SettingsReplay()),
        )
        assertDefaults(SettingsReplay.serializer(), SettingsReplay(), listOf("start", "stop", "speed"))
        val replay = decodeFromString(SettingsReplay.serializer(), """{"start":5,"stop":15.5,"speed":"0.5x"}""")
        assertEquals(5.0, replay.start)
        assertEquals(15.5, replay.stop)
        assertEquals(SettingsReplaySpeed.oneHalf, replay.speed)
        val fallback = decodeFromString(SettingsReplay.serializer(), """{"start":"x","speed":"2x"}""")
        assertEquals(20.0, fallback.start)
        assertEquals(SettingsReplaySpeed.one, fallback.speed)
    }

    @Test
    fun cyclingPowerDevices() {
        val device = SettingsCyclingPowerDevice()
        assertEquals("", device.name)
        assertKeys(
            SettingsCyclingPowerDevice.serializer(),
            device,
            listOf("id", "name", "enabled", "bluetoothPeripheralName", "bluetoothPeripheralId"),
        )
        assertRoundTrip(SettingsCyclingPowerDevice.serializer(), device)
        assertEmptyDecodesTo(
            SettingsCyclingPowerDevice.serializer(),
            SettingsCyclingPowerDevice(name = SettingsCyclingPowerDevice.baseName),
        )
        assertDefaults(SettingsCyclingPowerDevices.serializer(), SettingsCyclingPowerDevices(), listOf("devices"))
        val devices = decodeFromString(
            SettingsCyclingPowerDevices.serializer(),
            """{"devices":[{"id":"$sampleId","name":"Bike","enabled":true,"bluetoothPeripheralName":"BT",
                            "bluetoothPeripheralId":"$otherId"}]}""",
        )
        assertEquals(UUID.fromString(sampleId), devices.devices[0].id)
        assertEquals("Bike", devices.devices[0].name)
        assertTrue(devices.devices[0].enabled)
        assertEquals("BT", devices.devices[0].bluetoothPeripheralName)
        assertEquals(UUID.fromString(otherId), devices.devices[0].bluetoothPeripheralId)
        val fallback = decodeFromString(
            SettingsCyclingPowerDevice.serializer(),
            """{"enabled":[],"bluetoothPeripheralName":1,"bluetoothPeripheralId":"x"}""",
        )
        assertFalse(fallback.enabled)
        assertNull(fallback.bluetoothPeripheralName)
        assertNull(fallback.bluetoothPeripheralId)
    }

    @Test
    fun workoutDevices() {
        assertDefaults(
            SettingsWorkoutDevice.serializer(),
            SettingsWorkoutDevice(),
            listOf("id", "name", "enabled", "bluetoothPeripheralName", "bluetoothPeripheralId", "wheelCircumference"),
        )
        assertDefaults(SettingsWorkoutDevices.serializer(), SettingsWorkoutDevices(), listOf("devices"))
        val devices = decodeFromString(
            SettingsWorkoutDevices.serializer(),
            """{"devices":[{"id":"$sampleId","name":"HR","enabled":true,"bluetoothPeripheralId":"$otherId",
                            "wheelCircumference":2000}]}""",
        )
        val device = devices.devices[0]
        assertEquals(UUID.fromString(sampleId), device.id)
        assertEquals("HR", device.name)
        assertTrue(device.enabled)
        assertNull(device.bluetoothPeripheralName)
        assertEquals(UUID.fromString(otherId), device.bluetoothPeripheralId)
        assertEquals(2000, device.wheelCircumference)
        devices.devices.add(SettingsWorkoutDevice())
        val fallback = decodeFromString(SettingsWorkoutDevice.serializer(), """{"wheelCircumference":"big"}""")
        assertEquals(defaultWheelCircumference, fallback.wheelCircumference)
    }

    @Test
    fun blackSharkCoolerDevices() {
        assertDefaults(
            SettingsBlackSharkCoolerDevice.serializer(),
            SettingsBlackSharkCoolerDevice(),
            listOf(
                "id", "name", "enabled", "bluetoothPeripheralName", "bluetoothPeripheralId", "rgbLightEnabled",
                "rgbLightColor", "rgbLightBrightness",
            ),
        )
        assertDefaults(SettingsBlackSharkCoolerDevices.serializer(), SettingsBlackSharkCoolerDevices(), listOf("devices"))
        val devices = decodeFromString(
            SettingsBlackSharkCoolerDevices.serializer(),
            """{"devices":[{"name":"Cooler","rgbLightEnabled":true,"rgbLightColor":{"red":255,"green":0,"blue":0},
                            "rgbLightBrightness":50}]}""",
        )
        val device = devices.devices[0]
        assertEquals("Cooler", device.name)
        assertTrue(device.rgbLightEnabled)
        assertEquals(RgbColor(red = 255, green = 0, blue = 0), device.rgbLightColor)
        assertEquals(1.0f, device.rgbLightColorColor.red)
        assertEquals(50.0, device.rgbLightBrightness)
        val fallback = decodeFromString(
            SettingsBlackSharkCoolerDevice.serializer(),
            """{"rgbLightColor":"red","rgbLightBrightness":"max"}""",
        )
        assertEquals(RgbColor(red = 0, green = 255, blue = 0), fallback.rgbLightColor)
        assertEquals(1.0f, fallback.rgbLightColorColor.green)
        assertEquals(100.0, fallback.rgbLightBrightness)
    }

    @Test
    fun networkInterfaceNameIsSynthesized() {
        val name = SettingsNetworkInterfaceName(id = UUID.fromString(sampleId), interfaceName = "en0", name = "WiFi")
        assertEquals(
            "{\"id\":\"$sampleId\",\"interfaceName\":\"en0\",\"name\":\"WiFi\"}",
            encodeToString(SettingsNetworkInterfaceName.serializer(), name),
        )
        assertRoundTrip(SettingsNetworkInterfaceName.serializer(), SettingsNetworkInterfaceName())
        val decoded = decodeFromString(
            SettingsNetworkInterfaceName.serializer(),
            """{"id":"$sampleId","interfaceName":"pdp_ip0","name":"Cellular"}""",
        )
        assertEquals(UUID.fromString(sampleId), decoded.id)
        assertEquals("pdp_ip0", decoded.interfaceName)
        assertEquals("Cellular", decoded.name)
        assertFailsWith<Exception> { decodeFromString(SettingsNetworkInterfaceName.serializer(), "{}") }
        assertFailsWith<Exception> {
            decodeFromString(SettingsNetworkInterfaceName.serializer(), """{"id":"$sampleId","interfaceName":"en0"}""")
        }
        val container = codableJson.parseToJsonElement(
            """{"names":[{"id":"$sampleId","interfaceName":"en0","name":"A"},{"interfaceName":"en1","name":"B"}]}""",
        ).jsonObject
        assertTrue(
            container.decode("names", ListSerializer(SettingsNetworkInterfaceName.serializer()), emptyList()).isEmpty(),
        )
    }

    @Test
    fun webBrowser() {
        assertEquals(
            "{\"url\":\"https://google.com\"}",
            encodeToString(WebBrowserBookmarkSettings.serializer(), WebBrowserBookmarkSettings()),
        )
        assertDefaults(WebBrowserBookmarkSettings.serializer(), WebBrowserBookmarkSettings(), listOf("url"))
        assertDefaults(WebBrowserSettings.serializer(), WebBrowserSettings(), listOf("home", "bookmarks"))
        val webBrowser = decodeFromString(
            WebBrowserSettings.serializer(),
            """{"home":"https://moblin.org","bookmarks":[{"url":"https://a.com"},{"url":5}]}""",
        )
        assertEquals("https://moblin.org", webBrowser.home)
        assertEquals(listOf("https://a.com", "https://google.com"), webBrowser.bookmarks.map { it.url })
        assertNotEquals(webBrowser.bookmarks[0].id, webBrowser.bookmarks[1].id)
        val fallback = decodeFromString(WebBrowserSettings.serializer(), """{"home":false,"bookmarks":"x"}""")
        assertEquals("https://google.com", fallback.home)
        assertTrue(fallback.bookmarks.isEmpty())
    }

    @Test
    fun alertsMediaGallery() {
        assertDefaults(SettingsAlertsMediaGalleryItem.serializer(), SettingsAlertsMediaGalleryItem(), listOf("id", "name"))
        val gallery = SettingsAlertsMediaGallery()
        assertKeys(
            SettingsAlertsMediaGallery.serializer(),
            gallery,
            listOf("bundledImages", "customImages", "bundledSounds", "customSounds"),
        )
        assertRoundTrip(SettingsAlertsMediaGallery.serializer(), gallery)
        assertEquals(
            encodeToString(SettingsAlertsMediaGallery.serializer(), gallery),
            encodeToString(SettingsAlertsMediaGallery.serializer(), decodeFromString(SettingsAlertsMediaGallery.serializer(), "{}")),
        )
        val decoded = decodeFromString(
            SettingsAlertsMediaGallery.serializer(),
            """{"bundledImages":[{"id":"$sampleId","name":"White star"}],
                "customSounds":[{"id":"$otherId","name":"Mine"}]}""",
        )
        assertEquals(UUID.fromString(sampleId), decoded.bundledImages[0].id)
        assertEquals(14, decoded.bundledSounds.size)
        assertTrue(decoded.customImages.isEmpty())
        assertEquals("Mine", decoded.customSounds[0].name)
        val fallback = decodeFromString(SettingsAlertsMediaGallery.serializer(), """{"bundledImages":"x"}""")
        assertEquals(8, fallback.bundledImages.size)
    }

    @Test
    fun disconnectProtection() {
        assertEquals(
            "{\"liveSceneId\":null,\"fallbackSceneId\":null}",
            encodeToString(SettingsDisconnectProtection.serializer(), SettingsDisconnectProtection()),
        )
        assertRoundTrip(SettingsDisconnectProtection.serializer(), SettingsDisconnectProtection())
        val nulls = decodeFromString(
            SettingsDisconnectProtection.serializer(),
            """{"liveSceneId":null,"fallbackSceneId":null}""",
        )
        assertNull(nulls.liveSceneId)
        assertNull(nulls.fallbackSceneId)
        val empty = decodeFromString(SettingsDisconnectProtection.serializer(), "{}")
        assertNotNull(empty.liveSceneId)
        assertNotNull(empty.fallbackSceneId)
        val decoded = decodeFromString(
            SettingsDisconnectProtection.serializer(),
            """{"liveSceneId":"$sampleId","fallbackSceneId":5}""",
        )
        assertEquals(UUID.fromString(sampleId), decoded.liveSceneId)
        assertNotNull(decoded.fallbackSceneId)
        assertNotEquals(UUID.fromString(sampleId), decoded.fallbackSceneId)
        assertRoundTrip(SettingsDisconnectProtection.serializer(), decoded)
    }

    @Test
    fun face() {
        assertEquals(
            "{\"privacyMode\":\"blur\",\"blurStrength\":0.8,\"pixellateStrength\":0.3}",
            encodeToString(SettingsFace.serializer(), SettingsFace()),
        )
        assertDefaults(SettingsFace.serializer(), SettingsFace(), listOf("privacyMode", "blurStrength", "pixellateStrength"))
        val face = decodeFromString(
            SettingsFace.serializer(),
            """{"privacyMode":"backgroundImage","blurStrength":0.1,"pixellateStrength":0.9,"blurFaces":true}""",
        )
        assertEquals(SettingsFacePrivacyMode.backgroundImage, face.privacyMode)
        assertEquals(0.1f, face.blurStrength)
        assertEquals(0.9f, face.pixellateStrength)
        assertFalse(face.blurFaces)
        val fallback = decodeFromString(SettingsFace.serializer(), """{"privacyMode":"hide","blurStrength":"x"}""")
        assertEquals(SettingsFacePrivacyMode.blur, fallback.privacyMode)
        assertEquals(0.8f, fallback.blurStrength)
    }

    @Test
    fun beauty() {
        assertDefaults(
            SettingsBeauty.serializer(),
            SettingsBeauty(),
            listOf("enabled", "smoothRadius", "smoothStrength", "shapePosition", "shapeRadius", "shapeStrength"),
        )
        val beauty = decodeFromString(
            SettingsBeauty.serializer(),
            """{"enabled":true,"smoothRadius":5,"smoothStrength":0.1,"shapePosition":0.2,"shapeRadius":0.3,
               "shapeStrength":0.4}""",
        )
        assertTrue(beauty.enabled)
        assertEquals(5.0f, beauty.smoothnessRadius)
        assertEquals(0.1f, beauty.smoothnessStrength)
        assertEquals(0.2f, beauty.shapePosition)
        assertEquals(0.3f, beauty.shapeRadius)
        assertEquals(0.4f, beauty.shapeStrength)
        val fallback = decodeFromString(SettingsBeauty.serializer(), """{"smoothRadius":"x"}""")
        assertEquals(10.0f, fallback.smoothnessRadius)
    }

    @Test
    fun wiFi() {
        assertDefaults(SettingsWiFi.serializer(), SettingsWiFi(), listOf("ssid", "password"))
        val wiFi = decodeFromString(SettingsWiFi.serializer(), """{"ssid":"Home","password":"secret"}""")
        assertEquals("Home", wiFi.ssid)
        assertEquals("Home", wiFi.id)
        assertEquals("secret", wiFi.password)
        val fallback = decodeFromString(SettingsWiFi.serializer(), """{"ssid":["Home"],"password":"secret"}""")
        assertEquals("", fallback.ssid)
        assertEquals("secret", fallback.password)
    }

    @Test
    fun wrongTypedFieldsFallBack() {
        val file = decodeFromString(SettingsMediaPlayerFile.serializer(), """{"id":7,"name":false}""")
        assertEquals("My video", file.name)
        val item = decodeFromString(SettingsAlertsMediaGalleryItem.serializer(), """{"id":"$sampleId","name":{}}""")
        assertEquals(UUID.fromString(sampleId), item.id)
        assertEquals("", item.name)
        val preset = decodeFromString(SettingsZoomPreset.serializer(), """{"id":"$sampleId","name":1,"x":true}""")
        assertEquals(UUID.fromString(sampleId), preset.id)
        assertEquals("", preset.name)
        assertEquals(1.0f, preset.x)
        val switchTo = decodeFromString(SettingsZoomSwitchTo.serializer(), """{"level":[],"x":2.5,"enabled":"yes"}""")
        assertEquals(1.0f, switchTo.level)
        assertEquals(2.5f, switchTo.x)
        assertFalse(switchTo.enabled)
        val lut = decodeFromString(SettingsColorLut.serializer(), """{"type":"disk","name":[],"enabled":true}""")
        assertEquals(SettingsColorLutType.disk, lut.type)
        assertEquals("", lut.name)
        assertTrue(lut.enabled)
        val player = decodeFromString(
            SettingsMediaPlayer.serializer(),
            """{"name":"P","playerId":1,"autoSelectMic":"no"}""",
        )
        assertEquals("P", player.name)
        assertEquals("", player.playerId)
        assertTrue(player.autoSelectMic)
        assertTrue(decodeFromString(SettingsCyclingPowerDevices.serializer(), """{"devices":{}}""").devices.isEmpty())
        assertTrue(decodeFromString(SettingsWorkoutDevices.serializer(), """{"devices":"x"}""").devices.isEmpty())
        assertTrue(decodeFromString(SettingsBlackSharkCoolerDevices.serializer(), """{"devices":[1]}""").devices.isEmpty())
        val gallery = decodeFromString(
            SettingsAlertsMediaGallery.serializer(),
            """{"customImages":{},"bundledSounds":[{"name":"Boing"},5]}""",
        )
        assertTrue(gallery.customImages.isEmpty())
        assertEquals(14, gallery.bundledSounds.size)
        val wiFiAware = decodeFromString(SettingsWiFiAware.serializer(), """{"enabled":"on","role":{"receiver":{}}}""")
        assertFalse(wiFiAware.enabled)
        assertEquals(SettingsWiFiAwareRole.receiver, wiFiAware.role)
        val replay = decodeFromString(SettingsReplay.serializer(), """{"stop":{},"speed":5}""")
        assertEquals(SettingsReplay.stop, replay.stop)
        assertEquals(SettingsReplaySpeed.one, replay.speed)
    }

    @Test
    fun nullValuesFallBackToDefaults() {
        val show = decodeFromString(SettingsShow.serializer(), """{"chat":null,"cpu":null}""")
        assertTrue(show.chat)
        assertFalse(show.systemMonitor)
        val httpProxy = decodeFromString(SettingsHttpProxy.serializer(), """{"enabled":null,"port":null}""")
        assertFalse(httpProxy.enabled.value)
        assertEquals(8450, httpProxy.port.value)
        val zoom = decodeFromString(SettingsZoom.serializer(), """{"back":null,"switchToBack":null,"backgroundColor":null}""")
        assertTrue(zoom.back.isEmpty())
        assertEquals(1.0f, zoom.switchToBack.level)
        assertEquals(defaultSegmentedPickerSelectedColor, zoom.backgroundColor)
        val tesla = decodeFromString(
            SettingsTesla.serializer(),
            """{"vin":null,"bluetoothPeripheralName":null,"bluetoothPeripheralId":null}""",
        )
        assertEquals("", tesla.vin)
        assertNull(tesla.bluetoothPeripheralName)
        assertNull(tesla.bluetoothPeripheralId)
        val device = decodeFromString(
            SettingsWorkoutDevice.serializer(),
            """{"name":null,"bluetoothPeripheralName":null,"wheelCircumference":null}""",
        )
        assertEquals(SettingsWorkoutDevice.baseName, device.name)
        assertNull(device.bluetoothPeripheralName)
        assertEquals(defaultWheelCircumference, device.wheelCircumference)
        val mic = decodeFromString(SettingsMicsMic.serializer(), """{"name":null,"dataSourceID":null,"delay":null}""")
        assertEquals("", mic.name)
        assertNull(mic.dataSourceId)
        assertEquals(0.0, mic.delay.value)
        val debug = decodeFromString(
            SettingsDebug.serializer(),
            """{"logLevel":null,"debugLoggingMigrated":null,"builtinAudioAndVideoDelay":null,"tesla":null}""",
        )
        assertEquals(SettingsLogLevel.error, debug.logLevel)
        assertTrue(debug.debugLoggingMigrated)
        assertEquals(SettingsDebug.builtinAudioAndVideoDelayDefault, debug.builtinAudioAndVideoDelay.value)
        assertTrue(debug.tesla.enabled)
        assertEquals(SettingsWiFiAwareRole.sender, decodeFromString(SettingsWiFiAware.serializer(), """{"role":null}""").role)
    }

    @Test
    fun debugMigrationFlagsAlwaysEndTrue() {
        val debug = decodeFromString(
            SettingsDebug.serializer(),
            """{"logLevel":"Error","debugLogging":true,"debugLoggingMigrated":false,
               "builtinAudioAndVideoDelay":0.5,"builtinAudioAndVideoDelay70msMigrated":false}""",
        )
        assertTrue(debug.debugLoggingMigrated)
        assertFalse(debug.debugLogging.value)
        assertTrue(debug.builtinAudioAndVideoDelay70msMigrated)
        assertEquals(0.5, debug.builtinAudioAndVideoDelay.value)
        val json = encodeToString(SettingsDebug.serializer(), debug)
        assertTrue(json.contains("\"debugLoggingMigrated\":true"))
        assertTrue(json.contains("\"builtinAudioAndVideoDelay70msMigrated\":true"))
    }

    @Test
    fun nestedObjectsMustBeJsonObjects() {
        assertFailsWith<Exception> { decodeFromString(SettingsShow.serializer(), "[]") }
        val container = JsonObject(mapOf("show" to codableJson.parseToJsonElement("[]")))
        assertTrue(container.decode("show", SettingsShow.serializer(), SettingsShow(chat = false)).chat.not())
    }

    @Test
    fun publishedPropertiesAreObservable() {
        val database = Database()
        assertPublished({ database.show.chat }) { database.show.chat = false }
        assertPublished({ database.tapToFocus }) { database.tapToFocus = true }
        assertPublished({ database.appMode }) { database.appMode = SettingsAppMode.chatPhone }
        assertPublished({ database.savedWifiNetworks }) {
            database.savedWifiNetworks = database.savedWifiNetworks + SettingsWiFi(ssid = "Home")
        }
        assertPublished({ database.zoom.speed }) { database.zoom.speed = 2.0f }
        assertPublished({ database.zoom.switchToBack.enabled }) { database.zoom.switchToBack.enabled = true }
        assertPublished({ database.color.diskLutsCube }) {
            database.color.diskLutsCube = listOf(SettingsColorLut(type = SettingsColorLutType.diskCube))
        }
        assertPublished({ database.color.diskLutsCube[0].name }) { database.color.diskLutsCube[0].name = "LUT" }
        assertPublished({ database.face.privacyMode }) { database.face.privacyMode = SettingsFacePrivacyMode.icon }
        assertPublished({ database.beauty.settings }) { database.beauty.settings = SettingsBeautySettings.shape }
        assertPublished({ database.replay.speed }) { database.replay.speed = SettingsReplaySpeed.oneHalf }
        assertPublished({ database.tesla.bluetoothPeripheralId }) {
            database.tesla.bluetoothPeripheralId = UUID.randomUUID()
        }
        assertPublished({ database.disconnectProtection.liveSceneId }) {
            database.disconnectProtection.liveSceneId = UUID.randomUUID()
        }
        assertPublished({ database.wiFiAware.role }) { database.wiFiAware.role = SettingsWiFiAwareRole.receiver }
        assertPublished({ database.webBrowser.bookmarks }) {
            database.webBrowser.bookmarks = listOf(WebBrowserBookmarkSettings())
        }
        assertPublished({ database.alertsMediaGallery.customSounds }) {
            database.alertsMediaGallery.customSounds = listOf(SettingsAlertsMediaGalleryItem(name = "Sound"))
        }
        val cooler = SettingsBlackSharkCoolerDevice()
        assertPublished({ cooler.rgbLightColorColor }) { cooler.rgbLightColorColor = Color.Red }
        assertPublished({ database.blackSharkCoolerDevices.devices }) {
            database.blackSharkCoolerDevices.devices = listOf(cooler)
        }
        assertPublished({ database.zoom.back.size }) {
            database.zoom.back.add(SettingsZoomPreset(name = "3x", x = 3.0f))
        }
        assertEquals("3x", database.zoom.back.last().name)
        assertPublished({ database.zoom.back[0].x }) { database.zoom.back[0].x = 4.0f }
        assertPublished({ database.bitratePresets.toList() }) {
            database.bitratePresets.add(SettingsBitratePreset(bitrate = 1_000_000))
        }
        assertPublished({ database.bitratePresets[0].bitrate }) { database.bitratePresets[0].bitrate = 2_000_000 }
        val player = SettingsMediaPlayer()
        database.mediaPlayers.players = listOf(player)
        assertPublished({ database.mediaPlayers.players.firstOrNull()?.playlist?.size }) {
            player.playlist.add(SettingsMediaPlayerFile())
        }
        assertPublished({ player.name }) { player.name = "Player" }
        database.workoutDevices.devices.add(SettingsWorkoutDevice())
        assertPublished({ database.workoutDevices.devices.size }) { database.workoutDevices.devices.removeAt(0) }
        assertTrue(database.workoutDevices.devices.isEmpty())
        val source = mutableListOf(SettingsZoomPreset(name = "1x"))
        database.zoom.front = source
        source.clear()
        assertEquals(1, database.zoom.front.size)
        val decoded = decodeFromString(Database.serializer(), encodeToString(Database.serializer(), database))
        assertEquals(database.zoom.back.map { it.name }, decoded.zoom.back.map { it.name })
        assertEquals(database.bitratePresets.map { it.bitrate }, decoded.bitratePresets.map { it.bitrate })
        assertTrue(decoded.tapToFocus)
        assertEquals(SettingsAppMode.chatPhone, decoded.appMode)
        assertPublished({ decoded.zoom.back.size }) { decoded.zoom.back.removeAt(0) }
    }
}

package com.moblin.android.various.settings

import com.moblin.android.platform.codable.codableJson
import com.moblin.android.various.network.DefaultTcpPorts
import com.moblin.android.various.storages.ReplaySettings
import com.moblin.android.various.storages.ReplaysDatabase
import com.moblin.android.various.storages.StreamingHistoryDatabase
import com.moblin.android.various.storages.StreamingHistoryStream
import com.moblin.android.various.storages.ThermalState
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.jsonObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val ID_1 = "E621E1F8-C36C-495A-93FC-0C247A3E6E5F"
private const val ID_2 = "6f1c9b8e-2a4d-4e3f-9b1a-7c5d3e2f1a0b"
private const val ID_3 = "0A1B2C3D-4E5F-4061-8273-94A5B6C7D8E9"
private const val ID_4 = "11111111-2222-4333-8444-555555555555"
private const val ID_5 = "AAAAAAAA-BBBB-4CCC-8DDD-EEEEEEEEEEEE"
private val uuidPattern = Regex("[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}")

private fun <T> toJson(serializer: KSerializer<T>, value: T): String = codableJson.encodeToString(serializer, value)

private fun <T> fromJson(serializer: KSerializer<T>, json: String): T = codableJson.decodeFromString(serializer, json)

private fun withoutIds(json: String): String = json.replace(uuidPattern, "UUID")

private fun upper(id: UUID): String = id.toString().uppercase()

private fun uuid(value: String): UUID = UUID.fromString(value)

private fun <T> assertRoundTrip(serializer: KSerializer<T>, value: T): String {
    val json = toJson(serializer, value)
    assertEquals(json, toJson(serializer, fromJson(serializer, json)))
    return json
}

private fun <T> assertEmptyDecodesTo(serializer: KSerializer<T>, expected: T, adjust: (T) -> Unit = {}) {
    val decoded = fromJson(serializer, "{}")
    adjust(decoded)
    assertEquals(withoutIds(toJson(serializer, expected)), withoutIds(toJson(serializer, decoded)))
}

private fun <T> assertEmptyFails(serializer: KSerializer<T>) {
    assertFailsWith<SerializationException> {
        fromJson(serializer, "{}")
    }
}

private fun <T> keysOf(serializer: KSerializer<T>, value: T): List<String> =
    codableJson.encodeToJsonElement(serializer, value).jsonObject.keys.toList()

private fun migratedStream(): SettingsStream {
    val stream = SettingsStream(name = "Main")
    stream.srt.bigPacketsMigrated = true
    stream.srt.implemenationMigrated = true
    return stream
}

@RunWith(RobolectricTestRunner::class)
class DevicesCodableSuite {
    @Test
    fun defaultsRoundTrip() {
        assertRoundTrip(SettingsDjiDevice.serializer(), SettingsDjiDevice())
        assertRoundTrip(SettingsDjiDevices.serializer(), SettingsDjiDevices())
        assertRoundTrip(SettingsGoProWifiCredentials.serializer(), SettingsGoProWifiCredentials())
        assertRoundTrip(SettingsGoProRtmpUrl.serializer(), SettingsGoProRtmpUrl())
        assertRoundTrip(SettingsGoProDevice.serializer(), SettingsGoProDevice())
        assertRoundTrip(SettingsGoProLaunchLiveStream.serializer(), SettingsGoProLaunchLiveStream())
        assertRoundTrip(SettingsGoPro.serializer(), SettingsGoPro())
        assertRoundTrip(SettingsGimbalPreset.serializer(), SettingsGimbalPreset())
        assertRoundTrip(SettingsGimbal.serializer(), SettingsGimbal())
        assertRoundTrip(SettingsCatPrinter.serializer(), SettingsCatPrinter())
        assertRoundTrip(SettingsCatPrinters.serializer(), SettingsCatPrinters())
        assertRoundTrip(SettingsSelfieStick.serializer(), SettingsSelfieStick())
        assertRoundTrip(SettingsMoblinkStreamer.serializer(), SettingsMoblinkStreamer())
        assertRoundTrip(SettingsMoblinkRelay.serializer(), SettingsMoblinkRelay())
        assertRoundTrip(SettingsMoblink.serializer(), SettingsMoblink())
        assertRoundTrip(SettingsPrivacyRegion.serializer(), SettingsPrivacyRegion())
        assertRoundTrip(SettingsLocation.serializer(), SettingsLocation())
        assertRoundTrip(StreamingHistoryStream.serializer(), StreamingHistoryStream(settings = migratedStream()))
        assertRoundTrip(StreamingHistoryDatabase.serializer(), StreamingHistoryDatabase())
        assertRoundTrip(ReplaySettings.serializer(), ReplaySettings())
        assertRoundTrip(ReplaysDatabase.serializer(), ReplaysDatabase())
    }

    @Test
    fun populatedInstancesRoundTrip() {
        val djiDevices = SettingsDjiDevices()
        djiDevices.devices = listOf(SettingsDjiDevice(), SettingsDjiDevice())
        djiDevices.devices[0].bluetoothPeripheralId = UUID.randomUUID()
        djiDevices.devices[0].serverRtmpUrl = "rtmp://x/y"
        assertRoundTrip(SettingsDjiDevices.serializer(), djiDevices)
        val goPro = SettingsGoPro()
        goPro.devices.add(SettingsGoProDevice())
        goPro.launchLiveStream.add(SettingsGoProLaunchLiveStream())
        goPro.wifiCredentials.add(SettingsGoProWifiCredentials())
        goPro.rtmpUrls.add(SettingsGoProRtmpUrl())
        goPro.selectedRtmpUrl = goPro.rtmpUrls[0].id
        assertRoundTrip(SettingsGoPro.serializer(), goPro)
        val gimbal = SettingsGimbal()
        gimbal.presets = listOf(SettingsGimbalPreset())
        gimbal.functionDataFlip.sceneId = UUID.randomUUID()
        assertRoundTrip(SettingsGimbal.serializer(), gimbal)
        val printers = SettingsCatPrinters()
        printers.devices.value = listOf(SettingsCatPrinter())
        assertRoundTrip(SettingsCatPrinters.serializer(), printers)
        val location = SettingsLocation()
        location.privacyRegions = listOf(SettingsPrivacyRegion(latitude = 1.5))
        location.distanceFilter = SettingsLocationDistanceFilter.twoHundredMeters
        assertRoundTrip(SettingsLocation.serializer(), location)
        val history = StreamingHistoryDatabase()
        val stream = StreamingHistoryStream(settings = migratedStream())
        stream.highestThermalState = null
        history.streams.value = listOf(stream)
        history.totalTime.value = 12345.seconds
        assertRoundTrip(StreamingHistoryDatabase.serializer(), history)
        val replays = ReplaysDatabase()
        replays.setReplays(listOf(ReplaySettings(duration = 12.5)))
        assertRoundTrip(ReplaysDatabase.serializer(), replays)
    }

    @Test
    fun emptyObjectDecodesToDefaults() {
        assertEmptyDecodesTo(SettingsDjiDevice.serializer(), SettingsDjiDevice())
        assertEmptyDecodesTo(SettingsDjiDevices.serializer(), SettingsDjiDevices())
        assertEmptyDecodesTo(SettingsGoProWifiCredentials.serializer(), SettingsGoProWifiCredentials())
        assertEmptyDecodesTo(SettingsGoProRtmpUrl.serializer(), SettingsGoProRtmpUrl())
        assertEmptyDecodesTo(SettingsGoProDevice.serializer(), SettingsGoProDevice())
        assertEmptyDecodesTo(SettingsGoProLaunchLiveStream.serializer(), SettingsGoProLaunchLiveStream())
        assertEmptyDecodesTo(SettingsGoPro.serializer(), SettingsGoPro())
        assertEmptyDecodesTo(SettingsGimbalPreset.serializer(), SettingsGimbalPreset())
        assertEmptyDecodesTo(SettingsGimbal.serializer(), SettingsGimbal())
        assertEmptyDecodesTo(SettingsCatPrinter.serializer(), SettingsCatPrinter())
        assertEmptyDecodesTo(SettingsCatPrinters.serializer(), SettingsCatPrinters())
        assertEmptyDecodesTo(SettingsSelfieStick.serializer(), SettingsSelfieStick())
        assertEmptyDecodesTo(SettingsMoblinkStreamer.serializer(), SettingsMoblinkStreamer())
        val relay = SettingsMoblinkRelay()
        assertEmptyDecodesTo(SettingsMoblinkRelay.serializer(), relay) { relay.name.value = it.name.value }
        val moblink = SettingsMoblink()
        assertEmptyDecodesTo(SettingsMoblink.serializer(), moblink) { moblink.relay.name.value = it.relay.name.value }
        assertEmptyDecodesTo(SettingsLocation.serializer(), SettingsLocation())
        assertEmptyDecodesTo(StreamingHistoryDatabase.serializer(), StreamingHistoryDatabase())
        assertEmptyDecodesTo(ReplaysDatabase.serializer(), ReplaysDatabase())
        assertEmptyFails(SettingsPrivacyRegion.serializer())
        assertEmptyFails(StreamingHistoryStream.serializer())
        assertEmptyFails(ReplaySettings.serializer())
    }

    @Test
    fun djiDeviceEncodesLikeSwift() {
        val device = SettingsDjiDevice()
        assertEquals(
            """{"id":"${upper(device.id)}","name":"My device","bluetoothPeripheralName":null,""" +
                """"bluetoothPeripheralId":null,"wifiSsid":"","wifiPassword":"","rtmpUrlType":"Server",""" +
                """"serverRtmpStreamId":"${upper(device.serverRtmpStreamId)}","serverRtmpUrl":null,""" +
                """"customRtmpUrl":"","autoRestartStream":false,"imageStabilization":"off","resolution":"1080p",""" +
                """"fps":30,"bitrate":6000000,"videoCodec":"H.265/HEVC","isStarted":false,"model":"unknown"}""",
            toJson(SettingsDjiDevice.serializer(), device),
        )
        assertEquals("""{"devices":[]}""", toJson(SettingsDjiDevices.serializer(), SettingsDjiDevices()))
    }

    @Test
    fun swiftDjiDeviceSampleDecodes() {
        val json = """
            {"devices":[{"id":"$ID_1","name":"Action","bluetoothPeripheralName":"OA5",
              "bluetoothPeripheralId":"$ID_2","wifiSsid":"ssid","wifiPassword":"pw","rtmpUrlType":"Custom",
              "serverRtmpStreamId":"$ID_3","serverRtmpUrl":"rtmp:\/\/a\/b","customRtmpUrl":"rtmp:\/\/c\/d",
              "autoRestartStream":true,"imageStabilization":"horizonSteady","resolution":"720p","fps":25,
              "bitrate":4294967295,"videoCodec":"H.264/AVC","isStarted":true,"model":"osmoAction5Pro"}]}
        """.trimIndent()
        val devices = fromJson(SettingsDjiDevices.serializer(), json)
        assertEquals(1, devices.devices.size)
        val device = devices.devices[0]
        assertEquals(uuid(ID_1), device.id)
        assertEquals("Action", device.name)
        assertEquals("OA5", device.bluetoothPeripheralName)
        assertEquals(uuid(ID_2), device.bluetoothPeripheralId)
        assertEquals("ssid", device.wifiSsid)
        assertEquals("pw", device.wifiPassword)
        assertEquals(SettingsDjiDeviceUrlType.custom, device.rtmpUrlType)
        assertEquals(uuid(ID_3), device.serverRtmpStreamId)
        assertEquals("rtmp://a/b", device.serverRtmpUrl)
        assertEquals("rtmp://c/d", device.customRtmpUrl)
        assertTrue(device.autoRestartStream)
        assertEquals(SettingsDjiDeviceImageStabilization.horizonSteady, device.imageStabilization)
        assertEquals(SettingsDjiDeviceResolution.r720p, device.resolution)
        assertEquals(25, device.fps)
        assertEquals(UInt.MAX_VALUE, device.bitrate)
        assertEquals(SettingsDjiDeviceVideoCodec.h264avc, device.videoCodec)
        assertTrue(device.isStarted)
        assertEquals(SettingsDjiDeviceModel.osmoAction5Pro, device.model)
        val encoded = toJson(SettingsDjiDevices.serializer(), devices)
        assertTrue(encoded.contains("\"bluetoothPeripheralId\":\"${ID_2.uppercase()}\""))
        assertTrue(encoded.contains("\"bitrate\":4294967295"))
    }

    @Test
    fun djiDeviceWrongTypesFallBack() {
        val json = """
            {"id":5,"name":7,"bluetoothPeripheralName":3,"bluetoothPeripheralId":"nope","wifiSsid":false,
             "rtmpUrlType":"Server2","serverRtmpStreamId":"x","serverRtmpUrl":1,"autoRestartStream":"yes",
             "imageStabilization":"RockSteady","resolution":"4k","fps":"thirty","bitrate":-1,"videoCodec":"AV1",
             "isStarted":1,"model":"osmoAction9"}
        """.trimIndent()
        val device = fromJson(SettingsDjiDevice.serializer(), json)
        assertEquals("My device", device.name)
        assertNull(device.bluetoothPeripheralName)
        assertNull(device.bluetoothPeripheralId)
        assertEquals("", device.wifiSsid)
        assertEquals(SettingsDjiDeviceUrlType.server, device.rtmpUrlType)
        assertNull(device.serverRtmpUrl)
        assertFalse(device.autoRestartStream)
        assertEquals(SettingsDjiDeviceImageStabilization.off, device.imageStabilization)
        assertEquals(SettingsDjiDeviceResolution.r1080p, device.resolution)
        assertEquals(30, device.fps)
        assertEquals(6_000_000u, device.bitrate)
        assertEquals(SettingsDjiDeviceVideoCodec.h265hevc, device.videoCodec)
        assertFalse(device.isStarted)
        assertEquals(SettingsDjiDeviceModel.unknown, device.model)
        assertTrue(fromJson(SettingsDjiDevices.serializer(), """{"devices":[{"name":"a"},5]}""").devices.isEmpty())
    }

    @Test
    fun goProEncodesLikeSwift() {
        val device = SettingsGoProDevice()
        assertEquals(
            """{"id":"${upper(device.id)}","name":"My GoPro","bluetoothPeripheralName":null,""" +
                """"bluetoothPeripheralId":null,"wifiSsid":"","wifiPassword":"","rtmpUrlType":"Server",""" +
                """"serverRtmpStreamId":"${upper(device.serverRtmpStreamId)}","serverRtmpUrl":null,""" +
                """"customRtmpUrl":"","resolution":"1080p","bitrate":6000000,"lens":"Auto",""" +
                """"autoRestartStream":false,"isStarted":false}""",
            toJson(SettingsGoProDevice.serializer(), device),
        )
        val rtmpUrl = SettingsGoProRtmpUrl()
        assertEquals(
            """{"id":"${upper(rtmpUrl.id)}","name":"My URL","type":"Server",""" +
                """"serverStreamId":"${upper(rtmpUrl.serverStreamId)}","serverUrl":"","customUrl":""}""",
            toJson(SettingsGoProRtmpUrl.serializer(), rtmpUrl),
        )
        val credentials = SettingsGoProWifiCredentials()
        assertEquals(
            """{"id":"${upper(credentials.id)}","name":"My SSID","ssid":"","password":""}""",
            toJson(SettingsGoProWifiCredentials.serializer(), credentials),
        )
        val launchLiveStream = SettingsGoProLaunchLiveStream()
        assertEquals(
            """{"id":"${upper(launchLiveStream.id)}","name":"My live","isHero12Or13":true,"resolution":"1080p"}""",
            toJson(SettingsGoProLaunchLiveStream.serializer(), launchLiveStream),
        )
        assertEquals(
            """{"devices":[],"launchLiveStream":[],"selectedLaunchLiveStream":null,"wifiCredentials":[],""" +
                """"selectedWifiCredentials":null,"rtmpUrls":[],"selectedRtmpUrl":null}""",
            toJson(SettingsGoPro.serializer(), SettingsGoPro()),
        )
    }

    @Test
    fun swiftGoProSampleDecodes() {
        val json = """
            {"devices":[{"id":"$ID_1","name":"Hero","bluetoothPeripheralName":"GoPro 1234",
               "bluetoothPeripheralId":"$ID_2","wifiSsid":"s","wifiPassword":"p","rtmpUrlType":"Custom",
               "serverRtmpStreamId":"$ID_3","serverRtmpUrl":null,"customRtmpUrl":"rtmp://h",
               "resolution":"480p","bitrate":800000,"lens":"SuperView","autoRestartStream":true,"isStarted":true}],
             "launchLiveStream":[{"id":"$ID_4","name":"Live","isHero12Or13":false,"resolution":"720p"}],
             "selectedLaunchLiveStream":"$ID_4",
             "wifiCredentials":[{"id":"$ID_5","name":"Home","ssid":"net","password":"secret"}],
             "selectedWifiCredentials":"$ID_5",
             "rtmpUrls":[{"id":"$ID_1","name":"Url","type":"Custom","serverStreamId":"$ID_2",
               "serverUrl":"rtmp://s","customUrl":"rtmp://c"}],
             "selectedRtmpUrl":7}
        """.trimIndent()
        val goPro = fromJson(SettingsGoPro.serializer(), json)
        val device = goPro.devices.single()
        assertEquals(uuid(ID_1), device.id)
        assertEquals("Hero", device.name)
        assertEquals("GoPro 1234", device.bluetoothPeripheralName)
        assertEquals(uuid(ID_2), device.bluetoothPeripheralId)
        assertEquals(SettingsDjiDeviceUrlType.custom, device.rtmpUrlType)
        assertEquals(uuid(ID_3), device.serverRtmpStreamId)
        assertNull(device.serverRtmpUrl)
        assertEquals("rtmp://h", device.customRtmpUrl)
        assertEquals(SettingsGoProLaunchLiveStreamResolution.r480p, device.resolution)
        assertEquals(800_000, device.bitrate)
        assertEquals(SettingsGoProLens.superView, device.lens)
        assertTrue(device.autoRestartStream)
        assertTrue(device.isStarted)
        val launchLiveStream = goPro.launchLiveStream.single()
        assertEquals(uuid(ID_4), launchLiveStream.id)
        assertFalse(launchLiveStream.isHero12Or13)
        assertEquals(SettingsGoProLaunchLiveStreamResolution.r720p, launchLiveStream.resolution)
        assertEquals(uuid(ID_4), goPro.selectedLaunchLiveStream)
        val credentials = goPro.wifiCredentials.single()
        assertEquals("Home", credentials.name)
        assertEquals("net", credentials.ssid)
        assertEquals("secret", credentials.password)
        assertEquals(uuid(ID_5), goPro.selectedWifiCredentials)
        val rtmpUrl = goPro.rtmpUrls.single()
        assertEquals(SettingsDjiDeviceUrlType.custom, rtmpUrl.type)
        assertEquals(uuid(ID_2), rtmpUrl.serverStreamId)
        assertEquals("rtmp://s", rtmpUrl.serverUrl)
        assertEquals("rtmp://c", rtmpUrl.customUrl)
        assertNull(goPro.selectedRtmpUrl)
    }

    @Test
    fun goProWrongTypesFallBack() {
        val device = fromJson(
            SettingsGoProDevice.serializer(),
            """{"resolution":"1440p","bitrate":-5,"lens":"Fisheye","rtmpUrlType":"server","isStarted":"on"}""",
        )
        assertEquals(SettingsGoProLaunchLiveStreamResolution.r1080p, device.resolution)
        assertEquals(6_000_000, device.bitrate)
        assertEquals(SettingsGoProLens.auto, device.lens)
        assertEquals(SettingsDjiDeviceUrlType.server, device.rtmpUrlType)
        assertFalse(device.isStarted)
        val goPro = fromJson(SettingsGoPro.serializer(), """{"devices":{},"rtmpUrls":"x"}""")
        assertTrue(goPro.devices.isEmpty())
        assertTrue(goPro.rtmpUrls.isEmpty())
    }

    @Test
    fun goProPartsWrongTypesFallBack() {
        val credentials = fromJson(
            SettingsGoProWifiCredentials.serializer(),
            """{"id":"bad","name":1,"ssid":true,"password":[]}""",
        )
        assertEquals("My SSID", credentials.name)
        assertEquals("", credentials.ssid)
        assertEquals("", credentials.password)
        assertTrue(uuidPattern.matches(upper(credentials.id)))
        val rtmpUrl = fromJson(
            SettingsGoProRtmpUrl.serializer(),
            """{"id":"$ID_1","name":null,"type":"custom","serverStreamId":5,"serverUrl":{},"customUrl":null}""",
        )
        assertEquals(uuid(ID_1), rtmpUrl.id)
        assertEquals("My URL", rtmpUrl.name)
        assertEquals(SettingsDjiDeviceUrlType.server, rtmpUrl.type)
        assertEquals("", rtmpUrl.serverUrl)
        assertEquals("", rtmpUrl.customUrl)
        val launchLiveStream = fromJson(
            SettingsGoProLaunchLiveStream.serializer(),
            """{"name":false,"isHero12Or13":0,"resolution":"1080"}""",
        )
        assertEquals("My live", launchLiveStream.name)
        assertTrue(launchLiveStream.isHero12Or13)
        assertEquals(SettingsGoProLaunchLiveStreamResolution.r1080p, launchLiveStream.resolution)
        val device = fromJson(
            SettingsGoProDevice.serializer(),
            """{"bluetoothPeripheralName":5,"bluetoothPeripheralId":"x","serverRtmpUrl":7,"customRtmpUrl":1,""" +
                """"wifiSsid":null,"autoRestartStream":"true1"}""",
        )
        assertNull(device.bluetoothPeripheralName)
        assertNull(device.bluetoothPeripheralId)
        assertNull(device.serverRtmpUrl)
        assertEquals("", device.customRtmpUrl)
        assertEquals("", device.wifiSsid)
        assertFalse(device.autoRestartStream)
        val goPro = fromJson(
            SettingsGoPro.serializer(),
            """{"selectedLaunchLiveStream":"nope","selectedWifiCredentials":3,"selectedRtmpUrl":null,""" +
                """"launchLiveStream":[{"name":"a"},1],"wifiCredentials":null}""",
        )
        assertNull(goPro.selectedLaunchLiveStream)
        assertNull(goPro.selectedWifiCredentials)
        assertNull(goPro.selectedRtmpUrl)
        assertTrue(goPro.launchLiveStream.isEmpty())
        assertTrue(goPro.wifiCredentials.isEmpty())
    }

    @Test
    fun remainingWrongTypesFallBack() {
        val printers = fromJson(
            SettingsCatPrinters.serializer(),
            """{"devices":[{"name":"a"},"b"],"backgroundPrinting":"yes"}""",
        )
        assertTrue(printers.devices.value.isEmpty())
        assertFalse(printers.backgroundPrinting.value)
        val printer = fromJson(SettingsCatPrinter.serializer(), """{"name":3,"bluetoothPeripheralName":[]}""")
        assertEquals("", printer.name)
        assertNull(printer.bluetoothPeripheralName.value)
        val preset = fromJson(
            SettingsGimbalPreset.serializer(),
            """{"id":1,"name":[],"x":"a","y":{},"zoomX":null}""",
        )
        assertEquals("My preset", preset.name)
        assertEquals(0f, preset.x)
        assertEquals(0f, preset.y)
        assertEquals(1f, preset.zoomX)
        val gimbal = fromJson(
            SettingsGimbal.serializer(),
            """{"naturalZoom":"no","tracking":3,"functionShutter":"stream","flipMacroId":5,"presets":{}}""",
        )
        assertTrue(gimbal.naturalZoom)
        assertTrue(gimbal.tracking)
        assertEquals(SettingsControllerFunction.RECORD, gimbal.functionShutter)
        assertNull(gimbal.functionDataFlip.macroId)
        assertTrue(gimbal.presets.isEmpty())
        val streamer = fromJson(SettingsMoblinkStreamer.serializer(), """{"enabled":1,"port":"x"}""")
        assertFalse(streamer.enabled.value)
        assertEquals(DefaultTcpPorts.moblinkStreamer, streamer.port.value)
        val relay = fromJson(SettingsMoblinkRelay.serializer(), """{"enabled":"x","manual":2,"url":5}""")
        assertFalse(relay.enabled.value)
        assertFalse(relay.manual.value)
        assertEquals("", relay.url.value)
        val selfieStick = fromJson(SettingsSelfieStick.serializer(), """{"sceneId":1,"widgetId":"$ID_2"}""")
        assertNull(selfieStick.functionData.value.sceneId)
        assertEquals(uuid(ID_2), selfieStick.functionData.value.widgetId)
        val location = fromJson(
            SettingsLocation.serializer(),
            """{"resetWhenGoingLive":1,"altitudeAscent":true,"desiredAccuracy":{"hundredMeters":[]},"privacyRegions":null}""",
        )
        assertFalse(location.resetWhenGoingLive)
        assertEquals(0.0, location.altitudeAscent)
        assertEquals(SettingsLocationDesiredAccuracy.best, location.desiredAccuracy)
        assertTrue(location.privacyRegions.isEmpty())
    }

    @Test
    fun unsignedValuesRoundTripLikeSwift() {
        val device = fromJson(SettingsGoProDevice.serializer(), """{"bitrate":4294967295}""")
        assertTrue(toJson(SettingsGoProDevice.serializer(), device).contains("\"bitrate\":4294967295"))
        assertEquals(
            6_000_000,
            fromJson(SettingsGoProDevice.serializer(), """{"bitrate":4294967296}""").bitrate,
        )
        val database = StreamingHistoryDatabase.fromString(
            """{"totalBytes":18446744073709551615,"totalStreams":9223372036854775808}""",
        )
        val encoded = database.toString()
        assertTrue(encoded.contains("\"totalBytes\":18446744073709551615"))
        assertTrue(encoded.contains("\"totalStreams\":9223372036854775808"))
        assertEquals(
            0L,
            StreamingHistoryDatabase.fromString("""{"totalBytes":18446744073709551616}""").totalBytes.value,
        )
    }

    @Test
    fun gimbalEncodesLikeSwift() {
        val preset = SettingsGimbalPreset()
        assertEquals(
            """{"id":"${upper(preset.id)}","name":"My preset","x":0.0,"y":0.0,"zoomX":1.0}""",
            toJson(SettingsGimbalPreset.serializer(), preset),
        )
        assertEquals(
            """{"zoomSpeed":50.0,"naturalZoom":true,"tracking":true,"functionShutter":"Record",""" +
                """"shutterSceneId":null,"shutterWidgetId":null,"shutterGimbalPresetId":null,""" +
                """"shutterMotion":{"kapow":{}},"shutterMacroId":null,"shutterStreamDeckLayoutId":null,""" +
                """"functionFlip":"Switch scene","flipSceneId":null,"flipWidgetId":null,"flipGimbalPresetId":null,""" +
                """"flipMotion":{"kapow":{}},"flipMacroId":null,"flipStreamDeckLayoutId":null,"presets":[]}""",
            toJson(SettingsGimbal.serializer(), SettingsGimbal()),
        )
    }

    @Test
    fun swiftGimbalSampleDecodes() {
        val json = """
            {"zoomSpeed":12.5,"naturalZoom":false,"tracking":false,"functionShutter":"Gimbal preset",
             "shutterSceneId":"$ID_1","shutterWidgetId":"$ID_2","shutterGimbalPresetId":"$ID_3",
             "shutterMotion":{"yes":{}},"shutterMacroId":"$ID_4","shutterStreamDeckLayoutId":"$ID_5",
             "functionFlip":"Macro","flipSceneId":null,"flipWidgetId":"$ID_1","flipGimbalPresetId":"$ID_2",
             "flipMotion":{"wakeup":{}},"flipMacroId":"$ID_3","flipStreamDeckLayoutId":"$ID_4",
             "presets":[{"id":"$ID_3","name":"Left","x":-0.25,"y":0.5,"zoomX":2}]}
        """.trimIndent()
        val gimbal = fromJson(SettingsGimbal.serializer(), json)
        assertEquals(12.5f, gimbal.zoomSpeed)
        assertFalse(gimbal.naturalZoom)
        assertFalse(gimbal.tracking)
        assertEquals(SettingsControllerFunction.GIMBAL_PRESET, gimbal.functionShutter)
        assertEquals(
            SettingsControllerFunctionData(
                sceneId = uuid(ID_1),
                widgetId = uuid(ID_2),
                gimbalPresetId = uuid(ID_3),
                gimbalMotion = SettingsGimbalMotion.YES,
                macroId = uuid(ID_4),
                streamDeckLayoutId = uuid(ID_5),
            ),
            gimbal.functionDataShutter,
        )
        assertEquals(SettingsControllerFunction.MACRO, gimbal.functionFlip)
        assertEquals(
            SettingsControllerFunctionData(
                sceneId = null,
                widgetId = uuid(ID_1),
                gimbalPresetId = uuid(ID_2),
                gimbalMotion = SettingsGimbalMotion.WAKEUP,
                macroId = uuid(ID_3),
                streamDeckLayoutId = uuid(ID_4),
            ),
            gimbal.functionDataFlip,
        )
        val preset = gimbal.presets.single()
        assertEquals(uuid(ID_3), preset.id)
        assertEquals("Left", preset.name)
        assertEquals(-0.25f, preset.x)
        assertEquals(0.5f, preset.y)
        assertEquals(2f, preset.zoomX)
        val encoded = toJson(SettingsGimbal.serializer(), gimbal)
        assertTrue(encoded.contains("\"shutterMotion\":{\"yes\":{}}"))
        assertTrue(encoded.contains("\"flipWidgetId\":\"$ID_1\""))
        assertTrue(encoded.contains("\"shutterWidgetId\":\"${ID_2.uppercase()}\""))
    }

    @Test
    fun gimbalWrongTypesFallBack() {
        val gimbal = fromJson(
            SettingsGimbal.serializer(),
            """{"zoomSpeed":"fast","functionShutter":"Explode","shutterMotion":"yes","flipMotion":{"no":1},""" +
                """"shutterSceneId":"bad","presets":[{"x":"left"}]}""",
        )
        assertEquals(SettingsGimbal.zoomSpeedDefault, gimbal.zoomSpeed)
        assertEquals(SettingsControllerFunction.RECORD, gimbal.functionShutter)
        assertEquals(SettingsGimbalMotion.KAPOW, gimbal.functionDataShutter.gimbalMotion)
        assertEquals(SettingsGimbalMotion.KAPOW, gimbal.functionDataFlip.gimbalMotion)
        assertNull(gimbal.functionDataShutter.sceneId)
        val preset = gimbal.presets.single()
        assertEquals(0f, preset.x)
        assertEquals("My preset", preset.name)
    }

    @Test
    fun catPrinterEncodesLikeSwift() {
        val printer = SettingsCatPrinter()
        assertEquals(
            listOf(
                "id",
                "name",
                "enabled",
                "bluetoothPeripheralName",
                "bluetoothPeripheralId",
                "printChat",
                "faxMeowSound",
                "printSnapshots",
                "printTwitch",
                "printKick",
            ),
            keysOf(SettingsCatPrinter.serializer(), printer),
        )
        val element = codableJson.encodeToJsonElement(SettingsCatPrinter.serializer(), printer).jsonObject
        assertEquals("\"${upper(printer.id)}\"", element["id"].toString())
        assertEquals("\"\"", element["name"].toString())
        assertEquals("null", element["bluetoothPeripheralId"].toString())
        assertEquals("true", element["faxMeowSound"].toString())
        assertEquals(
            toJson(SettingsTwitchAlerts.serializer(), SettingsTwitchAlerts()),
            element["printTwitch"].toString(),
        )
        assertEquals(
            """{"devices":[],"backgroundPrinting":false}""",
            toJson(SettingsCatPrinters.serializer(), SettingsCatPrinters()),
        )
    }

    @Test
    fun swiftCatPrinterSampleDecodes() {
        val twitch = SettingsTwitchAlerts()
        twitch.follows = false
        val kick = SettingsKickAlerts()
        kick.hosts = false
        val json = """
            {"devices":[{"id":"$ID_1","name":"Meow","enabled":true,"bluetoothPeripheralName":"MX10",
              "bluetoothPeripheralId":"$ID_2","printChat":true,"faxMeowSound":false,"printSnapshots":false,
              "printTwitch":${toJson(SettingsTwitchAlerts.serializer(), twitch)},
              "printKick":${toJson(SettingsKickAlerts.serializer(), kick)}}],
             "backgroundPrinting":true}
        """.trimIndent()
        val printers = fromJson(SettingsCatPrinters.serializer(), json)
        assertTrue(printers.backgroundPrinting.value)
        val printer = printers.devices.value.single()
        assertEquals(uuid(ID_1), printer.id)
        assertEquals("Meow", printer.name)
        assertTrue(printer.enabled.value)
        assertEquals("MX10", printer.bluetoothPeripheralName.value)
        assertEquals(uuid(ID_2), printer.bluetoothPeripheralId.value)
        assertTrue(printer.printChat.value)
        assertFalse(printer.faxMeowSound.value)
        assertFalse(printer.printSnapshots.value)
        assertFalse(printer.printTwitch.value.follows)
        assertFalse(printer.printKick.value.hosts)
        val fallback = fromJson(
            SettingsCatPrinter.serializer(),
            """{"enabled":"on","bluetoothPeripheralId":12,"printTwitch":[],"printKick":null,"faxMeowSound":0}""",
        )
        assertFalse(fallback.enabled.value)
        assertNull(fallback.bluetoothPeripheralId.value)
        assertTrue(fallback.printTwitch.value.follows)
        assertTrue(fallback.printKick.value.hosts)
        assertTrue(fallback.faxMeowSound.value)
    }

    @Test
    fun selfieStickEncodesAndDecodesLikeSwift() {
        assertEquals(
            """{"enabled":false,"function":"Switch scene","sceneId":null,"widgetId":null,"gimbalPresetId":null,""" +
                """"gimbalMotion":{"kapow":{}},"macroId":null,"streamDeckLayoutId":null}""",
            toJson(SettingsSelfieStick.serializer(), SettingsSelfieStick()),
        )
        val json = """
            {"enabled":true,"function":"Widget","sceneId":"$ID_1","widgetId":"$ID_2","gimbalPresetId":"$ID_3",
             "gimbalMotion":{"no":{}},"macroId":"$ID_4","streamDeckLayoutId":"$ID_5"}
        """.trimIndent()
        val selfieStick = fromJson(SettingsSelfieStick.serializer(), json)
        assertTrue(selfieStick.enabled.value)
        assertEquals(SettingsControllerFunction.WIDGET, selfieStick.function.value)
        assertEquals(
            SettingsControllerFunctionData(
                sceneId = uuid(ID_1),
                widgetId = uuid(ID_2),
                gimbalPresetId = uuid(ID_3),
                gimbalMotion = SettingsGimbalMotion.NO,
                macroId = uuid(ID_4),
                streamDeckLayoutId = uuid(ID_5),
            ),
            selfieStick.functionData.value,
        )
        val fallback = fromJson(
            SettingsSelfieStick.serializer(),
            """{"enabled":null,"function":"Nothing","gimbalMotion":{"kapow":{},"yes":{}},"macroId":"x"}""",
        )
        assertFalse(fallback.enabled.value)
        assertEquals(SettingsControllerFunction.SWITCH_SCENE, fallback.function.value)
        assertEquals(SettingsGimbalMotion.KAPOW, fallback.functionData.value.gimbalMotion)
        assertNull(fallback.functionData.value.macroId)
    }

    @Test
    fun moblinkEncodesAndDecodesLikeSwift() {
        val moblink = SettingsMoblink()
        assertEquals(
            """{"server":{"enabled":false,"port":7777},"client":{"enabled":false,""" +
                """"name":"${moblink.relay.name.value}","url":"","manual":false},"password":"1234"}""",
            toJson(SettingsMoblink.serializer(), moblink),
        )
        val json = """
            {"server":{"enabled":true,"port":65535},
             "client":{"enabled":true,"name":"Relay","url":"ws:\/\/1.2.3.4:7777","manual":true},
             "password":"secret"}
        """.trimIndent()
        val decoded = fromJson(SettingsMoblink.serializer(), json)
        assertTrue(decoded.streamer.enabled.value)
        assertEquals(65535, decoded.streamer.port.value)
        assertTrue(decoded.relay.enabled.value)
        assertEquals("Relay", decoded.relay.name.value)
        assertEquals("ws://1.2.3.4:7777", decoded.relay.url.value)
        assertTrue(decoded.relay.manual.value)
        assertEquals("secret", decoded.password)
        assertEquals(
            DefaultTcpPorts.moblinkStreamer,
            fromJson(SettingsMoblinkStreamer.serializer(), """{"port":70000}""").port.value,
        )
        assertEquals(
            DefaultTcpPorts.moblinkStreamer,
            fromJson(SettingsMoblinkStreamer.serializer(), """{"port":-1}""").port.value,
        )
        val fallback = fromJson(
            SettingsMoblink.serializer(),
            """{"server":[],"client":{"url":"//1.2.3.4:5678","name":3},"password":5678}""",
        )
        assertEquals(DefaultTcpPorts.moblinkStreamer, fallback.streamer.port.value)
        assertEquals("", fallback.relay.url.value)
        assertTrue(fallback.relay.name.value.isNotEmpty())
        assertEquals("1234", fallback.password)
    }

    @Test
    fun locationEncodesLikeSwift() {
        assertEquals(
            """{"enabled":false,"privacyRegions":[],"distance":0.0,"splitDistance":0.0,"altitudeAscent":0.0,""" +
                """"altitudeDescent":0.0,"splitAltitudeAscent":0.0,"splitAltitudeDescent":0.0,""" +
                """"resetWhenGoingLive":false,"desiredAccuracy":{"best":{}},"distanceFilter":{"none":{}}}""",
            toJson(SettingsLocation.serializer(), SettingsLocation()),
        )
        val region = SettingsPrivacyRegion(id = uuid(ID_2))
        assertEquals(
            """{"id":"${ID_2.uppercase()}","latitude":0.0,"longitude":0.0,"latitudeDelta":30.0,""" +
                """"longitudeDelta":30.0}""",
            toJson(SettingsPrivacyRegion.serializer(), region),
        )
        assertEquals(
            "{\"nearestTenMeters\":{}}",
            toJson(SettingsLocationDesiredAccuracy.serializer(), SettingsLocationDesiredAccuracy.nearestTenMeters),
        )
        assertEquals(
            "{\"twoHundredMeters\":{}}",
            toJson(SettingsLocationDistanceFilter.serializer(), SettingsLocationDistanceFilter.twoHundredMeters),
        )
    }

    @Test
    fun swiftLocationSampleDecodes() {
        val json = """
            {"enabled":true,"privacyRegions":[{"id":"$ID_1","latitude":59.33,"longitude":18.06,
              "latitudeDelta":0.5,"longitudeDelta":0.25}],"distance":1234.5,"splitDistance":12,
              "altitudeAscent":100,"altitudeDescent":50.5,"splitAltitudeAscent":3,"splitAltitudeDescent":4,
              "resetWhenGoingLive":true,"desiredAccuracy":{"hundredMeters":{}},
              "distanceFilter":{"fiftyMeters":{}}}
        """.trimIndent()
        val location = fromJson(SettingsLocation.serializer(), json)
        assertTrue(location.enabled)
        assertTrue(location.enabledFlow.value)
        val region = location.privacyRegions.single()
        assertEquals(uuid(ID_1), region.id)
        assertEquals(59.33, region.latitude)
        assertEquals(18.06, region.longitude)
        assertEquals(0.5, region.latitudeDelta)
        assertEquals(0.25, region.longitudeDelta)
        assertEquals(1234.5, location.distance)
        assertEquals(12.0, location.splitDistance)
        assertEquals(100.0, location.altitudeAscent)
        assertEquals(50.5, location.altitudeDescent)
        assertEquals(3.0, location.splitAltitudeAscent)
        assertEquals(4.0, location.splitAltitudeDescent)
        assertTrue(location.resetWhenGoingLive)
        assertEquals(SettingsLocationDesiredAccuracy.hundredMeters, location.desiredAccuracy)
        assertEquals(SettingsLocationDesiredAccuracy.hundredMeters, location.desiredAccuracyFlow.value)
        assertEquals(SettingsLocationDistanceFilter.fiftyMeters, location.distanceFilter)
    }

    @Test
    fun locationWrongTypesFallBack() {
        val location = fromJson(
            SettingsLocation.serializer(),
            """{"enabled":"yes","distance":"far","desiredAccuracy":"best","distanceFilter":{"tenMeters":{},""" +
                """"oneMeter":{}},"privacyRegions":[{"id":"$ID_1","latitude":1,"longitude":2,"latitudeDelta":3}]}""",
        )
        assertFalse(location.enabled)
        assertEquals(0.0, location.distance)
        assertEquals(SettingsLocationDesiredAccuracy.best, location.desiredAccuracy)
        assertEquals(SettingsLocationDistanceFilter.none, location.distanceFilter)
        assertTrue(location.privacyRegions.isEmpty())
        assertEquals(
            SettingsLocationDistanceFilter.tenMeters,
            fromJson(SettingsLocationDistanceFilter.serializer(), """{"tenMeters":{},"unknown":{}}"""),
        )
        assertFailsWith<SerializationException> {
            fromJson(SettingsLocationDistanceFilter.serializer(), """{"tenMeters":null}""")
        }
        assertFailsWith<SerializationException> {
            fromJson(SettingsPrivacyRegion.serializer(), """{"id":"$ID_1","latitude":"north","longitude":2,""" +
                """"latitudeDelta":3,"longitudeDelta":4}""")
        }
    }

    @Test
    fun streamingHistoryEncodesLikeSwift() {
        val stream = StreamingHistoryStream(
            id = uuid(ID_2),
            settings = migratedStream(),
            startTime = Instant.ofEpochSecond(978_307_200L + 10L, 500_000_000L),
            stopTime = Instant.ofEpochSecond(978_307_200L + 30L),
            totalBytes = 1000,
        )
        assertEquals(
            listOf(
                "id",
                "settings",
                "startTime",
                "stopTime",
                "totalBytes",
                "highestThermalState",
                "lowestBatteryLevel",
                "highestBitrate",
            ),
            keysOf(StreamingHistoryStream.serializer(), stream),
        )
        val element = codableJson.encodeToJsonElement(StreamingHistoryStream.serializer(), stream).jsonObject
        assertEquals("\"${ID_2.uppercase()}\"", element["id"].toString())
        assertEquals("10.5", element["startTime"].toString())
        assertEquals("30.0", element["stopTime"].toString())
        assertEquals("1000", element["totalBytes"].toString())
        assertEquals("0", element["highestThermalState"].toString())
        assertEquals("1.0", element["lowestBatteryLevel"].toString())
        assertEquals("-9223372036854775808", element["highestBitrate"].toString())
        stream.highestThermalState = null
        stream.lowestBatteryLevel = null
        stream.highestBitrate = null
        assertEquals(
            listOf("id", "settings", "startTime", "stopTime", "totalBytes"),
            keysOf(StreamingHistoryStream.serializer(), stream),
        )
        val database = StreamingHistoryDatabase()
        assertEquals(
            """{"totalTime":[0,0],"totalBytes":0,"totalStreams":0,"streams":[]}""",
            database.toString(),
        )
        database.totalTime.value = 5.seconds
        assertTrue(database.toString().startsWith("""{"totalTime":[0,5000000000000000000],"""))
        database.totalTime.value = 20.seconds
        assertTrue(database.toString().startsWith("""{"totalTime":[1,1553255926290448384],"""))
    }

    @Test
    fun swiftStreamingHistorySampleDecodes() {
        val settings = toJson(SettingsStream.serializer(), migratedStream())
        val json = """
            {"totalTime":[1,1553255926290448384],"totalBytes":123456789,"totalStreams":2,"streams":[
              {"id":"$ID_1","settings":$settings,"startTime":700000000.25,"stopTime":700000060,
               "totalBytes":5000,"highestThermalState":2,"lowestBatteryLevel":0.42,"highestBitrate":8000000},
              {"id":"$ID_2","settings":$settings,"startTime":1,"stopTime":2,"totalBytes":0}]}
        """.trimIndent()
        val database = StreamingHistoryDatabase.fromString(json)
        assertEquals(20.seconds, database.totalTime.value)
        assertEquals(123_456_789L, database.totalBytes.value)
        assertEquals(2L, database.totalStreams.value)
        assertEquals(2, database.streams.value.size)
        val first = database.streams.value[0]
        assertEquals(uuid(ID_1), first.id)
        assertEquals("Main", first.settings.name)
        assertEquals(Instant.ofEpochSecond(978_307_200L + 700_000_000L, 250_000_000L), first.startTime)
        assertEquals(59_750L, first.duration().inWholeMilliseconds)
        assertEquals(5000L, first.totalBytes)
        assertEquals(ThermalState.SERIOUS, first.highestThermalState)
        assertEquals(0.42, first.lowestBatteryLevel)
        assertEquals(8_000_000L, first.highestBitrate)
        val second = database.streams.value[1]
        assertNull(second.highestThermalState)
        assertNull(second.lowestBatteryLevel)
        assertNull(second.highestBitrate)
        assertEquals(database.toString(), StreamingHistoryDatabase.fromString(database.toString()).toString())
    }

    @Test
    fun streamingHistoryWrongTypesFallBack() {
        val settings = toJson(SettingsStream.serializer(), migratedStream())
        val database = StreamingHistoryDatabase.fromString(
            """{"totalTime":5,"totalBytes":-1,"totalStreams":true,"streams":[{"id":"$ID_1","settings":$settings,""" +
                """"startTime":1,"stopTime":2,"totalBytes":0,"highestThermalState":7}]}""",
        )
        assertEquals(0.seconds, database.totalTime.value)
        assertEquals(0L, database.totalBytes.value)
        assertEquals(0L, database.totalStreams.value)
        assertTrue(database.streams.value.isEmpty())
        assertTrue(
            StreamingHistoryDatabase.fromString(
                """{"streams":[{"id":"$ID_1","startTime":1,"stopTime":2,"totalBytes":0}]}""",
            ).streams.value.isEmpty(),
        )
        assertEquals(0.seconds, StreamingHistoryDatabase.fromString("""{"totalTime":[1]}""").totalTime.value)
        assertFailsWith<SerializationException> { StreamingHistoryDatabase.fromString("[]") }
    }

    @Test
    fun replaysEncodeAndDecodeLikeSwift() {
        val replay = ReplaySettings(id = uuid(ID_2))
        assertEquals(
            """{"id":"${ID_2.uppercase()}","duration":0.0,"start":20.0,"stop":30.0}""",
            toJson(ReplaySettings.serializer(), replay),
        )
        assertEquals("""{"replays":[]}""", ReplaysDatabase().toString())
        val database = ReplaysDatabase.fromString(
            """{"replays":[{"id":"$ID_1","duration":42.5,"start":12,"stop":25.5},""" +
                """{"id":"$ID_3","duration":10,"start":20,"stop":30}]}""",
        )
        assertEquals(2, database.replays.value.size)
        val first = database.replays.value[0]
        assertEquals(uuid(ID_1), first.id)
        assertEquals(42.5, first.duration)
        assertEquals(12.0, first.start)
        assertEquals(25.5, first.stop)
        assertEquals(uuid(ID_3), database.replays.value[1].id)
        assertEquals(database.toString(), ReplaysDatabase.fromString(database.toString()).toString())
        assertTrue(
            ReplaysDatabase.fromString("""{"replays":[{"id":"$ID_1","duration":1,"start":2}]}""")
                .replays.value.isEmpty(),
        )
        assertTrue(ReplaysDatabase.fromString("""{"replays":{"id":"$ID_1"}}""").replays.value.isEmpty())
    }
}

package com.moblin.android.remotecontrol

import com.moblin.android.various.Variables
import com.moblin.android.various.managers.GForce
import com.moblin.android.various.settings.SettingsGimbalMotion
import java.math.BigDecimal
import java.time.Instant
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.fail
import com.moblin.android.integrations.workoutdevice.WorkoutDeviceRunningMetrics
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.moblin.android.common.various.RgbColor
import com.moblin.android.various.model.Tally
import com.moblin.android.various.settings.SettingsWidgetScene
import java.util.UUID
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val streamId = "3F2B8C1E-5A4D-4E7F-9B6A-1C2D3E4F5A6B"

@RunWith(RobolectricTestRunner::class)
class RemoteControlSuite {
    private val json = Json {
        encodeDefaults = true
        explicitNulls = false
    }

    private fun decode(json: String): RemoteControlRequest =
        RemoteControlRequest.fromJsonElement(this.json.parseToJsonElement(json))

    @Test
    fun moblinCloudStreamRequestsDecode() {
        val getStream = decode(
            """{"getStream":{"id":"$streamId","secrets":false}}"""
        ) as? RemoteControlRequest.GetStream ?: fail("Not getStream")
        assertEquals(streamId, getStream.id.toString().uppercase())
        assertFalse(getStream.secrets)
        val updateStream = decode(
            """{"updateStream":{"stream":{"id":"$streamId","name":"Kick"},"secrets":true}}"""
        ) as? RemoteControlRequest.UpdateStream ?: fail("Not updateStream")
        assertEquals(streamId, updateStream.stream.id.toString().uppercase())
        assertEquals("Kick", updateStream.stream.name)
        assertTrue(updateStream.secrets)
        val createStream = decode(
            """{"createStream":{"stream":{"id":"$streamId"},"secrets":false}}"""
        ) as? RemoteControlRequest.CreateStream ?: fail("Not createStream")
        assertFalse(createStream.secrets)
        val deleteStream = decode("""{"deleteStream":{"id":"$streamId"}}""")
        assertTrue(deleteStream is RemoteControlRequest.DeleteStream)
        val getWidgets = decode("""{"getWidgets":{"secrets":false}}""")
            as? RemoteControlRequest.GetWidgets ?: fail("Not getWidgets")
        assertFalse(getWidgets.secrets)
    }

    @Test
    fun requestsWithoutTheSecretsFlagAreRefused() {
        val cases = listOf(
            """{"getStream":{"id":"3F2B8C1E-5A4D-4E7F-9B6A-1C2D3E4F5A6B"}}""",
            """{"updateStream":{"stream":{"id":"3F2B8C1E-5A4D-4E7F-9B6A-1C2D3E4F5A6B"}}}""",
            """{"getWidgets":{}}"""
        )
        for (value in cases) {
            assertFailsWith<Exception> {
                decode(value)
            }
        }
    }

    @Test
    fun settingsNameTheCurrentStream() {
        val settings = RemoteControlSettings(
            streams = emptyList(),
            currentStreamId = UUID.fromString(streamId),
            scenes = emptyList(),
            autoSceneSwitchers = null,
            bitratePresets = emptyList(),
            mics = emptyList(),
            srt = RemoteControlSettingsSrt(connectionPrioritiesEnabled = false, connectionPriorities = emptyList())
        )
        val encoded = json.encodeToString(settings)
        assertTrue(encoded.contains(""""currentStreamId":"$streamId""""))
    }

    private fun encode(request: RemoteControlRequest): String =
        formatJson(request.toJsonElement(), prettyPrint = false)

    private inline fun <reified T> encode(value: T, prettyPrint: Boolean): String =
        formatJson(json.encodeToJsonElement(value), prettyPrint)

    @Test
    fun activeZoomPresets() {
        val cases = listOf(
            RemoteControlZoomCamera(position = RemoteControlZoomCameraPosition.Front, minX = 1f, maxX = 2f) to listOf("1x", "2x"),
            RemoteControlZoomCamera(position = RemoteControlZoomCameraPosition.Front, minX = 1f, maxX = 1.5f) to listOf("1x"),
            RemoteControlZoomCamera(position = RemoteControlZoomCameraPosition.Back, minX = 0.5f, maxX = 15f) to listOf("0.5x", "1x", "5x"),
            RemoteControlZoomCamera(position = RemoteControlZoomCameraPosition.Back, minX = 1f, maxX = 15f) to listOf("1x", "5x"),
            RemoteControlZoomCamera(position = RemoteControlZoomCameraPosition.Back, minX = 1f, maxX = 4.9f) to listOf("1x"),
            RemoteControlZoomCamera(position = RemoteControlZoomCameraPosition.Other, minX = 0.5f, maxX = 15f) to listOf()
        )
        for ((camera, names) in cases) {
            val zoomPresets = RemoteControlSettingsZoomPresets(
                front = listOf(
                    RemoteControlZoomPreset(id = UUID.randomUUID(), name = "1x", x = 1f),
                    RemoteControlZoomPreset(id = UUID.randomUUID(), name = "2x", x = 2f)
                ),
                back = listOf(
                    RemoteControlZoomPreset(id = UUID.randomUUID(), name = "0.5x", x = 0.5f),
                    RemoteControlZoomPreset(id = UUID.randomUUID(), name = "1x", x = 1f),
                    RemoteControlZoomPreset(id = UUID.randomUUID(), name = "5x", x = 5f)
                )
            )
            assertEquals(names, zoomPresets.active(camera = camera).map { it.name })
        }
    }

    @Test
    fun stateChangesOnlyContainChangedFields() {
        var sent = RemoteControlState(zoom = 0.5f, torchOn = false)
        assertEquals(RemoteControlState(torchOn = true),
                     sent.applyChanges(state =RemoteControlState(zoom = 0.5f, torchOn = true)))
        assertEquals(RemoteControlState(zoom = 0.5f, torchOn = true), sent)
    }

    @Test
    fun stateChangesAreNilWhenNothingChanged() {
        var sent = RemoteControlState(
            zoomPreset = UUID.fromString("7E3854FA-A449-4C72-B2BF-C2246541CCE8"),
            zoomCamera = RemoteControlZoomCamera(position = RemoteControlZoomCameraPosition.Back, minX = 0.5f, maxX = 15f)
        )
        assertNull(sent.applyChanges(state =sent))
    }

    @Test
    fun stateChangesKeepFieldsMissingInUpdate() {
        var sent = RemoteControlState(torchOn = false)
        assertEquals(RemoteControlState(streaming = false),
                     sent.applyChanges(state =RemoteControlState(streaming = false)))
        assertEquals(RemoteControlState(streaming = false, torchOn = false), sent)
    }

    @Test
    fun stateChangesOnlyContainChangedFilters() {
        var sent = RemoteControlState(filters = mapOf(RemoteControlFilter.Movie to true))
        assertEquals(
            RemoteControlState(filters = mapOf(RemoteControlFilter.Sepia to true)),
            sent.applyChanges(state =RemoteControlState(filters = mapOf(RemoteControlFilter.Movie to true, RemoteControlFilter.Sepia to true)))
        )
        assertEquals(RemoteControlState(filters = mapOf(RemoteControlFilter.Movie to true, RemoteControlFilter.Sepia to true)), sent)
    }

    @Test
    fun stateChangesAreNilWhenNoFilterChanged() {
        var sent = RemoteControlState(filters = mapOf(RemoteControlFilter.Movie to true, RemoteControlFilter.Sepia to false))
        assertNull(sent.applyChanges(state =RemoteControlState(filters = mapOf(RemoteControlFilter.Sepia to false))))
    }

    @Test
    fun gimbalRequests() {
        assertEquals("""{"setGimbalTracking":{"on":true}}""",
                     encode(RemoteControlRequest.SetGimbalTracking(on = true)))
        assertEquals("""{"setGimbalMovement":{"x":1,"y":-1}}""",
                     encode(RemoteControlRequest.SetGimbalMovement(x = 1f, y = -1f)))
        assertEquals("""{"animateGimbal":{"motion":{"kapow":{}}}}""",
                     encode(RemoteControlRequest.AnimateGimbal(motion = SettingsGimbalMotion.KAPOW)))
        assertEquals("""{"saveGimbalPreset":{}}""",
                     encode(RemoteControlRequest.SaveGimbalPreset))
    }

    @Test
    fun tallyRequests() {
        assertEquals(
            """{"showTally":{"background":{"blue":0,"green":0,"red":255},"foreground":{"blue":255,"green":255,"red":255},"message":"On air"}}""",
            encode(RemoteControlRequest.ShowTally(
                message = "On air",
                background = RgbColor(red = 255, green = 0, blue = 0),
                foreground = RgbColor(red = 255, green = 255, blue = 255)
            ))
        )
        assertEquals("""{"hideTally":{}}""", encode(RemoteControlRequest.HideTally))
        val request = RemoteControlRequest.fromJsonElement(
            json.parseToJsonElement("""{"showTally":{"message":"Hi","background":{"red":1,"green":2,"blue":3},"foreground":{"red":4,"green":5,"blue":6}}}""")
        )
        val tally = request as? RemoteControlRequest.ShowTally ?: fail("not a tally")
        assertEquals(RgbColor(red = 1, green = 2, blue = 3), tally.background)
        assertEquals(RgbColor(red = 4, green = 5, blue = 6), tally.foreground)
        assertEquals("Hi", tally.message)
    }

    @Test
    fun tallyShowAndHide() {
        val tally = Tally()
        assertNull(tally.state.value)
        tally.show(message = "Preview", background = RgbColor(red = 255, green = 255, blue = 255), foreground = RgbColor(red = 0, green = 0, blue = 0))
        assertEquals(RgbColor(red = 255, green = 255, blue = 255), tally.state.value?.background)
        assertEquals(RgbColor(red = 0, green = 0, blue = 0), tally.state.value?.foreground)
        assertEquals("Preview", tally.state.value?.message)
        tally.show(message = "Program", background = RgbColor(red = 0, green = 0, blue = 0), foreground = RgbColor(red = 255, green = 255, blue = 255))
        assertEquals(RgbColor(red = 0, green = 0, blue = 0), tally.state.value?.background)
        assertEquals(RgbColor(red = 255, green = 255, blue = 255), tally.state.value?.foreground)
        assertEquals("Program", tally.state.value?.message)
        tally.hide()
        assertNull(tally.state.value)
    }

    @Test
    fun sendMessageRequest() {
        assertEquals("""{"sendMessage":{"text":"Hello"}}""",
                     encode(RemoteControlRequest.SendMessage(name = null, text = "Hello")))
        assertEquals("""{"sendMessage":{"name":"Erik","text":"Hello"}}""",
                     encode(RemoteControlRequest.SendMessage(name = "Erik", text = "Hello")))
        val request = RemoteControlRequest.fromJsonElement(
            json.parseToJsonElement("""{"sendMessage":{"text":"Hello"}}""")
        )
        val message = request as? RemoteControlRequest.SendMessage ?: fail("not a message")
        assertNull(message.name)
        assertEquals("Hello", message.text)
    }

    @Test
    fun musicRequests() {
        assertEquals("""{"music":{"command":{"play":{}}}}""",
                     encode(RemoteControlRequest.Music(command = RemoteControlMusicCommand.Play)))
        assertEquals("""{"music":{"command":{"add":{"title":"Song"}}}}""",
                     encode(RemoteControlRequest.Music(command = RemoteControlMusicCommand.Add(
                         title = "Song",
                         user = null,
                         requestId = null
                     ))))
        assertEquals("""{"music":{"command":{"add":{"title":"Song","user":"Erik"}}}}""",
                     encode(RemoteControlRequest.Music(command = RemoteControlMusicCommand.Add(
                         title = "Song",
                         user = "Erik",
                         requestId = null
                     ))))
        assertEquals("""{"music":{"command":{"add":{"requestId":"ORDER1","title":"Song","user":"Erik"}}}}""",
                     encode(RemoteControlRequest.Music(command = RemoteControlMusicCommand.Add(
                         title = "Song",
                         user = "Erik",
                         requestId = "ORDER1"
                     ))))
        assertEquals("""{"music":{"command":{"find":{"title":"Song"}}}}""",
                     encode(RemoteControlRequest.Music(command = RemoteControlMusicCommand.Find(title = "Song"))))
        assertEquals("""{"music":{"command":{"remove":{"id":"1"}}}}""",
                     encode(RemoteControlRequest.Music(command = RemoteControlMusicCommand.Remove(id = "1"))))
        val request = RemoteControlRequest.fromJsonElement(
            json.parseToJsonElement("""{"music":{"command":{"add":{"title":"Song","user":"Erik"}}}}""")
        )
        val add = (request as? RemoteControlRequest.Music)?.command as? RemoteControlMusicCommand.Add
            ?: fail("not an add")
        assertEquals("Song", add.title)
        assertEquals("Erik", add.user)
        assertNull(add.requestId)
    }

    @Test
    fun tipRequests() {
        val message = RemoteControlRequest.Tip(
            name = "Erik",
            message = "Hi",
            kind = RemoteControlTipKind.Message(amount = "7.00", currency = "USD"),
            requestId = "ORDER1"
        )
        assertEquals("""{"tip":{"kind":{"message":{"amount":"7.00","currency":"USD"}},"message":"Hi","name":"Erik","requestId":"ORDER1"}}""",
                     encode(message))
        val effect = RemoteControlRequest.Tip(
            name = "Erik",
            message = "",
            kind = RemoteControlTipKind.Effect(sound = "Bad chili fart"),
            requestId = "ORDER2"
        )
        assertEquals("""{"tip":{"kind":{"effect":{"sound":"Bad chili fart"}},"message":"","name":"Erik","requestId":"ORDER2"}}""",
                     encode(effect))
        val wheelOfLuck =
            """{"tip":{"name":"Erik","message":"Go!","kind":{"wheelOfLuck":{"count":3}},"requestId":"ORDER3"}}"""
        val request = RemoteControlRequest.fromJsonElement(json.parseToJsonElement(wheelOfLuck))
        val tip = request as? RemoteControlRequest.Tip ?: fail("not a wheel of luck tip")
        val kind = tip.kind as? RemoteControlTipKind.WheelOfLuck ?: fail("not a wheel of luck tip")
        assertEquals("Erik", tip.name)
        assertEquals("Go!", tip.message)
        assertEquals(3, kind.count)
        assertEquals("ORDER3", tip.requestId)
    }

    @Test
    fun musicResponses() {
        val found = RemoteControlResponse.MusicFind(
            song = RemoteControlMusicFoundSong(id = "1440818839", title = "Song", artist = "Band", duration = 213)
        )
        assertEquals("""{"musicFind":{"song":{"artist":"Band","duration":213,"id":"1440818839","title":"Song"}}}""",
                     formatJson(found.toJsonElement(), prettyPrint = false))
        assertEquals("""{"musicFind":{}}""",
                     formatJson(RemoteControlResponse.MusicFind(song = null).toJsonElement(), prettyPrint = false))
    }

    @Test
    fun remoteSceneDataVariables() {
        val variables = RemoteControlRemoteSceneDataVariables(variables = createVariables())
        val encoded = encode(variables, prettyPrint = true)
        assertEquals(
            """
            {
              "activeEnergyBurned" : 350,
              "altitude" : 243.5,
              "altitudeAscent" : 120.25,
              "altitudeDescent" : 80.5,
              "area" : "Malmö",
              "averageSpeed" : 7.25,
              "bitrate" : "5000 kbps",
              "bitrateAndTotal" : "5000 kbps, 1.2 GB",
              "bonding" : "60% Cellular, 40% WiFi",
              "browserTitle" : "Title",
              "city" : "Malmö",
              "condition" : "clear",
              "conditions" : "sun.max",
              "country" : "Sweden",
              "countryFlag" : "🇸🇪",
              "cyclingCadence" : "90",
              "cyclingPower" : "250 W",
              "cyclingSpeed" : 10,
              "date" : 745043166,
              "debugOverlayLines" : [
                "First line",
                "Second line"
              ],
              "distance" : 1700.75,
              "feelsLikeTemperature" : {
                "unit" : {
                  "converter" : {
                    "coefficient" : 1,
                    "constant" : 273.15
                  },
                  "symbol" : "°C"
                },
                "value" : 17
              },
              "fps" : 30,
              "gForce" : {
                "max" : 3.5,
                "now" : 1.5,
                "recentMax" : 2.5
              },
              "heartRates" : {
                "Belt" : null,
                "Watch" : 75
              },
              "latestFollower" : "Follower",
              "latestSubscriber" : "Subscriber",
              "muted" : true,
              "neighborhood" : "Möllevången",
              "power" : 210,
              "resolution" : "1920x1080",
              "runningMetrics" : {
                "Foot pod" : {
                  "cadence" : 180,
                  "distance" : 4200,
                  "speed" : 3.5
                }
              },
              "slope" : "5%",
              "speed" : 5.5,
              "splitAltitudeAscent" : 40.75,
              "splitAltitudeDescent" : 30.25,
              "splitDistance" : 5400.5,
              "state" : "Skåne",
              "stepCount" : 8000,
              "systemMonitor" : "12% 300 MB",
              "temperature" : {
                "unit" : {
                  "converter" : {
                    "coefficient" : 1,
                    "constant" : 273.15
                  },
                  "symbol" : "°C"
                },
                "value" : 22
              },
              "teslaBatteryLevel" : "80%",
              "teslaDrive" : "D",
              "teslaMedia" : "Song",
              "windGust" : {
                "unit" : {
                  "converter" : {
                    "coefficient" : 1,
                    "constant" : 0
                  },
                  "symbol" : "m\/s"
                },
                "value" : 8.5
              },
              "windSpeed" : {
                "unit" : {
                  "converter" : {
                    "coefficient" : 1,
                    "constant" : 0
                  },
                  "symbol" : "m\/s"
                },
                "value" : 3
              },
              "workoutDistance" : 4200
            }
            """.trimIndent(),
            encoded
        )
    }

    @Test
    fun stats() {
        val encoded = encode(createRemoteControlStats(), prettyPrint = true)
        assertEquals(
            """
            {
              "activeEnergyBurned" : 350,
              "altitude" : 243.5,
              "altitudeAscent" : 120.25,
              "altitudeDescent" : 80.5,
              "area" : "Malmö",
              "averageSpeed" : 7.25,
              "city" : "Malmö",
              "country" : "Sweden",
              "countryFlag" : "🇸🇪",
              "cyclingCadence" : 90,
              "cyclingPower" : 250,
              "cyclingSpeed" : 10,
              "date" : 745043166,
              "distance" : 1700.75,
              "feelsLikeTemperature" : 17,
              "gForce" : {
                "max" : 3.5,
                "now" : 1.5,
                "recentMax" : 2.5
              },
              "heartRates" : {
                "Belt" : null,
                "Watch" : 75
              },
              "latitude" : 55.60587,
              "longitude" : 13.00073,
              "neighborhood" : "Möllevången",
              "power" : 210,
              "slopePercent" : 5.5,
              "speed" : 5.5,
              "splitAltitudeAscent" : 40.75,
              "splitAltitudeDescent" : 30.25,
              "splitDistance" : 5400.5,
              "state" : "Skåne",
              "stepCount" : 8000,
              "temperature" : 22,
              "timeZone" : "Europe\/Stockholm",
              "windGust" : 8.5,
              "windSpeed" : 3,
              "workoutDistance" : 4200
            }
            """.trimIndent(),
            encoded
        )
    }

    @Test
    fun statsDecode() {
        val stats = createRemoteControlStats()
        val encoded = json.encodeToString(stats)
        val decoded = json.decodeFromString<RemoteControlStats>(encoded)
        assertEquals(stats.date, decoded.date)
        assertEquals("Europe/Stockholm", decoded.timeZone)
        assertEquals(5.5, decoded.speed)
        assertEquals(7.25, decoded.averageSpeed)
        assertEquals(243.5, decoded.altitude)
        assertEquals(55.60587, decoded.latitude)
        assertEquals(13.00073, decoded.longitude)
        assertEquals(1700.75, decoded.distance)
        assertEquals(5400.5, decoded.splitDistance)
        assertEquals(5.5, decoded.slopePercent)
        assertEquals(120.25, decoded.altitudeAscent)
        assertEquals(80.5, decoded.altitudeDescent)
        assertEquals(40.75, decoded.splitAltitudeAscent)
        assertEquals(30.25, decoded.splitAltitudeDescent)
        assertEquals(22.0, decoded.temperature)
        assertEquals(17.0, decoded.feelsLikeTemperature)
        assertEquals(3.0, decoded.windSpeed)
        assertEquals(8.5, decoded.windGust)
        assertEquals("Sweden", decoded.country)
        assertEquals("🇸🇪", decoded.countryFlag)
        assertEquals("Skåne", decoded.state)
        assertEquals("Malmö", decoded.area)
        assertEquals("Malmö", decoded.city)
        assertEquals("Möllevången", decoded.neighborhood)
        assertEquals(mapOf("Watch" to 75, "Belt" to null), decoded.heartRates)
        assertEquals(350, decoded.activeEnergyBurned)
        assertEquals(4200, decoded.workoutDistance)
        assertEquals(210, decoded.power)
        assertEquals(8000, decoded.stepCount)
        assertEquals(250, decoded.cyclingPower)
        assertEquals(90, decoded.cyclingCadence)
        assertEquals(10.0, decoded.cyclingSpeed)
        assertEquals(1.5, decoded.gForce?.now)
        assertEquals(2.5, decoded.gForce?.recentMax)
        assertEquals(3.5, decoded.gForce?.max)
    }

    private fun createRemoteControlStats(): RemoteControlStats =
        RemoteControlStats(
            date = Instant.ofEpochSecond(1_723_350_366),
            timeZone = "Europe/Stockholm",
            speed = 5.5,
            averageSpeed = 7.25,
            altitude = 243.5,
            latitude = 55.60587,
            longitude = 13.00073,
            distance = 1700.75,
            splitDistance = 5400.5,
            slopePercent = 5.5,
            altitudeAscent = 120.25,
            altitudeDescent = 80.5,
            splitAltitudeAscent = 40.75,
            splitAltitudeDescent = 30.25,
            temperature = 22.0,
            feelsLikeTemperature = 17.0,
            windSpeed = 3.0,
            windGust = 8.5,
            country = "Sweden",
            countryFlag = "🇸🇪",
            state = "Skåne",
            area = "Malmö",
            city = "Malmö",
            neighborhood = "Möllevången",
            heartRates = mapOf("Watch" to 75, "Belt" to null),
            activeEnergyBurned = 350,
            workoutDistance = 4200,
            power = 210,
            stepCount = 8000,
            cyclingPower = 250,
            cyclingCadence = 90,
            cyclingSpeed = 10.0,
            gForce = GForce(now = 1.5, recentMax = 2.5, max = 3.5)
        )

    private fun createVariables(): Variables =
        Variables(
            timestamp = System.currentTimeMillis(),
            bitrate = "5000 kbps",
            bitrateAndTotal = "5000 kbps, 1.2 GB",
            bonding = "60% Cellular, 40% WiFi",
            resolution = "1920x1080",
            fps = 30,
            date = Instant.ofEpochSecond(1_723_350_366),
            debugOverlayLines = listOf("First line", "Second line"),
            speed = 5.5,
            averageSpeed = 7.25,
            altitude = 243.5,
            distance = 1700.75,
            splitDistance = 5400.5,
            altitudeAscent = 120.25,
            altitudeDescent = 80.5,
            splitAltitudeAscent = 40.75,
            splitAltitudeDescent = 30.25,
            slope = "5%",
            conditions = "sun.max",
            condition = "clear",
            temperature = 22.0,
            feelsLikeTemperature = 17.0,
            windSpeed = 3.0,
            windGust = 8.5,
            country = "Sweden",
            countryFlag = "🇸🇪",
            state = "Skåne",
            area = "Malmö",
            city = "Malmö",
            neighborhood = "Möllevången",
            muted = true,
            heartRates = mapOf("Watch" to 75, "Belt" to null),
            activeEnergyBurned = 350,
            workoutDistance = 4200,
            power = 210,
            stepCount = 8000,
            teslaBatteryLevel = "80%",
            teslaDrive = "D",
            teslaMedia = "Song",
            cyclingPower = "250 W",
            cyclingCadence = "90",
            cyclingSpeed = 10.0,
            runningMetrics = mapOf("Foot pod" to WorkoutDeviceRunningMetrics(speed = 3.5, cadence = 180, distance = 4200.0)),
            browserTitle = "Title",
            gForce = GForce(now = 1.5, recentMax = 2.5, max = 3.5),
            latestSubscriber = "Subscriber",
            latestFollower = "Follower",
            systemMonitor = "12% 300 MB"
        )

    @Test
    fun sceneWidgetWithoutSceneOverRemoteControl() {
        val data = encode(RemoteControlRemoteSceneSettingsWidgetTypeScene(scene = SettingsWidgetScene()), prettyPrint = false)
        assertEquals("""{"sceneId":"00000000-0000-0000-0000-000000000000"}""", data)
        val decoded = json.decodeFromString<RemoteControlRemoteSceneSettingsWidgetTypeScene>(data)
        assertNull(decoded.toSettings().sceneId)
    }

    @Test
    fun sceneWidgetWithSceneOverRemoteControl() {
        val scene = SettingsWidgetScene()
        scene.sceneId = UUID.randomUUID()
        val wire = RemoteControlRemoteSceneSettingsWidgetTypeScene(scene = scene)
        assertEquals(scene.sceneId, wire.toSettings().sceneId)
    }

    private fun formatJson(element: JsonElement, prettyPrint: Boolean, indent: Int = 0): String = when (element) {
        is JsonObject -> {
            val entries = element.entries.sortedBy { it.key }
            if (entries.isEmpty()) {
                "{}"
            } else if (prettyPrint) {
                val padding = "  ".repeat(indent + 1)
                val body = entries.joinToString(",\n") { (key, value) ->
                    "$padding${quoteJson(key)} : ${formatJson(value, true, indent + 1)}"
                }
                "{\n$body\n${"  ".repeat(indent)}}"
            } else {
                entries.joinToString(",", "{", "}") { (key, value) ->
                    "${quoteJson(key)}:${formatJson(value, false, indent)}"
                }
            }
        }
        is JsonArray -> {
            if (element.isEmpty()) {
                "[]"
            } else if (prettyPrint) {
                val padding = "  ".repeat(indent + 1)
                val body = element.joinToString(",\n") { value ->
                    "$padding${formatJson(value, true, indent + 1)}"
                }
                "[\n$body\n${"  ".repeat(indent)}]"
            } else {
                element.joinToString(",", "[", "]") { value -> formatJson(value, false, indent) }
            }
        }
        is JsonPrimitive -> if (element.isString) quoteJson(element.content) else formatNumber(element.content)
        else -> throw IllegalStateException("Unsupported JSON element")
    }

    private fun formatNumber(content: String): String {
        if (content.toDoubleOrNull() == null || content.none { it == '.' || it == 'e' || it == 'E' }) {
            return content
        }
        return BigDecimal(content).stripTrailingZeros().toPlainString()
    }

    private fun quoteJson(value: String): String {
        val builder = StringBuilder("\"")
        for (character in value) {
            when (character) {
                '"' -> builder.append("\\\"")
                '\\' -> builder.append("\\\\")
                '/' -> builder.append("\\/")
                '\n' -> builder.append("\\n")
                '\r' -> builder.append("\\r")
                '\t' -> builder.append("\\t")
                else -> if (character < ' ') {
                    builder.append("\\u").append("%04x".format(character.code))
                } else {
                    builder.append(character)
                }
            }
        }
        return builder.append('"').toString()
    }
}

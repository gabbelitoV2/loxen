package com.moblin.android.remotecontrol

import com.moblin.android.various.Variables
import com.moblin.android.various.Variables.WorkoutDeviceRunningMetrics
import com.moblin.android.various.managers.GForce
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

class RemoteControlSuite {
    private val json = Json {
        encodeDefaults = true
        explicitNulls = false
    }

    private inline fun <reified T> encode(value: T, prettyPrint: Boolean = false): String =
        formatJson(json.encodeToJsonElement(value), prettyPrint)

    @Test
    fun gimbalRequests() {
        assertEquals("""{"setGimbalTracking":{"on":true}}""",
                     encode(RemoteControlRequest.SetGimbalTracking(on = true)))
        assertEquals("""{"setGimbalMovement":{"x":1,"y":-1}}""",
                     encode(RemoteControlRequest.SetGimbalMovement(x = 1f, y = -1f)))
        assertEquals("""{"animateGimbal":{"motion":{"kapow":{}}}}""",
                     encode(RemoteControlRequest.AnimateGimbal(
                         motion = RemoteControlRequest.GimbalMotion.Kapow)))
        assertEquals("""{"saveGimbalPreset":{}}""",
                     encode(RemoteControlRequest.SaveGimbalPreset))
    }

    @Test
    fun sendMessageRequest() {
        assertEquals("""{"sendMessage":{"text":"Hello"}}""",
                     encode(RemoteControlRequest.SendMessage(text = "Hello")))
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
            runningMetrics = mapOf("Foot pod" to WorkoutDeviceRunningMetrics(speed = 3.5, cadence = 180.0, distance = 4200.0)),
            browserTitle = "Title",
            gForce = GForce(now = 1.5, recentMax = 2.5, max = 3.5),
            latestSubscriber = "Subscriber",
            latestFollower = "Follower",
            systemMonitor = "12% 300 MB"
        )

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
        is JsonPrimitive -> if (element.isString) quoteJson(element.content) else element.content
        else -> throw IllegalStateException("Unsupported JSON element")
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

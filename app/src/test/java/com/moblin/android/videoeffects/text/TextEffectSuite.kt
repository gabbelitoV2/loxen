package com.moblin.android.videoeffects.text

import com.moblin.android.various.Variables
import com.moblin.android.various.managers.GForce
import java.time.Instant
import java.util.Locale
import kotlin.test.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rSE")
class TextEffectSuite {
    @Test
    fun time() {
        assumeTrue(Locale.getDefault().toString() == "en_SE")
        val lines = format(format = "{time}", variables = createVariables())
        assertEquals(createLine(data = TextEffectPartData.Text("06:26:06")), lines)
    }

    @Test
    fun date() {
        assumeTrue(Locale.getDefault().toString() == "en_SE")
        val lines = format(format = "{date}", variables = createVariables())
        assertEquals(createLine(data = TextEffectPartData.Text("2024-08-11")), lines)
    }

    @Test
    fun conditions() {
        var lines = format(format = "{conditions}", variables = createVariables())
        assertEquals(createLine(data = TextEffectPartData.Text("-")), lines)
        lines = format(
            format = "{conditions}",
            variables = createVariables(
                conditions = "sun.max",
                condition = "Clear"
            )
        )
        assertEquals(
            createLine(data = TextEffectPartData.ImageSystemNameTryFill("sun.max", "☀️")),
            lines
        )
    }

    @Test
    fun gForce() {
        var lines = format(format = "{gForce}", variables = createVariables())
        assertEquals(createLine(data = TextEffectPartData.Text("-")), lines)
        val variables = createVariables(gForce = GForce(now = 3.0, recentMax = 4.0, max = 5.0))
        lines = format(format = "{gForce}", variables = variables)
        assertEquals(createLine(data = TextEffectPartData.Text("3.0")), lines)
    }

    @Test
    fun gForceRecentMax() {
        var lines = format(format = "{gForceRecentMax}", variables = createVariables())
        assertEquals(createLine(data = TextEffectPartData.Text("-")), lines)
        val variables = createVariables(gForce = GForce(now = 3.0, recentMax = 4.0, max = 5.0))
        lines = format(format = "{gForceRecentMax}", variables = variables)
        assertEquals(createLine(data = TextEffectPartData.Text("4.0")), lines)
    }

    @Test
    fun gForceMax() {
        var lines = format(format = "{gForceMax}", variables = createVariables())
        assertEquals(createLine(data = TextEffectPartData.Text("-")), lines)
        val variables = createVariables(gForce = GForce(now = 3.0, recentMax = 4.0, max = 5.0))
        lines = format(format = "{gForceMax}", variables = variables)
        assertEquals(createLine(data = TextEffectPartData.Text("5.0")), lines)
    }

    @Test
    fun heartRate() {
        var lines = format(format = "{heartRate}", variables = createVariables())
        assertEquals(createLine(data = TextEffectPartData.Text("-")), lines)
        var variables = createVariables(heartRates = mapOf("" to 132))
        lines = format(format = "{heartRate}", variables = variables)
        assertEquals(createLine(data = TextEffectPartData.Text("132")), lines)
        variables = createVariables(heartRates = mapOf("polar" to 133))
        lines = format(format = "{heartRate:Polar}", variables = variables)
        assertEquals(createLine(data = TextEffectPartData.Text("133")), lines)
        variables = createVariables(heartRates = mapOf("polar" to 134))
        lines = format(format = "{heartRate:polar}", variables = variables)
        assertEquals(createLine(data = TextEffectPartData.Text("134")), lines)
    }

    @Test
    fun speed() {
        var lines = format(format = "{speed:m/s}", variables = createVariables())
        assertEquals(createLine(data = TextEffectPartData.Text("5 m/s")), lines)
        lines = format(format = "{speed:km/h}", variables = createVariables())
        assertEquals(createLine(data = TextEffectPartData.Text("18 km/h")), lines)
        lines = format(format = "{speed:mph}", variables = createVariables())
        assertEquals(createLine(data = TextEffectPartData.Text("11 mph")), lines)
        val systemPart = format(format = "{speed}", variables = createVariables())
        lines = format(format = "{speed:mph} {speed} {speed:m/s}", variables = createVariables())
        assertEquals(createLine(data = TextEffectPartData.Text("11 mph"))[0].parts[0], lines[0].parts[0])
        assertEquals(systemPart[0].parts[0].data, lines[0].parts[2].data)
        assertEquals(createLine(data = TextEffectPartData.Text("5 m/s"))[0].parts[0].data, lines[0].parts[4].data)
    }

    @Test
    fun averageSpeed() {
        var lines = format(format = "{averageSpeed:m/s}", variables = createVariables())
        assertEquals(createLine(data = TextEffectPartData.Text("7 m/s")), lines)
        lines = format(format = "{averageSpeed:km/h}", variables = createVariables())
        assertEquals(createLine(data = TextEffectPartData.Text("25 km/h")), lines)
        lines = format(format = "{averageSpeed:mph}", variables = createVariables())
        assertEquals(createLine(data = TextEffectPartData.Text("16 mph")), lines)
        val systemPart = format(format = "{averageSpeed}", variables = createVariables())
        lines = format(
            format = "{averageSpeed:mph} {averageSpeed} {averageSpeed:m/s}",
            variables = createVariables()
        )
        assertEquals(createLine(data = TextEffectPartData.Text("16 mph"))[0].parts[0], lines[0].parts[0])
        assertEquals(systemPart[0].parts[0].data, lines[0].parts[2].data)
        assertEquals(createLine(data = TextEffectPartData.Text("7 m/s"))[0].parts[0].data, lines[0].parts[4].data)
    }

    @Test
    fun wind() {
        var lines = format(format = "{wind:m/s}", variables = createVariables())
        assertEquals(createLine(data = TextEffectPartData.Text("3 m/s")), lines)
        lines = format(format = "{wind:km/h}", variables = createVariables())
        assertEquals(createLine(data = TextEffectPartData.Text("11 km/h")), lines)
        lines = format(format = "{wind:mph}", variables = createVariables())
        assertEquals(createLine(data = TextEffectPartData.Text("7 mph")), lines)
        val systemPart = format(format = "{wind}", variables = createVariables())
        lines = format(format = "{wind:mph} {wind} {wind:m/s}", variables = createVariables())
        assertEquals(createLine(data = TextEffectPartData.Text("7 mph"))[0].parts[0], lines[0].parts[0])
        assertEquals(systemPart[0].parts[0].data, lines[0].parts[2].data)
        assertEquals(createLine(data = TextEffectPartData.Text("3 m/s"))[0].parts[0].data, lines[0].parts[4].data)
    }

    @Test
    fun temperature() {
        var lines = format(format = "{temperature:c}", variables = createVariables())
        assertEquals(createLine(data = TextEffectPartData.Text("22°C")), lines)
        lines = format(format = "{temperature:f}", variables = createVariables())
        assertEquals(createLine(data = TextEffectPartData.Text("72°F")), lines)
        lines = format(format = "{temperature:k}", variables = createVariables())
        assertEquals(createLine(data = TextEffectPartData.Text("295 K")), lines)
        val systemPart = format(format = "{temperature}", variables = createVariables())
        lines = format(format = "{temperature:f} {temperature} {temperature:c}", variables = createVariables())
        assertEquals(createLine(data = TextEffectPartData.Text("72°F"))[0].parts[0], lines[0].parts[0])
        assertEquals(systemPart[0].parts[0].data, lines[0].parts[2].data)
        assertEquals(createLine(data = TextEffectPartData.Text("22°C"))[0].parts[0].data, lines[0].parts[4].data)
    }

    @Test
    fun feelsLikeTemperature() {
        var lines = format(format = "{feelsLikeTemperature:c}", variables = createVariables())
        assertEquals(createLine(data = TextEffectPartData.Text("17°C")), lines)
        lines = format(format = "{feelsLikeTemperature:f}", variables = createVariables())
        assertEquals(createLine(data = TextEffectPartData.Text("63°F")), lines)
        lines = format(format = "{feelsLikeTemperature:k}", variables = createVariables())
        assertEquals(createLine(data = TextEffectPartData.Text("290 K")), lines)
        val systemPart = format(format = "{feelsLikeTemperature}", variables = createVariables())
        lines = format(
            format = "{feelsLikeTemperature:f} {feelsLikeTemperature} {feelsLikeTemperature:c}",
            variables = createVariables()
        )
        assertEquals(createLine(data = TextEffectPartData.Text("63°F"))[0].parts[0], lines[0].parts[0])
        assertEquals(systemPart[0].parts[0].data, lines[0].parts[2].data)
        assertEquals(createLine(data = TextEffectPartData.Text("17°C"))[0].parts[0].data, lines[0].parts[4].data)
    }

    @Test
    fun altitude() {
        var lines = format(format = "{altitude:m}", variables = createVariables())
        assertEquals(createLine(data = TextEffectPartData.Text("243 m")), lines)
        lines = format(format = "{altitude:ft}", variables = createVariables())
        assertEquals(createLine(data = TextEffectPartData.Text("797 ft")), lines)
        val systemPart = format(format = "{altitude}", variables = createVariables())
        lines = format(
            format = "{altitude:ft} {altitude} {altitude:m}",
            variables = createVariables()
        )
        assertEquals(createLine(data = TextEffectPartData.Text("797 ft"))[0].parts[0], lines[0].parts[0])
        assertEquals(systemPart[0].parts[0].data, lines[0].parts[2].data)
        assertEquals(createLine(data = TextEffectPartData.Text("243 m"))[0].parts[0].data, lines[0].parts[4].data)
    }

    @Test
    fun distance() {
        var lines = format(format = "{distance:m}", variables = createVariables())
        assertEquals(createLine(data = TextEffectPartData.Text("1 700 m")), lines)
        lines = format(format = "{distance:km}", variables = createVariables())
        assertEquals(createLine(data = TextEffectPartData.Text("2 km")), lines)
        lines = format(format = "{distance:yd}", variables = createVariables())
        assertEquals(createLine(data = TextEffectPartData.Text("1 859 yd")), lines)
        lines = format(format = "{distance:ft}", variables = createVariables())
        assertEquals(createLine(data = TextEffectPartData.Text("5 577 ft")), lines)
        lines = format(format = "{distance:mi}", variables = createVariables())
        assertEquals(createLine(data = TextEffectPartData.Text("1 mi")), lines)
        lines = format(format = "{distance:nmi}", variables = createVariables())
        assertEquals(createLine(data = TextEffectPartData.Text("1 nmi")), lines)
        lines = format(format = "{distance:ly}", variables = createVariables())
        assertEquals(createLine(data = TextEffectPartData.Text("0 ly")), lines)
        val systemPart = format(format = "{distance}", variables = createVariables())
        lines = format(format = "{distance:mi} {distance} {distance:m}", variables = createVariables())
        assertEquals(createLine(data = TextEffectPartData.Text("1 mi"))[0].parts[0], lines[0].parts[0])
        assertEquals(systemPart[0].parts[0].data, lines[0].parts[2].data)
        assertEquals(createLine(data = TextEffectPartData.Text("1 700 m"))[0].parts[0].data, lines[0].parts[4].data)
    }

    @Test
    fun splitDistance() {
        var lines = format(format = "{splitDistance:m}", variables = createVariables())
        assertEquals(createLine(data = TextEffectPartData.Text("5 400 m")), lines)
        lines = format(format = "{splitDistance:km}", variables = createVariables())
        assertEquals(createLine(data = TextEffectPartData.Text("5 km")), lines)
        lines = format(format = "{splitDistance:yd}", variables = createVariables())
        assertEquals(createLine(data = TextEffectPartData.Text("5 906 yd")), lines)
        lines = format(format = "{splitDistance:ft}", variables = createVariables())
        assertEquals(createLine(data = TextEffectPartData.Text("17 717 ft")), lines)
        lines = format(format = "{splitDistance:mi}", variables = createVariables())
        assertEquals(createLine(data = TextEffectPartData.Text("3 mi")), lines)
        lines = format(format = "{splitDistance:nmi}", variables = createVariables())
        assertEquals(createLine(data = TextEffectPartData.Text("3 nmi")), lines)
        lines = format(format = "{splitDistance:ly}", variables = createVariables())
        assertEquals(createLine(data = TextEffectPartData.Text("0 ly")), lines)
        val systemPart = format(format = "{splitDistance}", variables = createVariables())
        lines = format(
            format = "{splitDistance:mi} {splitDistance} {splitDistance:m}",
            variables = createVariables()
        )
        assertEquals(createLine(data = TextEffectPartData.Text("3 mi"))[0].parts[0], lines[0].parts[0])
        assertEquals(systemPart[0].parts[0].data, lines[0].parts[2].data)
        assertEquals(createLine(data = TextEffectPartData.Text("5 400 m"))[0].parts[0].data, lines[0].parts[4].data)
    }

    @Test
    fun multiple() {
        val lines = format(format = "time: {time}, date: {date}\nsecond line", variables = createVariables())
        assertEquals(
            listOf(
                TextEffectLine(
                    id = 0,
                    parts = listOf(
                        TextEffectPart(id = 0, data = TextEffectPartData.Text("time: ")),
                        TextEffectPart(id = 1, data = TextEffectPartData.Text("06:26:06")),
                        TextEffectPart(id = 2, data = TextEffectPartData.Text(", date: ")),
                        TextEffectPart(id = 3, data = TextEffectPartData.Text("2024-08-11")),
                    )
                ),
                TextEffectLine(
                    id = 1,
                    parts = listOf(
                        TextEffectPart(id = 5, data = TextEffectPartData.Text("second line")),
                    )
                ),
            ),
            lines
        )
    }

    @Test
    fun loadFormatSpeed() {
        val loader = TextFormatLoader()
        var parts = loader.load(inputFormat = "{speed}")
        assertEquals(listOf(TextFormatPart.Speed(TextFormatSpeedUnit.system)), parts)
        parts = loader.load(inputFormat = "{speed:m/s}")
        assertEquals(listOf(TextFormatPart.Speed(TextFormatSpeedUnit.metersPerSecond)), parts)
        parts = loader.load(inputFormat = "{speed:km/h}")
        assertEquals(listOf(TextFormatPart.Speed(TextFormatSpeedUnit.kilometersPerHour)), parts)
        parts = loader.load(inputFormat = "{speed:mph}")
        assertEquals(listOf(TextFormatPart.Speed(TextFormatSpeedUnit.milesPerHour)), parts)
        parts = loader.load(inputFormat = "{speed:foo}")
        assertEquals(listOf(TextFormatPart.Text("{speed:foo}")), parts)
    }

    @Test
    fun loadFormatAverageSpeed() {
        val loader = TextFormatLoader()
        var parts = loader.load(inputFormat = "{averagespeed}")
        assertEquals(listOf(TextFormatPart.AverageSpeed(TextFormatSpeedUnit.system)), parts)
        parts = loader.load(inputFormat = "{averagespeed:m/s}")
        assertEquals(listOf(TextFormatPart.AverageSpeed(TextFormatSpeedUnit.metersPerSecond)), parts)
        parts = loader.load(inputFormat = "{averagespeed:km/h}")
        assertEquals(listOf(TextFormatPart.AverageSpeed(TextFormatSpeedUnit.kilometersPerHour)), parts)
        parts = loader.load(inputFormat = "{averagespeed:mph}")
        assertEquals(listOf(TextFormatPart.AverageSpeed(TextFormatSpeedUnit.milesPerHour)), parts)
        parts = loader.load(inputFormat = "{averagespeed:foo}")
        assertEquals(listOf(TextFormatPart.Text("{averagespeed:foo}")), parts)
    }

    @Test
    fun loadFormatHeartrate() {
        val loader = TextFormatLoader()
        var parts = loader.load(inputFormat = "{heartrate}")
        assertEquals(listOf(TextFormatPart.HeartRate("")), parts)
        parts = loader.load(inputFormat = "{heartrate:My device}")
        assertEquals(listOf(TextFormatPart.HeartRate("my device")), parts)
    }

    @Test
    fun loadFormatRunningPace() {
        val loader = TextFormatLoader()
        var parts = loader.load(inputFormat = "{runningpace}")
        assertEquals(listOf(TextFormatPart.RunningPace("")), parts)
        parts = loader.load(inputFormat = "{runningpace:My device}")
        assertEquals(listOf(TextFormatPart.RunningPace("my device")), parts)
    }

    @Test
    fun loadFormatRunningCadence() {
        val loader = TextFormatLoader()
        var parts = loader.load(inputFormat = "{runningcadence}")
        assertEquals(listOf(TextFormatPart.RunningCadence("")), parts)
        parts = loader.load(inputFormat = "{runningcadence:My device}")
        assertEquals(listOf(TextFormatPart.RunningCadence("my device")), parts)
    }

    @Test
    fun loadFormatRunningDistance() {
        val loader = TextFormatLoader()
        var parts = loader.load(inputFormat = "{runningdistance}")
        assertEquals(listOf(TextFormatPart.RunningDistance("")), parts)
        parts = loader.load(inputFormat = "{runningdistance:My device}")
        assertEquals(listOf(TextFormatPart.RunningDistance("my device")), parts)
    }

    @Test
    fun loadFormatSubtitles() {
        val loader = TextFormatLoader()
        var parts = loader.load(inputFormat = "{subtitles}")
        assertEquals(listOf(TextFormatPart.Subtitles(null)), parts)
        parts = loader.load(inputFormat = "{subtitles:dk}")
        assertEquals(listOf(TextFormatPart.Subtitles("dk")), parts)
    }

    @Test
    fun systemMonitor() {
        var lines = format(
            format = "{systemMonitor}",
            variables = createVariables(systemMonitor = "-% - MB")
        )
        assertEquals(createLine(data = TextEffectPartData.Text("-% - MB")), lines)
        lines = format(
            format = "{systemMonitor}",
            variables = createVariables(systemMonitor = "12% 300 MB")
        )
        assertEquals(createLine(data = TextEffectPartData.Text("12% 300 MB")), lines)
    }

    @Test
    fun plainText() {
        var lines = format(
            format = "Speed {speed:km/h}\\nGravity {gForce}",
            variables = createVariables()
        )
        assertEquals("Speed 18 km/h Gravity -", lines.toPlainText())
        lines = format(
            format = "{conditions} {speed:m/s}",
            variables = createVariables(
                conditions = "sun.max",
                condition = "Clear"
            )
        )
        assertEquals("☀️ 5 m/s", lines.toPlainText())
        lines = format(
            format = "Speed {speed:m/s} {conditions} today",
            variables = createVariables(
                conditions = "cloud.rain",
                condition = "Rain"
            )
        )
        assertEquals("Speed 5 m/s 🌧️ today", lines.toPlainText())
    }

    private fun format(format: String, variables: Variables): List<TextEffectLine> {
        val formatter = TextEffectFormatter(
            formatParts = loadTextFormat(format = format),
            timersEndTime = emptyList(),
            stopwatches = emptyList(),
            checkboxes = emptyList(),
            ratings = emptyList(),
            lapTimes = emptyList()
        )
        return formatter.format(variables = variables, now = Instant.now().toEpochMilli() * 1_000_000L)
    }

    private fun createVariables(
        conditions: String? = null,
        condition: String? = null,
        heartRates: Map<String, Int?> = emptyMap(),
        gForce: GForce? = null,
        systemMonitor: String = ""
    ): Variables {
        return Variables(
            timestamp = Instant.now().toEpochMilli(),
            bitrate = "",
            bitrateAndTotal = "",
            bonding = "",
            resolution = null,
            fps = null,
            date = Instant.ofEpochSecond(1_723_350_366),
            debugOverlayLines = emptyList(),
            speed = 5.0,
            averageSpeed = 7.0,
            altitude = 243.0,
            distance = 1700.0,
            splitDistance = 5400.0,
            altitudeAscent = 120.0,
            altitudeDescent = 80.0,
            splitAltitudeAscent = 40.0,
            splitAltitudeDescent = 30.0,
            slope = "",
            conditions = conditions,
            condition = condition,
            temperature = 22.0,
            feelsLikeTemperature = 17.0,
            windSpeed = 3.0,
            windGust = null,
            country = null,
            countryFlag = null,
            state = null,
            area = null,
            city = null,
            neighborhood = null,
            muted = false,
            heartRates = heartRates,
            activeEnergyBurned = null,
            workoutDistance = null,
            power = null,
            stepCount = null,
            teslaBatteryLevel = "",
            teslaDrive = "",
            teslaMedia = "",
            cyclingPower = "",
            cyclingCadence = "",
            cyclingSpeed = 0.0,
            runningMetrics = emptyMap(),
            browserTitle = "",
            gForce = gForce,
            latestSubscriber = "",
            latestFollower = "",
            systemMonitor = systemMonitor
        )
    }

    private fun createLine(data: TextEffectPartData): List<TextEffectLine> {
        return listOf(TextEffectLine(id = 0, parts = listOf(TextEffectPart(id = 0, data = data))))
    }
}

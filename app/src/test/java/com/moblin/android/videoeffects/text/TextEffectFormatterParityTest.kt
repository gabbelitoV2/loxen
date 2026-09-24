package com.moblin.android.videoeffects.text

import com.moblin.android.platform.core.ContinuousClock
import com.moblin.android.various.Variables
import java.time.Instant
import java.util.Locale
import java.util.TimeZone
import kotlin.test.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import com.moblin.android.various.settings.SettingsWidgetTextStopwatch
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class TextEffectFormatterParityTest {
    private val defaultLocale = Locale.getDefault()
    private val defaultTimeZone = TimeZone.getDefault()

    @Before
    fun setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Stockholm"))
        Locale.setDefault(Locale.forLanguageTag("en-SE"))
        plain("{time} {date}")
    }

    @After
    fun tearDown() {
        Locale.setDefault(defaultLocale)
        TimeZone.setDefault(defaultTimeZone)
    }

    @Test
    fun systemDistanceUsesLengthFormatterUnits() {
        Locale.setDefault(Locale.UK)
        assertEquals("1.7 km", plain("{distance}", distance = 1700.0))
        assertEquals("350 m", plain("{distance}", distance = 350.0))
        assertEquals("50 cm", plain("{distance}", distance = 0.5))
        assertEquals("12.3 km", plain("{splitDistance}", splitDistance = 12_345.0))
        Locale.setDefault(Locale.US)
        assertEquals("1.1 mi", plain("{distance}", distance = 1700.0))
        assertEquals("109.4 yd", plain("{distance}", distance = 100.0))
    }

    @Test
    fun explicitDistanceUnitsAreUnchanged() {
        Locale.setDefault(Locale.US)
        assertEquals("1,700 m", plain("{distance:m}", distance = 1700.0))
        assertEquals("2 km", plain("{distance:km}", distance = 1700.0))
    }

    @Test
    fun windWithoutGustUsesProvidedOrMeasurementSystemUnit() {
        Locale.setDefault(Locale.forLanguageTag("en-SE"))
        assertEquals("3 m/s", plain("{wind}", windSpeed = 3.0))
        Locale.setDefault(Locale.UK)
        assertEquals("7 mph", plain("{wind}", windSpeed = 3.0))
        assertEquals("11 km/h", plain("{wind:km/h}", windSpeed = 3.0))
    }

    @Test
    fun windWithGustTruncatesBothValues() {
        Locale.setDefault(Locale.UK)
        assertEquals("10 (18) km/h", plain("{wind:km/h}", windSpeed = 3.0, windGust = 5.0))
        assertEquals("6 (11) mph", plain("{wind}", windSpeed = 3.0, windGust = 5.0))
        assertEquals("3 (5) m/s", plain("{wind:m/s}", windSpeed = 3.0, windGust = 5.0))
    }

    @Test
    fun systemSpeedFollowsMeasurementSystem() {
        Locale.setDefault(Locale.forLanguageTag("en-SE"))
        assertEquals("18 km/h", plain("{speed}", speed = 5.0))
        Locale.setDefault(Locale.UK)
        assertEquals("11 mph", plain("{speed}", speed = 5.0))
        assertEquals("5 m/s", plain("{speed:m/s}", speed = 5.0))
        Locale.setDefault(Locale.US)
        assertEquals("11 mph", plain("{speed}", speed = 5.0))
    }

    @Test
    fun missingWindIsADash() {
        assertEquals("-", plain("{wind}"))
    }

    @Test
    @Config(qualifiers = "en-rSE")
    fun clockFollowsTimeZoneChanges() {
        assertEquals("06:26:06 06:26", plain("{time} {shorttime}"))
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        assertEquals("04:26:06 04:26", plain("{time} {shorttime}"))
        assertEquals("2024-08-11", plain("{date}"))
        TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Auckland"))
        assertEquals("16:26:06 2024-08-11", plain("{time} {date}"))
        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
        assertEquals("2024-08-10", plain("{date}"))
    }

    @Test
    fun systemTemperatureFollowsRegionalPreference() {
        Locale.setDefault(Locale.forLanguageTag("en-SE"))
        assertEquals("22°C", plain("{temperature}", temperature = 22.0))
        Locale.setDefault(Locale.forLanguageTag("en-SE-u-mu-fahrenhe"))
        assertEquals("72°F", plain("{temperature}", temperature = 22.0))
        Locale.setDefault(Locale.forLanguageTag("en-US-u-mu-celsius"))
        assertEquals("22°C", plain("{temperature}", temperature = 22.0))
        Locale.setDefault(Locale.US)
        assertEquals("72°F", plain("{temperature}", temperature = 22.0))
        assertEquals("22°C", plain("{temperature:c}", temperature = 22.0))
    }

    @Test
    fun fullDateUsesFullDateStyle() {
        Locale.setDefault(Locale.US)
        assertEquals("Sunday, August 11, 2024", plain("{fulldate}"))
    }

    @Test
    fun timerUsesAbbreviatedUnits() {
        val now = ContinuousClock.now
        assertEquals("1h 2m 3s", plain("{timer}", timersEndTime = listOf(now.advanced(bySeconds = 3723.9)), now = now))
        assertEquals("1d 1s", plain("{timer}", timersEndTime = listOf(now.advanced(bySeconds = 86_401.0)), now = now))
        assertEquals("5m", plain("{timer}", timersEndTime = listOf(now.advanced(bySeconds = 300.0)), now = now))
        assertEquals("4m 59s", plain("{timer}", timersEndTime = listOf(now.advanced(bySeconds = 299.99)), now = now))
        assertEquals("0s", plain("{timer}", timersEndTime = listOf(now.advanced(bySeconds = -10.0)), now = now))
        assertEquals("", plain("{timer}", now = now))
    }

    @Test
    fun stopwatchUsesAbbreviatedUnits() {
        val stopped = SettingsWidgetTextStopwatch(totalElapsed = 62.4)
        assertEquals("1m 2s", plain("{stopwatch}", stopwatches = listOf(stopped)))
        val running = SettingsWidgetTextStopwatch(totalElapsed = 10.0, running = true)
        running.playPressedTime = Instant.now().minusSeconds(3600)
        assertEquals("1h 10s", plain("{stopwatch}", stopwatches = listOf(running)))
    }

    @Test
    fun lapTimesUseAbbreviatedUnits() {
        val laps = listOf(listOf(65.0, 3601.0, Double.POSITIVE_INFINITY, 7.0))
        assertEquals("Lap 1 1m 5s Lap 2 1h 1s 🏁 Finished 🏁 Lap 1 7s", plain("{laptimes}", lapTimes = laps))
    }

    private fun plain(
        format: String,
        speed: Double = 0.0,
        distance: Double = 0.0,
        splitDistance: Double = 0.0,
        windSpeed: Double? = null,
        windGust: Double? = null,
        temperature: Double? = null,
        timersEndTime: List<ContinuousClock.Instant> = emptyList(),
        stopwatches: List<SettingsWidgetTextStopwatch> = emptyList(),
        lapTimes: List<List<Double>> = emptyList(),
        now: ContinuousClock.Instant = ContinuousClock.now,
    ): String {
        val formatter = TextEffectFormatter(
            formatParts = loadTextFormat(format = format),
            timersEndTime = timersEndTime,
            stopwatches = stopwatches,
            checkboxes = emptyList(),
            ratings = emptyList(),
            lapTimes = lapTimes,
        )
        val variables = Variables(
            timestamp = ContinuousClock.now.nanoseconds,
            bitrate = "",
            bitrateAndTotal = "",
            bonding = "",
            resolution = null,
            fps = null,
            date = Instant.ofEpochSecond(1_723_350_366),
            debugOverlayLines = emptyList(),
            speed = speed,
            averageSpeed = 0.0,
            altitude = 0.0,
            distance = distance,
            splitDistance = splitDistance,
            altitudeAscent = 0.0,
            altitudeDescent = 0.0,
            splitAltitudeAscent = 0.0,
            splitAltitudeDescent = 0.0,
            slope = "",
            conditions = null,
            condition = null,
            temperature = temperature,
            feelsLikeTemperature = null,
            windSpeed = windSpeed,
            windGust = windGust,
            country = null,
            countryFlag = null,
            state = null,
            area = null,
            city = null,
            neighborhood = null,
            muted = false,
            heartRates = emptyMap(),
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
            gForce = null,
            latestSubscriber = "",
            latestFollower = "",
            systemMonitor = "",
        )
        return formatter.format(variables = variables, now = now).toPlainText()
    }
}

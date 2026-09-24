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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TextEffectFormatterParityTest {
    private val defaultLocale = Locale.getDefault()

    @Before
    fun setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Stockholm"))
        Locale.setDefault(Locale.forLanguageTag("en-SE"))
        plain("{time} {date}")
    }

    @After
    fun tearDown() {
        Locale.setDefault(defaultLocale)
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

    private fun plain(
        format: String,
        speed: Double = 0.0,
        distance: Double = 0.0,
        splitDistance: Double = 0.0,
        windSpeed: Double? = null,
        windGust: Double? = null,
    ): String {
        val formatter = TextEffectFormatter(
            formatParts = loadTextFormat(format = format),
            timersEndTime = emptyList(),
            stopwatches = emptyList(),
            checkboxes = emptyList(),
            ratings = emptyList(),
            lapTimes = emptyList(),
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
            temperature = null,
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
        return formatter.format(variables = variables, now = ContinuousClock.now).toPlainText()
    }
}

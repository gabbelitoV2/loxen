package com.moblin.android.videoeffects.text

import android.icu.number.NumberFormatter
import android.icu.number.Precision
import android.icu.util.MeasureUnit
import com.moblin.android.common.various.format
import com.moblin.android.common.various.formatOneDecimal
import com.moblin.android.common.various.formatPace
import com.moblin.android.common.various.formatWindAndGustSpeed
import com.moblin.android.common.various.formatWindSpeed
import com.moblin.android.common.various.formatWithSeconds
import com.moblin.android.common.various.uptimeFormatter
import com.moblin.android.various.Variables
import com.moblin.android.various.settings.SettingsWidgetTextStopwatch
import com.moblin.android.various.subtitles.Subtitles
import java.text.NumberFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

private fun createDateFormatter(): DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withZone(ZoneId.systemDefault())

private fun createFullDateFormatter(): DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withZone(ZoneId.systemDefault())

val textEffectDateFormatter: DateTimeFormatter = createDateFormatter()
val textEffectFullDateFormatter: DateTimeFormatter = createFullDateFormatter()
val textEffectTimeFormat: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault())
val textEffectShortTimeFormat: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault())

sealed class TextEffectPartData {
    data class Text(val text: String) : TextEffectPartData()

    data class ImageSystemName(val systemName: String, val plainText: String) : TextEffectPartData()

    data class ImageSystemNameTryFill(val systemName: String, val plainText: String) : TextEffectPartData()

    data class Rating(val rating: Int) : TextEffectPartData()
}

data class TextEffectPart(val id: Int, val data: TextEffectPartData)

data class TextEffectLine(val id: Int, val parts: List<TextEffectPart>)

private fun conditionToEmoji(condition: String?): String = when (condition) {
    "Clear" -> "☀️"
    "MostlyClear" -> "🌤️"
    "PartlyCloudy" -> "⛅"
    "MostlyCloudy" -> "🌥️"
    "Cloudy" -> "☁️"
    "Foggy", "Haze", "Smoky", "BlowingDust" -> "🌫️"
    "Breezy", "Windy" -> "💨"
    "Drizzle", "SunShowers" -> "🌦️"
    "Rain", "HeavyRain", "FreezingRain", "FreezingDrizzle" -> "🌧️"
    "IsolatedThunderstorms", "ScatteredThunderstorms" -> "🌩️"
    "Thunderstorms", "StrongStorms" -> "⛈️"
    "Flurries", "Snow", "SunFlurries", "BlowingSnow", "Blizzard", "Sleet", "WintryMix", "Hail" -> "🌨️"
    "HeavySnow" -> "❄️"
    "Frigid" -> "🥶"
    "Hot" -> "🥵"
    "Hurricane", "TropicalStorm" -> "🌀"
    else -> ""
}

class TextEffectFormatter {
    var formatParts: List<TextFormatPart>
    var timersEndTime: List<com.moblin.android.platform.core.ContinuousClock.Instant>
    var stopwatches: List<SettingsWidgetTextStopwatch>
    private var temperatureFormatter = MeasurementFormatter()
    private val speedFormatter = MeasurementFormatter()
    private val altitudeFormatter = MeasurementFormatter()
    private val lengthFormatter = MeasurementFormatter()
    var checkboxes: List<Boolean>
    var ratings: List<Int>
    var subtitles: MutableMap<String?, Subtitles> = mutableMapOf()
    var lapTimes: List<List<Double>>
    var timerIndex = 0
    var stopwatchIndex = 0
    var checkboxIndex = 0
    var ratingIndex = 0
    var lapTimesIndex = 0
    var lines: MutableList<TextEffectLine> = mutableListOf()
    var parts: MutableList<TextEffectPart> = mutableListOf()
    var lineId = 0
    var partId = 0

    constructor(
        formatParts: List<TextFormatPart>,
        timersEndTime: List<com.moblin.android.platform.core.ContinuousClock.Instant>,
        stopwatches: List<SettingsWidgetTextStopwatch>,
        checkboxes: List<Boolean>,
        ratings: List<Int>,
        lapTimes: List<List<Double>>,
    ) {
        this.formatParts = formatParts
        this.timersEndTime = timersEndTime
        this.stopwatches = stopwatches
        this.checkboxes = checkboxes
        this.ratings = ratings
        this.lapTimes = lapTimes
        temperatureFormatter.numberFormatter.maximumFractionDigits = 0
        speedFormatter.numberFormatter.maximumFractionDigits = 0
        altitudeFormatter.unitOptions = setOf(UnitOptions.ProvidedUnit)
        altitudeFormatter.numberFormatter.maximumFractionDigits = 0
        lengthFormatter.unitOptions = setOf(UnitOptions.ProvidedUnit)
        lengthFormatter.numberFormatter.maximumFractionDigits = 0
    }

    fun format(variables: Variables, now: com.moblin.android.platform.core.ContinuousClock.Instant): List<TextEffectLine> {
        timerIndex = 0
        stopwatchIndex = 0
        checkboxIndex = 0
        ratingIndex = 0
        lapTimesIndex = 0
        lines = mutableListOf()
        parts = mutableListOf()
        lineId = 0
        partId = 0
        for (formatPart in formatParts) {
            when (formatPart) {
                is TextFormatPart.Text -> formatText(text = formatPart.text)
                TextFormatPart.NewLine -> formatNewLine()
                TextFormatPart.Clock -> formatClock(variables = variables)
                TextFormatPart.ShortClock -> formatShortClock(variables = variables)
                TextFormatPart.Date -> formatDate(variables = variables)
                TextFormatPart.FullDate -> formatFullDate(variables = variables)
                TextFormatPart.Bitrate -> formatBitrate(variables = variables)
                TextFormatPart.BitrateAndTotal -> formatBitrateAndTotal(variables = variables)
                TextFormatPart.Bonding -> formatBonding(variables = variables)
                TextFormatPart.Resolution -> formatResolution(variables = variables)
                TextFormatPart.Fps -> formatFps(variables = variables)
                TextFormatPart.DebugOverlay -> formatDebugOverlay(variables = variables)
                is TextFormatPart.Speed -> formatSpeed(variables = variables, unit = formatPart.unit)
                is TextFormatPart.AverageSpeed ->
                    formatAverageSpeed(variables = variables, unit = formatPart.unit)
                is TextFormatPart.Altitude -> formatAltitude(variables = variables, unit = formatPart.unit)
                is TextFormatPart.Distance -> formatDistance(variables = variables, unit = formatPart.unit)
                is TextFormatPart.SplitDistance ->
                    formatSplitDistance(variables = variables, unit = formatPart.unit)
                is TextFormatPart.AltitudeAscent ->
                    formatAltitudeAscent(variables = variables, unit = formatPart.unit)
                is TextFormatPart.AltitudeDescent ->
                    formatAltitudeDescent(variables = variables, unit = formatPart.unit)
                is TextFormatPart.SplitAltitudeAscent ->
                    formatSplitAltitudeAscent(variables = variables, unit = formatPart.unit)
                is TextFormatPart.SplitAltitudeDescent ->
                    formatSplitAltitudeDescent(variables = variables, unit = formatPart.unit)
                TextFormatPart.Slope -> formatSlope(variables = variables)
                TextFormatPart.Timer -> formatTimer(variables = variables, now = now)
                TextFormatPart.Stopwatch -> formatStopwatch(variables = variables, now = now)
                TextFormatPart.Conditions -> formatConditions(variables = variables)
                is TextFormatPart.Temperature ->
                    formatTemperature(variables = variables, unit = formatPart.unit)
                is TextFormatPart.FeelsLikeTemperature ->
                    formatFeelsLikeTemperature(variables = variables, unit = formatPart.unit)
                is TextFormatPart.Wind -> formatWind(variables = variables, unit = formatPart.unit)
                TextFormatPart.Country -> formatCountry(variables = variables)
                TextFormatPart.CountryFlag -> formatCountryFlag(variables = variables)
                TextFormatPart.State -> formatState(variables = variables)
                TextFormatPart.Area -> formatArea(variables = variables)
                TextFormatPart.City -> formatCity(variables = variables)
                TextFormatPart.Neighborhood -> formatNeighborhood(variables = variables)
                TextFormatPart.Checkbox -> formatCheckbox()
                TextFormatPart.Rating -> formatRating()
                is TextFormatPart.Subtitles -> formatSubtitles(identifier = formatPart.language)
                TextFormatPart.Muted -> formatMuted(variables = variables)
                is TextFormatPart.HeartRate ->
                    formatHeartRate(variables = variables, deviceName = formatPart.value)
                TextFormatPart.ActiveEnergyBurned -> formatActiveEnergyBurned(variables = variables)
                TextFormatPart.Power -> formatPower(variables = variables)
                TextFormatPart.StepCount -> formatStepCount(variables = variables)
                TextFormatPart.WorkoutDistance -> formatWorkoutDistance(variables = variables)
                TextFormatPart.TeslaBatteryLevel -> formatTeslaBatteryLevel(variables = variables)
                TextFormatPart.TeslaDrive -> formatTeslaDrive(variables = variables)
                TextFormatPart.TeslaMedia -> formatTeslaMedia(variables = variables)
                TextFormatPart.CyclingPower -> formatCyclingPower(variables = variables)
                TextFormatPart.CyclingCadence -> formatCyclingCadence(variables = variables)
                is TextFormatPart.CyclingSpeed ->
                    formatCyclingSpeed(variables = variables, unit = formatPart.unit)
                is TextFormatPart.RunningPace ->
                    formatRunningPace(variables = variables, deviceName = formatPart.value)
                is TextFormatPart.RunningCadence ->
                    formatRunningCadence(variables = variables, deviceName = formatPart.value)
                is TextFormatPart.RunningDistance ->
                    formatRunningDistance(variables = variables, deviceName = formatPart.value)
                TextFormatPart.LapTimes -> formatLapTimes()
                TextFormatPart.BrowserTitle -> formatBrowserTitle(variables = variables)
                is TextFormatPart.GForce -> formatGForce(variables = variables)
                is TextFormatPart.GForceRecentMax -> formatGForceRecentMax(variables = variables)
                is TextFormatPart.GForceMax -> formatGForceMax(variables = variables)
                TextFormatPart.LatestSubscriber -> formatLatestSubscriber(variables = variables)
                TextFormatPart.LatestFollower -> formatLatestFollower(variables = variables)
                TextFormatPart.SystemMonitor -> formatSystemMonitor(variables = variables)
            }
            partId += 1
        }
        if (parts.isNotEmpty()) {
            lines.add(TextEffectLine(id = lineId, parts = parts))
        }
        return lines
    }

    private fun formatText(text: String) {
        appendTextPart(value = text)
    }

    private fun formatNewLine() {
        lines.add(TextEffectLine(id = lineId, parts = parts))
        lineId += 1
        parts = mutableListOf()
    }

    private fun formatClock(variables: Variables) {
        appendTextPart(value = textEffectTimeFormat.format(variables.date))
    }

    private fun formatShortClock(variables: Variables) {
        appendTextPart(value = textEffectShortTimeFormat.format(variables.date))
    }

    private fun formatDate(variables: Variables) {
        appendTextPart(value = textEffectDateFormatter.format(variables.date))
    }

    private fun formatFullDate(variables: Variables) {
        appendTextPart(value = textEffectFullDateFormatter.format(variables.date))
    }

    private fun formatBitrate(variables: Variables) {
        val bitrate = if (variables.bitrate.isEmpty()) "-" else variables.bitrate
        appendTextPart(value = "$bitrate Mbps")
    }

    private fun formatBitrateAndTotal(variables: Variables) {
        appendTextPart(value = variables.bitrateAndTotal)
    }

    private fun formatBonding(variables: Variables) {
        appendTextPart(value = variables.bonding)
    }

    private fun formatResolution(variables: Variables) {
        appendTextPart(value = variables.resolution ?: "")
    }

    private fun formatFps(variables: Variables) {
        val fps = variables.fps
        if (fps != null) {
            appendTextPart(value = fps.toString())
        } else {
            appendTextPart(value = "")
        }
    }

    private fun formatDebugOverlay(variables: Variables) {
        appendTextPart(value = variables.debugOverlayLines.joinToString(separator = "\n"))
    }

    private fun formatSpeed(variables: Variables, unit: TextFormatSpeedUnit) {
        appendTextPart(value = formatSpeed(speed = variables.speed, unit = unit))
    }

    private fun formatAverageSpeed(variables: Variables, unit: TextFormatSpeedUnit) {
        appendTextPart(value = formatSpeed(speed = variables.averageSpeed, unit = unit))
    }

    private fun formatAltitude(variables: Variables, unit: TextFormatLengthUnit) {
        formatAltitude(altitude = variables.altitude, unit = unit)
    }

    private fun formatAltitudeAscent(variables: Variables, unit: TextFormatLengthUnit) {
        formatAltitude(altitude = variables.altitudeAscent, unit = unit)
    }

    private fun formatAltitudeDescent(variables: Variables, unit: TextFormatLengthUnit) {
        formatAltitude(altitude = variables.altitudeDescent, unit = unit)
    }

    private fun formatSplitAltitudeAscent(variables: Variables, unit: TextFormatLengthUnit) {
        formatAltitude(altitude = variables.splitAltitudeAscent, unit = unit)
    }

    private fun formatSplitAltitudeDescent(variables: Variables, unit: TextFormatLengthUnit) {
        formatAltitude(altitude = variables.splitAltitudeDescent, unit = unit)
    }

    private fun formatAltitude(altitude: Double, unit: TextFormatLengthUnit) {
        val (value, measureUnit) = when (unit) {
            TextFormatLengthUnit.system ->
                if (systemUsesImperialUnits) {
                    altitude / 0.3048 to MeasureUnit.FOOT
                } else {
                    altitude to MeasureUnit.METER
                }
            TextFormatLengthUnit.meters -> altitude to MeasureUnit.METER
            TextFormatLengthUnit.kilometers -> altitude / 1000.0 to MeasureUnit.KILOMETER
            TextFormatLengthUnit.feet -> altitude / 0.3048 to MeasureUnit.FOOT
            TextFormatLengthUnit.yards -> altitude / 0.9144 to MeasureUnit.YARD
            TextFormatLengthUnit.miles -> altitude / 1609.344 to MeasureUnit.MILE
            TextFormatLengthUnit.nauticalMiles -> altitude / 1852.0 to MeasureUnit.NAUTICAL_MILE
            else -> altitude / 9.4607304725808e15 to MeasureUnit.LIGHT_YEAR
        }
        appendTextPart(value = altitudeFormatter.string(value, measureUnit))
    }

    private fun formatDistance(variables: Variables, unit: TextFormatLengthUnit) {
        formatDistance(distance = variables.distance, unit = unit)
    }

    private fun formatSplitDistance(variables: Variables, unit: TextFormatLengthUnit) {
        formatDistance(distance = variables.splitDistance, unit = unit)
    }

    private fun formatSlope(variables: Variables) {
        appendTextPart(value = variables.slope)
    }

    private fun formatTimer(variables: Variables, now: com.moblin.android.platform.core.ContinuousClock.Instant) {
        if (timerIndex < timersEndTime.size) {
            val timeLeft = maxOf(now.duration(to = timersEndTime[timerIndex]).toDouble(kotlin.time.DurationUnit.SECONDS), 0.0)
            appendTextPart(value = uptimeFormatter.string(timeLeft) ?: "")
        }
        timerIndex += 1
    }

    private fun formatStopwatch(variables: Variables, now: com.moblin.android.platform.core.ContinuousClock.Instant) {
        if (stopwatchIndex < stopwatches.size) {
            val stopwatch = stopwatches[stopwatchIndex]
            var elapsed = stopwatch.totalElapsed
            if (stopwatch.running) {
                elapsed += java.time.Duration.between(stopwatch.playPressedTime, java.time.Instant.now()).toNanos() / 1_000_000_000.0
            }
            appendTextPart(value = uptimeFormatter.string(elapsed) ?: "")
        }
        stopwatchIndex += 1
    }

    private fun formatConditions(variables: Variables) {
        val conditions = variables.conditions
        if (conditions != null) {
            parts.add(
                TextEffectPart(
                    id = partId,
                    data = TextEffectPartData.ImageSystemNameTryFill(
                        conditions,
                        plainText = conditionToEmoji(variables.condition),
                    ),
                ),
            )
        } else {
            appendTextPart(value = "-")
        }
    }

    private fun formatTemperature(variables: Variables, unit: TextFormatTemperatureUnit) {
        appendTextPart(value = formatTemperature(temperature = variables.temperature, unit = unit))
    }

    private fun formatFeelsLikeTemperature(variables: Variables, unit: TextFormatTemperatureUnit) {
        appendTextPart(value = formatTemperature(temperature = variables.feelsLikeTemperature, unit = unit))
    }

    private fun formatWind(variables: Variables, unit: TextFormatSpeedUnit) {
        val windSpeed = variables.windSpeed
        if (windSpeed != null) {
            val windGust = variables.windGust
            if (windGust != null) {
                appendTextPart(
                    value = formatSpeed(speed = windSpeed, unit = unit) + " " +
                        formatSpeed(speed = windGust, unit = unit),
                )
            } else {
                appendTextPart(value = formatSpeed(speed = windSpeed, unit = unit))
            }
        } else {
            appendTextPart(value = "-")
        }
    }

    private fun formatCountry(variables: Variables) {
        appendTextPart(value = variables.country ?: "")
    }

    private fun formatCountryFlag(variables: Variables) {
        appendTextPart(value = variables.countryFlag ?: "-")
    }

    private fun formatState(variables: Variables) {
        appendTextPart(value = variables.state ?: "-")
    }

    private fun formatArea(variables: Variables) {
        appendTextPart(value = variables.area ?: "-")
    }

    private fun formatCity(variables: Variables) {
        appendTextPart(value = variables.city ?: "-")
    }

    private fun formatNeighborhood(variables: Variables) {
        appendTextPart(value = variables.neighborhood ?: "-")
    }

    private fun formatCheckbox() {
        if (checkboxIndex < checkboxes.size) {
            val checked = checkboxes[checkboxIndex]
            parts.add(
                TextEffectPart(
                    id = partId,
                    data = TextEffectPartData.ImageSystemName(
                        if (checked) "checkmark.square" else "square",
                        plainText = if (checked) "☑️" else "⬜",
                    ),
                ),
            )
        }
        checkboxIndex += 1
    }

    private fun formatRating() {
        if (ratingIndex < ratings.size) {
            parts.add(TextEffectPart(id = partId, data = TextEffectPartData.Rating(ratings[ratingIndex])))
        }
        ratingIndex += 1
    }

    private fun formatSubtitles(identifier: String?) {
        val subtitles = subtitles[identifier] ?: return
        for (line in subtitles.lines) {
            if (parts.isNotEmpty()) {
                lines.add(TextEffectLine(id = lineId, parts = parts))
                lineId += 1
                parts = mutableListOf()
            }
            appendTextPart(value = line)
            partId += 1
        }
        if (parts.isNotEmpty()) {
            lines.add(TextEffectLine(id = lineId, parts = parts))
            lineId += 1
            parts = mutableListOf()
        }
    }

    private fun formatMuted(variables: Variables) {
        if (variables.muted) {
            parts.add(
                TextEffectPart(
                    id = partId,
                    data = TextEffectPartData.ImageSystemName("mic.slash", plainText = "🔇"),
                ),
            )
        }
    }

    private fun formatHeartRate(variables: Variables, deviceName: String) {
        appendTextPart(value = formatOptional(value = variables.heartRates[deviceName]))
    }

    private fun formatActiveEnergyBurned(variables: Variables) {
        appendTextPart(value = formatOptional(value = variables.activeEnergyBurned))
    }

    private fun formatPower(variables: Variables) {
        appendTextPart(value = formatOptional(value = variables.power))
    }

    private fun formatStepCount(variables: Variables) {
        appendTextPart(value = formatOptional(value = variables.stepCount))
    }

    private fun formatWorkoutDistance(variables: Variables) {
        appendTextPart(value = formatOptional(value = variables.workoutDistance))
    }

    private fun formatTeslaBatteryLevel(variables: Variables) {
        appendTextPart(value = variables.teslaBatteryLevel)
    }

    private fun formatTeslaDrive(variables: Variables) {
        appendTextPart(value = variables.teslaDrive)
    }

    private fun formatTeslaMedia(variables: Variables) {
        appendTextPart(value = variables.teslaMedia)
    }

    private fun formatCyclingPower(variables: Variables) {
        appendTextPart(value = variables.cyclingPower)
    }

    private fun formatCyclingCadence(variables: Variables) {
        appendTextPart(value = variables.cyclingCadence)
    }

    private fun formatRunningPace(variables: Variables, deviceName: String) {
        val speed = variables.runningMetrics[deviceName]?.speed
        if (speed != null) {
            appendTextPart(value = formatPace(speed = speed))
        } else {
            appendTextPart(value = "-")
        }
    }

    private fun formatCyclingSpeed(variables: Variables, unit: TextFormatSpeedUnit) {
        appendTextPart(value = formatSpeed(speed = variables.cyclingSpeed, unit = unit))
    }

    private fun formatRunningCadence(variables: Variables, deviceName: String) {
        val cadence = variables.runningMetrics[deviceName]?.cadence
        if (cadence != null) {
            appendTextPart(value = cadence.toString())
        } else {
            appendTextPart(value = "-")
        }
    }

    private fun formatRunningDistance(variables: Variables, deviceName: String) {
        val distance = variables.runningMetrics[deviceName]?.distance
        if (distance != null) {
            appendTextPart(value = formatSystemDistance(distance = distance))
        } else {
            appendTextPart(value = "-")
        }
    }

    private fun formatLapTimes() {
        if (lapTimesIndex < lapTimes.size) {
            var lap = 1
            for (time in lapTimes[lapTimesIndex]) {
                if (parts.isNotEmpty()) {
                    lines.add(TextEffectLine(id = lineId, parts = parts))
                    lineId += 1
                    parts = mutableListOf()
                }
                val text: String
                if (time.isInfinite()) {
                    text = "🏁 Finished 🏁"
                    lap = 1
                } else {
                    val duration = time.toLong().seconds
                    text = "Lap $lap ${duration.formatWithSeconds()}"
                    lap += 1
                }
                appendTextPart(value = text)
                partId += 1
            }
            if (parts.isNotEmpty()) {
                lines.add(TextEffectLine(id = lineId, parts = parts))
                lineId += 1
                parts = mutableListOf()
            }
        }
        lapTimesIndex += 1
    }

    private fun formatBrowserTitle(variables: Variables) {
        appendTextPart(value = variables.browserTitle)
    }

    private fun formatGForce(variables: Variables) {
        appendTextPart(value = formatOptionalOneDecimal(value = variables.gForce?.now))
    }

    private fun formatGForceRecentMax(variables: Variables) {
        appendTextPart(value = formatOptionalOneDecimal(value = variables.gForce?.recentMax))
    }

    private fun formatGForceMax(variables: Variables) {
        appendTextPart(value = formatOptionalOneDecimal(value = variables.gForce?.max))
    }

    private fun formatLatestSubscriber(variables: Variables) {
        appendTextPart(value = variables.latestSubscriber)
    }

    private fun formatLatestFollower(variables: Variables) {
        appendTextPart(value = variables.latestFollower)
    }

    private fun formatSystemMonitor(variables: Variables) {
        appendTextPart(value = variables.systemMonitor)
    }

    private fun formatOptional(value: Int?): String {
        return if (value != null) {
            value.toString()
        } else {
            "-"
        }
    }

    private fun formatOptionalOneDecimal(value: Double?): String {
        return if (value != null) {
            formatOneDecimal(value.toFloat())
        } else {
            "-"
        }
    }

    private fun formatSpeed(speed: Double, unit: TextFormatSpeedUnit): String {
        val value = maxOf(speed, 0.0)
        val (converted, measureUnit) = when (unit) {
            TextFormatSpeedUnit.system -> {
                speedFormatter.unitOptions = emptySet()
                value to MeasureUnit.METER_PER_SECOND
            }
            TextFormatSpeedUnit.metersPerSecond -> {
                speedFormatter.unitOptions = setOf(UnitOptions.ProvidedUnit)
                value to MeasureUnit.METER_PER_SECOND
            }
            TextFormatSpeedUnit.kilometersPerHour -> {
                speedFormatter.unitOptions = setOf(UnitOptions.ProvidedUnit)
                value * 3.6 to MeasureUnit.KILOMETER_PER_HOUR
            }
            TextFormatSpeedUnit.milesPerHour -> {
                speedFormatter.unitOptions = setOf(UnitOptions.ProvidedUnit)
                value * 2.2369362920544 to MeasureUnit.MILE_PER_HOUR
            }
        }
        return speedFormatter.string(converted, measureUnit)
    }

    private fun formatTemperature(temperature: Double?, unit: TextFormatTemperatureUnit): String {
        if (temperature != null) {
            val (value, measureUnit) = when (unit) {
                TextFormatTemperatureUnit.system -> {
                    temperatureFormatter.unitOptions = emptySet()
                    temperature to MeasureUnit.CELSIUS
                }
                TextFormatTemperatureUnit.kelvin -> {
                    temperatureFormatter.unitOptions = setOf(UnitOptions.ProvidedUnit)
                    temperature + 273.15 to MeasureUnit.KELVIN
                }
                TextFormatTemperatureUnit.celsius -> {
                    temperatureFormatter.unitOptions = setOf(UnitOptions.ProvidedUnit)
                    temperature to MeasureUnit.CELSIUS
                }
                TextFormatTemperatureUnit.fahrenheit -> {
                    temperatureFormatter.unitOptions = setOf(UnitOptions.ProvidedUnit)
                    temperature * 9.0 / 5.0 + 32.0 to MeasureUnit.FAHRENHEIT
                }
            }
            return temperatureFormatter.string(value, measureUnit)
        } else {
            return "-"
        }
    }

    private fun formatDistance(distance: Double, unit: TextFormatLengthUnit) {
        if (unit == TextFormatLengthUnit.system) {
            appendTextPart(value = formatSystemDistance(distance = distance))
            return
        }
        val (value, measureUnit) = when (unit) {
            TextFormatLengthUnit.system -> distance to MeasureUnit.METER
            TextFormatLengthUnit.meters -> distance to MeasureUnit.METER
            TextFormatLengthUnit.kilometers -> distance / 1000.0 to MeasureUnit.KILOMETER
            TextFormatLengthUnit.feet -> distance / 0.3048 to MeasureUnit.FOOT
            TextFormatLengthUnit.yards -> distance / 0.9144 to MeasureUnit.YARD
            TextFormatLengthUnit.miles -> distance / 1609.344 to MeasureUnit.MILE
            TextFormatLengthUnit.nauticalMiles -> distance / 1852.0 to MeasureUnit.NAUTICAL_MILE
            else -> distance / 9.4607304725808e15 to MeasureUnit.LIGHT_YEAR
        }
        appendTextPart(value = lengthFormatter.string(value, measureUnit))
    }

    private fun formatSystemDistance(distance: Double): String {
        return if (systemUsesImperialUnits) {
            lengthFormatter.string(distance / 1609.344, MeasureUnit.MILE)
        } else {
            lengthFormatter.string(distance / 1000.0, MeasureUnit.KILOMETER)
        }
    }

    private fun appendTextPart(value: String) {
        parts.add(TextEffectPart(id = partId, data = TextEffectPartData.Text(value)))
    }
}

fun List<TextEffectLine>.toPlainText(): String {
    return map { line ->
        line.parts.map { part ->
            when (val data = part.data) {
                is TextEffectPartData.Text -> data.text
                is TextEffectPartData.ImageSystemName -> data.plainText
                is TextEffectPartData.ImageSystemNameTryFill -> data.plainText
                is TextEffectPartData.Rating -> "⭐".repeat(data.rating)
            }
        }.joinToString(separator = "")
    }.joinToString(separator = " ")
}

private val systemUsesImperialUnits: Boolean
    get() = Locale.getDefault().country in setOf("US", "LR", "MM")

private enum class UnitOptions {
    ProvidedUnit,
    NaturalScale,
    TemperatureWithoutUnit,
}

private class MeasurementFormatter {
    var unitOptions: Set<UnitOptions> = setOf(UnitOptions.NaturalScale)
    val numberFormatter: NumberFormat = NumberFormat.getNumberInstance()

    fun string(value: Double, unit: MeasureUnit): String {
        val (convertedValue, convertedUnit) = if (unitOptions.contains(UnitOptions.ProvidedUnit)) {
            value to unit
        } else {
            convertToSystem(value, unit)
        }
        return NumberFormatter.withLocale(Locale.getDefault())
            .unit(convertedUnit)
            .precision(Precision.maxFraction(numberFormatter.maximumFractionDigits))
            .format(convertedValue)
            .toString()
    }

    private fun convertToSystem(value: Double, unit: MeasureUnit): Pair<Double, MeasureUnit> =
        when (unit.subtype) {
            "meter-per-second" ->
                if (systemUsesImperialUnits) {
                    value * 2.2369362920544 to MeasureUnit.MILE_PER_HOUR
                } else {
                    value * 3.6 to MeasureUnit.KILOMETER_PER_HOUR
                }
            "celsius" ->
                if (systemUsesImperialUnits) {
                    (value * 9.0 / 5.0 + 32.0) to MeasureUnit.FAHRENHEIT
                } else {
                    value to MeasureUnit.CELSIUS
                }
            "meter" ->
                if (systemUsesImperialUnits) {
                    value / 0.3048 to MeasureUnit.FOOT
                } else {
                    value to MeasureUnit.METER
                }
            else -> value to unit
        }
}

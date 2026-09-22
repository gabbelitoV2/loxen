package com.moblin.android.videoeffects.text

import com.moblin.android.localized
import com.moblin.android.various.utils.isMac

enum class UnitSpeed(val symbol: String) {
    metersPerSecond("m/s"),
    kilometersPerHour("km/h"),
    milesPerHour("mph"),
}

enum class UnitTemperature(val symbol: String) {
    kelvin("K"),
    celsius("°C"),
    fahrenheit("°F"),
}

enum class UnitLength(val symbol: String) {
    meters("m"),
    kilometers("km"),
    feet("ft"),
    yards("yd"),
    miles("mi"),
    nauticalMiles("nmi"),
    lightyears("ly"),
}

enum class TextFormatSpeedUnit {
    system,
    metersPerSecond,
    kilometersPerHour,
    milesPerHour;

    fun symbol(): String {
        return when (this) {
            system -> ""
            metersPerSecond -> "m/s"
            kilometersPerHour -> "km/h"
            milesPerHour -> "mph"
        }
    }

    override fun toString(): String {
        val symbol = toSystem()?.symbol ?: return ""
        return when (this) {
            system -> ""
            metersPerSecond -> localized("Meters per second") + " [$symbol]"
            kilometersPerHour -> localized("Kilometers per hour") + " [$symbol]"
            milesPerHour -> localized("Miles per hour") + " [$symbol]"
        }
    }

    fun toSystem(): UnitSpeed? {
        return when (this) {
            system -> null
            metersPerSecond -> UnitSpeed.metersPerSecond
            kilometersPerHour -> UnitSpeed.kilometersPerHour
            milesPerHour -> UnitSpeed.milesPerHour
        }
    }

    companion object {
        val allCases: List<TextFormatSpeedUnit>
            get() = TextFormatSpeedUnit.entries

        fun fromString(value: String): TextFormatSpeedUnit? {
            return when (value) {
                "m/s" -> metersPerSecond
                "km/h" -> kilometersPerHour
                "mph" -> milesPerHour
                else -> null
            }
        }
    }
}

enum class TextFormatTemperatureUnit {
    system,
    kelvin,
    celsius,
    fahrenheit;

    fun symbol(): String {
        return when (this) {
            system -> ""
            kelvin -> "k"
            celsius -> "c"
            fahrenheit -> "f"
        }
    }

    override fun toString(): String {
        val symbol = toSystem()?.symbol ?: return ""
        return when (this) {
            system -> ""
            kelvin -> localized("Kelvin") + " [$symbol]"
            celsius -> localized("Celsius") + " [$symbol]"
            fahrenheit -> localized("Fahrenheit") + " [$symbol]"
        }
    }

    fun toSystem(): UnitTemperature? {
        return when (this) {
            system -> null
            kelvin -> UnitTemperature.kelvin
            celsius -> UnitTemperature.celsius
            fahrenheit -> UnitTemperature.fahrenheit
        }
    }

    companion object {
        val allCases: List<TextFormatTemperatureUnit>
            get() = TextFormatTemperatureUnit.entries

        fun fromString(value: String): TextFormatTemperatureUnit? {
            return when (value) {
                "k" -> kelvin
                "c" -> celsius
                "f" -> fahrenheit
                else -> null
            }
        }
    }
}

enum class TextFormatLengthUnit {
    system,
    meters,
    kilometers,
    feet,
    yards,
    miles,
    nauticalMiles,
    lightYears;

    fun symbol(): String {
        return when (this) {
            system -> ""
            meters -> "m"
            kilometers -> "km"
            feet -> "ft"
            yards -> "yd"
            miles -> "mi"
            nauticalMiles -> "nmi"
            lightYears -> "ly"
        }
    }

    override fun toString(): String {
        val symbol = toSystem()?.symbol ?: return ""
        return when (this) {
            system -> ""
            meters -> localized("Meters") + " [$symbol]"
            kilometers -> localized("Kilometers") + " [$symbol]"
            feet -> localized("Feet") + " [$symbol]"
            yards -> localized("Yards") + " [$symbol]"
            miles -> localized("Miles") + " [$symbol]"
            nauticalMiles -> localized("Nautic miles") + " [$symbol]"
            lightYears -> localized("Light years") + " [$symbol]"
        }
    }

    fun toSystem(): UnitLength? {
        return when (this) {
            system -> null
            meters -> UnitLength.meters
            kilometers -> UnitLength.kilometers
            feet -> UnitLength.feet
            yards -> UnitLength.yards
            miles -> UnitLength.miles
            nauticalMiles -> UnitLength.nauticalMiles
            lightYears -> UnitLength.lightyears
        }
    }

    companion object {
        val allCases: List<TextFormatLengthUnit>
            get() = TextFormatLengthUnit.entries

        fun fromString(value: String): TextFormatLengthUnit? {
            return when (value) {
                "m" -> meters
                "km" -> kilometers
                "ft" -> feet
                "yd" -> yards
                "mi" -> miles
                "nmi" -> nauticalMiles
                "ly" -> lightYears
                else -> null
            }
        }
    }
}

sealed class TextFormatPart {
    data class Text(val text: String) : TextFormatPart()
    object NewLine : TextFormatPart()
    object Clock : TextFormatPart()
    object ShortClock : TextFormatPart()
    object Date : TextFormatPart()
    object FullDate : TextFormatPart()
    object Bitrate : TextFormatPart()
    object BitrateAndTotal : TextFormatPart()
    object Bonding : TextFormatPart()
    object Resolution : TextFormatPart()
    object Fps : TextFormatPart()
    object DebugOverlay : TextFormatPart()
    data class Speed(val unit: TextFormatSpeedUnit) : TextFormatPart()
    data class AverageSpeed(val unit: TextFormatSpeedUnit) : TextFormatPart()
    data class Altitude(val unit: TextFormatLengthUnit) : TextFormatPart()
    data class Distance(val unit: TextFormatLengthUnit) : TextFormatPart()
    data class SplitDistance(val unit: TextFormatLengthUnit) : TextFormatPart()
    data class AltitudeAscent(val unit: TextFormatLengthUnit) : TextFormatPart()
    data class AltitudeDescent(val unit: TextFormatLengthUnit) : TextFormatPart()
    data class SplitAltitudeAscent(val unit: TextFormatLengthUnit) : TextFormatPart()
    data class SplitAltitudeDescent(val unit: TextFormatLengthUnit) : TextFormatPart()
    object Slope : TextFormatPart()
    object Timer : TextFormatPart()
    object Stopwatch : TextFormatPart()
    object Conditions : TextFormatPart()
    data class Temperature(val unit: TextFormatTemperatureUnit) : TextFormatPart()
    data class FeelsLikeTemperature(val unit: TextFormatTemperatureUnit) : TextFormatPart()
    data class Wind(val unit: TextFormatSpeedUnit) : TextFormatPart()
    object Country : TextFormatPart()
    object CountryFlag : TextFormatPart()
    object State : TextFormatPart()
    object Area : TextFormatPart()
    object City : TextFormatPart()
    object Neighborhood : TextFormatPart()
    object Checkbox : TextFormatPart()
    object Rating : TextFormatPart()
    data class Subtitles(val language: String?) : TextFormatPart()
    object Muted : TextFormatPart()
    data class HeartRate(val value: String) : TextFormatPart()
    object ActiveEnergyBurned : TextFormatPart()
    object Power : TextFormatPart()
    object StepCount : TextFormatPart()
    object WorkoutDistance : TextFormatPart()
    object TeslaBatteryLevel : TextFormatPart()
    object TeslaDrive : TextFormatPart()
    object TeslaMedia : TextFormatPart()
    object CyclingPower : TextFormatPart()
    object CyclingCadence : TextFormatPart()
    data class CyclingSpeed(val unit: TextFormatSpeedUnit) : TextFormatPart()
    data class RunningPace(val value: String) : TextFormatPart()
    data class RunningCadence(val value: String) : TextFormatPart()
    data class RunningDistance(val value: String) : TextFormatPart()
    object LapTimes : TextFormatPart()
    object BrowserTitle : TextFormatPart()
    data class GForce(val value: String?) : TextFormatPart()
    data class GForceRecentMax(val value: String?) : TextFormatPart()
    data class GForceMax(val value: String?) : TextFormatPart()
    object LatestSubscriber : TextFormatPart()
    object LatestFollower : TextFormatPart()
    object SystemMonitor : TextFormatPart()
}

class TextFormatLoader {
    private var format: String = ""
    private var parts: MutableList<TextFormatPart> = mutableListOf()
    private var index: Int = 0
    private var textStartIndex: Int = 0

    fun load(inputFormat: String): List<TextFormatPart> {
        format = inputFormat.replace("\\n", "\n")
        parts = mutableListOf()
        index = 0
        textStartIndex = 0
        while (index < format.length) {
            when (format[index]) {
                '{' -> {
                    val formatFromIndex = format.substring(index, format.length).lowercase()
                    if (formatFromIndex.startsWith("{time}")) {
                        loadItem(TextFormatPart.Clock, 6)
                    } else if (formatFromIndex.startsWith("{shorttime}")) {
                        loadItem(TextFormatPart.ShortClock, 11)
                    } else if (formatFromIndex.startsWith("{date}")) {
                        loadItem(TextFormatPart.Date, 6)
                    } else if (formatFromIndex.startsWith("{fulldate}")) {
                        loadItem(TextFormatPart.FullDate, 10)
                    } else if (formatFromIndex.startsWith("{bitrate}")) {
                        loadItem(TextFormatPart.Bitrate, 9)
                    } else if (formatFromIndex.startsWith("{bitrateandtotal}")) {
                        loadItem(TextFormatPart.BitrateAndTotal, 17)
                    } else if (formatFromIndex.startsWith("{bonding}")) {
                        loadItem(TextFormatPart.Bonding, 9)
                    } else if (formatFromIndex.startsWith("{resolution}")) {
                        loadItem(TextFormatPart.Resolution, 12)
                    } else if (formatFromIndex.startsWith("{fps}")) {
                        loadItem(TextFormatPart.Fps, 5)
                    } else if (formatFromIndex.startsWith("{debugoverlay}")) {
                        loadItem(TextFormatPart.DebugOverlay, 14)
                    } else if (appendSpeedIfPresent(formatFromIndex)) {
                    } else if (appendAverageSpeedIfPresent(formatFromIndex)) {
                    } else if (appendAltitudeAscentIfPresent(formatFromIndex)) {
                    } else if (appendAltitudeDescentIfPresent(formatFromIndex)) {
                    } else if (appendSplitAltitudeAscentIfPresent(formatFromIndex)) {
                    } else if (appendSplitAltitudeDescentIfPresent(formatFromIndex)) {
                    } else if (appendAltitudeIfPresent(formatFromIndex)) {
                    } else if (appendRunDistanceIfPresent(formatFromIndex)) {
                    } else if (appendSplitDistanceIfPresent(formatFromIndex)) {
                    } else if (appendDistanceIfPresent(formatFromIndex)) {
                    } else if (formatFromIndex.startsWith("{slope}")) {
                        loadItem(TextFormatPart.Slope, 7)
                    } else if (formatFromIndex.startsWith("{timer}")) {
                        loadItem(TextFormatPart.Timer, 7)
                    } else if (formatFromIndex.startsWith("{stopwatch}")) {
                        loadItem(TextFormatPart.Stopwatch, 11)
                    } else if (formatFromIndex.startsWith("{conditions}")) {
                        loadItem(TextFormatPart.Conditions, 12)
                    } else if (appendTemperatureIfPresent(formatFromIndex)) {
                    } else if (appendFeelsLikeTemperatureIfPresent(formatFromIndex)) {
                    } else if (appendWindIfPresent(formatFromIndex)) {
                    } else if (formatFromIndex.startsWith("{country}")) {
                        loadItem(TextFormatPart.Country, 9)
                    } else if (formatFromIndex.startsWith("{countryflag}")) {
                        loadItem(TextFormatPart.CountryFlag, 13)
                    } else if (formatFromIndex.startsWith("{state}")) {
                        loadItem(TextFormatPart.State, 7)
                    } else if (formatFromIndex.startsWith("{area}")) {
                        loadItem(TextFormatPart.Area, 6)
                    } else if (formatFromIndex.startsWith("{city}")) {
                        loadItem(TextFormatPart.City, 6)
                    } else if (formatFromIndex.startsWith("{neighborhood}")) {
                        loadItem(TextFormatPart.Neighborhood, 14)
                    } else if (formatFromIndex.startsWith("{checkbox}")) {
                        loadItem(TextFormatPart.Checkbox, 10)
                    } else if (formatFromIndex.startsWith("{rating}")) {
                        loadItem(TextFormatPart.Rating, 8)
                    } else if (formatFromIndex.startsWith("{muted}")) {
                        loadItem(TextFormatPart.Muted, 7)
                    } else if (appendHeartRateIfPresent(formatFromIndex)) {
                    } else if (appendSubtitlesIfPresent(formatFromIndex)) {
                    } else if (appendPaceIfPresent(formatFromIndex)) {
                    } else if (appendCadenceIfPresent(formatFromIndex)) {
                    } else if (!isMac() && formatFromIndex.startsWith("{activeenergyburned}")) {
                        loadItem(TextFormatPart.ActiveEnergyBurned, 20)
                    } else if (!isMac() && formatFromIndex.startsWith("{power}")) {
                        loadItem(TextFormatPart.Power, 7)
                    } else if (!isMac() && formatFromIndex.startsWith("{stepcount}")) {
                        loadItem(TextFormatPart.StepCount, 11)
                    } else if (!isMac() && formatFromIndex.startsWith("{workoutdistance}")) {
                        loadItem(TextFormatPart.WorkoutDistance, 17)
                    } else if (formatFromIndex.startsWith("{teslabatterylevel}")) {
                        loadItem(TextFormatPart.TeslaBatteryLevel, 19)
                    } else if (formatFromIndex.startsWith("{tesladrive}")) {
                        loadItem(TextFormatPart.TeslaDrive, 12)
                    } else if (formatFromIndex.startsWith("{teslamedia}")) {
                        loadItem(TextFormatPart.TeslaMedia, 12)
                    } else if (formatFromIndex.startsWith("{cyclingpower}")) {
                        loadItem(TextFormatPart.CyclingPower, 14)
                    } else if (formatFromIndex.startsWith("{cyclingcadence}")) {
                        loadItem(TextFormatPart.CyclingCadence, 16)
                    } else if (appendCyclingSpeedIfPresent(formatFromIndex)) {
                    } else if (formatFromIndex.startsWith("{laptimes}")) {
                        loadItem(TextFormatPart.LapTimes, 10)
                    } else if (formatFromIndex.startsWith("{browsertitle}")) {
                        loadItem(TextFormatPart.BrowserTitle, 14)
                    } else if (formatFromIndex.startsWith("{gforce}")) {
                        loadItem(TextFormatPart.GForce(null), 8)
                    } else if (formatFromIndex.startsWith("{gforcerecentmax}")) {
                        loadItem(TextFormatPart.GForceRecentMax(null), 17)
                    } else if (formatFromIndex.startsWith("{gforcemax}")) {
                        loadItem(TextFormatPart.GForceMax(null), 11)
                    } else if (formatFromIndex.startsWith("{latestsubscriber}")) {
                        loadItem(TextFormatPart.LatestSubscriber, 18)
                    } else if (formatFromIndex.startsWith("{latestfollower}")) {
                        loadItem(TextFormatPart.LatestFollower, 16)
                    } else if (formatFromIndex.startsWith("{systemmonitor}")) {
                        loadItem(TextFormatPart.SystemMonitor, 15)
                    } else {
                        index += 1
                    }
                }
                '\n' -> loadItem(TextFormatPart.NewLine, 1)
                else -> index += 1
            }
        }
        appendTextIfPresent()
        return parts
    }

    private fun appendSpeedIfPresent(formatFromIndex: String): Boolean {
        return appendOptionsIfPresent<TextFormatSpeedUnit>(
            formatFromIndex,
            "{speed}",
            Regex("""\{speed:([^}]+)\}"""),
            { value -> TextFormatSpeedUnit.fromString(value) },
            { options -> TextFormatPart.Speed(options ?: TextFormatSpeedUnit.system) }
        )
    }

    private fun appendCyclingSpeedIfPresent(formatFromIndex: String): Boolean {
        return appendOptionsIfPresent<TextFormatSpeedUnit>(
            formatFromIndex,
            "{cyclingspeed}",
            Regex("""\{cyclingspeed:([^}]+)\}"""),
            { value -> TextFormatSpeedUnit.fromString(value) },
            { options -> TextFormatPart.CyclingSpeed(options ?: TextFormatSpeedUnit.system) }
        )
    }

    private fun appendAverageSpeedIfPresent(formatFromIndex: String): Boolean {
        return appendOptionsIfPresent<TextFormatSpeedUnit>(
            formatFromIndex,
            "{averagespeed}",
            Regex("""\{averagespeed:([^}]+)\}"""),
            { value -> TextFormatSpeedUnit.fromString(value) },
            { options -> TextFormatPart.AverageSpeed(options ?: TextFormatSpeedUnit.system) }
        )
    }

    private fun appendAltitudeIfPresent(formatFromIndex: String): Boolean {
        return appendOptionsIfPresent<TextFormatLengthUnit>(
            formatFromIndex,
            "{altitude}",
            Regex("""\{altitude:([^}]+)\}"""),
            { value -> TextFormatLengthUnit.fromString(value) },
            { options -> TextFormatPart.Altitude(options ?: TextFormatLengthUnit.system) }
        )
    }

    private fun appendAltitudeAscentIfPresent(formatFromIndex: String): Boolean {
        return appendOptionsIfPresent<TextFormatLengthUnit>(
            formatFromIndex,
            "{altitudeascent}",
            Regex("""\{altitudeascent:([^}]+)\}"""),
            { value -> TextFormatLengthUnit.fromString(value) },
            { options -> TextFormatPart.AltitudeAscent(options ?: TextFormatLengthUnit.system) }
        )
    }

    private fun appendAltitudeDescentIfPresent(formatFromIndex: String): Boolean {
        return appendOptionsIfPresent<TextFormatLengthUnit>(
            formatFromIndex,
            "{altitudedescent}",
            Regex("""\{altitudedescent:([^}]+)\}"""),
            { value -> TextFormatLengthUnit.fromString(value) },
            { options -> TextFormatPart.AltitudeDescent(options ?: TextFormatLengthUnit.system) }
        )
    }

    private fun appendSplitAltitudeAscentIfPresent(formatFromIndex: String): Boolean {
        return appendOptionsIfPresent<TextFormatLengthUnit>(
            formatFromIndex,
            "{splitaltitudeascent}",
            Regex("""\{splitaltitudeascent:([^}]+)\}"""),
            { value -> TextFormatLengthUnit.fromString(value) },
            { options -> TextFormatPart.SplitAltitudeAscent(options ?: TextFormatLengthUnit.system) }
        )
    }

    private fun appendSplitAltitudeDescentIfPresent(formatFromIndex: String): Boolean {
        return appendOptionsIfPresent<TextFormatLengthUnit>(
            formatFromIndex,
            "{splitaltitudedescent}",
            Regex("""\{splitaltitudedescent:([^}]+)\}"""),
            { value -> TextFormatLengthUnit.fromString(value) },
            { options -> TextFormatPart.SplitAltitudeDescent(options ?: TextFormatLengthUnit.system) }
        )
    }

    private fun appendTemperatureIfPresent(formatFromIndex: String): Boolean {
        return appendOptionsIfPresent<TextFormatTemperatureUnit>(
            formatFromIndex,
            "{temperature}",
            Regex("""\{temperature:([^}]+)\}"""),
            { value -> TextFormatTemperatureUnit.fromString(value) },
            { options -> TextFormatPart.Temperature(options ?: TextFormatTemperatureUnit.system) }
        )
    }

    private fun appendFeelsLikeTemperatureIfPresent(formatFromIndex: String): Boolean {
        return appendOptionsIfPresent<TextFormatTemperatureUnit>(
            formatFromIndex,
            "{feelsliketemperature}",
            Regex("""\{feelsliketemperature:([^}]+)\}"""),
            { value -> TextFormatTemperatureUnit.fromString(value) },
            { options -> TextFormatPart.FeelsLikeTemperature(options ?: TextFormatTemperatureUnit.system) }
        )
    }

    private fun appendHeartRateIfPresent(formatFromIndex: String): Boolean {
        return appendOptionsIfPresent<String>(
            formatFromIndex,
            "{heartrate}",
            Regex("""\{heartrate:([^}]+)\}"""),
            { it },
            { options -> TextFormatPart.HeartRate(options ?: "") }
        )
    }

    private fun appendPaceIfPresent(formatFromIndex: String): Boolean {
        return appendOptionsIfPresent<String>(
            formatFromIndex,
            "{runningpace}",
            Regex("""\{runningpace:([^}]+)\}"""),
            { it },
            { options -> TextFormatPart.RunningPace(options ?: "") }
        )
    }

    private fun appendCadenceIfPresent(formatFromIndex: String): Boolean {
        return appendOptionsIfPresent<String>(
            formatFromIndex,
            "{runningcadence}",
            Regex("""\{runningcadence:([^}]+)\}"""),
            { it },
            { options -> TextFormatPart.RunningCadence(options ?: "") }
        )
    }

    private fun appendRunDistanceIfPresent(formatFromIndex: String): Boolean {
        return appendOptionsIfPresent<String>(
            formatFromIndex,
            "{runningdistance}",
            Regex("""\{runningdistance:([^}]+)\}"""),
            { it },
            { options -> TextFormatPart.RunningDistance(options ?: "") }
        )
    }

    private fun appendSplitDistanceIfPresent(formatFromIndex: String): Boolean {
        return appendOptionsIfPresent<TextFormatLengthUnit>(
            formatFromIndex,
            "{splitdistance}",
            Regex("""\{splitdistance:([^}]+)\}"""),
            { value -> TextFormatLengthUnit.fromString(value) },
            { options -> TextFormatPart.SplitDistance(options ?: TextFormatLengthUnit.system) }
        )
    }

    private fun appendDistanceIfPresent(formatFromIndex: String): Boolean {
        return appendOptionsIfPresent<TextFormatLengthUnit>(
            formatFromIndex,
            "{distance}",
            Regex("""\{distance:([^}]+)\}"""),
            { value -> TextFormatLengthUnit.fromString(value) },
            { options -> TextFormatPart.Distance(options ?: TextFormatLengthUnit.system) }
        )
    }

    private fun appendWindIfPresent(formatFromIndex: String): Boolean {
        return appendOptionsIfPresent<TextFormatSpeedUnit>(
            formatFromIndex,
            "{wind}",
            Regex("""\{wind:([^}]+)\}"""),
            { value -> TextFormatSpeedUnit.fromString(value) },
            { options -> TextFormatPart.Wind(options ?: TextFormatSpeedUnit.system) }
        )
    }

    private fun appendSubtitlesIfPresent(formatFromIndex: String): Boolean {
        return appendOptionsIfPresent<String>(
            formatFromIndex,
            "{subtitles}",
            Regex("""\{subtitles:([^}]+)\}"""),
            { it },
            { options -> TextFormatPart.Subtitles(options) }
        )
    }

    private fun <Options> appendOptionsIfPresent(
        formatFromIndex: String,
        plain: String,
        regex: Regex,
        makeOptions: (String) -> Options?,
        makePart: (Options?) -> TextFormatPart,
    ): Boolean {
        if (formatFromIndex.startsWith(plain)) {
            loadItem(makePart(null), plain.length)
            return true
        }
        val match = regex.find(formatFromIndex, 0)
        if (match != null && match.range.first == 0) {
            val options = makeOptions(match.groupValues[1])
            if (options != null) {
                loadItem(makePart(options), match.value.length)
                return true
            }
        }
        return false
    }

    private fun appendTextIfPresent() {
        if (textStartIndex < index) {
            parts.add(TextFormatPart.Text(format.substring(textStartIndex, index)))
        }
    }

    private fun loadItem(part: TextFormatPart, offsetBy: Int) {
        appendTextIfPresent()
        parts.add(part)
        index += offsetBy
        textStartIndex = index
    }
}

fun loadTextFormat(format: String): List<TextFormatPart> {
    return TextFormatLoader().load(format)
}

fun List<TextFormatPart>.getCheckboxText(index: Int): String {
    var afterCheckbox = false
    val checkboxTexts = mutableListOf<String>()
    var currentIndex = 0
    for (variable in this) {
        when (variable) {
            is TextFormatPart.Text -> {
                if (afterCheckbox) {
                    checkboxTexts.add(variable.text)
                    afterCheckbox = false
                }
            }
            TextFormatPart.NewLine -> {
                if (afterCheckbox) {
                    checkboxTexts.add("Checkbox $currentIndex")
                }
                afterCheckbox = false
            }
            TextFormatPart.Checkbox -> {
                if (afterCheckbox) {
                    checkboxTexts.add("Checkbox $currentIndex")
                }
                afterCheckbox = true
                currentIndex += 1
            }
            else -> {}
        }
    }
    if (index >= checkboxTexts.size) {
        return ""
    }
    return checkboxTexts[index]
}

fun List<TextFormatPart>.isWorkoutVariable(): Boolean {
    for (variable in this) {
        when (variable) {
            is TextFormatPart.HeartRate -> return true
            TextFormatPart.ActiveEnergyBurned -> return true
            TextFormatPart.Power -> return true
            TextFormatPart.StepCount -> return true
            TextFormatPart.WorkoutDistance -> return true
            else -> {}
        }
    }
    return false
}

fun List<TextFormatPart>.isWeatherVariable(): Boolean {
    for (variable in this) {
        when (variable) {
            TextFormatPart.Conditions -> return true
            is TextFormatPart.Temperature -> return true
            is TextFormatPart.FeelsLikeTemperature -> return true
            is TextFormatPart.Wind -> return true
            else -> {}
        }
    }
    return false
}

fun List<TextFormatPart>.isGeographyVariable(): Boolean {
    for (variable in this) {
        when (variable) {
            TextFormatPart.Country -> return true
            TextFormatPart.CountryFlag -> return true
            TextFormatPart.State -> return true
            TextFormatPart.Area -> return true
            TextFormatPart.City -> return true
            TextFormatPart.Neighborhood -> return true
            else -> {}
        }
    }
    return false
}

fun List<TextFormatPart>.isGForceVariable(): Boolean {
    for (variable in this) {
        when (variable) {
            is TextFormatPart.GForce -> return true
            is TextFormatPart.GForceRecentMax -> return true
            is TextFormatPart.GForceMax -> return true
            else -> {}
        }
    }
    return false
}

fun List<TextFormatPart>.isLocationVariable(): Boolean {
    for (variable in this) {
        when (variable) {
            is TextFormatPart.Speed -> return true
            is TextFormatPart.AverageSpeed -> return true
            is TextFormatPart.Altitude -> return true
            is TextFormatPart.Distance -> return true
            is TextFormatPart.SplitDistance -> return true
            is TextFormatPart.AltitudeAscent -> return true
            is TextFormatPart.AltitudeDescent -> return true
            is TextFormatPart.SplitAltitudeAscent -> return true
            is TextFormatPart.SplitAltitudeDescent -> return true
            TextFormatPart.Slope -> return true
            TextFormatPart.Country -> return true
            TextFormatPart.CountryFlag -> return true
            TextFormatPart.State -> return true
            TextFormatPart.Area -> return true
            TextFormatPart.City -> return true
            TextFormatPart.Neighborhood -> return true
            else -> {}
        }
    }
    return false
}

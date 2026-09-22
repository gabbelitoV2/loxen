package com.moblin.android.various.model

import com.moblin.android.common.various.format
import com.moblin.android.various.Variables
import com.moblin.android.various.utils.emojiFlag
import com.moblin.android.videoeffects.text.TextEffectFormatter
import com.moblin.android.videoeffects.text.loadTextFormat
import com.moblin.android.videoeffects.text.toPlainText
import java.time.Instant
import kotlin.time.TimeSource

fun Model.createVariables(now: Instant, timestamp: TimeSource.Monotonic.ValueTimeMark): Variables {
    val location = locationManager.getLatestKnownLocation()
    val weather = weatherManager.getLatestWeather()
    val placemark = geographyManager.getLatestPlacemark()
    return Variables(
        timestamp = timestamp.elapsedNow().inWholeNanoseconds,
        bitrate = bitrate.speedMbpsOneDecimal.value,
        bitrateAndTotal = bitrate.speedAndTotal.value,
        bonding = bonding.statistics.value,
        resolution = currentResolution,
        fps = currentFps,
        date = now,
        debugOverlayLines = debugOverlay.debugLines.value,
        speed = location?.speed?.toDouble() ?: 0.0,
        averageSpeed = averageSpeed,
        altitude = location?.altitude ?: 0.0,
        distance = database.location.distance,
        splitDistance = database.location.splitDistance,
        altitudeAscent = database.location.altitudeAscent,
        altitudeDescent = database.location.altitudeDescent,
        splitAltitudeAscent = database.location.splitAltitudeAscent,
        splitAltitudeDescent = database.location.splitAltitudeDescent,
        slope = "${slopePercent.toInt()}%",
        conditions = null,
        condition = null,
        temperature = null,
        feelsLikeTemperature = null,
        windSpeed = null,
        windGust = null,
        country = placemark?.countryName ?: "",
        countryFlag = emojiFlag(placemark?.countryCode),
        state = placemark?.adminArea,
        area = placemark?.subAdminArea,
        city = placemark?.locality,
        neighborhood = placemark?.subLocality,
        muted = audio.muted.value,
        heartRates = heartRates,
        activeEnergyBurned = workoutActiveEnergyBurned,
        workoutDistance = workoutDistance,
        power = workoutPower,
        stepCount = workoutStepCount,
        teslaBatteryLevel = textEffectTeslaBatteryLevel(),
        teslaDrive = textEffectTeslaDrive(),
        teslaMedia = textEffectTeslaMedia(),
        cyclingPower = "${cyclingPower} W",
        cyclingCadence = "$cyclingCadence",
        cyclingSpeed = cyclingSpeed,
        runningMetrics = runningMetrics,
        browserTitle = getBrowserTitle(),
        gForce = gForceManager?.getLatest(),
        latestSubscriber = latestSubscriber,
        latestFollower = latestFollower,
        systemMonitor = getSystemMonitor()
    )
}

fun Model.formatPlainText(formatString: String): String {
    val now = TimeSource.Monotonic.markNow()
    val variables = createVariables(Instant.now(), now)
    val formatter = TextEffectFormatter(
        formatParts = loadTextFormat(formatString),
        timersEndTime = emptyList(),
        stopwatches = emptyList(),
        checkboxes = emptyList(),
        ratings = emptyList(),
        lapTimes = emptyList()
    )
    return formatter.format(variables, now.elapsedNow().inWholeNanoseconds).toPlainText()
}

private fun Model.getSystemMonitor(): String {
    return if (database.show.systemMonitor) {
        systemMonitor.format()
    } else {
        "-% - MB"
    }
}

private fun Model.getBrowserTitle(): String {
    return if (showBrowser.value) {
        getWebBrowser().title ?: ""
    } else {
        ""
    }
}

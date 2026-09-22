package com.moblin.android.various.model

import android.location.Location
import com.moblin.android.integrations.realtimeirl.RealtimeIrl
import java.time.Instant
import kotlin.math.abs
import kotlin.math.max

fun Model.updateLocation() {
    var location = locationManager.status()
    realtimeIrl?.let { location += it.status() }
    if (location != statusTopRight.location.value) {
        statusTopRight.location.value = location
    }
}

fun Model.reloadLocation() {
    locationManager.stop()
    if (isLocationEnabled()) {
        locationManager.start(
            accuracy = database.location.desiredAccuracy,
            distanceFilter = database.location.distanceFilter,
            onUpdate = { handleLocationUpdate(it) },
        )
    }
    reloadRealtimeIrl()
}

fun Model.resetLocationData() {
    resetDistance()
    resetAltitude()
    resetAverageSpeed()
    resetSlope()
}

fun Model.resetSplitLocationData() {
    resetSplitDistance()
    resetSplitAltitude()
}

fun Model.isLocationEnabled(): Boolean {
    return database.location.enabled
}

fun Model.isLocationInPrivacyRegion(location: Location): Boolean {
    for (region in database.location.privacyRegions) {
        if (region.contains(location.latitude, location.longitude)) {
            return true
        }
    }
    return false
}

fun Model.getLatestKnownLocation(): Pair<Double, Double>? {
    val location = locationManager.getLatestKnownLocation()
    return if (location != null) {
        Pair(location.latitude, location.longitude)
    } else {
        null
    }
}

fun Model.isRealtimeIrlConfigured(): Boolean {
    return stream.value.realtimeIrlEnabled && stream.value.realtimeIrlBaseUrl.isNotEmpty() &&
        stream.value.realtimeIrlPushKey.isNotEmpty()
}

fun Model.reloadRealtimeIrl() {
    realtimeIrl?.stop()
    realtimeIrl = null
    if (isRealtimeIrlConfigured()) {
        realtimeIrl = RealtimeIrl(
            baseUrl = stream.value.realtimeIrlBaseUrl,
            pushKey = stream.value.realtimeIrlPushKey,
        )
    }
}

fun Model.updateDistance() {
    val location = locationManager.getLatestKnownLocation()
    val lastKnownLocation = latestKnownLocation
    if (lastKnownLocation != null) {
        val distance = location?.distanceTo(TODO("Convert last known location to an android location"))?.toDouble() ?: 0.0
        if (distance > (location?.accuracy?.toDouble() ?: 0.0)) {
            database.location.distance += distance
            database.location.splitDistance += distance
            latestKnownLocation = TODO("Convert android location to a model location")
        }
    } else {
        latestKnownLocation = TODO("Convert android location to a model location")
    }
}

fun Model.updateAltitude() {
    val location = locationManager.getLatestKnownLocation() ?: return
    if (location.verticalAccuracyMeters <= 0.0f) {
        return
    }
    val reference = altitudeReference
    if (reference == null) {
        altitudeReference = location.altitude
        return
    }
    val deltaAltitude = location.altitude - reference
    if (abs(deltaAltitude) < max(location.verticalAccuracyMeters.toDouble(), 3.0)) {
        return
    }
    if (deltaAltitude > 0) {
        database.location.altitudeAscent += deltaAltitude
        database.location.splitAltitudeAscent += deltaAltitude
    } else {
        database.location.altitudeDescent += -deltaAltitude
        database.location.splitAltitudeDescent += -deltaAltitude
    }
    altitudeReference = location.altitude
}

fun Model.resetSlope() {
    slopePercent = 0.0
    previousSlopeAltitude = null
    previousSlopeDistance = database.location.distance
}

fun Model.updateSlope() {
    val location = locationManager.getLatestKnownLocation() ?: return
    val deltaDistance = database.location.distance - previousSlopeDistance
    if (deltaDistance == 0.0) {
        return
    }
    previousSlopeDistance = database.location.distance
    val deltaAltitude = location.altitude - (previousSlopeAltitude ?: location.altitude)
    previousSlopeAltitude = location.altitude
    slopePercent = 0.7 * slopePercent + 0.3 * (100 * deltaAltitude / deltaDistance)
}

fun Model.resetAverageSpeed() {
    averageSpeed = 0.0
    averageSpeedStartTime = Instant.now()
    averageSpeedStartDistance = database.location.distance
}

fun Model.updateAverageSpeed(now: Long) {
    val distance = database.location.distance - averageSpeedStartDistance
    val elapsed = (now - averageSpeedStartTime.toEpochMilli()) / 1_000_000_000.0
    averageSpeed = distance / elapsed
}

fun Model.isShowingStatusLocation(): Boolean {
    return database.show.location && isLocationEnabled()
}

private fun Model.resetDistance() {
    database.location.distance = 0.0
    latestKnownLocation = null
    resetSplitDistance()
}

private fun Model.resetSplitDistance() {
    database.location.splitDistance = 0.0
}

private fun Model.resetAltitude() {
    database.location.altitudeAscent = 0.0
    database.location.altitudeDescent = 0.0
    altitudeReference = null
    resetSplitAltitude()
}

private fun Model.resetSplitAltitude() {
    database.location.splitAltitudeAscent = 0.0
    database.location.splitAltitudeDescent = 0.0
}

private fun Model.handleLocationUpdate(location: Location) {
    if (!isLive.value) {
        return
    }
    if (isLocationInPrivacyRegion(location)) {
        return
    }
    realtimeIrl?.update(location)
}

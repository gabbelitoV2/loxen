package com.moblin.android.various.utils

import com.moblin.android.various.settings.SettingsPrivacyRegion
import kotlin.math.cos

data class CLLocationCoordinate2D(val latitude: Double, val longitude: Double)

data class MKCoordinateSpan(val latitudeDelta: Double, val longitudeDelta: Double)

data class MKCoordinateRegion(val center: CLLocationCoordinate2D, val span: MKCoordinateSpan)

private fun Double.toRadians(): Double {
    return Math.toRadians(this)
}

fun SettingsPrivacyRegion.contains(coordinate: CLLocationCoordinate2D): Boolean {
    return cos((latitude - coordinate.latitude).toRadians()) >
        cos((latitudeDelta / 2.0).toRadians()) &&
        cos((longitude - coordinate.longitude).toRadians()) >
        cos((longitudeDelta / 2.0).toRadians())
}

fun toLatitudeDeltaDegrees(meters: Double): Double {
    return 360 * meters / 40_075_000
}

fun toLongitudeDeltaDegrees(meters: Double, latitudeDegrees: Double): Double {
    return 360 * meters / (40_075_000 * cos(latitudeDegrees.toRadians()))
}

fun CLLocationCoordinate2D.translateMeters(x: Double, y: Double): CLLocationCoordinate2D {
    val latitudeDelta = toLatitudeDeltaDegrees(y)
    var newLatitude = (if (latitude < 0) 360 + latitude else latitude) + latitudeDelta
    newLatitude -= (360 * (newLatitude.toInt() / 360)).toDouble()
    if (newLatitude > 270) {
        newLatitude -= 360
    } else if (newLatitude > 90) {
        newLatitude = 180 - newLatitude
    }
    val longitudeDelta = toLongitudeDeltaDegrees(x, latitude)
    var newLongitude = (if (longitude < 0) 360 + longitude else longitude) + longitudeDelta
    newLongitude -= (360 * (newLongitude.toInt() / 360)).toDouble()
    if (newLongitude > 180) {
        newLongitude -= 360
    }
    return CLLocationCoordinate2D(newLatitude, newLongitude)
}

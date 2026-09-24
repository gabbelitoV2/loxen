package com.moblin.android.platform.mapkit

import com.moblin.android.platform.coregraphics.CGPoint
import com.moblin.android.various.utils.CLLocationCoordinate2D
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

internal class MapFrame(
    val latitude: Double,
    val longitude: Double,
    val zoom: Double,
    val bearing: Double,
    val tilt: Double,
    val widthPoints: Int,
    val heightPoints: Int,
    val scale: Double,
) {
    val widthPixels: Int
        get() = (widthPoints * scale).roundToInt()

    val heightPixels: Int
        get() = (heightPoints * scale).roundToInt()
}

internal object MapKitProjection {
    const val metersPerPointAtZoomZero = 40_075_016.685578488 / 512.0
    const val minimumZoom = 0.0
    const val maximumZoom = 22.0
    private const val maximumMercatorLatitude = 85.0511287798066
    private const val tileSize = 512.0

    fun zoom(distance: Double, latitude: Double, sidePoints: Double): Double {
        val visibleMeters = 2.0 * distance * tan(PI / 12)
        if (!(visibleMeters > 0.0) || !visibleMeters.isFinite()) {
            return maximumZoom
        }
        val clampedLatitude = latitude.coerceIn(-maximumMercatorLatitude, maximumMercatorLatitude)
        val zoom = log2(metersPerPointAtZoomZero * cos(clampedLatitude * PI / 180) * sidePoints / visibleMeters)
        if (zoom.isNaN()) {
            return minimumZoom
        }
        return zoom.coerceIn(minimumZoom, maximumZoom)
    }

    fun frame(options: MKMapSnapshotter.Options): MapFrame? {
        val camera = options.camera
        val latitude = camera.centerCoordinate.latitude
        val longitude = camera.centerCoordinate.longitude
        if (!latitude.isFinite() || !longitude.isFinite()) {
            return null
        }
        val widthPoints = sanitizedPoints(options.size.width)
        val heightPoints = sanitizedPoints(options.size.height)
        val scale = if (options.scale.isFinite()) options.scale.coerceIn(0.5, 8.0) else 3.0
        val clampedLatitude = latitude.coerceIn(-90.0, 90.0)
        return MapFrame(
            latitude = clampedLatitude,
            longitude = wrapLongitude(longitude),
            zoom = zoom(
                distance = camera.centerCoordinateDistance,
                latitude = clampedLatitude,
                sidePoints = min(widthPoints, heightPoints).toDouble(),
            ),
            bearing = normalizeDegrees(if (camera.heading.isFinite()) camera.heading else 0.0),
            tilt = if (camera.pitch.isFinite()) camera.pitch.coerceIn(0.0, 60.0) else 0.0,
            widthPoints = widthPoints,
            heightPoints = heightPoints,
            scale = scale,
        )
    }

    fun point(frame: MapFrame, coordinate: CLLocationCoordinate2D): CGPoint {
        val worldSize = tileSize * 2.0.pow(frame.zoom)
        val centerX = mercatorX(frame.longitude, worldSize)
        val centerY = mercatorY(frame.latitude, worldSize)
        var dx = mercatorX(wrapLongitude(coordinate.longitude), worldSize) - centerX
        if (dx > worldSize / 2) {
            dx -= worldSize
        } else if (dx < -worldSize / 2) {
            dx += worldSize
        }
        val dy = mercatorY(coordinate.latitude, worldSize) - centerY
        val bearing = frame.bearing * PI / 180
        val x = dx * cos(bearing) + dy * sin(bearing)
        val y = -dx * sin(bearing) + dy * cos(bearing)
        return CGPoint(x = frame.widthPoints / 2.0 + x, y = frame.heightPoints / 2.0 + y)
    }

    fun wrapLongitude(longitude: Double): Double {
        if (longitude >= -180.0 && longitude <= 180.0) {
            return longitude
        }
        val wrapped = ((longitude + 180.0) % 360.0 + 360.0) % 360.0 - 180.0
        return if (wrapped == -180.0 && longitude > 0) 180.0 else wrapped
    }

    fun normalizeDegrees(degrees: Double): Double {
        val normalized = degrees % 360.0
        return if (normalized < 0) normalized + 360.0 else normalized
    }

    private fun sanitizedPoints(value: Double): Int {
        if (!value.isFinite()) {
            return 256
        }
        return max(1, min(2048, value.roundToInt()))
    }

    private fun mercatorX(longitude: Double, worldSize: Double): Double {
        return (longitude + 180.0) / 360.0 * worldSize
    }

    private fun mercatorY(latitude: Double, worldSize: Double): Double {
        val clamped = latitude.coerceIn(-maximumMercatorLatitude, maximumMercatorLatitude) * PI / 180
        return (0.5 - ln(tan(PI / 4 + clamped / 2)) / (2 * PI)) * worldSize
    }
}

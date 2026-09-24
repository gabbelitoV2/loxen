package com.moblin.android.platform.mapkit

import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.various.utils.CLLocationCoordinate2D
import com.moblin.android.various.utils.translateMeters
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.tan
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class MapKitProjectionTest {
    private fun options(
        latitude: Double,
        longitude: Double,
        distance: Double,
        heading: Double = 0.0,
    ): MKMapSnapshotter.Options {
        val camera = MKMapCamera()
        camera.centerCoordinate = CLLocationCoordinate2D(latitude = latitude, longitude = longitude)
        camera.centerCoordinateDistance = distance
        camera.heading = heading
        val options = MKMapSnapshotter.Options()
        options.camera = camera
        return options
    }

    private fun assertClose(expected: Double, actual: Double, tolerance: Double) {
        assertTrue(abs(expected - actual) <= tolerance, "expected $expected, got $actual")
    }

    @Test
    fun visibleSideMatchesThirtyDegreeFieldOfView() {
        val zoom = 16.0
        val metersPerPoint = MapKitProjection.metersPerPointAtZoomZero / 2.0.pow(zoom)
        val distance = 256 * metersPerPoint / (2 * tan(PI / 12))
        assertClose(zoom, MapKitProjection.zoom(distance = distance, latitude = 0.0, sidePoints = 256.0), 1e-9)
    }

    @Test
    fun zoomFollowsDistanceAndLatitude() {
        val base = MapKitProjection.zoom(distance = 500.0, latitude = 0.0, sidePoints = 256.0)
        assertClose(base - 1, MapKitProjection.zoom(distance = 1000.0, latitude = 0.0, sidePoints = 256.0), 1e-9)
        assertClose(base - 1, MapKitProjection.zoom(distance = 500.0, latitude = 60.0, sidePoints = 256.0), 1e-9)
        assertClose(base + 1, MapKitProjection.zoom(distance = 500.0, latitude = 0.0, sidePoints = 512.0), 1e-9)
    }

    @Test
    fun zoomIsClamped() {
        assertEquals(MapKitProjection.maximumZoom, MapKitProjection.zoom(distance = 0.0, latitude = 0.0, sidePoints = 256.0))
        assertEquals(MapKitProjection.maximumZoom, MapKitProjection.zoom(distance = -5.0, latitude = 0.0, sidePoints = 256.0))
        assertEquals(MapKitProjection.minimumZoom, MapKitProjection.zoom(distance = 1e12, latitude = 0.0, sidePoints = 256.0))
        assertEquals(MapKitProjection.maximumZoom, MapKitProjection.zoom(distance = 0.01, latitude = 0.0, sidePoints = 256.0))
    }

    @Test
    fun frameUsesOptionsSizeScaleAndHeading() {
        val options = options(latitude = 59.3, longitude = 18.07, distance = 800.0, heading = -1.0)
        options.size = CGSize(256.0, 256.0)
        options.scale = 3.0
        val frame = assertNotNull(MapKitProjection.frame(options))
        assertEquals(256, frame.widthPoints)
        assertEquals(256, frame.heightPoints)
        assertEquals(768, frame.widthPixels)
        assertEquals(768, frame.heightPixels)
        assertClose(359.0, frame.bearing, 1e-9)
        assertEquals(0.0, frame.tilt)
        assertClose(MapKitProjection.zoom(800.0, 59.3, 256.0), frame.zoom, 1e-12)
    }

    @Test
    fun frameRejectsNonFiniteCenter() {
        assertNull(MapKitProjection.frame(options(latitude = Double.NaN, longitude = 0.0, distance = 100.0)))
        assertNull(MapKitProjection.frame(options(latitude = 0.0, longitude = Double.POSITIVE_INFINITY, distance = 100.0)))
    }

    @Test
    fun longitudeWraps() {
        assertEquals(170.0, MapKitProjection.wrapLongitude(170.0))
        assertClose(-170.0, MapKitProjection.wrapLongitude(190.0), 1e-9)
        assertClose(170.0, MapKitProjection.wrapLongitude(-190.0), 1e-9)
        assertEquals(180.0, MapKitProjection.wrapLongitude(180.0))
    }

    @Test
    fun centerProjectsToImageCenter() {
        val frame = assertNotNull(MapKitProjection.frame(options(latitude = 48.1, longitude = 11.6, distance = 300.0, heading = 37.0)))
        val point = MapKitProjection.point(frame, CLLocationCoordinate2D(latitude = 48.1, longitude = 11.6))
        assertClose(128.0, point.x, 1e-6)
        assertClose(128.0, point.y, 1e-6)
    }

    @Test
    fun northIsUpWithoutHeadingAndCourseIsUpWithHeading() {
        val center = CLLocationCoordinate2D(latitude = 0.0, longitude = 0.0)
        val northUp = assertNotNull(MapKitProjection.frame(options(latitude = 0.0, longitude = 0.0, distance = 1000.0)))
        val north = MapKitProjection.point(northUp, center.translateMeters(x = 0.0, y = 50.0))
        assertClose(128.0, north.x, 1e-6)
        assertTrue(north.y < 128.0)
        val east = MapKitProjection.point(northUp, center.translateMeters(x = 50.0, y = 0.0))
        assertTrue(east.x > 128.0)
        assertClose(128.0, east.y, 1e-6)
        val facingEast = assertNotNull(
            MapKitProjection.frame(options(latitude = 0.0, longitude = 0.0, distance = 1000.0, heading = 90.0)),
        )
        val ahead = MapKitProjection.point(facingEast, center.translateMeters(x = 50.0, y = 0.0))
        assertClose(128.0, ahead.x, 1e-6)
        assertTrue(ahead.y < 128.0)
    }

    @Test
    fun dotOffsetMatchesMapEffectMaths() {
        val distance = 1200.0
        val offsetMeters = 100.0
        val location = CLLocationCoordinate2D(latitude = 0.0, longitude = 0.0)
        val center = location.translateMeters(x = 0.0, y = offsetMeters)
        val frame = assertNotNull(
            MapKitProjection.frame(options(latitude = center.latitude, longitude = center.longitude, distance = distance)),
        )
        val point = MapKitProjection.point(frame, location)
        val halfMapSideLength = tan(PI / 12) * distance
        val dotOffsetRatio = offsetMeters / halfMapSideLength
        assertClose(128.0 + dotOffsetRatio * 128.0, point.y, 0.05)
        assertClose(128.0, point.x, 1e-6)
    }
}

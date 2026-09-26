package com.moblin.android.platform.mapkit

import com.moblin.android.platform.coregraphics.CGPoint
import com.moblin.android.various.utils.CLLocationCoordinate2D
import com.moblin.android.various.utils.MKCoordinateRegion
import com.moblin.android.various.utils.MKCoordinateSpan
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class MapKitRegionTest {
    private fun frame(latitude: Double, longitude: Double, zoom: Double, bearing: Double = 0.0): MapFrame {
        return MapFrame(
            latitude = latitude,
            longitude = longitude,
            zoom = zoom,
            bearing = bearing,
            tilt = 0.0,
            widthPoints = 200,
            heightPoints = 300,
            scale = 3.0,
        )
    }

    @Test
    fun coordinateIsTheInverseOfPoint() {
        for (bearing in listOf(0.0, 37.0, 180.0, 300.0)) {
            val frame = frame(latitude = 59.3293, longitude = 18.0686, zoom = 13.5, bearing = bearing)
            val coordinate = CLLocationCoordinate2D(latitude = 59.335, longitude = 18.05)
            val point = MapKitProjection.point(frame, coordinate)
            val back = MapKitProjection.coordinate(frame, point)
            assertEquals(coordinate.latitude, back.latitude, 1e-9)
            assertEquals(coordinate.longitude, back.longitude, 1e-9)
        }
    }

    @Test
    fun centerPointIsTheCameraCenter() {
        val frame = frame(latitude = -33.86, longitude = 151.21, zoom = 10.0, bearing = 90.0)
        val center = MapKitProjection.coordinate(frame, CGPoint(x = 100.0, y = 150.0))
        assertEquals(-33.86, center.latitude, 1e-9)
        assertEquals(151.21, center.longitude, 1e-9)
    }

    @Test
    fun zoomFitsTheWholeRegion() {
        val region = MKCoordinateRegion(
            center = CLLocationCoordinate2D(latitude = 59.3293, longitude = 18.0686),
            span = MKCoordinateSpan(latitudeDelta = 0.1, longitudeDelta = 0.1),
        )
        val zoom = MapKitProjection.zoom(region, widthPoints = 200.0, heightPoints = 300.0)
        val shown = MapKitProjection.region(frame(latitude = 59.3293, longitude = 18.0686, zoom = zoom))
        assertTrue(shown.span.latitudeDelta >= 0.1 - 1e-6)
        assertTrue(shown.span.longitudeDelta >= 0.1 - 1e-6)
        assertTrue(abs(shown.span.longitudeDelta - 0.1) < 1e-6 || abs(shown.span.latitudeDelta - 0.1) < 1e-6)
    }

    @Test
    fun regionOfZoomedOutFrameIsWide() {
        val shown = MapKitProjection.region(frame(latitude = 0.0, longitude = 0.0, zoom = 0.0))
        assertTrue(shown.span.longitudeDelta > 100.0)
        assertEquals(0.0, shown.center.latitude, 1e-9)
    }

    @Test
    fun invalidRegionFallsBackToFiniteZoom() {
        val region = MKCoordinateRegion(
            center = CLLocationCoordinate2D(latitude = 10.0, longitude = 10.0),
            span = MKCoordinateSpan(latitudeDelta = 0.0, longitudeDelta = 0.0),
        )
        assertEquals(MapKitProjection.maximumZoom, MapKitProjection.zoom(region, 200.0, 200.0))
    }

    @Test
    fun rateLimiterSpacesRequestsOneIntervalApart() {
        var now = 10_000L
        val limiter = MapKitRateLimiter(intervalMs = 1_000) { now }
        assertEquals(0L, limiter.reserve())
        assertEquals(1_000L, limiter.reserve())
        assertEquals(2_000L, limiter.reserve())
        now += 5_000
        assertEquals(0L, limiter.reserve())
        now += 400
        assertEquals(600L, limiter.reserve())
    }

    @Test
    fun polylineGeoJsonCarriesColorAndWidth() {
        val json = polylineGeoJson(
            listOf(
                MapPolylineData(
                    coordinates = listOf(
                        CLLocationCoordinate2D(latitude = 1.0, longitude = 2.0),
                        CLLocationCoordinate2D(latitude = 3.0, longitude = 4.0),
                    ),
                    stroke = androidx.compose.ui.graphics.Color(0xFF007AFF),
                    lineWidth = 5.0,
                ),
                MapPolylineData(
                    coordinates = listOf(CLLocationCoordinate2D(latitude = 1.0, longitude = 2.0)),
                    stroke = androidx.compose.ui.graphics.Color.Red,
                    lineWidth = 1.0,
                ),
            ),
        )
        assertTrue(json.contains("\"coordinates\":[[2.0000000,1.0000000],[4.0000000,3.0000000]]"), json)
        assertTrue(json.contains("\"color\":\"rgba(0,122,255,1.000)\""), json)
        assertTrue(json.contains("\"width\":5.0"), json)
        assertEquals(1, Regex("\"Feature\"").findAll(json).count())
    }
}

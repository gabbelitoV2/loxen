package com.moblin.android.various.model

import android.location.Location
import android.os.Looper
import com.moblin.android.platform.mapkit.MKDirectionsTransportType
import com.moblin.android.platform.mapkit.MKMapItem
import com.moblin.android.platform.mapkit.MKPlacemark
import com.moblin.android.platform.mapkit.MapCameraPosition
import com.moblin.android.platform.mapkit.MapKitConfiguration
import com.moblin.android.platform.mapkit.MapKitCurrentLocation
import com.moblin.android.platform.mapkit.MapKitServices
import com.moblin.android.various.settings.SettingsNavigation
import com.moblin.android.various.utils.CLLocationCoordinate2D
import com.moblin.android.various.utils.MKCoordinateRegion
import com.moblin.android.various.utils.MKCoordinateSpan
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class NavigationSuite {
    private val region = MKCoordinateRegion(
        center = CLLocationCoordinate2D(latitude = 59.3, longitude = 18.0),
        span = MKCoordinateSpan(latitudeDelta = 0.01, longitudeDelta = 0.01),
    )
    private lateinit var server: MockWebServer
    private val routingUrl = MapKitConfiguration.routingUrl
    private val interval = MapKitServices.rateLimitIntervalMs

    private val osrm = """
        {"code":"Ok","routes":[{"distance":2500.0,"duration":1800.0,
          "geometry":{"type":"LineString","coordinates":[[18.06,59.33],[18.08,59.34]]},"legs":[{"summary":""}]}]}
    """.trimIndent()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        MapKitConfiguration.routingUrl = server.url("/").toString()
        MapKitServices.rateLimitIntervalMs = 0
        val location = Location("test")
        location.latitude = 59.33
        location.longitude = 18.06
        location.time = System.currentTimeMillis()
        MapKitCurrentLocation.reported = location
    }

    @After
    fun tearDown() {
        MapKitConfiguration.routingUrl = routingUrl
        MapKitServices.rateLimitIntervalMs = interval
        MapKitCurrentLocation.reported = null
        server.shutdown()
    }

    private fun waitFor(done: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 10_000
        while (!done() && System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        assertTrue(done(), "timed out")
    }

    @Test
    fun cameraPositionStaysAutomaticWithoutARegion() {
        val navigation = Navigation()
        navigation.updateCameraPosition(settings = SettingsNavigation(true, true))
        assertEquals(MapCameraPosition.automatic, navigation.cameraPosition.value)
    }

    @Test
    fun followingUserUsesUserLocationWithRegionFallback() {
        val navigation = Navigation()
        navigation.updateCameraPosition(settings = SettingsNavigation(true, false), region = region)
        assertEquals(
            MapCameraPosition.userLocation(followsHeading = false, fallback = MapCameraPosition.region(region)),
            navigation.cameraPosition.value,
        )
        navigation.updateCameraPosition(settings = SettingsNavigation(true, true), region = region)
        assertEquals(
            MapCameraPosition.userLocation(followsHeading = true, fallback = MapCameraPosition.region(region)),
            navigation.cameraPosition.value,
        )
    }

    @Test
    fun notFollowingUserUsesTheStoredRegion() {
        val navigation = Navigation()
        navigation.cameraRegion = region
        navigation.updateCameraPosition(settings = SettingsNavigation(false, true))
        assertEquals(MapCameraPosition.region(region), navigation.cameraPosition.value)
    }

    @Test
    fun transportTypesMapToMapKit() {
        assertEquals(MKDirectionsTransportType.walking, NavigationTransportType.walking.toSystem())
        assertEquals(MKDirectionsTransportType.cycling, NavigationTransportType.cycling.toSystem())
        assertEquals(MKDirectionsTransportType.automobile, NavigationTransportType.automobile.toSystem())
    }

    @Test
    fun directionsRouteFromCurrentLocationWithSelectedTransportType() {
        val navigation = Navigation()
        navigation.destination.value = MKMapItem(MKPlacemark(coordinate = CLLocationCoordinate2D(latitude = 59.34, longitude = 18.08)))
        server.enqueue(MockResponse().setBody(osrm))
        navigation.updateDirections()
        assertNull(navigation.route.value)
        waitFor { navigation.route.value != null }
        val route = assertNotNull(navigation.route.value)
        assertEquals(1800.0, route.expectedTravelTime)
        assertEquals(2500.0, route.distance)
        assertEquals(
            "/routed-foot/route/v1/driving/18.060000,59.330000;18.080000,59.340000",
            server.takeRequest(5, TimeUnit.SECONDS)!!.requestUrl!!.encodedPath,
        )
        server.enqueue(MockResponse().setBody(osrm))
        navigation.transportType.value = NavigationTransportType.cycling
        navigation.updateDirections()
        waitFor { navigation.route.value != null }
        assertTrue(server.takeRequest(5, TimeUnit.SECONDS)!!.requestUrl!!.encodedPath.startsWith("/routed-bike/"))
        server.enqueue(MockResponse().setBody(osrm))
        navigation.transportType.value = NavigationTransportType.automobile
        navigation.updateDirections()
        waitFor { navigation.route.value != null }
        assertTrue(server.takeRequest(5, TimeUnit.SECONDS)!!.requestUrl!!.encodedPath.startsWith("/routed-car/"))
    }

    @Test
    fun directionsNeedADestination() {
        val navigation = Navigation()
        navigation.updateDirections()
        Thread.sleep(100)
        shadowOf(Looper.getMainLooper()).idle()
        assertNull(navigation.route.value)
        assertEquals(0, server.requestCount)
    }
}

package com.moblin.android.platform.mapkit

import android.location.Location
import android.os.Looper
import com.moblin.android.various.utils.CLLocationCoordinate2D
import com.moblin.android.various.utils.MKCoordinateRegion
import com.moblin.android.various.utils.MKCoordinateSpan
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class MapKitServicesSuite {
    private lateinit var server: MockWebServer
    private val searchUrl = MapKitConfiguration.searchUrl
    private val routingUrl = MapKitConfiguration.routingUrl
    private val interval = MapKitServices.rateLimitIntervalMs

    private val photon = """
        {"type":"FeatureCollection","features":[
          {"type":"Feature","geometry":{"type":"Point","coordinates":[18.0710,59.3251]},
           "properties":{"name":"Stockholm Central","city":"Stockholm","country":"Sweden","countrycode":"se"}},
          {"type":"Feature","geometry":{"type":"Point","coordinates":[18.0500,59.3300]},
           "properties":{"street":"Drottninggatan","housenumber":"12","postcode":"111 51","city":"Stockholm"}},
          {"type":"Feature","geometry":{"type":"Point"},"properties":{"name":"Broken"}}
        ]}
    """.trimIndent()

    private val osrm = """
        {"code":"Ok","routes":[{"distance":1234.5,"duration":987.6,
          "geometry":{"type":"LineString","coordinates":[[18.06,59.33],[18.065,59.331],[18.07,59.3251]]},
          "legs":[{"summary":"Vasagatan","distance":1234.5,"duration":987.6}]}],"waypoints":[]}
    """.trimIndent()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        MapKitConfiguration.searchUrl = server.url("/photon").toString()
        MapKitConfiguration.routingUrl = server.url("/osrm").toString()
        MapKitServices.rateLimitIntervalMs = 0
        MapKitCurrentLocation.reported = null
    }

    @After
    fun tearDown() {
        MapKitConfiguration.searchUrl = searchUrl
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
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(done(), "timed out")
    }

    private fun reportLocation(latitude: Double, longitude: Double) {
        val location = Location("test")
        location.latitude = latitude
        location.longitude = longitude
        location.time = System.currentTimeMillis()
        MapKitCurrentLocation.reported = location
    }

    @Test
    fun localSearchIsBiasedToTheRegionAndReturnsNamedItems() {
        server.enqueue(MockResponse().setBody(photon))
        val request = MKLocalSearch.Request()
        request.naturalLanguageQuery = "central station"
        request.region = MKCoordinateRegion(
            center = CLLocationCoordinate2D(latitude = 59.33, longitude = 18.06),
            span = MKCoordinateSpan(latitudeDelta = 0.1, longitudeDelta = 0.1),
        )
        var response: MKLocalSearch.Response? = null
        var called = false
        MKLocalSearch(request = request).start { result, _ ->
            assertTrue(Looper.myLooper() === Looper.getMainLooper())
            response = result
            called = true
        }
        waitFor { called }
        val items = assertNotNull(response).mapItems
        assertEquals(listOf("Stockholm Central", "Drottninggatan 12"), items.map { it.name })
        assertEquals(59.3251, items[0].location.coordinate.latitude, 1e-9)
        assertEquals(18.0710, items[0].location.coordinate.longitude, 1e-9)
        assertEquals("SE", items[0].placemark.isoCountryCode)
        val recorded = server.takeRequest(5, TimeUnit.SECONDS)!!
        val url = recorded.requestUrl!!
        assertEquals("/photon/api", url.encodedPath)
        assertEquals("central station", url.queryParameter("q"))
        assertEquals("59.330000", url.queryParameter("lat"))
        assertEquals("18.060000", url.queryParameter("lon"))
        assertNotNull(url.queryParameter("zoom"))
        assertTrue(recorded.getHeader("User-Agent")!!.startsWith("com.loxen.app/"))
    }

    @Test
    fun localSearchWithoutResultsFails() {
        server.enqueue(MockResponse().setBody("""{"type":"FeatureCollection","features":[]}"""))
        var response: MKLocalSearch.Response? = null
        var error: Throwable? = null
        var called = false
        MKLocalSearch(request = MKLocalSearch.Request(naturalLanguageQuery = "nowhere")).start { result, failure ->
            response = result
            error = failure
            called = true
        }
        waitFor { called }
        assertNull(response)
        assertTrue(error is MKError)
    }

    @Test
    fun cancelledSearchNeverCallsItsHandler() {
        server.enqueue(MockResponse().setBody(photon).setBodyDelay(300, TimeUnit.MILLISECONDS))
        var called = false
        val search = MKLocalSearch(request = MKLocalSearch.Request(naturalLanguageQuery = "x"))
        search.start { _, _ -> called = true }
        search.cancel()
        Thread.sleep(600)
        shadowOf(Looper.getMainLooper()).idle()
        assertFalse(called)
        assertFalse(search.isSearching)
    }

    @Test
    fun mapItemRequestReverseGeocodesACoordinate() {
        server.enqueue(MockResponse().setBody(photon))
        val coordinate = CLLocationCoordinate2D(latitude = 59.3301, longitude = 18.0501)
        val descriptor = PlaceDescriptor(
            representations = listOf(PlaceDescriptor.PlaceRepresentation.coordinate(coordinate)),
            commonName = null,
        )
        val item = runBlocking { MKMapItemRequest(placeDescriptor = descriptor).mapItem() }
        assertEquals("Stockholm Central", item.name)
        assertEquals(coordinate, item.location.coordinate)
        val url = server.takeRequest(5, TimeUnit.SECONDS)!!.requestUrl!!
        assertEquals("/photon/reverse", url.encodedPath)
        assertEquals("59.330100", url.queryParameter("lat"))
        assertEquals("18.050100", url.queryParameter("lon"))
    }

    @Test
    fun mapItemRequestFailsWhenOffline() {
        server.enqueue(MockResponse().setResponseCode(503))
        val descriptor = PlaceDescriptor(
            representations = listOf(
                PlaceDescriptor.PlaceRepresentation.coordinate(CLLocationCoordinate2D(latitude = 1.0, longitude = 2.0)),
            ),
        )
        val item = runBlocking { runCatching { MKMapItemRequest(placeDescriptor = descriptor).mapItem() }.getOrNull() }
        assertNull(item)
    }

    private fun route(transportType: MKDirectionsTransportType): Pair<MKDirections.Response?, String> {
        server.enqueue(MockResponse().setBody(osrm))
        reportLocation(latitude = 59.33, longitude = 18.06)
        val destination = MKMapItem(MKPlacemark(coordinate = CLLocationCoordinate2D(latitude = 59.3251, longitude = 18.071)))
        val request = MKDirections.Request()
        request.source = MKMapItem.forCurrentLocation()
        request.destination = destination
        request.transportType = transportType
        var response: MKDirections.Response? = null
        var called = false
        MKDirections(request = request).calculate { result, _ ->
            response = result
            called = true
        }
        waitFor { called }
        return response to server.takeRequest(5, TimeUnit.SECONDS)!!.requestUrl!!.encodedPath
    }

    @Test
    fun walkingDirectionsUseTheFootRouter() {
        val (response, path) = route(MKDirectionsTransportType.walking)
        assertEquals("/osrm/routed-foot/route/v1/driving/18.060000,59.330000;18.071000,59.325100", path)
        val route = assertNotNull(response).routes.first()
        assertEquals(1234.5, route.distance)
        assertEquals(987.6, route.expectedTravelTime)
        assertEquals(3, route.polyline.pointCount)
        assertEquals(CLLocationCoordinate2D(latitude = 59.3251, longitude = 18.07), route.polyline.coordinates.last())
        assertEquals("Vasagatan", route.name)
        assertEquals(MKDirectionsTransportType.walking, route.transportType)
    }

    @Test
    fun cyclingAndDrivingDirectionsUseTheirRouters() {
        assertTrue(route(MKDirectionsTransportType.cycling).second.startsWith("/osrm/routed-bike/"))
        assertTrue(route(MKDirectionsTransportType.automobile).second.startsWith("/osrm/routed-car/"))
    }

    @Test
    fun directionsWithoutCurrentLocationFail() {
        val request = MKDirections.Request()
        request.source = MKMapItem.forCurrentLocation()
        request.destination = MKMapItem(MKPlacemark(coordinate = CLLocationCoordinate2D(latitude = 1.0, longitude = 1.0)))
        var error: Throwable? = null
        var called = false
        MKDirections(request = request).calculate { _, failure ->
            error = failure
            called = true
        }
        waitFor { called }
        assertTrue(error is MKError)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun routerErrorsAreReported() {
        server.enqueue(MockResponse().setBody("""{"code":"NoRoute","message":"Impossible route between points"}"""))
        reportLocation(latitude = 0.0, longitude = 0.0)
        val request = MKDirections.Request()
        request.destination = MKMapItem(MKPlacemark(coordinate = CLLocationCoordinate2D(latitude = 1.0, longitude = 1.0)))
        var error: Throwable? = null
        var called = false
        MKDirections(request = request).calculate { _, failure ->
            error = failure
            called = true
        }
        waitFor { called }
        assertEquals("Impossible route between points", error?.message)
    }
}

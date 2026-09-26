package com.moblin.android.platform.mapkit

import com.moblin.android.various.utils.CLLocationCoordinate2D
import kotlinx.coroutines.Job

enum class MKDirectionsTransportType {
    automobile,
    walking,
    transit,
    cycling,
    any,
}

class MKPolyline(val coordinates: List<CLLocationCoordinate2D>) {
    val pointCount: Int
        get() = coordinates.size

    val coordinate: CLLocationCoordinate2D
        get() = coordinates.getOrNull(coordinates.size / 2) ?: CLLocationCoordinate2D(latitude = 0.0, longitude = 0.0)
}

class MKRoute internal constructor(
    val polyline: MKPolyline,
    val distance: Double,
    val expectedTravelTime: Double,
    val name: String,
    val transportType: MKDirectionsTransportType,
) {
    val advisoryNotices: List<String> = emptyList()
}

class MKDirections(request: Request) {
    class Request {
        var source: MKMapItem? = null
        var destination: MKMapItem? = null
        var transportType: MKDirectionsTransportType = MKDirectionsTransportType.automobile
        var requestsAlternateRoutes: Boolean = false
    }

    class Response internal constructor(
        val source: MKMapItem,
        val destination: MKMapItem,
        val routes: List<MKRoute>,
    )

    private val source: MKMapItem? = request.source
    private val destination: MKMapItem? = request.destination
    private val transportType: MKDirectionsTransportType = request.transportType
    private val requestsAlternateRoutes: Boolean = request.requestsAlternateRoutes
    private var job: Job? = null

    @Volatile
    private var cancelled = false

    @Volatile
    var isCalculating: Boolean = false
        private set

    fun calculate(completionHandler: (Response?, Throwable?) -> Unit) {
        job?.cancel()
        cancelled = false
        isCalculating = true
        job = MapKitServices.run(
            block = { route() },
            isCancelled = { cancelled },
        ) { response, error ->
            isCalculating = false
            completionHandler(response, error)
        }
    }

    fun cancel() {
        cancelled = true
        isCalculating = false
        job?.cancel()
    }

    private suspend fun route(): Response {
        val destination = destination ?: throw MKError("Directions request has no destination")
        val profile = MapKitServices.routeProfile(transportType)
            ?: throw MKError("Directions not available for transit")
        val source = source?.takeUnless { it.isCurrentLocation } ?: currentLocationItem()
        val destinationCoordinate = if (destination.isCurrentLocation) {
            currentLocationItem().placemark.coordinate
        } else {
            destination.placemark.coordinate
        }
        val url = MapKitServices.routeUrl(
            profile = profile,
            source = source.placemark.coordinate,
            destination = destinationCoordinate,
            alternatives = requestsAlternateRoutes,
        )
        val routes = MapKitServices.parseRoutes(MapKitServices.getJson(url), transportType)
        if (routes.isEmpty()) {
            throw MKError("Directions not available")
        }
        return Response(source = source, destination = destination, routes = routes)
    }

    private fun currentLocationItem(): MKMapItem {
        val location = MapKitCurrentLocation.latest() ?: throw MKError("Current location is unknown")
        return MKMapItem(location)
    }
}

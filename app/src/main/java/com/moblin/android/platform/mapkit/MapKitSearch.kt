package com.moblin.android.platform.mapkit

import android.location.Location
import com.moblin.android.various.utils.CLLocationCoordinate2D
import com.moblin.android.various.utils.MKCoordinateRegion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext

class MKPlacemark(
    val coordinate: CLLocationCoordinate2D,
    val thoroughfare: String? = null,
    val subThoroughfare: String? = null,
    val locality: String? = null,
    val subLocality: String? = null,
    val postalCode: String? = null,
    val administrativeArea: String? = null,
    val country: String? = null,
    val isoCountryCode: String? = null,
) {
    val name: String?
        get() = listOfNotNull(thoroughfare, subThoroughfare).joinToString(" ").ifEmpty { null } ?: locality

    val location: Location
        get() = coordinate.toLocation()

    val title: String?
        get() {
            val street = listOfNotNull(thoroughfare, subThoroughfare).joinToString(" ").ifEmpty { null }
            val area = listOfNotNull(postalCode, locality).joinToString(" ").ifEmpty { null }
            return listOfNotNull(street, area, country).joinToString(", ").ifEmpty { null }
        }
}

class MKMapItem(val placemark: MKPlacemark) {
    var name: String? = null
    var phoneNumber: String? = null
    var url: String? = null
    var isCurrentLocation: Boolean = false
        private set

    constructor(location: Location) : this(MKPlacemark(coordinate = location.coordinate))

    val location: Location
        get() = placemark.location

    override fun toString(): String {
        return "MKMapItem(name=$name, coordinate=${placemark.coordinate})"
    }

    companion object {
        fun forCurrentLocation(): MKMapItem {
            val location = MapKitCurrentLocation.latest()
            val coordinate = location?.coordinate ?: CLLocationCoordinate2D(latitude = 0.0, longitude = 0.0)
            val item = MKMapItem(MKPlacemark(coordinate = coordinate))
            item.name = "Current Location"
            item.isCurrentLocation = true
            return item
        }
    }
}

class PlaceDescriptor(
    val representations: List<PlaceRepresentation>,
    val commonName: String? = null,
) {
    sealed class PlaceRepresentation {
        data class coordinate(val coordinate: CLLocationCoordinate2D) : PlaceRepresentation()

        data class address(val address: String) : PlaceRepresentation()

        data class deviceLocation(val location: Location) : PlaceRepresentation()
    }

    constructor(item: MKMapItem) : this(
        representations = listOf(PlaceRepresentation.coordinate(item.placemark.coordinate)),
        commonName = item.name,
    )
}

class MKMapItemRequest(val placeDescriptor: PlaceDescriptor) {
    @Volatile
    var isLoading: Boolean = false
        private set

    @Volatile
    var isCancelled: Boolean = false
        private set

    private var job: Job? = null

    suspend fun mapItem(): MKMapItem {
        isLoading = true
        try {
            return withContext(Dispatchers.IO) {
                resolve()
            }
        } finally {
            isLoading = false
        }
    }

    fun getMapItem(completionHandler: (MKMapItem?, Throwable?) -> Unit) {
        job?.cancel()
        isCancelled = false
        isLoading = true
        job = MapKitServices.run(
            block = { resolve() },
            isCancelled = { isCancelled },
        ) { item, error ->
            isLoading = false
            completionHandler(item, error)
        }
    }

    fun cancel() {
        isCancelled = true
        isLoading = false
        job?.cancel()
    }

    private suspend fun resolve(): MKMapItem {
        val representation = placeDescriptor.representations.firstOrNull()
            ?: throw MKError("Place descriptor has no representation")
        val item = when (representation) {
            is PlaceDescriptor.PlaceRepresentation.coordinate -> reverse(representation.coordinate)
            is PlaceDescriptor.PlaceRepresentation.deviceLocation -> reverse(representation.location.coordinate)
            is PlaceDescriptor.PlaceRepresentation.address -> {
                val url = MapKitServices.searchUrl(query = representation.address, region = null, limit = 1)
                MapKitServices.parsePhoton(MapKitServices.getJson(url)).firstOrNull()
                    ?: throw MKError("Placemark not found")
            }
        }
        placeDescriptor.commonName?.let { item.name = it }
        return item
    }

    private suspend fun reverse(coordinate: CLLocationCoordinate2D): MKMapItem {
        val found = MapKitServices.parsePhoton(MapKitServices.getJson(MapKitServices.reverseUrl(coordinate)))
        val item = MKMapItem(MKPlacemark(coordinate = coordinate))
        item.name = found.firstOrNull()?.name
        return item
    }
}

class MKLocalSearch(request: Request) {
    class Request() {
        var naturalLanguageQuery: String? = null
        var region: MKCoordinateRegion? = null

        constructor(naturalLanguageQuery: String, region: MKCoordinateRegion? = null) : this() {
            this.naturalLanguageQuery = naturalLanguageQuery
            this.region = region
        }
    }

    class Response internal constructor(
        val mapItems: List<MKMapItem>,
        val boundingRegion: MKCoordinateRegion,
    )

    private val query: String? = request.naturalLanguageQuery
    private val region: MKCoordinateRegion? = request.region
    private var job: Job? = null

    @Volatile
    private var cancelled = false

    @Volatile
    var isSearching: Boolean = false
        private set

    fun start(completionHandler: (Response?, Throwable?) -> Unit) {
        job?.cancel()
        cancelled = false
        isSearching = true
        job = MapKitServices.run(
            block = { search() },
            isCancelled = { cancelled },
        ) { response, error ->
            isSearching = false
            completionHandler(response, error)
        }
    }

    fun cancel() {
        cancelled = true
        isSearching = false
        job?.cancel()
    }

    private suspend fun search(): Response {
        val query = query?.trim()
        if (query.isNullOrEmpty()) {
            throw MKError("Search query is empty")
        }
        val url = MapKitServices.searchUrl(query = query, region = region, limit = 15)
        val items = MapKitServices.parsePhoton(MapKitServices.getJson(url))
        if (items.isEmpty()) {
            throw MKError("Placemark not found")
        }
        return Response(
            mapItems = items,
            boundingRegion = MapKitServices.boundingRegion(items.map { it.placemark.coordinate }),
        )
    }
}

internal fun CLLocationCoordinate2D.toLocation(): Location {
    val location = Location("")
    location.latitude = latitude
    location.longitude = longitude
    return location
}

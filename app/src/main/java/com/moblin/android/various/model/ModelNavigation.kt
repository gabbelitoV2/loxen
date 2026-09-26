package com.moblin.android.various.model

import com.moblin.android.platform.mapkit.MKDirections
import com.moblin.android.platform.mapkit.MKDirectionsTransportType
import com.moblin.android.platform.mapkit.MKMapItem
import com.moblin.android.platform.mapkit.MKRoute
import com.moblin.android.platform.mapkit.MapCameraPosition
import com.moblin.android.various.MainTimer
import com.moblin.android.various.settings.SettingsNavigation
import com.moblin.android.various.utils.MKCoordinateRegion
import kotlinx.coroutines.flow.MutableStateFlow

enum class NavigationTransportType {
    walking,
    cycling,
    automobile;

    fun toSystem(): MKDirectionsTransportType {
        return when (this) {
            NavigationTransportType.walking -> MKDirectionsTransportType.walking
            NavigationTransportType.cycling -> MKDirectionsTransportType.cycling
            NavigationTransportType.automobile -> MKDirectionsTransportType.automobile
        }
    }

    fun image(): String {
        return when (this) {
            NavigationTransportType.walking -> "figure.walk"
            NavigationTransportType.cycling -> "bicycle"
            NavigationTransportType.automobile -> "car.fill"
        }
    }
}

open class Navigation internal constructor() {
    open val cameraPosition = MutableStateFlow<MapCameraPosition>(MapCameraPosition.automatic)
    open var cameraRegion: MKCoordinateRegion? = null
    open val route = MutableStateFlow<MKRoute?>(null)
    open val isSmall = MutableStateFlow(true)
    open val destination = MutableStateFlow<MKMapItem?>(null)
    open val transportType = MutableStateFlow(NavigationTransportType.walking)
    open val longPressLocation = MutableStateFlow<MKMapItem?>(null)
    open val searchText = MutableStateFlow("")
    open val searchResults = MutableStateFlow<List<MKMapItem>>(emptyList())
    open val timer = MainTimer()

    open fun updateCameraPosition(settings: SettingsNavigation, region: MKCoordinateRegion? = null) {
        val region = region ?: cameraRegion ?: return
        if (settings.followUser.value) {
            cameraPosition.value = MapCameraPosition.userLocation(
                followsHeading = settings.followHeading.value,
                fallback = MapCameraPosition.region(region),
            )
        } else {
            cameraPosition.value = MapCameraPosition.region(region)
        }
    }

    open fun updateDirections() {
        val destination = destination.value ?: return
        route.value = null
        val request = MKDirections.Request()
        request.source = MKMapItem.forCurrentLocation()
        request.destination = destination
        request.transportType = transportType.value.toSystem()
        val directions = MKDirections(request = request)
        directions.calculate { response, _ ->
            if (response == null) {
                return@calculate
            }
            val route = response.routes.firstOrNull()
            this.route.value = route
        }
    }

    companion object {
        val shared by lazy { Navigation() }
    }
}

fun Model.navigation(): Navigation {
    return Navigation.shared
}

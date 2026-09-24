package com.moblin.android.various.model

import com.moblin.android.various.MainTimer
import com.moblin.android.various.settings.SettingsNavigation
import com.moblin.android.various.utils.MKCoordinateRegion
import kotlinx.coroutines.flow.MutableStateFlow

enum class MKDirectionsTransportType {
    automobile,
    walking,
    transit,
    cycling,
}

sealed class MapCameraPosition {
    data object automatic : MapCameraPosition()

    data class region(val region: MKCoordinateRegion) : MapCameraPosition()

    data class userLocation(
        val followsHeading: Boolean,
        val fallback: MapCameraPosition,
    ) : MapCameraPosition()
}

enum class NavigationTransportType {
    walking,
    cycling,
    automobile;

    companion object {
        val allCases: List<NavigationTransportType> = entries.toList()
    }

    fun toSystem(): MKDirectionsTransportType = when (this) {
        NavigationTransportType.walking -> MKDirectionsTransportType.walking
        NavigationTransportType.cycling -> MKDirectionsTransportType.cycling
        NavigationTransportType.automobile -> MKDirectionsTransportType.automobile
    }

    fun image(): String = when (this) {
        NavigationTransportType.walking -> "figure.walk"
        NavigationTransportType.cycling -> "bicycle"
        NavigationTransportType.automobile -> "car.fill"
    }
}

class Navigation {
    companion object {
        val shared = Navigation()
    }

    val cameraPosition = MutableStateFlow<MapCameraPosition>(MapCameraPosition.automatic)

    var cameraRegion: MKCoordinateRegion? = null

    val route = MutableStateFlow<Any?>(null)

    val isSmall = MutableStateFlow(true)

    val destination = MutableStateFlow<Any?>(null)

    val transportType = MutableStateFlow(NavigationTransportType.walking)

    val longPressLocation = MutableStateFlow<Any?>(null)

    val searchText = MutableStateFlow("")

    val searchResults = MutableStateFlow<List<Any>>(emptyList())

    val timer = MainTimer()

    fun updateCameraPosition(settings: SettingsNavigation, region: MKCoordinateRegion? = null) {
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

    fun updateDirections() {
        val destination = destination.value ?: return
        route.value = null
        Unit
    }
}

fun Model.navigation(): Navigation = Navigation.shared

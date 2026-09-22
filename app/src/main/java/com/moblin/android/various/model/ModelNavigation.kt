package com.moblin.android.various.model

import com.moblin.android.various.MainTimer
import com.moblin.android.various.settings.SettingsNavigation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class NavigationTransportType {
    walking,
    cycling,
    automobile;

    companion object {
        val allCases: List<NavigationTransportType> = entries.toList()
    }

    fun toSystem(): Any = TODO("no Android counterpart for MapKit MKDirectionsTransportType")

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

    val cameraPosition = MutableStateFlow<Any?>(null)

    var cameraRegion: Any? = null

    val route = MutableStateFlow<Any?>(null)

    val isSmall = MutableStateFlow(true)

    val destination = MutableStateFlow<Any?>(null)

    val transportType = MutableStateFlow(NavigationTransportType.walking)

    val longPressLocation = MutableStateFlow<Any?>(null)

    val searchText = MutableStateFlow("")

    val searchResults = MutableStateFlow<List<Any>>(emptyList())

    val timer = MainTimer()

    fun updateCameraPosition(settings: SettingsNavigation, region: Any? = null) {
        val region = region ?: cameraRegion ?: return
        if (settings.followUser.value) {
            cameraPosition.value = TODO("no Android counterpart for MapKit MapCameraPosition userLocation")
        } else {
            cameraPosition.value = TODO("no Android counterpart for MapKit MapCameraPosition region")
        }
    }

    fun updateDirections() {
        val destination = destination.value ?: return
        route.value = null
        TODO("no Android counterpart for MapKit MKDirections")
    }
}

fun Model.navigation(): Navigation = Navigation.shared

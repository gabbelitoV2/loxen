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

    private val _cameraPosition = MutableStateFlow<Any?>(null)
    val cameraPosition: StateFlow<Any?> = _cameraPosition.asStateFlow()

    var cameraRegion: Any? = null

    private val _route = MutableStateFlow<Any?>(null)
    val route: StateFlow<Any?> = _route.asStateFlow()

    private val _isSmall = MutableStateFlow(true)
    val isSmall: StateFlow<Boolean> = _isSmall.asStateFlow()

    private val _destination = MutableStateFlow<Any?>(null)
    val destination: StateFlow<Any?> = _destination.asStateFlow()

    private val _transportType = MutableStateFlow(NavigationTransportType.walking)
    val transportType: StateFlow<NavigationTransportType> = _transportType.asStateFlow()

    private val _longPressLocation = MutableStateFlow<Any?>(null)
    val longPressLocation: StateFlow<Any?> = _longPressLocation.asStateFlow()

    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Any>>(emptyList())
    val searchResults: StateFlow<List<Any>> = _searchResults.asStateFlow()

    val timer = MainTimer()

    fun updateCameraPosition(settings: SettingsNavigation, region: Any? = null) {
        val region = region ?: cameraRegion ?: return
        if (settings.followUser) {
            _cameraPosition.value = TODO("no Android counterpart for MapKit MapCameraPosition userLocation")
        } else {
            _cameraPosition.value = TODO("no Android counterpart for MapKit MapCameraPosition region")
        }
    }

    fun updateDirections() {
        val destination = _destination.value ?: return
        _route.value = null
        TODO("no Android counterpart for MapKit MKDirections")
    }
}

fun Model.navigation(): Navigation = Navigation.shared

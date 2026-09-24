package com.moblin.android.various.model

import com.moblin.android.various.settings.SettingsNavigation
import com.moblin.android.various.utils.CLLocationCoordinate2D
import com.moblin.android.various.utils.MKCoordinateRegion
import com.moblin.android.various.utils.MKCoordinateSpan
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NavigationSuite {
    private val region = MKCoordinateRegion(
        center = CLLocationCoordinate2D(latitude = 59.3, longitude = 18.0),
        span = MKCoordinateSpan(latitudeDelta = 0.01, longitudeDelta = 0.01),
    )

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
}

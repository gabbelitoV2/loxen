package com.moblin.android.various.settings

import com.moblin.android.localized
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class SettingsPrivacyRegion(
    var id: UUID = UUID.randomUUID(),
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var latitudeDelta: Double = 30.0,
    var longitudeDelta: Double = 30.0,
)

private fun formatMeters(value: Int): String {
    return if (value == 1) {
        localized("$value meter")
    } else {
        localized("$value meters")
    }
}

@Serializable
enum class SettingsLocationDesiredAccuracy {
    best,
    nearestTenMeters,
    hundredMeters;

    override fun toString(): String = when (this) {
        best -> localized("Best")
        nearestTenMeters -> formatMeters(10)
        hundredMeters -> formatMeters(100)
    }
}

@Serializable
enum class SettingsLocationDistanceFilter {
    none,
    oneMeter,
    threeMeters,
    fiveMeters,
    tenMeters,
    twentyMeters,
    fiftyMeters,
    hundredMeters,
    twoHundredMeters;

    override fun toString(): String = when (this) {
        none -> localized("None")
        oneMeter -> formatMeters(1)
        threeMeters -> formatMeters(3)
        fiveMeters -> formatMeters(5)
        tenMeters -> formatMeters(10)
        twentyMeters -> formatMeters(20)
        fiftyMeters -> formatMeters(50)
        hundredMeters -> formatMeters(100)
        twoHundredMeters -> formatMeters(200)
    }
}

@Serializable
class SettingsLocation {
    var enabled: Boolean = false
        set(value) {
            field = value
            enabled.value = value
        }

    @Transient
    private val enabled: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val enabledFlow: StateFlow<Boolean> = enabled.asStateFlow()

    var privacyRegions: List<SettingsPrivacyRegion> = emptyList()
        set(value) {
            field = value
            _privacyRegions.value = value
        }

    @Transient
    private val _privacyRegions: MutableStateFlow<List<SettingsPrivacyRegion>> =
        MutableStateFlow(emptyList())

    val privacyRegionsFlow: StateFlow<List<SettingsPrivacyRegion>> = _privacyRegions.asStateFlow()

    var distance: Double = 0.0
        set(value) {
            field = value
            _distance.value = value
        }

    @Transient
    private val _distance: MutableStateFlow<Double> = MutableStateFlow(0.0)

    val distanceFlow: StateFlow<Double> = _distance.asStateFlow()

    var splitDistance: Double = 0.0
        set(value) {
            field = value
            _splitDistance.value = value
        }

    @Transient
    private val _splitDistance: MutableStateFlow<Double> = MutableStateFlow(0.0)

    val splitDistanceFlow: StateFlow<Double> = _splitDistance.asStateFlow()

    var altitudeAscent: Double = 0.0
        set(value) {
            field = value
            _altitudeAscent.value = value
        }

    @Transient
    private val _altitudeAscent: MutableStateFlow<Double> = MutableStateFlow(0.0)

    val altitudeAscentFlow: StateFlow<Double> = _altitudeAscent.asStateFlow()

    var altitudeDescent: Double = 0.0
        set(value) {
            field = value
            _altitudeDescent.value = value
        }

    @Transient
    private val _altitudeDescent: MutableStateFlow<Double> = MutableStateFlow(0.0)

    val altitudeDescentFlow: StateFlow<Double> = _altitudeDescent.asStateFlow()

    var splitAltitudeAscent: Double = 0.0
        set(value) {
            field = value
            _splitAltitudeAscent.value = value
        }

    @Transient
    private val _splitAltitudeAscent: MutableStateFlow<Double> = MutableStateFlow(0.0)

    val splitAltitudeAscentFlow: StateFlow<Double> = _splitAltitudeAscent.asStateFlow()

    var splitAltitudeDescent: Double = 0.0
        set(value) {
            field = value
            _splitAltitudeDescent.value = value
        }

    @Transient
    private val _splitAltitudeDescent: MutableStateFlow<Double> = MutableStateFlow(0.0)

    val splitAltitudeDescentFlow: StateFlow<Double> = _splitAltitudeDescent.asStateFlow()

    var resetWhenGoingLive: Boolean = false
        set(value) {
            field = value
            _resetWhenGoingLive.value = value
        }

    @Transient
    private val _resetWhenGoingLive: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val resetWhenGoingLiveFlow: StateFlow<Boolean> = _resetWhenGoingLive.asStateFlow()

    var desiredAccuracy: SettingsLocationDesiredAccuracy = SettingsLocationDesiredAccuracy.best
        set(value) {
            field = value
            _desiredAccuracy.value = value
        }

    @Transient
    private val _desiredAccuracy: MutableStateFlow<SettingsLocationDesiredAccuracy> =
        MutableStateFlow(SettingsLocationDesiredAccuracy.best)

    val desiredAccuracyFlow: StateFlow<SettingsLocationDesiredAccuracy> = _desiredAccuracy.asStateFlow()

    var distanceFilter: SettingsLocationDistanceFilter = SettingsLocationDistanceFilter.none
        set(value) {
            field = value
            _distanceFilter.value = value
        }

    @Transient
    private val _distanceFilter: MutableStateFlow<SettingsLocationDistanceFilter> =
        MutableStateFlow(SettingsLocationDistanceFilter.none)

    val distanceFilterFlow: StateFlow<SettingsLocationDistanceFilter> = _distanceFilter.asStateFlow()
}

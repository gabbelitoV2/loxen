package com.moblin.android.various.settings

import com.moblin.android.localized
import com.moblin.android.platform.codable.JsonObjectSerializer
import com.moblin.android.platform.codable.UUIDSerializer
import com.moblin.android.platform.codable.codableJson
import com.moblin.android.platform.codable.decode
import com.moblin.android.platform.codable.encodeContainer
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonObject

private fun <T> JsonObject.decodeRequired(key: String, serializer: KSerializer<T>): T {
    val element = this[key] ?: throw SerializationException("Missing key '$key'")
    return codableJson.decodeFromJsonElement(serializer, element)
}

private fun <T> caseNameSerializer(serialName: String, cases: List<T>, caseName: (T) -> String): KSerializer<T> =
    JsonObjectSerializer(
        serialName,
        { value -> JsonObject(mapOf(caseName(value) to JsonObject(emptyMap()))) },
        { container ->
            val keys = container.keys.filter { key -> cases.any { caseName(it) == key } }
            if (keys.size != 1) {
                throw SerializationException("$serialName expects exactly one case key")
            }
            if (container[keys[0]] !is JsonObject) {
                throw SerializationException("$serialName case value must be an object")
            }
            cases.first { caseName(it) == keys[0] }
        },
    )

@Serializable(with = SettingsPrivacyRegion.Serializer::class)
class SettingsPrivacyRegion(
    var id: UUID = UUID.randomUUID(),
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var latitudeDelta: Double = 30.0,
    var longitudeDelta: Double = 30.0,
) {
    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("latitude", latitude)
        encode("longitude", longitude)
        encode("latitudeDelta", latitudeDelta)
        encode("longitudeDelta", longitudeDelta)
    }

    companion object {
        fun decode(container: JsonObject): SettingsPrivacyRegion {
            val region = SettingsPrivacyRegion()
            region.id = container.decodeRequired("id", UUIDSerializer)
            region.latitude = container.decodeRequired("latitude", Double.serializer())
            region.longitude = container.decodeRequired("longitude", Double.serializer())
            region.latitudeDelta = container.decodeRequired("latitudeDelta", Double.serializer())
            region.longitudeDelta = container.decodeRequired("longitudeDelta", Double.serializer())
            return region
        }
    }

    object Serializer : KSerializer<SettingsPrivacyRegion> by JsonObjectSerializer(
        "SettingsPrivacyRegion",
        { it.encode() },
        { decode(it) },
    )
}

private fun formatMeters(value: Int): String {
    return if (value == 1) {
        localized("$value meter")
    } else {
        localized("$value meters")
    }
}

@Serializable(with = SettingsLocationDesiredAccuracy.Serializer::class)
enum class SettingsLocationDesiredAccuracy {
    best,
    nearestTenMeters,
    hundredMeters;

    override fun toString(): String = when (this) {
        best -> localized("Best")
        nearestTenMeters -> formatMeters(10)
        hundredMeters -> formatMeters(100)
    }

    object Serializer : KSerializer<SettingsLocationDesiredAccuracy> by caseNameSerializer(
        "SettingsLocationDesiredAccuracy",
        entries,
        { it.name },
    )
}

@Serializable(with = SettingsLocationDistanceFilter.Serializer::class)
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

    object Serializer : KSerializer<SettingsLocationDistanceFilter> by caseNameSerializer(
        "SettingsLocationDistanceFilter",
        entries,
        { it.name },
    )
}

@Serializable(with = SettingsLocation.Serializer::class)
class SettingsLocation {
    var enabled: Boolean = false
        set(value) {
            field = value
            _enabled.value = value
        }

    private val _enabled: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val enabledFlow: StateFlow<Boolean> = _enabled.asStateFlow()

    var privacyRegions: List<SettingsPrivacyRegion> = emptyList()
        set(value) {
            field = value
            _privacyRegions.value = value
        }

    private val _privacyRegions: MutableStateFlow<List<SettingsPrivacyRegion>> =
        MutableStateFlow(emptyList())

    val privacyRegionsFlow: StateFlow<List<SettingsPrivacyRegion>> = _privacyRegions.asStateFlow()

    var distance: Double = 0.0
        set(value) {
            field = value
            _distance.value = value
        }

    private val _distance: MutableStateFlow<Double> = MutableStateFlow(0.0)

    val distanceFlow: StateFlow<Double> = _distance.asStateFlow()

    var splitDistance: Double = 0.0
        set(value) {
            field = value
            _splitDistance.value = value
        }

    private val _splitDistance: MutableStateFlow<Double> = MutableStateFlow(0.0)

    val splitDistanceFlow: StateFlow<Double> = _splitDistance.asStateFlow()

    var altitudeAscent: Double = 0.0
        set(value) {
            field = value
            _altitudeAscent.value = value
        }

    private val _altitudeAscent: MutableStateFlow<Double> = MutableStateFlow(0.0)

    val altitudeAscentFlow: StateFlow<Double> = _altitudeAscent.asStateFlow()

    var altitudeDescent: Double = 0.0
        set(value) {
            field = value
            _altitudeDescent.value = value
        }

    private val _altitudeDescent: MutableStateFlow<Double> = MutableStateFlow(0.0)

    val altitudeDescentFlow: StateFlow<Double> = _altitudeDescent.asStateFlow()

    var splitAltitudeAscent: Double = 0.0
        set(value) {
            field = value
            _splitAltitudeAscent.value = value
        }

    private val _splitAltitudeAscent: MutableStateFlow<Double> = MutableStateFlow(0.0)

    val splitAltitudeAscentFlow: StateFlow<Double> = _splitAltitudeAscent.asStateFlow()

    var splitAltitudeDescent: Double = 0.0
        set(value) {
            field = value
            _splitAltitudeDescent.value = value
        }

    private val _splitAltitudeDescent: MutableStateFlow<Double> = MutableStateFlow(0.0)

    val splitAltitudeDescentFlow: StateFlow<Double> = _splitAltitudeDescent.asStateFlow()

    var resetWhenGoingLive: Boolean = false
        set(value) {
            field = value
            _resetWhenGoingLive.value = value
        }

    private val _resetWhenGoingLive: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val resetWhenGoingLiveFlow: StateFlow<Boolean> = _resetWhenGoingLive.asStateFlow()

    var desiredAccuracy: SettingsLocationDesiredAccuracy = SettingsLocationDesiredAccuracy.best
        set(value) {
            field = value
            _desiredAccuracy.value = value
        }

    private val _desiredAccuracy: MutableStateFlow<SettingsLocationDesiredAccuracy> =
        MutableStateFlow(SettingsLocationDesiredAccuracy.best)

    val desiredAccuracyFlow: StateFlow<SettingsLocationDesiredAccuracy> = _desiredAccuracy.asStateFlow()

    var distanceFilter: SettingsLocationDistanceFilter = SettingsLocationDistanceFilter.none
        set(value) {
            field = value
            _distanceFilter.value = value
        }

    private val _distanceFilter: MutableStateFlow<SettingsLocationDistanceFilter> =
        MutableStateFlow(SettingsLocationDistanceFilter.none)

    val distanceFilterFlow: StateFlow<SettingsLocationDistanceFilter> = _distanceFilter.asStateFlow()

    fun encode(): JsonObject = encodeContainer {
        encode("enabled", enabled)
        encode("privacyRegions", privacyRegions, ListSerializer(SettingsPrivacyRegion.serializer()))
        encode("distance", distance)
        encode("splitDistance", splitDistance)
        encode("altitudeAscent", altitudeAscent)
        encode("altitudeDescent", altitudeDescent)
        encode("splitAltitudeAscent", splitAltitudeAscent)
        encode("splitAltitudeDescent", splitAltitudeDescent)
        encode("resetWhenGoingLive", resetWhenGoingLive)
        encode("desiredAccuracy", desiredAccuracy)
        encode("distanceFilter", distanceFilter)
    }

    companion object {
        fun decode(container: JsonObject): SettingsLocation {
            val location = SettingsLocation()
            location.enabled = container.decode("enabled", false)
            location.privacyRegions = container.decode(
                "privacyRegions",
                ListSerializer(SettingsPrivacyRegion.serializer()),
                emptyList(),
            )
            location.distance = container.decode("distance", 0.0)
            location.splitDistance = container.decode("splitDistance", 0.0)
            location.altitudeAscent = container.decode("altitudeAscent", 0.0)
            location.altitudeDescent = container.decode("altitudeDescent", 0.0)
            location.splitAltitudeAscent = container.decode("splitAltitudeAscent", 0.0)
            location.splitAltitudeDescent = container.decode("splitAltitudeDescent", 0.0)
            location.resetWhenGoingLive = container.decode("resetWhenGoingLive", false)
            location.desiredAccuracy = container.decode("desiredAccuracy", SettingsLocationDesiredAccuracy.best)
            location.distanceFilter = container.decode("distanceFilter", SettingsLocationDistanceFilter.none)
            return location
        }
    }

    object Serializer : KSerializer<SettingsLocation> by JsonObjectSerializer(
        "SettingsLocation",
        { it.encode() },
        { decode(it) },
    )
}

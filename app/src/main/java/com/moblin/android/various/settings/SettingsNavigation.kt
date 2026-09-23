package com.moblin.android.various.settings

import com.moblin.android.platform.codable.JsonObjectSerializer
import com.moblin.android.platform.codable.decode
import com.moblin.android.platform.codable.encodeContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable(with = SettingsNavigation.Serializer::class)
class SettingsNavigation internal constructor(
    followUser: Boolean,
    followHeading: Boolean,
) {
    enum class CodingKeys(val stringValue: String) {
        followUser("followUser"),
        followHeading("followHeading"),
    }

    private val _followUser = MutableStateFlow(followUser)
    val followUser: StateFlow<Boolean> = _followUser

    private val _followHeading = MutableStateFlow(followHeading)
    val followHeading: StateFlow<Boolean> = _followHeading

    constructor() : this(false, false)

    fun encode(): JsonObject = encodeContainer {
        encode("followUser", followUser.value)
        encode("followHeading", followHeading.value)
    }

    companion object {
        fun decode(container: JsonObject): SettingsNavigation {
            val navigation = SettingsNavigation()
            navigation._followUser.value = container.decode("followUser", false)
            navigation._followHeading.value = container.decode("followHeading", false)
            return navigation
        }
    }

    object Serializer : KSerializer<SettingsNavigation> by JsonObjectSerializer(
        "SettingsNavigation",
        { it.encode() },
        { decode(it) },
    )
}

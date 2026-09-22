package com.moblin.android.various.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

@Serializable(with = SettingsNavigationSerializer::class)
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
}

object SettingsNavigationSerializer : KSerializer<SettingsNavigation> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("SettingsNavigation") {
        element<Boolean>(SettingsNavigation.CodingKeys.followUser.stringValue)
        element<Boolean>(SettingsNavigation.CodingKeys.followHeading.stringValue)
    }

    override fun serialize(encoder: Encoder, value: SettingsNavigation) {
        encoder.encodeStructure(descriptor) {
            encodeBooleanElement(descriptor, 0, value.followUser.value)
            encodeBooleanElement(descriptor, 1, value.followHeading.value)
        }
    }

    override fun deserialize(decoder: Decoder): SettingsNavigation {
        var followUser = false
        var followHeading = false
        decoder.decodeStructure(descriptor) {
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break
                    0 -> followUser = decodeBooleanElement(descriptor, 0)
                    1 -> followHeading = decodeBooleanElement(descriptor, 1)
                    else -> throw SerializationException("Unexpected index: $index")
                }
            }
        }
        return SettingsNavigation(followUser, followHeading)
    }
}

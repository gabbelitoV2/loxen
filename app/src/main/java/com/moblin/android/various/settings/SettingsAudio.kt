package com.moblin.android.various.settings

import com.moblin.android.common.various.isMediaPlayerCameraOrMic
import com.moblin.android.common.various.isRistCameraOrMic
import com.moblin.android.common.various.isRtmpCameraOrMic
import com.moblin.android.common.various.isRtspCameraOrMic
import com.moblin.android.common.various.isSrtClientCameraOrMic
import com.moblin.android.common.various.isSrtlaCameraOrMic
import com.moblin.android.common.various.isWhepCameraOrMic
import com.moblin.android.common.various.isWhipCameraOrMic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

@Serializable(with = SettingsMicSerializer::class)
enum class SettingsMic(val rawValue: String) {
    bottom("Bottom"),
    front("Front"),
    back("Back"),
    top("Top");

    companion object {
        fun fromRawValue(value: String): SettingsMic {
            return SettingsMic.entries.firstOrNull { it.rawValue == value } ?: getDefaultMic()
        }
    }
}

object SettingsMicSerializer : KSerializer<SettingsMic> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("SettingsMic", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: SettingsMic) {
        encoder.encodeString(value.rawValue)
    }

    override fun deserialize(decoder: Decoder): SettingsMic {
        return SettingsMic.fromRawValue(decoder.decodeString())
    }
}

@Serializable(with = SettingsMicsMicSerializer::class)
class SettingsMicsMic {
    val id: String
        get() = "$inputUid ${dataSourceId ?: 0}"

    var name: String = ""
    var inputUid: String = ""
    var dataSourceId: Int? = null
    var builtInOrientation: SettingsMic? = null
    internal val _delay = MutableStateFlow(0.0)
    val delay: StateFlow<Double> get() = _delay
    internal val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> get() = _connected

    fun isAudioSession(): Boolean {
        return isBuiltin() || isExternal()
    }

    fun isBuiltin(): Boolean {
        return builtInOrientation != null
    }

    fun isExternal(): Boolean {
        if (isBuiltin()) {
            return false
        }
        if (isRtmpCameraOrMic(name)) {
            return false
        }
        if (isSrtlaCameraOrMic(name)) {
            return false
        }
        if (isSrtClientCameraOrMic(name)) {
            return false
        }
        if (isRistCameraOrMic(name)) {
            return false
        }
        if (isRtspCameraOrMic(name)) {
            return false
        }
        if (isWhipCameraOrMic(name)) {
            return false
        }
        if (isWhepCameraOrMic(name)) {
            return false
        }
        if (isMediaPlayerCameraOrMic(name)) {
            return false
        }
        return true
    }

    fun isRtmp(): Boolean {
        return isRtmpCameraOrMic(name)
    }

    fun isSrtla(): Boolean {
        return isSrtlaCameraOrMic(name)
    }

    fun isSrtClient(): Boolean {
        return isSrtClientCameraOrMic(name)
    }

    fun isRist(): Boolean {
        return isRistCameraOrMic(name)
    }

    fun isRtsp(): Boolean {
        return isRtspCameraOrMic(name)
    }

    fun isWhip(): Boolean {
        return isWhipCameraOrMic(name)
    }

    fun isWhep(): Boolean {
        return isWhepCameraOrMic(name)
    }

    fun isNetwork(): Boolean {
        return isRtmp() || isSrtla() || isSrtClient() || isRist() || isRtsp() || isWhip() || isWhep()
    }

    fun isMediaPlayer(): Boolean {
        return isMediaPlayerCameraOrMic(name)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is SettingsMicsMic) {
            return false
        }
        return inputUid == other.inputUid && dataSourceId == other.dataSourceId
    }

    override fun hashCode(): Int {
        return 31 * inputUid.hashCode() + (dataSourceId ?: 0)
    }
}

object SettingsMicsMicSerializer : KSerializer<SettingsMicsMic> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("SettingsMicsMic") {
        element("name", String.serializer().descriptor)
        element("inputUid", String.serializer().descriptor)
        element("dataSourceID", Int.serializer().nullable.descriptor)
        element("builtInOrientation", SettingsMicSerializer.nullable.descriptor)
        element("delay", Double.serializer().descriptor)
    }

    override fun serialize(encoder: Encoder, value: SettingsMicsMic) {
        val output = encoder.beginStructure(descriptor)
        output.encodeStringElement(descriptor, 0, value.name)
        output.encodeStringElement(descriptor, 1, value.inputUid)
        output.encodeNullableSerializableElement(descriptor, 2, Int.serializer(), value.dataSourceId)
        output.encodeNullableSerializableElement(descriptor, 3, SettingsMicSerializer, value.builtInOrientation)
        output.encodeDoubleElement(descriptor, 4, value.delay.value)
        output.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): SettingsMicsMic {
        val input = decoder.beginStructure(descriptor)
        val mic = SettingsMicsMic()
        while (true) {
            when (val index = input.decodeElementIndex(descriptor)) {
                0 -> mic.name = input.decodeStringElement(descriptor, 0)
                1 -> mic.inputUid = input.decodeStringElement(descriptor, 1)
                2 -> mic.dataSourceId =
                    input.decodeNullableSerializableElement(descriptor, 2, Int.serializer(), mic.dataSourceId)
                3 -> mic.builtInOrientation =
                    input.decodeNullableSerializableElement(descriptor, 3, SettingsMicSerializer, mic.builtInOrientation)
                4 -> mic._delay.value = input.decodeDoubleElement(descriptor, 4)
                CompositeDecoder.DECODE_DONE -> break
                else -> throw SerializationException("Unexpected index $index")
            }
        }
        input.endStructure(descriptor)
        return mic
    }
}

@Serializable(with = SettingsMicsSerializer::class)
class SettingsMics {
    internal val _mics = MutableStateFlow<List<SettingsMicsMic>>(emptyList())
    val mics: StateFlow<List<SettingsMicsMic>> get() = _mics
    internal val _autoSwitch = MutableStateFlow(true)
    val autoSwitch: StateFlow<Boolean> get() = _autoSwitch
    var defaultMic: String = ""
}

object SettingsMicsSerializer : KSerializer<SettingsMics> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("SettingsMics") {
        element("all", ListSerializer(SettingsMicsMicSerializer).descriptor)
        element("autoSwitch", Boolean.serializer().descriptor)
        element("defaultMic", String.serializer().descriptor)
    }

    override fun serialize(encoder: Encoder, value: SettingsMics) {
        val output = encoder.beginStructure(descriptor)
        output.encodeSerializableElement(
            descriptor,
            0,
            ListSerializer(SettingsMicsMicSerializer),
            value.mics.value
        )
        output.encodeBooleanElement(descriptor, 1, value.autoSwitch.value)
        output.encodeStringElement(descriptor, 2, value.defaultMic)
        output.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): SettingsMics {
        val input = decoder.beginStructure(descriptor)
        val settings = SettingsMics()
        while (true) {
            when (val index = input.decodeElementIndex(descriptor)) {
                0 -> settings._mics.value = input.decodeSerializableElement(
                    descriptor,
                    0,
                    ListSerializer(SettingsMicsMicSerializer),
                    settings._mics.value
                )
                1 -> settings._autoSwitch.value = input.decodeBooleanElement(descriptor, 1)
                2 -> settings.defaultMic = input.decodeStringElement(descriptor, 2)
                CompositeDecoder.DECODE_DONE -> break
                else -> throw SerializationException("Unexpected index $index")
            }
        }
        input.endStructure(descriptor)
        return settings
    }
}

@Serializable
class SettingsAudioOutputToInputChannelsMap {
    var channel1: Int = 0
    var channel2: Int = 1
}

@Serializable(with = SettingsAudioSerializer::class)
class SettingsAudio {
    var outputToInputChannelsMap: SettingsAudioOutputToInputChannelsMap = SettingsAudioOutputToInputChannelsMap()
    internal val _gainDb = MutableStateFlow(0.0f)
    val gainDb: StateFlow<Float> get() = _gainDb
    internal val _preferStereoMic = MutableStateFlow(false)
    val preferStereoMic: StateFlow<Boolean> get() = _preferStereoMic
}

object SettingsAudioSerializer : KSerializer<SettingsAudio> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("SettingsAudio") {
        element("audioOutputToInputChannelsMap", SettingsAudioOutputToInputChannelsMap.serializer().descriptor)
        element("gainDb", Float.serializer().descriptor)
        element("preferStereoMic", Boolean.serializer().descriptor)
    }

    override fun serialize(encoder: Encoder, value: SettingsAudio) {
        val output = encoder.beginStructure(descriptor)
        output.encodeSerializableElement(
            descriptor,
            0,
            SettingsAudioOutputToInputChannelsMap.serializer(),
            value.outputToInputChannelsMap
        )
        output.encodeFloatElement(descriptor, 1, value.gainDb.value)
        output.encodeBooleanElement(descriptor, 2, value.preferStereoMic.value)
        output.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): SettingsAudio {
        val input = decoder.beginStructure(descriptor)
        val settings = SettingsAudio()
        while (true) {
            when (val index = input.decodeElementIndex(descriptor)) {
                0 -> settings.outputToInputChannelsMap = input.decodeSerializableElement(
                    descriptor,
                    0,
                    SettingsAudioOutputToInputChannelsMap.serializer(),
                    settings.outputToInputChannelsMap
                )
                1 -> settings._gainDb.value = input.decodeFloatElement(descriptor, 1)
                2 -> settings._preferStereoMic.value = input.decodeBooleanElement(descriptor, 2)
                CompositeDecoder.DECODE_DONE -> break
                else -> throw SerializationException("Unexpected index $index")
            }
        }
        input.endStructure(descriptor)
        return settings
    }
}

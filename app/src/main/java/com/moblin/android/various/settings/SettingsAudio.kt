package com.moblin.android.various.settings

import com.moblin.android.common.various.isMediaPlayerCameraOrMic
import com.moblin.android.common.various.isRistCameraOrMic
import com.moblin.android.common.various.isRtmpCameraOrMic
import com.moblin.android.common.various.isRtspCameraOrMic
import com.moblin.android.common.various.isSrtClientCameraOrMic
import com.moblin.android.common.various.isSrtlaCameraOrMic
import com.moblin.android.common.various.isWhepCameraOrMic
import com.moblin.android.common.various.isWhipCameraOrMic
import com.moblin.android.platform.codable.JsonObjectSerializer
import com.moblin.android.platform.codable.codableJson
import com.moblin.android.platform.codable.decode
import com.moblin.android.platform.codable.encodeContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonObject

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

@Serializable(with = SettingsMicsMic.Serializer::class)
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
    constructor()

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

    fun encode(): JsonObject = encodeContainer {
        encode("name", name)
        encode("inputUid", inputUid)
        encode("dataSourceID", dataSourceId)
        encode("builtInOrientation", builtInOrientation)
        encode("delay", delay.value)
    }

    constructor(name: String, inputUid: String, connected: Boolean) {
        this.name = name
        this.inputUid = inputUid
        this._connected.value = connected
    }

    companion object {
        fun decode(container: JsonObject): SettingsMicsMic {
            val mic = SettingsMicsMic()
            mic.name = container.decode("name", "")
            mic.inputUid = container.decode("inputUid", "")
            mic.dataSourceId = container.decode<Int?>("dataSourceID", null)
            mic.builtInOrientation = container.decode<SettingsMic?>("builtInOrientation", null)
            mic._delay.value = container.decode("delay", 0.0)
            return mic
        }
    }

    object Serializer : KSerializer<SettingsMicsMic> by JsonObjectSerializer(
        "SettingsMicsMic",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsMics.Serializer::class)
class SettingsMics {
    internal val _mics = MutableStateFlow<List<SettingsMicsMic>>(emptyList())
    val mics: StateFlow<List<SettingsMicsMic>> get() = _mics
    internal val _autoSwitch = MutableStateFlow(true)
    val autoSwitch: StateFlow<Boolean> get() = _autoSwitch
    var defaultMic: String = ""

    fun encode(): JsonObject = encodeContainer {
        encode("all", mics.value, ListSerializer(SettingsMicsMic.serializer()))
        encode("autoSwitch", autoSwitch.value)
        encode("defaultMic", defaultMic)
    }

    companion object {
        fun decode(container: JsonObject): SettingsMics {
            val mics = SettingsMics()
            mics._mics.value = container.decode("all", ListSerializer(SettingsMicsMic.serializer()), emptyList())
            mics._autoSwitch.value = container.decode("autoSwitch", true)
            mics.defaultMic = container.decode("defaultMic", "")
            return mics
        }
    }

    object Serializer : KSerializer<SettingsMics> by JsonObjectSerializer(
        "SettingsMics",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsAudioOutputToInputChannelsMap.Serializer::class)
class SettingsAudioOutputToInputChannelsMap {
    var channel1: Int = 0
    var channel2: Int = 1

    fun encode(): JsonObject = encodeContainer {
        encode("channel1", channel1)
        encode("channel2", channel2)
    }

    companion object {
        fun decode(container: JsonObject): SettingsAudioOutputToInputChannelsMap {
            val map = SettingsAudioOutputToInputChannelsMap()
            map.channel1 = codableJson.decodeFromJsonElement(Int.serializer(), container.getValue("channel1"))
            map.channel2 = codableJson.decodeFromJsonElement(Int.serializer(), container.getValue("channel2"))
            return map
        }
    }

    object Serializer : KSerializer<SettingsAudioOutputToInputChannelsMap> by JsonObjectSerializer(
        "SettingsAudioOutputToInputChannelsMap",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsAudio.Serializer::class)
class SettingsAudio {
    var outputToInputChannelsMap: SettingsAudioOutputToInputChannelsMap = SettingsAudioOutputToInputChannelsMap()
    internal val _gainDb = MutableStateFlow(0.0f)
    val gainDb: StateFlow<Float> get() = _gainDb
    internal val _preferStereoMic = MutableStateFlow(false)
    val preferStereoMic: StateFlow<Boolean> get() = _preferStereoMic

    fun encode(): JsonObject = encodeContainer {
        encode(
            "audioOutputToInputChannelsMap",
            outputToInputChannelsMap,
            SettingsAudioOutputToInputChannelsMap.serializer(),
        )
        encode("gainDb", gainDb.value)
        encode("preferStereoMic", preferStereoMic.value)
    }

    companion object {
        fun decode(container: JsonObject): SettingsAudio {
            val audio = SettingsAudio()
            audio.outputToInputChannelsMap = container.decode(
                "audioOutputToInputChannelsMap",
                SettingsAudioOutputToInputChannelsMap.serializer(),
                SettingsAudioOutputToInputChannelsMap(),
            )
            audio._gainDb.value = container.decode("gainDb", 0.0f)
            audio._preferStereoMic.value = container.decode("preferStereoMic", false)
            return audio
        }
    }

    object Serializer : KSerializer<SettingsAudio> by JsonObjectSerializer(
        "SettingsAudio",
        { it.encode() },
        { decode(it) },
    )
}

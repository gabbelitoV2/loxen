package com.moblin.android.various.settings

import com.moblin.android.integrations.dji.djidevice.DjiDeviceState
import com.moblin.android.localized
import com.moblin.android.various.MainTimer
import com.moblin.android.various.utils.Named
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = SettingsDjiDeviceUrlTypeSerializer::class)
enum class SettingsDjiDeviceUrlType(val rawValue: String) {
    server("Server"),
    custom("Custom");

    override fun toString(): String {
        return when (this) {
            server -> localized("Server")
            custom -> localized("Custom")
        }
    }

    companion object {
        fun fromRawValue(value: String): SettingsDjiDeviceUrlType? {
            return SettingsDjiDeviceUrlType.entries.firstOrNull { it.rawValue == value }
        }
    }
}

@Serializable
enum class SettingsDjiDeviceImageStabilization(val rawValue: String) {
    off("off"),
    rockSteady("rockSteady"),
    rockSteadyPlus("rockSteadyPlus"),
    horizonBalancing("horizonBalancing"),
    horizonSteady("horizonSteady");

    override fun toString(): String {
        return when (this) {
            off -> localized("Off")
            rockSteady -> localized("RockSteady")
            rockSteadyPlus -> localized("RockSteady+")
            horizonBalancing -> localized("HorizonBalancing")
            horizonSteady -> localized("HorizonSteady")
        }
    }

    companion object {
        fun fromRawValue(value: String): SettingsDjiDeviceImageStabilization? {
            return SettingsDjiDeviceImageStabilization.entries.firstOrNull { it.rawValue == value }
        }
    }
}

@Serializable(with = SettingsDjiDeviceResolutionSerializer::class)
enum class SettingsDjiDeviceResolution(val rawValue: String) {
    r1080p("1080p"),
    r720p("720p"),
    r480p("480p");

    companion object {
        fun fromRawValue(value: String): SettingsDjiDeviceResolution? {
            return SettingsDjiDeviceResolution.entries.firstOrNull { it.rawValue == value }
        }
    }
}

@Serializable
enum class SettingsDjiDeviceModel(val rawValue: String) {
    osmoAction2("osmoAction2"),
    osmoAction3("osmoAction3"),
    osmoAction4("osmoAction4"),
    osmoAction5Pro("osmoAction5Pro"),
    osmoAction6("osmoAction6"),
    osmoPocket3("osmoPocket3"),
    osmoPocket4("osmoPocket4"),
    osmo360("osmo360"),
    unknown("unknown");

    fun hasImageStabilization(): Boolean {
        return when (this) {
            osmoAction2 -> false
            osmoAction3 -> false
            osmoAction4 -> true
            osmoAction5Pro -> true
            osmoAction6 -> true
            osmoPocket3 -> false
            osmoPocket4 -> false
            osmo360 -> true
            unknown -> false
        }
    }

    fun hasNewProtocol(): Boolean {
        return when (this) {
            osmoAction2 -> false
            osmoAction3 -> false
            osmoAction4 -> false
            osmoAction5Pro -> true
            osmoAction6 -> true
            osmoPocket3 -> false
            osmoPocket4 -> true
            osmo360 -> true
            unknown -> false
        }
    }

    fun hasVideoCodec(): Boolean {
        return when (this) {
            osmoAction2 -> false
            osmoAction3 -> false
            osmoAction4 -> false
            osmoAction5Pro -> false
            osmoAction6 -> true
            osmoPocket3 -> false
            osmoPocket4 -> true
            osmo360 -> false
            unknown -> false
        }
    }

    companion object {
        fun fromRawValue(value: String): SettingsDjiDeviceModel? {
            return SettingsDjiDeviceModel.entries.firstOrNull { it.rawValue == value }
        }
    }
}

@Serializable(with = SettingsDjiDeviceVideoCodecSerializer::class)
enum class SettingsDjiDeviceVideoCodec(val rawValue: String) {
    h265hevc("H.265/HEVC"),
    h264avc("H.264/AVC");

    fun toDjiCodec(): String {
        return when (this) {
            h264avc -> "AVC"
            h265hevc -> "HEVC"
        }
    }

    fun toDjiEnhancedRtmp(): Boolean {
        return when (this) {
            h264avc -> false
            h265hevc -> true
        }
    }

    companion object {
        fun fromRawValue(value: String): SettingsDjiDeviceVideoCodec? {
            return SettingsDjiDeviceVideoCodec.entries.firstOrNull { it.rawValue == value }
        }
    }
}

val djiDeviceBitrates: List<UInt> = listOf(
    20_000_000u,
    16_000_000u,
    12_000_000u,
    10_000_000u,
    8_000_000u,
    6_000_000u,
    4_000_000u,
    2_000_000u,
)

val djiDeviceFpss: List<Int> = listOf(25, 30)

@Serializable(with = SettingsDjiDeviceSerializer::class)
class SettingsDjiDevice : Named {
    companion object {
        val baseName: String = localized("My device")
    }

    var id: UUID = UUID.randomUUID()

    private val name = MutableStateFlow(baseName)
    override var name: String
        get() = name.value
        set(value) {
            name.value = value
        }

    private val bluetoothPeripheralName = MutableStateFlow<String?>(null)
    var bluetoothPeripheralName: String?
        get() = bluetoothPeripheralName.value
        set(value) {
            bluetoothPeripheralName.value = value
        }

    private val bluetoothPeripheralId = MutableStateFlow<UUID?>(null)
    var bluetoothPeripheralId: UUID?
        get() = bluetoothPeripheralId.value
        set(value) {
            bluetoothPeripheralId.value = value
        }

    private val _wifiSsid = MutableStateFlow("")
    var wifiSsid: String
        get() = _wifiSsid.value
        set(value) {
            _wifiSsid.value = value
        }

    private val _wifiPassword = MutableStateFlow("")
    var wifiPassword: String
        get() = _wifiPassword.value
        set(value) {
            _wifiPassword.value = value
        }

    private val _rtmpUrlType = MutableStateFlow(SettingsDjiDeviceUrlType.server)
    var rtmpUrlType: SettingsDjiDeviceUrlType
        get() = _rtmpUrlType.value
        set(value) {
            _rtmpUrlType.value = value
        }

    private val _serverRtmpStreamId = MutableStateFlow(UUID.randomUUID())
    var serverRtmpStreamId: UUID
        get() = _serverRtmpStreamId.value
        set(value) {
            _serverRtmpStreamId.value = value
        }

    private val _serverRtmpUrl = MutableStateFlow<String?>(null)
    var serverRtmpUrl: String?
        get() = _serverRtmpUrl.value
        set(value) {
            _serverRtmpUrl.value = value
        }

    private val _customRtmpUrl = MutableStateFlow("")
    var customRtmpUrl: String
        get() = _customRtmpUrl.value
        set(value) {
            _customRtmpUrl.value = value
        }

    private val _autoRestartStream = MutableStateFlow(false)
    var autoRestartStream: Boolean
        get() = _autoRestartStream.value
        set(value) {
            _autoRestartStream.value = value
        }

    private val _imageStabilization = MutableStateFlow(SettingsDjiDeviceImageStabilization.off)
    var imageStabilization: SettingsDjiDeviceImageStabilization
        get() = _imageStabilization.value
        set(value) {
            _imageStabilization.value = value
        }

    private val _resolution = MutableStateFlow(SettingsDjiDeviceResolution.r1080p)
    var resolution: SettingsDjiDeviceResolution
        get() = _resolution.value
        set(value) {
            _resolution.value = value
        }

    private val _fps = MutableStateFlow(30)
    var fps: Int
        get() = _fps.value
        set(value) {
            _fps.value = value
        }

    private val bitrate = MutableStateFlow<UInt>(6_000_000u)
    var bitrate: UInt
        get() = bitrate.value
        set(value) {
            bitrate.value = value
        }

    private val _videoCodec = MutableStateFlow(SettingsDjiDeviceVideoCodec.h265hevc)
    var videoCodec: SettingsDjiDeviceVideoCodec
        get() = _videoCodec.value
        set(value) {
            _videoCodec.value = value
        }

    private val _isStarted = MutableStateFlow(false)
    var isStarted: Boolean
        get() = _isStarted.value
        set(value) {
            _isStarted.value = value
        }

    private val _model = MutableStateFlow(SettingsDjiDeviceModel.unknown)
    var model: SettingsDjiDeviceModel
        get() = _model.value
        set(value) {
            _model.value = value
        }

    private val state = MutableStateFlow<DjiDeviceState?>(null)
    var state: DjiDeviceState?
        get() = state.value
        set(value) {
            state.value = value
        }

    val autoRestartStreamTimer = MainTimer()

    init {
        bluetoothPeripheralName = null
        bluetoothPeripheralId = null
    }

    enum class CodingKeys {
        id,
        name,
        bluetoothPeripheralName,
        bluetoothPeripheralId,
        wifiSsid,
        wifiPassword,
        rtmpUrlType,
        serverRtmpStreamId,
        serverRtmpUrl,
        customRtmpUrl,
        autoRestartStream,
        imageStabilization,
        resolution,
        fps,
        bitrate,
        videoCodec,
        isStarted,
        model,
    }
}

@Serializable(with = SettingsDjiDevicesSerializer::class)
class SettingsDjiDevices {
    private val devices = MutableStateFlow<List<SettingsDjiDevice>>(emptyList())
    var devices: List<SettingsDjiDevice>
        get() = devices.value
        set(value) {
            devices.value = value
        }

    init {
    }

    enum class CodingKeys {
        devices,
    }
}

object SettingsDjiDeviceUrlTypeSerializer : KSerializer<SettingsDjiDeviceUrlType> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("SettingsDjiDeviceUrlType", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: SettingsDjiDeviceUrlType) {
        encoder.encodeString(value.rawValue)
    }

    override fun deserialize(decoder: Decoder): SettingsDjiDeviceUrlType {
        return SettingsDjiDeviceUrlType.fromRawValue(decoder.decodeString())
            ?: throw SerializationException("Invalid SettingsDjiDeviceUrlType")
    }
}

object SettingsDjiDeviceResolutionSerializer : KSerializer<SettingsDjiDeviceResolution> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("SettingsDjiDeviceResolution", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: SettingsDjiDeviceResolution) {
        encoder.encodeString(value.rawValue)
    }

    override fun deserialize(decoder: Decoder): SettingsDjiDeviceResolution {
        return SettingsDjiDeviceResolution.fromRawValue(decoder.decodeString())
            ?: throw SerializationException("Invalid SettingsDjiDeviceResolution")
    }
}

object SettingsDjiDeviceVideoCodecSerializer : KSerializer<SettingsDjiDeviceVideoCodec> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("SettingsDjiDeviceVideoCodec", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: SettingsDjiDeviceVideoCodec) {
        encoder.encodeString(value.rawValue)
    }

    override fun deserialize(decoder: Decoder): SettingsDjiDeviceVideoCodec {
        return SettingsDjiDeviceVideoCodec.fromRawValue(decoder.decodeString())
            ?: throw SerializationException("Invalid SettingsDjiDeviceVideoCodec")
    }
}

private object UuidSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("java.util.UUID", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): UUID {
        return UUID.fromString(decoder.decodeString())
    }
}

object SettingsDjiDeviceSerializer : KSerializer<SettingsDjiDevice> {
    @Serializable
    private data class Surrogate(
        @Serializable(with = UuidSerializer::class) val id: UUID = UUID.randomUUID(),
        val name: String = SettingsDjiDevice.baseName,
        val bluetoothPeripheralName: String? = null,
        @Serializable(with = UuidSerializer::class) val bluetoothPeripheralId: UUID? = null,
        val wifiSsid: String = "",
        val wifiPassword: String = "",
        val rtmpUrlType: SettingsDjiDeviceUrlType = SettingsDjiDeviceUrlType.server,
        @Serializable(with = UuidSerializer::class) val serverRtmpStreamId: UUID = UUID.randomUUID(),
        val serverRtmpUrl: String? = null,
        val customRtmpUrl: String = "",
        val autoRestartStream: Boolean = false,
        val imageStabilization: SettingsDjiDeviceImageStabilization =
            SettingsDjiDeviceImageStabilization.off,
        val resolution: SettingsDjiDeviceResolution = SettingsDjiDeviceResolution.r1080p,
        val fps: Int = 30,
        val bitrate: UInt = 6_000_000u,
        val videoCodec: SettingsDjiDeviceVideoCodec = SettingsDjiDeviceVideoCodec.h265hevc,
        val isStarted: Boolean = false,
        val model: SettingsDjiDeviceModel = SettingsDjiDeviceModel.unknown,
    )

    override val descriptor: SerialDescriptor = Surrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: SettingsDjiDevice) {
        encoder.encodeSerializableValue(
            Surrogate.serializer(),
            Surrogate(
                id = value.id,
                name = value.name,
                bluetoothPeripheralName = value.bluetoothPeripheralName,
                bluetoothPeripheralId = value.bluetoothPeripheralId,
                wifiSsid = value.wifiSsid,
                wifiPassword = value.wifiPassword,
                rtmpUrlType = value.rtmpUrlType,
                serverRtmpStreamId = value.serverRtmpStreamId,
                serverRtmpUrl = value.serverRtmpUrl,
                customRtmpUrl = value.customRtmpUrl,
                autoRestartStream = value.autoRestartStream,
                imageStabilization = value.imageStabilization,
                resolution = value.resolution,
                fps = value.fps,
                bitrate = value.bitrate,
                videoCodec = value.videoCodec,
                isStarted = value.isStarted,
                model = value.model,
            ),
        )
    }

    override fun deserialize(decoder: Decoder): SettingsDjiDevice {
        val surrogate = decoder.decodeSerializableValue(Surrogate.serializer())
        val device = SettingsDjiDevice()
        device.id = surrogate.id
        device.name = surrogate.name
        device.bluetoothPeripheralName = surrogate.bluetoothPeripheralName
        device.bluetoothPeripheralId = surrogate.bluetoothPeripheralId
        device.wifiSsid = surrogate.wifiSsid
        device.wifiPassword = surrogate.wifiPassword
        device.rtmpUrlType = surrogate.rtmpUrlType
        device.serverRtmpStreamId = surrogate.serverRtmpStreamId
        device.serverRtmpUrl = surrogate.serverRtmpUrl
        device.customRtmpUrl = surrogate.customRtmpUrl
        device.autoRestartStream = surrogate.autoRestartStream
        device.imageStabilization = surrogate.imageStabilization
        device.resolution = surrogate.resolution
        device.fps = surrogate.fps
        device.bitrate = surrogate.bitrate
        device.videoCodec = surrogate.videoCodec
        device.isStarted = surrogate.isStarted
        device.model = surrogate.model
        return device
    }
}

object SettingsDjiDevicesSerializer : KSerializer<SettingsDjiDevices> {
    @Serializable
    private data class Surrogate(
        val devices: List<SettingsDjiDevice> = emptyList(),
    )

    override val descriptor: SerialDescriptor = Surrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: SettingsDjiDevices) {
        encoder.encodeSerializableValue(
            Surrogate.serializer(),
            Surrogate(devices = value.devices),
        )
    }

    override fun deserialize(decoder: Decoder): SettingsDjiDevices {
        val surrogate = decoder.decodeSerializableValue(Surrogate.serializer())
        val settings = SettingsDjiDevices()
        settings.devices = surrogate.devices
        return settings
    }
}

package com.moblin.android.various.settings

import com.moblin.android.integrations.dji.djidevice.DjiDeviceState
import com.moblin.android.localized
import com.moblin.android.platform.codable.JsonObjectSerializer
import com.moblin.android.platform.codable.decode
import com.moblin.android.platform.codable.decodeIfPresent
import com.moblin.android.platform.codable.encodeContainer
import com.moblin.android.platform.swiftui.Published
import com.moblin.android.various.MainTimer
import com.moblin.android.various.utils.Named
import java.util.UUID
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonObject

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

@Serializable(with = SettingsDjiDevice.Serializer::class)
class SettingsDjiDevice : Named {
    var id: UUID = UUID.randomUUID()

    override var name: String by Published(baseName)
    var bluetoothPeripheralName: String? by Published(null)
    var bluetoothPeripheralId: UUID? by Published(null)
    var wifiSsid: String by Published("")
    var wifiPassword: String by Published("")
    var rtmpUrlType: SettingsDjiDeviceUrlType by Published(SettingsDjiDeviceUrlType.server)
    var serverRtmpStreamId: UUID by Published(UUID.randomUUID())
    var serverRtmpUrl: String? by Published(null)
    var customRtmpUrl: String by Published("")
    var autoRestartStream: Boolean by Published(false)
    var imageStabilization: SettingsDjiDeviceImageStabilization by Published(SettingsDjiDeviceImageStabilization.off)
    var resolution: SettingsDjiDeviceResolution by Published(SettingsDjiDeviceResolution.r1080p)
    var fps: Int by Published(30)
    var bitrate: UInt by Published(6_000_000u)
    var videoCodec: SettingsDjiDeviceVideoCodec by Published(SettingsDjiDeviceVideoCodec.h265hevc)
    var isStarted: Boolean by Published(false)
    var model: SettingsDjiDeviceModel by Published(SettingsDjiDeviceModel.unknown)
    var state: DjiDeviceState? by Published(null)

    val autoRestartStreamTimer = MainTimer()

    init {
        bluetoothPeripheralName = null
        bluetoothPeripheralId = null
    }

    enum class CodingKeys {
        id,
        nameKey,
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

    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("name", name)
        encode("bluetoothPeripheralName", bluetoothPeripheralName)
        encode("bluetoothPeripheralId", bluetoothPeripheralId)
        encode("wifiSsid", wifiSsid)
        encode("wifiPassword", wifiPassword)
        encode("rtmpUrlType", rtmpUrlType)
        encode("serverRtmpStreamId", serverRtmpStreamId)
        encode("serverRtmpUrl", serverRtmpUrl)
        encode("customRtmpUrl", customRtmpUrl)
        encode("autoRestartStream", autoRestartStream)
        encode("imageStabilization", imageStabilization)
        encode("resolution", resolution)
        encode("fps", fps)
        encode("bitrate", bitrate)
        encode("videoCodec", videoCodec)
        encode("isStarted", isStarted)
        encode("model", model)
    }

    companion object {
        val baseName: String = localized("My device")

        fun decode(container: JsonObject): SettingsDjiDevice {
            val device = SettingsDjiDevice()
            device.id = container.decode("id", UUID.randomUUID())
            device.name = container.decode("name", baseName)
            device.bluetoothPeripheralName = container.decodeIfPresent<String>("bluetoothPeripheralName")
            device.bluetoothPeripheralId = container.decodeIfPresent<UUID>("bluetoothPeripheralId")
            device.wifiSsid = container.decode("wifiSsid", "")
            device.wifiPassword = container.decode("wifiPassword", "")
            device.rtmpUrlType = container.decode("rtmpUrlType", SettingsDjiDeviceUrlType.server)
            device.serverRtmpStreamId = container.decode("serverRtmpStreamId", UUID.randomUUID())
            device.serverRtmpUrl = container.decode<String?>("serverRtmpUrl", null)
            device.customRtmpUrl = container.decode("customRtmpUrl", "")
            device.autoRestartStream = container.decode("autoRestartStream", false)
            device.imageStabilization = container.decode(
                "imageStabilization",
                SettingsDjiDeviceImageStabilization.off,
            )
            device.resolution = container.decode("resolution", SettingsDjiDeviceResolution.r1080p)
            device.fps = container.decode("fps", 30)
            device.bitrate = container.decode("bitrate", 6_000_000u)
            device.videoCodec = container.decode("videoCodec", SettingsDjiDeviceVideoCodec.h265hevc)
            device.isStarted = container.decode("isStarted", false)
            device.model = container.decode("model", SettingsDjiDeviceModel.unknown)
            return device
        }
    }

    object Serializer : KSerializer<SettingsDjiDevice> by JsonObjectSerializer(
        "SettingsDjiDevice",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsDjiDevices.Serializer::class)
class SettingsDjiDevices {
    var devices: List<SettingsDjiDevice> by Published(emptyList())

    init {
    }

    enum class CodingKeys {
        devices,
    }

    fun encode(): JsonObject = encodeContainer {
        encode("devices", devices, ListSerializer(SettingsDjiDevice.serializer()))
    }

    companion object {
        fun decode(container: JsonObject): SettingsDjiDevices {
            val devices = SettingsDjiDevices()
            devices.devices = container.decode("devices", ListSerializer(SettingsDjiDevice.serializer()), emptyList())
            return devices
        }
    }

    object Serializer : KSerializer<SettingsDjiDevices> by JsonObjectSerializer(
        "SettingsDjiDevices",
        { it.encode() },
        { decode(it) },
    )
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

package com.moblin.android.various.settings

import com.moblin.android.integrations.gopro.GoProDeviceState
import com.moblin.android.localized
import com.moblin.android.platform.codable.JsonObjectSerializer
import com.moblin.android.platform.codable.decode
import com.moblin.android.platform.codable.decodeIfPresent
import com.moblin.android.platform.codable.encodeContainer
import com.moblin.android.various.MainTimer
import com.moblin.android.various.utils.Named
import java.util.UUID
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonObject

@Serializable(with = SettingsGoProWifiCredentials.Serializer::class)
class SettingsGoProWifiCredentials : Named {
    var id: UUID = UUID.randomUUID()
    override var name: String = baseName
    var ssid: String = ""
    var password: String = ""

    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("name", name)
        encode("ssid", ssid)
        encode("password", password)
    }

    companion object {
        val baseName: String = localized("My SSID")

        fun decode(container: JsonObject): SettingsGoProWifiCredentials {
            val credentials = SettingsGoProWifiCredentials()
            credentials.id = container.decode("id", UUID.randomUUID())
            credentials.name = container.decode("name", baseName)
            credentials.ssid = container.decode("ssid", "")
            credentials.password = container.decode("password", "")
            return credentials
        }
    }

    object Serializer : KSerializer<SettingsGoProWifiCredentials> by JsonObjectSerializer(
        "SettingsGoProWifiCredentials",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsGoProRtmpUrl.Serializer::class)
class SettingsGoProRtmpUrl : Named {
    var id: UUID = UUID.randomUUID()
    override var name: String = baseName
    var type: SettingsDjiDeviceUrlType = SettingsDjiDeviceUrlType.server
    var serverStreamId: UUID = UUID.randomUUID()
    var serverUrl: String = ""
    var customUrl: String = ""

    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("name", name)
        encode("type", type)
        encode("serverStreamId", serverStreamId)
        encode("serverUrl", serverUrl)
        encode("customUrl", customUrl)
    }

    companion object {
        val baseName: String = localized("My URL")

        fun decode(container: JsonObject): SettingsGoProRtmpUrl {
            val rtmpUrl = SettingsGoProRtmpUrl()
            rtmpUrl.id = container.decode("id", UUID.randomUUID())
            rtmpUrl.name = container.decode("name", baseName)
            rtmpUrl.type = container.decode("type", SettingsDjiDeviceUrlType.server)
            rtmpUrl.serverStreamId = container.decode("serverStreamId", UUID.randomUUID())
            rtmpUrl.serverUrl = container.decode("serverUrl", "")
            rtmpUrl.customUrl = container.decode("customUrl", "")
            return rtmpUrl
        }
    }

    object Serializer : KSerializer<SettingsGoProRtmpUrl> by JsonObjectSerializer(
        "SettingsGoProRtmpUrl",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable
enum class SettingsGoProLaunchLiveStreamResolution(val rawValue: String) {
    @SerialName("1080p")
    r1080p("1080p"),

    @SerialName("720p")
    r720p("720p"),

    @SerialName("480p")
    r480p("480p");

    companion object {
        fun fromRawValue(value: String): SettingsGoProLaunchLiveStreamResolution? =
            entries.firstOrNull { it.rawValue == value }
    }
}

@Serializable
enum class SettingsGoProLens(val rawValue: String) {
    @SerialName("Auto")
    auto("Auto"),

    @SerialName("Wide")
    wide("Wide"),

    @SerialName("Linear")
    linear("Linear"),

    @SerialName("SuperView")
    superView("SuperView");

    override fun toString(): String = when (this) {
        auto -> localized("Default")
        wide -> localized("Wide")
        linear -> localized("Linear")
        superView -> localized("SuperView")
    }

    companion object {
        fun fromRawValue(value: String): SettingsGoProLens? =
            entries.firstOrNull { it.rawValue == value }
    }
}

val goProDeviceBitrates: List<Int> = listOf(
    8_000_000,
    6_000_000,
    4_000_000,
    2_000_000,
    1_000_000,
    800_000,
)

@Serializable(with = SettingsGoProDevice.Serializer::class)
class SettingsGoProDevice : Named {
    var id: UUID = UUID.randomUUID()
    override var name: String = baseName
    var bluetoothPeripheralName: String? = null
    var bluetoothPeripheralId: UUID? = null
    var wifiSsid: String = ""
    var wifiPassword: String = ""
    var rtmpUrlType: SettingsDjiDeviceUrlType = SettingsDjiDeviceUrlType.server
    var serverRtmpStreamId: UUID = UUID.randomUUID()
    var serverRtmpUrl: String? = null
    var customRtmpUrl: String = ""
    var resolution: SettingsGoProLaunchLiveStreamResolution = SettingsGoProLaunchLiveStreamResolution.r1080p
    var bitrate: Int = 6_000_000
    var lens: SettingsGoProLens = SettingsGoProLens.auto
    var autoRestartStream: Boolean = false
    var isStarted: Boolean = false
    var state: GoProDeviceState? = null
    val autoRestartStreamTimer = MainTimer()

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
        encode("resolution", resolution)
        encode("bitrate", bitrate.toUInt())
        encode("lens", lens)
        encode("autoRestartStream", autoRestartStream)
        encode("isStarted", isStarted)
    }

    companion object {
        val baseName: String = localized("My GoPro")

        fun decode(container: JsonObject): SettingsGoProDevice {
            val device = SettingsGoProDevice()
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
            device.resolution = container.decode("resolution", SettingsGoProLaunchLiveStreamResolution.r1080p)
            device.bitrate = container.decode("bitrate", 6_000_000u).toInt()
            device.lens = container.decode("lens", SettingsGoProLens.auto)
            device.autoRestartStream = container.decode("autoRestartStream", false)
            device.isStarted = container.decode("isStarted", false)
            return device
        }
    }

    object Serializer : KSerializer<SettingsGoProDevice> by JsonObjectSerializer(
        "SettingsGoProDevice",
        { it.encode() },
        { decode(it) },
    )

    fun canStartLive(isConnectedToIpv4WiFi: Boolean): Boolean {
        if (bluetoothPeripheralId == null || wifiSsid.isEmpty()) {
            return false
        }
        return when (rtmpUrlType) {
            SettingsDjiDeviceUrlType.server -> {
                val serverRtmpUrl = serverRtmpUrl
                if (serverRtmpUrl != null) {
                    serverRtmpUrl.isNotEmpty()
                } else {
                    isConnectedToIpv4WiFi
                }
            }

            SettingsDjiDeviceUrlType.custom -> customRtmpUrl.isNotEmpty()
        }
    }
}

@Serializable(with = SettingsGoProLaunchLiveStream.Serializer::class)
class SettingsGoProLaunchLiveStream : Named {
    var id: UUID = UUID.randomUUID()
    override var name: String = baseName
    var isHero12Or13: Boolean = true
    var resolution: SettingsGoProLaunchLiveStreamResolution = SettingsGoProLaunchLiveStreamResolution.r1080p

    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("name", name)
        encode("isHero12Or13", isHero12Or13)
        encode("resolution", resolution)
    }

    companion object {
        val baseName: String = localized("My live")

        fun decode(container: JsonObject): SettingsGoProLaunchLiveStream {
            val launchLiveStream = SettingsGoProLaunchLiveStream()
            launchLiveStream.id = container.decode("id", UUID.randomUUID())
            launchLiveStream.name = container.decode("name", baseName)
            launchLiveStream.isHero12Or13 = container.decode("isHero12Or13", true)
            launchLiveStream.resolution = container.decode(
                "resolution",
                SettingsGoProLaunchLiveStreamResolution.r1080p,
            )
            return launchLiveStream
        }
    }

    object Serializer : KSerializer<SettingsGoProLaunchLiveStream> by JsonObjectSerializer(
        "SettingsGoProLaunchLiveStream",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsGoPro.Serializer::class)
class SettingsGoPro {
    var devices: MutableList<SettingsGoProDevice> = mutableListOf()
    var launchLiveStream: MutableList<SettingsGoProLaunchLiveStream> = mutableListOf()
    var selectedLaunchLiveStream: UUID? = null
    var wifiCredentials: MutableList<SettingsGoProWifiCredentials> = mutableListOf()
    var selectedWifiCredentials: UUID? = null
    var rtmpUrls: MutableList<SettingsGoProRtmpUrl> = mutableListOf()
    var selectedRtmpUrl: UUID? = null

    fun encode(): JsonObject = encodeContainer {
        encode("devices", devices, ListSerializer(SettingsGoProDevice.serializer()))
        encode("launchLiveStream", launchLiveStream, ListSerializer(SettingsGoProLaunchLiveStream.serializer()))
        encode("selectedLaunchLiveStream", selectedLaunchLiveStream)
        encode("wifiCredentials", wifiCredentials, ListSerializer(SettingsGoProWifiCredentials.serializer()))
        encode("selectedWifiCredentials", selectedWifiCredentials)
        encode("rtmpUrls", rtmpUrls, ListSerializer(SettingsGoProRtmpUrl.serializer()))
        encode("selectedRtmpUrl", selectedRtmpUrl)
    }

    companion object {
        fun decode(container: JsonObject): SettingsGoPro {
            val goPro = SettingsGoPro()
            goPro.devices = container.decode(
                "devices",
                ListSerializer(SettingsGoProDevice.serializer()),
                emptyList(),
            ).toMutableList()
            goPro.launchLiveStream = container.decode(
                "launchLiveStream",
                ListSerializer(SettingsGoProLaunchLiveStream.serializer()),
                emptyList(),
            ).toMutableList()
            goPro.selectedLaunchLiveStream = container.decodeIfPresent<UUID>("selectedLaunchLiveStream")
            goPro.wifiCredentials = container.decode(
                "wifiCredentials",
                ListSerializer(SettingsGoProWifiCredentials.serializer()),
                emptyList(),
            ).toMutableList()
            goPro.selectedWifiCredentials = container.decodeIfPresent<UUID>("selectedWifiCredentials")
            goPro.rtmpUrls = container.decode(
                "rtmpUrls",
                ListSerializer(SettingsGoProRtmpUrl.serializer()),
                emptyList(),
            ).toMutableList()
            goPro.selectedRtmpUrl = container.decodeIfPresent<UUID>("selectedRtmpUrl")
            return goPro
        }
    }

    object Serializer : KSerializer<SettingsGoPro> by JsonObjectSerializer(
        "SettingsGoPro",
        { it.encode() },
        { decode(it) },
    )
}

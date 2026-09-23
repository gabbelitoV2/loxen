package com.moblin.android.various.settings

import com.moblin.android.integrations.gopro.GoProDeviceState
import com.moblin.android.localized
import com.moblin.android.platform.codable.JsonObjectSerializer
import com.moblin.android.platform.codable.decode
import com.moblin.android.platform.codable.decodeIfPresent
import com.moblin.android.platform.codable.encodeContainer
import com.moblin.android.platform.swiftui.Published
import com.moblin.android.platform.swiftui.PublishedList
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
    override var name: String by Published(baseName)
    var ssid: String by Published("")
    var password: String by Published("")

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
    override var name: String by Published(baseName)
    var type: SettingsDjiDeviceUrlType by Published(SettingsDjiDeviceUrlType.server)
    var serverStreamId: UUID by Published(UUID.randomUUID())
    var serverUrl: String by Published("")
    var customUrl: String by Published("")

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
    override var name: String by Published(baseName)
    var bluetoothPeripheralName: String? by Published(null)
    var bluetoothPeripheralId: UUID? by Published(null)
    var wifiSsid: String by Published("")
    var wifiPassword: String by Published("")
    var rtmpUrlType: SettingsDjiDeviceUrlType by Published(SettingsDjiDeviceUrlType.server)
    var serverRtmpStreamId: UUID by Published(UUID.randomUUID())
    var serverRtmpUrl: String? by Published(null)
    var customRtmpUrl: String by Published("")
    var resolution: SettingsGoProLaunchLiveStreamResolution by Published(SettingsGoProLaunchLiveStreamResolution.r1080p)
    var bitrate: Int by Published(6_000_000)
    var lens: SettingsGoProLens by Published(SettingsGoProLens.auto)
    var autoRestartStream: Boolean by Published(false)
    var isStarted: Boolean by Published(false)
    var state: GoProDeviceState? by Published(null)
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
    override var name: String by Published(baseName)
    var isHero12Or13: Boolean by Published(true)
    var resolution: SettingsGoProLaunchLiveStreamResolution by Published(SettingsGoProLaunchLiveStreamResolution.r1080p)

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
    var devices: MutableList<SettingsGoProDevice> by PublishedList()
    var launchLiveStream: MutableList<SettingsGoProLaunchLiveStream> by PublishedList()
    var selectedLaunchLiveStream: UUID? by Published(null)
    var wifiCredentials: MutableList<SettingsGoProWifiCredentials> by PublishedList()
    var selectedWifiCredentials: UUID? by Published(null)
    var rtmpUrls: MutableList<SettingsGoProRtmpUrl> by PublishedList()
    var selectedRtmpUrl: UUID? by Published(null)

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

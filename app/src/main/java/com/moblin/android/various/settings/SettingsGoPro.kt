package com.moblin.android.various.settings

import com.moblin.android.integrations.gopro.GoProDeviceState
import com.moblin.android.localized
import com.moblin.android.various.MainTimer
import com.moblin.android.various.utils.Named
import java.util.UUID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class SettingsGoProWifiCredentials : Named {
    @Serializable(with = SettingsDjiDeviceUuidSerializer::class)
    var id: UUID = UUID.randomUUID()
    override var name: String = baseName
    var ssid: String = ""
    var password: String = ""

    companion object {
        val baseName: String = localized("My SSID")
    }
}

@Serializable
class SettingsGoProRtmpUrl : Named {
    @Serializable(with = SettingsDjiDeviceUuidSerializer::class)
    var id: UUID = UUID.randomUUID()
    override var name: String = baseName
    var type: SettingsDjiDeviceUrlType = SettingsDjiDeviceUrlType.server
    @Serializable(with = SettingsDjiDeviceUuidSerializer::class)
    var serverStreamId: UUID = UUID.randomUUID()
    var serverUrl: String = ""
    var customUrl: String = ""

    companion object {
        val baseName: String = localized("My URL")
    }
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

@Serializable
class SettingsGoProDevice : Named {
    @Serializable(with = SettingsDjiDeviceUuidSerializer::class)
    var id: UUID = UUID.randomUUID()
    override var name: String = baseName
    var bluetoothPeripheralName: String? = null
    @Serializable(with = SettingsDjiDeviceUuidSerializer::class)
    var bluetoothPeripheralId: UUID? = null
    var wifiSsid: String = ""
    var wifiPassword: String = ""
    var rtmpUrlType: SettingsDjiDeviceUrlType = SettingsDjiDeviceUrlType.server
    @Serializable(with = SettingsDjiDeviceUuidSerializer::class)
    var serverRtmpStreamId: UUID = UUID.randomUUID()
    var serverRtmpUrl: String? = null
    var customRtmpUrl: String = ""
    var resolution: SettingsGoProLaunchLiveStreamResolution = SettingsGoProLaunchLiveStreamResolution.r1080p
    var bitrate: Int = 6_000_000
    var lens: SettingsGoProLens = SettingsGoProLens.auto
    var autoRestartStream: Boolean = false
    var isStarted: Boolean = false

    @Transient
    var state: GoProDeviceState? = null

    @Transient
    val autoRestartStreamTimer = MainTimer()

    companion object {
        val baseName: String = localized("My GoPro")
    }

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

@Serializable
class SettingsGoProLaunchLiveStream : Named {
    @Serializable(with = SettingsDjiDeviceUuidSerializer::class)
    var id: UUID = UUID.randomUUID()
    override var name: String = baseName
    var isHero12Or13: Boolean = true
    var resolution: SettingsGoProLaunchLiveStreamResolution = SettingsGoProLaunchLiveStreamResolution.r1080p

    companion object {
        val baseName: String = localized("My live")
    }
}

@Serializable
class SettingsGoPro {
    var devices: MutableList<SettingsGoProDevice> = mutableListOf()
    var launchLiveStream: MutableList<SettingsGoProLaunchLiveStream> = mutableListOf()
    @Serializable(with = SettingsDjiDeviceUuidSerializer::class)
    var selectedLaunchLiveStream: UUID? = null
    var wifiCredentials: MutableList<SettingsGoProWifiCredentials> = mutableListOf()
    @Serializable(with = SettingsDjiDeviceUuidSerializer::class)
    var selectedWifiCredentials: UUID? = null
    var rtmpUrls: MutableList<SettingsGoProRtmpUrl> = mutableListOf()
    @Serializable(with = SettingsDjiDeviceUuidSerializer::class)
    var selectedRtmpUrl: UUID? = null
}

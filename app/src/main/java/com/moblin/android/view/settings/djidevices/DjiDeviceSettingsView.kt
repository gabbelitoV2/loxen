package com.moblin.android.view.settings.djidevices

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.common.various.formatBytesPerSecond
import com.moblin.android.common.various.personalHotspotLocalAddress
import com.moblin.android.common.various.urlImage
import com.moblin.android.integrations.dji.djidevice.DjiDeviceScanner
import com.moblin.android.integrations.dji.djidevice.DjiDeviceState
import com.moblin.android.integrations.dji.djidevice.canStartLive
import com.moblin.android.localized
import com.moblin.android.media.rtmpserver.rtmpServerApp
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.ContextMenu
import com.moblin.android.platform.swiftui.ForEach
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormButton
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.LocalNavigator
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.platform.swiftui.removing
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.StatusOther
import com.moblin.android.various.model.StatusTopRight
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsDjiDevice
import com.moblin.android.various.settings.SettingsDjiDeviceImageStabilization
import com.moblin.android.various.settings.SettingsDjiDeviceModel
import com.moblin.android.various.settings.SettingsDjiDeviceResolution
import com.moblin.android.various.settings.SettingsDjiDeviceUrlType
import com.moblin.android.various.settings.SettingsDjiDeviceVideoCodec
import com.moblin.android.various.settings.SettingsDjiDevices
import com.moblin.android.various.settings.SettingsRtmpServer
import com.moblin.android.various.settings.SettingsWiFi
import com.moblin.android.various.settings.djiDeviceBitrates
import com.moblin.android.various.settings.djiDeviceFpss
import com.moblin.android.various.utils.isMac
import com.moblin.android.view.settings.ingests.rtmpserver.RtmpServerSettingsView
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.ContextMenuDeleteButtonView
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.NameEditView
import com.moblin.android.view.utils.ShortcutSectionView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.view.utils.TextEditView
import com.moblin.android.view.utils.WiFiSsidEditView
import com.moblin.android.view.utils.TextItemLocalizedView
import java.util.UUID
import com.moblin.android.various.model.setCurrentDjiDevice
import com.moblin.android.various.model.startDjiDeviceLiveStream
import com.moblin.android.various.model.stopDjiDeviceLiveStream

fun rtmpServerStreamUrl(address: String, port: Int, streamKey: String): String {
    return "rtmp://$address:$port$rtmpServerApp/$streamKey"
}

fun formatDjiDeviceState(state: DjiDeviceState?): String {
    return when (state) {
        null, DjiDeviceState.idle -> localized("Not started")
        DjiDeviceState.discovering -> localized("Discovering")
        DjiDeviceState.connecting -> localized("Connecting")
        DjiDeviceState.checkingIfPaired, DjiDeviceState.pairing -> localized("Pairing")
        DjiDeviceState.stoppingStream, DjiDeviceState.cleaningUp -> localized("Stopping stream")
        DjiDeviceState.preparingStream -> localized("Preparing to stream")
        DjiDeviceState.settingUpWifi -> localized("Setting up WiFi")
        DjiDeviceState.wifiSetupFailed -> localized("WiFi setup failed")
        DjiDeviceState.configuring -> localized("Configuring")
        DjiDeviceState.startingStream -> localized("Starting stream")
        DjiDeviceState.streaming -> localized("Streaming")
        else -> localized("Unknown")
    }
}

@Composable
private fun ColumnScope.DjiDeviceSelectDeviceSettingsView(
    device: SettingsDjiDevice,
    onNavigate: (String) -> Unit,
) {
    val djiScanner = DjiDeviceScanner.shared
    val isStarted = device.isStarted
    val bluetoothPeripheralName = device.bluetoothPeripheralName

    fun onDeviceChange(value: String) {
        val deviceId = runCatching { UUID.fromString(value) }.getOrNull() ?: return
        val djiDevice = djiScanner.discoveredDevices.value.firstOrNull {
            it.peripheral.address == value
        } ?: return
        device.bluetoothPeripheralName = djiDevice.peripheral.name
        device.bluetoothPeripheralId = deviceId
        device.model = djiDevice.model
    }

    Section(header = "Device") {
        NavigationLink(
            destination = {
                val navigator = LocalNavigator.current
                DjiDeviceScannerSettingsView(
                    onChange = { onDeviceChange(it) },
                    selectedId = device.bluetoothPeripheralId?.toString() ?: localized("Select device"),
                    onDismiss = { navigator?.pop() },
                )
            },
            enabled = !isStarted,
        ) {
            GrayTextView(text = bluetoothPeripheralName ?: localized("Select device"))
        }
    }
}

@Composable
private fun ColumnScope.DjiDeviceWiFiSettingsView(
    model: Model,
    device: SettingsDjiDevice,
    onNavigate: (String) -> Unit,
) {
    val isStarted = device.isStarted
    val wifiSsid = device.wifiSsid

    Section(
        header = "WiFi",
        footer = "The DJI device will connect to and stream RTMP over this WiFi.",
    ) {
        NavigationLink(
            destination = {
                DjiDeviceWiFiSettingsInnerView(database = model.database, device = device)
            },
            enabled = !isStarted,
        ) {
            TextItemLocalizedView(name = "Network", value = wifiSsid)
        }
        if (wifiSsid.isEmpty()) {
            Text(localized("⚠️ Enter the SSID of the network the DJI device should connect to."))
        }
    }
}

@Composable
private fun DjiDeviceWiFiSettingsInnerView(
    database: Database,
    device: SettingsDjiDevice,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val wifiSsid = device.wifiSsid
    val wifiPassword = device.wifiPassword
    val savedWifiNetworks = database.savedWifiNetworks

    fun updateSavedNetworks() {
        if (device.wifiSsid.isEmpty()) {
            return
        }
        val network = database.getSavedWiFiNetwork(device.wifiSsid)
        if (network != null) {
            network.password = device.wifiPassword
        } else {
            val newNetwork = SettingsWiFi()
            newNetwork.ssid = device.wifiSsid
            newNetwork.password = device.wifiPassword
            database.savedWifiNetworks =
                (database.savedWifiNetworks + newNetwork).toMutableList()
        }
    }

    Form(title = "WiFi") {
        Section(header = "Network") {
            NavigationLink(
                destination = {
                    val navigator = LocalNavigator.current
                    var value by remember { mutableStateOf(device.wifiSsid) }
                    WiFiSsidEditView(
                        value = value,
                        onValueChange = { value = it },
                        onSubmit = {
                            device.wifiSsid = it
                            if (device.wifiPassword.isEmpty()) {
                                val network = database.getSavedWiFiNetwork(device.wifiSsid)
                                if (network != null) {
                                    device.wifiPassword = network.password
                                }
                            }
                            updateSavedNetworks()
                        },
                        onDismiss = { navigator?.pop() },
                    )
                },
            ) {
                TextItemLocalizedView(name = "SSID", value = wifiSsid)
            }
            NavigationLink(
                destination = {
                    val navigator = LocalNavigator.current
                    TextEditView(
                        title = localized("Password"),
                        value = device.wifiPassword,
                        onSubmit = {
                            device.wifiPassword = it
                            updateSavedNetworks()
                        },
                        onDismiss = { navigator?.pop() },
                    )
                },
            ) {
                TextItemLocalizedView(name = "Password", value = wifiPassword, sensitive = true)
            }
        }
        if (savedWifiNetworks.isNotEmpty()) {
            Section(
                header = "Saved networks",
                footerContent = { SwipeLeftToDeleteHelpView(kind = localized("a network")) },
            ) {
                ForEach(
                    database.savedWifiNetworks,
                    id = { it.id },
                    onDelete = { offsets ->
                        database.savedWifiNetworks = database.savedWifiNetworks.removing(atOffsets = offsets)
                    },
                ) { network ->
                    ContextMenu(
                        menu = {
                            if (isMac()) {
                                ContextMenuDeleteButtonView {
                                    database.savedWifiNetworks = database.savedWifiNetworks
                                        .filterNot { it.ssid == network.ssid }
                                        .toMutableList()
                                }
                            }
                        },
                    ) {
                        FormRow(
                            onClick = {
                                device.wifiSsid = network.ssid
                                device.wifiPassword = network.password
                            },
                        ) {
                            Text(network.ssid)
                            Spacer(Modifier.weight(1f))
                            if (device.wifiSsid == network.ssid &&
                                device.wifiPassword == network.password
                            ) {
                                SystemImage(
                                    name = "checkmark",
                                    fontSize = 17.sp,
                                    tint = formPalette().accent,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class RtmpUrlAndImage(val url: String, val image: String)

@Composable
private fun ColumnScope.DjiDeviceRtmpSettingsView(
    model: Model,
    device: SettingsDjiDevice,
    status: StatusOther,
    rtmpServer: SettingsRtmpServer,
    onNavigate: (String) -> Unit,
) {
    val isStarted = device.isStarted
    val rtmpUrlType = device.rtmpUrlType
    val serverRtmpStreamId = device.serverRtmpStreamId
    val serverRtmpUrl = device.serverRtmpUrl
    val customRtmpUrl = device.customRtmpUrl
    val rtmpServerStreams = rtmpServer.streams
    val rtmpServerEnabled = rtmpServer.enabled
    val ipStatuses by status.ipStatuses.collectAsState()

    fun serverUrls(): List<RtmpUrlAndImage> {
        val streamKey = rtmpServerStreams.firstOrNull { it.id == serverRtmpStreamId }?.streamKey
            ?: return emptyList()
        val serverUrls = mutableListOf<RtmpUrlAndImage>()
        for (ipStatus in ipStatuses.filter { it.ipType.toString() == "ipv4" }) {
            serverUrls.add(
                RtmpUrlAndImage(
                    url = rtmpServerStreamUrl(
                        ipStatus.ipType.formatAddress(ipStatus.ip),
                        rtmpServer.port,
                        streamKey,
                    ),
                    image = urlImage(ipStatus.interfaceType.ordinal),
                ),
            )
        }
        serverUrls.add(
            RtmpUrlAndImage(
                url = rtmpServerStreamUrl(
                    personalHotspotLocalAddress,
                    rtmpServer.port,
                    streamKey,
                ),
                image = "personalhotspot",
            ),
        )
        for (ipStatus in ipStatuses.filter { it.ipType.toString() == "ipv6" }) {
            serverUrls.add(
                RtmpUrlAndImage(
                    url = rtmpServerStreamUrl(
                        ipStatus.ipType.formatAddress(ipStatus.ip),
                        rtmpServer.port,
                        streamKey,
                    ),
                    image = urlImage(ipStatus.interfaceType.ordinal),
                ),
            )
        }
        if (serverRtmpUrl != null && serverUrls.none { it.url == serverRtmpUrl }) {
            serverUrls.add(0, RtmpUrlAndImage(url = serverRtmpUrl, image = "questionmark"))
        }
        return serverUrls
    }

    fun automaticServerRtmpUrl(): String? {
        val streamKey = rtmpServerStreams.firstOrNull { it.id == serverRtmpStreamId }?.streamKey
            ?: return null
        val ipStatus = ipStatuses.firstOrNull { it.ipType.toString() == "ipv4" } ?: return null
        return rtmpServerStreamUrl(
            ipStatus.ipType.formatAddress(ipStatus.ip),
            rtmpServer.port,
            streamKey,
        )
    }

    LaunchedEffect(Unit) {
        val streams = rtmpServer.streams
        if (streams.isNotEmpty() && streams.none { it.id == device.serverRtmpStreamId }) {
            device.serverRtmpStreamId = streams.first().id
        }
    }

    Section(
        header = "RTMP",
        footer = "Select ${localized("Server")} if you want the DJI camera to stream to " +
            "Moblin's RTMP server on this device. Select ${localized("Custom")} to make the " +
            "DJI camera stream to any destination.",
    ) {
        Picker(
            title = "Type",
            selection = rtmpUrlType,
            options = SettingsDjiDeviceUrlType.entries,
            enabled = !isStarted,
            text = { it.toString() },
            onChange = { device.rtmpUrlType = it },
        )
        if (rtmpUrlType == SettingsDjiDeviceUrlType.server) {
            if (rtmpServerStreams.isEmpty()) {
                Text(localized("No RTMP server streams exists"))
            } else {
                Picker(
                    title = "Stream",
                    selection = serverRtmpStreamId,
                    options = (listOf(serverRtmpStreamId) + rtmpServerStreams.map { it.id })
                        .distinct(),
                    enabled = !isStarted,
                    text = { id -> rtmpServerStreams.firstOrNull { it.id == id }?.name ?: "" },
                    onChange = { id ->
                        device.serverRtmpStreamId = id
                        device.serverRtmpUrl = null
                    },
                )
                Picker(
                    title = "URL",
                    selection = serverRtmpUrl,
                    options = listOf<String?>(null) + serverUrls().map { it.url },
                    enabled = !isStarted,
                    text = { it ?: (automaticServerRtmpUrl() ?: "") },
                    onChange = { device.serverRtmpUrl = it },
                )
                if (serverRtmpUrl == null && !status.isConnectedToIpv4WiFi()) {
                    Text(localized("⚠️ Not connected to an IPv4 WiFi network."))
                }
                if (!rtmpServerEnabled) {
                    Text(localized("⚠️ The RTMP server is not enabled"))
                }
            }
        } else if (rtmpUrlType == SettingsDjiDeviceUrlType.custom) {
            TextEditNavigationView(
                title = localized("URL"),
                value = customRtmpUrl,
                onSubmit = { device.customRtmpUrl = it },
                onNavigate = onNavigate,
            )
            if (customRtmpUrl.isEmpty()) {
                Text(localized("⚠️ Enter the URL the DJI device should stream to."))
            }
        }
    }
    ShortcutSectionView {
        RtmpServerSettingsView(rtmpServer = rtmpServer)
    }
}

@Composable
private fun ColumnScope.DjiDeviceSettingsSettingsView(device: SettingsDjiDevice) {
    val isStarted = device.isStarted
    val resolution = device.resolution
    val bitrate = device.bitrate
    val imageStabilization = device.imageStabilization
    val fps = device.fps
    val videoCodec = device.videoCodec
    val model = device.model

    Section(header = "Settings", footer = "High bitrates may be unstable.") {
        Picker(
            title = "Resolution",
            selection = resolution,
            options = SettingsDjiDeviceResolution.entries,
            enabled = !isStarted,
            text = { it.rawValue },
            onChange = { device.resolution = it },
        )
        Picker(
            title = "Bitrate",
            selection = bitrate,
            options = djiDeviceBitrates,
            enabled = !isStarted,
            text = { formatBytesPerSecond(it.toLong()) },
            onChange = { device.bitrate = it },
        )
        if (model.hasImageStabilization()) {
            Picker(
                title = "Image stabilization",
                selection = imageStabilization,
                options = SettingsDjiDeviceImageStabilization.entries,
                enabled = !isStarted,
                text = { it.toString() },
                onChange = { device.imageStabilization = it },
            )
        }
        if (model == SettingsDjiDeviceModel.osmoPocket3 ||
            model == SettingsDjiDeviceModel.osmoPocket4
        ) {
            Picker(
                title = "FPS",
                selection = fps,
                options = djiDeviceFpss,
                enabled = !isStarted,
                text = { it.toString() },
                onChange = { device.fps = it },
            )
        }
        if (model.hasVideoCodec()) {
            Picker(
                title = "Video codec",
                selection = videoCodec,
                options = SettingsDjiDeviceVideoCodec.entries,
                enabled = !isStarted,
                text = { it.rawValue },
                onChange = { device.videoCodec = it },
            )
        }
    }
}

@Composable
private fun ColumnScope.DjiDeviceAutoRestartSettingsView(device: SettingsDjiDevice) {
    if (device.rtmpUrlType == SettingsDjiDeviceUrlType.server) {
        Section {
            Toggle(
                title = "Auto-restart live stream when broken",
                isOn = device.autoRestartStream,
                onChange = { device.autoRestartStream = it },
            )
        }
    }
}

@Composable
private fun ColumnScope.DjiDeviceStartStopButtonSettingsView(
    model: Model,
    status: StatusOther,
    device: SettingsDjiDevice,
) {
    if (!device.isStarted) {
        Section {
            FormButton(
                title = "Start live stream",
                centered = true,
                enabled = device.canStartLive(status.isConnectedToIpv4WiFi()),
            ) {
                model.startDjiDeviceLiveStream(device)
            }
        }
    } else {
        Section {
            FormButton(title = "Stop live stream", centered = true) {
                model.stopDjiDeviceLiveStream(device)
            }
        }
    }
}

@Composable
fun DjiDeviceSettingsView(
    model: Model = LocalModel.current,
    djiDevices: SettingsDjiDevices,
    device: SettingsDjiDevice,
    status: StatusTopRight,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val name = device.name
    val existingNames = djiDevices.devices
    val djiDeviceStreamingState by status.djiDeviceStreamingState.collectAsState()

    fun state(): String {
        return formatDjiDeviceState(djiDeviceStreamingState)
    }

    LaunchedEffect(Unit) {
        model.setCurrentDjiDevice(device)
    }

    Form(title = "DJI device") {
        Section {
            NameEditView(
                name = name,
                onNameChange = { device.name = it },
                existingNames = existingNames,
            )
        }
        DjiDeviceSelectDeviceSettingsView(device = device, onNavigate = onNavigate)
        DjiDeviceWiFiSettingsView(
            model = model,
            device = device,
            onNavigate = onNavigate,
        )
        DjiDeviceRtmpSettingsView(
            model = model,
            device = device,
            status = model.statusOther,
            rtmpServer = model.database.rtmpServer,
            onNavigate = onNavigate,
        )
        DjiDeviceSettingsSettingsView(device = device)
        DjiDeviceAutoRestartSettingsView(device = device)
        Section {
            HCenter {
                Text(state())
            }
        }
        DjiDeviceStartStopButtonSettingsView(
            model = model,
            status = model.statusOther,
            device = device,
        )
    }
}

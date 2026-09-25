package com.moblin.android.view.settings.gopro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.formatBytesPerSecond
import com.moblin.android.common.various.personalHotspotLocalAddress
import com.moblin.android.common.various.urlImage
import com.moblin.android.integrations.gopro.GoProDeviceScanner
import com.moblin.android.integrations.gopro.GoProDeviceState
import com.moblin.android.integrations.gopro.GoProDiscoveredDevice
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.ForEach
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormButton
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.move
import com.moblin.android.platform.swiftui.rememberDismiss
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.StatusOther
import com.moblin.android.various.model.bluetoothNotAllowedMessage
import com.moblin.android.various.model.removeGoProDevices
import com.moblin.android.various.settings.SettingsDjiDeviceUrlType
import com.moblin.android.various.settings.SettingsGoPro
import com.moblin.android.various.settings.SettingsGoProDevice
import com.moblin.android.various.settings.SettingsGoProLaunchLiveStreamResolution
import com.moblin.android.various.settings.SettingsGoProLens
import com.moblin.android.various.settings.SettingsRtmpServer
import com.moblin.android.various.settings.SettingsWiFi
import com.moblin.android.various.settings.goProDeviceBitrates
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.djidevices.rtmpServerStreamUrl
import com.moblin.android.view.settings.ingests.rtmpserver.RtmpServerSettingsView
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.ContextMenuDeleteButton
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.DraggableItemPrefixView
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.NameEditView
import com.moblin.android.view.utils.ShortcutSectionView
import com.moblin.android.view.utils.TextButtonView
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.view.utils.TextItemLocalizedView
import com.moblin.android.view.utils.WiFiSsidEditView
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import java.util.UUID
import com.moblin.android.various.model.startGoProDeviceLiveStream
import com.moblin.android.various.model.stopGoProDeviceLiveStream

fun formatGoProDeviceState(state: GoProDeviceState?): String {
    return when (state) {
        null, GoProDeviceState.idle -> localized("Not started")
        GoProDeviceState.discovering -> localized("Discovering")
        GoProDeviceState.connecting -> localized("Connecting")
        GoProDeviceState.pairing -> localized("Pairing")
        GoProDeviceState.settingUpWifi -> localized("Setting up WiFi")
        GoProDeviceState.wifiSetupFailed -> localized("WiFi setup failed")
        GoProDeviceState.configuring -> localized("Configuring")
        GoProDeviceState.startingStream -> localized("Starting stream")
        GoProDeviceState.streaming -> localized("Streaming")
        GoProDeviceState.stoppingStream -> localized("Stopping stream")
        GoProDeviceState.failed -> localized("Failed")
    }
}

@Composable
private fun GoProDeviceScannerSettingsView(
    model: Model = LocalModel.current,
    onSelect: (GoProDiscoveredDevice) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scanner = GoProDeviceScanner.shared
    val bluetoothAllowed by model.bluetoothAllowed.collectAsState()
    val discoveredDevices by scanner.discoveredDevices.collectAsState()
    Form(title = localized("GoPro device")) {
        Section(
            footer = localized(
                "Put the GoPro in pairing mode, keep it nearby, and make sure GoPro Quik is disconnected.",
            ),
        ) {
            if (!bluetoothAllowed) {
                Text(bluetoothNotAllowedMessage)
            } else if (discoveredDevices.isEmpty()) {
                HCenter {
                    CircularProgressIndicator()
                }
            } else {
                for (discoveredDevice in discoveredDevices) {
                    FormButton(
                        title = discoveredDevice.name,
                        action = {
                            onSelect(discoveredDevice)
                            onDismiss()
                        },
                    )
                }
            }
        }
    }
    DisposableEffect(Unit) {
        scanner.startScanningForDevices(context)
        onDispose {
            scanner.stopScanningForDevices()
        }
    }
}

@Composable
private fun GoProDeviceSelectionSection(model: Model = LocalModel.current, device: SettingsGoProDevice) {
    Section(header = localized("Device")) {
        NavigationLink(
            destination = {
                GoProDeviceScannerSettingsView(
                    model = model,
                    onSelect = { discoveredDevice ->
                        device.bluetoothPeripheralId =
                            UUID.nameUUIDFromBytes(discoveredDevice.peripheral.address.toByteArray())
                        device.bluetoothPeripheralName = discoveredDevice.name
                    },
                    onDismiss = rememberDismiss(),
                )
            },
            enabled = !device.isStarted,
        ) {
            TextItemLocalizedView(
                name = localized("Device"),
                value = device.bluetoothPeripheralName ?: localized("Select device"),
            )
        }
        if (device.bluetoothPeripheralId == null) {
            Text(localized("⚠️ Select a GoPro device. The first connection requires pairing mode."))
        }
    }
}

@Composable
private fun GoProDeviceWifiSection(model: Model = LocalModel.current, device: SettingsGoProDevice) {
    Section(
        header = localized("WiFi"),
        footer = localized("Moblin sends these credentials securely to the paired GoPro over Bluetooth."),
    ) {
        NavigationLink(
            destination = {
                val editedSsid = remember { androidx.compose.runtime.mutableStateOf(device.wifiSsid) }
                val update = { ssid: String ->
                    device.wifiSsid = ssid
                    if (device.wifiPassword.isEmpty()) {
                        device.wifiPassword =
                            model.database.getSavedWiFiNetwork(ssid = ssid)?.password ?: ""
                    }
                }
                WiFiSsidEditView(
                    value = editedSsid.value,
                    onValueChange = { editedSsid.value = it },
                    onSubmit = update,
                    onDismiss = rememberDismiss(),
                )
            },
            enabled = !device.isStarted,
        ) {
            TextItemLocalizedView(name = localized("SSID"), value = device.wifiSsid)
        }
        TextEditNavigationView(
            title = localized("Password"),
            value = device.wifiPassword,
            onSubmit = { value ->
                device.wifiPassword = value
                if (device.wifiSsid.isNotEmpty()) {
                    val network = model.database.getSavedWiFiNetwork(ssid = device.wifiSsid)
                    if (network != null) {
                        network.password = value
                    } else {
                        val newNetwork = SettingsWiFi()
                        newNetwork.ssid = device.wifiSsid
                        newNetwork.password = value
                        model.database.savedWifiNetworks =
                            model.database.savedWifiNetworks + newNetwork
                    }
                }
            },
            sensitive = true,
        )
        if (device.wifiSsid.isEmpty()) {
            Text(localized("⚠️ Enter the WiFi network the GoPro should use for streaming."))
        }
    }
}

private data class GoProRtmpUrlAndImage(val url: String, val image: String)

private fun serverUrls(
    model: Model,
    device: SettingsGoProDevice,
    status: StatusOther,
    rtmpServer: SettingsRtmpServer,
): List<GoProRtmpUrlAndImage> {
    val stream = rtmpServer.streams.firstOrNull { it.id == device.serverRtmpStreamId }
        ?: return emptyList()
    val urls = status.ipStatuses.value.map { ipStatus ->
        GoProRtmpUrlAndImage(
            url = rtmpServerStreamUrl(
                address = ipStatus.ipType.formatAddress(ipStatus.ip),
                port = rtmpServer.port,
                streamKey = stream.streamKey,
            ),
            image = urlImage(ipStatus.interfaceType.ordinal),
        )
    }.toMutableList()
    urls.add(
        GoProRtmpUrlAndImage(
            url = rtmpServerStreamUrl(
                address = personalHotspotLocalAddress,
                port = rtmpServer.port,
                streamKey = stream.streamKey,
            ),
            image = "personalhotspot",
        ),
    )
    val fixedUrl = device.serverRtmpUrl
    if (fixedUrl != null && urls.none { it.url == fixedUrl }) {
        urls.add(0, GoProRtmpUrlAndImage(url = fixedUrl, image = "questionmark"))
    }
    return urls
}

private fun automaticServerRtmpUrl(
    device: SettingsGoProDevice,
    status: StatusOther,
    rtmpServer: SettingsRtmpServer,
): String? {
    val stream = rtmpServer.streams.firstOrNull { it.id == device.serverRtmpStreamId } ?: return null
    val ipStatus = status.ipStatuses.value.firstOrNull() ?: return null
    return rtmpServerStreamUrl(
        address = ipStatus.ipType.formatAddress(ipStatus.ip),
        port = rtmpServer.port,
        streamKey = stream.streamKey,
    )
}

@Composable
private fun GoProDeviceRtmpSection(
    model: Model = LocalModel.current,
    device: SettingsGoProDevice,
    status: StatusOther,
    rtmpServer: SettingsRtmpServer,
) {
    val ipStatuses by status.ipStatuses.collectAsState()
    val rtmpUrlType = device.rtmpUrlType
    val serverRtmpStreamId = device.serverRtmpStreamId
    val serverRtmpUrl = device.serverRtmpUrl
    val customRtmpUrl = device.customRtmpUrl
    val streams = rtmpServer.streams
    val started = device.isStarted
    val automaticUrl = remember(ipStatuses, streams, serverRtmpStreamId) {
        automaticServerRtmpUrl(device, status, rtmpServer)
    }
    val urlInfos = remember(ipStatuses, streams, serverRtmpStreamId, serverRtmpUrl) {
        serverUrls(model, device, status, rtmpServer)
    }
    Section(
        header = localized("RTMP"),
        footer = localized(
            "Select Server to stream into Moblin, or Custom to stream directly to another RTMP destination.",
        ),
    ) {
        Picker(
            title = localized("Type"),
            selection = rtmpUrlType,
            options = SettingsDjiDeviceUrlType.entries,
            enabled = !started,
            text = { it.toString() },
            onChange = { device.rtmpUrlType = it },
        )
        if (rtmpUrlType == SettingsDjiDeviceUrlType.server) {
            if (streams.isEmpty()) {
                Text(localized("No RTMP server streams exists"))
            } else {
                Picker(
                    title = localized("Stream"),
                    selection = streams.firstOrNull { it.id == serverRtmpStreamId } ?: streams.first(),
                    options = streams,
                    enabled = !started,
                    text = { it.name },
                    onChange = {
                        device.serverRtmpStreamId = it.id
                        device.serverRtmpUrl = null
                    },
                )
                Picker(
                    title = localized("URL"),
                    selection = serverRtmpUrl,
                    options = listOf(null) + urlInfos.map { it.url },
                    enabled = !started,
                    text = { it ?: automaticUrl ?: "" },
                    onChange = { device.serverRtmpUrl = it },
                )
                if (serverRtmpUrl == null && !status.isConnectedToIpv4WiFi()) {
                    Text(localized("⚠️ Not connected to an IPv4 WiFi network."))
                }
                if (!rtmpServer.enabled) {
                    Text(localized("⚠️ The RTMP server is not enabled"))
                }
            }
        } else {
            TextEditNavigationView(
                title = localized("URL"),
                value = customRtmpUrl,
                onSubmit = { device.customRtmpUrl = it },
            )
            if (customRtmpUrl.isEmpty()) {
                Text(localized("⚠️ Enter the URL the GoPro should stream to."))
            }
        }
    }
    LaunchedEffect(Unit) {
        val currentStreams = rtmpServer.streams
        if (currentStreams.isNotEmpty() && currentStreams.none { it.id == device.serverRtmpStreamId }) {
            device.serverRtmpStreamId = currentStreams.first().id
        }
    }
    ShortcutSectionView {
        RtmpServerSettingsView(rtmpServer = rtmpServer)
    }
}

@Composable
private fun GoProDeviceStreamSettingsSection(device: SettingsGoProDevice) {
    Section(header = localized("Stream settings")) {
        Picker(
            title = localized("Resolution"),
            selection = device.resolution,
            options = SettingsGoProLaunchLiveStreamResolution.entries,
            enabled = !device.isStarted,
            text = { it.rawValue },
            onChange = { device.resolution = it },
        )
        Picker(
            title = localized("Maximum bitrate"),
            selection = device.bitrate,
            options = goProDeviceBitrates,
            enabled = !device.isStarted,
            text = { formatBytesPerSecond(it.toLong()) },
            onChange = { device.bitrate = it },
        )
        Picker(
            title = localized("Lens"),
            selection = device.lens,
            options = SettingsGoProLens.entries,
            enabled = !device.isStarted,
            text = { it.toString() },
            onChange = { device.lens = it },
        )
        if (device.rtmpUrlType == SettingsDjiDeviceUrlType.server) {
            Toggle(
                title = localized("Auto-restart live stream when broken"),
                isOn = device.autoRestartStream,
                onChange = { device.autoRestartStream = it },
            )
        }
    }
}

@Composable
private fun GoProDeviceStartStopSection(
    model: Model = LocalModel.current,
    device: SettingsGoProDevice,
    status: StatusOther,
) {
    if (device.isStarted) {
        Section {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Blue)
                    .padding(8.dp),
            ) {
                TextButtonView("Stop live stream") {
                    model.stopGoProDeviceLiveStream(device = device)
                }
            }
        }
    } else {
        Section {
            TextButtonView("Start live stream") {
                if (device.canStartLive(status.isConnectedToIpv4WiFi())) {
                    model.startGoProDeviceLiveStream(device = device)
                }
            }
        }
    }
}

@Composable
fun GoProBleDeviceSettingsView(model: Model = LocalModel.current, device: SettingsGoProDevice) {
    Form(title = localized("GoPro device")) {
        Section {
            NameEditView(
                name = device.name,
                onNameChange = { name ->
                    device.name = name
                },
                existingNames = model.database.goPro.devices,
            )
        }
        GoProDeviceSelectionSection(model = model, device = device)
        GoProDeviceWifiSection(model = model, device = device)
        GoProDeviceRtmpSection(
            model = model,
            device = device,
            status = model.statusOther,
            rtmpServer = model.database.rtmpServer,
        )
        GoProDeviceStreamSettingsSection(device = device)
        Section {
            HCenter {
                Text(formatGoProDeviceState(device.state))
            }
        }
        GoProDeviceStartStopSection(
            model = model,
            device = device,
            status = model.statusOther,
        )
    }
}

@Composable
fun GoProBleDevicesSettingsSection(
    model: Model = LocalModel.current,
    goPro: SettingsGoPro,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Section(
        header = localized("Devices"),
        footer = localized(
            "Pair and control compatible GoPro devices over Bluetooth. HERO9 Black or newer is required.",
        ),
    ) {
        ForEach(
            goPro.devices,
            id = { it.id },
            onDelete = { offsets ->
                model.removeGoProDevices(offsets)
            },
            onMove = { from, to ->
                goPro.devices.move(fromOffsets = from, toOffset = to)
            },
        ) { device ->
            ContextMenuDeleteButton(
                action = {
                    val offset = goPro.devices.indexOfFirst { it.id == device.id }
                    if (offset >= 0) {
                        model.removeGoProDevices(setOf(offset))
                    }
                },
            ) {
                NavigationLink(
                    destination = {
                        GoProBleDeviceSettingsView(model = model, device = device)
                    },
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DraggableItemPrefixView()
                        Text(device.name)
                        Spacer(Modifier.weight(1f))
                        GrayTextView(text = formatGoProDeviceState(device.state))
                    }
                }
            }
        }
        CreateButtonView {
            val device = SettingsGoProDevice()
            device.name = makeUniqueName(
                name = SettingsGoProDevice.baseName,
                existingNames = goPro.devices,
            )
            goPro.devices.add(device)
        }
    }
}

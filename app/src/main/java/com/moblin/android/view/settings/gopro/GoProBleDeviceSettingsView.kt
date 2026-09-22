package com.moblin.android.view.settings.gopro

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.StatusOther
import com.moblin.android.various.model.bluetoothNotAllowedMessage
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

@OptIn(ExperimentalMaterial3Api::class)
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
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(localized("GoPro device")) })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            if (!bluetoothAllowed) {
                Text(bluetoothNotAllowedMessage)
            } else if (discoveredDevices.isEmpty()) {
                HCenter {
                    CircularProgressIndicator()
                }
            } else {
                for (discoveredDevice in discoveredDevices) {
                    TextButton(onClick = {
                        onSelect(discoveredDevice)
                        onDismiss()
                    }) {
                        Text(discoveredDevice.name)
                    }
                }
            }
            Text(
                localized(
                    "Put the GoPro in pairing mode, keep it nearby, and make sure GoPro Quik is disconnected.",
                ),
            )
        }
    }
    LaunchedEffect(Unit) {
        scanner.startScanningForDevices(context)
    }
    DisposableEffect(Unit) {
        onDispose {
            scanner.stopScanningForDevices()
        }
    }
}

@Composable
private fun GoProDeviceSelectionSection(model: Model = LocalModel.current, device: SettingsGoProDevice) {
    val bluetoothPeripheralId = device.bluetoothPeripheralId
    val bluetoothPeripheralName = device.bluetoothPeripheralName
    var showScanner by remember { mutableStateOf(false) }
    Text(localized("Device"), style = MaterialTheme.typography.titleSmall)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !device.isStarted) { showScanner = true }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextItemLocalizedView(
            name = localized("Device"),
            value = bluetoothPeripheralName ?: localized("Select device"),
        )
    }
    if (bluetoothPeripheralId == null) {
        Text(localized("⚠️ Select a GoPro device. The first connection requires pairing mode."))
    }
    if (showScanner) {
        GoProDeviceScannerSettingsView(
            model = model,
            onSelect = { discoveredDevice ->
                device.bluetoothPeripheralId =
                    UUID.nameUUIDFromBytes(discoveredDevice.peripheral.address.toByteArray())
                device.bluetoothPeripheralName = discoveredDevice.name
            },
            onDismiss = { showScanner = false },
        )
    }
}

@Composable
private fun GoProDeviceWifiSection(model: Model = LocalModel.current, device: SettingsGoProDevice) {
    val wifiSsid = device.wifiSsid
    val wifiPassword = device.wifiPassword
    var showSsidEdit by remember { mutableStateOf(false) }
    Text(localized("WiFi"), style = MaterialTheme.typography.titleSmall)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !device.isStarted) { showSsidEdit = true }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextItemLocalizedView(name = localized("SSID"), value = wifiSsid)
    }
    TextEditNavigationView(
        title = localized("Password"),
        value = wifiPassword,
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
                    model.database.savedWifiNetworks = model.database.savedWifiNetworks + newNetwork
                }
            }
        },
        sensitive = true,
    )
    if (wifiSsid.isEmpty()) {
        Text(localized("⚠️ Enter the WiFi network the GoPro should use for streaming."))
    }
    Text(localized("Moblin sends these credentials securely to the paired GoPro over Bluetooth."))
    if (showSsidEdit) {
        val update = { ssid: String ->
            device.wifiSsid = ssid
            if (device.wifiPassword.isEmpty()) {
                device.wifiPassword =
                    model.database.getSavedWiFiNetwork(ssid = ssid)?.password ?: ""
            }
        }
        WiFiSsidEditView(
            value = wifiSsid,
            onValueChange = update,
            onSubmit = update,
            onDismiss = { showSsidEdit = false },
        )
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
    val rtmpUrlType = device.rtmpUrlType
    val serverRtmpStreamId = device.serverRtmpStreamId
    val serverRtmpUrl = device.serverRtmpUrl
    val customRtmpUrl = device.customRtmpUrl
    val streams = rtmpServer.streams
    val rtmpServerEnabled = rtmpServer.enabled
    val started = device.isStarted
    val automaticUrl = automaticServerRtmpUrl(device, status, rtmpServer)

    var typeExpanded by remember { mutableStateOf(false) }
    var streamExpanded by remember { mutableStateOf(false) }
    var urlExpanded by remember { mutableStateOf(false) }

    Text(localized("RTMP"), style = MaterialTheme.typography.titleSmall)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(localized("Type"), modifier = Modifier.weight(1f))
        Box {
            TextButton(onClick = { typeExpanded = true }, enabled = !started) {
                Text(rtmpUrlType.toString())
            }
            DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                SettingsDjiDeviceUrlType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.toString()) },
                        onClick = {
                            device.rtmpUrlType = type
                            typeExpanded = false
                        },
                    )
                }
            }
        }
    }
    if (rtmpUrlType == SettingsDjiDeviceUrlType.server) {
        if (streams.isEmpty()) {
            Text(localized("No RTMP server streams exists"))
        } else {
            val selectedStream = streams.firstOrNull { it.id == serverRtmpStreamId }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(localized("Stream"), modifier = Modifier.weight(1f))
                Box {
                    TextButton(onClick = { streamExpanded = true }, enabled = !started) {
                        Text(selectedStream?.name ?: "")
                    }
                    DropdownMenu(
                        expanded = streamExpanded,
                        onDismissRequest = { streamExpanded = false },
                    ) {
                        streams.forEach { stream ->
                            DropdownMenuItem(
                                text = { Text(stream.name) },
                                onClick = {
                                    device.serverRtmpStreamId = stream.id
                                    device.serverRtmpUrl = null
                                    streamExpanded = false
                                },
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(localized("URL"), modifier = Modifier.weight(1f))
                Box {
                    TextButton(onClick = { urlExpanded = true }, enabled = !started) {
                        Text(serverRtmpUrl ?: automaticUrl ?: "")
                    }
                    DropdownMenu(
                        expanded = urlExpanded,
                        onDismissRequest = { urlExpanded = false },
                    ) {
                        Text(
                            localized("Auto IP address"),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    device.serverRtmpUrl = null
                                    urlExpanded = false
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null)
                            Text(automaticUrl ?: "")
                        }
                        Text(
                            localized("Fixed IP address"),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                        for (item in serverUrls(model, device, status, rtmpServer)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        device.serverRtmpUrl = item.url
                                        urlExpanded = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null)
                                Text(item.url)
                            }
                        }
                    }
                }
            }
            if (serverRtmpUrl == null && !status.isConnectedToIpv4WiFi()) {
                Text(localized("⚠️ Not connected to an IPv4 WiFi network."))
            }
            if (!rtmpServerEnabled) {
                Text(localized("⚠️ The RTMP server is not enabled"))
            }
        }
    } else {
        TextEditNavigationView(
            title = localized("URL"),
            value = customRtmpUrl,
            onSubmit = { value ->
                device.customRtmpUrl = value
            },
        )
        if (customRtmpUrl.isEmpty()) {
            Text(localized("⚠️ Enter the URL the GoPro should stream to."))
        }
    }
    Text(
        localized(
            "Select Server to stream into Moblin, or Custom to stream directly to another RTMP destination.",
        ),
    )
    LaunchedEffect(Unit) {
        if (streams.isNotEmpty() && streams.none { it.id == device.serverRtmpStreamId }) {
            device.serverRtmpStreamId = streams.first().id
        }
    }
    ShortcutSectionView {
        RtmpServerSettingsView(rtmpServer = rtmpServer)
    }
}

@Composable
private fun GoProDeviceStreamSettingsSection(device: SettingsGoProDevice) {
    val resolution = device.resolution
    val bitrate = device.bitrate
    val lens = device.lens
    val autoRestartStream = device.autoRestartStream
    val rtmpUrlType = device.rtmpUrlType
    val started = device.isStarted

    var resolutionExpanded by remember { mutableStateOf(false) }
    var bitrateExpanded by remember { mutableStateOf(false) }
    var lensExpanded by remember { mutableStateOf(false) }

    Text(localized("Stream settings"), style = MaterialTheme.typography.titleSmall)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(localized("Resolution"), modifier = Modifier.weight(1f))
        Box {
            TextButton(onClick = { resolutionExpanded = true }, enabled = !started) {
                Text(resolution.rawValue)
            }
            DropdownMenu(
                expanded = resolutionExpanded,
                onDismissRequest = { resolutionExpanded = false },
            ) {
                SettingsGoProLaunchLiveStreamResolution.entries.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item.rawValue) },
                        onClick = {
                            device.resolution = item
                            resolutionExpanded = false
                        },
                    )
                }
            }
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(localized("Maximum bitrate"), modifier = Modifier.weight(1f))
        Box {
            TextButton(onClick = { bitrateExpanded = true }, enabled = !started) {
                Text(formatBytesPerSecond(bitrate.toLong()))
            }
            DropdownMenu(
                expanded = bitrateExpanded,
                onDismissRequest = { bitrateExpanded = false },
            ) {
                goProDeviceBitrates.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(formatBytesPerSecond(item.toLong())) },
                        onClick = {
                            device.bitrate = item
                            bitrateExpanded = false
                        },
                    )
                }
            }
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(localized("Lens"), modifier = Modifier.weight(1f))
        Box {
            TextButton(onClick = { lensExpanded = true }, enabled = !started) {
                Text(lens.toString())
            }
            DropdownMenu(expanded = lensExpanded, onDismissRequest = { lensExpanded = false }) {
                SettingsGoProLens.entries.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item.toString()) },
                        onClick = {
                            device.lens = item
                            lensExpanded = false
                        },
                    )
                }
            }
        }
    }
    if (rtmpUrlType == SettingsDjiDeviceUrlType.server) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                localized("Auto-restart live stream when broken"),
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = autoRestartStream,
                onCheckedChange = { device.autoRestartStream = it },
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Blue)
                .padding(8.dp),
        ) {
            TextButtonView("Stop live stream") {
                Unit
            }
        }
    } else {
        TextButtonView("Start live stream") {
            if (device.canStartLive(status.isConnectedToIpv4WiFi())) {
                Unit
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoProBleDeviceSettingsView(model: Model = LocalModel.current, device: SettingsGoProDevice) {
    val state = device.state
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(localized("GoPro device")) })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            NameEditView(
                name = device.name,
                onNameChange = { name ->
                    device.name = name
                },
                existingNames = model.database.goPro.devices,
            )
            GoProDeviceSelectionSection(model = model, device = device)
            GoProDeviceWifiSection(model = model, device = device)
            GoProDeviceRtmpSection(
                model = model,
                device = device,
                status = model.statusOther,
                rtmpServer = model.database.rtmpServer,
            )
            GoProDeviceStreamSettingsSection(device = device)
            HCenter {
                Text(formatGoProDeviceState(state))
            }
            GoProDeviceStartStopSection(
                model = model,
                device = device,
                status = model.statusOther,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GoProBleDevicesSettingsSection(
    model: Model = LocalModel.current,
    goPro: SettingsGoPro,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val devices = goPro.devices
    Text(localized("Devices"), style = MaterialTheme.typography.titleSmall)
    for (device in devices) {
        val name = device.name
        val state = device.state
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { onNavigate("GoProBleDeviceSettingsView") },
                    onLongClick = {
                        Unit
                    },
                )
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DraggableItemPrefixView()
            Text(name)
            Spacer(Modifier.weight(1f))
            GrayTextView(text = formatGoProDeviceState(state))
        }
    }
    Unit
    CreateButtonView {
        val device = SettingsGoProDevice()
        device.name = makeUniqueName(
            name = SettingsGoProDevice.baseName,
            existingNames = devices,
        )
        goPro.devices.add(device)
    }
    Text(
        localized(
            "Pair and control compatible GoPro devices over Bluetooth. HERO9 Black or newer is required.",
        ),
    )
}

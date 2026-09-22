package com.moblin.android.view.settings.djidevices

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.formatBytesPerSecond
import com.moblin.android.common.various.personalHotspotLocalAddress
import com.moblin.android.common.various.urlImage
import com.moblin.android.integrations.dji.djidevice.DjiDeviceScanner
import com.moblin.android.integrations.dji.djidevice.DjiDeviceState
import com.moblin.android.integrations.dji.djidevice.canStartLive
import com.moblin.android.localized
import com.moblin.android.media.rtmpserver.rtmpServerApp
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
import com.moblin.android.view.settings.ingests.rtmpserver.RtmpServerSettingsView
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.NameEditView
import com.moblin.android.view.utils.ShortcutSectionView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.view.utils.TextButtonView
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.view.utils.TextItemLocalizedView
import java.util.UUID
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

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

    Text(
        text = "Device",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isStarted) {
                onNavigate("DjiDeviceScannerSettingsView")
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GrayTextView(
            text = bluetoothPeripheralName ?: localized("Select device"),
        )
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

    Text(
        text = "WiFi",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isStarted) {
                onNavigate("DjiDeviceWiFiSettingsInnerView")
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextItemLocalizedView(name = "Network", value = wifiSsid)
    }
    if (wifiSsid.isEmpty()) {
        Text(
            text = "⚠️ Enter the SSID of the network the DJI device should connect to.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        )
    }
    Text(
        text = "The DJI device will connect to and stream RTMP over this WiFi.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
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
        if (wifiSsid.isEmpty()) {
            return
        }
        val network = database.getSavedWiFiNetwork(wifiSsid)
        if (network != null) {
            network.password = wifiPassword
        } else {
            val newNetwork = SettingsWiFi()
            newNetwork.ssid = wifiSsid
            newNetwork.password = wifiPassword
            database.savedWifiNetworks =
                (database.savedWifiNetworks + newNetwork).toMutableList()
        }
    }

    LaunchedEffect(wifiSsid, wifiPassword) {
        updateSavedNetworks()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("WiFi") })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = "Network",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate("WiFiSsidEditView") }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextItemLocalizedView(name = "SSID", value = wifiSsid)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate("TextEditView") }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextItemLocalizedView(name = "Password", value = wifiPassword, sensitive = true)
            }
            if (savedWifiNetworks.isNotEmpty()) {
                Text(
                    text = "Saved networks",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp),
                )
                savedWifiNetworks.forEach { network ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                device.wifiSsid = network.ssid
                                device.wifiPassword = network.password
                            }
                            .padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(network.ssid)
                        Spacer(Modifier.weight(1f))
                        if (wifiSsid == network.ssid && wifiPassword == network.password) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.Blue)
                        }
                        IconButton(
                            onClick = {
                                database.savedWifiNetworks =
                                    database.savedWifiNetworks
                                        .filterNot {
                                            it.ssid == network.ssid
                                        }
                                        .toMutableList()
                            },
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                        }
                    }
                }
                SwipeLeftToDeleteHelpView(kind = localized("a network"))
            }
        }
    }
}

private data class RtmpUrlAndImage(val url: String, val image: String)

@OptIn(ExperimentalMaterial3Api::class)
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
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var streamMenuExpanded by remember { mutableStateOf(false) }
    var urlMenuExpanded by remember { mutableStateOf(false) }

    fun serverUrls(): List<RtmpUrlAndImage> {
        val streamKey: String = TODO("getRtmpStream")
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
            serverUrls.add(0, RtmpUrlAndImage(serverRtmpUrl, "questionmark"))
        }
        return serverUrls
    }

    LaunchedEffect(Unit) {
        val streams = rtmpServer.streams
        if (streams.isNotEmpty()) {
            if (streams.none { it.id == device.serverRtmpStreamId }) {
                device.serverRtmpStreamId = streams.first().id
            }
        }
    }
    LaunchedEffect(serverRtmpStreamId) {
        device.serverRtmpUrl = null
    }

    Text(
        text = "RTMP",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp),
    )
    ExposedDropdownMenuBox(
        expanded = typeMenuExpanded,
        onExpandedChange = { if (!isStarted) typeMenuExpanded = it },
    ) {
        OutlinedTextField(
            value = rtmpUrlType.toString(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Type") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded)
            },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        )
        ExposedDropdownMenu(
            expanded = typeMenuExpanded,
            onDismissRequest = { typeMenuExpanded = false },
        ) {
            SettingsDjiDeviceUrlType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.toString()) },
                    onClick = {
                        device.rtmpUrlType = type
                        typeMenuExpanded = false
                    },
                )
            }
        }
    }
    if (rtmpUrlType == SettingsDjiDeviceUrlType.server) {
        if (rtmpServerStreams.isEmpty()) {
            Text(
                text = "No RTMP server streams exists",
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
            )
        } else {
            ExposedDropdownMenuBox(
                expanded = streamMenuExpanded,
                onExpandedChange = { if (!isStarted) streamMenuExpanded = it },
            ) {
                OutlinedTextField(
                    value = rtmpServerStreams.firstOrNull { it.id == serverRtmpStreamId }?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Stream") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = streamMenuExpanded)
                    },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )
                ExposedDropdownMenu(
                    expanded = streamMenuExpanded,
                    onDismissRequest = { streamMenuExpanded = false },
                ) {
                    rtmpServerStreams.forEach { stream ->
                        DropdownMenuItem(
                            text = { Text(stream.name) },
                            onClick = {
                                device.serverRtmpStreamId = stream.id
                                streamMenuExpanded = false
                            },
                        )
                    }
                }
            }
            ExposedDropdownMenuBox(
                expanded = urlMenuExpanded,
                onExpandedChange = { if (!isStarted) urlMenuExpanded = it },
            ) {
                OutlinedTextField(
                    value = serverRtmpUrl ?: TODO("automaticServerRtmpUrl") ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("URL") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = urlMenuExpanded)
                    },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )
                ExposedDropdownMenu(
                    expanded = urlMenuExpanded,
                    onDismissRequest = { urlMenuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Wifi, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(TODO("automaticServerRtmpUrl") ?: "")
                            }
                        },
                        onClick = {
                            device.serverRtmpUrl = null
                            urlMenuExpanded = false
                        },
                    )
                    serverUrls().forEach { item ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(urlIcon(item.image), contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(item.url)
                                }
                            },
                            onClick = {
                                device.serverRtmpUrl = item.url
                                urlMenuExpanded = false
                            },
                        )
                    }
                }
            }
            if (serverRtmpUrl == null && !status.isConnectedToIpv4WiFi()) {
                Text(
                    text = "⚠️ Not connected to an IPv4 WiFi network.",
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                )
            }
            if (!rtmpServerEnabled) {
                Text(
                    text = "⚠️ The RTMP server is not enabled",
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                )
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
            Text(
                text = "⚠️ Enter the URL the DJI device should stream to.",
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
            )
        }
    }
    Text(
        text = "Select ${localized("Server")} if you want the DJI camera to stream to Moblin's RTMP server on this device. Select ${localized("Custom")} to make the DJI camera stream to any destination.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
    )
    ShortcutSectionView {
        RtmpServerSettingsView(rtmpServer = rtmpServer)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnScope.DjiDeviceSettingsSettingsView(device: SettingsDjiDevice) {
    val isStarted = device.isStarted
    val resolution = device.resolution
    val bitrate = device.bitrate
    val imageStabilization = device.imageStabilization
    val fps = device.fps
    val videoCodec = device.videoCodec
    val model = device.model
    var resolutionMenuExpanded by remember { mutableStateOf(false) }
    var bitrateMenuExpanded by remember { mutableStateOf(false) }
    var imageStabilizationMenuExpanded by remember { mutableStateOf(false) }
    var fpsMenuExpanded by remember { mutableStateOf(false) }
    var videoCodecMenuExpanded by remember { mutableStateOf(false) }

    Text(
        text = "Settings",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp),
    )
    ExposedDropdownMenuBox(
        expanded = resolutionMenuExpanded,
        onExpandedChange = { if (!isStarted) resolutionMenuExpanded = it },
    ) {
        OutlinedTextField(
            value = resolution.rawValue,
            onValueChange = {},
            readOnly = true,
            label = { Text("Resolution") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = resolutionMenuExpanded)
            },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        )
        ExposedDropdownMenu(
            expanded = resolutionMenuExpanded,
            onDismissRequest = { resolutionMenuExpanded = false },
        ) {
            SettingsDjiDeviceResolution.entries.forEach { value ->
                DropdownMenuItem(
                    text = { Text(value.rawValue) },
                    onClick = {
                        device.resolution = value
                        resolutionMenuExpanded = false
                    },
                )
            }
        }
    }
    ExposedDropdownMenuBox(
        expanded = bitrateMenuExpanded,
        onExpandedChange = { if (!isStarted) bitrateMenuExpanded = it },
    ) {
        OutlinedTextField(
            value = formatBytesPerSecond(bitrate.toLong()),
            onValueChange = {},
            readOnly = true,
            label = { Text("Bitrate") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = bitrateMenuExpanded)
            },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        )
        ExposedDropdownMenu(
            expanded = bitrateMenuExpanded,
            onDismissRequest = { bitrateMenuExpanded = false },
        ) {
            djiDeviceBitrates.forEach { value ->
                DropdownMenuItem(
                    text = { Text(formatBytesPerSecond(value.toLong())) },
                    onClick = {
                        device.bitrate = value
                        bitrateMenuExpanded = false
                    },
                )
            }
        }
    }
    if (model.hasImageStabilization()) {
        ExposedDropdownMenuBox(
            expanded = imageStabilizationMenuExpanded,
            onExpandedChange = { if (!isStarted) imageStabilizationMenuExpanded = it },
        ) {
            OutlinedTextField(
                value = imageStabilization.toString(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Image stabilization") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = imageStabilizationMenuExpanded,
                    )
                },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
            ExposedDropdownMenu(
                expanded = imageStabilizationMenuExpanded,
                onDismissRequest = { imageStabilizationMenuExpanded = false },
            ) {
                SettingsDjiDeviceImageStabilization.entries.forEach { value ->
                    DropdownMenuItem(
                        text = { Text(value.toString()) },
                        onClick = {
                            device.imageStabilization = value
                            imageStabilizationMenuExpanded = false
                        },
                    )
                }
            }
        }
    }
    if (model == SettingsDjiDeviceModel.osmoPocket3 || model == SettingsDjiDeviceModel.osmoPocket4) {
        ExposedDropdownMenuBox(
            expanded = fpsMenuExpanded,
            onExpandedChange = { if (!isStarted) fpsMenuExpanded = it },
        ) {
            OutlinedTextField(
                value = fps.toString(),
                onValueChange = {},
                readOnly = true,
                label = { Text("FPS") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = fpsMenuExpanded)
                },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
            ExposedDropdownMenu(
                expanded = fpsMenuExpanded,
                onDismissRequest = { fpsMenuExpanded = false },
            ) {
                djiDeviceFpss.forEach { value ->
                    DropdownMenuItem(
                        text = { Text(value.toString()) },
                        onClick = {
                            device.fps = value
                            fpsMenuExpanded = false
                        },
                    )
                }
            }
        }
    }
    if (model.hasVideoCodec()) {
        ExposedDropdownMenuBox(
            expanded = videoCodecMenuExpanded,
            onExpandedChange = { if (!isStarted) videoCodecMenuExpanded = it },
        ) {
            OutlinedTextField(
                value = videoCodec.rawValue,
                onValueChange = {},
                readOnly = true,
                label = { Text("Video codec") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = videoCodecMenuExpanded)
                },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
            ExposedDropdownMenu(
                expanded = videoCodecMenuExpanded,
                onDismissRequest = { videoCodecMenuExpanded = false },
            ) {
                SettingsDjiDeviceVideoCodec.entries.forEach { value ->
                    DropdownMenuItem(
                        text = { Text(value.rawValue) },
                        onClick = {
                            device.videoCodec = value
                            videoCodecMenuExpanded = false
                        },
                    )
                }
            }
        }
    }
    Text(
        text = "High bitrates may be unstable.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
    )
}

@Composable
private fun ColumnScope.DjiDeviceAutoRestartSettingsView(device: SettingsDjiDevice) {
    val rtmpUrlType = device.rtmpUrlType
    val autoRestartStream = device.autoRestartStream

    if (rtmpUrlType == SettingsDjiDeviceUrlType.server) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Auto-restart live stream when broken")
            Spacer(Modifier.weight(1f))
            Switch(
                checked = autoRestartStream,
                onCheckedChange = { device.autoRestartStream = it },
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
    val isStarted = device.isStarted

    if (!isStarted) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            if (device.canStartLive(status.isConnectedToIpv4WiFi())) {
                TextButtonView(title = "Start live stream") {
                    Unit
                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Blue)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            TextButtonView(title = "Stop live stream") {
                Unit
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
        Unit
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("DJI device") })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            NameEditView(
                name = name,
                onNameChange = { device.name = it },
                existingNames = existingNames,
            )
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
            HCenter {
                Text(state())
            }
            DjiDeviceStartStopButtonSettingsView(
                model = model,
                status = model.statusOther,
                device = device,
            )
        }
    }
}

private fun urlIcon(image: String): ImageVector {
    return when (image) {
        "personalhotspot" -> Icons.Default.Smartphone
        "questionmark" -> Icons.Default.HelpOutline
        else -> Icons.Default.Wifi
    }
}

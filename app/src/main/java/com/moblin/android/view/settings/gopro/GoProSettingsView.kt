package com.moblin.android.view.settings.gopro

import android.graphics.Bitmap
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import com.moblin.android.common.various.personalHotspotLocalAddress
import com.moblin.android.integrations.gopro.GoPro
import com.moblin.android.localized
import com.moblin.android.various.model.GoProState
import com.moblin.android.various.model.IpType
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.StatusOther
import com.moblin.android.various.settings.SettingsDjiDeviceUrlType
import com.moblin.android.various.settings.SettingsGoPro
import com.moblin.android.various.settings.SettingsGoProLaunchLiveStream
import com.moblin.android.various.settings.SettingsGoProLaunchLiveStreamResolution
import com.moblin.android.various.settings.SettingsGoProRtmpUrl
import com.moblin.android.various.settings.SettingsGoProWifiCredentials
import com.moblin.android.various.utils.makeOffsets
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.catprinters.IntegrationImageView
import com.moblin.android.view.settings.djidevices.rtmpServerStreamUrl
import com.moblin.android.view.settings.ingests.rtmpserver.RtmpServerSettingsView
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.DraggableItemTextView
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.NameEditView
import com.moblin.android.view.utils.QrCodeImageView
import com.moblin.android.view.utils.ShortcutSectionView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.view.utils.TextItemLocalizedView
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

fun qrCodeHeight(metrics: Dp): Dp = metrics * 0.5f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoProLaunchLiveStreamSettingsView(
    goPro: SettingsGoPro,
    launchLiveStream: SettingsGoProLaunchLiveStream,
) {
    var qrCode by remember { mutableStateOf<Bitmap?>(null) }
    var resolutionExpanded by remember { mutableStateOf(false) }

    val generate: () -> Unit = {
        qrCode = GoPro.generateLaunchLiveStream(
            isHero12Or13 = launchLiveStream.isHero12Or13,
            resolution = launchLiveStream.resolution,
        )
    }

    LaunchedEffect(Unit) { generate() }

    BoxWithConstraints {
        val metrics = maxWidth
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            NameEditView(
                name = launchLiveStream.name,
                existingNames = goPro.launchLiveStream,
                onChange = { launchLiveStream.name = it },
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("HERO 12/13")
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = launchLiveStream.isHero12Or13,
                    onCheckedChange = { value ->
                        launchLiveStream.isHero12Or13 = value
                        generate()
                    },
                )
            }
            ExposedDropdownMenuBox(
                expanded = resolutionExpanded,
                onExpandedChange = { resolutionExpanded = it },
            ) {
                OutlinedTextField(
                    value = launchLiveStream.resolution.rawValue,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Resolution") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = resolutionExpanded)
                    },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = resolutionExpanded,
                    onDismissRequest = { resolutionExpanded = false },
                ) {
                    SettingsGoProLaunchLiveStreamResolution.entries.forEach { resolution ->
                        DropdownMenuItem(
                            text = { Text(resolution.rawValue) },
                            onClick = {
                                launchLiveStream.resolution = resolution
                                resolutionExpanded = false
                                generate()
                            },
                        )
                    }
                }
            }
            qrCode?.let { code ->
                QrCodeImageView(image = code, height = qrCodeHeight(metrics))
            }
        }
    }
}

@Composable
private fun GoProLaunchLiveStreamSettingsEntryView(
    goPro: SettingsGoPro,
    launchLiveStream: SettingsGoProLaunchLiveStream,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Box(modifier = Modifier.clickable { onNavigate("goProLaunchLiveStreamSettings") }) {
        DraggableItemTextView(name = launchLiveStream.name)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoProWifiCredentialsSettingsView(
    goPro: SettingsGoPro,
    wifiCredentials: SettingsGoProWifiCredentials,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    var qrCode by remember { mutableStateOf<Bitmap?>(null) }

    val generate: () -> Unit = {
        qrCode = GoPro.generateWifiCredentialsQrCode(
            ssid = wifiCredentials.ssid,
            password = wifiCredentials.password,
        )
    }

    LaunchedEffect(Unit) { generate() }

    BoxWithConstraints {
        val metrics = maxWidth
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            NameEditView(
                name = wifiCredentials.name,
                existingNames = goPro.wifiCredentials,
                onChange = { wifiCredentials.name = it },
            )
            Box(modifier = Modifier.clickable { onNavigate("wifiSsid") }) {
                TextItemLocalizedView(name = "SSID", value = wifiCredentials.ssid)
            }
            Box(modifier = Modifier.clickable { onNavigate("wifiCredentialsPassword") }) {
                TextItemLocalizedView(
                    name = "Password",
                    value = wifiCredentials.password,
                    sensitive = true,
                )
            }
            qrCode?.let { code ->
                QrCodeImageView(image = code, height = qrCodeHeight(metrics))
            }
        }
    }
}

@Composable
private fun GoProWifiCredentialsSettingsEntryView(
    goPro: SettingsGoPro,
    wifiCredentials: SettingsGoProWifiCredentials,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Box(modifier = Modifier.clickable { onNavigate("goProWifiCredentialsSettings") }) {
        DraggableItemTextView(name = wifiCredentials.name)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoProRtmpUrlSettingsView(
    model: Model = LocalModel.current,
    goPro: SettingsGoPro,
    status: StatusOther,
    rtmpUrl: SettingsGoProRtmpUrl,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    var qrCode by remember { mutableStateOf<Bitmap?>(null) }
    var typeExpanded by remember { mutableStateOf(false) }
    var streamExpanded by remember { mutableStateOf(false) }
    var urlExpanded by remember { mutableStateOf(false) }

    val generate: () -> Unit = {
        when (rtmpUrl.type) {
            SettingsDjiDeviceUrlType.server -> {
                qrCode = GoPro.generateRtmpUrlQrCode(url = rtmpUrl.serverUrl)
            }
            SettingsDjiDeviceUrlType.custom -> {
                qrCode = GoPro.generateRtmpUrlQrCode(url = rtmpUrl.customUrl)
            }
        }
    }

    val serverUrls: () -> List<String> = {
        val stream = model.getRtmpStream(rtmpUrl.serverStreamId)
        if (stream == null) {
            emptyList()
        } else {
            val urls = mutableListOf<String>()
            for (ipStatus in status.ipStatuses.filter { it.ipType == IpType.ipv4 }) {
                urls.add(
                    rtmpServerStreamUrl(
                        address = ipStatus.ipType.formatAddress(ipStatus.ip),
                        port = model.database.rtmpServer.port,
                        streamKey = stream.streamKey,
                    )
                )
            }
            urls.add(
                rtmpServerStreamUrl(
                    address = personalHotspotLocalAddress,
                    port = model.database.rtmpServer.port,
                    streamKey = stream.streamKey,
                )
            )
            for (ipStatus in status.ipStatuses.filter { it.ipType == IpType.ipv6 }) {
                urls.add(
                    rtmpServerStreamUrl(
                        address = ipStatus.ipType.formatAddress(ipStatus.ip),
                        port = model.database.rtmpServer.port,
                        streamKey = stream.streamKey,
                    )
                )
            }
            urls
        }
    }

    LaunchedEffect(Unit) {
        val streams = model.database.rtmpServer.streams
        if (streams.isNotEmpty()) {
            if (streams.none { it.id == rtmpUrl.serverStreamId }) {
                rtmpUrl.serverStreamId = streams.first().id
            }
            if (serverUrls().none { it == rtmpUrl.serverUrl }) {
                rtmpUrl.serverUrl = serverUrls().firstOrNull() ?: ""
            }
        }
        generate()
    }

    LaunchedEffect(rtmpUrl.serverStreamId) {
        rtmpUrl.serverUrl = serverUrls().firstOrNull() ?: ""
    }

    LaunchedEffect(rtmpUrl.serverUrl, rtmpUrl.customUrl) {
        generate()
    }

    BoxWithConstraints {
        val metrics = maxWidth
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            NameEditView(
                name = rtmpUrl.name,
                existingNames = goPro.rtmpUrls,
                onChange = { rtmpUrl.name = it },
            )
            Text("RTMP", style = MaterialTheme.typography.titleSmall)
            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = it },
            ) {
                OutlinedTextField(
                    value = rtmpUrl.type.toString(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Type") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded)
                    },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = typeExpanded,
                    onDismissRequest = { typeExpanded = false },
                ) {
                    SettingsDjiDeviceUrlType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.toString()) },
                            onClick = {
                                rtmpUrl.type = type
                                typeExpanded = false
                                generate()
                            },
                        )
                    }
                }
            }
            if (rtmpUrl.type == SettingsDjiDeviceUrlType.server) {
                if (model.database.rtmpServer.streams.isEmpty()) {
                    Text("No RTMP server streams exists")
                } else {
                    ExposedDropdownMenuBox(
                        expanded = streamExpanded,
                        onExpandedChange = { streamExpanded = it },
                    ) {
                        val selectedStreamName = model.database.rtmpServer.streams
                            .firstOrNull { it.id == rtmpUrl.serverStreamId }
                            ?.name
                            ?: ""
                        OutlinedTextField(
                            value = selectedStreamName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Stream") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = streamExpanded)
                            },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = streamExpanded,
                            onDismissRequest = { streamExpanded = false },
                        ) {
                            model.database.rtmpServer.streams.forEach { stream ->
                                DropdownMenuItem(
                                    text = { Text(stream.name) },
                                    onClick = {
                                        rtmpUrl.serverStreamId = stream.id
                                        streamExpanded = false
                                    },
                                )
                            }
                        }
                    }
                    ExposedDropdownMenuBox(
                        expanded = urlExpanded,
                        onExpandedChange = { urlExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = rtmpUrl.serverUrl,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("URL") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = urlExpanded)
                            },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = urlExpanded,
                            onDismissRequest = { urlExpanded = false },
                        ) {
                            serverUrls().forEach { url ->
                                DropdownMenuItem(
                                    text = { Text(url) },
                                    onClick = {
                                        rtmpUrl.serverUrl = url
                                        urlExpanded = false
                                    },
                                )
                            }
                        }
                    }
                    if (!model.database.rtmpServer.enabled) {
                        Text("⚠️ The RTMP server is not enabled")
                    }
                }
            } else if (rtmpUrl.type == SettingsDjiDeviceUrlType.custom) {
                Box(modifier = Modifier.clickable { onNavigate("rtmpCustomUrl") }) {
                    Text(rtmpUrl.customUrl)
                }
            }
            Text(
                "Select ${localized("Server")} if you want the GoPro camera to stream to " +
                    "Moblin's RTMP server on this device. Select ${localized("Custom")} to " +
                    "make the GoPro camera stream to any destination.",
                style = MaterialTheme.typography.bodySmall,
            )
            ShortcutSectionView {
                RtmpServerSettingsView(rtmpServer = model.database.rtmpServer)
            }
            qrCode?.let { code ->
                QrCodeImageView(image = code, height = qrCodeHeight(metrics))
            }
        }
    }
}

@Composable
private fun GoProRtmpUrlSettingsEntryView(
    goPro: SettingsGoPro,
    status: StatusOther,
    rtmpUrl: SettingsGoProRtmpUrl,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Box(modifier = Modifier.clickable { onNavigate("goProRtmpUrlSettings") }) {
        DraggableItemTextView(name = rtmpUrl.name)
    }
}

@Composable
private fun GoProLaunchLiveStream(
    goPro: SettingsGoPro,
    goProState: GoProState,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val deleteLaunchLiveStream: (List<Int>) -> Unit = { offsets ->
        offsets.sortedDescending().forEach { goPro.launchLiveStream.removeAt(it) }
        if (goPro.launchLiveStream.none { it.id == goPro.selectedLaunchLiveStream }) {
            goPro.selectedLaunchLiveStream = goPro.launchLiveStream.firstOrNull()?.id
            goProState.launchLiveStreamSelection = goPro.selectedLaunchLiveStream
        }
    }

    Column {
        Text("Launch live streams", style = MaterialTheme.typography.titleSmall)
        goPro.launchLiveStream.forEach { launchLiveStream ->
            Box(
                modifier = Modifier.pointerInput(launchLiveStream.id) {
                    detectTapGestures(
                        onLongPress = {
                            makeOffsets(goPro.launchLiveStream, launchLiveStream.id)?.let {
                                deleteLaunchLiveStream(it)
                            }
                        },
                    )
                },
            ) {
                GoProLaunchLiveStreamSettingsEntryView(
                    goPro = goPro,
                    launchLiveStream = launchLiveStream,
                    onNavigate = onNavigate,
                )
            }
        }
        CreateButtonView {
            val launchLiveStream = SettingsGoProLaunchLiveStream()
            launchLiveStream.name = makeUniqueName(
                name = SettingsGoProLaunchLiveStream.baseName,
                existingNames = goPro.launchLiveStream,
            )
            if (goPro.launchLiveStream.isEmpty()) {
                goPro.selectedLaunchLiveStream = launchLiveStream.id
                goProState.launchLiveStreamSelection = goPro.selectedLaunchLiveStream
            }
            goPro.launchLiveStream.add(launchLiveStream)
        }
        SwipeLeftToDeleteHelpView(kind = localized("an entry"))
    }
}

@Composable
private fun GoProWifiCredentials(
    goPro: SettingsGoPro,
    goProState: GoProState,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val deleteWifiCredentials: (List<Int>) -> Unit = { offsets ->
        offsets.sortedDescending().forEach { goPro.wifiCredentials.removeAt(it) }
        if (goPro.wifiCredentials.none { it.id == goPro.selectedWifiCredentials }) {
            goPro.selectedWifiCredentials = goPro.wifiCredentials.firstOrNull()?.id
            goProState.wifiCredentialsSelection = goPro.selectedWifiCredentials
        }
    }

    Column {
        Text("WiFi credentials", style = MaterialTheme.typography.titleSmall)
        goPro.wifiCredentials.forEach { wifiCredentials ->
            Box(
                modifier = Modifier.pointerInput(wifiCredentials.id) {
                    detectTapGestures(
                        onLongPress = {
                            makeOffsets(goPro.wifiCredentials, wifiCredentials.id)?.let {
                                deleteWifiCredentials(it)
                            }
                        },
                    )
                },
            ) {
                GoProWifiCredentialsSettingsEntryView(
                    goPro = goPro,
                    wifiCredentials = wifiCredentials,
                    onNavigate = onNavigate,
                )
            }
        }
        CreateButtonView {
            val wifiCredentials = SettingsGoProWifiCredentials()
            wifiCredentials.name = makeUniqueName(
                name = SettingsGoProWifiCredentials.baseName,
                existingNames = goPro.wifiCredentials,
            )
            if (goPro.wifiCredentials.isEmpty()) {
                goPro.selectedWifiCredentials = wifiCredentials.id
                goProState.wifiCredentialsSelection = goPro.selectedWifiCredentials
            }
            goPro.wifiCredentials.add(wifiCredentials)
        }
        SwipeLeftToDeleteHelpView(kind = localized("an entry"))
    }
}

@Composable
private fun GoProRtmpUrls(
    status: StatusOther,
    goPro: SettingsGoPro,
    goProState: GoProState,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val deleteRtmpUrl: (List<Int>) -> Unit = { offsets ->
        offsets.sortedDescending().forEach { goPro.rtmpUrls.removeAt(it) }
        if (goPro.rtmpUrls.none { it.id == goPro.selectedRtmpUrl }) {
            goPro.selectedRtmpUrl = goPro.rtmpUrls.firstOrNull()?.id
            goProState.rtmpUrlSelection = goPro.selectedRtmpUrl
        }
    }

    Column {
        Text("RTMP URLs", style = MaterialTheme.typography.titleSmall)
        goPro.rtmpUrls.forEach { rtmpUrl ->
            Box(
                modifier = Modifier.pointerInput(rtmpUrl.id) {
                    detectTapGestures(
                        onLongPress = {
                            makeOffsets(goPro.rtmpUrls, rtmpUrl.id)?.let {
                                deleteRtmpUrl(it)
                            }
                        },
                    )
                },
            ) {
                GoProRtmpUrlSettingsEntryView(
                    goPro = goPro,
                    status = status,
                    rtmpUrl = rtmpUrl,
                    onNavigate = onNavigate,
                )
            }
        }
        CreateButtonView {
            val rtmpUrl = SettingsGoProRtmpUrl()
            rtmpUrl.name = makeUniqueName(
                name = SettingsGoProRtmpUrl.baseName,
                existingNames = goPro.rtmpUrls,
            )
            if (goPro.rtmpUrls.isEmpty()) {
                goPro.selectedRtmpUrl = rtmpUrl.id
                goProState.rtmpUrlSelection = goPro.selectedRtmpUrl
            }
            goPro.rtmpUrls.add(rtmpUrl)
        }
        SwipeLeftToDeleteHelpView(kind = localized("a URL"))
    }
}

@Composable
private fun GoProQrCodesSettingsView(
    model: Model = LocalModel.current,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        GoProLaunchLiveStream(
            goPro = model.database.goPro,
            goProState = model.goPro,
            onNavigate = onNavigate,
        )
        GoProWifiCredentials(
            goPro = model.database.goPro,
            goProState = model.goPro,
            onNavigate = onNavigate,
        )
        GoProRtmpUrls(
            status = model.statusOther,
            goPro = model.database.goPro,
            goProState = model.goPro,
            onNavigate = onNavigate,
        )
    }
}

@Composable
fun GoProSettingsView(
    model: Model = LocalModel.current,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        HCenter {
            IntegrationImageView(imageName = "GoPro")
        }
        GoProBleDevicesSettingsSection(goPro = model.database.goPro)
        Box(modifier = Modifier.clickable { onNavigate("goProQrCodes") }) {
            Text("QR codes")
        }
    }
}

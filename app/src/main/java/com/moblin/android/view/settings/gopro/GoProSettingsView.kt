package com.moblin.android.view.settings.gopro

import android.graphics.Bitmap
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.common.various.personalHotspotLocalAddress
import com.moblin.android.integrations.gopro.GoPro
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.ForEach
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.move
import com.moblin.android.various.model.GoProState
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
import com.moblin.android.view.utils.ContextMenuDeleteButton
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.DraggableItemTextView
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.NameEditView
import com.moblin.android.view.utils.QrCodeImageView
import com.moblin.android.view.utils.ShortcutSectionView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.view.utils.TextEditView
import com.moblin.android.view.utils.TextItemLocalizedView
import com.moblin.android.view.utils.WiFiSsidEditView
import com.moblin.android.various.model.getRtmpStream

fun qrCodeHeight(metrics: Dp): Double = metrics.value * 0.5

@Composable
private fun GoProLaunchLiveStreamSettingsView(
    goPro: SettingsGoPro,
    launchLiveStream: SettingsGoProLaunchLiveStream,
) {
    var qrCode by remember { mutableStateOf<Bitmap?>(null) }

    val generate: () -> Unit = {
        qrCode = GoPro.generateLaunchLiveStream(
            isHero12Or13 = launchLiveStream.isHero12Or13,
            resolution = launchLiveStream.resolution,
        )
    }

    LaunchedEffect(Unit) { generate() }

    BoxWithConstraints {
        val metrics = maxWidth
        Form(title = "Launch live stream") {
            Section {
                NameEditView(
                    name = launchLiveStream.name,
                    existingNames = goPro.launchLiveStream,
                    onNameChange = { launchLiveStream.name = it },
                )
            }
            Section {
                Toggle(
                    title = "HERO 12/13",
                    isOn = launchLiveStream.isHero12Or13,
                    onChange = { value ->
                        launchLiveStream.isHero12Or13 = value
                        generate()
                    },
                )
                Picker(
                    title = "Resolution",
                    selection = launchLiveStream.resolution,
                    options = SettingsGoProLaunchLiveStreamResolution.entries,
                    text = { it.rawValue },
                    onChange = { resolution ->
                        launchLiveStream.resolution = resolution
                        generate()
                    },
                )
            }
            qrCode?.let { code ->
                Section {
                    QrCodeImageView(image = code.asImageBitmap(), height = qrCodeHeight(metrics))
                }
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
    NavigationLink(
        destination = {
            GoProLaunchLiveStreamSettingsView(
                goPro = goPro,
                launchLiveStream = launchLiveStream,
            )
        },
    ) {
        DraggableItemTextView(name = launchLiveStream.name)
    }
}

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
        Form(title = "WiFi credentials") {
            Section {
                NameEditView(
                    name = wifiCredentials.name,
                    existingNames = goPro.wifiCredentials,
                    onNameChange = { wifiCredentials.name = it },
                )
            }
            Section {
                NavigationLink(
                    destination = {
                        WiFiSsidEditView(
                            value = wifiCredentials.ssid,
                            onValueChange = { wifiCredentials.ssid = it },
                            onSubmit = {
                                wifiCredentials.ssid = it
                                generate()
                            },
                            onDismiss = {},
                        )
                    },
                ) {
                    TextItemLocalizedView(name = "SSID", value = wifiCredentials.ssid)
                }
                NavigationLink(
                    destination = {
                        TextEditView(
                            title = localized("Password"),
                            value = wifiCredentials.password,
                            onSubmit = {
                                wifiCredentials.password = it
                                generate()
                            },
                        )
                    },
                ) {
                    TextItemLocalizedView(
                        name = "Password",
                        value = wifiCredentials.password,
                        sensitive = true,
                    )
                }
            }
            qrCode?.let { code ->
                Section {
                    QrCodeImageView(image = code.asImageBitmap(), height = qrCodeHeight(metrics))
                }
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
    NavigationLink(
        destination = {
            GoProWifiCredentialsSettingsView(
                goPro = goPro,
                wifiCredentials = wifiCredentials,
            )
        },
    ) {
        DraggableItemTextView(name = wifiCredentials.name)
    }
}

@Composable
private fun GoProRtmpUrlSettingsView(
    model: Model = LocalModel.current,
    goPro: SettingsGoPro,
    status: StatusOther,
    rtmpUrl: SettingsGoProRtmpUrl,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    var qrCode by remember { mutableStateOf<Bitmap?>(null) }

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

    val serverUrls: () -> List<String> = ports@{
        val stream = model.getRtmpStream(rtmpUrl.serverStreamId) ?: return@ports emptyList()
        val urls = mutableListOf<String>()
        status.ipStatuses.value.filter { it.ipType.toString().equals("ipv4", ignoreCase = true) }.forEach { ipStatus ->
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
        status.ipStatuses.value.filter { it.ipType.toString().equals("ipv6", ignoreCase = true) }.forEach { ipStatus ->
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
    }

    LaunchedEffect(rtmpUrl.serverUrl, rtmpUrl.customUrl) {
        generate()
    }

    BoxWithConstraints {
        val metrics = maxWidth
        Form(title = "RTMP URL") {
            Section {
                NameEditView(
                    name = rtmpUrl.name,
                    existingNames = goPro.rtmpUrls,
                    onNameChange = { rtmpUrl.name = it },
                )
            }
            Section(
                header = "RTMP",
                footer = "Select ${localized("Server")} if you want the GoPro camera to stream to " +
                    "Moblin's RTMP server on this device. Select ${localized("Custom")} to " +
                    "make the GoPro camera stream to any destination.",
            ) {
                Picker(
                    title = "Type",
                    selection = rtmpUrl.type,
                    options = SettingsDjiDeviceUrlType.entries,
                    text = { it.toString() },
                    onChange = { type ->
                        rtmpUrl.type = type
                        generate()
                    },
                )
                if (rtmpUrl.type == SettingsDjiDeviceUrlType.server) {
                    val streams = model.database.rtmpServer.streams
                    if (streams.isEmpty()) {
                        Text(localized("No RTMP server streams exists"))
                    } else {
                        Picker(
                            title = "Stream",
                            selection = rtmpUrl.serverStreamId,
                            options = streams.map { it.id },
                            text = { id -> streams.firstOrNull { it.id == id }?.name ?: "" },
                            onChange = { id ->
                                rtmpUrl.serverStreamId = id
                                rtmpUrl.serverUrl = serverUrls().firstOrNull() ?: ""
                            },
                        )
                        Picker(
                            title = "URL",
                            selection = rtmpUrl.serverUrl,
                            options = serverUrls(),
                            onChange = { url -> rtmpUrl.serverUrl = url },
                        )
                        if (!model.database.rtmpServer.enabled) {
                            Text(localized("⚠️ The RTMP server is not enabled"))
                        }
                    }
                } else if (rtmpUrl.type == SettingsDjiDeviceUrlType.custom) {
                    TextEditNavigationView(
                        title = localized("URL"),
                        value = rtmpUrl.customUrl,
                        onSubmit = { rtmpUrl.customUrl = it },
                    )
                }
            }
            ShortcutSectionView {
                RtmpServerSettingsView(rtmpServer = model.database.rtmpServer)
            }
            qrCode?.let { code ->
                Section {
                    QrCodeImageView(image = code.asImageBitmap(), height = qrCodeHeight(metrics))
                }
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
    NavigationLink(
        destination = {
            GoProRtmpUrlSettingsView(
                goPro = goPro,
                status = status,
                rtmpUrl = rtmpUrl,
            )
        },
    ) {
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
            goProState.launchLiveStreamSelection.value = goPro.selectedLaunchLiveStream
        }
    }

    Section(
        header = "Launch live streams",
        footerContent = { SwipeLeftToDeleteHelpView(kind = localized("an entry")) },
    ) {
        ForEach(
            goPro.launchLiveStream,
            id = { it.id },
            onDelete = { offsets ->
                deleteLaunchLiveStream(offsets.toList())
            },
            onMove = { froms, to ->
                goPro.launchLiveStream.move(fromOffsets = froms, toOffset = to)
            },
        ) { launchLiveStream ->
            ContextMenuDeleteButton(
                action = {
                    val offset = goPro.launchLiveStream.indexOfFirst { it.id == launchLiveStream.id }
                        .takeIf { it >= 0 }
                    if (offset != null) {
                        deleteLaunchLiveStream(listOf(offset))
                    }
                },
            ) {
                GoProLaunchLiveStreamSettingsEntryView(
                    goPro = goPro,
                    launchLiveStream = launchLiveStream,
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
                goProState.launchLiveStreamSelection.value = goPro.selectedLaunchLiveStream
            }
            goPro.launchLiveStream.add(launchLiveStream)
        }
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
            goProState.wifiCredentialsSelection.value = goPro.selectedWifiCredentials
        }
    }

    Section(
        header = "WiFi credentials",
        footerContent = { SwipeLeftToDeleteHelpView(kind = localized("an entry")) },
    ) {
        ForEach(
            goPro.wifiCredentials,
            id = { it.id },
            onDelete = { offsets ->
                deleteWifiCredentials(offsets.toList())
            },
            onMove = { froms, to ->
                goPro.wifiCredentials.move(fromOffsets = froms, toOffset = to)
            },
        ) { wifiCredentials ->
            ContextMenuDeleteButton(
                action = {
                    val offset = goPro.wifiCredentials.indexOfFirst { it.id == wifiCredentials.id }
                        .takeIf { it >= 0 }
                    if (offset != null) {
                        deleteWifiCredentials(listOf(offset))
                    }
                },
            ) {
                GoProWifiCredentialsSettingsEntryView(
                    goPro = goPro,
                    wifiCredentials = wifiCredentials,
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
                goProState.wifiCredentialsSelection.value = goPro.selectedWifiCredentials
            }
            goPro.wifiCredentials.add(wifiCredentials)
        }
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
            goProState.rtmpUrlSelection.value = goPro.selectedRtmpUrl
        }
    }

    Section(
        header = "RTMP URLs",
        footerContent = { SwipeLeftToDeleteHelpView(kind = localized("a URL")) },
    ) {
        ForEach(
            goPro.rtmpUrls,
            id = { it.id },
            onDelete = { offsets ->
                deleteRtmpUrl(offsets.toList())
            },
            onMove = { froms, to ->
                goPro.rtmpUrls.move(fromOffsets = froms, toOffset = to)
            },
        ) { rtmpUrl ->
            ContextMenuDeleteButton(
                action = {
                    val offset = goPro.rtmpUrls.indexOfFirst { it.id == rtmpUrl.id }
                        .takeIf { it >= 0 }
                    if (offset != null) {
                        deleteRtmpUrl(listOf(offset))
                    }
                },
            ) {
                GoProRtmpUrlSettingsEntryView(
                    goPro = goPro,
                    status = status,
                    rtmpUrl = rtmpUrl,
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
                goProState.rtmpUrlSelection.value = goPro.selectedRtmpUrl
            }
            goPro.rtmpUrls.add(rtmpUrl)
        }
    }
}

@Composable
private fun GoProQrCodesSettingsView(
    model: Model = LocalModel.current,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Form(title = "QR codes") {
        GoProLaunchLiveStream(
            goPro = model.database.goPro,
            goProState = model.goPro,
        )
        GoProWifiCredentials(
            goPro = model.database.goPro,
            goProState = model.goPro,
        )
        GoProRtmpUrls(
            status = model.statusOther,
            goPro = model.database.goPro,
            goProState = model.goPro,
        )
    }
}

@Composable
fun GoProSettingsView(
    model: Model = LocalModel.current,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Form(title = "GoPro") {
        Section {
            HCenter {
                IntegrationImageView(imageName = "GoPro")
            }
        }
        GoProBleDevicesSettingsSection(goPro = model.database.goPro)
        Section {
            NavigationLink(
                destination = {
                    GoProQrCodesSettingsView(model = model)
                },
            ) {
                Text(localized("QR codes"))
            }
        }
    }
}

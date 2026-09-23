package com.moblin.android.view.controlbar.quickbutton

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.integrations.gopro.GoPro
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Label
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.model.GoProState
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsDjiDeviceUrlType
import com.moblin.android.various.settings.SettingsGoPro
import com.moblin.android.various.settings.SettingsGoProDevice
import com.moblin.android.view.settings.gopro.GoProSettingsView
import com.moblin.android.view.settings.gopro.formatGoProDeviceState
import com.moblin.android.view.settings.gopro.qrCodeHeight
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.QrCodeImageView
import com.moblin.android.view.utils.ShortcutSectionView
import java.util.UUID
import kotlinx.coroutines.launch
import com.moblin.android.localized
import com.moblin.android.various.model.startGoProDeviceLiveStream
import com.moblin.android.various.model.stopGoProDeviceLiveStream

private data class PickerEntry(val id: UUID, val name: String)

@Composable
private fun QuickButtonGoProBleDeviceView(
    model: Model = LocalModel.current,
    device: SettingsGoProDevice,
) {
    Toggle(
        isOn = device.isStarted,
        onChange = { value ->
            if (value) {
                model.startGoProDeviceLiveStream(device = device)
            } else {
                model.stopGoProDeviceLiveStream(device = device)
            }
        },
        enabled = device.canStartLive(model.statusOther.isConnectedToIpv4WiFi()),
    ) {
        Text(device.name)
        Spacer(Modifier.weight(1f))
        GrayTextView(text = formatGoProDeviceState(device.state))
    }
}

@Composable
private fun QuickButtonGoProLaunchLiveStreamView(
    goProState: GoProState,
    goPro: SettingsGoPro,
    height: Double,
    onLaunchLiveStreamSelectionChange: (UUID?) -> Unit,
    onSelectedLaunchLiveStreamChange: (UUID?) -> Unit,
) {
    val launchLiveStreamSelection by goProState.launchLiveStreamSelection.collectAsState()
    val launchLiveStream = goPro.launchLiveStream
    var qrCode by remember { mutableStateOf<ImageBitmap?>(null) }
    var entries by remember { mutableStateOf<List<PickerEntry>>(emptyList()) }

    fun generate() {
        val selection = goProState.launchLiveStreamSelection.value
        val selected = launchLiveStream.firstOrNull { it.id == selection }
        qrCode = if (selected != null) {
            GoPro.generateLaunchLiveStream(
                isHero12Or13 = selected.isHero12Or13,
                resolution = selected.resolution,
            )?.asImageBitmap()
        } else {
            null
        }
    }

    LaunchedEffect(Unit) {
        entries = launchLiveStream.map { PickerEntry(id = it.id, name = it.name) }
        generate()
    }

    Column {
        if (launchLiveStreamSelection != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(localized("Launch live stream"))
                Spacer(Modifier.weight(1f))
                Picker(
                    title = "",
                    selection = launchLiveStreamSelection!!,
                    options = entries.map { it.id },
                    text = { id -> entries.firstOrNull { it.id == id }?.name ?: "" },
                    onChange = { id ->
                        onLaunchLiveStreamSelectionChange(id)
                        onSelectedLaunchLiveStreamChange(id)
                        generate()
                    },
                )
            }
            qrCode?.let { code ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(formPalette().separator),
                )
                QrCodeImageView(image = code, height = height)
            }
        } else {
            Text(localized("Press the shortcut below to create launch live streams."))
        }
    }
}

@Composable
private fun QuickButtonGoProWifiCredentialsView(
    goProState: GoProState,
    goPro: SettingsGoPro,
    height: Double,
    onWifiCredentialsSelectionChange: (UUID?) -> Unit,
    onSelectedWifiCredentialsChange: (UUID?) -> Unit,
) {
    val wifiCredentialsSelection by goProState.wifiCredentialsSelection.collectAsState()
    val wifiCredentials = goPro.wifiCredentials
    var qrCode by remember { mutableStateOf<ImageBitmap?>(null) }
    var entries by remember { mutableStateOf<List<PickerEntry>>(emptyList()) }

    fun generate() {
        val selection = goProState.wifiCredentialsSelection.value
        val selected = wifiCredentials.firstOrNull { it.id == selection }
        qrCode = if (selected != null) {
            GoPro.generateWifiCredentialsQrCode(
                ssid = selected.ssid,
                password = selected.password,
            )?.asImageBitmap()
        } else {
            null
        }
    }

    LaunchedEffect(Unit) {
        entries = wifiCredentials.map { PickerEntry(id = it.id, name = it.name) }
        generate()
    }

    Column {
        if (wifiCredentialsSelection != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(localized("WiFi credentials"))
                Spacer(Modifier.weight(1f))
                Picker(
                    title = "",
                    selection = wifiCredentialsSelection!!,
                    options = entries.map { it.id },
                    text = { id -> entries.firstOrNull { it.id == id }?.name ?: "" },
                    onChange = { id ->
                        onWifiCredentialsSelectionChange(id)
                        onSelectedWifiCredentialsChange(id)
                        generate()
                    },
                )
            }
            qrCode?.let { code ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(formPalette().separator),
                )
                QrCodeImageView(image = code, height = height)
            }
        } else {
            Text(localized("Press the shortcut below to create WiFi credentials."))
        }
    }
}

@Composable
private fun QuickButtonGoProRtmpUrlView(
    goProState: GoProState,
    goPro: SettingsGoPro,
    height: Double,
    onRtmpUrlSelectionChange: (UUID?) -> Unit,
    onSelectedRtmpUrlChange: (UUID?) -> Unit,
) {
    val rtmpUrlSelection by goProState.rtmpUrlSelection.collectAsState()
    val rtmpUrls = goPro.rtmpUrls
    var qrCode by remember { mutableStateOf<ImageBitmap?>(null) }
    var entries by remember { mutableStateOf<List<PickerEntry>>(emptyList()) }

    fun generate() {
        val selection = goProState.rtmpUrlSelection.value
        val selected = rtmpUrls.firstOrNull { it.id == selection }
        if (selected != null) {
            when (selected.type) {
                SettingsDjiDeviceUrlType.server -> qrCode =
                    GoPro.generateRtmpUrlQrCode(url = selected.serverUrl)?.asImageBitmap()

                SettingsDjiDeviceUrlType.custom -> qrCode =
                    GoPro.generateRtmpUrlQrCode(url = selected.customUrl)?.asImageBitmap()

                else -> qrCode = null
            }
        } else {
            qrCode = null
        }
    }

    LaunchedEffect(Unit) {
        entries = rtmpUrls.map { PickerEntry(id = it.id, name = it.name) }
        generate()
    }

    Column {
        if (rtmpUrlSelection != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(localized("RTMP URL"))
                Spacer(Modifier.weight(1f))
                Picker(
                    title = "",
                    selection = rtmpUrlSelection!!,
                    options = entries.map { it.id },
                    text = { id -> entries.firstOrNull { it.id == id }?.name ?: "" },
                    onChange = { id ->
                        onRtmpUrlSelectionChange(id)
                        onSelectedRtmpUrlChange(id)
                        generate()
                    },
                )
            }
            qrCode?.let { code ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(formPalette().separator),
                )
                QrCodeImageView(image = code, height = height)
            }
        } else {
            Text(localized("Press the shortcut below to create RTMP URLs."))
        }
    }
}

@Composable
fun QuickButtonGoProView(
    model: Model = LocalModel.current,
    goProState: GoProState,
    goPro: SettingsGoPro,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val devices = goPro.devices
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val height = qrCodeHeight(maxHeight)
        Form(title = "GoPro") {
            if (devices.isNotEmpty()) {
                Section(header = localized("Devices")) {
                    devices.forEach { device ->
                        key(device.id) {
                            QuickButtonGoProBleDeviceView(model = model, device = device)
                        }
                    }
                }
            }
            Section {
                Column {
                    val pagerState = rememberPagerState(pageCount = { 3 })
                    val scope = rememberCoroutineScope()
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((height + 90.0).dp),
                    ) { page ->
                        when (page) {
                            0 -> QuickButtonGoProLaunchLiveStreamView(
                                goProState = goProState,
                                goPro = goPro,
                                height = height,
                                onLaunchLiveStreamSelectionChange = {
                                    goProState.launchLiveStreamSelection.value = it
                                },
                                onSelectedLaunchLiveStreamChange = {
                                    goPro.selectedLaunchLiveStream = it
                                },
                            )

                            1 -> QuickButtonGoProWifiCredentialsView(
                                goProState = goProState,
                                goPro = goPro,
                                height = height,
                                onWifiCredentialsSelectionChange = {
                                    goProState.wifiCredentialsSelection.value = it
                                },
                                onSelectedWifiCredentialsChange = {
                                    goPro.selectedWifiCredentials = it
                                },
                            )

                            else -> QuickButtonGoProRtmpUrlView(
                                goProState = goProState,
                                goPro = goPro,
                                height = height,
                                onRtmpUrlSelectionChange = {
                                    goProState.rtmpUrlSelection.value = it
                                },
                                onSelectedRtmpUrlChange = {
                                    goPro.selectedRtmpUrl = it
                                },
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        for (index in 0 until 3) {
                            SystemImage(
                                name = if (pagerState.currentPage == index) {
                                    "circle.fill"
                                } else {
                                    "circle"
                                },
                                fontSize = 10.sp,
                                modifier = Modifier
                                    .padding(bottom = 10.dp)
                                    .pointerInput(index) {
                                        detectTapGestures(
                                            onTap = {
                                                scope.launch {
                                                    pagerState.animateScrollToPage(index)
                                                }
                                            },
                                        )
                                    },
                            )
                        }
                    }
                }
            }
            ShortcutSectionView {
                NavigationLink(destination = { GoProSettingsView(model = model) }) {
                    Label(localized("GoPro"), systemImage = "appletvremote.gen1")
                }
            }
        }
    }
}

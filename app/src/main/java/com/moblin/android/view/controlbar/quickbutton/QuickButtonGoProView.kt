package com.moblin.android.view.controlbar.quickbutton

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.moblin.android.integrations.gopro.GoPro
import com.moblin.android.various.model.GoProState
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsDjiDeviceUrlType
import com.moblin.android.various.settings.SettingsGoPro
import com.moblin.android.various.settings.SettingsGoProDevice
import com.moblin.android.view.settings.gopro.formatGoProDeviceState
import com.moblin.android.view.settings.gopro.qrCodeHeight
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.QrCodeImageView
import com.moblin.android.view.utils.ShortcutSectionView
import java.util.UUID
import kotlinx.coroutines.launch
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

private data class PickerEntry(val id: UUID, val name: String)

@Composable
private fun QuickButtonGoProBleDeviceView(
    model: Model = LocalModel.current,
    device: SettingsGoProDevice,
) {
    val name = device.name
    val isStarted = device.isStarted
    val state = device.state
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name)
        Spacer(Modifier.weight(1f))
        GrayTextView(text = formatGoProDeviceState(state))
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = isStarted,
            onCheckedChange = { value ->
                if (value) {
                    Unit
                } else {
                    Unit
                }
            },
            enabled = device.canStartLive(model.statusOther.isConnectedToIpv4WiFi()),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    var expanded by remember { mutableStateOf(false) }
    var firstSelectionEffect by remember { mutableStateOf(true) }

    fun generate() {
        val selected = launchLiveStream.firstOrNull { it.id == launchLiveStreamSelection }
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
    LaunchedEffect(launchLiveStreamSelection) {
        if (firstSelectionEffect) {
            firstSelectionEffect = false
        } else {
            onSelectedLaunchLiveStreamChange(launchLiveStreamSelection)
            generate()
        }
    }

    Column {
        if (launchLiveStreamSelection != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Launch live stream")
                Spacer(Modifier.weight(1f))
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    modifier = Modifier.weight(1f),
                ) {
                    OutlinedTextField(
                        value = entries.firstOrNull { it.id == launchLiveStreamSelection }?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        entries.forEach { entry ->
                            DropdownMenuItem(
                                text = { Text(entry.name) },
                                onClick = {
                                    expanded = false
                                    onLaunchLiveStreamSelectionChange(entry.id)
                                    onSelectedLaunchLiveStreamChange(entry.id)
                                    generate()
                                },
                            )
                        }
                    }
                }
            }
            qrCode?.let { code ->
                HorizontalDivider()
                QrCodeImageView(image = code, height = height)
            }
        } else {
            Text("Press the shortcut below to create launch live streams.")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    var expanded by remember { mutableStateOf(false) }
    var firstSelectionEffect by remember { mutableStateOf(true) }

    fun generate() {
        val selected = wifiCredentials.firstOrNull { it.id == wifiCredentialsSelection }
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
    LaunchedEffect(wifiCredentialsSelection) {
        if (firstSelectionEffect) {
            firstSelectionEffect = false
        } else {
            onSelectedWifiCredentialsChange(wifiCredentialsSelection)
            generate()
        }
    }

    Column {
        if (wifiCredentialsSelection != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("WiFi credentials")
                Spacer(Modifier.weight(1f))
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    modifier = Modifier.weight(1f),
                ) {
                    OutlinedTextField(
                        value = entries.firstOrNull { it.id == wifiCredentialsSelection }?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        entries.forEach { entry ->
                            DropdownMenuItem(
                                text = { Text(entry.name) },
                                onClick = {
                                    expanded = false
                                    onWifiCredentialsSelectionChange(entry.id)
                                    onSelectedWifiCredentialsChange(entry.id)
                                    generate()
                                },
                            )
                        }
                    }
                }
            }
            qrCode?.let { code ->
                HorizontalDivider()
                QrCodeImageView(image = code, height = height)
            }
        } else {
            Text("Press the shortcut below to create WiFi credentials.")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    var expanded by remember { mutableStateOf(false) }
    var firstSelectionEffect by remember { mutableStateOf(true) }

    fun generate() {
        val selected = rtmpUrls.firstOrNull { it.id == rtmpUrlSelection }
        if (selected != null) {
            when (selected.type) {
                SettingsDjiDeviceUrlType.server -> qrCode = GoPro.generateRtmpUrlQrCode(url = selected.serverUrl)?.asImageBitmap()
                SettingsDjiDeviceUrlType.custom -> qrCode = GoPro.generateRtmpUrlQrCode(url = selected.customUrl)?.asImageBitmap()
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
    LaunchedEffect(rtmpUrlSelection) {
        if (firstSelectionEffect) {
            firstSelectionEffect = false
        } else {
            onSelectedRtmpUrlChange(rtmpUrlSelection)
            generate()
        }
    }

    Column {
        if (rtmpUrlSelection != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("RTMP URL")
                Spacer(Modifier.weight(1f))
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    modifier = Modifier.weight(1f),
                ) {
                    OutlinedTextField(
                        value = entries.firstOrNull { it.id == rtmpUrlSelection }?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        entries.forEach { entry ->
                            DropdownMenuItem(
                                text = { Text(entry.name) },
                                onClick = {
                                    expanded = false
                                    onRtmpUrlSelectionChange(entry.id)
                                    onSelectedRtmpUrlChange(entry.id)
                                    generate()
                                },
                            )
                        }
                    }
                }
            }
            qrCode?.let { code ->
                HorizontalDivider()
                QrCodeImageView(image = code, height = height)
            }
        } else {
            Text("Press the shortcut below to create RTMP URLs.")
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
    var activeIndex by remember { mutableStateOf<Int?>(0) }
    val devices = goPro.devices
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    BoxWithConstraints {
        val height = qrCodeHeight(maxHeight)
        LazyColumn {
            if (devices.isNotEmpty()) {
                item {
                    Text("Devices", style = MaterialTheme.typography.titleMedium)
                }
                items(devices, key = { it.id }) { device ->
                    QuickButtonGoProBleDeviceView(model = model, device = device)
                }
            }
            item {
                Column {
                    LaunchedEffect(pagerState.currentPage) {
                        activeIndex = pagerState.currentPage
                    }
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
                                onLaunchLiveStreamSelectionChange = { goProState.launchLiveStreamSelection.value = it },
                                onSelectedLaunchLiveStreamChange = { goPro.selectedLaunchLiveStream = it },
                            )
                            1 -> QuickButtonGoProWifiCredentialsView(
                                goProState = goProState,
                                goPro = goPro,
                                height = height,
                                onWifiCredentialsSelectionChange = { goProState.wifiCredentialsSelection.value = it },
                                onSelectedWifiCredentialsChange = { goPro.selectedWifiCredentials = it },
                            )
                            else -> QuickButtonGoProRtmpUrlView(
                                goProState = goProState,
                                goPro = goPro,
                                height = height,
                                onRtmpUrlSelectionChange = { goProState.rtmpUrlSelection.value = it },
                                onSelectedRtmpUrlChange = { goPro.selectedRtmpUrl = it },
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        for (index in 0 until 3) {
                            Box(
                                modifier = Modifier
                                    .padding(bottom = 10.dp, end = 4.dp)
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (activeIndex == index) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.outline
                                        }
                                    )
                                    .clickable {
                                        activeIndex = index
                                        scope.launch { pagerState.animateScrollToPage(index) }
                                    }
                            )
                        }
                    }
                }
            }
            item {
                ShortcutSectionView {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate("GoProSettingsView") }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Text("GoPro", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}

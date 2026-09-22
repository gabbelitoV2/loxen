package com.moblin.android.view.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FormatIndentIncrease
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsAppMode
import com.moblin.android.various.utils.isMac
import com.moblin.android.various.utils.isPad
import com.moblin.android.various.utils.isPhone
import com.moblin.android.view.settings.savereset.SettingsResetView
import com.moblin.android.view.settings.savereset.SettingsSaveView
import com.moblin.android.view.utils.InfoBannerView
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

val settingsHalfWidth = 350.0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppModeView(
    model: Model = LocalModel.current,
    database: Database,
) {
    val isLive by model.isLive.collectAsState()
    val isRecording by model.isRecording.collectAsState()
    val appMode by database.appMode.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    LaunchedEffect(appMode) {
        model.appModeChanged()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("App mode", modifier = Modifier.weight(1f))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = {
                if (!(isLive || isRecording)) {
                    expanded = it
                }
            },
            modifier = Modifier.weight(1f),
        ) {
            OutlinedTextField(
                value = appMode.toString(),
                onValueChange = {},
                readOnly = true,
                enabled = !(isLive || isRecording),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                for (mode in SettingsAppMode.entries) {
                    DropdownMenuItem(
                        text = { Text(mode.toString()) },
                        onClick = {
                            database.appMode.value = mode
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsView(
    model: Model = LocalModel.current,
    database: Database,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val isLive by model.isLive.collectAsState()
    val showAllSettings by database.showAllSettings.collectAsState()
    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues),
        ) {
            if (isLive) {
                item {
                    InfoBannerView(text = "Settings that would stop the stream are disabled when live.")
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("StreamsSettingsView") }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Wifi, contentDescription = null)
                    Spacer(Modifier.width(16.dp))
                    Text("Streams")
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("ScenesSettingsView") }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Photo, contentDescription = null)
                    Spacer(Modifier.width(16.dp))
                    Text("Scenes")
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("ChatSettingsView") }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null)
                    Spacer(Modifier.width(16.dp))
                    Text("Chat")
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("DisplaySettingsView") }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Smartphone, contentDescription = null)
                    Spacer(Modifier.width(16.dp))
                    Text("Display")
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("CameraSettingsView") }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null)
                    Spacer(Modifier.width(16.dp))
                    Text("Camera")
                }
            }
            if (showAllSettings) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate("AudioSettingsView") }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null)
                        Spacer(Modifier.width(16.dp))
                        Text("Audio")
                    }
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate("MacrosSettingsView") }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.FormatIndentIncrease, contentDescription = null)
                        Spacer(Modifier.width(16.dp))
                        Text("Macros")
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("LocationSettingsView") }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null)
                    Spacer(Modifier.width(16.dp))
                    Text("Location")
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("StoreSettingsView") }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null)
                    Spacer(Modifier.width(16.dp))
                    Text("Store (support us) ❤️")
                }
            }
            if (showAllSettings) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate("IngestsSettingsView") }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Dns, contentDescription = null)
                        Spacer(Modifier.width(16.dp))
                        Text("Ingests")
                    }
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate("TalkbackSettingsView") }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null)
                        Spacer(Modifier.width(16.dp))
                        Text("Talkback")
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("MoblinkSettingsView") }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Link, contentDescription = null)
                    Spacer(Modifier.width(16.dp))
                    Text("Moblink")
                }
            }
            if (showAllSettings) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate("MediaPlayersSettingsView") }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(16.dp))
                        Text("Media players")
                    }
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate("AppleMusicSettingsView") }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = null)
                        Spacer(Modifier.width(16.dp))
                        Text("Apple Music")
                    }
                }
            }
            if (showAllSettings) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate("GimbalSettingsView") }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.ScreenRotation, contentDescription = null)
                        Spacer(Modifier.width(16.dp))
                        Text("Gimbal")
                    }
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate("SelfieStickSettingsView") }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Straighten, contentDescription = null)
                        Spacer(Modifier.width(16.dp))
                        Text("Selfie stick")
                    }
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate("GameControllersSettingsView") }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.SportsEsports, contentDescription = null)
                        Spacer(Modifier.width(16.dp))
                        Text("Game controllers")
                    }
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate("KeyboardSettingsView") }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Keyboard, contentDescription = null)
                        Spacer(Modifier.width(16.dp))
                        Text("Keyboard")
                    }
                }
                if (isPad()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigate("StreamDecksSettingsView") }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.GridView, contentDescription = null)
                            Spacer(Modifier.width(16.dp))
                            Text("Stream decks")
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate("RemoteControlSettingsView") }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.SettingsRemote, contentDescription = null)
                        Spacer(Modifier.width(16.dp))
                        Text("Remote control")
                    }
                }
            }
            if (showAllSettings) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate("DjiDevicesSettingsView") }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.SettingsRemote, contentDescription = null)
                        Spacer(Modifier.width(16.dp))
                        Text("DJI devices")
                    }
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate("GoProSettingsView") }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.SettingsRemote, contentDescription = null)
                        Spacer(Modifier.width(16.dp))
                        Text("GoPro")
                    }
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate("CatPrintersSettingsView") }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Pets, contentDescription = null)
                        Spacer(Modifier.width(16.dp))
                        Text("Cat printers")
                    }
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate("TeslaSettingsView") }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.DirectionsCar, contentDescription = null)
                        Spacer(Modifier.width(16.dp))
                        Text("Tesla")
                    }
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate("WorkoutDevicesSettingsView") }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.DirectionsWalk, contentDescription = null)
                        Spacer(Modifier.width(16.dp))
                        Text("Workout devices")
                    }
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate("BlackSharkCoolerDevicesSettingsView") }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Air, contentDescription = null)
                        Spacer(Modifier.width(16.dp))
                        Text("Black Shark coolers")
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("RecordingsSettingsView") }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.width(16.dp))
                    Text("Recordings")
                }
            }
            if (showAllSettings) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate("StreamingHistorySettingsView") }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.MenuBook, contentDescription = null)
                        Spacer(Modifier.width(16.dp))
                        Text("Streaming history")
                    }
                }
            }
            if (showAllSettings && isPhone()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate("WatchSettingsView") }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Watch, contentDescription = null)
                        Spacer(Modifier.width(16.dp))
                        Text("Apple Watch")
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("HelpAndSupportSettingsView") }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.HelpOutline, contentDescription = null)
                    Spacer(Modifier.width(16.dp))
                    Text("Help and support")
                }
            }
            if (showAllSettings) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate("AboutSettingsView") }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null)
                        Spacer(Modifier.width(16.dp))
                        Text("About")
                    }
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate("DebugSettingsView") }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.BugReport, contentDescription = null)
                        Spacer(Modifier.width(16.dp))
                        Text("Debug")
                    }
                }
            }
            if (showAllSettings) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate("ImportExportSettingsView") }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Spacer(Modifier.width(16.dp))
                        Text("Import and export settings")
                    }
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate("DeepLinkCreatorSettingsView") }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.AddLink, contentDescription = null)
                        Spacer(Modifier.width(16.dp))
                        Text("Deep link creator")
                    }
                }
            }
            if (showAllSettings && !isMac()) {
                item {
                    AppModeView(model = model, database = database)
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Show all settings", modifier = Modifier.weight(1f))
                    Switch(
                        checked = showAllSettings,
                        onCheckedChange = { database.showAllSettings.value = it },
                    )
                }
            }
            item {
                SettingsSaveView(model = model)
            }
            item {
                SettingsResetView()
            }
        }
    }
}

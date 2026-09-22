package com.moblin.android.view.settings.location

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import com.moblin.android.localized
import com.moblin.android.various.managers.Location
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.fallbackStream
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsLocation
import com.moblin.android.various.settings.SettingsLocationDesiredAccuracy
import com.moblin.android.various.settings.SettingsLocationDistanceFilter
import com.moblin.android.various.settings.SettingsPrivacyRegion
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.various.utils.makeOffsets
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.ShortcutSectionView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.view.utils.TextButtonView
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

private data class CLLocationCoordinate2D(
    val latitude: Double,
    val longitude: Double,
)

private data class MKCoordinateSpan(
    val latitudeDelta: Double,
    val longitudeDelta: Double,
)

private data class MKCoordinateRegion(
    val center: CLLocationCoordinate2D,
    val span: MKCoordinateSpan,
)

@Composable
private fun PrivacyRegionView(
    model: Model = LocalModel.current,
    region: SettingsPrivacyRegion,
    current: MKCoordinateRegion,
) {
    val currentRegion = remember { mutableStateOf(current) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f),
    ) {
        TODO("no Android counterpart for MapKit")
    }
    LaunchedEffect(currentRegion.value) {
        region.latitude = currentRegion.value.center.latitude
        region.longitude = currentRegion.value.center.longitude
        region.latitudeDelta = currentRegion.value.span.latitudeDelta
        region.longitudeDelta = currentRegion.value.span.longitudeDelta
    }
    DisposableEffect(Unit) {
        onDispose {
            TODO("reloadLocation")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LocationSettingsView(
    model: Model = LocalModel.current,
    database: Database,
    location: SettingsLocation,
    locationManager: Location,
    stream: SettingsStream,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val showAllSettings = database.showAllSettings
    val isDenied by locationManager.isDenied.collectAsState()
    val enabled by location.enabledFlow.collectAsState()
    val desiredAccuracy by location.desiredAccuracyFlow.collectAsState()
    val distanceFilter by location.distanceFilterFlow.collectAsState()
    val resetWhenGoingLive by location.resetWhenGoingLiveFlow.collectAsState()
    val privacyRegions by location.privacyRegionsFlow.collectAsState()
    val realtimeIrlEnabled = stream.realtimeIrlEnabled

    var desiredAccuracyExpanded by remember { mutableStateOf(false) }
    var distanceFilterExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(enabled) {
        TODO("reloadLocation")
    }
    LaunchedEffect(desiredAccuracy) {
        TODO("reloadLocation")
    }
    LaunchedEffect(distanceFilter) {
        TODO("reloadLocation")
    }
    LaunchedEffect(realtimeIrlEnabled) {
        TODO("reloadLocation")
    }

    fun deletePrivacyRegion(offsets: List<Int>) {
        val regions = location.privacyRegions.toMutableList()
        offsets.sortedDescending().forEach { index ->
            if (index in regions.indices) {
                regions.removeAt(index)
            }
        }
        location.privacyRegions = regions
        TODO("reloadLocation")
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Location") })
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Enabled", modifier = Modifier.weight(1f))
                    Switch(
                        checked = enabled,
                        onCheckedChange = {
                            location.enabled = it
                        },
                    )
                }
            }
            if (enabled && isDenied) {
                item {
                    Text("⚠️ Allow Moblin to access your location in iOS Settings to use location.")
                }
            }
            if (showAllSettings) {
                item {
                    ExposedDropdownMenuBox(
                        expanded = desiredAccuracyExpanded,
                        onExpandedChange = { desiredAccuracyExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = desiredAccuracy.toString(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Desired accuracy") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = desiredAccuracyExpanded,
                                )
                            },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = desiredAccuracyExpanded,
                            onDismissRequest = { desiredAccuracyExpanded = false },
                        ) {
                            SettingsLocationDesiredAccuracy.entries.forEach { accuracy ->
                                DropdownMenuItem(
                                    text = { Text(accuracy.toString()) },
                                    onClick = {
                                        location.desiredAccuracy = accuracy
                                        desiredAccuracyExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
                item {
                    ExposedDropdownMenuBox(
                        expanded = distanceFilterExpanded,
                        onExpandedChange = { distanceFilterExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = distanceFilter.toString(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Distance filter") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = distanceFilterExpanded,
                                )
                            },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = distanceFilterExpanded,
                            onDismissRequest = { distanceFilterExpanded = false },
                        ) {
                            SettingsLocationDistanceFilter.entries.forEach { filter ->
                                DropdownMenuItem(
                                    text = { Text(filter.toString()) },
                                    onClick = {
                                        location.distanceFilter = filter
                                        distanceFilterExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
            item {
                Text("Location data", style = MaterialTheme.typography.titleSmall)
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Reset when going live", modifier = Modifier.weight(1f))
                    Switch(
                        checked = resetWhenGoingLive,
                        onCheckedChange = {
                            location.resetWhenGoingLive = it
                        },
                    )
                }
            }
            item {
                TextButtonView("Split") {
                    TODO("resetSplitLocationData")
                }
            }
            item {
                TextButtonView("Reset") {
                    TODO("resetLocationData")
                }
            }
            item {
                Text(
                    "Resets distances, average speed and slope.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (showAllSettings && stream !== fallbackStream) {
                item {
                    ShortcutSectionView {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        onNavigate("StreamRealtimeIrlSettingsView")
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null)
                                Text("RealtimeIRL")
                            }
                            Switch(
                                checked = realtimeIrlEnabled,
                                onCheckedChange = {
                                    stream.realtimeIrlEnabled = it
                                },
                            )
                        }
                    }
                }
            }
            item {
                Text("Privacy regions", style = MaterialTheme.typography.titleSmall)
            }
            items(items = privacyRegions, key = { it.id }) { region ->
                var regionMenuExpanded by remember { mutableStateOf(false) }
                val latitude = region.latitude
                val longitude = region.longitude
                val latitudeDelta = region.latitudeDelta
                val longitudeDelta = region.longitudeDelta
                Box {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {},
                                onLongClick = { regionMenuExpanded = true },
                            ),
                    ) {
                        PrivacyRegionView(
                            model = model,
                            region = region,
                            current = MKCoordinateRegion(
                                center = CLLocationCoordinate2D(
                                    latitude = latitude,
                                    longitude = longitude,
                                ),
                                span = MKCoordinateSpan(
                                    latitudeDelta = latitudeDelta,
                                    longitudeDelta = longitudeDelta,
                                ),
                            ),
                        )
                    }
                    DropdownMenu(
                        expanded = regionMenuExpanded,
                        onDismissRequest = { regionMenuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                regionMenuExpanded = false
                                val index = location.privacyRegions.indexOfFirst { it.id == region.id }
                                if (index != -1) {
                                    deletePrivacyRegion(listOf(index))
                                }
                            },
                        )
                    }
                }
            }
            item {
                CreateButtonView {
                    val privacyRegion = SettingsPrivacyRegion()
                    locationManager.getLatestKnownLocation()?.let { latest ->
                        privacyRegion.latitude = latest.latitude
                        privacyRegion.longitude = latest.longitude
                        privacyRegion.latitudeDelta = 0.02
                        privacyRegion.longitudeDelta = 0.02
                    }
                    location.privacyRegions = location.privacyRegions + privacyRegion
                    TODO("reloadLocation")
                }
            }
            item {
                Column {
                    Text("Your location will not be shared with any service when within any privacy region.")
                    Text("")
                    SwipeLeftToDeleteHelpView(kind = localized("a privacy region"))
                }
            }
        }
    }
}

package com.moblin.android.view.settings.location

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.ForEach
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.IosSwitch
import com.moblin.android.platform.swiftui.Label
import com.moblin.android.platform.swiftui.LocalTint
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.managers.Location
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.fallbackStream
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsLocation
import com.moblin.android.various.settings.SettingsLocationDesiredAccuracy
import com.moblin.android.various.settings.SettingsLocationDistanceFilter
import com.moblin.android.various.settings.SettingsPrivacyRegion
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.view.utils.ContextMenuDeleteButton
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.ShortcutSectionView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.view.utils.TextButtonView
import com.moblin.android.view.settings.streams.stream.realtimeirl.StreamRealtimeIrlSettingsView
import com.moblin.android.various.model.reloadLocation
import com.moblin.android.various.model.resetLocationData
import com.moblin.android.various.model.resetSplitLocationData

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
    )
    LaunchedEffect(currentRegion.value) {
        region.latitude = currentRegion.value.center.latitude
        region.longitude = currentRegion.value.center.longitude
        region.latitudeDelta = currentRegion.value.span.latitudeDelta
        region.longitudeDelta = currentRegion.value.span.longitudeDelta
    }
    DisposableEffect(Unit) {
        onDispose {
            model.reloadLocation()
        }
    }
}

@Composable
fun LocationSettingsView(
    model: Model = LocalModel.current,
    database: Database,
    location: SettingsLocation,
    locationManager: Location,
    stream: SettingsStream,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val palette = formPalette()
    val isDenied by locationManager.isDenied.collectAsState()
    val enabled by location.enabledFlow.collectAsState()
    val desiredAccuracy by location.desiredAccuracyFlow.collectAsState()
    val distanceFilter by location.distanceFilterFlow.collectAsState()
    val resetWhenGoingLive by location.resetWhenGoingLiveFlow.collectAsState()
    val privacyRegions by location.privacyRegionsFlow.collectAsState()
    val showAllSettings = database.showAllSettings
    val realtimeIrlEnabled = stream.realtimeIrlEnabled

    fun deletePrivacyRegion(offsets: List<Int>) {
        val regions = location.privacyRegions.toMutableList()
        offsets.sortedDescending().forEach { index ->
            if (index in regions.indices) {
                regions.removeAt(index)
            }
        }
        location.privacyRegions = regions
        model.reloadLocation()
    }

    Form(title = localized("Location")) {
        Section {
            Toggle(
                title = localized("Enabled"),
                isOn = enabled,
                onChange = {
                    location.enabled = it
                    model.reloadLocation()
                },
            )
        }
        if (enabled && isDenied) {
            Section {
                Text(localized("⚠️ Allow Moblin to access your location in iOS Settings to use location."))
            }
        }
        if (showAllSettings) {
            Section {
                Picker(
                    title = localized("Desired accuracy"),
                    selection = desiredAccuracy,
                    options = SettingsLocationDesiredAccuracy.entries,
                    onChange = {
                        location.desiredAccuracy = it
                        model.reloadLocation()
                    },
                )
                Picker(
                    title = localized("Distance filter"),
                    selection = distanceFilter,
                    options = SettingsLocationDistanceFilter.entries,
                    onChange = {
                        location.distanceFilter = it
                        model.reloadLocation()
                    },
                )
            }
        }
        Section(
            header = localized("Location data"),
            footer = localized("Resets distances, average speed and slope."),
        ) {
            Toggle(
                title = localized("Reset when going live"),
                isOn = resetWhenGoingLive,
                onChange = {
                    location.resetWhenGoingLive = it
                },
            )
            TextButtonView(localized("Split")) {
                model.resetSplitLocationData()
            }
            CompositionLocalProvider(LocalTint provides palette.red) {
                TextButtonView(localized("Reset")) {
                    model.resetLocationData()
                }
            }
        }
        if (showAllSettings && stream !== fallbackStream) {
            ShortcutSectionView {
                NavigationLink(destination = {
                    StreamRealtimeIrlSettingsView(model = model, stream = stream)
                }) {
                    Label(
                        "RealtimeIRL",
                        systemImage = "dot.radiowaves.left.and.right",
                        modifier = Modifier.weight(1f),
                    )
                    IosSwitch(
                        checked = realtimeIrlEnabled,
                        onCheckedChange = {
                            stream.realtimeIrlEnabled = it
                            model.reloadLocation()
                        },
                    )
                }
            }
        }
        Section(
            header = localized("Privacy regions"),
            footerContent = {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        localized(
                            "Your location will not be shared with any service when within any privacy region.",
                        ),
                    )
                    Text("")
                    SwipeLeftToDeleteHelpView(kind = localized("a privacy region"))
                }
            },
        ) {
            ForEach(
                privacyRegions,
                id = { it.id },
                onDelete = { deletePrivacyRegion(it.toList()) },
            ) { region ->
                val latitude = region.latitude
                val longitude = region.longitude
                val latitudeDelta = region.latitudeDelta
                val longitudeDelta = region.longitudeDelta
                ContextMenuDeleteButton(action = {
                    val index = location.privacyRegions.indexOfFirst { it.id == region.id }
                    if (index != -1) {
                        deletePrivacyRegion(listOf(index))
                    }
                }) {
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
            }
            CreateButtonView {
                val privacyRegion = SettingsPrivacyRegion()
                locationManager.getLatestKnownLocation()?.let { latest ->
                    privacyRegion.latitude = latest.latitude
                    privacyRegion.longitude = latest.longitude
                    privacyRegion.latitudeDelta = 0.02
                    privacyRegion.longitudeDelta = 0.02
                }
                location.privacyRegions = location.privacyRegions + privacyRegion
                model.reloadLocation()
            }
        }
    }
}

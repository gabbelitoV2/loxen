package com.moblin.android.view.stream.overlay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.common.various.format
import com.moblin.android.localized
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.mapkit.CoordinateSpace
import com.moblin.android.platform.mapkit.MKLocalSearch
import com.moblin.android.platform.mapkit.MKMapItem
import com.moblin.android.platform.mapkit.MKMapItemRequest
import com.moblin.android.platform.mapkit.Map
import com.moblin.android.platform.mapkit.MapInteractionModes
import com.moblin.android.platform.mapkit.MapPolyline
import com.moblin.android.platform.mapkit.MapReader
import com.moblin.android.platform.mapkit.Marker
import com.moblin.android.platform.mapkit.PlaceDescriptor
import com.moblin.android.platform.mapkit.UserAnnotation
import com.moblin.android.platform.mapkit.coordinate
import com.moblin.android.platform.swiftui.Button
import com.moblin.android.platform.swiftui.LocalTint
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.PickerStyle
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.platform.swiftui.glassEffect
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.Navigation
import com.moblin.android.various.model.NavigationTransportType
import com.moblin.android.various.model.getLatestKnownLocation
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsNavigation
import com.moblin.android.various.utils.CLLocationCoordinate2D
import com.moblin.android.various.utils.MKCoordinateRegion
import com.moblin.android.various.utils.MKCoordinateSpan
import com.moblin.android.view.stream.overlay.right.segmentHeight
import com.moblin.android.view.stream.overlay.right.segmentHeightBig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import com.moblin.android.LocalModel
import androidx.compose.foundation.layout.fillMaxWidth

private val smallMapSide = 200.0
private val maximumBigMapSide = 600.0

@Composable
private fun ImageLocationView(slash: Boolean) {
    SystemImage(
        name = if (slash) "location.slash" else "location",
        fontSize = 17.sp,
        modifier = Modifier.wrapContentSize(unbounded = true).offset(x = (-8).dp),
        tint = formPalette().label,
    )
}

@Composable
private fun ImageConeView(slash: Boolean) {
    Box(
        modifier = Modifier.wrapContentSize(unbounded = true).offset(x = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        SystemImage(
            name = "cone",
            fontSize = 17.sp,
            modifier = Modifier.rotate(180f).scale(0.9f),
            tint = formPalette().label,
        )
        if (slash) {
            SystemImage(
                name = "line.diagonal",
                fontSize = 17.sp,
                modifier = Modifier.rotate(90f),
                tint = formPalette().label,
            )
        }
    }
}

private fun search(navigation: Navigation, text: String) {
    val cameraRegion = navigation.cameraRegion ?: return
    if (text.isEmpty()) {
        return
    }
    val searchRequest = MKLocalSearch.Request()
    searchRequest.naturalLanguageQuery = text
    searchRequest.region = cameraRegion
    val search = MKLocalSearch(request = searchRequest)
    search.start { response, _ ->
        if (response == null) {
            return@start
        }
        navigation.searchResults.value = response.mapItems
    }
}

@Composable
private fun ControlSearchView(navigation: Navigation) {
    val isSmall by navigation.isSmall.collectAsState()
    val searchText by navigation.searchText.collectAsState()
    val transportType by navigation.transportType.collectAsState()
    if (!isSmall) {
        BasicTextField(
            value = searchText,
            onValueChange = { value ->
                navigation.searchText.value = value
                if (value.isEmpty()) {
                    navigation.searchResults.value = emptyList()
                }
            },
            singleLine = true,
            textStyle = TextStyle(fontSize = 17.sp, lineHeight = 20.sp, color = formPalette().label),
            cursorBrush = SolidColor(formPalette().accent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                search(navigation = navigation, text = navigation.searchText.value)
            }),
            modifier = Modifier
                .glassEffect()
                .padding(12.dp)
                .widthIn(max = 300.dp)
                .fillMaxWidth()
                .heightIn(max = 20.dp),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (searchText.isEmpty()) {
                        Text(
                            localized("What are you looking for?"),
                            style = TextStyle(
                                fontSize = 17.sp,
                                lineHeight = 20.sp,
                                color = formPalette().secondaryLabel,
                            ),
                        )
                    }
                    inner()
                }
            },
        )
        Box(
            modifier = Modifier.glassEffect().padding(16.dp).size(35.dp, 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(modifier = Modifier.wrapContentSize(unbounded = true)) {
                CompositionLocalProvider(LocalTint provides formPalette().label) {
                    Picker(
                        title = "",
                        selection = transportType,
                        options = NavigationTransportType.entries,
                        systemImage = { it.image() },
                        pickerStyle = PickerStyle.menu,
                        onChange = { value ->
                            navigation.transportType.value = value
                            navigation.updateDirections()
                        },
                    )
                }
            }
        }
    }
}

private fun minMaxButtonIcon(isSmall: Boolean): String =
    if (isSmall) "arrow.up.left.and.arrow.down.right" else "arrow.down.right.and.arrow.up.left"

private fun shouldStackVertically(metrics: CGSize): Boolean = metrics.width < maximumBigMapSide

@Composable
private fun ControlsView(navigationSettings: SettingsNavigation, navigation: Navigation, metrics: CGSize) {
    val isSmall by navigation.isSmall.collectAsState()
    val followUser by navigationSettings.followUser.collectAsState()
    val followHeading by navigationSettings.followHeading.collectAsState()
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Spacer(Modifier.weight(1f))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Spacer(Modifier.weight(1f))
            if (shouldStackVertically(metrics = metrics)) {
                Row(
                    modifier = Modifier.padding(end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ControlSearchView(navigation = navigation)
                }
            }
        }
        Row(
            modifier = Modifier.padding(bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Spacer(Modifier.weight(1f))
            if (!shouldStackVertically(metrics = metrics)) {
                ControlSearchView(navigation = navigation)
            }
            Button(
                action = {
                    if (followUser && followHeading) {
                        (navigationSettings.followUser as MutableStateFlow<Boolean>).value = false
                        (navigationSettings.followHeading as MutableStateFlow<Boolean>).value = false
                    } else if (!followUser && !followHeading) {
                        (navigationSettings.followUser as MutableStateFlow<Boolean>).value = true
                    } else if (followUser && !followHeading) {
                        (navigationSettings.followHeading as MutableStateFlow<Boolean>).value = true
                    }
                },
            ) {
                Box(
                    modifier = Modifier.glassEffect().padding(16.dp).size(12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier.wrapContentSize(unbounded = true),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (followUser && followHeading) {
                            ImageLocationView(slash = false)
                            ImageConeView(slash = false)
                        } else if (!followUser && !followHeading) {
                            ImageLocationView(slash = true)
                            ImageConeView(slash = true)
                        } else if (followUser && !followHeading) {
                            ImageLocationView(slash = false)
                            ImageConeView(slash = true)
                        }
                    }
                }
            }
            Button(
                action = {
                    navigation.isSmall.value = !navigation.isSmall.value
                },
            ) {
                Box(
                    modifier = Modifier.padding(end = 10.dp).glassEffect().padding(16.dp).size(12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    SystemImage(
                        name = minMaxButtonIcon(isSmall = isSmall),
                        fontSize = 17.sp,
                        modifier = Modifier.wrapContentSize(unbounded = true),
                        tint = formPalette().label,
                    )
                }
            }
        }
    }
}

@Composable
private fun MarkerLabel(navigation: Navigation, item: MKMapItem) {
    val route by navigation.route.collectAsState()
    val destination by navigation.destination.collectAsState()
    val name = item.name
    if (name != null) {
        val expectedTravelTime = route?.expectedTravelTime
        if (expectedTravelTime != null && item == destination) {
            Text("$name (${expectedTravelTime.toDuration(DurationUnit.SECONDS).format()})")
        } else {
            Text(name)
        }
    }
}

private fun serLongPressLocation(navigation: Navigation, coordinate: CLLocationCoordinate2D, scope: CoroutineScope) {
    val placeDescriptor = PlaceDescriptor(
        representations = listOf(PlaceDescriptor.PlaceRepresentation.coordinate(coordinate)),
        commonName = null,
    )
    val request = MKMapItemRequest(placeDescriptor = placeDescriptor)
    scope.launch {
        navigation.longPressLocation.value = runCatching { request.mapItem() }.getOrNull()
        navigation.destination.value = null
    }
}

private fun mapSide(maximum: Double, isSmall: Boolean): Double =
    minOf(maximum - 10, if (isSmall) smallMapSide else maximumBigMapSide)

@Composable
private fun MapView(model: Model = LocalModel.current, navigationSettings: SettingsNavigation, navigation: Navigation, metrics: CGSize) {
    val scope = rememberCoroutineScope()
    val cameraPosition by navigation.cameraPosition.collectAsState()
    val destination by navigation.destination.collectAsState()
    val longPressLocation by navigation.longPressLocation.collectAsState()
    val searchResults by navigation.searchResults.collectAsState()
    val route by navigation.route.collectAsState()
    val isSmall by navigation.isSmall.collectAsState()
    val followUser by navigationSettings.followUser.collectAsState()
    val followHeading by navigationSettings.followHeading.collectAsState()
    val width = mapSide(maximum = metrics.width, isSmall = isSmall)
    val height = mapSide(maximum = metrics.height, isSmall = isSmall)
    MapReader { proxy ->
        Map(
            position = cameraPosition,
            onPositionChange = { navigation.cameraPosition.value = it },
            selection = destination,
            onSelectionChange = { value ->
                navigation.destination.value = value
                navigation.updateDirections()
            },
            modifier = Modifier
                .padding(end = 3.dp)
                .clip(RoundedCornerShape(7.dp))
                .widthIn(max = width.dp)
                .heightIn(max = height.dp),
            interactionModes = setOf(
                MapInteractionModes.pan,
                MapInteractionModes.rotate,
                MapInteractionModes.zoom,
            ),
            onMapCameraChange = { context ->
                navigation.cameraRegion = context.region
                if (followUser) {
                    navigation.timer.startSingleShot(timeout = 5.0) {
                        navigation.updateCameraPosition(settings = navigationSettings)
                    }
                }
            },
            onLongPress = { location ->
                proxy.convert(location, from = CoordinateSpace.local)?.let { coordinate ->
                    serLongPressLocation(navigation = navigation, coordinate = coordinate, scope = scope)
                }
            },
        ) {
            UserAnnotation()
            longPressLocation?.let { item ->
                Marker(coordinate = item.location.coordinate, tag = item) {
                    MarkerLabel(navigation = navigation, item = item)
                }
            }
            searchResults.forEach { searchResult ->
                key(searchResult) {
                    Marker(coordinate = searchResult.location.coordinate, tag = searchResult) {
                        MarkerLabel(navigation = navigation, item = searchResult)
                    }
                }
            }
            route?.let {
                MapPolyline(it, stroke = Color(0xFF007AFF), lineWidth = 5.0)
            }
        }
    }
    LaunchedEffect(followUser, followHeading) {
        navigation.updateCameraPosition(settings = navigationSettings)
    }
    LaunchedEffect(Unit) {
        if (navigation.cameraRegion != null) {
            navigation.updateCameraPosition(settings = navigationSettings)
        } else {
            val location = model.getLatestKnownLocation()
            if (location != null) {
                val (latitude, longitude) = location
                val center = CLLocationCoordinate2D(latitude = latitude, longitude = longitude)
                val span = MKCoordinateSpan(latitudeDelta = 0.1, longitudeDelta = 0.1)
                navigation.updateCameraPosition(
                    settings = navigationSettings,
                    region = MKCoordinateRegion(center = center, span = span),
                )
            }
        }
    }
}

private fun offset(database: Database, isSmall: Boolean, metrics: CGSize): Double {
    val offset: Double = if (database.bigButtons) {
        -(2 * segmentHeightBig + 10)
    } else {
        -(2 * segmentHeight + 10)
    }
    return if (isSmall || metrics.height - offset > maximumBigMapSide) {
        offset
    } else {
        0.0
    }
}

@Composable
fun StreamOverlayNavigationView(model: Model = LocalModel.current, database: Database, navigation: Navigation) {
    val isSmall by navigation.isSmall.collectAsState()
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val metrics = CGSize(maxWidth.value.toDouble(), maxHeight.value.toDouble())
        val yOffset = offset(database = database, isSmall = isSmall, metrics = metrics)
        Box(modifier = Modifier.offset(y = yOffset.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Spacer(Modifier.weight(1f))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Spacer(Modifier.weight(1f))
                    MapView(
                        model = model,
                        navigationSettings = model.database.navigation,
                        navigation = navigation,
                        metrics = metrics,
                    )
                }
            }
            ControlsView(
                navigationSettings = model.database.navigation,
                navigation = navigation,
                metrics = metrics,
            )
        }
    }
}

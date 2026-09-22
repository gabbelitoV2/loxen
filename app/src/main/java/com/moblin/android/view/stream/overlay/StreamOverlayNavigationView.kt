package com.moblin.android.view.stream.overlay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.Navigation
import com.moblin.android.various.model.NavigationTransportType
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsNavigation
import com.moblin.android.view.stream.overlay.right.segmentHeight
import com.moblin.android.view.stream.overlay.right.segmentHeightBig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.min
import com.moblin.android.LocalModel

private const val smallMapSide = 200.0
private const val maximumBigMapSide = 600.0

@Composable
private fun ImageLocationView(slash: Boolean) {
    Icon(
        imageVector = if (slash) Icons.Default.LocationOff else Icons.Default.LocationOn,
        contentDescription = null,
        modifier = Modifier.offset(x = (-8).dp, y = 0.dp)
    )
}

@Composable
private fun ImageConeView(slash: Boolean) {
    Box(modifier = Modifier.offset(x = 8.dp, y = 0.dp)) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.scale(0.9f).rotate(180f)
        )
        if (slash) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                modifier = Modifier.rotate(90f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ControlSearchView(navigation: Navigation, modifier: Modifier = Modifier) {
    fun search(text: String) {
        TODO("no Android counterpart for MapKit")
    }

    val isSmall by navigation.isSmall.collectAsState()
    val searchText by navigation.searchText.collectAsState()
    val transportType by navigation.transportType.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    if (!isSmall) {
        Row(modifier = modifier) {
            OutlinedTextField(
                value = searchText,
                onValueChange = { navigation.searchText.value = it },
                singleLine = true,
                placeholder = { Text("What are you looking for?") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { search(searchText) }),
                modifier = Modifier
                    .width(300.dp)
                    .height(20.dp)
                    .padding(12.dp)
                    .then(TODO("no Android counterpart for glassEffect"))
            )
            LaunchedEffect(searchText) {
                if (searchText.isEmpty()) {
                    navigation.searchResults.value = mutableListOf()
                }
            }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier
                    .width(35.dp)
                    .height(12.dp)
                    .padding(8.dp)
                    .then(TODO("no Android counterpart for glassEffect"))
            ) {
                OutlinedTextField(
                    value = transportType.name,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    NavigationTransportType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.name) },
                            leadingIcon = {
                                Icon(
                                    imageVector = TODO("map NavigationTransportType to a Material icon"),
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                navigation.transportType.value = type
                                expanded = false
                            }
                        )
                    }
                }
            }
            LaunchedEffect(transportType) {
                navigation.updateDirections()
            }
        }
    }
}

@Composable
private fun ControlsView(
    navigationSettings: SettingsNavigation,
    navigation: Navigation,
    maxWidth: Dp
) {
    fun minMaxButtonIcon(): ImageVector =
        if (navigation.isSmall.value) Icons.Default.OpenInFull else Icons.Default.CloseFullscreen

    fun shouldStackVertically(): Boolean = maxWidth < maximumBigMapSide.dp

    val followUser by navigationSettings.followUser.collectAsState()
    val followHeading by navigationSettings.followHeading.collectAsState()

    Column {
        Spacer(Modifier.weight(1f))
        Row {
            Spacer(Modifier.weight(1f))
            if (shouldStackVertically()) {
                ControlSearchView(
                    navigation = navigation,
                    modifier = Modifier.padding(end = 10.dp)
                )
            }
        }
        Row(modifier = Modifier.padding(bottom = 7.dp)) {
            Spacer(Modifier.weight(1f))
            if (!shouldStackVertically()) {
                ControlSearchView(navigation = navigation)
            }
            Button(
                onClick = {
                    val followUserState = navigationSettings.followUser as MutableStateFlow<Boolean>
                    val followHeadingState = navigationSettings.followHeading as MutableStateFlow<Boolean>
                    if (followUser && followHeading) {
                        followUserState.value = false
                        followHeadingState.value = false
                    } else if (!followUser && !followHeading) {
                        followUserState.value = true
                    } else if (followUser && !followHeading) {
                        followHeadingState.value = true
                    }
                },
                modifier = Modifier.then(TODO("no Android counterpart for glassEffect")),
                contentPadding = PaddingValues(8.dp)
            ) {
                Box(modifier = Modifier.size(12.dp)) {
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
            Button(
                onClick = { navigation.isSmall.value = !navigation.isSmall.value },
                modifier = Modifier
                    .padding(end = 10.dp)
                    .then(TODO("no Android counterpart for glassEffect")),
                contentPadding = PaddingValues(8.dp)
            ) {
                Icon(
                    imageVector = minMaxButtonIcon(),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun MarkerLabel(navigation: Navigation, item: Any) {
    TODO("no Android counterpart for MapKit")
}

@Composable
private fun MapView(
    model: Model = LocalModel.current,
    navigationSettings: SettingsNavigation,
    navigation: Navigation,
    maxWidth: Dp,
    maxHeight: Dp
) {
    fun serLongPressLocation(latitude: Double, longitude: Double) {
        TODO("no Android counterpart for MapKit")
    }

    fun mapSide(maximum: Double): Double =
        min(maximum - 10, if (navigation.isSmall.value) smallMapSide else maximumBigMapSide)

    TODO("no Android counterpart for MapKit")
}

@Composable
fun StreamOverlayNavigationView(
    model: Model = LocalModel.current,
    database: Database,
    navigation: Navigation
) {
    fun offset(height: Dp): Dp {
        val offset: Dp = if (database.bigButtons) {
            -(2 * segmentHeightBig + 10).dp
        } else {
            -(2 * segmentHeight + 10).dp
        }
        return if (navigation.isSmall.value || height - offset > maximumBigMapSide.dp) {
            offset
        } else {
            0.dp
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val maxWidth = this.maxWidth
        val maxHeight = this.maxHeight
        Box(modifier = Modifier.offset(y = offset(maxHeight))) {
            Row(modifier = Modifier.fillMaxSize()) {
                Spacer(Modifier.weight(1f))
                Column {
                    Spacer(Modifier.weight(1f))
                    MapView(
                        model = model,
                        navigationSettings = model.database.navigation,
                        navigation = navigation,
                        maxWidth = maxWidth,
                        maxHeight = maxHeight
                    )
                }
            }
            ControlsView(
                navigationSettings = model.database.navigation,
                navigation = navigation,
                maxWidth = maxWidth
            )
        }
    }
}

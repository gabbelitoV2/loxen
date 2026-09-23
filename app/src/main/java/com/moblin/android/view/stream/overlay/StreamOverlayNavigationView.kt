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
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.moblin.android.platform.swiftui.LocalTint
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.PickerStyle
import com.moblin.android.platform.swiftui.formPalette
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
        Unit
    }

    val isSmall by navigation.isSmall.collectAsState()
    val searchText by navigation.searchText.collectAsState()
    val transportType by navigation.transportType.collectAsState()

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
                    .then(Modifier)
            )
            LaunchedEffect(searchText) {
                if (searchText.isEmpty()) {
                    navigation.searchResults.value = mutableListOf()
                }
            }
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .size(width = 35.dp, height = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.wrapContentSize(unbounded = true)) {
                    CompositionLocalProvider(LocalTint provides formPalette().label) {
                        Picker(
                            "",
                            selection = transportType,
                            options = NavigationTransportType.entries,
                            text = { "" },
                            systemImage = { it.image() },
                            pickerStyle = PickerStyle.menu
                        ) {
                            navigation.transportType.value = it
                            navigation.updateDirections()
                        }
                    }
                }
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
                modifier = Modifier.then(Modifier),
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
                    .then(Modifier),
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
    Unit
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
        Unit
    }

    fun mapSide(maximum: Double): Double =
        min(maximum - 10, if (navigation.isSmall.value) smallMapSide else maximumBigMapSide)

    Unit
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

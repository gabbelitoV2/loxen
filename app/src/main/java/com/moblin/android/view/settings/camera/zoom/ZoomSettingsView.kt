package com.moblin.android.view.settings.camera.zoom

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.localized
import com.moblin.android.common.various.formatOneDecimal
import com.moblin.android.various.model.CameraPosition
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsZoom
import com.moblin.android.various.settings.SettingsZoomPreset
import com.moblin.android.various.settings.defaultSegmentedPickerSelectedColor
import com.moblin.android.various.settings.minZoomX
import com.moblin.android.various.utils.makeOffsets
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.RgbColorPickerView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.view.utils.TextButtonView
import java.util.UUID

private fun deleteBackZoomPreset(model: Model, zoom: SettingsZoom, offsets: List<Int>) {
    offsets.sortedDescending().forEach { zoom.back.removeAt(it) }
    model.backZoomPresetSettingsUpdated()
}

private fun deleteFrontZoomPreset(model: Model, zoom: SettingsZoom, offsets: List<Int>) {
    offsets.sortedDescending().forEach { zoom.front.removeAt(it) }
    model.frontZoomPresetSettingUpdated()
}

private fun moveBackZoomPreset(fromOffsets: List<Int>, toOffset: Int) {
    TODO("no Android counterpart for SwiftUI List onMove")
}

private fun moveFrontZoomPreset(fromOffsets: List<Int>, toOffset: Int) {
    TODO("no Android counterpart for SwiftUI List onMove")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZoomSettingsView(model: Model, zoom: SettingsZoom) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Zoom") })
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Speed")
                    Slider(
                        value = zoom.speed.toFloat(),
                        onValueChange = { zoom.speed = it.toDouble() },
                        valueRange = 1.0f..10.0f,
                        steps = 89,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatOneDecimal(zoom.speed),
                        modifier = Modifier.width(35.dp)
                    )
                }
            }
            item {
                Text(
                    text = "Back camera presets",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            items(items = zoom.back, key = { it.id }) { preset ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart && zoom.back.size > 1) {
                            makeOffsets(zoom.back, preset.id)?.let { offsets ->
                                deleteBackZoomPreset(model, zoom, offsets)
                            }
                            true
                        } else {
                            false
                        }
                    }
                )
                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = false,
                    backgroundContent = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                        }
                    }
                ) {
                    ZoomPresetSettingsView(
                        preset = preset,
                        minX = minZoomX,
                        maxX = model.getMinMaxZoomX(position = CameraPosition.BACK).second
                    )
                }
            }
            item {
                CreateButtonView {
                    zoom.back.add(
                        SettingsZoomPreset(
                            id = UUID.randomUUID(),
                            name = "1x",
                            x = 1.0f
                        )
                    )
                    model.backZoomPresetSettingsUpdated()
                }
            }
            item {
                SwipeLeftToDeleteHelpView(kind = localized("a preset"))
            }
            item {
                Text(
                    text = "Front camera presets",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            items(items = zoom.front, key = { it.id }) { preset ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart && zoom.front.size > 1) {
                            makeOffsets(zoom.front, preset.id)?.let { offsets ->
                                deleteFrontZoomPreset(model, zoom, offsets)
                            }
                            true
                        } else {
                            false
                        }
                    }
                )
                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = false,
                    backgroundContent = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                        }
                    }
                ) {
                    ZoomPresetSettingsView(
                        preset = preset,
                        minX = minZoomX,
                        maxX = model.getMinMaxZoomX(position = CameraPosition.FRONT).second
                    )
                }
            }
            item {
                CreateButtonView {
                    zoom.front.add(
                        SettingsZoomPreset(
                            id = UUID.randomUUID(),
                            name = "1x",
                            x = 1.0f
                        )
                    )
                    model.frontZoomPresetSettingUpdated()
                }
            }
            item {
                SwipeLeftToDeleteHelpView(kind = localized("a preset"))
            }
            item {
                Text(
                    text = "Camera switching",
                    style = MaterialTheme.typography.titleMedium
                )
                ZoomSwitchToSettingsView(
                    name = localized("back"),
                    position = CameraPosition.BACK,
                    defaultZoom = zoom.switchToBack
                )
                ZoomSwitchToSettingsView(
                    name = localized("front"),
                    position = CameraPosition.FRONT,
                    defaultZoom = zoom.switchToFront
                )
                Text(
                    text = "The zoom (in X) to set when switching to given camera, if enabled.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            item {
                Text(
                    text = "Color",
                    style = MaterialTheme.typography.titleMedium
                )
                RgbColorPickerView(
                    title = "Background",
                    color = zoom.backgroundColorColor,
                    opacity = true
                ) {
                    zoom.backgroundColor = it
                    TODO("no Android counterpart for Combine objectWillChange")
                }
                TextButtonView("Reset") {
                    zoom.backgroundColor = defaultSegmentedPickerSelectedColor
                    zoom.backgroundColorColor = zoom.backgroundColor.color()
                    TODO("no Android counterpart for Combine objectWillChange")
                }
                Text(
                    text = "Background color of the zoom preset button when selected.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

package com.moblin.android.view.settings.camera.zoom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalModel
import com.moblin.android.common.various.color
import com.moblin.android.common.various.formatOneDecimal
import com.moblin.android.common.various.iconWidth
import com.moblin.android.localized
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.FormSlider
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.backZoomPresetSettingsUpdated
import com.moblin.android.various.model.frontZoomPresetSettingUpdated
import com.moblin.android.various.model.getMinMaxZoomX
import com.moblin.android.various.model.zoomPresetsMayHaveChanged
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
import kotlin.math.roundToInt
import com.moblin.android.platform.avfoundation.AVCaptureDevice

private const val cameraPositionBack = 0
private const val cameraPositionFront = 1

private fun deleteBackZoomPreset(model: Model, zoom: SettingsZoom, offsets: List<Int>) {
    offsets.sortedDescending().forEach { zoom.back.removeAt(it) }
    model.backZoomPresetSettingsUpdated()
}

private fun deleteFrontZoomPreset(model: Model, zoom: SettingsZoom, offsets: List<Int>) {
    offsets.sortedDescending().forEach { zoom.front.removeAt(it) }
    model.frontZoomPresetSettingUpdated()
}

private fun moveBackZoomPreset(fromOffsets: List<Int>, toOffset: Int) {
    Unit
}

private fun moveFrontZoomPreset(fromOffsets: List<Int>, toOffset: Int) {
    Unit
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZoomSettingsView(model: Model = LocalModel.current, zoom: SettingsZoom) {
    Form(title = "Zoom") {
        Section {
            FormRow {
                Text(localized("Speed"))
                FormSlider(
                    value = zoom.speed,
                    onValueChange = { zoom.speed = (it * 10f).roundToInt() / 10f },
                    modifier = Modifier.weight(1f),
                    valueRange = 1f..10f,
                )
                Box(
                    modifier = Modifier.width(35.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(formatOneDecimal(zoom.speed))
                }
            }
        }
        Section(
            header = "Back camera presets",
            footerContent = { SwipeLeftToDeleteHelpView(kind = localized("a preset")) },
        ) {
            zoom.back.forEach { preset ->
                key(preset.id) {
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart && zoom.back.size > 1) {
                                val offset = zoom.back.indexOfFirst { it.id == preset.id }
                                if (offset >= 0) {
                                    deleteBackZoomPreset(model, zoom, listOf(offset))
                                }
                                true
                            } else {
                                false
                            }
                        }
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(formPalette().red),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                SystemImage(name = "trash", fontSize = iconWidth.sp, tint = Color.White)
                            }
                        },
                        enableDismissFromStartToEnd = false,
                    ) {
                        ZoomPresetSettingsView(
                            model = model,
                            preset = preset,
                            minX = minZoomX,
                            maxX = model.getMinMaxZoomX(position = AVCaptureDevice.Position.back).second
                        )
                    }
                }
            }
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
        Section(
            header = "Front camera presets",
            footerContent = { SwipeLeftToDeleteHelpView(kind = localized("a preset")) },
        ) {
            zoom.front.forEach { preset ->
                key(preset.id) {
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart && zoom.front.size > 1) {
                                val offset = zoom.front.indexOfFirst { it.id == preset.id }
                                if (offset >= 0) {
                                    deleteFrontZoomPreset(model, zoom, listOf(offset))
                                }
                                true
                            } else {
                                false
                            }
                        }
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(formPalette().red),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                SystemImage(name = "trash", fontSize = iconWidth.sp, tint = Color.White)
                            }
                        },
                        enableDismissFromStartToEnd = false,
                    ) {
                        ZoomPresetSettingsView(
                            model = model,
                            preset = preset,
                            minX = minZoomX,
                            maxX = model.getMinMaxZoomX(position = AVCaptureDevice.Position.front).second
                        )
                    }
                }
            }
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
        Section(
            header = "Camera switching",
            footer = "The zoom (in X) to set when switching to given camera, if enabled.",
        ) {
            ZoomSwitchToSettingsView(
                name = localized("back"),
                position = AVCaptureDevice.Position.back,
                defaultZoom = zoom.switchToBack
            )
            ZoomSwitchToSettingsView(
                name = localized("front"),
                position = AVCaptureDevice.Position.front,
                defaultZoom = zoom.switchToFront
            )
        }
        Section(
            header = "Color",
            footer = "Background color of the zoom preset button when selected.",
        ) {
            RgbColorPickerView(
                title = "Background",
                color = zoom.backgroundColorColor,
                onColorChanged = { zoom.backgroundColorColor = it },
                opacity = true
            ) {
                zoom.backgroundColor = it
                model.zoomPresetsMayHaveChanged()
            }
            TextButtonView("Reset") {
                zoom.backgroundColor = defaultSegmentedPickerSelectedColor
                zoom.backgroundColorColor = zoom.backgroundColor.color()
                model.zoomPresetsMayHaveChanged()
            }
        }
    }
}

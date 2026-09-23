package com.moblin.android.view.settings.camera.zoom

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.common.various.color
import com.moblin.android.common.various.formatOneDecimal
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.DeleteDisabled
import com.moblin.android.platform.swiftui.ForEach
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.FormSlider
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.move
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
import com.moblin.android.view.utils.ContextMenuDeleteButton
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
            ForEach(
                zoom.back,
                id = { it.id },
                onDelete = { offsets ->
                    deleteBackZoomPreset(model, zoom, offsets.toList())
                },
                onMove = { froms, to ->
                    zoom.back.move(fromOffsets = froms, toOffset = to)
                    model.backZoomPresetSettingsUpdated()
                },
            ) { preset ->
                ContextMenuDeleteButton(
                    disabled = zoom.back.size == 1,
                    action = {
                        val offset = zoom.back.indexOfFirst { it.id == preset.id }
                        if (offset >= 0) {
                            deleteBackZoomPreset(model, zoom, listOf(offset))
                        }
                    },
                ) {
                    DeleteDisabled(zoom.back.size == 1) {
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
            ForEach(
                zoom.front,
                id = { it.id },
                onDelete = { offsets ->
                    deleteFrontZoomPreset(model, zoom, offsets.toList())
                },
                onMove = { froms, to ->
                    zoom.front.move(fromOffsets = froms, toOffset = to)
                    model.frontZoomPresetSettingUpdated()
                },
            ) { preset ->
                ContextMenuDeleteButton(
                    disabled = zoom.front.size == 1,
                    action = {
                        val offset = zoom.front.indexOfFirst { it.id == preset.id }
                        if (offset >= 0) {
                            deleteFrontZoomPreset(model, zoom, listOf(offset))
                        }
                    },
                ) {
                    DeleteDisabled(zoom.front.size == 1) {
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

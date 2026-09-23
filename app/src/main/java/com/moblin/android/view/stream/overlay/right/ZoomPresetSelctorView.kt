package com.moblin.android.view.stream.overlay.right

import androidx.camera.core.CameraSelector
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.common.various.color
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.Zoom
import com.moblin.android.various.model.setZoomPreset
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsZoomPreset
import java.util.UUID

@Composable
private fun PickerItemView(preset: SettingsZoomPreset, width: Dp, height: Dp) {
    PickerLabelText(text = preset.name, width = width, height = height)
}

@Composable
private fun ZoomPresetView(
    model: Model = LocalModel.current,
    database: Database,
    presets: List<SettingsZoomPreset>,
    selectedPresetId: UUID,
    selectedColor: Color,
    width: Float,
) {
    fun segmentWidth(): Double = if (database.bigButtons) zoomSegmentWidthBig else zoomSegmentWidth

    fun height(): Double = if (database.bigButtons) segmentHeightBig else segmentHeight

    CompositionLocalProvider(LocalContentColor provides Color.White) {
        Box(
            modifier = Modifier
                .padding(bottom = 5.dp)
                .width(minOf(segmentWidth() * presets.size, maxOf(width - 20.0, 1.0)).dp)
                .clip(RoundedCornerShape(7.dp))
                .background(pickerBackgroundColor)
                .border(1.dp, pickerBorderColor, RoundedCornerShape(7.dp)),
            contentAlignment = Alignment.Center,
        ) {
            SegmentedHPicker(
                items = presets,
                selectedItem = presets.firstOrNull { it.id == selectedPresetId },
                onSelectedItemChange = { value ->
                    if (value != null) {
                        model.setZoomPreset(id = value.id)
                    }
                },
                selectedColor = selectedColor,
            ) { preset ->
                PickerItemView(
                    preset = preset,
                    width = minOf(segmentWidth(), (width - 20.0) / presets.size).coerceAtLeast(0.0).dp,
                    height = height().dp,
                )
            }
        }
    }
}

@Composable
private fun ZoomPresetVView(
    model: Model = LocalModel.current,
    database: Database,
    presets: List<SettingsZoomPreset>,
    selectedPresetId: UUID,
    selectedColor: Color,
    width: Float,
) {
    fun segmentWidth(): Double = if (database.bigButtons) zoomSegmentWidthBig else zoomSegmentWidth

    fun height(): Double = if (database.bigButtons) segmentHeightBig else segmentHeight

    CompositionLocalProvider(LocalContentColor provides Color.White) {
        Box(
            modifier = Modifier
                .padding(bottom = 5.dp)
                .width(segmentWidth().dp)
                .clip(RoundedCornerShape(7.dp))
                .background(pickerBackgroundColor)
                .border(1.dp, pickerBorderColor, RoundedCornerShape(7.dp)),
            contentAlignment = Alignment.Center,
        ) {
            SegmentedVPicker(
                items = presets.reversed(),
                selectedItem = presets.firstOrNull { it.id == selectedPresetId },
                onSelectedItemChange = { value ->
                    if (value != null) {
                        model.setZoomPreset(id = value.id)
                    }
                },
                selectedColor = selectedColor,
            ) { preset ->
                PickerItemView(
                    preset = preset,
                    width = minOf(segmentWidth(), (width - 20.0) / presets.size).coerceAtLeast(0.0).dp,
                    height = height().dp,
                )
            }
        }
    }
}

@Composable
fun StreamOverlayRightZoomPresetSelctorView(model: Model = LocalModel.current, zoom: Zoom, width: Float) {
    val frontZoomPresets by zoom.frontZoomPresets.collectAsState()
    val backZoomPresets by zoom.backZoomPresets.collectAsState()
    val frontPresetId by zoom.frontPresetId.collectAsState()
    val backPresetId by zoom.backPresetId.collectAsState()

    fun presets(): List<SettingsZoomPreset> =
        if (model.cameraPosition == CameraSelector.LENS_FACING_FRONT) frontZoomPresets else backZoomPresets

    fun selectedPresetId(): UUID =
        if (model.cameraPosition == CameraSelector.LENS_FACING_FRONT) frontPresetId else backPresetId

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        ZoomPresetView(
            model = model,
            database = model.database,
            presets = presets(),
            selectedPresetId = selectedPresetId(),
            selectedColor = model.database.zoom.backgroundColor.color(),
            width = width,
        )
    }
}

@Composable
fun StreamOverlayRightZoomPresetVSelctorView(model: Model = LocalModel.current, zoom: Zoom, width: Float) {
    val frontZoomPresets by zoom.frontZoomPresets.collectAsState()
    val backZoomPresets by zoom.backZoomPresets.collectAsState()
    val frontPresetId by zoom.frontPresetId.collectAsState()
    val backPresetId by zoom.backPresetId.collectAsState()

    fun presets(): List<SettingsZoomPreset> =
        if (model.cameraPosition == CameraSelector.LENS_FACING_FRONT) frontZoomPresets else backZoomPresets

    fun selectedPresetId(): UUID =
        if (model.cameraPosition == CameraSelector.LENS_FACING_FRONT) frontPresetId else backPresetId

    ZoomPresetVView(
        model = model,
        database = model.database,
        presets = presets(),
        selectedPresetId = selectedPresetId(),
        selectedColor = model.database.zoom.backgroundColor.color(),
        width = width,
    )
}

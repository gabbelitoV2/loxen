package com.moblin.android.view.stream.overlay.right

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moblin.android.various.model.CameraPosition
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.Zoom
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsZoomPreset
import java.util.UUID
import com.moblin.android.LocalModel

@Composable
private fun PickerItemView(preset: SettingsZoomPreset, modifier: Modifier = Modifier) {
    val name by preset.name.collectAsState()
    Text(
        text = name,
        modifier = modifier,
        color = Color.White,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
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
    val bigButtons by database.bigButtons.collectAsState()

    fun segmentWidth(): Float = if (bigButtons) zoomSegmentWidthBig else zoomSegmentWidth

    fun height(): Float = if (bigButtons) segmentHeightBig else segmentHeight

    val selectedPreset = presets.firstOrNull { it.id == selectedPresetId }
    SegmentedHPicker(
        items = presets,
        selectedItem = selectedPreset,
        onSelectedItemChange = { value ->
            value?.let { model.setZoomPreset(id = it.id) }
        },
        selectedColor = selectedColor,
        modifier = Modifier
            .padding(bottom = 5.dp)
            .width(minOf(segmentWidth() * presets.size, maxOf(width - 20f, 1f)).dp)
            .clip(RoundedCornerShape(7.dp))
            .background(pickerBackgroundColor)
            .border(1.dp, pickerBorderColor, RoundedCornerShape(7.dp)),
        content = { preset ->
            PickerItemView(
                preset = preset,
                modifier = Modifier
                    .width(minOf(segmentWidth(), (width - 20f) / presets.size).dp)
                    .height(height().dp),
            )
        },
    )
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
    val bigButtons by database.bigButtons.collectAsState()

    fun segmentWidth(): Float = if (bigButtons) zoomSegmentWidthBig else zoomSegmentWidth

    fun height(): Float = if (bigButtons) segmentHeightBig else segmentHeight

    val selectedPreset = presets.firstOrNull { it.id == selectedPresetId }
    SegmentedVPicker(
        items = presets.reversed(),
        selectedItem = selectedPreset,
        onSelectedItemChange = { value ->
            value?.let { model.setZoomPreset(id = it.id) }
        },
        selectedColor = selectedColor,
        modifier = Modifier
            .padding(bottom = 5.dp)
            .width(segmentWidth().dp)
            .clip(RoundedCornerShape(7.dp))
            .background(pickerBackgroundColor)
            .border(1.dp, pickerBorderColor, RoundedCornerShape(7.dp)),
        content = { preset ->
            PickerItemView(
                preset = preset,
                modifier = Modifier
                    .width(minOf(segmentWidth(), (width - 20f) / presets.size).dp)
                    .height(height().dp),
            )
        },
    )
}

@Composable
fun StreamOverlayRightZoomPresetSelctorView(model: Model = LocalModel.current, zoom: Zoom, width: Float) {
    val cameraPosition by model.cameraPosition.collectAsState()
    val frontZoomPresets by zoom.frontZoomPresets.collectAsState()
    val backZoomPresets by zoom.backZoomPresets.collectAsState()
    val frontPresetId by zoom.frontPresetId.collectAsState()
    val backPresetId by zoom.backPresetId.collectAsState()

    fun presets(): List<SettingsZoomPreset> =
        if (cameraPosition == CameraPosition.front) frontZoomPresets else backZoomPresets

    fun selectedPresetId(): UUID =
        if (cameraPosition == CameraPosition.front) frontPresetId else backPresetId

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
    val cameraPosition by model.cameraPosition.collectAsState()
    val frontZoomPresets by zoom.frontZoomPresets.collectAsState()
    val backZoomPresets by zoom.backZoomPresets.collectAsState()
    val frontPresetId by zoom.frontPresetId.collectAsState()
    val backPresetId by zoom.backPresetId.collectAsState()

    fun presets(): List<SettingsZoomPreset> =
        if (cameraPosition == CameraPosition.front) frontZoomPresets else backZoomPresets

    fun selectedPresetId(): UUID =
        if (cameraPosition == CameraPosition.front) frontPresetId else backPresetId

    ZoomPresetVView(
        model = model,
        database = model.database,
        presets = presets(),
        selectedPresetId = selectedPresetId(),
        selectedColor = model.database.zoom.backgroundColor.color(),
        width = width,
    )
}

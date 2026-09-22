package com.moblin.android.view.settings.scenes.widgets.widget.pngtuber

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsSensitivity
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetPngTuber
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.InlinePickerItem
import com.moblin.android.view.utils.InlinePickerView
import com.moblin.android.LocalModel

@Composable
private fun PickerView(model: Model = LocalModel.current) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            model.onDocumentPickerUrl?.invoke(uri.toString())
        }
    }
    LaunchedEffect(Unit) {
        launcher.launch(arrayOf("*/*"))
    }
}

private fun onUrl(
    model: Model,
    pngTuber: SettingsWidgetPngTuber,
    url: String,
    onSelected: (() -> Unit)?,
) {
    pngTuber.modelName = url.substringAfterLast('/')
    model.pngTuberStorage.add(id = pngTuber.id, url = java.io.File(url))
    onSelected?.invoke()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetPngTuberPickerView(
    model: Model = LocalModel.current,
    pngTuber: SettingsWidgetPngTuber,
    onSelected: (() -> Unit)? = null,
) {
    var showPicker by remember { mutableStateOf(false) }
    Column {
        Text(text = "Model", style = MaterialTheme.typography.titleSmall)
        Button(
            onClick = {
                showPicker = true
                model.onDocumentPickerUrl = { url ->
                    onUrl(model, pngTuber, url, onSelected)
                }
            },
        ) {
            HCenter {
                Text(
                    text = if (pngTuber.modelName.isEmpty()) {
                        localized("Select model")
                    } else {
                        pngTuber.modelName
                    },
                )
            }
        }
        Text(
            text = "A .save-file from PNGTuberPlus.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
    if (showPicker) {
        ModalBottomSheet(onDismissRequest = { showPicker = false }) {
            PickerView(model = model)
        }
    }
}

@Composable
fun WidgetSensitivityView(
    sensitivity: SettingsSensitivity,
    onChange: (SettingsSensitivity) -> Unit,
) {
    Column {
        Text(text = "Sensitivity", style = MaterialTheme.typography.titleSmall)
        Row {
            Text(text = "Mouth")
            Slider(
                value = sensitivity.mouth.toFloat(),
                onValueChange = { onChange(sensitivity.copy(mouth = it.toDouble())) },
                valueRange = 0.05f..3f,
                modifier = Modifier.weight(1f),
            )
        }
        Row {
            Text(text = "Eyes")
            Slider(
                value = sensitivity.eyes.toFloat(),
                onValueChange = { onChange(sensitivity.copy(eyes = it.toDouble())) },
                valueRange = 0.05f..5f,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private fun onCameraChange(
    model: Model,
    pngTuber: SettingsWidgetPngTuber,
    cameraId: String,
) {
    TODO("Model.cameraIdToSettingsCameraId and Model.sceneUpdated are not available")
}

private fun setEffectSettings(
    model: Model,
    widget: SettingsWidget,
    pngTuber: SettingsWidgetPngTuber,
) {
    TODO("Model.getPngTuberEffect is not available")
}

@Composable
fun WidgetPngTuberSettingsView(
    model: Model = LocalModel.current,
    widget: SettingsWidget,
    pngTuber: SettingsWidgetPngTuber,
) {
    var showVideoSourcePicker by remember { mutableStateOf(false) }
    var sensitivity by remember(pngTuber) { mutableStateOf(pngTuber.sensitivity) }
    var mirror by remember(pngTuber) { mutableStateOf(pngTuber.mirror) }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showVideoSourcePicker = true },
        ) {
            Text(text = "Video source")
            Spacer(Modifier.weight(1f))
            GrayTextView(text = TODO("Model.getCameraPositionName is not available"))
        }
    }
    if (showVideoSourcePicker) {
        InlinePickerView(
            title = "Video source",
            onChange = { cameraId -> onCameraChange(model, pngTuber, cameraId) },
            items = TODO("Model.listCameras is not available"),
            initialSelectedId = TODO("Model.getCameraId is not available"),
        )
    }
    WidgetPngTuberPickerView(model = model, pngTuber = pngTuber) {
        TODO("Model.resetSelectedScene is not available")
    }
    WidgetSensitivityView(
        sensitivity = sensitivity,
        onChange = { newSensitivity ->
            sensitivity = newSensitivity
            pngTuber.sensitivity = newSensitivity
        },
    )
    LaunchedEffect(sensitivity) {
        setEffectSettings(model, widget, pngTuber)
    }
    Column {
        Row {
            Text(text = "Mirror")
            Spacer(Modifier.weight(1f))
            Switch(
                checked = mirror,
                onCheckedChange = { checked ->
                    mirror = checked
                    pngTuber.mirror = checked
                },
            )
        }
    }
    LaunchedEffect(mirror) {
        setEffectSettings(model, widget, pngTuber)
    }
}

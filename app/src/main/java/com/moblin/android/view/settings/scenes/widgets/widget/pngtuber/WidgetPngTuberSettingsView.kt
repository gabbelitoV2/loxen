package com.moblin.android.view.settings.scenes.widgets.widget.pngtuber

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.moblin.android.LocalModel
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.FormButton
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.FormSlider
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Sheet
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.VideoSources
import com.moblin.android.various.settings.SettingsSensitivity
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetPngTuber
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.InlinePickerItem
import com.moblin.android.view.utils.InlinePickerView
import com.moblin.android.various.model.cameraIdToSettingsCameraId
import com.moblin.android.various.model.getCameraId
import com.moblin.android.various.model.getCameraPositionName
import com.moblin.android.various.model.getPngTuberEffect
import com.moblin.android.various.model.resetSelectedScene
import com.moblin.android.various.model.sceneUpdated

@Composable
private fun PickerView(model: Model = LocalModel.current) {
    val launcher = com.moblin.android.platform.DocumentPicker.rememberLauncher(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            com.moblin.android.platform.DocumentPicker.copy(uri) { url -> model.onDocumentPickerUrl?.invoke(url) }
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

@Composable
fun WidgetPngTuberPickerView(
    model: Model = LocalModel.current,
    pngTuber: SettingsWidgetPngTuber,
    onSelected: (() -> Unit)? = null,
) {
    var showPicker by remember { mutableStateOf(false) }
    Section(
        header = localized("Model"),
        footer = localized("A .save-file from PNGTuberPlus."),
    ) {
        FormButton(
            title = if (pngTuber.modelName.isEmpty()) {
                localized("Select model")
            } else {
                pngTuber.modelName
            },
            centered = true,
        ) {
            showPicker = true
            model.onDocumentPickerUrl = { url ->
                showPicker = false
                onUrl(model, pngTuber, url, onSelected)
            }
        }
    }
    if (showPicker) {
        Sheet(onDismissRequest = { showPicker = false }) {
            PickerView(model = model)
        }
    }
}

@Composable
fun WidgetSensitivityView(
    sensitivity: SettingsSensitivity,
    onChange: (SettingsSensitivity) -> Unit,
) {
    Section(header = localized("Sensitivity")) {
        FormRow {
            Text(localized("Mouth"))
            FormSlider(
                value = sensitivity.mouth.toFloat(),
                onValueChange = { onChange(sensitivity.copy(mouth = it.toDouble())) },
                valueRange = 0.05f..3f,
                modifier = Modifier.weight(1f),
            )
        }
        FormRow {
            Text(localized("Eyes"))
            FormSlider(
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
    pngTuber.updateCameraId(
        settingsCameraId = model.cameraIdToSettingsCameraId(cameraId = cameraId),
    )
    model.sceneUpdated(attachCamera = true, updateRemoteScene = false)
}

private fun setEffectSettings(
    model: Model,
    widget: SettingsWidget,
    pngTuber: SettingsWidgetPngTuber,
) {
    model.getPngTuberEffect(id = widget.id)?.setSettings(
        mirror = pngTuber.mirror,
        sensitivity = pngTuber.sensitivity,
    )
}

@Composable
fun WidgetPngTuberSettingsView(
    model: Model = LocalModel.current,
    widget: SettingsWidget,
    pngTuber: SettingsWidgetPngTuber,
    videoSources: VideoSources,
) {
    var sensitivity by remember(pngTuber) { mutableStateOf(pngTuber.sensitivity) }
    var mirror by remember(pngTuber) { mutableStateOf(pngTuber.mirror) }
    Column {
        Section {
            NavigationLink(
                destination = {
                    InlinePickerView(
                        title = localized("Video source"),
                        onChange = { cameraId -> onCameraChange(model, pngTuber, cameraId) },
                        items = videoSources.all().map {
                            InlinePickerItem(id = it.id, text = it.name)
                        },
                        initialSelectedId = model.getCameraId(pngTuberWidget = pngTuber),
                    )
                },
            ) {
                Text(localized("Video source"))
                Spacer(Modifier.weight(1f))
                GrayTextView(
                    text = model.getCameraPositionName(pngTuberWidget = pngTuber),
                )
            }
        }
        WidgetPngTuberPickerView(model = model, pngTuber = pngTuber) {
            model.resetSelectedScene(changeScene = false)
        }
        WidgetSensitivityView(
            sensitivity = sensitivity,
            onChange = { newSensitivity ->
                sensitivity = newSensitivity
                pngTuber.sensitivity = newSensitivity
                setEffectSettings(model, widget, pngTuber)
            },
        )
        Section {
            Toggle(
                title = localized("Mirror"),
                isOn = mirror,
                onChange = { newValue ->
                    mirror = newValue
                    pngTuber.mirror = newValue
                    setEffectSettings(model, widget, pngTuber)
                },
            )
        }
    }
}

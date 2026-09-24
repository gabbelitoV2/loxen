package com.moblin.android.view.settings.scenes.widgets.widget.vtuber

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.FormSlider
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Sheet
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.cameraIdToSettingsCameraId
import com.moblin.android.various.model.getCameraId
import com.moblin.android.various.model.getCameraPositionName
import com.moblin.android.various.model.listCameras
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetVTuber
import com.moblin.android.various.settings.SettingsWidgetVTuberType
import com.moblin.android.view.settings.scenes.widgets.widget.pngtuber.WidgetSensitivityView
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.InlinePickerItem
import com.moblin.android.view.utils.InlinePickerView
import com.moblin.android.view.utils.TextButtonView
import java.io.File
import java.util.UUID
import java.util.zip.ZipFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.moblin.android.various.model.getVTuberEffect
import com.moblin.android.various.model.resetSelectedScene
import com.moblin.android.various.model.sceneUpdated

private fun unzipLive2DModel(from: String, to: File) {
    ZipFile(from).use { zip ->
        val entries = zip.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            if (entry.isDirectory) {
                continue
            }
            val components = entry.name.split("/")
            if (components.contains("__MACOSX") || components.contains("..")) {
                continue
            }
            val file = File(to, components.joinToString("/"))
            file.parentFile?.mkdirs()
            zip.getInputStream(entry).use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }
}

@Composable
private fun PickerView(model: Model = LocalModel.current) {
    val launcher = com.moblin.android.platform.DocumentPicker.rememberLauncher(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        com.moblin.android.platform.DocumentPicker.copy(uri) { url -> model.onDocumentPickerUrl?.invoke(url) }
    }
    LaunchedEffect(Unit) {
        launcher.launch(arrayOf("*/*"))
    }
}

@Composable
fun WidgetVTuberPickerView(
    model: Model = LocalModel.current,
    vTuber: SettingsWidgetVTuber,
    onSelected: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showPicker by remember { mutableStateOf(false) }

    fun onLive2DUrl(url: String) {
        val directory = File(context.cacheDir, UUID.randomUUID().toString())
        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    unzipLive2DModel(from = url, to = directory)
                }
            }
            if (result.isFailure) {
                model.makeErrorToast(
                    title = localized("Failed to unzip model"),
                    subTitle = result.exceptionOrNull()?.localizedMessage ?: "",
                )
                return@launch
            }
            vTuber.type = SettingsWidgetVTuberType.live2D
            vTuber.modelName = url.substringAfterLast('/')
            model.vTuberStorage.add(id = vTuber.id, url = directory)
            onSelected?.invoke()
        }
    }

    fun onUrl(url: String) {
        if (url.substringAfterLast('.', "").lowercase() == "zip") {
            onLive2DUrl(url)
        } else {
            vTuber.type = SettingsWidgetVTuberType.vrm
            vTuber.modelName = url.substringAfterLast('/')
            model.vTuberStorage.add(id = vTuber.id, url = File(url))
            onSelected?.invoke()
        }
    }

    Section(
        header = localized("Model"),
        footer = localized("Most VRM 0.0 files and zipped Live2D Cubism models are supported."),
    ) {
        TextButtonView(
            title = if (vTuber.modelName.isEmpty()) localized("Select model") else vTuber.modelName,
        ) {
            showPicker = true
            model.onDocumentPickerUrl = { url -> onUrl(url) }
        }
        if (showPicker) {
            Sheet(onDismissRequest = { showPicker = false }) {
                PickerView(model = model)
            }
        }
    }
}

@Composable
fun WidgetVTuberSettingsView(
    model: Model = LocalModel.current,
    widget: SettingsWidget,
    vTuber: SettingsWidgetVTuber,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
    modifier: Modifier = Modifier,
) {
    fun onCameraChange(cameraId: String) {
        vTuber.updateCameraId(
            settingsCameraId = model.cameraIdToSettingsCameraId(cameraId = cameraId),
        )
        model.sceneUpdated(attachCamera = true, updateRemoteScene = false)
    }

    fun setEffectSettings() {
        model.getVTuberEffect(widget.id)?.setSettings(
            cameraFieldOfView = vTuber.cameraFieldOfView,
            cameraPositionY = vTuber.cameraPositionY,
            mirror = vTuber.mirror,
            sensitivity = vTuber.sensitivity,
            armsAngle = vTuber.armsAngle,
        )
    }

    var cameraPositionY by remember { mutableStateOf(vTuber.cameraPositionY) }
    var cameraFieldOfView by remember { mutableStateOf(vTuber.cameraFieldOfView) }
    var mirror by remember { mutableStateOf(vTuber.mirror) }
    var sensitivity by remember { mutableStateOf(vTuber.sensitivity) }
    var armsAngle by remember { mutableStateOf(vTuber.armsAngle) }

    Section {
        NavigationLink(
            destination = {
                InlinePickerView(
                    title = localized("Video source"),
                    onChange = { onCameraChange(it) },
                    items = model.listCameras(excludeBuiltin = false).map {
                        InlinePickerItem(id = it.id, text = it.name)
                    },
                    initialSelectedId = model.getCameraId(vTuberWidget = vTuber),
                )
            },
        ) {
            Text(localized("Video source"))
            Spacer(Modifier.weight(1f))
            GrayTextView(text = model.getCameraPositionName(vTuberWidget = vTuber))
        }
    }

    WidgetVTuberPickerView(
        model = model,
        vTuber = vTuber,
        onSelected = { model.resetSelectedScene(changeScene = false) },
    )

    if (vTuber.type == SettingsWidgetVTuberType.vrm) {
        Section(header = localized("Camera")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(localized("Vertical position"))
                FormSlider(
                    value = cameraPositionY.toFloat(),
                    onValueChange = {
                        cameraPositionY = it.toDouble()
                        vTuber.cameraPositionY = it.toDouble()
                        setEffectSettings()
                    },
                    modifier = Modifier.weight(1f),
                    valueRange = 1f..2f,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(localized("Field of view"))
                FormSlider(
                    value = cameraFieldOfView.toFloat(),
                    onValueChange = {
                        cameraFieldOfView = it.toDouble()
                        vTuber.cameraFieldOfView = it.toDouble()
                        setEffectSettings()
                    },
                    modifier = Modifier.weight(1f),
                    valueRange = 10f..30f,
                )
            }
        }
    }

    Section {
        Toggle(
            title = localized("Mirror"),
            isOn = mirror,
            onChange = {
                mirror = it
                vTuber.mirror = it
                setEffectSettings()
            },
        )
    }

    WidgetSensitivityView(
        sensitivity = sensitivity,
        onChange = {
            sensitivity = it
            vTuber.sensitivity = it
            setEffectSettings()
        },
    )

    if (vTuber.type == SettingsWidgetVTuberType.vrm) {
        Section(header = localized("Angles")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(localized("Arms"))
                FormSlider(
                    value = armsAngle.toFloat(),
                    onValueChange = {
                        armsAngle = it.toDouble()
                        vTuber.armsAngle = it.toDouble()
                        setEffectSettings()
                    },
                    modifier = Modifier.weight(1f),
                    valueRange = 20f..90f,
                )
            }
        }
    }
}

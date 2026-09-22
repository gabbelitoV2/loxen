package com.moblin.android.view.settings.scenes.widgets.widget.vtuber

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetVTuber
import com.moblin.android.various.settings.SettingsWidgetVTuberType
import com.moblin.android.view.settings.scenes.widgets.widget.pngtuber.WidgetSensitivityView
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.TextButtonView
import java.io.File
import java.util.UUID
import java.util.zip.ZipFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

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
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let { model.onDocumentPickerUrl?.invoke(it.toString()) }
    }
    LaunchedEffect(Unit) {
        launcher.launch(arrayOf("*/*"))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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

    Column(modifier = modifier.fillMaxWidth()) {
        Text("Model", style = MaterialTheme.typography.titleSmall)
        TextButtonView(title = if (vTuber.modelName.isEmpty()) localized("Select model") else vTuber.modelName) {
            showPicker = true
            model.onDocumentPickerUrl = { url -> onUrl(url) }
        }
        if (showPicker) {
            ModalBottomSheet(onDismissRequest = { showPicker = false }) {
                PickerView()
            }
        }
        Text(
            "Most VRM 0.0 files and zipped Live2D Cubism models are supported.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetVTuberSettingsView(
    model: Model = LocalModel.current,
    widget: SettingsWidget,
    vTuber: SettingsWidgetVTuber,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
    modifier: Modifier = Modifier,
) {
    fun onCameraChange(cameraId: String) {
        vTuber.updateCameraId(settingsCameraId = TODO("model.cameraIdToSettingsCameraId is not available"))
        Unit
    }

    fun setEffectSettings() {
        Unit
    }

    var cameraPositionY by remember { mutableStateOf(vTuber.cameraPositionY) }
    var cameraFieldOfView by remember { mutableStateOf(vTuber.cameraFieldOfView) }
    var mirror by remember { mutableStateOf(vTuber.mirror) }
    var sensitivity by remember { mutableStateOf(vTuber.sensitivity) }
    var armsAngle by remember { mutableStateOf(vTuber.armsAngle) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text("Video source", style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate("Video source") },
        ) {
            Text("Video source")
            Spacer(Modifier.weight(1f))
            GrayTextView(text = TODO("model.getCameraPositionName is not available"))
        }

        WidgetVTuberPickerView(
            model = model,
            vTuber = vTuber,
            onSelected = { TODO("model.resetSelectedScene is not available") },
        )

        if (vTuber.type == SettingsWidgetVTuberType.vrm) {
            Text("Camera", style = MaterialTheme.typography.titleSmall)
            Row {
                Text("Vertical position")
                Slider(
                    value = cameraPositionY.toFloat(),
                    onValueChange = {
                        cameraPositionY = it.toDouble()
                        vTuber.cameraPositionY = it.toDouble()
                    },
                    valueRange = 1.0f..2.0f,
                    steps = 99,
                )
            }
            LaunchedEffect(cameraPositionY) { setEffectSettings() }
            Row {
                Text("Field of view")
                Slider(
                    value = cameraFieldOfView.toFloat(),
                    onValueChange = {
                        cameraFieldOfView = it.toDouble()
                        vTuber.cameraFieldOfView = it.toDouble()
                    },
                    valueRange = 10.0f..30.0f,
                    steps = 19,
                )
            }
            LaunchedEffect(cameraFieldOfView) { setEffectSettings() }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Mirror")
            Spacer(Modifier.weight(1f))
            Switch(
                checked = mirror,
                onCheckedChange = {
                    mirror = it
                    vTuber.mirror = it
                },
            )
        }
        LaunchedEffect(mirror) { setEffectSettings() }

        WidgetSensitivityView(
            sensitivity = sensitivity,
            onChange = {
                sensitivity = it
                vTuber.sensitivity = it
            },
        )
        LaunchedEffect(sensitivity) { setEffectSettings() }

        if (vTuber.type == SettingsWidgetVTuberType.vrm) {
            Text("Angles", style = MaterialTheme.typography.titleSmall)
            Row {
                Text("Arms")
                Slider(
                    value = armsAngle.toFloat(),
                    onValueChange = {
                        armsAngle = it.toDouble()
                        vTuber.armsAngle = it.toDouble()
                    },
                    valueRange = 20.0f..90.0f,
                    steps = 69,
                )
            }
            LaunchedEffect(armsAngle) { setEffectSettings() }
        }
    }
}

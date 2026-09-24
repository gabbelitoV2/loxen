package com.moblin.android.view.settings.camera

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.platform.swiftui.ForEach
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormButton
import com.moblin.android.platform.swiftui.Label
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.PickerStyle
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Sheet
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.addLutCube
import com.moblin.android.various.model.addLutPng
import com.moblin.android.various.model.colorSpaceUpdated
import com.moblin.android.various.model.fallbackStream
import com.moblin.android.various.model.lutEnabledUpdated
import com.moblin.android.various.model.lutUpdated
import com.moblin.android.various.model.removeLutCube
import com.moblin.android.various.model.removeLutPng
import com.moblin.android.various.model.setLutName
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsColor
import com.moblin.android.various.settings.SettingsColorLut
import com.moblin.android.various.settings.SettingsColorSpace
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.various.utils.makeOffsets
import com.moblin.android.view.settings.camera.cameracontrols.CameraControlsView
import com.moblin.android.view.settings.camera.fixedhorizon.FixedHorizonView
import com.moblin.android.view.settings.camera.mirrorfrontcamera.MirrorFrontCameraOnStreamView
import com.moblin.android.view.settings.camera.tapscreentofocus.TapScreenToFocusSettingsView
import com.moblin.android.view.settings.camera.videostabilization.VideoStabilizationSettingsView
import com.moblin.android.view.settings.camera.zoom.ZoomSettingsView
import com.moblin.android.view.settings.scenes.widgets.widget.alerts.AlertPickerView
import com.moblin.android.view.settings.selfiestick.SelfieStickDoesNotWorkView
import com.moblin.android.view.settings.streams.stream.video.StreamVideoSettingsView
import com.moblin.android.view.utils.ContextMenuDeleteButton
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.NameEditView
import com.moblin.android.view.utils.ShortcutSectionView
import com.moblin.android.view.utils.TextButtonView

@Composable
fun CustomLutView(
    model: Model = LocalModel.current,
    lut: SettingsColorLut,
    name: String,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    var nameState by remember(lut.id) { mutableStateOf(name) }

    NavigationLink(
        destination = {
            Form(title = "Custom LUT") {
                Section {
                    NameEditView(
                        name = nameState,
                        onNameChange = { newName ->
                            nameState = newName
                            model.setLutName(lut, newName)
                        },
                    )
                }
                Section {
                    val image = model.imageStorage.tryRead(lut.id)?.let { data ->
                        BitmapFactory.decodeByteArray(data, 0, data.size)?.asImageBitmap()
                    }
                    if (image != null) {
                        HCenter {
                            Image(
                                bitmap = image,
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size((1920 / 6).dp, (1080 / 6).dp),
                            )
                        }
                    }
                }
            }
        },
    ) {
        Text(nameState)
    }
}

@Composable
fun CustomLutDestinationView(
    model: Model = LocalModel.current,
    lut: SettingsColorLut,
    name: String,
) {
    var nameState by remember(lut.id) { mutableStateOf(name) }

    Form(title = "Custom LUT") {
        Section {
            NameEditView(
                name = nameState,
                onNameChange = { newName ->
                    nameState = newName
                    model.setLutName(lut, newName)
                },
            )
        }
        Section {
            val image = model.imageStorage.tryRead(lut.id)?.let { data ->
                BitmapFactory.decodeByteArray(data, 0, data.size)?.asImageBitmap()
            }
            if (image != null) {
                HCenter {
                    Image(
                        bitmap = image,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size((1920 / 6).dp, (1080 / 6).dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraSettingsCubeLutsView(
    model: Model = LocalModel.current,
    color: SettingsColor,
) {
    var showPicker by remember { mutableStateOf(false) }

    Section(header = "My .cube LUTs") {
        ForEach(
            color.diskLutsCube,
            id = { it.id },
            onDelete = { offsets ->
                model.removeLutCube(offsets.toList())
            },
        ) { lut ->
            ContextMenuDeleteButton(
                action = {
                    val offset = color.diskLutsCube.indexOfFirst { it.id == lut.id }
                    if (offset >= 0) {
                        model.removeLutCube(listOf(offset))
                    }
                },
            ) {
                CustomLutView(model = model, lut = lut, name = lut.name)
            }
        }
        TextButtonView(title = "Add") {
            showPicker = true
            model.onDocumentPickerUrl = { url -> model.addLutCube(url) }
        }
    }
    Sheet(isPresented = showPicker, onDismissRequest = { showPicker = false }) {
        AlertPickerView(type = "item")
    }
}

@Composable
private fun CameraSettingsPngLutsView(
    model: Model = LocalModel.current,
    color: SettingsColor,
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            context.contentResolver.openInputStream(uri)?.use { input ->
                model.addLutPng(input.readBytes())
            }
        }
    }

    Section(header = "My .png LUTs") {
        ForEach(
            color.diskLutsPng,
            id = { it.id },
            onDelete = { offsets ->
                model.removeLutPng(offsets.toList())
            },
        ) { lut ->
            ContextMenuDeleteButton(
                action = {
                    val offset = color.diskLutsPng.indexOfFirst { it.id == lut.id }
                    if (offset >= 0) {
                        model.removeLutPng(listOf(offset))
                    }
                },
            ) {
                CustomLutView(model = model, lut = lut, name = lut.name)
            }
        }
        FormButton(title = "Add", centered = true) {
            launcher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        }
    }
}

@Composable
fun CameraSettingsLutsView(
    model: Model = LocalModel.current,
    color: SettingsColor,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Form(title = "LUTs") {
        Section(header = "Bundled") {
            color.bundledLuts.forEach { lut ->
                key(lut.id) {
                    Text(lut.name)
                }
            }
        }
        CameraSettingsCubeLutsView(model = model, color = color)
        CameraSettingsPngLutsView(model = model, color = color)
    }
}

@Composable
private fun CameraSettingsAppleLogLutView(
    model: Model = LocalModel.current,
    color: SettingsColor,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Form(title = "Apple Log LUT") {
        Section(
            footer = "If enabled, selected LUT is applied when the Apple Log color space is used.",
        ) {
            Toggle(
                title = "Enabled",
                isOn = color.lutEnabled,
            ) { value ->
                color.lutEnabled = value
                model.lutEnabledUpdated()
            }
        }
        Section {
            val luts = color.allLuts()
            Picker(
                title = "",
                selection = color.lut,
                options = luts.map { it.id },
                text = { id -> luts.firstOrNull { it.id == id }?.name ?: "" },
                pickerStyle = PickerStyle.inline,
            ) { id ->
                color.lut = id
                model.lutUpdated()
            }
        }
    }
}

@Composable
private fun CameraPreviewSettingsView(
    model: Model = LocalModel.current,
    database: Database,
) {
    Section(
        footer = "The Camera preview quick button shows and hides the camera preview instantly, " +
            "without the scene switch transition. Uses slightly more system resources.",
    ) {
        Toggle(
            title = "Instant camera preview",
            isOn = database.alwaysAttachCameraPreview,
        ) { value ->
            database.alwaysAttachCameraPreview = value
            model.attachCamera()
        }
    }
}

@Composable
private fun PhotoShootSettingsView(
    model: Model = LocalModel.current,
    database: Database,
) {
    Section(
        footer = "The Photo shoot quick button starts and stops the photo shoot instantly, " +
            "without the scene switch transition. Uses slightly more system resources.",
    ) {
        Toggle(
            title = "Instant photo shoot",
            isOn = database.alwaysAttachPhotoShoot,
        ) { value ->
            database.alwaysAttachPhotoShoot = value
            model.attachCamera()
        }
    }
}

@Composable
fun CameraSettingsView(
    model: Model = LocalModel.current,
    database: Database,
    stream: SettingsStream,
    color: SettingsColor,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val isLive by model.isLive.collectAsState()
    val isRecording by model.isRecording.collectAsState()

    Form(title = "Camera") {
        if (stream !== fallbackStream) {
            ShortcutSectionView {
                NavigationLink(
                    destination = {
                        StreamVideoSettingsView(database = database, stream = stream)
                    },
                ) {
                    Label("Video", systemImage = "dot.radiowaves.left.and.right")
                }
            }
        }
        Section {
            if (database.showAllSettings) {
                NavigationLink(
                    destination = {
                        ZoomSettingsView(zoom = database.zoom)
                    },
                ) {
                    Text("Zoom")
                }
            }
            VideoStabilizationSettingsView(mode = database.videoStabilizationMode)
            if (database.showAllSettings) {
                FixedHorizonView(database = database)
            }
            MirrorFrontCameraOnStreamView(model = model, database = database)
            SelfieStickDoesNotWorkView(database = database, selfieStick = database.selfieStick)
        }
        if (database.showAllSettings) {
            Section(
                footer = "⚠️ Does not work well when interactive chat is enabled.",
            ) {
                TapScreenToFocusSettingsView(model = model, database = database)
            }
        }
        if (database.showAllSettings) {
            Section(
                footer = "⚠️ Hijacks volume buttons. You can only change volume in Control " +
                    "Center when enabled.",
            ) {
                CameraControlsView(database = database)
            }
        }
        if (database.showAllSettings) {
            CameraPreviewSettingsView(model = model, database = database)
            PhotoShootSettingsView(model = model, database = database)
            if (model.supportsAppleLog) {
                Section(
                    footer = "The Apple Log LUT is only applied when the Apple Log color space " +
                        "is selected.",
                ) {
                    Picker(
                        title = "Color space",
                        selection = color.space,
                        options = SettingsColorSpace.entries,
                        enabled = !(isLive || isRecording),
                        text = { it.rawValue },
                    ) { space ->
                        color.space = space
                        model.colorSpaceUpdated()
                    }
                    NavigationLink(
                        destination = {
                            CameraSettingsAppleLogLutView(model = model, color = color)
                        },
                    ) {
                        Text("Apple Log LUT")
                    }
                }
            } else {
                Section {
                    Picker(
                        title = "Color space",
                        selection = color.space,
                        options = SettingsColorSpace.entries
                            .filter { it != SettingsColorSpace.appleLog },
                        enabled = !(isLive || isRecording),
                        text = { it.rawValue },
                    ) { space ->
                        color.space = space
                        model.colorSpaceUpdated()
                    }
                }
            }
            Section(
                footer = "LUTs modifies image colors when applied.",
            ) {
                NavigationLink(
                    destination = {
                        CameraSettingsLutsView(color = color)
                    },
                ) {
                    Text("LUTs")
                }
            }
        }
    }
}

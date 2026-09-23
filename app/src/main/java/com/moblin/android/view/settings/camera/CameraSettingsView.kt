package com.moblin.android.view.settings.camera

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.addLutCube
import com.moblin.android.various.model.colorSpaceUpdated
import com.moblin.android.various.model.fallbackStream
import com.moblin.android.various.model.lutEnabledUpdated
import com.moblin.android.various.model.lutUpdated
import com.moblin.android.various.model.removeLutCube
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
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.NameEditView
import com.moblin.android.view.utils.ShortcutSectionView
import com.moblin.android.view.utils.TextButtonView
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

@Composable
fun CustomLutView(
    model: Model = LocalModel.current,
    lut: SettingsColorLut,
    name: String,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    var nameState by remember { mutableStateOf(name) }

    fun loadImage(): ByteArray? {
        return model.imageStorage.tryRead(lut.id)
    }

    Text(
        text = nameState,
        modifier = Modifier.clickable { onNavigate("CustomLut") },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomLutDestinationView(
    model: Model = LocalModel.current,
    lut: SettingsColorLut,
    name: String,
) {
    var nameState by remember { mutableStateOf(name) }

    val image: ImageBitmap? = remember(lut.id) {
        model.imageStorage.tryRead(lut.id)?.let { data ->
            BitmapFactory.decodeByteArray(data, 0, data.size)?.asImageBitmap()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Custom LUT") })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            NameEditView(
                name = nameState,
                onNameChange = { newName ->
                    nameState = newName
                    lut.name = newName
                },
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CameraSettingsCubeLutsView(
    model: Model = LocalModel.current,
    color: SettingsColor,
) {
    var showPicker by remember { mutableStateOf(false) }

    fun onUrl(url: String) {
        model.addLutCube(url)
    }

    fun deleteLutCube(offsets: List<Int>) {
        model.removeLutCube(offsets)
    }

    val diskLutsCube = color.diskLutsCube

    Column {
        Text("My .cube LUTs", style = MaterialTheme.typography.titleSmall)
        diskLutsCube.forEach { lut ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    CustomLutView(
                        model = model,
                        lut = lut,
                        name = lut.name,
                        onNavigate = {},
                    )
                }
                IconButton(onClick = {
                    val offsets = diskLutsCube.indexOf(lut)
                    if (offsets != -1) {
                        deleteLutCube(listOf(offsets))
                    }
                }) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                }
            }
        }
        TextButtonView("Add") {
            showPicker = true
            model.onDocumentPickerUrl = { url -> onUrl(url) }
        }
        if (showPicker) {
            ModalBottomSheet(onDismissRequest = { showPicker = false }) {
                AlertPickerView(type = "item")
            }
        }
    }
}

@Composable
private fun CameraSettingsPngLutsView(
    model: Model = LocalModel.current,
    color: SettingsColor,
) {
    var presentingPicker by remember { mutableStateOf(false) }
    var selectedImageItem: Any? by remember { mutableStateOf<Any?>(null) }

    fun deleteLutPng(offsets: List<Int>) {
        color.diskLutsPng = color.diskLutsPng.filterIndexed { index, _ -> index !in offsets }
    }

    val diskLutsPng = color.diskLutsPng

    Column {
        Text("My .png LUTs", style = MaterialTheme.typography.titleSmall)
        diskLutsPng.forEach { lut ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    CustomLutView(
                        model = model,
                        lut = lut,
                        name = lut.name,
                        onNavigate = {},
                    )
                }
                IconButton(onClick = {
                    val offsets = diskLutsPng.indexOf(lut)
                    if (offsets != -1) {
                        deleteLutPng(listOf(offsets))
                    }
                }) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                }
            }
        }
        Button(onClick = { presentingPicker = true }) {
            HCenter {
                Text("Add")
            }
        }
        if (presentingPicker) {
            Unit
        }
        LaunchedEffect(selectedImageItem) {
            if (selectedImageItem != null) {
                Unit
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraSettingsLutsView(
    model: Model = LocalModel.current,
    color: SettingsColor,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    var selectedImageItem: Any? by remember { mutableStateOf<Any?>(null) }

    val bundledLuts = color.bundledLuts

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("LUTs") })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            Text("Bundled", style = MaterialTheme.typography.titleSmall)
            bundledLuts.forEach { lut ->
                Text(lut.name)
            }
            CameraSettingsCubeLutsView(model = model, color = color)
            CameraSettingsPngLutsView(model = model, color = color)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColorSpacePicker(
    label: String,
    spaces: List<SettingsColorSpace>,
    selected: SettingsColorSpace,
    enabled: Boolean,
    onSelect: (SettingsColorSpace) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
    ) {
        OutlinedTextField(
            value = selected.rawValue,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            spaces.forEach { space ->
                DropdownMenuItem(
                    text = { Text(space.rawValue) },
                    onClick = {
                        onSelect(space)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CameraSettingsAppleLogLutView(
    model: Model = LocalModel.current,
    color: SettingsColor,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val lutEnabled = color.lutEnabled
    val lut = color.lut

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Apple Log LUT") })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Enabled", modifier = Modifier.weight(1f))
                Switch(
                    checked = lutEnabled,
                    onCheckedChange = {
                        color.lutEnabled = it
                        model.lutEnabledUpdated()
                    },
                )
            }
            Text(
                "If enabled, selected LUT is applied when the Apple Log color space is used.",
                style = MaterialTheme.typography.bodySmall,
            )
            color.allLuts().forEach { item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            color.lut = item.id
                            model.lutUpdated()
                        },
                ) {
                    RadioButton(
                        selected = lut == item.id,
                        onClick = null,
                    )
                    Text(item.name)
                }
            }
        }
    }
}

@Composable
private fun CameraPreviewSettingsView(
    model: Model = LocalModel.current,
    database: Database,
) {
    val alwaysAttachCameraPreview = database.alwaysAttachCameraPreview

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Instant camera preview", modifier = Modifier.weight(1f))
            Switch(
                checked = alwaysAttachCameraPreview,
                onCheckedChange = {
                    database.alwaysAttachCameraPreview = it
                    model.attachCamera()
                },
            )
        }
        Text(
            "The Camera preview quick button shows and hides the camera preview instantly, without " +
                "the scene switch transition. Uses slightly more system resources.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun PhotoShootSettingsView(
    model: Model = LocalModel.current,
    database: Database,
) {
    val alwaysAttachPhotoShoot = database.alwaysAttachPhotoShoot

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Instant photo shoot", modifier = Modifier.weight(1f))
            Switch(
                checked = alwaysAttachPhotoShoot,
                onCheckedChange = {
                    database.alwaysAttachPhotoShoot = it
                    model.attachCamera()
                },
            )
        }
        Text(
            "The Photo shoot quick button starts and stops the photo shoot instantly, without " +
                "the scene switch transition. Uses slightly more system resources.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraSettingsView(
    model: Model = LocalModel.current,
    database: Database,
    stream: SettingsStream,
    color: SettingsColor,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val showAllSettings = database.showAllSettings
    val zoom = database.zoom
    val videoStabilizationMode = database.videoStabilizationMode
    val selfieStick = database.selfieStick
    val colorSpace = color.space
    val isLive by model.isLive.collectAsState()
    val isRecording by model.isRecording.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Camera") })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            if (stream !== fallbackStream) {
                ShortcutSectionView {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onNavigate("StreamVideoSettingsView") },
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Text("Video")
                    }
                }
            }
            Column {
                if (showAllSettings) {
                    Text(
                        "Zoom",
                        modifier = Modifier.clickable { onNavigate("ZoomSettingsView") },
                    )
                }
                VideoStabilizationSettingsView(mode = videoStabilizationMode)
                if (showAllSettings) {
                    FixedHorizonView(database = database)
                }
                MirrorFrontCameraOnStreamView(model = model, database = database)
                SelfieStickDoesNotWorkView(database = database, selfieStick = selfieStick)
            }
            if (showAllSettings) {
                Column {
                    TapScreenToFocusSettingsView(model = model, database = database)
                    Text(
                        "⚠️ Does not work well when interactive chat is enabled.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            if (showAllSettings) {
                Column {
                    CameraControlsView(database = database)
                    Text(
                        "⚠️ Hijacks volume buttons. You can only change volume in Control Center when enabled.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            if (showAllSettings) {
                CameraPreviewSettingsView(model = model, database = database)
                PhotoShootSettingsView(model = model, database = database)
                if (model.supportsAppleLog) {
                    Column {
                        ColorSpacePicker(
                            label = "Color space",
                            spaces = SettingsColorSpace.entries,
                            selected = colorSpace,
                            enabled = !(isLive || isRecording),
                            onSelect = {
                                color.space = it
                                model.colorSpaceUpdated()
                                Unit
                            },
                        )
                        Text(
                            "Apple Log LUT",
                            modifier = Modifier
                                .clickable { onNavigate("CameraSettingsAppleLogLutView") },
                        )
                        Text(
                            "The Apple Log LUT is only applied when the Apple Log color space is selected.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                } else {
                    Column {
                        ColorSpacePicker(
                            label = "Color space",
                            spaces = SettingsColorSpace.entries.filter { it != SettingsColorSpace.appleLog },
                            selected = colorSpace,
                            enabled = !(isLive || isRecording),
                            onSelect = {
                                color.space = it
                                model.colorSpaceUpdated()
                                Unit
                            },
                        )
                    }
                }
                Column {
                    Text(
                        "LUTs",
                        modifier = Modifier.clickable { onNavigate("CameraSettingsLutsView") },
                    )
                    Text(
                        "LUTs modifies image colors when applied.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

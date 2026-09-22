package com.moblin.android.view.settings.scenes.scene

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.localized
import com.moblin.android.various.model.Mic
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.appendWidgetToScene
import com.moblin.android.various.model.findWidget
import com.moblin.android.various.model.getCameraPositionName
import com.moblin.android.various.model.getMicById
import com.moblin.android.various.model.getSelectedScene
import com.moblin.android.various.model.isCaptureDeviceWidget
import com.moblin.android.various.model.isSceneVideoSourceActive
import com.moblin.android.various.model.resetSelectedScene
import com.moblin.android.various.model.sceneUpdated
import com.moblin.android.various.model.switchMicIfNeededAfterSceneSwitch
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsScene
import com.moblin.android.various.settings.SettingsSceneCameraPosition
import com.moblin.android.various.settings.SettingsSceneWidget
import com.moblin.android.various.settings.defaultSegmentedPickerSelectedColor
import com.moblin.android.various.settings.videoStabilizationModes
import com.moblin.android.various.utils.makeOffsets
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.AddButtonView
import com.moblin.android.view.utils.DraggableItemPrefixView
import com.moblin.android.view.utils.IconAndTextView
import com.moblin.android.view.utils.NameEditView
import com.moblin.android.view.utils.RgbColorPickerView
import com.moblin.android.view.utils.SwipeLeftToRemoveHelpView
import com.moblin.android.view.utils.TextButtonView
import com.moblin.android.view.utils.VideoSourceRotationView
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoStabilizationView(model: Model = LocalModel.current, scene: SettingsScene) {
    var expanded by remember { mutableStateOf(false) }
    val videoStabilizationMode = scene.videoStabilizationMode
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(localized("Video stabilization"), modifier = Modifier.weight(1f))
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = videoStabilizationMode.toString(),
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .width(180.dp),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                for (mode in videoStabilizationModes) {
                    DropdownMenuItem(
                        text = { Text(mode.toString()) },
                        onClick = {
                            expanded = false
                            scene.videoStabilizationMode = mode
                        },
                    )
                }
            }
        }
    }
    LaunchedEffect(videoStabilizationMode) {
        model.sceneUpdated(attachCamera = true, updateRemoteScene = false)
    }
}

@Composable
private fun MicView(
    model: Model = LocalModel.current,
    scene: SettingsScene,
    mic: Mic,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val micId = scene.micId
    LaunchedEffect(Unit) {
        if (micId.isEmpty()) {
            scene.micId = mic.current.value.id
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("Mic") }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(localized("Mic"))
        Spacer(Modifier.weight(1f))
        GrayTextView(model.getMicById(id = micId)?.name ?: "Unknown 😢")
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SceneWidgetView(
    model: Model = LocalModel.current,
    database: Database,
    sceneWidget: SettingsSceneWidget,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
    onLongClick: () -> Unit,
) {
    val widgets = database.widgets
    val widget = widgets.firstOrNull { it.id == sceneWidget.widgetId } ?: return
    val enabled = widget.enabled
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onNavigate("SceneWidgetSettingsView/${sceneWidget.widgetId}") },
                onLongClick = onLongClick,
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DraggableItemPrefixView()
        IconAndTextView(widget.image(), widget.name)
        Spacer(Modifier.weight(1f))
        Switch(
            checked = enabled,
            onCheckedChange = { value ->
                widget.enabled = value
                model.sceneUpdated(attachCamera = model.isCaptureDeviceWidget(widget = widget))
            },
        )
    }
}

val startScreenCatptureHelp = localized("Start a screen capture by long-pressing the record button in iOS Control Center and select Moblin.")

@Composable
private fun VideoSourceView(
    model: Model = LocalModel.current,
    database: Database,
    scene: SettingsScene,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    var presentingScreenCaptureAlert by remember { mutableStateOf(false) }
    val showAllSettings = database.showAllSettings
    val videoSource = scene.videoSource
    val videoSourceRotation = scene.videoSourceRotation
    val mirror = scene.mirror
    val overrideVideoStabilizationMode = scene.overrideVideoStabilizationMode
    val fillFrame = scene.fillFrame
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            localized("Video source"),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate("Name") }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.PhotoCamera, contentDescription = null)
            Text(localized("Name"))
            Spacer(Modifier.weight(1f))
            if (!model.isSceneVideoSourceActive(scene = scene)) {
                Icon(Icons.Default.Warning, contentDescription = null)
            }
            GrayTextView(model.getCameraPositionName(scene = scene))
        }
        if (showAllSettings) {
            if (videoSource.cameraPosition != null) {
                VideoSourceRotationView(
                    selectedRotation = videoSourceRotation,
                    onSelectedRotationChange = { scene.videoSourceRotation = it },
                )
                LaunchedEffect(videoSourceRotation) {
                    model.sceneUpdated(updateRemoteScene = false)
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(localized("Mirror"))
                Spacer(Modifier.weight(1f))
                Switch(checked = mirror, onCheckedChange = { scene.mirror = it })
            }
            LaunchedEffect(mirror) {
                model.sceneUpdated(attachCamera = true, updateRemoteScene = false)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(localized("Override video stabilization"))
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = overrideVideoStabilizationMode,
                    onCheckedChange = { scene.overrideVideoStabilizationMode = it },
                )
            }
            LaunchedEffect(overrideVideoStabilizationMode) {
                model.sceneUpdated(attachCamera = true, updateRemoteScene = false)
            }
            if (overrideVideoStabilizationMode) {
                VideoStabilizationView(model = model, scene = scene)
            }
            if (videoSource.cameraPosition != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(localized("Fill frame"))
                    Spacer(Modifier.weight(1f))
                    Switch(checked = fillFrame, onCheckedChange = { scene.fillFrame = it })
                }
                LaunchedEffect(fillFrame) {
                    model.sceneUpdated(attachCamera = true, updateRemoteScene = false)
                }
            }
        }
        if (showAllSettings) {
            Text(
                localized("Enable Override video stabilization to override Settings → Camera → Video stabilization in this scene."),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
    if (presentingScreenCaptureAlert) {
        AlertDialog(
            onDismissRequest = { presentingScreenCaptureAlert = false },
            text = { Text(startScreenCatptureHelp) },
            confirmButton = {
                TextButton(onClick = { presentingScreenCaptureAlert = false }) {
                    Text(localized("Got it"))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickSwitchGroupView(model: Model = LocalModel.current, database: Database, scene: SettingsScene) {
    var expanded by remember { mutableStateOf(false) }
    val quickSwitchGroup = scene.quickSwitchGroup
    val forceSceneSwitchTransition = database.forceSceneSwitchTransition
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(localized("Quick switch group"), modifier = Modifier.weight(1f))
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = quickSwitchGroup?.toString() ?: localized("-- None --"),
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .width(160.dp),
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text(localized("-- None --")) },
                        onClick = {
                            expanded = false
                            scene.quickSwitchGroup = null
                        },
                    )
                    for (group in 1 until 5) {
                        DropdownMenuItem(
                            text = { Text(group.toString()) },
                            onClick = {
                                expanded = false
                                scene.quickSwitchGroup = group
                            },
                        )
                    }
                }
            }
        }
        LaunchedEffect(quickSwitchGroup) {
            if (model.getSelectedScene() === scene) {
                model.resetSelectedScene(changeScene = false, attachCamera = true)
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(localized("Switching between scenes in the same group may be instant."))
            if (forceSceneSwitchTransition) {
                Text("")
                Text(localized("⚠️ Disable Settings → Scenes → Scene switching → Force transition to enable groups."))
            }
        }
    }
}

@Composable
private fun SceneColorView(model: Model = LocalModel.current, scene: SettingsScene) {
    val backgroundColorColor = scene.backgroundColorColor
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            localized("Color"),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        RgbColorPickerView(
            title = "Background",
            color = backgroundColorColor,
            opacity = true,
            onColorChanged = { },
            onChange = { rgbColor ->
                scene.backgroundColor = rgbColor
            },
        )
        TextButtonView("Reset") {
            scene.backgroundColor = defaultSegmentedPickerSelectedColor
        }
        Text(
            localized("Background color of the scene button when selected."),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun SceneMicView(
    model: Model = LocalModel.current,
    database: Database,
    scene: SettingsScene,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val showAllSettings = database.showAllSettings
    val overrideMic = scene.overrideMic
    if (showAllSettings) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                localized("Mic"),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(localized("Override"))
                Spacer(Modifier.weight(1f))
                Switch(checked = overrideMic, onCheckedChange = { scene.overrideMic = it })
            }
            LaunchedEffect(overrideMic) {
                model.switchMicIfNeededAfterSceneSwitch()
            }
            if (overrideMic) {
                MicView(model = model, scene = scene, mic = model.mic, onNavigate = onNavigate)
            }
            Text(
                localized("Enable Override to automatically switch to selected mic (if available) when switching to this scene."),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetsView(
    model: Model = LocalModel.current,
    database: Database,
    scene: SettingsScene,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    var presentingAddWidget by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<SettingsSceneWidget?>(null) }
    val widgets = database.widgets
    val sceneWidgets = scene.widgets

    fun deleteSceneWidget(offsets: List<Int>) {
        var attachCamera = false
        if (scene.id == model.getSelectedScene()?.id) {
            for (offset in offsets) {
                val widget = model.findWidget(id = sceneWidgets[offset].widgetId)
                if (widget != null) {
                    attachCamera = model.isCaptureDeviceWidget(widget = widget)
                }
            }
        }
        scene.widgets = sceneWidgets.toMutableList().also { list ->
            offsets.sortedDescending().forEach { list.removeAt(it) }
        }
        model.sceneUpdated(attachCamera = attachCamera)
        model.sceneSettingsPanelSceneId.value += 1
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            localized("Widgets"),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        for (sceneWidget in sceneWidgets) {
            key(sceneWidget.id) {
                SceneWidgetView(
                    model = model,
                    database = database,
                    sceneWidget = sceneWidget,
                    onNavigate = onNavigate,
                    onLongClick = { pendingDelete = sceneWidget },
                )
            }
        }
        TODO("onMove: drag and drop reordering of scene widgets is not available in Compose")
        AddButtonView {
            presentingAddWidget = true
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            SwipeLeftToRemoveHelpView(localized("a widget"))
        }
    }
    if (presentingAddWidget) {
        ModalBottomSheet(onDismissRequest = { presentingAddWidget = false }) {
            Column(
                modifier = Modifier
                    .widthIn(min = 220.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(5.dp),
            ) {
                for (widget in widgets) {
                    TextButton(
                        onClick = {
                            model.appendWidgetToScene(scene = scene, widget = widget)
                            presentingAddWidget = false
                        },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconAndTextView(widget.image(), widget.name)
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
    pendingDelete?.let { sceneWidget ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(localized("Delete widget?")) },
            text = { Text(localized("Remove this widget from the scene?")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val offset = sceneWidgets.indexOfFirst { it.id == sceneWidget.id }
                        if (offset != -1) {
                            deleteSceneWidget(listOf(offset))
                        }
                        pendingDelete = null
                    },
                ) {
                    Text(localized("Delete"))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(localized("Cancel"))
                }
            },
        )
    }
}

@Composable
fun SceneShortcutView(
    database: Database,
    scene: SettingsScene,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("SceneSettingsView") }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(localized("Scene"))
    }
}

@Composable
fun SceneSettingsView(
    model: Model = LocalModel.current,
    database: Database,
    scene: SettingsScene,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val name = scene.name
    val scenes = database.scenes
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        NameEditView(
            name = name,
            onNameChange = { scene.name = it },
            existingNames = scenes,
        )
        VideoSourceView(model = model, database = database, scene = scene, onNavigate = onNavigate)
        QuickSwitchGroupView(model = model, database = database, scene = scene)
        SceneMicView(model = model, database = database, scene = scene, onNavigate = onNavigate)
        WidgetsView(model = model, database = database, scene = scene, onNavigate = onNavigate)
        SceneColorView(model = model, scene = scene)
    }
}

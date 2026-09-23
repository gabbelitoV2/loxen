package com.moblin.android.view.settings.scenes.scene

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.common.various.color
import com.moblin.android.localized
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.*
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
import com.moblin.android.various.settings.SettingsSceneWidget
import com.moblin.android.various.settings.defaultSegmentedPickerSelectedColor
import com.moblin.android.various.settings.videoStabilizationModes
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.AddButtonView
import com.moblin.android.view.utils.DraggableItemPrefixView
import com.moblin.android.view.utils.IconAndTextView
import com.moblin.android.view.utils.NameEditView
import com.moblin.android.view.utils.RgbColorPickerView
import com.moblin.android.view.utils.SwipeLeftToRemoveHelpView
import com.moblin.android.view.utils.TextButtonView
import com.moblin.android.view.utils.VideoSourceRotationView

@Composable
private fun VideoStabilizationView(model: Model = LocalModel.current, scene: SettingsScene) {
    Picker(
        title = localized("Video stabilization"),
        selection = scene.videoStabilizationMode,
        options = videoStabilizationModes,
        onChange = { value ->
            scene.videoStabilizationMode = value
            model.sceneUpdated(attachCamera = true, updateRemoteScene = false)
        },
    )
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
    FormRow(onClick = { onNavigate("Mic") }) {
        Text(localized("Mic"))
        Spacer(Modifier.weight(1f))
        GrayTextView(model.getMicById(id = micId)?.name ?: localized("Unknown 😢"))
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
        IosSwitch(
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
    Section(
        header = localized("Video source"),
        footer = if (showAllSettings) {
            localized("Enable Override video stabilization to override Settings → Camera → Video stabilization in this scene.")
        } else {
            null
        },
    ) {
        FormRow(onClick = { onNavigate("Name") }) {
            SystemImage("camera", fontSize = 17.sp)
            Text(localized("Name"))
            Spacer(Modifier.weight(1f))
            if (!model.isSceneVideoSourceActive(scene = scene)) {
                SystemImage("cable.connector.slash", fontSize = 17.sp)
            }
            GrayTextView(model.getCameraPositionName(scene = scene))
        }
        if (showAllSettings) {
            if (videoSource.cameraPosition != null) {
                VideoSourceRotationView(
                    selectedRotation = videoSourceRotation,
                    onSelectedRotationChange = { value ->
                        scene.videoSourceRotation = value
                        model.sceneUpdated(updateRemoteScene = false)
                    },
                )
            }
            Toggle(
                localized("Mirror"),
                isOn = mirror,
                onChange = { value ->
                    scene.mirror = value
                    model.sceneUpdated(attachCamera = true, updateRemoteScene = false)
                },
            )
            Toggle(
                localized("Override video stabilization"),
                isOn = overrideVideoStabilizationMode,
                onChange = { value ->
                    scene.overrideVideoStabilizationMode = value
                    model.sceneUpdated(attachCamera = true, updateRemoteScene = false)
                },
            )
            if (overrideVideoStabilizationMode) {
                VideoStabilizationView(model = model, scene = scene)
            }
            if (videoSource.cameraPosition != null) {
                Toggle(
                    localized("Fill frame"),
                    isOn = fillFrame,
                    onChange = { value ->
                        scene.fillFrame = value
                        model.sceneUpdated(attachCamera = true, updateRemoteScene = false)
                    },
                )
            }
        }
    }
    Alert(
        title = startScreenCatptureHelp,
        isPresented = presentingScreenCaptureAlert,
        onDismissRequest = { presentingScreenCaptureAlert = false },
    ) {
        Button("Got it") {
            presentingScreenCaptureAlert = false
        }
    }
}

@Composable
private fun QuickSwitchGroupView(model: Model = LocalModel.current, database: Database, scene: SettingsScene) {
    val quickSwitchGroup = scene.quickSwitchGroup
    val forceSceneSwitchTransition = database.forceSceneSwitchTransition
    Section(
        footerContent = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(localized("Switching between scenes in the same group may be instant."))
                if (forceSceneSwitchTransition) {
                    Text("")
                    Text(localized("⚠️ Disable Settings → Scenes → Scene switching → Force transition to enable groups."))
                }
            }
        },
    ) {
        Picker(
            title = localized("Quick switch group"),
            selection = quickSwitchGroup,
            options = listOf<Int?>(null, 1, 2, 3, 4),
            text = { it?.toString() ?: localized("-- None --") },
            onChange = { value ->
                scene.quickSwitchGroup = value
                if (model.getSelectedScene() === scene) {
                    model.resetSelectedScene(changeScene = false, attachCamera = true)
                }
            },
        )
    }
}

@Composable
private fun SceneColorView(model: Model = LocalModel.current, scene: SettingsScene) {
    val backgroundColorColor = scene.backgroundColorColor
    Section(
        header = localized("Color"),
        footer = localized("Background color of the scene button when selected."),
    ) {
        RgbColorPickerView(
            title = "Background",
            color = backgroundColorColor,
            opacity = true,
            onColorChanged = { scene.backgroundColorColor = it },
            onChange = { rgbColor ->
                scene.backgroundColor = rgbColor
            },
        )
        TextButtonView("Reset") {
            scene.backgroundColor = defaultSegmentedPickerSelectedColor
            scene.backgroundColorColor = scene.backgroundColor.color()
        }
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
        Section(
            header = localized("Mic"),
            footer = localized("Enable Override to automatically switch to selected mic (if available) when switching to this scene."),
        ) {
            Toggle(
                localized("Override"),
                isOn = overrideMic,
                onChange = { value ->
                    scene.overrideMic = value
                    model.switchMicIfNeededAfterSceneSwitch()
                },
            )
            if (overrideMic) {
                MicView(model = model, scene = scene, mic = model.mic, onNavigate = onNavigate)
            }
        }
    }
}

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

    Section(
        header = localized("Widgets"),
        footerContent = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
            ) {
                SwipeLeftToRemoveHelpView(localized("a widget"))
            }
        },
    ) {
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
        AddButtonView {
            if (widgets.isNotEmpty()) {
                presentingAddWidget = true
            }
        }
        if (presentingAddWidget) {
            Sheet(onDismissRequest = { presentingAddWidget = false }) {
                Column(
                    modifier = Modifier
                        .padding(5.dp)
                        .widthIn(min = 220.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    for (widget in widgets) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) {
                                    model.appendWidgetToScene(scene = scene, widget = widget)
                                    presentingAddWidget = false
                                }
                                .padding(11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconAndTextView(widget.image(), widget.name)
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        val sceneWidgetToDelete = pendingDelete
        Alert(
            title = "Delete widget?",
            isPresented = sceneWidgetToDelete != null,
            onDismissRequest = { pendingDelete = null },
            message = "Remove this widget from the scene?",
        ) {
            Button("Cancel", role = ButtonRole.cancel)
            Button("Delete", role = ButtonRole.destructive) {
                if (sceneWidgetToDelete != null) {
                    val offset = sceneWidgets.indexOfFirst { it.id == sceneWidgetToDelete.id }
                    if (offset != -1) {
                        deleteSceneWidget(listOf(offset))
                    }
                }
            }
        }
    }
}

@Composable
fun SceneShortcutView(
    database: Database,
    scene: SettingsScene,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    FormRow(onClick = { onNavigate("SceneSettingsView") }) {
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
    Form(title = localized("Scene")) {
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

package com.moblin.android.view.settings.scenes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.common.various.formatOneDecimal
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.ContextMenu
import com.moblin.android.platform.swiftui.ForEach
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormSlider
import com.moblin.android.platform.swiftui.HorizontalEdge
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.SwipeActions
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.binding
import com.moblin.android.platform.swiftui.move
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsDebug
import com.moblin.android.various.settings.SettingsGraphicsImplementation
import com.moblin.android.various.settings.SettingsScene
import com.moblin.android.various.settings.SettingsSceneSwitchTransition
import com.moblin.android.various.utils.isMac
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.scenes.autoswitchers.AutoSwitchersSettingsView
import com.moblin.android.view.settings.scenes.disconnectprotection.DisconnectProtectionSettingsView
import com.moblin.android.view.settings.scenes.scene.SceneSettingsView
import com.moblin.android.view.settings.scenes.widgets.WidgetsSettingsView
import com.moblin.android.view.utils.ContextMenuDeleteButtonView
import com.moblin.android.view.utils.ContextMenuDuplicateButtonView
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.DraggableItemPrefixView
import com.moblin.android.view.utils.SwipeLeftToDeleteButtonView
import com.moblin.android.view.utils.SwipeLeftToDuplicateButtonView
import com.moblin.android.view.utils.SwipeLeftToDuplicateOrDeleteHelpView
import java.util.UUID
import com.moblin.android.various.model.getSelectedScene
import com.moblin.android.various.model.remoteSceneSettingsUpdated
import com.moblin.android.various.model.resetSelectedScene
import com.moblin.android.various.model.setGraphicsImplementation

@Composable
private fun SceneItemView(
    model: Model = LocalModel.current,
    database: Database,
    scene: SettingsScene,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    fun duplicate() {
        val clone = scene.clone()
        clone.name = makeUniqueName(scene.name, database.scenes)
        database.scenes.add(clone)
    }

    fun delete() {
        val deletedCurrentScene = model.getSelectedScene() === scene
        database.scenes.removeAll { it === scene }
        if (deletedCurrentScene) {
            model.resetSelectedScene()
        }
    }

    ContextMenu(
        menu = {
            if (isMac()) {
                ContextMenuDuplicateButtonView { duplicate() }
                ContextMenuDeleteButtonView { delete() }
            }
        },
    ) {
        SwipeActions(
            edge = HorizontalEdge.trailing,
            allowsFullSwipe = false,
            actions = {
                SwipeLeftToDeleteButtonView { delete() }
                SwipeLeftToDuplicateButtonView { duplicate() }
            },
        ) {
            NavigationLink(
                destination = {
                    SceneSettingsView(database = model.database, scene = scene)
                },
                label = {
                    DraggableItemPrefixView()
                    Toggle(
                        title = scene.name,
                        isOn = binding(
                            get = { scene.enabled },
                            set = { value ->
                                scene.enabled = value
                                if (model.getSelectedScene() === scene) {
                                    model.resetSelectedScene()
                                } else {
                                    model.sceneSelector.sceneIndex.value += 0
                                }
                            },
                        ),
                    )
                },
            )
        }
    }
}

@Composable
private fun ScenesListView(
    model: Model = LocalModel.current,
    database: Database,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Section(
        header = "Scenes",
        footerContent = {
            SwipeLeftToDuplicateOrDeleteHelpView(kind = localized("a scene"))
        },
    ) {
        ForEach(
            database.scenes,
            id = { it.id },
            onMove = { froms, to ->
                database.scenes.move(fromOffsets = froms, toOffset = to)
            },
        ) { scene ->
            SceneItemView(
                model = model,
                database = database,
                scene = scene,
            )
        }
        CreateButtonView {
            val name = makeUniqueName(SettingsScene.baseName, database.scenes)
            val scene = SettingsScene(name)
            database.scenes.add(scene)
        }
    }
}

@Composable
private fun SceneSwitching(
    model: Model = LocalModel.current,
    database: Database,
    debug: SettingsDebug,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    NavigationLink(
        destination = {
            SceneSwitchingDetail(model = model, database = database, debug = debug)
        },
        label = {
            Text(localized("Scene switching"))
        },
    )
}

@Composable
private fun SceneSwitchingDetail(
    model: Model = LocalModel.current,
    database: Database,
    debug: SettingsDebug,
) {
    val cameraSwitchRemoveBlackish by debug.cameraSwitchRemoveBlackish.collectAsState()

    Form(title = "Scene switching") {
        Section(
            footer = "Ingest, screen capture and media player video sources can instantly be switched to, but if you want consistency you can force scene switch transitions to these as well.",
        ) {
            Picker(
                title = "Transition",
                selection = database.sceneSwitchTransition,
                options = SettingsSceneSwitchTransition.entries,
                onChange = { value ->
                    database.sceneSwitchTransition = value
                    model.setSceneSwitchTransition()
                },
            )
            Toggle(
                title = "Force transition",
                isOn = binding(
                    get = { database.forceSceneSwitchTransition },
                    set = { value ->
                        database.forceSceneSwitchTransition = value
                        model.resetSelectedScene(changeScene = false, attachCamera = true)
                    },
                ),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(localized("Video blackish"))
                FormSlider(
                    value = cameraSwitchRemoveBlackish,
                    onValueChange = { debug.cameraSwitchRemoveBlackish.value = it },
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier.width(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${formatOneDecimal(cameraSwitchRemoveBlackish)} s")
                }
            }
        }
    }
}

@Composable
private fun RemoteSceneView(model: Model = LocalModel.current) {
    var selectedSceneId by remember { mutableStateOf(model.database.remoteSceneId) }
    val scenes = model.database.scenes

    Section(
        footer = "Widgets in selected scene will be shown on the Moblin device the remote control assistant is connected to.",
    ) {
        Picker(
            title = "Remote scene",
            selection = selectedSceneId,
            options = listOf<UUID?>(null) + scenes.map { it.id },
            text = { id -> scenes.firstOrNull { it.id == id }?.name ?: "-- None --" },
            onChange = { value ->
                selectedSceneId = value
                model.database.remoteSceneId = value
                model.remoteSceneSettingsUpdated()
            },
        )
    }
}

@Composable
private fun GraphicsView(
    model: Model = LocalModel.current,
    database: Database,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    NavigationLink(
        destination = {
            GraphicsDetail(model = model, database = database)
        },
        label = {
            Text(localized("Graphics"))
        },
    )
}

@Composable
private fun GraphicsDetail(
    model: Model = LocalModel.current,
    database: Database,
) {
    Form(title = "Graphics") {
        Section(
            footer = "Core Image is Apple's image processing framework. MetalPetal is experimental. MetalPetal provides similar image processing, and hopefully uses less system resources.",
        ) {
            Picker(
                title = "Implementation",
                selection = database.graphicsImplementation,
                options = SettingsGraphicsImplementation.entries,
                onChange = { value ->
                    database.graphicsImplementation = value
                    model.setGraphicsImplementation()
                },
            )
            if (database.graphicsImplementation == SettingsGraphicsImplementation.metalPetal) {
                Text(localized("⚠️ MetalPetal does not work when Moblin is in background."))
            }
        }
        if (database.graphicsImplementation == SettingsGraphicsImplementation.coreImage) {
            Section(
                footer = "High quality downsampling makes downscaled images look better, but uses more system resources.",
            ) {
                Toggle(
                    title = "High quality downsampling",
                    isOn = binding(
                        get = { database.graphicsHighQualityDownsampling },
                        set = { value ->
                            database.graphicsHighQualityDownsampling = value
                            model.setHighQualityDownsampling()
                        },
                    ),
                )
            }
        }
    }
}

@Composable
fun SceneNameView(scene: SettingsScene) {
    Text(scene.name)
}

@Composable
fun ScenesSettingsView(
    model: Model = LocalModel.current,
    database: Database,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Form(title = "Scenes") {
        ScenesListView(model = model, database = database, onNavigate = onNavigate)
        WidgetsSettingsView(database = database)
        if (database.showAllSettings) {
            SceneSwitching(
                model = model,
                database = database,
                debug = database.debug,
                onNavigate = onNavigate,
            )
            AutoSwitchersSettingsView(
                autoSceneSwitchers = database.autoSceneSwitchers,
                showSelector = true,
            )
            DisconnectProtectionSettingsView(
                database = database,
                disconnectProtection = database.disconnectProtection,
            )
            RemoteSceneView(model = model)
            GraphicsView(model = model, database = database, onNavigate = onNavigate)
        }
    }
}

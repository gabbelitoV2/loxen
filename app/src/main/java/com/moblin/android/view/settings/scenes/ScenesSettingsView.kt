package com.moblin.android.view.settings.scenes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.formatOneDecimal
import com.moblin.android.localized
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
import com.moblin.android.view.settings.scenes.widgets.WidgetsSettingsView
import com.moblin.android.view.utils.ContextMenuDeleteButtonView
import com.moblin.android.view.utils.ContextMenuDuplicateButtonView
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.DraggableItemPrefixView
import com.moblin.android.view.utils.SwipeLeftToDeleteButtonView
import com.moblin.android.view.utils.SwipeLeftToDuplicateButtonView
import com.moblin.android.view.utils.SwipeLeftToDuplicateOrDeleteHelpView
import java.util.UUID

@Composable
private fun SceneItemView(
    model: Model,
    database: Database,
    scene: SettingsScene,
    onNavigate: (String) -> Unit,
) {
    val name by scene.name.collectAsState()
    val enabled by scene.enabled.collectAsState()

    fun duplicate() {
        val clone = scene.clone()
        clone.name.value = makeUniqueName(name, database.scenes.value)
        database.scenes.value = database.scenes.value + clone
    }

    fun delete() {
        val deletedCurrentScene = model.getSelectedScene() === scene
        database.scenes.value = database.scenes.value.filter { it !== scene }
        if (deletedCurrentScene) {
            model.resetSelectedScene()
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate("SceneSettingsView") },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DraggableItemPrefixView()
            Text(name)
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = enabled,
                onCheckedChange = { value ->
                    scene.enabled.value = value
                    if (model.getSelectedScene() === scene) {
                        model.resetSelectedScene()
                    } else {
                        model.sceneSelector.value.sceneIndex.value += 0
                    }
                },
            )
        }
        SwipeLeftToDeleteButtonView { delete() }
        SwipeLeftToDuplicateButtonView { duplicate() }
        if (isMac()) {
            ContextMenuDuplicateButtonView { duplicate() }
            ContextMenuDeleteButtonView { delete() }
        }
    }
}

@Composable
private fun ScenesListView(
    model: Model,
    database: Database,
    onNavigate: (String) -> Unit,
) {
    val scenes by database.scenes.collectAsState()

    fun move(froms: List<Int>, to: Int) {
        val list = database.scenes.value.toMutableList()
        val moving = froms.mapNotNull { list.getOrNull(it) }
        froms.sortedDescending().forEach { index ->
            if (index in list.indices) {
                list.removeAt(index)
            }
        }
        val insertAt = if (froms.isNotEmpty() && to > froms.max()) to - froms.size else to
        list.addAll(insertAt.coerceIn(0, list.size), moving)
        database.scenes.value = list
    }

    Column {
        Text("Scenes", style = MaterialTheme.typography.titleMedium)
        scenes.forEach { scene ->
            key(scene.id) {
                SceneItemView(
                    model = model,
                    database = database,
                    scene = scene,
                    onNavigate = onNavigate,
                )
            }
        }
        CreateButtonView {
            val name = makeUniqueName(SettingsScene.baseName, database.scenes.value)
            val scene = SettingsScene(name)
            database.scenes.value = database.scenes.value + scene
        }
        SwipeLeftToDuplicateOrDeleteHelpView(kind = localized("a scene"))
    }
}

@Composable
private fun SceneSwitching(
    model: Model,
    database: Database,
    debug: SettingsDebug,
    onNavigate: (String) -> Unit,
) {
    Text(
        text = "Scene switching",
        modifier = Modifier.clickable { onNavigate("Scene switching") },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SceneSwitchingDetail(
    model: Model,
    database: Database,
    debug: SettingsDebug,
) {
    val transition by database.sceneSwitchTransition.collectAsState()
    val forceTransition by database.forceSceneSwitchTransition.collectAsState()
    val cameraSwitchRemoveBlackish by debug.cameraSwitchRemoveBlackish.collectAsState()
    var transitionExpanded by remember { mutableStateOf(false) }

    Column {
        Text("Scene switching", style = MaterialTheme.typography.titleLarge)
        ExposedDropdownMenuBox(
            expanded = transitionExpanded,
            onExpandedChange = { transitionExpanded = it },
        ) {
            OutlinedTextField(
                value = transition.toString(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Transition") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = transitionExpanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(
                expanded = transitionExpanded,
                onDismissRequest = { transitionExpanded = false },
            ) {
                SettingsSceneSwitchTransition.entries.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item.toString()) },
                        onClick = {
                            database.sceneSwitchTransition.value = item
                            model.setSceneSwitchTransition()
                            transitionExpanded = false
                        },
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Force transition")
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = forceTransition,
                onCheckedChange = { value ->
                    database.forceSceneSwitchTransition.value = value
                    model.resetSelectedScene(changeScene = false, attachCamera = true)
                },
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Video blackish")
            Slider(
                value = cameraSwitchRemoveBlackish.toFloat(),
                onValueChange = { debug.cameraSwitchRemoveBlackish.value = it.toDouble() },
                valueRange = 0.0f..1.0f,
                steps = 9,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${formatOneDecimal(cameraSwitchRemoveBlackish)} s",
                modifier = Modifier.width(40.dp),
            )
        }
        Text(
            "Ingest, screen capture and media player video sources can instantly be switched " +
                "to, but if you want consistency you can force scene switch transitions to these as well."
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemoteSceneView(model: Model) {
    var selectedSceneId by remember { mutableStateOf(model.database.remoteSceneId.value) }
    var expanded by remember { mutableStateOf(false) }
    val scenes by model.database.scenes.collectAsState()
    val selectedScene = scenes.firstOrNull { it.id == selectedSceneId }

    Column {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            OutlinedTextField(
                value = selectedScene?.name?.value ?: "-- None --",
                onValueChange = {},
                readOnly = true,
                label = { Text("Remote scene") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text("-- None --") },
                    onClick = {
                        selectedSceneId = null
                        model.database.remoteSceneId.value = null
                        model.remoteSceneSettingsUpdated()
                        expanded = false
                    },
                )
                scenes.forEach { scene ->
                    DropdownMenuItem(
                        text = { SceneNameView(scene = scene) },
                        onClick = {
                            selectedSceneId = scene.id
                            model.database.remoteSceneId.value = scene.id
                            model.remoteSceneSettingsUpdated()
                            expanded = false
                        },
                    )
                }
            }
        }
        Text(
            "Widgets in selected scene will be shown on the Moblin device the remote control " +
                "assistant is connected to."
        )
    }
}

@Composable
private fun GraphicsView(
    model: Model,
    database: Database,
    onNavigate: (String) -> Unit,
) {
    Text(
        text = "Graphics",
        modifier = Modifier.clickable { onNavigate("Graphics") },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GraphicsDetail(
    model: Model,
    database: Database,
) {
    val implementation by database.graphicsImplementation.collectAsState()
    val highQualityDownsampling by database.graphicsHighQualityDownsampling.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text("Graphics", style = MaterialTheme.typography.titleLarge)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            OutlinedTextField(
                value = implementation.toString(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Implementation") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                SettingsGraphicsImplementation.entries.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item.toString()) },
                        onClick = {
                            database.graphicsImplementation.value = item
                            model.setGraphicsImplementation()
                            expanded = false
                        },
                    )
                }
            }
        }
        if (implementation == SettingsGraphicsImplementation.metalPetal) {
            Text("⚠️ MetalPetal does not work when Moblin is in background.")
        }
        Text(
            "Core Image is Apple's image processing framework. MetalPetal is experimental. " +
                "MetalPetal provides similar image processing, and hopefully uses less system resources."
        )
        if (implementation == SettingsGraphicsImplementation.coreImage) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("High quality downsampling")
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = highQualityDownsampling,
                    onCheckedChange = { value ->
                        database.graphicsHighQualityDownsampling.value = value
                        model.setHighQualityDownsampling()
                    },
                )
            }
            Text(
                "High quality downsampling makes downscaled images look better, but uses " +
                    "more system resources."
            )
        }
    }
}

@Composable
fun SceneNameView(scene: SettingsScene) {
    val name by scene.name.collectAsState()
    Text(name)
}

@Composable
fun ScenesSettingsView(
    model: Model,
    database: Database,
    onNavigate: (String) -> Unit,
) {
    val showAllSettings by database.showAllSettings.collectAsState()

    Column {
        Text("Scenes", style = MaterialTheme.typography.titleLarge)
        ScenesListView(model = model, database = database, onNavigate = onNavigate)
        WidgetsSettingsView(database = database)
        if (showAllSettings) {
            SceneSwitching(
                model = model,
                database = database,
                debug = database.debug,
                onNavigate = onNavigate,
            )
            AutoSwitchersSettingsView(
                autoSceneSwitchers = database.autoSceneSwitchers.value,
                showSelector = true,
            )
            DisconnectProtectionSettingsView(
                database = database,
                disconnectProtection = database.disconnectProtection.value,
            )
            RemoteSceneView(model = model)
            GraphicsView(model = model, database = database, onNavigate = onNavigate)
        }
    }
}

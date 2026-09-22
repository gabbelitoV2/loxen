package com.moblin.android.view.settings.scenes.autoswitchers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
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
import com.moblin.android.common.various.formatShortDuration
import com.moblin.android.localized
import com.moblin.android.various.model.AutoSceneSwitcherProvider
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.deleteAutoSceneSwitchers
import com.moblin.android.various.model.setAutoSceneSwitcher
import com.moblin.android.various.settings.SettingsAutoSceneSwitcher
import com.moblin.android.various.settings.SettingsAutoSceneSwitcherScene
import com.moblin.android.various.settings.SettingsAutoSceneSwitchers
import com.moblin.android.various.utils.makeOffsets
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.scenes.SceneNameView
import com.moblin.android.view.utils.AddButtonView
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.DraggableItemPrefixView
import com.moblin.android.view.utils.DraggableItemTextView
import com.moblin.android.view.utils.NameEditView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import java.util.UUID
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

private const val AUTO_SWITCHERS_VIEW_DESTINATION = "AutoSwitchersView"
private const val AUTO_SWITCHER_SETTINGS_VIEW_DESTINATION = "AutoSwitcherSettingsView"
private const val AUTO_SWITCHER_SCENE_SETTINGS_VIEW_DESTINATION = "AutoSwitcherSceneSettingsView"

private fun getSceneName(model: Model, sceneId: UUID?): String {
    if (sceneId != null) {
        val sceneName = model.database.scenes.firstOrNull { it.id == sceneId }?.name
        if (sceneName != null) {
            return sceneName
        }
    }
    return localized("-- None --")
}

private fun deleteAutoSceneSwitcher(model: Model, offsets: Set<Int>) {
    model.deleteAutoSceneSwitchers(offsets)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwitcherTimePickerView(time: Int, onTimeChange: (Int) -> Unit) {
    val times = listOf(5, 10, 15, 30, 45, 60, 90, 120, 180, 240, 300)
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = formatShortDuration(time),
            onValueChange = {},
            readOnly = true,
            label = { Text("Time") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            times.forEach { value ->
                DropdownMenuItem(
                    text = { Text(formatShortDuration(value)) },
                    onClick = {
                        onTimeChange(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutoSwitcherSceneSettingsDestinationView(
    model: Model = LocalModel.current,
    scene: SettingsAutoSceneSwitcherScene
) {
    val sceneId = scene.sceneId
    val time = scene.time
    var expanded by remember { mutableStateOf(false) }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = getSceneName(model, sceneId),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Scene") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text(localized("-- None --")) },
                        onClick = {
                            scene.sceneId = null
                            expanded = false
                        }
                    )
                    model.database.scenes.forEach { item ->
                        DropdownMenuItem(
                            text = { SceneNameView(item) },
                            onClick = {
                                scene.sceneId = item.id
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
        item {
            SwitcherTimePickerView(time = time, onTimeChange = { scene.time = it })
        }
    }
}

@Composable
private fun AutoSwitcherSceneSettingsView(
    model: Model = LocalModel.current,
    scene: SettingsAutoSceneSwitcherScene,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    val sceneId = scene.sceneId
    val time = scene.time
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onNavigate("$AUTO_SWITCHER_SCENE_SETTINGS_VIEW_DESTINATION/${scene.id}")
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        DraggableItemPrefixView()
        Text(getSceneName(model, sceneId))
        Spacer(Modifier.weight(1f))
        Text(formatShortDuration(time))
    }
}

@Composable
private fun AutoSwitcherScenesSettingsView(
    model: Model = LocalModel.current,
    autoSwitcher: SettingsAutoSceneSwitcher,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    val scenes = autoSwitcher.scenes
    val onDeleteScene: (SettingsAutoSceneSwitcherScene) -> Unit = { target ->
        autoSwitcher.scenes = autoSwitcher.scenes.filterNot { it.id == target.id }.toMutableList()
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        scenes.forEach { scene ->
            AutoSwitcherSceneSettingsView(model = model, scene = scene, onNavigate = onNavigate)
            TODO("no Android counterpart for context menu delete button")
        }
        TODO("no Android counterpart for drag to reorder list items")
        TODO("no Android counterpart for swipe to delete list items")
        AddButtonView {
            autoSwitcher.scenes = (autoSwitcher.scenes + SettingsAutoSceneSwitcherScene()).toMutableList()
        }
        SwipeLeftToDeleteHelpView(localized("a scene"))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutoSwitcherSettingsView(
    model: Model = LocalModel.current,
    autoSceneSwitchers: SettingsAutoSceneSwitchers,
    autoSwitcher: SettingsAutoSceneSwitcher,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    val name = autoSwitcher.name
    val shuffle = autoSwitcher.shuffle
    val switchers = autoSceneSwitchers.switchers
    Scaffold(
        topBar = { TopAppBar(title = { Text("Auto scene switcher") }) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                NameEditView(
                    name = name,
                    onNameChange = { autoSwitcher.name = it },
                    existingNames = switchers
                )
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Shuffle")
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = shuffle,
                        onCheckedChange = { autoSwitcher.shuffle = it }
                    )
                }
            }
            item {
                AutoSwitcherScenesSettingsView(
                    model = model,
                    autoSwitcher = autoSwitcher,
                    onNavigate = onNavigate
                )
            }
        }
    }
}

@Composable
private fun AutoSwitcherSettingsItemView(
    autoSceneSwitchers: SettingsAutoSceneSwitchers,
    autoSwitcher: SettingsAutoSceneSwitcher,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    val name = autoSwitcher.name
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onNavigate("$AUTO_SWITCHER_SETTINGS_VIEW_DESTINATION/${autoSwitcher.id}")
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        DraggableItemTextView(name)
    }
}

@Composable
private fun AutoSceneSwitcherItemView(autoSceneSwitcher: SettingsAutoSceneSwitcher) {
    val name = autoSceneSwitcher.name
    Text(name)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoSwitchersSelectView(
    model: Model = LocalModel.current,
    autoSceneSwitcher: AutoSceneSwitcherProvider,
    autoSceneSwitchers: SettingsAutoSceneSwitchers
) {
    val currentSwitcherId by autoSceneSwitcher.currentSwitcherId.collectAsState()
    val switchers = autoSceneSwitchers.switchers
    var expanded by remember { mutableStateOf(false) }
    val currentSwitcher = switchers.firstOrNull { it.id == currentSwitcherId }
    val currentName: String = if (currentSwitcher == null) {
        localized("-- None --")
    } else {
        val name = currentSwitcher.name
        name
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = currentName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Current") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text(localized("-- None --")) },
                    onClick = {
                        autoSceneSwitcher.currentSwitcherId.value = null
                        expanded = false
                    }
                )
                switchers.forEach { switcher ->
                    DropdownMenuItem(
                        text = { AutoSceneSwitcherItemView(switcher) },
                        onClick = {
                            autoSceneSwitcher.currentSwitcherId.value = switcher.id
                            expanded = false
                        }
                    )
                }
            }
        }
    }
    LaunchedEffect(currentSwitcherId) {
        model.setAutoSceneSwitcher(currentSwitcherId)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoSwitchersView(
    model: Model = LocalModel.current,
    autoSceneSwitchers: SettingsAutoSceneSwitchers,
    showSelector: Boolean,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    val switchers = autoSceneSwitchers.switchers
    val onDeleteAutoSwitcher: (SettingsAutoSceneSwitcher) -> Unit = { target ->
        val offsets = makeOffsets(autoSceneSwitchers.switchers, target.id)
        if (offsets != null) {
            deleteAutoSceneSwitcher(model, setOf(offsets))
        }
    }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Auto scene switchers") }) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (showSelector) {
                item {
                    AutoSwitchersSelectView(
                        model = model,
                        autoSceneSwitcher = model.autoSceneSwitcher,
                        autoSceneSwitchers = autoSceneSwitchers
                    )
                }
            }
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    switchers.forEach { autoSwitcher ->
                        AutoSwitcherSettingsItemView(
                            autoSceneSwitchers = autoSceneSwitchers,
                            autoSwitcher = autoSwitcher,
                            onNavigate = onNavigate
                        )
                        TODO("no Android counterpart for context menu delete button")
                    }
                    TODO("no Android counterpart for drag to reorder list items")
                    TODO("no Android counterpart for swipe to delete list items")
                    CreateButtonView {
                        val switcher = SettingsAutoSceneSwitcher()
                        switcher.name = makeUniqueName(
                            SettingsAutoSceneSwitcher.baseName,
                            autoSceneSwitchers.switchers
                        )
                        autoSceneSwitchers.switchers =
                            (autoSceneSwitchers.switchers + switcher).toMutableList()
                    }
                    SwipeLeftToDeleteHelpView(localized("an auto scene switcher"))
                }
            }
        }
    }
}

@Composable
fun AutoSwitchersSettingsView(
    autoSceneSwitchers: SettingsAutoSceneSwitchers,
    showSelector: Boolean,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onNavigate("$AUTO_SWITCHERS_VIEW_DESTINATION?showSelector=$showSelector")
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Auto scene switchers")
    }
}

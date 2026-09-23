package com.moblin.android.view.settings.scenes.autoswitchers

import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.common.various.formatShortDuration
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.various.model.AutoSceneSwitcherProvider
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.deleteAutoSceneSwitchers
import com.moblin.android.various.model.setAutoSceneSwitcher
import com.moblin.android.various.settings.SettingsAutoSceneSwitcher
import com.moblin.android.various.settings.SettingsAutoSceneSwitcherScene
import com.moblin.android.various.settings.SettingsAutoSceneSwitchers
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.utils.AddButtonView
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.DraggableItemPrefixView
import com.moblin.android.view.utils.DraggableItemTextView
import com.moblin.android.view.utils.NameEditView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import java.util.UUID

private val switcherTimes: List<Int> = listOf(5, 10, 15, 30, 45, 60, 90, 120, 180, 240, 300)

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

@Composable
fun SwitcherTimePickerView(time: Int, onTimeChange: (Int) -> Unit) {
    Picker(
        title = "Time",
        selection = time,
        options = switcherTimes,
        text = { formatShortDuration(it) },
        onChange = onTimeChange
    )
}

@Composable
private fun AutoSwitcherSceneSettingsView(
    model: Model = LocalModel.current,
    scene: SettingsAutoSceneSwitcherScene,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    NavigationLink(
        destination = {
            Form {
                Section {
                    val sceneOptions: List<UUID?> =
                        listOf(null) + model.database.scenes.map { it.id }
                    Picker(
                        title = "Scene",
                        selection = scene.sceneId,
                        options = sceneOptions,
                        text = { getSceneName(model, it) },
                        onChange = { scene.sceneId = it }
                    )
                    SwitcherTimePickerView(
                        time = scene.time,
                        onTimeChange = { scene.time = it }
                    )
                }
            }
        },
        label = {
            DraggableItemPrefixView()
            Text(getSceneName(model, scene.sceneId))
            Spacer(Modifier.weight(1f))
            Text(formatShortDuration(scene.time))
        }
    )
}

@Composable
private fun AutoSwitcherScenesSettingsView(
    model: Model = LocalModel.current,
    autoSwitcher: SettingsAutoSceneSwitcher,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    Section(footerContent = { SwipeLeftToDeleteHelpView(localized("a scene")) }) {
        autoSwitcher.scenes.forEach { scene ->
            key(scene.id) {
                AutoSwitcherSceneSettingsView(model = model, scene = scene)
            }
        }
        AddButtonView {
            autoSwitcher.scenes = (autoSwitcher.scenes + SettingsAutoSceneSwitcherScene()).toMutableList()
        }
    }
}

@Composable
private fun AutoSwitcherSettingsView(
    model: Model = LocalModel.current,
    autoSceneSwitchers: SettingsAutoSceneSwitchers,
    autoSwitcher: SettingsAutoSceneSwitcher,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    Form(title = "Auto scene switcher") {
        Section {
            NameEditView(
                name = autoSwitcher.name,
                onNameChange = { autoSwitcher.name = it },
                existingNames = autoSceneSwitchers.switchers
            )
        }
        Section {
            Toggle(
                title = "Shuffle",
                isOn = autoSwitcher.shuffle,
                onChange = { autoSwitcher.shuffle = it }
            )
        }
        AutoSwitcherScenesSettingsView(model = model, autoSwitcher = autoSwitcher)
    }
}

@Composable
private fun AutoSwitcherSettingsItemView(
    autoSceneSwitchers: SettingsAutoSceneSwitchers,
    autoSwitcher: SettingsAutoSceneSwitcher,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    NavigationLink(
        destination = {
            AutoSwitcherSettingsView(
                autoSceneSwitchers = autoSceneSwitchers,
                autoSwitcher = autoSwitcher
            )
        },
        label = {
            DraggableItemTextView(autoSwitcher.name)
        }
    )
}

@Composable
private fun AutoSceneSwitcherItemView(autoSceneSwitcher: SettingsAutoSceneSwitcher) {
    Text(autoSceneSwitcher.name)
}

@Composable
fun AutoSwitchersSelectView(
    model: Model = LocalModel.current,
    autoSceneSwitcher: AutoSceneSwitcherProvider,
    autoSceneSwitchers: SettingsAutoSceneSwitchers
) {
    val currentSwitcherId by autoSceneSwitcher.currentSwitcherId.collectAsState()
    val switchers = autoSceneSwitchers.switchers
    val switcherOptions: List<UUID?> = listOf(null) + switchers.map { it.id }
    Section {
        Picker(
            title = "Current",
            selection = currentSwitcherId,
            options = switcherOptions,
            text = { id ->
                if (id == null) {
                    localized("-- None --")
                } else {
                    switchers.firstOrNull { it.id == id }?.name ?: localized("-- None --")
                }
            },
            onChange = {
                autoSceneSwitcher.currentSwitcherId.value = it
                model.setAutoSceneSwitcher(it)
            }
        )
    }
}

@Composable
fun AutoSwitchersView(
    model: Model = LocalModel.current,
    autoSceneSwitchers: SettingsAutoSceneSwitchers,
    showSelector: Boolean,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    Form(title = "Auto scene switchers") {
        if (showSelector) {
            AutoSwitchersSelectView(
                model = model,
                autoSceneSwitcher = model.autoSceneSwitcher,
                autoSceneSwitchers = autoSceneSwitchers
            )
        }
        Section(footerContent = { SwipeLeftToDeleteHelpView(localized("an auto scene switcher")) }) {
            autoSceneSwitchers.switchers.forEach { autoSwitcher ->
                key(autoSwitcher.id) {
                    AutoSwitcherSettingsItemView(
                        autoSceneSwitchers = autoSceneSwitchers,
                        autoSwitcher = autoSwitcher
                    )
                }
            }
            CreateButtonView {
                val switcher = SettingsAutoSceneSwitcher()
                switcher.name = makeUniqueName(
                    SettingsAutoSceneSwitcher.baseName,
                    autoSceneSwitchers.switchers
                )
                autoSceneSwitchers.switchers =
                    (autoSceneSwitchers.switchers + switcher).toMutableList()
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
    NavigationLink(
        destination = {
            AutoSwitchersView(
                autoSceneSwitchers = autoSceneSwitchers,
                showSelector = showSelector
            )
        },
        label = {
            Text(localized("Auto scene switchers"))
        }
    )
}

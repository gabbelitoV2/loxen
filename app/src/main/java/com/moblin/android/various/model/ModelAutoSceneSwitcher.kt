package com.moblin.android.various.model

import android.util.Log
import com.moblin.android.various.settings.SettingsAutoSceneSwitcher
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AutoSceneSwitcherProvider {
    internal var switchTime: Instant? = null
    internal var sceneIds: MutableList<UUID> = mutableListOf()
    internal var currentSwitcherSceneId: UUID? = null
    val currentSwitcherId = MutableStateFlow<UUID?>(null)
}

fun Model.setAutoSceneSwitcher(id: UUID?) {
    autoSceneSwitcher.currentSwitcherId.value = id
    database.autoSceneSwitchers.switcherId = id
    autoSceneSwitcher.switchTime = Instant.now()
    autoSceneSwitcher.sceneIds.clear()
    remoteControlStateChanged(RemoteControlState(autoSceneSwitcher = RemoteControlAutoSceneSwitcher(id = id)))
}

fun Model.deleteAutoSceneSwitchers(offsets: Set<Int>) {
    offsets.sortedDescending().forEach { database.autoSceneSwitchers.switchers.removeAt(it) }
    if (database.autoSceneSwitchers.switchers.none { it.id == autoSceneSwitcher.currentSwitcherId.value }) {
        autoSceneSwitcher.currentSwitcherId.value = null
        setAutoSceneSwitcher(id = null)
    }
}

fun Model.updateAutoSceneSwitcher(now: Instant, forceSwitch: Boolean = false) {
    val switcherId = autoSceneSwitcher.currentSwitcherId.value ?: return
    if (!forceSwitch) {
        val switchTime = autoSceneSwitcher.switchTime
        if (switchTime != null && now <= switchTime) {
            return
        }
    }
    val autoSwitcher = database.autoSceneSwitchers.switchers.firstOrNull { it.id == switcherId } ?: return
    fillAutoSceneSwitcherIfNeeded(autoSwitcher = autoSwitcher)
    if (!trySwitchToNextScene(autoSwitcher = autoSwitcher, now = now)) {
        fillAutoSceneSwitcherIfNeeded(autoSwitcher = autoSwitcher)
        if (!trySwitchToNextScene(autoSwitcher = autoSwitcher, now = now)) {
            Log.i("Model", "No scene to auto switch to")
        }
    }
}

private fun Model.fillAutoSceneSwitcherIfNeeded(autoSwitcher: SettingsAutoSceneSwitcher) {
    if (autoSceneSwitcher.sceneIds.isEmpty()) {
        autoSceneSwitcher.sceneIds = autoSwitcher.scenes.map { it.id }.reversed().toMutableList()
        if (autoSwitcher.shuffle) {
            autoSceneSwitcher.sceneIds.shuffle()
            if (autoSceneSwitcher.sceneIds.lastOrNull() == autoSceneSwitcher.currentSwitcherSceneId) {
                autoSceneSwitcher.sceneIds.removeLastOrNull()?.let { switcherSceneId ->
                    autoSceneSwitcher.sceneIds.add(0, switcherSceneId)
                }
            }
        }
    }
}

private fun Model.trySwitchToNextScene(autoSwitcher: SettingsAutoSceneSwitcher, now: Instant): Boolean {
    while (true) {
        val switcherSceneId = autoSceneSwitcher.sceneIds.removeLastOrNull() ?: break
        val switcherScene = autoSwitcher.scenes.firstOrNull { it.id == switcherSceneId } ?: continue
        val sceneId = switcherScene.sceneId ?: continue
        if (enabledScenes.none { it.id == sceneId }) {
            continue
        }
        if (!isSceneVideoSourceActive(sceneId = sceneId)) {
            continue
        }
        selectScene(id = sceneId)
        autoSceneSwitcher.switchTime = now.plusMillis((switcherScene.time * 1000).toLong())
        autoSceneSwitcher.currentSwitcherSceneId = switcherSceneId
        return true
    }
    return false
}

fun Model.updateAutoSceneSwitcherVideoSourceDisconnected() {
    if (autoSceneSwitcher.currentSwitcherId.value == null) {
        return
    }
    val currentSceneId = autoSceneSwitcher.currentSwitcherSceneId ?: return
    if (isSceneVideoSourceActive(sceneId = currentSceneId)) {
        return
    }
    updateAutoSceneSwitcher(now = Instant.now(), forceSwitch = true)
}

fun Model.updateAutoSceneSwitcherButtonState() {
    var isOn = false
    if (database.autoSceneSwitchers.switcherId != null) {
        isOn = true
    }
    if (showingPanel == ShowingPanel.autoSceneSwitcher) {
        isOn = true
    }
    setQuickButton(type = QuickButtonType.autoSceneSwitcher, isOn = isOn)
}

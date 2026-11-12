package com.moblin.android.various.model

import com.moblin.android.platform.avfoundation.AVSpeechSynthesisVoice
import com.moblin.android.platform.uikit.UIFont
import com.moblin.android.platform.uikit.jpegData
import com.moblin.android.remotecontrol.RemoteControlAppleVoice
import com.moblin.android.remotecontrol.RemoteControlAppleVoices
import com.moblin.android.remotecontrol.RemoteControlEditorOptions
import com.moblin.android.remotecontrol.RemoteControlFontFamily
import com.moblin.android.remotecontrol.RemoteControlNamedItem
import com.moblin.android.remotecontrol.RemoteControlSize
import com.moblin.android.remotecontrol.RemoteControlVideoSource
import com.moblin.android.remotecontrol.RemoteControlWebRemote
import com.moblin.android.remotecontrol.hasUniqueRemoteControlIds
import com.moblin.android.remotecontrol.isValidRemoteControlName
import com.moblin.android.remotecontrol.isValidRemoteControlScene
import com.moblin.android.remotecontrol.isValidRemoteControlSceneUpdate
import com.moblin.android.remotecontrol.isValidRemoteControlWidgetScenes
import com.moblin.android.various.settings.SettingsAlertsMediaGallery
import com.moblin.android.various.settings.SettingsScene
import com.moblin.android.various.settings.SettingsVideoSource
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetScoreboardPlayer
import com.moblin.android.various.settings.SettingsWidgetType
import com.moblin.android.various.network.IPMonitor
import com.moblin.android.various.network.makeMdnsHostname
import com.moblin.android.various.utils.isPhone
import com.moblin.android.various.utils.makeUniqueName
import java.util.UUID
import com.moblin.android.common.various.deepCopy

fun emptyRemoteControlEditorOptions(): RemoteControlEditorOptions =
    RemoteControlEditorOptions(
        fonts = emptyList(),
        luts = emptyList(),
        scoreboardPlayers = emptyList(),
        webRemote = RemoteControlWebRemote(enabled = false, port = 0.toUShort(), deviceName = "", addresses = emptyList()),
        workoutDevices = emptyList(),
        streamDeckLayouts = emptyList(),
        appleVoices = emptyList(),
        showAllSettings = false,
        isPhone = false,
        forceSceneSwitchTransition = false,
        workoutActive = false,
        inactiveVideoSourceSceneIds = emptyList(),
        canvasSize = RemoteControlSize(width = 1920.0, height = 1080.0),
    )

fun Model.remoteControlGetScenes(): List<SettingsScene> {
    return database.scenes
}

fun Model.remoteControlGetWidgets(secrets: Boolean): List<SettingsWidget> {
    if (secrets) {
        return database.widgets
    }
    return database.widgets.mapNotNull {
        val widget: SettingsWidget? = it.deepCopy()
        widget?.removeSecrets()
        widget
    }
}

fun Model.remoteControlCreateScene(scene: SettingsScene): Boolean {
    if (database.scenes.any { it.id == scene.id }) {
        return false
    }
    if (scene.name.isEmpty()) {
        scene.name = makeUniqueName(name = SettingsScene.baseName, existingNames = database.scenes)
    }
    val newScene = SettingsScene(name = scene.name)
    newScene.id = scene.id
    val cameras = remoteControlCameras()
    if (!isValidRemoteControlScene(scene = scene, scenes = database.scenes + scene, database = database) ||
        !isValidRemoteControlSceneUpdate(scene = newScene, new = scene, database = database, cameras = cameras)
    ) {
        return false
    }
    database.scenes.add(newScene)
    remoteControlApplySceneUpdate(scene = newScene, new = scene, creating = true)
    return true
}

fun Model.remoteControlUpdateScene(scene: SettingsScene): Boolean {
    val index = database.scenes.indexOfFirst { it.id == scene.id }
    if (index == -1) {
        return false
    }
    val oldScene = database.scenes[index]
    val scenes = database.scenes.toMutableList()
    scenes[index] = scene
    val cameras = remoteControlCameras()
    if (!isValidRemoteControlScene(scene = scene, scenes = scenes, database = database) ||
        !isValidRemoteControlSceneUpdate(scene = oldScene, new = scene, database = database, cameras = cameras)
    ) {
        return false
    }
    remoteControlApplySceneUpdate(scene = oldScene, new = scene, creating = false)
    return true
}

fun Model.remoteControlDeleteScene(id: UUID): Boolean {
    val scene = database.findScene(id) ?: return false
    deleteScene(scene = scene)
    return true
}

fun Model.remoteControlCreateWidget(widget: SettingsWidget, sceneIds: List<UUID>, secrets: Boolean): Boolean {
    if (!secrets) {
        widget.keepSecrets(from = SettingsWidget(name = ""))
    }
    if (database.widgets.any { it.id == widget.id }) {
        return false
    }
    val newWidget = remoteControlMakeWidget(widget = widget) ?: return false
    if (!isValidRemoteControlName(newWidget, database.widgets + newWidget, database.widgets) ||
        !isValidRemoteControlWidgetScenes(newWidget, sceneIds, database = database)
    ) {
        return false
    }
    widgetWizardCreate(
        type = newWidget.type,
        name = newWidget.name,
        widget = newWidget,
        scenes = database.scenes.filter { sceneIds.contains(it.id) },
    )
    return true
}

fun Model.remoteControlUpdateWidget(widget: SettingsWidget, secrets: Boolean): Boolean {
    val oldWidget = database.findWidget(widget.id) ?: return false
    if (!secrets) {
        widget.keepSecrets(from = oldWidget)
    }
    if (!isValidRemoteControlName(widget, database.widgets, database.widgets)) {
        return false
    }
    remoteControlApplyWidgetUpdate(widget = oldWidget, new = widget, creating = false)
        ?: return false
    return true
}

fun Model.remoteControlDeleteWidget(id: UUID): Boolean {
    val widget = database.findWidget(id) ?: return false
    deleteWidget(widget = widget)
    return true
}

fun Model.remoteControlGetEditorOptions(): RemoteControlEditorOptions {
    val web = database.remoteControl.web
    val addresses = mutableListOf<String>()
    if (web.deviceName.isNotEmpty()) {
        addresses.add(makeMdnsHostname(deviceName = web.deviceName))
    }
    for (status in statusOther.ipStatuses.value) {
        if (status.ipType == IPMonitor.IPType.ipv4) {
            if (status.interfaceType == IPMonitor.InterfaceType.wifi ||
                status.interfaceType == IPMonitor.InterfaceType.wiredEthernet
            ) {
                addresses.add(status.ip)
            }
        }
    }
    val appleVoices = AVSpeechSynthesisVoice.speechVoices()
        .groupBy { it.language.take(2) }
        .map { (language, voices) ->
            RemoteControlAppleVoices(
                language = language,
                voices = voices.map {
                    RemoteControlAppleVoice(
                        name = it.name,
                        identifier = it.identifier,
                        language = it.language,
                    )
                },
            )
        }
        .sortedBy { it.language }
    val canvasSize = media.getCanvasSize()
    return RemoteControlEditorOptions(
        fonts = UIFont.familyNames.sorted().map {
            RemoteControlFontFamily(family = it, styles = UIFont.fontNames(forFamilyName = it))
        },
        luts = database.color.allLuts().map { RemoteControlNamedItem(id = it.id, name = it.name) },
        scoreboardPlayers = database.scoreboardPlayers,
        webRemote = RemoteControlWebRemote(
            enabled = web.enabled,
            port = web.port.toUShort(),
            deviceName = web.deviceName,
            addresses = addresses,
        ),
        workoutDevices = database.workoutDevices.devices.map { RemoteControlNamedItem(id = it.id, name = it.name) },
        streamDeckLayouts = database.streamDecks.layouts.value.map { RemoteControlNamedItem(id = it.id, name = it.name) },
        appleVoices = appleVoices,
        showAllSettings = database.showAllSettings,
        isPhone = isPhone(),
        forceSceneSwitchTransition = database.forceSceneSwitchTransition,
        workoutActive = workoutType != null,
        inactiveVideoSourceSceneIds = database.scenes.filter { !isSceneVideoSourceActive(scene = it) }
            .map { it.id },
        canvasSize = RemoteControlSize(width = canvasSize.width.toDouble(), height = canvasSize.height.toDouble()),
    )
}

fun Model.remoteControlGetVideoSourceSnapshot(id: UUID, onCompleted: (ByteArray?) -> Unit) {
    val widget = database.findWidget(id)
    if (widget == null) {
        onCompleted(null)
        return
    }
    takeVideoSourcePreviewImage(widget = widget) { image ->
        onCompleted(image?.jpegData(compressionQuality = 0.8f))
    }
}

fun Model.remoteControlSetScoreboardPlayers(players: List<SettingsWidgetScoreboardPlayer>): Boolean {
    if (!hasUniqueRemoteControlIds(players) ||
        !players.all { isValidRemoteControlName(it, players, database.scoreboardPlayers) }
    ) {
        return false
    }
    database.scoreboardPlayers = players.toMutableList()
    scoreboardPlayersChanged()
    return true
}

fun Model.remoteControlGetAlertsMediaGallery(): SettingsAlertsMediaGallery {
    return database.alertsMediaGallery
}

fun Model.remoteControlGetVideoSources(): List<RemoteControlVideoSource> {
    return videoSources.all().map { camera ->
        var videoSource = SettingsVideoSource()
        videoSource = videoSource.updatingCameraId(settingsCameraId = cameraIdToSettingsCameraId(camera.id))
        RemoteControlVideoSource(
            id = camera.id,
            name = camera.name,
            position = videoSource.cameraPosition,
        )
    }
}

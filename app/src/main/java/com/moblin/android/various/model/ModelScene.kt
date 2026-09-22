package com.moblin.android.various.model

import android.location.Location
import androidx.camera.core.CameraInfo
import com.moblin.android.localized
import com.moblin.android.media.haishinkit.media.video.CaptureDevice
import com.moblin.android.media.haishinkit.media.video.CaptureDevices
import com.moblin.android.remotecontrol.RemoteControlAssistantStreamerState
import com.moblin.android.remotecontrol.remoteControlStateChanged
import com.moblin.android.remotecontrol.remoteControlAssistantSetRemoteSceneSettings
import com.moblin.android.remotecontrol.remoteControlAssistantSetRemoteSceneDataLocation
import com.moblin.android.remotecontrol.remoteControlAssistantSetRemoteSceneDataVariables
import com.moblin.android.various.AudioPlayer
import com.moblin.android.various.Variables
import com.moblin.android.various.createVariables
import com.moblin.android.various.settings.MacroEvent
import com.moblin.android.various.settings.SettingsAlignment
import com.moblin.android.various.settings.SettingsMacrosEvent
import com.moblin.android.various.settings.SettingsQuickButtonType
import com.moblin.android.various.settings.SettingsScene
import com.moblin.android.various.settings.SettingsSceneCameraPosition
import com.moblin.android.various.settings.SettingsSceneWidget
import com.moblin.android.various.settings.SettingsVideoEffect
import com.moblin.android.various.settings.SettingsVideoSource
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetAlertPositionType
import com.moblin.android.various.settings.SettingsWidgetAlerts
import com.moblin.android.various.settings.SettingsWidgetPomodoroTimer
import com.moblin.android.various.settings.SettingsWidgetScene
import com.moblin.android.various.settings.SettingsWidgetScoreboardSport
import com.moblin.android.various.settings.SettingsWidgetText
import com.moblin.android.various.settings.SettingsWidgetTextCheckbox
import com.moblin.android.various.settings.SettingsWidgetTextLapTimes
import com.moblin.android.various.settings.SettingsWidgetTextRating
import com.moblin.android.various.settings.SettingsWidgetTextStopwatch
import com.moblin.android.various.settings.SettingsWidgetTextSubtitles
import com.moblin.android.various.settings.SettingsWidgetTextTimer
import com.moblin.android.various.settings.SettingsWidgetType
import com.moblin.android.various.settings.SettingsWidgetVTuberType
import com.moblin.android.various.settings.SettingsWidgetWheelOfLuckOption
import com.moblin.android.various.settings.PomodoroPhase
import com.moblin.android.various.utils.utcTimeDeltaFromNow
import com.moblin.android.videoeffects.AnamorphicLensEffect
import com.moblin.android.videoeffects.BeautyEffect
import com.moblin.android.videoeffects.BingoCardEffect
import com.moblin.android.videoeffects.BrowserEffect
import com.moblin.android.videoeffects.CameraManEffect
import com.moblin.android.videoeffects.ChatEffect
import com.moblin.android.videoeffects.ChatEmoteComboEffect
import com.moblin.android.videoeffects.FaceEffect
import com.moblin.android.videoeffects.FixedHorizonEffect
import com.moblin.android.videoeffects.GrayScaleEffect
import com.moblin.android.videoeffects.ImageEffect
import com.moblin.android.videoeffects.LutEffect
import com.moblin.android.videoeffects.MapEffect
import com.moblin.android.videoeffects.MaskEffect
import com.moblin.android.videoeffects.MovieEffect
import com.moblin.android.videoeffects.OpacityEffect
import com.moblin.android.videoeffects.PinchEffect
import com.moblin.android.videoeffects.PixellateEffect
import com.moblin.android.videoeffects.PngTuberEffect
import com.moblin.android.videoeffects.PomodoroTimerEffect
import com.moblin.android.videoeffects.QrCodeEffect
import com.moblin.android.videoeffects.RemoveBackgroundEffect
import com.moblin.android.videoeffects.SepiaEffect
import com.moblin.android.videoeffects.ShapeEffect
import com.moblin.android.videoeffects.SlideshowEffect
import com.moblin.android.videoeffects.SlideshowEffectSlide
import com.moblin.android.videoeffects.SnapshotEffect
import com.moblin.android.videoeffects.TripleEffect
import com.moblin.android.videoeffects.TwinEffect
import com.moblin.android.videoeffects.VideoEffect
import com.moblin.android.videoeffects.VideoSourceEffect
import com.moblin.android.videoeffects.VTuberEffect
import com.moblin.android.videoeffects.WheelOfLuckEffect
import com.moblin.android.videoeffects.WhirlpoolEffect
import com.moblin.android.videoeffects.alerts.AlertsEffect
import com.moblin.android.videoeffects.alerts.alertsEffectBackgroundRightEyeRectangle
import com.moblin.android.videoeffects.dewarp360.Dewarp360Effect
import com.moblin.android.videoeffects.scoreboard.ScoreboardEffect
import com.moblin.android.videoeffects.text.TextEffect
import com.moblin.android.videoeffects.text.TextFormatPart
import com.moblin.android.videoeffects.text.loadTextFormat
import com.moblin.android.videoeffects.vtuber.VTuberLive2DEffect
import com.moblin.android.videoeffects.vtuber.VTuberVrmEffect
import java.net.URI
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private val mainScope = CoroutineScope(Dispatchers.Main)

class CreateWidgetWizard {
    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()
    private val _type = MutableStateFlow(SettingsWidgetType.TEXT)
    val type: StateFlow<SettingsWidgetType> = _type.asStateFlow()
    var widget: SettingsWidget = SettingsWidget(name = "")

    fun reset() {
        _name.value = ""
        _type.value = SettingsWidgetType.TEXT
        widget = SettingsWidget(name = "")
        widget.text.formatString = ""
        val yes = SettingsWidgetWheelOfLuckOption()
        yes.text = localized("Yes")
        val no = SettingsWidgetWheelOfLuckOption()
        no.text = localized("No")
        val later = SettingsWidgetWheelOfLuckOption()
        later.text = localized("Later")
        val spinAgain = SettingsWidgetWheelOfLuckOption()
        spinAgain.text = localized("Spin again")
        widget.wheelOfLuck.options = mutableListOf(yes, no, later, spinAgain)
        widget.wheelOfLuck.updateText()
        widget.wheelOfLuck.updateTotalWeight()
    }
}

data class WidgetInScene(val widget: SettingsWidget, val sceneWidget: SettingsSceneWidget) {
    val id: UUID
        get() = widget.id
}

const val defaultScoreboardSize = 18.52

fun Model.getTextEffects(id: UUID): List<TextEffect> {
    val effects = mutableListOf<TextEffect>()
    textEffects[id]?.let { effects.add(it) }
    for (slideshow in slideshowEffects.values) {
        for (slide in slideshow.slides) {
            if (slide.widgetId != id) {
                continue
            }
            (slide.effect as? TextEffect)?.let { effects.add(it) }
        }
    }
    return effects
}

fun Model.getVideoSourceEffect(id: UUID): VideoSourceEffect? {
    return videoSourceEffects[id]
}

fun Model.getVTuberEffect(id: UUID): VTuberEffect? {
    return vTuberEffects[id]
}

fun Model.getPngTuberEffect(id: UUID): PngTuberEffect? {
    return pngTuberEffects[id]
}

fun Model.getSnapshotEffect(id: UUID): SnapshotEffect? {
    return snapshotEffects[id]
}

fun Model.getChatEffect(id: UUID): ChatEffect? {
    return chatEffects[id]
}

fun Model.getChatEmoteComboEffect(id: UUID): ChatEmoteComboEffect? {
    return chatEmoteComboEffects[id]
}

fun Model.getQrCodeEffect(id: UUID): QrCodeEffect? {
    return qrCodeEffects[id]
}

fun Model.getWheelOfLuckEffect(id: UUID): WheelOfLuckEffect? {
    return wheelOfLuckEffects[id]
}

fun Model.getBingoCardEffect(id: UUID): BingoCardEffect? {
    return bingoCardEffects[id]
}

fun Model.getScoreboardEffect(id: UUID): ScoreboardEffect? {
    return scoreboardEffects[id]
}

fun Model.getWidgetShapeEffect(widget: SettingsWidget, effect: SettingsVideoEffect): ShapeEffect? {
    return getWidgetVideoEffect<ShapeEffect>(widget, effect)
}

fun Model.getWidgetAnamorphicLensEffect(
    widget: SettingsWidget,
    effect: SettingsVideoEffect
): AnamorphicLensEffect? {
    return getWidgetVideoEffect<AnamorphicLensEffect>(widget, effect)
}

fun Model.getWidgetDewarp360Effect(
    widget: SettingsWidget,
    effect: SettingsVideoEffect
): Dewarp360Effect? {
    return getWidgetVideoEffect<Dewarp360Effect>(widget, effect)
}

fun Model.getWidgetLutEffect(widget: SettingsWidget, effect: SettingsVideoEffect): LutEffect? {
    return getWidgetVideoEffect<LutEffect>(widget, effect)
}

fun Model.getWidgetOpacityEffect(widget: SettingsWidget, effect: SettingsVideoEffect): OpacityEffect? {
    return getWidgetVideoEffect<OpacityEffect>(widget, effect)
}

fun Model.getWidgetMaskEffect(widget: SettingsWidget, effect: SettingsVideoEffect): MaskEffect? {
    return getWidgetVideoEffect<MaskEffect>(widget, effect)
}

fun Model.getWidgetRemoveBackgroundEffect(
    widget: SettingsWidget,
    effect: SettingsVideoEffect
): RemoveBackgroundEffect? {
    return getWidgetVideoEffect<RemoveBackgroundEffect>(widget, effect)
}

private fun Model.getEffectWithPossibleEffects(id: UUID): VideoEffect? {
    return getVideoSourceEffect(id)
        ?: getImageEffect(id)
        ?: getBrowserEffect(id)
        ?: getMapEffect(id)
        ?: getSnapshotEffect(id)
        ?: getQrCodeEffect(id)
}

private inline fun <reified T> Model.getWidgetVideoEffect(
    widget: SettingsWidget,
    effect: SettingsVideoEffect
): T? {
    val effectIndex = widget.effects.filter { it.enabled }.indexOfFirst { it === effect }
    if (effectIndex == -1) {
        return null
    }
    val videoEffect = getEffectWithPossibleEffects(id = widget.id) ?: return null
    if (effectIndex >= videoEffect.effects.size) {
        return null
    }
    return videoEffect.effects[effectIndex] as? T
}

fun Model.isFixedHorizonEnabled(scene: SettingsScene): Boolean {
    return database.fixedHorizon && scene.videoSource.cameraPosition.isBuiltin()
}

fun Model.resetSelectedScene(changeScene: Boolean = true, attachCamera: Boolean = true) {
    if (enabledScenes.isNotEmpty() && changeScene) {
        setSceneId(id = enabledScenes[0].id)
        sceneSelector.sceneIndex = 0
    }
    resetVideoEffects(widgets = getLocalAndRemoteWidgets())
    drawOnStreamEffect.updateOverlay(
        videoSize = media.getCanvasSize(),
        size = drawOnStreamSize,
        lines = drawOnStream.lines,
        mirror = streamOverlay.isFrontCameraSelected && !database.mirrorFrontCameraOnStream
    )
    lutEffects.clear()
    for (lut in database.color.allLuts()) {
        val lutEffect = LutEffect()
        lutEffect.setLut(lut = lut.clone(), imageStorage = imageStorage) { title, subTitle ->
            makeErrorToast(title = title, subTitle = subTitle)
        }
        lutEffects[lut.id] = lutEffect
    }
    sceneUpdated(attachCamera = attachCamera)
}

fun Model.getSelectedScene(): SettingsScene? {
    return findEnabledScene(id = sceneSelector.selectedSceneId)
}

fun Model.showSceneSettings(scene: SettingsScene) {
    sceneSettingsPanelScene = scene
    sceneSettingsPanelSceneId += 1
    toggleShowingPanel(type = null, panel = ShowingPanel.NONE)
    toggleShowingPanel(type = null, panel = ShowingPanel.SCENE_SETTINGS)
}

fun Model.selectSceneByName(name: String) {
    val scene = enabledScenes.firstOrNull { it.name.lowercase() == name.lowercase() } ?: return
    selectScene(id = scene.id)
}

fun Model.selectScene(id: UUID) {
    if (id == sceneSelector.selectedSceneId) {
        return
    }
    val index = findEnabledSceneIndex(id = id) ?: return
    macrosEventOccurred(MacroEvent(event = SettingsMacrosEvent.SWITCH_SCENE, sceneId = id))
    sceneSelector.sceneIndex = index
    setSceneId(id = id)
    sceneUpdated(attachCamera = true, updateRemoteScene = false)
    switchMicIfNeededAfterSceneSwitch()
}

fun Model.setCurrentSceneVideoSource(cameraId: UUID) {
    val settingsCameraId = cameraIdToSettingsCameraId(cameraId = cameraId) ?: return
    getSelectedScene()?.updateCameraId(settingsCameraId = settingsCameraId)
    sceneUpdated(attachCamera = true, updateRemoteScene = false)
}

fun Model.toggleWidgetOnOff(id: UUID) {
    val widget = findWidget(id = id) ?: return
    widget.enabled = !widget.enabled
    sceneUpdated()
}

fun Model.sceneUpdated(attachCamera: Boolean = false, updateRemoteScene: Boolean = true) {
    val scene = getSelectedScene()
    if (scene == null) {
        sceneUpdatedOff()
        return
    }
    for (browserEffect in browserEffects.values) {
        browserEffect.stop()
    }
    sceneUpdatedOn(scene = scene, attachCamera = attachCamera)
    startWeatherManager()
    startGeographyManager()
    startGForceManager()
    if (updateRemoteScene) {
        remoteSceneSettingsUpdated()
    }
    updateStatusCameraText()
    updateSpeechToText()
}

fun Model.getSceneName(id: UUID?): String? {
    return database.scenes.firstOrNull { it.id == id }?.name
}

fun Model.getWidgetName(id: UUID?): String? {
    return database.widgets.firstOrNull { it.id == id }?.name
}

fun Model.removeDeadWidgetsFromScenes() {
    for (scene in database.scenes) {
        scene.widgets = scene.widgets.filter { findWidget(id = it.widgetId) != null }.toMutableList()
    }
}

fun Model.remoteSceneSettingsUpdated() {
    remoteSceneSettingsUpdateRequested = true
    updateRemoteSceneSettings()
}

fun Model.attachSingleLayout(scene: SettingsScene) {
    if (isChatPhone()) {
        return
    }
    streamOverlay.isFrontCameraSelected = false
    deactivateAllMediaPlayers()
    when (scene.videoSource.cameraPosition) {
        SettingsSceneCameraPosition.BACK ->
            attachCamera(scene = scene, position = SettingsSceneCameraPosition.BACK)
        SettingsSceneCameraPosition.FRONT -> {
            attachCamera(scene = scene, position = SettingsSceneCameraPosition.FRONT)
            streamOverlay.isFrontCameraSelected = true
        }
        SettingsSceneCameraPosition.RTMP ->
            attachBufferedCamera(cameraId = scene.videoSource.rtmpCameraId, scene = scene)
        SettingsSceneCameraPosition.SRTLA ->
            attachBufferedCamera(cameraId = scene.videoSource.srtlaCameraId, scene = scene)
        SettingsSceneCameraPosition.SRT_CLIENT ->
            attachBufferedCamera(cameraId = scene.videoSource.srtClientCameraId, scene = scene)
        SettingsSceneCameraPosition.RIST ->
            attachBufferedCamera(cameraId = scene.videoSource.ristCameraId, scene = scene)
        SettingsSceneCameraPosition.RTSP ->
            attachBufferedCamera(cameraId = scene.videoSource.rtspCameraId, scene = scene)
        SettingsSceneCameraPosition.WHIP ->
            attachBufferedCamera(cameraId = scene.videoSource.whipCameraId, scene = scene)
        SettingsSceneCameraPosition.WHEP ->
            attachBufferedCamera(cameraId = scene.videoSource.whepCameraId, scene = scene)
        SettingsSceneCameraPosition.MEDIA_PLAYER -> {
            mediaPlayers[scene.videoSource.mediaPlayerCameraId]?.activate()
            attachBufferedCamera(cameraId = scene.videoSource.mediaPlayerCameraId, scene = scene)
        }
        SettingsSceneCameraPosition.EXTERNAL ->
            attachExternalCamera(scene = scene)
        SettingsSceneCameraPosition.SCREEN_CAPTURE ->
            attachBufferedCamera(cameraId = screenCaptureCameraId, scene = scene)
        SettingsSceneCameraPosition.BACK_TRIPLE_LOW_ENERGY ->
            attachBackTripleLowEnergyCamera()
        SettingsSceneCameraPosition.BACK_DUAL_LOW_ENERGY ->
            attachBackDualLowEnergyCamera()
        SettingsSceneCameraPosition.BACK_WIDE_DUAL_LOW_ENERGY ->
            attachBackWideDualLowEnergyCamera()
        SettingsSceneCameraPosition.NONE ->
            attachBufferedCamera(cameraId = noneCameraId, scene = scene)
    }
}

fun Model.findWidget(id: UUID): SettingsWidget? {
    for (widget in getLocalAndRemoteWidgets()) {
        if (widget.id == id) {
            return widget
        }
    }
    return null
}

fun Model.findWidget(name: String): SettingsWidget? {
    for (widget in getLocalAndRemoteWidgets()) {
        if (widget.name == name) {
            return widget
        }
    }
    return null
}

fun Model.getTextWidget(id: UUID?): SettingsWidget? {
    if (id == null) {
        return null
    }
    val widget = findWidget(id = id)
    if (widget != null && widget.type == SettingsWidgetType.TEXT) {
        return widget
    }
    return null
}

fun Model.findEnabledScene(id: UUID): SettingsScene? {
    return enabledScenes.firstOrNull { it.id == id }
}

fun Model.findEnabledSceneIndex(id: UUID): Int? {
    val index = enabledScenes.indexOfFirst { it.id == id }
    return if (index == -1) null else index
}

fun Model.isCaptureDeviceWidget(widget: SettingsWidget): Boolean {
    val addedSceneIds = mutableSetOf<UUID>()
    return isCaptureDeviceWidgetInternal(widget = widget, addedSceneIds = addedSceneIds)
}

fun Model.getFillFrame(scene: SettingsScene): Boolean {
    return scene.fillFrame
}

fun Model.widgetsInCurrentSceneOrRemoteScene(onlyEnabled: Boolean): List<WidgetInScene> {
    val widgets = mutableListOf<WidgetInScene>()
    getSelectedScene()?.let { scene ->
        widgets.addAll(getSceneWidgets(scene = scene, onlyEnabled = onlyEnabled))
    }
    val remoteSceneId = database.remoteSceneId
    if (remoteSceneId != null) {
        val scene = database.scenes.firstOrNull { it.id == remoteSceneId }
        if (scene != null) {
            widgets.addAll(getSceneWidgets(scene = scene, onlyEnabled = onlyEnabled))
        }
    }
    return removeDuplicatedWidgets(widgets = widgets)
}

fun Model.widgetsInCurrentScene(onlyEnabled: Boolean): List<WidgetInScene> {
    val scene = getSelectedScene() ?: return emptyList()
    val widgets = getSceneWidgets(scene = scene, onlyEnabled = onlyEnabled)
    return removeDuplicatedWidgets(widgets = widgets)
}

private fun Model.removeDuplicatedWidgets(widgets: List<WidgetInScene>): List<WidgetInScene> {
    val found = mutableListOf<UUID>()
    val result = mutableListOf<WidgetInScene>()
    for (widget in widgets) {
        if (found.contains(widget.widget.id)) {
            continue
        }
        found.add(widget.widget.id)
        result.add(widget)
    }
    return result
}

fun Model.getSceneWidgets(scene: SettingsScene, onlyEnabled: Boolean): List<WidgetInScene> {
    val addedSceneIds = mutableSetOf<UUID>()
    return getSceneWidgetsInternal(scene = scene, onlyEnabled = onlyEnabled, addedSceneIds = addedSceneIds)
}

fun Model.switchToNextSceneRoundRobin() {
    val currentSceneIndex = findEnabledSceneIndex(id = sceneSelector.selectedSceneId) ?: return
    val nextSceneIndex = (currentSceneIndex + 1) % enabledScenes.count()
    if (nextSceneIndex == currentSceneIndex) {
        return
    }
    selectScene(id = enabledScenes[nextSceneIndex].id)
}

fun Model.appendWidgetToScene(scene: SettingsScene, widget: SettingsWidget) {
    scene.widgets.add(createSceneWidget(widget = widget))
    var attachCamera = false
    if (scene.id == getSelectedScene()?.id) {
        attachCamera = isCaptureDeviceWidget(widget = widget)
    }
    sceneUpdated(attachCamera = attachCamera)
}

fun Model.textWidgetTextChanged(widget: SettingsWidget) {
    val parts = loadTextFormat(format = widget.text.formatString)
    for (effect in getTextEffects(id = widget.id)) {
        effect.setFormat(format = widget.text.formatString)
        updateTimers(widget.text, effect, parts)
        updateStopwatches(widget.text, effect, parts)
        updateCheckboxes(widget.text, effect, parts)
        updateRatings(widget.text, effect, parts)
        updateLapTimes(widget.text, effect, parts)
        updateSubtitles(widget.text, effect, parts)
    }
    updateNeedsWeather(widget.text, parts)
    updateNeedsGeography(widget.text, parts)
    updateNeedsGForce(widget.text, parts)
    sceneUpdated()
}

fun Model.updateSettingsFromTextWidgets() {
    for (widgetId in textEffects.keys) {
        val widget = findWidget(id = widgetId) ?: continue
        for (stopwatch in widget.text.stopwatches) {
            if (!stopwatch.running) {
                continue
            }
            stopwatch.totalElapsed += Duration.between(stopwatch.playPressedTime, Instant.now())
                .toNanos() / 1_000_000_000.0
        }
    }
}

fun Model.loadTextWidgetStopwatches() {
    for (widget in database.widgets) {
        if (widget.type != SettingsWidgetType.TEXT) {
            continue
        }
        for (stopwatch in widget.text.stopwatches) {
            if (stopwatch.running) {
                stopwatch.playPressedTime = Instant.now()
            }
        }
    }
}

fun Model.forceUpdateTextEffects() {
    for (effect in textEffects.values) {
        effect.forceOverlayUpdate()
    }
    for (effect in slideshowEffects.values) {
        for (slide in effect.slides) {
            (slide.effect as? TextEffect)?.forceOverlayUpdate()
        }
    }
}

fun Model.updateMapEffects() {
    if (mapEffects.isEmpty()) {
        return
    }
    val location: Location
    val remoteSceneLocation = remoteSceneData.location
    if (remoteSceneLocation != null) {
        location = remoteSceneLocation.toLocation()
    } else {
        var latestKnownLocation = locationManager.getLatestKnownLocation() ?: return
        if (isLocationInPrivacyRegion(location = latestKnownLocation)) {
            latestKnownLocation = Location("")
        }
        remoteControlAssistantSetRemoteSceneDataLocation(location = latestKnownLocation)
        location = latestKnownLocation
    }
    for (mapEffect in mapEffects.values) {
        mapEffect.updateLocation(location = location)
    }
}

fun Model.isSceneVideoSourceActive(scene: SettingsScene): Boolean {
    return when (scene.videoSource.cameraPosition) {
        SettingsSceneCameraPosition.RTMP ->
            activeBufferedVideoIds.contains(scene.videoSource.rtmpCameraId)
        SettingsSceneCameraPosition.SRTLA ->
            activeBufferedVideoIds.contains(scene.videoSource.srtlaCameraId)
        SettingsSceneCameraPosition.SRT_CLIENT ->
            activeBufferedVideoIds.contains(scene.videoSource.srtClientCameraId)
        SettingsSceneCameraPosition.RIST ->
            activeBufferedVideoIds.contains(scene.videoSource.ristCameraId)
        SettingsSceneCameraPosition.RTSP ->
            activeBufferedVideoIds.contains(scene.videoSource.rtspCameraId)
        SettingsSceneCameraPosition.WHIP ->
            activeBufferedVideoIds.contains(scene.videoSource.whipCameraId)
        SettingsSceneCameraPosition.WHEP ->
            activeBufferedVideoIds.contains(scene.videoSource.whepCameraId)
        SettingsSceneCameraPosition.EXTERNAL ->
            isExternalCameraConnected(cameraId = scene.videoSource.externalCameraId)
        else -> true
    }
}

fun Model.isCurrentScenesVideoSourceNetwork(cameraId: UUID): Boolean {
    val scene = getSelectedScene() ?: return false
    return scene.videoSource.isNetwork(cameraId = cameraId)
}

fun Model.isSceneVideoSourceActive(sceneId: UUID): Boolean {
    val scene = findEnabledScene(id = sceneId) ?: return false
    return isSceneVideoSourceActive(scene = scene)
}

fun Model.getBuiltinCameraDevices(scene: SettingsScene, sceneDevice: CameraInfo?): CaptureDevices {
    val devices = CaptureDevices(hasSceneDevice = false, devices = mutableListOf())
    if (sceneDevice != null) {
        devices.hasSceneDevice = true
        devices.devices.add(makeCaptureDevice(device = sceneDevice))
    }
    val addedSceneIds = mutableSetOf<UUID>()
    val quickSwitchGroup = scene.quickSwitchGroup
    if (quickSwitchGroup != null) {
        for (otherScene in enabledScenes) {
            if (otherScene.quickSwitchGroup == quickSwitchGroup) {
                getBuiltinCameraDevices(videoSource = otherScene.videoSource, devices = devices.devices)
                getBuiltinCameraDevicesInScene(
                    scene = otherScene,
                    devices = devices.devices,
                    addedSceneIds = addedSceneIds
                )
            }
        }
    }
    getBuiltinCameraDevicesInScene(scene = scene, devices = devices.devices, addedSceneIds = addedSceneIds)
    return devices
}

fun Model.getCameraPreviewDeviceIds(scene: SettingsScene, sceneDevice: CameraInfo?): List<UUID> {
    val devices = mutableListOf<CaptureDevice>()
    if (sceneDevice != null) {
        devices.add(makeCaptureDevice(device = sceneDevice))
    }
    val quickSwitchGroup = scene.quickSwitchGroup
    if (quickSwitchGroup != null) {
        for (otherScene in enabledScenes) {
            if (otherScene.quickSwitchGroup == quickSwitchGroup) {
                getBuiltinCameraDevices(videoSource = otherScene.videoSource, devices = devices)
            }
        }
    }
    return devices.map { it.id }
}

private fun Model.createGlobalVideoEffects() {
    faceEffect = FaceEffect()
    updateFaceFilterSettings()
    movieEffect = MovieEffect()
    grayScaleEffect = GrayScaleEffect()
    sepiaEffect = SepiaEffect()
    tripleEffect = TripleEffect()
    twinEffect = TwinEffect()
    pixellateEffect = PixellateEffect(strength = database.pixellateStrength)
    cameraManEffect = CameraManEffect(
        moveVertically = database.debug.cameraManMoveVertically,
        speed = database.debug.cameraManSpeed,
        alwaysMove = database.debug.cameraManAlwaysMove
    )
    pollEffect = null
    whirlpoolEffect = WhirlpoolEffect(angle = database.whirlpoolAngle)
    pinchEffect = PinchEffect(scale = database.pinchScale)
    fixedHorizonEffect = FixedHorizonEffect()
    glassesEffect = createGlassesEffect()
    sparkleEffect = createSparkleEffect()
    beautyEffect = BeautyEffect(fps = stream.fps.toFloat())
    beautyEffect.setSmoothnessSettings(
        radius = database.beauty.smoothnessRadius,
        strength = database.beauty.smoothnessStrength
    )
    beautyEffect.setShapeSettings(
        position = database.beauty.shapePosition,
        radius = database.beauty.shapeRadius,
        strength = database.beauty.shapeStrength
    )
}

private fun Model.createGlassesEffect(): AlertsEffect {
    val settings = SettingsWidgetAlerts()
    settings.disableAll()
    settings.quickButton.enabled = true
    settings.quickButton.positionType = SettingsWidgetAlertPositionType.FACE
    settings.quickButton.facePosition.x = 0.24
    settings.quickButton.facePosition.y = 0.30
    settings.quickButton.facePosition.width = 0.49
    settings.quickButton.facePosition.height = 0.49
    settings.quickButton.imageId = database.alertsMediaGallery.getGlassesImageId()
    settings.quickButton.imageLoopCount = 3
    return AlertsEffect(
        settings = settings,
        delegate = this,
        mediaStorage = alertMediaStorage,
        bundledImages = database.alertsMediaGallery.bundledImages,
        bundledSounds = database.alertsMediaGallery.bundledSounds
    )
}

private fun Model.createSparkleEffect(): AlertsEffect {
    val settings = SettingsWidgetAlerts()
    settings.disableAll()
    settings.quickButton.enabled = true
    settings.quickButton.positionType = SettingsWidgetAlertPositionType.FACE
    settings.quickButton.facePosition.x = (alertsEffectBackgroundRightEyeRectangle.topLeftX
        + alertsEffectBackgroundRightEyeRectangle.bottomRightX) / 2 + 0.01
    settings.quickButton.facePosition.y = alertsEffectBackgroundRightEyeRectangle.topLeftY + 0.03
    settings.quickButton.facePosition.width = alertsEffectBackgroundRightEyeRectangle.width() * 0.8
    settings.quickButton.facePosition.height = alertsEffectBackgroundRightEyeRectangle.height() * 0.8
    settings.quickButton.imageId = database.alertsMediaGallery.getWhiteStarImageId()
    return AlertsEffect(
        settings = settings,
        delegate = this,
        mediaStorage = alertMediaStorage,
        bundledImages = database.alertsMediaGallery.bundledImages,
        bundledSounds = database.alertsMediaGallery.bundledSounds
    )
}

private fun Model.registerGlobalVideoEffects(scene: SettingsScene): List<VideoEffect> {
    val effects = mutableListOf<VideoEffect>()
    sparkleEffect?.let { effects.add(it) }
    glassesEffect?.let { effects.add(it) }
    val fixedHorizonStatus: String
    if (isFixedHorizonEnabled(scene = scene)) {
        fixedHorizonEffect.start(portrait = stream.portrait)
        fixedHorizonStatus = "Enabled"
        effects.add(fixedHorizonEffect)
    } else {
        fixedHorizonStatus = "Disabled"
        fixedHorizonEffect.stop()
    }
    if (fixedHorizonStatus != statusTopRight.fixedHorizonStatus) {
        statusTopRight.fixedHorizonStatus = fixedHorizonStatus
    }
    if (isFaceEnabled()) {
        effects.add(faceEffect)
    }
    if (isQuickButtonOn(SettingsQuickButtonType.CAMERA_MAN)) {
        effects.add(cameraManEffect)
    }
    if (isQuickButtonOn(SettingsQuickButtonType.WHIRLPOOL)) {
        effects.add(whirlpoolEffect)
    }
    if (isQuickButtonOn(SettingsQuickButtonType.PINCH)) {
        effects.add(pinchEffect)
    }
    if (isQuickButtonOn(SettingsQuickButtonType.MOVIE)) {
        effects.add(movieEffect)
    }
    if (isQuickButtonOn(SettingsQuickButtonType.FOUR_THREE)) {
        effects.add(fourThreeEffect)
    }
    if (isQuickButtonOn(SettingsQuickButtonType.GRAY_SCALE)) {
        effects.add(grayScaleEffect)
    }
    if (isQuickButtonOn(SettingsQuickButtonType.SEPIA)) {
        effects.add(sepiaEffect)
    }
    if (isQuickButtonOn(SettingsQuickButtonType.TRIPLE)) {
        effects.add(tripleEffect)
    }
    if (isQuickButtonOn(SettingsQuickButtonType.TWIN)) {
        effects.add(twinEffect)
    }
    if (isQuickButtonOn(SettingsQuickButtonType.PIXELLATE)) {
        pixellateEffect.setSettings(strength = database.pixellateStrength)
        effects.add(pixellateEffect)
    }
    if (database.beauty.enabled) {
        effects.add(beautyEffect)
    }
    if (isQuickButtonOn(SettingsQuickButtonType.CRT)) {
        effects.add(crtEffect)
    }
    return effects
}

private fun Model.registerGlobalVideoEffectsOnTop(): List<VideoEffect> {
    val effects = mutableListOf<VideoEffect>()
    if (isQuickButtonOn(SettingsQuickButtonType.POLL)) {
        pollEffect?.let { effects.add(it) }
    }
    return effects
}

fun Model.getImageEffect(id: UUID): ImageEffect? {
    return imageEffects[id]
}

private fun Model.getBrowserEffect(id: UUID): BrowserEffect? {
    return browserEffects[id]
}

private fun Model.getMapEffect(id: UUID): MapEffect? {
    return mapEffects[id]
}

fun Model.setBrowserEffectsProxyServer() {
    for (effect in browserEffects.values) {
        effect.setProxyServer(endpoint = getHttpProxyServerEndpoint())
    }
}

private fun Model.resetVideoEffects(widgets: List<SettingsWidget>) {
    createGlobalVideoEffects()
    resetImageEffects(widgets = widgets)
    resetTextVideoEffects(widgets = widgets)
    resetBrowserVideoEffects(widgets = widgets)
    resetMapVideoEffects(widgets = widgets)
    resetQrCodeVideoEffects(widgets = widgets)
    resetVideoSourceVideoEffects(widgets = widgets)
    resetScoreboardVideoEffects(widgets = widgets)
    resetAlertsVideoEffects(widgets = widgets)
    resetVTuberVideoEffects(widgets = widgets)
    resetPngTuberVideoEffects(widgets = widgets)
    resetSnapshotVideoEffects(widgets = widgets)
    resetChatVideoEffects(widgets = widgets)
    resetChatEmoteComboVideoEffects(widgets = widgets)
    resetSlideshowVideoEffects(widgets = widgets)
    resetWheelOfLuckEffects(widgets = widgets)
    resetBingoCardEffects(widgets = widgets)
    resetPomodoroTimerEffects(widgets = widgets)
    browsers = browserEffects.map { (widgetId, browser) ->
        val name = getWidgetName(id = widgetId) ?: "Unknown"
        Browser(name = name, browserEffect = browser)
    }.sortedBy { it.name }
}

private fun Model.createImageEffect(widget: SettingsWidget): ImageEffect {
    return ImageEffect(imageStorage = imageStorage, widgetId = widget.id)
}

private fun Model.resetImageEffects(widgets: List<SettingsWidget>) {
    imageEffects.clear()
    for (widget in widgets) {
        if (widget.type != SettingsWidgetType.IMAGE) {
            continue
        }
        val effect = createImageEffect(widget = widget)
        effect.effects = widget.getEffects(model = this)
        imageEffects[widget.id] = effect
    }
}

private fun Model.createTextEffect(widget: SettingsWidget): TextEffect {
    return TextEffect(
        format = widget.text.formatString,
        backgroundColor = widget.text.backgroundColor,
        foregroundColor = widget.text.foregroundColor,
        fontSize = widget.text.fontSize.toFloat(),
        fontFamily = widget.text.fontFamily,
        fontStyle = widget.text.fontStyle,
        fontDesign = widget.text.fontDesign.toSystem(),
        fontWeight = widget.text.fontWeight.toSystem(),
        fontMonospacedDigits = widget.text.fontMonospacedDigits,
        horizontalAlignment = widget.text.horizontalAlignment.toSystem(),
        width = if (widget.text.widthEnabled) widget.text.width else null,
        cornerRadius = widget.text.cornerRadius.toDouble(),
        delay = widget.text.delay,
        timersEndTime = widget.text.timers.map {
            Instant.now().plusNanos((utcTimeDeltaFromNow(it.endTime) * 1_000_000_000.0).toLong())
        },
        stopwatches = widget.text.stopwatches.map { it.clone() },
        checkboxes = widget.text.checkboxes.map { it.checked },
        ratings = widget.text.ratings.map { it.rating },
        lapTimes = widget.text.lapTimes.map { it.lapTimes }
    )
}

private fun Model.resetTextVideoEffects(widgets: List<SettingsWidget>) {
    textEffects.clear()
    for (widget in widgets) {
        if (widget.type != SettingsWidgetType.TEXT) {
            continue
        }
        textEffects[widget.id] = createTextEffect(widget = widget)
    }
}

private fun Model.resetBrowserVideoEffects(widgets: List<SettingsWidget>) {
    for (effect in browserEffects.values) {
        effect.stop()
    }
    browserEffects.clear()
    for (widget in widgets) {
        if (widget.type != SettingsWidgetType.BROWSER) {
            continue
        }
        val url = runCatching { URI(widget.browser.url) }.getOrNull() ?: continue
        val effect = BrowserEffect(
            url = url,
            styleSheet = widget.browser.styleSheet,
            widget = widget.browser,
            moblinAccess = widget.browser.moblinAccess,
            proxyServer = getHttpProxyServerEndpoint()
        )
        effect.effects = widget.getEffects(model = this)
        browserEffects[widget.id] = effect
    }
}

private fun Model.resetMapVideoEffects(widgets: List<SettingsWidget>) {
    mapEffects.clear()
    for (widget in widgets) {
        if (widget.type != SettingsWidgetType.MAP) {
            continue
        }
        val effect = MapEffect(widget = widget.map)
        effect.effects = widget.getEffects(model = this)
        mapEffects[widget.id] = effect
    }
}

private fun Model.resetQrCodeVideoEffects(widgets: List<SettingsWidget>) {
    qrCodeEffects.clear()
    for (widget in widgets) {
        if (widget.type != SettingsWidgetType.QR_CODE) {
            continue
        }
        val effect = QrCodeEffect(widget = widget.qrCode.clone())
        effect.effects = widget.getEffects(model = this)
        qrCodeEffects[widget.id] = effect
    }
}

private fun Model.resetVideoSourceVideoEffects(widgets: List<SettingsWidget>) {
    videoSourceEffects.clear()
    for (widget in widgets) {
        if (widget.type != SettingsWidgetType.VIDEO_SOURCE) {
            continue
        }
        val effect = VideoSourceEffect()
        effect.effects = widget.getEffects(model = this)
        videoSourceEffects[widget.id] = effect
    }
}

private fun Model.resetScoreboardVideoEffects(widgets: List<SettingsWidget>) {
    scoreboardEffects.clear()
    for (widget in widgets) {
        if (widget.type != SettingsWidgetType.SCOREBOARD) {
            continue
        }
        scoreboardEffects[widget.id] = ScoreboardEffect(canvasSize = media.getCanvasSize())
    }
}

private fun Model.resetAlertsVideoEffects(widgets: List<SettingsWidget>) {
    alertsEffects.clear()
    for (widget in widgets) {
        if (widget.type != SettingsWidgetType.ALERTS) {
            continue
        }
        alertsEffects[widget.id] = AlertsEffect(
            settings = widget.alerts.clone(),
            delegate = this,
            mediaStorage = alertMediaStorage,
            bundledImages = database.alertsMediaGallery.bundledImages,
            bundledSounds = database.alertsMediaGallery.bundledSounds
        )
    }
}

private fun Model.resetVTuberVideoEffects(widgets: List<SettingsWidget>) {
    vTuberEffects.clear()
    for (widget in widgets) {
        if (widget.type != SettingsWidgetType.V_TUBER) {
            continue
        }
        val path = vTuberStorage.makePath(id = widget.vTuber.id)
        when (widget.vTuber.type) {
            SettingsWidgetVTuberType.VRM -> vTuberEffects[widget.id] = VTuberVrmEffect(
                vrm = path,
                cameraFieldOfView = widget.vTuber.cameraFieldOfView,
                cameraPositionY = widget.vTuber.cameraPositionY
            )
            SettingsWidgetVTuberType.LIVE2D ->
                vTuberEffects[widget.id] = VTuberLive2DEffect(directory = path)
        }
    }
}

private fun Model.resetPngTuberVideoEffects(widgets: List<SettingsWidget>) {
    pngTuberEffects.clear()
    for (widget in widgets) {
        if (widget.type != SettingsWidgetType.PNG_TUBER) {
            continue
        }
        pngTuberEffects[widget.id] = PngTuberEffect(
            model = pngTuberStorage.makePath(id = widget.pngTuber.id),
            costume = 1
        )
    }
}

private fun Model.resetSnapshotVideoEffects(widgets: List<SettingsWidget>) {
    snapshotEffects.clear()
    for (widget in widgets) {
        if (widget.type != SettingsWidgetType.SNAPSHOT) {
            continue
        }
        val effect = SnapshotEffect(showtime = widget.snapshot.showtime)
        effect.effects = widget.getEffects(model = this)
        snapshotEffects[widget.id] = effect
    }
}

private fun Model.resetChatVideoEffects(widgets: List<SettingsWidget>) {
    chatEffects.clear()
    for (widget in widgets) {
        if (widget.type != SettingsWidgetType.CHAT) {
            continue
        }
        val effect = ChatEffect(chat = chatWidgetChat)
        effect.setSettings(settings = widget.chat)
        chatEffects[widget.id] = effect
    }
}

private fun Model.resetChatEmoteComboVideoEffects(widgets: List<SettingsWidget>) {
    chatEmoteComboEffects.clear()
    for (widget in widgets) {
        if (widget.type != SettingsWidgetType.CHAT_EMOTE_COMBO) {
            continue
        }
        val effect = ChatEmoteComboEffect(canvasSize = media.getCanvasSize())
        effect.setSettings(settings = widget.chatEmoteCombo)
        chatEmoteComboEffects[widget.id] = effect
    }
}

private fun Model.resetSlideshowVideoEffects(widgets: List<SettingsWidget>) {
    slideshowEffects.clear()
    for (widget in widgets) {
        if (widget.type != SettingsWidgetType.SLIDESHOW) {
            continue
        }
        val slides = mutableListOf<SlideshowEffectSlide>()
        for (slide in widget.slideshow.slides) {
            val widgetId = slide.widgetId ?: continue
            val slideWidget = findWidget(id = widgetId) ?: continue
            val effect: VideoEffect
            when (slideWidget.type) {
                SettingsWidgetType.TEXT -> effect = createTextEffect(widget = slideWidget)
                SettingsWidgetType.IMAGE -> effect = createImageEffect(widget = slideWidget)
                else -> continue
            }
            slides.add(
                SlideshowEffectSlide(
                    widgetId = widgetId,
                    effect = effect,
                    time = slide.time.toDouble()
                )
            )
        }
        slideshowEffects[widget.id] = SlideshowEffect(slides = slides)
    }
}

private fun Model.resetWheelOfLuckEffects(widgets: List<SettingsWidget>) {
    wheelOfLuckEffects.clear()
    for (widget in widgets) {
        if (widget.type != SettingsWidgetType.WHEEL_OF_LUCK) {
            continue
        }
        wheelOfLuckEffects[widget.id] = WheelOfLuckEffect(canvasSize = media.getCanvasSize())
    }
}

private fun Model.resetBingoCardEffects(widgets: List<SettingsWidget>) {
    bingoCardEffects.clear()
    for (widget in widgets) {
        if (widget.type != SettingsWidgetType.BINGO_CARD) {
            continue
        }
        bingoCardEffects[widget.id] = BingoCardEffect(canvasSize = media.getCanvasSize())
    }
}

private fun Model.resetPomodoroTimerEffects(widgets: List<SettingsWidget>) {
    pomodoroTimerEffects.clear()
    for (widget in widgets) {
        if (widget.type != SettingsWidgetType.POMODORO_TIMER) {
            continue
        }
        pomodoroTimerEffects[widget.id] = PomodoroTimerEffect(canvasSize = media.getCanvasSize())
        val pomodoroTimer = widget.pomodoroTimer
        pomodoroTimer.onPhaseChanged = { newPhase ->
            onPomodoroTimerPhaseChanged(pomodoroTimer, newPhase = newPhase)
        }
    }
}

private fun Model.onPomodoroTimerPhaseChanged(
    settings: SettingsWidgetPomodoroTimer,
    newPhase: PomodoroPhase
) {
    when (newPhase) {
        PomodoroPhase.FOCUS -> {
            settings.breakToFocusSoundId?.let { playPomodoroSound(soundId = it) }
            if (settings.breakToFocusChatMessage.isNotEmpty()) {
                sendChatMessage(message = settings.breakToFocusChatMessage)
            }
        }
        PomodoroPhase.SHORT_BREAK -> {
            settings.focusToBreakSoundId?.let { playPomodoroSound(soundId = it) }
            if (settings.focusToBreakChatMessage.isNotEmpty()) {
                sendChatMessage(message = settings.focusToBreakChatMessage)
            }
        }
    }
}

private fun Model.playPomodoroSound(soundId: UUID) {
    val url = getAlertSoundUrl(soundId = soundId) ?: return
    pomodoroAudioPlayer = runCatching { AudioPlayer(contentsOf = url) }.getOrNull()
    pomodoroAudioPlayer?.play()
}

private fun Model.isQuickButtonOn(type: SettingsQuickButtonType): Boolean {
    return database.quickButtons.firstOrNull { it.type == type }?.isOn ?: false
}

private fun Model.isFaceEnabled(): Boolean {
    val settings = database.face
    return settings.blurFaces || settings.blurText || settings.blurBackground || settings.showMoblin
}

private fun Model.setSceneId(id: UUID) {
    sceneSelector.selectedSceneId = id
    remoteControlStateChanged(state = RemoteControlAssistantStreamerState(scene = id))
    if (isWatchLocal()) {
        sendSceneToWatch(id = sceneSelector.selectedSceneId)
    }
    val showMediaPlayerControls =
        findEnabledScene(id = id)?.videoSource.cameraPosition == SettingsSceneCameraPosition.MEDIA_PLAYER
    if (showMediaPlayerControls != streamOverlay.showMediaPlayerControls) {
        streamOverlay.showMediaPlayerControls = showMediaPlayerControls
    }
}

private fun Model.sceneUpdatedOff() {
    media.unregisterAllEffects()
    createGlobalVideoEffects()
    for (effect in browserEffects.values) {
        effect.stop()
    }
}

private fun Model.findSceneWidget(scene: SettingsScene, widgetId: UUID): SettingsSceneWidget? {
    return scene.widgets.firstOrNull { it.widgetId == widgetId }
}

private fun Model.sceneUpdatedOn(scene: SettingsScene, attachCamera: Boolean) {
    val effects = mutableListOf<VideoEffect>()
    if (database.color.lutEnabled && database.color.space == SettingsColorSpace.APPLE_LOG) {
        effects.add(lutEffect)
    }
    for (lut in database.color.allLuts()) {
        if (!lut.enabled) {
            continue
        }
        val lutEffect = lutEffects[lut.id] ?: continue
        effects.add(lutEffect)
    }
    effects.addAll(registerGlobalVideoEffects(scene = scene))
    val addedScenes = mutableListOf<SettingsScene>()
    val needsSpeechToText = BooleanArray(1)
    enabledAlertsEffects.clear()
    enabledSnapshotEffects.clear()
    enabledChatEffects.clear()
    enabledChatEmoteComboEffects.clear()
    var effectiveScene = scene
    val remoteSceneWidget = remoteSceneWidgets.firstOrNull()
    if (remoteSceneWidget != null) {
        effectiveScene = scene.clone()
        effectiveScene.widgets.add(SettingsSceneWidget(widgetId = remoteSceneWidget.id))
    }
    addSceneEffects(effectiveScene, effects, addedScenes, needsSpeechToText)
    if (drawOnStream.lines.isNotEmpty()) {
        effects.add(drawOnStreamEffect)
    }
    effects.addAll(registerGlobalVideoEffectsOnTop())
    media.setPendingAfterAttachEffects(
        effects = effects,
        rotation = effectiveScene.videoSourceRotation,
        mirror = effectiveScene.mirror
    )
    for (effect in browserEffects.values) {
        if (!effects.contains(effect)) {
            effect.setSceneWidget(sceneWidget = null, crops = emptyList())
        }
    }
    for (effect in mapEffects.values) {
        if (!effects.contains(effect)) {
            effect.setSceneWidget(sceneWidget = null)
        }
    }
    for (effect in chatEffects.values) {
        if (!effects.contains(effect)) {
            effect.stop()
        }
    }
    for ((id, scoreboardEffect) in scoreboardEffects) {
        if (!effects.contains(scoreboardEffect)) {
            if (isWatchLocal()) {
                sendRemoveScoreboardToWatch(id = id)
            }
        }
    }
    media.setSpeechToText(enabled = needsSpeechToText[0])
    if (attachCamera) {
        attachSingleLayout(scene = effectiveScene)
    } else {
        media.usePendingAfterAttachEffects()
    }
    if (drawOnStream.lines.isNotEmpty()) {
        drawOnStreamEffect.updateOverlay(
            videoSize = media.getCanvasSize(),
            size = drawOnStreamSize,
            lines = drawOnStream.lines,
            mirror = streamOverlay.isFrontCameraSelected && !database.mirrorFrontCameraOnStream
        )
    }
}

private fun Model.addSceneEffects(
    scene: SettingsScene,
    effects: MutableList<VideoEffect>,
    addedScenes: MutableList<SettingsScene>,
    needsSpeechToText: BooleanArray
) {
    if (addedScenes.contains(scene)) {
        return
    }
    addedScenes.add(scene)
    for (sceneWidget in scene.widgets) {
        val widget = findWidget(id = sceneWidget.widgetId) ?: continue
        if (!widget.enabled) {
            continue
        }
        when (widget.type) {
            SettingsWidgetType.IMAGE -> addSceneImageEffects(sceneWidget, widget, effects)
            SettingsWidgetType.TEXT -> addSceneTextEffects(sceneWidget, widget, effects, needsSpeechToText)
            SettingsWidgetType.BROWSER -> addSceneBrowserEffects(
                sceneWidget,
                widget,
                scene,
                effects,
                needsSpeechToText
            )
            SettingsWidgetType.CROP -> addSceneCropEffects(widget, scene, effects)
            SettingsWidgetType.MAP -> addSceneMapEffects(sceneWidget, widget, effects)
            SettingsWidgetType.SCENE -> addSceneSceneEffects(widget, effects, addedScenes, needsSpeechToText)
            SettingsWidgetType.SLIDESHOW -> addSceneSlideshowEffects(sceneWidget, widget, effects)
            SettingsWidgetType.QR_CODE -> addSceneQrCodeEffects(sceneWidget, widget, effects)
            SettingsWidgetType.ALERTS -> addSceneAlertsEffects(sceneWidget, widget, effects, needsSpeechToText)
            SettingsWidgetType.VIDEO_SOURCE -> addSceneVideoSourceEffects(sceneWidget, widget, effects)
            SettingsWidgetType.SCOREBOARD -> addSceneScoreboardEffects(sceneWidget, widget, effects)
            SettingsWidgetType.V_TUBER -> addSceneVTuberEffects(sceneWidget, widget, effects)
            SettingsWidgetType.PNG_TUBER -> addScenePngTuberEffects(sceneWidget, widget, effects)
            SettingsWidgetType.SNAPSHOT -> addSceneSnapshotEffects(sceneWidget, widget, effects)
            SettingsWidgetType.CHAT -> addSceneChatEffects(sceneWidget, widget, effects)
            SettingsWidgetType.CHAT_EMOTE_COMBO ->
                addSceneChatEmoteComboEffects(sceneWidget, widget, effects)
            SettingsWidgetType.WHEEL_OF_LUCK -> addSceneWheelOfLuckEffects(sceneWidget, widget, effects)
            SettingsWidgetType.BINGO_CARD -> addSceneBingoCardEffects(sceneWidget, widget, effects)
            SettingsWidgetType.POMODORO_TIMER -> addScenePomodoroTimerEffects(sceneWidget, widget, effects)
        }
    }
}

private fun Model.addSceneImageEffects(
    sceneWidget: SettingsSceneWidget,
    widget: SettingsWidget,
    effects: MutableList<VideoEffect>
) {
    val effect = imageEffects[widget.id] ?: return
    if (effects.contains(effect)) {
        return
    }
    effect.setSceneWidget(sceneWidget = sceneWidget.clone())
    effects.add(effect)
}

private fun Model.addSceneTextEffects(
    sceneWidget: SettingsSceneWidget,
    widget: SettingsWidget,
    effects: MutableList<VideoEffect>,
    needsSpeechToText: BooleanArray
) {
    val effect = textEffects[widget.id] ?: return
    if (effects.contains(effect)) {
        return
    }
    effect.setSceneWidget(sceneWidget = sceneWidget.clone())
    effects.add(effect)
    if (widget.text.needsSubtitles) {
        needsSpeechToText[0] = true
    }
}

private fun Model.addSceneBrowserEffects(
    sceneWidget: SettingsSceneWidget,
    widget: SettingsWidget,
    scene: SettingsScene,
    effects: MutableList<VideoEffect>,
    needsSpeechToText: BooleanArray
) {
    val effect = browserEffects[widget.id] ?: return
    if (effects.contains(effect)) {
        return
    }
    effect.setSceneWidget(
        sceneWidget = sceneWidget.clone(),
        crops = findWidgetCrops(scene = scene, sourceWidgetId = widget.id)
    )
    effects.add(effect)
    if (widget.browser.moblinAccess && widget.browser.speechToText) {
        needsSpeechToText[0] = true
    }
}

private fun Model.addSceneCropEffects(
    widget: SettingsWidget,
    scene: SettingsScene,
    effects: MutableList<VideoEffect>
) {
    val effect = browserEffects[widget.crop.sourceWidgetId] ?: return
    if (effects.contains(effect)) {
        return
    }
    val sceneWidget: SettingsSceneWidget? =
        if (findWidget(id = widget.crop.sourceWidgetId)?.enabled == true) {
            findSceneWidget(scene = scene, widgetId = widget.crop.sourceWidgetId)
        } else {
            null
        }
    effect.setSceneWidget(
        sceneWidget = sceneWidget?.clone(),
        crops = findWidgetCrops(scene = scene, sourceWidgetId = widget.crop.sourceWidgetId)
    )
    effects.add(effect)
}

private fun Model.addSceneMapEffects(
    sceneWidget: SettingsSceneWidget,
    widget: SettingsWidget,
    effects: MutableList<VideoEffect>
) {
    val effect = mapEffects[widget.id] ?: return
    if (effects.contains(effect)) {
        return
    }
    effect.setSceneWidget(sceneWidget = sceneWidget.clone())
    effects.add(effect)
}

private fun Model.addSceneSceneEffects(
    widget: SettingsWidget,
    effects: MutableList<VideoEffect>,
    addedScenes: MutableList<SettingsScene>,
    needsSpeechToText: BooleanArray
) {
    val sceneWidgetScene = getLocalAndRemoteScenes().firstOrNull { it.id == widget.scene.sceneId } ?: return
    addSceneEffects(sceneWidgetScene, effects, addedScenes, needsSpeechToText)
}

private fun Model.addSceneSlideshowEffects(
    sceneWidget: SettingsSceneWidget,
    widget: SettingsWidget,
    effects: MutableList<VideoEffect>
) {
    val effect = slideshowEffects[widget.id] ?: return
    if (effects.contains(effect)) {
        return
    }
    effect.setSceneWidget(sceneWidget = sceneWidget.clone())
    effects.add(effect)
}

private fun Model.addSceneQrCodeEffects(
    sceneWidget: SettingsSceneWidget,
    widget: SettingsWidget,
    effects: MutableList<VideoEffect>
) {
    val effect = qrCodeEffects[widget.id] ?: return
    if (effects.contains(effect)) {
        return
    }
    effect.setSceneWidget(sceneWidget = sceneWidget.clone())
    effects.add(effect)
}

private fun Model.addSceneAlertsEffects(
    sceneWidget: SettingsSceneWidget,
    widget: SettingsWidget,
    effects: MutableList<VideoEffect>,
    needsSpeechToText: BooleanArray
) {
    val effect = alertsEffects[widget.id] ?: return
    if (effects.contains(effect)) {
        return
    }
    effect.setLayout(sceneWidget.layout)
    enabledAlertsEffects.add(effect)
    effects.add(effect)
    if (widget.alerts.needsSubtitles) {
        needsSpeechToText[0] = true
    }
}

private fun Model.addSceneVideoSourceEffects(
    sceneWidget: SettingsSceneWidget,
    widget: SettingsWidget,
    effects: MutableList<VideoEffect>
) {
    val effect = videoSourceEffects[widget.id] ?: return
    if (effects.contains(effect)) {
        return
    }
    getVideoSourceId(cameraId = widget.videoSource.toCameraId())?.let {
        effect.setVideoSourceId(videoSourceId = it)
    }
    effect.setSceneWidget(sceneWidget = sceneWidget.clone())
    effect.setSettings(settings = widget.videoSource.toEffectSettings())
    effects.add(effect)
}

private fun Model.addSceneScoreboardEffects(
    sceneWidget: SettingsSceneWidget,
    widget: SettingsWidget,
    effects: MutableList<VideoEffect>
) {
    val effect = getScoreboardEffect(id = widget.id) ?: return
    if (effects.contains(effect)) {
        return
    }
    effect.setSceneWidget(sceneWidget = sceneWidget.clone())
    effect.update(
        scoreboard = widget.scoreboard,
        config = getModularScoreboardConfig(scoreboard = widget.scoreboard),
        players = database.scoreboardPlayers
    )
    if (isWatchLocal()) {
        when (widget.scoreboard.sport) {
            SettingsWidgetScoreboardSport.PADEL ->
                sendUpdatePadelScoreboardToWatch(id = widget.id, padel = widget.scoreboard.padel)
            SettingsWidgetScoreboardSport.GENERIC ->
                sendUpdateGenericScoreboardToWatch(id = widget.id, generic = widget.scoreboard.generic)
            else -> {}
        }
    }
    effects.add(effect)
}

private fun Model.addSceneVTuberEffects(
    sceneWidget: SettingsSceneWidget,
    widget: SettingsWidget,
    effects: MutableList<VideoEffect>
) {
    val effect = vTuberEffects[widget.id] ?: return
    if (effects.contains(effect)) {
        return
    }
    getVideoSourceId(cameraId = widget.vTuber.toCameraId())?.let {
        effect.setVideoSourceId(videoSourceId = it)
    }
    effect.setSceneWidget(sceneWidget = sceneWidget.clone())
    effect.setSettings(
        cameraFieldOfView = widget.vTuber.cameraFieldOfView,
        cameraPositionY = widget.vTuber.cameraPositionY,
        mirror = widget.vTuber.mirror,
        sensitivity = widget.vTuber.sensitivity,
        armsAngle = widget.vTuber.armsAngle
    )
    effects.add(effect)
}

private fun Model.addScenePngTuberEffects(
    sceneWidget: SettingsSceneWidget,
    widget: SettingsWidget,
    effects: MutableList<VideoEffect>
) {
    val effect = pngTuberEffects[widget.id] ?: return
    if (effects.contains(effect)) {
        return
    }
    getVideoSourceId(cameraId = widget.pngTuber.toCameraId())?.let {
        effect.setVideoSourceId(videoSourceId = it)
    }
    effect.setSceneWidget(sceneWidget = sceneWidget.clone())
    effect.setSettings(mirror = widget.pngTuber.mirror, sensitivity = widget.pngTuber.sensitivity)
    effects.add(effect)
}

private fun Model.addSceneSnapshotEffects(
    sceneWidget: SettingsSceneWidget,
    widget: SettingsWidget,
    effects: MutableList<VideoEffect>
) {
    val effect = snapshotEffects[widget.id] ?: return
    if (effects.contains(effect)) {
        return
    }
    effect.setSceneWidget(sceneWidget = sceneWidget.clone())
    enabledSnapshotEffects.add(effect)
    effects.add(effect)
}

private fun Model.addSceneChatEffects(
    sceneWidget: SettingsSceneWidget,
    widget: SettingsWidget,
    effects: MutableList<VideoEffect>
) {
    val effect = chatEffects[widget.id] ?: return
    if (effects.contains(effect)) {
        return
    }
    effect.setSceneWidget(sceneWidget = sceneWidget.clone())
    effect.start()
    enabledChatEffects.add(effect)
    effects.add(effect)
}

private fun Model.addSceneChatEmoteComboEffects(
    sceneWidget: SettingsSceneWidget,
    widget: SettingsWidget,
    effects: MutableList<VideoEffect>
) {
    val effect = chatEmoteComboEffects[widget.id] ?: return
    if (effects.contains(effect)) {
        return
    }
    effect.setSceneWidget(sceneWidget = sceneWidget.clone())
    effect.setSettings(settings = widget.chatEmoteCombo)
    enabledChatEmoteComboEffects.add(effect)
    effects.add(effect)
}

private fun Model.addSceneWheelOfLuckEffects(
    sceneWidget: SettingsSceneWidget,
    widget: SettingsWidget,
    effects: MutableList<VideoEffect>
) {
    val effect = wheelOfLuckEffects[widget.id] ?: return
    if (effects.contains(effect)) {
        return
    }
    effect.setSceneWidget(sceneWidget = sceneWidget.clone())
    effect.setSettings(settings = widget.wheelOfLuck)
    effects.add(effect)
}

private fun Model.addSceneBingoCardEffects(
    sceneWidget: SettingsSceneWidget,
    widget: SettingsWidget,
    effects: MutableList<VideoEffect>
) {
    val effect = bingoCardEffects[widget.id] ?: return
    if (effects.contains(effect)) {
        return
    }
    effect.setSceneWidget(sceneWidget = sceneWidget.clone())
    effect.setSettings(settings = widget.bingoCard)
    effects.add(effect)
}

private fun Model.addScenePomodoroTimerEffects(
    sceneWidget: SettingsSceneWidget,
    widget: SettingsWidget,
    effects: MutableList<VideoEffect>
) {
    val effect = pomodoroTimerEffects[widget.id] ?: return
    if (effects.contains(effect)) {
        return
    }
    effect.setSceneWidget(sceneWidget = sceneWidget.clone())
    effect.setSettings(settings = widget.pomodoroTimer)
    effects.add(effect)
}

private fun Model.updateRemoteSceneSettings() {
    if (remoteSceneSettingsUpdating) {
        return
    }
    remoteSceneSettingsUpdating = true
    remoteControlAssistantSetRemoteSceneSettings()
    mainScope.launch {
        delay(1000)
        remoteSceneSettingsUpdating = false
        if (remoteSceneSettingsUpdateRequested) {
            remoteSceneSettingsUpdateRequested = false
            updateRemoteSceneSettings()
        }
    }
}

private fun Model.getLocalAndRemoteScenes(): List<SettingsScene> {
    return database.scenes + remoteSceneScenes
}

private fun Model.getLocalAndRemoteWidgets(): List<SettingsWidget> {
    return database.widgets + remoteSceneWidgets
}

private fun Model.findWidgetCrops(scene: SettingsScene, sourceWidgetId: UUID): List<WidgetCrop> {
    val crops = mutableListOf<WidgetCrop>()
    for (widget in getSceneWidgets(scene = scene, onlyEnabled = true)) {
        if (widget.widget.type != SettingsWidgetType.CROP) {
            continue
        }
        val crop = widget.widget.crop
        if (crop.sourceWidgetId != sourceWidgetId) {
            continue
        }
        crops.add(WidgetCrop(crop = crop.clone(), sceneWidget = widget.sceneWidget.clone()))
    }
    return crops
}

private fun Model.isCaptureDeviceWidgetInternal(
    widget: SettingsWidget,
    addedSceneIds: MutableSet<UUID>
): Boolean {
    when (widget.type) {
        SettingsWidgetType.SCENE -> {
            if (addedSceneIds.contains(widget.scene.sceneId)) {
                return false
            }
            addedSceneIds.add(widget.scene.sceneId)
            val scene = database.scenes.firstOrNull { it.id == widget.scene.sceneId }
            if (scene != null) {
                for (sceneWidget in getSceneWidgets(scene = scene, onlyEnabled = false)) {
                    if (isCaptureDeviceWidgetInternal(
                            widget = sceneWidget.widget,
                            addedSceneIds = addedSceneIds
                        )
                    ) {
                        return true
                    }
                }
            }
            return false
        }
        SettingsWidgetType.VIDEO_SOURCE -> return widget.videoSource.videoSource.isCaptureDevice()
        SettingsWidgetType.V_TUBER -> return widget.vTuber.videoSource.isCaptureDevice()
        SettingsWidgetType.PNG_TUBER -> return widget.pngTuber.videoSource.isCaptureDevice()
        else -> return false
    }
}

private fun Model.getSceneWidgetsInternal(
    scene: SettingsScene,
    onlyEnabled: Boolean,
    addedSceneIds: MutableSet<UUID>
): List<WidgetInScene> {
    val widgets = mutableListOf<WidgetInScene>()
    for (sceneWidget in scene.widgets) {
        val widget = findWidget(id = sceneWidget.widgetId) ?: continue
        if (onlyEnabled && !widget.enabled) {
            continue
        }
        if (widget.type == SettingsWidgetType.SCENE) {
            if (addedSceneIds.contains(widget.scene.sceneId)) {
                continue
            }
            widgets.add(WidgetInScene(widget = widget, sceneWidget = sceneWidget))
            addedSceneIds.add(widget.scene.sceneId)
            val nestedScene = database.scenes.firstOrNull { it.id == widget.scene.sceneId }
            if (nestedScene != null) {
                widgets.addAll(getSceneWidgetsInternal(nestedScene, onlyEnabled, addedSceneIds))
            }
        } else {
            widgets.add(WidgetInScene(widget = widget, sceneWidget = sceneWidget))
        }
    }
    return widgets
}

private fun Model.updateTextWidgetsLapTimes(now: Instant) {
    for (widget in database.widgets) {
        if (widget.type != SettingsWidgetType.TEXT) {
            continue
        }
        if (widget.text.lapTimes.isEmpty()) {
            continue
        }
        val nowSeconds = now.toEpochMilli() / 1000.0
        for (lapTimes in widget.text.lapTimes) {
            val lastIndex = lapTimes.lapTimes.size - 1
            val currentLapStartTime = lapTimes.currentLapStartTime
            if (lastIndex < 0 || currentLapStartTime == null) {
                continue
            }
            lapTimes.lapTimes[lastIndex] = nowSeconds - currentLapStartTime
        }
        for (effect in getTextEffects(id = widget.id)) {
            effect.setLapTimes(lapTimes = widget.text.lapTimes.map { it.lapTimes })
        }
    }
}

fun Model.updateTextEffects(now: Instant, timestamp: Instant) {
    if (textEffects.isEmpty()) {
        return
    }
    val variables: Variables
    val textStats = remoteSceneData.textStats
    if (textStats != null) {
        variables = textStats.toVariables()
    } else {
        updateTextWidgetsLapTimes(now = now)
        variables = createVariables(now = now, timestamp = timestamp)
        remoteControlAssistantSetRemoteSceneDataVariables(variables = variables)
    }
    for (effect in textEffects.values) {
        effect.updateVariables(variables = variables)
    }
    for (effect in slideshowEffects.values) {
        for (slide in effect.slides) {
            (slide.effect as? TextEffect)?.updateVariables(variables = variables)
        }
    }
}

private fun Model.getBuiltinCameraDevicesInScene(
    scene: SettingsScene,
    devices: MutableList<CaptureDevice>,
    addedSceneIds: MutableSet<UUID>
) {
    if (addedSceneIds.contains(scene.id)) {
        return
    }
    addedSceneIds.add(scene.id)
    for (sceneWidget in scene.widgets) {
        val widget = findWidget(id = sceneWidget.widgetId) ?: continue
        if (!widget.enabled) {
            continue
        }
        when (widget.type) {
            SettingsWidgetType.VIDEO_SOURCE ->
                getBuiltinCameraDevices(videoSource = widget.videoSource.videoSource, devices = devices)
            SettingsWidgetType.V_TUBER ->
                getBuiltinCameraDevices(videoSource = widget.vTuber.videoSource, devices = devices)
            SettingsWidgetType.PNG_TUBER ->
                getBuiltinCameraDevices(videoSource = widget.pngTuber.videoSource, devices = devices)
            SettingsWidgetType.SCENE -> getBuiltinCameraDevicesForSceneWidget(
                scene = widget.scene,
                devices = devices,
                addedSceneIds = addedSceneIds
            )
            else -> {}
        }
    }
}

private fun Model.getBuiltinCameraDevices(
    videoSource: SettingsVideoSource,
    devices: MutableList<CaptureDevice>
) {
    val cameraId = videoSource.getCaptureDeviceCameraId() ?: return
    val device: CameraInfo = TODO("no Android counterpart for AVCaptureDevice(uniqueID:)")
    if (devices.none { it.device == device }) {
        devices.add(makeCaptureDevice(device = device))
    }
}

private fun Model.getBuiltinCameraDevicesForSceneWidget(
    scene: SettingsWidgetScene,
    devices: MutableList<CaptureDevice>,
    addedSceneIds: MutableSet<UUID>
) {
    val found = database.scenes.firstOrNull { it.id == scene.sceneId } ?: return
    getBuiltinCameraDevicesInScene(scene = found, devices = devices, addedSceneIds = addedSceneIds)
}

private fun Model.createSceneWidget(widget: SettingsWidget): SettingsSceneWidget {
    val sceneWidget = SettingsSceneWidget(widgetId = widget.id)
    when (widget.type) {
        SettingsWidgetType.IMAGE, SettingsWidgetType.SLIDESHOW -> {
            sceneWidget.layout.size = 30.0
        }
        SettingsWidgetType.MAP, SettingsWidgetType.QR_CODE -> {
            sceneWidget.layout.size = 23.0
        }
        SettingsWidgetType.VIDEO_SOURCE, SettingsWidgetType.V_TUBER, SettingsWidgetType.PNG_TUBER -> {
            sceneWidget.layout.size = 28.0
            sceneWidget.layout.alignment = SettingsAlignment.BOTTOM_RIGHT
        }
        SettingsWidgetType.SNAPSHOT -> {
            sceneWidget.layout.size = 40.0
            sceneWidget.layout.alignment = SettingsAlignment.TOP_RIGHT
        }
        SettingsWidgetType.CHAT -> {
            sceneWidget.layout.alignment = SettingsAlignment.BOTTOM_LEFT
        }
        SettingsWidgetType.CHAT_EMOTE_COMBO -> {
            sceneWidget.layout.x = 2.0
            sceneWidget.layout.y = 25.0
            sceneWidget.layout.size = 10.0
        }
        SettingsWidgetType.ALERTS -> {
            sceneWidget.layout.x = 20.0
            sceneWidget.layout.y = 5.0
        }
        SettingsWidgetType.SCOREBOARD -> {
            sceneWidget.layout.size = defaultScoreboardSize
            sceneWidget.layout.x = 0.78
            sceneWidget.layout.y = 1.388
            when (widget.scoreboard.sport) {
                SettingsWidgetScoreboardSport.GOLF_FULL_SCORECARD ->
                    sceneWidget.layout.alignment = SettingsAlignment.BOTTOM_RIGHT
                else -> {}
            }
        }
        SettingsWidgetType.WHEEL_OF_LUCK -> {
            sceneWidget.layout.alignment = SettingsAlignment.TOP_RIGHT
            sceneWidget.layout.x = 1.3
            sceneWidget.layout.y = 31.0
        }
        SettingsWidgetType.BINGO_CARD -> {
            sceneWidget.layout.alignment = SettingsAlignment.TOP_RIGHT
            sceneWidget.layout.x = 1.3
            sceneWidget.layout.y = 33.0
            sceneWidget.layout.size = 33.0
        }
        SettingsWidgetType.POMODORO_TIMER -> {
            sceneWidget.layout.alignment = SettingsAlignment.TOP_RIGHT
            sceneWidget.layout.x = 0.78
            sceneWidget.layout.y = 1.388
            sceneWidget.layout.size = 20.0
        }
        else -> {}
    }
    sceneWidget.layout.updateXString()
    sceneWidget.layout.updateYString()
    sceneWidget.layout.updateSizeString()
    return sceneWidget
}

private fun Model.updateTimers(
    text: SettingsWidgetText,
    textEffect: TextEffect,
    parts: List<TextFormatPart>
) {
    val length = parts.count { it == TextFormatPart.Timer }
    while (text.timers.size > length) {
        text.timers.removeAt(text.timers.size - 1)
    }
    while (text.timers.size < length) {
        text.timers.add(SettingsWidgetTextTimer())
    }
    textEffect.setTimersEndTime(
        endTimes = text.timers.map {
            Instant.now().plusNanos((utcTimeDeltaFromNow(it.endTime) * 1_000_000_000.0).toLong())
        }
    )
}

private fun Model.updateStopwatches(
    text: SettingsWidgetText,
    textEffect: TextEffect,
    parts: List<TextFormatPart>
) {
    val length = parts.count { it == TextFormatPart.Stopwatch }
    while (text.stopwatches.size > length) {
        text.stopwatches.removeAt(text.stopwatches.size - 1)
    }
    while (text.stopwatches.size < length) {
        text.stopwatches.add(SettingsWidgetTextStopwatch())
    }
    textEffect.setStopwatches(stopwatches = text.stopwatches.map { it.clone() })
}

private fun Model.updateCheckboxes(
    text: SettingsWidgetText,
    textEffect: TextEffect,
    parts: List<TextFormatPart>
) {
    val length = parts.count { it == TextFormatPart.Checkbox }
    while (text.checkboxes.size > length) {
        text.checkboxes.removeAt(text.checkboxes.size - 1)
    }
    while (text.checkboxes.size < length) {
        text.checkboxes.add(SettingsWidgetTextCheckbox())
    }
    textEffect.setCheckboxes(checkboxes = text.checkboxes.map { it.checked })
}

private fun Model.updateRatings(
    text: SettingsWidgetText,
    textEffect: TextEffect,
    parts: List<TextFormatPart>
) {
    val length = parts.count { it == TextFormatPart.Rating }
    while (text.ratings.size > length) {
        text.ratings.removeAt(text.ratings.size - 1)
    }
    while (text.ratings.size < length) {
        text.ratings.add(SettingsWidgetTextRating())
    }
    textEffect.setRatings(ratings = text.ratings.map { it.rating })
}

private fun Model.updateLapTimes(
    text: SettingsWidgetText,
    textEffect: TextEffect,
    parts: List<TextFormatPart>
) {
    val length = parts.count { it == TextFormatPart.LapTimes }
    while (text.lapTimes.size > length) {
        text.lapTimes.removeAt(text.lapTimes.size - 1)
    }
    while (text.lapTimes.size < length) {
        text.lapTimes.add(SettingsWidgetTextLapTimes())
    }
    textEffect.setLapTimes(lapTimes = text.lapTimes.map { it.lapTimes })
}

private fun Model.updateSubtitles(
    text: SettingsWidgetText,
    textEffect: TextEffect?,
    parts: List<TextFormatPart>
) {
    text.subtitles.clear()
    for (part in parts) {
        when (part) {
            is TextFormatPart.Subtitles -> {
                val item = SettingsWidgetTextSubtitles()
                item.identifier = part.identifier
                text.subtitles.add(item)
            }
            else -> {}
        }
    }
    text.needsSubtitles = text.subtitles.isNotEmpty()
    reloadSpeechToText()
}

private fun Model.updateNeedsWeather(text: SettingsWidgetText, parts: List<TextFormatPart>) {
    text.needsWeather = parts.isWeatherVariable()
    startWeatherManager()
}

private fun Model.updateNeedsGeography(text: SettingsWidgetText, parts: List<TextFormatPart>) {
    text.needsGeography = parts.isGeographyVariable()
    startGeographyManager()
}

private fun Model.updateNeedsGForce(text: SettingsWidgetText, parts: List<TextFormatPart>) {
    text.needsGForce = parts.isGForceVariable()
    startGForceManager()
}

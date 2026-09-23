package com.moblin.android.various.model

import android.location.Location
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import com.moblin.android.localized
import com.moblin.android.media.haishinkit.media.video.CaptureDevice
import com.moblin.android.media.haishinkit.media.video.CaptureDevices
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.remotecontrol.RemoteControlAssistantStreamerState
import com.moblin.android.various.AudioPlayer
import com.moblin.android.various.Variables
import com.moblin.android.various.settings.MacroEvent
import com.moblin.android.various.settings.SettingsAlignment
import com.moblin.android.various.settings.SettingsColorSpace
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
import com.moblin.android.videoeffects.browser.BrowserEffect
import com.moblin.android.videoeffects.browser.WidgetCrop
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
import com.moblin.android.videoeffects.VideoSourceEffect
import com.moblin.android.videoeffects.vtuber.VTuberEffect
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
import java.io.File
import java.net.URI
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.moblin.android.AppDelegate

private val mainScope = CoroutineScope(Dispatchers.Main)

class CreateWidgetWizard {
    val name = MutableStateFlow("")
    val type = MutableStateFlow(SettingsWidgetType.text)
    var widget: SettingsWidget = SettingsWidget(name = "")

    fun reset() {
        name.value = ""
        type.value = SettingsWidgetType.text
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
        sceneSelector.sceneIndex.value = 0
    }
    resetVideoEffects(widgets = getLocalAndRemoteWidgets())
    drawOnStreamEffect.updateOverlay(
        videoSize = canvasSize(media.getCanvasSize()),
        size = drawOnStreamSize,
        lines = drawOnStream.lines.value,
        mirror = streamOverlay.isFrontCameraSelected.value && !database.mirrorFrontCameraOnStream
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
    sceneSettingsPanelSceneId.value += 1
    toggleShowingPanel(type = null, panel = ShowingPanel.none)
    toggleShowingPanel(type = null, panel = ShowingPanel.sceneSettings)
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
    sceneSelector.sceneIndex.value = index
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
    streamOverlay.isFrontCameraSelected.value = false
    deactivateAllMediaPlayers()
    when (scene.videoSource.cameraPosition) {
        SettingsSceneCameraPosition.back ->
            attachCamera(scene = scene, position = CameraSelector.LENS_FACING_BACK)
        SettingsSceneCameraPosition.front -> {
            attachCamera(scene = scene, position = CameraSelector.LENS_FACING_FRONT)
            streamOverlay.isFrontCameraSelected.value = true
        }
        SettingsSceneCameraPosition.rtmp ->
            attachBufferedCamera(cameraId = scene.videoSource.rtmpCameraId, scene = scene)
        SettingsSceneCameraPosition.srtla ->
            attachBufferedCamera(cameraId = scene.videoSource.srtlaCameraId, scene = scene)
        SettingsSceneCameraPosition.srtClient ->
            attachBufferedCamera(cameraId = scene.videoSource.srtClientCameraId, scene = scene)
        SettingsSceneCameraPosition.rist ->
            attachBufferedCamera(cameraId = scene.videoSource.ristCameraId, scene = scene)
        SettingsSceneCameraPosition.rtsp ->
            attachBufferedCamera(cameraId = scene.videoSource.rtspCameraId, scene = scene)
        SettingsSceneCameraPosition.whip ->
            attachBufferedCamera(cameraId = scene.videoSource.whipCameraId, scene = scene)
        SettingsSceneCameraPosition.whep ->
            attachBufferedCamera(cameraId = scene.videoSource.whepCameraId, scene = scene)
        SettingsSceneCameraPosition.mediaPlayer -> {
            mediaPlayers[scene.videoSource.mediaPlayerCameraId]?.activate()
            attachBufferedCamera(cameraId = scene.videoSource.mediaPlayerCameraId, scene = scene)
        }
        SettingsSceneCameraPosition.external ->
            attachExternalCamera(scene = scene)
        SettingsSceneCameraPosition.screenCapture ->
            attachBufferedCamera(cameraId = screenCaptureCameraId, scene = scene)
        SettingsSceneCameraPosition.backTripleLowEnergy ->
            attachBackTripleLowEnergyCamera()
        SettingsSceneCameraPosition.backDualLowEnergy ->
            attachBackDualLowEnergyCamera()
        SettingsSceneCameraPosition.backWideDualLowEnergy ->
            attachBackWideDualLowEnergyCamera()
        SettingsSceneCameraPosition.none ->
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
    if (widget != null && widget.type == SettingsWidgetType.text) {
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
        if (widget.type != SettingsWidgetType.text) {
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
        mapEffect.updateLocation(location = TODO("no Android counterpart for MapLocation conversion"))
    }
}

fun Model.isSceneVideoSourceActive(scene: SettingsScene): Boolean {
    return when (scene.videoSource.cameraPosition) {
        SettingsSceneCameraPosition.rtmp ->
            activeBufferedVideoIds.contains(scene.videoSource.rtmpCameraId)
        SettingsSceneCameraPosition.srtla ->
            activeBufferedVideoIds.contains(scene.videoSource.srtlaCameraId)
        SettingsSceneCameraPosition.srtClient ->
            activeBufferedVideoIds.contains(scene.videoSource.srtClientCameraId)
        SettingsSceneCameraPosition.rist ->
            activeBufferedVideoIds.contains(scene.videoSource.ristCameraId)
        SettingsSceneCameraPosition.rtsp ->
            activeBufferedVideoIds.contains(scene.videoSource.rtspCameraId)
        SettingsSceneCameraPosition.whip ->
            activeBufferedVideoIds.contains(scene.videoSource.whipCameraId)
        SettingsSceneCameraPosition.whep ->
            activeBufferedVideoIds.contains(scene.videoSource.whepCameraId)
        SettingsSceneCameraPosition.external ->
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

fun Model.getBuiltinCameraDevices(scene: SettingsScene, sceneDevice: CaptureDevice?): CaptureDevices {
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

fun Model.getCameraPreviewDeviceIds(scene: SettingsScene, sceneDevice: CaptureDevice?): List<UUID> {
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
        moveVertically = database.debug.cameraManMoveVertically.value,
        speed = database.debug.cameraManSpeed.value,
        alwaysMove = database.debug.cameraManAlwaysMove.value
    )
    pollEffect = null
    whirlpoolEffect = WhirlpoolEffect(angle = database.whirlpoolAngle)
    pinchEffect = PinchEffect(scale = database.pinchScale)
    fixedHorizonEffect = FixedHorizonEffect()
    glassesEffect = createGlassesEffect()
    sparkleEffect = createSparkleEffect()
    beautyEffect = BeautyEffect(fps = (stream.value.fps).toFloat())
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
    settings.quickButton.positionType = SettingsWidgetAlertPositionType.face
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
    settings.quickButton.positionType = SettingsWidgetAlertPositionType.face
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
        fixedHorizonEffect.start(portrait = database.portrait)
        fixedHorizonStatus = "Enabled"
        effects.add(fixedHorizonEffect)
    } else {
        fixedHorizonStatus = "Disabled"
        fixedHorizonEffect.stop()
    }
    if (fixedHorizonStatus != statusTopRight.fixedHorizonStatus.value) {
        statusTopRight.fixedHorizonStatus.value = fixedHorizonStatus
    }
    if (isFaceEnabled()) {
        effects.add(faceEffect)
    }
    if (isQuickButtonOn(SettingsQuickButtonType.cameraMan)) {
        effects.add(cameraManEffect)
    }
    if (isQuickButtonOn(SettingsQuickButtonType.whirlpool)) {
        effects.add(whirlpoolEffect)
    }
    if (isQuickButtonOn(SettingsQuickButtonType.pinch)) {
        effects.add(pinchEffect)
    }
    if (isQuickButtonOn(SettingsQuickButtonType.movie)) {
        effects.add(movieEffect)
    }
    if (isQuickButtonOn(SettingsQuickButtonType.fourThree)) {
        effects.add(fourThreeEffect)
    }
    if (isQuickButtonOn(SettingsQuickButtonType.grayScale)) {
        effects.add(grayScaleEffect)
    }
    if (isQuickButtonOn(SettingsQuickButtonType.sepia)) {
        effects.add(sepiaEffect)
    }
    if (isQuickButtonOn(SettingsQuickButtonType.triple)) {
        effects.add(tripleEffect)
    }
    if (isQuickButtonOn(SettingsQuickButtonType.twin)) {
        effects.add(twinEffect)
    }
    if (isQuickButtonOn(SettingsQuickButtonType.pixellate)) {
        pixellateEffect.setSettings(strength = database.pixellateStrength)
        effects.add(pixellateEffect)
    }
    if (database.beauty.enabled) {
        effects.add(beautyEffect)
    }
    if (isQuickButtonOn(SettingsQuickButtonType.crt)) {
        effects.add(crtEffect)
    }
    return effects
}

private fun Model.registerGlobalVideoEffectsOnTop(): List<VideoEffect> {
    val effects = mutableListOf<VideoEffect>()
    if (isQuickButtonOn(SettingsQuickButtonType.poll)) {
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
    browsers.value = browserEffects.map { (widgetId, browser) ->
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
        if (widget.type != SettingsWidgetType.image) {
            continue
        }
        val effect = createImageEffect(widget = widget)
        effect.effects = widget.getEffects(model = this).toMutableList()
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
        fontDesign = widget.text.fontDesign,
        fontWeight = widget.text.fontWeight,
        fontMonospacedDigits = widget.text.fontMonospacedDigits,
        horizontalAlignment = widget.text.horizontalAlignment,
        width = if (widget.text.widthEnabled) widget.text.width else null,
        cornerRadius = widget.text.cornerRadius.toDouble(),
        delay = widget.text.delay,
        timersEndTime = widget.text.timers.map {
            Instant.now().plusNanos((utcTimeDeltaFromNow(it.endTime) * 1_000_000_000.0).toLong()).toEpochMilli()
        }.toMutableList(),
        stopwatches = widget.text.stopwatches.map { it.clone() }.toMutableList(),
        checkboxes = widget.text.checkboxes.map { it.checked }.toMutableList(),
        ratings = widget.text.ratings.map { it.rating }.toMutableList(),
        lapTimes = widget.text.lapTimes.map { it.lapTimes.toMutableList() }.toMutableList()
    )
}

private fun Model.resetTextVideoEffects(widgets: List<SettingsWidget>) {
    textEffects.clear()
    for (widget in widgets) {
        if (widget.type != SettingsWidgetType.text) {
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
        if (widget.type != SettingsWidgetType.browser) {
            continue
        }
        val url = runCatching { URI(widget.browser.url) }.getOrNull() ?: continue
        val effect = BrowserEffect(
            url = url,
            styleSheet = widget.browser.styleSheet,
            widget = widget.browser,
            moblinAccess = widget.browser.moblinAccess,
            proxyServer = getHttpProxyServerEndpoint(),
            context = AppDelegate.context
        )
        effect.effects = widget.getEffects(model = this).toMutableList()
        browserEffects[widget.id] = effect
    }
}

private fun Model.resetMapVideoEffects(widgets: List<SettingsWidget>) {
    mapEffects.clear()
    for (widget in widgets) {
        if (widget.type != SettingsWidgetType.map) {
            continue
        }
        val effect = MapEffect(widget = widget.map)
        effect.effects = widget.getEffects(model = this).toMutableList()
        mapEffects[widget.id] = effect
    }
}

private fun Model.resetQrCodeVideoEffects(widgets: List<SettingsWidget>) {
    qrCodeEffects.clear()
    for (widget in widgets) {
        if (widget.type != SettingsWidgetType.qrCode) {
            continue
        }
        val effect = QrCodeEffect(widget = widget.qrCode.clone())
        effect.effects = widget.getEffects(model = this).toMutableList()
        qrCodeEffects[widget.id] = effect
    }
}

private fun Model.resetVideoSourceVideoEffects(widgets: List<SettingsWidget>) {
    videoSourceEffects.clear()
    for (widget in widgets) {
        if (widget.type != SettingsWidgetType.videoSource) {
            continue
        }
        val effect = VideoSourceEffect()
        effect.effects = widget.getEffects(model = this).toMutableList()
        videoSourceEffects[widget.id] = effect
    }
}

private fun Model.resetScoreboardVideoEffects(widgets: List<SettingsWidget>) {
    scoreboardEffects.clear()
    for (widget in widgets) {
        if (widget.type != SettingsWidgetType.scoreboard) {
            continue
        }
        scoreboardEffects[widget.id] = ScoreboardEffect(canvasSize = media.getCanvasSize())
    }
}

private fun Model.resetAlertsVideoEffects(widgets: List<SettingsWidget>) {
    alertsEffects.clear()
    for (widget in widgets) {
        if (widget.type != SettingsWidgetType.alerts) {
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
        if (widget.type != SettingsWidgetType.vTuber) {
            continue
        }
        val path = vTuberStorage.makePath(id = widget.vTuber.id)
        when (widget.vTuber.type) {
            SettingsWidgetVTuberType.vrm -> vTuberEffects[widget.id] = VTuberVrmEffect(
                vrm = path.toString(),
                cameraFieldOfView = widget.vTuber.cameraFieldOfView,
                cameraPositionY = widget.vTuber.cameraPositionY
            )
            SettingsWidgetVTuberType.live2D ->
                vTuberEffects[widget.id] = VTuberLive2DEffect(directory = File(path.toString()))
        }
    }
}

private fun Model.resetPngTuberVideoEffects(widgets: List<SettingsWidget>) {
    pngTuberEffects.clear()
    for (widget in widgets) {
        if (widget.type != SettingsWidgetType.pngTuber) {
            continue
        }
        pngTuberEffects[widget.id] = PngTuberEffect(
            modelPath = TODO("no Android counterpart for pngTuberStorage.makePath"),
            costume = 1
        )
    }
}

private fun Model.resetSnapshotVideoEffects(widgets: List<SettingsWidget>) {
    snapshotEffects.clear()
    for (widget in widgets) {
        if (widget.type != SettingsWidgetType.snapshot) {
            continue
        }
        val effect = SnapshotEffect(showtime = widget.snapshot.showtime)
        effect.effects = widget.getEffects(model = this).toMutableList()
        snapshotEffects[widget.id] = effect
    }
}

private fun Model.resetChatVideoEffects(widgets: List<SettingsWidget>) {
    chatEffects.clear()
    for (widget in widgets) {
        if (widget.type != SettingsWidgetType.chat) {
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
        if (widget.type != SettingsWidgetType.chatEmoteCombo) {
            continue
        }
        val effect = ChatEmoteComboEffect(canvasSize = canvasSize(media.getCanvasSize()))
        effect.setSettings(settings = widget.chatEmoteCombo)
        chatEmoteComboEffects[widget.id] = effect
    }
}

private fun Model.resetSlideshowVideoEffects(widgets: List<SettingsWidget>) {
    slideshowEffects.clear()
    for (widget in widgets) {
        if (widget.type != SettingsWidgetType.slideshow) {
            continue
        }
        val slides = mutableListOf<SlideshowEffectSlide>()
        for (slide in widget.slideshow.slides) {
            val widgetId = slide.widgetId ?: continue
            val slideWidget = findWidget(id = widgetId) ?: continue
            val effect: VideoEffect
            when (slideWidget.type) {
                SettingsWidgetType.text -> effect = createTextEffect(widget = slideWidget)
                SettingsWidgetType.image -> effect = createImageEffect(widget = slideWidget)
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
        if (widget.type != SettingsWidgetType.wheelOfLuck) {
            continue
        }
        wheelOfLuckEffects[widget.id] = WheelOfLuckEffect(canvasSize = canvasSize(media.getCanvasSize()))
    }
}

private fun Model.resetBingoCardEffects(widgets: List<SettingsWidget>) {
    bingoCardEffects.clear()
    for (widget in widgets) {
        if (widget.type != SettingsWidgetType.bingoCard) {
            continue
        }
        bingoCardEffects[widget.id] = BingoCardEffect(canvasSize = canvasSize(media.getCanvasSize()))
    }
}

private fun Model.resetPomodoroTimerEffects(widgets: List<SettingsWidget>) {
    pomodoroTimerEffects.clear()
    for (widget in widgets) {
        if (widget.type != SettingsWidgetType.pomodoroTimer) {
            continue
        }
        pomodoroTimerEffects[widget.id] = PomodoroTimerEffect(canvasSize = canvasSize(media.getCanvasSize()))
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
        PomodoroPhase.focus -> {
            settings.breakToFocusSoundId?.let { playPomodoroSound(soundId = it) }
            if (settings.breakToFocusChatMessage.isNotEmpty()) {
                sendChatMessage(message = settings.breakToFocusChatMessage)
            }
        }
        PomodoroPhase.shortBreak -> {
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
    return database.quickButtons.firstOrNull { it.type == type }?.isOn?.value ?: false
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
        findEnabledScene(id = id)?.videoSource?.cameraPosition == SettingsSceneCameraPosition.mediaPlayer
    if (showMediaPlayerControls != streamOverlay.showMediaPlayerControls.value) {
        streamOverlay.showMediaPlayerControls.value = showMediaPlayerControls
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
    if (database.color.lutEnabled && database.color.space == SettingsColorSpace.appleLog) {
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
    if (drawOnStream.lines.value.isNotEmpty()) {
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
    if (drawOnStream.lines.value.isNotEmpty()) {
        drawOnStreamEffect.updateOverlay(
            videoSize = canvasSize(media.getCanvasSize()),
            size = drawOnStreamSize,
            lines = drawOnStream.lines.value,
            mirror = streamOverlay.isFrontCameraSelected.value && !database.mirrorFrontCameraOnStream
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
            SettingsWidgetType.image -> addSceneImageEffects(sceneWidget, widget, effects)
            SettingsWidgetType.text -> addSceneTextEffects(sceneWidget, widget, effects, needsSpeechToText)
            SettingsWidgetType.browser -> addSceneBrowserEffects(
                sceneWidget,
                widget,
                scene,
                effects,
                needsSpeechToText
            )
            SettingsWidgetType.crop -> addSceneCropEffects(widget, scene, effects)
            SettingsWidgetType.map -> addSceneMapEffects(sceneWidget, widget, effects)
            SettingsWidgetType.scene -> addSceneSceneEffects(widget, effects, addedScenes, needsSpeechToText)
            SettingsWidgetType.slideshow -> addSceneSlideshowEffects(sceneWidget, widget, effects)
            SettingsWidgetType.qrCode -> addSceneQrCodeEffects(sceneWidget, widget, effects)
            SettingsWidgetType.alerts -> addSceneAlertsEffects(sceneWidget, widget, effects, needsSpeechToText)
            SettingsWidgetType.videoSource -> addSceneVideoSourceEffects(sceneWidget, widget, effects)
            SettingsWidgetType.scoreboard -> addSceneScoreboardEffects(sceneWidget, widget, effects)
            SettingsWidgetType.vTuber -> addSceneVTuberEffects(sceneWidget, widget, effects)
            SettingsWidgetType.pngTuber -> addScenePngTuberEffects(sceneWidget, widget, effects)
            SettingsWidgetType.snapshot -> addSceneSnapshotEffects(sceneWidget, widget, effects)
            SettingsWidgetType.chat -> addSceneChatEffects(sceneWidget, widget, effects)
            SettingsWidgetType.chatEmoteCombo ->
                addSceneChatEmoteComboEffects(sceneWidget, widget, effects)
            SettingsWidgetType.wheelOfLuck -> addSceneWheelOfLuckEffects(sceneWidget, widget, effects)
            SettingsWidgetType.bingoCard -> addSceneBingoCardEffects(sceneWidget, widget, effects)
            SettingsWidgetType.pomodoroTimer -> addScenePomodoroTimerEffects(sceneWidget, widget, effects)
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
            SettingsWidgetScoreboardSport.padel ->
                sendUpdatePadelScoreboardToWatch(id = widget.id, padel = widget.scoreboard.padel)
            SettingsWidgetScoreboardSport.generic ->
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
        if (widget.widget.type != SettingsWidgetType.crop) {
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
        SettingsWidgetType.scene -> {
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
        SettingsWidgetType.videoSource -> return widget.videoSource.videoSource.isCaptureDevice()
        SettingsWidgetType.vTuber -> return widget.vTuber.videoSource.isCaptureDevice()
        SettingsWidgetType.pngTuber -> return widget.pngTuber.videoSource.isCaptureDevice()
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
        if (widget.type == SettingsWidgetType.scene) {
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
        if (widget.type != SettingsWidgetType.text) {
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
            val newLapTimes = lapTimes.lapTimes.toMutableList()
            newLapTimes[lastIndex] = nowSeconds - currentLapStartTime
            lapTimes.lapTimes = newLapTimes
        }
        for (effect in getTextEffects(id = widget.id)) {
            effect.setLapTimes(lapTimes = widget.text.lapTimes.map { it.lapTimes.toMutableList() }.toMutableList())
        }
    }
}

fun Model.updateTextEffects(now: Instant, timestamp: TimeSource.Monotonic.ValueTimeMark) {
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
            SettingsWidgetType.videoSource ->
                getBuiltinCameraDevices(videoSource = widget.videoSource.videoSource, devices = devices)
            SettingsWidgetType.vTuber ->
                getBuiltinCameraDevices(videoSource = widget.vTuber.videoSource, devices = devices)
            SettingsWidgetType.pngTuber ->
                getBuiltinCameraDevices(videoSource = widget.pngTuber.videoSource, devices = devices)
            SettingsWidgetType.scene -> getBuiltinCameraDevicesForSceneWidget(
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
    val device: CaptureDevice = CaptureDevice(device = com.moblin.android.platform.avfoundation.AVCaptureDevice.withUniqueID(cameraId) ?: return, id = UUID.randomUUID(), isVideoMirrored = false)
    if (devices.none { it.device == device.device }) {
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
        SettingsWidgetType.image, SettingsWidgetType.slideshow -> {
            sceneWidget.layout = sceneWidget.layout.copy(size = 30.0)
        }
        SettingsWidgetType.map, SettingsWidgetType.qrCode -> {
            sceneWidget.layout = sceneWidget.layout.copy(size = 23.0)
        }
        SettingsWidgetType.videoSource, SettingsWidgetType.vTuber, SettingsWidgetType.pngTuber -> {
            sceneWidget.layout = sceneWidget.layout.copy(
                size = 28.0,
                alignment = SettingsAlignment.bottomRight,
            )
        }
        SettingsWidgetType.snapshot -> {
            sceneWidget.layout = sceneWidget.layout.copy(size = 40.0, alignment = SettingsAlignment.topRight)
        }
        SettingsWidgetType.chat -> {
            sceneWidget.layout = sceneWidget.layout.copy(alignment = SettingsAlignment.bottomLeft)
        }
        SettingsWidgetType.chatEmoteCombo -> {
            sceneWidget.layout = sceneWidget.layout.copy(x = 2.0, y = 25.0, size = 10.0)
        }
        SettingsWidgetType.alerts -> {
            sceneWidget.layout = sceneWidget.layout.copy(x = 20.0, y = 5.0)
        }
        SettingsWidgetType.scoreboard -> {
            sceneWidget.layout = sceneWidget.layout.copy(size = defaultScoreboardSize, x = 0.78, y = 1.388)
            when (widget.scoreboard.sport) {
                SettingsWidgetScoreboardSport.golfFullScorecard ->
                    sceneWidget.layout = sceneWidget.layout.copy(alignment = SettingsAlignment.bottomRight)
                else -> {}
            }
        }
        SettingsWidgetType.wheelOfLuck -> {
            sceneWidget.layout = sceneWidget.layout.copy(
                alignment = SettingsAlignment.topRight,
                x = 1.3,
                y = 31.0,
            )
        }
        SettingsWidgetType.bingoCard -> {
            sceneWidget.layout = sceneWidget.layout.copy(
                alignment = SettingsAlignment.topRight,
                x = 1.3,
                y = 33.0,
                size = 33.0,
            )
        }
        SettingsWidgetType.pomodoroTimer -> {
            sceneWidget.layout = sceneWidget.layout.copy(
                alignment = SettingsAlignment.topRight,
                x = 0.78,
                y = 1.388,
                size = 20.0,
            )
        }
        else -> {}
    }
    sceneWidget.layout = sceneWidget.layout.updatingXString().updatingYString().updatingSizeString()
    return sceneWidget
}

private fun Model.updateTimers(
    text: SettingsWidgetText,
    textEffect: TextEffect,
    parts: List<TextFormatPart>
) {
    val length = parts.count { it == TextFormatPart.Timer }
    while (text.timers.size > length) {
        text.timers = text.timers.dropLast(1)
    }
    while (text.timers.size < length) {
        text.timers = text.timers + SettingsWidgetTextTimer()
    }
    textEffect.setTimersEndTime(
        endTimes = text.timers.map {
            Instant.now().plusNanos((utcTimeDeltaFromNow(it.endTime) * 1_000_000_000.0).toLong()).toEpochMilli()
        }.toMutableList()
    )
}

private fun Model.updateStopwatches(
    text: SettingsWidgetText,
    textEffect: TextEffect,
    parts: List<TextFormatPart>
) {
    val length = parts.count { it == TextFormatPart.Stopwatch }
    while (text.stopwatches.size > length) {
        text.stopwatches = text.stopwatches.dropLast(1)
    }
    while (text.stopwatches.size < length) {
        text.stopwatches = text.stopwatches + SettingsWidgetTextStopwatch()
    }
    textEffect.setStopwatches(stopwatches = text.stopwatches.map { it.clone() }.toMutableList())
}

private fun Model.updateCheckboxes(
    text: SettingsWidgetText,
    textEffect: TextEffect,
    parts: List<TextFormatPart>
) {
    val length = parts.count { it == TextFormatPart.Checkbox }
    while (text.checkboxes.size > length) {
        text.checkboxes = text.checkboxes.dropLast(1)
    }
    while (text.checkboxes.size < length) {
        text.checkboxes = text.checkboxes + SettingsWidgetTextCheckbox()
    }
    textEffect.setCheckboxes(checkboxes = text.checkboxes.map { it.checked }.toMutableList())
}

private fun Model.updateRatings(
    text: SettingsWidgetText,
    textEffect: TextEffect,
    parts: List<TextFormatPart>
) {
    val length = parts.count { it == TextFormatPart.Rating }
    while (text.ratings.size > length) {
        text.ratings = text.ratings.dropLast(1)
    }
    while (text.ratings.size < length) {
        text.ratings = text.ratings + SettingsWidgetTextRating()
    }
    textEffect.setRatings(ratings = text.ratings.map { it.rating }.toMutableList())
}

private fun Model.updateLapTimes(
    text: SettingsWidgetText,
    textEffect: TextEffect,
    parts: List<TextFormatPart>
) {
    val length = parts.count { it == TextFormatPart.LapTimes }
    while (text.lapTimes.size > length) {
        text.lapTimes = text.lapTimes.dropLast(1)
    }
    while (text.lapTimes.size < length) {
        text.lapTimes = text.lapTimes + SettingsWidgetTextLapTimes()
    }
    textEffect.setLapTimes(lapTimes = text.lapTimes.map { it.lapTimes.toMutableList() }.toMutableList())
}

private fun Model.updateSubtitles(
    text: SettingsWidgetText,
    textEffect: TextEffect?,
    parts: List<TextFormatPart>
) {
    text.subtitles = mutableListOf()
    for (part in parts) {
        when (part) {
            is TextFormatPart.Subtitles -> {
                val item = SettingsWidgetTextSubtitles()
                text.subtitles = text.subtitles + item
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

private fun List<TextFormatPart>.isWeatherVariable(): Boolean {
    return false
}

private fun List<TextFormatPart>.isGeographyVariable(): Boolean {
    return false
}

private fun List<TextFormatPart>.isGForceVariable(): Boolean {
    return false
}

private fun canvasSize(size: android.util.Size): androidx.compose.ui.geometry.Size {
    return androidx.compose.ui.geometry.Size(size.width.toFloat(), size.height.toFloat())
}

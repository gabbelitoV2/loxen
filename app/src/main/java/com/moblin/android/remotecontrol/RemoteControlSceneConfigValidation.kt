package com.moblin.android.remotecontrol

import com.moblin.android.common.various.isValidHttpUrl
import com.moblin.android.various.model.*
import com.moblin.android.various.settings.*
import com.moblin.android.various.utils.*
import com.moblin.android.view.settings.scenes.widgets.widget.wheelofluck.wheelOfLuckOptionWeights
import java.util.UUID

val remoteControlVideoSourceRotations: List<Double> = listOf(0.0, 90.0, 180.0, 270.0)

data class RemoteControlCameras(
    var ids: Set<CameraId> = emptySet(),
    var cameraId: (SettingsVideoSource) -> CameraId = { "" },
)

private fun <T> isValidChange(old: T, new: T, isValid: (T) -> Boolean): Boolean {
    return old == new || isValid(new)
}

private fun isValidLayoutChange(old: SettingsWidgetLayout, new: SettingsWidgetLayout): Boolean {
    return isValidChange(old.x, new.x) { it in 0.0..100.0 } &&
        isValidChange(old.y, new.y) { it in 0.0..100.0 } &&
        isValidChange(old.size, new.size) { it in 1.0..100.0 }
}

fun remoteControlAllAlerts(alerts: SettingsWidgetAlerts): List<SettingsWidgetAlertsAlert> {
    val all = mutableListOf<SettingsWidgetAlertsAlert>(
        alerts.twitch.follows,
        alerts.twitch.subscriptions,
        alerts.twitch.raids,
        alerts.kick.subscriptions,
        alerts.kick.giftedSubscriptions,
        alerts.kick.hosts,
        alerts.kick.rewards,
        alerts.moblinCloud.tips,
    )
    all += alerts.twitch.cheerBits.map { it.alert }
    all += alerts.kick.kickGifts.map { it.alert }
    all += alerts.chatBot.commands.map { it.alert }
    all += alerts.speechToText.strings.map { it.alert }
    return all
}

fun <T> isValidRemoteControlName(item: T, items: List<T>, oldItems: List<T>): Boolean
    where T : Named, T : Identifiable<UUID>
{
    if (oldItems.firstOrNull { it.id == item.id }?.name == item.name) {
        return true
    }
    return item.name.isNotEmpty() && !items.any { it.id != item.id && it.name == item.name }
}

fun <T> hasUniqueRemoteControlIds(items: List<T>): Boolean where T : Identifiable<UUID> {
    return items.map { it.id }.toSet().size == items.size
}

fun isValidRemoteControlWidgetUpdate(
    widget: SettingsWidget,
    new: SettingsWidget,
    creating: Boolean,
    editableTextTimers: Int,
    database: Database,
    cameras: RemoteControlCameras,
): Boolean {
    if (new.type != widget.type) {
        return false
    }
    return when (widget.type) {
        SettingsWidgetType.text -> isValidTextUpdate(widget.text, new.text, creating, editableTextTimers)
        SettingsWidgetType.browser ->
            isValidBrowserUpdate(widget.browser, new.browser, creating) &&
                isValidEffectsUpdate(widget, new, database)
        SettingsWidgetType.crop -> isValidCropUpdate(widget.crop, new.crop, creating, database)
        SettingsWidgetType.map ->
            isValidChange(widget.map.size, new.map.size) { it in 500.0..10000.0 } &&
                isValidChange(widget.map.delay, new.map.delay) { it in 0.0..10.0 } &&
                isValidEffectsUpdate(widget, new, database)
        SettingsWidgetType.scene -> isValidSceneWidgetUpdate(widget, new, creating, database)
        SettingsWidgetType.qrCode, SettingsWidgetType.image ->
            isValidEffectsUpdate(widget, new, database)
        SettingsWidgetType.snapshot ->
            isValidChange(widget.snapshot.showtime, new.snapshot.showtime) {
                it in listOf(3, 5, 10, 15, 30, 60, 120)
            } &&
                isValidEffectsUpdate(widget, new, database)
        SettingsWidgetType.chat -> isValidChatUpdate(widget.chat, new.chat)
        SettingsWidgetType.chatEmoteCombo ->
            isValidChange(widget.chatEmoteCombo.minimumCombo, new.chatEmoteCombo.minimumCombo) {
                it in 2..10
            } &&
                isValidChange(widget.chatEmoteCombo.resetAfter, new.chatEmoteCombo.resetAfter) {
                    it in 3..10
                }
        SettingsWidgetType.slideshow ->
            isValidSlideshowUpdate(widget.slideshow, new.slideshow, creating, database)
        SettingsWidgetType.wheelOfLuck ->
            isValidWheelOfLuckUpdate(widget.wheelOfLuck, new.wheelOfLuck)
        SettingsWidgetType.bingoCard -> true
        SettingsWidgetType.videoSource ->
            isValidVideoSourceUpdate(widget.videoSource, new.videoSource, cameras) &&
                isValidEffectsUpdate(widget, new, database)
        SettingsWidgetType.vTuber -> isValidVTuberUpdate(widget.vTuber, new.vTuber, cameras)
        SettingsWidgetType.pngTuber ->
            isValidCameraChange(widget.pngTuber.videoSource, new.pngTuber.videoSource, cameras) &&
                isValidSensitivityUpdate(widget.pngTuber.sensitivity, new.pngTuber.sensitivity)
        SettingsWidgetType.pomodoroTimer ->
            isValidPomodoroTimerUpdate(widget.pomodoroTimer, new.pomodoroTimer, database)
        SettingsWidgetType.scoreboard ->
            isValidScoreboardUpdate(widget.scoreboard, new.scoreboard, creating, database)
        SettingsWidgetType.alerts -> isValidAlertsUpdate(widget.alerts, new.alerts, database)
    }
}

fun isValidRemoteControlSceneUpdate(
    scene: SettingsScene,
    new: SettingsScene,
    database: Database,
    cameras: RemoteControlCameras,
): Boolean {
    for (sceneWidget in new.widgets) {
        val oldSceneWidget = scene.widgets.firstOrNull { it.id == sceneWidget.id }
        if (oldSceneWidget != null) {
            if (oldSceneWidget.widgetId != sceneWidget.widgetId ||
                !isValidLayoutChange(oldSceneWidget.layout, sceneWidget.layout)
            ) {
                return false
            }
        } else {
            if (database.findWidget(sceneWidget.widgetId) == null ||
                !isValidLayoutChange(SettingsWidgetLayout(), sceneWidget.layout)
            ) {
                return false
            }
        }
    }
    return isValidCameraChange(scene.videoSource, new.videoSource, cameras) &&
        isValidChange(scene.videoSourceRotation, new.videoSourceRotation) {
            remoteControlVideoSourceRotations.contains(it)
        } &&
        isValidChange(scene.videoStabilizationMode, new.videoStabilizationMode) {
            videoStabilizationModes.contains(it)
        } &&
        isValidChange(scene.quickSwitchGroup, new.quickSwitchGroup) { group ->
            group?.let { it in 1..4 } ?: true
        } &&
        isValidChange(scene.micId, new.micId) { micId ->
            database.mics.mics.value.any { it.id == micId }
        }
}

fun isValidRemoteControlScene(scene: SettingsScene, scenes: List<SettingsScene>, database: Database): Boolean {
    return isValidRemoteControlName(scene, scenes, database.scenes) &&
        hasUniqueRemoteControlIds(scene.widgets) &&
        !introducesDuplicatedWidgets(scenes = scenes, widgets = database.widgets, database = database)
}

fun isValidRemoteControlWidgetScenes(widget: SettingsWidget, sceneIds: List<UUID>, database: Database): Boolean {
    if (sceneIds.toSet().size != sceneIds.size || !sceneIds.all { database.findScene(it) != null }) {
        return false
    }
    val scenes = database.scenes.map { scene ->
        if (!sceneIds.contains(scene.id)) {
            return@map scene
        }
        val newScene = SettingsScene(name = scene.name)
        newScene.id = scene.id
        newScene.widgets = (scene.widgets + SettingsSceneWidget(widgetId = widget.id)).toMutableList()
        newScene
    }
    return !introducesDuplicatedWidgets(
        scenes = scenes,
        widgets = database.widgets + widget,
        database = database,
    )
}

fun remoteControlMergeSceneWidgets(
    sceneWidgets: List<SettingsSceneWidget>,
    newSceneWidgets: List<SettingsSceneWidget>,
): List<SettingsSceneWidget> {
    return newSceneWidgets.map { sceneWidget ->
        val oldSceneWidget = sceneWidgets.firstOrNull { it.id == sceneWidget.id }
        var layout = sceneWidget.layout
        if (layout.alignment != (oldSceneWidget?.layout?.alignment ?: SettingsWidgetLayout().alignment) &&
            layout.alignment.isHorizontalOrVerticalCenter()
        ) {
            layout = layout.copy(positioningLock = false)
        }
        if (oldSceneWidget == null) {
            sceneWidget.layout = layout
            return@map sceneWidget
        }
        oldSceneWidget.layout = layout
        oldSceneWidget
    }
}

private fun introducesDuplicatedWidgets(
    scenes: List<SettingsScene>,
    widgets: List<SettingsWidget>,
    database: Database,
): Boolean {
    val before = SceneResolver(scenes = database.scenes, widgets = widgets)
    val after = SceneResolver(scenes = scenes, widgets = widgets)
    return scenes.any { scene ->
        val beforeCounts = database.findScene(scene.id)?.let {
            before.resolve(it, includeDisabledSceneWidgets = true).duplicatedWidgetCounts()
        } ?: emptyMap()
        return@any after.resolve(scene, includeDisabledSceneWidgets = true).duplicatedWidgetCounts()
            .any { (widgetId, count) ->
                count > (beforeCounts[widgetId] ?: 0)
            }
    }
}

private fun isValidTextUpdate(
    text: SettingsWidgetText,
    newText: SettingsWidgetText,
    creating: Boolean,
    editableTimers: Int,
): Boolean {
    return (!creating || newText.formatString.isNotEmpty()) &&
        isValidChange(text.fontSize, newText.fontSize) { it in 10..200 } &&
        isValidChange(text.width, newText.width) { it > 0 && it < 4000 } &&
        isValidChange(text.cornerRadius, newText.cornerRadius) { it >= 0 && it < 1000 } &&
        isValidChange(text.delay, newText.delay) { it in 0.0..10.0 } &&
        newText.timers.withIndex().all { (index, newTimer) ->
            val timer = if (index < text.timers.size) text.timers[index] else SettingsWidgetTextTimer()
            isValidChange(timer.delta, newTimer.delta) {
                index < editableTimers && it in listOf(1, 2, 5, 15, 60)
            }
        }
}

private fun isValidBrowserUpdate(
    browser: SettingsWidgetBrowser,
    newBrowser: SettingsWidgetBrowser,
    creating: Boolean,
): Boolean {
    val url = newBrowser.url.trim()
    return (!creating || url.isNotEmpty() && isValidHttpUrl(url) == null) &&
        isValidChange(browser.url, newBrowser.url) { isValidHttpUrl(it.trim()) == null } &&
        isValidChange(browser.width, newBrowser.width) { it > 0 && it < 4000 } &&
        isValidChange(browser.height, newBrowser.height) { it > 0 && it < 4000 } &&
        isValidChange(browser.baseFps, newBrowser.baseFps) { it in 1.0..15.0 }
}

private fun isValidCropUpdate(
    crop: SettingsWidgetCrop,
    newCrop: SettingsWidgetCrop,
    creating: Boolean,
    database: Database,
): Boolean {
    return isValidChange(crop.sourceWidgetId, newCrop.sourceWidgetId) { sourceWidgetId ->
        val sourceWidget = database.findWidget(sourceWidgetId)
        if (sourceWidget == null) {
            return@isValidChange creating
        }
        sourceWidget.type == SettingsWidgetType.browser
    } &&
        isValidChange(crop.x, newCrop.x) { it >= 0 } &&
        isValidChange(crop.y, newCrop.y) { it >= 0 } &&
        isValidChange(crop.width, newCrop.width) { it > 0 } &&
        isValidChange(crop.height, newCrop.height) { it > 0 }
}

private fun isValidSceneWidgetUpdate(
    widget: SettingsWidget,
    new: SettingsWidget,
    creating: Boolean,
    database: Database,
): Boolean {
    return isValidChange(widget.scene.sceneId, new.scene.sceneId) { sceneId ->
        if (sceneId == null) {
            return@isValidChange true
        }
        if (!database.scenes.any { it.id == sceneId }) {
            return@isValidChange creating
        }
        SceneResolver(scenes = database.scenes, widgets = database.widgets)
            .selectableScenes(widget = widget)
            .any { it.id == sceneId }
    }
}

private fun isValidChatUpdate(chat: SettingsWidgetChat, newChat: SettingsWidgetChat): Boolean {
    return isValidChange(chat.fontSize, newChat.fontSize) { it in 10.0..50.0 } &&
        isValidChange(chat.maximumNumberOfMessages, newChat.maximumNumberOfMessages) {
            it in 1..5
        } &&
        isValidChange(chat.height, newChat.height) { it in 0.1..1.0 }
}

private fun isValidSlideshowUpdate(
    slideshow: SettingsWidgetSlideshow,
    newSlideshow: SettingsWidgetSlideshow,
    creating: Boolean,
    database: Database,
): Boolean {
    if (creating && newSlideshow.slides.isEmpty()) {
        return false
    }
    return newSlideshow.slides.all { slide ->
        val oldSlide = slideshow.slides.firstOrNull { it.id == slide.id } ?: SettingsWidgetSlideshowSlide()
        return@all isValidChange(oldSlide.widgetId, slide.widgetId) { widgetId ->
            if (widgetId == null) {
                return@isValidChange true
            }
            database.widgets.any {
                it.id == widgetId &&
                    (it.type == SettingsWidgetType.text || it.type == SettingsWidgetType.image)
            }
        } &&
            isValidChange(oldSlide.time, slide.time) {
                it in listOf(5, 10, 15, 30, 45, 60, 90, 120, 180, 240, 300)
            }
    }
}

private fun isValidWheelOfLuckUpdate(
    wheelOfLuck: SettingsWidgetWheelOfLuck,
    newWheelOfLuck: SettingsWidgetWheelOfLuck,
): Boolean {
    return newWheelOfLuck.options.isNotEmpty() && newWheelOfLuck.options.all { option ->
        val oldOption = wheelOfLuck.options.firstOrNull { it.id == option.id } ?: SettingsWidgetWheelOfLuckOption()
        return@all isValidChange(oldOption.weight, option.weight) {
            wheelOfLuckOptionWeights.contains(it)
        } &&
            isValidChange(oldOption.function, option.function) {
                it !in listOf(
                    SettingsControllerFunction.ZOOM_IN,
                    SettingsControllerFunction.ZOOM_OUT,
                    SettingsControllerFunction.GIMBAL_UP,
                    SettingsControllerFunction.GIMBAL_DOWN,
                    SettingsControllerFunction.GIMBAL_LEFT,
                    SettingsControllerFunction.GIMBAL_RIGHT,
                    SettingsControllerFunction.GIMBAL_PRESET,
                )
            }
    }
}

private fun isValidVideoSourceUpdate(
    videoSource: SettingsWidgetVideoSource,
    newVideoSource: SettingsWidgetVideoSource,
    cameras: RemoteControlCameras,
): Boolean {
    return isValidCameraChange(videoSource.videoSource, newVideoSource.videoSource, cameras) &&
        isValidChange(videoSource.rotation, newVideoSource.rotation) {
            remoteControlVideoSourceRotations.contains(it)
        } &&
        isValidChange(videoSource.trackFaceZoom, newVideoSource.trackFaceZoom) { it in 0.0..1.0 }
}

private fun isValidVTuberUpdate(
    vTuber: SettingsWidgetVTuber,
    newVTuber: SettingsWidgetVTuber,
    cameras: RemoteControlCameras,
): Boolean {
    return isValidCameraChange(vTuber.videoSource, newVTuber.videoSource, cameras) &&
        isValidChange(vTuber.cameraPositionY, newVTuber.cameraPositionY) { it in 1.0..2.0 } &&
        isValidChange(vTuber.cameraFieldOfView, newVTuber.cameraFieldOfView) { it in 10.0..30.0 } &&
        isValidChange(vTuber.armsAngle, newVTuber.armsAngle) { it in 20.0..90.0 } &&
        isValidSensitivityUpdate(vTuber.sensitivity, newVTuber.sensitivity)
}

private fun isValidPomodoroTimerUpdate(
    pomodoroTimer: SettingsWidgetPomodoroTimer,
    newPomodoroTimer: SettingsWidgetPomodoroTimer,
    database: Database,
): Boolean {
    val isValidSoundId: (UUID?) -> Boolean = { soundId ->
        soundId?.let { id ->
            database.alertsMediaGallery.allSounds().any { it.id == id }
        } ?: true
    }
    return isValidChange(pomodoroTimer.focusDuration, newPomodoroTimer.focusDuration) {
        it in listOf(1, 2, 3, 5, 10, 15, 20, 25, 30, 45, 60, 90, 120)
    } &&
        isValidChange(pomodoroTimer.breakDuration, newPomodoroTimer.breakDuration) {
            it in listOf(1, 2, 3, 5, 7, 10, 15, 20, 25, 30)
        } &&
        isValidChange(pomodoroTimer.width, newPomodoroTimer.width) { it in 1.0..5.0 } &&
        isValidChange(
            pomodoroTimer.focusToBreakSoundId,
            newPomodoroTimer.focusToBreakSoundId,
            isValidSoundId,
        ) &&
        isValidChange(
            pomodoroTimer.breakToFocusSoundId,
            newPomodoroTimer.breakToFocusSoundId,
            isValidSoundId,
        )
}

private fun isValidScoreboardUpdate(
    scoreboard: SettingsWidgetScoreboard,
    newScoreboard: SettingsWidgetScoreboard,
    creating: Boolean,
    database: Database,
): Boolean {
    val isValidPlayerId: (UUID) -> Boolean = { playerId ->
        creating || database.scoreboardPlayers.any { it.id == playerId }
    }
    val padel = scoreboard.padel
    val newPadel = newScoreboard.padel
    val golf = scoreboard.golf
    val newGolf = newScoreboard.golf
    val modular = scoreboard.modular
    val newModular = newScoreboard.modular
    return isValidChange(padel.homePlayer1, newPadel.homePlayer1, isValidPlayerId) &&
        isValidChange(padel.homePlayer2, newPadel.homePlayer2, isValidPlayerId) &&
        isValidChange(padel.awayPlayer1, newPadel.awayPlayer1, isValidPlayerId) &&
        isValidChange(padel.awayPlayer2, newPadel.awayPlayer2, isValidPlayerId) &&
        isValidChange(scoreboard.generic.clock.maximum, newScoreboard.generic.clock.maximum) {
            it in 1..180
        } &&
        isValidChange(golf.numberOfHoles, newGolf.numberOfHoles) { it in listOf(9, 18) } &&
        isValidChange(golf.pars, newGolf.pars) { it.all { par -> par in 1..9 } } &&
        isValidChange(golf.players.size, newGolf.players.size) { it <= 4 } &&
        isValidChange(modular.clock.maximum, newModular.clock.maximum) { it in 1..180 } &&
        isValidChange(modular.width, newModular.width) { it in 100.0..1000.0 } &&
        isValidChange(modular.rowHeight, newModular.rowHeight) { it in 10.0..150.0 }
}

private fun isValidAlertsUpdate(
    alerts: SettingsWidgetAlerts,
    newAlerts: SettingsWidgetAlerts,
    database: Database,
): Boolean {
    val oldAlerts = remoteControlAllAlerts(alerts)
    return remoteControlAllAlerts(newAlerts).all { alert ->
        val oldAlert = oldAlerts.firstOrNull { it.id == alert.id }
        if (oldAlert == null) {
            return@all isValidAlertUpdate(SettingsWidgetAlertsAlert(), alert, null)
        }
        isValidAlertUpdate(oldAlert, alert, database)
    } &&
        isValidChange(alerts.aiEnabled, newAlerts.aiEnabled) { !it || newAlerts.ai.isConfigured() } &&
        isValidChange(alerts.ai.baseUrl, newAlerts.ai.baseUrl) { isValidHttpUrl(it) == null }
}

private fun isValidAlertUpdate(
    old: SettingsWidgetAlertsAlert,
    new: SettingsWidgetAlertsAlert,
    mediaDatabase: Database?,
): Boolean {
    if (mediaDatabase != null) {
        if (!isValidChange(
                old.imageId,
                new.imageId,
            ) { imageId -> mediaDatabase.alertsMediaGallery.allImages().any { it.id == imageId } } ||
            !isValidChange(
                old.soundId,
                new.soundId,
            ) { soundId -> mediaDatabase.alertsMediaGallery.allSounds().any { it.id == soundId } }
        ) {
            return false
        }
    }
    return isValidChange(old.imageLoopCount, new.imageLoopCount) { it in 1..10 } &&
        isValidChange(old.fontSize, new.fontSize) { it in 10..80 } &&
        isValidChange(old.textToSpeechDelay, new.textToSpeechDelay) { it in 0.0..5.0 }
}

private fun isValidCameraChange(
    old: SettingsVideoSource,
    new: SettingsVideoSource,
    cameras: RemoteControlCameras,
): Boolean {
    return isValidChange(cameras.cameraId(old), cameras.cameraId(new)) { cameras.ids.contains(it) }
}

private fun isValidSensitivityUpdate(old: SettingsSensitivity, new: SettingsSensitivity): Boolean {
    return isValidChange(old.mouth, new.mouth) { it in 0.05..3.0 } &&
        isValidChange(old.eyes, new.eyes) { it in 0.05..5.0 }
}

private fun isValidEffectsUpdate(
    widget: SettingsWidget,
    new: SettingsWidget,
    database: Database,
): Boolean {
    if (!hasUniqueRemoteControlIds(new.effects)) {
        return false
    }
    return new.effects.all { effect ->
        val oldEffect = widget.effects.firstOrNull { it.id == effect.id }
        if (oldEffect != null) {
            return@all oldEffect.type == effect.type && isValidEffectUpdate(oldEffect, effect, database)
        }
        val defaultEffect = SettingsVideoEffect()
        defaultEffect.type = effect.type
        isValidEffectUpdate(defaultEffect, effect, database)
    }
}

private fun isValidEffectUpdate(
    old: SettingsVideoEffect,
    new: SettingsVideoEffect,
    database: Database,
): Boolean {
    return when (new.type) {
        SettingsVideoEffectType.shape -> {
            val oldShape = old.shape
            val newShape = new.shape
            isValidChange(oldShape.cornerRadius, newShape.cornerRadius) { it in 0.0..1.0 } &&
                isValidChange(oldShape.borderWidth, newShape.borderWidth) { it in 0.0..1.0 } &&
                isValidChange(oldShape.cropX, newShape.cropX) { it in 0.0..1.0 } &&
                isValidChange(oldShape.cropY, newShape.cropY) { it in 0.0..1.0 } &&
                isValidChange(oldShape.cropWidth, newShape.cropWidth) { it in 0.0..1.0 } &&
                isValidChange(oldShape.cropHeight, newShape.cropHeight) { it in 0.0..1.0 }
        }
        SettingsVideoEffectType.dewarp360 -> {
            val oldDewarp = old.dewarp360
            val newDewarp = new.dewarp360
            isValidChange(oldDewarp.pan, newDewarp.pan) { it in -180f..180f } &&
                isValidChange(oldDewarp.tilt, newDewarp.tilt) { it in -90f..90f } &&
                isValidChange(oldDewarp.inverseFieldOfView, newDewarp.inverseFieldOfView) {
                    it in 30f..170f
                }
        }
        SettingsVideoEffectType.anamorphicLens ->
            isValidChange(old.anamorphicLens.scale, new.anamorphicLens.scale) { it > 0 && it <= 10 }
        SettingsVideoEffectType.lut ->
            isValidChange(old.lut.lut, new.lut.lut) { lutId ->
                lutId?.let { id -> database.color.allLuts().any { it.id == id } } ?: true
            }
        SettingsVideoEffectType.opacity ->
            isValidChange(old.opacity.opacity, new.opacity.opacity) { it in 0.0..1.0 }
        SettingsVideoEffectType.mask -> {
            val oldPoints = old.mask.points.map { listOf(it.x, it.y) }
            val newPoints = new.mask.points.map { listOf(it.x, it.y) }
            isValidChange(oldPoints, newPoints) { points ->
                points.size >= 3 && points.all { point -> point.all { it in 0.0..100.0 } }
            } &&
                isValidChange(old.mask.tension, new.mask.tension) { it in 0.0..0.5 }
        }
        SettingsVideoEffectType.removeBackground,
        SettingsVideoEffectType.grayScale,
        SettingsVideoEffectType.sepia,
        SettingsVideoEffectType.whirlpool,
        SettingsVideoEffectType.pinch,
        -> true
    }
}

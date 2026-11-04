package com.moblin.android.various.model

import com.moblin.android.common.various.color
import com.moblin.android.remotecontrol.RemoteControlCameras
import com.moblin.android.remotecontrol.isValidRemoteControlWidgetUpdate
import com.moblin.android.remotecontrol.remoteControlAllAlerts
import com.moblin.android.remotecontrol.remoteControlMergeSceneWidgets
import com.moblin.android.various.settings.PomodoroPhase
import com.moblin.android.various.settings.SettingsScene
import com.moblin.android.various.settings.SettingsSceneWidget
import com.moblin.android.various.settings.SettingsVideoEffect
import com.moblin.android.various.settings.SettingsVideoEffectType
import com.moblin.android.various.settings.SettingsVideoSource
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetAlertsChatBotCommandImageType
import com.moblin.android.various.settings.SettingsWidgetGolfScoreboardPlayer
import com.moblin.android.various.settings.SettingsWidgetModularScoreboardTeam
import com.moblin.android.various.settings.SettingsWidgetScoreboardClock
import com.moblin.android.various.settings.SettingsWidgetScoreboardSport
import com.moblin.android.various.settings.SettingsWidgetType
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.various.utils.removeAllWhitespaces
import com.moblin.android.videoeffects.text.TextFormatPart
import com.moblin.android.videoeffects.text.loadTextFormat
import java.util.UUID

data class RemoteControlWidgetUpdate(
    var reloadEffects: Boolean = false,
    var reloadEffectsAttachCamera: Boolean = true,
    var updateScene: Boolean = false,
    var attachCamera: Boolean = false,
    var updateEffects: () -> Unit = {},
)

private class ChangeTracker {
    var changed = false
        private set

    @Suppress("UNUSED_PARAMETER")
    fun <T> apply(get: () -> T, set: (T) -> Unit, new: T): Boolean {
        if (get() == new) {
            return false
        }
        set(new)
        changed = true
        return true
    }

    fun <T> applyItems(items: List<T>, newItems: List<T>, id: (T) -> Any) {
        if (items.map(id) != newItems.map(id)) {
            changed = true
        }
    }
}

fun Model.remoteControlMakeWidget(widget: SettingsWidget): SettingsWidget? {
    if (widget.name.isEmpty()) {
        widget.name = makeUniqueName(SettingsWidget.baseName, database.widgets)
    }
    val newWidget = SettingsWidget(name = widget.name)
    newWidget.id = widget.id
    newWidget.type = widget.type
    if (remoteControlApplyWidgetUpdate(widget = newWidget, new = widget, creating = true) == null) {
        return null
    }
    return newWidget
}

fun Model.remoteControlApplyWidgetUpdate(
    widget: SettingsWidget,
    new: SettingsWidget,
    creating: Boolean,
): RemoteControlWidgetUpdate? {
    if (!isValidRemoteControlWidgetUpdate(
            widget = widget,
            new = new,
            creating = creating,
            editableTextTimers = remoteControlEditableTextTimers(widget = widget, new = new),
            database = database,
            cameras = remoteControlCameras(),
        )
    ) {
        return null
    }
    var update = applyWidgetTypeUpdate(widget = widget, new = new)
    widget.name = new.name
    if (widget.enabled != new.enabled) {
        widget.enabled = new.enabled
        update.updateScene = true
        update.attachCamera = update.attachCamera || isCaptureDeviceWidget(widget = widget)
    }
    return update
}

fun Model.remoteControlWidgetUpdated(update: RemoteControlWidgetUpdate) {
    if (update.reloadEffects) {
        resetSelectedScene(
            changeScene = false,
            attachCamera = update.reloadEffectsAttachCamera || update.attachCamera,
        )
    } else if (update.updateScene) {
        sceneUpdated(attachCamera = update.attachCamera)
    }
    update.updateEffects()
}

private fun Model.remoteControlEditableTextTimers(widget: SettingsWidget, new: SettingsWidget): Int {
    if (widget.type != SettingsWidgetType.text || getTextEffects(id = widget.id).isEmpty()) {
        return 0
    }
    return loadTextFormat(format = new.text.formatString).count { it == TextFormatPart.Timer }
}

fun Model.remoteControlCameras(): RemoteControlCameras =
    RemoteControlCameras(
        ids = videoSources.all().map { it.id }.toSet(),
        cameraId = { this.cameraId(it) },
    )

private fun Model.applyWidgetTypeUpdate(
    widget: SettingsWidget,
    new: SettingsWidget,
): RemoteControlWidgetUpdate = when (widget.type) {
    SettingsWidgetType.text -> updateTextWidget(widget, new)
    SettingsWidgetType.browser -> updateBrowserWidget(widget, new)
    SettingsWidgetType.crop -> updateCropWidget(widget, new)
    SettingsWidgetType.map -> updateMapWidget(widget, new)
    SettingsWidgetType.scene -> updateSceneWidget(widget, new)
    SettingsWidgetType.qrCode -> updateQrCodeWidget(widget, new)
    SettingsWidgetType.image -> updateImageWidget(widget, new)
    SettingsWidgetType.snapshot -> updateSnapshotWidget(widget, new)
    SettingsWidgetType.chat -> updateChatWidget(widget, new)
    SettingsWidgetType.chatEmoteCombo -> updateChatEmoteComboWidget(widget, new)
    SettingsWidgetType.slideshow -> updateSlideshowWidget(widget, new)
    SettingsWidgetType.wheelOfLuck -> updateWheelOfLuckWidget(widget, new)
    SettingsWidgetType.bingoCard -> updateBingoCardWidget(widget, new)
    SettingsWidgetType.videoSource -> updateVideoSourceWidget(widget, new)
    SettingsWidgetType.vTuber -> updateVTuberWidget(widget, new)
    SettingsWidgetType.pngTuber -> updatePngTuberWidget(widget, new)
    SettingsWidgetType.pomodoroTimer -> updatePomodoroTimerWidget(widget, new)
    SettingsWidgetType.scoreboard -> updateScoreboardWidget(widget, new)
    SettingsWidgetType.alerts -> updateAlertsWidget(widget, new)
    else -> RemoteControlWidgetUpdate()
}

private fun Model.updateTextWidget(widget: SettingsWidget, new: SettingsWidget): RemoteControlWidgetUpdate {
    val text = widget.text
    val newText = new.text
    val formatChanged = text.formatString != newText.formatString
    text.formatString = newText.formatString
    val style = ChangeTracker()
    if (style.apply({ text.backgroundColor }, { text.backgroundColor = it }, newText.backgroundColor)) {
        text.backgroundColorColor = text.backgroundColor.color()
    }
    if (style.apply({ text.foregroundColor }, { text.foregroundColor = it }, newText.foregroundColor)) {
        text.foregroundColorColor = text.foregroundColor.color()
    }
    if (style.apply({ text.fontSize }, { text.fontSize = it }, newText.fontSize)) {
        text.fontSizeFloat = text.fontSize.toFloat()
    }
    style.apply({ text.font }, { text.font = it }, newText.font)
    style.apply({ text.fontDesign }, { text.fontDesign = it }, newText.fontDesign)
    style.apply({ text.fontWeight }, { text.fontWeight = it }, newText.fontWeight)
    style.apply({ text.fontMonospacedDigits }, { text.fontMonospacedDigits = it }, newText.fontMonospacedDigits)
    style.apply({ text.horizontalAlignment }, { text.horizontalAlignment = it }, newText.horizontalAlignment)
    style.apply({ text.widthEnabled }, { text.widthEnabled = it }, newText.widthEnabled)
    style.apply({ text.width }, { text.width = it }, newText.width)
    style.apply({ text.cornerRadius }, { text.cornerRadius = it }, newText.cornerRadius)
    val styleChanged = style.changed
    val delayChanged = text.delay != newText.delay
    text.delay = newText.delay
    return RemoteControlWidgetUpdate(reloadEffects = delayChanged) {
        if (styleChanged) {
            for (effect in getTextEffects(id = widget.id)) {
                effect.setBackgroundColor(color = text.backgroundColor)
                effect.setForegroundColor(color = text.foregroundColor)
                effect.setFontSize(size = text.fontSize.toFloat())
                effect.setFont(font = text.font)
                effect.setFontDesign(design = text.fontDesign.toUiKit())
                effect.setFontWeight(weight = text.fontWeight.toSystem())
                effect.setFontMonospacedDigits(enabled = text.fontMonospacedDigits)
                effect.setLayout(
                    alignment = text.horizontalAlignment,
                    width = if (text.widthEnabled) text.width else null,
                    cornerRadius = text.cornerRadius.toDouble(),
                )
            }
            remoteSceneSettingsUpdated()
        }
        if (formatChanged) {
            textWidgetTextChanged(widget = widget)
        }
        for ((timer, newTimer) in text.timers.zip(newText.timers)) {
            timer.delta = newTimer.delta
        }
    }
}

private fun Model.updateBrowserWidget(widget: SettingsWidget, new: SettingsWidget): RemoteControlWidgetUpdate {
    val browser = widget.browser
    val newBrowser = new.browser
    val changes = ChangeTracker()
    val updateVideoEffects = applyEffectsUpdate(widget = widget, new = new, changes = changes)
    changes.apply({ browser.url }, { browser.url = it }, newBrowser.url.trim())
    changes.apply({ browser.styleSheet }, { browser.styleSheet = it }, newBrowser.styleSheet.trim())
    changes.apply({ browser.width }, { browser.width = it }, newBrowser.width)
    changes.apply({ browser.height }, { browser.height = it }, newBrowser.height)
    changes.apply({ browser.localOnly }, { browser.localOnly = it }, newBrowser.localOnly)
    changes.apply({ browser.mode }, { browser.mode = it }, newBrowser.mode)
    changes.apply({ browser.baseFps }, { browser.baseFps = it }, newBrowser.baseFps)
    changes.apply({ browser.moblinAccess }, { browser.moblinAccess = it }, newBrowser.moblinAccess)
    changes.apply({ browser.speechToText }, { browser.speechToText = it }, newBrowser.speechToText)
    return RemoteControlWidgetUpdate(reloadEffects = changes.changed, updateEffects = updateVideoEffects)
}

private fun Model.updateCropWidget(widget: SettingsWidget, new: SettingsWidget): RemoteControlWidgetUpdate {
    val crop = widget.crop
    val newCrop = new.crop
    val changes = ChangeTracker()
    changes.apply({ crop.sourceWidgetId }, { crop.sourceWidgetId = it }, newCrop.sourceWidgetId)
    changes.apply({ crop.x }, { crop.x = it }, newCrop.x)
    changes.apply({ crop.y }, { crop.y = it }, newCrop.y)
    changes.apply({ crop.width }, { crop.width = it }, newCrop.width)
    changes.apply({ crop.height }, { crop.height = it }, newCrop.height)
    return RemoteControlWidgetUpdate(reloadEffects = changes.changed)
}

private fun Model.updateMapWidget(widget: SettingsWidget, new: SettingsWidget): RemoteControlWidgetUpdate {
    val map = widget.map
    val newMap = new.map
    val changes = ChangeTracker()
    val updateVideoEffects = applyEffectsUpdate(widget = widget, new = new, changes = changes)
    changes.apply({ map.northUp }, { map.northUp = it }, newMap.northUp)
    changes.apply({ map.size }, { map.size = it }, newMap.size)
    changes.apply({ map.delay }, { map.delay = it }, newMap.delay)
    return RemoteControlWidgetUpdate(reloadEffects = changes.changed, updateEffects = updateVideoEffects)
}

private fun Model.updateSceneWidget(widget: SettingsWidget, new: SettingsWidget): RemoteControlWidgetUpdate {
    val changes = ChangeTracker()
    changes.apply({ widget.scene.sceneId }, { widget.scene.sceneId = it }, new.scene.sceneId)
    return RemoteControlWidgetUpdate(reloadEffects = changes.changed)
}

private fun Model.updateQrCodeWidget(widget: SettingsWidget, new: SettingsWidget): RemoteControlWidgetUpdate {
    val changes = ChangeTracker()
    val updateVideoEffects = applyEffectsUpdate(widget = widget, new = new, changes = changes)
    changes.apply({ widget.qrCode.message }, { widget.qrCode.message = it }, new.qrCode.message)
    return RemoteControlWidgetUpdate(reloadEffects = changes.changed, updateEffects = updateVideoEffects)
}

private fun Model.updateImageWidget(widget: SettingsWidget, new: SettingsWidget): RemoteControlWidgetUpdate {
    val changes = ChangeTracker()
    val updateVideoEffects = applyEffectsUpdate(widget = widget, new = new, changes = changes)
    return RemoteControlWidgetUpdate(reloadEffects = changes.changed, updateEffects = updateVideoEffects)
}

private fun Model.updateSnapshotWidget(widget: SettingsWidget, new: SettingsWidget): RemoteControlWidgetUpdate {
    val effects = ChangeTracker()
    val updateVideoEffects = applyEffectsUpdate(widget = widget, new = new, changes = effects)
    val showtimeChanged = widget.snapshot.showtime != new.snapshot.showtime
    widget.snapshot.showtime = new.snapshot.showtime
    return RemoteControlWidgetUpdate(reloadEffects = effects.changed) {
        updateVideoEffects()
        if (showtimeChanged) {
            getSnapshotEffect(id = widget.id)?.setSettings(showtime = widget.snapshot.showtime)
        }
    }
}

private fun Model.updateChatWidget(widget: SettingsWidget, new: SettingsWidget): RemoteControlWidgetUpdate {
    val chat = widget.chat
    val newChat = new.chat
    chat.fontSize = newChat.fontSize
    chat.font = newChat.font
    chat.usernameColor = newChat.usernameColor
    chat.usernameColorColor = newChat.usernameColor.color()
    chat.messageColor = newChat.messageColor
    chat.messageColorColor = newChat.messageColor.color()
    chat.backgroundColor = newChat.backgroundColor
    chat.backgroundColorColor = newChat.backgroundColor.color()
    chat.backgroundColorEnabled = newChat.backgroundColorEnabled
    chat.shadowColor = newChat.shadowColor
    chat.shadowColorColor = newChat.shadowColor.color()
    chat.shadowColorEnabled = newChat.shadowColorEnabled
    chat.boldUsername = newChat.boldUsername
    chat.boldMessage = newChat.boldMessage
    chat.badges = newChat.badges
    chat.displayStyle = newChat.displayStyle
    chat.sharedChatIcons = newChat.sharedChatIcons
    chat.height = newChat.height
    chat.maximumNumberOfMessages = newChat.maximumNumberOfMessages
    return RemoteControlWidgetUpdate {
        getChatEffect(id = widget.id)?.setSettings(settings = chat)
        updateWidgetOutlines()
    }
}

private fun Model.updateChatEmoteComboWidget(
    widget: SettingsWidget,
    new: SettingsWidget,
): RemoteControlWidgetUpdate {
    val chatEmoteCombo = widget.chatEmoteCombo
    val newChatEmoteCombo = new.chatEmoteCombo
    chatEmoteCombo.minimumCombo = newChatEmoteCombo.minimumCombo
    chatEmoteCombo.resetAfter = newChatEmoteCombo.resetAfter
    return RemoteControlWidgetUpdate {
        getChatEmoteComboEffect(id = widget.id)?.setSettings(settings = chatEmoteCombo)
    }
}

private fun Model.updateSlideshowWidget(widget: SettingsWidget, new: SettingsWidget): RemoteControlWidgetUpdate {
    val slideshow = widget.slideshow
    val changes = ChangeTracker()
    val slides = new.slideshow.slides.map { slide ->
        val oldSlide = slideshow.slides.firstOrNull { it.id == slide.id }
        if (oldSlide == null) {
            return@map slide
        }
        changes.apply({ oldSlide.widgetId }, { oldSlide.widgetId = it }, slide.widgetId)
        changes.apply({ oldSlide.time }, { oldSlide.time = it }, slide.time)
        oldSlide
    }
    changes.applyItems(slideshow.slides, slides) { it.id }
    slideshow.slides = slides.toMutableList()
    return RemoteControlWidgetUpdate(reloadEffects = changes.changed, reloadEffectsAttachCamera = false)
}

private fun Model.updateWheelOfLuckWidget(widget: SettingsWidget, new: SettingsWidget): RemoteControlWidgetUpdate {
    val wheelOfLuck = widget.wheelOfLuck
    val newWheelOfLuck = new.wheelOfLuck
    wheelOfLuck.advanced = newWheelOfLuck.advanced
    wheelOfLuck.onlyShowWhenSpinning = newWheelOfLuck.onlyShowWhenSpinning
    wheelOfLuck.options = newWheelOfLuck.options.map { option ->
        val oldOption = wheelOfLuck.options.firstOrNull { it.id == option.id }
        if (oldOption == null) {
            return@map option
        }
        oldOption.text = option.text
        oldOption.weight = option.weight
        oldOption.function = option.function
        oldOption.functionData = option.functionData
        oldOption
    }.toMutableList()
    wheelOfLuck.updateText()
    wheelOfLuck.updateTotalWeight()
    return RemoteControlWidgetUpdate {
        updateWheelOfLuckEffect(widget = widget)
    }
}

private fun Model.updateBingoCardWidget(widget: SettingsWidget, new: SettingsWidget): RemoteControlWidgetUpdate {
    val bingoCard = widget.bingoCard
    val newBingoCard = new.bingoCard
    bingoCard.backgroundColor = newBingoCard.backgroundColor
    bingoCard.backgroundColorColor = newBingoCard.backgroundColor.color()
    bingoCard.foregroundColor = newBingoCard.foregroundColor
    bingoCard.foregroundColorColor = newBingoCard.foregroundColor.color()
    bingoCard.squares = newBingoCard.squares
    bingoCard.squaresText = newBingoCard.squaresText
    return RemoteControlWidgetUpdate {
        getBingoCardEffect(id = widget.id)?.setSettings(settings = bingoCard)
    }
}

private fun Model.updateVideoSourceWidget(widget: SettingsWidget, new: SettingsWidget): RemoteControlWidgetUpdate {
    val videoSource = widget.videoSource
    val newVideoSource = new.videoSource
    val effects = ChangeTracker()
    val updateVideoEffects = applyEffectsUpdate(widget = widget, new = new, changes = effects)
    val cameraChanged = cameraId(videoSource.videoSource) != cameraId(newVideoSource.videoSource)
    if (cameraChanged) {
        videoSource.updateCameraId(settingsCameraId = newVideoSource.videoSource.toCameraId())
    }
    val settings = ChangeTracker()
    settings.apply({ videoSource.rotation }, { videoSource.rotation = it }, newVideoSource.rotation)
    settings.apply({ videoSource.mirror }, { videoSource.mirror = it }, newVideoSource.mirror)
    settings.apply(
        { videoSource.trackFaceEnabled },
        { videoSource.trackFaceEnabled = it },
        newVideoSource.trackFaceEnabled,
    )
    settings.apply({ videoSource.trackFaceZoom }, { videoSource.trackFaceZoom = it }, newVideoSource.trackFaceZoom)
    val settingsChanged = settings.changed
    return RemoteControlWidgetUpdate(
        reloadEffects = effects.changed,
        updateScene = cameraChanged,
        attachCamera = cameraChanged,
    ) {
        updateVideoEffects()
        if (settingsChanged) {
            getVideoSourceEffect(id = widget.id)?.setSettings(settings = videoSource.toEffectSettings())
            updateCameraPreviewWidgets()
        }
    }
}

private fun Model.updateVTuberWidget(widget: SettingsWidget, new: SettingsWidget): RemoteControlWidgetUpdate {
    val vTuber = widget.vTuber
    val newVTuber = new.vTuber
    val cameraChanged = cameraId(vTuber.videoSource) != cameraId(newVTuber.videoSource)
    if (cameraChanged) {
        vTuber.updateCameraId(settingsCameraId = newVTuber.videoSource.toCameraId())
    }
    val settings = ChangeTracker()
    settings.apply({ vTuber.cameraPositionY }, { vTuber.cameraPositionY = it }, newVTuber.cameraPositionY)
    settings.apply({ vTuber.cameraFieldOfView }, { vTuber.cameraFieldOfView = it }, newVTuber.cameraFieldOfView)
    settings.apply({ vTuber.mirror }, { vTuber.mirror = it }, newVTuber.mirror)
    settings.apply({ vTuber.sensitivity }, { vTuber.sensitivity = it }, newVTuber.sensitivity)
    settings.apply({ vTuber.armsAngle }, { vTuber.armsAngle = it }, newVTuber.armsAngle)
    val settingsChanged = settings.changed
    return RemoteControlWidgetUpdate(updateScene = cameraChanged, attachCamera = cameraChanged) {
        if (settingsChanged) {
            getVTuberEffect(id = widget.id)?.setSettings(
                cameraFieldOfView = vTuber.cameraFieldOfView,
                cameraPositionY = vTuber.cameraPositionY,
                mirror = vTuber.mirror,
                sensitivity = vTuber.sensitivity,
                armsAngle = vTuber.armsAngle,
            )
        }
    }
}

private fun Model.updatePngTuberWidget(widget: SettingsWidget, new: SettingsWidget): RemoteControlWidgetUpdate {
    val pngTuber = widget.pngTuber
    val newPngTuber = new.pngTuber
    val cameraChanged = cameraId(pngTuber.videoSource) != cameraId(newPngTuber.videoSource)
    if (cameraChanged) {
        pngTuber.updateCameraId(settingsCameraId = newPngTuber.videoSource.toCameraId())
    }
    val settings = ChangeTracker()
    settings.apply({ pngTuber.mirror }, { pngTuber.mirror = it }, newPngTuber.mirror)
    settings.apply({ pngTuber.sensitivity }, { pngTuber.sensitivity = it }, newPngTuber.sensitivity)
    val settingsChanged = settings.changed
    return RemoteControlWidgetUpdate(updateScene = cameraChanged, attachCamera = cameraChanged) {
        if (settingsChanged) {
            getPngTuberEffect(id = widget.id)?.setSettings(
                mirror = pngTuber.mirror,
                sensitivity = pngTuber.sensitivity,
            )
        }
    }
}

private fun Model.updatePomodoroTimerWidget(
    widget: SettingsWidget,
    new: SettingsWidget,
): RemoteControlWidgetUpdate {
    val pomodoroTimer = widget.pomodoroTimer
    val newPomodoroTimer = new.pomodoroTimer
    if (pomodoroTimer.focusDuration != newPomodoroTimer.focusDuration) {
        pomodoroTimer.focusDuration = newPomodoroTimer.focusDuration
        if (pomodoroTimer.phase == PomodoroPhase.focus && !pomodoroTimer.isRunning) {
            pomodoroTimer.secondsRemaining = pomodoroTimer.focusDuration * 60
        }
    }
    if (pomodoroTimer.breakDuration != newPomodoroTimer.breakDuration) {
        pomodoroTimer.breakDuration = newPomodoroTimer.breakDuration
        if (pomodoroTimer.phase == PomodoroPhase.shortBreak && !pomodoroTimer.isRunning) {
            pomodoroTimer.secondsRemaining = pomodoroTimer.breakDuration * 60
        }
    }
    pomodoroTimer.width = newPomodoroTimer.width
    pomodoroTimer.focusName = newPomodoroTimer.focusName
    pomodoroTimer.breakName = newPomodoroTimer.breakName
    pomodoroTimer.focusIcon = newPomodoroTimer.focusIcon
    pomodoroTimer.breakIcon = newPomodoroTimer.breakIcon
    pomodoroTimer.backgroundColor = newPomodoroTimer.backgroundColor
    pomodoroTimer.backgroundColorColor = newPomodoroTimer.backgroundColor.color()
    pomodoroTimer.foregroundColor = newPomodoroTimer.foregroundColor
    pomodoroTimer.foregroundColorColor = newPomodoroTimer.foregroundColor.color()
    pomodoroTimer.focusColor = newPomodoroTimer.focusColor
    pomodoroTimer.focusColorColor = newPomodoroTimer.focusColor.color()
    pomodoroTimer.breakColor = newPomodoroTimer.breakColor
    pomodoroTimer.breakColorColor = newPomodoroTimer.breakColor.color()
    pomodoroTimer.focusToBreakSoundId = newPomodoroTimer.focusToBreakSoundId
    pomodoroTimer.breakToFocusSoundId = newPomodoroTimer.breakToFocusSoundId
    pomodoroTimer.focusToBreakChatMessage = newPomodoroTimer.focusToBreakChatMessage
    pomodoroTimer.breakToFocusChatMessage = newPomodoroTimer.breakToFocusChatMessage
    return RemoteControlWidgetUpdate {
        updateWidgetOutlines()
    }
}

private fun Model.updateScoreboardWidget(widget: SettingsWidget, new: SettingsWidget): RemoteControlWidgetUpdate {
    val scoreboard = widget.scoreboard
    val newScoreboard = new.scoreboard
    val padel = scoreboard.padel
    val newPadel = newScoreboard.padel
    val generic = scoreboard.generic
    val newGeneric = newScoreboard.generic
    val golf = scoreboard.golf
    val newGolf = newScoreboard.golf
    val modular = scoreboard.modular
    val newModular = newScoreboard.modular
    if (scoreboard.sport != newScoreboard.sport) {
        scoreboard.sport = newScoreboard.sport
        modular.config = null
    }
    scoreboard.textColor = newScoreboard.textColor
    scoreboard.primaryBackgroundColor = newScoreboard.primaryBackgroundColor
    scoreboard.secondaryBackgroundColor = newScoreboard.secondaryBackgroundColor
    scoreboard.loadColors()
    padel.type = newPadel.type
    padel.homePlayer1 = newPadel.homePlayer1
    padel.homePlayer2 = newPadel.homePlayer2
    padel.awayPlayer1 = newPadel.awayPlayer1
    padel.awayPlayer2 = newPadel.awayPlayer2
    generic.home = newGeneric.home
    generic.away = newGeneric.away
    generic.title = newGeneric.title
    applyScoreboardClockUpdate(generic.clock, newGeneric.clock)
    golf.title = newGolf.title
    if (golf.numberOfHoles != newGolf.numberOfHoles) {
        golf.numberOfHoles = newGolf.numberOfHoles
        golf.currentHole = 0
    }
    golf.updatePars(newGolf.pars)
    golf.players = newGolf.players.map { player ->
        val oldPlayer = golf.players.firstOrNull { it.id == player.id }
        if (oldPlayer != null) {
            oldPlayer.name = player.name
            oldPlayer
        } else {
            val newPlayer = SettingsWidgetGolfScoreboardPlayer(name = player.name)
            newPlayer.id = player.id
            newPlayer
        }
    }.toMutableList()
    golf.showPars = newGolf.showPars
    applyScoreboardTeamUpdate(modular.home, newModular.home)
    applyScoreboardTeamUpdate(modular.away, newModular.away)
    applyScoreboardClockUpdate(modular.clock, newModular.clock)
    modular.layout = newModular.layout
    modular.width = newModular.width
    modular.rowHeight = newModular.rowHeight
    modular.isBold = newModular.isBold
    modular.showTitle = newModular.showTitle
    modular.showMoreStats = newModular.showMoreStats
    modular.showGlobalStatsBlock = newModular.showGlobalStatsBlock
    modular.showClock = newModular.showClock
    return RemoteControlWidgetUpdate {
        remoteControlScoreboardWidgetUpdated(widget = widget)
    }
}

private fun Model.updateAlertsWidget(widget: SettingsWidget, new: SettingsWidget): RemoteControlWidgetUpdate {
    val alerts = widget.alerts
    val newAlerts = new.alerts
    val oldAlerts = remoteControlAllAlerts(alerts)
    for (alert in remoteControlAllAlerts(newAlerts)) {
        val oldAlert = oldAlerts.firstOrNull { it.id == alert.id }
        if (oldAlert != null) {
            alert.videoName = oldAlert.videoName
        } else {
            alert.videoName = ""
            fixAlert(alert = alert)
        }
    }
    for (command in newAlerts.chatBot.commands) {
        command.name = command.name.lowercase().removeAllWhitespaces()
        command.imageType = alerts.chatBot.commands.firstOrNull { it.id == command.id }?.imageType
            ?: SettingsWidgetAlertsChatBotCommandImageType.file
    }
    newAlerts.twitch.cheers = alerts.twitch.cheers
    newAlerts.twitch.redemptions = alerts.twitch.redemptions
    newAlerts.quickButton = alerts.quickButton
    newAlerts.needsSubtitles = alerts.needsSubtitles
    newAlerts.ttsMonster = alerts.ttsMonster
    widget.alerts = newAlerts
    return RemoteControlWidgetUpdate {
        fixAlertMedias()
    }
}

fun Model.remoteControlApplySceneUpdate(scene: SettingsScene, new: SettingsScene, creating: Boolean) {
    val enabledChanged = scene.enabled != new.enabled
    val cameraChanged = cameraId(scene.videoSource) != cameraId(new.videoSource)
    val attachCameraChanged = (!creating && cameraChanged) ||
        scene.mirror != new.mirror ||
        scene.overrideVideoStabilizationMode != new.overrideVideoStabilizationMode ||
        scene.videoStabilizationMode != new.videoStabilizationMode ||
        scene.fillFrame != new.fillFrame
    val rotationChanged = scene.videoSourceRotation != new.videoSourceRotation
    val quickSwitchGroupChanged = scene.quickSwitchGroup != new.quickSwitchGroup
    val overrideMicChanged = scene.overrideMic != new.overrideMic
    val micIdChanged = scene.micId != new.micId
    val backgroundColorChanged = scene.backgroundColor != new.backgroundColor
    val oldWidgetIds = scene.widgets.map { it.id }
    val newWidgetIds = new.widgets.map { it.id }
    val widgetsRemovedOrMoved = oldWidgetIds.any { !newWidgetIds.contains(it) } ||
        oldWidgetIds.filter { newWidgetIds.contains(it) } != newWidgetIds.filter { oldWidgetIds.contains(it) }
    val widgetsAdded = newWidgetIds.any { !oldWidgetIds.contains(it) }
    val layoutsChanged = new.widgets.any { sceneWidget ->
        scene.widgets.firstOrNull { it.id == sceneWidget.id }?.let { it.layout != sceneWidget.layout } ?: false
    }
    val captureDeviceWidgetsChanged =
        captureDeviceWidgetIds(scene.widgets) != captureDeviceWidgetIds(new.widgets)
    scene.name = new.name
    scene.enabled = new.enabled
    if (cameraChanged) {
        scene.updateCameraId(settingsCameraId = new.videoSource.toCameraId())
    }
    scene.videoSourceRotation = new.videoSourceRotation
    scene.mirror = new.mirror
    scene.overrideVideoStabilizationMode = new.overrideVideoStabilizationMode
    scene.videoStabilizationMode = new.videoStabilizationMode
    scene.fillFrame = new.fillFrame
    scene.quickSwitchGroup = new.quickSwitchGroup
    scene.overrideMic = new.overrideMic
    scene.micId = new.micId
    scene.backgroundColor = new.backgroundColor
    scene.backgroundColorColor = new.backgroundColor.color()
    scene.widgets = remoteControlMergeSceneWidgets(scene.widgets, new.widgets).toMutableList()
    val selected = getSelectedScene() === scene
    val widgetsChanged = widgetsRemovedOrMoved || widgetsAdded || layoutsChanged
    if (enabledChanged && selected) {
        resetSelectedScene()
    } else if (quickSwitchGroupChanged && selected) {
        resetSelectedScene(changeScene = false, attachCamera = true)
    } else if (attachCameraChanged || rotationChanged || widgetsChanged) {
        sceneUpdated(
            attachCamera = attachCameraChanged || (selected && captureDeviceWidgetsChanged),
            updateRemoteScene = widgetsChanged,
        )
    }
    if (enabledChanged && !selected) {
        sceneSelector.sceneIndex.value += 0
    }
    if (overrideMicChanged || (micIdChanged && selected)) {
        switchMicIfNeededAfterSceneSwitch()
    }
    if (backgroundColorChanged) {
        Unit
    }
    if (widgetsRemovedOrMoved) {
        sceneSettingsPanelSceneId.value += 1
    }
}

private fun Model.captureDeviceWidgetIds(sceneWidgets: List<SettingsSceneWidget>): Set<UUID> =
    sceneWidgets
        .filter { sceneWidget ->
            findWidget(id = sceneWidget.widgetId)?.let { isCaptureDeviceWidget(widget = it) } ?: false
        }
        .map { it.widgetId }
        .toSet()

private fun Model.remoteControlScoreboardWidgetUpdated(widget: SettingsWidget) {
    val scoreboard = widget.scoreboard
    when (scoreboard.sport) {
        SettingsWidgetScoreboardSport.generic ->
            sendUpdateGenericScoreboardToWatch(id = widget.id, generic = scoreboard.generic)
        SettingsWidgetScoreboardSport.padel ->
            sendUpdatePadelScoreboardToWatch(id = widget.id, padel = scoreboard.padel)
        else -> {}
    }
    remoteControlScoreboardUpdate(scoreboard = scoreboard)
    getScoreboardEffect(id = widget.id)?.update(
        scoreboard = scoreboard,
        config = getModularScoreboardConfig(scoreboard = scoreboard),
        players = database.scoreboardPlayers,
    )
}

private fun Model.cameraId(videoSource: SettingsVideoSource): CameraId =
    getCameraId(settingsCameraId = videoSource.toCameraId())

private fun Model.applyEffectsUpdate(
    widget: SettingsWidget,
    new: SettingsWidget,
    changes: ChangeTracker,
): () -> Unit {
    val updatedEffects = mutableListOf<SettingsVideoEffect>()
    val effects = new.effects.map { effect ->
        val oldEffect = widget.effects.firstOrNull { it.id == effect.id }
        if (oldEffect == null) {
            return@map effect
        }
        changes.apply({ oldEffect.enabled }, { oldEffect.enabled = it }, effect.enabled)
        if (applyEffectSettingsUpdate(oldEffect, effect)) {
            updatedEffects.add(oldEffect)
        }
        oldEffect
    }
    changes.applyItems(widget.effects, effects) { it.id }
    widget.effects = effects.toMutableList()
    return {
        for (effect in updatedEffects) {
            updateWidgetVideoEffect(widget, effect)
        }
    }
}

private fun Model.applyEffectSettingsUpdate(
    effect: SettingsVideoEffect,
    new: SettingsVideoEffect,
): Boolean {
    val changes = ChangeTracker()
    when (effect.type) {
        SettingsVideoEffectType.removeBackground -> {
            val removeBackground = effect.removeBackground
            if (changes.apply(
                    { removeBackground.from },
                    { removeBackground.from = it },
                    new.removeBackground.from,
                )
            ) {
                removeBackground.fromColor = removeBackground.from.color()
            }
            if (changes.apply(
                    { removeBackground.to },
                    { removeBackground.to = it },
                    new.removeBackground.to,
                )
            ) {
                removeBackground.toColor = removeBackground.to.color()
            }
        }
        SettingsVideoEffectType.shape -> {
            val shape = effect.shape
            changes.apply({ shape.cornerRadius }, { shape.cornerRadius = it }, new.shape.cornerRadius)
            changes.apply({ shape.borderWidth }, { shape.borderWidth = it }, new.shape.borderWidth)
            if (changes.apply({ shape.borderColor }, { shape.borderColor = it }, new.shape.borderColor)) {
                shape.borderColorColor = shape.borderColor.color()
            }
            changes.apply({ shape.cropEnabled }, { shape.cropEnabled = it }, new.shape.cropEnabled)
            changes.apply({ shape.cropX }, { shape.cropX = it }, new.shape.cropX)
            changes.apply({ shape.cropY }, { shape.cropY = it }, new.shape.cropY)
            changes.apply({ shape.cropWidth }, { shape.cropWidth = it }, new.shape.cropWidth)
            changes.apply({ shape.cropHeight }, { shape.cropHeight = it }, new.shape.cropHeight)
        }
        SettingsVideoEffectType.dewarp360 -> {
            val dewarp360 = effect.dewarp360
            changes.apply({ dewarp360.pan }, { dewarp360.pan = it }, new.dewarp360.pan)
            changes.apply({ dewarp360.tilt }, { dewarp360.tilt = it }, new.dewarp360.tilt)
            if (changes.apply(
                    { dewarp360.inverseFieldOfView },
                    { dewarp360.inverseFieldOfView = it },
                    new.dewarp360.inverseFieldOfView,
                )
            ) {
                dewarp360.updateZoomFromInverseFieldOfView()
            }
        }
        SettingsVideoEffectType.anamorphicLens -> {
            changes.apply(
                { effect.anamorphicLens.scale },
                { effect.anamorphicLens.scale = it },
                new.anamorphicLens.scale,
            )
        }
        SettingsVideoEffectType.lut -> {
            changes.apply({ effect.lut.lut }, { effect.lut.lut = it }, new.lut.lut)
        }
        SettingsVideoEffectType.opacity -> {
            changes.apply({ effect.opacity.opacity }, { effect.opacity.opacity = it }, new.opacity.opacity)
        }
        SettingsVideoEffectType.mask -> {
            val mask = effect.mask
            changes.apply({ mask.points }, { mask.points = it }, new.mask.points)
            changes.apply({ mask.inverted }, { mask.inverted = it }, new.mask.inverted)
            changes.apply({ mask.tension }, { mask.tension = it }, new.mask.tension)
            changes.apply({ mask.backgroundType }, { mask.backgroundType = it }, new.mask.backgroundType)
            if (changes.apply(
                    { mask.backgroundColor },
                    { mask.backgroundColor = it },
                    new.mask.backgroundColor,
                )
            ) {
                mask.backgroundColorColor = mask.backgroundColor.color()
            }
            if (changes.apply(
                    { mask.backgroundColor2 },
                    { mask.backgroundColor2 = it },
                    new.mask.backgroundColor2,
                )
            ) {
                mask.backgroundColorColor2 = mask.backgroundColor2.color()
            }
        }
        SettingsVideoEffectType.grayScale,
        SettingsVideoEffectType.sepia,
        SettingsVideoEffectType.whirlpool,
        SettingsVideoEffectType.pinch,
        -> {}
        else -> {}
    }
    return changes.changed
}

private fun Model.updateWidgetVideoEffect(widget: SettingsWidget, effect: SettingsVideoEffect) {
    when (effect.type) {
        SettingsVideoEffectType.removeBackground ->
            getWidgetRemoveBackgroundEffect(widget, effect)?.setColorRange(
                from = effect.removeBackground.from,
                to = effect.removeBackground.to,
            )
        SettingsVideoEffectType.shape -> {
            getWidgetShapeEffect(widget, effect)?.setSettings(settings = effect.shape.toSettings())
            updateCameraPreviewWidgets()
        }
        SettingsVideoEffectType.dewarp360 ->
            getWidgetDewarp360Effect(widget, effect)?.setSettings(settings = effect.dewarp360.toSettings())
        SettingsVideoEffectType.anamorphicLens ->
            getWidgetAnamorphicLensEffect(widget, effect)?.setSettings(
                settings = effect.anamorphicLens.clone(),
            )
        SettingsVideoEffectType.lut ->
            getWidgetLutEffect(widget, effect)?.setLut(
                lut = getLogLutById(id = effect.lut.lut),
                imageStorage = imageStorage,
            ) { title, subTitle -> makeErrorToast(title = title, subTitle = subTitle) }
        SettingsVideoEffectType.opacity ->
            getWidgetOpacityEffect(widget, effect)?.setOpacity(opacity = effect.opacity.opacity)
        SettingsVideoEffectType.mask ->
            getWidgetMaskEffect(widget, effect)?.setSettings(settings = effect.mask.toEffectSettings())
        SettingsVideoEffectType.grayScale,
        SettingsVideoEffectType.sepia,
        SettingsVideoEffectType.whirlpool,
        SettingsVideoEffectType.pinch,
        -> {}
        else -> {}
    }
}

private fun applyScoreboardClockUpdate(
    clock: SettingsWidgetScoreboardClock,
    new: SettingsWidgetScoreboardClock,
) {
    if (clock.maximum == new.maximum && clock.direction == new.direction) {
        return
    }
    clock.maximum = new.maximum
    clock.direction = new.direction
    clock.reset()
}

private fun applyScoreboardTeamUpdate(
    team: SettingsWidgetModularScoreboardTeam,
    new: SettingsWidgetModularScoreboardTeam,
) {
    team.name = new.name
    team.textColor = new.textColor
    team.backgroundColor = new.backgroundColor
    team.loadColors()
}

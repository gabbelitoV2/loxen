package com.moblin.android.various.model

import com.moblin.android.common.various.color
import com.moblin.android.remotecontrol.RemoteControlCameras
import com.moblin.android.remotecontrol.isValidRemoteControlWidgetUpdate
import com.moblin.android.remotecontrol.remoteControlAllAlerts
import com.moblin.android.remotecontrol.remoteControlMergeSceneWidgets
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
import com.moblin.android.various.settings.SettingsWidgetType
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.various.utils.removeAllWhitespaces
import com.moblin.android.videoeffects.text.TextFormatPart
import com.moblin.android.videoeffects.text.loadTextFormat
import java.util.UUID

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
    if (!remoteControlApplyWidgetUpdate(widget = newWidget, new = widget, creating = true)) {
        return null
    }
    return newWidget
}

fun Model.remoteControlApplyWidgetUpdate(
    widget: SettingsWidget,
    new: SettingsWidget,
    creating: Boolean,
): Boolean {
    if (!isValidRemoteControlWidgetUpdate(
            widget = widget,
            new = new,
            creating = creating,
            editableTextTimers = remoteControlEditableTextTimers(widget = widget, new = new),
            database = database,
            cameras = remoteControlCameras(),
        )
    ) {
        return false
    }
    applyWidgetTypeUpdate(widget = widget, new = new, creating = creating)
    widget.name = new.name
    if (widget.enabled != new.enabled) {
        widget.enabled = new.enabled
        if (!creating) {
            widgetEnabledChanged(widget = widget)
        }
    }
    return true
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
    creating: Boolean,
) {
    when (widget.type) {
        SettingsWidgetType.text -> updateTextWidget(widget, new, creating)
        SettingsWidgetType.browser -> updateBrowserWidget(widget, new, creating)
        SettingsWidgetType.crop -> updateCropWidget(widget, new, creating)
        SettingsWidgetType.map -> updateMapWidget(widget, new, creating)
        SettingsWidgetType.scene -> updateSceneWidget(widget, new, creating)
        SettingsWidgetType.qrCode -> updateQrCodeWidget(widget, new, creating)
        SettingsWidgetType.image -> updateImageWidget(widget, new, creating)
        SettingsWidgetType.snapshot -> updateSnapshotWidget(widget, new, creating)
        SettingsWidgetType.chat -> updateChatWidget(widget, new, creating)
        SettingsWidgetType.chatEmoteCombo -> updateChatEmoteComboWidget(widget, new, creating)
        SettingsWidgetType.slideshow -> updateSlideshowWidget(widget, new, creating)
        SettingsWidgetType.wheelOfLuck -> updateWheelOfLuckWidget(widget, new, creating)
        SettingsWidgetType.bingoCard -> updateBingoCardWidget(widget, new, creating)
        SettingsWidgetType.videoSource -> updateVideoSourceWidget(widget, new, creating)
        SettingsWidgetType.vTuber -> updateVTuberWidget(widget, new, creating)
        SettingsWidgetType.pngTuber -> updatePngTuberWidget(widget, new, creating)
        SettingsWidgetType.pomodoroTimer -> updatePomodoroTimerWidget(widget, new, creating)
        SettingsWidgetType.scoreboard -> updateScoreboardWidget(widget, new, creating)
        SettingsWidgetType.alerts -> updateAlertsWidget(widget, new, creating)
        else -> {}
    }
}

private fun Model.updateTextWidget(widget: SettingsWidget, new: SettingsWidget, creating: Boolean) {
    val text = widget.text
    val newText = new.text
    val formatChanged = text.formatString != newText.formatString
    text.formatString = newText.formatString
    val changes = ChangeTracker()
    val backgroundColorChanged =
        changes.apply({ text.backgroundColor }, { text.backgroundColor = it }, newText.backgroundColor)
    if (backgroundColorChanged) {
        text.backgroundColorColor = text.backgroundColor.color()
    }
    val foregroundColorChanged =
        changes.apply({ text.foregroundColor }, { text.foregroundColor = it }, newText.foregroundColor)
    if (foregroundColorChanged) {
        text.foregroundColorColor = text.foregroundColor.color()
    }
    val fontSizeChanged = changes.apply({ text.fontSize }, { text.fontSize = it }, newText.fontSize)
    if (fontSizeChanged) {
        text.fontSizeFloat = text.fontSize.toFloat()
    }
    val fontChanged = changes.apply({ text.font }, { text.font = it }, newText.font)
    val fontDesignChanged = changes.apply({ text.fontDesign }, { text.fontDesign = it }, newText.fontDesign)
    val fontWeightChanged = changes.apply({ text.fontWeight }, { text.fontWeight = it }, newText.fontWeight)
    val fontMonospacedDigitsChanged = changes.apply(
        { text.fontMonospacedDigits },
        { text.fontMonospacedDigits = it },
        newText.fontMonospacedDigits,
    )
    val layout = ChangeTracker()
    layout.apply({ text.horizontalAlignment }, { text.horizontalAlignment = it }, newText.horizontalAlignment)
    layout.apply({ text.widthEnabled }, { text.widthEnabled = it }, newText.widthEnabled)
    layout.apply({ text.width }, { text.width = it }, newText.width)
    layout.apply({ text.cornerRadius }, { text.cornerRadius = it }, newText.cornerRadius)
    val delayChanged = text.delay != newText.delay
    text.delay = newText.delay
    if (creating) {
        return
    }
    if (delayChanged) {
        resetSelectedScene(changeScene = false)
    }
    if (backgroundColorChanged) {
        textWidgetBackgroundColorChanged(widget = widget)
    }
    if (foregroundColorChanged) {
        textWidgetForegroundColorChanged(widget = widget)
    }
    if (fontSizeChanged) {
        textWidgetFontSizeChanged(widget = widget)
    }
    if (fontChanged) {
        textWidgetFontChanged(widget = widget)
    }
    if (fontDesignChanged) {
        textWidgetFontDesignChanged(widget = widget)
    }
    if (fontWeightChanged) {
        textWidgetFontWeightChanged(widget = widget)
    }
    if (fontMonospacedDigitsChanged) {
        textWidgetFontMonospacedDigitsChanged(widget = widget)
    }
    if (layout.changed) {
        textWidgetLayoutChanged(widget = widget)
    }
    if (formatChanged) {
        textWidgetTextChanged(widget = widget)
    }
    for ((timer, newTimer) in text.timers.zip(newText.timers)) {
        timer.delta = newTimer.delta
    }
}

private fun Model.updateBrowserWidget(widget: SettingsWidget, new: SettingsWidget, creating: Boolean) {
    val browser = widget.browser
    val newBrowser = new.browser
    val changes = ChangeTracker()
    applyEffectsUpdate(widget = widget, new = new, changes = changes)
    changes.apply({ browser.url }, { browser.url = it }, newBrowser.url.trim())
    changes.apply({ browser.styleSheet }, { browser.styleSheet = it }, newBrowser.styleSheet.trim())
    changes.apply({ browser.width }, { browser.width = it }, newBrowser.width)
    changes.apply({ browser.height }, { browser.height = it }, newBrowser.height)
    changes.apply({ browser.localOnly }, { browser.localOnly = it }, newBrowser.localOnly)
    changes.apply({ browser.mode }, { browser.mode = it }, newBrowser.mode)
    changes.apply({ browser.baseFps }, { browser.baseFps = it }, newBrowser.baseFps)
    changes.apply({ browser.moblinAccess }, { browser.moblinAccess = it }, newBrowser.moblinAccess)
    changes.apply({ browser.speechToText }, { browser.speechToText = it }, newBrowser.speechToText)
    if (changes.changed && !creating) {
        resetSelectedScene(changeScene = false)
    }
}

private fun Model.updateCropWidget(widget: SettingsWidget, new: SettingsWidget, creating: Boolean) {
    val crop = widget.crop
    val newCrop = new.crop
    val changes = ChangeTracker()
    changes.apply({ crop.sourceWidgetId }, { crop.sourceWidgetId = it }, newCrop.sourceWidgetId)
    changes.apply({ crop.x }, { crop.x = it }, newCrop.x)
    changes.apply({ crop.y }, { crop.y = it }, newCrop.y)
    changes.apply({ crop.width }, { crop.width = it }, newCrop.width)
    changes.apply({ crop.height }, { crop.height = it }, newCrop.height)
    if (changes.changed && !creating) {
        resetSelectedScene(changeScene = false)
    }
}

private fun Model.updateMapWidget(widget: SettingsWidget, new: SettingsWidget, creating: Boolean) {
    val map = widget.map
    val newMap = new.map
    val changes = ChangeTracker()
    applyEffectsUpdate(widget = widget, new = new, changes = changes)
    changes.apply({ map.northUp }, { map.northUp = it }, newMap.northUp)
    changes.apply({ map.size }, { map.size = it }, newMap.size)
    changes.apply({ map.delay }, { map.delay = it }, newMap.delay)
    if (changes.changed && !creating) {
        resetSelectedScene(changeScene = false)
    }
}

private fun Model.updateSceneWidget(widget: SettingsWidget, new: SettingsWidget, creating: Boolean) {
    val changes = ChangeTracker()
    changes.apply({ widget.scene.sceneId }, { widget.scene.sceneId = it }, new.scene.sceneId)
    if (changes.changed && !creating) {
        resetSelectedScene(changeScene = false)
    }
}

private fun Model.updateQrCodeWidget(widget: SettingsWidget, new: SettingsWidget, creating: Boolean) {
    val changes = ChangeTracker()
    applyEffectsUpdate(widget = widget, new = new, changes = changes)
    changes.apply({ widget.qrCode.message }, { widget.qrCode.message = it }, new.qrCode.message)
    if (changes.changed && !creating) {
        resetSelectedScene(changeScene = false)
    }
}

private fun Model.updateImageWidget(widget: SettingsWidget, new: SettingsWidget, creating: Boolean) {
    val changes = ChangeTracker()
    applyEffectsUpdate(widget = widget, new = new, changes = changes)
    if (changes.changed && !creating) {
        resetSelectedScene(changeScene = false)
    }
}

private fun Model.updateSnapshotWidget(widget: SettingsWidget, new: SettingsWidget, creating: Boolean) {
    val effects = ChangeTracker()
    applyEffectsUpdate(widget = widget, new = new, changes = effects)
    val settings = ChangeTracker()
    settings.apply({ widget.snapshot.showtime }, { widget.snapshot.showtime = it }, new.snapshot.showtime)
    if (creating) {
        return
    }
    if (effects.changed) {
        resetSelectedScene(changeScene = false)
    }
    if (settings.changed) {
        snapshotWidgetShowtimeChanged(widget = widget)
    }
}

private fun Model.updateChatWidget(widget: SettingsWidget, new: SettingsWidget, creating: Boolean) {
    val chat = widget.chat
    val newChat = new.chat
    val changes = ChangeTracker()
    changes.apply({ chat.fontSize }, { chat.fontSize = it }, newChat.fontSize)
    changes.apply({ chat.font }, { chat.font = it }, newChat.font)
    if (changes.apply({ chat.usernameColor }, { chat.usernameColor = it }, newChat.usernameColor)) {
        chat.usernameColorColor = chat.usernameColor.color()
    }
    if (changes.apply({ chat.messageColor }, { chat.messageColor = it }, newChat.messageColor)) {
        chat.messageColorColor = chat.messageColor.color()
    }
    if (changes.apply({ chat.backgroundColor }, { chat.backgroundColor = it }, newChat.backgroundColor)) {
        chat.backgroundColorColor = chat.backgroundColor.color()
    }
    changes.apply({ chat.backgroundColorEnabled }, { chat.backgroundColorEnabled = it }, newChat.backgroundColorEnabled)
    if (changes.apply({ chat.shadowColor }, { chat.shadowColor = it }, newChat.shadowColor)) {
        chat.shadowColorColor = chat.shadowColor.color()
    }
    changes.apply({ chat.shadowColorEnabled }, { chat.shadowColorEnabled = it }, newChat.shadowColorEnabled)
    changes.apply({ chat.boldUsername }, { chat.boldUsername = it }, newChat.boldUsername)
    changes.apply({ chat.boldMessage }, { chat.boldMessage = it }, newChat.boldMessage)
    changes.apply({ chat.badges }, { chat.badges = it }, newChat.badges)
    changes.apply({ chat.displayStyle }, { chat.displayStyle = it }, newChat.displayStyle)
    changes.apply({ chat.sharedChatIcons }, { chat.sharedChatIcons = it }, newChat.sharedChatIcons)
    changes.apply({ chat.height }, { chat.height = it }, newChat.height)
    changes.apply(
        { chat.maximumNumberOfMessages },
        { chat.maximumNumberOfMessages = it },
        newChat.maximumNumberOfMessages,
    )
    if (changes.changed && !creating) {
        chatWidgetSettingsChanged(widget = widget)
    }
}

private fun Model.updateChatEmoteComboWidget(
    widget: SettingsWidget,
    new: SettingsWidget,
    creating: Boolean,
) {
    val chatEmoteCombo = widget.chatEmoteCombo
    val newChatEmoteCombo = new.chatEmoteCombo
    val changes = ChangeTracker()
    changes.apply({ chatEmoteCombo.minimumCombo }, { chatEmoteCombo.minimumCombo = it }, newChatEmoteCombo.minimumCombo)
    changes.apply({ chatEmoteCombo.resetAfter }, { chatEmoteCombo.resetAfter = it }, newChatEmoteCombo.resetAfter)
    if (changes.changed && !creating) {
        chatEmoteComboWidgetSettingsChanged(widget = widget)
    }
}

private fun Model.updateSlideshowWidget(widget: SettingsWidget, new: SettingsWidget, creating: Boolean) {
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
    if (changes.changed && !creating) {
        resetSelectedScene(changeScene = false, attachCamera = false)
    }
}

private fun Model.updateWheelOfLuckWidget(widget: SettingsWidget, new: SettingsWidget, creating: Boolean) {
    val wheelOfLuck = widget.wheelOfLuck
    val newWheelOfLuck = new.wheelOfLuck
    val changes = ChangeTracker()
    wheelOfLuck.advanced = newWheelOfLuck.advanced
    changes.apply(
        { wheelOfLuck.onlyShowWhenSpinning },
        { wheelOfLuck.onlyShowWhenSpinning = it },
        newWheelOfLuck.onlyShowWhenSpinning,
    )
    val options = newWheelOfLuck.options.map { option ->
        val oldOption = wheelOfLuck.options.firstOrNull { it.id == option.id }
        if (oldOption == null) {
            return@map option
        }
        changes.apply({ oldOption.text }, { oldOption.text = it }, option.text)
        changes.apply({ oldOption.weight }, { oldOption.weight = it }, option.weight)
        oldOption.function = option.function
        oldOption.functionData = option.functionData
        oldOption
    }
    changes.applyItems(wheelOfLuck.options, options) { it.id }
    wheelOfLuck.options = options.toMutableList()
    wheelOfLuck.updateText()
    wheelOfLuck.updateTotalWeight()
    if (changes.changed && !creating) {
        updateWheelOfLuckEffect(widget = widget)
    }
}

private fun Model.updateBingoCardWidget(widget: SettingsWidget, new: SettingsWidget, creating: Boolean) {
    val bingoCard = widget.bingoCard
    val newBingoCard = new.bingoCard
    val changes = ChangeTracker()
    if (changes.apply(
            { bingoCard.backgroundColor },
            { bingoCard.backgroundColor = it },
            newBingoCard.backgroundColor,
        )
    ) {
        bingoCard.backgroundColorColor = bingoCard.backgroundColor.color()
    }
    if (changes.apply(
            { bingoCard.foregroundColor },
            { bingoCard.foregroundColor = it },
            newBingoCard.foregroundColor,
        )
    ) {
        bingoCard.foregroundColorColor = bingoCard.foregroundColor.color()
    }
    changes.apply({ bingoCard.squares }, { bingoCard.squares = it }, newBingoCard.squares)
    changes.apply({ bingoCard.squaresText }, { bingoCard.squaresText = it }, newBingoCard.squaresText)
    if (changes.changed && !creating) {
        bingoCardWidgetSettingsChanged(widget = widget)
    }
}

private fun Model.updateVideoSourceWidget(widget: SettingsWidget, new: SettingsWidget, creating: Boolean) {
    val videoSource = widget.videoSource
    val newVideoSource = new.videoSource
    val effects = ChangeTracker()
    applyEffectsUpdate(widget = widget, new = new, changes = effects)
    val cameraChanged = cameraId(videoSource.videoSource) != cameraId(newVideoSource.videoSource)
    val settingsCameraId = newVideoSource.videoSource.toCameraId()
    if (cameraChanged) {
        videoSource.updateCameraId(settingsCameraId = settingsCameraId)
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
    if (creating) {
        return
    }
    if (effects.changed) {
        resetSelectedScene(changeScene = false)
    } else if (cameraChanged) {
        videoSourceWidgetSetCamera(widget = widget, settingsCameraId = settingsCameraId)
    }
    if (settings.changed) {
        videoSourceWidgetSettingsChanged(widget = widget)
    }
}

private fun Model.updateVTuberWidget(widget: SettingsWidget, new: SettingsWidget, creating: Boolean) {
    val vTuber = widget.vTuber
    val newVTuber = new.vTuber
    val cameraChanged = cameraId(vTuber.videoSource) != cameraId(newVTuber.videoSource)
    val settingsCameraId = newVTuber.videoSource.toCameraId()
    if (cameraChanged) {
        vTuber.updateCameraId(settingsCameraId = settingsCameraId)
    }
    val settings = ChangeTracker()
    settings.apply({ vTuber.cameraPositionY }, { vTuber.cameraPositionY = it }, newVTuber.cameraPositionY)
    settings.apply({ vTuber.cameraFieldOfView }, { vTuber.cameraFieldOfView = it }, newVTuber.cameraFieldOfView)
    settings.apply({ vTuber.mirror }, { vTuber.mirror = it }, newVTuber.mirror)
    settings.apply({ vTuber.sensitivity }, { vTuber.sensitivity = it }, newVTuber.sensitivity)
    settings.apply({ vTuber.armsAngle }, { vTuber.armsAngle = it }, newVTuber.armsAngle)
    if (creating) {
        return
    }
    if (cameraChanged) {
        vTuberWidgetSetCamera(widget = widget, settingsCameraId = settingsCameraId)
    }
    if (settings.changed) {
        vTuberWidgetSettingsChanged(widget = widget)
    }
}

private fun Model.updatePngTuberWidget(widget: SettingsWidget, new: SettingsWidget, creating: Boolean) {
    val pngTuber = widget.pngTuber
    val newPngTuber = new.pngTuber
    val cameraChanged = cameraId(pngTuber.videoSource) != cameraId(newPngTuber.videoSource)
    val settingsCameraId = newPngTuber.videoSource.toCameraId()
    if (cameraChanged) {
        pngTuber.updateCameraId(settingsCameraId = settingsCameraId)
    }
    val settings = ChangeTracker()
    settings.apply({ pngTuber.mirror }, { pngTuber.mirror = it }, newPngTuber.mirror)
    settings.apply({ pngTuber.sensitivity }, { pngTuber.sensitivity = it }, newPngTuber.sensitivity)
    if (creating) {
        return
    }
    if (cameraChanged) {
        pngTuberWidgetSetCamera(widget = widget, settingsCameraId = settingsCameraId)
    }
    if (settings.changed) {
        pngTuberWidgetSettingsChanged(widget = widget)
    }
}

private fun Model.updatePomodoroTimerWidget(
    widget: SettingsWidget,
    new: SettingsWidget,
    creating: Boolean,
) {
    val pomodoroTimer = widget.pomodoroTimer
    val newPomodoroTimer = new.pomodoroTimer
    val changes = ChangeTracker()
    if (changes.apply(
            { pomodoroTimer.focusDuration },
            { pomodoroTimer.focusDuration = it },
            newPomodoroTimer.focusDuration,
        )
    ) {
        pomodoroTimer.focusDurationChanged()
    }
    if (changes.apply(
            { pomodoroTimer.breakDuration },
            { pomodoroTimer.breakDuration = it },
            newPomodoroTimer.breakDuration,
        )
    ) {
        pomodoroTimer.breakDurationChanged()
    }
    val widthChanged = changes.apply({ pomodoroTimer.width }, { pomodoroTimer.width = it }, newPomodoroTimer.width)
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
    if (widthChanged && !creating) {
        updateWidgetOutlines()
    }
}

private fun Model.updateScoreboardWidget(widget: SettingsWidget, new: SettingsWidget, creating: Boolean) {
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
        scoreboard.sportChanged()
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
        golf.numberOfHolesChanged()
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
    if (!creating) {
        scoreboardWidgetUpdated(widget = widget)
    }
}

private fun Model.updateAlertsWidget(widget: SettingsWidget, new: SettingsWidget, creating: Boolean) {
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
    if (!creating) {
        fixAlertMedias()
    }
}

fun Model.remoteControlApplySceneUpdate(scene: SettingsScene, new: SettingsScene, creating: Boolean) {
    scene.name = new.name
    applySceneEnabledUpdate(scene, new, creating)
    applySceneVideoSourceUpdate(scene, new, creating)
    applySceneQuickSwitchGroupUpdate(scene, new, creating)
    applySceneMicUpdate(scene, new, creating)
    applySceneBackgroundColorUpdate(scene, new, creating)
    applySceneWidgetsUpdate(scene, new)
}

private fun Model.applySceneEnabledUpdate(scene: SettingsScene, new: SettingsScene, creating: Boolean) {
    val changes = ChangeTracker()
    if (changes.apply({ scene.enabled }, { scene.enabled = it }, new.enabled) && !creating) {
        sceneEnabledChanged(scene = scene)
    }
}

private fun Model.applySceneVideoSourceUpdate(scene: SettingsScene, new: SettingsScene, creating: Boolean) {
    val cameraChanged = cameraId(scene.videoSource) != cameraId(new.videoSource)
    val settingsCameraId = new.videoSource.toCameraId()
    if (cameraChanged) {
        scene.updateCameraId(settingsCameraId = settingsCameraId)
    }
    val changes = ChangeTracker()
    changes.apply({ scene.mirror }, { scene.mirror = it }, new.mirror)
    changes.apply(
        { scene.overrideVideoStabilizationMode },
        { scene.overrideVideoStabilizationMode = it },
        new.overrideVideoStabilizationMode,
    )
    changes.apply(
        { scene.videoStabilizationMode },
        { scene.videoStabilizationMode = it },
        new.videoStabilizationMode,
    )
    changes.apply({ scene.fillFrame }, { scene.fillFrame = it }, new.fillFrame)
    val settingsChanged = changes.changed
    val rotationChanged = changes.apply(
        { scene.videoSourceRotation },
        { scene.videoSourceRotation = it },
        new.videoSourceRotation,
    )
    if (creating) {
        return
    }
    if (cameraChanged) {
        sceneSetCamera(scene = scene, settingsCameraId = settingsCameraId)
    } else if (settingsChanged) {
        sceneVideoSourceSettingsChanged()
    } else if (rotationChanged) {
        sceneVideoSourceRotationChanged()
    }
}

private fun Model.applySceneQuickSwitchGroupUpdate(
    scene: SettingsScene,
    new: SettingsScene,
    creating: Boolean,
) {
    val changes = ChangeTracker()
    if (changes.apply({ scene.quickSwitchGroup }, { scene.quickSwitchGroup = it }, new.quickSwitchGroup) && !creating) {
        sceneQuickSwitchGroupChanged(scene = scene)
    }
}

private fun Model.applySceneMicUpdate(scene: SettingsScene, new: SettingsScene, creating: Boolean) {
    val changes = ChangeTracker()
    val overrideMicChanged = changes.apply({ scene.overrideMic }, { scene.overrideMic = it }, new.overrideMic)
    val micIdChanged = changes.apply({ scene.micId }, { scene.micId = it }, new.micId)
    if (creating) {
        return
    }
    if (overrideMicChanged) {
        switchMicIfNeededAfterSceneSwitch()
    } else if (micIdChanged) {
        sceneMicChanged(scene = scene)
    }
}

private fun Model.applySceneBackgroundColorUpdate(
    scene: SettingsScene,
    new: SettingsScene,
    creating: Boolean,
) {
    val changes = ChangeTracker()
    if (changes.apply({ scene.backgroundColor }, { scene.backgroundColor = it }, new.backgroundColor)) {
        scene.backgroundColorColor = scene.backgroundColor.color()
        if (!creating) {
            sceneBackgroundColorChanged()
        }
    }
}

private fun Model.applySceneWidgetsUpdate(scene: SettingsScene, new: SettingsScene) {
    val oldWidgetIds = scene.widgets.map { it.id }
    val newWidgetIds = new.widgets.map { it.id }
    val removedOrMoved = oldWidgetIds.any { !newWidgetIds.contains(it) } ||
        oldWidgetIds.filter { newWidgetIds.contains(it) } != newWidgetIds.filter { oldWidgetIds.contains(it) }
    val added = newWidgetIds.any { !oldWidgetIds.contains(it) }
    val layoutsChanged = new.widgets.any { sceneWidget ->
        scene.widgets.firstOrNull { it.id == sceneWidget.id }?.let { it.layout != sceneWidget.layout } ?: false
    }
    val captureDeviceWidgetsChanged =
        captureDeviceWidgetIds(scene.widgets) != captureDeviceWidgetIds(new.widgets)
    scene.widgets = remoteControlMergeSceneWidgets(scene.widgets, new.widgets).toMutableList()
    if (removedOrMoved || added || layoutsChanged) {
        sceneUpdated(attachCamera = getSelectedScene() === scene && captureDeviceWidgetsChanged)
    }
    if (removedOrMoved) {
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

private fun Model.cameraId(videoSource: SettingsVideoSource): CameraId =
    getCameraId(settingsCameraId = videoSource.toCameraId())

private fun Model.applyEffectsUpdate(
    widget: SettingsWidget,
    new: SettingsWidget,
    changes: ChangeTracker,
) {
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
    for (effect in updatedEffects) {
        updateWidgetVideoEffect(widget, effect)
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
            removeBackgroundEffectSettingsChanged(widget = widget, effect = effect)
        SettingsVideoEffectType.shape ->
            shapeEffectSettingsChanged(widget = widget, effect = effect)
        SettingsVideoEffectType.dewarp360 ->
            dewarp360EffectSettingsChanged(widget = widget, effect = effect)
        SettingsVideoEffectType.anamorphicLens ->
            anamorphicLensEffectSettingsChanged(widget = widget, effect = effect)
        SettingsVideoEffectType.lut ->
            lutEffectSettingsChanged(widget = widget, effect = effect)
        SettingsVideoEffectType.opacity ->
            opacityEffectSettingsChanged(widget = widget, effect = effect)
        SettingsVideoEffectType.mask ->
            maskEffectSettingsChanged(widget = widget, effect = effect)
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

package com.moblin.android.various.model

import android.util.Log
import com.moblin.android.remotecontrol.RemoteControlAssistantStreamerState
import com.moblin.android.remotecontrol.RemoteControlMacro
import com.moblin.android.various.settings.MacroEvent
import com.moblin.android.various.settings.SettingsMacrosAction
import com.moblin.android.various.settings.SettingsMacrosActionFunction
import com.moblin.android.various.settings.SettingsMacrosMacro
import com.moblin.android.various.settings.SettingsMacrosMacroRepeatMode
import com.moblin.android.various.settings.SettingsQuickButtonType
import com.moblin.android.videoeffects.text.TextFormatPart
import com.moblin.android.videoeffects.text.loadTextFormat
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "Model"

fun Model.startMacro(macro: SettingsMacrosMacro) {
    if (macro.running) {
        return
    }
    macro.running = true
    macro.nextActionIndex = 0
    macro.repeatCurrentCount = 0
    macro.delayed = false
    macro.waitingForEventAction = null
    macro.eventQueue.clear()
    macro.stack = mutableListOf(macro)
    remoteControlMacrosStateChanged()
    executeNextAction(macro = macro)
}

fun Model.startMacro(id: UUID) {
    val macro = database.macros.macros.firstOrNull { it.id == id } ?: return
    startMacro(macro = macro)
}

fun Model.stopMacro(macro: SettingsMacrosMacro) {
    if (!macro.running) {
        return
    }
    macro.running = false
    macro.finished = false
    for (stackMacro in macro.stack) {
        stackMacro.delayTimer.stop()
        stackMacro.finishedTimer.stop()
        stackMacro.waitingForEventAction = null
    }
    macro.stack.clear()
    macro.eventQueue.clear()
    remoteControlMacrosStateChanged()
}

fun Model.stopAllMacros() {
    for (macro in database.macros.macros) {
        stopMacro(macro = macro)
    }
}

fun Model.stopMacro(id: UUID) {
    val macro = database.macros.macros.firstOrNull { it.id == id } ?: return
    stopMacro(macro = macro)
}

fun Model.toggleMacroStartStop(id: UUID) {
    val macro = database.macros.macros.firstOrNull { it.id == id } ?: return
    if (macro.running) {
        stopMacro(macro = macro)
    } else if (!macro.finished) {
        startMacro(macro = macro)
    }
}

fun Model.autoStartMacros() {
    for (macro in database.macros.macros) {
        if (macro.runAtAppStart) {
            startMacro(macro = macro)
        }
    }
}

fun Model.macrosEventOccurred(event: MacroEvent) {
    var anyRunning = false
    for (macro in database.macros.macros) {
        if (!macro.running) {
            continue
        }
        macro.eventQueue.add(event)
        if (macro.eventQueue.size > 100) {
            macro.eventQueue.removeAt(0)
        }
        anyRunning = true
    }
    if (!anyRunning) {
        return
    }
    CoroutineScope(Dispatchers.Main).launch {
        for (macro in database.macros.macros) {
            if (!macro.running) {
                continue
            }
            continueMacroIfQueuedEventMatches(macro = macro)
        }
    }
}

private fun Model.continueMacroIfQueuedEventMatches(macro: SettingsMacrosMacro) {
    val currentMacro = macro.stack.lastOrNull() ?: return
    val action = currentMacro.waitingForEventAction ?: return
    if (!takeQueuedEvent(macro = macro, action = action)) {
        return
    }
    currentMacro.waitingForEventAction = null
    executeNextAction(macro = macro)
}

private fun Model.takeQueuedEvent(macro: SettingsMacrosMacro,
                                  action: SettingsMacrosAction): Boolean
{
    while (macro.eventQueue.isNotEmpty()) {
        if (action.matches(event = macro.eventQueue.removeAt(0))) {
            return true
        }
    }
    return false
}

fun Model.getRemoteControlMacros(): List<RemoteControlMacro> {
    return database.macros.macros.map {
        RemoteControlMacro(id = it.id, name = it.name, running = it.running)
    }
}

fun Model.remoteControlMacrosStateChanged() {
    remoteControlStateChanged(
        state = RemoteControlAssistantStreamerState(macros = getRemoteControlMacros())
    )
}

fun Model.isMacrosWeatherNeeded(): Boolean {
    return macrosTextFormatActions().any { it.needsWeather }
}

fun Model.isMacrosGeographyNeeded(): Boolean {
    return macrosTextFormatActions().any { it.needsGeography }
}

fun Model.isMacrosGForceNeeded(): Boolean {
    return macrosTextFormatActions().any { it.needsGForce }
}

fun Model.macrosTextFormatChanged() {
    for (action in macrosTextFormatActions()) {
        val parts = loadTextFormat(format = macrosActionTextFormat(action = action))
        action.needsWeather = parts.isWeatherVariable()
        action.needsGeography = parts.isGeographyVariable()
        action.needsGForce = parts.isGForceVariable()
    }
    startWeatherManager()
    startGeographyManager()
    startGForceManager()
}

private fun Model.macrosTextFormatActions(): List<SettingsMacrosAction> {
    return database.macros.macros.flatMap { macro ->
        macro.actions.filter {
            it.function == SettingsMacrosActionFunction.SEND_CHAT_MESSAGE ||
                it.function == SettingsMacrosActionFunction.IF_CONDITION
        }
    }
}

private fun Model.macrosActionTextFormat(action: SettingsMacrosAction): String {
    return when (action.function) {
        SettingsMacrosActionFunction.IF_CONDITION -> "${action.ifValue} ${action.ifOtherValue}"
        else -> action.chatMessage
    }
}

fun Model.removeDeadMacrosSettings() {
    for (macro in database.macros.macros) {
        for (action in macro.actions) {
            val sceneIds = database.scenes.map { it.id }
            action.sceneIds = action.sceneIds.filter { id -> sceneIds.contains(id) }.toSet()
            val djiDeviceIds = database.djiDevices.devices.map { it.id }
            action.djiDevices = action.djiDevices.filter { id -> djiDeviceIds.contains(id) }.toSet()
        }
    }
}

private fun Model.processMacroEnded(macro: SettingsMacrosMacro,
                                    currentMacro: SettingsMacrosMacro)
{
    currentMacro.repeatCurrentCount += 1
    val shouldRepeat: Boolean = when (currentMacro.repeatMode) {
        SettingsMacrosMacroRepeatMode.FOREVER -> true
        SettingsMacrosMacroRepeatMode.COUNT ->
            currentMacro.repeatCurrentCount < currentMacro.repeatCount
        SettingsMacrosMacroRepeatMode.OFF -> false
    }
    if (shouldRepeat) {
        currentMacro.nextActionIndex = 0
        val timeout = if (currentMacro.delayed) 0.0 else 1.0
        currentMacro.delayed = false
        currentMacro.delayTimer.startSingleShot(timeout) {
            executeNextAction(macro = macro)
        }
        return
    }
    currentMacro.running = false
    currentMacro.finished = true
    currentMacro.finishedTimer.startSingleShot(2.0) {
        currentMacro.finished = false
    }
    macro.stack.removeAt(macro.stack.size - 1)
    if (macro.stack.isEmpty()) {
        remoteControlMacrosStateChanged()
    }
    executeNextAction(macro = macro)
}

private fun Model.executeNextAction(macro: SettingsMacrosMacro) {
    val currentMacro = macro.stack.lastOrNull() ?: return
    if (currentMacro.nextActionIndex >= currentMacro.actions.size) {
        processMacroEnded(macro = macro, currentMacro = currentMacro)
        return
    }
    val action = currentMacro.actions[currentMacro.nextActionIndex]
    currentMacro.nextActionIndex += 1
    val executeNext: Boolean = when (action.function) {
        SettingsMacrosActionFunction.SCENE ->
            executeScene(action = action)
        SettingsMacrosActionFunction.ENABLE_DISABLE_SCENES ->
            executeEnableDisableScenes(action = action)
        SettingsMacrosActionFunction.AUTO_SCENE_SWITCHER ->
            executeAutoSceneSwitcher(action = action)
        SettingsMacrosActionFunction.ZOOM ->
            executeZoom(action = action)
        SettingsMacrosActionFunction.GIMBAL_PRESET ->
            executeGimbalPreset(action = action)
        SettingsMacrosActionFunction.SEND_CHAT_MESSAGE ->
            executeSendChatMessage(action = action)
        SettingsMacrosActionFunction.DELAY ->
            executeDelay(currentMacro = currentMacro, action = action, macro = macro)
        SettingsMacrosActionFunction.WAIT_FOR_EVENT ->
            executeWaitForEvent(currentMacro = currentMacro, action = action, macro = macro)
        SettingsMacrosActionFunction.IF_CONDITION ->
            executeIfCondition(currentMacro = currentMacro, action = action)
        SettingsMacrosActionFunction.MACRO ->
            executeMacro(action = action, macro = macro)
        SettingsMacrosActionFunction.DJI_DEVICES ->
            executeDjiDevices(action = action)
        SettingsMacrosActionFunction.RECORD ->
            executeRecord(action = action)
        SettingsMacrosActionFunction.MUTE ->
            executeMute(action = action)
        SettingsMacrosActionFunction.TORCH ->
            executeTorch(action = action)
        SettingsMacrosActionFunction.SNAPSHOT ->
            executeSnapshot()
        SettingsMacrosActionFunction.FILTERS ->
            executeFilters(action = action)
        SettingsMacrosActionFunction.REACTION ->
            executeReaction(action = action)
        null -> true
    }
    if (executeNext) {
        executeNextAction(macro = macro)
    }
}

private fun Model.executeScene(action: SettingsMacrosAction): Boolean {
    action.sceneId?.let { sceneId ->
        selectScene(id = sceneId)
    }
    return true
}

private fun Model.executeEnableDisableScenes(action: SettingsMacrosAction): Boolean {
    for (scene in database.scenes) {
        scene.enabled = action.sceneIds.contains(scene.id)
    }
    sceneSelector.trigger.value += 1
    return true
}

private fun Model.executeAutoSceneSwitcher(action: SettingsMacrosAction): Boolean {
    setAutoSceneSwitcher(id = action.autoSceneSwitcherId)
    return true
}

private fun Model.executeZoom(action: SettingsMacrosAction): Boolean {
    setZoomX(x = action.zoomX, rate = database.zoom.speed)
    return true
}

private fun Model.executeGimbalPreset(action: SettingsMacrosAction): Boolean {
    action.gimbalPresetId?.let { gimbalPresetId ->
        moveToGimbalPreset(id = gimbalPresetId)
    }
    return true
}

private fun Model.executeSendChatMessage(action: SettingsMacrosAction): Boolean {
    sendChatMessage(message = formatPlainText(formatString = action.chatMessage))
    return true
}

private fun Model.executeDelay(currentMacro: SettingsMacrosMacro,
                               action: SettingsMacrosAction,
                               macro: SettingsMacrosMacro): Boolean
{
    for (stackMacro in macro.stack) {
        stackMacro.delayed = true
    }
    currentMacro.delayTimer.startSingleShot(action.delay) {
        executeNextAction(macro = macro)
    }
    return false
}

private fun Model.executeMacro(action: SettingsMacrosAction,
                               macro: SettingsMacrosMacro): Boolean
{
    val subMacro = database.macros.macros.firstOrNull { it.id == action.macroId } ?: return true
    if (macro.stack.any { it.id == subMacro.id }) {
        return true
    }
    macro.stack.add(subMacro.copy())
    return true
}

private fun Model.executeIfCondition(currentMacro: SettingsMacrosMacro,
                                     action: SettingsMacrosAction): Boolean
{
    val value = formatPlainText(formatString = action.ifValue)
    val otherValue = formatPlainText(formatString = action.ifOtherValue)
    if (!action.ifComparison.evaluate(value = value, otherValue = otherValue)) {
        currentMacro.nextActionIndex += action.ifRunCount
    }
    return true
}

private fun Model.executeWaitForEvent(currentMacro: SettingsMacrosMacro,
                                      action: SettingsMacrosAction,
                                      macro: SettingsMacrosMacro): Boolean
{
    if (takeQueuedEvent(macro = macro, action = action)) {
        return true
    }
    currentMacro.waitingForEventAction = action
    return false
}

private fun Model.executeFilters(action: SettingsMacrosAction): Boolean {
    for (filter in SettingsQuickButtonType.filters()) {
        val on = action.filters.contains(filter)
        when (filter) {
            SettingsQuickButtonType.pixellate -> setPixellateQuickButton(on = on)
            SettingsQuickButtonType.movie ->
                setFilterQuickButton(type = SettingsQuickButtonType.movie, on = on)
            SettingsQuickButtonType.grayScale ->
                setFilterQuickButton(type = SettingsQuickButtonType.grayScale, on = on)
            SettingsQuickButtonType.sepia ->
                setFilterQuickButton(type = SettingsQuickButtonType.sepia, on = on)
            SettingsQuickButtonType.triple ->
                setFilterQuickButton(type = SettingsQuickButtonType.triple, on = on)
            SettingsQuickButtonType.twin ->
                setFilterQuickButton(type = SettingsQuickButtonType.twin, on = on)
            SettingsQuickButtonType.fourThree ->
                setFilterQuickButton(type = SettingsQuickButtonType.fourThree, on = on)
            SettingsQuickButtonType.crt ->
                setFilterQuickButton(type = SettingsQuickButtonType.crt, on = on)
            SettingsQuickButtonType.pinch -> setPinchQuickButton(on = on)
            SettingsQuickButtonType.whirlpool -> setWhirlpoolQuickButton(on = on)
            SettingsQuickButtonType.poll -> setPollQuickButton(on = on)
            SettingsQuickButtonType.blurFaces -> setBlurFaces(on = on)
            SettingsQuickButtonType.privacy -> setPrivacy(on = on)
            SettingsQuickButtonType.beauty -> setBeautyQuickButton(on = on)
            SettingsQuickButtonType.moblinInMouth -> setMoblinInMouth(on = on)
            SettingsQuickButtonType.cameraMan -> setCameraManQuickButton(on = on)
            else -> Log.i(TAG, "macro: Filter button $filter not supported")
        }
    }
    return true
}

private fun Model.executeDjiDevices(action: SettingsMacrosAction): Boolean {
    reloadDjiDevices(enabledDeviceIds = action.djiDevices)
    return true
}

private fun Model.executeRecord(action: SettingsMacrosAction): Boolean {
    if (action.record) {
        startRecording()
    } else {
        stopRecording()
    }
    return true
}

private fun Model.executeMute(action: SettingsMacrosAction): Boolean {
    setMuted(value = action.mute)
    setQuickButton(type = SettingsQuickButtonType.mute, isOn = action.mute)
    return true
}

private fun Model.executeTorch(action: SettingsMacrosAction): Boolean {
    setTorch(on = action.torch)
    setQuickButton(type = SettingsQuickButtonType.torch, isOn = action.torch)
    return true
}

private fun Model.executeSnapshot(): Boolean {
    takeSnapshot()
    return true
}

private fun Model.executeReaction(action: SettingsMacrosAction): Boolean {
    triggerReaction(reaction = action.reaction)
    return true
}

private fun List<TextFormatPart>.isWeatherVariable(): Boolean {
    return any {
        it is TextFormatPart.Temperature ||
            it is TextFormatPart.FeelsLikeTemperature ||
            it is TextFormatPart.Wind ||
            it === TextFormatPart.Conditions
    }
}

private fun List<TextFormatPart>.isGeographyVariable(): Boolean {
    return any {
        it === TextFormatPart.Country ||
            it === TextFormatPart.CountryFlag ||
            it === TextFormatPart.State ||
            it === TextFormatPart.Area ||
            it === TextFormatPart.City ||
            it === TextFormatPart.Neighborhood
    }
}

private fun List<TextFormatPart>.isGForceVariable(): Boolean {
    return any { it is TextFormatPart.GForce }
}

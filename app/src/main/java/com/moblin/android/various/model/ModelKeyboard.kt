package com.moblin.android.various.model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val mainScope = CoroutineScope(Dispatchers.Main)

class KeyPress(val characters: String) {
    enum class Result {
        handled,
        ignored,
    }
}

fun Model.isKeyboardActive(): Boolean {
    if (showingPanel != ShowingPanel.none) {
        return false
    }
    if (showBrowser) {
        return false
    }
    if (showTwitchAuth) {
        return false
    }
    if (showModerationAuth) {
        return false
    }
    if (createStreamWizard.presenting) {
        return false
    }
    if (createStreamWizard.presentingSetup) {
        return false
    }
    if (createStreamWizard.showTwitchAuth) {
        return false
    }
    return true
}

fun Model.handleKeyPressCharacters(characters: String): Boolean {
    if (!isKeyboardActive()) {
        return false
    }
    val key = database.keyboard.keys.firstOrNull { it.key == characters } ?: return false
    mainScope.launch {
        handleControllerFunction(
            buttonId = "kb:${key.key}",
            function = key.function,
            functionData = key.functionData,
            pressed = false,
        )
    }
    return true
}

fun Model.handleKeyPress(press: KeyPress): KeyPress.Result {
    return if (handleKeyPressCharacters(press.characters)) KeyPress.Result.handled else KeyPress.Result.ignored
}

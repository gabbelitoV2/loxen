package com.moblin.android.various.model

import android.view.KeyEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val mainScope = CoroutineScope(Dispatchers.Main)

fun Model.handleKeyPresses(presses: Set<KeyEvent>, pressed: Boolean): Boolean {
    fun characters(event: KeyEvent): String {
        val codePoint = event.unicodeChar
        return if (codePoint == 0) {
            ""
        } else {
            String(Character.toChars(codePoint))
        }
    }
    var handled = false
    for (press in presses) {
        val pressCharacters = characters(press)
        if (pressCharacters.isEmpty()) {
            continue
        }
        if (press.isCtrlPressed || press.isMetaPressed) {
            continue
        }
        val keyboardKey = database.keyboard.keys.firstOrNull { it.key == pressCharacters } ?: continue
        mainScope.launch {
            handleControllerFunction(
                buttonId = "kb:${keyboardKey.key}",
                function = keyboardKey.function,
                functionData = keyboardKey.functionData,
                pressed = pressed,
            )
        }
        handled = true
    }
    return handled
}

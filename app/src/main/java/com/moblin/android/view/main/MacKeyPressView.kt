package com.moblin.android.view.main

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import com.moblin.android.various.model.Model
import com.moblin.android.LocalModel

class MacKeyPressUIView {
    var model: Model? = null
    var isFirstResponder: Boolean = false

    val canBecomeFirstResponder: Boolean
        get() = true

    val focusRequester: FocusRequester = FocusRequester()

    fun modifier(): Modifier = Modifier
        .focusRequester(focusRequester)
        .focusable()
        .onFocusChanged { state ->
            isFirstResponder = state.isFocused
        }

    fun pressesBegan(presses: List<KeyEvent>, event: KeyEvent? = null): Boolean {
        var handled = false
        for (press in presses) {
            if (press.type != KeyEventType.KeyDown) {
                continue
            }
            val characters = characters(press)
            if (characters.isNotEmpty()) {
                handled = TODO("handleKeyPressCharacters")
            }
        }
        return handled
    }

    fun claimFocus() {
        if (!isFirstResponder) {
            becomeFirstResponder()
        }
    }

    private fun becomeFirstResponder() {
        if (canBecomeFirstResponder) {
            focusRequester.requestFocus()
        }
    }

    private fun characters(press: KeyEvent): String {
        val codePoint = press.utf16CodePoint
        if (codePoint == 0) {
            return ""
        }
        return String(Character.toChars(codePoint))
    }
}

private fun makeUIView(model: Model): MacKeyPressUIView {
    val view = MacKeyPressUIView()
    view.model = model
    return view
}

private suspend fun updateUIView(uiView: MacKeyPressUIView, shouldClaimFocus: Boolean) {
    if (shouldClaimFocus) {
        uiView.claimFocus()
    }
}

@Composable
fun MacKeyPressView(
    model: Model = LocalModel.current,
    shouldClaimFocus: Boolean,
    modifier: Modifier = Modifier,
) {
    val view = remember { makeUIView(model) }
    view.model = model
    LaunchedEffect(shouldClaimFocus) {
        updateUIView(view, shouldClaimFocus)
    }
    Box(
        modifier = modifier
            .then(view.modifier())
            .onPreviewKeyEvent { event -> view.pressesBegan(listOf(event)) },
    )
}

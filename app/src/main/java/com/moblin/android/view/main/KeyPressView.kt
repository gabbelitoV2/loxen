package com.moblin.android.view.main

import android.app.Activity
import android.content.Context
import android.view.KeyEvent
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.moblin.android.AppDelegate
import com.moblin.android.platform.uikit.UIView
import com.moblin.android.various.MainTimer
import com.moblin.android.various.model.Model
import com.moblin.android.LocalModel
import com.moblin.android.various.model.handleKeyPresses

private fun currentFirstResponder(context: Context): View? = (context as? Activity)?.currentFocus

open class KeyPressUIView(context: Context = AppDelegate.context) : UIView(context) {
    open var model: Model? = null
    private val reclaimFocusTimer = MainTimer()

    init {
        isFocusable = true
        isFocusableInTouchMode = true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        claimFocus()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        claimFocus()
    }

    override fun clearFocus() {
        reclaimFocusTimer.startPeriodic(interval = 1.0) {
            claimFocus()
        }
        super.clearFocus()
    }

    private fun claimFocus() {
        if (!isAttachedToWindow) {
            reclaimFocusTimer.stop()
            return
        }
        if (hasFocus()) {
            reclaimFocusTimer.stop()
            return
        }
        val current = currentFirstResponder(context)
        if (current != null) {
            return
        }
        if (requestFocus()) {
            reclaimFocusTimer.stop()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (model?.handleKeyPresses(setOf(event), pressed = event.action == KeyEvent.ACTION_DOWN) != true) {
            return super.dispatchKeyEvent(event)
        }
        return true
    }
}

@Composable
fun KeyPressView(model: Model = LocalModel.current, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            KeyPressUIView(context).apply {
                backgroundColor = Color.Transparent
                this.model = model
            }
        },
        modifier = modifier,
        update = { view ->
            view.model = model
        },
    )
}

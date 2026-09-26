package com.moblin.android.platform.uikit

import android.app.Presentation
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Display
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.moblin.android.ExternalScreenContentView
import com.moblin.android.LocalModel
import com.moblin.android.MoblinApp
import com.moblin.android.various.model.Model
import java.lang.ref.WeakReference

class ExternalDisplayWindow internal constructor(internal val presentation: Presentation) {
    val displayId: Int = presentation.display.displayId

    var isHidden: Boolean
        get() = !presentation.isShowing
        set(value) {
            if (value) {
                resignKey()
                presentation.dismiss()
            } else {
                makeKeyAndVisible()
            }
        }

    fun makeKeyAndVisible() {
        if (presentation.isShowing) {
            return
        }
        try {
            presentation.show()
        } catch (e: WindowManager.InvalidDisplayException) {
            Log.w("ExternalDisplay", "Display $displayId is gone: ${e.message}")
        }
    }

    fun resignKey() = Unit
}

object ExternalDisplay {
    private const val TAG = "ExternalDisplay"
    private val handler = Handler(Looper.getMainLooper())
    private var activityRef: WeakReference<ComponentActivity>? = null
    private var displayManager: DisplayManager? = null
    private var connectedModel: Model? = null
    private val retry = Runnable { update() }

    internal var window: ExternalDisplayWindow? = null
        private set

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = update()

        override fun onDisplayRemoved(displayId: Int) = update()

        override fun onDisplayChanged(displayId: Int) = update()
    }

    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            if (activityRef?.get() === owner) {
                uninstall()
            }
        }
    }

    fun install(activity: ComponentActivity) {
        if (activityRef?.get() === activity) {
            return
        }
        uninstall()
        activityRef = WeakReference(activity)
        val manager = activity.getSystemService(DisplayManager::class.java)
        displayManager = manager
        manager.registerDisplayListener(displayListener, handler)
        activity.lifecycle.addObserver(lifecycleObserver)
        update()
    }

    internal fun uninstall() {
        handler.removeCallbacks(retry)
        displayManager?.unregisterDisplayListener(displayListener)
        displayManager = null
        activityRef?.get()?.lifecycle?.removeObserver(lifecycleObserver)
        activityRef = null
        disconnect()
    }

    private fun update() {
        val activity = activityRef?.get() ?: return
        val display = presentationDisplay()
        val current = window
        if (current != null && current.displayId == display?.displayId) {
            return
        }
        disconnect()
        if (display != null) {
            connect(activity, display)
        }
    }

    private fun presentationDisplay(): Display? {
        return displayManager?.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
            ?.firstOrNull { it.displayId != Display.DEFAULT_DISPLAY && it.isValid }
    }

    private fun connect(activity: ComponentActivity, display: Display) {
        handler.removeCallbacks(retry)
        val model = MoblinApp.globalModel
        if (model == null) {
            handler.postDelayed(retry, 300)
            return
        }
        Log.i(TAG, "Connected to display ${display.displayId} (${display.name})")
        val presentation = Presentation(activity, display)
        val content = ComposeView(presentation.context)
        content.setViewTreeLifecycleOwner(activity)
        content.setViewTreeViewModelStoreOwner(activity)
        content.setViewTreeSavedStateRegistryOwner(activity)
        content.setContent {
            CompositionLocalProvider(LocalModel provides model) {
                ExternalScreenContentView()
            }
        }
        presentation.setContentView(content)
        val window = ExternalDisplayWindow(presentation)
        this.window = window
        connectedModel = model
        model.externalMonitorConnected(windowScene = window)
    }

    private fun disconnect() {
        val window = window ?: return
        this.window = null
        Log.i(TAG, "Disconnected from display ${window.displayId}")
        window.presentation.dismiss()
        connectedModel?.externalMonitorDisconnected()
        connectedModel = null
    }
}

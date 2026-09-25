package com.moblin.android.platform.uikit

import android.app.Activity
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.view.Display
import android.view.Surface
import com.moblin.android.platform.host.SystemEvents
import java.lang.ref.WeakReference

object UIInterfaceOrientation {
    const val unknown = 0
    const val portrait = 1
    const val portraitUpsideDown = 2
    const val landscapeLeft = 4
    const val landscapeRight = 3
}

object UIUserInterfaceIdiom {
    const val unspecified = -1
    const val phone = 0
    const val pad = 1
}

val UIDevice.userInterfaceIdiom: Int
    get() {
        val size = Resources.getSystem().configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
        return if (size >= Configuration.SCREENLAYOUT_SIZE_LARGE) {
            UIUserInterfaceIdiom.pad
        } else {
            UIUserInterfaceIdiom.phone
        }
    }

class UIApplication private constructor() {
    val connectedScenes: Set<UIScene>
        get() {
            val activity = SystemEvents.activity ?: return emptySet()
            return setOf(scene(activity))
        }

    private var lastScene: WeakReference<UIWindowScene>? = null

    @Synchronized
    private fun scene(activity: Activity): UIWindowScene {
        val scene = lastScene?.get()
        if (scene != null && scene.activity === activity) {
            return scene
        }
        return UIWindowScene(activity).also { lastScene = WeakReference(it) }
    }

    companion object {
        val shared: UIApplication by lazy { UIApplication() }
    }
}

open class UIScene internal constructor(val activity: Activity)

class UIWindowScene internal constructor(activity: Activity) : UIScene(activity) {
    val windows: List<UIWindow> = listOf(UIWindow(activity))

    val screen: UIScreen
        get() = windows.first().screen

    val interfaceOrientation: Int
        get() {
            val rotation = displayRotation() ?: return UIInterfaceOrientation.unknown
            val angle = when (rotation) {
                Surface.ROTATION_90 -> 270
                Surface.ROTATION_180 -> 180
                Surface.ROTATION_270 -> 90
                else -> 0
            }
            return when ((angle + UIDevice.current.naturalOrientationOffsetCw) % 360) {
                90 -> UIInterfaceOrientation.landscapeLeft
                180 -> UIInterfaceOrientation.portraitUpsideDown
                270 -> UIInterfaceOrientation.landscapeRight
                else -> UIInterfaceOrientation.portrait
            }
        }

    private fun displayRotation(): Int? {
        return display()?.rotation
    }

    private fun display(): Display? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching { activity.display }.getOrNull()?.let { return it }
        }
        @Suppress("DEPRECATION")
        return runCatching { activity.windowManager.defaultDisplay }.getOrNull()
    }
}

class UIWindow internal constructor(val activity: Activity) {
    val rootViewController: UIViewController? = UIViewController(activity)

    val screen: UIScreen = UIScreen { activity.resources }

    val window: android.view.Window?
        get() = activity.window
}

open class UIViewController(val activity: Activity)

class UIScreen internal constructor(private val resources: () -> Resources) {
    val scale: Float
        get() = resources().displayMetrics.density

    companion object {
        val main: UIScreen by lazy { UIScreen { Resources.getSystem() } }
    }
}

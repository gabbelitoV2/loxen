package com.moblin.android.platform.host

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import com.moblin.android.platform.capture.Camera2Engine
import com.moblin.android.platform.core.NotificationCenter
import com.moblin.android.platform.uikit.UIDevice
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.handleAudioRouteChange

object SystemEvents {
    private const val TAG = "SystemEvents"
    private const val routeChangeNotification = "AVAudioSession.routeChangeNotification"
    private var installed = false
    private var observing = false
    private var model: Model? = null
    private var startedActivities = 0
    private var hasEnteredBackground = false

    val isInBackground: Boolean
        get() = hasEnteredBackground && startedActivities == 0

    private val lifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

        override fun onActivityStarted(activity: Activity) {
            startedActivities += 1
            if (startedActivities == 1 && hasEnteredBackground) {
                hasEnteredBackground = false
                applicationWillEnterForeground()
            }
        }

        override fun onActivityResumed(activity: Activity) {}

        override fun onActivityPaused(activity: Activity) {}

        override fun onActivityStopped(activity: Activity) {
            if (startedActivities > 0) {
                startedActivities -= 1
            }
            if (startedActivities == 0 && !activity.isChangingConfigurations) {
                hasEnteredBackground = true
                applicationDidEnterBackground()
            }
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

        override fun onActivityDestroyed(activity: Activity) {
            if (activity.isFinishing && !activity.isChangingConfigurations) {
                applicationWillTerminate()
            }
        }
    }

    fun install(application: Application) {
        if (installed) {
            return
        }
        installed = true
        application.registerActivityLifecycleCallbacks(lifecycleCallbacks)
    }

    fun start(model: Model) {
        this.model = model
        if (observing) {
            return
        }
        observing = true
        NotificationCenter.default.addObserver(this, UIDevice.orientationDidChangeNotification, null) {
            this.model?.handleOrientationDidChange(animated = false)
        }
        NotificationCenter.default.addObserver(this, routeChangeNotification, null) { notification ->
            this.model?.handleAudioRouteChange(notification = notification)
        }
        UIDevice.current.beginGeneratingDeviceOrientationNotifications()
    }

    private fun applicationDidEnterBackground() {
        Log.i(TAG, "Application did enter background")
        Camera2Engine.applicationDidEnterBackground()
        try {
            model?.handleApplicationDidEnterBackground()
        } catch (error: Throwable) {
            Log.e(TAG, "handleApplicationDidEnterBackground failed", error)
        }
        storeState()
    }

    private fun applicationWillTerminate() {
        Log.i(TAG, "Application will terminate")
        try {
            model?.handleApplicationWillTerminate()
        } catch (error: Throwable) {
            Log.e(TAG, "handleApplicationWillTerminate failed", error)
        }
    }

    private fun storeState() {
        val model = model ?: return
        try {
            model.storeSettings()
            model.replaysStorage.store()
        } catch (error: Throwable) {
            Log.e(TAG, "Storing settings failed", error)
        }
    }

    private fun applicationWillEnterForeground() {
        Log.i(TAG, "Application will enter foreground")
        Camera2Engine.applicationWillEnterForeground()
        try {
            model?.handleApplicationWillEnterForeground()
        } catch (error: Throwable) {
            Log.e(TAG, "handleApplicationWillEnterForeground failed", error)
        }
    }
}

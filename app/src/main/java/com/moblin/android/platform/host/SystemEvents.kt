package com.moblin.android.platform.host

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.Process
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
    private var resumedActivities = 0
    private var hasEnteredBackground = false
    private var hasTerminated = false
    internal var terminateProcess: () -> Unit = { Process.killProcess(Process.myPid()) }

    val isInBackground: Boolean
        get() = hasEnteredBackground

    private val lifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            hasTerminated = false
        }

        override fun onActivityStarted(activity: Activity) {
            startedActivities += 1
            if (startedActivities == 1 && hasEnteredBackground) {
                hasEnteredBackground = false
                applicationWillEnterForeground()
            }
        }

        override fun onActivityResumed(activity: Activity) {
            resumedActivities += 1
            if (hasEnteredBackground && startedActivities > 0 && !activity.isInPictureInPictureMode) {
                hasEnteredBackground = false
                applicationWillEnterForeground()
            }
            applicationDidChangeActive(active = true)
        }

        override fun onActivityPaused(activity: Activity) {
            if (resumedActivities > 0) {
                resumedActivities -= 1
            }
            applicationDidChangeActive(active = false)
            if (activity.isInPictureInPictureMode) {
                pictureInPictureModeChanged(isInPictureInPictureMode = true)
            }
        }

        override fun onActivityStopped(activity: Activity) {
            if (startedActivities > 0) {
                startedActivities -= 1
            }
            if (startedActivities == 0 && !activity.isChangingConfigurations && !hasEnteredBackground) {
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

    fun pictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        if (isInPictureInPictureMode && !hasEnteredBackground) {
            hasEnteredBackground = true
            applicationDidEnterBackground()
        } else if (!isInPictureInPictureMode && hasEnteredBackground && startedActivities > 0 && resumedActivities > 0) {
            hasEnteredBackground = false
            applicationWillEnterForeground()
        }
    }

    private fun applicationDidChangeActive(active: Boolean) {
        val model = model ?: return
        val name = if (active) {
            "UIApplication.didBecomeActiveNotification"
        } else {
            "UIApplication.willResignActiveNotification"
        }
        try {
            model.handleApplicationDidChangeActive(name)
        } catch (error: Throwable) {
            Log.e(TAG, "handleApplicationDidChangeActive failed", error)
        }
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

    fun applicationTaskRemoved(context: Context) {
        Log.i(TAG, "Task removed")
        applicationWillTerminate()
        StreamingService.stopAll(context)
        terminateProcess()
    }

    private fun applicationWillTerminate() {
        if (hasTerminated) {
            return
        }
        hasTerminated = true
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

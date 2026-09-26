package com.moblin.android.platform.uikit

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer
import com.moblin.android.MoblinApp
import java.lang.ref.WeakReference

object InterfaceOrientation {
    private var activity: WeakReference<ComponentActivity>? = null
    private var windowOrientation = Configuration.ORIENTATION_UNDEFINED

    private val configurationListener = Consumer<Configuration> { configuration ->
        windowChanged(configuration)
    }

    private val pictureInPictureListener = Consumer<PictureInPictureModeChangedInfo> { info ->
        if (!info.isInPictureInPictureMode) {
            windowChanged(info.newConfig)
        }
    }

    fun install(activity: ComponentActivity) {
        uninstall()
        this.activity = WeakReference(activity)
        windowOrientation = activity.resources.configuration.orientation
        activity.addOnConfigurationChangedListener(configurationListener)
        activity.addOnPictureInPictureModeChangedListener(pictureInPictureListener)
    }

    fun uninstall() {
        activity?.get()?.let {
            it.removeOnConfigurationChangedListener(configurationListener)
            it.removeOnPictureInPictureModeChangedListener(pictureInPictureListener)
        }
        activity = null
        windowOrientation = Configuration.ORIENTATION_UNDEFINED
    }

    fun lock(portrait: Boolean): Int {
        return if (portrait) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    fun isPortrait(requested: Boolean): Boolean {
        if (activity?.get()?.takeUnless { it.isDestroyed } == null) {
            return requested
        }
        return isPortrait(windowOrientation, requested)
    }

    internal fun isPortrait(orientation: Int, requested: Boolean): Boolean {
        return when (orientation) {
            Configuration.ORIENTATION_PORTRAIT -> true
            Configuration.ORIENTATION_LANDSCAPE -> false
            else -> requested
        }
    }

    private fun windowChanged(configuration: Configuration) {
        val window = activity?.get() ?: return
        if (window.isInPictureInPictureMode || configuration.orientation == windowOrientation) {
            return
        }
        windowOrientation = configuration.orientation
        MoblinApp.globalModel?.updateIsPortrait()
    }
}

package com.moblin.android.platform.uikit

import android.graphics.Point
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Display
import android.view.OrientationEventListener
import android.view.Surface
import com.moblin.android.AppDelegate
import com.moblin.android.platform.core.NotificationCenter

private const val TAG = "MoblinCamera"

object UIDeviceOrientation {
    const val unknown = 0
    const val portrait = 1
    const val portraitUpsideDown = 2
    const val landscapeLeft = 3
    const val landscapeRight = 4
    const val faceUp = 5
    const val faceDown = 6
}

class UIDevice private constructor() {
    @Volatile
    var orientation: Int = UIDeviceOrientation.unknown
        private set

    val isNaturalOrientationLandscape: Boolean by lazy { computeNaturalOrientationLandscape() }

    internal val naturalOrientationOffsetCw: Int
        get() = if (isNaturalOrientationLandscape) 270 else 0

    val isGeneratingDeviceOrientationNotifications: Boolean
        get() = generatingCount > 0

    private val mainHandler = Handler(Looper.getMainLooper())
    private var listener: OrientationEventListener? = null
    private var generatingCount = 0
    private var quadrant = -1

    fun beginGeneratingDeviceOrientationNotifications() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { beginGeneratingDeviceOrientationNotifications() }
            return
        }
        generatingCount += 1
        if (listener != null) {
            return
        }
        val newListener = try {
            object : OrientationEventListener(AppDelegate.context) {
                override fun onOrientationChanged(angle: Int) {
                    handleAngle(angle)
                }
            }
        } catch (error: Throwable) {
            Log.i(TAG, "Orientation listener not available: $error")
            return
        }
        listener = newListener
        displayAngle()?.let { angle ->
            quadrant = angle
            setOrientation(orientationFromNaturalAngle(angle))
        }
        if (newListener.canDetectOrientation()) {
            newListener.enable()
        } else {
            Log.i(TAG, "Orientation sensor not available")
        }
    }

    fun endGeneratingDeviceOrientationNotifications() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { endGeneratingDeviceOrientationNotifications() }
            return
        }
        if (generatingCount > 0) {
            generatingCount -= 1
        }
        if (generatingCount == 0) {
            listener?.disable()
            listener = null
        }
    }

    private fun handleAngle(angle: Int) {
        if (angle == OrientationEventListener.ORIENTATION_UNKNOWN) {
            return
        }
        val current = quadrant
        val next = if (current >= 0 && angularDistance(angle, current) <= 45 + 20) {
            current
        } else {
            ((angle + 45) / 90 % 4) * 90
        }
        if (next == current) {
            return
        }
        quadrant = next
        setOrientation(orientationFromNaturalAngle(next))
    }

    private fun angularDistance(first: Int, second: Int): Int {
        val difference = Math.floorMod(first - second, 360)
        return minOf(difference, 360 - difference)
    }

    private fun orientationFromNaturalAngle(angle: Int): Int {
        return when ((angle + naturalOrientationOffsetCw) % 360) {
            90 -> UIDeviceOrientation.landscapeRight
            180 -> UIDeviceOrientation.portraitUpsideDown
            270 -> UIDeviceOrientation.landscapeLeft
            else -> UIDeviceOrientation.portrait
        }
    }

    private fun setOrientation(value: Int) {
        if (orientation == value) {
            return
        }
        orientation = value
        Log.i(TAG, "UIDevice orientation=$value quadrant=$quadrant naturalLandscape=$isNaturalOrientationLandscape")
        NotificationCenter.default.post(orientationDidChangeNotification, this)
    }

    private fun defaultDisplay(): Display? {
        return try {
            val manager = AppDelegate.context.getSystemService(DisplayManager::class.java)
            manager?.getDisplay(Display.DEFAULT_DISPLAY)
        } catch (error: Throwable) {
            null
        }
    }

    private fun displayAngle(): Int? {
        val display = defaultDisplay() ?: return null
        return when (display.rotation) {
            Surface.ROTATION_90 -> 270
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 90
            else -> 0
        }
    }

    private fun computeNaturalOrientationLandscape(): Boolean {
        val display = defaultDisplay() ?: return false
        val size = Point()
        @Suppress("DEPRECATION")
        display.getRealSize(size)
        val rotated = display.rotation == Surface.ROTATION_90 || display.rotation == Surface.ROTATION_270
        val naturalWidth = if (rotated) size.y else size.x
        val naturalHeight = if (rotated) size.x else size.y
        return naturalWidth > naturalHeight
    }

    companion object {
        const val orientationDidChangeNotification = "UIDevice.orientationDidChangeNotification"

        val current: UIDevice by lazy { UIDevice() }
    }
}

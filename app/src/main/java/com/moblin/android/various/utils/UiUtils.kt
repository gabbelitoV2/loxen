package com.moblin.android.various.utils

import android.app.Activity
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Window
import com.moblin.android.AppDelegate

fun Bitmap.resize(height: Float): Bitmap {
    val targetWidth = (width * (height / this.height.toFloat())).toInt()
    return Bitmap.createScaledBitmap(this, targetWidth, height.toInt(), true)
}

enum class DeviceOrientation {
    UNKNOWN,
    PORTRAIT,
    PORTRAIT_UPSIDE_DOWN,
    LANDSCAPE_LEFT,
    LANDSCAPE_RIGHT,
    FACE_UP,
    FACE_DOWN,
}

fun getOrientation(): DeviceOrientation {
    when (com.moblin.android.platform.uikit.UIDevice.current.orientation) { com.moblin.android.platform.uikit.UIDeviceOrientation.portrait -> return DeviceOrientation.PORTRAIT; com.moblin.android.platform.uikit.UIDeviceOrientation.portraitUpsideDown -> return DeviceOrientation.PORTRAIT_UPSIDE_DOWN; com.moblin.android.platform.uikit.UIDeviceOrientation.landscapeLeft -> return DeviceOrientation.LANDSCAPE_LEFT; com.moblin.android.platform.uikit.UIDeviceOrientation.landscapeRight -> return DeviceOrientation.LANDSCAPE_RIGHT; com.moblin.android.platform.uikit.UIDeviceOrientation.faceUp -> return DeviceOrientation.FACE_UP; com.moblin.android.platform.uikit.UIDeviceOrientation.faceDown -> return DeviceOrientation.FACE_DOWN }
    val configuration = Resources.getSystem().configuration
    return when (configuration.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> DeviceOrientation.LANDSCAPE_RIGHT
        Configuration.ORIENTATION_PORTRAIT -> DeviceOrientation.PORTRAIT
        else -> DeviceOrientation.UNKNOWN
    }
}

object UIDevice {
    fun vibrate() {
        runCatching {
            AppDelegate.context.getSystemService(Vibrator::class.java)
                ?.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }
}

fun isPhone(): Boolean {
    val screenLayout = Resources.getSystem().configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
    return screenLayout <= Configuration.SCREENLAYOUT_SIZE_NORMAL
}

fun isPad(): Boolean {
    val screenLayout = Resources.getSystem().configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
    return screenLayout >= Configuration.SCREENLAYOUT_SIZE_LARGE
}

fun isMac(): Boolean = false

fun getWindow(): Window? = null

fun getRootViewController(): Activity? = null

fun screenScale(): Float {
    return if (isMac()) {
        2f
    } else {
        Resources.getSystem().displayMetrics.density
    }
}

interface Identifiable<ID> {
    val id: ID
}

fun <ID, T : Identifiable<ID>> makeOffsets(items: List<T>, id: ID): Int? {
    val index = items.indexOfFirst { it.id == id }
    return if (index >= 0) index else null
}

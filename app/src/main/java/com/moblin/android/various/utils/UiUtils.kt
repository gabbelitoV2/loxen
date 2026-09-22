package com.moblin.android.various.utils

import android.app.Activity
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.view.Window

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
    val configuration = Resources.getSystem().configuration
    return when (configuration.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> DeviceOrientation.LANDSCAPE_RIGHT
        Configuration.ORIENTATION_PORTRAIT -> DeviceOrientation.PORTRAIT
        else -> DeviceOrientation.UNKNOWN
    }
}

object UIDevice {
    fun vibrate() {
        TODO("no global Context available to obtain a Vibrator")
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

fun getWindow(): Window? = TODO("Android has no global UIWindow; needs the current Activity")

fun getRootViewController(): Activity? = TODO("Android has no global UIViewController; needs the current Activity")

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

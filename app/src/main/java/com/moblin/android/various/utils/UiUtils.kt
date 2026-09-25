package com.moblin.android.various.utils

import android.graphics.Bitmap
import com.moblin.android.platform.audiotoolbox.AudioServicesPlaySystemSound
import com.moblin.android.platform.audiotoolbox.kSystemSoundID_Vibrate
import com.moblin.android.platform.uikit.UIApplication
import com.moblin.android.platform.uikit.UIInterfaceOrientation
import com.moblin.android.platform.uikit.UIScreen
import com.moblin.android.platform.uikit.UIUserInterfaceIdiom
import com.moblin.android.platform.uikit.UIViewController
import com.moblin.android.platform.uikit.UIWindow
import com.moblin.android.platform.uikit.UIWindowScene
import com.moblin.android.platform.uikit.userInterfaceIdiom

fun Bitmap.resize(height: Float): Bitmap {
    return Bitmap.createScaledBitmap(this, (width * (height / this.height)).toInt(), height.toInt(), true)
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
    val interfaceOrientation = UIApplication.shared.connectedScenes
        .firstOrNull { it is UIWindowScene }
        ?.let { it as UIWindowScene }?.interfaceOrientation
    return when (interfaceOrientation) {
        UIInterfaceOrientation.landscapeLeft -> DeviceOrientation.LANDSCAPE_RIGHT
        UIInterfaceOrientation.landscapeRight -> DeviceOrientation.LANDSCAPE_LEFT
        else -> DeviceOrientation.UNKNOWN
    }
}

object UIDevice {
    fun vibrate() {
        AudioServicesPlaySystemSound(kSystemSoundID_Vibrate)
    }
}

fun isPhone(): Boolean {
    return com.moblin.android.platform.uikit.UIDevice.current.userInterfaceIdiom == UIUserInterfaceIdiom.phone
}

fun isPad(): Boolean {
    return com.moblin.android.platform.uikit.UIDevice.current.userInterfaceIdiom == UIUserInterfaceIdiom.pad
}

fun isMac(): Boolean {
    return false
}

fun getWindow(): UIWindow? {
    val scene = UIApplication.shared.connectedScenes.firstOrNull() as? UIWindowScene ?: return null
    return scene.windows.firstOrNull()
}

fun getRootViewController(): UIViewController? {
    return getWindow()?.rootViewController
}

fun screenScale(): Float {
    if (isMac()) {
        return 2.0f
    }
    return getWindow()?.screen?.scale ?: UIScreen.main.scale
}

interface Identifiable<ID> {
    val id: ID
}

fun <ID, T : Identifiable<ID>> makeOffsets(items: List<T>, id: ID): Int? {
    val index = items.indexOfFirst { it.id == id }
    if (index >= 0) {
        return index
    }
    return null
}

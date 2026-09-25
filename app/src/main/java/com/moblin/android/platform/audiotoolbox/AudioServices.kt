package com.moblin.android.platform.audiotoolbox

import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import com.moblin.android.AppDelegate

const val kSystemSoundID_Vibrate = 4095

fun AudioServicesPlaySystemSound(inSystemSoundID: Int) {
    if (inSystemSoundID != kSystemSoundID_Vibrate) {
        return
    }
    try {
        AppDelegate.context.getSystemService(Vibrator::class.java)
            ?.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE))
    } catch (error: Throwable) {
        Log.i("AudioServices", "Vibration failed: $error")
    }
}

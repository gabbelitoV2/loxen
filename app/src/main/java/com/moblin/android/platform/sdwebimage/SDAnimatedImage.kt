package com.moblin.android.platform.sdwebimage

import android.graphics.Bitmap
import android.util.Log
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "MoblinEffects"

private val loggedMessages = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

private fun notImplemented(member: String) {
    if (!loggedMessages.add(member)) {
        return
    }
    try {
        Log.i(TAG, "$member not implemented yet")
    } catch (_: Throwable) {
    }
}

class SDAnimatedImage private constructor(
    private val frames: List<Bitmap>,
    private val durations: List<Double>,
    val animatedImageLoopCount: Int,
) {
    val animatedImageFrameCount: Int
        get() = frames.size

    fun animatedImageFrame(at: Int): Bitmap? {
        return frames.getOrNull(at)
    }

    fun animatedImageDuration(at: Int): Double {
        return durations.getOrNull(at) ?: 0.0
    }

    companion object {
        operator fun invoke(data: ByteArray): SDAnimatedImage? {
            notImplemented("SDAnimatedImage(data:)")
            return null
        }
    }
}

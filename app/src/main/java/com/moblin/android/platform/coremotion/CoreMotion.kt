package com.moblin.android.platform.coremotion

import com.moblin.android.platform.coreimage.internal.EffectsLog
import kotlinx.coroutines.CoroutineScope

data class CMAcceleration(val x: Double, val y: Double, val z: Double)

class CMDeviceMotion internal constructor(val gravity: CMAcceleration, val userAcceleration: CMAcceleration)

class OperationQueue {
    var underlyingQueue: CoroutineScope? = null
    var maxConcurrentOperationCount: Int = defaultMaxConcurrentOperationCount

    companion object {
        const val defaultMaxConcurrentOperationCount = -1
    }
}

class CMMotionManager {
    var deviceMotionUpdateInterval: Double = 0.01

    @Volatile
    private var active = false

    val isDeviceMotionAvailable: Boolean
        get() {
            EffectsLog.notImplemented("CMMotionManager.isDeviceMotionAvailable")
            return false
        }

    val isDeviceMotionActive: Boolean
        get() = active

    val deviceMotion: CMDeviceMotion?
        get() = null

    @Suppress("UNUSED_PARAMETER")
    fun startDeviceMotionUpdates(to: OperationQueue, withHandler: (CMDeviceMotion?, Throwable?) -> Unit) {
        EffectsLog.notImplemented("CMMotionManager.startDeviceMotionUpdates")
        active = true
    }

    fun stopDeviceMotionUpdates() {
        active = false
    }
}

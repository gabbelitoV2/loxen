package com.moblin.android.various.managers

import com.moblin.android.platform.coremotion.CMAccelerometerData
import com.moblin.android.platform.coremotion.CMMotionManager
import com.moblin.android.platform.coremotion.OperationQueue
import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

@Serializable
data class GForce(
    var now: Double,
    var recentMax: Double,
    var max: Double,
)

class GForceManager(private val motionManager: CMMotionManager) {
    private var maximum = 0.0
    private var recentMax = 0.0
    private var recentMaxNow = 0.0
    private var recentMaxProgress = 0.0
    private var now = 0.0
    private var started = false

    fun start() {
        if (started) {
            return
        }
        started = true
        recentMax = 0.0
        recentMaxNow = 0.0
        recentMaxProgress = 0.0
        motionManager.accelerometerUpdateInterval = 0.1
        motionManager.startAccelerometerUpdates(to = OperationQueue.main) { data, error ->
            if (data == null || error != null) {
                return@startAccelerometerUpdates
            }
            handleAccelerometerUpdate(data = data)
        }
    }

    fun stop() {
        if (!started) {
            return
        }
        started = false
        motionManager.stopAccelerometerUpdates()
    }

    fun getLatest(): GForce? {
        return GForce(now = now, recentMax = recentMaxNow, max = maximum)
    }

    private fun handleAccelerometerUpdate(data: CMAccelerometerData) {
        val x = data.acceleration.x
        val y = data.acceleration.y
        val z = data.acceleration.z
        now = sqrt(x * x + y * y + z * z)
        if (now > maximum) {
            maximum = now
        }
        if (now > recentMaxNow) {
            recentMaxProgress = 0.0
            recentMax = now
        } else {
            recentMaxProgress += 0.01
            recentMaxProgress = min(recentMaxProgress, 1.0)
        }
        recentMaxNow = max((1 - easeIn(progress = recentMaxProgress)) * recentMax, now)
    }

    private fun easeIn(progress: Double): Double {
        return progress * progress * progress * progress
    }
}

package com.moblin.android.platform.coremotion

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.moblin.android.AppDelegate
import com.moblin.android.platform.core.PipelineThread
import com.moblin.android.platform.uikit.UIDevice
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val TAG = "MoblinMotion"
private const val STANDARD_GRAVITY = 9.81
private const val LOW_PASS_ALPHA = 0.8

data class CMAcceleration(val x: Double, val y: Double, val z: Double)

class CMDeviceMotion internal constructor(val gravity: CMAcceleration, val userAcceleration: CMAcceleration)

class OperationQueue {
    var underlyingQueue: CoroutineScope? = null
    var maxConcurrentOperationCount: Int = defaultMaxConcurrentOperationCount

    companion object {
        const val defaultMaxConcurrentOperationCount = -1
    }
}

private object MotionSensors {
    private var thread: HandlerThread? = null

    val sensorManager: SensorManager? by lazy {
        try {
            AppDelegate.context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        } catch (error: Throwable) {
            Log.i(TAG, "Sensor manager not available: $error")
            null
        }
    }

    val gravitySensor: Sensor?
        get() = sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)

    val accelerometer: Sensor?
        get() = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    val linearAccelerationSensor: Sensor?
        get() = sensorManager?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

    @Synchronized
    fun handler(): Handler {
        val current = thread ?: HandlerThread("MoblinMotion").also {
            it.isDaemon = true
            it.start()
            thread = it
        }
        return Handler(current.looper)
    }

    fun toDevice(values: DoubleArray, scale: Double): CMAcceleration {
        val naturalX = values[0] * scale
        val naturalY = values[1] * scale
        val naturalZ = values[2] * scale
        return if (isNaturalOrientationLandscape()) {
            CMAcceleration(naturalY, -naturalX, naturalZ)
        } else {
            CMAcceleration(naturalX, naturalY, naturalZ)
        }
    }

    private fun isNaturalOrientationLandscape(): Boolean {
        return try {
            UIDevice.current.isNaturalOrientationLandscape
        } catch (error: Throwable) {
            false
        }
    }
}

class CMMotionManager {
    var deviceMotionUpdateInterval: Double = 0.01

    @Volatile
    private var listener: MotionListener? = null

    @Volatile
    internal var latest: CMDeviceMotion? = null

    val isDeviceMotionAvailable: Boolean
        get() = MotionSensors.gravitySensor != null || MotionSensors.accelerometer != null

    val isDeviceMotionActive: Boolean
        get() = listener != null

    val deviceMotion: CMDeviceMotion?
        get() = latest

    internal fun isCurrent(candidate: MotionListener): Boolean {
        return listener === candidate
    }

    fun startDeviceMotionUpdates(to: OperationQueue, withHandler: (CMDeviceMotion?, Throwable?) -> Unit) {
        stopDeviceMotionUpdates()
        val sensorManager = MotionSensors.sensorManager
        val gravitySensor = MotionSensors.gravitySensor
        val primarySensor = gravitySensor ?: MotionSensors.accelerometer
        if (sensorManager == null || primarySensor == null) {
            Log.i(TAG, "Device motion not available")
            return
        }
        val linearSensor = if (gravitySensor != null) MotionSensors.linearAccelerationSensor else null
        val periodUs = (deviceMotionUpdateInterval.coerceIn(0.005, 1.0) * 1_000_000).toInt()
        val intervalNs = (deviceMotionUpdateInterval.coerceAtLeast(0.0) * 1_000_000_000).toLong()
        val newListener = MotionListener(WeakReference(this), sensorManager, to, withHandler, intervalNs)
        val handler = MotionSensors.handler()
        listener = newListener
        if (!sensorManager.registerListener(newListener, primarySensor, periodUs, handler)) {
            listener = null
            Log.i(TAG, "Failed to register the device motion listener")
            return
        }
        if (linearSensor != null) {
            sensorManager.registerListener(newListener, linearSensor, periodUs, handler)
        }
        Log.i(
            TAG,
            "Device motion started (${if (gravitySensor != null) "gravity" else "accelerometer"} sensor, " +
                "interval $deviceMotionUpdateInterval s, natural landscape " +
                "${UIDevice.current.isNaturalOrientationLandscape})"
        )
    }

    fun stopDeviceMotionUpdates() {
        val current = listener ?: return
        listener = null
        latest = null
        current.unregister()
        Log.i(TAG, "Device motion stopped")
    }
}

internal class MotionListener(
    private val manager: WeakReference<CMMotionManager>,
    private val sensorManager: SensorManager,
    private val queue: OperationQueue,
    private val handler: (CMDeviceMotion?, Throwable?) -> Unit,
    private val intervalNs: Long,
) : SensorEventListener {
    private val gravity = DoubleArray(3)
    private val userAcceleration = DoubleArray(3)
    private var hasGravity = false
    private var nextDeliveryNs = 0L

    @Volatile
    private var registered = true

    fun unregister() {
        if (!registered) {
            return
        }
        registered = false
        try {
            sensorManager.unregisterListener(this)
        } catch (error: Throwable) {
            Log.w(TAG, "Failed to stop device motion: $error")
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        val owner = manager.get()
        if (owner == null || !owner.isCurrent(this)) {
            if (owner == null && registered) {
                Log.i(TAG, "Device motion stopped (manager released)")
            }
            unregister()
            return
        }
        when (event.sensor.type) {
            Sensor.TYPE_GRAVITY -> {
                for (index in 0 until 3) {
                    gravity[index] = event.values[index].toDouble()
                }
                hasGravity = true
                deliver(owner, event.timestamp)
            }
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                for (index in 0 until 3) {
                    userAcceleration[index] = event.values[index].toDouble()
                }
            }
            Sensor.TYPE_ACCELEROMETER -> {
                for (index in 0 until 3) {
                    val value = event.values[index].toDouble()
                    gravity[index] = if (hasGravity) {
                        LOW_PASS_ALPHA * gravity[index] + (1 - LOW_PASS_ALPHA) * value
                    } else {
                        value
                    }
                    userAcceleration[index] = value - gravity[index]
                }
                hasGravity = true
                deliver(owner, event.timestamp)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    private fun deliver(owner: CMMotionManager, timestampNs: Long) {
        if (nextDeliveryNs != 0L && timestampNs < nextDeliveryNs - intervalNs / 4) {
            return
        }
        nextDeliveryNs = if (nextDeliveryNs == 0L || timestampNs - nextDeliveryNs > intervalNs) {
            timestampNs + intervalNs
        } else {
            nextDeliveryNs + intervalNs
        }
        val scale = -1.0 / STANDARD_GRAVITY
        val motion = CMDeviceMotion(
            gravity = MotionSensors.toDevice(gravity, scale),
            userAcceleration = MotionSensors.toDevice(userAcceleration, scale)
        )
        owner.latest = motion
        val scope = queue.underlyingQueue
        if (scope != null) {
            scope.launch {
                runHandler(motion)
            }
        } else {
            PipelineThread.post {
                runHandler(motion)
            }
        }
    }

    private fun runHandler(motion: CMDeviceMotion) {
        val owner = manager.get()
        if (owner == null || !owner.isCurrent(this)) {
            return
        }
        try {
            handler(motion, null)
        } catch (error: Throwable) {
            Log.w(TAG, "Device motion handler failed", error)
        }
    }
}

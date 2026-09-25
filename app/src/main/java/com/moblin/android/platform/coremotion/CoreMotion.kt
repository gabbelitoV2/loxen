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
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "MoblinMotion"
private const val STANDARD_GRAVITY = 9.81
private const val ACCELERATION_SCALE = -1.0 / STANDARD_GRAVITY
private const val LOW_PASS_ALPHA = 0.8
private const val ATTITUDE_WAIT_NS = 1_000_000_000L

data class CMAcceleration(val x: Double, val y: Double, val z: Double)

data class CMQuaternion(val x: Double, val y: Double, val z: Double, val w: Double)

class CMAttitude internal constructor(val quaternion: CMQuaternion) {
    val roll: Double
    val pitch: Double
    val yaw: Double

    init {
        val x = quaternion.x
        val y = quaternion.y
        val z = quaternion.z
        val w = quaternion.w
        roll = atan2(2 * (w * y - x * z), 1 - 2 * (x * x + y * y))
        pitch = asin((2 * (w * x + y * z)).coerceIn(-1.0, 1.0))
        yaw = atan2(2 * (w * z - x * y), 1 - 2 * (x * x + z * z))
    }

    internal companion object {
        private val identity = CMQuaternion(0.0, 0.0, 0.0, 1.0)
        private val halfSqrt2 = sqrt(0.5)

        fun fromRotationVector(values: FloatArray, naturalLandscape: Boolean): CMAttitude {
            val x = values[0].toDouble()
            val y = values[1].toDouble()
            val z = values[2].toDouble()
            val w = if (values.size >= 4) {
                values[3].toDouble()
            } else {
                sqrt(max(0.0, 1 - x * x - y * y - z * z))
            }
            val natural = normalized(CMQuaternion(x, y, z, w))
            if (!naturalLandscape) {
                return CMAttitude(natural)
            }
            return CMAttitude(
                CMQuaternion(
                    x = (natural.x + natural.y) * halfSqrt2,
                    y = (natural.y - natural.x) * halfSqrt2,
                    z = (natural.z + natural.w) * halfSqrt2,
                    w = (natural.w - natural.z) * halfSqrt2,
                )
            )
        }

        fun fromGravity(gravity: CMAcceleration): CMAttitude {
            val length = sqrt(gravity.x * gravity.x + gravity.y * gravity.y + gravity.z * gravity.z)
            if (length < 1e-6) {
                return CMAttitude(identity)
            }
            val pitch = asin((-gravity.y / length).coerceIn(-1.0, 1.0))
            val roll = atan2(gravity.x / length, -gravity.z / length)
            val pitchCos = cos(pitch / 2)
            val pitchSin = sin(pitch / 2)
            val rollCos = cos(roll / 2)
            val rollSin = sin(roll / 2)
            return CMAttitude(
                CMQuaternion(
                    x = pitchSin * rollCos,
                    y = pitchCos * rollSin,
                    z = pitchSin * rollSin,
                    w = pitchCos * rollCos,
                )
            )
        }

        private fun normalized(quaternion: CMQuaternion): CMQuaternion {
            val length = sqrt(
                quaternion.x * quaternion.x + quaternion.y * quaternion.y +
                    quaternion.z * quaternion.z + quaternion.w * quaternion.w
            )
            if (length < 1e-9) {
                return identity
            }
            return CMQuaternion(
                quaternion.x / length,
                quaternion.y / length,
                quaternion.z / length,
                quaternion.w / length,
            )
        }
    }
}

class CMDeviceMotion internal constructor(
    val attitude: CMAttitude,
    val gravity: CMAcceleration,
    val userAcceleration: CMAcceleration,
)

class CMAccelerometerData internal constructor(val acceleration: CMAcceleration)

class OperationQueue {
    var underlyingQueue: CoroutineScope? = null
    var maxConcurrentOperationCount: Int = defaultMaxConcurrentOperationCount

    internal fun dispatch(block: () -> Unit) {
        val scope = underlyingQueue
        if (scope != null) {
            scope.launch {
                block()
            }
        } else {
            PipelineThread.post(block)
        }
    }

    companion object {
        const val defaultMaxConcurrentOperationCount = -1

        val main: OperationQueue by lazy {
            OperationQueue().apply {
                underlyingQueue = CoroutineScope(Dispatchers.Main + SupervisorJob())
            }
        }
    }
}

internal object MotionSensors {
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

    val attitudeSensor: Sensor?
        get() = sensorManager?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

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

    fun toDeviceAttitude(values: FloatArray): CMAttitude {
        return CMAttitude.fromRotationVector(values, isNaturalOrientationLandscape())
    }

    fun samplingPeriodUs(interval: Double): Int {
        return (interval.coerceIn(0.005, 1.0) * 1_000_000).toInt()
    }

    fun intervalNs(interval: Double): Long {
        return (interval.coerceAtLeast(0.0) * 1_000_000_000).toLong()
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
    var accelerometerUpdateInterval: Double = 0.01

    @Volatile
    private var listener: MotionListener? = null

    @Volatile
    private var accelerometerListener: AccelerometerListener? = null

    @Volatile
    internal var latest: CMDeviceMotion? = null

    @Volatile
    internal var latestAccelerometerData: CMAccelerometerData? = null

    val isDeviceMotionAvailable: Boolean
        get() = MotionSensors.gravitySensor != null || MotionSensors.accelerometer != null

    val isDeviceMotionActive: Boolean
        get() = listener != null

    val deviceMotion: CMDeviceMotion?
        get() = latest

    val isAccelerometerAvailable: Boolean
        get() = MotionSensors.accelerometer != null

    val isAccelerometerActive: Boolean
        get() = accelerometerListener != null

    val accelerometerData: CMAccelerometerData?
        get() = latestAccelerometerData

    internal fun isCurrent(candidate: MotionListener): Boolean {
        return listener === candidate
    }

    internal fun isCurrent(candidate: AccelerometerListener): Boolean {
        return accelerometerListener === candidate
    }

    fun startAccelerometerUpdates(to: OperationQueue, withHandler: (CMAccelerometerData?, Throwable?) -> Unit) {
        stopAccelerometerUpdates()
        val sensorManager = MotionSensors.sensorManager
        val accelerometer = MotionSensors.accelerometer
        if (sensorManager == null || accelerometer == null) {
            Log.i(TAG, "Accelerometer not available")
            return
        }
        val newListener = AccelerometerListener(
            WeakReference(this),
            sensorManager,
            to,
            withHandler,
            MotionSensors.intervalNs(accelerometerUpdateInterval),
        )
        accelerometerListener = newListener
        val periodUs = MotionSensors.samplingPeriodUs(accelerometerUpdateInterval)
        if (!sensorManager.registerListener(newListener, accelerometer, periodUs, MotionSensors.handler())) {
            accelerometerListener = null
            Log.i(TAG, "Failed to register the accelerometer listener")
            return
        }
        Log.i(
            TAG,
            "Accelerometer started (interval $accelerometerUpdateInterval s, natural landscape " +
                "${UIDevice.current.isNaturalOrientationLandscape})"
        )
    }

    fun stopAccelerometerUpdates() {
        val current = accelerometerListener ?: return
        accelerometerListener = null
        latestAccelerometerData = null
        current.unregister()
        Log.i(TAG, "Accelerometer stopped")
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
        val attitudeSensor = MotionSensors.attitudeSensor
        val periodUs = MotionSensors.samplingPeriodUs(deviceMotionUpdateInterval)
        val intervalNs = MotionSensors.intervalNs(deviceMotionUpdateInterval)
        val newListener = MotionListener(
            WeakReference(this),
            sensorManager,
            to,
            withHandler,
            intervalNs,
            attitudeSensor,
        )
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
        if (attitudeSensor != null &&
            !sensorManager.registerListener(newListener, attitudeSensor, periodUs, handler)
        ) {
            newListener.attitudeUnavailable()
        }
        val attitudeSource = when (attitudeSensor?.type) {
            Sensor.TYPE_GAME_ROTATION_VECTOR -> "game rotation vector"
            Sensor.TYPE_ROTATION_VECTOR -> "rotation vector"
            else -> "gravity"
        }
        Log.i(
            TAG,
            "Device motion started (${if (gravitySensor != null) "gravity" else "accelerometer"} sensor, " +
                "attitude from $attitudeSource, interval $deviceMotionUpdateInterval s, natural landscape " +
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
    intervalNs: Long,
    private val attitudeSensor: Sensor?,
) : SensorEventListener {
    private val gravity = DoubleArray(3)
    private val userAcceleration = DoubleArray(3)
    private var hasGravity = false
    private var attitude: CMAttitude? = null
    private var attitudeWaitStartNs = 0L
    private val pacer = UpdatePacer(intervalNs)

    @Volatile
    private var usesAttitudeSensor = attitudeSensor != null

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

    fun attitudeUnavailable() {
        if (!usesAttitudeSensor) {
            return
        }
        usesAttitudeSensor = false
        if (attitudeSensor != null && registered) {
            try {
                sensorManager.unregisterListener(this, attitudeSensor)
            } catch (error: Throwable) {
                Log.w(TAG, "Failed to stop the attitude sensor: $error")
            }
        }
        Log.i(TAG, "No attitude from the rotation vector sensor, deriving it from gravity")
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
            Sensor.TYPE_GAME_ROTATION_VECTOR, Sensor.TYPE_ROTATION_VECTOR -> {
                if (usesAttitudeSensor) {
                    attitude = MotionSensors.toDeviceAttitude(event.values)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    private fun deliver(owner: CMMotionManager, timestampNs: Long) {
        if (usesAttitudeSensor && attitude == null) {
            if (attitudeWaitStartNs == 0L) {
                attitudeWaitStartNs = timestampNs
            }
            if (timestampNs - attitudeWaitStartNs < ATTITUDE_WAIT_NS) {
                return
            }
            attitudeUnavailable()
        }
        if (!pacer.isDue(timestampNs)) {
            return
        }
        val deviceGravity = MotionSensors.toDevice(gravity, ACCELERATION_SCALE)
        val sensorAttitude = if (usesAttitudeSensor) attitude else null
        val motion = CMDeviceMotion(
            attitude = sensorAttitude ?: CMAttitude.fromGravity(deviceGravity),
            gravity = deviceGravity,
            userAcceleration = MotionSensors.toDevice(userAcceleration, ACCELERATION_SCALE)
        )
        owner.latest = motion
        queue.dispatch {
            runHandler(motion)
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

internal class AccelerometerListener(
    private val manager: WeakReference<CMMotionManager>,
    private val sensorManager: SensorManager,
    private val queue: OperationQueue,
    private val handler: (CMAccelerometerData?, Throwable?) -> Unit,
    intervalNs: Long,
) : SensorEventListener {
    private val acceleration = DoubleArray(3)
    private val pacer = UpdatePacer(intervalNs)

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
            Log.w(TAG, "Failed to stop the accelerometer: $error")
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        val owner = manager.get()
        if (owner == null || !owner.isCurrent(this)) {
            if (owner == null && registered) {
                Log.i(TAG, "Accelerometer stopped (manager released)")
            }
            unregister()
            return
        }
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER || !pacer.isDue(event.timestamp)) {
            return
        }
        for (index in 0 until 3) {
            acceleration[index] = event.values[index].toDouble()
        }
        val data = CMAccelerometerData(MotionSensors.toDevice(acceleration, ACCELERATION_SCALE))
        owner.latestAccelerometerData = data
        queue.dispatch {
            runHandler(data)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    private fun runHandler(data: CMAccelerometerData) {
        val owner = manager.get()
        if (owner == null || !owner.isCurrent(this)) {
            return
        }
        try {
            handler(data, null)
        } catch (error: Throwable) {
            Log.w(TAG, "Accelerometer handler failed", error)
        }
    }
}

internal class UpdatePacer(private val intervalNs: Long) {
    private var nextDeliveryNs = 0L

    fun isDue(timestampNs: Long): Boolean {
        if (nextDeliveryNs != 0L && timestampNs < nextDeliveryNs - intervalNs / 4) {
            return false
        }
        nextDeliveryNs = if (nextDeliveryNs == 0L || timestampNs - nextDeliveryNs > intervalNs) {
            timestampNs + intervalNs
        } else {
            nextDeliveryNs + intervalNs
        }
        return true
    }
}

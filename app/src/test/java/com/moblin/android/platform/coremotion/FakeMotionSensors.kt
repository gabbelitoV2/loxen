package com.moblin.android.platform.coremotion

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import com.moblin.android.platform.uikit.UIDevice
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.SensorEventBuilder
import org.robolectric.shadows.ShadowSensor
import org.robolectric.shadows.ShadowSensorManager

internal class FakeMotionSensors(
    gravity: Boolean = true,
    linearAcceleration: Boolean = true,
    gameRotationVector: Boolean = true,
    rotationVector: Boolean = false,
) {
    val sensorManager: SensorManager = MotionSensors.sensorManager!!
    val shadow: ShadowSensorManager = shadowOf(sensorManager)
    val gravitySensor: Sensor? = if (gravity) add(Sensor.TYPE_GRAVITY) else null
    val linearAccelerationSensor: Sensor? = if (linearAcceleration) add(Sensor.TYPE_LINEAR_ACCELERATION) else null
    val gameRotationVectorSensor: Sensor? = if (gameRotationVector) add(Sensor.TYPE_GAME_ROTATION_VECTOR) else null
    val rotationVectorSensor: Sensor? = if (rotationVector) add(Sensor.TYPE_ROTATION_VECTOR) else null
    var nowNs = 10_000_000_000L

    private fun add(type: Int): Sensor {
        val sensor = ShadowSensor.newInstance(type)
        shadow.addSensor(sensor)
        return sensor
    }

    fun motionListeners(): Set<MotionListener> {
        return shadow.listeners.filterIsInstance<MotionListener>().toSet()
    }

    fun isRegistered(listener: MotionListener, sensor: Sensor?): Boolean {
        return sensor != null && shadow.hasListener(listener, sensor)
    }

    fun sendAttitude(quaternion: CMQuaternion, sensor: Sensor? = gameRotationVectorSensor ?: rotationVectorSensor) {
        val natural = toNatural(quaternion)
        val values = floatArrayOf(natural.x.toFloat(), natural.y.toFloat(), natural.z.toFloat(), natural.w.toFloat())
        send(sensor!!, values)
    }

    fun sendGravity(quaternion: CMQuaternion) {
        val natural = toNatural(quaternion)
        val x = natural.x
        val y = natural.y
        val z = natural.z
        val w = natural.w
        val values = floatArrayOf(
            (STANDARD_GRAVITY * 2 * (x * z - w * y)).toFloat(),
            (STANDARD_GRAVITY * 2 * (y * z + w * x)).toFloat(),
            (STANDARD_GRAVITY * (1 - 2 * (x * x + y * y))).toFloat(),
        )
        send(gravitySensor!!, values)
    }

    fun sendPose(quaternion: CMQuaternion, attitude: Boolean = true) {
        if (attitude) {
            sendAttitude(quaternion)
        }
        sendGravity(quaternion)
        nowNs += 200_000_000L
    }

    fun send(sensor: Sensor, values: FloatArray) {
        shadow.sendSensorEventToListeners(event(sensor, values), sensor)
    }

    fun event(sensor: Sensor, values: FloatArray): SensorEvent {
        return SensorEventBuilder.newBuilder()
            .setSensor(sensor)
            .setValues(values)
            .setTimestamp(nowNs)
            .build()
    }

    private fun toNatural(quaternion: CMQuaternion): CMQuaternion {
        if (!UIDevice.current.isNaturalOrientationLandscape) {
            return quaternion
        }
        return multiply(quaternion, axisAngle(0.0, 0.0, 1.0, -90.0))
    }

    companion object {
        const val STANDARD_GRAVITY = 9.81

        fun pose(yaw: Double = 0.0, pitch: Double = 0.0, roll: Double = 0.0): CMQuaternion {
            return multiply(
                multiply(axisAngle(0.0, 0.0, 1.0, yaw), axisAngle(1.0, 0.0, 0.0, pitch)),
                axisAngle(0.0, 1.0, 0.0, roll),
            )
        }

        fun axisAngle(x: Double, y: Double, z: Double, degrees: Double): CMQuaternion {
            val length = sqrt(x * x + y * y + z * z)
            val half = Math.toRadians(degrees) / 2
            val scale = sin(half) / length
            return CMQuaternion(x * scale, y * scale, z * scale, cos(half))
        }

        fun multiply(a: CMQuaternion, b: CMQuaternion): CMQuaternion {
            return CMQuaternion(
                x = a.w * b.x + a.x * b.w + a.y * b.z - a.z * b.y,
                y = a.w * b.y - a.x * b.z + a.y * b.w + a.z * b.x,
                z = a.w * b.z + a.x * b.y - a.y * b.x + a.z * b.w,
                w = a.w * b.w - a.x * b.x - a.y * b.y - a.z * b.z,
            )
        }

        fun rotate(quaternion: CMQuaternion, x: Double, y: Double, z: Double): DoubleArray {
            val vector = CMQuaternion(x, y, z, 0.0)
            val conjugate = CMQuaternion(-quaternion.x, -quaternion.y, -quaternion.z, quaternion.w)
            val rotated = multiply(multiply(quaternion, vector), conjugate)
            return doubleArrayOf(rotated.x, rotated.y, rotated.z)
        }
    }
}

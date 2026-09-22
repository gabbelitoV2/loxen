package com.moblin.android.videoeffects

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.Image
import android.util.SizeF
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.various.utils.calcCameraAngle
import com.moblin.android.various.utils.getWindow
import com.moblin.android.various.utils.isMac
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private fun minimalBoundingRectWithAspect(width: Float, height: Float, angle: Float): SizeF {
    val cosAngle = cos(angle)
    val sinAngle = sin(angle)
    var corners = listOf(
        PointF(-width / 2, -height / 2),
        PointF(-width / 2, height / 2),
        PointF(width / 2, -height / 2),
        PointF(width / 2, height / 2),
    )
    corners = corners.map {
        PointF(it.x * cosAngle - it.y * sinAngle, it.x * sinAngle + it.y * cosAngle)
    }
    val xs = corners.map { it.x }
    val ys = corners.map { it.y }
    val boxWidth = xs.maxOrNull()!! - xs.minOrNull()!!
    val boxHeight = ys.maxOrNull()!! - ys.minOrNull()!!
    val aspect = width / height
    val scaleWidth = max(boxWidth, boxHeight * aspect)
    val scaleHeight = max(boxHeight, boxWidth / aspect)
    return SizeF(scaleWidth, scaleHeight)
}

class FixedHorizonEffect : VideoEffect() {
    private var targetAngle: Double? = null
    private var currentAngle = 0.0
    private var motionManager: SensorManager? = null
    private var sensorListener: SensorEventListener? = null
    private var started = false
    private val operationQueue = CoroutineScope(processorPipelineQueue.coroutineContext)

    protected fun finalize() {
        stop()
    }

    fun start(portrait: Boolean) {
        if (started || isMac()) {
            return
        }
        val sensorManager = getWindow()?.context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            ?: return
        val gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) ?: return
        started = true
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val gravity = event.values.copyOf()
                operationQueue.launch {
                    targetAngle = calcCameraAngle(gravity = TODO("CMAcceleration is iOS-only"), portrait = portrait).toDouble()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            }
        }
        sensorListener = listener
        motionManager = sensorManager
        sensorManager.registerListener(listener, gravitySensor, SensorManager.SENSOR_DELAY_UI)
    }

    fun stop() {
        if (!started) {
            return
        }
        started = false
        val sensorManager = motionManager
        val listener = sensorListener
        if (sensorManager != null && listener != null) {
            sensorManager.unregisterListener(listener)
        }
        motionManager = null
        sensorListener = null
    }

    private fun calcScale(size: SizeF): Double? {
        val targetAngle = this.targetAngle ?: return null
        val targetWeight: Double = if (abs(targetAngle) < 0.1) {
            2 * abs(targetAngle)
        } else {
            0.2
        }
        currentAngle = targetWeight * targetAngle + (1 - targetWeight) * currentAngle
        val boundingSize = minimalBoundingRectWithAspect(
            width = size.width,
            height = size.height,
            angle = currentAngle.toFloat(),
        )
        return (boundingSize.width / size.width).toDouble()
    }

    override fun execute(image: Image, info: VideoEffectInfo): Image {
        val targetSize = SizeF(image.width.toFloat(), image.height.toFloat())
        if (calcScale(targetSize) == null) {
            return image
        }
        return TODO("OpenGL ES port")
    }

    override fun executeMetalPetal(image: Image, info: VideoEffectInfo): Image {
        val targetSize = SizeF(image.width.toFloat(), image.height.toFloat())
        if (calcScale(targetSize) == null) {
            return image
        }
        return TODO("OpenGL ES port")
    }
}

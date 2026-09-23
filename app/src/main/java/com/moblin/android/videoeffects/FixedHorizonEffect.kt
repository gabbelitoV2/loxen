package com.moblin.android.videoeffects

import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.platform.coregraphics.CGAffineTransform
import com.moblin.android.platform.coregraphics.CGPoint
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.coremotion.CMMotionManager
import com.moblin.android.platform.coremotion.OperationQueue
import com.moblin.android.platform.metalpetal.MTILayer
import com.moblin.android.platform.metalpetal.MTIImage
import com.moblin.android.platform.metalpetal.MTIMultilayerCompositingFilter
import com.moblin.android.various.utils.calcCameraAngle
import com.moblin.android.various.utils.isMac
import java.lang.ref.WeakReference
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

private fun minimalBoundingRectWithAspect(width: Double, height: Double, angle: Double): CGSize {
    val cosAngle = cos(angle)
    val sinAngle = sin(angle)
    var corners = listOf(
        CGPoint(x = -width / 2, y = -height / 2),
        CGPoint(x = -width / 2, y = height / 2),
        CGPoint(x = width / 2, y = -height / 2),
        CGPoint(x = width / 2, y = height / 2),
    )
    corners = corners.map {
        CGPoint(x = it.x * cosAngle - it.y * sinAngle, y = it.x * sinAngle + it.y * cosAngle)
    }
    val xs = corners.map { it.x }
    val ys = corners.map { it.y }
    val boxWidth = xs.max() - xs.min()
    val boxHeight = ys.max() - ys.min()
    val aspect = width / height
    val scaleWidth = max(boxWidth, boxHeight * aspect)
    val scaleHeight = max(boxHeight, boxWidth / aspect)
    return CGSize(width = scaleWidth, height = scaleHeight)
}

class FixedHorizonEffect : VideoEffect() {
    private var targetAngle: Double? = null
    private var currentAngle = 0.0
    private var motionManager: CMMotionManager? = null
    private var started = false
    private val operationQueue = OperationQueue()

    init {
        operationQueue.underlyingQueue = processorPipelineQueue
    }

    fun start(portrait: Boolean) {
        if (started || isMac()) {
            return
        }
        started = true
        motionManager = CMMotionManager()
        motionManager?.deviceMotionUpdateInterval = 0.1
        val weakSelf = WeakReference(this)
        motionManager?.startDeviceMotionUpdates(to = operationQueue) { data, _ ->
            val self = weakSelf.get()
            if (self == null || data == null) {
                return@startDeviceMotionUpdates
            }
            self.targetAngle = calcCameraAngle(gravity = data.gravity, portrait = portrait)
        }
    }

    fun stop() {
        if (!started) {
            return
        }
        started = false
        motionManager?.stopDeviceMotionUpdates()
        motionManager = null
    }

    private fun calcScale(size: CGSize): Double? {
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
            angle = currentAngle,
        )
        return boundingSize.width / size.width
    }

    override fun execute(image: CIImage, info: VideoEffectInfo): CIImage {
        val scale = calcScale(image.extent.size) ?: return image
        return image
            .translated(x = -image.extent.width / 2, y = -image.extent.height / 2)
            .transformed(by = CGAffineTransform(rotationAngle = currentAngle))
            .scaled(x = scale, y = scale)
            .translated(x = image.extent.width / 2, y = image.extent.height / 2)
            .cropped(to = image.extent)
    }

    override fun executeMetalPetal(image: MTIImage, info: VideoEffectInfo): MTIImage {
        val size = image.extent.size
        val scale = calcScale(size) ?: return image
        val rotation = (-currentAngle).toFloat()
        val filter = MTIMultilayerCompositingFilter()
        filter.inputBackgroundImage = image
        filter.layers = listOf(
            MTILayer(
                content = image,
                position = CGPoint(x = size.width / 2, y = size.height / 2),
                size = CGSize(width = size.width * scale, height = size.height * scale),
                rotation = rotation,
            ),
        )
        return filter.outputImage ?: image
    }
}

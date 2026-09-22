package com.moblin.android.videoeffects

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.util.SizeF
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectDetectionsMode
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.various.settings.SettingsSceneWidget
import java.util.UUID
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.launch

data class VideoSourceEffectSettings(
    var rotation: Double = 0.0,
    var trackFaceEnabled: Boolean = false,
    var trackFaceZoom: Double = 2.2,
    var mirror: Boolean = false,
)

class PositionInterpolator {
    private var currentValue: Double? = null

    val current: Double?
        get() = currentValue

    private var delta = 0.0

    var target: Double? = null

    fun update(timeElapsed: Double): Double {
        val current = currentValue
        val target = this.target
        if (current != null && target != null) {
            delta = 0.8 * delta + 0.2 * (target - current)
            if (abs(delta) > 5) {
                currentValue = current + delta * 2 * timeElapsed
            }
        } else if (target != null) {
            currentValue = target
        } else {
            currentValue = 0.0
        }
        return currentValue!!
    }
}

class VideoSourceEffect : VideoEffect() {
    private var videoSourceId: UUID = UUID.randomUUID()
    private var sceneWidget: SettingsSceneWidget? = null
    private var settings: VideoSourceEffectSettings = VideoSourceEffectSettings()
    private val trackFaceLeft = PositionInterpolator()
    private val trackFaceRight = PositionInterpolator()
    private val trackFaceTop = PositionInterpolator()
    private val trackFaceBottom = PositionInterpolator()
    private var trackFacePresentationTimeStamp = 0.0

    override fun needsFaceDetections(presentationTimeStamp: Double): VideoEffectDetectionsMode {
        return if (settings.trackFaceEnabled) {
            VideoEffectDetectionsMode.Interval(videoSourceId, 0.5)
        } else {
            VideoEffectDetectionsMode.Off
        }
    }

    fun setVideoSourceId(videoSourceId: UUID) {
        processorPipelineQueue.launch {
            this@VideoSourceEffect.videoSourceId = videoSourceId
        }
    }

    fun setSceneWidget(sceneWidget: SettingsSceneWidget) {
        processorPipelineQueue.launch {
            this@VideoSourceEffect.sceneWidget = sceneWidget
        }
    }

    fun setSettings(settings: VideoSourceEffectSettings) {
        processorPipelineQueue.launch {
            this@VideoSourceEffect.settings = settings
        }
    }

    private fun shouldUseFace(
        boundingBox: RectF,
        biggestBoundingBox: RectF,
        videoSourceImageSize: SizeF,
    ): Boolean {
        return if (boundingBox.height() < videoSourceImageSize.height / 10) {
            false
        } else if (boundingBox.height() < biggestBoundingBox.height() / 2) {
            false
        } else {
            true
        }
    }

    private fun calcFaceCropRegion(
        videoSourceImageSize: SizeF,
        faceDetections: List<RectF>?,
        presentationTimeStamp: Double,
        zoom: Double,
    ): RectF {
        var left = videoSourceImageSize.width.toDouble()
        var right = 0.0
        var top = 0.0
        var bottom = videoSourceImageSize.height.toDouble()
        if (faceDetections != null) {
            val biggestBoundingBox = faceDetections.firstOrNull()
            if (biggestBoundingBox != null) {
                var anyFaceUsed = false
                for (faceDetection in faceDetections) {
                    val boundingBox = faceDetection
                    if (!shouldUseFace(boundingBox, biggestBoundingBox, videoSourceImageSize)) {
                        continue
                    }
                    left = min(left, boundingBox.left.toDouble())
                    right = max(right, boundingBox.right.toDouble())
                    top = max(top, boundingBox.top.toDouble())
                    bottom = min(bottom, boundingBox.bottom.toDouble())
                    anyFaceUsed = true
                }
                if (anyFaceUsed) {
                    trackFaceLeft.target = left
                    trackFaceRight.target = right
                    trackFaceTop.target = top
                    trackFaceBottom.target = bottom
                }
            }
        }
        if (trackFaceLeft.target == null) {
            trackFaceLeft.target = videoSourceImageSize.width.toDouble() * 0.33
            trackFaceRight.target = videoSourceImageSize.width.toDouble() * 0.67
            trackFaceTop.target = videoSourceImageSize.height.toDouble() * 0.67
            trackFaceBottom.target = videoSourceImageSize.height.toDouble() * 0.33
        }
        val timeElapsed = presentationTimeStamp - trackFacePresentationTimeStamp
        trackFacePresentationTimeStamp = presentationTimeStamp
        left = trackFaceLeft.update(timeElapsed)
        right = trackFaceRight.update(timeElapsed)
        top = trackFaceTop.update(timeElapsed)
        bottom = trackFaceBottom.update(timeElapsed)
        val width = (right - left) * zoom
        val height = (top - bottom) * zoom
        val centerX = (right + left) / 2
        val centerY = (top + bottom) / 2 * 1.05
        val side = max(width, height)
        val cropWidth = min(side, videoSourceImageSize.width.toDouble())
        val cropHeight = min(side, videoSourceImageSize.height.toDouble())
        val cropSquareSize = floor(min(cropWidth, cropHeight))
        var cropX = max(centerX - cropSquareSize / 2, 0.0)
        var cropY = max(videoSourceImageSize.height.toDouble() - centerY - cropSquareSize / 2, 0.0)
        cropX = min(cropX, videoSourceImageSize.width.toDouble() - cropSquareSize)
        cropY = min(cropY, videoSourceImageSize.height.toDouble() - cropSquareSize)
        return RectF(
            cropX.toFloat(),
            cropY.toFloat(),
            (cropX + cropSquareSize).toFloat(),
            (cropY + cropSquareSize).toFloat(),
        )
    }

    private fun cropFace(
        videoSourceImage: Bitmap,
        faceDetections: List<RectF>?,
        presentationTimeStamp: Double,
        zoom: Double,
    ): Bitmap {
        val cropRegion = calcFaceCropRegion(
            SizeF(videoSourceImage.width.toFloat(), videoSourceImage.height.toFloat()),
            faceDetections,
            presentationTimeStamp,
            zoom,
        )
        val cropY = videoSourceImage.height - cropRegion.bottom
        return Bitmap.createBitmap(
            videoSourceImage,
            cropRegion.left.toInt(),
            cropY.toInt(),
            cropRegion.width().toInt(),
            cropRegion.height().toInt(),
        )
    }

    private fun rotate(videoSourceImage: Bitmap, settings: VideoSourceEffectSettings): Bitmap {
        val degrees = when (settings.rotation) {
            90.0 -> 90f
            180.0 -> 180f
            270.0 -> 270f
            else -> return videoSourceImage
        }
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(
            videoSourceImage,
            0,
            0,
            videoSourceImage.width,
            videoSourceImage.height,
            matrix,
            true,
        )
    }

    fun execute(backgroundImage: Bitmap, info: VideoEffectInfo): Bitmap {
        TODO()
    }

    fun executeMetalPetal(backgroundImage: Bitmap, info: VideoEffectInfo): Bitmap {
        TODO()
    }
}

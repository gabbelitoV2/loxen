package com.moblin.android.videoeffects

import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectDetectionsMode
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.platform.coregraphics.CGAffineTransform
import com.moblin.android.platform.coregraphics.CGImagePropertyOrientation
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.metalpetal.MTIImage
import com.moblin.android.platform.vision.VNFaceObservation
import com.moblin.android.various.settings.SettingsSceneWidget
import java.util.UUID
import kotlin.math.abs
import kotlin.math.floor
import kotlinx.coroutines.launch
import com.moblin.android.various.stableBoundingBox

data class VideoSourceEffectSettings(
    var rotation: Double = 0.0,
    var trackFaceEnabled: Boolean = false,
    var trackFaceZoom: Double = 2.2,
    var mirror: Boolean = false,
)

open class PositionInterpolator {
    var current: Double? = null
        private set
    private var delta = 0.0
    open var target: Double? = null

    open fun update(timeElapsed: Double): Double {
        val current = this.current
        val target = this.target
        if (current != null && target != null) {
            delta = 0.8 * delta + 0.2 * (target - current)
            if (abs(delta) > 5) {
                this.current = current + delta * 2 * timeElapsed
            }
        } else if (target != null) {
            this.current = target
        } else {
            this.current = 0.0
        }
        return this.current!!
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

    override fun needsFaceDetections(interval: Double): VideoEffectDetectionsMode {
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
        boundingBox: CGRect,
        biggestBoundingBox: CGRect,
        videoSourceImageSize: CGSize,
    ): Boolean {
        return if (boundingBox.height < videoSourceImageSize.height / 10) {
            false
        } else if (boundingBox.height < biggestBoundingBox.height / 2) {
            false
        } else {
            true
        }
    }

    private fun calcFaceCropRegion(
        videoSourceImageSize: CGSize,
        faceDetections: List<VNFaceObservation>?,
        presentationTimeStamp: Double,
        zoom: Double,
    ): CGRect {
        var left = videoSourceImageSize.width
        var right = 0.0
        var top = 0.0
        var bottom = videoSourceImageSize.height
        val biggestBoundingBox = faceDetections?.firstOrNull()
            ?.stableBoundingBox(imageSize = videoSourceImageSize)
        if (faceDetections != null && biggestBoundingBox != null) {
            var anyFaceUsed = false
            for (faceDetection in faceDetections) {
                val boundingBox = faceDetection.stableBoundingBox(imageSize = videoSourceImageSize)
                    ?: continue
                if (!shouldUseFace(boundingBox, biggestBoundingBox, videoSourceImageSize)) {
                    continue
                }
                left = minOf(left, boundingBox.minX)
                right = maxOf(right, boundingBox.maxX)
                top = maxOf(top, boundingBox.maxY)
                bottom = minOf(bottom, boundingBox.minY)
                anyFaceUsed = true
            }
            if (anyFaceUsed) {
                trackFaceLeft.target = left
                trackFaceRight.target = right
                trackFaceTop.target = top
                trackFaceBottom.target = bottom
            }
        }
        if (trackFaceLeft.target == null) {
            trackFaceLeft.target = videoSourceImageSize.width * 0.33
            trackFaceRight.target = videoSourceImageSize.width * 0.67
            trackFaceTop.target = videoSourceImageSize.height * 0.67
            trackFaceBottom.target = videoSourceImageSize.height * 0.33
        }
        val timeElapsed = presentationTimeStamp - trackFacePresentationTimeStamp
        trackFacePresentationTimeStamp = presentationTimeStamp
        left = trackFaceLeft.update(timeElapsed = timeElapsed)
        right = trackFaceRight.update(timeElapsed = timeElapsed)
        top = trackFaceTop.update(timeElapsed = timeElapsed)
        bottom = trackFaceBottom.update(timeElapsed = timeElapsed)
        val width = (right - left) * zoom
        val height = (top - bottom) * zoom
        val centerX = (right + left) / 2
        val centerY = (top + bottom) / 2 * 1.05
        val side = maxOf(width, height)
        val cropWidth = minOf(side, videoSourceImageSize.width)
        val cropHeight = minOf(side, videoSourceImageSize.height)
        val cropSquareSize = floor(minOf(cropWidth, cropHeight))
        var cropX = maxOf(centerX - cropSquareSize / 2, 0.0)
        var cropY = maxOf(videoSourceImageSize.height - centerY - cropSquareSize / 2, 0.0)
        cropX = minOf(cropX, videoSourceImageSize.width - cropSquareSize)
        cropY = minOf(cropY, videoSourceImageSize.height - cropSquareSize)
        return CGRect(x = cropX, y = cropY, width = cropSquareSize, height = cropSquareSize)
    }

    private fun cropFace(
        videoSourceImage: CIImage,
        faceDetections: List<VNFaceObservation>?,
        presentationTimeStamp: Double,
        zoom: Double,
    ): CIImage {
        val cropRegion = calcFaceCropRegion(
            videoSourceImage.extent.size,
            faceDetections,
            presentationTimeStamp,
            zoom,
        )
        val cropY = videoSourceImage.extent.height - cropRegion.maxY
        return videoSourceImage
            .cropped(
                to = CGRect(
                    x = cropRegion.minX,
                    y = cropY,
                    width = cropRegion.width,
                    height = cropRegion.height,
                ),
            )
            .transformed(by = CGAffineTransform(translationX = -cropRegion.minX, y = -cropY))
    }

    private fun rotate(videoSourceImage: CIImage, settings: VideoSourceEffectSettings): CIImage {
        return when (settings.rotation) {
            90.0 -> videoSourceImage.oriented(CGImagePropertyOrientation.right)
            180.0 -> videoSourceImage.oriented(CGImagePropertyOrientation.down)
            270.0 -> videoSourceImage.oriented(CGImagePropertyOrientation.left)
            else -> videoSourceImage
        }
    }

    override fun execute(backgroundImage: CIImage, info: VideoEffectInfo): CIImage {
        val sceneWidget = this.sceneWidget ?: return backgroundImage
        var widgetImage = info.getCiImage(videoSourceId) ?: return backgroundImage
        if (settings.trackFaceEnabled) {
            widgetImage = cropFace(
                widgetImage,
                info.faceDetections(videoSourceId),
                info.presentationTimeStamp / 1_000_000.0,
                settings.trackFaceZoom,
            )
        }
        return applyEffectsResizeMirrorMove(
            rotate(widgetImage, settings),
            sceneWidget,
            settings.mirror,
            backgroundImage.extent,
            info,
        ).composited(over = backgroundImage)
    }

    override fun executeMetalPetal(backgroundImage: MTIImage, info: VideoEffectInfo): MTIImage {
        val sceneWidget = this.sceneWidget ?: return backgroundImage
        val widgetImage = info.getMetalPetalImage(videoSourceId) ?: return backgroundImage
        var shape = WidgetShape(contentRegion = widgetImage.extent)
        if (settings.trackFaceEnabled) {
            shape.contentRegion = calcFaceCropRegion(
                widgetImage.extent.size,
                info.faceDetections(videoSourceId),
                info.presentationTimeStamp / 1_000_000.0,
                settings.trackFaceZoom,
            )
        }
        shape.rotation = settings.rotation
        return applyEffectsResizeMirrorMoveMetalPetal(
            widgetImage,
            sceneWidget,
            settings.mirror,
            backgroundImage,
            info,
            shape,
        )
    }
}

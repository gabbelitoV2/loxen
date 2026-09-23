package com.moblin.android.videoeffects

import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectDetectionsMode
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.platform.metalpetal.MTIBulgeDistortionFilter
import com.moblin.android.platform.metalpetal.MTIHighPassSkinSmoothingFilter
import com.moblin.android.platform.metalpetal.MTIImage
import com.moblin.android.platform.simd.SIMD2
import com.moblin.android.platform.vision.VNFaceObservation
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.launch

class BeautyEffect(fps: Float) : VideoEffect() {
    private var smoothnessRadius: Float = 10.0f
    private var smoothnessStrength: Float = 0.65f
    private var shapePosition: Float = 0.5f
    private var shapeRadius: Float = 0.5f
    private var shapeStrength: Float = 0.5f
    private var shapeScaleFactor: Float = 1.0f
    private var lastFaceDetections: List<VNFaceObservation> = emptyList()
    private var framesPerFade: Float = 30f

    init {
        framesPerFade = 15f * (fps / 30f)
    }

    fun setSmoothnessSettings(radius: Float, strength: Float) {
        processorPipelineQueue.launch {
            smoothnessRadius = radius
            smoothnessStrength = strength
        }
    }

    fun setShapeSettings(position: Float, radius: Float, strength: Float) {
        processorPipelineQueue.launch {
            shapePosition = position
            shapeRadius = radius
            shapeStrength = strength
        }
    }

    override fun executeMetalPetal(image: MTIImage, info: VideoEffectInfo): MTIImage {
        val detections = info.sceneFaceDetections()
        updateLastFaceDetectionsBefore(info.isFirstAfterAttach)
        updateScaleFactors(detections, info.isFirstAfterAttach)
        var result = image
        if (smoothnessStrength > 0f) {
            result = addBeautySmoothnessMetalPetal(result) ?: result
        }
        if (shapeStrength > 0f) {
            result = addBeautyShapeMetalPetal(result, detections, info) ?: result
        }
        updateLastFaceDetectionsAfter(detections)
        return result
    }

    override fun isEnabled(): Boolean {
        return smoothnessStrength > 0f || shapeStrength > 0f
    }

    override fun isMetalPetal(): Boolean {
        return true
    }

    override fun needsFaceDetections(interval: Double): VideoEffectDetectionsMode {
        return if (shapeStrength > 0f) {
            VideoEffectDetectionsMode.Now(null)
        } else {
            VideoEffectDetectionsMode.Off
        }
    }

    private fun updateLastFaceDetectionsBefore(isFirstAfterAttach: Boolean) {
        if (isFirstAfterAttach) {
            lastFaceDetections = emptyList()
        }
    }

    private fun updateLastFaceDetectionsAfter(faceDetections: List<VNFaceObservation>?) {
        if (faceDetections != null && faceDetections.isNotEmpty()) {
            lastFaceDetections = faceDetections
        }
    }

    private fun increaseShapeScaleFactor() {
        shapeScaleFactor = min(shapeScaleFactor + (1.0f / framesPerFade), 1f)
    }

    private fun decreaseShapeScaleFactor() {
        shapeScaleFactor = max(shapeScaleFactor - (1.0f / framesPerFade), 0f)
    }

    private fun updateScaleFactors(detections: List<VNFaceObservation>?, isFirstAfterAttach: Boolean) {
        if (isFirstAfterAttach) {
            shapeScaleFactor = 1f
        } else {
            if (detections == null || detections.isEmpty()) {
                decreaseShapeScaleFactor()
            } else {
                increaseShapeScaleFactor()
            }
        }
    }

    private fun addBeautySmoothnessMetalPetal(image: MTIImage?): MTIImage? {
        val filter = MTIHighPassSkinSmoothingFilter()
        filter.amount = smoothnessStrength
        filter.radius = smoothnessRadius
        filter.inputImage = image
        return filter.outputImage
    }

    private fun addBeautyShapeMetalPetal(
        image: MTIImage?,
        detections: List<VNFaceObservation>?,
        info: VideoEffectInfo,
    ): MTIImage? {
        if (image == null || detections == null) {
            return image
        }
        var faceDetections: List<VNFaceObservation> = detections
        if (faceDetections.isEmpty()) {
            faceDetections = lastFaceDetections
        }
        var outputImage: MTIImage? = image
        for (detection in faceDetections) {
            val medianLine = detection.landmarks?.medianLine
            if (medianLine != null) {
                val points = medianLine.pointsInImage(imageSize = image.extent.size)
                val firstPoint = points.firstOrNull()
                val lastPoint = points.lastOrNull()
                if (firstPoint == null || lastPoint == null) {
                    continue
                }
                val maxY = firstPoint.y.toFloat()
                val minY = lastPoint.y.toFloat()
                val centerX = lastPoint.x.toFloat()
                val filter = MTIBulgeDistortionFilter()
                val y = image.size.height.toFloat() -
                    (minY + (maxY - minY) * ((shapePosition - 0.5f) * 0.5f))
                filter.inputImage = outputImage
                filter.center = SIMD2(centerX, y)
                filter.radius = (maxY - minY) * (0.7f + shapeRadius * 0.3f)
                filter.scale = shapeScaleMetalPetal()
                outputImage = filter.outputImage
            }
        }
        return outputImage
    }

    private fun shapeScaleMetalPetal(): Float {
        return -(shapeStrength * 0.075f) * shapeScaleFactor
    }
}

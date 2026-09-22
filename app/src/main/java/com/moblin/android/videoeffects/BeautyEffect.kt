package com.moblin.android.videoeffects

import android.media.Image
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectDetectionsMode
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

class BeautyEffect(fps: Float) : VideoEffect() {
    private var smoothnessRadius: Float = 10.0f
    private var smoothnessStrength: Float = 0.65f
    private var shapePosition: Float = 0.5f
    private var shapeRadius: Float = 0.5f
    private var shapeStrength: Float = 0.5f
    private var shapeScaleFactor: Float = 1.0f
    private var lastFaceDetections: List<Any> = emptyList()
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

    override fun executeMetalPetal(image: Image, info: VideoEffectInfo): Image {
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

    override fun needsFaceDetections(pts: Double): VideoEffectDetectionsMode {
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

    private fun updateLastFaceDetectionsAfter(faceDetections: List<Any>?) {
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

    private fun updateScaleFactors(detections: List<Any>?, isFirstAfterAttach: Boolean) {
        if (isFirstAfterAttach) {
            shapeScaleFactor = 1f
        } else {
            if (detections.isNullOrEmpty()) {
                decreaseShapeScaleFactor()
            } else {
                increaseShapeScaleFactor()
            }
        }
    }

    private fun addBeautySmoothnessMetalPetal(image: Image?): Image? {
        TODO("OpenGL ES port")
    }

    private fun addBeautyShapeMetalPetal(
        image: Image?,
        detections: List<Any>?,
        info: VideoEffectInfo,
    ): Image? {
        if (image == null || detections == null) {
            return image
        }
        var faceDetections = detections
        if (faceDetections.isEmpty()) {
            faceDetections = lastFaceDetections
        }
        var outputImage: Image? = image
        for (detection in faceDetections) {
            TODO("OpenGL ES port")
        }
        return outputImage
    }

    private fun shapeScaleMetalPetal(): Float {
        return -(shapeStrength * 0.075f) * shapeScaleFactor
    }
}

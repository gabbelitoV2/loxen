package com.moblin.android.media.haishinkit.media.video

import android.graphics.RectF
import com.moblin.android.platform.video.CVPixelBuffer as Image
import android.util.Size
import androidx.compose.ui.geometry.Rect
import com.moblin.android.various.settings.SettingsSceneWidget
import com.moblin.android.videoeffects.MetalPetalWidgetShape
import com.moblin.android.videoeffects.dewarp360.graphicsEpsilon
import java.util.UUID

data class VideoEffectInfo(
    val sceneVideoSourceId: UUID,
    val detectionJobs: List<DetectionJob>,
    val detections: Map<UUID, Detections>,
    val presentationTimeStamp: Long,
    val videoUnit: VideoUnit,
    val isFirstAfterAttach: Boolean,
) {
    fun sceneDetections(): Detections? {
        return detections[sceneVideoSourceId]
    }

    fun sceneFaceDetections(): List<Any>? {
        return null
    }

    fun faceDetections(videoSourceId: UUID): List<Any>? {
        return null
    }

    fun getCiImage(videoSourceId: UUID): com.moblin.android.platform.coreimage.CIImage? {
        val imageBuffer = detectionJobs
            .firstOrNull { it.videoSourceId == videoSourceId }
            ?.imageBuffer
            ?: return videoUnit.getCiImage(videoSourceId, presentationTimeStamp)
        return com.moblin.android.platform.coreimage.CIImage(cvPixelBuffer = imageBuffer)
    }

    fun getMetalPetalImage(videoSourceId: UUID): com.moblin.android.platform.metalpetal.MTIImage? {
        val imageBuffer = detectionJobs
            .firstOrNull { it.videoSourceId == videoSourceId }
            ?.imageBuffer
            ?: return videoUnit.getMetalPetalImage(videoSourceId, presentationTimeStamp)
        return com.moblin.android.platform.metalpetal.MTIImage(cvPixelBuffer = imageBuffer, alphaType = com.moblin.android.platform.metalpetal.MTIAlphaType.alphaIsOne)
    }
}

sealed class VideoEffectDetectionsMode {
    object Off : VideoEffectDetectionsMode()
    data class Now(val videoSourceId: UUID?) : VideoEffectDetectionsMode()
    data class Interval(val videoSourceId: UUID?, val interval: Double) : VideoEffectDetectionsMode()
}

open class VideoEffect {
    var effects: MutableList<VideoEffect> = mutableListOf()

    open fun needsFaceDetections(time: Double): VideoEffectDetectionsMode {
        return VideoEffectDetectionsMode.Off
    }

    open fun needsTextDetections(time: Double): VideoEffectDetectionsMode {
        return VideoEffectDetectionsMode.Off
    }

    open fun isEnabled(): Boolean {
        return true
    }

    open fun executeEarly(image: Image, info: VideoEffectInfo): Image {
        return image
    }

    open fun execute(image: Image, info: VideoEffectInfo): Image {
        return image
    }

    open fun executeMetalPetal(image: Image, info: VideoEffectInfo): Image {
        return image
    }

    open fun isMetalPetal(): Boolean {
        return false
    }

    open fun prepare(size: Size, info: VideoEffectInfo) {
    }

    open fun removed() {
    }

    open fun shouldRemove(): Boolean {
        return false
    }

    open fun applyEffectsResizeMirrorMove(
        image: Image,
        sceneWidget: SettingsSceneWidget,
        mirror: Boolean,
        backgroundImageExtent: RectF,
        info: VideoEffectInfo,
    ): Image {
        val backgroundSize = Size(
            backgroundImageExtent.width().toInt(),
            backgroundImageExtent.height().toInt(),
        )
        val resizedImage = applyEarlyEffects(image, info)
        return TODO("CIImage resizeMirror/move/cropped has no Android counterpart")
    }

    open fun applyEffectsResizeMirrorMoveMetalPetal(
        image: Image,
        sceneWidget: SettingsSceneWidget,
        mirror: Boolean,
        backgroundImage: Image,
        info: VideoEffectInfo,
        widgetShape: MetalPetalWidgetShape? = null,
    ): Image {
        val shape = widgetShape
            ?: MetalPetalWidgetShape(Rect(0.0f, 0.0f, image.width.toFloat(), image.height.toFloat()))
        val processed = applyEffectsMetalPetal(image, info)
        for (effect in effects) {
            effect.modifyMetalPetalWidgetShape(shape)
        }
        return TODO("MTIImage resizeMirrorMoveComposited has no Android counterpart")
    }

    open fun modifyMetalPetalWidgetShape(shape: MetalPetalWidgetShape) {
    }

    private fun applyEarlyEffects(image: Image, info: VideoEffectInfo): Image {
        var result = image
        for (effect in effects) {
            result = effect.executeEarly(result, info)
        }
        return result
    }

    private fun applyEffects(image: Image, info: VideoEffectInfo): Image {
        var result = image
        for (effect in effects) {
            result = effect.execute(result, info)
        }
        val extent = RectF(0f, 0f, result.width.toFloat(), result.height.toFloat())
        extent.inset(graphicsEpsilon.toFloat(), graphicsEpsilon.toFloat())
        return TODO("CIImage cropped(to:) has no Android counterpart")
    }

    private fun applyEffectsMetalPetal(image: Image, info: VideoEffectInfo): Image {
        var result = image
        for (effect in effects) {
            result = effect.executeMetalPetal(result, info)
        }
        return result
    }
}

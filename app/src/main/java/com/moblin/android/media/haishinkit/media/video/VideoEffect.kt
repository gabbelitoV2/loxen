package com.moblin.android.media.haishinkit.media.video

import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.metalpetal.MTIAlphaType
import com.moblin.android.platform.metalpetal.MTIImage
import com.moblin.android.platform.video.CVPixelBuffer
import com.moblin.android.platform.vision.VNFaceObservation
import com.moblin.android.various.settings.SettingsSceneWidget
import com.moblin.android.videoeffects.WidgetShape
import com.moblin.android.videoeffects.dewarp360.graphicsEpsilon
import java.util.UUID
import com.moblin.android.videoeffects.move
import com.moblin.android.videoeffects.resizeMirror
import com.moblin.android.videoeffects.resizeMirrorMoveComposited

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

    fun sceneFaceDetections(): List<VNFaceObservation>? {
        return detections[sceneVideoSourceId]?.face
    }

    fun faceDetections(videoSourceId: UUID): List<VNFaceObservation>? {
        return detections[videoSourceId]?.face
    }

    fun getCiImage(videoSourceId: UUID): CIImage? {
        val imageBuffer = detectionJobs
            .firstOrNull { it.videoSourceId == videoSourceId }
            ?.imageBuffer
            ?: return videoUnit.getCiImage(videoSourceId, presentationTimeStamp)
        return videoUnit.makeCiImage(imageBuffer)
    }

    fun getMetalPetalImage(videoSourceId: UUID): MTIImage? {
        val imageBuffer = detectionJobs
            .firstOrNull { it.videoSourceId == videoSourceId }
            ?.imageBuffer
            ?: return videoUnit.getMetalPetalImage(videoSourceId, presentationTimeStamp)
        return MTIImage(cvPixelBuffer = imageBuffer, alphaType = MTIAlphaType.alphaIsOne)
    }
}

sealed class VideoEffectDetectionsMode {
    object Off : VideoEffectDetectionsMode()

    data class Now(val videoSourceId: UUID?) : VideoEffectDetectionsMode()

    data class Interval(val videoSourceId: UUID?, val interval: Double) : VideoEffectDetectionsMode()
}

open class VideoEffect {
    var effects: MutableList<VideoEffect> = mutableListOf()

    open fun needsFaceDetections(interval: Double): VideoEffectDetectionsMode {
        return VideoEffectDetectionsMode.Off
    }

    open fun needsTextDetections(interval: Double): VideoEffectDetectionsMode {
        return VideoEffectDetectionsMode.Off
    }

    open fun isEnabled(): Boolean {
        return true
    }

    open fun executeEarly(image: CIImage, info: VideoEffectInfo): CIImage {
        return image
    }

    open fun execute(image: CIImage, info: VideoEffectInfo): CIImage {
        return image
    }

    open fun executeMetalPetal(image: MTIImage, info: VideoEffectInfo): MTIImage {
        return image
    }

    open fun isMetalPetal(): Boolean {
        return false
    }

    open fun prepare(size: CGSize, info: VideoEffectInfo) {}

    open fun removed() {}

    open fun shouldRemove(): Boolean {
        return false
    }

    open fun applyEffectsResizeMirrorMove(
        image: CIImage,
        sceneWidget: SettingsSceneWidget,
        mirror: Boolean,
        backgroundImageExtent: CGRect,
        info: VideoEffectInfo,
    ): CIImage {
        val resizedImage = applyEarlyEffects(image, info)
            .resizeMirror(sceneWidget.layout, backgroundImageExtent.size, mirror)
        return applyEffects(resizedImage, info)
            .move(sceneWidget.layout, backgroundImageExtent.size)
            .cropped(to = backgroundImageExtent)
    }

    open fun applyEffectsResizeMirrorMoveMetalPetal(
        image: MTIImage,
        sceneWidget: SettingsSceneWidget,
        mirror: Boolean,
        backgroundImage: MTIImage,
        info: VideoEffectInfo,
        widgetShape: WidgetShape? = null,
    ): MTIImage {
        val shape = widgetShape?.copy() ?: WidgetShape(contentRegion = image.extent)
        val processedImage = applyEffectsMetalPetal(image, info)
        for (effect in effects) {
            effect.modifyWidgetShape(shape)
        }
        return processedImage.resizeMirrorMoveComposited(sceneWidget.layout, mirror, backgroundImage, shape)
    }

    open fun modifyWidgetShape(shape: WidgetShape) {}

    private fun applyEarlyEffects(image: CIImage, info: VideoEffectInfo): CIImage {
        var result = image
        for (effect in effects) {
            result = effect.executeEarly(result, info)
        }
        return result
    }

    private fun applyEffects(image: CIImage, info: VideoEffectInfo): CIImage {
        var result = image
        for (effect in effects) {
            result = effect.execute(result, info)
        }
        return result.cropped(to = result.extent.insetBy(dx = graphicsEpsilon, dy = graphicsEpsilon))
    }

    private fun applyEffectsMetalPetal(image: MTIImage, info: VideoEffectInfo): MTIImage {
        var result = image
        for (effect in effects) {
            result = effect.executeMetalPetal(result, info)
        }
        return result
    }
}

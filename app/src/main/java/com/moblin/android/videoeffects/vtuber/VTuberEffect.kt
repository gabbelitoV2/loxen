package com.moblin.android.videoeffects.vtuber

import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectDetectionsMode
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.platform.core.structCopy
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.metalpetal.MTIImage
import com.moblin.android.various.settings.SettingsSceneWidget
import com.moblin.android.various.settings.SettingsSensitivity
import com.moblin.android.various.utils.TimeStampRebaser
import com.moblin.android.videoeffects.EffectImage
import com.moblin.android.videoeffects.WidgetShape
import com.moblin.android.videoeffects.move
import com.moblin.android.videoeffects.resizeMirror
import com.moblin.android.videoeffects.resizeMirrorMoveComposited
import kotlinx.coroutines.launch
import java.util.UUID
import com.moblin.android.various.calcFaceAngleSide
import com.moblin.android.various.isRightEyeOpen
import com.moblin.android.various.calcFaceAngle
import com.moblin.android.various.isLeftEyeOpen
import com.moblin.android.various.isMouthOpen

data class VTuberFace(
    var sideAngle: Double = 0.0,
    var rotationAngle: Double = 0.0,
    var mouthOpen: Double = 0.0,
    var leftEyeOpen: Double = 1.0,
    var rightEyeOpen: Double = 1.0,
)

open class VTuberEffect : VideoEffect() {
    private var videoSourceId: UUID = UUID.randomUUID()
    private var mirror: Boolean = false
    private var sensitivity = SettingsSensitivity()
    private var sceneWidget: SettingsSceneWidget? = null
    private var timeStampRebaser = TimeStampRebaser()
    private var previousPresentationTimeStamp = 0.0
    private var face = VTuberFace()
    private var latestSideAngle = 0.0
    private var latestRotationAngle = 0.0
    private var renderedImagePresentationTimeStamp = 0.0
    private var renderedImage: EffectImage? = null

    open fun setVideoSourceId(videoSourceId: UUID) {
        processorPipelineQueue.launch {
            this@VTuberEffect.videoSourceId = videoSourceId
        }
    }

    open fun setSettings(
        cameraFieldOfView: Double,
        cameraPositionY: Double,
        mirror: Boolean,
        sensitivity: SettingsSensitivity,
        armsAngle: Double,
    ) {
        processorPipelineQueue.launch {
            this@VTuberEffect.mirror = mirror
            this@VTuberEffect.sensitivity = sensitivity
            this@VTuberEffect.setModelSettings(
                cameraFieldOfView = cameraFieldOfView,
                cameraPositionY = cameraPositionY,
                armsAngle = armsAngle,
            )
        }
    }

    open fun setSceneWidget(sceneWidget: SettingsSceneWidget) {
        processorPipelineQueue.launch {
            this@VTuberEffect.sceneWidget = sceneWidget
        }
    }

    open fun setModelSettings(cameraFieldOfView: Double, cameraPositionY: Double, armsAngle: Double) {
    }

    open fun isModelLoaded(): Boolean {
        return false
    }

    open fun updateModel(face: VTuberFace, time: Double, timeDelta: Double) {
    }

    open fun renderModel(time: Double, size: CGSize): EffectImage? {
        return null
    }

    override fun execute(image: CIImage, info: VideoEffectInfo): CIImage {
        val renderedImage = update(size = image.extent.size, info = info)?.getCiImage()
        val sceneWidget = sceneWidget
        if (renderedImage == null || sceneWidget == null) {
            return image
        }
        return renderedImage
            .resizeMirror(sceneWidget.layout, image.extent.size, mirror)
            .move(sceneWidget.layout, image.extent.size)
            .cropped(to = image.extent)
            .composited(over = image)
    }

    override fun executeMetalPetal(image: MTIImage, info: VideoEffectInfo): MTIImage {
        val renderedImage = update(size = image.extent.size, info = info)?.getMetalPetalImage()
        val sceneWidget = sceneWidget
        if (renderedImage == null || sceneWidget == null) {
            return image
        }
        return renderedImage.resizeMirrorMoveComposited(
            layout = sceneWidget.layout,
            mirror = mirror,
            backgroundImage = image,
            shape = WidgetShape(contentRegion = renderedImage.extent),
        )
    }

    private fun update(size: CGSize, info: VideoEffectInfo): EffectImage? {
        val presentationTimeStamp = info.presentationTimeStamp / 1_000_000.0
        val time = timeStampRebaser.rebase(presentationTimeStamp) ?: return null
        if (!isModelLoaded()) {
            return null
        }
        val timeDelta = presentationTimeStamp - previousPresentationTimeStamp
        previousPresentationTimeStamp = presentationTimeStamp
        updateFace(size = size, info = info, timeDelta = timeDelta)
        updateModel(face = structCopy(face), time = time, timeDelta = timeDelta)
        if (presentationTimeStamp - renderedImagePresentationTimeStamp > 0.025) {
            val image = renderModel(time = time, size = size)
            if (image != null) {
                renderedImage = image
                renderedImagePresentationTimeStamp = presentationTimeStamp
            }
        }
        return renderedImage
    }

    private fun updateFace(size: CGSize, info: VideoEffectInfo, timeDelta: Double) {
        val detection = info.faceDetections(videoSourceId)?.firstOrNull()
        val rotationAngle = detection?.calcFaceAngle(imageSize = size)
        val sideAngle = detection?.calcFaceAngleSide()
        if (detection != null && rotationAngle != null && sideAngle != null) {
            face.mouthOpen = detection.isMouthOpen(
                rotationAngle = rotationAngle,
                sensitivity = sensitivity.mouth,
            )
            face.leftEyeOpen = detection.isLeftEyeOpen(
                rotationAngle = rotationAngle,
                sensitivity = sensitivity.eyes,
            )
            face.rightEyeOpen = detection.isRightEyeOpen(
                rotationAngle = rotationAngle,
                sensitivity = sensitivity.eyes,
            )
            latestSideAngle = sideAngle
            latestRotationAngle = rotationAngle
        }
        val newFactor = minOf(0.1 * (timeDelta / 0.033), 0.5)
        val oldFactor = 1 - newFactor
        face.sideAngle = oldFactor * face.sideAngle + newFactor * latestSideAngle
        face.rotationAngle = oldFactor * face.rotationAngle + newFactor * latestRotationAngle
    }

    override fun needsFaceDetections(interval: Double): VideoEffectDetectionsMode {
        return VideoEffectDetectionsMode.Interval(videoSourceId = videoSourceId, interval = 0.1)
    }
}

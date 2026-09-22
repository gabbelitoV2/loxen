package com.moblin.android.videoeffects.vtuber

import android.media.Image
import android.util.Size
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectDetectionsMode
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.various.settings.SettingsSceneWidget
import com.moblin.android.various.settings.SettingsSensitivity
import com.moblin.android.various.utils.TimeStampRebaser
import com.moblin.android.videoeffects.EffectImage
import java.util.UUID
import kotlinx.coroutines.launch

data class VTuberFace(
    var sideAngle: Double = 0.0,
    var rotationAngle: Double = 0.0,
    var mouthOpen: Double = 0.0,
    var leftEyeOpen: Double = 1.0,
    var rightEyeOpen: Double = 1.0
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

    fun setVideoSourceId(videoSourceId: UUID) {
        processorPipelineQueue.launch {
            this@VTuberEffect.videoSourceId = videoSourceId
        }
    }

    fun setSettings(cameraFieldOfView: Double,
                    cameraPositionY: Double,
                    mirror: Boolean,
                    sensitivity: SettingsSensitivity,
                    armsAngle: Double)
    {
        processorPipelineQueue.launch {
            this@VTuberEffect.mirror = mirror
            this@VTuberEffect.sensitivity = sensitivity
            setModelSettings(cameraFieldOfView = cameraFieldOfView,
                             cameraPositionY = cameraPositionY,
                             armsAngle = armsAngle)
        }
    }

    fun setSceneWidget(sceneWidget: SettingsSceneWidget) {
        processorPipelineQueue.launch {
            this@VTuberEffect.sceneWidget = sceneWidget
        }
    }

    open fun setModelSettings(cameraFieldOfView: Double, cameraPositionY: Double, armsAngle: Double) {}

    open fun isModelLoaded(): Boolean {
        return false
    }

    open fun updateModel(face: VTuberFace, time: Double, timeDelta: Double) {}

    open fun renderModel(time: Double, size: Size): EffectImage? {
        return null
    }

    override fun execute(image: Image, info: VideoEffectInfo): Image {
        val renderedImage = update(Size(image.width, image.height), info) ?: return image
        val sceneWidget = this.sceneWidget ?: return image
        return TODO("OpenGL ES port: Core Image resizeMirror/move/cropped/composited chain")
    }

    override fun executeMetalPetal(image: Image, info: VideoEffectInfo): Image {
        return TODO("MetalPetal has no Android counterpart")
    }

    private fun update(size: Size, info: VideoEffectInfo): EffectImage? {
        val presentationTimeStamp = info.presentationTimeStamp / 1_000_000.0
        val time = timeStampRebaser.rebase(presentationTimeStamp) ?: return null
        if (!isModelLoaded()) {
            return null
        }
        val timeDelta = presentationTimeStamp - previousPresentationTimeStamp
        previousPresentationTimeStamp = presentationTimeStamp
        updateFace(size = size, info = info, timeDelta = timeDelta)
        updateModel(face = face, time = time, timeDelta = timeDelta)
        if (presentationTimeStamp - renderedImagePresentationTimeStamp > 0.025) {
            val image = renderModel(time = time, size = size)
            if (image != null) {
                renderedImage = image
                renderedImagePresentationTimeStamp = presentationTimeStamp
            }
        }
        return renderedImage
    }

    private fun updateFace(size: Size, info: VideoEffectInfo, timeDelta: Double) {
        val detection = info.faceDetections(videoSourceId)?.firstOrNull()
        if (detection != null) {
            val rotationAngle: Double = TODO("Vision framework: VNFaceObservation.calcFaceAngle")
            val sideAngle: Double = TODO("Vision framework: VNFaceObservation.calcFaceAngleSide")
            face.mouthOpen = TODO("Vision framework: VNFaceObservation.isMouthOpen")
            face.leftEyeOpen = TODO("Vision framework: VNFaceObservation.isLeftEyeOpen")
            face.rightEyeOpen = TODO("Vision framework: VNFaceObservation.isRightEyeOpen")
            latestSideAngle = sideAngle
            latestRotationAngle = rotationAngle
        }
        val newFactor = minOf(0.1 * (timeDelta / 0.033), 0.5)
        val oldFactor = 1 - newFactor
        face.sideAngle = oldFactor * face.sideAngle + newFactor * latestSideAngle
        face.rotationAngle = oldFactor * face.rotationAngle + newFactor * latestRotationAngle
    }

    override fun needsFaceDetections(time: Double): VideoEffectDetectionsMode {
        return VideoEffectDetectionsMode.Interval(videoSourceId, 0.1)
    }
}

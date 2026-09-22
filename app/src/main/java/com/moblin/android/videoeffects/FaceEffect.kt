package com.moblin.android.videoeffects

import android.media.Image
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.Detections
import com.moblin.android.media.haishinkit.media.video.TextDetection
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectDetectionsMode
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import kotlinx.coroutines.launch

private fun makeFaceMask(ratio: Float): Any? = TODO("OpenGL ES port")

data class FaceEffectSettings(
    var blurFaces: Boolean = true,
    var blurText: Boolean = true,
    var blurBackground: Boolean = true,
    var showMouth: Boolean = true,
    var privacyMode: FaceEffectPrivacyMode = FaceEffectPrivacyMode.Blur(1.0f),
)

sealed class FaceEffectPrivacyMode {
    data class Blur(val strength: Float) : FaceEffectPrivacyMode()

    data class Pixellate(val strength: Float) : FaceEffectPrivacyMode()

    data class BackgroundImage(val image: Any?) : FaceEffectPrivacyMode()

    data class Icon(val image: Any?) : FaceEffectPrivacyMode()
}

class FaceEffect : VideoEffect() {
    private var settings = FaceEffectSettings()
    private val moblinImage: EffectImageCgImage? = null
    private var backgroundImage: EffectImageCiImage? = null
    private var iconImage: EffectImageCgImage? = null
    private var faceMasks: MutableMap<Float, Any?> = mutableMapOf()

    fun setSettings(settings: FaceEffectSettings) {
        val backgroundImage: EffectImageCiImage? = when (settings.privacyMode) {
            is FaceEffectPrivacyMode.BackgroundImage -> TODO("OpenGL ES port")
            else -> null
        }
        val iconImage: EffectImageCgImage? = when (settings.privacyMode) {
            is FaceEffectPrivacyMode.Icon -> TODO("OpenGL ES port")
            else -> null
        }
        processorPipelineQueue.launch {
            this@FaceEffect.settings = settings
            this@FaceEffect.backgroundImage = backgroundImage
            this@FaceEffect.iconImage = iconImage
        }
    }

    override fun needsFaceDetections(time: Double): VideoEffectDetectionsMode {
        return if (settings.blurFaces || settings.blurBackground || settings.showMouth) {
            VideoEffectDetectionsMode.Now(null)
        } else {
            VideoEffectDetectionsMode.Off
        }
    }

    override fun needsTextDetections(time: Double): VideoEffectDetectionsMode {
        return if (settings.blurText) {
            VideoEffectDetectionsMode.Now(null)
        } else {
            VideoEffectDetectionsMode.Off
        }
    }

    override fun execute(image: Image, info: VideoEffectInfo): Image =
        TODO("OpenGL ES port")

    override fun executeMetalPetal(image: Image, info: VideoEffectInfo): Image =
        TODO("OpenGL ES port")

    private fun makePrivacyImageMetalPetal(image: Any): Any? = TODO("OpenGL ES port")

    private fun calcIconPlacement(detection: Any, imageSize: Any): Any? = TODO("Vision port")

    private fun makeIconLayers(image: Any, icon: Any, detections: List<Any>): List<Any> =
        TODO("OpenGL ES port")

    private fun makeFaceLayers(image: Any, facesImage: Any, detections: List<Any>): List<Any> =
        TODO("OpenGL ES port")

    private fun makeTextLayers(image: Any,
                               privacyImage: Any,
                               detections: List<TextDetection>): List<Any> =
        TODO("OpenGL ES port")

    private fun makeMouthLayers(image: Any, detections: List<Any>): List<Any> =
        TODO("OpenGL ES port")

    private fun makePrivacyImage(image: Any): Any? = TODO("OpenGL ES port")

    private fun createFacesMaskImage(imageExtent: Any, detections: List<Any>): Any? =
        TODO("OpenGL ES port")

    private fun createTextsMaskImage(imageExtent: Any, detections: List<TextDetection>): Any? =
        TODO("OpenGL ES port")

    private fun applyBlur(image: Any,
                          detections: Detections,
                          blurFaces: Boolean,
                          blurText: Boolean,
                          blurBackground: Boolean): Any? =
        TODO("OpenGL ES port")

    private fun addIcons(image: Any?, icon: Any, detections: List<Any>): Any? =
        TODO("OpenGL ES port")

    private fun calcMouth(detection: Any, imageSize: Any, moblinImageSize: Any): Any? =
        TODO("Vision port")

    private fun addMouth(image: Any?, detections: List<Any>?): Any? = TODO("OpenGL ES port")
}

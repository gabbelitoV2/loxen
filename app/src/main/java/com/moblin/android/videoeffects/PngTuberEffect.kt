package com.moblin.android.videoeffects

import android.util.Log
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectDetectionsMode
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.platform.codable.codableJson
import com.moblin.android.platform.coregraphics.CGPoint
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.metalpetal.MTIImage
import com.moblin.android.platform.metalpetal.MTILayer
import com.moblin.android.platform.metalpetal.MTIMultilayerCompositingFilter
import com.moblin.android.platform.uikit.UIImage
import com.moblin.android.platform.uikit.cgImage
import com.moblin.android.various.settings.SettingsSceneWidget
import com.moblin.android.various.settings.SettingsSensitivity
import java.util.UUID
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import com.moblin.android.platform.codable.decode
import com.moblin.android.platform.codable.decodeIfPresent
import com.moblin.android.various.calcFaceAngle
import com.moblin.android.various.isLeftEyeOpen
import com.moblin.android.various.isMouthOpen

private class PngCoordinate(val x: Double, val y: Double) {
    companion object {
        private val coordinateRegex = Regex("""Vector2\(([-\d]+), ([-\d]+)\)""")

        fun decode(value: String): PngCoordinate {
            val match = coordinateRegex.find(value)
            return if (match != null) {
                PngCoordinate(
                    x = match.groupValues[1].toDoubleOrNull() ?: 0.0,
                    y = match.groupValues[2].toDoubleOrNull() ?: 0.0,
                )
            } else {
                PngCoordinate(x = 0.0, y = 0.0)
            }
        }
    }
}

private enum class BlinkTalkState(val rawValue: Int) {
    closed(1),
    `open`(2);

    companion object {
        fun fromRawValue(rawValue: Int): BlinkTalkState? =
            BlinkTalkState.entries.firstOrNull { it.rawValue == rawValue }
    }
}

private class PngTuberImage(
    val costumeLayers: List<Int>,
    val identification: Int,
    val imageData: EffectImageCgImage,
    val offset: PngCoordinate,
    val parentId: Int?,
    val pos: PngCoordinate,
    val showBlink: BlinkTalkState?,
    val showTalk: BlinkTalkState?,
    val zIndex: Int,
) {
    companion object {
        fun decode(container: JsonObject): PngTuberImage {
            val costumeLayers = codableJson.decodeFromString<List<Int>>(container.decode("costumeLayers", ""))
            if (costumeLayers.size != 10) {
                throw SerializationException("Not 10 costumes: ${costumeLayers.size}")
            }
            val identification = container.decode("identification", 0)
            val imageDataString = container.decode("imageData", "")
            val imageDataBytes = runCatching {
                java.util.Base64.getDecoder().decode(imageDataString)
            }.getOrNull()
            val cgImage = imageDataBytes?.let { UIImage(data = it)?.cgImage }
            if (cgImage == null) {
                throw SerializationException("Failed to decode image data")
            }
            val imageData = cgImage.toEffectImage()
            val offset = PngCoordinate.decode(container.decode("offset", ""))
            val parentId = container.decodeIfPresent<Int>("parentId")
            val pos = PngCoordinate.decode(container.decode("pos", ""))
            val showBlink = BlinkTalkState.fromRawValue(container.decode("showBlink", 0))
            val showTalk = BlinkTalkState.fromRawValue(container.decode("showTalk", 0))
            val zIndex = container.decode("zindex", 0)
            return PngTuberImage(
                costumeLayers = costumeLayers,
                identification = identification,
                imageData = imageData,
                offset = offset,
                parentId = parentId,
                pos = pos,
                showBlink = showBlink,
                showTalk = showTalk,
                zIndex = zIndex,
            )
        }
    }
}

private class PngTuberFile(var images: List<PngTuberImage>)

class PngTuberEffect(model: String, costume: Int) : VideoEffect() {
    private val model: PngTuberFile?
    private var videoSourceId: UUID = UUID.randomUUID()
    private var sceneWidget: SettingsSceneWidget? = null
    private var mirror: Boolean = false
    private var isMouthOpen = false
    private var isLeftEyeOpen = true
    private var currentCostumeImages: MutableList<PngTuberImage> = mutableListOf()
    private var sensitivity = SettingsSensitivity()

    init {
        var loadedModel: PngTuberFile? = null
        try {
            val json = codableJson.parseToJsonElement(java.io.File(model).readText()).jsonObject
            val images = json.entries
                .sortedBy { it.key.toIntOrNull() ?: 0 }
                .map { PngTuberImage.decode(it.value.jsonObject) }
            loadedModel = PngTuberFile(images = images)
        } catch (error: Throwable) {
            Log.i("PngTuberEffect", "png-tuber: Failed to load model with error: $error")
        }
        this.model = loadedModel
        setCostume(number = costume)
    }

    fun setVideoSourceId(videoSourceId: UUID) {
        processorPipelineQueue.launch {
            this@PngTuberEffect.videoSourceId = videoSourceId
        }
    }

    fun setSceneWidget(sceneWidget: SettingsSceneWidget) {
        processorPipelineQueue.launch {
            this@PngTuberEffect.sceneWidget = sceneWidget
        }
    }

    fun setSettings(mirror: Boolean, sensitivity: SettingsSensitivity) {
        processorPipelineQueue.launch {
            this@PngTuberEffect.mirror = mirror
            this@PngTuberEffect.sensitivity = sensitivity
        }
    }

    override fun execute(image: CIImage, info: VideoEffectInfo): CIImage {
        val sceneWidget = sceneWidget ?: return image
        updateModelPose(size = image.extent.size, info = info)
        var pngTuberImage: CIImage? = null
        for (costumeImage in visibleCostumeImages()) {
            val layerImage = costumeImage.imageData.getCiImage()
            val currentImage = pngTuberImage
            pngTuberImage = if (currentImage != null) {
                layerImage.composited(over = currentImage)
            } else {
                layerImage
            }
        }
        return pngTuberImage
            ?.resizeMirror(sceneWidget.layout, image.extent.size, mirror)
            ?.move(sceneWidget.layout, image.extent.size)
            ?.composited(over = image)
            ?.cropped(to = image.extent)
            ?: image
    }

    override fun executeMetalPetal(image: MTIImage, info: VideoEffectInfo): MTIImage {
        val sceneWidget = sceneWidget ?: return image
        updateModelPose(size = image.extent.size, info = info)
        val layerImages = visibleCostumeImages().map { it.imageData.getMetalPetalImage() }
        if (layerImages.isEmpty()) {
            return image
        }
        val backgroundSize = image.extent.size
        val contentSize = layerImages.fold(CGSize.zero) { size, layerImage ->
            CGSize(
                width = maxOf(size.width, layerImage.extent.width),
                height = maxOf(size.height, layerImage.extent.height),
            )
        }
        val scale = minOf(
            toPixels(sceneWidget.layout.size, backgroundSize.width) / contentSize.width,
            toPixels(sceneWidget.layout.size, backgroundSize.height) / contentSize.height,
        )
        val size = CGSize(width = contentSize.width * scale, height = contentSize.height * scale)
        val position = metalPetalLayerPosition(sceneWidget.layout, size, backgroundSize)
        val filter = MTIMultilayerCompositingFilter()
        filter.inputBackgroundImage = image
        filter.layers = layerImages.map { layerImage ->
            val layerSize = CGSize(
                width = layerImage.extent.width * scale,
                height = layerImage.extent.height * scale,
            )
            val x = if (mirror) {
                position.x + size.width / 2 - layerSize.width / 2
            } else {
                position.x - size.width / 2 + layerSize.width / 2
            }
            val y = position.y + size.height / 2 - layerSize.height / 2
            MTILayer(
                content = layerImage,
                contentFlipOptions = if (mirror) {
                    MTILayer.FlipOptions.flipHorizontally
                } else {
                    MTILayer.FlipOptions.donotFlip
                },
                position = CGPoint(x = x, y = y),
                size = layerSize,
            )
        }
        return filter.outputImage ?: image
    }

    private fun visibleCostumeImages(): List<PngTuberImage> {
        return currentCostumeImages.filter { shouldShowImage(image = it) }
    }

    private fun shouldShowImage(image: PngTuberImage): Boolean {
        when (image.showBlink) {
            BlinkTalkState.closed -> {
                if (isLeftEyeOpen) {
                    return false
                }
            }
            BlinkTalkState.open -> {
                if (!isLeftEyeOpen) {
                    return false
                }
            }
            null -> {}
        }
        when (image.showTalk) {
            BlinkTalkState.closed -> {
                if (isMouthOpen) {
                    return false
                }
            }
            BlinkTalkState.open -> {
                if (!isMouthOpen) {
                    return false
                }
            }
            null -> {}
        }
        return true
    }

    private fun updateModelPose(size: CGSize, info: VideoEffectInfo) {
        val detection = info.faceDetections(videoSourceId)?.firstOrNull()
        val rotationAngle = detection?.calcFaceAngle(imageSize = size)
        if (detection != null && rotationAngle != null) {
            isMouthOpen = detection.isMouthOpen(
                rotationAngle = rotationAngle,
                sensitivity = sensitivity.mouth,
            ) > 0.15
            isLeftEyeOpen = -(detection.isLeftEyeOpen(
                rotationAngle = rotationAngle,
                sensitivity = sensitivity.eyes,
            ) - 1) > 0.1
        }
    }

    private fun setCostume(number: Int) {
        val model = this.model ?: return
        if (number < 1 || number > 10) {
            return
        }
        currentCostumeImages.clear()
        for (image in model.images.sortedBy { it.zIndex }) {
            if (image.costumeLayers[number - 1] != 1) {
                continue
            }
            currentCostumeImages.add(image)
        }
    }

    override fun needsFaceDetections(interval: Double): VideoEffectDetectionsMode {
        return VideoEffectDetectionsMode.Interval(videoSourceId, 0.1)
    }
}

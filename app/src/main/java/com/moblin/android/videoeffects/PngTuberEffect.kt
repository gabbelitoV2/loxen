package com.moblin.android.videoeffects

import android.graphics.BitmapFactory
import android.media.Image
import android.util.Base64
import android.util.Log
import android.util.SizeF
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectDetectionsMode
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.various.settings.SettingsSceneWidget
import com.moblin.android.various.settings.SettingsSensitivity
import java.io.File
import java.util.UUID
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private fun JsonObject.requireString(key: String): String =
    (this[key] ?: throw IllegalStateException("Missing key $key")).jsonPrimitive.content

private fun JsonObject.requireInt(key: String): Int =
    (this[key] ?: throw IllegalStateException("Missing key $key")).jsonPrimitive.int

private class PngCoordinate(val x: Double, val y: Double) {
    companion object {
        private val regex = Regex("""Vector2\(([-\d]+), ([-\d]+)\)""")

        fun fromString(value: String): PngCoordinate {
            val match = regex.find(value)
            return if (match != null) {
                PngCoordinate(
                    match.groupValues[1].toDoubleOrNull() ?: 0.0,
                    match.groupValues[2].toDoubleOrNull() ?: 0.0
                )
            } else {
                PngCoordinate(0.0, 0.0)
            }
        }
    }
}

private enum class BlinkTalkState(val rawValue: Int) {
    Closed(1),
    Open(2);

    companion object {
        fun fromRawValue(rawValue: Int): BlinkTalkState? = entries.firstOrNull { it.rawValue == rawValue }
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
    val zIndex: Int
) {
    companion object {
        fun fromJson(json: JsonObject): PngTuberImage {
            val costumeLayersData = json.requireString("costumeLayers")
            val costumeLayers = Json.decodeFromString<List<Int>>(costumeLayersData)
            if (costumeLayers.size != 10) {
                throw IllegalStateException("Not 10 costumes: ${costumeLayers.size}")
            }
            val identification = json.requireInt("identification")
            val imageDataString = json.requireString("imageData")
            val imageDataBytes = Base64.decode(imageDataString, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageDataBytes, 0, imageDataBytes.size)
                ?: throw IllegalStateException("Failed to decode image data")
            val imageData: EffectImageCgImage = TODO("EffectImageCgImage from decoded Bitmap")
            val offset = PngCoordinate.fromString(json.requireString("offset"))
            val parentId = json["parentId"]?.jsonPrimitive?.intOrNull
            val pos = PngCoordinate.fromString(json.requireString("pos"))
            val showBlink = BlinkTalkState.fromRawValue(json.requireInt("showBlink"))
            val showTalk = BlinkTalkState.fromRawValue(json.requireInt("showTalk"))
            val zIndex = json.requireInt("zindex")
            return PngTuberImage(
                costumeLayers,
                identification,
                imageData,
                offset,
                parentId,
                pos,
                showBlink,
                showTalk,
                zIndex
            )
        }
    }
}

private class PngTuberFile(val images: List<PngTuberImage>)

class PngTuberEffect(modelPath: String, costume: Int) : VideoEffect() {
    private val model: PngTuberFile?
    private var videoSourceId: UUID = UUID.randomUUID()
    private var sceneWidget: SettingsSceneWidget? = null
    private var mirror: Boolean = false
    private var isMouthOpen = false
    private var isLeftEyeOpen = true
    private var currentCostumeImages: MutableList<PngTuberImage> = mutableListOf()
    private var sensitivity = SettingsSensitivity()

    init {
        this.model = try {
            val root = Json.parseToJsonElement(File(modelPath).readText()).jsonObject
            val images = root.entries
                .sortedBy { it.key.toIntOrNull() ?: 0 }
                .map { PngTuberImage.fromJson(it.value.jsonObject) }
            PngTuberFile(images)
        } catch (e: Exception) {
            Log.i(TAG, "png-tuber: Failed to load model with error: $e")
            null
        }
        setCostume(costume)
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

    override fun execute(image: Image, info: VideoEffectInfo): Image {
        val sceneWidget = sceneWidget ?: return image
        updateModelPose(SizeF(image.width.toFloat(), image.height.toFloat()), info)
        TODO()
    }

    override fun executeMetalPetal(image: Image, info: VideoEffectInfo): Image {
        TODO()
    }

    private fun visibleCostumeImages(): List<PngTuberImage> =
        currentCostumeImages.filter { shouldShowImage(it) }

    private fun shouldShowImage(image: PngTuberImage): Boolean {
        when (image.showBlink) {
            BlinkTalkState.Closed -> if (isLeftEyeOpen) return false
            BlinkTalkState.Open -> if (!isLeftEyeOpen) return false
            else -> {}
        }
        when (image.showTalk) {
            BlinkTalkState.Closed -> if (isMouthOpen) return false
            BlinkTalkState.Open -> if (!isMouthOpen) return false
            else -> {}
        }
        return true
    }

    private fun updateModelPose(size: SizeF, info: VideoEffectInfo) {
        val detection = info.faceDetections(videoSourceId)?.firstOrNull() ?: return
        Unit
    }

    private fun setCostume(number: Int) {
        val model = model ?: return
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

    companion object {
        private const val TAG = "PngTuberEffect"
    }
}

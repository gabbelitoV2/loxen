package com.moblin.android.various.settings

import android.graphics.RectF
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.moblin.android.common.various.RgbColor
import com.moblin.android.media.haishinkit.media.video.SceneSwitchTransition
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.remotecontrol.RemoteControlScoreboardMatchConfig
import com.moblin.android.various.MainTimer
import com.moblin.android.various.model.CameraId
import com.moblin.android.various.model.Model
import com.moblin.android.various.utils.Named
import com.moblin.android.various.utils.bestBackCameraId
import com.moblin.android.various.utils.bestFrontCameraId
import com.moblin.android.various.utils.defaultBackCameraPosition
import com.moblin.android.various.utils.fieldOfViewToZoom
import com.moblin.android.various.utils.hasDualBackCamera
import com.moblin.android.various.utils.hasTripleBackCamera
import com.moblin.android.various.utils.hasWideDualBackCamera
import com.moblin.android.various.utils.utcTimeDeltaFromNow
import com.moblin.android.various.utils.zoomToFieldOfView
import com.moblin.android.videoeffects.AnamorphicLensEffect
import com.moblin.android.videoeffects.dewarp360.Dewarp360Effect
import com.moblin.android.videoeffects.dewarp360.Dewarp360EffectSettings
import com.moblin.android.videoeffects.GrayScaleEffect
import com.moblin.android.videoeffects.LutEffect
import com.moblin.android.videoeffects.MaskEffect
import com.moblin.android.videoeffects.MaskEffectPoint
import com.moblin.android.videoeffects.MaskEffectSettings
import com.moblin.android.videoeffects.OpacityEffect
import com.moblin.android.videoeffects.PinchEffect
import com.moblin.android.videoeffects.RemoveBackgroundEffect
import com.moblin.android.videoeffects.SepiaEffect
import com.moblin.android.videoeffects.ShapeEffect
import com.moblin.android.videoeffects.ShapeEffectSettings
import com.moblin.android.videoeffects.VideoSourceEffectSettings
import com.moblin.android.videoeffects.WhirlpoolEffect
import com.moblin.android.view.settings.scenes.widgets.widget.text.fontStyleName
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import com.moblin.android.localized

private fun RgbColor.color(): Color = Color(
    red = red.toFloat() / 255.0f,
    green = green.toFloat() / 255.0f,
    blue = blue.toFloat() / 255.0f,
    alpha = (opacity ?: 1.0).toFloat()
)

private fun decodeCameraId(container: JsonObject, key: String, defaultValue: CameraId): CameraId {
    val cameraId = container[key]?.jsonPrimitive?.contentOrNull ?: return defaultValue
    return cameraId
}

private fun decodeCameraPosition(
    container: JsonObject,
    key: String,
    defaultValue: SettingsSceneCameraPosition
): SettingsSceneCameraPosition {
    var position = SettingsSceneCameraPosition.fromRawValue(
        container[key]?.jsonPrimitive?.contentOrNull ?: ""
    ) ?: defaultValue
    if ((position == SettingsSceneCameraPosition.backTripleLowEnergy && !hasTripleBackCamera) ||
        (position == SettingsSceneCameraPosition.backDualLowEnergy && !hasDualBackCamera) ||
        (position == SettingsSceneCameraPosition.backWideDualLowEnergy && !hasWideDualBackCamera)
    ) {
        position = defaultValue
    }
    return position
}

@Serializable
enum class SettingsVideoEffectType(val rawValue: String) {
    @SerialName("shape") shape("shape"),
    @SerialName("grayScale") grayScale("grayScale"),
    @SerialName("sepia") sepia("sepia"),
    @SerialName("whirlpool") whirlpool("whirlpool"),
    @SerialName("pinch") pinch("pinch"),
    @SerialName("removeBackground") removeBackground("removeBackground"),
    @SerialName("dewarp360") dewarp360("dewarp360"),
    @SerialName("anamorphicLens") anamorphicLens("anamorphicLens"),
    @SerialName("lut") lut("lut"),
    @SerialName("opacity") opacity("opacity"),
    @SerialName("mask") mask("mask");

    override fun toString(): String = when (this) {
        shape -> localized("Shape")
        grayScale -> localized("Gray scale")
        sepia -> localized("Sepia")
        whirlpool -> localized("Whirlpool")
        pinch -> localized("Pinch")
        removeBackground -> localized("Remove background")
        dewarp360 -> localized("Dewarp 360")
        anamorphicLens -> localized("Anamorphic lens")
        lut -> localized("LUT")
        opacity -> localized("Opacity")
        mask -> localized("Mask")
    }

    companion object {
        fun fromRawValue(rawValue: String): SettingsVideoEffectType? =
            entries.firstOrNull { it.rawValue == rawValue }
    }
}

private val defaultFromColor = RgbColor(red = 220, green = 235, blue = 92)
private val defaultToColor = RgbColor(red = 82, green = 180, blue = 203)

@Serializable
class SettingsVideoEffectRemoveBackground(
    var from: RgbColor = defaultFromColor,
    var to: RgbColor = defaultToColor
) {
    @Transient var fromColor: Color = from.color()
    @Transient var toColor: Color = to.color()
}

@Serializable
class SettingsVideoEffectShape(
    var cornerRadius: Float = 0.1f,
    var borderWidth: Double = 0.0,
    var borderColor: RgbColor = RgbColor(red = 0, green = 0, blue = 0),
    var cropEnabled: Boolean = false,
    var cropX: Double = 0.25,
    var cropY: Double = 0.0,
    var cropWidth: Double = 0.5,
    var cropHeight: Double = 1.0
) {
    @Transient var borderColorColor: Color = borderColor.color()

    fun toSettings(): ShapeEffectSettings = ShapeEffectSettings(
        cornerRadius = cornerRadius,
        borderWidth = borderWidth,
        borderColor = TODO("CIColor has no Android counterpart"),
        cropEnabled = cropEnabled,
        cropX = cropX,
        cropY = cropY,
        cropWidth = cropWidth,
        cropHeight = cropHeight
    )
}

@Serializable
class SettingsVideoEffectDewarp360(
    var pan: Float = 0f,
    var tilt: Float = 0f,
    var zoom: Float = 1f
) {
    @Transient var inverseFieldOfView: Float =
        (180.0 - Math.toDegrees(zoomToFieldOfView(zoom).toDouble())).toFloat()

    fun updateZoomFromInverseFieldOfView() {
        zoom = fieldOfViewToZoom(Math.toRadians((180f - inverseFieldOfView).toDouble()).toFloat())
    }

    fun toSettings(): Dewarp360EffectSettings = Dewarp360EffectSettings.Direct(
        pan = -Math.toRadians(pan.toDouble()).toFloat(),
        tilt = Math.toRadians(tilt.toDouble()).toFloat(),
        fieldOfView = zoomToFieldOfView(zoom)
    )
}

@Serializable
class SettingsVideoEffectAnamorphicLens(
    var scale: Double = 1.33
) {
    fun clone(): SettingsVideoEffectAnamorphicLens {
        val new = SettingsVideoEffectAnamorphicLens()
        new.scale = scale
        return new
    }
}

@Serializable
class SettingsVideoEffectLut(
    @Contextual var lut: UUID? = UUID.randomUUID()
)

@Serializable
class SettingsVideoEffectOpacity(
    var opacity: Double = 0.5
)

@Serializable
enum class SettingsMaskBackgroundType(val rawValue: String) {
    @SerialName("Transparent") transparent("Transparent"),
    @SerialName("Solid") solid("Solid"),
    @SerialName("Checkerboard") checkerboard("Checkerboard");

    override fun toString(): String = when (this) {
        transparent -> localized("Transparent")
        solid -> localized("Solid")
        checkerboard -> localized("Checkerboard")
    }

    companion object {
        fun fromRawValue(rawValue: String): SettingsMaskBackgroundType? =
            entries.firstOrNull { it.rawValue == rawValue }
    }
}

@Serializable
data class SettingsVideoEffectMaskEffectPoint(
    val x: Double = 50.0,
    val y: Double = 50.0
) {
    @Transient val id: UUID = UUID.randomUUID()
}

@Serializable
class SettingsVideoEffectMask(
    var points: List<SettingsVideoEffectMaskEffectPoint> = defaultPoints,
    var inverted: Boolean = false,
    var tension: Double = defaultTension,
    var backgroundType: SettingsMaskBackgroundType = SettingsMaskBackgroundType.transparent,
    var backgroundColor: RgbColor = defaultBackgroundColor,
    var backgroundColor2: RgbColor = defaultBackgroundColor2
) {
    companion object {
        private val defaultPoints = listOf(
            SettingsVideoEffectMaskEffectPoint(x = 25.0, y = 50.0),
            SettingsVideoEffectMaskEffectPoint(x = 50.0, y = 75.0),
            SettingsVideoEffectMaskEffectPoint(x = 75.0, y = 50.0),
            SettingsVideoEffectMaskEffectPoint(x = 50.0, y = 25.0)
        )
        private val defaultTension = 1.0 / 6.0
        private val defaultBackgroundColor = RgbColor(red = 0, green = 0, blue = 0)
        private val defaultBackgroundColor2 = RgbColor(red = 255, green = 255, blue = 255)
    }

    @Transient var backgroundColorColor: Color = backgroundColor.color()
    @Transient var backgroundColorColor2: Color = backgroundColor2.color()

    fun toEffectSettings(): MaskEffectSettings = MaskEffectSettings(
        points = points.map { MaskEffectPoint(x = it.x / 100, y = it.y / 100) },
        inverted = inverted,
        tension = tension,
        backgroundType = backgroundType,
        backgroundColor = backgroundColor,
        backgroundColor2 = backgroundColor2
    )
}

@Serializable
class SettingsVideoEffect(
    @Contextual var id: UUID = UUID.randomUUID(),
    var enabled: Boolean = true,
    var type: SettingsVideoEffectType = SettingsVideoEffectType.shape,
    var removeBackground: SettingsVideoEffectRemoveBackground = SettingsVideoEffectRemoveBackground(),
    var shape: SettingsVideoEffectShape = SettingsVideoEffectShape(),
    var dewarp360: SettingsVideoEffectDewarp360 = SettingsVideoEffectDewarp360(),
    var anamorphicLens: SettingsVideoEffectAnamorphicLens = SettingsVideoEffectAnamorphicLens(),
    var lut: SettingsVideoEffectLut = SettingsVideoEffectLut(),
    var opacity: SettingsVideoEffectOpacity = SettingsVideoEffectOpacity(),
    var mask: SettingsVideoEffectMask = SettingsVideoEffectMask()
) {
    fun getEffect(model: Model): VideoEffect = when (type) {
        SettingsVideoEffectType.grayScale -> GrayScaleEffect()
        SettingsVideoEffectType.sepia -> SepiaEffect()
        SettingsVideoEffectType.whirlpool -> WhirlpoolEffect(angle = (Math.PI / 2).toFloat())
        SettingsVideoEffectType.pinch -> PinchEffect(scale = 0.5f)
        SettingsVideoEffectType.removeBackground -> {
            val effect = RemoveBackgroundEffect()
            effect.setColorRange(from = removeBackground.from, to = removeBackground.to)
            effect
        }
        SettingsVideoEffectType.shape -> {
            val effect = ShapeEffect()
            effect.setSettings(settings = shape.toSettings())
            effect
        }
        SettingsVideoEffectType.dewarp360 -> {
            val effect = Dewarp360Effect()
            effect.setSettings(settings = dewarp360.toSettings())
            effect
        }
        SettingsVideoEffectType.anamorphicLens ->
            AnamorphicLensEffect(settings = anamorphicLens.clone())
        SettingsVideoEffectType.lut -> {
            val effect = LutEffect()
            TODO("Model.getLogLutById is not available")
        }
        SettingsVideoEffectType.opacity -> {
            val effect = OpacityEffect()
            effect.setOpacity(opacity = opacity.opacity)
            effect
        }
        SettingsVideoEffectType.mask -> {
            val effect = MaskEffect()
            effect.setSettings(settings = mask.toEffectSettings())
            effect
        }
    }
}

@Serializable
enum class SettingsFontDesign(val rawValue: String) {
    @SerialName("Default") `default`("Default"),
    @SerialName("Serif") serif("Serif"),
    @SerialName("Rounded") rounded("Rounded"),
    @SerialName("Monospaced") monospaced("Monospaced");

    override fun toString(): String = when (this) {
        `default` -> localized("Default")
        serif -> localized("Serif")
        rounded -> localized("Rounded")
        monospaced -> localized("Monospaced")
    }

    fun toSystem(): FontFamily = when (this) {
        `default` -> FontFamily.Default
        serif -> FontFamily.Serif
        rounded -> TODO("Compose has no rounded system font family")
        monospaced -> FontFamily.Monospace
    }

    fun toUiKit(): Any = TODO("no Android counterpart for UIFontDescriptor.SystemDesign")

    companion object {
        fun fromRawValue(rawValue: String): SettingsFontDesign? =
            entries.firstOrNull { it.rawValue == rawValue }
    }
}

@Serializable
enum class SettingsFontWeight(val rawValue: String) {
    @SerialName("Regular") regular("Regular"),
    @SerialName("Light") light("Light"),
    @SerialName("Bold") bold("Bold");

    override fun toString(): String = when (this) {
        regular -> localized("Regular")
        light -> localized("Light")
        bold -> localized("Bold")
    }

    fun toSystem(): FontWeight = when (this) {
        regular -> FontWeight.Normal
        light -> FontWeight.Light
        bold -> FontWeight.Bold
    }

    fun toUiKit(): Any = TODO("no Android counterpart for UIFont.Weight")

    companion object {
        fun fromRawValue(rawValue: String): SettingsFontWeight? =
            entries.firstOrNull { it.rawValue == rawValue }
    }
}

@Serializable
enum class SettingsHorizontalAlignment(val rawValue: String) {
    @SerialName("Leading") leading("Leading"),
    @SerialName("Trailing") trailing("Trailing"),
    @SerialName("Center") center("Center");

    override fun toString(): String = when (this) {
        leading -> localized("Leading")
        trailing -> localized("Trailing")
        center -> localized("Center")
    }

    fun toSystem(): Alignment.Horizontal = when (this) {
        leading -> Alignment.Start
        trailing -> Alignment.End
        center -> Alignment.CenterHorizontally
    }

    companion object {
        fun fromRawValue(rawValue: String): SettingsHorizontalAlignment? =
            entries.firstOrNull { it.rawValue == rawValue }
    }
}

@Serializable
enum class SettingsVerticalAlignment(val rawValue: String) {
    @SerialName("Top") top("Top"),
    @SerialName("Bottom") bottom("Bottom");

    companion object {
        fun fromRawValue(rawValue: String): SettingsVerticalAlignment? =
            entries.firstOrNull { it.rawValue == rawValue }
    }
}

@Serializable
enum class SettingsAlignment(val rawValue: String) {
    @SerialName("TopLeft") topLeft("TopLeft"),
    @SerialName("TopRight") topRight("TopRight"),
    @SerialName("BottomLeft") bottomLeft("BottomLeft"),
    @SerialName("BottomRight") bottomRight("BottomRight"),
    @SerialName("TopCenter") topCenter("TopCenter"),
    @SerialName("BottomCenter") bottomCenter("BottomCenter"),
    @SerialName("LeftCenter") leftCenter("LeftCenter"),
    @SerialName("RightCenter") rightCenter("RightCenter"),
    @SerialName("Center") center("Center");

    fun isLeft(): Boolean = this == topLeft || this == bottomLeft || this == leftCenter

    fun isHorizontalCenter(): Boolean = this == topCenter || this == bottomCenter || this == center

    fun isVerticalCenter(): Boolean = this == leftCenter || this == rightCenter || this == center

    fun isTop(): Boolean = this == topLeft || this == topRight || this == topCenter

    fun mirrorPositionHorizontally(): Boolean =
        this == topRight || this == bottomRight || this == rightCenter

    fun mirrorPositionVertically(): Boolean =
        this == bottomLeft || this == bottomRight || this == bottomCenter

    companion object {
        fun fromRawValue(rawValue: String): SettingsAlignment? =
            entries.firstOrNull { it.rawValue == rawValue }
    }
}

@Serializable
class SettingsWidgetTextTimer(
    @Contextual var id: UUID = UUID.randomUUID(),
    var delta: Int = 5,
    var endTime: Double = 0.0
) {
    fun add(delta: Double) {
        if (timeLeft() < 0) {
            endTime = Instant.now().toEpochMilli() / 1000.0
        }
        endTime += delta
    }

    fun set(time: Double) {
        endTime = Instant.now().toEpochMilli() / 1000.0 + time
    }

    fun format(): String {
        val total = maxOf(timeLeft(), 0.0).toLong()
        val hours = total / 3600
        val minutes = (total % 3600) / 60
        val seconds = total % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%d:%02d".format(minutes, seconds)
        }
    }

    fun textEffectEndTime(): Any = TODO("ContinuousClock.Instant has no Android counterpart")

    fun timeLeft(): Double = utcTimeDeltaFromNow(endTime)
}

@Serializable
class SettingsWidgetTextStopwatch(
    @Contextual var id: UUID = UUID.randomUUID(),
    var totalElapsed: Double = 0.0,
    var running: Boolean = false
) {
    @Transient var playPressedTime: Instant = Instant.now()

    fun clone(): SettingsWidgetTextStopwatch {
        val new = SettingsWidgetTextStopwatch()
        new.id = id
        new.totalElapsed = totalElapsed
        new.playPressedTime = playPressedTime
        new.running = running
        return new
    }

    fun currentTime(): Double = if (running) {
        totalElapsed + Duration.between(playPressedTime, Instant.now()).toNanos() / 1e9
    } else {
        totalElapsed
    }
}

@Serializable
class SettingsWidgetTextSubtitles(
    var identifier: String? = null
)

@Serializable
class SettingsWidgetTextCheckbox(
    @Contextual var id: UUID = UUID.randomUUID(),
    var checked: Boolean = false
)

@Serializable
class SettingsWidgetTextRating(
    @Contextual var id: UUID = UUID.randomUUID(),
    var rating: Int = 0
)

@Serializable
class SettingsWidgetTextLapTimes(
    @Contextual var id: UUID = UUID.randomUUID(),
    var currentLapStartTime: Double? = null,
    var lapTimes: List<Double> = emptyList()
)

@Serializable
class SettingsWidgetText(
    var formatString: String = "{shortTime}",
    var backgroundColor: RgbColor = RgbColor(red = 0, green = 0, blue = 0, opacity = 0.75),
    var clearBackgroundColor: Boolean = false,
    var foregroundColor: RgbColor = RgbColor(red = 255, green = 255, blue = 255),
    var clearForegroundColor: Boolean = false,
    var fontSize: Int = 30,
    var fontFamily: String? = null,
    var fontStyle: String = "",
    var fontDesign: SettingsFontDesign = SettingsFontDesign.`default`,
    var fontWeight: SettingsFontWeight = SettingsFontWeight.regular,
    var fontMonospacedDigits: Boolean = false,
    var alignment: SettingsHorizontalAlignment = SettingsHorizontalAlignment.leading,
    var horizontalAlignment: SettingsHorizontalAlignment = SettingsHorizontalAlignment.leading,
    var verticalAlignment: SettingsVerticalAlignment = SettingsVerticalAlignment.top,
    var delay: Double = 0.0,
    var timers: List<SettingsWidgetTextTimer> = emptyList(),
    var stopwatches: List<SettingsWidgetTextStopwatch> = emptyList(),
    var needsWeather: Boolean = false,
    var needsGeography: Boolean = false,
    var needsSubtitles: Boolean = false,
    var subtitles: List<SettingsWidgetTextSubtitles> = emptyList(),
    var checkboxes: List<SettingsWidgetTextCheckbox> = emptyList(),
    var ratings: List<SettingsWidgetTextRating> = emptyList(),
    var lapTimes: List<SettingsWidgetTextLapTimes> = emptyList(),
    var needsGForce: Boolean = false,
    var widthEnabled: Boolean = false,
    var width: Int = defaultWidth,
    var cornerRadius: Int = defaultCornerRadius
) {
    companion object {
        private const val defaultWidth = 300
        private const val defaultCornerRadius = 10
    }

    @Transient var backgroundColorColor: Color = backgroundColor.color()
    @Transient var foregroundColorColor: Color = foregroundColor.color()
    @Transient var fontSizeFloat: Float = fontSize.toFloat()

    fun fontFamilyString(): String = fontFamily ?: localized("System")

    fun fontStyleString(): String {
        val family = fontFamily
        return if (family != null) {
            fontStyleName(family = family, fontName = fontStyle)
        } else {
            ""
        }
    }
}

@Serializable
class SettingsWidgetCrop(
    @Contextual var sourceWidgetId: UUID = UUID.randomUUID(),
    var x: Int = 0,
    var y: Int = 0,
    var width: Int = 200,
    var height: Int = 200
) {
    fun clone(): SettingsWidgetCrop {
        val new = SettingsWidgetCrop()
        new.sourceWidgetId = sourceWidgetId
        new.x = x
        new.y = y
        new.width = width
        new.height = height
        return new
    }
}

@Serializable
enum class SettingsWidgetBrowserMode {
    periodicAudioAndVideo,
    audioAndVideoOnly,
    audioOnly;

    override fun toString(): String = when (this) {
        periodicAudioAndVideo -> localized("Periodic, audio and video")
        audioAndVideoOnly -> localized("Audio and video only")
        audioOnly -> localized("Audio only")
    }

    companion object {
        fun fromRawValue(rawValue: String): SettingsWidgetBrowserMode? =
            entries.firstOrNull { it.name == rawValue }
    }
}

@Serializable
class SettingsWidgetBrowser(
    var url: String = "",
    var width: Int = 500,
    var height: Int = 500,
    var mode: SettingsWidgetBrowserMode = SettingsWidgetBrowserMode.periodicAudioAndVideo,
    @SerialName("fps") var baseFps: Float = 5.0f,
    var styleSheet: String = "",
    var moblinAccess: Boolean = false,
    var speechToText: Boolean = false,
    var localOnly: Boolean = false
)

@Serializable
class SettingsWidgetMap(
    var northUp: Boolean = false,
    var delay: Double = 0.0,
    @SerialName("scale") var size: Double = 1000.0
) {
    fun clone(): SettingsWidgetMap {
        val new = SettingsWidgetMap()
        new.northUp = northUp
        new.delay = delay
        new.size = size
        return new
    }
}

@Serializable
class SettingsWidgetScene(
    @Contextual var sceneId: UUID = UUID.randomUUID()
)

@Serializable
class SettingsWidgetQrCode(
    var message: String = ""
) {
    fun clone(): SettingsWidgetQrCode {
        val new = SettingsWidgetQrCode()
        new.message = message
        return new
    }
}

@Serializable
enum class SettingsWidgetAlertPositionType(val rawValue: String) {
    @SerialName("Scene") scene("Scene"),
    @SerialName("Face") face("Face");

    override fun toString(): String = when (this) {
        scene -> localized("Scene")
        face -> localized("Face")
    }

    companion object {
        fun fromRawValue(rawValue: String): SettingsWidgetAlertPositionType? =
            entries.firstOrNull { it.rawValue == rawValue }
    }
}

@Serializable
class SettingsWidgetAlertFacePosition(
    var x: Double = 0.25,
    var y: Double = 0.25,
    var width: Double = 0.5,
    var height: Double = 0.5
) {
    fun clone(): SettingsWidgetAlertFacePosition {
        val new = SettingsWidgetAlertFacePosition()
        new.x = x
        new.y = y
        new.width = width
        new.height = height
        return new
    }
}

@Serializable
enum class SettingsWidgetAlertsAlertMediaType {
    gifAndSound,
    video;

    override fun toString(): String = when (this) {
        gifAndSound -> "GIF and sound"
        video -> "Video"
    }

    companion object {
        fun fromRawValue(rawValue: String): SettingsWidgetAlertsAlertMediaType? =
            entries.firstOrNull { it.name == rawValue }
    }
}

@Serializable
class SettingsWidgetAlertsAlert(
    @Contextual var id: UUID = UUID.randomUUID(),
    var enabled: Boolean = true,
    var mediaType: SettingsWidgetAlertsAlertMediaType = SettingsWidgetAlertsAlertMediaType.gifAndSound,
    @Contextual var imageId: UUID = UUID.randomUUID(),
    var imageLoopCount: Int = 1,
    @Contextual var soundId: UUID = UUID.randomUUID(),
    var videoName: String = "",
    var textColor: RgbColor = RgbColor(red = 255, green = 255, blue = 255),
    var accentColor: RgbColor = RgbColor(red = 0xFD, green = 0xFB, blue = 0x67),
    var fontSize: Int = 45,
    var fontDesign: SettingsFontDesign = SettingsFontDesign.monospaced,
    var fontWeight: SettingsFontWeight = SettingsFontWeight.bold,
    var textToSpeechEnabled: Boolean = true,
    var textToSpeechDelay: Double = 1.5,
    var textToSpeechLanguageVoices: Map<String, SettingsVoice> = emptyMap(),
    var positionType: SettingsWidgetAlertPositionType = SettingsWidgetAlertPositionType.scene,
    var facePosition: SettingsWidgetAlertFacePosition = SettingsWidgetAlertFacePosition()
) {
    fun isTextToSpeechEnabled(): Boolean = enabled && textToSpeechEnabled

    fun makeVideoFilename(): String? {
        val fileExtension = videoName.substringAfterLast('.', "")
        if (fileExtension.isEmpty()) {
            return null
        }
        return "$id.$fileExtension"
    }

    fun clone(): SettingsWidgetAlertsAlert {
        val new = SettingsWidgetAlertsAlert()
        new.id = id
        new.enabled = enabled
        new.mediaType = mediaType
        new.imageId = imageId
        new.imageLoopCount = imageLoopCount
        new.soundId = soundId
        new.videoName = videoName
        new.textColor = textColor
        new.accentColor = accentColor
        new.fontSize = fontSize
        new.fontDesign = fontDesign
        new.fontWeight = fontWeight
        new.textToSpeechEnabled = textToSpeechEnabled
        new.textToSpeechDelay = textToSpeechDelay
        new.textToSpeechLanguageVoices = textToSpeechLanguageVoices
        new.positionType = positionType
        new.facePosition = facePosition.clone()
        return new
    }
}

@Serializable
enum class SettingsWidgetAlertsCheerBitsAlertOperator(val rawValue: String) {
    @SerialName("=") equal("="),
    @SerialName(">=") greaterEqual(">=");

    companion object {
        fun fromRawValue(rawValue: String): SettingsWidgetAlertsCheerBitsAlertOperator =
            entries.firstOrNull { it.rawValue == rawValue } ?: equal
    }
}

val cheerBitsAlertOperators: List<String> =
    SettingsWidgetAlertsCheerBitsAlertOperator.entries.map { it.rawValue }

@Serializable
class SettingsWidgetAlertsCheerBitsAlert(
    @Contextual var id: UUID = UUID.randomUUID(),
    var bits: Int = 1,
    var comparisonOperator: SettingsWidgetAlertsCheerBitsAlertOperator =
        SettingsWidgetAlertsCheerBitsAlertOperator.greaterEqual,
    var alert: SettingsWidgetAlertsAlert = SettingsWidgetAlertsAlert()
) {
    fun clone(): SettingsWidgetAlertsCheerBitsAlert {
        val new = SettingsWidgetAlertsCheerBitsAlert()
        new.bits = bits
        new.comparisonOperator = comparisonOperator
        new.alert = alert.clone()
        return new
    }
}

@Serializable
class SettingsWidgetAlertsKickGiftsAlert(
    @Contextual var id: UUID = UUID.randomUUID(),
    var amount: Int = 1,
    var comparisonOperator: SettingsWidgetAlertsCheerBitsAlertOperator =
        SettingsWidgetAlertsCheerBitsAlertOperator.greaterEqual,
    var alert: SettingsWidgetAlertsAlert = SettingsWidgetAlertsAlert()
) {
    fun clone(): SettingsWidgetAlertsKickGiftsAlert {
        val new = SettingsWidgetAlertsKickGiftsAlert()
        new.amount = amount
        new.comparisonOperator = comparisonOperator
        new.alert = alert.clone()
        return new
    }
}

private fun createDefaultCheerBits(): List<SettingsWidgetAlertsCheerBitsAlert> {
    val cheerBits = mutableListOf<SettingsWidgetAlertsCheerBitsAlert>()
    for ((index, bits) in listOf(1).withIndex()) {
        val cheer = SettingsWidgetAlertsCheerBitsAlert()
        cheer.bits = bits
        cheer.alert.enabled = index == 0
        cheerBits.add(cheer)
    }
    return cheerBits
}

private fun createDefaultKickGifts(): List<SettingsWidgetAlertsKickGiftsAlert> {
    val kickGifts = mutableListOf<SettingsWidgetAlertsKickGiftsAlert>()
    for ((index, amount) in listOf(1).withIndex()) {
        val gift = SettingsWidgetAlertsKickGiftsAlert()
        gift.amount = amount
        gift.alert.enabled = index == 0
        kickGifts.add(gift)
    }
    return kickGifts
}

@Serializable
class SettingsWidgetAlertsTwitch(
    var follows: SettingsWidgetAlertsAlert = SettingsWidgetAlertsAlert(),
    var subscriptions: SettingsWidgetAlertsAlert = SettingsWidgetAlertsAlert(),
    var raids: SettingsWidgetAlertsAlert = SettingsWidgetAlertsAlert(),
    var cheers: SettingsWidgetAlertsAlert = SettingsWidgetAlertsAlert(),
    var cheerBits: List<SettingsWidgetAlertsCheerBitsAlert> = createDefaultCheerBits()
) {
    @Transient var redemptions: List<SettingsWidgetAlertsAlert> = emptyList()

    fun clone(): SettingsWidgetAlertsTwitch {
        val new = SettingsWidgetAlertsTwitch()
        new.follows = follows.clone()
        new.subscriptions = subscriptions.clone()
        new.raids = raids.clone()
        new.cheers = cheers.clone()
        new.cheerBits = cheerBits.map { it.clone() }
        return new
    }

    fun disableAll() {
        follows.enabled = false
        subscriptions.enabled = false
        raids.enabled = false
        cheers.enabled = false
        for (cheerBit in cheerBits) {
            cheerBit.alert.enabled = false
        }
    }
}

@Serializable
class SettingsWidgetAlertsKick(
    var subscriptions: SettingsWidgetAlertsAlert = SettingsWidgetAlertsAlert(),
    var giftedSubscriptions: SettingsWidgetAlertsAlert = SettingsWidgetAlertsAlert(),
    var hosts: SettingsWidgetAlertsAlert = SettingsWidgetAlertsAlert(),
    var rewards: SettingsWidgetAlertsAlert = SettingsWidgetAlertsAlert(),
    var kickGifts: List<SettingsWidgetAlertsKickGiftsAlert> = createDefaultKickGifts()
) {
    fun clone(): SettingsWidgetAlertsKick {
        val new = SettingsWidgetAlertsKick()
        new.subscriptions = subscriptions.clone()
        new.giftedSubscriptions = giftedSubscriptions.clone()
        new.hosts = hosts.clone()
        new.rewards = rewards.clone()
        new.kickGifts = kickGifts.map { it.clone() }
        return new
    }

    fun disableAll() {
        subscriptions.enabled = false
        giftedSubscriptions.enabled = false
        hosts.enabled = false
        rewards.enabled = false
        for (kickGift in kickGifts) {
            kickGift.alert.enabled = false
        }
    }
}

@Serializable
enum class SettingsWidgetAlertsChatBotCommandImageType(val rawValue: String) {
    @SerialName("File") file("File");

    companion object {
        fun fromRawValue(rawValue: String): SettingsWidgetAlertsChatBotCommandImageType? =
            entries.firstOrNull { it.rawValue == rawValue }
    }
}

@Serializable
class SettingsWidgetAlertsChatBotCommand(
    @Contextual var id: UUID = UUID.randomUUID(),
    var name: String = "myname",
    var alert: SettingsWidgetAlertsAlert = SettingsWidgetAlertsAlert(),
    var imageType: SettingsWidgetAlertsChatBotCommandImageType =
        SettingsWidgetAlertsChatBotCommandImageType.file
) {
    fun clone(): SettingsWidgetAlertsChatBotCommand {
        val new = SettingsWidgetAlertsChatBotCommand()
        new.name = name
        new.alert = alert.clone()
        new.imageType = imageType
        return new
    }
}

@Serializable
class SettingsWidgetAlertsChatBot(
    var commands: List<SettingsWidgetAlertsChatBotCommand> = emptyList()
) {
    fun clone(): SettingsWidgetAlertsChatBot {
        val new = SettingsWidgetAlertsChatBot()
        for (command in commands) {
            new.commands = new.commands + command.clone()
        }
        return new
    }

    fun disableAll() {
        for (command in commands) {
            command.alert.enabled = false
        }
    }
}

@Serializable
class SettingsWidgetAlertsSpeechToTextString(
    @Contextual var id: UUID = UUID.randomUUID(),
    var string: String = "",
    var alert: SettingsWidgetAlertsAlert = SettingsWidgetAlertsAlert()
) {
    fun clone(): SettingsWidgetAlertsSpeechToTextString {
        val new = SettingsWidgetAlertsSpeechToTextString()
        new.id = id
        new.string = string
        new.alert = alert.clone()
        return new
    }
}

@Serializable
class SettingsWidgetAlertsSpeechToText(
    var strings: List<SettingsWidgetAlertsSpeechToTextString> = emptyList()
) {
    fun clone(): SettingsWidgetAlertsSpeechToText {
        val new = SettingsWidgetAlertsSpeechToText()
        for (string in strings) {
            new.strings = new.strings + string.clone()
        }
        return new
    }

    fun disableAll() {
        for (string in strings) {
            string.alert.enabled = false
        }
    }
}

@Serializable
class SettingsTtsMonster(
    var apiToken: String = ""
) {
    fun clone(): SettingsTtsMonster {
        val new = SettingsTtsMonster()
        new.apiToken = apiToken
        return new
    }
}

@Serializable
class SettingsWidgetAlerts(
    var twitch: SettingsWidgetAlertsTwitch = SettingsWidgetAlertsTwitch(),
    var kick: SettingsWidgetAlertsKick = SettingsWidgetAlertsKick(),
    var chatBot: SettingsWidgetAlertsChatBot = SettingsWidgetAlertsChatBot(),
    var speechToText: SettingsWidgetAlertsSpeechToText = SettingsWidgetAlertsSpeechToText(),
    var needsSubtitles: Boolean = false,
    var ai: SettingsOpenAi = SettingsOpenAi(personality = aiPersonality),
    var aiEnabled: Boolean = false,
    var ttsMonster: SettingsTtsMonster = SettingsTtsMonster()
) {
    companion object {
        private const val aiPersonality =
            "You are rude and gives insulting answers. Answer in a few sentences."
    }

    @Transient var quickButton: SettingsWidgetAlertsAlert = SettingsWidgetAlertsAlert()

    fun clone(): SettingsWidgetAlerts {
        val new = SettingsWidgetAlerts()
        new.twitch = twitch.clone()
        new.kick = kick.clone()
        new.chatBot = chatBot.clone()
        new.speechToText = speechToText.clone()
        new.needsSubtitles = needsSubtitles
        new.ai = ai.clone()
        new.aiEnabled = aiEnabled
        new.ttsMonster = ttsMonster.clone()
        return new
    }

    fun disableAll() {
        twitch.disableAll()
        kick.disableAll()
        chatBot.disableAll()
        speechToText.disableAll()
        quickButton.enabled = false
    }
}

@Serializable
enum class SettingsSceneSwitchTransition(val rawValue: String) {
    @SerialName("Blur") blur("Blur"),
    @SerialName("Freeze") freeze("Freeze"),
    @SerialName("Blur & zoom") blurAndZoom("Blur & zoom");

    override fun toString(): String = when (this) {
        blur -> localized("Blur")
        freeze -> localized("Freeze")
        blurAndZoom -> localized("Blur & zoom")
    }

    fun toVideoUnit(): SceneSwitchTransition =
        TODO("SceneSwitchTransition members are not available")

    companion object {
        fun fromRawValue(rawValue: String): SettingsSceneSwitchTransition? =
            entries.firstOrNull { it.rawValue == rawValue }
    }
}

@Serializable
data class SettingsSensitivity(
    val mouth: Double = 1.0,
    val eyes: Double = 1.0
)

@Serializable
enum class SettingsWidgetVTuberType(val rawValue: String) {
    @SerialName("VRM") vrm("VRM"),
    @SerialName("Live2D") live2D("Live2D");

    companion object {
        fun fromRawValue(rawValue: String): SettingsWidgetVTuberType? =
            entries.firstOrNull { it.rawValue == rawValue }
    }
}

@Serializable
class SettingsWidgetVTuber(
    @Contextual var id: UUID = UUID.randomUUID(),
    var type: SettingsWidgetVTuberType = SettingsWidgetVTuberType.vrm,
    var videoSource: SettingsVideoSource = SettingsVideoSource(),
    var cameraPositionY: Double = 1.37,
    var cameraFieldOfView: Double = 18.0,
    var modelName: String = "",
    var mirror: Boolean = false,
    var sensitivity: SettingsSensitivity = SettingsSensitivity(),
    var armsAngle: Double = 72.0
) {
    fun toCameraId(): SettingsCameraId = videoSource.toCameraId()

    fun updateCameraId(settingsCameraId: SettingsCameraId) {
        videoSource.updateCameraId(settingsCameraId)
    }
}

@Serializable
class SettingsWidgetPngTuber(
    @Contextual var id: UUID = UUID.randomUUID(),
    var videoSource: SettingsVideoSource = SettingsVideoSource(),
    var modelName: String = "",
    var mirror: Boolean = false,
    var sensitivity: SettingsSensitivity = SettingsSensitivity()
) {
    fun toCameraId(): SettingsCameraId = videoSource.toCameraId()

    fun updateCameraId(settingsCameraId: SettingsCameraId) {
        videoSource.updateCameraId(settingsCameraId)
    }
}

@Serializable
class SettingsWidgetSnapshot(
    @Contextual var id: UUID = UUID.randomUUID(),
    var showtime: Int = 5
)

@Serializable
class SettingsWidgetChat(
    @Contextual var id: UUID = UUID.randomUUID(),
    var fontSize: Float = 19.0f,
    var usernameColor: RgbColor = RgbColor(red = 255, green = 163, blue = 0),
    var messageColor: RgbColor = RgbColor(red = 255, green = 255, blue = 255),
    var backgroundColor: RgbColor = RgbColor(red = 0, green = 0, blue = 0),
    var backgroundColorEnabled: Boolean = false,
    var shadowColor: RgbColor = RgbColor(red = 0, green = 0, blue = 0),
    var shadowColorEnabled: Boolean = true,
    var boldUsername: Boolean = true,
    var boldMessage: Boolean = true,
    var badges: Boolean = true,
    var displayStyle: SettingsChatDisplayStyle = SettingsChatDisplayStyle.internationalNameAndUsername,
    var sharedChatIcons: Boolean = false,
    var height: Float = 1f,
    var maximumNumberOfMessages: Int = 5
) {
    @Transient var usernameColorColor: Color = usernameColor.color()
    @Transient var messageColorColor: Color = messageColor.color()
    @Transient var backgroundColorColor: Color = backgroundColor.color()
    @Transient var shadowColorColor: Color = shadowColor.color()
    @Transient val nicknames: SettingsChatNicknames = SettingsChatNicknames()

    fun update(other: SettingsWidgetChat) {
        fontSize = other.fontSize
        usernameColor = other.usernameColor
        usernameColorColor = other.usernameColorColor
        messageColor = other.messageColor
        messageColorColor = other.messageColorColor
        backgroundColor = other.backgroundColor
        backgroundColorColor = other.backgroundColorColor
        backgroundColorEnabled = other.backgroundColorEnabled
        shadowColor = other.shadowColor
        shadowColorColor = other.shadowColorColor
        shadowColorEnabled = other.shadowColorEnabled
        boldUsername = other.boldUsername
        boldMessage = other.boldMessage
        badges = other.badges
        displayStyle = other.displayStyle
        sharedChatIcons = other.sharedChatIcons
        height = other.height
        maximumNumberOfMessages = other.maximumNumberOfMessages
    }
}

@Serializable
class SettingsWidgetSlideshowSlide(
    @Contextual var id: UUID = UUID.randomUUID(),
    @Contextual var widgetId: UUID? = UUID.randomUUID(),
    var time: Int = 15
)

@Serializable
class SettingsWidgetSlideshow(
    @Contextual var id: UUID = UUID.randomUUID(),
    var slides: List<SettingsWidgetSlideshowSlide> = emptyList()
)

@Serializable
class SettingsWidgetWheelOfLuckOption(
    @Contextual var id: UUID = UUID.randomUUID(),
    var text: String = "",
    var weight: Int = 1
) {
    override fun equals(other: Any?): Boolean =
        other is SettingsWidgetWheelOfLuckOption && id == other.id

    override fun hashCode(): Int = id.hashCode()
}

@Serializable
class SettingsWidgetWheelOfLuck(
    var advanced: Boolean = false,
    var options: List<SettingsWidgetWheelOfLuckOption> = emptyList()
) {
    @Transient var totalWeight: Int = 1
    @Transient var text: String = ""

    init {
        updateTotalWeight()
        updateText()
    }

    fun updateTotalWeight() {
        totalWeight = maxOf(options.sumOf { it.weight }, 1)
    }

    fun updateText() {
        text = optionsToText()
    }

    fun shuffle() {
        options = options.shuffled()
        updateText()
    }

    fun optionsFromText(text: String) {
        val newOptions = mutableListOf<SettingsWidgetWheelOfLuckOption>()
        for (line in text.trim().split("\n")) {
            val option = SettingsWidgetWheelOfLuckOption()
            option.text = line.trim()
            newOptions.add(option)
        }
        if (newOptions.isEmpty()) {
            newOptions.add(SettingsWidgetWheelOfLuckOption())
        }
        options = newOptions
        updateTotalWeight()
    }

    private fun optionsToText(): String = options.map { it.text }.joinToString("\n")
}

@Serializable
data class SettingsBingoCardSquare(
    val text: String,
    val checked: Boolean
) {
    @Transient val id: UUID = UUID.randomUUID()
}

@Serializable
class SettingsWidgetBingoCard(
    var backgroundColor: RgbColor = baseBackgroundColor,
    var foregroundColor: RgbColor = baseForegroundColor,
    var squares: List<SettingsBingoCardSquare> = emptyList()
) {
    companion object {
        val baseBackgroundColor: RgbColor = RgbColor.black.withOpacity(0.75)
        val baseForegroundColor: RgbColor = RgbColor.white
    }

    @Transient var backgroundColorColor: Color = baseBackgroundColor.color()
    @Transient var foregroundColorColor: Color = baseForegroundColor.color()
    @Transient var squaresText: String = ""

    fun squaresTextChanged() {
        val lines = squaresText.split("\n")
        val newSquares = mutableListOf<SettingsBingoCardSquare>()
        for (index in lines.indices) {
            val checked = squares.getOrNull(index)?.checked ?: false
            newSquares.add(SettingsBingoCardSquare(text = lines[index].trim(), checked = checked))
        }
        squares = newSquares
    }

    fun uncheckAll() {
        squares = squares.map { SettingsBingoCardSquare(text = it.text, checked = false) }
    }

    fun size(): Int = when {
        squares.size <= 4 -> 2
        squares.size <= 9 -> 3
        squares.size <= 16 -> 4
        else -> 5
    }
}

@Serializable
enum class PomodoroPhase(val rawValue: String) {
    @SerialName("Focus") focus("Focus"),
    @SerialName("Break") shortBreak("Break");

    companion object {
        fun fromRawValue(rawValue: String): PomodoroPhase? =
            entries.firstOrNull { it.rawValue == rawValue }
    }
}

@Serializable
enum class PomodoroFocusIcon(val rawValue: String) {
    @SerialName("sun.max") sun("sun.max"),
    @SerialName("bolt.circle") bolt("bolt.circle"),
    @SerialName("graduationcap") cap("graduationcap"),
    @SerialName("text.book.closed") book("text.book.closed"),
    @SerialName("arrowtriangle.right.circle") play("arrowtriangle.right.circle"),
    @SerialName("brain.head.profile") brain("brain.head.profile"),
    @SerialName("pencil") pencil("pencil"),
    @SerialName("flame") flame("flame"),
    @SerialName("timer") timer("timer"),
    @SerialName("target") target("target");

    override fun toString(): String = when (this) {
        sun -> localized("Sun")
        bolt -> localized("Bolt")
        cap -> localized("Cap")
        book -> localized("Book")
        play -> localized("Play")
        brain -> localized("Brain")
        pencil -> localized("Pencil")
        flame -> localized("Flame")
        timer -> localized("Timer")
        target -> localized("Target")
    }

    companion object {
        fun fromRawValue(rawValue: String): PomodoroFocusIcon? =
            entries.firstOrNull { it.rawValue == rawValue }
    }
}

@Serializable
enum class PomodoroBreakIcon(val rawValue: String) {
    @SerialName("cup.and.saucer") cup("cup.and.saucer"),
    @SerialName("figure.dance") dance("figure.dance"),
    @SerialName("figure.walk") walk("figure.walk"),
    @SerialName("music.note") music("music.note"),
    @SerialName("leaf") leaf("leaf"),
    @SerialName("gamecontroller") game("gamecontroller"),
    @SerialName("fork.knife") fork("fork.knife");

    override fun toString(): String = when (this) {
        cup -> localized("Cup")
        dance -> localized("Dance")
        walk -> localized("Walk")
        music -> localized("Music")
        leaf -> localized("Leaf")
        game -> localized("Controller")
        fork -> localized("Fork and knife")
    }

    companion object {
        fun fromRawValue(rawValue: String): PomodoroBreakIcon? =
            entries.firstOrNull { it.rawValue == rawValue }
    }
}

@Serializable
class SettingsWidgetPomodoroTimer(
    var focusDuration: Int = 30,
    var breakDuration: Int = 5,
    var width: Double = 1.6,
    var focusName: String = "Focus",
    var breakName: String = "Break",
    var focusIcon: PomodoroFocusIcon = PomodoroFocusIcon.sun,
    var breakIcon: PomodoroBreakIcon = PomodoroBreakIcon.cup,
    var backgroundColor: RgbColor = baseBackgroundColor,
    var foregroundColor: RgbColor = baseForegroundColor,
    var focusColor: RgbColor = baseFocusColor,
    var breakColor: RgbColor = baseBreakColor,
    @Contextual var focusToBreakSoundId: UUID? = null,
    @Contextual var breakToFocusSoundId: UUID? = null,
    var focusToBreakChatMessage: String = "",
    var breakToFocusChatMessage: String = ""
) {
    companion object {
        val baseBackgroundColor: RgbColor = RgbColor.black.withOpacity(0.75)
        val baseForegroundColor: RgbColor = RgbColor.white
        val baseFocusColor: RgbColor = RgbColor(red = 122, green = 181, blue = 255)
        val baseBreakColor: RgbColor = RgbColor(red = 103, green = 208, blue = 69)
    }

    @Transient var backgroundColorColor: Color = baseBackgroundColor.color()
    @Transient var foregroundColorColor: Color = baseForegroundColor.color()
    @Transient var focusColorColor: Color = baseFocusColor.color()
    @Transient var breakColorColor: Color = baseBreakColor.color()
    @Transient var isRunning: Boolean = false
    @Transient var phase: PomodoroPhase = PomodoroPhase.focus
    @Transient var secondsRemaining: Int = 30 * 60
    @Transient var onPhaseChanged: ((PomodoroPhase) -> Unit)? = null
    @Transient private var timer = MainTimer()

    fun start() {
        if (isRunning) {
            return
        }
        isRunning = true
        timer.startPeriodic(interval = 1.0) { tick() }
    }

    fun pause() {
        isRunning = false
        timer.stop()
    }

    fun reset() {
        pause()
        phase = PomodoroPhase.focus
        secondsRemaining = focusDuration * 60
    }

    fun totalSecondsForCurrentPhase(): Int = when (phase) {
        PomodoroPhase.focus -> focusDuration * 60
        PomodoroPhase.shortBreak -> breakDuration * 60
    }

    private fun tick() {
        if (secondsRemaining > 0) {
            secondsRemaining -= 1
        } else {
            advancePhase()
        }
    }

    fun advancePhase() {
        when (phase) {
            PomodoroPhase.focus -> {
                phase = PomodoroPhase.shortBreak
                secondsRemaining = breakDuration * 60
            }
            PomodoroPhase.shortBreak -> {
                phase = PomodoroPhase.focus
                secondsRemaining = focusDuration * 60
            }
        }
        onPhaseChanged?.invoke(phase)
    }
}

@Serializable
class SettingsWidgetChatEmoteCombo(
    var minimumCombo: Int = 3,
    var resetAfter: Int = 5
)

@Serializable
class SettingsWidget(
    override var name: String = baseName,
    @Contextual var id: UUID = UUID.randomUUID(),
    var type: SettingsWidgetType = SettingsWidgetType.text,
    var text: SettingsWidgetText = SettingsWidgetText(),
    var browser: SettingsWidgetBrowser = SettingsWidgetBrowser(),
    var crop: SettingsWidgetCrop = SettingsWidgetCrop(),
    var map: SettingsWidgetMap = SettingsWidgetMap(),
    var scene: SettingsWidgetScene = SettingsWidgetScene(),
    var qrCode: SettingsWidgetQrCode = SettingsWidgetQrCode(),
    var alerts: SettingsWidgetAlerts = SettingsWidgetAlerts(),
    var videoSource: SettingsWidgetVideoSource = SettingsWidgetVideoSource(),
    var scoreboard: SettingsWidgetScoreboard = SettingsWidgetScoreboard(),
    var vTuber: SettingsWidgetVTuber = SettingsWidgetVTuber(),
    var pngTuber: SettingsWidgetPngTuber = SettingsWidgetPngTuber(),
    var snapshot: SettingsWidgetSnapshot = SettingsWidgetSnapshot(),
    var chat: SettingsWidgetChat = SettingsWidgetChat(),
    var chatEmoteCombo: SettingsWidgetChatEmoteCombo = SettingsWidgetChatEmoteCombo(),
    var slideshow: SettingsWidgetSlideshow = SettingsWidgetSlideshow(),
    var wheelOfLuck: SettingsWidgetWheelOfLuck = SettingsWidgetWheelOfLuck(),
    var bingoCard: SettingsWidgetBingoCard = SettingsWidgetBingoCard(),
    var pomodoroTimer: SettingsWidgetPomodoroTimer = SettingsWidgetPomodoroTimer(),
    var enabled: Boolean = true,
    var effects: List<SettingsVideoEffect> = emptyList()
) : Named {
    companion object {
        val baseName: String = localized("My widget")
    }

    override fun equals(other: Any?): Boolean = other is SettingsWidget && id == other.id

    override fun hashCode(): Int = id.hashCode()

    private fun migrateFromOlderVersions() {
        if (type == SettingsWidgetType.videoSource &&
            effects.none { it.type == SettingsVideoEffectType.shape }
        ) {
            val shape = SettingsVideoEffectShape()
            shape.cornerRadius = 0f
            var updated = false
            if (videoSource.videoSource.cameraPosition != SettingsSceneCameraPosition.none) {
                updated = updated
            }
            if (videoSource.cornerRadius != 0f || videoSource.borderWidth != 0.0) {
                shape.cornerRadius = videoSource.cornerRadius
                shape.borderWidth = videoSource.borderWidth
                shape.borderColor = videoSource.borderColor
                shape.borderColorColor = videoSource.borderColorColor
                updated = true
                videoSource.cornerRadius = 0f
                videoSource.borderWidth = 0.0
            }
            if (videoSource.cropEnabled && !videoSource.trackFaceEnabled) {
                shape.cropEnabled = videoSource.cropEnabled
                shape.cropX = videoSource.cropX
                shape.cropY = videoSource.cropY
                shape.cropWidth = videoSource.cropWidth
                shape.cropHeight = videoSource.cropHeight
                updated = true
                videoSource.cropEnabled = false
            }
            if (updated) {
                val effect = SettingsVideoEffect()
                effect.type = SettingsVideoEffectType.shape
                effect.shape = shape
                effects = effects + effect
            }
        }
    }

    fun getEffects(model: Model): List<VideoEffect> =
        effects.filter { it.enabled }.map { it.getEffect(model) }

    fun image(): String = type.image()

    fun hasPosition(): Boolean = type in listOf(
        SettingsWidgetType.image,
        SettingsWidgetType.browser,
        SettingsWidgetType.text,
        SettingsWidgetType.crop,
        SettingsWidgetType.map,
        SettingsWidgetType.qrCode,
        SettingsWidgetType.alerts,
        SettingsWidgetType.videoSource,
        SettingsWidgetType.vTuber,
        SettingsWidgetType.pngTuber,
        SettingsWidgetType.snapshot,
        SettingsWidgetType.chat,
        SettingsWidgetType.chatEmoteCombo,
        SettingsWidgetType.slideshow,
        SettingsWidgetType.scoreboard,
        SettingsWidgetType.wheelOfLuck,
        SettingsWidgetType.bingoCard,
        SettingsWidgetType.pomodoroTimer
    )

    fun hasSize(): Boolean = type in listOf(
        SettingsWidgetType.image,
        SettingsWidgetType.browser,
        SettingsWidgetType.crop,
        SettingsWidgetType.map,
        SettingsWidgetType.qrCode,
        SettingsWidgetType.videoSource,
        SettingsWidgetType.vTuber,
        SettingsWidgetType.pngTuber,
        SettingsWidgetType.snapshot,
        SettingsWidgetType.chatEmoteCombo,
        SettingsWidgetType.slideshow,
        SettingsWidgetType.bingoCard,
        SettingsWidgetType.pomodoroTimer,
        SettingsWidgetType.scoreboard
    )

    fun hasAlignment(): Boolean = type in listOf(
        SettingsWidgetType.image,
        SettingsWidgetType.browser,
        SettingsWidgetType.text,
        SettingsWidgetType.crop,
        SettingsWidgetType.map,
        SettingsWidgetType.qrCode,
        SettingsWidgetType.alerts,
        SettingsWidgetType.videoSource,
        SettingsWidgetType.vTuber,
        SettingsWidgetType.pngTuber,
        SettingsWidgetType.snapshot,
        SettingsWidgetType.chat,
        SettingsWidgetType.chatEmoteCombo,
        SettingsWidgetType.slideshow,
        SettingsWidgetType.scoreboard,
        SettingsWidgetType.wheelOfLuck,
        SettingsWidgetType.bingoCard,
        SettingsWidgetType.pomodoroTimer
    )
}

@Serializable
class SettingsWidgetLayout(
    var x: Double = 0.0,
    var xString: String = "0.0",
    var y: Double = 0.0,
    var yString: String = "0.0",
    var size: Double = 100.0,
    var sizeString: String = "100.0",
    var alignment: SettingsAlignment = SettingsAlignment.topLeft,
    var positioningLock: Boolean = false
) {
    fun updateXString() {
        xString = x.toString()
    }

    fun updateYString() {
        yString = y.toString()
    }

    fun updateSizeString() {
        sizeString = size.toString()
    }

    fun extent(): RectF = RectF(x.toFloat(), y.toFloat(), (x + size).toFloat(), (y + size).toFloat())
}

@Serializable
class SettingsSceneWidget(
    @Contextual var widgetId: UUID = UUID.randomUUID(),
    @Contextual var id: UUID = UUID.randomUUID(),
    var layout: SettingsWidgetLayout = SettingsWidgetLayout(),
    @SerialName("width") var width2: Double = 100.0,
    @SerialName("height") var height2: Double = 100.0,
    var migrated: Boolean = false,
    var migrated2: Boolean = false
) {
    override fun equals(other: Any?): Boolean = other is SettingsSceneWidget && id == other.id

    override fun hashCode(): Int = id.hashCode()

    fun clone(): SettingsSceneWidget {
        val new = SettingsSceneWidget(widgetId = widgetId)
        new.layout = layout
        new.migrated = migrated
        new.migrated2 = migrated2
        return new
    }
}

@Serializable
enum class SettingsSceneCameraPosition(val rawValue: String) {
    @SerialName("Back") back("Back"),
    @SerialName("Front") front("Front"),
    @SerialName("RTMP") rtmp("RTMP"),
    @SerialName("External") `external`("External"),
    @SerialName("SRT(LA)") srtla("SRT(LA)"),
    @SerialName("SRT client") srtClient("SRT client"),
    @SerialName("RIST") rist("RIST"),
    @SerialName("RTSP") rtsp("RTSP"),
    @SerialName("WHIP") whip("WHIP"),
    @SerialName("WHEP") whep("WHEP"),
    @SerialName("Media player") mediaPlayer("Media player"),
    @SerialName("Screen capture") screenCapture("Screen capture"),
    @SerialName("Back triple") backTripleLowEnergy("Back triple"),
    @SerialName("Back dual") backDualLowEnergy("Back dual"),
    @SerialName("Back wide dual") backWideDualLowEnergy("Back wide dual"),
    @SerialName("None") none("None");

    fun isBuiltin(): Boolean = this in builtinCameraPositions

    companion object {
        fun fromRawValue(rawValue: String): SettingsSceneCameraPosition =
            entries.firstOrNull { it.rawValue == rawValue } ?: back
    }
}

private val builtinCameraPositions: List<SettingsSceneCameraPosition> = listOf(
    SettingsSceneCameraPosition.back,
    SettingsSceneCameraPosition.front,
    SettingsSceneCameraPosition.backTripleLowEnergy,
    SettingsSceneCameraPosition.backDualLowEnergy,
    SettingsSceneCameraPosition.backWideDualLowEnergy
)

@Serializable
data class SettingsVideoSource(
    var cameraPosition: SettingsSceneCameraPosition = SettingsSceneCameraPosition.none,
    var backCameraId: CameraId = bestBackCameraId,
    var frontCameraId: CameraId = bestFrontCameraId,
    @Contextual var rtmpCameraId: UUID = UUID.randomUUID(),
    @Contextual var srtlaCameraId: UUID = UUID.randomUUID(),
    @Contextual var srtClientCameraId: UUID = UUID.randomUUID(),
    @Contextual var ristCameraId: UUID = UUID.randomUUID(),
    @Contextual var rtspCameraId: UUID = UUID.randomUUID(),
    @Contextual var whipCameraId: UUID = UUID.randomUUID(),
    @Contextual var whepCameraId: UUID = UUID.randomUUID(),
    @Contextual var mediaPlayerCameraId: UUID = UUID.randomUUID(),
    var externalCameraId: CameraId = "",
    var externalCameraName: String = ""
) {
    fun toCameraId(): SettingsCameraId = when (cameraPosition) {
        SettingsSceneCameraPosition.back -> SettingsCameraId.Back(backCameraId)
        SettingsSceneCameraPosition.front -> SettingsCameraId.Front(frontCameraId)
        SettingsSceneCameraPosition.rtmp -> SettingsCameraId.Rtmp(rtmpCameraId)
        SettingsSceneCameraPosition.`external` ->
            SettingsCameraId.External(externalCameraId, externalCameraName)
        SettingsSceneCameraPosition.srtla -> SettingsCameraId.Srtla(srtlaCameraId)
        SettingsSceneCameraPosition.srtClient -> SettingsCameraId.Srt(srtClientCameraId)
        SettingsSceneCameraPosition.rist -> SettingsCameraId.Rist(ristCameraId)
        SettingsSceneCameraPosition.rtsp -> SettingsCameraId.Rtsp(rtspCameraId)
        SettingsSceneCameraPosition.whip -> SettingsCameraId.Whip(whipCameraId)
        SettingsSceneCameraPosition.whep -> SettingsCameraId.Whep(whepCameraId)
        SettingsSceneCameraPosition.mediaPlayer -> SettingsCameraId.MediaPlayer(mediaPlayerCameraId)
        SettingsSceneCameraPosition.screenCapture -> SettingsCameraId.ScreenCapture
        SettingsSceneCameraPosition.backTripleLowEnergy -> SettingsCameraId.BackTripleLowEnergy
        SettingsSceneCameraPosition.backDualLowEnergy -> SettingsCameraId.BackDualLowEnergy
        SettingsSceneCameraPosition.backWideDualLowEnergy -> SettingsCameraId.BackWideDualLowEnergy
        SettingsSceneCameraPosition.none -> SettingsCameraId.None
    }

    fun updateCameraId(settingsCameraId: SettingsCameraId) {
        when (settingsCameraId) {
            is SettingsCameraId.Back -> {
                cameraPosition = SettingsSceneCameraPosition.back
                backCameraId = settingsCameraId.id
            }
            is SettingsCameraId.Front -> {
                cameraPosition = SettingsSceneCameraPosition.front
                frontCameraId = settingsCameraId.id
            }
            is SettingsCameraId.Rtmp -> {
                cameraPosition = SettingsSceneCameraPosition.rtmp
                rtmpCameraId = settingsCameraId.id
            }
            is SettingsCameraId.Srtla -> {
                cameraPosition = SettingsSceneCameraPosition.srtla
                srtlaCameraId = settingsCameraId.id
            }
            is SettingsCameraId.Srt -> {
                cameraPosition = SettingsSceneCameraPosition.srtClient
                srtClientCameraId = settingsCameraId.id
            }
            is SettingsCameraId.Rist -> {
                cameraPosition = SettingsSceneCameraPosition.rist
                ristCameraId = settingsCameraId.id
            }
            is SettingsCameraId.Rtsp -> {
                cameraPosition = SettingsSceneCameraPosition.rtsp
                rtspCameraId = settingsCameraId.id
            }
            is SettingsCameraId.Whip -> {
                cameraPosition = SettingsSceneCameraPosition.whip
                whipCameraId = settingsCameraId.id
            }
            is SettingsCameraId.Whep -> {
                cameraPosition = SettingsSceneCameraPosition.whep
                whepCameraId = settingsCameraId.id
            }
            is SettingsCameraId.MediaPlayer -> {
                cameraPosition = SettingsSceneCameraPosition.mediaPlayer
                mediaPlayerCameraId = settingsCameraId.id
            }
            is SettingsCameraId.External -> {
                cameraPosition = SettingsSceneCameraPosition.`external`
                externalCameraId = settingsCameraId.id
                externalCameraName = settingsCameraId.name
            }
            SettingsCameraId.ScreenCapture -> cameraPosition = SettingsSceneCameraPosition.screenCapture
            SettingsCameraId.BackTripleLowEnergy ->
                cameraPosition = SettingsSceneCameraPosition.backTripleLowEnergy
            SettingsCameraId.BackDualLowEnergy ->
                cameraPosition = SettingsSceneCameraPosition.backDualLowEnergy
            SettingsCameraId.BackWideDualLowEnergy ->
                cameraPosition = SettingsSceneCameraPosition.backWideDualLowEnergy
            SettingsCameraId.None -> cameraPosition = SettingsSceneCameraPosition.none
        }
    }

    fun isCaptureDevice(): Boolean = when (cameraPosition) {
        SettingsSceneCameraPosition.back -> true
        SettingsSceneCameraPosition.backWideDualLowEnergy -> true
        SettingsSceneCameraPosition.backDualLowEnergy -> true
        SettingsSceneCameraPosition.backTripleLowEnergy -> true
        SettingsSceneCameraPosition.front -> true
        SettingsSceneCameraPosition.`external` -> true
        else -> false
    }

    fun getCaptureDeviceCameraId(): CameraId? = when (cameraPosition) {
        SettingsSceneCameraPosition.back -> backCameraId
        SettingsSceneCameraPosition.front -> frontCameraId
        SettingsSceneCameraPosition.`external` -> externalCameraId
        else -> null
    }

    fun isNetwork(cameraId: UUID): Boolean = when (cameraPosition) {
        SettingsSceneCameraPosition.rtmp -> cameraId == rtmpCameraId
        SettingsSceneCameraPosition.srtla -> cameraId == srtlaCameraId
        SettingsSceneCameraPosition.srtClient -> cameraId == srtClientCameraId
        SettingsSceneCameraPosition.rist -> cameraId == ristCameraId
        SettingsSceneCameraPosition.rtsp -> cameraId == rtspCameraId
        SettingsSceneCameraPosition.whip -> cameraId == whipCameraId
        SettingsSceneCameraPosition.whep -> cameraId == whepCameraId
        else -> false
    }
}

@Serializable
class SettingsWidgetVideoSource(
    var cornerRadius: Float = 0f,
    var videoSource: SettingsVideoSource = SettingsVideoSource(),
    var cropEnabled: Boolean = false,
    var cropX: Double = 0.25,
    var cropY: Double = 0.0,
    var cropWidth: Double = 0.5,
    var cropHeight: Double = 1.0,
    var rotation: Double = 0.0,
    var trackFaceEnabled: Boolean = false,
    var trackFaceZoom: Double = 0.75,
    var mirror: Boolean = false,
    var borderWidth: Double = 0.0,
    var borderColor: RgbColor = RgbColor(red = 0, green = 0, blue = 0)
) {
    @Transient var borderColorColor: Color = borderColor.color()

    fun toEffectSettings(): VideoSourceEffectSettings = VideoSourceEffectSettings(
        rotation = rotation,
        trackFaceEnabled = trackFaceEnabled,
        trackFaceZoom = 1.5 + (1 - trackFaceZoom) * 4,
        mirror = mirror
    )

    fun toCameraId(): SettingsCameraId = videoSource.toCameraId()

    fun updateCameraId(settingsCameraId: SettingsCameraId) {
        videoSource.updateCameraId(settingsCameraId)
    }
}

@Serializable
enum class SettingsWidgetScoreboardSport {
    generic,
    padel,
    golf,
    golfFullScorecard,
    basketball,
    generic2,
    genericSets,
    hockey,
    football,
    tennis,
    volleyball;

    override fun toString(): String = when (this) {
        generic -> localized("Generic")
        padel -> localized("Padel")
        golf -> localized("Golf")
        golfFullScorecard -> localized("Golf full scorecard")
        basketball -> localized("Basketball")
        generic2 -> localized("Generic 2")
        genericSets -> localized("Generic sets")
        hockey -> localized("Hockey")
        football -> localized("Football")
        tennis -> localized("Tennis")
        volleyball -> localized("Volleyball")
    }

    companion object {
        fun fromRawValue(rawValue: String): SettingsWidgetScoreboardSport? =
            entries.firstOrNull { it.name == rawValue }
    }
}

@Serializable
enum class SettingsWidgetScoreboardLayout {
    stacked,
    stackedInline,
    sideBySide,
    stackHistory;

    override fun toString(): String = when (this) {
        stacked -> localized("Stacked")
        stackedInline -> localized("Stacked inline")
        sideBySide -> localized("Side by side")
        stackHistory -> localized("Stack history")
    }

    companion object {
        fun fromRawValue(rawValue: String): SettingsWidgetScoreboardLayout? =
            entries.firstOrNull { it.name == rawValue }
    }
}

@Serializable
class SettingsWidgetScoreboardPlayer(
    @Contextual var id: UUID = UUID.randomUUID(),
    override var name: String = baseName
) : Named {
    companion object {
        val baseName: String = localized("🇸🇪 Moblin")
    }
}

@Serializable
class SettingsWidgetScoreboardScore(
    var home: Int = 0,
    var away: Int = 0
)

@Serializable
enum class SettingsWidgetPadelScoreboardGameType(val rawValue: String) {
    @SerialName("Double") doubles("Double"),
    @SerialName("Single") singles("Single");

    override fun toString(): String = when (this) {
        doubles -> localized("Doubles")
        singles -> localized("Singles")
    }

    companion object {
        fun fromRawValue(rawValue: String): SettingsWidgetPadelScoreboardGameType? =
            entries.firstOrNull { it.rawValue == rawValue }
    }
}

@Serializable
enum class SettingsWidgetScoreboardScoreIncrement {
    home,
    away;

    companion object {
        fun fromRawValue(rawValue: String): SettingsWidgetScoreboardScoreIncrement? =
            entries.firstOrNull { it.name == rawValue }
    }
}

@Serializable
class SettingsWidgetPadelScoreboard(
    var type: SettingsWidgetPadelScoreboardGameType = SettingsWidgetPadelScoreboardGameType.doubles,
    @Contextual var homePlayer1: UUID = UUID.randomUUID(),
    @Contextual var homePlayer2: UUID = UUID.randomUUID(),
    @Contextual var awayPlayer1: UUID = UUID.randomUUID(),
    @Contextual var awayPlayer2: UUID = UUID.randomUUID(),
    var score: List<SettingsWidgetScoreboardScore> = listOf(SettingsWidgetScoreboardScore())
) {
    @Transient var scoreChanges: List<SettingsWidgetScoreboardScoreIncrement> = emptyList()
}

@Serializable
class SettingsWidgetGolfScoreboardPlayer(
    override var name: String = "Player",
    @Contextual var id: UUID = UUID.randomUUID(),
    var scores: List<Int> = defaultScores,
    var color: RgbColor = RgbColor.white
) : Named {
    companion object {
        val defaultScores: List<Int> = List(18) { -1 }
    }

    fun totalRelativeToPar(pars: List<Int>, numberOfHoles: Int): Int {
        var total = 0
        for (holeIndex in 0 until minOf(numberOfHoles, minOf(pars.size, scores.size))) {
            val score = scores[holeIndex]
            if (score != -1) {
                total += score - pars[holeIndex]
            }
        }
        return total
    }

    fun totalStrokes(numberOfHoles: Int): Int {
        var total = 0
        for (holeIndex in 0 until minOf(numberOfHoles, scores.size)) {
            val score = scores[holeIndex]
            if (score >= 0) {
                total += score
            }
        }
        return total
    }

    fun holesPlayed(numHoles: Int): Int = scores.take(numHoles).count { it != -1 }
}

@Serializable
class SettingsWidgetGolfScoreboard(
    @SerialName("eventName") var title: String = defaultTitle,
    var numberOfHoles: Int = 18,
    var currentHole: Int = 0,
    var pars: List<Int> = defaultPars,
    var players: List<SettingsWidgetGolfScoreboardPlayer> = defaultPlayers,
    var playerColors: Boolean = false,
    var showPars: Boolean = true
) {
    companion object {
        const val defaultTitle = "⛳ Masters 2026"
        val defaultPars: List<Int> = listOf(4, 4, 3, 4, 5, 4, 3, 4, 4, 4, 4, 3, 5, 4, 4, 3, 4, 5)
        val defaultPlayers: List<SettingsWidgetGolfScoreboardPlayer> = listOf(
            SettingsWidgetGolfScoreboardPlayer(name = "Player 1"),
            SettingsWidgetGolfScoreboardPlayer(name = "Player 2")
        )
    }

    fun setPars(pars: List<Int>) {
        val newPars = pars.toMutableList()
        while (newPars.size < 18) {
            newPars.add(defaultPars[newPars.size])
        }
        this.pars = newPars
    }
}

@Serializable
enum class SettingsWidgetGenericScoreboardClockDirection {
    up,
    down;

    override fun toString(): String = when (this) {
        up -> localized("Up")
        down -> localized("Down")
    }

    companion object {
        fun fromRawValue(rawValue: String): SettingsWidgetGenericScoreboardClockDirection? =
            entries.firstOrNull { it.name == rawValue }
    }
}

@Serializable
class SettingsWidgetGenericScoreboard(
    var home: String = baseName,
    var away: String = baseName,
    var title: String = baseTitle,
    var period: String = "1",
    var clock: SettingsWidgetScoreboardClock = SettingsWidgetScoreboardClock()
) {
    companion object {
        val baseName: String = localized("🇸🇪 Moblin")
        const val baseTitle = "⚽️"
    }

    @Transient var score: SettingsWidgetScoreboardScore = SettingsWidgetScoreboardScore()
    @Transient var scoreChanges: List<SettingsWidgetScoreboardScoreIncrement> = emptyList()
}

@Serializable
class SettingsWidgetModularScoreboardTeam(
    var name: String = "",
    var textColor: RgbColor = RgbColor.black,
    var backgroundColor: RgbColor = RgbColor.black
) {
    @Transient var textColorColor: Color = Color.Transparent
    @Transient var backgroundColorColor: Color = Color.Transparent

    init {
        loadColors()
    }

    fun setHexColors(textColor: String, backgroundColor: String) {
        RgbColor.fromHex(textColor)?.let { this.textColor = it }
        RgbColor.fromHex(backgroundColor)?.let { this.backgroundColor = it }
        loadColors()
    }

    fun loadColors() {
        textColorColor = textColor.color()
        backgroundColorColor = backgroundColor.color()
    }
}

@Serializable
class SettingsWidgetScoreboardClock(
    var maximum: Int = 45,
    var direction: SettingsWidgetGenericScoreboardClockDirection =
        SettingsWidgetGenericScoreboardClockDirection.up
) {
    @Transient var minutes: Int = 0
    @Transient var seconds: Int = 0
    @Transient var isStopped: Boolean = true

    init {
        reset()
    }

    fun format(): String = if (seconds < 10) "$minutes:0$seconds" else "$minutes:$seconds"

    fun tick() {
        when (direction) {
            SettingsWidgetGenericScoreboardClockDirection.up -> {
                if (minutes != maximum) {
                    if (seconds == 59) {
                        seconds = 0
                        minutes += 1
                    } else {
                        seconds += 1
                    }
                }
            }
            SettingsWidgetGenericScoreboardClockDirection.down -> {
                if (minutes != 0 || seconds != 0) {
                    if (seconds == 0) {
                        seconds = 59
                        minutes -= 1
                    } else {
                        seconds -= 1
                    }
                }
            }
        }
    }

    fun reset() {
        when (direction) {
            SettingsWidgetGenericScoreboardClockDirection.up -> {
                minutes = 0
                seconds = 0
            }
            SettingsWidgetGenericScoreboardClockDirection.down -> {
                minutes = maximum
                seconds = 0
            }
        }
    }
}

@Serializable
class SettingsWidgetModularScoreboard(
    var home: SettingsWidgetModularScoreboardTeam = createHomeTeam(),
    var away: SettingsWidgetModularScoreboardTeam = createAwayTeam(),
    var title: String = baseTitle,
    var period: String = "1",
    var infoBoxText: String = "",
    var clock: SettingsWidgetScoreboardClock = SettingsWidgetScoreboardClock(),
    var layout: SettingsWidgetScoreboardLayout = SettingsWidgetScoreboardLayout.stacked,
    var width: Float = 350f,
    var rowHeight: Float = 45f,
    var isBold: Boolean = true,
    var showTitle: Boolean = false,
    var showMoreStats: Boolean = false,
    var showGlobalStatsBlock: Boolean = false,
    var showClock: Boolean = true
) {
    companion object {
        val baseName: String = localized("🇸🇪 Moblin")
        const val baseTitle = "⚽️"
        val baseHomeTextColor: RgbColor = RgbColor.white
        val baseHomeBackgroundColor: RgbColor = RgbColor(red = 11, green = 16, blue = 172)
        val baseAwayTextColor: RgbColor = RgbColor.white
        val baseAwayBackgroundColor: RgbColor = RgbColor(red = 220, green = 38, blue = 38)

        private fun createHomeTeam(): SettingsWidgetModularScoreboardTeam =
            SettingsWidgetModularScoreboardTeam(
                name = baseName,
                textColor = baseHomeTextColor,
                backgroundColor = baseHomeBackgroundColor
            )

        private fun createAwayTeam(): SettingsWidgetModularScoreboardTeam =
            SettingsWidgetModularScoreboardTeam(
                name = baseName,
                textColor = baseAwayTextColor,
                backgroundColor = baseAwayBackgroundColor
            )
    }

    @Transient var score: SettingsWidgetScoreboardScore = SettingsWidgetScoreboardScore()
    @Transient var scoreChanges: List<SettingsWidgetScoreboardScoreIncrement> = emptyList()
    @Transient var config: RemoteControlScoreboardMatchConfig? = null

    fun fontSize(): Double = rowHeight * 0.8

    fun setLayout(name: String) {
        layout = when (name) {
            "sideBySide" -> SettingsWidgetScoreboardLayout.sideBySide
            "stackHistory" -> SettingsWidgetScoreboardLayout.stackHistory
            "stackedInline" -> SettingsWidgetScoreboardLayout.stackedInline
            else -> SettingsWidgetScoreboardLayout.stacked
        }
    }
}

@Serializable
class SettingsWidgetScoreboard(
    @SerialName("type") var sport: SettingsWidgetScoreboardSport = SettingsWidgetScoreboardSport.generic,
    var textColor: RgbColor = baseTextColor,
    var primaryBackgroundColor: RgbColor = basePrimaryBackgroundColor,
    var secondaryBackgroundColor: RgbColor = baseSecondaryBackgroundColor,
    var padel: SettingsWidgetPadelScoreboard = SettingsWidgetPadelScoreboard(),
    var golf: SettingsWidgetGolfScoreboard = SettingsWidgetGolfScoreboard(),
    var generic: SettingsWidgetGenericScoreboard = SettingsWidgetGenericScoreboard(),
    var modular: SettingsWidgetModularScoreboard = SettingsWidgetModularScoreboard()
) {
    companion object {
        val baseTextColor: RgbColor = RgbColor.white
        val basePrimaryBackgroundColor: RgbColor = RgbColor(red = 0x0B, green = 0x10, blue = 0xAC)
        val baseSecondaryBackgroundColor: RgbColor = RgbColor(red = 0, green = 3, blue = 0x5B)
    }

    @Transient var textColorColor: Color = textColor.color()
    @Transient var primaryBackgroundColorColor: Color = primaryBackgroundColor.color()
    @Transient var secondaryBackgroundColorColor: Color = secondaryBackgroundColor.color()

    init {
        loadColors()
    }

    fun resetColors() {
        textColor = baseTextColor
        primaryBackgroundColor = basePrimaryBackgroundColor
        secondaryBackgroundColor = baseSecondaryBackgroundColor
        loadColors()
    }

    fun loadColors() {
        textColorColor = textColor.color()
        primaryBackgroundColorColor = primaryBackgroundColor.color()
        secondaryBackgroundColorColor = secondaryBackgroundColor.color()
    }

    fun setModularSport(sportId: String) {
        if (!isModularSport()) {
            return
        }
        when (sportId) {
            "basketball" -> sport = SettingsWidgetScoreboardSport.basketball
            "generic" -> sport = SettingsWidgetScoreboardSport.generic2
            "generic sets" -> sport = SettingsWidgetScoreboardSport.genericSets
            "hockey" -> sport = SettingsWidgetScoreboardSport.hockey
            "football" -> sport = SettingsWidgetScoreboardSport.football
            "tennis" -> sport = SettingsWidgetScoreboardSport.tennis
            "volleyball" -> sport = SettingsWidgetScoreboardSport.volleyball
            else -> {}
        }
    }

    private fun isModularSport(): Boolean = sport == SettingsWidgetScoreboardSport.basketball ||
        sport == SettingsWidgetScoreboardSport.generic2 ||
        sport == SettingsWidgetScoreboardSport.genericSets ||
        sport == SettingsWidgetScoreboardSport.hockey ||
        sport == SettingsWidgetScoreboardSport.football ||
        sport == SettingsWidgetScoreboardSport.tennis ||
        sport == SettingsWidgetScoreboardSport.volleyball
}

@Serializable
enum class SettingsWidgetType(val rawValue: String) {
    @SerialName("Text") text("Text"),
    @SerialName("Browser") browser("Browser"),
    @SerialName("Video source") videoSource("Video source"),
    @SerialName("Image") image("Image"),
    @SerialName("Alerts") alerts("Alerts"),
    @SerialName("Map") map("Map"),
    @SerialName("Snapshot") snapshot("Snapshot"),
    @SerialName("Chat") chat("Chat"),
    @SerialName("Chat emote combo") chatEmoteCombo("Chat emote combo"),
    @SerialName("Scene") scene("Scene"),
    @SerialName("Slideshow") slideshow("Slideshow"),
    @SerialName("VTuber") vTuber("VTuber"),
    @SerialName("PNGTuber") pngTuber("PNGTuber"),
    @SerialName("QR code") qrCode("QR code"),
    @SerialName("Scoreboard") scoreboard("Scoreboard"),
    @SerialName("Wheel of luck") wheelOfLuck("Wheel of luck"),
    @SerialName("Bingo card") bingoCard("Bingo card"),
    @SerialName("Crop") crop("Crop"),
    @SerialName("Pomodoro timer") pomodoroTimer("Pomodoro timer");

    override fun toString(): String = when (this) {
        text -> localized("Text")
        browser -> localized("Browser")
        videoSource -> localized("Video source")
        image -> localized("Image")
        alerts -> localized("Alerts")
        map -> localized("Map")
        snapshot -> localized("Snapshot")
        chat -> localized("Chat")
        chatEmoteCombo -> localized("Chat emote combo")
        scene -> localized("Scene")
        slideshow -> localized("Slideshow")
        vTuber -> localized("VTuber")
        pngTuber -> localized("PNGTuber")
        qrCode -> localized("QR code")
        scoreboard -> localized("Scoreboard")
        wheelOfLuck -> localized("Wheel of luck")
        bingoCard -> localized("Bingo card")
        crop -> localized("Crop")
        pomodoroTimer -> localized("Pomodoro timer")
    }

    fun image(): String = when (this) {
        image -> "photo"
        browser -> "globe"
        text -> "textformat"
        crop -> "crop"
        map -> "map"
        snapshot -> "camera.aperture"
        chat -> "message"
        chatEmoteCombo -> "hands.clap"
        scene -> "photo.on.rectangle"
        slideshow -> "play.rectangle"
        qrCode -> "qrcode"
        alerts -> "megaphone"
        videoSource -> "video"
        scoreboard -> "rectangle.split.2x1"
        vTuber -> "person.crop.circle"
        pngTuber -> "person.crop.circle.dashed"
        wheelOfLuck -> "burn"
        bingoCard -> "square.grid.3x3.square"
        pomodoroTimer -> "timer"
    }

    fun description(): String = when (this) {
        text -> localized("A text widget shows text, weather, clock and much more.")
        browser -> localized("A browser widget shows a webpage.")
        videoSource -> localized("A video source widget shows another camera or screen capture.")
        image -> localized("An image widget shows an image.")
        alerts -> localized("An alerts widget shows various alerts (subscriptions, raids, ...).")
        map -> localized("A map widget shows a map with your location.")
        snapshot -> localized("A snapshot widget shows snapshots when taken.")
        chat -> localized("A chat widget shows your chat.")
        chatEmoteCombo -> localized(
            "A chat emote combo widget shows an emote streak when chatters spam the same emote."
        )
        scene -> localized("A scene widget shows a scene's widgets.")
        slideshow -> localized("A slideshow widget shows a slideshow of widgets.")
        vTuber -> localized(
            "A VTuber widget shows a VRM or Live2D model that imitates your facial movements."
        )
        pngTuber -> localized(
            "A PNGTuber widget shows a PNGTuber model that imitates your facial movements."
        )
        qrCode -> localized("A QR code widget shows a QR code of any text.")
        scoreboard -> localized(
            "A scoreboard widget shows a sports scoreboard, controlled with an Apple Watch."
        )
        crop -> localized("A crop widget shows parts of a browser widget.")
        wheelOfLuck -> localized("A wheel of luck widget shows a wheel of luck that you can spin.")
        bingoCard -> localized("A bingo card widget shows an interactive bingo card.")
        pomodoroTimer -> localized("A Pomodoro timer widget shows a focus and break timer.")
    }

    companion object {
        fun fromRawValue(rawValue: String): SettingsWidgetType? =
            entries.firstOrNull { it.rawValue == rawValue }
    }
}

@Serializable
class SettingsScene(
    override var name: String = baseName,
    @Contextual var id: UUID = UUID.randomUUID(),
    var enabled: Boolean = true,
    var videoSource: SettingsVideoSource = SettingsVideoSource(),
    var widgets: List<SettingsSceneWidget> = emptyList(),
    var videoSourceRotation: Double = 0.0,
    var videoStabilizationMode: SettingsVideoStabilizationMode = SettingsVideoStabilizationMode.off,
    var overrideVideoStabilizationMode: Boolean = false,
    var fillFrame: Boolean = false,
    var overrideMic: Boolean = false,
    var micId: String = "",
    var quickSwitchGroup: Int? = null,
    var mirror: Boolean = false,
    var backgroundColor: RgbColor = defaultSegmentedPickerSelectedColor
) : Named {
    companion object {
        val baseName: String = localized("My scene")
    }

    @Transient var backgroundColorColor: Color = defaultSegmentedPickerSelectedColor.color()

    override fun equals(other: Any?): Boolean = other is SettingsScene && id == other.id

    override fun hashCode(): Int = id.hashCode()

    fun clone(): SettingsScene {
        val new = SettingsScene(name = name)
        new.enabled = enabled
        new.videoSource = videoSource
        val newWidgets = mutableListOf<SettingsSceneWidget>()
        for (widget in widgets) {
            newWidgets.add(widget.clone())
        }
        new.widgets = newWidgets
        new.videoSourceRotation = videoSourceRotation
        new.videoStabilizationMode = videoStabilizationMode
        new.overrideVideoStabilizationMode = overrideVideoStabilizationMode
        new.fillFrame = fillFrame
        new.overrideMic = overrideMic
        new.micId = micId
        new.quickSwitchGroup = quickSwitchGroup
        new.mirror = mirror
        new.backgroundColor = backgroundColor
        new.backgroundColorColor = backgroundColorColor
        return new
    }

    fun toCameraId(): SettingsCameraId = videoSource.toCameraId()

    fun updateCameraId(settingsCameraId: SettingsCameraId) {
        videoSource.updateCameraId(settingsCameraId)
    }
}

@Serializable
class SettingsAutoSceneSwitcherScene(
    @Contextual var id: UUID = UUID.randomUUID(),
    @Contextual var sceneId: UUID? = null,
    var time: Int = 15
)

@Serializable
class SettingsAutoSceneSwitcher(
    @Contextual var id: UUID = UUID.randomUUID(),
    override var name: String = baseName,
    var shuffle: Boolean = false,
    var scenes: List<SettingsAutoSceneSwitcherScene> = emptyList()
) : Named {
    companion object {
        val baseName: String = localized("My switcher")
    }
}

@Serializable
class SettingsAutoSceneSwitchers(
    @Contextual var switcherId: UUID? = null,
    var switchers: List<SettingsAutoSceneSwitcher> = emptyList()
)

@Serializable
enum class SettingsGraphicsImplementation {
    coreImage,
    metalPetal;

    override fun toString(): String = when (this) {
        coreImage -> "Core Image"
        metalPetal -> "MetalPetal"
    }

    companion object {
        fun fromRawValue(rawValue: String): SettingsGraphicsImplementation? =
            entries.firstOrNull { it.name == rawValue }
    }
}

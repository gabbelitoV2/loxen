package com.moblin.android.various.settings

import android.graphics.RectF
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.moblin.android.common.various.RgbColor
import com.moblin.android.localized
import com.moblin.android.media.haishinkit.media.video.SceneSwitchTransition
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.platform.avfoundation.AVCaptureDevice
import com.moblin.android.platform.codable.JsonObjectSerializer
import com.moblin.android.platform.codable.codableJson
import com.moblin.android.platform.codable.decode
import com.moblin.android.platform.codable.decodeIfPresent
import com.moblin.android.platform.codable.encodeContainer
import com.moblin.android.platform.swiftui.Published
import com.moblin.android.platform.swiftui.PublishedList
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
import com.moblin.android.videoeffects.dewarp360.Dewarp360Effect
import com.moblin.android.videoeffects.dewarp360.Dewarp360EffectSettings
import com.moblin.android.view.settings.scenes.widgets.widget.text.fontStyleName
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer

private fun RgbColor.color(): Color = Color(
    red = red.toFloat() / 255.0f,
    green = green.toFloat() / 255.0f,
    blue = blue.toFloat() / 255.0f,
    alpha = (opacity ?: 1.0).toFloat()
)

private fun <T> JsonObject.decodeSynthesized(key: String, serializer: KSerializer<T>): T {
    val element = this[key] ?: throw SerializationException("Key '$key' not found")
    return codableJson.decodeFromJsonElement(serializer, element)
}

private inline fun <reified T> JsonObject.decodeSynthesized(key: String): T =
    decodeSynthesized(key, codableJson.serializersModule.serializer<T>())

private fun <T : Any> JsonObject.decodeSynthesizedIfPresent(key: String, serializer: KSerializer<T>): T? {
    val element = this[key]
    if (element == null || element is JsonNull) {
        return null
    }
    return codableJson.decodeFromJsonElement(serializer, element)
}

private fun <T : Enum<T>> synthesizedEnumSerializer(serialName: String, cases: List<T>): KSerializer<T> =
    JsonObjectSerializer(
        serialName,
        { JsonObject(mapOf(it.name to JsonObject(emptyMap()))) },
        { container ->
            val keys = container.keys.filter { key -> cases.any { it.name == key } }
            if (keys.size != 1) {
                throw SerializationException("$serialName expects exactly one case")
            }
            if (container[keys[0]] !is JsonObject) {
                throw SerializationException("$serialName expects an object for case '${keys[0]}'")
            }
            cases.first { it.name == keys[0] }
        },
    )

private fun <T> rawValueSerializer(
    serialName: String,
    rawValue: (T) -> String,
    fromRawValue: (String) -> T,
): KSerializer<T> = object : KSerializer<T> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(serialName, PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeString(rawValue(value))
    }

    override fun deserialize(decoder: Decoder): T = fromRawValue(decoder.decodeString())
}

private fun decodeCameraId(container: JsonObject, key: String, defaultValue: CameraId): CameraId {
    var cameraId = container.decode(key, defaultValue)
    if (AVCaptureDevice.withUniqueID(cameraId) == null) {
        cameraId = defaultValue
    }
    return cameraId
}

private fun decodeCameraPosition(
    container: JsonObject,
    key: String,
    defaultValue: SettingsSceneCameraPosition
): SettingsSceneCameraPosition {
    var position = container.decode(key, SettingsSceneCameraPosition.serializer(), defaultValue)
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

@Serializable(with = SettingsVideoEffectRemoveBackground.Serializer::class)
class SettingsVideoEffectRemoveBackground(
    var from: RgbColor = defaultFromColor,
    var to: RgbColor = defaultToColor
) {
    var fromColor: Color by Published(from.color())
    var toColor: Color by Published(to.color())

    fun encode(): JsonObject = encodeContainer {
        encode("from", from)
        encode("to", to)
    }

    companion object {
        fun decode(container: JsonObject): SettingsVideoEffectRemoveBackground {
            val removeBackground = SettingsVideoEffectRemoveBackground()
            removeBackground.from = container.decode("from", defaultFromColor)
            removeBackground.fromColor = removeBackground.from.color()
            removeBackground.to = container.decode("to", defaultToColor)
            removeBackground.toColor = removeBackground.to.color()
            return removeBackground
        }
    }

    object Serializer : KSerializer<SettingsVideoEffectRemoveBackground> by JsonObjectSerializer(
        "SettingsVideoEffectRemoveBackground",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsVideoEffectShape.Serializer::class)
class SettingsVideoEffectShape(
    cornerRadius: Float = 0.1f,
    borderWidth: Double = 0.0,
    var borderColor: RgbColor = RgbColor(red = 0, green = 0, blue = 0),
    cropEnabled: Boolean = false,
    var cropX: Double = 0.25,
    var cropY: Double = 0.0,
    var cropWidth: Double = 0.5,
    var cropHeight: Double = 1.0
) {
    var cornerRadius: Float by Published(cornerRadius)
    var borderWidth: Double by Published(borderWidth)
    var borderColorColor: Color by Published(borderColor.color())
    var cropEnabled: Boolean by Published(cropEnabled)

    fun encode(): JsonObject = encodeContainer {
        encode("cornerRadius", cornerRadius)
        encode("borderWidth", borderWidth)
        encode("borderColor", borderColor)
        encode("cropEnabled", cropEnabled)
        encode("cropX", cropX)
        encode("cropY", cropY)
        encode("cropWidth", cropWidth)
        encode("cropHeight", cropHeight)
    }

    companion object {
        fun decode(container: JsonObject): SettingsVideoEffectShape {
            val shape = SettingsVideoEffectShape()
            shape.cornerRadius = container.decode("cornerRadius", 0.1f)
            shape.borderWidth = container.decode("borderWidth", 0.0)
            shape.borderColor = container.decode("borderColor", RgbColor(red = 0, green = 0, blue = 0))
            shape.borderColorColor = shape.borderColor.color()
            shape.cropEnabled = container.decode("cropEnabled", false)
            shape.cropX = container.decode("cropX", 0.25)
            shape.cropY = container.decode("cropY", 0.0)
            shape.cropWidth = container.decode("cropWidth", 0.5)
            shape.cropHeight = container.decode("cropHeight", 1.0)
            return shape
        }
    }

    object Serializer : KSerializer<SettingsVideoEffectShape> by JsonObjectSerializer(
        "SettingsVideoEffectShape",
        { it.encode() },
        { decode(it) },
    )

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

@Serializable(with = SettingsVideoEffectDewarp360.Serializer::class)
class SettingsVideoEffectDewarp360(
    pan: Float = 0f,
    tilt: Float = 0f,
    var zoom: Float = 1f
) {
    var pan: Float by Published(pan)
    var tilt: Float by Published(tilt)
    var inverseFieldOfView: Float by Published(
        (180.0 - Math.toDegrees(zoomToFieldOfView(zoom).toDouble())).toFloat()
    )

    fun encode(): JsonObject = encodeContainer {
        encode("pan", pan)
        encode("tilt", tilt)
        encode("zoom", zoom)
    }

    companion object {
        fun decode(container: JsonObject): SettingsVideoEffectDewarp360 {
            val dewarp360 = SettingsVideoEffectDewarp360()
            dewarp360.pan = container.decode("pan", 0f)
            dewarp360.tilt = container.decode("tilt", 0f)
            dewarp360.zoom = container.decode("zoom", 1f)
            dewarp360.inverseFieldOfView =
                (180.0 - Math.toDegrees(zoomToFieldOfView(dewarp360.zoom).toDouble())).toFloat()
            return dewarp360
        }
    }

    object Serializer : KSerializer<SettingsVideoEffectDewarp360> by JsonObjectSerializer(
        "SettingsVideoEffectDewarp360",
        { it.encode() },
        { decode(it) },
    )

    fun updateZoomFromInverseFieldOfView() {
        zoom = fieldOfViewToZoom(Math.toRadians((180f - inverseFieldOfView).toDouble()).toFloat())
    }

    fun toSettings(): Dewarp360EffectSettings = Dewarp360EffectSettings.Direct(
        pan = -Math.toRadians(pan.toDouble()).toFloat(),
        tilt = Math.toRadians(tilt.toDouble()).toFloat(),
        fieldOfView = zoomToFieldOfView(zoom)
    )
}

@Serializable(with = SettingsVideoEffectAnamorphicLens.Serializer::class)
class SettingsVideoEffectAnamorphicLens(
    scale: Double = 1.33
) {
    var scale: Double by Published(scale)

    fun encode(): JsonObject = encodeContainer {
        encode("scale", scale)
    }

    companion object {
        fun decode(container: JsonObject): SettingsVideoEffectAnamorphicLens {
            val anamorphicLens = SettingsVideoEffectAnamorphicLens()
            anamorphicLens.scale = container.decode("scale", 1.33)
            return anamorphicLens
        }
    }

    object Serializer : KSerializer<SettingsVideoEffectAnamorphicLens> by JsonObjectSerializer(
        "SettingsVideoEffectAnamorphicLens",
        { it.encode() },
        { decode(it) },
    )

    fun clone(): SettingsVideoEffectAnamorphicLens {
        val new = SettingsVideoEffectAnamorphicLens()
        new.scale = scale
        return new
    }
}

@Serializable(with = SettingsVideoEffectLut.Serializer::class)
class SettingsVideoEffectLut(
    lut: UUID? = null
) {
    var lut: UUID? by Published(lut)

    fun encode(): JsonObject = encodeContainer {
        encode("lut", lut)
    }

    companion object {
        fun decode(container: JsonObject): SettingsVideoEffectLut {
            val lut = SettingsVideoEffectLut()
            lut.lut = if (container["lut"] is JsonNull) null else container.decode<UUID?>("lut", UUID.randomUUID())
            return lut
        }
    }

    object Serializer : KSerializer<SettingsVideoEffectLut> by JsonObjectSerializer(
        "SettingsVideoEffectLut",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsVideoEffectOpacity.Serializer::class)
class SettingsVideoEffectOpacity(
    opacity: Double = 0.5
) {
    var opacity: Double by Published(opacity)

    fun encode(): JsonObject = encodeContainer {
        encode("opacity", opacity)
    }

    companion object {
        fun decode(container: JsonObject): SettingsVideoEffectOpacity {
            val opacity = SettingsVideoEffectOpacity()
            opacity.opacity = container.decode("opacity", 0.5)
            return opacity
        }
    }

    object Serializer : KSerializer<SettingsVideoEffectOpacity> by JsonObjectSerializer(
        "SettingsVideoEffectOpacity",
        { it.encode() },
        { decode(it) },
    )
}

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

@Serializable(with = SettingsVideoEffectMaskEffectPoint.Serializer::class)
data class SettingsVideoEffectMaskEffectPoint(
    val x: Double = 50.0,
    val y: Double = 50.0
) {
    val id: UUID = UUID.randomUUID()

    fun encode(): JsonObject = encodeContainer {
        encode("x", x)
        encode("y", y)
    }

    companion object {
        fun decode(container: JsonObject): SettingsVideoEffectMaskEffectPoint {
            val x = container.decode("x", 50.0)
            val y = container.decode("y", 50.0)
            return SettingsVideoEffectMaskEffectPoint(x = x, y = y)
        }
    }

    object Serializer : KSerializer<SettingsVideoEffectMaskEffectPoint> by JsonObjectSerializer(
        "SettingsVideoEffectMaskEffectPoint",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsVideoEffectMask.Serializer::class)
class SettingsVideoEffectMask(
    points: List<SettingsVideoEffectMaskEffectPoint> = defaultPoints,
    inverted: Boolean = false,
    tension: Double = defaultTension,
    backgroundType: SettingsMaskBackgroundType = SettingsMaskBackgroundType.transparent,
    var backgroundColor: RgbColor = defaultBackgroundColor,
    var backgroundColor2: RgbColor = defaultBackgroundColor2
) {
    var points: List<SettingsVideoEffectMaskEffectPoint> by Published(points)
    var inverted: Boolean by Published(inverted)
    var tension: Double by Published(tension)
    var backgroundType: SettingsMaskBackgroundType by Published(backgroundType)
    var backgroundColorColor: Color by Published(backgroundColor.color())
    var backgroundColorColor2: Color by Published(backgroundColor2.color())

    fun encode(): JsonObject = encodeContainer {
        encode("points", points, ListSerializer(SettingsVideoEffectMaskEffectPoint.serializer()))
        encode("inverted", inverted)
        encode("tension", tension)
        encode("backgroundType", backgroundType)
        encode("backgroundColor", backgroundColor)
        encode("backgroundColor2", backgroundColor2)
    }

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

        fun decode(container: JsonObject): SettingsVideoEffectMask {
            val mask = SettingsVideoEffectMask()
            mask.points = container.decode(
                "points",
                ListSerializer(SettingsVideoEffectMaskEffectPoint.serializer()),
                defaultPoints
            )
            mask.inverted = container.decode("inverted", false)
            mask.tension = container.decode("tension", defaultTension)
            mask.backgroundType = container.decode("backgroundType", SettingsMaskBackgroundType.transparent)
            mask.backgroundColor = container.decode("backgroundColor", defaultBackgroundColor)
            mask.backgroundColor2 = container.decode("backgroundColor2", defaultBackgroundColor2)
            mask.backgroundColorColor = mask.backgroundColor.color()
            mask.backgroundColorColor2 = mask.backgroundColor2.color()
            return mask
        }
    }

    object Serializer : KSerializer<SettingsVideoEffectMask> by JsonObjectSerializer(
        "SettingsVideoEffectMask",
        { it.encode() },
        { decode(it) },
    )

    fun toEffectSettings(): MaskEffectSettings = MaskEffectSettings(
        points = points.map { MaskEffectPoint(x = it.x / 100, y = it.y / 100) },
        inverted = inverted,
        tension = tension,
        backgroundType = backgroundType,
        backgroundColor = backgroundColor,
        backgroundColor2 = backgroundColor2
    )
}

@Serializable(with = SettingsVideoEffect.Serializer::class)
class SettingsVideoEffect(
    var id: UUID = UUID.randomUUID(),
    enabled: Boolean = true,
    type: SettingsVideoEffectType = SettingsVideoEffectType.shape,
    var removeBackground: SettingsVideoEffectRemoveBackground = SettingsVideoEffectRemoveBackground(),
    var shape: SettingsVideoEffectShape = SettingsVideoEffectShape(),
    var dewarp360: SettingsVideoEffectDewarp360 = SettingsVideoEffectDewarp360(),
    var anamorphicLens: SettingsVideoEffectAnamorphicLens = SettingsVideoEffectAnamorphicLens(),
    var lut: SettingsVideoEffectLut = SettingsVideoEffectLut(),
    var opacity: SettingsVideoEffectOpacity = SettingsVideoEffectOpacity(),
    var mask: SettingsVideoEffectMask = SettingsVideoEffectMask()
) {
    var enabled: Boolean by Published(enabled)
    var type: SettingsVideoEffectType by Published(type)

    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("enabled", enabled)
        encode("type", type)
        encode("removeBackground", removeBackground, SettingsVideoEffectRemoveBackground.serializer())
        encode("shape", shape, SettingsVideoEffectShape.serializer())
        encode("dewarp360", dewarp360, SettingsVideoEffectDewarp360.serializer())
        encode("anamorphicLens", anamorphicLens, SettingsVideoEffectAnamorphicLens.serializer())
        encode("lut", lut, SettingsVideoEffectLut.serializer())
        encode("opacity", opacity, SettingsVideoEffectOpacity.serializer())
        encode("mask", mask, SettingsVideoEffectMask.serializer())
    }

    companion object {
        fun decode(container: JsonObject): SettingsVideoEffect {
            val effect = SettingsVideoEffect()
            effect.id = container.decode("id", UUID.randomUUID())
            effect.enabled = container.decode("enabled", true)
            effect.type = container.decode("type", SettingsVideoEffectType.shape)
            effect.removeBackground = container.decode(
                "removeBackground",
                SettingsVideoEffectRemoveBackground.serializer(),
                SettingsVideoEffectRemoveBackground()
            )
            effect.shape = container.decode("shape", SettingsVideoEffectShape.serializer(), SettingsVideoEffectShape())
            effect.dewarp360 = container.decode(
                "dewarp360",
                SettingsVideoEffectDewarp360.serializer(),
                SettingsVideoEffectDewarp360()
            )
            effect.anamorphicLens = container.decode(
                "anamorphicLens",
                SettingsVideoEffectAnamorphicLens.serializer(),
                SettingsVideoEffectAnamorphicLens()
            )
            effect.lut = container.decode("lut", SettingsVideoEffectLut.serializer(), SettingsVideoEffectLut())
            effect.opacity = container.decode(
                "opacity",
                SettingsVideoEffectOpacity.serializer(),
                SettingsVideoEffectOpacity()
            )
            effect.mask = container.decode("mask", SettingsVideoEffectMask.serializer(), SettingsVideoEffectMask())
            return effect
        }
    }

    object Serializer : KSerializer<SettingsVideoEffect> by JsonObjectSerializer(
        "SettingsVideoEffect",
        { it.encode() },
        { decode(it) },
    )

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
            TODO()
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

@Serializable(with = SettingsWidgetTextTimer.Serializer::class)
class SettingsWidgetTextTimer(
    var id: UUID = UUID.randomUUID(),
    delta: Int = 5,
    endTime: Double = 0.0
) {
    var delta: Int by Published(delta)
    var endTime: Double by Published(endTime)

    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("delta", delta)
        encode("endTime", endTime)
    }

    companion object {
        fun decode(container: JsonObject): SettingsWidgetTextTimer {
            val timer = SettingsWidgetTextTimer()
            timer.id = container.decode("id", UUID.randomUUID())
            timer.delta = container.decode("delta", 5)
            timer.endTime = container.decode("endTime", 0.0)
            return timer
        }
    }

    object Serializer : KSerializer<SettingsWidgetTextTimer> by JsonObjectSerializer(
        "SettingsWidgetTextTimer",
        { it.encode() },
        { decode(it) },
    )

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

@Serializable(with = SettingsWidgetTextStopwatch.Serializer::class)
class SettingsWidgetTextStopwatch(
    var id: UUID = UUID.randomUUID(),
    var totalElapsed: Double = 0.0,
    running: Boolean = false
) {
    var playPressedTime: Instant = Instant.now()
    var running: Boolean by Published(running)

    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("totalElapsed", totalElapsed)
        encode("running", running)
    }

    companion object {
        fun decode(container: JsonObject): SettingsWidgetTextStopwatch {
            val stopwatch = SettingsWidgetTextStopwatch()
            stopwatch.id = container.decode("id", UUID.randomUUID())
            stopwatch.totalElapsed = container.decode("totalElapsed", 0.0)
            stopwatch.running = container.decode("running", false)
            return stopwatch
        }
    }

    object Serializer : KSerializer<SettingsWidgetTextStopwatch> by JsonObjectSerializer(
        "SettingsWidgetTextStopwatch",
        { it.encode() },
        { decode(it) },
    )

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

@Serializable(with = SettingsWidgetTextSubtitles.Serializer::class)
class SettingsWidgetTextSubtitles(
    var identifier: String? = null
) {
    fun encode(): JsonObject = encodeContainer {
        encodeIfPresent("identifier", identifier)
    }

    companion object {
        fun decode(container: JsonObject): SettingsWidgetTextSubtitles {
            val subtitles = SettingsWidgetTextSubtitles()
            subtitles.identifier = container.decodeSynthesizedIfPresent("identifier", String.serializer())
            return subtitles
        }
    }

    object Serializer : KSerializer<SettingsWidgetTextSubtitles> by JsonObjectSerializer(
        "SettingsWidgetTextSubtitles",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsWidgetTextCheckbox.Serializer::class)
class SettingsWidgetTextCheckbox(
    var id: UUID = UUID.randomUUID(),
    var checked: Boolean = false
) {
    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("checked", checked)
    }

    companion object {
        fun decode(container: JsonObject): SettingsWidgetTextCheckbox {
            val checkbox = SettingsWidgetTextCheckbox()
            checkbox.id = container.decodeSynthesized("id")
            checkbox.checked = container.decodeSynthesized("checked")
            return checkbox
        }
    }

    object Serializer : KSerializer<SettingsWidgetTextCheckbox> by JsonObjectSerializer(
        "SettingsWidgetTextCheckbox",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsWidgetTextRating.Serializer::class)
class SettingsWidgetTextRating(
    var id: UUID = UUID.randomUUID(),
    var rating: Int = 0
) {
    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("rating", rating)
    }

    companion object {
        fun decode(container: JsonObject): SettingsWidgetTextRating {
            val rating = SettingsWidgetTextRating()
            rating.id = container.decodeSynthesized("id")
            rating.rating = container.decodeSynthesized("rating")
            return rating
        }
    }

    object Serializer : KSerializer<SettingsWidgetTextRating> by JsonObjectSerializer(
        "SettingsWidgetTextRating",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsWidgetTextLapTimes.Serializer::class)
class SettingsWidgetTextLapTimes(
    var id: UUID = UUID.randomUUID(),
    var currentLapStartTime: Double? = null,
    var lapTimes: List<Double> = emptyList()
) {
    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encodeIfPresent("currentLapStartTime", currentLapStartTime)
        encode("lapTimes", lapTimes)
    }

    companion object {
        fun decode(container: JsonObject): SettingsWidgetTextLapTimes {
            val lapTimes = SettingsWidgetTextLapTimes()
            lapTimes.id = container.decodeSynthesized("id")
            lapTimes.currentLapStartTime =
                container.decodeSynthesizedIfPresent("currentLapStartTime", Double.serializer())
            lapTimes.lapTimes = container.decodeSynthesized("lapTimes")
            return lapTimes
        }
    }

    object Serializer : KSerializer<SettingsWidgetTextLapTimes> by JsonObjectSerializer(
        "SettingsWidgetTextLapTimes",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsWidgetText.Serializer::class)
class SettingsWidgetText(
    formatString: String = "{shortTime}",
    var backgroundColor: RgbColor = RgbColor(red = 0, green = 0, blue = 0, opacity = 0.75),
    var clearBackgroundColor: Boolean = false,
    var foregroundColor: RgbColor = RgbColor(red = 255, green = 255, blue = 255),
    var clearForegroundColor: Boolean = false,
    var fontSize: Int = 30,
    fontFamily: String? = null,
    fontStyle: String = "",
    fontDesign: SettingsFontDesign = SettingsFontDesign.`default`,
    fontWeight: SettingsFontWeight = SettingsFontWeight.regular,
    fontMonospacedDigits: Boolean = false,
    alignment: SettingsHorizontalAlignment = SettingsHorizontalAlignment.leading,
    horizontalAlignment: SettingsHorizontalAlignment = SettingsHorizontalAlignment.leading,
    verticalAlignment: SettingsVerticalAlignment = SettingsVerticalAlignment.top,
    delay: Double = 0.0,
    timers: List<SettingsWidgetTextTimer> = emptyList(),
    stopwatches: List<SettingsWidgetTextStopwatch> = emptyList(),
    var needsWeather: Boolean = false,
    var needsGeography: Boolean = false,
    var needsSubtitles: Boolean = false,
    var subtitles: List<SettingsWidgetTextSubtitles> = emptyList(),
    checkboxes: List<SettingsWidgetTextCheckbox> = emptyList(),
    ratings: List<SettingsWidgetTextRating> = emptyList(),
    lapTimes: List<SettingsWidgetTextLapTimes> = emptyList(),
    var needsGForce: Boolean = false,
    widthEnabled: Boolean = false,
    width: Int = defaultWidth,
    cornerRadius: Int = defaultCornerRadius
) {
    var formatString: String by Published(formatString)
    var backgroundColorColor: Color by Published(backgroundColor.color())
    var foregroundColorColor: Color by Published(foregroundColor.color())
    var fontSizeFloat: Float by Published(fontSize.toFloat())
    var fontFamily: String? by Published(fontFamily)
    var fontStyle: String by Published(fontStyle)
    var fontDesign: SettingsFontDesign by Published(fontDesign)
    var fontWeight: SettingsFontWeight by Published(fontWeight)
    var fontMonospacedDigits: Boolean by Published(fontMonospacedDigits)
    var alignment: SettingsHorizontalAlignment by Published(alignment)
    var horizontalAlignment: SettingsHorizontalAlignment by Published(horizontalAlignment)
    var verticalAlignment: SettingsVerticalAlignment by Published(verticalAlignment)
    var delay: Double by Published(delay)
    var timers: List<SettingsWidgetTextTimer> by Published(timers)
    var stopwatches: List<SettingsWidgetTextStopwatch> by Published(stopwatches)
    var checkboxes: List<SettingsWidgetTextCheckbox> by Published(checkboxes)
    var ratings: List<SettingsWidgetTextRating> by Published(ratings)
    var lapTimes: List<SettingsWidgetTextLapTimes> by Published(lapTimes)
    var widthEnabled: Boolean by Published(widthEnabled)
    var width: Int by Published(width)
    var cornerRadius: Int by Published(cornerRadius)

    fun encode(): JsonObject = encodeContainer {
        encode("formatString", formatString)
        encode("backgroundColor", backgroundColor)
        encode("clearBackgroundColor", clearBackgroundColor)
        encode("foregroundColor", foregroundColor)
        encode("clearForegroundColor", clearForegroundColor)
        encode("fontSize", fontSize)
        encode("fontFamily", fontFamily)
        encode("fontStyle", fontStyle)
        encode("fontDesign", fontDesign)
        encode("fontWeight", fontWeight)
        encode("fontMonospacedDigits", fontMonospacedDigits)
        encode("alignment", alignment)
        encode("horizontalAlignment", horizontalAlignment)
        encode("verticalAlignment", verticalAlignment)
        encode("delay", delay)
        encode("timers", timers, ListSerializer(SettingsWidgetTextTimer.serializer()))
        encode("stopwatches", stopwatches, ListSerializer(SettingsWidgetTextStopwatch.serializer()))
        encode("needsWeather", needsWeather)
        encode("needsGeography", needsGeography)
        encode("needsSubtitles", needsSubtitles)
        encode("subtitles", subtitles, ListSerializer(SettingsWidgetTextSubtitles.serializer()))
        encode("checkboxes", checkboxes, ListSerializer(SettingsWidgetTextCheckbox.serializer()))
        encode("ratings", ratings, ListSerializer(SettingsWidgetTextRating.serializer()))
        encode("lapTimes", lapTimes, ListSerializer(SettingsWidgetTextLapTimes.serializer()))
        encode("needsGForce", needsGForce)
        encode("widthEnabled", widthEnabled)
        encode("width", width)
        encode("cornerRadius", cornerRadius)
    }

    companion object {
        private const val defaultWidth = 300
        private const val defaultCornerRadius = 10

        fun decode(container: JsonObject): SettingsWidgetText {
            val text = SettingsWidgetText()
            text.formatString = container.decode("formatString", "{shortTime}")
            text.backgroundColor = container.decode(
                "backgroundColor",
                RgbColor(red = 0, green = 0, blue = 0, opacity = 0.75)
            )
            text.backgroundColorColor = text.backgroundColor.color()
            text.clearBackgroundColor = container.decode("clearBackgroundColor", false)
            text.foregroundColor = container.decode(
                "foregroundColor",
                RgbColor(red = 255, green = 255, blue = 255)
            )
            text.foregroundColorColor = text.foregroundColor.color()
            text.clearForegroundColor = container.decode("clearForegroundColor", false)
            text.fontSize = container.decode("fontSize", 30)
            text.fontSizeFloat = text.fontSize.toFloat()
            text.fontFamily = container.decode<String?>("fontFamily", null)
            text.fontStyle = container.decode("fontStyle", "")
            text.fontDesign = container.decode("fontDesign", SettingsFontDesign.`default`)
            text.fontWeight = container.decode("fontWeight", SettingsFontWeight.regular)
            text.fontMonospacedDigits = container.decode("fontMonospacedDigits", false)
            text.alignment = container.decode("alignment", SettingsHorizontalAlignment.leading)
            text.horizontalAlignment = container.decode(
                "horizontalAlignment",
                SettingsHorizontalAlignment.leading
            )
            text.verticalAlignment = container.decode("verticalAlignment", SettingsVerticalAlignment.top)
            text.delay = container.decode("delay", 0.0)
            text.timers = container.decode("timers", ListSerializer(SettingsWidgetTextTimer.serializer()), emptyList())
            text.stopwatches = container.decode(
                "stopwatches",
                ListSerializer(SettingsWidgetTextStopwatch.serializer()),
                emptyList()
            )
            text.needsWeather = container.decode("needsWeather", false)
            text.needsGeography = container.decode("needsGeography", false)
            text.needsSubtitles = container.decode("needsSubtitles", false)
            text.subtitles = container.decode(
                "subtitles",
                ListSerializer(SettingsWidgetTextSubtitles.serializer()),
                emptyList()
            )
            text.checkboxes = container.decode(
                "checkboxes",
                ListSerializer(SettingsWidgetTextCheckbox.serializer()),
                emptyList()
            )
            text.ratings = container.decode(
                "ratings",
                ListSerializer(SettingsWidgetTextRating.serializer()),
                emptyList()
            )
            text.lapTimes = container.decode(
                "lapTimes",
                ListSerializer(SettingsWidgetTextLapTimes.serializer()),
                emptyList()
            )
            text.needsGForce = container.decode("needsGForce", false)
            text.widthEnabled = container.decode("widthEnabled", false)
            text.width = container.decode("width", defaultWidth)
            text.cornerRadius = container.decode("cornerRadius", defaultCornerRadius)
            return text
        }
    }

    object Serializer : KSerializer<SettingsWidgetText> by JsonObjectSerializer(
        "SettingsWidgetText",
        { it.encode() },
        { decode(it) },
    )

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

@Serializable(with = SettingsWidgetCrop.Serializer::class)
class SettingsWidgetCrop(
    var sourceWidgetId: UUID = UUID.randomUUID(),
    var x: Int = 0,
    var y: Int = 0,
    var width: Int = 200,
    var height: Int = 200
) {
    fun encode(): JsonObject = encodeContainer {
        encode("sourceWidgetId", sourceWidgetId)
        encode("x", x)
        encode("y", y)
        encode("width", width)
        encode("height", height)
    }

    companion object {
        fun decode(container: JsonObject): SettingsWidgetCrop {
            val crop = SettingsWidgetCrop()
            crop.sourceWidgetId = container.decodeSynthesized("sourceWidgetId")
            crop.x = container.decodeSynthesized("x")
            crop.y = container.decodeSynthesized("y")
            crop.width = container.decodeSynthesized("width")
            crop.height = container.decodeSynthesized("height")
            return crop
        }
    }

    object Serializer : KSerializer<SettingsWidgetCrop> by JsonObjectSerializer(
        "SettingsWidgetCrop",
        { it.encode() },
        { decode(it) },
    )

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

@Serializable(with = SettingsWidgetBrowser.Serializer::class)
class SettingsWidgetBrowser(
    url: String = "",
    width: Int = 500,
    height: Int = 500,
    mode: SettingsWidgetBrowserMode = SettingsWidgetBrowserMode.periodicAudioAndVideo,
    baseFps: Float = 5.0f,
    styleSheet: String = "",
    moblinAccess: Boolean = false,
    speechToText: Boolean = false,
    localOnly: Boolean = false
) {
    var url: String by Published(url)
    var width: Int by Published(width)
    var height: Int by Published(height)
    var mode: SettingsWidgetBrowserMode by Published(mode)
    var baseFps: Float by Published(baseFps)
    var styleSheet: String by Published(styleSheet)
    var moblinAccess: Boolean by Published(moblinAccess)
    var speechToText: Boolean by Published(speechToText)
    var localOnly: Boolean by Published(localOnly)

    fun encode(): JsonObject = encodeContainer {
        encode("url", url)
        encode("width", width)
        encode("height", height)
        encode("mode", mode)
        encode("fps", baseFps)
        encode("styleSheet", styleSheet)
        encode("moblinAccess", moblinAccess)
        encode("speechToText", speechToText)
        encode("localOnly", localOnly)
    }

    companion object {
        fun decode(container: JsonObject): SettingsWidgetBrowser {
            val browser = SettingsWidgetBrowser()
            browser.url = container.decode("url", "")
            browser.width = container.decode("width", 500)
            browser.height = container.decode("height", 500)
            val decodedMode = container.decodeIfPresent("mode", SettingsWidgetBrowserMode.serializer())
            if (decodedMode != null) {
                browser.mode = decodedMode
            } else {
                val audioOnly = container.decode("audioOnly", false)
                browser.mode = if (audioOnly) {
                    SettingsWidgetBrowserMode.audioAndVideoOnly
                } else {
                    SettingsWidgetBrowserMode.periodicAudioAndVideo
                }
            }
            browser.baseFps = container.decode("fps", 5.0f)
            browser.styleSheet = container.decode("styleSheet", "")
            browser.moblinAccess = container.decode("moblinAccess", false)
            browser.speechToText = container.decode("speechToText", false)
            browser.localOnly = container.decode("localOnly", false)
            return browser
        }
    }

    object Serializer : KSerializer<SettingsWidgetBrowser> by JsonObjectSerializer(
        "SettingsWidgetBrowser",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsWidgetMap.Serializer::class)
class SettingsWidgetMap(
    var northUp: Boolean = false,
    var delay: Double = 0.0,
    var size: Double = 1000.0
) {
    fun encode(): JsonObject = encodeContainer {
        encode("northUp", northUp)
        encode("delay", delay)
        encode("scale", size)
    }

    companion object {
        fun decode(container: JsonObject): SettingsWidgetMap {
            val map = SettingsWidgetMap()
            map.northUp = container.decode("northUp", false)
            map.delay = container.decode("delay", 0.0)
            map.size = container.decode("scale", 1000.0)
            return map
        }
    }

    object Serializer : KSerializer<SettingsWidgetMap> by JsonObjectSerializer(
        "SettingsWidgetMap",
        { it.encode() },
        { decode(it) },
    )

    fun clone(): SettingsWidgetMap {
        val new = SettingsWidgetMap()
        new.northUp = northUp
        new.delay = delay
        new.size = size
        return new
    }
}

@Serializable(with = SettingsWidgetScene.Serializer::class)
class SettingsWidgetScene(
    var sceneId: UUID = UUID.randomUUID()
) {
    fun encode(): JsonObject = encodeContainer {
        encode("sceneId", sceneId)
    }

    companion object {
        fun decode(container: JsonObject): SettingsWidgetScene {
            val scene = SettingsWidgetScene()
            scene.sceneId = container.decodeSynthesized("sceneId")
            return scene
        }
    }

    object Serializer : KSerializer<SettingsWidgetScene> by JsonObjectSerializer(
        "SettingsWidgetScene",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsWidgetQrCode.Serializer::class)
class SettingsWidgetQrCode(
    var message: String = ""
) {
    fun encode(): JsonObject = encodeContainer {
        encode("message", message)
    }

    companion object {
        fun decode(container: JsonObject): SettingsWidgetQrCode {
            val qrCode = SettingsWidgetQrCode()
            qrCode.message = container.decodeSynthesized("message")
            return qrCode
        }
    }

    object Serializer : KSerializer<SettingsWidgetQrCode> by JsonObjectSerializer(
        "SettingsWidgetQrCode",
        { it.encode() },
        { decode(it) },
    )

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

@Serializable(with = SettingsWidgetAlertFacePosition.Serializer::class)
class SettingsWidgetAlertFacePosition(
    var x: Double = 0.25,
    var y: Double = 0.25,
    var width: Double = 0.5,
    var height: Double = 0.5
) {
    fun encode(): JsonObject = encodeContainer {
        encode("x", x)
        encode("y", y)
        encode("width", width)
        encode("height", height)
    }

    companion object {
        fun decode(container: JsonObject): SettingsWidgetAlertFacePosition {
            val facePosition = SettingsWidgetAlertFacePosition()
            facePosition.x = container.decodeSynthesized("x")
            facePosition.y = container.decodeSynthesized("y")
            facePosition.width = container.decodeSynthesized("width")
            facePosition.height = container.decodeSynthesized("height")
            return facePosition
        }
    }

    object Serializer : KSerializer<SettingsWidgetAlertFacePosition> by JsonObjectSerializer(
        "SettingsWidgetAlertFacePosition",
        { it.encode() },
        { decode(it) },
    )

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

@Serializable(with = SettingsWidgetAlertsAlert.Serializer::class)
class SettingsWidgetAlertsAlert(
    var id: UUID = UUID.randomUUID(),
    var enabled: Boolean = true,
    mediaType: SettingsWidgetAlertsAlertMediaType = SettingsWidgetAlertsAlertMediaType.gifAndSound,
    imageId: UUID = UUID.randomUUID(),
    imageLoopCount: Int = 1,
    soundId: UUID = UUID.randomUUID(),
    videoName: String = "",
    var textColor: RgbColor = RgbColor(red = 255, green = 255, blue = 255),
    var accentColor: RgbColor = RgbColor(red = 0xFD, green = 0xFB, blue = 0x67),
    var fontSize: Int = 45,
    var fontDesign: SettingsFontDesign = SettingsFontDesign.monospaced,
    var fontWeight: SettingsFontWeight = SettingsFontWeight.bold,
    var textToSpeechEnabled: Boolean = true,
    var textToSpeechDelay: Double = 1.5,
    textToSpeechLanguageVoices: Map<String, SettingsVoice> = emptyMap(),
    positionType: SettingsWidgetAlertPositionType = SettingsWidgetAlertPositionType.scene,
    var facePosition: SettingsWidgetAlertFacePosition = SettingsWidgetAlertFacePosition()
) {
    var mediaType: SettingsWidgetAlertsAlertMediaType by Published(mediaType)
    var imageId: UUID by Published(imageId)
    var imageLoopCount: Int by Published(imageLoopCount)
    var soundId: UUID by Published(soundId)
    var videoName: String by Published(videoName)
    var textToSpeechLanguageVoices: Map<String, SettingsVoice> by Published(textToSpeechLanguageVoices)
    var positionType: SettingsWidgetAlertPositionType by Published(positionType)

    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("enabled", enabled)
        encode("mediaType", mediaType)
        encode("imageId", imageId)
        encode("imageLoopCount", imageLoopCount)
        encode("soundId", soundId)
        encode("videoName", videoName)
        encode("textColor", textColor)
        encode("accentColor", accentColor)
        encode("fontSize", fontSize)
        encode("fontDesign", fontDesign)
        encode("fontWeight", fontWeight)
        encode("textToSpeechEnabled", textToSpeechEnabled)
        encode("textToSpeechDelay", textToSpeechDelay)
        encode(
            "textToSpeechLanguageVoices",
            textToSpeechLanguageVoices,
            MapSerializer(String.serializer(), SettingsVoice.serializer())
        )
        encode("positionType", positionType)
        encode("facePosition", facePosition, SettingsWidgetAlertFacePosition.serializer())
    }

    companion object {
        fun decode(container: JsonObject): SettingsWidgetAlertsAlert {
            val alert = SettingsWidgetAlertsAlert()
            alert.id = container.decode("id", UUID.randomUUID())
            alert.enabled = container.decode("enabled", true)
            alert.mediaType = container.decode("mediaType", SettingsWidgetAlertsAlertMediaType.gifAndSound)
            alert.imageId = container.decode("imageId", UUID.randomUUID())
            alert.imageLoopCount = container.decode("imageLoopCount", 1)
            alert.soundId = container.decode("soundId", UUID.randomUUID())
            alert.videoName = container.decode("videoName", "")
            alert.textColor = container.decode("textColor", RgbColor(red = 255, green = 255, blue = 255))
            alert.accentColor = container.decode("accentColor", RgbColor(red = 0xFD, green = 0xFB, blue = 0x67))
            alert.fontSize = container.decode("fontSize", 45)
            alert.fontDesign = container.decode("fontDesign", SettingsFontDesign.monospaced)
            alert.fontWeight = container.decode("fontWeight", SettingsFontWeight.bold)
            alert.textToSpeechEnabled = container.decode("textToSpeechEnabled", true)
            alert.textToSpeechDelay = container.decode("textToSpeechDelay", 1.5)
            alert.textToSpeechLanguageVoices = container.decode(
                "textToSpeechLanguageVoices",
                MapSerializer(String.serializer(), SettingsVoice.serializer()),
                emptyMap()
            )
            for ((languageCode, voice) in container.decode(
                "textToSpeechLanguageVoices",
                MapSerializer(String.serializer(), String.serializer()),
                emptyMap()
            )) {
                val settingsVoice = SettingsVoice()
                settingsVoice.apple.voice = voice
                alert.textToSpeechLanguageVoices = alert.textToSpeechLanguageVoices + (languageCode to settingsVoice)
            }
            alert.positionType = container.decode("positionType", SettingsWidgetAlertPositionType.scene)
            alert.facePosition = container.decode(
                "facePosition",
                SettingsWidgetAlertFacePosition.serializer(),
                SettingsWidgetAlertFacePosition()
            )
            return alert
        }
    }

    object Serializer : KSerializer<SettingsWidgetAlertsAlert> by JsonObjectSerializer(
        "SettingsWidgetAlertsAlert",
        { it.encode() },
        { decode(it) },
    )

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

@Serializable(with = SettingsWidgetAlertsCheerBitsAlertOperator.Serializer::class)
enum class SettingsWidgetAlertsCheerBitsAlertOperator(val rawValue: String) {
    equal("="),
    greaterEqual(">=");

    companion object {
        fun fromRawValue(rawValue: String): SettingsWidgetAlertsCheerBitsAlertOperator =
            entries.firstOrNull { it.rawValue == rawValue } ?: equal
    }

    object Serializer : KSerializer<SettingsWidgetAlertsCheerBitsAlertOperator> by rawValueSerializer(
        "com.moblin.android.various.settings.SettingsWidgetAlertsCheerBitsAlertOperator",
        { it.rawValue },
        { fromRawValue(it) },
    )
}

val cheerBitsAlertOperators: List<String> =
    SettingsWidgetAlertsCheerBitsAlertOperator.entries.map { it.rawValue }

@Serializable(with = SettingsWidgetAlertsCheerBitsAlert.Serializer::class)
class SettingsWidgetAlertsCheerBitsAlert(
    var id: UUID = UUID.randomUUID(),
    var bits: Int = 1,
    var comparisonOperator: SettingsWidgetAlertsCheerBitsAlertOperator =
        SettingsWidgetAlertsCheerBitsAlertOperator.greaterEqual,
    var alert: SettingsWidgetAlertsAlert = SettingsWidgetAlertsAlert()
) {
    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("bits", bits)
        encode("comparisonOperator", comparisonOperator, SettingsWidgetAlertsCheerBitsAlertOperator.serializer())
        encode("alert", alert, SettingsWidgetAlertsAlert.serializer())
    }

    companion object {
        fun decode(container: JsonObject): SettingsWidgetAlertsCheerBitsAlert {
            val cheerBits = SettingsWidgetAlertsCheerBitsAlert()
            cheerBits.id = container.decodeSynthesized("id")
            cheerBits.bits = container.decodeSynthesized("bits")
            cheerBits.comparisonOperator = container.decodeSynthesized(
                "comparisonOperator",
                SettingsWidgetAlertsCheerBitsAlertOperator.serializer()
            )
            cheerBits.alert = container.decodeSynthesized("alert", SettingsWidgetAlertsAlert.serializer())
            return cheerBits
        }
    }

    object Serializer : KSerializer<SettingsWidgetAlertsCheerBitsAlert> by JsonObjectSerializer(
        "SettingsWidgetAlertsCheerBitsAlert",
        { it.encode() },
        { decode(it) },
    )

    fun clone(): SettingsWidgetAlertsCheerBitsAlert {
        val new = SettingsWidgetAlertsCheerBitsAlert()
        new.bits = bits
        new.comparisonOperator = comparisonOperator
        new.alert = alert.clone()
        return new
    }
}

@Serializable(with = SettingsWidgetAlertsKickGiftsAlert.Serializer::class)
class SettingsWidgetAlertsKickGiftsAlert(
    var id: UUID = UUID.randomUUID(),
    var amount: Int = 1,
    var comparisonOperator: SettingsWidgetAlertsCheerBitsAlertOperator =
        SettingsWidgetAlertsCheerBitsAlertOperator.greaterEqual,
    var alert: SettingsWidgetAlertsAlert = SettingsWidgetAlertsAlert()
) {
    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("amount", amount)
        encode("comparisonOperator", comparisonOperator, SettingsWidgetAlertsCheerBitsAlertOperator.serializer())
        encode("alert", alert, SettingsWidgetAlertsAlert.serializer())
    }

    companion object {
        fun decode(container: JsonObject): SettingsWidgetAlertsKickGiftsAlert {
            val gift = SettingsWidgetAlertsKickGiftsAlert()
            gift.id = container.decodeSynthesized("id")
            gift.amount = container.decodeSynthesized("amount")
            gift.comparisonOperator = container.decodeSynthesized(
                "comparisonOperator",
                SettingsWidgetAlertsCheerBitsAlertOperator.serializer()
            )
            gift.alert = container.decodeSynthesized("alert", SettingsWidgetAlertsAlert.serializer())
            return gift
        }
    }

    object Serializer : KSerializer<SettingsWidgetAlertsKickGiftsAlert> by JsonObjectSerializer(
        "SettingsWidgetAlertsKickGiftsAlert",
        { it.encode() },
        { decode(it) },
    )

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

@Serializable(with = SettingsWidgetAlertsTwitch.Serializer::class)
class SettingsWidgetAlertsTwitch(
    var follows: SettingsWidgetAlertsAlert = SettingsWidgetAlertsAlert(),
    var subscriptions: SettingsWidgetAlertsAlert = SettingsWidgetAlertsAlert(),
    var raids: SettingsWidgetAlertsAlert = SettingsWidgetAlertsAlert(),
    var cheers: SettingsWidgetAlertsAlert = SettingsWidgetAlertsAlert(),
    cheerBits: List<SettingsWidgetAlertsCheerBitsAlert> = createDefaultCheerBits()
) {
    var cheerBits: List<SettingsWidgetAlertsCheerBitsAlert> by Published(cheerBits)
    var redemptions: List<SettingsWidgetAlertsAlert> = emptyList()

    fun encode(): JsonObject = encodeContainer {
        encode("follows", follows, SettingsWidgetAlertsAlert.serializer())
        encode("subscriptions", subscriptions, SettingsWidgetAlertsAlert.serializer())
        encode("raids", raids, SettingsWidgetAlertsAlert.serializer())
        encode("cheers", cheers, SettingsWidgetAlertsAlert.serializer())
        encode("cheerBits", cheerBits, ListSerializer(SettingsWidgetAlertsCheerBitsAlert.serializer()))
    }

    companion object {
        fun decode(container: JsonObject): SettingsWidgetAlertsTwitch {
            val twitch = SettingsWidgetAlertsTwitch()
            twitch.follows = container.decode("follows", SettingsWidgetAlertsAlert.serializer(), SettingsWidgetAlertsAlert())
            twitch.subscriptions = container.decode(
                "subscriptions",
                SettingsWidgetAlertsAlert.serializer(),
                SettingsWidgetAlertsAlert()
            )
            twitch.raids = container.decode("raids", SettingsWidgetAlertsAlert.serializer(), SettingsWidgetAlertsAlert())
            twitch.cheers = container.decode("cheers", SettingsWidgetAlertsAlert.serializer(), SettingsWidgetAlertsAlert())
            twitch.cheerBits = container.decode(
                "cheerBits",
                ListSerializer(SettingsWidgetAlertsCheerBitsAlert.serializer()),
                createDefaultCheerBits()
            )
            return twitch
        }
    }

    object Serializer : KSerializer<SettingsWidgetAlertsTwitch> by JsonObjectSerializer(
        "SettingsWidgetAlertsTwitch",
        { it.encode() },
        { decode(it) },
    )

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

@Serializable(with = SettingsWidgetAlertsKick.Serializer::class)
class SettingsWidgetAlertsKick(
    var subscriptions: SettingsWidgetAlertsAlert = SettingsWidgetAlertsAlert(),
    var giftedSubscriptions: SettingsWidgetAlertsAlert = SettingsWidgetAlertsAlert(),
    var hosts: SettingsWidgetAlertsAlert = SettingsWidgetAlertsAlert(),
    var rewards: SettingsWidgetAlertsAlert = SettingsWidgetAlertsAlert(),
    kickGifts: List<SettingsWidgetAlertsKickGiftsAlert> = createDefaultKickGifts()
) {
    var kickGifts: List<SettingsWidgetAlertsKickGiftsAlert> by Published(kickGifts)

    fun encode(): JsonObject = encodeContainer {
        encode("subscriptions", subscriptions, SettingsWidgetAlertsAlert.serializer())
        encode("giftedSubscriptions", giftedSubscriptions, SettingsWidgetAlertsAlert.serializer())
        encode("hosts", hosts, SettingsWidgetAlertsAlert.serializer())
        encode("rewards", rewards, SettingsWidgetAlertsAlert.serializer())
        encode("kickGifts", kickGifts, ListSerializer(SettingsWidgetAlertsKickGiftsAlert.serializer()))
    }

    companion object {
        fun decode(container: JsonObject): SettingsWidgetAlertsKick {
            val kick = SettingsWidgetAlertsKick()
            kick.subscriptions = container.decode(
                "subscriptions",
                SettingsWidgetAlertsAlert.serializer(),
                SettingsWidgetAlertsAlert()
            )
            kick.giftedSubscriptions = container.decode(
                "giftedSubscriptions",
                SettingsWidgetAlertsAlert.serializer(),
                SettingsWidgetAlertsAlert()
            )
            kick.hosts = container.decode("hosts", SettingsWidgetAlertsAlert.serializer(), SettingsWidgetAlertsAlert())
            kick.rewards = container.decode("rewards", SettingsWidgetAlertsAlert.serializer(), SettingsWidgetAlertsAlert())
            kick.kickGifts = container.decode(
                "kickGifts",
                ListSerializer(SettingsWidgetAlertsKickGiftsAlert.serializer()),
                createDefaultKickGifts()
            )
            return kick
        }
    }

    object Serializer : KSerializer<SettingsWidgetAlertsKick> by JsonObjectSerializer(
        "SettingsWidgetAlertsKick",
        { it.encode() },
        { decode(it) },
    )

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

@Serializable(with = SettingsWidgetAlertsChatBotCommand.Serializer::class)
class SettingsWidgetAlertsChatBotCommand(
    var id: UUID = UUID.randomUUID(),
    var name: String = "myname",
    var alert: SettingsWidgetAlertsAlert = SettingsWidgetAlertsAlert(),
    var imageType: SettingsWidgetAlertsChatBotCommandImageType =
        SettingsWidgetAlertsChatBotCommandImageType.file
) {
    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("name", name)
        encode("alert", alert, SettingsWidgetAlertsAlert.serializer())
        encode("imageType", imageType)
    }

    companion object {
        fun decode(container: JsonObject): SettingsWidgetAlertsChatBotCommand {
            val command = SettingsWidgetAlertsChatBotCommand()
            command.id = container.decode("id", UUID.randomUUID())
            command.name = container.decode("name", "myname")
            command.alert = container.decode("alert", SettingsWidgetAlertsAlert.serializer(), SettingsWidgetAlertsAlert())
            command.imageType = container.decode("imageType", SettingsWidgetAlertsChatBotCommandImageType.file)
            return command
        }
    }

    object Serializer : KSerializer<SettingsWidgetAlertsChatBotCommand> by JsonObjectSerializer(
        "SettingsWidgetAlertsChatBotCommand",
        { it.encode() },
        { decode(it) },
    )

    fun clone(): SettingsWidgetAlertsChatBotCommand {
        val new = SettingsWidgetAlertsChatBotCommand()
        new.name = name
        new.alert = alert.clone()
        new.imageType = imageType
        return new
    }
}

@Serializable(with = SettingsWidgetAlertsChatBot.Serializer::class)
class SettingsWidgetAlertsChatBot(
    commands: List<SettingsWidgetAlertsChatBotCommand> = emptyList()
) {
    var commands: List<SettingsWidgetAlertsChatBotCommand> by Published(commands)

    fun encode(): JsonObject = encodeContainer {
        encode("commands", commands, ListSerializer(SettingsWidgetAlertsChatBotCommand.serializer()))
    }

    companion object {
        fun decode(container: JsonObject): SettingsWidgetAlertsChatBot {
            val chatBot = SettingsWidgetAlertsChatBot()
            chatBot.commands = container.decode(
                "commands",
                ListSerializer(SettingsWidgetAlertsChatBotCommand.serializer()),
                emptyList()
            )
            return chatBot
        }
    }

    object Serializer : KSerializer<SettingsWidgetAlertsChatBot> by JsonObjectSerializer(
        "SettingsWidgetAlertsChatBot",
        { it.encode() },
        { decode(it) },
    )

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

@Serializable(with = SettingsWidgetAlertsSpeechToTextString.Serializer::class)
class SettingsWidgetAlertsSpeechToTextString(
    var id: UUID = UUID.randomUUID(),
    var string: String = "",
    var alert: SettingsWidgetAlertsAlert = SettingsWidgetAlertsAlert()
) {
    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("string", string)
        encode("alert", alert, SettingsWidgetAlertsAlert.serializer())
    }

    companion object {
        fun decode(container: JsonObject): SettingsWidgetAlertsSpeechToTextString {
            val string = SettingsWidgetAlertsSpeechToTextString()
            string.id = container.decodeSynthesized("id")
            string.string = container.decodeSynthesized("string")
            string.alert = container.decodeSynthesized("alert", SettingsWidgetAlertsAlert.serializer())
            return string
        }
    }

    object Serializer : KSerializer<SettingsWidgetAlertsSpeechToTextString> by JsonObjectSerializer(
        "SettingsWidgetAlertsSpeechToTextString",
        { it.encode() },
        { decode(it) },
    )

    fun clone(): SettingsWidgetAlertsSpeechToTextString {
        val new = SettingsWidgetAlertsSpeechToTextString()
        new.id = id
        new.string = string
        new.alert = alert.clone()
        return new
    }
}

@Serializable(with = SettingsWidgetAlertsSpeechToText.Serializer::class)
class SettingsWidgetAlertsSpeechToText(
    strings: List<SettingsWidgetAlertsSpeechToTextString> = emptyList()
) {
    var strings: List<SettingsWidgetAlertsSpeechToTextString> by Published(strings)

    fun encode(): JsonObject = encodeContainer {
        encode("strings", strings, ListSerializer(SettingsWidgetAlertsSpeechToTextString.serializer()))
    }

    companion object {
        fun decode(container: JsonObject): SettingsWidgetAlertsSpeechToText {
            val speechToText = SettingsWidgetAlertsSpeechToText()
            speechToText.strings = container.decode(
                "strings",
                ListSerializer(SettingsWidgetAlertsSpeechToTextString.serializer()),
                emptyList()
            )
            return speechToText
        }
    }

    object Serializer : KSerializer<SettingsWidgetAlertsSpeechToText> by JsonObjectSerializer(
        "SettingsWidgetAlertsSpeechToText",
        { it.encode() },
        { decode(it) },
    )

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

@Serializable(with = SettingsTtsMonster.Serializer::class)
class SettingsTtsMonster(
    apiToken: String = ""
) {
    var apiToken: String by Published(apiToken)

    fun encode(): JsonObject = encodeContainer {
        encode("apiToken", apiToken)
    }

    companion object {
        fun decode(container: JsonObject): SettingsTtsMonster {
            val ttsMonster = SettingsTtsMonster()
            ttsMonster.apiToken = container.decode("apiToken", "")
            return ttsMonster
        }
    }

    object Serializer : KSerializer<SettingsTtsMonster> by JsonObjectSerializer(
        "SettingsTtsMonster",
        { it.encode() },
        { decode(it) },
    )

    fun clone(): SettingsTtsMonster {
        val new = SettingsTtsMonster()
        new.apiToken = apiToken
        return new
    }
}

@Serializable(with = SettingsWidgetAlerts.Serializer::class)
class SettingsWidgetAlerts(
    var twitch: SettingsWidgetAlertsTwitch = SettingsWidgetAlertsTwitch(),
    var kick: SettingsWidgetAlertsKick = SettingsWidgetAlertsKick(),
    var chatBot: SettingsWidgetAlertsChatBot = SettingsWidgetAlertsChatBot(),
    var speechToText: SettingsWidgetAlertsSpeechToText = SettingsWidgetAlertsSpeechToText(),
    var needsSubtitles: Boolean = false,
    var ai: SettingsOpenAi = SettingsOpenAi(personality = aiPersonality),
    aiEnabled: Boolean = false,
    var ttsMonster: SettingsTtsMonster = SettingsTtsMonster()
) {
    var quickButton: SettingsWidgetAlertsAlert = SettingsWidgetAlertsAlert()
    var aiEnabled: Boolean by Published(aiEnabled)

    fun encode(): JsonObject = encodeContainer {
        encode("twitch", twitch, SettingsWidgetAlertsTwitch.serializer())
        encode("kick", kick, SettingsWidgetAlertsKick.serializer())
        encode("chatBot", chatBot, SettingsWidgetAlertsChatBot.serializer())
        encode("speechToText", speechToText, SettingsWidgetAlertsSpeechToText.serializer())
        encode("needsSubtitles", needsSubtitles)
        encode("ai", ai, SettingsOpenAi.serializer())
        encode("aiEnabled", aiEnabled)
        encode("ttsMonster", ttsMonster, SettingsTtsMonster.serializer())
    }

    companion object {
        private const val aiPersonality =
            "You are rude and gives insulting answers. Answer in a few sentences."

        fun decode(container: JsonObject): SettingsWidgetAlerts {
            val alerts = SettingsWidgetAlerts()
            alerts.twitch = container.decode("twitch", SettingsWidgetAlertsTwitch.serializer(), SettingsWidgetAlertsTwitch())
            alerts.kick = container.decode("kick", SettingsWidgetAlertsKick.serializer(), SettingsWidgetAlertsKick())
            alerts.chatBot = container.decode(
                "chatBot",
                SettingsWidgetAlertsChatBot.serializer(),
                SettingsWidgetAlertsChatBot()
            )
            alerts.speechToText = container.decode(
                "speechToText",
                SettingsWidgetAlertsSpeechToText.serializer(),
                SettingsWidgetAlertsSpeechToText()
            )
            alerts.needsSubtitles = container.decode("needsSubtitles", false)
            alerts.ai = container.decode(
                "ai",
                SettingsOpenAi.serializer(),
                SettingsOpenAi(personality = aiPersonality)
            )
            alerts.aiEnabled = container.decode("aiEnabled", false)
            alerts.ttsMonster = container.decode("ttsMonster", SettingsTtsMonster.serializer(), SettingsTtsMonster())
            return alerts
        }
    }

    object Serializer : KSerializer<SettingsWidgetAlerts> by JsonObjectSerializer(
        "SettingsWidgetAlerts",
        { it.encode() },
        { decode(it) },
    )

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

    fun toVideoUnit(): SceneSwitchTransition = when (this) {
        blur -> SceneSwitchTransition.BLUR
        freeze -> SceneSwitchTransition.FREEZE
        blurAndZoom -> SceneSwitchTransition.BLUR_AND_ZOOM
    }
    companion object {
        fun fromRawValue(rawValue: String): SettingsSceneSwitchTransition? =
            entries.firstOrNull { it.rawValue == rawValue }
    }
}

@Serializable(with = SettingsSensitivity.Serializer::class)
data class SettingsSensitivity(
    val mouth: Double = 1.0,
    val eyes: Double = 1.0
) {
    fun encode(): JsonObject = encodeContainer {
        encode("mouth", mouth)
        encode("eyes", eyes)
    }

    companion object {
        fun decode(container: JsonObject): SettingsSensitivity {
            val mouth: Double = container.decodeSynthesized("mouth")
            val eyes: Double = container.decodeSynthesized("eyes")
            return SettingsSensitivity(mouth = mouth, eyes = eyes)
        }
    }

    object Serializer : KSerializer<SettingsSensitivity> by JsonObjectSerializer(
        "SettingsSensitivity",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable
enum class SettingsWidgetVTuberType(val rawValue: String) {
    @SerialName("VRM") vrm("VRM"),
    @SerialName("Live2D") live2D("Live2D");

    companion object {
        fun fromRawValue(rawValue: String): SettingsWidgetVTuberType? =
            entries.firstOrNull { it.rawValue == rawValue }
    }
}

@Serializable(with = SettingsWidgetVTuber.Serializer::class)
class SettingsWidgetVTuber(
    var id: UUID = UUID.randomUUID(),
    type: SettingsWidgetVTuberType = SettingsWidgetVTuberType.vrm,
    videoSource: SettingsVideoSource = SettingsVideoSource(),
    cameraPositionY: Double = 1.37,
    cameraFieldOfView: Double = 18.0,
    modelName: String = "",
    mirror: Boolean = false,
    sensitivity: SettingsSensitivity = SettingsSensitivity(),
    armsAngle: Double = 72.0
) {
    var type: SettingsWidgetVTuberType by Published(type)
    var videoSource: SettingsVideoSource by Published(videoSource)
    var cameraPositionY: Double by Published(cameraPositionY)
    var cameraFieldOfView: Double by Published(cameraFieldOfView)
    var modelName: String by Published(modelName)
    var mirror: Boolean by Published(mirror)
    var sensitivity: SettingsSensitivity by Published(sensitivity)
    var armsAngle: Double by Published(armsAngle)

    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("type", type)
        encode("cameraPosition", videoSource.cameraPosition, SettingsSceneCameraPosition.serializer())
        encode("backCameraId", videoSource.backCameraId)
        encode("frontCameraId", videoSource.frontCameraId)
        encode("rtmpCameraId", videoSource.rtmpCameraId)
        encode("srtlaCameraId", videoSource.srtlaCameraId)
        encode("srtClientCameraId", videoSource.srtClientCameraId)
        encode("ristCameraId", videoSource.ristCameraId)
        encode("rtspCameraId", videoSource.rtspCameraId)
        encode("whipCameraId", videoSource.whipCameraId)
        encode("whepCameraId", videoSource.whepCameraId)
        encode("mediaPlayerCameraId", videoSource.mediaPlayerCameraId)
        encode("externalCameraId", videoSource.externalCameraId)
        encode("externalCameraName", videoSource.externalCameraName)
        encode("cameraPositionY", cameraPositionY)
        encode("cameraFieldOfView", cameraFieldOfView)
        encode("modelName", modelName)
        encode("mirror", mirror)
        encode("sensitivity", sensitivity, SettingsSensitivity.serializer())
        encode("armsAngle", armsAngle)
    }

    companion object {
        fun decode(container: JsonObject): SettingsWidgetVTuber {
            val vTuber = SettingsWidgetVTuber()
            vTuber.id = container.decode("id", UUID.randomUUID())
            vTuber.type = container.decode("type", SettingsWidgetVTuberType.vrm)
            vTuber.videoSource = SettingsVideoSource(
                cameraPosition = decodeCameraPosition(container, "cameraPosition", SettingsSceneCameraPosition.none),
                backCameraId = decodeCameraId(container, "backCameraId", bestBackCameraId),
                frontCameraId = decodeCameraId(container, "frontCameraId", bestFrontCameraId),
                rtmpCameraId = container.decode("rtmpCameraId", UUID.randomUUID()),
                srtlaCameraId = container.decode("srtlaCameraId", UUID.randomUUID()),
                srtClientCameraId = container.decode("srtClientCameraId", UUID.randomUUID()),
                ristCameraId = container.decode("ristCameraId", UUID.randomUUID()),
                rtspCameraId = container.decode("rtspCameraId", UUID.randomUUID()),
                whipCameraId = container.decode("whipCameraId", UUID.randomUUID()),
                whepCameraId = container.decode("whepCameraId", UUID.randomUUID()),
                mediaPlayerCameraId = container.decode("mediaPlayerCameraId", UUID.randomUUID()),
                externalCameraId = container.decode("externalCameraId", ""),
                externalCameraName = container.decode("externalCameraName", ""),
            )
            vTuber.cameraPositionY = container.decode("cameraPositionY", 1.37)
            vTuber.cameraFieldOfView = container.decode("cameraFieldOfView", 18.0)
            vTuber.modelName = container.decode("modelName", "")
            vTuber.mirror = container.decode("mirror", false)
            vTuber.sensitivity = container.decode("sensitivity", SettingsSensitivity.serializer(), SettingsSensitivity())
            vTuber.armsAngle = container.decode("armsAngle", 72.0)
            return vTuber
        }
    }

    object Serializer : KSerializer<SettingsWidgetVTuber> by JsonObjectSerializer(
        "SettingsWidgetVTuber",
        { it.encode() },
        { decode(it) },
    )

    fun toCameraId(): SettingsCameraId = videoSource.toCameraId()

    fun updateCameraId(settingsCameraId: SettingsCameraId) {
        videoSource = videoSource.updatingCameraId(settingsCameraId)
    }
}

@Serializable(with = SettingsWidgetPngTuber.Serializer::class)
class SettingsWidgetPngTuber(
    var id: UUID = UUID.randomUUID(),
    videoSource: SettingsVideoSource = SettingsVideoSource(),
    modelName: String = "",
    mirror: Boolean = false,
    sensitivity: SettingsSensitivity = SettingsSensitivity()
) {
    var videoSource: SettingsVideoSource by Published(videoSource)
    var modelName: String by Published(modelName)
    var mirror: Boolean by Published(mirror)
    var sensitivity: SettingsSensitivity by Published(sensitivity)

    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("cameraPosition", videoSource.cameraPosition, SettingsSceneCameraPosition.serializer())
        encode("backCameraId", videoSource.backCameraId)
        encode("frontCameraId", videoSource.frontCameraId)
        encode("rtmpCameraId", videoSource.rtmpCameraId)
        encode("srtlaCameraId", videoSource.srtlaCameraId)
        encode("srtClientCameraId", videoSource.srtClientCameraId)
        encode("ristCameraId", videoSource.ristCameraId)
        encode("rtspCameraId", videoSource.rtspCameraId)
        encode("whipCameraId", videoSource.whipCameraId)
        encode("whepCameraId", videoSource.whepCameraId)
        encode("mediaPlayerCameraId", videoSource.mediaPlayerCameraId)
        encode("externalCameraId", videoSource.externalCameraId)
        encode("externalCameraName", videoSource.externalCameraName)
        encode("modelName", modelName)
        encode("mirror", mirror)
        encode("sensitivity", sensitivity, SettingsSensitivity.serializer())
    }

    companion object {
        fun decode(container: JsonObject): SettingsWidgetPngTuber {
            val pngTuber = SettingsWidgetPngTuber()
            pngTuber.id = container.decode("id", UUID.randomUUID())
            pngTuber.videoSource = SettingsVideoSource(
                cameraPosition = decodeCameraPosition(container, "cameraPosition", SettingsSceneCameraPosition.none),
                backCameraId = decodeCameraId(container, "backCameraId", bestBackCameraId),
                frontCameraId = decodeCameraId(container, "frontCameraId", bestFrontCameraId),
                rtmpCameraId = container.decode("rtmpCameraId", UUID.randomUUID()),
                srtlaCameraId = container.decode("srtlaCameraId", UUID.randomUUID()),
                srtClientCameraId = container.decode("srtClientCameraId", UUID.randomUUID()),
                ristCameraId = container.decode("ristCameraId", UUID.randomUUID()),
                rtspCameraId = container.decode("rtspCameraId", UUID.randomUUID()),
                whipCameraId = container.decode("whipCameraId", UUID.randomUUID()),
                whepCameraId = container.decode("whepCameraId", UUID.randomUUID()),
                mediaPlayerCameraId = container.decode("mediaPlayerCameraId", UUID.randomUUID()),
                externalCameraId = container.decode("externalCameraId", ""),
                externalCameraName = container.decode("externalCameraName", ""),
            )
            pngTuber.modelName = container.decode("modelName", "")
            pngTuber.mirror = container.decode("mirror", false)
            pngTuber.sensitivity = container.decode(
                "sensitivity",
                SettingsSensitivity.serializer(),
                SettingsSensitivity()
            )
            return pngTuber
        }
    }

    object Serializer : KSerializer<SettingsWidgetPngTuber> by JsonObjectSerializer(
        "SettingsWidgetPngTuber",
        { it.encode() },
        { decode(it) },
    )

    fun toCameraId(): SettingsCameraId = videoSource.toCameraId()

    fun updateCameraId(settingsCameraId: SettingsCameraId) {
        videoSource = videoSource.updatingCameraId(settingsCameraId)
    }
}

@Serializable(with = SettingsWidgetSnapshot.Serializer::class)
class SettingsWidgetSnapshot(
    var id: UUID = UUID.randomUUID(),
    showtime: Int = 5
) {
    var showtime: Int by Published(showtime)

    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("showtime", showtime)
    }

    companion object {
        fun decode(container: JsonObject): SettingsWidgetSnapshot {
            val snapshot = SettingsWidgetSnapshot()
            snapshot.id = container.decode("id", UUID.randomUUID())
            snapshot.showtime = container.decode("showtime", 5)
            return snapshot
        }
    }

    object Serializer : KSerializer<SettingsWidgetSnapshot> by JsonObjectSerializer(
        "SettingsWidgetSnapshot",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsWidgetChat.Serializer::class)
class SettingsWidgetChat(
    var id: UUID = UUID.randomUUID(),
    fontSize: Float = 19.0f,
    var usernameColor: RgbColor = RgbColor(red = 255, green = 163, blue = 0),
    var messageColor: RgbColor = RgbColor(red = 255, green = 255, blue = 255),
    var backgroundColor: RgbColor = RgbColor(red = 0, green = 0, blue = 0),
    backgroundColorEnabled: Boolean = false,
    var shadowColor: RgbColor = RgbColor(red = 0, green = 0, blue = 0),
    shadowColorEnabled: Boolean = true,
    boldUsername: Boolean = true,
    boldMessage: Boolean = true,
    badges: Boolean = true,
    displayStyle: SettingsChatDisplayStyle = SettingsChatDisplayStyle.internationalNameAndUsername,
    sharedChatIcons: Boolean = false,
    height: Float = 1f,
    maximumNumberOfMessages: Int = 5
) {
    var fontSize: Float by Published(fontSize)
    var usernameColorColor: Color by Published(usernameColor.color())
    var messageColorColor: Color by Published(messageColor.color())
    var backgroundColorColor: Color by Published(backgroundColor.color())
    var backgroundColorEnabled: Boolean by Published(backgroundColorEnabled)
    var shadowColorColor: Color by Published(shadowColor.color())
    var shadowColorEnabled: Boolean by Published(shadowColorEnabled)
    var boldUsername: Boolean by Published(boldUsername)
    var boldMessage: Boolean by Published(boldMessage)
    var badges: Boolean by Published(badges)
    val nicknames: SettingsChatNicknames = SettingsChatNicknames()
    var displayStyle: SettingsChatDisplayStyle by Published(displayStyle)
    var sharedChatIcons: Boolean by Published(sharedChatIcons)
    var height: Float by Published(height)
    var maximumNumberOfMessages: Int by Published(maximumNumberOfMessages)

    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("fontSize", fontSize)
        encode("usernameColor", usernameColor)
        encode("messageColor", messageColor)
        encode("backgroundColor", backgroundColor)
        encode("backgroundColorEnabled", backgroundColorEnabled)
        encode("shadowColor", shadowColor)
        encode("shadowColorEnabled", shadowColorEnabled)
        encode("boldUsername", boldUsername)
        encode("boldMessage", boldMessage)
        encode("badges", badges)
        encode("displayStyle", displayStyle)
        encode("sharedChatIcons", sharedChatIcons)
        encode("height", height)
        encode("maximumNumberOfMessages", maximumNumberOfMessages)
    }

    companion object {
        fun decode(container: JsonObject): SettingsWidgetChat {
            val chat = SettingsWidgetChat()
            chat.id = container.decode("id", UUID.randomUUID())
            chat.fontSize = container.decode("fontSize", 19.0f)
            chat.usernameColor = container.decode("usernameColor", RgbColor(red = 255, green = 163, blue = 0))
            chat.usernameColorColor = chat.usernameColor.color()
            chat.messageColor = container.decode("messageColor", RgbColor(red = 255, green = 255, blue = 255))
            chat.messageColorColor = chat.messageColor.color()
            chat.backgroundColor = container.decode("backgroundColor", RgbColor(red = 0, green = 0, blue = 0))
            chat.backgroundColorColor = chat.backgroundColor.color()
            chat.backgroundColorEnabled = container.decode("backgroundColorEnabled", false)
            chat.shadowColor = container.decode("shadowColor", RgbColor(red = 0, green = 0, blue = 0))
            chat.shadowColorColor = chat.shadowColor.color()
            chat.shadowColorEnabled = container.decode("shadowColorEnabled", true)
            chat.boldUsername = container.decode("boldUsername", true)
            chat.boldMessage = container.decode("boldMessage", true)
            chat.badges = container.decode("badges", true)
            chat.displayStyle = container.decode("displayStyle", SettingsChatDisplayStyle.internationalName)
            chat.sharedChatIcons = container.decode("sharedChatIcons", false)
            chat.height = container.decode("height", 1f)
            chat.maximumNumberOfMessages = container.decode("maximumNumberOfMessages", 5)
            return chat
        }
    }

    object Serializer : KSerializer<SettingsWidgetChat> by JsonObjectSerializer(
        "SettingsWidgetChat",
        { it.encode() },
        { decode(it) },
    )

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

@Serializable(with = SettingsWidgetSlideshowSlide.Serializer::class)
class SettingsWidgetSlideshowSlide(
    var id: UUID = UUID.randomUUID(),
    widgetId: UUID? = null,
    time: Int = 15
) {
    var widgetId: UUID? by Published(widgetId)
    var time: Int by Published(time)

    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("widgetId", widgetId)
        encode("time", time)
    }

    companion object {
        fun decode(container: JsonObject): SettingsWidgetSlideshowSlide {
            val slide = SettingsWidgetSlideshowSlide()
            slide.id = container.decode("id", UUID.randomUUID())
            slide.widgetId = container.decode<UUID>("widgetId", UUID.randomUUID())
            slide.time = container.decode("time", 0)
            return slide
        }
    }

    object Serializer : KSerializer<SettingsWidgetSlideshowSlide> by JsonObjectSerializer(
        "SettingsWidgetSlideshowSlide",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsWidgetSlideshow.Serializer::class)
class SettingsWidgetSlideshow(
    var id: UUID = UUID.randomUUID(),
    slides: List<SettingsWidgetSlideshowSlide> = emptyList()
) {
    var slides: List<SettingsWidgetSlideshowSlide> by Published(slides)

    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("slides", slides, ListSerializer(SettingsWidgetSlideshowSlide.serializer()))
    }

    companion object {
        fun decode(container: JsonObject): SettingsWidgetSlideshow {
            val slideshow = SettingsWidgetSlideshow()
            slideshow.id = container.decode("id", UUID.randomUUID())
            slideshow.slides = container.decode(
                "slides",
                ListSerializer(SettingsWidgetSlideshowSlide.serializer()),
                emptyList()
            )
            return slideshow
        }
    }

    object Serializer : KSerializer<SettingsWidgetSlideshow> by JsonObjectSerializer(
        "SettingsWidgetSlideshow",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsWidgetWheelOfLuckOption.Serializer::class)
class SettingsWidgetWheelOfLuckOption(
    var id: UUID = UUID.randomUUID(),
    text: String = "",
    weight: Int = 1
) {
    var text: String by Published(text)
    var weight: Int by Published(weight)

    override fun equals(other: Any?): Boolean =
        other is SettingsWidgetWheelOfLuckOption && id == other.id

    override fun hashCode(): Int = id.hashCode()

    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("text", text)
        encode("weight", weight)
    }

    companion object {
        fun decode(container: JsonObject): SettingsWidgetWheelOfLuckOption {
            val option = SettingsWidgetWheelOfLuckOption()
            option.id = container.decode("id", UUID.randomUUID())
            option.text = container.decode("text", "")
            option.weight = container.decode("weight", 1)
            return option
        }
    }

    object Serializer : KSerializer<SettingsWidgetWheelOfLuckOption> by JsonObjectSerializer(
        "SettingsWidgetWheelOfLuckOption",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsWidgetWheelOfLuck.Serializer::class)
class SettingsWidgetWheelOfLuck(
    advanced: Boolean = false,
    options: List<SettingsWidgetWheelOfLuckOption> = emptyList()
) {
    var advanced: Boolean by Published(advanced)
    var totalWeight: Int by Published(1)
    var options: List<SettingsWidgetWheelOfLuckOption> by Published(options)
    var text: String by Published("")

    init {
        updateTotalWeight()
        updateText()
    }

    fun encode(): JsonObject = encodeContainer {
        encode("advanced", advanced)
        encode("options", options, ListSerializer(SettingsWidgetWheelOfLuckOption.serializer()))
    }

    companion object {
        fun decode(container: JsonObject): SettingsWidgetWheelOfLuck {
            val wheelOfLuck = SettingsWidgetWheelOfLuck()
            wheelOfLuck.advanced = container.decode("advanced", false)
            wheelOfLuck.options = container.decode(
                "options",
                ListSerializer(SettingsWidgetWheelOfLuckOption.serializer()),
                emptyList()
            )
            wheelOfLuck.updateText()
            wheelOfLuck.updateTotalWeight()
            return wheelOfLuck
        }
    }

    object Serializer : KSerializer<SettingsWidgetWheelOfLuck> by JsonObjectSerializer(
        "SettingsWidgetWheelOfLuck",
        { it.encode() },
        { decode(it) },
    )

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

@Serializable(with = SettingsBingoCardSquare.Serializer::class)
data class SettingsBingoCardSquare(
    val text: String,
    val checked: Boolean
) {
    var id: UUID = UUID.randomUUID()

    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("text", text)
        encode("checked", checked)
    }

    companion object {
        fun decode(container: JsonObject): SettingsBingoCardSquare {
            val id: UUID = container.decodeSynthesized("id")
            val text: String = container.decodeSynthesized("text")
            val checked: Boolean = container.decodeSynthesized("checked")
            val square = SettingsBingoCardSquare(text = text, checked = checked)
            square.id = id
            return square
        }
    }

    object Serializer : KSerializer<SettingsBingoCardSquare> by JsonObjectSerializer(
        "SettingsBingoCardSquare",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsWidgetBingoCard.Serializer::class)
class SettingsWidgetBingoCard(
    var backgroundColor: RgbColor = baseBackgroundColor,
    var foregroundColor: RgbColor = baseForegroundColor,
    squares: List<SettingsBingoCardSquare> = emptyList()
) {
    var backgroundColorColor: Color by Published(baseBackgroundColor.color())
    var foregroundColorColor: Color by Published(baseForegroundColor.color())
    var squares: List<SettingsBingoCardSquare> by Published(squares)
    var squaresText: String by Published("")

    fun encode(): JsonObject = encodeContainer {
        encode("backgroundColor", backgroundColor)
        encode("foregroundColor", foregroundColor)
        encode("squares", squares, ListSerializer(SettingsBingoCardSquare.serializer()))
    }

    companion object {
        val baseBackgroundColor: RgbColor = RgbColor.black.withOpacity(0.75)
        val baseForegroundColor: RgbColor = RgbColor.white

        fun decode(container: JsonObject): SettingsWidgetBingoCard {
            val bingoCard = SettingsWidgetBingoCard()
            bingoCard.backgroundColor = container.decode("backgroundColor", baseBackgroundColor)
            bingoCard.backgroundColorColor = bingoCard.backgroundColor.color()
            bingoCard.foregroundColor = container.decode("foregroundColor", baseForegroundColor)
            bingoCard.foregroundColorColor = bingoCard.foregroundColor.color()
            bingoCard.squares = container.decode(
                "squares",
                ListSerializer(SettingsBingoCardSquare.serializer()),
                emptyList()
            )
            bingoCard.squaresText = bingoCard.squares.map { it.text }.joinToString("\n")
            return bingoCard
        }
    }

    object Serializer : KSerializer<SettingsWidgetBingoCard> by JsonObjectSerializer(
        "SettingsWidgetBingoCard",
        { it.encode() },
        { decode(it) },
    )

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

@Serializable(with = SettingsWidgetPomodoroTimer.Serializer::class)
class SettingsWidgetPomodoroTimer(
    focusDuration: Int = 30,
    breakDuration: Int = 5,
    width: Double = 1.6,
    focusName: String = "Focus",
    breakName: String = "Break",
    focusIcon: PomodoroFocusIcon = PomodoroFocusIcon.sun,
    breakIcon: PomodoroBreakIcon = PomodoroBreakIcon.cup,
    var backgroundColor: RgbColor = baseBackgroundColor,
    var foregroundColor: RgbColor = baseForegroundColor,
    var focusColor: RgbColor = baseFocusColor,
    var breakColor: RgbColor = baseBreakColor,
    focusToBreakSoundId: UUID? = null,
    breakToFocusSoundId: UUID? = null,
    focusToBreakChatMessage: String = "",
    breakToFocusChatMessage: String = ""
) {
    var focusDuration: Int by Published(focusDuration)
    var breakDuration: Int by Published(breakDuration)
    var width: Double by Published(width)
    var focusName: String by Published(focusName)
    var breakName: String by Published(breakName)
    var focusIcon: PomodoroFocusIcon by Published(focusIcon)
    var breakIcon: PomodoroBreakIcon by Published(breakIcon)
    var backgroundColorColor: Color by Published(baseBackgroundColor.color())
    var foregroundColorColor: Color by Published(baseForegroundColor.color())
    var focusColorColor: Color by Published(baseFocusColor.color())
    var breakColorColor: Color by Published(baseBreakColor.color())
    var isRunning: Boolean by Published(false)
    var phase: PomodoroPhase by Published(PomodoroPhase.focus)
    var secondsRemaining: Int by Published(30 * 60)
    var focusToBreakSoundId: UUID? by Published(focusToBreakSoundId)
    var breakToFocusSoundId: UUID? by Published(breakToFocusSoundId)
    var focusToBreakChatMessage: String by Published(focusToBreakChatMessage)
    var breakToFocusChatMessage: String by Published(breakToFocusChatMessage)
    var onPhaseChanged: ((PomodoroPhase) -> Unit)? = null
    private var timer = MainTimer()

    fun encode(): JsonObject = encodeContainer {
        encode("focusDuration", focusDuration)
        encode("breakDuration", breakDuration)
        encode("width", width)
        encode("focusName", focusName)
        encode("breakName", breakName)
        encode("focusIcon", focusIcon)
        encode("breakIcon", breakIcon)
        encode("backgroundColor", backgroundColor)
        encode("foregroundColor", foregroundColor)
        encode("focusColor", focusColor)
        encode("breakColor", breakColor)
        encode("focusToBreakSoundId", focusToBreakSoundId)
        encode("breakToFocusSoundId", breakToFocusSoundId)
        encode("focusToBreakChatMessage", focusToBreakChatMessage)
        encode("breakToFocusChatMessage", breakToFocusChatMessage)
    }

    companion object {
        val baseBackgroundColor: RgbColor = RgbColor.black.withOpacity(0.75)
        val baseForegroundColor: RgbColor = RgbColor.white
        val baseFocusColor: RgbColor = RgbColor(red = 122, green = 181, blue = 255)
        val baseBreakColor: RgbColor = RgbColor(red = 103, green = 208, blue = 69)

        fun decode(container: JsonObject): SettingsWidgetPomodoroTimer {
            val pomodoroTimer = SettingsWidgetPomodoroTimer()
            pomodoroTimer.focusDuration = container.decode("focusDuration", 30)
            pomodoroTimer.breakDuration = container.decode("breakDuration", 5)
            pomodoroTimer.width = container.decode("width", 1.6)
            pomodoroTimer.focusName = container.decode("focusName", "Focus")
            pomodoroTimer.breakName = container.decode("breakName", "Break")
            pomodoroTimer.focusIcon = container.decode("focusIcon", PomodoroFocusIcon.sun)
            pomodoroTimer.breakIcon = container.decode("breakIcon", PomodoroBreakIcon.cup)
            pomodoroTimer.backgroundColor = container.decode("backgroundColor", baseBackgroundColor)
            pomodoroTimer.backgroundColorColor = pomodoroTimer.backgroundColor.color()
            pomodoroTimer.foregroundColor = container.decode("foregroundColor", baseForegroundColor)
            pomodoroTimer.foregroundColorColor = pomodoroTimer.foregroundColor.color()
            pomodoroTimer.focusColor = container.decode("focusColor", baseFocusColor)
            pomodoroTimer.focusColorColor = pomodoroTimer.focusColor.color()
            pomodoroTimer.breakColor = container.decode("breakColor", baseBreakColor)
            pomodoroTimer.breakColorColor = pomodoroTimer.breakColor.color()
            pomodoroTimer.focusToBreakSoundId = container.decode<UUID?>("focusToBreakSoundId", null)
            pomodoroTimer.breakToFocusSoundId = container.decode<UUID?>("breakToFocusSoundId", null)
            pomodoroTimer.focusToBreakChatMessage = container.decode("focusToBreakChatMessage", "")
            pomodoroTimer.breakToFocusChatMessage = container.decode("breakToFocusChatMessage", "")
            pomodoroTimer.secondsRemaining = pomodoroTimer.focusDuration * 60
            return pomodoroTimer
        }
    }

    object Serializer : KSerializer<SettingsWidgetPomodoroTimer> by JsonObjectSerializer(
        "SettingsWidgetPomodoroTimer",
        { it.encode() },
        { decode(it) },
    )

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

@Serializable(with = SettingsWidgetChatEmoteCombo.Serializer::class)
class SettingsWidgetChatEmoteCombo(
    minimumCombo: Int = 3,
    resetAfter: Int = 5
) {
    var minimumCombo: Int by Published(minimumCombo)
    var resetAfter: Int by Published(resetAfter)

    fun encode(): JsonObject = encodeContainer {
        encode("minimumCombo", minimumCombo)
        encode("resetAfter", resetAfter)
    }

    companion object {
        fun decode(container: JsonObject): SettingsWidgetChatEmoteCombo {
            val chatEmoteCombo = SettingsWidgetChatEmoteCombo()
            chatEmoteCombo.minimumCombo = container.decode("minimumCombo", 3)
            chatEmoteCombo.resetAfter = container.decode("resetAfter", 5)
            return chatEmoteCombo
        }
    }

    object Serializer : KSerializer<SettingsWidgetChatEmoteCombo> by JsonObjectSerializer(
        "SettingsWidgetChatEmoteCombo",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsWidget.Serializer::class)
class SettingsWidget(
    name: String = baseName,
    var id: UUID = UUID.randomUUID(),
    type: SettingsWidgetType = SettingsWidgetType.text,
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
    enabled: Boolean = true,
    effects: List<SettingsVideoEffect> = emptyList()
) : Named {
    override var name: String by Published(name)
    var type: SettingsWidgetType by Published(type)
    var enabled: Boolean by Published(enabled)
    var effects: List<SettingsVideoEffect> by Published(effects)

    override fun equals(other: Any?): Boolean = other is SettingsWidget && id == other.id

    override fun hashCode(): Int = id.hashCode()

    fun encode(): JsonObject = encodeContainer {
        encode("name", name)
        encode("id", id)
        encode("type", type)
        encode("text", text, SettingsWidgetText.serializer())
        encode("browser", browser, SettingsWidgetBrowser.serializer())
        encode("crop", crop, SettingsWidgetCrop.serializer())
        encode("map", map, SettingsWidgetMap.serializer())
        encode("scene", scene, SettingsWidgetScene.serializer())
        encode("qrCode", qrCode, SettingsWidgetQrCode.serializer())
        encode("alerts", alerts, SettingsWidgetAlerts.serializer())
        encode("videoSource", videoSource, SettingsWidgetVideoSource.serializer())
        encode("scoreboard", scoreboard, SettingsWidgetScoreboard.serializer())
        encode("vTuber", vTuber, SettingsWidgetVTuber.serializer())
        encode("pngTuber", pngTuber, SettingsWidgetPngTuber.serializer())
        encode("snapshot", snapshot, SettingsWidgetSnapshot.serializer())
        encode("chat", chat, SettingsWidgetChat.serializer())
        encode("chatEmoteCombo", chatEmoteCombo, SettingsWidgetChatEmoteCombo.serializer())
        encode("slideshow", slideshow, SettingsWidgetSlideshow.serializer())
        encode("wheelOfLuck", wheelOfLuck, SettingsWidgetWheelOfLuck.serializer())
        encode("bingoCard", bingoCard, SettingsWidgetBingoCard.serializer())
        encode("pomodoroTimer", pomodoroTimer, SettingsWidgetPomodoroTimer.serializer())
        encode("enabled", enabled)
        encode("effects", effects, ListSerializer(SettingsVideoEffect.serializer()))
    }

    companion object {
        val baseName: String = localized("My widget")

        fun decode(container: JsonObject): SettingsWidget {
            val widget = SettingsWidget()
            widget.name = container.decode("name", baseName)
            widget.id = container.decode("id", UUID.randomUUID())
            widget.type = container.decode("type", SettingsWidgetType.text)
            widget.text = container.decode("text", SettingsWidgetText.serializer(), SettingsWidgetText())
            widget.browser = container.decode("browser", SettingsWidgetBrowser.serializer(), SettingsWidgetBrowser())
            widget.crop = container.decode("crop", SettingsWidgetCrop.serializer(), SettingsWidgetCrop())
            widget.map = container.decode("map", SettingsWidgetMap.serializer(), SettingsWidgetMap())
            widget.scene = container.decode("scene", SettingsWidgetScene.serializer(), SettingsWidgetScene())
            widget.qrCode = container.decode("qrCode", SettingsWidgetQrCode.serializer(), SettingsWidgetQrCode())
            widget.alerts = container.decode("alerts", SettingsWidgetAlerts.serializer(), SettingsWidgetAlerts())
            widget.videoSource = container.decode(
                "videoSource",
                SettingsWidgetVideoSource.serializer(),
                SettingsWidgetVideoSource()
            )
            widget.scoreboard = container.decode(
                "scoreboard",
                SettingsWidgetScoreboard.serializer(),
                SettingsWidgetScoreboard()
            )
            widget.vTuber = container.decode("vTuber", SettingsWidgetVTuber.serializer(), SettingsWidgetVTuber())
            widget.pngTuber = container.decode("pngTuber", SettingsWidgetPngTuber.serializer(), SettingsWidgetPngTuber())
            widget.snapshot = container.decode("snapshot", SettingsWidgetSnapshot.serializer(), SettingsWidgetSnapshot())
            widget.chat = container.decode("chat", SettingsWidgetChat.serializer(), SettingsWidgetChat())
            widget.chatEmoteCombo = container.decode(
                "chatEmoteCombo",
                SettingsWidgetChatEmoteCombo.serializer(),
                SettingsWidgetChatEmoteCombo()
            )
            widget.slideshow = container.decode(
                "slideshow",
                SettingsWidgetSlideshow.serializer(),
                SettingsWidgetSlideshow()
            )
            widget.wheelOfLuck = container.decode(
                "wheelOfLuck",
                SettingsWidgetWheelOfLuck.serializer(),
                SettingsWidgetWheelOfLuck()
            )
            widget.bingoCard = container.decode(
                "bingoCard",
                SettingsWidgetBingoCard.serializer(),
                SettingsWidgetBingoCard()
            )
            widget.pomodoroTimer = container.decode(
                "pomodoroTimer",
                SettingsWidgetPomodoroTimer.serializer(),
                SettingsWidgetPomodoroTimer()
            )
            widget.enabled = container.decode("enabled", true)
            widget.effects = container.decode("effects", ListSerializer(SettingsVideoEffect.serializer()), emptyList())
            widget.migrateFromOlderVersions()
            return widget
        }
    }

    object Serializer : KSerializer<SettingsWidget> by JsonObjectSerializer(
        "SettingsWidget",
        { it.encode() },
        { decode(it) },
    )

    private fun migrateFromOlderVersions() {
        if (type == SettingsWidgetType.videoSource &&
            effects.none { it.type == SettingsVideoEffectType.shape }
        ) {
            val shape = SettingsVideoEffectShape()
            shape.cornerRadius = 0f
            var updated = false
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
data class SettingsWidgetLayout(
    val x: Double = 0.0,
    val xString: String = "0.0",
    val y: Double = 0.0,
    val yString: String = "0.0",
    val size: Double = 100.0,
    val sizeString: String = "100.0",
    val alignment: SettingsAlignment = SettingsAlignment.topLeft,
    val positioningLock: Boolean = false
) {
    fun updatingXString(): SettingsWidgetLayout = copy(xString = x.toString())

    fun updatingYString(): SettingsWidgetLayout = copy(yString = y.toString())

    fun updatingSizeString(): SettingsWidgetLayout = copy(sizeString = size.toString())

    fun extent(): RectF = RectF(x.toFloat(), y.toFloat(), (x + size).toFloat(), (y + size).toFloat())
}

@Serializable(with = SettingsSceneWidget.Serializer::class)
class SettingsSceneWidget(
    widgetId: UUID = UUID.randomUUID(),
    var id: UUID = UUID.randomUUID(),
    layout: SettingsWidgetLayout = SettingsWidgetLayout(),
    width2: Double = 100.0,
    height2: Double = 100.0,
    var migrated: Boolean = true,
    var migrated2: Boolean = true
) {
    var widgetId: UUID by Published(widgetId)
    var layout: SettingsWidgetLayout by Published(layout)
    var width2: Double by Published(width2)
    var height2: Double by Published(height2)

    override fun equals(other: Any?): Boolean = other is SettingsSceneWidget && id == other.id

    override fun hashCode(): Int = id.hashCode()

    fun encode(): JsonObject = encodeContainer {
        encode("widgetId", widgetId)
        encode("id", id)
        encode("x", layout.x)
        encode("y", layout.y)
        encode("width", width2)
        encode("height", height2)
        encode("size", layout.size)
        encode("alignment", layout.alignment)
        encode("positioningLock", layout.positioningLock)
        encode("migrated", migrated)
        encode("migrated2", migrated2)
    }

    companion object {
        fun decode(container: JsonObject): SettingsSceneWidget {
            val sceneWidget = SettingsSceneWidget()
            sceneWidget.widgetId = container.decode("widgetId", UUID.randomUUID())
            sceneWidget.id = container.decode("id", UUID.randomUUID())
            val x = container.decode("x", 0.0)
            val y = container.decode("y", 0.0)
            sceneWidget.width2 = container.decode("width", 100.0)
            sceneWidget.height2 = container.decode("height", 100.0)
            val size = container.decode<Double?>("size", null)
                ?: container.decode("size", minOf(sceneWidget.width2, sceneWidget.height2))
            sceneWidget.layout = SettingsWidgetLayout(
                x = x,
                xString = x.toString(),
                y = y,
                yString = y.toString(),
                size = size,
                sizeString = size.toString(),
                alignment = container.decode("alignment", SettingsAlignment.topLeft),
                positioningLock = container.decode("positioningLock", false),
            )
            sceneWidget.migrated = container.decode("migrated", false)
            sceneWidget.migrated2 = container.decode("migrated2", false)
            return sceneWidget
        }
    }

    object Serializer : KSerializer<SettingsSceneWidget> by JsonObjectSerializer(
        "SettingsSceneWidget",
        { it.encode() },
        { decode(it) },
    )

    fun clone(): SettingsSceneWidget {
        val new = SettingsSceneWidget(widgetId = widgetId)
        new.layout = layout.copy()
        new.migrated = migrated
        new.migrated2 = migrated2
        return new
    }
}

@Serializable(with = SettingsSceneCameraPosition.Serializer::class)
enum class SettingsSceneCameraPosition(val rawValue: String) {
    back("Back"),
    front("Front"),
    rtmp("RTMP"),
    `external`("External"),
    srtla("SRT(LA)"),
    srtClient("SRT client"),
    rist("RIST"),
    rtsp("RTSP"),
    whip("WHIP"),
    whep("WHEP"),
    mediaPlayer("Media player"),
    screenCapture("Screen capture"),
    backTripleLowEnergy("Back triple"),
    backDualLowEnergy("Back dual"),
    backWideDualLowEnergy("Back wide dual"),
    none("None");

    fun isBuiltin(): Boolean = this in builtinCameraPositions

    companion object {
        fun fromRawValue(rawValue: String): SettingsSceneCameraPosition =
            entries.firstOrNull { it.rawValue == rawValue } ?: back
    }

    object Serializer : KSerializer<SettingsSceneCameraPosition> by rawValueSerializer(
        "com.moblin.android.various.settings.SettingsSceneCameraPosition",
        { it.rawValue },
        { fromRawValue(it) },
    )
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
    val cameraPosition: SettingsSceneCameraPosition = SettingsSceneCameraPosition.none,
    val backCameraId: CameraId = bestBackCameraId,
    val frontCameraId: CameraId = bestFrontCameraId,
    @Contextual val rtmpCameraId: UUID = UUID.randomUUID(),
    @Contextual val srtlaCameraId: UUID = UUID.randomUUID(),
    @Contextual val srtClientCameraId: UUID = UUID.randomUUID(),
    @Contextual val ristCameraId: UUID = UUID.randomUUID(),
    @Contextual val rtspCameraId: UUID = UUID.randomUUID(),
    @Contextual val whipCameraId: UUID = UUID.randomUUID(),
    @Contextual val whepCameraId: UUID = UUID.randomUUID(),
    @Contextual val mediaPlayerCameraId: UUID = UUID.randomUUID(),
    val externalCameraId: CameraId = "",
    val externalCameraName: String = ""
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

    fun updatingCameraId(settingsCameraId: SettingsCameraId): SettingsVideoSource = when (settingsCameraId) {
        is SettingsCameraId.Back ->
            copy(cameraPosition = SettingsSceneCameraPosition.back, backCameraId = settingsCameraId.id)
        is SettingsCameraId.Front ->
            copy(cameraPosition = SettingsSceneCameraPosition.front, frontCameraId = settingsCameraId.id)
        is SettingsCameraId.Rtmp ->
            copy(cameraPosition = SettingsSceneCameraPosition.rtmp, rtmpCameraId = settingsCameraId.id)
        is SettingsCameraId.Srtla ->
            copy(cameraPosition = SettingsSceneCameraPosition.srtla, srtlaCameraId = settingsCameraId.id)
        is SettingsCameraId.Srt ->
            copy(cameraPosition = SettingsSceneCameraPosition.srtClient, srtClientCameraId = settingsCameraId.id)
        is SettingsCameraId.Rist ->
            copy(cameraPosition = SettingsSceneCameraPosition.rist, ristCameraId = settingsCameraId.id)
        is SettingsCameraId.Rtsp ->
            copy(cameraPosition = SettingsSceneCameraPosition.rtsp, rtspCameraId = settingsCameraId.id)
        is SettingsCameraId.Whip ->
            copy(cameraPosition = SettingsSceneCameraPosition.whip, whipCameraId = settingsCameraId.id)
        is SettingsCameraId.Whep ->
            copy(cameraPosition = SettingsSceneCameraPosition.whep, whepCameraId = settingsCameraId.id)
        is SettingsCameraId.MediaPlayer -> copy(
            cameraPosition = SettingsSceneCameraPosition.mediaPlayer,
            mediaPlayerCameraId = settingsCameraId.id,
        )
        is SettingsCameraId.External -> copy(
            cameraPosition = SettingsSceneCameraPosition.`external`,
            externalCameraId = settingsCameraId.id,
            externalCameraName = settingsCameraId.name,
        )
        SettingsCameraId.ScreenCapture -> copy(cameraPosition = SettingsSceneCameraPosition.screenCapture)
        SettingsCameraId.BackTripleLowEnergy ->
            copy(cameraPosition = SettingsSceneCameraPosition.backTripleLowEnergy)
        SettingsCameraId.BackDualLowEnergy ->
            copy(cameraPosition = SettingsSceneCameraPosition.backDualLowEnergy)
        SettingsCameraId.BackWideDualLowEnergy ->
            copy(cameraPosition = SettingsSceneCameraPosition.backWideDualLowEnergy)
        SettingsCameraId.None -> copy(cameraPosition = SettingsSceneCameraPosition.none)
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

@Serializable(with = SettingsWidgetVideoSource.Serializer::class)
class SettingsWidgetVideoSource(
    cornerRadius: Float = 0f,
    videoSource: SettingsVideoSource = SettingsVideoSource(),
    var cropEnabled: Boolean = false,
    var cropX: Double = 0.25,
    var cropY: Double = 0.0,
    var cropWidth: Double = 0.5,
    var cropHeight: Double = 1.0,
    rotation: Double = 0.0,
    trackFaceEnabled: Boolean = false,
    trackFaceZoom: Double = 0.75,
    mirror: Boolean = false,
    borderWidth: Double = 0.0,
    var borderColor: RgbColor = RgbColor(red = 0, green = 0, blue = 0)
) {
    var cornerRadius: Float by Published(cornerRadius)
    var videoSource: SettingsVideoSource by Published(videoSource)
    var rotation: Double by Published(rotation)
    var trackFaceEnabled: Boolean by Published(trackFaceEnabled)
    var trackFaceZoom: Double by Published(trackFaceZoom)
    var mirror: Boolean by Published(mirror)
    var borderWidth: Double by Published(borderWidth)
    var borderColorColor: Color by Published(borderColor.color())

    fun encode(): JsonObject = encodeContainer {
        encode("cornerRadius", cornerRadius)
        encode("cameraPosition", videoSource.cameraPosition, SettingsSceneCameraPosition.serializer())
        encode("backCameraId", videoSource.backCameraId)
        encode("frontCameraId", videoSource.frontCameraId)
        encode("rtmpCameraId", videoSource.rtmpCameraId)
        encode("srtlaCameraId", videoSource.srtlaCameraId)
        encode("srtClientCameraId", videoSource.srtClientCameraId)
        encode("ristCameraId", videoSource.ristCameraId)
        encode("rtspCameraId", videoSource.rtspCameraId)
        encode("whipCameraId", videoSource.whipCameraId)
        encode("whepCameraId", videoSource.whepCameraId)
        encode("mediaPlayerCameraId", videoSource.mediaPlayerCameraId)
        encode("externalCameraId", videoSource.externalCameraId)
        encode("externalCameraName", videoSource.externalCameraName)
        encode("cropEnabled", cropEnabled)
        encode("cropX", cropX)
        encode("cropY", cropY)
        encode("cropWidth", cropWidth)
        encode("cropHeight", cropHeight)
        encode("rotation", rotation)
        encode("trackFaceEnabled", trackFaceEnabled)
        encode("trackFaceZoom", trackFaceZoom)
        encode("mirror", mirror)
        encode("borderWidth", borderWidth)
        encode("borderColor", borderColor)
    }

    companion object {
        fun decode(container: JsonObject): SettingsWidgetVideoSource {
            val videoSource = SettingsWidgetVideoSource()
            videoSource.cornerRadius = container.decode("cornerRadius", 0f)
            videoSource.videoSource = SettingsVideoSource(
                cameraPosition = decodeCameraPosition(container, "cameraPosition", SettingsSceneCameraPosition.none),
                backCameraId = decodeCameraId(container, "backCameraId", bestBackCameraId),
                frontCameraId = decodeCameraId(container, "frontCameraId", bestFrontCameraId),
                rtmpCameraId = container.decode("rtmpCameraId", UUID.randomUUID()),
                srtlaCameraId = container.decode("srtlaCameraId", UUID.randomUUID()),
                srtClientCameraId = container.decode("srtClientCameraId", UUID.randomUUID()),
                ristCameraId = container.decode("ristCameraId", UUID.randomUUID()),
                rtspCameraId = container.decode("rtspCameraId", UUID.randomUUID()),
                whipCameraId = container.decode("whipCameraId", UUID.randomUUID()),
                whepCameraId = container.decode("whepCameraId", UUID.randomUUID()),
                mediaPlayerCameraId = container.decode("mediaPlayerCameraId", UUID.randomUUID()),
                externalCameraId = container.decode("externalCameraId", ""),
                externalCameraName = container.decode("externalCameraName", ""),
            )
            videoSource.cropEnabled = container.decode("cropEnabled", false)
            videoSource.cropX = container.decode("cropX", 0.25)
            videoSource.cropY = container.decode("cropY", 0.0)
            videoSource.cropWidth = container.decode("cropWidth", 0.5)
            videoSource.cropHeight = container.decode("cropHeight", 1.0)
            videoSource.rotation = container.decode("rotation", 0.0)
            videoSource.trackFaceEnabled = container.decode("trackFaceEnabled", false)
            videoSource.trackFaceZoom = container.decode("trackFaceZoom", 0.75)
            videoSource.mirror = container.decode("mirror", false)
            videoSource.borderWidth = container.decode("borderWidth", 0.0)
            videoSource.borderColor = container.decode("borderColor", RgbColor(red = 0, green = 0, blue = 0))
            videoSource.borderColorColor = videoSource.borderColor.color()
            return videoSource
        }
    }

    object Serializer : KSerializer<SettingsWidgetVideoSource> by JsonObjectSerializer(
        "SettingsWidgetVideoSource",
        { it.encode() },
        { decode(it) },
    )

    fun toEffectSettings(): VideoSourceEffectSettings = VideoSourceEffectSettings(
        rotation = rotation,
        trackFaceEnabled = trackFaceEnabled,
        trackFaceZoom = 1.5 + (1 - trackFaceZoom) * 4,
        mirror = mirror
    )

    fun toCameraId(): SettingsCameraId = videoSource.toCameraId()

    fun updateCameraId(settingsCameraId: SettingsCameraId) {
        videoSource = videoSource.updatingCameraId(settingsCameraId)
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

@Serializable(with = SettingsWidgetScoreboardLayout.Serializer::class)
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

    object Serializer : KSerializer<SettingsWidgetScoreboardLayout> by synthesizedEnumSerializer(
        "SettingsWidgetScoreboardLayout",
        SettingsWidgetScoreboardLayout.entries,
    )
}

@Serializable(with = SettingsWidgetScoreboardPlayer.Serializer::class)
class SettingsWidgetScoreboardPlayer(
    var id: UUID = UUID.randomUUID(),
    name: String = baseName
) : Named {
    override var name: String by Published(name)

    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("name", name)
    }

    companion object {
        val baseName: String = localized("🇸🇪 Moblin")

        fun decode(container: JsonObject): SettingsWidgetScoreboardPlayer {
            val player = SettingsWidgetScoreboardPlayer()
            player.id = container.decode("id", UUID.randomUUID())
            player.name = container.decode("name", baseName)
            return player
        }
    }

    object Serializer : KSerializer<SettingsWidgetScoreboardPlayer> by JsonObjectSerializer(
        "SettingsWidgetScoreboardPlayer",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsWidgetScoreboardScore.Serializer::class)
class SettingsWidgetScoreboardScore(
    var home: Int = 0,
    var away: Int = 0
) {
    fun encode(): JsonObject = encodeContainer {
        encode("home", home)
        encode("away", away)
    }

    companion object {
        fun decode(container: JsonObject): SettingsWidgetScoreboardScore {
            val score = SettingsWidgetScoreboardScore()
            score.home = container.decodeSynthesized("home")
            score.away = container.decodeSynthesized("away")
            return score
        }
    }

    object Serializer : KSerializer<SettingsWidgetScoreboardScore> by JsonObjectSerializer(
        "SettingsWidgetScoreboardScore",
        { it.encode() },
        { decode(it) },
    )
}

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

@Serializable(with = SettingsWidgetPadelScoreboard.Serializer::class)
class SettingsWidgetPadelScoreboard(
    type: SettingsWidgetPadelScoreboardGameType = SettingsWidgetPadelScoreboardGameType.doubles,
    homePlayer1: UUID = UUID.randomUUID(),
    homePlayer2: UUID = UUID.randomUUID(),
    awayPlayer1: UUID = UUID.randomUUID(),
    awayPlayer2: UUID = UUID.randomUUID(),
    var score: List<SettingsWidgetScoreboardScore> = listOf(SettingsWidgetScoreboardScore())
) {
    var type: SettingsWidgetPadelScoreboardGameType by Published(type)
    var homePlayer1: UUID by Published(homePlayer1)
    var homePlayer2: UUID by Published(homePlayer2)
    var awayPlayer1: UUID by Published(awayPlayer1)
    var awayPlayer2: UUID by Published(awayPlayer2)
    var scoreChanges: List<SettingsWidgetScoreboardScoreIncrement> = emptyList()

    fun encode(): JsonObject = encodeContainer {
        encode("type", type)
        encode("homePlayer1", homePlayer1)
        encode("homePlayer2", homePlayer2)
        encode("awayPlayer1", awayPlayer1)
        encode("awayPlayer2", awayPlayer2)
        encode("score", score, ListSerializer(SettingsWidgetScoreboardScore.serializer()))
    }

    companion object {
        fun decode(container: JsonObject): SettingsWidgetPadelScoreboard {
            val padel = SettingsWidgetPadelScoreboard()
            padel.type = container.decode("type", SettingsWidgetPadelScoreboardGameType.doubles)
            padel.homePlayer1 = container.decode("homePlayer1", UUID.randomUUID())
            padel.homePlayer2 = container.decode("homePlayer2", UUID.randomUUID())
            padel.awayPlayer1 = container.decode("awayPlayer1", UUID.randomUUID())
            padel.awayPlayer2 = container.decode("awayPlayer2", UUID.randomUUID())
            padel.score = container.decode(
                "score",
                ListSerializer(SettingsWidgetScoreboardScore.serializer()),
                listOf(SettingsWidgetScoreboardScore())
            )
            return padel
        }
    }

    object Serializer : KSerializer<SettingsWidgetPadelScoreboard> by JsonObjectSerializer(
        "SettingsWidgetPadelScoreboard",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsWidgetGolfScoreboardPlayer.Serializer::class)
class SettingsWidgetGolfScoreboardPlayer(
    name: String = "Player",
    var id: UUID = UUID.randomUUID(),
    var scores: List<Int> = defaultScores,
    var color: RgbColor = RgbColor.white
) : Named {
    override var name: String by Published(name)

    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("name", name)
        encode("scores", scores)
        encode("color", color)
    }

    companion object {
        val defaultScores: List<Int> = List(18) { -1 }

        fun decode(container: JsonObject): SettingsWidgetGolfScoreboardPlayer {
            val player = SettingsWidgetGolfScoreboardPlayer()
            player.id = container.decode("id", UUID.randomUUID())
            player.name = container.decode("name", "Player")
            player.scores = container.decode("scores", defaultScores)
            player.color = container.decode("color", RgbColor.white)
            return player
        }
    }

    object Serializer : KSerializer<SettingsWidgetGolfScoreboardPlayer> by JsonObjectSerializer(
        "SettingsWidgetGolfScoreboardPlayer",
        { it.encode() },
        { decode(it) },
    )

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

@Serializable(with = SettingsWidgetGolfScoreboard.Serializer::class)
class SettingsWidgetGolfScoreboard(
    title: String = defaultTitle,
    numberOfHoles: Int = 18,
    currentHole: Int = 0,
    pars: List<Int> = defaultPars,
    players: List<SettingsWidgetGolfScoreboardPlayer> = defaultPlayers,
    playerColors: Boolean = false,
    showPars: Boolean = true
) {
    var title: String by Published(title)
    var numberOfHoles: Int by Published(numberOfHoles)
    var currentHole: Int by Published(currentHole)
    var pars: List<Int> by Published(pars)
    var players: List<SettingsWidgetGolfScoreboardPlayer> by Published(players)
    var playerColors: Boolean by Published(playerColors)
    var showPars: Boolean by Published(showPars)

    fun encode(): JsonObject = encodeContainer {
        encode("eventName", title)
        encode("numberOfHoles", numberOfHoles)
        encode("currentHole", currentHole)
        encode("pars", pars)
        encode("players", players, ListSerializer(SettingsWidgetGolfScoreboardPlayer.serializer()))
        encode("playerColors", playerColors)
        encode("showPars", showPars)
    }

    companion object {
        const val defaultTitle = "⛳ Masters 2026"
        val defaultPars: List<Int> = listOf(4, 4, 3, 4, 5, 4, 3, 4, 4, 4, 4, 3, 5, 4, 4, 3, 4, 5)
        val defaultPlayers: List<SettingsWidgetGolfScoreboardPlayer> = listOf(
            SettingsWidgetGolfScoreboardPlayer(name = "Player 1"),
            SettingsWidgetGolfScoreboardPlayer(name = "Player 2")
        )

        fun decode(container: JsonObject): SettingsWidgetGolfScoreboard {
            val golf = SettingsWidgetGolfScoreboard()
            golf.title = container.decode("eventName", defaultTitle)
            golf.numberOfHoles = container.decode("numberOfHoles", 18)
            golf.currentHole = container.decode("currentHole", 0)
            golf.updatePars(container.decode("pars", defaultPars))
            golf.players = container.decode(
                "players",
                ListSerializer(SettingsWidgetGolfScoreboardPlayer.serializer()),
                defaultPlayers
            )
            golf.playerColors = container.decode("playerColors", false)
            golf.showPars = container.decode("showPars", true)
            return golf
        }
    }

    object Serializer : KSerializer<SettingsWidgetGolfScoreboard> by JsonObjectSerializer(
        "SettingsWidgetGolfScoreboard",
        { it.encode() },
        { decode(it) },
    )

    fun updatePars(pars: List<Int>) {
        val newPars = pars.toMutableList()
        while (newPars.size < 18) {
            newPars.add(defaultPars[newPars.size])
        }
        this.pars = newPars
    }
}

@Serializable(with = SettingsWidgetGenericScoreboardClockDirection.Serializer::class)
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

    object Serializer : KSerializer<SettingsWidgetGenericScoreboardClockDirection> by synthesizedEnumSerializer(
        "SettingsWidgetGenericScoreboardClockDirection",
        SettingsWidgetGenericScoreboardClockDirection.entries,
    )
}

@Serializable(with = SettingsWidgetGenericScoreboard.Serializer::class)
class SettingsWidgetGenericScoreboard(
    home: String = baseName,
    away: String = baseName,
    title: String = baseTitle,
    period: String = "1",
    var clock: SettingsWidgetScoreboardClock = SettingsWidgetScoreboardClock()
) {
    var home: String by Published(home)
    var away: String by Published(away)
    var title: String by Published(title)
    var period: String by Published(period)
    var score: SettingsWidgetScoreboardScore = SettingsWidgetScoreboardScore()
    var scoreChanges: List<SettingsWidgetScoreboardScoreIncrement> = emptyList()

    fun encode(): JsonObject = encodeContainer {
        encode("home", home)
        encode("away", away)
        encode("title", title)
        encode("period", period)
        encode("clock", clock, SettingsWidgetScoreboardClock.serializer())
    }

    companion object {
        val baseName: String = localized("🇸🇪 Moblin")
        const val baseTitle = "⚽️"

        fun decode(container: JsonObject): SettingsWidgetGenericScoreboard {
            val generic = SettingsWidgetGenericScoreboard()
            generic.home = container.decode("home", baseName)
            generic.away = container.decode("away", baseName)
            generic.title = container.decode("title", baseTitle)
            generic.period = container.decode("period", "1")
            generic.clock = container.decode(
                "clock",
                SettingsWidgetScoreboardClock.serializer(),
                SettingsWidgetScoreboardClock()
            )
            return generic
        }
    }

    object Serializer : KSerializer<SettingsWidgetGenericScoreboard> by JsonObjectSerializer(
        "SettingsWidgetGenericScoreboard",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsWidgetModularScoreboardTeam.Serializer::class)
class SettingsWidgetModularScoreboardTeam(
    name: String = "",
    var textColor: RgbColor = RgbColor.black,
    var backgroundColor: RgbColor = RgbColor.black
) {
    var name: String by Published(name)
    var textColorColor: Color by Published(Color.Transparent)
    var backgroundColorColor: Color by Published(Color.Transparent)

    init {
        loadColors()
    }

    fun encode(): JsonObject = encodeContainer {
        encode("name", name)
        encode("textColor", textColor)
        encode("backgroundColor", backgroundColor)
    }

    companion object {
        fun decode(container: JsonObject): SettingsWidgetModularScoreboardTeam {
            val team = SettingsWidgetModularScoreboardTeam()
            team.name = container.decode("name", "")
            team.textColor = container.decode("textColor", RgbColor.black)
            team.backgroundColor = container.decode("backgroundColor", RgbColor.black)
            team.loadColors()
            return team
        }
    }

    object Serializer : KSerializer<SettingsWidgetModularScoreboardTeam> by JsonObjectSerializer(
        "SettingsWidgetModularScoreboardTeam",
        { it.encode() },
        { decode(it) },
    )

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

@Serializable(with = SettingsWidgetScoreboardClock.Serializer::class)
class SettingsWidgetScoreboardClock(
    maximum: Int = 45,
    direction: SettingsWidgetGenericScoreboardClockDirection =
        SettingsWidgetGenericScoreboardClockDirection.up
) {
    var maximum: Int by Published(maximum)
    var direction: SettingsWidgetGenericScoreboardClockDirection by Published(direction)
    var minutes: Int = 0
    var seconds: Int = 0
    var isStopped: Boolean by Published(true)

    init {
        reset()
    }

    fun encode(): JsonObject = encodeContainer {
        encode("maximum", maximum)
        encode("direction", direction, SettingsWidgetGenericScoreboardClockDirection.serializer())
    }

    companion object {
        fun decode(container: JsonObject): SettingsWidgetScoreboardClock {
            val clock = SettingsWidgetScoreboardClock()
            clock.maximum = container.decode("maximum", 45)
            clock.direction = container.decode(
                "direction",
                SettingsWidgetGenericScoreboardClockDirection.serializer(),
                SettingsWidgetGenericScoreboardClockDirection.up
            )
            clock.reset()
            return clock
        }
    }

    object Serializer : KSerializer<SettingsWidgetScoreboardClock> by JsonObjectSerializer(
        "SettingsWidgetScoreboardClock",
        { it.encode() },
        { decode(it) },
    )

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

@Serializable(with = SettingsWidgetModularScoreboard.Serializer::class)
class SettingsWidgetModularScoreboard(
    home: SettingsWidgetModularScoreboardTeam = createHomeTeam(),
    away: SettingsWidgetModularScoreboardTeam = createAwayTeam(),
    title: String = baseTitle,
    period: String = "1",
    infoBoxText: String = "",
    var clock: SettingsWidgetScoreboardClock = SettingsWidgetScoreboardClock(),
    layout: SettingsWidgetScoreboardLayout = SettingsWidgetScoreboardLayout.stacked,
    width: Float = 350f,
    rowHeight: Float = 45f,
    isBold: Boolean = true,
    showTitle: Boolean = false,
    showMoreStats: Boolean = false,
    showGlobalStatsBlock: Boolean = false,
    showClock: Boolean = true
) {
    var home: SettingsWidgetModularScoreboardTeam by Published(home)
    var away: SettingsWidgetModularScoreboardTeam by Published(away)
    var title: String by Published(title)
    var period: String by Published(period)
    var infoBoxText: String by Published(infoBoxText)
    var score: SettingsWidgetScoreboardScore = SettingsWidgetScoreboardScore()
    var scoreChanges: List<SettingsWidgetScoreboardScoreIncrement> = emptyList()
    var layout: SettingsWidgetScoreboardLayout by Published(layout)
    var config: RemoteControlScoreboardMatchConfig? by Published(null)
    var width: Float by Published(width)
    var rowHeight: Float by Published(rowHeight)
    var isBold: Boolean by Published(isBold)
    var showTitle: Boolean by Published(showTitle)
    var showMoreStats: Boolean by Published(showMoreStats)
    var showGlobalStatsBlock: Boolean by Published(showGlobalStatsBlock)
    var showClock: Boolean by Published(showClock)

    fun encode(): JsonObject = encodeContainer {
        encode("home", home, SettingsWidgetModularScoreboardTeam.serializer())
        encode("away", away, SettingsWidgetModularScoreboardTeam.serializer())
        encode("title", title)
        encode("period", period)
        encode("infoBoxText", infoBoxText)
        encode("clock", clock, SettingsWidgetScoreboardClock.serializer())
        encode("layout", layout, SettingsWidgetScoreboardLayout.serializer())
        encode("width", width)
        encode("rowHeight", rowHeight)
        encode("isBold", isBold)
        encode("showTitle", showTitle)
        encode("showMoreStats", showMoreStats)
        encode("showGlobalStatsBlock", showGlobalStatsBlock)
        encode("showClock", showClock)
    }

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

        fun decode(container: JsonObject): SettingsWidgetModularScoreboard {
            val modular = SettingsWidgetModularScoreboard()
            modular.home = container.decode("home", SettingsWidgetModularScoreboardTeam.serializer(), createHomeTeam())
            modular.away = container.decode("away", SettingsWidgetModularScoreboardTeam.serializer(), createAwayTeam())
            modular.title = container.decode("title", baseTitle)
            modular.period = container.decode("period", "1")
            modular.infoBoxText = container.decode("infoBoxText", "")
            modular.clock = container.decode(
                "clock",
                SettingsWidgetScoreboardClock.serializer(),
                SettingsWidgetScoreboardClock()
            )
            modular.layout = container.decode(
                "layout",
                SettingsWidgetScoreboardLayout.serializer(),
                SettingsWidgetScoreboardLayout.stacked
            )
            modular.width = container.decode("width", 350f)
            modular.rowHeight = container.decode("rowHeight", 45f)
            modular.isBold = container.decode("isBold", true)
            modular.showTitle = container.decode("showTitle", false)
            modular.showMoreStats = container.decode("showMoreStats", false)
            modular.showGlobalStatsBlock = container.decode("showGlobalStatsBlock", false)
            modular.showClock = container.decode("showClock", true)
            return modular
        }
    }

    object Serializer : KSerializer<SettingsWidgetModularScoreboard> by JsonObjectSerializer(
        "SettingsWidgetModularScoreboard",
        { it.encode() },
        { decode(it) },
    )

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

@Serializable(with = SettingsWidgetScoreboard.Serializer::class)
class SettingsWidgetScoreboard(
    sport: SettingsWidgetScoreboardSport = SettingsWidgetScoreboardSport.generic,
    var textColor: RgbColor = baseTextColor,
    var primaryBackgroundColor: RgbColor = basePrimaryBackgroundColor,
    var secondaryBackgroundColor: RgbColor = baseSecondaryBackgroundColor,
    var padel: SettingsWidgetPadelScoreboard = SettingsWidgetPadelScoreboard(),
    var golf: SettingsWidgetGolfScoreboard = SettingsWidgetGolfScoreboard(),
    var generic: SettingsWidgetGenericScoreboard = SettingsWidgetGenericScoreboard(),
    var modular: SettingsWidgetModularScoreboard = SettingsWidgetModularScoreboard()
) {
    var sport: SettingsWidgetScoreboardSport by Published(sport)
    var textColorColor: Color by Published(textColor.color())
    var primaryBackgroundColorColor: Color by Published(primaryBackgroundColor.color())
    var secondaryBackgroundColorColor: Color by Published(secondaryBackgroundColor.color())

    init {
        loadColors()
    }

    fun encode(): JsonObject = encodeContainer {
        encode("type", sport)
        encode("textColor", textColor)
        encode("primaryBackgroundColor", primaryBackgroundColor)
        encode("secondaryBackgroundColor", secondaryBackgroundColor)
        encode("padel", padel, SettingsWidgetPadelScoreboard.serializer())
        encode("golf", golf, SettingsWidgetGolfScoreboard.serializer())
        encode("generic", generic, SettingsWidgetGenericScoreboard.serializer())
        encode("modular", modular, SettingsWidgetModularScoreboard.serializer())
    }

    companion object {
        val baseTextColor: RgbColor = RgbColor.white
        val basePrimaryBackgroundColor: RgbColor = RgbColor(red = 0x0B, green = 0x10, blue = 0xAC)
        val baseSecondaryBackgroundColor: RgbColor = RgbColor(red = 0, green = 3, blue = 0x5B)

        fun decode(container: JsonObject): SettingsWidgetScoreboard {
            val scoreboard = SettingsWidgetScoreboard()
            scoreboard.sport = container.decode("type", SettingsWidgetScoreboardSport.generic)
            scoreboard.textColor = container.decode("textColor", baseTextColor)
            scoreboard.primaryBackgroundColor = container.decode("primaryBackgroundColor", basePrimaryBackgroundColor)
            scoreboard.secondaryBackgroundColor = container.decode(
                "secondaryBackgroundColor",
                baseSecondaryBackgroundColor
            )
            scoreboard.padel = container.decode(
                "padel",
                SettingsWidgetPadelScoreboard.serializer(),
                SettingsWidgetPadelScoreboard()
            )
            scoreboard.golf = container.decode(
                "golf",
                SettingsWidgetGolfScoreboard.serializer(),
                SettingsWidgetGolfScoreboard()
            )
            scoreboard.generic = container.decode(
                "generic",
                SettingsWidgetGenericScoreboard.serializer(),
                SettingsWidgetGenericScoreboard()
            )
            scoreboard.modular = container.decode(
                "modular",
                SettingsWidgetModularScoreboard.serializer(),
                SettingsWidgetModularScoreboard()
            )
            scoreboard.loadColors()
            return scoreboard
        }
    }

    object Serializer : KSerializer<SettingsWidgetScoreboard> by JsonObjectSerializer(
        "SettingsWidgetScoreboard",
        { it.encode() },
        { decode(it) },
    )

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

@Serializable(with = SettingsScene.Serializer::class)
class SettingsScene(
    name: String = baseName,
    var id: UUID = UUID.randomUUID(),
    enabled: Boolean = true,
    videoSource: SettingsVideoSource = SettingsVideoSource(),
    widgets: List<SettingsSceneWidget> = emptyList(),
    videoSourceRotation: Double = 0.0,
    videoStabilizationMode: SettingsVideoStabilizationMode = SettingsVideoStabilizationMode.off,
    overrideVideoStabilizationMode: Boolean = false,
    fillFrame: Boolean = false,
    overrideMic: Boolean = false,
    micId: String = "",
    quickSwitchGroup: Int? = null,
    mirror: Boolean = false,
    var backgroundColor: RgbColor = defaultSegmentedPickerSelectedColor
) : Named {
    override var name: String by Published(name)
    var enabled: Boolean by Published(enabled)
    var videoSource: SettingsVideoSource by Published(videoSource)
    var widgets: MutableList<SettingsSceneWidget> by PublishedList(widgets)
    var videoSourceRotation: Double by Published(videoSourceRotation)
    var videoStabilizationMode: SettingsVideoStabilizationMode by Published(videoStabilizationMode)
    var overrideVideoStabilizationMode: Boolean by Published(overrideVideoStabilizationMode)
    var fillFrame: Boolean by Published(fillFrame)
    var overrideMic: Boolean by Published(overrideMic)
    var micId: String by Published(micId)
    var quickSwitchGroup: Int? by Published(quickSwitchGroup)
    var mirror: Boolean by Published(mirror)
    var backgroundColorColor: Color by Published(defaultSegmentedPickerSelectedColor.color())

    override fun equals(other: Any?): Boolean = other is SettingsScene && id == other.id

    override fun hashCode(): Int = id.hashCode()

    fun encode(): JsonObject = encodeContainer {
        encode("name", name)
        encode("id", id)
        encode("enabled", enabled)
        encode("cameraPosition", videoSource.cameraPosition, SettingsSceneCameraPosition.serializer())
        encode("backCameraId", videoSource.backCameraId)
        encode("frontCameraId", videoSource.frontCameraId)
        encode("rtmpCameraId", videoSource.rtmpCameraId)
        encode("srtlaCameraId", videoSource.srtlaCameraId)
        encode("srtClientCameraId", videoSource.srtClientCameraId)
        encode("ristCameraId", videoSource.ristCameraId)
        encode("rtspCameraId", videoSource.rtspCameraId)
        encode("whipCameraId", videoSource.whipCameraId)
        encode("whepCameraId", videoSource.whepCameraId)
        encode("mediaPlayerCameraId", videoSource.mediaPlayerCameraId)
        encode("externalCameraId", videoSource.externalCameraId)
        encode("externalCameraName", videoSource.externalCameraName)
        encode("widgets", widgets, ListSerializer(SettingsSceneWidget.serializer()))
        encode("videoSourceRotation", videoSourceRotation)
        encode("videoStabilizationMode", videoStabilizationMode)
        encode("overrideVideoStabilizationMode", overrideVideoStabilizationMode)
        encode("fillFrame", fillFrame)
        encode("overrideMic", overrideMic)
        encode("micId", micId)
        encode("quickSwitchGroup", quickSwitchGroup)
        encode("mirror", mirror)
        encode("backgroundColor", backgroundColor)
    }

    companion object {
        val baseName: String = localized("My scene")

        fun decode(container: JsonObject): SettingsScene {
            val scene = SettingsScene()
            scene.name = container.decode("name", baseName)
            scene.id = container.decode("id", UUID.randomUUID())
            scene.enabled = container.decode("enabled", true)
            scene.videoSource = SettingsVideoSource(
                cameraPosition = decodeCameraPosition(container, "cameraPosition", defaultBackCameraPosition),
                backCameraId = decodeCameraId(container, "backCameraId", bestBackCameraId),
                frontCameraId = decodeCameraId(container, "frontCameraId", bestFrontCameraId),
                rtmpCameraId = container.decode("rtmpCameraId", UUID.randomUUID()),
                srtlaCameraId = container.decode("srtlaCameraId", UUID.randomUUID()),
                srtClientCameraId = container.decode("srtClientCameraId", UUID.randomUUID()),
                ristCameraId = container.decode("ristCameraId", UUID.randomUUID()),
                rtspCameraId = container.decode("rtspCameraId", UUID.randomUUID()),
                whipCameraId = container.decode("whipCameraId", UUID.randomUUID()),
                whepCameraId = container.decode("whepCameraId", UUID.randomUUID()),
                mediaPlayerCameraId = container.decode("mediaPlayerCameraId", UUID.randomUUID()),
                externalCameraId = container.decode("externalCameraId", ""),
                externalCameraName = container.decode("externalCameraName", ""),
            )
            scene.widgets = container.decode("widgets", ListSerializer(SettingsSceneWidget.serializer()), emptyList())
                .toMutableList()
            scene.videoSourceRotation = container.decode("videoSourceRotation", 0.0)
            scene.videoStabilizationMode = container.decode(
                "videoStabilizationMode",
                SettingsVideoStabilizationMode.off
            )
            scene.overrideVideoStabilizationMode = container.decode("overrideVideoStabilizationMode", false)
            scene.fillFrame = container.decode("fillFrame", false)
            scene.overrideMic = container.decode("overrideMic", false)
            scene.micId = container.decode("micId", "")
            scene.quickSwitchGroup = container.decode<Int?>("quickSwitchGroup", null)
            scene.mirror = container.decode("mirror", false)
            scene.backgroundColor = container.decode(
                "backgroundColor",
                defaultSegmentedPickerSelectedColor
            )
            scene.backgroundColorColor = scene.backgroundColor.color()
            return scene
        }
    }

    object Serializer : KSerializer<SettingsScene> by JsonObjectSerializer(
        "SettingsScene",
        { it.encode() },
        { decode(it) },
    )

    fun clone(): SettingsScene {
        val new = SettingsScene(name = name)
        new.enabled = enabled
        new.videoSource = videoSource.copy()
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
        videoSource = videoSource.updatingCameraId(settingsCameraId)
    }
}

@Serializable(with = SettingsAutoSceneSwitcherScene.Serializer::class)
class SettingsAutoSceneSwitcherScene(
    var id: UUID = UUID.randomUUID(),
    sceneId: UUID? = null,
    time: Int = 15
) {
    var sceneId: UUID? by Published(sceneId)
    var time: Int by Published(time)

    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("sceneId", sceneId)
        encode("time", time)
    }

    companion object {
        fun decode(container: JsonObject): SettingsAutoSceneSwitcherScene {
            val scene = SettingsAutoSceneSwitcherScene()
            scene.id = container.decode("id", UUID.randomUUID())
            scene.sceneId = container.decode<UUID?>("sceneId", null)
            scene.time = container.decode("time", 15)
            return scene
        }
    }

    object Serializer : KSerializer<SettingsAutoSceneSwitcherScene> by JsonObjectSerializer(
        "SettingsAutoSceneSwitcherScene",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsAutoSceneSwitcher.Serializer::class)
class SettingsAutoSceneSwitcher(
    var id: UUID = UUID.randomUUID(),
    name: String = baseName,
    shuffle: Boolean = false,
    scenes: List<SettingsAutoSceneSwitcherScene> = emptyList()
) : Named {
    override var name: String by Published(name)
    var shuffle: Boolean by Published(shuffle)
    var scenes: List<SettingsAutoSceneSwitcherScene> by Published(scenes)

    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("name", name)
        encode("shuffle", shuffle)
        encode("scenes", scenes, ListSerializer(SettingsAutoSceneSwitcherScene.serializer()))
    }

    companion object {
        val baseName: String = localized("My switcher")

        fun decode(container: JsonObject): SettingsAutoSceneSwitcher {
            val switcher = SettingsAutoSceneSwitcher()
            switcher.id = container.decode("id", UUID.randomUUID())
            switcher.name = container.decode("name", baseName)
            switcher.shuffle = container.decode("shuffle", false)
            switcher.scenes = container.decode(
                "scenes",
                ListSerializer(SettingsAutoSceneSwitcherScene.serializer()),
                emptyList()
            )
            return switcher
        }
    }

    object Serializer : KSerializer<SettingsAutoSceneSwitcher> by JsonObjectSerializer(
        "SettingsAutoSceneSwitcher",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsAutoSceneSwitchers.Serializer::class)
class SettingsAutoSceneSwitchers(
    switcherId: UUID? = null,
    switchers: List<SettingsAutoSceneSwitcher> = emptyList()
) {
    var switcherId: UUID? by Published(switcherId)
    var switchers: List<SettingsAutoSceneSwitcher> by Published(switchers)

    fun encode(): JsonObject = encodeContainer {
        encode("switcherId", switcherId)
        encode("switchers", switchers, ListSerializer(SettingsAutoSceneSwitcher.serializer()))
    }

    companion object {
        fun decode(container: JsonObject): SettingsAutoSceneSwitchers {
            val switchers = SettingsAutoSceneSwitchers()
            switchers.switcherId = container.decodeIfPresent<UUID>("switcherId")
            switchers.switchers = container.decode(
                "switchers",
                ListSerializer(SettingsAutoSceneSwitcher.serializer()),
                emptyList()
            )
            return switchers
        }
    }

    object Serializer : KSerializer<SettingsAutoSceneSwitchers> by JsonObjectSerializer(
        "SettingsAutoSceneSwitchers",
        { it.encode() },
        { decode(it) },
    )
}

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

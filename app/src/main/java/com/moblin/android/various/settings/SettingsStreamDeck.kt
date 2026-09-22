package com.moblin.android.various.settings

import androidx.compose.ui.graphics.Color
import com.moblin.android.common.various.RgbColor
import com.moblin.android.localized
import com.moblin.android.various.utils.Named
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

@Serializable(with = SettingsStreamDeckKeySerializer::class)
class SettingsStreamDeckKey {
    val id: UUID
    var color: RgbColor
    private val text: MutableStateFlow<String>
    val text: StateFlow<String>
    private val _colorColor: MutableStateFlow<Color>
    val colorColor: StateFlow<Color>
    private val function: MutableStateFlow<SettingsControllerFunction>
    val function: StateFlow<SettingsControllerFunction>
    private val functionData: MutableStateFlow<SettingsControllerFunctionData>
    val functionData: StateFlow<SettingsControllerFunctionData>

    companion object {
        val defaultColor: RgbColor = RgbColor.black
    }

    constructor() {
        id = UUID.randomUUID()
        color = defaultColor
        text = MutableStateFlow("")
        text = text.asStateFlow()
        _colorColor = MutableStateFlow(defaultColor.color())
        colorColor = _colorColor.asStateFlow()
        function = MutableStateFlow(SettingsControllerFunction.unused)
        function = function.asStateFlow()
        functionData = MutableStateFlow(SettingsControllerFunctionData())
        functionData = functionData.asStateFlow()
    }

    internal constructor(
        id: UUID,
        text: String,
        color: RgbColor,
        function: SettingsControllerFunction,
        functionData: SettingsControllerFunctionData,
    ) {
        this.id = id
        this.color = color
        this.text = MutableStateFlow(text)
        this.text = this.text.asStateFlow()
        this._colorColor = MutableStateFlow(color.color())
        this.colorColor = this._colorColor.asStateFlow()
        this.function = MutableStateFlow(function)
        this.function = this.function.asStateFlow()
        this.functionData = MutableStateFlow(functionData)
        this.functionData = this.functionData.asStateFlow()
    }
}

object SettingsStreamDeckKeySerializer : KSerializer<SettingsStreamDeckKey> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("SettingsStreamDeckKey") {
        element<String>("id")
        element<String>("text")
        element<RgbColor>("color")
        element<SettingsControllerFunction>("function")
        element<String?>("sceneId")
        element<String?>("widgetId")
        element<String?>("gimbalPresetId")
        element<SettingsGimbalMotion>("gimbalMotion")
        element<String?>("macroId")
        element<String?>("streamDeckLayoutId")
    }

    override fun serialize(encoder: Encoder, value: SettingsStreamDeckKey) {
        val data = value.functionData.value
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.id.toString())
            encodeStringElement(descriptor, 1, value.text.value)
            encodeSerializableElement(descriptor, 2, RgbColor.serializer(), value.color)
            encodeSerializableElement(descriptor, 3, SettingsControllerFunction.serializer(), value.function.value)
            encodeNullableSerializableElement(descriptor, 4, String.serializer(), data.sceneId?.toString())
            encodeNullableSerializableElement(descriptor, 5, String.serializer(), data.widgetId?.toString())
            encodeNullableSerializableElement(descriptor, 6, String.serializer(), data.gimbalPresetId?.toString())
            encodeSerializableElement(descriptor, 7, SettingsGimbalMotion.serializer(), data.gimbalMotion)
            encodeNullableSerializableElement(descriptor, 8, String.serializer(), data.macroId?.toString())
            encodeNullableSerializableElement(descriptor, 9, String.serializer(), data.streamDeckLayoutId?.toString())
        }
    }

    override fun deserialize(decoder: Decoder): SettingsStreamDeckKey {
        var id: UUID = UUID.randomUUID()
        var text: String = ""
        var color: RgbColor = RgbColor.white
        var function: SettingsControllerFunction = SettingsControllerFunction.unused
        var sceneId: UUID? = null
        var widgetId: UUID? = null
        var gimbalPresetId: UUID? = null
        var gimbalMotion: SettingsGimbalMotion = SettingsGimbalMotion.kapow
        var macroId: UUID? = null
        var streamDeckLayoutId: UUID? = null
        decoder.decodeStructure(descriptor) {
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break
                    0 -> id = UUID.fromString(decodeStringElement(descriptor, 0))
                    1 -> text = decodeStringElement(descriptor, 1)
                    2 -> color = decodeSerializableElement(descriptor, 2, RgbColor.serializer())
                    3 -> function = decodeSerializableElement(descriptor, 3, SettingsControllerFunction.serializer())
                    4 -> sceneId = decodeNullableSerializableElement(descriptor, 4, String.serializer())?.let { UUID.fromString(it) }
                    5 -> widgetId = decodeNullableSerializableElement(descriptor, 5, String.serializer())?.let { UUID.fromString(it) }
                    6 -> gimbalPresetId = decodeNullableSerializableElement(descriptor, 6, String.serializer())?.let { UUID.fromString(it) }
                    7 -> gimbalMotion = decodeSerializableElement(descriptor, 7, SettingsGimbalMotion.serializer())
                    8 -> macroId = decodeNullableSerializableElement(descriptor, 8, String.serializer())?.let { UUID.fromString(it) }
                    9 -> streamDeckLayoutId = decodeNullableSerializableElement(descriptor, 9, String.serializer())?.let { UUID.fromString(it) }
                    else -> {}
                }
            }
        }
        val functionData = SettingsControllerFunctionData().apply {
            this.sceneId = sceneId
            this.widgetId = widgetId
            this.gimbalPresetId = gimbalPresetId
            this.gimbalMotion = gimbalMotion
            this.macroId = macroId
            this.streamDeckLayoutId = streamDeckLayoutId
        }
        return SettingsStreamDeckKey(id, text, color, function, functionData)
    }
}

@Serializable
enum class SettingsStreamDeckModel(val rawValue: String) {
    mini("mini"),
    classic("classic"),
    xl("xl");

    override fun toString(): String {
        return when (this) {
            mini -> localized("Mini")
            classic -> localized("Classic")
            xl -> localized("XL")
        }
    }

    companion object {
        fun fromRawValue(rawValue: String): SettingsStreamDeckModel? =
            entries.firstOrNull { it.rawValue == rawValue }
    }
}

@Serializable(with = SettingsStreamDeckLayoutSerializer::class)
class SettingsStreamDeckLayout : Named {
    val id: UUID
    private val name: MutableStateFlow<String>
    override val name: StateFlow<String>
    private val _model: MutableStateFlow<SettingsStreamDeckModel>
    val model: StateFlow<SettingsStreamDeckModel>
    private val _keys: MutableStateFlow<List<SettingsStreamDeckKey>>
    val keys: StateFlow<List<SettingsStreamDeckKey>>

    companion object {
        const val baseName: String = "My layout"
    }

    constructor() {
        id = UUID.randomUUID()
        name = MutableStateFlow(baseName)
        name = name.asStateFlow()
        _model = MutableStateFlow(SettingsStreamDeckModel.classic)
        model = _model.asStateFlow()
        val initialKeys = MutableList(36) { SettingsStreamDeckKey() }
        _keys = MutableStateFlow(initialKeys)
        keys = _keys.asStateFlow()
    }

    internal constructor(
        id: UUID,
        name: String,
        model: SettingsStreamDeckModel,
        keys: List<SettingsStreamDeckKey>,
    ) {
        this.id = id
        this.name = MutableStateFlow(name)
        this.name = this.name.asStateFlow()
        this._model = MutableStateFlow(model)
        this.model = this._model.asStateFlow()
        val initialKeys = keys.toMutableList()
        for (i in keys.size until 36) {
            initialKeys.add(SettingsStreamDeckKey())
        }
        this._keys = MutableStateFlow(initialKeys)
        this.keys = this._keys.asStateFlow()
    }
}

object SettingsStreamDeckLayoutSerializer : KSerializer<SettingsStreamDeckLayout> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("SettingsStreamDeckLayout") {
        element<String>("id")
        element<String>("name")
        element<SettingsStreamDeckModel>("model")
        element<List<SettingsStreamDeckKey>>("keys")
    }

    override fun serialize(encoder: Encoder, value: SettingsStreamDeckLayout) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.id.toString())
            encodeStringElement(descriptor, 1, value.name.value)
            encodeSerializableElement(descriptor, 2, SettingsStreamDeckModel.serializer(), value.model.value)
            encodeSerializableElement(
                descriptor,
                3,
                ListSerializer(SettingsStreamDeckKeySerializer),
                value.keys.value,
            )
        }
    }

    override fun deserialize(decoder: Decoder): SettingsStreamDeckLayout {
        var id: UUID = UUID.randomUUID()
        var name: String = SettingsStreamDeckLayout.baseName
        var model: SettingsStreamDeckModel = SettingsStreamDeckModel.classic
        var keys: List<SettingsStreamDeckKey> = emptyList()
        decoder.decodeStructure(descriptor) {
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break
                    0 -> id = UUID.fromString(decodeStringElement(descriptor, 0))
                    1 -> name = decodeStringElement(descriptor, 1)
                    2 -> model = decodeSerializableElement(descriptor, 2, SettingsStreamDeckModel.serializer())
                    3 -> keys = decodeSerializableElement(
                        descriptor,
                        3,
                        ListSerializer(SettingsStreamDeckKeySerializer),
                    )
                    else -> {}
                }
            }
        }
        return SettingsStreamDeckLayout(id, name, model, keys)
    }
}

@Serializable(with = SettingsStreamDecksSerializer::class)
class SettingsStreamDecks {
    private val _layouts: MutableStateFlow<List<SettingsStreamDeckLayout>>
    val layouts: StateFlow<List<SettingsStreamDeckLayout>>
    private val selectedId: MutableStateFlow<UUID?>
    val selectedId: StateFlow<UUID?>

    constructor() {
        _layouts = MutableStateFlow(emptyList())
        layouts = _layouts.asStateFlow()
        selectedId = MutableStateFlow(null)
        selectedId = selectedId.asStateFlow()
    }

    internal constructor(layouts: List<SettingsStreamDeckLayout>, selectedId: UUID?) {
        this._layouts = MutableStateFlow(layouts)
        this.layouts = this._layouts.asStateFlow()
        this.selectedId = MutableStateFlow(selectedId)
        this.selectedId = this.selectedId.asStateFlow()
    }
}

object SettingsStreamDecksSerializer : KSerializer<SettingsStreamDecks> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("SettingsStreamDecks") {
        element<List<SettingsStreamDeckLayout>>("layouts")
        element<String?>("selectedId")
    }

    override fun serialize(encoder: Encoder, value: SettingsStreamDecks) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(
                descriptor,
                0,
                ListSerializer(SettingsStreamDeckLayoutSerializer),
                value.layouts.value,
            )
            encodeNullableSerializableElement(descriptor, 1, String.serializer(), value.selectedId.value?.toString())
        }
    }

    override fun deserialize(decoder: Decoder): SettingsStreamDecks {
        var layouts: List<SettingsStreamDeckLayout> = emptyList()
        var selectedId: UUID? = null
        decoder.decodeStructure(descriptor) {
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break
                    0 -> layouts = decodeSerializableElement(
                        descriptor,
                        0,
                        ListSerializer(SettingsStreamDeckLayoutSerializer),
                    )
                    1 -> selectedId = decodeNullableSerializableElement(descriptor, 1, String.serializer())?.let { UUID.fromString(it) }
                    else -> {}
                }
            }
        }
        return SettingsStreamDecks(layouts, selectedId)
    }
}

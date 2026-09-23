package com.moblin.android.various.settings

import androidx.compose.ui.graphics.Color
import com.moblin.android.common.various.RgbColor
import com.moblin.android.common.various.color
import com.moblin.android.localized
import com.moblin.android.platform.codable.JsonObjectSerializer
import com.moblin.android.platform.codable.decode
import com.moblin.android.platform.codable.encodeContainer
import com.moblin.android.platform.swiftui.Published
import com.moblin.android.various.utils.Named
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonObject

@Serializable(with = SettingsStreamDeckKey.Serializer::class)
class SettingsStreamDeckKey {
    var id: UUID
        private set
    var color: RgbColor
    private val _text: MutableStateFlow<String>
    val text: StateFlow<String>
    private val _colorColor: MutableStateFlow<Color>
    val colorColor: StateFlow<Color>
    private val _function: MutableStateFlow<SettingsControllerFunction>
    val function: StateFlow<SettingsControllerFunction>
    private val _functionData: MutableStateFlow<SettingsControllerFunctionData>
    val functionData: StateFlow<SettingsControllerFunctionData>

    constructor() {
        id = UUID.randomUUID()
        color = defaultColor
        _text = MutableStateFlow("")
        text = _text.asStateFlow()
        _colorColor = MutableStateFlow(defaultColor.color())
        colorColor = _colorColor.asStateFlow()
        _function = MutableStateFlow(SettingsControllerFunction.UNUSED)
        function = _function.asStateFlow()
        _functionData = MutableStateFlow(SettingsControllerFunctionData())
        functionData = _functionData.asStateFlow()
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
        this._text = MutableStateFlow(text)
        this.text = this._text.asStateFlow()
        this._colorColor = MutableStateFlow(color.color())
        this.colorColor = this._colorColor.asStateFlow()
        this._function = MutableStateFlow(function)
        this.function = this._function.asStateFlow()
        this._functionData = MutableStateFlow(functionData)
        this.functionData = this._functionData.asStateFlow()
    }

    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("text", text.value)
        encode("color", color)
        encode("function", function.value)
        encode("sceneId", functionData.value.sceneId)
        encode("widgetId", functionData.value.widgetId)
        encode("gimbalPresetId", functionData.value.gimbalPresetId)
        encode("gimbalMotion", functionData.value.gimbalMotion)
        encode("macroId", functionData.value.macroId)
        encode("streamDeckLayoutId", functionData.value.streamDeckLayoutId)
    }

    companion object {
        val defaultColor: RgbColor = RgbColor.black

        fun decode(container: JsonObject): SettingsStreamDeckKey {
            val key = SettingsStreamDeckKey()
            key.id = container.decode("id", UUID.randomUUID())
            key._text.value = container.decode("text", "")
            key.color = container.decode("color", RgbColor.white)
            key._colorColor.value = key.color.color()
            key._function.value = container.decode("function", SettingsControllerFunction.UNUSED)
            key._functionData.value.sceneId = container.decode<UUID?>("sceneId", null)
            key._functionData.value.widgetId = container.decode<UUID?>("widgetId", null)
            key._functionData.value.gimbalPresetId = container.decode<UUID?>("gimbalPresetId", null)
            key._functionData.value.gimbalMotion = container.decode("gimbalMotion", SettingsGimbalMotion.KAPOW)
            key._functionData.value.macroId = container.decode<UUID?>("macroId", null)
            key._functionData.value.streamDeckLayoutId = container.decode<UUID?>("streamDeckLayoutId", null)
            return key
        }
    }

    object Serializer : KSerializer<SettingsStreamDeckKey> by JsonObjectSerializer(
        "SettingsStreamDeckKey",
        { it.encode() },
        { decode(it) },
    )
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

@Serializable(with = SettingsStreamDeckLayout.Serializer::class)
class SettingsStreamDeckLayout : Named {
    var id: UUID
        private set
    override var name: String by Published(baseName)
    private val _model: MutableStateFlow<SettingsStreamDeckModel>
    val model: StateFlow<SettingsStreamDeckModel>
    private val _keys: MutableStateFlow<List<SettingsStreamDeckKey>>
    val keys: StateFlow<List<SettingsStreamDeckKey>>

    constructor() {
        id = UUID.randomUUID()
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
        this.name = name
        this._model = MutableStateFlow(model)
        this.model = this._model.asStateFlow()
        val initialKeys = keys.toMutableList()
        for (i in keys.size until 36) {
            initialKeys.add(SettingsStreamDeckKey())
        }
        this._keys = MutableStateFlow(initialKeys)
        this.keys = this._keys.asStateFlow()
    }

    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("name", name)
        encode("model", model.value)
        encode("keys", keys.value)
    }

    companion object {
        const val baseName: String = "My layout"

        fun decode(container: JsonObject): SettingsStreamDeckLayout {
            val layout = SettingsStreamDeckLayout()
            layout.id = container.decode("id", UUID.randomUUID())
            layout.name = container.decode("name", baseName)
            layout._model.value = container.decode("model", SettingsStreamDeckModel.classic)
            val keys = container.decode("keys", ListSerializer(SettingsStreamDeckKey.serializer()), emptyList())
                .toMutableList()
            for (i in keys.size until 36) {
                keys.add(SettingsStreamDeckKey())
            }
            layout._keys.value = keys
            return layout
        }
    }

    object Serializer : KSerializer<SettingsStreamDeckLayout> by JsonObjectSerializer(
        "SettingsStreamDeckLayout",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsStreamDecks.Serializer::class)
class SettingsStreamDecks {
    private val _layouts: MutableStateFlow<List<SettingsStreamDeckLayout>>
    val layouts: StateFlow<List<SettingsStreamDeckLayout>>
    private val _selectedId: MutableStateFlow<UUID?>
    val selectedId: StateFlow<UUID?>

    constructor() {
        _layouts = MutableStateFlow(emptyList())
        layouts = _layouts.asStateFlow()
        _selectedId = MutableStateFlow(null)
        selectedId = _selectedId.asStateFlow()
    }

    internal constructor(layouts: List<SettingsStreamDeckLayout>, selectedId: UUID?) {
        this._layouts = MutableStateFlow(layouts)
        this.layouts = this._layouts.asStateFlow()
        this._selectedId = MutableStateFlow(selectedId)
        this.selectedId = this._selectedId.asStateFlow()
    }

    fun setSelectedId(id: UUID?) {
        _selectedId.value = id
    }

    fun encode(): JsonObject = encodeContainer {
        encode("layouts", layouts.value)
        encode("selectedId", selectedId.value)
    }

    companion object {
        fun decode(container: JsonObject): SettingsStreamDecks {
            val streamDecks = SettingsStreamDecks()
            streamDecks._layouts.value = container.decode(
                "layouts",
                ListSerializer(SettingsStreamDeckLayout.serializer()),
                emptyList(),
            )
            streamDecks._selectedId.value = container.decode<UUID?>("selectedId", null)
            return streamDecks
        }
    }

    object Serializer : KSerializer<SettingsStreamDecks> by JsonObjectSerializer(
        "SettingsStreamDecks",
        { it.encode() },
        { decode(it) },
    )
}

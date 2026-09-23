package com.moblin.android.various.settings

import com.moblin.android.localized
import com.moblin.android.various.MainTimer
import com.moblin.android.various.utils.Named
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Transient
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

private object SettingsMacrosUuidSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("java.util.UUID", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): UUID {
        return UUID.fromString(decoder.decodeString())
    }
}

private class RawValueSerializer<T>(
    private val serialName: String,
    private val toRawValue: (T) -> String,
    private val fromRawValue: (String) -> T?,
) : KSerializer<T> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor(serialName, PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeString(toRawValue(value))
    }

    override fun deserialize(decoder: Decoder): T {
        val rawValue = decoder.decodeString()
        return fromRawValue(rawValue)
            ?: throw SerializationException("Unknown $serialName value: $rawValue")
    }
}

private val uuidSetSerializer: KSerializer<Set<UUID>> = SetSerializer(SettingsMacrosUuidSerializer)
private val settingsReactionSerializer: KSerializer<SettingsReaction> = RawValueSerializer(
    "SettingsReaction",
    { it.rawValue },
    { SettingsReaction.fromRawValue(it) },
)
private val settingsMacrosActionFunctionSerializer: KSerializer<SettingsMacrosActionFunction> = RawValueSerializer(
    "SettingsMacrosActionFunction",
    { it.rawValue },
    { SettingsMacrosActionFunction.fromRawValue(it) },
)
private val settingsMacrosEventSerializer: KSerializer<SettingsMacrosEvent> = RawValueSerializer(
    "SettingsMacrosEvent",
    { it.rawValue },
    { SettingsMacrosEvent.fromRawValue(it) },
)
private val settingsMacrosActionIfComparisonSerializer: KSerializer<SettingsMacrosActionIfComparison> = RawValueSerializer(
    "SettingsMacrosActionIfComparison",
    { it.rawValue },
    { SettingsMacrosActionIfComparison.fromRawValue(it) },
)
private val settingsMacrosMacroRepeatModeSerializer: KSerializer<SettingsMacrosMacroRepeatMode> = RawValueSerializer(
    "SettingsMacrosMacroRepeatMode",
    { it.rawValue },
    { SettingsMacrosMacroRepeatMode.fromRawValue(it) },
)
private val quickButtonTypeSerializer: KSerializer<SettingsQuickButtonType> = RawValueSerializer(
    "SettingsQuickButtonType",
    { it.rawValue },
    { SettingsQuickButtonType.fromRawValue(it) },
)
private val quickButtonTypeSetSerializer: KSerializer<Set<SettingsQuickButtonType>> =
    SetSerializer(quickButtonTypeSerializer)

enum class SettingsReaction(val rawValue: String) {
    FIREWORKS("fireworks"),
    BALLOONS("balloons"),
    HEARTS("hearts"),
    CONFETTI("confetti"),
    LASERS("lasers"),
    RAIN("rain"),
    GLASSES("glasses"),
    SPARKLE("sparkle");

    fun toSystem(): Any? = null

    override fun toString(): String {
        return when (this) {
            FIREWORKS -> localized("Fireworks")
            BALLOONS -> localized("Balloons")
            HEARTS -> localized("Hearts")
            CONFETTI -> localized("Confetti")
            LASERS -> localized("Lasers")
            RAIN -> localized("Rain")
            GLASSES -> localized("Glasses")
            SPARKLE -> localized("Sparkle")
        }
    }

    companion object {
        fun fromRawValue(value: String?): SettingsReaction? {
            return entries.firstOrNull { it.rawValue == value }
        }
    }
}

enum class SettingsMacrosActionFunction(val rawValue: String) {
    SCENE("Scene"),
    ZOOM("Zoom"),
    FILTERS("Filters"),
    REACTION("Reaction"),
    ENABLE_DISABLE_SCENES("Enable/disable scenes"),
    RECORD("Record"),
    SNAPSHOT("Snapshot"),
    MUTE("Mute"),
    TORCH("Torch"),
    AUTO_SCENE_SWITCHER("Auto scene switcher"),
    DJI_DEVICES("DJI devices"),
    GIMBAL_PRESET("Move to gimbal preset"),
    SEND_CHAT_MESSAGE("Send chat message"),
    SEND_TWITCH_SHOUTOUT("Send Twitch shoutout"),
    DELAY("Delay"),
    WAIT_FOR_EVENT("Wait for event"),
    IF_CONDITION("If"),
    MACRO("Macro");

    override fun toString(): String {
        return when (this) {
            SCENE -> localized("Scene")
            ZOOM -> localized("Zoom")
            FILTERS -> localized("Filters")
            REACTION -> localized("Reaction")
            ENABLE_DISABLE_SCENES -> localized("Scenes")
            RECORD -> localized("Record")
            SNAPSHOT -> localized("Snapshot")
            MUTE -> localized("Mute")
            TORCH -> localized("Torch")
            AUTO_SCENE_SWITCHER -> localized("Auto scene switcher")
            DJI_DEVICES -> localized("DJI devices")
            GIMBAL_PRESET -> localized("Move to gimbal preset")
            SEND_CHAT_MESSAGE -> localized("Send chat message")
            SEND_TWITCH_SHOUTOUT -> localized("Send Twitch shoutout")
            DELAY -> localized("Delay")
            WAIT_FOR_EVENT -> localized("Wait for event")
            IF_CONDITION -> localized("If")
            MACRO -> localized("Run macro")
        }
    }

    companion object {
        fun fromRawValue(value: String?): SettingsMacrosActionFunction? {
            return entries.firstOrNull { it.rawValue == value }
        }
    }
}

enum class SettingsMacrosEvent(val rawValue: String) {
    TWITCH_FOLLOW("Twitch follow"),
    TWITCH_SUBSCRIPTION("Twitch subscription"),
    TWITCH_GIFT_SUBSCRIPTION("Twitch gift subscription"),
    TWITCH_RESUBSCRIPTION("Twitch resubscription"),
    TWITCH_REWARD("Twitch reward"),
    TWITCH_RAID("Twitch raid"),
    TWITCH_CHEER("Twitch cheer"),
    TWITCH_WATCH_STREAK("Twitch watch streak"),
    KICK_SUBSCRIPTION("Kick subscription"),
    KICK_GIFT_SUBSCRIPTIONS("Kick gift subscriptions"),
    KICK_REWARD("Kick reward"),
    KICK_HOST("Kick host"),
    KICK_KICKS("Kick kicks"),
    GO_LIVE("Stream started"),
    END("Stream stopped"),
    START_RECORDING("Recording started"),
    STOP_RECORDING("Recording stopped"),
    SWITCH_SCENE("Scene switched");

    override fun toString(): String {
        return when (this) {
            TWITCH_FOLLOW -> localized("Twitch follow")
            TWITCH_SUBSCRIPTION -> localized("Twitch subscription")
            TWITCH_GIFT_SUBSCRIPTION -> localized("Twitch gift subscription")
            TWITCH_RESUBSCRIPTION -> localized("Twitch resubscription")
            TWITCH_REWARD -> localized("Twitch reward")
            TWITCH_RAID -> localized("Twitch raid")
            TWITCH_CHEER -> localized("Twitch bits")
            TWITCH_WATCH_STREAK -> localized("Twitch watch streak")
            KICK_SUBSCRIPTION -> localized("Kick subscription")
            KICK_GIFT_SUBSCRIPTIONS -> localized("Kick gift subscriptions")
            KICK_REWARD -> localized("Kick reward")
            KICK_HOST -> localized("Kick host")
            KICK_KICKS -> localized("Kick kicks")
            GO_LIVE -> localized("Go live")
            END -> localized("End")
            START_RECORDING -> localized("Start recording")
            STOP_RECORDING -> localized("Stop recording")
            SWITCH_SCENE -> localized("Switch scene")
        }
    }

    fun minimumAmountTitle(): String? {
        return when (this) {
            TWITCH_GIFT_SUBSCRIPTION, KICK_GIFT_SUBSCRIPTIONS -> localized("Minimum subscriptions")
            TWITCH_RESUBSCRIPTION, KICK_SUBSCRIPTION -> localized("Minimum months")
            TWITCH_RAID, KICK_HOST -> localized("Minimum viewers")
            TWITCH_CHEER -> localized("Minimum bits")
            TWITCH_WATCH_STREAK -> localized("Minimum watch streak")
            KICK_KICKS -> localized("Minimum kicks")
            else -> null
        }
    }

    fun variables(): List<MacroVariable> {
        return when (this) {
            TWITCH_FOLLOW -> listOf(MacroVariable.TWITCH_FOLLOW_USER)
            TWITCH_SUBSCRIPTION -> listOf(MacroVariable.TWITCH_SUBSCRIPTION_USER)
            TWITCH_GIFT_SUBSCRIPTION -> listOf(MacroVariable.TWITCH_GIFT_SUBSCRIPTION_USER)
            TWITCH_RESUBSCRIPTION -> listOf(MacroVariable.TWITCH_RESUBSCRIPTION_USER)
            TWITCH_REWARD -> listOf(MacroVariable.TWITCH_REWARD_USER)
            TWITCH_WATCH_STREAK -> listOf(MacroVariable.TWITCH_WATCH_STREAK_USER)
            TWITCH_CHEER -> listOf(MacroVariable.TWITCH_CHEER_USER)
            TWITCH_RAID -> listOf(
                MacroVariable.TWITCH_RAID_CHANNEL_ID,
                MacroVariable.TWITCH_RAID_CHANNEL_NAME,
            )
            else -> emptyList()
        }
    }

    fun variablesToString(): String? {
        val variables = variables()
        if (variables.isEmpty()) {
            return null
        }
        return variables.joinToString(separator = ", ") { it.toString() }
    }

    fun textTitle(): String? {
        return when (this) {
            TWITCH_REWARD, KICK_REWARD -> localized("Reward")
            else -> null
        }
    }

    companion object {
        fun fromRawValue(value: String?): SettingsMacrosEvent? {
            return entries.firstOrNull { it.rawValue == value }
        }
    }
}

enum class MacroVariable(val rawValue: String) {
    TWITCH_FOLLOW_USER("twitchFollowUser"),
    TWITCH_SUBSCRIPTION_USER("twitchSubscriptionUser"),
    TWITCH_GIFT_SUBSCRIPTION_USER("twitchGiftSubscriptionUser"),
    TWITCH_RESUBSCRIPTION_USER("twitchResubscriptionUser"),
    TWITCH_REWARD_USER("twitchRewardUser"),
    TWITCH_WATCH_STREAK_USER("twitchWatchStreakUser"),
    TWITCH_CHEER_USER("twitchCheerUser"),
    TWITCH_RAID_CHANNEL_ID("twitchRaidChannelId"),
    TWITCH_RAID_CHANNEL_NAME("twitchRaidChannelName");

    override fun toString(): String {
        return "{$rawValue}"
    }
}

class MacroVariables {
    private val values: MutableMap<MacroVariable, String> = mutableMapOf()

    fun set(variables: Map<MacroVariable, String>) {
        values.putAll(variables)
    }

    fun get(variable: MacroVariable): String? {
        return values[variable]
    }

    fun removeAll() {
        values.clear()
    }

    fun substitute(text: String): String {
        var text = text
        for ((variable, value) in values) {
            text = text.replace(variable.toString(), value, ignoreCase = true)
        }
        return text
    }
}

data class MacroEvent(
    val event: SettingsMacrosEvent,
    var amount: Int = 0,
    var text: String = "",
    var sceneId: UUID? = null,
    var variables: MutableMap<MacroVariable, String> = mutableMapOf(),
)

enum class SettingsMacrosActionIfComparison(val rawValue: String) {
    EQUAL("="),
    NOT_EQUAL("!="),
    LESS_THAN("<"),
    LESS_EQUAL("<="),
    GREATER_THAN(">"),
    GREATER_EQUAL(">="),
    CONTAINS("Contains");

    override fun toString(): String {
        return when (this) {
            CONTAINS -> localized("Contains")
            else -> rawValue
        }
    }

    fun evaluate(value: String, otherValue: String): Boolean {
        if (this == CONTAINS) {
            return value.contains(otherValue, ignoreCase = true)
        }
        val number = toNumber(value)
        val otherNumber = toNumber(otherValue)
        if (number != null && otherNumber != null) {
            val order = when {
                number < otherNumber -> -1
                number > otherNumber -> 1
                else -> 0
            }
            return compare(order)
        }
        val order = value.compareTo(otherValue, ignoreCase = true)
        return compare(if (order < 0) -1 else if (order > 0) 1 else 0)
    }

    private fun compare(order: Int): Boolean {
        return when (this) {
            EQUAL -> order == 0
            NOT_EQUAL -> order != 0
            LESS_THAN -> order < 0
            LESS_EQUAL -> order <= 0
            GREATER_THAN -> order > 0
            GREATER_EQUAL -> order >= 0
            CONTAINS -> false
        }
    }

    private fun toNumber(value: String): Double? {
        val trimmed = value.trim()
        val prefix = trimmed.takeWhile { it.isDigit() || it == '.' || it == '-' || it == '+' }
        return prefix.toDoubleOrNull()
    }

    companion object {
        fun fromRawValue(value: String?): SettingsMacrosActionIfComparison? {
            return entries.firstOrNull { it.rawValue == value }
        }
    }
}

object SettingsMacrosActionSerializer : KSerializer<SettingsMacrosAction> {
    private const val ID = 0
    private const val FUNCTION = 1
    private const val SCENE_ID = 2
    private const val SCENE_IDS = 3
    private const val AUTO_SCENE_SWITCHER_ID = 4
    private const val ZOOM_X = 5
    private const val GIMBAL_PRESET_ID = 6
    private const val CHAT_MESSAGE = 7
    private const val DELAY = 8
    private const val MACRO_ID = 9
    private const val DJI_DEVICES = 10
    private const val FILTERS = 11
    private const val RECORD = 12
    private const val MUTE = 13
    private const val TORCH = 14
    private const val REACTION = 15
    private const val IF_VALUE = 16
    private const val IF_COMPARISON = 17
    private const val IF_OTHER_VALUE = 18
    private const val IF_RUN_COUNT = 19
    private const val EVENT = 20
    private const val EVENT_MINIMUM_AMOUNT = 21
    private const val EVENT_TEXT = 22
    private const val EVENT_SCENE_ID = 23

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("SettingsMacrosAction") {
            element("id", SettingsMacrosUuidSerializer.descriptor)
            element("function", settingsMacrosActionFunctionSerializer.nullable.descriptor)
            element("sceneId", SettingsMacrosUuidSerializer.nullable.descriptor)
            element("sceneIds", uuidSetSerializer.descriptor)
            element("autoSceneSwitcherId", SettingsMacrosUuidSerializer.nullable.descriptor)
            element("zoomX", PrimitiveSerialDescriptor("Float", PrimitiveKind.FLOAT))
            element("gimbalPresetId", SettingsMacrosUuidSerializer.nullable.descriptor)
            element("chatMessage", PrimitiveSerialDescriptor("String", PrimitiveKind.STRING))
            element("delay", PrimitiveSerialDescriptor("Double", PrimitiveKind.DOUBLE))
            element("macroId", SettingsMacrosUuidSerializer.nullable.descriptor)
            element("djiDevices", uuidSetSerializer.descriptor)
            element("filters", quickButtonTypeSetSerializer.descriptor)
            element("record", PrimitiveSerialDescriptor("Boolean", PrimitiveKind.BOOLEAN))
            element("mute", PrimitiveSerialDescriptor("Boolean", PrimitiveKind.BOOLEAN))
            element("torch", PrimitiveSerialDescriptor("Boolean", PrimitiveKind.BOOLEAN))
            element("reaction", settingsReactionSerializer.descriptor)
            element("ifValue", PrimitiveSerialDescriptor("String", PrimitiveKind.STRING))
            element("ifComparison", settingsMacrosActionIfComparisonSerializer.descriptor)
            element("ifOtherValue", PrimitiveSerialDescriptor("String", PrimitiveKind.STRING))
            element("ifRunCount", PrimitiveSerialDescriptor("Int", PrimitiveKind.INT))
            element("event", settingsMacrosEventSerializer.descriptor)
            element("eventMinimumAmount", PrimitiveSerialDescriptor("Int", PrimitiveKind.INT))
            element("eventText", PrimitiveSerialDescriptor("String", PrimitiveKind.STRING))
            element("eventSceneId", SettingsMacrosUuidSerializer.nullable.descriptor)
        }

    override fun serialize(encoder: Encoder, value: SettingsMacrosAction) {
        val composite = encoder.beginStructure(descriptor)
        composite.encodeSerializableElement(descriptor, ID, SettingsMacrosUuidSerializer, value.id)
        composite.encodeSerializableElement(
            descriptor,
            FUNCTION,
            settingsMacrosActionFunctionSerializer.nullable,
            value.function,
        )
        composite.encodeSerializableElement(descriptor, SCENE_ID, SettingsMacrosUuidSerializer.nullable, value.sceneId)
        composite.encodeSerializableElement(descriptor, SCENE_IDS, uuidSetSerializer, value.sceneIds)
        composite.encodeSerializableElement(
            descriptor,
            AUTO_SCENE_SWITCHER_ID,
            SettingsMacrosUuidSerializer.nullable,
            value.autoSceneSwitcherId,
        )
        composite.encodeFloatElement(descriptor, ZOOM_X, value.zoomX)
        composite.encodeSerializableElement(
            descriptor,
            GIMBAL_PRESET_ID,
            SettingsMacrosUuidSerializer.nullable,
            value.gimbalPresetId,
        )
        composite.encodeStringElement(descriptor, CHAT_MESSAGE, value.chatMessage)
        composite.encodeDoubleElement(descriptor, DELAY, value.delay)
        composite.encodeSerializableElement(descriptor, MACRO_ID, SettingsMacrosUuidSerializer.nullable, value.macroId)
        composite.encodeSerializableElement(descriptor, DJI_DEVICES, uuidSetSerializer, value.djiDevices)
        composite.encodeSerializableElement(
            descriptor,
            FILTERS,
            quickButtonTypeSetSerializer,
            value.filters,
        )
        composite.encodeBooleanElement(descriptor, RECORD, value.record)
        composite.encodeBooleanElement(descriptor, MUTE, value.mute)
        composite.encodeBooleanElement(descriptor, TORCH, value.torch)
        composite.encodeSerializableElement(
            descriptor,
            REACTION,
            settingsReactionSerializer,
            value.reaction,
        )
        composite.encodeStringElement(descriptor, IF_VALUE, value.ifValue)
        composite.encodeSerializableElement(
            descriptor,
            IF_COMPARISON,
            settingsMacrosActionIfComparisonSerializer,
            value.ifComparison,
        )
        composite.encodeStringElement(descriptor, IF_OTHER_VALUE, value.ifOtherValue)
        composite.encodeIntElement(descriptor, IF_RUN_COUNT, value.ifRunCount)
        composite.encodeSerializableElement(descriptor, EVENT, settingsMacrosEventSerializer, value.event)
        composite.encodeIntElement(descriptor, EVENT_MINIMUM_AMOUNT, value.eventMinimumAmount)
        composite.encodeStringElement(descriptor, EVENT_TEXT, value.eventText)
        composite.encodeSerializableElement(
            descriptor,
            EVENT_SCENE_ID,
            SettingsMacrosUuidSerializer.nullable,
            value.eventSceneId,
        )
        composite.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): SettingsMacrosAction {
        val composite = decoder.beginStructure(descriptor)
        val value = SettingsMacrosAction()
        while (true) {
            when (val index = composite.decodeElementIndex(descriptor)) {
                ID -> value.id = composite.decodeSerializableElement(descriptor, ID, SettingsMacrosUuidSerializer)
                FUNCTION -> value.function = composite.decodeSerializableElement(
                    descriptor,
                    FUNCTION,
                    settingsMacrosActionFunctionSerializer.nullable,
                    value.function,
                )
                SCENE_ID -> value.sceneId = composite.decodeSerializableElement(
                    descriptor,
                    SCENE_ID,
                    SettingsMacrosUuidSerializer.nullable,
                    value.sceneId,
                )
                SCENE_IDS -> value.sceneIds =
                    composite.decodeSerializableElement(descriptor, SCENE_IDS, uuidSetSerializer)
                AUTO_SCENE_SWITCHER_ID -> value.autoSceneSwitcherId = composite.decodeSerializableElement(
                    descriptor,
                    AUTO_SCENE_SWITCHER_ID,
                    SettingsMacrosUuidSerializer.nullable,
                    value.autoSceneSwitcherId,
                )
                ZOOM_X -> value.zoomX = composite.decodeFloatElement(descriptor, ZOOM_X)
                GIMBAL_PRESET_ID -> value.gimbalPresetId = composite.decodeSerializableElement(
                    descriptor,
                    GIMBAL_PRESET_ID,
                    SettingsMacrosUuidSerializer.nullable,
                    value.gimbalPresetId,
                )
                CHAT_MESSAGE -> value.chatMessage =
                    composite.decodeStringElement(descriptor, CHAT_MESSAGE)
                DELAY -> value.delay = composite.decodeDoubleElement(descriptor, DELAY)
                MACRO_ID -> value.macroId = composite.decodeSerializableElement(
                    descriptor,
                    MACRO_ID,
                    SettingsMacrosUuidSerializer.nullable,
                    value.macroId,
                )
                DJI_DEVICES -> value.djiDevices =
                    composite.decodeSerializableElement(descriptor, DJI_DEVICES, uuidSetSerializer)
                FILTERS -> value.filters = composite.decodeSerializableElement(
                    descriptor,
                    FILTERS,
                    quickButtonTypeSetSerializer,
                )
                RECORD -> value.record = composite.decodeBooleanElement(descriptor, RECORD)
                MUTE -> value.mute = composite.decodeBooleanElement(descriptor, MUTE)
                TORCH -> value.torch = composite.decodeBooleanElement(descriptor, TORCH)
                REACTION -> value.reaction = composite.decodeSerializableElement(
                    descriptor,
                    REACTION,
                    settingsReactionSerializer,
                )
                IF_VALUE -> value.ifValue = composite.decodeStringElement(descriptor, IF_VALUE)
                IF_COMPARISON -> value.ifComparison = composite.decodeSerializableElement(
                    descriptor,
                    IF_COMPARISON,
                    settingsMacrosActionIfComparisonSerializer,
                )
                IF_OTHER_VALUE -> value.ifOtherValue =
                    composite.decodeStringElement(descriptor, IF_OTHER_VALUE)
                IF_RUN_COUNT -> value.ifRunCount = composite.decodeIntElement(descriptor, IF_RUN_COUNT)
                EVENT -> value.event = composite.decodeSerializableElement(
                    descriptor,
                    EVENT,
                    settingsMacrosEventSerializer,
                )
                EVENT_MINIMUM_AMOUNT -> value.eventMinimumAmount =
                    composite.decodeIntElement(descriptor, EVENT_MINIMUM_AMOUNT)
                EVENT_TEXT -> value.eventText =
                    composite.decodeStringElement(descriptor, EVENT_TEXT)
                EVENT_SCENE_ID -> value.eventSceneId = composite.decodeSerializableElement(
                    descriptor,
                    EVENT_SCENE_ID,
                    SettingsMacrosUuidSerializer.nullable,
                    value.eventSceneId,
                )
                CompositeDecoder.DECODE_DONE -> break
                else -> throw SerializationException("Unexpected index $index")
            }
        }
        composite.endStructure(descriptor)
        return value
    }
}

@Serializable(with = SettingsMacrosActionSerializer::class)
class SettingsMacrosAction {
    var id: UUID = UUID.randomUUID()

    @Transient private val _function = MutableStateFlow<SettingsMacrosActionFunction?>(null)
    var function: SettingsMacrosActionFunction?
        get() = _function.value
        set(value) {
            _function.value = value
        }

    @Transient private val _sceneId = MutableStateFlow<UUID?>(null)
    var sceneId: UUID?
        get() = _sceneId.value
        set(value) {
            _sceneId.value = value
        }

    @Transient private val _sceneIds = MutableStateFlow<Set<UUID>>(emptySet())
    var sceneIds: Set<UUID>
        get() = _sceneIds.value
        set(value) {
            _sceneIds.value = value
        }

    @Transient private val _autoSceneSwitcherId = MutableStateFlow<UUID?>(null)
    var autoSceneSwitcherId: UUID?
        get() = _autoSceneSwitcherId.value
        set(value) {
            _autoSceneSwitcherId.value = value
        }

    @Transient private val _zoomX = MutableStateFlow(1f)
    var zoomX: Float
        get() = _zoomX.value
        set(value) {
            _zoomX.value = value
        }

    @Transient private val _gimbalPresetId = MutableStateFlow<UUID?>(null)
    var gimbalPresetId: UUID?
        get() = _gimbalPresetId.value
        set(value) {
            _gimbalPresetId.value = value
        }

    @Transient private val _chatMessage = MutableStateFlow("")
    var chatMessage: String
        get() = _chatMessage.value
        set(value) {
            _chatMessage.value = value
        }

    @Transient private val _delay = MutableStateFlow(3.0)
    var delay: Double
        get() = _delay.value
        set(value) {
            _delay.value = value
        }

    @Transient private val _macroId = MutableStateFlow<UUID?>(null)
    var macroId: UUID?
        get() = _macroId.value
        set(value) {
            _macroId.value = value
        }

    @Transient private val _djiDevices = MutableStateFlow<Set<UUID>>(emptySet())
    var djiDevices: Set<UUID>
        get() = _djiDevices.value
        set(value) {
            _djiDevices.value = value
        }

    @Transient private val _filters = MutableStateFlow<Set<SettingsQuickButtonType>>(emptySet())
    var filters: Set<SettingsQuickButtonType>
        get() = _filters.value
        set(value) {
            _filters.value = value
        }

    @Transient private val _record = MutableStateFlow(true)
    var record: Boolean
        get() = _record.value
        set(value) {
            _record.value = value
        }

    @Transient private val _mute = MutableStateFlow(true)
    var mute: Boolean
        get() = _mute.value
        set(value) {
            _mute.value = value
        }

    @Transient private val _torch = MutableStateFlow(true)
    var torch: Boolean
        get() = _torch.value
        set(value) {
            _torch.value = value
        }

    @Transient private val _reaction = MutableStateFlow(SettingsReaction.FIREWORKS)
    var reaction: SettingsReaction
        get() = _reaction.value
        set(value) {
            _reaction.value = value
        }

    @Transient private val _ifValue = MutableStateFlow("")
    var ifValue: String
        get() = _ifValue.value
        set(value) {
            _ifValue.value = value
        }

    @Transient private val _ifComparison = MutableStateFlow(SettingsMacrosActionIfComparison.EQUAL)
    var ifComparison: SettingsMacrosActionIfComparison
        get() = _ifComparison.value
        set(value) {
            _ifComparison.value = value
        }

    @Transient private val _ifOtherValue = MutableStateFlow("")
    var ifOtherValue: String
        get() = _ifOtherValue.value
        set(value) {
            _ifOtherValue.value = value
        }

    @Transient private val _ifRunCount = MutableStateFlow(1)
    var ifRunCount: Int
        get() = _ifRunCount.value
        set(value) {
            _ifRunCount.value = value
        }

    @Transient private val _event = MutableStateFlow(SettingsMacrosEvent.TWITCH_FOLLOW)
    var event: SettingsMacrosEvent
        get() = _event.value
        set(value) {
            _event.value = value
        }

    @Transient private val _eventMinimumAmount = MutableStateFlow(0)
    var eventMinimumAmount: Int
        get() = _eventMinimumAmount.value
        set(value) {
            _eventMinimumAmount.value = value
        }

    @Transient private val _eventText = MutableStateFlow("")
    var eventText: String
        get() = _eventText.value
        set(value) {
            _eventText.value = value
        }

    @Transient private val _eventSceneId = MutableStateFlow<UUID?>(null)
    var eventSceneId: UUID?
        get() = _eventSceneId.value
        set(value) {
            _eventSceneId.value = value
        }

    var needsWeather: Boolean = false
    var needsGeography: Boolean = false
    var needsGForce: Boolean = false

    fun matches(event: MacroEvent): Boolean {
        if (event.event != this.event || event.amount < eventMinimumAmount) {
            return false
        }
        val eventSceneId = this.eventSceneId
        if (eventSceneId != null && event.sceneId != eventSceneId) {
            return false
        }
        val text = eventText.trim()
        return text.isEmpty() || event.text.trim().equals(text, ignoreCase = true)
    }
}

enum class SettingsMacrosMacroRepeatMode(val rawValue: String) {
    OFF("off"),
    COUNT("count"),
    FOREVER("forever");

    override fun toString(): String {
        return when (this) {
            OFF -> localized("Off")
            COUNT -> localized("Count")
            FOREVER -> localized("Forever")
        }
    }

    companion object {
        fun fromRawValue(value: String?): SettingsMacrosMacroRepeatMode? {
            return entries.firstOrNull { it.rawValue == value }
        }
    }
}

object SettingsMacrosMacroSerializer : KSerializer<SettingsMacrosMacro> {
    private const val ID = 0
    private const val NAME = 1
    private const val ACTIONS = 2
    private const val REPEAT_MODE = 3
    private const val REPEAT_COUNT = 4
    private const val CLOSE_PANEL_ON_RUN = 5
    private const val RUN_AT_APP_START = 6

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("SettingsMacrosMacro") {
            element("id", SettingsMacrosUuidSerializer.descriptor)
            element("name", PrimitiveSerialDescriptor("String", PrimitiveKind.STRING))
            element("actions", ListSerializer(SettingsMacrosActionSerializer).descriptor)
            element("repeatMode", settingsMacrosMacroRepeatModeSerializer.descriptor)
            element("repeatCount", PrimitiveSerialDescriptor("Int", PrimitiveKind.INT))
            element("closePanelOnRun", PrimitiveSerialDescriptor("Boolean", PrimitiveKind.BOOLEAN))
            element("runAtAppStart", PrimitiveSerialDescriptor("Boolean", PrimitiveKind.BOOLEAN))
        }

    override fun serialize(encoder: Encoder, value: SettingsMacrosMacro) {
        val composite = encoder.beginStructure(descriptor)
        composite.encodeSerializableElement(descriptor, ID, SettingsMacrosUuidSerializer, value.id)
        composite.encodeStringElement(descriptor, NAME, value.name)
        composite.encodeSerializableElement(
            descriptor,
            ACTIONS,
            ListSerializer(SettingsMacrosActionSerializer),
            value.actions,
        )
        composite.encodeSerializableElement(
            descriptor,
            REPEAT_MODE,
            settingsMacrosMacroRepeatModeSerializer,
            value.repeatMode,
        )
        composite.encodeIntElement(descriptor, REPEAT_COUNT, value.repeatCount)
        composite.encodeBooleanElement(descriptor, CLOSE_PANEL_ON_RUN, value.closePanelOnRun)
        composite.encodeBooleanElement(descriptor, RUN_AT_APP_START, value.runAtAppStart)
        composite.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): SettingsMacrosMacro {
        val composite = decoder.beginStructure(descriptor)
        val value = SettingsMacrosMacro()
        while (true) {
            when (val index = composite.decodeElementIndex(descriptor)) {
                ID -> value.id = composite.decodeSerializableElement(descriptor, ID, SettingsMacrosUuidSerializer)
                NAME -> value.name = composite.decodeStringElement(descriptor, NAME)
                ACTIONS -> value.actions = composite.decodeSerializableElement(
                    descriptor,
                    ACTIONS,
                    ListSerializer(SettingsMacrosActionSerializer),
                )
                REPEAT_MODE -> value.repeatMode = composite.decodeSerializableElement(
                    descriptor,
                    REPEAT_MODE,
                    settingsMacrosMacroRepeatModeSerializer,
                )
                REPEAT_COUNT -> value.repeatCount =
                    composite.decodeIntElement(descriptor, REPEAT_COUNT)
                CLOSE_PANEL_ON_RUN -> value.closePanelOnRun =
                    composite.decodeBooleanElement(descriptor, CLOSE_PANEL_ON_RUN)
                RUN_AT_APP_START -> value.runAtAppStart =
                    composite.decodeBooleanElement(descriptor, RUN_AT_APP_START)
                CompositeDecoder.DECODE_DONE -> break
                else -> throw SerializationException("Unexpected index $index")
            }
        }
        composite.endStructure(descriptor)
        return value
    }
}

@Serializable(with = SettingsMacrosMacroSerializer::class)
class SettingsMacrosMacro : Named {
    var id: UUID = UUID.randomUUID()

    @Transient private val _name = MutableStateFlow(baseName)
    override var name: String
        get() = _name.value
        set(value) {
            _name.value = value
        }

    @Transient private val _actions = MutableStateFlow<List<SettingsMacrosAction>>(emptyList())
    var actions: List<SettingsMacrosAction>
        get() = _actions.value
        set(value) {
            _actions.value = value
        }

    @Transient private val _running = MutableStateFlow(false)
    var running: Boolean
        get() = _running.value
        set(value) {
            _running.value = value
        }

    @Transient private val _finished = MutableStateFlow(false)
    var finished: Boolean
        get() = _finished.value
        set(value) {
            _finished.value = value
        }

    @Transient private val _repeatMode = MutableStateFlow(SettingsMacrosMacroRepeatMode.OFF)
    var repeatMode: SettingsMacrosMacroRepeatMode
        get() = _repeatMode.value
        set(value) {
            _repeatMode.value = value
        }

    @Transient private val _repeatCount = MutableStateFlow(5)
    var repeatCount: Int
        get() = _repeatCount.value
        set(value) {
            _repeatCount.value = value
        }

    @Transient private val _closePanelOnRun = MutableStateFlow(false)
    var closePanelOnRun: Boolean
        get() = _closePanelOnRun.value
        set(value) {
            _closePanelOnRun.value = value
        }

    @Transient private val _runAtAppStart = MutableStateFlow(false)
    var runAtAppStart: Boolean
        get() = _runAtAppStart.value
        set(value) {
            _runAtAppStart.value = value
        }

    val variables: MacroVariables = MacroVariables()
    var nextActionIndex: Int = 0
    var waitingForEventAction: SettingsMacrosAction? = null
    var eventQueue: ArrayDeque<MacroEvent> = ArrayDeque()
    var repeatCurrentCount: Int = 0
    var delayed: Boolean = false
    val delayTimer: MainTimer = MainTimer()
    val finishedTimer: MainTimer = MainTimer()
    var stack: MutableList<SettingsMacrosMacro> = mutableListOf()

    fun copy(): SettingsMacrosMacro {
        val new = SettingsMacrosMacro()
        new.id = id
        new.name = name
        new.repeatMode = repeatMode
        new.repeatCount = repeatCount
        new.actions = actions
        return new
    }

    companion object {
        val baseName: String = localized("My macro")
    }
}

object SettingsMacrosSerializer : KSerializer<SettingsMacros> {
    private const val MACROS = 0

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("SettingsMacros") {
            element("macros", ListSerializer(SettingsMacrosMacroSerializer).descriptor)
        }

    override fun serialize(encoder: Encoder, value: SettingsMacros) {
        val composite = encoder.beginStructure(descriptor)
        composite.encodeSerializableElement(
            descriptor,
            MACROS,
            ListSerializer(SettingsMacrosMacroSerializer),
            value.macros,
        )
        composite.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): SettingsMacros {
        val composite = decoder.beginStructure(descriptor)
        val value = SettingsMacros()
        while (true) {
            when (val index = composite.decodeElementIndex(descriptor)) {
                MACROS -> value.macros = composite.decodeSerializableElement(
                    descriptor,
                    MACROS,
                    ListSerializer(SettingsMacrosMacroSerializer),
                )
                CompositeDecoder.DECODE_DONE -> break
                else -> throw SerializationException("Unexpected index $index")
            }
        }
        composite.endStructure(descriptor)
        return value
    }
}

@Serializable(with = SettingsMacrosSerializer::class)
class SettingsMacros {
    @Transient private val _macros = MutableStateFlow<List<SettingsMacrosMacro>>(emptyList())
    var macros: List<SettingsMacrosMacro>
        get() = _macros.value
        set(value) {
            _macros.value = value
        }
}

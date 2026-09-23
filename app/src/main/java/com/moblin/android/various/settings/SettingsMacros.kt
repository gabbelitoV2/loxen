package com.moblin.android.various.settings

import com.moblin.android.localized
import com.moblin.android.platform.codable.JsonObjectSerializer
import com.moblin.android.platform.codable.decode
import com.moblin.android.platform.codable.encodeContainer
import com.moblin.android.various.MainTimer
import com.moblin.android.various.utils.Named
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonObject

private fun <T> rawValueSerializer(
    serialName: String,
    rawValue: (T) -> String,
    fromRawValue: (String) -> T?,
): KSerializer<T> = object : KSerializer<T> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(serialName, PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeString(rawValue(value))
    }

    override fun deserialize(decoder: Decoder): T {
        val value = decoder.decodeString()
        return fromRawValue(value) ?: throw SerializationException("Unknown $serialName raw value '$value'")
    }
}

private fun <T> caseNameSerializer(serialName: String, cases: List<T>, caseName: (T) -> String): KSerializer<T> =
    JsonObjectSerializer(
        serialName,
        { value -> JsonObject(mapOf(caseName(value) to JsonObject(emptyMap()))) },
        { container ->
            val keys = container.keys.filter { key -> cases.any { caseName(it) == key } }
            if (keys.size != 1) {
                throw SerializationException("$serialName expects exactly one case key")
            }
            if (container[keys[0]] !is JsonObject) {
                throw SerializationException("$serialName case value must be an object")
            }
            cases.first { caseName(it) == keys[0] }
        },
    )

@Serializable(with = SettingsReaction.Serializer::class)
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

    object Serializer : KSerializer<SettingsReaction> by caseNameSerializer(
        "com.moblin.android.various.settings.SettingsReaction",
        entries,
        { it.rawValue },
    )
}

@Serializable(with = SettingsMacrosActionFunction.Serializer::class)
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

    object Serializer : KSerializer<SettingsMacrosActionFunction> by rawValueSerializer(
        "com.moblin.android.various.settings.SettingsMacrosActionFunction",
        { it.rawValue },
        { rawValue -> fromRawValue(rawValue) },
    )
}

@Serializable(with = SettingsMacrosEvent.Serializer::class)
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

    object Serializer : KSerializer<SettingsMacrosEvent> by rawValueSerializer(
        "com.moblin.android.various.settings.SettingsMacrosEvent",
        { it.rawValue },
        { rawValue -> fromRawValue(rawValue) },
    )
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

@Serializable(with = SettingsMacrosActionIfComparison.Serializer::class)
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

    object Serializer : KSerializer<SettingsMacrosActionIfComparison> by rawValueSerializer(
        "com.moblin.android.various.settings.SettingsMacrosActionIfComparison",
        { it.rawValue },
        { rawValue -> fromRawValue(rawValue) },
    )
}

@Serializable(with = SettingsMacrosAction.Serializer::class)
class SettingsMacrosAction {
    var id: UUID = UUID.randomUUID()

    private val _function = MutableStateFlow<SettingsMacrosActionFunction?>(null)
    var function: SettingsMacrosActionFunction?
        get() = _function.value
        set(value) {
            _function.value = value
        }

    private val _sceneId = MutableStateFlow<UUID?>(null)
    var sceneId: UUID?
        get() = _sceneId.value
        set(value) {
            _sceneId.value = value
        }

    private val _sceneIds = MutableStateFlow<Set<UUID>>(emptySet())
    var sceneIds: Set<UUID>
        get() = _sceneIds.value
        set(value) {
            _sceneIds.value = value
        }

    private val _autoSceneSwitcherId = MutableStateFlow<UUID?>(null)
    var autoSceneSwitcherId: UUID?
        get() = _autoSceneSwitcherId.value
        set(value) {
            _autoSceneSwitcherId.value = value
        }

    private val _zoomX = MutableStateFlow(1f)
    var zoomX: Float
        get() = _zoomX.value
        set(value) {
            _zoomX.value = value
        }

    private val _gimbalPresetId = MutableStateFlow<UUID?>(null)
    var gimbalPresetId: UUID?
        get() = _gimbalPresetId.value
        set(value) {
            _gimbalPresetId.value = value
        }

    private val _chatMessage = MutableStateFlow("")
    var chatMessage: String
        get() = _chatMessage.value
        set(value) {
            _chatMessage.value = value
        }

    private val _delay = MutableStateFlow(3.0)
    var delay: Double
        get() = _delay.value
        set(value) {
            _delay.value = value
        }

    private val _macroId = MutableStateFlow<UUID?>(null)
    var macroId: UUID?
        get() = _macroId.value
        set(value) {
            _macroId.value = value
        }

    private val _djiDevices = MutableStateFlow<Set<UUID>>(emptySet())
    var djiDevices: Set<UUID>
        get() = _djiDevices.value
        set(value) {
            _djiDevices.value = value
        }

    private val _filters = MutableStateFlow<Set<SettingsQuickButtonType>>(emptySet())
    var filters: Set<SettingsQuickButtonType>
        get() = _filters.value
        set(value) {
            _filters.value = value
        }

    private val _record = MutableStateFlow(true)
    var record: Boolean
        get() = _record.value
        set(value) {
            _record.value = value
        }

    private val _mute = MutableStateFlow(true)
    var mute: Boolean
        get() = _mute.value
        set(value) {
            _mute.value = value
        }

    private val _torch = MutableStateFlow(true)
    var torch: Boolean
        get() = _torch.value
        set(value) {
            _torch.value = value
        }

    private val _reaction = MutableStateFlow(SettingsReaction.FIREWORKS)
    var reaction: SettingsReaction
        get() = _reaction.value
        set(value) {
            _reaction.value = value
        }

    private val _ifValue = MutableStateFlow("")
    var ifValue: String
        get() = _ifValue.value
        set(value) {
            _ifValue.value = value
        }

    private val _ifComparison = MutableStateFlow(SettingsMacrosActionIfComparison.EQUAL)
    var ifComparison: SettingsMacrosActionIfComparison
        get() = _ifComparison.value
        set(value) {
            _ifComparison.value = value
        }

    private val _ifOtherValue = MutableStateFlow("")
    var ifOtherValue: String
        get() = _ifOtherValue.value
        set(value) {
            _ifOtherValue.value = value
        }

    private val _ifRunCount = MutableStateFlow(1)
    var ifRunCount: Int
        get() = _ifRunCount.value
        set(value) {
            _ifRunCount.value = value
        }

    private val _event = MutableStateFlow(SettingsMacrosEvent.TWITCH_FOLLOW)
    var event: SettingsMacrosEvent
        get() = _event.value
        set(value) {
            _event.value = value
        }

    private val _eventMinimumAmount = MutableStateFlow(0)
    var eventMinimumAmount: Int
        get() = _eventMinimumAmount.value
        set(value) {
            _eventMinimumAmount.value = value
        }

    private val _eventText = MutableStateFlow("")
    var eventText: String
        get() = _eventText.value
        set(value) {
            _eventText.value = value
        }

    private val _eventSceneId = MutableStateFlow<UUID?>(null)
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

    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("function", function)
        encode("sceneId", sceneId)
        encode("sceneIds", sceneIds)
        encode("autoSceneSwitcherId", autoSceneSwitcherId)
        encode("zoomX", zoomX)
        encode("gimbalPresetId", gimbalPresetId)
        encode("chatMessage", chatMessage)
        encode("delay", delay)
        encode("macroId", macroId)
        encode("djiDevices", djiDevices)
        encode("filters", filters)
        encode("record", record)
        encode("mute", mute)
        encode("torch", torch)
        encode("reaction", reaction)
        encode("ifValue", ifValue)
        encode("ifComparison", ifComparison)
        encode("ifOtherValue", ifOtherValue)
        encode("ifRunCount", ifRunCount)
        encode("event", event)
        encode("eventMinimumAmount", eventMinimumAmount)
        encode("eventText", eventText)
        encode("eventSceneId", eventSceneId)
    }

    companion object {
        fun decode(container: JsonObject): SettingsMacrosAction {
            val action = SettingsMacrosAction()
            action.id = container.decode("id", UUID.randomUUID())
            action.function = container.decode<SettingsMacrosActionFunction?>("function", null)
            action.sceneId = container.decode<UUID?>("sceneId", null)
            action.sceneIds = container.decode("sceneIds", emptySet<UUID>())
            action.autoSceneSwitcherId = container.decode<UUID?>("autoSceneSwitcherId", null)
            action.zoomX = container.decode("zoomX", 1f)
            action.gimbalPresetId = container.decode<UUID?>("gimbalPresetId", null)
            action.chatMessage = container.decode("chatMessage", "")
            action.delay = container.decode("delay", 3.0)
            action.macroId = container.decode<UUID?>("macroId", null)
            action.djiDevices = container.decode("djiDevices", emptySet<UUID>())
            action.filters = container.decode("filters", emptySet<SettingsQuickButtonType>())
            action.record = container.decode("record", true)
            action.mute = container.decode("mute", true)
            action.torch = container.decode("torch", true)
            action.reaction = container.decode("reaction", SettingsReaction.FIREWORKS)
            action.ifValue = container.decode("ifValue", "")
            action.ifComparison = container.decode("ifComparison", SettingsMacrosActionIfComparison.EQUAL)
            action.ifOtherValue = container.decode("ifOtherValue", "")
            action.ifRunCount = container.decode("ifRunCount", 1)
            action.event = container.decode("event", SettingsMacrosEvent.TWITCH_FOLLOW)
            action.eventMinimumAmount = container.decode("eventMinimumAmount", 0)
            action.eventText = container.decode("eventText", "")
            action.eventSceneId = container.decode<UUID?>("eventSceneId", null)
            return action
        }
    }

    object Serializer : KSerializer<SettingsMacrosAction> by JsonObjectSerializer(
        "SettingsMacrosAction",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsMacrosMacroRepeatMode.Serializer::class)
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

    object Serializer : KSerializer<SettingsMacrosMacroRepeatMode> by rawValueSerializer(
        "com.moblin.android.various.settings.SettingsMacrosMacroRepeatMode",
        { it.rawValue },
        { rawValue -> fromRawValue(rawValue) },
    )
}

@Serializable(with = SettingsMacrosMacro.Serializer::class)
class SettingsMacrosMacro : Named {
    var id: UUID = UUID.randomUUID()

    private val _name = MutableStateFlow(baseName)
    override var name: String
        get() = _name.value
        set(value) {
            _name.value = value
        }

    private val _actions = MutableStateFlow<List<SettingsMacrosAction>>(emptyList())
    var actions: List<SettingsMacrosAction>
        get() = _actions.value
        set(value) {
            _actions.value = value
        }

    private val _running = MutableStateFlow(false)
    var running: Boolean
        get() = _running.value
        set(value) {
            _running.value = value
        }

    private val _finished = MutableStateFlow(false)
    var finished: Boolean
        get() = _finished.value
        set(value) {
            _finished.value = value
        }

    private val _repeatMode = MutableStateFlow(SettingsMacrosMacroRepeatMode.OFF)
    var repeatMode: SettingsMacrosMacroRepeatMode
        get() = _repeatMode.value
        set(value) {
            _repeatMode.value = value
        }

    private val _repeatCount = MutableStateFlow(5)
    var repeatCount: Int
        get() = _repeatCount.value
        set(value) {
            _repeatCount.value = value
        }

    private val _closePanelOnRun = MutableStateFlow(false)
    var closePanelOnRun: Boolean
        get() = _closePanelOnRun.value
        set(value) {
            _closePanelOnRun.value = value
        }

    private val _runAtAppStart = MutableStateFlow(false)
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

    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("name", name)
        encode("actions", actions)
        encode("repeatMode", repeatMode)
        encode("repeatCount", repeatCount)
        encode("closePanelOnRun", closePanelOnRun)
        encode("runAtAppStart", runAtAppStart)
    }

    companion object {
        val baseName: String = localized("My macro")

        fun decode(container: JsonObject): SettingsMacrosMacro {
            val macro = SettingsMacrosMacro()
            macro.id = container.decode("id", UUID.randomUUID())
            macro.name = container.decode("name", baseName)
            macro.actions = container.decode("actions", ListSerializer(SettingsMacrosAction.serializer()), emptyList())
            macro.repeatMode = container.decode("repeatMode", SettingsMacrosMacroRepeatMode.OFF)
            macro.repeatCount = container.decode("repeatCount", 5)
            macro.closePanelOnRun = container.decode("closePanelOnRun", false)
            macro.runAtAppStart = container.decode("runAtAppStart", false)
            return macro
        }
    }

    object Serializer : KSerializer<SettingsMacrosMacro> by JsonObjectSerializer(
        "SettingsMacrosMacro",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsMacros.Serializer::class)
class SettingsMacros {
    private val _macros = MutableStateFlow<List<SettingsMacrosMacro>>(emptyList())
    var macros: List<SettingsMacrosMacro>
        get() = _macros.value
        set(value) {
            _macros.value = value
        }

    fun encode(): JsonObject = encodeContainer {
        encode("macros", macros)
    }

    companion object {
        fun decode(container: JsonObject): SettingsMacros {
            val macros = SettingsMacros()
            macros.macros = container.decode("macros", ListSerializer(SettingsMacrosMacro.serializer()), emptyList())
            return macros
        }
    }

    object Serializer : KSerializer<SettingsMacros> by JsonObjectSerializer(
        "SettingsMacros",
        { it.encode() },
        { decode(it) },
    )
}

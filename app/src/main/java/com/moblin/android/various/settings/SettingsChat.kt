package com.moblin.android.various.settings

import androidx.compose.ui.graphics.Color
import com.moblin.android.common.various.RgbColor
import com.moblin.android.common.various.color
import com.moblin.android.common.various.isValidHttpUrl
import com.moblin.android.localized
import com.moblin.android.platform.codable.JsonObjectSerializer
import com.moblin.android.platform.codable.decode
import com.moblin.android.platform.codable.decodeIfPresent
import com.moblin.android.platform.codable.encodeContainer
import com.moblin.android.platform.swiftui.Published
import com.moblin.android.platform.swiftui.PublishedList
import com.moblin.android.platform.swiftui.PublishedMap
import com.moblin.android.various.ChatPostSegment
import java.util.UUID
import kotlinx.serialization.KSerializer
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
import kotlinx.serialization.json.JsonObject

@Serializable(with = SettingsChatFilter.Serializer::class)
class SettingsChatFilter {
    var id: UUID = UUID.randomUUID()
    var enabled: Boolean by Published(false)
    var user: String by Published("")
    var messageStartWords: MutableList<String> = mutableListOf()
    var messageStart: String by Published(messageStartWords.joinToString(" "))
    var showOnScreen: Boolean by Published(false)
    var textToSpeech: Boolean by Published(false)
    var chatBot: Boolean by Published(false)
    var poll: Boolean by Published(false)
    var print: Boolean by Published(false)

    fun isMatching(user: String?, segments: List<ChatPostSegment>): Boolean {
        if (!enabled) {
            return false
        }
        if (this.user.isNotEmpty() && user != this.user) {
            return false
        }
        val segmentsIterator = segments.iterator()
        messageStartWords.forEachIndexed { index, messageWord ->
            val text = firstText(segmentsIterator)
            if (text != null) {
                if (index == messageStartWords.size - 1) {
                    if (!text.startsWith(messageWord)) {
                        return false
                    }
                } else {
                    if (text != messageWord) {
                        return false
                    }
                }
            } else {
                return false
            }
        }
        return true
    }

    fun username(): String {
        return if (user.isEmpty()) {
            localized("-- Any --")
        } else {
            user
        }
    }

    fun message(): String {
        return if (messageStart.isEmpty()) {
            localized("-- Any --")
        } else {
            messageStart
        }
    }

    private fun firstText(segmentsIterator: Iterator<ChatPostSegment>): String? {
        while (segmentsIterator.hasNext()) {
            val segment = segmentsIterator.next()
            val text = segment.text
            if (text != null && text.isNotEmpty()) {
                return text
            }
        }
        return null
    }

    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("enabled", enabled)
        encode("value", user)
        encode("messageWords", messageStartWords)
        encode("showOnScreen", showOnScreen)
        encode("textToSpeech", textToSpeech)
        encode("chatBot", chatBot)
        encode("poll", poll)
        encode("print", print)
    }

    companion object {
        fun decode(container: JsonObject): SettingsChatFilter {
            val filter = SettingsChatFilter()
            filter.id = container.decode("id", UUID.randomUUID())
            filter.enabled = container.decode("enabled", true)
            filter.user = container.decode("value", "")
            filter.messageStartWords = container.decode("messageWords", emptyList<String>()).toMutableList()
            filter.messageStart = filter.messageStartWords.joinToString(" ")
            filter.showOnScreen = container.decode("showOnScreen", false)
            filter.textToSpeech = container.decode("textToSpeech", false)
            filter.chatBot = container.decode("chatBot", false)
            filter.poll = container.decode("poll", false)
            filter.print = container.decode("print", false)
            return filter
        }
    }

    object Serializer : KSerializer<SettingsChatFilter> by JsonObjectSerializer(
        "SettingsChatFilter",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsChatBotPermissionsCommand.Serializer::class)
class SettingsChatBotPermissionsCommand(
    moderatorsEnabled: Boolean = true,
) {
    var moderatorsEnabled: Boolean by Published(moderatorsEnabled)
    var subscribersEnabled: Boolean by Published(false)
    var minimumSubscriberTier: Int by Published(1)
    var othersEnabled: Boolean by Published(false)
    var sendChatMessages: Boolean by Published(false)
    var cooldown: Int? by Published<Int?>(null)
    var latestExecutionTime: Long? = null

    fun encode(): JsonObject = encodeContainer {
        encode("moderatorsEnabled", moderatorsEnabled)
        encode("subscribersEnabled", subscribersEnabled)
        encode("minimumSubscriberTier", minimumSubscriberTier)
        encode("othersEnabled", othersEnabled)
        encode("sendChatMessages", sendChatMessages)
        encode("cooldown", cooldown)
    }

    companion object {
        fun decode(container: JsonObject): SettingsChatBotPermissionsCommand {
            val command = SettingsChatBotPermissionsCommand()
            command.moderatorsEnabled = container.decode("moderatorsEnabled", true)
            command.subscribersEnabled = container.decode("subscribersEnabled", false)
            command.minimumSubscriberTier = container.decode("minimumSubscriberTier", 1)
            command.othersEnabled = container.decode("othersEnabled", false)
            command.sendChatMessages = container.decode("sendChatMessages", false)
            command.cooldown = container.decode<Int?>("cooldown", null)
            return command
        }
    }

    object Serializer : KSerializer<SettingsChatBotPermissionsCommand> by JsonObjectSerializer(
        "SettingsChatBotPermissionsCommand",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsChatBotPermissions.Serializer::class)
class SettingsChatBotPermissions {
    var tts: SettingsChatBotPermissionsCommand = SettingsChatBotPermissionsCommand()
    var fix: SettingsChatBotPermissionsCommand = SettingsChatBotPermissionsCommand()
    var map: SettingsChatBotPermissionsCommand = SettingsChatBotPermissionsCommand()
    var alert: SettingsChatBotPermissionsCommand = SettingsChatBotPermissionsCommand()
    var fax: SettingsChatBotPermissionsCommand = SettingsChatBotPermissionsCommand()
    var snapshot: SettingsChatBotPermissionsCommand = SettingsChatBotPermissionsCommand()
    var filter: SettingsChatBotPermissionsCommand = SettingsChatBotPermissionsCommand()
    var zoom: SettingsChatBotPermissionsCommand = SettingsChatBotPermissionsCommand()
    var tesla: SettingsChatBotPermissionsCommand = SettingsChatBotPermissionsCommand()
    var audio: SettingsChatBotPermissionsCommand = SettingsChatBotPermissionsCommand()
    var reaction: SettingsChatBotPermissionsCommand = SettingsChatBotPermissionsCommand()
    var scene: SettingsChatBotPermissionsCommand =
        SettingsChatBotPermissionsCommand(moderatorsEnabled = false)
    var stream: SettingsChatBotPermissionsCommand =
        SettingsChatBotPermissionsCommand(moderatorsEnabled = false)
    var widget: SettingsChatBotPermissionsCommand = SettingsChatBotPermissionsCommand()
    var location: SettingsChatBotPermissionsCommand = SettingsChatBotPermissionsCommand()
    var ai: SettingsChatBotPermissionsCommand = SettingsChatBotPermissionsCommand()
    var twitch: SettingsChatBotPermissionsCommand = SettingsChatBotPermissionsCommand()
    var gimbal: SettingsChatBotPermissionsCommand = SettingsChatBotPermissionsCommand()
    var macro: SettingsChatBotPermissionsCommand =
        SettingsChatBotPermissionsCommand(moderatorsEnabled = false)
    var send: SettingsChatBotPermissionsCommand = SettingsChatBotPermissionsCommand()
    var music: SettingsChatBotPermissionsCommand = SettingsChatBotPermissionsCommand()
    var torch: SettingsChatBotPermissionsCommand = SettingsChatBotPermissionsCommand()
    var migrated: Boolean = false

    fun encode(): JsonObject = encodeContainer {
        encode("tts", tts)
        encode("fix", fix)
        encode("map", map)
        encode("alert", alert)
        encode("fax", fax)
        encode("snapshot", snapshot)
        encode("filter", filter)
        encode("zoom", zoom)
        encode("tesla", tesla)
        encode("audio", audio)
        encode("reaction", reaction)
        encode("scene", scene)
        encode("stream", stream)
        encode("widget", widget)
        encode("location", location)
        encode("ai", ai)
        encode("twitch", twitch)
        encode("gimbal", gimbal)
        encode("macro", macro)
        encode("send", send)
        encode("music", music)
        encode("torch", torch)
        encode("migrated", migrated)
    }

    companion object {
        fun decode(container: JsonObject): SettingsChatBotPermissions {
            val serializer = SettingsChatBotPermissionsCommand.serializer()
            val permissions = SettingsChatBotPermissions()
            permissions.tts = container.decode("tts", serializer, SettingsChatBotPermissionsCommand())
            permissions.fix = container.decode("fix", serializer, SettingsChatBotPermissionsCommand())
            permissions.map = container.decode("map", serializer, SettingsChatBotPermissionsCommand())
            permissions.alert = container.decode("alert", serializer, SettingsChatBotPermissionsCommand())
            permissions.fax = container.decode("fax", serializer, SettingsChatBotPermissionsCommand())
            permissions.snapshot = container.decode("snapshot", serializer, SettingsChatBotPermissionsCommand())
            permissions.filter = container.decode("filter", serializer, SettingsChatBotPermissionsCommand())
            permissions.zoom = container.decode("zoom", serializer, SettingsChatBotPermissionsCommand())
            permissions.tesla = container.decode("tesla", serializer, SettingsChatBotPermissionsCommand())
            permissions.audio = container.decode("audio", serializer, SettingsChatBotPermissionsCommand())
            permissions.reaction = container.decode("reaction", serializer, SettingsChatBotPermissionsCommand())
            permissions.scene = container.decode("scene", serializer, SettingsChatBotPermissionsCommand())
            permissions.stream = container.decode("stream", serializer, SettingsChatBotPermissionsCommand())
            permissions.widget = container.decode("widget", serializer, SettingsChatBotPermissionsCommand())
            permissions.location = container.decode("location", serializer, SettingsChatBotPermissionsCommand())
            permissions.ai = container.decode("ai", serializer, SettingsChatBotPermissionsCommand())
            permissions.twitch = container.decode("twitch", serializer, SettingsChatBotPermissionsCommand())
            permissions.gimbal = container.decode("gimbal", serializer, SettingsChatBotPermissionsCommand())
            permissions.macro = container.decode(
                "macro",
                serializer,
                SettingsChatBotPermissionsCommand(moderatorsEnabled = false),
            )
            permissions.send = container.decode("send", serializer, SettingsChatBotPermissionsCommand())
            permissions.music = container.decode("music", serializer, SettingsChatBotPermissionsCommand())
            permissions.torch = container.decode("torch", serializer, SettingsChatBotPermissionsCommand())
            permissions.migrated = container.decode("migrated", false)
            if (!permissions.migrated) {
                permissions.scene.moderatorsEnabled = false
                permissions.stream.moderatorsEnabled = false
                permissions.migrated = true
            }
            return permissions
        }
    }

    object Serializer : KSerializer<SettingsChatBotPermissions> by JsonObjectSerializer(
        "SettingsChatBotPermissions",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsChatBotAlias.Serializer::class)
class SettingsChatBotAlias {
    var id: UUID = UUID.randomUUID()
    var alias: String by Published("!myalias")
    var replacement: String by Published("!moblin")

    fun encode(): JsonObject = encodeContainer {
        encode("alias", alias)
        encode("replacement", replacement)
    }

    companion object {
        fun decode(container: JsonObject): SettingsChatBotAlias {
            val alias = SettingsChatBotAlias()
            alias.alias = container.decode("alias", "")
            alias.replacement = container.decode("replacement", "")
            return alias
        }
    }

    object Serializer : KSerializer<SettingsChatBotAlias> by JsonObjectSerializer(
        "SettingsChatBotAlias",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsChatBotCustomCommand.Serializer::class)
class SettingsChatBotCustomCommand {
    var id: UUID = UUID.randomUUID()
    var name: String by Published("myCommand")
    var formatString: String by Published("")
    var permissions: SettingsChatBotPermissionsCommand = SettingsChatBotPermissionsCommand()
    var needsWeather: Boolean = false
    var needsGeography: Boolean = false
    var needsGForce: Boolean = false

    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("name", name)
        encode("formatString", formatString)
        encode("permissions", permissions)
    }

    companion object {
        fun decode(container: JsonObject): SettingsChatBotCustomCommand {
            val command = SettingsChatBotCustomCommand()
            command.id = container.decode("id", UUID.randomUUID())
            command.name = container.decode("name", "")
            command.formatString = container.decode("formatString", "")
            command.permissions = container.decode(
                "permissions",
                SettingsChatBotPermissionsCommand.serializer(),
                SettingsChatBotPermissionsCommand(),
            )
            return command
        }
    }

    object Serializer : KSerializer<SettingsChatBotCustomCommand> by JsonObjectSerializer(
        "SettingsChatBotCustomCommand",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsChatPredefinedMessage.Serializer::class)
class SettingsChatPredefinedMessage {
    companion object {
        const val tagRed = "🌹"
        const val tagGreen = "🐸"
        const val tagBlue = "🐳"
        const val tagYellow = "🐥"
        const val tagOrange = "🦊"

        fun decode(container: JsonObject): SettingsChatPredefinedMessage {
            val message = SettingsChatPredefinedMessage()
            message.id = container.decode("id", UUID.randomUUID())
            message.text = container.decode("text", "")
            message.blueTag = container.decode("blueTag", false)
            message.greenTag = container.decode("greenTag", false)
            message.yellowTag = container.decode("yellowTag", false)
            message.orangeTag = container.decode("orangeTag", false)
            message.redTag = container.decode("redTag", false)
            return message
        }
    }

    var id: UUID = UUID.randomUUID()
    var text: String by Published("")
    var blueTag: Boolean by Published(false)
    var greenTag: Boolean by Published(false)
    var yellowTag: Boolean by Published(false)
    var orangeTag: Boolean by Published(false)
    var redTag: Boolean by Published(false)

    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("text", text)
        encode("blueTag", blueTag)
        encode("greenTag", greenTag)
        encode("yellowTag", yellowTag)
        encode("orangeTag", orangeTag)
        encode("redTag", redTag)
    }

    fun tagsString(): String {
        var tags = ""
        if (blueTag) {
            tags += tagBlue
        }
        if (greenTag) {
            tags += tagGreen
        }
        if (yellowTag) {
            tags += tagYellow
        }
        if (orangeTag) {
            tags += tagOrange
        }
        if (redTag) {
            tags += tagRed
        }
        return tags
    }

    object Serializer : KSerializer<SettingsChatPredefinedMessage> by JsonObjectSerializer(
        "SettingsChatPredefinedMessage",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsChatPredefinedMessagesFilter.Serializer::class)
class SettingsChatPredefinedMessagesFilter {
    var redTag: Boolean by Published(false)
    var greenTag: Boolean by Published(false)
    var blueTag: Boolean by Published(false)
    var yellowTag: Boolean by Published(false)
    var orangeTag: Boolean by Published(false)

    fun encode(): JsonObject = encodeContainer {
        encode("redTag", redTag)
        encode("greenTag", greenTag)
        encode("blueTag", blueTag)
        encode("yellowTag", yellowTag)
        encode("orangeTag", orangeTag)
    }

    fun isEnabled(): Boolean {
        return redTag || greenTag || blueTag || yellowTag || orangeTag
    }

    companion object {
        fun decode(container: JsonObject): SettingsChatPredefinedMessagesFilter {
            val filter = SettingsChatPredefinedMessagesFilter()
            filter.redTag = container.decode("redTag", false)
            filter.greenTag = container.decode("greenTag", false)
            filter.blueTag = container.decode("blueTag", false)
            filter.yellowTag = container.decode("yellowTag", false)
            filter.orangeTag = container.decode("orangeTag", false)
            return filter
        }
    }

    object Serializer : KSerializer<SettingsChatPredefinedMessagesFilter> by JsonObjectSerializer(
        "SettingsChatPredefinedMessagesFilter",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsChatNickname.Serializer::class)
class SettingsChatNickname {
    var id: UUID = UUID.randomUUID()
    var user: String by Published("")
    var nickname: String by Published("")

    fun encode(): JsonObject = encodeContainer {
        encode("id", id)
        encode("user", user)
        encode("nickname", nickname)
    }

    companion object {
        fun decode(container: JsonObject): SettingsChatNickname {
            val nickname = SettingsChatNickname()
            nickname.id = container.decode("id", UUID.randomUUID())
            nickname.user = container.decode("user", "")
            nickname.nickname = container.decode("nickname", "")
            return nickname
        }
    }

    object Serializer : KSerializer<SettingsChatNickname> by JsonObjectSerializer(
        "SettingsChatNickname",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsChatNicknames.Serializer::class)
class SettingsChatNicknames {
    var nicknames: MutableList<SettingsChatNickname> by PublishedList()

    fun encode(): JsonObject = encodeContainer {
        encode("nicknames", nicknames)
    }

    fun getNickname(user: String): String? {
        return nicknames.firstOrNull { it.user == user }?.nickname
    }

    companion object {
        fun decode(container: JsonObject): SettingsChatNicknames {
            val nicknames = SettingsChatNicknames()
            nicknames.nicknames = container.decode(
                "nicknames",
                ListSerializer(SettingsChatNickname.serializer()),
                emptyList(),
            ).toMutableList()
            return nicknames
        }
    }

    object Serializer : KSerializer<SettingsChatNicknames> by JsonObjectSerializer(
        "SettingsChatNicknames",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsOpenAi.Serializer::class)
class SettingsOpenAi(
    personality: String = defaultPersonality,
) {
    var personality: String by Published(personality)

    companion object {
        private const val defaultPersonality = "You give fast and short answers."
        private const val defaultModel = "gemini-3.5-flash-lite"

        fun decode(container: JsonObject): SettingsOpenAi {
            val openAi = SettingsOpenAi()
            openAi.baseUrl = container.decode(
                "baseUrl",
                "https://generativelanguage.googleapis.com/v1beta/openai",
            )
            openAi.apiKey = container.decode("apiKey", "")
            openAi.model = container.decode("model", defaultModel)
            openAi.personality = container.decode("role", defaultPersonality)
            return openAi
        }
    }

    var baseUrl: String by Published("https://generativelanguage.googleapis.com/v1beta/openai")
    var apiKey: String by Published("")
    var model: String by Published(defaultModel)

    fun encode(): JsonObject = encodeContainer {
        encode("baseUrl", baseUrl)
        encode("apiKey", apiKey)
        encode("model", model)
        encode("role", personality)
    }

    fun clone(): SettingsOpenAi {
        val new = SettingsOpenAi()
        new.baseUrl = baseUrl
        new.apiKey = apiKey
        new.model = model
        new.personality = personality
        return new
    }

    fun isConfigured(): Boolean {
        if (isValidHttpUrl(baseUrl) != null) {
            return false
        }
        if (apiKey.isEmpty()) {
            return false
        }
        if (model.isEmpty()) {
            return false
        }
        if (personality.isEmpty()) {
            return false
        }
        return true
    }

    object Serializer : KSerializer<SettingsOpenAi> by JsonObjectSerializer(
        "SettingsOpenAi",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable
enum class SettingsChatDisplayStyle(val rawValue: String) {
    internationalName("internationalName"),
    internationalNameAndUsername("internationalNameAndUsername"),
    username("username");

    override fun toString(): String {
        return when (this) {
            internationalName -> localized("International name")
            internationalNameAndUsername -> localized("International name (Username)")
            username -> localized("Username")
        }
    }

    companion object {
        fun fromRawValue(rawValue: String): SettingsChatDisplayStyle? {
            return entries.firstOrNull { it.rawValue == rawValue }
        }
    }
}

@Serializable(with = SettingsVoiceType.Serializer::class)
enum class SettingsVoiceType(val rawValue: String) {
    apple("apple"),
    ttsMonster("ttsMonster");

    companion object {
        fun fromRawValue(rawValue: String): SettingsVoiceType {
            return entries.firstOrNull { it.rawValue == rawValue } ?: apple
        }
    }

    object Serializer : KSerializer<SettingsVoiceType> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("com.moblin.android.various.settings.SettingsVoiceType", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: SettingsVoiceType) {
            encoder.encodeString(value.rawValue)
        }

        override fun deserialize(decoder: Decoder): SettingsVoiceType {
            return fromRawValue(decoder.decodeString())
        }
    }
}

@Serializable(with = SettingsVoiceApple.Serializer::class)
class SettingsVoiceApple {
    var voice: String = ""

    fun encode(): JsonObject = encodeContainer {
        encode("voice", voice)
    }

    companion object {
        fun decode(container: JsonObject): SettingsVoiceApple {
            val apple = SettingsVoiceApple()
            apple.voice = container.decodeIfPresent<String>("voice")
                ?: throw SerializationException("Missing key 'voice'")
            return apple
        }
    }

    object Serializer : KSerializer<SettingsVoiceApple> by JsonObjectSerializer(
        "SettingsVoiceApple",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsVoiceTtsMonster.Serializer::class)
class SettingsVoiceTtsMonster {
    var name: String = ""
    var voiceId: String = ""

    fun encode(): JsonObject = encodeContainer {
        encode("name", name)
        encode("voiceId", voiceId)
    }

    companion object {
        fun decode(container: JsonObject): SettingsVoiceTtsMonster {
            val ttsMonster = SettingsVoiceTtsMonster()
            ttsMonster.name = container.decodeIfPresent<String>("name")
                ?: throw SerializationException("Missing key 'name'")
            ttsMonster.voiceId = container.decodeIfPresent<String>("voiceId")
                ?: throw SerializationException("Missing key 'voiceId'")
            return ttsMonster
        }
    }

    object Serializer : KSerializer<SettingsVoiceTtsMonster> by JsonObjectSerializer(
        "SettingsVoiceTtsMonster",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsVoice.Serializer::class)
class SettingsVoice {
    var type: SettingsVoiceType = SettingsVoiceType.apple
    var apple: SettingsVoiceApple = SettingsVoiceApple()
    var ttsMonster: SettingsVoiceTtsMonster = SettingsVoiceTtsMonster()

    fun encode(): JsonObject = encodeContainer {
        encode("type", type)
        encode("apple", apple)
        encode("ttsMonster", ttsMonster)
    }

    companion object {
        fun decode(container: JsonObject): SettingsVoice {
            val voice = SettingsVoice()
            voice.type = container.decodeIfPresent<SettingsVoiceType>("type")
                ?: throw SerializationException("Missing key 'type'")
            voice.apple = container.decodeIfPresent("apple", SettingsVoiceApple.serializer())
                ?: throw SerializationException("Missing key 'apple'")
            voice.ttsMonster = container.decodeIfPresent("ttsMonster", SettingsVoiceTtsMonster.serializer())
                ?: throw SerializationException("Missing key 'ttsMonster'")
            return voice
        }
    }

    object Serializer : KSerializer<SettingsVoice> by JsonObjectSerializer(
        "SettingsVoice",
        { it.encode() },
        { decode(it) },
    )
}

@Serializable(with = SettingsChat.Serializer::class)
class SettingsChat {
    companion object {
        const val defaultBigGifScale: Float = 2.0f
        const val defaultActivityFeedHeight: Double = 0.2

        fun decode(container: JsonObject): SettingsChat {
            val chat = SettingsChat()
            chat.fontSize = container.decode("fontSize", 19.0f)
            chat.font = chat.font.copy(
                family = container.decode<String?>("fontFamily", null),
                style = container.decode("fontStyle", "")
            )
            chat.usernameColor = container.decode("usernameColor", RgbColor(red = 255, green = 163, blue = 0))
            chat.usernameColorColor = chat.usernameColor.color()
            chat.sameUsernameColor = container.decode("sameUsernameColor", false)
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
            chat.animatedEmotes = container.decode("animatedEmotes", false)
            chat.timestampColor = container.decode(
                "timestampColor",
                RgbColor(red = 180, green = 180, blue = 180),
            )
            chat.timestampColorColor = chat.timestampColor.color()
            chat.timestampColorEnabled = container.decode("timestampColorEnabled", false)
            chat.height = container.decode("height", 0.7)
            chat.width = container.decode("width", 1.0)
            chat.activityFeed = container.decode("activityFeed", true)
            chat.activityFeedHeight = container.decode("activityFeedHeight", defaultActivityFeedHeight)
            chat.maximumAge = container.decode("maximumAge", 30)
            chat.maximumAgeEnabled = container.decode("maximumAgeEnabled", false)
            chat.meInUsernameColor = container.decode("meInUsernameColor", true)
            chat.enabled = container.decode("enabled", true)
            chat.filters = container.decode(
                "usernamesToIgnore",
                ListSerializer(SettingsChatFilter.serializer()),
                emptyList(),
            ).toMutableList()
            chat.textToSpeechEnabled = container.decode("textToSpeechEnabled", false)
            chat.textToSpeechDefaultLanguage = container.decode<String?>("textToSpeechDefaultLanguage", null)
            chat.textToSpeechDetectLanguagePerMessage = container.decode(
                "textToSpeechDetectLanguagePerMessage",
                false,
            )
            chat.textToSpeechSayUsername = container.decode("textToSpeechSayUsername", true)
            chat.textToSpeechRate = container.decode("textToSpeechRate", 0.4f)
            chat.textToSpeechSayVolume = container.decode("textToSpeechSayVolume", 0.6f)
            chat.textToSpeechLanguageVoices = container.decode(
                "textToSpeechLanguageVoices",
                MapSerializer(String.serializer(), SettingsVoice.serializer()),
                emptyMap(),
            ).toMutableMap()
            for ((languageCode, voice) in container.decode(
                "textToSpeechLanguageVoices",
                MapSerializer(String.serializer(), String.serializer()),
                emptyMap(),
            )) {
                val settingsVoice = SettingsVoice()
                settingsVoice.apple.voice = voice
                chat.textToSpeechLanguageVoices[languageCode] = settingsVoice
            }
            chat.textToSpeechSubscribersOnly = container.decode("textToSpeechSubscribersOnly", false)
            chat.textToSpeechFilter = container.decode("textToSpeechFilter", true)
            chat.textToSpeechFilterMentions = container.decode("textToSpeechFilterMentions", true)
            chat.textToSpeechBluetoothSpeakerOnly = container.decode(
                "textToSpeechBluetoothSpeakerOnly",
                false,
            )
            chat.ttsMonster = container.decode("ttsMonster", SettingsTtsMonster.serializer(), SettingsTtsMonster())
            chat.mirrored = container.decode("mirrored", false)
            chat.botEnabled = container.decode("botEnabled", false)
            chat.botCommandPermissions = container.decode(
                "botCommandPermissions",
                SettingsChatBotPermissions.serializer(),
                SettingsChatBotPermissions(),
            )
            chat.botSendLowBatteryWarning = container.decode("botSendLowBatteryWarning", false)
            chat.botCommandAi = container.decode("botCommandAi", SettingsOpenAi.serializer(), SettingsOpenAi())
            chat.badges = container.decode("badges", true)
            chat.showFirstTimeChatterMessage = container.decode("showFirstTimeChatterMessage", true)
            chat.showNewFollowerMessage = container.decode("showNewFollowerMessage", true)
            chat.bottomPoints = container.decode("bottomPoints", 80.0)
            chat.newMessagesAtTop = container.decode("newMessagesAtTop", false)
            chat.textToSpeechPauseBetweenMessages = container.decode(
                "textToSpeechPauseBetweenMessages",
                1.0,
            )
            chat.showDeletedMessages = container.decode("showDeletedMessages", false)
            chat.aliases = container.decode(
                "aliases",
                ListSerializer(SettingsChatBotAlias.serializer()),
                emptyList(),
            ).toMutableList()
            chat.customCommands = container.decode(
                "customCommands",
                ListSerializer(SettingsChatBotCustomCommand.serializer()),
                emptyList(),
            ).toMutableList()
            chat.predefinedMessages = container.decode(
                "predefinedMessages",
                ListSerializer(SettingsChatPredefinedMessage.serializer()),
                emptyList(),
            ).toMutableList()
            chat.predefinedMessagesFilter = container.decode(
                "predefinedMessagesFilter",
                SettingsChatPredefinedMessagesFilter.serializer(),
                SettingsChatPredefinedMessagesFilter(),
            )
            chat.nicknames = container.decode(
                "nicknames",
                SettingsChatNicknames.serializer(),
                SettingsChatNicknames(),
            )
            chat.displayStyle = container.decode("displayStyle", SettingsChatDisplayStyle.internationalName)
            chat.background = container.decode("background", false)
            chat.sharedChatIcons = container.decode("sharedChatIcons", true)
            chat.bigGifScale = container.decode("bigGifScale", defaultBigGifScale)
            chat.compactEvents = container.decode("compactEvents", true)
            return chat
        }
    }

    var fontSize: Float by Published(19.0f)
    var font: SettingsFont by Published(SettingsFont())
    var usernameColor: RgbColor = RgbColor(red = 255, green = 163, blue = 0)
    var usernameColorColor: Color by Published(Color(usernameColor.red, usernameColor.green, usernameColor.blue))
    var sameUsernameColor: Boolean by Published(false)
    var messageColor: RgbColor = RgbColor(red = 255, green = 255, blue = 255)
    var messageColorColor: Color by Published(Color(messageColor.red, messageColor.green, messageColor.blue))
    var backgroundColor: RgbColor = RgbColor(red = 0, green = 0, blue = 0)
    var backgroundColorColor: Color by Published(
        Color(backgroundColor.red, backgroundColor.green, backgroundColor.blue),
    )
    var backgroundColorEnabled: Boolean by Published(false)
    var shadowColor: RgbColor = RgbColor(red = 0, green = 0, blue = 0)
    var shadowColorColor: Color by Published(Color(shadowColor.red, shadowColor.green, shadowColor.blue))
    var shadowColorEnabled: Boolean by Published(true)
    var boldUsername: Boolean by Published(true)
    var boldMessage: Boolean by Published(true)
    var animatedEmotes: Boolean by Published(false)
    var timestampColor: RgbColor = RgbColor(red = 180, green = 180, blue = 180)
    var timestampColorColor: Color by Published(Color(timestampColor.red, timestampColor.green, timestampColor.blue))
    var timestampColorEnabled: Boolean by Published(false)
    var height: Double by Published(0.7)
    var width: Double by Published(1.0)
    var activityFeedHeight: Double by Published(defaultActivityFeedHeight)
    var activityFeed: Boolean by Published(true)
    var maximumAge: Int by Published(30)
    var maximumAgeEnabled: Boolean by Published(false)
    var meInUsernameColor: Boolean by Published(true)
    var enabled: Boolean by Published(true)
    var filters: MutableList<SettingsChatFilter> by PublishedList()
    var textToSpeechEnabled: Boolean = false
    var textToSpeechDefaultLanguage: String? by Published<String?>(null)
    var textToSpeechDetectLanguagePerMessage: Boolean by Published(false)
    var textToSpeechSayUsername: Boolean by Published(true)
    var textToSpeechRate: Float by Published(0.4f)
    var textToSpeechSayVolume: Float by Published(0.6f)
    var textToSpeechLanguageVoices: MutableMap<String, SettingsVoice> by PublishedMap()
    var textToSpeechSubscribersOnly: Boolean by Published(false)
    var textToSpeechFilter: Boolean by Published(true)
    var textToSpeechFilterMentions: Boolean by Published(true)
    var textToSpeechBluetoothSpeakerOnly: Boolean by Published(false)
    var ttsMonster: SettingsTtsMonster by Published(SettingsTtsMonster())
    var mirrored: Boolean by Published(false)
    var botEnabled: Boolean by Published(false)
    var botCommandPermissions: SettingsChatBotPermissions = SettingsChatBotPermissions()
    var botSendLowBatteryWarning: Boolean = false
    var botCommandAi: SettingsOpenAi = SettingsOpenAi()
    var badges: Boolean by Published(true)
    var showFirstTimeChatterMessage: Boolean = true
    var showNewFollowerMessage: Boolean = true
    var bottomPoints: Double by Published(80.0)
    var newMessagesAtTop: Boolean by Published(false)
    var textToSpeechPauseBetweenMessages: Double by Published(1.0)
    var showDeletedMessages: Boolean by Published(false)
    var aliases: MutableList<SettingsChatBotAlias> by PublishedList()
    var customCommands: MutableList<SettingsChatBotCustomCommand> by PublishedList()
    var predefinedMessages: MutableList<SettingsChatPredefinedMessage> by PublishedList()
    var predefinedMessagesFilter: SettingsChatPredefinedMessagesFilter by Published(
        SettingsChatPredefinedMessagesFilter(),
    )
    var nicknames: SettingsChatNicknames by Published(SettingsChatNicknames())
    var displayStyle: SettingsChatDisplayStyle by Published(SettingsChatDisplayStyle.internationalNameAndUsername)
    var background: Boolean by Published(false)
    var sharedChatIcons: Boolean by Published(true)
    var bigGifScale: Float by Published(defaultBigGifScale)
    var compactEvents: Boolean by Published(true)

    fun encode(): JsonObject = encodeContainer {
        encode("fontSize", fontSize)
        encode("fontFamily", font.family)
        encode("fontStyle", font.style)
        encode("usernameColor", usernameColor)
        encode("sameUsernameColor", sameUsernameColor)
        encode("messageColor", messageColor)
        encode("backgroundColor", backgroundColor)
        encode("backgroundColorEnabled", backgroundColorEnabled)
        encode("shadowColor", shadowColor)
        encode("shadowColorEnabled", shadowColorEnabled)
        encode("boldUsername", boldUsername)
        encode("boldMessage", boldMessage)
        encode("animatedEmotes", animatedEmotes)
        encode("timestampColor", timestampColor)
        encode("timestampColorEnabled", timestampColorEnabled)
        encode("height", height)
        encode("width", width)
        encode("activityFeed", activityFeed)
        encode("activityFeedHeight", activityFeedHeight)
        encode("maximumAge", maximumAge)
        encode("maximumAgeEnabled", maximumAgeEnabled)
        encode("meInUsernameColor", meInUsernameColor)
        encode("enabled", enabled)
        encode("usernamesToIgnore", filters)
        encode("textToSpeechEnabled", textToSpeechEnabled)
        encode("textToSpeechDefaultLanguage", textToSpeechDefaultLanguage)
        encode("textToSpeechDetectLanguagePerMessage", textToSpeechDetectLanguagePerMessage)
        encode("textToSpeechSayUsername", textToSpeechSayUsername)
        encode("textToSpeechRate", textToSpeechRate)
        encode("textToSpeechSayVolume", textToSpeechSayVolume)
        encode("textToSpeechLanguageVoices", textToSpeechLanguageVoices)
        encode("textToSpeechSubscribersOnly", textToSpeechSubscribersOnly)
        encode("textToSpeechFilter", textToSpeechFilter)
        encode("textToSpeechFilterMentions", textToSpeechFilterMentions)
        encode("textToSpeechBluetoothSpeakerOnly", textToSpeechBluetoothSpeakerOnly)
        encode("ttsMonster", ttsMonster)
        encode("mirrored", mirrored)
        encode("botEnabled", botEnabled)
        encode("botCommandPermissions", botCommandPermissions)
        encode("botSendLowBatteryWarning", botSendLowBatteryWarning)
        encode("botCommandAi", botCommandAi)
        encode("badges", badges)
        encode("showFirstTimeChatterMessage", showFirstTimeChatterMessage)
        encode("showNewFollowerMessage", showNewFollowerMessage)
        encode("bottomPoints", bottomPoints)
        encode("newMessagesAtTop", newMessagesAtTop)
        encode("textToSpeechPauseBetweenMessages", textToSpeechPauseBetweenMessages)
        encode("showDeletedMessages", showDeletedMessages)
        encode("aliases", aliases)
        encode("customCommands", customCommands)
        encode("predefinedMessages", predefinedMessages)
        encode("predefinedMessagesFilter", predefinedMessagesFilter)
        encode("nicknames", nicknames)
        encode("displayStyle", displayStyle)
        encode("background", background)
        encode("sharedChatIcons", sharedChatIcons)
        encode("bigGifScale", bigGifScale)
        encode("compactEvents", compactEvents)
    }

    fun getRotation(): Double {
        return if (newMessagesAtTop) {
            0.0
        } else {
            180.0
        }
    }

    fun getScaleX(): Double {
        return if (newMessagesAtTop) {
            1.0
        } else {
            -1.0
        }
    }

    fun isMirrored(): Float {
        return if (mirrored) {
            -1f
        } else {
            1f
        }
    }

    object Serializer : KSerializer<SettingsChat> by JsonObjectSerializer(
        "SettingsChat",
        { it.encode() },
        { decode(it) },
    )
}

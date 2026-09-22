package com.moblin.android.various.settings

import androidx.compose.ui.graphics.Color
import com.moblin.android.common.various.RgbColor
import com.moblin.android.common.various.isValidHttpUrl
import com.moblin.android.localized
import com.moblin.android.various.ChatPostSegment
import java.util.UUID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class SettingsChatFilter {
    @SerialName("id")
    var id: UUID = UUID.randomUUID()

    @SerialName("enabled")
    var enabled: Boolean = true

    @SerialName("value")
    var user: String = ""

    @SerialName("messageWords")
    var messageStartWords: MutableList<String> = mutableListOf()

    @Transient
    var messageStart: String = messageStartWords.joinToString(" ")

    @SerialName("showOnScreen")
    var showOnScreen: Boolean = false

    @SerialName("textToSpeech")
    var textToSpeech: Boolean = false

    @SerialName("chatBot")
    var chatBot: Boolean = false

    @SerialName("poll")
    var poll: Boolean = false

    @SerialName("print")
    var print: Boolean = false

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
}

@Serializable
class SettingsChatBotPermissionsCommand(
    @SerialName("moderatorsEnabled")
    var moderatorsEnabled: Boolean = true,
) {
    @SerialName("subscribersEnabled")
    var subscribersEnabled: Boolean = false

    @SerialName("minimumSubscriberTier")
    var minimumSubscriberTier: Int = 1

    @SerialName("othersEnabled")
    var othersEnabled: Boolean = false

    @SerialName("sendChatMessages")
    var sendChatMessages: Boolean = false

    @SerialName("cooldown")
    var cooldown: Int? = null

    @Transient
    var latestExecutionTime: Long? = null
}

@Serializable
class SettingsChatBotPermissions {
    @SerialName("tts")
    var tts: SettingsChatBotPermissionsCommand = SettingsChatBotPermissionsCommand()

    @SerialName("fix")
    var fix: SettingsChatBotPermissionsCommand = SettingsChatBotPermissionsCommand()

    @SerialName("map")
    var map: SettingsChatBotPermissionsCommand = SettingsChatBotPermissionsCommand()

    @SerialName("alert")
    var alert: SettingsChatBotPermissionsCommand = SettingsChatBotPermissionsCommand()

    @SerialName("fax")
    var fax: SettingsChatBotPermissionsCommand = SettingsChatBotPermissionsCommand()

    @SerialName("snapshot")
    var snapshot: SettingsChatBotPermissionsCommand = SettingsChatBotPermissionsCommand()

    @SerialName("filter")
    var filter: SettingsChatBotPermissionsCommand = SettingsChatBotPermissionsCommand()

    @SerialName("zoom")
    var zoom: SettingsChatBotPermissionsCommand = SettingsChatBotPermissionsCommand()

    @SerialName("tesla")
    var tesla: SettingsChatBotPermissionsCommand = SettingsChatBotPermissionsCommand()

    @SerialName("audio")
    var audio: SettingsChatBotPermissionsCommand = SettingsChatBotPermissionsCommand()

    @SerialName("reaction")
    var reaction: SettingsChatBotPermissionsCommand = SettingsChatBotPermissionsCommand()

    @SerialName("scene")
    var scene: SettingsChatBotPermissionsCommand =
        SettingsChatBotPermissionsCommand(moderatorsEnabled = false)

    @SerialName("stream")
    var stream: SettingsChatBotPermissionsCommand =
        SettingsChatBotPermissionsCommand(moderatorsEnabled = false)

    @SerialName("widget")
    var widget: SettingsChatBotPermissionsCommand = SettingsChatBotPermissionsCommand()

    @SerialName("location")
    var location: SettingsChatBotPermissionsCommand = SettingsChatBotPermissionsCommand()

    @SerialName("ai")
    var ai: SettingsChatBotPermissionsCommand = SettingsChatBotPermissionsCommand()

    @SerialName("twitch")
    var twitch: SettingsChatBotPermissionsCommand = SettingsChatBotPermissionsCommand()

    @SerialName("gimbal")
    var gimbal: SettingsChatBotPermissionsCommand = SettingsChatBotPermissionsCommand()

    @SerialName("macro")
    var macro: SettingsChatBotPermissionsCommand =
        SettingsChatBotPermissionsCommand(moderatorsEnabled = false)

    @SerialName("send")
    var send: SettingsChatBotPermissionsCommand = SettingsChatBotPermissionsCommand()

    @SerialName("music")
    var music: SettingsChatBotPermissionsCommand = SettingsChatBotPermissionsCommand()

    @SerialName("migrated")
    var migrated: Boolean = false

    init {
        if (!migrated) {
            scene.moderatorsEnabled = false
            stream.moderatorsEnabled = false
            migrated = true
        }
    }
}

@Serializable
class SettingsChatBotAlias {
    @Transient
    var id: UUID = UUID.randomUUID()

    @SerialName("alias")
    var alias: String = "!myalias"

    @SerialName("replacement")
    var replacement: String = "!moblin"
}

@Serializable
class SettingsChatBotCustomCommand {
    @SerialName("id")
    var id: UUID = UUID.randomUUID()

    @SerialName("name")
    var name: String = "myCommand"

    @SerialName("formatString")
    var formatString: String = ""

    @SerialName("permissions")
    var permissions: SettingsChatBotPermissionsCommand = SettingsChatBotPermissionsCommand()

    @Transient
    var needsWeather: Boolean = false

    @Transient
    var needsGeography: Boolean = false

    @Transient
    var needsGForce: Boolean = false
}

@Serializable
class SettingsChatPredefinedMessage {
    companion object {
        const val tagRed = "🌹"
        const val tagGreen = "🐸"
        const val tagBlue = "🐳"
        const val tagYellow = "🐥"
        const val tagOrange = "🦊"
    }

    @SerialName("id")
    var id: UUID = UUID.randomUUID()

    @SerialName("text")
    var text: String = ""

    @SerialName("blueTag")
    var blueTag: Boolean = false

    @SerialName("greenTag")
    var greenTag: Boolean = false

    @SerialName("yellowTag")
    var yellowTag: Boolean = false

    @SerialName("orangeTag")
    var orangeTag: Boolean = false

    @SerialName("redTag")
    var redTag: Boolean = false

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
}

@Serializable
class SettingsChatPredefinedMessagesFilter {
    @SerialName("redTag")
    var redTag: Boolean = false

    @SerialName("greenTag")
    var greenTag: Boolean = false

    @SerialName("blueTag")
    var blueTag: Boolean = false

    @SerialName("yellowTag")
    var yellowTag: Boolean = false

    @SerialName("orangeTag")
    var orangeTag: Boolean = false

    fun isEnabled(): Boolean {
        return redTag || greenTag || blueTag || yellowTag || orangeTag
    }
}

@Serializable
class SettingsChatNickname {
    @SerialName("id")
    var id: UUID = UUID.randomUUID()

    @SerialName("user")
    var user: String = ""

    @SerialName("nickname")
    var nickname: String = ""
}

@Serializable
class SettingsChatNicknames {
    @SerialName("nicknames")
    var nicknames: MutableList<SettingsChatNickname> = mutableListOf()

    fun getNickname(user: String): String? {
        return nicknames.firstOrNull { it.user == user }?.nickname
    }
}

@Serializable
class SettingsOpenAi(
    @SerialName("role")
    var personality: String = defaultPersonality,
) {
    companion object {
        private const val defaultPersonality = "You give fast and short answers."
        private const val defaultModel = "gemini-3.5-flash-lite"
    }

    @SerialName("baseUrl")
    var baseUrl: String = "https://generativelanguage.googleapis.com/v1beta/openai"

    @SerialName("apiKey")
    var apiKey: String = ""

    @SerialName("model")
    var model: String = defaultModel

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

@Serializable
enum class SettingsVoiceType(val rawValue: String) {
    apple("apple"),
    ttsMonster("ttsMonster");

    companion object {
        fun fromRawValue(rawValue: String): SettingsVoiceType {
            return entries.firstOrNull { it.rawValue == rawValue } ?: apple
        }
    }
}

@Serializable
class SettingsVoiceApple {
    @SerialName("voice")
    var voice: String = ""
}

@Serializable
class SettingsVoiceTtsMonster {
    @SerialName("name")
    var name: String = ""

    @SerialName("voiceId")
    var voiceId: String = ""
}

@Serializable
class SettingsVoice {
    @SerialName("type")
    var type: SettingsVoiceType = SettingsVoiceType.apple

    @SerialName("apple")
    var apple: SettingsVoiceApple = SettingsVoiceApple()

    @SerialName("ttsMonster")
    var ttsMonster: SettingsVoiceTtsMonster = SettingsVoiceTtsMonster()
}

@Serializable
class SettingsChat {
    companion object {
        const val defaultBigGifScale: Float = 2.0f
        const val defaultActivityFeedHeight: Double = 0.2
    }

    @SerialName("fontSize")
    var fontSize: Float = 19.0f

    @SerialName("usernameColor")
    var usernameColor: RgbColor = RgbColor(red = 255, green = 163, blue = 0)

    @Transient
    var usernameColorColor: Color = usernameColor.color()

    @SerialName("sameUsernameColor")
    var sameUsernameColor: Boolean = false

    @SerialName("messageColor")
    var messageColor: RgbColor = RgbColor(red = 255, green = 255, blue = 255)

    @Transient
    var messageColorColor: Color = messageColor.color()

    @SerialName("backgroundColor")
    var backgroundColor: RgbColor = RgbColor(red = 0, green = 0, blue = 0)

    @Transient
    var backgroundColorColor: Color = backgroundColor.color()

    @SerialName("backgroundColorEnabled")
    var backgroundColorEnabled: Boolean = false

    @SerialName("shadowColor")
    var shadowColor: RgbColor = RgbColor(red = 0, green = 0, blue = 0)

    @Transient
    var shadowColorColor: Color = shadowColor.color()

    @SerialName("shadowColorEnabled")
    var shadowColorEnabled: Boolean = true

    @SerialName("boldUsername")
    var boldUsername: Boolean = true

    @SerialName("boldMessage")
    var boldMessage: Boolean = true

    @SerialName("animatedEmotes")
    var animatedEmotes: Boolean = false

    @SerialName("timestampColor")
    var timestampColor: RgbColor = RgbColor(red = 180, green = 180, blue = 180)

    @Transient
    var timestampColorColor: Color = timestampColor.color()

    @SerialName("timestampColorEnabled")
    var timestampColorEnabled: Boolean = false

    @SerialName("height")
    var height: Double = 0.7

    @SerialName("width")
    var width: Double = 1.0

    @SerialName("activityFeedHeight")
    var activityFeedHeight: Double = defaultActivityFeedHeight

    @SerialName("activityFeed")
    var activityFeed: Boolean = true

    @SerialName("maximumAge")
    var maximumAge: Int = 30

    @SerialName("maximumAgeEnabled")
    var maximumAgeEnabled: Boolean = false

    @SerialName("meInUsernameColor")
    var meInUsernameColor: Boolean = true

    @SerialName("enabled")
    var enabled: Boolean = true

    @SerialName("usernamesToIgnore")
    var filters: MutableList<SettingsChatFilter> = mutableListOf()

    @SerialName("textToSpeechEnabled")
    var textToSpeechEnabled: Boolean = false

    @SerialName("textToSpeechDefaultLanguage")
    var textToSpeechDefaultLanguage: String? = null

    @SerialName("textToSpeechDetectLanguagePerMessage")
    var textToSpeechDetectLanguagePerMessage: Boolean = false

    @SerialName("textToSpeechSayUsername")
    var textToSpeechSayUsername: Boolean = true

    @SerialName("textToSpeechRate")
    var textToSpeechRate: Float = 0.4f

    @SerialName("textToSpeechSayVolume")
    var textToSpeechSayVolume: Float = 0.6f

    @SerialName("textToSpeechLanguageVoices")
    var textToSpeechLanguageVoices: MutableMap<String, SettingsVoice> = mutableMapOf()

    @SerialName("textToSpeechSubscribersOnly")
    var textToSpeechSubscribersOnly: Boolean = false

    @SerialName("textToSpeechFilter")
    var textToSpeechFilter: Boolean = true

    @SerialName("textToSpeechFilterMentions")
    var textToSpeechFilterMentions: Boolean = true

    @SerialName("ttsMonster")
    var ttsMonster: SettingsTtsMonster = SettingsTtsMonster()

    @SerialName("mirrored")
    var mirrored: Boolean = false

    @SerialName("botEnabled")
    var botEnabled: Boolean = false

    @SerialName("botCommandPermissions")
    var botCommandPermissions: SettingsChatBotPermissions = SettingsChatBotPermissions()

    @SerialName("botSendLowBatteryWarning")
    var botSendLowBatteryWarning: Boolean = false

    @SerialName("botCommandAi")
    var botCommandAi: SettingsOpenAi = SettingsOpenAi()

    @SerialName("badges")
    var badges: Boolean = true

    @SerialName("showFirstTimeChatterMessage")
    var showFirstTimeChatterMessage: Boolean = true

    @SerialName("showNewFollowerMessage")
    var showNewFollowerMessage: Boolean = true

    @SerialName("bottomPoints")
    var bottomPoints: Double = 80.0

    @SerialName("newMessagesAtTop")
    var newMessagesAtTop: Boolean = false

    @SerialName("textToSpeechPauseBetweenMessages")
    var textToSpeechPauseBetweenMessages: Double = 1.0

    @SerialName("showDeletedMessages")
    var showDeletedMessages: Boolean = false

    @SerialName("aliases")
    var aliases: MutableList<SettingsChatBotAlias> = mutableListOf()

    @SerialName("customCommands")
    var customCommands: MutableList<SettingsChatBotCustomCommand> = mutableListOf()

    @SerialName("predefinedMessages")
    var predefinedMessages: MutableList<SettingsChatPredefinedMessage> = mutableListOf()

    @SerialName("predefinedMessagesFilter")
    var predefinedMessagesFilter: SettingsChatPredefinedMessagesFilter =
        SettingsChatPredefinedMessagesFilter()

    @SerialName("nicknames")
    var nicknames: SettingsChatNicknames = SettingsChatNicknames()

    @SerialName("displayStyle")
    var displayStyle: SettingsChatDisplayStyle = SettingsChatDisplayStyle.internationalName

    @SerialName("background")
    var background: Boolean = false

    @SerialName("sharedChatIcons")
    var sharedChatIcons: Boolean = true

    @SerialName("bigGifScale")
    var bigGifScale: Float = defaultBigGifScale

    @SerialName("compactEvents")
    var compactEvents: Boolean = true

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
}

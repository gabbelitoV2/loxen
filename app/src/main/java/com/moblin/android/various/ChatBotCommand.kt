package com.moblin.android.various

import com.moblin.android.streamingplatforms.Platform
import com.moblin.android.various.settings.SettingsChatBotAlias

private data class FuzzyMatchOptions(val threshold: Double, val distance: Int)

private val fuzzyMatchOptions = FuzzyMatchOptions(threshold = 0.34, distance = 1000)

internal fun fuzzyMatches(text: String, pattern: String): Boolean {
    return fuzzyMatchPattern(text + " ", pattern, fuzzyMatchOptions) != null
}

private fun fuzzyMatchPattern(text: String, pattern: String, options: FuzzyMatchOptions): Double? {
    val needle = pattern.lowercase()
    val haystack = text.lowercase()
    if (needle.isEmpty()) {
        return null
    }
    if (!isSubsequence(needle, haystack)) {
        return null
    }
    val distance = levenshteinDistance(needle, haystack)
    if (distance > options.distance) {
        return null
    }
    val length = maxOf(needle.length, haystack.length)
    if (length == 0) {
        return null
    }
    val similarity = 1.0 - distance.toDouble() / length.toDouble()
    if (similarity < options.threshold) {
        return null
    }
    return similarity
}

private fun isSubsequence(needle: String, haystack: String): Boolean {
    var index = 0
    for (character in haystack) {
        if (index < needle.length && character == needle[index]) {
            index += 1
        }
    }
    return index == needle.length
}

private fun levenshteinDistance(a: String, b: String): Int {
    val n = a.length
    val m = b.length
    if (n == 0) {
        return m
    }
    if (m == 0) {
        return n
    }
    var previous = IntArray(m + 1) { it }
    var current = IntArray(m + 1)
    for (i in 1..n) {
        current[0] = i
        for (j in 1..m) {
            val substitution = previous[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1
            current[j] = minOf(previous[j] + 1, current[j - 1] + 1, substitution)
        }
        val swap = previous
        previous = current
        current = swap
    }
    return previous[m]
}

internal inline fun <reified T> matchArgument(argument: String): T? where T : Enum<T>, T : ChatBotArgument {
    enumValues<T>().firstOrNull { it.rawValue == argument }?.let { return it }
    val matches = enumValues<T>().filter {
        fuzzyMatches(text = it.rawValue, pattern = argument) &&
            fuzzyMatches(text = argument, pattern = it.rawValue)
    }
    if (matches.size != 1) {
        return null
    }
    return matches.first()
}

interface ChatBotArgument {
    val rawValue: String
}

enum class ChatBotMainArgument(override val rawValue: String) : ChatBotArgument {
    help("help"),
    tts("tts"),
    obs("obs"),
    map("map"),
    location("location"),
    snapshot("snapshot"),
    mute("mute"),
    unmute("unmute"),
    alert("alert"),
    fax("fax"),
    filter("filter"),
    zoom("zoom"),
    say("say"),
    tesla("tesla"),
    reaction("reaction"),
    scene("scene"),
    stream("stream"),
    widget("widget"),
    ai("ai"),
    twitch("twitch"),
    gimbal("gimbal"),
    macro("macro"),
    send("send"),
    music("music"),
    custom("custom"),
}

enum class ChatBotOnOffArgument(override val rawValue: String) : ChatBotArgument {
    on("on"),
    off("off"),
}

enum class ChatBotObsArgument(override val rawValue: String) : ChatBotArgument {
    fix("fix"),
}

enum class ChatBotMapArgument(override val rawValue: String) : ChatBotArgument {
    zoom("zoom"),
}

enum class ChatBotMapZoomArgument(override val rawValue: String) : ChatBotArgument {
    out("out"),
}

enum class ChatBotLocationArgument(override val rawValue: String) : ChatBotArgument {
    data("data"),
}

enum class ChatBotLocationDataArgument(override val rawValue: String) : ChatBotArgument {
    reset("reset"),
    split("split"),
}

enum class ChatBotAiArgument(override val rawValue: String) : ChatBotArgument {
    ask("ask"),
}

enum class ChatBotTwitchArgument(override val rawValue: String) : ChatBotArgument {
    raid("raid"),
}

enum class ChatBotGimbalArgument(override val rawValue: String) : ChatBotArgument {
    preset("preset"),
}

enum class ChatBotMacroArgument(override val rawValue: String) : ChatBotArgument {
    run("run"),
    cancel("cancel"),
}

enum class ChatBotMusicArgument(override val rawValue: String) : ChatBotArgument {
    play("play"),
    pause("pause"),
    add("add"),
    next("next"),
    previous("previous"),
    status("status"),
}

enum class ChatBotStreamArgument(override val rawValue: String) : ChatBotArgument {
    start("start"),
    stop("stop"),
    title("title"),
    category("category"),
}

enum class ChatBotWidgetArgument(override val rawValue: String) : ChatBotArgument {
    enable("enable"),
    disable("disable"),
    timer("timer"),
    wheelOfLuck("wheelofluck"),
}

enum class ChatBotWidgetTimerArgument(override val rawValue: String) : ChatBotArgument {
    add("add"),
}

enum class ChatBotWidgetWheelOfLuckArgument(override val rawValue: String) : ChatBotArgument {
    spin("spin"),
    options("options"),
}

enum class ChatBotFilterArgument(override val rawValue: String) : ChatBotArgument {
    movie("movie"),
    grayscale("grayscale"),
    sepia("sepia"),
    triple("triple"),
    twin("twin"),
    pixellate("pixellate"),
    fourThree("4:3"),
    whirlpool("whirlpool"),
    pinch("pinch"),
}

enum class ChatBotReactionArgument(override val rawValue: String) : ChatBotArgument {
    fireworks("fireworks"),
    balloons("balloons"),
    hearts("hearts"),
    confetti("confetti"),
    lasers("lasers"),
    rain("rain"),
    glasses("glasses"),
    sparkle("sparkle"),
}

enum class ChatBotTeslaArgument(override val rawValue: String) : ChatBotArgument {
    trunk("trunk"),
    media("media"),
}

enum class ChatBotTeslaTrunkArgument(override val rawValue: String) : ChatBotArgument {
    open("open"),
    close("close"),
}

enum class ChatBotTeslaMediaArgument(override val rawValue: String) : ChatBotArgument {
    next("next"),
    previous("previous"),
    togglePlayback("toggle-playback"),
}

data class ChatBotMessage(
    val platform: Platform,
    val user: String?,
    val isOwner: Boolean,
    val isModerator: Boolean,
    val isSubscriber: Boolean,
    val userId: String?,
    val segments: List<ChatPostSegment>,
)

class ChatBotCommand private constructor(
    val message: ChatBotMessage,
) {
    private val parts: ArrayDeque<String> = ArrayDeque()

    companion object {
        operator fun invoke(
            message: ChatBotMessage,
            aliases: List<SettingsChatBotAlias>,
        ): ChatBotCommand? {
            val firstWord = message.segments.firstOrNull()?.text?.lowercase()?.trim() ?: return null
            val command = ChatBotCommand(message)
            if (firstWord != "!moblin") {
                val alias = aliases.firstOrNull { it.alias == firstWord } ?: return null
                for (word in alias.replacement.split(" ").filter { it.isNotEmpty() }.drop(1)) {
                    command.parts.addLast(word.trim())
                }
            }
            if (message.segments.size > 1) {
                for (segment in message.segments.drop(1)) {
                    val text = segment.text
                    if (text != null) {
                        command.parts.addLast(text.trim())
                    }
                }
            }
            return command
        }
    }

    fun popFirst(): String? {
        var first = parts.removeFirstOrNull() ?: return null
        if (!first.startsWith("\"")) {
            return first
        }
        first = first.substring(1)
        if (first == "\"") {
            return ""
        }
        val words = mutableListOf(first)
        while (true) {
            var word = parts.removeFirstOrNull() ?: return null
            if (word.endsWith("\"")) {
                word = word.dropLast(1)
                words.add(word)
                return words.joinToString(" ")
            } else {
                words.add(word)
            }
        }
    }

    fun popFirstLowerCased(): String? {
        return popFirst()?.lowercase()
    }

    internal inline fun <reified T> popFirstArgument(): T? where T : Enum<T>, T : ChatBotArgument {
        val word = popFirstLowerCased() ?: return null
        return matchArgument<T>(word)
    }

    fun popFirstInt(range: ClosedRange<Int>): Int? {
        val value = popFirst() ?: return null
        val intValue = value.toIntOrNull() ?: return null
        if (!range.contains(intValue)) {
            return null
        }
        return intValue
    }

    fun popFirstDouble(range: ClosedRange<Double>): Double? {
        val value = popFirst() ?: return null
        val doubleValue = value.toDoubleOrNull() ?: return null
        if (!range.contains(doubleValue)) {
            return null
        }
        return doubleValue
    }

    fun popAll(): List<String> {
        val parts: MutableList<String> = mutableListOf()
        while (true) {
            val part = popFirst() ?: break
            parts.add(part)
        }
        return parts
    }

    fun peekFirst(): String? {
        return parts.firstOrNull()
    }

    fun rest(): String {
        return parts.joinToString(" ")
    }

    fun user(): String? {
        return message.user
    }
}

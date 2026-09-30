package com.moblin.android.various

import android.icu.text.BreakIterator
import com.moblin.android.streamingplatforms.Platform
import com.moblin.android.various.settings.SettingsChatBotAlias
import kotlin.math.abs

private data class FuzzyMatchOptions(val threshold: Double, val distance: Double)

private val fuzzyMatchOptions = FuzzyMatchOptions(threshold = 0.34, distance = 1000.0)

internal fun fuzzyMatches(text: String, pattern: String): Boolean {
    return fuzzyMatchPattern(text + " ", pattern, fuzzyMatchOptions) != null
}

private fun characters(text: String): List<String> {
    val iterator = BreakIterator.getCharacterInstance()
    iterator.setText(text)
    val characters = mutableListOf<String>()
    var start = iterator.first()
    var end = iterator.next()
    while (end != BreakIterator.DONE) {
        characters.add(text.substring(start, end))
        start = end
        end = iterator.next()
    }
    return characters
}

private fun fuzzyMatchPattern(text: String, pattern: String, options: FuzzyMatchOptions): Int? {
    if (text.isEmpty()) {
        return null
    }
    val location = 0
    if (text.equals(pattern, ignoreCase = true)) {
        return 0
    }
    if (pattern.isEmpty()) {
        return null
    }
    val textCharacters = characters(text)
    val patternCharacters = characters(pattern)
    if (patternCharacters.size <= textCharacters.size &&
        pattern.equals(textCharacters.subList(0, patternCharacters.size).joinToString(""), ignoreCase = true)
    ) {
        return location
    }
    return matchBitap(textCharacters, patternCharacters, location, options.threshold, options.distance)
}

private fun matchBitap(
    text: List<String>,
    pattern: List<String>,
    loc: Int,
    threshold: Double,
    distance: Double,
): Int? {
    val alphabet = matchAlphabet(pattern)
    var scoreThreshold = threshold
    var bestLoc = searchForSubstring(text, pattern, loc)
    val matchMask = shiftLeft(1, pattern.size - 1)
    var lastRd = LongArray(0)
    for (index in pattern.indices) {
        var binMin = 0
        var binMax = pattern.size + text.size
        var binMid = binMax
        while (binMin < binMid) {
            if (bitapScore(index, loc + binMid, loc, pattern, distance) <= scoreThreshold) {
                binMin = binMid
            } else {
                binMax = binMid
            }
            binMid = (binMax - binMin) / 2 + binMin
        }
        val start = if (loc <= binMid) 1 else loc - binMid + 1
        val finish = minOf(loc + binMid, text.size) + pattern.size
        val rd = LongArray(finish + 2)
        rd[finish + 1] = shiftLeft(1, index) - 1
        for (j in finish downTo start) {
            val charMatch = characterMatch(text, j, alphabet)
            rd[j] = if (index == 0) {
                ((rd[j + 1] shl 1) or 1) and charMatch
            } else {
                val lastMatch = ((lastRd[j + 1] or lastRd[j]) shl 1) or 1
                (((rd[j + 1] shl 1) or 1) and charMatch) or lastMatch or lastRd[j + 1]
            }
            if ((rd[j] and matchMask) != 0L) {
                val score = bitapScore(index, j - 1, loc, pattern, distance)
                if (score <= scoreThreshold) {
                    scoreThreshold = score
                    bestLoc = j - 1
                    if (j - 1 <= loc) {
                        break
                    }
                }
            }
        }
        if (bitapScore(index + 1, loc, loc, pattern, distance) > scoreThreshold) {
            break
        }
        lastRd = rd
    }
    return bestLoc
}

private fun shiftLeft(value: Long, count: Int): Long {
    return if (count >= Long.SIZE_BITS) 0 else value shl count
}

private fun characterMatch(text: List<String>, position: Int, alphabet: Map<String, Long>): Long {
    if (position <= 0 || position >= text.size) {
        return 0
    }
    return alphabet[text[position - 1]] ?: 0
}

private fun matchAlphabet(pattern: List<String>): Map<String, Long> {
    val alphabet = mutableMapOf<String, Long>()
    for ((index, character) in pattern.withIndex()) {
        alphabet[character] = (alphabet[character] ?: 0) or shiftLeft(1, pattern.size - index - 1)
    }
    return alphabet
}

private fun bitapScore(errorCount: Int, x: Int, loc: Int, pattern: List<String>, distance: Double): Double {
    val accuracy = errorCount.toDouble() / pattern.size.toDouble()
    val proximity = abs(loc - x)
    if (distance == 0.0) {
        return accuracy
    }
    return accuracy + proximity.toDouble() / distance
}

private fun characterIndex(characters: List<String>, offset: Int): Int {
    var end = 0
    for ((index, character) in characters.withIndex()) {
        end += character.length
        if (end > offset) {
            return index
        }
    }
    return characters.size
}

private fun searchForSubstring(text: List<String>, pattern: List<String>, loc: Int): Int? {
    val textString = text.joinToString("")
    val patternString = pattern.joinToString("")
    val forwardMatch = textString.indexOf(patternString)
    if (forwardMatch == -1) {
        return null
    }
    val backwardEnd = text.subList(0, minOf(loc + pattern.size, text.size)).sumOf { it.length }
    val backwardMatch = textString.substring(0, backwardEnd).lastIndexOf(patternString)
    if (backwardMatch == -1) {
        return characterIndex(text, forwardMatch)
    }
    return characterIndex(text, backwardMatch)
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
    torch("torch"),
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
    `out`("out"),
}

enum class ChatBotLocationArgument(override val rawValue: String) : ChatBotArgument {
    `data`("data"),
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
    `open`("open"),
    close("close"),
}

enum class ChatBotTeslaMediaArgument(override val rawValue: String) : ChatBotArgument {
    next("next"),
    previous("previous"),
    togglePlayback("toggle-playback"),
}

enum class ChatBotTorchArgument(override val rawValue: String) : ChatBotArgument {
    on("on"),
    off("off"),
    level("level"),
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

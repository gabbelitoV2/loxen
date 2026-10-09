package com.moblin.android.streamingplatforms.twitch

import com.moblin.android.platform.log.Log
import com.moblin.android.common.various.RgbColor
import com.moblin.android.integrations.emotes.Emotes
import com.moblin.android.integrations.emotes.EmotesPlatform
import com.moblin.android.various.ChatHighlight
import com.moblin.android.various.ChatMessageEmote
import com.moblin.android.various.ChatPostEmote
import com.moblin.android.various.ChatPostSegment
import com.moblin.android.various.MainTimer
import com.moblin.android.various.network.WebSocketClient
import com.moblin.android.various.network.WebSocketClientDelegate
import com.moblin.android.various.settings.SettingsStreamChat
import java.net.URI
import java.util.concurrent.atomic.AtomicInteger
import com.moblin.android.AppDelegate

private sealed class MessageError(message: String) : Exception(message) {
    class InvalidCommand(val command: String) : MessageError("Invalid command: $command")

    class MissingCommand(val string: String) : MessageError("Missing command: $string")
}

enum class TwitchChatCommand(val rawValue: String) {
    ping("PING"),
    privateMessage("PRIVMSG"),
    userNotice("USERNOTICE"),
    clearChat("CLEARCHAT"),
    clearMsg("CLEARMSG"),
    ;

    companion object {
        fun fromRawValue(value: String): TwitchChatCommand? {
            return entries.firstOrNull { it.rawValue == value }
        }
    }
}

fun String.removingPrefix(prefix: String): String {
    if (!startsWith(prefix)) {
        return this
    }
    return substring(prefix.length)
}

private fun parseRange(string: String): IntRange? {
    val rangeIndexStrings = string.split("-")
    if (rangeIndexStrings.size != 2) {
        return null
    }
    val rangeStartIndex = rangeIndexStrings.first().toIntOrNull() ?: return null
    val rangeEndIndex = rangeIndexStrings.last().toIntOrNull() ?: return null
    if (rangeStartIndex > rangeEndIndex) {
        return null
    }
    return rangeStartIndex..rangeEndIndex
}

private fun parseEmotes(from: String): List<ChatMessageEmote> {
    val emoteDefinitions = from.split("/")
    return emoteDefinitions.flatMap { emotes(fromDefinition = it) }
}

private fun emotes(fromDefinition: String): List<ChatMessageEmote> {
    val parts = fromDefinition.split(":")
    if (parts.size != 2) {
        return emptyList()
    }
    val emoteId = parts.first()
    val emoteRangesString = parts.last()
    val urls = makeTwitchEmoteUrls(emoteId) ?: return emptyList()
    val result = mutableListOf<ChatMessageEmote>()
    for (emoteRangeString in emoteRangesString.split(",")) {
        val range = parseRange(emoteRangeString) ?: continue
        result.add(ChatMessageEmote(url = urls.moving, stillUrl = urls.still, range = range))
    }
    return result
}

private fun parseGif(from: String): ChatMessageEmote? {
    val parts = from.split("|", limit = 3)
    if (parts.size != 3) {
        return null
    }
    val range = parseRange(parts[0]) ?: return null
    val url = parts[2]
    return ChatMessageEmote(url = lowResolutionGiphyUrl(url), range = range, isGif = true)
}

private fun lowResolutionGiphyUrl(url: String): String {
    val host = runCatching { URI(url).host }.getOrNull()
    if (host == null || !host.endsWith("giphy.com")) {
        return url
    }
    val pathEndIndex = url.indexOfAny(charArrayOf('?', '#')).takeIf { it != -1 } ?: url.length
    val pathStartIndex = url.indexOf('/', url.indexOf("//") + 2).takeIf { it != -1 && it < pathEndIndex }
        ?: pathEndIndex
    val path = url.substring(pathStartIndex, pathEndIndex)
    var newPath = if (path.trimEnd('/').substringAfterLast('/') == "..") {
        path
    } else {
        deletingLastPathComponent(path)
    }
    if (!newPath.endsWith("/")) {
        newPath += "/"
    }
    return url.substring(0, pathStartIndex) + newPath + "100.gif" + url.substring(pathEndIndex)
}

private fun deletingLastPathComponent(path: String): String {
    val trimmedPath = path.trimEnd('/')
    if (trimmedPath.isEmpty()) {
        return if (path.isEmpty()) "" else "/"
    }
    val lastSlashIndex = trimmedPath.lastIndexOf('/')
    if (lastSlashIndex == -1) {
        return ""
    }
    return trimmedPath.substring(0, lastSlashIndex).trimEnd('/').ifEmpty { "/" }
}

private fun tagNameAndValue(from: String): Pair<String, String>? {
    val parts = from.split("=", limit = 2)
    if (parts.size != 2) {
        return null
    }
    val name = parts.first()
    val value = parts.last()
    if (value.isEmpty()) {
        return null
    }
    return Pair(name, unescapeTagValue(value))
}

private fun unescapeTagValue(value: String): String {
    if (!value.contains('\\')) {
        return value
    }
    val unescapedValue = StringBuilder(value.length)
    var index = 0
    while (index < value.length) {
        val character = value[index]
        index += 1
        if (character != '\\' || index >= value.length) {
            if (character != '\\') {
                unescapedValue.append(character)
            }
            continue
        }
        val escapedCharacter = value[index]
        index += 1
        when (escapedCharacter) {
            ':' -> unescapedValue.append(';')
            's' -> unescapedValue.append(' ')
            'r' -> unescapedValue.append('\r')
            'n' -> unescapedValue.append('\n')
            else -> unescapedValue.append(escapedCharacter)
        }
    }
    return unescapedValue.toString()
}

private fun parseParameters(from: List<String>): List<String> {
    val parameters = mutableListOf<String>()
    for (index in from.indices) {
        val part = from[index]
        if (!part.startsWith(":")) {
            parameters.add(part)
            continue
        }
        val finalPart = from.subList(index, from.size).joinToString(" ").removingPrefix(":")
        return parameters + listOf(finalPart)
    }
    return parameters
}

fun createTwitchSegments(
    text: String,
    emotes: List<ChatMessageEmote>,
    emotesManager: Emotes,
    id: AtomicInteger,
): List<ChatPostSegment> {
    val segments = mutableListOf<ChatPostSegment>()
    val unicodeText = text.codePoints().toArray()
    val unicodeTextCount = unicodeText.size
    var startIndex = 0
    var startOffset = 0
    for (emote in emotes.sortedBy { it.range.first }) {
        if (emote.range.last >= unicodeTextCount) {
            break
        }
        if (emote.range.first < startOffset) {
            continue
        }
        if (emote.range.first > startOffset) {
            val endIndex = startIndex + (emote.range.first - startOffset)
            segments += emotesManager.createSegments(
                text = String(unicodeText, startIndex, endIndex - startIndex),
                id = id,
            )
        }
        val emoteStartIndex = startIndex + (emote.range.first - startOffset)
        val emoteEndIndex = startIndex + (emote.range.last + 1 - startOffset)
        val emoteName = String(unicodeText, emoteStartIndex, emoteEndIndex - emoteStartIndex)
        if (emote.isGif) {
            segments.add(
                ChatPostSegment(
                    id = id.get(),
                    bigGifUrl = ChatPostEmote(
                        moving = emote.url,
                        still = null,
                        name = emoteName.trim('[', ']'),
                    ),
                )
            )
        } else {
            segments.add(
                ChatPostSegment(
                    id = id.get(),
                    url = ChatPostEmote(moving = emote.url, still = emote.stillUrl, name = emoteName),
                )
            )
        }
        id.incrementAndGet()
        segments.add(ChatPostSegment(id = id.get(), text = ""))
        id.incrementAndGet()
        startIndex = emoteEndIndex
        startOffset = emote.range.last + 1
    }
    if (startIndex < unicodeTextCount) {
        segments += emotesManager.createSegments(
            text = String(unicodeText, startIndex, unicodeTextCount - startIndex),
            id = id,
        )
    }
    return segments
}

fun createTwitchSegments(
    fragments: List<TwitchEventSubMessageFragment>,
    emotesManager: Emotes,
    id: AtomicInteger,
): List<ChatPostSegment> {
    val segments = mutableListOf<ChatPostSegment>()
    for (fragment in fragments) {
        val emote = fragment.emote
        val urls = if (fragment.type == "emote" && emote != null) {
            makeTwitchEmoteUrls(emote.id)
        } else {
            null
        }
        if (urls != null) {
            segments.add(
                ChatPostSegment(
                    id = id.get(),
                    url = ChatPostEmote(moving = urls.moving, still = urls.still, name = fragment.text),
                )
            )
            id.incrementAndGet()
            segments.add(ChatPostSegment(id = id.get(), text = ""))
            id.incrementAndGet()
        } else {
            segments += emotesManager.createSegments(text = fragment.text, id = id)
        }
    }
    return segments
}

class TwitchChatMessage(string: String) {
    val command: TwitchChatCommand
    val parameters: List<String>
    var displayName: String? = null
    var user: String? = null
    var userId: String? = null
    var color: String? = null
    var emotes: List<ChatMessageEmote> = emptyList()
    var badges: List<String> = emptyList()
    var messageId: String? = null
    var id: String? = null
    var firstMessage: Boolean = false
    var subscriber: Boolean = false
    var moderator: Boolean = false
    var bits: String? = null
    var replySender: String? = null
    var replyText: String? = null
    var targetMessageId: String? = null
    var targetUserId: String? = null
    var sourceRoomId: String? = null

    init {
        val parts = string.split(" ").toMutableList()
        val tagsPart = parts.firstOrNull()
        if (tagsPart != null && tagsPart.startsWith("@")) {
            val tagsString = tagsPart.substring(1)
            for (tag in tagsString.split(";")) {
                val nameAndValue = tagNameAndValue(tag) ?: continue
                val name = nameAndValue.first
                val value = nameAndValue.second
                when (name) {
                    "display-name" -> displayName = value
                    "user-id" -> userId = value
                    "login" -> user = value
                    "color" -> color = value
                    "emotes" -> emotes = emotes + parseEmotes(value)
                    "gifs" -> {
                        val gif = parseGif(value)
                        if (gif != null) {
                            emotes = emotes + gif
                        }
                    }
                    "badges" -> badges = value.split(",").filter { it.isNotEmpty() }
                    "msg-id" -> messageId = value
                    "id" -> id = value
                    "first-msg" -> firstMessage = value == "1"
                    "subscriber" -> subscriber = value == "1"
                    "mod" -> moderator = value == "1"
                    "bits" -> bits = value
                    "reply-parent-display-name" -> replySender = value
                    "reply-parent-msg-body" -> replyText = value
                    "target-msg-id" -> targetMessageId = value
                    "target-user-id" -> targetUserId = value
                    "source-room-id" -> sourceRoomId = value
                    else -> Unit
                }
            }
            parts.removeAt(0)
        }
        val sourcePart = parts.firstOrNull()
        if (sourcePart != null && sourcePart.startsWith(":")) {
            val source = sourcePart.removingPrefix(":")
            val senderEndIndex = source.indexOf('!')
            if (senderEndIndex != -1) {
                user = source.substring(0, senderEndIndex)
            }
            parts.removeAt(0)
        }
        val commandPart = parts.firstOrNull() ?: throw MessageError.MissingCommand(string)
        val commandValue = TwitchChatCommand.fromRawValue(commandPart)
            ?: throw MessageError.InvalidCommand(commandPart)
        command = commandValue
        parts.removeAt(0)
        parameters = parseParameters(parts)
    }

    val isGigantifiedEmote: Boolean
        get() = messageId == "gigantified-emote-message"
}

private class Badges {
    private var channelId: String = ""
    private var accessToken: String = ""
    private val badges: MutableMap<String, String> = mutableMapOf()
    private val tryFetchAgainTimer = MainTimer()

    fun start(channelId: String, accessToken: String) {
        this.channelId = channelId
        this.accessToken = accessToken
        if (accessToken.isEmpty()) {
            return
        }
        tryFetch()
    }

    fun stop() {
        stopTryFetchAgainTimer()
    }

    fun getUrl(badgeId: String): String? {
        return badges[badgeId]
    }

    fun tryFetch() {
        startTryFetchAgainTimer()
        TwitchApi(accessToken).getGlobalChatBadges { data ->
            if (data == null) {
                return@getGlobalChatBadges
            }
            addBadges(data)
            TwitchApi(accessToken).getChannelChatBadges(channelId) { channelData ->
                if (channelData == null) {
                    return@getChannelChatBadges
                }
                addBadges(channelData)
                stopTryFetchAgainTimer()
            }
        }
    }

    private fun startTryFetchAgainTimer() {
        tryFetchAgainTimer.startSingleShot(30.0) { tryFetch() }
    }

    private fun stopTryFetchAgainTimer() {
        tryFetchAgainTimer.stop()
    }

    private fun addBadges(badges: List<TwitchApiChatBadgesData>) {
        for (badge in badges) {
            for (version in badge.versions) {
                this.badges["${badge.set_id}/${version.id}"] = version.image_url_2x
            }
        }
    }
}

class Cheermotes {
    private var channelId: String = ""
    private var accessToken: String = ""
    private val emotes: MutableMap<String, List<TwitchApiGetCheermotesDataTier>> = mutableMapOf()
    private val tryFetchAgainTimer = MainTimer()

    fun start(channelId: String, accessToken: String) {
        this.channelId = channelId
        this.accessToken = accessToken
        if (accessToken.isEmpty()) {
            return
        }
        tryFetch()
    }

    fun stop() {
        stopTryFetchAgainTimer()
    }

    fun tryFetch() {
        startTryFetchAgainTimer()
        TwitchApi(accessToken).getCheermotes(channelId) { datas ->
            if (datas == null) {
                return@getCheermotes
            }
            addCheermotes(datas)
            stopTryFetchAgainTimer()
        }
    }

    private fun startTryFetchAgainTimer() {
        tryFetchAgainTimer.startSingleShot(30.0) { tryFetch() }
    }

    private fun stopTryFetchAgainTimer() {
        tryFetchAgainTimer.stop()
    }

    fun addCheermotes(datas: List<TwitchApiGetCheermotesData>) {
        for (data in datas) {
            emotes[data.prefix.lowercase()] = data.tiers
        }
    }

    fun getUrlAndBits(word: String): Pair<String, Int>? {
        val text = word.lowercase().trim()
        var prefixEndIndex = text.length
        while (prefixEndIndex > 0) {
            val index = prefixEndIndex - 1
            if (!text[index].isDigit()) {
                return null
            }
            prefixEndIndex = index
            val bits = text.substring(prefixEndIndex).toIntOrNull()
            val tiers = emotes[text.substring(0, prefixEndIndex)]
            val tier = if (bits != null) {
                tiers?.reversed()?.firstOrNull { bits >= it.min_bits }
            } else {
                null
            }
            if (bits == null || tier == null) {
                continue
            }
            return Pair(tier.images.dark.static_.two, bits)
        }
        return null
    }
}

interface TwitchChatDelegate {
    fun twitchChatMakeErrorToast(title: String, subTitle: String?)

    fun twitchChatAppendMessage(
        messageId: String?,
        displayName: String,
        user: String,
        userId: String?,
        userColor: RgbColor?,
        userBadges: List<String>,
        segments: List<ChatPostSegment>,
        isAction: Boolean,
        isSubscriber: Boolean,
        isModerator: Boolean,
        bits: String?,
        highlight: ChatHighlight?,
        sourceChannelIcon: String?,
    )

    fun twitchChatDeleteMessage(messageId: String)

    fun twitchChatDeleteUser(userId: String)
}

class TwitchChat(private val delegate: TwitchChatDelegate?) : WebSocketClientDelegate {
    private var webSocket: WebSocketClient = WebSocketClient(
        context = AppDelegate.context,
        url = "wss://irc-ws.chat.twitch.tv",
    )
    private val emotes: Emotes = Emotes()
    private val badges: Badges = Badges()
    private val cheermotes: Cheermotes = Cheermotes()
    private var channelName: String = ""
    private val sourceRoomIcons: MutableMap<String, String?> = mutableMapOf()
    private var accessToken: String = ""

    fun start(
        channelName: String,
        channelId: String,
        settings: SettingsStreamChat,
        accessToken: String,
    ) {
        this.channelName = channelName
        this.accessToken = accessToken
        Log.d("TwitchChat", "twitch: chat: Start")
        stopInternal()
        emotes.start(
            platform = EmotesPlatform.twitch,
            channelId = channelId,
            onError = ::handleError,
            onOk = ::handleOk,
            settings = settings,
        )
        badges.start(channelId, accessToken)
        cheermotes.start(channelId, accessToken)
        webSocket = WebSocketClient(
            context = AppDelegate.context,
            url = "wss://irc-ws.chat.twitch.tv",
        )
        webSocket.delegate = this
        webSocket.start()
    }

    fun stop() {
        Log.d("TwitchChat", "twitch: chat: Stop")
        stopInternal()
    }

    fun stopInternal() {
        emotes.stop()
        badges.stop()
        cheermotes.stop()
        webSocket.stop()
    }

    fun createSegmentsNoTwitchEmotes(text: String, bits: String?): List<ChatPostSegment> {
        return createSegments(text = text, fragments = emptyList(), bits = bits)
    }

    fun createSegments(
        text: String,
        fragments: List<TwitchEventSubMessageFragment>,
        bits: String?,
    ): List<ChatPostSegment> {
        val id = AtomicInteger(0)
        var segments = createTwitchSegments(text = text, emotes = emptyList(), emotesManager = emotes, id = id)
        segments = segments + createTwitchSegments(fragments = fragments, emotesManager = emotes, id = id)
        if (bits != null) {
            segments = replaceCheermotes(segments)
        }
        return segments
    }

    fun isConnected(): Boolean {
        return webSocket.isConnected()
    }

    fun hasEmotes(): Boolean {
        return emotes.isReady()
    }

    fun getBadgeUrl(badgeId: String): String? {
        return badges.getUrl(badgeId)
    }

    private fun handleMessage(message: String) {
        val chatMessage = TwitchChatMessage(message)
        when (chatMessage.command) {
            TwitchChatCommand.privateMessage, TwitchChatCommand.userNotice -> handleChatMessage(chatMessage)
            TwitchChatCommand.clearMsg -> handleClearMessage(chatMessage)
            TwitchChatCommand.clearChat -> handleClearChat(chatMessage)
            TwitchChatCommand.ping -> handlePing(chatMessage)
        }
    }

    fun getSourceChannelIcon(sourceRoomId: String, onComplete: (String?) -> Unit) {
        if (accessToken.isEmpty()) {
            onComplete(null)
            return
        }
        if (sourceRoomIcons.containsKey(sourceRoomId)) {
            onComplete(sourceRoomIcons[sourceRoomId])
        } else {
            TwitchApi(accessToken).getUserById(sourceRoomId) { user ->
                val sourceRoomIcon = user?.profile_image_url
                sourceRoomIcons[sourceRoomId] = sourceRoomIcon
                onComplete(sourceRoomIcon)
            }
        }
    }

    private fun handleChatMessage(message: TwitchChatMessage) {
        val sourceRoomId = message.sourceRoomId
        if (sourceRoomId != null) {
            getSourceChannelIcon(sourceRoomId) { sourceRoomIcon ->
                processChatMessage(message, sourceRoomIcon)
            }
        } else {
            processChatMessage(message, null)
        }
    }

    private fun processChatMessage(message: TwitchChatMessage, sourceChannelIcon: String?) {
        if (message.parameters.size != 2) {
            return
        }
        var text = message.parameters.lastOrNull() ?: return
        val displayName = message.displayName ?: return
        val user = message.user ?: return
        var announcement = false
        var firstMessage = false
        var gigantifiedEmote = false
        var subscriber = false
        var moderator = false
        when (message.command) {
            TwitchChatCommand.privateMessage -> {
                firstMessage = message.firstMessage
                gigantifiedEmote = message.isGigantifiedEmote
                subscriber = message.subscriber
                moderator = message.moderator
            }
            TwitchChatCommand.userNotice -> {
                if (message.messageId != "announcement") {
                    return
                }
                announcement = true
            }
            else -> return
        }
        val badgeUrls = mutableListOf<String>()
        for (badge in message.badges) {
            val badgeUrl = badges.getUrl(badge)
            if (badgeUrl != null) {
                badgeUrls.add(badgeUrl)
            }
        }
        val isAction = text.startsWith("\u0001ACTION ")
        if (isAction) {
            text = text.drop(8)
            if (text.endsWith("\u0001")) {
                text = text.dropLast(1)
            }
        }
        val segments = createSegments(
            text = text,
            emotes = message.emotes,
            emotesManager = emotes,
            bits = message.bits,
        )
        val highlight = createHighlight(
            announcement = announcement,
            firstMessage = firstMessage,
            gigantifiedEmote = gigantifiedEmote,
            replySender = message.replySender,
            replyText = message.replyText,
        )
        delegate?.twitchChatAppendMessage(
            messageId = message.id,
            displayName = displayName,
            user = user,
            userId = message.userId,
            userColor = RgbColor.fromHex(message.color ?: ""),
            userBadges = badgeUrls,
            segments = segments,
            isAction = isAction,
            isSubscriber = subscriber,
            isModerator = moderator,
            bits = message.bits,
            highlight = highlight,
            sourceChannelIcon = sourceChannelIcon,
        )
    }

    private fun handleClearMessage(message: TwitchChatMessage) {
        val targetMessageId = message.targetMessageId ?: return
        delegate?.twitchChatDeleteMessage(messageId = targetMessageId)
    }

    private fun handleClearChat(message: TwitchChatMessage) {
        val targetUserId = message.targetUserId ?: return
        delegate?.twitchChatDeleteUser(userId = targetUserId)
    }

    private fun handlePing(message: TwitchChatMessage) {
        webSocket.send("PONG ${message.parameters.joinToString(" ")}")
    }

    private fun createHighlight(
        announcement: Boolean,
        firstMessage: Boolean,
        gigantifiedEmote: Boolean,
        replySender: String?,
        replyText: String?,
    ): ChatHighlight? {
        return if (announcement) {
            ChatHighlight.makeAnnouncement()
        } else if (firstMessage) {
            ChatHighlight.makeFirstMessage()
        } else if (gigantifiedEmote) {
            ChatHighlight.makeGigantifiedEmote()
        } else if (replySender != null && replyText != null) {
            ChatHighlight.makeReply(
                user = replySender,
                segments = createSegmentsNoTwitchEmotes(replyText, null),
            )
        } else {
            null
        }
    }

    private fun handleError(title: String, subTitle: String) {
        delegate?.twitchChatMakeErrorToast(title = title, subTitle = subTitle)
    }

    private fun handleOk(title: String) {
        delegate?.twitchChatMakeErrorToast(title = title, subTitle = null)
    }

    private fun createSegments(
        text: String,
        emotes: List<ChatMessageEmote>,
        emotesManager: Emotes,
        bits: String?,
    ): List<ChatPostSegment> {
        val id = AtomicInteger(0)
        var segments = createTwitchSegments(text = text, emotes = emotes, emotesManager = emotesManager, id = id)
        if (bits != null) {
            segments = replaceCheermotes(segments)
        }
        return segments
    }

    private fun replaceCheermotes(segments: List<ChatPostSegment>): List<ChatPostSegment> {
        val newSegments = mutableListOf<ChatPostSegment>()
        var id = segments.lastOrNull()?.id ?: return newSegments
        for (segment in segments) {
            val text = segment.text
            if (text == null) {
                newSegments.add(segment)
                continue
            }
            val urlAndBits = cheermotes.getUrlAndBits(text)
            if (urlAndBits == null) {
                newSegments.add(segment)
                continue
            }
            id += 1
            newSegments.add(ChatPostSegment(id = id, url = ChatPostEmote(moving = null, still = urlAndBits.first)))
            id += 1
            newSegments.add(ChatPostSegment(id = id, text = "${urlAndBits.second} "))
        }
        return newSegments
    }

    override fun webSocketClientConnected(client: WebSocketClient) {
        Log.d("TwitchChat", "twitch: chat: Connected")
        webSocket.send("CAP REQ :twitch.tv/membership")
        webSocket.send("CAP REQ :twitch.tv/tags")
        webSocket.send("CAP REQ :twitch.tv/commands")
        webSocket.send("PASS oauth:SCHMOOPIIE")
        webSocket.send("NICK justinfan67420")
        webSocket.send("JOIN #${channelName.lowercase()}")
    }

    override fun webSocketClientDisconnected(client: WebSocketClient) {
        Log.d("TwitchChat", "twitch: chat: Disconnected")
    }

    override fun webSocketClientReceiveMessage(client: WebSocketClient, string: String) {
        for (line in string.lines()) {
            runCatching { handleMessage(line) }
        }
    }
}

package com.moblin.android.streamingplatforms.youtube

import kotlinx.coroutines.delay
import com.moblin.android.integrations.emotes.Emotes
import com.moblin.android.integrations.emotes.EmotesPlatform
import com.moblin.android.localized
import com.moblin.android.various.ChatHighlight
import com.moblin.android.various.ChatPostSegment
import com.moblin.android.various.ChatPostEmote
import com.moblin.android.streamingplatforms.Platform
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsStreamChat
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import com.moblin.android.various.model.appendChatMessage
import com.moblin.android.various.network.httpUrlSession
import com.moblin.android.various.network.httpGet
import com.moblin.android.common.various.isSuccessful

private const val userAgent =
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:124.0) Gecko/20100101 Firefox/124.0"

suspend fun fetchYouTubeVideoId(handle: String): String {
    val url = "https://www.youtube.com/$handle/live"
    val request = Request.Builder()
        .url(url)
        .header("User-Agent", userAgent)
        .header("Cookie", "CONSENT=YES+1")
        .build()
    val (data, response) = httpGet(request)
    if (!response.isSuccessful) {
        throw IllegalStateException("Not successful")
    }
    val html = data.decodeToString()
    val patterns = listOf(
        Regex("<link rel=\"shortlinkUrl\" href=\"https://youtu\\.be/([^\"]+)\""),
        Regex("<link rel='shortlinkUrl' href='https://youtu\\.be/([^']+)'"),
        Regex("shortlinkUrl[^>]*href=[\"']https://youtu\\.be/([^\"']+)"),
    )
    for (pattern in patterns) {
        val match = pattern.find(html)
        if (match != null) {
            return match.groupValues[1]
        }
    }
    throw IllegalStateException("Video id not found")
}

private const val minimumPollDelayMs = 200L
private const val maximumPollDelayMs = 3000L

private val json = Json { ignoreUnknownKeys = true }

private fun createPaidMessageText(chatDescription: ChatDescription): String {
    val amount = chatDescription.purchaseAmountText?.simpleText
    return if (amount != null) {
        localized("sent a $amount Super Chat!")
    } else {
        localized("sent a Super Chat!")
    }
}

private fun createPaidStickerText(chatDescription: ChatDescription): String {
    val amount = chatDescription.purchaseAmountText?.simpleText
    return if (amount != null) {
        localized("sent a $amount Super Sticker!")
    } else {
        localized("sent a Super Sticker!")
    }
}

@Serializable
private data class InvalidationContinuationData(
    val continuation: String,
)

@Serializable
private data class Continuations(
    val invalidationContinuationData: InvalidationContinuationData,
)

@Serializable
private data class Thumbnail(
    val url: String,
)

@Serializable
private data class Image(
    val thumbnails: List<Thumbnail>,
)

@Serializable
private data class Emoji(
    val image: Image,
    val shortcuts: List<String>? = null,
) {
    fun name(): String? {
        val shortcut = shortcuts?.firstOrNull() ?: return null
        return shortcut.trim(':')
    }
}

@Serializable
private data class Run(
    val text: String? = null,
    val emoji: Emoji? = null,
)

@Serializable
private data class Message(
    val runs: List<Run>,
)

@Serializable
private data class Author(
    val simpleText: String,
)

@Serializable
private data class Amount(
    val simpleText: String,
)

@Serializable
private data class BadgeIcon(
    val iconType: String? = null,
)

@Serializable
private data class AuthorBadgeRenderer(
    val icon: BadgeIcon? = null,
)

@Serializable
private data class AuthorBadge(
    val liveChatAuthorBadgeRenderer: AuthorBadgeRenderer? = null,
)

@Serializable
private data class ChatDescription(
    val authorName: Author,
    val authorExternalChannelId: String? = null,
    val message: Message? = null,
    val purchaseAmountText: Amount? = null,
    val headerSubtext: Message? = null,
    val authorBadges: List<AuthorBadge>? = null,
)

@Serializable
private data class SponsorshipsHeaderRenderer(
    val authorName: Author,
    val authorExternalChannelId: String? = null,
    val primaryText: Message? = null,
    val authorBadges: List<AuthorBadge>? = null,
)

private fun getUserRoles(authorBadges: List<AuthorBadge>?): Pair<Boolean, Boolean> {
    var isOwner = false
    var isModerator = false
    if (authorBadges != null) {
        for (authorBadge in authorBadges) {
            val iconType = authorBadge.liveChatAuthorBadgeRenderer?.icon?.iconType
            when (iconType) {
                "OWNER" -> isOwner = true
                "MODERATOR" -> isModerator = true
                else -> {}
            }
        }
    }
    return Pair(isOwner, isModerator)
}

@Serializable
private data class SponsorshipsHeader(
    val liveChatSponsorshipsHeaderRenderer: SponsorshipsHeaderRenderer? = null,
)

@Serializable
private data class GiftPurchaseAnnouncementDescription(
    val header: SponsorshipsHeader? = null,
)

@Serializable
private data class Content(
    val content: String,
)

@Serializable
private data class GiftMessageVieModel(
    val authorName: Content,
    val text: Content,
)

@Serializable
private data class AddChatItemActionItem(
    val liveChatTextMessageRenderer: ChatDescription? = null,
    val liveChatPaidMessageRenderer: ChatDescription? = null,
    val liveChatPaidStickerRenderer: ChatDescription? = null,
    val liveChatMembershipItemRenderer: ChatDescription? = null,
    val liveChatSponsorshipsGiftPurchaseAnnouncementRenderer: GiftPurchaseAnnouncementDescription? = null,
    val liveChatSponsorshipsGiftRedemptionAnnouncementRenderer: ChatDescription? = null,
    val giftMessageViewModel: GiftMessageVieModel? = null,
)

@Serializable
private data class AddChatItemAction(
    val item: AddChatItemActionItem,
)

@Serializable
private data class Action(
    val addChatItemAction: AddChatItemAction? = null,
)

@Serializable
private data class LiveChatContinuation(
    val continuations: List<Continuations>,
    val actions: List<Action>? = null,
)

@Serializable
private data class ContinuationContents(
    val liveChatContinuation: LiveChatContinuation,
)

@Serializable
private data class GetLiveChat(
    val continuationContents: ContinuationContents,
)

interface YouTubeLiveChatDelegate {
    fun youTubeLiveChatMakeErrorToast(title: String, subTitle: String)

    fun youTubeLiveChatMakeToast(title: String)

    fun youTubeLiveChatAppendMessage(
        user: String,
        userId: String?,
        segments: List<ChatPostSegment>,
        isModerator: Boolean,
        isOwner: Boolean,
        highlight: ChatHighlight?,
    )
}

class YouTubeLiveChat(delegate: YouTubeLiveChatDelegate, videoId: String, settings: SettingsStreamChat) {
    private val delegate: YouTubeLiveChatDelegate = delegate
    private val videoId: String = videoId
    private var task: Job? = null
    private val emotes: Emotes = Emotes()
    private val settings: SettingsStreamChat = settings.clone()
    private var connected: Boolean = false
    private var continuation: String = ""
    private var delay: Long = 2000
    private val scope = MainScope()

    fun start() {
        emotes.start(
            platform = EmotesPlatform.youtube,
            channelId = videoId,
            onError = { title, subTitle -> handleError(title, subTitle) },
            onOk = { title -> handleOk(title) },
            settings = settings,
        )
        task = scope.launch {
            while (true) {
                try {
                    getInitialContinuation()
                    connected = true
                    readMessages()
                } catch (error: Exception) {
                }
                connected = false
                if (!isActive) {
                    break
                }
                sleep(seconds = 5.0)
            }
        }
    }

    fun stop() {
        emotes.stop()
        task?.cancel()
        task = null
        connected = false
    }

    fun isConnected(): Boolean {
        return connected
    }

    fun hasEmotes(): Boolean {
        return emotes.isReady()
    }

    private fun handleError(title: String, subTitle: String) {
        delegate.youTubeLiveChatMakeErrorToast(title = title, subTitle = subTitle)
    }

    private fun handleOk(title: String) {
        delegate.youTubeLiveChatMakeToast(title = title)
    }

    private fun makeLiveChatUrl(): String {
        return "https://www.youtube.com/live_chat?is_popout=1&v=$videoId"
    }

    private fun makeGetLiveChatUrl(): String {
        return "https://www.youtube.com/youtubei/v1/live_chat/get_live_chat?prettyPrint=false"
    }

    private suspend fun getInitialContinuation() {
        val url = makeLiveChatUrl()
        val (data, response) = fetch(url)
        if (!response.isSuccessful) {
            throw IllegalStateException("Unsuccessful HTTP response")
        }
        val body = data.decodeToString()
        val reContinuation = Regex("\"continuation\":\"([^\"]+)\"")
        val match = reContinuation.find(body)
        if (match == null) {
            throw IllegalStateException("No continuation")
        }
        continuation = match.groupValues[1]
    }

    private suspend fun readMessages() {
        val url = makeGetLiveChatUrl()
        while (true) {
            val (data, response) = upload(url, makeGetLiveChatBody())
            if (!response.isSuccessful) {
                throw IllegalStateException("Unsuccessful HTTP response")
            }
            handleGetLiveChat(data)
            sleep(milliSeconds = delay)
        }
    }

    fun handleGetLiveChat(data: ByteArray) {
        var numberOfMessages = 0
        val getLiveChat = json.decodeFromString<GetLiveChat>(data.decodeToString())
        val actions = getLiveChat.continuationContents.liveChatContinuation.actions
        if (actions != null) {
            for (action in actions) {
                val item = action.addChatItemAction?.item
                if (item == null) {
                    continue
                }
                item.liveChatTextMessageRenderer?.let { chatDescription ->
                    numberOfMessages += handleChatDescription(
                        chatDescription = chatDescription,
                        highlight = null,
                    )
                }
                item.liveChatPaidMessageRenderer?.let { chatDescription ->
                    numberOfMessages += handleChatDescription(
                        chatDescription = chatDescription,
                        text = createPaidMessageText(chatDescription),
                        highlight = ChatHighlight.makePaidMessage(),
                    )
                }
                item.liveChatPaidStickerRenderer?.let { chatDescription ->
                    numberOfMessages += handleChatDescription(
                        chatDescription = chatDescription,
                        text = createPaidStickerText(chatDescription),
                        highlight = ChatHighlight.makePaidSticker(),
                    )
                }
                item.liveChatMembershipItemRenderer?.let { chatDescription ->
                    numberOfMessages += handleChatDescription(
                        chatDescription = chatDescription,
                        highlight = ChatHighlight.makeMember(),
                    )
                }
                item.liveChatSponsorshipsGiftPurchaseAnnouncementRenderer?.let { giftPurchase ->
                    val headerRenderer = giftPurchase.header?.liveChatSponsorshipsHeaderRenderer
                    if (headerRenderer != null) {
                        numberOfMessages += handleGiftPurchaseDescription(
                            headerRenderer = headerRenderer,
                        )
                    }
                }
                item.liveChatSponsorshipsGiftRedemptionAnnouncementRenderer?.let { chatDescription ->
                    numberOfMessages += handleChatDescription(
                        chatDescription = chatDescription,
                        highlight = ChatHighlight.makeGiftedMemberships(),
                    )
                }
                item.giftMessageViewModel?.let { giftMessageViewModel ->
                    numberOfMessages += handleGiftMessageViewModel(
                        giftMessageViewModel = giftMessageViewModel,
                    )
                }
            }
        }
        updateContinuation(getLiveChat)
        updateDelayMs(numberOfMessages)
    }

    private fun updateDelayMs(numberOfMessages: Int) {
        if (numberOfMessages > 0) {
            delay = delay * 5 / numberOfMessages
        } else {
            delay = maximumPollDelayMs
        }

        if (delay > maximumPollDelayMs) {
            delay = maximumPollDelayMs
        }

        if (delay < minimumPollDelayMs) {
            delay = minimumPollDelayMs
        }
    }

    private fun handleChatDescription(
        chatDescription: ChatDescription,
        text: String? = null,
        highlight: ChatHighlight?,
    ): Int {
        var id = 0
        val segments = mutableListOf<ChatPostSegment>()
        if (text != null) {
            val (createdSegments, nextId) = createSegments(text, id)
            segments += createdSegments
            id = nextId
        }
        val headerSubtext = chatDescription.headerSubtext
        if (headerSubtext != null) {
            for (run in headerSubtext.runs) {
                val runText = run.text
                if (runText != null) {
                    val (createdSegments, nextId) = createSegments(runText, id)
                    segments += createdSegments
                    id = nextId
                }
                val emojiUrl = run.emoji?.image?.thumbnails?.firstOrNull()?.url
                if (emojiUrl != null) {
                    segments.add(
                        ChatPostSegment(
                            id = id,
                            url = ChatPostEmote(
                                moving = emojiUrl,
                                still = emojiUrl,
                                name = run.emoji?.name(),
                            ),
                        ),
                    )
                    id += 1
                }
            }
        }
        val message = chatDescription.message
        if (message != null) {
            for (run in message.runs) {
                val runText = run.text
                if (runText != null) {
                    val (createdSegments, nextId) = createSegments(runText, id)
                    segments += createdSegments
                    id = nextId
                }
                val emojiUrl = run.emoji?.image?.thumbnails?.firstOrNull()?.url
                if (emojiUrl != null) {
                    segments.add(
                        ChatPostSegment(
                            id = id,
                            url = ChatPostEmote(
                                moving = emojiUrl,
                                still = emojiUrl,
                                name = run.emoji?.name(),
                            ),
                        ),
                    )
                    id += 1
                }
            }
        }
        if (segments.isEmpty() && highlight == null) {
            return 0
        }
        val (isOwner, isModerator) = getUserRoles(chatDescription.authorBadges)
        delegate.youTubeLiveChatAppendMessage(
            user = chatDescription.authorName.simpleText,
            userId = chatDescription.authorExternalChannelId,
            segments = segments,
            isModerator = isModerator,
            isOwner = isOwner,
            highlight = highlight,
        )
        return 1
    }

    private fun handleGiftPurchaseDescription(headerRenderer: SponsorshipsHeaderRenderer): Int {
        var id = 0
        val segments = mutableListOf<ChatPostSegment>()
        val primaryText = headerRenderer.primaryText
        if (primaryText != null) {
            for (run in primaryText.runs) {
                val text = run.text
                if (text != null) {
                    val (createdSegments, nextId) = createSegments(text, id)
                    segments += createdSegments
                    id = nextId
                }
            }
        }
        if (segments.isEmpty()) {
            return 0
        }
        val (isOwner, isModerator) = getUserRoles(headerRenderer.authorBadges)
        delegate.youTubeLiveChatAppendMessage(
            user = headerRenderer.authorName.simpleText,
            userId = headerRenderer.authorExternalChannelId,
            segments = segments,
            isModerator = isModerator,
            isOwner = isOwner,
            highlight = ChatHighlight.makeGiftedMemberships(),
        )
        return 1
    }

    private fun handleGiftMessageViewModel(giftMessageViewModel: GiftMessageVieModel): Int {
        var id = 0
        val (segments, _) = createSegments(giftMessageViewModel.text.content, id)
        delegate.youTubeLiveChatAppendMessage(
            user = giftMessageViewModel.authorName.content,
            userId = null,
            segments = segments,
            isModerator = false,
            isOwner = false,
            highlight = ChatHighlight.makeJewels(),
        )
        return 1
    }

    private fun updateContinuation(getLiveChat: GetLiveChat) {
        val continuation = getLiveChat.continuationContents.liveChatContinuation.continuations
            .firstOrNull()
        if (continuation == null) {
            throw IllegalStateException("Continuation missing")
        }
        this.continuation = continuation.invalidationContinuationData.continuation
    }

    private fun makeGetLiveChatBody(): ByteArray {
        return """
            {
                "context": {
                    "client": {
                        "clientName": "WEB",
                        "clientVersion": "2.20210128.02.00"
                    }
                },
                "continuation": "$continuation"
            }
            """.trimIndent().encodeToByteArray()
    }

    private fun createSegments(message: String, id: Int): Pair<List<ChatPostSegment>, Int> {
        val nextId = AtomicInteger(id)
        val segments = emotes.createSegments(text = message, id = nextId)
        return Pair(segments, nextId.get())
    }

    private suspend fun fetch(url: String): Pair<ByteArray, Response> {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .build()
            httpUrlSession().newCall(request).execute().use { response ->
                val data = response.body?.bytes() ?: ByteArray(0)
                data to response
            }
        }
    }

    private suspend fun upload(url: String, data: ByteArray): Pair<ByteArray, Response> {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url)
                .post(data.toRequestBody("application/json".toMediaType()))
                .header("User-Agent", userAgent)
                .build()
            httpUrlSession().newCall(request).execute().use { response ->
                val responseData = response.body?.bytes() ?: ByteArray(0)
                responseData to response
            }
        }
    }
}

private suspend fun sleep(milliSeconds: Long) {
    delay(milliSeconds)
}

private suspend fun sleep(seconds: Double) {
    delay((seconds * 1000.0).toLong())
}

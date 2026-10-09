package com.moblin.android.streamingplatforms.kick

import android.content.Context
import com.moblin.android.platform.log.Log
import com.moblin.android.common.various.RgbColor
import com.moblin.android.integrations.emotes.Emotes
import com.moblin.android.integrations.emotes.EmotesPlatform
import com.moblin.android.various.ChatHighlight
import com.moblin.android.various.ChatPostEmote
import com.moblin.android.various.ChatPostSegment
import com.moblin.android.various.network.WebSocketClient
import com.moblin.android.various.network.WebSocketClientDelegate
import com.moblin.android.various.settings.SettingsStreamChat
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

private const val TAG = "KickPusher"

@Serializable
private data class Badge(
    var type: String,
    var text: String? = null,
    var count: Int? = null,
)

private object BadgeType {
    const val verified = "verified"
    const val staff = "staff"
    const val moderator = "moderator"
    const val og = "og"
    const val vip = "vip"
    const val bot = "bot"
    const val broadcaster = "broadcaster"
    const val founder = "founder"
    const val subscriber = "subscriber"
    const val subGifter = "sub_gifter"
}

private const val badgesBaseUrl =
    "https://raw.githubusercontent.com/id3adeye/kickicons/refs/heads/main"

private val emoteRegex = Regex("""\[emote:(\d+):([^\]]+)\]""")

fun createKickSegments(
    message: String,
    emotesManager: Emotes,
    id: AtomicInteger,
): List<ChatPostSegment> {
    val segments = mutableListOf<ChatPostSegment>()
    var startIndex = 0
    for (match in emoteRegex.findAll(message)) {
        val emoteId = match.groupValues[1]
        val textBeforeEmote = message.substring(startIndex, match.range.first)
        val url = "https://files.kick.com/emotes/$emoteId/fullsize"
        segments.addAll(emotesManager.createSegments(textBeforeEmote, id))
        segments.add(
            ChatPostSegment(
                id = id.get(),
                url = ChatPostEmote(moving = url, still = url, name = match.groupValues[2]),
            ),
        )
        id.incrementAndGet()
        startIndex = match.range.last + 1
    }
    if (startIndex != message.length) {
        segments.addAll(emotesManager.createSegments(message.substring(startIndex), id))
    }
    return segments
}

private data class KickBadge(
    val months: Int,
    val url: String,
)

private class KickBadges {
    private var subscriberBadges: MutableList<KickBadge> = mutableListOf()
    private val staticBadges: Map<String, String> = mapOf(
        BadgeType.verified to "$badgesBaseUrl/kick-verified.png",
        BadgeType.staff to "$badgesBaseUrl/kick-staff.png",
        BadgeType.moderator to "$badgesBaseUrl/kick-moderator.png",
        BadgeType.og to "$badgesBaseUrl/kick-og.png",
        BadgeType.vip to "$badgesBaseUrl/kick-vip.png",
        BadgeType.bot to "$badgesBaseUrl/kick-bot.png",
        BadgeType.broadcaster to "$badgesBaseUrl/kick-broadcaster.png",
        BadgeType.founder to "$badgesBaseUrl/kick-founder.png",
        BadgeType.subGifter to "$badgesBaseUrl/kick-sub_gifter.png",
    )

    fun setBadges(badges: List<SubscriberBadge>) {
        subscriberBadges.clear()
        for (badge in badges.sortedBy { it.months }) {
            subscriberBadges.add(KickBadge(badge.months, badge.badge_image.src))
        }
    }

    fun getSubscriberBadgeUrl(months: Int): String? {
        return subscriberBadges.lastOrNull { months >= it.months }?.url
    }

    fun getStaticBadgeUrl(badgeType: String): String? {
        return staticBadges[badgeType]
    }
}

@Serializable
private data class BadgeV2(
    var image_url: String,
    var selected: Boolean,
)

@Serializable
private data class Identity(
    var color: String,
    var badges: List<Badge>,
    var badges_v2: List<BadgeV2>? = null,
)

@Serializable
private data class Sender(
    var id: Int? = null,
    var username: String,
    var identity: Identity,
)

@Serializable
private data class OriginalSender(
    var username: String,
)

@Serializable
private data class OriginalMessage(
    var content: String,
)

@Serializable
private data class Metadata(
    var original_sender: OriginalSender? = null,
    var original_message: OriginalMessage? = null,
)

@Serializable
private data class ChatMessageEvent(
    var id: String? = null,
    var type: String? = null,
    var content: String,
    var sender: Sender,
    var metadata: Metadata? = null,
) {
    fun isModerator(): Boolean {
        return sender.identity.badges.any { it.type == BadgeType.moderator }
    }

    fun isSubscriber(): Boolean {
        return sender.identity.badges.any { it.type == BadgeType.subscriber }
    }
}

@Serializable
private data class Message(
    var id: String,
)

@Serializable
private data class MessageDeletedEvent(
    var message: Message,
)

@Serializable
data class User(
    var id: Int,
    var slug: String,
    var username: String,
)

@Serializable
data class Moderator(
    var id: Int,
    var slug: String,
    var username: String,
)

@Serializable
data class KickPusherUserBannedEvent(
    var id: String,
    var user: User,
    var banned_by: Moderator,
    var permanent: Boolean,
)

@Serializable
data class KickPusherSubscriptionEvent(
    var username: String,
    var months: Int,
)

@Serializable
data class KickPusherGiftedSubscriptionsEvent(
    var gifted_usernames: List<String>,
    var gifter_username: String,
    var gifter_total: Int,
)

@Serializable
data class KickPusherRewardRedeemedEvent(
    var reward_title: String,
    var username: String,
    var user_input: String,
)

@Serializable
data class KickPusherStreamHostEvent(
    var host_username: String,
    var number_viewers: Int,
)

@Serializable
data class KickPusherKickSender(
    var id: Int,
    var username: String,
)

@Serializable
data class KickPusherKickGift(
    var name: String,
    var amount: Int,
)

@Serializable
data class KickPusherKicksGiftedEvent(
    var message: String,
    var sender: KickPusherKickSender,
    var gift: KickPusherKickGift,
)

private val json = Json { ignoreUnknownKeys = true }

private val url =
    "wss://ws-us2.pusher.com/app/34bf7a0ff419a2a775b9?protocol=7&client=js&version=7.6.0&flash=false"

private fun decodeEvent(message: String): Pair<String, String> {
    val jsonResult = json.parseToJsonElement(message).jsonObject
    val type = (jsonResult["event"] as? JsonPrimitive)?.contentOrNull
    val data = (jsonResult["data"] as? JsonPrimitive)?.contentOrNull
    if (type != null && data != null) {
        return type to data
    }
    throw IllegalStateException("Failed to get message event type")
}

private fun decodeChatMessageEvent(data: String): ChatMessageEvent {
    return json.decodeFromString(ChatMessageEvent.serializer(), data)
}

private fun decodeMessageDeletedEvent(data: String): MessageDeletedEvent {
    return json.decodeFromString(MessageDeletedEvent.serializer(), data)
}

private fun decodeUserBannedEvent(data: String): KickPusherUserBannedEvent {
    return json.decodeFromString(KickPusherUserBannedEvent.serializer(), data)
}

private fun decodeSubscriptionEvent(data: String): KickPusherSubscriptionEvent {
    return json.decodeFromString(KickPusherSubscriptionEvent.serializer(), data)
}

private fun decodeGiftedSubscriptionsEvent(data: String): KickPusherGiftedSubscriptionsEvent {
    return json.decodeFromString(KickPusherGiftedSubscriptionsEvent.serializer(), data)
}

private fun decodeRewardRedeemedEvent(data: String): KickPusherRewardRedeemedEvent {
    return json.decodeFromString(KickPusherRewardRedeemedEvent.serializer(), data)
}

private fun decodeStreamHostEvent(data: String): KickPusherStreamHostEvent {
    return json.decodeFromString(KickPusherStreamHostEvent.serializer(), data)
}

private fun decodeKicksGiftedEvent(data: String): KickPusherKicksGiftedEvent {
    return json.decodeFromString(KickPusherKicksGiftedEvent.serializer(), data)
}

interface KickPusherDelegate {
    fun kickPusherMakeErrorToast(title: String, subTitle: String?)
    fun kickPusherAppendMessage(
        messageId: String?,
        user: String,
        userId: String?,
        userColor: RgbColor?,
        userBadges: List<String>,
        segments: List<ChatPostSegment>,
        isSubscriber: Boolean,
        isModerator: Boolean,
        highlight: ChatHighlight?,
    )
    fun kickPusherDeleteMessage(messageId: String)
    fun kickPusherDeleteUser(userId: String)
    fun kickPusherSubscription(event: KickPusherSubscriptionEvent)
    fun kickPusherGiftedSubscription(event: KickPusherGiftedSubscriptionsEvent)
    fun kickPusherRewardRedeemed(event: KickPusherRewardRedeemedEvent)
    fun kickPusherStreamHost(event: KickPusherStreamHostEvent)
    fun kickPusherUserBanned(event: KickPusherUserBannedEvent)
    fun kickPusherKicksGifted(event: KickPusherKicksGiftedEvent)
}

class KickPusher(
    private val context: Context,
    delegate: KickPusherDelegate?,
    channelName: String,
    channelId: String,
    chatroomChannelId: String,
    settings: SettingsStreamChat,
) : WebSocketClientDelegate {
    private var channelName: String = channelName
    private var channelId: String = channelId
    private var chatroomChannelId: String = chatroomChannelId
    private var webSocket: WebSocketClient = WebSocketClient(context, url)
    private var emotes: Emotes = Emotes()
    private var badges: KickBadges = KickBadges()
    private val settings: SettingsStreamChat = settings.clone()
    private var gotInfo: Boolean = false
    private val delegate: KickPusherDelegate? = delegate

    fun start() {
        Log.d(TAG, "kick: Start")
        stopInternal()
        connect()
        fetchSubscriberBadges()
    }

    private fun connect() {
        emotes.stop()
        emotes.start(
            EmotesPlatform.kick,
            channelId,
            ::handleError,
            ::handleOk,
            settings,
        )
        webSocket = WebSocketClient(context, url)
        webSocket.delegate = this
        webSocket.start()
    }

    fun stop() {
        Log.d(TAG, "kick: Stop")
        stopInternal()
    }

    fun stopInternal() {
        emotes.stop()
        webSocket.stop()
        gotInfo = false
    }

    fun isConnected(): Boolean {
        return webSocket.isConnected()
    }

    fun hasEmotes(): Boolean {
        return emotes.isReady()
    }

    private fun fetchSubscriberBadges() {
        getKickChannelInfo(channelName) { channelInfo ->
            val subscriberBadges = channelInfo?.subscriber_badges
            if (subscriberBadges != null) {
                badges.setBadges(subscriberBadges)
            }
        }
    }

    private fun handleError(title: String, subTitle: String) {
        delegate?.kickPusherMakeErrorToast(title = title, subTitle = subTitle)
    }

    private fun handleOk(title: String) {
        delegate?.kickPusherMakeErrorToast(title = title, subTitle = null)
    }

    private fun handleMessage(message: String) {
        try {
            val (type, data) = decodeEvent(message)
            when (type) {
                "App\\Events\\ChatMessageEvent" -> handleChatMessageEvent(data)
                "App\\Events\\MessageDeletedEvent" -> handleMessageDeletedEvent(data)
                "App\\Events\\UserBannedEvent" -> handleUserBannedEvent(data)
                "App\\Events\\SubscriptionEvent" -> handleSubscriptionEvent(data)
                "GiftedSubscriptionsEvent" -> handleGiftedSubscriptionsEvent(data)
                "RewardRedeemedEvent" -> handleRewardRedeemedEvent(data)
                "App\\Events\\StreamHostEvent" -> handleStreamHostEvent(data)
                "KicksGifted" -> handleKicksGiftedEvent(data)
                else -> Log.d(TAG, "kick: pusher: $channelId: Unsupported type: $type")
            }
        } catch (e: Exception) {
            Log.i(
                TAG,
                "kick: pusher: $channelId: Failed to process message \"$message\" with error $e",
            )
        }
    }

    private fun handleChatMessageEvent(data: String) {
        val event = decodeChatMessageEvent(data)
        val badgeUrls = mutableListOf<String>()
        event.sender.identity.badges_v2?.let { badgesV2 ->
            for (badge in badgesV2) {
                if (badge.selected) {
                    badgeUrls.add(badge.image_url)
                }
            }
        }
        for (badge in event.sender.identity.badges) {
            val months = badge.count
            if (badge.type == BadgeType.subscriber && months != null) {
                badges.getSubscriberBadgeUrl(months)?.let { badgeUrls.add(it) }
            } else {
                badges.getStaticBadgeUrl(badge.type)?.let { badgeUrls.add(it) }
            }
        }
        delegate?.kickPusherAppendMessage(
            messageId = event.id,
            user = event.sender.username,
            userId = event.sender.id?.toString(),
            userColor = RgbColor.fromHex(event.sender.identity.color),
            userBadges = badgeUrls,
            segments = makeChatPostSegments(event.content),
            isSubscriber = event.isSubscriber(),
            isModerator = event.isModerator(),
            highlight = makeHighlight(event),
        )
    }

    private fun handleMessageDeletedEvent(data: String) {
        val event = decodeMessageDeletedEvent(data)
        delegate?.kickPusherDeleteMessage(messageId = event.message.id)
    }

    private fun handleUserBannedEvent(data: String) {
        val event = decodeUserBannedEvent(data)
        delegate?.kickPusherDeleteUser(userId = event.user.id.toString())
        delegate?.kickPusherUserBanned(event = event)
    }

    private fun handleSubscriptionEvent(data: String) {
        val event = decodeSubscriptionEvent(data)
        delegate?.kickPusherSubscription(event = event)
    }

    private fun handleGiftedSubscriptionsEvent(data: String) {
        val event = decodeGiftedSubscriptionsEvent(data)
        delegate?.kickPusherGiftedSubscription(event = event)
    }

    private fun handleRewardRedeemedEvent(data: String) {
        val event = decodeRewardRedeemedEvent(data)
        delegate?.kickPusherRewardRedeemed(event = event)
    }

    private fun handleStreamHostEvent(data: String) {
        val event = decodeStreamHostEvent(data)
        delegate?.kickPusherStreamHost(event = event)
    }

    private fun handleKicksGiftedEvent(data: String) {
        val event = decodeKicksGiftedEvent(data)
        delegate?.kickPusherKicksGifted(event = event)
    }

    fun makeChatPostSegments(content: String): List<ChatPostSegment> {
        val id = AtomicInteger(0)
        return createKickSegments(message = content, emotesManager = emotes, id = id)
    }

    private fun makeHighlight(message: ChatMessageEvent): ChatHighlight? {
        if (message.type == "reply") {
            val username = message.metadata?.original_sender?.username
            val content = message.metadata?.original_message?.content
            if (username != null && content != null) {
                return ChatHighlight.makeReply(
                    user = username,
                    segments = makeChatPostSegments(content),
                )
            }
        }
        return null
    }

    private fun sendMessage(message: String) {
        Log.d(TAG, "kick: pusher: $channelId: Sending $message")
        webSocket.send(message)
    }

    fun sendSubscribe(channel: String) {
        sendMessage(
            """{"event":"pusher:subscribe",
                "data":{"auth":"","channel":"$channel"}}""",
        )
    }

    override fun webSocketClientConnected(client: WebSocketClient) {
        Log.d(TAG, "kick: Connected")
        sendSubscribe(channel = "chatrooms.$channelId.v2")
        sendSubscribe(channel = "chatroom_$channelId")
        sendSubscribe(channel = "chatrooms.$channelId")
        sendSubscribe(channel = "predictions-channel-$channelId")
        sendSubscribe(channel = "channel_$chatroomChannelId")
    }

    override fun webSocketClientDisconnected(client: WebSocketClient) {
        Log.d(TAG, "kick: Disconnected")
    }

    override fun webSocketClientReceiveMessage(client: WebSocketClient, string: String) {
        handleMessage(message = string)
    }
}

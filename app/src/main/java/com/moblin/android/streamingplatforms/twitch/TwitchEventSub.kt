package com.moblin.android.streamingplatforms.twitch

import android.content.Context
import android.util.JsonReader
import android.util.JsonToken
import android.util.Log
import com.moblin.android.various.MainTimer
import com.moblin.android.various.network.NetworkResponse
import com.moblin.android.various.network.OperationResult
import com.moblin.android.various.network.WebSocketClient
import com.moblin.android.various.network.WebSocketClientDelegate
import java.io.StringReader
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonUnquotedLiteral
import kotlinx.serialization.json.decodeFromJsonElement

private val json = Json { ignoreUnknownKeys = true }

@OptIn(ExperimentalSerializationApi::class)
private fun readJsonElement(reader: JsonReader): JsonElement {
    return when (reader.peek()) {
        JsonToken.BEGIN_OBJECT -> {
            val content = mutableMapOf<String, JsonElement>()
            reader.beginObject()
            while (reader.hasNext()) {
                val name = reader.nextName()
                val value = readJsonElement(reader)
                if (name !in content) {
                    content[name] = value
                }
            }
            reader.endObject()
            JsonObject(content)
        }
        JsonToken.BEGIN_ARRAY -> {
            val content = mutableListOf<JsonElement>()
            reader.beginArray()
            while (reader.hasNext()) {
                content.add(readJsonElement(reader))
            }
            reader.endArray()
            JsonArray(content)
        }
        JsonToken.STRING -> JsonPrimitive(reader.nextString())
        JsonToken.NUMBER -> JsonUnquotedLiteral(reader.nextString())
        JsonToken.BOOLEAN -> JsonPrimitive(reader.nextBoolean())
        JsonToken.NULL -> {
            reader.nextNull()
            JsonNull
        }
        else -> throw SerializationException("Unexpected JSON token ${reader.peek()}")
    }
}

private inline fun <reified T> decodeJson(data: ByteArray): T {
    val reader = JsonReader(StringReader(data.decodeToString()))
    val element = readJsonElement(reader)
    if (reader.peek() != JsonToken.END_DOCUMENT) {
        throw SerializationException("Unexpected data after JSON document")
    }
    return json.decodeFromJsonElement(element)
}

private const val tag = "TwitchEventSub"

@Serializable
private data class BasicMetadata(
    var message_type: String,
    var subscription_type: String? = null,
)

@Serializable
private data class BasicMessage(
    var metadata: BasicMetadata,
)

@Serializable
private data class WelcomePayloadSession(
    var id: String,
)

@Serializable
private data class WelcomePayload(
    var session: WelcomePayloadSession,
)

@Serializable
private data class WelcomeMessage(
    var payload: WelcomePayload,
)

@Serializable
data class TwitchEventSubMessageFragmentEmote(
    var id: String,
)

@Serializable
data class TwitchEventSubMessageFragment(
    var type: String,
    var text: String,
    var emote: TwitchEventSubMessageFragmentEmote? = null,
)

@Serializable
data class TwitchEventSubMessage(
    var text: String,
    var fragments: List<TwitchEventSubMessageFragment>,
)

@Serializable
data class TwitchEventSubSharedChat(
    var broadcasterUserId: String,
    var broadcasterUserName: String,
)

@Serializable
data class TwitchEventSubBadge(
    var set_id: String,
    var id: String,
)

@Serializable
data class TwitchEventSubChatter(
    var color: String,
    var badges: List<TwitchEventSubBadge>,
)

data class TwitchEventSubNotificationChannelSubscribeEvent(
    var user_name: String,
    var tier: String,
    var is_gift: Boolean,
    var is_prime: Boolean? = null,
    var message: TwitchEventSubMessage? = null,
    var sharedChat: TwitchEventSubSharedChat? = null,
    var chatter: TwitchEventSubChatter? = null,
) {
    fun tierAsNumber(): Int {
        return twitchTierAsNumber(tier)
    }

    fun isPrime(): Boolean {
        return is_prime == true
    }
}

data class TwitchEventSubNotificationChannelSubscriptionUpgradeEvent(
    var user_name: String,
    var tier: String? = null,
    var message: TwitchEventSubMessage? = null,
    var sharedChat: TwitchEventSubSharedChat? = null,
    var chatter: TwitchEventSubChatter? = null,
) {
    fun tierAsNumber(): Int? {
        val tier = tier ?: return null
        return twitchTierAsNumber(tier)
    }
}

data class TwitchEventSubNotificationChannelWatchStreakEvent(
    var user_name: String,
    var streak_count: Int,
    var message: TwitchEventSubMessage,
    var sharedChat: TwitchEventSubSharedChat? = null,
    var chatter: TwitchEventSubChatter? = null,
)

data class TwitchEventSubNotificationChannelSubscriptionGiftEvent(
    var user_name: String? = null,
    var total: Int,
    var tier: String,
    var message: TwitchEventSubMessage? = null,
    var sharedChat: TwitchEventSubSharedChat? = null,
    var chatter: TwitchEventSubChatter? = null,
) {
    fun tierAsNumber(): Int {
        return twitchTierAsNumber(tier)
    }
}

data class TwitchEventSubNotificationChannelSubscriptionMessageEvent(
    var user_name: String,
    var cumulative_months: Int,
    var streak_months: Int? = null,
    var tier: String,
    var message: TwitchEventSubMessage,
    var sharedChat: TwitchEventSubSharedChat? = null,
    var chatter: TwitchEventSubChatter? = null,
) {
    fun tierAsNumber(): Int {
        return twitchTierAsNumber(tier)
    }
}

@Serializable
private data class NotificationChannelChatNotificationSub(
    var sub_tier: String,
    var is_prime: Boolean,
)

@Serializable
private data class NotificationChannelChatNotificationResub(
    var cumulative_months: Int,
    var streak_months: Int? = null,
    var sub_tier: String,
)

@Serializable
private data class NotificationChannelChatNotificationSubGift(
    var sub_tier: String,
    var community_gift_id: String? = null,
)

@Serializable
private data class NotificationChannelChatNotificationCommunitySubGift(
    var total: Int,
    var sub_tier: String,
)

@Serializable
private data class NotificationChannelChatNotificationPrimePaidUpgrade(
    var sub_tier: String,
)

@Serializable
private data class NotificationChannelChatNotificationWatchStreak(
    var streak_count: Int,
)

@Serializable
private data class NotificationChannelChatNotificationRaid(
    var user_id: String,
    var user_name: String,
    var viewer_count: Int,
)

@Serializable
private data class NotificationChannelChatNotificationEvent(
    var chatter_user_name: String? = null,
    var chatter_is_anonymous: Boolean,
    var color: String,
    var badges: List<TwitchEventSubBadge>,
    var source_broadcaster_user_id: String? = null,
    var source_broadcaster_user_name: String? = null,
    var message: TwitchEventSubMessage,
    var notice_type: String,
    var sub: NotificationChannelChatNotificationSub? = null,
    var resub: NotificationChannelChatNotificationResub? = null,
    var sub_gift: NotificationChannelChatNotificationSubGift? = null,
    var community_sub_gift: NotificationChannelChatNotificationCommunitySubGift? = null,
    var prime_paid_upgrade: NotificationChannelChatNotificationPrimePaidUpgrade? = null,
    var watch_streak: NotificationChannelChatNotificationWatchStreak? = null,
    var shared_chat_sub: NotificationChannelChatNotificationSub? = null,
    var shared_chat_resub: NotificationChannelChatNotificationResub? = null,
    var shared_chat_sub_gift: NotificationChannelChatNotificationSubGift? = null,
    var shared_chat_community_sub_gift: NotificationChannelChatNotificationCommunitySubGift? = null,
    var shared_chat_prime_paid_upgrade: NotificationChannelChatNotificationPrimePaidUpgrade? = null,
    var shared_chat_raid: NotificationChannelChatNotificationRaid? = null,
) {
    fun sharedChat(): TwitchEventSubSharedChat? {
        val broadcasterUserId = source_broadcaster_user_id ?: return null
        val broadcasterUserName = source_broadcaster_user_name ?: return null
        return TwitchEventSubSharedChat(broadcasterUserId, broadcasterUserName)
    }

    fun chatter(): TwitchEventSubChatter? {
        if (chatter_is_anonymous) {
            return null
        }
        return TwitchEventSubChatter(color, badges)
    }

    fun userName(): String {
        return chatter_user_name ?: ""
    }
}

@Serializable
private data class NotificationChannelChatNotificationPayload(
    var event: NotificationChannelChatNotificationEvent,
)

@Serializable
private data class NotificationChannelChatNotificationMessage(
    var payload: NotificationChannelChatNotificationPayload,
)

@Serializable
data class TwitchEventSubNotificationChannelFollowEvent(
    var user_name: String,
)

@Serializable
private data class NotificationChannelFollowPayload(
    var event: TwitchEventSubNotificationChannelFollowEvent,
)

@Serializable
private data class NotificationChannelFollowMessage(
    var payload: NotificationChannelFollowPayload,
)

@Serializable
data class TwitchEventSubNotificationChannelPointsCustomRewardRedemptionAddEventReward(
    var id: String,
    var title: String,
    var cost: Int,
    var prompt: String,
)

@Serializable
data class TwitchEventSubNotificationChannelPointsCustomRewardRedemptionAddEvent(
    var id: String,
    var user_id: String,
    var user_login: String,
    var user_name: String,
    var broadcaster_user_id: String,
    var broadcaster_user_login: String,
    var broadcaster_user_name: String,
    var status: String,
    var reward: TwitchEventSubNotificationChannelPointsCustomRewardRedemptionAddEventReward,
    var redeemed_at: String,
)

@Serializable
private data class NotificationChannelPointsCustomRewardRedemptionAddPayload(
    var event: TwitchEventSubNotificationChannelPointsCustomRewardRedemptionAddEvent,
)

@Serializable
private data class NotificationChannelPointsCustomRewardRedemptionAddMessage(
    var payload: NotificationChannelPointsCustomRewardRedemptionAddPayload,
)

@Serializable
data class TwitchEventSubChannelRaidEvent(
    var from_broadcaster_user_id: String,
    var from_broadcaster_user_name: String,
    var viewers: Int,
    var message: TwitchEventSubMessage? = null,
    var sharedChat: TwitchEventSubSharedChat? = null,
    var chatter: TwitchEventSubChatter? = null,
)

@Serializable
private data class NotificationChannelRaidPayload(
    var event: TwitchEventSubChannelRaidEvent,
)

@Serializable
private data class NotificationChannelRaidMessage(
    var payload: NotificationChannelRaidPayload,
)

@Serializable
data class TwitchEventSubChannelCheerEvent(
    var user_name: String? = null,
    var message: String,
    var bits: Int,
)

@Serializable
private data class NotificationChannelCheerPayload(
    var event: TwitchEventSubChannelCheerEvent,
)

@Serializable
private data class NotificationChannelCheerMessage(
    var payload: NotificationChannelCheerPayload,
)

@Serializable
data class TwitchEventSubChannelHypeTrainBeginEvent(
    var progress: Int,
    var goal: Int,
    var level: Int,
    var started_at: String,
    var expires_at: String,
)

@Serializable
private data class NotificationChannelHypeTrainBeginPayload(
    var event: TwitchEventSubChannelHypeTrainBeginEvent,
)

@Serializable
private data class NotificationChannelHypeTrainBeginMessage(
    var payload: NotificationChannelHypeTrainBeginPayload,
)

@Serializable
data class TwitchEventSubChannelHypeTrainProgressEvent(
    var progress: Int,
    var goal: Int,
    var level: Int,
    var started_at: String,
    var expires_at: String,
)

@Serializable
private data class NotificationChannelHypeTrainProgressPayload(
    var event: TwitchEventSubChannelHypeTrainProgressEvent,
)

@Serializable
private data class NotificationChannelHypeTrainProgressMessage(
    var payload: NotificationChannelHypeTrainProgressPayload,
)

@Serializable
data class TwitchEventSubChannelHypeTrainEndEvent(
    var level: Int,
    var started_at: String,
    var ended_at: String,
)

@Serializable
private data class NotificationChannelHypeTrainEndPayload(
    var event: TwitchEventSubChannelHypeTrainEndEvent,
)

@Serializable
private data class NotificationChannelHypeTrainEndMessage(
    var payload: NotificationChannelHypeTrainEndPayload,
)

@Serializable
data class TwitchEventSubChannelPollChoice(
    var id: String,
    var title: String,
    var votes: Int? = null,
)

@Serializable
data class TwitchEventSubChannelPollEvent(
    var id: String,
    var title: String,
    var choices: List<TwitchEventSubChannelPollChoice>,
    var ends_at: String? = null,
    var status: String? = null,
)

@Serializable
private data class NotificationChannelPollPayload(
    var event: TwitchEventSubChannelPollEvent,
)

@Serializable
private data class NotificationChannelPollMessage(
    var payload: NotificationChannelPollPayload,
)

@Serializable
data class TwitchEventSubChannelPredictionOutcome(
    var id: String,
    var title: String,
    var color: String,
    var users: Int? = null,
    var channel_points: Int? = null,
)

@Serializable
data class TwitchEventSubChannelPredictionEvent(
    var id: String,
    var title: String,
    var outcomes: List<TwitchEventSubChannelPredictionOutcome>,
    var locks_at: String? = null,
    var winning_outcome_id: String? = null,
    var status: String? = null,
)

@Serializable
private data class NotificationChannelPredictionPayload(
    var event: TwitchEventSubChannelPredictionEvent,
)

@Serializable
private data class NotificationChannelPredictionMessage(
    var payload: NotificationChannelPredictionPayload,
)

@Serializable
data class TwitchEventSubChannelAdBreakBeginEvent(
    var duration_seconds: Int,
    var is_automatic: Boolean,
)

@Serializable
private data class NotificationChannelAdBreakBeginPayload(
    var event: TwitchEventSubChannelAdBreakBeginEvent,
)

@Serializable
private data class NotificationChannelAdBreakBeginMessage(
    var payload: NotificationChannelAdBreakBeginPayload,
)

@Serializable
data class TwitchEventSubChannelModerateRaid(
    var user_login: String,
    var user_name: String,
)

@Serializable
data class TwitchEventSubChannelModerateEvent(
    var action: String,
    var raid: TwitchEventSubChannelModerateRaid? = null,
)

@Serializable
private data class NotificationChannelModeratePayload(
    var event: TwitchEventSubChannelModerateEvent,
)

@Serializable
private data class NotificationChannelModerateMessage(
    var payload: NotificationChannelModeratePayload,
)

@Serializable
data class TwitchEventSubChannelShoutoutCreateEvent(
    var moderator_user_name: String,
    var to_broadcaster_user_name: String,
)

@Serializable
private data class NotificationChannelShoutoutCreatePayload(
    var event: TwitchEventSubChannelShoutoutCreateEvent,
)

@Serializable
private data class NotificationChannelShoutoutCreateMessage(
    var payload: NotificationChannelShoutoutCreatePayload,
)

private val url = "wss://eventsub.wss.twitch.tv/ws"

interface TwitchEventSubDelegate {
    fun twitchEventSubChannelFollow(event: TwitchEventSubNotificationChannelFollowEvent)
    fun twitchEventSubChannelSubscribe(event: TwitchEventSubNotificationChannelSubscribeEvent)
    fun twitchEventSubChannelSubscriptionGift(event: TwitchEventSubNotificationChannelSubscriptionGiftEvent)
    fun twitchEventSubChannelSubscriptionMessage(
        event: TwitchEventSubNotificationChannelSubscriptionMessageEvent,
    )
    fun twitchEventSubChannelSubscriptionUpgrade(
        event: TwitchEventSubNotificationChannelSubscriptionUpgradeEvent,
    )
    fun twitchEventSubChannelWatchStreak(event: TwitchEventSubNotificationChannelWatchStreakEvent)
    fun twitchEventSubChannelPointsCustomRewardRedemptionAdd(
        event: TwitchEventSubNotificationChannelPointsCustomRewardRedemptionAddEvent,
    )
    fun twitchEventSubChannelRaid(event: TwitchEventSubChannelRaidEvent)
    fun twitchEventSubChannelCheer(event: TwitchEventSubChannelCheerEvent)
    fun twitchEventSubChannelHypeTrainBegin(event: TwitchEventSubChannelHypeTrainBeginEvent)
    fun twitchEventSubChannelHypeTrainProgress(event: TwitchEventSubChannelHypeTrainProgressEvent)
    fun twitchEventSubChannelHypeTrainEnd(event: TwitchEventSubChannelHypeTrainEndEvent)
    fun twitchEventSubChannelAdBreakBegin(event: TwitchEventSubChannelAdBreakBeginEvent)
    fun twitchEventSubChannelPollBegin(event: TwitchEventSubChannelPollEvent)
    fun twitchEventSubChannelPollProgress(event: TwitchEventSubChannelPollEvent)
    fun twitchEventSubChannelPollEnd(event: TwitchEventSubChannelPollEvent)
    fun twitchEventSubChannelPredictionBegin(event: TwitchEventSubChannelPredictionEvent)
    fun twitchEventSubChannelPredictionProgress(event: TwitchEventSubChannelPredictionEvent)
    fun twitchEventSubChannelPredictionLock(event: TwitchEventSubChannelPredictionEvent)
    fun twitchEventSubChannelPredictionEnd(event: TwitchEventSubChannelPredictionEvent)
    fun twitchEventSubChannelModerate(event: TwitchEventSubChannelModerateEvent)
    fun twitchEventSubChannelShoutoutCreate(event: TwitchEventSubChannelShoutoutCreateEvent)
    fun twitchEventSubUnauthorized()
    fun twitchEventSubNotification(message: String)
}

private val subTypeChannelFollow = "channel.follow"
private val subTypeChannelChatNotification = "channel.chat.notification"
private val subTypeChannelChannelPointsCustomRewardRedemptionAdd =
    "channel.channel_points_custom_reward_redemption.add"
private val subTypeChannelRaid = "channel.raid"
private val subTypeChannelCheer = "channel.cheer"
private val subTypeChannelHypeTrainBegin = "channel.hype_train.begin"
private val subTypeChannelHypeTrainProgress = "channel.hype_train.progress"
private val subTypeChannelHypeTrainEnd = "channel.hype_train.end"
private val subTypeChannelAdBreakBegin = "channel.ad_break.begin"
private val subTypeChannelModerate = "channel.moderate"
private val subTypeChannelPollBegin = "channel.poll.begin"
private val subTypeChannelPollProgress = "channel.poll.progress"
private val subTypeChannelPollEnd = "channel.poll.end"
private val subTypeChannelPredictionBegin = "channel.prediction.begin"
private val subTypeChannelPredictionProgress = "channel.prediction.progress"
private val subTypeChannelPredictionLock = "channel.prediction.lock"
private val subTypeChannelPredictionEnd = "channel.prediction.end"
private val subTypeChannelShoutoutCreate = "channel.shoutout.create"

private const val initialReconnectDelay = 5.0

private data class Subscription(
    val type: String,
    val version: Int,
    val condition: String,
)

class TwitchEventSub(
    context: Context,
    remoteControl: Boolean,
    userId: String,
    accessToken: String,
    delegate: TwitchEventSubDelegate,
) : WebSocketClientDelegate {
    private var webSocket: WebSocketClient
    private val context: Context
    private var remoteControl: Boolean
    private val userId: String
    private var sessionId: String = ""
    private var remainingSubscriptions = 0
    private var reconnectDelay = initialReconnectDelay
    private var twitchApi: TwitchApi
    private val delegate: TwitchEventSubDelegate
    private var connected = false
    private var started = false
    private val connectDelayTimer = MainTimer()

    init {
        this.context = context
        this.remoteControl = remoteControl
        this.userId = userId
        this.delegate = delegate
        twitchApi = TwitchApi(accessToken)
        webSocket = WebSocketClient(context, url)
        twitchApi.onUnauthorized = {
            if (started) {
                this.delegate.twitchEventSubUnauthorized()
            }
        }
    }

    fun start() {
        Log.d(tag, "twitch: event-sub: Start")
        stopInternal()
        reconnectDelay = initialReconnectDelay
        connectDelayTimer.startSingleShot(2.0) {
            if (started) {
                connect()
            }
        }
        started = true
    }

    fun stop() {
        Log.d(tag, "twitch: event-sub: Stop")
        webSocket.delegate = null
        started = false
        stopInternal()
    }

    fun stopInternal() {
        connected = false
        sessionId = ""
        webSocket.stop()
        connectDelayTimer.stop()
    }

    fun isConnected(): Boolean {
        return connected
    }

    fun handleMessage(messageText: String) {
        val messageData = messageText.toByteArray()
        val message = runCatching {
            decodeJson<BasicMessage>(messageData)
        }.getOrNull() ?: return
        when (message.metadata.message_type) {
            "session_welcome" -> handleSessionWelcome(messageData)
            "session_keepalive" -> Unit
            "notification" -> handleNotification(message, messageText, messageData)
            else -> Log.i(
                tag,
                "twitch: event-sub: Unknown message type ${message.metadata.message_type}",
            )
        }
    }

    private fun connect() {
        connected = false
        webSocket = WebSocketClient(context, url)
        webSocket.delegate = this
        if (!remoteControl) {
            webSocket.start()
        }
    }

    private fun handleSessionWelcome(messageData: ByteArray) {
        val message = runCatching {
            decodeJson<WelcomeMessage>(messageData)
        }.getOrNull()
        if (message == null) {
            Log.i(tag, "twitch: event-sub: Failed to decode welcome message")
            return
        }
        sessionId = message.payload.session.id
        val subscriptions = makeSubscriptions()
        remainingSubscriptions = subscriptions.size
        subscribe(subscriptions)
    }

    private fun makeSubscriptions(): List<Subscription> {
        val broadcaster = "{\"broadcaster_user_id\":\"$userId\"}"
        val broadcasterAndModerator =
            "{\"broadcaster_user_id\":\"$userId\",\"moderator_user_id\":\"$userId\"}"
        return listOf(
            Subscription(subTypeChannelFollow, 2, broadcasterAndModerator),
            Subscription(
                subTypeChannelChatNotification,
                1,
                "{\"broadcaster_user_id\":\"$userId\",\"user_id\":\"$userId\"}",
            ),
            Subscription(subTypeChannelChannelPointsCustomRewardRedemptionAdd, 1, broadcaster),
            Subscription(subTypeChannelRaid, 1, "{\"to_broadcaster_user_id\":\"$userId\"}"),
            Subscription(subTypeChannelRaid, 1, "{\"from_broadcaster_user_id\":\"$userId\"}"),
            Subscription(subTypeChannelCheer, 1, broadcaster),
            Subscription(subTypeChannelHypeTrainBegin, 2, broadcaster),
            Subscription(subTypeChannelHypeTrainProgress, 2, broadcaster),
            Subscription(subTypeChannelHypeTrainEnd, 2, broadcaster),
            Subscription(subTypeChannelAdBreakBegin, 1, broadcaster),
            Subscription(subTypeChannelModerate, 2, broadcasterAndModerator),
            Subscription(subTypeChannelPollBegin, 1, broadcaster),
            Subscription(subTypeChannelPollProgress, 1, broadcaster),
            Subscription(subTypeChannelPollEnd, 1, broadcaster),
            Subscription(subTypeChannelPredictionBegin, 1, broadcaster),
            Subscription(subTypeChannelPredictionProgress, 1, broadcaster),
            Subscription(subTypeChannelPredictionLock, 1, broadcaster),
            Subscription(subTypeChannelPredictionEnd, 1, broadcaster),
            Subscription(subTypeChannelShoutoutCreate, 1, broadcasterAndModerator),
        )
    }

    private fun subscribe(subscriptions: List<Subscription>) {
        val sessionId = sessionId
        for (subscription in subscriptions) {
            val body = createBody(subscription.type, subscription.version, subscription.condition)
            twitchApi.createEventSubSubscription(body) { result ->
                if (sessionId != this.sessionId) {
                    return@createEventSubSubscription
                }
                handleSubscribeResult(subscription, result)
            }
        }
    }

    private fun handleSubscribeResult(subscription: Subscription, result: OperationResult) {
        when (result) {
            is NetworkResponse.Success<*> -> {
                remainingSubscriptions -= 1
                if (remainingSubscriptions == 0) {
                    connected = true
                    reconnectDelay = initialReconnectDelay
                }
            }
            NetworkResponse.AuthError -> Log.i(
                tag,
                "twitch: event-sub: Not authorized to subscribe to ${subscription.type}",
            )
            NetworkResponse.Error -> {
                Log.i(tag, "twitch: event-sub: Failed to subscribe to ${subscription.type}")
                reconnectLater()
            }
        }
    }

    private fun reconnectLater() {
        Log.i(tag, "twitch: event-sub: Reconnecting in $reconnectDelay seconds")
        stopInternal()
        connectDelayTimer.startSingleShot(reconnectDelay) {
            if (started) {
                connect()
            }
        }
        reconnectDelay = minOf(reconnectDelay * 2, 120.0)
    }

    private fun createBody(type: String, version: Int, condition: String): String {
        return """
        {
            "type": "$type",
            "version": "$version",
            "condition": $condition,
            "transport": {
                "method": "websocket",
                "session_id": "$sessionId"
            }
        }
        """.trimIndent()
    }

    private fun handleNotification(
        message: BasicMessage,
        messageText: String,
        messageData: ByteArray,
    ) {
        try {
            when (message.metadata.subscription_type) {
                subTypeChannelFollow -> handleNotificationChannelFollow(messageData)
                subTypeChannelChatNotification -> handleNotificationChannelChatNotification(messageData)
                subTypeChannelChannelPointsCustomRewardRedemptionAdd ->
                    handleChannelPointsCustomRewardRedemptionAdd(messageData)
                subTypeChannelRaid -> handleChannelRaid(messageData)
                subTypeChannelCheer -> handleChannelCheer(messageData)
                subTypeChannelHypeTrainBegin -> handleChannelHypeTrainBegin(messageData)
                subTypeChannelHypeTrainProgress -> handleChannelHypeTrainProgress(messageData)
                subTypeChannelHypeTrainEnd -> handleChannelHypeTrainEnd(messageData)
                subTypeChannelAdBreakBegin -> handleChannelAdBreakBegin(messageData)
                subTypeChannelModerate -> handleChannelModerate(messageData)
                subTypeChannelPollBegin -> handleChannelPollBegin(messageData)
                subTypeChannelPollProgress -> handleChannelPollProgress(messageData)
                subTypeChannelPollEnd -> handleChannelPollEnd(messageData)
                subTypeChannelPredictionBegin -> handleChannelPredictionBegin(messageData)
                subTypeChannelPredictionProgress -> handleChannelPredictionProgress(messageData)
                subTypeChannelPredictionLock -> handleChannelPredictionLock(messageData)
                subTypeChannelPredictionEnd -> handleChannelPredictionEnd(messageData)
                subTypeChannelShoutoutCreate -> handleChannelShoutoutCreate(messageData)
                else -> {
                    val type = message.metadata.subscription_type
                    if (type != null) {
                        Log.i(tag, "twitch: event-sub: Unknown notification type $type")
                    } else {
                        Log.i(tag, "twitch: event-sub: Missing notification type")
                    }
                }
            }
            delegate.twitchEventSubNotification(messageText)
        } catch (e: Exception) {
            val subscriptionType = message.metadata.subscription_type ?: "unknown"
            Log.i(tag, "twitch: event-sub: Failed to handle notification $subscriptionType.")
        }
    }

    private fun handleNotificationChannelFollow(messageData: ByteArray) {
        val message = decodeJson<NotificationChannelFollowMessage>(messageData)
        delegate.twitchEventSubChannelFollow(message.payload.event)
    }

    private fun handleNotificationChannelChatNotification(messageData: ByteArray) {
        val message = decodeJson<NotificationChannelChatNotificationMessage>(messageData)
        val event = message.payload.event
        when (event.notice_type) {
            "sub", "shared_chat_sub" -> handleChatNotificationSub(event)
            "resub", "shared_chat_resub" -> handleChatNotificationResub(event)
            "sub_gift", "shared_chat_sub_gift" -> handleChatNotificationSubGift(event)
            "community_sub_gift", "shared_chat_community_sub_gift" ->
                handleChatNotificationCommunitySubGift(event)
            "prime_paid_upgrade", "shared_chat_prime_paid_upgrade" ->
                handleChatNotificationPrimePaidUpgrade(event)
            "gift_paid_upgrade", "shared_chat_gift_paid_upgrade" ->
                handleChatNotificationGiftPaidUpgrade(event)
            "shared_chat_raid" -> handleChatNotificationRaid(event)
            "watch_streak" -> handleChatNotificationWatchStreak(event)
            else -> Unit
        }
    }

    private fun handleChatNotificationSub(event: NotificationChannelChatNotificationEvent) {
        val sub = event.sub ?: event.shared_chat_sub ?: return
        delegate.twitchEventSubChannelSubscribe(
            TwitchEventSubNotificationChannelSubscribeEvent(
                user_name = event.userName(),
                tier = sub.sub_tier,
                is_gift = false,
                is_prime = sub.is_prime,
                message = event.message,
                sharedChat = event.sharedChat(),
                chatter = event.chatter(),
            ),
        )
    }

    private fun handleChatNotificationResub(event: NotificationChannelChatNotificationEvent) {
        val resub = event.resub ?: event.shared_chat_resub ?: return
        delegate.twitchEventSubChannelSubscriptionMessage(
            TwitchEventSubNotificationChannelSubscriptionMessageEvent(
                user_name = event.userName(),
                cumulative_months = resub.cumulative_months,
                streak_months = resub.streak_months,
                tier = resub.sub_tier,
                message = event.message,
                sharedChat = event.sharedChat(),
                chatter = event.chatter(),
            ),
        )
    }

    private fun handleChatNotificationSubGift(event: NotificationChannelChatNotificationEvent) {
        val subGift = event.sub_gift ?: event.shared_chat_sub_gift ?: return
        if (subGift.community_gift_id != null) {
            return
        }
        handleChatNotificationGift(event, 1, subGift.sub_tier)
    }

    private fun handleChatNotificationCommunitySubGift(
        event: NotificationChannelChatNotificationEvent,
    ) {
        val communitySubGift = event.community_sub_gift ?: event.shared_chat_community_sub_gift ?: return
        handleChatNotificationGift(event, communitySubGift.total, communitySubGift.sub_tier)
    }

    private fun handleChatNotificationPrimePaidUpgrade(
        event: NotificationChannelChatNotificationEvent,
    ) {
        val primePaidUpgrade = event.prime_paid_upgrade ?: event.shared_chat_prime_paid_upgrade ?: return
        delegate.twitchEventSubChannelSubscriptionUpgrade(
            TwitchEventSubNotificationChannelSubscriptionUpgradeEvent(
                user_name = event.userName(),
                tier = primePaidUpgrade.sub_tier,
                message = event.message,
                sharedChat = event.sharedChat(),
                chatter = event.chatter(),
            ),
        )
    }

    private fun handleChatNotificationGiftPaidUpgrade(
        event: NotificationChannelChatNotificationEvent,
    ) {
        delegate.twitchEventSubChannelSubscriptionUpgrade(
            TwitchEventSubNotificationChannelSubscriptionUpgradeEvent(
                user_name = event.userName(),
                tier = null,
                message = event.message,
                sharedChat = event.sharedChat(),
                chatter = event.chatter(),
            ),
        )
    }

    private fun handleChatNotificationRaid(event: NotificationChannelChatNotificationEvent) {
        val raid = event.shared_chat_raid ?: return
        delegate.twitchEventSubChannelRaid(
            TwitchEventSubChannelRaidEvent(
                from_broadcaster_user_id = raid.user_id,
                from_broadcaster_user_name = raid.user_name,
                viewers = raid.viewer_count,
                message = event.message,
                sharedChat = event.sharedChat(),
                chatter = event.chatter(),
            ),
        )
    }

    private fun handleChatNotificationWatchStreak(event: NotificationChannelChatNotificationEvent) {
        val watchStreak = event.watch_streak ?: return
        delegate.twitchEventSubChannelWatchStreak(
            TwitchEventSubNotificationChannelWatchStreakEvent(
                user_name = event.userName(),
                streak_count = watchStreak.streak_count,
                message = event.message,
                sharedChat = event.sharedChat(),
                chatter = event.chatter(),
            ),
        )
    }

    private fun handleChatNotificationGift(
        event: NotificationChannelChatNotificationEvent,
        total: Int,
        tier: String,
    ) {
        delegate.twitchEventSubChannelSubscriptionGift(
            TwitchEventSubNotificationChannelSubscriptionGiftEvent(
                user_name = if (event.chatter_is_anonymous) null else event.userName(),
                total = total,
                tier = tier,
                message = event.message,
                sharedChat = event.sharedChat(),
                chatter = event.chatter(),
            ),
        )
    }

    private fun handleChannelPointsCustomRewardRedemptionAdd(messageData: ByteArray) {
        val message = decodeJson<NotificationChannelPointsCustomRewardRedemptionAddMessage>(messageData)
        delegate.twitchEventSubChannelPointsCustomRewardRedemptionAdd(message.payload.event)
    }

    private fun handleChannelRaid(messageData: ByteArray) {
        val message = decodeJson<NotificationChannelRaidMessage>(messageData)
        delegate.twitchEventSubChannelRaid(message.payload.event)
    }

    private fun handleChannelCheer(messageData: ByteArray) {
        val message = decodeJson<NotificationChannelCheerMessage>(messageData)
        delegate.twitchEventSubChannelCheer(message.payload.event)
    }

    private fun handleChannelHypeTrainBegin(messageData: ByteArray) {
        val message = decodeJson<NotificationChannelHypeTrainBeginMessage>(messageData)
        delegate.twitchEventSubChannelHypeTrainBegin(message.payload.event)
    }

    private fun handleChannelHypeTrainProgress(messageData: ByteArray) {
        val message = decodeJson<NotificationChannelHypeTrainProgressMessage>(messageData)
        delegate.twitchEventSubChannelHypeTrainProgress(message.payload.event)
    }

    private fun handleChannelHypeTrainEnd(messageData: ByteArray) {
        val message = decodeJson<NotificationChannelHypeTrainEndMessage>(messageData)
        delegate.twitchEventSubChannelHypeTrainEnd(message.payload.event)
    }

    private fun handleChannelAdBreakBegin(messageData: ByteArray) {
        val message = decodeJson<NotificationChannelAdBreakBeginMessage>(messageData)
        delegate.twitchEventSubChannelAdBreakBegin(message.payload.event)
    }

    private fun handleChannelPollBegin(messageData: ByteArray) {
        val message = decodeJson<NotificationChannelPollMessage>(messageData)
        delegate.twitchEventSubChannelPollBegin(message.payload.event)
    }

    private fun handleChannelPollProgress(messageData: ByteArray) {
        val message = decodeJson<NotificationChannelPollMessage>(messageData)
        delegate.twitchEventSubChannelPollProgress(message.payload.event)
    }

    private fun handleChannelPollEnd(messageData: ByteArray) {
        val message = decodeJson<NotificationChannelPollMessage>(messageData)
        delegate.twitchEventSubChannelPollEnd(message.payload.event)
    }

    private fun handleChannelPredictionBegin(messageData: ByteArray) {
        val message = decodeJson<NotificationChannelPredictionMessage>(messageData)
        delegate.twitchEventSubChannelPredictionBegin(message.payload.event)
    }

    private fun handleChannelPredictionProgress(messageData: ByteArray) {
        val message = decodeJson<NotificationChannelPredictionMessage>(messageData)
        delegate.twitchEventSubChannelPredictionProgress(message.payload.event)
    }

    private fun handleChannelPredictionLock(messageData: ByteArray) {
        val message = decodeJson<NotificationChannelPredictionMessage>(messageData)
        delegate.twitchEventSubChannelPredictionLock(message.payload.event)
    }

    private fun handleChannelPredictionEnd(messageData: ByteArray) {
        val message = decodeJson<NotificationChannelPredictionMessage>(messageData)
        delegate.twitchEventSubChannelPredictionEnd(message.payload.event)
    }

    private fun handleChannelModerate(messageData: ByteArray) {
        val message = decodeJson<NotificationChannelModerateMessage>(messageData)
        delegate.twitchEventSubChannelModerate(message.payload.event)
    }

    private fun handleChannelShoutoutCreate(messageData: ByteArray) {
        val message = decodeJson<NotificationChannelShoutoutCreateMessage>(messageData)
        delegate.twitchEventSubChannelShoutoutCreate(message.payload.event)
    }

    override fun webSocketClientConnected(client: WebSocketClient) {}

    override fun webSocketClientDisconnected(client: WebSocketClient) {
        connected = false
    }

    override fun webSocketClientReceiveMessage(client: WebSocketClient, string: String) {
        handleMessage(string)
    }
}

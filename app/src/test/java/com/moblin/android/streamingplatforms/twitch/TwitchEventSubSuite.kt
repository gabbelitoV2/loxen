package com.moblin.android.streamingplatforms.twitch

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import com.moblin.android.AppDelegate
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private class EventSubTestDelegate : TwitchEventSubDelegate {
    val follows = mutableListOf<TwitchEventSubNotificationChannelFollowEvent>()
    val subscribes = mutableListOf<TwitchEventSubNotificationChannelSubscribeEvent>()
    val resubscribes = mutableListOf<TwitchEventSubNotificationChannelSubscriptionMessageEvent>()
    val gifts = mutableListOf<TwitchEventSubNotificationChannelSubscriptionGiftEvent>()
    val upgrades = mutableListOf<TwitchEventSubNotificationChannelSubscriptionUpgradeEvent>()
    val watchStreaks = mutableListOf<TwitchEventSubNotificationChannelWatchStreakEvent>()
    val redemptions = mutableListOf<TwitchEventSubNotificationChannelPointsCustomRewardRedemptionAddEvent>()
    val raids = mutableListOf<TwitchEventSubChannelRaidEvent>()
    val cheers = mutableListOf<TwitchEventSubChannelCheerEvent>()
    val hypeTrainBegins = mutableListOf<TwitchEventSubChannelHypeTrainBeginEvent>()
    val hypeTrainProgresses = mutableListOf<TwitchEventSubChannelHypeTrainProgressEvent>()
    val hypeTrainEnds = mutableListOf<TwitchEventSubChannelHypeTrainEndEvent>()
    val adBreaks = mutableListOf<TwitchEventSubChannelAdBreakBeginEvent>()
    val polls = mutableListOf<Pair<String, TwitchEventSubChannelPollEvent>>()
    val predictions = mutableListOf<Pair<String, TwitchEventSubChannelPredictionEvent>>()
    val moderates = mutableListOf<TwitchEventSubChannelModerateEvent>()
    var unauthorizedCount = 0
    val notifications = mutableListOf<String>()

    override fun twitchEventSubChannelFollow(event: TwitchEventSubNotificationChannelFollowEvent) {
        follows.add(event)
    }

    override fun twitchEventSubChannelSubscribe(event: TwitchEventSubNotificationChannelSubscribeEvent) {
        subscribes.add(event)
    }

    override fun twitchEventSubChannelSubscriptionGift(
        event: TwitchEventSubNotificationChannelSubscriptionGiftEvent
    ) {
        gifts.add(event)
    }

    override fun twitchEventSubChannelSubscriptionMessage(
        event: TwitchEventSubNotificationChannelSubscriptionMessageEvent
    ) {
        resubscribes.add(event)
    }

    override fun twitchEventSubChannelSubscriptionUpgrade(
        event: TwitchEventSubNotificationChannelSubscriptionUpgradeEvent
    ) {
        upgrades.add(event)
    }

    override fun twitchEventSubChannelWatchStreak(event: TwitchEventSubNotificationChannelWatchStreakEvent) {
        watchStreaks.add(event)
    }

    override fun twitchEventSubChannelPointsCustomRewardRedemptionAdd(
        event: TwitchEventSubNotificationChannelPointsCustomRewardRedemptionAddEvent
    ) {
        redemptions.add(event)
    }

    override fun twitchEventSubChannelRaid(event: TwitchEventSubChannelRaidEvent) {
        raids.add(event)
    }

    override fun twitchEventSubChannelCheer(event: TwitchEventSubChannelCheerEvent) {
        cheers.add(event)
    }

    override fun twitchEventSubChannelHypeTrainBegin(event: TwitchEventSubChannelHypeTrainBeginEvent) {
        hypeTrainBegins.add(event)
    }

    override fun twitchEventSubChannelHypeTrainProgress(event: TwitchEventSubChannelHypeTrainProgressEvent) {
        hypeTrainProgresses.add(event)
    }

    override fun twitchEventSubChannelHypeTrainEnd(event: TwitchEventSubChannelHypeTrainEndEvent) {
        hypeTrainEnds.add(event)
    }

    override fun twitchEventSubChannelAdBreakBegin(event: TwitchEventSubChannelAdBreakBeginEvent) {
        adBreaks.add(event)
    }

    override fun twitchEventSubChannelPollBegin(event: TwitchEventSubChannelPollEvent) {
        polls.add("begin" to event)
    }

    override fun twitchEventSubChannelPollProgress(event: TwitchEventSubChannelPollEvent) {
        polls.add("progress" to event)
    }

    override fun twitchEventSubChannelPollEnd(event: TwitchEventSubChannelPollEvent) {
        polls.add("end" to event)
    }

    override fun twitchEventSubChannelPredictionBegin(event: TwitchEventSubChannelPredictionEvent) {
        predictions.add("begin" to event)
    }

    override fun twitchEventSubChannelPredictionProgress(event: TwitchEventSubChannelPredictionEvent) {
        predictions.add("progress" to event)
    }

    override fun twitchEventSubChannelPredictionLock(event: TwitchEventSubChannelPredictionEvent) {
        predictions.add("lock" to event)
    }

    override fun twitchEventSubChannelPredictionEnd(event: TwitchEventSubChannelPredictionEvent) {
        predictions.add("end" to event)
    }

    override fun twitchEventSubChannelModerate(event: TwitchEventSubChannelModerateEvent) {
        moderates.add(event)
    }

    override fun twitchEventSubUnauthorized() {
        unauthorizedCount += 1
    }

    override fun twitchEventSubNotification(message: String) {
        notifications.add(message)
    }
}

private fun notification(subscriptionType: String, event: String): String {
    return """
    {
      "metadata": {
        "message_id": "1",
        "message_type": "notification",
        "message_timestamp": "2026-09-06T10:00:00.000Z",
        "subscription_type": "$subscriptionType",
        "subscription_version": "1"
      },
      "payload": {
        "subscription": {"id": "s", "type": "$subscriptionType"},
        "event": $event
      }
    }
    """
}

private fun chatNotification(
    noticeType: String,
    shared: Boolean,
    payload: String,
    message: String = """{"text": "hello", "fragments": []}""",
    chatterUserName: String = "\"Viewer\"",
    chatterIsAnonymous: Boolean = false,
    color: String = "",
    badges: String = "[]",
): String {
    val payloadPrefix = if (payload.isEmpty()) "" else "$payload,"
    val source = if (shared) {
        """
    "source_broadcaster_user_id": "222",
    "source_broadcaster_user_name": "Partner",
    "source_broadcaster_user_login": "partner",
    """
    } else {
        """
    "source_broadcaster_user_id": null,
    "source_broadcaster_user_name": null,
    "source_broadcaster_user_login": null,
    """
    }
    return """
    {
      "metadata": {
        "message_id": "1",
        "message_type": "notification",
        "message_timestamp": "2026-09-06T10:00:00.000Z",
        "subscription_type": "channel.chat.notification",
        "subscription_version": "1"
      },
      "payload": {
        "subscription": {"id": "s", "type": "channel.chat.notification"},
        "event": {
          "broadcaster_user_id": "111",
          "broadcaster_user_login": "me",
          "broadcaster_user_name": "Me",
          $source
          $payloadPrefix
          "chatter_user_id": "333",
          "chatter_user_login": "viewer",
          "chatter_user_name": $chatterUserName,
          "chatter_is_anonymous": $chatterIsAnonymous,
          "color": "$color",
          "badges": $badges,
          "system_message": "",
          "message_id": "m",
          "message": $message,
          "notice_type": "$noticeType",
          "sub": null,
          "resub": null,
          "sub_gift": null,
          "community_sub_gift": null,
          "gift_paid_upgrade": null,
          "prime_paid_upgrade": null,
          "raid": null,
          "shared_chat_sub": null,
          "shared_chat_resub": null,
          "shared_chat_sub_gift": null,
          "shared_chat_community_sub_gift": null,
          "shared_chat_gift_paid_upgrade": null,
          "shared_chat_prime_paid_upgrade": null,
          "shared_chat_raid": null
        }
      }
    }
    """
}

private fun makeEventSub(delegate: EventSubTestDelegate): TwitchEventSub {
    return TwitchEventSub(
        context = AppDelegate.context,
        remoteControl = true,
        userId = "111",
        accessToken = "",
        delegate = delegate,
    )
}

@RunWith(RobolectricTestRunner::class)
class TwitchEventSubSuite {
    @Test
    fun sub() {
        val delegate = EventSubTestDelegate()
        makeEventSub(delegate).handleMessage(messageText = chatNotification(
            noticeType = "sub",
            shared = false,
            payload = "\"sub\": {\"sub_tier\": \"1000\", \"is_prime\": false, \"duration_months\": 1}"
        ))
        assertEquals(1, delegate.subscribes.size)
        assertEquals("Viewer", delegate.subscribes.firstOrNull()?.user_name)
        assertEquals(1, delegate.subscribes.firstOrNull()?.tierAsNumber())
        assertEquals("hello", delegate.subscribes.firstOrNull()?.message?.text)
        assertNull(delegate.subscribes.firstOrNull()?.sharedChat)
    }

    @Test
    fun sharedChatSub() {
        val delegate = EventSubTestDelegate()
        makeEventSub(delegate).handleMessage(messageText = chatNotification(
            noticeType = "shared_chat_sub",
            shared = true,
            payload = "\"shared_chat_sub\": {\"sub_tier\": \"2000\", \"is_prime\": true, \"duration_months\": 1}"
        ))
        assertEquals(1, delegate.subscribes.size)
        assertEquals("Viewer", delegate.subscribes.firstOrNull()?.user_name)
        assertEquals(2, delegate.subscribes.firstOrNull()?.tierAsNumber())
        assertEquals(true, delegate.subscribes.firstOrNull()?.isPrime())
        assertEquals("222", delegate.subscribes.firstOrNull()?.sharedChat?.broadcasterUserId)
        assertEquals("Partner", delegate.subscribes.firstOrNull()?.sharedChat?.broadcasterUserName)
    }

    @Test
    fun sharedChatResub() {
        val delegate = EventSubTestDelegate()
        makeEventSub(delegate).handleMessage(messageText = chatNotification(
            noticeType = "shared_chat_resub",
            shared = true,
            payload = """
            "shared_chat_resub": {
              "cumulative_months": 7,
              "duration_months": 1,
              "streak_months": 3,
              "sub_tier": "1000",
              "is_prime": false,
              "is_gift": false
            }
            """
        ))
        assertEquals(1, delegate.resubscribes.size)
        assertEquals(7, delegate.resubscribes.firstOrNull()?.cumulative_months)
        assertEquals(3, delegate.resubscribes.firstOrNull()?.streak_months)
        assertEquals("hello", delegate.resubscribes.firstOrNull()?.message?.text)
        assertEquals("222", delegate.resubscribes.firstOrNull()?.sharedChat?.broadcasterUserId)
    }

    @Test
    fun resubMessageFragments() {
        val delegate = EventSubTestDelegate()
        makeEventSub(delegate).handleMessage(messageText = chatNotification(
            noticeType = "resub",
            shared = false,
            payload = """
            "resub": {
              "cumulative_months": 2,
              "duration_months": 1,
              "streak_months": null,
              "sub_tier": "1000",
              "is_prime": false,
              "is_gift": false
            }
            """,
            message = """
            {
              "text": "Kappa hi",
              "fragments": [
                {"type": "emote", "text": "Kappa", "cheermote": null,
                 "emote": {"id": "25", "emote_set_id": "0", "owner_id": "0", "format": ["static"]},
                 "mention": null},
                {"type": "text", "text": " hi", "cheermote": null, "emote": null, "mention": null}
              ]
            }
            """
        ))
        assertEquals(1, delegate.resubscribes.size)
        val fragments = delegate.resubscribes.firstOrNull()?.message?.fragments ?: emptyList()
        assertEquals(listOf("emote", "text"), fragments.map { it.type })
        assertEquals(listOf("Kappa", " hi"), fragments.map { it.text })
        assertEquals(listOf<String?>("25", null), fragments.map { it.emote?.id })
    }

    @Test
    fun sharedChatCommunitySubGift() {
        val delegate = EventSubTestDelegate()
        makeEventSub(delegate).handleMessage(messageText = chatNotification(
            noticeType = "shared_chat_community_sub_gift",
            shared = true,
            payload = "\"shared_chat_community_sub_gift\": {\"id\": \"g\", \"total\": 5, \"sub_tier\": \"1000\"}"
        ))
        assertEquals(1, delegate.gifts.size)
        assertEquals("Viewer", delegate.gifts.firstOrNull()?.user_name)
        assertEquals(5, delegate.gifts.firstOrNull()?.total)
        assertEquals("hello", delegate.gifts.firstOrNull()?.message?.text)
        assertEquals("Partner", delegate.gifts.firstOrNull()?.sharedChat?.broadcasterUserName)
    }

    @Test
    fun sharedChatSubGiftPartOfCommunityGiftIsIgnored() {
        val delegate = EventSubTestDelegate()
        makeEventSub(delegate).handleMessage(messageText = chatNotification(
            noticeType = "shared_chat_sub_gift",
            shared = true,
            payload = """
            "shared_chat_sub_gift": {
              "duration_months": 1,
              "cumulative_total": 1,
              "recipient_user_id": "4",
              "recipient_user_name": "R",
              "recipient_user_login": "r",
              "sub_tier": "1000",
              "community_gift_id": "g"
            }
            """
        ))
        assertEquals(0, delegate.gifts.size)
    }

    @Test
    fun sharedChatGiftPaidUpgrade() {
        val delegate = EventSubTestDelegate()
        makeEventSub(delegate).handleMessage(messageText = chatNotification(
            noticeType = "shared_chat_gift_paid_upgrade",
            shared = true,
            payload = "\"shared_chat_gift_paid_upgrade\": {\"gifter_is_anonymous\": true}"
        ))
        assertEquals(1, delegate.upgrades.size)
        assertNull(delegate.upgrades.firstOrNull()?.tierAsNumber())
        assertEquals("hello", delegate.upgrades.firstOrNull()?.message?.text)
        assertEquals("222", delegate.upgrades.firstOrNull()?.sharedChat?.broadcasterUserId)
    }

    @Test
    fun sharedChatRaid() {
        val delegate = EventSubTestDelegate()
        makeEventSub(delegate).handleMessage(messageText = chatNotification(
            noticeType = "shared_chat_raid",
            shared = true,
            payload = """
            "shared_chat_raid": {
              "user_id": "555",
              "user_name": "Raider",
              "user_login": "raider",
              "viewer_count": 42,
              "profile_image_url": "https://example.com/raider.png"
            }
            """
        ))
        assertEquals(1, delegate.raids.size)
        assertEquals("555", delegate.raids.firstOrNull()?.from_broadcaster_user_id)
        assertEquals("Raider", delegate.raids.firstOrNull()?.from_broadcaster_user_name)
        assertEquals(42, delegate.raids.firstOrNull()?.viewers)
        assertEquals("hello", delegate.raids.firstOrNull()?.message?.text)
        assertEquals("222", delegate.raids.firstOrNull()?.sharedChat?.broadcasterUserId)
    }

    @Test
    fun invalidJsonIsIgnored() {
        val delegate = EventSubTestDelegate()
        val eventSub = makeEventSub(delegate)
        eventSub.handleMessage(messageText = "not json")
        eventSub.handleMessage(messageText = "{}")
        eventSub.handleMessage(messageText = """{"metadata": {}}""")
        assertEquals(0, delegate.notifications.size)
        assertEquals(0, delegate.subscribes.size)
    }

    @Test
    fun keepaliveAndUnknownMessageTypesAreIgnored() {
        val delegate = EventSubTestDelegate()
        val eventSub = makeEventSub(delegate)
        eventSub.handleMessage(messageText = """
        {
          "metadata": {"message_id": "1", "message_type": "session_keepalive"},
          "payload": {}
        }
        """)
        eventSub.handleMessage(messageText = """
        {
          "metadata": {"message_id": "2", "message_type": "revocation", "subscription_type": "channel.follow"},
          "payload": {}
        }
        """)
        assertEquals(0, delegate.notifications.size)
        assertEquals(0, delegate.follows.size)
    }

    @Test
    fun unknownNotificationTypeStillForwardsRawMessage() {
        val delegate = EventSubTestDelegate()
        val message = notification(subscriptionType = "channel.unknown", event = """{"foo": 1}""")
        makeEventSub(delegate).handleMessage(messageText = message)
        assertEquals(listOf(message), delegate.notifications)
    }

    @Test
    fun notificationWithoutSubscriptionTypeStillForwardsRawMessage() {
        val delegate = EventSubTestDelegate()
        val message = """
        {
          "metadata": {"message_id": "1", "message_type": "notification"},
          "payload": {}
        }
        """
        makeEventSub(delegate).handleMessage(messageText = message)
        assertEquals(listOf(message), delegate.notifications)
    }

    @Test
    fun undecodableNotificationIsDroppedWithoutForwarding() {
        val delegate = EventSubTestDelegate()
        makeEventSub(delegate).handleMessage(messageText = notification(
            subscriptionType = "channel.follow",
            event = """{"user_login": "viewer"}"""
        ))
        assertEquals(0, delegate.follows.size)
        assertEquals(0, delegate.notifications.size)
    }

    @Test
    fun follow() {
        val delegate = EventSubTestDelegate()
        val message = notification(
            subscriptionType = "channel.follow",
            event = """
            {
              "user_id": "333",
              "user_login": "viewer",
              "user_name": "Viewer",
              "broadcaster_user_id": "111",
              "broadcaster_user_login": "me",
              "broadcaster_user_name": "Me",
              "followed_at": "2026-09-06T10:00:00.000Z"
            }
            """
        )
        makeEventSub(delegate).handleMessage(messageText = message)
        assertEquals(1, delegate.follows.size)
        assertEquals("Viewer", delegate.follows.firstOrNull()?.user_name)
        assertEquals(listOf(message), delegate.notifications)
    }

    @Test
    fun channelPointsCustomRewardRedemptionAdd() {
        val delegate = EventSubTestDelegate()
        makeEventSub(delegate).handleMessage(messageText = notification(
            subscriptionType = "channel.channel_points_custom_reward_redemption.add",
            event = """
            {
              "id": "r1",
              "broadcaster_user_id": "111",
              "broadcaster_user_login": "me",
              "broadcaster_user_name": "Me",
              "user_id": "333",
              "user_login": "viewer",
              "user_name": "Viewer",
              "user_input": "hi",
              "status": "unfulfilled",
              "reward": {"id": "rw", "title": "Hydrate", "cost": 500, "prompt": "Drink water"},
              "redeemed_at": "2026-09-06T10:00:00.000Z"
            }
            """
        ))
        assertEquals(1, delegate.redemptions.size)
        assertEquals("Viewer", delegate.redemptions.firstOrNull()?.user_name)
        assertEquals("unfulfilled", delegate.redemptions.firstOrNull()?.status)
        assertEquals("Hydrate", delegate.redemptions.firstOrNull()?.reward?.title)
        assertEquals(500, delegate.redemptions.firstOrNull()?.reward?.cost)
    }

    @Test
    fun channelRaid() {
        val delegate = EventSubTestDelegate()
        makeEventSub(delegate).handleMessage(messageText = notification(
            subscriptionType = "channel.raid",
            event = """
            {
              "from_broadcaster_user_id": "555",
              "from_broadcaster_user_login": "raider",
              "from_broadcaster_user_name": "Raider",
              "to_broadcaster_user_id": "111",
              "to_broadcaster_user_login": "me",
              "to_broadcaster_user_name": "Me",
              "viewers": 9001
            }
            """
        ))
        assertEquals(1, delegate.raids.size)
        assertEquals("555", delegate.raids.firstOrNull()?.from_broadcaster_user_id)
        assertEquals("Raider", delegate.raids.firstOrNull()?.from_broadcaster_user_name)
        assertEquals(9001, delegate.raids.firstOrNull()?.viewers)
        assertNull(delegate.raids.firstOrNull()?.message)
        assertNull(delegate.raids.firstOrNull()?.sharedChat)
    }

    @Test
    fun channelCheer() {
        val delegate = EventSubTestDelegate()
        makeEventSub(delegate).handleMessage(messageText = notification(
            subscriptionType = "channel.cheer",
            event = """
            {
              "is_anonymous": false,
              "user_id": "333",
              "user_login": "viewer",
              "user_name": "Viewer",
              "broadcaster_user_id": "111",
              "broadcaster_user_login": "me",
              "broadcaster_user_name": "Me",
              "message": "Cheer100 nice!",
              "bits": 100
            }
            """
        ))
        assertEquals(1, delegate.cheers.size)
        assertEquals("Viewer", delegate.cheers.firstOrNull()?.user_name)
        assertEquals("Cheer100 nice!", delegate.cheers.firstOrNull()?.message)
        assertEquals(100, delegate.cheers.firstOrNull()?.bits)
    }

    @Test
    fun anonymousChannelCheer() {
        val delegate = EventSubTestDelegate()
        makeEventSub(delegate).handleMessage(messageText = notification(
            subscriptionType = "channel.cheer",
            event = """
            {
              "is_anonymous": true,
              "user_id": null,
              "user_login": null,
              "user_name": null,
              "broadcaster_user_id": "111",
              "broadcaster_user_login": "me",
              "broadcaster_user_name": "Me",
              "message": "Cheer1",
              "bits": 1
            }
            """
        ))
        assertEquals(1, delegate.cheers.size)
        assertNull(delegate.cheers.firstOrNull()?.user_name)
        assertEquals(1, delegate.cheers.firstOrNull()?.bits)
    }

    @Test
    fun hypeTrain() {
        val delegate = EventSubTestDelegate()
        val eventSub = makeEventSub(delegate)
        eventSub.handleMessage(messageText = notification(
            subscriptionType = "channel.hype_train.begin",
            event = """
            {
              "id": "h",
              "broadcaster_user_id": "111",
              "total": 137,
              "progress": 137,
              "goal": 500,
              "top_contributions": [],
              "last_contribution": {"user_id": "333", "type": "bits", "total": 137},
              "level": 1,
              "started_at": "2026-09-06T10:00:00.000Z",
              "expires_at": "2026-09-06T10:05:00.000Z"
            }
            """
        ))
        eventSub.handleMessage(messageText = notification(
            subscriptionType = "channel.hype_train.progress",
            event = """
            {
              "id": "h",
              "broadcaster_user_id": "111",
              "total": 700,
              "progress": 200,
              "goal": 1000,
              "top_contributions": [],
              "level": 2,
              "started_at": "2026-09-06T10:00:00.000Z",
              "expires_at": "2026-09-06T10:05:00.000Z"
            }
            """
        ))
        eventSub.handleMessage(messageText = notification(
            subscriptionType = "channel.hype_train.end",
            event = """
            {
              "id": "h",
              "broadcaster_user_id": "111",
              "level": 3,
              "total": 1500,
              "top_contributions": [],
              "started_at": "2026-09-06T10:00:00.000Z",
              "ended_at": "2026-09-06T10:05:00.000Z",
              "cooldown_ends_at": "2026-09-06T11:05:00.000Z"
            }
            """
        ))
        assertEquals(1, delegate.hypeTrainBegins.size)
        assertEquals(137, delegate.hypeTrainBegins.firstOrNull()?.progress)
        assertEquals(500, delegate.hypeTrainBegins.firstOrNull()?.goal)
        assertEquals(1, delegate.hypeTrainBegins.firstOrNull()?.level)
        assertEquals(1, delegate.hypeTrainProgresses.size)
        assertEquals(200, delegate.hypeTrainProgresses.firstOrNull()?.progress)
        assertEquals(1000, delegate.hypeTrainProgresses.firstOrNull()?.goal)
        assertEquals(2, delegate.hypeTrainProgresses.firstOrNull()?.level)
        assertEquals(1, delegate.hypeTrainEnds.size)
        assertEquals(3, delegate.hypeTrainEnds.firstOrNull()?.level)
        assertEquals(3, delegate.notifications.size)
    }

    @Test
    fun adBreakBegin() {
        val delegate = EventSubTestDelegate()
        makeEventSub(delegate).handleMessage(messageText = notification(
            subscriptionType = "channel.ad_break.begin",
            event = """
            {
              "duration_seconds": 60,
              "started_at": "2026-09-06T10:00:00.000Z",
              "is_automatic": false,
              "broadcaster_user_id": "111",
              "broadcaster_user_login": "me",
              "broadcaster_user_name": "Me",
              "requester_user_id": "111",
              "requester_user_login": "me",
              "requester_user_name": "Me"
            }
            """
        ))
        assertEquals(1, delegate.adBreaks.size)
        assertEquals(60, delegate.adBreaks.firstOrNull()?.duration_seconds)
        assertEquals(false, delegate.adBreaks.firstOrNull()?.is_automatic)
    }

    @Test
    fun pollBeginProgressEnd() {
        val delegate = EventSubTestDelegate()
        val eventSub = makeEventSub(delegate)
        eventSub.handleMessage(messageText = notification(
            subscriptionType = "channel.poll.begin",
            event = """
            {
              "id": "p",
              "broadcaster_user_id": "111",
              "title": "Pizza?",
              "choices": [{"id": "c1", "title": "Yes"}, {"id": "c2", "title": "No"}],
              "bits_voting": {"is_enabled": false, "amount_per_vote": 0},
              "channel_points_voting": {"is_enabled": false, "amount_per_vote": 0},
              "started_at": "2026-09-06T10:00:00.000Z",
              "ends_at": "2026-09-06T10:05:00.000Z"
            }
            """
        ))
        eventSub.handleMessage(messageText = notification(
            subscriptionType = "channel.poll.progress",
            event = """
            {
              "id": "p",
              "title": "Pizza?",
              "choices": [
                {"id": "c1", "title": "Yes", "bits_votes": 0, "channel_points_votes": 2, "votes": 7},
                {"id": "c2", "title": "No", "bits_votes": 0, "channel_points_votes": 0, "votes": 3}
              ],
              "ends_at": "2026-09-06T10:05:00.000Z"
            }
            """
        ))
        eventSub.handleMessage(messageText = notification(
            subscriptionType = "channel.poll.end",
            event = """
            {
              "id": "p",
              "title": "Pizza?",
              "choices": [
                {"id": "c1", "title": "Yes", "votes": 8},
                {"id": "c2", "title": "No", "votes": 3}
              ],
              "status": "completed",
              "ended_at": "2026-09-06T10:05:00.000Z"
            }
            """
        ))
        assertEquals(listOf("begin", "progress", "end"), delegate.polls.map { it.first })
        assertEquals(listOf("p", "p", "p"), delegate.polls.map { it.second.id })
        assertEquals("Pizza?", delegate.polls[0].second.title)
        assertEquals(listOf("Yes", "No"), delegate.polls[0].second.choices.map { it.title })
        assertEquals(listOf<Int?>(null, null), delegate.polls[0].second.choices.map { it.votes })
        assertEquals("2026-09-06T10:05:00.000Z", delegate.polls[0].second.ends_at)
        assertNull(delegate.polls[0].second.status)
        assertEquals(listOf<Int?>(7, 3), delegate.polls[1].second.choices.map { it.votes })
        assertEquals(listOf<Int?>(8, 3), delegate.polls[2].second.choices.map { it.votes })
        assertEquals("completed", delegate.polls[2].second.status)
        assertNull(delegate.polls[2].second.ends_at)
    }

    @Test
    fun predictionBeginProgressLockEnd() {
        val delegate = EventSubTestDelegate()
        val eventSub = makeEventSub(delegate)
        eventSub.handleMessage(messageText = notification(
            subscriptionType = "channel.prediction.begin",
            event = """
            {
              "id": "pr",
              "broadcaster_user_id": "111",
              "title": "Win?",
              "outcomes": [
                {"id": "o1", "title": "Yes", "color": "blue"},
                {"id": "o2", "title": "No", "color": "pink"}
              ],
              "started_at": "2026-09-06T10:00:00.000Z",
              "locks_at": "2026-09-06T10:02:00.000Z"
            }
            """
        ))
        eventSub.handleMessage(messageText = notification(
            subscriptionType = "channel.prediction.progress",
            event = """
            {
              "id": "pr",
              "title": "Win?",
              "outcomes": [
                {"id": "o1", "title": "Yes", "color": "blue", "users": 4, "channel_points": 900,
                 "top_predictors": []},
                {"id": "o2", "title": "No", "color": "pink", "users": 1, "channel_points": 100,
                 "top_predictors": []}
              ],
              "locks_at": "2026-09-06T10:02:00.000Z"
            }
            """
        ))
        eventSub.handleMessage(messageText = notification(
            subscriptionType = "channel.prediction.lock",
            event = """
            {
              "id": "pr",
              "title": "Win?",
              "outcomes": [
                {"id": "o1", "title": "Yes", "color": "blue", "users": 5, "channel_points": 1000},
                {"id": "o2", "title": "No", "color": "pink", "users": 1, "channel_points": 100}
              ],
              "locked_at": "2026-09-06T10:02:00.000Z"
            }
            """
        ))
        eventSub.handleMessage(messageText = notification(
            subscriptionType = "channel.prediction.end",
            event = """
            {
              "id": "pr",
              "title": "Win?",
              "winning_outcome_id": "o1",
              "outcomes": [
                {"id": "o1", "title": "Yes", "color": "blue", "users": 5, "channel_points": 1000},
                {"id": "o2", "title": "No", "color": "pink", "users": 1, "channel_points": 100}
              ],
              "status": "resolved",
              "ended_at": "2026-09-06T10:10:00.000Z"
            }
            """
        ))
        assertEquals(listOf("begin", "progress", "lock", "end"), delegate.predictions.map { it.first })
        assertEquals("Win?", delegate.predictions[0].second.title)
        assertEquals(listOf("blue", "pink"), delegate.predictions[0].second.outcomes.map { it.color })
        assertEquals(listOf<Int?>(null, null), delegate.predictions[0].second.outcomes.map { it.users })
        assertEquals("2026-09-06T10:02:00.000Z", delegate.predictions[0].second.locks_at)
        assertNull(delegate.predictions[0].second.winning_outcome_id)
        assertEquals(listOf<Int?>(4, 1), delegate.predictions[1].second.outcomes.map { it.users })
        assertEquals(listOf<Int?>(900, 100), delegate.predictions[1].second.outcomes.map { it.channel_points })
        assertNull(delegate.predictions[2].second.locks_at)
        assertEquals("o1", delegate.predictions[3].second.winning_outcome_id)
        assertEquals("resolved", delegate.predictions[3].second.status)
    }

    @Test
    fun moderateRaid() {
        val delegate = EventSubTestDelegate()
        makeEventSub(delegate).handleMessage(messageText = notification(
            subscriptionType = "channel.moderate",
            event = """
            {
              "broadcaster_user_id": "111",
              "moderator_user_id": "111",
              "action": "raid",
              "raid": {"user_id": "555", "user_login": "target", "user_name": "Target", "viewer_count": 12},
              "unraid": null,
              "ban": null,
              "timeout": null
            }
            """
        ))
        assertEquals(1, delegate.moderates.size)
        assertEquals("raid", delegate.moderates.firstOrNull()?.action)
        assertEquals("target", delegate.moderates.firstOrNull()?.raid?.user_login)
        assertEquals("Target", delegate.moderates.firstOrNull()?.raid?.user_name)
    }

    @Test
    fun moderateOtherAction() {
        val delegate = EventSubTestDelegate()
        makeEventSub(delegate).handleMessage(messageText = notification(
            subscriptionType = "channel.moderate",
            event = """
            {
              "broadcaster_user_id": "111",
              "moderator_user_id": "111",
              "action": "ban",
              "raid": null,
              "ban": {"user_id": "666", "user_login": "bad", "user_name": "Bad", "reason": ""}
            }
            """
        ))
        assertEquals(1, delegate.moderates.size)
        assertEquals("ban", delegate.moderates.firstOrNull()?.action)
        assertNull(delegate.moderates.firstOrNull()?.raid)
    }

    @Test
    fun subWithChatterColorAndBadges() {
        val delegate = EventSubTestDelegate()
        makeEventSub(delegate).handleMessage(messageText = chatNotification(
            noticeType = "sub",
            shared = false,
            payload = "\"sub\": {\"sub_tier\": \"1000\", \"is_prime\": true, \"duration_months\": 1}",
            color = "#FF0000",
            badges = """[{"set_id": "subscriber", "id": "12", "info": "12"}, {"set_id": "vip", "id": "1", "info": ""}]"""
        ))
        assertEquals(1, delegate.subscribes.size)
        assertEquals(true, delegate.subscribes.firstOrNull()?.isPrime())
        assertEquals("#FF0000", delegate.subscribes.firstOrNull()?.chatter?.color)
        assertEquals(listOf("subscriber", "vip"), delegate.subscribes.firstOrNull()?.chatter?.badges?.map { it.set_id })
        assertEquals(listOf("12", "1"), delegate.subscribes.firstOrNull()?.chatter?.badges?.map { it.id })
    }

    @Test
    fun subWithMissingPayloadIsIgnored() {
        val delegate = EventSubTestDelegate()
        makeEventSub(delegate).handleMessage(messageText = chatNotification(
            noticeType = "sub",
            shared = false,
            payload = ""
        ))
        assertEquals(0, delegate.subscribes.size)
        assertEquals(1, delegate.notifications.size)
    }

    @Test
    fun resub() {
        val delegate = EventSubTestDelegate()
        makeEventSub(delegate).handleMessage(messageText = chatNotification(
            noticeType = "resub",
            shared = false,
            payload = """
            "resub": {
              "cumulative_months": 12,
              "duration_months": 1,
              "streak_months": null,
              "sub_tier": "3000",
              "is_prime": false,
              "is_gift": false
            }
            """
        ))
        assertEquals(1, delegate.resubscribes.size)
        assertEquals("Viewer", delegate.resubscribes.firstOrNull()?.user_name)
        assertEquals(12, delegate.resubscribes.firstOrNull()?.cumulative_months)
        assertNull(delegate.resubscribes.firstOrNull()?.streak_months)
        assertEquals(3, delegate.resubscribes.firstOrNull()?.tierAsNumber())
        assertNull(delegate.resubscribes.firstOrNull()?.sharedChat)
    }

    @Test
    fun subGift() {
        val delegate = EventSubTestDelegate()
        makeEventSub(delegate).handleMessage(messageText = chatNotification(
            noticeType = "sub_gift",
            shared = false,
            payload = """
            "sub_gift": {
              "duration_months": 3,
              "cumulative_total": 5,
              "recipient_user_id": "4",
              "recipient_user_name": "R",
              "recipient_user_login": "r",
              "sub_tier": "2000",
              "community_gift_id": null
            }
            """
        ))
        assertEquals(1, delegate.gifts.size)
        assertEquals("Viewer", delegate.gifts.firstOrNull()?.user_name)
        assertEquals(1, delegate.gifts.firstOrNull()?.total)
        assertEquals(2, delegate.gifts.firstOrNull()?.tierAsNumber())
        assertNull(delegate.gifts.firstOrNull()?.sharedChat)
    }

    @Test
    fun subGiftPartOfCommunityGiftIsIgnored() {
        val delegate = EventSubTestDelegate()
        makeEventSub(delegate).handleMessage(messageText = chatNotification(
            noticeType = "sub_gift",
            shared = false,
            payload = """
            "sub_gift": {
              "duration_months": 1,
              "cumulative_total": 1,
              "recipient_user_id": "4",
              "recipient_user_name": "R",
              "recipient_user_login": "r",
              "sub_tier": "1000",
              "community_gift_id": "g"
            }
            """
        ))
        assertEquals(0, delegate.gifts.size)
    }

    @Test
    fun communitySubGift() {
        val delegate = EventSubTestDelegate()
        makeEventSub(delegate).handleMessage(messageText = chatNotification(
            noticeType = "community_sub_gift",
            shared = false,
            payload = "\"community_sub_gift\": {\"id\": \"g\", \"total\": 20, \"sub_tier\": \"3000\", \"cumulative_total\": 40}"
        ))
        assertEquals(1, delegate.gifts.size)
        assertEquals("Viewer", delegate.gifts.firstOrNull()?.user_name)
        assertEquals(20, delegate.gifts.firstOrNull()?.total)
        assertEquals(3, delegate.gifts.firstOrNull()?.tierAsNumber())
    }

    @Test
    fun anonymousCommunitySubGift() {
        val delegate = EventSubTestDelegate()
        makeEventSub(delegate).handleMessage(messageText = chatNotification(
            noticeType = "community_sub_gift",
            shared = false,
            payload = "\"community_sub_gift\": {\"id\": \"g\", \"total\": 5, \"sub_tier\": \"1000\", \"cumulative_total\": null}",
            chatterUserName = "null",
            chatterIsAnonymous = true,
            badges = """[{"set_id": "vip", "id": "1", "info": ""}]"""
        ))
        assertEquals(1, delegate.gifts.size)
        assertNull(delegate.gifts.firstOrNull()?.user_name)
        assertEquals(5, delegate.gifts.firstOrNull()?.total)
        assertNull(delegate.gifts.firstOrNull()?.chatter)
    }

    @Test
    fun anonymousSubHasEmptyUserName() {
        val delegate = EventSubTestDelegate()
        makeEventSub(delegate).handleMessage(messageText = chatNotification(
            noticeType = "sub",
            shared = false,
            payload = "\"sub\": {\"sub_tier\": \"1000\", \"is_prime\": false, \"duration_months\": 1}",
            chatterUserName = "null",
            chatterIsAnonymous = true
        ))
        assertEquals(1, delegate.subscribes.size)
        assertEquals("", delegate.subscribes.firstOrNull()?.user_name)
        assertNull(delegate.subscribes.firstOrNull()?.chatter)
    }

    @Test
    fun primePaidUpgrade() {
        val delegate = EventSubTestDelegate()
        makeEventSub(delegate).handleMessage(messageText = chatNotification(
            noticeType = "prime_paid_upgrade",
            shared = false,
            payload = "\"prime_paid_upgrade\": {\"sub_tier\": \"2000\"}"
        ))
        assertEquals(1, delegate.upgrades.size)
        assertEquals("Viewer", delegate.upgrades.firstOrNull()?.user_name)
        assertEquals(2, delegate.upgrades.firstOrNull()?.tierAsNumber())
        assertEquals("hello", delegate.upgrades.firstOrNull()?.message?.text)
        assertNull(delegate.upgrades.firstOrNull()?.sharedChat)
    }

    @Test
    fun sharedChatPrimePaidUpgrade() {
        val delegate = EventSubTestDelegate()
        makeEventSub(delegate).handleMessage(messageText = chatNotification(
            noticeType = "shared_chat_prime_paid_upgrade",
            shared = true,
            payload = "\"shared_chat_prime_paid_upgrade\": {\"sub_tier\": \"1000\"}"
        ))
        assertEquals(1, delegate.upgrades.size)
        assertEquals(1, delegate.upgrades.firstOrNull()?.tierAsNumber())
        assertEquals("Partner", delegate.upgrades.firstOrNull()?.sharedChat?.broadcasterUserName)
    }

    @Test
    fun giftPaidUpgrade() {
        val delegate = EventSubTestDelegate()
        makeEventSub(delegate).handleMessage(messageText = chatNotification(
            noticeType = "gift_paid_upgrade",
            shared = false,
            payload = """
            "gift_paid_upgrade": {
              "gifter_is_anonymous": false,
              "gifter_user_id": "9",
              "gifter_user_name": "G",
              "gifter_user_login": "g"
            }
            """
        ))
        assertEquals(1, delegate.upgrades.size)
        assertEquals("Viewer", delegate.upgrades.firstOrNull()?.user_name)
        assertNull(delegate.upgrades.firstOrNull()?.tier)
        assertNull(delegate.upgrades.firstOrNull()?.tierAsNumber())
    }

    @Test
    fun watchStreak() {
        val delegate = EventSubTestDelegate()
        makeEventSub(delegate).handleMessage(messageText = chatNotification(
            noticeType = "watch_streak",
            shared = false,
            payload = "\"watch_streak\": {\"streak_count\": 25}",
            message = """{"text": "streak!", "fragments": []}"""
        ))
        assertEquals(1, delegate.watchStreaks.size)
        assertEquals("Viewer", delegate.watchStreaks.firstOrNull()?.user_name)
        assertEquals(25, delegate.watchStreaks.firstOrNull()?.streak_count)
        assertEquals("streak!", delegate.watchStreaks.firstOrNull()?.message?.text)
        assertEquals("", delegate.watchStreaks.firstOrNull()?.chatter?.color)
    }

    @Test
    fun unknownNoticeTypeIsIgnored() {
        val delegate = EventSubTestDelegate()
        val message = chatNotification(
            noticeType = "announcement",
            shared = false,
            payload = "\"announcement\": {\"color\": \"PRIMARY\"}"
        )
        makeEventSub(delegate).handleMessage(messageText = message)
        assertEquals(0, delegate.subscribes.size)
        assertEquals(0, delegate.resubscribes.size)
        assertEquals(0, delegate.gifts.size)
        assertEquals(0, delegate.upgrades.size)
        assertEquals(0, delegate.raids.size)
        assertEquals(0, delegate.watchStreaks.size)
        assertEquals(listOf(message), delegate.notifications)
    }

    @Test
    fun sharedChatSourceRequiresBothIdAndName() {
        val delegate = EventSubTestDelegate()
        makeEventSub(delegate).handleMessage(messageText = chatNotification(
            noticeType = "sub",
            shared = false,
            payload = "\"sub\": {\"sub_tier\": \"1000\", \"is_prime\": false, \"duration_months\": 1}"
        ).replace("\"source_broadcaster_user_id\": null", "\"source_broadcaster_user_id\": \"222\""))
        assertEquals(1, delegate.subscribes.size)
        assertNull(delegate.subscribes.firstOrNull()?.sharedChat)
    }
}

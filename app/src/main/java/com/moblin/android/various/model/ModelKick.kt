package com.moblin.android.various.model

import androidx.compose.ui.graphics.Color
import com.moblin.android.common.various.RgbColor
import com.moblin.android.common.various.countFormatter
import com.moblin.android.localized
import com.moblin.android.streamingplatforms.kick.KickApi
import com.moblin.android.streamingplatforms.kick.KickApiDelegate
import com.moblin.android.streamingplatforms.kick.KickCategory
import com.moblin.android.streamingplatforms.kick.KickLiveSearchChannel
import com.moblin.android.streamingplatforms.kick.KickPlatformStatus
import com.moblin.android.streamingplatforms.kick.KickPusher
import com.moblin.android.streamingplatforms.kick.KickPusherDelegate
import com.moblin.android.streamingplatforms.kick.KickPusherGiftedSubscriptionsEvent
import com.moblin.android.streamingplatforms.kick.KickPusherKicksGiftedEvent
import com.moblin.android.streamingplatforms.kick.KickPusherRewardRedeemedEvent
import com.moblin.android.streamingplatforms.kick.KickPusherStreamHostEvent
import com.moblin.android.streamingplatforms.kick.KickPusherSubscriptionEvent
import com.moblin.android.streamingplatforms.kick.KickPusherUserBannedEvent
import com.moblin.android.streamingplatforms.kick.KickStreamInfo
import com.moblin.android.streamingplatforms.kick.getKickChannelInfo
import com.moblin.android.streamingplatforms.kick.removeKickAccessTokenInKeychain
import com.moblin.android.streamingplatforms.kick.storeKickAccessTokenInKeychain
import com.moblin.android.various.ChatHighlight
import com.moblin.android.various.ChatHighlightKind
import com.moblin.android.various.ChatPostSegment
import com.moblin.android.various.makeChatPostTextSegments
import com.moblin.android.various.network.NetworkResponse
import com.moblin.android.various.network.OperationResult
import com.moblin.android.various.settings.MacroEvent
import com.moblin.android.various.settings.SettingsMacrosEvent
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.view.controlbar.quickbutton.chat.ChatterInfo
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun Model.updateViewersKick(): StreamingPlatformStatus {
    val platformStatus = kickPlatformStatus?.platformStatus
    return if (platformStatus != null) {
        StreamingPlatformStatus(platform = TODO("no Android counterpart for Platform.kick"), status = platformStatus)
    } else {
        StreamingPlatformStatus(platform = TODO("no Android counterpart for Platform.kick"), status = PlatformStatus.unknown)
    }
}

fun Model.kickLogin(stream: SettingsStream, onComplete: (() -> Unit)? = null) {
    kickAuthOnComplete = { accessToken ->
        storeKickAccessTokenInKeychain(streamId = stream.id, accessToken = accessToken)
        stream.kickLoggedIn = true
        stream.kickWantsToBeLoggedIn = true
        stream.kickNotLoggedInCount = 0
        stream.kickAccessToken = accessToken
        createStreamWizard.showKickAuth = false
        createKickApi(stream = stream).getUser { userData ->
            if (userData == null) {
                onComplete?.invoke()
                return@getUser
            }
            stream.kickChannelName = userData.username
            getKickChannelInfo(channelName = userData.username) { channelInfo ->
                if (channelInfo != null) {
                    stream.kickChannelId = channelInfo.chatroom.id.toString()
                    stream.kickSlug = channelInfo.slug
                    stream.kickChatroomChannelId = channelInfo.chatroom.channel_id.toString()
                }
                if (stream.enabled) {
                    kickAccessTokenUpdated()
                }
                onComplete?.invoke()
            }
        }
    }
}

fun Model.kickLogout(stream: SettingsStream) {
    stream.kickAccessToken = ""
    removeKickAccessTokenInKeychain(streamId = stream.id)
    stream.kickLoggedIn = false
    stream.kickWantsToBeLoggedIn = false
    stream.kickChannelName = ""
    stream.kickChannelId = null
    stream.kickSlug = null
    stream.kickChatroomChannelId = null
    if (stream.enabled) {
        kickAccessTokenUpdated()
    }
}

fun Model.isKickPusherConfigured(): Boolean {
    return database.chat.enabled && stream.value.kickChannelName != ""
}

fun Model.isKickPusherConnected(): Boolean {
    return kickPusher?.isConnected() ?: false
}

fun Model.hasKickPusherEmotes(): Boolean {
    return kickPusher?.hasEmotes() ?: false
}

fun Model.isKickViewersConfigured(): Boolean {
    return stream.value.kickChannelName != ""
}

fun Model.reloadKickViewers() {
    kickPlatformStatus?.stop()
    if (isKickViewersConfigured()) {
        kickPlatformStatus = KickPlatformStatus().also {
            it.start(channelName = stream.value.kickChannelName)
        }
    }
}

fun Model.reloadKickPusher() {
    kickPusher?.stop()
    kickPusher = null
    setTextToSpeechStreamerMentions()
    val channelId = stream.value.kickChannelId
    val chatroomChannelId = stream.value.kickChatroomChannelId
    if (isKickPusherConfigured() &&
        !isRemoteControlChatAndEvents(platform = TODO("no Android counterpart for Platform.kick")) &&
        channelId != null &&
        chatroomChannelId != null
    ) {
        val pusher = KickPusher(
            context = TODO("no Android counterpart for context"),
            delegate = KickPusherDelegateAdapter(this),
            channelName = stream.value.kickChannelName,
            channelId = channelId,
            chatroomChannelId = chatroomChannelId,
            settings = stream.value.chat
        )
        pusher.start()
        kickPusher = pusher
    }
    updateChatMoreThanOneChatConfigured()
}

fun Model.kickChannelNameUpdated() {
    reloadViewers()
    reloadKickPusher()
    reloadKickViewers()
    resetChat()
}

fun Model.kickAccessTokenUpdated() {
    reloadViewers()
    reloadKickPusher()
    reloadKickViewers()
    resetChat()
}

fun Model.updateKickChannelInfoIfNeeded() {
    if (stream.value.kickChannelName.isEmpty()) {
        return
    }
    if (stream.value.kickChannelId != null && stream.value.kickSlug != null && stream.value.kickChatroomChannelId != null) {
        return
    }
    getKickChannelInfo(channelName = stream.value.kickChannelName) { channelInfo ->
        if (channelInfo != null) {
            stream.value.kickChannelId = channelInfo.chatroom.id.toString()
            stream.value.kickSlug = channelInfo.slug
            stream.value.kickChatroomChannelId = channelInfo.chatroom.channel_id.toString()
        }
        kickChannelNameUpdated()
    }
}

fun Model.makeNotLoggedInToKickToastIfNeeded() {
    if (!stream.value.kickWantsToBeLoggedIn || stream.value.kickLoggedIn) {
        return
    }
    stream.value.kickNotLoggedInCount += 1
    if (stream.value.kickNotLoggedInCount >= maxNotLoggedInToastCount) {
        stream.value.kickWantsToBeLoggedIn = false
    }
    makeNotLoggedInToToast(platform = TODO("no Android counterpart for Platform.kick"))
}

fun Model.sendKickChatMessage(message: String) {
    createKickApi(stream = stream.value).sendMessage(message = message)
}

fun Model.banKickUser(
    user: String,
    duration: Int? = null,
    reason: String? = null,
    onComplete: (OperationResult) -> Unit
) {
    createKickApi(stream = stream.value).banUser(
        user = user,
        duration = duration,
        reason = reason,
        onComplete = onComplete
    )
}

fun Model.unbanKickUser(user: String, onComplete: (OperationResult) -> Unit) {
    createKickApi(stream = stream.value).unbanUser(user = user, onComplete = onComplete)
}

fun Model.modKickUser(user: String, onComplete: (OperationResult) -> Unit) {
    createKickApi(stream = stream.value).addModerator(user = user, onComplete = onComplete)
}

fun Model.unmodKickUser(user: String, onComplete: (OperationResult) -> Unit) {
    createKickApi(stream = stream.value).removeModerator(user = user, onComplete = onComplete)
}

fun Model.vipKickUser(user: String, onComplete: (OperationResult) -> Unit) {
    createKickApi(stream = stream.value).addVip(user = user, onComplete = onComplete)
}

fun Model.unvipKickUser(user: String, onComplete: (OperationResult) -> Unit) {
    createKickApi(stream = stream.value).removeVip(user = user, onComplete = onComplete)
}

fun Model.hostKickChannel(channel: String, onComplete: (OperationResult) -> Unit) {
    createKickApi(stream = stream.value).hostChannel(channel = channel, onComplete = onComplete)
}

fun Model.searchKickChannels(query: String, onComplete: (List<KickLiveSearchChannel>?) -> Unit) {
    kickSearchTimerScope.launch {
        delay(500)
        createKickApi(stream = stream.value).searchLiveChannels(query = query, onComplete = onComplete)
    }
}

fun Model.enableKickSlowMode(messageInterval: Int, onComplete: (OperationResult) -> Unit) {
    createKickApi(stream = stream.value).enableSlowMode(messageInterval = messageInterval, onComplete = onComplete)
}

fun Model.disableKickSlowMode(onComplete: (OperationResult) -> Unit) {
    createKickApi(stream = stream.value).disableSlowMode(onComplete = onComplete)
}

fun Model.enableKickFollowersMode(followingMinDuration: Int, onComplete: (OperationResult) -> Unit) {
    createKickApi(stream = stream.value).enableFollowersMode(
        minimumDuration = followingMinDuration,
        onComplete = onComplete
    )
}

fun Model.disableKickFollowersMode(onComplete: (OperationResult) -> Unit) {
    createKickApi(stream = stream.value).disableFollowersMode(onComplete = onComplete)
}

fun Model.setKickEmoteOnlyMode(enabled: Boolean, onComplete: (OperationResult) -> Unit) {
    createKickApi(stream = stream.value).setEmoteOnlyMode(enabled = enabled, onComplete = onComplete)
}

fun Model.setKickSubscribersOnlyMode(enabled: Boolean, onComplete: (OperationResult) -> Unit) {
    createKickApi(stream = stream.value).setSubscribersOnlyMode(enabled = enabled, onComplete = onComplete)
}

fun Model.setKickShowViewCount(enabled: Boolean, onComplete: (OperationResult) -> Unit) {
    createKickApi(stream = stream.value).setShowViewCount(
        channelId = stream.value.kickChatroomChannelId ?: "",
        enabled = enabled,
        onComplete = onComplete
    )
}

fun Model.createKickPoll(
    title: String,
    options: List<String>,
    duration: Int,
    resultDisplayDuration: Int,
    onComplete: (OperationResult) -> Unit
) {
    createKickApi(stream = stream.value).createPoll(
        title = title,
        options = options,
        duration = duration,
        resultDisplayDuration = resultDisplayDuration,
        onComplete = onComplete
    )
}

fun Model.deleteKickPoll(onComplete: (OperationResult) -> Unit) {
    createKickApi(stream = stream.value).deletePoll {
        onComplete(it)
    }
}

fun Model.createKickPrediction(
    title: String,
    outcomes: List<String>,
    duration: Int,
    onComplete: (OperationResult) -> Unit
) {
    createKickApi(stream = stream.value).createPrediction(
        title = title,
        outcomes = outcomes,
        duration = duration,
        onComplete = onComplete
    )
}

fun Model.deleteKickMessage(messageId: String) {
    createKickApi(stream = stream.value).deleteMessage(messageId = messageId)
}

fun Model.getKickStreamInfo(
    stream: SettingsStream,
    onComplete: (NetworkResponse<KickStreamInfo>) -> Unit
) {
    createKickApi(stream = stream).getStreamInfo(onComplete = onComplete)
}

fun Model.setKickStreamTitle(
    stream: SettingsStream,
    title: String,
    onComplete: (OperationResult) -> Unit
) {
    createKickApi(stream = stream).setStreamTitle(title = title, onComplete = onComplete)
}

fun Model.searchKickCategories(
    stream: SettingsStream,
    query: String,
    onComplete: (List<KickCategory>?) -> Unit
) {
    kickSearchTimerScope.launch {
        delay(500)
        createKickApi(stream = stream).searchCategories(query = query, onComplete = onComplete)
    }
}

fun Model.fetchKickCategories(
    stream: SettingsStream,
    query: String,
    onComplete: (List<KickCategory>?) -> Unit
) {
    createKickApi(stream = stream).searchCategories(query = query, onComplete = onComplete)
}

fun Model.setKickStreamCategory(stream: SettingsStream, categoryId: Int) {
    createKickApi(stream = stream).setStreamCategory(categoryId = categoryId) {
        if (!it.isSuccessful()) {
            makeErrorToast(title = "Failed to set stream category")
        }
    }
}

fun Model.getKickChatterInfo(user: String, onComplete: (ChatterInfo?) -> Unit) {
    createKickApi(stream = stream.value).getChatterInfo(user = user) { chatterInfo ->
        if (chatterInfo == null) {
            onComplete(null)
            return@getChatterInfo
        }
        getKickChannelInfo(channelName = user) { channelInfo ->
            val accountCreated = channelInfo?.chatroom?.created_at
            val bio = channelInfo?.user?.bio
            val followers = channelInfo?.followersCount
            onComplete(
                chatterInfo.toChatterInfo(
                    accountCreated = accountCreated,
                    bio = bio,
                    followers = followers
                )
            )
        }
    }
}

fun Model.createKickApi(stream: SettingsStream): KickApi {
    val kickApi = KickApi(
        channelId = stream.kickChannelId ?: "",
        slug = stream.kickSlug ?: "",
        accessToken = stream.kickAccessToken
    )
    kickApi.delegate = KickApiDelegateAdapter(this)
    return kickApi
}

private fun Model.appendKickChatAlertMessage(
    user: String,
    text: String,
    title: String,
    color: Color,
    image: String,
    kind: ChatHighlightKind
) {
    var id = 0
    appendChatMessage(
        platform = TODO("no Android counterpart for Platform.kick"),
        messageId = null,
        displayName = user,
        user = user,
        userId = null,
        userColor = null,
        userBadges = emptyList(),
        segments = makeChatPostTextSegments(text = text, id = id).first,
        timestamp = statusOther.digitalClock.value,
        timestampTime = Instant.now(),
        isAction = false,
        isSubscriber = false,
        isModerator = false,
        isOwner = false,
        bits = null,
        highlight = ChatHighlight(
            kind = kind,
            barColor = color,
            image = image,
            titleSegments = listOf(ChatPostSegment(id = 0, text = title))
        ),
        live = true
    )
}

fun Model.kickPusherMakeErrorToast(title: String, subTitle: String?) {
    makeErrorToast(title = title, subTitle = subTitle)
}

fun Model.kickPusherAppendMessage(
    messageId: String?,
    user: String,
    userId: String?,
    userColor: RgbColor?,
    userBadges: List<String>,
    segments: List<ChatPostSegment>,
    isSubscriber: Boolean,
    isModerator: Boolean,
    highlight: ChatHighlight?
) {
    appendChatMessage(
        platform = TODO("no Android counterpart for Platform.kick"),
        messageId = messageId,
        displayName = user,
        user = user,
        userId = userId,
        userColor = userColor,
        userBadges = userBadges,
        segments = segments,
        timestamp = statusOther.digitalClock.value,
        timestampTime = Instant.now(),
        isAction = false,
        isSubscriber = isSubscriber,
        isModerator = isModerator,
        isOwner = false,
        bits = null,
        highlight = highlight,
        live = true
    )
}

fun Model.kickPusherDeleteMessage(messageId: String) {
    deleteChatMessage(messageId = messageId)
}

fun Model.kickPusherDeleteUser(userId: String) {
    deleteChatUser(userId = userId)
}

fun Model.kickPusherSubscription(event: KickPusherSubscriptionEvent) {
    val text = localized("just subscribed! They've been subscribed for ${event.months} months!")
    if (stream.value.kickToastAlerts.subscriptions) {
        makeToast(title = "🎉 ${event.username} $text")
    }
    if (stream.value.kickChatAlerts.subscriptions) {
        appendKickChatAlertMessage(
            user = event.username,
            text = text,
            title = localized("New subscriber"),
            color = Color.Cyan,
            image = "party.popper",
            kind = TODO("no Android counterpart for ChatHighlightKind.other")
        )
    }
    playAlert(alert = TODO("no Android counterpart for Alert.kickSubscription"))
    printEventCatPrinters(
        event = TODO("no Android counterpart for EventCatPrinter.kickSubscription"),
        username = event.username,
        message = text
    )
    macrosEventOccurred(
        MacroEvent(event = TODO("no Android counterpart for SettingsMacrosEvent.kickSubscription"), amount = event.months)
    )
    latestSubscriber = event.username
}

fun Model.kickPusherGiftedSubscription(event: KickPusherGiftedSubscriptionsEvent) {
    val user = event.gifter_username
    val text = localized(
        "just gifted ${event.gifted_usernames.size} subscription(s)! " +
            "They've gifted ${event.gifter_total} in total!"
    )
    if (stream.value.kickToastAlerts.giftedSubscriptions) {
        makeToast(title = "🎁 $user $text")
    }
    if (stream.value.kickChatAlerts.giftedSubscriptions) {
        appendKickChatAlertMessage(
            user = user,
            text = text,
            title = localized("Gift subscriptions"),
            color = Color.Cyan,
            image = "gift",
            kind = TODO("no Android counterpart for ChatHighlightKind.other")
        )
    }
    playAlert(alert = TODO("no Android counterpart for Alert.kickGiftedSubscriptions"))
    printEventCatPrinters(
        event = TODO("no Android counterpart for EventCatPrinter.kickGiftedSubscriptions"),
        username = user,
        message = text
    )
    macrosEventOccurred(
        MacroEvent(
            event = TODO("no Android counterpart for SettingsMacrosEvent.kickGiftSubscriptions"),
            amount = event.gifted_usernames.size
        )
    )
    latestSubscriber = user
}

fun Model.kickPusherRewardRedeemed(event: KickPusherRewardRedeemedEvent) {
    val user = event.username
    val baseText = localized("redeemed ${event.reward_title}")
    val text = if (event.user_input.isEmpty()) baseText else "$baseText: ${event.user_input}"
    if (stream.value.kickToastAlerts.rewards) {
        makeToast(title = "🎁 $user $text")
    }
    if (stream.value.kickChatAlerts.rewards) {
        appendKickChatAlertMessage(
            user = user,
            text = text,
            title = localized("Reward Redeemed"),
            color = Color.Green,
            image = "medal.star",
            kind = TODO("no Android counterpart for ChatHighlightKind.other")
        )
    }
    playAlert(alert = TODO("no Android counterpart for Alert.kickReward"))
    printEventCatPrinters(
        event = TODO("no Android counterpart for EventCatPrinter.kickReward"),
        username = user,
        message = text
    )
    macrosEventOccurred(
        MacroEvent(event = TODO("no Android counterpart for SettingsMacrosEvent.kickReward"), text = event.reward_title)
    )
}

fun Model.kickPusherStreamHost(event: KickPusherStreamHostEvent) {
    val user = event.host_username
    val text = localized("is now hosting with ${event.number_viewers} viewers!")
    if (stream.value.kickToastAlerts.hosts) {
        makeToast(title = "📺 $user $text")
    }
    if (stream.value.kickChatAlerts.hosts) {
        appendKickChatAlertMessage(
            user = user,
            text = text,
            title = localized("Host"),
            color = Color(0xFFFF9800),
            image = "person.3",
            kind = TODO("no Android counterpart for ChatHighlightKind.other")
        )
    }
    playAlert(alert = TODO("no Android counterpart for Alert.kickHost"))
    printEventCatPrinters(
        event = TODO("no Android counterpart for EventCatPrinter.kickHost"),
        username = user,
        message = text
    )
    macrosEventOccurred(
        MacroEvent(event = TODO("no Android counterpart for SettingsMacrosEvent.kickHost"), amount = event.number_viewers)
    )
}

fun Model.kickPusherUserBanned(event: KickPusherUserBannedEvent) {
    val text: String
    val title: String
    if (event.permanent) {
        text = localized("was banned from chat!")
        title = localized("User banned")
    } else {
        text = localized("was timed out from chat!")
        title = localized("User timed out")
    }
    if (stream.value.kickChatAlerts.bans) {
        appendKickChatAlertMessage(
            user = event.user.username,
            text = text,
            title = title,
            color = Color.Red,
            image = "nosign",
            kind = TODO("no Android counterpart for ChatHighlightKind.other")
        )
    }
}

fun Model.kickPusherKicksGifted(event: KickPusherKicksGiftedEvent) {
    val user = event.sender.username
    val amount = countFormatter.format(event.gift.amount)
    val text = localized("sent ${event.gift.name} 💎 $amount")
    val message = if (event.message.isEmpty()) text else "$text ${event.message}"
    if (stream.value.kickToastAlerts.isKicksEnabled(amount = event.gift.amount)) {
        makeToast(title = "$user $message")
    }
    if (stream.value.kickChatAlerts.isKicksEnabled(amount = event.gift.amount)) {
        appendKickChatAlertMessage(
            user = user,
            text = message,
            title = localized("Kicks"),
            color = Color.Green,
            image = "suit.diamond",
            kind = TODO("no Android counterpart for ChatHighlightKind.other")
        )
    }
    playAlert(alert = TODO("no Android counterpart for Alert.kickKicks"))
    printEventCatPrinters(
        event = TODO("no Android counterpart for EventCatPrinter.kickKicks"),
        username = user,
        message = message
    )
    macrosEventOccurred(
        MacroEvent(event = TODO("no Android counterpart for SettingsMacrosEvent.kickKicks"), amount = event.gift.amount)
    )
}

fun Model.kickApiUnauthorized() {
    if (!stream.value.kickLoggedIn) {
        return
    }
    stream.value.kickLoggedIn = false
    makeNotLoggedInToToast(platform = TODO("no Android counterpart for Platform.kick"))
}

private val kickSearchTimerScope = CoroutineScope(Dispatchers.Main)

private class KickApiDelegateAdapter(private val model: Model) : KickApiDelegate {
    override fun kickApiUnauthorized() {
        model.kickApiUnauthorized()
    }
}

private class KickPusherDelegateAdapter(private val model: Model) : KickPusherDelegate {
    override fun kickPusherMakeErrorToast(title: String, subTitle: String?) {
        model.kickPusherMakeErrorToast(title = title, subTitle = subTitle)
    }

    override fun kickPusherAppendMessage(
        messageId: String?,
        user: String,
        userId: String?,
        userColor: RgbColor?,
        userBadges: List<String>,
        segments: List<ChatPostSegment>,
        isSubscriber: Boolean,
        isModerator: Boolean,
        highlight: ChatHighlight?
    ) {
        model.kickPusherAppendMessage(
            messageId = messageId,
            user = user,
            userId = userId,
            userColor = userColor,
            userBadges = userBadges,
            segments = segments,
            isSubscriber = isSubscriber,
            isModerator = isModerator,
            highlight = highlight
        )
    }

    override fun kickPusherDeleteMessage(messageId: String) {
        model.kickPusherDeleteMessage(messageId = messageId)
    }

    override fun kickPusherDeleteUser(userId: String) {
        model.kickPusherDeleteUser(userId = userId)
    }

    override fun kickPusherSubscription(event: KickPusherSubscriptionEvent) {
        model.kickPusherSubscription(event = event)
    }

    override fun kickPusherGiftedSubscription(event: KickPusherGiftedSubscriptionsEvent) {
        model.kickPusherGiftedSubscription(event = event)
    }

    override fun kickPusherRewardRedeemed(event: KickPusherRewardRedeemedEvent) {
        model.kickPusherRewardRedeemed(event = event)
    }

    override fun kickPusherStreamHost(event: KickPusherStreamHostEvent) {
        model.kickPusherStreamHost(event = event)
    }

    override fun kickPusherUserBanned(event: KickPusherUserBannedEvent) {
        model.kickPusherUserBanned(event = event)
    }

    override fun kickPusherKicksGifted(event: KickPusherKicksGiftedEvent) {
        model.kickPusherKicksGifted(event = event)
    }
}

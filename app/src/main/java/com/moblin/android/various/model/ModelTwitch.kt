package com.moblin.android.various.model

import android.util.Log
import androidx.compose.ui.graphics.Color
import com.moblin.android.common.various.RgbColor
import com.moblin.android.common.various.countFormatter
import com.moblin.android.common.various.formatShortDuration
import com.moblin.android.common.various.uptimeFormatter
import com.moblin.android.localized
import com.moblin.android.streamingplatforms.twitch.*
import com.moblin.android.various.ChatHighlight
import com.moblin.android.various.ChatHighlightKind
import com.moblin.android.various.ChatPostSegment
import com.moblin.android.various.makeChatPostTextSegments
import com.moblin.android.various.network.NetworkResponse
import com.moblin.android.various.network.OperationResult
import com.moblin.android.various.settings.MacroEvent
import com.moblin.android.various.settings.SettingsMacrosEvent
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.various.settings.SettingsStreamTwitchReward
import com.moblin.android.various.settings.SettingsTwitchAlerts
import com.moblin.android.various.settings.appendTwitchRaidChannel
import com.moblin.android.various.settings.maxNotLoggedInToastCount
import java.time.Duration
import java.time.Instant
import kotlin.math.ceil
import kotlin.math.max
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark

private const val TAG = "Model"

fun Model.updateViewersTwitch(): StreamingPlatformStatus {
    return StreamingPlatformStatus(platform = StreamingPlatform.twitch, status = twitchPlatformStatus)
}

fun Model.isTwitchEventSubConfigured(): Boolean {
    return stream.twitchLoggedIn
}

fun Model.isTwitchEventsConnected(): Boolean {
    return twitchEventSub?.isConnected() ?: false
}

fun Model.isTwitchViewersConfigured(): Boolean {
    return stream.twitchChannelId != "" && stream.twitchLoggedIn
}

fun Model.isTwitchChatConfigured(): Boolean {
    return database.chat.enabled && stream.twitchChannelName != ""
}

fun Model.isTwitchChatConnected(): Boolean {
    return twitchChat?.isConnected() ?: false
}

fun Model.hasTwitchChatEmotes(): Boolean {
    return twitchChat?.hasEmotes() ?: false
}

fun Model.reloadTwitchChat() {
    twitchChat?.stop()
    setTextToSpeechStreamerMentions()
    if (isTwitchChatConfigured() && !isRemoteControlChatAndEvents(ChatPlatform.twitch)) {
        twitchChat?.start(
            channelName = stream.twitchChannelName,
            channelId = stream.twitchChannelId,
            settings = stream.chat,
            accessToken = stream.twitchAccessToken
        )
    }
    updateChatMoreThanOneChatConfigured()
}

fun Model.twitchChannelNameUpdated() {
    reloadViewers()
    reloadTwitchEventSub()
    reloadTwitchChat()
    resetChat()
}

fun Model.twitchChannelIdUpdated() {
    reloadViewers()
    reloadTwitchEventSub()
    reloadTwitchChat()
    resetChat()
}

fun Model.reloadTwitchEventSub() {
    twitchEventSub?.stop()
    twitchEventSub = null
    if (isTwitchEventSubConfigured()) {
        twitchEventSub = TwitchEventSub(
            remoteControl = useRemoteControlForChatAndEvents,
            userId = stream.twitchChannelId,
            accessToken = stream.twitchAccessToken,
            delegate = this
        )
        twitchEventSub!!.start()
    }
}

fun Model.fetchTwitchRewards() {
    createTwitchApi(stream)
        .getChannelPointsCustomRewards(stream.twitchChannelId) { rewards ->
            if (rewards == null) {
                Log.i(TAG, "Failed to get Twitch rewards")
                return@getChannelPointsCustomRewards
            }
            Log.i(TAG, "Twitch rewards: $rewards")
            stream.twitchRewards = rewards.data.map {
                val reward = SettingsStreamTwitchReward()
                reward.rewardId = it.id
                reward.title = it.title
                reward
            }
        }
}

fun Model.fetchTwitchGameId(
    stream: SettingsStream,
    name: String,
    onComplete: (String?) -> Unit
) {
    createTwitchApi(stream).getGames(listOf(name)) {
        onComplete(it?.firstOrNull()?.id)
    }
}

fun Model.searchTwitchCategories(
    stream: SettingsStream,
    filter: String,
    onComplete: (List<TwitchApiGameData>?) -> Unit
) {
    twitchSearchCategoriesTimer.startSingleShot(0.5) {
        createTwitchApi(stream).searchCategories(filter, onComplete)
    }
}

fun Model.fetchTwitchGames(
    stream: SettingsStream,
    names: List<String>,
    onComplete: (List<TwitchApiGameData>?) -> Unit
) {
    createTwitchApi(stream).getGames(names, onComplete)
}

fun Model.searchTwitchChannel(
    stream: SettingsStream,
    channelName: String,
    onComplete: (TwitchApiChannel?) -> Unit
) {
    createTwitchApi(stream).searchChannel(channelName, onComplete)
}

fun Model.searchTwitchChannels(
    stream: SettingsStream,
    filter: String,
    onComplete: (NetworkResponse<List<TwitchApiChannel>>) -> Unit
) {
    twitchSearchChannelsTimer.startSingleShot(0.5) {
        createTwitchApi(stream).searchChannels(
            filter = filter,
            liveOnly = true,
            onComplete = onComplete
        )
    }
}

fun Model.getTwitchFollowedStreams(
    stream: SettingsStream,
    onComplete: (NetworkResponse<List<TwitchApiStreamData>>) -> Unit
) {
    createTwitchApi(stream).getFollowedStreams(
        userId = stream.twitchChannelId,
        onComplete = onComplete
    )
}

fun Model.getTwitchStreams(
    stream: SettingsStream,
    userIds: List<String>,
    live: Boolean,
    onComplete: (List<TwitchApiStreamData>?) -> Unit
) {
    createTwitchApi(stream).getStreams(userIds = userIds, live = live, onComplete = onComplete)
}

fun Model.getTwitchUsers(
    stream: SettingsStream,
    userIds: List<String>,
    onComplete: (List<TwitchApiUser>?) -> Unit
) {
    createTwitchApi(stream).getUsersByIds(ids = userIds, onComplete = onComplete)
}

fun Model.getTwitchChannelInformation(
    stream: SettingsStream,
    onComplete: (TwitchApiChannelInformationData) -> Unit
) {
    createTwitchApi(stream).getChannelInformation(stream.twitchChannelId) { info ->
        if (info == null) {
            return@getChannelInformation
        }
        onComplete(info)
    }
}

fun Model.getTwitchTokenExpiresIn(
    stream: SettingsStream,
    onComplete: (Duration?) -> Unit
) {
    createTwitchApi(stream).validateToken { data ->
        onComplete(data?.let { Duration.ofSeconds(it.expires_in.toLong()) })
    }
}

fun Model.setTwitchStreamTitle(stream: SettingsStream, title: String) {
    createTwitchApi(stream).modifyChannelInformation(
        broadcasterId = stream.twitchChannelId,
        categoryId = null,
        title = title
    ) { }
}

fun Model.setTwitchStreamCategory(stream: SettingsStream, categoryId: String) {
    createTwitchApi(stream).modifyChannelInformation(
        broadcasterId = stream.twitchChannelId,
        categoryId = categoryId,
        title = null
    ) { }
}

fun Model.twitchLogin(
    stream: SettingsStream,
    onComplete: (() -> Unit)? = null,
    showWebBrowser: () -> Unit
) {
    twitchAuthOnComplete = { accessToken ->
        storeTwitchAccessTokenInKeychain(stream.id, accessToken)
        stream.twitchLoggedIn = true
        stream.twitchWantsToBeLoggedIn = true
        stream.twitchNotLoggedInCount = 0
        stream.twitchAccessToken = accessToken
        showTwitchAuth = false
        showModerationAuth = false
        createStreamWizard.showTwitchAuth = false
        TwitchApi(accessToken).getUserInfo { info ->
            if (info == null) {
                return@getUserInfo
            }
            stream.twitchChannelName = info.login
            stream.twitchChannelId = info.id
            if (stream.enabled) {
                twitchChannelIdUpdated()
            }
            onComplete?.invoke()
        }
    }
    twitchAuth.login(showWebBrowser)
}

fun Model.twitchLogout(stream: SettingsStream) {
    stream.twitchLoggedIn = false
    stream.twitchWantsToBeLoggedIn = false
    stream.twitchAccessToken = ""
    removeTwitchAccessTokenInKeychain(stream.id)
    if (stream.enabled) {
        reloadViewers()
        reloadTwitchEventSub()
        reloadChats()
    }
}

fun Model.handleTwitchAccessToken(accessToken: String) {
    twitchAuthOnComplete?.invoke(accessToken)
}

fun Model.makeNotLoggedInToTwitchToastIfNeeded() {
    if (!stream.twitchWantsToBeLoggedIn || stream.twitchLoggedIn) {
        return
    }
    stream.twitchNotLoggedInCount += 1
    if (stream.twitchNotLoggedInCount >= maxNotLoggedInToastCount) {
        stream.twitchWantsToBeLoggedIn = false
    }
    makeNotLoggedInToToast(ChatPlatform.twitch)
}

fun Model.createStreamMarker() {
    createTwitchApi(stream).createStreamMarker(stream.twitchChannelId) { data ->
        if (data != null) {
            makeToast(title = localized("Stream marker created"))
        } else {
            makeErrorToast(title = localized("Failed to create stream marker"))
        }
    }
}

fun Model.updateTwitchStream(monotonicNow: TimeMark) {
    if (!isLive || !isTwitchViewersConfigured()) {
        twitchPlatformStatus = StreamingPlatformStatus.Status.Unknown
        return
    }
    if (monotonicNow - twitchStreamUpdateTime <= 25.seconds) {
        return
    }
    twitchStreamUpdateTime = monotonicNow
    getStream()
}

fun Model.sendTwitchChatMessage(message: String, onComplete: (OperationResult) -> Unit) {
    createTwitchApi(stream).sendChatMessage(
        broadcasterId = stream.twitchChannelId,
        message = message,
        onComplete = onComplete
    )
}

fun Model.startAds(seconds: Int, onComplete: (OperationResult) -> Unit) {
    createTwitchApi(stream)
        .startCommercial(broadcasterId = stream.twitchChannelId, length = seconds) { result ->
            when (result) {
                is OperationResult.Success -> onComplete(OperationResult.Success(ByteArray(0)))
                is OperationResult.AuthError -> onComplete(OperationResult.AuthError)
                is OperationResult.Error -> onComplete(OperationResult.Error)
            }
        }
}

fun Model.banTwitchUser(
    user: String,
    userId: String,
    duration: Int?,
    reason: String? = null,
    onComplete: (OperationResult) -> Unit
) {
    createTwitchApi(stream).banUser(
        broadcasterId = stream.twitchChannelId,
        userId = userId,
        duration = duration,
        reason = reason,
        onComplete = onComplete
    )
}

fun Model.banTwitchUser(
    user: String,
    duration: Int?,
    reason: String?,
    onComplete: (OperationResult) -> Unit
) {
    createTwitchApi(stream).getUserByLogin(user) { twitchUser ->
        if (twitchUser == null) {
            onComplete(OperationResult.Error)
            return@getUserByLogin
        }
        banTwitchUser(
            user = user,
            userId = twitchUser.id,
            duration = duration,
            reason = reason,
            onComplete = onComplete
        )
    }
}

fun Model.unbanTwitchUser(user: String, onComplete: (OperationResult) -> Unit) {
    val twitchApi = createTwitchApi(stream)
    twitchApi.getUserByLogin(user) { twitchUser ->
        if (twitchUser == null) {
            onComplete(OperationResult.Error)
            return@getUserByLogin
        }
        twitchApi.unbanUser(
            broadcasterId = stream.twitchChannelId,
            userId = twitchUser.id,
            onComplete = onComplete
        )
    }
}

fun Model.modTwitchUser(user: String, onComplete: (OperationResult) -> Unit) {
    val twitchApi = createTwitchApi(stream)
    twitchApi.getUserByLogin(user) { twitchUser ->
        if (twitchUser == null) {
            onComplete(OperationResult.Error)
            return@getUserByLogin
        }
        twitchApi.addModerator(
            broadcasterId = stream.twitchChannelId,
            userId = twitchUser.id,
            onComplete = onComplete
        )
    }
}

fun Model.unmodTwitchUser(user: String, onComplete: (OperationResult) -> Unit) {
    val twitchApi = createTwitchApi(stream)
    twitchApi.getUserByLogin(user) { twitchUser ->
        if (twitchUser == null) {
            onComplete(OperationResult.Error)
            return@getUserByLogin
        }
        twitchApi.removeModerator(
            broadcasterId = stream.twitchChannelId,
            userId = twitchUser.id,
            onComplete = onComplete
        )
    }
}

fun Model.vipTwitchUser(user: String, onComplete: (OperationResult) -> Unit) {
    val twitchApi = createTwitchApi(stream)
    twitchApi.getUserByLogin(user) { twitchUser ->
        if (twitchUser == null) {
            onComplete(OperationResult.Error)
            return@getUserByLogin
        }
        twitchApi.addVip(
            broadcasterId = stream.twitchChannelId,
            userId = twitchUser.id,
            onComplete = onComplete
        )
    }
}

fun Model.unvipTwitchUser(user: String, onComplete: (OperationResult) -> Unit) {
    val twitchApi = createTwitchApi(stream)
    twitchApi.getUserByLogin(user) { twitchUser ->
        if (twitchUser == null) {
            onComplete(OperationResult.Error)
            return@getUserByLogin
        }
        twitchApi.removeVip(
            broadcasterId = stream.twitchChannelId,
            userId = twitchUser.id,
            onComplete = onComplete
        )
    }
}

fun Model.sendTwitchAnnouncement(
    message: String,
    color: String,
    onComplete: (OperationResult) -> Unit
) {
    createTwitchApi(stream).sendAnnouncement(
        broadcasterId = stream.twitchChannelId,
        message = message,
        color = color,
        onComplete = onComplete
    )
}

fun Model.setTwitchSlowMode(
    enabled: Boolean,
    duration: Int? = null,
    onComplete: (OperationResult) -> Unit
) {
    val settings = mutableMapOf<String, Any>("slow_mode" to enabled)
    if (enabled && duration != null) {
        settings["slow_mode_wait_time"] = duration
    }
    createTwitchApi(stream).updateChatSettings(
        broadcasterId = stream.twitchChannelId,
        settings = settings,
        onComplete = onComplete
    )
}

fun Model.setTwitchFollowersMode(
    enabled: Boolean,
    duration: Int? = null,
    onComplete: (OperationResult) -> Unit
) {
    val settings = mutableMapOf<String, Any>("follower_mode" to enabled)
    if (enabled && duration != null) {
        settings["follower_mode_duration"] = duration
    }
    createTwitchApi(stream).updateChatSettings(
        broadcasterId = stream.twitchChannelId,
        settings = settings,
        onComplete = onComplete
    )
}

fun Model.setTwitchEmoteOnlyMode(enabled: Boolean, onComplete: (OperationResult) -> Unit) {
    createTwitchApi(stream).updateChatSettings(
        broadcasterId = stream.twitchChannelId,
        settings = mapOf<String, Any>("emote_mode" to enabled),
        onComplete = onComplete
    )
}

fun Model.setTwitchSubscribersOnlyMode(enabled: Boolean, onComplete: (OperationResult) -> Unit) {
    createTwitchApi(stream).updateChatSettings(
        broadcasterId = stream.twitchChannelId,
        settings = mapOf<String, Any>("subscriber_mode" to enabled),
        onComplete = onComplete
    )
}

fun Model.deleteTwitchChatMessage(messageId: String) {
    createTwitchApi(stream)
        .deleteChatMessage(broadcasterId = stream.twitchChannelId, messageId = messageId) { }
}

fun Model.getTwitchPolls(onComplete: (NetworkResponse<List<TwitchApiPollData>>) -> Unit) {
    createTwitchApi(stream).getPolls(
        broadcasterId = stream.twitchChannelId,
        onComplete = onComplete
    )
}

fun Model.createTwitchPoll(
    title: String,
    choices: List<String>,
    duration: Int,
    onComplete: (OperationResult) -> Unit
) {
    createTwitchApi(stream).createPoll(
        broadcasterId = stream.twitchChannelId,
        title = title,
        choices = choices,
        duration = duration,
        onComplete = onComplete
    )
}

fun Model.endTwitchPoll(
    id: String,
    status: TwitchApiPollStatus,
    onComplete: (OperationResult) -> Unit
) {
    createTwitchApi(stream).endPoll(
        broadcasterId = stream.twitchChannelId,
        id = id,
        status = status,
        onComplete = onComplete
    )
}

fun Model.getTwitchPredictions(
    onComplete: (NetworkResponse<List<TwitchApiPredictionData>>) -> Unit
) {
    createTwitchApi(stream).getPredictions(
        broadcasterId = stream.twitchChannelId,
        onComplete = onComplete
    )
}

fun Model.createTwitchPrediction(
    title: String,
    outcomes: List<String>,
    predictionWindow: Int,
    onComplete: (OperationResult) -> Unit
) {
    createTwitchApi(stream).createPrediction(
        broadcasterId = stream.twitchChannelId,
        title = title,
        outcomes = outcomes,
        predictionWindow = predictionWindow,
        onComplete = onComplete
    )
}

fun Model.endTwitchPrediction(
    id: String,
    status: TwitchApiPredictionStatus,
    winningOutcomeId: String? = null,
    onComplete: (OperationResult) -> Unit
) {
    createTwitchApi(stream).endPrediction(
        broadcasterId = stream.twitchChannelId,
        id = id,
        status = status,
        winningOutcomeId = winningOutcomeId,
        onComplete = onComplete
    )
}

fun Model.startRaidTwitchChannel(
    channelId: String,
    onComplete: (OperationResult) -> Unit
) {
    createTwitchApi(stream).startRaid(
        broadcasterId = stream.twitchChannelId,
        toBroadcasterId = channelId,
        onComplete = onComplete
    )
}

fun Model.cancelRaidTwitchChannel(onComplete: (OperationResult) -> Unit) {
    createTwitchApi(stream).cancelRaid(
        broadcasterId = stream.twitchChannelId,
        onComplete = onComplete
    )
}

fun Model.twitchRaidStarted(channelLogin: String, channelName: String) {
    raid.state = RaidState.ongoing
    raid.channelLogin = channelLogin
    raid.message = localized("Raiding $channelName")
    raid.progress.progress = 0f
    raid.progress.goal = 90f
    searchTwitchChannel(stream, channelLogin) { channel ->
        raid.channelImage = channel?.thumbnail_url ?: ""
        val channelId = channel?.id
        if (channelId != null) {
            appendTwitchRaidSent(channelId = channelId, channelName = channelName)
        }
    }
}

fun Model.twitchRaidCancelled() {
    raid.message = localized("Raid cancelled")
    raid.state = RaidState.completed
}

fun Model.twitchRaidCompleted() {
    raid.state = RaidState.completed
    raid.message = localized("Raid completed!")
}

fun Model.updateTwitchRaid() {
    if (raid.state != RaidState.ongoing) {
        return
    }
    if (raid.progress.progress < raid.progress.goal) {
        raid.progress.progress += 1f
    }
}

fun Model.removeRaid() {
    raid.state = RaidState.idle
    raid.channelImage = ""
    raid.channelLogin = ""
}

private fun Model.appendTwitchRaidSent(channelId: String, channelName: String) {
    stream.twitchRaidsSent = appendTwitchRaidChannel(
        stream.twitchRaidsSent,
        channelId = channelId,
        channelName = channelName
    )
}

private fun Model.appendTwitchRaidReceived(channelId: String, channelName: String) {
    stream.twitchRaidsReceived = appendTwitchRaidChannel(
        stream.twitchRaidsReceived,
        channelId = channelId,
        channelName = channelName
    )
}

fun Model.createTwitchApi(stream: SettingsStream): TwitchApi {
    val twitchApi = TwitchApi(stream.twitchAccessToken)
    twitchApi.onUnauthorized = {
        twitchApiUnauthorized(stream)
    }
    return twitchApi
}

private fun Model.twitchApiUnauthorized(stream: SettingsStream) {
    if (!stream.twitchLoggedIn) {
        return
    }
    stream.twitchLoggedIn = false
    makeNotLoggedInToToast(ChatPlatform.twitch)
}

private fun Model.getStream() {
    createTwitchApi(stream).getStream(stream.twitchChannelId) { response ->
        when (response) {
            is NetworkResponse.Success -> {
                val data = response.data
                twitchPlatformStatus = if (data != null) {
                    StreamingPlatformStatus.Status.Live(data.viewer_count)
                } else {
                    StreamingPlatformStatus.Status.Offline
                }
            }
            else -> {
                twitchPlatformStatus = StreamingPlatformStatus.Status.Unknown
            }
        }
    }
}

private fun Model.parseTwitchTimestamp(value: String?): Instant? {
    if (value == null) {
        return null
    }
    var result = value
    val index = result.indexOf('.')
    if (index != -1) {
        result = result.substring(0, index) + "Z"
    }
    return runCatching { Instant.parse(result) }.getOrNull()
}

private fun Model.formatTwitchCountdown(date: Instant): String {
    val timeIntervalSinceNow = Duration.between(Instant.now(), date).toMillis().toDouble() / 1000.0
    return uptimeFormatter.format(ceil(max(0.0, timeIntervalSinceNow)))
}

private fun Model.updateTwitchPoll(event: TwitchEventSubChannelPollEvent, state: TwitchPollState) {
    twitchPoll.state = state
    twitchPoll.title = event.title
    twitchPoll.choices = event.choices.map {
        TwitchPollChoice(id = it.id, title = it.title, votes = it.votes ?: 0)
    }
    twitchPoll.totalVotes = twitchPoll.choices.sumOf { it.votes }
    twitchPoll.endsAt = parseTwitchTimestamp(event.ends_at)
}

fun Model.updateTwitchPollCountdown() {
    if (twitchPoll.state != TwitchPollState.ongoing) {
        return
    }
    val endsAt = twitchPoll.endsAt ?: return
    val countdown = formatTwitchCountdown(endsAt)
    twitchPoll.message = localized("Ends in $countdown")
}

fun Model.removeTwitchPoll() {
    twitchPoll.state = TwitchPollState.idle
}

private fun Model.updateTwitchPrediction(
    event: TwitchEventSubChannelPredictionEvent,
    state: TwitchPredictionState
) {
    twitchPrediction.state = state
    twitchPrediction.title = event.title
    twitchPrediction.outcomes = event.outcomes.map {
        TwitchPredictionOutcome(
            id = it.id,
            title = it.title,
            color = it.color,
            users = it.users ?: 0,
            channelPoints = it.channel_points ?: 0,
            winner = it.id == event.winning_outcome_id
        )
    }
    twitchPrediction.totalChannelPoints = twitchPrediction.outcomes.sumOf { it.channelPoints }
    twitchPrediction.locksAt = parseTwitchTimestamp(event.locks_at)
}

fun Model.updateTwitchPredictionCountdown() {
    if (twitchPrediction.state != TwitchPredictionState.ongoing) {
        return
    }
    val locksAt = twitchPrediction.locksAt ?: return
    val countdown = formatTwitchCountdown(locksAt)
    twitchPrediction.message = localized("Locks in $countdown")
}

fun Model.removeTwitchPrediction() {
    twitchPrediction.state = TwitchPredictionState.idle
}

fun Model.updateHypeTrainCountdown() {
    if (hypeTrain.progress == null) {
        return
    }
    val expiresAt = hypeTrain.expiresAt ?: return
    val countdown = formatTwitchCountdown(expiresAt)
    hypeTrain.message = localized("Ends in $countdown")
}

fun Model.removeHypeTrain() {
    hypeTrain.level = null
    hypeTrain.progress = null
    hypeTrain.expiresAt = null
    hypeTrain.message = ""
}

fun Model.makeTwitchAlertSegments(
    text: String,
    fragments: List<TwitchEventSubMessageFragment> = emptyList(),
    bits: String? = null
): List<ChatPostSegment> {
    return twitchChat?.createSegments(text = text, fragments = fragments, bits = bits)
        ?: makeChatPostTextSegments(text = text)
}

private fun Model.appendTwitchChatAlertMessage(
    user: String,
    segments: List<ChatPostSegment>,
    title: String,
    color: Color,
    image: String,
    kind: ChatHighlightKind,
    sharedChat: TwitchEventSubSharedChat?,
    chatter: TwitchEventSubChatter? = null
) {
    val highlight = ChatHighlight(
        kind = kind,
        barColor = color,
        image = image,
        titleSegments = listOf(ChatPostSegment(id = 0, text = title))
    )
    val userColor = RgbColor.fromHex(chatter?.color ?: "")
    val userBadges = (chatter?.badges ?: emptyList()).mapNotNull {
        twitchChat?.getBadgeUrl("${it.set_id}/${it.id}")
    }
    val chat = twitchChat
    if (sharedChat != null && chat != null) {
        chat.getSourceChannelIcon(sharedChat.broadcasterUserId) { sourceChannelIcon ->
            appendTwitchChatAlertMessage(
                user = user,
                userColor = userColor,
                userBadges = userBadges,
                segments = segments,
                highlight = highlight,
                sourceChannelIcon = sourceChannelIcon
            )
        }
    } else {
        appendTwitchChatAlertMessage(
            user = user,
            userColor = userColor,
            userBadges = userBadges,
            segments = segments,
            highlight = highlight,
            sourceChannelIcon = null
        )
    }
}

private fun Model.appendTwitchChatAlertMessage(
    user: String,
    userColor: RgbColor?,
    userBadges: List<String>,
    segments: List<ChatPostSegment>,
    highlight: ChatHighlight,
    sourceChannelIcon: String?
) {
    appendChatMessage(
        platform = ChatPlatform.twitch,
        messageId = null,
        displayName = user,
        user = user,
        userId = null,
        userColor = userColor,
        userBadges = userBadges,
        segments = segments,
        timestamp = statusOther.digitalClock,
        timestampTime = Instant.now(),
        isAction = false,
        isSubscriber = false,
        isModerator = false,
        isOwner = false,
        bits = null,
        highlight = highlight,
        live = true,
        sourceChannelIcon = sourceChannelIcon
    )
}

private fun Model.joinTwitchAlertText(text: String, message: TwitchEventSubMessage?): String {
    if (message == null || message.text.isEmpty()) {
        return text
    }
    return "$text ${message.text}"
}

private fun Model.isTwitchSharedChatAlertEnabled(
    sharedChat: TwitchEventSubSharedChat?,
    alerts: SettingsTwitchAlerts
): Boolean {
    return sharedChat == null || alerts.sharedChat
}

fun Model.twitchEventSubChannelFollow(event: TwitchEventSubNotificationChannelFollowEvent) {
    latestFollower = event.user_name
    val text = localized("just followed!")
    if (stream.twitchToastAlerts.follows) {
        makeToast(title = "${event.user_name} $text")
    }
    playAlert(Alert.TwitchFollow(event))
    if (stream.twitchChatAlerts.follows) {
        appendTwitchChatAlertMessage(
            user = event.user_name,
            segments = makeTwitchAlertSegments(text = text),
            title = localized("New follower"),
            color = Color(0xFFFF2D55),
            image = "medal",
            kind = ChatHighlightKind.newFollower,
            sharedChat = null
        )
    }
    printEventCatPrinters(
        event = EventCatPrinterEvent.TwitchFollow,
        username = event.user_name,
        message = text
    )
    macrosEventOccurred(MacroEvent(event = SettingsMacrosEvent.twitchFollow))
}

fun Model.twitchEventSubChannelSubscribe(event: TwitchEventSubNotificationChannelSubscribeEvent) {
    if (event.is_gift) {
        return
    }
    val text = if (event.isPrime()) {
        localized("just subscribed with Prime!")
    } else {
        localized("just subscribed tier ${event.tierAsNumber()}!")
    }
    val textWithMessage = joinTwitchAlertText(text, event.message)
    if (stream.twitchToastAlerts.subscriptions &&
        isTwitchSharedChatAlertEnabled(event.sharedChat, stream.twitchToastAlerts)
    ) {
        makeToast(title = "${event.user_name} $textWithMessage")
    }
    if (!isTwitchSharedChatAlertEnabled(event.sharedChat, stream.twitchChatAlerts)) {
        return
    }
    playAlert(Alert.TwitchSubscribe(event))
    if (stream.twitchChatAlerts.subscriptions) {
        appendTwitchChatAlertMessage(
            user = event.user_name,
            segments = makeTwitchAlertSegments(
                text = text,
                fragments = event.message?.fragments ?: emptyList()
            ),
            title = localized("New subscriber"),
            color = Color(0xFF32ADE6),
            image = "party.popper",
            kind = ChatHighlightKind.other,
            sharedChat = event.sharedChat,
            chatter = event.chatter
        )
    }
    printEventCatPrinters(
        event = EventCatPrinterEvent.TwitchSubscribe,
        username = event.user_name,
        message = textWithMessage
    )
    macrosEventOccurred(MacroEvent(event = SettingsMacrosEvent.twitchSubscription))
    latestSubscriber = event.user_name
}

fun Model.twitchEventSubChannelSubscriptionGift(
    event: TwitchEventSubNotificationChannelSubscriptionGiftEvent
) {
    val user = event.user_name ?: localized("Anonymous")
    val text = localized(
        "just gifted ${event.total} tier ${event.tierAsNumber()} subscriptions!"
    )
    val textWithMessage = joinTwitchAlertText(text, event.message)
    if (stream.twitchToastAlerts.giftSubscriptions &&
        isTwitchSharedChatAlertEnabled(event.sharedChat, stream.twitchToastAlerts)
    ) {
        makeToast(title = "$user $textWithMessage")
    }
    if (!isTwitchSharedChatAlertEnabled(event.sharedChat, stream.twitchChatAlerts)) {
        return
    }
    playAlert(Alert.TwitchSubscrptionGift(event))
    if (stream.twitchChatAlerts.giftSubscriptions) {
        appendTwitchChatAlertMessage(
            user = user,
            segments = makeTwitchAlertSegments(
                text = text,
                fragments = event.message?.fragments ?: emptyList()
            ),
            title = localized("Gift subscriptions"),
            color = Color(0xFF32ADE6),
            image = "gift",
            kind = ChatHighlightKind.other,
            sharedChat = event.sharedChat,
            chatter = event.chatter
        )
    }
    printEventCatPrinters(
        event = EventCatPrinterEvent.TwitchSubscrptionGift,
        username = user,
        message = textWithMessage
    )
    macrosEventOccurred(
        MacroEvent(event = SettingsMacrosEvent.twitchGiftSubscription, amount = event.total)
    )
    latestSubscriber = user
}

fun Model.twitchEventSubChannelSubscriptionMessage(
    event: TwitchEventSubNotificationChannelSubscriptionMessageEvent
) {
    val streakMonths = event.streak_months
    val text = if (streakMonths != null) {
        localized(
            "just resubscribed tier ${event.tierAsNumber()} for " +
                "${event.cumulative_months} months, $streakMonths in a row!"
        )
    } else {
        localized(
            "just resubscribed tier ${event.tierAsNumber()} for " +
                "${event.cumulative_months} months!"
        )
    }
    val textWithMessage = joinTwitchAlertText(text, event.message)
    if (stream.twitchToastAlerts.resubscriptions &&
        isTwitchSharedChatAlertEnabled(event.sharedChat, stream.twitchToastAlerts)
    ) {
        makeToast(title = "${event.user_name} $textWithMessage")
    }
    if (!isTwitchSharedChatAlertEnabled(event.sharedChat, stream.twitchChatAlerts)) {
        return
    }
    playAlert(Alert.TwitchResubscribe(event))
    if (stream.twitchChatAlerts.resubscriptions) {
        appendTwitchChatAlertMessage(
            user = event.user_name,
            segments = makeTwitchAlertSegments(text = text, fragments = event.message.fragments),
            title = localized("New resubscribe"),
            color = Color(0xFF32ADE6),
            image = "party.popper",
            kind = ChatHighlightKind.other,
            sharedChat = event.sharedChat,
            chatter = event.chatter
        )
    }
    printEventCatPrinters(
        event = EventCatPrinterEvent.TwitchResubscribe,
        username = event.user_name,
        message = textWithMessage
    )
    macrosEventOccurred(
        MacroEvent(event = SettingsMacrosEvent.twitchResubscription, amount = event.cumulative_months)
    )
    latestSubscriber = event.user_name
}

fun Model.twitchEventSubChannelSubscriptionUpgrade(
    event: TwitchEventSubNotificationChannelSubscriptionUpgradeEvent
) {
    val tier = event.tierAsNumber()
    val text = if (tier != null) {
        localized("just converted their Prime subscription to tier $tier!")
    } else {
        localized("just continued their gift subscription!")
    }
    val textWithMessage = joinTwitchAlertText(text, event.message)
    if (stream.twitchToastAlerts.subscriptions &&
        isTwitchSharedChatAlertEnabled(event.sharedChat, stream.twitchToastAlerts)
    ) {
        makeToast(title = "${event.user_name} $textWithMessage")
    }
    if (!isTwitchSharedChatAlertEnabled(event.sharedChat, stream.twitchChatAlerts)) {
        return
    }
    playAlert(Alert.TwitchSubscriptionUpgrade(event))
    if (stream.twitchChatAlerts.subscriptions) {
        appendTwitchChatAlertMessage(
            user = event.user_name,
            segments = makeTwitchAlertSegments(
                text = text,
                fragments = event.message?.fragments ?: emptyList()
            ),
            title = localized("New subscriber"),
            color = Color(0xFF32ADE6),
            image = "party.popper",
            kind = ChatHighlightKind.other,
            sharedChat = event.sharedChat,
            chatter = event.chatter
        )
    }
    printEventCatPrinters(
        event = EventCatPrinterEvent.TwitchSubscribe,
        username = event.user_name,
        message = textWithMessage
    )
    macrosEventOccurred(MacroEvent(event = SettingsMacrosEvent.twitchSubscription))
    latestSubscriber = event.user_name
}

fun Model.twitchEventSubChannelWatchStreak(
    event: TwitchEventSubNotificationChannelWatchStreakEvent
) {
    val text = localized("just watched ${event.streak_count} streams in a row!")
    if (stream.twitchToastAlerts.isWatchStreakEnabled(event.streak_count) &&
        isTwitchSharedChatAlertEnabled(event.sharedChat, stream.twitchToastAlerts)
    ) {
        makeToast(
            title = "${event.user_name} ${joinTwitchAlertText(text, event.message)}"
        )
    }
    if (!isTwitchSharedChatAlertEnabled(event.sharedChat, stream.twitchChatAlerts)) {
        return
    }
    if (stream.twitchChatAlerts.isWatchStreakEnabled(event.streak_count)) {
        appendTwitchChatAlertMessage(
            user = event.user_name,
            segments = makeTwitchAlertSegments(text = text, fragments = event.message.fragments),
            title = localized("Watch streak"),
            color = Color(0xFFFF9500),
            image = "flame",
            kind = ChatHighlightKind.other,
            sharedChat = event.sharedChat,
            chatter = event.chatter
        )
    }
    macrosEventOccurred(
        MacroEvent(event = SettingsMacrosEvent.twitchWatchStreak, amount = event.streak_count)
    )
}

fun Model.twitchEventSubChannelPointsCustomRewardRedemptionAdd(
    event: TwitchEventSubNotificationChannelPointsCustomRewardRedemptionAddEvent
) {
    val text = localized("redeemed ${event.reward.title}!")
    if (stream.twitchToastAlerts.rewards) {
        makeToast(title = "${event.user_name} $text")
    }
    if (false) {
        playAlert(Alert.TwitchRedemption(event))
    }
    if (stream.twitchChatAlerts.rewards) {
        appendTwitchChatAlertMessage(
            user = event.user_name,
            segments = makeTwitchAlertSegments(text = text),
            title = localized("Reward redemption"),
            color = Color(0xFF007AFF),
            image = "medal.star",
            kind = ChatHighlightKind.redemption,
            sharedChat = null
        )
    }
    printEventCatPrinters(
        event = EventCatPrinterEvent.TwitchReward,
        username = event.user_name,
        message = text
    )
    macrosEventOccurred(
        MacroEvent(event = SettingsMacrosEvent.twitchReward, text = event.reward.title)
    )
}

fun Model.twitchEventSubChannelRaid(event: TwitchEventSubChannelRaidEvent) {
    if (event.sharedChat == null && event.from_broadcaster_user_id == stream.twitchChannelId) {
        twitchRaidCompleted()
    } else {
        appendTwitchRaidReceived(
            channelId = event.from_broadcaster_user_id,
            channelName = event.from_broadcaster_user_name
        )
        val text = localized("raided with a party of ${event.viewers}!")
        val textWithMessage = joinTwitchAlertText(text, event.message)
        if (stream.twitchToastAlerts.raids &&
            isTwitchSharedChatAlertEnabled(event.sharedChat, stream.twitchToastAlerts)
        ) {
            makeToast(title = "${event.from_broadcaster_user_name} $textWithMessage")
        }
        if (!isTwitchSharedChatAlertEnabled(event.sharedChat, stream.twitchChatAlerts)) {
            return
        }
        playAlert(Alert.TwitchRaid(event))
        if (stream.twitchChatAlerts.raids) {
            appendTwitchChatAlertMessage(
                user = event.from_broadcaster_user_name,
                segments = makeTwitchAlertSegments(
                    text = text,
                    fragments = event.message?.fragments ?: emptyList()
                ),
                title = localized("Raid"),
                color = Color(0xFFFF2D55),
                image = "person.3",
                kind = ChatHighlightKind.other,
                sharedChat = event.sharedChat,
                chatter = event.chatter
            )
        }
        printEventCatPrinters(
            event = EventCatPrinterEvent.TwitchRaid,
            username = event.from_broadcaster_user_name,
            message = textWithMessage
        )
        macrosEventOccurred(MacroEvent(event = SettingsMacrosEvent.twitchRaid, amount = event.viewers))
    }
}

fun Model.twitchEventSubChannelCheer(event: TwitchEventSubChannelCheerEvent) {
    val user = event.user_name ?: localized("Anonymous")
    val bits = countFormatter.format(event.bits)
    val text = localized("cheered $bits bits!")
    if (stream.twitchToastAlerts.isBitsEnabled(event.bits)) {
        makeToast(title = "$user $text", subTitle = event.message)
    }
    playAlert(Alert.TwitchCheer(event))
    if (stream.twitchChatAlerts.isBitsEnabled(event.bits)) {
        appendTwitchChatAlertMessage(
            user = user,
            segments = makeTwitchAlertSegments(text = "$text ${event.message}", bits = ""),
            title = localized("Cheer"),
            color = Color(0xFF34C759),
            image = "suit.diamond",
            kind = ChatHighlightKind.other,
            sharedChat = null
        )
    }
    val message = if (event.message.isEmpty()) text else "$text ${event.message}"
    printEventCatPrinters(
        event = EventCatPrinterEvent.TwitchCheer(event.bits),
        username = user,
        message = message
    )
    macrosEventOccurred(MacroEvent(event = SettingsMacrosEvent.twitchCheer, amount = event.bits))
}

fun Model.twitchEventSubChannelHypeTrainBegin(event: TwitchEventSubChannelHypeTrainBeginEvent) {
    hypeTrain.level = event.level
    hypeTrain.progress = ProgressBar()
    hypeTrain.progress?.progress = event.progress.toFloat()
    hypeTrain.progress?.goal = event.goal.toFloat()
    hypeTrain.expiresAt = parseTwitchTimestamp(event.expires_at)
    updateHypeTrainCountdown()
    appendTwitchChatAlertMessage(
        user = stream.twitchChannelName,
        segments = makeTwitchAlertSegments(text = localized("started a hype train!")),
        title = localized("Hype train started"),
        color = Color(0xFFAF52DE),
        image = "train.side.front.car",
        kind = ChatHighlightKind.other,
        sharedChat = null
    )
}

fun Model.twitchEventSubChannelHypeTrainProgress(
    event: TwitchEventSubChannelHypeTrainProgressEvent
) {
    hypeTrain.level = event.level
    if (hypeTrain.progress == null) {
        hypeTrain.progress = ProgressBar()
    }
    hypeTrain.progress?.progress = event.progress.toFloat()
    hypeTrain.progress?.goal = event.goal.toFloat()
    hypeTrain.expiresAt = parseTwitchTimestamp(event.expires_at)
    updateHypeTrainCountdown()
}

fun Model.twitchEventSubChannelHypeTrainEnd(event: TwitchEventSubChannelHypeTrainEndEvent) {
    hypeTrain.level = event.level
    if (hypeTrain.progress == null) {
        hypeTrain.progress = ProgressBar()
    }
    hypeTrain.progress?.progress = 1f
    hypeTrain.progress?.goal = 1f
    hypeTrain.expiresAt = null
    hypeTrain.message = localized("Ended")
    appendTwitchChatAlertMessage(
        user = stream.twitchChannelName,
        segments = makeTwitchAlertSegments(
            text = localized("ended the hype train at level ${event.level}!")
        ),
        title = localized("Hype train ended"),
        color = Color(0xFFAF52DE),
        image = "train.side.rear.car",
        kind = ChatHighlightKind.other,
        sharedChat = null
    )
}

fun Model.twitchEventSubChannelAdBreakBegin(event: TwitchEventSubChannelAdBreakBeginEvent) {
    adsEndDate = Instant.now().plusSeconds(event.duration_seconds.toLong())
    val duration = formatShortDuration(event.duration_seconds)
    val kind = if (event.is_automatic) localized("automatic") else localized("manual")
    makeToast(title = localized("$duration $kind commercial starting"))
}

private fun Model.updateOngoingTwitchPoll(event: TwitchEventSubChannelPollEvent) {
    updateTwitchPoll(event = event, state = TwitchPollState.ongoing)
    updateTwitchPollCountdown()
}

fun Model.twitchEventSubChannelPollBegin(event: TwitchEventSubChannelPollEvent) {
    updateOngoingTwitchPoll(event)
    appendTwitchChatAlertMessage(
        user = stream.twitchChannelName,
        segments = makeTwitchAlertSegments(
            text = localized("started a poll: ${event.title}")
        ),
        title = localized("Poll started"),
        color = Color(0xFF5856D6),
        image = "chart.bar",
        kind = ChatHighlightKind.other,
        sharedChat = null
    )
}

fun Model.twitchEventSubChannelPollProgress(event: TwitchEventSubChannelPollEvent) {
    updateOngoingTwitchPoll(event)
}

fun Model.twitchEventSubChannelPollEnd(event: TwitchEventSubChannelPollEvent) {
    updateTwitchPoll(event = event, state = TwitchPollState.completed)
    val text: String
    if (event.status != "archived") {
        twitchPoll.message = localized("Poll ended")
        val winner = twitchPoll.choices.maxByOrNull { it.votes }
        if (winner != null) {
            text = localized("ended the poll: ${event.title} Winner: ${winner.title}")
        } else {
            text = localized("ended the poll: ${event.title}")
        }
    } else {
        return
    }
    appendTwitchChatAlertMessage(
        user = stream.twitchChannelName,
        segments = makeTwitchAlertSegments(text = text),
        title = localized("Poll ended"),
        color = Color(0xFF5856D6),
        image = "chart.bar",
        kind = ChatHighlightKind.other,
        sharedChat = null
    )
}

private fun Model.updateOngoingTwitchPrediction(event: TwitchEventSubChannelPredictionEvent) {
    updateTwitchPrediction(event = event, state = TwitchPredictionState.ongoing)
    updateTwitchPredictionCountdown()
}

fun Model.twitchEventSubChannelPredictionBegin(event: TwitchEventSubChannelPredictionEvent) {
    updateOngoingTwitchPrediction(event)
    appendTwitchChatAlertMessage(
        user = stream.twitchChannelName,
        segments = makeTwitchAlertSegments(
            text = localized("started a prediction: ${event.title}")
        ),
        title = localized("Prediction started"),
        color = Color(0xFF00C7BE),
        image = "questionmark.diamond",
        kind = ChatHighlightKind.other,
        sharedChat = null
    )
}

fun Model.twitchEventSubChannelPredictionProgress(event: TwitchEventSubChannelPredictionEvent) {
    updateOngoingTwitchPrediction(event)
}

fun Model.twitchEventSubChannelPredictionLock(event: TwitchEventSubChannelPredictionEvent) {
    updateTwitchPrediction(event = event, state = TwitchPredictionState.locked)
    twitchPrediction.message = localized("Locked, waiting for outcome")
}

fun Model.twitchEventSubChannelPredictionEnd(event: TwitchEventSubChannelPredictionEvent) {
    updateTwitchPrediction(event = event, state = TwitchPredictionState.completed)
    val text: String
    val winner = twitchPrediction.outcomes.firstOrNull { it.winner }
    if (winner != null) {
        twitchPrediction.message = localized("Outcome: ${winner.title}")
        text = localized("ended the prediction: ${event.title} Outcome: ${winner.title}")
    } else {
        twitchPrediction.message = localized("Prediction cancelled")
        text = localized("cancelled the prediction: ${event.title}")
    }
    appendTwitchChatAlertMessage(
        user = stream.twitchChannelName,
        segments = makeTwitchAlertSegments(text = text),
        title = localized("Prediction ended"),
        color = Color(0xFF00C7BE),
        image = "trophy",
        kind = ChatHighlightKind.other,
        sharedChat = null
    )
}

fun Model.twitchEventSubChannelModerate(event: TwitchEventSubChannelModerateEvent) {
    when (event.action) {
        "raid" -> {
            val raid = event.raid ?: return
            twitchRaidStarted(channelLogin = raid.user_login, channelName = raid.user_name)
        }
        "unraid" -> twitchRaidCancelled()
    }
}

fun Model.twitchEventSubUnauthorized() {
    twitchApiUnauthorized(stream)
}

fun Model.twitchEventSubNotification(message: String) {
}

fun Model.twitchChatMakeErrorToast(title: String, subTitle: String?) {
    makeErrorToast(title = title, subTitle = subTitle)
}

fun Model.twitchChatAppendMessage(
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
    sourceChannelIcon: String?
) {
    appendChatMessage(
        platform = ChatPlatform.twitch,
        messageId = messageId,
        displayName = displayName,
        user = user,
        userId = userId,
        userColor = userColor,
        userBadges = userBadges,
        segments = segments,
        timestamp = statusOther.digitalClock,
        timestampTime = Instant.now(),
        isAction = isAction,
        isSubscriber = isSubscriber,
        isModerator = isModerator,
        isOwner = false,
        bits = bits,
        highlight = highlight,
        live = true,
        sourceChannelIcon = sourceChannelIcon
    )
}

fun Model.twitchChatDeleteMessage(messageId: String) {
    deleteChatMessage(messageId = messageId)
}

fun Model.twitchChatDeleteUser(userId: String) {
    deleteChatUser(userId = userId)
}

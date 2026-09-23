package com.moblin.android.various.model

import android.util.Log
import androidx.compose.ui.graphics.Color
import com.moblin.android.common.various.RgbColor
import com.moblin.android.common.various.countFormatter
import com.moblin.android.common.various.formatShortDuration
import com.moblin.android.common.various.uptimeFormatter
import com.moblin.android.localized
import com.moblin.android.streamingplatforms.Platform
import com.moblin.android.streamingplatforms.twitch.*
import com.moblin.android.various.ChatHighlight
import com.moblin.android.various.ChatHighlightKind
import com.moblin.android.various.ChatPostSegment
import com.moblin.android.various.makeChatPostTextSegments
import com.moblin.android.various.network.NetworkResponse
import com.moblin.android.various.network.OperationResult
import com.moblin.android.various.settings.MacroEvent
import com.moblin.android.various.settings.MacroVariable
import com.moblin.android.various.settings.SettingsMacrosEvent
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.various.settings.SettingsStreamTwitchReward
import com.moblin.android.various.settings.SettingsTwitchAlerts
import com.moblin.android.various.settings.appendTwitchRaidChannel
import java.time.Duration
import java.time.Instant
import kotlin.math.ceil
import kotlin.math.max
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import com.moblin.android.AppDelegate

private const val TAG = "Model"

private fun macroVariableFromRawValue(rawValue: String): MacroVariable =
    enumValues<MacroVariable>().firstOrNull { it.name == rawValue || it.rawValue == rawValue }
        ?: error("Unknown macro variable: $rawValue")

fun Model.updateViewersTwitch(): StreamingPlatformStatus {
    return StreamingPlatformStatus(platform = Platform.twitch, status = twitchPlatformStatus)
}

fun Model.isTwitchEventSubConfigured(): Boolean {
    return stream.value.twitchLoggedIn
}

fun Model.isTwitchEventsConnected(): Boolean {
    return twitchEventSub?.isConnected() ?: false
}

fun Model.isTwitchViewersConfigured(): Boolean {
    return stream.value.twitchChannelId != "" && stream.value.twitchLoggedIn
}

fun Model.isTwitchChatConfigured(): Boolean {
    return database.chat.enabled && stream.value.twitchChannelName != ""
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
    if (isTwitchChatConfigured() && !isRemoteControlChatAndEvents(Platform.twitch)) {
        twitchChat?.start(
            channelName = stream.value.twitchChannelName,
            channelId = stream.value.twitchChannelId,
            settings = stream.value.chat,
            accessToken = stream.value.twitchAccessToken
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
            userId = stream.value.twitchChannelId,
            accessToken = stream.value.twitchAccessToken,
            context = AppDelegate.context,
            delegate = object : TwitchEventSubDelegate {
                override fun twitchEventSubChannelFollow(event: TwitchEventSubNotificationChannelFollowEvent) {
                    this@reloadTwitchEventSub.twitchEventSubChannelFollow(event)
                }
                override fun twitchEventSubChannelSubscribe(event: TwitchEventSubNotificationChannelSubscribeEvent) {
                    this@reloadTwitchEventSub.twitchEventSubChannelSubscribe(event)
                }
                override fun twitchEventSubChannelSubscriptionGift(event: TwitchEventSubNotificationChannelSubscriptionGiftEvent) {
                    this@reloadTwitchEventSub.twitchEventSubChannelSubscriptionGift(event)
                }
                override fun twitchEventSubChannelSubscriptionMessage(event: TwitchEventSubNotificationChannelSubscriptionMessageEvent) {
                    this@reloadTwitchEventSub.twitchEventSubChannelSubscriptionMessage(event)
                }
                override fun twitchEventSubChannelSubscriptionUpgrade(event: TwitchEventSubNotificationChannelSubscriptionUpgradeEvent) {
                    this@reloadTwitchEventSub.twitchEventSubChannelSubscriptionUpgrade(event)
                }
                override fun twitchEventSubChannelWatchStreak(event: TwitchEventSubNotificationChannelWatchStreakEvent) {
                    this@reloadTwitchEventSub.twitchEventSubChannelWatchStreak(event)
                }
                override fun twitchEventSubChannelPointsCustomRewardRedemptionAdd(event: TwitchEventSubNotificationChannelPointsCustomRewardRedemptionAddEvent) {
                    this@reloadTwitchEventSub.twitchEventSubChannelPointsCustomRewardRedemptionAdd(event)
                }
                override fun twitchEventSubChannelRaid(event: TwitchEventSubChannelRaidEvent) {
                    this@reloadTwitchEventSub.twitchEventSubChannelRaid(event)
                }
                override fun twitchEventSubChannelCheer(event: TwitchEventSubChannelCheerEvent) {
                    this@reloadTwitchEventSub.twitchEventSubChannelCheer(event)
                }
                override fun twitchEventSubChannelHypeTrainBegin(event: TwitchEventSubChannelHypeTrainBeginEvent) {
                    this@reloadTwitchEventSub.twitchEventSubChannelHypeTrainBegin(event)
                }
                override fun twitchEventSubChannelHypeTrainProgress(event: TwitchEventSubChannelHypeTrainProgressEvent) {
                    this@reloadTwitchEventSub.twitchEventSubChannelHypeTrainProgress(event)
                }
                override fun twitchEventSubChannelHypeTrainEnd(event: TwitchEventSubChannelHypeTrainEndEvent) {
                    this@reloadTwitchEventSub.twitchEventSubChannelHypeTrainEnd(event)
                }
                override fun twitchEventSubChannelAdBreakBegin(event: TwitchEventSubChannelAdBreakBeginEvent) {
                    this@reloadTwitchEventSub.twitchEventSubChannelAdBreakBegin(event)
                }
                override fun twitchEventSubChannelPollBegin(event: TwitchEventSubChannelPollEvent) {
                    this@reloadTwitchEventSub.twitchEventSubChannelPollBegin(event)
                }
                override fun twitchEventSubChannelPollProgress(event: TwitchEventSubChannelPollEvent) {
                    this@reloadTwitchEventSub.twitchEventSubChannelPollProgress(event)
                }
                override fun twitchEventSubChannelPollEnd(event: TwitchEventSubChannelPollEvent) {
                    this@reloadTwitchEventSub.twitchEventSubChannelPollEnd(event)
                }
                override fun twitchEventSubChannelPredictionBegin(event: TwitchEventSubChannelPredictionEvent) {
                    this@reloadTwitchEventSub.twitchEventSubChannelPredictionBegin(event)
                }
                override fun twitchEventSubChannelPredictionProgress(event: TwitchEventSubChannelPredictionEvent) {
                    this@reloadTwitchEventSub.twitchEventSubChannelPredictionProgress(event)
                }
                override fun twitchEventSubChannelPredictionLock(event: TwitchEventSubChannelPredictionEvent) {
                    this@reloadTwitchEventSub.twitchEventSubChannelPredictionLock(event)
                }
                override fun twitchEventSubChannelPredictionEnd(event: TwitchEventSubChannelPredictionEvent) {
                    this@reloadTwitchEventSub.twitchEventSubChannelPredictionEnd(event)
                }
                override fun twitchEventSubChannelModerate(event: TwitchEventSubChannelModerateEvent) {
                    this@reloadTwitchEventSub.twitchEventSubChannelModerate(event)
                }
                override fun twitchEventSubChannelShoutoutCreate(event: TwitchEventSubChannelShoutoutCreateEvent) {
                    this@reloadTwitchEventSub.twitchEventSubChannelShoutoutCreate(event)
                }
                override fun twitchEventSubUnauthorized() {
                    this@reloadTwitchEventSub.twitchEventSubUnauthorized()
                }
                override fun twitchEventSubNotification(message: String) {
                    this@reloadTwitchEventSub.twitchEventSubNotification(message)
                }
            }
        )
        twitchEventSub!!.start()
    }
}

fun Model.fetchTwitchRewards() {
    createTwitchApi(stream.value)
        .getChannelPointsCustomRewards(stream.value.twitchChannelId) { rewards ->
            if (rewards == null) {
                Log.i(TAG, "Failed to get Twitch rewards")
                return@getChannelPointsCustomRewards
            }
            Log.i(TAG, "Twitch rewards: $rewards")
            stream.value.twitchRewards = rewards.data.map {
                val reward = SettingsStreamTwitchReward()
                reward.rewardId = it.id
                reward.title = it.title
                reward
            }.toMutableList()
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
        showTwitchAuth.value = false
        showModerationAuth.value = false
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
    if (!stream.value.twitchWantsToBeLoggedIn || stream.value.twitchLoggedIn) {
        return
    }
    stream.value.twitchNotLoggedInCount += 1
    if (stream.value.twitchNotLoggedInCount >= maxNotLoggedInToastCount) {
        stream.value.twitchWantsToBeLoggedIn = false
    }
    makeNotLoggedInToToast(Platform.twitch)
}

fun Model.createStreamMarker() {
    createTwitchApi(stream.value).createStreamMarker(stream.value.twitchChannelId) { data ->
        if (data != null) {
            makeToast(title = localized("Stream marker created"))
        } else {
            makeErrorToast(title = localized("Failed to create stream marker"))
        }
    }
}

fun Model.updateTwitchStream(monotonicNow: Instant) {
    if (!isLive.value || !isTwitchViewersConfigured()) {
        twitchPlatformStatus = PlatformStatus.unknown
        return
    }
    if (monotonicNow.isBefore(twitchStreamUpdateTime.plusSeconds(25))) {
        return
    }
    twitchStreamUpdateTime = monotonicNow
    getStream()
}

fun Model.sendTwitchChatMessage(message: String, onComplete: (OperationResult) -> Unit) {
    createTwitchApi(stream.value).sendChatMessage(
        broadcasterId = stream.value.twitchChannelId,
        message = message,
        onComplete = onComplete
    )
}

fun Model.startAds(seconds: Int, onComplete: (OperationResult) -> Unit) {
    createTwitchApi(stream.value)
        .startCommercial(broadcasterId = stream.value.twitchChannelId, length = seconds) { result ->
            when (result) {
                is NetworkResponse.Success -> onComplete(NetworkResponse.Success(ByteArray(0)))
                is NetworkResponse.AuthError -> onComplete(NetworkResponse.AuthError)
                is NetworkResponse.Error -> onComplete(NetworkResponse.Error)
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
    createTwitchApi(stream.value).banUser(
        broadcasterId = stream.value.twitchChannelId,
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
    createTwitchApi(stream.value).getUserByLogin(user) { twitchUser ->
        if (twitchUser == null) {
            onComplete(NetworkResponse.Error)
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
    val twitchApi = createTwitchApi(stream.value)
    twitchApi.getUserByLogin(user) { twitchUser ->
        if (twitchUser == null) {
            onComplete(NetworkResponse.Error)
            return@getUserByLogin
        }
        twitchApi.unbanUser(
            broadcasterId = stream.value.twitchChannelId,
            userId = twitchUser.id,
            onComplete = onComplete
        )
    }
}

fun Model.modTwitchUser(user: String, onComplete: (OperationResult) -> Unit) {
    val twitchApi = createTwitchApi(stream.value)
    twitchApi.getUserByLogin(user) { twitchUser ->
        if (twitchUser == null) {
            onComplete(NetworkResponse.Error)
            return@getUserByLogin
        }
        twitchApi.addModerator(
            broadcasterId = stream.value.twitchChannelId,
            userId = twitchUser.id,
            onComplete = onComplete
        )
    }
}

fun Model.unmodTwitchUser(user: String, onComplete: (OperationResult) -> Unit) {
    val twitchApi = createTwitchApi(stream.value)
    twitchApi.getUserByLogin(user) { twitchUser ->
        if (twitchUser == null) {
            onComplete(NetworkResponse.Error)
            return@getUserByLogin
        }
        twitchApi.removeModerator(
            broadcasterId = stream.value.twitchChannelId,
            userId = twitchUser.id,
            onComplete = onComplete
        )
    }
}

fun Model.vipTwitchUser(user: String, onComplete: (OperationResult) -> Unit) {
    val twitchApi = createTwitchApi(stream.value)
    twitchApi.getUserByLogin(user) { twitchUser ->
        if (twitchUser == null) {
            onComplete(NetworkResponse.Error)
            return@getUserByLogin
        }
        twitchApi.addVip(
            broadcasterId = stream.value.twitchChannelId,
            userId = twitchUser.id,
            onComplete = onComplete
        )
    }
}

fun Model.unvipTwitchUser(user: String, onComplete: (OperationResult) -> Unit) {
    val twitchApi = createTwitchApi(stream.value)
    twitchApi.getUserByLogin(user) { twitchUser ->
        if (twitchUser == null) {
            onComplete(NetworkResponse.Error)
            return@getUserByLogin
        }
        twitchApi.removeVip(
            broadcasterId = stream.value.twitchChannelId,
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
    createTwitchApi(stream.value).sendAnnouncement(
        broadcasterId = stream.value.twitchChannelId,
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
    createTwitchApi(stream.value).updateChatSettings(
        broadcasterId = stream.value.twitchChannelId,
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
    createTwitchApi(stream.value).updateChatSettings(
        broadcasterId = stream.value.twitchChannelId,
        settings = settings,
        onComplete = onComplete
    )
}

fun Model.setTwitchEmoteOnlyMode(enabled: Boolean, onComplete: (OperationResult) -> Unit) {
    createTwitchApi(stream.value).updateChatSettings(
        broadcasterId = stream.value.twitchChannelId,
        settings = mapOf<String, Any>("emote_mode" to enabled),
        onComplete = onComplete
    )
}

fun Model.setTwitchSubscribersOnlyMode(enabled: Boolean, onComplete: (OperationResult) -> Unit) {
    createTwitchApi(stream.value).updateChatSettings(
        broadcasterId = stream.value.twitchChannelId,
        settings = mapOf<String, Any>("subscriber_mode" to enabled),
        onComplete = onComplete
    )
}

fun Model.deleteTwitchChatMessage(messageId: String) {
    createTwitchApi(stream.value)
        .deleteChatMessage(broadcasterId = stream.value.twitchChannelId, messageId = messageId) { }
}

fun Model.getTwitchPolls(onComplete: (NetworkResponse<List<TwitchApiPollData>>) -> Unit) {
    createTwitchApi(stream.value).getPolls(
        broadcasterId = stream.value.twitchChannelId,
        onComplete = onComplete
    )
}

fun Model.createTwitchPoll(
    title: String,
    choices: List<String>,
    duration: Int,
    onComplete: (OperationResult) -> Unit
) {
    createTwitchApi(stream.value).createPoll(
        broadcasterId = stream.value.twitchChannelId,
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
    createTwitchApi(stream.value).endPoll(
        broadcasterId = stream.value.twitchChannelId,
        id = id,
        status = status,
        onComplete = onComplete
    )
}

fun Model.getTwitchPredictions(
    onComplete: (NetworkResponse<List<TwitchApiPredictionData>>) -> Unit
) {
    createTwitchApi(stream.value).getPredictions(
        broadcasterId = stream.value.twitchChannelId,
        onComplete = onComplete
    )
}

fun Model.createTwitchPrediction(
    title: String,
    outcomes: List<String>,
    predictionWindow: Int,
    onComplete: (OperationResult) -> Unit
) {
    createTwitchApi(stream.value).createPrediction(
        broadcasterId = stream.value.twitchChannelId,
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
    createTwitchApi(stream.value).endPrediction(
        broadcasterId = stream.value.twitchChannelId,
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
    createTwitchApi(stream.value).startRaid(
        broadcasterId = stream.value.twitchChannelId,
        toBroadcasterId = channelId,
        onComplete = onComplete
    )
}

fun Model.sendTwitchShoutout(channelId: String, onComplete: (OperationResult) -> Unit) {
    createTwitchApi(stream.value).sendShoutout(
        broadcasterId = stream.value.twitchChannelId,
        toBroadcasterId = channelId,
        onComplete = onComplete
    )
}

fun Model.sendTwitchShoutout(channelId: String) {
    sendTwitchShoutout(channelId) { result ->
        if (result !is NetworkResponse.Success) {
            Log.i(TAG, "Failed to shoutout Twitch channel $channelId")
        }
    }
}

fun Model.cancelRaidTwitchChannel(onComplete: (OperationResult) -> Unit) {
    createTwitchApi(stream.value).cancelRaid(
        broadcasterId = stream.value.twitchChannelId,
        onComplete = onComplete
    )
}

fun Model.twitchRaidStarted(channelLogin: String, channelName: String) {
    raid.state.value = RaidState.ongoing
    raid.channelLogin.value = channelLogin
    raid.message.value = localized("Raiding $channelName")
    raid.progress.value.progress.value = 0f
    raid.progress.value.goal.value = 90f
    searchTwitchChannel(stream.value, channelLogin) { channel ->
        raid.channelImage.value = channel?.thumbnail_url ?: ""
        val channelId = channel?.id
        if (channelId != null) {
            appendTwitchRaidSent(channelId = channelId, channelName = channelName)
        }
    }
}

fun Model.twitchRaidCancelled() {
    raid.message.value = localized("Raid cancelled")
    raid.state.value = RaidState.completed
}

fun Model.twitchRaidCompleted() {
    raid.state.value = RaidState.completed
    raid.message.value = localized("Raid completed!")
}

fun Model.updateTwitchRaid() {
    if (raid.state.value != RaidState.ongoing) {
        return
    }
    if (raid.progress.value.progress.value < raid.progress.value.goal.value) {
        raid.progress.value.progress.value += 1f
    }
}

fun Model.removeRaid() {
    raid.state.value = RaidState.idle
    raid.channelImage.value = ""
    raid.channelLogin.value = ""
}

private fun Model.appendTwitchRaidSent(channelId: String, channelName: String) {
    stream.value.twitchRaidsSent = appendTwitchRaidChannel(
        stream.value.twitchRaidsSent,
        channelId = channelId,
        channelName = channelName
    ).toMutableList()
}

private fun Model.appendTwitchRaidReceived(channelId: String, channelName: String) {
    stream.value.twitchRaidsReceived = appendTwitchRaidChannel(
        stream.value.twitchRaidsReceived,
        channelId = channelId,
        channelName = channelName
    ).toMutableList()
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
    makeNotLoggedInToToast(Platform.twitch)
}

private fun Model.getStream() {
    createTwitchApi(stream.value).getStream(stream.value.twitchChannelId) { response ->
        when (response) {
            is NetworkResponse.Success -> {
                val data = response.value
                twitchPlatformStatus = if (data != null) {
                    PlatformStatus.live(data.viewer_count)
                } else {
                    PlatformStatus.offline
                }
            }
            else -> {
                twitchPlatformStatus = PlatformStatus.unknown
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
    return formatShortDuration(ceil(max(0.0, timeIntervalSinceNow)).toInt())
}

private fun Model.updateTwitchPoll(event: TwitchEventSubChannelPollEvent, state: TwitchPollState) {
    twitchPoll.state.value = state
    twitchPoll.title.value = event.title
    twitchPoll.choices.value = event.choices.map {
        TwitchPollChoice(id = it.id, title = it.title, votes = it.votes ?: 0)
    }
    twitchPoll.totalVotes.value = twitchPoll.choices.value.sumOf { it.votes }
    twitchPoll.endsAt = parseTwitchTimestamp(event.ends_at)
}

fun Model.updateTwitchPollCountdown() {
    if (twitchPoll.state.value != TwitchPollState.ongoing) {
        return
    }
    val endsAt = twitchPoll.endsAt ?: return
    val countdown = formatTwitchCountdown(endsAt)
    twitchPoll.message.value = localized("Ends in $countdown")
}

fun Model.removeTwitchPoll() {
    twitchPoll.state.value = TwitchPollState.idle
}

private fun Model.updateTwitchPrediction(
    event: TwitchEventSubChannelPredictionEvent,
    state: TwitchPredictionState
) {
    twitchPrediction.state.value = state
    twitchPrediction.title.value = event.title
    twitchPrediction.outcomes.value = event.outcomes.map {
        TwitchPredictionOutcome(
            id = it.id,
            title = it.title,
            color = it.color,
            users = it.users ?: 0,
            channelPoints = it.channel_points ?: 0,
            winner = it.id == event.winning_outcome_id
        )
    }
    twitchPrediction.totalChannelPoints.value = twitchPrediction.outcomes.value.sumOf { it.channelPoints }
    twitchPrediction.locksAt = parseTwitchTimestamp(event.locks_at)
}

fun Model.updateTwitchPredictionCountdown() {
    if (twitchPrediction.state.value != TwitchPredictionState.ongoing) {
        return
    }
    val locksAt = twitchPrediction.locksAt ?: return
    val countdown = formatTwitchCountdown(locksAt)
    twitchPrediction.message.value = localized("Locks in $countdown")
}

fun Model.removeTwitchPrediction() {
    twitchPrediction.state.value = TwitchPredictionState.idle
}

fun Model.updateHypeTrainCountdown() {
    if (hypeTrain.progress.value == null) {
        return
    }
    val expiresAt = hypeTrain.expiresAt ?: return
    val countdown = formatTwitchCountdown(expiresAt)
    hypeTrain.message.value = localized("Ends in $countdown")
}

fun Model.removeHypeTrain() {
    hypeTrain.level.value = null
    hypeTrain.progress.value = null
    hypeTrain.expiresAt = null
    hypeTrain.message.value = ""
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
        platform = Platform.twitch,
        messageId = null,
        displayName = user,
        user = user,
        userId = null,
        userColor = userColor,
        userBadges = userBadges,
        segments = segments,
        timestamp = statusOther.digitalClock.value,
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
    if (stream.value.twitchToastAlerts.follows) {
        makeToast(title = "${event.user_name} $text")
    }
    playAlert(com.moblin.android.videoeffects.alerts.AlertsEffectAlert.TwitchFollow(event))
    if (stream.value.twitchChatAlerts.follows) {
        appendTwitchChatAlertMessage(
            user = event.user_name,
            segments = makeTwitchAlertSegments(text = text),
            title = localized("New follower"),
            color = Color(0xFFFF2D55),
            image = "medal",
            kind = TODO("no ChatHighlightKind case for newFollower"),
            sharedChat = null
        )
    }
    printEventCatPrinters(
        event = TODO("no Android counterpart for EventCatPrinterEvent"),
        username = event.user_name,
        message = text
    )
    macrosEventOccurred(
        MacroEvent(
            event = SettingsMacrosEvent.fromRawValue("twitchFollow")!!,
            variables = mutableMapOf(macroVariableFromRawValue("twitchFollowUser") to event.user_name)
        )
    )
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
    if (stream.value.twitchToastAlerts.subscriptions &&
        isTwitchSharedChatAlertEnabled(event.sharedChat, stream.value.twitchToastAlerts)
    ) {
        makeToast(title = "${event.user_name} $textWithMessage")
    }
    if (!isTwitchSharedChatAlertEnabled(event.sharedChat, stream.value.twitchChatAlerts)) {
        return
    }
    playAlert(com.moblin.android.videoeffects.alerts.AlertsEffectAlert.TwitchSubscribe(event))
    if (stream.value.twitchChatAlerts.subscriptions) {
        appendTwitchChatAlertMessage(
            user = event.user_name,
            segments = makeTwitchAlertSegments(
                text = text,
                fragments = event.message?.fragments ?: emptyList()
            ),
            title = localized("New subscriber"),
            color = Color(0xFF32ADE6),
            image = "party.popper",
            kind = TODO("no ChatHighlightKind case for other"),
            sharedChat = event.sharedChat,
            chatter = event.chatter
        )
    }
    printEventCatPrinters(
        event = TODO("no Android counterpart for EventCatPrinterEvent"),
        username = event.user_name,
        message = textWithMessage
    )
    macrosEventOccurred(
        MacroEvent(
            event = SettingsMacrosEvent.fromRawValue("twitchSubscription")!!,
            variables = mutableMapOf(macroVariableFromRawValue("twitchSubscriptionUser") to event.user_name)
        )
    )
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
    if (stream.value.twitchToastAlerts.giftSubscriptions &&
        isTwitchSharedChatAlertEnabled(event.sharedChat, stream.value.twitchToastAlerts)
    ) {
        makeToast(title = "$user $textWithMessage")
    }
    if (!isTwitchSharedChatAlertEnabled(event.sharedChat, stream.value.twitchChatAlerts)) {
        return
    }
    playAlert(com.moblin.android.videoeffects.alerts.AlertsEffectAlert.TwitchSubscrptionGift(event))
    if (stream.value.twitchChatAlerts.giftSubscriptions) {
        appendTwitchChatAlertMessage(
            user = user,
            segments = makeTwitchAlertSegments(
                text = text,
                fragments = event.message?.fragments ?: emptyList()
            ),
            title = localized("Gift subscriptions"),
            color = Color(0xFF32ADE6),
            image = "gift",
            kind = TODO("no ChatHighlightKind case for other"),
            sharedChat = event.sharedChat,
            chatter = event.chatter
        )
    }
    printEventCatPrinters(
        event = TODO("no Android counterpart for EventCatPrinterEvent"),
        username = user,
        message = textWithMessage
    )
    macrosEventOccurred(
        MacroEvent(
            event = SettingsMacrosEvent.fromRawValue("twitchGiftSubscription")!!,
            amount = event.total,
            variables = mutableMapOf(macroVariableFromRawValue("twitchGiftSubscriptionUser") to user)
        )
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
    if (stream.value.twitchToastAlerts.resubscriptions &&
        isTwitchSharedChatAlertEnabled(event.sharedChat, stream.value.twitchToastAlerts)
    ) {
        makeToast(title = "${event.user_name} $textWithMessage")
    }
    if (!isTwitchSharedChatAlertEnabled(event.sharedChat, stream.value.twitchChatAlerts)) {
        return
    }
    playAlert(com.moblin.android.videoeffects.alerts.AlertsEffectAlert.TwitchResubscribe(event))
    if (stream.value.twitchChatAlerts.resubscriptions) {
        appendTwitchChatAlertMessage(
            user = event.user_name,
            segments = makeTwitchAlertSegments(text = text, fragments = event.message.fragments),
            title = localized("New resubscribe"),
            color = Color(0xFF32ADE6),
            image = "party.popper",
            kind = TODO("no ChatHighlightKind case for other"),
            sharedChat = event.sharedChat,
            chatter = event.chatter
        )
    }
    printEventCatPrinters(
        event = TODO("no Android counterpart for EventCatPrinterEvent"),
        username = event.user_name,
        message = textWithMessage
    )
    macrosEventOccurred(
        MacroEvent(
            event = SettingsMacrosEvent.fromRawValue("twitchResubscription")!!,
            amount = event.cumulative_months,
            variables = mutableMapOf(macroVariableFromRawValue("twitchResubscriptionUser") to event.user_name)
        )
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
    if (stream.value.twitchToastAlerts.subscriptions &&
        isTwitchSharedChatAlertEnabled(event.sharedChat, stream.value.twitchToastAlerts)
    ) {
        makeToast(title = "${event.user_name} $textWithMessage")
    }
    if (!isTwitchSharedChatAlertEnabled(event.sharedChat, stream.value.twitchChatAlerts)) {
        return
    }
    playAlert(com.moblin.android.videoeffects.alerts.AlertsEffectAlert.TwitchSubscriptionUpgrade(event))
    if (stream.value.twitchChatAlerts.subscriptions) {
        appendTwitchChatAlertMessage(
            user = event.user_name,
            segments = makeTwitchAlertSegments(
                text = text,
                fragments = event.message?.fragments ?: emptyList()
            ),
            title = localized("New subscriber"),
            color = Color(0xFF32ADE6),
            image = "party.popper",
            kind = TODO("no ChatHighlightKind case for other"),
            sharedChat = event.sharedChat,
            chatter = event.chatter
        )
    }
    printEventCatPrinters(
        event = TODO("no Android counterpart for EventCatPrinterEvent"),
        username = event.user_name,
        message = textWithMessage
    )
    macrosEventOccurred(
        MacroEvent(
            event = SettingsMacrosEvent.fromRawValue("twitchSubscription")!!,
            variables = mutableMapOf(macroVariableFromRawValue("twitchSubscriptionUser") to event.user_name)
        )
    )
    latestSubscriber = event.user_name
}

fun Model.twitchEventSubChannelWatchStreak(
    event: TwitchEventSubNotificationChannelWatchStreakEvent
) {
    val text = localized("just watched ${event.streak_count} streams in a row!")
    if (stream.value.twitchToastAlerts.isWatchStreakEnabled(event.streak_count) &&
        isTwitchSharedChatAlertEnabled(event.sharedChat, stream.value.twitchToastAlerts)
    ) {
        makeToast(
            title = "${event.user_name} ${joinTwitchAlertText(text, event.message)}"
        )
    }
    if (!isTwitchSharedChatAlertEnabled(event.sharedChat, stream.value.twitchChatAlerts)) {
        return
    }
    if (stream.value.twitchChatAlerts.isWatchStreakEnabled(event.streak_count)) {
        appendTwitchChatAlertMessage(
            user = event.user_name,
            segments = makeTwitchAlertSegments(text = text, fragments = event.message.fragments),
            title = localized("Watch streak"),
            color = Color(0xFFFF9500),
            image = "flame",
            kind = TODO("no ChatHighlightKind case for other"),
            sharedChat = event.sharedChat,
            chatter = event.chatter
        )
    }
    macrosEventOccurred(
        MacroEvent(
            event = SettingsMacrosEvent.fromRawValue("twitchWatchStreak")!!,
            amount = event.streak_count,
            variables = mutableMapOf(macroVariableFromRawValue("twitchWatchStreakUser") to event.user_name)
        )
    )
}

fun Model.twitchEventSubChannelPointsCustomRewardRedemptionAdd(
    event: TwitchEventSubNotificationChannelPointsCustomRewardRedemptionAddEvent
) {
    val text = localized("redeemed ${event.reward.title}!")
    if (stream.value.twitchToastAlerts.rewards) {
        makeToast(title = "${event.user_name} $text")
    }
    if (false) {
        playAlert(com.moblin.android.videoeffects.alerts.AlertsEffectAlert.TwitchRedemption(event))
    }
    if (stream.value.twitchChatAlerts.rewards) {
        appendTwitchChatAlertMessage(
            user = event.user_name,
            segments = makeTwitchAlertSegments(text = text),
            title = localized("Reward redemption"),
            color = Color(0xFF007AFF),
            image = "medal.star",
            kind = TODO("no ChatHighlightKind case for redemption"),
            sharedChat = null
        )
    }
    printEventCatPrinters(
        event = TODO("no Android counterpart for EventCatPrinterEvent"),
        username = event.user_name,
        message = text
    )
    macrosEventOccurred(
        MacroEvent(
            event = SettingsMacrosEvent.fromRawValue("twitchReward")!!,
            text = event.reward.title,
            variables = mutableMapOf(macroVariableFromRawValue("twitchRewardUser") to event.user_name)
        )
    )
}

fun Model.twitchEventSubChannelRaid(event: TwitchEventSubChannelRaidEvent) {
    if (event.sharedChat == null && event.from_broadcaster_user_id == stream.value.twitchChannelId) {
        twitchRaidCompleted()
    } else {
        appendTwitchRaidReceived(
            channelId = event.from_broadcaster_user_id,
            channelName = event.from_broadcaster_user_name
        )
        val text = localized("raided with a party of ${event.viewers}!")
        val textWithMessage = joinTwitchAlertText(text, event.message)
        if (stream.value.twitchToastAlerts.raids &&
            isTwitchSharedChatAlertEnabled(event.sharedChat, stream.value.twitchToastAlerts)
        ) {
            makeToast(title = "${event.from_broadcaster_user_name} $textWithMessage")
        }
        if (!isTwitchSharedChatAlertEnabled(event.sharedChat, stream.value.twitchChatAlerts)) {
            return
        }
        playAlert(com.moblin.android.videoeffects.alerts.AlertsEffectAlert.TwitchRaid(event))
        if (stream.value.twitchChatAlerts.raids) {
            appendTwitchChatAlertMessage(
                user = event.from_broadcaster_user_name,
                segments = makeTwitchAlertSegments(
                    text = text,
                    fragments = event.message?.fragments ?: emptyList()
                ),
                title = localized("Raid"),
                color = Color(0xFFFF2D55),
                image = "person.3",
                kind = TODO("no ChatHighlightKind case for other"),
                sharedChat = event.sharedChat,
                chatter = event.chatter
            )
        }
        printEventCatPrinters(
            event = TODO("no Android counterpart for EventCatPrinterEvent"),
            username = event.from_broadcaster_user_name,
            message = textWithMessage
        )
        macrosEventOccurred(
            MacroEvent(
                event = SettingsMacrosEvent.fromRawValue("twitchRaid")!!,
                amount = event.viewers,
                variables = mutableMapOf(
                    macroVariableFromRawValue("twitchRaidChannelId") to event.from_broadcaster_user_id,
                    macroVariableFromRawValue("twitchRaidChannelName") to event.from_broadcaster_user_name
                )
            )
        )
    }
}

fun Model.twitchEventSubChannelCheer(event: TwitchEventSubChannelCheerEvent) {
    val user = event.user_name ?: localized("Anonymous")
    val bits = countFormatter.format(event.bits)
    val text = localized("cheered $bits bits!")
    if (stream.value.twitchToastAlerts.isBitsEnabled(event.bits)) {
        makeToast(title = "$user $text", subTitle = event.message)
    }
    playAlert(com.moblin.android.videoeffects.alerts.AlertsEffectAlert.TwitchCheer(event))
    if (stream.value.twitchChatAlerts.isBitsEnabled(event.bits)) {
        appendTwitchChatAlertMessage(
            user = user,
            segments = makeTwitchAlertSegments(text = "$text ${event.message}", bits = ""),
            title = localized("Cheer"),
            color = Color(0xFF34C759),
            image = "suit.diamond",
            kind = TODO("no ChatHighlightKind case for other"),
            sharedChat = null
        )
    }
    val message = if (event.message.isEmpty()) text else "$text ${event.message}"
    printEventCatPrinters(
        event = TODO("no Android counterpart for EventCatPrinterEvent"),
        username = user,
        message = message
    )
    macrosEventOccurred(
        MacroEvent(
            event = SettingsMacrosEvent.fromRawValue("twitchCheer")!!,
            amount = event.bits,
            variables = mutableMapOf(macroVariableFromRawValue("twitchCheerUser") to user)
        )
    )
}

fun Model.twitchEventSubChannelHypeTrainBegin(event: TwitchEventSubChannelHypeTrainBeginEvent) {
    hypeTrain.level.value = event.level
    hypeTrain.progress.value = ProgressBar()
    hypeTrain.progress.value?.progress?.value = event.progress.toFloat()
    hypeTrain.progress.value?.goal?.value = event.goal.toFloat()
    hypeTrain.expiresAt = parseTwitchTimestamp(event.expires_at)
    updateHypeTrainCountdown()
    appendTwitchChatAlertMessage(
        user = stream.value.twitchChannelName,
        segments = makeTwitchAlertSegments(text = localized("started a hype train!")),
        title = localized("Hype train started"),
        color = Color(0xFFAF52DE),
        image = "train.side.front.car",
        kind = TODO("no ChatHighlightKind case for other"),
        sharedChat = null
    )
}

fun Model.twitchEventSubChannelHypeTrainProgress(
    event: TwitchEventSubChannelHypeTrainProgressEvent
) {
    hypeTrain.level.value = event.level
    if (hypeTrain.progress.value == null) {
        hypeTrain.progress.value = ProgressBar()
    }
    hypeTrain.progress.value?.progress?.value = event.progress.toFloat()
    hypeTrain.progress.value?.goal?.value = event.goal.toFloat()
    hypeTrain.expiresAt = parseTwitchTimestamp(event.expires_at)
    updateHypeTrainCountdown()
}

fun Model.twitchEventSubChannelHypeTrainEnd(event: TwitchEventSubChannelHypeTrainEndEvent) {
    hypeTrain.level.value = event.level
    if (hypeTrain.progress.value == null) {
        hypeTrain.progress.value = ProgressBar()
    }
    hypeTrain.progress.value?.progress?.value = 1f
    hypeTrain.progress.value?.goal?.value = 1f
    hypeTrain.expiresAt = null
    hypeTrain.message.value = localized("Ended")
    appendTwitchChatAlertMessage(
        user = stream.value.twitchChannelName,
        segments = makeTwitchAlertSegments(
            text = localized("ended the hype train at level ${event.level}!")
        ),
        title = localized("Hype train ended"),
        color = Color(0xFFAF52DE),
        image = "train.side.rear.car",
        kind = TODO("no ChatHighlightKind case for other"),
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
        user = stream.value.twitchChannelName,
        segments = makeTwitchAlertSegments(
            text = localized("started a poll: ${event.title}")
        ),
        title = localized("Poll started"),
        color = Color(0xFF5856D6),
        image = "chart.bar",
        kind = TODO("no ChatHighlightKind case for other"),
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
        twitchPoll.message.value = localized("Poll ended")
        val winner = twitchPoll.choices.value.maxByOrNull { it.votes }
        if (winner != null) {
            text = localized("ended the poll: ${event.title} Winner: ${winner.title}")
        } else {
            text = localized("ended the poll: ${event.title}")
        }
    } else {
        return
    }
    appendTwitchChatAlertMessage(
        user = stream.value.twitchChannelName,
        segments = makeTwitchAlertSegments(text = text),
        title = localized("Poll ended"),
        color = Color(0xFF5856D6),
        image = "chart.bar",
        kind = TODO("no ChatHighlightKind case for other"),
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
        user = stream.value.twitchChannelName,
        segments = makeTwitchAlertSegments(
            text = localized("started a prediction: ${event.title}")
        ),
        title = localized("Prediction started"),
        color = Color(0xFF00C7BE),
        image = "questionmark.diamond",
        kind = TODO("no ChatHighlightKind case for other"),
        sharedChat = null
    )
}

fun Model.twitchEventSubChannelPredictionProgress(event: TwitchEventSubChannelPredictionEvent) {
    updateOngoingTwitchPrediction(event)
}

fun Model.twitchEventSubChannelPredictionLock(event: TwitchEventSubChannelPredictionEvent) {
    updateTwitchPrediction(event = event, state = TwitchPredictionState.locked)
    twitchPrediction.message.value = localized("Locked, waiting for outcome")
}

fun Model.twitchEventSubChannelPredictionEnd(event: TwitchEventSubChannelPredictionEvent) {
    updateTwitchPrediction(event = event, state = TwitchPredictionState.completed)
    val text: String
    val winner = twitchPrediction.outcomes.value.firstOrNull { it.winner }
    if (winner != null) {
        twitchPrediction.message.value = localized("Outcome: ${winner.title}")
        text = localized("ended the prediction: ${event.title} Outcome: ${winner.title}")
    } else {
        twitchPrediction.message.value = localized("Prediction cancelled")
        text = localized("cancelled the prediction: ${event.title}")
    }
    appendTwitchChatAlertMessage(
        user = stream.value.twitchChannelName,
        segments = makeTwitchAlertSegments(text = text),
        title = localized("Prediction ended"),
        color = Color(0xFF00C7BE),
        image = "trophy",
        kind = TODO("no ChatHighlightKind case for other"),
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

fun Model.twitchEventSubChannelShoutoutCreate(event: TwitchEventSubChannelShoutoutCreateEvent) {
    appendTwitchChatAlertMessage(
        user = event.moderator_user_name,
        segments = makeTwitchAlertSegments(
            text = localized("gave a shoutout to ${event.to_broadcaster_user_name}!")
        ),
        title = localized("Shoutout sent"),
        color = Color(0xFFFF9500),
        image = "megaphone",
        kind = TODO("no ChatHighlightKind case for other"),
        sharedChat = null
    )
}

fun Model.twitchEventSubUnauthorized() {
    twitchApiUnauthorized(stream.value)
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
        platform = Platform.twitch,
        messageId = messageId,
        displayName = displayName,
        user = user,
        userId = userId,
        userColor = userColor,
        userBadges = userBadges,
        segments = segments,
        timestamp = statusOther.digitalClock.value,
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

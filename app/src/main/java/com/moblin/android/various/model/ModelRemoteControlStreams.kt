package com.moblin.android.various.model

import com.moblin.android.common.various.cleanUrl
import com.moblin.android.remotecontrol.RemoteControlStreamState
import com.moblin.android.remotecontrol.isValidRemoteControlStreamUpdate
import com.moblin.android.streamingplatforms.kick.getKickChannelInfo
import com.moblin.android.streamingplatforms.kick.removeKickAccessTokenInKeychain
import com.moblin.android.streamingplatforms.twitch.removeTwitchAccessTokenInKeychain
import com.moblin.android.streamingplatforms.youtube.removeYouTubeAuthStateInKeychain
import com.moblin.android.various.settings.SettingsKickAlerts
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.various.settings.SettingsStreamMultiStreaming
import com.moblin.android.various.settings.SettingsStreamMultiStreamingDestination
import com.moblin.android.various.settings.SettingsStreamRateControl
import com.moblin.android.various.settings.SettingsStreamRecording
import com.moblin.android.various.settings.SettingsStreamReplay
import com.moblin.android.various.settings.SettingsStreamSrt
import com.moblin.android.various.settings.SettingsStreamSrtConnectionPriority
import com.moblin.android.various.settings.SettingsTwitchAlerts
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.various.utils.removeAllWhitespaces
import java.util.UUID
import com.moblin.android.common.various.deepCopy

private class StreamUpdateEffects {
    var reload = ChangeTracker()
    var portrait = false
    var pictureInPicture = ChangeTracker()
    var obsWebSocket = ChangeTracker()
    var location = ChangeTracker()
    var chats = ChangeTracker()
    var lowLightBoost = false
    var bitrate = false
    var audioBitrate = false
    var timecodes = ChangeTracker()
    var adaptiveBitrateAlgorithm = false
    var adaptiveBitrate = ChangeTracker()
    var connectionPriorities = ChangeTracker()
    var cleanRecordings = false
    var cleanSnapshots = false
    var replayEnabled = false
    var replayLayout = ChangeTracker()
    var twitchChannelName = false
    var twitchChannelId = false
    var kickChannelName = false
    var youTubeVideoIds = false
    var soopChannelName = false
    var soopStreamId = false
    var openStreamingPlatformUrl = false
    var openStreamingPlatformChannelId = false
}

fun Model.remoteControlGetStream(id: UUID, secrets: Boolean): SettingsStream? {
    val stream = findStream(id = id) ?: return null
    val copy = stream.deepCopy() ?: return null
    copy.removeAccessTokens()
    if (!secrets) {
        copy.removeSecrets()
    }
    return copy
}

fun Model.remoteControlCreateStream(stream: SettingsStream, secrets: Boolean): Boolean {
    stream.removeAccessTokens()
    if (!secrets) {
        stream.keepSecrets(old = SettingsStream(name = ""))
    }
    if (database.streams.any { it.id == stream.id }) {
        return false
    }
    if (stream.name.isEmpty()) {
        stream.name = makeUniqueName(name = SettingsStream.baseName, existingNames = database.streams)
    }
    val newStream = SettingsStream(name = stream.name)
    newStream.id = stream.id
    newStream.srt.connectionPriorities.priorities = stream.srt.connectionPriorities.priorities.map {
        val priority = SettingsStreamSrtConnectionPriority(name = it.name)
        priority.id = it.id
        priority.relayId = it.relayId
        priority
    }.toMutableList()
    if (!remoteControlApplyStreamUpdate(stream = newStream, new = stream, creating = true)) {
        return false
    }
    removeTwitchAccessTokenInKeychain(streamId = newStream.id)
    removeKickAccessTokenInKeychain(streamId = newStream.id)
    removeYouTubeAuthStateInKeychain(streamId = newStream.id)
    database.streams.add(newStream)
    return true
}

fun Model.remoteControlUpdateStream(stream: SettingsStream, secrets: Boolean): Boolean {
    stream.removeAccessTokens()
    val oldStream = findStream(id = stream.id) ?: return false
    if (!secrets) {
        stream.keepSecrets(old = oldStream)
    }
    return remoteControlApplyStreamUpdate(stream = oldStream, new = stream, creating = false)
}

fun Model.remoteControlDeleteStream(id: UUID): Boolean {
    val stream = findStream(id = id) ?: return false
    if (stream.enabled) {
        return false
    }
    database.streams.removeAll { it === stream }
    return true
}

private fun Model.remoteControlApplyStreamUpdate(
    stream: SettingsStream,
    new: SettingsStream,
    creating: Boolean,
): Boolean {
    if (!isValidRemoteControlStreamUpdate(
            stream = stream,
            new = new,
            streams = database.streams,
            state = RemoteControlStreamState(isLive = isLive.value, isRecording = isRecording.value),
            creating = creating,
        )
    ) {
        return false
    }
    val effects = StreamUpdateEffects()
    stream.name = new.name
    effects.reload.apply({ stream.url }, { stream.url = it }, cleanUrl(value = new.url))
    effects.portrait = stream.portrait != new.portrait
    stream.portrait = new.portrait
    effects.pictureInPicture.apply(
        { stream.backgroundStreaming },
        { stream.backgroundStreaming = it },
        new.backgroundStreaming,
    )
    effects.pictureInPicture.apply(
        { stream.backgroundStreamingPiP },
        { stream.backgroundStreamingPiP = it },
        new.backgroundStreamingPiP,
    )
    stream.autoGoLive = new.autoGoLive
    stream.estimatedViewerDelay = new.estimatedViewerDelay
    updateStreamObs(stream, new, effects)
    effects.location.apply(
        { stream.realtimeIrlEnabled },
        { stream.realtimeIrlEnabled = it },
        new.realtimeIrlEnabled,
    )
    effects.location.apply(
        { stream.realtimeIrlBaseUrl },
        { stream.realtimeIrlBaseUrl = it },
        new.realtimeIrlBaseUrl,
    )
    effects.location.apply(
        { stream.realtimeIrlPushKey },
        { stream.realtimeIrlPushKey = it },
        new.realtimeIrlPushKey,
    )
    effects.chats.apply({ stream.chat.bttvEmotes }, { stream.chat.bttvEmotes = it }, new.chat.bttvEmotes)
    effects.chats.apply({ stream.chat.ffzEmotes }, { stream.chat.ffzEmotes = it }, new.chat.ffzEmotes)
    effects.chats.apply({ stream.chat.seventvEmotes }, { stream.chat.seventvEmotes = it }, new.chat.seventvEmotes)
    stream.goLiveNotificationDiscordMessage = new.goLiveNotificationDiscordMessage
    stream.goLiveNotificationDiscordWebhookUrl =
        cleanUrl(value = new.goLiveNotificationDiscordWebhookUrl)
    stream.goLiveNotificationMoblinWebsite = new.goLiveNotificationMoblinWebsite
    stream.discordSnapshotWebhook = cleanUrl(value = new.discordSnapshotWebhook)
    stream.discordChatBotSnapshotWebhook = cleanUrl(value = new.discordChatBotSnapshotWebhook)
    stream.discordSnapshotWebhookOnlyWhenLive = new.discordSnapshotWebhookOnlyWhenLive
    updateStreamVideo(stream, new, effects)
    updateStreamSrt(stream.srt, new.srt, effects)
    effects.reload.apply(
        { stream.rtmp.adaptiveBitrateEnabled },
        { stream.rtmp.adaptiveBitrateEnabled = it },
        new.rtmp.adaptiveBitrateEnabled,
    )
    effects.reload.apply(
        { stream.rist.adaptiveBitrateEnabled },
        { stream.rist.adaptiveBitrateEnabled = it },
        new.rist.adaptiveBitrateEnabled,
    )
    effects.reload.apply({ stream.rist.bonding }, { stream.rist.bonding = it }, new.rist.bonding)
    effects.reload.apply({ stream.whip.headers }, { stream.whip.headers = it }, new.whip.headers)
    effects.reload.apply(
        { stream.whip.httpTransport },
        { stream.whip.httpTransport = it },
        new.whip.httpTransport,
    )
    updateStreamMultiStreaming(stream.multiStreaming, new.multiStreaming, effects)
    updateStreamRecording(stream.recording, new.recording, effects)
    stream.previewStream.url = cleanUrl(value = new.previewStream.url)
    stream.previewStream.resolution = new.previewStream.resolution
    stream.previewStream.bitrate = new.previewStream.bitrate
    updateStreamReplay(stream.replay, new.replay, effects)
    updateStreamPlatforms(stream, new, effects)
    if (creating) {
        return true
    }
    applyStreamUpdateEffects(stream, effects)
    return true
}

private fun Model.updateStreamObs(
    stream: SettingsStream,
    new: SettingsStream,
    effects: StreamUpdateEffects,
) {
    effects.obsWebSocket.apply(
        { stream.obsWebSocketEnabled },
        { stream.obsWebSocketEnabled = it },
        new.obsWebSocketEnabled,
    )
    effects.obsWebSocket.apply(
        { stream.obsWebSocketUrl },
        { stream.obsWebSocketUrl = it },
        cleanUrl(value = new.obsWebSocketUrl),
    )
    effects.obsWebSocket.apply(
        { stream.obsWebSocketPassword },
        { stream.obsWebSocketPassword = it },
        new.obsWebSocketPassword,
    )
    stream.obsSourceName = new.obsSourceName
    stream.obsMainScene = new.obsMainScene
    stream.obsBrbScene = new.obsBrbScene
    stream.obsBrbSceneVideoSourceBroken = new.obsBrbSceneVideoSourceBroken
    stream.obsAutoStartStream = new.obsAutoStartStream
    stream.obsAutoStopStream = new.obsAutoStopStream
    stream.obsAutoStartRecording = new.obsAutoStartRecording
    stream.obsAutoStopRecording = new.obsAutoStopRecording
    stream.streamingDirectlyToObs = new.streamingDirectlyToObs
}

private fun Model.updateStreamVideo(
    stream: SettingsStream,
    new: SettingsStream,
    effects: StreamUpdateEffects,
) {
    effects.reload.apply({ stream.resolution }, { stream.resolution = it }, new.resolution)
    effects.reload.apply({ stream.fps }, { stream.fps = it }, new.fps)
    effects.lowLightBoost = stream.lowLightBoost != new.lowLightBoost
    stream.lowLightBoost = new.lowLightBoost
    effects.reload.apply({ stream.codec }, { stream.codec = it }, new.codec)
    effects.reload.apply({ stream.h264Profile }, { stream.h264Profile = it }, new.h264Profile)
    effects.reload.apply(
        { stream.rateControl },
        { stream.rateControl = it },
        SettingsStreamRateControl.makeValid(new.rateControl),
    )
    effects.bitrate = stream.bitrate != new.bitrate
    stream.bitrate = new.bitrate
    effects.reload.apply(
        { stream.maxKeyFrameInterval },
        { stream.maxKeyFrameInterval = it },
        new.maxKeyFrameInterval,
    )
    effects.reload.apply({ stream.bFrames }, { stream.bFrames = it }, new.bFrames)
    effects.reload.apply({ stream.colorRange }, { stream.colorRange = it }, new.colorRange)
    effects.reload.apply(
        { stream.adaptiveEncoderResolution },
        { stream.adaptiveEncoderResolution = it },
        new.adaptiveEncoderResolution,
    )
    effects.reload.apply(
        { stream.adaptiveEncoderResolutionThreashold },
        { stream.adaptiveEncoderResolutionThreashold = it },
        new.adaptiveEncoderResolutionThreashold,
    )
    effects.timecodes.apply(
        { stream.timecodesEnabled },
        { stream.timecodesEnabled = it },
        new.timecodesEnabled,
    )
    effects.timecodes.apply(
        { stream.ntpPoolAddress },
        { stream.ntpPoolAddress = it },
        new.ntpPoolAddress,
    )
    effects.reload.apply({ stream.audioCodec }, { stream.audioCodec = it }, new.audioCodec)
    effects.audioBitrate = stream.audioBitrate != new.audioBitrate
    stream.audioBitrate = new.audioBitrate
}

private fun Model.updateStreamSrt(
    srt: SettingsStreamSrt,
    newSrt: SettingsStreamSrt,
    effects: StreamUpdateEffects,
) {
    effects.reload.apply({ srt.latency }, { srt.latency = it }, newSrt.latency)
    effects.reload.apply(
        { srt.adaptiveBitrateEnabled },
        { srt.adaptiveBitrateEnabled = it },
        newSrt.adaptiveBitrateEnabled,
    )
    effects.reload.apply(
        { srt.maximumBandwidthFollowInput },
        { srt.maximumBandwidthFollowInput = it },
        newSrt.maximumBandwidthFollowInput,
    )
    effects.reload.apply({ srt.overheadBandwidth }, { srt.overheadBandwidth = it }, newSrt.overheadBandwidth)
    effects.reload.apply({ srt.bigPackets }, { srt.bigPackets = it }, newSrt.bigPackets)
    effects.reload.apply({ srt.implementation }, { srt.implementation = it }, newSrt.implementation)
    srt.dnsLookupStrategy = newSrt.dnsLookupStrategy
    val adaptiveBitrate = srt.adaptiveBitrate
    val newAdaptiveBitrate = newSrt.adaptiveBitrate
    effects.adaptiveBitrateAlgorithm = adaptiveBitrate.algorithm != newAdaptiveBitrate.algorithm
    effects.adaptiveBitrate.apply(
        { adaptiveBitrate.algorithm },
        { adaptiveBitrate.algorithm = it },
        newAdaptiveBitrate.algorithm,
    )
    val fastIrl = adaptiveBitrate.fastIrlSettings
    val newFastIrl = newAdaptiveBitrate.fastIrlSettings
    effects.adaptiveBitrate.apply(
        { fastIrl.packetsInFlight },
        { fastIrl.packetsInFlight = it },
        newFastIrl.packetsInFlight,
    )
    effects.adaptiveBitrate.apply(
        { fastIrl.minimumBitrate },
        { fastIrl.minimumBitrate = it },
        newFastIrl.minimumBitrate,
    )
    val custom = adaptiveBitrate.customSettings
    val newCustom = newAdaptiveBitrate.customSettings
    effects.adaptiveBitrate.apply(
        { custom.packetsInFlight },
        { custom.packetsInFlight = it },
        newCustom.packetsInFlight,
    )
    effects.adaptiveBitrate.apply(
        { custom.pifDiffIncreaseFactor },
        { custom.pifDiffIncreaseFactor = it },
        newCustom.pifDiffIncreaseFactor,
    )
    effects.adaptiveBitrate.apply(
        { custom.rttDiffHighDecreaseFactor },
        { custom.rttDiffHighDecreaseFactor = it },
        newCustom.rttDiffHighDecreaseFactor,
    )
    effects.adaptiveBitrate.apply(
        { custom.rttDiffHighAllowedSpike },
        { custom.rttDiffHighAllowedSpike = it },
        newCustom.rttDiffHighAllowedSpike,
    )
    effects.adaptiveBitrate.apply(
        { custom.rttDiffHighMinimumDecrease },
        { custom.rttDiffHighMinimumDecrease = it },
        newCustom.rttDiffHighMinimumDecrease,
    )
    effects.adaptiveBitrate.apply(
        { custom.minimumBitrate },
        { custom.minimumBitrate = it },
        newCustom.minimumBitrate,
    )
    effects.adaptiveBitrate.apply(
        { adaptiveBitrate.belaboxSettings.minimumBitrate },
        { adaptiveBitrate.belaboxSettings.minimumBitrate = it },
        newAdaptiveBitrate.belaboxSettings.minimumBitrate,
    )
    val connectionPriorities = srt.connectionPriorities
    val newConnectionPriorities = newSrt.connectionPriorities
    effects.connectionPriorities.apply(
        { connectionPriorities.enabled },
        { connectionPriorities.enabled = it },
        newConnectionPriorities.enabled,
    )
    val priorities = newConnectionPriorities.priorities.mapNotNull { newPriority ->
        val priority = connectionPriorities.priorities.firstOrNull { it.id == newPriority.id }
        if (priority != null) {
            effects.connectionPriorities.apply(
                { priority.priority },
                { priority.priority = it },
                newPriority.priority,
            )
            effects.connectionPriorities.apply(
                { priority.enabled },
                { priority.enabled = it },
                newPriority.enabled,
            )
        }
        priority
    }
    effects.connectionPriorities.applyItems(connectionPriorities.priorities, priorities) { it.id }
}

private fun Model.updateStreamMultiStreaming(
    multiStreaming: SettingsStreamMultiStreaming,
    newMultiStreaming: SettingsStreamMultiStreaming,
    effects: StreamUpdateEffects,
) {
    val destinations = newMultiStreaming.destinations.mapIndexed { index, newDestination ->
        val destination = if (index < multiStreaming.destinations.size) {
            multiStreaming.destinations[index]
        } else {
            SettingsStreamMultiStreamingDestination()
        }
        destination.name = newDestination.name
        effects.reload.apply(
            { destination.url },
            { destination.url = it },
            cleanUrl(value = newDestination.url),
        )
        effects.reload.apply(
            { destination.enabled },
            { destination.enabled = it },
            newDestination.enabled,
        )
        destination
    }
    if (destinations.size != multiStreaming.destinations.size) {
        multiStreaming.destinations = destinations.toMutableList()
    }
}

private fun Model.updateStreamRecording(
    recording: SettingsStreamRecording,
    newRecording: SettingsStreamRecording,
    effects: StreamUpdateEffects,
) {
    effects.reload.apply(
        { recording.overrideStream },
        { recording.overrideStream = it },
        newRecording.overrideStream,
    )
    if (recording.overrideStream) {
        effects.reload.apply(
            { recording.resolution },
            { recording.resolution = it },
            newRecording.resolution,
        )
    } else {
        recording.resolution = newRecording.resolution
    }
    recording.videoCodec = newRecording.videoCodec
    recording.videoBitrate = newRecording.videoBitrate
    recording.maxKeyFrameInterval = newRecording.maxKeyFrameInterval
    recording.audioBitrate = newRecording.audioBitrate
    recording.autoStartRecording = newRecording.autoStartRecording
    recording.autoStopRecording = newRecording.autoStopRecording
    effects.cleanRecordings = recording.cleanRecordings != newRecording.cleanRecordings
    recording.cleanRecordings = newRecording.cleanRecordings
    effects.cleanSnapshots = recording.cleanSnapshots != newRecording.cleanSnapshots
    recording.cleanSnapshots = newRecording.cleanSnapshots
}

private fun Model.updateStreamReplay(
    replay: SettingsStreamReplay,
    newReplay: SettingsStreamReplay,
    effects: StreamUpdateEffects,
) {
    effects.replayEnabled = replay.enabled != newReplay.enabled
    replay.enabled = newReplay.enabled
    replay.transitionType = newReplay.transitionType
    replay.inStinger = replay.inStinger.copy(transitionPoint = newReplay.inStinger.transitionPoint)
    replay.outStinger = replay.outStinger.copy(transitionPoint = newReplay.outStinger.transitionPoint)
    replay.postTriggerDelay = newReplay.postTriggerDelay
    var layout = replay.layout
    var x = layout.x
    effects.replayLayout.apply({ x }, { x = it }, newReplay.layout.x)
    var y = layout.y
    effects.replayLayout.apply({ y }, { y = it }, newReplay.layout.y)
    var size = layout.size
    effects.replayLayout.apply({ size }, { size = it }, newReplay.layout.size)
    var alignment = layout.alignment
    effects.replayLayout.apply({ alignment }, { alignment = it }, newReplay.layout.alignment)
    var positioningLock = layout.positioningLock
    effects.replayLayout.apply(
        { positioningLock },
        { positioningLock = it },
        newReplay.layout.positioningLock,
    )
    layout = layout.copy(
        x = x,
        y = y,
        size = size,
        alignment = alignment,
        positioningLock = positioningLock,
    )
    if (effects.replayLayout.changed) {
        layout = layout.updatingXString()
        layout = layout.updatingYString()
        layout = layout.updatingSizeString()
        replay.layout = layout
    }
}

private fun Model.updateStreamPlatforms(
    stream: SettingsStream,
    new: SettingsStream,
    effects: StreamUpdateEffects,
) {
    effects.twitchChannelName = stream.twitchChannelName != new.twitchChannelName
    stream.twitchChannelName = new.twitchChannelName
    effects.twitchChannelId = stream.twitchChannelId != new.twitchChannelId
    stream.twitchChannelId = new.twitchChannelId
    updateTwitchAlerts(stream.twitchChatAlerts, new.twitchChatAlerts)
    updateTwitchAlerts(stream.twitchToastAlerts, new.twitchToastAlerts)
    if (stream.kickChannelName != new.kickChannelName) {
        effects.kickChannelName = true
        stream.kickChannelName = new.kickChannelName
        stream.kickChannelId = null
        stream.kickSlug = null
        stream.kickChatroomChannelId = null
    }
    updateKickAlerts(stream.kickChatAlerts, new.kickChatAlerts)
    updateKickAlerts(stream.kickToastAlerts, new.kickToastAlerts)
    val youTubeVideoIds = new.youTubeVideoIds.removeAllWhitespaces()
    effects.youTubeVideoIds = stream.youTubeVideoIds != youTubeVideoIds
    stream.youTubeVideoIds = youTubeVideoIds
    stream.youTubeHandle = new.youTubeHandle
    stream.youTubeScheduleStreamTitle = new.youTubeScheduleStreamTitle
    stream.youTubeScheduleStreamVisibility = new.youTubeScheduleStreamVisibility
    stream.youTubeScheduleStreamAutoStop = new.youTubeScheduleStreamAutoStop
    effects.soopChannelName = stream.soopChannelName != new.soopChannelName
    stream.soopChannelName = new.soopChannelName
    effects.soopStreamId = stream.soopStreamId != new.soopStreamId
    stream.soopStreamId = new.soopStreamId
    effects.openStreamingPlatformUrl = stream.openStreamingPlatformUrl != new.openStreamingPlatformUrl
    stream.openStreamingPlatformUrl = new.openStreamingPlatformUrl
    effects.openStreamingPlatformChannelId =
        stream.openStreamingPlatformChannelId != new.openStreamingPlatformChannelId
    stream.openStreamingPlatformChannelId = new.openStreamingPlatformChannelId
}

private fun Model.updateTwitchAlerts(alerts: SettingsTwitchAlerts, newAlerts: SettingsTwitchAlerts) {
    alerts.follows = newAlerts.follows
    alerts.subscriptions = newAlerts.subscriptions
    alerts.giftSubscriptions = newAlerts.giftSubscriptions
    alerts.resubscriptions = newAlerts.resubscriptions
    alerts.rewards = newAlerts.rewards
    alerts.raids = newAlerts.raids
    alerts.cheers = newAlerts.cheers
    alerts.minimumCheerBits = newAlerts.minimumCheerBits
    alerts.watchStreaks = newAlerts.watchStreaks
    alerts.minimumWatchStreak = newAlerts.minimumWatchStreak
    alerts.sharedChat = newAlerts.sharedChat
}

private fun Model.updateKickAlerts(alerts: SettingsKickAlerts, newAlerts: SettingsKickAlerts) {
    alerts.subscriptions = newAlerts.subscriptions
    alerts.giftedSubscriptions = newAlerts.giftedSubscriptions
    alerts.rewards = newAlerts.rewards
    alerts.hosts = newAlerts.hosts
    alerts.bans = newAlerts.bans
    alerts.kicks = newAlerts.kicks
    alerts.minimumKicks = newAlerts.minimumKicks
}

private fun Model.applyStreamUpdateEffects(stream: SettingsStream, effects: StreamUpdateEffects) {
    if (effects.connectionPriorities.changed) {
        updateSrtlaPriorities()
    }
    if (effects.lowLightBoost) {
        setStreamFps()
    }
    if (effects.cleanRecordings) {
        setCleanRecordings()
    }
    if (effects.cleanSnapshots) {
        setCleanSnapshots()
    }
    if (effects.replayLayout.changed) {
        replayEffect?.setLayout(layout = stream.replay.layout)
    }
    if (effects.kickChannelName) {
        remoteControlFetchKickChannelInfo(stream = stream)
    }
    if (!stream.enabled) {
        return
    }
    if (effects.pictureInPicture.changed) {
        updatePictureInPicture()
    }
    if (effects.obsWebSocket.changed) {
        obsWebSocketEnabledUpdated()
    }
    if (effects.location.changed) {
        reloadLocation()
    }
    if (effects.chats.changed) {
        reloadChats()
    }
    if (effects.bitrate) {
        setStreamBitrate(stream = stream)
    }
    if (effects.audioBitrate) {
        setAudioStreamBitrate(stream = stream)
    }
    if (effects.timecodes.changed) {
        reloadNtpClient()
        reloadIngests()
    }
    if (effects.adaptiveBitrateAlgorithm && stream.srt.adaptiveBitrateEnabled) {
        setAdaptiveBitrateSrtAlgorithm(stream = stream)
    }
    if (effects.adaptiveBitrate.changed) {
        updateAdaptiveBitrateSrt(srt = stream.srt)
    }
    if (effects.replayEnabled) {
        streamReplayEnabledUpdated()
    }
    if (effects.twitchChannelName) {
        twitchChannelNameUpdated()
    }
    if (effects.twitchChannelId) {
        twitchChannelIdUpdated()
    }
    if (effects.kickChannelName && stream.kickChannelName.isEmpty()) {
        kickChannelNameUpdated()
    }
    if (effects.youTubeVideoIds) {
        youTubeVideoIdUpdated()
    }
    if (effects.soopChannelName) {
        soopChannelNameUpdated()
    }
    if (effects.soopStreamId) {
        soopStreamIdUpdated()
    }
    if (effects.openStreamingPlatformUrl) {
        openStreamingPlatformUrlUpdated()
    }
    if (effects.openStreamingPlatformChannelId) {
        openStreamingPlatformRoomUpdated()
    }
    if (effects.portrait) {
        setCurrentStream(stream = stream)
        reloadStream()
        resetSelectedScene(changeScene = false)
        updateOrientation()
        updateOrientationLock()
    } else if (effects.reload.changed) {
        reloadStreamIfEnabled(stream = stream)
    }
}

private fun Model.remoteControlFetchKickChannelInfo(stream: SettingsStream) {
    val channelName = stream.kickChannelName
    getKickChannelInfo(channelName = channelName) { channelInfo ->
        if (stream.kickChannelName != channelName) {
            return@getKickChannelInfo
        }
        if (channelInfo != null) {
            stream.kickChannelId = channelInfo.chatroom.id.toString()
            stream.kickSlug = channelInfo.slug
            stream.kickChatroomChannelId = channelInfo.chatroom.channel_id.toString()
        }
        if (stream.enabled) {
            kickAccessTokenUpdated()
        }
    }
}

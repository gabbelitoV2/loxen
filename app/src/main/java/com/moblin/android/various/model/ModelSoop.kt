package com.moblin.android.various.model

import com.moblin.android.streamingplatforms.soop.SoopChat
import com.moblin.android.streamingplatforms.soop.SoopPlatformStatus

fun Model.soopChannelNameUpdated() {
    reloadSoopChat()
    resetChat()
    reloadSoopPlatformStatus()
}

fun Model.soopStreamIdUpdated() {
    reloadSoopChat()
    resetChat()
}

fun Model.reloadSoopPlatformStatus() {
    soopPlatformStatus?.stop()
    if (isSoopViewersConfigured()) {
        val platformStatus = SoopPlatformStatus()
        soopPlatformStatus = platformStatus
        platformStatus.start(userId = stream.soopChannelName)
    }
}

fun Model.updateViewersSoop(): StreamingPlatformStatus {
    val platformStatus = soopPlatformStatus?.platformStatus
    return if (platformStatus != null) {
        StreamingPlatformStatus(platform = Platform.Soop, status = platformStatus)
    } else {
        StreamingPlatformStatus(platform = Platform.Soop, status = PlatformStatus.Unknown)
    }
}

fun Model.isSoopChatConfigured(): Boolean {
    return database.chat.enabled && stream.soopChannelName != "" && stream.soopStreamId != ""
}

fun Model.isSoopViewersConfigured(): Boolean {
    return stream.soopChannelName.isNotEmpty()
}

fun Model.isSoopChatConnected(): Boolean {
    return soopChat?.isConnected() ?: false
}

fun Model.hasSoopChatEmotes(): Boolean {
    return soopChat?.hasEmotes() ?: false
}

fun Model.reloadSoopChat() {
    soopChat?.stop()
    soopChat = null
    setTextToSpeechStreamerMentions()
    if (isSoopChatConfigured() && !isRemoteControlChatAndEvents(platform = Platform.Soop)) {
        val chat = SoopChat(
            model = this,
            channelName = stream.soopChannelName,
            streamId = stream.soopStreamId
        )
        soopChat = chat
        chat.start()
    }
    updateChatMoreThanOneChatConfigured()
}

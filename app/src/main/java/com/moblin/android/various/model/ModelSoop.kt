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
        platformStatus.start(userId = stream.value.soopChannelName)
    }
}

fun Model.updateViewersSoop(): StreamingPlatformStatus {
    val platformStatus = soopPlatformStatus?.platformStatus
    return if (platformStatus != null) {
        StreamingPlatformStatus(platform = TODO("Platform.soop"), status = platformStatus)
    } else {
        StreamingPlatformStatus(platform = TODO("Platform.soop"), status = PlatformStatus.unknown)
    }
}

fun Model.isSoopChatConfigured(): Boolean {
    return database.chat.enabled && stream.value.soopChannelName != "" && stream.value.soopStreamId != ""
}

fun Model.isSoopViewersConfigured(): Boolean {
    return stream.value.soopChannelName.isNotEmpty()
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
    if (isSoopChatConfigured() && !isRemoteControlChatAndEvents(platform = TODO("Platform.soop"))) {
        val chat = SoopChat(
            model = this,
            channelName = stream.value.soopChannelName,
            streamId = stream.value.soopStreamId
        )
        soopChat = chat
        chat.start()
    }
    updateChatMoreThanOneChatConfigured()
}

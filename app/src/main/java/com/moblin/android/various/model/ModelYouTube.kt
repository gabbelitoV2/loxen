package com.moblin.android.various.model

import com.moblin.android.localized
import com.moblin.android.streamingplatforms.youtube.YouTubeApi
import com.moblin.android.streamingplatforms.youtube.YouTubeLiveChat
import com.moblin.android.streamingplatforms.youtube.fetchYouTubeVideoId
import com.moblin.android.streamingplatforms.youtube.removeYouTubeAuthStateInKeychain
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.various.utils.getRootViewController
import java.time.Duration
import java.time.Instant
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val mainScope = CoroutineScope(Dispatchers.Main)

class YouTube {
    var session: Any? = null
}

fun Model.youTubeVideoIdUpdated() {
    reloadViewers()
    reloadYouTubeLiveChat()
    resetChat()
}

fun Model.updateViewersYouTube(): StreamingPlatformStatus {
    return StreamingPlatformStatus(Platform.youTube, youTubePlatformStatus)
}

fun Model.youTubeSignIn(stream: SettingsStream) {
    if (getRootViewController() == null) {
        return
    }
    TODO("no Android counterpart for AppAuth")
}

private fun Model.youTubeSignIn(stream: SettingsStream, configuration: Any, rootViewController: Any) {
    TODO("no Android counterpart for AppAuth")
}

private fun Model.makeYouTubeSignInCallback(stream: SettingsStream): (Any?, Any?) -> Unit {
    return { authState, _ ->
        mainScope.launch {
            stream.youTubeAuthState = authState
            stream.youTubeWantsToBeLoggedIn = authState != null
            stream.youTubeNotLoggedInCount = 0
            youTube.session = null
        }
    }
}

fun Model.youTubeSignOut(stream: SettingsStream) {
    stream.youTubeAuthState = null
    stream.youTubeWantsToBeLoggedIn = false
    removeYouTubeAuthStateInKeychain(stream.id)
}

fun Model.makeNotLoggedInToYouTubeToastIfNeeded() {
    if (!stream.youTubeWantsToBeLoggedIn || stream.isYouTubeAuthorized()) {
        return
    }
    stream.youTubeNotLoggedInCount += 1
    if (stream.youTubeNotLoggedInCount >= maxNotLoggedInToastCount) {
        stream.youTubeWantsToBeLoggedIn = false
    }
    makeNotLoggedInToToast(platform = Platform.youTube)
}

fun Model.getYouTubeApi(stream: SettingsStream, onCompleted: (YouTubeApi?) -> Unit) {
    getYouTubeAccesssToken(stream) { accessToken ->
        if (accessToken == null) {
            onCompleted(null)
            return@getYouTubeAccesssToken
        }
        val youTubeApi = YouTubeApi(accessToken)
        youTubeApi.delegate = this
        onCompleted(youTubeApi)
    }
}

fun Model.startFetchingYouTubeChatVideoId() {
    youTubeFetchVideoIdStartTime = Instant.now()
}

fun Model.stopFetchingYouTubeChatVideoId() {
    youTubeFetchVideoIdStartTime = null
}

fun Model.tryToFetchYouTubeVideoId() {
    if (!database.chat.enabled) {
        return
    }
    if (!(stream.isYouTubeAuthorized() || stream.youTubeHandle.isNotEmpty())) {
        return
    }
    val youTubeFetchVideoIdStartTime = this.youTubeFetchVideoIdStartTime ?: return
    if (Duration.between(youTubeFetchVideoIdStartTime, Instant.now()) >= Duration.ofSeconds(60)) {
        stopFetchingYouTubeChatVideoId()
        makeErrorToast(title = localized("Failed to fetch YouTube Video ID"),
                       subTitle = localized("You must be live on YouTube for this to work."))
        return
    }
    if (stream.isYouTubeAuthorized()) {
        getYouTubeApi(stream) { youTubeApi ->
            if (youTubeApi == null) {
                return@getYouTubeApi
            }
            youTubeApi.listLiveBroadcasts(status = "active") { response ->
                response.onSuccess { listResponse ->
                    val videoIds = listResponse.items.map { it.id }
                    if (videoIds.isEmpty()) {
                        return@onSuccess
                    }
                    stopFetchingYouTubeChatVideoId()
                    val newVideoIds = videoIds.joinToString(",")
                    if (newVideoIds == stream.youTubeVideoIds) {
                        return@onSuccess
                    }
                    stream.youTubeVideoIds = newVideoIds
                    if (stream.enabled) {
                        youTubeVideoIdUpdated()
                    }
                }
            }
        }
    } else if (stream.youTubeHandle.isNotEmpty()) {
        mainScope.launch {
            val videoId = runCatching { fetchYouTubeVideoId(stream.youTubeHandle) }.getOrNull()
            if (videoId != null) {
                stopFetchingYouTubeChatVideoId()
                if (videoId == stream.youTubeVideoIds) {
                    return@launch
                }
                stream.youTubeVideoIds = videoId
                if (stream.enabled) {
                    youTubeVideoIdUpdated()
                }
            }
        }
    }
}

fun Model.isYouTubeViewersConfigured(): Boolean {
    return stream.isYouTubeAuthorized() && stream.youTubeVideoIds.isNotEmpty()
}

fun Model.isYouTubeLiveChatConfigured(): Boolean {
    return database.chat.enabled && stream.youTubeVideoIds.isNotEmpty()
}

fun Model.isYouTubeLiveChatConnected(): Boolean {
    for (youTubeLiveChat in youTubeLiveChats.values) {
        if (!youTubeLiveChat.isConnected()) {
            return false
        }
    }
    return true
}

fun Model.hasYouTubeLiveChatEmotes(): Boolean {
    for (youTubeLiveChat in youTubeLiveChats.values) {
        if (!youTubeLiveChat.hasEmotes()) {
            return false
        }
    }
    return true
}

fun Model.reloadYouTubeLiveChat() {
    for (chat in youTubeLiveChats.values) {
        chat.stop()
    }
    youTubeLiveChats.clear()
    if (isYouTubeLiveChatConfigured() && !isRemoteControlChatAndEvents(platform = Platform.youTube)) {
        for (videoId in stream.getYouTubeVideoIds()) {
            val chat = YouTubeLiveChat(this, videoId, stream.chat)
            youTubeLiveChats[videoId] = chat
            chat.start()
        }
    }
    updateChatMoreThanOneChatConfigured()
}

fun Model.updateYouTubeStream(monotonicNow: Long) {
    if (!isLive || !isYouTubeViewersConfigured()) {
        youTubePlatformStatus = StreamingPlatformStatusState.Unknown
        return
    }
    if (monotonicNow - youTubeStreamUpdateTime <= youTubeStreamUpdateTimePollDelta) {
        return
    }
    youTubeStreamUpdateTime = monotonicNow
    youTubeStreamUpdateTimePollDelta = minOf(youTubeStreamUpdateTimePollDelta * 2,
                                              900.seconds.inWholeNanoseconds)
    getVideo()
}

private fun Model.getYouTubeAccesssToken(stream: SettingsStream, onCompleted: (String?) -> Unit) {
    val authState = stream.youTubeAuthState
    if (authState == null) {
        onCompleted(null)
        return
    }
    TODO("no Android counterpart for AppAuth")
}

private fun Model.getVideo() {
    getYouTubeApi(stream) { youTubeApi ->
        youTubeApi?.listVideos(videoIds = stream.youTubeVideoIds) { response ->
            response.onSuccess { response ->
                var totalViewers = 0
                var isLive = false
                for (item in response.items) {
                    val liveStreamingDetails = item.liveStreamingDetails
                    if (liveStreamingDetails.isLive()) {
                        isLive = true
                        totalViewers += (liveStreamingDetails.concurrentViewers ?: "0").toIntOrNull() ?: 0
                    }
                }
                if (isLive) {
                    youTubePlatformStatus = StreamingPlatformStatusState.Live(viewerCount = totalViewers)
                } else if (response.items.isNotEmpty()) {
                    youTubePlatformStatus = StreamingPlatformStatusState.Offline
                } else {
                    youTubePlatformStatus = StreamingPlatformStatusState.Unknown
                }
            }.onFailure {
                youTubePlatformStatus = StreamingPlatformStatusState.Unknown
            }
        }
    }
}

fun Model.youTubeApiUnauthorized() {
    if (!stream.isYouTubeAuthorized()) {
        return
    }
    stream.youTubeAuthState = null
    removeYouTubeAuthStateInKeychain(stream.id)
    makeNotLoggedInToToast(platform = Platform.youTube)
}

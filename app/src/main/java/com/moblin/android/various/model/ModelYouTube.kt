package com.moblin.android.various.model

import com.moblin.android.localized
import com.moblin.android.streamingplatforms.youtube.YouTubeApi
import com.moblin.android.streamingplatforms.youtube.YouTubeApiDelegate
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
import com.moblin.android.streamingplatforms.Platform
import com.moblin.android.various.network.NetworkResponse

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
    if (!stream.value.youTubeWantsToBeLoggedIn || stream.value.isYouTubeAuthorized()) {
        return
    }
    stream.value.youTubeNotLoggedInCount += 1
    if (stream.value.youTubeNotLoggedInCount >= maxNotLoggedInToastCount) {
        stream.value.youTubeWantsToBeLoggedIn = false
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
        val model = this
        youTubeApi.delegate = object : YouTubeApiDelegate {
            override fun youTubeApiUnauthorized() {
                model.youTubeApiUnauthorized()
            }
        }
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
    if (!(stream.value.isYouTubeAuthorized() || stream.value.youTubeHandle.isNotEmpty())) {
        return
    }
    val youTubeFetchVideoIdStartTime = this.youTubeFetchVideoIdStartTime ?: return
    if (Duration.between(youTubeFetchVideoIdStartTime, Instant.now()) >= Duration.ofSeconds(60)) {
        stopFetchingYouTubeChatVideoId()
        makeErrorToast(title = localized("Failed to fetch YouTube Video ID"),
                       subTitle = localized("You must be live on YouTube for this to work."))
        return
    }
    if (stream.value.isYouTubeAuthorized()) {
        getYouTubeApi(stream.value) { youTubeApi ->
            if (youTubeApi == null) {
                return@getYouTubeApi
            }
            youTubeApi.listLiveBroadcasts(status = "active") { response ->
                val listResponse = (response as? NetworkResponse.Success)?.value
                if (listResponse != null) {
                    val videoIds = listResponse.items.map { it.id }
                    if (videoIds.isNotEmpty()) {
                        stopFetchingYouTubeChatVideoId()
                        val newVideoIds = videoIds.joinToString(",")
                        if (newVideoIds != stream.value.youTubeVideoIds) {
                            stream.value.youTubeVideoIds = newVideoIds
                            if (stream.value.enabled) {
                                youTubeVideoIdUpdated()
                            }
                        }
                    }
                }
            }
        }
    } else if (stream.value.youTubeHandle.isNotEmpty()) {
        mainScope.launch {
            val videoId = runCatching { fetchYouTubeVideoId(stream.value.youTubeHandle) }.getOrNull()
            if (videoId != null) {
                stopFetchingYouTubeChatVideoId()
                if (videoId == stream.value.youTubeVideoIds) {
                    return@launch
                }
                stream.value.youTubeVideoIds = videoId
                if (stream.value.enabled) {
                    youTubeVideoIdUpdated()
                }
            }
        }
    }
}

fun Model.isYouTubeViewersConfigured(): Boolean {
    return stream.value.isYouTubeAuthorized() && stream.value.youTubeVideoIds.isNotEmpty()
}

fun Model.isYouTubeLiveChatConfigured(): Boolean {
    return database.chat.enabled && stream.value.youTubeVideoIds.isNotEmpty()
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
        for (videoId in stream.value.getYouTubeVideoIds()) {
            val chat = YouTubeLiveChat(this, videoId, stream.value.chat)
            youTubeLiveChats[videoId] = chat
            chat.start()
        }
    }
    updateChatMoreThanOneChatConfigured()
}

fun Model.updateYouTubeStream(monotonicNow: Instant) {
    if (!isLive.value || !isYouTubeViewersConfigured()) {
        youTubePlatformStatus = PlatformStatus.unknown
        return
    }
    if (Duration.between(youTubeStreamUpdateTime, monotonicNow) <= youTubeStreamUpdateTimePollDelta) {
        return
    }
    youTubeStreamUpdateTime = monotonicNow
    youTubeStreamUpdateTimePollDelta = minOf(youTubeStreamUpdateTimePollDelta.multipliedBy(2),
                                              Duration.ofSeconds(900))
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
    getYouTubeApi(stream.value) { youTubeApi ->
        youTubeApi?.listVideos(videoIds = stream.value.youTubeVideoIds) { response ->
            val videosResponse = (response as? NetworkResponse.Success)?.value
            if (videosResponse != null) {
                var totalViewers = 0
                var isLive = false
                for (item in videosResponse.items) {
                    val liveStreamingDetails = item.liveStreamingDetails
                    if (liveStreamingDetails.isLive()) {
                        isLive = true
                        totalViewers += (liveStreamingDetails.concurrentViewers ?: "0").toIntOrNull() ?: 0
                    }
                }
                if (isLive) {
                    youTubePlatformStatus = PlatformStatus.live(viewerCount = totalViewers)
                } else if (videosResponse.items.isNotEmpty()) {
                    youTubePlatformStatus = PlatformStatus.offline
                } else {
                    youTubePlatformStatus = PlatformStatus.unknown
                }
            } else {
                youTubePlatformStatus = PlatformStatus.unknown
            }
        }
    }
}

fun Model.youTubeApiUnauthorized() {
    if (!stream.value.isYouTubeAuthorized()) {
        return
    }
    stream.value.youTubeAuthState = null
    removeYouTubeAuthStateInKeychain(stream.value.id)
    makeNotLoggedInToToast(platform = Platform.youTube)
}

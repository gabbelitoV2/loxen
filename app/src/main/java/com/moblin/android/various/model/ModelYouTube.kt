package com.moblin.android.various.model

import com.moblin.android.localized
import com.moblin.android.platform.appauth.OIDAuthState
import com.moblin.android.platform.appauth.OIDAuthStateAuthorizationCallback
import com.moblin.android.platform.appauth.OIDAuthorizationRequest
import com.moblin.android.platform.appauth.OIDAuthorizationService
import com.moblin.android.platform.appauth.OIDExternalUserAgentIOS
import com.moblin.android.platform.appauth.OIDExternalUserAgentSession
import com.moblin.android.platform.appauth.OIDResponseTypeCode
import com.moblin.android.platform.appauth.OIDServiceConfiguration
import com.moblin.android.platform.core.ContinuousClock
import com.moblin.android.platform.uikit.UIViewController
import com.moblin.android.streamingplatforms.Platform
import com.moblin.android.streamingplatforms.youtube.YouTubeApi
import com.moblin.android.streamingplatforms.youtube.YouTubeApiDelegate
import com.moblin.android.streamingplatforms.youtube.YouTubeLiveChat
import com.moblin.android.streamingplatforms.youtube.fetchYouTubeVideoId
import com.moblin.android.streamingplatforms.youtube.removeYouTubeAuthStateInKeychain
import com.moblin.android.streamingplatforms.youtube.youTubeClientId
import com.moblin.android.streamingplatforms.youtube.youTubeIssuer
import com.moblin.android.streamingplatforms.youtube.youTubeRedirectUri
import com.moblin.android.streamingplatforms.youtube.youTubeScopes
import com.moblin.android.streamingplatforms.youtube.*
import com.moblin.android.various.network.NetworkResponse
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.various.settings.*
import com.moblin.android.various.utils.getRootViewController
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val mainScope = CoroutineScope(Dispatchers.Main)

open class YouTube {
    open var session: OIDExternalUserAgentSession? = null
}

private class ModelYouTubeApiDelegate(private val model: Model) : YouTubeApiDelegate {
    override fun youTubeApiUnauthorized() {
        model.youTubeApiUnauthorized()
    }
}

fun Model.youTubeVideoIdUpdated() {
    reloadViewers()
    reloadYouTubeLiveChat()
    resetChat()
}

fun Model.updateViewersYouTube(): StreamingPlatformStatus {
    return StreamingPlatformStatus(platform = Platform.youTube, status = youTubePlatformStatus)
}

fun Model.youTubeSignIn(stream: SettingsStream) {
    val rootViewController = getRootViewController() ?: return
    OIDAuthorizationService.discoverConfiguration(forIssuer = youTubeIssuer) { configuration, _ ->
        val serviceConfiguration = configuration ?: return@discoverConfiguration
        youTubeSignIn(stream = stream,
                      configuration = serviceConfiguration,
                      rootViewController = rootViewController)
    }
}

private fun Model.youTubeSignIn(stream: SettingsStream,
                                configuration: OIDServiceConfiguration,
                                rootViewController: UIViewController)
{
    val request = OIDAuthorizationRequest(configuration = configuration,
                                          clientId = youTubeClientId,
                                          clientSecret = null,
                                          scopes = youTubeScopes,
                                          redirectURL = youTubeRedirectUri,
                                          responseType = OIDResponseTypeCode,
                                          additionalParameters = null)
    val userAgent = OIDExternalUserAgentIOS(presenting = rootViewController) ?: return
    youTube.session = OIDAuthState.authState(byPresenting = request,
                                             externalUserAgent = userAgent,
                                             callback = makeYouTubeSignInCallback(stream = stream))
}

private fun Model.makeYouTubeSignInCallback(stream: SettingsStream): OIDAuthStateAuthorizationCallback {
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
    removeYouTubeAuthStateInKeychain(streamId = stream.id)
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
    getYouTubeAccesssToken(stream = stream) { accessToken ->
        if (accessToken == null) {
            onCompleted(null)
            return@getYouTubeAccesssToken
        }
        val youTubeApi = YouTubeApi(accessToken)
        youTubeApi.delegate = ModelYouTubeApiDelegate(this)
        onCompleted(youTubeApi)
    }
}

fun Model.startFetchingYouTubeChatVideoId() {
    youTubeFetchVideoIdStartTime = java.time.Instant.now()
}

fun Model.stopFetchingYouTubeChatVideoId() {
    youTubeFetchVideoIdStartTime = null
}

fun Model.tryToFetchYouTubeVideoId() {
    if (!database.chat.enabled) {
        return
    }
    if (!stream.value.isYouTubeAuthorized() && stream.value.youTubeHandle.isEmpty()) {
        return
    }
    val youTubeFetchVideoIdStartTime = this.youTubeFetchVideoIdStartTime ?: return
    if (java.time.Duration.between(youTubeFetchVideoIdStartTime, java.time.Instant.now()) >= java.time.Duration.ofSeconds(60)) {
        stopFetchingYouTubeChatVideoId()
        makeErrorToast(title = localized("Failed to fetch YouTube Video ID"),
                       subTitle = localized("You must be live on YouTube for this to work."))
        return
    }
    if (stream.value.isYouTubeAuthorized()) {
        getYouTubeApi(stream = stream.value) { youTubeApi ->
            if (youTubeApi == null) {
                return@getYouTubeApi
            }
            youTubeApi.listLiveBroadcasts(status = "active") { response ->
                when (response) {
                    is NetworkResponse.Success -> {
                        val videoIds = response.value.items.map { it.id }
                        if (videoIds.isEmpty()) {
                            return@listLiveBroadcasts
                        }
                        stopFetchingYouTubeChatVideoId()
                        val newVideoIds = videoIds.joinToString(",")
                        if (newVideoIds == stream.value.youTubeVideoIds) {
                            return@listLiveBroadcasts
                        }
                        stream.value.youTubeVideoIds = newVideoIds
                        if (stream.value.enabled) {
                            youTubeVideoIdUpdated()
                        }
                    }
                    else -> {}
                }
            }
        }
    } else if (stream.value.youTubeHandle.isNotEmpty()) {
        mainScope.launch {
            val videoId = runCatching { fetchYouTubeVideoId(handle = stream.value.youTubeHandle) }.getOrNull()
                ?: return@launch
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
            val chat = YouTubeLiveChat(model = this, videoId = videoId, settings = stream.value.chat)
            youTubeLiveChats[videoId] = chat
            chat.start()
        }
    }
    updateChatMoreThanOneChatConfigured()
}

fun Model.updateYouTubeStream(monotonicNow: java.time.Instant) {
    if (!isLive.value || !isYouTubeViewersConfigured()) {
        youTubePlatformStatus = PlatformStatus.unknown
        return
    }
    if (java.time.Duration.between(youTubeStreamUpdateTime, monotonicNow) <= youTubeStreamUpdateTimePollDelta) {
        return
    }
    youTubeStreamUpdateTime = monotonicNow
    youTubeStreamUpdateTimePollDelta = minOf(youTubeStreamUpdateTimePollDelta.multipliedBy(2), java.time.Duration.ofSeconds(900))
    getVideo()
}

private fun Model.getYouTubeAccesssToken(stream: SettingsStream, onCompleted: (String?) -> Unit) {
    val authState = stream.youTubeAuthState
    if (authState == null) {
        onCompleted(null)
        return
    }
    authState.performAction { accessToken, _, error ->
        if (accessToken == null || error != null) {
            onCompleted(null)
            return@performAction
        }
        onCompleted(accessToken)
    }
}

private fun Model.getVideo() {
    getYouTubeApi(stream = stream.value) { youTubeApi ->
        youTubeApi?.listVideos(videoIds = stream.value.youTubeVideoIds) { response ->
            when (response) {
                is NetworkResponse.Success -> {
                    val listResponse = response.value
                    var totalViewers = 0
                    var isLive = false
                    for (item in listResponse.items) {
                        val liveStreamingDetails = item.liveStreamingDetails
                        if (liveStreamingDetails.isLive()) {
                            isLive = true
                            totalViewers += liveStreamingDetails.concurrentViewers?.toIntOrNull() ?: 0
                        }
                    }
                    if (isLive) {
                        youTubePlatformStatus = PlatformStatus.live(viewerCount = totalViewers)
                    } else if (listResponse.items.isNotEmpty()) {
                        youTubePlatformStatus = PlatformStatus.offline
                    } else {
                        youTubePlatformStatus = PlatformStatus.unknown
                    }
                }
                else -> {
                    youTubePlatformStatus = PlatformStatus.unknown
                }
            }
        }
    }
}

fun Model.youTubeApiUnauthorized() {
    if (!stream.value.isYouTubeAuthorized()) {
        return
    }
    stream.value.youTubeAuthState = null
    removeYouTubeAuthStateInKeychain(streamId = stream.value.id)
    makeNotLoggedInToToast(platform = Platform.youTube)
}

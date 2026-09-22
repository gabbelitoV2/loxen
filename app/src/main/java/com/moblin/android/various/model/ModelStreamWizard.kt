package com.moblin.android.various.model

import com.moblin.android.common.various.cleanUrl
import com.moblin.android.common.various.isValidUrl
import com.moblin.android.common.various.isValidWebSocketUrl
import com.moblin.android.localized
import com.moblin.android.streamingplatforms.kick.storeKickAccessTokenInKeychain
import com.moblin.android.streamingplatforms.twitch.storeTwitchAccessTokenInKeychain
import com.moblin.android.various.MoblinSettingsUrl
import com.moblin.android.various.network.DefaultTcpPorts
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.various.settings.SettingsStreamAudioCodec
import com.moblin.android.various.settings.SettingsStreamCodec
import com.moblin.android.various.settings.SettingsStreamRateControl
import com.moblin.android.various.settings.defaultStreamUrl
import java.net.URI
import java.net.URLEncoder

enum class WizardPlatform {
    twitch,
    kick,
    youTube,
    soop,
    custom,
    obs,
    mobcam,
}

enum class WizardNetworkSetup {
    obs,
    belaboxCloudObs,
    direct,
    myServers,
}

enum class WizardCustomProtocol {
    none,
    srt,
    rtmp,
    rist,
    whip;

    fun toDefaultCodec(): SettingsStreamCodec {
        return when (this) {
            WizardCustomProtocol.none -> SettingsStreamCodec.h264avc
            WizardCustomProtocol.srt -> SettingsStreamCodec.h265hevc
            WizardCustomProtocol.rtmp -> SettingsStreamCodec.h264avc
            WizardCustomProtocol.rist -> SettingsStreamCodec.h265hevc
            WizardCustomProtocol.whip -> SettingsStreamCodec.h264avc
        }
    }

    fun toDefaultAudioCodec(): SettingsStreamAudioCodec {
        return when (this) {
            WizardCustomProtocol.none -> SettingsStreamAudioCodec.aac
            WizardCustomProtocol.srt -> SettingsStreamAudioCodec.aac
            WizardCustomProtocol.rtmp -> SettingsStreamAudioCodec.aac
            WizardCustomProtocol.rist -> SettingsStreamAudioCodec.aac
            WizardCustomProtocol.whip -> SettingsStreamAudioCodec.opus
        }
    }
}

private fun Model.cleanWizardUrl(url: String): String {
    var cleanedUrl = cleanUrl(value = url)
    if (isValidUrl(value = cleanedUrl) != null) {
        cleanedUrl = defaultStreamUrl
        makeErrorToast(
            title = localized("Malformed stream URL"),
            subTitle = localized("Using default")
        )
    }
    return cleanedUrl
}

private fun Model.createStreamFromWizardCustomUrl(): String? {
    when (createStreamWizard.customProtocol) {
        WizardCustomProtocol.none -> {}
        WizardCustomProtocol.srt -> {
            val uri = runCatching {
                URI(createStreamWizard.customSrtUrl.trim())
            }.getOrNull()
            if (uri != null) {
                val query = uri.rawQuery
                if (query != null) {
                    val items = query.split("&")
                        .filter { it.substringBefore("=") != "streamid" }
                        .toMutableList()
                    items.add(
                        "streamid=" + URLEncoder.encode(
                            createStreamWizard.customSrtStreamId.trim(),
                            "UTF-8"
                        )
                    )
                    val fullUrl = runCatching {
                        URI(
                            uri.scheme,
                            uri.rawAuthority,
                            uri.rawPath,
                            items.joinToString("&"),
                            uri.rawFragment
                        )
                    }.getOrNull()
                    if (fullUrl != null) {
                        return fullUrl.toString()
                    }
                } else {
                    return uri.toString()
                }
            }
        }
        WizardCustomProtocol.rtmp -> {
            val uri = runCatching {
                URI(
                    createStreamWizard.customRtmpUrl
                        .trim()
                        .trim('/')
                )
            }.getOrNull()
            if (uri != null) {
                val path = (uri.rawPath ?: "") + "/" + createStreamWizard.customRtmpStreamKey.trim()
                return runCatching {
                    URI(uri.scheme, uri.rawAuthority, path, uri.rawQuery, uri.rawFragment).toString()
                }.getOrNull()
            }
        }
        WizardCustomProtocol.rist -> return createStreamWizard.customRistUrl.trim()
        WizardCustomProtocol.whip -> return createStreamWizard.customWhipUrl.trim()
    }
    return null
}

private fun Model.createStreamFromWizardUrl(): String {
    var url = defaultStreamUrl
    when (createStreamWizard.platform) {
        WizardPlatform.custom -> {
            createStreamFromWizardCustomUrl()?.let { url = it }
        }
        WizardPlatform.mobcam -> {
            url = "mobcam://localhost:${DefaultTcpPorts.mobcamStream}"
        }
        else -> {
            when (createStreamWizard.networkSetup) {
                WizardNetworkSetup.obs -> {
                    url = "srt://${createStreamWizard.obsAddress}:${createStreamWizard.obsPort}"
                }
                WizardNetworkSetup.belaboxCloudObs -> {
                    url = createStreamWizard.belaboxUrl
                }
                WizardNetworkSetup.direct -> {
                    val ingestUrl = createStreamWizard.directIngest
                        .trim('/')
                    url = "$ingestUrl/${createStreamWizard.directStreamKey}"
                }
                WizardNetworkSetup.myServers -> {
                    createStreamFromWizardCustomUrl()?.let { url = it }
                }
            }
        }
    }
    return cleanWizardUrl(url = url)
}

fun Model.createStreamFromWizard() {
    val stream = SettingsStream(name = createStreamWizard.name.trim())
    stream.backgroundStreaming = createStreamWizard.backgroundStreaming
    stream.autoGoLive = createStreamWizard.autoGoLive
    stream.goLiveNotificationMoblinWebsite = createStreamWizard.goLiveNotificationMoblinWebsite
    if (createStreamWizard.platform != WizardPlatform.custom) {
        if (createStreamWizard.networkSetup != WizardNetworkSetup.direct) {
            if (createStreamWizard.obsRemoteControlEnabled) {
                val url = cleanUrl(value = createStreamWizard.obsRemoteControlUrl.trim())
                if (isValidWebSocketUrl(value = url) == null) {
                    stream.obsWebSocketEnabled = true
                    stream.obsWebSocketUrl = url
                    stream.obsWebSocketPassword = createStreamWizard.obsRemoteControlPassword.trim()
                    stream.obsSourceName = createStreamWizard.obsRemoteControlSourceName.trim()
                    stream.obsMainScene = createStreamWizard.obsRemoteControlMainScene.trim()
                    stream.obsBrbScene = createStreamWizard.obsRemoteControlBrbScene.trim()
                    stream.streamingDirectlyToObs = createStreamWizard.networkSetup == WizardNetworkSetup.obs
                }
            }
        }
    }
    when (createStreamWizard.platform) {
        WizardPlatform.twitch -> {
            stream.twitchChannelName = createStreamWizard.twitchChannelName.trim()
            stream.twitchChannelId = createStreamWizard.twitchChannelId.trim()
            stream.twitchAccessToken = createStreamWizard.twitchAccessToken
            stream.twitchLoggedIn = createStreamWizard.twitchLoggedIn
            stream.twitchWantsToBeLoggedIn = createStreamWizard.twitchLoggedIn
            if (stream.twitchLoggedIn && stream.twitchAccessToken.isNotEmpty()) {
                storeTwitchAccessTokenInKeychain(
                    streamId = stream.id,
                    accessToken = stream.twitchAccessToken
                )
            }
        }
        WizardPlatform.kick -> {
            stream.kickChannelName = createStreamWizard.kickChannelName.trim()
            stream.kickAccessToken = createStreamWizard.kickAccessToken
            stream.kickLoggedIn = createStreamWizard.kickLoggedIn
            stream.kickWantsToBeLoggedIn = createStreamWizard.kickLoggedIn
            if (stream.kickLoggedIn && stream.kickAccessToken.isNotEmpty()) {
                storeKickAccessTokenInKeychain(
                    streamId = stream.id,
                    accessToken = stream.kickAccessToken
                )
            }
            stream.kickChannelId = createStreamWizard.kickChannelId
            stream.kickSlug = createStreamWizard.kickSlug
            stream.kickChatroomChannelId = createStreamWizard.kickChatroomChannelId
        }
        WizardPlatform.youTube -> {
            stream.youTubeHandle = createStreamWizard.youTubeHandle.trim()
            stream.youTubeAuthState = createStreamWizard.youTubeStream.youTubeAuthState
            stream.youTubeWantsToBeLoggedIn = createStreamWizard.youTubeStream.youTubeWantsToBeLoggedIn
        }
        WizardPlatform.soop -> {
            if (createStreamWizard.soopChannelName.isNotEmpty() &&
                createStreamWizard.soopStreamId.isNotEmpty()
            ) {
                stream.soopChannelName = createStreamWizard.soopChannelName.trim()
                stream.soopStreamId = createStreamWizard.soopStreamId.trim()
            }
        }
        WizardPlatform.obs -> {}
        WizardPlatform.custom -> {}
        WizardPlatform.mobcam -> {}
    }
    stream.chat.bttvEmotes = false
    stream.chat.ffzEmotes = false
    stream.chat.seventvEmotes = false
    stream.url = createStreamFromWizardUrl()
    if (stream.url.startsWith("rtmp") || stream.url.startsWith("mobcam")) {
        stream.rateControl = SettingsStreamRateControl.cbr
    } else {
        stream.rateControl = SettingsStreamRateControl.abr
    }
    stream.audioBitrate = 128_000
    when (createStreamWizard.platform) {
        WizardPlatform.custom -> {
            stream.codec = createStreamWizard.customProtocol.toDefaultCodec()
            stream.audioCodec = createStreamWizard.customProtocol.toDefaultAudioCodec()
        }
        WizardPlatform.mobcam -> {
            stream.codec = SettingsStreamCodec.h265hevc
            stream.bitrate = database.getHighestBitratePreset()
            stream.audioCodec = SettingsStreamAudioCodec.aac
            stream.audioBitrate = 192_000
        }
        else -> {
            when (createStreamWizard.networkSetup) {
                WizardNetworkSetup.obs -> {
                    stream.codec = SettingsStreamCodec.h265hevc
                }
                WizardNetworkSetup.belaboxCloudObs -> {
                    stream.codec = SettingsStreamCodec.h265hevc
                }
                WizardNetworkSetup.direct -> {
                    stream.codec = SettingsStreamCodec.h264avc
                }
                WizardNetworkSetup.myServers -> {
                    stream.codec = createStreamWizard.customProtocol.toDefaultCodec()
                    stream.audioCodec = createStreamWizard.customProtocol.toDefaultAudioCodec()
                }
            }
        }
    }
    database.streams.add(stream)
    setCurrentStream(stream = stream)
    reloadStream()
    sceneUpdated(attachCamera = true, updateRemoteScene = false)
    startStreamIfAutoGoLive()
}

fun Model.resetWizard() {
    createStreamWizard.platform = WizardPlatform.custom
    createStreamWizard.networkSetup = WizardNetworkSetup.direct
    createStreamWizard.name = ""
    createStreamWizard.backgroundStreaming = false
    createStreamWizard.autoGoLive = false
    createStreamWizard.goLiveNotificationMoblinWebsite = false
    createStreamWizard.twitchChannelName = ""
    createStreamWizard.twitchChannelId = ""
    createStreamWizard.twitchAccessToken = ""
    createStreamWizard.twitchLoggedIn = false
    createStreamWizard.kickChannelName = ""
    createStreamWizard.kickAccessToken = ""
    createStreamWizard.kickLoggedIn = false
    createStreamWizard.kickChannelId = null
    createStreamWizard.kickSlug = null
    createStreamWizard.kickChatroomChannelId = null
    createStreamWizard.youTubeHandle = ""
    createStreamWizard.youTubeStream.youTubeAuthState = null
    createStreamWizard.soopChannelName = ""
    createStreamWizard.soopStreamId = ""
    createStreamWizard.obsAddress = ""
    createStreamWizard.obsPort = ""
    createStreamWizard.obsRemoteControlEnabled = false
    createStreamWizard.obsRemoteControlUrl = ""
    createStreamWizard.obsRemoteControlPassword = ""
    createStreamWizard.directIngest = ""
    createStreamWizard.directStreamKey = ""
    createStreamWizard.belaboxUrl = ""
}

fun Model.handleSettingsUrlsInWizard(settings: MoblinSettingsUrl) {
    when (createStreamWizard.networkSetup) {
        WizardNetworkSetup.obs -> {}
        WizardNetworkSetup.belaboxCloudObs -> {
            for (stream in settings.streams ?: emptyList()) {
                createStreamWizard.name = stream.name
                createStreamWizard.belaboxUrl = stream.url
            }
        }
        WizardNetworkSetup.direct -> {}
        WizardNetworkSetup.myServers -> {}
    }
}

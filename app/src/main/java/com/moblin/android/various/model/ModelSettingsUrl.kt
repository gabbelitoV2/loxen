package com.moblin.android.various.model

import android.net.Uri
import com.moblin.android.localized
import com.moblin.android.common.various.isValidAudioBitrate
import com.moblin.android.various.MoblinSettingsUrl
import com.moblin.android.various.MoblinSettingsUrlStream
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.various.settings.fpss
import com.moblin.android.various.utils.makeUniqueName

private fun streamImportCollisionTitle(names: Set<String>): String {
    if (names.size <= 1) {
        return String.format(
            localized("A stream named ‘%s’ already exists."),
            names.firstOrNull() ?: ""
        )
    }
    val joined = names.joinToString(separator = ", ") { "‘$it’" }
    return String.format(localized("Streams named %s already exist."), joined)
}

private fun Model.handleSettingsUrlsDefaultStreams(settings: MoblinSettingsUrl,
                                                   replaceCollisions: Boolean) {
    var newSelectedStream: SettingsStream? = null
    for (stream in settings.streams ?: emptyList()) {
        val targetStream: SettingsStream
        val existingStream = database.streams.firstOrNull { it.name == stream.name }
        if (replaceCollisions && existingStream != null) {
            targetStream = existingStream
            if (targetStream.enabled && newSelectedStream == null) {
                newSelectedStream = targetStream
            }
        } else {
            targetStream = SettingsStream(
                name = makeUniqueName(name = stream.name, existingNames = database.streams)
            )
            database.streams.add(targetStream)
        }
        targetStream.url = stream.url.trim()
        if (stream.selected == true) {
            newSelectedStream = targetStream
        }
        stream.backgroundStreaming?.let { targetStream.backgroundStreaming = it }
        stream.backgroundStreamingPiP?.let { targetStream.backgroundStreamingPiP = it }
        stream.video?.let { video ->
            video.resolution?.let { targetStream.resolution = it }
            video.fps?.let { if (fpss.contains(it)) targetStream.fps = it }
            video.bitrate?.let { if (it >= 50000 && it <= 50_000_000) targetStream.bitrate = it }
            video.codec?.let { targetStream.codec = it }
            video.bFrames?.let { targetStream.bFrames = it }
            video.maxKeyFrameInterval?.let { if (it >= 0 && it <= 10) targetStream.maxKeyFrameInterval = it }
        }
        stream.audio?.let { audio ->
            audio.bitrate?.let { if (isValidAudioBitrate(it)) targetStream.audioBitrate = it }
        }
        stream.srt?.let { srt ->
            srt.latency?.let { targetStream.srt.latency = it }
            srt.adaptiveBitrateEnabled?.let { targetStream.srt.adaptiveBitrateEnabled = it }
            srt.dnsLookupStrategy?.let { targetStream.srt.dnsLookupStrategy = it }
        }
        stream.obs?.let { obs ->
            targetStream.obsWebSocketEnabled = true
            targetStream.obsWebSocketUrl = obs.webSocketUrl.trim()
            targetStream.obsWebSocketPassword = obs.webSocketPassword.trim()
        }
        stream.twitch?.let { twitch ->
            targetStream.twitchChannelName = twitch.channelName.trim()
            targetStream.twitchChannelId = twitch.channelId.trim()
        }
        stream.kick?.let { kick ->
            targetStream.kickChannelName = kick.channelName.trim()
        }
    }
    newSelectedStream?.let { stream ->
        setCurrentStream(stream = stream)
        reloadStreamIfEnabled(stream = stream)
    }
}

private fun Model.handleSettingsUrlsDefaultQuickButtons(settings: MoblinSettingsUrl) {
    val quickButtons = settings.quickButtons ?: return
    quickButtons.twoColumns?.let { database.quickButtonsGeneral.twoColumns = it }
    quickButtons.showName?.let { database.quickButtonsGeneral.showName = it }
    quickButtons.enableScroll?.let { database.quickButtonsGeneral.enableScroll = it }
    if (quickButtons.disableAllButtons == true) {
        for (databaseQuickButton in database.quickButtons) {
            databaseQuickButton.enabled = false
        }
    }
    for (quickButton in quickButtons.buttons ?: emptyList()) {
        val databaseQuickButton = database.quickButtons.firstOrNull { quickButton.type == it.type }
        if (databaseQuickButton != null) {
            quickButton.enabled?.let { databaseQuickButton.enabled = it }
            quickButton.page?.let { databaseQuickButton.page = it }
        }
    }
}

private fun Model.handleSettingsUrlsDefaultWebBrowser(settings: MoblinSettingsUrl) {
    val webBrowser = settings.webBrowser ?: return
    webBrowser.home?.let { database.webBrowser.home = it }
}

private fun Model.handleSettingsUrlsDefaultRemoteControl(settings: MoblinSettingsUrl) {
    val remoteControl = settings.remoteControl ?: return
    remoteControl.assistant?.let { assistant ->
        database.remoteControl.assistant.enabled = assistant.enabled
        database.remoteControl.assistant.port = assistant.port
        assistant.relay?.let { relay ->
            database.remoteControl.assistant.relay.enabled = relay.enabled
            database.remoteControl.assistant.relay.baseUrl = relay.baseUrl.trim()
            database.remoteControl.assistant.relay.bridgeId = relay.bridgeId.trim()
        }
    }
    remoteControl.streamer?.let { streamer ->
        database.remoteControl.streamer.enabled = streamer.enabled
        database.remoteControl.streamer.url = streamer.url.trim()
    }
    database.remoteControl.password = remoteControl.password
    reloadRemoteControlStreamer()
    reloadRemoteControlAssistant()
    reloadRemoteControlRelay()
}

private fun Model.handleSettingsUrlsDefault(settings: MoblinSettingsUrl) {
    val collisions = findCollidingStreamNames(settings.streams ?: emptyList())
    if (collisions.isEmpty()) {
        handleSettingsUrlsDefaultStreamCollisions(
            settings = settings,
            replaceStreamCollisions = false
        )
    } else {
        pendingStreamImportCollisionTitle = streamImportCollisionTitle(collisions)
        pendingStreamImportCollisionAction = { replaceStreamCollisions ->
            this.handleSettingsUrlsDefaultStreamCollisions(
                settings = settings,
                replaceStreamCollisions = replaceStreamCollisions
            )
        }
        presentingStreamImportCollisionConfirmation = true
    }
}

private fun Model.handleSettingsUrlsDefaultStreamCollisions(settings: MoblinSettingsUrl,
                                                            replaceStreamCollisions: Boolean) {
    handleSettingsUrlsDefaultStreams(
        settings = settings,
        replaceCollisions = replaceStreamCollisions
    )
    handleSettingsUrlsDefaultQuickButtons(settings = settings)
    handleSettingsUrlsDefaultWebBrowser(settings = settings)
    handleSettingsUrlsDefaultRemoteControl(settings = settings)
    makeToast(title = localized("URL import successful"))
    updateQuickButtonPairs()
}

private fun Model.findCollidingStreamNames(streams: List<MoblinSettingsUrlStream>): Set<String> {
    val collisions = mutableSetOf<String>()
    for (stream in streams) {
        if (database.streams.any { stream.name == it.name }) {
            collisions.add(stream.name)
        }
    }
    return collisions
}

fun Model.handleSettingsUrls(urls: Set<Uri>) {
    if (isLive || isRecording) {
        makeErrorToast(title = localized("Cannot import settings when live or recording"))
        return
    }
    for (url in urls) {
        val pathExtension = url.lastPathSegment?.substringAfterLast('.')
        if (url.scheme == "file" &&
            pathExtension?.equals("moblinSettings", ignoreCase = true) == true
        ) {
            importSettingsWithConfirmation {
                this.handleSettingsFileImport(url = url)
            }
        } else {
            val message = handleSettingsUrlWithConfirmation(url = url)
            if (message != null) {
                makeErrorToast(
                    title = localized("URL import failed"),
                    subTitle = message
                )
            }
        }
    }
}

private fun Model.handleSettingsFileImport(url: Uri) {
    if (isLive || isRecording) {
        return
    }
    TODO("no Android counterpart for security-scoped resource access")
    importSettingsFromFile(url) { _ ->
        TODO("no Android counterpart for security-scoped resource access")
    }
}

private fun Model.handleSettingsUrlWithConfirmation(url: Uri): String? {
    if (!url.path.isNullOrEmpty()) {
        return "Custom URL path is not empty"
    }
    val query = url.query ?: return "Custom URL query is missing"
    val settings = try {
        MoblinSettingsUrl.fromString(query)
    } catch (e: Exception) {
        return e.message
    }
    if (createStreamWizard.presenting || createStreamWizard.presentingSetup) {
        handleSettingsUrlsInWizard(settings = settings)
    } else {
        importSettingsWithConfirmation {
            this@handleSettingsUrlWithConfirmation.handleSettingsUrlsDefault(settings = settings)
        }
    }
    return null
}

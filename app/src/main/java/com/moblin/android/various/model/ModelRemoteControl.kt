package com.moblin.android.various.model

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.media.MediaMetadataRetriever
import android.os.PowerManager
import com.moblin.android.localized
import com.moblin.android.common.various.RgbColor
import com.moblin.android.common.various.formatAudioLevel
import com.moblin.android.common.various.formatAudioLevelChannels
import com.moblin.android.common.various.formatBytes
import com.moblin.android.common.various.noValue
import com.moblin.android.remotecontrol.RemoteControlAssistant
import com.moblin.android.remotecontrol.RemoteControlAssistantDelegate
import com.moblin.android.remotecontrol.RemoteControlAssistantStreamerState
import com.moblin.android.remotecontrol.RemoteControlChatMessage
import com.moblin.android.remotecontrol.RemoteControlFilter
import com.moblin.android.remotecontrol.RemoteControlGolfScoreboard
import com.moblin.android.remotecontrol.RemoteControlMacro
import com.moblin.android.remotecontrol.RemoteControlReaction
import com.moblin.android.remotecontrol.RemoteControlRelay
import com.moblin.android.remotecontrol.RemoteControlRemoteSceneData
import com.moblin.android.remotecontrol.RemoteControlRemoteSceneDataLocation
import com.moblin.android.remotecontrol.RemoteControlRemoteSceneDataVariables
import com.moblin.android.remotecontrol.RemoteControlRemoteSceneSettings
import com.moblin.android.remotecontrol.RemoteControlScoreboardMatchConfig
import com.moblin.android.remotecontrol.RemoteControlSettings
import com.moblin.android.remotecontrol.RemoteControlSettingsAutoSceneSwitcher
import com.moblin.android.remotecontrol.RemoteControlSettingsBitratePreset
import com.moblin.android.remotecontrol.RemoteControlSettingsGimbalPreset
import com.moblin.android.remotecontrol.RemoteControlSettingsMic
import com.moblin.android.remotecontrol.RemoteControlSettingsScene
import com.moblin.android.remotecontrol.RemoteControlSettingsSrt
import com.moblin.android.remotecontrol.RemoteControlSettingsSrtConnectionPriority
import com.moblin.android.remotecontrol.RemoteControlSettingsStream
import com.moblin.android.remotecontrol.RemoteControlStartStatsFilter
import com.moblin.android.remotecontrol.RemoteControlStartStatusFilter
import com.moblin.android.remotecontrol.RemoteControlStateAutoSceneSwitcher
import com.moblin.android.remotecontrol.RemoteControlStats
import com.moblin.android.remotecontrol.RemoteControlStatusGeneral
import com.moblin.android.remotecontrol.RemoteControlStatusGeneralFlame
import com.moblin.android.remotecontrol.RemoteControlStatusItem
import com.moblin.android.remotecontrol.RemoteControlStatusTopLeft
import com.moblin.android.remotecontrol.RemoteControlStatusTopRight
import com.moblin.android.remotecontrol.RemoteControlStatusTopRightAudioInfo
import com.moblin.android.remotecontrol.RemoteControlStatusTopRightAudioLevel
import com.moblin.android.remotecontrol.RemoteControlStreamer
import com.moblin.android.remotecontrol.RemoteControlStreamerDelegate
import com.moblin.android.remotecontrol.RemoteControlWeb
import com.moblin.android.remotecontrol.RemoteControlWebDelegate
import com.moblin.android.remotecontrol.RemoteControlZoomPreset
import com.moblin.android.remotecontrol.remoteControlStartStatsFilterAllEnabled
import com.moblin.android.various.ChatHighlight
import com.moblin.android.various.Variables
import com.moblin.android.various.makeChatPostTextSegments
import com.moblin.android.various.settings.SettingsGimbalMotion
import com.moblin.android.various.settings.SettingsHttpHeader
import com.moblin.android.various.settings.SettingsQuickButtonType
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetScoreboard
import com.moblin.android.various.settings.SettingsWidgetType
import com.moblin.android.various.utils.emojiFlag
import com.moblin.android.view.settings.streams.stream.srt.clampConnectionPriority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URI
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import com.moblin.android.AppDelegate

class RemoteControl {
    val general = MutableStateFlow<RemoteControlStatusGeneral?>(null)
    val topLeft = MutableStateFlow<RemoteControlStatusTopLeft?>(null)
    val topRight = MutableStateFlow<RemoteControlStatusTopRight?>(null)
    val settings = MutableStateFlow<RemoteControlSettings?>(null)
    val scene = MutableStateFlow(UUID.randomUUID())
    val autoSceneSwitcher = MutableStateFlow<UUID?>(null)
    val mic = MutableStateFlow("")
    val bitrate = MutableStateFlow(UUID.randomUUID())
    val zoom = MutableStateFlow("")
    val zoomPresets = MutableStateFlow<List<RemoteControlZoomPreset>>(emptyList())
    val gimbalPresets = MutableStateFlow<List<RemoteControlSettingsGimbalPreset>>(emptyList())
    val macros = MutableStateFlow<List<RemoteControlMacro>>(emptyList())
    val zoomPreset = MutableStateFlow(UUID.randomUUID())
    val debugLogging = MutableStateFlow(false)
    val preview = MutableStateFlow<Bitmap?>(null)
    val recording = MutableStateFlow(false)
    val streaming = MutableStateFlow(false)
    val muted = MutableStateFlow(false)
    val stealthMode = MutableStateFlow(false)
    val previewStream = MutableStateFlow(false)
    val presentingPreview = MutableStateFlow(true)
    val presentingPreviewFullScreen = MutableStateFlow(false)
    val presentingStreamers = MutableStateFlow(false)
    val pixellate = MutableStateFlow(false)
    val movie = MutableStateFlow(false)
    val grayScale = MutableStateFlow(false)
    val sepia = MutableStateFlow(false)
    val triple = MutableStateFlow(false)
    val twin = MutableStateFlow(false)
    val fourThree = MutableStateFlow(false)
    val crt = MutableStateFlow(false)
    val pinch = MutableStateFlow(false)
    val whirlpool = MutableStateFlow(false)
    val poll = MutableStateFlow(false)
    val blurFaces = MutableStateFlow(false)
    val privacy = MutableStateFlow(false)
    val beauty = MutableStateFlow(false)
    val moblinInMouth = MutableStateFlow(false)
    val cameraMan = MutableStateFlow(false)
}

enum class RemoteControlAssistantPreviewUser {
    panel,
    watch,
}

fun Model.isShowingStatusRemoteControl(): Boolean {
    return database.show.remoteControl && isAnyRemoteControlConfigured()
}

private fun Model.isAnyRemoteControlConfigured(): Boolean {
    return isRemoteControlStreamerConfigured() || isRemoteControlAssistantConfigured()
}

fun Model.clearRemoteControlAssistantLog() {
    remoteControlAssistantLog.clear()
}

fun Model.reloadRemoteControlStreamer() {
    remoteControlStreamer?.stop()
    remoteControlStreamer = null
    if (!isRemoteControlStreamerConfigured()) {
        reloadTwitchEventSub()
        reloadChats()
        return
    }
    val url = runCatching { URI(database.remoteControl.streamer.url) }.getOrNull()
    if (url == null) {
        reloadTwitchEventSub()
        reloadChats()
        return
    }
    remoteControlStreamer = RemoteControlStreamer(
        clientUrl = url.toString(),
        password = database.remoteControl.password,
        delegate = RemoteControlStreamerDelegateAdapter(this)
    )
    remoteControlStreamer?.start()
}

private fun Model.remoteControlStreamerSendTwitchStart() {
    remoteControlStreamer?.twitchStart(
        channelName = stream.value.twitchChannelName,
        channelId = stream.value.twitchChannelId,
        accessToken = stream.value.twitchAccessToken
    )
}

fun Model.updateRemoteControlStatus() {
    val status: String
    val ok: Boolean
    if (isRemoteControlAssistantConnected() && isRemoteControlStreamerConnected()) {
        status = localized("Assistant and streamer")
        ok = true
    } else if (isRemoteControlAssistantConnected()) {
        status = localized("Assistant")
        ok = true
    } else if (isRemoteControlStreamerConnected()) {
        status = localized("Streamer")
        ok = true
    } else {
        val assistantError = remoteControlAssistant?.connectionErrorMessage ?: ""
        val streamerError = remoteControlStreamer?.connectionErrorMessage ?: ""
        if (isRemoteControlAssistantConfigured() && isRemoteControlStreamerConfigured()) {
            status = "$assistantError, $streamerError"
        } else if (isRemoteControlAssistantConfigured()) {
            status = assistantError
        } else if (isRemoteControlStreamerConfigured()) {
            status = streamerError
        } else {
            status = noValue
        }
        ok = false
    }
    if (status != statusTopRight.remoteControlStatus.value) {
        statusTopRight.remoteControlStatus.value = status
    }
    if (ok != statusTopRight.remoteControlOk.value) {
        statusTopRight.remoteControlOk.value = ok
    }
}

fun Model.isRemoteControlStreamerConfigured(): Boolean {
    val streamer = database.remoteControl.streamer
    return streamer.enabled && streamer.url.isNotEmpty() && database.remoteControl.password.isNotEmpty()
}

fun Model.isRemoteControlStreamerConnected(): Boolean {
    return remoteControlStreamer?.isConnected() ?: false
}

fun Model.stopRemoteControlAssistant() {
    remoteControlAssistant?.stop()
    remoteControlAssistant = null
}

fun Model.reloadRemoteControlAssistant() {
    stopRemoteControlAssistant()
    if (!isRemoteControlAssistantConfigured()) {
        return
    }
    remoteControlAssistant = RemoteControlAssistant(
        port = database.remoteControl.assistant.port,
        password = database.remoteControl.password,
        delegate = RemoteControlAssistantDelegateAdapter(this)
    )
    remoteControlAssistant?.start()
}

fun Model.isRemoteControlAssistantConnected(): Boolean {
    return remoteControlAssistant?.isConnected() ?: false
}

fun Model.updateRemoteControlAssistantStatus() {
    if (!(showingRemoteControl.value || isWatchRemoteControl()) || !isRemoteControlAssistantConnected()) {
        return
    }
    remoteControlAssistant?.getStatus { general, topLeft, topRight ->
        remoteControl.general.value = general
        remoteControl.topLeft.value = topLeft
        remoteControl.topRight.value = topRight
        if (isWatchRemoteControl()) {
            sendRemoteControlAssistantStatusToWatch()
        }
    }
    remoteControlAssistant?.getSettings { settings ->
        remoteControl.settings.value = settings
    }
}

fun Model.isRemoteControlAssistantConfigured(): Boolean {
    val assistant = database.remoteControl.assistant
    return assistant.enabled && assistant.port > 0 && database.remoteControl.password.isNotEmpty()
}

fun Model.remoteControlAssistantSetRemoteSceneSettings() {
    val data = RemoteControlRemoteSceneSettings.fromSettings(
        scenes = database.scenes,
        widgets = database.widgets,
        selectedSceneId = database.remoteSceneId
    )
    remoteControlAssistant?.setRemoteSceneSettings(data = data) {}
}

private fun Model.shouldSendRemoteScene(): Boolean {
    return database.remoteSceneId != null && remoteControlAssistant?.isConnected() == true
}

fun Model.remoteControlAssistantSetRemoteSceneDataVariables(variables: Variables) {
    if (!shouldSendRemoteScene()) {
        return
    }
    val data = RemoteControlRemoteSceneData(
        textStats = RemoteControlRemoteSceneDataVariables(variables = variables)
    )
    remoteControlAssistant?.setRemoteSceneData(data = data) {}
}

fun Model.remoteControlAssistantSetRemoteSceneDataLocation(location: Location) {
    if (!shouldSendRemoteScene()) {
        return
    }
    val data = RemoteControlRemoteSceneData(
        location = RemoteControlRemoteSceneDataLocation(location = location)
    )
    remoteControlAssistant?.setRemoteSceneData(data = data) {}
}

fun Model.remoteControlAssistantSetLive(on: Boolean) {
    remoteControlAssistant?.setLive(on = on) {
        updateRemoteControlAssistantStatus()
    }
}

fun Model.remoteControlAssistantSetRecord(on: Boolean) {
    remoteControlAssistant?.setRecord(on = on) {
        updateRemoteControlAssistantStatus()
    }
}

fun Model.remoteControlAssistantSetMute(on: Boolean) {
    remoteControlAssistant?.setMute(on = on) {
        updateRemoteControlAssistantStatus()
    }
}

fun Model.remoteControlAssistantSetStealthMode(on: Boolean) {
    remoteControlAssistant?.setStealthMode(on = on) {}
}

fun Model.remoteControlAssistantSetPreviewStream(on: Boolean) {
    remoteControlAssistant?.setPreviewStream(on = on) {
        updateRemoteControlAssistantStatus()
    }
}

fun Model.remoteControlAssistantSetScene(id: UUID) {
    remoteControlAssistant?.setScene(id = id) {
        updateRemoteControlAssistantStatus()
    }
}

fun Model.remoteControlAssistantSetAutoSceneSwitcher(id: UUID?) {
    remoteControlAssistant?.setAutoSceneSwitcher(id = id) {
        updateRemoteControlAssistantStatus()
    }
}

fun Model.remoteControlAssistantSetMic(id: String) {
    remoteControlAssistant?.setMic(id = id) {
        updateRemoteControlAssistantStatus()
    }
}

fun Model.remoteControlAssistantSetZoom(x: Float) {
    remoteControlAssistant?.setZoom(x = x) {}
}

fun Model.remoteControlAssistantSetZoomPreset(id: UUID) {
    remoteControlAssistant?.setZoomPreset(id = id) {}
}

fun Model.remoteControlAssistantSetBitratePreset(id: UUID) {
    remoteControlAssistant?.setBitratePreset(id = id) {
        updateRemoteControlAssistantStatus()
    }
}

fun Model.remoteControlAssistantSetDebugLogging(on: Boolean) {
    remoteControlAssistant?.setDebugLogging(on = on) {}
}

fun Model.remoteControlAssistantReloadBrowserWidgets() {
    remoteControlAssistant?.reloadBrowserWidgets {
        makeToast(title = localized("Browser widgets reloaded"))
    }
}

fun Model.remoteControlAssistantSetSrtConnectionPriorityEnabled(enabled: Boolean) {
    remoteControlAssistant?.setSrtConnectionPrioritiesEnabled(
        enabled = enabled
    ) {}
}

fun Model.remoteControlAssistantSetSrtConnectionPriority(priority: RemoteControlSettingsSrtConnectionPriority) {
    remoteControlAssistant?.setSrtConnectionPriority(
        id = priority.id,
        priority = priority.priority,
        enabled = priority.enabled
    ) {}
}

fun Model.remoteControlAssistantMoveToGimbalPreset(id: UUID) {
    remoteControlAssistant?.moveToGimbalPreset(id = id) {}
}

fun Model.remoteControlAssistantStartMacro(id: UUID) {
    remoteControlAssistant?.startMacro(id = id) {}
}

fun Model.remoteControlAssistantStopMacro(id: UUID) {
    remoteControlAssistant?.stopMacro(id = id) {}
}

fun Model.remoteControlAssistantSetFilter(filter: RemoteControlFilter, on: Boolean) {
    remoteControlAssistant?.setFilter(filter = filter, on = on)
}

fun Model.remoteControlAssistantSendMessage(text: String) {
    remoteControlAssistant?.sendMessage(text = text)
}

fun Model.remoteControlAssistantStartPreview(user: RemoteControlAssistantPreviewUser) {
    remoteControlAssistantPreviewUsers.add(user)
    remoteControlAssistant?.startPreview()
}

fun Model.remoteControlAssistantStopPreview(user: RemoteControlAssistantPreviewUser) {
    remoteControlAssistantPreviewUsers.remove(user)
    if (remoteControlAssistantPreviewUsers.isEmpty()) {
        remoteControlAssistant?.stopPreview()
    }
}

fun Model.remoteControlAssistantStartStatus() {
    remoteControlAssistantStatusRequested = true
    remoteControlAssistant?.startStatus()
}

fun Model.remoteControlAssistantStopStatus() {
    remoteControlAssistantStatusRequested = false
    remoteControlAssistant?.stopStatus()
}

fun Model.remoteControlAssistantStartStats(filter: RemoteControlStartStatsFilter? = null) {
    remoteControlAssistant?.startStats(filter = filter)
}

fun Model.remoteControlAssistantStopStats() {
    remoteControlAssistant?.stopStats()
}

fun Model.reloadRemoteControlRelay() {
    remoteControlRelay?.stop()
    remoteControlRelay = null
    if (!isRemoteControlRelayConfigured()) {
        return
    }
    val assistantUrl = runCatching {
        URI("ws://localhost:${database.remoteControl.assistant.port}")
    }.getOrNull() ?: return
    remoteControlRelay = RemoteControlRelay(
        context = AppDelegate.context,
        baseUrl = database.remoteControl.assistant.relay.baseUrl,
        bridgeId = database.remoteControl.assistant.relay.bridgeId,
        assistantUrl = assistantUrl.toString()
    )
    remoteControlRelay?.start()
}

fun Model.isRemoteControlRelayConfigured(): Boolean {
    val relay = database.remoteControl.assistant.relay
    return relay.enabled && relay.baseUrl.isNotEmpty()
}

fun Model.remoteControlStreamerCreateStatus(
    filter: RemoteControlStartStatusFilter?
): Triple<RemoteControlStatusGeneral?, RemoteControlStatusTopLeft?, RemoteControlStatusTopRight?> {
    var general: RemoteControlStatusGeneral? = null
    var topLeft: RemoteControlStatusTopLeft? = null
    var topRight: RemoteControlStatusTopRight? = null
    if (filter != null) {
        if (filter.topRight) {
            topRight = remoteControlStreamerCreateStatusTopRight()
        }
    } else {
        general = remoteControlStreamerCreateStatusGeneral()
        topLeft = remoteControlStreamerCreateStatusTopLeft()
        topRight = remoteControlStreamerCreateStatusTopRight()
    }
    return Triple(general, topLeft, topRight)
}

fun Model.isRemoteControlStreamerGeographyStatsFilterEnabled(): Boolean {
    return isRemoteControlAssistantRequestingStats &&
        remoteControlAssistantRequestingStatsFilter.geography == true
}

fun Model.isRemoteControlStreamerWeatherStatsFilterEnabled(): Boolean {
    return isRemoteControlAssistantRequestingStats &&
        remoteControlAssistantRequestingStatsFilter.weather == true
}

fun Model.isRemoteControlStreamerGForceStatsFilterEnabled(): Boolean {
    return isRemoteControlAssistantRequestingStats &&
        remoteControlAssistantRequestingStatsFilter.gForce == true
}

private fun Model.remoteControlStreamerCreateStatusGeneral(): RemoteControlStatusGeneral {
    val general = RemoteControlStatusGeneral()
    general.batteryCharging = isBatteryCharging()
    general.batteryLevel = (100 * battery.level.value).toInt()
    when (statusOther.thermalState.value.ordinal) {
        0,
        1 -> general.flame = RemoteControlStatusGeneralFlame.White
        2 -> general.flame = RemoteControlStatusGeneralFlame.Yellow
        else -> general.flame = RemoteControlStatusGeneralFlame.Red
    }
    general.wiFiSsid = currentWiFiSsid
    general.isLive = isLive.value
    general.isRecording = isRecording.value
    general.isMuted = audio.muted.value
    return general
}

private fun Model.remoteControlStreamerCreateStatusTopLeft(): RemoteControlStatusTopLeft {
    val topLeft = RemoteControlStatusTopLeft()
    if (isStreamConfigured()) {
        topLeft.stream = RemoteControlStatusItem(message = statusTopLeft.streamText.value)
    }
    topLeft.camera = RemoteControlStatusItem(message = statusTopLeft.statusCameraText.value)
    topLeft.mic = RemoteControlStatusItem(message = mic.current.value.name)
    if (zoom.hasZoom.value) {
        topLeft.zoom = RemoteControlStatusItem(message = zoom.statusText())
    }
    if (isObsRemoteControlConfigured()) {
        topLeft.obs = RemoteControlStatusItem(message = statusTopLeft.statusObsText.value)
    }
    if (isEventsConfigured()) {
        topLeft.events = RemoteControlStatusItem(message = statusTopLeft.statusEventsText.value)
    }
    if (isChatConfigured()) {
        topLeft.chat = RemoteControlStatusItem(message = statusTopLeft.statusChatText.value)
    }
    if (isViewersConfigured() && isLive.value) {
        topLeft.viewers = RemoteControlStatusItem(message = statusViewersText())
    }
    return topLeft
}

private fun Model.remoteControlStreamerCreateStatusTopRight(): RemoteControlStatusTopRight {
    val topRight = RemoteControlStatusTopRight()
    val level = formatAudioLevel(level = audio.level.level.value, muted = audio.muted.value) +
        formatAudioLevelChannels(channels = audio.numberOfChannels.value)
    topRight.audioLevel = RemoteControlStatusItem(message = level)
    topRight.audioInfo = RemoteControlStatusTopRightAudioInfo(
        audioLevel = RemoteControlStatusTopRightAudioLevel.Unknown,
        numberOfAudioChannels = audio.numberOfChannels.value
    )
    if (audio.muted.value) {
        topRight.audioInfo!!.audioLevel = RemoteControlStatusTopRightAudioLevel.Muted
    } else {
        topRight.audioInfo!!.audioLevel =
            RemoteControlStatusTopRightAudioLevel.Value(audio.level.level.value)
    }
    if (isIngestsConfigured()) {
        topRight.rtmpServer = RemoteControlStatusItem(message = ingests.speedAndTotal.value)
    }
    if (isAnyRemoteControlConfigured()) {
        topRight.remoteControl = RemoteControlStatusItem(message = statusTopRight.remoteControlStatus.value)
    }
    if (isGameControllerConnected()) {
        topRight.gameController = RemoteControlStatusItem(message = statusTopRight.gameControllersTotal.value)
    }
    if (isLive.value) {
        topRight.bitrate = RemoteControlStatusItem(message = bitrate.speedAndTotal.value)
    }
    if (isLive.value) {
        topRight.uptime = RemoteControlStatusItem(message = streamUptime.uptime.value)
    }
    if (isLocationEnabled()) {
        topRight.location = RemoteControlStatusItem(message = statusTopRight.location.value)
    }
    if (isStatusBondingActive()) {
        topRight.srtla = RemoteControlStatusItem(message = bonding.statistics.value)
    }
    if (isStatusBondingRttsActive()) {
        topRight.srtlaRtts = RemoteControlStatusItem(message = bonding.rtts.value)
    }
    if (isRecording.value) {
        topRight.recording = RemoteControlStatusItem(message = recording.length)
    }
    if (stream.value.replay.enabled) {
        topRight.replay = RemoteControlStatusItem(message = localized("Enabled"))
    }
    if (isStatusBrowserWidgetsActive()) {
        topRight.browserWidgets = RemoteControlStatusItem(message = statusTopRight.browserWidgetsStatus.value)
    }
    if (isAnyMoblinkConfigured()) {
        topRight.moblink = RemoteControlStatusItem(message = moblink.status.value)
    }
    if (statusTopRight.djiDevicesStatus.value.isNotEmpty()) {
        topRight.djiDevices = RemoteControlStatusItem(message = statusTopRight.djiDevicesStatus.value)
    }
    if (database.show.systemMonitor) {
        topRight.systemMonitor = RemoteControlStatusItem(message = systemMonitor.format())
    }
    return topRight
}

fun Model.sendPeriodicRemoteControlStreamerStatus() {
    if (!isRemoteControlStreamerConnected() || !isRemoteControlAssistantRequestingStatus) {
        return
    }
    val (general, topLeft, topRight) =
        remoteControlStreamerCreateStatus(filter = remoteControlAssistantRequestingStatusFilter)
    remoteControlStreamer?.sendStatus(general = general, topLeft = topLeft, topRight = topRight)
}

fun Model.sendPeriodicRemoteControlStreamerStats(now: Instant) {
    if (!isRemoteControlAssistantRequestingStats) {
        return
    }
    val location = locationManager.getLatestKnownLocation()
    val placemark = geographyManager.getLatestPlacemark()
    remoteControlStreamer?.sendStats(
        data = RemoteControlStats(
            date = now,
            timeZone = ZoneId.systemDefault().id,
            speed = (location?.speed ?: 0f).toDouble(),
            averageSpeed = averageSpeed,
            altitude = location?.altitude ?: 0.0,
            latitude = location?.latitude,
            longitude = location?.longitude,
            distance = database.location.distance,
            splitDistance = database.location.splitDistance,
            slopePercent = slopePercent,
            altitudeAscent = database.location.altitudeAscent,
            altitudeDescent = database.location.altitudeDescent,
            splitAltitudeAscent = database.location.splitAltitudeAscent,
            splitAltitudeDescent = database.location.splitAltitudeDescent,
            temperature = null,
            feelsLikeTemperature = null,
            windSpeed = null,
            windGust = null,
            country = placemark?.countryName,
            countryFlag = emojiFlag(countryCode = placemark?.countryCode),
            state = placemark?.adminArea,
            area = placemark?.subAdminArea,
            city = placemark?.locality,
            neighborhood = placemark?.subLocality,
            heartRates = heartRates,
            activeEnergyBurned = workoutActiveEnergyBurned,
            workoutDistance = workoutDistance,
            power = workoutPower,
            stepCount = workoutStepCount,
            cyclingPower = cyclingPower,
            cyclingCadence = cyclingCadence,
            cyclingSpeed = cyclingSpeed,
            gForce = gForceManager?.getLatest()
        )
    )
}

fun Model.isRemoteControlStreamerPreviewActive(): Boolean {
    return isRemoteControlStreamerConnected() &&
        isRemoteControlAssistantRequestingPreview &&
        database.remoteControl.streamer.previewFps > 0
}

fun Model.isRemoteControlWebPreviewActive(): Boolean {
    return isRemoteControlWebRequestingPreview
}

private fun Model.createRemoteControlStateChanged(): RemoteControlAssistantStreamerState {
    val state = RemoteControlAssistantStreamerState()
    if (sceneSelector.sceneIndex.value < enabledScenes.size) {
        state.scene = enabledScenes[sceneSelector.sceneIndex.value].id
    }
    state.autoSceneSwitcher = RemoteControlStateAutoSceneSwitcher(id = autoSceneSwitcher.currentSwitcherId.value)
    state.mic = mic.current.value.id
    val preset = getBitratePresetByBitrate(bitrate = stream.value.bitrate)
    if (preset != null) {
        state.bitrate = preset.id
    }
    when (cameraPosition) {
        else -> {
            state.zoomPresets = mutableListOf()
        }
    }
    state.zoom = zoom.x.value
    state.debugLogging = database.debug.debugLogging.value
    state.streaming = isLive.value
    state.recording = isRecording.value
    state.muted = audio.muted.value
    state.stealthMode = showStealthMode.value
    state.previewStream = isPreviewStreaming.value
    state.torchOn = streamOverlay.isTorchOn.value
    state.batteryCharging = isBatteryCharging()
    state.filters = mutableMapOf()
    for (filter in RemoteControlFilter.entries) {
        state.filters = (state.filters ?: emptyMap()) +
            (filter to (getQuickButton(type = filter.toSettings())?.isOn?.value ?: false))
    }
    state.gimbalTracking = database.gimbal.tracking
    state.gimbalPresets = getRemoteControlGimbalPresets()
    state.macros = getRemoteControlMacros()
    return state
}

fun Model.remoteControlStateChanged(state: RemoteControlAssistantStreamerState) {
    remoteControlStreamer?.stateChanged(state = state)
    remoteControlWeb?.stateChanged(state = state)
}

fun Model.remoteControlLog(entry: String) {
    remoteControlStreamer?.log(entry = entry)
    remoteControlWeb?.log(entry = entry)
}

fun Model.remoteControlScoreboardUpdate(scoreboard: SettingsWidgetScoreboard) {
    val config = getModularScoreboardConfig(scoreboard = scoreboard)
    remoteControlStreamer?.sendScoreboardUpdate(config = config)
    remoteControlWeb?.sendScoreboardUpdate(config = config)
}

fun Model.reloadRemoteControlWeb() {
    remoteControlWeb?.stop()
    remoteControlWeb = null
    if (!database.remoteControl.web.enabled) {
        return
    }
    remoteControlWeb = RemoteControlWeb(delegate = RemoteControlWebDelegateAdapter(this))
    remoteControlWeb?.start(port = database.remoteControl.web.port)
}

private fun Model.handleRemoteControlSetFilter(filter: RemoteControlFilter, on: Boolean) {
    when (filter) {
        RemoteControlFilter.Pixellate -> setPixellateQuickButton(on = on)
        RemoteControlFilter.Movie -> setFilterQuickButton(type = SettingsQuickButtonType.movie, on = on)
        RemoteControlFilter.GrayScale -> setFilterQuickButton(type = SettingsQuickButtonType.grayScale, on = on)
        RemoteControlFilter.Sepia -> setFilterQuickButton(type = SettingsQuickButtonType.sepia, on = on)
        RemoteControlFilter.Triple -> setFilterQuickButton(type = SettingsQuickButtonType.triple, on = on)
        RemoteControlFilter.Twin -> setFilterQuickButton(type = SettingsQuickButtonType.twin, on = on)
        RemoteControlFilter.FourThree -> setFilterQuickButton(type = SettingsQuickButtonType.fourThree, on = on)
        RemoteControlFilter.Crt -> setFilterQuickButton(type = SettingsQuickButtonType.crt, on = on)
        RemoteControlFilter.Pinch -> setPinchQuickButton(on = on)
        RemoteControlFilter.Whirlpool -> setWhirlpoolQuickButton(on = on)
        RemoteControlFilter.Poll -> setPollQuickButton(on = on)
        RemoteControlFilter.BlurFaces -> setBlurFaces(on = on)
        RemoteControlFilter.Privacy -> setPrivacy(on = on)
        RemoteControlFilter.Beauty -> setBeautyQuickButton(on = on)
        RemoteControlFilter.MoblinInMouth -> setMoblinInMouth(on = on)
        RemoteControlFilter.CameraMan -> setCameraManQuickButton(on = on)
    }
}

private fun Model.handleRemoteControlTriggerReaction(reaction: RemoteControlReaction) {
    triggerReaction(reaction = reaction.toSettings())
}

private fun Model.handleGetSettings(): RemoteControlSettings {
    val scenes = enabledScenes.map {
        RemoteControlSettingsScene(id = it.id, name = it.name)
    }
    val streams = database.streams.map {
        RemoteControlSettingsStream(id = it.id, name = it.name)
    }
    val autoSceneSwitchers = database.autoSceneSwitchers.switchers.map {
        RemoteControlSettingsAutoSceneSwitcher(id = it.id, name = it.name)
    }
    val mics = database.mics.mics.value.map {
        RemoteControlSettingsMic(id = it.id, name = it.name)
    }
    val bitratePresets = database.bitratePresets.map {
        RemoteControlSettingsBitratePreset(id = it.id, bitrate = it.bitrate.toUInt())
    }
    val connectionPriorities = stream.value.srt.connectionPriorities.priorities.map {
        RemoteControlSettingsSrtConnectionPriority(
            id = it.id,
            name = it.name,
            priority = it.priority,
            enabled = it.enabled
        )
    }
    val connectionPrioritiesEnabled = stream.value.srt.connectionPriorities.enabled
    return RemoteControlSettings(
        streams = streams,
        scenes = scenes,
        autoSceneSwitchers = autoSceneSwitchers,
        bitratePresets = bitratePresets,
        mics = mics,
        srt = RemoteControlSettingsSrt(
            connectionPrioritiesEnabled = connectionPrioritiesEnabled,
            connectionPriorities = connectionPriorities
        )
    )
}

fun Model.remoteControlStreamerConnected() {
    useRemoteControlForChatAndEvents = database.remoteControl.streamer.reliableChatAndEvents
    val subTitle: String?
    if (useRemoteControlForChatAndEvents) {
        reloadTwitchEventSub()
        reloadTwitchChat()
        remoteControlStreamerSendTwitchStart()
        subTitle = localized("Reliable alerts and chat messages activated")
    } else {
        subTitle = null
    }
    makeToast(title = localized("Remote control assistant connected"), subTitle = subTitle)
    isRemoteControlAssistantRequestingPreview = false
    isRemoteControlAssistantRequestingStatus = false
    remoteControlStreamerStopStats()
    setLowFpsImage()
    updateRemoteControlStatus()
    remoteControlStateChanged(state = createRemoteControlStateChanged())
}

fun Model.remoteControlStreamerDisconnected() {
    makeToast(title = localized("Remote control assistant disconnected"))
    setGimbalMovement(x = 0f, y = 0f)
    isRemoteControlAssistantRequestingPreview = false
    isRemoteControlAssistantRequestingStatus = false
    remoteControlStreamerStopStats()
    setLowFpsImage()
    updateRemoteControlStatus()
}

fun Model.remoteControlStreamerWrongPassword() {
    makeErrorToast(title = localized("Remote control assistant rejected the password"))
    updateRemoteControlStatus()
}

fun Model.remoteControlStreamerGetStatus(): Triple<RemoteControlStatusGeneral, RemoteControlStatusTopLeft, RemoteControlStatusTopRight> {
    val (general, topLeft, topRight) = remoteControlStreamerCreateStatus(filter = null)
    return Triple(general!!, topLeft!!, topRight!!)
}

fun Model.remoteControlStreamerGetSettings(): RemoteControlSettings {
    return handleGetSettings()
}

fun Model.remoteControlStreamerSetStream(id: UUID) {
    val stream = findStream(id = id) ?: return
    if (stream.enabled || isLive.value || isRecording.value) {
        return
    }
    setCurrentStream(stream = stream)
    reloadStreamIfEnabled(stream = stream)
}

fun Model.remoteControlStreamerSetScene(id: UUID) {
    selectScene(id = id)
}

fun Model.remoteControlStreamerSetAutoSceneSwitcher(id: UUID?) {
    setAutoSceneSwitcher(id = id)
}

fun Model.remoteControlStreamerSetMic(id: String) {
    manualSelectMicById(id = id)
}

fun Model.remoteControlStreamerSetTalkbackMic(id: String) {
    setTalkbackMic(id = id)
}

fun Model.remoteControlStreamerSetBitratePreset(id: UUID) {
    val preset = database.bitratePresets.firstOrNull { preset -> preset.id == id } ?: return
    setBitrate(bitrate = preset.bitrate.toInt())
}

fun Model.remoteControlStreamerSetRecord(on: Boolean) {
    if (on) {
        startRecording()
    } else {
        stopRecording()
    }
}

fun Model.remoteControlStreamerSetLive(on: Boolean) {
    if (on) {
        startStream()
    } else {
        stopStream()
    }
}

fun Model.remoteControlStreamerSetPreviewStream(on: Boolean) {
    if (on) {
        startPreviewStream()
    } else {
        stopPreviewStream()
    }
}

fun Model.remoteControlStreamerSetDebugLogging(on: Boolean) {
    database.debug.debugLogging.value = on
    setDebugLogging(on = on)
}

fun Model.remoteControlStreamerSetZoom(x: Float) {
    setZoomX(x = x, rate = database.zoom.speed)
}

fun Model.remoteControlStreamerSetZoomPreset(id: UUID) {
    setZoomPreset(id = id)
}

fun Model.remoteControlStreamerSetMute(on: Boolean) {
    setMuteOn(value = on)
}

fun Model.remoteControlStreamerSetStealthMode(on: Boolean) {
    setStealthMode(on = on)
}

fun Model.remoteControlStreamerSetTorch(on: Boolean) {
    streamOverlay.isTorchOn.value = on
    updateTorch()
    setQuickButton(type = SettingsQuickButtonType.torch, isOn = on)
}

fun Model.remoteControlStreamerReloadBrowserWidgets() {
    reloadBrowserWidgets()
}

fun Model.remoteControlStreamerSetSrtConnectionPrioritiesEnabled(enabled: Boolean) {
    stream.value.srt.connectionPriorities.enabled = enabled
    updateSrtlaPriorities()
}

fun Model.remoteControlStreamerSetSrtConnectionPriority(id: UUID, priority: Int, enabled: Boolean) {
    val entry = stream.value.srt.connectionPriorities.priorities.firstOrNull { it.id == id }
    if (entry != null) {
        entry.priority = clampConnectionPriority(value = priority)
        entry.enabled = enabled
        updateSrtlaPriorities()
    }
}

fun Model.sendPreviewToRemoteControlAssistant(preview: ByteArray) {
    if (!isRemoteControlStreamerPreviewActive()) {
        return
    }
    remoteControlStreamer?.sendPreview(preview = preview)
}

fun Model.sendPreviewToRemoteControlWeb(preview: ByteArray) {
    if (!isRemoteControlWebPreviewActive()) {
        return
    }
    remoteControlWeb?.sendPreview(preview = preview)
}

fun Model.remoteControlStreamerTwitchEventSubNotification(message: String) {
    twitchEventSub?.handleMessage(messageText = message)
}

fun Model.remoteControlStreamerChatMessages(history: Boolean, messages: List<RemoteControlChatMessage>) {
    val live = !history || remoteControlStreamerLatestReceivedChatMessageId != -1
    for (message in messages) {
        if (message.id <= remoteControlStreamerLatestReceivedChatMessageId) {
            continue
        }
        appendChatMessage(
            platform = message.platform,
            messageId = message.messageId,
            displayName = message.displayName,
            user = message.user,
            userId = message.userId,
            userColor = message.userColor,
            userBadges = message.userBadges,
            segments = message.segments,
            timestamp = message.timestamp,
            timestampTime = Instant.now(),
            isAction = message.isAction,
            isSubscriber = message.isSubscriber,
            isModerator = message.isModerator,
            isOwner = message.isOwner,
            bits = message.bits,
            highlight = message.highlight?.let { ChatHighlight(remoteControl = it) },
            live = live
        )
        remoteControlStreamerLatestReceivedChatMessageId = message.id
    }
}

fun Model.remoteControlStreamerSendMessage(text: String) {
    val user = localized("Mom")
    appendChatMessage(
        platform = null,
        messageId = null,
        displayName = user,
        user = user,
        userId = null,
        userColor = RgbColor(red = 0x2F, green = 0xF5, blue = 0x2C),
        userBadges = emptyList(),
        segments = makeChatPostTextSegments(text = text),
        timestamp = statusOther.digitalClock.value,
        timestampTime = Instant.now(),
        isAction = false,
        isSubscriber = false,
        isModerator = false,
        isOwner = false,
        bits = null,
        highlight = ChatHighlight.makeRemoteControlAssistant(),
        live = true
    )
}

fun Model.remoteControlStreamerStartPreview() {
    isRemoteControlAssistantRequestingPreview = true
    setLowFpsImage()
}

fun Model.remoteControlStreamerStopPreview() {
    isRemoteControlAssistantRequestingPreview = false
    setLowFpsImage()
}

fun Model.remoteControlStreamerSetRemoteSceneSettings(data: RemoteControlRemoteSceneSettings) {
    val (scenes, widgets, selectedSceneId) = data.toSettings()
    if (selectedSceneId != null) {
        val widget = SettingsWidget(name = "")
        widget.type = SettingsWidgetType.scene
        widget.scene.sceneId = selectedSceneId
        remoteSceneScenes = scenes.toMutableList()
        remoteSceneWidgets = (listOf(widget) + widgets).toMutableList()
        resetSelectedScene(changeScene = false)
    } else if (remoteSceneScenes.isNotEmpty()) {
        remoteSceneScenes = mutableListOf()
        remoteSceneWidgets = mutableListOf()
        resetSelectedScene(changeScene = false)
    }
}

fun Model.remoteControlStreamerSetRemoteSceneData(data: RemoteControlRemoteSceneData) {
    val textStats = data.textStats
    if (textStats != null) {
        remoteSceneData.textStats = textStats
    }
    val location = data.location
    if (location != null) {
        remoteSceneData.location = location
    }
}

fun Model.remoteControlStreamerInstantReplay() {
    instantReplay()
}

fun Model.remoteControlStreamerSaveReplay() {
    saveReplay()
}

fun Model.clearRemoteSceneSettingsAndData() {
    remoteSceneScenes = mutableListOf()
    remoteSceneWidgets = mutableListOf()
    remoteSceneData.textStats = null
    remoteSceneData.location = null
}

fun Model.remoteControlStreamerStartStatus(interval: Int, filter: RemoteControlStartStatusFilter) {
    isRemoteControlAssistantRequestingStatus = true
    remoteControlAssistantRequestingStatusFilter = filter
}

fun Model.remoteControlStreamerStopStatus() {
    isRemoteControlAssistantRequestingStatus = false
    remoteControlAssistantRequestingStatusFilter = null
}

fun Model.remoteControlStreamerStartStats(filter: RemoteControlStartStatsFilter?) {
    isRemoteControlAssistantRequestingStats = true
    remoteControlAssistantRequestingStatsFilter = filter ?: remoteControlStartStatsFilterAllEnabled
    startWeatherManager()
    startGeographyManager()
    startGForceManager()
}

fun Model.remoteControlStreamerStopStats() {
    isRemoteControlAssistantRequestingStats = false
    remoteControlAssistantRequestingStatsFilter = RemoteControlStartStatsFilter()
    startWeatherManager()
    startGeographyManager()
    startGForceManager()
}

fun Model.remoteControlStreamerStartMacro(id: UUID) {
    startMacro(id = id)
}

fun Model.remoteControlStreamerStopMacro(id: UUID) {
    stopMacro(id = id)
}

fun Model.remoteControlStreamerGetScoreboardSports(): List<String> {
    return getScoreboardSports()
}

fun Model.remoteControlStreamerSetScoreboardSport(sportId: String) {
    handleSportSwitch(sportId = sportId)
}

fun Model.remoteControlStreamerUpdateScoreboard(config: RemoteControlScoreboardMatchConfig) {
    handleExternalScoreboardUpdate(config = config)
}

fun Model.remoteControlStreamerToggleScoreboardClock() {
    handleScoreboardToggleClock()
}

fun Model.remoteControlStreamerSetScoreboardDuration(minutes: Int) {
    handleScoreboardSetDuration(minutes = minutes)
}

fun Model.remoteControlStreamerSetScoreboardClock(time: String) {
    handleScoreboardSetClockManual(time = time)
}

fun Model.remoteControlStreamerWhip(
    url: String,
    method: String,
    headers: List<SettingsHttpHeader>,
    body: ByteArray,
    onCompleted: (Int, List<SettingsHttpHeader>, ByteArray) -> Unit
) {
    val whipServer = ingests.whip
    if (whipServer == null) {
        onCompleted(500, emptyList(), ByteArray(0))
        return
    }
    when (method) {
        "POST" -> {
            val sdpOffer = String(body, Charsets.UTF_8)
            val streamKey = url.split("/").lastOrNull()
            if (streamKey == null) {
                onCompleted(400, emptyList(), ByteArray(0))
                return
            }
            whipServer.startClient(streamKey = streamKey, sdpOffer = sdpOffer) { sdpAnswer ->
                if (sdpAnswer != null) {
                    onCompleted(200, emptyList(), sdpAnswer.toByteArray(Charsets.UTF_8))
                } else {
                    onCompleted(400, emptyList(), ByteArray(0))
                }
            }
        }
        "DELETE" -> {
            onCompleted(200, emptyList(), ByteArray(0))
        }
        else -> {
            onCompleted(400, emptyList(), ByteArray(0))
        }
    }
}

fun Model.remoteControlStreamerSetFilter(filter: RemoteControlFilter, on: Boolean) {
    handleRemoteControlSetFilter(filter = filter, on = on)
}

fun Model.remoteControlStreamerTriggerReaction(reaction: RemoteControlReaction) {
    handleRemoteControlTriggerReaction(reaction = reaction)
}

fun Model.remoteControlStreamerMoveToGimbalPreset(id: UUID) {
    moveToGimbalPreset(id = id)
}

fun Model.remoteControlStreamerSetGimbalTracking(on: Boolean) {
    setGimbalTracking(on = on)
}

fun Model.remoteControlStreamerSetGimbalMovement(x: Float, y: Float) {
    setGimbalMovement(x = x, y = y)
}

fun Model.remoteControlStreamerAnimateGimbal(motion: SettingsGimbalMotion) {
    animateGimbal(motion = motion)
}

fun Model.remoteControlStreamerSaveGimbalPreset() {
    saveGimbalPreset(id = null)
}

fun Model.remoteControlStreamerImportSettings(settings: ByteArray, onCompleted: (Boolean) -> Unit) {
    if (isLive.value || isRecording.value) {
        onCompleted(false)
        return
    }
    importSettingsFromData(context = AppDelegate.context, settings = settings) { onCompleted(it) }
}

fun Model.remoteControlAssistantConnected() {
    makeToast(title = localized("Remote control streamer connected"))
    remoteControlAssistantStreamerState.filters = mutableMapOf()
    updateRemoteControlStatus()
    updateRemoteControlAssistantStatus()
    remoteControlAssistantSetRemoteSceneSettings()
    if (remoteControlAssistantPreviewUsers.isNotEmpty()) {
        remoteControlAssistant?.startPreview()
    }
    if (remoteControlAssistantStatusRequested) {
        remoteControlAssistant?.startStatus()
    }
}

fun Model.remoteControlAssistantDisconnected() {
    makeToast(title = localized("Remote control streamer disconnected"))
    remoteControl.topLeft.value = null
    remoteControl.topRight.value = null
    updateRemoteControlStatus()
}

fun Model.remoteControlAssistantStateChanged(state: RemoteControlAssistantStreamerState) {
    state.scene?.let {
        remoteControlAssistantStreamerState.scene = it
        remoteControl.scene.value = it
    }
    state.autoSceneSwitcher?.let {
        remoteControlAssistantStreamerState.autoSceneSwitcher = it
        remoteControl.autoSceneSwitcher.value = it.id
    }
    state.mic?.let {
        remoteControlAssistantStreamerState.mic = it
        remoteControl.mic.value = it
    }
    state.bitrate?.let {
        remoteControlAssistantStreamerState.bitrate = it
        remoteControl.bitrate.value = it
    }
    state.zoomPresets?.let {
        remoteControlAssistantStreamerState.zoomPresets = it
        remoteControl.zoomPresets.value = it
    }
    state.gimbalPresets?.let {
        remoteControlAssistantStreamerState.gimbalPresets = it
        remoteControl.gimbalPresets.value = it
    }
    state.macros?.let {
        remoteControlAssistantStreamerState.macros = it
        remoteControl.macros.value = it
    }
    state.zoomPreset?.let {
        remoteControlAssistantStreamerState.zoomPreset = it
        remoteControl.zoomPreset.value = it
    }
    state.zoom?.let {
        remoteControlAssistantStreamerState.zoom = it
        remoteControl.zoom.value = it.toString()
    }
    state.debugLogging?.let {
        remoteControlAssistantStreamerState.debugLogging = it
        remoteControl.debugLogging.value = it
    }
    state.streaming?.let {
        remoteControlAssistantStreamerState.streaming = it
        remoteControl.streaming.value = it
    }
    state.recording?.let {
        remoteControlAssistantStreamerState.recording = it
        remoteControl.recording.value = it
    }
    state.muted?.let {
        remoteControlAssistantStreamerState.muted = it
        remoteControl.muted.value = it
    }
    state.stealthMode?.let {
        remoteControlAssistantStreamerState.stealthMode = it
        remoteControl.stealthMode.value = it
    }
    state.previewStream?.let {
        remoteControlAssistantStreamerState.previewStream = it
        remoteControl.previewStream.value = it
    }
    state.filters?.let { filters ->
        for ((filter, on) in filters) {
            remoteControlAssistantStreamerState.filters =
                (remoteControlAssistantStreamerState.filters ?: emptyMap()) + (filter to on)
            when (filter) {
                RemoteControlFilter.Pixellate -> remoteControl.pixellate.value = on
                RemoteControlFilter.Movie -> remoteControl.movie.value = on
                RemoteControlFilter.GrayScale -> remoteControl.grayScale.value = on
                RemoteControlFilter.Sepia -> remoteControl.sepia.value = on
                RemoteControlFilter.Triple -> remoteControl.triple.value = on
                RemoteControlFilter.Twin -> remoteControl.twin.value = on
                RemoteControlFilter.FourThree -> remoteControl.fourThree.value = on
                RemoteControlFilter.Crt -> remoteControl.crt.value = on
                RemoteControlFilter.Pinch -> remoteControl.pinch.value = on
                RemoteControlFilter.Whirlpool -> remoteControl.whirlpool.value = on
                RemoteControlFilter.Poll -> remoteControl.poll.value = on
                RemoteControlFilter.BlurFaces -> remoteControl.blurFaces.value = on
                RemoteControlFilter.Privacy -> remoteControl.privacy.value = on
                RemoteControlFilter.Beauty -> remoteControl.beauty.value = on
                RemoteControlFilter.MoblinInMouth -> remoteControl.moblinInMouth.value = on
                RemoteControlFilter.CameraMan -> remoteControl.cameraMan.value = on
            }
        }
    }
    if (isWatchRemoteControl()) {
        sendRemoteControlAssistantStatusToWatch()
    }
}

fun Model.remoteControlAssistantPreview(preview: ByteArray) {
    remoteControl.preview.value = BitmapFactory.decodeByteArray(preview, 0, preview.size)
    if (isWatchRemoteControl()) {
        sendPreviewToWatch(image = preview)
    }
}

fun Model.remoteControlAssistantLog(entry: String) {
    if (remoteControlAssistantLog.size > 100_000) {
        remoteControlAssistantLog.removeFirst()
    }
    logId += 1
    remoteControlAssistantLog.add(LogEntry(id = logId, message = entry))
}

fun Model.remoteControlAssistantStatus(
    general: RemoteControlStatusGeneral?,
    topLeft: RemoteControlStatusTopLeft?,
    topRight: RemoteControlStatusTopRight?
) {
    if (topRight != null) {
        remoteControl.topRight.value = topRight
    }
}

fun Model.remoteControlAssistantStats(data: RemoteControlStats) {
}

fun Model.remoteControlWebDisconnected() {
    setGimbalMovement(x = 0f, y = 0f)
}

fun Model.remoteControlWebConnected() {
    remoteControlWeb?.stateChanged(state = createRemoteControlStateChanged())
    remoteControlWeb?.sendGolfScoreboardUpdate(data = getGolfScoreboardForRemoteControl())
    val scoreboard = getEnabledScoreboardWidgetsInSelectedScene().firstOrNull()?.scoreboard
    remoteControlWeb?.sendScoreboardUpdate(config = getModularScoreboardConfig(scoreboard = scoreboard))
}

fun Model.remoteControlWebGetStatus(): Triple<RemoteControlStatusGeneral, RemoteControlStatusTopLeft, RemoteControlStatusTopRight> {
    return remoteControlStreamerGetStatus()
}

fun Model.remoteControlWebGetSettings(): RemoteControlSettings {
    return handleGetSettings()
}

fun Model.remoteControlWebSetScene(id: UUID) {
    selectScene(id = id)
}

fun Model.remoteControlWebSetAutoSceneSwitcher(id: UUID?) {
    remoteControlStreamerSetAutoSceneSwitcher(id = id)
}

fun Model.remoteControlWebSetMic(id: String) {
    remoteControlStreamerSetMic(id = id)
}

fun Model.remoteControlWebSetBitratePreset(id: UUID) {
    remoteControlStreamerSetBitratePreset(id = id)
}

fun Model.remoteControlWebSetRecord(on: Boolean) {
    remoteControlStreamerSetRecord(on = on)
}

fun Model.remoteControlWebSetLive(on: Boolean) {
    remoteControlStreamerSetLive(on = on)
}

fun Model.remoteControlWebSetPreviewStream(on: Boolean) {
    remoteControlStreamerSetPreviewStream(on = on)
}

fun Model.remoteControlWebSetZoom(x: Float) {
    remoteControlStreamerSetZoom(x = x)
}

fun Model.remoteControlWebSetZoomPreset(id: UUID) {
    remoteControlStreamerSetZoomPreset(id = id)
}

fun Model.remoteControlWebSetDebugLogging(on: Boolean) {
    remoteControlStreamerSetDebugLogging(on = on)
}

fun Model.remoteControlWebSetMute(on: Boolean) {
    remoteControlStreamerSetMute(on = on)
}

fun Model.remoteControlWebSetStealthMode(on: Boolean) {
    remoteControlStreamerSetStealthMode(on = on)
}

fun Model.remoteControlWebSetTorch(on: Boolean) {
    remoteControlStreamerSetTorch(on = on)
}

fun Model.remoteControlWebReloadBrowserWidgets() {
    reloadBrowserWidgets()
}

fun Model.remoteControlWebSetSrtConnectionPrioritiesEnabled(enabled: Boolean) {
    remoteControlStreamerSetSrtConnectionPrioritiesEnabled(enabled = enabled)
}

fun Model.remoteControlWebSetSrtConnectionPriority(id: UUID, priority: Int, enabled: Boolean) {
    remoteControlStreamerSetSrtConnectionPriority(id = id, priority = priority, enabled = enabled)
}

fun Model.remoteControlWebMoveToGimbalPreset(id: UUID) {
    remoteControlStreamerMoveToGimbalPreset(id = id)
}

fun Model.remoteControlWebSetGimbalTracking(on: Boolean) {
    remoteControlStreamerSetGimbalTracking(on = on)
}

fun Model.remoteControlWebSetGimbalMovement(x: Float, y: Float) {
    remoteControlStreamerSetGimbalMovement(x = x, y = y)
}

fun Model.remoteControlWebAnimateGimbal(motion: SettingsGimbalMotion) {
    remoteControlStreamerAnimateGimbal(motion = motion)
}

fun Model.remoteControlWebSaveGimbalPreset() {
    remoteControlStreamerSaveGimbalPreset()
}

fun Model.remoteControlWebGetScoreboardSports(): List<String> {
    return getScoreboardSports()
}

fun Model.remoteControlWebSetScoreboardSport(sportId: String) {
    handleSportSwitch(sportId = sportId)
}

fun Model.remoteControlWebUpdateScoreboard(config: RemoteControlScoreboardMatchConfig) {
    handleExternalScoreboardUpdate(config = config)
}

fun Model.remoteControlWebToggleScoreboardClock() {
    handleScoreboardToggleClock()
}

fun Model.remoteControlWebSetScoreboardDuration(minutes: Int) {
    handleScoreboardSetDuration(minutes = minutes)
}

fun Model.remoteControlWebSetScoreboardClock(time: String) {
    handleScoreboardSetClockManual(time = time)
}

fun Model.remoteControlWebGetGolfScoreboard(): RemoteControlGolfScoreboard {
    return getGolfScoreboardForRemoteControl()
}

fun Model.remoteControlWebUpdateGolfScoreboard(data: RemoteControlGolfScoreboard) {
    handleExternalGolfScoreboardUpdate(remoteScorecard = data)
}

fun Model.remoteControlWebSetFilter(filter: RemoteControlFilter, on: Boolean) {
    handleRemoteControlSetFilter(filter = filter, on = on)
}

fun Model.remoteControlWebTriggerReaction(reaction: RemoteControlReaction) {
    handleRemoteControlTriggerReaction(reaction = reaction)
}

fun Model.remoteControlWebGetRecordings(): List<Map<String, String>> {
    val directory = recordingsStorage.defaultStorageDirectory()
    val files = directory.list() ?: return emptyList()
    return files
        .filter { it.endsWith(".mp4") }
        .sortedDescending()
        .map { filename ->
            val url = File(directory, filename)
            val size = url.length()
            mapOf("name" to filename, "size" to size.toULong().formatBytes())
        }
}

fun Model.remoteControlWebGetRecordingThumbnail(filename: String): ByteArray? {
    recordingThumbnailsCache[filename]?.let { return it }
    val url = File(recordingsStorage.defaultStorageDirectory(), filename)
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(url.absolutePath)
        val bitmap = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) ?: return null
        val scale = minOf(480f / bitmap.width.toFloat(), 480f / bitmap.height.toFloat(), 1f)
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt(),
                (bitmap.height * scale).toInt(),
                true
            )
        } else {
            bitmap
        }
        val stream = ByteArrayOutputStream()
        if (!scaled.compress(Bitmap.CompressFormat.JPEG, 90, stream)) {
            return null
        }
        val thumbnail = stream.toByteArray()
        recordingThumbnailsCache[filename] = thumbnail
        thumbnail
    } catch (e: Exception) {
        null
    } finally {
        retriever.release()
    }
}

fun Model.remoteControlWebGetRecordingUrl(filename: String): File? {
    val url = File(recordingsStorage.defaultStorageDirectory(), filename)
    if (!url.exists()) {
        return null
    }
    return url
}

fun Model.remoteControlWebDeleteRecording(filename: String) {
    val url = File(recordingsStorage.defaultStorageDirectory(), filename)
    runCatching { url.delete() }
    recordingThumbnailsCache.remove(filename)
}

fun Model.remoteControlWebStartPreview() {
    isRemoteControlWebRequestingPreview = true
    setLowFpsImage()
}

fun Model.remoteControlWebStopPreview() {
    isRemoteControlWebRequestingPreview = false
    setLowFpsImage()
}

private class RemoteControlStreamerDelegateAdapter(
    private val model: Model
) : RemoteControlStreamerDelegate {
    override fun remoteControlStreamerConnected() {
        model.remoteControlStreamerConnected()
    }

    override fun remoteControlStreamerDisconnected() {
        model.remoteControlStreamerDisconnected()
    }

    override fun remoteControlStreamerWrongPassword() {
        model.remoteControlStreamerWrongPassword()
    }

    override fun remoteControlStreamerGetStatus(): Triple<RemoteControlStatusGeneral, RemoteControlStatusTopLeft, RemoteControlStatusTopRight> {
        return model.remoteControlStreamerGetStatus()
    }

    override fun remoteControlStreamerGetSettings(): RemoteControlSettings {
        return model.remoteControlStreamerGetSettings()
    }

    override fun remoteControlStreamerSetStream(id: UUID) {
        model.remoteControlStreamerSetStream(id = id)
    }

    override fun remoteControlStreamerSetScene(id: UUID) {
        model.remoteControlStreamerSetScene(id = id)
    }

    override fun remoteControlStreamerSetAutoSceneSwitcher(id: UUID?) {
        model.remoteControlStreamerSetAutoSceneSwitcher(id = id)
    }

    override fun remoteControlStreamerSetMic(id: String) {
        model.remoteControlStreamerSetMic(id = id)
    }

    override fun remoteControlStreamerSetTalkbackMic(id: String) {
        model.remoteControlStreamerSetTalkbackMic(id = id)
    }

    override fun remoteControlStreamerSetBitratePreset(id: UUID) {
        model.remoteControlStreamerSetBitratePreset(id = id)
    }

    override fun remoteControlStreamerSetRecord(on: Boolean) {
        model.remoteControlStreamerSetRecord(on = on)
    }

    override fun remoteControlStreamerSetLive(on: Boolean) {
        model.remoteControlStreamerSetLive(on = on)
    }

    override fun remoteControlStreamerSetPreviewStream(on: Boolean) {
        model.remoteControlStreamerSetPreviewStream(on = on)
    }

    override fun remoteControlStreamerSetDebugLogging(on: Boolean) {
        model.remoteControlStreamerSetDebugLogging(on = on)
    }

    override fun remoteControlStreamerSetZoom(x: Float) {
        model.remoteControlStreamerSetZoom(x = x)
    }

    override fun remoteControlStreamerSetZoomPreset(id: UUID) {
        model.remoteControlStreamerSetZoomPreset(id = id)
    }

    override fun remoteControlStreamerSetMute(on: Boolean) {
        model.remoteControlStreamerSetMute(on = on)
    }

    override fun remoteControlStreamerSetStealthMode(on: Boolean) {
        model.remoteControlStreamerSetStealthMode(on = on)
    }

    override fun remoteControlStreamerSetTorch(on: Boolean) {
        model.remoteControlStreamerSetTorch(on = on)
    }

    override fun remoteControlStreamerReloadBrowserWidgets() {
        model.remoteControlStreamerReloadBrowserWidgets()
    }

    override fun remoteControlStreamerSetSrtConnectionPriority(id: UUID, priority: Int, enabled: Boolean) {
        model.remoteControlStreamerSetSrtConnectionPriority(id = id, priority = priority, enabled = enabled)
    }

    override fun remoteControlStreamerSetSrtConnectionPrioritiesEnabled(enabled: Boolean) {
        model.remoteControlStreamerSetSrtConnectionPrioritiesEnabled(enabled = enabled)
    }

    override fun remoteControlStreamerTwitchEventSubNotification(message: String) {
        model.remoteControlStreamerTwitchEventSubNotification(message = message)
    }

    override fun remoteControlStreamerChatMessages(history: Boolean, messages: List<RemoteControlChatMessage>) {
        model.remoteControlStreamerChatMessages(history = history, messages = messages)
    }

    override fun remoteControlStreamerStartPreview() {
        model.remoteControlStreamerStartPreview()
    }

    override fun remoteControlStreamerStopPreview() {
        model.remoteControlStreamerStopPreview()
    }

    override fun remoteControlStreamerSetRemoteSceneSettings(data: RemoteControlRemoteSceneSettings) {
        model.remoteControlStreamerSetRemoteSceneSettings(data = data)
    }

    override fun remoteControlStreamerSetRemoteSceneData(data: RemoteControlRemoteSceneData) {
        model.remoteControlStreamerSetRemoteSceneData(data = data)
    }

    override fun remoteControlStreamerInstantReplay() {
        model.remoteControlStreamerInstantReplay()
    }

    override fun remoteControlStreamerSaveReplay() {
        model.remoteControlStreamerSaveReplay()
    }

    override fun remoteControlStreamerStartStatus(interval: Int, filter: RemoteControlStartStatusFilter) {
        model.remoteControlStreamerStartStatus(interval = interval, filter = filter)
    }

    override fun remoteControlStreamerStopStatus() {
        model.remoteControlStreamerStopStatus()
    }

    override fun remoteControlStreamerGetScoreboardSports(): List<String> {
        return model.remoteControlStreamerGetScoreboardSports()
    }

    override fun remoteControlStreamerSetScoreboardSport(sportId: String) {
        model.remoteControlStreamerSetScoreboardSport(sportId = sportId)
    }

    override fun remoteControlStreamerUpdateScoreboard(config: RemoteControlScoreboardMatchConfig) {
        model.remoteControlStreamerUpdateScoreboard(config = config)
    }

    override fun remoteControlStreamerToggleScoreboardClock() {
        model.remoteControlStreamerToggleScoreboardClock()
    }

    override fun remoteControlStreamerSetScoreboardDuration(minutes: Int) {
        model.remoteControlStreamerSetScoreboardDuration(minutes = minutes)
    }

    override fun remoteControlStreamerSetScoreboardClock(time: String) {
        model.remoteControlStreamerSetScoreboardClock(time = time)
    }

    override fun remoteControlStreamerWhip(
        url: String,
        method: String,
        headers: List<SettingsHttpHeader>,
        body: ByteArray,
        onCompleted: (Int, List<SettingsHttpHeader>, ByteArray) -> Unit
    ) {
        model.remoteControlStreamerWhip(
            url = url,
            method = method,
            headers = headers,
            body = body,
            onCompleted = onCompleted
        )
    }

    override fun remoteControlStreamerSetFilter(filter: RemoteControlFilter, on: Boolean) {
        model.remoteControlStreamerSetFilter(filter = filter, on = on)
    }

    override fun remoteControlStreamerTriggerReaction(reaction: RemoteControlReaction) {
        model.remoteControlStreamerTriggerReaction(reaction = reaction)
    }

    override fun remoteControlStreamerMoveToGimbalPreset(id: UUID) {
        model.remoteControlStreamerMoveToGimbalPreset(id = id)
    }

    override fun remoteControlStreamerSetGimbalTracking(on: Boolean) {
        model.remoteControlStreamerSetGimbalTracking(on = on)
    }

    override fun remoteControlStreamerSetGimbalMovement(x: Float, y: Float) {
        model.remoteControlStreamerSetGimbalMovement(x = x, y = y)
    }

    override fun remoteControlStreamerAnimateGimbal(motion: SettingsGimbalMotion) {
        model.remoteControlStreamerAnimateGimbal(motion = motion)
    }

    override fun remoteControlStreamerSaveGimbalPreset() {
        model.remoteControlStreamerSaveGimbalPreset()
    }

    override fun remoteControlStreamerImportSettings(settings: ByteArray, onCompleted: (Boolean) -> Unit) {
        model.remoteControlStreamerImportSettings(settings = settings, onCompleted = onCompleted)
    }

    override fun remoteControlStreamerStartStats(filter: RemoteControlStartStatsFilter?) {
        model.remoteControlStreamerStartStats(filter = filter)
    }

    override fun remoteControlStreamerStopStats() {
        model.remoteControlStreamerStopStats()
    }

    override fun remoteControlStreamerStartMacro(id: UUID) {
        model.remoteControlStreamerStartMacro(id = id)
    }

    override fun remoteControlStreamerStopMacro(id: UUID) {
        model.remoteControlStreamerStopMacro(id = id)
    }

    override fun remoteControlStreamerSendMessage(text: String) {
        model.remoteControlStreamerSendMessage(text = text)
    }
}

private class RemoteControlAssistantDelegateAdapter(
    private val model: Model
) : RemoteControlAssistantDelegate {
    override fun remoteControlAssistantConnected() {
        model.remoteControlAssistantConnected()
    }

    override fun remoteControlAssistantDisconnected() {
        model.remoteControlAssistantDisconnected()
    }

    override fun remoteControlAssistantPreview(preview: ByteArray) {
        model.remoteControlAssistantPreview(preview = preview)
    }

    override fun remoteControlAssistantStateChanged(state: RemoteControlAssistantStreamerState) {
        model.remoteControlAssistantStateChanged(state = state)
    }

    override fun remoteControlAssistantLog(entry: String) {
        model.remoteControlAssistantLog(entry = entry)
    }

    override fun remoteControlAssistantStatus(
        general: RemoteControlStatusGeneral?,
        topLeft: RemoteControlStatusTopLeft?,
        topRight: RemoteControlStatusTopRight?
    ) {
        model.remoteControlAssistantStatus(general = general, topLeft = topLeft, topRight = topRight)
    }

    override fun remoteControlAssistantStats(data: RemoteControlStats) {
        model.remoteControlAssistantStats(data = data)
    }
}

private class RemoteControlWebDelegateAdapter(
    private val model: Model
) : RemoteControlWebDelegate {
    override fun remoteControlWebConnected() {
        model.remoteControlWebConnected()
    }

    override fun remoteControlWebDisconnected() {
        model.remoteControlWebDisconnected()
    }

    override fun remoteControlWebGetStatus(): Triple<RemoteControlStatusGeneral, RemoteControlStatusTopLeft, RemoteControlStatusTopRight> {
        return model.remoteControlWebGetStatus()
    }

    override fun remoteControlWebGetSettings(): RemoteControlSettings {
        return model.remoteControlWebGetSettings()
    }

    override fun remoteControlWebSetScene(id: UUID) {
        model.remoteControlWebSetScene(id = id)
    }

    override fun remoteControlWebSetAutoSceneSwitcher(id: UUID?) {
        model.remoteControlWebSetAutoSceneSwitcher(id = id)
    }

    override fun remoteControlWebSetMic(id: String) {
        model.remoteControlWebSetMic(id = id)
    }

    override fun remoteControlWebSetBitratePreset(id: UUID) {
        model.remoteControlWebSetBitratePreset(id = id)
    }

    override fun remoteControlWebSetRecord(on: Boolean) {
        model.remoteControlWebSetRecord(on = on)
    }

    override fun remoteControlWebSetLive(on: Boolean) {
        model.remoteControlWebSetLive(on = on)
    }

    override fun remoteControlWebSetPreviewStream(on: Boolean) {
        model.remoteControlWebSetPreviewStream(on = on)
    }

    override fun remoteControlWebSetZoom(x: Float) {
        model.remoteControlWebSetZoom(x = x)
    }

    override fun remoteControlWebSetZoomPreset(id: UUID) {
        model.remoteControlWebSetZoomPreset(id = id)
    }

    override fun remoteControlWebSetDebugLogging(on: Boolean) {
        model.remoteControlWebSetDebugLogging(on = on)
    }

    override fun remoteControlWebSetMute(on: Boolean) {
        model.remoteControlWebSetMute(on = on)
    }

    override fun remoteControlWebSetStealthMode(on: Boolean) {
        model.remoteControlWebSetStealthMode(on = on)
    }

    override fun remoteControlWebSetTorch(on: Boolean) {
        model.remoteControlWebSetTorch(on = on)
    }

    override fun remoteControlWebReloadBrowserWidgets() {
        model.remoteControlWebReloadBrowserWidgets()
    }

    override fun remoteControlWebSetSrtConnectionPrioritiesEnabled(enabled: Boolean) {
        model.remoteControlWebSetSrtConnectionPrioritiesEnabled(enabled = enabled)
    }

    override fun remoteControlWebSetSrtConnectionPriority(id: UUID, priority: Int, enabled: Boolean) {
        model.remoteControlWebSetSrtConnectionPriority(id = id, priority = priority, enabled = enabled)
    }

    override fun remoteControlWebMoveToGimbalPreset(id: UUID) {
        model.remoteControlWebMoveToGimbalPreset(id = id)
    }

    override fun remoteControlWebSetGimbalTracking(on: Boolean) {
        model.remoteControlWebSetGimbalTracking(on = on)
    }

    override fun remoteControlWebSetGimbalMovement(x: Float, y: Float) {
        model.remoteControlWebSetGimbalMovement(x = x, y = y)
    }

    override fun remoteControlWebAnimateGimbal(motion: SettingsGimbalMotion) {
        model.remoteControlWebAnimateGimbal(motion = motion)
    }

    override fun remoteControlWebSaveGimbalPreset() {
        model.remoteControlWebSaveGimbalPreset()
    }

    override fun remoteControlWebGetScoreboardSports(): List<String> {
        return model.remoteControlWebGetScoreboardSports()
    }

    override fun remoteControlWebSetScoreboardSport(sportId: String) {
        model.remoteControlWebSetScoreboardSport(sportId = sportId)
    }

    override fun remoteControlWebUpdateScoreboard(config: RemoteControlScoreboardMatchConfig) {
        model.remoteControlWebUpdateScoreboard(config = config)
    }

    override fun remoteControlWebToggleScoreboardClock() {
        model.remoteControlWebToggleScoreboardClock()
    }

    override fun remoteControlWebSetScoreboardDuration(minutes: Int) {
        model.remoteControlWebSetScoreboardDuration(minutes = minutes)
    }

    override fun remoteControlWebSetScoreboardClock(time: String) {
        model.remoteControlWebSetScoreboardClock(time = time)
    }

    override fun remoteControlWebGetGolfScoreboard(): RemoteControlGolfScoreboard {
        return model.remoteControlWebGetGolfScoreboard()
    }

    override fun remoteControlWebUpdateGolfScoreboard(data: RemoteControlGolfScoreboard) {
        model.remoteControlWebUpdateGolfScoreboard(data = data)
    }

    override fun remoteControlWebSetFilter(filter: RemoteControlFilter, on: Boolean) {
        model.remoteControlWebSetFilter(filter = filter, on = on)
    }

    override fun remoteControlWebTriggerReaction(reaction: RemoteControlReaction) {
        model.remoteControlWebTriggerReaction(reaction = reaction)
    }

    override fun remoteControlWebGetRecordings(): List<Map<String, String>> {
        return model.remoteControlWebGetRecordings()
    }

    override fun remoteControlWebGetRecordingUrl(filename: String): URI? {
        return model.remoteControlWebGetRecordingUrl(filename = filename)?.toURI()
    }

    override fun remoteControlWebGetRecordingThumbnail(filename: String): ByteArray? {
        return model.remoteControlWebGetRecordingThumbnail(filename = filename)
    }

    override fun remoteControlWebDeleteRecording(filename: String) {
        model.remoteControlWebDeleteRecording(filename = filename)
    }

    override fun remoteControlWebStartPreview() {
        model.remoteControlWebStartPreview()
    }

    override fun remoteControlWebStopPreview() {
        model.remoteControlWebStopPreview()
    }
}

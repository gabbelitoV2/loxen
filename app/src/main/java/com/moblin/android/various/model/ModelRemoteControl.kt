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
import com.moblin.android.remotecontrol.RemoteControlWeb
import com.moblin.android.remotecontrol.RemoteControlZoomPreset
import com.moblin.android.remotecontrol.remoteControlStartStatsFilterAllEnabled
import com.moblin.android.various.ChatHighlight
import com.moblin.android.various.Variables
import com.moblin.android.various.makeChatPostTextSegments
import com.moblin.android.various.settings.SettingsGimbalMotion
import com.moblin.android.various.settings.SettingsHttpHeader
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

class RemoteControl {
    internal val _general = MutableStateFlow<RemoteControlStatusGeneral?>(null)
    val general: StateFlow<RemoteControlStatusGeneral?> = _general.asStateFlow()
    internal val _topLeft = MutableStateFlow<RemoteControlStatusTopLeft?>(null)
    val topLeft: StateFlow<RemoteControlStatusTopLeft?> = _topLeft.asStateFlow()
    internal val _topRight = MutableStateFlow<RemoteControlStatusTopRight?>(null)
    val topRight: StateFlow<RemoteControlStatusTopRight?> = _topRight.asStateFlow()
    internal val _settings = MutableStateFlow<RemoteControlSettings?>(null)
    val settings: StateFlow<RemoteControlSettings?> = _settings.asStateFlow()
    internal val _scene = MutableStateFlow(UUID.randomUUID())
    val scene: StateFlow<UUID> = _scene.asStateFlow()
    internal val _autoSceneSwitcher = MutableStateFlow<UUID?>(null)
    val autoSceneSwitcher: StateFlow<UUID?> = _autoSceneSwitcher.asStateFlow()
    internal val _mic = MutableStateFlow("")
    val mic: StateFlow<String> = _mic.asStateFlow()
    internal val _bitrate = MutableStateFlow(UUID.randomUUID())
    val bitrate: StateFlow<UUID> = _bitrate.asStateFlow()
    internal val _zoom = MutableStateFlow("")
    val zoom: StateFlow<String> = _zoom.asStateFlow()
    internal val _zoomPresets = MutableStateFlow<List<RemoteControlZoomPreset>>(emptyList())
    val zoomPresets: StateFlow<List<RemoteControlZoomPreset>> = _zoomPresets.asStateFlow()
    internal val _gimbalPresets = MutableStateFlow<List<RemoteControlSettingsGimbalPreset>>(emptyList())
    val gimbalPresets: StateFlow<List<RemoteControlSettingsGimbalPreset>> = _gimbalPresets.asStateFlow()
    internal val _macros = MutableStateFlow<List<RemoteControlMacro>>(emptyList())
    val macros: StateFlow<List<RemoteControlMacro>> = _macros.asStateFlow()
    internal val _zoomPreset = MutableStateFlow(UUID.randomUUID())
    val zoomPreset: StateFlow<UUID> = _zoomPreset.asStateFlow()
    internal val _debugLogging = MutableStateFlow(false)
    val debugLogging: StateFlow<Boolean> = _debugLogging.asStateFlow()
    internal val _preview = MutableStateFlow<Bitmap?>(null)
    val preview: StateFlow<Bitmap?> = _preview.asStateFlow()
    internal val _recording = MutableStateFlow(false)
    val recording: StateFlow<Boolean> = _recording.asStateFlow()
    internal val _streaming = MutableStateFlow(false)
    val streaming: StateFlow<Boolean> = _streaming.asStateFlow()
    internal val _muted = MutableStateFlow(false)
    val muted: StateFlow<Boolean> = _muted.asStateFlow()
    internal val _stealthMode = MutableStateFlow(false)
    val stealthMode: StateFlow<Boolean> = _stealthMode.asStateFlow()
    internal val _previewStream = MutableStateFlow(false)
    val previewStream: StateFlow<Boolean> = _previewStream.asStateFlow()
    internal val _presentingPreview = MutableStateFlow(true)
    val presentingPreview: StateFlow<Boolean> = _presentingPreview.asStateFlow()
    internal val _presentingPreviewFullScreen = MutableStateFlow(false)
    val presentingPreviewFullScreen: StateFlow<Boolean> = _presentingPreviewFullScreen.asStateFlow()
    internal val _presentingStreamers = MutableStateFlow(false)
    val presentingStreamers: StateFlow<Boolean> = _presentingStreamers.asStateFlow()
    internal val _pixellate = MutableStateFlow(false)
    val pixellate: StateFlow<Boolean> = _pixellate.asStateFlow()
    internal val _movie = MutableStateFlow(false)
    val movie: StateFlow<Boolean> = _movie.asStateFlow()
    internal val _grayScale = MutableStateFlow(false)
    val grayScale: StateFlow<Boolean> = _grayScale.asStateFlow()
    internal val _sepia = MutableStateFlow(false)
    val sepia: StateFlow<Boolean> = _sepia.asStateFlow()
    internal val _triple = MutableStateFlow(false)
    val triple: StateFlow<Boolean> = _triple.asStateFlow()
    internal val _twin = MutableStateFlow(false)
    val twin: StateFlow<Boolean> = _twin.asStateFlow()
    internal val _fourThree = MutableStateFlow(false)
    val fourThree: StateFlow<Boolean> = _fourThree.asStateFlow()
    internal val _crt = MutableStateFlow(false)
    val crt: StateFlow<Boolean> = _crt.asStateFlow()
    internal val _pinch = MutableStateFlow(false)
    val pinch: StateFlow<Boolean> = _pinch.asStateFlow()
    internal val _whirlpool = MutableStateFlow(false)
    val whirlpool: StateFlow<Boolean> = _whirlpool.asStateFlow()
    internal val _poll = MutableStateFlow(false)
    val poll: StateFlow<Boolean> = _poll.asStateFlow()
    internal val _blurFaces = MutableStateFlow(false)
    val blurFaces: StateFlow<Boolean> = _blurFaces.asStateFlow()
    internal val _privacy = MutableStateFlow(false)
    val privacy: StateFlow<Boolean> = _privacy.asStateFlow()
    internal val _beauty = MutableStateFlow(false)
    val beauty: StateFlow<Boolean> = _beauty.asStateFlow()
    internal val _moblinInMouth = MutableStateFlow(false)
    val moblinInMouth: StateFlow<Boolean> = _moblinInMouth.asStateFlow()
    internal val _cameraMan = MutableStateFlow(false)
    val cameraMan: StateFlow<Boolean> = _cameraMan.asStateFlow()
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
    remoteControlAssistantLog = mutableListOf()
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
        clientUrl = url,
        password = database.remoteControl.password,
        delegate = this
    )
    remoteControlStreamer?.start()
}

private fun Model.remoteControlStreamerSendTwitchStart() {
    remoteControlStreamer?.twitchStart(
        channelName = stream.twitchChannelName,
        channelId = stream.twitchChannelId,
        accessToken = stream.twitchAccessToken
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
    if (status != statusTopRight.remoteControlStatus) {
        statusTopRight.remoteControlStatus = status
    }
    if (ok != statusTopRight.remoteControlOk) {
        statusTopRight.remoteControlOk = ok
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
        delegate = this
    )
    remoteControlAssistant?.start()
}

fun Model.isRemoteControlAssistantConnected(): Boolean {
    return remoteControlAssistant?.isConnected() ?: false
}

fun Model.updateRemoteControlAssistantStatus() {
    if (!(showingRemoteControl || isWatchRemoteControl()) || !isRemoteControlAssistantConnected()) {
        return
    }
    remoteControlAssistant?.getStatus { general, topLeft, topRight ->
        remoteControl._general.value = general
        remoteControl._topLeft.value = topLeft
        remoteControl._topRight.value = topRight
        if (isWatchRemoteControl()) {
            sendRemoteControlAssistantStatusToWatch()
        }
    }
    remoteControlAssistant?.getSettings { settings ->
        remoteControl._settings.value = settings
    }
}

fun Model.isRemoteControlAssistantConfigured(): Boolean {
    val assistant = database.remoteControl.assistant
    return assistant.enabled && assistant.port > 0 && database.remoteControl.password.isNotEmpty()
}

fun Model.remoteControlAssistantSetRemoteSceneSettings() {
    val data = RemoteControlRemoteSceneSettings(
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
        baseUrl = database.remoteControl.assistant.relay.baseUrl,
        bridgeId = database.remoteControl.assistant.relay.bridgeId,
        assistantUrl = assistantUrl
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
    general.batteryLevel = (100 * battery.level).toInt()
    when (statusOther.thermalState) {
        PowerManager.THERMAL_STATUS_NONE,
        PowerManager.THERMAL_STATUS_LIGHT -> general.flame = RemoteControlStatusGeneralFlame.white
        PowerManager.THERMAL_STATUS_MODERATE -> general.flame = RemoteControlStatusGeneralFlame.yellow
        else -> general.flame = RemoteControlStatusGeneralFlame.red
    }
    general.wiFiSsid = currentWiFiSsid
    general.isLive = isLive
    general.isRecording = isRecording
    general.isMuted = audio.muted
    return general
}

private fun Model.remoteControlStreamerCreateStatusTopLeft(): RemoteControlStatusTopLeft {
    val topLeft = RemoteControlStatusTopLeft()
    if (isStreamConfigured()) {
        topLeft.stream = RemoteControlStatusItem(message = statusTopLeft.streamText)
    }
    topLeft.camera = RemoteControlStatusItem(message = statusTopLeft.statusCameraText)
    topLeft.mic = RemoteControlStatusItem(message = mic.current.name)
    if (zoom.hasZoom) {
        topLeft.zoom = RemoteControlStatusItem(message = zoom.statusText())
    }
    if (isObsRemoteControlConfigured()) {
        topLeft.obs = RemoteControlStatusItem(message = statusTopLeft.statusObsText)
    }
    if (isEventsConfigured()) {
        topLeft.events = RemoteControlStatusItem(message = statusTopLeft.statusEventsText)
    }
    if (isChatConfigured()) {
        topLeft.chat = RemoteControlStatusItem(message = statusTopLeft.statusChatText)
    }
    if (isViewersConfigured() && isLive) {
        topLeft.viewers = RemoteControlStatusItem(message = statusViewersText())
    }
    return topLeft
}

private fun Model.remoteControlStreamerCreateStatusTopRight(): RemoteControlStatusTopRight {
    val topRight = RemoteControlStatusTopRight()
    val level = formatAudioLevel(level = audio.level.level, muted = audio.muted) +
        formatAudioLevelChannels(channels = audio.numberOfChannels)
    topRight.audioLevel = RemoteControlStatusItem(message = level)
    topRight.audioInfo = RemoteControlStatusTopRightAudioInfo(
        audioLevel = RemoteControlStatusTopRightAudioLevel.unknown,
        numberOfAudioChannels = audio.numberOfChannels
    )
    if (audio.muted) {
        topRight.audioInfo!!.audioLevel = RemoteControlStatusTopRightAudioLevel.muted
    } else {
        topRight.audioInfo!!.audioLevel = RemoteControlStatusTopRightAudioLevel.value(audio.level.level)
    }
    if (isIngestsConfigured()) {
        topRight.rtmpServer = RemoteControlStatusItem(message = ingests.speedAndTotal)
    }
    if (isAnyRemoteControlConfigured()) {
        topRight.remoteControl = RemoteControlStatusItem(message = statusTopRight.remoteControlStatus)
    }
    if (isGameControllerConnected()) {
        topRight.gameController = RemoteControlStatusItem(message = statusTopRight.gameControllersTotal)
    }
    if (isLive) {
        topRight.bitrate = RemoteControlStatusItem(message = bitrate.speedAndTotal)
    }
    if (isLive) {
        topRight.uptime = RemoteControlStatusItem(message = streamUptime.uptime)
    }
    if (isLocationEnabled()) {
        topRight.location = RemoteControlStatusItem(message = statusTopRight.location)
    }
    if (isStatusBondingActive()) {
        topRight.srtla = RemoteControlStatusItem(message = bonding.statistics)
    }
    if (isStatusBondingRttsActive()) {
        topRight.srtlaRtts = RemoteControlStatusItem(message = bonding.rtts)
    }
    if (isRecording) {
        topRight.recording = RemoteControlStatusItem(message = recording.length)
    }
    if (stream.replay.enabled) {
        topRight.replay = RemoteControlStatusItem(message = localized("Enabled"))
    }
    if (isStatusBrowserWidgetsActive()) {
        topRight.browserWidgets = RemoteControlStatusItem(message = statusTopRight.browserWidgetsStatus)
    }
    if (isAnyMoblinkConfigured()) {
        topRight.moblink = RemoteControlStatusItem(message = moblink.status)
    }
    if (statusTopRight.djiDevicesStatus.isNotEmpty()) {
        topRight.djiDevices = RemoteControlStatusItem(message = statusTopRight.djiDevicesStatus)
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
    val weather = weatherManager.getLatestWeather()?.currentWeather
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
            temperature = weather?.temperature,
            feelsLikeTemperature = weather?.apparentTemperature,
            windSpeed = weather?.wind.speed,
            windGust = weather?.wind.gust,
            country = placemark?.country,
            countryFlag = emojiFlag(countryCode = placemark?.isoCountryCode),
            state = placemark?.administrativeArea,
            area = placemark?.subAdministrativeArea,
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
    if (sceneSelector.sceneIndex < enabledScenes.size) {
        state.scene = enabledScenes[sceneSelector.sceneIndex].id
    }
    state.autoSceneSwitcher = RemoteControlStateAutoSceneSwitcher(id = autoSceneSwitcher.currentSwitcherId)
    state.mic = mic.current.id
    val preset = getBitratePresetByBitrate(bitrate = stream.bitrate)
    if (preset != null) {
        state.bitrate = preset.id
    }
    when (cameraPosition) {
        CameraPosition.front -> {
            state.zoomPresets = zoom.frontZoomPresets.map {
                RemoteControlZoomPreset(id = it.id, name = it.name)
            }
            state.zoomPreset = zoom.frontPresetId
        }
        CameraPosition.back -> {
            state.zoomPresets = zoom.backZoomPresets.map {
                RemoteControlZoomPreset(id = it.id, name = it.name)
            }
            state.zoomPreset = zoom.backPresetId
        }
        else -> {
            state.zoomPresets = mutableListOf()
        }
    }
    state.zoom = zoom.x
    state.debugLogging = database.debug.debugLogging
    state.streaming = isLive
    state.recording = isRecording
    state.muted = audio.muted
    state.stealthMode = showStealthMode
    state.previewStream = isPreviewStreaming
    state.torchOn = streamOverlay.isTorchOn
    state.batteryCharging = isBatteryCharging()
    state.filters = mutableMapOf()
    for (filter in RemoteControlFilter.entries) {
        state.filters?.put(filter, getQuickButton(type = filter.toSettings())?.isOn ?: false)
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
    remoteControlWeb = RemoteControlWeb(delegate = this)
    remoteControlWeb?.start(port = database.remoteControl.web.port)
}

private fun Model.handleRemoteControlSetFilter(filter: RemoteControlFilter, on: Boolean) {
    when (filter) {
        RemoteControlFilter.pixellate -> setPixellateQuickButton(on = on)
        RemoteControlFilter.movie -> setFilterQuickButton(type = SettingsQuickButtonType.movie, on = on)
        RemoteControlFilter.grayScale -> setFilterQuickButton(type = SettingsQuickButtonType.grayScale, on = on)
        RemoteControlFilter.sepia -> setFilterQuickButton(type = SettingsQuickButtonType.sepia, on = on)
        RemoteControlFilter.triple -> setFilterQuickButton(type = SettingsQuickButtonType.triple, on = on)
        RemoteControlFilter.twin -> setFilterQuickButton(type = SettingsQuickButtonType.twin, on = on)
        RemoteControlFilter.fourThree -> setFilterQuickButton(type = SettingsQuickButtonType.fourThree, on = on)
        RemoteControlFilter.crt -> setFilterQuickButton(type = SettingsQuickButtonType.crt, on = on)
        RemoteControlFilter.pinch -> setPinchQuickButton(on = on)
        RemoteControlFilter.whirlpool -> setWhirlpoolQuickButton(on = on)
        RemoteControlFilter.poll -> setPollQuickButton(on = on)
        RemoteControlFilter.blurFaces -> setBlurFaces(on = on)
        RemoteControlFilter.privacy -> setPrivacy(on = on)
        RemoteControlFilter.beauty -> setBeautyQuickButton(on = on)
        RemoteControlFilter.moblinInMouth -> setMoblinInMouth(on = on)
        RemoteControlFilter.cameraMan -> setCameraManQuickButton(on = on)
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
    val mics = database.mics.mics.map {
        RemoteControlSettingsMic(id = it.id, name = it.name)
    }
    val bitratePresets = database.bitratePresets.map {
        RemoteControlSettingsBitratePreset(id = it.id, bitrate = it.bitrate)
    }
    val connectionPriorities = stream.srt.connectionPriorities.priorities.map {
        RemoteControlSettingsSrtConnectionPriority(
            id = it.id,
            name = it.name,
            priority = it.priority,
            enabled = it.enabled
        )
    }
    val connectionPrioritiesEnabled = stream.srt.connectionPriorities.enabled
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
    if (stream.enabled || isLive || isRecording) {
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
    setBitrate(bitrate = preset.bitrate)
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
    database.debug.debugLogging = on
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
    streamOverlay.isTorchOn = on
    updateTorch()
    setQuickButton(type = SettingsQuickButtonType.torch, isOn = on)
}

fun Model.remoteControlStreamerReloadBrowserWidgets() {
    reloadBrowserWidgets()
}

fun Model.remoteControlStreamerSetSrtConnectionPrioritiesEnabled(enabled: Boolean) {
    stream.srt.connectionPriorities.enabled = enabled
    updateSrtlaPriorities()
}

fun Model.remoteControlStreamerSetSrtConnectionPriority(id: UUID, priority: Int, enabled: Boolean) {
    val entry = stream.srt.connectionPriorities.priorities.firstOrNull { it.id == id }
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
        timestamp = statusOther.digitalClock,
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
    if (isLive || isRecording) {
        onCompleted(false)
        return
    }
    importSettingsFromData(settings = settings) { onCompleted(it) }
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
    remoteControl._topLeft.value = null
    remoteControl._topRight.value = null
    updateRemoteControlStatus()
}

fun Model.remoteControlAssistantStateChanged(state: RemoteControlAssistantStreamerState) {
    state.scene?.let {
        remoteControlAssistantStreamerState.scene = it
        remoteControl._scene.value = it
    }
    state.autoSceneSwitcher?.let {
        remoteControlAssistantStreamerState.autoSceneSwitcher = it
        remoteControl._autoSceneSwitcher.value = it.id
    }
    state.mic?.let {
        remoteControlAssistantStreamerState.mic = it
        remoteControl._mic.value = it
    }
    state.bitrate?.let {
        remoteControlAssistantStreamerState.bitrate = it
        remoteControl._bitrate.value = it
    }
    state.zoomPresets?.let {
        remoteControlAssistantStreamerState.zoomPresets = it
        remoteControl._zoomPresets.value = it
    }
    state.gimbalPresets?.let {
        remoteControlAssistantStreamerState.gimbalPresets = it
        remoteControl._gimbalPresets.value = it
    }
    state.macros?.let {
        remoteControlAssistantStreamerState.macros = it
        remoteControl._macros.value = it
    }
    state.zoomPreset?.let {
        remoteControlAssistantStreamerState.zoomPreset = it
        remoteControl._zoomPreset.value = it
    }
    state.zoom?.let {
        remoteControlAssistantStreamerState.zoom = it
        remoteControl._zoom.value = it.toString()
    }
    state.debugLogging?.let {
        remoteControlAssistantStreamerState.debugLogging = it
        remoteControl._debugLogging.value = it
    }
    state.streaming?.let {
        remoteControlAssistantStreamerState.streaming = it
        remoteControl._streaming.value = it
    }
    state.recording?.let {
        remoteControlAssistantStreamerState.recording = it
        remoteControl._recording.value = it
    }
    state.muted?.let {
        remoteControlAssistantStreamerState.muted = it
        remoteControl._muted.value = it
    }
    state.stealthMode?.let {
        remoteControlAssistantStreamerState.stealthMode = it
        remoteControl._stealthMode.value = it
    }
    state.previewStream?.let {
        remoteControlAssistantStreamerState.previewStream = it
        remoteControl._previewStream.value = it
    }
    state.filters?.let { filters ->
        for ((filter, on) in filters) {
            remoteControlAssistantStreamerState.filters?.put(filter, on)
            when (filter) {
                RemoteControlFilter.pixellate -> remoteControl._pixellate.value = on
                RemoteControlFilter.movie -> remoteControl._movie.value = on
                RemoteControlFilter.grayScale -> remoteControl._grayScale.value = on
                RemoteControlFilter.sepia -> remoteControl._sepia.value = on
                RemoteControlFilter.triple -> remoteControl._triple.value = on
                RemoteControlFilter.twin -> remoteControl._twin.value = on
                RemoteControlFilter.fourThree -> remoteControl._fourThree.value = on
                RemoteControlFilter.crt -> remoteControl._crt.value = on
                RemoteControlFilter.pinch -> remoteControl._pinch.value = on
                RemoteControlFilter.whirlpool -> remoteControl._whirlpool.value = on
                RemoteControlFilter.poll -> remoteControl._poll.value = on
                RemoteControlFilter.blurFaces -> remoteControl._blurFaces.value = on
                RemoteControlFilter.privacy -> remoteControl._privacy.value = on
                RemoteControlFilter.beauty -> remoteControl._beauty.value = on
                RemoteControlFilter.moblinInMouth -> remoteControl._moblinInMouth.value = on
                RemoteControlFilter.cameraMan -> remoteControl._cameraMan.value = on
            }
        }
    }
    if (isWatchRemoteControl()) {
        sendRemoteControlAssistantStatusToWatch()
    }
}

fun Model.remoteControlAssistantPreview(preview: ByteArray) {
    remoteControl._preview.value = BitmapFactory.decodeByteArray(preview, 0, preview.size)
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
        remoteControl._topRight.value = topRight
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
            mapOf("name" to filename, "size" to size.formatBytes())
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

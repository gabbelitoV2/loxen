package com.moblin.android.various.model

import com.moblin.android.common.various.RgbColor
import com.moblin.android.common.various.noValue
import com.moblin.android.localized
import com.moblin.android.platform.Bundle
import com.moblin.android.platform.core.CharacterSet
import com.moblin.android.platform.core.addingPercentEncoding
import com.moblin.android.remotecontrol.RemoteControlChatMessage
import com.moblin.android.remotecontrol.RemoteControlFilter
import com.moblin.android.remotecontrol.RemoteControlMusicCommand
import com.moblin.android.remotecontrol.RemoteControlReaction
import com.moblin.android.remotecontrol.RemoteControlRemoteSceneData
import com.moblin.android.remotecontrol.RemoteControlRemoteSceneSettings
import com.moblin.android.remotecontrol.RemoteControlResponse
import com.moblin.android.remotecontrol.RemoteControlResult
import com.moblin.android.remotecontrol.RemoteControlScoreboardMatchConfig
import com.moblin.android.remotecontrol.RemoteControlSettings
import com.moblin.android.remotecontrol.RemoteControlSettingsSrt
import com.moblin.android.remotecontrol.RemoteControlStartStatsFilter
import com.moblin.android.remotecontrol.RemoteControlStartStatusFilter
import com.moblin.android.remotecontrol.RemoteControlStatusGeneral
import com.moblin.android.remotecontrol.RemoteControlStatusTopLeft
import com.moblin.android.remotecontrol.RemoteControlStatusTopRight
import com.moblin.android.remotecontrol.RemoteControlStreamer
import com.moblin.android.remotecontrol.RemoteControlStreamerDelegate
import com.moblin.android.remotecontrol.RemoteControlTipKind
import com.moblin.android.remotecontrol.RemoteControlWheelOfLuck
import com.moblin.android.remotecontrol.RemoteControlWheelOfLuckOption
import com.moblin.android.various.AudioPlayer
import com.moblin.android.various.ChatHighlight
import com.moblin.android.various.makeChatPostTextSegments
import com.moblin.android.various.settings.SettingsGimbalMotion
import com.moblin.android.various.settings.SettingsHttpHeader
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetType
import com.moblin.android.videoeffects.alerts.AlertsEffectAlert
import java.net.URI
import java.time.Instant
import java.util.UUID
import com.moblin.android.various.utils.add
import com.moblin.android.platform.log.Log
import com.moblin.android.platform.codable.DataSerializer
import com.moblin.android.platform.codable.codableJson
import kotlinx.coroutines.CoroutineScope
import com.moblin.android.remotecontrol.RemoteControlAppAttest
import com.moblin.android.remotecontrol.RemoteControlAssistantStreamerState
import com.moblin.android.various.network.NetworkResponse
import com.moblin.android.various.network.httpUrlSession
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.various.storages.SimpleStringStorage
import com.moblin.android.various.utils.isMac
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlin.coroutines.resume
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.util.Base64

private const val TAG = "MoblinCloud"

private val mainScope = CoroutineScope(Dispatchers.Main)

val moblinWebsiteApiUrl: String = ""
private val moblinCloudApiUrl: String = ""
private val liveUrl: String = "$moblinWebsiteApiUrl/streamers/live"
private val challengeUrl: String = "$moblinWebsiteApiUrl/streamers/live/challenge"
private val appAttestStorage = SimpleStringStorage(key = "moblinWebsiteAppAttest")

@Serializable
private data class MoblinCloudChannel(val platform: String, val name: String)

@Serializable
private data class MoblinCloudLive(
    val channels: List<MoblinCloudChannel>,
    val challenge: String,
    val keyId: String,
    val attestation: String,
    val attestationChallenge: String,
)

@Serializable
private data class MoblinCloudChallenge(val challenge: String)

@Serializable
private data class MoblinCloudAppAttest(
    val keyId: String,
    @Serializable(with = DataSerializer::class) val challenge: ByteArray,
    @Serializable(with = DataSerializer::class) var attestation: ByteArray? = null,
) {
    fun attestationBase64(): String {
        return Base64.getEncoder().encodeToString(attestation!!)
    }

    fun challengeBase64(): String {
        return Base64.getEncoder().encodeToString(challenge)
    }
}

private sealed class MoblinCloudError : Exception() {
    class NotSupported : MoblinCloudError()
    class KeyRejected(val reason: String) : MoblinCloudError()
    class BadResponse(val reason: String) : MoblinCloudError()
}

private fun describe(response: Response, data: ByteArray): String {
    return "${response.code} ${data.decodeToString()}"
}

private suspend fun performRequest(request: Request): Pair<ByteArray, Response> {
    return withContext(Dispatchers.IO) {
        httpUrlSession().newCall(request).execute().use { response ->
            Pair(response.body?.bytes() ?: ByteArray(0), response)
        }
    }
}

private fun loadAppAttest(): MoblinCloudAppAttest? {
    return runCatching {
        codableJson.decodeFromString(MoblinCloudAppAttest.serializer(), appAttestStorage.get())
    }.getOrNull()
}

private fun storeAppAttest(appAttest: MoblinCloudAppAttest?) {
    val data = if (appAttest != null) {
        runCatching {
            codableJson.encodeToString(MoblinCloudAppAttest.serializer(), appAttest)
        }.getOrNull()
    } else {
        null
    }
    if (data == null) {
        appAttestStorage.set("")
        return
    }
    appAttestStorage.set(data)
}

private suspend fun attestedKey(): MoblinCloudAppAttest =
    TODO("no Android counterpart for DeviceCheck")

private suspend fun <T> withAttestedKey(body: suspend (MoblinCloudAppAttest) -> T): T =
    TODO("no Android counterpart for DeviceCheck")

private suspend fun generateAssertion(appAttest: MoblinCloudAppAttest, clientData: ByteArray): ByteArray =
    ByteArray(0)

private suspend fun fetchChallenge(): String {
    val request = Request.Builder()
        .url(challengeUrl)
        .post(ByteArray(0).toRequestBody(null))
        .build()
    val (data, response) = performRequest(request)
    if (!response.isSuccessful) {
        throw MoblinCloudError.BadResponse("challenge: ${describe(response, data)}")
    }
    return codableJson.decodeFromString(MoblinCloudChallenge.serializer(), data.decodeToString()).challenge
}

private suspend fun postLive(channels: List<MoblinCloudChannel>, appAttest: MoblinCloudAppAttest) {
    val challenge = fetchChallenge()
    val live = MoblinCloudLive(
        channels = channels,
        challenge = challenge,
        keyId = appAttest.keyId,
        attestation = appAttest.attestationBase64(),
        attestationChallenge = appAttest.challengeBase64(),
    )
    val body = codableJson.encodeToString(MoblinCloudLive.serializer(), live).encodeToByteArray()
    val assertion = generateAssertion(appAttest = appAttest, clientData = body)
    val request = Request.Builder()
        .url(liveUrl)
        .header("Content-Type", "application/json")
        .header("Moblin-Assertion", Base64.getEncoder().encodeToString(assertion))
        .post(body.toRequestBody("application/json".toMediaType()))
        .build()
    val (data, response) = performRequest(request)
    if (response.code == 401) {
        throw MoblinCloudError.KeyRejected("live: ${describe(response, data)}")
    }
    if (!response.isSuccessful) {
        throw MoblinCloudError.BadResponse("live: ${describe(response, data)}")
    }
}

private suspend fun makeAppAttest(
    challenge: ByteArray,
    appAttest: MoblinCloudAppAttest,
): RemoteControlAppAttest {
    val assertion = generateAssertion(appAttest = appAttest, clientData = challenge)
    return RemoteControlAppAttest(
        keyId = appAttest.keyId,
        attestation = appAttest.attestationBase64(),
        attestationChallenge = appAttest.challengeBase64(),
        assertion = Base64.getEncoder().encodeToString(assertion),
    )
}

private suspend fun makeMoblinCloudAppAttest(challenge: ByteArray): RemoteControlAppAttest? {
    return try {
        withAttestedKey { makeAppAttest(challenge = challenge, appAttest = it) }
    } catch (error: Exception) {
        Log.i(TAG, "moblin-website: App Attest failed: $error")
        null
    }
}

private suspend fun sendLive(channels: List<MoblinCloudChannel>) {
    try {
        withAttestedKey { postLive(channels = channels, appAttest = it) }
    } catch (error: Exception) {
        Log.i(TAG, "moblin-website: Failed to send live: $error")
    }
}

private fun makeMoblinCloudUrl(key: String): String? {
    val components = runCatching { URI(moblinCloudApiUrl) }.getOrNull() ?: return null
    val scheme = when (components.scheme) {
        "https" -> "wss"
        "http" -> "ws"
        else -> return null
    }
    val encodedKey = key.addingPercentEncoding(CharacterSet.urlPathAllowed) ?: return null
    val authority = components.rawAuthority ?: return null
    val path = (components.rawPath ?: "") + "/remote-control/streamer/$encodedKey"
    val query = components.rawQuery?.let { "?$it" } ?: ""
    val fragment = components.rawFragment?.let { "#$it" } ?: ""
    return "$scheme://$authority$path$query$fragment"
}

fun Model.sendLiveToMoblinWebsite(onCompleted: (() -> Unit)? = null) {
    val stream = stream.value
    if (isMac() || !stream.goLiveNotificationMoblinWebsite) {
        onCompleted?.invoke()
        return
    }
    mainScope.launch {
        try {
            val channels = mutableListOf<MoblinCloudChannel>()
            if (stream.twitchLoggedIn) {
                val name = fetchTwitchChannelName(stream = stream)
                if (name != null && name.isNotEmpty()) {
                    channels.add(MoblinCloudChannel(platform = "twitch", name = name))
                }
            }
            if (stream.isYouTubeAuthorized()) {
                val name = fetchYouTubeHandle(stream = stream)
                if (name != null && name.isNotEmpty()) {
                    channels.add(MoblinCloudChannel(platform = "youtube", name = name))
                }
            }
            if (stream.kickLoggedIn) {
                val name = fetchKickChannelName(stream = stream)
                if (name != null && name.isNotEmpty()) {
                    channels.add(MoblinCloudChannel(platform = "kick", name = name))
                }
            }
            if (channels.isEmpty()) {
                return@launch
            }
            sendLive(channels = channels)
        } finally {
            onCompleted?.invoke()
        }
    }
}

fun Model.stopMoblinCloud() {
    moblinCloud?.stop()
    moblinCloud = null
    setLowFpsImage()
}

fun Model.isMoblinCloudPreviewActive(): Boolean {
    return moblinCloud?.isRequestingPreview() ?: false
}

fun Model.sendPreviewToMoblinCloud(preview: ByteArray) {
    moblinCloud?.sendPreview(preview = preview)
}

fun Model.reloadMoblinCloud() {
    stopMoblinCloud()
    val settings = database.moblinCloud
    val key = settings.key.trim()
    if (isMac() || !settings.enabled || key.isEmpty()) {
        updateMoblinCloudStatus()
        return
    }
    val url = makeMoblinCloudUrl(key)
    if (url == null) {
        updateMoblinCloudStatus()
        return
    }
    moblinCloud = MoblinCloud(
        model = this,
        url = url,
        streamerId = getRemoteControlStreamerId(),
    )
    moblinCloud?.start()
    updateMoblinCloudStatus()
}

fun Model.isShowingStatusMoblinCloud(): Boolean {
    return database.show.moblinCloud && database.moblinCloud.enabled && !isMac()
}

fun Model.updateMoblinCloudStatus() {
    val ok = moblinCloud?.isConnected() ?: false
    val cloud = moblinCloud
    val status = if (ok) {
        localized("Connected")
    } else if (cloud != null) {
        cloud.connectionErrorMessage()
    } else {
        noValue
    }
    if (status != statusTopRight.moblinCloudStatus.value) {
        statusTopRight.moblinCloudStatus.value = status
    }
    if (ok != statusTopRight.moblinCloudOk.value) {
        statusTopRight.moblinCloudOk.value = ok
    }
}

fun Model.getMoblinCloudWheelOfLuckWidget(): SettingsWidget? {
    val id = database.moblinCloud.wheelOfLuckWidgetId ?: return null
    return database.widgets.firstOrNull {
        it.id == id && it.type == SettingsWidgetType.wheelOfLuck
    }
}

fun Model.isMoblinCloudWheelOfLuckWidget(widget: SettingsWidget): Boolean {
    return widget.id == database.moblinCloud.wheelOfLuckWidgetId
}

fun Model.moblinCloudSendWheelOfLuck() {
    val wheel = getMoblinCloudWheelOfLuckWidget()?.let { widget ->
        RemoteControlWheelOfLuck(
            options = widget.wheelOfLuck.options.map {
                RemoteControlWheelOfLuckOption(text = it.text, weight = it.weight)
            },
        )
    }
    moblinCloud?.sendWheelOfLuck(wheel = wheel)
}

fun Model.moblinCloudSendIcon() {
    moblinCloud?.sendIcon(id = database.iconImage)
}

fun Model.moblinCloudPlayTipAlert(name: String, message: String, amount: String, currency: String) {
    val id = database.moblinCloud.alertsWidgetId ?: return
    val effect = getAlertsEffect(id = id) ?: return
    if (!enabledAlertsEffects.any { it === effect }) {
        return
    }
    effect.play(
        alert = AlertsEffectAlert.MoblinCloudTip(
            name = name,
            message = message,
            amount = amount,
            currency = currency,
        ),
    )
}

fun Model.moblinCloudTip(name: String, message: String, kind: RemoteControlTipKind, requestId: String) {
    if (isRemoteControlRequestAlreadyHandled(requestId = requestId)) {
        return
    }
    when (kind) {
        is RemoteControlTipKind.Message -> moblinCloudShowTipMessage(
            name = name,
            message = message,
            amount = kind.amount,
            currency = kind.currency,
        )
        is RemoteControlTipKind.Effect -> moblinCloudTipEffect(
            name = name,
            message = message,
            sound = kind.sound,
        )
        is RemoteControlTipKind.WheelOfLuck -> moblinCloudTipWheelOfLuck(
            name = name,
            message = message,
            count = kind.count,
        )
    }
}

private fun Model.moblinCloudShowTipMessage(name: String, message: String, amount: String, currency: String) {
    var text = localized("Tipped $amount $currency")
    if (message.isNotEmpty()) {
        text += ": $message"
    }
    moblinCloudAppendChatMessage(name = name, text = text)
    moblinCloudPlayTipAlert(name = name, message = message, amount = amount, currency = currency)
}

private fun Model.moblinCloudTipEffect(name: String, message: String, sound: String) {
    if (!database.alertsMediaGallery.bundledSounds.any { it.name == sound }) {
        return
    }
    val url = Bundle.url("Alerts.bundle/$sound", "mp3") ?: return
    var text = localized("Played $sound")
    if (message.isNotEmpty()) {
        text += ": $message"
    }
    moblinCloudTipEffects.add(Triple(name, text, url))
    moblinCloudTryNextEffect()
}

private fun Model.moblinCloudTryNextEffect() {
    if (moblinCloudTipEffectPlayer != null) {
        return
    }
    val effect = moblinCloudTipEffects.removeFirstOrNull() ?: return
    val (name, text, url) = effect
    moblinCloudAppendChatMessage(name = name, text = text)
    moblinCloudTipEffectPlayer = runCatching { AudioPlayer(contentsOf = url) }.getOrNull()
    moblinCloudTipEffectPlayer?.play()
    val timeUntilNextEffect = (moblinCloudTipEffectPlayer?.duration() ?: 0.0) + 3
    moblinCloudTipEffectTimer.startSingleShot(timeUntilNextEffect) {
        moblinCloudTipEffectPlayer = null
        moblinCloudTryNextEffect()
    }
}

private fun Model.moblinCloudTipWheelOfLuck(name: String, message: String, count: Int) {
    val widget = getMoblinCloudWheelOfLuckWidget() ?: return
    getWheelOfLuckEffect(id = widget.id)?.spin(
        name = name,
        message = message,
        count = count.coerceIn(1, 10),
    )
}

private fun Model.moblinCloudAppendChatMessage(name: String, text: String) {
    appendChatMessage(
        platform = null,
        messageId = null,
        displayName = name,
        user = name,
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
        live = true,
    )
}

private suspend fun Model.fetchTwitchChannelName(stream: SettingsStream): String? {
    return suspendCancellableCoroutine { continuation ->
        createTwitchApi(stream = stream).getUserInfo { info ->
            continuation.resume(info?.login?.trim())
        }
    }
}

private suspend fun Model.fetchYouTubeHandle(stream: SettingsStream): String? {
    return suspendCancellableCoroutine { continuation ->
        getYouTubeApi(stream = stream) { youTubeApi ->
            if (youTubeApi == null) {
                continuation.resume(null)
                return@getYouTubeApi
            }
            youTubeApi.listChannels { result ->
                val handle = (result as? NetworkResponse.Success)?.value?.items?.firstOrNull()?.snippet?.customUrl
                    ?.trim()
                    ?.removePrefix("@")
                continuation.resume(handle)
            }
        }
    }
}

private suspend fun Model.fetchKickChannelName(stream: SettingsStream): String? {
    return suspendCancellableCoroutine { continuation ->
        createKickApi(stream = stream).getUser { user ->
            continuation.resume(user?.username?.trim())
        }
    }
}

open class MoblinCloud(model: Model, url: String, streamerId: String) : RemoteControlStreamerDelegate {
    private var model: Model? = model
    private var streamer: RemoteControlStreamer? = RemoteControlStreamer(
        clientUrl = url,
        password = "",
        streamerId = streamerId,
        delegate = this,
    )
    private var requestingStatus = false
    private var statusFilter: RemoteControlStartStatusFilter? = null
    private var requestingPreview = false

    open fun start() {
        streamer?.start()
    }

    open fun stop() {
        streamer?.stop()
        streamer = null
    }

    open fun isConnected(): Boolean {
        return streamer?.isConnected() ?: false
    }

    open fun connectionErrorMessage(): String {
        return streamer?.connectionErrorMessage ?: ""
    }

    open fun sendWheelOfLuck(wheel: RemoteControlWheelOfLuck?) {
        streamer?.sendWheelOfLuck(wheel = wheel)
    }

    open fun sendIcon(id: String) {
        streamer?.sendIcon(id = id)
    }

    open fun stateChanged(state: RemoteControlAssistantStreamerState) {
        if (!requestingStatus) {
            return
        }
        streamer?.stateChanged(state = state)
    }

    open fun isRequestingPreview(): Boolean {
        return requestingPreview
    }

    open fun sendPreview(preview: ByteArray) {
        val streamer = streamer
        if (!requestingPreview || streamer == null || !streamer.isConnected()) {
            return
        }
        streamer.sendPreview(preview = preview)
    }

    open fun sendPeriodicStatus() {
        val streamer = streamer
        val model = model
        if (!requestingStatus || streamer == null || !streamer.isConnected() || model == null) {
            return
        }
        val (general, topLeft, topRight) = model.createRemoteControlStatus(filter = statusFilter)
        streamer.sendStatus(general = general, topLeft = topLeft, topRight = topRight)
    }

    private fun setRequestingStatus(on: Boolean, filter: RemoteControlStartStatusFilter? = null) {
        requestingStatus = on
        statusFilter = filter
    }

    private fun setRequestingPreview(on: Boolean) {
        requestingPreview = on
        model?.setLowFpsImage()
    }

    override fun remoteControlStreamerAppAttest(
        challenge: ByteArray,
        onCompleted: (RemoteControlAppAttest?) -> Unit,
    ) {
        mainScope.launch {
            onCompleted(makeMoblinCloudAppAttest(challenge = challenge))
        }
    }

    override fun remoteControlStreamerConnected() {
        setRequestingStatus(on = false)
        setRequestingPreview(on = false)
        model?.updateMoblinCloudStatus()
        model?.moblinCloudSendWheelOfLuck()
        model?.moblinCloudSendIcon()
    }

    override fun remoteControlStreamerDisconnected() {
        setRequestingStatus(on = false)
        setRequestingPreview(on = false)
        model?.updateMoblinCloudStatus()
    }

    override fun remoteControlStreamerWrongPassword() {
    }

    override fun remoteControlStreamerGetStatus(): Triple<RemoteControlStatusGeneral, RemoteControlStatusTopLeft, RemoteControlStatusTopRight> {
        return model?.remoteControlGetStatus() ?: Triple(
            RemoteControlStatusGeneral(),
            RemoteControlStatusTopLeft(),
            RemoteControlStatusTopRight(),
        )
    }

    override fun remoteControlStreamerGetSettings(): RemoteControlSettings {
        return model?.remoteControlGetSettings() ?: RemoteControlSettings(
            streams = emptyList(),
            scenes = emptyList(),
            autoSceneSwitchers = null,
            bitratePresets = emptyList(),
            mics = emptyList(),
            srt = RemoteControlSettingsSrt(
                connectionPrioritiesEnabled = false,
                connectionPriorities = emptyList(),
            ),
        )
    }

    override fun remoteControlStreamerSetStream(id: UUID) {
        model?.remoteControlSetStream(id = id)
    }

    override fun remoteControlStreamerSetScene(id: UUID) {
        model?.selectScene(id = id)
    }

    override fun remoteControlStreamerSetAutoSceneSwitcher(id: UUID?) {
        model?.setAutoSceneSwitcher(id = id)
    }

    override fun remoteControlStreamerSetMic(id: String) {
        model?.manualSelectMicById(id = id)
    }

    override fun remoteControlStreamerSetTalkbackMic(id: String) {
    }

    override fun remoteControlStreamerSetBitratePreset(id: UUID) {
        model?.remoteControlSetBitratePreset(id = id)
    }

    override fun remoteControlStreamerSetRecord(on: Boolean) {
        model?.remoteControlSetRecord(on = on)
    }

    override fun remoteControlStreamerSetLive(on: Boolean) {
        model?.remoteControlSetLive(on = on)
    }

    override fun remoteControlStreamerSetPreviewStream(on: Boolean) {
        model?.remoteControlSetPreviewStream(on = on)
    }

    override fun remoteControlStreamerSetDebugLogging(on: Boolean) {
    }

    override fun remoteControlStreamerSetZoom(x: Float) {
        model?.remoteControlSetZoom(x = x)
    }

    override fun remoteControlStreamerSetZoomPreset(id: UUID) {
        model?.setZoomPreset(id = id)
    }

    override fun remoteControlStreamerSetMute(on: Boolean) {
        model?.setMuteOn(value = on)
    }

    override fun remoteControlStreamerSetStealthMode(on: Boolean) {
        model?.setStealthMode(on = on)
    }

    override fun remoteControlStreamerSetTorch(on: Boolean) {
        model?.remoteControlSetTorch(on = on)
    }

    override fun remoteControlStreamerReloadBrowserWidgets() {
        model?.reloadBrowserWidgets()
    }

    override fun remoteControlStreamerSetSrtConnectionPriority(id: UUID, priority: Int, enabled: Boolean) {
        model?.remoteControlSetSrtConnectionPriority(id = id, priority = priority, enabled = enabled)
    }

    override fun remoteControlStreamerSetSrtConnectionPrioritiesEnabled(enabled: Boolean) {
        model?.remoteControlSetSrtConnectionPrioritiesEnabled(enabled = enabled)
    }

    override fun remoteControlStreamerTwitchEventSubNotification(message: String) {
    }

    override fun remoteControlStreamerChatMessages(history: Boolean, messages: List<RemoteControlChatMessage>) {
    }

    override fun remoteControlStreamerStartPreview() {
        setRequestingPreview(on = true)
    }

    override fun remoteControlStreamerStopPreview() {
        setRequestingPreview(on = false)
    }

    override fun remoteControlStreamerSetRemoteSceneSettings(data: RemoteControlRemoteSceneSettings) {
    }

    override fun remoteControlStreamerSetRemoteSceneData(data: RemoteControlRemoteSceneData) {
    }

    override fun remoteControlStreamerInstantReplay() {
        model?.instantReplay()
    }

    override fun remoteControlStreamerSaveReplay() {
        model?.saveReplay()
    }

    override fun remoteControlStreamerStartStatus(interval: Int, filter: RemoteControlStartStatusFilter) {
        setRequestingStatus(on = true, filter = filter)
        val model = model
        if (model != null) {
            streamer?.stateChanged(state = model.createRemoteControlStateChanged())
        }
        sendPeriodicStatus()
    }

    override fun remoteControlStreamerStopStatus() {
        setRequestingStatus(on = false)
    }

    override fun remoteControlStreamerGetScoreboardSports(): List<String> {
        return emptyList()
    }

    override fun remoteControlStreamerSetScoreboardSport(sportId: String) {
    }

    override fun remoteControlStreamerUpdateScoreboard(config: RemoteControlScoreboardMatchConfig) {
    }

    override fun remoteControlStreamerToggleScoreboardClock() {
    }

    override fun remoteControlStreamerSetScoreboardDuration(minutes: Int) {
    }

    override fun remoteControlStreamerSetScoreboardClock(time: String) {
    }

    override fun remoteControlStreamerWhip(
        url: String,
        method: String,
        headers: List<SettingsHttpHeader>,
        body: ByteArray,
        onCompleted: (Int, List<SettingsHttpHeader>, ByteArray) -> Unit,
    ) {
        onCompleted(404, emptyList(), ByteArray(0))
    }

    override fun remoteControlStreamerSetFilter(filter: RemoteControlFilter, on: Boolean) {
        model?.remoteControlSetFilter(filter = filter, on = on)
    }

    override fun remoteControlStreamerTriggerReaction(reaction: RemoteControlReaction) {
    }

    override fun remoteControlStreamerMoveToGimbalPreset(id: UUID) {
    }

    override fun remoteControlStreamerSetGimbalTracking(on: Boolean) {
        model?.setGimbalTracking(on = on)
    }

    override fun remoteControlStreamerSetGimbalMovement(x: Float, y: Float) {
    }

    override fun remoteControlStreamerAnimateGimbal(motion: SettingsGimbalMotion) {
    }

    override fun remoteControlStreamerSaveGimbalPreset() {
    }

    override fun remoteControlStreamerImportSettings(settings: ByteArray, onCompleted: (Boolean) -> Unit) {
        onCompleted(false)
    }

    override fun remoteControlStreamerStartStats(filter: RemoteControlStartStatsFilter?) {
    }

    override fun remoteControlStreamerStopStats() {
    }

    override fun remoteControlStreamerStartMacro(id: UUID) {
        model?.startMacro(id = id)
    }

    override fun remoteControlStreamerStopMacro(id: UUID) {
        model?.stopMacro(id = id)
    }

    override fun remoteControlStreamerSendMessage(name: String?, text: String) {
        model?.remoteControlSendMessage(name = name, text = text)
    }

    override fun remoteControlStreamerMusic(
        command: RemoteControlMusicCommand,
        onCompleted: (RemoteControlResult, RemoteControlResponse?) -> Unit,
    ) {
        val model = model
        if (model == null) {
            onCompleted(TODO("RemoteControlResult.error"), null)
            return
        }
        model.remoteControlMusic(command = command, onCompleted = onCompleted)
    }

    override fun remoteControlStreamerTip(
        name: String,
        message: String,
        kind: RemoteControlTipKind,
        requestId: String,
    ) {
        model?.moblinCloudTip(
            name = name,
            message = message,
            kind = kind,
            requestId = requestId,
        )
    }
}

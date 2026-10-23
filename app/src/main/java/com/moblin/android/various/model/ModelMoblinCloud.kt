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

private fun makeMoblinCloudUrl(key: String): String? {
    val components = runCatching { URI(moblinWebsiteApiUrl) }.getOrNull() ?: return null
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

fun Model.stopMoblinCloud() {
    moblinCloud?.stop()
    moblinCloud = null
}

fun Model.reloadMoblinCloud() {
    stopMoblinCloud()
    val settings = database.moblinCloud
    val key = settings.key.trim()
    if (!settings.enabled || key.isEmpty()) {
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
    return database.show.moblinCloud && database.moblinCloud.enabled
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

open class MoblinCloud(model: Model, url: String, streamerId: String) : RemoteControlStreamerDelegate {
    private var model: Model? = model
    private var streamer: RemoteControlStreamer? = RemoteControlStreamer(
        clientUrl = url,
        password = "",
        streamerId = streamerId,
        delegate = this,
    )

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

    override fun remoteControlStreamerConnected() {
        model?.makeToast(title = localized("Moblin Cloud connected"))
        model?.updateMoblinCloudStatus()
        model?.moblinCloudSendWheelOfLuck()
    }

    override fun remoteControlStreamerDisconnected() {
        model?.makeToast(title = localized("Moblin Cloud disconnected"))
        model?.updateMoblinCloudStatus()
    }

    override fun remoteControlStreamerWrongPassword() {
    }

    override fun remoteControlStreamerGetStatus(): Triple<RemoteControlStatusGeneral, RemoteControlStatusTopLeft, RemoteControlStatusTopRight> {
        return Triple(
            RemoteControlStatusGeneral(),
            RemoteControlStatusTopLeft(),
            RemoteControlStatusTopRight(),
        )
    }

    override fun remoteControlStreamerGetSettings(): RemoteControlSettings {
        return RemoteControlSettings(
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
    }

    override fun remoteControlStreamerSetScene(id: UUID) {
    }

    override fun remoteControlStreamerSetAutoSceneSwitcher(id: UUID?) {
    }

    override fun remoteControlStreamerSetMic(id: String) {
    }

    override fun remoteControlStreamerSetTalkbackMic(id: String) {
    }

    override fun remoteControlStreamerSetBitratePreset(id: UUID) {
    }

    override fun remoteControlStreamerSetRecord(on: Boolean) {
    }

    override fun remoteControlStreamerSetLive(on: Boolean) {
    }

    override fun remoteControlStreamerSetPreviewStream(on: Boolean) {
    }

    override fun remoteControlStreamerSetDebugLogging(on: Boolean) {
    }

    override fun remoteControlStreamerSetZoom(x: Float) {
    }

    override fun remoteControlStreamerSetZoomPreset(id: UUID) {
    }

    override fun remoteControlStreamerSetMute(on: Boolean) {
    }

    override fun remoteControlStreamerSetStealthMode(on: Boolean) {
    }

    override fun remoteControlStreamerSetTorch(on: Boolean) {
    }

    override fun remoteControlStreamerReloadBrowserWidgets() {
    }

    override fun remoteControlStreamerSetSrtConnectionPriority(id: UUID, priority: Int, enabled: Boolean) {
    }

    override fun remoteControlStreamerSetSrtConnectionPrioritiesEnabled(enabled: Boolean) {
    }

    override fun remoteControlStreamerTwitchEventSubNotification(message: String) {
    }

    override fun remoteControlStreamerChatMessages(history: Boolean, messages: List<RemoteControlChatMessage>) {
    }

    override fun remoteControlStreamerStartPreview() {
    }

    override fun remoteControlStreamerStopPreview() {
    }

    override fun remoteControlStreamerSetRemoteSceneSettings(data: RemoteControlRemoteSceneSettings) {
    }

    override fun remoteControlStreamerSetRemoteSceneData(data: RemoteControlRemoteSceneData) {
    }

    override fun remoteControlStreamerInstantReplay() {
    }

    override fun remoteControlStreamerSaveReplay() {
    }

    override fun remoteControlStreamerStartStatus(interval: Int, filter: RemoteControlStartStatusFilter) {
    }

    override fun remoteControlStreamerStopStatus() {
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
    }

    override fun remoteControlStreamerTriggerReaction(reaction: RemoteControlReaction) {
    }

    override fun remoteControlStreamerMoveToGimbalPreset(id: UUID) {
    }

    override fun remoteControlStreamerSetGimbalTracking(on: Boolean) {
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
    }

    override fun remoteControlStreamerStopMacro(id: UUID) {
    }

    override fun remoteControlStreamerSendMessage(text: String) {
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
        model.remoteControlStreamerMusic(command = command, onCompleted = onCompleted)
    }

    override fun remoteControlStreamerTip(
        name: String,
        message: String,
        kind: RemoteControlTipKind,
        requestId: String,
    ) {
        model?.remoteControlStreamerTip(
            name = name,
            message = message,
            kind = kind,
            requestId = requestId,
        )
    }
}

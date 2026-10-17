package com.moblin.android.various.model

import com.moblin.android.common.various.noValue
import com.moblin.android.localized
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
import com.moblin.android.remotecontrol.RemoteControlWheelOfLuck
import com.moblin.android.remotecontrol.RemoteControlWheelOfLuckOption
import com.moblin.android.various.settings.SettingsGimbalMotion
import com.moblin.android.various.settings.SettingsHttpHeader
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetType
import java.net.URI
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

fun Model.moblinCloudSendSongRequests() {
    moblinCloud?.sendSongRequests(enabled = database.moblinCloud.songRequests)
}

fun Model.moblinCloudSpinWheelOfLuck(name: String, message: String, count: Int, requestId: String?) {
    if (isRemoteControlRequestAlreadyHandled(requestId = requestId)) {
        return
    }
    val widget = getMoblinCloudWheelOfLuckWidget() ?: return
    getWheelOfLuckEffect(id = widget.id)?.spin(
        name = name,
        message = message,
        count = count.coerceIn(1, 10),
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

    open fun sendSongRequests(enabled: Boolean) {
        streamer?.sendSongRequests(enabled = enabled)
    }

    override fun remoteControlStreamerConnected() {
        model?.makeToast(title = localized("Moblin Cloud connected"))
        model?.updateMoblinCloudStatus()
        model?.moblinCloudSendWheelOfLuck()
        model?.moblinCloudSendSongRequests()
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

    override fun remoteControlStreamerSpinWheelOfLuck(name: String, message: String, count: Int, requestId: String?) {
        model?.moblinCloudSpinWheelOfLuck(
            name = name,
            message = message,
            count = count,
            requestId = requestId,
        )
    }

    override fun remoteControlStreamerTip(
        name: String,
        message: String,
        amount: String,
        currency: String,
        requestId: String,
    ) {
        model?.remoteControlStreamerTip(
            name = name,
            message = message,
            amount = amount,
            currency = currency,
            requestId = requestId,
        )
    }
}

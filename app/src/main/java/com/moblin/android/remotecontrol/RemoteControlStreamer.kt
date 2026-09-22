package com.moblin.android.remotecontrol

import android.util.Base64
import android.util.Log
import com.moblin.android.localized
import com.moblin.android.various.MainTimer
import com.moblin.android.various.network.WebSocketClient
import com.moblin.android.various.network.WebSocketClientDelegate
import com.moblin.android.various.settings.SettingsGimbalMotion
import com.moblin.android.various.settings.SettingsHttpHeader
import com.moblin.android.various.storages.SimpleStringStorage
import java.net.URI
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "RemoteControlStreamer"

interface RemoteControlStreamerDelegate {
    fun remoteControlStreamerConnected()
    fun remoteControlStreamerDisconnected()
    fun remoteControlStreamerWrongPassword()
    fun remoteControlStreamerGetStatus(): Triple<RemoteControlStatusGeneral, RemoteControlStatusTopLeft, RemoteControlStatusTopRight>
    fun remoteControlStreamerGetSettings(): RemoteControlSettings
    fun remoteControlStreamerSetStream(id: UUID)
    fun remoteControlStreamerSetScene(id: UUID)
    fun remoteControlStreamerSetAutoSceneSwitcher(id: UUID?)
    fun remoteControlStreamerSetMic(id: String)
    fun remoteControlStreamerSetTalkbackMic(id: String)
    fun remoteControlStreamerSetBitratePreset(id: UUID)
    fun remoteControlStreamerSetRecord(on: Boolean)
    fun remoteControlStreamerSetLive(on: Boolean)
    fun remoteControlStreamerSetPreviewStream(on: Boolean)
    fun remoteControlStreamerSetDebugLogging(on: Boolean)
    fun remoteControlStreamerSetZoom(x: Float)
    fun remoteControlStreamerSetZoomPreset(id: UUID)
    fun remoteControlStreamerSetMute(on: Boolean)
    fun remoteControlStreamerSetStealthMode(on: Boolean)
    fun remoteControlStreamerSetTorch(on: Boolean)
    fun remoteControlStreamerReloadBrowserWidgets()
    fun remoteControlStreamerSetSrtConnectionPriority(id: UUID, priority: Int, enabled: Boolean)
    fun remoteControlStreamerSetSrtConnectionPrioritiesEnabled(enabled: Boolean)
    fun remoteControlStreamerTwitchEventSubNotification(message: String)
    fun remoteControlStreamerChatMessages(history: Boolean, messages: List<RemoteControlChatMessage>)
    fun remoteControlStreamerStartPreview()
    fun remoteControlStreamerStopPreview()
    fun remoteControlStreamerSetRemoteSceneSettings(data: RemoteControlRemoteSceneSettings)
    fun remoteControlStreamerSetRemoteSceneData(data: RemoteControlRemoteSceneData)
    fun remoteControlStreamerInstantReplay()
    fun remoteControlStreamerSaveReplay()
    fun remoteControlStreamerStartStatus(interval: Int, filter: RemoteControlStartStatusFilter)
    fun remoteControlStreamerStopStatus()
    fun remoteControlStreamerGetScoreboardSports(): List<String>
    fun remoteControlStreamerSetScoreboardSport(sportId: String)
    fun remoteControlStreamerUpdateScoreboard(config: RemoteControlScoreboardMatchConfig)
    fun remoteControlStreamerToggleScoreboardClock()
    fun remoteControlStreamerSetScoreboardDuration(minutes: Int)
    fun remoteControlStreamerSetScoreboardClock(time: String)
    fun remoteControlStreamerWhip(
        url: String,
        method: String,
        headers: List<SettingsHttpHeader>,
        body: ByteArray,
        onCompleted: (Int, List<SettingsHttpHeader>, ByteArray) -> Unit,
    )
    fun remoteControlStreamerSetFilter(filter: RemoteControlFilter, on: Boolean)
    fun remoteControlStreamerTriggerReaction(reaction: RemoteControlReaction)
    fun remoteControlStreamerMoveToGimbalPreset(id: UUID)
    fun remoteControlStreamerSetGimbalTracking(on: Boolean)
    fun remoteControlStreamerSetGimbalMovement(x: Float, y: Float)
    fun remoteControlStreamerAnimateGimbal(motion: SettingsGimbalMotion)
    fun remoteControlStreamerSaveGimbalPreset()
    fun remoteControlStreamerImportSettings(settings: ByteArray, onCompleted: (Boolean) -> Unit)
    fun remoteControlStreamerStartStats(filter: RemoteControlStartStatsFilter?)
    fun remoteControlStreamerStopStats()
    fun remoteControlStreamerStartMacro(id: UUID)
    fun remoteControlStreamerStopMacro(id: UUID)
    fun remoteControlStreamerSendMessage(text: String)
}

private val idStorage = SimpleStringStorage("remoteControlStreamerId")

class RemoteControlStreamer(
    private var clientUrl: String,
    private var password: String,
    delegate: RemoteControlStreamerDelegate,
) : WebSocketClientDelegate {
    private var delegate: RemoteControlStreamerDelegate? = delegate
    private var webSocket: WebSocketClient
    var connectionErrorMessage: String = ""
    private var connected = false
    private var encryption: RemoteControlEncryption
    private val keepAliveTimer = MainTimer()
    private var gotPong = true
    private var wrongPassword = false
    private val mainScope = CoroutineScope(Dispatchers.Main)

    init {
        encryption = RemoteControlEncryption(password)
        webSocket = WebSocketClient(clientUrl)
        if (idStorage.get().isEmpty()) {
            idStorage.set(UUID.randomUUID().toString())
        }
    }

    fun start() {
        Log.d(TAG, "remote-control-streamer: start")
        wrongPassword = false
        startInternal()
    }

    fun stop() {
        Log.d(TAG, "remote-control-streamer: stop")
        stopInternal()
    }

    private fun startInternal() {
        stopInternal()
        gotPong = true
        webSocket = WebSocketClient(clientUrl, clientUrl.isLoopback())
        webSocket.delegate = this
        webSocket.start()
    }

    fun stopInternal() {
        webSocket.delegate = null
        webSocket.stop()
        handleDisconnected()
    }

    private fun handleDisconnected() {
        stopKeepAlive()
        if (connected) {
            delegate?.remoteControlStreamerDisconnected()
        }
        connected = false
        if (!wrongPassword) {
            connectionErrorMessage = localized("Disconnected")
        }
    }

    fun isConnected(): Boolean {
        return connected
    }

    fun stateChanged(state: RemoteControlAssistantStreamerState) {
        if (!connected) {
            return
        }
        send(RemoteControlMessageToAssistant.Event(RemoteControlEvent.State(state)))
    }

    fun log(entry: String) {
        if (!connected) {
            return
        }
        send(RemoteControlMessageToAssistant.Event(RemoteControlEvent.Log(entry)))
    }

    fun sendScoreboardUpdate(config: RemoteControlScoreboardMatchConfig) {
        send(RemoteControlMessageToAssistant.Event(RemoteControlEvent.Scoreboard(config)))
    }

    fun sendStats(data: RemoteControlStats) {
        if (!connected) {
            return
        }
        send(RemoteControlMessageToAssistant.Event(RemoteControlEvent.Stats(data)))
    }

    fun sendPreview(preview: ByteArray) {
        send(RemoteControlMessageToAssistant.Preview(preview))
    }

    fun sendStatus(
        general: RemoteControlStatusGeneral?,
        topLeft: RemoteControlStatusTopLeft?,
        topRight: RemoteControlStatusTopRight?,
    ) {
        send(
            RemoteControlMessageToAssistant.Event(
                RemoteControlEvent.Status(general, topLeft, topRight),
            ),
        )
    }

    fun twitchStart(channelName: String?, channelId: String, accessToken: String) {
        if (!connected) {
            return
        }
        val accessToken = encryption.encrypt(accessToken.toByteArray(Charsets.UTF_8))
            ?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
            ?: return
        send(RemoteControlMessageToAssistant.TwitchStart(channelName, channelId, accessToken))
    }

    private fun startKeepAlive() {
        keepAliveTimer.startPeriodic(30.0) {
            if (gotPong) {
                gotPong = false
                send(RemoteControlMessageToAssistant.Ping)
            } else {
                Log.i(TAG, "remote-control-streamer: Pong not received")
                startInternal()
            }
        }
    }

    private fun stopKeepAlive() {
        keepAliveTimer.stop()
    }

    private fun send(message: RemoteControlMessageToAssistant) {
        try {
            val message = message.toJson()
            webSocket.send(message)
        } catch (error: Exception) {
            Log.i(TAG, "remote-control-streamer: Encode failed")
        }
    }

    private fun handleMessage(message: String) {
        try {
            when (val message = RemoteControlMessageToStreamer.fromJson(message)) {
                is RemoteControlMessageToStreamer.Hello ->
                    handleHello(message.apiVersion, message.authentication)
                is RemoteControlMessageToStreamer.Identified -> {
                    if (!handleIdentified(message.result)) {
                        Log.d(TAG, "remote-control-streamer: Failed to identify")
                        return
                    }
                }
                is RemoteControlMessageToStreamer.Request ->
                    handleRequest(message.id, message.data)
                is RemoteControlMessageToStreamer.Pong ->
                    gotPong = true
            }
        } catch (error: Exception) {
            Log.i(TAG, "remote-control-streamer: Decode failed")
            connectionErrorMessage = error.message ?: ""
        }
    }

    private fun handleHello(apiVersion: String, authentication: RemoteControlAuthentication) {
        val hash = remoteControlHashPassword(
            authentication.challenge,
            authentication.salt,
            password,
        )
        send(RemoteControlMessageToAssistant.Identify(idStorage.get(), hash))
    }

    private fun handleIdentified(result: RemoteControlResult): Boolean {
        when (result) {
            RemoteControlResult.OK -> {
                connected = true
                wrongPassword = false
                delegate?.remoteControlStreamerConnected()
                return true
            }
            RemoteControlResult.WRONG_PASSWORD -> {
                connectionErrorMessage = localized("Wrong password")
                if (!wrongPassword) {
                    wrongPassword = true
                    delegate?.remoteControlStreamerWrongPassword()
                }
            }
            else -> {
                connectionErrorMessage = localized("Failed to identify")
            }
        }
        return false
    }

    private fun handleRequest(id: Int, data: RemoteControlRequest) {
        val delegate = this.delegate ?: return
        when (data) {
            is RemoteControlRequest.GetStatus -> {
                val (general, topLeft, topRight) = delegate.remoteControlStreamerGetStatus()
                send(
                    RemoteControlMessageToAssistant.Response(
                        id,
                        RemoteControlResult.OK,
                        RemoteControlResponse.GetStatus(general, topLeft, topRight),
                    ),
                )
            }
            is RemoteControlRequest.GetSettings -> {
                val settings = delegate.remoteControlStreamerGetSettings()
                send(
                    RemoteControlMessageToAssistant.Response(
                        id,
                        RemoteControlResult.OK,
                        RemoteControlResponse.GetSettings(settings),
                    ),
                )
            }
            is RemoteControlRequest.SetStream -> {
                delegate.remoteControlStreamerSetStream(data.id)
                sendEmptyOkResponse(id)
            }
            is RemoteControlRequest.SetScene -> {
                delegate.remoteControlStreamerSetScene(data.id)
                sendEmptyOkResponse(id)
            }
            is RemoteControlRequest.SetAutoSceneSwitcher -> {
                delegate.remoteControlStreamerSetAutoSceneSwitcher(data.id)
                sendEmptyOkResponse(id)
            }
            is RemoteControlRequest.SetMic -> {
                delegate.remoteControlStreamerSetMic(data.id)
                sendEmptyOkResponse(id)
            }
            is RemoteControlRequest.SetTalkbackMic -> {
                delegate.remoteControlStreamerSetTalkbackMic(data.id)
                sendEmptyOkResponse(id)
            }
            is RemoteControlRequest.SetBitratePreset -> {
                delegate.remoteControlStreamerSetBitratePreset(data.id)
                sendEmptyOkResponse(id)
            }
            is RemoteControlRequest.SetRecord -> {
                delegate.remoteControlStreamerSetRecord(data.on)
                sendEmptyOkResponse(id)
            }
            is RemoteControlRequest.SetLive -> {
                delegate.remoteControlStreamerSetLive(data.on)
                sendEmptyOkResponse(id)
            }
            is RemoteControlRequest.SetPreviewStream -> {
                delegate.remoteControlStreamerSetPreviewStream(data.on)
                sendEmptyOkResponse(id)
            }
            is RemoteControlRequest.SetZoom -> {
                delegate.remoteControlStreamerSetZoom(data.x)
                sendEmptyOkResponse(id)
            }
            is RemoteControlRequest.SetZoomPreset -> {
                delegate.remoteControlStreamerSetZoomPreset(data.id)
                sendEmptyOkResponse(id)
            }
            is RemoteControlRequest.SetMute -> {
                delegate.remoteControlStreamerSetMute(data.on)
                sendEmptyOkResponse(id)
            }
            is RemoteControlRequest.SetStealthMode -> {
                delegate.remoteControlStreamerSetStealthMode(data.on)
                sendEmptyOkResponse(id)
            }
            is RemoteControlRequest.SetTorch -> {
                delegate.remoteControlStreamerSetTorch(data.on)
                sendEmptyOkResponse(id)
            }
            is RemoteControlRequest.ReloadBrowserWidgets -> {
                delegate.remoteControlStreamerReloadBrowserWidgets()
                sendEmptyOkResponse(id)
            }
            is RemoteControlRequest.SetSrtConnectionPriority -> {
                delegate.remoteControlStreamerSetSrtConnectionPriority(
                    data.id,
                    data.priority,
                    data.enabled,
                )
                sendEmptyOkResponse(id)
            }
            is RemoteControlRequest.SetSrtConnectionPrioritiesEnabled -> {
                delegate.remoteControlStreamerSetSrtConnectionPrioritiesEnabled(data.enabled)
                sendEmptyOkResponse(id)
            }
            is RemoteControlRequest.TwitchEventSubNotification -> {
                delegate.remoteControlStreamerTwitchEventSubNotification(data.message)
                sendEmptyOkResponse(id)
            }
            is RemoteControlRequest.ChatMessages -> {
                delegate.remoteControlStreamerChatMessages(data.history, data.messages)
                sendEmptyOkResponse(id)
            }
            is RemoteControlRequest.StartPreview -> {
                delegate.remoteControlStreamerStartPreview()
                sendEmptyOkResponse(id)
            }
            is RemoteControlRequest.StopPreview -> {
                delegate.remoteControlStreamerStopPreview()
                sendEmptyOkResponse(id)
            }
            is RemoteControlRequest.SetDebugLogging -> {
                delegate.remoteControlStreamerSetDebugLogging(data.on)
                sendEmptyOkResponse(id)
            }
            is RemoteControlRequest.SetRemoteSceneSettings -> {
                delegate.remoteControlStreamerSetRemoteSceneSettings(data.data)
                sendEmptyOkResponse(id)
            }
            is RemoteControlRequest.SetRemoteSceneData -> {
                delegate.remoteControlStreamerSetRemoteSceneData(data.data)
                sendEmptyOkResponse(id)
            }
            is RemoteControlRequest.InstantReplay -> {
                delegate.remoteControlStreamerInstantReplay()
                sendEmptyOkResponse(id)
            }
            is RemoteControlRequest.SaveReplay -> {
                delegate.remoteControlStreamerSaveReplay()
                sendEmptyOkResponse(id)
            }
            is RemoteControlRequest.StartStatus -> {
                delegate.remoteControlStreamerStartStatus(data.interval, data.filter)
                sendEmptyOkResponse(id)
            }
            is RemoteControlRequest.StopStatus -> {
                delegate.remoteControlStreamerStopStatus()
                sendEmptyOkResponse(id)
            }
            is RemoteControlRequest.GetScoreboardSports -> {
                val sports = delegate.remoteControlStreamerGetScoreboardSports()
                send(
                    RemoteControlMessageToAssistant.Response(
                        id,
                        RemoteControlResult.OK,
                        RemoteControlResponse.GetScoreboardSports(sports),
                    ),
                )
            }
            is RemoteControlRequest.SetScoreboardSport -> {
                delegate.remoteControlStreamerSetScoreboardSport(data.sportId)
                sendEmptyOkResponse(id)
            }
            is RemoteControlRequest.UpdateScoreboard -> {
                delegate.remoteControlStreamerUpdateScoreboard(data.config)
                sendEmptyOkResponse(id)
            }
            is RemoteControlRequest.ToggleScoreboardClock -> {
                delegate.remoteControlStreamerToggleScoreboardClock()
                sendEmptyOkResponse(id)
            }
            is RemoteControlRequest.SetScoreboardDuration -> {
                delegate.remoteControlStreamerSetScoreboardDuration(data.minutes)
                sendEmptyOkResponse(id)
            }
            is RemoteControlRequest.SetScoreboardClock -> {
                delegate.remoteControlStreamerSetScoreboardClock(data.time)
                sendEmptyOkResponse(id)
            }
            is RemoteControlRequest.Whip -> {
                delegate.remoteControlStreamerWhip(
                    data.url,
                    data.method,
                    data.headers,
                    data.body,
                ) { status, headers, body ->
                    send(
                        RemoteControlMessageToAssistant.Response(
                            id,
                            RemoteControlResult.OK,
                            RemoteControlResponse.Whip(status, headers, body),
                        ),
                    )
                }
            }
            is RemoteControlRequest.SetFilter -> {
                delegate.remoteControlStreamerSetFilter(data.filter, data.on)
                sendEmptyOkResponse(id)
            }
            is RemoteControlRequest.TriggerReaction -> {
                delegate.remoteControlStreamerTriggerReaction(data.reaction)
                sendEmptyOkResponse(id)
            }
            is RemoteControlRequest.MoveToGimbalPreset -> {
                delegate.remoteControlStreamerMoveToGimbalPreset(data.id)
                sendEmptyOkResponse(id)
            }
            is RemoteControlRequest.SetGimbalTracking -> {
                delegate.remoteControlStreamerSetGimbalTracking(data.on)
                sendEmptyOkResponse(id)
            }
            is RemoteControlRequest.SetGimbalMovement -> {
                delegate.remoteControlStreamerSetGimbalMovement(data.x, data.y)
                sendEmptyOkResponse(id)
            }
            is RemoteControlRequest.AnimateGimbal -> {
                delegate.remoteControlStreamerAnimateGimbal(data.motion)
                sendEmptyOkResponse(id)
            }
            is RemoteControlRequest.SaveGimbalPreset -> {
                delegate.remoteControlStreamerSaveGimbalPreset()
                sendEmptyOkResponse(id)
            }
            is RemoteControlRequest.GetGolfScoreboard -> {
                sendEmptyOkResponse(id)
            }
            is RemoteControlRequest.UpdateGolfScoreboard -> {
                sendEmptyOkResponse(id)
            }
            is RemoteControlRequest.ImportSettings -> {
                delegate.remoteControlStreamerImportSettings(data.data) { succeeded ->
                    send(
                        RemoteControlMessageToAssistant.Response(
                            id,
                            if (succeeded) RemoteControlResult.OK else RemoteControlResult.ERROR,
                            null,
                        ),
                    )
                }
            }
            is RemoteControlRequest.StartStats -> {
                delegate.remoteControlStreamerStartStats(data.filter)
                sendEmptyOkResponse(id)
            }
            is RemoteControlRequest.StopStats -> {
                delegate.remoteControlStreamerStopStats()
                sendEmptyOkResponse(id)
            }
            is RemoteControlRequest.StartMacro -> {
                delegate.remoteControlStreamerStartMacro(data.id)
                sendEmptyOkResponse(id)
            }
            is RemoteControlRequest.StopMacro -> {
                delegate.remoteControlStreamerStopMacro(data.id)
                sendEmptyOkResponse(id)
            }
            is RemoteControlRequest.SendMessage -> {
                delegate.remoteControlStreamerSendMessage(data.text)
                sendEmptyOkResponse(id)
            }
        }
    }

    private fun sendEmptyOkResponse(id: Int) {
        send(RemoteControlMessageToAssistant.Response(id, RemoteControlResult.OK, null))
    }

    override fun webSocketClientConnected(client: WebSocketClient) {
        mainScope.launch {
            Log.i(TAG, "remote-control-streamer: Connected")
            startKeepAlive()
        }
    }

    override fun webSocketClientDisconnected(client: WebSocketClient) {
        mainScope.launch {
            Log.i(TAG, "remote-control-streamer: Disconnected")
            handleDisconnected()
        }
    }

    override fun webSocketClientReceiveMessage(client: WebSocketClient, string: String) {
        mainScope.launch {
            runCatching { handleMessage(string) }
        }
    }
}

private fun String.isLoopback(): Boolean {
    val host = runCatching { URI(this).host }.getOrNull() ?: return false
    return host == "127.0.0.1" || host == "localhost" || host == "::1"
}

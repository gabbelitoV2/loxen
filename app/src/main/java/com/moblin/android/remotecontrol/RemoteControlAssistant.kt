package com.moblin.android.remotecontrol

import com.moblin.android.platform.log.Log
import com.moblin.android.common.various.RgbColor
import com.moblin.android.common.various.digitalClockFormatter
import com.moblin.android.common.various.formatDate
import com.moblin.android.streamingplatforms.Platform
import com.moblin.android.streamingplatforms.twitch.TwitchChat
import com.moblin.android.streamingplatforms.twitch.TwitchChatDelegate
import com.moblin.android.streamingplatforms.twitch.TwitchEventSub
import com.moblin.android.streamingplatforms.twitch.TwitchEventSubChannelAdBreakBeginEvent
import com.moblin.android.streamingplatforms.twitch.TwitchEventSubChannelCheerEvent
import com.moblin.android.streamingplatforms.twitch.TwitchEventSubChannelHypeTrainBeginEvent
import com.moblin.android.streamingplatforms.twitch.TwitchEventSubChannelHypeTrainEndEvent
import com.moblin.android.streamingplatforms.twitch.TwitchEventSubChannelHypeTrainProgressEvent
import com.moblin.android.streamingplatforms.twitch.TwitchEventSubChannelModerateEvent
import com.moblin.android.streamingplatforms.twitch.TwitchEventSubChannelPollEvent
import com.moblin.android.streamingplatforms.twitch.TwitchEventSubChannelPredictionEvent
import com.moblin.android.streamingplatforms.twitch.TwitchEventSubChannelRaidEvent
import com.moblin.android.streamingplatforms.twitch.TwitchEventSubChannelShoutoutCreateEvent
import com.moblin.android.streamingplatforms.twitch.TwitchEventSubDelegate
import com.moblin.android.streamingplatforms.twitch.TwitchEventSubNotificationChannelFollowEvent
import com.moblin.android.streamingplatforms.twitch.TwitchEventSubNotificationChannelPointsCustomRewardRedemptionAddEvent
import com.moblin.android.streamingplatforms.twitch.TwitchEventSubNotificationChannelSubscribeEvent
import com.moblin.android.streamingplatforms.twitch.TwitchEventSubNotificationChannelSubscriptionGiftEvent
import com.moblin.android.streamingplatforms.twitch.TwitchEventSubNotificationChannelSubscriptionMessageEvent
import com.moblin.android.streamingplatforms.twitch.TwitchEventSubNotificationChannelSubscriptionUpgradeEvent
import com.moblin.android.streamingplatforms.twitch.TwitchEventSubNotificationChannelWatchStreakEvent
import com.moblin.android.various.ChatHighlight
import com.moblin.android.various.ChatPostSegment
import com.moblin.android.various.MainTimer
import com.moblin.android.various.settings.SettingsHttpHeader
import com.moblin.android.various.settings.SettingsStreamChat
import com.moblin.android.various.utils.randomString
import java.io.BufferedInputStream
import java.io.InputStream
import java.io.OutputStream
import java.lang.ref.WeakReference
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import java.time.Instant
import java.time.ZonedDateTime
import java.util.ArrayDeque
import java.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import com.moblin.android.AppDelegate

private const val assistantLogTag = "RemoteControlAssistant"

interface RemoteControlAssistantDelegate {
    fun remoteControlAssistantConnected()
    fun remoteControlAssistantDisconnected()
    fun remoteControlAssistantPreview(preview: ByteArray)
    fun remoteControlAssistantStateChanged(state: RemoteControlAssistantStreamerState)
    fun remoteControlAssistantLog(entry: String)
    fun remoteControlAssistantStatus(
        general: RemoteControlStatusGeneral?,
        topLeft: RemoteControlStatusTopLeft?,
        topRight: RemoteControlStatusTopRight?
    )

    fun remoteControlAssistantStats(data: RemoteControlStats)
}

private data class RemoteControlRequestResponse(
    val onSuccess: (RemoteControlResponse?) -> Unit,
    val onError: (String) -> Unit
)

class RemoteControlAssistant(
    private val port: Int,
    private val password: String,
    delegate: RemoteControlAssistantDelegate
) : TwitchEventSubDelegate, TwitchChatDelegate {
    private var connected: Boolean = false
    private var nextId: Int = 0
    private val requests: MutableMap<Int, RemoteControlRequestResponse> = mutableMapOf()
    private var server: ServerSocket? = null
    var connectionErrorMessage = ""
    private var streamerWebSocket: RemoteControlWebSocket? = null
    private val retryStartTimer = MainTimer()
    private val delegateReference = WeakReference(delegate)
    private val delegate: RemoteControlAssistantDelegate? get() = delegateReference.get()
    private var streamerIdentified = false
    private var challenge = ""
    private var salt = ""
    private var encryption: RemoteControlEncryption
    private var twitchEventSub: TwitchEventSub? = null
    private var twitchChat: TwitchChat? = null
    private var twitchChannelName: String? = null
    private var twitchChannelId: String? = null
    private var twitchAccessToken: String? = null
    private var twitchEventSubNotitications: MutableList<String> = mutableListOf()
    private var twitchEventSubNotiticationWaitForResponse = false
    private val chatMessageHistory: ArrayDeque<RemoteControlChatMessage> = ArrayDeque()
    private var nextChatMessageId = 0
    private val keepAliveTimer = MainTimer()
    private var gotPing = false
    private var pingTimer = MainTimer()
    private var pongReceived = true

    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        encryption = RemoteControlEncryption(password)
    }

    fun start() {
        stop()
        Log.d(assistantLogTag, "remote-control-assistant: start")
        startInternal()
    }

    fun stop() {
        Log.d(assistantLogTag, "remote-control-assistant: stop")
        runCatching { server?.close() }
        server = null
        streamerWebSocket?.cancel()
        streamerWebSocket = null
        connected = false
        requests.clear()
        stopRetryStartTimer()
        twitchEventSub?.stop()
        twitchChat?.stop()
        stopKeepAlive()
        stopPingTimer()
    }

    fun isConnected(): Boolean {
        return connected
    }

    fun getStatus(
        onSuccess: (RemoteControlStatusGeneral?, RemoteControlStatusTopLeft, RemoteControlStatusTopRight) -> Unit
    ) {
        performRequest(
            data = RemoteControlRequest.GetStatus,
            onSuccess = { response ->
                if (response != null) {
                    when (response) {
                        is RemoteControlResponse.GetStatus -> onSuccess(
                            response.general,
                            response.topLeft,
                            response.topRight
                        )

                        else -> Log.i(assistantLogTag, "remote-control-assistant: Wrong response to getStatus")
                    }
                }
            },
            onError = { error ->
                Log.i(assistantLogTag, "remote-control-assistant: Get status failed with $error")
            }
        )
    }

    fun getSettings(onSuccess: (RemoteControlSettings) -> Unit) {
        performRequest(
            data = RemoteControlRequest.GetSettings,
            onSuccess = { response ->
                if (response != null) {
                    when (response) {
                        is RemoteControlResponse.GetSettings -> onSuccess(response.data)
                        else -> Log.i(assistantLogTag, "remote-control-assistant: Wrong response to getSettings")
                    }
                }
            },
            onError = { error ->
                Log.i(assistantLogTag, "remote-control-assistant: Get settings failed with $error")
            }
        )
    }

    fun setRecord(on: Boolean, onSuccess: () -> Unit) {
        performRequestNoResponseData(RemoteControlRequest.SetRecord(on = on), onSuccess)
    }

    fun setLive(on: Boolean, onSuccess: () -> Unit) {
        performRequestNoResponseData(RemoteControlRequest.SetLive(on = on), onSuccess)
    }

    fun setPreviewStream(on: Boolean, onSuccess: () -> Unit) {
        performRequestNoResponseData(RemoteControlRequest.SetPreviewStream(on = on), onSuccess)
    }

    fun setZoom(x: Float, onSuccess: () -> Unit) {
        performRequestNoResponseData(RemoteControlRequest.SetZoom(x = x), onSuccess)
    }

    fun setZoomPreset(id: java.util.UUID, onSuccess: () -> Unit) {
        performRequestNoResponseData(RemoteControlRequest.SetZoomPreset(id = id), onSuccess)
    }

    fun setMute(on: Boolean, onSuccess: () -> Unit) {
        performRequestNoResponseData(RemoteControlRequest.SetMute(on = on), onSuccess)
    }

    fun setStealthMode(on: Boolean, onSuccess: () -> Unit) {
        performRequestNoResponseData(RemoteControlRequest.SetStealthMode(on = on), onSuccess)
    }

    fun setTorch(on: Boolean, onSuccess: () -> Unit) {
        performRequestNoResponseData(RemoteControlRequest.SetTorch(on = on), onSuccess)
    }

    fun setScene(id: java.util.UUID, onSuccess: () -> Unit) {
        performRequestNoResponseData(RemoteControlRequest.SetScene(id = id), onSuccess)
    }

    fun setAutoSceneSwitcher(id: java.util.UUID?, onSuccess: () -> Unit) {
        performRequestNoResponseData(RemoteControlRequest.SetAutoSceneSwitcher(id = id), onSuccess)
    }

    fun setMic(id: String, onSuccess: () -> Unit) {
        performRequestNoResponseData(RemoteControlRequest.SetMic(id = id), onSuccess)
    }

    fun setBitratePreset(id: java.util.UUID, onSuccess: () -> Unit) {
        performRequestNoResponseData(RemoteControlRequest.SetBitratePreset(id = id), onSuccess)
    }

    fun setDebugLogging(on: Boolean, onSuccess: () -> Unit) {
        performRequestNoResponseData(RemoteControlRequest.SetDebugLogging(on = on), onSuccess)
    }

    fun moveToGimbalPreset(id: java.util.UUID, onSuccess: () -> Unit) {
        performRequestNoResponseData(RemoteControlRequest.MoveToGimbalPreset(id = id), onSuccess)
    }

    fun startMacro(id: java.util.UUID, onSuccess: () -> Unit) {
        performRequestNoResponseData(RemoteControlRequest.StartMacro(id = id), onSuccess)
    }

    fun stopMacro(id: java.util.UUID, onSuccess: () -> Unit) {
        performRequestNoResponseData(RemoteControlRequest.StopMacro(id = id), onSuccess)
    }

    fun setRemoteSceneSettings(data: RemoteControlRemoteSceneSettings, onSuccess: () -> Unit) {
        performRequestNoResponseData(RemoteControlRequest.SetRemoteSceneSettings(data = data), onSuccess)
    }

    fun setRemoteSceneData(data: RemoteControlRemoteSceneData, onSuccess: () -> Unit) {
        performRequestNoResponseData(RemoteControlRequest.SetRemoteSceneData(data = data), onSuccess)
    }

    fun importSettings(data: ByteArray, onSuccess: () -> Unit, onError: (String) -> Unit) {
        performRequest(
            data = RemoteControlRequest.ImportSettings(data = data),
            onSuccess = { _ -> onSuccess() },
            onError = { error -> onError(error) }
        )
    }

    fun reloadBrowserWidgets(onSuccess: () -> Unit) {
        performRequestNoResponseData(RemoteControlRequest.ReloadBrowserWidgets, onSuccess)
    }

    fun setSrtConnectionPrioritiesEnabled(enabled: Boolean, onSuccess: () -> Unit) {
        performRequestNoResponseData(
            RemoteControlRequest.SetSrtConnectionPrioritiesEnabled(enabled = enabled),
            onSuccess
        )
    }

    fun setSrtConnectionPriority(id: java.util.UUID, priority: Int, enabled: Boolean, onSuccess: () -> Unit) {
        performRequestNoResponseData(
            RemoteControlRequest.SetSrtConnectionPriority(id = id, priority = priority, enabled = enabled),
            onSuccess
        )
    }

    fun startPreview() {
        performRequestNoResponseData(RemoteControlRequest.StartPreview, {})
    }

    fun stopPreview() {
        performRequestNoResponseData(RemoteControlRequest.StopPreview, {})
    }

    fun startStatus() {
        val filter = RemoteControlStartStatusFilter()
        performRequestNoResponseData(RemoteControlRequest.StartStatus(interval = 1, filter = filter), {})
    }

    fun stopStatus() {
        performRequestNoResponseData(RemoteControlRequest.StopStatus, {})
    }

    fun startStats(filter: RemoteControlStartStatsFilter? = null) {
        performRequestNoResponseData(RemoteControlRequest.StartStats(filter = filter), {})
    }

    fun stopStats() {
        performRequestNoResponseData(RemoteControlRequest.StopStats, {})
    }

    fun whipPerform(
        url: String,
        method: String,
        headers: List<SettingsHttpHeader>,
        body: ByteArray,
        completion: ((ByteArray?, Response?, Throwable?) -> Unit)?
    ) {
        performRequest(
            data = RemoteControlRequest.Whip(url = url, method = method, headers = headers, body = body),
            onSuccess = { response ->
                when (response) {
                    is RemoteControlResponse.Whip -> {
                        val headerBuilder = Headers.Builder()
                        for (header in response.headers) {
                            headerBuilder.add(header.name, header.value)
                        }
                        val httpResponse = Response.Builder()
                            .request(Request.Builder().url(url).build())
                            .protocol(Protocol.HTTP_1_1)
                            .code(response.status)
                            .message("")
                            .headers(headerBuilder.build())
                            .body(response.body.toResponseBody(null))
                            .build()
                        completion?.invoke(response.body, httpResponse, null)
                    }

                    else -> completion?.invoke(null, null, Exception(""))
                }
            },
            onError = { _ -> completion?.invoke(null, null, Exception("")) }
        )
    }

    fun setFilter(filter: RemoteControlFilter, on: Boolean) {
        performRequestNoResponseData(RemoteControlRequest.SetFilter(filter = filter, on = on), {})
    }

    fun sendMessage(text: String) {
        performRequestNoResponseData(RemoteControlRequest.SendMessage(text = text), {})
    }

    private fun tryNextTwitchEventSubNotification() {
        if (twitchEventSubNotiticationWaitForResponse) {
            return
        }
        val message = twitchEventSubNotitications.firstOrNull()
        if (message == null) {
            return
        }
        twitchEventSubNotiticationWaitForResponse = true
        performRequestNoResponseData(
            RemoteControlRequest.TwitchEventSubNotification(message = message),
            {
                twitchEventSubNotitications.removeAt(0)
                twitchEventSubNotiticationWaitForResponse = false
                tryNextTwitchEventSubNotification()
            }
        )
    }

    private fun getNextChatMessageId(): Int {
        nextChatMessageId += 1
        return nextChatMessageId
    }

    private fun sendChatMessage(message: RemoteControlChatMessage) {
        performRequestNoResponseData(
            RemoteControlRequest.ChatMessages(history = false, messages = listOf(message)),
            {}
        )
    }

    private fun sendChatMessageHistory() {
        performRequestNoResponseData(
            RemoteControlRequest.ChatMessages(history = true, messages = chatMessageHistory.toList()),
            {}
        )
    }

    private fun startInternal() {
        try {
            val serverSocket = ServerSocket()
            serverSocket.reuseAddress = true
            serverSocket.bind(InetSocketAddress(port))
            server = serverSocket
            ioScope.launch { acceptLoop(serverSocket) }
        } catch (e: Exception) {
            connectionErrorMessage = e.localizedMessage ?: ""
        }
        startRetryStartTimer()
    }

    private suspend fun acceptLoop(serverSocket: ServerSocket) {
        withContext(Dispatchers.IO) {
            var running = true
            while (running) {
                val socket = runCatching { serverSocket.accept() }.getOrNull()
                if (socket == null) {
                    running = false
                } else {
                    val webSocket = runCatching { handshake(socket) }.getOrNull()
                    if (webSocket != null) {
                        mainScope.launch { handleNewConnection(webSocket) }
                    } else {
                        runCatching { socket.close() }
                    }
                }
            }
        }
    }

    private fun handshake(socket: Socket): RemoteControlWebSocket? {
        val input = BufferedInputStream(socket.getInputStream())
        var key: String? = null
        var inHeaders = true
        while (inHeaders) {
            val line = readHttpLine(input)
            if (line == null || line.isEmpty()) {
                inHeaders = false
            } else {
                val index = line.indexOf(':')
                if (index > 0) {
                    val name = line.substring(0, index).trim()
                    val value = line.substring(index + 1).trim()
                    if (name.equals("Sec-WebSocket-Key", ignoreCase = true)) {
                        key = value
                    }
                }
            }
        }
        val acceptKey = key?.let { secWebSocketAccept(it) } ?: return null
        val output = socket.getOutputStream()
        val response = "HTTP/1.1 101 Switching Protocols\r\n" +
            "Upgrade: websocket\r\n" +
            "Connection: Upgrade\r\n" +
            "Sec-WebSocket-Accept: $acceptKey\r\n" +
            "\r\n"
        output.write(response.toByteArray(Charsets.US_ASCII))
        output.flush()
        return RemoteControlWebSocket(socket)
    }

    private fun readHttpLine(input: InputStream): String? {
        val line = StringBuilder()
        var done = false
        while (!done) {
            val value = input.read()
            if (value < 0) {
                return null
            } else if (value == '\n'.code) {
                done = true
            } else if (value != '\r'.code) {
                line.append(value.toChar())
            }
        }
        return line.toString()
    }

    private fun secWebSocketAccept(key: String): String {
        val digest = MessageDigest.getInstance("SHA-1")
        val bytes = digest.digest(
            (key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").toByteArray(Charsets.US_ASCII)
        )
        return Base64.getEncoder().encodeToString(bytes)
    }

    private fun startRetryStartTimer() {
        retryStartTimer.startSingleShot(timeout = 5.0) {
            startInternal()
        }
    }

    private fun stopRetryStartTimer() {
        retryStartTimer.stop()
    }

    private fun startPingTimer() {
        pongReceived = true
        pingTimer.startPeriodic(interval = 30.0, initial = 0.0) {
            if (pongReceived) {
                pongReceived = false
                streamerWebSocket?.sendPing()
            } else {
                Log.i(assistantLogTag, "remote-control-assistant: Ping timeout")
                closeStreamer()
            }
        }
    }

    private fun stopPingTimer() {
        pingTimer.stop()
    }

    private fun startKeepAlive() {
        gotPing = false
        keepAliveTimer.startPeriodic(interval = 60.0) {
            if (!gotPing) {
                Log.i(assistantLogTag, "remote-control-assistant: Ping not received")
                closeStreamer()
            }
            gotPing = false
        }
    }

    private fun stopKeepAlive() {
        keepAliveTimer.stop()
    }

    private fun closeStreamer() {
        streamerWebSocket?.cancel()
        streamerWebSocket = null
    }

    private fun handleNewConnection(webSocket: RemoteControlWebSocket) {
        Log.d(assistantLogTag, "remote-control-assistant: Streamer connected")
        streamerWebSocket?.cancel()
        streamerWebSocket = webSocket
        webSocket.onText = { text ->
            mainScope.launch {
                handleStringMessage(webSocket = webSocket, message = text)
            }
        }
        webSocket.onPing = {}
        webSocket.onPong = {
            mainScope.launch {
                pongReceived = true
            }
        }
        webSocket.onClosed = {
            mainScope.launch {
                handleDisconnected(webSocket = webSocket)
            }
        }
        ioScope.launch { webSocket.readLoop() }
        challenge = randomString()
        salt = randomString()
        send(
            RemoteControlMessageToStreamer.Hello(
                apiVersion = remoteControlApiVersion,
                authentication = RemoteControlAuthentication(challenge = challenge, salt = salt)
            )
        )
        streamerIdentified = false
        startKeepAlive()
        startPingTimer()
    }

    private fun handleDisconnected(webSocket: RemoteControlWebSocket) {
        if (streamerWebSocket != null && streamerWebSocket !== webSocket) {
            return
        }
        Log.d(assistantLogTag, "remote-control-assistant: Streamer disconnected")
        stopKeepAlive()
        stopPingTimer()
        streamerWebSocket?.cancel()
        streamerWebSocket = null
        connected = false
        requests.clear()
        delegate?.remoteControlAssistantDisconnected()
    }

    private fun handleStringMessage(webSocket: RemoteControlWebSocket, message: String) {
        try {
            val parsed = RemoteControlMessageToAssistant.fromJson(message)
            when (parsed) {
                is RemoteControlMessageToAssistant.Identify -> handleIdentify(parsed.authentication)
                is RemoteControlMessageToAssistant.Event -> handleEvent(parsed.data)
                is RemoteControlMessageToAssistant.Response -> handleResponse(
                    id = parsed.id,
                    result = parsed.result,
                    data = parsed.data
                )

                is RemoteControlMessageToAssistant.Preview -> handlePreview(parsed.preview)
                is RemoteControlMessageToAssistant.TwitchStart -> handleTwitchStart(
                    channelName = parsed.channelName,
                    channelId = parsed.channelId,
                    accessToken = parsed.accessToken
                )

                is RemoteControlMessageToAssistant.Ping -> handlePing()
            }
        } catch (e: Exception) {
            Log.d(assistantLogTag, "remote-control-assistant: Failed to process message with error $e")
        }
    }

    private fun handleIdentify(authentication: String) {
        if (authentication == remoteControlHashPassword(
                challenge = challenge,
                salt = salt,
                password = password
            )
        ) {
            streamerIdentified = true
            connected = true
            send(RemoteControlMessageToStreamer.Identified(result = RemoteControlResult.Ok))
            delegate?.remoteControlAssistantConnected()
            twitchEventSubNotiticationWaitForResponse = false
            tryNextTwitchEventSubNotification()
        } else {
            Log.i(assistantLogTag, "remote-control-assistant: Streamer sent wrong password")
            sendAndClose(RemoteControlMessageToStreamer.Identified(result = RemoteControlResult.WrongPassword))
        }
    }

    private fun handleEvent(data: RemoteControlEvent) {
        if (!streamerIdentified) {
            throw Exception("Streamer not identified")
        }
        when (data) {
            is RemoteControlEvent.State -> handleStateEvent(state = data.data)
            is RemoteControlEvent.Log -> handleLogEvent(entry = data.entry)
            is RemoteControlEvent.Status -> handleStatusEvent(
                general = data.general,
                topLeft = data.topLeft,
                topRight = data.topRight
            )

            is RemoteControlEvent.Scoreboard -> {}
            is RemoteControlEvent.GolfScoreboard -> {}
            is RemoteControlEvent.Stats -> delegate?.remoteControlAssistantStats(data = data.data)
        }
    }

    private fun handleResponse(id: Int, result: RemoteControlResult, data: RemoteControlResponse?) {
        if (!streamerIdentified) {
            throw Exception("Streamer not identified")
        }
        val request = requests.remove(id)
        if (request == null) {
            Log.d(assistantLogTag, "remote-control-assistant: Unexpected id in response")
            return
        }
        when (result) {
            RemoteControlResult.Ok -> request.onSuccess(data)
            RemoteControlResult.WrongPassword -> request.onError("Wrong password")
            RemoteControlResult.NotIdentified -> Log.i(assistantLogTag, "remote-control-assistant: Not identified")
            RemoteControlResult.AlreadyIdentified -> Log.i(
                assistantLogTag,
                "remote-control-assistant: Already identified"
            )

            RemoteControlResult.UnknownRequest -> Log.i(assistantLogTag, "remote-control-assistant: Unknown request")
            RemoteControlResult.Error -> request.onError("")
        }
    }

    private fun handlePreview(preview: ByteArray) {
        if (!streamerIdentified) {
            throw Exception("Streamer not identified")
        }
        delegate?.remoteControlAssistantPreview(preview = preview)
    }

    private fun handleTwitchStart(channelName: String?, channelId: String, accessToken: String) {
        if (!streamerIdentified) {
            throw Exception("Streamer not identified")
        }
        val tokenData = runCatching { Base64.getDecoder().decode(accessToken) }.getOrNull()
            ?: throw Exception("Access token not base64")
        val decrypted = encryption.decrypt(tokenData)
            ?: throw Exception("Access token decryption failed")
        val plainAccessToken = decrypted.toString(Charsets.UTF_8)
        if (channelName != null) {
            sendChatMessageHistory()
        }
        if (channelName == twitchChannelName
            && channelId == twitchChannelId
            && plainAccessToken == twitchAccessToken
            && twitchEventSub?.isConnected() != false
        ) {
            return
        }
        twitchChannelName = channelName
        twitchChannelId = channelId
        twitchAccessToken = plainAccessToken
        twitchEventSub?.stop()
        twitchEventSub = TwitchEventSub(
            context = AppDelegate.context,
            remoteControl = false,
            userId = channelId,
            accessToken = plainAccessToken,
            delegate = this
        )
        twitchEventSub?.start()
        twitchChat?.stop()
        if (channelName != null) {
            twitchChat = TwitchChat(delegate = this)
            twitchChat?.start(
                channelName = channelName,
                channelId = channelId,
                settings = SettingsStreamChat(),
                accessToken = plainAccessToken
            )
        }
    }

    private fun handlePing() {
        send(RemoteControlMessageToStreamer.Pong)
        gotPing = true
    }

    private fun handleStateEvent(state: RemoteControlAssistantStreamerState) {
        delegate?.remoteControlAssistantStateChanged(state = state)
    }

    private fun handleLogEvent(entry: String) {
        delegate?.remoteControlAssistantLog(entry = entry)
    }

    private fun handleStatusEvent(
        general: RemoteControlStatusGeneral?,
        topLeft: RemoteControlStatusTopLeft?,
        topRight: RemoteControlStatusTopRight?
    ) {
        delegate?.remoteControlAssistantStatus(general = general, topLeft = topLeft, topRight = topRight)
    }

    private fun performRequest(
        data: RemoteControlRequest,
        onSuccess: (RemoteControlResponse?) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!connected) {
            onError("Not connected to streamer")
            return
        }
        val id = getNextId()
        requests[id] = RemoteControlRequestResponse(onSuccess = onSuccess, onError = onError)
        send(RemoteControlMessageToStreamer.Request(id = id, data = data))
    }

    private fun performRequestNoResponseData(data: RemoteControlRequest, onSuccess: () -> Unit) {
        performRequest(data = data, onSuccess = { _ -> onSuccess() }, onError = { _ -> })
    }

    private fun getNextId(): Int {
        nextId += 1
        return nextId
    }

    private fun send(message: RemoteControlMessageToStreamer) {
        val text = message.toJson() ?: return
        streamerWebSocket?.sendText(text)
    }

    private fun sendAndClose(message: RemoteControlMessageToStreamer) {
        val text = message.toJson() ?: return
        val webSocket = streamerWebSocket ?: return
        webSocket.sendText(text)
        webSocket.cancel()
        streamerWebSocket = null
    }

    override fun twitchEventSubChannelAdBreakBegin(event: TwitchEventSubChannelAdBreakBeginEvent) {}

    override fun twitchEventSubChannelFollow(event: TwitchEventSubNotificationChannelFollowEvent) {}

    override fun twitchEventSubChannelSubscribe(event: TwitchEventSubNotificationChannelSubscribeEvent) {}

    override fun twitchEventSubChannelSubscriptionUpgrade(
        event: TwitchEventSubNotificationChannelSubscriptionUpgradeEvent
    ) {
    }

    override fun twitchEventSubChannelWatchStreak(event: TwitchEventSubNotificationChannelWatchStreakEvent) {}

    override fun twitchEventSubChannelSubscriptionGift(
        event: TwitchEventSubNotificationChannelSubscriptionGiftEvent
    ) {
    }

    override fun twitchEventSubChannelSubscriptionMessage(
        event: TwitchEventSubNotificationChannelSubscriptionMessageEvent
    ) {
    }

    override fun twitchEventSubChannelPointsCustomRewardRedemptionAdd(
        event: TwitchEventSubNotificationChannelPointsCustomRewardRedemptionAddEvent
    ) {
    }

    override fun twitchEventSubChannelRaid(event: TwitchEventSubChannelRaidEvent) {}

    override fun twitchEventSubChannelCheer(event: TwitchEventSubChannelCheerEvent) {}

    override fun twitchEventSubChannelHypeTrainBegin(event: TwitchEventSubChannelHypeTrainBeginEvent) {}

    override fun twitchEventSubChannelHypeTrainProgress(event: TwitchEventSubChannelHypeTrainProgressEvent) {}

    override fun twitchEventSubChannelHypeTrainEnd(event: TwitchEventSubChannelHypeTrainEndEvent) {}

    override fun twitchEventSubChannelModerate(event: TwitchEventSubChannelModerateEvent) {}

    override fun twitchEventSubChannelShoutoutCreate(event: TwitchEventSubChannelShoutoutCreateEvent) {}

    override fun twitchEventSubChannelPollBegin(event: TwitchEventSubChannelPollEvent) {}

    override fun twitchEventSubChannelPollProgress(event: TwitchEventSubChannelPollEvent) {}

    override fun twitchEventSubChannelPollEnd(event: TwitchEventSubChannelPollEvent) {}

    override fun twitchEventSubChannelPredictionBegin(event: TwitchEventSubChannelPredictionEvent) {}

    override fun twitchEventSubChannelPredictionProgress(event: TwitchEventSubChannelPredictionEvent) {}

    override fun twitchEventSubChannelPredictionLock(event: TwitchEventSubChannelPredictionEvent) {}

    override fun twitchEventSubChannelPredictionEnd(event: TwitchEventSubChannelPredictionEvent) {}

    override fun twitchEventSubUnauthorized() {
        Log.i(assistantLogTag, "remote-control-assistant: twitch-event-sub: Twitch not authorized")
        twitchEventSub?.stop()
        twitchEventSub = null
    }

    override fun twitchEventSubNotification(message: String) {
        twitchEventSubNotitications.add(message)
        tryNextTwitchEventSubNotification()
    }

    override fun twitchChatMakeErrorToast(title: String, subTitle: String?) {}

    override fun twitchChatAppendMessage(
        messageId: String?,
        displayName: String,
        user: String,
        userId: String?,
        userColor: RgbColor?,
        userBadges: List<String>,
        segments: List<ChatPostSegment>,
        isAction: Boolean,
        isSubscriber: Boolean,
        isModerator: Boolean,
        bits: String?,
        highlight: ChatHighlight?,
        sourceChannelIcon: String?
    ) {
        val timestamp = formatDate(Instant.now())
        val message = RemoteControlChatMessage(
            id = getNextChatMessageId(),
            platform = Platform.twitch,
            messageId = messageId,
            displayName = displayName,
            user = user,
            userId = userId,
            userColor = userColor,
            userBadges = userBadges,
            segments = segments,
            timestamp = timestamp,
            isAction = isAction,
            isModerator = isModerator,
            isSubscriber = isSubscriber,
            isOwner = false,
            bits = bits,
            highlight = highlight?.toRemoteControl()
        )
        chatMessageHistory.addLast(message)
        if (chatMessageHistory.size > 100) {
            chatMessageHistory.removeFirst()
        }
        sendChatMessage(message = message)
    }

    override fun twitchChatDeleteMessage(messageId: String) {}

    override fun twitchChatDeleteUser(userId: String) {}
}

private class RemoteControlWebSocket(private val socket: Socket) {
    private val input: InputStream = socket.getInputStream()
    private val output: OutputStream = socket.getOutputStream()
    private val writeLock = Any()

    var onText: ((String) -> Unit)? = null
    var onPing: ((ByteArray) -> Unit)? = null
    var onPong: (() -> Unit)? = null
    var onClosed: (() -> Unit)? = null

    fun sendText(text: String) {
        sendFrame(opcode = 0x1, payload = text.toByteArray(Charsets.UTF_8))
    }

    fun sendPing() {
        sendFrame(opcode = 0x9, payload = ByteArray(0))
    }

    fun sendPong(payload: ByteArray) {
        sendFrame(opcode = 0xA, payload = payload)
    }

    fun cancel() {
        runCatching { socket.close() }
    }

    fun readLoop() {
        var running = true
        try {
            while (running) {
                val header = ByteArray(2)
                if (!readFully(header)) {
                    running = false
                } else {
                    running = handleFrame(header)
                }
            }
        } catch (e: Exception) {
            Log.d("RemoteControlWebSocket", "remote-control-assistant: WebSocket read failed with $e")
        } finally {
            runCatching { socket.close() }
            onClosed?.invoke()
        }
    }

    private fun handleFrame(header: ByteArray): Boolean {
        val opcode = header[0].toInt() and 0x0F
        val masked = (header[1].toInt() and 0x80) != 0
        var length = (header[1].toInt() and 0x7F).toLong()
        var valid = true
        if (length == 126L) {
            val extended = ByteArray(2)
            valid = readFully(extended)
            if (valid) {
                length = (((extended[0].toInt() and 0xFF) shl 8) or (extended[1].toInt() and 0xFF)).toLong()
            }
        } else if (length == 127L) {
            val extended = ByteArray(8)
            valid = readFully(extended)
            if (valid) {
                var value = 0L
                for (i in 0 until 8) {
                    value = (value shl 8) or (extended[i].toInt() and 0xFF).toLong()
                }
                length = value
            }
        }
        if (!valid) {
            return false
        }
        var maskKey: ByteArray? = null
        if (masked) {
            val key = ByteArray(4)
            if (!readFully(key)) {
                return false
            }
            maskKey = key
        }
        val payload = ByteArray(length.toInt())
        if (!readFully(payload)) {
            return false
        }
        if (maskKey != null) {
            for (i in payload.indices) {
                payload[i] = (payload[i].toInt() xor maskKey[i % 4].toInt()).toByte()
            }
        }
        return when (opcode) {
            0x1, 0x2 -> {
                onText?.invoke(payload.toString(Charsets.UTF_8))
                true
            }

            0x8 -> false
            0x9 -> {
                sendFrame(opcode = 0xA, payload = payload)
                onPing?.invoke(payload)
                true
            }

            0xA -> {
                onPong?.invoke()
                true
            }

            else -> true
        }
    }

    private fun readFully(buffer: ByteArray): Boolean {
        var offset = 0
        while (offset < buffer.size) {
            val read = input.read(buffer, offset, buffer.size - offset)
            if (read < 0) {
                return false
            }
            offset += read
        }
        return true
    }

    private fun sendFrame(opcode: Int, payload: ByteArray) {
        synchronized(writeLock) {
            try {
                val header = java.io.ByteArrayOutputStream()
                header.write(0x80 or opcode)
                if (payload.size < 126) {
                    header.write(payload.size)
                } else if (payload.size < 65536) {
                    header.write(126)
                    header.write((payload.size shr 8) and 0xFF)
                    header.write(payload.size and 0xFF)
                } else {
                    header.write(127)
                    val size = payload.size.toLong()
                    for (i in 7 downTo 0) {
                        header.write(((size shr (8 * i)) and 0xFF).toInt())
                    }
                }
                output.write(header.toByteArray())
                output.write(payload)
                output.flush()
            } catch (e: Exception) {
                Log.d("RemoteControlWebSocket", "remote-control-assistant: WebSocket write failed with $e")
            }
        }
    }
}

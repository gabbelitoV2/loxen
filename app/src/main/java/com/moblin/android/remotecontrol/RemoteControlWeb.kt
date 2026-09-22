package com.moblin.android.remotecontrol

import android.util.Log
import com.moblin.android.various.MainTimer
import com.moblin.android.various.network.HttpServer
import com.moblin.android.various.network.HttpServerRequest
import com.moblin.android.various.network.HttpServerResponse
import com.moblin.android.various.network.HttpServerRoute
import com.moblin.android.various.network.HttpServerStatus
import com.moblin.android.various.settings.SettingsGimbalMotion
import com.moblin.android.various.settings.SettingsHttpHeader
import com.moblin.android.various.utils.loadResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID

private const val TAG = "RemoteControlWeb"
private const val OPCODE_TEXT = 0x1
private const val OPCODE_PING = 0x9
private const val OPCODE_PONG = 0xA
private const val WEBSOCKET_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"
private const val MAX_WEBSOCKET_PAYLOAD = 16 * 1024 * 1024

interface RemoteControlWebDelegate {
    fun remoteControlWebConnected()
    fun remoteControlWebDisconnected()
    fun remoteControlWebGetStatus(): Triple<RemoteControlStatusGeneral, RemoteControlStatusTopLeft, RemoteControlStatusTopRight>
    fun remoteControlWebGetSettings(): RemoteControlSettings
    fun remoteControlWebSetScene(id: UUID)
    fun remoteControlWebSetAutoSceneSwitcher(id: UUID?)
    fun remoteControlWebSetMic(id: String)
    fun remoteControlWebSetBitratePreset(id: UUID)
    fun remoteControlWebSetRecord(on: Boolean)
    fun remoteControlWebSetLive(on: Boolean)
    fun remoteControlWebSetPreviewStream(on: Boolean)
    fun remoteControlWebSetZoom(x: Float)
    fun remoteControlWebSetZoomPreset(id: UUID)
    fun remoteControlWebSetDebugLogging(on: Boolean)
    fun remoteControlWebSetMute(on: Boolean)
    fun remoteControlWebSetStealthMode(on: Boolean)
    fun remoteControlWebSetTorch(on: Boolean)
    fun remoteControlWebReloadBrowserWidgets()
    fun remoteControlWebSetSrtConnectionPrioritiesEnabled(enabled: Boolean)
    fun remoteControlWebSetSrtConnectionPriority(id: UUID, priority: Int, enabled: Boolean)
    fun remoteControlWebMoveToGimbalPreset(id: UUID)
    fun remoteControlWebSetGimbalTracking(on: Boolean)
    fun remoteControlWebSetGimbalMovement(x: Float, y: Float)
    fun remoteControlWebAnimateGimbal(motion: SettingsGimbalMotion)
    fun remoteControlWebSaveGimbalPreset()
    fun remoteControlWebGetScoreboardSports(): List<String>
    fun remoteControlWebSetScoreboardSport(sportId: String)
    fun remoteControlWebUpdateScoreboard(config: RemoteControlScoreboardMatchConfig)
    fun remoteControlWebToggleScoreboardClock()
    fun remoteControlWebSetScoreboardDuration(minutes: Int)
    fun remoteControlWebSetScoreboardClock(time: String)
    fun remoteControlWebGetGolfScoreboard(): RemoteControlGolfScoreboard
    fun remoteControlWebUpdateGolfScoreboard(data: RemoteControlGolfScoreboard)
    fun remoteControlWebSetFilter(filter: RemoteControlFilter, on: Boolean)
    fun remoteControlWebTriggerReaction(reaction: RemoteControlReaction)
    fun remoteControlWebGetRecordings(): List<Map<String, String>>
    fun remoteControlWebGetRecordingUrl(filename: String): URI?
    fun remoteControlWebGetRecordingThumbnail(filename: String): ByteArray?
    fun remoteControlWebDeleteRecording(filename: String)
    fun remoteControlWebStartPreview()
    fun remoteControlWebStopPreview()
}

private data class StaticFile(val path: String, val name: String, val ext: String) {
    fun makePath(): String {
        return "$path$name.$ext"
    }
}

private val staticFiles: List<StaticFile> = listOf(
    StaticFile("/", "favicon", "ico"),
    StaticFile("/", "golf", "html"),
    StaticFile("/", "index", "html"),
    StaticFile("/", "recordings", "html"),
    StaticFile("/", "remote", "html"),
    StaticFile("/", "scoreboard", "html"),
    StaticFile("/", "volleyball", "png"),
    StaticFile("/css/", "app", "css"),
    StaticFile("/css/", "common", "css"),
    StaticFile("/css/", "components", "css"),
    StaticFile("/css/", "golf", "css"),
    StaticFile("/css/", "recordings", "css"),
    StaticFile("/css/", "remote", "css"),
    StaticFile("/css/", "scoreboard", "css"),
    StaticFile("/js/", "app", "mjs"),
    StaticFile("/js/", "golf", "mjs"),
    StaticFile("/js/", "index", "mjs"),
    StaticFile("/js/", "components", "mjs"),
    StaticFile("/js/", "recordings", "mjs"),
    StaticFile("/js/", "remote", "mjs"),
    StaticFile("/js/", "scoreboard", "mjs"),
    StaticFile("/js/", "utils", "mjs"),
    StaticFile("/js/", "vendor", "mjs"),
)

private const val recordingsPrefix = "/recordings/"
private const val thumbnailsPrefix = "/thumbnails/"

private class WebsocketFrame(val opcode: Int, val payload: ByteArray)

private class WebsocketConnection(
    val socket: Socket,
    val id: Int,
    val input: InputStream,
    val output: OutputStream,
) {
    fun cancel() {
        runCatching { socket.close() }
    }
}

class RemoteControlWeb(delegate: RemoteControlWebDelegate) {
    private val delegate: RemoteControlWebDelegate? = delegate
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var server: HttpServer? = null
    @Volatile private var started: Boolean = false
    private var websocketServer: ServerSocket? = null
    private var websocketServerJob: Job? = null
    private var websocketPort: Int = 0
    private val websocketRetryTimer = MainTimer()
    private val connections = mutableListOf<WebsocketConnection>()
    private val connectionsRequestingPreview = mutableSetOf<Int>()
    private var connectionIdCounter = 0

    fun start(port: Int) {
        started = true
        startServer(port)
        startWebsocketServer(port + 1)
    }

    fun stop() {
        started = false
        stopServer()
        stopWebsocketServer()
    }

    fun stateChanged(state: RemoteControlAssistantStreamerState) {
        for (connection in connections) {
            send(
                connection = connection,
                message = RemoteControlMessageToAssistant.Event(data = RemoteControlEvent.State(data = state)),
            )
        }
    }

    fun log(entry: String) {
        for (connection in connections) {
            send(
                connection = connection,
                message = RemoteControlMessageToAssistant.Event(data = RemoteControlEvent.Log(entry = entry)),
            )
        }
    }

    fun sendScoreboardUpdate(config: RemoteControlScoreboardMatchConfig) {
        for (connection in connections) {
            send(
                connection = connection,
                message = RemoteControlMessageToAssistant.Event(data = RemoteControlEvent.Scoreboard(config = config)),
            )
        }
    }

    fun sendGolfScoreboardUpdate(data: RemoteControlGolfScoreboard) {
        for (connection in connections) {
            send(
                connection = connection,
                message = RemoteControlMessageToAssistant.Event(data = RemoteControlEvent.GolfScoreboard(data = data)),
            )
        }
    }

    fun sendPreview(preview: ByteArray) {
        for (connection in connections) {
            if (!connectionsRequestingPreview.contains(connection.id)) {
                continue
            }
            send(
                connection = connection,
                message = RemoteControlMessageToAssistant.Preview(preview = preview),
            )
        }
    }

    private fun startServer(port: Int) {
        val routes = mutableListOf<HttpServerRoute>()
        for (file in staticFiles) {
            routes.add(HttpServerRoute(path = file.makePath(), handler = ::handleStatic))
        }
        routes.add(HttpServerRoute(path = "/", handler = ::handleRoot))
        routes.add(HttpServerRoute(path = "/js/config.mjs", handler = ::handleConfigMjs))
        routes.add(HttpServerRoute(path = "/recordings.json", handler = ::handleRecordingsJson))
        routes.add(
            HttpServerRoute(
                path = recordingsPrefix,
                prefixMatch = true,
                handler = ::handleRecordingsFile,
            )
        )
        routes.add(
            HttpServerRoute(
                path = thumbnailsPrefix,
                prefixMatch = true,
                handler = ::handleRecordingsThumbnail,
            )
        )
        server = HttpServer(
            queue = Dispatchers.Main,
            routes = routes,
            service = TODO("no Android counterpart for NetService advertisement"),
        )
        server?.start(port = port)
    }

    private fun startWebsocketServer(port: Int) {
        websocketPort = port
        setupWebsocketServer()
    }

    private fun setupWebsocketServer() {
        websocketServerJob = ioScope.launch {
            try {
                val serverSocket = ServerSocket(websocketPort)
                websocketServer = serverSocket
                while (started) {
                    val socket = serverSocket.accept()
                    socket.tcpNoDelay = true
                    mainScope.launch {
                        handleNewWebsocketConnection(socket)
                    }
                }
            } catch (exception: Exception) {
                if (started) {
                    mainScope.launch {
                        handleWebsocketStateUpdate(failed = true)
                    }
                }
            }
        }
    }

    private fun stopServer() {
        server?.stop()
        server = null
    }

    private fun stopWebsocketServer() {
        for (connection in connections) {
            connection.cancel()
        }
        connections.clear()
        websocketRetryTimer.stop()
        websocketServer?.close()
        websocketServer = null
        websocketServerJob?.cancel()
        websocketServerJob = null
    }

    private fun handleRoot(request: HttpServerRequest, response: HttpServerResponse) {
        if (request.method != "GET") {
            return
        }
        response.send(data = loadResource(name = "index", ext = "html"))
    }

    private fun handleStatic(request: HttpServerRequest, response: HttpServerResponse) {
        if (request.method != "GET") {
            return
        }
        val staticPath = staticFiles.firstOrNull {
            request.path == it.makePath()
        } ?: return
        response.send(data = loadResource(name = staticPath.name, ext = staticPath.ext))
    }

    private fun handleConfigMjs(request: HttpServerRequest, response: HttpServerResponse) {
        if (request.method != "GET") {
            return
        }
        val configMjs = "export const websocketPort = $websocketPort;"
        response.send(text = configMjs)
    }

    private fun handleRecordingsJson(request: HttpServerRequest, response: HttpServerResponse) {
        if (request.method != "GET") {
            return
        }
        val delegate = this.delegate ?: run {
            response.send(status = HttpServerStatus.notFound)
            return
        }
        val recordings = delegate.remoteControlWebGetRecordings()
        val json = runCatching { JSONArray(recordings).toString().toByteArray(Charsets.UTF_8) }.getOrNull()
        if (json == null) {
            response.send(status = HttpServerStatus.notFound)
            return
        }
        response.send(data = json, status = HttpServerStatus.ok, contentType = "application/json")
    }

    private fun handleRecordingsFile(request: HttpServerRequest, response: HttpServerResponse) {
        val filename = request.path.substring(recordingsPrefix.length)
        when (request.method) {
            "GET" -> {
                val fileUrl = delegate?.remoteControlWebGetRecordingUrl(filename = filename) ?: run {
                    response.send(status = HttpServerStatus.notFound)
                    return
                }
                val headers = listOf(
                    SettingsHttpHeader(
                        name = "Content-Disposition",
                        value = "attachment; filename=\"$filename\"",
                    ),
                )
                response.sendFile(url = fileUrl, contentType = "video/mp4", headers = headers)
            }
            "DELETE" -> {
                delegate?.remoteControlWebDeleteRecording(filename = filename)
                response.send(status = HttpServerStatus.ok)
            }
            else -> {
            }
        }
    }

    private fun handleRecordingsThumbnail(request: HttpServerRequest, response: HttpServerResponse) {
        if (request.method != "GET") {
            return
        }
        val filename = request.path.substring(thumbnailsPrefix.length)
        val thumbnail = delegate?.remoteControlWebGetRecordingThumbnail(filename = filename) ?: run {
            response.send(status = HttpServerStatus.notFound)
            return
        }
        response.send(data = thumbnail, status = HttpServerStatus.ok, contentType = "image/jpeg")
    }

    private fun handleWebsocketStateUpdate(failed: Boolean) {
        if (failed) {
            websocketRetryTimer.startSingleShot(timeout = 1.0) {
                if (started) {
                    setupWebsocketServer()
                }
            }
        }
    }

    private fun handleNewWebsocketConnection(socket: Socket) {
        val connection = try {
            val input = socket.getInputStream()
            val output = socket.getOutputStream()
            performWebsocketHandshake(input, output)
            connectionIdCounter += 1
            WebsocketConnection(
                socket = socket,
                id = connectionIdCounter,
                input = input,
                output = output,
            )
        } catch (exception: Exception) {
            runCatching { socket.close() }
            return
        }
        connections.add(connection)
        receiveWebsocketPacket(connection = connection)
        delegate?.remoteControlWebConnected()
    }

    private fun receiveWebsocketPacket(connection: WebsocketConnection) {
        ioScope.launch {
            while (true) {
                val frame = try {
                    readWebsocketFrame(connection.input)
                } catch (exception: Exception) {
                    null
                }
                if (frame == null) {
                    mainScope.launch { handleDisconnected(connection = connection) }
                    return@launch
                }
                when (frame.opcode) {
                    OPCODE_TEXT -> {
                        if (frame.payload.isNotEmpty()) {
                            mainScope.launch {
                                handleWebsocketMessage(connection = connection, packet = frame.payload)
                            }
                        } else {
                            mainScope.launch { handleDisconnected(connection = connection) }
                            return@launch
                        }
                    }
                    OPCODE_PING -> {
                        sendWebSocket(connection = connection, data = frame.payload, opcode = OPCODE_PONG)
                    }
                    OPCODE_PONG -> {
                    }
                    else -> {
                        mainScope.launch { handleDisconnected(connection = connection) }
                        return@launch
                    }
                }
            }
        }
    }

    private fun handleDisconnected(connection: WebsocketConnection) {
        connection.cancel()
        connections.removeAll { it === connection }
        delegate?.remoteControlWebDisconnected()
        val id = connection.id
        if (connectionsRequestingPreview.contains(id)) {
            connectionsRequestingPreview.remove(id)
            if (connectionsRequestingPreview.isEmpty()) {
                delegate?.remoteControlWebStopPreview()
            }
        }
    }

    private fun handleWebsocketMessage(connection: WebsocketConnection, packet: ByteArray) {
        val message = packet.toString(Charsets.UTF_8)
        try {
            when (val decoded = RemoteControlMessageToStreamer.fromJson(message)) {
                is RemoteControlMessageToStreamer.Request -> handleRequest(
                    connection = connection,
                    id = decoded.id,
                    data = decoded.data,
                )
                else -> {
                }
            }
        } catch (exception: Exception) {
            Log.i(TAG, "remote-control-web: Decode error $exception for message $message")
        }
    }

    private fun handleRequest(connection: WebsocketConnection, id: Int, data: RemoteControlRequest) {
        val delegate = this.delegate ?: return
        when (data) {
            is RemoteControlRequest.GetStatus -> {
                val (general, topLeft, topRight) = delegate.remoteControlWebGetStatus()
                send(
                    connection = connection,
                    message = RemoteControlMessageToAssistant.Response(
                        id = id,
                        result = RemoteControlResult.ok,
                        data = RemoteControlResponse.GetStatus(
                            general = general,
                            topLeft = topLeft,
                            topRight = topRight,
                        ),
                    ),
                )
            }
            is RemoteControlRequest.GetSettings -> {
                val settings = delegate.remoteControlWebGetSettings()
                send(
                    connection = connection,
                    message = RemoteControlMessageToAssistant.Response(
                        id = id,
                        result = RemoteControlResult.ok,
                        data = RemoteControlResponse.GetSettings(data = settings),
                    ),
                )
            }
            is RemoteControlRequest.SetScene -> {
                delegate.remoteControlWebSetScene(id = data.id)
                sendEmptyOkResponse(connection = connection, id = id)
            }
            is RemoteControlRequest.SetAutoSceneSwitcher -> {
                delegate.remoteControlWebSetAutoSceneSwitcher(id = data.id)
                sendEmptyOkResponse(connection = connection, id = id)
            }
            is RemoteControlRequest.SetMic -> {
                delegate.remoteControlWebSetMic(id = data.id)
                sendEmptyOkResponse(connection = connection, id = id)
            }
            is RemoteControlRequest.SetBitratePreset -> {
                delegate.remoteControlWebSetBitratePreset(id = data.id)
                sendEmptyOkResponse(connection = connection, id = id)
            }
            is RemoteControlRequest.SetRecord -> {
                delegate.remoteControlWebSetRecord(on = data.on)
                sendEmptyOkResponse(connection = connection, id = id)
            }
            is RemoteControlRequest.SetLive -> {
                delegate.remoteControlWebSetLive(on = data.on)
                sendEmptyOkResponse(connection = connection, id = id)
            }
            is RemoteControlRequest.SetPreviewStream -> {
                delegate.remoteControlWebSetPreviewStream(on = data.on)
                sendEmptyOkResponse(connection = connection, id = id)
            }
            is RemoteControlRequest.SetZoom -> {
                delegate.remoteControlWebSetZoom(x = data.x)
                sendEmptyOkResponse(connection = connection, id = id)
            }
            is RemoteControlRequest.SetZoomPreset -> {
                delegate.remoteControlWebSetZoomPreset(id = data.id)
                sendEmptyOkResponse(connection = connection, id = id)
            }
            is RemoteControlRequest.SetMute -> {
                delegate.remoteControlWebSetMute(on = data.on)
                sendEmptyOkResponse(connection = connection, id = id)
            }
            is RemoteControlRequest.SetStealthMode -> {
                delegate.remoteControlWebSetStealthMode(on = data.on)
                sendEmptyOkResponse(connection = connection, id = id)
            }
            is RemoteControlRequest.SetTorch -> {
                delegate.remoteControlWebSetTorch(on = data.on)
                sendEmptyOkResponse(connection = connection, id = id)
            }
            is RemoteControlRequest.SetDebugLogging -> {
                delegate.remoteControlWebSetDebugLogging(on = data.on)
                sendEmptyOkResponse(connection = connection, id = id)
            }
            is RemoteControlRequest.ReloadBrowserWidgets -> {
                delegate.remoteControlWebReloadBrowserWidgets()
                sendEmptyOkResponse(connection = connection, id = id)
            }
            is RemoteControlRequest.SetSrtConnectionPrioritiesEnabled -> {
                delegate.remoteControlWebSetSrtConnectionPrioritiesEnabled(enabled = data.enabled)
                sendEmptyOkResponse(connection = connection, id = id)
            }
            is RemoteControlRequest.SetSrtConnectionPriority -> {
                delegate.remoteControlWebSetSrtConnectionPriority(
                    id = data.id,
                    priority = data.priority,
                    enabled = data.enabled,
                )
                sendEmptyOkResponse(connection = connection, id = id)
            }
            is RemoteControlRequest.MoveToGimbalPreset -> {
                delegate.remoteControlWebMoveToGimbalPreset(id = data.id)
                sendEmptyOkResponse(connection = connection, id = id)
            }
            is RemoteControlRequest.SetGimbalTracking -> {
                delegate.remoteControlWebSetGimbalTracking(on = data.on)
                sendEmptyOkResponse(connection = connection, id = id)
            }
            is RemoteControlRequest.SetGimbalMovement -> {
                delegate.remoteControlWebSetGimbalMovement(x = data.x, y = data.y)
                sendEmptyOkResponse(connection = connection, id = id)
            }
            is RemoteControlRequest.AnimateGimbal -> {
                delegate.remoteControlWebAnimateGimbal(motion = data.motion)
                sendEmptyOkResponse(connection = connection, id = id)
            }
            is RemoteControlRequest.SaveGimbalPreset -> {
                delegate.remoteControlWebSaveGimbalPreset()
                sendEmptyOkResponse(connection = connection, id = id)
            }
            is RemoteControlRequest.GetScoreboardSports -> {
                val sports = delegate.remoteControlWebGetScoreboardSports()
                send(
                    connection = connection,
                    message = RemoteControlMessageToAssistant.Response(
                        id = id,
                        result = RemoteControlResult.ok,
                        data = RemoteControlResponse.GetScoreboardSports(names = sports),
                    ),
                )
            }
            is RemoteControlRequest.SetScoreboardSport -> {
                delegate.remoteControlWebSetScoreboardSport(sportId = data.sportId)
                sendEmptyOkResponse(connection = connection, id = id)
            }
            is RemoteControlRequest.UpdateScoreboard -> {
                delegate.remoteControlWebUpdateScoreboard(config = data.config)
                sendEmptyOkResponse(connection = connection, id = id)
            }
            is RemoteControlRequest.ToggleScoreboardClock -> {
                delegate.remoteControlWebToggleScoreboardClock()
                sendEmptyOkResponse(connection = connection, id = id)
            }
            is RemoteControlRequest.SetScoreboardDuration -> {
                delegate.remoteControlWebSetScoreboardDuration(minutes = data.minutes)
                sendEmptyOkResponse(connection = connection, id = id)
            }
            is RemoteControlRequest.SetScoreboardClock -> {
                delegate.remoteControlWebSetScoreboardClock(time = data.time)
                sendEmptyOkResponse(connection = connection, id = id)
            }
            is RemoteControlRequest.GetGolfScoreboard -> {
                val golfScoreboard = delegate.remoteControlWebGetGolfScoreboard()
                send(
                    connection = connection,
                    message = RemoteControlMessageToAssistant.Response(
                        id = id,
                        result = RemoteControlResult.ok,
                        data = RemoteControlResponse.GetGolfScoreboard(data = golfScoreboard),
                    ),
                )
            }
            is RemoteControlRequest.UpdateGolfScoreboard -> {
                delegate.remoteControlWebUpdateGolfScoreboard(data = data.data)
                sendEmptyOkResponse(connection = connection, id = id)
            }
            is RemoteControlRequest.SetFilter -> {
                delegate.remoteControlWebSetFilter(filter = data.filter, on = data.on)
                sendEmptyOkResponse(connection = connection, id = id)
            }
            is RemoteControlRequest.TriggerReaction -> {
                delegate.remoteControlWebTriggerReaction(reaction = data.reaction)
                sendEmptyOkResponse(connection = connection, id = id)
            }
            is RemoteControlRequest.StartPreview -> {
                val connectionId = connection.id
                connectionsRequestingPreview.add(connectionId)
                if (connectionsRequestingPreview.size == 1) {
                    delegate.remoteControlWebStartPreview()
                }
                sendEmptyOkResponse(connection = connection, id = id)
            }
            is RemoteControlRequest.StopPreview -> {
                val connectionId = connection.id
                connectionsRequestingPreview.remove(connectionId)
                if (connectionsRequestingPreview.isEmpty()) {
                    delegate.remoteControlWebStopPreview()
                }
                sendEmptyOkResponse(connection = connection, id = id)
            }
            else -> {
            }
        }
    }

    private fun send(connection: WebsocketConnection, message: RemoteControlMessageToAssistant) {
        try {
            val text = message.toJson()
            sendWebSocket(connection = connection, data = text.toByteArray(Charsets.UTF_8), opcode = OPCODE_TEXT)
        } catch (exception: Exception) {
            Log.i(TAG, "remote-control-web: Encode failed")
        }
    }

    private fun sendEmptyOkResponse(connection: WebsocketConnection, id: Int) {
        send(
            connection = connection,
            message = RemoteControlMessageToAssistant.Response(
                id = id,
                result = RemoteControlResult.ok,
                data = null,
            ),
        )
    }

    private fun sendWebSocket(connection: WebsocketConnection, data: ByteArray, opcode: Int) {
        try {
            writeWebsocketFrame(output = connection.output, opcode = opcode, payload = data)
        } catch (exception: Exception) {
            Log.i(TAG, "remote-control-web: send failed")
        }
    }

    private fun performWebsocketHandshake(input: InputStream, output: OutputStream) {
        readHttpLine(input) ?: throw java.io.IOException("Invalid websocket handshake")
        var key: String? = null
        while (true) {
            val line = readHttpLine(input) ?: break
            if (line.isEmpty()) {
                break
            }
            val separator = line.indexOf(':')
            if (separator > 0) {
                val name = line.substring(0, separator).trim()
                val value = line.substring(separator + 1).trim()
                if (name.equals("Sec-WebSocket-Key", ignoreCase = true)) {
                    key = value
                }
            }
        }
        val websocketKey = key ?: throw java.io.IOException("Missing Sec-WebSocket-Key")
        val accept = Base64.getEncoder().encodeToString(
            MessageDigest.getInstance("SHA-1")
                .digest((websocketKey + WEBSOCKET_GUID).toByteArray(Charsets.UTF_8))
        )
        val response = "HTTP/1.1 101 Switching Protocols\r\n" +
            "Upgrade: websocket\r\n" +
            "Connection: Upgrade\r\n" +
            "Sec-WebSocket-Accept: $accept\r\n" +
            "\r\n"
        output.write(response.toByteArray(Charsets.US_ASCII))
        output.flush()
    }

    private fun readHttpLine(input: InputStream): String? {
        val builder = StringBuilder()
        while (true) {
            val byte = input.read()
            if (byte < 0) {
                return if (builder.isEmpty()) null else builder.toString()
            }
            if (byte == '\n'.code) {
                return builder.toString().trimEnd('\r')
            }
            builder.append(byte.toChar())
        }
    }

    private fun readWebsocketFrame(input: InputStream): WebsocketFrame? {
        val first = input.read()
        if (first < 0) {
            return null
        }
        val opcode = first and 0x0F
        val second = input.read()
        if (second < 0) {
            return null
        }
        val masked = (second and 0x80) != 0
        var length = (second and 0x7F).toLong()
        if (length == 126L) {
            val b0 = input.read()
            val b1 = input.read()
            if (b0 < 0 || b1 < 0) {
                return null
            }
            length = (((b0 and 0xFF) shl 8) or (b1 and 0xFF)).toLong()
        } else if (length == 127L) {
            length = 0
            for (index in 0 until 8) {
                val byte = input.read()
                if (byte < 0) {
                    return null
                }
                length = (length shl 8) or (byte and 0xFF).toLong()
            }
        }
        val mask = ByteArray(4)
        if (masked) {
            if (!readFully(input, mask)) {
                return null
            }
        }
        if (length > MAX_WEBSOCKET_PAYLOAD) {
            return null
        }
        val payload = ByteArray(length.toInt())
        if (!readFully(input, payload)) {
            return null
        }
        if (masked) {
            for (index in payload.indices) {
                payload[index] = (payload[index].toInt() xor mask[index % 4].toInt()).toByte()
            }
        }
        return WebsocketFrame(opcode = opcode, payload = payload)
    }

    private fun readFully(input: InputStream, buffer: ByteArray): Boolean {
        var offset = 0
        while (offset < buffer.size) {
            val count = input.read(buffer, offset, buffer.size - offset)
            if (count < 0) {
                return false
            }
            offset += count
        }
        return true
    }

    private fun writeWebsocketFrame(output: OutputStream, opcode: Int, payload: ByteArray) {
        val header = ByteArray(10)
        var index = 0
        header[index++] = (0x80 or opcode).toByte()
        val length = payload.size
        if (length < 126) {
            header[index++] = length.toByte()
        } else if (length < 65536) {
            header[index++] = 126
            header[index++] = ((length shr 8) and 0xFF).toByte()
            header[index++] = (length and 0xFF).toByte()
        } else {
            header[index++] = 127
            var shift = 56
            while (shift >= 0) {
                header[index++] = ((length.toLong() shr shift) and 0xFF).toByte()
                shift -= 8
            }
        }
        synchronized(output) {
            output.write(header, 0, index)
            output.write(payload)
            output.flush()
        }
    }
}

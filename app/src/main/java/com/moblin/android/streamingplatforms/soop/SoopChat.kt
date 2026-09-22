package com.moblin.android.streamingplatforms.soop

import android.util.Log
import com.moblin.android.integrations.emotes.Emotes
import com.moblin.android.various.ChatPostSegment
import com.moblin.android.various.model.Model
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.time.Instant
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString

private const val TAG = "SoopChat"

private enum class MessageKind(val rawValue: Int) {
    Null(0),
    One(1),
    Two(2),
    Join(4),
    Post(5),
    A(54),
    B(90),
    C(94),
    Image(109),
    D(87),
    E(12),
    ;

    companion object {
        fun fromRawValue(value: Int): MessageKind? = entries.firstOrNull { it.rawValue == value }
    }
}

private fun packMessage(kind: MessageKind, parts: List<String>): ByteArray {
    var payload = "\u000c"
    payload += parts.joinToString("\u000c")
    if (parts.isNotEmpty()) {
        payload += "\u000c"
    }
    var message = String.format(Locale.US, "\u001b\t%04d%06d00", kind.rawValue, payload.length)
    message += payload
    return message.toByteArray(Charsets.UTF_8)
}

private fun unpackMessage(message: ByteArray): Pair<MessageKind?, List<String>> {
    val text = try {
        Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(message))
            .toString()
    } catch (e: Exception) {
        throw Exception("Bad message not UTF-8")
    }
    if (text.length <= 14) {
        throw Exception("Message too short")
    }
    val value = text.substring(2, 6).toIntOrNull() ?: throw Exception("Bad kind")
    val kind = MessageKind.fromRawValue(value)
    if (kind == null) {
        Log.d(TAG, "soop: Unknown kind $value")
        return null to emptyList()
    }
    val payload = text.substring(14)
    return kind to payload.trim { it == '\u000c' }.split("\u000c")
}

@Serializable
data class PlayerLiveChannel(
    @SerialName("CHDOMAIN") val chdomain: String,
    @SerialName("CHPT") val chpt: String,
    @SerialName("CHATNO") val chatno: String,
    @SerialName("FTK") val ftk: String,
)

@Serializable
data class PlayerLiveResponse(@SerialName("CHANNEL") val channel: PlayerLiveChannel)

private sealed class WebSocketMessage {
    class Binary(val data: ByteArray) : WebSocketMessage()

    class Text(val text: String) : WebSocketMessage()
}

class SoopChat(
    private val model: Model,
    private val channelName: String,
    private val streamId: String,
) {
    private var task: Job? = null
    private var connected: Boolean = false
    private var webSocket: WebSocket? = null
    private var emotes: Emotes = Emotes()
    private var keepAliveTask: Job? = null
    private var messages: Channel<WebSocketMessage>? = null
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    private val mainScope = MainScope()

    private fun makeWebSocketUrl(chdomain: String, chpt: String): String? {
        val port = chpt.toIntOrNull() ?: return null
        return "wss://$chdomain:${port + 1}/Websocket/$channelName"
    }

    private suspend fun sendOne() {
        webSocket?.send(packMessage(MessageKind.One, listOf("", "", "16")).toByteString())
    }

    private suspend fun sendTwo(chatno: String, ftk: String) {
        webSocket?.send(
            packMessage(
                MessageKind.Two,
                listOf(
                    chatno,
                    ftk,
                    "0",
                    "",
                    "log\u0011\u0006&\u0006" +
                        "set_bps\u0006=\u00068000\u0006&\u0006" +
                        "view_bps\u0006=\u00061000\u0006&\u0006" +
                        "quality\u0006=\u0006normal\u0006&\u0006" +
                        "uuid\u0006=\u00061e43cf6d37913c36b35d580e0b5656ec\u0006&\u0006" +
                        "geo_cc\u0006=\u0006KR\u0006&\u0006" +
                        "geo_rc\u0006=\u000611\u0006&\u0006" +
                        "acpt_lang\u0006=\u0006ko_KR\u0006&\u0006" +
                        "svc_lang\u0006=\u0006ko_KR\u0012" +
                        "pwd\u0011" + "\u0012" +
                        "auth_info\u0011NULL\u0012" +
                        "pver\u0011" + "1\u0012" +
                        "access_system\u0011html5\u0012",
                ),
            ).toByteString(),
        )
    }

    fun start() {
        stop()
        Log.d(TAG, "soop: start")
        task = mainScope.launch {
            while (true) {
                try {
                    val info = getChannelInfo()
                    setupConnection(info)
                    setupKeepAlive()
                    receiveMessages(info)
                } catch (e: Exception) {
                    Log.d(TAG, "soop: error: $e")
                }
                if (!coroutineContext.isActive) {
                    Log.d(TAG, "soop: Cancelled")
                    connected = false
                    break
                }
                Log.d(TAG, "soop: Disconnected")
                connected = false
                delay(5_000)
                Log.d(TAG, "soop: Reconnecting")
            }
        }
    }

    fun stop() {
        Log.d(TAG, "soop: stop")
        webSocket?.cancel()
        keepAliveTask?.cancel()
        keepAliveTask = null
        task?.cancel()
        task = null
    }

    fun isConnected(): Boolean {
        return connected
    }

    fun hasEmotes(): Boolean {
        return true
    }

    private suspend fun setupConnection(info: PlayerLiveChannel) {
        val url = makeWebSocketUrl(info.chdomain, info.chpt)
            ?: throw Exception("Failed to create URL")
        Log.d(TAG, "soop: URL $url")
        val channel = Channel<WebSocketMessage>(Channel.UNLIMITED)
        messages = channel
        val request = Request.Builder()
            .url(url)
            .header("Sec-WebSocket-Protocol", "chat")
            .build()
        webSocket = client.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    channel.trySend(WebSocketMessage.Binary(bytes.toByteArray()))
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    channel.trySend(WebSocketMessage.Text(text))
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    channel.close()
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    channel.close(t)
                }
            },
        )
        sendOne()
    }

    private fun setupKeepAlive() {
        keepAliveTask?.cancel()
        keepAliveTask = mainScope.launch {
            val message = packMessage(MessageKind.Null, emptyList())
            while (isActive) {
                delay(60_000)
                Log.d(TAG, "soop: Sending keep alive")
                webSocket?.send(message.toByteString())
            }
        }
    }

    private suspend fun receive(): WebSocketMessage {
        val channel = messages ?: throw Exception("No web socket")
        return channel.receive()
    }

    private suspend fun receiveMessages(info: PlayerLiveChannel) {
        while (true) {
            val message = receive()
            if (!coroutineContext.isActive) {
                break
            }
            when (message) {
                is WebSocketMessage.Binary -> {
                    val (kind, parts) = unpackMessage(message.data)
                    if (kind != null) {
                        if (kind != MessageKind.Join) {
                            Log.d(TAG, "soop: Got $kind $parts")
                        }
                    } else {
                        Log.d(TAG, "soop: Got $parts")
                    }
                    when (kind) {
                        MessageKind.One -> {
                            Log.d(TAG, "soop: Connected?")
                            connected = true
                            sendTwo(info.chatno, info.ftk)
                        }
                        MessageKind.Post -> handlePostMessage(parts)
                        else -> {}
                    }
                }
                is WebSocketMessage.Text -> Log.d(TAG, "soop: Got string ${message.text}")
            }
        }
    }

    private fun handlePostMessage(parts: List<String>) {
        if (parts.size <= 5) {
            Log.i(TAG, "soop: Bad post length")
            return
        }
        val user = parts[5]
        val segments = createSegments(parts[0])
        Unit
    }

    private suspend fun getChannelInfo(): PlayerLiveChannel {
        val url = "https://live.afreecatv.com/afreeca/player_live_api.php?bjid=$channelName"
        val body = "bid=$channelName&bno=$streamId&type=live&confirm_adult=false" +
            "&player_type=html5&mode=landing&from_api=0&pwd=&stream_type=common&quality=HD"
        val request = Request.Builder()
            .url(url)
            .post(body.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
            .build()
        val (data, successful) = withContext(kotlinx.coroutines.Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                (response.body?.bytes() ?: ByteArray(0)) to response.isSuccessful
            }
        }
        if (!successful) {
            throw Exception("Not successful")
        }
        return json.decodeFromString(PlayerLiveResponse.serializer(), String(data, Charsets.UTF_8))
            .channel
    }

    private fun createSegments(message: String): List<ChatPostSegment> {
        val id = AtomicInteger(0)
        return emotes.createSegments(message, id)
    }
}

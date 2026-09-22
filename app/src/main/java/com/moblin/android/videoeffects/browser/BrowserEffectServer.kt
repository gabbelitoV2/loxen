package com.moblin.android.videoeffects.browser

import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.moblin.android.various.ChatPost
import com.moblin.android.various.ChatPostSegment
import com.moblin.android.various.MainTimer
import com.moblin.android.various.utils.loadStringResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

private const val TAG = "BrowserEffectServer"

private fun moblinScript(): String {
    return loadStringResource("moblin", "js")
}

private sealed class PublishMessage {
    data class VideoPlaying(val value: Boolean) : PublishMessage()

    data class Log(val message: String) : PublishMessage()

    companion object {
        fun fromJson(json: JsonObject): PublishMessage {
            json["videoPlaying"]?.let {
                return VideoPlaying(it.jsonObject.getValue("value").jsonPrimitive.boolean)
            }
            json["log"]?.let {
                return Log(it.jsonObject.getValue("message").jsonPrimitive.content)
            }
            throw IllegalArgumentException("Unknown publish message")
        }
    }
}

private sealed class SubscribeTopic {
    data class Chat(val prefix: String?) : SubscribeTopic()

    data object SpeechToText : SubscribeTopic()

    companion object {
        fun fromJson(json: JsonObject): SubscribeTopic {
            json["chat"]?.let {
                return Chat(it.jsonObject["prefix"]?.jsonPrimitive?.contentOrNull)
            }
            json["speechToText"]?.let {
                return SpeechToText
            }
            throw IllegalArgumentException("Unknown subscribe topic")
        }
    }
}

private sealed class Message {
    data class Chat(val message: ChatMessage) : Message()

    data class SpeechToText(val position: Int, val text: String) : Message()

    data object SpeechToTextClear : Message()

    fun toJson(): JsonObject {
        return when (this) {
            is Chat -> {
                val chatMessage = message
                buildJsonObject {
                    put("chat", buildJsonObject {
                        put("message", Json.encodeToJsonElement(chatMessage))
                    })
                }
            }
            is SpeechToText -> {
                val speechToTextPosition = position
                val speechToTextText = text
                buildJsonObject {
                    put("speechToText", buildJsonObject {
                        put("position", speechToTextPosition)
                        put("text", speechToTextText)
                    })
                }
            }
            SpeechToTextClear -> buildJsonObject {
                put("speechToTextClear", buildJsonObject {})
            }
        }
    }
}

@Serializable
private data class ChatMessage(
    @SerialName("user") var user: String,
    @SerialName("segments") var segments: List<ChatPostSegment>,
) {
    constructor(message: ChatPost) : this(
        user = message.user ?: "???",
        segments = message.segments,
    )
}

private sealed class MessageToMoblin {
    data object Ping : MessageToMoblin()

    data class Publish(val message: PublishMessage) : MessageToMoblin()

    data class Subscribe(val topic: SubscribeTopic) : MessageToMoblin()

    companion object {
        fun fromJson(data: String): MessageToMoblin {
            val json = Json.parseToJsonElement(data).jsonObject
            json["ping"]?.let {
                return Ping
            }
            json["publish"]?.let {
                val message = it.jsonObject.getValue("message").jsonObject
                return Publish(PublishMessage.fromJson(message))
            }
            json["subscribe"]?.let {
                val topic = it.jsonObject.getValue("topic").jsonObject
                return Subscribe(SubscribeTopic.fromJson(topic))
            }
            throw IllegalArgumentException("Not a message to moblin")
        }
    }
}

private sealed class MessageToBrowser {
    data class Message(val data: com.moblin.android.videoeffects.browser.Message) : MessageToBrowser()

    fun toJson(): String? {
        return when (this) {
            is Message -> {
                val message = data
                buildJsonObject {
                    put("message", message.toJson())
                }.toString()
            }
        }
    }
}

private data class Chat(val prefix: String?)

private class Subscriptions {
    var chat: Chat? = null
    var speechToText: Boolean = false
}

interface BrowserEffectServerDelegate {
    fun browserEffectServerVideoPlaying()
    fun browserEffectServerVideoEnded()
}

class BrowserEffectServer(
    configuration: WebView?,
    private val moblinAccess: Boolean,
) {
    var webView: WebView? = configuration
    private val subscriptions = Subscriptions()
    private val pingTimer = MainTimer()
    private var gotPing = true
    var delegate: BrowserEffectServerDelegate? = null
    private val mainScope = CoroutineScope(Dispatchers.Main)

    init {
        webView?.addJavascriptInterface(this, "moblinAndroid")
        webView?.evaluateJavascript(moblinScript(), null)
    }

    fun enable() {
        pingTimer.startPeriodic(5.0) {
            handlePingTimer()
        }
    }

    fun disable() {
        pingTimer.stop()
    }

    fun sendChatMessage(post: ChatPost) {
        val chat = subscriptions.chat ?: return
        val prefix = chat.prefix
        if (prefix != null) {
            val text = post.segments.firstOrNull()?.text ?: return
            if (!text.startsWith(prefix)) {
                return
            }
        }
        send(MessageToBrowser.Message(Message.Chat(ChatMessage(post))))
    }

    fun sendSpeechToText(position: Int, text: String) {
        if (!subscriptions.speechToText) {
            return
        }
        send(MessageToBrowser.Message(Message.SpeechToText(position = position, text = text)))
    }

    fun sendSpeechToTextClear() {
        if (!subscriptions.speechToText) {
            return
        }
        send(MessageToBrowser.Message(Message.SpeechToTextClear))
    }

    private fun handlePingTimer() {
        if (!gotPing) {
            Log.i(TAG, "browser-effect-server: Ping timeout")
            delegate?.browserEffectServerVideoEnded()
        }
        gotPing = false
    }

    private fun send(message: MessageToBrowser) {
        val json = message.toJson() ?: return
        val data = Base64.encodeToString(json.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        webView?.evaluateJavascript(
            """
            moblin.handleMessage("$data")
            """.trimIndent(),
            null,
        )
    }

    private fun handleMessage(message: String) {
        val parsed = try {
            MessageToMoblin.fromJson(message)
        } catch (e: Exception) {
            Log.i(TAG, "browser-effect-server: Decode failed with error: $e")
            return
        }
        when (parsed) {
            is MessageToMoblin.Ping -> handlePing()
            is MessageToMoblin.Publish -> handlePublish(parsed.message)
            is MessageToMoblin.Subscribe -> handleSubscribe(parsed.topic)
        }
    }

    private fun handlePing() {
        gotPing = true
    }

    private fun handlePublish(message: PublishMessage) {
        when (message) {
            is PublishMessage.VideoPlaying -> {
                Log.d(TAG, "browser-effect-server: Got video playing: ${message.value}")
                if (message.value) {
                    delegate?.browserEffectServerVideoPlaying()
                } else {
                    delegate?.browserEffectServerVideoEnded()
                }
            }
            is PublishMessage.Log -> {
                Log.i(TAG, "browser-effect-server: Log ${message.message}")
            }
        }
    }

    private fun handleSubscribe(topic: SubscribeTopic) {
        if (!moblinAccess) {
            return
        }
        when (topic) {
            is SubscribeTopic.Chat -> subscriptions.chat = Chat(topic.prefix)
            is SubscribeTopic.SpeechToText -> subscriptions.speechToText = true
        }
    }

    @JavascriptInterface
    fun userContentController(message: String) {
        mainScope.launch {
            handleMessage(message)
        }
    }
}

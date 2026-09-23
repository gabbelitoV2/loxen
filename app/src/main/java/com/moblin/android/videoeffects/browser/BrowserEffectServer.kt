package com.moblin.android.videoeffects.browser

import android.util.Log
import com.moblin.android.platform.codable.codableJson
import com.moblin.android.platform.webkit.WKScriptMessage
import com.moblin.android.platform.webkit.WKScriptMessageHandler
import com.moblin.android.platform.webkit.WKUserContentController
import com.moblin.android.platform.webkit.WKUserScript
import com.moblin.android.platform.webkit.WKUserScriptInjectionTime
import com.moblin.android.platform.webkit.WKWebView
import com.moblin.android.platform.webkit.WKWebViewConfiguration
import com.moblin.android.various.ChatPost
import com.moblin.android.various.ChatPostSegment
import com.moblin.android.various.ChatPostUrl
import com.moblin.android.various.MainTimer
import com.moblin.android.various.utils.loadStringResource
import java.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*

private fun moblinScript(): String {
    return loadStringResource("moblin", "js")
}

private sealed class PublishMessage {
    data class VideoPlaying(val value: Boolean) : PublishMessage()
    data class Log(val message: String) : PublishMessage()

    companion object {
        fun decode(json: JsonObject): PublishMessage {
            (json["videoPlaying"] as? JsonObject)?.let { obj ->
                val value = (obj["value"] as? JsonPrimitive)?.booleanOrNull ?: false
                return PublishMessage.VideoPlaying(value)
            }
            (json["log"] as? JsonObject)?.let { obj ->
                val message = (obj["message"] as? JsonPrimitive)?.contentOrNull ?: ""
                return PublishMessage.Log(message)
            }
            throw IllegalArgumentException("Invalid publish message")
        }
    }
}

private sealed class SubscribeTopic {
    data class Chat(val prefix: String?) : SubscribeTopic()
    object SpeechToText : SubscribeTopic()

    companion object {
        fun decode(json: JsonObject): SubscribeTopic {
            (json["chat"] as? JsonObject)?.let { obj ->
                val prefix = (obj["prefix"] as? JsonPrimitive)?.contentOrNull
                return SubscribeTopic.Chat(prefix)
            }
            if (json.containsKey("speechToText")) {
                return SubscribeTopic.SpeechToText
            }
            throw IllegalArgumentException("Invalid subscribe topic")
        }
    }
}

private sealed class Message {
    data class Chat(val message: ChatMessage) : Message()
    data class SpeechToText(val position: Int, val text: String) : Message()
    object SpeechToTextClear : Message()

    fun encode(): JsonObject = when (this) {
        is Chat -> buildJsonObject {
            put("chat", buildJsonObject { put("message", message.encode()) })
        }
        is SpeechToText -> buildJsonObject {
            put("speechToText", buildJsonObject {
                put("position", position)
                put("text", text)
            })
        }
        is SpeechToTextClear -> buildJsonObject {
            put("speechToTextClear", buildJsonObject { })
        }
    }
}

private data class ChatMessage(val user: String, val segments: List<ChatPostSegment>) {
    constructor(message: ChatPost) : this(message.user ?: "???", message.segments)

    fun encode(): JsonObject = buildJsonObject {
        put("user", user)
        put("segments", buildJsonArray {
            for (segment in segments) {
                add(encodeSegment(segment))
            }
        })
    }

    private fun encodeSegment(segment: ChatPostSegment): JsonObject = buildJsonObject {
        put("id", segment.id)
        segment.text?.let { put("text", it) }
        segment.url?.let { put("url", encodeUrl(it)) }
        segment.bigGifUrl?.let { put("bigGifUrl", encodeUrl(it)) }
    }

    private fun encodeUrl(url: ChatPostUrl): JsonObject = buildJsonObject {
        url.moving?.let { put("moving", it) }
        url.still?.let { put("still", it) }
    }
}

private sealed class MessageToMoblin {
    object Ping : MessageToMoblin()
    data class Publish(val message: PublishMessage) : MessageToMoblin()
    data class Subscribe(val topic: SubscribeTopic) : MessageToMoblin()

    companion object {
        fun fromJson(data: String): MessageToMoblin {
            val json = codableJson.parseToJsonElement(data) as? JsonObject
                ?: throw IllegalArgumentException("Not a UTF-8 string")
            if (json.containsKey("ping")) {
                return Ping
            }
            (json["publish"] as? JsonObject)?.let { obj ->
                val message = obj["message"] as? JsonObject
                    ?: throw IllegalArgumentException("Missing message")
                return Publish(PublishMessage.decode(message))
            }
            (json["subscribe"] as? JsonObject)?.let { obj ->
                val topic = obj["topic"] as? JsonObject
                    ?: throw IllegalArgumentException("Missing topic")
                return Subscribe(SubscribeTopic.decode(topic))
            }
            throw IllegalArgumentException("Invalid message")
        }
    }
}

private sealed class MessageToBrowser {
    data class Message(val data: com.moblin.android.videoeffects.browser.Message) : MessageToBrowser()

    fun encode(): JsonObject = when (this) {
        is Message -> buildJsonObject {
            put("message", buildJsonObject { put("data", data.encode()) })
        }
    }

    fun toJson(): String? {
        return runCatching {
            codableJson.encodeToString(JsonObject.serializer(), encode())
        }.getOrNull()
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

open class BrowserEffectServer(
    configuration: WKWebViewConfiguration,
    private val moblinAccess: Boolean,
) : WKScriptMessageHandler {
    open var webView: WKWebView? = null
    private val subscriptions = Subscriptions()
    private val pingTimer = MainTimer()
    private var gotPing = true
    open var delegate: BrowserEffectServerDelegate? = null

    init {
        configuration.userContentController.addUserScript(
            WKUserScript(
                source = moblinScript(),
                injectionTime = WKUserScriptInjectionTime.atDocumentStart,
                forMainFrameOnly = false,
            )
        )
        configuration.userContentController.add(this, name = "moblin")
    }

    open fun enable() {
        val (weakSelf, timer) = java.lang.ref.WeakReference(this) to pingTimer
        pingTimer.startPeriodic(interval = 5.0) {
            weakSelf.get()?.handlePingTimer() ?: timer.stop()
        }
    }

    open fun disable() {
        pingTimer.stop()
    }

    open fun sendChatMessage(post: ChatPost) {
        val chat = subscriptions.chat ?: return
        val prefix = chat.prefix
        if (prefix != null) {
            val text = post.segments.firstOrNull()?.text
            if (text == null || !text.startsWith(prefix)) {
                return
            }
        }
        send(MessageToBrowser.Message(data = Message.Chat(message = ChatMessage(post))))
    }

    open fun sendSpeechToText(position: Int, text: String) {
        if (!subscriptions.speechToText) {
            return
        }
        send(MessageToBrowser.Message(data = Message.SpeechToText(position = position, text = text)))
    }

    open fun sendSpeechToTextClear() {
        if (!subscriptions.speechToText) {
            return
        }
        send(MessageToBrowser.Message(data = Message.SpeechToTextClear))
    }

    private fun handlePingTimer() {
        if (!gotPing) {
            Log.i("BrowserEffectServer", "browser-effect-server: Ping timeout")
            delegate?.browserEffectServerVideoEnded()
        }
        gotPing = false
    }

    private fun send(message: MessageToBrowser) {
        val json = message.toJson() ?: return
        val data = Base64.getEncoder().encodeToString(json.toByteArray(Charsets.UTF_8))
        webView?.evaluateJavaScript("\nmoblin.handleMessage(\"$data\")\n")
    }

    private fun handleMessage(message: String) {
        try {
            when (val parsed = MessageToMoblin.fromJson(message)) {
                MessageToMoblin.Ping -> handlePing()
                is MessageToMoblin.Publish -> handlePublish(parsed.message)
                is MessageToMoblin.Subscribe -> handleSubscribe(parsed.topic)
            }
        } catch (error: Exception) {
            Log.i("BrowserEffectServer", "browser-effect-server: Decode failed with error: $error")
        }
    }

    private fun handlePing() {
        gotPing = true
    }

    private fun handlePublish(message: PublishMessage) {
        when (message) {
            is PublishMessage.VideoPlaying -> {
                Log.d("BrowserEffectServer", "browser-effect-server: Got video playing: ${message.value}")
                if (message.value) {
                    delegate?.browserEffectServerVideoPlaying()
                } else {
                    delegate?.browserEffectServerVideoEnded()
                }
            }
            is PublishMessage.Log -> {
                Log.i("BrowserEffectServer", "browser-effect-server: Log ${message.message}")
            }
        }
    }

    private fun handleSubscribe(topic: SubscribeTopic) {
        if (!moblinAccess) {
            return
        }
        when (topic) {
            is SubscribeTopic.Chat -> subscriptions.chat = Chat(topic.prefix)
            SubscribeTopic.SpeechToText -> subscriptions.speechToText = true
        }
    }

    override fun userContentController(userContentController: WKUserContentController, didReceive: WKScriptMessage) {
        val message = didReceive.body as? String
        if (message == null) {
            Log.i("BrowserEffectServer", "browser-effect-server: Not a string message")
            return
        }
        CoroutineScope(Dispatchers.Main.immediate).launch {
            handleMessage(message)
        }
    }
}

package com.moblin.android.streamingplatforms.openstreamingplatform

import android.util.Log
import com.moblin.android.common.various.RgbColor
import com.moblin.android.various.ChatPostSegment
import com.moblin.android.various.makeChatPostTextSegments
import com.moblin.android.various.model.Model
import com.moblin.android.various.utils.randomString
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.Base64
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.w3c.dom.Element
import org.w3c.dom.Node

private data class Message(
    val from: String,
    val body: String,
    val color: String?,
) {
    fun user(): String? {
        val slashIndex = from.indexOf('/')
        if (slashIndex == -1) {
            return null
        }
        return from.substring(slashIndex + 1)
    }
}

private data class MessageContainer(val message: Message)

private data class Open(
    val xmlns: String,
    val to: String?,
    val from: String?,
    val version: String,
    val id: String?,
) : XmlNode {
    override fun toXml(root: String): String {
        val builder = StringBuilder()
        builder.append("<").append(root)
        builder.append(" xmlns=\"").append(xmlEscape(xmlns)).append("\"")
        to?.let { builder.append(" to=\"").append(xmlEscape(it)).append("\"") }
        from?.let { builder.append(" from=\"").append(xmlEscape(it)).append("\"") }
        builder.append(" version=\"").append(xmlEscape(version)).append("\"")
        id?.let { builder.append(" id=\"").append(xmlEscape(it)).append("\"") }
        builder.append("/>")
        return builder.toString()
    }
}

private data class OpenContainer(val open: Open)

private class Success

private class SuccessContainer(val success: Success)

private data class IqBind(var jid: String)

private data class Iq(var bind: IqBind)

private data class IqContainer(val iq: Iq)

private data class Auth(
    val xmlns: String,
    val mechanism: String,
    val value: String,
) : XmlNode {
    override fun toXml(root: String): String =
        "<$root xmlns=\"${xmlEscape(xmlns)}\" mechanism=\"${xmlEscape(mechanism)}\">${xmlEscape(value)}</$root>"
}

private data class Mechanisms(val mechanism: List<String>)

private data class Features(val mechanisms: Mechanisms?)

private data class FeaturesContainer(val features: Features)

class OpenStreamingPlatformChat(
    private var model: Model,
    private val url: String,
    private val channelId: String,
) {
    private var task: Job? = null
    private var connected: Boolean = false
    private var webSocket: WebSocket? = null
    private val username: String = "moblin"
    private val password: String = randomString()
    private var authenticated: Boolean = false
    private var jid: String = ""
    private val client: OkHttpClient = OkHttpClient()
    private var messageChannel: Channel<String> = Channel(Channel.UNLIMITED)
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)

    fun start() {
        stop()
        Log.d(TAG, "open-streaming-platform: start")
        task = scope.launch {
            while (true) {
                try {
                    setupConnection()
                    sendOpen()
                    receiveMessages()
                } catch (e: Exception) {
                    Log.d(TAG, "open-streaming-platform: error: $e")
                }
                if (!isActive) {
                    Log.d(TAG, "open-streaming-platform: Cancelled")
                    connected = false
                    break
                }
                Log.d(TAG, "open-streaming-platform: Disconnected")
                connected = false
                delay(5_000)
                Log.d(TAG, "open-streaming-platform: Reconnecting")
            }
        }
    }

    fun stop() {
        Log.d(TAG, "open-streaming-platform: stop")
        webSocket?.cancel()
        task?.cancel()
        task = null
    }

    fun isConnected(): Boolean {
        return connected
    }

    fun hasEmotes(): Boolean {
        return true
    }

    private suspend fun setupConnection() {
        authenticated = false
        val httpUrl = url.toHttpUrlOrNull()
            ?: throw IllegalArgumentException("Failed to create URL")
        Log.d(TAG, "open-streaming-platform: URL $httpUrl")
        messageChannel = Channel(Channel.UNLIMITED)
        val channel = messageChannel
        val request = Request.Builder()
            .url(httpUrl)
            .header("Sec-WebSocket-Protocol", "xmpp")
            .build()
        webSocket = client.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onMessage(webSocket: WebSocket, text: String) {
                    channel.trySend(text)
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    channel.close()
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    channel.close()
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.d(TAG, "open-streaming-platform: failure: $t")
                    channel.close()
                }
            },
        )
    }

    private suspend fun receiveMessages() {
        while (true) {
            val message = messageChannel.receive()
            if (!coroutineContext.isActive) {
                break
            }
            handleMessage(message)
        }
    }

    private suspend fun handleMessage(message: String) {
        Log.d(TAG, "open-streaming-platform: Got string $message")
        val data = "<container>$message</container>"
        runCatching { decodeMessageContainer(data) }.getOrNull()?.let {
            handleMessageMessage(it.message)
            return
        }
        runCatching { decodeOpenContainer(data) }.getOrNull()?.let {
            handleMessageOpen(it.open)
            return
        }
        runCatching { decodeSuccessContainer(data) }.getOrNull()?.let {
            handleMessageSuccess()
            return
        }
        runCatching { decodeIqContainer(data) }.getOrNull()?.let {
            handleMessageIq(it.iq)
            return
        }
        runCatching { decodeFeaturesContainer(data) }.getOrNull()?.let {
            handleMessageFeatures(it.features)
            return
        }
        Log.d(TAG, "open-streaming-platform: Ignoring message $message")
    }

    private suspend fun handleMessageOpen(message: Open) {
        Log.d(TAG, "open-streaming-platform: handle open")
    }

    private suspend fun handleMessageMessage(message: Message) {
        val segments = createSegments(message.body)
        val user = message.user() ?: "unknown"
        Unit
    }

    private suspend fun handleMessageIq(message: Iq) {
        jid = message.bind.jid
        Log.d(TAG, "open-streaming-platform: Got JID $jid")
        sendPresence()
    }

    private suspend fun handleMessageSuccess() {
        Log.d(TAG, "open-streaming-platform: handle success")
        authenticated = true
        connected = true
        sendOpen()
    }

    private suspend fun handleMessageFeatures(message: Features) {
        Log.d(TAG, "open-streaming-platform: handle features")
        if (authenticated) {
            sendString(
                """<iq id="_bind_auth_2" type="set" xmlns="jabber:client"><bind xmlns="urn:ietf:params:xml:ns:xmpp-bind"/></iq>""",
            )
            sendString(
                """<iq id="_session_auth_2" type="set" xmlns="jabber:client"><session xmlns="urn:ietf:params:xml:ns:xmpp-session"/></iq>""",
            )
        } else {
            val value = packPlainAuth() ?: return
            sendAuth(value = value, mechanism = "PLAIN")
        }
    }

    private suspend fun sendOpen() {
        send(
            root = "open",
            data = Open(
                xmlns = "urn:ietf:params:xml:ns:xmpp-framing",
                to = "osp.internal",
                from = null,
                version = "1.0",
                id = null,
            ),
        )
    }

    private suspend fun sendPresence() {
        sendString(
            """
            <presence
               from="$jid"
               to="$channelId@conference.osp.internal/$username"
               xmlns="jabber:client">
              <x xmlns="http://jabber.org/protocol/muc"/>
            </presence>
            """.trimIndent(),
        )
    }

    private suspend fun sendAuth(value: String, mechanism: String) {
        send(
            root = "auth",
            data = Auth(
                xmlns = "urn:ietf:params:xml:ns:xmpp-sasl",
                mechanism = mechanism,
                value = value,
            ),
        )
    }

    private suspend fun send(root: String, data: XmlNode) {
        val message = data.toXml(root)
        sendString(message)
    }

    private suspend fun sendString(message: String) {
        val socket = webSocket ?: throw IllegalStateException("WebSocket not connected")
        if (!socket.send(message)) {
            throw IllegalStateException("Failed to send message")
        }
    }

    private fun packPlainAuth(): String? {
        val data = ByteArrayOutputStream()
        data.write(0)
        data.write(username.toByteArray(Charsets.UTF_8))
        data.write(0)
        data.write(password.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(data.toByteArray())
    }

    private fun createSegments(message: String): List<ChatPostSegment> {
        val id = 0
        return makeChatPostTextSegments(message, id).first
    }

    companion object {
        private const val TAG = "OpenStreamingPlatformChat"
    }
}

private interface XmlNode {
    fun toXml(root: String): String
}

private fun xmlEscape(value: String): String {
    val builder = StringBuilder()
    for (c in value) {
        when (c) {
            '&' -> builder.append("&amp;")
            '<' -> builder.append("&lt;")
            '>' -> builder.append("&gt;")
            '"' -> builder.append("&quot;")
            '\'' -> builder.append("&apos;")
            else -> builder.append(c)
        }
    }
    return builder.toString()
}

private fun parseXml(xml: String): Element {
    val factory = DocumentBuilderFactory.newInstance()
    val builder = factory.newDocumentBuilder()
    return builder.parse(xml.byteInputStream(Charsets.UTF_8)).documentElement
}

private fun Element.childElement(name: String): Element? {
    val nodes = childNodes
    for (i in 0 until nodes.length) {
        val node = nodes.item(i)
        if (node.nodeType == Node.ELEMENT_NODE && node.nodeName == name) {
            return node as Element
        }
    }
    return null
}

private fun xmlValue(element: Element, name: String): String? {
    if (element.hasAttribute(name)) {
        return element.getAttribute(name)
    }
    return element.childElement(name)?.textContent
}

private fun decodeMessageContainer(xml: String): MessageContainer {
    val root = parseXml(xml).childElement("message")
        ?: throw IllegalArgumentException("No message element")
    val from = xmlValue(root, "from")
        ?: throw IllegalArgumentException("No from")
    val body = xmlValue(root, "body")
        ?: throw IllegalArgumentException("No body")
    val color = xmlValue(root, "color")
    return MessageContainer(Message(from = from, body = body, color = color))
}

private fun decodeOpenContainer(xml: String): OpenContainer {
    val root = parseXml(xml).childElement("open")
        ?: throw IllegalArgumentException("No open element")
    return OpenContainer(
        Open(
            xmlns = xmlValue(root, "xmlns") ?: "",
            to = xmlValue(root, "to"),
            from = xmlValue(root, "from"),
            version = xmlValue(root, "version") ?: "",
            id = xmlValue(root, "id"),
        ),
    )
}

private fun decodeSuccessContainer(xml: String): SuccessContainer {
    if (parseXml(xml).childElement("success") == null) {
        throw IllegalArgumentException("No success element")
    }
    return SuccessContainer(Success())
}

private fun decodeIqContainer(xml: String): IqContainer {
    val root = parseXml(xml).childElement("iq")
        ?: throw IllegalArgumentException("No iq element")
    val bind = root.childElement("bind")
        ?: throw IllegalArgumentException("No bind element")
    val jid = bind.childElement("jid")?.textContent ?: ""
    return IqContainer(Iq(IqBind(jid = jid)))
}

private fun decodeFeaturesContainer(xml: String): FeaturesContainer {
    val root = parseXml(xml).childElement("features")
        ?: throw IllegalArgumentException("No features element")
    val mechanismsElement = root.childElement("mechanisms")
    val mechanisms = mechanismsElement?.let { element ->
        val list = element.getElementsByTagName("mechanism")
        Mechanisms((0 until list.length).map { list.item(it).textContent ?: "" })
    }
    return FeaturesContainer(Features(mechanisms = mechanisms))
}

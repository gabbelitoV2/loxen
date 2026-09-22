package com.moblin.android.remotecontrol

import android.content.Context
import android.util.Log
import com.moblin.android.various.network.WebSocketClient
import com.moblin.android.various.network.WebSocketClientDelegate
import java.net.URI
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
private data class MessageType(val type: String)

@Serializable
private data class MessageConnectData(val connectionId: String)

@Serializable
private data class MessageConnect(val data: MessageConnectData)

private class Connection(
    private val context: Context,
    private val baseUrl: String,
    private val bridgeId: String,
    private val connectionId: String,
    private val assistantUrl: String
) : WebSocketClientDelegate {
    private var relayDataWebsocket: WebSocketClient = WebSocketClient(context, "wss://foo.bar")
    private var assistantWebsocket: WebSocketClient = WebSocketClient(context, assistantUrl)

    fun setupRelayDataWebsocket() {
        val url = "$baseUrl/bridge/data/$bridgeId/$connectionId"
        relayDataWebsocket = WebSocketClient(context, url)
        relayDataWebsocket.delegate = this
        relayDataWebsocket.start()
    }

    fun close() {
        relayDataWebsocket.stop()
        assistantWebsocket.stop()
    }

    private fun setupAssistantWebsocket() {
        assistantWebsocket = WebSocketClient(context, assistantUrl, loopback = true)
        assistantWebsocket.delegate = this
        assistantWebsocket.start()
    }

    override fun webSocketClientConnected(webSocket: WebSocketClient) {
        if (webSocket === relayDataWebsocket) {
            setupAssistantWebsocket()
        }
    }

    override fun webSocketClientDisconnected(webSocket: WebSocketClient) {
        close()
    }

    override fun webSocketClientReceiveMessage(webSocket: WebSocketClient, string: String) {
        if (webSocket === relayDataWebsocket) {
            assistantWebsocket.send(string)
        } else if (webSocket === assistantWebsocket) {
            relayDataWebsocket.send(string)
        }
    }
}

class RemoteControlRelay private constructor(
    private val context: Context,
    private val baseUrl: String,
    private val bridgeId: String,
    private val assistantUrl: String,
    private val controlUrl: String
) : WebSocketClientDelegate {
    private var controlWebsocket: WebSocketClient = WebSocketClient(context, controlUrl)
    private val connections: ArrayDeque<Connection> = ArrayDeque()

    fun start() {
        stop()
        controlWebsocket = WebSocketClient(context, controlUrl)
        controlWebsocket.delegate = this
        controlWebsocket.start()
    }

    fun stop() {
        controlWebsocket.stop()
        for (connection in connections) {
            connection.close()
        }
    }

    private fun handleControlMessage(message: String) {
        val decoded = Json.decodeFromString<MessageType>(message)
        when (decoded.type) {
            "connect" -> handleControlMessageConnect(message)
            else -> {}
        }
    }

    private fun handleControlMessageConnect(message: String) {
        val decoded = Json.decodeFromString<MessageConnect>(message)
        val connection = Connection(
            context = context,
            baseUrl = baseUrl,
            bridgeId = bridgeId,
            connectionId = decoded.data.connectionId,
            assistantUrl = assistantUrl
        )
        connection.setupRelayDataWebsocket()
        connections.addLast(connection)
        if (connections.size > 5) {
            connections.removeFirstOrNull()?.close()
        }
    }

    override fun webSocketClientConnected(webSocket: WebSocketClient) {
        Log.i("RemoteControlRelay", "remote-control-relay: Control connected.")
    }

    override fun webSocketClientDisconnected(webSocket: WebSocketClient) {
        Log.i("RemoteControlRelay", "remote-control-relay: Control disconnected.")
        for (connection in connections) {
            connection.close()
        }
    }

    override fun webSocketClientReceiveMessage(webSocket: WebSocketClient, string: String) {
        try {
            handleControlMessage(string)
        } catch (e: Exception) {
            Log.d("RemoteControlRelay", "remote-control-relay: Control error $e")
        }
    }

    companion object {
        operator fun invoke(context: Context, baseUrl: String, bridgeId: String, assistantUrl: String): RemoteControlRelay? {
            val controlUrl = "$baseUrl/bridge/control/$bridgeId"
            return runCatching { URI(controlUrl) }.getOrNull()?.let {
                RemoteControlRelay(context, baseUrl, bridgeId, assistantUrl, controlUrl)
            }
        }
    }
}

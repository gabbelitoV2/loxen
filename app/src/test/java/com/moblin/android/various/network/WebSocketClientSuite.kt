package com.moblin.android.various.network

import com.moblin.android.AppDelegate
import com.moblin.android.MessageQueue
import com.moblin.android.platform.core.ContinuousClock
import com.moblin.android.platform.network.NWConnection
import com.moblin.android.platform.network.NWEndpoint
import com.moblin.android.platform.network.NWError
import com.moblin.android.platform.network.NWListener
import com.moblin.android.platform.network.NWParameters
import com.moblin.android.platform.network.NWProtocolWebSocket
import com.moblin.android.runMainTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private sealed class ClientEvent {
    data object Connected : ClientEvent()
    data object Disconnected : ClientEvent()
    data class Message(val value: String) : ClientEvent()
}

private sealed class ServerEvent {
    data class Text(val value: String) : ServerEvent()
    data class Close(val code: NWProtocolWebSocket.CloseCode) : ServerEvent()
    data object Disconnected : ServerEvent()
}

private sealed class ReadyResult {
    data class Success(val port: Int) : ReadyResult()
    data class Failure(val error: NWError) : ReadyResult()

    fun get(): Int = when (this) {
        is Success -> port
        is Failure -> throw error
    }
}

private class Delegate : WebSocketClientDelegate {
    val events = MessageQueue<ClientEvent>()
    var disconnections = 0
        private set
    var messages: MutableList<String> = mutableListOf()
        private set

    override fun webSocketClientConnected(webSocket: WebSocketClient) {
        events.put(ClientEvent.Connected)
    }

    override fun webSocketClientDisconnected(webSocket: WebSocketClient) {
        disconnections += 1
        events.put(ClientEvent.Disconnected)
    }

    override fun webSocketClientReceiveMessage(webSocket: WebSocketClient, string: String) {
        messages.add(string)
        events.put(ClientEvent.Message(string))
    }
}

private class WebSocketServerMock(private var port: Int? = null) {
    private val listener: NWListener
    private val ready = MessageQueue<ReadyResult>()
    private val connections = MessageQueue<NWConnection>()
    val events = MessageQueue<ServerEvent>()
    var connectionCount = 0
        private set
    private var connection: NWConnection? = null

    init {
        val parameters = NWParameters.tcp
        parameters.allowLocalEndpointReuse = true
        val options = NWProtocolWebSocket.Options()
        options.autoReplyPing = true
        insertApplicationProtocol(parameters, options)
        val listenerPort = port
        listener = if (listenerPort != null) {
            NWListener(using = parameters, on = NWEndpoint.Port(listenerPort))
        } else {
            NWListener(using = parameters)
        }
        listener.stateUpdateHandler = { state ->
            CoroutineScope(Dispatchers.Main.immediate).launch {
                handleListenerState(state)
            }
        }
        listener.newConnectionHandler = { connection ->
            CoroutineScope(Dispatchers.Main.immediate).launch {
                handleNewConnection(connection)
            }
        }
        listener.start(queue = Dispatchers.Main)
    }

    suspend fun url(): String {
        if (port == null) {
            port = ready.get().get()
        }
        return "ws://127.0.0.1:$port"
    }

    suspend fun expectEvent(expected: ServerEvent) {
        val event = events.get()
        assertEquals(expected, event)
    }

    suspend fun acceptConnection() {
        connection = connections.get()
    }

    fun disconnect() {
        connection?.cancel()
        connection = null
    }

    fun send(text: String) {
        Unit
    }

    fun close(code: NWProtocolWebSocket.CloseCode) {
        Unit
    }

    private fun send(content: ByteArray?, metadata: Any?) {
        Unit
    }

    private fun handleListenerState(state: NWListener.State) {
        when (state) {
            NWListener.State.ready -> ready.put(ReadyResult.Success(listener.port!!.rawValue.toInt()))
            is NWListener.State.waiting -> ready.put(ReadyResult.Failure(state.error))
            is NWListener.State.failed -> ready.put(ReadyResult.Failure(state.error))
            else -> Unit
        }
    }

    private fun handleNewConnection(connection: NWConnection) {
        connection.stateUpdateHandler = { state ->
            CoroutineScope(Dispatchers.Main.immediate).launch {
                if (state == NWConnection.State.ready) {
                    connectionCount += 1
                    connections.put(connection)
                    receive(connection)
                }
            }
        }
        connection.start(queue = Dispatchers.Main)
    }

    private fun receive(connection: NWConnection) {
        connection.receiveMessage { content, context, _, error ->
            CoroutineScope(Dispatchers.Main.immediate).launch {
                Unit
            }
        }
    }

    private fun insertApplicationProtocol(parameters: NWParameters, options: NWProtocolWebSocket.Options) {
        Unit
    }
}

private data class Connection(
    val server: WebSocketServerMock,
    val delegate: Delegate,
    val client: WebSocketClient,
) {
    suspend fun expectClientEvent(expected: ClientEvent) {
        val event = delegate.events.get()
        assertEquals(expected, event)
    }

    suspend fun expectServerEvent(expected: ServerEvent) {
        server.expectEvent(expected)
    }

    companion object {
        suspend fun make(): Connection {
            val server = WebSocketServerMock()
            val delegate = Delegate()
            val client = WebSocketClient(context = AppDelegate.context, url = server.url(), loopback = true)
            client.delegate = delegate
            client.start()
            return Connection(server = server, delegate = delegate, client = client)
        }

        suspend fun makeConnected(): Connection {
            val connection = make()
            connection.server.acceptConnection()
            connection.expectClientEvent(ClientEvent.Connected)
            return connection
        }
    }
}

@RunWith(RobolectricTestRunner::class)
class WebSocketClientSuite {
    @Test
    fun connectSendAndReceive() = runMainTest {
        val connection = Connection.make()
        assertFalse(connection.client.isConnected())
        connection.server.acceptConnection()
        connection.expectClientEvent(ClientEvent.Connected)
        assertTrue(connection.client.isConnected())
        connection.client.send(string = "hello")
        connection.expectServerEvent(ServerEvent.Text("hello"))
        connection.server.send(text = "world")
        connection.expectClientEvent(ClientEvent.Message("world"))
        connection.client.stop()
    }

    @Test
    fun closeWithHandshake() = runMainTest {
        val connection = Connection.makeConnected()
        connection.client.stop()
        assertFalse(connection.client.isConnected())
        connection.expectServerEvent(ServerEvent.Close(NWProtocolWebSocket.CloseCode.privateCode(4001)))
        connection.server.close(code = NWProtocolWebSocket.CloseCode.privateCode(4001))
        connection.expectServerEvent(ServerEvent.Disconnected)
        delay(1000)
        assertEquals(0, connection.delegate.disconnections)
        assertEquals(1, connection.server.connectionCount)
    }

    @Test
    fun closeWithDefaultCloseCode() = runMainTest {
        val connection = Connection.makeConnected()
        connection.client.stop()
        connection.expectServerEvent(
            ServerEvent.Close(
                NWProtocolWebSocket.CloseCode.protocolCode(NWProtocolWebSocket.CloseCode.Defined.normalClosure),
            ),
        )
        connection.server.close(
            code = NWProtocolWebSocket.CloseCode.protocolCode(NWProtocolWebSocket.CloseCode.Defined.normalClosure),
        )
        connection.expectServerEvent(ServerEvent.Disconnected)
    }

    @Test
    fun closeWithoutPeerResponse() = runMainTest {
        val connection = Connection.makeConnected()
        connection.client.stop()
        connection.expectServerEvent(ServerEvent.Close(NWProtocolWebSocket.CloseCode.privateCode(4002)))
        connection.expectServerEvent(ServerEvent.Disconnected)
        assertEquals(0, connection.delegate.disconnections)
    }

    @Test
    fun messagesAreDroppedWhileClosing() = runMainTest {
        val connection = Connection.makeConnected()
        connection.client.stop()
        connection.server.send(text = "late")
        connection.expectServerEvent(
            ServerEvent.Close(
                NWProtocolWebSocket.CloseCode.protocolCode(NWProtocolWebSocket.CloseCode.Defined.normalClosure),
            ),
        )
        connection.server.close(
            code = NWProtocolWebSocket.CloseCode.protocolCode(NWProtocolWebSocket.CloseCode.Defined.normalClosure),
        )
        connection.expectServerEvent(ServerEvent.Disconnected)
        assertTrue(connection.delegate.messages.isEmpty())
    }

    @Test
    fun closeWhenClientIsDeallocated() = runMainTest {
        val server = WebSocketServerMock()
        val delegate = Delegate()
        var client: WebSocketClient? = WebSocketClient(
            context = AppDelegate.context,
            url = server.url(),
            loopback = true,
        )
        client?.delegate = delegate
        client?.start()
        server.acceptConnection()
        val event = delegate.events.get()
        assertEquals(ClientEvent.Connected, event)
        client?.stop()
        client = null
        server.expectEvent(ServerEvent.Close(NWProtocolWebSocket.CloseCode.privateCode(4003)))
        server.close(code = NWProtocolWebSocket.CloseCode.privateCode(4003))
        server.expectEvent(ServerEvent.Disconnected)
    }

    @Test
    fun closeWithoutPeerResponseWhenClientIsDeallocated() = runMainTest {
        val server = WebSocketServerMock()
        val delegate = Delegate()
        var client: WebSocketClient? = WebSocketClient(
            context = AppDelegate.context,
            url = server.url(),
            loopback = true,
        )
        client?.delegate = delegate
        client?.start()
        server.acceptConnection()
        val event = delegate.events.get()
        assertEquals(ClientEvent.Connected, event)
        client?.stop()
        client = null
        server.expectEvent(ServerEvent.Close(NWProtocolWebSocket.CloseCode.privateCode(4004)))
        server.expectEvent(ServerEvent.Disconnected)
    }

    @Test
    fun closeWhenClientIsDeallocatedWhileReceiving() = runMainTest {
        val server = WebSocketServerMock()
        val delegate = Delegate()
        var client: WebSocketClient? = WebSocketClient(
            context = AppDelegate.context,
            url = server.url(),
            loopback = true,
        )
        client?.delegate = delegate
        client?.start()
        server.acceptConnection()
        val event = delegate.events.get()
        assertEquals(ClientEvent.Connected, event)
        server.send(text = "late")
        client?.stop()
        client = null
        server.expectEvent(ServerEvent.Close(NWProtocolWebSocket.CloseCode.privateCode(4005)))
        server.close(code = NWProtocolWebSocket.CloseCode.privateCode(4005))
        server.expectEvent(ServerEvent.Disconnected)
    }

    @Test
    fun sendIsIgnoredWhileClosing() = runMainTest {
        val connection = Connection.makeConnected()
        connection.client.stop()
        connection.client.send(string = "ignored")
        connection.expectServerEvent(
            ServerEvent.Close(
                NWProtocolWebSocket.CloseCode.protocolCode(NWProtocolWebSocket.CloseCode.Defined.normalClosure),
            ),
        )
        connection.server.close(
            code = NWProtocolWebSocket.CloseCode.protocolCode(NWProtocolWebSocket.CloseCode.Defined.normalClosure),
        )
        connection.expectServerEvent(ServerEvent.Disconnected)
    }

    @Test
    fun reconnectAfterServerClose() = runMainTest {
        val connection = Connection.makeConnected()
        connection.server.close(code = NWProtocolWebSocket.CloseCode.privateCode(4000))
        connection.expectClientEvent(ClientEvent.Disconnected)
        assertFalse(connection.client.isConnected())
        connection.server.acceptConnection()
        connection.expectClientEvent(ClientEvent.Connected)
        assertTrue(connection.client.isConnected())
        connection.client.stop()
    }

    @Test
    fun reconnectAfterServerDisconnect() = runMainTest {
        val connection = Connection.makeConnected()
        val disconnectTime = ContinuousClock.now
        connection.server.disconnect()
        connection.expectClientEvent(ClientEvent.Disconnected)
        connection.server.acceptConnection()
        connection.expectClientEvent(ClientEvent.Connected)
        assertTrue(disconnectTime.duration(to = ContinuousClock.now) >= 400.milliseconds)
        connection.client.stop()
    }

    @Test
    fun reconnectWhenServerStartsLater() = runMainTest {
        val delegate = Delegate()
        val client = WebSocketClient(
            context = AppDelegate.context,
            url = "ws://127.0.0.1:24680",
            loopback = true,
        )
        client.delegate = delegate
        client.start()
        delay(700)
        assertFalse(client.isConnected())
        val server = WebSocketServerMock(port = 24680)
        server.url()
        server.acceptConnection()
        val event = delegate.events.get()
        assertEquals(ClientEvent.Connected, event)
        assertTrue(client.isConnected())
        client.stop()
    }

    @Test
    fun restart() = runMainTest {
        val connection = Connection.makeConnected()
        connection.client.stop()
        assertFalse(connection.client.isConnected())
        connection.client.start()
        connection.server.acceptConnection()
        connection.expectClientEvent(ClientEvent.Connected)
        assertTrue(connection.client.isConnected())
        connection.client.stop()
    }
}

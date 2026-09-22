package com.moblin.android.obs

import android.util.Log
import com.moblin.android.MessageQueue
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import java.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject

data class ObsMockIdentify(
    val rpcVersion: Int,
    val authentication: String?,
)

data class ObsMockRequest(
    val type: String,
    val id: String,
    val data: String?,
)

data class ObsMockRequestBatch(
    val id: String,
    val requests: List<ObsMockRequest>,
)

sealed class ObsMockResult {
    data class Success(val data: String? = null) : ObsMockResult()

    data class Failure(val code: Int, val comment: String? = null) : ObsMockResult()
}

private sealed class ObsMockMessage {
    data class Identify(val identify: ObsMockIdentify) : ObsMockMessage()

    data class Reidentify(val eventSubscriptions: ULong) : ObsMockMessage()

    data class Request(val request: ObsMockRequest) : ObsMockMessage()

    data class RequestBatch(val batch: ObsMockRequestBatch) : ObsMockMessage()
}

private class ObsMockException(message: String) : Exception(message)

private class WsFrame(val opcode: Int, val payload: ByteArray)

private const val TAG = "ObsWebSocketServerMock"

private const val WEB_SOCKET_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"

private val json = Json

private fun JsonElement?.asStringOrNull(): String? = (this as? JsonPrimitive)?.contentOrNull

private fun JsonElement?.asIntOrNull(): Int? = (this as? JsonPrimitive)?.intOrNull

private fun JsonElement?.asULongOrNull(): ULong? = asStringOrNull()?.toULongOrNull()

private fun jsonString(value: JsonElement): String =
    json.encodeToString(JsonElement.serializer(), sortJsonKeys(value))

private fun sortJsonKeys(element: JsonElement): JsonElement = when (element) {
    is JsonObject -> JsonObject(
        element.entries.sortedBy { it.key }.associate { it.key to sortJsonKeys(it.value) }
    )

    is JsonArray -> JsonArray(element.map(::sortJsonKeys))

    else -> element
}

class ObsWebSocketServerMock(private val password: String? = null) {
    private val salt = "lM1GncleQOaCu9vsLMhnvT94j+pSHfs+aGZk0+3M8Xw="
    private val challenge = "+IxH4CnCiqpX1rM9scsNynZzbOe4KhDeYcTNS3PDaeY="
    private val ready = MessageQueue<UShort>()
    private val connections = MessageQueue<Socket>()
    private val messages = MessageQueue<ObsMockMessage>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val serverSocket: ServerSocket = ServerSocket().apply {
        reuseAddress = true
        bind(InetSocketAddress("127.0.0.1", 0))
    }
    private var port: UShort? = null
    private var connection: Socket? = null

    init {
        scope.launch {
            runListener()
        }
    }

    suspend fun url(): String {
        if (port == null) {
            port = ready.get()
        }
        return "ws://127.0.0.1:${port!!}"
    }

    suspend fun connect() {
        acceptConnection()
        sendHello()
        val identify = receiveIdentify()
        if (identify.rpcVersion != 1) {
            throw ObsMockException("Unsupported RPC version ${identify.rpcVersion}")
        }
        if (identify.authentication != expectedAuthentication()) {
            throw ObsMockException("Authentication mismatch")
        }
        sendIdentified()
    }

    suspend fun acceptConnection() {
        connection = connections.get()
    }

    fun disconnect() {
        connection?.let { socket -> runCatching { socket.close() } }
        connection = null
    }

    fun close(code: UShort) {
        val socket = connection ?: return
        val payload = byteArrayOf(
            ((code.toInt() ushr 8) and 0xFF).toByte(),
            (code.toInt() and 0xFF).toByte(),
        )
        try {
            writeFrame(socket.getOutputStream(), 0x8, payload)
        } catch (e: Exception) {
            Log.i(TAG, "obs-mock: Failed to close: $e")
        }
    }

    fun expectedAuthentication(): String? {
        val password = this.password ?: return null
        val secret = sha256Base64("$password$salt")
        return sha256Base64("$secret$challenge")
    }

    fun sendHello() {
        var authentication = ""
        if (password != null) {
            authentication = ",\"authentication\":{\"challenge\":\"$challenge\",\"salt\":\"$salt\"}"
        }
        send(op = 0, data = "{\"obsWebSocketVersion\":\"5.5.2\",\"rpcVersion\":1$authentication}")
    }

    fun sendIdentified() {
        send(op = 2, data = "{\"negotiatedRpcVersion\":1}")
    }

    fun sendEvent(type: String, intent: Int, data: String? = null) {
        var eventData = ""
        if (data != null) {
            eventData = ",\"eventData\":$data"
        }
        send(op = 5, data = "{\"eventType\":\"$type\",\"eventIntent\":$intent$eventData}")
    }

    fun respond(request: ObsMockRequest, data: String? = null) {
        send(op = 7, data = responseJson(request, ObsMockResult.Success(data = data)))
    }

    fun respond(request: ObsMockRequest, errorCode: Int, comment: String? = null) {
        send(op = 7, data = responseJson(request, ObsMockResult.Failure(code = errorCode, comment = comment)))
    }

    fun respond(batch: ObsMockRequestBatch, results: List<ObsMockResult>) {
        val joined = batch.requests.zip(results) { request, result ->
            responseJson(request, result)
        }.joinToString(",")
        send(op = 9, data = "{\"requestId\":${batch.id},\"results\":[$joined]}")
    }

    fun send(op: Int, data: String) {
        send("{\"op\":$op,\"d\":$data}")
    }

    fun send(text: String) {
        val socket = connection ?: return
        try {
            writeFrame(socket.getOutputStream(), 0x1, text.toByteArray(Charsets.UTF_8))
        } catch (e: Exception) {
            Log.i(TAG, "obs-mock: Failed to send: $e")
        }
    }

    suspend fun receiveIdentify(): ObsMockIdentify {
        val message = messages.get()
        if (message !is ObsMockMessage.Identify) {
            throw ObsMockException("Expected identify")
        }
        return message.identify
    }

    suspend fun receiveReidentify(): ULong {
        val message = messages.get()
        if (message !is ObsMockMessage.Reidentify) {
            throw ObsMockException("Expected reidentify")
        }
        return message.eventSubscriptions
    }

    suspend fun receiveRequest(): ObsMockRequest {
        val message = messages.get()
        if (message !is ObsMockMessage.Request) {
            throw ObsMockException("Expected request")
        }
        return message.request
    }

    suspend fun receiveRequestBatch(): ObsMockRequestBatch {
        val message = messages.get()
        if (message !is ObsMockMessage.RequestBatch) {
            throw ObsMockException("Expected request batch")
        }
        return message.batch
    }

    private fun responseJson(request: ObsMockRequest, result: ObsMockResult): String {
        var status = ""
        var responseData = ""
        when (result) {
            is ObsMockResult.Success -> {
                status = "{\"result\":true,\"code\":100}"
                if (result.data != null) {
                    responseData = ",\"responseData\":${result.data}"
                }
            }

            is ObsMockResult.Failure -> {
                var text = "{\"result\":false,\"code\":${result.code}"
                if (result.comment != null) {
                    text += ",\"comment\":\"${result.comment}\""
                }
                text += "}"
                status = text
            }
        }
        return "{\"requestType\":\"${request.type}\",\"requestId\":\"${request.id}\",\"requestStatus\":$status$responseData}"
    }

    private suspend fun handleListenerState() {
        ready.put(serverSocket.localPort.toUShort())
    }

    private fun handleNewConnection(socket: Socket) {
        scope.launch {
            try {
                performHandshake(socket)
            } catch (e: Exception) {
                Log.i(TAG, "obs-mock: Failed to handle connection: $e")
                runCatching { socket.close() }
                return@launch
            }
            connections.put(socket)
            receive(socket)
        }
    }

    private suspend fun receive(socket: Socket) {
        val input = socket.getInputStream()
        while (true) {
            val frame = runCatching { readFrame(input) }.getOrNull() ?: break
            if (frame.opcode == 0x8) {
                break
            }
            if (frame.opcode == 0x1) {
                try {
                    handleMessage(String(frame.payload, Charsets.UTF_8))
                } catch (e: Exception) {
                    Log.i(TAG, "obs-mock: Failed to handle message: $e")
                }
            }
        }
    }

    private suspend fun handleMessage(text: String) {
        val message = Json.parseToJsonElement(text) as? JsonObject
            ?: throw ObsMockException("Malformed message")
        val op = message["op"].asIntOrNull()
            ?: throw ObsMockException("Malformed message")
        val data = message["d"] as? JsonObject
            ?: throw ObsMockException("Malformed message")
        when (op) {
            1 -> {
                val rpcVersion = data["rpcVersion"].asIntOrNull()
                    ?: throw ObsMockException("Missing rpcVersion")
                messages.put(
                    ObsMockMessage.Identify(
                        ObsMockIdentify(
                            rpcVersion = rpcVersion,
                            authentication = data["authentication"].asStringOrNull(),
                        )
                    )
                )
            }

            3 -> {
                val eventSubscriptions = data["eventSubscriptions"].asULongOrNull()
                    ?: throw ObsMockException("Missing eventSubscriptions")
                messages.put(ObsMockMessage.Reidentify(eventSubscriptions = eventSubscriptions))
            }

            6 -> messages.put(ObsMockMessage.Request(parseRequest(data)))

            8 -> {
                val id = data["requestId"]
                    ?: throw ObsMockException("Malformed request batch")
                val requests = data["requests"] as? JsonArray
                    ?: throw ObsMockException("Malformed request batch")
                messages.put(
                    ObsMockMessage.RequestBatch(
                        ObsMockRequestBatch(
                            id = jsonString(id),
                            requests = requests.map { parseRequest(it.jsonObject) },
                        )
                    )
                )
            }

            else -> throw ObsMockException("Unexpected op $op")
        }
    }

    private fun parseRequest(data: JsonObject): ObsMockRequest {
        val type = data["requestType"].asStringOrNull()
            ?: throw ObsMockException("Malformed request")
        val id = data["requestId"].asStringOrNull()
            ?: throw ObsMockException("Malformed request")
        return ObsMockRequest(type = type, id = id, data = data["requestData"]?.let { jsonString(it) })
    }

    private suspend fun runListener() {
        handleListenerState()
        while (scope.isActive) {
            val socket = try {
                serverSocket.accept()
            } catch (e: Exception) {
                break
            }
            handleNewConnection(socket)
        }
    }

    private fun performHandshake(socket: Socket) {
        val input = socket.getInputStream()
        val request = readHttpRequest(input)
        val key = request.lineSequence()
            .mapNotNull { line ->
                val index = line.indexOf(':')
                if (index <= 0) {
                    null
                } else {
                    line.substring(0, index).trim() to line.substring(index + 1).trim()
                }
            }
            .firstOrNull { it.first.equals("Sec-WebSocket-Key", ignoreCase = true) }
            ?.second
            ?: throw IOException("Missing Sec-WebSocket-Key")
        val accept = Base64.getEncoder().encodeToString(
            MessageDigest.getInstance("SHA-1").digest("$key$WEB_SOCKET_GUID".toByteArray(Charsets.US_ASCII))
        )
        val response = "HTTP/1.1 101 Switching Protocols\r\n" +
            "Upgrade: websocket\r\n" +
            "Connection: Upgrade\r\n" +
            "Sec-WebSocket-Accept: $accept\r\n\r\n"
        val output = socket.getOutputStream()
        output.write(response.toByteArray(Charsets.US_ASCII))
        output.flush()
    }

    private fun readHttpRequest(input: InputStream): String {
        val buffer = ByteArrayOutputStream()
        var state = 0
        while (true) {
            val b = input.read()
            if (b == -1) {
                break
            }
            buffer.write(b)
            state = when {
                state == 0 && b == '\r'.code -> 1
                state == 1 && b == '\n'.code -> 2
                state == 2 && b == '\r'.code -> 3
                state == 3 && b == '\n'.code -> 4
                b == '\r'.code -> 1
                else -> 0
            }
            if (state == 4) {
                break
            }
        }
        return buffer.toByteArray().toString(Charsets.US_ASCII)
    }

    private fun readFrame(input: InputStream): WsFrame? {
        val first = input.read()
        if (first == -1) {
            return null
        }
        val second = input.read()
        if (second == -1) {
            return null
        }
        val opcode = first and 0x0F
        val masked = (second and 0x80) != 0
        var length = (second and 0x7F).toLong()
        if (length == 126L) {
            val bytes = ByteArray(2)
            readFully(input, bytes)
            length = (((bytes[0].toInt() and 0xFF) shl 8) or (bytes[1].toInt() and 0xFF)).toLong()
        } else if (length == 127L) {
            val bytes = ByteArray(8)
            readFully(input, bytes)
            length = 0
            for (b in bytes) {
                length = (length shl 8) or (b.toLong() and 0xFF)
            }
        }
        var mask: ByteArray? = null
        if (masked) {
            mask = ByteArray(4)
            readFully(input, mask)
        }
        val payload = ByteArray(length.toInt())
        readFully(input, payload)
        if (mask != null) {
            for (i in payload.indices) {
                payload[i] = (payload[i].toInt() xor mask[i % 4].toInt()).toByte()
            }
        }
        return WsFrame(opcode, payload)
    }

    private fun readFully(input: InputStream, buffer: ByteArray) {
        var offset = 0
        while (offset < buffer.size) {
            val read = input.read(buffer, offset, buffer.size - offset)
            if (read == -1) {
                throw IOException("Unexpected end of stream")
            }
            offset += read
        }
    }

    private fun writeFrame(output: OutputStream, opcode: Int, payload: ByteArray) {
        val header = ByteArrayOutputStream()
        header.write(0x80 or opcode)
        if (payload.size < 126) {
            header.write(payload.size)
        } else if (payload.size <= 0xFFFF) {
            header.write(126)
            header.write((payload.size ushr 8) and 0xFF)
            header.write(payload.size and 0xFF)
        } else {
            header.write(127)
            for (shift in 7 downTo 0) {
                header.write(((payload.size.toLong() ushr (8 * shift)) and 0xFF).toInt())
            }
        }
        output.write(header.toByteArray())
        output.write(payload)
        output.flush()
    }

    private fun sha256Base64(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(digest)
    }
}

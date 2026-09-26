package com.moblin.android.various.network

import com.moblin.android.platform.network.NWConnection
import com.moblin.android.platform.network.NWEndpoint
import com.moblin.android.platform.network.NWInterface
import com.moblin.android.platform.network.NWParameters
import com.moblin.android.various.MainTimer
import kotlinx.coroutines.Dispatchers
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

open class HttpParser {
    open var data: ByteArray = ByteArray(0)

    open fun append(data: ByteArray) {
        this.data += data
    }

    open fun getLine(data: ByteArray, offset: Int): Pair<String, Int>? {
        if (offset < 0 || offset > data.size) {
            return null
        }
        val slice = data.copyOfRange(offset, data.size)
        val rIndex = slice.indexOfFirst { it == 0x0D.toByte() }
        if (rIndex < 0 || slice.size <= rIndex + 1 || slice[rIndex + 1] != 0x0A.toByte()) {
            return null
        }
        val line = decodeUtf8Strict(bytes = slice, offset = 0, length = rIndex) ?: return null
        return Pair(line, offset + rIndex + 2)
    }
}

open class HttpResponseParser : HttpParser() {
    open fun parse(): Pair<Boolean, ByteArray?> {
        var offset = 0
        val status = getLine(data = data, offset = offset) ?: return Pair(false, null)
        offset = status.second
        val statusLine = status.first
        val statusParts = statusLine.split(" ").filter { it.isNotEmpty() }
        if (!statusLine.startsWith("HTTP") || statusParts.size < 3) {
            return Pair(true, null)
        }
        val statusCode = parseIntStrict(statusParts[1])
        if (statusCode == null || statusCode !in 200..299) {
            return Pair(true, null)
        }
        var contentLength = 0
        while (true) {
            val entry = getLine(data = data, offset = offset) ?: break
            val line = entry.first
            val nextLineOffset = entry.second
            val parts = line.lowercase().split(" ").filter { it.isNotEmpty() }
            if (parts.size == 2 && parts.first() == "content-length:") {
                val length = parseIntStrict(parts.last())
                if (length == null || length < 0) {
                    return Pair(true, null)
                }
                contentLength = length
            } else if (line.isEmpty()) {
                val body = data.copyOfRange(nextLineOffset, data.size)
                if (body.size == contentLength) {
                    return Pair(true, body)
                }
            }
            offset = nextLineOffset
        }
        return Pair(false, null)
    }
}

private class InterfaceTypeHttpClient {
    companion object {
        private var interfaceTypes: List<NWInterface.InterfaceType> = listOf(
            NWInterface.InterfaceType.cellular,
            NWInterface.InterfaceType.wifi,
            NWInterface.InterfaceType.wiredEthernet,
        )
    }

    private var interfaceTypes: List<NWInterface.InterfaceType> = emptyList()
    private var interfaceTypeIndex: Int = 0
    private var connection: NWConnection? = null
    private val timer = MainTimer()
    private var completion: ((ByteArray?) -> Unit)? = null
    private var responseParser = HttpResponseParser()

    init {
        this.interfaceTypes = Companion.interfaceTypes
    }

    private fun stop() {
        completion = null
        timer.stop()
        connection?.stateUpdateHandler = null
        connection?.cancel()
        connection = null
    }

    private fun completed(data: ByteArray?) {
        completion?.invoke(data)
        stop()
    }

    fun call(request: Request, body: ByteArray?, completion: (ByteArray?) -> Unit) {
        val created = createRequest(request = request, body = body) ?: run {
            completion(null)
            return
        }
        val (host, port, useTls, content) = created
        val endpoint = NWEndpoint.hostPort(host = NWEndpoint.Host(host), port = NWEndpoint.Port(port))
        this.completion = completion
        timer.startSingleShot(timeout = 60.0) {
            completed(data = null)
        }
        connect(endpoint = endpoint, useTls = useTls) { interfaceTypeIndex ->
            connection?.send(
                content = content,
                completion = NWConnection.SendCompletion.contentProcessed { error ->
                    if (error != null) {
                        completed(data = null)
                    } else {
                        receiveData(interfaceTypeIndex)
                    }
                },
            )
        }
    }

    private fun connect(endpoint: NWEndpoint, useTls: Boolean, onConnected: (Int) -> Unit) {
        if (this.interfaceTypeIndex >= this.interfaceTypes.size) {
            completed(data = null)
            return
        }
        responseParser = HttpResponseParser()
        val interfaceType = this.interfaceTypes[this.interfaceTypeIndex]
        val currentIndex = this.interfaceTypeIndex
        val parameters: NWParameters = if (useTls) NWParameters.tls else NWParameters.tcp
        parameters.requiredInterfaceType = interfaceType
        connection = NWConnection(endpoint = endpoint, parameters = parameters)
        connection?.stateUpdateHandler = { state ->
            if (isCurrentConnection(currentIndex)) {
                when (state) {
                    NWConnection.State.preparing -> {
                    }
                    NWConnection.State.ready -> {
                        updateGlobalInterfaceTypesIfNeeded()
                        onConnected(currentIndex)
                    }
                    else -> {
                        connection?.stateUpdateHandler = null
                        connection?.cancel()
                        this.interfaceTypeIndex += 1
                        connect(endpoint = endpoint, useTls = useTls, onConnected = onConnected)
                    }
                }
            }
        }
        connection?.start(queue = Dispatchers.Main)
    }

    private fun isCurrentConnection(interfaceTypeIndex: Int): Boolean {
        return this.interfaceTypeIndex == interfaceTypeIndex
    }

    private fun updateGlobalInterfaceTypesIfNeeded() {
        if (this.interfaceTypeIndex == 0) {
            return
        }
        val swapped = this.interfaceTypes.toMutableList()
        val first = swapped[0]
        swapped[0] = swapped[this.interfaceTypeIndex]
        swapped[this.interfaceTypeIndex] = first
        Companion.interfaceTypes = swapped
    }

    private fun createRequest(request: Request, body: ByteArray?): CreatedRequest? {
        val url = request.url
        val host = url.host
        val scheme = url.scheme
        val method = request.method
        val useTls = scheme == "https"
        val port = url.port
        var path = url.encodedPath
        if (path.isEmpty()) {
            path = "/"
        }
        val query = url.encodedQuery
        if (query != null) {
            path += "?$query"
        }
        var data = "$method $path HTTP/1.1\r\nHost: $host\r\n"
        for ((name, value) in request.headers) {
            data += "$name: $value\r\n"
        }
        if (body != null) {
            data += "Content-Length: ${body.size}\r\n"
        }
        data += "\r\n"
        var content = data.encodeToByteArray()
        if (body != null) {
            content += body
        }
        return CreatedRequest(host = host, port = port, useTls = useTls, content = content)
    }

    private fun receiveData(interfaceTypeIndex: Int) {
        connection?.receive(minimumIncompleteLength = 1, maximumLength = 4096) { data, _, _, error ->
            if (isCurrentConnection(interfaceTypeIndex)) {
                if (data == null || error != null) {
                    completed(data = null)
                } else {
                    handleResponse(data = data)
                    receiveData(interfaceTypeIndex)
                }
            }
        }
    }

    private fun handleResponse(data: ByteArray) {
        responseParser.append(data = data)
        val (done, body) = responseParser.parse()
        if (done) {
            completed(data = body)
        }
    }

    private data class CreatedRequest(
        val host: String,
        val port: Int,
        val useTls: Boolean,
        val content: ByteArray,
    )
}

fun httpCall(request: Request, body: ByteArray?, completion: (ByteArray?) -> Unit) {
    InterfaceTypeHttpClient().call(request = request, body = body) { data ->
        if (data != null) {
            completion(data)
        } else {
            httpCallUrlSession(request = request, body = body, completion = completion)
        }
    }
}

private fun httpCallUrlSession(request: Request, body: ByteArray?, completion: (ByteArray?) -> Unit) {
    val effective = if (body != null) {
        request.newBuilder().method(request.method, body.toRequestBody()).build()
    } else {
        request
    }
    httpRequest(request = effective) { data, response, error ->
        if (error != null || response?.isSuccessful != true) {
            completion(null)
            return@httpRequest
        }
        completion(data)
    }
}

private fun parseIntStrict(value: String): Int? {
    if (value.isEmpty()) {
        return null
    }
    var index = 0
    if (value[0] == '+' || value[0] == '-') {
        index = 1
    }
    if (index >= value.length) {
        return null
    }
    for (i in index until value.length) {
        val c = value[i]
        if (c < '0' || c > '9') {
            return null
        }
    }
    return value.toIntOrNull()
}

private fun decodeUtf8Strict(bytes: ByteArray, offset: Int, length: Int): String? = runCatching {
    val decoder = Charsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
    decoder.decode(ByteBuffer.wrap(bytes, offset, length)).toString().removePrefix("\uFEFF")
}.getOrNull()

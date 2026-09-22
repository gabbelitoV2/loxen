package com.moblin.android.various.network

import android.net.Network
import com.moblin.android.various.MainTimer
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

class HttpParser {
    var data: ByteArray = ByteArray(0)

    fun append(data: ByteArray) {
        this.data += data
    }

    fun getLine(data: ByteArray, offset: Int): Pair<String, Int>? {
        val remaining = data.copyOfRange(offset, data.size)
        var rIndex = -1
        for (i in remaining.indices) {
            if (remaining[i] == 0x0D.toByte()) {
                rIndex = i
                break
            }
        }
        if (rIndex < 0 || remaining.size <= rIndex + 1 || remaining[rIndex + 1] != 0x0A.toByte()) {
            return null
        }
        val line = String(remaining, 0, rIndex, Charsets.UTF_8)
        return line to (offset + rIndex + 2)
    }
}

class HttpResponseParser : HttpParser() {
    fun parse(): Pair<Boolean, ByteArray?> {
        var offset = 0
        val firstLine = getLine(data, offset) ?: return false to null
        var nextLineOffset = firstLine.second
        offset = nextLineOffset
        val statusLine = firstLine.first
        val statusParts = statusLine.split(" ").filter { it.isNotEmpty() }
        val statusCode = statusParts.getOrNull(1)?.toIntOrNull()
        if (!statusLine.startsWith("HTTP") ||
            statusParts.size < 3 ||
            statusCode == null ||
            statusCode !in 200..299
        ) {
            return true to null
        }
        var contentLength = 0
        while (true) {
            val lineResult = getLine(data, offset) ?: break
            val line = lineResult.first
            nextLineOffset = lineResult.second
            val parts = line.lowercase().split(" ").filter { it.isNotEmpty() }
            if (parts.size == 2 && parts.first() == "content-length:") {
                val length = parts.last().toIntOrNull()
                if (length == null || length < 0) {
                    return true to null
                }
                contentLength = length
            } else if (line.isEmpty()) {
                val body = data.copyOfRange(nextLineOffset, data.size)
                if (body.size == contentLength) {
                    return true to body
                }
            }
            offset = nextLineOffset
        }
        return false to null
    }
}

private class PlainRequest(
    val host: String,
    val port: Int,
    val useTls: Boolean,
    val content: ByteArray
)

private class InterfaceTypeHttpClient {
    companion object {
        private var interfaceTypes: List<Network> = getInterfaceTypes()

        private fun getInterfaceTypes(): List<Network> {
            TODO("resolve android.net.Network for cellular/wifi/ethernet via ConnectivityManager")
        }
    }

    private var interfaceTypes: List<Network> = emptyList()
    private var interfaceTypeIndex: Int = 0
    private var connection: Socket? = null
    private val timer = MainTimer()
    private var completion: ((ByteArray?) -> Unit)? = null
    private var responseParser = HttpResponseParser()

    init {
        interfaceTypes = Companion.interfaceTypes
    }

    private fun stop() {
        completion = null
        timer.stop()
        connection?.let { socket -> runCatching { socket.close() } }
        connection = null
    }

    private fun completed(data: ByteArray?) {
        completion?.invoke(data)
        stop()
    }

    fun call(request: Request, body: ByteArray?, completion: (ByteArray?) -> Unit) {
        val created = createRequest(request, body)
        if (created == null) {
            completion(null)
            return
        }
        this.completion = completion
        timer.startSingleShot(60) {
            completed(null)
        }
        connect(created.host, created.port, created.useTls) { index ->
            val socket = connection
            if (socket == null) {
                completed(null)
                return@connect
            }
            mainScope.launch {
                val sent = withContext(Dispatchers.IO) {
                    runCatching {
                        val output = socket.getOutputStream()
                        output.write(created.content)
                        output.flush()
                    }.isSuccess
                }
                if (!sent) {
                    completed(null)
                    return@launch
                }
                receiveData(index)
            }
        }
    }

    private fun connect(host: String, port: Int, useTls: Boolean, onConnected: (Int) -> Unit) {
        if (interfaceTypeIndex >= interfaceTypes.size) {
            completed(null)
            return
        }
        responseParser = HttpResponseParser()
        val index = interfaceTypeIndex
        val network = interfaceTypes[index]
        mainScope.launch {
            val socket = withContext(Dispatchers.IO) {
                runCatching {
                    val raw = Socket()
                    network.bindSocket(raw)
                    raw.connect(InetSocketAddress(host, port), 60000)
                    if (useTls) {
                        val ssl = (SSLSocketFactory.getDefault() as SSLSocketFactory)
                            .createSocket(raw, host, port, true) as SSLSocket
                        ssl.startHandshake()
                        ssl
                    } else {
                        raw
                    }
                }.getOrNull()
            }
            if (!isCurrentConnection(index)) {
                socket?.let { active -> runCatching { active.close() } }
                return@launch
            }
            if (socket == null) {
                connection = null
                interfaceTypeIndex += 1
                connect(host, port, useTls, onConnected)
                return@launch
            }
            updateGlobalInterfaceTypesIfNeeded()
            connection = socket
            onConnected(index)
        }
    }

    private fun isCurrentConnection(interfaceTypeIndex: Int): Boolean {
        return this.interfaceTypeIndex == interfaceTypeIndex
    }

    private fun updateGlobalInterfaceTypesIfNeeded() {
        if (interfaceTypeIndex == 0) {
            return
        }
        val types = interfaceTypes.toMutableList()
        val first = types[0]
        types[0] = types[interfaceTypeIndex]
        types[interfaceTypeIndex] = first
        Companion.interfaceTypes = types
    }

    private fun createRequest(request: Request, body: ByteArray?): PlainRequest? {
        val url = request.url
        val host = url.host
        val scheme = url.scheme
        val method = request.method
        if (host.isEmpty() || scheme.isEmpty() || method.isEmpty()) {
            return null
        }
        val useTls = scheme == "https"
        val port = if (url.port != -1) url.port else if (useTls) 443 else 80
        var path = url.encodedPath
        if (path.isEmpty()) {
            path = "/"
        }
        url.encodedQuery?.let { query -> path += "?$query" }
        var data = "$method $path HTTP/1.1\r\nHost: $host\r\n"
        for ((name, value) in request.headers) {
            data += "$name: $value\r\n"
        }
        if (body != null) {
            data += "Content-Length: ${body.size}\r\n"
        }
        data += "\r\n"
        var content = data.toByteArray(Charsets.UTF_8)
        if (body != null) {
            content += body
        }
        return PlainRequest(host, port, useTls, content)
    }

    private fun receiveData(interfaceTypeIndex: Int) {
        val socket = connection ?: return
        mainScope.launch {
            val buffer = ByteArray(4096)
            val count = withContext(Dispatchers.IO) {
                runCatching {
                    socket.getInputStream().read(buffer)
                }.getOrNull()
            }
            if (!isCurrentConnection(interfaceTypeIndex)) {
                return@launch
            }
            if (count == null || count < 0) {
                completed(null)
                return@launch
            }
            val data = buffer.copyOf(count)
            handleResponse(data)
            receiveData(interfaceTypeIndex)
        }
    }

    private fun handleResponse(data: ByteArray) {
        responseParser.append(data)
        val (done, body) = responseParser.parse()
        if (done) {
            completed(body)
        }
    }
}

fun httpCall(request: Request, body: ByteArray?, completion: (ByteArray?) -> Unit) {
    InterfaceTypeHttpClient().call(request, body) { data ->
        if (data != null) {
            completion(data)
        } else {
            httpCallUrlSession(request, body, completion)
        }
    }
}

private fun httpCallUrlSession(request: Request, body: ByteArray?, completion: (ByteArray?) -> Unit) {
    if (body != null) {
        val client = OkHttpClient()
        val contentType = request.body?.contentType()
        val upload = request.newBuilder()
            .method(request.method, body.toRequestBody(contentType))
            .build()
        client.newCall(upload).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainScope.launch {
                    completion(null)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        mainScope.launch {
                            completion(null)
                        }
                        return
                    }
                    val bytes = it.body?.bytes()
                    mainScope.launch {
                        completion(bytes)
                    }
                }
            }
        })
    } else {
        httpRequest(request) { data, response, error ->
            if (error != null || response?.isSuccessful != true) {
                completion(null)
                return@httpRequest
            }
            completion(data)
        }
    }
}

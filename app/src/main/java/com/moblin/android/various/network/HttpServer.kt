package com.moblin.android.various.network

import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.moblin.android.various.SimpleTimer
import com.moblin.android.various.settings.SettingsHttpHeader
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

private const val TAG = "HttpServer"

private data class HttpRequestParseResult(
    val method: String,
    val path: String,
    val version: String,
    val headers: List<SettingsHttpHeader>,
    val data: ByteArray,
)

private class HttpRequestParser {
    private val parser = HttpParser()

    fun append(data: ByteArray) {
        parser.append(data)
    }

    fun parse(): Pair<Boolean, HttpRequestParseResult?> {
        var offset = 0
        val startLineResult = parser.getLine(parser.data, offset) ?: return Pair(false, null)
        val startLine = startLineResult.first
        var nextLineOffset = startLineResult.second
        offset = nextLineOffset
        val startParts = startLine.split(" ").filter { it.isNotEmpty() }
        if (startParts.size != 3) {
            return Pair(true, null)
        }
        val method = startParts[0]
        val path = startParts[1]
        if (path.contains("//") || path.contains("..")) {
            return Pair(true, null)
        }
        val version = startParts[2]
        if (!version.startsWith("HTTP/1.")) {
            return Pair(true, null)
        }
        val headers = mutableListOf<SettingsHttpHeader>()
        while (true) {
            val lineResult = parser.getLine(parser.data, offset) ?: break
            val line = lineResult.first
            nextLineOffset = lineResult.second
            val parts = line.lowercase().split(" ").filter { it.isNotEmpty() }
            if (parts.size == 2) {
                headers.add(SettingsHttpHeader(name = parts[0], value = parts[1]))
            }
            if (line.isEmpty()) {
                val contentLengthHeader = headers.firstOrNull { it.name == "content-length:" }
                val contentLength = contentLengthHeader?.value?.toIntOrNull() ?: 0
                if (contentLength < 0) {
                    return Pair(true, null)
                }
                val body = parser.data.copyOfRange(nextLineOffset.coerceAtMost(parser.data.size), parser.data.size)
                if (body.size < contentLength) {
                    return Pair(false, null)
                }
                return Pair(
                    true,
                    HttpRequestParseResult(
                        method = method,
                        path = path,
                        version = version,
                        headers = headers,
                        data = body.copyOf(contentLength),
                    ),
                )
            }
            offset = nextLineOffset
        }
        return Pair(false, null)
    }
}

class HttpServerRequest internal constructor(
    val method: String,
    val path: String,
    val version: String,
    val headers: List<SettingsHttpHeader>,
    val body: ByteArray,
) {
    internal fun getContentType(): String {
        return when (path.split(".").lastOrNull()) {
            "html" -> "text/html"
            "mjs" -> "text/javascript"
            "css" -> "text/css"
            "woff2" -> "font/woff2"
            "ico" -> "image/vnd.microsoft.icon"
            "png" -> "image/png"
            else -> "text/html"
        }
    }
}

enum class HttpServerStatus {
    ok,
    created,
    noContent,
    badRequest,
    notFound,
    methodNotAllowed;

    fun code(): Int {
        return when (this) {
            ok -> 200
            created -> 201
            noContent -> 204
            badRequest -> 400
            notFound -> 404
            methodNotAllowed -> 405
        }
    }

    fun text(): String {
        return when (this) {
            ok -> "OK"
            created -> "Created"
            noContent -> "No Content"
            badRequest -> "Bad Request"
            notFound -> "Not Found"
            methodNotAllowed -> "Method Not Allowed"
        }
    }
}

class HttpServerResponse internal constructor(
    private val connection: HttpServerConnection?,
) {
    fun send(status: HttpServerStatus = HttpServerStatus.ok) {
        send(data = ByteArray(0), status = status)
    }

    fun send(data: ByteArray, status: HttpServerStatus = HttpServerStatus.ok) {
        connection?.sendAndClose(status = status, content = data)
    }

    fun send(text: String, status: HttpServerStatus = HttpServerStatus.ok) {
        send(data = text.toByteArray(), status = status)
    }

    fun send(
        data: ByteArray,
        status: HttpServerStatus,
        contentType: String,
        headers: List<SettingsHttpHeader> = emptyList(),
    ) {
        connection?.sendAndClose(
            status = status,
            content = data,
            contentType = contentType,
            extraHeaders = headers,
        )
    }

    fun sendFile(url: File, contentType: String, headers: List<SettingsHttpHeader> = emptyList()) {
        connection?.sendFileAndClose(
            fileUrl = url,
            contentType = contentType,
            extraHeaders = headers,
        )
    }
}

internal class HttpServerConnection(
    private val connection: Socket,
    private val server: HttpServer?,
) {
    private var parser = HttpRequestParser()
    private var request: HttpServerRequest? = null
    private val output: OutputStream = connection.getOutputStream()

    fun receiveData() {
        val scope = server?.scope ?: return
        scope.launch(Dispatchers.IO) {
            try {
                val input = connection.getInputStream()
                val buffer = ByteArray(4096)
                while (isActive) {
                    val count = input.read(buffer)
                    if (count < 0) {
                        break
                    }
                    if (count == 0) {
                        continue
                    }
                    handleData(buffer.copyOf(count))
                }
            } catch (e: Exception) {
                Log.i(TAG, "http-server: Connection error: ${e.message}")
            }
        }
    }

    private fun handleData(data: ByteArray) {
        parser.append(data)
        val (done, result) = parser.parse()
        if (!done) {
            return
        }
        val server = server
        if (result == null || server == null) {
            runCatching { connection.close() }
            return
        }
        val request = HttpServerRequest(
            method = result.method,
            path = result.path,
            version = result.version,
            headers = result.headers,
            body = result.data,
        )
        this.request = request
        val route = server.findRoute(request)
        if (route == null) {
            sendAndClose(status = HttpServerStatus.notFound, content = ByteArray(0))
            return
        }
        route.handler(request, HttpServerResponse(this))
    }

    fun sendAndClose(
        status: HttpServerStatus,
        content: ByteArray,
        contentType: String? = null,
        extraHeaders: List<SettingsHttpHeader> = emptyList(),
    ) {
        val request = request ?: return
        val lines = mutableListOf<String>()
        lines.add("${request.version} ${status.code()} ${status.text()}")
        if (content.isNotEmpty()) {
            lines.add("Content-Type: ${contentType ?: request.getContentType()}")
        }
        if (status != HttpServerStatus.noContent) {
            lines.add("Content-Length: ${content.size}")
        }
        appendExtraHeaders(headers = extraHeaders, lines = lines)
        val headerData = appendCloseHeaderAndFinalize(lines)
        sendAndClose(headerData + content)
    }

    fun sendFileAndClose(
        fileUrl: File,
        contentType: String,
        extraHeaders: List<SettingsHttpHeader> = emptyList(),
    ) {
        val request = request ?: return
        if (!fileUrl.isFile) {
            sendAndClose(status = HttpServerStatus.notFound, content = ByteArray(0))
            return
        }
        val fileHandle = runCatching { FileInputStream(fileUrl) }.getOrNull()
        if (fileHandle == null) {
            sendAndClose(status = HttpServerStatus.notFound, content = ByteArray(0))
            return
        }
        val fileSize = fileUrl.length()
        val lines = mutableListOf<String>()
        lines.add("${request.version} ${HttpServerStatus.ok.code()} ${HttpServerStatus.ok.text()}")
        lines.add("Content-Type: $contentType")
        lines.add("Content-Length: $fileSize")
        appendExtraHeaders(headers = extraHeaders, lines = lines)
        val headerData = appendCloseHeaderAndFinalize(lines)
        val headerSent = runCatching {
            output.write(headerData)
            output.flush()
        }.isSuccess
        if (!headerSent) {
            closeFileAndConnection(fileHandle)
            return
        }
        sendFileChunk(fileHandle = fileHandle, remaining = fileSize)
    }

    private fun sendFileChunk(fileHandle: FileInputStream, remaining: Long) {
        var remainingBytes = remaining
        while (true) {
            val chunkSize = minOf(remainingBytes, 512L * 1024L).toInt()
            if (chunkSize <= 0) {
                closeFileAndConnection(fileHandle)
                return
            }
            val chunk = ByteArray(chunkSize)
            val read = runCatching { fileHandle.read(chunk) }.getOrDefault(-1)
            if (read <= 0) {
                closeFileAndConnection(fileHandle)
                return
            }
            val chunkSent = runCatching {
                output.write(chunk, 0, read)
                output.flush()
            }.isSuccess
            if (!chunkSent) {
                closeFileAndConnection(fileHandle)
                return
            }
            remainingBytes -= read.toLong()
        }
    }

    private fun closeFileAndConnection(fileHandle: FileInputStream) {
        runCatching { fileHandle.close() }
        runCatching { connection.close() }
    }

    private fun sendAndClose(data: ByteArray) {
        runCatching {
            output.write(data)
            output.flush()
        }
        runCatching { connection.close() }
    }
}

private fun appendExtraHeaders(headers: List<SettingsHttpHeader>, lines: MutableList<String>) {
    for (header in headers) {
        lines.add("${header.name}: ${header.value}")
    }
}

private fun appendCloseHeaderAndFinalize(lines: MutableList<String>): ByteArray {
    lines.add("Connection: close")
    lines.add("")
    lines.add("")
    return lines.joinToString("\r\n").toByteArray()
}

class HttpServerRoute(
    val path: String,
    val prefixMatch: Boolean = false,
    val handler: (HttpServerRequest, HttpServerResponse) -> Unit,
) {
    fun matches(path: String): Boolean {
        return if (prefixMatch) {
            path.startsWith(this.path)
        } else {
            path == this.path
        }
    }
}

class HttpServer(
    private val queue: CoroutineDispatcher,
    private val routes: List<HttpServerRoute>,
    private val service: NsdServiceInfo? = null,
) {
    internal val scope = CoroutineScope(queue + SupervisorJob())
    private var listener: ServerSocket? = null
    private var acceptJob: Job? = null
    private val retryTimer: SimpleTimer
    private var port: Int = 80
    private var started: Boolean = false

    init {
        retryTimer = SimpleTimer(queue)
    }

    fun start(port: Int) {
        Log.d(TAG, "http-server: Start")
        scope.launch {
            startInternal(port)
        }
    }

    fun stop() {
        Log.d(TAG, "http-server: Stop")
        scope.launch {
            stopInternal()
        }
    }

    private fun startInternal(port: Int) {
        this.port = port
        started = true
        setupListener()
    }

    private fun stopInternal() {
        started = false
        retryTimer.stop()
        acceptJob?.cancel()
        acceptJob = null
        runCatching { listener?.close() }
        listener = null
    }

    private fun setupListener() {
        try {
            val serverSocket = ServerSocket()
            serverSocket.reuseAddress = true
            serverSocket.bind(InetSocketAddress(port))
            listener = serverSocket
            acceptJob = scope.launch(Dispatchers.IO) {
                while (isActive) {
                    val client = runCatching { serverSocket.accept() }.getOrNull() ?: break
                    handleNewConnection(client)
                }
            }
            if (service != null) {
                TODO("register mDNS service with NsdManager")
            }
        } catch (e: Exception) {
            handleStateUpdate(failed = true)
        }
    }

    private fun handleStateUpdate(failed: Boolean) {
        if (!failed) {
            return
        }
        retryTimer.startSingleShot(timeout = 1.0) {
            if (!started) {
                return@startSingleShot
            }
            setupListener()
        }
    }

    private fun handleNewConnection(connection: Socket) {
        val serverConnection = HttpServerConnection(connection = connection, server = this)
        serverConnection.receiveData()
    }

    internal fun findRoute(request: HttpServerRequest): HttpServerRoute? {
        return routes.firstOrNull { it.matches(path = request.path) }
    }
}

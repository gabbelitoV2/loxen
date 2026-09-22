package com.moblin.android.various.network

import android.net.Network
import android.webkit.WebView
import java.io.IOException
import java.net.InetAddress
import java.net.URI
import java.net.URLEncoder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okio.ByteString
import okio.ByteString.Companion.toByteString

class NWPath(val availableInterfaces: List<Network>)

fun NWPath.uniqueAvailableInterfaces(): List<Network> {
    val interfaces = mutableListOf<Network>()
    for (network in availableInterfaces) {
        if (!interfaces.contains(network)) {
            interfaces.add(network)
        }
    }
    return interfaces
}

data class NWEndpoint(val host: String, val port: Int) {
    companion object {
        fun port(integer: Int): Int {
            return integer.coerceIn(0, 65_535)
        }
    }
}

enum class NWProtocolWebSocketOpcode(val rawValue: Int) {
    Text(1),
    Binary(2),
    Close(8),
    Ping(9),
    Pong(10),
    ;

    companion object {
        fun fromRawValue(rawValue: Int): NWProtocolWebSocketOpcode? {
            return entries.firstOrNull { it.rawValue == rawValue }
        }
    }
}

data class NWProtocolWebSocketMetadata(val opcode: NWProtocolWebSocketOpcode)

data class NWConnectionContentContext(
    val identifier: String,
    val metadata: List<NWProtocolWebSocketMetadata> = emptyList(),
)

fun NWConnectionContentContext.webSocketOperation(): NWProtocolWebSocketOpcode? {
    return metadata.firstOrNull()?.opcode
}

sealed class NWConnectionSendCompletion {
    object Idempotent : NWConnectionSendCompletion()
    data class ContentProcessed(val completion: (Throwable?) -> Unit) : NWConnectionSendCompletion()
}

fun WebSocket.sendWebSocket(
    data: ByteArray?,
    opcode: NWProtocolWebSocketOpcode,
    completion: NWConnectionSendCompletion = NWConnectionSendCompletion.Idempotent,
) {
    when (opcode) {
        NWProtocolWebSocketOpcode.Text -> send(data?.decodeToString() ?: "")
        NWProtocolWebSocketOpcode.Binary -> send((data ?: ByteArray(0)).toByteString())
        NWProtocolWebSocketOpcode.Close -> close(1000, data?.decodeToString())
        NWProtocolWebSocketOpcode.Ping -> TODO("no OkHttp public API to send a ping frame")
        NWProtocolWebSocketOpcode.Pong -> TODO("no OkHttp public API to send a pong frame")
    }
    if (completion is NWConnectionSendCompletion.ContentProcessed) {
        completion.completion(null)
    }
}

sealed class NetworkResponse<out T> {
    data class Success<out T>(val value: T) : NetworkResponse<T>()
    object AuthError : NetworkResponse<Nothing>()
    object Error : NetworkResponse<Nothing>()

    fun isSuccessful(): Boolean {
        return this is Success
    }
}

typealias OperationResult = NetworkResponse<ByteArray>

fun makeUrl(path: String, parameters: List<Pair<String, String>>): String {
    val query = parameters.joinToString("&") { (name, value) ->
        "${urlEncode(name)}=${urlEncode(value)}"
    }
    return if (query.isEmpty()) {
        path
    } else {
        "$path?$query"
    }
}

fun makeMdnsHostname(deviceName: String): String {
    var name = deviceName.lowercase()
    name = name.replace(" ", "-")
    name = name.replace(Regex("-+"), "-")
    name = name.trim('-')
    name = name.replace(Regex("[^\\w\\d-]"), "")
    return "$name.local"
}

private val httpClient = OkHttpClient()

fun httpRequest(
    request: Request,
    queue: CoroutineDispatcher = Dispatchers.Main,
    completion: ((ByteArray?, Response?, Throwable?) -> Unit)? = null,
) {
    val scope = CoroutineScope(queue)
    httpClient.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            val callback = completion ?: return
            scope.launch {
                callback(null, null, e)
            }
        }

        override fun onResponse(call: Call, response: Response) {
            val body = runCatching { response.body?.bytes() }.getOrNull()
            response.close()
            val callback = completion ?: return
            scope.launch {
                callback(body, response, null)
            }
        }
    })
}

fun getHttpsUrl(text: String): URI? {
    if (text.startsWith("https://")) {
        val url = runCatching { URI(text.trim()) }.getOrNull()
        if (url != null) {
            return url
        }
    }
    return null
}

fun WebView.setHttpProxy(endpoint: NWEndpoint?) {
    TODO("no Android counterpart for WKWebViewConfiguration.proxyConfigurations")
}

fun URI.isLoopback(): Boolean {
    val host = host?.trim('[', ']') ?: return false
    if (host == "localhost") {
        return true
    }
    if (!looksLikeIpLiteral(host)) {
        return false
    }
    val address = runCatching { InetAddress.getByName(host) }.getOrNull() ?: return false
    return address.isLoopbackAddress
}

private fun looksLikeIpLiteral(host: String): Boolean {
    if (host.isEmpty()) {
        return false
    }
    return if (host.contains(':')) {
        host.all { it.isDigit() || it in "abcdefABCDEF:." }
    } else {
        host.count { it == '.' } == 3 && host.all { it.isDigit() || it == '.' }
    }
}

private fun urlEncode(value: String): String {
    return URLEncoder.encode(value, "UTF-8").replace("+", "%20")
}

package com.moblin.android.media.haishinkit.rtmp

import com.moblin.android.media.haishinkit.rtmp.amf.AsObject
import com.moblin.android.media.haishinkit.rtmp.amf.AsValue
import com.moblin.android.media.haishinkit.rtmp.message.RtmpAcknowledgementMessage
import com.moblin.android.media.haishinkit.rtmp.message.RtmpCommandMessage
import com.moblin.android.media.haishinkit.rtmp.message.RtmpCommandName
import com.moblin.android.media.haishinkit.rtmp.message.RtmpDataMessage
import com.moblin.android.media.haishinkit.rtmp.message.RtmpMessage
import com.moblin.android.media.haishinkit.rtmp.message.RtmpMessageType
import com.moblin.android.media.haishinkit.rtmp.message.RtmpSetChunkSizeMessage
import com.moblin.android.media.haishinkit.rtmp.message.RtmpUserControlMessage
import com.moblin.android.media.haishinkit.rtmp.message.RtmpWindowAcknowledgementSizeMessage
import com.moblin.android.media.haishinkit.util.calculateMd5Base64
import com.moblin.android.various.SimpleTimer
import java.net.URI
import java.net.URLDecoder
import kotlin.random.Random
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private enum class SupportVideo(val rawValue: Int) {
    h264(0x0080),
}

private enum class SupportSound(val rawValue: Int) {
    aac(0x0400),
}

private enum class VideoFunction(val rawValue: Int) {
    clientSeek(1),
}

enum class RtmpConnectionCode(val rawValue: String) {
    connectClosed("NetConnection.Connect.Closed"),
    connectFailed("NetConnection.Connect.Failed"),
    connectRejected("NetConnection.Connect.Rejected"),
    connectSuccess("NetConnection.Connect.Success"),
    ;

    fun eventData(): AsObject {
        return mutableMapOf<String, AsValue>("code" to AsValue.String(rawValue))
    }

    companion object {
        fun fromRawValue(rawValue: String): RtmpConnectionCode? {
            return entries.firstOrNull { it.rawValue == rawValue }
        }
    }
}

private val URI.user: String?
    get() = userInfo?.substringBefore(":")

private val URI.password: String?
    get() = userInfo?.takeIf { it.contains(":") }?.substringAfter(":")

private fun absoluteWithoutAuthentication(uri: URI): String {
    val port = if (uri.port >= 0) ":" + uri.port else ""
    val path = uri.path ?: ""
    val query = if (uri.query != null) "?" + uri.query else ""
    return uri.scheme + "://" + uri.host + port + path + query
}

private fun decodeQueryComponent(value: String): String {
    return runCatching { URLDecoder.decode(value, "UTF-8") }.getOrDefault(value)
}

private fun String.dictionaryFromQuery(): Map<String, String> {
    val dictionary = mutableMapOf<String, String>()
    for (pair in split("&")) {
        val keyValue = pair.split("=")
        if (keyValue.size != 2) {
            continue
        }
        dictionary[decodeQueryComponent(keyValue[0])] = decodeQueryComponent(keyValue[1])
    }
    return dictionary
}

private fun makeSanJoseAuthCommand(url: URI, description: String): String {
    var command = url.toString()
    val index = description.indexOf("?")
    if (index < 0) {
        return command
    }
    val query = description.substring(index + 1)
    val challenge = String.format("%08x", Random.nextLong(0L, 0x100000000L))
    val dictionary = query.dictionaryFromQuery()
    val salt = dictionary["salt"]
    val user = url.user
    val password = url.password
    if (salt == null || user == null || password == null) {
        return command
    }
    var response = calculateMd5Base64("$user$salt$password")
    val opaque = dictionary["opaque"]
    if (opaque != null) {
        command += "&opaque=$opaque"
        response += opaque
    } else {
        val queryChallenge = dictionary["challenge"]
        if (queryChallenge != null) {
            response += queryChallenge
        }
    }
    response = calculateMd5Base64("$response$challenge")
    command += "&challenge=$challenge&response=$response"
    return command
}

class RtmpConnection(private val name: String, private val queue: CoroutineDispatcher) : RtmpSocketDelegate {
    private var uri: URI? = null
    var socket: RtmpSocket
        private set
    var stream: RtmpStream? = null
    var callCompletions: MutableMap<Int, (List<AsValue>) -> Unit> = mutableMapOf()
    private var nextTransactionId = 0
    private val timer: SimpleTimer
    private val chunkReader = RtmpChunkReader()
    private val scope = CoroutineScope(queue)

    init {
        timer = SimpleTimer(queue)
        socket = RtmpSocket(name, queue)
    }

    fun connect(url: String) {
        val uri = runCatching { URI(url) }.getOrNull() ?: return
        val scheme = uri.scheme ?: return
        val host = uri.host ?: return
        this.uri = uri
        chunkReader.clear()
        socket = RtmpSocket(name, queue)
        socket.delegate = this
        if (scheme == "rtmps") {
            socket.connect(host, if (uri.port >= 0) uri.port else 443, TlsOptions())
        } else {
            socket.connect(host, if (uri.port >= 0) uri.port else 1935, null)
        }
    }

    fun disconnect() {
        timer.stop()
        stream?.closeInternal()
        socket.close(false)
        socket = RtmpSocket(name, queue)
    }

    fun call(
        commandName: RtmpCommandName,
        arguments: List<AsValue>,
        onCompleted: ((List<AsValue>) -> Unit)? = null,
    ) {
        val message = RtmpCommandMessage(
            streamId = 0,
            transactionId = getNextTransactionId(),
            commandType = RtmpMessageType.amf0Command,
            commandName = commandName,
            commandObject = null,
            arguments = arguments,
        )
        if (onCompleted != null) {
            callCompletions[message.transactionId] = onCompleted
        }
        socket.write(RtmpChunk(message = message))
    }

    fun gotCommand(data: AsObject) {
        on(data)
    }

    fun getNextTransactionId(): Int {
        nextTransactionId += 1
        return nextTransactionId
    }

    private fun on(data: AsObject) {
        scope.launch {
            onInternal(data)
            stream?.onInternal(data)
        }
    }

    private fun onInternal(data: AsObject) {
        val code = (data["code"] as? AsValue.String)?.value ?: return
        when (RtmpConnectionCode.fromRawValue(code)) {
            RtmpConnectionCode.connectSuccess -> handleConnectSuccess()
            RtmpConnectionCode.connectRejected -> handleConnectRejected(data)
            RtmpConnectionCode.connectClosed -> handleConnectClosed()
            else -> {}
        }
    }

    private fun handleConnectSuccess() {
        socket.maximumChunkSizeToServer = 1024 * 8
        socket.write(
            RtmpChunk(
                type = RtmpChunkType.zero,
                chunkStreamId = RtmpChunk.ChunkStreamId.control.rawValue,
                message = RtmpSetChunkSizeMessage(socket.maximumChunkSizeToServer.toUInt()),
            )
        )
    }

    private fun handleConnectRejected(data: AsObject) {
        val uri = this.uri ?: return
        val user = uri.user ?: return
        val password = uri.password ?: return
        val description = (data["description"] as? AsValue.String)?.value ?: return
        socket.close(false)
        if (description.contains("reason=nosuchuser")) {
        } else if (description.contains("reason=authfailed")) {
        } else if (description.contains("reason=needauth")) {
            connect(makeSanJoseAuthCommand(uri, description))
        } else if (description.contains("authmod=adobe")) {
            if (user.isEmpty() || password.isEmpty()) {
                disconnect()
            } else {
                val query = uri.query ?: ""
                val command = uri.toString() + (if (query.isEmpty()) "?" else "&") + "authmod=adobe&user=$user"
                connect(command)
            }
        }
    }

    private fun handleConnectClosed() {
        disconnect()
    }

    private fun makeConnectChunk(): RtmpChunk? {
        val uri = this.uri ?: return null
        var app = uri.path?.trimStart('/') ?: ""
        val query = uri.query
        if (query != null) {
            app += "?" + query
        }
        val message = RtmpCommandMessage(
            streamId = 0,
            transactionId = getNextTransactionId(),
            commandType = RtmpMessageType.amf0Command,
            commandName = RtmpCommandName.connect,
            commandObject = mutableMapOf<String, AsValue>(
                "app" to AsValue.String(app),
                "flashVer" to AsValue.String("FMLE/3.0 (compatible; FMSc/1.0)"),
                "swfUrl" to AsValue.Null,
                "tcUrl" to AsValue.String(absoluteWithoutAuthentication(uri)),
                "fpad" to AsValue.Boolean(false),
                "capabilities" to AsValue.Number(239.0),
                "audioCodecs" to AsValue.Number(SupportSound.aac.rawValue.toDouble()),
                "videoCodecs" to AsValue.Number(SupportVideo.h264.rawValue.toDouble()),
                "videoFunction" to AsValue.Number(VideoFunction.clientSeek.rawValue.toDouble()),
                "pageUrl" to AsValue.Null,
                "objectEncoding" to AsValue.Number(0.0),
            ),
            arguments = emptyList(),
        )
        return RtmpChunk(message = message)
    }

    private fun handleHandshakeDone() {
        val chunk = makeConnectChunk()
        if (chunk == null) {
            disconnect()
            return
        }
        socket.write(chunk)
        timer.startPeriodic(1.0) {
            stream?.onTimeout()
        }
    }

    private fun handleClosed() {
        nextTransactionId = 0
        callCompletions.clear()
        chunkReader.clear()
    }

    private fun processMessageSetChunkSize(message: RtmpSetChunkSizeMessage) {
        chunkReader.maximumChunkSize = message.size.toInt()
    }

    private fun processMessageAcknowledgementMessage(message: RtmpAcknowledgementMessage) {
        stream?.info.onAck(message.sequence)
    }

    private fun processMessageWindowAcknowledgementSize() {
        socket.write(
            RtmpChunk(
                type = RtmpChunkType.zero,
                chunkStreamId = RtmpChunk.ChunkStreamId.control.rawValue,
                message = RtmpWindowAcknowledgementSizeMessage(100_000u),
            )
        )
    }

    private fun processMessageUserControl(message: RtmpUserControlMessage) {
        when (message.event) {
            RtmpUserControlMessage.Event.ping -> {
                socket.write(
                    RtmpChunk(
                        type = RtmpChunkType.zero,
                        chunkStreamId = RtmpChunk.ChunkStreamId.control.rawValue,
                        message = RtmpUserControlMessage(
                            event = RtmpUserControlMessage.Event.pong,
                            value = message.value,
                        ),
                    )
                )
            }
            else -> {}
        }
    }

    private fun processMessageCommand(message: RtmpCommandMessage) {
        val completion = callCompletions.remove(message.transactionId)
        if (completion == null) {
            when (message.commandName) {
                RtmpCommandName.close -> disconnect()
                else -> {
                    val data = (message.arguments.firstOrNull() as? AsValue.Object)?.value
                    if (data != null) {
                        gotCommand(data)
                    }
                }
            }
            return
        }
        when (message.commandName) {
            RtmpCommandName.result -> completion(message.arguments)
            else -> {}
        }
    }

    private fun processMessageData(message: RtmpDataMessage) {
        stream?.info.bitrateStats.mutate { it.add(bytesTransferred = message.encoded.size) }
    }

    override fun socketReadyStateChanged(readyState: RtmpSocketReadyState) {
        when (readyState) {
            RtmpSocketReadyState.handshakeDone -> handleHandshakeDone()
            RtmpSocketReadyState.closed -> handleClosed()
            else -> {}
        }
    }

    override fun socketUpdateStats(totalBytesSent: Long) {
        stream?.info.onWritten(totalBytesSent)
    }

    override fun socketDataReceived(data: ByteArray): ByteArray {
        chunkReader.read(data) { message -> processMessage(message) }
        return data
    }

    private fun processMessage(message: RtmpMessage) {
        if (message is RtmpSetChunkSizeMessage) {
            processMessageSetChunkSize(message)
        } else if (message is RtmpAcknowledgementMessage) {
            processMessageAcknowledgementMessage(message)
        } else if (message is RtmpUserControlMessage) {
            processMessageUserControl(message)
        } else if (message is RtmpWindowAcknowledgementSizeMessage) {
            processMessageWindowAcknowledgementSize()
        } else if (message is RtmpCommandMessage) {
            processMessageCommand(message)
        } else if (message is RtmpDataMessage) {
            processMessageData(message)
        }
    }

    override fun socketPost(data: AsObject) {
        on(data)
    }
}

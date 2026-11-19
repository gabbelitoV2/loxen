package com.moblin.android.media.haishinkit.rtmp

import android.media.MediaFormat
import com.moblin.android.platform.log.Log
import com.moblin.android.common.various.makeRtmpStreamKey
import com.moblin.android.common.various.makeRtmpUri
import com.moblin.android.localized
import com.moblin.android.media.MediaSample
import com.moblin.android.media.haishinkit.codec.audio.AudioEncoderDelegate
import com.moblin.android.media.haishinkit.codec.video.VideoEncoder
import com.moblin.android.media.haishinkit.codec.video.VideoEncoderDelegate
import com.moblin.android.media.haishinkit.codec.video.VideoEncoderSettings
import com.moblin.android.media.haishinkit.flv.FlvAacPacketType
import com.moblin.android.media.haishinkit.flv.FlvAudioCodec
import com.moblin.android.media.haishinkit.flv.FlvAvcPacketType
import com.moblin.android.media.haishinkit.flv.FlvFrameType
import com.moblin.android.media.haishinkit.flv.FlvOpusPacketType
import com.moblin.android.media.haishinkit.flv.FlvSoundRate
import com.moblin.android.media.haishinkit.flv.FlvSoundSize
import com.moblin.android.media.haishinkit.flv.FlvSoundType
import com.moblin.android.media.haishinkit.flv.FlvTagType
import com.moblin.android.media.haishinkit.flv.FlvVideoCodec
import com.moblin.android.media.haishinkit.flv.FlvVideoFourCC
import com.moblin.android.media.haishinkit.flv.FlvVideoPacketType
import com.moblin.android.media.haishinkit.flv.extendedVideoHeader
import com.moblin.android.media.haishinkit.media.AudioVideoEncoderDelegate
import com.moblin.android.media.haishinkit.media.Processor
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.mpeg.MpegTsAudioConfig
import com.moblin.android.media.haishinkit.mpeg.avc.MpegTsVideoConfigAvc
import com.moblin.android.media.haishinkit.mpeg.hevc.MpegTsVideoConfigHevc
import com.moblin.android.media.haishinkit.rtmp.amf.AsObject
import com.moblin.android.media.haishinkit.rtmp.amf.AsValue
import com.moblin.android.media.haishinkit.rtmp.RtmpAbortMessage
import com.moblin.android.media.haishinkit.rtmp.RtmpAcknowledgementMessage
import com.moblin.android.media.haishinkit.rtmp.RtmpAudioMessage
import com.moblin.android.media.haishinkit.rtmp.RtmpCommandMessage
import com.moblin.android.media.haishinkit.rtmp.RtmpCommandName
import com.moblin.android.media.haishinkit.rtmp.RtmpDataMessage
import com.moblin.android.media.haishinkit.rtmp.RtmpMessageType
import com.moblin.android.media.haishinkit.rtmp.RtmpSetChunkSizeMessage
import com.moblin.android.media.haishinkit.rtmp.RtmpUserControlMessage
import com.moblin.android.media.haishinkit.rtmp.RtmpVideoMessage
import com.moblin.android.media.haishinkit.rtmp.RtmpWindowAcknowledgementSizeMessage
import com.moblin.android.media.haishinkit.util.ByteWriter
import com.moblin.android.media.haishinkit.util.calculateMd5Base64
import com.moblin.android.platform.network.NWProtocolTLS
import com.moblin.android.various.SimpleTimer
import java.io.ByteArrayOutputStream
import java.net.URI
import java.time.Duration
import java.time.Instant
import java.util.Locale
import kotlin.math.floor
import kotlin.random.Random
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "RtmpStream"

class RtmpTimestamps {
    private class Track {
        private var previous: Double? = null
        private var fraction = 0.0

        fun next(timestamp: Double): UInt? {
            var delta = 0.0
            val prior = previous
            if (prior != null) {
                delta = (timestamp - prior) * 1000
            }
            if (delta < 0) {
                return null
            }
            previous = timestamp
            fraction += delta
            val timestampDelta = fraction.toUInt()
            fraction -= floor(fraction)
            return timestampDelta
        }
    }

    private var base: Double? = null
    private var audioTrack = Track()
    private var videoTrack = Track()

    fun audio(timestamp: Double): UInt? {
        val rebased = rebase(timestamp) ?: return null
        return audioTrack.next(rebased)
    }

    fun video(timestamp: Double): UInt? {
        val rebased = rebase(timestamp) ?: return null
        return videoTrack.next(rebased)
    }

    private fun rebase(timestamp: Double): Double? {
        val resolvedBase = base ?: timestamp
        base = resolvedBase
        val rebased = timestamp - resolvedBase
        if (rebased < 0) {
            return null
        }
        return rebased
    }
}

private val aac: UByte = (
    (FlvAudioCodec.aac.rawValue.toInt() shl 4) or
        (FlvSoundRate.kHz44.rawValue.toInt() shl 2) or
        (FlvSoundSize.snd16bit.rawValue.toInt() shl 1) or
        FlvSoundType.stereo.rawValue.toInt()
    ).toUByte()

private val opus: UByte = (
    (FlvAudioCodec.exHeader.rawValue.toInt() shl 4) or
        FlvOpusPacketType.sequenceStart.rawValue.toInt()
    ).toUByte()

fun makeRtmpAudioSequenceHeader(format: MediaFormat): ByteArray? {
    val writer = ByteWriter()
    val mime = format.getString(MediaFormat.KEY_MIME)
    if (mime == "audio/opus") {
        writer.writeUInt8(opus)
        writer.writeUTF8Bytes("Opus")
        writer.writeUTF8Bytes("OpusHead")
        writer.writeUInt8(1u.toUByte())
        writer.writeUInt8(format.getInteger(MediaFormat.KEY_CHANNEL_COUNT).toUByte())
        writer.writeUInt16(0u.toUShort())
        writer.writeUInt32(format.getInteger(MediaFormat.KEY_SAMPLE_RATE).toUInt())
        writer.writeUInt16(0u.toUShort())
        writer.writeUInt8(0u.toUByte())
    } else {
        writer.writeUInt8(aac)
        writer.writeUInt8(FlvAacPacketType.seq.rawValue)
        writer.writeBytes(MpegTsAudioConfig(format).encode())
    }
    return writer.data
}

fun makeRtmpAudioFrame(audioBuffer: MediaSample, mime: String?): ByteArray {
    var buffer: ByteArray = if (mime == "audio/opus") {
        byteArrayOf(
            (
                (FlvAudioCodec.exHeader.rawValue.toInt() shl 4) or
                    FlvOpusPacketType.codedFrames.rawValue.toInt()
                ).toByte(),
        ) + "Opus".toByteArray(Charsets.UTF_8)
    } else {
        byteArrayOf(aac.toByte(), FlvAacPacketType.raw.rawValue.toByte())
    }
    buffer += audioBuffer.data
    return buffer
}

private fun makeAvcVideoTagHeader(frameType: FlvFrameType, packetType: FlvAvcPacketType): ByteArray {
    return byteArrayOf(
        ((frameType.rawValue.toInt() shl 4) or FlvVideoCodec.avc.rawValue.toInt()).toByte(),
        packetType.rawValue.toByte(),
    )
}

private fun makeHevcVideoTagHeader(frameType: FlvFrameType, packetType: FlvVideoPacketType): ByteArray {
    val writer = ByteWriter()
    writer.writeUInt8(
        (
            extendedVideoHeader.toInt() or
                (frameType.rawValue.toInt() shl 4) or
                packetType.rawValue.toInt()
            ).toUByte(),
    )
    writer.writeUInt32(FlvVideoFourCC.hevc.rawValue)
    return writer.data
}

fun makeRtmpVideoSequenceHeader(
    format: VideoEncoderSettings.Format,
    formatDescription: MediaFormat,
): ByteArray? {
    return when (format) {
        VideoEncoderSettings.Format.h264 -> {
            val avcC = MpegTsVideoConfigAvc.getAvcC(formatDescription) ?: return null
            makeAvcVideoTagHeader(FlvFrameType.key, FlvAvcPacketType.seq) +
                byteArrayOf(0, 0, 0) +
                avcC
        }
        VideoEncoderSettings.Format.hevc -> {
            val hvcC = MpegTsVideoConfigHevc.getHvcC(formatDescription) ?: return null
            makeHevcVideoTagHeader(FlvFrameType.key, FlvVideoPacketType.sequenceStart) + hvcC
        }
    }
}

fun makeRtmpVideoFrame(
    format: VideoEncoderSettings.Format,
    sampleBuffer: MediaSample,
    data: ByteArray,
): ByteArray {
    val frameType = if (sampleBuffer.isKeyFrame) FlvFrameType.key else FlvFrameType.inter
    var buffer: ByteArray = when (format) {
        VideoEncoderSettings.Format.h264 -> makeAvcVideoTagHeader(frameType, FlvAvcPacketType.nal)
        VideoEncoderSettings.Format.hevc -> makeHevcVideoTagHeader(frameType, FlvVideoPacketType.codedFrames)
    }
    var compositionTime = 0
    if (sampleBuffer.decodeTimeStampUs >= 0) {
        compositionTime = ((sampleBuffer.presentationTimeUs - sampleBuffer.decodeTimeStampUs) / 1000).toInt()
    }
    buffer += byteArrayOf(
        ((compositionTime shr 16) and 0xFF).toByte(),
        ((compositionTime shr 8) and 0xFF).toByte(),
        (compositionTime and 0xFF).toByte(),
    )
    buffer += data
    return buffer
}

enum class RtmpStatusCode(val rawValue: String) {
    connectClosed("NetConnection.Connect.Closed"),
    connectFailed("NetConnection.Connect.Failed"),
    connectRejected("NetConnection.Connect.Rejected"),
    connectSuccess("NetConnection.Connect.Success"),
    publishStart("NetStream.Publish.Start"),
    ;

    companion object {
        fun fromRawValue(rawValue: String): RtmpStatusCode? {
            return entries.firstOrNull { it.rawValue == rawValue }
        }
    }
}

private fun rtmpPercentDecode(value: String): String {
    if (!value.contains('%')) {
        return value
    }
    val bytes = ByteArrayOutputStream()
    var index = 0
    while (index < value.length) {
        val c = value[index]
        if (c == '%' && index + 2 < value.length) {
            val code = value.substring(index + 1, index + 3).toIntOrNull(16)
            if (code != null) {
                bytes.write(code)
                index += 3
                continue
            }
        }
        bytes.write(c.toString().toByteArray(Charsets.UTF_8))
        index += 1
    }
    return bytes.toByteArray().toString(Charsets.UTF_8)
}

private fun rtmpDictionaryFromQuery(query: String): Map<String, String> {
    val dictionary = mutableMapOf<String, String>()
    for (item in query.split("&")) {
        if (item.isEmpty()) {
            continue
        }
        val separator = item.indexOf('=')
        val name = if (separator == -1) item else item.substring(0, separator)
        val value = if (separator == -1) "" else item.substring(separator + 1)
        dictionary[rtmpPercentDecode(name)] = rtmpPercentDecode(value)
    }
    return dictionary
}

private fun rtmpUrlUser(uri: URI): String? {
    val userInfo = uri.userInfo ?: return null
    val separator = userInfo.indexOf(':')
    return if (separator == -1) userInfo else userInfo.substring(0, separator)
}

private fun rtmpUrlPassword(uri: URI): String? {
    val userInfo = uri.userInfo ?: return null
    val separator = userInfo.indexOf(':')
    return if (separator == -1) null else userInfo.substring(separator + 1)
}

private fun rtmpAbsoluteWithoutAuthenticationString(uri: URI): String {
    val string = uri.toString()
    val userInfo = uri.rawUserInfo ?: return string
    if (userInfo.isEmpty()) {
        return string
    }
    val index = string.indexOf("$userInfo@")
    if (index == -1) {
        return string
    }
    return string.substring(0, index) + string.substring(index + userInfo.length + 1)
}

fun makeSanJoseAuthCommand(url: URI, description: String): String {
    var command = url.toString()
    val index = description.indexOf('?')
    if (index == -1) {
        return command
    }
    val query = description.substring(index + 1)
    val challenge = String.format(Locale.US, "%08x", Random.nextInt())
    val dictionary = rtmpDictionaryFromQuery(query)
    val salt = dictionary["salt"] ?: return command
    val user = rtmpUrlUser(url) ?: return command
    val password = rtmpUrlPassword(url) ?: return command
    var response = calculateMd5Base64("$user$salt$password")
    val opaque = dictionary["opaque"]
    if (opaque != null) {
        command += "&opaque=$opaque"
        response += opaque
    } else {
        val challengeValue = dictionary["challenge"]
        if (challengeValue != null) {
            response += challengeValue
        }
    }
    response = calculateMd5Base64("$response$challenge")
    command += "&challenge=$challenge&response=$response"
    return command
}

interface RtmpStreamDelegate {
    fun rtmpStreamConnected(rtmpStream: RtmpStream)
    fun rtmpStreamDisconnected(rtmpStream: RtmpStream, reason: RtmpDisconnectReason)
}

sealed class RtmpDisconnectReason {
    data object connectFailed : RtmpDisconnectReason()
    data object connectTimeout : RtmpDisconnectReason()
    data object serverClosed : RtmpDisconnectReason()
    data object streamRejected : RtmpDisconnectReason()
    data object connectionLost : RtmpDisconnectReason()
    data class serverError(val code: String, val description: String?) : RtmpDisconnectReason()

    override fun toString(): String {
        return when (this) {
            connectFailed -> localized("Could not connect to the server. Check the URL and the network.")
            connectTimeout -> localized("Timed out connecting to the server. Check the URL and the network.")
            serverClosed -> localized("The server closed the connection. Check the URL.")
            streamRejected -> localized("The server rejected the stream. The stream key is likely wrong.")
            connectionLost -> localized("The connection to the server was lost.")
            is serverError -> {
                val error = listOfNotNull(code, description).joinToString(": ")
                if (code.startsWith("NetStream.Publish.")) {
                    localized("The server rejected the stream ($error). The stream key is likely wrong.")
                } else if (code == RtmpStatusCode.connectRejected.rawValue) {
                    localized("The server rejected the connection ($error). The URL or credentials are likely wrong.")
                } else {
                    localized("The server reported an error ($error).")
                }
            }
        }
    }
}

private enum class State {
    initialized,
    open,
    publishing,
}

class RtmpStream(
    val name: String,
    private val processor: Processor,
    val delegate: RtmpStreamDelegate?,
    private val queue: CoroutineDispatcher,
) : AudioVideoEncoderDelegate, RtmpSocketDelegate {
    val info = RtmpStreamInfo()
    private var streamId: UInt = 0u
    private var state: State = State.initialized
    private var startedAt: Instant = Instant.now()
    private var audioChunkType: RtmpChunkType = RtmpChunkType.zero
    private var videoChunkType: RtmpChunkType = RtmpChunkType.zero
    private val dataTimeStamps = mutableMapOf<String, Instant>()
    private var streamKey = ""
    private var url = ""
    private var uri: URI? = null
    var socket: RtmpSocket
        private set
    private var chunkReader = RtmpChunkReader()
    var callCompletions: MutableMap<Int, (List<AsValue>) -> Unit> = mutableMapOf()
    private var nextTransactionId = 0
    private val connectTimer: SimpleTimer
    private val statsTimer: SimpleTimer
    private val queueScope = CoroutineScope(queue + kotlinx.coroutines.SupervisorJob())

    private var timestamps = RtmpTimestamps()
    private var serverError: Pair<String, String?>? = null
    private var connectTimedOut = false
    private var audioMime: String? = null

    init {
        connectTimer = SimpleTimer(queue)
        statsTimer = SimpleTimer(queue)
        socket = RtmpSocket(name, queue)
    }

    fun setUrl(url: String) {
        streamKey = makeRtmpStreamKey(url)
        this.url = makeRtmpUri(url)
    }

    fun connect() {
        queueScope.launch {
            connectInternal()
        }
    }

    fun disconnect() {
        queueScope.launch {
            disconnectInternal()
        }
    }

    fun reconnectSoon() {
        queueScope.launch {
            delay(5000)
            connectInternal()
        }
    }

    private fun connectInternal() {
        connectTimedOut = false
        startConnectTimer()
        connectSocket(url)
    }

    private fun connectSocket(url: String) {
        serverError = null
        val uri = runCatching { URI(url) }.getOrNull() ?: return
        val scheme = uri.scheme ?: return
        val host = uri.host ?: return
        this.uri = uri
        chunkReader = RtmpChunkReader()
        socket = RtmpSocket(name, queue)
        socket.delegate = this
        if (scheme == "rtmps") {
            socket.connect(host, uri.port.takeIf { it != -1 } ?: 443, NWProtocolTLS.Options())
        } else {
            socket.connect(host, uri.port.takeIf { it != -1 } ?: 1935, null)
        }
    }

    private fun disconnectInternal() {
        statsTimer.stop()
        setState(State.initialized)
        stopConnectTimer()
        processorPipelineQueue.launch {
            processor.stopEncoding(this@RtmpStream)
        }
        socket.close(isDisconnected = false)
        socket = RtmpSocket(name, queue)
    }

    private fun call(
        commandName: RtmpCommandName,
        arguments: List<AsValue>,
        onCompleted: ((List<AsValue>) -> Unit)? = null,
    ) {
        val message = RtmpCommandMessage(
            streamId = 0u,
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

    private fun getNextTransactionId(): Int {
        nextTransactionId += 1
        return nextTransactionId
    }

    private fun handleStatus(code: String, description: String?) {
        Log.i(TAG, "rtmp: $name: Status $code")
        when (RtmpStatusCode.fromRawValue(code)) {
            RtmpStatusCode.connectSuccess -> handleConnectSuccess()
            RtmpStatusCode.connectRejected -> handleConnectRejected(description)
            RtmpStatusCode.connectFailed -> reportDisconnected(handshakeDone = false)
            RtmpStatusCode.connectClosed -> {
                reportDisconnected(handshakeDone = true)
                disconnectInternal()
            }
            RtmpStatusCode.publishStart -> {
                if (state != State.initialized) {
                    setState(State.publishing)
                }
            }
            null -> Unit
        }
    }

    private fun reportDisconnected(handshakeDone: Boolean) {
        val reason = makeDisconnectReason(handshakeDone)
        Log.i(TAG, "rtmp: $name: Disconnected: $reason")
        delegate?.rtmpStreamDisconnected(this, reason)
    }

    private fun makeDisconnectReason(handshakeDone: Boolean): RtmpDisconnectReason {
        val serverError = serverError
        if (serverError != null) {
            return RtmpDisconnectReason.serverError(serverError.first, serverError.second)
        }
        if (connectTimedOut) {
            return RtmpDisconnectReason.connectTimeout
        }
        return when (state) {
            State.publishing -> RtmpDisconnectReason.connectionLost
            State.open -> RtmpDisconnectReason.streamRejected
            State.initialized ->
                if (handshakeDone) RtmpDisconnectReason.serverClosed else RtmpDisconnectReason.connectFailed
        }
    }

    private fun handleConnectSuccess() {
        socket.maximumChunkSizeToServer = 1024 * 8
        socket.write(
            RtmpChunk(
                type = RtmpChunkType.zero,
                chunkStreamId = RtmpChunk.ChunkStreamId.control.rawValue,
                message = RtmpSetChunkSizeMessage(socket.maximumChunkSizeToServer.toUInt()),
            ),
        )
        setState(State.initialized)
        sendReleaseStream()
        sendFCPublish()
        sendCreateStream()
    }

    private fun handleConnectRejected(description: String?) {
        val currentUri = uri ?: return
        val user = rtmpUrlUser(currentUri) ?: return
        val password = rtmpUrlPassword(currentUri) ?: return
        val description = description ?: return
        socket.close(isDisconnected = false)
        if (description.contains("reason=nosuchuser")) {
        } else if (description.contains("reason=authfailed")) {
        } else if (description.contains("reason=needauth")) {
            connectSocket(makeSanJoseAuthCommand(currentUri, description))
        } else if (description.contains("authmod=adobe")) {
            if (user.isEmpty() || password.isEmpty()) {
                disconnectInternal()
            } else {
                val query = currentUri.query ?: ""
                val command = currentUri.toString() + (if (query.isEmpty()) "?" else "&") + "authmod=adobe&user=$user"
                connectSocket(command)
            }
        }
    }

    private fun makeConnectChunk(): RtmpChunk? {
        val uri = uri ?: return null
        var app = uri.path.orEmpty().trimStart('/')
        uri.query?.let { app += "?" + it }
        val message = RtmpCommandMessage(
            streamId = 0u,
            transactionId = getNextTransactionId(),
            commandType = RtmpMessageType.amf0Command,
            commandName = RtmpCommandName.connect,
            commandObject = mapOf(
                "app" to AsValue.String(app),
                "flashVer" to AsValue.String("FMLE/3.0 (compatible; FMSc/1.0)"),
                "swfUrl" to AsValue.Null,
                "tcUrl" to AsValue.String(rtmpAbsoluteWithoutAuthenticationString(uri)),
                "fpad" to AsValue.Bool(false),
                "capabilities" to AsValue.Number(239.0),
                "audioCodecs" to AsValue.Number(0x0400.toDouble()),
                "videoCodecs" to AsValue.Number(0x0080.toDouble()),
                "videoFunction" to AsValue.Number(1.0),
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
            disconnectInternal()
            return
        }
        socket.write(chunk)
        statsTimer.startPeriodic(1.0) {
            info.onTimeout()
        }
    }

    private fun handleClosed() {
        nextTransactionId = 0
        callCompletions.clear()
        chunkReader = RtmpChunkReader()
    }

    private fun processMessage(message: RtmpMessage) {
        when (message) {
            is RtmpSetChunkSizeMessage -> chunkReader.maximumChunkSize = message.size.toInt()
            is RtmpAcknowledgementMessage -> info.onAck(message.sequence)
            is RtmpUserControlMessage -> processMessageUserControl(message)
            is RtmpWindowAcknowledgementSizeMessage -> socket.write(
                RtmpChunk(
                    type = RtmpChunkType.zero,
                    chunkStreamId = RtmpChunk.ChunkStreamId.control.rawValue,
                    message = RtmpWindowAcknowledgementSizeMessage(100_000u),
                ),
            )
            is RtmpCommandMessage -> processMessageCommand(message)
            is RtmpDataMessage -> info.bitrateStats.value.add(bytesTransferred = message.encoded.size)
            else -> Unit
        }
    }

    private fun processMessageUserControl(message: RtmpUserControlMessage) {
        when (message.event) {
            RtmpUserControlMessage.Event.ping -> socket.write(
                RtmpChunk(
                    type = RtmpChunkType.zero,
                    chunkStreamId = RtmpChunk.ChunkStreamId.control.rawValue,
                    message = RtmpUserControlMessage(RtmpUserControlMessage.Event.pong, message.value),
                ),
            )
            else -> Unit
        }
    }

    private fun processMessageCommand(message: RtmpCommandMessage) {
        val completion = callCompletions.remove(message.transactionId)
        if (completion == null) {
            when (message.commandName) {
                RtmpCommandName.close -> disconnectInternal()
                else -> {
                    val data = (message.arguments.firstOrNull() as? AsValue.Object)?.value
                    val codeValue = data?.get("code")
                    if (codeValue is AsValue.String) {
                        val code = codeValue.value
                        var description: String? = null
                        val descriptionValue = data["description"]
                        if (descriptionValue is AsValue.String) {
                            description = descriptionValue.value
                        }
                        val level = data["level"]
                        if (level is AsValue.String && level.value == "error" &&
                            (code.startsWith("NetConnection.Connect.") || code.startsWith("NetStream."))
                        ) {
                            serverError = code to description
                        }
                        handleStatus(code, description)
                    }
                }
            }
            return
        }
        when (message.commandName) {
            RtmpCommandName.result -> completion(message.arguments.toList())
            else -> Unit
        }
    }

    private fun setState(state: State) {
        if (this.state == state) {
            return
        }
        val oldState = this.state
        this.state = state
        Log.i(TAG, "rtmp: $name: Stream state $oldState -> $state")
        if (oldState == State.publishing) {
            sendFCUnpublish()
            sendDeleteStream()
            sendCloseStream()
            processorPipelineQueue.launch {
                processor.stopEncoding(this@RtmpStream)
            }
        }
        when (state) {
            State.open -> handleStateChangeToOpen()
            State.publishing -> handleStateChangeToPublishing()
            else -> Unit
        }
    }

    private fun startConnectTimer() {
        connectTimer.startSingleShot(20.0) {
            Log.i(TAG, "rtmp: $name: Connect timeout")
            connectTimedOut = true
            if (socket.readyState == RtmpSocketReadyState.closed) {
                handleStatus(RtmpStatusCode.connectFailed.rawValue, null)
            } else {
                socket.close(isDisconnected = true)
            }
        }
    }

    private fun stopConnectTimer() {
        connectTimer.stop()
    }

    private fun send(handlerName: String, vararg arguments: AsValue) {
        if (state != State.publishing) {
            return
        }
        val dataWasSent = dataTimeStamps[handlerName] != null
        val elapsed = if (dataWasSent) {
            dataTimeStamps[handlerName]?.let { Duration.between(it, Instant.now()).toMillis() } ?: 0L
        } else {
            Duration.between(startedAt, Instant.now()).toMillis()
        }
        val timestamp = elapsed.coerceAtLeast(0L).toUInt()
        val chunk = RtmpChunk(
            type = if (dataWasSent) RtmpChunkType.one else RtmpChunkType.zero,
            chunkStreamId = RtmpChunk.ChunkStreamId.`data`.rawValue,
            message = RtmpDataMessage(
                streamId = streamId,
                dataType = RtmpMessageType.amf0Data,
                timestamp = timestamp,
                handlerName = handlerName,
                arguments = arguments.toList(),
            ),
        )
        val length = socket.write(chunk)
        dataTimeStamps[handlerName] = Instant.now()
        info.bitrateStats.value.add(bytesTransferred = length)
    }

    private fun createOnMetaData(): AsObject {
        val audioEncoder = processor.getAudioEncoder()
        val videoEncoder = processor.getVideoEncoder()
        val metadata: MutableMap<String, AsValue> = mutableMapOf()
        val settings = videoEncoder.settings.value
        metadata["width"] = AsValue.Number(settings.videoSize.width.toDouble())
        metadata["height"] = AsValue.Number(settings.videoSize.height.toDouble())
        metadata["framerate"] = AsValue.Number(processor.getFps())
        when (settings.format) {
            VideoEncoderSettings.Format.h264 ->
                metadata["videocodecid"] = AsValue.Number(FlvVideoCodec.avc.rawValue.toInt().toDouble())
            VideoEncoderSettings.Format.hevc ->
                metadata["videocodecid"] = AsValue.Number(FlvVideoFourCC.hevc.rawValue.toInt().toDouble())
            else -> Unit
        }
        metadata["videodatarate"] = AsValue.Number(settings.bitrate.toDouble() / 1000)
        metadata["audiocodecid"] = AsValue.Number(FlvAudioCodec.aac.rawValue.toInt().toDouble())
        metadata["audiodatarate"] = AsValue.Number(audioEncoder.getBitrate().toDouble() / 1000)
        audioEncoder.getSampleRate()?.let {
            metadata["audiosamplerate"] = AsValue.Number(it.toDouble())
        }
        return metadata
    }

    private fun handleStateChangeToOpen() {
        info.clear()
        socket.write(
            RtmpChunk(
                message = RtmpCommandMessage(
                    streamId = streamId,
                    transactionId = getNextTransactionId(),
                    commandType = RtmpMessageType.amf0Command,
                    commandName = RtmpCommandName.publish,
                    commandObject = null,
                    arguments = listOf(AsValue.String(streamKey), AsValue.String("live")),
                ),
            ),
        )
        startedAt = Instant.now()
        timestamps = RtmpTimestamps()
        videoChunkType = RtmpChunkType.zero
        audioChunkType = RtmpChunkType.zero
        dataTimeStamps.clear()
    }

    private fun handleStateChangeToPublishing() {
        send("@setDataFrame", AsValue.String("onMetaData"), AsValue.Object(createOnMetaData()))
        stopConnectTimer()
        serverError = null
        delegate?.rtmpStreamConnected(this)
        processorPipelineQueue.launch {
            processor.startEncoding(this@RtmpStream)
        }
    }

    private fun sendCreateStream() {
        call(RtmpCommandName.createStream, listOf()) { arguments ->
            if (arguments.isEmpty()) {
                return@call
            }
            val id = (arguments[0] as? AsValue.Number)?.value ?: return@call
            if (id < 0.0 || id > UInt.MAX_VALUE.toDouble()) {
                return@call
            }
            streamId = id.toUInt()
            setState(State.open)
        }
    }

    private fun sendReleaseStream() {
        call(RtmpCommandName.releaseStream, listOf(AsValue.String(streamKey)))
    }

    private fun sendFCPublish() {
        call(RtmpCommandName.fcPublish, listOf(AsValue.String(streamKey)))
    }

    private fun sendFCUnpublish() {
        call(RtmpCommandName.fcUnpublish, listOf(AsValue.String(streamKey)))
    }

    private fun sendDeleteStream() {
        socket.write(
            RtmpChunk(
                message = RtmpCommandMessage(
                    streamId = streamId,
                    transactionId = 0,
                    commandType = RtmpMessageType.amf0Command,
                    commandName = RtmpCommandName.deleteStream,
                    commandObject = null,
                    arguments = listOf(AsValue.Number(streamId.toDouble())),
                ),
            ),
        )
    }

    private fun sendCloseStream() {
        socket.write(
            RtmpChunk(
                type = RtmpChunkType.zero,
                chunkStreamId = RtmpChunk.ChunkStreamId.command.rawValue,
                message = RtmpCommandMessage(
                    streamId = 0u,
                    transactionId = 0,
                    commandType = RtmpMessageType.amf0Command,
                    commandName = RtmpCommandName.closeStream,
                    commandObject = null,
                    arguments = listOf(AsValue.Number(streamId.toDouble())),
                ),
            ),
        )
    }

    private fun handleEncodedAudioBuffer(buffer: ByteArray, timestamp: UInt) {
        if (state != State.publishing) {
            return
        }
        val length = socket.write(
            RtmpChunk(
                type = audioChunkType,
                chunkStreamId = FlvTagType.audio.streamId,
                message = RtmpAudioMessage(streamId = streamId, timestamp = timestamp, payload = buffer),
            ),
        )
        audioChunkType = RtmpChunkType.one
        info.bitrateStats.value.add(bytesTransferred = length)
    }

    private fun handleEncodedVideoBuffer(buffer: ByteArray, timestamp: UInt) {
        if (state != State.publishing) {
            return
        }
        val length = socket.write(
            RtmpChunk(
                type = videoChunkType,
                chunkStreamId = FlvTagType.video.streamId,
                message = RtmpVideoMessage(streamId = streamId, timestamp = timestamp, payload = buffer),
            ),
        )
        videoChunkType = RtmpChunkType.one
        info.bitrateStats.value.add(bytesTransferred = length)
    }

    private fun audioEncoderOutputFormatInternal(format: MediaFormat) {
        audioMime = format.getString(MediaFormat.KEY_MIME)
        val buffer = makeRtmpAudioSequenceHeader(format) ?: return
        handleEncodedAudioBuffer(buffer, 0u)
    }

    private fun audioEncoderOutputBufferInternal(audioBuffer: MediaSample, presentationTimeUs: Long) {
        val timestamp = timestamps.audio(presentationTimeUs / 1_000_000.0)
        if (timestamp == null) {
            Log.i(TAG, "rtmp: $name: Dropping audio buffer")
            return
        }
        handleEncodedAudioBuffer(
            makeRtmpAudioFrame(audioBuffer, audioBuffer.format?.getString(MediaFormat.KEY_MIME) ?: audioMime),
            timestamp,
        )
    }

    private fun videoEncoderOutputFormatInternal(
        format: VideoEncoderSettings.Format,
        formatDescription: MediaFormat,
    ) {
        val buffer = makeRtmpVideoSequenceHeader(format, formatDescription) ?: return
        handleEncodedVideoBuffer(buffer, 0u)
    }

    private fun videoEncoderOutputSampleBufferInternal(
        format: VideoEncoderSettings.Format,
        sampleBuffer: MediaSample,
    ) {
        val decodeTimeStampUs = if (sampleBuffer.decodeTimeStampUs >= 0) {
            sampleBuffer.decodeTimeStampUs
        } else {
            sampleBuffer.presentationTimeUs
        }
        val timestamp = timestamps.video(decodeTimeStampUs / 1_000_000.0)
        if (timestamp == null) {
            Log.i(TAG, "rtmp: $name: Dropping video buffer")
            return
        }
        handleEncodedVideoBuffer(makeRtmpVideoFrame(format, sampleBuffer, sampleBuffer.data), timestamp)
    }

    override fun audioEncoderOutputFormat(format: MediaFormat) {
        queueScope.launch {
            audioEncoderOutputFormatInternal(format)
        }
    }

    override fun audioEncoderOutputBuffer(buffer: MediaSample, presentationTimeStamp: Long) {
        queueScope.launch {
            audioEncoderOutputBufferInternal(buffer, presentationTimeStamp)
        }
    }

    override fun videoEncoderOutputFormat(encoder: VideoEncoder, formatDescription: MediaFormat) {
        val format = encoder.settings.value.format
        queueScope.launch {
            videoEncoderOutputFormatInternal(format, formatDescription)
        }
    }

    override fun videoEncoderOutputSampleBuffer(
        codec: VideoEncoder,
        sampleBuffer: MediaSample,
        presentationTimeUs: Long,
    ) {
        val format = codec.settings.value.format
        queueScope.launch {
            videoEncoderOutputSampleBufferInternal(format, sampleBuffer)
        }
    }

    override fun socketHandshakeDone(socket: RtmpSocket) {
        if (socket !== this.socket) {
            return
        }
        handleHandshakeDone()
    }

    override fun socketClosed(socket: RtmpSocket) {
        if (socket !== this.socket) {
            return
        }
        handleClosed()
    }

    override fun socketDisconnected(socket: RtmpSocket, code: RtmpStatusCode) {
        if (socket !== this.socket) {
            return
        }
        handleStatus(code.rawValue, null)
    }

    override fun socketDataReceived(socket: RtmpSocket, data: ByteArray): ByteArray {
        return chunkReader.read(data) { message ->
            if (socket === this.socket && socket.readyState != RtmpSocketReadyState.closed) {
                processMessage(message)
            }
        }
    }

    override fun socketUpdateStats(socket: RtmpSocket, totalBytesSent: Long) {
        if (socket !== this.socket) {
            return
        }
        info.onWritten(totalBytesSent)
    }
}

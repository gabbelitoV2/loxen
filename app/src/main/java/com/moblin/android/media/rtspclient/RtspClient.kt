package com.moblin.android.media.rtspclient

import android.media.MediaFormat
import android.util.Log
import com.moblin.android.localized
import com.moblin.android.media.MediaSample
import com.moblin.android.media.WrappingTimestamp
import com.moblin.android.media.haishinkit.codec.video.VideoDecoder
import com.moblin.android.media.haishinkit.codec.video.VideoDecoderDelegate
import com.moblin.android.media.haishinkit.mpeg.avc.AvcNalUnit
import com.moblin.android.media.haishinkit.mpeg.avc.AvcNalUnitType
import com.moblin.android.media.haishinkit.mpeg.avc.makeFormatDescription
import com.moblin.android.media.haishinkit.mpeg.hevc.HevcNalUnit
import com.moblin.android.media.haishinkit.mpeg.hevc.HevcNalUnitType
import com.moblin.android.media.haishinkit.mpeg.hevc.makeFormatDescription
import com.moblin.android.media.haishinkit.mpeg.nalUnitStartCode
import com.moblin.android.media.haishinkit.util.BitrateStats
import com.moblin.android.media.haishinkit.util.BitrateStatsInstant
import com.moblin.android.media.haishinkit.util.calculateMd5
import com.moblin.android.various.SimpleTimer
import com.moblin.android.various.settings.SettingsRtspTransport
import com.moblin.android.various.utils.TimeStampRebaser
import com.moblin.android.various.utils.currentPresentationTimeStamp
import java.net.URI
import java.util.Base64
import java.util.UUID
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private const val TAG = "RtspClient"

private const val rtpH264PacketTypeFuA = 28

val rtspClientQueue: CoroutineDispatcher = Executors
    .newSingleThreadExecutor { runnable -> Thread(runnable, "com.moblin.android.rtsp") }
    .asCoroutineDispatcher()

private val rtspClientScope = CoroutineScope(rtspClientQueue)

interface RtspClientDelegate {
    fun rtspClientErrorToast(title: String)

    fun rtspClientConnected(cameraId: UUID)

    fun rtspClientDisconnected(cameraId: UUID)

    fun rtspClientOnVideoBuffer(cameraId: UUID, sampleBuffer: MediaSample)
}

private data class SdpVideoH264(val sps: AvcNalUnit, val pps: AvcNalUnit)

private data class SdpVideoH265(val vps: HevcNalUnit, val sps: HevcNalUnit, val pps: HevcNalUnit)

private sealed class SdpVideoCodec {
    class H264(val video: SdpVideoH264) : SdpVideoCodec()

    class H265(val video: SdpVideoH265) : SdpVideoCodec()
}

private class SdpVideo {
    var control: URI? = null
    var codec: SdpVideoCodec? = null
}

private class SdpLinesReader(private val lines: List<String>) {
    private var nextIndex = 0

    fun next(): Pair<String, String>? {
        if (nextIndex >= lines.size) {
            return null
        }
        val line = lines[nextIndex]
        Log.d(TAG, "rtsp-client: SDP line ${line.trim()}")
        nextIndex += 1
        return partition(line, "=")
    }

    fun back() {
        if (nextIndex > 0) {
            nextIndex -= 1
        }
    }
}

private class SdpMediaDescription(
    val media: String,
    val port: String,
    val proto: String,
    var attributes: MutableList<SdpAttribute> = mutableListOf(),
) {
    fun getValue(attributeName: String): String {
        val attribute = attributes.firstOrNull { it.attribute == attributeName }
            ?: throw Exception("Attribute $attributeName is missing.")
        return attribute.value ?: throw Exception("Attribute $attributeName has no value.")
    }
}

private data class SdpAttribute(var attribute: String, var value: String?)

private class SdpLinesParser(value: String) {
    private val mediaDescriptions = mutableListOf<SdpMediaDescription>()

    init {
        parse(value)
    }

    fun getMediaDescriptions(): List<SdpMediaDescription> {
        return mediaDescriptions
    }

    private fun parse(value: String) {
        val linesReader = SdpLinesReader(value.split("\r\n").filter { it.isNotEmpty() })
        while (true) {
            val (kind, lineValue) = linesReader.next() ?: break
            when (kind) {
                "m" -> parseMedia(lineValue, linesReader)
                else -> {}
            }
        }
    }

    private fun parseMedia(value: String, linesReader: SdpLinesReader) {
        val parts = value.split(" ").filter { it.isNotEmpty() }
        if (parts.size < 3) {
            throw Exception("Bad media description $value")
        }
        val mediaDescription = SdpMediaDescription(parts[0], parts[1], parts[2])
        mediaLoop@ while (true) {
            val (kind, lineValue) = linesReader.next() ?: break@mediaLoop
            when (kind) {
                "m" -> {
                    linesReader.back()
                    break@mediaLoop
                }
                "a" -> parseAttribute(lineValue, mediaDescription)
                else -> {}
            }
        }
        mediaDescriptions.add(mediaDescription)
    }

    private fun parseAttribute(value: String, mediaDescription: SdpMediaDescription) {
        val (attribute, attributeValue) = partitionOptional(value, ":")
        mediaDescription.attributes.add(SdpAttribute(attribute = attribute, value = attributeValue))
    }
}

private class Sdp(value: String) {
    var video: SdpVideo? = null

    init {
        val linesParser = SdpLinesParser(value)
        parse(linesParser)
    }

    private fun parse(linesParser: SdpLinesParser) {
        for (mediaDescription in linesParser.getMediaDescriptions()) {
            when (mediaDescription.media) {
                "video" -> {
                    val video = SdpVideo()
                    val rtpmap = mediaDescription.getValue("rtpmap")
                    val fmtp = mediaDescription.getValue("fmtp")
                    if (rtpmap.contains("H264")) {
                        parseVideoAttributeFmtpH264(fmtp, video)
                    } else if (rtpmap.contains("H265")) {
                        parseVideoAttributeFmtpH265(fmtp, video)
                    } else {
                        throw Exception("Unsupported codec in rtpmap: $rtpmap")
                    }
                    video.control = URI(mediaDescription.getValue("control"))
                    this.video = video
                }
                else -> {}
            }
        }
    }

    private fun parseVideoAttributeFmtpH264(value: String, video: SdpVideo) {
        val fmtpValue = partition(value, " ").second
        for (part in fmtpValue.split(Regex(";\\s*"))) {
            val (name, parameterValue) = partition(part, "=")
            when (name) {
                "sprop-parameter-sets" -> parseVideoAttributeFmtpSpropParameterSets(parameterValue, video)
                else -> {}
            }
        }
    }

    private fun parseVideoAttributeFmtpSpropParameterSets(value: String, video: SdpVideo) {
        val (spsBase64, ppsBase64) = partition(value, ",")
        val sps = runCatching { Base64.getDecoder().decode(spsBase64) }.getOrNull()
            ?: throw Exception("Failed to decode SPS.")
        val pps = runCatching { Base64.getDecoder().decode(ppsBase64) }.getOrNull()
            ?: throw Exception("Failed to decode PPS.")
        val spsNalUnit = AvcNalUnit.create(sps, 0) ?: throw Exception("Failed to parse SPS NAL unit.")
        val ppsNalUnit = AvcNalUnit.create(pps, 0) ?: throw Exception("Failed to parse PPS NAL unit.")
        video.codec = SdpVideoCodec.H264(SdpVideoH264(sps = spsNalUnit, pps = ppsNalUnit))
    }

    private fun parseVideoAttributeFmtpH265(value: String, video: SdpVideo) {
        var vps: HevcNalUnit? = null
        var sps: HevcNalUnit? = null
        var pps: HevcNalUnit? = null
        val fmtpValue = partition(value, " ").second
        for (part in fmtpValue.split(Regex(";\\s*"))) {
            val (name, parameterValue) = partition(part, "=")
            when (name) {
                "sprop-vps" -> vps = parseVideoAttributeFmtpSpropVpsSpsPps(parameterValue)
                "sprop-sps" -> sps = parseVideoAttributeFmtpSpropVpsSpsPps(parameterValue)
                "sprop-pps" -> pps = parseVideoAttributeFmtpSpropVpsSpsPps(parameterValue)
                else -> {}
            }
        }
        if (vps == null || sps == null || pps == null) {
            throw Exception("VPS, SPS or PPS missing.")
        }
        video.codec = SdpVideoCodec.H265(SdpVideoH265(vps = vps, sps = sps, pps = pps))
    }

    private fun parseVideoAttributeFmtpSpropVpsSpsPps(value: String): HevcNalUnit {
        val data = runCatching { Base64.getDecoder().decode(value) }.getOrNull()
            ?: throw Exception("Failed to decode VPS, SPS or PPS.")
        return HevcNalUnit(data, 0) ?: throw Exception("Failed to parse VPS, SPS or PPS unit.")
    }
}

private fun partition(text: String, separator: String): Pair<String, String> {
    val (first, second) = partitionOptional(text, separator)
    return Pair(first, second ?: throw Exception("Cannot partition '$text'"))
}

private fun partitionOptional(text: String, optionalSeparator: String): Pair<String, String?> {
    val parts = text.split(optionalSeparator, limit = 2)
    return when (parts.size) {
        1 -> Pair(parts[0], null)
        2 -> Pair(parts[0], parts[1].trim())
        else -> throw Exception("Cannot partition '$text'")
    }
}

private class Request(
    val method: String,
    val url: URI,
    var headers: MutableMap<String, String> = mutableMapOf(),
    val content: ByteArray? = null,
    val completion: (Response) -> Unit,
) {
    var dueToAuthenticationFailure = false

    fun pack(cSeq: Int): ByteArray {
        var request = "$method $url RTSP/1.0\r\n"
        request += "CSeq: $cSeq\r\n"
        for ((name, value) in headers) {
            request += "$name: $value\r\n"
        }
        request += "\r\n"
        Log.d(TAG, "rtsp-client: Sending header $request")
        return request.toByteArray(Charsets.UTF_8)
    }
}

private class Response(
    val statusCode: Int,
    val headers: Map<String, String>,
    var content: ByteArray? = null,
)

private enum class State {
    DISCONNECTED,
    CONNECTING,
    SETUP,
    STREAMING,
}

private fun md5String(data: String): String {
    return calculateMd5(data).joinToString("") { "%02x".format(it.toInt() and 0xFF) }
}

fun URI.removeCredentialsAndPort(): URI {
    return URI(scheme, null, host, -1, path, query, fragment)
}

private open class RtpProcessor {
    open fun stop() {}
    open fun process(packet: ByteArray, timestamp: Long) {
        throw Exception("Not implemented")
    }
}

private open class RtpVideoProcessor(
    formatDescription: MediaFormat,
    client: RtspClient,
) : RtpProcessor(), VideoDecoderDelegate {
    private var timestamp: Long = 0
    var data = ByteArray(0)
    private var basePresentationTimeStamp: Double = -1.0
    private var timeStampRebaser = TimeStampRebaser()
    private val decoder: VideoDecoder
    private var formatDescription: MediaFormat?
    private val client: RtspClient
    override fun stop() { decoder.delegate = null; decoder.stopRunning() }

    init {
        this.formatDescription = formatDescription
        this.client = client
        decoder = VideoDecoder(
            name = "rtsp-client",
            lockQueue = rtspClientScope,
            softwareDecoding = client.softwareDecoding,
        )
        decoder.delegate = this
        decoder.startRunning(formatDescription)
    }

    fun startNewFrame(timestamp: Long, first: ByteArray, second: ByteArray? = null) {
        this.timestamp = timestamp
        data = ByteArray(0)
        data = data + nalUnitStartCode
        data = data + first
        if (second != null) {
            data = data + second
        }
    }

    fun tryDecodeFrame() {
        if (data.size <= 4) {
            return
        }
        val count = data.size - 4
        data[0] = (count ushr 24).toByte()
        data[1] = (count ushr 16).toByte()
        data[2] = (count ushr 8).toByte()
        data[3] = count.toByte()
        var presenationTimeStamp = timestamp.toDouble() / 90000
        val rebasedPresentationTimeStamp = timeStampRebaser.rebase(presenationTimeStamp) ?: return
        presenationTimeStamp = getBasePresentationTimeStamp() + rebasedPresentationTimeStamp + client.latency
        val sample = MediaSample(
            data = data,
            presentationTimeUs = (presenationTimeStamp * 1_000_000.0).toLong(),
            isKeyFrame = false,
            format = formatDescription,
        )
        decoder.decodeSampleBuffer(sample)
    }

    private fun getBasePresentationTimeStamp(): Double {
        if (basePresentationTimeStamp == -1.0) {
            basePresentationTimeStamp = currentPresentationTimeStamp().toDouble() / 1_000_000.0
        }
        return basePresentationTimeStamp
    }

    override fun videoDecoderOutputSampleBuffer(decoder: VideoDecoder, sampleBuffer: MediaSample) {
        client.videoOutputSampleBuffer(sampleBuffer)
    }
}

private class RtpProcessorVideoH264(
    formatDescription: MediaFormat,
    client: RtspClient,
) : RtpVideoProcessor(formatDescription, client) {
    override fun process(packet: ByteArray, timestamp: Long) {
        if (packet.size < 14) {
            throw Exception("Packet shorter than 14 bytes: $packet")
        }
        val type = packet[12].toInt() and 0x1F
        when (type) {
            in 1..23 -> processBufferTypeSingle(packet, timestamp)
            24 -> processBufferTypeStapA(packet, timestamp)
            rtpH264PacketTypeFuA -> processBufferTypeFuA(packet, timestamp)
            else -> throw Exception("Unsupported RTP packet type $type.")
        }
    }

    private fun processBufferTypeSingle(packet: ByteArray, timestamp: Long) {
        decodeFrame()
        startNewFrame(timestamp, packet.copyOfRange(12, packet.size))
    }

    private fun processBufferTypeStapA(packet: ByteArray, timestamp: Long) {
        var offset = 13
        while (offset < packet.size) {
            if (offset + 2 > packet.size) {
                throw Exception("STAP-A packet short NAL header")
            }
            val nalUnitSize = ((packet[offset].toInt() and 0xFF) shl 8) or
                (packet[offset + 1].toInt() and 0xFF)
            offset += 2
            if (offset + nalUnitSize > packet.size) {
                throw Exception("STAP-A packet short NAL data")
            }
            decodeFrame()
            startNewFrame(timestamp, packet.copyOfRange(offset, offset + nalUnitSize))
            offset += nalUnitSize
        }
    }

    private fun processBufferTypeFuA(packet: ByteArray, timestamp: Long) {
        val fuIndicator = packet[12].toInt() and 0xFF
        val fuHeader = packet[13].toInt() and 0xFF
        val startBit = fuHeader shr 7
        val nalType = fuHeader and 0x1F
        val nal = (fuIndicator and 0xE0) or nalType
        if (startBit == 1) {
            decodeFrame()
            startNewFrame(timestamp, byteArrayOf(nal.toByte()), packet.copyOfRange(14, packet.size))
        } else {
            data = data + packet.copyOfRange(14, packet.size)
        }
    }

    private fun decodeFrame() {
        if (data.size <= 4) {
            return
        }
        if (!AvcNalUnitType.isPicture((data[4].toInt() and 0x1F).toUByte())) {
            return
        }
        tryDecodeFrame()
    }
}

private class RtpProcessorVideoH265(
    formatDescription: MediaFormat,
    client: RtspClient,
) : RtpVideoProcessor(formatDescription, client) {
    override fun process(packet: ByteArray, timestamp: Long) {
        if (packet.size < 14) {
            throw Exception("Packet shorter than 14 bytes: $packet")
        }
        val type = (packet[12].toInt() and 0xFF) shr 1 and 0x3F
        when (type) {
            in 1..47 -> processBufferTypeSingle(packet, timestamp)
            48 -> processBufferTypeAp(packet, timestamp)
            49 -> processBufferTypeFu(packet, timestamp)
            else -> throw Exception("Unsupported RTP packet type $type.")
        }
    }

    private fun processBufferTypeSingle(packet: ByteArray, timestamp: Long) {
        decodeFrame()
        startNewFrame(timestamp, packet.copyOfRange(12, packet.size))
    }

    private fun processBufferTypeAp(packet: ByteArray, timestamp: Long) {
        var offset = 14
        while (offset < packet.size) {
            if (offset + 2 > packet.size) {
                throw Exception("AP packet short NAL header")
            }
            val nalUnitSize = ((packet[offset].toInt() and 0xFF) shl 8) or
                (packet[offset + 1].toInt() and 0xFF)
            offset += 2
            if (offset + nalUnitSize > packet.size) {
                throw Exception("AP packet short NAL data")
            }
            decodeFrame()
            startNewFrame(timestamp, packet.copyOfRange(offset, offset + nalUnitSize))
            offset += nalUnitSize
        }
    }

    private fun processBufferTypeFu(packet: ByteArray, timestamp: Long) {
        if (packet.size < 15) {
            throw Exception("Packet shorter than 15 bytes: $packet")
        }
        val fuHeader = packet[14].toInt() and 0xFF
        val startBit = fuHeader shr 7
        val nalType = fuHeader and 0x3F
        val nal = (packet[12].toInt() and 0x81) or (nalType shl 1)
        if (startBit == 1) {
            decodeFrame()
            startNewFrame(
                timestamp,
                byteArrayOf(nal.toByte(), packet[13]),
                packet.copyOfRange(15, packet.size),
            )
        } else {
            data = data + packet.copyOfRange(15, packet.size)
        }
    }

    private fun decodeFrame() {
        if (data.size <= 4) {
            return
        }
        if (!HevcNalUnitType.isPicture(((data[4].toInt() and 0xFF) shr 1 and 0x3F).toUByte())) {
            return
        }
        tryDecodeFrame()
    }
}

private class Rtp {
    private var nextExpectedSequenceNumber: Int? = null
    private val reorderBuffer = mutableMapOf<Int, ByteArray>()
    private val reorderBufferMaxSize = 64
    var processor: RtpProcessor? = null; set(value) { field?.stop(); field = value }
    private val wrappingTimestamp = WrappingTimestamp(name = "RTP", maximumTimestamp = 0x1_0000_0000L)

    fun handlePacket(packet: ByteArray) {
        if (packet.size < 12) {
            throw Exception("Packet shorter than 12 bytes: $packet")
        }
        val value = packet[0].toInt() and 0xFF
        val version = value shr 6
        val x = (value shr 4) and 0x1
        val cc = value and 0xF
        val sequenceNumber = ((packet[2].toInt() and 0xFF) shl 8) or (packet[3].toInt() and 0xFF)
        if (version != 2) {
            throw Exception("Unsupported version $version")
        }
        if (x != 0) {
            throw Exception("Unsupported x $x")
        }
        if (cc != 0) {
            throw Exception("Unsupported cc $cc")
        }
        if (nextExpectedSequenceNumber == null) {
            nextExpectedSequenceNumber = sequenceNumber
        }
        val expected = nextExpectedSequenceNumber!!
        val sequenceDelta = (sequenceNumber - expected) and 0xFFFF
        if (sequenceDelta == 0) {
            processPacket(packet)
            nextExpectedSequenceNumber = (expected + 1) and 0xFFFF
            drainReorderBuffer()
        } else if (sequenceDelta < 0x8000) {
            if (reorderBuffer.size < reorderBufferMaxSize) {
                reorderBuffer[sequenceNumber] = packet
            } else {
                reorderBuffer.clear()
                processPacket(packet)
                nextExpectedSequenceNumber = (sequenceNumber + 1) and 0xFFFF
            }
        }
    }

    private fun drainReorderBuffer() {
        while (true) {
            val expected = nextExpectedSequenceNumber ?: break
            val packet = reorderBuffer.remove(expected) ?: break
            processPacket(packet)
            nextExpectedSequenceNumber = (expected + 1) and 0xFFFF
        }
    }

    private fun processPacket(packet: ByteArray) {
        val a = packet[4].toInt() and 0xFF
        val b = packet[5].toInt() and 0xFF
        val c = packet[6].toInt() and 0xFF
        val d = packet[7].toInt() and 0xFF
        val timestamp = (a shl 24) or (b shl 16) or (c shl 8) or d
        processor?.process(packet, updateTimestamp(timestamp.toLong() and 0xFFFFFFFFL))
    }

    private fun updateTimestamp(timestamp: Long): Long {
        return wrappingTimestamp.update(timestamp)
    }
}

class RtspClient(
    private val cameraId: UUID,
    private var url: URI,
    internal val latency: Double,
    transport: SettingsRtspTransport,
    internal val softwareDecoding: Boolean,
    private val delegate: RtspClientDelegate,
) : RtspTransportDelegate {
    private var state = State.DISCONNECTED
    private var transport: RtspTransport? = null
    private val username: String?
    private val password: String?
    private val port: Int
    private var realm: String? = null
    private var nonce: String? = null
    private var nextCSeq = 0
    private val requests = mutableMapOf<Int, Request>()
    private var videoSession: String? = null
    private var rtpVideo = Rtp()
    private var connectTimer = SimpleTimer(rtspClientQueue)
    private var keepAliveTimer = SimpleTimer(rtspClientQueue)
    private var reconnectTimer = SimpleTimer(rtspClientQueue)
    private var started = false
    private var isAlive = true
    private val bitrateStats = BitrateStats()
    private val transportType: SettingsRtspTransport = transport

    init {
        val userInfo = url.userInfo
        if (userInfo != null && userInfo.contains(":")) {
            username = userInfo.substringBefore(":")
            password = userInfo.substringAfter(":")
        } else {
            username = userInfo
            password = null
        }
        port = if (url.port == -1) 554 else url.port
        url = url.removeCredentialsAndPort()
    }

    fun start() {
        Log.d(TAG, "rtsp-client: Start")
        rtspClientScope.launch {
            started = true
            startInternal()
        }
    }

    fun stop() {
        Log.d(TAG, "rtsp-client: Stop")
        rtspClientScope.launch {
            started = false
            stopInternal()
            rtpVideo.processor = null
        }
    }

    fun updateStats(): BitrateStatsInstant {
        return runBlocking(rtspClientQueue) {
            bitrateStats.update()
        }
    }

    private fun setState(newState: State) {
        if (newState == state) {
            return
        }
        Log.d(TAG, "rtsp-client: State change $state -> $newState")
        when (newState) {
            State.DISCONNECTED -> {
                if (state == State.STREAMING) {
                    delegate.rtspClientDisconnected(cameraId)
                }
            }
            State.STREAMING -> delegate.rtspClientConnected(cameraId)
            else -> {}
        }
        state = newState
    }

    private fun startInternal() {
        if (!started) {
            return
        }
        stopInternal()
        val host = url.host ?: return
        Log.i(TAG, "rtsp-debug: Connecting to $host:$port with transport $transportType")
        transport = createTransport()
        transport?.delegate = this
        transport?.start(host, port)
        rtpVideo.processor = null
        rtpVideo = Rtp()
        setState(State.CONNECTING)
        connectTimer.startSingleShot(5.0) {
            reconnectSoon()
        }
        isAlive = true
        realm = null
        nonce = null
        nextCSeq = 0
        requests.clear()
        videoSession = null
    }

    private fun stopInternal() {
        connectTimer.stop()
        keepAliveTimer.stop()
        reconnectTimer.stop()
        transport?.stop()
        transport = null
        setState(State.DISCONNECTED)
    }

    private fun reconnectSoon() {
        stopInternal()
        reconnectTimer.startSingleShot(5.0) {
            startInternal()
        }
    }

    private fun getNextCSeq(): Int {
        nextCSeq += 1
        return nextCSeq
    }

    private fun createDigestHeader(request: Request): String? {
        val realm = this.realm ?: return null
        val nonce = this.nonce ?: return null
        val username = this.username ?: return null
        val password = this.password ?: return null
        val ha1 = md5String("$username:$realm:$password")
        val ha2 = md5String("${request.method}:$url")
        val response = md5String("$ha1:$nonce:$ha2")
        return "Digest username=\"$username\", realm=\"$realm\", nonce=\"$nonce\", " +
            "uri=\"$url\", response=\"$response\""
    }

    private fun perform(request: Request) {
        val cSeq = getNextCSeq()
        requests[cSeq] = request
        createDigestHeader(request)?.let { authorization ->
            request.headers["Authorization"] = authorization
        }
        send(request.pack(cSeq))
    }

    private fun handleRtcpVideoPacket(packet: ByteArray) {
        if (packet.size < 8) {
            return
        }
        val value = packet[0].toInt() and 0xFF
        val version = value shr 6
        val pt = packet[1].toInt() and 0xFF
        if (version != 2) {
            Log.d(TAG, "rtsp-client: Unsupported version $version")
            return
        }
        if (pt == 200) {
            val receiverReport = ByteArray(8)
            receiverReport[0] = (2 shl 6).toByte()
            receiverReport[1] = 201.toByte()
            receiverReport[3] = 1
            sendRtcp(receiverReport)
        }
    }

    private fun sendRtcp(data: ByteArray) {
        transport?.sendRtcp(data)
    }

    private fun send(data: ByteArray) {
        transport?.sendRtsp(data)
    }

    private fun handleRtspMessage(header: ByteArray, content: ByteArray?) {
        val headerString = header.toString(Charsets.UTF_8)
        Log.d(TAG, "rtsp-client: Got header $headerString")
        val lines = headerString.split("\r\n").filter { it.isNotEmpty() }
        if (lines.isEmpty()) {
            throw Exception("Status line missing.")
        }
        val statusLine = Regex("^RTSP/1\\.0 (\\d+) .*$").matchEntire(lines[0])
            ?: throw Exception("Invalid status line '${lines[0]}'.")
        val statusCode = statusLine.groupValues[1].toIntOrNull()
            ?: throw Exception("Status code not an integer.")
        val headers = mutableMapOf<String, String>()
        for (line in lines.drop(1)) {
            val (name, value) = partition(line, ":")
            headers[name.lowercase()] = value
        }
        val response = Response(statusCode, headers)
        response.content = content
        handleResponse(response)
    }

    private fun handleResponse(response: Response) {
        val cSeqString = response.headers["cseq"] ?: throw Exception("No request found for response.")
        val cSeq = cSeqString.toIntOrNull() ?: throw Exception("No request found for response.")
        val request = requests.remove(cSeq) ?: throw Exception("No request found for response.")
        if (response.statusCode == 401) {
            handleUnauthorizedResponse(request, response)
            request.dueToAuthenticationFailure = true
            perform(request)
            return
        }
        request.completion(response)
    }

    private fun handleUnauthorizedResponse(request: Request, response: Response) {
        if (request.dueToAuthenticationFailure) {
            delegate.rtspClientErrorToast(localized("Wrong RTSP username or password"))
            throw Exception("Wrong username or password")
        }
        if (username == null || password == null) {
            delegate.rtspClientErrorToast(localized("RTSP username or password missing"))
            throw Exception("Username or password missing.")
        }
        val wwwAuthenticate = response.headers["www-authenticate"]
            ?: throw Exception("Missing authenticate field when authentication failed.")
        if (!wwwAuthenticate.startsWith("Digest ")) {
            delegate.rtspClientErrorToast(localized("RTSP only supports Digest authentication"))
            throw Exception("Only Digest authentication is supported.")
        }
        val parameters = wwwAuthenticate.substring(7).split(Regex(",\\s*")).filter { it.isNotEmpty() }
        for (parameter in parameters) {
            val (name, value) = partition(parameter, "=")
            when (name) {
                "realm" -> realm = value.trim('"')
                "nonce" -> nonce = value.trim('"')
                "algorithm" -> {
                    if (value.trim('"') != "MD5") {
                        delegate.rtspClientErrorToast(
                            localized("RTSP only supports MD5 algorithm in authentication"),
                        )
                        throw Exception("Only authentication using MD5 algorithm is supported.")
                    }
                }
                else -> {}
            }
        }
    }

    private fun keepAlive() {
        if (!isAlive) {
            reconnectSoon()
            return
        }
        isAlive = false
        performGetParameter()
    }

    private fun performOptions() {
        perform(
            Request("OPTIONS", url) {
                performDescribe()
            },
        )
    }

    private fun performGetParameter() {
        perform(
            Request("GET_PARAMETER", url) {
                isAlive = true
            },
        )
    }

    private fun performDescribe() {
        val headers = mutableMapOf("Accept" to "application/sdp")
        perform(
            Request("DESCRIBE", url, headers) { response ->
                handleDescribeResponse(response)
            },
        )
    }

    private fun handleDescribeResponse(response: Response) {
        val content = response.content ?: throw Exception("Bad or missing DESCRIBE content.")
        val contentString = content.toString(Charsets.UTF_8)
        val baseUrl = response.headers["content-base"] ?: url.toString()
        val sdp = Sdp(contentString)
        val codec = sdp.video?.codec
        when (codec) {
            is SdpVideoCodec.H264 -> setupH264(codec.video)
            is SdpVideoCodec.H265 -> setupH265(codec.video)
            else -> throw Exception("No video media found.")
        }
        performSetup(makeSetupUrl(baseUrl, sdp.video?.control))
    }

    private fun makeSetupUrl(baseUrl: String, controlUrl: URI?): URI {
        if (controlUrl != null) {
            if (controlUrl.host != null) {
                return controlUrl
            }
            val combined = baseUrl + (controlUrl.path ?: "")
            return runCatching { URI(combined) }.getOrElse {
                throw Exception("Bad control URL: $controlUrl")
            }
        }
        return url
    }

    private fun setupH264(sdpVideo: SdpVideoH264) {
        val nalUnits = listOf(sdpVideo.sps, sdpVideo.pps)
        val formatDescription = nalUnits.makeFormatDescription()
            ?: throw Exception("Failed to create H.264 format description.")
        rtpVideo.processor = RtpProcessorVideoH264(formatDescription, this)
    }

    private fun setupH265(sdpVideo: SdpVideoH265) {
        val nalUnits = listOf(sdpVideo.vps, sdpVideo.sps, sdpVideo.pps)
        val formatDescription = nalUnits.makeFormatDescription()
            ?: throw Exception("Failed to create H.265 format description.")
        rtpVideo.processor = RtpProcessorVideoH265(formatDescription, this)
    }

    private fun performSetup(url: URI) {
        val transport = this.transport ?: return
        val headers = mutableMapOf("Transport" to transport.setupTransportHeader())
        perform(
            Request("SETUP", url, headers) { response ->
                handleSetupResponse(response)
            },
        )
    }

    private fun handleSetupResponse(response: Response) {
        val session = response.headers["session"] ?: throw Exception("Session header missing.")
        val transportHeader = response.headers["transport"] ?: throw Exception("Transport header missing.")
        videoSession = partitionOptional(session, ";").first
        transport?.handleSetupTransportResponse(transportHeader)
        performPlay()
    }

    private fun performPlay() {
        val session = videoSession ?: throw Exception("No video session.")
        val headers = mutableMapOf(
            "Session" to session,
            "Range" to "npt=now-",
        )
        perform(
            Request("PLAY", url, headers) { response ->
                handlePlayResponse(response)
            },
        )
    }

    private fun handlePlayResponse(response: Response) {
        setState(State.STREAMING)
        connectTimer.stop()
        keepAliveTimer.startPeriodic(5.0) {
            keepAlive()
        }
    }

    fun videoOutputSampleBuffer(sampleBuffer: MediaSample) {
        delegate.rtspClientOnVideoBuffer(cameraId, sampleBuffer)
    }

    private fun createTransport(): RtspTransport {
        return when (transportType) {
            SettingsRtspTransport.rtpRtspTcp -> RtspTransportRtpRtspTcp()
            SettingsRtspTransport.rtpUdp -> RtspTransportRtpUdp()
        }
    }

    override fun rtspTransportConnected() {
        setState(State.SETUP)
        performOptions()
    }

    override fun rtspTransportDisconnected() {}

    override fun rtspTransportReceivedRtspMessage(header: ByteArray, content: ByteArray?) {
        try {
            handleRtspMessage(header, content)
        } catch (e: Exception) {
            Log.d(TAG, "rtsp-client: Error handling RTSP message: $e")
        }
    }

    override fun rtspTransportReceivedRtpPacket(packet: ByteArray) {
        bitrateStats.add(bytesTransferred = packet.size)
        try {
            rtpVideo.handlePacket(packet)
        } catch (e: Exception) {
            Log.d(TAG, "rtsp-client: Error handling RTP packet: $e")
        }
    }

    override fun rtspTransportReceivedRtcpPacket(packet: ByteArray) {
        bitrateStats.add(bytesTransferred = packet.size)
        handleRtcpVideoPacket(packet)
    }
}

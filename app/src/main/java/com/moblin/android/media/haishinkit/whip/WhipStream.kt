package com.moblin.android.media.haishinkit.whip

import android.media.MediaFormat
import android.util.Log
import com.moblin.android.media.MediaSample
import com.moblin.android.media.haishinkit.codec.audio.AudioEncoderDelegate
import com.moblin.android.media.haishinkit.codec.video.VideoEncoder
import com.moblin.android.media.haishinkit.codec.video.VideoEncoderDelegate
import com.moblin.android.media.haishinkit.media.AudioVideoEncoderDelegate
import com.moblin.android.media.haishinkit.media.processorControlQueue
import com.moblin.android.media.haishinkit.mpeg.avc.MpegTsVideoConfigAvc
import com.moblin.android.media.haishinkit.mpeg.hevc.MpegTsVideoConfigHevc
import com.moblin.android.platform.datachannel.*
import com.moblin.android.various.SimpleTimer
import com.moblin.android.various.settings.SettingsHttpHeader
import com.moblin.android.various.settings.SettingsStreamAudioCodec
import com.moblin.android.various.settings.SettingsStreamCodec
import com.moblin.android.various.utils.TimeStampRebaser
import com.moblin.android.various.utils.stringFromCArray
import com.moblin.android.various.utils.withCPointers
import java.io.ByteArrayOutputStream
import java.net.URI
import java.util.UUID
import java.util.concurrent.Executors
import kotlin.random.Random
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

private const val TAG = "WhipStream"

private val whipQueue: CoroutineDispatcher =
    Executors.newSingleThreadExecutor { Thread(it, "com.eerimoq.Moblin.whip") }.asCoroutineDispatcher()
private val whipScope = CoroutineScope(whipQueue)
val h264PayloadType: UByte = 96u
private val h265PayloadType: UByte = 97u
val opusPayloadType: UByte = 111u
private val aacPayloadType: UByte = 112u
private val videoNackMaxStoredPacketCount: UInt = 8192u
private val audioNackMaxStoredPacketCount: UInt = 512u

fun makeSsrc(): UInt {
    var ssrc: UInt = 0u
    while (ssrc == 0u) {
        ssrc = Random.nextInt().toUInt()
    }
    return ssrc
}

fun checkOkReturnResult(result: Int): Int {
    if (result < 0) {
        throw DataChannelError("Error $result")
    }
    return result
}

fun checkOk(result: Int) {
    checkOkReturnResult(result)
}

private fun makeEndpointUrl(url: String): String? {
    val components = try {
        URI(url)
    } catch (error: Exception) {
        return null
    }
    val scheme = components.scheme ?: return url
    return scheme.replace("whip", "http") + url.substring(scheme.length)
}

private enum class TrackState {
    connecting,
    open,
    closed,
}

private class H264NalUnits {
    private var sps: ByteArray? = null
    private var pps: ByteArray? = null

    fun setParameterSets(sps: ByteArray?, pps: ByteArray?) {
        this.sps = sps
        this.pps = pps
    }

    fun process(sampleBuffer: MediaSample): ByteArray? {
        val sampleData = sampleBuffer.data
        if (sampleData.isEmpty()) {
            return null
        }
        if (sampleBuffer.isKeyFrame) {
            val data = ByteArrayOutputStream()
            val sps = sps
            if (sps != null) {
                appendNalUnit(data, sps)
            }
            val pps = pps
            if (pps != null) {
                appendNalUnit(data, pps)
            }
            data.write(sampleData)
            return data.toByteArray()
        }
        return sampleData
    }

    private fun appendNalUnit(data: ByteArrayOutputStream, nalUnit: ByteArray) {
        val length = nalUnit.size
        data.write((length ushr 24) and 0xFF)
        data.write((length ushr 16) and 0xFF)
        data.write((length ushr 8) and 0xFF)
        data.write(length and 0xFF)
        data.write(nalUnit)
    }
}

private class H265NalUnits {
    private var vps: ByteArray? = null
    private var sps: ByteArray? = null
    private var pps: ByteArray? = null

    fun setParameterSets(vps: ByteArray?, sps: ByteArray?, pps: ByteArray?) {
        this.vps = vps
        this.sps = sps
        this.pps = pps
    }

    fun process(sampleBuffer: MediaSample): ByteArray? {
        val sampleData = sampleBuffer.data
        if (sampleData.isEmpty()) {
            return null
        }
        if (sampleBuffer.isKeyFrame) {
            val data = ByteArrayOutputStream()
            val vps = vps
            if (vps != null) {
                appendNalUnit(data, vps)
            }
            val sps = sps
            if (sps != null) {
                appendNalUnit(data, sps)
            }
            val pps = pps
            if (pps != null) {
                appendNalUnit(data, pps)
            }
            data.write(sampleData)
            return data.toByteArray()
        }
        return sampleData
    }

    private fun appendNalUnit(data: ByteArrayOutputStream, nalUnit: ByteArray) {
        val length = nalUnit.size
        data.write((length ushr 24) and 0xFF)
        data.write((length ushr 16) and 0xFF)
        data.write((length ushr 8) and 0xFF)
        data.write(length and 0xFF)
        data.write(nalUnit)
    }
}

private fun toRtcTrack(pointer: Any?): RtcTrack? {
    return pointer as? RtcTrack
}

private class RtcTrack(private val trackId: Int) {
    @Volatile
    private var state: TrackState = TrackState.connecting

    init {
        try {
            rtcSetUserPointer(trackId, this)
            checkOk(
                rtcSetOpenCallback(trackId) { _, pointer ->
                    toRtcTrack(pointer)?.setState(TrackState.open)
                },
            )
            checkOk(
                rtcSetClosedCallback(trackId) { _, pointer ->
                    toRtcTrack(pointer)?.setState(TrackState.closed)
                },
            )
            checkOk(
                rtcSetErrorCallback(trackId) { _, _, pointer ->
                    toRtcTrack(pointer)?.setState(TrackState.closed)
                },
            )
            if (false) {
                checkOk(
                    rtcChainRembHandler(trackId) { _, bitrate, pointer ->
                        toRtcTrack(pointer)?.handleRemb(bitrate)
                    },
                )
            }
        } catch (error: Exception) {
            rtcDeleteTrack(trackId)
            throw error
        }
    }

    fun setTimestamp(presentationTimeStamp: Double) {
        val timestamp = IntArray(1)
        checkOk(rtcTransformSecondsToTimestamp(trackId, presentationTimeStamp, timestamp))
        checkOk(rtcSetTrackRtpTimestamp(trackId, timestamp[0].toUInt()))
    }

    fun send(message: ByteArray): Boolean {
        if (state != TrackState.open) {
            return false
        }
        val result = rtcSendMessage(trackId, message, message.size)
        return result >= 0
    }

    fun handleRemb(bitrate: UInt) {
        Log.i(TAG, "whip: $trackId: Got estimated maximum bitrate: $bitrate")
    }

    private fun setState(state: TrackState) {
        this.state = state
    }
}

private data class RtcTrackConfig(
    val name: String,
    val codec: rtcCodec,
    val payloadType: Int,
    val ssrc: UInt,
    val mid: String,
    val profile: String,
    val bitrate: Double,
) {
    companion object {
        fun makeAudio(ssrc: UInt, codec: SettingsStreamAudioCodec): RtcTrackConfig {
            return when (codec) {
                SettingsStreamAudioCodec.opus -> RtcTrackConfig(
                    name = "audio",
                    codec = RTC_CODEC_OPUS,
                    payloadType = opusPayloadType.toInt(),
                    ssrc = ssrc,
                    mid = "0",
                    profile = "",
                    bitrate = 0.0,
                )
                SettingsStreamAudioCodec.aac -> RtcTrackConfig(
                    name = "audio",
                    codec = RTC_CODEC_AAC,
                    payloadType = aacPayloadType.toInt(),
                    ssrc = ssrc,
                    mid = "0",
                    profile = "",
                    bitrate = 0.0,
                )
            }
        }

        fun makeVideo(ssrc: UInt, codec: SettingsStreamCodec, bitrate: Double): RtcTrackConfig {
            return when (codec) {
                SettingsStreamCodec.h264avc -> RtcTrackConfig(
                    name = "video",
                    codec = RTC_CODEC_H264,
                    payloadType = h264PayloadType.toInt(),
                    ssrc = ssrc,
                    mid = "1",
                    profile = "level-asymmetry-allowed=1;packetization-mode=1;profile-level-id=42e01f",
                    bitrate = bitrate,
                )
                SettingsStreamCodec.h265hevc -> RtcTrackConfig(
                    name = "video",
                    codec = RTC_CODEC_H265,
                    payloadType = h265PayloadType.toInt(),
                    ssrc = ssrc,
                    mid = "1",
                    profile = "",
                    bitrate = bitrate,
                )
            }
        }
    }

    fun isVideo(): Boolean {
        return when (codec) {
            RTC_CODEC_H264 -> true
            RTC_CODEC_H265 -> true
            else -> false
        }
    }
}

private interface PeerConnectionDelegate {
    fun peerConnectionOnConnectionStateChanged(state: DataChannelConnectionState)

    fun peerConnectionOnGatheringStateChanged(state: DataChannelGatheringState)
}

private fun toPeerConnection(pointer: Any?): PeerConnection? {
    return pointer as? PeerConnection
}

private class PeerConnection(delegate: PeerConnectionDelegate, iceServers: List<String>) {
    private val peerConnectionId: Int
    var delegate: PeerConnectionDelegate? = delegate

    init {
        val config = rtcConfiguration()
        peerConnectionId = iceServers.withCPointers {
            config.iceServers = it
            config.iceServersCount = iceServers.size
            rtcCreatePeerConnection(config)
        }
        checkOk(peerConnectionId)
        try {
            rtcSetUserPointer(peerConnectionId, this)
            checkOk(
                rtcSetStateChangeCallback(peerConnectionId) { _, state, pointer ->
                    toPeerConnection(pointer)?.handleStateChange(state)
                },
            )
            checkOk(
                rtcSetGatheringStateChangeCallback(peerConnectionId) { _, state, pointer ->
                    toPeerConnection(pointer)?.handleGatheringStateChange(state)
                },
            )
        } catch (error: Exception) {
            rtcDeletePeerConnection(peerConnectionId)
            throw error
        }
    }

    fun close() {
        rtcDeletePeerConnection(peerConnectionId)
    }

    fun addTrack(config: RtcTrackConfig, streamId: String): RtcTrack {
        val trackInit = rtcTrackInit(
            direction = RTC_DIRECTION_SENDONLY,
            codec = config.codec,
            payloadType = config.payloadType,
            ssrc = config.ssrc,
            mid = config.mid,
            name = config.name,
            msid = streamId,
            trackId = UUID.randomUUID().toString().uppercase(),
            profile = config.profile,
        )
        val trackId = checkOkReturnResult(rtcAddTrackEx(peerConnectionId, trackInit))
        val packetizerInit = rtcPacketizerInit()
        packetizerInit.ssrc = config.ssrc
        packetizerInit.cname = config.name
        packetizerInit.payloadType = config.payloadType.toUByte()
        val nackMaxStoredPacketCount: UInt
        if (config.isVideo()) {
            packetizerInit.clockRate = 90000u
            when (config.codec) {
                RTC_CODEC_H264 -> checkOk(rtcSetH264Packetizer(trackId, packetizerInit))
                RTC_CODEC_H265 -> checkOk(rtcSetH265Packetizer(trackId, packetizerInit))
                else -> throw DataChannelError("Unsupported video codec ${config.codec}")
            }
            if (false) {
                checkOk(rtcChainPacingHandler(trackId, 1.2 * config.bitrate, 5))
            }
            nackMaxStoredPacketCount = videoNackMaxStoredPacketCount
        } else {
            packetizerInit.clockRate = 48000u
            when (config.codec) {
                RTC_CODEC_AAC -> checkOk(rtcSetAACPacketizer(trackId, packetizerInit))
                RTC_CODEC_OPUS -> checkOk(rtcSetOpusPacketizer(trackId, packetizerInit))
                else -> throw DataChannelError("Unsupported audio codec ${config.codec}")
            }
            nackMaxStoredPacketCount = audioNackMaxStoredPacketCount
        }
        checkOk(rtcChainRtcpSrReporter(trackId))
        checkOk(rtcChainRtcpNackResponder(trackId, nackMaxStoredPacketCount))
        return RtcTrack(trackId)
    }

    fun setLocalDescriptionOffer() {
        checkOk(rtcSetLocalDescription(peerConnectionId, "offer"))
    }

    fun getLocalDescription(): String {
        val size = checkOkReturnResult(rtcGetLocalDescription(peerConnectionId, null, 0))
        val buffer = ByteArray(size)
        checkOk(rtcGetLocalDescription(peerConnectionId, buffer, size))
        return stringFromCArray(buffer)
    }

    fun setRemoteAnswer(sdp: String) {
        checkOk(rtcSetRemoteDescription(peerConnectionId, sdp, "answer"))
    }

    fun getSelectedCandidatePair(): Pair<String, String>? {
        try {
            val local = ByteArray(1024)
            val remote = ByteArray(1024)
            checkOk(rtcGetSelectedCandidatePair(peerConnectionId, local, local.size, remote, remote.size))
            return Pair(stringFromCArray(local), stringFromCArray(remote))
        } catch (error: Exception) {
            Log.i(TAG, "whip: Failed to get selected candidate pair")
            return null
        }
    }

    private fun handleStateChange(state: rtcState) {
        val connectionState = DataChannelConnectionState(value = state) ?: return
        delegate?.peerConnectionOnConnectionStateChanged(connectionState)
    }

    private fun handleGatheringStateChange(state: rtcGatheringState) {
        val gatheringState = DataChannelGatheringState(value = state) ?: return
        delegate?.peerConnectionOnGatheringStateChanged(gatheringState)
    }
}

interface WhipStreamDelegate {
    fun whipStreamOnConnected()

    fun whipStreamOnDisconnected(reason: String)

    fun whipStreamPerform(
        request: Request,
        queue: CoroutineDispatcher,
        completion: ((ByteArray?, Response?, Throwable?) -> Unit)?,
    )

    fun whipStreamStartEncoding(audioDelegate: AudioEncoderDelegate, videoDelegate: VideoEncoderDelegate)

    fun whipStreamStopEncoding(audioDelegate: AudioEncoderDelegate, videoDelegate: VideoEncoderDelegate)
}

class WhipStream(delegate: WhipStreamDelegate) : AudioVideoEncoderDelegate, PeerConnectionDelegate {
    private var delegate: WhipStreamDelegate? = delegate
    private var peerConnection: PeerConnection? = null
    private var videoTrack: RtcTrack? = null
    private var audioTrack: RtcTrack? = null
    private var h264NalUnits = H264NalUnits()
    private var h265NalUnits = H265NalUnits()
    private var videoCodec: SettingsStreamCodec = SettingsStreamCodec.h264avc
    private var totalByteCount: Long = 0
    private var sessionUrl: String? = null
    private var endpointUrl: String? = null
    private var headers: List<SettingsHttpHeader> = emptyList()
    private var connected = false
    private var offerSent = false
    private var timeStampRebaser = TimeStampRebaser()
    private val connectTimer = SimpleTimer(whipQueue)

    fun start(
        url: String,
        headers: List<SettingsHttpHeader>,
        iceServers: List<String>,
        videoCodec: SettingsStreamCodec,
        audioCodec: SettingsStreamAudioCodec,
        videoBitrate: Double,
    ) {
        whipScope.launch {
            startInternal(
                url = url,
                headers = headers,
                iceServers = iceServers,
                videoCodec = videoCodec,
                audioCodec = audioCodec,
                videoBitrate = videoBitrate,
            )
        }
    }

    fun stop() {
        whipScope.launch {
            stopInternal()
        }
    }

    fun getTotalByteCount(): Long {
        return runBlocking(whipQueue) {
            totalByteCount
        }
    }

    private fun startInternal(
        url: String,
        headers: List<SettingsHttpHeader>,
        iceServers: List<String>,
        videoCodec: SettingsStreamCodec,
        audioCodec: SettingsStreamAudioCodec,
        videoBitrate: Double,
    ) {
        stopInternal()
        val endpointUrl = makeEndpointUrl(url) ?: return
        this.endpointUrl = endpointUrl
        this.headers = headers
        this.videoCodec = videoCodec
        totalByteCount = 0
        connected = false
        offerSent = false
        Log.i(TAG, "whip: Start URL: $endpointUrl")
        h264NalUnits = H264NalUnits()
        h265NalUnits = H265NalUnits()
        try {
            val peerConnection = PeerConnection(delegate = this, iceServers = iceServers)
            val streamId = UUID.randomUUID().toString().uppercase()
            audioTrack = peerConnection.addTrack(
                config = RtcTrackConfig.makeAudio(ssrc = makeSsrc(), codec = audioCodec),
                streamId = streamId,
            )
            videoTrack = peerConnection.addTrack(
                config = RtcTrackConfig.makeVideo(ssrc = makeSsrc(), codec = videoCodec, bitrate = videoBitrate),
                streamId = streamId,
            )
            this.peerConnection = peerConnection
            peerConnection.setLocalDescriptionOffer()
            connectTimer.startSingleShot(timeout = 10.0) {
                handleConnectTimeout()
            }
        } catch (error: Exception) {
            stopInternal(reason = "Start failed: $error")
        }
    }

    private fun stopInternal(reason: String? = null) {
        stopEncoding()
        val sessionUrl = sessionUrl
        if (sessionUrl != null) {
            sendDeleteRequest(url = sessionUrl)
        }
        this.sessionUrl = null
        endpointUrl = null
        peerConnection?.close()
        peerConnection = null
        videoTrack = null
        audioTrack = null
        connected = false
        offerSent = false
        connectTimer.stop()
        if (reason != null) {
            notifyDisconnected(reason = reason)
        }
    }

    private fun handleConnectTimeout() {
        stopInternal(reason = "Connect timeout")
    }

    private fun handleConnectionStateChanged(state: DataChannelConnectionState) {
        Log.i(TAG, "whip: Connection state: $state")
        when (state) {
            DataChannelConnectionState.connected -> {
                if (connected) {
                    return
                }
                connectTimer.stop()
                connected = true
                val candidatePair = peerConnection?.getSelectedCandidatePair()
                if (candidatePair != null) {
                    val (local, remote) = candidatePair
                    Log.i(TAG, "whip: Local candidate: $local")
                    Log.i(TAG, "whip: Remote candidate: $remote")
                }
                startEncoding()
                notifyConnected()
            }
            DataChannelConnectionState.disconnected,
            DataChannelConnectionState.failed,
            DataChannelConnectionState.closed,
            -> stopInternal(reason = "Connection $state")
            DataChannelConnectionState.new,
            DataChannelConnectionState.connecting,
            -> Unit
        }
    }

    private fun handleGatheringStateChanged(state: DataChannelGatheringState) {
        Log.i(TAG, "whip: ICE gathering state: $state")
        when (state) {
            DataChannelGatheringState.complete -> {
                val peerConnection = peerConnection ?: return
                try {
                    sendOffer(offer = peerConnection.getLocalDescription())
                } catch (error: Exception) {
                    stopInternal(reason = "Failed to create offer")
                }
            }
            DataChannelGatheringState.new,
            DataChannelGatheringState.inProgress,
            -> Unit
        }
    }

    private fun sendOffer(offer: String) {
        if (offerSent) {
            return
        }
        val endpointUrl = endpointUrl ?: return
        Log.d(TAG, "whip: Sending offer: ${offer.replace("\r", "")}")
        val request = try {
            val builder = Request.Builder()
                .url(endpointUrl)
                .header("Content-Type", "application/sdp")
            for (header in headers) {
                builder.header(header.name, header.value)
            }
            builder
                .post(offer.toByteArray(Charsets.UTF_8).toRequestBody("application/sdp".toMediaType()))
                .build()
        } catch (error: IllegalArgumentException) {
            whipScope.launch {
                handleOfferResponse(data = null, response = null, error = error)
            }
            offerSent = true
            return
        }
        delegate?.whipStreamPerform(request = request, queue = whipQueue) { data, response, error ->
            handleOfferResponse(data = data, response = response, error = error)
        }
        offerSent = true
    }

    private fun handleOfferResponse(data: ByteArray?, response: Response?, error: Throwable?) {
        if (error != null) {
            stopInternal(reason = "Sending WHIP offer failed: ${error.localizedMessage}")
            return
        }
        if (response == null) {
            stopInternal(reason = "Bad WHIP server response")
            return
        }
        if (!response.isSuccessful) {
            stopInternal(reason = "WHIP server returned HTTP status ${response.code}")
            return
        }
        val locationHeader = response.header("Location")
        if (locationHeader != null) {
            sessionUrl = try {
                URI(endpointUrl).resolve(locationHeader).toString()
            } catch (error: Exception) {
                null
            }
        }
        if (data == null) {
            stopInternal(reason = "WHIP answer missing")
            return
        }
        val answer = data.toString(Charsets.UTF_8)
        Log.d(TAG, "whip: Got answer: ${answer.replace("\r", "")}")
        try {
            peerConnection?.setRemoteAnswer(answer)
        } catch (error: Exception) {
            stopInternal(reason = "Failed to set remote answer")
        }
    }

    private fun sendDeleteRequest(url: String) {
        val request = try {
            Request.Builder().url(url).delete().build()
        } catch (error: IllegalArgumentException) {
            return
        }
        delegate?.whipStreamPerform(request = request, queue = whipQueue, completion = null)
    }

    private fun startEncoding() {
        processorControlQueue.launch {
            delegate?.whipStreamStartEncoding(this@WhipStream, this@WhipStream)
        }
    }

    private fun stopEncoding() {
        processorControlQueue.launch {
            delegate?.whipStreamStopEncoding(this@WhipStream, this@WhipStream)
        }
    }

    private fun notifyConnected() {
        delegate?.whipStreamOnConnected()
    }

    private fun notifyDisconnected(reason: String) {
        delegate?.whipStreamOnDisconnected(reason = reason)
    }

    private fun rebaseTimestamp(presentationTimeStamp: Long): Double? {
        return timeStampRebaser.rebase(presentationTimeStamp / 1_000_000.0)
    }

    private fun handleAudioEncoderOutputBuffer(buffer: MediaSample, presentationTimeStamp: Long) {
        if (!connected) {
            return
        }
        val audioTrack = audioTrack ?: return
        val rebasedPresentationTimeStamp = rebaseTimestamp(presentationTimeStamp) ?: return
        if (buffer.data.isEmpty()) {
            return
        }
        try {
            audioTrack.setTimestamp(presentationTimeStamp = rebasedPresentationTimeStamp)
        } catch (error: Exception) {
            Log.i(TAG, "whip: Failed to set audio timestamp")
            return
        }
        val allData = buffer.data
        if (audioTrack.send(message = allData)) {
            totalByteCount += allData.size.toLong()
        }
    }

    private fun handleVideoEncoderOutputFormat(formatDescription: MediaFormat) {
        when (videoCodec) {
            SettingsStreamCodec.h264avc -> {
                val config = MpegTsVideoConfigAvc.fromFormatDescription(formatDescription) ?: return
                h264NalUnits.setParameterSets(sps = config.sequenceParameterSet, pps = config.pictureParameterSet)
            }
            SettingsStreamCodec.h265hevc -> {
                val config = MpegTsVideoConfigHevc.create(formatDescription) ?: return
                h265NalUnits.setParameterSets(
                    vps = config.videoParameterSet,
                    sps = config.sequenceParameterSet,
                    pps = config.pictureParameterSet,
                )
            }
        }
    }

    private fun handleVideoEncoderOutputSampleBuffer(sampleBuffer: MediaSample) {
        if (!connected) {
            return
        }
        val videoTrack = videoTrack ?: return
        val presentationTimeStamp = rebaseTimestamp(sampleBuffer.presentationTimeUs) ?: return
        try {
            videoTrack.setTimestamp(presentationTimeStamp = presentationTimeStamp)
        } catch (error: Exception) {
            Log.i(TAG, "whip: Failed to set timestamp")
            return
        }
        val data: ByteArray? = when (videoCodec) {
            SettingsStreamCodec.h264avc -> h264NalUnits.process(sampleBuffer)
            SettingsStreamCodec.h265hevc -> h265NalUnits.process(sampleBuffer)
        }
        if (data == null) {
            return
        }
        if (videoTrack.send(message = data)) {
            totalByteCount += data.size.toLong()
        }
    }

    override fun peerConnectionOnConnectionStateChanged(state: DataChannelConnectionState) {
        whipScope.launch {
            handleConnectionStateChanged(state = state)
        }
    }

    override fun peerConnectionOnGatheringStateChanged(state: DataChannelGatheringState) {
        whipScope.launch {
            handleGatheringStateChanged(state = state)
        }
    }

    override fun audioEncoderOutputFormat(format: MediaFormat) {}

    override fun audioEncoderOutputBuffer(buffer: MediaSample, presentationTimeStamp: Long) {
        whipScope.launch {
            handleAudioEncoderOutputBuffer(buffer, presentationTimeStamp)
        }
    }

    override fun videoEncoderOutputFormat(encoder: VideoEncoder, format: MediaFormat) {
        whipScope.launch {
            handleVideoEncoderOutputFormat(format)
        }
    }

    override fun videoEncoderOutputSampleBuffer(
        encoder: VideoEncoder,
        sampleBuffer: MediaSample,
        presentationTimeStamp: Long,
    ) {
        whipScope.launch {
            handleVideoEncoderOutputSampleBuffer(sampleBuffer)
        }
    }
}

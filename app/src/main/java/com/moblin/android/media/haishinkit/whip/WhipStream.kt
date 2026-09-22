package com.moblin.android.media.haishinkit.whip

import android.media.MediaFormat
import android.util.Log
import com.moblin.android.media.MediaSample
import com.moblin.android.media.haishinkit.codec.audio.AudioEncoderDelegate
import com.moblin.android.media.haishinkit.codec.video.VideoEncoder
import com.moblin.android.media.haishinkit.codec.video.VideoEncoderDelegate
import com.moblin.android.media.haishinkit.mpeg.avc.MpegTsVideoConfigAvc
import com.moblin.android.media.haishinkit.mpeg.hevc.MpegTsVideoConfigHevc
import com.moblin.android.media.processorControlQueue
import com.moblin.android.various.SimpleTimer
import com.moblin.android.various.settings.SettingsHttpHeader
import com.moblin.android.various.settings.SettingsStreamAudioCodec
import com.moblin.android.various.settings.SettingsStreamCodec
import com.moblin.android.various.utils.TimeStampRebaser
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

private val whipQueue: CoroutineDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
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
        throw IllegalStateException("Error $result")
    }
    return result
}

fun checkOk(result: Int) {
    checkOkReturnResult(result)
}

private fun makeEndpointUrl(url: String): String? {
    val components = try {
        URI(url)
    } catch (e: Exception) {
        return null
    }
    val scheme = components.scheme?.replace("whip", "http") ?: return null
    return try {
        URI(
            scheme,
            components.userInfo,
            components.host,
            components.port,
            components.path,
            components.query,
            components.fragment,
        ).toString()
    } catch (e: Exception) {
        null
    }
}

private enum class TrackState {
    CONNECTING,
    OPEN,
    CLOSED,
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
            sps?.let { appendNalUnit(data, it) }
            pps?.let { appendNalUnit(data, it) }
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
            vps?.let { appendNalUnit(data, it) }
            sps?.let { appendNalUnit(data, it) }
            pps?.let { appendNalUnit(data, it) }
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

private fun toRtcTrack(pointer: Long?): RtcTrack? {
    TODO("libdatachannel JNI bridge is unavailable")
}

private class RtcTrack(private val trackId: Int) {
    private var state: TrackState = TrackState.CONNECTING

    init {
        TODO("libdatachannel JNI bridge is unavailable: rtcSetUserPointer/rtcSetOpenCallback/rtcSetClosedCallback/rtcSetErrorCallback")
    }

    fun setTimestamp(presentationTimeStamp: Double) {
        TODO("libdatachannel JNI bridge is unavailable: rtcTransformSecondsToTimestamp/rtcSetTrackRtpTimestamp")
    }

    fun send(message: ByteArray): Boolean {
        if (state != TrackState.OPEN) {
            return false
        }
        TODO("libdatachannel JNI bridge is unavailable: rtcSendMessage")
    }

    fun handleRemb(bitrate: UInt) {
        Log.i(TAG, "whip: $trackId: Got estimated maximum bitrate: $bitrate")
    }

    private fun setState(state: TrackState) {
        this.state = state
    }
}

private enum class rtcCodec {
    H264,
    H265,
    OPUS,
    AAC,
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
        fun makeAudio(ssrc: UInt, codec: SettingsStreamAudioCodec): RtcTrackConfig = when (codec) {
            SettingsStreamAudioCodec.OPUS -> RtcTrackConfig(
                name = "audio",
                codec = rtcCodec.OPUS,
                payloadType = opusPayloadType.toInt(),
                ssrc = ssrc,
                mid = "0",
                profile = "",
                bitrate = 0.0,
            )
            SettingsStreamAudioCodec.AAC -> RtcTrackConfig(
                name = "audio",
                codec = rtcCodec.AAC,
                payloadType = aacPayloadType.toInt(),
                ssrc = ssrc,
                mid = "0",
                profile = "",
                bitrate = 0.0,
            )
        }

        fun makeVideo(ssrc: UInt, codec: SettingsStreamCodec, bitrate: Double): RtcTrackConfig = when (codec) {
            SettingsStreamCodec.H264AVC -> RtcTrackConfig(
                name = "video",
                codec = rtcCodec.H264,
                payloadType = h264PayloadType.toInt(),
                ssrc = ssrc,
                mid = "1",
                profile = "level-asymmetry-allowed=1;packetization-mode=1;profile-level-id=42e01f",
                bitrate = bitrate,
            )
            SettingsStreamCodec.H265HEVC -> RtcTrackConfig(
                name = "video",
                codec = rtcCodec.H265,
                payloadType = h265PayloadType.toInt(),
                ssrc = ssrc,
                mid = "1",
                profile = "",
                bitrate = bitrate,
            )
        }
    }

    fun isVideo(): Boolean = when (codec) {
        rtcCodec.H264 -> true
        rtcCodec.H265 -> true
        else -> false
    }
}

private interface PeerConnectionDelegate {
    fun peerConnectionOnConnectionStateChanged(state: DataChannelConnectionState)

    fun peerConnectionOnGatheringStateChanged(state: DataChannelGatheringState)
}

private fun toPeerConnection(pointer: Long?): PeerConnection? {
    TODO("libdatachannel JNI bridge is unavailable")
}

private class PeerConnection(
    private var delegate: PeerConnectionDelegate?,
    iceServers: List<String>,
) {
    private val peerConnectionId: Int

    init {
        peerConnectionId = TODO("libdatachannel JNI bridge is unavailable: rtcCreatePeerConnection")
    }

    fun close() {
        TODO("libdatachannel JNI bridge is unavailable: rtcDeletePeerConnection")
    }

    fun addTrack(config: RtcTrackConfig, streamId: String): RtcTrack {
        TODO("libdatachannel JNI bridge is unavailable: rtcAddTrackEx/rtcSetH264Packetizer/rtcSetH265Packetizer/rtcSetAACPacketizer/rtcSetOpusPacketizer/rtcChainRtcpSrReporter/rtcChainRtcpNackResponder")
    }

    fun setLocalDescriptionOffer() {
        TODO("libdatachannel JNI bridge is unavailable: rtcSetLocalDescription")
    }

    fun getLocalDescription(): String {
        TODO("libdatachannel JNI bridge is unavailable: rtcGetLocalDescription")
    }

    fun setRemoteAnswer(sdp: String) {
        TODO("libdatachannel JNI bridge is unavailable: rtcSetRemoteDescription")
    }

    fun getSelectedCandidatePair(): Pair<String, String>? {
        TODO("libdatachannel JNI bridge is unavailable: rtcGetSelectedCandidatePair")
    }

    private fun handleStateChange(state: Int) {
        val mapped = DataChannelConnectionState.fromValue(state) ?: return
        delegate?.peerConnectionOnConnectionStateChanged(mapped)
    }

    private fun handleGatheringStateChange(state: Int) {
        val mapped = DataChannelGatheringState.fromValue(state) ?: return
        delegate?.peerConnectionOnGatheringStateChanged(mapped)
    }
}

enum class DataChannelConnectionState {
    NEW,
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    FAILED,
    CLOSED;

    companion object {
        fun fromValue(value: Int): DataChannelConnectionState? = when (value) {
            0 -> NEW
            1 -> CONNECTING
            2 -> CONNECTED
            3 -> DISCONNECTED
            4 -> FAILED
            5 -> CLOSED
            else -> null
        }
    }
}

enum class DataChannelGatheringState {
    NEW,
    IN_PROGRESS,
    COMPLETE;

    companion object {
        fun fromValue(value: Int): DataChannelGatheringState? = when (value) {
            0 -> NEW
            1 -> IN_PROGRESS
            2 -> COMPLETE
            else -> null
        }
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

class WhipStream(delegate: WhipStreamDelegate) : AudioEncoderDelegate, VideoEncoderDelegate, PeerConnectionDelegate {
    private var delegate: WhipStreamDelegate? = delegate
    private var peerConnection: PeerConnection? = null
    private var videoTrack: RtcTrack? = null
    private var audioTrack: RtcTrack? = null
    private var h264NalUnits = H264NalUnits()
    private var h265NalUnits = H265NalUnits()
    private var videoCodec: SettingsStreamCodec = SettingsStreamCodec.H264AVC
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
            startInternal(url, headers, iceServers, videoCodec, audioCodec, videoBitrate)
        }
    }

    fun stop() {
        whipScope.launch {
            stopInternal()
        }
    }

    fun getTotalByteCount(): Long = runBlocking(whipQueue) {
        totalByteCount
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
            val peerConnection = PeerConnection(this, iceServers)
            val streamId = UUID.randomUUID().toString()
            audioTrack = peerConnection.addTrack(
                RtcTrackConfig.makeAudio(makeSsrc(), audioCodec),
                streamId,
            )
            videoTrack = peerConnection.addTrack(
                RtcTrackConfig.makeVideo(makeSsrc(), videoCodec, videoBitrate),
                streamId,
            )
            this.peerConnection = peerConnection
            peerConnection.setLocalDescriptionOffer()
            connectTimer.startSingleShot(10.0) { handleConnectTimeout() }
        } catch (e: Exception) {
            stopInternal("Start failed: $e")
        }
    }

    private fun stopInternal(reason: String? = null) {
        stopEncoding()
        val currentSessionUrl = sessionUrl
        if (currentSessionUrl != null) {
            sendDeleteRequest(currentSessionUrl)
        }
        sessionUrl = null
        endpointUrl = null
        peerConnection?.close()
        peerConnection = null
        videoTrack = null
        audioTrack = null
        connected = false
        offerSent = false
        connectTimer.stop()
        if (reason != null) {
            notifyDisconnected(reason)
        }
    }

    private fun handleConnectTimeout() {
        stopInternal("Connect timeout")
    }

    private fun handleConnectionStateChanged(state: DataChannelConnectionState) {
        Log.i(TAG, "whip: Connection state: $state")
        when (state) {
            DataChannelConnectionState.CONNECTED -> {
                if (connected) {
                    return
                }
                connectTimer.stop()
                connected = true
                peerConnection?.getSelectedCandidatePair()?.let { (local, remote) ->
                    Log.i(TAG, "whip: Local candidate: $local")
                    Log.i(TAG, "whip: Remote candidate: $remote")
                }
                startEncoding()
                notifyConnected()
            }
            DataChannelConnectionState.DISCONNECTED,
            DataChannelConnectionState.FAILED,
            DataChannelConnectionState.CLOSED -> {
                stopInternal("Connection $state")
            }
            DataChannelConnectionState.NEW,
            DataChannelConnectionState.CONNECTING -> {
            }
        }
    }

    private fun handleGatheringStateChanged(state: DataChannelGatheringState) {
        Log.i(TAG, "whip: ICE gathering state: $state")
        when (state) {
            DataChannelGatheringState.COMPLETE -> {
                val peerConnection = peerConnection ?: return
                try {
                    sendOffer(peerConnection.getLocalDescription())
                } catch (e: Exception) {
                    stopInternal("Failed to create offer")
                }
            }
            DataChannelGatheringState.NEW,
            DataChannelGatheringState.IN_PROGRESS -> {
            }
        }
    }

    private fun sendOffer(offer: String) {
        if (offerSent) {
            return
        }
        val endpointUrl = endpointUrl ?: return
        Log.d(TAG, "whip: Sending offer: ${offer.replace("\r", "")}")
        val builder = Request.Builder()
            .url(endpointUrl)
            .post(offer.toRequestBody("application/sdp".toMediaType()))
        for (header in headers) {
            builder.header(header.name, header.value)
        }
        val request = builder.build()
        delegate?.whipStreamPerform(request, whipQueue) { data, response, error ->
            handleOfferResponse(data, response, error)
        }
        offerSent = true
    }

    private fun handleOfferResponse(data: ByteArray?, response: Response?, error: Throwable?) {
        if (error != null) {
            stopInternal("Sending WHIP offer failed: ${error.message}")
            return
        }
        if (response == null) {
            stopInternal("Bad WHIP server response")
            return
        }
        if (!response.isSuccessful) {
            stopInternal("WHIP server returned HTTP status ${response.code}")
            return
        }
        response.header("Location")?.let { locationHeader ->
            sessionUrl = try {
                val base = endpointUrl
                if (base != null) {
                    URI(base).resolve(locationHeader).toString()
                } else {
                    URI(locationHeader).toString()
                }
            } catch (e: Exception) {
                null
            }
        }
        if (data == null) {
            stopInternal("WHIP answer missing")
            return
        }
        val answer = data.toString(Charsets.UTF_8)
        Log.d(TAG, "whip: Got answer: ${answer.replace("\r", "")}")
        try {
            peerConnection?.setRemoteAnswer(answer)
        } catch (e: Exception) {
            stopInternal("Failed to set remote answer")
        }
    }

    private fun sendDeleteRequest(url: String) {
        val request = Request.Builder().url(url).delete().build()
        delegate?.whipStreamPerform(request, whipQueue, null)
    }

    private fun startEncoding() {
        CoroutineScope(processorControlQueue).launch {
            delegate?.whipStreamStartEncoding(this@WhipStream, this@WhipStream)
        }
    }

    private fun stopEncoding() {
        CoroutineScope(processorControlQueue).launch {
            delegate?.whipStreamStopEncoding(this@WhipStream, this@WhipStream)
        }
    }

    private fun notifyConnected() {
        delegate?.whipStreamOnConnected()
    }

    private fun notifyDisconnected(reason: String) {
        delegate?.whipStreamOnDisconnected(reason)
    }

    private fun rebaseTimestamp(presentationTimeStampUs: Long): Double? =
        timeStampRebaser.rebase(presentationTimeStampUs / 1_000_000.0)

    private fun handleAudioEncoderOutputBuffer(buffer: ByteArray, presentationTimeStampUs: Long) {
        if (!connected) {
            return
        }
        val audioTrack = audioTrack ?: return
        val presentationTimeStamp = rebaseTimestamp(presentationTimeStampUs) ?: return
        if (buffer.isEmpty()) {
            return
        }
        try {
            audioTrack.setTimestamp(presentationTimeStamp)
        } catch (e: Exception) {
            Log.i(TAG, "whip: Failed to set audio timestamp")
            return
        }
        if (audioTrack.send(buffer)) {
            totalByteCount += buffer.size.toLong()
        }
    }

    private fun handleVideoEncoderOutputFormat(format: MediaFormat) {
        when (videoCodec) {
            SettingsStreamCodec.H264AVC -> {
                val config = MpegTsVideoConfigAvc.create(format) ?: return
                h264NalUnits.setParameterSets(config.sequenceParameterSet, config.pictureParameterSet)
            }
            SettingsStreamCodec.H265HEVC -> {
                val config = MpegTsVideoConfigHevc.create(format) ?: return
                h265NalUnits.setParameterSets(
                    config.videoParameterSet,
                    config.sequenceParameterSet,
                    config.pictureParameterSet,
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
            videoTrack.setTimestamp(presentationTimeStamp)
        } catch (e: Exception) {
            Log.i(TAG, "whip: Failed to set timestamp")
            return
        }
        val data: ByteArray? = when (videoCodec) {
            SettingsStreamCodec.H264AVC -> h264NalUnits.process(sampleBuffer)
            SettingsStreamCodec.H265HEVC -> h265NalUnits.process(sampleBuffer)
        }
        val payload = data ?: return
        if (videoTrack.send(payload)) {
            totalByteCount += payload.size.toLong()
        }
    }

    override fun peerConnectionOnConnectionStateChanged(state: DataChannelConnectionState) {
        whipScope.launch {
            handleConnectionStateChanged(state)
        }
    }

    override fun peerConnectionOnGatheringStateChanged(state: DataChannelGatheringState) {
        whipScope.launch {
            handleGatheringStateChanged(state)
        }
    }

    override fun audioEncoderOutputFormat(format: MediaFormat) {
    }

    override fun audioEncoderOutputBuffer(buffer: ByteArray, presentationTimeStamp: Long) {
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

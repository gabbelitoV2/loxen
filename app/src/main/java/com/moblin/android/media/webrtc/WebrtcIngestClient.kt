package com.moblin.android.media.webrtc

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import com.moblin.android.media.MediaSample
import com.moblin.android.media.WrappingTimestamp
import com.moblin.android.media.haishinkit.codec.video.VideoDecoder
import com.moblin.android.media.haishinkit.codec.video.VideoDecoderDelegate
import com.moblin.android.media.haishinkit.mpeg.NalUnitType
import com.moblin.android.media.haishinkit.mpeg.getNalUnits
import com.moblin.android.media.haishinkit.mpeg.readH264NalUnits
import com.moblin.android.media.haishinkit.mpeg.readH265NalUnits
import com.moblin.android.media.haishinkit.mpeg.removeNalUnitStartCodes
import com.moblin.android.media.haishinkit.whip.checkOk
import com.moblin.android.media.haishinkit.whip.checkOkReturnResult
import com.moblin.android.media.haishinkit.whip.makeSsrc
import com.moblin.android.various.utils.TimeStampRebaser
import com.moblin.android.various.utils.currentPresentationTimeStamp
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val TAG = "WebrtcIngestClient"

private const val MAXIMUM_TIMESTAMP_US = 4_294_967_296_000_000L

private const val OPUS_SAMPLE_RATE = 48000
private const val OPUS_CHANNELS = 2
private const val MAXIMUM_OPUS_PACKET_SIZE = 4096

interface WebrtcIngestClientDelegate {
    fun webrtcIngestClientOnConnected(streamId: UUID)

    fun webrtcIngestClientOnDisconnected(streamId: UUID, reason: String)

    fun webrtcIngestClientOnVideoBuffer(streamId: UUID, sampleBuffer: MediaSample)

    fun webrtcIngestClientOnAudioBuffer(streamId: UUID, sampleBuffer: MediaSample)

    fun webrtcIngestClientOnGatheringComplete(streamId: UUID, localDescription: String)

    fun webrtcIngestClientOnDataReceived(streamId: UUID, count: Int)
}

fun decodeNtpTimestamp(v: ULong): Double? {
    if (v shr 32 < 2_208_988_800uL) {
        return null
    }
    val secs = ((v shr 32) - 2_208_988_800uL).toLong()
    val nanos = (((v and 0xFFFF_FFFFuL) * 1_000_000_000uL) / (1uL shl 32)).toLong()
    return secs.toDouble() + nanos.toDouble() / 1_000_000_000.0
}

private fun toIngestClient(pointer: Long): WebrtcIngestClient? {
    return IngestClientHandles.get(pointer)
}

private object IngestClientHandles {
    private val lock = Any()
    private val byClient = java.util.IdentityHashMap<WebrtcIngestClient, Long>()
    private val byHandle = HashMap<Long, WebrtcIngestClient>()
    private var next = 1L

    fun register(client: WebrtcIngestClient): Long = synchronized(lock) {
        val existing = byClient[client]
        if (existing != null) {
            existing
        } else {
            val handle = next
            next += 1
            byClient[client] = handle
            byHandle[handle] = client
            handle
        }
    }

    fun get(handle: Long): WebrtcIngestClient? = synchronized(lock) {
        byHandle[handle]
    }

    fun unregister(client: WebrtcIngestClient) {
        synchronized(lock) {
            val handle = byClient.remove(client)
            if (handle != null) {
                byHandle.remove(handle)
            }
        }
    }
}

private class RtcTrackInit(
    val direction: Int,
    val codec: Int,
    val payloadType: Int,
    val ssrc: UInt,
    val mid: String,
    val name: String,
    val msid: String,
    val trackId: String,
    val profile: String,
)

internal fun interface RtcStateChangeCallback {
    fun onStateChange(peerConnectionId: Int, state: Int, userPointer: Long)
}

internal fun interface RtcGatheringStateChangeCallback {
    fun onGatheringStateChange(peerConnectionId: Int, state: Int, userPointer: Long)
}

internal fun interface RtcTrackCallback {
    fun onTrack(peerConnectionId: Int, trackId: Int, userPointer: Long)
}

internal fun interface RtcFrameCallback {
    fun onFrame(trackId: Int, data: ByteArray?, size: Int, timestamp: UInt, userPointer: Long)
}

internal object RtcNative {
    const val RTC_DIRECTION_RECVONLY = 2
    const val RTC_NAL_SEPARATOR_LONG_START_SEQUENCE = 1

    external fun rtcCreatePeerConnection(iceServers: Array<String>): Int

    external fun rtcSetUserPointer(peerConnectionId: Int, userPointer: Long)

    external fun rtcSetStateChangeCallback(
        peerConnectionId: Int,
        callback: RtcStateChangeCallback,
    ): Int

    external fun rtcSetGatheringStateChangeCallback(
        peerConnectionId: Int,
        callback: RtcGatheringStateChangeCallback,
    ): Int

    external fun rtcSetTrackCallback(peerConnectionId: Int, callback: RtcTrackCallback): Int

    external fun rtcSetRemoteDescription(peerConnectionId: Int, sdp: String, type: String): Int

    external fun rtcSetLocalDescription(peerConnectionId: Int, type: String): Int

    external fun rtcGetLocalDescription(peerConnectionId: Int, buffer: ByteArray?, size: Int): Int

    external fun rtcAddTrackEx(peerConnectionId: Int, trackInit: RtcTrackInit): Int

    external fun rtcDeletePeerConnection(peerConnectionId: Int)

    external fun rtcSetH264Depacketizer(trackId: Int, separator: Int)

    external fun rtcSetH265Depacketizer(trackId: Int, separator: Int)

    external fun rtcSetOpusDepacketizer(trackId: Int)

    external fun rtcChainRtcpReceivingSession(trackId: Int)

    external fun rtcSetFrameCallback(trackId: Int, callback: RtcFrameCallback)

    external fun rtcGetTrackDescription(trackId: Int, buffer: ByteArray, size: Int): Int

    external fun rtcGetTrackRtcpSyncTimestamps(
        trackId: Int,
        rtpTimestamp: LongArray,
        ntpTimestamp: LongArray,
    ): Int
}

private enum class VideoCodec {
    H264,
    H265,
    ;

    companion object {
        fun fromTrackDescription(trackDescription: String): VideoCodec? {
            return when {
                trackDescription.contains("h264") -> H264
                trackDescription.contains("h265") -> H265
                else -> null
            }
        }
    }
}

private class TrackTimestamper(
    private val clockRate: Double,
    private val syncTimestamps: Boolean,
    name: String,
) {
    private val wrappingTimestamp = WrappingTimestamp(
        name = name,
        maximumTimestamp = MAXIMUM_TIMESTAMP_US,
    )

    private var offset: Double? = null

    fun timestampSeconds(trackId: Int, timestamp: UInt): Double? {
        val timestampSeconds = unwrap(timestamp)
        if (!syncTimestamps) {
            return timestampSeconds
        }
        val currentOffset = offset ?: run {
            val rtpTimestamp = LongArray(1)
            val ntpTimestamp = LongArray(1)
            RtcNative.rtcGetTrackRtcpSyncTimestamps(trackId, rtpTimestamp, ntpTimestamp)
            val ntpTimestampSeconds = decodeNtpTimestamp(ntpTimestamp[0].toULong()) ?: return null
            val newOffset = ntpTimestampSeconds - unwrap(rtpTimestamp[0].toUInt())
            offset = newOffset
            newOffset
        }
        return timestampSeconds + currentOffset
    }

    private fun unwrap(timestamp: UInt): Double {
        val timestampUs = wrappingTimestamp.update(timestamp.toLong())
        return timestampUs.toDouble() / clockRate
    }
}

private enum class DataChannelConnectionState(val rawValue: Int) {
    NEW(0),
    CONNECTING(1),
    CONNECTED(2),
    DISCONNECTED(3),
    FAILED(4),
    CLOSED(5),
    ;

    companion object {
        fun fromValue(value: Int): DataChannelConnectionState? {
            return entries.firstOrNull { it.rawValue == value }
        }
    }
}

private enum class DataChannelGatheringState(val rawValue: Int) {
    NEW(0),
    IN_PROGRESS(1),
    COMPLETE(2),
    ;

    companion object {
        fun fromValue(value: Int): DataChannelGatheringState? {
            return entries.firstOrNull { it.rawValue == value }
        }
    }
}

class WebrtcIngestClient(
    private val name: String,
    val streamId: UUID,
    private val latency: Double,
    private val syncTimestamps: Boolean,
    private val softwareDecoding: Boolean,
    private val iceServers: List<String>,
    private val dispatchQueue: CoroutineDispatcher,
    private val delegate: WebrtcIngestClientDelegate,
) : VideoDecoderDelegate {
    var peerConnectionId: Int = -1
        private set
    private var connected = false
    private var videoDecoder: VideoDecoder? = null
    private var videoFormatDescription: MediaFormat? = null
    private var basePresentationTimeStamp: Double = -1.0
    private var timeStampRebaser = TimeStampRebaser()
    private var opusDecoder: MediaCodec? = null
    private var pcmAudioFormat: MediaFormat? = null
    private var videoCodec: VideoCodec = VideoCodec.H264
    private var videoTrackId: Int = -1
    private var audioTrackId: Int = -1
    private val videoTimestamper = TrackTimestamper(
        name = "$name video",
        clockRate = 90000.0,
        syncTimestamps = syncTimestamps,
    )
    private val audioTimestamper = TrackTimestamper(
        name = "$name audio",
        clockRate = 48000.0,
        syncTimestamps = syncTimestamps,
    )
    private val scope = CoroutineScope(dispatchQueue)

    fun createPeerConnection() {
        peerConnectionId = RtcNative.rtcCreatePeerConnection(iceServers.toTypedArray())
        if (peerConnectionId < 0) {
            throw IllegalStateException("Failed to create peer connection")
        }
        RtcNative.rtcSetUserPointer(peerConnectionId, IngestClientHandles.register(this))
        checkOk(
            RtcNative.rtcSetStateChangeCallback(peerConnectionId) { _, state, pointer ->
                toIngestClient(pointer)?.handleStateChange(state)
            }
        )
        checkOk(
            RtcNative.rtcSetGatheringStateChangeCallback(peerConnectionId) { _, state, pointer ->
                toIngestClient(pointer)?.handleGatheringStateChange(state)
            }
        )
        checkOk(
            RtcNative.rtcSetTrackCallback(peerConnectionId) { _, trackId, pointer ->
                toIngestClient(pointer)?.handleTrack(trackId)
            }
        )
    }

    fun setRemoteDescription(sdp: String, type: String) {
        checkOk(RtcNative.rtcSetRemoteDescription(peerConnectionId, sdp, type))
    }

    fun setLocalDescription(type: String) {
        checkOk(RtcNative.rtcSetLocalDescription(peerConnectionId, type))
    }

    fun getLocalDescription(): String {
        if (peerConnectionId < 0) {
            throw IllegalStateException("No peer connection")
        }
        val size = RtcNative.rtcGetLocalDescription(peerConnectionId, null, 0)
        if (size <= 0) {
            throw IllegalStateException("Failed to get local description size")
        }
        val buffer = ByteArray(size)
        val result = RtcNative.rtcGetLocalDescription(peerConnectionId, buffer, size)
        if (result < 0) {
            throw IllegalStateException("Failed to get local description")
        }
        return String(buffer, Charsets.UTF_8).substringBefore('\u0000')
    }

    fun addRecvOnlyTrack(
        codec: Int,
        payloadType: Int,
        mid: String,
        msid: String,
        name: String,
        profile: String,
    ): Int {
        val trackInit = RtcTrackInit(
            direction = RtcNative.RTC_DIRECTION_RECVONLY,
            codec = codec,
            payloadType = payloadType,
            ssrc = makeSsrc(),
            mid = mid,
            name = name,
            msid = msid,
            trackId = UUID.randomUUID().toString(),
            profile = profile,
        )
        return checkOkReturnResult(RtcNative.rtcAddTrackEx(peerConnectionId, trackInit))
    }

    fun stop() {
        stopInternal()
    }

    fun setTrackCodec(trackId: Int, description: String) {
        val descriptionLower = description.lowercase()
        val clientPointer = IngestClientHandles.register(this)
        RtcNative.rtcSetUserPointer(trackId, clientPointer)
        val codec = VideoCodec.fromTrackDescription(descriptionLower)
        if (codec != null) {
            videoCodec = codec
            videoTrackId = trackId
            when (codec) {
                VideoCodec.H264 -> RtcNative.rtcSetH264Depacketizer(
                    trackId,
                    RtcNative.RTC_NAL_SEPARATOR_LONG_START_SEQUENCE,
                )
                VideoCodec.H265 -> RtcNative.rtcSetH265Depacketizer(
                    trackId,
                    RtcNative.RTC_NAL_SEPARATOR_LONG_START_SEQUENCE,
                )
            }
            RtcNative.rtcChainRtcpReceivingSession(trackId)
            RtcNative.rtcSetFrameCallback(trackId) { _, data, size, timestamp, pointer ->
                if (data != null && size > 0 && pointer != 0L) {
                    toIngestClient(pointer)?.handleVideoMessage(data, timestamp)
                }
            }
        } else if (descriptionLower.contains("opus")) {
            audioTrackId = trackId
            setupOpusDecoder()
            RtcNative.rtcSetOpusDepacketizer(trackId)
            RtcNative.rtcChainRtcpReceivingSession(trackId)
            RtcNative.rtcSetFrameCallback(trackId) { _, data, size, timestamp, pointer ->
                if (data != null && size > 0 && pointer != 0L) {
                    toIngestClient(pointer)?.handleAudioMessage(data, timestamp)
                }
            }
        }
    }

    private fun stopInternal(reason: String? = null) {
        videoDecoder?.stopRunning()
        videoDecoder = null
        stopOpusDecoder()
        RtcNative.rtcDeletePeerConnection(peerConnectionId)
        peerConnectionId = -1
        connected = false
        IngestClientHandles.unregister(this)
        if (reason != null) {
            delegate.webrtcIngestClientOnDisconnected(streamId = streamId, reason = reason)
        }
    }

    private fun handleStateChange(state: Int) {
        scope.launch {
            handleStateChangeInternal(state)
        }
    }

    private fun handleStateChangeInternal(state: Int) {
        val connectionState = DataChannelConnectionState.fromValue(state) ?: return
        Log.i(TAG, "webrtc-ingest-client: Connection state: $connectionState")
        when (connectionState) {
            DataChannelConnectionState.CONNECTED -> {
                if (connected) {
                    return
                }
                connected = true
                delegate.webrtcIngestClientOnConnected(streamId = streamId)
            }
            DataChannelConnectionState.DISCONNECTED,
            DataChannelConnectionState.FAILED,
            DataChannelConnectionState.CLOSED,
            -> stopInternal(reason = "Connection $connectionState")
            DataChannelConnectionState.NEW,
            DataChannelConnectionState.CONNECTING,
            -> {}
        }
    }

    private fun handleGatheringStateChange(state: Int) {
        scope.launch {
            handleGatheringStateChangeInternal(state)
        }
    }

    private fun handleGatheringStateChangeInternal(state: Int) {
        val gatheringState = DataChannelGatheringState.fromValue(state) ?: return
        Log.i(TAG, "webrtc-ingest-client: ICE gathering state: $gatheringState")
        when (gatheringState) {
            DataChannelGatheringState.COMPLETE -> {
                try {
                    val localDescription = getLocalDescription()
                    delegate.webrtcIngestClientOnGatheringComplete(
                        streamId = streamId,
                        localDescription = localDescription,
                    )
                } catch (e: Exception) {
                    stopInternal(reason = "Failed to get local description")
                }
            }
            DataChannelGatheringState.NEW,
            DataChannelGatheringState.IN_PROGRESS,
            -> {}
        }
    }

    private fun handleTrack(trackId: Int) {
        scope.launch {
            handleTrackInternal(trackId)
        }
    }

    private fun handleTrackInternal(trackId: Int) {
        val descBuffer = ByteArray(4096)
        val descSize = RtcNative.rtcGetTrackDescription(trackId, descBuffer, descBuffer.size)
        val description = if (descSize > 0) {
            String(descBuffer, 0, descSize, Charsets.UTF_8).substringBefore('\u0000')
        } else {
            ""
        }
        setTrackCodec(trackId = trackId, description = description)
    }

    private fun handleVideoMessage(data: ByteArray, timestamp: UInt) {
        scope.launch {
            handleVideoMessageInternal(data, timestamp)
        }
    }

    private fun handleVideoMessageInternal(data: ByteArray, timestamp: UInt) {
        delegate.webrtcIngestClientOnDataReceived(streamId = streamId, count = data.size)
        val timestampSeconds = videoTimestamper.timestampSeconds(
            videoTrackId,
            timestamp,
        ) ?: return
        val frameData = data
        val nalUnits = getNalUnits(frameData)
        val formatDescription: MediaFormat? = when (videoCodec) {
            VideoCodec.H264 -> readH264NalUnits(
                frameData,
                nalUnits,
                listOf(NalUnitType.SPS, NalUnitType.PPS, NalUnitType.IDR),
            ).makeFormatDescription()
            VideoCodec.H265 -> readH265NalUnits(
                frameData,
                nalUnits,
                listOf(NalUnitType.SPS, NalUnitType.PPS, NalUnitType.VPS),
            ).makeFormatDescription()
        }
        if (formatDescription != null &&
            !isSameFormatDescription(videoFormatDescription, formatDescription)
        ) {
            videoFormatDescription = formatDescription
            videoDecoder?.stopRunning()
            videoDecoder = null
        }
        val currentFormatDescription = videoFormatDescription ?: return
        val keyFrame = isKeyFrame(frameData, videoCodec)
        removeNalUnitStartCodes(frameData, nalUnits)
        val rebasedTimeStamp = timeStampRebaser.rebase(timestampSeconds) ?: return
        val presentationTimeStamp = getBasePresentationTimeStamp() + rebasedTimeStamp
        val sampleBuffer = MediaSample(
            data = frameData,
            presentationTimeUs = (presentationTimeStamp * 1_000_000.0).toLong(),
            isKeyFrame = keyFrame,
            format = currentFormatDescription,
        )
        if (videoDecoder == null) {
            val decoder = VideoDecoder(
                name = name,
                lockQueue = dispatchQueue,
                softwareDecoding = softwareDecoding,
            )
            decoder.delegate = this
            decoder.startRunning(formatDescription = currentFormatDescription)
            videoDecoder = decoder
        }
        videoDecoder?.decodeSampleBuffer(sampleBuffer)
    }

    private fun handleAudioMessage(data: ByteArray, timestamp: UInt) {
        scope.launch {
            handleAudioMessageInternal(data, timestamp)
        }
    }

    private fun handleAudioMessageInternal(data: ByteArray, timestamp: UInt) {
        delegate.webrtcIngestClientOnDataReceived(streamId = streamId, count = data.size)
        val timestampSeconds = audioTimestamper.timestampSeconds(
            audioTrackId,
            timestamp,
        ) ?: return
        if (data.isEmpty()) {
            return
        }
        if (opusDecoder == null) {
            return
        }
        val audioFormat = pcmAudioFormat ?: return
        if (data.size > MAXIMUM_OPUS_PACKET_SIZE) {
            return
        }
        val pcmAudioBuffer = decodeOpusPacket(data)
        val rebasedTimeStamp = timeStampRebaser.rebase(timestampSeconds) ?: return
        val presentationTimeStamp = getBasePresentationTimeStamp() + rebasedTimeStamp
        val sampleBuffer = MediaSample(
            data = pcm16ToBytes(pcmAudioBuffer),
            presentationTimeUs = (presentationTimeStamp * 1_000_000.0).toLong(),
            isKeyFrame = false,
            format = audioFormat,
        )
        delegate.webrtcIngestClientOnAudioBuffer(streamId = streamId, sampleBuffer = sampleBuffer)
    }

    private fun setupOpusDecoder() {
        stopOpusDecoder()
        pcmAudioFormat = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_RAW,
            OPUS_SAMPLE_RATE,
            OPUS_CHANNELS,
        ).apply {
            setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
        }
        opusDecoder = TODO("MediaCodec audio/opus decoder requires the OpusHead codec specific data from the SDP")
    }

    private fun stopOpusDecoder() {
        opusDecoder?.stop()
        opusDecoder?.release()
        opusDecoder = null
        pcmAudioFormat = null
    }

    private fun decodeOpusPacket(data: ByteArray): ShortArray {
        return TODO("MediaCodec audio/opus decode of a single Opus packet")
    }

    private fun pcm16ToBytes(pcm: ShortArray): ByteArray {
        val buffer = ByteBuffer.allocate(pcm.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (sample in pcm) {
            buffer.putShort(sample)
        }
        return buffer.array()
    }

    private fun getBasePresentationTimeStamp(): Double {
        if (basePresentationTimeStamp == -1.0) {
            basePresentationTimeStamp = currentPresentationTimeStamp() / 1_000_000.0 + latency
        }
        return basePresentationTimeStamp
    }

    private fun isSameFormatDescription(a: MediaFormat?, b: MediaFormat?): Boolean {
        if (a == null || b == null) {
            return false
        }
        return a.toString() == b.toString()
    }

    override fun videoDecoderOutputSampleBuffer(decoder: VideoDecoder, sampleBuffer: MediaSample) {
        delegate.webrtcIngestClientOnVideoBuffer(streamId = streamId, sampleBuffer = sampleBuffer)
    }
}

private fun isKeyFrame(data: ByteArray, codec: VideoCodec): Boolean {
    var i = 0
    while (i + 3 < data.size) {
        if (data[i] == 0.toByte() && data[i + 1] == 0.toByte() && data[i + 2] == 1.toByte()) {
            val header = data[i + 3].toInt() and 0xFF
            when (codec) {
                VideoCodec.H264 -> if ((header and 0x1F) == 5) {
                    return true
                }
                VideoCodec.H265 -> if (((header shr 1) and 0x3F) in 16..21) {
                    return true
                }
            }
            i += 4
        } else {
            i += 1
        }
    }
    return false
}

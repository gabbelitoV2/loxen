package com.moblin.android.media.webrtc

import android.media.MediaFormat
import android.util.Log
import com.moblin.android.media.MediaSample
import com.moblin.android.media.WrappingTimestamp
import com.moblin.android.media.haishinkit.codec.video.VideoDecoder
import com.moblin.android.media.haishinkit.codec.video.VideoDecoderDelegate
import com.moblin.android.media.haishinkit.mpeg.avc.AvcNalUnitType
import com.moblin.android.media.haishinkit.mpeg.avc.makeFormatDescription
import com.moblin.android.media.haishinkit.mpeg.getNalUnits
import com.moblin.android.media.haishinkit.mpeg.hevc.HevcNalUnitType
import com.moblin.android.media.haishinkit.mpeg.hevc.makeFormatDescription
import com.moblin.android.media.haishinkit.mpeg.readH264NalUnits
import com.moblin.android.media.haishinkit.mpeg.readH265NalUnits
import com.moblin.android.media.haishinkit.mpeg.removeNalUnitStartCodes
import com.moblin.android.media.haishinkit.whip.checkOk
import com.moblin.android.media.haishinkit.whip.checkOkReturnResult
import com.moblin.android.media.haishinkit.whip.makeSsrc
import com.moblin.android.platform.audio.makePcmFormat
import com.moblin.android.platform.avfoundation.AVAudioCompressedBuffer
import com.moblin.android.platform.avfoundation.AVAudioConverter
import com.moblin.android.platform.avfoundation.AVAudioPCMBuffer
import com.moblin.android.platform.datachannel.*
import com.moblin.android.platform.videotoolbox.CMFormatDescriptionEqual
import com.moblin.android.various.utils.TimeStampRebaser
import com.moblin.android.various.utils.currentPresentationTimeStamp
import com.moblin.android.various.utils.stringFromCArray
import com.moblin.android.various.utils.withCPointers
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val TAG = "WebrtcIngestClient"

private const val opusMaximumPacketSize = 4096

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
    val nanos = (((v and 0xFFFF_FFFFuL) * 1_000_000_000uL) / (1uL shl 32)).toDouble().toLong()
    return secs.toDouble() + nanos.toDouble() / 1_000_000_000
}

private fun toIngestClient(pointer: Any?): WebrtcIngestClient? {
    return pointer as? WebrtcIngestClient
}

private enum class VideoCodec {
    h264,
    h265,
    ;

    companion object {
        operator fun invoke(trackDescription: String): VideoCodec? {
            return if (trackDescription.contains("h264")) {
                h264
            } else if (trackDescription.contains("h265")) {
                h265
            } else {
                null
            }
        }
    }
}

private class TrackTimestamper(name: String, private val clockRate: Double, private val syncTimestamps: Boolean) {
    private val wrappingTimestamp = WrappingTimestamp(
        name = name,
        maximumTimestamp = 0x1_0000_0000L * 1_000_000L,
    )
    private var offset: Double? = null

    fun timestampSeconds(trackId: Int, timestamp: UInt): Double? {
        val timestampSeconds = unwrap(timestamp)
        if (!syncTimestamps) {
            return timestampSeconds
        }
        if (offset == null) {
            val rtpTimestamp = LongArray(1)
            val ntpTimestamp = LongArray(1)
            rtcGetTrackRtcpSyncTimestamps(trackId, rtpTimestamp, ntpTimestamp)
            val ntpTimestampSeconds = decodeNtpTimestamp(v = ntpTimestamp[0].toULong()) ?: return null
            offset = ntpTimestampSeconds - unwrap(rtpTimestamp[0].toUInt())
        }
        return timestampSeconds + offset!!
    }

    private fun unwrap(timestamp: UInt): Double {
        val timestampUs = timestamp.toLong() * 1_000_000L
        return (wrappingTimestamp.update(timestampUs) / 1_000_000L).toDouble() / clockRate
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
    delegate: WebrtcIngestClientDelegate,
) : VideoDecoderDelegate {
    var peerConnectionId: Int = -1
        private set
    private val delegate: WebrtcIngestClientDelegate? = delegate
    private var connected = false
    private var videoDecoder: VideoDecoder? = null
    private var videoFormatDescription: MediaFormat? = null
    private var basePresentationTimeStamp: Double = -1.0
    private var timeStampRebaser = TimeStampRebaser()
    private var opusAudioConverter: AVAudioConverter? = null
    private var opusCompressedBuffer: AVAudioCompressedBuffer? = null
    private var pcmAudioFormat: MediaFormat? = null
    private var pcmAudioBuffer: AVAudioPCMBuffer? = null
    private var videoCodec: VideoCodec = VideoCodec.h264
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
        val config = rtcConfiguration()
        peerConnectionId = iceServers.withCPointers {
            config.iceServers = it
            config.iceServersCount = iceServers.size
            rtcCreatePeerConnection(config)
        }
        if (peerConnectionId < 0) {
            throw DataChannelError("Failed to create peer connection")
        }
        rtcSetUserPointer(peerConnectionId, this)
        checkOk(
            rtcSetStateChangeCallback(peerConnectionId) { _, state, pointer ->
                toIngestClient(pointer)?.handleStateChange(state = state)
            },
        )
        checkOk(
            rtcSetGatheringStateChangeCallback(peerConnectionId) { _, state, pointer ->
                toIngestClient(pointer)?.handleGatheringStateChange(state = state)
            },
        )
        checkOk(
            rtcSetTrackCallback(peerConnectionId) { _, trackId, pointer ->
                toIngestClient(pointer)?.handleTrack(trackId = trackId)
            },
        )
    }

    fun setRemoteDescription(sdp: String, type: String) {
        checkOk(rtcSetRemoteDescription(peerConnectionId, sdp, type))
    }

    fun setLocalDescription(type: String) {
        checkOk(rtcSetLocalDescription(peerConnectionId, type))
    }

    fun getLocalDescription(): String {
        if (peerConnectionId < 0) {
            throw DataChannelError("No peer connection")
        }
        val size = rtcGetLocalDescription(peerConnectionId, null, 0)
        if (size <= 0) {
            throw DataChannelError("Failed to get local description size")
        }
        val buffer = ByteArray(size)
        val result = rtcGetLocalDescription(peerConnectionId, buffer, size)
        if (result < 0) {
            throw DataChannelError("Failed to get local description")
        }
        return stringFromCArray(buffer)
    }

    fun addRecvOnlyTrack(
        codec: rtcCodec,
        payloadType: Int,
        mid: String,
        msid: String,
        name: String,
        profile: String,
    ): Int {
        val trackInit = rtcTrackInit(
            direction = RTC_DIRECTION_RECVONLY,
            codec = codec,
            payloadType = payloadType,
            ssrc = makeSsrc(),
            mid = mid,
            name = name,
            msid = msid,
            trackId = UUID.randomUUID().toString().uppercase(),
            profile = profile,
        )
        return checkOkReturnResult(rtcAddTrackEx(peerConnectionId, trackInit))
    }

    fun stop() {
        stopInternal()
    }

    fun setTrackCodec(trackId: Int, description: String) {
        val descriptionLower = description.lowercase()
        val clientPointer = this
        rtcSetUserPointer(trackId, clientPointer)
        val videoCodec = VideoCodec(trackDescription = descriptionLower)
        if (videoCodec != null) {
            this.videoCodec = videoCodec
            videoTrackId = trackId
            when (videoCodec) {
                VideoCodec.h264 -> rtcSetH264Depacketizer(trackId, RTC_NAL_SEPARATOR_LONG_START_SEQUENCE)
                VideoCodec.h265 -> rtcSetH265Depacketizer(trackId, RTC_NAL_SEPARATOR_LONG_START_SEQUENCE)
            }
            rtcChainRtcpReceivingSession(trackId)
            rtcSetFrameCallback(trackId) { _, data, size, info, pointer ->
                if (data == null || size <= 0 || info == null || pointer == null) {
                    return@rtcSetFrameCallback
                }
                val frameData = data
                val timestamp = info.timestamp
                toIngestClient(pointer)?.handleVideoMessage(data = frameData, timestamp = timestamp)
            }
        } else if (descriptionLower.contains("opus")) {
            audioTrackId = trackId
            setupOpusDecoder()
            rtcSetOpusDepacketizer(trackId)
            rtcChainRtcpReceivingSession(trackId)
            rtcSetFrameCallback(trackId) { _, data, size, info, pointer ->
                if (data == null || size <= 0 || info == null || pointer == null) {
                    return@rtcSetFrameCallback
                }
                val frameData = data
                val timestamp = info.timestamp
                toIngestClient(pointer)?.handleAudioMessage(data = frameData, timestamp = timestamp)
            }
        }
    }

    private fun stopInternal(reason: String? = null) {
        val hadPeerConnection = peerConnectionId >= 0
        videoDecoder?.stopRunning()
        videoDecoder = null
        opusAudioConverter?.release()
        opusAudioConverter = null
        opusCompressedBuffer = null
        pcmAudioBuffer = null
        rtcDeletePeerConnection(peerConnectionId)
        peerConnectionId = -1
        connected = false
        if (reason != null && hadPeerConnection) {
            delegate?.webrtcIngestClientOnDisconnected(streamId = streamId, reason = reason)
        }
    }

    private fun handleStateChange(state: rtcState) {
        scope.launch {
            handleStateChangeInternal(state = state)
        }
    }

    private fun handleStateChangeInternal(state: rtcState) {
        val connectionState = DataChannelConnectionState(value = state) ?: return
        Log.i(TAG, "webrtc-ingest-client: Connection state: $connectionState")
        when (connectionState) {
            DataChannelConnectionState.connected -> {
                if (connected) {
                    return
                }
                connected = true
                delegate?.webrtcIngestClientOnConnected(streamId = streamId)
            }
            DataChannelConnectionState.disconnected,
            DataChannelConnectionState.failed,
            DataChannelConnectionState.closed,
            -> stopInternal(reason = "Connection $connectionState")
            DataChannelConnectionState.new,
            DataChannelConnectionState.connecting,
            -> Unit
        }
    }

    private fun handleGatheringStateChange(state: rtcGatheringState) {
        scope.launch {
            handleGatheringStateChangeInternal(state = state)
        }
    }

    private fun handleGatheringStateChangeInternal(state: rtcGatheringState) {
        val gatheringState = DataChannelGatheringState(value = state) ?: return
        Log.i(TAG, "webrtc-ingest-client: ICE gathering state: $gatheringState")
        when (gatheringState) {
            DataChannelGatheringState.complete -> {
                try {
                    val localDescription = getLocalDescription()
                    delegate?.webrtcIngestClientOnGatheringComplete(
                        streamId = streamId,
                        localDescription = localDescription,
                    )
                } catch (error: Exception) {
                    stopInternal(reason = "Failed to get local description")
                }
            }
            DataChannelGatheringState.new,
            DataChannelGatheringState.inProgress,
            -> Unit
        }
    }

    private fun handleTrack(trackId: Int) {
        scope.launch {
            handleTrackInternal(trackId = trackId)
        }
    }

    private fun handleTrackInternal(trackId: Int) {
        if (peerConnectionId < 0) return
        val descBuffer = ByteArray(4096)
        val descSize = rtcGetTrackDescription(trackId, descBuffer, descBuffer.size)
        val description = if (descSize > 0) stringFromCArray(descBuffer) else ""
        setTrackCodec(trackId = trackId, description = description)
    }

    private fun handleVideoMessage(data: ByteArray, timestamp: UInt) {
        scope.launch {
            handleVideoMessageInternal(data = data, timestamp = timestamp)
        }
    }

    private fun handleVideoMessageInternal(data: ByteArray, timestamp: UInt) {
        if (peerConnectionId < 0) return
        delegate?.webrtcIngestClientOnDataReceived(streamId = streamId, count = data.size)
        val timestampSeconds = videoTimestamper.timestampSeconds(
            trackId = videoTrackId,
            timestamp = timestamp,
        ) ?: return
        var frameData = data
        val nalUnits = getNalUnits(frameData)
        val formatDescription: MediaFormat? = when (videoCodec) {
            VideoCodec.h264 -> {
                val units = readH264NalUnits(
                    frameData,
                    nalUnits,
                    listOf(AvcNalUnitType.sps, AvcNalUnitType.pps, AvcNalUnitType.idr),
                )
                units.makeFormatDescription()
            }
            VideoCodec.h265 -> {
                val units = readH265NalUnits(
                    frameData,
                    nalUnits,
                    listOf(HevcNalUnitType.sps, HevcNalUnitType.pps, HevcNalUnitType.vps),
                )
                units.makeFormatDescription()
            }
        }
        if (formatDescription != null && !CMFormatDescriptionEqual(videoFormatDescription, formatDescription)) {
            videoFormatDescription = formatDescription
            videoDecoder?.stopRunning()
            videoDecoder = null
        }
        val videoFormatDescription = videoFormatDescription ?: return
        frameData = removeNalUnitStartCodes(frameData, nalUnits)
        val rebasedTimeStamp = timeStampRebaser.rebase(timestampSeconds) ?: return
        val presentationTimeStamp = getBasePresentationTimeStamp() + rebasedTimeStamp
        val sampleBuffer = MediaSample(
            data = frameData,
            presentationTimeUs = (presentationTimeStamp * 1_000_000.0).toLong(),
            isKeyFrame = true,
            format = videoFormatDescription,
        )
        if (videoDecoder == null) {
            videoDecoder = VideoDecoder(
                name = name,
                lockQueue = scope,
                softwareDecoding = softwareDecoding,
            )
            videoDecoder?.delegate = this
            videoDecoder?.startRunning(formatDescription = videoFormatDescription)
        }
        videoDecoder?.decodeSampleBuffer(sampleBuffer)
    }

    private fun handleAudioMessage(data: ByteArray, timestamp: UInt) {
        scope.launch {
            handleAudioMessageInternal(data = data, timestamp = timestamp)
        }
    }

    private fun handleAudioMessageInternal(data: ByteArray, timestamp: UInt) {
        if (peerConnectionId < 0) return
        delegate?.webrtcIngestClientOnDataReceived(streamId = streamId, count = data.size)
        val timestampSeconds = audioTimestamper.timestampSeconds(
            trackId = audioTrackId,
            timestamp = timestamp,
        ) ?: return
        if (data.isEmpty()) {
            return
        }
        val opusCompressedBuffer = opusCompressedBuffer
        val opusAudioConverter = opusAudioConverter
        val pcmAudioBuffer = pcmAudioBuffer
        if (opusCompressedBuffer == null ||
            opusAudioConverter == null ||
            pcmAudioBuffer == null ||
            pcmAudioFormat == null
        ) {
            return
        }
        val length = data.size
        if (length > opusMaximumPacketSize) {
            return
        }
        opusCompressedBuffer.data = data
        val error = opusAudioConverter.convert(to = pcmAudioBuffer) {
            opusCompressedBuffer.data
        }
        if (error != null) {
            Log.i(TAG, "webrtc-ingest-client: Opus decode error: $error")
            return
        }
        val rebasedTimeStamp = timeStampRebaser.rebase(timestampSeconds) ?: return
        val presentationTimeStamp = getBasePresentationTimeStamp() + rebasedTimeStamp
        val pts = (presentationTimeStamp * 1_000_000.0).toLong()
        val sampleBuffer = pcmAudioBuffer.replacePresentationTimeStamp(pts)
        delegate?.webrtcIngestClientOnAudioBuffer(streamId = streamId, sampleBuffer = sampleBuffer)
    }

    private fun setupOpusDecoder() {
        val opusFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_OPUS, 48000, 2)
        opusCompressedBuffer = AVAudioCompressedBuffer(
            format = opusFormat,
            packetCapacity = 1,
            maximumPacketSize = opusMaximumPacketSize,
        )
        val pcmAudioFormat = makePcmFormat(sampleRate = 48000, channels = 2)
        this.pcmAudioFormat = pcmAudioFormat
        pcmAudioBuffer = AVAudioPCMBuffer(pcmFormat = pcmAudioFormat, frameCapacity = 960)
        opusAudioConverter?.release()
        opusAudioConverter = AVAudioConverter.create(from = opusFormat, to = pcmAudioFormat)
        if (opusAudioConverter == null) {
            Log.i(TAG, "webrtc-ingest-client: Failed to create Opus audio converter")
        }
    }

    private fun getBasePresentationTimeStamp(): Double {
        if (basePresentationTimeStamp == -1.0) {
            basePresentationTimeStamp = currentPresentationTimeStamp() / 1_000_000.0 + latency
        }
        return basePresentationTimeStamp
    }

    override fun videoDecoderOutputSampleBuffer(codec: VideoDecoder, sampleBuffer: MediaSample) {
        delegate?.webrtcIngestClientOnVideoBuffer(streamId = streamId, sampleBuffer = sampleBuffer)
    }
}

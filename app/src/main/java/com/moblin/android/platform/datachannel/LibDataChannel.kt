package com.moblin.android.platform.datachannel

import android.util.Log
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

private const val TAG = "MoblinDataChannel"

typealias rtcState = Int
typealias rtcIceState = Int
typealias rtcGatheringState = Int
typealias rtcSignalingState = Int
typealias rtcLogLevel = Int
typealias rtcCertificateType = Int
typealias rtcCodec = Int
typealias rtcDirection = Int
typealias rtcTransportPolicy = Int
typealias rtcNalUnitSeparator = Int
typealias rtcObuPacketization = Int

const val RTC_NEW: rtcState = 0
const val RTC_CONNECTING: rtcState = 1
const val RTC_CONNECTED: rtcState = 2
const val RTC_DISCONNECTED: rtcState = 3
const val RTC_FAILED: rtcState = 4
const val RTC_CLOSED: rtcState = 5

const val RTC_ICE_NEW: rtcIceState = 0
const val RTC_ICE_CHECKING: rtcIceState = 1
const val RTC_ICE_CONNECTED: rtcIceState = 2
const val RTC_ICE_COMPLETED: rtcIceState = 3
const val RTC_ICE_FAILED: rtcIceState = 4
const val RTC_ICE_DISCONNECTED: rtcIceState = 5
const val RTC_ICE_CLOSED: rtcIceState = 6

const val RTC_GATHERING_NEW: rtcGatheringState = 0
const val RTC_GATHERING_INPROGRESS: rtcGatheringState = 1
const val RTC_GATHERING_COMPLETE: rtcGatheringState = 2

const val RTC_SIGNALING_STABLE: rtcSignalingState = 0
const val RTC_SIGNALING_HAVE_LOCAL_OFFER: rtcSignalingState = 1
const val RTC_SIGNALING_HAVE_REMOTE_OFFER: rtcSignalingState = 2
const val RTC_SIGNALING_HAVE_LOCAL_PRANSWER: rtcSignalingState = 3
const val RTC_SIGNALING_HAVE_REMOTE_PRANSWER: rtcSignalingState = 4

const val RTC_LOG_NONE: rtcLogLevel = 0
const val RTC_LOG_FATAL: rtcLogLevel = 1
const val RTC_LOG_ERROR: rtcLogLevel = 2
const val RTC_LOG_WARNING: rtcLogLevel = 3
const val RTC_LOG_INFO: rtcLogLevel = 4
const val RTC_LOG_DEBUG: rtcLogLevel = 5
const val RTC_LOG_VERBOSE: rtcLogLevel = 6

const val RTC_CERTIFICATE_DEFAULT: rtcCertificateType = 0
const val RTC_CERTIFICATE_ECDSA: rtcCertificateType = 1
const val RTC_CERTIFICATE_RSA: rtcCertificateType = 2

const val RTC_CODEC_H264: rtcCodec = 0
const val RTC_CODEC_VP8: rtcCodec = 1
const val RTC_CODEC_VP9: rtcCodec = 2
const val RTC_CODEC_H265: rtcCodec = 3
const val RTC_CODEC_AV1: rtcCodec = 4
const val RTC_CODEC_OPUS: rtcCodec = 128
const val RTC_CODEC_PCMU: rtcCodec = 129
const val RTC_CODEC_PCMA: rtcCodec = 130
const val RTC_CODEC_AAC: rtcCodec = 131
const val RTC_CODEC_G722: rtcCodec = 132

const val RTC_DIRECTION_UNKNOWN: rtcDirection = 0
const val RTC_DIRECTION_SENDONLY: rtcDirection = 1
const val RTC_DIRECTION_RECVONLY: rtcDirection = 2
const val RTC_DIRECTION_SENDRECV: rtcDirection = 3
const val RTC_DIRECTION_INACTIVE: rtcDirection = 4

const val RTC_TRANSPORT_POLICY_ALL: rtcTransportPolicy = 0
const val RTC_TRANSPORT_POLICY_RELAY: rtcTransportPolicy = 1

const val RTC_ERR_SUCCESS: Int = 0
const val RTC_ERR_INVALID: Int = -1
const val RTC_ERR_FAILURE: Int = -2
const val RTC_ERR_NOT_AVAIL: Int = -3
const val RTC_ERR_TOO_SMALL: Int = -4

const val RTC_DEFAULT_MTU: Int = 1280
const val RTC_DEFAULT_MAX_FRAGMENT_SIZE: Int = RTC_DEFAULT_MTU - 12 - 8 - 40
const val RTC_DEFAULT_MAX_STORED_PACKET_COUNT: Int = 512
const val RTC_DEFAULT_MAXIMUM_FRAGMENT_SIZE: Int = RTC_DEFAULT_MAX_FRAGMENT_SIZE
const val RTC_DEFAULT_MAXIMUM_PACKET_COUNT_FOR_NACK_CACHE: Int = RTC_DEFAULT_MAX_STORED_PACKET_COUNT

const val RTC_OBU_PACKETIZED_OBU: rtcObuPacketization = 0
const val RTC_OBU_PACKETIZED_TEMPORAL_UNIT: rtcObuPacketization = 1

const val RTC_NAL_SEPARATOR_LENGTH: rtcNalUnitSeparator = 0
const val RTC_NAL_SEPARATOR_LONG_START_SEQUENCE: rtcNalUnitSeparator = 1
const val RTC_NAL_SEPARATOR_SHORT_START_SEQUENCE: rtcNalUnitSeparator = 2
const val RTC_NAL_SEPARATOR_START_SEQUENCE: rtcNalUnitSeparator = 3

typealias rtcLogCallbackFunc = (level: rtcLogLevel, message: String?) -> Unit
typealias rtcStateChangeCallbackFunc = (pc: Int, state: rtcState, ptr: Any?) -> Unit
typealias rtcGatheringStateCallbackFunc = (pc: Int, state: rtcGatheringState, ptr: Any?) -> Unit
typealias rtcTrackCallbackFunc = (pc: Int, tr: Int, ptr: Any?) -> Unit
typealias rtcOpenCallbackFunc = (id: Int, ptr: Any?) -> Unit
typealias rtcClosedCallbackFunc = (id: Int, ptr: Any?) -> Unit
typealias rtcErrorCallbackFunc = (id: Int, error: String?, ptr: Any?) -> Unit
typealias rtcPliHandlerCallbackFunc = (tr: Int, ptr: Any?) -> Unit
typealias rtcRembHandlerCallbackFunc = (tr: Int, bitrate: UInt, ptr: Any?) -> Unit
typealias rtcFrameCallbackFunc = (tr: Int, data: ByteArray?, size: Int, info: rtcFrameInfo?, ptr: Any?) -> Unit

class rtcFrameInfo(
    val timestamp: UInt,
    val payloadType: UByte,
    val timestampSeconds: Double,
)

class rtcConfiguration(
    var iceServers: Array<ByteArray?>? = null,
    var iceServersCount: Int = 0,
    var proxyServer: String? = null,
    var bindAddress: String? = null,
    var certificateType: rtcCertificateType = RTC_CERTIFICATE_DEFAULT,
    var certificatePemFile: String? = null,
    var keyPemFile: String? = null,
    var keyPemPass: String? = null,
    var iceTransportPolicy: rtcTransportPolicy = RTC_TRANSPORT_POLICY_ALL,
    var enableIceTcp: Boolean = false,
    var enableIceUdpMux: Boolean = false,
    var disableAutoNegotiation: Boolean = false,
    var forceMediaTransport: Boolean = false,
    var portRangeBegin: UShort = 0u,
    var portRangeEnd: UShort = 0u,
    var mtu: Int = 0,
    var maxMessageSize: Int = 0,
    var disableFingerprintVerification: Boolean = false,
)

class rtcTrackInit(
    var direction: rtcDirection = RTC_DIRECTION_UNKNOWN,
    var codec: rtcCodec = RTC_CODEC_H264,
    var payloadType: Int = 0,
    var ssrc: UInt = 0u,
    var mid: String? = null,
    var name: String? = null,
    var msid: String? = null,
    var trackId: String? = null,
    var profile: String? = null,
)

class rtcPacketizerInit(
    var ssrc: UInt = 0u,
    var cname: String? = null,
    var payloadType: UByte = 0u,
    var clockRate: UInt = 0u,
    var sequenceNumber: UShort = 0u,
    var timestamp: UInt = 0u,
    var maxFragmentSize: UShort = 0u,
    var nalSeparator: rtcNalUnitSeparator = RTC_NAL_SEPARATOR_LENGTH,
    var obuPacketization: rtcObuPacketization = RTC_OBU_PACKETIZED_OBU,
    var playoutDelayId: UByte = 0u,
    var playoutDelayMin: UShort = 0u,
    var playoutDelayMax: UShort = 0u,
    var colorSpaceId: UByte = 0u,
    var colorChromaSitingHorz: UByte = 0u,
    var colorChromaSitingVert: UByte = 0u,
    var colorRange: UByte = 0u,
    var colorPrimaries: UByte = 0u,
    var colorTransfer: UByte = 0u,
    var colorMatrix: UByte = 0u,
)

typealias rtcPacketizationHandlerInit = rtcPacketizerInit

fun rtcInitLogger(level: rtcLogLevel, cb: rtcLogCallbackFunc?) {
    DataChannelCallbacks.log = cb
    if (DataChannelNative.isLoaded) {
        DataChannelNative.rtcInitLogger(level, cb != null)
    }
}

fun rtcSetUserPointer(id: Int, ptr: Any?) {
    if (DataChannelNative.isLoaded) {
        DataChannelNative.rtcSetUserPointer(id, DataChannelUserPointers.register(ptr))
    }
}

fun rtcGetUserPointer(id: Int): Any? {
    if (!DataChannelNative.isLoaded) {
        return null
    }
    return DataChannelUserPointers.get(DataChannelNative.rtcGetUserPointer(id))
}

fun rtcCreatePeerConnection(config: rtcConfiguration): Int {
    if (!DataChannelNative.isLoaded) {
        return RTC_ERR_FAILURE
    }
    val fields = intArrayOf(
        config.certificateType,
        config.iceTransportPolicy,
        if (config.enableIceTcp) 1 else 0,
        if (config.enableIceUdpMux) 1 else 0,
        if (config.disableAutoNegotiation) 1 else 0,
        if (config.forceMediaTransport) 1 else 0,
        config.portRangeBegin.toInt(),
        config.portRangeEnd.toInt(),
        config.mtu,
        config.maxMessageSize,
        if (config.disableFingerprintVerification) 1 else 0,
    )
    return DataChannelNative.rtcCreatePeerConnection(
        config.iceServers,
        config.iceServersCount,
        cStringOf(config.proxyServer),
        cStringOf(config.bindAddress),
        cStringOf(config.certificatePemFile),
        cStringOf(config.keyPemFile),
        cStringOf(config.keyPemPass),
        fields,
    )
}

fun rtcClosePeerConnection(pc: Int): Int {
    if (!DataChannelNative.isLoaded) {
        return RTC_ERR_FAILURE
    }
    return DataChannelNative.rtcClosePeerConnection(pc)
}

fun rtcDeletePeerConnection(pc: Int): Int {
    if (!DataChannelNative.isLoaded) {
        return RTC_ERR_FAILURE
    }
    val result = DataChannelNative.rtcDeletePeerConnection(pc)
    if (result >= 0) {
        DataChannelCallbacks.removePeerConnection(pc)
    }
    return result
}

fun rtcSetStateChangeCallback(pc: Int, cb: rtcStateChangeCallbackFunc?): Int {
    return DataChannelCallbacks.set(DataChannelCallbacks.stateChange, pc, cb) {
        DataChannelNative.rtcSetStateChangeCallback(pc, it)
    }
}

fun rtcSetGatheringStateChangeCallback(pc: Int, cb: rtcGatheringStateCallbackFunc?): Int {
    return DataChannelCallbacks.set(DataChannelCallbacks.gatheringStateChange, pc, cb) {
        DataChannelNative.rtcSetGatheringStateChangeCallback(pc, it)
    }
}

fun rtcSetLocalDescription(pc: Int, type: String?): Int {
    if (!DataChannelNative.isLoaded) {
        return RTC_ERR_FAILURE
    }
    return DataChannelNative.rtcSetLocalDescription(pc, cStringOf(type))
}

fun rtcSetRemoteDescription(pc: Int, sdp: String?, type: String?): Int {
    if (!DataChannelNative.isLoaded) {
        return RTC_ERR_FAILURE
    }
    return DataChannelNative.rtcSetRemoteDescription(pc, cStringOf(sdp), cStringOf(type))
}

fun rtcAddRemoteCandidate(pc: Int, cand: String?, mid: String?): Int {
    if (!DataChannelNative.isLoaded) {
        return RTC_ERR_FAILURE
    }
    return DataChannelNative.rtcAddRemoteCandidate(pc, cStringOf(cand), cStringOf(mid))
}

fun rtcGetLocalDescription(pc: Int, buffer: ByteArray?, size: Int): Int {
    if (!DataChannelNative.isLoaded) {
        return RTC_ERR_FAILURE
    }
    return DataChannelNative.rtcGetLocalDescription(pc, buffer, size)
}

fun rtcGetRemoteDescription(pc: Int, buffer: ByteArray?, size: Int): Int {
    if (!DataChannelNative.isLoaded) {
        return RTC_ERR_FAILURE
    }
    return DataChannelNative.rtcGetRemoteDescription(pc, buffer, size)
}

fun rtcGetLocalDescriptionType(pc: Int, buffer: ByteArray?, size: Int): Int {
    if (!DataChannelNative.isLoaded) {
        return RTC_ERR_FAILURE
    }
    return DataChannelNative.rtcGetLocalDescriptionType(pc, buffer, size)
}

fun rtcGetRemoteDescriptionType(pc: Int, buffer: ByteArray?, size: Int): Int {
    if (!DataChannelNative.isLoaded) {
        return RTC_ERR_FAILURE
    }
    return DataChannelNative.rtcGetRemoteDescriptionType(pc, buffer, size)
}

fun rtcGetLocalAddress(pc: Int, buffer: ByteArray?, size: Int): Int {
    if (!DataChannelNative.isLoaded) {
        return RTC_ERR_FAILURE
    }
    return DataChannelNative.rtcGetLocalAddress(pc, buffer, size)
}

fun rtcGetRemoteAddress(pc: Int, buffer: ByteArray?, size: Int): Int {
    if (!DataChannelNative.isLoaded) {
        return RTC_ERR_FAILURE
    }
    return DataChannelNative.rtcGetRemoteAddress(pc, buffer, size)
}

fun rtcGetSelectedCandidatePair(
    pc: Int,
    local: ByteArray?,
    localSize: Int,
    remote: ByteArray?,
    remoteSize: Int,
): Int {
    if (!DataChannelNative.isLoaded) {
        return RTC_ERR_FAILURE
    }
    return DataChannelNative.rtcGetSelectedCandidatePair(pc, local, localSize, remote, remoteSize)
}

fun rtcSetOpenCallback(id: Int, cb: rtcOpenCallbackFunc?): Int {
    return DataChannelCallbacks.set(DataChannelCallbacks.open, id, cb) {
        DataChannelNative.rtcSetOpenCallback(id, it)
    }
}

fun rtcSetClosedCallback(id: Int, cb: rtcClosedCallbackFunc?): Int {
    return DataChannelCallbacks.set(DataChannelCallbacks.closed, id, cb) {
        DataChannelNative.rtcSetClosedCallback(id, it)
    }
}

fun rtcSetErrorCallback(id: Int, cb: rtcErrorCallbackFunc?): Int {
    return DataChannelCallbacks.set(DataChannelCallbacks.error, id, cb) {
        DataChannelNative.rtcSetErrorCallback(id, it)
    }
}

fun rtcSendMessage(id: Int, data: ByteArray?, size: Int): Int {
    if (!DataChannelNative.isLoaded) {
        return RTC_ERR_FAILURE
    }
    return DataChannelNative.rtcSendMessage(id, data, size)
}

fun rtcClose(id: Int): Int {
    if (!DataChannelNative.isLoaded) {
        return RTC_ERR_FAILURE
    }
    return DataChannelNative.rtcClose(id)
}

fun rtcDelete(id: Int): Int {
    if (!DataChannelNative.isLoaded) {
        return RTC_ERR_FAILURE
    }
    val result = DataChannelNative.rtcDelete(id)
    if (result >= 0) {
        DataChannelCallbacks.removePeerConnection(id)
        DataChannelCallbacks.removeId(id)
    }
    return result
}

fun rtcIsOpen(id: Int): Boolean {
    return DataChannelNative.isLoaded && DataChannelNative.rtcIsOpen(id)
}

fun rtcIsClosed(id: Int): Boolean {
    return !DataChannelNative.isLoaded || DataChannelNative.rtcIsClosed(id)
}

fun rtcSetTrackCallback(pc: Int, cb: rtcTrackCallbackFunc?): Int {
    return DataChannelCallbacks.set(DataChannelCallbacks.track, pc, cb) {
        DataChannelNative.rtcSetTrackCallback(pc, it)
    }
}

fun rtcAddTrack(pc: Int, mediaDescriptionSdp: String?): Int {
    if (!DataChannelNative.isLoaded) {
        return RTC_ERR_FAILURE
    }
    val tr = DataChannelNative.rtcAddTrack(pc, cStringOf(mediaDescriptionSdp))
    DataChannelCallbacks.addTrack(pc, tr)
    return tr
}

fun rtcAddTrackEx(pc: Int, init: rtcTrackInit): Int {
    if (!DataChannelNative.isLoaded) {
        return RTC_ERR_FAILURE
    }
    val tr = DataChannelNative.rtcAddTrackEx(
        pc,
        init.direction,
        init.codec,
        init.payloadType,
        init.ssrc.toInt(),
        cStringOf(init.mid),
        cStringOf(init.name),
        cStringOf(init.msid),
        cStringOf(init.trackId),
        cStringOf(init.profile),
    )
    DataChannelCallbacks.addTrack(pc, tr)
    return tr
}

fun rtcDeleteTrack(tr: Int): Int {
    if (!DataChannelNative.isLoaded) {
        return RTC_ERR_FAILURE
    }
    val result = DataChannelNative.rtcDeleteTrack(tr)
    if (result >= 0) {
        DataChannelCallbacks.removeId(tr)
    }
    return result
}

fun rtcGetTrackDescription(tr: Int, buffer: ByteArray?, size: Int): Int {
    if (!DataChannelNative.isLoaded) {
        return RTC_ERR_FAILURE
    }
    return DataChannelNative.rtcGetTrackDescription(tr, buffer, size)
}

fun rtcGetTrackMid(tr: Int, buffer: ByteArray?, size: Int): Int {
    if (!DataChannelNative.isLoaded) {
        return RTC_ERR_FAILURE
    }
    return DataChannelNative.rtcGetTrackMid(tr, buffer, size)
}

fun rtcGetTrackDirection(tr: Int, direction: IntArray): Int {
    if (!DataChannelNative.isLoaded) {
        return RTC_ERR_FAILURE
    }
    return DataChannelNative.rtcGetTrackDirection(tr, direction)
}

fun rtcRequestKeyframe(tr: Int): Int {
    if (!DataChannelNative.isLoaded) {
        return RTC_ERR_FAILURE
    }
    return DataChannelNative.rtcRequestKeyframe(tr)
}

fun rtcRequestBitrate(tr: Int, bitrate: UInt): Int {
    if (!DataChannelNative.isLoaded) {
        return RTC_ERR_FAILURE
    }
    return DataChannelNative.rtcRequestBitrate(tr, bitrate.toInt())
}

fun rtcSetFrameCallback(tr: Int, cb: rtcFrameCallbackFunc?): Int {
    return DataChannelCallbacks.set(DataChannelCallbacks.frame, tr, cb) {
        DataChannelNative.rtcSetFrameCallback(tr, it)
    }
}

fun rtcSetH264Packetizer(tr: Int, init: rtcPacketizerInit?): Int {
    return setPacketizer(0, tr, init)
}

fun rtcSetH265Packetizer(tr: Int, init: rtcPacketizerInit?): Int {
    return setPacketizer(1, tr, init)
}

fun rtcSetAV1Packetizer(tr: Int, init: rtcPacketizerInit?): Int {
    return setPacketizer(2, tr, init)
}

fun rtcSetVP8Packetizer(tr: Int, init: rtcPacketizerInit?): Int {
    return setPacketizer(3, tr, init)
}

fun rtcSetVP9Packetizer(tr: Int, init: rtcPacketizerInit?): Int {
    return setPacketizer(4, tr, init)
}

fun rtcSetOpusPacketizer(tr: Int, init: rtcPacketizerInit?): Int {
    return setPacketizer(5, tr, init)
}

fun rtcSetAACPacketizer(tr: Int, init: rtcPacketizerInit?): Int {
    return setPacketizer(6, tr, init)
}

fun rtcSetPCMUPacketizer(tr: Int, init: rtcPacketizerInit?): Int {
    return setPacketizer(7, tr, init)
}

fun rtcSetPCMAPacketizer(tr: Int, init: rtcPacketizerInit?): Int {
    return setPacketizer(8, tr, init)
}

fun rtcSetG722Packetizer(tr: Int, init: rtcPacketizerInit?): Int {
    return setPacketizer(9, tr, init)
}

fun rtcSetH264Depacketizer(tr: Int, nalSeparator: rtcNalUnitSeparator): Int {
    if (!DataChannelNative.isLoaded) {
        return RTC_ERR_FAILURE
    }
    return DataChannelNative.rtcSetH264Depacketizer(tr, nalSeparator)
}

fun rtcSetH265Depacketizer(tr: Int, nalSeparator: rtcNalUnitSeparator): Int {
    if (!DataChannelNative.isLoaded) {
        return RTC_ERR_FAILURE
    }
    return DataChannelNative.rtcSetH265Depacketizer(tr, nalSeparator)
}

fun rtcSetAV1Depacketizer(tr: Int, obuPacketization: rtcObuPacketization): Int {
    if (!DataChannelNative.isLoaded) {
        return RTC_ERR_FAILURE
    }
    return DataChannelNative.rtcSetAV1Depacketizer(tr, obuPacketization)
}

fun rtcSetVP8Depacketizer(tr: Int): Int {
    if (!DataChannelNative.isLoaded) {
        return RTC_ERR_FAILURE
    }
    return DataChannelNative.rtcSetVP8Depacketizer(tr)
}

fun rtcSetVP9Depacketizer(tr: Int): Int {
    if (!DataChannelNative.isLoaded) {
        return RTC_ERR_FAILURE
    }
    return DataChannelNative.rtcSetVP9Depacketizer(tr)
}

fun rtcSetOpusDepacketizer(tr: Int): Int {
    if (!DataChannelNative.isLoaded) {
        return RTC_ERR_FAILURE
    }
    return DataChannelNative.rtcSetOpusDepacketizer(tr)
}

fun rtcSetAACDepacketizer(tr: Int): Int {
    if (!DataChannelNative.isLoaded) {
        return RTC_ERR_FAILURE
    }
    return DataChannelNative.rtcSetAACDepacketizer(tr)
}

fun rtcSetPCMUDepacketizer(tr: Int): Int {
    if (!DataChannelNative.isLoaded) {
        return RTC_ERR_FAILURE
    }
    return DataChannelNative.rtcSetPCMUDepacketizer(tr)
}

fun rtcSetPCMADepacketizer(tr: Int): Int {
    if (!DataChannelNative.isLoaded) {
        return RTC_ERR_FAILURE
    }
    return DataChannelNative.rtcSetPCMADepacketizer(tr)
}

fun rtcSetG722Depacketizer(tr: Int): Int {
    if (!DataChannelNative.isLoaded) {
        return RTC_ERR_FAILURE
    }
    return DataChannelNative.rtcSetG722Depacketizer(tr)
}

fun rtcChainRtcpReceivingSession(tr: Int): Int {
    if (!DataChannelNative.isLoaded) {
        return RTC_ERR_FAILURE
    }
    return DataChannelNative.rtcChainRtcpReceivingSession(tr)
}

fun rtcChainRtcpSrReporter(tr: Int): Int {
    if (!DataChannelNative.isLoaded) {
        return RTC_ERR_FAILURE
    }
    return DataChannelNative.rtcChainRtcpSrReporter(tr)
}

fun rtcChainRtcpNackResponder(tr: Int, maxStoredPacketsCount: UInt): Int {
    if (!DataChannelNative.isLoaded) {
        return RTC_ERR_FAILURE
    }
    return DataChannelNative.rtcChainRtcpNackResponder(tr, maxStoredPacketsCount.toInt())
}

fun rtcChainPliHandler(tr: Int, cb: rtcPliHandlerCallbackFunc): Int {
    return DataChannelCallbacks.set(DataChannelCallbacks.pli, tr, cb) {
        DataChannelNative.rtcChainPliHandler(tr)
    }
}

fun rtcChainRembHandler(tr: Int, cb: rtcRembHandlerCallbackFunc): Int {
    return DataChannelCallbacks.set(DataChannelCallbacks.remb, tr, cb) {
        DataChannelNative.rtcChainRembHandler(tr)
    }
}

fun rtcChainPacingHandler(tr: Int, bitsPerSecond: Double, sendIntervalMs: Int): Int {
    if (!DataChannelNative.isLoaded) {
        return RTC_ERR_FAILURE
    }
    return DataChannelNative.rtcChainPacingHandler(tr, bitsPerSecond, sendIntervalMs)
}

fun rtcTransformSecondsToTimestamp(id: Int, seconds: Double, timestamp: IntArray): Int {
    if (!DataChannelNative.isLoaded) {
        return RTC_ERR_FAILURE
    }
    return DataChannelNative.rtcTransformSecondsToTimestamp(id, seconds, timestamp)
}

fun rtcTransformTimestampToSeconds(id: Int, timestamp: UInt, seconds: DoubleArray): Int {
    if (!DataChannelNative.isLoaded) {
        return RTC_ERR_FAILURE
    }
    return DataChannelNative.rtcTransformTimestampToSeconds(id, timestamp.toInt(), seconds)
}

fun rtcGetCurrentTrackTimestamp(id: Int, timestamp: IntArray): Int {
    if (!DataChannelNative.isLoaded) {
        return RTC_ERR_FAILURE
    }
    return DataChannelNative.rtcGetCurrentTrackTimestamp(id, timestamp)
}

fun rtcSetTrackRtpTimestamp(id: Int, timestamp: UInt): Int {
    if (!DataChannelNative.isLoaded) {
        return RTC_ERR_FAILURE
    }
    return DataChannelNative.rtcSetTrackRtpTimestamp(id, timestamp.toInt())
}

fun rtcGetLastTrackSenderReportTimestamp(id: Int, timestamp: IntArray): Int {
    if (!DataChannelNative.isLoaded) {
        return RTC_ERR_FAILURE
    }
    return DataChannelNative.rtcGetLastTrackSenderReportTimestamp(id, timestamp)
}

fun rtcGetTrackRtcpSyncTimestamps(tr: Int, rtpTimestamp: LongArray, ntpTimestamp: LongArray): Int {
    if (!DataChannelNative.isLoaded) {
        return RTC_ERR_FAILURE
    }
    return DataChannelNative.rtcGetTrackRtcpSyncTimestamps(tr, rtpTimestamp, ntpTimestamp)
}

fun rtcSetThreadPoolSize(count: UInt): Int {
    if (!DataChannelNative.isLoaded) {
        return RTC_ERR_FAILURE
    }
    return DataChannelNative.rtcSetThreadPoolSize(count.toInt())
}

fun rtcPreload(): Boolean {
    return DataChannelNative.isLoaded && DataChannelNative.rtcPreload()
}

fun rtcCleanup() {
    if (DataChannelNative.isLoaded) {
        DataChannelNative.rtcCleanup()
    }
}

private fun setPacketizer(kind: Int, tr: Int, init: rtcPacketizerInit?): Int {
    if (!DataChannelNative.isLoaded) {
        return RTC_ERR_FAILURE
    }
    if (init == null) {
        return DataChannelNative.rtcSetPacketizer(kind, tr, null, null)
    }
    val fields = intArrayOf(
        init.ssrc.toInt(),
        init.payloadType.toInt(),
        init.clockRate.toInt(),
        init.sequenceNumber.toInt(),
        init.timestamp.toInt(),
        init.maxFragmentSize.toInt(),
        init.nalSeparator,
        init.obuPacketization,
        init.playoutDelayId.toInt(),
        init.playoutDelayMin.toInt(),
        init.playoutDelayMax.toInt(),
        init.colorSpaceId.toInt(),
        init.colorChromaSitingHorz.toInt(),
        init.colorChromaSitingVert.toInt(),
        init.colorRange.toInt(),
        init.colorPrimaries.toInt(),
        init.colorTransfer.toInt(),
        init.colorMatrix.toInt(),
    )
    return DataChannelNative.rtcSetPacketizer(kind, tr, fields, cStringOf(init.cname))
}

private fun cStringOf(value: String?): ByteArray? {
    return value?.toByteArray(Charsets.UTF_8)
}

private fun stringOf(value: ByteArray?): String? {
    return value?.toString(Charsets.UTF_8)
}

internal object DataChannelUserPointers {
    private val next = AtomicLong(1)
    private val pointers = ConcurrentHashMap<Long, WeakReference<Any>>()

    fun register(ptr: Any?): Long {
        if (ptr == null) {
            return 0
        }
        pointers.entries.removeIf { it.value.get() == null }
        for ((handle, reference) in pointers) {
            if (reference.get() === ptr) {
                return handle
            }
        }
        val handle = next.getAndIncrement()
        pointers[handle] = WeakReference(ptr)
        return handle
    }

    fun get(handle: Long): Any? {
        if (handle == 0L) {
            return null
        }
        return pointers[handle]?.get()
    }
}

internal object DataChannelCallbacks {
    @Volatile
    var log: rtcLogCallbackFunc? = null
    val stateChange = ConcurrentHashMap<Int, rtcStateChangeCallbackFunc>()
    val gatheringStateChange = ConcurrentHashMap<Int, rtcGatheringStateCallbackFunc>()
    val track = ConcurrentHashMap<Int, rtcTrackCallbackFunc>()
    val open = ConcurrentHashMap<Int, rtcOpenCallbackFunc>()
    val closed = ConcurrentHashMap<Int, rtcClosedCallbackFunc>()
    val error = ConcurrentHashMap<Int, rtcErrorCallbackFunc>()
    val frame = ConcurrentHashMap<Int, rtcFrameCallbackFunc>()
    val pli = ConcurrentHashMap<Int, rtcPliHandlerCallbackFunc>()
    val remb = ConcurrentHashMap<Int, rtcRembHandlerCallbackFunc>()
    private val tracks = ConcurrentHashMap<Int, MutableSet<Int>>()

    fun <T : Any> set(callbacks: ConcurrentHashMap<Int, T>, id: Int, cb: T?, native: (Boolean) -> Int): Int {
        if (!DataChannelNative.isLoaded) {
            return RTC_ERR_FAILURE
        }
        if (cb != null) {
            val previous = callbacks.put(id, cb)
            val result = native(true)
            if (result < 0) {
                if (previous != null) {
                    callbacks[id] = previous
                } else {
                    callbacks.remove(id, cb)
                }
            }
            return result
        } else {
            val result = native(false)
            if (result >= 0) {
                callbacks.remove(id)
            }
            return result
        }
    }

    fun addTrack(pc: Int, tr: Int) {
        if (tr < 0) {
            return
        }
        tracks.getOrPut(pc) { ConcurrentHashMap.newKeySet() }.add(tr)
    }

    fun removePeerConnection(pc: Int) {
        tracks.remove(pc)?.forEach { removeId(it) }
        removeId(pc)
    }

    fun removeId(id: Int) {
        stateChange.remove(id)
        gatheringStateChange.remove(id)
        track.remove(id)
        open.remove(id)
        closed.remove(id)
        error.remove(id)
        frame.remove(id)
        pli.remove(id)
        remb.remove(id)
    }

    fun onLog(level: Int, message: ByteArray?) {
        guarded("log") {
            log?.invoke(level, stringOf(message))
        }
    }

    fun onStateChange(pc: Int, state: Int, handle: Long) {
        guarded("state change") {
            stateChange[pc]?.invoke(pc, state, DataChannelUserPointers.get(handle))
        }
    }

    fun onGatheringStateChange(pc: Int, state: Int, handle: Long) {
        guarded("gathering state change") {
            gatheringStateChange[pc]?.invoke(pc, state, DataChannelUserPointers.get(handle))
        }
    }

    fun onTrack(pc: Int, tr: Int, handle: Long) {
        addTrack(pc, tr)
        guarded("track") {
            track[pc]?.invoke(pc, tr, DataChannelUserPointers.get(handle))
        }
    }

    fun onOpen(id: Int, handle: Long) {
        guarded("open") {
            open[id]?.invoke(id, DataChannelUserPointers.get(handle))
        }
    }

    fun onClosed(id: Int, handle: Long) {
        guarded("closed") {
            closed[id]?.invoke(id, DataChannelUserPointers.get(handle))
        }
    }

    fun onError(id: Int, message: ByteArray?, handle: Long) {
        guarded("error") {
            error[id]?.invoke(id, stringOf(message), DataChannelUserPointers.get(handle))
        }
    }

    fun onFrame(
        tr: Int,
        data: ByteArray?,
        size: Int,
        hasInfo: Boolean,
        timestamp: Int,
        payloadType: Int,
        timestampSeconds: Double,
        handle: Long,
    ) {
        guarded("frame") {
            val info = if (hasInfo) {
                rtcFrameInfo(
                    timestamp = timestamp.toUInt(),
                    payloadType = payloadType.toUByte(),
                    timestampSeconds = timestampSeconds,
                )
            } else {
                null
            }
            frame[tr]?.invoke(tr, data, size, info, DataChannelUserPointers.get(handle))
        }
    }

    fun onPli(tr: Int, handle: Long) {
        guarded("pli") {
            pli[tr]?.invoke(tr, DataChannelUserPointers.get(handle))
        }
    }

    fun onRemb(tr: Int, bitrate: Int, handle: Long) {
        guarded("remb") {
            remb[tr]?.invoke(tr, bitrate.toUInt(), DataChannelUserPointers.get(handle))
        }
    }

    private inline fun guarded(name: String, block: () -> Unit) {
        try {
            block()
        } catch (error: Throwable) {
            Log.e(TAG, "Uncaught failure in $name callback", error)
        }
    }
}

package com.moblin.android.platform.datachannel

import android.util.Log

private const val TAG = "MoblinDataChannel"

object DataChannelNative {
    val isLoaded: Boolean = try {
        System.loadLibrary("moblin_datachannel")
        Log.i(TAG, "libmoblin_datachannel loaded")
        true
    } catch (error: Throwable) {
        Log.e(TAG, "libmoblin_datachannel failed to load: $error")
        false
    }

    @JvmStatic
    external fun rtcInitLogger(level: Int, enabled: Boolean)

    @JvmStatic
    external fun rtcSetUserPointer(id: Int, handle: Long)

    @JvmStatic
    external fun rtcGetUserPointer(id: Int): Long

    @JvmStatic
    external fun rtcCreatePeerConnection(
        iceServers: Array<ByteArray?>?,
        iceServersCount: Int,
        proxyServer: ByteArray?,
        bindAddress: ByteArray?,
        certificatePemFile: ByteArray?,
        keyPemFile: ByteArray?,
        keyPemPass: ByteArray?,
        fields: IntArray,
    ): Int

    @JvmStatic
    external fun rtcClosePeerConnection(pc: Int): Int

    @JvmStatic
    external fun rtcDeletePeerConnection(pc: Int): Int

    @JvmStatic
    external fun rtcSetStateChangeCallback(pc: Int, enabled: Boolean): Int

    @JvmStatic
    external fun rtcSetGatheringStateChangeCallback(pc: Int, enabled: Boolean): Int

    @JvmStatic
    external fun rtcSetLocalDescription(pc: Int, type: ByteArray?): Int

    @JvmStatic
    external fun rtcSetRemoteDescription(pc: Int, sdp: ByteArray?, type: ByteArray?): Int

    @JvmStatic
    external fun rtcAddRemoteCandidate(pc: Int, cand: ByteArray?, mid: ByteArray?): Int

    @JvmStatic
    external fun rtcGetLocalDescription(pc: Int, buffer: ByteArray?, size: Int): Int

    @JvmStatic
    external fun rtcGetRemoteDescription(pc: Int, buffer: ByteArray?, size: Int): Int

    @JvmStatic
    external fun rtcGetLocalDescriptionType(pc: Int, buffer: ByteArray?, size: Int): Int

    @JvmStatic
    external fun rtcGetRemoteDescriptionType(pc: Int, buffer: ByteArray?, size: Int): Int

    @JvmStatic
    external fun rtcGetLocalAddress(pc: Int, buffer: ByteArray?, size: Int): Int

    @JvmStatic
    external fun rtcGetRemoteAddress(pc: Int, buffer: ByteArray?, size: Int): Int

    @JvmStatic
    external fun rtcGetSelectedCandidatePair(
        pc: Int,
        local: ByteArray?,
        localSize: Int,
        remote: ByteArray?,
        remoteSize: Int,
    ): Int

    @JvmStatic
    external fun rtcSetOpenCallback(id: Int, enabled: Boolean): Int

    @JvmStatic
    external fun rtcSetClosedCallback(id: Int, enabled: Boolean): Int

    @JvmStatic
    external fun rtcSetErrorCallback(id: Int, enabled: Boolean): Int

    @JvmStatic
    external fun rtcSendMessage(id: Int, data: ByteArray?, size: Int): Int

    @JvmStatic
    external fun rtcClose(id: Int): Int

    @JvmStatic
    external fun rtcDelete(id: Int): Int

    @JvmStatic
    external fun rtcIsOpen(id: Int): Boolean

    @JvmStatic
    external fun rtcIsClosed(id: Int): Boolean

    @JvmStatic
    external fun rtcSetTrackCallback(pc: Int, enabled: Boolean): Int

    @JvmStatic
    external fun rtcAddTrack(pc: Int, mediaDescriptionSdp: ByteArray?): Int

    @JvmStatic
    external fun rtcAddTrackEx(
        pc: Int,
        direction: Int,
        codec: Int,
        payloadType: Int,
        ssrc: Int,
        mid: ByteArray?,
        name: ByteArray?,
        msid: ByteArray?,
        trackId: ByteArray?,
        profile: ByteArray?,
    ): Int

    @JvmStatic
    external fun rtcDeleteTrack(tr: Int): Int

    @JvmStatic
    external fun rtcGetTrackDescription(tr: Int, buffer: ByteArray?, size: Int): Int

    @JvmStatic
    external fun rtcGetTrackMid(tr: Int, buffer: ByteArray?, size: Int): Int

    @JvmStatic
    external fun rtcGetTrackDirection(tr: Int, direction: IntArray?): Int

    @JvmStatic
    external fun rtcRequestKeyframe(tr: Int): Int

    @JvmStatic
    external fun rtcRequestBitrate(tr: Int, bitrate: Int): Int

    @JvmStatic
    external fun rtcSetFrameCallback(tr: Int, enabled: Boolean): Int

    @JvmStatic
    external fun rtcSetPacketizer(kind: Int, tr: Int, fields: IntArray?, cname: ByteArray?): Int

    @JvmStatic
    external fun rtcSetH264Depacketizer(tr: Int, nalSeparator: Int): Int

    @JvmStatic
    external fun rtcSetH265Depacketizer(tr: Int, nalSeparator: Int): Int

    @JvmStatic
    external fun rtcSetAV1Depacketizer(tr: Int, obuPacketization: Int): Int

    @JvmStatic
    external fun rtcSetVP8Depacketizer(tr: Int): Int

    @JvmStatic
    external fun rtcSetVP9Depacketizer(tr: Int): Int

    @JvmStatic
    external fun rtcSetOpusDepacketizer(tr: Int): Int

    @JvmStatic
    external fun rtcSetAACDepacketizer(tr: Int): Int

    @JvmStatic
    external fun rtcSetPCMUDepacketizer(tr: Int): Int

    @JvmStatic
    external fun rtcSetPCMADepacketizer(tr: Int): Int

    @JvmStatic
    external fun rtcSetG722Depacketizer(tr: Int): Int

    @JvmStatic
    external fun rtcChainRtcpReceivingSession(tr: Int): Int

    @JvmStatic
    external fun rtcChainRtcpSrReporter(tr: Int): Int

    @JvmStatic
    external fun rtcChainRtcpNackResponder(tr: Int, maxStoredPacketsCount: Int): Int

    @JvmStatic
    external fun rtcChainPliHandler(tr: Int): Int

    @JvmStatic
    external fun rtcChainRembHandler(tr: Int): Int

    @JvmStatic
    external fun rtcChainPacingHandler(tr: Int, bitsPerSecond: Double, sendIntervalMs: Int): Int

    @JvmStatic
    external fun rtcTransformSecondsToTimestamp(id: Int, seconds: Double, timestamp: IntArray?): Int

    @JvmStatic
    external fun rtcTransformTimestampToSeconds(id: Int, timestamp: Int, seconds: DoubleArray?): Int

    @JvmStatic
    external fun rtcGetCurrentTrackTimestamp(id: Int, timestamp: IntArray?): Int

    @JvmStatic
    external fun rtcSetTrackRtpTimestamp(id: Int, timestamp: Int): Int

    @JvmStatic
    external fun rtcGetLastTrackSenderReportTimestamp(id: Int, timestamp: IntArray?): Int

    @JvmStatic
    external fun rtcGetTrackRtcpSyncTimestamps(tr: Int, rtpTimestamp: LongArray?, ntpTimestamp: LongArray?): Int

    @JvmStatic
    external fun rtcSetThreadPoolSize(count: Int): Int

    @JvmStatic
    external fun rtcPreload(): Boolean

    @JvmStatic
    external fun rtcCleanup()

    @JvmStatic
    fun onLog(level: Int, message: ByteArray?) {
        DataChannelCallbacks.onLog(level, message)
    }

    @JvmStatic
    fun onStateChange(pc: Int, state: Int, handle: Long) {
        DataChannelCallbacks.onStateChange(pc, state, handle)
    }

    @JvmStatic
    fun onGatheringStateChange(pc: Int, state: Int, handle: Long) {
        DataChannelCallbacks.onGatheringStateChange(pc, state, handle)
    }

    @JvmStatic
    fun onTrack(pc: Int, tr: Int, handle: Long) {
        DataChannelCallbacks.onTrack(pc, tr, handle)
    }

    @JvmStatic
    fun onOpen(id: Int, handle: Long) {
        DataChannelCallbacks.onOpen(id, handle)
    }

    @JvmStatic
    fun onClosed(id: Int, handle: Long) {
        DataChannelCallbacks.onClosed(id, handle)
    }

    @JvmStatic
    fun onError(id: Int, error: ByteArray?, handle: Long) {
        DataChannelCallbacks.onError(id, error, handle)
    }

    @JvmStatic
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
        DataChannelCallbacks.onFrame(tr, data, size, hasInfo, timestamp, payloadType, timestampSeconds, handle)
    }

    @JvmStatic
    fun onPli(tr: Int, handle: Long) {
        DataChannelCallbacks.onPli(tr, handle)
    }

    @JvmStatic
    fun onRemb(tr: Int, bitrate: Int, handle: Long) {
        DataChannelCallbacks.onRemb(tr, bitrate, handle)
    }
}

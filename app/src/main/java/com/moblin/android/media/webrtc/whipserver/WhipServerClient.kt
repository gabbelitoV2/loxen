package com.moblin.android.media.webrtc.whipserver

import com.moblin.android.media.MediaSample
import com.moblin.android.media.webrtc.WebrtcIngestClient
import com.moblin.android.media.webrtc.WebrtcIngestClientDelegate
import com.moblin.android.various.settings.SettingsStreamColorRange
import java.util.UUID

interface WhipServerClientDelegate {
    fun whipServerClientOnConnected(streamId: UUID)
    fun whipServerClientOnDisconnected(streamId: UUID, reason: String)
    fun whipServerClientOnVideoBuffer(streamId: UUID, sampleBuffer: MediaSample)
    fun whipServerClientOnAudioBuffer(streamId: UUID, sampleBuffer: MediaSample)
    fun whipServerClientOnDataReceived(streamId: UUID, count: Int)
}

class WhipServerClient(
    val streamId: UUID,
    latency: Double,
    syncTimestamps: Boolean,
    softwareDecoding: Boolean,
    colorRange: SettingsStreamColorRange,
    iceServers: List<String>,
    val delegate: WhipServerClientDelegate?
) : WebrtcIngestClientDelegate {
    private var ingestClient: WebrtcIngestClient? = null
    private var answerCompletion: ((String?) -> Unit)? = null

    init {
        ingestClient = WebrtcIngestClient(
            name = "whip-server",
            streamId = streamId,
            latency = latency,
            syncTimestamps = syncTimestamps,
            softwareDecoding = softwareDecoding,
            colorRange = colorRange,
            iceServers = iceServers,
            dispatchQueue = whipServerDispatchQueue,
            delegate = this
        )
    }

    fun handleOffer(sdpOffer: String, completion: (String?) -> Unit) {
        val ingestClient = ingestClient
        if (ingestClient == null) {
            completion(null)
            return
        }
        try {
            ingestClient.createPeerConnection()
            ingestClient.setRemoteDescription(sdpOffer, "offer")
            answerCompletion = completion
        } catch (e: Exception) {
            completion(null)
            stop()
        }
    }

    fun stop() {
        ingestClient?.stop()
        ingestClient = null
        answerCompletion = null
    }

    override fun webrtcIngestClientOnConnected(streamId: UUID) {
        delegate?.whipServerClientOnConnected(streamId = streamId)
    }

    override fun webrtcIngestClientOnDisconnected(streamId: UUID, reason: String) {
        delegate?.whipServerClientOnDisconnected(streamId = streamId, reason = reason)
    }

    override fun webrtcIngestClientOnVideoBuffer(streamId: UUID, sampleBuffer: MediaSample) {
        delegate?.whipServerClientOnVideoBuffer(streamId = streamId, sampleBuffer = sampleBuffer)
    }

    override fun webrtcIngestClientOnAudioBuffer(streamId: UUID, sampleBuffer: MediaSample) {
        delegate?.whipServerClientOnAudioBuffer(streamId = streamId, sampleBuffer = sampleBuffer)
    }

    override fun webrtcIngestClientOnGatheringComplete(streamId: UUID, localDescription: String) {
        answerCompletion?.invoke(localDescription)
        answerCompletion = null
    }

    override fun webrtcIngestClientOnDataReceived(streamId: UUID, count: Int) {
        delegate?.whipServerClientOnDataReceived(streamId = streamId, count = count)
    }
}

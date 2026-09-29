package com.moblin.android.media.webrtc.whepclient

import android.util.Log
import com.moblin.android.media.MediaSample
import com.moblin.android.media.haishinkit.util.BitrateStats
import com.moblin.android.media.haishinkit.util.BitrateStatsInstant
import com.moblin.android.media.haishinkit.whip.h264PayloadType
import com.moblin.android.media.haishinkit.whip.opusPayloadType
import com.moblin.android.media.webrtc.WebrtcIngestClient
import com.moblin.android.media.webrtc.WebrtcIngestClientDelegate
import com.moblin.android.media.webrtc.defaultStunServer
import com.moblin.android.platform.datachannel.*
import com.moblin.android.various.SimpleTimer
import com.moblin.android.various.network.httpRequest
import com.moblin.android.various.settings.SettingsStreamColorRange
import java.net.URI
import java.util.UUID
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

private const val TAG = "WhepClient"

private val dispatchQueue: CoroutineDispatcher =
    Executors.newSingleThreadExecutor { Thread(it, "com.eerimoq.whep-client") }.asCoroutineDispatcher()

private val dispatchQueueScope = CoroutineScope(dispatchQueue)

private const val reconnectDelay = 5.0

interface WhepClientDelegate {
    fun whepClientOnPublishStart(streamId: UUID)

    fun whepClientOnPublishStop(streamId: UUID, reason: String)

    fun whepClientOnVideoBuffer(streamId: UUID, sampleBuffer: MediaSample)

    fun whepClientOnAudioBuffer(streamId: UUID, sampleBuffer: MediaSample)
}

class WhepClient(
    val streamId: UUID,
    private val url: String,
    private val latency: Double,
    private val syncTimestamps: Boolean,
    private val softwareDecoding: Boolean,
    private val colorRange: SettingsStreamColorRange,
    private val delegate: WhepClientDelegate,
) : WebrtcIngestClientDelegate {
    private var ingestClient: WebrtcIngestClient? = null
    private var sessionUrl: String? = null
    private var started = false
    private var reconnectTimer = SimpleTimer(dispatchQueue)
    private var connected: Boolean = false
    private var bitrateStats = BitrateStats()

    fun start() {
        dispatchQueueScope.launch {
            Log.i(TAG, "whep-client: $streamId: Start")
            started = true
            startInternal()
        }
    }

    fun stop() {
        dispatchQueueScope.launch {
            Log.i(TAG, "whep-client: $streamId: Stop")
            started = false
            stopInternal()
        }
    }

    fun isConnected(): Boolean {
        return runBlocking(dispatchQueue) {
            connected
        }
    }

    fun updateStats(): BitrateStatsInstant {
        return runBlocking(dispatchQueue) {
            bitrateStats.update()
        }
    }

    private fun startInternal() {
        if (!started) {
            return
        }
        stopInternal()
        ingestClient = WebrtcIngestClient(
            name = "whep-client",
            streamId = streamId,
            latency = latency,
            syncTimestamps = syncTimestamps,
            softwareDecoding = softwareDecoding,
            colorRange = colorRange,
            iceServers = listOf(defaultStunServer),
            dispatchQueue = dispatchQueue,
            delegate = this,
        )
        val ingestClient = ingestClient ?: return
        try {
            val msid = UUID.randomUUID().toString().uppercase()
            ingestClient.createPeerConnection()
            val videoTrackId = ingestClient.addRecvOnlyTrack(
                codec = RTC_CODEC_H264,
                payloadType = h264PayloadType.toInt(),
                mid = "0",
                msid = msid,
                name = "video",
                profile = "",
            )
            ingestClient.setTrackCodec(trackId = videoTrackId, description = "h264")
            val audioTrackId = ingestClient.addRecvOnlyTrack(
                codec = RTC_CODEC_OPUS,
                payloadType = opusPayloadType.toInt(),
                mid = "1",
                msid = msid,
                name = "audio",
                profile = "",
            )
            ingestClient.setTrackCodec(trackId = audioTrackId, description = "opus")
            ingestClient.setLocalDescription("offer")
        } catch (error: Exception) {
            Log.i(TAG, "whep-client: $streamId: Failed to create offer: $error")
            reconnectSoon(reason = "Failed to create offer")
        }
    }

    private fun stopInternal() {
        reconnectTimer.stop()
        val sessionUrl = sessionUrl
        if (sessionUrl != null) {
            sendDeleteRequest(url = sessionUrl)
        }
        this.sessionUrl = null
        ingestClient?.stop()
        ingestClient = null
        connected = false
    }

    private fun reconnectSoon(reason: String) {
        stopInternal()
        Log.d(TAG, "whep-client: $streamId: Reconnecting in $reconnectDelay seconds ($reason)")
        reconnectTimer.startSingleShot(timeout = reconnectDelay) {
            startInternal()
        }
    }

    private fun sendOffer(offer: String) {
        Log.d(TAG, "whep-client: $streamId: Sending offer to $url")
        val request = try {
            Request.Builder()
                .url(url)
                .header("Content-Type", "application/sdp")
                .post(offer.toByteArray(Charsets.UTF_8).toRequestBody("application/sdp".toMediaType()))
                .build()
        } catch (error: IllegalArgumentException) {
            dispatchQueueScope.launch {
                handleOfferResponse(data = null, response = null, error = error)
            }
            return
        }
        httpRequest(request = request, queue = dispatchQueue) { data, response, error ->
            handleOfferResponse(data = data, response = response, error = error)
        }
    }

    private fun handleOfferResponse(data: ByteArray?, response: Response?, error: Throwable?) {
        if (error != null || response == null || !response.isSuccessful || data == null) {
            Log.i(TAG, "whep-client: $streamId: HTTP response not ok")
            reconnectSoon(reason = "Bad HTTP response")
            return
        }
        val answer = data.toString(Charsets.UTF_8)
        val locationHeader = response.header("Location")
        if (locationHeader != null) {
            sessionUrl = try {
                URI(url).resolve(locationHeader).toString()
            } catch (error: Exception) {
                null
            }
        }
        Log.d(TAG, "whep-client: $streamId: Got answer $answer")
        try {
            ingestClient?.setRemoteDescription(answer, type = "answer")
        } catch (error: Exception) {
            Log.i(TAG, "whep-client: $streamId: Failed to set remote answer: $error")
            reconnectSoon(reason = "Failed to set remote answer")
        }
    }

    private fun sendDeleteRequest(url: String) {
        val request = try {
            Request.Builder()
                .url(url)
                .delete()
                .build()
        } catch (error: IllegalArgumentException) {
            return
        }
        httpRequest(request = request)
    }

    override fun webrtcIngestClientOnConnected(streamId: UUID) {
        connected = true
        delegate.whepClientOnPublishStart(streamId = streamId)
    }

    override fun webrtcIngestClientOnDisconnected(streamId: UUID, reason: String) {
        if (connected) {
            delegate.whepClientOnPublishStop(streamId = streamId, reason = reason)
            connected = false
        }
        reconnectSoon(reason = reason)
    }

    override fun webrtcIngestClientOnVideoBuffer(streamId: UUID, sampleBuffer: MediaSample) {
        delegate.whepClientOnVideoBuffer(streamId = streamId, sampleBuffer = sampleBuffer)
    }

    override fun webrtcIngestClientOnAudioBuffer(streamId: UUID, sampleBuffer: MediaSample) {
        delegate.whepClientOnAudioBuffer(streamId = streamId, sampleBuffer = sampleBuffer)
    }

    override fun webrtcIngestClientOnGatheringComplete(streamId: UUID, localDescription: String) {
        sendOffer(offer = localDescription)
    }

    override fun webrtcIngestClientOnDataReceived(streamId: UUID, count: Int) {
        bitrateStats.add(bytesTransferred = count)
    }
}

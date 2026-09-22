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
import com.moblin.android.various.SimpleTimer
import java.io.IOException
import java.net.URI
import java.util.UUID
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

private const val tag = "WhepClient"

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
    private val delegate: WhepClientDelegate,
) : WebrtcIngestClientDelegate {
    private var ingestClient: WebrtcIngestClient? = null
    private var sessionUrl: String? = null
    private var started = false
    private val reconnectTimer = SimpleTimer(dispatchQueue)
    private var connected: Boolean = false
    private var bitrateStats = BitrateStats()
    private val httpClient = OkHttpClient()

    fun start() {
        dispatchQueueScope.launch {
            Log.i(tag, "whep-client: $streamId: Start")
            started = true
            startInternal()
        }
    }

    fun stop() {
        dispatchQueueScope.launch {
            Log.i(tag, "whep-client: $streamId: Stop")
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
            iceServers = listOf(defaultStunServer),
            dispatchQueue = dispatchQueue,
            delegate = this,
        )
        val client = ingestClient ?: return
        try {
            val msid = UUID.randomUUID().toString()
            client.createPeerConnection()
            val videoTrackId = client.addRecvOnlyTrack(
                codec = TODO("libdatachannel RTC_CODEC_H264 has no Kotlin declaration"),
                payloadType = h264PayloadType.toInt(),
                mid = "0",
                msid = msid,
                name = "video",
                profile = "",
            )
            client.setTrackCodec(trackId = videoTrackId, description = "h264")
            val audioTrackId = client.addRecvOnlyTrack(
                codec = TODO("libdatachannel RTC_CODEC_OPUS has no Kotlin declaration"),
                payloadType = opusPayloadType.toInt(),
                mid = "1",
                msid = msid,
                name = "audio",
                profile = "",
            )
            client.setTrackCodec(trackId = audioTrackId, description = "opus")
            client.setLocalDescription("offer")
        } catch (e: Exception) {
            Log.i(tag, "whep-client: $streamId: Failed to create offer: $e")
            reconnectSoon(reason = "Failed to create offer")
        }
    }

    private fun stopInternal() {
        reconnectTimer.stop()
        sessionUrl?.let { sendDeleteRequest(url = it) }
        sessionUrl = null
        ingestClient?.stop()
        ingestClient = null
        connected = false
    }

    private fun reconnectSoon(reason: String) {
        stopInternal()
        Log.d(tag, "whep-client: $streamId: Reconnecting in $reconnectDelay seconds ($reason)")
        reconnectTimer.startSingleShot(reconnectDelay) {
            startInternal()
        }
    }

    private fun sendOffer(offer: String) {
        Log.d(tag, "whep-client: $streamId: Sending offer to $url")
        val request = Request.Builder()
            .url(url)
            .header("Content-Type", "application/sdp")
            .post(offer.toRequestBody("application/sdp".toMediaType()))
            .build()
        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                dispatchQueueScope.launch {
                    handleOfferResponse(data = null, code = null, locationHeader = null, error = e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val data = response.body?.string()
                val code = response.code
                val locationHeader = response.header("Location")
                response.close()
                dispatchQueueScope.launch {
                    handleOfferResponse(
                        data = data,
                        code = code,
                        locationHeader = locationHeader,
                        error = null,
                    )
                }
            }
        })
    }

    private fun handleOfferResponse(
        data: String?,
        code: Int?,
        locationHeader: String?,
        error: Throwable?,
    ) {
        if (error != null || code == null || code !in 200..299 || data == null) {
            Log.i(tag, "whep-client: $streamId: HTTP response not ok")
            reconnectSoon(reason = "Bad HTTP response")
            return
        }
        if (locationHeader != null) {
            sessionUrl = runCatching {
                URI(url).resolve(locationHeader).toString()
            }.getOrNull()
        }
        Log.d(tag, "whep-client: $streamId: Got answer $data")
        try {
            ingestClient?.setRemoteDescription(data, type = "answer")
        } catch (e: Exception) {
            Log.i(tag, "whep-client: $streamId: Failed to set remote answer: $e")
            reconnectSoon(reason = "Failed to set remote answer")
        }
    }

    private fun sendDeleteRequest(url: String) {
        val request = Request.Builder()
            .url(url)
            .delete()
            .build()
        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}

            override fun onResponse(call: Call, response: Response) {
                response.close()
            }
        })
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

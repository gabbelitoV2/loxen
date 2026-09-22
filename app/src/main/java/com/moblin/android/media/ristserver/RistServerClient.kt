package com.moblin.android.media.ristserver

import android.util.Log
import com.moblin.android.media.MediaSample
import com.moblin.android.media.haishinkit.mpeg.MpegTsReader
import com.moblin.android.media.haishinkit.mpeg.MpegTsReaderDelegate
import java.util.UUID
import kotlinx.coroutines.ContinuationInterceptor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

private const val TAG = "RistServerClient"

private val ristServerDispatcher: CoroutineDispatcher =
    ristServerQueue.coroutineContext[ContinuationInterceptor] as? CoroutineDispatcher
        ?: Dispatchers.Default

class RistServerClient(
    private val cameraId: UUID,
    latency: Double,
    softwareDecoding: Boolean,
) : MpegTsReaderDelegate {
    var server: RistServer? = null
    private val reader: MpegTsReader = MpegTsReader(
        name = "rist-server",
        decoderQueue = ristServerDispatcher,
        timecodesEnabled = false,
        softwareDecoding = softwareDecoding,
        targetLatency = latency,
    )

    init {
        reader.delegate = this
    }

    fun handlePacketFromClient(packet: ByteArray) {
        runCatching {
            reader.handlePacketFromClient(packet)
        }.onFailure { error ->
            Log.i(TAG, "rist-server-client: Got corrupt packet $error.")
        }
    }

    override fun mpegTsReaderAudioBuffer(sampleBuffer: MediaSample) {
        server?.delegate?.ristServerOnAudioBuffer(cameraId = cameraId, sampleBuffer = sampleBuffer)
    }

    override fun mpegTsReaderVideoBuffer(sampleBuffer: MediaSample) {
        server?.delegate?.ristServerOnVideoBuffer(cameraId = cameraId, sampleBuffer = sampleBuffer)
    }
}

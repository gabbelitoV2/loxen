package com.moblin.android.media.srtla.server

import android.util.Log
import com.moblin.android.media.MediaSample
import com.moblin.android.media.haishinkit.mpeg.MpegTsReader
import com.moblin.android.media.haishinkit.mpeg.MpegTsReaderDelegate
import com.moblin.android.platform.srt.SrtNative
import java.lang.ref.WeakReference
import java.util.UUID
import kotlin.coroutines.ContinuationInterceptor
import kotlinx.coroutines.CoroutineDispatcher

private const val TAG = "SrtServerClient"

val srtServerClientLatency = 0.5

open class SrtServerClient(
    server: SrtServer,
    private val cameraId: UUID,
    timecodesEnabled: Boolean,
    softwareDecoding: Boolean,
) : MpegTsReaderDelegate {
    private val server: WeakReference<SrtServer> = WeakReference(server)
    private val reader: MpegTsReader = MpegTsReader(
        name = "srt-server",
        decoderQueue = (srtlaServerQueue.coroutineContext[ContinuationInterceptor] as CoroutineDispatcher)
            .limitedParallelism(1),
        timecodesEnabled = timecodesEnabled,
        softwareDecoding = softwareDecoding,
        targetLatency = srtServerClientLatency,
    )

    init {
        reader.delegate = this
    }

    open fun run(clientSocket: Int) {
        val packetSize = 2048
        val packet = ByteArray(packetSize)
        while (server.get()?.running == true) {
            val count = SrtNative.srt_recvmsg(clientSocket, packet, packetSize)
            if (count == SrtNative.SRT_ERROR) {
                break
            }
            server.get()?.srtlaServer?.bitrateStats?.mutate {
                it.value.add(bytesTransferred = count)
            }
            try {
                reader.handlePacketFromClient(packet = packet.copyOf(count))
            } catch (error: Exception) {
                Log.i(TAG, "srt-server-client: Got corrupt packet $error.")
            }
        }
        SrtNative.srt_close(clientSocket)
        reader.stop()
    }

    override fun mpegTsReaderAudioBuffer(sampleBuffer: MediaSample) {
        server.get()?.srtlaServer?.delegate?.srtlaServerOnAudioBuffer(
            cameraId = cameraId,
            sampleBuffer = sampleBuffer,
        )
    }

    override fun mpegTsReaderVideoBuffer(sampleBuffer: MediaSample) {
        server.get()?.srtlaServer?.delegate?.srtlaServerOnVideoBuffer(
            cameraId = cameraId,
            sampleBuffer = sampleBuffer,
        )
    }
}

package com.moblin.android.media.srtla.server

import android.util.Log
import com.moblin.android.media.BitrateStats
import com.moblin.android.media.MediaSample
import com.moblin.android.media.haishinkit.mpeg.MpegTsReader
import com.moblin.android.media.haishinkit.mpeg.MpegTsReaderDelegate
import java.lang.ref.WeakReference
import java.util.UUID
import kotlinx.coroutines.Dispatchers

val srtServerClientLatency = 0.5

class SrtServerClient(
    server: SrtServer,
    private val cameraId: UUID,
    timecodesEnabled: Boolean,
    softwareDecoding: Boolean,
) : MpegTsReaderDelegate {
    private val server: WeakReference<SrtServer> = WeakReference(server)
    private val reader: MpegTsReader = MpegTsReader(
        name = "srt-server",
        decoderQueue = Dispatchers.IO,
        timecodesEnabled = timecodesEnabled,
        softwareDecoding = softwareDecoding,
        targetLatency = srtServerClientLatency,
    )

    init {
        reader.delegate = this
    }

    fun run(clientSocket: Int) {
        val packetSize = 2048
        val packet = ByteArray(packetSize)
        while (server.get()?.running == true) {
            val count = SrtNative.srt_recvmsg(clientSocket, packet, packetSize)
            if (count == SrtNative.SRT_ERROR) {
                break
            }
            val payload = if (count == packetSize) packet else packet.copyOf(count)
            server.get()?.srtlaServer?.bitrateStats?.mutate { it: BitrateStats ->
                it.add(bytesTransferred = payload.size)
            }
            try {
                reader.handlePacketFromClient(payload)
            } catch (e: Exception) {
                Log.i("SrtServerClient", "srt-server-client: Got corrupt packet $e.")
            }
        }
        SrtNative.srt_close(clientSocket)
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

object SrtNative {
    const val SRT_ERROR: Int = -1

    external fun srt_recvmsg(socket: Int, buf: ByteArray, len: Int): Int

    external fun srt_close(socket: Int): Int
}

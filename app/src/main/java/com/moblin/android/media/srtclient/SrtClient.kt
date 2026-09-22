package com.moblin.android.media.srtclient

import android.system.OsConstants
import android.util.Log
import com.moblin.android.media.MediaSample
import com.moblin.android.media.haishinkit.mpeg.MpegTsReader
import com.moblin.android.media.haishinkit.mpeg.MpegTsReaderDelegate
import com.moblin.android.media.haishinkit.srt.SrtSocketOption
import com.moblin.android.media.haishinkit.util.Atomic
import com.moblin.android.media.haishinkit.util.BitrateStats
import com.moblin.android.media.haishinkit.util.BitrateStatsInstant
import com.moblin.android.various.SimpleTimer
import com.moblin.android.various.utils.startBlockingThread
import java.net.InetAddress
import java.net.URI
import java.util.UUID
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch

private val srtClientExecutor = Executors.newSingleThreadExecutor { runnable ->
    Thread(runnable, "com.moblin.android.srt-client")
}

private val srtClientQueue: CoroutineDispatcher = srtClientExecutor.asCoroutineDispatcher()

private val srtClientScope = CoroutineScope(srtClientQueue)

interface SrtClientDelegate {
    fun srtClientConnected(cameraId: UUID)

    fun srtClientDisconnected(cameraId: UUID)

    fun srtClientOnVideoBuffer(cameraId: UUID, sampleBuffer: MediaSample)

    fun srtClientOnAudioBuffer(cameraId: UUID, sampleBuffer: MediaSample)
}

val srtClientLatency = 0.5

private const val reconnectDelay = 5.0

class SrtClient(
    cameraId: UUID,
    url: URI,
    softwareDecoding: Boolean,
    delegate: SrtClientDelegate?,
) : MpegTsReaderDelegate {
    private val cameraId: UUID = cameraId
    private val url: URI = url
    private val delegate: SrtClientDelegate? = delegate
    private var running = false
    private var socket: Int = SrtNative.SRT_INVALID_SOCK
    private var bitrateStats: Atomic<BitrateStats> = Atomic(BitrateStats())
    private val reconnectTimer = SimpleTimer(srtClientQueue)
    private var reader: MpegTsReader
    private val softwareDecoding: Boolean = softwareDecoding

    init {
        reader = MpegTsReader(
            name = "srt-client",
            decoderQueue = srtClientQueue,
            timecodesEnabled = false,
            softwareDecoding = softwareDecoding,
            targetLatency = srtClientLatency,
        )
        reader.delegate = this
    }

    fun start() {
        srtClientScope.launch {
            SrtNative.srt_startup()
            running = true
            connectSoon(0.0)
        }
    }

    fun stop() {
        srtClientScope.launch {
            running = false
            reconnectTimer.stop()
            closeSocket()
            SrtNative.srt_cleanup()
        }
    }

    fun updateStats(): BitrateStatsInstant {
        return bitrateStats.mutate {
            it.update()
        }
    }

    private fun connectSoon(delay: Double) {
        closeSocket()
        if (!running) {
            return
        }
        reconnectTimer.startSingleShot(delay) {
            connectAsync()
        }
    }

    private fun connectAsync() {
        val socket = SrtNative.srt_create_socket()
        if (socket == SrtNative.SRT_INVALID_SOCK) {
            Log.i(tag, "srt-client: $cameraId: Failed to create socket: ${lastSrtError()}")
            srtClientScope.launch {
                connectSoon(reconnectDelay)
            }
            return
        }
        this.socket = socket
        startBlockingThread("com.eerimoq.moblin.srt-client-connection") {
            main(socket)
        }
    }

    private fun main(socket: Int) {
        val host = url.host
        val port = url.port
        if (host == null || port == -1) {
            Log.i(tag, "srt-client: $cameraId: Invalid URL $url.")
            srtClientScope.launch {
                connectSoon(reconnectDelay)
            }
            return
        }
        val options = SrtSocketOption.from(url)
        val failures = SrtSocketOption.configure(socket, SrtSocketOption.Binding.PRE, options)
        if (failures.isNotEmpty()) {
            Log.i(tag, "srt-client: $cameraId: Failed to set pre-bind options: $failures.")
        }
        val address = sockaddrIn(host, port)
        val addressSize = address.size
        val result = SrtNative.srt_connect(socket, address, addressSize)
        if (result == SrtNative.SRT_ERROR) {
            Log.d(tag, "srt-client: $cameraId: Connect failed: ${lastSrtError()}")
            srtClientScope.launch {
                connectSoon(reconnectDelay)
            }
            return
        }
        val postFailures = SrtSocketOption.configure(socket, SrtSocketOption.Binding.POST, options)
        if (postFailures.isNotEmpty()) {
            Log.i(tag, "srt-client: $cameraId: Failed to set post-bind options: $postFailures.")
        }
        reader = MpegTsReader(
            name = "srt-client",
            decoderQueue = srtClientQueue,
            timecodesEnabled = false,
            softwareDecoding = softwareDecoding,
            targetLatency = srtClientLatency,
        )
        reader.delegate = this
        delegate?.srtClientConnected(cameraId)
        receive(socket)
        delegate?.srtClientDisconnected(cameraId)
        srtClientScope.launch {
            connectSoon(reconnectDelay)
        }
    }

    private fun receive(socket: Int) {
        val packetSize = 2048
        val packet = ByteArray(packetSize)
        while (true) {
            val count = SrtNative.srt_recvmsg(socket, packet, packetSize)
            if (count == SrtNative.SRT_ERROR) {
                break
            }
            bitrateStats.mutate {
                it.add(bytesTransferred = count)
            }
            try {
                reader.handlePacketFromClient(packet.copyOf(count))
            } catch (e: Exception) {
                Log.i(tag, "srt-client: $cameraId: Got corrupt packet: $e.")
            }
        }
    }

    private fun closeSocket() {
        if (socket == SrtNative.SRT_INVALID_SOCK) {
            return
        }
        SrtNative.srt_close(socket)
        socket = SrtNative.SRT_INVALID_SOCK
    }

    override fun mpegTsReaderVideoBuffer(sampleBuffer: MediaSample) {
        delegate?.srtClientOnVideoBuffer(cameraId, sampleBuffer)
    }

    override fun mpegTsReaderAudioBuffer(sampleBuffer: MediaSample) {
        delegate?.srtClientOnAudioBuffer(cameraId, sampleBuffer)
    }
}

private fun sockaddrIn(host: String, port: Int): ByteArray {
    val address = ByteArray(16)
    address[0] = (OsConstants.AF_INET and 0xFF).toByte()
    address[1] = ((OsConstants.AF_INET shr 8) and 0xFF).toByte()
    address[2] = ((port shr 8) and 0xFF).toByte()
    address[3] = (port and 0xFF).toByte()
    val resolved = runCatching { InetAddress.getByName(host) }.getOrNull() ?: return address
    val raw = resolved.address
    if (raw.size == 4) {
        raw.copyInto(address, destinationOffset = 4)
    }
    return address
}

private fun lastSrtError(): String {
    return SrtNative.srt_getlasterror_str()
}

private const val tag = "SrtClient"

object SrtNative {
    const val SRT_INVALID_SOCK: Int = -1
    const val SRT_ERROR: Int = -1

    external fun srt_startup()

    external fun srt_cleanup()

    external fun srt_create_socket(): Int

    external fun srt_connect(socket: Int, address: ByteArray, addressSize: Int): Int

    external fun srt_recvmsg(socket: Int, buffer: ByteArray, length: Int): Int

    external fun srt_close(socket: Int)

    external fun srt_getlasterror_str(): String
}

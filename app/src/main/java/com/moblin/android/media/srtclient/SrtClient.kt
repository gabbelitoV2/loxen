package com.moblin.android.media.srtclient

import android.util.Log
import com.moblin.android.media.MediaSample
import com.moblin.android.media.haishinkit.mpeg.MpegTsReader
import com.moblin.android.media.haishinkit.mpeg.MpegTsReaderDelegate
import com.moblin.android.media.haishinkit.srt.SrtSocketOption
import com.moblin.android.media.haishinkit.util.Atomic
import com.moblin.android.media.haishinkit.util.BitrateStats
import com.moblin.android.media.haishinkit.util.BitrateStatsInstant
import com.moblin.android.platform.srt.SrtNative
import com.moblin.android.various.SimpleTimer
import com.moblin.android.various.settings.SettingsStreamColorRange
import com.moblin.android.various.utils.startBlockingThread
import java.net.URI
import java.util.UUID
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch

private const val TAG = "SrtClient"

private val srtClientQueue: CoroutineDispatcher = Executors.newSingleThreadExecutor { runnable ->
    Thread(runnable, "com.eerimoq.moblin.srt-client").apply { priority = Thread.MAX_PRIORITY }
}.asCoroutineDispatcher()

interface SrtClientDelegate {
    fun srtClientConnected(cameraId: UUID)

    fun srtClientDisconnected(cameraId: UUID)

    fun srtClientOnVideoBuffer(cameraId: UUID, sampleBuffer: MediaSample)

    fun srtClientOnAudioBuffer(cameraId: UUID, sampleBuffer: MediaSample)
}

val srtClientLatency = 0.5

private const val reconnectDelay = 5.0

open class SrtClient(
    private val cameraId: UUID,
    private val url: URI,
    private val softwareDecoding: Boolean,
    private val colorRange: SettingsStreamColorRange,
    delegate: SrtClientDelegate,
) : MpegTsReaderDelegate {
    private val delegate: SrtClientDelegate? = delegate
    private var running = false
    private var socket: Int = SrtNative.SRT_INVALID_SOCK
    private var bitrateStats: Atomic<BitrateStats> = Atomic(BitrateStats())
    private val reconnectTimer = SimpleTimer(queue = srtClientQueue)
    private var reader: MpegTsReader

    init {
        reader = MpegTsReader(
            name = "srt-client",
            decoderQueue = srtClientQueue,
            timecodesEnabled = false,
            softwareDecoding = softwareDecoding,
            colorRange = colorRange,
            targetLatency = srtClientLatency,
        )
        reader.delegate = this
    }

    open fun start() {
        CoroutineScope(srtClientQueue).launch {
            SrtNative.srt_startup()
            running = true
            connectSoon(delay = 0.0)
        }
    }

    open fun stop() {
        CoroutineScope(srtClientQueue).launch {
            running = false
            reconnectTimer.stop()
            if (socket == SrtNative.SRT_INVALID_SOCK) reader.stop()
            closeSocket()
            SrtNative.srt_cleanup()
        }
    }

    open fun updateStats(): BitrateStatsInstant {
        return bitrateStats.mutate { it.value.update() }
    }

    private fun connectSoon(delay: Double) {
        closeSocket()
        if (!running) {
            reader.stop()
            return
        }
        reconnectTimer.startSingleShot(timeout = delay) {
            connectAsync()
        }
    }

    private fun connectAsync() {
        val socket = SrtNative.srt_create_socket()
        if (socket == SrtNative.SRT_INVALID_SOCK) {
            Log.i(TAG, "srt-client: $cameraId: Failed to create socket: ${lastSrtError()}")
            CoroutineScope(srtClientQueue).launch {
                connectSoon(delay = reconnectDelay)
            }
            return
        }
        this.socket = socket
        startBlockingThread(name = "com.eerimoq.moblin.srt-client-connection") {
            main(socket = socket)
        }
    }

    private fun main(socket: Int) {
        val host = url.host
        val port = url.port
        if (host == null || port == -1) {
            Log.i(TAG, "srt-client: $cameraId: Invalid URL $url.")
            CoroutineScope(srtClientQueue).launch {
                connectSoon(delay = reconnectDelay)
            }
            return
        }
        val options = SrtSocketOption.from(uri = url)
        val failures = SrtSocketOption.configure(socket, binding = SrtSocketOption.Binding.pre, options = options)
        if (failures.isNotEmpty()) {
            Log.i(TAG, "srt-client: $cameraId: Failed to set pre-bind options: $failures.")
        }
        val address = sockaddrIn(host, port = port.coerceIn(0, 0xFFFF))
        val addressSize = address.size
        val result = SrtNative.srt_connect(socket, address, addressSize)
        if (result == SrtNative.SRT_ERROR) {
            Log.d(TAG, "srt-client: $cameraId: Connect failed: ${lastSrtError()}")
            CoroutineScope(srtClientQueue).launch {
                connectSoon(delay = reconnectDelay)
            }
            return
        }
        val postFailures = SrtSocketOption.configure(socket, binding = SrtSocketOption.Binding.post, options = options)
        reader.stop()
        if (postFailures.isNotEmpty()) {
            Log.i(TAG, "srt-client: $cameraId: Failed to set post-bind options: $postFailures.")
        }
        reader = MpegTsReader(
            name = "srt-client",
            decoderQueue = srtClientQueue,
            timecodesEnabled = false,
            softwareDecoding = softwareDecoding,
            colorRange = colorRange,
            targetLatency = srtClientLatency,
        )
        reader.delegate = this
        delegate?.srtClientConnected(cameraId = cameraId)
        receive(socket = socket)
        delegate?.srtClientDisconnected(cameraId = cameraId)
        CoroutineScope(srtClientQueue).launch {
            connectSoon(delay = reconnectDelay)
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
                it.value.add(bytesTransferred = count)
            }
            try {
                reader.handlePacketFromClient(packet = packet.copyOf(count))
            } catch (error: Exception) {
                Log.i(TAG, "srt-client: $cameraId: Got corrupt packet: $error.")
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
        delegate?.srtClientOnVideoBuffer(cameraId = cameraId, sampleBuffer = sampleBuffer)
    }

    override fun mpegTsReaderAudioBuffer(sampleBuffer: MediaSample) {
        delegate?.srtClientOnAudioBuffer(cameraId = cameraId, sampleBuffer = sampleBuffer)
    }
}

private fun sockaddrIn(host: String, port: Int): ByteArray {
    return SrtNative.sockaddrIn(host, port)
}

private fun lastSrtError(): String {
    return SrtNative.srt_getlasterror_str()
}

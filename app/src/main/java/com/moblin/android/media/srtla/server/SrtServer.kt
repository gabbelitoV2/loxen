package com.moblin.android.media.srtla.server

import android.util.Log
import com.moblin.android.media.haishinkit.srt.SrtSocketOption
import com.moblin.android.platform.srt.SrtError
import com.moblin.android.platform.srt.SrtNative
import com.moblin.android.various.settings.SettingsStreamColorRange
import com.moblin.android.various.utils.startBlockingThread

private const val TAG = "SrtServer"

open class SrtServer(
    private val timecodesEnabled: Boolean,
    private val softwareDecoding: Boolean,
    private val colorRange: SettingsStreamColorRange,
    private val port: Int,
    private val srtlaPatches: Boolean,
) {
    var srtlaServer: SrtlaServer? = null

    @Volatile
    private var listenerSocket: Int = SrtNative.SRT_INVALID_SOCK

    @Volatile
    var running: Boolean = false

    open fun start() {
        SrtNative.srt_startup()
        running = true
        startBlockingThread(name = "com.eerimoq.srtla-srt-server") {
            try {
                main()
            } catch (error: Exception) {
                Log.i(TAG, "srt-server: $port: $error")
            }
        }
    }

    open fun stop() {
        SrtNative.srt_close(listenerSocket)
        listenerSocket = SrtNative.SRT_INVALID_SOCK
        running = false
        SrtNative.srt_cleanup()
    }

    private fun main() {
        open()
        if (srtlaPatches) {
            Log.i(TAG, "srt-server: $port: Enabling SRTLA patches.")
            setSrtlaPatchesOption()
            setLossMaxTtlOption()
        }
        bind()
        listen()
        while (true) {
            Log.i(TAG, "srt-server: $port: Waiting for client to connect.")
            val clientSocket = accept()
            val streamId = getStreamId(clientSocket)
            val srtlaServer = this.srtlaServer
            val stream = srtlaServer?.settings?.streams?.firstOrNull { it.streamId == streamId }
            if (srtlaServer == null || stream == null || srtlaServer.connectedStreamIds.value.contains(streamId)) {
                SrtNative.srt_close(clientSocket)
                Log.i(TAG, "srt-server: $port: Client with stream id '$streamId' denied.")
                continue
            }
            Log.i(TAG, "srt-server: $port: Accepted client ${stream.name}.")
            val cameraId = stream.id
            val name = stream.camera()
            startBlockingThread(name = "com.eerimoq.Moblin.SrtClient") {
                srtlaServer.connectedStreamIds.mutate { it.value = it.value + streamId }
                srtlaServer.clientConnected(cameraId = cameraId, name = name)
                SrtServerClient(
                    server = this@SrtServer,
                    cameraId = cameraId,
                    timecodesEnabled = timecodesEnabled,
                    softwareDecoding = softwareDecoding,
                    colorRange = colorRange,
                ).run(clientSocket = clientSocket)
                srtlaServer.connectedStreamIds.mutate { it.value = it.value.filterNot { id -> id == streamId } }
                srtlaServer.clientDisconnected(cameraId = cameraId, name = name)
                Log.i(TAG, "srt-server: $port: Closed client.")
            }
        }
    }

    private fun getStreamId(socket: Int): String {
        val streamId = ByteArray(513)
        val size = intArrayOf(512)
        if (SrtNative.srt_getsockflag(socket, SrtNative.SRTO_STREAMID, streamId, size) == SrtNative.SRT_ERROR) {
            return ""
        }
        val end = streamId.indexOfFirst { it.toInt() == 0 }
        val bytes = if (end == -1) streamId else streamId.copyOf(end)
        return bytes.toString(Charsets.UTF_8)
    }

    private fun open() {
        listenerSocket = SrtNative.srt_create_socket()
        if (listenerSocket == SrtNative.SRT_INVALID_SOCK) {
            throw SrtError("Failed to create socket: ${lastSrtSocketError()}")
        }
    }

    private fun setSrtlaPatchesOption() {
        val srtlaPatches = SrtSocketOption.fromRawValue("srtlaPatches")!!
        if (!srtlaPatches.setOption(listenerSocket, value = "1")) {
            throw SrtError("Failed to set srtlaPatches option.")
        }
    }

    private fun setLossMaxTtlOption() {
        val option = SrtSocketOption.fromRawValue("lossmaxttl")!!
        if (!option.setOption(listenerSocket, value = "30")) {
            Log.i(TAG, "srt-server: $port: Failed to set lossmaxttl option.")
        }
    }

    private fun bind() {
        val addr = SrtNative.sockaddrIn("0.0.0.0", port)
        val addrSize = addr.size
        val res = SrtNative.srt_bind(listenerSocket, addr, addrSize)
        if (res == SrtNative.SRT_ERROR) {
            throw SrtError("Bind failed: ${lastSrtSocketError()}")
        }
    }

    private fun listen() {
        if (SrtNative.srt_listen(listenerSocket, 5) == SrtNative.SRT_ERROR) {
            throw SrtError("Listen failed: ${lastSrtSocketError()}")
        }
    }

    private fun accept(): Int {
        val clientSocket = SrtNative.srt_accept(listenerSocket)
        if (clientSocket == SrtNative.SRT_ERROR) {
            throw SrtError("Accept failed: ${lastSrtSocketError()}")
        }
        return clientSocket
    }
}

private fun lastSrtSocketError(): String {
    return SrtNative.srt_getlasterror_str()
}

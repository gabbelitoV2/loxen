package com.moblin.android.media.srtla.server

import android.util.Log
import com.moblin.android.media.haishinkit.srt.SrtNative
import com.moblin.android.media.haishinkit.srt.SrtSocketOption
import com.moblin.android.various.utils.startBlockingThread

private const val TAG = "SrtServer"

class SrtServer(
    private val timecodesEnabled: Boolean,
    private val softwareDecoding: Boolean,
    private val port: Int,
    private val srtlaPatches: Boolean,
) {
    var srtlaServer: SrtlaServer? = null
    private var listenerSocket: Int = SrtNative.SRT_INVALID_SOCK
    var running: Boolean = false

    fun start() {
        SrtNative.srt_startup()
        running = true
        startBlockingThread(name = "com.eerimoq.srtla-srt-server") {
            try {
                main()
            } catch (e: Exception) {
                Log.i(TAG, "srt-server: $port: $e")
            }
        }
    }

    fun stop() {
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
            if (
                srtlaServer == null ||
                stream == null ||
                srtlaServer.connectedStreamIds.value.contains(streamId)
            ) {
                SrtNative.srt_close(clientSocket)
                Log.i(TAG, "srt-server: $port: Client with stream id '$streamId' denied.")
                continue
            }
            Log.i(TAG, "srt-server: $port: Accepted client ${stream.name}.")
            val cameraId = stream.id
            val name = stream.camera()
            startBlockingThread(name = "com.eerimoq.Moblin.SrtClient") {
                srtlaServer.connectedStreamIds.mutate { ids -> ids.add(streamId) }
                srtlaServer.clientConnected(cameraId = cameraId, name = name)
                SrtServerClient(
                    server = this@SrtServer,
                    cameraId = cameraId,
                    timecodesEnabled = timecodesEnabled,
                    softwareDecoding = softwareDecoding,
                ).run(clientSocket = clientSocket)
                srtlaServer.connectedStreamIds.mutate { ids -> ids.removeAll { it == streamId } }
                srtlaServer.clientDisconnected(cameraId = cameraId, name = name)
                Log.i(TAG, "srt-server: $port: Closed client.")
            }
        }
    }

    private fun getStreamId(socket: Int): String {
        val streamId = ByteArray(513)
        val size = intArrayOf(512)
        if (
            SrtNative.srt_getsockflag(socket, SrtNative.SRTO_STREAMID, streamId, size) ==
            SrtNative.SRT_ERROR
        ) {
            return ""
        }
        val end = streamId.indexOfFirst { it.toInt() == 0 }
        val bytes = if (end == -1) streamId else streamId.copyOf(end)
        return bytes.toString(Charsets.UTF_8)
    }

    private fun open() {
        listenerSocket = SrtNative.srt_create_socket()
        if (listenerSocket == SrtNative.SRT_INVALID_SOCK) {
            throw IllegalStateException("Failed to create socket: ${lastSrtSocketError()}")
        }
    }

    private fun setSrtlaPatchesOption() {
        val srtlaPatches = SrtSocketOption("srtlaPatches")!!
        if (!srtlaPatches.setOption(listenerSocket, "1")) {
            throw IllegalStateException("Failed to set srtlaPatches option.")
        }
    }

    private fun setLossMaxTtlOption() {
        val option = SrtSocketOption("lossmaxttl")!!
        if (!option.setOption(listenerSocket, "30")) {
            Log.i(TAG, "srt-server: $port: Failed to set lossmaxttl option.")
        }
    }

    private fun bind() {
        val addr = ByteArray(16)
        addr[0] = 2
        addr[1] = 0
        addr[2] = ((port shr 8) and 0xFF).toByte()
        addr[3] = (port and 0xFF).toByte()
        val res = SrtNative.srt_bind(listenerSocket, addr, addr.size)
        if (res == SrtNative.SRT_ERROR) {
            throw IllegalStateException("Bind failed: ${lastSrtSocketError()}")
        }
    }

    private fun listen() {
        if (SrtNative.srt_listen(listenerSocket, 5) == SrtNative.SRT_ERROR) {
            throw IllegalStateException("Listen failed: ${lastSrtSocketError()}")
        }
    }

    private fun accept(): Int {
        val clientSocket = SrtNative.srt_accept(listenerSocket, null, null)
        if (clientSocket == SrtNative.SRT_ERROR) {
            throw IllegalStateException("Accept failed: ${lastSrtSocketError()}")
        }
        return clientSocket
    }
}

private fun lastSrtSocketError(): String {
    return SrtNative.srt_getlasterror_str()
}

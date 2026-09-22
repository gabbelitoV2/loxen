package com.moblin.android.media.srtla.server

import android.util.Log
import com.moblin.android.media.haishinkit.srt.SrtNative
import com.moblin.android.media.haishinkit.srt.SrtSocketOption
import com.moblin.android.various.utils.startBlockingThread

private const val TAG = "SrtServer"
private const val SRT_INVALID_SOCK: Int = -1
private const val SRT_ERROR: Int = -1
private const val SRTO_STREAMID: Int = 15

class SrtServer(
    private val timecodesEnabled: Boolean,
    private val softwareDecoding: Boolean,
    private val port: Int,
    private val srtlaPatches: Boolean,
) {
    var srtlaServer: SrtlaServer? = null
    private var listenerSocket: Int = SRT_INVALID_SOCK
    var running: Boolean = false

    fun start() {
        Unit
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
        Unit
        listenerSocket = SRT_INVALID_SOCK
        running = false
        Unit
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
                Unit
                Log.i(TAG, "srt-server: $port: Client with stream id '$streamId' denied.")
                continue
            }
            Log.i(TAG, "srt-server: $port: Accepted client ${stream.name}.")
            val cameraId = stream.id
            val name = stream.camera()
            startBlockingThread(name = "com.eerimoq.Moblin.SrtClient") {
                srtlaServer.connectedStreamIds.mutate { ref -> ref.value + streamId }
                srtlaServer.clientConnected(cameraId = cameraId, name = name)
                SrtServerClient(
                    server = this@SrtServer,
                    cameraId = cameraId,
                    timecodesEnabled = timecodesEnabled,
                    softwareDecoding = softwareDecoding,
                ).run(clientSocket = clientSocket)
                srtlaServer.connectedStreamIds.mutate { ref -> ref.value.filterNot { it == streamId } }
                srtlaServer.clientDisconnected(cameraId = cameraId, name = name)
                Log.i(TAG, "srt-server: $port: Closed client.")
            }
        }
    }

    private fun getStreamId(socket: Int): String {
        val streamId = ByteArray(513)
        val size = intArrayOf(512)
        val result: Int = TODO("srt_getsockflag")
        if (
            result ==
            SRT_ERROR
        ) {
            return ""
        }
        val end = streamId.indexOfFirst { it.toInt() == 0 }
        val bytes = if (end == -1) streamId else streamId.copyOf(end)
        return bytes.toString(Charsets.UTF_8)
    }

    private fun open() {
        listenerSocket = TODO("srt_create_socket")
        if (listenerSocket == SRT_INVALID_SOCK) {
            throw IllegalStateException("Failed to create socket: ${lastSrtSocketError()}")
        }
    }

    private fun setSrtlaPatchesOption() {
        val srtlaPatches = SrtSocketOption.fromRawValue("srtlaPatches")!!
        if (!srtlaPatches.setOption(listenerSocket, "1")) {
            throw IllegalStateException("Failed to set srtlaPatches option.")
        }
    }

    private fun setLossMaxTtlOption() {
        val option = SrtSocketOption.fromRawValue("lossmaxttl")!!
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
        val res: Int = TODO("srt_bind")
        if (res == SRT_ERROR) {
            throw IllegalStateException("Bind failed: ${lastSrtSocketError()}")
        }
    }

    private fun listen() {
        val result: Int = TODO("srt_listen")
        if (result == SRT_ERROR) {
            throw IllegalStateException("Listen failed: ${lastSrtSocketError()}")
        }
    }

    private fun accept(): Int {
        val clientSocket: Int = TODO("srt_accept")
        if (clientSocket == SRT_ERROR) {
            throw IllegalStateException("Accept failed: ${lastSrtSocketError()}")
        }
        return clientSocket
    }
}

private fun lastSrtSocketError(): String {
    return ""
}

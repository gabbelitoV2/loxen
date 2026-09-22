package com.moblin.android.media.srtla.client

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress

class LocalListener {
    private var listener: DatagramSocket? = null
    @Volatile private var connection: DatagramSocket? = null
    @Volatile private var remoteAddress: InetSocketAddress? = null
    var onReady: ((port: Int) -> Unit)? = null
    var onError: ((message: String) -> Unit)? = null
    private val scope = CoroutineScope(SupervisorJob() + srtlaClientQueue)
    private var receiveLoopJob: Job? = null

    init {
    }

    fun start() {
        val socket = try {
            DatagramSocket(null).apply {
                reuseAddress = true
                bind(InetSocketAddress(InetAddress.getLoopbackAddress(), 0))
            }
        } catch (e: Exception) {
            Log.i(TAG, "srtla: local: Failed to create listener with error $e")
            return
        }
        listener = socket
        receiveLoopJob = scope.launch {
            handleListenerStateChange(ListenerState.ready)
            receiveLoop(socket)
        }
    }

    fun stop() {
        receiveLoopJob?.cancel()
        receiveLoopJob = null
        listener?.close()
        listener = null
        connection?.close()
        connection = null
        remoteAddress = null
    }

    fun sendPacket(packet: ByteArray) {
        val socket = connection ?: return
        val address = remoteAddress ?: return
        scope.launch {
            try {
                socket.send(DatagramPacket(packet, packet.size, address))
            } catch (e: Exception) {
                Log.i(TAG, "srtla: local: Failed to send packet with error $e")
            }
        }
    }

    private fun handleListenerStateChange(state: ListenerState) {
        when (state) {
            ListenerState.setup -> {
            }
            ListenerState.ready -> {
                val port = listener?.localPort ?: return
                onReady?.invoke(port)
            }
            ListenerState.failed -> onError?.invoke("bad network state")
        }
    }

    private fun handleNewListenerConnection(socket: DatagramSocket, address: InetSocketAddress) {
        connection = socket
        remoteAddress = address
    }

    private suspend fun receiveLoop(socket: DatagramSocket) {
        val buffer = ByteArray(MAX_PACKET_SIZE)
        while (currentCoroutineContext().isActive) {
            val packet = DatagramPacket(buffer, buffer.size)
            try {
                socket.receive(packet)
            } catch (e: Exception) {
                Log.i(TAG, "srtla: local: Failed to receive packet with error $e")
                return
            }
            val address = InetSocketAddress(packet.address, packet.port)
            val current = remoteAddress
            if (current == null || current.address != address.address || current.port != address.port) {
                handleNewListenerConnection(socket, address)
            }
        }
    }

    private enum class ListenerState {
        setup,
        ready,
        failed,
    }

    companion object {
        private const val TAG = "LocalListener"
        private const val MAX_PACKET_SIZE = 65507
    }
}

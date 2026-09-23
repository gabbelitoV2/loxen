package com.moblin.android.media.srtla.client

import android.util.Log
import com.moblin.android.platform.network.NWConnection
import com.moblin.android.platform.network.NWListener
import com.moblin.android.platform.network.NWParameters
import com.moblin.android.platform.network.NWProtocolUDP

private const val TAG = "LocalListener"

class LocalListener {
    private var listener: NWListener? = null
    private var connection: NWConnection? = null
    var onReady: ((port: Int) -> Unit)? = null
    var onError: ((message: String) -> Unit)? = null

    fun start() {
        val listener = try {
            val options = NWProtocolUDP.Options()
            val parameters = NWParameters.dtls(null, options)
            parameters.acceptLocalOnly = true
            NWListener(using = parameters)
        } catch (error: Exception) {
            Log.i(TAG, "srtla: local: Failed to create listener with error $error")
            return
        }
        this.listener = listener
        listener.stateUpdateHandler = ::handleListenerStateChange
        listener.newConnectionHandler = ::handleNewListenerConnection
        listener.start(queue = srtlaClientQueue)
    }

    fun stop() {
        listener?.cancel()
        listener = null
        connection?.cancel()
        connection = null
    }

    fun sendPacket(packet: ByteArray) {
        val connection = connection ?: return
        connection.send(content = packet, completion = NWConnection.SendCompletion.idempotent)
    }

    private fun handleListenerStateChange(state: NWListener.State) {
        when (state) {
            NWListener.State.setup -> Unit
            NWListener.State.ready -> {
                val port = listener?.port ?: return
                onReady?.invoke(port.value)
            }
            else -> onError?.invoke("bad network state")
        }
    }

    private fun handleNewListenerConnection(connection: NWConnection) {
        this.connection = connection
        connection.start(queue = srtlaClientQueue)
    }
}

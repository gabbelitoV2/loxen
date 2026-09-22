package com.moblin.android.media.haishinkit.rtmp

import android.util.Log
import com.moblin.android.media.haishinkit.rtmp.amf.AsObject
import java.io.OutputStream
import java.net.Socket
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class RtmpSocketReadyState {
    uninitialized,
    versionSent,
    ackSent,
    handshakeDone,
    closed,
}

interface RtmpSocketDelegate {
    fun socketDataReceived(data: ByteArray): ByteArray

    fun socketReadyStateChanged(readyState: RtmpSocketReadyState)

    fun socketUpdateStats(totalBytesSent: Long)

    fun socketPost(data: AsObject)
}

private sealed class RtmpSocketConnectionState {
    data object Ready : RtmpSocketConnectionState()

    data class Failed(val error: Throwable) : RtmpSocketConnectionState()

    data object Cancelled : RtmpSocketConnectionState()
}

class RtmpSocket(private val name: String, private val queue: CoroutineDispatcher) {
    var maximumChunkSizeToServer: Int = RtmpChunk.defaultSize
    private var readyState: RtmpSocketReadyState = RtmpSocketReadyState.uninitialized
    private var inputBuffer: ByteArray = ByteArray(0)
    var delegate: RtmpSocketDelegate? = null
    private var totalBytesSending: Long = 0
    private var totalBytesSent: Long = 0
    private var connection: Socket? = null
    private var outputStream: OutputStream? = null
    private val scope: CoroutineScope = CoroutineScope(queue)

    fun connect(host: String, port: Int, tlsOptions: SSLContext?) {
        setReadyState(RtmpSocketReadyState.uninitialized)
        maximumChunkSizeToServer = RtmpChunk.defaultSize
        totalBytesSending = 0
        totalBytesSent = 0
        inputBuffer = ByteArray(0)
        scope.launch(Dispatchers.IO) {
            try {
                val socket: Socket = if (tlsOptions != null) {
                    val sslSocket = tlsOptions.socketFactory.createSocket(host, port) as SSLSocket
                    sslSocket.startHandshake()
                    sslSocket
                } else {
                    Socket(host, port)
                }
                connection = socket
                outputStream = socket.getOutputStream()
                stateDidChange(RtmpSocketConnectionState.Ready)
                receive(socket)
            } catch (error: Throwable) {
                stateDidChange(RtmpSocketConnectionState.Failed(error))
            }
        }
    }

    fun close(isDisconnected: Boolean) {
        val current = connection
        if (current != null) {
            outputStream = null
            scope.launch {
                delay(1000)
                runCatching { current.close() }
            }
        }
        val wasHandshakeDone = readyState == RtmpSocketReadyState.handshakeDone
        setReadyState(RtmpSocketReadyState.closed)
        if (isDisconnected) {
            val data: AsObject = if (wasHandshakeDone) {
                RtmpConnectionCode.connectClosed.eventData()
            } else {
                RtmpConnectionCode.connectFailed.eventData()
            }
            delegate?.socketPost(data = data)
        }
    }

    fun write(chunk: RtmpChunk): Int {
        for (data in chunk.split(maximumChunkSizeToServer)) {
            write(data = data)
        }
        return chunk.message.length
    }

    private fun setReadyState(state: RtmpSocketReadyState) {
        if (readyState == state) {
            return
        }
        Log.i(tag, "rtmp: $name: Setting socket state $readyState -> $state")
        readyState = state
        delegate?.socketReadyStateChanged(readyState = state)
    }

    private fun write(data: ByteArray) {
        val size = data.size.toLong()
        totalBytesSending += size
        val output = outputStream
        if (output != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    output.write(data)
                    output.flush()
                    totalBytesSent += size
                } catch (error: Throwable) {
                    close(isDisconnected = true)
                    return@launch
                }
            }
        }
        delegate?.socketUpdateStats(totalBytesSent = totalBytesSending)
        if (hasTooMuchDataBuffered()) {
            Log.i(tag, "rtmp: $name: Too much data buffered. Disconnecting.")
            scope.launch {
                close(isDisconnected = true)
            }
        }
    }

    private fun hasTooMuchDataBuffered(): Boolean {
        return totalBytesSending - totalBytesSent > 100_000_000
    }

    private fun viabilityDidChange(viability: Boolean) {
        Log.i(tag, "rtmp: $name: Connection viability changed to $viability")
        if (!viability) {
            close(isDisconnected = true)
        }
    }

    private fun stateDidChange(state: RtmpSocketConnectionState) {
        when (state) {
            RtmpSocketConnectionState.Ready -> {
                Log.i(tag, "rtmp: $name: Connection is ready.")
                write(data = RtmpHandshake.createC0C1Packet())
                setReadyState(RtmpSocketReadyState.versionSent)
            }
            is RtmpSocketConnectionState.Failed -> {
                Log.i(tag, "rtmp: $name: Connection failed: ${state.error}")
                close(isDisconnected = true)
            }
            RtmpSocketConnectionState.Cancelled -> {
                Log.i(tag, "rtmp: $name: Connection cancelled.")
                close(isDisconnected = true)
            }
        }
    }

    private fun receive(connection: Socket) {
        scope.launch(Dispatchers.IO) {
            try {
                val input = connection.getInputStream()
                val buffer = ByteArray(255)
                while (isActive && !connection.isClosed) {
                    val read = input.read(buffer)
                    if (read < 0) {
                        break
                    }
                    if (read > 0) {
                        inputBuffer += buffer.copyOf(read)
                        processInput()
                    }
                }
            } catch (error: Throwable) {
                Log.i(tag, "rtmp: $name: Receive ended: $error")
            }
        }
    }

    private fun processInput() {
        when (readyState) {
            RtmpSocketReadyState.versionSent -> processInputVersionSent()
            RtmpSocketReadyState.ackSent -> processInputAckSent()
            RtmpSocketReadyState.handshakeDone -> processInputHandshakeDone()
            else -> Unit
        }
    }

    private fun processInputVersionSent() {
        if (inputBuffer.size < RtmpHandshake.sigSize + 1) {
            return
        }
        write(data = RtmpHandshake.createC2Packet(inputBuffer))
        inputBuffer = inputBuffer.copyOfRange(RtmpHandshake.sigSize + 1, inputBuffer.size)
        setReadyState(RtmpSocketReadyState.ackSent)
        processInput()
    }

    private fun processInputAckSent() {
        if (inputBuffer.size < RtmpHandshake.sigSize) {
            return
        }
        inputBuffer = ByteArray(0)
        setReadyState(RtmpSocketReadyState.handshakeDone)
    }

    private fun processInputHandshakeDone() {
        if (inputBuffer.isEmpty()) {
            return
        }
        val delegate = this.delegate ?: return
        inputBuffer = delegate.socketDataReceived(data = inputBuffer)
    }

    private companion object {
        private const val tag = "RtmpSocket"
    }
}

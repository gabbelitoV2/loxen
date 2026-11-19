package com.moblin.android.media.haishinkit.rtmp

import com.moblin.android.platform.log.Log
import com.moblin.android.media.haishinkit.util.ByteWriter
import com.moblin.android.platform.network.NWConnection
import com.moblin.android.platform.network.NWEndpoint
import com.moblin.android.platform.network.NWParameters
import com.moblin.android.platform.network.NWProtocolTLS
import java.time.Instant
import kotlin.random.Random
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "RtmpSocket"

enum class RtmpSocketReadyState {
    uninitialized,
    versionSent,
    ackSent,
    handshakeDone,
    closed,
}

object RtmpHandshake {
    const val sigSize = 1536
    val protocolVersion: UByte = 3.toUByte()
    private const val timestamp: Double = 0.0

    fun createC0C1Packet(): ByteArray {
        val writer = ByteWriter()
        writer.writeUInt8(RtmpHandshake.protocolVersion)
        writer.writeInt32(timestamp.toInt())
        writer.writeBytes(byteArrayOf(0x00, 0x00, 0x00, 0x00))
        repeat(RtmpHandshake.sigSize - 8) {
            writer.writeUInt8(Random.nextInt(0, 256).toUByte())
        }
        return writer.data
    }

    fun createC2Packet(s0s1packet: ByteArray): ByteArray {
        val writer = ByteWriter()
        writer.writeBytes(s0s1packet.copyOfRange(1, 5))
        writer.writeUInt32((Instant.now().toEpochMilli() / 1000.0 - timestamp).toLong().toUInt())
        writer.writeBytes(s0s1packet.copyOfRange(9, RtmpHandshake.sigSize + 1))
        return writer.data
    }
}

interface RtmpSocketDelegate {
    fun socketHandshakeDone(socket: RtmpSocket)

    fun socketClosed(socket: RtmpSocket)

    fun socketDisconnected(socket: RtmpSocket, code: RtmpStatusCode)

    fun socketDataReceived(socket: RtmpSocket, data: ByteArray): ByteArray

    fun socketUpdateStats(socket: RtmpSocket, totalBytesSent: Long)
}

class RtmpSocket(private val name: String, private val queue: CoroutineDispatcher) {
    var maximumChunkSizeToServer = RtmpChunk.defaultSize
    var readyState: RtmpSocketReadyState = RtmpSocketReadyState.uninitialized
        private set
    private var inputBuffer = ByteArray(0)
    var delegate: RtmpSocketDelegate? = null
    private var totalBytesSending: Long = 0
    private var totalBytesSent: Long = 0
    private var connection: NWConnection? = null

    fun connect(host: String, port: Int, tlsOptions: NWProtocolTLS.Options?) {
        setReadyState(state = RtmpSocketReadyState.uninitialized)
        maximumChunkSizeToServer = RtmpChunk.defaultSize
        totalBytesSending = 0
        totalBytesSent = 0
        inputBuffer = ByteArray(0)
        connection = NWConnection(
            endpoint = NWEndpoint.hostPort(host = NWEndpoint.Host(host), port = NWEndpoint.Port(port)),
            parameters = NWParameters.tls(tlsOptions),
        )
        connection!!.viabilityUpdateHandler = ::viabilityDidChange
        connection!!.stateUpdateHandler = ::stateDidChange
        connection!!.start(queue = queue)
        receive(connection = connection!!)
    }

    fun close(isDisconnected: Boolean) {
        if (readyState == RtmpSocketReadyState.closed) {
            return
        }
        val connection = connection
        if (connection != null) {
            connection.viabilityUpdateHandler = null
            connection.stateUpdateHandler = null
            CoroutineScope(queue).launch {
                delay(1000)
                connection.cancel()
            }
        }
        val wasHandshakeDone = readyState == RtmpSocketReadyState.handshakeDone
        setReadyState(state = RtmpSocketReadyState.closed)
        if (isDisconnected) {
            delegate?.socketDisconnected(
                socket = this,
                code = if (wasHandshakeDone) RtmpStatusCode.connectClosed else RtmpStatusCode.connectFailed,
            )
        }
    }

    fun write(chunk: RtmpChunk): Int {
        for (data in chunk.split(maximumSize = maximumChunkSizeToServer)) {
            write(data = data)
        }
        return chunk.message.length
    }

    private fun setReadyState(state: RtmpSocketReadyState) {
        if (readyState == state) {
            return
        }
        Log.i(TAG, "rtmp: $name: Setting socket state $readyState -> $state")
        readyState = state
        when (state) {
            RtmpSocketReadyState.handshakeDone -> delegate?.socketHandshakeDone(socket = this)
            RtmpSocketReadyState.closed -> delegate?.socketClosed(socket = this)
            else -> Unit
        }
    }

    private fun write(data: ByteArray) {
        val size = data.size.toLong()
        totalBytesSending += size
        connection?.send(content = data, completion = NWConnection.SendCompletion.contentProcessed { error ->
            if (readyState != RtmpSocketReadyState.closed) {
                if (error != null) {
                    close(isDisconnected = true)
                } else {
                    totalBytesSent += size
                }
            }
        })
        delegate?.socketUpdateStats(socket = this, totalBytesSent = totalBytesSending)
        if (hasTooMuchDataBuffered()) {
            Log.i(TAG, "rtmp: $name: Too much data buffered. Disconnecting.")
            CoroutineScope(queue).launch {
                close(isDisconnected = true)
            }
        }
    }

    private fun hasTooMuchDataBuffered(): Boolean {
        return totalBytesSending - totalBytesSent > 100_000_000
    }

    private fun viabilityDidChange(viability: Boolean) {
        Log.i(TAG, "rtmp: $name: Connection viability changed to $viability")
        if (!viability) {
            close(isDisconnected = true)
        }
    }

    private fun stateDidChange(state: NWConnection.State) {
        when (state) {
            NWConnection.State.ready -> {
                Log.i(TAG, "rtmp: $name: Connection is ready.")
                write(data = RtmpHandshake.createC0C1Packet())
                setReadyState(state = RtmpSocketReadyState.versionSent)
            }
            is NWConnection.State.waiting -> {
                Log.i(TAG, "rtmp: $name: Connection waiting: ${state.error}")
                close(isDisconnected = true)
            }
            is NWConnection.State.failed -> {
                Log.i(TAG, "rtmp: $name: Connection failed: ${state.error}")
                close(isDisconnected = true)
            }
            NWConnection.State.cancelled -> {
                Log.i(TAG, "rtmp: $name: Connection cancelled.")
                close(isDisconnected = true)
            }
            else -> Unit
        }
    }

    private fun receive(connection: NWConnection) {
        connection.receive(minimumIncompleteLength = 0, maximumLength = 255) { data, _, isComplete, error ->
            if (data != null && data.isNotEmpty()) {
                inputBuffer += data
                processInput()
            }
            if (error != null) {
                Log.i(TAG, "rtmp: $name: Receive failed: $error")
                close(isDisconnected = true)
            } else if (isComplete) {
                Log.i(TAG, "rtmp: $name: Connection closed by server")
                close(isDisconnected = true)
            } else {
                receive(connection = connection)
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
        setReadyState(state = RtmpSocketReadyState.ackSent)
        processInput()
    }

    private fun processInputAckSent() {
        if (inputBuffer.size < RtmpHandshake.sigSize) {
            return
        }
        inputBuffer = ByteArray(0)
        setReadyState(state = RtmpSocketReadyState.handshakeDone)
    }

    private fun processInputHandshakeDone() {
        val delegate = delegate
        if (inputBuffer.isEmpty() || delegate == null) {
            return
        }
        inputBuffer = delegate.socketDataReceived(socket = this, data = inputBuffer)
    }
}

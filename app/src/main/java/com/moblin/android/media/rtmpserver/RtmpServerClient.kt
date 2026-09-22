package com.moblin.android.media.rtmpserver

import android.util.Log
import com.moblin.android.media.MediaSample
import com.moblin.android.media.haishinkit.rtmp.RtmpChunk
import com.moblin.android.media.haishinkit.rtmp.RtmpChunkType
import com.moblin.android.media.haishinkit.rtmp.message.RtmpAcknowledgementMessage
import com.moblin.android.various.utils.currentPresentationTimeStamp
import java.io.IOException
import java.net.Socket
import java.util.UUID
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val rtmpVersion: Byte = 3
private const val logTag = "RtmpServerClient"

private enum class ClientState {
    uninitialized,
    versionSent,
    ackSent,
    handshakeDone,
}

private enum class ChunkState {
    basicHeaderFirstByte,
    messageHeaderType0,
    messageHeaderType1,
    messageHeaderType2,
    extendedTimestamp,
    data,
}

enum class RtmpServerClientConnectionState {
    idle,
    connecting,
    connected,
}

class RtmpServerClient(
    val server: RtmpServer?,
    private val connection: Socket,
    private val softwareDecoding: Boolean,
) {
    private var state: ClientState
    private var chunkState: ChunkState
    var chunkSizeToClient = 128
    var chunkSizeFromClient = 128
    var windowAcknowledgementSize = 2_500_000
    private var chunkStreams: MutableMap<UShort, RtmpServerChunkStream>
    private var chunkStream: RtmpServerChunkStream? = null
    var streamKey: String = ""
    var latestReceiveTime: Long = System.nanoTime()
    var connectionState: RtmpServerClientConnectionState = RtmpServerClientConnectionState.idle
        set(value) {
            Log.i(logTag, "rtmp-server: client: State change ${field} -> $value")
            field = value
        }

    private var totalBytesReceived: ULong = 0u
    private var totalBytesReceivedAcked: ULong = 0u
    var latency: Int = 2000
    var cameraId: UUID = UUID.randomUUID()
    private var basePresentationTimeStamp: Double
    private var inputBuffer: ByteArray = ByteArray(0)
    private var receiveSize: Int = 0
    private var receiveMinimumSize: Int = 0
    private var isProcessing = false
    private val scope = CoroutineScope(SupervisorJob() + rtmpServerDispatchQueue)
    private var receiveJob: Job? = null
    private val sendLock = Any()

    init {
        state = ClientState.uninitialized
        chunkState = ChunkState.basicHeaderFirstByte
        chunkStreams = mutableMapOf()
        basePresentationTimeStamp = -1.0
    }

    fun start() {
        state = ClientState.uninitialized
        chunkState = ChunkState.basicHeaderFirstByte
        connectionState = RtmpServerClientConnectionState.connecting
        receiveData(size = 1 + 1536)
    }

    fun stop(reason: String) {
        Log.i(logTag, "rtmp-server: client: Stopping with reason: $reason")
        for (chunkStream in chunkStreams.values) {
            chunkStream.stop()
        }
        chunkStreams.clear()
        receiveJob?.cancel()
        connection.close()
        connectionState = RtmpServerClientConnectionState.idle
    }

    fun stopInternal(reason: String) {
        if (connectionState == RtmpServerClientConnectionState.idle) {
            return
        }
        server?.handleClientDisconnected(client = this, reason = reason)
    }

    private fun handleStateUpdate(state: Any?) {
    }

    fun handleFrame(sampleBuffer: MediaSample) {
        server?.delegate?.rtmpServerOnVideoBuffer(cameraId, sampleBuffer)
    }

    fun handleAudioBuffer(sampleBuffer: MediaSample) {
        server?.delegate?.rtmpServerOnAudioBuffer(cameraId, sampleBuffer)
    }

    private fun handleData(data: ByteArray) {
        if (connectionState == RtmpServerClientConnectionState.idle) {
            return
        }
        when (state) {
            ClientState.uninitialized -> handleDataUninitialized(data = data)
            ClientState.versionSent -> Unit
            ClientState.ackSent -> handleDataAckSent()
            ClientState.handshakeDone -> handleDataHandshakeDone(data = data)
        }
    }

    private fun handleDataUninitialized(data: ByteArray) {
        if (data.size != 1 + 1536) {
            stopInternal(reason = "Wrong length ${data.size} in uninitialized")
            return
        }
        val version = data[0]
        if (version != rtmpVersion) {
            stopInternal(reason = "Only version 3 is supported, not $version")
            return
        }
        val s0 = byteArrayOf(rtmpVersion)
        send(data = s0)
        var s1 = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
        s1 += randomData(length = 1528)
        send(data = s1)
        state = ClientState.versionSent
        var s2 = byteArrayOf(data[1], data[2], data[3], data[4], 0, 0, 0, 0)
        s2 += data.copyOfRange(9, data.size)
        send(data = s2)
        state = ClientState.ackSent
        receiveData(size = 1536)
    }

    private fun handleDataAckSent() {
        state = ClientState.handshakeDone
        receiveBasicHeaderFirstByte()
    }

    private fun handleDataHandshakeDone(data: ByteArray) {
        when (chunkState) {
            ChunkState.basicHeaderFirstByte -> handleDataHandshakeDoneBasicHeaderFirstByte(data = data)
            ChunkState.messageHeaderType0 -> handleDataHandshakeDoneMessageHeaderType0(data = data)
            ChunkState.messageHeaderType1 -> handleDataHandshakeDoneMessageHeaderType1(data = data)
            ChunkState.messageHeaderType2 -> handleDataHandshakeDoneMessageHeaderType2(data = data)
            ChunkState.extendedTimestamp -> handleExtendedTimestamp(data = data)
            ChunkState.data -> handleDataHandshakeDoneData(data = data)
        }
    }

    private fun handleExtendedTimestamp(data: ByteArray) {
        if (data.size != 4) {
            stopInternal(reason = "Wrong length ${data.size} in extended timestamp")
            return
        }
        chunkStream!!.messageTimestamp = data.getUInt32Be()
        receiveChunkData()
    }

    private fun handleDataHandshakeDoneBasicHeaderFirstByte(data: ByteArray) {
        if (data.size != 1) {
            stopInternal(reason = "Wrong length ${data.size} in basic header first byte")
            return
        }
        val firstByte = data[0].toInt() and 0xFF
        val format = firstByte shr 6
        val chunkStreamId = (firstByte and 0x3F).toUShort()
        when (chunkStreamId.toInt()) {
            0 -> {
                stopInternal(reason = "Two bytes basic header is not implemented")
                return
            }
            1 -> {
                stopInternal(reason = "Three bytes basic header is not implemented")
                return
            }
        }
        if (chunkStreams[chunkStreamId] == null) {
            chunkStreams[chunkStreamId] = RtmpServerChunkStream(
                client = this,
                streamId = chunkStreamId,
                softwareDecoding = softwareDecoding
            )
        }
        chunkStream = chunkStreams[chunkStreamId]
        when (format) {
            0 -> receiveMessageHeaderType0()
            1 -> receiveMessageHeaderType1()
            2 -> receiveMessageHeaderType2()
            3 -> receiveMessageHeaderType3()
            else -> error("Invalid chunk format")
        }
    }

    private fun handleDataHandshakeDoneMessageHeaderType0(data: ByteArray) {
        if (data.size != 11) {
            stopInternal(reason = "Wrong length ${data.size} in message header type 0 header")
            return
        }
        chunkStream!!.isAbsoluteTimeStamp = true
        chunkStream!!.messageTimestamp = data.getThreeBytesBe()
        chunkStream!!.messageLength = data.getThreeBytesBe(offset = 3).toInt()
        chunkStream!!.messageTypeId = data[6].toUByte()
        chunkStream!!.messageStreamId = data.getFourBytesLe(offset = 7)
        receiveExtendedTimestampOrData()
    }

    private fun handleDataHandshakeDoneMessageHeaderType1(data: ByteArray) {
        if (data.size != 7) {
            stopInternal(reason = "Wrong length ${data.size} in message header type 1 header")
            return
        }
        chunkStream!!.isAbsoluteTimeStamp = false
        chunkStream!!.messageTimestamp = data.getThreeBytesBe()
        chunkStream!!.messageLength = data.getThreeBytesBe(offset = 3).toInt()
        chunkStream!!.messageTypeId = data[6].toUByte()
        receiveExtendedTimestampOrData()
    }

    private fun handleDataHandshakeDoneMessageHeaderType2(data: ByteArray) {
        if (data.size != 3) {
            stopInternal(reason = "Wrong length ${data.size} in message header type 2 header")
            return
        }
        chunkStream!!.isAbsoluteTimeStamp = false
        chunkStream!!.messageTimestamp = data.getThreeBytesBe()
        receiveExtendedTimestampOrData()
    }

    private fun handleDataHandshakeDoneData(data: ByteArray) {
        chunkStream!!.handleBody(data = data)
        receiveBasicHeaderFirstByte()
    }

    private fun receiveExtendedTimestampOrData() {
        if (isExtendedTimestamp(timestamp = chunkStream!!.messageTimestamp)) {
            receiveExtendedTimestamp()
        } else {
            receiveChunkData()
        }
    }

    private fun isExtendedTimestamp(timestamp: UInt): Boolean {
        chunkStream!!.extendedTimestampPresentInType3 = timestamp == 0xFFFFFFu
        return chunkStream!!.extendedTimestampPresentInType3
    }

    private fun receiveExtendedTimestamp() {
        chunkState = ChunkState.extendedTimestamp
        receiveData(size = 4)
    }

    private fun receiveBasicHeaderFirstByte() {
        chunkState = ChunkState.basicHeaderFirstByte
        receiveData(size = 1)
    }

    private fun receiveMessageHeaderType0() {
        chunkState = ChunkState.messageHeaderType0
        receiveData(size = 11)
    }

    private fun receiveMessageHeaderType1() {
        chunkState = ChunkState.messageHeaderType1
        receiveData(size = 7)
    }

    private fun receiveMessageHeaderType2() {
        chunkState = ChunkState.messageHeaderType2
        receiveData(size = 3)
    }

    private fun receiveMessageHeaderType3() {
        if (chunkStream!!.extendedTimestampPresentInType3) {
            receiveExtendedTimestamp()
        } else {
            receiveChunkData()
        }
    }

    private fun receiveChunkData() {
        val size = chunkStream!!.getChunkDataSize()
        if (size > 0) {
            chunkState = ChunkState.data
            receiveData(size = size)
        } else {
            stopInternal(reason = "Unexpected data")
        }
    }

    fun receiveData(size: Int) {
        receiveSize = size
        receiveMinimumSize = size
        if (isProcessing) {
            return
        }
        receiveDataFromNetwork()
    }

    private fun receiveDataFromNetwork() {
        receiveJob = scope.launch {
            while (isActive) {
                val buffer = ByteArray(maxOf(receiveMinimumSize, 8192))
                val length = try {
                    connection.getInputStream().read(buffer)
                } catch (e: IOException) {
                    stopInternal(reason = "Error ${e.message}")
                    return@launch
                }
                if (length < 0) {
                    stopInternal(reason = "Connection closed")
                    return@launch
                }
                processReceivedData(data = buffer.copyOfRange(0, length))
            }
        }
    }

    private fun processReceivedData(data: ByteArray) {
        totalBytesReceived += data.size.toULong()
        TODO("BitrateStats.add not available")
        latestReceiveTime = System.nanoTime()
        inputBuffer += data
        isProcessing = true
        var offset = 0
        while (inputBuffer.size - offset >= receiveSize) {
            val chunk = inputBuffer.copyOfRange(offset, offset + receiveSize)
            offset += receiveSize
            handleData(data = chunk)
        }
        inputBuffer = inputBuffer.copyOfRange(offset, inputBuffer.size)
        receiveMinimumSize = maxOf(receiveSize - inputBuffer.size, 1)
        if (totalBytesReceived - totalBytesReceivedAcked > windowAcknowledgementSize.toULong()) {
            sendAck()
            totalBytesReceivedAcked = totalBytesReceived
        }
        isProcessing = false
    }

    private fun sendAck() {
        val message = RtmpAcknowledgementMessage()
        message.sequence = (totalBytesReceived and 0xFFFF_FFFFuL).toUInt()
        sendMessage(
            chunk = RtmpChunk(
                type = RtmpChunkType.zero,
                chunkStreamId = RtmpChunk.ChunkStreamId.control.rawValue,
                message = message
            )
        )
    }

    fun sendMessage(chunk: RtmpChunk) {
        for (data in chunk.split(maximumSize = chunkSizeToClient)) {
            send(data = data)
        }
    }

    private fun send(data: ByteArray) {
        try {
            synchronized(sendLock) {
                val outputStream = connection.getOutputStream()
                outputStream.write(data)
                outputStream.flush()
            }
        } catch (e: IOException) {
            stopInternal(reason = "Error ${e.message}")
        }
    }

    fun getBasePresentationTimeStamp(): Double {
        if (basePresentationTimeStamp == -1.0) {
            basePresentationTimeStamp = 1000 * (currentPresentationTimeStamp() / 1_000_000.0)
        }
        return basePresentationTimeStamp
    }
}

private fun ByteArray.getUInt32Be(offset: Int = 0): UInt {
    return (
        ((this[offset].toInt() and 0xFF) shl 24) or
            ((this[offset + 1].toInt() and 0xFF) shl 16) or
            ((this[offset + 2].toInt() and 0xFF) shl 8) or
            (this[offset + 3].toInt() and 0xFF)
        ).toUInt()
}

private fun ByteArray.getThreeBytesBe(offset: Int = 0): UInt {
    return (
        ((this[offset].toInt() and 0xFF) shl 16) or
            ((this[offset + 1].toInt() and 0xFF) shl 8) or
            (this[offset + 2].toInt() and 0xFF)
        ).toUInt()
}

private fun ByteArray.getFourBytesLe(offset: Int = 0): UInt {
    return (
        ((this[offset + 3].toInt() and 0xFF) shl 24) or
            ((this[offset + 2].toInt() and 0xFF) shl 16) or
            ((this[offset + 1].toInt() and 0xFF) shl 8) or
            (this[offset].toInt() and 0xFF)
        ).toUInt()
}

private fun randomData(length: Int): ByteArray {
    val result = ByteArray(length)
    Random.nextBytes(result)
    return result
}

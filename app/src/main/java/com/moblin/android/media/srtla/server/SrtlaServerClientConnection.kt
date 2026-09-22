package com.moblin.android.media.srtla.server

import android.util.Log
import com.moblin.android.media.srtla.common.SrtlaPacketType
import com.moblin.android.media.srtla.common.createSrtlaPacket
import com.moblin.android.media.srtla.common.getSrtControlPacketType
import com.moblin.android.media.srtla.common.getSrtSequenceNumber
import com.moblin.android.media.srtla.common.isSrtDataPacket
import com.moblin.android.media.srtla.common.srtControlTypeSize
import java.net.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val tag = "SrtlaServerClientConnection"

private const val removeTimeout = 10.0
private val removeTimeoutNanos = (removeTimeout * 1_000_000_000.0).toLong()
private val ackPacketLength = srtControlTypeSize + 2 + 10 * 4
private const val connectionReceiveBatchSize = 100

private fun ByteArray.setUInt32Be(value: Long, offset: Int) {
    this[offset] = ((value shr 24) and 0xFF).toByte()
    this[offset + 1] = ((value shr 16) and 0xFF).toByte()
    this[offset + 2] = ((value shr 8) and 0xFF).toByte()
    this[offset + 3] = (value and 0xFF).toByte()
}

interface SrtlaServerClientConnectionDelegate {
    fun handlePacketFromSrtClient(connection: SrtlaServerClientConnection, packet: ByteArray)
}

class AckPacket {
    var data: ByteArray = createSrtlaPacket(SrtlaPacketType.ack, ackPacketLength)
    private var nextSnOffset: Int = srtControlTypeSize + 2

    fun appendSequenceNumber(sn: Long): Boolean {
        data.setUInt32Be(sn, nextSnOffset)
        nextSnOffset += 4
        return if (nextSnOffset == ackPacketLength) {
            nextSnOffset = srtControlTypeSize + 2
            true
        } else {
            false
        }
    }
}

class SrtlaServerClientConnection(val connection: Socket) {
    var latestReceivedTime: Long = System.nanoTime()
    var delegate: SrtlaServerClientConnectionDelegate? = null
    private var ackPacket = AckPacket()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var receiveJob: Job? = null

    init {
        receivePackets()
    }

    fun stop() {
        runCatching { connection.close() }
        receiveJob?.cancel()
    }

    fun isActive(now: Long): Boolean {
        return (now - latestReceivedTime) < removeTimeoutNanos
    }

    private fun receivePackets() {
        receiveJob = scope.launch {
            val input = runCatching { connection.getInputStream() }.getOrNull() ?: return@launch
            val buffer = ByteArray(8192)
            while (isActive) {
                val count = try {
                    input.read(buffer)
                } catch (e: Exception) {
                    if (isActive) {
                        Log.i(tag, "srtla-server-client: Error $e")
                    }
                    return@launch
                }
                if (count <= 0) {
                    return@launch
                }
                handlePacketFromClient(buffer.copyOf(count))
            }
        }
    }

    private fun handlePacketFromClient(packet: ByteArray) {
        if (packet.size < srtControlTypeSize) {
            Log.i(tag, "srtla-server-client: Packet too short (${packet.size} bytes.")
            return
        }
        latestReceivedTime = System.nanoTime()
        if (isSrtDataPacket(packet)) {
            handleDataPacket(packet)
        } else {
            handleControlPacket(packet)
        }
    }

    private fun handleControlPacket(packet: ByteArray) {
        val type = getSrtControlPacketType(packet)
        val srtlaType = SrtlaPacketType.fromRawValue(type)
        if (srtlaType != null) {
            handleSrtlaControlPacket(srtlaType, packet)
        } else {
            handleSrtControlPacket(packet)
        }
    }

    private fun handleSrtlaControlPacket(type: SrtlaPacketType, packet: ByteArray) {
        when (type) {
            SrtlaPacketType.keepalive -> handleSrtlaKeepalive(packet)
            SrtlaPacketType.reg2 -> handleSrtlaReg2()
            else -> Log.i(tag, "srtla-server-client: Unexpected packet $type")
        }
    }

    private fun handleSrtControlPacket(packet: ByteArray) {
        delegate?.handlePacketFromSrtClient(this, packet)
    }

    private fun handleSrtlaKeepalive(packet: ByteArray) {
        sendPacket(packet)
    }

    private fun handleSrtlaReg2() {
        Log.d(tag, "srtla-server-client: Sending reg 3 (connection registered)")
        val packet = createSrtlaPacket(SrtlaPacketType.reg3, srtControlTypeSize)
        sendPacket(packet)
    }

    private fun handleDataPacket(packet: ByteArray) {
        if (packet.size < 4) {
            return
        }
        if (ackPacket.appendSequenceNumber(getSrtSequenceNumber(packet).toLong())) {
            sendPacket(ackPacket.data)
        }
        delegate?.handlePacketFromSrtClient(this, packet)
    }

    fun sendPacket(packet: ByteArray) {
        scope.launch {
            runCatching {
                val output = connection.getOutputStream()
                output.write(packet)
                output.flush()
            }.onFailure {
                if (isActive) {
                    Log.i(tag, "srtla-server-client: Error $it")
                }
            }
        }
    }
}

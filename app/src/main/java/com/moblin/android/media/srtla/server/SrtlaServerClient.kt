package com.moblin.android.media.srtla.server

import com.moblin.android.platform.log.Log
import com.moblin.android.media.haishinkit.util.ByteWriter
import com.moblin.android.media.srtla.common.SrtPacketType
import com.moblin.android.media.srtla.common.getSrtControlPacketType
import com.moblin.android.media.srtla.common.getSrtSequenceNumber
import com.moblin.android.media.srtla.common.isSrtDataPacket
import com.moblin.android.media.srtla.common.isSrtSnAcked
import com.moblin.android.media.srtla.common.processSrtNak
import com.moblin.android.media.srtla.common.srtControlPacketTypeBit
import com.moblin.android.media.srtla.common.srtControlTypeSize
import com.moblin.android.various.SimpleTimer
import java.net.InetAddress
import java.net.Socket
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

private const val tag = "SrtlaServerClient"
private const val clientRemoveTimeout = 10.0
private const val localSrtServerConnectionReceiveBatchSize = 25

private fun ByteArray.getUInt32Be(offset: Int): UInt {
    return ((this[offset].toInt() and 0xff).toUInt() shl 24) or
        ((this[offset + 1].toInt() and 0xff).toUInt() shl 16) or
        ((this[offset + 2].toInt() and 0xff).toUInt() shl 8) or
        (this[offset + 3].toInt() and 0xff).toUInt()
}

private class NakPacket {
    private var sns: MutableList<UInt> = mutableListOf()
    private var latestNakTimestamp: UInt? = null
    private var latestNakDestinationSrtSocketId: UInt? = null

    fun setLatestTimestamp(timestamp: UInt) {
        latestNakTimestamp = timestamp
    }

    fun setLatestNakDestinationSrtSocketId(socketId: UInt) {
        latestNakDestinationSrtSocketId = socketId
    }

    fun add(sn: UInt) {
        val index = sns.indexOfFirst { it >= sn }
        if (index >= 0) {
            if (sns[index] != sn) {
                sns.add(index, sn)
            }
        } else {
            sns.add(sn)
        }
    }

    fun removeUpTo(ackSn: UInt) {
        sns = sns.filterNot { isSrtSnAcked(it, ackSn) }.toMutableList()
    }

    fun remove(sn: UInt) {
        val index = sns.indexOfFirst { it == sn }
        if (index >= 0) {
            sns.removeAt(index)
        }
    }

    fun pack(): ByteArray? {
        if (sns.isEmpty()) {
            return null
        }
        val timestamp = latestNakTimestamp ?: return null
        val socketId = latestNakDestinationSrtSocketId ?: return null
        val writer = ByteWriter()
        writer.writeUInt16(SrtPacketType.nak.rawValue or srtControlPacketTypeBit)
        writer.writeUInt16(0u.toUShort())
        writer.writeUInt32(0u)
        writer.writeUInt32(timestamp)
        writer.writeUInt32(socketId)
        for (sn in sns.take(1300 / 4)) {
            writer.writeUInt32(sn)
        }
        return writer.data
    }
}

class SrtlaServerClient(srtPort: Int) : SrtlaServerClientConnectionDelegate {
    private var localSrtServerConnection: Socket? = null
    private var connections: MutableList<SrtlaServerClientConnection> = mutableListOf()
    private var latestConnection: SrtlaServerClientConnection? = null
    val createdAt: Instant = Instant.now()
    private var nakPacket = NakPacket()
    private var periodicNakTimer = SimpleTimer(queue = Dispatchers.IO)
    private var dataPacketsToSend: MutableList<ByteArray> = mutableListOf()
    private var latestFlushDataPacketsTime: Instant = Instant.now()
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        Log.i(tag, "srtla-server-client: Creating local SRT server connection.")
        createLocalSrtServerConnection(srtPort)
        startPeriodicNakTimer()
    }

    fun stop() {
        stopPeriodicNakTimer()
        runCatching { localSrtServerConnection?.close() }
        localSrtServerConnection = null
    }

    private fun createLocalSrtServerConnection(srtPort: Int) {
        val socket = runCatching {
            Socket(InetAddress.getByName("127.0.0.1"), srtPort).also { it.tcpNoDelay = true }
        }.onFailure {
            handleStateUpdate("failed: ${it.message}")
        }.getOrNull()
        localSrtServerConnection = socket
        if (socket != null) {
            handleStateUpdate("ready")
        }
        receivePackets()
    }

    private fun startPeriodicNakTimer() {
        periodicNakTimer.startPeriodic(interval = 0.1) {
            handlePeriodicNakTimer()
        }
    }

    private fun stopPeriodicNakTimer() {
        periodicNakTimer.stop()
    }

    private fun handlePeriodicNakTimer() {
        val packet = nakPacket.pack() ?: return
        sendPacketOnLatestConnection(packet)
    }

    private fun handleStateUpdate(state: String) {
        Log.i(tag, "srtla-server-client: State change to $state")
    }

    private fun receivePackets() {
        val socket = localSrtServerConnection ?: return
        scope.launch {
            val input = runCatching { socket.getInputStream() }.getOrNull() ?: return@launch
            val buffer = ByteArray(1500)
            while (isActive) {
                var count = 0
                while (count < localSrtServerConnectionReceiveBatchSize) {
                    val read = runCatching { input.read(buffer) }.getOrElse {
                        Log.i(tag, "srtla-server-client: Receive $it")
                        return@launch
                    }
                    if (read < 0) {
                        return@launch
                    }
                    if (read > 0) {
                        handlePacketFromLocalSrtServer(buffer.copyOf(read))
                    }
                    count += 1
                }
                yield()
            }
        }
    }

    fun addConnection(connection: Socket) {
        if (connections.any { it.connection.remoteSocketAddress == connection.remoteSocketAddress }) {
            Log.i(tag, "srtla-server-client: Connection ${connection.remoteSocketAddress} already registered")
            return
        }
        val clientConnection = SrtlaServerClientConnection(connection)
        clientConnection.delegate = this
        connections.add(clientConnection)
        Log.i(tag, "srtla-server-client: Added connection. Using ${connections.size} connection(s)")
    }

    private fun handlePacketFromLocalSrtServer(packet: ByteArray) {
        if (packet.size < srtControlTypeSize) {
            Log.i(tag, "srtla-server-client: Packet too short (${packet.size} bytes.")
            return
        }
        if (isSrtDataPacket(packet)) {
            sendPacketOnLatestConnection(packet)
        } else {
            when (SrtPacketType.fromRawValue(getSrtControlPacketType(packet))) {
                SrtPacketType.ack -> handleAckPacketFromLocalSrtServer(packet)
                SrtPacketType.nak -> handleNakPacketFromLocalSrtServer(packet)
                else -> sendPacketOnLatestConnection(packet)
            }
        }
    }

    private fun handleAckPacketFromLocalSrtServer(packet: ByteArray) {
        sendPacketOnAllConnections(packet)
        if (packet.size < 20) {
            return
        }
        nakPacket.removeUpTo(getSrtSequenceNumber(packet.copyOfRange(16, 20)))
    }

    private fun handleNakPacketFromLocalSrtServer(packet: ByteArray) {
        sendPacketOnAllConnections(packet)
        if (packet.size < 16) {
            return
        }
        processSrtNak(packet) { sn ->
            nakPacket.add(sn)
        }
        nakPacket.setLatestTimestamp(packet.getUInt32Be(8))
        nakPacket.setLatestNakDestinationSrtSocketId(packet.getUInt32Be(12))
    }

    private fun sendPacketOnLatestConnection(packet: ByteArray) {
        latestConnection?.sendPacket(packet)
    }

    private fun sendPacketOnAllConnections(packet: ByteArray) {
        for (connection in connections) {
            connection.sendPacket(packet)
        }
    }

    fun handlePeriodicTimer(): Boolean {
        val now = Instant.now()
        var index = 0
        while (index < connections.size) {
            val connection = connections[index]
            if (connection.isActive(System.nanoTime())) {
                index += 1
            } else {
                connection.stop()
                connections.removeAt(index)
                Log.i(tag, "srtla-server-client: Removed connection. Using ${connections.size} connection(s)")
            }
        }
        val elapsed = Duration.between(createdAt, now).toNanos() / 1_000_000_000.0
        return connections.isEmpty() && elapsed > clientRemoveTimeout
    }

    override fun handlePacketFromSrtClient(connection: SrtlaServerClientConnection, packet: ByteArray) {
        if (isSrtDataPacket(packet)) {
            nakPacket.remove(getSrtSequenceNumber(packet))
            dataPacketsToSend.add(packet)
            val now = Instant.now()
            if (Duration.between(latestFlushDataPacketsTime, now).toMillis() > 25) {
                val socket = localSrtServerConnection
                val packets = dataPacketsToSend.toList()
                if (socket != null) {
                    scope.launch {
                        runCatching {
                            val output = socket.getOutputStream()
                            for (dataPacket in packets) {
                                output.write(dataPacket)
                            }
                            output.flush()
                        }.onFailure {
                            android.util.Log.i(tag, "srtla-server-client: Send $it")
                        }
                    }
                }
                dataPacketsToSend.clear()
                latestFlushDataPacketsTime = now
            }
        } else {
            val socket = localSrtServerConnection
            if (socket != null) {
                scope.launch {
                    runCatching {
                        val output = socket.getOutputStream()
                        output.write(packet)
                        output.flush()
                    }.onFailure {
                        android.util.Log.i(tag, "srtla-server-client: Send $it")
                    }
                }
            }
        }
        latestConnection = connection
    }
}

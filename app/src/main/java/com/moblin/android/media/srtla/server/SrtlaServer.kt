package com.moblin.android.media.srtla.server

import android.util.Log
import com.moblin.android.media.MediaSample
import com.moblin.android.media.haishinkit.util.Atomic
import com.moblin.android.media.haishinkit.util.BitrateStats
import com.moblin.android.media.haishinkit.util.BitrateStatsInstant
import com.moblin.android.media.srtla.common.SrtlaPacketType
import com.moblin.android.media.srtla.common.createSrtlaPacket
import com.moblin.android.media.srtla.common.getSrtControlPacketType
import com.moblin.android.media.srtla.common.isSrtDataPacket
import com.moblin.android.media.srtla.common.srtControlTypeSize
import com.moblin.android.various.SimpleTimer
import com.moblin.android.various.settings.SettingsSrtlaServer
import com.moblin.android.various.settings.SettingsStreamColorRange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.security.SecureRandom
import java.util.UUID

val srtlaServerQueue = CoroutineScope(Dispatchers.IO)
private const val periodicTimerTimeout = 3.0
private const val TAG = "SrtlaServer"
private val secureRandom = SecureRandom()

private fun randomBytes(length: Int): ByteArray {
    val bytes = ByteArray(length)
    secureRandom.nextBytes(bytes)
    return bytes
}

private fun groupIdKey(groupId: ByteArray): String {
    return String(groupId, Charsets.ISO_8859_1)
}

interface SrtlaServerDelegate {
    fun srtlaServerOnClientStart(cameraId: UUID, name: String)
    fun srtlaServerOnClientStop(cameraId: UUID, name: String)
    fun srtlaServerOnVideoBuffer(cameraId: UUID, sampleBuffer: MediaSample)
    fun srtlaServerOnAudioBuffer(cameraId: UUID, sampleBuffer: MediaSample)
}

class SrtlaServerConnection(val socket: DatagramSocket, val address: InetSocketAddress) {
    fun start() {
    }

    fun send(packet: ByteArray) {
        socket.send(DatagramPacket(packet, packet.size, address))
    }
}

class SrtlaServer(
    settings: SettingsSrtlaServer,
    val delegate: SrtlaServerDelegate,
    timecodesEnabled: Boolean,
    softwareDecoding: Boolean,
    colorRange: SettingsStreamColorRange,
) {
    private var listener: DatagramSocket? = null
    private var clients: MutableMap<String, SrtlaServerClient> = mutableMapOf()
    val settings: SettingsSrtlaServer = settings.clone()
    private val srtServer: SrtServer
    private val srtServerNoSrtlaPatches: SrtServer
    private val periodicTimer = SimpleTimer(Dispatchers.IO)
    val bitrateStats: Atomic<BitrateStats> = Atomic(BitrateStats())
    private var numberOfClients: Atomic<Int> = Atomic(0)
    var connectedStreamIds: Atomic<List<String>> = Atomic<List<String>>(listOf())
    private var connections: MutableMap<InetSocketAddress, SrtlaServerConnection> = mutableMapOf()

    init {
        srtServer = SrtServer(
            timecodesEnabled = timecodesEnabled,
            softwareDecoding = softwareDecoding,
            colorRange = colorRange,
            port = settings.srtlaSrtPort(),
            srtlaPatches = true
        )
        srtServerNoSrtlaPatches = SrtServer(
            timecodesEnabled = timecodesEnabled,
            softwareDecoding = softwareDecoding,
            colorRange = colorRange,
            port = settings.srtPort,
            srtlaPatches = false
        )
        srtServer.srtlaServer = this
        srtServerNoSrtlaPatches.srtlaServer = this
    }

    fun start() {
        srtlaServerQueue.launch {
            srtServer.start()
            srtServerNoSrtlaPatches.start()
            startListener()
            startPeriodicTimer()
        }
    }

    fun stop() {
        srtlaServerQueue.launch {
            stopPeriodicTimer()
            stopListener()
            srtServer.stop()
            srtServerNoSrtlaPatches.stop()
        }
    }

    fun isStreamConnected(streamId: String): Boolean {
        return connectedStreamIds.value.contains(streamId)
    }

    fun updateStats(): BitrateStatsInstant {
        return bitrateStats.mutate { it.value.update() }
    }

    fun getNumberOfClients(): Int {
        return numberOfClients.value
    }

    fun clientConnected(cameraId: UUID, name: String) {
        numberOfClients.mutate { it.value += 1 }
        delegate.srtlaServerOnClientStart(cameraId, name)
    }

    fun clientDisconnected(cameraId: UUID, name: String) {
        delegate.srtlaServerOnClientStop(cameraId, name)
        numberOfClients.mutate { it.value -= 1 }
    }

    private fun startPeriodicTimer() {
        periodicTimer.startPeriodic(periodicTimerTimeout) { handlePeriodicTimer() }
    }

    private fun stopPeriodicTimer() {
        periodicTimer.stop()
    }

    private fun handlePeriodicTimer() {
        val groupIdsToRemove = mutableListOf<String>()
        for ((groupId, client) in clients) {
            if (client.handlePeriodicTimer()) {
                client.stop()
                groupIdsToRemove.add(groupId)
                Log.d(TAG, "srtla-server: Removed client")
            }
        }
        for (groupId in groupIdsToRemove) {
            clients.remove(groupId)
        }
    }

    private fun startListener() {
        Log.i(TAG, "srtla-server: Setup listener")
        val srtlaPort = settings.srtlaPort.toInt()
        if (srtlaPort <= 0 || srtlaPort > 65535) {
            Log.i(TAG, "srtla-server: Bad listener port ${settings.srtlaPort}")
            return
        }
        val socket = try {
            val socket = DatagramSocket(null)
            socket.reuseAddress = true
            socket.bind(InetSocketAddress(srtlaPort))
            socket
        } catch (e: Exception) {
            Log.i(TAG, "srtla-server: Failed to create listener with error $e")
            return
        }
        listener = socket
        handleListenerStateChange("ready")
        srtlaServerQueue.launch {
            receiveLoop(socket)
        }
    }

    private fun stopListener() {
        listener?.close()
        listener = null
    }

    private fun handleListenerStateChange(state: String) {
        Log.d(TAG, "srtla-server: State change to $state")
        when (state) {
            "ready" -> Log.d(TAG, "srtla-server: Listening on port ${listener?.localPort ?: 0}")
            else -> {}
        }
    }

    private fun receiveLoop(socket: DatagramSocket) {
        val buffer = ByteArray(65536)
        while (!socket.isClosed) {
            val datagram = DatagramPacket(buffer, buffer.size)
            try {
                socket.receive(datagram)
            } catch (e: Exception) {
                if (socket.isClosed) {
                    return
                }
                Log.i(TAG, "srtla-server: Error $e")
                continue
            }
            val address = datagram.socketAddress as? InetSocketAddress ?: continue
            val connection = connections[address] ?: SrtlaServerConnection(socket, address).also {
                connections[address] = it
                handleNewListenerConnection(it)
            }
            val packet = datagram.data.copyOfRange(datagram.offset, datagram.offset + datagram.length)
            receivePacket(connection = connection, packet = packet)
        }
    }

    private fun handleNewListenerConnection(connection: SrtlaServerConnection) {
        Log.d(TAG, "srtla-server: Client ${connection.address} connected")
        connection.start()
    }

    private fun receivePacket(connection: SrtlaServerConnection, packet: ByteArray): Boolean {
        if (packet.isEmpty()) {
            return false
        }
        return handlePacket(connection = connection, packet = packet)
    }

    private fun handlePacket(connection: SrtlaServerConnection, packet: ByteArray): Boolean {
        if (packet.size < srtControlTypeSize) {
            Log.i(TAG, "srtla-server: Packet too short (${packet.size}).")
            return false
        }
        if (!isSrtDataPacket(packet)) {
            return handleControlPacket(connection = connection, packet = packet)
        }
        return false
    }

    private fun handleControlPacket(connection: SrtlaServerConnection, packet: ByteArray): Boolean {
        val type = getSrtControlPacketType(packet)
        val srtlaType = SrtlaPacketType.fromRawValue(type) ?: return false
        return handleSrtlaControlPacket(connection = connection, type = srtlaType, packet = packet)
    }

    private fun handleSrtlaControlPacket(connection: SrtlaServerConnection,
                                         type: SrtlaPacketType,
                                         packet: ByteArray): Boolean
    {
        when (type) {
            SrtlaPacketType.reg1 -> handleSrtlaReg1(connection = connection, packet = packet)
            SrtlaPacketType.reg2 -> return handleSrtlaReg2(connection = connection, packet = packet)
            else -> Log.i(TAG, "srtla-server: Discarding srtla control packet $type")
        }
        return false
    }

    private fun handleSrtlaReg1(connection: SrtlaServerConnection, packet: ByteArray) {
        Log.d(TAG, "srtla-server: Got reg 1 (create group)")
        if (packet.size != 258) {
            Log.i(TAG, "srtla-server: Wrong reg 1 packet length ${packet.size}")
            return
        }
        val groupId = packet.copyOfRange(srtControlTypeSize, srtControlTypeSize + 128) +
            randomBytes(128)
        val key = groupIdKey(groupId)
        if (clients[key] != null) {
            return
        }
        clients[key] = SrtlaServerClient(settings.srtlaSrtPort())
        sendSrtlaReg2(connection = connection, groupId = groupId)
    }

    private fun handleSrtlaReg2(connection: SrtlaServerConnection, packet: ByteArray): Boolean {
        Log.d(TAG, "srtla-server: Got reg 2 (register connection)")
        if (packet.size != 258) {
            Log.i(TAG, "srtla-server: Wrong reg 2 packet length ${packet.size}")
            return false
        }
        val groupId = packet.copyOfRange(srtControlTypeSize, packet.size)
        val client = clients[groupIdKey(groupId)]
        if (client == null) {
            Log.i(TAG, "srtla-server: Unknown group id in reg 2 packet.")
            sendSrtlaNgp(connection = connection)
            return false
        }
        Unit
        sendSrtlaReg3(connection = connection)
        return true
    }

    private fun sendSrtlaReg2(connection: SrtlaServerConnection, groupId: ByteArray) {
        Log.d(TAG, "srtla-server: Sending reg 2 (group created)")
        val packet = createSrtlaPacket(SrtlaPacketType.reg2, 258)
        groupId.copyInto(
            packet,
            srtControlTypeSize,
            0,
            minOf(groupId.size, packet.size - srtControlTypeSize)
        )
        sendPacket(connection = connection, packet = packet)
    }

    private fun sendSrtlaReg3(connection: SrtlaServerConnection) {
        Log.d(TAG, "srtla-server: Sending reg 3 (connection registered)")
        val packet = createSrtlaPacket(SrtlaPacketType.reg3, srtControlTypeSize)
        sendPacket(connection = connection, packet = packet)
    }

    private fun sendSrtlaNgp(connection: SrtlaServerConnection) {
        Log.d(TAG, "srtla-server: Sending ngp (no group)")
        val packet = createSrtlaPacket(SrtlaPacketType.regNgp, srtControlTypeSize)
        sendPacket(connection = connection, packet = packet)
    }

    private fun sendPacket(connection: SrtlaServerConnection, packet: ByteArray) {
        connection.send(packet)
    }
}

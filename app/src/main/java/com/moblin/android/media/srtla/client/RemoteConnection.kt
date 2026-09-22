package com.moblin.android.media.srtla.client

import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import com.moblin.android.common.various.sizeFormatter
import com.moblin.android.media.haishinkit.mpeg.MpegTsPacket
import com.moblin.android.media.srtla.common.SrtPacketType
import com.moblin.android.media.srtla.common.SrtlaPacketType
import com.moblin.android.media.srtla.common.createSrtlaPacket
import com.moblin.android.media.srtla.common.getSrtControlPacketType
import com.moblin.android.media.srtla.common.getSrtSequenceNumber
import com.moblin.android.media.srtla.common.isSrtDataPacket
import com.moblin.android.media.srtla.common.isSrtSnAcked
import com.moblin.android.media.srtla.common.processSrtNak
import com.moblin.android.media.srtla.common.srtControlTypeSize
import com.moblin.android.various.SimpleTimer
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.SecureRandom
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "RemoteConnection"

private const val connectionReceiveBatchSize = 25

private const val windowDefault = 20
private const val windowMinimum = 1
private const val windowMaximum = 60
private const val windowStableMinimum = 10
private const val windowStableMaximum = 20
private const val windowMultiply = 1000
private const val windowDecrement = 100
private const val windowIncrement = 30

private val connectionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

private val random = SecureRandom()

private enum class ConnectionState {
    ready,
    failed,
}

interface RemoteConnectionDelegate {
    fun remoteConnectionOnSocketConnected(connection: RemoteConnection)
    fun remoteConnectionOnRegNgp(connection: RemoteConnection)
    fun remoteConnectionOnReg2(groupId: ByteArray)
    fun remoteConnectionOnRegistered()
    fun remoteConnectionPacketHandler(packet: ByteArray)
    fun remoteConnectionOnSrtAck(sn: UInt)
    fun remoteConnectionOnSrtNak(sn: UInt)
    fun remoteConnectionOnSrtlaAck(sn: UInt)
    fun remoteConnectionOnMoblinkReconnect(connection: RemoteConnection)
}

class RemoteConnection(
    var type: Int?,
    private val mpegtsPacketsPerPacket: Int,
    private val packetPadding: Boolean,
    val `interface`: Network?,
    private var networkInterfaces: SrtlaNetworkInterfaces,
    private var priority: Float,
    val relayId: UUID? = null,
    private val relayName: String? = null,
) {
    private var connection: Socket? = null
        set(value) {
            val previous = field
            field = value
            if (previous != null) {
                try {
                    previous.close()
                } catch (e: IOException) {
                    Log.i(TAG, "srtla: $typeString: Close $e")
                }
            }
        }

    private val connectTimer = SimpleTimer(queue = srtlaClientQueue)
    private val keepaliveTimer = SimpleTimer(queue = srtlaClientQueue)
    private var latestReceivedTime: Long = System.nanoTime()
    private val packetsInFlight: MutableSet<UInt> = mutableSetOf()
    private var windowSize: Int = 0
    private var hasFullGroupId: Boolean = false
    private var groupId: ByteArray = ByteArray(0)
    private var state: State = State.idle
        set(value) {
            Log.d(TAG, "srtla: $typeString: State $field -> $value")
            field = value
        }

    private var keepAliveSendBaseTime: Long = System.nanoTime()
    var rtt: Int = 0
    private val dataPacketsToSend: MutableList<ByteArray> = mutableListOf()
    private var totalDataSentByteCount: Long = 0

    private val nullPacket: ByteArray = ByteArray(MpegTsPacket.size).also { packet ->
        packet.setUInt32Be(
            value = (MpegTsPacket.syncByte.toInt().toUInt() shl 24) or (0x1FFFu shl 8) or (1u shl 4),
        )
    }

    var destinationHost: String? = null
        private set
    var destinationPort: Int? = null
        private set

    val typeString: String
        get() = when (type) {
            NetworkCapabilities.TRANSPORT_WIFI -> "WiFi"
            NetworkCapabilities.TRANSPORT_ETHERNET -> {
                networkInterfaces.names[`interface`?.toString() ?: ""] ?: `interface`?.toString() ?: "Ethernet"
            }
            NetworkCapabilities.TRANSPORT_CELLULAR -> "Cellular"
            else -> relayName ?: "Any"
        }

    var delegate: RemoteConnectionDelegate? = null

    override fun finalize() {
        Log.d(TAG, "srtla: $typeString: deinit remote connection")
    }

    fun setPriority(priority: Float) {
        this.priority = priority
    }

    fun start(host: String, port: Int) {
        destinationHost = host
        destinationPort = port
        startInternal()
    }

    private fun startInternal() {
        val host = destinationHost
        val port = destinationPort
        if (state != State.idle || host == null || port == null) {
            return
        }
        Log.i(TAG, "srtla: $typeString: Start with destination $host:$port")
        val socket = try {
            `interface`?.socketFactory?.createSocket() ?: Socket()
        } catch (e: IOException) {
            Log.i(TAG, "srtla: $typeString: Create socket failed $e")
            handleStateUpdate(to = ConnectionState.failed)
            return
        }
        connection = socket
        state = State.socketConnecting
        connectionScope.launch {
            val connected = try {
                socket.connect(InetSocketAddress(host, port), 5000)
                true
            } catch (e: IOException) {
                Log.i(TAG, "srtla: $typeString: Connect failed $e")
                false
            }
            if (connected) {
                receivePackets()
                handleStateUpdate(to = ConnectionState.ready)
            } else {
                handleStateUpdate(to = ConnectionState.failed)
            }
        }
    }

    fun stop(reason: String) {
        val sent = sizeFormatter.format(totalDataSentByteCount)
        Log.d(TAG, "srtla: $typeString: Stop with reason: $reason ($sent sent)")
        connection = null
        cancelAllTimers()
        state = State.idle
    }

    fun score(): Int {
        if (state != State.registered) {
            return -1
        }
        if (type == null) {
            return 1
        } else if (priority == 0f) {
            return -1
        } else {
            val score = windowSize / (packetsInFlight.size + 1)
            if (windowSize > windowStableMaximum * windowMultiply) {
                return (score * priority).toInt()
            } else if (windowSize > windowStableMinimum * windowMultiply) {
                var factor = (windowSize - windowStableMinimum * windowMultiply).toFloat()
                factor /= ((windowStableMaximum - windowStableMinimum) * windowMultiply).toFloat()
                val scaledPriority = 1 + (priority - 1) * factor
                return (score * scaledPriority).toInt()
            } else {
                return score
            }
        }
    }

    fun isEnabled(): Boolean {
        return priority > 0
    }

    fun sendSrtPacket(packet: ByteArray) {
        sendPacket(packet = packet)
    }

    fun flushDataPackets() {
        if (dataPacketsToSend.isNotEmpty()) {
            sendDataPackets()
        }
    }

    fun probe() {
        groupId = randomData(length = 256)
        sendSrtlaReg2()
    }

    fun register(groupId: ByteArray) {
        this.groupId = groupId
        hasFullGroupId = true
        if (state == State.shouldSendRegisterRequest) {
            sendSrtlaReg2()
            state = State.waitForRegisterResponse
        }
    }

    fun sendSrtlaReg1() {
        Log.d(TAG, "srtla: $typeString: Sending reg 1 (create group)")
        groupId = randomData(length = 256)
        val packet = createSrtlaPacket(type = SrtlaPacketType.reg1, length = srtControlTypeSize + groupId.size)
        groupId.copyInto(packet, srtControlTypeSize)
        sendPacket(packet = packet)
    }

    fun handleSrtAckSn(sn ackSn: UInt) {
        packetsInFlight.retainAll { sn -> !isSrtSnAcked(sn = sn, ackSn = ackSn) }
    }

    fun handleSrtNakSn(sn: UInt) {
        if (!packetsInFlight.remove(sn)) {
            return
        }
        windowSize = maxOf(windowSize - windowDecrement, windowMinimum * windowMultiply)
    }

    fun handleSrtlaAckSn(sn: UInt) {
        if (packetsInFlight.remove(sn)) {
            if (packetsInFlight.size * windowMultiply > windowSize) {
                windowSize += windowIncrement - 1
            }
        }
        windowSize = minOf(windowSize + 1, windowMaximum * windowMultiply)
    }

    fun logStatistics() {
        if (state != State.registered) {
            return
        }
        if (type != null) {
            Log.d(
                TAG,
                "srtla: $typeString: Score: ${score()}, In flight: ${packetsInFlight.size}, " +
                    "Window size: $windowSize, Priority: $priority",
            )
        }
    }

    fun getDataSentDelta(): Long? {
        val dataSent = totalDataSentByteCount
        totalDataSentByteCount = 0
        return if (state == State.registered) {
            dataSent
        } else {
            null
        }
    }

    private fun cancelAllTimers() {
        keepaliveTimer.stop()
        connectTimer.stop()
    }

    private fun isMoblink(): Boolean {
        return relayId != null
    }

    private fun handleStateUpdate(to state: ConnectionState) {
        Log.d(TAG, "srtla: $typeString: State change to $state")
        when (state) {
            ConnectionState.ready -> {
                cancelAllTimers()
                connectTimer.startSingleShot(timeout = 5) {
                    reconnect(reason = "Connection timeout")
                }
                latestReceivedTime = System.nanoTime()
                packetsInFlight.clear()
                totalDataSentByteCount = 0
                windowSize = windowDefault * windowMultiply
                if (type == null) {
                    this.state = State.registered
                    connectTimer.stop()
                } else if (this.state == State.shouldSendRegisterRequest || hasFullGroupId) {
                    sendSrtlaReg2()
                    this.state = State.waitForRegisterResponse
                } else {
                    this.state = State.shouldSendRegisterRequest
                }
                delegate?.remoteConnectionOnSocketConnected(connection = this)
            }

            ConnectionState.failed -> {
                reconnect(reason = "Connection failed")
            }
        }
    }

    private fun reconnect(reason: String) {
        if (isMoblink()) {
            delegate?.remoteConnectionOnMoblinkReconnect(connection = this)
        } else {
            stop(reason = reason)
            startInternal()
        }
    }

    private fun receivePackets() {
        val socket = connection ?: return
        connectionScope.launch {
            val input = try {
                socket.getInputStream()
            } catch (e: IOException) {
                Log.i(TAG, "srtla: $typeString: Receive $e")
                return@launch
            }
            val buffer = ByteArray(2048)
            var index = 0
            while (index < connectionReceiveBatchSize) {
                val count = try {
                    input.read(buffer)
                } catch (e: IOException) {
                    Log.i(TAG, "srtla: $typeString: Receive $e")
                    return@launch
                }
                if (count > 0) {
                    handlePacketFromClient(packet = buffer.copyOf(count))
                }
                if (count < 0) {
                    Log.i(TAG, "srtla: $typeString: Receive end of stream")
                    return@launch
                }
                index += 1
            }
            receivePackets()
        }
    }

    private fun sendPacket(packet: ByteArray) {
        if (isSrtDataPacket(packet = packet)) {
            packetsInFlight.add(getSrtSequenceNumber(packet = packet))
            var numberOfMpegTsPackets = (packet.size - 16) / MpegTsPacket.size
            if (packetPadding && numberOfMpegTsPackets < mpegtsPacketsPerPacket) {
                var paddedPacket = packet
                while (numberOfMpegTsPackets < mpegtsPacketsPerPacket) {
                    paddedPacket += nullPacket
                    numberOfMpegTsPackets += 1
                }
                sendDataPacketInternal(packet = paddedPacket)
                totalDataSentByteCount += paddedPacket.size.toLong()
            } else {
                sendDataPacketInternal(packet = packet)
                totalDataSentByteCount += packet.size.toLong()
            }
        } else {
            sendControlPacketInternal(packet = packet)
        }
    }

    private fun sendControlPacketInternal(packet: ByteArray) {
        connectionScope.launch {
            writePacket(packet = packet)
        }
    }

    private fun sendDataPacketInternal(packet: ByteArray) {
        dataPacketsToSend.add(packet)
        if (dataPacketsToSend.size > 15) {
            sendDataPackets()
        }
    }

    private fun sendDataPackets() {
        val packets = dataPacketsToSend.toList()
        dataPacketsToSend.clear()
        connectionScope.launch {
            for (packet in packets) {
                writePacket(packet = packet)
            }
        }
    }

    private fun writePacket(packet: ByteArray) {
        val socket = connection ?: return
        try {
            synchronized(socket) {
                val output = socket.getOutputStream()
                output.write(packet)
                output.flush()
            }
        } catch (e: IOException) {
            Log.i(TAG, "srtla: $typeString: Send $e")
        }
    }

    private fun sendSrtlaReg2() {
        Log.d(TAG, "srtla: $typeString: Sending reg 2 (register connection)")
        val packet = createSrtlaPacket(type = SrtlaPacketType.reg2, length = srtControlTypeSize + groupId.size)
        groupId.copyInto(packet, srtControlTypeSize)
        sendPacket(packet = packet)
    }

    private fun sendSrtlaKeepalive() {
        val packet = createSrtlaPacket(type = SrtlaPacketType.keepalive, length = srtControlTypeSize + 8)
        packet.setInt64Be(value = getKeepAliveTime(), offset = srtControlTypeSize)
        sendPacket(packet = packet)
    }

    private fun getKeepAliveTime(): Long {
        return (System.nanoTime() - keepAliveSendBaseTime) / 1_000_000
    }

    private fun handleSrtAck(packet: ByteArray) {
        if (packet.size < 20) {
            return
        }
        delegate?.remoteConnectionOnSrtAck(sn = getSrtSequenceNumber(packet = packet.copyOfRange(16, 20)))
    }

    private fun handleSrtNak(packet: ByteArray) {
        processSrtNak(packet = packet) { sn ->
            delegate?.remoteConnectionOnSrtNak(sn = sn)
        }
    }

    private fun handleSrtlaKeepalive(packet: ByteArray) {
        if (packet.size < 10) {
            return
        }
        val sendTime = packet.getInt64Be(offset = srtControlTypeSize)
        rtt = (getKeepAliveTime() - sendTime).coerceIn(0L, 10000L).toInt()
    }

    private fun handleSrtlaAck(packet: ByteArray) {
        if (packet.size % 4 != 0) {
            return
        }
        var offset = 4
        while (offset < packet.size) {
            delegate?.remoteConnectionOnSrtlaAck(sn = packet.getUInt32Be(offset = offset))
            offset += 4
        }
    }

    private fun handleSrtlaReg2(packet: ByteArray) {
        Log.d(TAG, "srtla: $typeString: Got reg 2 (group created)")
        if (packet.size != 258) {
            Log.i(TAG, "srtla: $typeString: Wrong reg 2 packet length ${packet.size}")
            return
        }
        if (groupId.size != 256) {
            return
        }
        val length = groupId.size / 2
        val packetGroupId = packet.copyOfRange(srtControlTypeSize, srtControlTypeSize + length)
        val localGroupId = groupId.copyOfRange(0, length)
        if (!packetGroupId.contentEquals(localGroupId)) {
            Log.i(TAG, "srtla: $typeString: Wrong group id in reg 2")
            return
        }
        delegate?.remoteConnectionOnReg2(groupId = packet.copyOfRange(srtControlTypeSize, packet.size))
    }

    private fun handleSrtlaReg3() {
        Log.d(TAG, "srtla: $typeString: Got reg 3 (connection registered)")
        if (state != State.waitForRegisterResponse) {
            return
        }
        state = State.registered
        delegate?.remoteConnectionOnRegistered()
        connectTimer.stop()
        keepaliveTimer.startPeriodic(interval = 1) {
            val now = System.nanoTime()
            sendSrtlaKeepalive()
            if (latestReceivedTime < now - 5_000_000_000L) {
                reconnect(reason = "No packet received in 5 seconds")
            }
        }
    }

    private fun handleSrtlaRegErr() {
        Log.d(TAG, "srtla: $typeString: Register error")
    }

    private fun handleSrtlaRegNgp() {
        Log.d(TAG, "srtla: $typeString: Register no group")
        delegate?.remoteConnectionOnRegNgp(connection = this)
    }

    private fun handleSrtlaRegNak() {
        Log.d(TAG, "srtla: $typeString: Register nak")
    }

    private fun handleSrtlaControlPacket(type: SrtlaPacketType, packet: ByteArray) {
        when (type) {
            SrtlaPacketType.keepalive -> handleSrtlaKeepalive(packet = packet)
            SrtlaPacketType.ack -> handleSrtlaAck(packet = packet)
            SrtlaPacketType.reg1 -> Log.i(TAG, "srtla: $typeString: Received register 1 packet")
            SrtlaPacketType.reg2 -> handleSrtlaReg2(packet = packet)
            SrtlaPacketType.reg3 -> handleSrtlaReg3()
            SrtlaPacketType.regErr -> handleSrtlaRegErr()
            SrtlaPacketType.regNgp -> handleSrtlaRegNgp()
            SrtlaPacketType.regNak -> handleSrtlaRegNak()
            else -> Unit
        }
    }

    private fun handleSrtControlPacket(type: SrtPacketType, packet: ByteArray) {
        if (packet.size < 16) {
            return
        }
        when (type) {
            SrtPacketType.ack -> handleSrtAck(packet = packet)
            SrtPacketType.nak -> handleSrtNak(packet = packet)
            else -> Unit
        }
    }

    private fun handleControlPacket(packet: ByteArray) {
        val type = getSrtControlPacketType(packet = packet)
        val srtlaPacketType = SrtlaPacketType.fromRawValue(type)
        if (srtlaPacketType != null) {
            handleSrtlaControlPacket(type = srtlaPacketType, packet = packet)
        } else {
            val srtPacketType = SrtPacketType.fromRawValue(type)
            if (srtPacketType != null) {
                handleSrtControlPacket(type = srtPacketType, packet = packet)
            }
            delegate?.remoteConnectionPacketHandler(packet = packet)
        }
    }

    private fun handleDataPacket(packet: ByteArray) {
        delegate?.remoteConnectionPacketHandler(packet = packet)
    }

    private fun handlePacketFromClient(packet: ByteArray) {
        if (packet.size < srtControlTypeSize) {
            Log.i(TAG, "srtla: $typeString: Packet too short (${packet.size}) bytes.")
            return
        }
        latestReceivedTime = System.nanoTime()
        if (isSrtDataPacket(packet = packet)) {
            handleDataPacket(packet = packet)
        } else {
            handleControlPacket(packet = packet)
        }
    }
}

private fun ByteArray.getUInt32Be(offset: Int): UInt {
    return ByteBuffer.wrap(this).order(ByteOrder.BIG_ENDIAN).getInt(offset).toUInt()
}

private fun ByteArray.setUInt32Be(value: UInt, offset: Int = 0) {
    ByteBuffer.wrap(this).order(ByteOrder.BIG_ENDIAN).putInt(offset, value.toInt())
}

private fun ByteArray.getInt64Be(offset: Int): Long {
    return ByteBuffer.wrap(this).order(ByteOrder.BIG_ENDIAN).getLong(offset)
}

private fun ByteArray.setInt64Be(value: Long, offset: Int) {
    ByteBuffer.wrap(this).order(ByteOrder.BIG_ENDIAN).putLong(offset, value)
}

private fun randomData(length: Int): ByteArray {
    return ByteArray(length).also { random.nextBytes(it) }
}

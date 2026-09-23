package com.moblin.android.media.srtla.client

import android.util.Log
import com.moblin.android.common.various.getInt64Be
import com.moblin.android.common.various.getUInt32Be
import com.moblin.android.common.various.setInt64Be
import com.moblin.android.common.various.setUInt32Be
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
import com.moblin.android.platform.network.NWConnection
import com.moblin.android.platform.network.NWEndpoint
import com.moblin.android.platform.network.NWInterface
import com.moblin.android.platform.network.NWParameters
import com.moblin.android.various.SimpleTimer
import java.util.UUID
import kotlin.random.Random

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
    type: NWInterface.InterfaceType?,
    mpegtsPacketsPerPacket: Int,
    packetPadding: Boolean,
    `interface`: NWInterface?,
    networkInterfaces: SrtlaNetworkInterfaces,
    priority: Float,
    relayId: UUID? = null,
    relayName: String? = null,
) {
    private enum class State {
        idle,
        socketConnecting,
        shouldSendRegisterRequest,
        waitForRegisterResponse,
        registered,
    }

    var type: NWInterface.InterfaceType? = type

    private var connection: NWConnection? = null
        set(value) {
            val oldValue = field
            field = value
            oldValue?.stateUpdateHandler = null
            oldValue?.forceCancel()
        }

    private val connectTimer = SimpleTimer(queue = srtlaClientQueue)
    private val keepaliveTimer = SimpleTimer(queue = srtlaClientQueue)
    private var latestReceivedTime = System.nanoTime()
    private var packetsInFlight: HashSet<UInt> = HashSet()
    private var windowSize: Int = 0
    private var hasFullGroupId: Boolean = false
    private var groupId = ByteArray(0)
    private var priority: Float = priority
    private var state = State.idle
        set(value) {
            val oldValue = field
            field = value
            Log.d(TAG, "srtla: $typeString: State $oldValue -> $value")
        }

    private var keepAliveSendBaseTime = System.nanoTime()
    var rtt: Int = 0
    val `interface`: NWInterface? = `interface`
    private val dataPacketsToSend: MutableList<ByteArray> = mutableListOf()
    private var totalDataSentByteCount: Long = 0

    private val nullPacket: ByteArray = ByteArray(MpegTsPacket.size).also { packet ->
        packet.setUInt32Be(
            value = (MpegTsPacket.syncByte.toUInt() shl 24) or (0x1FFFu shl 8) or (0x1u shl 4),
        )
    }

    var destinationHost: NWEndpoint.Host? = null
        private set
    var destinationPort: NWEndpoint.Port? = null
        private set
    private val mpegtsPacketsPerPacket: Int = mpegtsPacketsPerPacket
    private val packetPadding: Boolean = packetPadding
    val typeString: String
        get() = when (type) {
            NWInterface.InterfaceType.wifi -> "WiFi"
            NWInterface.InterfaceType.wiredEthernet ->
                networkInterfaces.names[`interface`?.name ?: ""] ?: `interface`?.name ?: "Ethernet"
            NWInterface.InterfaceType.cellular -> "Cellular"
            else -> relayName ?: "Any"
        }

    val relayId: UUID? = relayId
    private val relayName: String? = relayName

    var delegate: RemoteConnectionDelegate? = null
    private var networkInterfaces: SrtlaNetworkInterfaces = networkInterfaces

    fun setPriority(priority: Float) {
        this.priority = priority
    }

    fun start(host: NWEndpoint.Host, port: NWEndpoint.Port) {
        destinationHost = host
        destinationPort = port
        startInternal()
    }

    private fun startInternal() {
        val destinationHost = destinationHost
        val destinationPort = destinationPort
        if (state != State.idle || destinationHost == null || destinationPort == null) {
            return
        }
        Log.i(TAG, "srtla: $typeString: Start with destination $destinationHost:$destinationPort")
        val params = NWParameters.dtls(null)
        params.prohibitExpensivePaths = false
        params.requiredInterface = `interface`
        connection = NWConnection(host = destinationHost, port = destinationPort, using = params)
        connection!!.stateUpdateHandler = ::handleStateUpdate
        connection!!.start(queue = srtlaClientQueue)
        receivePackets()
        state = State.socketConnecting
    }

    fun stop(reason: String) {
        val sent = sizeFormatter.string(fromByteCount = totalDataSentByteCount)
        Log.d(TAG, "srtla: $typeString: Stop with reason: $reason ($sent sent)")
        connection?.forceCancel()
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
                return (score.toFloat() * priority).toInt()
            } else if (windowSize > windowStableMinimum * windowMultiply) {
                var factor = (windowSize - windowStableMinimum * windowMultiply).toFloat()
                factor /= ((windowStableMaximum - windowStableMinimum) * windowMultiply).toFloat()
                val scaledPriority = 1 + (priority - 1) * factor
                return (score.toFloat() * scaledPriority).toInt()
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
        groupId = Random.nextBytes(256)
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
        groupId = Random.nextBytes(256)
        val packet = createSrtlaPacket(type = SrtlaPacketType.reg1, length = srtControlTypeSize + groupId.size)
        groupId.copyInto(packet, srtControlTypeSize)
        sendPacket(packet = packet)
    }

    fun handleSrtAckSn(sn: UInt) {
        val ackSn = sn
        packetsInFlight.retainAll { packetSn -> !isSrtSnAcked(sn = packetSn, ackSn = ackSn) }
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
        try {
            if (state != State.registered) {
                return null
            }
            return totalDataSentByteCount
        } finally {
            totalDataSentByteCount = 0
        }
    }

    private fun cancelAllTimers() {
        keepaliveTimer.stop()
        connectTimer.stop()
    }

    private fun isMoblink(): Boolean {
        return relayId != null
    }

    private fun handleStateUpdate(state: NWConnection.State) {
        Log.d(TAG, "srtla: $typeString: State change to $state")
        when (state) {
            NWConnection.State.ready -> {
                cancelAllTimers()
                connectTimer.startSingleShot(timeout = 5.0) {
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
            is NWConnection.State.failed -> reconnect(reason = "Connection failed")
            else -> Unit
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
        connection?.batch {
            for (index in 0 until connectionReceiveBatchSize) {
                connection?.receiveMessage { packet, _, _, error ->
                    if (packet != null && packet.isNotEmpty()) {
                        handlePacketFromClient(packet = packet)
                    }
                    if (index != connectionReceiveBatchSize - 1) {
                        return@receiveMessage
                    }
                    if (error != null) {
                        Log.i(TAG, "srtla: $typeString: Receive $error")
                        return@receiveMessage
                    }
                    receivePackets()
                }
            }
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
        connection?.send(content = packet, completion = NWConnection.SendCompletion.idempotent)
    }

    private fun sendDataPacketInternal(packet: ByteArray) {
        dataPacketsToSend.add(packet)
        if (dataPacketsToSend.size > 15) {
            sendDataPackets()
        }
    }

    private fun sendDataPackets() {
        connection?.batch {
            for (packet in dataPacketsToSend) {
                connection?.send(content = packet, completion = NWConnection.SendCompletion.idempotent)
            }
        }
        dataPacketsToSend.clear()
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
        return (System.nanoTime() - keepAliveSendBaseTime) / 1_000_000L
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
        for (offset in 4 until packet.size step 4) {
            delegate?.remoteConnectionOnSrtlaAck(sn = packet.getUInt32Be(offset = offset))
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
        val groupIdLength = groupId.size / 2
        val packetGroupId = packet.copyOfRange(srtControlTypeSize, srtControlTypeSize + groupIdLength)
        if (!packetGroupId.contentEquals(groupId.copyOfRange(0, groupIdLength))) {
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
        keepaliveTimer.startPeriodic(interval = 1.0) {
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
            Log.i(TAG, "srtla: $typeString: Packet too short (${packet.size} bytes.")
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

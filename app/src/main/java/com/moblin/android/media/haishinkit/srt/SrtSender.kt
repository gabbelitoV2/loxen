package com.moblin.android.media.haishinkit.srt

import android.util.Log
import com.moblin.android.media.haishinkit.util.Atomic
import com.moblin.android.media.haishinkit.util.ByteReader
import com.moblin.android.media.haishinkit.util.ByteWriter
import com.moblin.android.media.srtla.client.srtlaClientQueue
import com.moblin.android.media.srtla.common.isSrtSnAcked
import com.moblin.android.media.srtla.common.isSrtSnRange
import com.moblin.android.various.SimpleTimer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.random.Random

private const val TAG = "SrtSender"

private val srtSenderScope = CoroutineScope(srtlaClientQueue)

private const val srtDataPacketHeaderSize = 16
private val srtHandshakeVersion4: UInt = 4u
private val srtHandshakeVersion5: UInt = 5u
private val srtDestinationSocket: UInt = 0u
private val srtMaximumTransmissionUnitSize: UInt = 1500u
private val srtMaximumFlowWindowSizeInPackets: UInt = 8192u
private val srtSocketId: UInt = Random.nextLong(1L, 1L shl 32).toUInt()
private val srtIpUdpHeaderSize: ULong = 28uL

private class SrtSenderException(message: String) : Exception(message)

private class SrtClock {
    private val startTime = System.nanoTime()

    fun timestamp(): UInt {
        return timestamp(System.nanoTime())
    }

    fun timestamp(now: Long): UInt {
        return ((now - startTime) / 1000L).toUInt()
    }
}

private fun createCommonControlPacketHeader(type: ControlPacketType,
                                            typeSpecificInformation: UInt,
                                            timestamp: UInt,
                                            destinationSocketId: UInt): ByteArray
{
    val writer = ByteWriter()
    writer.writeUInt16((0x8000u or type.rawValue).toUShort())
    writer.writeUInt16(0u.toUShort())
    writer.writeUInt32(typeSpecificInformation)
    writer.writeUInt32(timestamp)
    writer.writeUInt32(destinationSocketId)
    return writer.data
}

private fun ByteArray.writeUInt32(value: UInt, offset: Int) {
    this[offset] = (value shr 24).toByte()
    this[offset + 1] = (value shr 16).toByte()
    this[offset + 2] = (value shr 8).toByte()
    this[offset + 3] = value.toByte()
}

private class AckAckPacket {
    var data: ByteArray

    init {
        data = createCommonControlPacketHeader(ControlPacketType.ackack, 0u, 0u, 0u)
        data += ByteArray(4)
    }

    fun update(ackNumber: UInt, timestamp: UInt) {
        data.writeUInt32(ackNumber, 4)
        data.writeUInt32(timestamp, 8)
    }

    fun update(destinationSocketId: UInt) {
        data.writeUInt32(destinationSocketId, 12)
    }
}

private class KeepAlivePacket {
    var data: ByteArray

    init {
        data = createCommonControlPacketHeader(ControlPacketType.keepAlive, 0u, 0u, 0u)
    }

    fun updateTimestamp(timestamp: UInt) {
        data.writeUInt32(timestamp, 8)
    }

    fun updateDestinationSocketId(destinationSocketId: UInt) {
        data.writeUInt32(destinationSocketId, 12)
    }
}

private enum class SrtSenderState {
    connecting,
    connected,
    disconnected,
}

private class CommonControlPacketHeader(reader: ByteReader) {
    val controlType: ControlPacketType
    val typeSpecificInformation: UInt
    val timestamp: UInt
    val destinationSrtSocketId: UInt

    init {
        var value = reader.readUInt16()
        value = (value.toUInt() and 0x7FFFu).toUShort()
        controlType = ControlPacketType.fromRawValue(value.toUInt())
            ?: throw SrtSenderException("Unsupported packet type $value")
        reader.readUInt16()
        typeSpecificInformation = reader.readUInt32()
        timestamp = reader.readUInt32()
        destinationSrtSocketId = reader.readUInt32()
    }
}

class SrtDataPacket(payload: ByteArray) {
    internal var data: ByteArray = ByteArray(srtDataPacketHeaderSize + payload.size)
    internal var sequenceNumber: UInt = 0u
    internal var createdAt: Long = System.nanoTime()
    internal var retransmittedAt: Long? = null
    var containsAudio: Boolean = false

    init {
        payload.copyInto(data, srtDataPacketHeaderSize)
    }

    internal fun setHeader(sequenceNumber: UInt,
                           now: Long,
                           timestamp: UInt,
                           destinationSrtSocketId: UInt)
    {
        this.sequenceNumber = sequenceNumber
        createdAt = now
        data.writeUInt32(sequenceNumber, 0)
        data.writeUInt32(0xE000_0001u, 4)
        data.writeUInt32(timestamp, 8)
        data.writeUInt32(destinationSrtSocketId, 12)
    }

    internal fun setRetransmissionBit() {
        data[4] = (data[4].toInt() or 0x4).toByte()
    }
}

interface SrtSenderDelegate {
    fun srtSenderConnected()
    fun srtSenderDisconnected()
    fun srtSenderOutput(packet: ByteArray)
}

private enum class ControlPacketType(val rawValue: UInt) {
    handshake(0u),
    keepAlive(1u),
    ack(2u),
    nak(3u),
    shutdown(5u),
    ackack(6u);

    companion object {
        fun fromRawValue(value: UInt): ControlPacketType? {
            return entries.firstOrNull { it.rawValue == value }
        }
    }
}

private enum class HandshakeType(val rawValue: UInt) {
    conclusion(0xFFFF_FFFFu),
    induction(0x0000_0001u);

    companion object {
        fun fromRawValue(value: UInt): HandshakeType? {
            return entries.firstOrNull { it.rawValue == value }
        }
    }
}

class SrtSender(private val streamId: String?,
                private val latency: UShort,
                private val experimental: Boolean)
{
    var delegate: SrtSenderDelegate? = null
    private var nextSequenceNumber: UInt = Random.nextInt(0, 10000).toUInt()
    private var peerDestinationSrtSocketId: UInt = 0u
    private var packetsToSend: ArrayDeque<SrtDataPacket> = ArrayDeque()
    private var packetsInFlight: ArrayDeque<SrtDataPacket> = ArrayDeque()
    private var packetsInFlightBySequenceNumber: MutableMap<UInt, SrtDataPacket> = mutableMapOf()
    private var audioSequenceNumbersToRetransmit: MutableSet<UInt> = linkedSetOf()
    private var videoSequenceNumbersToRetransmit: MutableSet<UInt> = linkedSetOf()
    private var state: SrtSenderState = SrtSenderState.connecting
    private var performanceData: Atomic<SrtPerformanceData> = Atomic(SrtPerformanceData(
        pktRetransTotal = 0,
        pktRecvNakTotal = 0,
        pktSndDropTotal = 0,
        pktFlightSize = 0,
        msRtt = 0.0,
        pktSndBuf = 0,
        mbpsSendRate = 0.0,
    ))
    private var numberOfBytesSent: ULong = 0uL
    private var latestNumberOfBytesSentTime = System.nanoTime()
    private var pktRetransTotal: Int = 0
    private var pktRecvNakTotal: Int = 0
    private var pktSndDropTotal: Int = 0
    private var rttUs: UInt = 0u
    private var mbpsSendRate: Double = 0.0
    private var latestOutputPacketsTime = System.nanoTime()
    private val ackAckPacket = AckAckPacket()
    private val keepAlivePacket = KeepAlivePacket()
    private var latestReceivedPacketTime = System.nanoTime()
    private val packetsInFlightDropThreshold: Long
    private val packetsToSendDropThreshold: Long
    private var clock = SrtClock()
    private val connectTimer = SimpleTimer(srtlaClientQueue)

    init {
        val latencyUs = 1000L * latency.toLong()
        if (experimental) {
            packetsInFlightDropThreshold = latencyUs * 2 / 3 * 1000L
            packetsToSendDropThreshold = latencyUs * 4 / 5 * 1000L
        } else {
            packetsInFlightDropThreshold = latencyUs * 3 / 2 * 1000L
            packetsToSendDropThreshold = latencyUs * 5 / 4 * 1000L
        }
    }

    fun start() {
        srtSenderScope.launch {
            clock = SrtClock()
            latestReceivedPacketTime = System.nanoTime()
            setState(SrtSenderState.connecting)
            outputPacket(createInductionHandshakePacket())
            connectTimer.startSingleShot(5.0) {
                handleConnectTimeout()
            }
        }
    }

    fun stop() {
        srtSenderScope.launch {
            setDisconnected()
        }
    }

    fun newDataPacket(payload: ByteArray): SrtDataPacket {
        return SrtDataPacket(payload)
    }

    fun enqueue(packet: SrtDataPacket, now: Long) {
        if (state != SrtSenderState.connected) {
            return
        }
        if (!experimental) {
            packet.containsAudio = true
        }
        packet.setHeader(
            getNextSequenceNumber(),
            now,
            clock.timestamp(now),
            peerDestinationSrtSocketId
        )
        packetsToSend.addLast(packet)
    }

    fun send(now: Long) {
        if (state != SrtSenderState.connected) {
            return
        }
        outputPackets(now)
        dropOldPackets(now)
        checkIfConnected(now)
    }

    fun input(packet: ByteArray) {
        latestReceivedPacketTime = System.nanoTime()
        try {
            handleControlPacket(packet, latestReceivedPacketTime)
        } catch (e: Exception) {
            Log.i(TAG, "srt-sender: Input error: $e")
        }
    }

    fun getPerformanceData(): SrtPerformanceData? {
        return performanceData.value
    }

    private fun handleConnectTimeout() {
        setDisconnected()
    }

    private fun dropOldPackets(now: Long) {
        while (true) {
            val packet = packetsInFlight.firstOrNull() ?: break
            if (now - packet.createdAt <= packetsInFlightDropThreshold) {
                break
            }
            packetsInFlight.removeFirst()
            packetsInFlightBySequenceNumber.remove(packet.sequenceNumber)
            pktSndDropTotal += 1
        }
        while (true) {
            val packet = packetsToSend.firstOrNull() ?: break
            if (now - packet.createdAt <= packetsToSendDropThreshold) {
                break
            }
            packetsToSend.removeFirst()
            pktSndDropTotal += 1
        }
    }

    private fun setDisconnected() {
        if (state == SrtSenderState.disconnected) {
            return
        }
        connectTimer.stop()
        setState(SrtSenderState.disconnected)
        delegate?.srtSenderDisconnected()
    }

    private fun checkIfConnected(now: Long) {
        if (now - latestReceivedPacketTime <= 5_000_000_000L) {
            return
        }
        setDisconnected()
    }

    private fun outputPackets(now: Long) {
        if (now - latestOutputPacketsTime <= 2_000_000L) {
            return
        }
        latestOutputPacketsTime = now
        var numberOfPacketsToSend = numberOfPacketsToRetransmit() + packetsToSend.size
        numberOfPacketsToSend = maxOf(numberOfPacketsToSend / 10, minOf(numberOfPacketsToSend, 10))
        var numberOfRetransmittedPackets = 0
        for (i in 0 until numberOfPacketsToSend) {
            if (!experimental || numberOfRetransmittedPackets < (numberOfPacketsToSend + 1) / 2) {
                if (retransmitPacketIfNeeded(audioSequenceNumbersToRetransmit, now)) {
                    numberOfRetransmittedPackets += 1
                    continue
                }
                if (retransmitPacketIfNeeded(videoSequenceNumbersToRetransmit, now)) {
                    numberOfRetransmittedPackets += 1
                    continue
                }
            }
            val packet = packetsToSend.removeFirstOrNull() ?: break
            sendPacket(packet)
        }
        updatePerformanceData()
    }

    private fun retransmitPacketIfNeeded(sequenceNumbers: MutableSet<UInt>, now: Long): Boolean {
        while (sequenceNumbers.isNotEmpty()) {
            val packetSequenceNumber = sequenceNumbers.first()
            sequenceNumbers.remove(packetSequenceNumber)
            val packet = packetsInFlightBySequenceNumber[packetSequenceNumber] ?: continue
            val retransmittedAt = packet.retransmittedAt
            if (retransmittedAt != null && now - retransmittedAt < rttUs.toLong() * 1000L) {
                continue
            }
            packet.retransmittedAt = now
            packet.setRetransmissionBit()
            pktRetransTotal += 1
            outputPacket(packet.data)
            return true
        }
        return false
    }

    private fun sendPacket(packet: SrtDataPacket) {
        outputPacket(packet.data)
        packetsInFlight.addLast(packet)
        packetsInFlightBySequenceNumber[packet.sequenceNumber] = packet
    }

    private fun outputPacket(packet: ByteArray) {
        delegate?.srtSenderOutput(packet)
        numberOfBytesSent += packet.size.toULong() + srtIpUdpHeaderSize
    }

    private fun setState(state: SrtSenderState) {
        if (state == this.state) {
            return
        }
        Log.i(TAG, "srt-sender: Job state change ${this.state} -> $state")
        this.state = state
    }

    private fun getNextSequenceNumber(): UInt {
        try {
            return nextSequenceNumber
        } finally {
            nextSequenceNumber += 1u
        }
    }

    private fun createInductionHandshakePacket(): ByteArray {
        val writer = ByteWriter()
        writer.writeBytes(createCommonControlPacketHeader(
            ControlPacketType.handshake,
            0u,
            clock.timestamp(),
            srtDestinationSocket
        ))
        writer.writeUInt32(srtHandshakeVersion4)
        writer.writeUInt16(0u.toUShort())
        writer.writeUInt16(2u.toUShort())
        writer.writeUInt32(nextSequenceNumber)
        writer.writeUInt32(srtMaximumTransmissionUnitSize)
        writer.writeUInt32(srtMaximumFlowWindowSizeInPackets)
        writer.writeUInt32(HandshakeType.induction.rawValue)
        writer.writeUInt32(srtSocketId)
        writer.writeUInt32(0u)
        writer.writeBytes(byteArrayOf(1, 0, 0, 127,
                                      0, 0, 0, 0,
                                      0, 0, 0, 0,
                                      0, 0, 0, 0))
        return writer.data
    }

    private fun createConclusionHandshakePacket(peerSocketId: UInt, synCookie: UInt): ByteArray {
        val writer = ByteWriter()
        writer.writeBytes(createCommonControlPacketHeader(
            ControlPacketType.handshake,
            0u,
            clock.timestamp(),
            srtDestinationSocket
        ))
        writer.writeUInt32(srtHandshakeVersion5)
        writer.writeUInt16(0u.toUShort())
        writer.writeUInt16(5u.toUShort())
        writer.writeUInt32(nextSequenceNumber)
        writer.writeUInt32(srtMaximumTransmissionUnitSize)
        writer.writeUInt32(srtMaximumFlowWindowSizeInPackets)
        writer.writeUInt32(HandshakeType.conclusion.rawValue)
        writer.writeUInt32(peerSocketId)
        writer.writeUInt32(synCookie)
        writer.writeBytes(byteArrayOf(1, 0, 0, 127,
                                      0, 0, 0, 0,
                                      0, 0, 0, 0,
                                      0, 0, 0, 0))
        writer.writeUInt16(1u.toUShort())
        writer.writeUInt16(3u.toUShort())
        writer.writeUInt32(0x0001_0503u)
        writer.writeUInt32(0xBFu)
        writer.writeUInt16(latency)
        writer.writeUInt16(latency)
        val streamId = streamId
        if (streamId != null) {
            writer.writeUInt16(5u.toUShort())
            val encodedStreamId = encodeStreamId(streamId)
            writer.writeUInt16((encodedStreamId.size / 4).toUShort())
            writer.writeBytes(encodedStreamId)
        }
        return writer.data
    }

    private fun encodeStreamId(streamId: String): ByteArray {
        var encodedStreamId = streamId.toByteArray(Charsets.UTF_8)
        val paddingLength = 4 - (encodedStreamId.size % 4)
        if (paddingLength < 4) {
            encodedStreamId += ByteArray(paddingLength)
        }
        var offset = 0
        while (offset < encodedStreamId.size) {
            val reversed = encodedStreamId.copyOfRange(offset, offset + 4).reversedArray()
            reversed.copyInto(encodedStreamId, offset)
            offset += 4
        }
        return encodedStreamId
    }

    private fun handleControlPacket(packet: ByteArray, now: Long) {
        val reader = ByteReader(packet)
        val commonHeader = CommonControlPacketHeader(reader)
        when (commonHeader.controlType) {
            ControlPacketType.handshake -> handleHandshakePacket(reader)
            ControlPacketType.keepAlive -> handleKeepAlivePacket()
            ControlPacketType.ack -> handleAckPacket(commonHeader, reader, now)
            ControlPacketType.nak -> handleNakPacket(reader)
            ControlPacketType.shutdown -> handleShutdownPacket()
            ControlPacketType.ackack -> handleAckAckPacket()
        }
        outputPackets(now)
    }

    private fun handleHandshakePacket(reader: ByteReader) {
        reader.readUInt32()
        reader.readUInt16()
        reader.readUInt16()
        reader.readUInt32()
        reader.readUInt32()
        reader.readUInt32()
        val handshakeType = HandshakeType.fromRawValue(reader.readUInt32())
            ?: throw SrtSenderException("Unsupported handshake type")
        val peerSocketId = reader.readUInt32()
        val synCookie = reader.readUInt32()
        reader.readBytes(16)
        when (handshakeType) {
            HandshakeType.induction -> handleHandshakeInduction(peerSocketId, synCookie)
            HandshakeType.conclusion -> handleHandshakeConclusion(peerSocketId)
        }
    }

    private fun handleHandshakeInduction(peerSocketId: UInt, synCookie: UInt) {
        outputPacket(createConclusionHandshakePacket(
            peerSocketId,
            synCookie
        ))
    }

    private fun handleHandshakeConclusion(peerSocketId: UInt) {
        peerDestinationSrtSocketId = peerSocketId
        ackAckPacket.update(peerSocketId)
        keepAlivePacket.updateDestinationSocketId(peerSocketId)
        connectTimer.stop()
        setState(SrtSenderState.connected)
        delegate?.srtSenderConnected()
    }

    private fun handleKeepAlivePacket() {
        keepAlivePacket.updateTimestamp(clock.timestamp())
        outputPacket(keepAlivePacket.data)
    }

    private fun handleAckPacket(commonHeader: CommonControlPacketHeader,
                                reader: ByteReader,
                                now: Long)
    {
        val lastAcknowledgedPacketSequenceNumber = reader.readUInt32()
        removeAckedPackets(lastAcknowledgedPacketSequenceNumber)
        if (commonHeader.typeSpecificInformation != 0u) {
            rttUs = reader.readUInt32()
            updateSendRate(now)
            updatePerformanceData()
            ackAckPacket.update(commonHeader.typeSpecificInformation, clock.timestamp())
            outputPacket(ackAckPacket.data)
        }
    }

    private fun updatePerformanceData() {
        performanceData.mutate {
            it.value.pktRetransTotal = pktRetransTotal
            it.value.pktRecvNakTotal = pktRecvNakTotal
            it.value.pktSndDropTotal = pktSndDropTotal
            it.value.pktFlightSize = packetsInFlight.size
            it.value.msRtt = rttUs.toDouble() / 1000
            it.value.mbpsSendRate = mbpsSendRate
        }
    }

    private fun updateSendRate(now: Long) {
        val duration = now - latestNumberOfBytesSentTime
        if (duration <= 200_000_000L) {
            return
        }
        latestNumberOfBytesSentTime = now
        val durationSeconds = duration.toDouble() / 1_000_000_000.0
        val latestMbpsSendRate = (numberOfBytesSent * 8uL).toDouble() / durationSeconds / 1_000_000
        numberOfBytesSent = 0uL
        mbpsSendRate = 0.7 * mbpsSendRate + 0.3 * latestMbpsSendRate
    }

    private fun removeAckedPackets(lastAcknowledgedPacketSequenceNumber: UInt) {
        val lastAcknowledgedPacketIndex = packetsInFlight.indexOfFirst {
            !isSrtSnAcked(it.sequenceNumber, lastAcknowledgedPacketSequenceNumber)
        }
        if (lastAcknowledgedPacketIndex >= 0) {
            for (index in 0 until lastAcknowledgedPacketIndex) {
                packetsInFlightBySequenceNumber.remove(packetsInFlight[index].sequenceNumber)
            }
            repeat(lastAcknowledgedPacketIndex) {
                packetsInFlight.removeFirst()
            }
        } else {
            packetsInFlightBySequenceNumber.clear()
            packetsInFlight.clear()
        }
    }

    private fun handleNakPacket(reader: ByteReader) {
        while (true) {
            val sequenceNumber = runCatching { reader.readUInt32() }.getOrNull() ?: break
            if (isSrtSnRange(sequenceNumber)) {
                val firstSequenceNumber = sequenceNumber and 0x7FFF_FFFFu
                val upToNakSequenceNumber = reader.readUInt32()
                if (upToNakSequenceNumber - firstSequenceNumber >= srtMaximumFlowWindowSizeInPackets) {
                    continue
                }
                var sn = firstSequenceNumber
                while (sn <= upToNakSequenceNumber) {
                    if (numberOfPacketsToRetransmit() >= 1000) {
                        return
                    }
                    appendSequenceNumberToRetransmit(sn)
                    sn += 1u
                }
            } else {
                if (numberOfPacketsToRetransmit() >= 1000) {
                    return
                }
                appendSequenceNumberToRetransmit(sequenceNumber)
            }
        }
        pktRecvNakTotal += 1
    }

    private fun appendSequenceNumberToRetransmit(sequenceNumber: UInt) {
        val packet = packetsInFlightBySequenceNumber[sequenceNumber] ?: return
        if (packet.containsAudio) {
            audioSequenceNumbersToRetransmit.add(sequenceNumber)
        } else {
            videoSequenceNumbersToRetransmit.add(sequenceNumber)
        }
    }

    private fun handleShutdownPacket() {
        throw SrtSenderException("Got shutdown packet")
    }

    private fun handleAckAckPacket() {
        throw SrtSenderException("Got ack ack packet")
    }

    private fun numberOfPacketsToRetransmit(): Int {
        return audioSequenceNumbersToRetransmit.size + videoSequenceNumbersToRetransmit.size
    }
}

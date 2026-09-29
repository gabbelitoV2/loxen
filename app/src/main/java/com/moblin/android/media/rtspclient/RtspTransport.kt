package com.moblin.android.media.rtspclient

import com.moblin.android.platform.log.Log
import com.moblin.android.media.haishinkit.util.ByteWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private const val TAG = "RtspTransport"

private val rtspEndOfHeaders = byteArrayOf(0xD, 0xA, 0xD, 0xA)

interface RtspTransportDelegate {
    fun rtspTransportConnected()

    fun rtspTransportDisconnected()

    fun rtspTransportReceivedRtspMessage(header: ByteArray, content: ByteArray?)

    fun rtspTransportReceivedRtpPacket(packet: ByteArray)

    fun rtspTransportReceivedRtcpPacket(packet: ByteArray)
}

open class RtspTransport {
    var delegate: RtspTransportDelegate? = null
    protected fun deliver(block: RtspTransportDelegate.() -> Unit) { CoroutineScope(rtspClientQueue).launch { delegate?.block() } }

    open fun start(host: String, port: Int) {
    }

    open fun stop() {
    }

    open fun sendRtsp(data: ByteArray) {
    }

    open fun sendRtcp(data: ByteArray) {
    }

    open fun setupTransportHeader(): String {
        return ""
    }

    @Throws(Exception::class)
    open fun handleSetupTransportResponse(value: String) {
    }
}

class RtspTransportRtpRtspTcp : RtspTransport() {
    private val channelStart: UByte = '$'.code.toUByte()
    @Volatile private var connection: Socket? = null
    @Volatile private var rtpChannel: UByte? = null
    @Volatile private var rtcpChannel: UByte? = null
    private var header = ByteArray(0)
    private var scope: CoroutineScope? = null

    override fun start(host: String, port: Int) {
        val newScope = CoroutineScope(Dispatchers.IO)
        scope = newScope
        newScope.launch {
            try {
                val socket = Socket().also { connection = it }
                socket.connect(InetSocketAddress(host, port))
                connection = socket
                deliver { rtspTransportConnected() }
                receiveMessage()
            } catch (e: Exception) {
                Log.d(TAG, "rtsp-client: TCP transport error: $e")
            }
        }
    }

    override fun stop() {
        delegate = null
        scope?.coroutineContext?.cancel()
        scope = null
        try {
            connection?.close()
        } catch (e: Exception) {
            Log.d(TAG, "rtsp-client: TCP transport error: $e")
        }
        connection = null
    }

    @Synchronized
    override fun sendRtsp(data: ByteArray) {
        val socket = connection ?: return
        try {
            val output = socket.getOutputStream()
            output.write(data)
            output.flush()
        } catch (e: Exception) {
            Log.d(TAG, "rtsp-client: TCP transport error: $e")
        }
    }

    @Synchronized
    override fun sendRtcp(data: ByteArray) {
        val rtcp = rtcpChannel ?: return
        if (data.size > 0xFFFF) {
            return
        }
        val writer = ByteWriter()
        writer.writeUInt8(channelStart)
        writer.writeUInt8(rtcp)
        writer.writeUInt16(data.size.toUShort())
        writer.writeBytes(data)
        val socket = connection ?: return
        try {
            val output = socket.getOutputStream()
            output.write(writer.data)
            output.flush()
        } catch (e: Exception) {
            Log.d(TAG, "rtsp-client: TCP transport error: $e")
        }
    }

    override fun setupTransportHeader(): String {
        return "RTP/AVP/TCP;unicast;interleaved=0-1"
    }

    @Throws(Exception::class)
    override fun handleSetupTransportResponse(value: String) {
        val match = Regex("interleaved=(\\d+)-(\\d+)").find(value)
            ?: throw Exception("Invalid interleaving in $value.")
        rtpChannel = match.groupValues[1].toUByteOrNull()
        rtcpChannel = match.groupValues[2].toUByteOrNull()
        if (rtpChannel == null || rtcpChannel == null) {
            throw Exception("Invalid interleaving channels in $value.")
        }
    }

    private suspend fun receiveMessage() {
        while (true) {
            val data = receive(1) ?: return
            if ((data[0].toInt() and 0xFF) == channelStart.toInt()) {
                receiveChannelHeader()
            } else {
                header = ByteArray(0)
                header += data
                receiveRtspHeaderRemaining()
            }
        }
    }

    private suspend fun receiveChannelHeader() {
        val data = receive(3) ?: return
        val channel = data[0].toUByte()
        val size = ((data[1].toInt() and 0xFF) shl 8) or (data[2].toInt() and 0xFF)
        receiveChannelData(channel, size)
    }

    private suspend fun receiveChannelData(channel: UByte, size: Int) {
        val data = receive(size) ?: return
        if (channel == rtpChannel) {
            deliver { rtspTransportReceivedRtpPacket(data) }
        } else if (channel == rtcpChannel) {
            deliver { rtspTransportReceivedRtcpPacket(data) }
        }
    }

    private suspend fun receiveRtspHeaderRemaining() {
        while (true) {
            val data = receive(1) ?: return
            header += data
            if (header.size >= rtspEndOfHeaders.size &&
                header.copyOfRange(header.size - rtspEndOfHeaders.size, header.size)
                    .contentEquals(rtspEndOfHeaders)
            ) {
                val contentLength = parseContentLength(header)
                if (contentLength > 0) {
                    receiveRtspContent(header, contentLength)
                } else {
                    header.let { message -> deliver { rtspTransportReceivedRtspMessage(message, null) } }
                }
                return
            }
        }
    }

    private suspend fun receiveRtspContent(header: ByteArray, size: Int) {
        val data = receive(size) ?: return
        deliver { rtspTransportReceivedRtspMessage(header, data) }
    }

    private suspend fun receive(size: Int): ByteArray? {
        val input = connection?.getInputStream() ?: return null
        val buffer = ByteArray(size)
        var offset = 0
        while (offset < size) {
            val read = try {
                input.read(buffer, offset, size - offset)
            } catch (e: Exception) {
                Log.d(TAG, "rtsp-client: TCP transport error: $e")
                return null
            }
            if (read < 0) {
                return null
            }
            offset += read
        }
        return buffer
    }
}

class RtspTransportRtpUdp : RtspTransport() {
    private var host: String = ""
    private var port: Int = 554
    @Volatile private var rtspConnection: Socket? = null
    private var rtpListener: DatagramSocket? = null
    private var rtcpListener: DatagramSocket? = null
    private var rtcpSendConnection: DatagramSocket? = null
    private var header = ByteArray(0)
    private var localRtpPort: Int? = null
    private var localRtcpPort: Int? = null
    private var remoteRtcpPort: Int? = null
    private var scope: CoroutineScope? = null

    override fun start(host: String, port: Int) {
        this.host = host
        this.port = port
        setupUdpListeners()
    }

    override fun stop() {
        delegate = null
        scope?.coroutineContext?.cancel()
        scope = null
        try {
            rtspConnection?.close()
        } catch (e: Exception) {
            Log.d(TAG, "rtsp-client: UDP transport error: $e")
        }
        rtspConnection = null
        try {
            rtpListener?.close()
        } catch (e: Exception) {
            Log.d(TAG, "rtsp-client: UDP transport error: $e")
        }
        rtpListener = null
        try {
            rtcpListener?.close()
        } catch (e: Exception) {
            Log.d(TAG, "rtsp-client: UDP transport error: $e")
        }
        rtcpListener = null
        try {
            rtcpSendConnection?.close()
        } catch (e: Exception) {
            Log.d(TAG, "rtsp-client: UDP transport error: $e")
        }
        rtcpSendConnection = null
        localRtpPort = null
        localRtcpPort = null
    }

    @Synchronized
    override fun sendRtsp(data: ByteArray) {
        val socket = rtspConnection ?: return
        try {
            val output = socket.getOutputStream()
            output.write(data)
            output.flush()
        } catch (e: Exception) {
            Log.d(TAG, "rtsp-client: UDP transport error: $e")
        }
    }

    @Synchronized
    override fun sendRtcp(data: ByteArray) {
        if (rtcpSendConnection == null) {
            val remotePort = remoteRtcpPort
            if (remotePort != null) {
                try {
                    val socket = DatagramSocket()
                    socket.connect(InetSocketAddress(host, remotePort))
                    rtcpSendConnection = socket
                } catch (e: Exception) {
                    Log.d(TAG, "rtsp-client: UDP transport error: $e")
                    return
                }
            }
        }
        val socket = rtcpSendConnection ?: return
        try {
            socket.send(DatagramPacket(data, data.size))
        } catch (e: Exception) {
            Log.d(TAG, "rtsp-client: UDP transport error: $e")
        }
    }

    override fun setupTransportHeader(): String {
        val rtpPort = localRtpPort
        val rtcpPort = localRtcpPort
        if (rtpPort == null || rtcpPort == null) {
            Log.i(TAG, "rtsp-client: UDP listeners not ready when building transport header")
            return "RTP/AVP;unicast;client_port=0-1"
        }
        return "RTP/AVP;unicast;client_port=$rtpPort-$rtcpPort"
    }

    @Throws(Exception::class)
    override fun handleSetupTransportResponse(value: String) {
        val match = Regex("server_port=(\\d+)-(\\d+)").find(value)
            ?: throw Exception("Missing server_port in UDP transport response: $value")
        val rtpPortValue = match.groupValues[1].toIntOrNull()
        val rtcpPortValue = match.groupValues[2].toIntOrNull()
        if (rtpPortValue == null || rtpPortValue !in 0..0xFFFF ||
            rtcpPortValue == null || rtcpPortValue !in 0..0xFFFF
        ) {
            throw Exception("Invalid RTP or RTCP server port in: $value")
        }
        remoteRtcpPort = rtcpPortValue
    }

    private fun setupUdpListeners() {
        val newScope = CoroutineScope(Dispatchers.IO)
        scope = newScope
        val rtp: DatagramSocket
        val rtcp: DatagramSocket
        try {
            rtp = DatagramSocket()
            rtcp = DatagramSocket()
        } catch (e: Exception) {
            Log.d(TAG, "rtsp-client: Failed to create UDP listeners: $e")
            return
        }
        rtpListener = rtp
        rtcpListener = rtcp
        localRtpPort = rtp.localPort
        localRtcpPort = rtcp.localPort
        newScope.launch {
            receiveRtpDatagram(rtp)
        }
        newScope.launch {
            receiveRtcpDatagram(rtcp)
        }
        connectRtspIfReady()
    }

    private fun connectRtspIfReady() {
        if (localRtpPort != null && localRtcpPort != null) {
            connectRtsp()
        }
    }

    private fun connectRtsp() {
        val newScope = scope ?: return
        newScope.launch {
            try {
                val socket = Socket().also { rtspConnection = it }
                socket.connect(InetSocketAddress(host, port))
                rtspConnection = socket
                deliver { rtspTransportConnected() }
                receiveRtspMessage()
            } catch (e: Exception) {
                Log.d(TAG, "rtsp-client: UDP transport error: $e")
            }
        }
    }

    private suspend fun receiveRtpDatagram(socket: DatagramSocket) {
        val buffer = ByteArray(65536)
        while (true) {
            val packet = DatagramPacket(buffer, buffer.size)
            try {
                socket.receive(packet)
            } catch (e: Exception) {
                Log.d(TAG, "rtsp-client: RTP datagram receive ended")
                return
            }
            val data = packet.data.copyOfRange(packet.offset, packet.offset + packet.length)
            deliver { rtspTransportReceivedRtpPacket(data) }
        }
    }

    private suspend fun receiveRtcpDatagram(socket: DatagramSocket) {
        val buffer = ByteArray(65536)
        while (true) {
            val packet = DatagramPacket(buffer, buffer.size)
            try {
                socket.receive(packet)
            } catch (e: Exception) {
                Log.d(TAG, "rtsp-client: RTCP datagram receive ended")
                return
            }
            val data = packet.data.copyOfRange(packet.offset, packet.offset + packet.length)
            deliver { rtspTransportReceivedRtcpPacket(data) }
        }
    }

    private suspend fun receiveRtspMessage() {
        while (true) {
            val data = receiveRtsp(1) ?: return
            header = ByteArray(0)
            header += data
            receiveRtspHeaderRemaining()
        }
    }

    private suspend fun receiveRtspHeaderRemaining() {
        while (true) {
            val data = receiveRtsp(1) ?: return
            header += data
            if (header.size >= rtspEndOfHeaders.size &&
                header.copyOfRange(header.size - rtspEndOfHeaders.size, header.size)
                    .contentEquals(rtspEndOfHeaders)
            ) {
                val contentLength = parseContentLength(header)
                if (contentLength > 0) {
                    receiveRtspContent(header, contentLength)
                } else {
                    header.let { message -> deliver { rtspTransportReceivedRtspMessage(message, null) } }
                }
                return
            }
        }
    }

    private suspend fun receiveRtspContent(header: ByteArray, size: Int) {
        val data = receiveRtsp(size) ?: return
        deliver { rtspTransportReceivedRtspMessage(header, data) }
    }

    private suspend fun receiveRtsp(size: Int): ByteArray? {
        val input = rtspConnection?.getInputStream() ?: return null
        val buffer = ByteArray(size)
        var offset = 0
        while (offset < size) {
            val read = try {
                input.read(buffer, offset, size - offset)
            } catch (e: Exception) {
                Log.d(TAG, "rtsp-client: UDP transport error: $e")
                return null
            }
            if (read < 0) {
                return null
            }
            offset += read
        }
        return buffer
    }
}

private fun parseContentLength(header: ByteArray): Int {
    val headerString = header.toString(Charsets.UTF_8)
    for (line in headerString.split("\r\n")) {
        val lower = line.lowercase()
        if (lower.startsWith("content-length:")) {
            val parts = lower.split(":", limit = 2)
            if (parts.size == 2) {
                val length = parts[1].trim().toIntOrNull()
                if (length != null) {
                    return length
                }
            }
        }
    }
    return 0
}

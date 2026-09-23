package com.moblin.android.platform.network

import android.net.Network
import android.util.Log
import com.moblin.android.platform.core.PipelineStats
import java.io.BufferedOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.PortUnreachableException
import java.net.Socket
import java.net.SocketAddress
import java.net.SocketTimeoutException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SNIHostName
import javax.net.ssl.SNIServerName
import javax.net.ssl.SSLPeerUnverifiedException
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import kotlin.concurrent.thread
import kotlin.coroutines.ContinuationInterceptor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NWError(message: String) : Exception(message) {
    override fun toString(): String {
        return message ?: "NWError"
    }
}

class NWConnection private constructor(
    val endpoint: NWEndpoint,
    val parameters: NWParameters,
    private val acceptedSocket: Socket?,
    private val listenerSocket: DatagramSocket?,
    private val listenerPeer: SocketAddress?,
) {
    constructor(endpoint: NWEndpoint, parameters: NWParameters) : this(endpoint, parameters, null, null, null)

    constructor(host: NWEndpoint.Host, port: NWEndpoint.Port, using: NWParameters) :
        this(NWEndpoint.hostPort(host = host, port = port), using, null, null, null)

    internal constructor(endpoint: NWEndpoint, parameters: NWParameters, acceptedSocket: Socket) :
        this(endpoint, parameters, acceptedSocket, null, null)

    internal constructor(
        endpoint: NWEndpoint,
        parameters: NWParameters,
        listenerSocket: DatagramSocket,
        peer: SocketAddress,
    ) : this(endpoint, parameters, null, listenerSocket, peer)

    sealed class State {
        data object setup : State()

        data object preparing : State()

        data object ready : State()

        class waiting(val error: NWError) : State() {
            override fun toString(): String {
                return "waiting($error)"
            }
        }

        class failed(val error: NWError) : State() {
            override fun toString(): String {
                return "failed($error)"
            }
        }

        data object cancelled : State()
    }

    sealed class SendCompletion {
        data object idempotent : SendCompletion()

        class contentProcessed(val handler: (NWError?) -> Unit) : SendCompletion()
    }

    private class ReceiveRequest(
        val minimumIncompleteLength: Int,
        val maximumLength: Int,
        val completion: (ByteArray?, Any?, Boolean, NWError?) -> Unit,
    )

    private class Delivery(
        val request: ReceiveRequest,
        val content: ByteArray?,
        val isComplete: Boolean,
        val error: NWError?,
    )

    private class PendingSend(val content: ByteArray?, val completion: SendCompletion)

    private val lock = Any()
    private val id = nextId.incrementAndGet()
    private val isDatagram = listenerSocket != null || (acceptedSocket == null && parameters.isDatagram)

    @Volatile
    private var scope: CoroutineScope? = null

    @Volatile
    private var cancelled = false

    @Volatile
    private var currentState: State = State.setup

    @Volatile
    private var rawSocket: Socket? = null

    @Volatile
    private var tlsSocket: SSLSocket? = null

    @Volatile
    private var datagramSocket: DatagramSocket? = null

    private var started = false
    private var hasFailed = false
    private var viabilityLost = false
    private var writeError: NWError? = null
    private var pendingWriteBytes = 0L
    private val writeQueue = LinkedBlockingQueue<PendingSend>()
    private val receiveRequests = ArrayDeque<ReceiveRequest>()
    private val inboundChunks = ArrayDeque<ByteArray>()
    private var inboundOffset = 0
    private var inboundBytes = 0
    private var inboundComplete = false
    private var inboundError: NWError? = null
    private var latestSendFailureLogNs = 0L

    @Volatile
    var stateUpdateHandler: ((State) -> Unit)? = null

    @Volatile
    var viabilityUpdateHandler: ((Boolean) -> Unit)? = null

    val state: State
        get() = currentState

    internal val isCancelled: Boolean
        get() = cancelled

    private val description: String
        get() = if (isDatagram) "udp#$id $endpoint" else "tcp#$id $endpoint"

    fun start(queue: CoroutineDispatcher) {
        startInternal(CoroutineScope(queue + SupervisorJob()))
    }

    fun start(queue: CoroutineScope) {
        val dispatcher = queue.coroutineContext[ContinuationInterceptor] as? CoroutineDispatcher ?: Dispatchers.Default
        startInternal(CoroutineScope(dispatcher + SupervisorJob()))
    }

    fun send(content: ByteArray?, completion: SendCompletion) {
        if (cancelled) {
            return
        }
        val peerSocket = listenerSocket
        when {
            peerSocket != null -> sendDatagram(peerSocket, listenerPeer, content, completion)
            isDatagram -> sendDatagram(datagramSocket, null, content, completion)
            else -> sendStream(content, completion)
        }
    }

    fun receive(
        minimumIncompleteLength: Int,
        maximumLength: Int,
        completion: (ByteArray?, Any?, Boolean, NWError?) -> Unit,
    ) {
        synchronized(lock) {
            if (cancelled) {
                return
            }
            receiveRequests.addLast(ReceiveRequest(minimumIncompleteLength, maximumLength, completion))
        }
        postDeliverReceives()
    }

    fun receiveMessage(completion: (ByteArray?, Any?, Boolean, NWError?) -> Unit) {
        receive(minimumIncompleteLength = 1, maximumLength = Int.MAX_VALUE, completion = completion)
    }

    fun batch(block: () -> Unit) {
        block()
    }

    fun cancel() {
        cancelInternal(force = false)
    }

    fun forceCancel() {
        cancelInternal(force = true)
    }

    internal fun deliverDatagram(datagram: ByteArray) {
        if (cancelled) {
            return
        }
        synchronized(lock) {
            if (inboundChunks.size >= MAXIMUM_BUFFERED_DATAGRAMS) {
                val dropped = inboundChunks.removeFirst()
                inboundBytes -= dropped.size
            }
            inboundChunks.addLast(datagram)
            inboundBytes += datagram.size
        }
        PipelineStats.increment("udpRx", datagram.size.toLong())
        postDeliverReceives()
    }

    private fun startInternal(queueScope: CoroutineScope) {
        synchronized(lock) {
            if (started || cancelled) {
                return
            }
            started = true
            scope = queueScope
        }
        setState(State.preparing)
        postDeliverReceives()
        val socket = acceptedSocket
        when {
            listenerSocket != null -> setState(State.ready)
            socket != null -> startThread("tcp") { runAccepted(socket) }
            isDatagram -> startThread("udp") { runUdp() }
            else -> startThread("tcp") { runTcp() }
        }
    }

    private fun startThread(kind: String, block: () -> Unit) {
        thread(name = "nw-$kind-$id", isDaemon = true) {
            try {
                block()
            } catch (error: Throwable) {
                Log.e(TAG, "$description: Thread failed", error)
            }
        }
    }

    private fun runTcp() {
        val host = endpoint.host.value
        val port = endpoint.port.value
        val startNs = System.nanoTime()
        try {
            val network = parameters.requiredInterface?.resolveNetwork()
            val addresses = resolve(host, network)
            val timeoutSeconds = parameters.tcpOptions.connectionTimeout
            val timeoutMs = if (timeoutSeconds > 0) timeoutSeconds * 1000L else DEFAULT_CONNECT_TIMEOUT_MS
            val deadlineNs = startNs + timeoutMs * 1_000_000L
            var lastError: Throwable? = null
            var connected: Socket? = null
            for ((index, address) in addresses.withIndex()) {
                val remainingMs = (deadlineNs - System.nanoTime()) / 1_000_000L
                if (cancelled || remainingMs <= 0) {
                    break
                }
                val attemptMs = minOf(remainingMs, maxOf(remainingMs / (addresses.size - index), 3_000L))
                val candidate = Socket()
                rawSocket = candidate
                if (cancelled) {
                    closeQuietly(candidate)
                    return
                }
                try {
                    network?.bindSocket(candidate)
                    configure(candidate)
                    candidate.connect(InetSocketAddress(address, port), attemptMs.toInt())
                    connected = candidate
                    break
                } catch (error: IOException) {
                    lastError = error
                    closeQuietly(candidate)
                }
            }
            if (cancelled) {
                return
            }
            val socket = connected ?: throw (lastError ?: SocketTimeoutException("Connect timed out"))
            val stream = if (parameters.tlsOptions != null) startTls(socket, host, port) else socket
            if (cancelled) {
                closeQuietly(stream)
                return
            }
            val elapsedMs = (System.nanoTime() - startNs) / 1_000_000L
            Log.i(TAG, "$description: Connected to ${socket.inetAddress?.hostAddress} in $elapsedMs ms")
            openStreams(stream)
        } catch (error: Throwable) {
            if (cancelled) {
                return
            }
            fail(makeError(error))
        }
    }

    private fun runAccepted(socket: Socket) {
        rawSocket = socket
        if (cancelled) {
            closeQuietly(socket)
            return
        }
        try {
            openStreams(socket)
        } catch (error: Throwable) {
            if (cancelled) {
                return
            }
            fail(makeError(error))
        }
    }

    private fun configure(socket: Socket) {
        val tcpOptions = parameters.tcpOptions
        socket.tcpNoDelay = tcpOptions.noDelay
        socket.keepAlive = tcpOptions.enableKeepalive
        if (parameters.allowLocalEndpointReuse) {
            socket.reuseAddress = true
        }
    }

    private fun startTls(socket: Socket, host: String, port: Int): Socket {
        val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
        val tls = factory.createSocket(socket, host, port, true) as SSLSocket
        tlsSocket = tls
        if (cancelled) {
            closeQuietly(tls)
            throw IOException("Cancelled")
        }
        try {
            val sslParameters = tls.sslParameters
            sslParameters.endpointIdentificationAlgorithm = "HTTPS"
            if (isHostName(host)) {
                try {
                    sslParameters.serverNames = listOf<SNIServerName>(SNIHostName(host))
                } catch (error: IllegalArgumentException) {
                    Log.i(TAG, "$description: No SNI for $host: $error")
                }
            }
            tls.sslParameters = sslParameters
            tls.soTimeout = TLS_HANDSHAKE_TIMEOUT_MS
            tls.startHandshake()
            tls.soTimeout = 0
            if (!HttpsURLConnection.getDefaultHostnameVerifier().verify(host, tls.session)) {
                throw SSLPeerUnverifiedException("Certificate does not match")
            }
        } catch (error: IOException) {
            closeQuietly(tls)
            throw NWError("TLS handshake failed for hostname $host: ${describe(error)}")
        }
        Log.i(TAG, "$description: ${tls.session.protocol} ${tls.session.cipherSuite}")
        return tls
    }

    private fun openStreams(socket: Socket) {
        val input = socket.getInputStream()
        val output = socket.getOutputStream()
        startThread("tcp-write") { runWriter(output) }
        setState(State.ready)
        runReader(input)
    }

    private fun runReader(input: InputStream) {
        val buffer = ByteArray(STREAM_READ_SIZE)
        try {
            while (!cancelled) {
                waitForReceiveSpace()
                val count = input.read(buffer)
                if (count < 0) {
                    synchronized(lock) {
                        inboundComplete = true
                    }
                    Log.i(TAG, "$description: Closed by peer")
                    postDeliverReceives()
                    postViabilityLost()
                    return
                }
                if (count > 0) {
                    synchronized(lock) {
                        inboundChunks.addLast(buffer.copyOf(count))
                        inboundBytes += count
                    }
                    postDeliverReceives()
                }
            }
        } catch (error: Throwable) {
            if (cancelled) {
                return
            }
            postViabilityLost()
            fail(makeError(error))
        }
    }

    private fun waitForReceiveSpace() {
        while (!cancelled && synchronized(lock) { inboundBytes } > MAXIMUM_BUFFERED_STREAM_BYTES) {
            Thread.sleep(5)
        }
    }

    private fun runWriter(output: OutputStream) {
        val stream = BufferedOutputStream(output, WRITE_BUFFER_SIZE)
        val written = ArrayList<PendingSend>()
        var unflushedBytes = 0
        try {
            while (true) {
                val item = writeQueue.take()
                if (item === stopWriting) {
                    if (written.isNotEmpty() && !cancelled) {
                        finishSends(written, synchronized(lock) { writeError } ?: NWError("Connection failed"))
                    }
                    return
                }
                val content = item.content
                if (content != null && content.isNotEmpty()) {
                    stream.write(content)
                    unflushedBytes += content.size
                }
                written.add(item)
                if (writeQueue.isEmpty() || unflushedBytes >= WRITE_BUFFER_SIZE) {
                    stream.flush()
                    unflushedBytes = 0
                    finishSends(written, null)
                    written.clear()
                }
            }
        } catch (error: Throwable) {
            if (cancelled) {
                return
            }
            val nwError = makeError(error)
            finishSends(written, nwError)
            fail(nwError)
        }
    }

    private fun sendStream(content: ByteArray?, completion: SendCompletion) {
        val size = content?.size?.toLong() ?: 0L
        val error = synchronized(lock) {
            if (cancelled) {
                return
            }
            val currentError = writeError
            if (currentError == null) {
                writeQueue.offer(PendingSend(content, completion))
                pendingWriteBytes += size
                totalStreamPendingBytes.addAndGet(size)
            }
            currentError
        }
        if (error != null) {
            complete(completion, error)
        }
    }

    private fun finishSends(items: List<PendingSend>, error: NWError?) {
        var bytes = 0L
        val handlers = ArrayList<(NWError?) -> Unit>()
        for (item in items) {
            if (item === stopWriting) {
                continue
            }
            bytes += item.content?.size ?: 0
            val completion = item.completion
            if (completion is SendCompletion.contentProcessed) {
                handlers.add(completion.handler)
            }
        }
        val total = synchronized(lock) {
            if (!cancelled) {
                pendingWriteBytes -= bytes
                totalStreamPendingBytes.addAndGet(-bytes)
            } else {
                totalStreamPendingBytes.get()
            }
        }
        if (error == null && bytes > 0) {
            PipelineStats.increment("tcpTx", bytes)
        }
        PipelineStats.gauge("tcpBufferedBytes", total)
        if (handlers.isEmpty()) {
            return
        }
        post {
            for (handler in handlers) {
                if (cancelled) {
                    break
                }
                runHandler {
                    handler(error)
                }
            }
        }
    }

    private fun runUdp() {
        val host = endpoint.host.value
        val port = endpoint.port.value
        var waitingReported = false
        var socket: DatagramSocket? = null
        while (!cancelled && socket == null) {
            try {
                val network = parameters.requiredInterface?.resolveNetwork()
                val address = resolve(host, network).first()
                val candidate = DatagramSocket()
                datagramSocket = candidate
                if (cancelled) {
                    candidate.close()
                    return
                }
                try {
                    network?.bindSocket(candidate)
                    candidate.connect(InetSocketAddress(address, port))
                } catch (error: Throwable) {
                    candidate.close()
                    throw error
                }
                socket = candidate
            } catch (error: Throwable) {
                if (cancelled) {
                    return
                }
                if (!waitingReported) {
                    waitingReported = true
                    val nwError = makeError(error)
                    Log.i(TAG, "$description: Waiting: $nwError")
                    setState(State.waiting(nwError))
                }
                sleepUnlessCancelled(UDP_RETRY_INTERVAL_MS)
            }
        }
        val connectedSocket = socket ?: return
        if (cancelled) {
            connectedSocket.close()
            return
        }
        Log.i(TAG, "$description: Ready on local port ${connectedSocket.localPort}")
        setState(State.ready)
        receiveDatagrams(connectedSocket)
    }

    private fun receiveDatagrams(socket: DatagramSocket) {
        val buffer = ByteArray(MAXIMUM_DATAGRAM_SIZE)
        val packet = DatagramPacket(buffer, buffer.size)
        while (!cancelled) {
            try {
                packet.length = buffer.size
                socket.receive(packet)
                deliverDatagram(buffer.copyOf(packet.length))
            } catch (error: PortUnreachableException) {
                continue
            } catch (error: Throwable) {
                if (cancelled) {
                    return
                }
                fail(makeError(error))
                return
            }
        }
    }

    private fun sendDatagram(
        socket: DatagramSocket?,
        peer: SocketAddress?,
        content: ByteArray?,
        completion: SendCompletion,
    ) {
        if (content == null) {
            complete(completion, null)
            return
        }
        if (socket == null || currentState != State.ready) {
            complete(completion, NWError("Not connected"))
            return
        }
        try {
            val packet = if (peer != null) {
                DatagramPacket(content, content.size, peer)
            } else {
                DatagramPacket(content, content.size)
            }
            socket.send(packet)
            PipelineStats.increment("udpTx", content.size.toLong())
            complete(completion, null)
        } catch (error: Throwable) {
            val nwError = makeError(error)
            logSendFailure(nwError)
            complete(completion, nwError)
        }
    }

    private fun logSendFailure(error: NWError) {
        val nowNs = System.nanoTime()
        synchronized(lock) {
            if (latestSendFailureLogNs != 0L && nowNs - latestSendFailureLogNs < 5_000_000_000L) {
                return
            }
            latestSendFailureLogNs = nowNs
        }
        Log.i(TAG, "$description: Send failed: $error")
    }

    private fun complete(completion: SendCompletion, error: NWError?) {
        if (completion is SendCompletion.contentProcessed) {
            post {
                completion.handler(error)
            }
        }
    }

    private fun postDeliverReceives() {
        post {
            deliverReceives()
        }
    }

    private fun deliverReceives() {
        while (!cancelled) {
            val delivery = nextDelivery() ?: return
            runHandler {
                delivery.request.completion(delivery.content, null, delivery.isComplete, delivery.error)
            }
        }
    }

    private fun nextDelivery(): Delivery? {
        return synchronized(lock) {
            val request = receiveRequests.firstOrNull()
            val delivery = if (request == null) {
                null
            } else if (isDatagram) {
                nextDatagramDelivery(request)
            } else {
                nextStreamDelivery(request)
            }
            if (delivery != null) {
                receiveRequests.removeFirst()
            }
            delivery
        }
    }

    private fun nextDatagramDelivery(request: ReceiveRequest): Delivery? {
        val datagram = inboundChunks.removeFirstOrNull()
        if (datagram != null) {
            inboundBytes -= datagram.size
            return Delivery(request, datagram, true, null)
        }
        val error = inboundError ?: return null
        return Delivery(request, null, false, error)
    }

    private fun nextStreamDelivery(request: ReceiveRequest): Delivery? {
        val needed = maxOf(request.minimumIncompleteLength, 1)
        val ended = inboundComplete || inboundError != null
        if (inboundBytes >= needed || (inboundBytes > 0 && ended)) {
            val content = takeInbound(minOf(inboundBytes, maxOf(request.maximumLength, 1)))
            return Delivery(request, content, inboundComplete && inboundBytes == 0, null)
        }
        if (inboundComplete) {
            return Delivery(request, null, true, null)
        }
        val error = inboundError ?: return null
        return Delivery(request, null, false, error)
    }

    private fun takeInbound(count: Int): ByteArray {
        val result = ByteArray(count)
        var filled = 0
        while (filled < count) {
            val chunk = inboundChunks.first()
            val length = minOf(chunk.size - inboundOffset, count - filled)
            System.arraycopy(chunk, inboundOffset, result, filled, length)
            filled += length
            inboundOffset += length
            if (inboundOffset == chunk.size) {
                inboundChunks.removeFirst()
                inboundOffset = 0
            }
        }
        inboundBytes -= count
        return result
    }

    private fun setState(newState: State) {
        synchronized(lock) {
            val current = currentState
            if (cancelled || current is State.failed || current == newState) {
                return
            }
            currentState = newState
        }
        post {
            stateUpdateHandler?.invoke(newState)
        }
    }

    private fun postViabilityLost() {
        synchronized(lock) {
            if (viabilityLost || cancelled) {
                return
            }
            viabilityLost = true
        }
        post {
            viabilityUpdateHandler?.invoke(false)
        }
    }

    private fun fail(error: NWError) {
        val failedSends = ArrayList<PendingSend>()
        synchronized(lock) {
            if (hasFailed || cancelled) {
                return
            }
            hasFailed = true
            if (writeError == null) {
                writeError = error
            }
            if (inboundError == null) {
                inboundError = error
            }
            writeQueue.drainTo(failedSends)
        }
        Log.i(TAG, "$description: Failed: $error")
        writeQueue.offer(stopWriting)
        finishSends(failedSends, error)
        postDeliverReceives()
        setState(State.failed(error))
        closeSockets(force = true)
    }

    private fun cancelInternal(force: Boolean) {
        synchronized(lock) {
            if (cancelled) {
                return
            }
            cancelled = true
            currentState = State.cancelled
            receiveRequests.clear()
            inboundChunks.clear()
            inboundOffset = 0
            inboundBytes = 0
            writeQueue.clear()
            totalStreamPendingBytes.addAndGet(-pendingWriteBytes)
            pendingWriteBytes = 0
        }
        writeQueue.offer(stopWriting)
        if (!isDatagram) {
            PipelineStats.gauge("tcpBufferedBytes", totalStreamPendingBytes.get())
        }
        closeSockets(force)
        val queueScope = scope ?: return
        queueScope.launch {
            runHandler {
                stateUpdateHandler?.invoke(State.cancelled)
            }
        }
    }

    private fun closeSockets(force: Boolean) {
        val raw = rawSocket
        val tls = tlsSocket
        val datagram = datagramSocket
        closer.execute {
            if (raw != null) {
                if (force) {
                    runCatching {
                        raw.setSoLinger(true, 0)
                    }
                }
                closeQuietly(raw)
            }
            if (tls != null) {
                closeQuietly(tls)
            }
            datagram?.close()
        }
    }

    private fun post(block: () -> Unit) {
        val queueScope = scope ?: return
        queueScope.launch {
            if (!cancelled) {
                runHandler(block)
            }
        }
    }

    private fun runHandler(block: () -> Unit) {
        try {
            block()
        } catch (error: Throwable) {
            Log.e(TAG, "$description: Handler failed", error)
        }
    }

    private fun sleepUnlessCancelled(milliseconds: Long) {
        var remaining = milliseconds
        while (!cancelled && remaining > 0) {
            Thread.sleep(minOf(remaining, 50L))
            remaining -= 50L
        }
    }

    private fun resolve(host: String, network: Network?): List<InetAddress> {
        val addresses = if (network != null) {
            network.getAllByName(host)
        } else {
            InetAddress.getAllByName(host)
        }
        return addresses.toList()
    }

    private fun closeQuietly(socket: Socket) {
        runCatching {
            socket.close()
        }
    }

    private fun isHostName(host: String): Boolean {
        if (host.contains(':')) {
            return false
        }
        return !host.all { it.isDigit() || it == '.' }
    }

    private fun makeError(error: Throwable): NWError {
        return error as? NWError ?: NWError(describe(error))
    }

    private fun describe(error: Throwable): String {
        val message = error.message
        return if (message.isNullOrEmpty()) {
            error.javaClass.simpleName
        } else {
            "${error.javaClass.simpleName}: $message"
        }
    }

    private companion object {
        private const val TAG = "MoblinNet"
        private const val DEFAULT_CONNECT_TIMEOUT_MS = 10_000L
        private const val TLS_HANDSHAKE_TIMEOUT_MS = 10_000
        private const val STREAM_READ_SIZE = 65_536
        private const val WRITE_BUFFER_SIZE = 65_536
        private const val MAXIMUM_BUFFERED_STREAM_BYTES = 4 * 1024 * 1024
        private const val MAXIMUM_DATAGRAM_SIZE = 65_535
        private const val MAXIMUM_BUFFERED_DATAGRAMS = 4096
        private const val UDP_RETRY_INTERVAL_MS = 1_000L
        private val nextId = AtomicInteger(0)
        private val totalStreamPendingBytes = AtomicLong(0)
        private val stopWriting = PendingSend(null, SendCompletion.idempotent)
        private val closer: ExecutorService = Executors.newCachedThreadPool { runnable ->
            Thread(runnable, "nw-close").apply {
                isDaemon = true
            }
        }
    }
}

package com.moblin.android.platform.rist

interface RistReceiverContextDelegate {
    fun ristReceiverContextConnected(virtualDestinationPort: Int)

    fun ristReceiverContextDisconnected(virtualDestinationPort: Int)

    fun ristReceiverContextReceivedData(virtualDestinationPort: Int, packets: List<ByteArray>)
}

private class RistReceiverContextStream {
    val peers = HashSet<Long>()
    var receivedPackets = ArrayList<ByteArray>()
    var latestReceivedPacketsTime = System.nanoTime()
}

class RistReceiverContext private constructor(private val callbacks: Callbacks) {
    internal constructor() : this(Callbacks())

    private val lock = Any()
    private var receiver = 0L
    private val streams = HashMap<Int, RistReceiverContextStream>()
    private val streamsLock = Any()

    @Volatile
    var delegate: RistReceiverContextDelegate? = null

    fun start(): Boolean {
        return synchronized(lock) {
            receiver != 0L && RistNative.receiverStart(receiver) == 0
        }
    }

    fun stop() {
        val receiver = synchronized(lock) {
            val receiver = this.receiver
            this.receiver = 0L
            receiver
        }
        if (receiver != 0L) {
            RistNative.receiverDestroy(receiver)
        }
    }

    internal fun handleConnectionStatusCallback(status: Int, peer: Long) {
        if (status != RistNative.RIST_CONNECTION_TIMED_OUT && status != RistNative.RIST_CLIENT_TIMED_OUT) {
            return
        }
        val disconnectedVirtualDestinationPorts = mutableListOf<Int>()
        synchronized(streamsLock) {
            val iterator = streams.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (entry.value.peers.remove(peer) && entry.value.peers.isEmpty()) {
                    iterator.remove()
                    disconnectedVirtualDestinationPorts.add(entry.key)
                }
            }
        }
        for (virtualDestinationPort in disconnectedVirtualDestinationPorts) {
            delegate?.ristReceiverContextDisconnected(virtualDestinationPort)
        }
    }

    internal fun handleDataHandlerCallback(virtualDestinationPort: Int, peer: Long, data: ByteArray) {
        var connected = false
        var packets: List<ByteArray>? = null
        synchronized(streamsLock) {
            var stream = streams[virtualDestinationPort]
            if (stream == null) {
                stream = RistReceiverContextStream()
                streams[virtualDestinationPort] = stream
                connected = true
            }
            stream.peers.add(peer)
            stream.receivedPackets.add(data)
            val now = System.nanoTime()
            if (now - stream.latestReceivedPacketsTime > 50_000_000L) {
                packets = stream.receivedPackets
                stream.latestReceivedPacketsTime = now
                stream.receivedPackets = ArrayList()
            }
        }
        if (connected) {
            delegate?.ristReceiverContextConnected(virtualDestinationPort)
        }
        packets?.let {
            delegate?.ristReceiverContextReceivedData(virtualDestinationPort, it)
        }
    }

    private class Callbacks : RistReceiverCallbacks {
        @Volatile
        var context: RistReceiverContext? = null

        override fun onConnectionStatus(peer: Long, status: Int) {
            context?.handleConnectionStatusCallback(status, peer)
        }

        override fun onData(virtualDestinationPort: Int, peer: Long, payload: ByteArray) {
            context?.handleDataHandlerCallback(virtualDestinationPort, peer, payload)
        }
    }

    companion object {
        operator fun invoke(inputUrl: String, profile: rist_profile = RIST_PROFILE_MAIN): RistReceiverContext? {
            if (!RistNative.loaded) {
                return null
            }
            val callbacks = Callbacks()
            val receiver = RistNative.receiverCreate(profile, inputUrl, callbacks)
            if (receiver == 0L) {
                return null
            }
            val context = RistReceiverContext(callbacks)
            context.receiver = receiver
            callbacks.context = context
            return context
        }
    }
}

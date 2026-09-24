package com.moblin.android.platform.rist

import android.net.Uri
import android.util.Log
import com.moblin.android.platform.network.NWInterface
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

private const val TAG = "MoblinRist"

data class RistSenderStats(
    val peerId: UInt,
    val bandwidth: ULong,
    val retryBandwidth: ULong,
    val sentPackets: ULong,
    val receivedPackets: ULong,
    val retransmittedPackets: ULong,
    val quality: Double,
    val rtt: UInt,
)

data class RistStats(val sender: RistSenderStats)

class RistPeer internal constructor(
    private val context: RistSenderContext,
    private val peer: Long,
    private val id: UInt,
) {
    private var closed = false

    fun setWeight(weight: UInt) {
        synchronized(this) {
            if (!closed) {
                context.setPeerWeight(peer, weight)
            }
        }
    }

    fun getId(): UInt {
        return id
    }

    fun close() {
        synchronized(this) {
            if (closed) {
                return
            }
            closed = true
            context.removePeer(peer)
        }
    }
}

interface RistSenderContextDelegate {
    fun ristSenderContextStats(context: RistSenderContext, stats: RistStats)

    fun ristSenderContextPeerConnected(context: RistSenderContext, peerId: UInt)

    fun ristSenderContextPeerDisconnected(context: RistSenderContext, peerId: UInt)
}

class RistSenderContext private constructor(private val callbacks: Callbacks) {
    private val lock = ReentrantReadWriteLock()
    private var sender = 0L

    @Volatile
    var delegate: RistSenderContextDelegate? = null

    fun start(): Boolean {
        return lock.read {
            sender != 0L && RistNative.senderStart(sender) == 0
        }
    }

    fun stop() {
        val sender = lock.write {
            val sender = this.sender
            this.sender = 0L
            sender
        }
        if (sender != 0L) {
            RistNative.senderDestroy(sender)
        }
    }

    fun addPeer(url: String): RistPeer? {
        val network = networkHandle(url) ?: return null
        return lock.read {
            if (sender == 0L) {
                return null
            }
            val peer = RistNative.senderAddPeer(sender, url, network)
            if (peer == 0L) {
                return null
            }
            RistPeer(this, peer, RistNative.peerGetId(peer).toUInt())
        }
    }

    fun send(data: ByteArray): Boolean {
        return send(data, data.size)
    }

    fun send(dataPointer: ByteArray, count: Int): Boolean {
        return lock.read {
            sender != 0L && RistNative.senderWrite(sender, dataPointer, count) == count
        }
    }

    internal fun setPeerWeight(peer: Long, weight: UInt) {
        lock.read {
            if (sender != 0L) {
                RistNative.senderSetPeerWeight(sender, peer, weight.toInt())
            }
        }
    }

    internal fun removePeer(peer: Long) {
        lock.read {
            if (sender != 0L) {
                RistNative.senderRemovePeer(sender, peer)
            }
        }
    }

    private fun handleStats(stats: RistStats) {
        delegate?.ristSenderContextStats(this, stats)
    }

    private fun handleConnectionStatus(peerId: UInt, status: Int) {
        val delegate = delegate ?: return
        if (status == RistNative.RIST_CONNECTION_ESTABLISHED) {
            delegate.ristSenderContextPeerConnected(this, peerId)
        } else {
            delegate.ristSenderContextPeerDisconnected(this, peerId)
        }
    }

    private fun networkHandle(url: String): Long? {
        val interfaceName = try {
            Uri.parse(url).getQueryParameters("miface").lastOrNull()
        } catch (error: Exception) {
            null
        }
        if (interfaceName.isNullOrEmpty() || isIpAddress(interfaceName)) {
            return 0L
        }
        val network = NWInterface(interfaceName, NWInterface.InterfaceType.other, null).resolveNetwork()
        if (network == null) {
            Log.i(TAG, "No network for interface $interfaceName")
            return null
        }
        return network.networkHandle
    }

    private fun isIpAddress(value: String): Boolean {
        return value.contains(':') || value.all { it.isDigit() || it == '.' }
    }

    private class Callbacks : RistSenderCallbacks {
        @Volatile
        var context: RistSenderContext? = null

        override fun onStats(
            peerId: Int,
            bandwidth: Long,
            retryBandwidth: Long,
            sentPackets: Long,
            receivedPackets: Long,
            retransmittedPackets: Long,
            quality: Double,
            rtt: Int,
        ) {
            context?.handleStats(
                RistStats(
                    sender = RistSenderStats(
                        peerId = peerId.toUInt(),
                        bandwidth = bandwidth.toULong(),
                        retryBandwidth = retryBandwidth.toULong(),
                        sentPackets = sentPackets.toULong(),
                        receivedPackets = receivedPackets.toULong(),
                        retransmittedPackets = retransmittedPackets.toULong(),
                        quality = quality,
                        rtt = rtt.toUInt(),
                    ),
                ),
            )
        }

        override fun onConnectionStatus(peerId: Int, status: Int) {
            context?.handleConnectionStatus(peerId.toUInt(), status)
        }
    }

    companion object {
        operator fun invoke(profile: rist_profile = RIST_PROFILE_MAIN): RistSenderContext? {
            if (!RistNative.loaded) {
                return null
            }
            val callbacks = Callbacks()
            val sender = RistNative.senderCreate(profile, callbacks)
            if (sender == 0L) {
                return null
            }
            val context = RistSenderContext(callbacks)
            context.sender = sender
            callbacks.context = context
            return context
        }
    }
}

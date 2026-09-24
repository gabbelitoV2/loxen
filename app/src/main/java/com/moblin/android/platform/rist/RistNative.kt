package com.moblin.android.platform.rist

import android.util.Log

private const val TAG = "MoblinRist"

internal interface RistSenderCallbacks {
    fun onStats(
        peerId: Int,
        bandwidth: Long,
        retryBandwidth: Long,
        sentPackets: Long,
        receivedPackets: Long,
        retransmittedPackets: Long,
        quality: Double,
        rtt: Int,
    )

    fun onConnectionStatus(peerId: Int, status: Int)
}

internal interface RistReceiverCallbacks {
    fun onConnectionStatus(peer: Long, status: Int)

    fun onData(virtualDestinationPort: Int, peer: Long, payload: ByteArray)
}

internal object RistNative {
    const val RIST_CONNECTION_ESTABLISHED: Int = 0
    const val RIST_CONNECTION_TIMED_OUT: Int = 1
    const val RIST_CLIENT_CONNECTED: Int = 2
    const val RIST_CLIENT_TIMED_OUT: Int = 3

    val loaded: Boolean = try {
        System.loadLibrary("moblin_rist")
        true
    } catch (error: Throwable) {
        Log.e(TAG, "libmoblin_rist failed to load: $error")
        false
    }

    @JvmStatic
    external fun librist_version(): String

    @JvmStatic
    external fun senderCreate(profile: Int, callbacks: RistSenderCallbacks): Long

    @JvmStatic
    external fun senderStart(sender: Long): Int

    @JvmStatic
    external fun senderDestroy(sender: Long)

    @JvmStatic
    external fun senderAddPeer(sender: Long, url: String, network: Long): Long

    @JvmStatic
    external fun senderRemovePeer(sender: Long, peer: Long): Int

    @JvmStatic
    external fun senderSetPeerWeight(sender: Long, peer: Long, weight: Int): Int

    @JvmStatic
    external fun senderWrite(sender: Long, data: ByteArray, count: Int): Int

    @JvmStatic
    external fun peerGetId(peer: Long): Int

    @JvmStatic
    external fun receiverCreate(profile: Int, url: String, callbacks: RistReceiverCallbacks): Long

    @JvmStatic
    external fun receiverStart(receiver: Long): Int

    @JvmStatic
    external fun receiverDestroy(receiver: Long)
}

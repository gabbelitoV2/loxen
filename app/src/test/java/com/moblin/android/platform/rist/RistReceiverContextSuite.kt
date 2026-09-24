package com.moblin.android.platform.rist

import kotlin.test.assertEquals
import org.junit.Test

class RistReceiverContextSuite {
    private class Delegate : RistReceiverContextDelegate {
        val connected = mutableListOf<Int>()
        val disconnected = mutableListOf<Int>()

        override fun ristReceiverContextConnected(virtualDestinationPort: Int) {
            connected.add(virtualDestinationPort)
        }

        override fun ristReceiverContextDisconnected(virtualDestinationPort: Int) {
            disconnected.add(virtualDestinationPort)
        }

        override fun ristReceiverContextReceivedData(virtualDestinationPort: Int, packets: List<ByteArray>) {}
    }

    @Test
    fun peerTimeoutDisconnectsEveryPortItFed() {
        val context = RistReceiverContext()
        val delegate = Delegate()
        context.delegate = delegate
        context.handleDataHandlerCallback(0, 7L, byteArrayOf(1))
        context.handleDataHandlerCallback(1, 7L, byteArrayOf(2))
        assertEquals(listOf(0, 1), delegate.connected)
        context.handleConnectionStatusCallback(RistNative.RIST_CLIENT_TIMED_OUT, 7L)
        assertEquals(listOf(0, 1), delegate.disconnected.sorted())
    }

    @Test
    fun portStaysConnectedWhileAnotherPeerFeedsIt() {
        val context = RistReceiverContext()
        val delegate = Delegate()
        context.delegate = delegate
        context.handleDataHandlerCallback(1, 7L, byteArrayOf(1))
        context.handleDataHandlerCallback(1, 8L, byteArrayOf(2))
        context.handleConnectionStatusCallback(RistNative.RIST_CONNECTION_TIMED_OUT, 7L)
        assertEquals(emptyList(), delegate.disconnected)
        context.handleConnectionStatusCallback(RistNative.RIST_CONNECTION_TIMED_OUT, 8L)
        assertEquals(listOf(1), delegate.disconnected)
    }

    @Test
    fun otherStatusesDoNotDisconnect() {
        val context = RistReceiverContext()
        val delegate = Delegate()
        context.delegate = delegate
        context.handleDataHandlerCallback(1, 7L, byteArrayOf(1))
        context.handleConnectionStatusCallback(RistNative.RIST_CLIENT_CONNECTED, 7L)
        assertEquals(emptyList(), delegate.disconnected)
    }
}

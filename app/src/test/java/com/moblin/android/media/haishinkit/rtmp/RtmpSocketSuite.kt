package com.moblin.android.media.haishinkit.rtmp

import java.util.concurrent.Executors
import kotlinx.coroutines.asCoroutineDispatcher
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private class SocketDelegateMock : RtmpSocketDelegate {
    var numberOfCloses = 0
    var disconnectCodes: MutableList<RtmpStatusCode> = mutableListOf()

    override fun socketHandshakeDone(socket: RtmpSocket) {}

    override fun socketClosed(socket: RtmpSocket) {
        numberOfCloses += 1
    }

    override fun socketDisconnected(socket: RtmpSocket, code: RtmpStatusCode) {
        disconnectCodes.add(code)
    }

    override fun socketDataReceived(socket: RtmpSocket, data: ByteArray): ByteArray {
        return data
    }

    override fun socketUpdateStats(socket: RtmpSocket, totalBytesSent: Long) {}
}

@RunWith(RobolectricTestRunner::class)
class RtmpSocketSuite {
    @Test
    fun closeReportsDisconnectBeforeHandshake() {
        val delegate = SocketDelegateMock()
        val socket = RtmpSocket(
            name = "test",
            queue = Executors.newSingleThreadExecutor().asCoroutineDispatcher(),
        )
        socket.delegate = delegate
        socket.close(isDisconnected = true)
        assertEquals(RtmpSocketReadyState.closed, socket.readyState)
        assertEquals(1, delegate.numberOfCloses)
        assertEquals(listOf(RtmpStatusCode.connectFailed), delegate.disconnectCodes)
    }

    @Test
    fun closeWithoutDisconnectDoesNotReportDisconnect() {
        val delegate = SocketDelegateMock()
        val socket = RtmpSocket(
            name = "test",
            queue = Executors.newSingleThreadExecutor().asCoroutineDispatcher(),
        )
        socket.delegate = delegate
        socket.close(isDisconnected = false)
        assertEquals(1, delegate.numberOfCloses)
        assertTrue(delegate.disconnectCodes.isEmpty())
    }

    @Test
    fun secondCloseDoesNothing() {
        val delegate = SocketDelegateMock()
        val socket = RtmpSocket(
            name = "test",
            queue = Executors.newSingleThreadExecutor().asCoroutineDispatcher(),
        )
        socket.delegate = delegate
        socket.close(isDisconnected = false)
        socket.close(isDisconnected = true)
        assertEquals(1, delegate.numberOfCloses)
        assertTrue(delegate.disconnectCodes.isEmpty())
    }
}

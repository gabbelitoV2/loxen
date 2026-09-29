package com.moblin.android.platform.network

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Before
import org.junit.Test

class OffMainSocketSuite {
    private val mainExecutor = Executors.newSingleThreadExecutor { Thread(it, "main") }
    private val mainScope = CoroutineScope(SupervisorJob() + mainExecutor.asCoroutineDispatcher())
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var server: ServerSocket
    private lateinit var client: Socket
    private lateinit var accepted: Socket

    @Before
    fun setUp() {
        server = ServerSocket(0, 1, InetAddress.getLoopbackAddress())
        client = Socket(InetAddress.getLoopbackAddress(), server.localPort)
        accepted = server.accept()
    }

    @After
    fun tearDown() {
        runCatching { client.close() }
        runCatching { accepted.close() }
        runCatching { server.close() }
        ioScope.cancel()
        mainScope.cancel()
        mainExecutor.shutdownNow()
    }

    private fun readLine(input: InputStream): String {
        val line = StringBuilder()
        while (true) {
            val byte = input.read()
            if (byte < 0 || byte == '\n'.code) {
                return line.toString()
            }
            line.append(byte.toChar())
        }
    }

    @Test
    fun theRequestHeadIsReadOffMainAndTheHandshakeReadsItOnMainWithoutTheSocket() {
        val request = "GET / HTTP/1.1\r\nHost: phone\r\nSec-WebSocket-Key: abc\r\n\r\n"
        val ran = CountDownLatch(1)
        var thread: Thread? = null
        val lines = mutableListOf<String>()
        var after = ""
        OffMainSocket.readRequest(accepted, ioScope, mainScope, { true }) {
            thread = Thread.currentThread()
            val input = OffMainSocket.input(accepted)
            repeat(4) {
                lines.add(readLine(input))
            }
            after = "${input.read().toChar()}${input.read().toChar()}"
            ran.countDown()
        }
        client.getOutputStream().write(request.substring(0, 20).toByteArray())
        assertFalse(ran.await(200, TimeUnit.MILLISECONDS))
        client.getOutputStream().write((request.substring(20) + "AB").toByteArray())
        assertTrue(ran.await(5, TimeUnit.SECONDS))
        assertSame(mainExecutor.submit(Callable { Thread.currentThread() }).get(), thread)
        assertEquals(listOf("GET / HTTP/1.1\r", "Host: phone\r", "Sec-WebSocket-Key: abc\r", "\r"), lines)
        assertEquals("AB", after)
        assertEquals(0, accepted.soTimeout)
    }

    @Test
    fun aClientThatLeavesBeforeTheEndOfTheHeadIsClosedAndNeverHandled() {
        val ran = CountDownLatch(1)
        OffMainSocket.readRequest(accepted, ioScope, mainScope, { true }) { ran.countDown() }
        client.getOutputStream().write("GET / HTTP/1.1\r\nHost: phone\r\n".toByteArray())
        client.close()
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (!accepted.isClosed && System.nanoTime() < deadline) {
            Thread.sleep(10)
        }
        assertTrue(accepted.isClosed)
        assertFalse(ran.await(100, TimeUnit.MILLISECONDS))
    }

    @Test
    fun aRequestThatEndsAfterTheServerStoppedAcceptingIsClosedOnMainAndNeverHandled() {
        val ran = CountDownLatch(1)
        val checked = AtomicReference<Thread>()
        OffMainSocket.readRequest(accepted, ioScope, mainScope, {
            checked.set(Thread.currentThread())
            false
        }) { ran.countDown() }
        client.soTimeout = 5_000
        client.getOutputStream().write("GET / HTTP/1.1\r\nHost: phone\r\n\r\n".toByteArray())
        assertEquals(-1, client.getInputStream().read())
        assertTrue(accepted.isClosed)
        assertFalse(ran.await(100, TimeUnit.MILLISECONDS))
        assertSame(mainExecutor.submit(Callable { Thread.currentThread() }).get(), checked.get())
    }

    @Test
    fun inputOfASocketWithoutAReadRequestIsTheSocketItself() {
        client.getOutputStream().write("xy".toByteArray())
        val input = OffMainSocket.input(accepted)
        assertEquals('x'.code, input.read())
        assertEquals('y'.code, input.read())
    }

    private class FakeSocket(private val output: OutputStream) : Socket() {
        val closes = AtomicInteger()

        override fun getOutputStream(): OutputStream = output

        override fun close() {
            closes.incrementAndGet()
        }

        override fun isClosed(): Boolean = closes.get() > 0
    }

    @Test
    fun framesAreWrittenInOrderOnAnotherThreadAndOnlyWhenFlushed() {
        val release = CountDownLatch(1)
        val written = CountDownLatch(2)
        val bytes = ByteArrayOutputStream()
        val threads = mutableListOf<Thread>()
        val output = object : OutputStream() {
            override fun write(b: Int) {
                write(byteArrayOf(b.toByte()), 0, 1)
            }

            override fun write(b: ByteArray, off: Int, len: Int) {
                release.await(5, TimeUnit.SECONDS)
                synchronized(bytes) {
                    threads.add(Thread.currentThread())
                    bytes.write(b, off, len)
                }
                written.countDown()
            }
        }
        val queued = OffMainSocket.output(FakeSocket(output))
        queued.write("abc".toByteArray())
        queued.flush()
        queued.write("de".toByteArray())
        queued.write('f'.code)
        queued.flush()
        queued.flush()
        queued.write("g".toByteArray())
        assertEquals(0, synchronized(bytes) { bytes.size() })
        release.countDown()
        assertTrue(written.await(5, TimeUnit.SECONDS))
        synchronized(bytes) {
            assertEquals("abcdef", bytes.toString(Charsets.UTF_8.name()))
            assertTrue(threads.none { it === Thread.currentThread() })
        }
    }

    @Test
    fun aFailedWriteClosesTheSocketOnceAndDropsTheWritesAfterIt() {
        val attempts = AtomicInteger()
        val failed = CountDownLatch(1)
        val output = object : OutputStream() {
            override fun write(b: Int) {
                attempts.incrementAndGet()
                failed.countDown()
                throw IOException("Broken pipe")
            }
        }
        val socket = FakeSocket(output)
        val queued = OffMainSocket.output(socket)
        queued.write("a".toByteArray())
        queued.flush()
        assertTrue(failed.await(5, TimeUnit.SECONDS))
        queued.write("b".toByteArray())
        queued.flush()
        queued.write("c".toByteArray())
        queued.flush()
        Thread.sleep(300)
        assertEquals(1, attempts.get())
        assertEquals(1, socket.closes.get())
    }
}
